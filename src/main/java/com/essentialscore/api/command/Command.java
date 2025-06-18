package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Interface defining a command in the advanced command framework.
 */
public interface Command {
    
    /**
     * Gets the name of the command.
     *
     * @return The command name
     */
    String getName();
    
    /**
     * Gets the description of the command.
     *
     * @return The command description
     */
    String getDescription();
    
    /**
     * Gets the usage message for the command.
     *
     * @return The usage message
     */
    String getUsage();
    
    /**
     * Gets the aliases for the command.
     *
     * @return List of aliases
     */
    List<String> getAliases();
    
    /**
     * Gets the permission required to use the command.
     *
     * @return The permission string
     */
    String getPermission();
    
    /**
     * Gets the module ID that owns this command.
     *
     * @return The module ID
     */
    String getModuleId();
    
    /**
     * Gets the parent command, if this is a sub-command.
     *
     * @return The parent command, or null if this is a root command
     */
    Command getParent();
    
    /**
     * Gets the sub-commands of this command.
     *
     * @return List of sub-commands
     */
    List<Command> getSubCommands();
    
    /**
     * Adds a sub-command to this command.
     *
     * @param subCommand The sub-command to add
     */
    void addSubCommand(Command subCommand);
    
    /**
     * Removes a sub-command from this command.
     *
     * @param subCommand The sub-command to remove
     * @return true if the sub-command was removed
     */
    boolean removeSubCommand(Command subCommand);
    
    /**
     * Gets a sub-command by name or alias.
     *
     * @param name The name or alias to look up
     * @return The sub-command, or null if not found
     */
    Command getSubCommand(String name);
    
    /**
     * Executes the command.
     *
     * @param context The command context
     * @return true if the command was executed successfully
     */
    boolean execute(CommandContext context);
    
    /**
     * Provides tab completion options for the command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return List of tab completion options
     */
    List<String> tabComplete(CommandSender sender, String[] args);
    
    /**
     * Gets the minimum number of arguments required for this command.
     *
     * @return The minimum number of arguments
     */
    int getMinArgs();
    
    /**
     * Gets the maximum number of arguments allowed for this command.
     *
     * @return The maximum number of arguments, or -1 for unlimited
     */
    int getMaxArgs();
    
    /**
     * Gets the category of this command for help organization.
     *
     * @return The command category
     */
    String getCategory();
    
    /**
     * Gets detailed help information for this command.
     *
     * @return The detailed help text
     */
    String getDetailedHelp();
    
    /**
     * Gets examples of how to use this command.
     *
     * @return List of example usages
     */
    List<String> getExamples();
    
    /**
     * Checks if this command is hidden from help listings.
     *
     * @return true if the command is hidden
     */
    boolean isHidden();
    
    /**
     * Gets the cooldown time for this command in seconds.
     *
     * @return The cooldown time in seconds, or 0 for no cooldown
     */
    int getCooldown();
} 
