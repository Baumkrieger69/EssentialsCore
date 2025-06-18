package com.essentialscore.api.config.impl;

import com.essentialscore.api.config.Configuration;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TOML implementation of the Configuration interface.
 */
public class TomlConfiguration implements Configuration {
    private static final Logger logger = Logger.getLogger(TomlConfiguration.class.getName());
    private static final TomlWriter TOML_WRITER = new TomlWriter();
    
    private final File file;
    private Map<String, Object> tomlMap;
    private FileConfiguration yamlConfig;
    
    /**
     * Creates a new TOML configuration.
     *
     * @param file The configuration file
     */
    public TomlConfiguration(File file) {
        this.file = file;
        this.tomlMap = new HashMap<>();
        this.yamlConfig = new YamlConfiguration();
    }
    
    @Override
    public boolean load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                save();
                return true;
            }
            
            // Parse TOML
            Toml toml = new Toml().read(file);
            tomlMap = toml.toMap();
            
            // Convert to YAML for compatibility
            yamlConfig = new YamlConfiguration();
            convertMapToYaml(tomlMap, yamlConfig);
            
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load TOML configuration: " + file.getPath(), e);
            return false;
        }
    }
    
    @Override
    public boolean save() {
        try {
            // Ensure parent directory exists
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            // Convert YAML to map for TOML
            tomlMap = convertYamlToMap(yamlConfig);
            
            // Write TOML
            TOML_WRITER.write(tomlMap, file);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save TOML configuration: " + file.getPath(), e);
            return false;
        }
    }
    
    @Override
    public boolean reload() {
        return load();
    }
    
    @Override
    public void set(String path, Object value) {
        yamlConfig.set(path, value);
    }
    
    @Override
    public Object get(String path) {
        return yamlConfig.get(path);
    }
    
    @Override
    public String getString(String path) {
        return yamlConfig.getString(path);
    }
    
    @Override
    public String getString(String path, String defaultValue) {
        return yamlConfig.getString(path, defaultValue);
    }
    
    @Override
    public int getInt(String path) {
        return yamlConfig.getInt(path);
    }
    
    @Override
    public int getInt(String path, int defaultValue) {
        return yamlConfig.getInt(path, defaultValue);
    }
    
    @Override
    public boolean getBoolean(String path) {
        return yamlConfig.getBoolean(path);
    }
    
    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return yamlConfig.getBoolean(path, defaultValue);
    }
    
    @Override
    public List<String> getStringList(String path) {
        return yamlConfig.getStringList(path);
    }
    
    @Override
    public FileConfiguration getFileConfiguration() {
        return yamlConfig;
    }
    
    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public boolean contains(String path) {
        return yamlConfig.contains(path);
    }
    
    @Override
    public Set<String> getKeys(boolean deep) {
        return yamlConfig.getKeys(deep);
    }
    
    @Override
    public ConfigurationSection getSection(String path) {
        return yamlConfig.getConfigurationSection(path);
    }
    
    @Override
    public ConfigurationSection createSection(String path) {
        return yamlConfig.createSection(path);
    }
    
    @Override
    public Map<String, Object> getValues(boolean deep) {
        return yamlConfig.getValues(deep);
    }
    
    /**
     * Converts a map to YAML configuration.
     *
     * @param map The map
     * @param yaml The YAML configuration
     */
    private void convertMapToYaml(Map<String, Object> map, FileConfiguration yaml) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                // Nested map
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                ConfigurationSection section = yaml.createSection(key);
                
                for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
                    convertMapEntryToYaml(nestedEntry.getKey(), nestedEntry.getValue(), section);
                }
            } else {
                // Regular value
                yaml.set(key, value);
            }
        }
    }
    
    /**
     * Converts a map entry to YAML configuration.
     *
     * @param key The key
     * @param value The value
     * @param section The configuration section
     */
    private void convertMapEntryToYaml(String key, Object value, ConfigurationSection section) {
        if (value instanceof Map) {
            // Nested map
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) value;
            ConfigurationSection nestedSection = section.createSection(key);
            
            for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
                convertMapEntryToYaml(entry.getKey(), entry.getValue(), nestedSection);
            }
        } else {
            // Regular value
            section.set(key, value);
        }
    }
    
    /**
     * Converts YAML configuration to a map for TOML.
     *
     * @param yaml The YAML configuration
     * @return The map
     */
    private Map<String, Object> convertYamlToMap(FileConfiguration yaml) {
        Map<String, Object> map = new HashMap<>();
        
        for (String key : yaml.getKeys(false)) {
            Object value = yaml.get(key);
            
            if (value instanceof ConfigurationSection) {
                // Configuration section
                ConfigurationSection section = (ConfigurationSection) value;
                map.put(key, convertYamlSectionToMap(section));
            } else {
                // Regular value
                map.put(key, value);
            }
        }
        
        return map;
    }
    
    /**
     * Converts a YAML configuration section to a map.
     *
     * @param section The configuration section
     * @return The map
     */
    private Map<String, Object> convertYamlSectionToMap(ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();
        
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            
            if (value instanceof ConfigurationSection) {
                // Nested section
                ConfigurationSection nestedSection = (ConfigurationSection) value;
                map.put(key, convertYamlSectionToMap(nestedSection));
            } else {
                // Regular value
                map.put(key, value);
            }
        }
        
        return map;
    }
} 
