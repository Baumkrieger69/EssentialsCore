package com.essentialscore.api.web;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Represents an active WebUI session with authentication details.
 * This class manages session state, permissions, and message sending.
 */
public class WebSession {
    private final String sessionId;
    private final String username;
    private final String ipAddress;
    private final UUID playerUUID;
    private Instant lastActivity;
    private final Map<String, Object> attributes;
    private Consumer<Map<String, Object>> messageSender;
    private boolean authenticated;
    private final Instant createdAt;
    
    /**
     * Creates a new WebSession
     * 
     * @param sessionId The unique session ID
     * @param username The username associated with the session
     * @param ipAddress The IP address of the client
     * @param playerUUID The UUID of the Minecraft player (may be null for API-only users)
     */
    public WebSession(String sessionId, String username, String ipAddress, UUID playerUUID) {
        this.sessionId = sessionId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.playerUUID = playerUUID;
        this.lastActivity = Instant.now();
        this.createdAt = Instant.now();
        this.attributes = new ConcurrentHashMap<>();
        this.authenticated = false;
    }
    
    /**
     * Gets the session ID
     * 
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
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
     * Gets the IP address
     * 
     * @return The IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * Gets the player UUID
     * 
     * @return The player UUID, or null if this is an API-only user
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Gets the last activity time
     * 
     * @return The last activity time
     */
    public Instant getLastActivity() {
        return lastActivity;
    }
    
    /**
     * Updates the last activity time to now
     */
    public void updateLastActivity() {
        this.lastActivity = Instant.now();
    }
    
    /**
     * Gets the time when this session was created
     * 
     * @return The creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets a session attribute
     * 
     * @param key The attribute key
     * @param value The attribute value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * Gets a session attribute
     * 
     * @param key The attribute key
     * @return The attribute value, or null if not set
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * Gets a session attribute with a default value
     * 
     * @param key The attribute key
     * @param defaultValue The default value if the attribute is not set
     * @return The attribute value, or the default value if not set
     */
    public Object getAttribute(String key, Object defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }
    
    /**
     * Removes a session attribute
     * 
     * @param key The attribute key
     * @return The removed value, or null if not set
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }
    
    /**
     * Sets the message sender function for this session
     * 
     * @param messageSender A function that sends messages to the client
     */
    public void setMessageSender(Consumer<Map<String, Object>> messageSender) {
        this.messageSender = messageSender;
    }
    
    /**
     * Sends a message to the client
     * 
     * @param event The event name
     * @param data The event data
     */
    public void sendMessage(String event, Map<String, Object> data) {
        if (messageSender == null) {
            return;
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("event", event);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());
        
        messageSender.accept(message);
        updateLastActivity();
    }
    
    /**
     * Checks if the session is authenticated
     * 
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Sets the authenticated state
     * 
     * @param authenticated true if authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    /**
     * Checks if the session has expired
     * 
     * @param timeoutMinutes The session timeout in minutes
     * @return true if the session has expired
     */
    public boolean isExpired(int timeoutMinutes) {
        return lastActivity.plusSeconds(timeoutMinutes * 60).isBefore(Instant.now());
    }
    
    /**
     * Gets a summary of this session for reporting
     * 
     * @return A map containing session summary information
     */
    public Map<String, Object> getSessionSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("sessionId", sessionId);
        summary.put("username", username);
        summary.put("ipAddress", ipAddress);
        summary.put("authenticated", authenticated);
        summary.put("lastActivity", lastActivity.toString());
        summary.put("createdAt", createdAt.toString());
        summary.put("attributeCount", attributes.size());
        
        if (playerUUID != null) {
            summary.put("playerUUID", playerUUID.toString());
        }
        
        return summary;
    }
} 