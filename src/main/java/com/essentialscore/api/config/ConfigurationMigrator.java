package com.essentialscore.api.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles migration of configuration files between versions.
 */
public class ConfigurationMigrator {
    private final Logger logger;
    private final File backupDir;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    /**
     * Creates a new configuration migrator.
     *
     * @param logger The logger
     * @param backupDir The directory to store backups
     */
    public ConfigurationMigrator(Logger logger, File backupDir) {
        this.logger = logger;
        this.backupDir = backupDir;
        
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }
    
    /**
     * Migrates a configuration file from one version to another.
     *
     * @param configFile The configuration file to migrate
     * @param migrations The list of migrations to apply
     * @return true if migration was successful
     */
    public boolean migrateConfig(File configFile, List<ConfigMigration> migrations) {
        if (!configFile.exists()) {
            logger.warning("Config file does not exist: " + configFile.getPath());
            return false;
        }
        
        // Create backup before migrating
        File backupFile = createBackup(configFile);
        if (backupFile == null) {
            logger.severe("Failed to create backup before migration: " + configFile.getPath());
            return false;
        }
        
        // Load the configuration
        Configuration config = ConfigurationLoader.load(configFile);
        if (config == null) {
            logger.severe("Failed to load configuration for migration: " + configFile.getPath());
            return false;
        }
        
        // Check current version
        String currentVersion = config.getString("version", "0.0.0");
        
        // Sort migrations by from version
        migrations.sort((a, b) -> a.getFromVersion().compareTo(b.getFromVersion()));
        
        // Apply migrations in order
        boolean migrated = false;
        for (ConfigMigration migration : migrations) {
            if (migration.getFromVersion().equals(currentVersion)) {
                try {
                    logger.info("Migrating config " + configFile.getName() + 
                               " from " + currentVersion + " to " + migration.getToVersion());
                    
                    // Apply the migration
                    migration.migrate(config);
                    
                    // Update version
                    config.set("version", migration.getToVersion());
                    currentVersion = migration.getToVersion();
                    migrated = true;
                    
                    // Save after each migration
                    config.save();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during config migration", e);
                    
                    // Try to restore backup
                    restoreBackup(backupFile, configFile);
                    return false;
                }
            }
        }
        
        return migrated;
    }
    
    /**
     * Creates a backup of a configuration file.
     *
     * @param configFile The file to backup
     * @return The backup file, or null if backup failed
     */
    public File createBackup(File configFile) {
        if (!configFile.exists()) {
            return null;
        }
        
        try {
            String timestamp = LocalDateTime.now().format(dateFormatter);
            String backupFileName = configFile.getName() + "." + timestamp + ".bak";
            File backupFile = new File(backupDir, backupFileName);
            
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Created backup of " + configFile.getName() + " at " + backupFile.getPath());
            
            return backupFile;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create config backup", e);
            return null;
        }
    }
    
    /**
     * Restores a configuration file from a backup.
     *
     * @param backupFile The backup file
     * @param configFile The destination file
     * @return true if restore was successful
     */
    public boolean restoreBackup(File backupFile, File configFile) {
        if (!backupFile.exists()) {
            logger.warning("Backup file does not exist: " + backupFile.getPath());
            return false;
        }
        
        try {
            Files.copy(backupFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Restored config from backup: " + backupFile.getPath());
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to restore config from backup", e);
            return false;
        }
    }
    
    /**
     * Lists available backups for a configuration file.
     *
     * @param configFileName The name of the config file
     * @return List of backup files
     */
    public List<File> listBackups(String configFileName) {
        List<File> backups = new ArrayList<>();
        
        if (!backupDir.exists()) {
            return backups;
        }
        
        File[] files = backupDir.listFiles((dir, name) -> name.startsWith(configFileName + ".") && name.endsWith(".bak"));
        if (files != null) {
            for (File file : files) {
                backups.add(file);
            }
        }
        
        return backups;
    }
    
    /**
     * Interface for configuration migrations.
     */
    public interface ConfigMigration {
        /**
         * Gets the version this migration starts from.
         *
         * @return The from version
         */
        String getFromVersion();
        
        /**
         * Gets the version this migration results in.
         *
         * @return The to version
         */
        String getToVersion();
        
        /**
         * Applies the migration to a configuration.
         *
         * @param config The configuration to migrate
         */
        void migrate(Configuration config);
    }
} 
