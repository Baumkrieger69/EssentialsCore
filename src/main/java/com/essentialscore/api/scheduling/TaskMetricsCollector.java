package com.essentialscore.api.scheduling;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Collects and provides metrics about task execution.
 */
public class TaskMetricsCollector {
    private static final Logger LOGGER = Logger.getLogger(TaskMetricsCollector.class.getName());
    
    // Execution counts
    private final AtomicInteger totalExecutions;
    private final AtomicInteger successfulExecutions;
    private final AtomicInteger failedExecutions;
    
    // Execution timing
    private final AtomicLong totalExecutionTimeMs;
    private final Map<UUID, TaskExecutionMetrics> taskMetrics;
    private final Map<String, ResourceMetrics> resourceMetrics;
    
    // Execution start times for tasks currently running
    private final Map<UUID, Long> executionStartTimes;
    
    /**
     * Creates a new task metrics collector.
     */
    public TaskMetricsCollector() {
        this.totalExecutions = new AtomicInteger(0);
        this.successfulExecutions = new AtomicInteger(0);
        this.failedExecutions = new AtomicInteger(0);
        this.totalExecutionTimeMs = new AtomicLong(0);
        this.taskMetrics = new ConcurrentHashMap<>();
        this.resourceMetrics = new ConcurrentHashMap<>();
        this.executionStartTimes = new ConcurrentHashMap<>();
    }
    
    /**
     * Records the start of a task execution.
     *
     * @param task The task
     */
    public void recordTaskStart(ScheduledTask task) {
        long startTime = System.currentTimeMillis();
        executionStartTimes.put(task.getId(), startTime);
        
        // Update execution count
        totalExecutions.incrementAndGet();
        
        // Update per-task metrics
        TaskExecutionMetrics metrics = taskMetrics.computeIfAbsent(
            task.getId(),
            id -> new TaskExecutionMetrics(task.getName())
        );
        metrics.incrementExecutionCount();
        
        // Update per-resource metrics if applicable
        String resourceId = task.getResourceId();
        if (resourceId != null) {
            ResourceMetrics resource = resourceMetrics.computeIfAbsent(
                resourceId,
                id -> new ResourceMetrics(resourceId)
            );
            resource.incrementExecutionCount();
        }
    }
    
    /**
     * Records the end of a task execution.
     *
     * @param task The task
     * @param success Whether the execution was successful
     */
    public void recordTaskEnd(ScheduledTask task, boolean success) {
        Long startTime = executionStartTimes.remove(task.getId());
        if (startTime == null) {
            LOGGER.warning("Task end recorded without matching start: " + task.getName());
            return;
        }
        
        long endTime = System.currentTimeMillis();
        long executionTimeMs = endTime - startTime;
        
        // Update success/failure counts
        if (success) {
            successfulExecutions.incrementAndGet();
        } else {
            failedExecutions.incrementAndGet();
        }
        
        // Update total execution time
        totalExecutionTimeMs.addAndGet(executionTimeMs);
        
        // Update per-task metrics
        TaskExecutionMetrics metrics = taskMetrics.get(task.getId());
        if (metrics != null) {
            metrics.recordExecutionTime(executionTimeMs);
            if (success) {
                metrics.incrementSuccessCount();
            } else {
                metrics.incrementFailureCount();
            }
        }
        
        // Update per-resource metrics if applicable
        String resourceId = task.getResourceId();
        if (resourceId != null) {
            ResourceMetrics resource = resourceMetrics.get(resourceId);
            if (resource != null) {
                resource.recordExecutionTime(executionTimeMs);
                if (success) {
                    resource.incrementSuccessCount();
                } else {
                    resource.incrementFailureCount();
                }
            }
        }
    }
    
    /**
     * Gets the total number of task executions.
     *
     * @return The total executions
     */
    public int getTotalExecutions() {
        return totalExecutions.get();
    }
    
    /**
     * Gets the number of successful task executions.
     *
     * @return The successful executions
     */
    public int getSuccessfulExecutions() {
        return successfulExecutions.get();
    }
    
    /**
     * Gets the number of failed task executions.
     *
     * @return The failed executions
     */
    public int getFailedExecutions() {
        return failedExecutions.get();
    }
    
