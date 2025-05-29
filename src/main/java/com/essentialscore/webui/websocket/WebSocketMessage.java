package com.essentialscore.webui.websocket;

import com.google.gson.JsonElement;

/**
 * WebSocket-Nachrichtenklasse für strukturierte Kommunikation
 */
public class WebSocketMessage {
    private String type;
    private String action;
    private JsonElement data;
    private long timestamp;
    private String requestId;
    
    public WebSocketMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public WebSocketMessage(String type, String action, JsonElement data) {
        this();
        this.type = type;
        this.action = action;
        this.data = data;
    }
    
    // Getter und Setter
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public JsonElement getData() {
        return data;
    }
    
    public void setData(JsonElement data) {
        this.data = data;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
