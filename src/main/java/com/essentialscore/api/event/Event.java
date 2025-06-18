package com.essentialscore.api.event;

/**
 * Base class for all events in the event system.
 * Events can be cancellable or not.
 */
public abstract class Event {
    private boolean cancelled = false;
    private final boolean cancellable;
    
    /**
     * Creates a new event.
     *
     * @param cancellable Whether this event can be cancelled
     */
    public Event(boolean cancellable) {
        this.cancellable = cancellable;
    }
    
    /**
     * Creates a new non-cancellable event.
     */
    public Event() {
        this(false);
    }
    
    /**
     * Checks if this event is cancellable.
     *
     * @return true if this event can be cancelled
     */
    public boolean isCancellable() {
        return cancellable;
    }
    
    /**
     * Checks if this event has been cancelled.
     * If the event is not cancellable, this will always return false.
     *
     * @return true if the event has been cancelled
     */
    public boolean isCancelled() {
        return cancellable && cancelled;
    }
    
    /**
     * Sets the cancelled state of this event.
     * Has no effect if the event is not cancellable.
     *
     * @param cancelled Whether the event should be cancelled
     * @throws IllegalStateException if the event is not cancellable
     */
    public void setCancelled(boolean cancelled) {
        if (!cancellable) {
            throw new IllegalStateException("Cannot cancel a non-cancellable event");
        }
        this.cancelled = cancelled;
    }
    
    /**
     * Gets the event name, which is the simple class name by default.
     * Subclasses can override this to provide a more specific name.
     *
     * @return The event name
     */
    public String getEventName() {
        return getClass().getSimpleName();
    }
    
    /**
     * Gets whether handlers of this event type can be unregistered.
     * Defaults to true, but can be overridden by subclasses.
     *
     * @return true if handlers can be unregistered
     */
    public boolean canUnregister() {
        return true;
    }
} 
