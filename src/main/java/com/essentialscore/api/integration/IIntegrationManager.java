package com.essentialscore.api.integration;

import java.util.Optional;

/**
 * Interface for integration manager operations.
 * This interface defines the contract for managing plugin integrations.
 */
public interface IIntegrationManager {
    
    /**
     * Initializes the integration manager.
     */
    void initialize();
    
    /**
     * Registers a plugin integration.
     *
     * @param integration The integration to register
     */
    void registerIntegration(PluginIntegration integration);
    
    /**
     * Gets an integration by its class.
     *
     * @param clazz The integration class
     * @param <T> The integration type
     * @return The integration, or empty if not found
     */
    <T extends PluginIntegration> Optional<T> getIntegration(Class<T> clazz);
    
    /**
     * Gets an integration by the plugin name.
     *
     * @param pluginName The plugin name
     * @return The integration, or empty if not found
     */
    Optional<PluginIntegration> getIntegrationByPlugin(String pluginName);
    
    /**
     * Checks if an integration is available.
     *
     * @param clazz The integration class
     * @return true if the integration is available
     */
    boolean hasIntegration(Class<? extends PluginIntegration> clazz);
}
