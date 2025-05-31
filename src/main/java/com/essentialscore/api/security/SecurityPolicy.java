package com.essentialscore.api.security;

import java.util.HashMap;
import java.util.Map;

public class SecurityPolicy {
    private final String name;
    private final Map<String, Object> rules;
    private final boolean enforced;
    
    public SecurityPolicy(String name, boolean enforced) {
        this.name = name;
        this.enforced = enforced;
        this.rules = new HashMap<>();
    }
    
    public String getName() { return name; }
    public boolean isEnforced() { return enforced; }
    
    // Füge fehlende Methoden hinzu
    public boolean isFileOperationAllowed(java.io.File file, String operation) {
        return enforced ? false : true; // Default: Erlaube alle Operationen wenn nicht erzwungen
    }
    
    public boolean isNetworkOperationAllowed(String host, int port, String operation) {
        return enforced ? false : true; // Default: Erlaube alle Operationen wenn nicht erzwungen
    }
    
    public boolean isPluginInteractionAllowed(String pluginName) {
        return enforced ? false : true; // Default: Erlaube alle Operationen wenn nicht erzwungen
    }
    
    public boolean isOperationAllowed(String operationType, String target) {
        return enforced ? false : true; // Default: Erlaube alle Operationen wenn nicht erzwungen
    }
    
    // Methods to use the rules map
    public void addRule(String key, Object value) {
        rules.put(key, value);
    }
    
    public Object getRule(String key) {
        return rules.get(key);
    }
    
    public Map<String, Object> getRules() {
        return new HashMap<>(rules); // Return a copy to prevent modification
    }
}
