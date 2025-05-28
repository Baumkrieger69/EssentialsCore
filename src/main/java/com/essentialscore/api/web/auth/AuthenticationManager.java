package com.essentialscore.api.web.auth;

import com.essentialscore.api.security.SecurityManager;
import com.essentialscore.api.web.WebSession;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages authentication for the WebUI.
 * Handles login attempts, session validation, and user permissions.
 */
public class AuthenticationManager {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationManager.class.getName());
    
    private final Plugin plugin;
    private final SecurityManager securityManager;
    private final File usersFile;
    private final Map<String, UserCredentials> users;
    private final Map<String, LoginAttemptInfo> loginAttempts;
    private final Map<String, String> sessionTokens;
    private final SecureRandom secureRandom;
    private final int maxLoginAttempts;
    private final int lockoutMinutes;
    private final int sessionTimeoutMinutes;
    
    /**
     * Creates a new authentication manager
     * 
     * @param plugin The plugin instance
     * @param securityManager The security manager
     * @param config The WebUI configuration
     */
    public AuthenticationManager(Plugin plugin, SecurityManager securityManager, FileConfiguration config) {
        this.plugin = plugin;
        this.securityManager = securityManager;
        this.usersFile = new File(plugin.getDataFolder(), "webui/users.yml");
        this.users = new ConcurrentHashMap<>();
        this.loginAttempts = new ConcurrentHashMap<>();
        this.sessionTokens = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
        
        // Load security settings
        this.maxLoginAttempts = config.getInt("security.max-login-attempts", 5);
        this.lockoutMinutes = config.getInt("security.lockout-minutes", 15);
        this.sessionTimeoutMinutes = config.getInt("security.session-timeout-minutes", 30);
        
        // Load users
        loadUsers();
        
        // Create default admin if no users exist
        if (users.isEmpty()) {
            createDefaultAdmin();
        }
        
        // Schedule cleanup of expired login attempts
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupExpiredLoginAttempts, 
                1200, 1200); // Run every minute (20 ticks * 60)
    }
    
    /**
     * Loads users from the users file
     */
    private void loadUsers() {
        if (!usersFile.exists()) {
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(usersFile);
            
            for (String username : config.getKeys(false)) {
                if (!config.isConfigurationSection(username)) {
                    continue;
                }
                
                String passwordHash = config.getString(username + ".password-hash");
                String salt = config.getString(username + ".salt");
                String playerUUID = config.getString(username + ".player-uuid");
                String role = config.getString(username + ".role", "user");
                boolean enabled = config.getBoolean(username + ".enabled", true);
                
                UUID uuid = null;
                if (playerUUID != null && !playerUUID.isEmpty()) {
                    try {
                        uuid = UUID.fromString(playerUUID);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Invalid UUID for user " + username + ": " + playerUUID);
                    }
                }
                
                UserCredentials credentials = new UserCredentials(
                        username, 
                        passwordHash, 
                        salt, 
                        uuid, 
                        role, 
                        enabled
                );
                
                users.put(username.toLowerCase(), credentials);
            }
            
            LOGGER.info("Loaded " + users.size() + " WebUI users");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load WebUI users", e);
        }
    }
    
    /**
     * Saves users to the users file
     */
    private void saveUsers() {
        try {
            // Ensure parent directory exists
            if (!usersFile.getParentFile().exists()) {
                usersFile.getParentFile().mkdirs();
            }
            
            YamlConfiguration config = new YamlConfiguration();
            
            for (UserCredentials credentials : users.values()) {
                String username = credentials.getUsername();
                config.set(username + ".password-hash", credentials.getPasswordHash());
                config.set(username + ".salt", credentials.getSalt());
                
                if (credentials.getPlayerUUID() != null) {
                    config.set(username + ".player-uuid", credentials.getPlayerUUID().toString());
                }
                
                config.set(username + ".role", credentials.getRole());
                config.set(username + ".enabled", credentials.isEnabled());
            }
            
            config.save(usersFile);
            LOGGER.info("Saved " + users.size() + " WebUI users");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save WebUI users", e);
        }
    }
    
    /**
     * Creates the default admin user
     */
    private void createDefaultAdmin() {
        // Generate a random password
        String password = generateRandomPassword();
        String salt = generateSalt();
        String passwordHash = hashPassword(password, salt);
        
        UserCredentials adminCredentials = new UserCredentials(
                "admin",
                passwordHash,
                salt,
                null,
                "admin",
                true
        );
        
        users.put("admin", adminCredentials);
        saveUsers();
        
        LOGGER.info("Created default WebUI admin user with password: " + password);
        LOGGER.info("IMPORTANT: Please change this password immediately!");
    }
    
    /**
     * Generates a random password
     * 
     * @return A random password
     */
    private String generateRandomPassword() {
        // Characters to use in the password (excluding ambiguous characters)
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*()-_=+";
        
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = secureRandom.nextInt(chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }
    
    /**
     * Generates a random salt
     * 
     * @return A random salt
     */
    private String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Hashes a password with a salt
     * 
     * @param password The password
     * @param salt The salt
     * @return The hashed password
     */
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Failed to hash password", e);
            return null;
        }
    }
    
    /**
     * Attempts to authenticate a user
     * 
     * @param username The username
     * @param password The password
     * @param ipAddress The IP address
     * @return A result with authentication details
     */
    public AuthResult authenticate(String username, String password, String ipAddress) {
        // Check if username is empty
        if (username == null || username.isEmpty()) {
            return new AuthResult(false, "Username cannot be empty", null);
        }
        
        // Normalize username
        username = username.toLowerCase();
        
        // Check if user is locked out
        LoginAttemptInfo attemptInfo = loginAttempts.get(ipAddress + ":" + username);
        if (attemptInfo != null && attemptInfo.isLockedOut()) {
            return new AuthResult(false, "Too many failed login attempts. Please try again later.", null);
        }
        
        // Check if user exists
        UserCredentials credentials = users.get(username);
        if (credentials == null) {
            recordFailedAttempt(username, ipAddress);
            return new AuthResult(false, "Invalid username or password", null);
        }
        
        // Check if user is enabled
        if (!credentials.isEnabled()) {
            recordFailedAttempt(username, ipAddress);
            return new AuthResult(false, "Account is disabled", null);
        }
        
        // Check password
        String hashedPassword = hashPassword(password, credentials.getSalt());
        if (!hashedPassword.equals(credentials.getPasswordHash())) {
            recordFailedAttempt(username, ipAddress);
            return new AuthResult(false, "Invalid username or password", null);
        }
        
        // Authentication successful
        resetFailedAttempts(username, ipAddress);
        
        // Generate session token
        String sessionToken = generateSessionToken();
        
        // Create web session
        WebSession session = new WebSession(
                sessionToken,
                credentials.getUsername(),
                ipAddress,
                credentials.getPlayerUUID()
        );
        
        // Mark as authenticated
        session.setAuthenticated(true);
        
        // Store session token
        sessionTokens.put(sessionToken, username);
        
        return new AuthResult(true, "Authentication successful", session);
    }
    
    /**
     * Records a failed login attempt
     * 
     * @param username The username
     * @param ipAddress The IP address
     */
    private void recordFailedAttempt(String username, String ipAddress) {
        String key = ipAddress + ":" + username;
        LoginAttemptInfo attemptInfo = loginAttempts.get(key);
        
        if (attemptInfo == null) {
            attemptInfo = new LoginAttemptInfo();
            loginAttempts.put(key, attemptInfo);
        }
        
        attemptInfo.incrementAttempts();
        
        if (attemptInfo.getAttempts() >= maxLoginAttempts) {
            attemptInfo.setLockedOutUntil(Instant.now().plusSeconds(lockoutMinutes * 60));
            LOGGER.warning("Locked out user " + username + " from IP " + ipAddress + 
                    " due to too many failed login attempts");
        }
    }
    
    /**
     * Resets failed login attempts
     * 
     * @param username The username
     * @param ipAddress The IP address
     */
    private void resetFailedAttempts(String username, String ipAddress) {
        loginAttempts.remove(ipAddress + ":" + username);
    }
    
    /**
     * Cleans up expired login attempts
     */
    private void cleanupExpiredLoginAttempts() {
        Instant now = Instant.now();
        loginAttempts.entrySet().removeIf(entry -> {
            LoginAttemptInfo info = entry.getValue();
            return info.getLockedOutUntil() != null && info.getLockedOutUntil().isBefore(now);
        });
    }
    
    /**
     * Generates a unique session token
     * 
     * @return A session token
     */
    private String generateSessionToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * Validates a session token
     * 
     * @param token The token to validate
     * @return The associated username, or null if invalid
     */
    public String validateToken(String token) {
        return sessionTokens.get(token);
    }
    
    /**
     * Invalidates a session token
     * 
     * @param token The token to invalidate
     */
    public void invalidateToken(String token) {
        sessionTokens.remove(token);
    }
    
    /**
     * Creates a new user
     * 
     * @param username The username
     * @param password The password
     * @param playerUUID The player UUID (optional)
     * @param role The role
     * @return true if successful
     */
    public boolean createUser(String username, String password, UUID playerUUID, String role) {
        // Normalize username
        username = username.toLowerCase();
        
        // Check if user already exists
        if (users.containsKey(username)) {
            return false;
        }
        
        // Generate salt and hash password
        String salt = generateSalt();
        String passwordHash = hashPassword(password, salt);
        
        // Create user
        UserCredentials credentials = new UserCredentials(
                username,
                passwordHash,
                salt,
                playerUUID,
                role,
                true
        );
        
        users.put(username, credentials);
        saveUsers();
        
        LOGGER.info("Created WebUI user: " + username);
        return true;
    }
    
    /**
     * Updates a user's password
     * 
     * @param username The username
     * @param newPassword The new password
     * @return true if successful
     */
    public boolean updatePassword(String username, String newPassword) {
        // Normalize username
        username = username.toLowerCase();
        
        // Check if user exists
        UserCredentials credentials = users.get(username);
        if (credentials == null) {
            return false;
        }
        
        // Generate new salt and hash password
        String salt = generateSalt();
        String passwordHash = hashPassword(newPassword, salt);
        
        // Update credentials
        credentials.setPasswordHash(passwordHash);
        credentials.setSalt(salt);
        
        saveUsers();
        
        LOGGER.info("Updated password for WebUI user: " + username);
        return true;
    }
    
    /**
     * Deletes a user
     * 
     * @param username The username
     * @return true if successful
     */
    public boolean deleteUser(String username) {
        // Normalize username
        final String normalizedUsername = username.toLowerCase();
        
        // Check if user exists
        if (!users.containsKey(normalizedUsername)) {
            return false;
        }
        
        // Don't delete the last admin
        if (normalizedUsername.equals("admin") && users.size() == 1) {
            return false;
        }
        
        // Delete user
        users.remove(normalizedUsername);
        saveUsers();
        
        // Remove any active sessions for this user
        sessionTokens.entrySet().removeIf(entry -> entry.getValue().equals(normalizedUsername));
        
        LOGGER.info("Deleted WebUI user: " + normalizedUsername);
        return true;
    }
    
    /**
     * Enables or disables a user
     * 
     * @param username The username
     * @param enabled Whether the user should be enabled
     * @return true if successful
     */
    public boolean setUserEnabled(String username, boolean enabled) {
        // Normalize username
        username = username.toLowerCase();
        
        // Check if user exists
        UserCredentials credentials = users.get(username);
        if (credentials == null) {
            return false;
        }
        
        // Update enabled state
        credentials.setEnabled(enabled);
        saveUsers();
        
        LOGGER.info((enabled ? "Enabled" : "Disabled") + " WebUI user: " + username);
        return true;
    }
    
    /**
     * Gets information about all users
     * 
     * @return A map of usernames to user information
     */
    public Map<String, Map<String, Object>> getUsersInfo() {
        Map<String, Map<String, Object>> usersInfo = new HashMap<>();
        
        for (UserCredentials credentials : users.values()) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", credentials.getUsername());
            userInfo.put("role", credentials.getRole());
            userInfo.put("enabled", credentials.isEnabled());
            
            if (credentials.getPlayerUUID() != null) {
                userInfo.put("playerUUID", credentials.getPlayerUUID().toString());
                
                // Get player name if online
                Player player = Bukkit.getPlayer(credentials.getPlayerUUID());
                if (player != null) {
                    userInfo.put("playerName", player.getName());
                    userInfo.put("playerOnline", true);
                } else {
                    userInfo.put("playerOnline", false);
                }
            } else {
                userInfo.put("playerLinked", false);
            }
            
            usersInfo.put(credentials.getUsername(), userInfo);
        }
        
        return usersInfo;
    }
    
    /**
     * Checks if a user has permission to access a resource
     * 
     * @param username The username
     * @param permission The permission to check
     * @return true if the user has permission
     */
    public boolean hasPermission(String username, String permission) {
        // Normalize username
        username = username.toLowerCase();
        
        // Check if user exists
        UserCredentials credentials = users.get(username);
        if (credentials == null) {
            return false;
        }
        
        // Admin has all permissions
        if (credentials.getRole().equals("admin")) {
            return true;
        }
        
        // TODO: Implement more sophisticated permission checking based on roles
        // For now, check a few basic permissions
        if (credentials.getRole().equals("user")) {
            // Basic user permissions
            return permission.equals("dashboard.view") || 
                   permission.equals("console.view") ||
                   permission.equals("files.view");
        }
        
        return false;
    }
    
    /**
     * Represents the result of an authentication attempt
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final WebSession session;
        
        /**
         * Creates a new authentication result
         * 
         * @param success Whether authentication was successful
         * @param message A message describing the result
         * @param session The session if successful, null otherwise
         */
        public AuthResult(boolean success, String message, WebSession session) {
            this.success = success;
            this.message = message;
            this.session = session;
        }
        
        /**
         * Checks if authentication was successful
         * 
         * @return true if successful
         */
        public boolean isSuccess() {
            return success;
        }
        
        /**
         * Gets the result message
         * 
         * @return The message
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Gets the session
         * 
         * @return The session, or null if authentication failed
         */
        public WebSession getSession() {
            return session;
        }
    }
    
    /**
     * Represents credentials for a WebUI user
     */
    private static class UserCredentials {
        private final String username;
        private String passwordHash;
        private String salt;
        private final UUID playerUUID;
        private final String role;
        private boolean enabled;
        
        /**
         * Creates new user credentials
         * 
         * @param username The username
         * @param passwordHash The hashed password
         * @param salt The salt used to hash the password
         * @param playerUUID The player UUID (optional)
         * @param role The user role
         * @param enabled Whether the user is enabled
         */
        public UserCredentials(String username, String passwordHash, String salt, 
                              UUID playerUUID, String role, boolean enabled) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.salt = salt;
            this.playerUUID = playerUUID;
            this.role = role;
            this.enabled = enabled;
        }
        
        /**
         * Gets the username
         * 
         * @return The username
         */
        public String getUsername() {
            return username;
        }
        
        /**
         * Gets the hashed password
         * 
         * @return The hashed password
         */
        public String getPasswordHash() {
            return passwordHash;
        }
        
        /**
         * Sets the hashed password
         * 
         * @param passwordHash The hashed password
         */
        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
        
        /**
         * Gets the salt
         * 
         * @return The salt
         */
        public String getSalt() {
            return salt;
        }
        
        /**
         * Sets the salt
         * 
         * @param salt The salt
         */
        public void setSalt(String salt) {
            this.salt = salt;
        }
        
        /**
         * Gets the player UUID
         * 
         * @return The player UUID, or null if not linked
         */
        public UUID getPlayerUUID() {
            return playerUUID;
        }
        
        /**
         * Gets the user role
         * 
         * @return The role
         */
        public String getRole() {
            return role;
        }
        
        /**
         * Checks if the user is enabled
         * 
         * @return true if enabled
         */
        public boolean isEnabled() {
            return enabled;
        }
        
        /**
         * Sets whether the user is enabled
         * 
         * @param enabled true to enable, false to disable
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * Tracks login attempts and lockouts
     */
    private static class LoginAttemptInfo {
        private int attempts;
        private Instant lockedOutUntil;
        
        /**
         * Creates a new login attempt info
         */
        public LoginAttemptInfo() {
            this.attempts = 0;
            this.lockedOutUntil = null;
        }
        
        /**
         * Increments the attempt counter
         */
        public void incrementAttempts() {
            attempts++;
        }
        
        /**
         * Gets the number of attempts
         * 
         * @return The number of attempts
         */
        public int getAttempts() {
            return attempts;
        }
        
        /**
         * Gets the time until the lockout expires
         * 
         * @return The lockout expiration time, or null if not locked out
         */
        public Instant getLockedOutUntil() {
            return lockedOutUntil;
        }
        
        /**
         * Sets the time until the lockout expires
         * 
         * @param lockedOutUntil The lockout expiration time
         */
        public void setLockedOutUntil(Instant lockedOutUntil) {
            this.lockedOutUntil = lockedOutUntil;
        }
        
        /**
         * Checks if the user is currently locked out
         * 
         * @return true if locked out
         */
        public boolean isLockedOut() {
            return lockedOutUntil != null && lockedOutUntil.isAfter(Instant.now());
        }
    }
} 