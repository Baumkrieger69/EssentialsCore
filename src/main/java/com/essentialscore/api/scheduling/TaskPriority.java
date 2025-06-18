package com.essentialscore.api.scheduling;

/**
 * Defines priority levels for scheduled tasks.
 */
public enum TaskPriority {
    /**
     * Low priority tasks are executed after normal and high priority tasks.
     */
    LOW(0),
    
    /**
     * Normal priority tasks are the default.
     */
    NORMAL(5),
    
    /**
     * High priority tasks are executed before normal and low priority tasks.
     */
    HIGH(10),
    
    /**
     * Critical priority tasks are executed before all other tasks.
     */
    CRITICAL(15);
    
    private final int value;
    
    /**
     * Creates a new task priority.
     *
     * @param value The priority value
     */
    TaskPriority(int value) {
        this.value = value;
    }
    
    /**
     * Gets the priority value.
     *
     * @return The priority value
     */
    public int getValue() {
        return value;
    }
} 
