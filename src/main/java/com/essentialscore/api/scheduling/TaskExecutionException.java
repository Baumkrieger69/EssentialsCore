package com.essentialscore.api.scheduling;

/**
 * Exception thrown when a task execution fails.
 */
public class TaskExecutionException extends RuntimeException {
    
    /**
     * Creates a new task execution exception.
     *
     * @param message The exception message
     */
    public TaskExecutionException(String message) {
        super(message);
    }
    
    /**
     * Creates a new task execution exception.
     *
     * @param message The exception message
     * @param cause The exception cause
     */
    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new task execution exception.
     *
     * @param cause The exception cause
     */
    public TaskExecutionException(Throwable cause) {
        super(cause);
    }
}
