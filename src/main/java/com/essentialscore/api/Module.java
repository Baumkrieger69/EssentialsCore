package com.essentialscore.api;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Main interface for EssentialsCore modules.
 * All modules should implement this interface to be properly loaded by the core.
 */
public interface Module {
    
    /**
     * Called before the module is initialized, allowing setup of early resources
     * 
     * @param api The module API instance for interacting with the core
     */
    default void onPreLoad(ModuleAPI api) {}
    
    /**
     * Called when the module is initialized
     * 
     * @param api The module API instance for interacting with the core
     * @param config The module's configuration file
     */
    void init(ModuleAPI api, FileConfiguration config);
    
    /**
     * Called after the module is initialized, allowing final setup steps
     */
    default void onPostLoad() {}
    
    /**
     * Called when the module is enabled or re-enabled after a reload
     * This is called after init() and can be used to start tasks or register listeners
     */
    default void onEnable() {}
    
    /**
     * Called when the module is disabled
     */
    void onDisable();
    
    /**
     * Called when the module is being unloaded completely
     * This allows the module to clean up resources before being removed
     */
    default void onUnload() {}
    
    /**
     * Called when the module is being reloaded
     * 
     * @param config The new configuration for the module
     * @return true if reload was successful, false otherwise
     */
    default boolean onReload(FileConfiguration config) {
        return true;
    }
    
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
     * Gets the module ID.
     *
     * @return The module ID
     */
    default String getId() {
        return getName().toLowerCase().replace(" ", "_");
    }
    
    /**
     * Gets the module author.
     *
     * @return The module author
     */
    default String getAuthor() {
        return "Unknown";
    }
    
    /**
     * Gets the list of module dependencies
     * Dependencies are required for the module to function
     * 
     * @return Map of dependency names to version range requirements
     */
    default Map<String, String> getDependencies() { return Map.of(); }
    
    /**
     * Gets the list of optional module dependencies
     * Optional dependencies enhance functionality but are not required
     * 
     * @return Map of dependency names to version range requirements
     */
    default Map<String, String> getOptionalDependencies() { return Map.of(); }
    
    /**
     * Gets the module dependencies as an array of strings
     * @deprecated Use {@link #getDependencies()} instead
     * @return The module dependencies
     */
    @Deprecated
    default String[] getDependenciesArray() {
        return getDependencies().keySet().toArray(new String[0]);
    }
    
    /**
     * Gets the module soft dependencies as an array of strings
     * @deprecated Use {@link #getOptionalDependencies()} instead
     * @return The module soft dependencies
     */
    @Deprecated
    default String[] getSoftDependencies() {
        return getOptionalDependencies().keySet().toArray(new String[0]);
    }
} 
