package com.essentialscore.api.security;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages security permissions for modules.
 */
public class PermissionManager {
    private static final Logger LOGGER = Logger.getLogger(PermissionManager.class.getName());
    
    private final Plugin plugin;
    private final Map<String, Set<String>> modulePermissions;
    private final Map<String, Map<String, List<String>>> userPermissions;
    private final Map<String, Map<String, List<String>>> groupPermissions;
    private final File permissionsFile;
    
    /**
     * Creates a new permission manager.
     *
     * @param plugin The EssentialsCore plugin
     */
    public PermissionManager(Plugin plugin) {
        this.plugin = plugin;
        this.modulePermissions = new ConcurrentHashMap<>();
        this.userPermissions = new ConcurrentHashMap<>();
        this.groupPermissions = new ConcurrentHashMap<>();
        this.permissionsFile = new File(plugin.getDataFolder(), "security/permissions.yml");
    }
    
    /**
     * Initializes the permission manager.
     */
    public void initialize() {
        LOGGER.info("Initializing permission manager...");
        
        // Create permissions file if it doesn't exist
        if (!permissionsFile.exists()) {
            permissionsFile.getParentFile().mkdirs();
            saveDefaultPermissions();
        }
        
        // Load permissions
        loadPermissions();
        
        LOGGER.info("Permission manager initialized");
    }
    
    /**
     * Shuts down the permission manager.
     */
    public void shutdown() {
        LOGGER.info("Shutting down permission manager...");
        
        // Save permissions
        savePermissions();
        
        // Clear maps
        modulePermissions.clear();
        userPermissions.clear();
        groupPermissions.clear();
        
        LOGGER.info("Permission manager shut down");
    }
    
    /**
     * Grants a permission to a module.
     *
     * @param moduleId The module ID
     * @param permission The permission
     */
    public void grantModulePermission(String moduleId, String permission) {
        modulePermissions.computeIfAbsent(moduleId, k -> ConcurrentHashMap.newKeySet()).add(permission);
        savePermissions();
    }
    
    /**
     * Revokes a permission from a module.
     *
     * @param moduleId The module ID
     * @param permission The permission
     */
    public void revokeModulePermission(String moduleId, String permission) {
        Set<String> permissions = modulePermissions.get(moduleId);
        
        if (permissions != null) {
            permissions.remove(permission);
            savePermissions();
        }
    }
    
    /**
     * Checks if a module has a permission.
     *
     * @param moduleId The module ID
     * @param permission The permission
     * @return true if the module has the permission
     */
    public boolean hasModulePermission(String moduleId, String permission) {
        Set<String> permissions = modulePermissions.get(moduleId);
        
        if (permissions == null) {
            return false;
        }
        
        return permissions.contains(permission) || permissions.contains("*");
    }
    
    /**
     * Grants a permission to a user for a module.
     *
     * @param player The player
     * @param moduleId The module ID
     * @param permission The permission
     */
    public void grantUserPermission(Player player, String moduleId, String permission) {
        grantUserPermission(player.getUniqueId().toString(), moduleId, permission);
    }
    
    /**
     * Grants a permission to a user for a module.
     *
     * @param userId The user ID
     * @param moduleId The module ID
     * @param permission The permission
     */
    public void grantUserPermission(String userId, String moduleId, String permission) {
        userPermissions
            .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(moduleId, k -> new ArrayList<>())
            .add(permission);
        
        savePermissions();
    }
    
    /**
     * Revokes a permission from a user for a module.
     *
     * @param player The player
     * @param moduleId The module ID
     * @param permission The permission
     */
    public void revokeUserPermission(Player player, String moduleId, String permission) {
        revokeUserPermission(player.getUniqueId().toString(), moduleId, permission);
    }
    
    /**
     * Revokes a permission from a user for a module.
     *
     * @param userId The user ID
     * @param moduleId The module ID
     * @param permission The permission
     */
    public void revokeUserPermission(String userId, String moduleId, String permission) {
        Map<String, List<String>> modulePerms = userPermissions.get(userId);
        
        if (modulePerms != null) {
            List<String> permissions = modulePerms.get(moduleId);
            
            if (permissions != null) {
                permissions.remove(permission);
                savePermissions();
            }
        }
    }
    
