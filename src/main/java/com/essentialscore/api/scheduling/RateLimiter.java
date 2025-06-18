package com.essentialscore.api.scheduling;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Implements rate limiting for tasks based on resource IDs.
 */
public class RateLimiter {
    private static final Logger LOGGER = Logger.getLogger(RateLimiter.class.getName());
    
    private static class RateLimit {
        private final int maxExecutions;
        private final long windowMillis;
        private final long[] executionTimes;
        private int currentIndex;
        
        public RateLimit(int maxExecutions, Duration window) {
            this.maxExecutions = maxExecutions;
            this.windowMillis = window.toMillis();
            this.executionTimes = new long[maxExecutions];
            this.currentIndex = 0;
        }
        
        /**
         * Tries to record an execution within the rate limit.
         *
         * @return true if the execution is allowed
         */
        public synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            
            // Check if we've reached the maximum executions within the time window
            if (executionTimes[currentIndex] != 0 && 
                    now - executionTimes[currentIndex] < windowMillis) {
                return false;
            }
            
            // Record this execution
            executionTimes[currentIndex] = now;
            currentIndex = (currentIndex + 1) % maxExecutions;
            return true;
        }
        
        /**
         * Gets the time when the next execution will be allowed.
         *
         * @return The time in milliseconds
         */
        public synchronized long getNextAllowedTime() {
            long now = System.currentTimeMillis();
            long oldestExecution = now;
            
            // Find the oldest execution in our window
            for (long time : executionTimes) {
                if (time > 0 && time < oldestExecution) {
                    oldestExecution = time;
                }
            }
            
            // If we haven't filled the window yet, we can execute now
            if (oldestExecution == now) {
                return now;
            }
            
            // Otherwise, we need to wait until the oldest execution is outside the window
            return oldestExecution + windowMillis;
        }
    }
    
    private final Map<String, RateLimit> rateLimits;
    
    /**
     * Creates a new rate limiter.
     */
    public RateLimiter() {
        this.rateLimits = new ConcurrentHashMap<>();
    }
    
    /**
     * Sets a rate limit for a resource.
     *
     * @param resourceId The resource ID
     * @param maxExecutions The maximum number of executions
     * @param window The time window
     */
    public void setLimit(String resourceId, int maxExecutions, Duration window) {
        if (maxExecutions <= 0) {
            throw new IllegalArgumentException("Max executions must be positive");
        }
        
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Window must be positive");
        }
        
        rateLimits.put(resourceId, new RateLimit(maxExecutions, window));
        LOGGER.info("Set rate limit for resource " + resourceId + ": " 
                + maxExecutions + " executions per " + window.toSeconds() + " seconds");
    }
    
    /**
     * Removes a rate limit for a resource.
     *
     * @param resourceId The resource ID
     */
    public void removeLimit(String resourceId) {
        rateLimits.remove(resourceId);
        LOGGER.info("Removed rate limit for resource " + resourceId);
    }
    
    /**
     * Checks if a task is allowed to execute based on its resource ID.
     *
     * @param task The task
     * @return true if the task is allowed to execute
     */
    public boolean allowExecution(ScheduledTask task) {
        String resourceId = task.getResourceId();
        
        // If task has no resource ID or there's no rate limit for this resource,
        // allow execution
        if (resourceId == null || !rateLimits.containsKey(resourceId)) {
            return true;
        }
        
        RateLimit limit = rateLimits.get(resourceId);
        boolean allowed = limit.tryAcquire();
        
        if (!allowed) {
            LOGGER.fine("Rate limit reached for resource " + resourceId + ", task delayed: " + task.getName());
        }
        
        return allowed;
    }
    
    /**
     * Gets the next time when a task will be allowed to execute.
     *
     * @param task The task
     * @return The next allowed time in milliseconds
     */
    public long getNextAllowedTime(ScheduledTask task) {
        String resourceId = task.getResourceId();
        
        // If task has no resource ID or there's no rate limit for this resource,
        // allow execution now
        if (resourceId == null || !rateLimits.containsKey(resourceId)) {
            return System.currentTimeMillis();
        }
        
        return rateLimits.get(resourceId).getNextAllowedTime();
    }
} 
