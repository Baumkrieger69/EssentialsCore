package com.essentialscore.api.integration.permissions;

import com.essentialscore.api.integration.AbstractPluginDependentIntegration;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Integration with Vault permissions.
 */
public class VaultPermissionIntegration extends AbstractPluginDependentIntegration {
    private Permission vaultPermission;
    
    /**
     * Creates a new Vault permission integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public VaultPermissionIntegration(Plugin plugin) {
        super(plugin, "Vault");
    }
    
    @Override
    protected void onPluginInitialize() {
        // Get Vault permissions
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        
        if (rsp == null) {
            throw new IllegalStateException("No Vault permission provider found");
        }
        
        vaultPermission = rsp.getProvider();
        
        if (vaultPermission == null) {
            throw new IllegalStateException("Vault permission provider is null");
        }
        
        logger.info("Using Vault permission provider: " + vaultPermission.getName());
    }
    
    @Override
    protected void onPluginShutdown() {
        vaultPermission = null;
    }
    
    @Override
    public String getName() {
        return "Vault Permissions";
    }
    
    /**
     * Gets the plugin instance.
     *
     * @return The plugin instance
     */
    @Override
    public Plugin getPlugin() {
        return super.getPlugin();
    }
    
    /**
     * Gets the Vault permission provider.
     *
     * @return The Vault permission provider
     */
    public Permission getVaultPermission() {
        return vaultPermission;
    }
    
    /**
     * Checks if a player has a permission.
     *
     * @param player The player
     * @param permission The permission to check
     * @return true if the player has the permission
     */
    public boolean hasPermission(Player player, String permission) {
        if (!isAvailable() || player == null || permission == null) {
            return false;
        }
        
        try {
            return vaultPermission.has(player, permission);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check Vault permission: " + permission + " for player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Checks if a player has a permission in a world.
     *
     * @param player The player
     * @param permission The permission to check
     * @param world The world name
     * @return true if the player has the permission in the world
     */
    public boolean hasPermission(Player player, String permission, String world) {
        if (!isAvailable() || player == null || permission == null) {
            return false;
        }
        
        try {
            return vaultPermission.has(player, permission);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check Vault permission: " + permission + " for player: " + player.getName() + " in world: " + world, e);
            return false;
        }
    }
    
    /**
     * Checks if an offline player has a permission.
     *
     * @param player The offline player
     * @param permission The permission to check
     * @return true if the player has the permission
     */
    public boolean hasPermission(OfflinePlayer player, String permission) {
        if (!isAvailable() || player == null || permission == null) {
            return false;
        }
        
        try {
            if (player.isOnline() && player.getPlayer() != null) {
                return vaultPermission.has(player.getPlayer(), permission);
            } else {
                return vaultPermission.playerHas(null, player, permission);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check Vault permission: " + permission + " for offline player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Adds a permission to a player.
     *
     * @param player The player
     * @param permission The permission to add
     * @return true if the permission was added
     */
    public boolean addPermission(Player player, String permission) {
        if (!isAvailable() || player == null || permission == null) {
            return false;
        }
        
        try {
            return vaultPermission.playerAdd(player, permission);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to add Vault permission: " + permission + " to player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Removes a permission from a player.
     *
     * @param player The player
     * @param permission The permission to remove
     * @return true if the permission was removed
     */
    public boolean removePermission(Player player, String permission) {
        if (!isAvailable() || player == null || permission == null) {
            return false;
        }
        
        try {
            return vaultPermission.playerRemove(player, permission);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to remove Vault permission: " + permission + " from player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Adds a player to a group.
     *
     * @param player The player
     * @param group The group
     * @return true if the player was added to the group
     */
    public boolean addGroup(Player player, String group) {
        if (!isAvailable() || player == null || group == null) {
            return false;
        }
        
        try {
            return vaultPermission.playerAddGroup(player, group);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to add player: " + player.getName() + " to group: " + group, e);
            return false;
        }
    }
    
    /**
     * Removes a player from a group.
     *
     * @param player The player
     * @param group The group
     * @return true if the player was removed from the group
     */
    public boolean removeGroup(Player player, String group) {
        if (!isAvailable() || player == null || group == null) {
            return false;
        }
        
        try {
            return vaultPermission.playerRemoveGroup(player, group);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to remove player: " + player.getName() + " from group: " + group, e);
            return false;
        }
    }
    
    /**
     * Gets all groups for a player.
     *
     * @param player The player
     * @return List of groups
     */
    public List<String> getGroups(Player player) {
        if (!isAvailable() || player == null) {
            return Collections.emptyList();
        }
        
        try {
            return Arrays.asList(vaultPermission.getPlayerGroups(player));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get groups for player: " + player.getName(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets all available groups.
     *
     * @return List of groups
     */
    public List<String> getGroups() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        
        try {
            return Arrays.asList(vaultPermission.getGroups());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get all groups", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets the primary group for a player.
     *
     * @param player The player
     * @return The primary group, or empty string if not found
     */
    public String getPrimaryGroup(Player player) {
        if (!isAvailable() || player == null) {
            return "";
        }
        
        try {
            return vaultPermission.getPrimaryGroup(player);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get primary group for player: " + player.getName(), e);
            return "";
        }
    }
} 
