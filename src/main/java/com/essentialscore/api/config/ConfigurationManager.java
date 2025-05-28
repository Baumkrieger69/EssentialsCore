package com.essentialscore.api.config;

import com.essentialscore.api.Module;
<<<<<<< HEAD
<<<<<<< HEAD
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Manager für Konfigurationsdateien der Module
 */
public class ConfigurationManager {

    /**
     * Lädt eine Konfiguration für ein Modul
     * 
     * @param module Das Modul
     * @param filename Der Dateiname der Konfiguration
     * @return Die Konfiguration
     */
    public static Configuration loadConfiguration(Module module, String filename) {
        return new DefaultConfiguration(module, filename);
    }
    
    /**
     * Speichert eine Konfiguration
     * 
     * @param config Die Konfiguration
     * @return true wenn erfolgreich
     */
    public static boolean saveConfiguration(Configuration config) {
=======
=======
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
import com.essentialscore.api.config.schema.ConfigSchema;
import com.essentialscore.api.config.schema.ValidationError;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for configuration files of modules.
 */
public class ConfigurationManager {
    private static final Logger logger = Logger.getLogger(ConfigurationManager.class.getName());
    private static final Map<String, Configuration> cachedConfigs = new ConcurrentHashMap<>();
    private static final Map<String, ConfigSchema> configSchemas = new ConcurrentHashMap<>();
    private static final File backupDir = new File("config-backups");
    private static final ConfigurationMigrator migrator = new ConfigurationMigrator(logger, backupDir);
    
    static {
        // Create backup directory if it doesn't exist
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }
    
    /**
     * Loads a configuration for a module with the specified schema.
     *
     * @param module The module
     * @param filename The configuration filename
     * @param schema The configuration schema for validation
     * @return The loaded configuration
     */
    public static Configuration loadConfiguration(Module module, String filename, ConfigSchema schema) {
        String configKey = module.getName() + ":" + filename;
        
        // Check cache first
        if (cachedConfigs.containsKey(configKey)) {
            return cachedConfigs.get(configKey);
        }
        
        // Store schema for later use
        configSchemas.put(configKey, schema);
        
        // Get the module's data folder
        File dataFolder = new File("modules/" + module.getName());
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Create configuration file
        File configFile = new File(dataFolder, filename);
        ConfigFormat format = ConfigFormat.fromFilename(filename);
        
        // Load or create the configuration
        Configuration config;
        if (configFile.exists()) {
            config = ConfigurationLoader.load(configFile);
            if (config == null) {
                logger.severe("Failed to load configuration: " + configFile.getPath());
                return null;
            }
            
            // Validate configuration against schema
            validateConfig(config, schema);
        } else {
            config = ConfigurationLoader.create(configFile, format);
            if (config == null) {
                logger.severe("Failed to create configuration: " + configFile.getPath());
                return null;
            }
            
            // Apply default values from schema
            applyDefaults(config, schema);
            config.save();
        }
        
        // Cache the configuration
        cachedConfigs.put(configKey, config);
        
        return config;
    }
    
    /**
     * Loads a configuration for a module without schema validation.
     *
     * @param module The module
     * @param filename The configuration filename
     * @return The loaded configuration
     */
    public static Configuration loadConfiguration(Module module, String filename) {
        String configKey = module.getName() + ":" + filename;
        
        // Check cache first
        if (cachedConfigs.containsKey(configKey)) {
            return cachedConfigs.get(configKey);
        }
        
        // Get the module's data folder
        File dataFolder = new File("modules/" + module.getName());
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Create configuration file
        File configFile = new File(dataFolder, filename);
        ConfigFormat format = ConfigFormat.fromFilename(filename);
        
        // Load or create the configuration
        Configuration config;
        if (configFile.exists()) {
            config = ConfigurationLoader.load(configFile);
        } else {
            config = ConfigurationLoader.create(configFile, format);
            config.save();
        }
        
        if (config == null) {
            logger.severe("Failed to load/create configuration: " + configFile.getPath());
            return null;
        }
        
        // Cache the configuration
        cachedConfigs.put(configKey, config);
        
        return config;
    }
    
    /**
     * Saves a configuration.
     *
     * @param config The configuration to save
     * @return true if successful
     */
    public static boolean saveConfiguration(Configuration config) {
        if (config == null) {
            return false;
        }
        
<<<<<<< HEAD
>>>>>>> 1cd13da (Das ist Dumm)
=======
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        return config.save();
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
     * Implementierung des Configuration Interfaces
     */
    private static class DefaultConfiguration implements Configuration {
        private final Module module;
        private final String filename;
        private File configFile;
        private FileConfiguration fileConfiguration;
        
        public DefaultConfiguration(Module module, String filename) {
            this.module = module;
            this.filename = filename;
            load();
        }
        
        @Override
        public boolean load() {
            try {
                // Modul-Datenordner holen
                File dataFolder = new File("modules/" + module.getName());
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                
                // Konfigurationsdatei erstellen oder laden
                configFile = new File(dataFolder, filename);
                if (!configFile.exists()) {
                    configFile.createNewFile();
                }
                
                fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        public boolean save() {
            if (fileConfiguration == null || configFile == null) {
                return false;
            }
            
            try {
                fileConfiguration.save(configFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        public boolean reload() {
            return load();
        }
        
        @Override
        public void set(String path, Object value) {
            fileConfiguration.set(path, value);
        }
        
        @Override
        public Object get(String path) {
            return fileConfiguration.get(path);
        }
        
        @Override
        public String getString(String path) {
            return fileConfiguration.getString(path);
        }
        
        @Override
        public String getString(String path, String defaultValue) {
            return fileConfiguration.getString(path, defaultValue);
        }
        
        @Override
        public int getInt(String path) {
            return fileConfiguration.getInt(path);
        }
        
        @Override
        public int getInt(String path, int defaultValue) {
            return fileConfiguration.getInt(path, defaultValue);
        }
        
        @Override
        public boolean getBoolean(String path) {
            return fileConfiguration.getBoolean(path);
        }
        
        @Override
        public boolean getBoolean(String path, boolean defaultValue) {
            return fileConfiguration.getBoolean(path, defaultValue);
        }
        
        @Override
        public java.util.List<String> getStringList(String path) {
            return fileConfiguration.getStringList(path);
        }
        
        @Override
        public FileConfiguration getFileConfiguration() {
            return fileConfiguration;
        }
        
        @Override
        public File getFile() {
            return configFile;
        }
=======
=======
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
     * Reloads a configuration.
     *
     * @param module The module
     * @param filename The configuration filename
     * @return The reloaded configuration, or null if not found
     */
    public static Configuration reloadConfiguration(Module module, String filename) {
        String configKey = module.getName() + ":" + filename;
        
        // Remove from cache
        cachedConfigs.remove(configKey);
        
        // Get schema if available
        ConfigSchema schema = configSchemas.get(configKey);
        
        // Reload with or without schema
        if (schema != null) {
            return loadConfiguration(module, filename, schema);
        } else {
            return loadConfiguration(module, filename);
        }
    }
    
    /**
     * Creates a backup of a configuration.
     *
     * @param config The configuration to backup
     * @return The backup file, or null if backup failed
     */
    public static File backupConfiguration(Configuration config) {
        if (config == null) {
            return null;
        }
        
        return migrator.createBackup(config.getFile());
    }
    
    /**
     * Validates a configuration against its schema.
     *
     * @param config The configuration to validate
     * @param schema The schema to validate against
     */
    private static void validateConfig(Configuration config, ConfigSchema schema) {
        // Convert config to map for validation
        Map<String, Object> configMap = new HashMap<>();
        for (String key : config.getKeys(true)) {
            configMap.put(key, config.get(key));
        }
        
        // Validate against schema
        List<ValidationError> errors = schema.validate(configMap);
        
        if (!errors.isEmpty()) {
            logger.warning("Configuration validation errors in " + config.getFile().getName() + ":");
            for (ValidationError error : errors) {
                logger.warning("  - " + error.toString());
            }
            
            // Apply missing defaults for required values
            applyDefaults(config, schema);
            config.save();
        }
    }
    
    /**
     * Applies default values from a schema to a configuration.
     *
     * @param config The configuration to update
     * @param schema The schema with default values
     */
    private static void applyDefaults(Configuration config, ConfigSchema schema) {
        Map<String, Object> defaults = schema.createDefaultConfig();
        
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String path = entry.getKey();
            Object value = entry.getValue();
            
            // Only set if not already present
            if (!config.contains(path)) {
                config.set(path, value);
            }
        }
<<<<<<< HEAD
>>>>>>> 1cd13da (Das ist Dumm)
=======
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
    }
} 