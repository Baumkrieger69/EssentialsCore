package com.essentialscore.api.config.schema;

/**
 * Abstract base class for schema entries.
 *
 * @param <T> The type of value for this entry
 */
public abstract class AbstractSchemaEntry<T> implements SchemaEntry<T> {
    private final String path;
    private final T defaultValue;
    private final String description;
    private final boolean required;
    private final Validator<T> validator;
    
    /**
     * Creates a new schema entry.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     * @param required Whether this entry is required
     * @param validator The validator
     */
    protected AbstractSchemaEntry(String path, T defaultValue, String description, 
                                boolean required, Validator<T> validator) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.description = description;
        this.required = required;
        this.validator = validator;
    }
    
    /**
     * Creates a new schema entry with a default validator.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     * @param required Whether this entry is required
     */
    protected AbstractSchemaEntry(String path, T defaultValue, String description, boolean required) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.description = description;
        this.required = required;
        this.validator = createDefaultValidator();
    }
    
    /**
     * Creates a new schema entry with a specific validator.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     * @param validator The validator
     */
    protected AbstractSchemaEntry(String path, T defaultValue, String description, Validator<T> validator) {
        this(path, defaultValue, description, true, validator);
    }
    
    /**
     * Creates a new required schema entry with a default validator.
     *
     * @param path The path to this entry
     * @param defaultValue The default value
     * @param description The description
     */
    protected AbstractSchemaEntry(String path, T defaultValue, String description) {
        this(path, defaultValue, description, true);
    }
    
    /**
     * Creates a default validator for this entry type.
     *
     * @return A default validator
     */
    protected abstract Validator<T> createDefaultValidator();
    
    /**
     * Gets the type-safe value from an object.
     *
     * @param value The value to convert
     * @return The converted value, or null if not valid
     */
    protected abstract T convertValue(Object value);
    
    @Override
    public String getPath() {
        return path;
    }
    
    @Override
    public T getDefaultValue() {
        return defaultValue;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isRequired() {
        return required;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        if (value == null) {
            return required ? 
                ValidationResult.invalid("Required value is missing") : 
                ValidationResult.valid();
        }
        
        T convertedValue = convertValue(value);
        if (convertedValue == null) {
            return ValidationResult.invalid("Value is not a valid " + getType().getSimpleName());
        }
        
        return validator.validate(convertedValue);
    }
} 
