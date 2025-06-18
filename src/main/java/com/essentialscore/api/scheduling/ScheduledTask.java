package com.essentialscore.api.scheduling;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a scheduled task in the system.
 */
public class ScheduledTask implements Serializable, Comparable<ScheduledTask> {
    private static final long serialVersionUID = 1L;
    
    private final UUID id;
    private final String name;
    private final transient Runnable runnable;
    private final TaskPriority priority;
    private final boolean async;
    private final boolean distributed;
    private final String cronExpression;
    private final long periodMillis;
    private final Set<UUID> dependencies;
    private final int maxRetries;
    private final RetryStrategy retryStrategy;
    private final String resourceId;
    private final transient Consumer<Throwable> failureCallback;
    private final long expirationTime;
    
    private long nextExecutionTime;
    private int retryCount;
    private TaskState state;
    
    /**
     * Creates a new scheduled task.
     *
     * @param builder The task builder
     */
    private ScheduledTask(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.name = builder.name;
        this.runnable = builder.runnable;
        this.priority = builder.priority;
        this.async = builder.async;
        this.distributed = builder.distributed;
        this.cronExpression = builder.cronExpression;
        this.periodMillis = builder.periodMillis;
        this.dependencies = builder.dependencies;
        this.maxRetries = builder.maxRetries;
        this.retryStrategy = builder.retryStrategy;
        this.resourceId = builder.resourceId;
        this.failureCallback = builder.failureCallback;
        this.expirationTime = builder.expirationTime;
        
        this.nextExecutionTime = builder.initialExecutionTime;
        this.retryCount = 0;
        this.state = TaskState.SCHEDULED;
    }
    
    /**
     * Gets the task ID.
     *
     * @return The task ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Gets the task name.
     *
     * @return The task name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the task runnable.
     *
     * @return The task runnable
     */
    public Runnable getRunnable() {
        return runnable;
    }
    
    /**
     * Gets the task priority.
     *
     * @return The task priority
     */
    public TaskPriority getPriority() {
        return priority;
    }
    
    /**
     * Checks if the task is asynchronous.
     *
     * @return true if the task is asynchronous
     */
    public boolean isAsync() {
        return async;
    }
    
    /**
     * Checks if the task is distributed.
     *
     * @return true if the task is distributed
     */
    public boolean isDistributed() {
        return distributed;
    }
    
    /**
     * Gets the cron expression.
     *
     * @return The cron expression
     */
    public String getCronExpression() {
        return cronExpression;
    }
    
    /**
     * Gets the period in milliseconds.
     *
     * @return The period
     */
    public long getPeriodMillis() {
        return periodMillis;
    }
    
    /**
     * Gets the dependencies.
     *
     * @return The dependencies
     */
    public Set<UUID> getDependencies() {
        return dependencies;
    }
    
    /**
     * Gets the maximum number of retries.
     *
     * @return The maximum number of retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }
    
    /**
     * Gets the retry strategy.
     *
     * @return The retry strategy
     */
    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
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
     * Gets the failure callback.
     *
     * @return The failure callback
     */
    public Consumer<Throwable> getFailureCallback() {
        return failureCallback;
    }
    
    /**
     * Gets the expiration time.
     *
     * @return The expiration time
     */
    public long getExpirationTime() {
        return expirationTime;
    }
    
    /**
     * Gets the next execution time.
     *
     * @return The next execution time
     */
    public long getNextExecutionTime() {
        return nextExecutionTime;
    }
    
    /**
     * Gets the retry count.
     *
     * @return The retry count
     */
    public int getRetryCount() {
        return retryCount;
    }
    
    /**
     * Gets the task state.
     *
     * @return The task state
     */
    public TaskState getState() {
        return state;
    }
    
    /**
     * Sets the task state.
     *
     * @param state The task state
     */
    public void setState(TaskState state) {
        this.state = state;
    }
    
    /**
     * Increments the retry count.
     *
     * @return The new retry count
     */
    public int incrementRetryCount() {
        return ++retryCount;
    }
    
    /**
     * Checks if the task should be rescheduled.
     *
     * @return true if the task should be rescheduled
     */
    public boolean shouldReschedule() {
        // One-time tasks don't reschedule
        if (periodMillis <= 0 && cronExpression == null) {
            return false;
        }
        
        // Check if task has expired
        return !isExpired();
    }
    
    /**
     * Checks if the task has expired.
     *
     * @return true if the task has expired
     */
    public boolean isExpired() {
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }
    
