package com.essentialscore.api.security;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Security policy class that defines access rules for modules.
 */
public class SecurityPolicy {
    private final String name;
    private final Map<String, Object> rules;
    private final boolean enforced;
    
    /**
     * Creates a new security policy.
     * @param name The policy name
     * @param enforced Whether the policy is enforced
     */
    public SecurityPolicy(String name, boolean enforced) {
        this.name = name;
        this.enforced = enforced;
        this.rules = new HashMap<>();
    }
    
    /**
     * Gets the policy name.
     * @return The policy name
     */
    public String getName() { 
        return name; 
    }
    
    /**
     * Checks if the policy is enforced.
     * @return true if the policy is enforced
     */
    public boolean isEnforced() { 
        return enforced; 
    }
    
    /**
     * Checks if a file operation is allowed.
     * @param file The file to check
     * @param operation The operation type (read, write, execute)
     * @return true if the operation is allowed
     */
    public boolean isFileOperationAllowed(File file, String operation) {
        if (!enforced) {
            return true; // Allow all operations when not enforced
        }
        // Add more sophisticated logic here if needed
        return false; // Default: deny when enforced
    }
    
    /**
     * Checks if a network operation is allowed.
     * @param host The target host
     * @param port The target port
     * @param operation The operation type (connect, listen)
     * @return true if the operation is allowed
     */
    public boolean isNetworkOperationAllowed(String host, int port, String operation) {
        if (!enforced) {
            return true; // Allow all operations when not enforced
        }
        // Add more sophisticated logic here if needed
        return false; // Default: deny when enforced
    }
    
    /**
     * Checks if a plugin interaction is allowed.
     * @param pluginName The name of the plugin to interact with
     * @return true if the interaction is allowed
     */
    public boolean isPluginInteractionAllowed(String pluginName) {
        if (!enforced) {
            return true; // Allow all interactions when not enforced
        }
        // Add more sophisticated logic here if needed
        return false; // Default: deny when enforced
    }
    
    /**
     * Checks if a general operation is allowed.
     * @param operationType The type of operation
     * @param target The target of the operation
     * @return true if the operation is allowed
     */
    public boolean isOperationAllowed(String operationType, String target) {
        if (!enforced) {
            return true; // Allow all operations when not enforced
        }
        // Add more sophisticated logic here if needed
        return false; // Default: deny when enforced
    }
    
    /**
     * Adds a rule to the policy.
     * @param key The rule key
     * @param value The rule value
     */
    public void addRule(String key, Object value) {
        rules.put(key, value);
    }
    
    /**
     * Gets a rule value.
     * @param key The rule key
     * @return The rule value
     */
    public Object getRule(String key) {
        return rules.get(key);
    }
    
    /**
     * Gets all rules.
     * @return A copy of the rules map
     */
    public Map<String, Object> getRules() {
        return new HashMap<>(rules); // Return a copy to prevent modification
    }
}
