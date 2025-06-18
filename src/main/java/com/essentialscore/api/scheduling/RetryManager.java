package com.essentialscore.api.scheduling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages retry logic for failed tasks with circuit breaker pattern.
 */
public class RetryManager {
    private static final Logger LOGGER = Logger.getLogger(RetryManager.class.getName());
    
    // Default base delay in milliseconds
    private static final long DEFAULT_BASE_DELAY_MS = 1000;
    
    // Map of resource IDs to their circuit breaker status
    private final Map<String, CircuitBreaker> circuitBreakers;
    
    /**
     * Creates a new retry manager.
     */
    public RetryManager() {
        this.circuitBreakers = new ConcurrentHashMap<>();
    }
    
    /**
     * Checks if a task should be retried.
     *
     * @param task The task
     * @return true if the task should be retried
     */
    public boolean shouldRetry(ScheduledTask task) {
        // Check if we've reached the maximum number of retries
        if (task.getRetryCount() >= task.getMaxRetries()) {
            LOGGER.info("Task " + task.getName() + " has reached maximum retries: " + task.getMaxRetries());
            return false;
        }
        
        // Check circuit breaker if the task has a resource ID
        String resourceId = task.getResourceId();
        if (resourceId != null) {
            CircuitBreaker breaker = getOrCreateCircuitBreaker(resourceId);
            
            if (breaker.isOpen()) {
                LOGGER.warning("Circuit breaker is open for resource " + resourceId + ", not retrying task: " + task.getName());
                return false;
            }
            
            // Record a failure
            breaker.recordFailure();
        }
        
        return true;
    }
    
    /**
     * Gets the retry delay in milliseconds for a task.
     *
     * @param task The task
     * @return The retry delay
     */
    public long getRetryDelayMillis(ScheduledTask task) {
        RetryStrategy strategy = task.getRetryStrategy();
        int retryCount = task.getRetryCount();
        
        // Use the retry strategy to calculate the delay
        return strategy.getDelayMillis(retryCount + 1, DEFAULT_BASE_DELAY_MS);
    }
    
    /**
     * Records a success for a resource.
     *
     * @param resourceId The resource ID
     */
    public void recordSuccess(String resourceId) {
        if (resourceId == null) {
            return;
        }
        
        CircuitBreaker breaker = getOrCreateCircuitBreaker(resourceId);
        breaker.recordSuccess();
    }
    
    /**
     * Records a failure for a resource.
     *
     * @param resourceId The resource ID
     */
    public void recordFailure(String resourceId) {
        if (resourceId == null) {
            return;
        }
        
        CircuitBreaker breaker = getOrCreateCircuitBreaker(resourceId);
        breaker.recordFailure();
    }
    
    /**
     * Gets or creates a circuit breaker for a resource.
     *
     * @param resourceId The resource ID
     * @return The circuit breaker
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String resourceId) {
        return circuitBreakers.computeIfAbsent(resourceId, id -> new CircuitBreaker(id));
    }
    
    /**
     * Gets the circuit breakers.
     *
     * @return The circuit breakers
     */
    public Map<String, CircuitBreaker> getCircuitBreakers() {
        return circuitBreakers;
    }
    
    /**
     * Resets all circuit breakers.
     */
    public void resetAllCircuitBreakers() {
        circuitBreakers.values().forEach(CircuitBreaker::reset);
        LOGGER.info("Reset all circuit breakers");
    }
    
    /**
     * Implementation of the Circuit Breaker pattern.
     */
    public static class CircuitBreaker {
        // Circuit breaker states
        private enum State {
            CLOSED,      // Normal operation, allowing requests
            OPEN,        // Failing fast, not allowing requests
            HALF_OPEN    // Testing if the system has recovered
        }
        
        private static final int FAILURE_THRESHOLD = 5;  // Number of failures before opening
        private static final int SUCCESS_THRESHOLD = 2;  // Number of successes to close again
        private static final long RESET_TIMEOUT_MS = 30000;  // 30 seconds timeout before half-open
        
        private final String resourceId;
        private State state;
        private int failureCount;
        private int successCount;
        private long lastFailureTime;
        
        /**
         * Creates a new circuit breaker.
         *
         * @param resourceId The resource ID
         */
        public CircuitBreaker(String resourceId) {
            this.resourceId = resourceId;
            this.state = State.CLOSED;
            this.failureCount = 0;
            this.successCount = 0;
            this.lastFailureTime = 0;
        }
        
        /**
         * Records a success.
         */
        public synchronized void recordSuccess() {
            if (state == State.HALF_OPEN) {
                successCount++;
                if (successCount >= SUCCESS_THRESHOLD) {
                    // Transition to closed state
                    state = State.CLOSED;
                    successCount = 0;
                    failureCount = 0;
                    LOGGER.info("Circuit breaker closed for resource " + resourceId);
                }
            } else if (state == State.CLOSED) {
                // Reset failure count on success
                failureCount = Math.max(0, failureCount - 1);
            }
        }
        
        /**
         * Records a failure.
         */
        public synchronized void recordFailure() {
            lastFailureTime = System.currentTimeMillis();
            
            if (state == State.HALF_OPEN) {
                // Transition back to open state
                state = State.OPEN;
                successCount = 0;
                LOGGER.warning("Circuit breaker reopened for resource " + resourceId);
            } else if (state == State.CLOSED) {
                failureCount++;
                if (failureCount >= FAILURE_THRESHOLD) {
                    // Transition to open state
                    state = State.OPEN;
                    LOGGER.warning("Circuit breaker opened for resource " + resourceId + " after " + failureCount + " failures");
                }
            }
        }
        
        /**
         * Checks if the circuit breaker is open.
         *
         * @return true if the circuit breaker is open
         */
        public synchronized boolean isOpen() {
            if (state == State.OPEN) {
                // Check if reset timeout has elapsed
                long now = System.currentTimeMillis();
                if (now - lastFailureTime >= RESET_TIMEOUT_MS) {
                    // Transition to half-open state
                    state = State.HALF_OPEN;
                    successCount = 0;
                    LOGGER.info("Circuit breaker half-open for resource " + resourceId);
                    return false;
                }
                return true;
            }
            return false;
        }
        
        /**
         * Resets the circuit breaker to closed state.
         */
        public synchronized void reset() {
            state = State.CLOSED;
            failureCount = 0;
            successCount = 0;
            LOGGER.info("Circuit breaker reset for resource " + resourceId);
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
         * Gets the state.
         *
         * @return The state
         */
        public String getState() {
            return state.name();
        }
        
        /**
         * Gets the failure count.
         *
         * @return The failure count
         */
        public int getFailureCount() {
            return failureCount;
        }
        
        /**
         * Gets the success count.
         *
         * @return The success count
         */
        public int getSuccessCount() {
            return successCount;
        }
    }
} 
