package com.essentialscore.api.config.impl;

import com.essentialscore.api.config.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * YAML implementation of the Configuration interface.
 */
public class YamlConfiguration implements Configuration {
    private static final Logger logger = Logger.getLogger(YamlConfiguration.class.getName());
    
    private final File file;
    private FileConfiguration config;
    
    /**
     * Creates a new YAML configuration.
     *
     * @param file The configuration file
     */
    public YamlConfiguration(File file) {
        this.file = file;
        this.config = new org.bukkit.configuration.file.YamlConfiguration();
    }
    
    @Override
    public boolean load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            
            config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load YAML configuration: " + file.getPath(), e);
            return false;
        }
    }
    
    @Override
    public boolean save() {
        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save YAML configuration: " + file.getPath(), e);
            return false;
        }
    }
    
    @Override
    public boolean reload() {
        return load();
    }
    
    @Override
    public void set(String path, Object value) {
        config.set(path, value);
    }
    
    @Override
    public Object get(String path) {
        return config.get(path);
    }
    
    @Override
    public String getString(String path) {
        return config.getString(path);
    }
    
    @Override
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    @Override
    public int getInt(String path) {
        return config.getInt(path);
    }
    
    @Override
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    @Override
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }
    
    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    @Override
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    @Override
    public FileConfiguration getFileConfiguration() {
        return config;
    }
    
    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public boolean contains(String path) {
        return config.contains(path);
    }
    
    @Override
    public Set<String> getKeys(boolean deep) {
        return config.getKeys(deep);
    }
    
    @Override
    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }
    
    @Override
    public ConfigurationSection createSection(String path) {
        return config.createSection(path);
    }
    
    @Override
    public Map<String, Object> getValues(boolean deep) {
        return config.getValues(deep);
    }
} 
