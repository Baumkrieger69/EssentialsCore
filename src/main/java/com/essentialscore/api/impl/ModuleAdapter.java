package com.essentialscore.api.impl;

import com.essentialscore.ApiCore;
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Map;

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
     * @param apiCore The ApiCore instance
     * @param config The module configuration
     */
    public void init(ApiCore apiCore, FileConfiguration config) {
        // The module should already be initialized with ModuleAPI
        // No need to re-assign apiCore as it's now final
    }
    
    /**
     * Called when the module is disabled
     */
    public void onDisable() {
        try {
            module.onDisable();
        } catch (Exception e) {
            apiCore.getLogger().severe("Fehler beim Deaktivieren des Moduls " + module.getClass().getName() + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
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
    
    /**
     * Gibt den Modulnamen zurück
     * 
     * @return Der Name des Moduls
     */
    public String getName() {
        return moduleAPI.getModuleName();
    }
    
    /**
     * Gibt die Modulkonfiguration zurück
     * 
     * @return Die Konfiguration
     */
    public FileConfiguration getConfig() {
        return moduleAPI.getConfig();
    }
    
    /**
     * Gibt das Datenverzeichnis des Moduls zurück
     * 
     * @return Das Datenverzeichnis
     */
    public File getDataFolder() {
        return moduleAPI.getDataFolder();
    }
    
    /**
     * Prüft, ob ein Spieler eine Berechtigung hat
     * 
     * @param player Der Spieler
     * @param permission Die zu prüfende Berechtigung
     * @return true, wenn der Spieler die Berechtigung hat
     */
    public boolean hasPermission(Player player, String permission) {
        return moduleAPI.hasPermission(player, permission);
    }
    
    /**
     * Speichert einen gemeinsam genutzten Datenwert
     * 
     * @param key Der Schlüssel
     * @param value Der Wert
     */
    public void setSharedData(String key, Object value) {
        moduleAPI.setSharedData(key, value);
    }
    
    /**
     * Holt einen gemeinsam genutzten Datenwert
     * 
     * @param key Der Schlüssel
     * @return Der Wert oder null, wenn nicht gefunden
     */
    public Object getSharedData(String key) {
        return moduleAPI.getSharedData(key);
    }
    
    /**
     * Sendet einem Spieler oder CommandSender eine formatierte Nachricht
     * 
     * @param target Der Empfänger
     * @param message Die Nachricht
     */
    public void sendMessage(CommandSender target, String message) {
        moduleAPI.sendMessage(target, message);
    }
    
    /**
     * Löst ein Modul-Event aus
     * 
     * @param eventName Der Name des Events
     * @param data Die Event-Daten
     */
    public void fireModuleEvent(String eventName, Map<String, Object> data) {
        moduleAPI.fireModuleEvent(eventName, data);
    }
} 