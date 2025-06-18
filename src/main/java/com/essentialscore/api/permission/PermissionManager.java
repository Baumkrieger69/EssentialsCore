package com.essentialscore.api.permission;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;
import java.util.Map;

/**
 * Manages permissions for the plugin and modules.
 */
public interface PermissionManager {
    
    /**
     * Registers a permission.
     *
     * @param permission The permission to register
     * @return True if the permission was registered successfully
     */
    boolean registerPermission(Permission permission);
    
    /**
     * Registers a permission for a module.
     *
     * @param moduleId The module ID
     * @param permission The permission to register
     * @return True if the permission was registered successfully
     */
    boolean registerModulePermission(String moduleId, Permission permission);
    
    /**
     * Registers a permission for a module.
     *
     * @param moduleId The module ID
     * @param name The permission name
     * @param description The permission description
     * @param defaultValue The default permission value
     * @return True if the permission was registered successfully
     */
    boolean registerModulePermission(String moduleId, String name, String description, PermissionDefault defaultValue);
    
    /**
     * Unregisters a permission.
     *
     * @param permissionName The permission name
     * @return True if the permission was unregistered successfully
     */
    boolean unregisterPermission(String permissionName);
    
    /**
     * Unregisters all permissions for a module.
     *
     * @param moduleId The module ID
     * @return The number of permissions unregistered
     */
    int unregisterModulePermissions(String moduleId);
    
    /**
     * Gets all permissions for a module.
     *
     * @param moduleId The module ID
     * @return The module's permissions
     */
    List<Permission> getModulePermissions(String moduleId);
    
    /**
     * Gets all permissions.
     *
     * @return All registered permissions
     */
    List<Permission> getAllPermissions();
    
    /**
     * Gets all module permissions.
     *
     * @return A map of module IDs to permissions
     */
    Map<String, List<Permission>> getAllModulePermissions();
    
    /**
     * Checks if a player has a permission.
     *
     * @param player The player
     * @param permission The permission name
     * @return True if the player has the permission
     */
    boolean hasPermission(Player player, String permission);
    
    /**
     * Checks if a player has a permission for a specific module.
     *
     * @param player The player
     * @param moduleId The module ID
     * @param permission The permission name
     * @return True if the player has the permission
     */
    boolean hasModulePermission(Player player, String moduleId, String permission);
    
    /**
     * Adds a permission to a player.
     *
     * @param player The player
     * @param permission The permission name
     * @return True if the permission was added successfully
     */
    boolean addPermission(Player player, String permission);
    
    /**
     * Removes a permission from a player.
     *
     * @param player The player
     * @param permission The permission name
     * @return True if the permission was removed successfully
     */
    boolean removePermission(Player player, String permission);
    
    /**
     * Gets a player's permissions.
     *
     * @param player The player
     * @return The player's permissions
     */
    List<String> getPlayerPermissions(Player player);
    
    /**
     * Checks if a permission is registered.
     *
     * @param permissionName The permission name
     * @return True if the permission is registered
     */
    boolean isPermissionRegistered(String permissionName);
    
    /**
     * Gets a permission by name.
     *
     * @param permissionName The permission name
     * @return The permission, or null if not found
     */
    Permission getPermission(String permissionName);
} 
