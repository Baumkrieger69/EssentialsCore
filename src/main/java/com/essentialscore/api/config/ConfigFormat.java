package com.essentialscore.api.config;

import java.util.Optional;

/**
 * Supported configuration file formats.
 */
public enum ConfigFormat {
    /**
     * YAML format (.yml, .yaml)
     */
    YAML("yml", "yaml"),
    
    /**
     * JSON format (.json)
     */
    JSON("json"),
    
    /**
     * TOML format (.toml)
     */
    TOML("toml"),
    
    /**
     * Java Properties format (.properties)
     */
    PROPERTIES("properties");
    
    private final String[] extensions;
    
    ConfigFormat(String... extensions) {
        this.extensions = extensions;
    }
    
    /**
     * Gets the file extensions associated with this format.
     *
     * @return Array of file extensions without dots
     */
    public String[] getExtensions() {
        return extensions;
    }
    
    /**
     * Detects the format from a filename.
     *
     * @param filename The filename to check
     * @return The detected format, or YAML as default if not detected
     */
    public static ConfigFormat fromFilename(String filename) {
        String lowerFilename = filename.toLowerCase();
        
        for (ConfigFormat format : values()) {
            for (String extension : format.getExtensions()) {
                if (lowerFilename.endsWith("." + extension)) {
                    return format;
                }
            }
        }
        
        // Default to YAML if no extension matches
        return YAML;
    }
    
    /**
     * Gets the primary file extension for this format.
     *
     * @return The primary file extension with dot
     */
    public String getDefaultExtension() {
        return "." + extensions[0];
    }
} 
