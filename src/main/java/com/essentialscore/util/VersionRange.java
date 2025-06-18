package com.essentialscore.util;

/**
 * Represents a version range specification for module dependencies.
 * Handles version requirements like "&gt;1.0.0", "&gt;=2.0.0", "&lt;3.0.0", etc.
 */
public class VersionRange {
    private final String rawRange;
    private final String operator;
    private final SemanticVersion version;
    
    /**
     * Creates a new VersionRange from a raw range string.
     * 
     * @param range The raw version range string (e.g., ">=1.0.0")
     */
    public VersionRange(String range) {
        this.rawRange = range.trim();
        
        if (rawRange.startsWith(">=")) {
            operator = ">=";
            version = SemanticVersion.parse(rawRange.substring(2).trim());
        } else if (rawRange.startsWith(">")) {
            operator = ">";
            version = SemanticVersion.parse(rawRange.substring(1).trim());
        } else if (rawRange.startsWith("<=")) {
            operator = "<=";
            version = SemanticVersion.parse(rawRange.substring(2).trim());
        } else if (rawRange.startsWith("<")) {
            operator = "<";
            version = SemanticVersion.parse(rawRange.substring(1).trim());
        } else if (rawRange.startsWith("=")) {
            operator = "=";
            version = SemanticVersion.parse(rawRange.substring(1).trim());
        } else if (rawRange.startsWith("^")) {
            operator = "^";
            version = SemanticVersion.parse(rawRange.substring(1).trim());
        } else if (rawRange.startsWith("~")) {
            operator = "~";
            version = SemanticVersion.parse(rawRange.substring(1).trim());
        } else {
            // Default to exact match if no operator is provided
            operator = "=";
            version = SemanticVersion.parse(rawRange);
        }
    }
    
    /**
     * Parses a version range string into a VersionRange object.
     * 
     * @param range The version range string to parse
     * @return A new VersionRange object
     */
    public static VersionRange parse(String range) {
        if (range == null || range.trim().isEmpty() || range.equals("*")) {
            // Special case for "*" meaning any version
            return new VersionRange(">=0.0.0");
        }
        return new VersionRange(range);
    }
    
    /**
     * Checks if the given version satisfies this version range.
     * 
     * @param otherVersion The version to check
     * @return true if the version satisfies this range
     */
    public boolean isSatisfiedBy(String otherVersion) {
        SemanticVersion other = SemanticVersion.parse(otherVersion);
        return isSatisfiedBy(other);
    }
    
    /**
     * Checks if the given version satisfies this version range.
     * 
     * @param other The version to check
     * @return true if the version satisfies this range
     */
    public boolean isSatisfiedBy(SemanticVersion other) {
        switch (operator) {
            case ">=":
                return other.compareTo(version) >= 0;
            case ">":
                return other.compareTo(version) > 0;
            case "<=":
                return other.compareTo(version) <= 0;
            case "<":
                return other.compareTo(version) < 0;
            case "=":
                return other.equals(version);
            case "^":
                // Compatible with, equivalent to >=version and <next major version
                return other.compareTo(version) >= 0 && 
                       other.getMajor() == version.getMajor();
            case "~":
                // Compatible with, equivalent to >=version and <next minor version
                return other.compareTo(version) >= 0 && 
                       other.getMajor() == version.getMajor() &&
                       other.getMinor() == version.getMinor();
            default:
                return false;
        }
    }
    
    /**
     * Gets the raw version range string.
     * 
     * @return The raw version range string
     */
    public String getRawRange() {
        return rawRange;
    }
    
    /**
     * Gets the operator part of the version range.
     * 
     * @return The operator
     */
    public String getOperator() {
        return operator;
    }
    
    /**
     * Gets the version part of the version range.
     * 
     * @return The version
     */
    public SemanticVersion getVersion() {
        return version;
    }
    
    @Override
    public String toString() {
        return rawRange;
    }
} 
