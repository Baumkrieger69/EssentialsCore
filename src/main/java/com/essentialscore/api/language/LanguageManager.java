package com.essentialscore.api.language;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages language and localization for the plugin
 */
public class LanguageManager {
    private final Plugin plugin;
    private final Map<String, String> supportedLanguages;
    private final Map<String, FileConfiguration> languageConfigs;
    private String currentLanguage;
    private static final Logger LOGGER = Logger.getLogger(LanguageManager.class.getName());
    
    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        this.supportedLanguages = new HashMap<>();
        this.languageConfigs = new HashMap<>();
        this.currentLanguage = "en_US";
        
        // Initialize with default supported languages
        supportedLanguages.put("en_US", "English (US)");
        supportedLanguages.put("de_DE", "Deutsch (Deutschland)");
        
        // Load language files
        loadLanguageFiles();
    }
    
    /**
     * Loads all language files from the plugin resources
     */
    private void loadLanguageFiles() {
        File languageDir = new File(plugin.getDataFolder(), "languages");
        if (!languageDir.exists()) {
            languageDir.mkdirs();
        }
        
        // Load each supported language
        for (String langCode : supportedLanguages.keySet()) {
            loadLanguageFile(langCode);
        }
    }
    
    /**
     * Loads a specific language file
     * 
     * @param langCode The language code to load
     */
    private void loadLanguageFile(String langCode) {
        try {
            File langFile = new File(plugin.getDataFolder(), "languages/" + langCode + ".yml");
            FileConfiguration config;
            
            if (!langFile.exists()) {
                // Create from plugin resources
                plugin.saveResource("languages/" + langCode + ".yml", false);
            }
            
            config = YamlConfiguration.loadConfiguration(langFile);
            
            // Load defaults from plugin resources
            InputStream defConfigStream = plugin.getResource("languages/" + langCode + ".yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
                config.setDefaults(defConfig);
            }
            
            languageConfigs.put(langCode, config);
            plugin.getLogger().info("Loaded language file: " + langCode);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load language file: " + langCode, e);
        }
    }
    
    /**
     * Gets the current language
     * 
     * @return The current language code
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Sets the current language
     * 
     * @param languageCode The language code to set
     * @return true if the language was set successfully
     */
    public boolean setLanguage(String languageCode) {
        if (supportedLanguages.containsKey(languageCode)) {
            this.currentLanguage = languageCode;
            plugin.getLogger().info("Language changed to: " + languageCode);
            return true;
        }
        return false;
    }
    
    /**
     * Gets all supported languages
     * 
     * @return A set of supported language codes
     */
    public Set<String> getSupportedLanguages() {
        return supportedLanguages.keySet();
    }
    
    /**
     * Gets the display name for a language code
     * 
     * @param langCode The language code
     * @return The display name
     */
    public String getLanguageDisplayName(String langCode) {
        return supportedLanguages.getOrDefault(langCode, langCode);
    }
    
    /**
     * Reloads all language files
     */
    public void reload() {
        languageConfigs.clear();
        loadLanguageFiles();
        plugin.getLogger().info("Language files reloaded");
    }
    
    /**
     * Gets a localized message
     * 
     * @param key The message key (dot-separated path)
     * @return The localized message
     */
    public String getMessage(String key) {
        FileConfiguration config = languageConfigs.get(currentLanguage);
        if (config == null) {
            config = languageConfigs.get("en_US"); // Fallback to English
        }
        
        if (config != null) {
            String message = config.getString(key, key);
            // Replace separator placeholder
            if (message.contains("{separator}")) {
                String separator = config.getString("separator", "");
                message = message.replace("{separator}", separator);
            }
            return message;
        }
        
        return key; // Return key if no config found
    }
      /**
     * Gets a localized message with parameters
     * 
     * @param key The message key
     * @param params The parameters to substitute
     * @return The localized message with substituted parameters
     */
    public String getMessage(String key, Object... params) {
        String message = getMessage(key);
        if (params != null && params.length > 0) {
            try {
                return java.text.MessageFormat.format(message, params);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to format message: " + key + " with params: " + java.util.Arrays.toString(params));
                return message;
            }
        }
        return message;
    }    /**
     * Formats a message with color codes including hex colors
     * 
     * @param message The message to format
     * @return The formatted message
     */
    public String formatMessage(String message) {
        if (message == null) return "";
        
        try {
            // Use Bungee's ChatColor for both hex and legacy colors
            // First, handle hex colors (#RRGGBB) using Bungee API
            message = java.util.regex.Pattern.compile("#([A-Fa-f0-9]{6})").matcher(message)
                .replaceAll(matchResult -> {
                    try {
                        return net.md_5.bungee.api.ChatColor.of("#" + matchResult.group(1)).toString();
                    } catch (Exception e) {
                        // Fallback to manual conversion if Bungee ChatColor.of fails
                        String hex = matchResult.group(1);
                        StringBuilder sb = new StringBuilder("§x");
                        for (char c : hex.toCharArray()) {
                            sb.append("§").append(c);
                        }
                        return sb.toString();
                    }
                });
            
            // Then handle standard color codes (&)
            return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
            
        } catch (Exception e) {
            // Ultimate fallback - manual processing
            // Handle hex colors manually
            message = java.util.regex.Pattern.compile("#([A-Fa-f0-9]{6})").matcher(message)
                .replaceAll(matchResult -> {
                    String hex = matchResult.group(1);
                    StringBuilder sb = new StringBuilder("§x");
                    for (char c : hex.toCharArray()) {
                        sb.append("§").append(c);
                    }
                    return sb.toString();
                });
            
            // Handle legacy color codes
            return message.replaceAll("&([0-9a-fk-or])", "§$1");
        }
    }
      /**
     * Sends a formatted message to a command sender
     * 
     * @param sender The command sender
     * @param key The message key
     * @param args The message arguments
     */
    public void sendMessage(CommandSender sender, String key, Object... args) {
        String message = getMessage(key, args);
        String formatted = formatMessage(message);
        
        // Try to use the enhanced message sending for better hex support
        try {
            sendFormattedMessage(sender, message);
        } catch (Exception e) {
            // Fallback to standard formatting
            sender.sendMessage(formatted);
        }
    }
    
    /**
     * Sends a success message to a command sender
     * 
     * @param sender The command sender
     * @param key The message key
     * @param args The message arguments
     */
    public void sendSuccess(CommandSender sender, String key, Object... args) {
        String message = getMessage("general.success", getMessage(key, args));
        sender.sendMessage(formatMessage(message));
    }
    
    /**
     * Sends an error message to a command sender
     * 
     * @param sender The command sender
     * @param key The message key
     * @param args The message arguments
     */
    public void sendError(CommandSender sender, String key, Object... args) {
        String message = getMessage("general.error", getMessage(key, args));
        sender.sendMessage(formatMessage(message));
    }
    
    /**
     * Sends an info message to a command sender
     * 
     * @param sender The command sender
     * @param key The message key  
     * @param args The message arguments
     */
    public void sendInfo(CommandSender sender, String key, Object... args) {
        String message = getMessage("general.info", getMessage(key, args));
        sender.sendMessage(formatMessage(message));
    }
    
    /**
     * Sends a warning message to a command sender
     * 
     * @param sender The command sender
     * @param key The message key
     * @param args The message arguments
     */
    public void sendWarning(CommandSender sender, String key, Object... args) {
        String message = getMessage("general.warning", getMessage(key, args));
        sender.sendMessage(formatMessage(message));
    }
    
    /**
     * Gets a list of strings from the language file
     * 
     * @param key The key for the list
     * @return The list of strings
     */
    public List<String> getMessageList(String key) {
        FileConfiguration config = languageConfigs.get(currentLanguage);
        if (config == null) {
            config = languageConfigs.get("en_US");
        }
        
        if (config != null) {
            return config.getStringList(key);
        }
        
        return new java.util.ArrayList<>();
    }
    
    /**
     * Checks if a message key exists
     * 
     * @param key The message key to check
     * @return true if the key exists
     */
    public boolean hasMessage(String key) {
        FileConfiguration config = languageConfigs.get(currentLanguage);
        if (config == null) {
            config = languageConfigs.get("en_US");
        }
        
        return config != null && config.contains(key);
    }
    
    /**
     * Sends a formatted message using modern Component API when available
     * 
     * @param sender The command sender
     * @param message The message to send
     */
    private void sendFormattedMessage(CommandSender sender, String message) {
        try {
            // Try to use modern Component API for better hex color support
            if (sender instanceof Player) {
                Player player = (Player) sender;
                
                // Check if we have access to Adventure API
                try {
                    // Use reflection to check for Component support
                    Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                    Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
                    
                    Object miniMessage = miniMessageClass.getMethod("miniMessage").invoke(null);
                    
                    // Convert our color format to MiniMessage format
                    String miniMessageFormat = message
                        .replaceAll("#([A-Fa-f0-9]{6})", "<#$1>")
                        .replaceAll("&([0-9a-fk-or])", "<$1>");
                    
                    Object component = miniMessageClass.getMethod("deserialize", String.class)
                        .invoke(miniMessage, miniMessageFormat);
                    
                    // Send using Adventure API
                    player.getClass().getMethod("sendMessage", componentClass)
                        .invoke(player, component);
                    
                    return;
                } catch (Exception e) {
                    // Adventure API not available, fall back to legacy
                }
            }
            
            // Fallback to standard formatted message
            sender.sendMessage(formatMessage(message));
            
        } catch (Exception e) {
            // Ultimate fallback - send raw message
            sender.sendMessage(message);
        }
    }
    
    /**
     * Debug method to test hex color formatting
     * 
     * @param testMessage The message to test
     * @return The formatted message
     */
    public String debugFormatMessage(String testMessage) {
        LOGGER.info("Debug: Original message: " + testMessage);
        String formatted = formatMessage(testMessage);
        LOGGER.info("Debug: Formatted message: " + formatted);
        return formatted;
    }
}
