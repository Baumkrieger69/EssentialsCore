package com.essentialscore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

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
        
        // Rohpräfix auslesen (ohne Formatierung)
        String rawPrefix = apiCore.getConfig().getString("console.prefixes.command-manager", "&8[&3&lCommand&b&lManager&8]");
        
        // Konsolen-Formatter initialisieren
        boolean useColors = apiCore.getConfig().getBoolean("console.use-colors", true);
        boolean showTimestamps = apiCore.getConfig().getBoolean("console.show-timestamps", false);
        boolean useUnicodeSymbols = apiCore.getConfig().getBoolean("console.use-unicode-symbols", true);
        String stylePreset = apiCore.getConfig().getString("console.style-preset", "default");
        
        console = new ConsoleFormatter(
            apiCore.getLogger(),
            rawPrefix,
            useColors, showTimestamps, useUnicodeSymbols, stylePreset
        );
        
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
            
            // Now get the knownCommands map from SimpleCommandMap
            Field knownCommandsMapField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsMapField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsMapField.get(commandMap);
            
            return new ArrayList<>(knownCommands.values());
        } catch (Exception e) {
            console.error("Fehler beim Zugriff auf die CommandMap: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Registriert die Core-Befehle des Plugins
     */
    public void registerCoreCommands() {
        // Prüfe, ob der Hauptbefehl in der Konfiguration deaktiviert ist
        if (isConfigDisabled("apicore")) {
            console.warning("Der apicore-Befehl ist in der Konfiguration deaktiviert!");
            return;
        }
        
        // Hauptbefehl registrieren
        apiCore.getCommand("apicore").setExecutor((sender, command, label, args) -> {
            if (args.length == 0) {
                showHelp(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            // Prüfe, ob der Unterbefehl in der Konfiguration deaktiviert ist
            if (isApiCoreCommand(subCommand) && isConfigDisabled(subCommand)) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cDer Befehl &e" + subCommand + " &cist deaktiviert."));
                return true;
            }

            // Handle subcommands
            switch (subCommand) {
                case "help":
                    showHelp(sender);
                    break;
                case "modules":
                case "list":
                    listModules(sender);
                    break;
                case "language":
                case "lang":
                    // Pass the command to the language command handler
                    // We need to remove the first argument (language/lang)
                    String[] langArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, langArgs, 0, args.length - 1);
                    return languageCommand.onCommand(sender, command, label, langArgs);
                // Weitere Unterbefehle hier einfügen
                default:
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cUnbekannter Befehl! Nutze &f/apicore help &cfür eine Übersicht."));
                    break;
            }

            return true;
        });
        
        // Tab-Completer registrieren
        apiCore.getCommand("apicore").setTabCompleter((sender, command, alias, args) -> {
            List<String> completions = new ArrayList<>();
            
            if (args.length == 1) {
                // Hauptbefehle
                List<String> commands = Arrays.asList("help", "modules", "list", "info", "commands", 
                    "enable", "disable", "reload", "debug", "status", "benchmark", "cache", 
                    "permission", "perm", "sandbox", "performance", "perf", "language", "lang");
                
                // Filtere deaktivierte Befehle
                commands = commands.stream()
                    .filter(cmd -> !isConfigDisabled(cmd))
                    .collect(Collectors.toList());
                
                for (String cmd : commands) {
                    if (cmd.startsWith(args[0].toLowerCase())) {
                        completions.add(cmd);
                    }
                }
            } else if (args.length >= 2 && (args[0].equalsIgnoreCase("language") || args[0].equalsIgnoreCase("lang"))) {
                // For language subcommands, delegate to the language command handler
                String[] langArgs = new String[args.length - 1];
                System.arraycopy(args, 1, langArgs, 0, args.length - 1);
                return languageCommand.onTabComplete(sender, command, alias, langArgs);
            }
            
            return completions;
        });
    }
    
    /**
     * Registriert den Befehl zum Verwalten der Befehlsdeaktivierung
     */
    public void registerCommandDeactivationCommand() {
        // Diese Funktionalität wurde entfernt und wird jetzt komplett über die config.yml verwaltet
        console.info("Befehlsdeaktivierung wird jetzt über die config.yml verwaltet");
    }
    
    /**
     * Zeigt die Hilfe für den ApiCore-Befehl
     * 
     * @param sender Der Empfänger der Hilfe-Nachricht
     */
    private void showHelp(CommandSender sender) {
        // Implementation of help command
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fVerfügbare Befehle:"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/apicore help &8- &7Zeigt diese Hilfe an"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/apicore modules &8- &7Listet alle geladenen Module auf"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/apicore info <modul> &8- &7Zeigt Informationen zu einem Modul"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/apicore language &8- &7Spracheinstellungen verwalten"));
    }
    
    /**
     * Listet alle geladenen Module auf
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void listModules(CommandSender sender) {
        // Implementation of module listing
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fGeladene Module:"));
        for (String moduleName : apiCore.getLoadedModules().keySet()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &b" + moduleName));
        }
    }

    /**
     * Registriert mehrere Befehle auf einmal
     * 
     * @param commands Die zu registrierenden Befehle
     */
    public void registerCommands(List<DynamicCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        for (DynamicCommand command : commands) {
            try {
                registerCommand(command);
            } catch (Exception e) {
                console.error("Fehler beim Registrieren des Befehls " + command.getName() + ": " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Registriert alle Befehle
     */
    public void registerCommands() {
        // Register module command for lifecycle management
        ModuleCommand moduleCommand = new ModuleCommand(apiCore);
        try {
            PluginCommand moduleCmdPlug = apiCore.getCommand("module");
            if (moduleCmdPlug != null) {
                moduleCmdPlug.setExecutor(moduleCommand);
                moduleCmdPlug.setTabCompleter(moduleCommand);
                console.debug("Registered module command", apiCore.isDebugMode());
            } else {
                console.error("Failed to register module command - command not found in plugin.yml");
            }
        } catch (Exception e) {
            console.error("Error registering module command: " + e.getMessage());
        }
        
        // Register core commands
        registerCoreCommands();
    }
    
    /**
     * Deregistriert mehrere Befehle auf einmal
     * 
     * @param commands Die zu deregistrierenden Befehle
     */
    public void unregisterCommands(List<DynamicCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        for (DynamicCommand command : commands) {
            try {
                unregisterCommand(command.getName());
            } catch (Exception e) {
                console.error("Fehler beim Deregistrieren des Befehls " + command.getName() + ": " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Registriert einen Befehl
     * 
     * @param command Der zu registrierende Befehl
     */
    private void registerCommand(DynamicCommand command) {
        // Implementation of command registration
    }
    
    /**
     * Für die Kompatibilität mit bestehenden API-Aufrufen
     */
    public boolean isCommandDeactivated(String commandName) {
        return isConfigDisabled(commandName);
    }
    
    /**
     * Für die Kompatibilität mit bestehenden API-Aufrufen
     */
    public boolean deactivateCommand(String commandName) {
        console.info("Befehlsdeaktivierung wird jetzt über die config.yml verwaltet. Bitte aktualisiere die Konfigurationsdatei.");
        return false;
    }
    
    /**
     * Für die Kompatibilität mit bestehenden API-Aufrufen
     */
    public boolean reactivateCommand(String commandName) {
        console.info("Befehlsdeaktivierung wird jetzt über die config.yml verwaltet. Bitte aktualisiere die Konfigurationsdatei.");
        return false;
    }
} 