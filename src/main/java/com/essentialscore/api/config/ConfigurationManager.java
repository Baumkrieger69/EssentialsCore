package com.essentialscore.api.config;

import com.essentialscore.api.Module;
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
        
        return config.save();
    }
    
    /**
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
    }
} 