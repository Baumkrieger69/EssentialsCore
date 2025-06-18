package com.essentialscore.api.scheduling;

/**
 * Exception thrown when retry attempts are exhausted.
 */
public class RetryExhaustedException extends RuntimeException {
    
    /**
     * Creates a new retry exhausted exception.
     *
     * @param message The exception message
     */
    public RetryExhaustedException(String message) {
        super(message);
    }
    
    /**
     * Creates a new retry exhausted exception.
     *
     * @param message The exception message
     * @param cause The exception cause
     */
    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new retry exhausted exception.
     *
     * @param cause The exception cause
     */
    public RetryExhaustedException(Throwable cause) {
        super(cause);
    }
}
