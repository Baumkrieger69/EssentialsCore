package com.essentialscore.lifecycle;

import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.ConsoleFormatter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

/**
 * Verwaltet die Zustände aller Module und stellt Thread-Safety sowie korrekte
 * Zustandsübergänge sicher.
 */
public class ModuleStateManager {
    private final ModuleAPI api;
    private final File configDir;
    private final ConsoleFormatter console;
    private final Logger logger;
    
    private final Map<String, ModuleState> moduleStates;
    private final Map<String, Long> stateTransitionTimes;
    private final Map<String, List<ModuleState>> stateHistory;
    private final ReentrantReadWriteLock stateLock;
    private final ConcurrentLinkedQueue<StateTransitionEvent> transitionQueue;
    private final ScheduledExecutorService stateMonitor;
    private final Map<String, Module> moduleInstances;
    private final Map<String, FileConfiguration> moduleConfigs;
    private final Map<String, Lock> moduleLocks;
    private static final int MAX_HISTORY_SIZE = 10;
    private static final long STATE_TRANSITION_TIMEOUT = 30000; // 30 Sekunden
    private static final Map<ModuleState, Set<ModuleState>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(ModuleState.class);
        
        // Definiere erlaubte Zustandsübergänge
        VALID_TRANSITIONS.put(ModuleState.UNLOADED, 
            EnumSet.of(ModuleState.LOADING));
            
