package com.essentialscore.api;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
<<<<<<< HEAD
 * Hauptschnittstelle für Module des ApiCore
 * Jedes Modul sollte dieses Interface implementieren, um mit dem ApiCore zu funktionieren.
 */
public interface Module {
    
    /**
     * Wird beim Laden des Moduls aufgerufen
     * 
     * @param moduleAPI Die API für dieses Modul
     * @param config Die Konfiguration des Moduls
     */
    void init(ModuleAPI moduleAPI, FileConfiguration config);
    
    /**
     * Wird beim Aktivieren des Moduls aufgerufen (nach der Initialisierung)
     */
    default void onEnable() {
        // Standardmäßig keine Aktion notwendig
    }
    
    /**
     * Wird beim Deaktivieren des Moduls aufgerufen
=======
 * Main interface for EssentialsCore modules.
 * All modules should implement this interface to be properly loaded by the core.
 */
public interface Module {
    /**
     * Called before the module is initialized, allowing setup of early resources
     * 
     * @param api The module API instance for interacting with the core
     */
    default void onPreLoad(ModuleAPI api) {}
    
    /**
     * Called when the module is initialized
     * 
     * @param api The module API instance for interacting with the core
     * @param config The module's configuration file
     */
    void init(ModuleAPI api, FileConfiguration config);
    
    /**
     * Called after the module is initialized, allowing final setup steps
     */
    default void onPostLoad() {}
    
    /**
     * Called when the module is enabled or re-enabled after a reload
     * This is called after init() and can be used to start tasks or register listeners
     */
    default void onEnable() {}
    
    /**
     * Called when the module is disabled
>>>>>>> 1cd13da (Das ist Dumm)
     */
    void onDisable();
    
    /**
<<<<<<< HEAD
     * Wird aufgerufen, wenn ein Befehl dieses Moduls ausgeführt wird
     * 
     * @param command Das verwendete Kommando
     * @param sender Der Absender des Befehls
     * @param args Die Argumente des Befehls
     * @return true, wenn der Befehl erfolgreich ausgeführt wurde
     */
    default boolean onCommand(String command, CommandSender sender, String[] args) {
        return false;
    }
    
    /**
     * Wird aufgerufen, wenn eine Tab-Vervollständigung für einen Befehl dieses Moduls angefordert wird
     * 
     * @param command Das verwendete Kommando
     * @param sender Der Absender des Befehls
     * @param args Die aktuellen Argumente des Befehls
     * @return Liste möglicher Vervollständigungen oder null
     */
    default List<String> onTabComplete(String command, CommandSender sender, String[] args) {
        return null;
    }
    
    /**
     * Wird aufgerufen, wenn ein Spieler dem Server beitritt
     * 
     * @param player Der Spieler, der dem Server beigetreten ist
     */
    default void onPlayerJoin(Player player) {
        // Standardmäßig keine Aktion notwendig
    }
    
    /**
     * Wird aufgerufen, wenn das Modul neu geladen werden soll
     * 
     * @return true, wenn das Neuladen erfolgreich war
     */
    default boolean onReload() {
=======
     * Called when the module is being unloaded completely
     * This allows the module to clean up resources before being removed
     */
    default void onUnload() {}
    
    /**
     * Called when the module is being reloaded
     * 
     * @param config The new configuration for the module
     * @return true if reload was successful, false otherwise
     */
    default boolean onReload(FileConfiguration config) {
>>>>>>> 1cd13da (Das ist Dumm)
        return true;
    }
    
    /**
<<<<<<< HEAD
     * Wird aufgerufen, um den Status des Moduls abzurufen
     * 
     * @return Eine Map mit Statusinformationen
     */
    default Map<String, Object> getStatus() {
        return new java.util.HashMap<>();
    }
    
    /**
     * Wird aufgerufen, um die Berechtigungen des Moduls zu registrieren
     * 
     * @param api Die ModuleAPI-Instanz
     */
    default void registerPermissions(ModuleAPI api) {
        // Standardmäßig keine Aktion notwendig
    }
=======
     * Called when a player joins the server
     * 
     * @param player The player who joined
     */
    default void onPlayerJoin(Player player) {}
    
    /**
     * Called when a command registered by this module is executed
     * 
     * @param commandName The name of the command
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    default boolean onCommand(String commandName, CommandSender sender, String[] args) { return true; }
    
    /**
     * Called when tab completion is requested for a command registered by this module
     * 
     * @param commandName The name of the command
     * @param sender The command sender
     * @param args The command arguments
     * @return A list of tab completion options, or null for default behavior
     */
    default List<String> onTabComplete(String commandName, CommandSender sender, String[] args) { return null; }
>>>>>>> 1cd13da (Das ist Dumm)
    
    /**
     * Gets the name of the module
     * 
     * @return The module name
     */
    String getName();
    
    /**
     * Gets the version of the module
     * 
     * @return The module version
     */
    String getVersion();
    
    /**
     * Gets the description of the module
     * 
     * @return The module description
     */
    String getDescription();
    
    /**
     * Gets the list of module dependencies
<<<<<<< HEAD
     * 
     * @return List of dependencies
     */
    default List<String> getDependencies() { return List.of(); }
=======
     * Dependencies are required for the module to function
     * 
     * @return Map of dependency names to version range requirements
     */
    default Map<String, String> getDependencies() { return Map.of(); }
    
    /**
     * Gets the list of optional module dependencies
     * Optional dependencies enhance functionality but are not required
     * 
     * @return Map of dependency names to version range requirements
     */
    default Map<String, String> getOptionalDependencies() { return Map.of(); }
>>>>>>> 1cd13da (Das ist Dumm)
} 