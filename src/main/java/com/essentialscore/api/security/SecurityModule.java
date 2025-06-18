package com.essentialscore.api.security;

import org.bukkit.plugin.Plugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Main module for integrating all security components into a plugin.
 * Provides a single entry point for security features.
 */
public class SecurityModule {
    private static final Logger LOGGER = Logger.getLogger(SecurityModule.class.getName());
    
    private final SecurityManager securityManager;
    
    /**
     * Creates a new security module.
     *
     * @param plugin The plugin
     */
    public SecurityModule(Plugin plugin) {
        this.securityManager = new SecurityManager(plugin);
    }
    /**
     * Initializes the security module.
     */
    public void initialize() {
        LOGGER.info("Initializing security module...");
        securityManager.initialize();
        LOGGER.info("Security module initialized");
    }
    
    /**
     * Shuts down the security module.
     */
    public void shutdown() {
        LOGGER.info("Shutting down security module...");
        securityManager.shutdown();
        LOGGER.info("Security module shut down");
    }
    
    /**
     * Gets the security manager.
     *
     * @return The security manager
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    /**
     * Checks if a player has a permission.
     *
     * @param player The player
     * @param permission The permission
     * @return True if the player has the permission
     */
    public boolean hasPermission(Player player, String permission) {
        return securityManager.getRBAC().hasPermission(player, permission);
    }
    
    /**
     * Checks if a player has access to an administrative function.
     *
     * @param player The player
     * @param permission The permission
     * @return True if the player has access
     */
    public boolean hasAdminAccess(Player player, String permission) {
        return securityManager.hasAdminAccess(player, permission);
    }
    
    /**
     * Logs a security event.
     *
     * @param category The event category
     * @param action The event action
     * @param player The player
     * @param details The event details
     */
    public void logSecurityEvent(String category, String action, Player player, String details) {
        securityManager.getAuditLogger().logPlayerAction(category, action, player, details);
    }
    
    /**
     * Logs a system security event.
     *
     * @param category The event category
     * @param action The event action
     * @param details The event details
     */
    public void logSystemEvent(String category, String action, String details) {
        securityManager.getAuditLogger().logSystemAction(category, action, details);
    }
    
    /**
     * Encrypts sensitive data.
     *
     * @param data The data to encrypt
     * @return The encrypted data
     */
    public String encryptData(String data) {
        return securityManager.encryptData(data);
    }
    
    /**
     * Decrypts sensitive data.
     *
     * @param encryptedData The encrypted data
     * @return The decrypted data
     */
    public String decryptData(String encryptedData) {
        return securityManager.decryptData(encryptedData);
    }
    
    /**
     * Sets up two-factor authentication for a player.
     *
     * @param player The player
     * @return Setup information, or null if already enabled or not permitted
     */
    public Map<String, Object> setupTwoFactor(Player player) {
        return securityManager.enableTwoFactor(player);
    }
    
    /**
     * Verifies a two-factor authentication code.
     *
     * @param player The player
     * @param code The code
     * @return True if verification succeeded
     */
    public boolean verifyTwoFactorCode(Player player, String code) {
        return securityManager.verifyTwoFactorAndCreateSession(player, code);
    }
    
    /**
     * Executes an action if the player has administrative access.
     *
     * @param player The player
     * @param permission The required permission
     * @param action The action to execute
     * @return True if the action was executed
     */
    public boolean executeSecureAction(Player player, String permission, Runnable action) {
        return securityManager.executeAdminAction(player, permission, action);
    }
    
    /**
     * Assigns a role to a player.
     *
     * @param admin The admin executing the action
     * @param targetId The target player ID
     * @param roleId The role ID
     * @return True if the role was assigned
     */
    public boolean assignRole(Player admin, UUID targetId, String roleId) {
        return securityManager.addRole(admin, targetId, roleId);
    }
    
    /**
     * Creates a new role.
     *
     * @param admin The admin executing the action
     * @param roleId The role ID
     * @param displayName The display name
     * @param description The description
     * @param priority The priority
     * @return True if the role was created
     */
    public boolean createRole(Player admin, String roleId, String displayName, String description, int priority) {
        return securityManager.createRole(admin, roleId, displayName, description, priority) != null;
    }
    
    /**
     * Gets all roles.
     *
     * @return The roles
     */
    public Set<RoleBasedAccessControl.Role> getAllRoles() {
        return Set.copyOf(securityManager.getRBAC().getAllRoles());
    }
    
    /**
     * Exports a player's data for GDPR compliance.
     *
     * @param player The player
     * @param callback The callback to execute when the export is complete
     * @return True if the export request was accepted
     */
    public boolean exportPlayerData(Player player, Runnable callback) {
        try {
            securityManager.exportPlayerData(player)
                    .thenRun(callback)
                    .exceptionally(ex -> {
                        LOGGER.warning("Error exporting player data: " + ex.getMessage());
                        return null;
                    });
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
    
    /**
     * Requests deletion of a player's data.
     *
     * @param player The player
     * @return True if the request was submitted
     */
    public boolean requestDataDeletion(Player player) {
        return securityManager.requestDataDeletion(player);
    }
    
    /**
     * Processes pending data deletion requests.
     *
     * @param admin The admin executing the action
     * @return True if the requests were processed
     */
    public boolean processDataDeletionRequests(Player admin) {
        return securityManager.processDeletionRequests(admin);
    }
    
    /**
     * Sends security information to a command sender.
     *
     * @param sender The command sender
     */
    public void sendSecurityInfo(CommandSender sender) {
        sender.sendMessage("§6§l==== Security Status ====");
        sender.sendMessage("§e• Active sessions: §f" + securityManager.getSessionManager().getActiveSessionCount());
        
        // Show RBAC info
        int roleCount = securityManager.getRBAC().getAllRoles().size();
        sender.sendMessage("§e• Roles configured: §f" + roleCount);
        
        // Show 2FA info if sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean has2fa = securityManager.getTwoFactorAuth().isTwoFactorEnabled(player.getUniqueId());
            sender.sendMessage("§e• Two-factor authentication: §f" + (has2fa ? "§aEnabled" : "§cDisabled"));
            
            boolean hasSession = securityManager.getSessionManager().hasSession(player);
            sender.sendMessage("§e• Active session: §f" + (hasSession ? "§aYes" : "§cNo"));
        }
        
        // Show GDPR info
        int pendingDeletions = 0;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Set<UUID> requests = securityManager.getPendingDeletionRequests(player);
            if (requests != null) {
                pendingDeletions = requests.size();
            }
        }
        sender.sendMessage("§e• Pending data deletion requests: §f" + pendingDeletions);
    }
} 
