package com.essentialscore.api.config.schema;

/**
 * Schema entry for integer values.
 */
public class IntegerEntry extends AbstractSchemaEntry<Integer> {
    
    /**
     * Creates a new integer schema entry.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     */
    public IntegerEntry(String path, Integer defaultValue, String description) {
        super(path, defaultValue, description);
    }
    
    /**
     * Creates a new integer schema entry with validation.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     * @param validator The validator
     */
    public IntegerEntry(String path, Integer defaultValue, String description, Validator<Integer> validator) {
        super(path, defaultValue, description, validator);
    }
    
    @Override
    protected Validator<Integer> createDefaultValidator() {
        return value -> ValidationResult.valid();
    }
    
    @Override
    protected Integer convertValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }
} 
