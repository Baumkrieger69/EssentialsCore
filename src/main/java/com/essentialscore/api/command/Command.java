package com.essentialscore.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation für Befehlsmethoden
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    
    /**
     * Der Name des Befehls
     * @return Der Name
     */
    String name();
    
    /**
     * Die Beschreibung des Befehls
     * @return Die Beschreibung
     */
    String description() default "";
    
    /**
     * Die Verwendung des Befehls
     * @return Die Verwendung
     */
    String usage() default "";
    
    /**
     * Die Berechtigung für den Befehl
     * @return Die Berechtigung
     */
    String permission() default "";
    
    /**
     * Die Aliase für den Befehl
     * @return Die Aliase
     */
    String[] aliases() default {};
} 