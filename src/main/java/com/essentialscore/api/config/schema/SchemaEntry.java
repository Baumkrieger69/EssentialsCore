package com.essentialscore.api.config.schema;

/**
 * Represents a configuration schema entry.
 *
 * @param <T> The type of value for this entry
 */
public interface SchemaEntry<T> {
    /**
     * Gets the path to this entry in the config.
     *
     * @return The config path
     */
    String getPath();
    
    /**
     * Gets the default value for this entry.
     *
     * @return The default value
     */
    T getDefaultValue();
    
    /**
     * Gets the description of what this entry does.
     *
     * @return The description
     */
    String getDescription();
    
    /**
     * Checks if this entry is required in the config.
     *
     * @return true if required
     */
    boolean isRequired();
    
    /**
     * Validates a value against this schema entry.
     *
     * @param value The value to validate
     * @return The validation result
     */
    ValidationResult validate(Object value);
    
    /**
     * Gets the type of this entry.
     *
     * @return The entry type class
     */
    Class<T> getType();
} 
