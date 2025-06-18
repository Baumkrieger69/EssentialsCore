package com.essentialscore.api.command;

/**
 * Utility class for command creation
 */
public class CommandUtil {
    
    /**
     * Creates a new command builder
     *
     * @param name The command name
     * @param moduleId The module ID
     * @return A new command builder
     */
    public static SimpleCommand.Builder createCommandBuilder(String name, String moduleId) {
        return new SimpleCommand.Builder(name);
    }
} 
