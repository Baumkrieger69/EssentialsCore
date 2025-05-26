package com.essentialscore.api.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects anomalies in performance metrics.
 */
public class AnomalyDetector {
    private static final Logger LOGGER = Logger.getLogger(AnomalyDetector.class.getName());
    
    private final ProfilingSystem profilingSystem;
    private final Map<String, AnomalyRule> rules;
    private final List<Anomaly> detectedAnomalies;
    private final List<Consumer<Anomaly>> anomalyListeners;
    private final Map<String, BaselineStats> metricBaselines;
    
    /**
     * Creates a new anomaly detector.
     *
     * @param profilingSystem The profiling system
     */
    public AnomalyDetector(ProfilingSystem profilingSystem) {
        this.profilingSystem = profilingSystem;
        this.rules = new ConcurrentHashMap<>();
        this.detectedAnomalies = new ArrayList<>();
        this.anomalyListeners = new ArrayList<>();
        this.metricBaselines = new ConcurrentHashMap<>();
        
        // Register default rules
        registerRule(new ThresholdRule("cpu.usage", 90, ThresholdType.GREATER_THAN));
        registerRule(new ThresholdRule("memory.usage", 90, ThresholdType.GREATER_THAN));
        registerRule(new ThresholdRule("response.time", 500, ThresholdType.GREATER_THAN));
        registerRule(new DeviationRule("tps", 3.0)); // 3 standard deviations
    }
    
    /**
     * Registers an anomaly rule.
     *
     * @param rule The rule to register
     */
    public void registerRule(AnomalyRule rule) {
        rules.put(rule.getMetricName(), rule);
    }
    
    /**
     * Unregisters an anomaly rule.
     *
     * @param metricName The metric name
     */
    public void unregisterRule(String metricName) {
        rules.remove(metricName);
    }
    
    /**
     * Adds an anomaly listener.
     *
     * @param listener The listener to add
     */
    public void addAnomalyListener(Consumer<Anomaly> listener) {
        anomalyListeners.add(listener);
    }
    
    /**
     * Removes an anomaly listener.
     *
     * @param listener The listener to remove
     */
    public void removeAnomalyListener(Consumer<Anomaly> listener) {
        anomalyListeners.remove(listener);
    }
    
    /**
     * Detects anomalies in metrics.
     */
    public void detectAnomalies() {
        try {
            Map<String, ProfilingSystem.Metric> metrics = profilingSystem.getMetrics();
            
            // Update baselines
            updateBaselines(metrics);
            
            // Check rules
            for (Map.Entry<String, AnomalyRule> entry : rules.entrySet()) {
                String metricName = entry.getKey();
                AnomalyRule rule = entry.getValue();
                
                ProfilingSystem.Metric metric = metrics.get(metricName);
                if (metric == null) continue;
                
                BaselineStats baseline = metricBaselines.get(metricName);
                if (baseline == null) continue;
                
                // Check if the rule is violated
                if (rule.isViolated(metric, baseline)) {
                    Anomaly anomaly = new Anomaly(
                        metricName,
                        metric.getValue(),
                        rule.getDescription(),
                        Instant.now(),
                        AnomalySeverity.fromDeviation(
                            Math.abs(metric.getValue() - baseline.mean) / baseline.stdDev
                        )
                    );
                    
                    // Add to detected anomalies
                    detectedAnomalies.add(anomaly);
                    
                    // Notify listeners
                    for (Consumer<Anomaly> listener : anomalyListeners) {
                        try {
                            listener.accept(anomaly);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error in anomaly listener", e);
                        }
                    }
                    
                    // Log the anomaly
                    LOGGER.warning("Anomaly detected: " + anomaly);
                }
            }
            
            // Clean up old anomalies (older than 1 hour)
            Instant cutoff = Instant.now().minus(Duration.ofHours(1));
            detectedAnomalies.removeIf(anomaly -> anomaly.getDetectedAt().isBefore(cutoff));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error detecting anomalies", e);
        }
    }
    
    /**
     * Updates metric baselines.
     *
     * @param metrics The current metrics
     */
    private void updateBaselines(Map<String, ProfilingSystem.Metric> metrics) {
        for (Map.Entry<String, ProfilingSystem.Metric> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            ProfilingSystem.Metric metric = entry.getValue();
            
            // Skip metrics with no samples
            if (metric.getSampleCount() == 0) continue;
            
            // Get or create baseline stats
            BaselineStats baseline = metricBaselines.computeIfAbsent(
                metricName, 
                k -> new BaselineStats(metric.getSampleAverage(), metric.getSampleStdDev())
            );
            
            // Update baseline stats with exponential moving average
            double alpha = 0.1; // Smoothing factor
            baseline.mean = alpha * metric.getSampleAverage() + (1 - alpha) * baseline.mean;
            baseline.stdDev = alpha * metric.getSampleStdDev() + (1 - alpha) * baseline.stdDev;
        }
    }
    
    /**
     * Gets detected anomalies.
     *
     * @return The detected anomalies
     */
    public List<Anomaly> getDetectedAnomalies() {
        return new ArrayList<>(detectedAnomalies);
    }
    
    /**
     * Gets recent anomalies.
     *
     * @param duration The duration to look back
     * @return The recent anomalies
     */
    public List<Anomaly> getRecentAnomalies(Duration duration) {
        Instant cutoff = Instant.now().minus(duration);
        List<Anomaly> recentAnomalies = new ArrayList<>();
        
        for (Anomaly anomaly : detectedAnomalies) {
            if (anomaly.getDetectedAt().isAfter(cutoff)) {
                recentAnomalies.add(anomaly);
            }
        }
        
        return recentAnomalies;
    }
    
    /**
     * Represents a baseline for a metric.
     */
    private static class BaselineStats {
        double mean;
        double stdDev;
        
