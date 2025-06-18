package com.essentialscore.api.integration;

import com.essentialscore.api.integration.bukkit.BukkitIntegration;
import com.essentialscore.api.integration.permissions.PermissionIntegration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages integrations with other plugins and Bukkit systems.
 */
public class IntegrationManager implements IIntegrationManager {
    private static final Logger LOGGER = Logger.getLogger(IntegrationManager.class.getName());
    private final Plugin plugin;
    private final Map<Class<? extends PluginIntegration>, PluginIntegration> integrations = new HashMap<>();
    private final Map<String, PluginIntegration> pluginNameMap = new HashMap<>();
    
    /**
     * Creates a new integration manager.
     *
     * @param plugin The EssentialsCore plugin instance
     */
    public IntegrationManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initializes all integrations.
     */
    public void initialize() {
        LOGGER.info("Initializing integration manager...");
        
        // Register default integrations
        registerBukkitIntegration();
        registerPermissionIntegration();
        registerEconomyIntegration();
        registerPlaceholderIntegration();
        registerWorldGuardIntegration();
        
        LOGGER.info("Integration manager initialized with " + integrations.size() + " integrations.");
    }
    
    /**
     * Registers a plugin integration.
     *
     * @param integration The integration to register
     */
    public void registerIntegration(PluginIntegration integration) {
        try {
            integrations.put(integration.getClass(), integration);
            
            // Add plugin name mapping if available
            if (integration instanceof PluginDependentIntegration) {
                PluginDependentIntegration pluginDep = (PluginDependentIntegration) integration;
                pluginNameMap.put(pluginDep.getPluginName().toLowerCase(), integration);
            }
            
            integration.initialize();
            LOGGER.info("Registered integration: " + integration.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register integration: " + integration.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Gets an integration by its class.
     *
     * @param clazz The integration class
     * @param <T> The integration type
     * @return The integration, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends PluginIntegration> Optional<T> getIntegration(Class<T> clazz) {
        return Optional.ofNullable((T) integrations.get(clazz));
    }
    
    /**
     * Gets an integration by the plugin name.
     *
     * @param pluginName The plugin name
     * @return The integration, or empty if not found
     */
    public Optional<PluginIntegration> getIntegrationByPlugin(String pluginName) {
        return Optional.ofNullable(pluginNameMap.get(pluginName.toLowerCase()));
    }
    
    /**
     * Checks if an integration is available.
     *
     * @param clazz The integration class
     * @return true if the integration is available
     */
    public boolean hasIntegration(Class<? extends PluginIntegration> clazz) {
        return integrations.containsKey(clazz);
    }
    
    /**
     * Checks if a plugin integration is available.
     *
     * @param pluginName The plugin name
     * @return true if the integration is available
     */
    public boolean hasPluginIntegration(String pluginName) {
        return pluginNameMap.containsKey(pluginName.toLowerCase());
    }
    
    /**
     * Disables all integrations.
     */
    public void shutdown() {
        LOGGER.info("Shutting down integration manager...");
        
        for (PluginIntegration integration : integrations.values()) {
            try {
                integration.shutdown();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to shutdown integration: " + integration.getClass().getSimpleName(), e);
            }
        }
        
        integrations.clear();
        pluginNameMap.clear();
        
        LOGGER.info("Integration manager shutdown complete.");
    }
    
    /**
     * Checks if a plugin is available.
     *
     * @param pluginName The plugin name
     * @return true if the plugin is available
     */
    public boolean isPluginAvailable(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }
    
    /**
     * Gets a plugin by name.
     *
     * @param pluginName The plugin name
     * @return The plugin, or empty if not found
     */
    public Optional<Plugin> getPlugin(String pluginName) {
        return Optional.ofNullable(Bukkit.getPluginManager().getPlugin(pluginName));
    }
    
    // Private methods to register default integrations
    
    private void registerBukkitIntegration() {
        registerIntegration(new BukkitIntegration(plugin));
    }
    
    private void registerPermissionIntegration() {
        // Default permission system
        registerIntegration(new PermissionIntegration(plugin));
        
        // Try to register Vault permissions if available
        if (isPluginAvailable("Vault")) {
            try {
                Class<?> vaultPermissionClass = Class.forName("com.essentialscore.api.integration.permissions.VaultPermissionIntegration");
                PluginIntegration vaultPermission = (PluginIntegration) vaultPermissionClass
                    .getConstructor(Plugin.class)
                    .newInstance(plugin);
                
                registerIntegration(vaultPermission);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load Vault permission integration", e);
            }
        }
    }
    
    private void registerEconomyIntegration() {
        // Try to register Vault economy if available
        if (isPluginAvailable("Vault")) {
            try {
                Class<?> vaultEconomyClass = Class.forName("com.essentialscore.api.integration.economy.VaultEconomyIntegration");
                PluginIntegration vaultEconomy = (PluginIntegration) vaultEconomyClass
                    .getConstructor(Plugin.class)
                    .newInstance(plugin);
                
                registerIntegration(vaultEconomy);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load Vault economy integration", e);
            }
        }
    }
    
    private void registerPlaceholderIntegration() {
        // Try to register PlaceholderAPI if available
        if (isPluginAvailable("PlaceholderAPI")) {
            try {
                Class<?> placeholderApiClass = Class.forName("com.essentialscore.api.integration.placeholders.PlaceholderAPIIntegration");
                PluginIntegration placeholderApi = (PluginIntegration) placeholderApiClass
                    .getConstructor(Plugin.class)
                    .newInstance(plugin);
                
                registerIntegration(placeholderApi);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load PlaceholderAPI integration", e);
            }
        }
    }
    
    private void registerWorldGuardIntegration() {
        // Try to register WorldGuard if available
        if (isPluginAvailable("WorldGuard")) {
            try {
                Class<?> worldGuardClass = Class.forName("com.essentialscore.api.integration.worldguard.WorldGuardIntegration");
                PluginIntegration worldGuard = (PluginIntegration) worldGuardClass
                    .getConstructor(Plugin.class)
                    .newInstance(plugin);
                
                registerIntegration(worldGuard);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load WorldGuard integration", e);
            }
        }
    }
} 
