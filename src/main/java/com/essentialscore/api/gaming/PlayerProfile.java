package com.essentialscore.api.gaming;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

/**
 * Represents a player's profile with gaming information
 */
public class PlayerProfile {
    private final UUID playerId;
    private final String playerName;
    private final int level;
    private final Map<String, Object> attributes;
    private final LocalDateTime lastSeen;
    
    public PlayerProfile(UUID playerId, String playerName, int level) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.level = level;
        this.attributes = new HashMap<>();
        this.lastSeen = LocalDateTime.now();
    }
    
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getLevel() { return level; }
    public Map<String, Object> getAttributes() { return attributes; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}
