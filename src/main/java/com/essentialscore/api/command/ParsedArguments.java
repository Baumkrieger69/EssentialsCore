package com.essentialscore.api.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and provides access to command arguments.
 * Supports positional arguments, named flags, and quoted strings.
 */
public class ParsedArguments {
    private static final Pattern QUOTED_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");
    private static final Pattern FLAG_PATTERN = Pattern.compile("-{1,2}([\\w-]+)(=(.+))?");

    private final String[] rawArgs;
    private final List<String> arguments;
    private final Map<String, String> namedFlags;
    private final Set<String> booleanFlags;

    /**
     * Parses command arguments.
     *
     * @param args The raw arguments
     */
    public ParsedArguments(String[] args) {
        this.rawArgs = args.clone();
        this.arguments = new ArrayList<>();
        this.namedFlags = new HashMap<>();
        this.booleanFlags = new HashSet<>();
        
        parseArguments();
    }

    /**
     * Gets the number of positional arguments.
     *
     * @return The number of arguments
     */
    public int size() {
        return arguments.size();
    }

    /**
     * Checks if the arguments are empty.
     *
     * @return true if there are no arguments
     */
    public boolean isEmpty() {
        return arguments.isEmpty();
    }

    /**
     * Gets a positional argument at the specified index.
     *
     * @param index The argument index
     * @return The argument, or null if the index is out of bounds
     */
    public String get(int index) {
        return index >= 0 && index < arguments.size() ? arguments.get(index) : null;
    }

    /**
     * Gets a positional argument at the specified index with a default value.
     *
     * @param index The argument index
     * @param defaultValue The default value if the index is out of bounds
     * @return The argument, or the default value if the index is out of bounds
     */
    public String get(int index, String defaultValue) {
        return index >= 0 && index < arguments.size() ? arguments.get(index) : defaultValue;
    }

