package com.essentialscore.api.impl;

import com.essentialscore.api.integration.IIntegrationManager;
import com.essentialscore.api.integration.PluginIntegration;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An adapter for an object that acts like an IntegrationManager but doesn't implement the interface.
 * Uses reflection to call methods on the wrapped object.
 */
public class IntegrationManagerAdapter implements IIntegrationManager {
    private static final Logger LOGGER = Logger.getLogger(IntegrationManagerAdapter.class.getName());
    private final Object wrappedManager;
    
    public IntegrationManagerAdapter(Object wrappedManager) {
        this.wrappedManager = wrappedManager;
    }
    
    @Override
    public void initialize() {
        try {
            Method method = wrappedManager.getClass().getMethod("initialize");
            method.invoke(wrappedManager);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to initialize integration manager", e);
        }
    }
    
    @Override
    public void registerIntegration(PluginIntegration integration) {
        try {
            Method method = wrappedManager.getClass().getMethod("registerIntegration", PluginIntegration.class);
            method.invoke(wrappedManager, integration);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to register integration", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends PluginIntegration> Optional<T> getIntegration(Class<T> clazz) {
        try {
            Method method = wrappedManager.getClass().getMethod("getIntegration", Class.class);
            Object result = method.invoke(wrappedManager, clazz);
            
            if (result instanceof Optional) {
                return (Optional<T>) result;
            }
            
            if (clazz.isInstance(result)) {
                return Optional.of((T) result);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get integration", e);
            return Optional.empty();
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Optional<PluginIntegration> getIntegrationByPlugin(String pluginName) {
        try {
            Method method = wrappedManager.getClass().getMethod("getIntegrationByPlugin", String.class);
            Object result = method.invoke(wrappedManager, pluginName);
            
            if (result instanceof Optional) {
                return (Optional<PluginIntegration>) result;
            }
            
            if (result instanceof PluginIntegration) {
                return Optional.of((PluginIntegration) result);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get integration by plugin", e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean hasIntegration(Class<? extends PluginIntegration> clazz) {
        try {
            Method method = wrappedManager.getClass().getMethod("hasIntegration", Class.class);
            Object result = method.invoke(wrappedManager, clazz);
            
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to check if integration exists", e);
            return false;
        }
    }
}
