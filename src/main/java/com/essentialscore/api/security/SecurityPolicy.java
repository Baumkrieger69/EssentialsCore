package com.essentialscore.api.security;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Defines the security policy for a module, specifying what operations are allowed.
 */
public class SecurityPolicy {
    private static final Logger LOGGER = Logger.getLogger(SecurityPolicy.class.getName());
    
    private final String moduleId;
    private final Map<SecurityPermission, Set<String>> permissions;
    private final Map<SecurityPermission, Set<Pattern>> patternPermissions;
    
    /**
     * Creates a new security policy.
     *
     * @param moduleId The module ID
     */
    public SecurityPolicy(String moduleId) {
        this.moduleId = moduleId;
        this.permissions = new ConcurrentHashMap<>();
        this.patternPermissions = new ConcurrentHashMap<>();
        
        // Initialize permission sets
        for (SecurityPermission permission : SecurityPermission.values()) {
            permissions.put(permission, ConcurrentHashMap.newKeySet());
            patternPermissions.put(permission, ConcurrentHashMap.newKeySet());
        }
    }
    
    /**
     * Gets the module ID.
     *
     * @return The module ID
     */
    public String getModuleId() {
        return moduleId;
    }
    
    /**
     * Adds a permission.
     *
     * @param permission The permission
     * @param target The target
     */
    public void addPermission(SecurityPermission permission, String target) {
        if (target.contains("*")) {
            // Convert glob pattern to regex pattern
            String regex = target.replace(".", "\\.").replace("*", ".*");
            patternPermissions.get(permission).add(Pattern.compile(regex));
        } else {
            permissions.get(permission).add(target);
        }
    }
    
    /**
     * Removes a permission.
     *
     * @param permission The permission
     * @param target The target
     */
    public void removePermission(SecurityPermission permission, String target) {
        permissions.get(permission).remove(target);
        
        // Also remove any pattern permissions that match the exact string
        Set<Pattern> patterns = patternPermissions.get(permission);
        patterns.removeIf(pattern -> pattern.pattern().equals(target.replace(".", "\\.").replace("*", ".*")));
    }
    
    /**
     * Checks if an operation is allowed.
     *
     * @param operationType The operation type
     * @param target The operation target
     * @return true if the operation is allowed
     */
    public boolean isOperationAllowed(String operationType, String target) {
        SecurityPermission permission = SecurityPermission.fromString(operationType);
        
        if (permission == null) {
            return false;
        }
        
        return isPermissionGranted(permission, target);
    }
    
    /**
     * Checks if a file operation is allowed.
     *
     * @param file The file
     * @param operation The operation (read, write, execute)
     * @return true if the file operation is allowed
     */
    public boolean isFileOperationAllowed(File file, String operation) {
        SecurityPermission permission;
        
        switch (operation.toLowerCase()) {
            case "read":
                permission = SecurityPermission.FILE_READ;
                break;
            case "write":
                permission = SecurityPermission.FILE_WRITE;
                break;
            case "execute":
                permission = SecurityPermission.FILE_EXECUTE;
                break;
            default:
                return false;
        }
        
        return isPermissionGranted(permission, file.getAbsolutePath());
    }
    
    /**
     * Checks if a network operation is allowed.
     *
     * @param host The host
     * @param port The port
     * @param operation The operation (connect, listen)
     * @return true if the network operation is allowed
     */
    public boolean isNetworkOperationAllowed(String host, int port, String operation) {
        SecurityPermission permission;
        
        switch (operation.toLowerCase()) {
            case "connect":
                permission = SecurityPermission.NETWORK_CONNECT;
                break;
            case "listen":
                permission = SecurityPermission.NETWORK_LISTEN;
                break;
            default:
                return false;
        }
        
        String target = host + ":" + port;
        return isPermissionGranted(permission, target);
    }
    
    /**
     * Checks if a plugin interaction is allowed.
     *
     * @param pluginName The plugin name
     * @return true if the plugin interaction is allowed
     */
    public boolean isPluginInteractionAllowed(String pluginName) {
        return isPermissionGranted(SecurityPermission.PLUGIN_INTERACT, pluginName);
    }
    
    /**
     * Checks if a permission is granted.
     *
     * @param permission The permission
     * @param target The target
     * @return true if the permission is granted
     */
    private boolean isPermissionGranted(SecurityPermission permission, String target) {
        // Check direct permissions
        if (permissions.get(permission).contains(target)) {
            return true;
        }
        
        // Check pattern permissions
        for (Pattern pattern : patternPermissions.get(permission)) {
            if (pattern.matcher(target).matches()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets all permissions.
     *
     * @return The permissions
     */
    public Map<SecurityPermission, Set<String>> getPermissions() {
        Map<SecurityPermission, Set<String>> result = new HashMap<>();
        
        for (Map.Entry<SecurityPermission, Set<String>> entry : permissions.entrySet()) {
            result.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        return result;
    }
} 