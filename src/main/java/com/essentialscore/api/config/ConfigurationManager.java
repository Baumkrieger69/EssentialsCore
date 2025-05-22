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
    }
} 