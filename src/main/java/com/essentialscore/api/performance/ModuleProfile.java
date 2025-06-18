package com.essentialscore.api.performance;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Maintains a profile of a module's performance over time.
 */
public class ModuleProfile {
    private final String moduleId;
    private final List<DataPoint> dataPoints;
    private final Map<String, List<Double>> methodResponseTimes;
    private final Map<String, Map<LoadLevel, List<Double>>> loadLevelPerformance;
    
    // Calculated performance characteristics
    private double averageResponseTime;
    private double averageCpuUsage;
    private double averageMemoryUsage;
    private double peakResponseTime;
    private double peakCpuUsage;
    private double peakMemoryUsage;
    private LoadScalingCharacteristics loadScaling;
    
    /**
     * Creates a new module profile.
     *
     * @param moduleId The module ID
     */
    public ModuleProfile(String moduleId) {
        this.moduleId = moduleId;
        this.dataPoints = new ArrayList<>();
        this.methodResponseTimes = new ConcurrentHashMap<>();
        this.loadLevelPerformance = new ConcurrentHashMap<>();
    }
    
    /**
     * Adds a data point to the profile.
     *
     * @param moduleMetrics The module metrics
     * @param serverMetrics The server metrics
     */
    public synchronized void addDataPoint(ModuleMetrics moduleMetrics, ServerMetrics serverMetrics) {
        DataPoint dataPoint = new DataPoint(
            moduleMetrics.getAverageExecutionTime(moduleId),
            moduleMetrics.getCpuUsage(),
            moduleMetrics.getMemoryUsage(),
            moduleMetrics.getErrorCount(),
            moduleMetrics.getWarningCount(),
            serverMetrics.getCpuLoad(),
            serverMetrics.getMemoryUsagePercentage(),
            serverMetrics.getPlayerCount(),
            serverMetrics.getTps(),
            System.currentTimeMillis()
        );
        
        dataPoints.add(dataPoint);
        
        // Update performance characteristics
        updatePerformanceCharacteristics();
        
        // Categorize by load level
        categorizeByLoadLevel(dataPoint);
    }
    
    /**
     * Updates the calculated performance characteristics.
     */
    private void updatePerformanceCharacteristics() {
        if (dataPoints.isEmpty()) return;
        
        // Calculate averages
        averageResponseTime = dataPoints.stream()
            .mapToDouble(DataPoint::getResponseTime)
            .average()
            .orElse(0.0);
        
        averageCpuUsage = dataPoints.stream()
            .mapToDouble(DataPoint::getModuleCpuUsage)
            .average()
            .orElse(0.0);
        
        averageMemoryUsage = dataPoints.stream()
            .mapToDouble(DataPoint::getModuleMemoryUsage)
            .average()
            .orElse(0.0);
        
        // Calculate peaks
        peakResponseTime = dataPoints.stream()
            .mapToDouble(DataPoint::getResponseTime)
            .max()
            .orElse(0.0);
        
        peakCpuUsage = dataPoints.stream()
            .mapToDouble(DataPoint::getModuleCpuUsage)
            .max()
            .orElse(0.0);
        
        peakMemoryUsage = dataPoints.stream()
            .mapToDouble(DataPoint::getModuleMemoryUsage)
            .max()
            .orElse(0.0);
        
        // Update load scaling characteristics
        updateLoadScalingCharacteristics();
    }
    
    /**
     * Updates the load scaling characteristics.
     */
    private void updateLoadScalingCharacteristics() {
        if (dataPoints.size() < 10) return; // Need enough data points
        
        // Analyze how response time scales with server load
        Map<LoadLevel, List<Double>> responseTimesByLoad = new HashMap<>();
        
        for (DataPoint point : dataPoints) {
            LoadLevel loadLevel = categorizeLoadLevel(point.getServerCpuLoad());
            responseTimesByLoad.computeIfAbsent(loadLevel, k -> new ArrayList<>())
                .add(point.getResponseTime());
        }
        
        // Calculate average response time for each load level
        Map<LoadLevel, Double> avgResponseByLoad = new HashMap<>();
        for (Map.Entry<LoadLevel, List<Double>> entry : responseTimesByLoad.entrySet()) {
            double avgResponse = entry.getValue().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            avgResponseByLoad.put(entry.getKey(), avgResponse);
        }
        
        // Calculate scaling factor from LOW to HIGH
        double lowLoad = avgResponseByLoad.getOrDefault(LoadLevel.LOW, 0.0);
        double mediumLoad = avgResponseByLoad.getOrDefault(LoadLevel.MEDIUM, 0.0);
        double highLoad = avgResponseByLoad.getOrDefault(LoadLevel.HIGH, 0.0);
        
        double lowToMediumScaling = (lowLoad > 0 && mediumLoad > 0) ? mediumLoad / lowLoad : 1.0;
        double mediumToHighScaling = (mediumLoad > 0 && highLoad > 0) ? highLoad / mediumLoad : 1.0;
        
        this.loadScaling = new LoadScalingCharacteristics(
            lowToMediumScaling,
            mediumToHighScaling,
            avgResponseByLoad
        );
    }
    
