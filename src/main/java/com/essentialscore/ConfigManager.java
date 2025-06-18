package com.essentialscore;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;

/**
 * Manager für Konfigurationen des ApiCore
 */
public class ConfigManager {
    private final ApiCore apiCore;
    private String messagePrefix;
    private boolean debugMode;
    
    public ConfigManager(ApiCore apiCore) {
        this.apiCore = apiCore;
        this.messagePrefix = "&8[&b&lApiCore&8] &7";
        this.debugMode = false;
    }
    
    /**
     * Lädt die Konfiguration des ApiCore
     */
    public void loadConfiguration() {
        // Grundlegende Konfiguration
        messagePrefix = apiCore.getConfig().getString("message-prefix", "&8[&b&lApiCore&8] &7");
        debugMode = apiCore.getConfig().getBoolean("debug-mode", false);

        // Performance-Optionen
        int processors = Runtime.getRuntime().availableProcessors();
        int defaultThreads = Math.max(2, processors - 1); // Reserviere einen Kern für das Hauptsystem
        
        // Default config template erstellen
        ConfigurationTemplate template = new ConfigurationTemplate()
            // Allgemeine Einstellungen
            .add("message-prefix", "&8[&b&lApiCore&8] &7", "Präfix für Nachrichten des ApiCore")
            .add("debug-mode", false, "Debug-Modus für zusätzliche Logs aktivieren")
            
            // Performance-Einstellungen
            .addSection("performance", "Leistungsoptimierungen")
            .add("performance.thread-pool-size", defaultThreads, "Anzahl der Threads im Thread-Pool")
            .add("performance.use-method-handles", true, "MethodHandles statt Reflection verwenden (schneller)")
            .add("performance.cache-size.methods", 500, "Maximale Anzahl der Cache-Einträge für Methoden")
            .add("performance.cache-size.reflection", 200, "Maximale Anzahl der Cache-Einträge für Reflection")
            
            // Modul-Ladung
            .addSection("performance.module-loading", "Einstellungen zum Laden von Modulen")
            .add("performance.module-loading.parallel", true, "Module parallel laden")
            .add("performance.module-loading.batch-size", 5, "Anzahl der gleichzeitig zu ladenden Module")
            
            // Performance-Monitoring
            .addSection("performance.monitoring", "Überwachung der Leistung")
            .add("performance.monitoring.enabled", false, "Leistungsüberwachung aktivieren")
            .add("performance.monitoring.interval", 300, "Intervall in Sekunden")
            .add("performance.monitoring.log-to-file", false, "Leistungsdaten in Datei speichern")
            
            // Modul-Konfiguration
            .addSection("modules", "Einstellungen für Module")
            .add("modules.hot-reload", false, "Hot-Reload für Module aktivieren")
            .add("modules.auto-update", false, "Automatische Updates für Module")
            .add("modules.dependency-resolution", true, "Abhängigkeiten zwischen Modulen auflösen")
            
            // Nachrichten-Konfiguration
            .addSection("messages", "Nachrichteneinstellungen")
            .add("messages.use-hex-colors", true, "Hexadezimale Farbcodes unterstützen")
            .add("messages.error-verbose", false, "Ausführliche Fehlermeldungen anzeigen");
        
        // Konfiguration mit Template aktualisieren
        updateConfigurationWithTemplate(template);
        
        // Werte aus der Konfiguration laden
        messagePrefix = apiCore.getConfig().getString("message-prefix", messagePrefix);
        debugMode = apiCore.getConfig().getBoolean("debug-mode", debugMode);
        
        // Konfiguration speichern
        apiCore.saveConfig();
    }
    
    /**
     * Aktualisiert die Konfiguration mit einem Template
     */
    private void updateConfigurationWithTemplate(ConfigurationTemplate template) {
        boolean configChanged = false;
        FileConfiguration config = apiCore.getConfig();
        
        // Alle Template-Werte prüfen
        for (Map.Entry<String, Object> entry : template.getDefaults().entrySet()) {
            String path = entry.getKey();
            Object defaultValue = entry.getValue();
            
            if (!config.contains(path)) {
                config.set(path, defaultValue);
                configChanged = true;
                
                // Kommentar hinzufügen, wenn vorhanden
                String comment = template.getComments().get(path);
                if (comment != null && config instanceof YamlConfiguration) {
                    // Hier würde man normalerweise Kommentare hinzufügen, aber das ist in Bukkit's YamlConfiguration 
                    // nicht direkt möglich ohne Erweiterungen. Eine mögliche Lösung wäre ein Custom-YamlConfiguration.
                }
            }
        }
        
        // Legacy-Pfade migrieren
        for (Map.Entry<String, String> entry : template.getMigrations().entrySet()) {
            String oldPath = entry.getKey();
            String newPath = entry.getValue();
            
            if (config.contains(oldPath) && !config.contains(newPath)) {
                config.set(newPath, config.get(oldPath));
                config.set(oldPath, null);
                configChanged = true;
            }
        }
        
        // Config speichern, falls Änderungen vorgenommen wurden
        if (configChanged) {
            apiCore.saveConfig();
        }
    }
    
    /**
     * Hilfsklasse für Konfigurationsvorlagen
     */
    public static class ConfigurationTemplate {
        private final Map<String, Object> defaults = new HashMap<>();
        private final Map<String, String> comments = new HashMap<>();
        private final Map<String, String> migrations = new HashMap<>();
        
        public ConfigurationTemplate add(String path, Object defaultValue, String comment) {
            defaults.put(path, defaultValue);
            if (comment != null) {
                comments.put(path, comment);
            }
            return this;
        }
        
        public ConfigurationTemplate addSection(String path, String comment) {
            comments.put(path, comment);
            return this;
        }
        
        public ConfigurationTemplate migrate(String oldPath, String newPath) {
            migrations.put(oldPath, newPath);
            return this;
        }
        
        public Map<String, Object> getDefaults() {
            return defaults;
        }
        
        public Map<String, String> getComments() {
            return comments;
        }
        
        public Map<String, String> getMigrations() {
            return migrations;
        }
    }
    
    /**
     * Gibt das Nachrichtenprefix zurück
     */
    public String getMessagePrefix() {
        return messagePrefix;
    }
    
    /**
     * Gibt zurück, ob der Debug-Modus aktiviert ist
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Erstellt die benötigten Verzeichnisse
     */
    public void setupDirectories(File dataFolder, File[] dirs) {
        // Hauptverzeichnis erstellen
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        // Andere Verzeichnisse erstellen
        for (File dir : dirs) {
            if (!dir.exists()) {
                dir.mkdir();
            }
        }
    }
    
    /**
     * Lädt eine Moduldatei
     */
    public YamlConfiguration loadModuleConfig(File configFile) throws IOException {
        if (!configFile.exists()) {
            configFile.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.save(configFile);
            return config;
        }
        
        return YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Formatiert eine Nachricht mit Hex-Farbcodes und fügt sie zum Cache hinzu
     */
    public String formatHexMessage(String message, Map<String, String> cache) {
        if (message == null) return "";
        
        // Static Pattern für bessere Performance
        Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

        while (matcher.find()) {
            String group = matcher.group(0);
            matcher.appendReplacement(buffer, ChatColor.of(group).toString());
        }

        String result = ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
        
        // Cache result if it's a reasonable size
        if (message.length() < 128 && cache != null) {
            cache.put(message, result);
        }
        
        return result;
    }
} 
