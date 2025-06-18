package com.essentialscore.api.config;

import com.essentialscore.api.config.impl.JsonConfiguration;
import com.essentialscore.api.config.impl.PropertiesConfiguration;
import com.essentialscore.api.config.impl.TomlConfiguration;
import com.essentialscore.api.config.impl.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for loading configurations in different formats.
 */
public class ConfigurationLoader {
    private static final Logger logger = Logger.getLogger(ConfigurationLoader.class.getName());
    
    /**
     * Loads a configuration file in the appropriate format.
     *
     * @param file The configuration file to load
     * @return The loaded configuration, or null if loading failed
     */
    public static Configuration load(File file) {
        if (file == null || !file.exists()) {
            logger.warning("Cannot load configuration, file does not exist");
            return null;
        }
        
        ConfigFormat format = ConfigFormat.fromFilename(file.getName());
        
        try {
            Configuration config;
            
            switch (format) {
                case YAML:
                    config = new YamlConfiguration(file);
                    break;
                case JSON:
                    config = new JsonConfiguration(file);
                    break;
                case TOML:
                    config = new TomlConfiguration(file);
                    break;
                case PROPERTIES:
                    config = new PropertiesConfiguration(file);
                    break;
                default:
                    logger.warning("Unsupported configuration format: " + format);
                    return null;
            }
            
            config.load();
            return config;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading configuration: " + file.getPath(), e);
            return null;
        }
    }
    
    /**
     * Creates a new configuration file in the specified format.
     *
     * @param file The file to create
     * @param format The configuration format
     * @return The created configuration, or null if creation failed
     */
    public static Configuration create(File file, ConfigFormat format) {
        try {
            // Create parent directories if they don't exist
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }
            
            Configuration config;
            
            switch (format) {
                case YAML:
                    config = new YamlConfiguration(file);
                    break;
                case JSON:
                    config = new JsonConfiguration(file);
                    break;
                case TOML:
                    config = new TomlConfiguration(file);
                    break;
                case PROPERTIES:
                    config = new PropertiesConfiguration(file);
                    break;
                default:
                    logger.warning("Unsupported configuration format: " + format);
                    return null;
            }
            
            config.save();
            return config;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating configuration: " + file.getPath(), e);
            return null;
        }
    }
} 
