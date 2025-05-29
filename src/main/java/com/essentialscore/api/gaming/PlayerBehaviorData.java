package com.essentialscore.api.gaming;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

/**
 * Represents behavioral data for a player
 */
public class PlayerBehaviorData {
    private final Map<String, Integer> actionCounts;
    private final Map<String, LocalDateTime> lastActions;
    private final Map<String, Double> preferences;
    
    public PlayerBehaviorData() {
        this.actionCounts = new HashMap<>();
        this.lastActions = new HashMap<>();
        this.preferences = new HashMap<>();
    }
    
    public void recordAction(String action) {
        actionCounts.put(action, actionCounts.getOrDefault(action, 0) + 1);
        lastActions.put(action, LocalDateTime.now());
    }
    
    public int getActionCount(String action) {
        return actionCounts.getOrDefault(action, 0);
    }
    
    public LocalDateTime getLastAction(String action) {
        return lastActions.get(action);
    }
    
    public void setPreference(String key, double value) {
        preferences.put(key, value);
    }
    
    public double getPreference(String key) {
        return preferences.getOrDefault(key, 0.0);
    }
    
    public Map<String, Integer> getActionCounts() { return actionCounts; }
    public Map<String, LocalDateTime> getLastActions() { return lastActions; }
    public Map<String, Double> getPreferences() { return preferences; }
}
