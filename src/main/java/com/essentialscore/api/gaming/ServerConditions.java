package com.essentialscore.api.gaming;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;

/**
 * Represents server conditions and state
 */
public class ServerConditions {
    private final Map<String, Object> conditions;
    private final int playerCount;
    private final double serverLoad;
    private final boolean isPeakTime;
    private final LocalDateTime timeOfDay;
    private final Set<GameEvent> activeEvents;
    private final Map<String, Double> economyMetrics;
    
    public ServerConditions(int playerCount, double serverLoad, boolean isPeakTime) {
        this.conditions = new HashMap<>();
        this.playerCount = playerCount;
        this.serverLoad = serverLoad;
        this.isPeakTime = isPeakTime;
        this.timeOfDay = LocalDateTime.now();
        this.activeEvents = new HashSet<>();
        this.economyMetrics = new HashMap<>();
    }
    
    // Default constructor for compatibility
    public ServerConditions() {
        this.conditions = new HashMap<>();
        this.playerCount = 0;
        this.serverLoad = 0.0;
        this.isPeakTime = false;
        this.timeOfDay = LocalDateTime.now();
        this.activeEvents = new HashSet<>();
        this.economyMetrics = new HashMap<>();
    }
    
    public int getPlayerCount() { return playerCount; }
    public double getServerLoad() { return serverLoad; }
    public boolean isPeakTime() { return isPeakTime; }
    public LocalDateTime getTimeOfDay() { return timeOfDay; }
    public Set<GameEvent> getActiveEvents() { return activeEvents; }
    public Map<String, Double> getEconomyMetrics() { return economyMetrics; }
    
    public void setCondition(String key, Object value) {
        conditions.put(key, value);
    }
    
    public Object getCondition(String key) {
        return conditions.get(key);
    }
    
    public Map<String, Object> getAllConditions() {
        return new HashMap<>(conditions);
    }
}
