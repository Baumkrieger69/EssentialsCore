package com.essentialscore.api.scheduling;

/**
 * Exception thrown when a circuit breaker is open and prevents execution.
 */
public class CircuitBreakerException extends RuntimeException {
    
    /**
     * Creates a new circuit breaker exception.
     *
     * @param message The exception message
     */
    public CircuitBreakerException(String message) {
        super(message);
    }
    
    /**
     * Creates a new circuit breaker exception.
     *
     * @param message The exception message
     * @param cause The exception cause
     */
    public CircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new circuit breaker exception.
     *
     * @param cause The exception cause
     */
    public CircuitBreakerException(Throwable cause) {
        super(cause);
    }
}
