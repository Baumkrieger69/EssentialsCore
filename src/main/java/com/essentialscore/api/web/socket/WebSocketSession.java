package com.essentialscore.api.web.socket;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a WebSocket session.
 */
public class WebSocketSession {
    private final String sessionId;
    private final String resourceDescriptor;
    private boolean authenticated;
    private String username;
    private final long createdAt;
    private Consumer<String> messageSender;
    
    /**
     * Creates a new WebSocket session
     * 
     * @param sessionId The session ID
     * @param resourceDescriptor The resource descriptor
     */
    public WebSocketSession(String sessionId, String resourceDescriptor) {
        this.sessionId = sessionId;
        this.resourceDescriptor = resourceDescriptor;
        this.authenticated = false;
        this.username = null;
        this.createdAt = System.currentTimeMillis();
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
     * Gets the resource descriptor
     * 
     * @return The resource descriptor
     */
    public String getResourceDescriptor() {
        return resourceDescriptor;
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
     * Sets whether the session is authenticated
     * 
     * @param authenticated true if authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
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
     * Sets the username
     * 
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the time when this session was created
     * 
     * @return The creation time
     */
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the message sender function for this session
     * 
     * @param messageSender A function that sends messages to the client
     */
    public void setMessageSender(Consumer<String> messageSender) {
        this.messageSender = messageSender;
    }
    
    /**
     * Sends a message to the client
     * 
     * @param message The message to send
     */
    public void sendMessage(String message) {
        if (messageSender != null) {
            messageSender.accept(message);
        }
    }
    
    /**
     * Checks if the session is open
     * 
     * @return true if the session is open
     */
    public boolean isOpen() {
        return messageSender != null;
    }
} 