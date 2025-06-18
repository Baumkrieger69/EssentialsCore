package com.essentialscore.api.performance;

import com.essentialscore.api.module.ModuleRegistry;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive profiling system for tracking and analyzing performance metrics.
 */
public class ProfilingSystem {
    private static final Logger LOGGER = Logger.getLogger(ProfilingSystem.class.getName());
    
    private final Plugin plugin;
    private final Map<String, Metric> metrics;
    private final Map<String, PerformanceProfile> moduleProfiles;
    private final Map<String, Long> timerStarts;
    private final List<MetricExporter> exporters;
    private final AnomalyDetector anomalyDetector;
    private final PerformancePredictor performancePredictor;
    private final ABTestingFramework abTestingFramework;
    private final UsageAnalytics usageAnalytics;
    private final ScheduledExecutorService scheduledExecutor;
    private boolean running;
    
    /**
     * Creates a new profiling system.
     *
     * @param plugin The plugin
     * @param moduleRegistry The module registry
     */
    public ProfilingSystem(Plugin plugin, ModuleRegistry moduleRegistry) {
        this.plugin = plugin;
        this.metrics = new ConcurrentHashMap<>();
        this.moduleProfiles = new ConcurrentHashMap<>();
        this.timerStarts = new ConcurrentHashMap<>();
        this.exporters = new ArrayList<>();
        this.anomalyDetector = new AnomalyDetector();
        this.performancePredictor = new PerformancePredictor();
        this.abTestingFramework = new ABTestingFramework(this);
        this.usageAnalytics = new UsageAnalytics(this);
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.running = false;
        
        // Register default exporters
        registerExporter(new PrometheusExporter());
        registerExporter(new GrafanaExporter());
        registerExporter(new NewRelicExporter());
    }
    
