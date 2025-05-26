package com.essentialscore.lifecycle;

/**
 * Represents the possible states of a module in its lifecycle.
 */
public enum ModuleState {
    /**
     * Module is discovered but not yet loaded
     */
    DISCOVERED,
    
    /**
     * Module is in the pre-loading phase
     */
    PRE_LOADING,
    
    /**
     * Module has completed pre-loading
     */
    PRE_LOADED,
    
    /**
     * Module is currently being loaded
     */
    LOADING,
    
    /**
     * Module is loaded but not yet enabled
     */
    LOADED,
    
    /**
     * Module is in the post-loading phase
     */
    POST_LOADING,
    
    /**
     * Module has completed post-loading
     */
    POST_LOADED,
    
    /**
     * Module is currently being enabled
     */
    ENABLING,
    
    /**
     * Module is fully enabled and running
     */
    ENABLED,
    
    /**
     * Module is currently being disabled
     */
    DISABLING,
    
    /**
     * Module is disabled but still loaded
     */
    DISABLED,
    
    /**
     * Module is currently being unloaded
     */
    UNLOADING,
    
    /**
     * Module is unloaded completely
     */
    UNLOADED,
    
    /**
     * Module is currently being reloaded
     */
    RELOADING,
    
    /**
     * Module failed to load due to an error
     */
    ERROR;
    
    /**
     * Checks if this state represents an active module that is usable.
     * 
     * @return true if the module is in a usable state
     */
    public boolean isActive() {
        return this == ENABLED;
    }
    
    /**
     * Checks if this state represents a module that is loaded but not necessarily enabled.
     * 
     * @return true if the module is loaded
     */
    public boolean isLoaded() {
        return this == LOADED || this == POST_LOADED || this == ENABLING || 
               this == ENABLED || this == DISABLING || this == DISABLED || 
               this == RELOADING;
    }
    
    /**
     * Checks if this state represents a terminal state.
     * 
     * @return true if the module is in a terminal state
     */
    public boolean isTerminal() {
        return this == UNLOADED || this == ERROR;
    }
    
    /**
     * Gets the next state in the normal lifecycle progression.
     * 
     * @return the next state, or null if this is a terminal state
     */
    public ModuleState getNextState() {
        switch (this) {
            case DISCOVERED: return PRE_LOADING;
            case PRE_LOADING: return PRE_LOADED;
            case PRE_LOADED: return LOADING;
            case LOADING: return LOADED;
            case LOADED: return POST_LOADING;
            case POST_LOADING: return POST_LOADED;
            case POST_LOADED: return ENABLING;
            case ENABLING: return ENABLED;
            case DISABLING: return DISABLED;
            case RELOADING: return ENABLED;
            default: return null;
        }
    }
} 