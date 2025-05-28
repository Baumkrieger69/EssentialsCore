package com.essentialscore.api.security;

<<<<<<< HEAD
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Duration;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
=======
import com.essentialscore.api.integration.PluginIntegration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
import java.util.logging.Level;
import java.util.logging.Logger;

/**
<<<<<<< HEAD
 * Manages all security-related components and serves as the main security API.
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
    private final Map<String, Object> securitySettings;
    private final File configFile;
=======
 * The SecurityManager is responsible for enforcing security policies,
 * sandboxing modules, and controlling access to system resources.
 */
public class SecurityManager {
    private static final Logger LOGGER = Logger.getLogger(SecurityManager.class.getName());
    
    private final Plugin plugin;
    private final PermissionManager permissionManager;
    private final AuditLogger auditLogger;
    private final Map<String, ModuleSandbox> moduleSandboxes;
    private final Map<String, Certificate> trustedCertificates;
    private final VulnerabilityScanner vulnerabilityScanner;
    private final Set<String> blockedOperations;
    private boolean strictMode = false;
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
    
    /**
     * Creates a new security manager.
     *
<<<<<<< HEAD
     * @param plugin The plugin
     */
    public SecurityManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "security/config.yml");
        this.securitySettings = new HashMap<>();
        
        // Load security settings first
        loadSettings();
        
        // Create security components
        this.auditLogger = new AuditLogger(
                plugin, 
                getSettingAsInt("audit.retentionDays", 90)
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
        
        // Save settings if they were modified (e.g., generated password)
        saveSettings();
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
=======
     * @param plugin The EssentialsCore plugin
     */
    public SecurityManager(Plugin plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
        this.auditLogger = new AuditLogger(plugin);
        this.moduleSandboxes = new ConcurrentHashMap<>();
        this.trustedCertificates = new ConcurrentHashMap<>();
        this.vulnerabilityScanner = new VulnerabilityScanner(plugin);
        this.blockedOperations = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Initializes the security manager.
     */
    public void initialize() {
        LOGGER.info("Initializing security manager...");
        
        // Initialize permission manager
        permissionManager.initialize();
        
        // Initialize audit logger
        auditLogger.initialize();
        
        // Initialize vulnerability scanner
        vulnerabilityScanner.initialize();
        
        // Load trusted certificates
        loadTrustedCertificates();
        
        // Load blocked operations
        loadBlockedOperations();
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        
        LOGGER.info("Security manager initialized");
    }
    
    /**
<<<<<<< HEAD
     * Shuts down the security manager and all components.
     */
    public void shutdown() {
        LOGGER.info("Shutting down security components...");
        
        // Shutdown in reverse order
        gdprManager.processDeletionRequests(); // Process any pending deletion requests
        sessionManager.shutdown();
        auditLogger.shutdown();
=======
     * Shuts down the security manager.
     */
    public void shutdown() {
        LOGGER.info("Shutting down security manager...");
        
        // Shutdown all sandboxes
        for (ModuleSandbox sandbox : moduleSandboxes.values()) {
            sandbox.shutdown();
        }
        moduleSandboxes.clear();
        
        // Shutdown components
        permissionManager.shutdown();
        auditLogger.shutdown();
        vulnerabilityScanner.shutdown();
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        
        LOGGER.info("Security manager shut down");
    }
    
    /**
<<<<<<< HEAD
     * Gets the role-based access control component.
     *
     * @return The RBAC component
     */
    public RoleBasedAccessControl getRBAC() {
        return rbac;
    }
    
    /**
     * Gets the audit logger component.
=======
     * Creates a sandbox for a module.
     *
     * @param moduleId The module ID
     * @param moduleName The module name
     * @param moduleFile The module file
     * @return The module sandbox
     */
    public ModuleSandbox createSandbox(String moduleId, String moduleName, File moduleFile) {
        // Check if module is verified
        boolean verified = verifyModuleSignature(moduleFile);
        
        // Scan for vulnerabilities
        boolean vulnerabilitiesFound = vulnerabilityScanner.scanModule(moduleFile);
        
        if (vulnerabilitiesFound) {
            auditLogger.logSecurity(moduleId, "Module failed vulnerability scan: " + moduleName);
            throw new SecurityException("Module failed vulnerability scan: " + moduleName);
        }
        
        // Create security policy
        SecurityPolicy policy = createSecurityPolicy(moduleId, verified);
        
        // Create and register sandbox
        ModuleSandbox sandbox = new ModuleSandbox(plugin, moduleId, moduleName, policy);
        moduleSandboxes.put(moduleId, sandbox);
        
        auditLogger.logSecurity(moduleId, "Created sandbox for module: " + moduleName);
        return sandbox;
    }
    
    /**
     * Gets a module sandbox.
     *
     * @param moduleId The module ID
     * @return The module sandbox, or null if not found
     */
    public ModuleSandbox getSandbox(String moduleId) {
        return moduleSandboxes.get(moduleId);
    }
    
    /**
     * Destroys a module sandbox.
     *
     * @param moduleId The module ID
     */
    public void destroySandbox(String moduleId) {
        ModuleSandbox sandbox = moduleSandboxes.remove(moduleId);
        
        if (sandbox != null) {
            sandbox.shutdown();
            auditLogger.logSecurity(moduleId, "Destroyed sandbox for module: " + sandbox.getModuleName());
        }
    }
    
    /**
     * Verifies a module signature.
     *
     * @param moduleFile The module file
     * @return true if the module signature is valid
     */
    public boolean verifyModuleSignature(File moduleFile) {
        try {
            // This would implement actual signature verification
            // For now, just log the attempt
            auditLogger.logSecurity("system", "Verifying module signature: " + moduleFile.getName());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to verify module signature: " + moduleFile.getName(), e);
            auditLogger.logSecurity("system", "Failed to verify module signature: " + moduleFile.getName());
            return false;
        }
    }
    
    /**
     * Checks if an operation is allowed for a module.
     *
     * @param moduleId The module ID
     * @param operationType The operation type
     * @param target The operation target
     * @return true if the operation is allowed
     */
    public boolean isOperationAllowed(String moduleId, String operationType, String target) {
        ModuleSandbox sandbox = getSandbox(moduleId);
        
        if (sandbox == null) {
            auditLogger.logSecurity(moduleId, "Operation denied (no sandbox): " + operationType + " -> " + target);
            return false;
        }
        
        boolean allowed = sandbox.getSecurityPolicy().isOperationAllowed(operationType, target);
        
        if (!allowed) {
            auditLogger.logSecurity(moduleId, "Operation denied: " + operationType + " -> " + target);
        }
        
        return allowed;
    }
    
    /**
     * Checks if a file operation is allowed for a module.
     *
     * @param moduleId The module ID
     * @param file The file
     * @param operation The operation (read, write, execute)
     * @return true if the file operation is allowed
     */
    public boolean isFileOperationAllowed(String moduleId, File file, String operation) {
        ModuleSandbox sandbox = getSandbox(moduleId);
        
        if (sandbox == null) {
            auditLogger.logSecurity(moduleId, "File operation denied (no sandbox): " + operation + " -> " + file.getPath());
            return false;
        }
        
        boolean allowed = sandbox.getSecurityPolicy().isFileOperationAllowed(file, operation);
        
        if (!allowed) {
            auditLogger.logSecurity(moduleId, "File operation denied: " + operation + " -> " + file.getPath());
        }
        
        return allowed;
    }
    
    /**
     * Checks if a network operation is allowed for a module.
     *
     * @param moduleId The module ID
     * @param host The host
     * @param port The port
     * @param operation The operation (connect, listen)
     * @return true if the network operation is allowed
     */
    public boolean isNetworkOperationAllowed(String moduleId, String host, int port, String operation) {
        ModuleSandbox sandbox = getSandbox(moduleId);
        
        if (sandbox == null) {
            auditLogger.logSecurity(moduleId, "Network operation denied (no sandbox): " + operation + " -> " + host + ":" + port);
            return false;
        }
        
        boolean allowed = sandbox.getSecurityPolicy().isNetworkOperationAllowed(host, port, operation);
        
        if (!allowed) {
            auditLogger.logSecurity(moduleId, "Network operation denied: " + operation + " -> " + host + ":" + port);
        }
        
        return allowed;
    }
    
    /**
     * Checks if a plugin interaction is allowed for a module.
     *
     * @param moduleId The module ID
     * @param pluginName The plugin name
     * @return true if the plugin interaction is allowed
     */
    public boolean isPluginInteractionAllowed(String moduleId, String pluginName) {
        ModuleSandbox sandbox = getSandbox(moduleId);
        
        if (sandbox == null) {
            auditLogger.logSecurity(moduleId, "Plugin interaction denied (no sandbox): " + pluginName);
            return false;
        }
        
        boolean allowed = sandbox.getSecurityPolicy().isPluginInteractionAllowed(pluginName);
        
        if (!allowed) {
            auditLogger.logSecurity(moduleId, "Plugin interaction denied: " + pluginName);
        }
        
        return allowed;
    }
    
    /**
     * Gets the permission manager.
     *
     * @return The permission manager
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    /**
     * Gets the audit logger.
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
     *
     * @return The audit logger
     */
    public AuditLogger getAuditLogger() {
        return auditLogger;
    }
    
    /**
<<<<<<< HEAD
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
    }
    
    /**
     * Loads security settings from disk.
     */
    private void loadSettings() {
        try {
            // Create security directory if it doesn't exist
            File securityDir = new File(plugin.getDataFolder(), "security");
            if (!securityDir.exists()) {
                securityDir.mkdirs();
            }
            
            // Load settings if the file exists
            if (configFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                
                // Load all settings
                for (String key : config.getKeys(true)) {
                    securitySettings.put(key, config.get(key));
                }
                
                LOGGER.info("Loaded security settings");
            } else {
                // Initialize with default settings
                securitySettings.put("audit.retentionDays", 90);
                securitySettings.put("session.timeoutMinutes", 15);
                securitySettings.put("twoFactor.requiredPermissions", new String[]{
                        "essentials.roles.manage",
                        "essentials.gdpr.admin",
                        "essentials.admin"
                });
                
                LOGGER.info("Initialized default security settings");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading security settings", e);
            
            // Use default settings
            securitySettings.put("audit.retentionDays", 90);
            securitySettings.put("session.timeoutMinutes", 15);
        }
    }
    
    /**
     * Saves security settings to disk.
     */
    private void saveSettings() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            // Save all settings
            for (Map.Entry<String, Object> entry : securitySettings.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            
            // Save to file
            config.save(configFile);
            
            LOGGER.info("Saved security settings");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving security settings", e);
        }
    }
    
    /**
     * Gets a setting as a string.
     *
     * @param key The setting key
     * @param defaultValue The default value
     * @return The setting value
     */
    private String getSettingAsString(String key, String defaultValue) {
        Object value = securitySettings.get(key);
        
        if (value instanceof String) {
            return (String) value;
        }
        
        // Use default and update settings
        securitySettings.put(key, defaultValue);
        return defaultValue;
    }
    
    /**
     * Gets a setting as an integer.
     *
     * @param key The setting key
     * @param defaultValue The default value
     * @return The setting value
     */
    private int getSettingAsInt(String key, int defaultValue) {
        Object value = securitySettings.get(key);
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        // Use default and update settings
        securitySettings.put(key, defaultValue);
        return defaultValue;
    }
    
    /**
     * Checks if a permission requires two-factor authentication.
     *
     * @param permission The permission to check
     * @return True if 2FA is required
     */
    private boolean requiresTwoFactor(String permission) {
        Object value = securitySettings.get("twoFactor.requiredPermissions");
        
        if (value instanceof String[]) {
            String[] requiredPermissions = (String[]) value;
            
            for (String requiredPermission : requiredPermissions) {
                if (permission.startsWith(requiredPermission)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Generates a random password for the keystore.
     *
     * @return The generated password
     */
    private String generateRandomPassword() {
        // Since encryption isn't initialized yet, use a simple method
        StringBuilder password = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        
        for (int i = 0; i < 32; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }
    
    /**
     * Handles player join events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Log player join
        auditLogger.logPlayerAction(
                AuditLogger.Categories.USER,
                AuditLogger.Actions.LOGIN,
                player,
                "Player joined the server"
        );
    }
    
    /**
     * Handles player quit events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Log player quit
        auditLogger.logPlayerAction(
                AuditLogger.Categories.USER,
                AuditLogger.Actions.LOGOUT,
                player,
                "Player left the server"
        );
        
        // Invalidate player's session
        sessionManager.invalidateSession(playerId);
=======
     * Gets the vulnerability scanner.
     *
     * @return The vulnerability scanner
     */
    public VulnerabilityScanner getVulnerabilityScanner() {
        return vulnerabilityScanner;
    }
    
    /**
     * Sets strict mode.
     *
     * @param strictMode true to enable strict mode
     */
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
        LOGGER.info("Security strict mode: " + strictMode);
    }
    
    /**
     * Checks if strict mode is enabled.
     *
     * @return true if strict mode is enabled
     */
    public boolean isStrictMode() {
        return strictMode;
    }
    
    /**
     * Creates a security policy for a module.
     *
     * @param moduleId The module ID
     * @param verified true if the module is verified
     * @return The security policy
     */
    private SecurityPolicy createSecurityPolicy(String moduleId, boolean verified) {
        SecurityPolicy policy = new SecurityPolicy(moduleId);
        
        // Add default permissions based on verification status
        if (verified) {
            policy.addPermission(SecurityPermission.FILE_READ, plugin.getDataFolder().getPath() + "/*");
            policy.addPermission(SecurityPermission.FILE_WRITE, plugin.getDataFolder().getPath() + "/modules/" + moduleId + "/*");
            policy.addPermission(SecurityPermission.NETWORK_CONNECT, "api.example.com:443");
        } else {
            // Unverified modules get minimal permissions
            policy.addPermission(SecurityPermission.FILE_READ, plugin.getDataFolder().getPath() + "/modules/" + moduleId + "/*");
            policy.addPermission(SecurityPermission.FILE_WRITE, plugin.getDataFolder().getPath() + "/modules/" + moduleId + "/data/*");
        }
        
        return policy;
    }
    
    /**
     * Loads trusted certificates.
     */
    private void loadTrustedCertificates() {
        // This would load certificates from a keystore
        // For now, just log the attempt
        LOGGER.info("Loading trusted certificates...");
    }
    
    /**
     * Loads blocked operations.
     */
    private void loadBlockedOperations() {
        // This would load blocked operations from configuration
        // For now, add some default blocked operations
        blockedOperations.add("system.exit");
        blockedOperations.add("java.lang.Runtime.exec");
        blockedOperations.add("java.lang.System.setSecurityManager");
        
        LOGGER.info("Loaded " + blockedOperations.size() + " blocked operations");
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
    }
} 