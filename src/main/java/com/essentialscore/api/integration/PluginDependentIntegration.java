package com.essentialscore.api.integration;

import org.bukkit.plugin.Plugin;

/**
 * Interface for integrations that depend on a specific plugin.
 */
public interface PluginDependentIntegration extends PluginIntegration {
    
    /**
     * Gets the plugin name.
     *
     * @return The plugin name
     */
    String getPluginName();
    
    /**
     * Gets the plugin instance.
     *
     * @return The plugin instance, or null if not available
     */
    Plugin getPlugin();
} 
