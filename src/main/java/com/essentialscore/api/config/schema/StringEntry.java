package com.essentialscore.api.config.schema;

/**
 * Schema entry for string values.
 */
public class StringEntry extends AbstractSchemaEntry<String> {
    
    /**
     * Creates a new string schema entry.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     */
    public StringEntry(String path, String defaultValue, String description) {
        super(path, defaultValue, description);
    }
    
    /**
     * Creates a new string schema entry with validation.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     * @param validator The validator
     */
    public StringEntry(String path, String defaultValue, String description, Validator<String> validator) {
        super(path, defaultValue, description, validator);
    }
    
    @Override
    protected Validator<String> createDefaultValidator() {
        return value -> ValidationResult.valid();
    }
    
    @Override
    protected String convertValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        // Convert other types to string representation
        return value.toString();
    }
    
    @Override
    public Class<String> getType() {
        return String.class;
    }
} 
