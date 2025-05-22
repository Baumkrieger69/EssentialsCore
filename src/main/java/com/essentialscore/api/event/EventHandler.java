package com.essentialscore.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation für Event-Handler-Methoden
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
    
    /**
     * Die Priorität des Event-Handlers
     * @return Die Priorität
     */
    EventPriority priority() default EventPriority.NORMAL;
    
    /**
     * Ob der Event-Handler ignoriert werden soll, wenn der Event gecancelled wurde
     * @return true wenn ignoriert
     */
    boolean ignoreCancelled() default false;
} 