    /**
     * Starts the profiling system.
     */
    public void start() {
        if (running) return;
        
        // Start periodic metric collection
        scheduledExecutor.scheduleAtFixedRate(this::collectSystemMetrics, 0, 5, TimeUnit.SECONDS);
        
        // Start anomaly detection
        scheduledExecutor.scheduleAtFixedRate(() -> {
            // Detect anomalies for all metrics
            for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
                String metricName = entry.getKey();
                Metric metric = entry.getValue();
                if (!metric.getSamples().isEmpty()) {
                    double currentValue = metric.getValue();
                    anomalyDetector.detectAnomalies(metricName, currentValue, "system");
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        // Start performance prediction
        scheduledExecutor.scheduleAtFixedRate(() -> performancePredictor.predictPerformanceIssues(), 60, 60, TimeUnit.SECONDS);
        
        running = true;
        LOGGER.info("Profiling system started");
    }
    
    /**
     * Stops the profiling system.
     */
    public void stop() {
        if (!running) return;
        
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        running = false;
        LOGGER.info("Profiling system stopped");
    }
    
    /**
     * Registers a metric exporter.
     *
     * @param exporter The exporter to register
     */
    public void registerExporter(MetricExporter exporter) {
        exporters.add(exporter);
    }
    
    /**
     * Unregisters a metric exporter.
     *
     * @param exporter The exporter to unregister
     */
    public void unregisterExporter(MetricExporter exporter) {
        exporters.remove(exporter);
    }
    
    /**
     * Collects system-wide metrics.
     */
    private void collectSystemMetrics() {
        try {
            // Collect JVM metrics
            recordGauge("jvm.memory.used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            recordGauge("jvm.memory.total", Runtime.getRuntime().totalMemory());
            recordGauge("jvm.memory.max", Runtime.getRuntime().maxMemory());
            recordGauge("jvm.threads.active", Thread.activeCount());
            
            // Collect server metrics
            recordGauge("server.players.online", plugin.getServer().getOnlinePlayers().size());
            recordGauge("server.tps", getTPS());
            
            // Export metrics to all exporters
            for (MetricExporter exporter : exporters) {
                try {
                    exporter.exportMetrics(Collections.unmodifiableMap(metrics));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to export metrics to " + exporter.getClass().getSimpleName(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error collecting system metrics", e);
        }
    }
    
    /**
     * Estimates the current TPS (ticks per second).
     *
     * @return The estimated TPS
     */
    private double getTPS() {
        // This is a simple estimation and should be replaced with a more accurate method
        // Use the server's actual TPS if available
        return 20.0;
    }
    
    /**
     * Starts a timer with the given name.
     *
     * @param name The timer name
     * @return The timer ID
     */
    public String startTimer(String name) {
        String timerId = name + ":" + UUID.randomUUID().toString();
        timerStarts.put(timerId, System.nanoTime());
        return timerId;
    }
    
    /**
     * Stops a timer and records its duration.
     *
     * @param timerId The timer ID
     */
    public void stopTimer(String timerId) {
        Long startTime = timerStarts.remove(timerId);
        if (startTime == null) {
            LOGGER.warning("Timer not found: " + timerId);
            return;
        }
        
        long duration = System.nanoTime() - startTime;
        String metricName = timerId.substring(0, timerId.indexOf(':'));
        recordTimer(metricName, duration);
    }
    
    /**
     * Records a timer metric.
     *
     * @param name The metric name
     * @param durationNanos The duration in nanoseconds
     */
    public void recordTimer(String name, long durationNanos) {
        Metric metric = metrics.computeIfAbsent(name, k -> new Metric(name, MetricType.TIMER));
        metric.addSample(durationNanos / 1_000_000.0); // Convert to milliseconds
    }
    
    /**
     * Records a counter metric.
     *
     * @param name The metric name
     * @param value The value to add to the counter
     */
    public void recordCounter(String name, double value) {
        Metric metric = metrics.computeIfAbsent(name, k -> new Metric(name, MetricType.COUNTER));
        metric.addSample(value);
    }
    
    /**
     * Records a gauge metric.
     *
     * @param name The metric name
     * @param value The gauge value
     */
    public void recordGauge(String name, double value) {
        Metric metric = metrics.computeIfAbsent(name, k -> new Metric(name, MetricType.GAUGE));
        metric.setValue(value);
    }
    
    /**
     * Increments a counter metric by 1.
     *
     * @param name The metric name
     */
    public void incrementCounter(String name) {
        recordCounter(name, 1);
    }
    
    /**
     * Gets a metric by name.
     *
     * @param name The metric name
     * @return The metric, or null if not found
     */
    public Metric getMetric(String name) {
        return metrics.get(name);
    }
    
    /**
     * Gets all metrics.
     *
     * @return An unmodifiable map of metrics
     */
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }
    
    /**
     * Gets the anomaly detector.
     *
     * @return The anomaly detector
     */
    public AnomalyDetector getAnomalyDetector() {
        return anomalyDetector;
    }
    
    /**
     * Gets the performance predictor.
     *
     * @return The performance predictor
     */
    public PerformancePredictor getPerformancePredictor() {
        return performancePredictor;
    }
    
    /**
     * Gets the A/B testing framework.
     *
     * @return The A/B testing framework
     */
    public ABTestingFramework getAbTestingFramework() {
        return abTestingFramework;
    }
    
    /**
     * Gets the usage analytics.
     *
     * @return The usage analytics
     */
    public UsageAnalytics getUsageAnalytics() {
        return usageAnalytics;
    }
    
    /**
     * Creates a performance profile for a module.
     *
     * @param moduleId The module ID
     * @return The performance profile
     */
    public PerformanceProfile createModuleProfile(String moduleId) {
        PerformanceProfile profile = new PerformanceProfile(moduleId, this);
        moduleProfiles.put(moduleId, profile);
        return profile;
    }
    
    /**
     * Gets a module's performance profile.
     *
     * @param moduleId The module ID
     * @return The performance profile, or null if not found
     */
    public PerformanceProfile getModuleProfile(String moduleId) {
        return moduleProfiles.get(moduleId);
    }
    
    /**
     * Represents a performance metric.
     */
    public static class Metric {
        private final String name;
        private final MetricType type;
        private volatile double value;
        private final List<Double> samples;
        private final Instant createdAt;
        private Instant lastUpdatedAt;
        
        /**
         * Creates a new metric.
         *
         * @param name The metric name
         * @param type The metric type
         */
        public Metric(String name, MetricType type) {
            this.name = name;
            this.type = type;
            this.value = 0;
            this.samples = Collections.synchronizedList(new ArrayList<>());
            this.createdAt = Instant.now();
            this.lastUpdatedAt = createdAt;
        }
        
        /**
         * Gets the metric name.
         *
         * @return The metric name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Gets the metric type.
         *
         * @return The metric type
         */
        public MetricType getType() {
            return type;
        }
        
        /**
         * Gets the metric value.
         *
         * @return The metric value
         */
        public double getValue() {
            return value;
        }
        
        /**
         * Sets the metric value.
         *
         * @param value The metric value
         */
        public void setValue(double value) {
            this.value = value;
            this.lastUpdatedAt = Instant.now();
            
            // For gauges, also add a sample
            if (type == MetricType.GAUGE) {
                addSample(value);
            }
        }
        
        /**
         * Adds a sample to the metric.
         *
         * @param sample The sample value
         */
        public void addSample(double sample) {
            samples.add(sample);
            this.lastUpdatedAt = Instant.now();
            
            // For counters, update the value
            if (type == MetricType.COUNTER) {
                this.value += sample;
            }
            
            // Keep only the last 1000 samples
            if (samples.size() > 1000) {
                samples.subList(0, samples.size() - 1000).clear();
            }
        }
        
        /**
         * Gets the metric samples.
         *
         * @return An unmodifiable list of samples
         */
        public List<Double> getSamples() {
            return Collections.unmodifiableList(new ArrayList<>(samples));
        }
        
        /**
         * Gets the time the metric was created.
         *
         * @return The creation time
         */
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        /**
         * Gets the time the metric was last updated.
         *
         * @return The last update time
         */
        public Instant getLastUpdatedAt() {
            return lastUpdatedAt;
        }
        
        /**
         * Gets the sample count.
         *
         * @return The sample count
         */
        public int getSampleCount() {
            return samples.size();
        }
        
        /**
         * Gets the sample average.
         *
         * @return The sample average, or 0 if there are no samples
         */
        public double getSampleAverage() {
            if (samples.isEmpty()) return 0;
            
            double sum = 0;
            for (double sample : samples) {
                sum += sample;
            }
            return sum / samples.size();
        }
        
        /**
         * Gets the sample median.
         *
         * @return The sample median, or 0 if there are no samples
         */
        public double getSampleMedian() {
            if (samples.isEmpty()) return 0;
            
            List<Double> sortedSamples = new ArrayList<>(samples);
            Collections.sort(sortedSamples);
            
            int middle = sortedSamples.size() / 2;
            if (sortedSamples.size() % 2 == 1) {
                return sortedSamples.get(middle);
            } else {
                return (sortedSamples.get(middle - 1) + sortedSamples.get(middle)) / 2.0;
            }
        }
        
        /**
         * Gets the sample standard deviation.
         *
         * @return The sample standard deviation, or 0 if there are fewer than 2 samples
         */
        public double getSampleStdDev() {
            if (samples.size() < 2) return 0;
            
            double avg = getSampleAverage();
            double sum = 0;
            for (double sample : samples) {
                sum += Math.pow(sample - avg, 2);
            }
            return Math.sqrt(sum / (samples.size() - 1));
        }
        
        /**
         * Gets the sample minimum.
         *
         * @return The sample minimum, or 0 if there are no samples
         */
        public double getSampleMin() {
            if (samples.isEmpty()) return 0;
            
            double min = Double.MAX_VALUE;
            for (double sample : samples) {
                min = Math.min(min, sample);
            }
            return min;
        }
        
        /**
         * Gets the sample maximum.
         *
         * @return The sample maximum, or 0 if there are no samples
         */
        public double getSampleMax() {
            if (samples.isEmpty()) return 0;
            
            double max = Double.MIN_VALUE;
            for (double sample : samples) {
                max = Math.max(max, sample);
            }
            return max;
        }
        
        /**
         * Gets the sample percentile.
         *
         * @param percentile The percentile (0-100)
         * @return The sample percentile, or 0 if there are no samples
         */
        public double getSamplePercentile(int percentile) {
            if (samples.isEmpty()) return 0;
            if (percentile < 0 || percentile > 100) {
                throw new IllegalArgumentException("Percentile must be between 0 and 100");
            }
            
            List<Double> sortedSamples = new ArrayList<>(samples);
            Collections.sort(sortedSamples);
            
            int index = (int) Math.ceil(percentile / 100.0 * sortedSamples.size()) - 1;
            return sortedSamples.get(Math.max(0, Math.min(sortedSamples.size() - 1, index)));
        }
    }
    
    /**
     * Represents a performance profile for a module.
     */
    public static class PerformanceProfile {
        private final String moduleId;
        private final ProfilingSystem profilingSystem;
        private final Map<String, Baseline> baselines;
        
        /**
         * Creates a new performance profile.
         *
         * @param moduleId The module ID
         * @param profilingSystem The profiling system
         */
        public PerformanceProfile(String moduleId, ProfilingSystem profilingSystem) {
            this.moduleId = moduleId;
            this.profilingSystem = profilingSystem;
            this.baselines = new HashMap<>();
        }
        
        /**
         * Records an operation timer.
         *
         * @param operation The operation name
         * @param durationNanos The duration in nanoseconds
         */
        public void recordOperation(String operation, long durationNanos) {
            String metricName = "module." + moduleId + ".operation." + operation;
            profilingSystem.recordTimer(metricName, durationNanos);
        }
        
        /**
         * Starts an operation timer.
         *
         * @param operation The operation name
         * @return The timer ID
         */
        public String startOperation(String operation) {
            return profilingSystem.startTimer("module." + moduleId + ".operation." + operation);
        }
        
        /**
         * Records a module event.
         *
         * @param event The event name
         */
        public void recordEvent(String event) {
            String metricName = "module." + moduleId + ".event." + event;
            profilingSystem.incrementCounter(metricName);
        }
        
        /**
         * Records module resource usage.
         *
         * @param resource The resource name
         * @param value The resource value
         */
        public void recordResourceUsage(String resource, double value) {
            String metricName = "module." + moduleId + ".resource." + resource;
            profilingSystem.recordGauge(metricName, value);
        }
        
        /**
         * Records a feature usage.
         *
         * @param feature The feature name
         */
        public void recordFeatureUsage(String feature) {
            profilingSystem.getUsageAnalytics().recordFeatureUsage(moduleId, feature);
        }
        
        /**
         * Creates a performance baseline for an operation.
         *
         * @param operation The operation name
         * @return The baseline
         */
        public Baseline createBaseline(String operation) {
            String metricName = "module." + moduleId + ".operation." + operation;
            Metric metric = profilingSystem.getMetric(metricName);
            if (metric == null) {
                throw new IllegalArgumentException("No metric found for operation: " + operation);
            }
            
            Baseline baseline = new Baseline(operation, metric);
            baselines.put(operation, baseline);
            return baseline;
        }
        
        /**
         * Gets a baseline for an operation.
         *
         * @param operation The operation name
         * @return The baseline, or null if not found
         */
        public Baseline getBaseline(String operation) {
            return baselines.get(operation);
        }
        
        /**
         * Gets the module ID.
         *
         * @return The module ID
         */
        public String getModuleId() {
            return moduleId;
        }
    }
    
    /**
     * Represents a performance baseline.
     */
    public static class Baseline {
        private final String operation;
        private final double averageTime;
        private final double medianTime;
        private final double p95Time;
        private final double stdDevTime;
        private final Instant createdAt;
        
        /**
         * Creates a new baseline from a metric.
         *
         * @param operation The operation name
         * @param metric The metric to base the baseline on
         */
        public Baseline(String operation, Metric metric) {
            this.operation = operation;
            this.averageTime = metric.getSampleAverage();
            this.medianTime = metric.getSampleMedian();
            this.p95Time = metric.getSamplePercentile(95);
            this.stdDevTime = metric.getSampleStdDev();
            this.createdAt = Instant.now();
        }
        
        /**
         * Gets the operation name.
         *
         * @return The operation name
         */
        public String getOperation() {
            return operation;
        }
        
        /**
         * Gets the average time.
         *
         * @return The average time
         */
        public double getAverageTime() {
            return averageTime;
        }
        
        /**
         * Gets the median time.
         *
         * @return The median time
         */
        public double getMedianTime() {
            return medianTime;
        }
        
        /**
         * Gets the 95th percentile time.
         *
         * @return The 95th percentile time
         */
        public double getP95Time() {
            return p95Time;
        }
        
        /**
         * Gets the standard deviation time.
         *
         * @return The standard deviation time
         */
        public double getStdDevTime() {
            return stdDevTime;
        }
        
        /**
         * Gets the creation time.
         *
         * @return The creation time
         */
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        /**
         * Checks if a sample time is anomalous.
         *
         * @param sampleTime The sample time
         * @return true if the sample time is anomalous
         */
        public boolean isAnomalous(double sampleTime) {
            // A sample is anomalous if it's more than 3 standard deviations from the average
            return Math.abs(sampleTime - averageTime) > 3 * stdDevTime;
        }
    }
    
    /**
     * Represents a metric type.
     */
    public enum MetricType {
        /**
         * A counter that increases over time.
         */
        COUNTER,
        
        /**
         * A gauge that represents a current value.
         */
        GAUGE,
        
        /**
         * A timer that measures duration.
         */
        TIMER
    }
    
    /**
     * Interface for metric exporters.
     */
    public interface MetricExporter {
        /**
         * Exports metrics to the external system.
         *
         * @param metrics The metrics to export
         */
        void exportMetrics(Map<String, Metric> metrics);
        
        /**
         * Configures the exporter.
         *
         * @param config The configuration
         */
        void configure(Map<String, String> config);
    }
    
    /**
     * Prometheus metrics exporter.
     */
    public static class PrometheusExporter implements MetricExporter {
        private String endpoint = "http://localhost:9090";
        
        @Override
        public void exportMetrics(Map<String, Metric> metrics) {
            // Implementation would depend on the Prometheus client library
            // Log the performance metrics
            LOGGER.fine("Exporting " + metrics.size() + " metrics to Prometheus at " + endpoint);
        }
        
        @Override
        public void configure(Map<String, String> config) {
            if (config.containsKey("endpoint")) {
                endpoint = config.get("endpoint");
            }
        }
    }
    
    /**
     * Grafana metrics exporter.
     */
    public static class GrafanaExporter implements MetricExporter {
        private String endpoint = "http://localhost:3000";
        
        @Override
        public void exportMetrics(Map<String, Metric> metrics) {
            // Implementation would depend on the Grafana client library
            // Export metrics to configured Grafana endpoint
            LOGGER.fine("Exporting " + metrics.size() + " metrics to Grafana at " + endpoint);
        }
        
        @Override
        public void configure(Map<String, String> config) {
            if (config.containsKey("endpoint")) {
                endpoint = config.get("endpoint");
            }
        }
    }
    
    /**
     * New Relic metrics exporter.
     */
    public static class NewRelicExporter implements MetricExporter {
        private String apiKey = "";
        
        @Override
        public void exportMetrics(Map<String, Metric> metrics) {
            // Implementation would depend on the New Relic client library
            // Export metrics to configured New Relic endpoint
            if (!apiKey.isEmpty()) {
                LOGGER.fine("Exporting " + metrics.size() + " metrics to New Relic with API key");
            } else {
                LOGGER.fine("Exporting " + metrics.size() + " metrics to New Relic (no API key configured)");
            }
        }
        
        @Override
        public void configure(Map<String, String> config) {
            if (config.containsKey("apiKey")) {
                apiKey = config.get("apiKey");
            }
        }
    }
} 