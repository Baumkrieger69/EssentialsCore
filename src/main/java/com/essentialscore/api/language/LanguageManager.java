package com.essentialscore.api.language;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages language and localization for the plugin
 */
public class LanguageManager {
    private final Map<String, String> supportedLanguages;
    private String currentLanguage;
    
    public LanguageManager() {
        this.supportedLanguages = new HashMap<>();
        this.currentLanguage = "en";
        
        // Initialize with default supported languages
        supportedLanguages.put("en", "English");
        supportedLanguages.put("de", "Deutsch");
        supportedLanguages.put("fr", "Français");
        supportedLanguages.put("es", "Español");
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
     * Reloads the language configuration
     */
    public void reload() {
        // Reload language files and configurations
        // Implementation would load from language files
    }
    
    /**
     * Gets a localized message
     * 
     * @param key The message key
     * @return The localized message
     */
    public String getMessage(String key) {
        // This would normally load from language files
        return key;
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
        return String.format(message, params);
    }
}
