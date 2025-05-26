package com.essentialscore.lifecycle;

import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.ConsoleFormatter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the lifecycle states of modules and ensures safe state transitions.
 */
public class ModuleStateManager {
    private final Map<String, ModuleState> moduleStates;
    private final Map<String, ModuleState> previousStates;
    private final Map<String, Throwable> moduleErrors;
    private final Map<String, Module> moduleInstances;
    private final Map<String, FileConfiguration> moduleConfigs;
    private final Map<String, Lock> moduleLocks;
    private final ModuleAPI api;
    private final File configDir;
    private final ConsoleFormatter console;
    private final Logger logger;
    
    /**
     * Creates a new module state manager.
     *
     * @param api The module API
     * @param configDir The directory containing module configurations
     * @param logger The logger
     */
    public ModuleStateManager(ModuleAPI api, File configDir, Logger logger) {
        this.moduleStates = new ConcurrentHashMap<>();
        this.previousStates = new ConcurrentHashMap<>();
        this.moduleErrors = new ConcurrentHashMap<>();
        this.moduleInstances = new ConcurrentHashMap<>();
        this.moduleConfigs = new ConcurrentHashMap<>();
        this.moduleLocks = new ConcurrentHashMap<>();
        this.api = api;
        this.configDir = configDir;
        this.logger = logger;
        
        // Create formatter for nice console output
        String rawPrefix = "&8[&6&lStateManager&8]";
        this.console = new ConsoleFormatter(
            logger,
            rawPrefix,
            true, false, true, "default"
        );
    }
    
    /**
     * Registers a module with the state manager.
     *
     * @param moduleName The name of the module
     * @param module The module instance
     * @return true if the module was registered, false if it was already registered
     */
    public boolean registerModule(String moduleName, Module module) {
        if (moduleInstances.containsKey(moduleName)) {
            return false;
        }
        
        moduleInstances.put(moduleName, module);
        moduleStates.put(moduleName, ModuleState.DISCOVERED);
        moduleLocks.put(moduleName, new ReentrantLock());
        
        return true;
    }
    
    /**
     * Gets the current state of a module.
     *
     * @param moduleName The name of the module
     * @return The current state, or null if the module is not registered
     */
    public ModuleState getModuleState(String moduleName) {
        return moduleStates.get(moduleName);
    }
    
    /**
     * Gets the previous state of a module.
     *
     * @param moduleName The name of the module
     * @return The previous state, or null if the module has no previous state
     */
    public ModuleState getPreviousState(String moduleName) {
        return previousStates.get(moduleName);
    }
    
    /**
     * Gets the module instance.
     *
     * @param moduleName The name of the module
     * @return The module instance, or null if not found
     */
    public Module getModuleInstance(String moduleName) {
        return moduleInstances.get(moduleName);
    }
    
    /**
     * Gets all registered module instances.
     *
     * @return Map of module names to instances
     */
    public Map<String, Module> getAllModuleInstances() {
        return Collections.unmodifiableMap(moduleInstances);
    }
    
    /**
     * Gets the module configuration.
     *
     * @param moduleName The name of the module
     * @return The module configuration, or null if not found
     */
    public FileConfiguration getModuleConfig(String moduleName) {
        return moduleConfigs.get(moduleName);
    }
    
    /**
     * Gets any error that occurred during module lifecycle.
     *
     * @param moduleName The name of the module
     * @return The error, or null if no error occurred
     */
    public Throwable getModuleError(String moduleName) {
        return moduleErrors.get(moduleName);
    }
    
