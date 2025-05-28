package com.essentialscore.api.performance;

import org.bukkit.Bukkit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects anomalies in module behavior and performance.
 */
public class AnomalyDetector {
    private static final Logger LOGGER = Logger.getLogger(AnomalyDetector.class.getName());
    
    private final ModuleOptimizer optimizer;
    private final ScheduledExecutorService scheduler;
    private final Map<String, AnomalyDetectionModel> models;
    private final List<AnomalyListener> listeners;
    private final Map<String, List<Anomaly>> detectedAnomalies;
    
    private boolean running = false;
    private int detectionInterval = 60; // seconds
    
    /**
     * Creates a new anomaly detector.
     *
     * @param optimizer The module optimizer
     */
    public AnomalyDetector(ModuleOptimizer optimizer) {
        this.optimizer = optimizer;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.models = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.detectedAnomalies = new ConcurrentHashMap<>();
    }
    
    /**
     * Starts the anomaly detector.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting anomaly detection service");
        
        // Schedule anomaly detection
        scheduler.scheduleAtFixedRate(
            this::detectAnomalies,
            detectionInterval,
            detectionInterval,
            TimeUnit.SECONDS
        );
        
        running = true;
    }
    
    /**
     * Stops the anomaly detector.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping anomaly detection service");
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        running = false;
    }
    
    /**
     * Adds an anomaly listener.
     *
     * @param listener The listener to add
     */
    public void addListener(AnomalyListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes an anomaly listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(AnomalyListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Detects anomalies across all modules.
     */
    private void detectAnomalies() {
        try {
            LOGGER.fine("Running anomaly detection");
            
            // Get current server metrics
            ServerMetrics serverMetrics = optimizer.getPerformanceMonitor().getCurrentMetrics();
            
            // Check each module
            optimizer.getModuleRegistry().getModules().forEach((moduleId, module) -> {
                try {
                    ModuleProfile profile = optimizer.getModuleProfiles().get(moduleId);
                    if (profile == null || profile.getDataPointCount() < 10) {
                        // Not enough data yet
                        return;
                    }
                    
                    // Get or create model
                    AnomalyDetectionModel model = models.computeIfAbsent(
                        moduleId,
                        k -> new AnomalyDetectionModel(moduleId)
                    );
                    
                    // Update model with latest data
                    model.update(profile);
                    
                    // Detect anomalies
                    List<Anomaly> anomalies = model.detectAnomalies(profile, serverMetrics);
                    
                    if (!anomalies.isEmpty()) {
                        // Store anomalies
                        detectedAnomalies.computeIfAbsent(moduleId, k -> new ArrayList<>())
                            .addAll(anomalies);
                        
                        // Notify listeners
                        for (Anomaly anomaly : anomalies) {
                            notifyListeners(anomaly);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error detecting anomalies for module " + moduleId, e);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error in anomaly detection", e);
        }
    }
    
    /**
     * Checks for anomalies in a specific module.
     *
     * @param moduleId The module ID
     * @param moduleMetrics The module metrics
     * @param serverMetrics The server metrics
     */
    public void checkForAnomalies(String moduleId, ModuleMetrics moduleMetrics, ServerMetrics serverMetrics) {
        try {
            ModuleProfile profile = optimizer.getModuleProfiles().get(moduleId);
            if (profile == null || profile.getDataPointCount() < 10) {
                // Not enough data yet
                return;
            }
            
            // Get or create model
            AnomalyDetectionModel model = models.computeIfAbsent(
                moduleId,
                k -> new AnomalyDetectionModel(moduleId)
            );
            
            // Perform real-time anomaly detection
            List<Anomaly> anomalies = model.detectRealTimeAnomalies(moduleMetrics, serverMetrics);
            
            if (!anomalies.isEmpty()) {
                // Store anomalies
                detectedAnomalies.computeIfAbsent(moduleId, k -> new ArrayList<>())
                    .addAll(anomalies);
                
                // Notify listeners
                for (Anomaly anomaly : anomalies) {
                    notifyListeners(anomaly);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking for anomalies in module " + moduleId, e);
        }
    }
    
    /**
     * Notifies listeners of an anomaly.
     *
     * @param anomaly The anomaly
     */
    private void notifyListeners(Anomaly anomaly) {
        for (AnomalyListener listener : listeners) {
            try {
                listener.onAnomalyDetected(anomaly);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying anomaly listener", e);
            }
        }
        
        // Log the anomaly
        String message = String.format(
            "Anomaly detected in module %s: %s (severity: %s, score: %.2f)",
            anomaly.getModuleId(),
            anomaly.getType(),
            anomaly.getSeverity(),
            anomaly.getAnomalyScore()
        );
        
        switch (anomaly.getSeverity()) {
            case HIGH:
                LOGGER.warning(message);
                break;
            case MEDIUM:
                LOGGER.info(message);
                break;
            case LOW:
                LOGGER.fine(message);
                break;
        }
        
        // Trigger self-healing if appropriate
        if (anomaly.getSeverity() == Anomaly.Severity.HIGH) {
            optimizer.getSelfHealingManager().triggerHealing(anomaly);
        }
    }
    
    /**
     * Gets the detected anomalies for a module.
     *
     * @param moduleId The module ID
     * @return The detected anomalies
     */
    public List<Anomaly> getDetectedAnomalies(String moduleId) {
        return detectedAnomalies.getOrDefault(moduleId, new ArrayList<>());
    }
    
    /**
     * Gets all detected anomalies.
     *
     * @return The detected anomalies
     */
    public Map<String, List<Anomaly>> getAllDetectedAnomalies() {
        return detectedAnomalies;
    }
    
    /**
     * Interface for anomaly listeners.
     */
    public interface AnomalyListener {
        /**
         * Called when an anomaly is detected.
         *
         * @param anomaly The anomaly
         */
        void onAnomalyDetected(Anomaly anomaly);
    }
    
    /**
     * Class representing an anomaly.
     */
    public static class Anomaly {
        private final String moduleId;
        private final AnomalyType type;
        private final Severity severity;
        private final double anomalyScore;
        private final long timestamp;
        private final Map<String, Object> details;
        
        /**
         * Creates a new anomaly.
         *
         * @param moduleId The module ID
         * @param type The anomaly type
         * @param severity The severity
         * @param anomalyScore The anomaly score
         * @param details The details
         */
        public Anomaly(String moduleId, AnomalyType type, Severity severity, double anomalyScore, Map<String, Object> details) {
            this.moduleId = moduleId;
            this.type = type;
            this.severity = severity;
            this.anomalyScore = anomalyScore;
            this.timestamp = System.currentTimeMillis();
            this.details = details;
        }
        
        /**
         * Gets the module ID.
         *
         * @return The module ID
         */
        public String getModuleId() {
            return moduleId;
        }
        
        /**
         * Gets the anomaly type.
         *
         * @return The anomaly type
         */
        public AnomalyType getType() {
            return type;
        }
        
        /**
         * Gets the severity.
         *
         * @return The severity
         */
        public Severity getSeverity() {
            return severity;
        }
        
        /**
         * Gets the anomaly score.
         *
         * @return The anomaly score
         */
        public double getAnomalyScore() {
            return anomalyScore;
        }
        
        /**
         * Gets the timestamp.
         *
         * @return The timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Gets the details.
         *
         * @return The details
         */
        public Map<String, Object> getDetails() {
            return details;
        }
        
        /**
         * Enum for anomaly types.
         */
        public enum AnomalyType {
            RESPONSE_TIME_SPIKE,
            MEMORY_LEAK,
            CPU_USAGE_SPIKE,
            ERROR_RATE_SPIKE,
            RESOURCE_EXHAUSTION,
            DEADLOCK,
            CONFIGURATION_ISSUE
        }
        
        /**
         * Enum for severity levels.
         */
        public enum Severity {
            LOW,
            MEDIUM,
            HIGH
        }
    }
    
    /**
     * Class representing an anomaly detection model.
     */
    private static class AnomalyDetectionModel {
        private final String moduleId;
        
        // Statistics for anomaly detection
        private double avgResponseTime = 0;
        private double stdDevResponseTime = 0;
        private double avgCpuUsage = 0;
        private double stdDevCpuUsage = 0;
        private double avgMemoryUsage = 0;
        private double stdDevMemoryUsage = 0;
        private double avgErrorRate = 0;
        private double stdDevErrorRate = 0;
        
        /**
         * Creates a new anomaly detection model.
         *
         * @param moduleId The module ID
         */
        public AnomalyDetectionModel(String moduleId) {
            this.moduleId = moduleId;
        }
        
        /**
         * Updates the model with the latest profile data.
         *
         * @param profile The module profile
         */
        public void update(ModuleProfile profile) {
            List<ModuleProfile.DataPoint> dataPoints = profile.getDataPoints();
            if (dataPoints.isEmpty()) return;
            
            // Calculate average and standard deviation for key metrics
            
            // Response time
            double[] responseTimes = dataPoints.stream()
                .mapToDouble(ModuleProfile.DataPoint::getResponseTime)
                .toArray();
            avgResponseTime = calculateMean(responseTimes);
            stdDevResponseTime = calculateStdDev(responseTimes, avgResponseTime);
            
            // CPU usage
            double[] cpuUsages = dataPoints.stream()
                .mapToDouble(ModuleProfile.DataPoint::getModuleCpuUsage)
                .toArray();
            avgCpuUsage = calculateMean(cpuUsages);
            stdDevCpuUsage = calculateStdDev(cpuUsages, avgCpuUsage);
            
            // Memory usage
            double[] memoryUsages = dataPoints.stream()
                .mapToDouble(ModuleProfile.DataPoint::getModuleMemoryUsage)
                .toArray();
            avgMemoryUsage = calculateMean(memoryUsages);
            stdDevMemoryUsage = calculateStdDev(memoryUsages, avgMemoryUsage);
            
            // Error rate
            double[] errorRates = dataPoints.stream()
                .mapToDouble(dp -> dp.getErrorCount())
                .toArray();
            avgErrorRate = calculateMean(errorRates);
            stdDevErrorRate = calculateStdDev(errorRates, avgErrorRate);
        }
        
        /**
         * Detects anomalies in the module profile.
         *
         * @param profile The module profile
         * @param serverMetrics The server metrics
         * @return The detected anomalies
         */
        public List<Anomaly> detectAnomalies(ModuleProfile profile, ServerMetrics serverMetrics) {
            List<Anomaly> anomalies = new ArrayList<>();
            
            // Get recent data points
            List<ModuleProfile.DataPoint> recentPoints = profile.getDataPoints();
            if (recentPoints.isEmpty()) return anomalies;
            
            // Get the most recent data point
            ModuleProfile.DataPoint latest = recentPoints.get(recentPoints.size() - 1);
            
            // Check for response time anomalies
            double zScoreResponseTime = calculateZScore(latest.getResponseTime(), avgResponseTime, stdDevResponseTime);
            if (zScoreResponseTime > 3.0) {
                Map<String, Object> details = new HashMap<>();
                details.put("currentValue", latest.getResponseTime());
                details.put("averageValue", avgResponseTime);
                details.put("zScore", zScoreResponseTime);
                
                Anomaly.Severity severity = zScoreResponseTime > 5.0 ? Anomaly.Severity.HIGH :
                                        zScoreResponseTime > 4.0 ? Anomaly.Severity.MEDIUM :
                                        Anomaly.Severity.LOW;
                
                anomalies.add(new Anomaly(
                    moduleId,
                    Anomaly.AnomalyType.RESPONSE_TIME_SPIKE,
                    severity,
                    zScoreResponseTime,
                    details
                ));
            }
            
            // Check for CPU usage anomalies
            double zScoreCpuUsage = calculateZScore(latest.getModuleCpuUsage(), avgCpuUsage, stdDevCpuUsage);
            if (zScoreCpuUsage > 3.0) {
                Map<String, Object> details = new HashMap<>();
                details.put("currentValue", latest.getModuleCpuUsage());
                details.put("averageValue", avgCpuUsage);
                details.put("zScore", zScoreCpuUsage);
                
                Anomaly.Severity severity = zScoreCpuUsage > 5.0 ? Anomaly.Severity.HIGH :
                                        zScoreCpuUsage > 4.0 ? Anomaly.Severity.MEDIUM :
                                        Anomaly.Severity.LOW;
                
                anomalies.add(new Anomaly(
                    moduleId,
                    Anomaly.AnomalyType.CPU_USAGE_SPIKE,
                    severity,
                    zScoreCpuUsage,
                    details
                ));
            }
            
            // Check for memory usage anomalies (potential memory leaks)
            // For memory leaks, we look for a consistent upward trend
            if (recentPoints.size() >= 10) {
                boolean potentialMemoryLeak = true;
                double memoryGrowth = 0;
                
                for (int i = 1; i < 10; i++) {
                    ModuleProfile.DataPoint current = recentPoints.get(recentPoints.size() - i);
                    ModuleProfile.DataPoint previous = recentPoints.get(recentPoints.size() - i - 1);
                    
                    if (current.getModuleMemoryUsage() <= previous.getModuleMemoryUsage()) {
                        potentialMemoryLeak = false;
                        break;
                    }
                    
                    memoryGrowth += (current.getModuleMemoryUsage() - previous.getModuleMemoryUsage());
                }
                
                if (potentialMemoryLeak && memoryGrowth > 0) {
                    double avgGrowthRate = memoryGrowth / 9; // 10 points = 9 intervals
                    double zScoreMemoryGrowth = avgGrowthRate / (stdDevMemoryUsage > 0 ? stdDevMemoryUsage : 1);
                    
                    if (zScoreMemoryGrowth > 2.0) {
                        Map<String, Object> details = new HashMap<>();
                        details.put("averageGrowthRate", avgGrowthRate);
                        details.put("totalGrowth", memoryGrowth);
                        details.put("zScore", zScoreMemoryGrowth);
                        
                        Anomaly.Severity severity = zScoreMemoryGrowth > 4.0 ? Anomaly.Severity.HIGH :
                                                zScoreMemoryGrowth > 3.0 ? Anomaly.Severity.MEDIUM :
                                                Anomaly.Severity.LOW;
                        
                        anomalies.add(new Anomaly(
                            moduleId,
                            Anomaly.AnomalyType.MEMORY_LEAK,
                            severity,
                            zScoreMemoryGrowth,
                            details
                        ));
                    }
                }
            }
            
            // Check for error rate anomalies
            double zScoreErrorRate = calculateZScore(latest.getErrorCount(), avgErrorRate, stdDevErrorRate);
            if (zScoreErrorRate > 3.0 || latest.getErrorCount() > 0) {
                Map<String, Object> details = new HashMap<>();
                details.put("currentValue", latest.getErrorCount());
                details.put("averageValue", avgErrorRate);
                details.put("zScore", zScoreErrorRate);
                
                Anomaly.Severity severity = zScoreErrorRate > 5.0 ? Anomaly.Severity.HIGH :
                                        zScoreErrorRate > 4.0 ? Anomaly.Severity.MEDIUM :
                                        Anomaly.Severity.LOW;
                
                anomalies.add(new Anomaly(
                    moduleId,
                    Anomaly.AnomalyType.ERROR_RATE_SPIKE,
                    severity,
                    zScoreErrorRate,
                    details
                ));
            }
            
            return anomalies;
        }
        
        /**
         * Detects anomalies in real-time metrics.
         *
         * @param moduleMetrics The module metrics
         * @param serverMetrics The server metrics
         * @return The detected anomalies
         */
        public List<Anomaly> detectRealTimeAnomalies(ModuleMetrics moduleMetrics, ServerMetrics serverMetrics) {
            List<Anomaly> anomalies = new ArrayList<>();
            
            // Check for response time anomalies
            double responseTime = moduleMetrics.getAverageResponseTime();
            double zScoreResponseTime = calculateZScore(responseTime, avgResponseTime, stdDevResponseTime);
            if (zScoreResponseTime > 3.0) {
                Map<String, Object> details = new HashMap<>();
                details.put("currentValue", responseTime);
                details.put("averageValue", avgResponseTime);
                details.put("zScore", zScoreResponseTime);
                
                Anomaly.Severity severity = zScoreResponseTime > 5.0 ? Anomaly.Severity.HIGH :
                                        zScoreResponseTime > 4.0 ? Anomaly.Severity.MEDIUM :
                                        Anomaly.Severity.LOW;
                
                anomalies.add(new Anomaly(
                    moduleId,
                    Anomaly.AnomalyType.RESPONSE_TIME_SPIKE,
                    severity,
                    zScoreResponseTime,
                    details
                ));
            }
            
            // Check for CPU usage anomalies
            double cpuUsage = moduleMetrics.getCpuUsage();
            double zScoreCpuUsage = calculateZScore(cpuUsage, avgCpuUsage, stdDevCpuUsage);
            if (zScoreCpuUsage > 3.0) {
                Map<String, Object> details = new HashMap<>();
                details.put("currentValue", cpuUsage);
                details.put("averageValue", avgCpuUsage);
                details.put("zScore", zScoreCpuUsage);
                
                Anomaly.Severity severity = zScoreCpuUsage > 5.0 ? Anomaly.Severity.HIGH :
                                        zScoreCpuUsage > 4.0 ? Anomaly.Severity.MEDIUM :
                                        Anomaly.Severity.LOW;
                
                anomalies.add(new Anomaly(
                    moduleId,
                    Anomaly.AnomalyType.CPU_USAGE_SPIKE,
                    severity,
                    zScoreCpuUsage,
                    details
                ));
            }
            
            // Check for error rate anomalies
            int errorCount = moduleMetrics.getErrorCount();
            double zScoreErrorRate = calculateZScore(errorCount, avgErrorRate, stdDevErrorRate);
            if (zScoreErrorRate > 3.0 || errorCount > 0) {
                Map<String, Object> details = new HashMap<>();
                details.put("currentValue", errorCount);
                details.put("averageValue", avgErrorRate);
                details.put("zScore", zScoreErrorRate);
                
                Anomaly.Severity severity = zScoreErrorRate > 5.0 ? Anomaly.Severity.HIGH :
                                        zScoreErrorRate > 4.0 ? Anomaly.Severity.MEDIUM :
                                        Anomaly.Severity.LOW;
                
                anomalies.add(new Anomaly(
                    moduleId,
                    Anomaly.AnomalyType.ERROR_RATE_SPIKE,
                    severity,
                    zScoreErrorRate,
                    details
                ));
            }
            
            return anomalies;
        }
        
        /**
         * Calculates the mean of an array of values.
         *
         * @param values The values
         * @return The mean
         */
        private double calculateMean(double[] values) {
            if (values.length == 0) return 0;
            
            double sum = 0;
            for (double value : values) {
                sum += value;
            }
            return sum / values.length;
        }
        
        /**
         * Calculates the standard deviation of an array of values.
         *
         * @param values The values
         * @param mean The mean
         * @return The standard deviation
         */
        private double calculateStdDev(double[] values, double mean) {
            if (values.length <= 1) return 0;
            
            double sumOfSquaredDifferences = 0;
            for (double value : values) {
                double difference = value - mean;
                sumOfSquaredDifferences += difference * difference;
            }
            return Math.sqrt(sumOfSquaredDifferences / (values.length - 1));
        }
        
        /**
         * Calculates the Z-score of a value.
         *
         * @param value The value
         * @param mean The mean
         * @param stdDev The standard deviation
         * @return The Z-score
         */
        private double calculateZScore(double value, double mean, double stdDev) {
            if (stdDev == 0) return 0;
            return Math.abs(value - mean) / stdDev;
        }
    }
} 