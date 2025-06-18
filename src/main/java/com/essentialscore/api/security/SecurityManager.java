package com.essentialscore.api.security;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Manages all security-related components and serves as the main security API.
 * The SecurityManager is responsible for enforcing security policies,
 * sandboxing modules, and controlling access to system resources.
 */
public class SecurityManager implements Listener {
    private static final Logger LOGGER = Logger.getLogger(SecurityManager.class.getName());
    
    private final Plugin plugin;
    private final RoleBasedAccessControl rbac;
    private final AuditLogger auditLogger;
    private final DataEncryption encryption;
    private final TwoFactorAuthentication twoFactor;
    private final SessionManager sessionManager;
    private final GDPRComplianceManager gdprManager;
    private final Map<String, ModuleSandbox> moduleSandboxes;
    private final Map<String, Certificate> trustedCertificates;
    private final VulnerabilityScanner vulnerabilityScanner;
    private final Set<String> blockedOperations;
    private final Map<String, Object> securitySettings;
    private final File configFile;
    private boolean strictMode;
    
    /**
     * Creates a new security manager.
     *
     * @param plugin The plugin
     */
    public SecurityManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "security-config.yml");
        this.securitySettings = new ConcurrentHashMap<>();
        this.moduleSandboxes = new ConcurrentHashMap<>();
        this.trustedCertificates = new ConcurrentHashMap<>();
        this.blockedOperations = ConcurrentHashMap.newKeySet();
        this.strictMode = false;
        
        // Load security settings first
        loadSettings();
        
        // Create security components
        this.auditLogger = new AuditLogger(
                plugin.getDataFolder().toPath().resolve("audit")
        );
        
        this.encryption = new DataEncryption(
                plugin, 
                getSettingAsString("encryption.keystorePassword", generateRandomPassword())
        );
        
        this.rbac = new RoleBasedAccessControl(plugin);
        
        this.twoFactor = new TwoFactorAuthentication(plugin, encryption);
        
        this.sessionManager = new SessionManager(
                plugin, 
                Duration.ofMinutes(getSettingAsInt("session.timeoutMinutes", 15)),
                auditLogger
        );

        this.gdprManager = new GDPRComplianceManager(plugin, auditLogger);
        
        this.vulnerabilityScanner = new VulnerabilityScanner(plugin);
    }
    
    /**
     * Initializes the security manager and all components.
     */
    public void initialize() {
        // Initialize components in the correct order
        LOGGER.info("Initializing security components...");
        
        // Initialize encryption first (other components depend on it)
        encryption.initialize();
        
        // Initialize other components
        auditLogger.initialize();
        rbac.initialize();
        twoFactor.initialize();
        sessionManager.initialize();
        gdprManager.initialize();
        
        // Register this class as an event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        LOGGER.info("Security manager initialized");
    }
    
    /**
     * Shuts down the security manager and all components.
     */
    public void shutdown() {
        LOGGER.info("Shutting down security components...");
        
        // Shutdown in reverse order
        gdprManager.processDeletionRequests(); // Process any pending deletion requests
        sessionManager.shutdown();
        auditLogger.shutdown();
        
        LOGGER.info("Security manager shut down");
    }
    
    /**
     * Gets the role-based access control component.
     *
     * @return The RBAC component
     */
    public RoleBasedAccessControl getRBAC() {
        return rbac;
    }
    
    /**
     * Gets the audit logger component.
     *
     * @return The audit logger
     */
    public AuditLogger getAuditLogger() {
        return auditLogger;
    }
    
    /**
     * Gets the data encryption component.
     *
     * @return The encryption component
     */
    public DataEncryption getEncryption() {
        return encryption;
    }
    
    /**
     * Gets the two-factor authentication component.
     *
     * @return The 2FA component
     */
    public TwoFactorAuthentication getTwoFactorAuth() {
        return twoFactor;
    }
    
    /**
     * Gets the session manager component.
     *
     * @return The session manager
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }
    
    /**
     * Gets the GDPR compliance manager component.
     *
     * @return The GDPR manager
     */
    public GDPRComplianceManager getGDPRManager() {
        return gdprManager;
    }
    
    /**
     * Checks if a player has permission to access an administrative function.
     *
     * @param player The player
     * @param permission The required permission
     * @return True if the player has access
     */
    public boolean hasAdminAccess(Player player, String permission) {
        UUID playerId = player.getUniqueId();
        
        // First check for basic permission
        if (!rbac.hasPermission(player, permission)) {
            return false;
        }
        
        // Check if the permission requires 2FA
        if (requiresTwoFactor(permission)) {
            // Check if 2FA is enabled for this player
            if (!twoFactor.isTwoFactorEnabled(playerId)) {
                return false;
            }
            
            // Check if the player has an active session
            return sessionManager.hasSession(playerId);
        }
        
        return true;
    }
    
    /**
     * Verifies a player's two-factor authentication code and creates a session if valid.
     *
     * @param player The player
     * @param code The 2FA code
     * @return True if verification succeeded and a session was created
     */
    public boolean verifyTwoFactorAndCreateSession(Player player, String code) {
        UUID playerId = player.getUniqueId();
        
        // Check if 2FA is enabled for this player
        if (!twoFactor.isTwoFactorEnabled(playerId)) {
            return false;
        }
        
        // Verify the 2FA code
        if (!twoFactor.verifyCode(playerId, code)) {
            // Log failed verification
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.AUTHENTICATION,
                    AuditLogger.Actions.TWO_FACTOR_VERIFIED,
                    player,
                    "Failed 2FA verification"
            );
            
            return false;
        }
        
        // Log successful verification
        auditLogger.logPlayerAction(
                AuditLogger.Categories.AUTHENTICATION,
                AuditLogger.Actions.TWO_FACTOR_VERIFIED,
                player,
                "Successful 2FA verification"
        );
        
        // Create a session
        sessionManager.createSession(player);
        
        return true;
    }
    
    /**
     * Exports a player's data for GDPR compliance.
     *
     * @param player The player
     * @return A future that completes when the export is done
     */
    public CompletableFuture<File> exportPlayerData(Player player) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.gdpr.export")) {
            CompletableFuture<File> future = new CompletableFuture<>();
            future.completeExceptionally(new SecurityException("No permission to export data"));
            return future;
        }
        
        return gdprManager.exportPlayerData(player);
    }
    
    /**
     * Requests deletion of a player's data for GDPR compliance.
     *
     * @param player The player
     * @return True if the request was submitted
     */
    public boolean requestDataDeletion(Player player) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.gdpr.delete")) {
            return false;
        }
        
        return gdprManager.requestDataDeletion(player);
    }
    
    /**
     * Enables two-factor authentication for a player.
     *
     * @param player The player
     * @return Setup information, or null if already enabled
     */
    public Map<String, Object> enableTwoFactor(Player player) {
        // Check permissions
        if (!rbac.hasPermission(player, "essentials.2fa.setup")) {
            return null;
        }
        
        Map<String, Object> result = twoFactor.enableTwoFactor(player);
        
        if (result != null) {
            // Log 2FA enablement
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.AUTHENTICATION,
                    AuditLogger.Actions.TWO_FACTOR_ENABLED,
                    player,
                    "Two-factor authentication enabled"
            );
        }
        
        return result;
    }
    
    /**
     * Disables two-factor authentication for a player.
     *
     * @param player The player
     * @return True if disabled successfully
     */
    public boolean disableTwoFactor(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check permissions and active session
        if (!hasAdminAccess(player, "essentials.2fa.disable")) {
            return false;
        }
        
        boolean result = twoFactor.disableTwoFactor(playerId);
        
        if (result) {
            // Log 2FA disablement
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.AUTHENTICATION,
                    AuditLogger.Actions.TWO_FACTOR_DISABLED,
                    player,
                    "Two-factor authentication disabled"
            );
            
            // Invalidate session
            sessionManager.invalidateSession(playerId);
        }
        
        return result;
    }
    
    /**
     * Adds a role to a player.
     *
     * @param player The player
     * @param targetId The target player ID
     * @param roleId The role ID
     * @return True if the role was added
     */
    public boolean addRole(Player player, UUID targetId, String roleId) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.roles.manage")) {
            return false;
        }
        
        boolean result = rbac.setPlayerRole(targetId, roleId);
        
        if (result) {
            // Log role assignment
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.PERMISSIONS,
                    AuditLogger.Actions.ROLE_ASSIGNED,
                    player,
                    "Assigned role " + roleId + " to player " + targetId
            );
        }
        
        return result;
    }
    
    /**
     * Creates a new role.
     *
     * @param player The player
     * @param roleId The role ID
     * @param displayName The display name
     * @param description The description
     * @param priority The priority
     * @return The new role, or null if creation failed
     */
    public RoleBasedAccessControl.Role createRole(Player player, String roleId, 
                                              String displayName, String description, int priority) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.roles.manage")) {
            return null;
        }
        
        RoleBasedAccessControl.Role role = rbac.createRole(roleId, displayName, description, priority);
        
        if (role != null) {
            // Log role creation
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.PERMISSIONS,
                    AuditLogger.Actions.ROLE_CREATED,
                    player,
                    "Created role " + roleId
            );
        }
        
        return role;
    }
    
    /**
     * Deletes a role.
     *
     * @param player The player
     * @param roleId The role ID
     * @return True if the role was deleted
     */
    public boolean deleteRole(Player player, String roleId) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.roles.manage")) {
            return false;
        }
        
        boolean result = rbac.deleteRole(roleId);
        
        if (result) {
            // Log role deletion
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.PERMISSIONS,
                    AuditLogger.Actions.ROLE_DELETED,
                    player,
                    "Deleted role " + roleId
            );
        }
        
        return result;
    }
    
    /**
     * Adds a permission to a role.
     *
     * @param player The player
     * @param roleId The role ID
     * @param permission The permission
     * @return True if the permission was added
     */
    public boolean addPermission(Player player, String roleId, String permission) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.roles.manage")) {
            return false;
        }
        
        boolean result = rbac.addPermission(roleId, permission);
        
        if (result) {
            // Log permission addition
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.PERMISSIONS,
                    AuditLogger.Actions.PERMISSION_GRANTED,
                    player,
                    "Added permission " + permission + " to role " + roleId
            );
        }
        
        return result;
    }
    
    /**
     * Removes a permission from a role.
     *
     * @param player The player
     * @param roleId The role ID
     * @param permission The permission
     * @return True if the permission was removed
     */
    public boolean removePermission(Player player, String roleId, String permission) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.roles.manage")) {
            return false;
        }
        
        boolean result = rbac.removePermission(roleId, permission);
        
        if (result) {
            // Log permission removal
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.PERMISSIONS,
                    AuditLogger.Actions.PERMISSION_REVOKED,
                    player,
                    "Removed permission " + permission + " from role " + roleId
            );
        }
        
        return result;
    }
    
    /**
     * Gets all pending GDPR data deletion requests.
     *
     * @param player The player
     * @return The pending requests, or null if the player doesn't have permission
     */
    public Set<UUID> getPendingDeletionRequests(Player player) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.gdpr.admin")) {
            return null;
        }
        
        return gdprManager.getPendingDeletionRequests();
    }
    
    /**
     * Processes all pending GDPR data deletion requests.
     *
     * @param player The player
     * @return True if the player has permission
     */
    public boolean processDeletionRequests(Player player) {
        // Check permissions
        if (!hasAdminAccess(player, "essentials.gdpr.admin")) {
            return false;
        }
        
        // Log deletion processing
        auditLogger.logPlayerAction(
                AuditLogger.Categories.GDPR,
                "DELETION_PROCESSED",
                player,
                "Processing all data deletion requests"
        );
        
        gdprManager.processDeletionRequests();
        return true;
    }
    
    /**
     * Executes an administrative action if the player has permission.
     *
     * @param player The player
     * @param permission The required permission
     * @param action The action to execute
     * @return True if the action was executed
     */
    public boolean executeAdminAction(Player player, String permission, Runnable action) {
        // Check permissions
        if (!hasAdminAccess(player, permission)) {
            return false;
        }
        
        // Execute the action
        action.run();
        
        // Log admin action
        auditLogger.logPlayerAction(
                AuditLogger.Categories.ADMIN,
                AuditLogger.Actions.ADMIN_COMMAND,
                player,
                "Executed admin action requiring " + permission
        );
        
        return true;
    }
    
    /**
     * Encrypts sensitive data.
     *
     * @param data The data to encrypt
     * @return The encrypted data
     */
    public String encryptData(String data) {
        return encryption.encryptForStorage(data);
    }
    
    /**
     * Decrypts sensitive data.
     *
     * @param encryptedData The encrypted data
     * @return The decrypted data
     */
    public String decryptData(String encryptedData) {
        return encryption.decryptFromStorage(encryptedData);
    }    /**
     * Creates and configures a new sandbox for a module.
     *
     * @param moduleId The unique identifier of the module
     * @return The configured sandbox
     */
    public ModuleSandbox createModuleSandbox(String moduleId) {
        SecurityPolicy defaultPolicy = new SecurityPolicy("default", false);
        ModuleSandbox sandbox = new ModuleSandbox(plugin, moduleId, moduleId, defaultPolicy);
        moduleSandboxes.put(moduleId, sandbox);
        return sandbox;
    }

    /**
     * Adds a trusted certificate for module validation.
     *
     * @param alias The certificate alias
     * @param certificate The certificate to trust
     */
    public void addTrustedCertificate(String alias, Certificate certificate) {
        trustedCertificates.put(alias, certificate);
        auditLogger.logSecurityEvent("Added trusted certificate: " + alias);
    }

    /**
     * Blocks a specific operation type across all modules.
     *
     * @param operation The operation to block
     */
    public void blockOperation(String operation) {
        blockedOperations.add(operation);
        auditLogger.logSecurityEvent("Blocked operation: " + operation);
    }

    /**
     * Checks if an operation is blocked.
     *
     * @param operation The operation to check
     * @return True if the operation is blocked
     */
    public boolean isOperationBlocked(String operation) {
        return blockedOperations.contains(operation);
    }

    /**
     * Sets the strict mode for sandboxing.
     *
     * @param strict True to enable strict mode
     */
    public void setStrictMode(boolean strict) {
        this.strictMode = strict;
        moduleSandboxes.values().forEach(sandbox -> sandbox.setStrictMode(strict));
        auditLogger.logSecurityEvent("Strict mode set to: " + strict);
    }

    /**
     * Gets the current strict mode setting.
     *
     * @return True if strict mode is enabled
     */
    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Runs a security scan on all loaded modules.
     */
    public void runSecurityScan() {
        vulnerabilityScanner.scanAll(moduleSandboxes.values());
        auditLogger.logSecurityEvent("Completed security scan");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessionManager.initializeSession(player);
        
        // Check for required security updates
        if (vulnerabilityScanner.hasSecurityUpdates()) {
            if (player.hasPermission("essentialscore.admin")) {
                player.sendMessage("§c[Security] There are pending security updates!");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionManager.terminateSession(event.getPlayer());
    }

    private void loadSettings() {
        if (!configFile.exists()) {
            plugin.saveResource("security-config.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String key : config.getKeys(true)) {
            securitySettings.put(key, config.get(key));
        }
    }

    private String getSettingAsString(String key, String defaultValue) {
        return (String) securitySettings.getOrDefault(key, defaultValue);
    }

    private int getSettingAsInt(String key, int defaultValue) {
        Object value = securitySettings.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    private boolean requiresTwoFactor(String permission) {
        return securitySettings.containsKey("2fa.required." + permission) &&
               (boolean) securitySettings.get("2fa.required." + permission);
    }

    /**
     * Checks if an operation is allowed for a module
     * 
     * @param moduleId The module ID
     * @param operation The operation to check
     * @param target The target of the operation
     * @return true if the operation is allowed
     */
    public boolean isOperationAllowed(String moduleId, String operation, String target) {
        // Check if the operation is explicitly blocked
        String operationKey = moduleId + ":" + operation + ":" + target;
        if (blockedOperations.contains(operationKey)) {
            return false;
        }
        
        // Check module sandbox permissions
        ModuleSandbox sandbox = moduleSandboxes.get(moduleId);
        if (sandbox != null) {
            return sandbox.isOperationAllowed(operation, target);
        }
        
        // Default to allow if no specific restrictions
        return true;
    }

    private String generateRandomPassword() {
        // Implementation of secure random password generation
        // This is just a placeholder - implement proper secure random generation
        return UUID.randomUUID().toString();
    }
}
