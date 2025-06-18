package com.essentialscore.api.config.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema for validating configuration files.
 * Provides strong typing and validation rules for config values.
 */
public class ConfigSchema {
    private final Map<String, SchemaEntry<?>> entries;
    private final Map<String, ConfigSchema> sections;
    
    /**
     * Creates a new empty schema.
     */
    public ConfigSchema() {
        this.entries = new HashMap<>();
        this.sections = new HashMap<>();
    }
    
    /**
     * Adds a string entry to the schema.
     *
     * @param path The config path
     * @param defaultValue The default value
     * @param description Description of what this config value does
     * @return The schema builder for method chaining
     */
    public ConfigSchema addString(String path, String defaultValue, String description) {
        entries.put(path, new StringEntry(path, defaultValue, description));
        return this;
    }
    
    /**
     * Adds a string entry with validation to the schema.
     *
     * @param path The config path
     * @param defaultValue The default value
     * @param description Description of what this config value does
     * @param validator Validator for checking valid values
     * @return The schema builder for method chaining
     */
    public ConfigSchema addString(String path, String defaultValue, String description, Validator<String> validator) {
        entries.put(path, new StringEntry(path, defaultValue, description, validator));
        return this;
    }
    
    /**
     * Adds an integer entry to the schema.
     *
     * @param path The config path
     * @param defaultValue The default value
     * @param description Description of what this config value does
     * @return The schema builder for method chaining
     */
    public ConfigSchema addInteger(String path, int defaultValue, String description) {
        entries.put(path, new IntegerEntry(path, defaultValue, description));
        return this;
    }
    
    /**
     * Adds an integer entry with validation to the schema.
     *
     * @param path The config path
     * @param defaultValue The default value
     * @param description Description of what this config value does
     * @param validator Validator for checking valid values
     * @return The schema builder for method chaining
     */
    public ConfigSchema addInteger(String path, int defaultValue, String description, Validator<Integer> validator) {
        entries.put(path, new IntegerEntry(path, defaultValue, description, validator));
        return this;
    }
    
    /**
     * Adds a boolean entry to the schema.
     *
     * @param path The config path
     * @param defaultValue The default value
     * @param description Description of what this config value does
     * @return The schema builder for method chaining
     */
    public ConfigSchema addBoolean(String path, boolean defaultValue, String description) {
        entries.put(path, new BooleanEntry(path, defaultValue, description));
        return this;
    }
    
    /**
     * Adds a string list entry to the schema.
     *
     * @param path The config path
     * @param defaultValue The default value
     * @param description Description of what this config value does
     * @return The schema builder for method chaining
     */
    public ConfigSchema addStringList(String path, List<String> defaultValue, String description) {
        entries.put(path, new StringListEntry(path, defaultValue, description));
        return this;
    }
    
    /**
     * Adds a string list entry with validation to the schema.
     *
     * @param path The config path
     * @param defaultValue The default value
     * @param description Description of what this config value does
     * @param validator Validator for checking valid values
     * @return The schema builder for method chaining
     */
    public ConfigSchema addStringList(String path, List<String> defaultValue, String description, 
                                      Validator<List<String>> validator) {
        entries.put(path, new StringListEntry(path, defaultValue, description, validator));
        return this;
    }
    
    /**
     * Adds a nested section to the schema.
     *
     * @param path The config path for the section
     * @param schema The schema for the section
     * @return The schema builder for method chaining
     */
    public ConfigSchema addSection(String path, ConfigSchema schema) {
        sections.put(path, schema);
        return this;
    }
    
