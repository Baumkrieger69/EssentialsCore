package com.essentialscore.api.impl;

import com.essentialscore.ApiCore;
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Adapter that wraps a Module instance to present it as a legacy module.
 * This provides backward compatibility for systems that expect the old interface structure.
 */
public class ModuleAdapter {
    private final Module module;
    private final ModuleAPI moduleAPI;
    private final ApiCore apiCore;
    
    /**
     * Creates a new module adapter
     * 
     * @param module The module to adapt
     * @param moduleAPI The module API instance
     * @param apiCore The ApiCore instance
     */
    public ModuleAdapter(Module module, ModuleAPI moduleAPI, ApiCore apiCore) {
        this.module = module;
        this.moduleAPI = moduleAPI;
        this.apiCore = apiCore;
    }
    
    /**
     * Legacy initialization method
     * 
     * @param core The ApiCore instance
     * @param config The module configuration
     */
    public void init(ApiCore core, FileConfiguration config) {
        // The module should already be initialized with ModuleAPI
        // No need to re-assign apiCore as it's now final
    }
    
    /**
     * Called when the module is disabled
     */
    public void onDisable() {
        module.onDisable();
    }
    
    /**
     * Called when a player joins the server
     * 
     * @param player The player who joined
     */
    public void onPlayerJoin(Player player) {
        module.onPlayerJoin(player);
    }
    
    /**
     * Called when a command registered by this module is executed
     * 
     * @param commandName The name of the command
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        return module.onCommand(commandName, sender, args);
    }
    
    /**
     * Called when tab completion is requested for a command registered by this module
     * 
     * @param commandName The name of the command
     * @param sender The command sender
     * @param args The command arguments
     * @return A list of tab completion options, or null for default behavior
     */
    public List<String> onTabComplete(String commandName, CommandSender sender, String[] args) {
        return module.onTabComplete(commandName, sender, args);
    }
    
    /**
     * Gets the wrapped module
     * 
     * @return The adapted module
     */
    public Module getModule() {
        return module;
    }
    
    /**
     * Gets the module API instance
     * 
     * @return The module API
     */
    public ModuleAPI getModuleAPI() {
        return moduleAPI;
    }
} 