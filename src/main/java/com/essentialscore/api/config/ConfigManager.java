package com.essentialscore.api.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages configuration files for the plugin and its modules.
 */
public class ConfigManager {
    private final Plugin plugin;
    private final Map<String, FileConfiguration> configCache;
    private final Map<String, File> configFiles;

    /**
     * Creates a new configuration manager.
     *
     * @param plugin The plugin
     */
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configCache = new ConcurrentHashMap<>();
        this.configFiles = new ConcurrentHashMap<>();
    }

    /**
     * Gets a configuration file for a module.
     *
     * @param moduleName The module name
     * @return The configuration file
     */
    public File getConfigFile(String moduleName) {
        return configFiles.computeIfAbsent(moduleName, name -> {
            File moduleFolder = new File(plugin.getDataFolder(), "modules/" + name);
            if (!moduleFolder.exists()) {
                moduleFolder.mkdirs();
            }
            return new File(moduleFolder, "config.yml");
        });
    }

    /**
     * Gets a configuration for a module.
     *
     * @param moduleName The module name
     * @return The configuration
     */
    public FileConfiguration getConfig(String moduleName) {
        FileConfiguration config = configCache.get(moduleName);
        if (config == null) {
            config = loadConfig(moduleName);
            configCache.put(moduleName, config);
        }
        return config;
    }

    /**
     * Loads a configuration for a module.
     *
     * @param moduleName The module name
     * @return The loaded configuration
     */
    public FileConfiguration loadConfig(String moduleName) {
        File configFile = getConfigFile(moduleName);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // Check for defaults in plugin jar
        InputStream defaultConfigStream = plugin.getResource("modules/" + moduleName + "/config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }
        
        return config;
    }

    /**
     * Saves a configuration for a module.
     *
     * @param moduleName The module name
     */
    public void saveConfig(String moduleName) {
        FileConfiguration config = configCache.get(moduleName);
        if (config != null) {
            try {
                config.save(getConfigFile(moduleName));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config for module " + moduleName, e);
            }
        }
    }

    /**
     * Saves the default configuration for a module if it doesn't exist.
     *
     * @param moduleName The module name
     */
    public void saveDefaultConfig(String moduleName) {
        File configFile = getConfigFile(moduleName);
        if (!configFile.exists()) {
            InputStream defaultConfigStream = plugin.getResource("modules/" + moduleName + "/config.yml");
            if (defaultConfigStream != null) {
                try {
                    YamlConfiguration.loadConfiguration(
                            new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8))
                            .save(configFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save default config for module " + moduleName, e);
                }
            }
        }
    }

    /**
     * Reloads a configuration for a module.
     *
     * @param moduleName The module name
     * @return The reloaded configuration
     */
    public FileConfiguration reloadConfig(String moduleName) {
        FileConfiguration config = loadConfig(moduleName);
        configCache.put(moduleName, config);
        return config;
    }

    /**
     * Reloads all configurations.
     */
    public void reloadAllConfigs() {
        for (String moduleName : configCache.keySet()) {
            reloadConfig(moduleName);
        }
    }

    /**
     * Clears the configuration cache.
     */
    public void clearCache() {
        configCache.clear();
    }

    /**
     * Gets all module configurations.
     *
     * @return A map of module names to configurations
     */
    public Map<String, FileConfiguration> getAllConfigs() {
        return new HashMap<>(configCache);
    }
} 
