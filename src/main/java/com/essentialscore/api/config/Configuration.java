package com.essentialscore.api.config;

import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Erweiterte Konfigurationsschnittstelle für Module
 */
public interface Configuration {

    /**
     * Lädt die Konfiguration aus der Datei
     * @return true wenn erfolgreich
     */
    boolean load();
    
    /**
     * Speichert die Konfiguration in die Datei
     * @return true wenn erfolgreich
     */
    boolean save();
    
    /**
     * Lädt die Konfiguration neu
     * @return true wenn erfolgreich
     */
    boolean reload();
    
    /**
     * Setzt einen Wert in die Konfiguration
     * @param path Der Pfad zum Wert
     * @param value Der zu setzende Wert
     */
    void set(String path, Object value);
    
    /**
     * Holt einen Wert aus der Konfiguration
     * @param path Der Pfad zum Wert
     * @return Der Wert oder null
     */
    Object get(String path);
    
    /**
     * Holt einen String aus der Konfiguration
     * @param path Der Pfad zum Wert
     * @return Der String oder null
     */
    String getString(String path);
    
    /**
     * Holt einen String aus der Konfiguration mit Standardwert
     * @param path Der Pfad zum Wert
     * @param defaultValue Der Standardwert
     * @return Der String oder der Standardwert
     */
    String getString(String path, String defaultValue);
    
    /**
     * Holt einen Integer aus der Konfiguration
     * @param path Der Pfad zum Wert
     * @return Der Integer oder 0
     */
    int getInt(String path);
    
    /**
     * Holt einen Integer aus der Konfiguration mit Standardwert
     * @param path Der Pfad zum Wert
     * @param defaultValue Der Standardwert
     * @return Der Integer oder der Standardwert
     */
    int getInt(String path, int defaultValue);
    
    /**
     * Holt einen Boolean aus der Konfiguration
     * @param path Der Pfad zum Wert
     * @return Der Boolean oder false
     */
    boolean getBoolean(String path);
    
    /**
     * Holt einen Boolean aus der Konfiguration mit Standardwert
     * @param path Der Pfad zum Wert
     * @param defaultValue Der Standardwert
     * @return Der Boolean oder der Standardwert
     */
    boolean getBoolean(String path, boolean defaultValue);
    
    /**
     * Holt eine Liste aus der Konfiguration
     * @param path Der Pfad zum Wert
     * @return Die Liste oder eine leere Liste
     */
    List<String> getStringList(String path);
    
    /**
     * Holt die zugrundeliegende FileConfiguration
     * @return Die FileConfiguration
     */
    FileConfiguration getFileConfiguration();
    
    /**
     * Holt die Konfigurationsdatei
     * @return Die Konfigurationsdatei
     */
    File getFile();
} 