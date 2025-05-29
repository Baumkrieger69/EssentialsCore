package com.essentialscore.api.gaming;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents server conditions and state
 */
public class ServerConditions {
    private final Map<String, Object> conditions;
    private final int playerCount;
    private final double serverLoad;
    private final boolean isPeakTime;
    
    public ServerConditions(int playerCount, double serverLoad, boolean isPeakTime) {
        this.conditions = new HashMap<>();
        this.playerCount = playerCount;
        this.serverLoad = serverLoad;
        this.isPeakTime = isPeakTime;
    }
    
    public int getPlayerCount() { return playerCount; }
    public double getServerLoad() { return serverLoad; }
    public boolean isPeakTime() { return isPeakTime; }
    
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
