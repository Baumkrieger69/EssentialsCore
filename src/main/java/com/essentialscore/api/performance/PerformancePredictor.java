package com.essentialscore.api.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Predicts potential performance issues based on metrics and trends.
 */
public class PerformancePredictor {
    private static final Logger LOGGER = Logger.getLogger(PerformancePredictor.class.getName());
    
    private final ProfilingSystem profilingSystem;
    private final Map<String, TrendAnalysis> trendAnalyses;
    private final List<PredictedIssue> predictedIssues;
    private final List<Consumer<PredictedIssue>> issueListeners;
    
    /**
     * Creates a new performance predictor.
     *
     * @param profilingSystem The profiling system
     */
    public PerformancePredictor(ProfilingSystem profilingSystem) {
        this.profilingSystem = profilingSystem;
        this.trendAnalyses = new ConcurrentHashMap<>();
        this.predictedIssues = new ArrayList<>();
        this.issueListeners = new ArrayList<>();
    }
    
    /**
     * Creates a new performance predictor without a profiling system.
     */
    public PerformancePredictor() {
        this.profilingSystem = null;
        this.trendAnalyses = new ConcurrentHashMap<>();
        this.predictedIssues = new ArrayList<>();
        this.issueListeners = new ArrayList<>();
    }
    
    /**
     * Adds an issue listener.
     *
     * @param listener The listener to add
     */
    public void addIssueListener(Consumer<PredictedIssue> listener) {
        issueListeners.add(listener);
    }
    
    /**
     * Removes an issue listener.
     *
     * @param listener The listener to remove
     */
    public void removeIssueListener(Consumer<PredictedIssue> listener) {
        issueListeners.remove(listener);
    }
    
    /**
     * Predicts performance issues based on trends.
     */
    public void predictPerformanceIssues() {
        try {
            Map<String, ProfilingSystem.Metric> metrics = profilingSystem.getMetrics();
            
            // Update trend analyses
            updateTrendAnalyses(metrics);
            
            // Predict issues
            List<PredictedIssue> newIssues = new ArrayList<>();
            
            // Check for resource exhaustion
            predictResourceExhaustion(newIssues);
            
            // Check for response time degradation
            predictResponseTimeDegradation(newIssues);
            
            // Check for TPS degradation
            predictTpsDegradation(newIssues);
            
            // Add new issues
            for (PredictedIssue issue : newIssues) {
                if (!containsSimilarIssue(issue)) {
                    predictedIssues.add(issue);
                    
                    // Notify listeners
                    for (Consumer<PredictedIssue> listener : issueListeners) {
                        try {
                            listener.accept(issue);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error in issue listener", e);
                        }
                    }
                    
                    // Log the issue
                    LOGGER.warning("Predicted issue: " + issue);
                }
            }
            
            // Clean up old issues (older than 1 day)
            Instant cutoff = Instant.now().minus(Duration.ofDays(1));
            predictedIssues.removeIf(issue -> issue.getPredictedAt().isBefore(cutoff));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error predicting performance issues", e);
        }
    }
    
    /**
     * Updates trend analyses for metrics.
     *
     * @param metrics The current metrics
     */
    private void updateTrendAnalyses(Map<String, ProfilingSystem.Metric> metrics) {
        for (Map.Entry<String, ProfilingSystem.Metric> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            ProfilingSystem.Metric metric = entry.getValue();
            
            // Skip metrics with fewer than 10 samples
            if (metric.getSampleCount() < 10) continue;
            
            // Get or create trend analysis
            TrendAnalysis trendAnalysis = trendAnalyses.computeIfAbsent(
                metricName,
                k -> new TrendAnalysis(metricName)
            );
            
            // Update trend analysis
            trendAnalysis.addSample(metric.getValue());
        }
    }
    
    /**
     * Predicts resource exhaustion issues.
     *
     * @param issues The list to add issues to
     */
    private void predictResourceExhaustion(List<PredictedIssue> issues) {
        predictResourceExhaustion("jvm.memory.used", "jvm.memory.max", 0.8, "Memory", issues);
        predictResourceExhaustion("cpu.usage", null, 90.0, "CPU", issues);
        predictResourceExhaustion("disk.usage", null, 90.0, "Disk", issues);
    }
    
