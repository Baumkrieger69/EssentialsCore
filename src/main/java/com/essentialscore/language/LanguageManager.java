package com.essentialscore.language;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Modernes Sprachsystem für EssentialsCore
 * Lädt Sprach-YML-Dateien und bietet coole Nachrichten-Formatierung
 */
public class LanguageManager {
    private static final Logger LOGGER = Logger.getLogger(LanguageManager.class.getName());
    
    private final Plugin plugin;
    private final Map<String, YamlConfiguration> languages;
    private String currentLanguage;
    private YamlConfiguration currentConfig;
    
    // Standard-Sprachen
    private static final String[] SUPPORTED_LANGUAGES = {
        "en_US", "de_DE", "es_ES", "fr_FR", "it_IT", "pt_BR", "ru_RU", "zh_CN"
    };
    
    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        this.languages = new HashMap<>();
        this.currentLanguage = "en_US";
        
        loadLanguages();
    }
    
    /**
     * Lädt alle verfügbaren Sprachen
     */
    private void loadLanguages() {
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // Standard-Sprachdateien aus JAR extrahieren
        for (String lang : SUPPORTED_LANGUAGES) {
            String fileName = lang + ".yml";
            File langFile = new File(langDir, fileName);
            
            if (!langFile.exists()) {
                plugin.saveResource("languages/" + fileName, false);
            }
            
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                // Defaults aus JAR laden falls verfügbar
                InputStream defConfigStream = plugin.getResource("languages/" + fileName);
                if (defConfigStream != null) {
                    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                    config.setDefaults(defConfig);
                }
                
                languages.put(lang, config);
                LOGGER.info("Sprache geladen: " + lang);
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Laden der Sprache " + lang + ": " + e.getMessage());
            }
        }
        
        // Aktuelle Sprache setzen
        setLanguage(plugin.getConfig().getString("language", "en_US"));
    }
    
    /**
     * Setzt die aktuelle Sprache
     */
    public boolean setLanguage(String language) {
        if (languages.containsKey(language)) {
            this.currentLanguage = language;
            this.currentConfig = languages.get(language);
            plugin.getConfig().set("language", language);
            plugin.saveConfig();
            LOGGER.info("Sprache geändert zu: " + language);
            return true;
        }
        return false;
    }
    
    /**
     * Holt eine formatierte Nachricht
     */
    public String getMessage(String key, Object... args) {
        if (currentConfig == null) {
            return "§c[Missing Language Config] " + key;
        }
        
        String message = currentConfig.getString(key);
        if (message == null) {
            // Fallback zu Englisch
            YamlConfiguration englishConfig = languages.get("en_US");
            if (englishConfig != null) {
                message = englishConfig.getString(key);
            }
            if (message == null) {
                return "§c[Missing Translation] " + key;
            }
        }
        
        // Argumente ersetzen
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        
        return formatMessage(message);
    }
    
    /**
     * Formatiert eine Nachricht mit coolen Effekten
     */
    private String formatMessage(String message) {
        // Hex-Farben und Standard-Farben unterstützen
        message = message.replace("&", "§");
        
        // Hex-Farben verarbeiten (#RRGGBB format)
        message = message.replaceAll("#([A-Fa-f0-9]{6})", "§x§$1");
        message = message.replaceAll("§x§([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])", 
            "§x§$1§$2§$3§$4§$5§$6");
        
        // Spezielle Effekte
        message = message.replace("<gradient>", "§6");
        message = message.replace("</gradient>", "§r");
        message = message.replace("<rainbow>", "§c");
        message = message.replace("</rainbow>", "§r");
        message = message.replace("<bold>", "§l");
        message = message.replace("</bold>", "§r");
        message = message.replace("<italic>", "§o");
        message = message.replace("</italic>", "§r");
        
        return message;
    }
    
    /**
     * Holt das coole Präfix für ApiCore-Nachrichten
     */
    public String getPrefix() {
        return getMessage("prefix");
    }
    
    /**
     * Sendet eine formatierte Nachricht an einen CommandSender
     */
    public void sendMessage(org.bukkit.command.CommandSender sender, String key, Object... args) {
        sender.sendMessage(getPrefix() + getMessage(key, args));
    }
    
    /**
     * Sendet eine Erfolgs-Nachricht
     */
    public void sendSuccess(org.bukkit.command.CommandSender sender, String key, Object... args) {
        sender.sendMessage(getPrefix() + "§a" + getMessage(key, args));
    }
    
    /**
     * Sendet eine Fehler-Nachricht
     */
    public void sendError(org.bukkit.command.CommandSender sender, String key, Object... args) {
        sender.sendMessage(getPrefix() + "§c" + getMessage(key, args));
    }
    
    /**
     * Sendet eine Warn-Nachricht
     */
    public void sendWarning(org.bukkit.command.CommandSender sender, String key, Object... args) {
        sender.sendMessage(getPrefix() + "§e" + getMessage(key, args));
    }
    
    /**
     * Sendet eine Info-Nachricht
     */
    public void sendInfo(org.bukkit.command.CommandSender sender, String key, Object... args) {
        sender.sendMessage(getPrefix() + "§b" + getMessage(key, args));
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES.clone();
    }
    
    /**
     * Lädt alle Sprachen neu
     */
    public void reload() {
        languages.clear();
        loadLanguages();
        LOGGER.info("Sprachsystem neu geladen");
    }
}
