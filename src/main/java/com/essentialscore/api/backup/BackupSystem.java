package com.essentialscore.api.backup;

import com.essentialscore.api.module.ModuleRegistry;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive backup and disaster recovery system.
 */
public class BackupSystem {
    private static final Logger LOGGER = Logger.getLogger(BackupSystem.class.getName());
    
    private final Plugin plugin;
    private final ModuleRegistry moduleRegistry;
    private final BackupConfiguration config;
    private final File backupDirectory;
    private final ScheduledExecutorService scheduler;
    private final BackupMetadataManager metadataManager;
    private final DedupStore dedupStore;
    private final BackupValidationService validationService;
    private final Map<String, BackupProvider> backupProviders;
    private final CrossRegionReplication replicationService;
    
    private boolean isRunning = false;
    
    /**
     * Creates a new backup system.
     *
     * @param plugin The plugin
     * @param moduleRegistry The module registry
     */
    public BackupSystem(Plugin plugin, ModuleRegistry moduleRegistry) {
        this.plugin = plugin;
        this.moduleRegistry = moduleRegistry;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Create backup directory
        this.backupDirectory = new File(plugin.getDataFolder(), "backups");
        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs();
        }
        
        // Initialize components
        this.config = new BackupConfiguration(plugin);
        this.metadataManager = new BackupMetadataManager(new File(backupDirectory, "metadata"));
        this.dedupStore = new DedupStore(new File(backupDirectory, "chunks"));
        this.validationService = new BackupValidationService();
        this.backupProviders = new HashMap<>();
        this.replicationService = new CrossRegionReplication(this);
        
        // Register default backup providers
        registerDefaultProviders();
        
