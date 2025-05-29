package com.essentialscore.api.gaming;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Represents a dynamic quest that can be generated for players
 */
public class DynamicQuest {
    private final UUID questId;
    private final String title;
    private final String description;
    private final Map<String, Object> objectives;
    private final Map<String, Object> rewards;
    private final int difficulty;
    
    public DynamicQuest(String title, String description, int difficulty) {
        this.questId = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.objectives = new HashMap<>();
        this.rewards = new HashMap<>();
    }
    
    public UUID getQuestId() { return questId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getDifficulty() { return difficulty; }
    
    public void addObjective(String key, Object objective) {
        objectives.put(key, objective);
    }
    
    public void addReward(String key, Object reward) {
        rewards.put(key, reward);
    }
    
    public Map<String, Object> getObjectives() { return objectives; }
    public Map<String, Object> getRewards() { return rewards; }
}
