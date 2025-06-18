package com.essentialscore.api.security;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements Role-Based Access Control (RBAC) with granular permissions.
 */
public class RoleBasedAccessControl {
    private static final Logger LOGGER = Logger.getLogger(RoleBasedAccessControl.class.getName());
    
    private final Plugin plugin;
    private final Map<String, Role> roles;
    private final Map<UUID, String> playerRoles;
    private final Map<UUID, Set<String>> playerPermissions;
    private final File rolesFile;
    private final File playerRolesFile;
    
    private boolean initialized;
    
    /**
     * Creates a new RBAC system.
     *
     * @param plugin The plugin
     */
    public RoleBasedAccessControl(Plugin plugin) {
        this.plugin = plugin;
        this.roles = new ConcurrentHashMap<>();
        this.playerRoles = new ConcurrentHashMap<>();
        this.playerPermissions = new ConcurrentHashMap<>();
        this.rolesFile = new File(plugin.getDataFolder(), "roles.yml");
        this.playerRolesFile = new File(plugin.getDataFolder(), "player_roles.yml");
        this.initialized = false;
    }
    
    /**
     * Initializes the RBAC system.
     */
    public void initialize() {
        if (initialized) return;
        
        LOGGER.info("Initializing Role-Based Access Control");
        
        // Create data directory if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load roles and permissions
        loadRoles();
        loadPlayerRoles();
        
        // Create default roles if none exist
        if (roles.isEmpty()) {
            createDefaultRoles();
        }
        
        initialized = true;
    }
    
    /**
     * Saves all data.
     */
    public void saveData() {
        saveRoles();
        savePlayerRoles();
    }
    
    /**
     * Loads roles from disk.
     */
    private void loadRoles() {
        if (!rolesFile.exists()) {
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(rolesFile);
            
            for (String roleId : config.getKeys(false)) {
                ConfigurationSection roleSection = config.getConfigurationSection(roleId);
                
                if (roleSection == null) continue;
                
                String displayName = roleSection.getString("displayName", roleId);
                String description = roleSection.getString("description", "");
                int priority = roleSection.getInt("priority", 0);
                List<String> permissions = roleSection.getStringList("permissions");
                List<String> parents = roleSection.getStringList("parents");
                
                Role role = new Role(roleId, displayName, description, priority);
                role.getPermissions().addAll(permissions);
                role.getParents().addAll(parents);
                
                roles.put(roleId, role);
            }
            
            LOGGER.info("Loaded " + roles.size() + " roles");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading roles", e);
        }
    }
    
    /**
     * Saves roles to disk.
     */
    private void saveRoles() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            for (Role role : roles.values()) {
                ConfigurationSection roleSection = config.createSection(role.getId());
                
                roleSection.set("displayName", role.getDisplayName());
                roleSection.set("description", role.getDescription());
                roleSection.set("priority", role.getPriority());
                roleSection.set("permissions", new ArrayList<>(role.getPermissions()));
                roleSection.set("parents", new ArrayList<>(role.getParents()));
            }
            