    /**
     * Predicts resource exhaustion for a specific resource.
     *
     * @param usageMetric The usage metric name
     * @param maxMetric The maximum metric name, or null if percentage
     * @param threshold The threshold
     * @param resourceName The resource name
     * @param issues The list to add issues to
     */
    private void predictResourceExhaustion(String usageMetric, String maxMetric, double threshold, String resourceName, List<PredictedIssue> issues) {
        TrendAnalysis usageTrend = trendAnalyses.get(usageMetric);
        if (usageTrend == null) return;
        
        // Check if current usage is high
        ProfilingSystem.Metric usageMetricObj = profilingSystem.getMetric(usageMetric);
        if (usageMetricObj == null) return;
        
        double currentUsage = usageMetricObj.getValue();
        double maxUsage = threshold;
        
        // If max metric is provided, calculate percentage
        if (maxMetric != null) {
            ProfilingSystem.Metric maxMetricObj = profilingSystem.getMetric(maxMetric);
            if (maxMetricObj == null) return;
            
            double maxValue = maxMetricObj.getValue();
            if (maxValue > 0) {
                currentUsage = (currentUsage / maxValue) * 100.0;
                maxUsage = threshold * 100.0;
            }
        }
        
        // Check if current usage is approaching threshold
        if (currentUsage > maxUsage * 0.7) {
            // Check trend
            double slope = usageTrend.getSlope();
            if (slope > 0) {
                // Estimate time to threshold
                double timeToThreshold = (threshold - currentUsage) / slope;
                if (timeToThreshold > 0 && timeToThreshold < 24 * 60) { // Less than 24 hours
                    Duration predictedTime = Duration.ofMinutes((long) timeToThreshold);
                    
                    issues.add(new PredictedIssue(
                        IssueSeverity.HIGH,
                        resourceName + " exhaustion predicted",
                        resourceName + " usage is currently at " + String.format("%.2f", currentUsage) + "% and is " +
                        "increasing at a rate that will exceed the threshold of " + String.format("%.2f", threshold * 100.0) + "% " +
                        "in approximately " + formatDuration(predictedTime),
                        Instant.now(),
                        Instant.now().plus(predictedTime)
                    ));
                }
            }
        }
    }
    
    /**
     * Predicts response time degradation issues.
     *
     * @param issues The list to add issues to
     */
    private void predictResponseTimeDegradation(List<PredictedIssue> issues) {
        // Analyze response time trends for various operations
        for (Map.Entry<String, TrendAnalysis> entry : trendAnalyses.entrySet()) {
            String metricName = entry.getKey();
            TrendAnalysis trend = entry.getValue();
            
            // Check if this is a response time metric
            if (metricName.contains("response.time") || metricName.contains("operation")) {
                double slope = trend.getSlope();
                double currentValue = trend.getLatestValue();
                
                // If response time is increasing significantly
                if (slope > 0.1 && currentValue > 100) { // More than 100ms and increasing
                    issues.add(new PredictedIssue(
                        IssueSeverity.MEDIUM,
                        "Response time degradation predicted for " + metricName,
                        "Response time for " + metricName + " is currently " + String.format("%.2f", currentValue) + "ms " +
                        "and is increasing at a rate of " + String.format("%.2f", slope) + "ms per minute",
                        Instant.now(),
                        Instant.now().plus(Duration.ofHours(1)) // Arbitrary prediction time
                    ));
                }
            }
        }
    }
    
    /**
     * Predicts TPS degradation issues.
     *
     * @param issues The list to add issues to
     */
    private void predictTpsDegradation(List<PredictedIssue> issues) {
        TrendAnalysis tpsTrend = trendAnalyses.get("server.tps");
        if (tpsTrend == null) return;
        
        double slope = tpsTrend.getSlope();
        double currentTps = tpsTrend.getLatestValue();
        
        // If TPS is decreasing and below 18
        if (slope < -0.01 && currentTps < 18) {
            // Estimate time to critical TPS (e.g., 10)
            double timeToThreshold = (10 - currentTps) / slope;
            if (timeToThreshold > 0 && timeToThreshold < 60) { // Less than 60 minutes
                Duration predictedTime = Duration.ofMinutes((long) timeToThreshold);
                
                issues.add(new PredictedIssue(
                    IssueSeverity.CRITICAL,
                    "Server TPS degradation predicted",
                    "Server TPS is currently " + String.format("%.2f", currentTps) + " and is " +
                    "decreasing at a rate that will reach critical levels (10 TPS) " +
                    "in approximately " + formatDuration(predictedTime),
                    Instant.now(),
                    Instant.now().plus(predictedTime)
                ));
            }
        }
    }
    
