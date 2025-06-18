package com.essentialscore.api.config.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Schema entry for string list values.
 */
public class StringListEntry extends AbstractSchemaEntry<List<String>> {
    
    /**
     * Creates a new string list schema entry.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     */
    public StringListEntry(String path, List<String> defaultValue, String description) {
        super(path, defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>(), description);
    }
    
    /**
     * Creates a new string list schema entry with validation.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     * @param validator The validator
     */
    public StringListEntry(String path, List<String> defaultValue, String description, Validator<List<String>> validator) {
        super(path, defaultValue != null ? new ArrayList<>(defaultValue) : new ArrayList<>(), description, validator);
    }
    
    @Override
    protected Validator<List<String>> createDefaultValidator() {
        return value -> ValidationResult.valid();
    }
    
    @Override
    protected List<String> convertValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> result = new ArrayList<>();
            
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            
            return result;
        }
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            List<String> result = new ArrayList<>();
            
            for (Object item : collection) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            
            return result;
        }
        if (value instanceof String) {
            // Handle single string as a list with one item
            List<String> result = new ArrayList<>();
            result.add((String) value);
            return result;
        }
        
        return null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Class<List<String>> getType() {
        return (Class<List<String>>) (Class<?>) List.class;
    }
} 
