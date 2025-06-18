package com.essentialscore.api.scheduling;

/**
 * Defines possible states for a scheduled task.
 */
public enum TaskState {
    /**
     * The task is scheduled for execution.
     */
    SCHEDULED,
    
    /**
     * The task is currently executing.
     */
    EXECUTING,
    
    /**
     * The task is waiting for its dependencies to complete.
     */
    WAITING_FOR_DEPENDENCIES,
    
    /**
     * The task is rate-limited and waiting to be allowed to execute.
     */
    RATE_LIMITED,
    
    /**
     * The task completed successfully.
     */
    COMPLETED,
    
    /**
     * The task failed and is waiting to be retried.
     */
    RETRY_PENDING,
    
    /**
     * The task failed permanently.
     */
    FAILED,
    
    /**
     * The task was cancelled.
     */
    CANCELLED,
    
    /**
     * The task expired before it could execute.
     */
    EXPIRED
} 