        VALID_TRANSITIONS.put(ModuleState.LOADING, 
            EnumSet.of(ModuleState.PRE_INITIALIZED, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.PRE_INITIALIZED, 
            EnumSet.of(ModuleState.INITIALIZING, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.INITIALIZING, 
            EnumSet.of(ModuleState.INITIALIZED, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.INITIALIZED, 
            EnumSet.of(ModuleState.ENABLING, ModuleState.DISABLING, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.ENABLING, 
            EnumSet.of(ModuleState.ENABLED, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.ENABLED, 
            EnumSet.of(ModuleState.DISABLING, ModuleState.RELOADING, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.DISABLING, 
            EnumSet.of(ModuleState.DISABLED, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.DISABLED, 
            EnumSet.of(ModuleState.ENABLING, ModuleState.RELOADING, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.RELOADING, 
            EnumSet.of(ModuleState.INITIALIZING, ModuleState.ERROR));
            
        VALID_TRANSITIONS.put(ModuleState.ERROR, 
            EnumSet.of(ModuleState.UNLOADED, ModuleState.RELOADING));
    }

    /**
     * Creates a new module state manager.
     *
     * @param api The module API
     * @param configDir The directory containing module configurations
     * @param logger The logger
     */
    public ModuleStateManager(ModuleAPI api, File configDir, Logger logger) {
        this.api = api;
        this.configDir = configDir;
        this.logger = logger;
        
        this.moduleStates = new ConcurrentHashMap<>();
        this.stateTransitionTimes = new ConcurrentHashMap<>();
        this.stateHistory = new ConcurrentHashMap<>();
        this.stateLock = new ReentrantReadWriteLock();
        this.transitionQueue = new ConcurrentLinkedQueue<>();
        this.moduleInstances = new ConcurrentHashMap<>();
        this.moduleConfigs = new ConcurrentHashMap<>();
        this.moduleLocks = new ConcurrentHashMap<>();
        
        // Create formatter for nice console output
        String rawPrefix = "&8[&6&lStateManager&8]";
        this.console = new ConsoleFormatter(
            logger,
            rawPrefix,
            true, false, true, "default"
        );
        
        this.stateMonitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ModuleStateMonitor");
            t.setDaemon(true);
            return t;
        });

        // Startet den Zustandsüberwachungsdienst
        stateMonitor.scheduleAtFixedRate(this::monitorStates, 1, 1, TimeUnit.SECONDS);
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
        stateLock.readLock().lock();
        try {
            return moduleStates.getOrDefault(moduleName, null);
        } finally {
            stateLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the previous state of a module.
     *
     * @param moduleName The name of the module
     * @return The previous state, or null if the module has no previous state
     */
    public ModuleState getPreviousState(String moduleName) {
        return stateHistory.get(moduleName).size() > 1 ? stateHistory.get(moduleName).get(stateHistory.get(moduleName).size() - 2) : null;
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
        return null; // Fehlerbehandlung wurde in den Zustandsübergängen integriert
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
                    case LOADING:
                        // Load config
                        FileConfiguration config = loadModuleConfig(moduleName);
                        moduleConfigs.put(moduleName, config);
                        module.init(api, config);
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
                        
                    case DISCOVERED:
                    case PRE_INITIALIZED:
                    case INITIALIZING:
                    case INITIALIZED:
                    case ENABLED:
                    case DISABLED:
                    case UNLOADED:
                    case RELOADING:
                    case ERROR:
                        // These states don't require special actions during transition
                        break;
                        
                    default:
                        // Handle any future enum constants
                        break;
                }
                
                // Update state
                moduleStates.put(moduleName, nextState);
                stateTransitionTimes.put(moduleName, System.currentTimeMillis());
                logStateChange(moduleName, currentState, nextState);

                // Benachrichtigt Listener über den Zustandsübergang
                transitionQueue.offer(new StateTransitionEvent(
                    moduleName, currentState, nextState, System.currentTimeMillis()
                ));
                
                return true;
            } catch (Throwable t) {
                 console.error("Error transitioning module " + moduleName + " to state " + nextState);
                 logger.log(Level.SEVERE, "Module transition error", t);
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
            case DISCOVERED:
                if (currentState == ModuleState.UNLOADED) {
                    return safeStateTransition(moduleName, ModuleState.DISCOVERED, () -> {});
                }
                break;

            case DISABLED:
                if (currentState.isActive()) {
                    return safeStateTransition(moduleName, ModuleState.DISABLING, () -> {
                        Module module = moduleInstances.get(moduleName);
                        if (module != null) {
                            module.onDisable();
                        }
                    });
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
                    
                    return safeStateTransition(moduleName, ModuleState.UNLOADING, () -> {
                        Module module = moduleInstances.get(moduleName);
                        if (module != null) {
                            module.onUnload();
                        }
                    });
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
                    return safeStateTransition(moduleName, ModuleState.ENABLING, () -> {
                        Module module = moduleInstances.get(moduleName);
                        if (module != null) {
                            module.onEnable();
                        }
                    });
                }
                break;
                
            case UNLOADING:
                if (currentState == ModuleState.DISABLED) {
                    return safeStateTransition(moduleName, ModuleState.UNLOADING, () -> {
                        Module module = moduleInstances.get(moduleName);
                        if (module != null) {
                            module.onUnload();
                        }
                    });
                }
                break;
                
            case ERROR:
                // Any state can transition to ERROR
                moduleStates.put(moduleName, ModuleState.ERROR);
                logStateChange(moduleName, currentState, ModuleState.ERROR);
                return true;
                
            case LOADING:
                if (currentState == ModuleState.DISCOVERED || currentState == ModuleState.UNLOADED) {
                    return transitionToNextState(moduleName);
                }
                break;
                
            case INITIALIZING:
                if (currentState == ModuleState.PRE_INITIALIZED) {
                    return transitionToNextState(moduleName);
                }
                break;
                
            case PRE_INITIALIZED:
                if (currentState == ModuleState.LOADING) {
                    return transitionToNextState(moduleName);
                }
                break;
                
            case INITIALIZED:
                if (currentState == ModuleState.INITIALIZING) {
                    return transitionToNextState(moduleName);
                }
                break;
                
            case ENABLING:
                if (currentState == ModuleState.INITIALIZED || currentState == ModuleState.DISABLED) {
                    return transitionToNextState(moduleName);
                }
                break;
                
            case DISABLING:
                if (currentState == ModuleState.ENABLED) {
                    return transitionToNextState(moduleName);
                }
                break;
                
            case RELOADING:
                if (currentState == ModuleState.ENABLED || currentState == ModuleState.ERROR) {
                    moduleStates.put(moduleName, ModuleState.RELOADING);
                    return reloadModule(moduleName);
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
            ModuleState previousState = getPreviousState(moduleName);
            if (previousState == null) {
                console.warning("No previous state found for module: " + moduleName);
                return false;
            }
            
            console.info("Rolling back module " + moduleName + " to state: " + previousState);
            moduleStates.put(moduleName, previousState);
            
            // Clear error if there was one
            moduleStates.put(moduleName, ModuleState.ERROR);
            
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
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(moduleStates));
    }

    /**
     * Event-Klasse für Zustandsübergänge.
     */
    public static class StateTransitionEvent {
        private final String moduleName;
        private final ModuleState oldState;
        private final ModuleState newState;
        private final long timestamp;

        public StateTransitionEvent(String moduleName, ModuleState oldState, 
                                  ModuleState newState, long timestamp) {
            this.moduleName = moduleName;
            this.oldState = oldState;
            this.newState = newState;
            this.timestamp = timestamp;
        }

        public String getModuleName() { return moduleName; }
        public ModuleState getOldState() { return oldState; }
        public ModuleState getNewState() { return newState; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Exception class for module state transitions.
     */
    public static class ModuleStateException extends RuntimeException {
        private final String moduleName;
        private final ModuleState fromState;
        private final ModuleState toState;

        public ModuleStateException(String message, String moduleName, 
                                  ModuleState fromState, ModuleState toState, Throwable cause) {
            super(message, cause);
            this.moduleName = moduleName;
            this.fromState = fromState;
            this.toState = toState;
        }

        public String getModuleName() { return moduleName; }
        public ModuleState getFromState() { return fromState; }
        public ModuleState getToState() { return toState; }
    }

    /**
     * Validiert einen Zustandsübergang.
     */
    private void validateStateTransition(String moduleName, ModuleState currentState, ModuleState targetState) {
        if (currentState == null) {
            throw new ModuleStateException(
                "Module has no current state",
                moduleName, null, targetState, null
            );
        }
        
        if (targetState == null) {
            throw new ModuleStateException(
                "Target state cannot be null",
                moduleName, currentState, null, null
            );
        }
        
        if (!currentState.canTransitionTo(targetState)) {
            throw new ModuleStateException(
                String.format("Invalid state transition from %s to %s", 
                            currentState, targetState),
                moduleName, currentState, targetState, null
            );
        }
    }

    /**
     * Behandelt einen fehlgeschlagenen Zustandsübergang.
     */
    private void handleTransitionFailure(String moduleName, ModuleState fromState, 
                                       ModuleState toState, Throwable error) {
        console.error(String.format(
            "Failed to transition module %s from %s to %s: %s",
            moduleName, fromState, toState, error.getMessage()
        ));
        
        logger.log(Level.SEVERE, String.format(
            "Module state transition failed for %s", moduleName
        ), error);
        
        // Versuche Rollback zum vorherigen Zustand
        if (fromState != null && fromState != ModuleState.ERROR) {
            try {
                moduleStates.put(moduleName, fromState);
                logStateChange(moduleName, toState, fromState);
            } catch (Exception e) {
                logger.log(Level.SEVERE, 
                    "Failed to rollback module state after transition failure", e
                );
                // Letzter Ausweg: Setze in Fehlerzustand
                moduleStates.put(moduleName, ModuleState.ERROR);
            }
        } else {
            // Wenn kein Rollback möglich, setze in Fehlerzustand
            moduleStates.put(moduleName, ModuleState.ERROR);
        }
    }

    /**
     * Führt einen sicheren Zustandsübergang durch.
     */
    private boolean safeStateTransition(String moduleName, ModuleState targetState, 
                                      Runnable transitionAction) {
        Lock lock = moduleLocks.get(moduleName);
        if (lock == null) {
            console.error("No lock found for module: " + moduleName);
            return false;
        }
        
        lock.lock();
        try {
            ModuleState currentState = moduleStates.get(moduleName);
            validateStateTransition(moduleName, currentState, targetState);
            
            // Führe den Übergang durch
            transitionAction.run();
            
            // Aktualisiere Zustand und Zeit
            moduleStates.put(moduleName, targetState);
            stateTransitionTimes.put(moduleName, System.currentTimeMillis());
            logStateChange(moduleName, currentState, targetState);
            
            // Benachrichtige Listener
            transitionQueue.offer(new StateTransitionEvent(
                moduleName, currentState, targetState, System.currentTimeMillis()
            ));
            
            return true;
            
        } catch (ModuleStateException e) {
            handleTransitionFailure(moduleName, e.getFromState(), e.getToState(), e);
            return false;
        } catch (Exception e) {
            ModuleState currentState = moduleStates.get(moduleName);
            handleTransitionFailure(moduleName, currentState, targetState, e);
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Überwacht die Zustandsübergänge auf Timeouts.
     */
    private void monitorStates() {
        long currentTime = System.currentTimeMillis();
        
        stateLock.readLock().lock();
        try {
            for (Map.Entry<String, ModuleState> entry : moduleStates.entrySet()) {
                String moduleName = entry.getKey();
                ModuleState state = entry.getValue();
                
                if (state.isTransitionalState()) {
                    Long transitionStart = stateTransitionTimes.get(moduleName);
                    if (transitionStart != null && 
                        (currentTime - transitionStart) > STATE_TRANSITION_TIMEOUT) {
                        
                        logger.severe(String.format(
                            "Zustandsübergang-Timeout für Modul %s im Zustand %s",
                            moduleName, state
                        ));
                        
                        // Versuch, das Modul in einen sicheren Zustand zu bringen
                        transitionToState(moduleName, ModuleState.ERROR);
                    }
                }
            }
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Protokolliert einen Zustandsübergang in der Historie.
     */
    private void logStateChange(String moduleName, ModuleState oldState, ModuleState newState) {
        List<ModuleState> history = stateHistory.get(moduleName);
        if (history != null) {
            history.add(newState);
            while (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }
        }

        // Logging des Zustandsübergangs
        String message = String.format(
            "Modul %s: %s -> %s",
            moduleName,
            oldState != null ? oldState.getDescription() : "Initial",
            newState.getDescription()
        );
        
        if (newState == ModuleState.ERROR) {
            logger.severe(message);
        } else {
            logger.info(message);
        }
    }
    
    /**
     * Bereinigt Ressourcen beim Herunterfahren.
     */
    public void shutdown() {
        stateMonitor.shutdown();
        try {
            if (!stateMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                stateMonitor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stateMonitor.shutdownNow();
        }
    }
}