    /**
     * Categorizes a data point by load level.
     *
     * @param dataPoint The data point
     */
    private void categorizeByLoadLevel(DataPoint dataPoint) {
        LoadLevel loadLevel = categorizeLoadLevel(dataPoint.getServerCpuLoad());
        
        loadLevelPerformance.computeIfAbsent(
            "responseTime",
            k -> new HashMap<>()
        ).computeIfAbsent(
            loadLevel,
            k -> new ArrayList<>()
        ).add(dataPoint.getResponseTime());
        
        loadLevelPerformance.computeIfAbsent(
            "cpuUsage",
            k -> new HashMap<>()
        ).computeIfAbsent(
            loadLevel,
            k -> new ArrayList<>()
        ).add(dataPoint.getModuleCpuUsage());
        
        loadLevelPerformance.computeIfAbsent(
            "memoryUsage",
            k -> new HashMap<>()
        ).computeIfAbsent(
            loadLevel,
            k -> new ArrayList<>()
        ).add(dataPoint.getModuleMemoryUsage());
    }
    
    /**
     * Categorizes a CPU load into a load level.
     *
     * @param cpuLoad The CPU load
     * @return The load level
     */
    private LoadLevel categorizeLoadLevel(double cpuLoad) {
        if (cpuLoad < 0.3) {
            return LoadLevel.LOW;
        } else if (cpuLoad < 0.7) {
            return LoadLevel.MEDIUM;
        } else {
            return LoadLevel.HIGH;
        }
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
     * Gets the data points.
     *
     * @return The data points
     */
    public List<DataPoint> getDataPoints() {
        return new ArrayList<>(dataPoints);
    }
    
    /**
     * Gets the data points for a time range.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @return The data points
     */
    public List<DataPoint> getDataPoints(long startTime, long endTime) {
        return dataPoints.stream()
            .filter(dp -> dp.getTimestamp() >= startTime && dp.getTimestamp() <= endTime)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the average response time.
     *
     * @return The average response time
     */
    public double getAverageResponseTime() {
        return averageResponseTime;
    }
    
    /**
     * Gets the average CPU usage.
     *
     * @return The average CPU usage
     */
    public double getAverageCpuUsage() {
        return averageCpuUsage;
    }
    
    /**
     * Gets the average memory usage.
     *
     * @return The average memory usage
     */
    public double getAverageMemoryUsage() {
        return averageMemoryUsage;
    }
    
    /**
     * Gets the peak response time.
     *
     * @return The peak response time
     */
    public double getPeakResponseTime() {
        return peakResponseTime;
    }
    
    /**
     * Gets the peak CPU usage.
     *
     * @return The peak CPU usage
     */
    public double getPeakCpuUsage() {
        return peakCpuUsage;
    }
    
    /**
     * Gets the peak memory usage.
     *
     * @return The peak memory usage
     */
    public double getPeakMemoryUsage() {
        return peakMemoryUsage;
    }
    
    /**
     * Gets the load scaling characteristics.
     *
     * @return The load scaling characteristics
     */
    public LoadScalingCharacteristics getLoadScaling() {
        return loadScaling;
    }
    
    /**
     * Gets the number of data points.
     *
     * @return The number of data points
     */
    public int getDataPointCount() {
        return dataPoints.size();
    }
    
    /**
     * Gets the load level performance map.
     *
     * @return The load level performance map
     */
    public Map<String, Map<LoadLevel, List<Double>>> getLoadLevelPerformance() {
        return loadLevelPerformance;
    }
    
    /**
     * Records a response time for a specific method.
     *
     * @param methodName The name of the method
     * @param responseTime The response time in milliseconds
     */
    public void recordMethodResponseTime(String methodName, double responseTime) {
        methodResponseTimes.computeIfAbsent(methodName, k -> new ArrayList<>())
            .add(responseTime);
    }
    
    /**
     * Gets the response times for a specific method.
     *
     * @param methodName The name of the method
     * @return List of response times, or empty list if method not found
     */
    public List<Double> getMethodResponseTimes(String methodName) {
        return new ArrayList<>(methodResponseTimes.getOrDefault(methodName, new ArrayList<>()));
    }
    
    /**
     * Enum for load levels.
     */
    public enum LoadLevel {
        LOW,
        MEDIUM,
        HIGH
    }
    
    /**
     * Class representing a data point in the module profile.
     */
    public static class DataPoint {
        private final double responseTime;
        private final double moduleCpuUsage;
        private final double moduleMemoryUsage;
        private final int errorCount;
        private final int warningCount;
        private final double serverCpuLoad;
        private final double serverMemoryUsage;
        private final int playerCount;
        private final double tps;
        private final long timestamp;
        
        /**
         * Creates a new data point.
         *
         * @param responseTime The response time
         * @param moduleCpuUsage The module CPU usage
         * @param moduleMemoryUsage The module memory usage
         * @param errorCount The error count
         * @param warningCount The warning count
         * @param serverCpuLoad The server CPU load
         * @param serverMemoryUsage The server memory usage
         * @param playerCount The player count
         * @param tps The TPS
         * @param timestamp The timestamp
         */
        public DataPoint(double responseTime, double moduleCpuUsage, double moduleMemoryUsage,
                        int errorCount, int warningCount, double serverCpuLoad,
                        double serverMemoryUsage, int playerCount, double tps, long timestamp) {
            this.responseTime = responseTime;
            this.moduleCpuUsage = moduleCpuUsage;
            this.moduleMemoryUsage = moduleMemoryUsage;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
            this.serverCpuLoad = serverCpuLoad;
            this.serverMemoryUsage = serverMemoryUsage;
            this.playerCount = playerCount;
            this.tps = tps;
            this.timestamp = timestamp;
        }
        
        /**
         * Gets the response time.
         *
         * @return The response time
         */
        public double getResponseTime() {
            return responseTime;
        }
        
        /**
         * Gets the module CPU usage.
         *
         * @return The module CPU usage
         */
        public double getModuleCpuUsage() {
            return moduleCpuUsage;
        }
        
        /**
         * Gets the module memory usage.
         *
         * @return The module memory usage
         */
        public double getModuleMemoryUsage() {
            return moduleMemoryUsage;
        }
        
        /**
         * Gets the error count.
         *
         * @return The error count
         */
        public int getErrorCount() {
            return errorCount;
        }
        
        /**
         * Gets the warning count.
         *
         * @return The warning count
         */
        public int getWarningCount() {
            return warningCount;
        }
        
        /**
         * Gets the server CPU load.
         *
         * @return The server CPU load
         */
        public double getServerCpuLoad() {
            return serverCpuLoad;
        }
        
        /**
         * Gets the server memory usage.
         *
         * @return The server memory usage
         */
        public double getServerMemoryUsage() {
            return serverMemoryUsage;
        }
        
        /**
         * Gets the player count.
         *
         * @return The player count
         */
        public int getPlayerCount() {
            return playerCount;
        }
        
        /**
         * Gets the TPS.
         *
         * @return The TPS
         */
        public double getTps() {
            return tps;
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
         * Gets the time as a LocalDateTime.
         *
         * @return The time
         */
        public LocalDateTime getTime() {
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            );
        }
    }
    
    /**
     * Class representing load scaling characteristics.
     */
    public static class LoadScalingCharacteristics {
        private final double lowToMediumScaling;
        private final double mediumToHighScaling;
        private final Map<LoadLevel, Double> avgResponseByLoad;
        
        /**
         * Creates new load scaling characteristics.
         *
         * @param lowToMediumScaling The low to medium scaling factor
         * @param mediumToHighScaling The medium to high scaling factor
         * @param avgResponseByLoad The average response by load
         */
        public LoadScalingCharacteristics(double lowToMediumScaling, double mediumToHighScaling,
                                          Map<LoadLevel, Double> avgResponseByLoad) {
            this.lowToMediumScaling = lowToMediumScaling;
            this.mediumToHighScaling = mediumToHighScaling;
            this.avgResponseByLoad = avgResponseByLoad;
        }
        
        /**
         * Gets the low to medium scaling factor.
         *
         * @return The low to medium scaling factor
         */
        public double getLowToMediumScaling() {
            return lowToMediumScaling;
        }
        
        /**
         * Gets the medium to high scaling factor.
         *
         * @return The medium to high scaling factor
         */
        public double getMediumToHighScaling() {
            return mediumToHighScaling;
        }
        
        /**
         * Gets the average response by load.
         *
         * @return The average response by load
         */
        public Map<LoadLevel, Double> getAvgResponseByLoad() {
            return avgResponseByLoad;
        }
        
        /**
         * Checks if the module scales poorly with load.
         *
         * @return true if the module scales poorly
         */
        public boolean scalesPoorly() {
            return lowToMediumScaling > 2.0 || mediumToHighScaling > 2.0;
        }
        
        /**
         * Gets the predicted response time for a load level.
         *
         * @param loadLevel The load level
         * @return The predicted response time
         */
        public double getPredictedResponseTime(LoadLevel loadLevel) {
            return avgResponseByLoad.getOrDefault(loadLevel, 0.0);
        }
    }
} 