    /**
     * Gets the schema entry for a path.
     *
     * @param path The config path
     * @return The schema entry, or null if not found
     */
    public SchemaEntry<?> getEntry(String path) {
        // Check if this is a direct entry
        if (entries.containsKey(path)) {
            return entries.get(path);
        }
        
        // Check if this is within a section
        for (Map.Entry<String, ConfigSchema> section : sections.entrySet()) {
            String sectionPath = section.getKey();
            if (path.startsWith(sectionPath + ".")) {
                String subPath = path.substring(sectionPath.length() + 1);
                SchemaEntry<?> subEntry = section.getValue().getEntry(subPath);
                if (subEntry != null) {
                    return subEntry;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Gets all entries in this schema, including nested sections.
     *
     * @return List of all schema entries
     */
    public List<SchemaEntry<?>> getAllEntries() {
        List<SchemaEntry<?>> allEntries = new ArrayList<>(entries.values());
        
        // Add entries from nested sections
        for (Map.Entry<String, ConfigSchema> section : sections.entrySet()) {
            String sectionPath = section.getKey();
            for (SchemaEntry<?> entry : section.getValue().getAllEntries()) {
                allEntries.add(new SchemaEntryWrapper<>(sectionPath + "." + entry.getPath(), entry));
            }
        }
        
        return allEntries;
    }
    
    /**
     * Gets all entries directly in this schema, not including nested sections.
     *
     * @return Map of path to schema entry
     */
    public Map<String, SchemaEntry<?>> getEntries() {
        return new HashMap<>(entries);
    }
    
    /**
     * Gets all nested sections in this schema.
     *
     * @return Map of section path to schema
     */
    public Map<String, ConfigSchema> getSections() {
        return new HashMap<>(sections);
    }
    
    /**
     * Creates a default configuration based on this schema.
     *
     * @return Map representation of default config values
     */
    public Map<String, Object> createDefaultConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Add all direct entries
        for (SchemaEntry<?> entry : entries.values()) {
            config.put(entry.getPath(), entry.getDefaultValue());
        }
        
        // Add all section entries
        for (Map.Entry<String, ConfigSchema> section : sections.entrySet()) {
            String sectionPath = section.getKey();
            Map<String, Object> sectionConfig = section.getValue().createDefaultConfig();
            
            // Add the section
            config.put(sectionPath, sectionConfig);
        }
        
        return config;
    }
    
    /**
     * Validates a configuration against this schema.
     *
     * @param config The configuration to validate
     * @return List of validation errors, empty if valid
     */
    public List<ValidationError> validate(Map<String, Object> config) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Validate all direct entries
        for (SchemaEntry<?> entry : entries.values()) {
            String path = entry.getPath();
            if (config.containsKey(path)) {
                Object value = config.get(path);
                ValidationResult result = entry.validate(value);
                if (!result.isValid()) {
                    errors.add(new ValidationError(path, result.getErrorMessage()));
                }
            } else {
                // Missing required value
                if (entry.isRequired()) {
                    errors.add(new ValidationError(path, "Required value is missing"));
                }
            }
        }
        
        // Validate all section entries
        for (Map.Entry<String, ConfigSchema> section : sections.entrySet()) {
            String sectionPath = section.getKey();
            Object sectionObj = config.get(sectionPath);
            
            if (sectionObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sectionConfig = (Map<String, Object>) sectionObj;
                List<ValidationError> sectionErrors = section.getValue().validate(sectionConfig);
                
                // Add section path prefix to errors
                for (ValidationError error : sectionErrors) {
                    errors.add(new ValidationError(
                        sectionPath + "." + error.getPath(),
                        error.getMessage()
                    ));
                }
            } else if (sectionObj == null) {
                // Missing section, check if any required entries
                boolean hasRequired = section.getValue().getAllEntries().stream()
                    .anyMatch(SchemaEntry::isRequired);
                
                if (hasRequired) {
                    errors.add(new ValidationError(sectionPath, "Required section is missing"));
                }
            } else {
                // Section is not a map
                errors.add(new ValidationError(sectionPath, 
                    "Expected a configuration section, but got " + sectionObj.getClass().getSimpleName()));
            }
        }
        
        return errors;
    }
    
    /**
     * Wraps a schema entry with a different path.
     */
    private static class SchemaEntryWrapper<T> implements SchemaEntry<T> {
        private final String path;
        private final SchemaEntry<T> entry;
        
        public SchemaEntryWrapper(String path, SchemaEntry<T> entry) {
            this.path = path;
            this.entry = entry;
        }
        
        @Override
        public String getPath() {
            return path;
        }
        
        @Override
        public T getDefaultValue() {
            return entry.getDefaultValue();
        }
        
        @Override
        public String getDescription() {
            return entry.getDescription();
        }
        
        @Override
        public boolean isRequired() {
            return entry.isRequired();
        }
        
        @Override
        public ValidationResult validate(Object value) {
            return entry.validate(value);
        }
        
        @Override
        public Class<T> getType() {
            return entry.getType();
        }
    }
} 
