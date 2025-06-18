package com.essentialscore.api.performance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores performance metrics for a specific module.
 */
public class ModuleMetrics {
    private final String moduleId;
    private final Map<String, AtomicLong> callCounts;
    private final Map<String, AtomicLong> totalExecutionTimes;
    private final Map<String, AtomicLong> maxExecutionTimes;
    
    private long lastUpdateTime;
    private double averageResponseTime;
    private double cpuUsage;
    private long memoryUsage;
    private int errorCount;
    private int warningCount;
    
    /**
     * Creates a new module metrics object.
     *
     * @param moduleId The module ID
     */
    public ModuleMetrics(String moduleId) {
        this.moduleId = moduleId;
        this.callCounts = new ConcurrentHashMap<>();
        this.totalExecutionTimes = new ConcurrentHashMap<>();
        this.maxExecutionTimes = new ConcurrentHashMap<>();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Records a method execution.
     *
     * @param methodName The method name
     * @param executionTimeNanos The execution time in nanoseconds
     */
    public void recordMethodExecution(String methodName, long executionTimeNanos) {
        // Update call count
        callCounts.computeIfAbsent(
            methodName,
            k -> new AtomicLong(0)
        ).incrementAndGet();
        
        // Update total execution time
        totalExecutionTimes.computeIfAbsent(
            methodName,
            k -> new AtomicLong(0)
        ).addAndGet(executionTimeNanos);
        
        // Update max execution time
        maxExecutionTimes.compute(
            methodName,
            (k, v) -> {
                if (v == null) {
                    return new AtomicLong(executionTimeNanos);
                } else {
                    long current = v.get();
                    if (executionTimeNanos > current) {
                        v.set(executionTimeNanos);
                    }
                    return v;
                }
            }
        );
        
        // Update average response time across all methods
        updateAverageResponseTime();
    }
    
    /**
     * Records an error.
     */
    public void recordError() {
        errorCount++;
    }
    
    /**
     * Records a warning.
     */
    public void recordWarning() {
        warningCount++;
    }
    
    /**
     * Updates the average response time.
     */
    private void updateAverageResponseTime() {
        long totalCalls = 0;
        long totalTime = 0;
        
        for (Map.Entry<String, AtomicLong> entry : callCounts.entrySet()) {
            String methodName = entry.getKey();
            long calls = entry.getValue().get();
            
            if (calls > 0) {
                totalCalls += calls;
                totalTime += totalExecutionTimes.getOrDefault(methodName, new AtomicLong(0)).get();
            }
        }
        
        if (totalCalls > 0) {
            // Convert from nanoseconds to milliseconds
            averageResponseTime = (totalTime / totalCalls) / 1_000_000.0;
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
     * Gets the call count for a method.
     *
     * @param methodName The method name
     * @return The call count
     */
    public long getCallCount(String methodName) {
        return callCounts.getOrDefault(methodName, new AtomicLong(0)).get();
    }
    
    /**
     * Gets the total execution time for a method in nanoseconds.
     *
     * @param methodName The method name
     * @return The total execution time
     */
    public long getTotalExecutionTime(String methodName) {
        return totalExecutionTimes.getOrDefault(methodName, new AtomicLong(0)).get();
    }
    
    /**
     * Gets the maximum execution time for a method in nanoseconds.
     *
     * @param methodName The method name
     * @return The maximum execution time
     */
    public long getMaxExecutionTime(String methodName) {
        return maxExecutionTimes.getOrDefault(methodName, new AtomicLong(0)).get();
    }
    
    /**
     * Gets the average execution time for a method in milliseconds.
     *
     * @param methodName The method name
     * @return The average execution time
     */
    public double getAverageExecutionTime(String methodName) {
        long calls = getCallCount(methodName);
        if (calls == 0) return 0.0;
        
        long totalTime = getTotalExecutionTime(methodName);
        return (totalTime / calls) / 1_000_000.0; // Convert nanoseconds to milliseconds
    }
    
    /**
     * Gets the average response time across all methods in milliseconds.
     *
     * @return The average response time
     */
    public double getAverageResponseTime() {
        return averageResponseTime;
    }
    
    /**
     * Sets the average response time.
     *
     * @param averageResponseTime The average response time
     */
    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }
    
    /**
     * Gets the CPU usage.
     *
     * @return The CPU usage
     */
    public double getCpuUsage() {
        return cpuUsage;
    }
    
    /**
     * Sets the CPU usage.
     *
     * @param cpuUsage The CPU usage
     */
    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }
    
    /**
     * Gets the memory usage.
     *
     * @return The memory usage
     */
    public long getMemoryUsage() {
        return memoryUsage;
    }
    
    /**
     * Sets the memory usage.
     *
     * @param memoryUsage The memory usage
     */
    public void setMemoryUsage(long memoryUsage) {
        this.memoryUsage = memoryUsage;
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
     * Gets the last update time.
     *
     * @return The last update time
     */
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    /**
     * Sets the last update time.
     *
     * @param lastUpdateTime The last update time
     */
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    /**
     * Resets all counters.
     */
    public void reset() {
        callCounts.clear();
        totalExecutionTimes.clear();
        maxExecutionTimes.clear();
        errorCount = 0;
        warningCount = 0;
        lastUpdateTime = System.currentTimeMillis();
    }
} 
