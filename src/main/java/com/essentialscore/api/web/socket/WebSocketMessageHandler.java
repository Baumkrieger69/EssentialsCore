package com.essentialscore.api.web.socket;

import com.google.gson.JsonObject;

/**
 * Interface for handling WebSocket messages.
 */
@FunctionalInterface
public interface WebSocketMessageHandler {
    
    /**
     * Handles a WebSocket message.
     * 
     * @param session The WebSocket session
     * @param message The message data
     */
    void handle(WebSocketSession session, JsonObject message);
} 