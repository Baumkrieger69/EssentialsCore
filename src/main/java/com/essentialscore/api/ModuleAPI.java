package com.essentialscore.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main interface that modules use to interact with the EssentialsCore system.
 * This interface abstracts the core functionality to make modules less dependent
 * on the actual implementation.
 */
public interface ModuleAPI {
    
    /**
     * Gets the plugin instance
     * @return The plugin instance
     */
    Plugin getPlugin();
    
    /**
     * Gets the module's data folder
     * @param moduleName The name of the module
     * @return The module's data folder
     */
    File getModuleDataFolder(String moduleName);
    
    /**
     * Gets the module's config file
     * @param moduleName The name of the module
     * @return The module's config file
     */
    File getModuleConfigFile(String moduleName);
    
    /**
     * Gets the module's resources folder
     * @param moduleName The name of the module
     * @return The module's resources folder
     */
    File getModuleResourcesFolder(String moduleName);
    
    /**
     * Registers a module event listener
     * @param eventName The name of the event
     * @param listener The event listener
     */
    void registerModuleListener(String eventName, ModuleEventListener listener);
    
    /**
     * Unregisters a module event listener
     * @param eventName The name of the event
     * @param listener The event listener to remove
     */
    void unregisterModuleListener(String eventName, ModuleEventListener listener);
    
    /**
     * Fires a module event
     * @param eventName The name of the event
     * @param data The event data
     */
    void fireModuleEvent(String eventName, Map<String, Object> data);
    
    /**
     * Registers commands for a module
     * @param commands The commands to register
     */
    void registerCommands(List<? extends CommandDefinition> commands);
    
    /**
     * Unregisters commands for a module
     * @param commands The commands to unregister
     */
    void unregisterCommands(List<? extends CommandDefinition> commands);
    
    /**
     * Checks if a player has a permission
     * @param player The player
     * @param permission The permission to check
     * @return true if the player has the permission
     */
    boolean hasPermission(Player player, String permission);
    
    /**
     * Sets a shared data value
     * @param key The key
     * @param value The value to store
     */
    void setSharedData(String key, Object value);
    
    /**
     * Gets a shared data value
     * @param key The key
     * @return The stored value or null if not found
     */
    Object getSharedData(String key);
    
    /**
     * Formats a message with hex colors
     * @param message The message to format
     * @return The formatted message
     */
    String formatHex(String message);
    
    /**
     * Runs a task asynchronously
     * @param task The task to run
     * @return A CompletableFuture for the task result
     */
    <T> CompletableFuture<T> runAsync(java.util.function.Supplier<T> task);
    
    /**
     * Runs a task asynchronously
     * @param task The task to run
     */
    void runAsync(Runnable task);
    
    /**
     * Schedules a task to run after a delay
     * @param task The task to run
     * @param delayTicks The delay in ticks
     * @return The task ID
     */
    int scheduleTask(Runnable task, long delayTicks);
    
    /**
     * Schedules a repeating task
     * @param task The task to run
     * @param delayTicks The initial delay in ticks
     * @param periodTicks The period in ticks
     * @return The task ID
     */
    int scheduleRepeatingTask(Runnable task, long delayTicks, long periodTicks);
    
    /**
     * Cancels a scheduled task
     * @param taskId The task ID
     */
    void cancelTask(int taskId);
    
    /**
     * Logs an info message
     * @param message The message to log
     */
    void logInfo(String message);
    
    /**
     * Logs a warning message
     * @param message The message to log
     */
    void logWarning(String message);
    
    /**
     * Logs an error message
     * @param message The message to log
     * @param throwable The exception, if any
     */
    void logError(String message, Throwable throwable);
    
    /**
     * Logs a debug message
     * @param message The message to log
     */
    void logDebug(String message);
    
    /**
     * Checks if debug mode is enabled
     * @return true if debug mode is enabled
     */
    boolean isDebugMode();
} 