    /**
     * Transitions a module to the next state in its lifecycle.
     * This method handles all the necessary hooks and validations.
     *
     * @param moduleName The name of the module
     * @return true if the transition was successful, false otherwise
     */
    public boolean transitionToNextState(String moduleName) {
        Lock lock = moduleLocks.get(moduleName);
        if (lock == null) {
            console.error("No lock found for module: " + moduleName);
            return false;
        }
        
        lock.lock();
        try {
            Module module = moduleInstances.get(moduleName);
            if (module == null) {
                console.error("No module instance found for: " + moduleName);
                return false;
            }
            
            ModuleState currentState = moduleStates.get(moduleName);
            if (currentState == null) {
                console.error("No state found for module: " + moduleName);
                return false;
            }
            
            ModuleState nextState = currentState.getNextState();
            if (nextState == null) {
                console.warning("Module " + moduleName + " is in a terminal state: " + currentState);
                return false;
            }
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Transitioning module " + moduleName + " from " + currentState + " to " + nextState);
            }
            
            try {
                switch (nextState) {
                    case PRE_LOADING:
                        module.onPreLoad(api);
                        break;
                        
                    case LOADING:
                        // Load config
                        FileConfiguration config = loadModuleConfig(moduleName);
                        moduleConfigs.put(moduleName, config);
                        module.init(api, config);
                        break;
                        
                    case POST_LOADING:
                        module.onPostLoad();
                        break;
                        
                    case ENABLING:
                        module.onEnable();
                        break;
                        
                    case DISABLING:
                        module.onDisable();
                        break;
                        
                    case UNLOADING:
                        module.onUnload();
                        moduleInstances.remove(moduleName);
                        moduleConfigs.remove(moduleName);
                        break;
                }
                
                // Update state
                previousStates.put(moduleName, currentState);
                moduleStates.put(moduleName, nextState);
                
                return true;
            } catch (Throwable t) {
                                 console.error("Error transitioning module " + moduleName + " to state " + nextState);
                 logger.log(Level.SEVERE, "Module transition error", t);
                 moduleErrors.put(moduleName, t);
                 moduleStates.put(moduleName, ModuleState.ERROR);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Transitions a module to a specific state, calling all intermediate state transitions.
     *
     * @param moduleName The name of the module
     * @param targetState The target state
     * @return true if the transition was successful, false otherwise
     */
    public boolean transitionToState(String moduleName, ModuleState targetState) {
        ModuleState currentState = moduleStates.get(moduleName);
        if (currentState == null) {
            console.error("No state found for module: " + moduleName);
            return false;
        }
        
        // If already in target state, return success
        if (currentState == targetState) {
            return true;
        }
        
        // If current state is terminal and not the target, can't transition
        if (currentState.isTerminal() && currentState != targetState) {
            console.warning("Cannot transition module " + moduleName + " from terminal state " + currentState);
            return false;
        }
        
        // For certain target states, we have special transition logic
        switch (targetState) {
            case DISABLED:
                if (currentState.isActive()) {
                    return transitionToNextState(moduleName); // Transition to DISABLING
                }
                break;
                
            case UNLOADED:
                if (currentState.isLoaded()) {
                    // First disable if necessary
                    if (currentState.isActive()) {
                        if (!transitionToState(moduleName, ModuleState.DISABLED)) {
                            return false;
                        }
                    }
                    
                    // Then transition to UNLOADING
                    moduleStates.put(moduleName, ModuleState.UNLOADING);
                    return transitionToNextState(moduleName);
                }
                break;
                
            case ENABLED:
                // Need to go through all loading states first
                if (currentState == ModuleState.DISCOVERED) {
                    while (currentState != ModuleState.ENABLED) {
                        if (!transitionToNextState(moduleName)) {
                            return false;
                        }
                        currentState = moduleStates.get(moduleName);
                        
                        // Check for error or early termination
                        if (currentState == ModuleState.ERROR || currentState.isTerminal()) {
                            return false;
                        }
                    }
                    return true;
                } else if (currentState == ModuleState.DISABLED) {
                    // Re-enable a disabled module
                    moduleStates.put(moduleName, ModuleState.ENABLING);
                    return transitionToNextState(moduleName);
                }
                break;
        }
        
        // For other cases, just move one state at a time until we reach the target
        while (currentState != targetState) {
            if (!transitionToNextState(moduleName)) {
                return false;
            }
            currentState = moduleStates.get(moduleName);
            
            // Check for error or early termination
            if (currentState == ModuleState.ERROR || 
                (currentState.isTerminal() && currentState != targetState)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Reloads a module, applying new configuration if available.
     *
     * @param moduleName The name of the module
     * @return true if the reload was successful, false otherwise
     */
    public boolean reloadModule(String moduleName) {
        Lock lock = moduleLocks.get(moduleName);
        if (lock == null) {
            console.error("No lock found for module: " + moduleName);
            return false;
        }
        
        lock.lock();
        try {
            Module module = moduleInstances.get(moduleName);
            if (module == null) {
                console.error("No module instance found for: " + moduleName);
                return false;
            }
            
            ModuleState currentState = moduleStates.get(moduleName);
            if (currentState == null || !currentState.isLoaded()) {
                console.error("Module " + moduleName + " is not in a loadable state: " + currentState);
                return false;
            }
            
            // Set state to reloading
            previousStates.put(moduleName, currentState);
            moduleStates.put(moduleName, ModuleState.RELOADING);
            
            try {
                // Reload configuration
                FileConfiguration config = loadModuleConfig(moduleName);
                moduleConfigs.put(moduleName, config);
                
                // Call reload hook
                boolean success = module.onReload(config);
                
                if (success) {
                    // Set state back to what it was
                    moduleStates.put(moduleName, currentState);
                    console.info("Successfully reloaded module: " + moduleName);
                } else {
                    moduleStates.put(moduleName, ModuleState.ERROR);
                    console.error("Module " + moduleName + " reported reload failure");
                }
                
                return success;
            } catch (Throwable t) {
                                 console.error("Error reloading module " + moduleName);
                 logger.log(Level.SEVERE, "Module reload error", t);
                 moduleErrors.put(moduleName, t);
                 moduleStates.put(moduleName, ModuleState.ERROR);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Loads the configuration for a module.
     *
     * @param moduleName The name of the module
     * @return The configuration
     */
    private FileConfiguration loadModuleConfig(String moduleName) {
        File configFile = new File(configDir, moduleName + ".yml");
        
        if (!configFile.exists()) {
            try {
                // Create default config
                YamlConfiguration config = new YamlConfiguration();
                config.set("enabled", true);
                config.set("version", moduleInstances.get(moduleName).getVersion());
                config.save(configFile);
                console.info("Created default configuration for module: " + moduleName);
                return config;
            } catch (IOException e) {
                                 console.error("Failed to create default configuration for module: " + moduleName);
                 logger.log(Level.SEVERE, "Configuration file creation error", e);
                 return new YamlConfiguration();
            }
        }
        
        return YamlConfiguration.loadConfiguration(configFile);
    }
    
    /**
     * Performs a rollback for a module, returning it to its previous state.
     *
     * @param moduleName The name of the module
     * @return true if the rollback was successful, false otherwise
     */
    public boolean rollbackModule(String moduleName) {
        Lock lock = moduleLocks.get(moduleName);
        if (lock == null) {
            console.error("No lock found for module: " + moduleName);
            return false;
        }
        
        lock.lock();
        try {
            ModuleState previousState = previousStates.get(moduleName);
            if (previousState == null) {
                console.warning("No previous state found for module: " + moduleName);
                return false;
            }
            
            console.info("Rolling back module " + moduleName + " to state: " + previousState);
            moduleStates.put(moduleName, previousState);
            
            // Clear error if there was one
            moduleErrors.remove(moduleName);
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets a snapshot of all module states.
     *
     * @return Map of module names to states
     */
    public Map<String, ModuleState> getModuleStates() {
        return Collections.unmodifiableMap(new HashMap<>(moduleStates));
    }
} 