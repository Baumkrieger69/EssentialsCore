package com.essentialscore.api.performance;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for module states
 */
public class ModuleStateManager {
    private final Map<String, ModuleState> moduleStates;
    private final Map<String, Instant> stateChangeTimestamps;
    
    public ModuleStateManager() {
        this.moduleStates = new HashMap<>();
        this.stateChangeTimestamps = new HashMap<>();
    }
    
    public ModuleState getState(String moduleName) {
        return moduleStates.getOrDefault(moduleName, ModuleState.UNLOADED);
    }
    
    public void setState(String moduleName, ModuleState state) {
        moduleStates.put(moduleName, state);
        stateChangeTimestamps.put(moduleName, Instant.now());
    }
    
    public Instant getLastStateChange(String moduleName) {
        return stateChangeTimestamps.get(moduleName);
    }
}
