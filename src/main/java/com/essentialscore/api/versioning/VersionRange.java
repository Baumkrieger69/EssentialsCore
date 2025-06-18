package com.essentialscore.api.versioning;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a range of versions following semver rules.
 * Supports operators: =, &gt;, &gt;=, &lt;, &lt;=, ~, ^
 * Also supports range combinations with AND (space) and OR (||)
 */
public class VersionRange {
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "\\s*(=|>=|<=|>|<|~|\\^)?\\s*([0-9]+(?:\\.[0-9]+)?(?:\\.[0-9]+)?(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?)\\s*");
    
    private final List<VersionComparator> comparators = new ArrayList<>();
    
    /**
     * Creates a new version range from a range expression.     * Examples:
     * - "1.2.3" (exactly 1.2.3)
     * - "&gt;=1.2.3" (greater than or equal to 1.2.3)
     * - "&gt;=1.2.0 &lt;2.0.0" (greater than or equal to 1.2.0 and less than 2.0.0)
     * - "1.2.3 || 1.3.0" (either 1.2.3 or 1.3.0)
     *
     * @param rangeExpression The range expression
     * @throws IllegalArgumentException if the range expression is invalid
     */
    public VersionRange(String rangeExpression) {
        if (rangeExpression == null || rangeExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Range expression cannot be null or empty");
        }

        parseRangeExpression(rangeExpression);
    }
    
    private void parseRangeExpression(String rangeExpression) {
        // Split by OR operator
        String[] orParts = rangeExpression.split("\\|\\|");
        
        for (String orPart : orParts) {
            orPart = orPart.trim();
            if (orPart.isEmpty()) continue;
            
            // For each OR part, create an AND group
            List<VersionComparator> andGroup = new ArrayList<>();
            
            // Match each comparator in the AND group
            Matcher matcher = RANGE_PATTERN.matcher(orPart);
            while (matcher.find()) {
                String operator = matcher.group(1);
                String versionStr = matcher.group(2);
                
                Version version = new Version(versionStr);
                
                if (operator == null || operator.isEmpty() || operator.equals("=")) {
                    // Exact version match
                    andGroup.add(new ExactVersionComparator(version));
                } else if (operator.equals(">")) {
                    andGroup.add(new GreaterThanComparator(version));
                } else if (operator.equals(">=")) {
                    andGroup.add(new GreaterThanOrEqualComparator(version));
                } else if (operator.equals("<")) {
                    andGroup.add(new LessThanComparator(version));
                } else if (operator.equals("<=")) {
                    andGroup.add(new LessThanOrEqualComparator(version));
                } else if (operator.equals("~")) {
                    // Tilde range (~1.2.3 is >=1.2.3 <1.3.0)
                    andGroup.add(new GreaterThanOrEqualComparator(version));
                    andGroup.add(new LessThanComparator(
                            new Version(version.getMajor(), version.getMinor() + 1, 0, "", "")));
                } else if (operator.equals("^")) {
                    // Caret range (^1.2.3 is >=1.2.3 <2.0.0)
                    andGroup.add(new GreaterThanOrEqualComparator(version));
                    if (version.getMajor() > 0) {
                        andGroup.add(new LessThanComparator(
                                new Version(version.getMajor() + 1, 0, 0, "", "")));
                    } else if (version.getMinor() > 0) {
                        andGroup.add(new LessThanComparator(
                                new Version(0, version.getMinor() + 1, 0, "", "")));
                    } else {
                        andGroup.add(new LessThanComparator(
                                new Version(0, 0, version.getPatch() + 1, "", "")));
                    }
                }
            }
            
            if (andGroup.isEmpty()) {
                throw new IllegalArgumentException("Invalid range expression: " + orPart);
            }
            
            // Add the AND group as a single OR comparator
            comparators.add(new AndComparator(andGroup));
        }
        
        if (comparators.isEmpty()) {
            throw new IllegalArgumentException("Invalid range expression: " + rangeExpression);
        }
    }
    
    /**
     * Checks if a version satisfies this version range.
     *
     * @param version The version to check
     * @return true if the version satisfies the range
     */
    public boolean isSatisfiedBy(Version version) {
        for (VersionComparator comparator : comparators) {
            if (comparator.matches(version)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < comparators.size(); i++) {
            if (i > 0) {
                sb.append(" || ");
            }
            sb.append(comparators.get(i));
        }
        return sb.toString();
    }
    
    // --- Comparator interfaces and implementations ---
    
    private interface VersionComparator {
        boolean matches(Version version);
    }
    
    private static class ExactVersionComparator implements VersionComparator {
        private final Version version;
        
        public ExactVersionComparator(Version version) {
            this.version = version;
        }
        
        @Override
        public boolean matches(Version other) {
            return version.equals(other);
        }
        
        @Override
        public String toString() {
            return "=" + version;
        }
    }
    
    private static class GreaterThanComparator implements VersionComparator {
        private final Version version;
        
        public GreaterThanComparator(Version version) {
            this.version = version;
        }
        
        @Override
        public boolean matches(Version other) {
            return other.compareTo(version) > 0;
        }
        
        @Override
        public String toString() {
            return ">" + version;
        }
    }
    
    private static class GreaterThanOrEqualComparator implements VersionComparator {
        private final Version version;
        
        public GreaterThanOrEqualComparator(Version version) {
            this.version = version;
        }
        
        @Override
        public boolean matches(Version other) {
            return other.compareTo(version) >= 0;
        }
        
        @Override
        public String toString() {
            return ">=" + version;
        }
    }
    
    private static class LessThanComparator implements VersionComparator {
        private final Version version;
        
        public LessThanComparator(Version version) {
            this.version = version;
        }
        
        @Override
        public boolean matches(Version other) {
            return other.compareTo(version) < 0;
        }
        
        @Override
        public String toString() {
            return "<" + version;
        }
    }
    
    private static class LessThanOrEqualComparator implements VersionComparator {
        private final Version version;
        
        public LessThanOrEqualComparator(Version version) {
            this.version = version;
        }
        
        @Override
        public boolean matches(Version other) {
            return other.compareTo(version) <= 0;
        }
        
        @Override
        public String toString() {
            return "<=" + version;
        }
    }
    
    private static class AndComparator implements VersionComparator {
        private final List<VersionComparator> comparators;
        
        public AndComparator(List<VersionComparator> comparators) {
            this.comparators = new ArrayList<>(comparators);
        }
        
        @Override
        public boolean matches(Version version) {
            for (VersionComparator comparator : comparators) {
                if (!comparator.matches(version)) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < comparators.size(); i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(comparators.get(i));
            }
            return sb.toString();
        }
    }
} 
