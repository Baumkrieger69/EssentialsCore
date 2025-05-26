package com.essentialscore.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
<<<<<<< HEAD
 * Annotation für Event-Handler-Methoden
=======
 * Annotation to mark methods as event handlers.
 * Methods annotated with this will be registered as event handlers
 * for the event type specified in the method parameter.
>>>>>>> 1cd13da (Das ist Dumm)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
<<<<<<< HEAD
    
    /**
     * Die Priorität des Event-Handlers
     * @return Die Priorität
=======
    /**
     * The priority of the event handler.
     * Higher priority handlers are called first.
     *
     * @return The handler priority
>>>>>>> 1cd13da (Das ist Dumm)
     */
    EventPriority priority() default EventPriority.NORMAL;
    
    /**
<<<<<<< HEAD
     * Ob der Event-Handler ignoriert werden soll, wenn der Event gecancelled wurde
     * @return true wenn ignoriert
=======
     * Whether the handler should still be called if the event has been cancelled.
     * This is only relevant for cancellable events.
     *
     * @return true if the handler should be called for cancelled events
>>>>>>> 1cd13da (Das ist Dumm)
     */
    boolean ignoreCancelled() default false;
} 