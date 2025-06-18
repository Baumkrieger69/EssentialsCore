package com.essentialscore.api.integration.permissions;

import com.essentialscore.api.integration.AbstractPluginIntegration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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
 * Integration with Bukkit's permission system.
 */
public class PermissionIntegration extends AbstractPluginIntegration {
    private final Map<String, Permission> registeredPermissions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> modulePermissions = new ConcurrentHashMap<>();
    
    /**
     * Creates a new permission integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public PermissionIntegration(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    protected void onInitialize() {
        // Nothing to initialize for permission integration
    }
    
    @Override
    protected void onShutdown() {
        // Unregister all permissions
        unregisterAllPermissions();
    }
    
    @Override
    public String getName() {
        return "Bukkit Permissions";
    }
    
    /**
     * Registers a permission.
     *
     * @param permission The permission node
     * @param description The permission description
     * @param defaultValue The default permission value
     * @param moduleName The module name
     */
    public void registerPermission(String permission, String description, PermissionDefault defaultValue, String moduleName) {
        try {
            PluginManager pluginManager = Bukkit.getPluginManager();
            
            // Check if permission already exists
            if (pluginManager.getPermission(permission) != null) {
                logger.warning("Permission already exists: " + permission);
                return;
            }
            
            // Create the permission
            Permission perm = new Permission(permission, description, defaultValue);
            pluginManager.addPermission(perm);
            
            // Add to our maps
            registeredPermissions.put(permission, perm);
            
            // Add to module permissions map
            modulePermissions.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(permission);
            
            logger.info("Registered permission: " + permission + " for module: " + moduleName);
        } catch (Exception e) {
            logger.warning("Failed to register permission: " + permission + " - " + e.getMessage());
        }
    }
    
    /**
     * Unregisters a permission.
     *
     * @param permission The permission node
     */
    public void unregisterPermission(String permission) {
        try {
            PluginManager pluginManager = Bukkit.getPluginManager();
            Permission perm = pluginManager.getPermission(permission);
            
            if (perm != null) {
                pluginManager.removePermission(perm);
                registeredPermissions.remove(permission);
                
                // Remove from module permissions
                for (Map.Entry<String, List<String>> entry : modulePermissions.entrySet()) {
                    entry.getValue().remove(permission);
                }
                
                logger.info("Unregistered permission: " + permission);
            }
        } catch (Exception e) {
            logger.warning("Failed to unregister permission: " + permission + " - " + e.getMessage());
        }
    }
    
    /**
     * Unregisters all permissions for a module.
     *
     * @param moduleName The module name
     */
    public void unregisterModulePermissions(String moduleName) {
        List<String> permissions = modulePermissions.getOrDefault(moduleName, new ArrayList<>());
        
        for (String permission : new ArrayList<>(permissions)) {
            unregisterPermission(permission);
        }
        
        modulePermissions.remove(moduleName);
        logger.info("Unregistered all permissions for module: " + moduleName);
    }
    
    /**
     * Unregisters all permissions.
     */
    public void unregisterAllPermissions() {
        try {
            PluginManager pluginManager = Bukkit.getPluginManager();
            
            for (Permission permission : new ArrayList<>(registeredPermissions.values())) {
                pluginManager.removePermission(permission);
            }
            
            registeredPermissions.clear();
            modulePermissions.clear();
            
            logger.info("Unregistered all permissions");
        } catch (Exception e) {
            logger.warning("Failed to unregister all permissions: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a player has a permission.
     *
     * @param player The player
     * @param permission The permission to check
     * @return true if the player has the permission
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        try {
            return player.hasPermission(permission);
        } catch (Exception e) {
            logger.warning("Failed to check permission: " + permission + " for player: " + player.getName());
            return false;
        }
    }
    
    /**
     * Checks if a command sender has a permission.
     *
     * @param sender The command sender
     * @param permission The permission to check
     * @return true if the sender has the permission
     */
    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null || permission == null) {
            return false;
        }
        
        try {
            return sender.hasPermission(permission);
        } catch (Exception e) {
            logger.warning("Failed to check permission: " + permission + " for sender: " + sender.getName());
            return false;
        }
    }
    
    /**
     * Checks if an offline player has a permission.
     *
     * @param player The offline player
     * @param permission The permission to check
     * @return true if the player has the permission, or false if the player is not online or doesn't have the permission
     */
    public boolean hasPermission(OfflinePlayer player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer != null) {
            return hasPermission(onlinePlayer, permission);
        }
        
        return false;
    }
    
    /**
     * Gets all registered permissions.
     *
     * @return Map of permission nodes to Permission objects
     */
    public Map<String, Permission> getRegisteredPermissions() {
        return new HashMap<>(registeredPermissions);
    }
    
    /**
     * Gets all permissions for a module.
     *
     * @param moduleName The module name
     * @return List of permission nodes
     */
    public List<String> getModulePermissions(String moduleName) {
        return new ArrayList<>(modulePermissions.getOrDefault(moduleName, new ArrayList<>()));
    }
    
    /**
     * Creates a module permission node.
     *
     * @param moduleName The module name
     * @param permissionName The permission name
     * @return The full permission node
     */
    public String createModulePermission(String moduleName, String permissionName) {
        return "essentialscore." + moduleName.toLowerCase() + "." + permissionName;
    }
} 
