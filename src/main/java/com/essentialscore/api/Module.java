package com.essentialscore.api;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Interface for plugin modules.
 */
public interface Module {
    
    /**
     * Called when the module is loaded.
     *
     * @param plugin The base plugin
     * @param logger The module logger
     * @param config The module configuration
     */
    void onLoad(BasePlugin plugin, ModuleLogger logger, FileConfiguration config);
    
    /**
     * Called when the module is enabled.
     */
    void onEnable();
    
    /**
     * Called when the module is disabled.
     */
    void onDisable();
    
    /**
     * Called when the module is reloaded.
     */
    void onReload();
    
    /**
     * Gets the module ID.
     *
     * @return The module ID
     */
    String getId();
    
    /**
     * Gets the module name.
     *
     * @return The module name
     */
    String getName();
    
    /**
     * Gets the module version.
     *
     * @return The module version
     */
    String getVersion();
    
    /**
     * Gets the module description.
     *
     * @return The module description
     */
    String getDescription();
    
    /**
     * Gets the module author.
     *
     * @return The module author
     */
    String getAuthor();
    
    /**
     * Gets the module dependencies.
     *
     * @return The module dependencies
     */
    String[] getDependencies();
    
    /**
     * Gets the module soft dependencies.
     *
     * @return The module soft dependencies
     */
    String[] getSoftDependencies();
    
    /**
     * Gets the module's base plugin.
     *
     * @return The base plugin
     */
    BasePlugin getPlugin();
    
    /**
     * Gets the module logger.
     *
     * @return The module logger
     */
    ModuleLogger getLogger();
    
    /**
     * Gets the module configuration.
     *
     * @return The module configuration
     */
    FileConfiguration getConfig();
    
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
     * Gets the list of module dependencies
     * Dependencies are required for the module to function
     * 
     * @return Map of dependency names to version range requirements
     */
    default Map<String, String> getDependenciesMap() { return Map.of(); }
    
    /**
     * Gets the list of optional module dependencies
     * Optional dependencies enhance functionality but are not required
     * 
     * @return Map of dependency names to version range requirements
     */
    default Map<String, String> getOptionalDependencies() { return Map.of(); }
} 