package com.essentialscore;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.essentialscore.commands.ModuleCommand;
import com.essentialscore.commands.LanguageCommand;

/**
 * Manager für Befehle im ApiCore
 */
public class CommandManager {
    private final ApiCore apiCore;
    private ConsoleFormatter console;
    private LanguageCommand languageCommand;
    
    public CommandManager(ApiCore apiCore) {
        this.apiCore = apiCore;
        
        // Erweiterte Konsolen-Formatter Konfiguration
        boolean useColors = apiCore.getConfig().getBoolean("console.use-colors", true);
        boolean showTimestamps = apiCore.getConfig().getBoolean("console.show-timestamps", false);
        boolean useUnicodeSymbols = apiCore.getConfig().getBoolean("console.use-unicode-symbols", true);
        String stylePreset = apiCore.getConfig().getString("console.style-preset", "default");
        
        // Konsolen-Formatter initialisieren mit Rohpräfix (ohne Formatierung)
        String rawPrefix = apiCore.getConfig().getString("console.prefixes.command-manager", "&8[&3&lCommand&b&lManager&8]");
        console = new ConsoleFormatter(apiCore.getLogger(), rawPrefix, useColors);
        
        // Initialize language command
        this.languageCommand = new LanguageCommand(apiCore);
    }
    
    /**
     * Prüft, ob ein Befehl ein ApiCore-Befehl ist
     * 
     * @param commandName Der zu prüfende Befehl
     * @return true, wenn es sich um einen ApiCore-Befehl handelt
     */
    private boolean isApiCoreCommand(String commandName) {
        // Liste der ApiCore-Befehle (ohne Modul-Befehle)
        Set<String> apiCoreCommands = new HashSet<>(Arrays.asList(
            "apicore", "reload", "core", "modules", "module", "mod",
            "acp", "acr", "apm", "acpm", "ess",
            "essentials", "esscore", "ec", "info", "help",
            "version", "debug", "performance", "benchmark",
            "cache", "diagnose", "permissions", "commands", "resources",
            "language", "lang"
        ));
        
        return apiCoreCommands.contains(commandName.toLowerCase());
    }
    
    /**
     * Überprüft, ob ein Befehl durch die Konfiguration deaktiviert ist
     * 
     * @param command Der zu prüfende Befehl
     * @return true, wenn der Befehl durch die Konfiguration deaktiviert ist
     */
    public boolean isConfigDisabled(String command) {
        // Normalisiere den Befehlsnamen
        command = command.toLowerCase();
        
        // Prüfe deaktivierte Befehle
        List<String> configDisabled = apiCore.getConfig().getStringList("commands.disabled");
        if (configDisabled.contains(command)) {
            return true;
        }
        
        // Prüfe individuell deaktivierte Befehle über die permissions-Sektion
        if (apiCore.getConfig().contains("commands.permissions." + command)) {
            boolean enabled = apiCore.getConfig().getBoolean("commands.permissions." + command, true);
            return !enabled; // Wenn enabled=false, dann ist der Befehl deaktiviert
        }
        
        return false;
    }
    
    /**
     * Überprüft vor der Ausführung eines Befehls, ob dieser deaktiviert ist
     * 
     * @param command Der auszuführende Befehl
     * @return true, wenn der Befehl deaktiviert ist und nicht ausgeführt werden soll
     */
    public boolean shouldBlockCommand(String command) {
        // Extrahiere den Hauptbefehl (erster Teil)
        String mainCommand = command.split(" ")[0].toLowerCase();
        
        // Prüfe, ob der Befehl in der Konfiguration deaktiviert ist
        if (isConfigDisabled(mainCommand)) {
            if (apiCore.isDebugMode()) {
                console.debug("Befehl ist deaktiviert: " + mainCommand, false);
            }
            return true;
        }
    
        return false;
    }
    
    /**
     * Deregistriert einen Befehl aus dem Bukkit-CommandMap-System
     * 
     * @param commandName Der Name des zu deregistrierenden Befehls
     */
    private void unregisterCommand(String commandName) {
        try {
            // Direkten Zugriff auf die CommandMap von Bukkit erhalten
            Object craftServer = Bukkit.getServer();
            Field commandMapField = craftServer.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            Object commandMap = commandMapField.get(craftServer);
            
            // Hole die knownCommands-Map
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = 
                (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            if (knownCommands != null) {
                // Sammle alle zu entfernenden Kommandos
                Set<String> keysToRemove = new HashSet<>();
                
                for (Map.Entry<String, org.bukkit.command.Command> entry : knownCommands.entrySet()) {
                    String key = entry.getKey();
                    org.bukkit.command.Command cmd = entry.getValue();
                    
                    // Entferne den Befehl und alle Aliase
                    if (key.equalsIgnoreCase(commandName) || 
                        key.equalsIgnoreCase("apicore:" + commandName) ||
                        key.equalsIgnoreCase("essentialscore:" + commandName)) {
                        keysToRemove.add(key);
                    }
                    
                    // Prüfe auf Aliase
                    if (cmd.getName().equalsIgnoreCase(commandName)) {
                        keysToRemove.add(key);
                    }
                }
                
                // Entferne alle identifizierten Befehle
                if (!keysToRemove.isEmpty()) {
                    for (String key : keysToRemove) {
                        knownCommands.remove(key);
                    }
                    console.info("Befehl '" + commandName + "' wurde deregistriert (" + keysToRemove.size() + " Einträge)");
                } else {
                    console.info("Befehl '" + commandName + "' wurde nicht in der CommandMap gefunden");
                }
            }
        } catch (Exception e) {
            console.error("Fehler beim Entfernen des Befehls '" + commandName + "': " + e.getMessage());
        }
    }

    /**
     * Holt alle Befehle aus der CommandMap
     * 
     * @return Eine Sammlung von Commands
     */
    private Collection<Command> getCommandMapCommands() {
        try {
            // Using reflection to get commands from the CommandMap
            Field knownCommandsField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            knownCommandsField.setAccessible(true);
            CommandMap commandMap = (CommandMap) knownCommandsField.get(Bukkit.getServer());
            
            // Get the knownCommands map
            Field mapField = commandMap.getClass().getDeclaredField("knownCommands");
            mapField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) mapField.get(commandMap);
            
            return new ArrayList<>(knownCommands.values());
        } catch (Exception e) {
            console.error("Fehler beim Abrufen der Command-Map: " + e.getMessage());
            return Collections.emptyList();
        }
    }
} 