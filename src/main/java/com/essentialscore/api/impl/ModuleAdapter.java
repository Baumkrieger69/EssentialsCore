package com.essentialscore.api.impl;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.essentialscore.ApiCore;
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.lifecycle.ModuleStateManager;
import com.essentialscore.lifecycle.ModuleState;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adapter class that wraps a module to provide compatibility with different API versions.
 * This class serves as a bridge between the core system and modules, ensuring that 
 * modules can continue to function even as the API evolves.
 */
public class ModuleAdapter {
    private final Module module;
    private final ModuleAPI moduleAPI;
    private final ModuleStateManager stateManager;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    /**
     * Creates a new module adapter
     */
    public ModuleAdapter(Module module, ModuleAPI moduleAPI, ApiCore apiCore) {
        if (module == null) throw new IllegalArgumentException("Module cannot be null");
        if (moduleAPI == null) throw new IllegalArgumentException("ModuleAPI cannot be null");
        if (apiCore == null) throw new IllegalArgumentException("ApiCore cannot be null");
        
        this.module = module;
        this.moduleAPI = moduleAPI;
        this.stateManager = (ModuleStateManager) apiCore.getModuleStateManager();
    }

    /**
     * Initializes the module safely
     */
    public void initialize(FileConfiguration config) {
        String moduleName = moduleAPI.getModuleName();
        if (!initialized.compareAndSet(false, true)) {
            moduleAPI.logWarning("Module already initialized: " + moduleName);
            return;
        }

        try {
            stateManager.transitionToState(moduleName, ModuleState.LOADING);
            
            // Pre-initialization phase
            module.onPreLoad(moduleAPI);
            stateManager.transitionToState(moduleName, ModuleState.PRE_INITIALIZED);
            
            // Main initialization
            stateManager.transitionToState(moduleName, ModuleState.INITIALIZING);
            module.init(moduleAPI, config);
            stateManager.transitionToState(moduleName, ModuleState.INITIALIZED);
            
            // Post-initialization phase
            module.onPostLoad();
            
        } catch (Exception e) {
            moduleAPI.logError("Failed to initialize module: " + e.getMessage(), e);
            e.printStackTrace();
            stateManager.transitionToState(moduleName, ModuleState.ERROR);
            throw new ModuleInitializationException("Failed to initialize module: " + moduleName, e);
        }
    }

    /**
     * Enables the module safely
     */
    public void enable() {
        String moduleName = moduleAPI.getModuleName();
        if (!initialized.get()) {
            throw new IllegalStateException("Module must be initialized before enabling: " + moduleName);
        }
        
        if (!enabled.compareAndSet(false, true)) {
            moduleAPI.logWarning("Module already enabled: " + moduleName);
            return;
        }

        try {
            stateManager.transitionToState(moduleName, ModuleState.ENABLING);
            module.onEnable();
            stateManager.transitionToState(moduleName, ModuleState.ENABLED);
            
        } catch (Exception e) {
            moduleAPI.logError("Failed to enable module: " + e.getMessage(), e);
            e.printStackTrace();
            stateManager.transitionToState(moduleName, ModuleState.ERROR);
            enabled.set(false);
            throw new ModuleEnableException("Failed to enable module: " + moduleName, e);
        }
    }

    /**
     * Disables the module safely
     */
    public void disable() {
        String moduleName = moduleAPI.getModuleName();
        if (!enabled.compareAndSet(true, false)) {
            return; // Already disabled
        }

        try {
            stateManager.transitionToState(moduleName, ModuleState.DISABLING);
            module.onDisable();
            stateManager.transitionToState(moduleName, ModuleState.DISABLED);
            
        } catch (Exception e) {
            moduleAPI.logError("Failed to disable module: " + e.getMessage(), e);
            e.printStackTrace();
            stateManager.transitionToState(moduleName, ModuleState.ERROR);
            throw new ModuleDisableException("Failed to disable module: " + moduleName, e);
        }
    }

    /**
     * Reloads the module safely
     */
    public void reload() {
        String moduleName = moduleAPI.getModuleName();
        boolean wasEnabled = enabled.get();
        
        try {
            stateManager.transitionToState(moduleName, ModuleState.RELOADING);
            
            // Disable if necessary
            if (wasEnabled) {
                disable();
            }
            
            // Reload configuration
            moduleAPI.reloadConfig();
            
            // Re-initialize
            module.init(moduleAPI, moduleAPI.getConfig());
            
            // Re-enable if it was enabled before
            if (wasEnabled) {
                enable();
            }
            
            stateManager.transitionToState(moduleName, 
                wasEnabled ? ModuleState.ENABLED : ModuleState.DISABLED);
            
        } catch (Exception e) {
            moduleAPI.logError("Failed to reload module: " + e.getMessage(), e);
            e.printStackTrace();
            stateManager.transitionToState(moduleName, ModuleState.ERROR);
            throw new ModuleReloadException("Failed to reload module: " + moduleName, e);
        }
    }

    /**
     * Unloads the module completely
     */
    public void unload() {
        String moduleName = moduleAPI.getModuleName();
        
        try {
            // Disable first if necessary
            if (enabled.get()) {
                disable();
            }
            
            // Call unload hook
            module.onUnload();
            
            // Clean up resources
            initialized.set(false);
            stateManager.transitionToState(moduleName, ModuleState.UNLOADED);
            
        } catch (Exception e) {
            moduleAPI.logError("Failed to unload module: " + e.getMessage(), e);
            e.printStackTrace();
            stateManager.transitionToState(moduleName, ModuleState.ERROR);
            throw new ModuleUnloadException("Failed to unload module: " + moduleName, e);
        }
    }

    /**
     * Called when a player joins the server
     * 
     * @param player The player who joined
     */
    public void onPlayerJoin(Player player) {
        module.onPlayerJoin(player);
    }
    
    /**
     * Called when a command registered by this module is executed
     * 
     * @param commandName The name of the command
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        return module.onCommand(commandName, sender, args);
    }
    
    /**
     * Called when tab completion is requested for a command registered by this module
     * 
     * @param commandName The name of the command
     * @param sender The command sender
     * @param args The command arguments
     * @return A list of tab completion options, or null for default behavior
     */
    public List<String> onTabComplete(String commandName, CommandSender sender, String[] args) {
        return module.onTabComplete(commandName, sender, args);
    }
    
    /**
     * Gets the module instance
     */
    public Module getModule() {
        return module;
    }

    /**
     * Gets the module API instance
     */
    public ModuleAPI getModuleAPI() {
        return moduleAPI;
    }

    /**
     * Checks if the module is initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Checks if the module is enabled
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Gets the current state of the module
     */
    public ModuleState getState() {
        // For now, determine state based on initialized and enabled flags
        if (!initialized.get()) {
            return ModuleState.UNLOADED;
        } else if (!enabled.get()) {
            return ModuleState.DISABLED;
        } else {
            return ModuleState.ENABLED;
        }
    }

    /**
     * Custom exceptions for module lifecycle events
     */
    public static class ModuleInitializationException extends RuntimeException {
        public ModuleInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ModuleEnableException extends RuntimeException {
        public ModuleEnableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ModuleDisableException extends RuntimeException {
        public ModuleDisableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ModuleReloadException extends RuntimeException {
        public ModuleReloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ModuleUnloadException extends RuntimeException {
        public ModuleUnloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
