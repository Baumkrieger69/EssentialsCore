package com.essentialscore.api.gaming;

import org.bukkit.entity.Player;
import java.util.*;

/**
 * Additional missing types for gaming functionality
 */

// Difficulty levels
enum DifficultyLevel {
    EASY(1.0),
    NORMAL(1.5),
    HARD(2.0),
    EXPERT(2.5);
    
    private final double multiplier;
    
    DifficultyLevel(double multiplier) {
        this.multiplier = multiplier;
    }
    
    public double getMultiplier() { return multiplier; }
}

// Play styles
enum PlayStyle {
    CASUAL, COMPETITIVE, SOCIAL, EXPLORER, BUILDER, FIGHTER
}

// Game preferences
enum GamePreference {
    PVP, PVE, BUILDING, EXPLORATION, TRADING, SOCIAL
}

// Quest generation context
class QuestGenerationContext {
    private final Player player;
    private final PlayStyle playStyle;
    private final Set<GamePreference> preferences;
    private final DifficultyLevel difficulty;
    
    public QuestGenerationContext(Player player, PlayStyle playStyle, DifficultyLevel difficulty) {
        this.player = player;
        this.playStyle = playStyle;
        this.difficulty = difficulty;
        this.preferences = new HashSet<>();
    }
    
    public Player getPlayer() { return player; }
    public PlayStyle getPlayStyle() { return playStyle; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public Set<GamePreference> getPreferences() { return preferences; }
}

// Social context data
class SocialContextData {
    private final Map<String, Object> data;
    
    public SocialContextData() {
        this.data = new HashMap<>();
    }
    
    public Object getData(String key) { return data.get(key); }
    public void setData(String key, Object value) { data.put(key, value); }
}

// Weather pattern
class WeatherPattern {
    private final String type;
    private final double intensity;
    
    public WeatherPattern(String type, double intensity) {
        this.type = type;
        this.intensity = intensity;
    }
    
    public String getType() { return type; }
    public double getIntensity() { return intensity; }
}

// Economic transaction
class EconomicTransaction {
    private final UUID playerId;
    private final String type;
    private final double amount;
    private final long timestamp;
    
    public EconomicTransaction(UUID playerId, String type, double amount) {
        this.playerId = playerId;
        this.type = type;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }
    
    public UUID getPlayerId() { return playerId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
}

// Economy metrics
class EconomyMetrics {
    private final double inflation;
    private final double averageWealth;
    private final Map<String, Double> resourcePrices;
    
    public EconomyMetrics(double inflation, double averageWealth) {
        this.inflation = inflation;
        this.averageWealth = averageWealth;
        this.resourcePrices = new HashMap<>();
    }
    
    public double getInflation() { return inflation; }
    public double getAverageWealth() { return averageWealth; }
    public Map<String, Double> getResourcePrices() { return resourcePrices; }
}

// Behavior event
class BehaviorEvent {
    private final String action;
    private final Map<String, Object> context;
    private final long timestamp;
    
    public BehaviorEvent(String action) {
        this.action = action;
        this.context = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getAction() { return action; }
    public Map<String, Object> getContext() { return context; }
    public long getTimestamp() { return timestamp; }
}