    /**
     * Checks if a user has a permission for a module.
     *
     * @param player The player
     * @param moduleId The module ID
     * @param permission The permission
     * @return true if the user has the permission
     */
    public boolean hasUserPermission(Player player, String moduleId, String permission) {
        return hasUserPermission(player.getUniqueId().toString(), moduleId, permission);
    }
    
    /**
     * Checks if a user has a permission for a module.
     *
     * @param userId The user ID
     * @param moduleId The module ID
     * @param permission The permission
     * @return true if the user has the permission
     */
    public boolean hasUserPermission(String userId, String moduleId, String permission) {
        Map<String, List<String>> modulePerms = userPermissions.get(userId);
        
        if (modulePerms == null) {
            return false;
        }
        
        List<String> permissions = modulePerms.get(moduleId);
        
        if (permissions == null) {
            return false;
        }
        
        return permissions.contains(permission) || permissions.contains("*");
    }
    
    /**
     * Grants a permission to a group for a module.
     *
     * @param group The group name
     * @param moduleId The module ID
     * @param permission The permission
     */
    public void grantGroupPermission(String group, String moduleId, String permission) {
        groupPermissions
            .computeIfAbsent(group, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(moduleId, k -> new ArrayList<>())
            .add(permission);
        
        savePermissions();
    }
    
    /**
     * Revokes a permission from a group for a module.
     *
     * @param group The group name
     * @param moduleId The module ID
     * @param permission The permission
     */
    public void revokeGroupPermission(String group, String moduleId, String permission) {
        Map<String, List<String>> modulePerms = groupPermissions.get(group);
        
        if (modulePerms != null) {
            List<String> permissions = modulePerms.get(moduleId);
            
            if (permissions != null) {
                permissions.remove(permission);
                savePermissions();
            }
        }
    }
    
    /**
     * Checks if a group has a permission for a module.
     *
     * @param group The group name
     * @param moduleId The module ID
     * @param permission The permission
     * @return true if the group has the permission
     */
    public boolean hasGroupPermission(String group, String moduleId, String permission) {
        Map<String, List<String>> modulePerms = groupPermissions.get(group);
        
        if (modulePerms == null) {
            return false;
        }
        
        List<String> permissions = modulePerms.get(moduleId);
        
        if (permissions == null) {
            return false;
        }
        
        return permissions.contains(permission) || permissions.contains("*");
    }
    
    /**
     * Saves the default permissions configuration.
     */
    private void saveDefaultPermissions() {
        LOGGER.info("Creating default permissions file...");
        
        FileConfiguration config = new YamlConfiguration();
        
        // Add default console permissions
        config.set("users.console.core.permissions", List.of("*"));
        
        // Add default admin group permissions
        config.set("groups.admin.core.permissions", List.of("*"));
        
        try {
            config.save(permissionsFile);
            LOGGER.info("Default permissions file created");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save default permissions file", e);
        }
    }
    
    /**
     * Loads permissions from the permissions file.
     */
    private void loadPermissions() {
        LOGGER.info("Loading permissions...");
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(permissionsFile);
        
        // Load module permissions
        ConfigurationSection modulesSection = config.getConfigurationSection("modules");
        
        if (modulesSection != null) {
            for (String moduleId : modulesSection.getKeys(false)) {
                List<String> permissions = config.getStringList("modules." + moduleId + ".permissions");
                
                if (permissions != null && !permissions.isEmpty()) {
                    Set<String> permSet = modulePermissions.computeIfAbsent(moduleId, k -> ConcurrentHashMap.newKeySet());
                    permSet.addAll(permissions);
                }
            }
        }
        
        // Load user permissions
        ConfigurationSection usersSection = config.getConfigurationSection("users");
        
        if (usersSection != null) {
            for (String userId : usersSection.getKeys(false)) {
                ConfigurationSection userModulesSection = config.getConfigurationSection("users." + userId);
                
                if (userModulesSection != null) {
                    for (String moduleId : userModulesSection.getKeys(false)) {
                        List<String> permissions = config.getStringList("users." + userId + "." + moduleId + ".permissions");
                        
                        if (permissions != null && !permissions.isEmpty()) {
                            userPermissions
                                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                                .put(moduleId, new ArrayList<>(permissions));
                        }
                    }
                }
            }
        }
        
        // Load group permissions
        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        
        if (groupsSection != null) {
            for (String group : groupsSection.getKeys(false)) {
                ConfigurationSection groupModulesSection = config.getConfigurationSection("groups." + group);
                
                if (groupModulesSection != null) {
                    for (String moduleId : groupModulesSection.getKeys(false)) {
                        List<String> permissions = config.getStringList("groups." + group + "." + moduleId + ".permissions");
                        
                        if (permissions != null && !permissions.isEmpty()) {
                            groupPermissions
                                .computeIfAbsent(group, k -> new ConcurrentHashMap<>())
                                .put(moduleId, new ArrayList<>(permissions));
                        }
                    }
                }
            }
        }
        
        LOGGER.info("Loaded permissions for " + modulePermissions.size() + " modules, " + 
                    userPermissions.size() + " users, and " + groupPermissions.size() + " groups");
    }
    
    /**
     * Saves permissions to the permissions file.
     */
    private void savePermissions() {
        LOGGER.fine("Saving permissions...");
        
        FileConfiguration config = new YamlConfiguration();
        
        // Save module permissions
        for (Map.Entry<String, Set<String>> entry : modulePermissions.entrySet()) {
            String moduleId = entry.getKey();
            Set<String> permissions = entry.getValue();
            
            if (!permissions.isEmpty()) {
                config.set("modules." + moduleId + ".permissions", new ArrayList<>(permissions));
            }
        }
        
        // Save user permissions
        for (Map.Entry<String, Map<String, List<String>>> userEntry : userPermissions.entrySet()) {
            String userId = userEntry.getKey();
            Map<String, List<String>> modulePerms = userEntry.getValue();
            
            for (Map.Entry<String, List<String>> moduleEntry : modulePerms.entrySet()) {
                String moduleId = moduleEntry.getKey();
                List<String> permissions = moduleEntry.getValue();
                
                if (!permissions.isEmpty()) {
                    config.set("users." + userId + "." + moduleId + ".permissions", permissions);
                }
            }
        }
        
        // Save group permissions
        for (Map.Entry<String, Map<String, List<String>>> groupEntry : groupPermissions.entrySet()) {
            String group = groupEntry.getKey();
            Map<String, List<String>> modulePerms = groupEntry.getValue();
            
            for (Map.Entry<String, List<String>> moduleEntry : modulePerms.entrySet()) {
                String moduleId = moduleEntry.getKey();
                List<String> permissions = moduleEntry.getValue();
                
                if (!permissions.isEmpty()) {
                    config.set("groups." + group + "." + moduleId + ".permissions", permissions);
                }
            }
        }
        
        try {
            config.save(permissionsFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save permissions file", e);
        }
    }
    
    /**
     * Gets all module permissions.
     *
     * @return The module permissions
     */
    public Map<String, Set<String>> getModulePermissions() {
        Map<String, Set<String>> result = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : modulePermissions.entrySet()) {
            result.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        return result;
    }
    
    /**
     * Gets all user permissions.
     *
     * @return The user permissions
     */
    public Map<String, Map<String, List<String>>> getUserPermissions() {
        Map<String, Map<String, List<String>>> result = new HashMap<>();
        
        for (Map.Entry<String, Map<String, List<String>>> entry : userPermissions.entrySet()) {
            Map<String, List<String>> modulePerms = new HashMap<>();
            
            for (Map.Entry<String, List<String>> moduleEntry : entry.getValue().entrySet()) {
                modulePerms.put(moduleEntry.getKey(), new ArrayList<>(moduleEntry.getValue()));
            }
            
            result.put(entry.getKey(), modulePerms);
        }
        
        return result;
    }
    
    /**
     * Gets all group permissions.
     *
     * @return The group permissions
     */
    public Map<String, Map<String, List<String>>> getGroupPermissions() {
        Map<String, Map<String, List<String>>> result = new HashMap<>();
        
        for (Map.Entry<String, Map<String, List<String>>> entry : groupPermissions.entrySet()) {
            Map<String, List<String>> modulePerms = new HashMap<>();
            
            for (Map.Entry<String, List<String>> moduleEntry : entry.getValue().entrySet()) {
                modulePerms.put(moduleEntry.getKey(), new ArrayList<>(moduleEntry.getValue()));
            }
            
            result.put(entry.getKey(), modulePerms);
        }
        
        return result;
    }
    
    /**
     * Gets the plugin instance
     * 
     * @return The plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }
}
