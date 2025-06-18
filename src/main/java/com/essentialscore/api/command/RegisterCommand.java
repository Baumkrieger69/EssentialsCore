package com.essentialscore.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for registering command methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RegisterCommand {
    /**
     * The command name.
     * If empty, the method name will be used.
     *
     * @return The command name
     */
    String name() default "";
    
    /**
     * The command description.
     *
     * @return The command description
     */
    String description() default "";
    
    /**
     * The command usage.
     *
     * @return The command usage
     */
    String usage() default "";
    
    /**
     * The command aliases.
     *
     * @return The command aliases
     */
    String[] aliases() default {};
    
    /**
     * The permission required to use the command.
     *
     * @return The permission
     */
    String permission() default "";
    
    /**
     * The minimum number of arguments required.
     *
     * @return The minimum arguments
     */
    int minArgs() default 0;
    
    /**
     * The maximum number of arguments allowed.
     *
     * @return The maximum arguments, or -1 for unlimited
     */
    int maxArgs() default -1;
    
    /**
     * The command category.
     *
     * @return The category
     */
    String category() default "General";
    
    /**
     * Detailed help information.
     *
     * @return The detailed help
     */
    String detailedHelp() default "";
    
    /**
     * Example usages of the command.
     *
     * @return The examples
     */
    String[] examples() default {};
    
    /**
     * Whether the command is hidden from help listings.
     *
     * @return true if the command is hidden
     */
    boolean hidden() default false;
    
    /**
     * The cooldown time in seconds.
     *
     * @return The cooldown time, or 0 for no cooldown
     */
    int cooldown() default 0;
} 
