package com.essentialscore.api.module;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages permissions for modules.
 */
public class ModulePermissionManager {
    private final Plugin plugin;
    private final PluginManager pluginManager;
    private final Map<String, List<Permission>> modulePermissions;
    
    /**
     * Creates a new module permission manager.
     *
     * @param plugin The plugin
     */
    public ModulePermissionManager(Plugin plugin) {
        this.plugin = plugin;
        this.pluginManager = plugin.getServer().getPluginManager();
        this.modulePermissions = new ConcurrentHashMap<>();
    }
    
    /**
     * Registers a permission for a module.
     *
     * @param moduleId The module ID
     * @param permission The permission to register
     * @return True if the permission was registered successfully
     */
    public boolean registerPermission(String moduleId, Permission permission) {
        if (moduleId == null || permission == null) {
            return false;
        }
        
        try {
            pluginManager.addPermission(permission);
            modulePermissions.computeIfAbsent(moduleId, k -> new ArrayList<>()).add(permission);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register permission: " + permission.getName());
            return false;
        }
    }
    
    /**
     * Registers a permission for a module.
     *
     * @param moduleId The module ID
     * @param name The permission name
     * @param description The permission description
     * @param defaultValue The default permission value
     * @return True if the permission was registered successfully
     */
    public boolean registerPermission(String moduleId, String name, String description, PermissionDefault defaultValue) {
        Permission permission = new Permission(name, description, defaultValue);
        return registerPermission(moduleId, permission);
    }
    
    /**
     * Registers a permission for a module with children.
     *
     * @param moduleId The module ID
     * @param name The permission name
     * @param description The permission description
     * @param defaultValue The default permission value
     * @param children The permission children
     * @return True if the permission was registered successfully
     */
    public boolean registerPermission(String moduleId, String name, String description, 
                                     PermissionDefault defaultValue, Map<String, Boolean> children) {
        Permission permission = new Permission(name, description, defaultValue, children);
        return registerPermission(moduleId, permission);
    }
    
    /**
     * Unregisters all permissions for a module.
     *
     * @param moduleId The module ID
     * @return The number of permissions unregistered
     */
    public int unregisterModulePermissions(String moduleId) {
        if (moduleId == null || !modulePermissions.containsKey(moduleId)) {
            return 0;
        }
        
        int count = 0;
        List<Permission> permissions = modulePermissions.get(moduleId);
        
        for (Permission permission : permissions) {
            try {
                pluginManager.removePermission(permission);
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to unregister permission: " + permission.getName());
            }
        }
        
        modulePermissions.remove(moduleId);
        return count;
    }
    
    /**
     * Unregisters a specific permission for a module.
     *
     * @param moduleId The module ID
     * @param permissionName The permission name
     * @return True if the permission was unregistered successfully
     */
    public boolean unregisterPermission(String moduleId, String permissionName) {
        if (moduleId == null || permissionName == null || !modulePermissions.containsKey(moduleId)) {
            return false;
        }
        
        List<Permission> permissions = modulePermissions.get(moduleId);
        Permission permissionToRemove = null;
        
        for (Permission permission : permissions) {
            if (permission.getName().equals(permissionName)) {
                permissionToRemove = permission;
                break;
            }
        }
        
        if (permissionToRemove != null) {
            try {
                pluginManager.removePermission(permissionToRemove);
                permissions.remove(permissionToRemove);
                return true;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to unregister permission: " + permissionName);
            }
        }
        
        return false;
    }
    
    /**
     * Gets all permissions for a module.
     *
     * @param moduleId The module ID
     * @return The module's permissions
     */
    public List<Permission> getModulePermissions(String moduleId) {
        return modulePermissions.getOrDefault(moduleId, new ArrayList<>());
    }
    
    /**
     * Gets all module permissions.
     *
     * @return A map of module IDs to permissions
     */
    public Map<String, List<Permission>> getAllModulePermissions() {
        Map<String, List<Permission>> result = new HashMap<>();
        
        for (Map.Entry<String, List<Permission>> entry : modulePermissions.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        
        return result;
    }
    
    /**
     * Checks if a player has a permission.
     *
     * @param player The player
     * @param permissionName The permission name
     * @return True if the player has the permission
     */
    public boolean hasPermission(Player player, String permissionName) {
        return player != null && player.hasPermission(permissionName);
    }
    
    /**
     * Checks if a player has a permission for a specific module.
     *
     * @param player The player
     * @param moduleId The module ID
     * @param permissionName The permission name
     * @return True if the player has the permission
     */
    public boolean hasModulePermission(Player player, String moduleId, String permissionName) {
        if (player == null || moduleId == null || permissionName == null) {
            return false;
        }
        
        // Check for module-specific permission
        String fullPermission = moduleId + "." + permissionName;
        return player.hasPermission(fullPermission);
    }
} 
