package com.essentialscore.api.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provider for backing up configuration files.
 */
public class ConfigurationBackupProvider implements BackupProvider {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationBackupProvider.class.getName());
    
    private static final String[] CONFIG_EXTENSIONS = {".yml", ".yaml", ".json", ".properties", ".cfg", ".conf", ".config", ".xml", ".txt"};
    
    @Override
    public String getId() {
        return "configuration";
    }
    
    @Override
    public String getDisplayName() {
        return "Configuration Files";
    }
    
    @Override
    public Set<String> performBackup(BackupSystem backupSystem, File backupDir, Object context) throws Exception {
        LOGGER.info("Starting configuration backup");
        Set<String> backedUpFiles = new HashSet<>();
        
        // Create directory for configuration files
        File configBackupDir = new File(backupDir, "config");
        configBackupDir.mkdirs();
        
        // Get plugin directory
        File pluginDir = backupSystem.getPlugin().getDataFolder().getParentFile();
        
        // Save current server.properties
        backupServerProperties(configBackupDir, backedUpFiles);
        
        // Backup plugin configuration files
        backupPluginConfigurations(pluginDir, configBackupDir, backedUpFiles);
        
        LOGGER.info("Configuration backup completed, backed up " + backedUpFiles.size() + " files");
        return backedUpFiles;
    }
    
    @Override
    public Set<String> performIncrementalBackup(BackupSystem backupSystem, File backupDir, File previousBackupDir) throws Exception {
        File prevConfigBackupDir = new File(previousBackupDir, "config");
        
        // If the previous backup directory doesn't exist, perform a full backup
        if (!prevConfigBackupDir.exists()) {
            return performBackup(backupSystem, backupDir, null);
        }
        
        LOGGER.info("Starting incremental configuration backup");
        Set<String> backedUpFiles = new HashSet<>();
        
        // Create directory for configuration files
        File configBackupDir = new File(backupDir, "config");
        configBackupDir.mkdirs();
        
        // Get plugin directory
        File pluginDir = backupSystem.getPlugin().getDataFolder().getParentFile();
        
        // Create map of previous configuration files and their last modified times
        Map<String, Long> prevFiles = new HashMap<>();
        collectFileModificationTimes(prevConfigBackupDir, "", prevFiles);
        
        // Backup server.properties if changed
        File serverPropertiesFile = new File("server.properties");
        if (serverPropertiesFile.exists()) {
            File prevServerPropertiesFile = new File(prevConfigBackupDir, "server.properties");
            if (!prevServerPropertiesFile.exists() || 
                serverPropertiesFile.lastModified() > prevServerPropertiesFile.lastModified()) {
                backupServerProperties(configBackupDir, backedUpFiles);
            }
        }
        
        // Backup changed plugin configuration files
        backupChangedPluginConfigurations(pluginDir, configBackupDir, prevConfigBackupDir, prevFiles, backedUpFiles);
        
        LOGGER.info("Incremental configuration backup completed, backed up " + backedUpFiles.size() + " files");
        return backedUpFiles;
    }
    
    @Override
    public void performRestore(BackupSystem backupSystem, File backupDir, Set<String> files) throws Exception {
        LOGGER.info("Starting configuration restore");
        
        // Get config backup directory
        File configBackupDir = new File(backupDir, "config");
        if (!configBackupDir.exists()) {
            LOGGER.warning("Configuration backup directory not found: " + configBackupDir.getPath());
            return;
        }
        
        // Restore server.properties
        File serverPropertiesBackup = new File(configBackupDir, "server.properties");
        if (serverPropertiesBackup.exists() && files.contains("server.properties")) {
            File serverPropertiesFile = new File("server.properties");
            Files.copy(serverPropertiesBackup.toPath(), serverPropertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Restored server.properties");
        }
        
        // Restore plugin configuration files
        File pluginDir = backupSystem.getPlugin().getDataFolder().getParentFile();
        
        for (String file : files) {
            if (file.equals("server.properties")) continue;
            
            File backupFile = new File(configBackupDir, file);
            if (backupFile.exists()) {
                File targetFile = new File(pluginDir, file);
                
                // Create target directory if it doesn't exist
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }
                
                Files.copy(backupFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Restored configuration file: " + file);
            }
        }
        
        LOGGER.info("Configuration restore completed");
    }
    
    /**
     * Backs up the server.properties file.
     *
     * @param configBackupDir The configuration backup directory
     * @param backedUpFiles The set of backed up files
     * @throws IOException If an error occurs
     */
    private void backupServerProperties(File configBackupDir, Set<String> backedUpFiles) throws IOException {
        File serverPropertiesFile = new File("server.properties");
        if (serverPropertiesFile.exists()) {
            File serverPropertiesBackup = new File(configBackupDir, "server.properties");
            Files.copy(serverPropertiesFile.toPath(), serverPropertiesBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            backedUpFiles.add("server.properties");
            LOGGER.info("Backed up server.properties");
        }
    }
    
    /**
     * Backs up plugin configuration files.
     *
     * @param pluginDir The plugins directory
     * @param configBackupDir The configuration backup directory
     * @param backedUpFiles The set of backed up files
     */
    private void backupPluginConfigurations(File pluginDir, File configBackupDir, Set<String> backedUpFiles) {
        // Find and backup all configuration files in plugin directories
        for (File pluginFolder : pluginDir.listFiles(File::isDirectory)) {
            backupConfigFilesInDirectory(pluginFolder, pluginFolder, configBackupDir, backedUpFiles);
        }
    }
    
    /**
     * Backs up configuration files in a directory.
     *
     * @param baseDir The base directory
     * @param currentDir The current directory
     * @param configBackupDir The configuration backup directory
     * @param backedUpFiles The set of backed up files
     */
    private void backupConfigFilesInDirectory(File baseDir, File currentDir, File configBackupDir, Set<String> backedUpFiles) {
        if (currentDir == null || !currentDir.exists()) return;
        
        // Get all configuration files in the current directory
        File[] configFiles = currentDir.listFiles(this::isConfigFile);
        if (configFiles != null) {
            for (File configFile : configFiles) {
                try {
                    // Calculate relative path from plugins directory
                    String relativePath = baseDir.getParentFile().toPath().relativize(configFile.toPath()).toString();
                    
                    // Create backup file
                    File backupFile = new File(configBackupDir, relativePath);
                    if (!backupFile.getParentFile().exists()) {
                        backupFile.getParentFile().mkdirs();
                    }
                    
                    // Copy file
                    Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    backedUpFiles.add(relativePath);
                    LOGGER.fine("Backed up configuration file: " + relativePath);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to backup configuration file: " + configFile.getPath(), e);
                }
            }
        }
        
        // Recurse into subdirectories
        File[] subdirs = currentDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                backupConfigFilesInDirectory(baseDir, subdir, configBackupDir, backedUpFiles);
            }
        }
    }
    
    /**
     * Backs up changed plugin configuration files.
     *
     * @param pluginDir The plugins directory
     * @param configBackupDir The configuration backup directory
     * @param prevConfigBackupDir The previous configuration backup directory
     * @param prevFiles The previous files and their modification times
     * @param backedUpFiles The set of backed up files
     */
    private void backupChangedPluginConfigurations(File pluginDir, File configBackupDir, File prevConfigBackupDir,
                                                Map<String, Long> prevFiles, Set<String> backedUpFiles) {
        // Scan plugins folder for configuration files
        for (File pluginFolder : pluginDir.listFiles(File::isDirectory)) {
            backupChangedConfigFilesInDirectory(pluginDir.getParentFile(), pluginFolder, configBackupDir, 
                                             prevConfigBackupDir, prevFiles, backedUpFiles);
        }
    }
    
    /**
     * Backs up changed configuration files in a directory.
     *
     * @param baseDir The base directory
     * @param currentDir The current directory
     * @param configBackupDir The configuration backup directory
     * @param prevConfigBackupDir The previous configuration backup directory
     * @param prevFiles The previous files and their modification times
     * @param backedUpFiles The set of backed up files
     */
    private void backupChangedConfigFilesInDirectory(File baseDir, File currentDir, File configBackupDir,
                                                 File prevConfigBackupDir, Map<String, Long> prevFiles,
                                                 Set<String> backedUpFiles) {
        if (currentDir == null || !currentDir.exists()) return;
        
        // Get all configuration files in the current directory
        File[] configFiles = currentDir.listFiles(this::isConfigFile);
        if (configFiles != null) {
            for (File configFile : configFiles) {
                try {
                    // Calculate relative path from base directory
                    String relativePath = baseDir.toPath().relativize(configFile.toPath()).toString();
                    
                    // Check if file has changed
                    Long prevModified = prevFiles.get(relativePath);
                    if (prevModified == null || configFile.lastModified() > prevModified) {
                        // Create backup file
                        File backupFile = new File(configBackupDir, relativePath);
                        if (!backupFile.getParentFile().exists()) {
                            backupFile.getParentFile().mkdirs();
                        }
                        
                        // Copy file
                        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        backedUpFiles.add(relativePath);
                        LOGGER.fine("Backed up changed configuration file: " + relativePath);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to backup configuration file: " + configFile.getPath(), e);
                }
            }
        }
        
        // Recurse into subdirectories
        File[] subdirs = currentDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                backupChangedConfigFilesInDirectory(baseDir, subdir, configBackupDir, 
                                                prevConfigBackupDir, prevFiles, backedUpFiles);
            }
        }
    }
    
    /**
     * Collects file modification times.
     *
     * @param directory The directory to scan
     * @param basePath The base path
     * @param files The map to populate with file paths and modification times
     */
    private void collectFileModificationTimes(File directory, String basePath, Map<String, Long> files) {
        if (!directory.exists() || !directory.isDirectory()) return;
        
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                String path = basePath.isEmpty() ? file.getName() : basePath + "/" + file.getName();
                
                if (file.isFile()) {
                    files.put(path, file.lastModified());
                } else if (file.isDirectory()) {
                    collectFileModificationTimes(file, path, files);
                }
            }
        }
    }
    
    /**
     * Checks if a file is a configuration file.
     *
     * @param file The file to check
     * @return true if the file is a configuration file
     */
    private boolean isConfigFile(File file) {
        if (!file.isFile()) return false;
        
        String name = file.getName().toLowerCase();
        for (String ext : CONFIG_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }
} 
