package com.essentialscore.api.integration;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base implementation for plugin integrations.
 */
public abstract class AbstractPluginIntegration implements PluginIntegration {
    protected final Logger logger;
    protected final Plugin plugin;
    protected boolean available;
    
    /**
     * Creates a new abstract plugin integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public AbstractPluginIntegration(Plugin plugin) {
        this.plugin = plugin;
        this.logger = Logger.getLogger(getClass().getName());
        this.available = false;
    }
    
    @Override
    public void initialize() {
        try {
            onInitialize();
            available = true;
            logger.info("Initialized " + getName() + " integration");
        } catch (Exception e) {
            available = false;
            logger.log(Level.SEVERE, "Failed to initialize " + getName() + " integration", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (available) {
            try {
                onShutdown();
                logger.info("Shut down " + getName() + " integration");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to shut down " + getName() + " integration", e);
            }
        }
        available = false;
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * Called when the integration is being initialized.
     * Implement this to set up the integration.
     */
    protected abstract void onInitialize();
    
    /**
     * Called when the integration is being shut down.
     * Implement this to clean up the integration.
     */
    protected abstract void onShutdown();
} 
