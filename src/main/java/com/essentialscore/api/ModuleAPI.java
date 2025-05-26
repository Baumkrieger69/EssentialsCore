package com.essentialscore.api;

import com.essentialscore.api.integration.IntegrationManager;
import com.essentialscore.api.integration.PluginIntegration;
import com.essentialscore.api.integration.bukkit.BukkitIntegration;
import com.essentialscore.api.integration.economy.EconomyIntegration;
import com.essentialscore.api.integration.permissions.PermissionIntegration;
import com.essentialscore.api.integration.placeholders.PlaceholderIntegration;
import com.essentialscore.api.integration.worldguard.WorldGuardIntegration;
import com.essentialscore.api.gui.GUI;
import com.essentialscore.api.gui.GUIBuilder;
import com.essentialscore.api.gui.GUIManager;
import com.essentialscore.api.security.SecurityManager;
import com.essentialscore.api.security.ModuleSandbox;
import com.essentialscore.api.security.SecurityPermission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    default boolean hasPermission(Player player, String permission) {
        return getPermissionIntegration()
            .map(permIntegration -> permIntegration.hasPermission(player, permission))
            .orElseGet(() -> player != null && player.hasPermission(permission));
    }
    
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
    
    /**
     * Gets the integration manager
     * @return The integration manager
     */
    IntegrationManager getIntegrationManager();
    
    /**
     * Gets a plugin integration by class
     * @param integrationClass The integration class
     * @param <T> The integration type
     * @return The integration, or empty if not available
     */
    <T extends PluginIntegration> Optional<T> getIntegration(Class<T> integrationClass);
    
    /**
     * Gets a plugin integration by plugin name
     * @param pluginName The plugin name
     * @return The integration, or empty if not available
     */
    Optional<PluginIntegration> getIntegrationByPlugin(String pluginName);
    
    /**
     * Gets the Bukkit integration
     * @return The Bukkit integration, or empty if not available
     */
    default Optional<BukkitIntegration> getBukkitIntegration() {
        return getIntegration(BukkitIntegration.class);
    }
    
    /**
     * Gets the permission integration
     * @return The permission integration, or empty if not available
     */
    default Optional<PermissionIntegration> getPermissionIntegration() {
        return getIntegration(PermissionIntegration.class);
    }
    
    /**
     * Gets the economy integration
     * @return The economy integration, or empty if not available
     */
    default Optional<EconomyIntegration> getEconomyIntegration() {
        return getIntegration(EconomyIntegration.class);
    }
    
    /**
     * Gets the placeholder integration
     * @return The placeholder integration, or empty if not available
     */
    default Optional<PlaceholderIntegration> getPlaceholderIntegration() {
        return getIntegration(PlaceholderIntegration.class);
    }
    
    /**
     * Gets the WorldGuard integration
     * @return The WorldGuard integration, or empty if not available
     */
    default Optional<WorldGuardIntegration> getWorldGuardIntegration() {
        return getIntegration(WorldGuardIntegration.class);
    }
    
    /**
     * Registers a custom module permission
     * @param module The module
     * @param permissionName The permission name (without module prefix)
     * @param description The permission description
     * @return The full permission node
     */
    String registerModulePermission(Module module, String permissionName, String description);
    
    /**
     * Formats a string with placeholders
     * @param text The text to format
     * @param player The player for player-specific placeholders
     * @return The formatted text
     */
    String formatPlaceholders(String text, Player player);
    
    /**
     * Gets the security manager
     * @return The security manager
     */
    SecurityManager getSecurityManager();
    
    /**
     * Gets the sandbox for a module
     * @param moduleId The module ID
     * @return The module sandbox, or null if not found
     */
    ModuleSandbox getModuleSandbox(String moduleId);
    
    /**
     * Executes a task in a module's sandbox
     * @param moduleId The module ID
     * @param task The task to execute
     * @param <T> The task result type
     * @return The task result
     */
    <T> T executeInSandbox(String moduleId, ModuleSandbox.SandboxedTask<T> task);
    
    /**
     * Checks if a module has a security permission
     * @param moduleId The module ID
     * @param permission The permission
     * @param target The permission target
     * @return true if the module has the permission
     */
    boolean hasModuleSecurityPermission(String moduleId, SecurityPermission permission, String target);
    
    /**
     * Gets the GUI manager.
     * 
     * @return The GUI manager
     */
    GUIManager getGUIManager();
    
    /**
     * Creates a new GUI builder.
     * 
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows (1-6)
     * @return The GUI builder
     */
    GUIBuilder createGUI(String moduleId, String title, int rows);
    
    /**
     * Opens a GUI for a player.
     * 
     * @param player The player
     * @param gui The GUI to open
     */
    void openGUI(Player player, GUI gui);
} 