    /**
     * Gets a positional argument as an integer.
     *
     * @param index The argument index
     * @return The argument as an integer, or null if the index is out of bounds or the argument is not a valid integer
     */
    public Integer getInt(int index) {
        String value = get(index);
        if (value == null) return null;
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets a positional argument as an integer with a default value.
     *
     * @param index The argument index
     * @param defaultValue The default value if the index is out of bounds or the argument is not a valid integer
     * @return The argument as an integer, or the default value if the index is out of bounds or the argument is not a valid integer
     */
    public int getInt(int index, int defaultValue) {
        Integer value = getInt(index);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a positional argument as a double.
     *
     * @param index The argument index
     * @return The argument as a double, or null if the index is out of bounds or the argument is not a valid double
     */
    public Double getDouble(int index) {
        String value = get(index);
        if (value == null) return null;
        
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets a positional argument as a double with a default value.
     *
     * @param index The argument index
     * @param defaultValue The default value if the index is out of bounds or the argument is not a valid double
     * @return The argument as a double, or the default value if the index is out of bounds or the argument is not a valid double
     */
    public double getDouble(int index, double defaultValue) {
        Double value = getDouble(index);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a positional argument as a boolean.
     *
     * @param index The argument index
     * @return The argument as a boolean, or null if the index is out of bounds
     */
    public Boolean getBoolean(int index) {
        String value = get(index);
        if (value == null) return null;
        
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets a positional argument as a boolean with a default value.
     *
     * @param index The argument index
     * @param defaultValue The default value if the index is out of bounds
     * @return The argument as a boolean, or the default value if the index is out of bounds
     */
    public boolean getBoolean(int index, boolean defaultValue) {
        Boolean value = getBoolean(index);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets the value of a named flag.
     *
     * @param name The flag name
     * @return The flag value, or null if the flag is not set
     */
    public String getFlag(String name) {
        return namedFlags.get(name);
    }

    /**
     * Gets the value of a named flag with a default value.
     *
     * @param name The flag name
     * @param defaultValue The default value if the flag is not set
     * @return The flag value, or the default value if the flag is not set
     */
    public String getFlag(String name, String defaultValue) {
        return namedFlags.getOrDefault(name, defaultValue);
    }

    /**
     * Checks if a boolean flag is set.
     *
     * @param name The flag name
     * @return true if the flag is set
     */
    public boolean hasFlag(String name) {
        return booleanFlags.contains(name) || namedFlags.containsKey(name);
    }

    /**
     * Gets all positional arguments.
     *
     * @return An unmodifiable list of arguments
     */
    public List<String> getAll() {
        return Collections.unmodifiableList(arguments);
    }

    /**
     * Gets all named flags.
     *
     * @return An unmodifiable map of flag names to values
     */
    public Map<String, String> getAllFlags() {
        return Collections.unmodifiableMap(namedFlags);
    }

    /**
     * Gets all boolean flags.
     *
     * @return An unmodifiable set of flag names
     */
    public Set<String> getAllBooleanFlags() {
        return Collections.unmodifiableSet(booleanFlags);
    }

    /**
     * Gets a joined string from a range of positional arguments.
     *
     * @param startIndex The start index (inclusive)
     * @return The joined string, or an empty string if the start index is out of bounds
     */
    public String getJoinedFrom(int startIndex) {
        return getJoinedRange(startIndex, arguments.size());
    }

    /**
     * Gets a joined string from a range of positional arguments.
     *
     * @param startIndex The start index (inclusive)
     * @param endIndex The end index (exclusive)
     * @return The joined string, or an empty string if the indices are out of bounds
     */
    public String getJoinedRange(int startIndex, int endIndex) {
        if (startIndex < 0 || startIndex >= arguments.size() || endIndex <= startIndex) {
            return "";
        }
        
        endIndex = Math.min(endIndex, arguments.size());
        
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                builder.append(" ");
            }
            builder.append(arguments.get(i));
        }
        
        return builder.toString();
    }

    /**
     * Parses the raw arguments into positional arguments and flags.
     */
    private void parseArguments() {
        boolean skipFlags = false;
        
        for (int i = 0; i < rawArgs.length; i++) {
            String arg = rawArgs[i];
            
            // After -- we treat everything as a positional argument
            if (arg.equals("--") && !skipFlags) {
                skipFlags = true;
                continue;
            }
            
            // Check if this is a flag
            if (!skipFlags && arg.startsWith("-")) {
                Matcher flagMatcher = FLAG_PATTERN.matcher(arg);
                if (flagMatcher.matches()) {
                    String flagName = flagMatcher.group(1);
                    String flagValue = flagMatcher.group(3);
                    
                    if (flagValue != null) {
                        // Named flag with value (--flag=value or -f=value)
                        namedFlags.put(flagName, flagValue);
                    } else if (i + 1 < rawArgs.length && !rawArgs[i + 1].startsWith("-")) {
                        // Named flag with value as next argument (--flag value or -f value)
                        namedFlags.put(flagName, rawArgs[++i]);
                    } else {
                        // Boolean flag (--flag or -f)
                        booleanFlags.add(flagName);
                    }
                    continue;
                }
            }
            
            // Process quotes
            if (arg.contains("\"")) {
                String combinedArg = arg;
                // Collect arguments until quotes are closed
                while (!countQuotes(combinedArg) && i + 1 < rawArgs.length) {
                    combinedArg += " " + rawArgs[++i];
                }
                
                // Extract quoted content
                Matcher quoteMatcher = QUOTED_PATTERN.matcher(combinedArg);
                while (quoteMatcher.find()) {
                    String match = quoteMatcher.group(1) != null ? quoteMatcher.group(1) : quoteMatcher.group(2);
                    if (match != null && !match.isEmpty()) {
                        arguments.add(match);
                    }
                }
            } else {
                // Regular argument
                arguments.add(arg);
            }
        }
    }
    
    /**
     * Checks if a string has an even number of quotes.
     *
     * @param s The string to check
     * @return true if the string has an even number of quotes
     */
    private boolean countQuotes(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                count++;
            }
        }
        return count % 2 == 0;
    }
    
    /**
     * Gets all positional arguments as an array.
     *
     * @return An array of arguments
     */
    public String[] getAllAsArray() {
        return arguments.toArray(new String[0]);
    }
    
    /**
     * Gets the raw arguments array that was originally passed.
     *
     * @return The raw arguments
     */
    public String[] getRawArgs() {
        return rawArgs.clone();
    }

    @Override
    public String toString() {
        return "ParsedArguments{" +
                "arguments=" + arguments +
                ", namedFlags=" + namedFlags +
                ", booleanFlags=" + booleanFlags +
                '}';
    }
} 
