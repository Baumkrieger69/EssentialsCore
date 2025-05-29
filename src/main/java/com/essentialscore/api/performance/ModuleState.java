package com.essentialscore.api.performance;

/**
 * Module state enumeration
 */
public enum ModuleState {
    UNLOADED,
    LOADING,
    LOADED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR,
    DISABLED
}
