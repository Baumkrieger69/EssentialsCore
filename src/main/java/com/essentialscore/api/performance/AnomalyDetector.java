package com.essentialscore.api.performance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Detects performance anomalies in module behavior.
 */
public class AnomalyDetector {
    private static final Logger LOGGER = Logger.getLogger(AnomalyDetector.class.getName());
    
    private final Map<String, List<Anomaly>> recentAnomalies;
    private final Map<String, MetricBaseline> baselines;
    
    public AnomalyDetector() {
        this.recentAnomalies = new ConcurrentHashMap<>();
        this.baselines = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets recent anomalies within the specified duration.
     */
    public List<Anomaly> getRecentAnomalies(Duration duration) {
        LocalDateTime cutoff = LocalDateTime.now().minus(duration);
        List<Anomaly> result = new ArrayList<>();
        
        for (List<Anomaly> anomalies : recentAnomalies.values()) {
            for (Anomaly anomaly : anomalies) {
                if (anomaly.getTimestamp().isAfter(cutoff)) {
                    result.add(anomaly);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Detects anomalies in a metric value.
     */
    public void detectAnomalies(String metricName, double value, String moduleId) {
        MetricBaseline baseline = baselines.get(metricName);
        if (baseline == null) {
            baseline = new MetricBaseline();
            baselines.put(metricName, baseline);
        }
        
        baseline.addValue(value);
        
        // Check for anomalies
        if (baseline.isAnomaly(value)) {
            Anomaly anomaly = new Anomaly(
                metricName,
                determineAnomalyType(metricName, value, baseline),
                value > baseline.getAverage() ? Anomaly.Severity.HIGH : Anomaly.Severity.MEDIUM,
                "Metric value " + value + " deviates from baseline " + baseline.getAverage(),
                moduleId,
                Map.of("currentValue", value, "baseline", baseline.getAverage())
            );
            
            addAnomaly(metricName, anomaly);
        }
    }
    
    private AnomalyType determineAnomalyType(String metricName, double value, MetricBaseline baseline) {
        if (metricName.contains("response") || metricName.contains("time")) {
            return AnomalyType.RESPONSE_TIME_SPIKE;
        } else if (metricName.contains("memory")) {
            return AnomalyType.MEMORY_LEAK;
        } else if (metricName.contains("cpu")) {
            return AnomalyType.CPU_SPIKE;
        } else {
            return AnomalyType.PERFORMANCE_DEGRADATION;
        }
    }
    
    private void addAnomaly(String metricName, Anomaly anomaly) {
        recentAnomalies.computeIfAbsent(metricName, k -> new ArrayList<>()).add(anomaly);
        LOGGER.warning("Anomaly detected: " + anomaly.getDescription());
    }
    
    /**
     * Represents a detected anomaly.
     */
    public static class Anomaly {
        private final String metricName;
        private final AnomalyType type;
        private final Severity severity;
        private final String description;
        private final String moduleId;
        private final Map<String, Object> details;
        private final LocalDateTime timestamp;
        
        public Anomaly(String metricName, AnomalyType type, Severity severity, 
                      String description, String moduleId, Map<String, Object> details) {
            this.metricName = metricName;
            this.type = type;
            this.severity = severity;
            this.description = description;
            this.moduleId = moduleId;
            this.details = details;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getMetricName() { return metricName; }
        public AnomalyType getType() { return type; }
        public Severity getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getModuleId() { return moduleId; }
        public Map<String, Object> getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        /**
         * Severity levels for anomalies.
         */
        public enum Severity {
            LOW, MEDIUM, HIGH, CRITICAL
        }
    }
    
    /**
     * Types of anomalies that can be detected.
     */
    public enum AnomalyType {
        RESPONSE_TIME_SPIKE,
        MEMORY_LEAK,
        CPU_SPIKE,
        PERFORMANCE_DEGRADATION,
        ERROR_RATE_INCREASE,
        RESOURCE_EXHAUSTION
    }
    
    /**
     * Baseline metrics for anomaly detection.
     */
    private static class MetricBaseline {
        private final List<Double> values = new ArrayList<>();
        private double average = 0.0;
        private double standardDeviation = 0.0;
        
        public void addValue(double value) {
            values.add(value);
            if (values.size() > 100) {
                values.remove(0); // Keep only recent values
            }
            updateStatistics();
        }
        
        private void updateStatistics() {
            if (values.isEmpty()) return;
            
            average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            if (values.size() > 1) {
                double variance = values.stream()
                    .mapToDouble(v -> Math.pow(v - average, 2))
                    .average().orElse(0.0);
                standardDeviation = Math.sqrt(variance);
            }
        }
        
        public boolean isAnomaly(double value) {
            if (values.size() < 10) return false; // Need baseline
            return Math.abs(value - average) > 2 * standardDeviation;
        }
        
        public double getAverage() { return average; }
        @SuppressWarnings("unused")
        public double getStandardDeviation() { return standardDeviation; }
    }
}
