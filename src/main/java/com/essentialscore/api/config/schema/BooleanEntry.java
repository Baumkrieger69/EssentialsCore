package com.essentialscore.api.config.schema;

/**
 * Schema entry for boolean values.
 */
public class BooleanEntry extends AbstractSchemaEntry<Boolean> {
    
    /**
     * Creates a new boolean schema entry.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     */
    public BooleanEntry(String path, Boolean defaultValue, String description) {
        super(path, defaultValue, description);
    }
    
    @Override
    protected Validator<Boolean> createDefaultValidator() {
        return value -> ValidationResult.valid();
    }
    
    @Override
    protected Boolean convertValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String strValue = ((String) value).toLowerCase();
            if (strValue.equals("true") || strValue.equals("yes") || strValue.equals("1")) {
                return true;
            }
            if (strValue.equals("false") || strValue.equals("no") || strValue.equals("0")) {
                return false;
            }
        }
        return null;
    }
    
    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }
} 
