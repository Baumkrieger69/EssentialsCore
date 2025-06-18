package com.essentialscore.api;

import java.util.Map;

/**
 * Interface for module event listeners.
 * Modules implement this to handle events fired by the core or other modules.
 */
public interface ModuleEventListener {
    /**
     * Called when a module event is fired
     * 
     * @param eventName Name of the event
     * @param data Event data
     */
    void onModuleEvent(String eventName, Map<String, Object> data);
} 
