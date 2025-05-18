package com.essentialscore.api;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Interface for defining module commands.
 * This interface provides a clean abstraction for modules to define their commands
 * without directly depending on the core implementation.
 */
public interface CommandDefinition {
    /**
     * Gets the name of the command
     * 
     * @return The command name
     */
    String getName();
    
    /**
     * Gets the description of the command
     * 
     * @return The command description
     */
    String getDescription();
    
    /**
     * Gets the usage message for the command
     * 
     * @return The usage message
     */
    String getUsage();
    
    /**
     * Gets the aliases for the command
     * 
     * @return List of aliases
     */
    List<String> getAliases();
    
    /**
     * Gets the permission required to use the command
     * 
     * @return The permission string
     */
    String getPermission();
    
    /**
     * Gets the name of the module that owns this command
     * 
     * @return The module name
     */
    String getModuleName();
    
    /**
     * Gets the tab completion options for a specific argument index
     * 
     * @param argIndex The argument index (0-based)
     * @return List of tab completion options or null if none are defined
     */
    List<String> getTabCompletionOptions(int argIndex);
    
    /**
     * Executes the command
     * 
     * @param sender The command sender
     * @param label The command label used
     * @param args The command arguments
     * @return true if the command executed successfully
     */
    boolean execute(CommandSender sender, String label, String[] args);
    
    /**
     * Provides tab completion options for the command
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return List of tab completion options
     */
    List<String> tabComplete(CommandSender sender, String[] args);
} 