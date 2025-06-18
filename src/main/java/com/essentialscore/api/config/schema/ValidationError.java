package com.essentialscore.api.config.schema;

/**
 * Represents a validation error in a configuration.
 */
public class ValidationError {
    private final String path;
    private final String message;
    
    /**
     * Creates a new validation error.
     *
     * @param path The path to the invalid value
     * @param message The error message
     */
    public ValidationError(String path, String message) {
        this.path = path;
        this.message = message;
    }
    
    /**
     * Gets the path to the invalid value.
     *
     * @return The config path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Gets the error message.
     *
     * @return The error message
     */
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return path + ": " + message;
    }
} 
