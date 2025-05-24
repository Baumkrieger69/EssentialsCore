package com.essentialscore.api;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
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
     */
    void onDisable();
    
    /**
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
        return true;
    }
    
    /**
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
     * 
     * @return List of dependencies
     */
    default List<String> getDependencies() { return List.of(); }
} 