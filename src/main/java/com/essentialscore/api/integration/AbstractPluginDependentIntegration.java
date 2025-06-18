package com.essentialscore.api.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Abstract base implementation for plugin-dependent integrations.
 */
public abstract class AbstractPluginDependentIntegration extends AbstractPluginIntegration implements PluginDependentIntegration {
    protected final String pluginName;
    protected Plugin dependencyPlugin;
    
    /**
     * Creates a new abstract plugin-dependent integration.
     *
     * @param plugin The EssentialsCore plugin
     * @param pluginName The name of the plugin this integration depends on
     */
    public AbstractPluginDependentIntegration(Plugin plugin, String pluginName) {
        super(plugin);
        this.pluginName = pluginName;
    }
    
    @Override
    protected void onInitialize() {
        // Check if the plugin is available
        dependencyPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        
        if (dependencyPlugin == null) {
            logger.warning(pluginName + " plugin not found, integration will be disabled");
            throw new IllegalStateException(pluginName + " plugin not found");
        }
        
        if (!dependencyPlugin.isEnabled()) {
            logger.warning(pluginName + " plugin is not enabled, integration will be disabled");
            throw new IllegalStateException(pluginName + " plugin is not enabled");
        }
        
        try {
            // Call plugin-specific initialization
            onPluginInitialize();
            logger.info("Successfully hooked into " + pluginName);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to hook into " + pluginName, e);
            throw e;
        }
    }
    
    @Override
    protected void onShutdown() {
        if (dependencyPlugin != null) {
            try {
                // Call plugin-specific shutdown
                onPluginShutdown();
                logger.info("Unhooked from " + pluginName);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to unhook from " + pluginName, e);
                throw e;
            } finally {
                dependencyPlugin = null;
            }
        }
    }
    
    @Override
    public String getPluginName() {
        return pluginName;
    }
    
    @Override
    public Plugin getPlugin() {
        return dependencyPlugin;
    }
    
    /**
     * Called when the integration is being initialized with the plugin.
     * Implement this to set up the integration with the plugin.
     */
    protected abstract void onPluginInitialize();
    
    /**
     * Called when the integration is being shut down from the plugin.
     * Implement this to clean up the integration with the plugin.
     */
    protected abstract void onPluginShutdown();
} 
