package com.essentialscore.api.gaming;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents interaction patterns between players
 */
public class InteractionPattern {
    private final String patternType;
    private final Map<String, Object> patternData;
    private final double frequency;
    
    public InteractionPattern(String patternType, double frequency) {
        this.patternType = patternType;
        this.frequency = frequency;
        this.patternData = new HashMap<>();
    }
    
    public String getPatternType() { return patternType; }
    public double getFrequency() { return frequency; }
    
    public void setPatternData(String key, Object value) {
        patternData.put(key, value);
    }
    
    public Object getPatternData(String key) {
        return patternData.get(key);
    }
    
    public Map<String, Object> getAllPatternData() {
        return new HashMap<>(patternData);
    }
}
