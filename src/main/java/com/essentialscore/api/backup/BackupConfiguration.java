package com.essentialscore.api.backup;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages backup configuration settings.
 */
public class BackupConfiguration {
    private static final Logger LOGGER = Logger.getLogger(BackupConfiguration.class.getName());
    
    private final Plugin plugin;
    private final File configFile;
    private FileConfiguration config;
    
    // Default configuration values
    private static final long DEFAULT_FULL_BACKUP_INTERVAL = 24 * 60; // 24 hours in minutes
    private static final long DEFAULT_INCREMENTAL_BACKUP_INTERVAL = 60; // 60 minutes
    private static final String DEFAULT_FULL_BACKUP_TIME = "03:00"; // 3 AM
    private static final String DEFAULT_INCREMENTAL_BACKUP_TIME = "00:00"; // Every hour
    private static final int DEFAULT_FULL_BACKUP_RETENTION = 7; // Keep 7 full backups
    private static final int DEFAULT_INCREMENTAL_BACKUP_RETENTION = 24; // Keep 24 incremental backups
    private static final boolean DEFAULT_CROSS_REGION_REPLICATION = false;
    private static final String DEFAULT_REPLICATION_TARGET = "";
    private static final long DEFAULT_BACKUP_VALIDATION_INTERVAL = 12 * 60; // 12 hours in minutes
    
    /**
     * Creates a new backup configuration.
     *
     * @param plugin The plugin
     */
    public BackupConfiguration(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "backup-config.yml");
        reload();
    }
    
    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        try {
            if (!configFile.exists()) {
                plugin.saveResource("backup-config.yml", false);
            }
            
            config = YamlConfiguration.loadConfiguration(configFile);
            setDefaults();
            
            LOGGER.info("Backup configuration loaded");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load backup configuration", e);
            config = new YamlConfiguration();
            setDefaults();
        }
    }
    
    /**
     * Sets default configuration values.
     */
    private void setDefaults() {
        setDefaultIfNotExists("backup.full.interval", DEFAULT_FULL_BACKUP_INTERVAL);
        setDefaultIfNotExists("backup.incremental.interval", DEFAULT_INCREMENTAL_BACKUP_INTERVAL);
        setDefaultIfNotExists("backup.full.time", DEFAULT_FULL_BACKUP_TIME);
        setDefaultIfNotExists("backup.incremental.time", DEFAULT_INCREMENTAL_BACKUP_TIME);
        setDefaultIfNotExists("backup.full.retention", DEFAULT_FULL_BACKUP_RETENTION);
        setDefaultIfNotExists("backup.incremental.retention", DEFAULT_INCREMENTAL_BACKUP_RETENTION);
        setDefaultIfNotExists("backup.validation.interval", DEFAULT_BACKUP_VALIDATION_INTERVAL);
        setDefaultIfNotExists("backup.replication.enabled", DEFAULT_CROSS_REGION_REPLICATION);
        setDefaultIfNotExists("backup.replication.target", DEFAULT_REPLICATION_TARGET);
        
        // Default enabled providers
        setDefaultIfNotExists("backup.providers.configuration", true);
        setDefaultIfNotExists("backup.providers.moduleState", true);
        setDefaultIfNotExists("backup.providers.playerData", true);
        setDefaultIfNotExists("backup.providers.world", true);
        setDefaultIfNotExists("backup.providers.database", true);
        
        saveConfig();
    }
    
    /**
     * Sets a default configuration value if it doesn't exist.
     *
     * @param path The configuration path
     * @param value The default value
     */
    private void setDefaultIfNotExists(String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }
    
    /**
     * Saves the configuration to disk.
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save backup configuration", e);
        }
    }
    
    /**
     * Gets the full backup interval in minutes.
     *
     * @return The full backup interval
     */
    public long getFullBackupInterval() {
        return config.getLong("backup.full.interval", DEFAULT_FULL_BACKUP_INTERVAL);
    }
    
    /**
     * Gets the incremental backup interval in minutes.
     *
     * @return The incremental backup interval
     */
    public long getIncrementalBackupInterval() {
        return config.getLong("backup.incremental.interval", DEFAULT_INCREMENTAL_BACKUP_INTERVAL);
    }
    
    /**
     * Gets the full backup time (HH:mm).
     *
     * @return The full backup time
     */
    public String getFullBackupTime() {
        return config.getString("backup.full.time", DEFAULT_FULL_BACKUP_TIME);
    }
    
    /**
     * Gets the incremental backup time (HH:mm).
     *
     * @return The incremental backup time
     */
    public String getIncrementalBackupTime() {
        return config.getString("backup.incremental.time", DEFAULT_INCREMENTAL_BACKUP_TIME);
    }
    
    /**
     * Gets the number of full backups to retain.
     *
     * @return The number of full backups to retain
     */
    public int getFullBackupRetention() {
        return config.getInt("backup.full.retention", DEFAULT_FULL_BACKUP_RETENTION);
    }
    
    /**
     * Gets the number of incremental backups to retain.
     *
     * @return The number of incremental backups to retain
     */
    public int getIncrementalBackupRetention() {
        return config.getInt("backup.incremental.retention", DEFAULT_INCREMENTAL_BACKUP_RETENTION);
    }
    
    /**
     * Gets the backup validation interval in minutes.
     *
     * @return The backup validation interval
     */
    public long getBackupValidationInterval() {
        return config.getLong("backup.validation.interval", DEFAULT_BACKUP_VALIDATION_INTERVAL);
    }
    
    /**
     * Checks if cross-region replication is enabled.
     *
     * @return true if cross-region replication is enabled
     */
    public boolean isCrossRegionReplicationEnabled() {
        return config.getBoolean("backup.replication.enabled", DEFAULT_CROSS_REGION_REPLICATION);
    }
    
    /**
     * Gets the cross-region replication target.
     *
     * @return The cross-region replication target
     */
    public String getCrossRegionReplicationTarget() {
        return config.getString("backup.replication.target", DEFAULT_REPLICATION_TARGET);
    }
    
    /**
     * Checks if a provider is enabled.
     *
     * @param providerId The provider ID
     * @return true if the provider is enabled
     */
    public boolean isProviderEnabled(String providerId) {
        return config.getBoolean("backup.providers." + providerId, true);
    }
    
    /**
     * Sets whether a provider is enabled.
     *
     * @param providerId The provider ID
     * @param enabled Whether the provider is enabled
     */
    public void setProviderEnabled(String providerId, boolean enabled) {
        config.set("backup.providers." + providerId, enabled);
        saveConfig();
    }
    
    /**
     * Gets the FileConfiguration.
     *
     * @return The FileConfiguration
     */
    public FileConfiguration getConfig() {
        return config;
    }
} 
