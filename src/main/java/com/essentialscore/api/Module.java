package com.essentialscore.api;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Main interface for EssentialsCore modules.
 * All modules should implement this interface to be properly loaded by the core.
 */
public interface Module {
    /**
     * Called when the module is initialized
     * 
     * @param api The module API instance for interacting with the core
     * @param config The module's configuration file
     */
    void init(ModuleAPI api, FileConfiguration config);
    
    /**
     * Called when the module is disabled
     */
    void onDisable();
    
    /**
     * Called when a player joins the server
     * 
     * @param player The player who joined
     */
    default void onPlayerJoin(Player player) {}
    
    /**
     * Called when a command registered by this module is executed
     * 
     * @param commandName The name of the command
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    default boolean onCommand(String commandName, CommandSender sender, String[] args) { return true; }
    
    /**
     * Called when tab completion is requested for a command registered by this module
     * 
     * @param commandName The name of the command
     * @param sender The command sender
     * @param args The command arguments
     * @return A list of tab completion options, or null for default behavior
     */
    default List<String> onTabComplete(String commandName, CommandSender sender, String[] args) { return null; }
    
    /**
     * Gets the name of the module
     * 
     * @return The module name
     */
    String getName();
    
    /**
     * Gets the version of the module
     * 
     * @return The module version
     */
    String getVersion();
    
    /**
     * Gets the description of the module
     * 
     * @return The module description
     */
    String getDescription();
    
    /**
     * Gets the list of module dependencies
     * 
     * @return List of dependencies
     */
    default List<String> getDependencies() { return List.of(); }
} 