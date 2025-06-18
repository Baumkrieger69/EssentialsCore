package com.essentialscore.api.scheduling;

/**
 * Defines retry strategies for failed tasks.
 */
public enum RetryStrategy {
    /**
     * Retry immediately without delay.
     */
    IMMEDIATE {
        @Override
        public long getDelayMillis(int retryCount, long baseDelayMillis) {
            return 0;
        }
    },
    
    /**
     * Retry with a fixed delay.
     */
    FIXED_DELAY {
        @Override
        public long getDelayMillis(int retryCount, long baseDelayMillis) {
            return baseDelayMillis;
        }
    },
    
    /**
     * Retry with an exponential backoff delay.
     * The delay increases exponentially with each retry.
     */
    EXPONENTIAL_BACKOFF {
        @Override
        public long getDelayMillis(int retryCount, long baseDelayMillis) {
            return baseDelayMillis * (long) Math.pow(2, retryCount - 1);
        }
    },
    
    /**
     * Retry with a linear backoff delay.
     * The delay increases linearly with each retry.
     */
    LINEAR_BACKOFF {
        @Override
        public long getDelayMillis(int retryCount, long baseDelayMillis) {
            return baseDelayMillis * retryCount;
        }
    },
    
    /**
     * Retry with a random delay within a range.
     * Helps prevent thundering herd problems in distributed systems.
     */
    RANDOM_BACKOFF {
        @Override
        public long getDelayMillis(int retryCount, long baseDelayMillis) {
            double randomFactor = 0.5 + Math.random(); // 0.5 to 1.5
            return (long) (baseDelayMillis * retryCount * randomFactor);
        }
    };
    
    /**
     * Calculates the delay in milliseconds for a retry.
     *
     * @param retryCount The current retry count
     * @param baseDelayMillis The base delay in milliseconds
     * @return The delay in milliseconds
     */
    public abstract long getDelayMillis(int retryCount, long baseDelayMillis);
} 
