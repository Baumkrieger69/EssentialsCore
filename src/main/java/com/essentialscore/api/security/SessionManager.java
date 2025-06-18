package com.essentialscore.api.security;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Manages user sessions with automatic timeouts for administrative functions.
 */
public class SessionManager {
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());
    
    // Default timeout duration (15 minutes)
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(15);
    
    // Minimum allowed timeout (1 minute)
    private static final Duration MIN_TIMEOUT = Duration.ofMinutes(1);
    
    private final Plugin plugin;
    private final Map<UUID, Session> sessions;
    private final Duration timeout;
    private BukkitTask cleanupTask;
    private final AuditLogger auditLogger;
    
    /**
     * Creates a new session manager with default timeout.
     *
     * @param plugin The plugin
     * @param auditLogger The audit logger
     */
    public SessionManager(Plugin plugin, AuditLogger auditLogger) {
        this(plugin, DEFAULT_TIMEOUT, auditLogger);
    }
    
    /**
     * Creates a new session manager with a custom timeout.
     *
     * @param plugin The plugin
     * @param timeout The session timeout duration
     * @param auditLogger The audit logger
     */
    public SessionManager(Plugin plugin, Duration timeout, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.sessions = new ConcurrentHashMap<>();
        this.timeout = timeout.compareTo(MIN_TIMEOUT) < 0 ? MIN_TIMEOUT : timeout;
        this.auditLogger = auditLogger;
    }
    
    /**
     * Initializes the session manager.
     */
    public void initialize() {
        // Start cleanup task (runs every minute)
        cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::cleanupExpiredSessions,
                20 * 60, // 1 minute delay
                20 * 60  // Run every minute
        );
        
        LOGGER.info("Session manager initialized with timeout: " + timeout.toMinutes() + " minutes");
    }
    
    /**
     * Shuts down the session manager.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        
        sessions.clear();
        LOGGER.info("Session manager shut down");
    }
    
    /**
     * Creates a new session for a player.
     *
     * @param player The player
     * @return The session
     */
    public Session createSession(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check if a session already exists
        Session existingSession = sessions.get(playerId);
        if (existingSession != null && !existingSession.isExpired()) {
            // Refresh the session
            existingSession.refresh();
            return existingSession;
        }
        
        // Create a new session
        Session session = new Session(playerId, player.getName(), timeout);
        sessions.put(playerId, session);
        
        // Log session creation
        if (auditLogger != null) {
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.AUTHENTICATION,
                    AuditLogger.Actions.LOGIN,
                    player,
                    "Session created"
            );
        }
        
        return session;
    }
    
    /**
     * Initializes a session for a player
     * 
     * @param player The player to initialize a session for
     */
    public void initializeSession(Player player) {
        UUID playerId = player.getUniqueId();
        Session session = new Session(playerId, player.getName());
        sessions.put(playerId, session);
        
        LOGGER.info("Session initialized for player: " + player.getName());
        
        // Log the session creation
        auditLogger.logPlayerAction(
            AuditLogger.Actions.LOGIN,
            "session",
            player,
            "Session initialized"
        );
    }
    
    /**
     * Terminates a session for a player
     * 
     * @param player The player to terminate the session for
     */
    public void terminateSession(Player player) {
        UUID playerId = player.getUniqueId();
        Session session = sessions.remove(playerId);
        
        if (session != null) {
            LOGGER.info("Session terminated for player: " + player.getName());
            
            // Log the session termination
            auditLogger.logPlayerAction(
                AuditLogger.Actions.LOGOUT,
                "session", 
                player,
                "Session terminated"
            );
        }
    }

    /**
     * Gets a player's session.
     *
     * @param playerId The player ID
     * @return The session, or null if not found or expired
     */
    public Session getSession(UUID playerId) {
        Session session = sessions.get(playerId);
        
        if (session != null) {
            if (session.isExpired()) {
                // Session has expired, remove it
                sessions.remove(playerId);
                
                // Log session expiration
                if (auditLogger != null) {
                    auditLogger.logSystemAction(
                            AuditLogger.Categories.AUTHENTICATION,
                            AuditLogger.Actions.SESSION_EXPIRED,
                            "Session expired for player " + session.getPlayerName() + " (" + playerId + ")"
                    );
                }
                
                return null;
            }
            
            return session;
        }
        
        return null;
    }
    
    /**
     * Gets a player's session.
     *
     * @param player The player
     * @return The session, or null if not found or expired
     */
    public Session getSession(Player player) {
        return getSession(player.getUniqueId());
    }
    
    /**
     * Checks if a player has an active session.
     *
     * @param playerId The player ID
     * @return True if the player has an active session
     */
    public boolean hasSession(UUID playerId) {
        return getSession(playerId) != null;
    }
    
    /**
     * Checks if a player has an active session.
     *
     * @param player The player
     * @return True if the player has an active session
     */
    public boolean hasSession(Player player) {
        return hasSession(player.getUniqueId());
    }
    
    /**
     * Invalidates a player's session.
     *
     * @param playerId The player ID
     * @return True if a session was invalidated
     */
    public boolean invalidateSession(UUID playerId) {
        Session session = sessions.remove(playerId);
        
        if (session != null) {
            // Log session invalidation
            if (auditLogger != null) {
                auditLogger.logSystemAction(
                        AuditLogger.Categories.AUTHENTICATION,
                        AuditLogger.Actions.LOGOUT,
                        "Session invalidated for player " + session.getPlayerName() + " (" + playerId + ")"
                );
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Invalidates a player's session.
     *
     * @param player The player
     * @return True if a session was invalidated
     */
    public boolean invalidateSession(Player player) {
        return invalidateSession(player.getUniqueId());
    }
    
    /**
     * Refreshes a player's session.
     *
     * @param playerId The player ID
     * @return True if a session was refreshed
     */
    public boolean refreshSession(UUID playerId) {
        Session session = sessions.get(playerId);
        
        if (session != null) {
            session.refresh();
            return true;
        }
        
        return false;
    }
    
    /**
     * Refreshes a player's session.
     *
     * @param player The player
     * @return True if a session was refreshed
     */
    public boolean refreshSession(Player player) {
        return refreshSession(player.getUniqueId());
    }
    
    /**
     * Gets the number of active sessions.
     *
     * @return The number of active sessions
     */
    public int getActiveSessionCount() {
        // Clean up expired sessions first
        cleanupExpiredSessions();
        
        return sessions.size();
    }
    
    /**
     * Cleans up expired sessions.
     */
    private void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            
            if (session.isExpired()) {
                // Log session expiration
                if (auditLogger != null) {
                    auditLogger.logSystemAction(
                            AuditLogger.Categories.AUTHENTICATION,
                            AuditLogger.Actions.SESSION_EXPIRED,
                            "Session expired for player " + session.getPlayerName() + " (" + entry.getKey() + ")"
                    );
                }
                
                return true;
            }
            
            return false;
        });
    }
    
    /**
     * Executes an action if the player has an active session.
     *
     * @param player The player
     * @param action The action to execute
     * @return True if the action was executed
     */
    public boolean withActiveSession(Player player, Runnable action) {
        Session session = getSession(player);
        
        if (session != null) {
            action.run();
            session.refresh();
            return true;
        }
        
        return false;
    }
    
    /**
     * Executes an action if the player has an active session with a permission.
     *
     * @param player The player
     * @param permission The required permission
     * @param action The action to execute
     * @return True if the action was executed
     */
    public boolean withPermission(Player player, String permission, Runnable action) {
        Session session = getSession(player);
        
        if (session != null && session.hasPermission(permission)) {
            action.run();
            session.refresh();
            return true;
        }
        
        return false;
    }
    
    /**
     * Class representing a user session.
     */
    public class Session {
        private final UUID playerId;
        private final String playerName;
        private final Duration timeout;
        private Instant expiresAt;
        private final Map<String, Object> attributes;
        
        /**
         * Creates a new session.
         *
         * @param playerId The player ID
         * @param playerName The player name
         * @param timeout The session timeout
         */
        public Session(UUID playerId, String playerName, Duration timeout) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.timeout = timeout;
            this.attributes = new ConcurrentHashMap<>();
            refresh();
        }
        
        /**
         * Creates a new session with default timeout.
         *
         * @param playerId The player ID
         * @param playerName The player name
         */
        public Session(UUID playerId, String playerName) {
            this(playerId, playerName, DEFAULT_TIMEOUT);
        }

        /**
         * Refreshes the session, extending its expiration time.
         */
        public void refresh() {
            this.expiresAt = Instant.now().plus(timeout);
        }
        
        /**
         * Checks if the session has expired.
         *
         * @return True if the session has expired
         */
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
        
        /**
         * Gets the player ID.
         *
         * @return The player ID
         */
        public UUID getPlayerId() {
            return playerId;
        }
        
        /**
         * Gets the player name.
         *
         * @return The player name
         */
        public String getPlayerName() {
            return playerName;
        }
        
        /**
         * Gets the session expiration time.
         *
         * @return The expiration time
         */
        public Instant getExpiresAt() {
            return expiresAt;
        }
        
        /**
         * Gets the time remaining until the session expires.
         *
         * @return The time remaining
         */
        public Duration getTimeRemaining() {
            Instant now = Instant.now();
            return now.isBefore(expiresAt) ? Duration.between(now, expiresAt) : Duration.ZERO;
        }
        
        /**
         * Sets a session attribute.
         *
         * @param key The attribute key
         * @param value The attribute value
         */
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }
        
        /**
         * Gets a session attribute.
         *
         * @param key The attribute key
         * @return The attribute value, or null if not found
         */
        public Object getAttribute(String key) {
            return attributes.get(key);
        }
        
        /**
         * Gets a session attribute with a default value.
         *
         * @param key The attribute key
         * @param defaultValue The default value
         * @return The attribute value, or the default value if not found
         */
        public Object getAttribute(String key, Object defaultValue) {
            return attributes.getOrDefault(key, defaultValue);
        }
        
        /**
         * Removes a session attribute.
         *
         * @param key The attribute key
         * @return The removed attribute value, or null if not found
         */
        public Object removeAttribute(String key) {
            return attributes.remove(key);
        }
        
        /**
         * Checks if the session has a specific permission.
         *
         * @param permission The permission
         * @return True if the session has the permission
         */
        public boolean hasPermission(String permission) {
            Player player = plugin.getServer().getPlayer(playerId);
            return player != null && player.hasPermission(permission);
        }
        
        /**
         * Executes an action as the session's player.
         *
         * @param action The action to execute
         * @return True if the action was executed
         */
        public boolean execute(Consumer<Player> action) {
            Player player = plugin.getServer().getPlayer(playerId);
            
            if (player != null && player.isOnline()) {
                action.accept(player);
                refresh();
                return true;
            }
            
            return false;
        }
        
        /**
         * Invalidates the session.
         */
        public void invalidate() {
            invalidateSession(playerId);
        }
    }
}
