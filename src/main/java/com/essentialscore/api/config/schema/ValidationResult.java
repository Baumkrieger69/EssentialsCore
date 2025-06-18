package com.essentialscore.api.config.schema;

/**
 * Result of a configuration validation.
 */
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;
    
    /**
     * Creates a valid result.
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }
    
    /**
     * Creates an invalid result with an error message.
     *
     * @param errorMessage The error message
     * @return Invalid validation result
     */
    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
    
    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Checks if the validation was successful.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Gets the error message if validation failed.
     *
     * @return The error message, or null if valid
     */
    public String getErrorMessage() {
        return errorMessage;
    }
} 