    /**
     * Checks if the list of predicted issues contains a similar issue.
     *
     * @param issue The issue to check
     * @return true if a similar issue exists
     */
    private boolean containsSimilarIssue(PredictedIssue issue) {
        for (PredictedIssue existingIssue : predictedIssues) {
            if (existingIssue.getTitle().equals(issue.getTitle()) &&
                Duration.between(existingIssue.getPredictedAt(), Instant.now()).toHours() < 6) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Formats a duration for display.
     *
     * @param duration The duration
     * @return The formatted duration
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        
        if (hours > 0) {
            return hours + " hours " + minutes + " minutes";
        } else {
            return minutes + " minutes";
        }
    }
    
    /**
     * Gets predicted issues.
     *
     * @return The predicted issues
     */
    public List<PredictedIssue> getPredictedIssues() {
        return new ArrayList<>(predictedIssues);
    }
    
    /**
     * Gets recent predicted issues.
     *
     * @param duration The duration to look back
     * @return The recent predicted issues
     */
    public List<PredictedIssue> getRecentPredictedIssues(Duration duration) {
        Instant cutoff = Instant.now().minus(duration);
        List<PredictedIssue> recentIssues = new ArrayList<>();
        
        for (PredictedIssue issue : predictedIssues) {
            if (issue.getPredictedAt().isAfter(cutoff)) {
                recentIssues.add(issue);
            }
        }
        
        return recentIssues;
    }
    
    /**
     * Represents a trend analysis for a metric.
     */
    private static class TrendAnalysis {
        private final String metricName;
        private final List<DataPoint> dataPoints;
        private final int maxDataPoints = 60; // Keep one hour of minute data
        
        /**
         * Creates a new trend analysis.
         *
         * @param metricName The metric name
         */
        public TrendAnalysis(String metricName) {
            this.metricName = metricName;
            this.dataPoints = Collections.synchronizedList(new ArrayList<>());
        }
        
        /**
         * Gets the metric name.
         *
         * @return The metric name
         */
        @SuppressWarnings("unused")
        public String getMetricName() {
            return metricName;
        }
        
        /**
         * Adds a sample to the trend analysis.
         *
         * @param value The sample value
         */
        public void addSample(double value) {
            dataPoints.add(new DataPoint(Instant.now(), value));
            
            // Keep only the most recent data points
            if (dataPoints.size() > maxDataPoints) {
                dataPoints.remove(0);
            }
        }
        
        /**
         * Gets the slope of the trend (change per minute).
         *
         * @return The slope
         */
        public double getSlope() {
            if (dataPoints.size() < 2) return 0;
            
            // Simple linear regression
            double sumX = 0;
            double sumY = 0;
            double sumXY = 0;
            double sumXX = 0;
            int n = dataPoints.size();
            
            // Reference time (first data point)
            Instant referenceTime = dataPoints.get(0).time;
            
            for (DataPoint point : dataPoints) {
                double x = Duration.between(referenceTime, point.time).toMinutes();
                double y = point.value;
                
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumXX += x * x;
            }
            
            // Calculate slope
            double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
            if (Double.isNaN(slope) || Double.isInfinite(slope)) {
                return 0;
            }
            
            return slope;
        }
        
        /**
         * Gets the latest value.
         *
         * @return The latest value
         */
        public double getLatestValue() {
            if (dataPoints.isEmpty()) return 0;
            return dataPoints.get(dataPoints.size() - 1).value;
        }
        
        /**
         * Predicts the value at a future time.
         *
         * @param time The future time
         * @return The predicted value
         */
        @SuppressWarnings("unused")
        public double predictValue(Instant time) {
            if (dataPoints.isEmpty()) return 0;
            
            double slope = getSlope();
            double latestValue = getLatestValue();
            Instant latestTime = dataPoints.get(dataPoints.size() - 1).time;
            
            double minutesDiff = Duration.between(latestTime, time).toMinutes();
            return latestValue + slope * minutesDiff;
        }
        
        /**
         * Represents a data point in the trend analysis.
         */
        private static class DataPoint {
            final Instant time;
            final double value;
            
            DataPoint(Instant time, double value) {
                this.time = time;
                this.value = value;
            }
        }
    }
    
    /**
     * Represents a predicted performance issue.
     */
    public static class PredictedIssue {
        private final IssueSeverity severity;
        private final String title;
        private final String description;
        private final Instant predictedAt;
        private final Instant estimatedOccurrence;
        private final String metricName;
        
        /**
         * Creates a new predicted issue.
         *
         * @param severity The issue severity
         * @param title The issue title
         * @param description The issue description
         * @param predictedAt The prediction time
         * @param estimatedOccurrence The estimated occurrence time
         */
        public PredictedIssue(IssueSeverity severity, String title, String description, Instant predictedAt, Instant estimatedOccurrence) {
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.predictedAt = predictedAt;
            this.estimatedOccurrence = estimatedOccurrence;
            this.metricName = extractMetricName(title);
        }
        
        /**
         * Creates a new predicted issue with a specific metric name.
         *
         * @param severity The issue severity
         * @param title The issue title
         * @param description The issue description
         * @param predictedAt The prediction time
         * @param estimatedOccurrence The estimated occurrence time
         * @param metricName The metric name
         */
        public PredictedIssue(IssueSeverity severity, String title, String description, Instant predictedAt, Instant estimatedOccurrence, String metricName) {
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.predictedAt = predictedAt;
            this.estimatedOccurrence = estimatedOccurrence;
            this.metricName = metricName;
        }
        
        /**
         * Extracts a metric name from the issue title.
         * 
         * @param title The issue title
         * @return The extracted metric name or a default value
         */
        private String extractMetricName(String title) {
            // Try to extract the metric name from the title if possible
            if (title.contains("TPS")) {
                return "server.tps";
            } else if (title.contains("Memory")) {
                return "jvm.memory.used";
            } else if (title.contains("CPU")) {
                return "cpu.usage";
            } else if (title.contains("Disk")) {
                return "disk.usage";
            } else if (title.contains("Response time")) {
                // Extract the metric name from titles like "Response time degradation predicted for X"
                int forIndex = title.indexOf(" for ");
                if (forIndex > 0 && forIndex < title.length() - 5) {
                    return title.substring(forIndex + 5);
                }
                return "response.time";
            }
            return "unknown";
        }
        
        /**
         * Gets the issue severity.
         *
         * @return The issue severity
         */
        public IssueSeverity getSeverity() {
            return severity;
        }
        
        /**
         * Gets the issue title.
         *
         * @return The issue title
         */
        public String getTitle() {
            return title;
        }
        
        /**
         * Gets the issue description.
         *
         * @return The issue description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the prediction time.
         *
         * @return The prediction time
         */
        public Instant getPredictedAt() {
            return predictedAt;
        }
        
        /**
         * Gets the estimated occurrence time.
         *
         * @return The estimated occurrence time
         */
        public Instant getEstimatedOccurrence() {
            return estimatedOccurrence;
        }
        
        /**
         * Gets the metric name associated with this issue.
         *
         * @return The metric name
         */
        public String getMetricName() {
            return metricName;
        }
        
        /**
         * Gets the time until the estimated occurrence.
         *
         * @return The time until occurrence
         */
        public Duration getTimeUntilOccurrence() {
            return Duration.between(Instant.now(), estimatedOccurrence);
        }
        
        @Override
        public String toString() {
            return "PredictedIssue{" +
                   "severity=" + severity +
                   ", title='" + title + '\'' +
                   ", description='" + description + '\'' +
                   ", predictedAt=" + predictedAt +
                   ", estimatedOccurrence=" + estimatedOccurrence +
                   '}';
        }
    }
    
    /**
     * Represents the severity of a predicted issue.
     */
    public enum IssueSeverity {
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
        CRITICAL
    }
}
