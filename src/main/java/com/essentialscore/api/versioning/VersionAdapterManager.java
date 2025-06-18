package com.essentialscore.api.versioning;

import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for version-specific adapters to ensure compatibility across Minecraft versions.
 */
public class VersionAdapterManager {
    private static final Logger LOGGER = Logger.getLogger(VersionAdapterManager.class.getName());
    private final Map<Class<?>, Object> adapters = new HashMap<>();
    private final Plugin plugin;
    
    /**
     * Creates a new version adapter manager.
     *
     * @param plugin The plugin
     */
    public VersionAdapterManager(Plugin plugin) {
        this.plugin = plugin;
        initializeAdapters();
    }
    
    /**
     * Initializes version-specific adapters.
     */
    private void initializeAdapters() {
        // Add common adapters
        
        // Register version-specific adapters
        // These will be implemented later based on actual version differences
        
        LOGGER.info("Initialized version adapters for Minecraft " + ServerVersion.getVersion());
    }
    
    /**
     * Gets an adapter for a specific interface.
     *
     * @param adapterInterface The adapter interface
     * @param <T> The adapter type
     * @return The adapter, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAdapter(Class<T> adapterInterface) {
        return Optional.ofNullable((T) adapters.get(adapterInterface));
    }
    
    /**
     * Registers an adapter.
     *
     * @param adapterInterface The adapter interface
     * @param adapter The adapter instance
     * @param <T> The adapter type
     */
    public <T> void registerAdapter(Class<T> adapterInterface, T adapter) {
        adapters.put(adapterInterface, adapter);
        LOGGER.info("Registered " + adapterInterface.getSimpleName() + " adapter");
    }
    
    /**
     * Creates and registers a version-specific adapter.
     *
     * @param adapterInterface The adapter interface
     * @param <T> The adapter type
     * @return true if the adapter was successfully created and registered
     */
    public <T> boolean createVersionAdapter(Class<T> adapterInterface) {
        try {
            // Get the base package for adapters
            String basePackage = adapterInterface.getPackage().getName();
            
            // Determine which version-specific implementation to use
            String implementationClassName;
            int minorVersion = ServerVersion.getMinorVersion();
            
            // Try to find the closest matching implementation
            // Start with the exact version and work down
            T adapter = null;
            
            while (minorVersion >= 8 && adapter == null) { // 1.8 is our minimum supported version
                implementationClassName = basePackage + ".v1_" + minorVersion + "." + 
                                         adapterInterface.getSimpleName() + "Impl";
                
                try {
                    Class<?> implClass = Class.forName(implementationClassName);
                    
                    if (adapterInterface.isAssignableFrom(implClass)) {
                        @SuppressWarnings("unchecked")
                        T impl = (T) implClass.getDeclaredConstructor(Plugin.class).newInstance(plugin);
                        adapter = impl;
                    }
                } catch (ClassNotFoundException e) {
                    // Implementation for this version doesn't exist, try the next one down
                }
                
                minorVersion--;
            }
            
            // If no specific version found, try the default implementation
            if (adapter == null) {
                implementationClassName = basePackage + ".common." + adapterInterface.getSimpleName() + "Impl";
                
                try {
                    Class<?> implClass = Class.forName(implementationClassName);
                    
                    if (adapterInterface.isAssignableFrom(implClass)) {
                        @SuppressWarnings("unchecked")
                        T impl = (T) implClass.getDeclaredConstructor(Plugin.class).newInstance(plugin);
                        adapter = impl;
                    }
                } catch (ClassNotFoundException e) {
                    // Default implementation doesn't exist
                    LOGGER.warning("No implementation found for " + adapterInterface.getSimpleName());
                    return false;
                }
            }
            
            if (adapter != null) {
                registerAdapter(adapterInterface, adapter);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create version adapter for " + adapterInterface.getSimpleName(), e);
            return false;
        }
    }
} 
