package com.essentialscore.api;

import com.essentialscore.api.command.CommandManager;
import com.essentialscore.api.command.DynamicCommand;
import com.essentialscore.api.module.ModuleFileManager;
import com.essentialscore.api.module.ModuleManager;
import com.essentialscore.api.module.ModuleManager.ModuleInfo;
import com.essentialscore.api.module.ModuleSandbox;
import com.essentialscore.api.permission.PermissionManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Base interface for the core plugin that provides essential functionality
 * to modules.
 */
public interface BasePlugin {
    
    /**
     * Gets the message prefix used for plugin messages.
     *
     * @return The message prefix
     */
    String getMessagePrefix();
    
    /**
     * Checks if debug mode is enabled.
     *
     * @return True if debug mode is enabled
     */
    boolean isDebugMode();
    
    /**
     * Gets all loaded modules.
     *
     * @return A map of module names to module information
     */
    Map<String, ModuleInfo> getLoadedModules();
    
    /**
     * Gets information about a specific module.
     *
     * @param moduleName The module name
     * @return The module information, or null if not found
     */
    ModuleInfo getModuleInfo(String moduleName);
    
    /**
     * Checks if a player has a permission.
     *
     * @param player The player
     * @param permission The permission to check
     * @return True if the player has the permission
     */
    boolean hasPermission(Player player, String permission);
    
    /**
     * Gets the data folder for a module.
     *
     * @param moduleName The module name
     * @return The module's data folder
     */
    File getModuleDataFolder(String moduleName);
    
    /**
     * Gets the configuration file for a module.
     *
     * @param moduleName The module name
     * @return The module's configuration file
     */
    File getModuleConfigFile(String moduleName);
    
    /**
     * Gets the resources folder for a module.
     *
     * @param moduleName The module name
     * @return The module's resources folder
     */
    File getModuleResourcesFolder(String moduleName);
    
    /**
     * Initializes a module.
     *
     * @param moduleName The module name
     * @param moduleInstance The module instance
     * @param config The module configuration
     */
    void initializeModule(String moduleName, Object moduleInstance, FileConfiguration config);
    
    /**
     * Registers dynamic commands.
     *
     * @param commands The commands to register
     */
    void registerCommands(List<DynamicCommand> commands);
    
    /**
     * Unregisters dynamic commands.
     *
     * @param commands The commands to unregister
     */
    void unregisterCommands(List<DynamicCommand> commands);
    
    /**
     * Disables a module.
     *
     * @param moduleName The module name
     * @return True if the module was disabled successfully
     */
    boolean disableModule(String moduleName);
    
    /**
     * Gets the permission manager.
     *
     * @return The permission manager
     */
    PermissionManager getPermissionManager();
    
    /**
     * Gets the module manager.
     *
     * @return The module manager
     */
    ModuleManager getModuleManager();
    
    /**
     * Gets the module file manager.
     *
     * @return The module file manager
     */
    ModuleFileManager getModuleFileManager();
    
    /**
     * Gets the module sandbox.
     *
     * @return The module sandbox
     */
    ModuleSandbox getModuleSandbox();
    
    /**
     * Gets the module API for a specific module.
     *
     * @param moduleName The module name
     * @return The module API, or null if not found
     */
    ModuleAPI getModuleAPI(String moduleName);
    
    /**
     * Gets the command manager.
     *
     * @return The command manager
     */
    CommandManager getCommandManager();
} 