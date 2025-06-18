package com.essentialscore.api.security;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * A dummy implementation of SecurityManager for testing and fallback purposes.
 * This implementation allows all operations and provides minimal security functionality.
 */
public class DummySecurityManager extends SecurityManager {
    
    /**
     * Creates a new dummy security manager.
     *
     * @param plugin The plugin
     */
    public DummySecurityManager(Plugin plugin) {
        super(plugin);
    }
    
    /**
     * Default constructor for cases where no plugin is available.
     */
    public DummySecurityManager() {
        super(null);
    }
    
    @Override
    public boolean isOperationAllowed(String moduleId, String operation, String target) {
        // Dummy implementation - always allow operations
        return true;
    }
    
    @Override
    public void initialize() {
        // Dummy implementation - do nothing
    }
    
    @Override
    public void shutdown() {
        // Dummy implementation - do nothing
    }
    
    // Override other critical methods to provide safe dummy implementations
    public boolean validatePermission(String permission) {
        return true;
    }
    
    public boolean checkAccess(Player player, String resource) {
        return true;
    }
}
