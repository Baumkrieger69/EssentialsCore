package com.essentialscore.api.impl;

import com.essentialscore.api.integration.IIntegrationManager;
import com.essentialscore.api.integration.PluginIntegration;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * A dummy integration manager that returns empty results.
 * Used as a fallback when the real integration manager is not available.
 */
public class DummyIntegrationManager implements IIntegrationManager {
    private static final Logger LOGGER = Logger.getLogger(DummyIntegrationManager.class.getName());
    
    @Override
    public void initialize() {
        LOGGER.info("Dummy integration manager initialized");
    }
    
    @Override
    public void registerIntegration(PluginIntegration integration) {
        LOGGER.info("Ignoring registration of integration: " + 
            (integration != null ? integration.getClass().getSimpleName() : "null"));
    }
      @Override
    public <T extends PluginIntegration> Optional<T> getIntegration(Class<T> clazz) {
        return Optional.empty();
    }
    
    @Override
    public Optional<PluginIntegration> getIntegrationByPlugin(String pluginName) {
        return Optional.empty();
    }
    
    @Override
    public boolean hasIntegration(Class<? extends PluginIntegration> clazz) {
        return false;
    }
}