            config.save(rolesFile);
            LOGGER.info("Saved " + roles.size() + " roles");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error saving roles", e);
        }
    }
    
    /**
     * Loads player roles from disk.
     */
    private void loadPlayerRoles() {
        if (!playerRolesFile.exists()) {
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerRolesFile);
            
            for (String uuidString : config.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String roleId = config.getString(uuidString);
                    
                    if (roleId != null && !roleId.isEmpty()) {
                        playerRoles.put(uuid, roleId);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Invalid UUID in player roles file: " + uuidString);
                }
            }
            
            // Calculate effective permissions for all players
            for (UUID playerId : playerRoles.keySet()) {
                calculateEffectivePermissions(playerId);
            }
            
            LOGGER.info("Loaded roles for " + playerRoles.size() + " players");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading player roles", e);
        }
    }
    
    /**
     * Saves player roles to disk.
     */
    private void savePlayerRoles() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            for (Map.Entry<UUID, String> entry : playerRoles.entrySet()) {
                config.set(entry.getKey().toString(), entry.getValue());
            }
            
            config.save(playerRolesFile);
            LOGGER.info("Saved roles for " + playerRoles.size() + " players");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error saving player roles", e);
        }
    }
    
    /**
     * Creates default roles.
     */
    private void createDefaultRoles() {
        // Admin role
        Role adminRole = new Role("admin", "Administrator", "Full access to all features", 100);
        adminRole.getPermissions().add("*");
        roles.put(adminRole.getId(), adminRole);
        
        // Moderator role
        Role modRole = new Role("moderator", "Moderator", "Manage players and content", 50);
        modRole.getPermissions().addAll(Arrays.asList(
                "essentials.kick",
                "essentials.ban",
                "essentials.mute",
                "essentials.teleport",
                "essentials.invsee",
                "security.audit.view"
        ));
        roles.put(modRole.getId(), modRole);
        
        // VIP role
        Role vipRole = new Role("vip", "VIP", "Premium features", 25);
        vipRole.getPermissions().addAll(Arrays.asList(
                "essentials.kits.vip",
                "essentials.warp.vip",
                "essentials.fly"
        ));
        roles.put(vipRole.getId(), vipRole);
        
        // Default role
        Role defaultRole = new Role("default", "Player", "Basic permissions", 0);
        defaultRole.getPermissions().addAll(Arrays.asList(
                "essentials.spawn",
                "essentials.home",
                "essentials.tpa"
        ));
        roles.put(defaultRole.getId(), defaultRole);
        
        saveRoles();
        LOGGER.info("Created default roles");
    }
    
    /**
     * Calculates effective permissions for a player.
     *
     * @param playerId The player ID
     */
    private void calculateEffectivePermissions(UUID playerId) {
        Set<String> effectivePermissions = new HashSet<>();
        
        String roleId = playerRoles.get(playerId);
        if (roleId == null) {
            // No role assigned, use default
            roleId = "default";
        }
        
        calculateRolePermissions(roleId, effectivePermissions, new HashSet<>());
        
        // Store effective permissions
        playerPermissions.put(playerId, effectivePermissions);
    }
    
    /**
     * Recursively calculates permissions for a role, including parent roles.
     *
     * @param roleId The role ID
     * @param permissions The permissions set to populate
     * @param processedRoles Roles already processed (to prevent circular dependencies)
     */
    private void calculateRolePermissions(String roleId, Set<String> permissions, Set<String> processedRoles) {
        // Prevent circular dependencies
        if (processedRoles.contains(roleId)) {
            LOGGER.warning("Detected circular dependency in role inheritance: " + roleId);
            return;
        }
        
        Role role = roles.get(roleId);
        if (role == null) {
            LOGGER.warning("Role not found: " + roleId);
            return;
        }
        
        // Mark this role as processed
        processedRoles.add(roleId);
        
        // Process parent roles first
        for (String parentId : role.getParents()) {
            calculateRolePermissions(parentId, permissions, processedRoles);
        }
        
        // Add this role's permissions
        for (String permission : role.getPermissions()) {
            if (permission.equals("*")) {
                // Wildcard permission, add everything
                permissions.add("*");
                return;
            } else if (permission.endsWith(".*")) {
                // Namespace wildcard
                String namespace = permission.substring(0, permission.length() - 2);
                permissions.add(permission);
                
                // Add all existing permissions in this namespace
                for (Role r : roles.values()) {
                    for (String p : r.getPermissions()) {
                        if (p.startsWith(namespace + ".") && !p.endsWith(".*")) {
                            permissions.add(p);
                        }
                    }
                }
            } else {
                // Regular permission
                permissions.add(permission);
            }
        }
    }
    
    /**
     * Checks if a player has a permission.
     *
     * @param playerId The player ID
     * @param permission The permission
     * @return true if the player has the permission
     */
    public boolean hasPermission(UUID playerId, String permission) {
        // Get cached permissions
        Set<String> playerPerms = playerPermissions.get(playerId);
        
        if (playerPerms == null) {
            // Calculate permissions if not cached
            calculateEffectivePermissions(playerId);
            playerPerms = playerPermissions.get(playerId);
            
            if (playerPerms == null) {
                return false;
            }
        }
        
        // Check for wildcard permission
        if (playerPerms.contains("*")) {
            return true;
        }
        
        // Check for exact permission
        if (playerPerms.contains(permission)) {
            return true;
        }
        
        // Check for namespace wildcards
        int lastDot = permission.lastIndexOf('.');
        while (lastDot > 0) {
            String namespace = permission.substring(0, lastDot);
            if (playerPerms.contains(namespace + ".*")) {
                return true;
            }
            lastDot = namespace.lastIndexOf('.');
        }
        
        return false;
    }
    
    /**
     * Checks if a player has a permission.
     *
     * @param player The player
     * @param permission The permission
     * @return true if the player has the permission
     */
    public boolean hasPermission(Player player, String permission) {
        return hasPermission(player.getUniqueId(), permission);
    }
    
    /**
     * Gets a player's role.
     *
     * @param playerId The player ID
     * @return The role, or the default role if not found
     */
    public Role getPlayerRole(UUID playerId) {
        String roleId = playerRoles.get(playerId);
        
        if (roleId == null) {
            return roles.get("default");
        }
        
        Role role = roles.get(roleId);
        return role != null ? role : roles.get("default");
    }
    
    /**
     * Sets a player's role.
     *
     * @param playerId The player ID
     * @param roleId The role ID
     * @return true if the role was set
     */
    public boolean setPlayerRole(UUID playerId, String roleId) {
        if (!roles.containsKey(roleId)) {
            return false;
        }
        
        playerRoles.put(playerId, roleId);
        calculateEffectivePermissions(playerId);
        
        // Save change immediately
        savePlayerRoles();
        
        return true;
    }
    
    /**
     * Gets a role by ID.
     *
     * @param roleId The role ID
     * @return The role, or null if not found
     */
    public Role getRole(String roleId) {
        return roles.get(roleId);
    }
    
    /**
     * Gets all roles.
     *
     * @return The roles
     */
    public Collection<Role> getAllRoles() {
        return new ArrayList<>(roles.values());
    }
    
    /**
     * Creates a new role.
     *
     * @param roleId The role ID
     * @param displayName The display name
     * @param description The description
     * @param priority The priority
     * @return The new role, or null if the ID is already taken
     */
    public Role createRole(String roleId, String displayName, String description, int priority) {
        if (roles.containsKey(roleId)) {
            return null;
        }
        
        Role role = new Role(roleId, displayName, description, priority);
        roles.put(roleId, role);
        
        // Save changes
        saveRoles();
        
        return role;
    }
    
    /**
     * Deletes a role.
     *
     * @param roleId The role ID
     * @return true if the role was deleted
     */
    public boolean deleteRole(String roleId) {
        if (!roles.containsKey(roleId) || roleId.equals("default")) {
            return false;
        }
        
        roles.remove(roleId);
        
        // Update players with this role to default
        for (Map.Entry<UUID, String> entry : playerRoles.entrySet()) {
            if (entry.getValue().equals(roleId)) {
                entry.setValue("default");
                calculateEffectivePermissions(entry.getKey());
            }
        }
        
        // Update role parents
        for (Role role : roles.values()) {
            if (role.getParents().contains(roleId)) {
                role.getParents().remove(roleId);
            }
        }
        
        // Save changes
        saveRoles();
        savePlayerRoles();
        
        return true;
    }
    
    /**
     * Adds a permission to a role.
     *
     * @param roleId The role ID
     * @param permission The permission
     * @return true if the permission was added
     */
    public boolean addPermission(String roleId, String permission) {
        Role role = roles.get(roleId);
        
        if (role == null) {
            return false;
        }
        
        boolean added = role.getPermissions().add(permission);
        
        if (added) {
            // Update affected players
            updateAffectedPlayers(roleId);
            
            // Save changes
            saveRoles();
        }
        
        return added;
    }
    
    /**
     * Removes a permission from a role.
     *
     * @param roleId The role ID
     * @param permission The permission
     * @return true if the permission was removed
     */
    public boolean removePermission(String roleId, String permission) {
        Role role = roles.get(roleId);
        
        if (role == null) {
            return false;
        }
        
        boolean removed = role.getPermissions().remove(permission);
        
        if (removed) {
            // Update affected players
            updateAffectedPlayers(roleId);
            
            // Save changes
            saveRoles();
        }
        
        return removed;
    }
    
    /**
     * Adds a parent role.
     *
     * @param roleId The role ID
     * @param parentId The parent role ID
     * @return true if the parent was added
     */
    public boolean addParent(String roleId, String parentId) {
        Role role = roles.get(roleId);
        Role parent = roles.get(parentId);
        
        if (role == null || parent == null || roleId.equals(parentId)) {
            return false;
        }
        
        // Check for circular dependencies
        Set<String> processedRoles = new HashSet<>();
        processedRoles.add(roleId);
        if (wouldCreateCircularDependency(parentId, processedRoles)) {
            LOGGER.warning("Adding parent " + parentId + " to " + roleId + " would create a circular dependency");
            return false;
        }
        
        boolean added = role.getParents().add(parentId);
        
        if (added) {
            // Update affected players
            updateAffectedPlayers(roleId);
            
            // Save changes
            saveRoles();
        }
        
        return added;
    }
    
    /**
     * Removes a parent role.
     *
     * @param roleId The role ID
     * @param parentId The parent role ID
     * @return true if the parent was removed
     */
    public boolean removeParent(String roleId, String parentId) {
        Role role = roles.get(roleId);
        
        if (role == null) {
            return false;
        }
        
        boolean removed = role.getParents().remove(parentId);
        
        if (removed) {
            // Update affected players
            updateAffectedPlayers(roleId);
            
            // Save changes
            saveRoles();
        }
        
        return removed;
    }
    
    /**
     * Checks if adding a parent role would create a circular dependency.
     *
     * @param roleId The role to check
     * @param processedRoles Roles already processed
     * @return true if a circular dependency would be created
     */
    private boolean wouldCreateCircularDependency(String roleId, Set<String> processedRoles) {
        if (processedRoles.contains(roleId)) {
            return true;
        }
        
        Role role = roles.get(roleId);
        if (role == null) {
            return false;
        }
        
        processedRoles.add(roleId);
        
        for (String parentId : role.getParents()) {
            if (wouldCreateCircularDependency(parentId, processedRoles)) {
                return true;
            }
        }
        
        processedRoles.remove(roleId);
        return false;
    }
    
    /**
     * Updates effective permissions for all players affected by a role change.
     *
     * @param roleId The changed role ID
     */
    private void updateAffectedPlayers(String roleId) {
        // Find all affected roles (this role and roles that inherit from it)
        Set<String> affectedRoles = new HashSet<>();
        affectedRoles.add(roleId);
        
        for (Role role : roles.values()) {
            if (isRoleAffected(role.getId(), roleId, new HashSet<>())) {
                affectedRoles.add(role.getId());
            }
        }
        
        // Update all players with affected roles
        for (Map.Entry<UUID, String> entry : playerRoles.entrySet()) {
            if (affectedRoles.contains(entry.getValue())) {
                calculateEffectivePermissions(entry.getKey());
            }
        }
    }
    
    /**
     * Checks if a role is affected by changes to another role.
     *
     * @param roleId The role to check
     * @param changedRoleId The changed role
     * @param processedRoles Roles already processed
     * @return true if the role is affected
     */
    private boolean isRoleAffected(String roleId, String changedRoleId, Set<String> processedRoles) {
        // Prevent circular dependencies
        if (processedRoles.contains(roleId)) {
            return false;
        }
        
        Role role = roles.get(roleId);
        if (role == null) {
            return false;
        }
        
        // Mark this role as processed
        processedRoles.add(roleId);
        
        // Check if this role inherits from the changed role
        if (role.getParents().contains(changedRoleId)) {
            return true;
        }
        
        // Check parent roles
        for (String parentId : role.getParents()) {
            if (isRoleAffected(parentId, changedRoleId, processedRoles)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets effective permissions for a player.
     *
     * @param playerId The player ID
     * @return The effective permissions
     */
    public Set<String> getEffectivePermissions(UUID playerId) {
        Set<String> permissions = playerPermissions.get(playerId);
        
        if (permissions == null) {
            calculateEffectivePermissions(playerId);
            permissions = playerPermissions.get(playerId);
            
            if (permissions == null) {
                return new HashSet<>();
            }
        }
        
        return new HashSet<>(permissions);
    }
    
    /**
     * Clears permission cache for a player.
     *
     * @param playerId The player ID
     */
    public void clearCache(UUID playerId) {
        playerPermissions.remove(playerId);
    }
    
    /**
     * Clears all permission caches.
     */
    public void clearAllCaches() {
        playerPermissions.clear();
    }
    
    /**
     * Class representing a role.
     */
    public static class Role {
        private final String id;
        private String displayName;
        private String description;
        private int priority;
        private final Set<String> permissions;
        private final Set<String> parents;
        
        /**
         * Creates a new role.
         *
         * @param id The role ID
         * @param displayName The display name
         * @param description The description
         * @param priority The priority
         */
        public Role(String id, String displayName, String description, int priority) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.priority = priority;
            this.permissions = new HashSet<>();
            this.parents = new HashSet<>();
        }
        
        /**
         * Gets the role ID.
         *
         * @return The role ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Gets the display name.
         *
         * @return The display name
         */
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * Sets the display name.
         *
         * @param displayName The display name
         */
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        /**
         * Gets the description.
         *
         * @return The description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Sets the description.
         *
         * @param description The description
         */
        public void setDescription(String description) {
            this.description = description;
        }
        
        /**
         * Gets the priority.
         *
         * @return The priority
         */
        public int getPriority() {
            return priority;
        }
        
        /**
         * Sets the priority.
         *
         * @param priority The priority
         */
        public void setPriority(int priority) {
            this.priority = priority;
        }
        
        /**
         * Gets the permissions.
         *
         * @return The permissions
         */
        public Set<String> getPermissions() {
            return permissions;
        }
        
        /**
         * Gets the parent roles.
         *
         * @return The parent roles
         */
        public Set<String> getParents() {
            return parents;
        }
    }
} 
