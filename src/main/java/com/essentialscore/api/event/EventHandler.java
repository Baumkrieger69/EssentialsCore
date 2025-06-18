package com.essentialscore.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods as event handlers.
 * Methods annotated with this will be registered as event handlers
 * for the event type specified in the method parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
    /**
     * The priority of the event handler.
     * Higher priority handlers are called first.
     *
     * @return The handler priority
     */
    EventPriority priority() default EventPriority.NORMAL;
    
    /**
     * Whether the handler should still be called if the event has been cancelled.
     * This is only relevant for cancellable events.
     *
     * @return true if the handler should be called for cancelled events
     */
    boolean ignoreCancelled() default false;
} 