        LOGGER.info("Backup system initialized");
    }
    
    /**
     * Registers the default backup providers.
     */
    private void registerDefaultProviders() {
        registerBackupProvider(new ConfigurationBackupProvider());
        registerBackupProvider(new ModuleStateBackupProvider(moduleRegistry));
        registerBackupProvider(new PlayerDataBackupProvider());
        registerBackupProvider(new WorldBackupProvider());
        registerBackupProvider(new DatabaseBackupProvider());
    }
    
    /**
     * Registers a backup provider.
     *
     * @param provider The provider to register
     */
    public void registerBackupProvider(BackupProvider provider) {
        backupProviders.put(provider.getId(), provider);
        LOGGER.info("Registered backup provider: " + provider.getId());
    }
    
    /**
     * Unregisters a backup provider.
     *
     * @param providerId The provider ID
     */
    public void unregisterBackupProvider(String providerId) {
        backupProviders.remove(providerId);
        LOGGER.info("Unregistered backup provider: " + providerId);
    }
    
    /**
     * Starts the backup system.
     */
    public void start() {
        if (isRunning) return;
        
        // Load configuration
        config.reload();
        
        // Schedule automatic backups
        scheduleAutomaticBackups();
        
        // Schedule backup validation
        scheduleBackupValidation();
        
        // Start cross-region replication if enabled
        if (config.isCrossRegionReplicationEnabled()) {
            replicationService.start();
        }
        
        isRunning = true;
        LOGGER.info("Backup system started");
    }
    
    /**
     * Stops the backup system.
     */
    public void stop() {
        if (!isRunning) return;
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Stop replication service
        replicationService.stop();
        
        isRunning = false;
        LOGGER.info("Backup system stopped");
    }
    
    /**
     * Schedules automatic backups.
     */
    private void scheduleAutomaticBackups() {
        // Schedule full backups
        long fullBackupInterval = config.getFullBackupInterval();
        if (fullBackupInterval > 0) {
            scheduler.scheduleAtFixedRate(
                () -> createFullBackup("Scheduled full backup"),
                calculateInitialDelay(config.getFullBackupTime()),
                fullBackupInterval,
                TimeUnit.MINUTES
            );
            LOGGER.info("Scheduled full backups every " + fullBackupInterval + " minutes");
        }
        
        // Schedule incremental backups
        long incrementalBackupInterval = config.getIncrementalBackupInterval();
        if (incrementalBackupInterval > 0) {
            scheduler.scheduleAtFixedRate(
                () -> createIncrementalBackup("Scheduled incremental backup"),
                calculateInitialDelay(config.getIncrementalBackupTime()),
                incrementalBackupInterval,
                TimeUnit.MINUTES
            );
            LOGGER.info("Scheduled incremental backups every " + incrementalBackupInterval + " minutes");
        }
    }
    
    /**
     * Schedules backup validation.
     */
    private void scheduleBackupValidation() {
        long validationInterval = config.getBackupValidationInterval();
        if (validationInterval > 0) {
            scheduler.scheduleAtFixedRate(
                this::validateBackups,
                validationInterval,
                validationInterval,
                TimeUnit.MINUTES
            );
            LOGGER.info("Scheduled backup validation every " + validationInterval + " minutes");
        }
    }
    
    /**
     * Calculates the initial delay for a scheduled task.
     *
     * @param timeString The time string in HH:mm format
     * @return The initial delay in minutes
     */
    private long calculateInitialDelay(String timeString) {
        try {
            if (timeString == null || timeString.isEmpty()) {
                return 1; // Start in 1 minute if no time specified
            }
            
            String[] parts = timeString.split(":");
            int targetHour = Integer.parseInt(parts[0]);
            int targetMinute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime target = now.withHour(targetHour).withMinute(targetMinute).withSecond(0);
            
            if (target.isBefore(now)) {
                target = target.plusDays(1);
            }
            
            return java.time.Duration.between(now, target).toMinutes();
        } catch (Exception e) {
            LOGGER.warning("Invalid time format: " + timeString + ". Using default delay.");
            return 1; // Start in 1 minute as fallback
        }
    }
    
    /**
     * Creates a full backup.
     *
     * @param description The backup description
     * @return The backup metadata
     */
    public BackupMetadata createFullBackup(String description) {
        LOGGER.info("Starting full backup: " + description);
        
        try {
            // Create backup metadata
            String backupId = UUID.randomUUID().toString();
            Instant timestamp = Instant.now();
            BackupMetadata metadata = new BackupMetadata(
                backupId,
                BackupType.FULL,
                timestamp,
                description,
                null // No parent for full backups
            );
            
            // Create backup directory
            File backupDir = getBackupDirectory(backupId);
            backupDir.mkdirs();
            
            // Perform backup for each provider
            Map<String, Set<String>> backupFiles = new HashMap<>();
            
            for (BackupProvider provider : backupProviders.values()) {
                if (config.isProviderEnabled(provider.getId())) {
                    try {
                        LOGGER.info("Running backup provider: " + provider.getId());
                        Set<String> files = provider.performBackup(this, backupDir, null);
                        backupFiles.put(provider.getId(), files);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error in backup provider: " + provider.getId(), e);
                    }
                }
            }
            
            // Save metadata
            metadata.setBackupFiles(backupFiles);
            metadataManager.saveMetadata(metadata);
            
            // Validate backup
            boolean isValid = validationService.validateBackup(this, metadata);
            metadata.setValidated(true);
            metadata.setValid(isValid);
            metadataManager.saveMetadata(metadata);
            
            // Replicate if enabled
            if (config.isCrossRegionReplicationEnabled()) {
                replicationService.replicateBackup(metadata);
            }
            
            // Cleanup old backups
            cleanupOldBackups();
            
            LOGGER.info("Full backup completed: " + backupId);
            return metadata;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create full backup", e);
            return null;
        }
    }
    
    /**
     * Creates an incremental backup.
     *
     * @param description The backup description
     * @return The backup metadata
     */
    public BackupMetadata createIncrementalBackup(String description) {
        LOGGER.info("Starting incremental backup: " + description);
        
        try {
            // Find latest backup to use as parent
            BackupMetadata parent = metadataManager.getLatestBackup();
            if (parent == null) {
                LOGGER.info("No previous backup found, creating full backup instead");
                return createFullBackup(description);
            }
            
            // Create backup metadata
            String backupId = UUID.randomUUID().toString();
            Instant timestamp = Instant.now();
            BackupMetadata metadata = new BackupMetadata(
                backupId,
                BackupType.INCREMENTAL,
                timestamp,
                description,
                parent.getBackupId()
            );
            
            // Create backup directory
            File backupDir = getBackupDirectory(backupId);
            backupDir.mkdirs();
            
            // Perform backup for each provider
            Map<String, Set<String>> backupFiles = new HashMap<>();
            
            for (BackupProvider provider : backupProviders.values()) {
                if (config.isProviderEnabled(provider.getId())) {
                    try {
                        LOGGER.info("Running incremental backup provider: " + provider.getId());
                        File parentDir = getBackupDirectory(parent.getBackupId());
                        Set<String> files = provider.performIncrementalBackup(this, backupDir, parentDir);
                        backupFiles.put(provider.getId(), files);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error in incremental backup provider: " + provider.getId(), e);
                    }
                }
            }
            
            // Save metadata
            metadata.setBackupFiles(backupFiles);
            metadataManager.saveMetadata(metadata);
            
            // Validate backup
            boolean isValid = validationService.validateBackup(this, metadata);
            metadata.setValidated(true);
            metadata.setValid(isValid);
            metadataManager.saveMetadata(metadata);
            
            // Replicate if enabled
            if (config.isCrossRegionReplicationEnabled()) {
                replicationService.replicateBackup(metadata);
            }
            
            // Cleanup old backups
            cleanupOldBackups();
            
            LOGGER.info("Incremental backup completed: " + backupId);
            return metadata;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create incremental backup", e);
            return null;
        }
    }
    
    /**
     * Restores a backup to the specified point in time.
     *
     * @param pointInTime The point in time to restore to
     * @return true if successful
     */
    public boolean restoreToPointInTime(Instant pointInTime) {
        LOGGER.info("Starting point-in-time recovery to: " + pointInTime);
        
        try {
            // Find the appropriate backup
            BackupMetadata backup = metadataManager.findBackupClosestTo(pointInTime);
            if (backup == null) {
                LOGGER.warning("No backup found for point in time: " + pointInTime);
                return false;
            }
            
            return restoreBackup(backup);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to restore to point in time: " + pointInTime, e);
            return false;
        }
    }
    
    /**
     * Restores a specific backup.
     *
     * @param backupId The backup ID
     * @return true if successful
     */
    public boolean restoreBackup(String backupId) {
        try {
            BackupMetadata metadata = metadataManager.getMetadata(backupId);
            if (metadata == null) {
                LOGGER.warning("Backup not found: " + backupId);
                return false;
            }
            
            return restoreBackup(metadata);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to restore backup: " + backupId, e);
            return false;
        }
    }
    
    /**
     * Restores a backup.
     *
     * @param metadata The backup metadata
     * @return true if successful
     */
    private boolean restoreBackup(BackupMetadata metadata) {
        LOGGER.info("Restoring backup: " + metadata.getBackupId());
        
        try {
            // Validate backup first
            if (!metadata.isValidated() || !metadata.isValid()) {
                boolean valid = validationService.validateBackup(this, metadata);
                if (!valid) {
                    LOGGER.severe("Backup validation failed: " + metadata.getBackupId());
                    return false;
                }
            }
            
            // For incremental backups, we need to build the full backup chain
            List<BackupMetadata> backupChain = new ArrayList<>();
            BackupMetadata current = metadata;
            
            while (current != null) {
                backupChain.add(0, current); // Add to beginning of list
                
                if (current.getType() == BackupType.FULL) {
                    break; // Stop at full backup
                }
                
                String parentId = current.getParentBackupId();
                if (parentId == null) {
                    LOGGER.warning("Incremental backup without parent: " + current.getBackupId());
                    return false;
                }
                
                current = metadataManager.getMetadata(parentId);
                if (current == null) {
                    LOGGER.warning("Parent backup not found: " + parentId);
                    return false;
                }
            }
            
            // Perform restore for each provider, starting with the full backup
            for (BackupProvider provider : backupProviders.values()) {
                if (config.isProviderEnabled(provider.getId())) {
                    try {
                        LOGGER.info("Running restore for provider: " + provider.getId());
                        
                        // Apply backups in chain order
                        for (BackupMetadata backup : backupChain) {
                            File backupDir = getBackupDirectory(backup.getBackupId());
                            Set<String> providerFiles = backup.getBackupFiles().get(provider.getId());
                            
                            if (providerFiles != null && !providerFiles.isEmpty()) {
                                provider.performRestore(this, backupDir, providerFiles);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error in restore provider: " + provider.getId(), e);
                        return false;
                    }
                }
            }
            
            LOGGER.info("Backup restore completed: " + metadata.getBackupId());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to restore backup", e);
            return false;
        }
    }
    
    /**
     * Restores specific components from a backup.
     *
     * @param backupId The backup ID
     * @param providerIds The provider IDs to restore
     * @return true if successful
     */
    public boolean restoreComponents(String backupId, Set<String> providerIds) {
        LOGGER.info("Restoring specific components from backup: " + backupId);
        
        try {
            BackupMetadata metadata = metadataManager.getMetadata(backupId);
            if (metadata == null) {
                LOGGER.warning("Backup not found: " + backupId);
                return false;
            }
            
            // For incremental backups, we need to build the full backup chain
            List<BackupMetadata> backupChain = new ArrayList<>();
            BackupMetadata current = metadata;
            
            while (current != null) {
                backupChain.add(0, current); // Add to beginning of list
                
                if (current.getType() == BackupType.FULL) {
                    break; // Stop at full backup
                }
                
                String parentId = current.getParentBackupId();
                if (parentId == null) {
                    break;
                }
                
                current = metadataManager.getMetadata(parentId);
                if (current == null) {
                    LOGGER.warning("Parent backup not found: " + parentId);
                    break;
                }
            }
            
            // Perform restore for selected providers
            for (String providerId : providerIds) {
                BackupProvider provider = backupProviders.get(providerId);
                if (provider != null) {
                    try {
                        LOGGER.info("Running granular restore for provider: " + providerId);
                        
                        // Apply backups in chain order
                        for (BackupMetadata backup : backupChain) {
                            File backupDir = getBackupDirectory(backup.getBackupId());
                            Set<String> providerFiles = backup.getBackupFiles().get(providerId);
                            
                            if (providerFiles != null && !providerFiles.isEmpty()) {
                                provider.performRestore(this, backupDir, providerFiles);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error in granular restore provider: " + providerId, e);
                    }
                }
            }
            
            LOGGER.info("Granular restore completed for backup: " + backupId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to perform granular restore", e);
            return false;
        }
    }
    
    /**
     * Validates all backups.
     */
    public void validateBackups() {
        LOGGER.info("Starting backup validation");
        
        try {
            List<BackupMetadata> backups = metadataManager.getAllMetadata();
            
            for (BackupMetadata metadata : backups) {
                if (!metadata.isValidated()) {
                    try {
                        boolean isValid = validationService.validateBackup(this, metadata);
                        metadata.setValidated(true);
                        metadata.setValid(isValid);
                        metadataManager.saveMetadata(metadata);
                        
                        if (!isValid) {
                            LOGGER.warning("Backup validation failed: " + metadata.getBackupId());
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error validating backup: " + metadata.getBackupId(), e);
                    }
                }
            }
            
            LOGGER.info("Backup validation completed");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to validate backups", e);
        }
    }
    
    /**
     * Cleans up old backups according to retention policy.
     */
    public void cleanupOldBackups() {
        LOGGER.info("Cleaning up old backups");
        
        try {
            int retentionFull = config.getFullBackupRetention();
            int retentionIncremental = config.getIncrementalBackupRetention();
            
            List<BackupMetadata> fullBackups = metadataManager.getBackupsByType(BackupType.FULL);
            List<BackupMetadata> incrementalBackups = metadataManager.getBackupsByType(BackupType.INCREMENTAL);
            
            // Sort by timestamp (oldest first)
            fullBackups.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
            incrementalBackups.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
            
            // Remove excess full backups
            while (fullBackups.size() > retentionFull) {
                BackupMetadata oldestBackup = fullBackups.remove(0);
                deleteBackup(oldestBackup);
            }
            
            // Remove excess incremental backups
            while (incrementalBackups.size() > retentionIncremental) {
                BackupMetadata oldestBackup = incrementalBackups.remove(0);
                deleteBackup(oldestBackup);
            }
            
            LOGGER.info("Backup cleanup completed");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to clean up old backups", e);
        }
    }
    
    /**
     * Deletes a backup.
     *
     * @param metadata The backup metadata
     */
    private void deleteBackup(BackupMetadata metadata) {
        LOGGER.info("Deleting backup: " + metadata.getBackupId());
        
        try {
            // Delete backup directory
            File backupDir = getBackupDirectory(metadata.getBackupId());
            if (backupDir.exists()) {
                deleteDirectory(backupDir);
            }
            
            // Delete metadata
            metadataManager.deleteMetadata(metadata.getBackupId());
            
            LOGGER.info("Backup deleted: " + metadata.getBackupId());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete backup: " + metadata.getBackupId(), e);
        }
    }
    
    /**
     * Deletes a directory and its contents.
     *
     * @param directory The directory to delete
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    /**
     * Gets the backup directory for a backup ID.
     *
     * @param backupId The backup ID
     * @return The backup directory
     */
    public File getBackupDirectory(String backupId) {
        return new File(backupDirectory, backupId);
    }
    
    /**
     * Gets the deduplication store.
     *
     * @return The deduplication store
     */
    public DedupStore getDedupStore() {
        return dedupStore;
    }
    
    /**
     * Gets the backup configuration.
     *
     * @return The backup configuration
     */
    public BackupConfiguration getConfig() {
        return config;
    }
    
    /**
     * Gets the metadata manager.
     *
     * @return The metadata manager
     */
    public BackupMetadataManager getMetadataManager() {
        return metadataManager;
    }
    
    /**
     * Gets the validation service.
     *
     * @return The validation service
     */
    public BackupValidationService getValidationService() {
        return validationService;
    }
    
    /**
     * Gets the plugin.
     *
     * @return The plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }
    
    /**
     * Gets the module registry.
     *
     * @return The module registry
     */
    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }
} 