        BaselineStats(double mean, double stdDev) {
            this.mean = mean;
            this.stdDev = stdDev;
        }
    }
    
    /**
     * Represents an anomaly in a metric.
     */
    public static class Anomaly {
        private final String metricName;
        private final double value;
        private final String description;
        private final Instant detectedAt;
        private final AnomalySeverity severity;
        
        /**
         * Creates a new anomaly.
         *
         * @param metricName The metric name
         * @param value The anomalous value
         * @param description The anomaly description
         * @param detectedAt The detection time
         * @param severity The anomaly severity
         */
        public Anomaly(String metricName, double value, String description, Instant detectedAt, AnomalySeverity severity) {
            this.metricName = metricName;
            this.value = value;
            this.description = description;
            this.detectedAt = detectedAt;
            this.severity = severity;
        }
        
        /**
         * Gets the metric name.
         *
         * @return The metric name
         */
        public String getMetricName() {
            return metricName;
        }
        
        /**
         * Gets the anomalous value.
         *
         * @return The anomalous value
         */
        public double getValue() {
            return value;
        }
        
        /**
         * Gets the anomaly description.
         *
         * @return The anomaly description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the detection time.
         *
         * @return The detection time
         */
        public Instant getDetectedAt() {
            return detectedAt;
        }
        
        /**
         * Gets the anomaly severity.
         *
         * @return The anomaly severity
         */
        public AnomalySeverity getSeverity() {
            return severity;
        }
        
        @Override
        public String toString() {
            return "Anomaly{" +
                   "metricName='" + metricName + '\'' +
                   ", value=" + value +
                   ", description='" + description + '\'' +
                   ", detectedAt=" + detectedAt +
                   ", severity=" + severity +
                   '}';
        }
    }
    
    /**
     * Represents the severity of an anomaly.
     */
    public enum AnomalySeverity {
        /**
         * Low severity.
         */
        LOW,
        
        /**
         * Medium severity.
         */
        MEDIUM,
        
        /**
         * High severity.
         */
        HIGH,
        
        /**
         * Critical severity.
         */
        CRITICAL;
        
        /**
         * Gets the severity from a deviation.
         *
         * @param deviation The deviation in standard deviations
         * @return The severity
         */
        public static AnomalySeverity fromDeviation(double deviation) {
            if (deviation < 2.0) {
                return LOW;
            } else if (deviation < 3.0) {
                return MEDIUM;
            } else if (deviation < 4.0) {
                return HIGH;
            } else {
                return CRITICAL;
            }
        }
    }
    
    /**
     * Interface for anomaly rules.
     */
    public interface AnomalyRule {
        /**
         * Gets the metric name for this rule.
         *
         * @return The metric name
         */
        String getMetricName();
        
        /**
         * Checks if the rule is violated.
         *
         * @param metric The metric to check
         * @param baseline The metric baseline
         * @return true if the rule is violated
         */
        boolean isViolated(ProfilingSystem.Metric metric, BaselineStats baseline);
        
        /**
         * Gets the rule description.
         *
         * @return The rule description
         */
        String getDescription();
    }
    
    /**
     * Rule that checks if a metric exceeds a threshold.
     */
    public static class ThresholdRule implements AnomalyRule {
        private final String metricName;
        private final double threshold;
        private final ThresholdType type;
        
        /**
         * Creates a new threshold rule.
         *
         * @param metricName The metric name
         * @param threshold The threshold value
         * @param type The threshold type
         */
        public ThresholdRule(String metricName, double threshold, ThresholdType type) {
            this.metricName = metricName;
            this.threshold = threshold;
            this.type = type;
        }
        
        @Override
        public String getMetricName() {
            return metricName;
        }
        
        @Override
        public boolean isViolated(ProfilingSystem.Metric metric, BaselineStats baseline) {
            double value = metric.getValue();
            
            switch (type) {
                case GREATER_THAN:
                    return value > threshold;
                case LESS_THAN:
                    return value < threshold;
                case EQUAL_TO:
                    return Math.abs(value - threshold) < 0.0001;
                default:
                    return false;
            }
        }
        
        @Override
        public String getDescription() {
            return "Metric " + metricName + " " + type.getDescription() + " " + threshold;
        }
    }
    
    /**
     * Rule that checks if a metric deviates from its baseline.
     */
    public static class DeviationRule implements AnomalyRule {
        private final String metricName;
        private final double maxDeviations;
        
        /**
         * Creates a new deviation rule.
         *
         * @param metricName The metric name
         * @param maxDeviations The maximum number of standard deviations
         */
        public DeviationRule(String metricName, double maxDeviations) {
            this.metricName = metricName;
            this.maxDeviations = maxDeviations;
        }
        
        @Override
        public String getMetricName() {
            return metricName;
        }
        
        @Override
        public boolean isViolated(ProfilingSystem.Metric metric, BaselineStats baseline) {
            double value = metric.getValue();
            double deviation = Math.abs(value - baseline.mean) / baseline.stdDev;
            
            return deviation > maxDeviations;
        }
        
        @Override
        public String getDescription() {
            return "Metric " + metricName + " deviates more than " + maxDeviations + " standard deviations from baseline";
        }
    }
    
    /**
     * Represents threshold comparison types.
     */
    public enum ThresholdType {
        /**
         * Greater than comparison.
         */
        GREATER_THAN("is greater than"),
        
        /**
         * Less than comparison.
         */
        LESS_THAN("is less than"),
        
        /**
         * Equal to comparison.
         */
        EQUAL_TO("is equal to");
        
        private final String description;
        
        ThresholdType(String description) {
            this.description = description;
        }
        
        /**
         * Gets the type description.
         *
         * @return The type description
         */
        public String getDescription() {
            return description;
        }
    }
} 