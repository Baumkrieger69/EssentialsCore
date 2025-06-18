package com.essentialscore.api.performance;

import java.util.*;

/**
 * Missing performance monitoring classes
 */

// AnomalyDetector class moved to separate file: com.essentialscore.api.performance.AnomalyDetector

class PredictiveScaler {
    private final Map<String, List<Double>> historicalData;
    private final int windowSize;
    
    public PredictiveScaler(int windowSize) {
        this.historicalData = new HashMap<>();
        this.windowSize = windowSize;
    }
    
    public double predictNextValue(String metric) {
        List<Double> history = historicalData.get(metric);
        if (history == null || history.isEmpty()) {
            return 0.0;
        }
        
        // Simple moving average prediction
        double sum = history.stream().mapToDouble(Double::doubleValue).sum();
        return sum / history.size();
    }
    
    public void addDataPoint(String metric, double value) {
        List<Double> history = historicalData.computeIfAbsent(metric, k -> new ArrayList<>());
        history.add(value);
        
        // Keep only the last windowSize values
        if (history.size() > windowSize) {
            history.remove(0);
        }
    }
    
    public void updateMetrics(ServerMetrics serverMetrics, Map<String, ModuleProfile> moduleProfiles) {
        // Update historical data with current server metrics
        addDataPoint("cpu_usage", serverMetrics.getCpuLoad());
        addDataPoint("memory_usage", serverMetrics.getMemoryUsage());
        addDataPoint("tps", serverMetrics.getTps());
        
        // Update data for each module based on their profiles
        moduleProfiles.forEach((moduleId, profile) -> {
            if (profile.getDataPointCount() > 0) {
                // Add module-specific metrics to historical data
                addDataPoint(moduleId + "_cpu", profile.getAverageCpuUsage());
                addDataPoint(moduleId + "_memory", profile.getAverageMemoryUsage());
                addDataPoint(moduleId + "_response_time", profile.getAverageResponseTime());
            }
        });
    }
    
    public ScalingRecommendation getScalingRecommendation(String resourceType) {
        double predicted = predictNextValue(resourceType);
        double current = getCurrentValue(resourceType);
        
        if (predicted > current * 1.2) {
            return new ScalingRecommendation(ScalingAction.SCALE_UP, predicted);
        } else if (predicted < current * 0.8) {
            return new ScalingRecommendation(ScalingAction.SCALE_DOWN, predicted);
        } else {
            return new ScalingRecommendation(ScalingAction.MAINTAIN, predicted);
        }
    }
    
    private double getCurrentValue(String resourceType) {
        List<Double> history = historicalData.get(resourceType);
        if (history == null || history.isEmpty()) {
            return 0.0;
        }
        return history.get(history.size() - 1);
    }
    
    public static class ScalingRecommendation {
        private final ScalingAction action;
        private final double predictedValue;
        
        public ScalingRecommendation(ScalingAction action, double predictedValue) {
            this.action = action;
            this.predictedValue = predictedValue;
        }
        
        public ScalingAction getAction() { return action; }
        public double getPredictedValue() { return predictedValue; }
    }
    
    public enum ScalingAction {
        SCALE_UP, SCALE_DOWN, MAINTAIN
    }
}

// Configuration classes
class Configuration {
    private final Map<String, Object> properties;
    
    public Configuration() {
        this.properties = new HashMap<>();
    }
    
    public Configuration(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }
    
    public Object get(String key) {
        return properties.get(key);
    }
    
    public String getString(String key, String defaultValue) {
        Object value = properties.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    public int getInt(String key, int defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public void set(String key, Object value) {
        properties.put(key, value);
    }
}
