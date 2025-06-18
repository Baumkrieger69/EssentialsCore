package com.essentialscore.api.config.schema;

/**
 * Validator for configuration values.
 *
 * @param <T> The type of value to validate
 */
@FunctionalInterface
public interface Validator<T> {
    /**
     * Validates a value.
     *
     * @param value The value to validate
     * @return The validation result
     */
    ValidationResult validate(T value);
    
    /**
     * Creates a validator that requires a value to match a regex pattern.
     *
     * @param pattern The regex pattern
     * @return A validator for strings
     */
    static Validator<String> regex(String pattern) {
        return value -> {
            if (value == null) {
                return ValidationResult.invalid("Value cannot be null");
            }
            if (!value.matches(pattern)) {
                return ValidationResult.invalid("Value does not match pattern: " + pattern);
            }
            return ValidationResult.valid();
        };
    }
    
    /**
     * Creates a validator that requires a value to be in a specific range.
     *
     * @param min The minimum value (inclusive)
     * @param max The maximum value (inclusive)
     * @return A validator for integers
     */
    static Validator<Integer> range(int min, int max) {
        return value -> {
            if (value == null) {
                return ValidationResult.invalid("Value cannot be null");
            }
            if (value < min || value > max) {
                return ValidationResult.invalid("Value must be between " + min + " and " + max);
            }
            return ValidationResult.valid();
        };
    }
    
    /**
     * Creates a validator that requires a value to be one of a set of options.
     *
     * @param options The valid options
     * @return A validator for strings
     */
    static Validator<String> oneOf(String... options) {
        return value -> {
            if (value == null) {
                return ValidationResult.invalid("Value cannot be null");
            }
            for (String option : options) {
                if (option.equals(value)) {
                    return ValidationResult.valid();
                }
            }
            return ValidationResult.invalid("Value must be one of: " + String.join(", ", options));
        };
    }
    
    /**
     * Creates a validator that requires a list to have a specific size.
     *
     * @param minSize The minimum size (inclusive)
     * @param maxSize The maximum size (inclusive)
     * @return A validator for lists
     */
    static <T> Validator<java.util.List<T>> size(int minSize, int maxSize) {
        return value -> {
            if (value == null) {
                return ValidationResult.invalid("List cannot be null");
            }
            int size = value.size();
            if (size < minSize || size > maxSize) {
                return ValidationResult.invalid("List must have between " + minSize + " and " + maxSize + " items");
            }
            return ValidationResult.valid();
        };
    }
} 
