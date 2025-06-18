package com.essentialscore.api.event;

/**
 * Defines the priority levels for event handlers.
 * Event handlers with higher priorities are called first.
 */
public enum EventPriority {
    /**
     * Lowest priority, called last
     */
    LOWEST(0),
    
    /**
     * Low priority
     */
    LOW(1),
    
    /**
     * Normal priority (default)
     */
    NORMAL(2),
    
    /**
     * High priority
     */
    HIGH(3),
    
    /**
     * Highest priority, called first
     */
    HIGHEST(4),
    
    /**
     * Monitor priority, called after all other priorities
     * Should only be used for monitoring events, not modifying them
     */
    MONITOR(5);
    
    private final int value;
    
    EventPriority(int value) {
        this.value = value;
    }
    
    /**
     * Gets the numeric value of this priority.
     * Higher values are higher priority.
     *
     * @return The priority value
     */
    public int getValue() {
        return value;
    }
} 
