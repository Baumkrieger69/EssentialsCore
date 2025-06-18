package com.essentialscore.api.integration;

/**
 * Base interface for all plugin integrations.
 */
public interface PluginIntegration {
    
    /**
     * Initializes the integration.
     */
    void initialize();
    
    /**
     * Shuts down the integration.
     */
    void shutdown();
    
    /**
     * Checks if the integration is available.
     *
     * @return true if the integration is available
     */
    boolean isAvailable();
    
    /**
     * Gets the integration name.
     *
     * @return The integration name
     */
    String getName();
} 