    /**
     * Updates the next execution time.
     */
    public void updateNextExecutionTime() {
        if (cronExpression != null) {
            // Calculate next execution time based on cron expression
            nextExecutionTime = CronParser.getNextExecutionTime(cronExpression);
        } else if (periodMillis > 0) {
            // Simple periodic task
            nextExecutionTime = System.currentTimeMillis() + periodMillis;
        }
    }
    
    @Override
    public int compareTo(ScheduledTask other) {
        // First compare by priority (higher priority first)
        int priorityCompare = other.priority.compareTo(this.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        
        // Then compare by execution time (earlier first)
        return Long.compare(this.nextExecutionTime, other.nextExecutionTime);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledTask that = (ScheduledTask) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ScheduledTask{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", nextExecutionTime=" + Instant.ofEpochMilli(nextExecutionTime) +
                ", state=" + state +
                '}';
    }
    
    /**
     * Creates a new task builder.
     *
     * @return The task builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating scheduled tasks.
     */
    public static class Builder {
        private UUID id;
        private String name = "unnamed-task";
        private Runnable runnable;
        private TaskPriority priority = TaskPriority.NORMAL;
        private boolean async = false;
        private boolean distributed = false;
        private String cronExpression;
        private long periodMillis = 0;
        private long initialExecutionTime = System.currentTimeMillis();
        private final Set<UUID> dependencies = new HashSet<>();
        private int maxRetries = 3;
        private RetryStrategy retryStrategy = RetryStrategy.EXPONENTIAL_BACKOFF;
        private String resourceId;
        private Consumer<Throwable> failureCallback;
        private long expirationTime = 0; // 0 means no expiration
        
        /**
         * Sets the task ID.
         *
         * @param id The task ID
         * @return The builder
         */
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the task name.
         *
         * @param name The task name
         * @return The builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Sets the task runnable.
         *
         * @param runnable The task runnable
         * @return The builder
         */
        public Builder runnable(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }
        
        /**
         * Sets the task priority.
         *
         * @param priority The task priority
         * @return The builder
         */
        public Builder priority(TaskPriority priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * Sets the task as asynchronous.
         *
         * @param async true if the task is asynchronous
         * @return The builder
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }
        
        /**
         * Sets the task as distributed.
         *
         * @param distributed true if the task is distributed
         * @return The builder
         */
        public Builder distributed(boolean distributed) {
            this.distributed = distributed;
            return this;
        }
        
        /**
         * Sets the cron expression.
         *
         * @param cronExpression The cron expression
         * @return The builder
         */
        public Builder cronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
            // Calculate initial execution time based on cron
            if (cronExpression != null) {
                this.initialExecutionTime = CronParser.getNextExecutionTime(cronExpression);
            }
            return this;
        }
        
        /**
         * Sets the period in milliseconds.
         *
         * @param periodMillis The period
         * @return The builder
         */
        public Builder period(long periodMillis) {
            this.periodMillis = periodMillis;
            return this;
        }
        
        /**
         * Sets the execution time.
         *
         * @param executionTime The execution time
         * @return The builder
         */
        public Builder executeAt(long executionTime) {
            this.initialExecutionTime = executionTime;
            return this;
        }
        
        /**
         * Sets the task dependencies.
         *
         * @param dependencies The dependencies
         * @return The builder
         */
        public Builder dependencies(UUID... dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            return this;
        }
        
        /**
         * Sets the maximum number of retries.
         *
         * @param maxRetries The maximum number of retries
         * @return The builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        /**
         * Sets the retry strategy.
         *
         * @param retryStrategy The retry strategy
         * @return The builder
         */
        public Builder retryStrategy(RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
        }
        
        /**
         * Sets the resource ID.
         *
         * @param resourceId The resource ID
         * @return The builder
         */
        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }
        
        /**
         * Sets the failure callback.
         *
         * @param failureCallback The failure callback
         * @return The builder
         */
        public Builder onFailure(Consumer<Throwable> failureCallback) {
            this.failureCallback = failureCallback;
            return this;
        }
        
        /**
         * Sets the expiration time.
         *
         * @param expirationTime The expiration time
         * @return The builder
         */
        public Builder expiresAt(long expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }
        
        /**
         * Builds the scheduled task.
         *
         * @return The scheduled task
         */
        public ScheduledTask build() {
            if (runnable == null) {
                throw new IllegalStateException("Task runnable cannot be null");
            }
            return new ScheduledTask(this);
        }
    }
} 
