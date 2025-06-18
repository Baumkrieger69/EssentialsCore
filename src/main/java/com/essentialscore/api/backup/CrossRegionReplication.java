package com.essentialscore.api.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles cross-region backup replication for disaster recovery.
 */
public class CrossRegionReplication {
    private static final Logger LOGGER = Logger.getLogger(CrossRegionReplication.class.getName());
    
    private final BackupSystem backupSystem;
    private final ScheduledExecutorService executor;
    private boolean running = false;
    
    /**
     * Creates a new cross-region replication service.
     *
     * @param backupSystem The backup system
     */
    public CrossRegionReplication(BackupSystem backupSystem) {
        this.backupSystem = backupSystem;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }
    
    /**
     * Starts the replication service.
     */
    public void start() {
        if (running) return;
        
        // Schedule periodic replication check
        executor.scheduleAtFixedRate(
            this::checkPendingReplications,
            0,
            60,
            TimeUnit.MINUTES
        );
        
        running = true;
        LOGGER.info("Cross-region replication service started");
    }
    
    /**
     * Stops the replication service.
     */
    public void stop() {
        if (!running) return;
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        running = false;
        LOGGER.info("Cross-region replication service stopped");
    }
    
    /**
     * Checks for pending replications.
     */
    private void checkPendingReplications() {
        try {
            // Get replication target
            String targetPath = backupSystem.getConfig().getCrossRegionReplicationTarget();
            if (targetPath == null || targetPath.isEmpty()) {
                LOGGER.fine("No replication target configured");
                return;
            }
            
            File targetDir = new File(targetPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            
            // Check metadata for unreplicated backups
            for (BackupMetadata metadata : backupSystem.getMetadataManager().getAllMetadata()) {
                File replicationMarker = new File(backupSystem.getBackupDirectory(metadata.getBackupId()), ".replicated");
                
                if (!replicationMarker.exists()) {
                    replicateBackup(metadata);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking pending replications", e);
        }
    }
    
    /**
     * Replicates a backup to the remote location.
     *
     * @param metadata The backup metadata
     */
    public void replicateBackup(BackupMetadata metadata) {
        String targetPath = backupSystem.getConfig().getCrossRegionReplicationTarget();
        if (targetPath == null || targetPath.isEmpty()) {
            LOGGER.fine("No replication target configured");
            return;
        }
        
        // Execute replication asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                String backupId = metadata.getBackupId();
                LOGGER.info("Starting replication of backup: " + backupId);
                
                // Source and target directories
                File sourceDir = backupSystem.getBackupDirectory(backupId);
                File targetDir = new File(targetPath, backupId);
                
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                
                // Copy all files
                copyDirectory(sourceDir.toPath(), targetDir.toPath());
                
                // Save metadata
                File metadataFile = new File(backupSystem.getMetadataManager().getMetadataDir(), backupId + ".json");
                File targetMetadataDir = new File(targetPath, "metadata");
                if (!targetMetadataDir.exists()) {
                    targetMetadataDir.mkdirs();
                }
                
                Files.copy(
                    metadataFile.toPath(),
                    new File(targetMetadataDir, backupId + ".json").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
                
                // Mark as replicated
                File replicationMarker = new File(sourceDir, ".replicated");
                replicationMarker.createNewFile();
                
                LOGGER.info("Completed replication of backup: " + backupId);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to replicate backup: " + metadata.getBackupId(), e);
            }
        });
    }
    
    /**
     * Copies a directory recursively.
     *
     * @param source The source directory
     * @param target The target directory
     * @throws IOException If an error occurs
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
            .filter(path -> !path.toString().endsWith(".replicated"))
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }
    
    /**
     * Imports a backup from a remote location.
     *
     * @param backupId The backup ID
     * @return true if successful
     */
    public boolean importBackup(String backupId) {
        String sourcePath = backupSystem.getConfig().getCrossRegionReplicationTarget();
        if (sourcePath == null || sourcePath.isEmpty()) {
            LOGGER.warning("No replication target configured for import");
            return false;
        }
        
        try {
            LOGGER.info("Starting import of backup: " + backupId);
            
            // Source and target directories
            File sourceDir = new File(sourcePath, backupId);
            if (!sourceDir.exists()) {
                LOGGER.warning("Backup not found in remote location: " + backupId);
                return false;
            }
            
            // Import metadata first
            File sourceMetadataFile = new File(new File(sourcePath, "metadata"), backupId + ".json");
            if (!sourceMetadataFile.exists()) {
                LOGGER.warning("Backup metadata not found in remote location: " + backupId);
                return false;
            }
            
            // Create target directory
            File targetDir = backupSystem.getBackupDirectory(backupId);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            
            // Copy metadata
            File targetMetadataFile = new File(backupSystem.getMetadataManager().getMetadataDir(), backupId + ".json");
            Files.copy(
                sourceMetadataFile.toPath(),
                targetMetadataFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );
            
            // Copy files
            copyDirectory(sourceDir.toPath(), targetDir.toPath());
            
            // Load metadata
            BackupMetadata metadata = backupSystem.getMetadataManager().getMetadata(backupId);
            if (metadata == null) {
                LOGGER.warning("Failed to load imported metadata: " + backupId);
                return false;
            }
            
            // Mark as replicated
            File replicationMarker = new File(targetDir, ".replicated");
            replicationMarker.createNewFile();
            
            LOGGER.info("Completed import of backup: " + backupId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import backup: " + backupId, e);
            return false;
        }
    }
} 
