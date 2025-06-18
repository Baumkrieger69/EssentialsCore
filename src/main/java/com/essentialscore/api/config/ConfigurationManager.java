package com.essentialscore.api.config;

import com.essentialscore.api.Module;
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
        return config.save();
    }
    
    /**
     * Lädt eine Konfiguration neu
     * 
     * @param config Die Konfiguration
     * @return true wenn erfolgreich
     */
    public static boolean reloadConfiguration(Configuration config) {
        return config.reload();
    }
    
    /**
     * Erstellt eine neue Konfiguration
     * 
     * @param file Die Konfigurationsdatei
     * @return Die Konfiguration
     */
    public static Configuration createConfiguration(File file) {
        return new DefaultConfiguration(file);
    }
    
    /**
     * Default-Implementierung von Configuration
     */
    private static class DefaultConfiguration implements Configuration {
        private final File file;
        private FileConfiguration config;
        
        public DefaultConfiguration(Module module, String filename) {
            // Da Module keine getDataFolder-Methode hat, verwenden wir eine alternative Methode, um das Datenverzeichnis zu bestimmen
            File moduleDir = new File("modules/" + module.getName());
            if (!moduleDir.exists()) {
                moduleDir.mkdirs();
            }
            this.file = new File(moduleDir, filename);
            this.config = YamlConfiguration.loadConfiguration(file);
        }
        
        public DefaultConfiguration(File file) {
            this.file = file;
            this.config = YamlConfiguration.loadConfiguration(file);
        }
        
        @Override
        public boolean load() {
            try {
                config = YamlConfiguration.loadConfiguration(file);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        public boolean save() {
            try {
                config.save(file);
                return true;
            } catch (IOException e) {
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
        public java.util.List<String> getStringList(String path) {
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
        public java.util.Set<String> getKeys(boolean deep) {
            return config.getKeys(deep);
        }
        
        @Override
        public org.bukkit.configuration.ConfigurationSection getSection(String path) {
            return config.getConfigurationSection(path);
        }
        
        @Override
        public org.bukkit.configuration.ConfigurationSection createSection(String path) {
            return config.createSection(path);
        }
        
        @Override
        public java.util.Map<String, Object> getValues(boolean deep) {
            return config.getValues(deep);
        }
    }
} 