    /**
     * Gets the total execution time in milliseconds.
     *
     * @return The total execution time
     */
    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs.get();
    }
    
    /**
     * Gets the average execution time in milliseconds.
     *
     * @return The average execution time
     */
    public double getAverageExecutionTimeMs() {
        int total = totalExecutions.get();
        return total > 0 ? (double) totalExecutionTimeMs.get() / total : 0;
    }
    
    /**
     * Gets the success rate (percentage of successful executions).
     *
     * @return The success rate
     */
    public double getSuccessRate() {
        int total = totalExecutions.get();
        return total > 0 ? (double) successfulExecutions.get() / total * 100 : 0;
    }
    
    /**
     * Gets metrics for a specific task.
     *
     * @param taskId The task ID
     * @return The task metrics
     */
    public TaskExecutionMetrics getTaskMetrics(UUID taskId) {
        return taskMetrics.get(taskId);
    }
    
    /**
     * Gets metrics for a specific resource.
     *
     * @param resourceId The resource ID
     * @return The resource metrics
     */
    public ResourceMetrics getResourceMetrics(String resourceId) {
        return resourceMetrics.get(resourceId);
    }
    
    /**
     * Gets all task metrics.
     *
     * @return The task metrics
     */
    public Map<UUID, TaskExecutionMetrics> getAllTaskMetrics() {
        return new HashMap<>(taskMetrics);
    }
    
    /**
     * Gets all resource metrics.
     *
     * @return The resource metrics
     */
    public Map<String, ResourceMetrics> getAllResourceMetrics() {
        return new HashMap<>(resourceMetrics);
    }
    
    /**
     * Resets all metrics.
     */
    public void reset() {
        totalExecutions.set(0);
        successfulExecutions.set(0);
        failedExecutions.set(0);
        totalExecutionTimeMs.set(0);
        taskMetrics.clear();
        resourceMetrics.clear();
        executionStartTimes.clear();
        
        LOGGER.info("Task metrics reset");
    }
    
    /**
     * Class representing metrics for a specific task.
     */
    public static class TaskExecutionMetrics {
        private final String taskName;
        private final AtomicInteger executionCount;
        private final AtomicInteger successCount;
        private final AtomicInteger failureCount;
        private final AtomicLong totalExecutionTimeMs;
        private final AtomicLong minExecutionTimeMs;
        private final AtomicLong maxExecutionTimeMs;
        private final AtomicLong lastExecutionTimeMs;
        
        /**
         * Creates new task execution metrics.
         *
         * @param taskName The task name
         */
        public TaskExecutionMetrics(String taskName) {
            this.taskName = taskName;
            this.executionCount = new AtomicInteger(0);
            this.successCount = new AtomicInteger(0);
            this.failureCount = new AtomicInteger(0);
            this.totalExecutionTimeMs = new AtomicLong(0);
            this.minExecutionTimeMs = new AtomicLong(Long.MAX_VALUE);
            this.maxExecutionTimeMs = new AtomicLong(0);
            this.lastExecutionTimeMs = new AtomicLong(0);
        }
        
        /**
         * Increments the execution count.
         */
        public void incrementExecutionCount() {
            executionCount.incrementAndGet();
        }
        
        /**
         * Increments the success count.
         */
        public void incrementSuccessCount() {
            successCount.incrementAndGet();
        }
        
        /**
         * Increments the failure count.
         */
        public void incrementFailureCount() {
            failureCount.incrementAndGet();
        }
        
        /**
         * Records an execution time.
         *
         * @param executionTimeMs The execution time in milliseconds
         */
        public void recordExecutionTime(long executionTimeMs) {
            totalExecutionTimeMs.addAndGet(executionTimeMs);
            lastExecutionTimeMs.set(executionTimeMs);
            
            // Update min execution time if lower
            long currentMin = minExecutionTimeMs.get();
            while (executionTimeMs < currentMin) {
                if (minExecutionTimeMs.compareAndSet(currentMin, executionTimeMs)) {
                    break;
                }
                currentMin = minExecutionTimeMs.get();
            }
            
            // Update max execution time if higher
            long currentMax = maxExecutionTimeMs.get();
            while (executionTimeMs > currentMax) {
                if (maxExecutionTimeMs.compareAndSet(currentMax, executionTimeMs)) {
                    break;
                }
                currentMax = maxExecutionTimeMs.get();
            }
        }
        
        /**
         * Gets the task name.
         *
         * @return The task name
         */
        public String getTaskName() {
            return taskName;
        }
        
        /**
         * Gets the execution count.
         *
         * @return The execution count
         */
        public int getExecutionCount() {
            return executionCount.get();
        }
        
        /**
         * Gets the success count.
         *
         * @return The success count
         */
        public int getSuccessCount() {
            return successCount.get();
        }
        
        /**
         * Gets the failure count.
         *
         * @return The failure count
         */
        public int getFailureCount() {
            return failureCount.get();
        }
        
        /**
         * Gets the total execution time in milliseconds.
         *
         * @return The total execution time
         */
        public long getTotalExecutionTimeMs() {
            return totalExecutionTimeMs.get();
        }
        
        /**
         * Gets the minimum execution time in milliseconds.
         *
         * @return The minimum execution time
         */
        public long getMinExecutionTimeMs() {
            long min = minExecutionTimeMs.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        /**
         * Gets the maximum execution time in milliseconds.
         *
         * @return The maximum execution time
         */
        public long getMaxExecutionTimeMs() {
            return maxExecutionTimeMs.get();
        }
        
        /**
         * Gets the last execution time in milliseconds.
         *
         * @return The last execution time
         */
        public long getLastExecutionTimeMs() {
            return lastExecutionTimeMs.get();
        }
        
        /**
         * Gets the average execution time in milliseconds.
         *
         * @return The average execution time
         */
        public double getAverageExecutionTimeMs() {
            int count = executionCount.get();
            return count > 0 ? (double) totalExecutionTimeMs.get() / count : 0;
        }
        
        /**
         * Gets the success rate (percentage of successful executions).
         *
         * @return The success rate
         */
        public double getSuccessRate() {
            int count = executionCount.get();
            return count > 0 ? (double) successCount.get() / count * 100 : 0;
        }
    }
    
    /**
     * Class representing metrics for a specific resource.
     */
    public static class ResourceMetrics {
        private final String resourceId;
        private final AtomicInteger executionCount;
        private final AtomicInteger successCount;
        private final AtomicInteger failureCount;
        private final AtomicLong totalExecutionTimeMs;
        
        /**
         * Creates new resource metrics.
         *
         * @param resourceId The resource ID
         */
        public ResourceMetrics(String resourceId) {
            this.resourceId = resourceId;
            this.executionCount = new AtomicInteger(0);
            this.successCount = new AtomicInteger(0);
            this.failureCount = new AtomicInteger(0);
            this.totalExecutionTimeMs = new AtomicLong(0);
        }
        
        /**
         * Increments the execution count.
         */
        public void incrementExecutionCount() {
            executionCount.incrementAndGet();
        }
        
        /**
         * Increments the success count.
         */
        public void incrementSuccessCount() {
            successCount.incrementAndGet();
        }
        
        /**
         * Increments the failure count.
         */
        public void incrementFailureCount() {
            failureCount.incrementAndGet();
        }
        
        /**
         * Records an execution time.
         *
         * @param executionTimeMs The execution time in milliseconds
         */
        public void recordExecutionTime(long executionTimeMs) {
            totalExecutionTimeMs.addAndGet(executionTimeMs);
        }
        
        /**
         * Gets the resource ID.
         *
         * @return The resource ID
         */
        public String getResourceId() {
            return resourceId;
        }
        
        /**
         * Gets the execution count.
         *
         * @return The execution count
         */
        public int getExecutionCount() {
            return executionCount.get();
        }
        
        /**
         * Gets the success count.
         *
         * @return The success count
         */
        public int getSuccessCount() {
            return successCount.get();
        }
        
        /**
         * Gets the failure count.
         *
         * @return The failure count
         */
        public int getFailureCount() {
            return failureCount.get();
        }
        
        /**
         * Gets the total execution time in milliseconds.
         *
         * @return The total execution time
         */
        public long getTotalExecutionTimeMs() {
            return totalExecutionTimeMs.get();
        }
        
        /**
         * Gets the average execution time in milliseconds.
         *
         * @return The average execution time
         */
        public double getAverageExecutionTimeMs() {
            int count = executionCount.get();
            return count > 0 ? (double) totalExecutionTimeMs.get() / count : 0;
        }
        
        /**
         * Gets the success rate (percentage of successful executions).
         *
         * @return The success rate
         */
        public double getSuccessRate() {
            int count = executionCount.get();
            return count > 0 ? (double) successCount.get() / count * 100 : 0;
        }
    }
} 
