package com.essentialscore.api.module;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks performance data for a module.
 */
public class ModulePerformanceData {
    private final String moduleName;
    private final AtomicLong executionCount;
    private final AtomicLong totalExecutionTime;
    private final LocalDateTime startTime;
    private volatile long lastExecutionTime;
    private volatile LocalDateTime lastExecuted;

    /**
     * Creates new performance data for a module.
     *
     * @param moduleName The module name
     */
    public ModulePerformanceData(String moduleName) {
        this.moduleName = moduleName;
        this.executionCount = new AtomicLong(0);
        this.totalExecutionTime = new AtomicLong(0);
        this.startTime = LocalDateTime.now();
        this.lastExecutionTime = 0;
        this.lastExecuted = null;
    }

    /**
     * Gets the module name.
     *
     * @return The module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Gets the total number of executions.
     *
     * @return The execution count
     */
    public long getExecutionCount() {
        return executionCount.get();
    }

    /**
     * Gets the total execution time in milliseconds.
     *
     * @return The total execution time
     */
    public long getTotalExecutionTime() {
        return totalExecutionTime.get();
    }

    /**
     * Gets the average execution time in milliseconds.
     *
     * @return The average execution time, or 0 if no executions
     */
    public double getAverageExecutionTime() {
        long count = executionCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalExecutionTime.get() / count;
    }

    /**
     * Gets the minimum execution time (placeholder implementation).
     *
     * @return The minimum execution time
     */
    public long getMinExecutionTime() {
        // Simplified implementation - would need tracking of min/max in real scenario
        return lastExecutionTime > 0 ? lastExecutionTime : 0;
    }

    /**
     * Gets the maximum execution time (placeholder implementation).
     *
     * @return The maximum execution time
     */
    public long getMaxExecutionTime() {
        // Simplified implementation - would need tracking of min/max in real scenario
        return lastExecutionTime;
    }

    /**
     * Gets the memory usage (placeholder implementation).
     *
     * @return The estimated memory usage in bytes
     */
    public long getMemoryUsage() {
        // Simplified implementation - would need actual memory tracking
        return executionCount.get() * 1024; // Estimate 1KB per execution
    }

    /**
     * Gets the last execution time in milliseconds.
     *
     * @return The last execution time
     */
    public long getLastExecutionTime() {
        return lastExecutionTime;
    }

    /**
     * Gets when the module was last executed.
     *
     * @return The last execution timestamp, or null if never executed
     */
    public LocalDateTime getLastExecuted() {
        return lastExecuted;
    }

    /**
     * Gets when performance tracking started.
     *
     * @return The start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Records an execution with the given duration.
     *
     * @param durationMs The execution duration in milliseconds
     */
    public void recordExecution(long durationMs) {
        executionCount.incrementAndGet();
        totalExecutionTime.addAndGet(durationMs);
        lastExecutionTime = durationMs;
        lastExecuted = LocalDateTime.now();
    }

    /**
     * Resets all performance data.
     */
    public void reset() {
        executionCount.set(0);
        totalExecutionTime.set(0);
        lastExecutionTime = 0;
        lastExecuted = null;
    }

    @Override
    public String toString() {
        return "ModulePerformanceData{" +
                "moduleName='" + moduleName + '\'' +
                ", executionCount=" + executionCount.get() +
                ", totalExecutionTime=" + totalExecutionTime.get() +
                ", averageExecutionTime=" + getAverageExecutionTime() +
                ", lastExecutionTime=" + lastExecutionTime +
                ", lastExecuted=" + lastExecuted +
                ", startTime=" + startTime +
                '}';
    }
}
