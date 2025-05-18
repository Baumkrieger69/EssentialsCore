package com.essentialscore;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Manager für Befehle im ApiCore
 */
public class CommandManager {
    private final ApiCore apiCore;
    
    public CommandManager(ApiCore apiCore) {
        this.apiCore = apiCore;
    }
    
    /**
     * Registriert die Core-Befehle
     */
    public void registerCoreCommands() {
        apiCore.getLogger().info("Registriere Core-Befehle...");
        
        PluginCommand apiCoreCommand = apiCore.getCommand("apicore");
        
        if (apiCoreCommand == null) {
            apiCore.getLogger().severe("Konnte Hauptbefehl nicht registrieren! Stellen Sie sicher, dass plugin.yml korrekt ist.");
            return;
        }
        
        apiCoreCommand.setExecutor((sender, command, label, args) -> {
            if (args.length < 1) {
                showHelp(sender);
                return true;
            }

            String cmd = args[0].toLowerCase();
            
            switch (cmd) {
                case "help":
                    showHelp(sender);
                    break;
                case "modules":
                    if (args.length > 1 && args[1].equalsIgnoreCase("info") && args.length > 2) {
                        moduleInfo(sender, args[2]);
                    } else {
                        listModules(sender);
                    }
                    break;
                case "commands":
                    if (args.length > 1) {
                        listModuleCommands(sender, args[1]);
                    } else {
                        listAllCommands(sender);
                    }
                    break;
                case "enable":
                    if (args.length > 1) {
                    enableModule(sender, args[1]);
                    } else {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Du musst ein Modul angeben!"));
                    }
                    break;
                case "disable":
                    if (args.length > 1) {
                    disableModule(sender, args[1]);
                    } else {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Du musst ein Modul angeben!"));
                    }
                    break;
                case "reload":
                    if (args.length > 1) {
                        switch (args[1].toLowerCase()) {
                            case "all":
                        reloadAll(sender);
                                break;
                            case "config":
                        reloadConfig(sender);
                                break;
                            case "modules":
                        reloadAllModules(sender);
                                break;
                            case "resources":
                                reloadResources(sender);
                                break;
                            default:
                        reloadModule(sender, args[1]);
                                break;
                        }
                    } else {
                        reloadAll(sender);
                    }
                    break;
                case "debug":
                    toggleDebug(sender);
                    break;
                case "status":
                    showStatus(sender);
                    break;
                case "info":
                    if (args.length > 1) {
                        if (args[1].equalsIgnoreCase("system")) {
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                showSystemInfo(player);
                            } else {
                                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Dieser Befehl kann nur von Spielern ausgeführt werden!"));
                            }
                        } else {
                            moduleInfo(sender, args[1]);
                        }
                    } else {
                        showStatus(sender);
                    }
                    break;
                case "benchmark":
                    if (args.length > 1) {
                        if (args[1].equals("help")) {
                        showBenchmarkHelp(sender);
                        } else {
                    runBenchmark(sender, args);
                        }
                    } else {
                        showBenchmarkHelp(sender);
                    }
                    break;
                case "cache":
                    if (args.length > 1) {
                        if (args[1].equals("help")) {
                        showCacheHelp(sender);
                        } else {
                    handleCacheCommand(sender, args);
                        }
                    } else {
                        showCacheHelp(sender);
                    }
                    break;
                case "perm":
                case "permission":
                case "perms":
                    handlePermissionCommand(sender, args);
                    break;
                default:
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Unbekannter Befehl! Verwende &f/apicore help &cfor help."));
                    break;
            }

            return true;
        });
        
        apiCoreCommand.setTabCompleter((sender, command, alias, args) -> {
            List<String> completions = new ArrayList<>();
            
            if (args.length == 1) {
                List<String> commands = Arrays.asList("help", "modules", "commands", "enable", "disable", 
                                                    "reload", "debug", "status", "info", "benchmark", "cache", "permission");
                
                for (String cmd : commands) {
                    if (cmd.startsWith(args[0].toLowerCase())) {
                        completions.add(cmd);
                    }
                }
                
                return completions;
            }
            
            if (args.length == 2) {
                switch (args[0].toLowerCase()) {
                    case "modules":
                        completions.add("info");
                        break;
                    case "enable":
                    case "disable":
                        // Schlage Module vor, die aktiviert/deaktiviert werden können
                        Map<String, ApiCore.ModuleInfo> modules = apiCore.getLoadedModules();
                        for (String moduleName : modules.keySet()) {
                            if (moduleName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(moduleName);
                    }
                        }
                        break;
                    case "reload":
                        List<String> reloadOptions = Arrays.asList("all", "config", "modules", "resources");
                    for (String option : reloadOptions) {
                            if (option.startsWith(args[1].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                    
                        // Auch Module als Vorschläge anbieten
                        for (String moduleName : apiCore.getLoadedModules().keySet()) {
                            if (moduleName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(moduleName);
                            }
                        }
                        break;
                    case "commands":
                        // Alle Module vorschlagen
                        for (String moduleName : apiCore.getLoadedModules().keySet()) {
                            if (moduleName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(moduleName);
                            }
                        }
                        break;
                    case "benchmark":
                        List<String> benchmarkOptions = Arrays.asList("help", "full", "threads", "cache", 
                                                                    "modules", "io", "results", "compare", "clear");
                        for (String option : benchmarkOptions) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                        }
                    }
                        break;
                    case "cache":
                        List<String> cacheOptions = Arrays.asList("help", "clear", "info");
                        for (String option : cacheOptions) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                    case "info":
                        completions.add("system");
                        
                        // Alle Module vorschlagen
                        for (String moduleName : apiCore.getLoadedModules().keySet()) {
                            if (moduleName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(moduleName);
                            }
                        }
                        break;
                    case "permission":
                    case "perm":
                    case "perms":
                        List<String> permOptions = Arrays.asList("check", "list", "test", "add");
                        for (String option : permOptions) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                        }
                    }
                        break;
                }
                
                return completions;
            }
            
            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("modules") && args[1].equalsIgnoreCase("info")) {
                    // Schlage Module für Info vor
                    for (String moduleName : apiCore.getLoadedModules().keySet()) {
                        if (moduleName.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(moduleName);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("cache") && args[1].equalsIgnoreCase("clear")) {
                    List<String> cacheTypes = Arrays.asList("all", "methods", "permissions", "reflection");
                    for (String type : cacheTypes) {
                        if (type.startsWith(args[2].toLowerCase())) {
                            completions.add(type);
                        }
                    }
                }
                
                // Keine Return-Anweisung, damit auch die spezifischere Prüfung unten ausgeführt wird
            }
            
            // Verwende die dedizierte Tab-Completion für Permission-Befehle
            // Wenn der Befehl perm/permission/perms ist, verwende immer unsere spezialisierte Methode
            if (args.length > 0 && (args[0].equalsIgnoreCase("perm") || args[0].equalsIgnoreCase("permission") || args[0].equalsIgnoreCase("perms"))) {
                return tabCompletePermissionCommand(sender, args);
            }
            
            return completions;
        });
        
        apiCore.getLogger().info("Core-Befehle wurden registriert!");
    }
    
    /**
     * Batch registriert mehrere Befehle
     */
    public void registerCommands(List<DynamicCommand> commands) {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            
            int successCount = 0;
            
            for (DynamicCommand command : commands) {
                try {
                    boolean registered = commandMap.register(command.getModuleName(), command);
                    
                    if (registered) {
                        successCount++;
                        if (apiCore.isDebugMode()) {
                            apiCore.getLogger().info("Befehl registriert: " + command.getName() + " (" + command.getModuleName() + ")");
                        }
                    } else if (command != null) { // Add null check here
                        apiCore.getLogger().warning("Konnte Befehl nicht registrieren: " + command.getName() + " (" + command.getModuleName() + ")");
                    } else {
                        apiCore.getLogger().warning("Konnte Befehl nicht registrieren: null command object");
                    }
                } catch (Exception e) {
                    apiCore.getLogger().log(Level.WARNING, "Fehler beim Registrieren des Befehls " + (command != null ? command.getName() : "null") + ": " + e.getMessage(), e);
                }
            }

            // Command-Map synchronisieren, um sicherzustellen, dass Tab-Completion sofort funktioniert
            apiCore.getModuleManager().synchronizeCommands();
            
            apiCore.getLogger().info(successCount + " von " + commands.size() + " Befehlen erfolgreich registriert");
        } catch (Exception e) {
            apiCore.getLogger().log(Level.SEVERE, "Kritischer Fehler beim Registrieren der Befehle: " + e.getMessage(), e);
        }
    }
    
    /**
     * Entfernt mehrere Befehle in einem Durchgang
     */
    public void unregisterCommands(List<DynamicCommand> commands) {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            
            // Zugriff auf die knownCommands-Map über Reflection
            Field knownCommandsField = null;
            Map<String, org.bukkit.command.Command> knownCommands = null;
            
            try {
                // Versuche, die knownCommands-Map zu bekommen
                // Die genaue Implementierung kann je nach Bukkit-Version variieren
                knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                Map<String, org.bukkit.command.Command> cmdMap = 
                    (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);
                knownCommands = cmdMap;
            } catch (Exception e) {
                apiCore.getLogger().warning("Konnte knownCommands nicht per Reflection zugreifen: " + e.getMessage());
                // Wir machen trotzdem weiter, selbst wenn wir knownCommands nicht bekommen können
            }
            
            // Menge aller zu entfernenden Befehle (für effizientes Lookup)
            Set<String> commandIdentifiers = new HashSet<>();
            
            for (DynamicCommand cmd : commands) {
                try {
                    // Standardmäßigen unregister aufrufen
                    cmd.unregister(commandMap);
                    
                    // Sammle alle möglichen Befehlsbezeichner
                    String cmdName = cmd.getName().toLowerCase();
                    String prefixedCmdName = cmd.getModuleName().toLowerCase() + ":" + cmdName;
                    String minecraftPrefixedCmdName = "minecraft:" + cmdName;
                    
                    commandIdentifiers.add(cmdName);
                    commandIdentifiers.add(prefixedCmdName);
                    commandIdentifiers.add(minecraftPrefixedCmdName);
                    
                    // Sammle auch alle Alias-Kombinationen
                    for (String alias : cmd.getAliases()) {
                        String aliasLower = alias.toLowerCase();
                        commandIdentifiers.add(aliasLower);
                        commandIdentifiers.add(cmd.getModuleName().toLowerCase() + ":" + aliasLower);
                        commandIdentifiers.add("minecraft:" + aliasLower);
                    }
                    
                    // Zurücksetzen des internen Zustands
                    cmd.resetCommand();
                } catch (Exception e) {
                    apiCore.getLogger().warning("Fehler beim Entfernen des Befehls " + cmd.getName() + ": " + e.getMessage());
                }
            }
            
            // Zusätzlich aus der knownCommands-Map entfernen, falls wir Zugriff haben
            if (knownCommands != null) {
                // Erstelle eine Liste aller Schlüssel, die zu entfernen sind
                List<String> keysToRemove = new ArrayList<>();
                
                // Gehe alle Kommandos durch und überprüfe, ob sie entfernt werden sollen
                for (Map.Entry<String, org.bukkit.command.Command> entry : knownCommands.entrySet()) {
                    String key = entry.getKey();
                    org.bukkit.command.Command command = entry.getValue();
                    
                    // Direkte Übereinstimmung mit gespeicherten Identifikatoren
                    if (commandIdentifiers.contains(key)) {
                        keysToRemove.add(key);
                        continue;
                    }
                    
                    // Überprüfe auf Command-Instanz-Gleichheit oder wenn es ein DynamicCommand ist
                    if (command instanceof DynamicCommand) {
                        DynamicCommand dynamicCmd = (DynamicCommand) command;
                        
                        // Überprüfe, ob dieser DynamicCommand zu einem der zu entfernenden gehört
                        for (DynamicCommand cmdToRemove : commands) {
                            if (dynamicCmd.getName().equals(cmdToRemove.getName()) &&
                                dynamicCmd.getModuleName().equals(cmdToRemove.getModuleName())) {
                                keysToRemove.add(key);
                                break;
                            }
                        }
                    }
                }
                
                // Entferne alle identifizierten Schlüssel
                for (String key : keysToRemove) {
                    knownCommands.remove(key);
                }
                
                if (apiCore.isDebugMode() && !keysToRemove.isEmpty()) {
                    apiCore.getLogger().info("Befehle vollständig aus Command-Cache entfernt: " + keysToRemove.size() + " Einträge");
                }
            }
            
            // Versuche den Command-Cache von Bukkit zu aktualisieren (wichtig nach Reload)
            apiCore.getModuleManager().synchronizeCommands();
            
            if (apiCore.isDebugMode()) {
                apiCore.getLogger().info(commands.size() + " Befehle wurden entfernt");
            }
        } catch (Exception e) {
            apiCore.getLogger().log(Level.SEVERE, "Fehler beim Entfernen der Befehle", e);
        }
    }
    
    // Die verschiedenen Befehlsmethoden - Diese würden normal implementiert werden
    // Hier als Platzhalter dargestellt, in der tatsächlichen Implementierung würden sie den Code enthalten
    
    private void showHelp(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        
        // Header
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3ApiCore Befehle #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        // Module Verwaltung
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lModul-Verwaltung:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore list #FFFFFF- #A8A8A8Zeigt alle verfügbaren Module an"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore info <Modul> #FFFFFF- #A8A8A8Zeigt detaillierte Informationen zum Modul"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore enable <Modul> #FFFFFF- #A8A8A8Aktiviert ein Modul"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore disable <Modul> #FFFFFF- #A8A8A8Deaktiviert ein Modul"));
        
        // Befehle-Sektion
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lBefehle:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore commands #FFFFFF- #A8A8A8Zeigt alle verfügbaren Befehle an"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore commands <Modul> #FFFFFF- #A8A8A8Zeigt Befehle eines bestimmten Moduls"));
        
        // System-Sektion
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lSystem:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore reload #FFFFFF- #A8A8A8Lädt alle Module neu"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore reload <Modul> #FFFFFF- #A8A8A8Lädt ein bestimmtes Modul neu"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore debug #FFFFFF- #A8A8A8Schaltet den Debug-Modus ein/aus"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore status #FFFFFF- #A8A8A8Zeigt den aktuellen System-Status an"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark #FFFFFF- #A8A8A8Führt Performance-Tests durch"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore cache #FFFFFF- #A8A8A8Verwaltet den Cache des Systems"));
        
        // Footer
        sender.sendMessage(apiCore.formatHex("#38C6C3┌─────────────────────────┐"));
        sender.sendMessage(apiCore.formatHex("#38C6C3Version: #FFFFFF" + apiCore.getDescription().getVersion()));
    }
    
    private void listModules(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        Map<String, ApiCore.ModuleInfo> loadedModules = apiCore.getLoadedModules();
        List<String> availableModules = apiCore.getModuleManager().getAvailableButNotLoadedModules();
        
        // Header
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3Module Übersicht #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        // Geladene Module anzeigen
        if (!loadedModules.isEmpty()) {
            sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lAktive Module: #4DEEEB[" + loadedModules.size() + "]"));
            
            for (Map.Entry<String, ApiCore.ModuleInfo> entry : loadedModules.entrySet()) {
                ApiCore.ModuleInfo info = entry.getValue();
                String statusSymbol = "#00FF00●"; // Grüner Punkt für aktive Module
                
                sender.sendMessage(apiCore.formatHex("  " + statusSymbol + " #74E8E5" + info.getName() + 
                                                    " #FFFFFF- v" + info.getVersion() + 
                                                    " #A8A8A8- " + info.getDescription()));
            }
        } else {
            sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lAktive Module: #FF5555Keine"));
        }
        
        // Verfügbare aber nicht geladene Module anzeigen
        if (!availableModules.isEmpty()) {
            sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lVerfügbare Module: #4DEEEB[" + availableModules.size() + "]"));
            
            for (String moduleName : availableModules) {
                String statusSymbol = "#FF5555○"; // Roter Kreis für inaktive Module
                sender.sendMessage(apiCore.formatHex("  " + statusSymbol + " #74E8E5" + moduleName + 
                                                    " #A8A8A8- Nicht geladen"));
            }
        }
        
        // Tipp anzeigen
        sender.sendMessage(apiCore.formatHex("\n#A8A8A8Tipp: Nutze #74E8E5/apicore info <Modul> #A8A8A8für mehr Details"));
    }
    
    private void moduleInfo(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        ApiCore.ModuleInfo moduleInfo = apiCore.getModuleInfo(moduleName);
        
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + moduleName + " &cist nicht geladen!"));
            
            // Prüfen, ob das Modul verfügbar aber nicht geladen ist
            List<String> availableModules = apiCore.getModuleManager().getAvailableButNotLoadedModules();
            if (availableModules.contains(moduleName)) {
                sender.sendMessage(apiCore.formatHex(prefix + "&7Das Modul ist verfügbar, aber nicht geladen. Nutze &e/apicore enable " + moduleName + "&7 zum Aktivieren."));
            }
            return;
        }
        
        // Header
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3Modul Details: #4DEEEB" + moduleInfo.getName() + " #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        // Modulinformationen anzeigen
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lAllgemeine Informationen:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Name: #FFFFFF" + moduleInfo.getName()));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Version: #FFFFFF" + moduleInfo.getVersion()));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Beschreibung: #FFFFFF" + moduleInfo.getDescription()));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Status: #00FF00Aktiv"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Datei: #FFFFFF" + moduleInfo.getJarFile().getName()));
        
        // Plugin-Klasse
        sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lTechnische Details:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Hauptklasse: #FFFFFF" + moduleInfo.getInstance().getClass().getName()));
        
        // Befehle zählen
        List<DynamicCommand> commands = apiCore.getModuleManager().getModuleCommands(moduleName);
        int commandCount = commands != null ? commands.size() : 0;
        
        if (commandCount > 0) {
            sender.sendMessage(apiCore.formatHex("  #74E8E5Befehle: #FFFFFF" + commandCount + 
                                           " #A8A8A8(Nutze #74E8E5/apicore commands " + moduleName + "#A8A8A8)"));
        } else {
            sender.sendMessage(apiCore.formatHex("  #74E8E5Befehle: #A8A8A8Keine"));
        }
        
        // Aktionen anzeigen
        sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lAktionen:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore disable " + moduleName + " #FFFFFF- #A8A8A8Modul deaktivieren"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore reload " + moduleName + " #FFFFFF- #A8A8A8Modul neu laden"));
    }
    
    private void listAllCommands(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        Map<String, ApiCore.ModuleInfo> loadedModules = apiCore.getLoadedModules();
        
        // Header
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3Alle Befehle #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        // Core-Befehle anzeigen
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lApiCore:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore #FFFFFF- #A8A8A8Zeigt Hilfe zum ApiCore"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore help #FFFFFF- #A8A8A8Zeigt diese Hilfe"));
        
        // Befehle nach Modulen sortiert anzeigen
        boolean hasCommands = false;
        
        for (String moduleName : loadedModules.keySet()) {
            List<DynamicCommand> commands = apiCore.getModuleManager().getModuleCommands(moduleName);
            
            if (commands != null && !commands.isEmpty()) {
                hasCommands = true;
                sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&l" + moduleName + ":"));
                
                for (DynamicCommand cmd : commands) {
                    String description = cmd.getDescription();
                    if (description == null || description.isEmpty()) {
                        description = "Kein Beschreibung verfügbar";
                    }
                    
                    sender.sendMessage(apiCore.formatHex("  #74E8E5/" + cmd.getName() + " #FFFFFF- #A8A8A8" + description));
                }
            }
        }
        
        if (!hasCommands) {
            sender.sendMessage(apiCore.formatHex("\n#FF5555Keine zusätzlichen Befehle von Modulen gefunden."));
        }
        
        // Footer
        sender.sendMessage(apiCore.formatHex("\n#A8A8A8Tipp: Nutze #74E8E5/apicore commands <Modul> #A8A8A8für Modul-spezifische Befehle"));
    }
    
    private void listModuleCommands(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        ApiCore.ModuleInfo moduleInfo = apiCore.getModuleInfo(moduleName);
        
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + moduleName + " &cist nicht geladen!"));
            return;
        }
        
        List<DynamicCommand> commands = apiCore.getModuleManager().getModuleCommands(moduleName);
        
        // Header
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3Befehle: #4DEEEB" + moduleInfo.getName() + " #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        if (commands == null || commands.isEmpty()) {
            sender.sendMessage(apiCore.formatHex("#FF5555Dieses Modul stellt keine Befehle bereit."));
            return;
        }
        
        // Befehle auflisten
        for (DynamicCommand cmd : commands) {
            // Befehlsname und Beschreibung
            sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&l/" + cmd.getName() + "#FFFFFF:"));
            
            String description = cmd.getDescription();
            if (description != null && !description.isEmpty()) {
                sender.sendMessage(apiCore.formatHex("  #74E8E5Beschreibung: #FFFFFF" + description));
            }
            
            // Verwendung
            String usage = cmd.getUsage();
            if (usage != null && !usage.isEmpty()) {
                sender.sendMessage(apiCore.formatHex("  #74E8E5Verwendung: #FFFFFF" + usage));
            }
            
            // Aliase
            List<String> aliases = cmd.getAliases();
            if (aliases != null && !aliases.isEmpty()) {
                sender.sendMessage(apiCore.formatHex("  #74E8E5Aliase: #FFFFFF" + String.join(", ", aliases)));
            }
            
            // Berechtigung
            String permission = cmd.getPermission();
            if (permission != null && !permission.isEmpty()) {
                sender.sendMessage(apiCore.formatHex("  #74E8E5Berechtigung: #FFFFFF" + permission));
            }
        }
    }
    
    private void enableModule(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        
        // Modulnamen normalisieren (Versionsnummer entfernen, falls vorhanden)
        String normalizedModuleName = normalizeModuleName(moduleName);
        
        // Prüfen, ob das Modul bereits geladen ist
        if (apiCore.getModuleInfo(normalizedModuleName) != null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + normalizedModuleName + "&c ist bereits geladen!"));
            return;
        }
        
        // Prüfen, ob das Modul im Verzeichnis vorhanden ist
        List<String> availableModules = apiCore.getModuleManager().getAvailableButNotLoadedModules();
        
        // Normalisierte verfügbare Module und genauen Match suchen
        String matchedModuleName = null;
        for (String availableModule : availableModules) {
            String normalizedAvailable = normalizeModuleName(availableModule);
            if (normalizedAvailable.equalsIgnoreCase(normalizedModuleName)) {
                matchedModuleName = availableModule;
                break;
            }
        }
        
        if (matchedModuleName == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + normalizedModuleName + "&c wurde nicht gefunden!"));
            
            // Vorschläge für ähnliche Modulnamen anzeigen
            List<String> suggestions = new ArrayList<>();
            for (String available : availableModules) {
                if (available.toLowerCase().contains(normalizedModuleName.toLowerCase())) {
                    suggestions.add(available);
                }
            }
            
            if (!suggestions.isEmpty()) {
                sender.sendMessage(apiCore.formatHex(prefix + "&7Meintest du eines dieser Module?"));
                for (String suggestion : suggestions) {
                    sender.sendMessage(apiCore.formatHex("  &7- &e" + suggestion));
                }
            } else {
                sender.sendMessage(apiCore.formatHex(prefix + "&7Verfügbare Module: &e" + 
                                String.join(", ", availableModules)));
            }
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(prefix + "&7Aktiviere Modul &e" + matchedModuleName + "&7..."));
        
        // JAR-Datei des Moduls finden
        File modulesDir = new File(apiCore.getDataFolder(), "modules");
        File moduleFile = new File(modulesDir, matchedModuleName + ".jar");
        
        if (!moduleFile.exists()) {
            // Versuche die JAR-Datei mit dem ursprünglichen Namen zu finden, falls der normalisierte nicht funktioniert
            moduleFile = new File(modulesDir, moduleName + ".jar");
            if (!moduleFile.exists()) {
                sender.sendMessage(apiCore.formatHex(prefix + "&cModuldatei für &e" + matchedModuleName + "&c wurde nicht gefunden!"));
                return;
            }
        }
        
        try {
            // Modulname aus der JAR-Datei lesen
            String actualModuleName = matchedModuleName;
            try (JarFile jar = new JarFile(moduleFile)) {
                JarEntry moduleYml = jar.getJarEntry("module.yml");
                if (moduleYml != null) {
                    YamlConfiguration moduleConfig = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(jar.getInputStream(moduleYml), StandardCharsets.UTF_8));
                    String nameFromYml = moduleConfig.getString("name");
                    if (nameFromYml != null && !nameFromYml.isEmpty()) {
                        actualModuleName = nameFromYml;
                    }
                }
            } catch (Exception e) {
                apiCore.getLogger().warning("Konnte module.yml in " + moduleFile.getName() + " nicht lesen: " + e.getMessage());
            }
            
            // Konfigurationsdatei erstellen oder aktualisieren
            File configFile = apiCore.getModuleConfigFile(actualModuleName);
            org.bukkit.configuration.file.YamlConfiguration config;
            
            if (configFile.exists()) {
                config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
                // Sicherstellen, dass das Modul aktiviert ist
                config.set("enabled", true);
                config.save(configFile);
            } else {
                // Neue Konfigurationsdatei erstellen
                config = new org.bukkit.configuration.file.YamlConfiguration();
                config.set("enabled", true);
                try {
                    // Sicherstellen, dass das Verzeichnis existiert
                    configFile.getParentFile().mkdirs();
                    config.save(configFile);
                } catch (IOException e) {
                    apiCore.getLogger().warning("Konnte Konfigurationsdatei für " + actualModuleName + " nicht erstellen: " + e.getMessage());
                }
            }
            
            // Debug-Log
            if (apiCore.isDebugMode()) {
                apiCore.getLogger().info("Normalisierter Modulname: " + normalizedModuleName);
                apiCore.getLogger().info("Tatsächlicher Modulname aus module.yml: " + actualModuleName);
                apiCore.getLogger().info("Tatsächlicher JAR-Dateiname: " + moduleFile.getName());
                apiCore.getLogger().info("Konfigurationsdatei: " + configFile.getAbsolutePath() + " (exists: " + configFile.exists() + ")");
            }
            
            // Modul laden
            apiCore.getModuleManager().loadModule(moduleFile);
            
            // Erfolg melden
            ApiCore.ModuleInfo info = apiCore.getModuleInfo(actualModuleName);
            if (info != null) {
                sender.sendMessage(apiCore.formatHex(prefix + "&aModul &e" + info.getName() + " v" + 
                                info.getVersion() + "&a wurde erfolgreich aktiviert!"));
                
                // Stelle sicher, dass Bukkit's Command-System aktualisiert wird
                apiCore.getServer().getScheduler().runTask(apiCore, () -> {
                    // Sync-Task, um Bukkit-Command-System zu aktualisieren
                    try {
                        List<DynamicCommand> commands = apiCore.getModuleManager().getModuleCommands(info.getName());
                        if (commands != null && !commands.isEmpty()) {
                            apiCore.getLogger().info("Synchronisiere Befehle für " + info.getName() + " (" + commands.size() + " Befehle)");
                            // Fordern Sie die Spieler auf, /reload oder /minecraft:reload zu verwenden, falls Befehle nicht sofort arbeiten
                            for (Player player : apiCore.getServer().getOnlinePlayers()) {
                                if (player.isOp() || player.hasPermission("apicore.admin")) {
                                    player.sendMessage(apiCore.formatHex(prefix + "&7Falls Befehle nicht sofort funktionieren, gib &e/minecraft:reload &7ein."));
                                    break; // Nur einen Admin benachrichtigen
                                }
                            }
                        }
                    } catch (Exception e) {
                        apiCore.getLogger().warning("Fehler beim Synchronisieren der Befehle: " + e.getMessage());
                    }
                });
            } else {
                sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + matchedModuleName + 
                                "&c konnte nicht aktiviert werden. Siehe Konsole für Details."));
            }
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Aktivieren des Moduls &e" + 
                            matchedModuleName + "&c: " + e.getMessage()));
            apiCore.getLogger().severe("Fehler beim Aktivieren des Moduls " + matchedModuleName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Normalisiert einen Modulnamen durch Entfernen von Versionsnummern.
     * Zum Beispiel: "MyModule-1.0.0" wird zu "MyModule"
     */
    private String normalizeModuleName(String moduleName) {
        // Typische Versionsmuster entfernen
        return moduleName.replaceAll("-\\d+(\\.\\d+)*(-SNAPSHOT)?", "");
    }
    
    private void disableModule(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        
        // Modulnamen normalisieren
        String normalizedModuleName = normalizeModuleName(moduleName);
        
        // Prüfen, ob das Modul geladen ist - mit normalisiertem Namen
        ApiCore.ModuleInfo moduleInfo = apiCore.getModuleInfo(normalizedModuleName);
        
        // Falls nicht gefunden, versuche mit dem Originalnamen
        if (moduleInfo == null) {
            moduleInfo = apiCore.getModuleInfo(moduleName);
        }
        
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + normalizedModuleName + "&c ist nicht geladen!"));
            return;
        }
        
        // Verwende den tatsächlichen Namen des geladenen Moduls
        String actualModuleName = moduleInfo.getName();
        sender.sendMessage(apiCore.formatHex(prefix + "&7Deaktiviere Modul &e" + actualModuleName + "&7..."));
        
        try {
            // Verwende die zentrale disableModule-Methode in ApiCore
            // Diese Methode kümmert sich um Konfiguration, Entladen des Moduls und Command-Synchronisierung
            boolean success = apiCore.disableModule(actualModuleName);
            
            if (success) {
                // Erfolg melden
                sender.sendMessage(apiCore.formatHex(prefix + "&aModul &e" + actualModuleName + 
                                "&a wurde erfolgreich deaktiviert!"));
            } else {
                sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + actualModuleName + 
                                "&c konnte nicht deaktiviert werden. Details in der Konsole."));
            }
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Deaktivieren des Moduls &e" + 
                            actualModuleName + "&c: " + e.getMessage()));
            apiCore.getLogger().severe("Fehler beim Deaktivieren des Moduls " + actualModuleName + ": " + e.getMessage());
        }
    }
    
    private void reloadAll(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        sender.sendMessage(apiCore.formatHex(prefix + "&7Lade alle Komponenten neu..."));
        
        try {
            // Konfiguration neu laden
            sender.sendMessage(apiCore.formatHex(prefix + "&7Lade Konfiguration neu..."));
            apiCore.reloadConfig();
        
        // Liste der geladenen Module speichern
        List<String> moduleNames = new ArrayList<>(apiCore.getLoadedModules().keySet());
            
            // Verzeichnisse für Module erstellen/prüfen
            sender.sendMessage(apiCore.formatHex(prefix + "&7Erstelle Modul-Datenverzeichnisse..."));
            for (String moduleName : moduleNames) {
                File moduleDataDir = apiCore.getModuleDataFolder(moduleName);
                
                if (!moduleDataDir.exists()) {
                    boolean created = moduleDataDir.mkdirs();
                    if (created) {
                        if (apiCore.isDebugMode()) {
                            apiCore.getLogger().info("Datenverzeichnis für Modul " + moduleName + " erstellt: " + moduleDataDir.getAbsolutePath());
                        }
                    } else {
                        apiCore.getLogger().warning("Konnte Datenverzeichnis für Modul " + moduleName + " nicht erstellen: " + moduleDataDir.getAbsolutePath());
        }
                }
            }
            
            // Module neu laden, falls vorhanden
            if (!moduleNames.isEmpty()) {
                sender.sendMessage(apiCore.formatHex(prefix + "&7Lade alle Module neu..."));
        
        // Zähler für erfolgreich/fehlgeschlagene Reloads
        int success = 0;
        int failed = 0;
        
        // Alle Module neu laden
        for (String moduleName : moduleNames) {
            try {
                // Modul-Info und JAR-Datei speichern
                ApiCore.ModuleInfo info = apiCore.getModuleInfo(moduleName);
                if (info == null) continue;
                
                File jarFile = info.getJarFile();
                
                // Modul entladen
                apiCore.getModuleManager().unloadModule(moduleName);
                
                // Modul neu laden
                apiCore.getModuleManager().loadModule(jarFile);
                
                if (apiCore.getModuleInfo(moduleName) != null) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                apiCore.getLogger().severe("Fehler beim Neuladen des Moduls " + moduleName + ": " + e.getMessage());
            }
        }
        
        // Ergebnis melden
        if (failed == 0) {
            sender.sendMessage(apiCore.formatHex(prefix + "&aAlle Module (" + success + ") wurden erfolgreich neu geladen!"));
        } else {
            sender.sendMessage(apiCore.formatHex(prefix + "&eModule neu geladen: &a" + success + " &eerfolgreich, &c" + 
                            failed + " &efehlgeschlagen"));
                }
            } else {
                sender.sendMessage(apiCore.formatHex(prefix + "&7Keine Module zum Neuladen gefunden."));
            }
            
            // Nach neuen Modulen suchen
            sender.sendMessage(apiCore.formatHex(prefix + "&7Suche nach neuen Modulen..."));
            int newCount = apiCore.getModuleManager().checkForNewModules(sender);
            
            if (newCount > 0) {
                sender.sendMessage(apiCore.formatHex(prefix + "&a" + newCount + " neue Module gefunden und geladen!"));
            } else if (apiCore.isDebugMode()) {
                sender.sendMessage(apiCore.formatHex(prefix + "&7Keine neuen Module gefunden."));
            }
            
            // Modulressourcen extrahieren
            sender.sendMessage(apiCore.formatHex(prefix + "&7Extrahiere Modulressourcen..."));
            try {
                apiCore.extractModuleResources();
                sender.sendMessage(apiCore.formatHex(prefix + "&aModulressourcen wurden extrahiert. Bestehende Dateien wurden nicht überschrieben."));
            } catch (Exception e) {
                sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Extrahieren der Modulressourcen: " + e.getMessage()));
                apiCore.getLogger().log(Level.WARNING, "Fehler beim Extrahieren der Modulressourcen", e);
            }
            
            // Synchronisiere Befehle
            try {
                apiCore.getModuleManager().synchronizeCommands();
                sender.sendMessage(apiCore.formatHex(prefix + "&aBefehle wurden synchronisiert."));
            } catch (Exception e) {
                apiCore.getLogger().log(Level.WARNING, "Fehler bei der Befehl-Synchronisierung", e);
                sender.sendMessage(apiCore.formatHex(prefix + "&cFehler bei der Befehl-Synchronisierung: " + e.getMessage()));
            }
            
            // Caches leeren (optional)
            if (apiCore.getConfig().getBoolean("performance.auto-clear-cache-on-reload", false)) {
                sender.sendMessage(apiCore.formatHex(prefix + "&7Leere Caches..."));
                clearPermissionCache();
                clearMethodCache();
                clearReflectionCache();
                sender.sendMessage(apiCore.formatHex(prefix + "&aCaches wurden geleert."));
            }
            
            sender.sendMessage(apiCore.formatHex(prefix + "&aApiCore wurde vollständig neu geladen!"));
            
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim vollständigen Neuladen von ApiCore: " + e.getMessage()));
            apiCore.getLogger().log(Level.SEVERE, "Fehler beim Neuladen von ApiCore", e);
            
            // Versuche Minecraft-Befehle zu synchronisieren als Notfallmaßnahme
            sender.sendMessage(apiCore.formatHex(prefix + "&eVersuche, Befehle zu reparieren..."));
            try {
                apiCore.getModuleManager().synchronizeCommands();
                sender.sendMessage(apiCore.formatHex(prefix + "&aBefehle wurden synchronisiert."));
            } catch (Exception ex) {
                sender.sendMessage(apiCore.formatHex(prefix + "&cKritischer Fehler bei der Befehl-Synchronisierung. Server-Neustart empfohlen."));
            }
        }
    }
    
    private void reloadModule(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        
        // Prüfen, ob das Modul geladen ist
        ApiCore.ModuleInfo moduleInfo = apiCore.getModuleInfo(moduleName);
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + moduleName + "&c ist nicht geladen!"));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(prefix + "&7Lade Modul &e" + moduleName + "&7 neu..."));
        
        try {
            // JAR-Datei des Moduls speichern
            File jarFile = moduleInfo.getJarFile();
            
            // Modul entladen
            apiCore.getModuleManager().unloadModule(moduleName);
            
            // Modul neu laden
            apiCore.getModuleManager().loadModule(jarFile);
            
            // Erfolg prüfen
            ApiCore.ModuleInfo newInfo = apiCore.getModuleInfo(moduleName);
            if (newInfo != null) {
                sender.sendMessage(apiCore.formatHex(prefix + "&aModul &e" + moduleName + 
                                " v" + newInfo.getVersion() + "&a wurde erfolgreich neu geladen!"));
            } else {
                sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + moduleName + 
                                "&c konnte nicht neu geladen werden. Siehe Konsole für Details."));
            }
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Neuladen des Moduls &e" + 
                            moduleName + "&c: " + e.getMessage()));
            apiCore.getLogger().severe("Fehler beim Neuladen des Moduls " + moduleName + ": " + e.getMessage());
        }
    }
    
    private void toggleDebug(CommandSender sender) {
        boolean currentDebugMode = apiCore.isDebugMode();
        
        // Debug-Modus umschalten
        // (Diese Methode ist nur ein Platzhalter - in einer vollständigen Implementierung 
        // würde man die Konfiguration aktualisieren und speichern)
        
        String prefix = apiCore.getMessagePrefix();
        sender.sendMessage(apiCore.formatHex(prefix + 
                    (currentDebugMode ? "#FF5555Debug-Modus wurde deaktiviert" : "#00FF00Debug-Modus wurde aktiviert")));
        
        // Zeige Debugging-Informationen, wenn der Debug-Modus aktiviert ist oder wird
        if (sender instanceof Player) {
            Player player = (Player)sender;
            
            sender.sendMessage(apiCore.formatHex(prefix + "#74E8E5Debug-Information:"));
            sender.sendMessage(apiCore.formatHex("#FFFFFF- Spieler: #74E8E5" + player.getName()));
            sender.sendMessage(apiCore.formatHex("#FFFFFF- UUID: #74E8E5" + player.getUniqueId()));
            sender.sendMessage(apiCore.formatHex("#FFFFFF- OP: #74E8E5" + player.isOp()));
            
            // Prüfen wichtiger Berechtigungen
            String[] keyPermissions = {
                "apicore.admin", 
                "apicore.admin.*", 
                "apicore.commands", 
                "apicore.debug", 
                "apicore.status"
            };
            
            sender.sendMessage(apiCore.formatHex("#FFFFFF- Berechtigungen:"));
            for (String perm : keyPermissions) {
                boolean hasPerm = player.hasPermission(perm);
                boolean hasPermApiCore = apiCore.hasPermission(player, perm);
                sender.sendMessage(apiCore.formatHex("  #FFFFFF" + perm + ": #74E8E5" + hasPerm + 
                        " #FFFFFF(ApiCore: #74E8E5" + hasPermApiCore + "#FFFFFF)"));
            }
            
            // Zeige alle Befehle
            sender.sendMessage(apiCore.formatHex("#FFFFFF- Registrierte Befehle:"));
            for (String moduleName : apiCore.getLoadedModules().keySet()) {
                List<DynamicCommand> commands = apiCore.getModuleManager().getModuleCommands(moduleName);
                if (commands != null && !commands.isEmpty()) {
                    for (DynamicCommand cmd : commands) {
                        boolean hasCommandPerm = apiCore.hasPermission(player, cmd.getPermission());
                        sender.sendMessage(apiCore.formatHex("  #FFFFFF/" + cmd.getName() + 
                                " (#74E8E5" + cmd.getPermission() + "#FFFFFF): #74E8E5" + hasCommandPerm));
                    }
                }
            }
        }
    }
    
    private void showStatus(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        Map<String, ApiCore.ModuleInfo> loadedModules = apiCore.getLoadedModules();
        
        // Header
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3ApiCore Status #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        // Plugin-Version
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lSystem-Information:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Version: #FFFFFF" + apiCore.getDescription().getVersion()));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Debug-Modus: #FFFFFF" + 
                (apiCore.isDebugMode() ? "#00FF00Aktiv" : "#FF5555Inaktiv")));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Permissions-System: #FFFFFF" + 
                (apiCore.isPermissionsHooked() ? "#00FF00Verbunden" : "#FF5555Nicht verbunden")));
        
        // Modul-Statistiken
        sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lModul-Übersicht:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Geladene Module: #FFFFFF" + loadedModules.size()));
        
        // Detail-Tabelle für Module
        sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lModul-Status:"));
        for (Map.Entry<String, ApiCore.ModuleInfo> entry : loadedModules.entrySet()) {
            ApiCore.ModuleInfo info = entry.getValue();
            String statusSymbol = "#00FF00●"; // Grüner Punkt für aktive Module
            
            sender.sendMessage(apiCore.formatHex("  " + statusSymbol + " #74E8E5" + info.getName() + 
                                                " #FFFFFF| v" + info.getVersion()));
            
            // Befehle zählen
            List<DynamicCommand> commands = apiCore.getModuleManager().getModuleCommands(info.getName());
            int commandCount = commands != null ? commands.size() : 0;
            
            if (commandCount > 0) {
                sender.sendMessage(apiCore.formatHex("    #FFFFFF- Befehle: #74E8E5" + commandCount));
            }
        }
        
        // Performance-Daten anzeigen, wenn Performance-Monitoring aktiviert ist
        if (apiCore.getConfig().getBoolean("performance.monitoring.enabled", false)) {
            sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lPerformance-Übersicht:"));
            
            Map<String, ModulePerformanceData> performanceData = apiCore.getPerformanceMonitor().getPerformanceData();
            if (performanceData.isEmpty()) {
                sender.sendMessage(apiCore.formatHex("  #FF5555Keine Performance-Daten verfügbar"));
            } else {
                // Top 3 Module nach Ausführungszeit anzeigen
                List<ModulePerformanceData> sortedModules = new ArrayList<>(performanceData.values());
                sortedModules.sort((a, b) -> Double.compare(b.getAverageExecutionTime(), a.getAverageExecutionTime()));
                
                for (int i = 0; i < Math.min(3, sortedModules.size()); i++) {
                    ModulePerformanceData data = sortedModules.get(i);
                    if (data.getInvocationCount() > 0) {
                        String color = data.getAverageExecutionTime() > 50 ? "#FF5555" : 
                                      (data.getAverageExecutionTime() > 20 ? "#FFAA00" : "#00FF00");
                        
                        sender.sendMessage(apiCore.formatHex("  #FFFFFF" + (i+1) + ". #74E8E5" + data.getModuleName() + 
                                          " #FFFFFF- Durchschnitt: " + color + String.format("%.2f", data.getAverageExecutionTime()) + 
                                          "ms #FFFFFF- Aufrufe: #74E8E5" + data.getInvocationCount()));
                    }
                }
                
                sender.sendMessage(apiCore.formatHex("  #A8A8A8Hinweis: Vollständige Daten werden in der Konsole angezeigt"));
            }
        }
        
        // Speichernutzung
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        
        sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lSpeichernutzung:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Genutzt: #FFFFFF" + usedMemory + " MB / " + totalMemory + " MB"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Maximal: #FFFFFF" + maxMemory + " MB"));
        
        // Cache-Statistiken
        int permCacheSize = apiCore.getPermissionCacheSize();
        
        sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lCache-Statistiken:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5Permission-Cache: #FFFFFF" + permCacheSize + " Einträge"));
    }

    /**
     * Zeigt die Hilfe für Benchmark-Befehle
     */
    private void showBenchmarkHelp(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3Benchmark-Befehle #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lVerfügbare Benchmarks:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark full #FFFFFF- #A8A8A8Führt einen vollständigen Benchmark durch"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark threads #FFFFFF- #A8A8A8Testet nur den Thread-Pool"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark cache #FFFFFF- #A8A8A8Testet nur das Caching-System"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark modules #FFFFFF- #A8A8A8Testet nur das Modul-Laden"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark io #FFFFFF- #A8A8A8Testet nur I/O-Operationen"));
        
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lErgebnisse:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark results #FFFFFF- #A8A8A8Zeigt verfügbare Benchmark-Ergebnisse"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark results <Datei> #FFFFFF- #A8A8A8Zeigt detaillierte Ergebnisse"));
        
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lVergleiche:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark compare <Datei1> <Datei2> #FFFFFF- #A8A8A8Vergleicht zwei Benchmark-Ergebnisse"));
        
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lVerwaltung:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore benchmark clear #FFFFFF- #A8A8A8Löscht alle Benchmark- und Performance-Monitoring-Dateien"));
        
        sender.sendMessage(apiCore.formatHex("#38C6C3┌─────────────────────────┐"));
        sender.sendMessage(apiCore.formatHex("#38C6C3Benchmark-Ergebnisse werden in plugins/EssentialsCore/benchmarks/ gespeichert"));
    }
    
    /**
     * Führt einen Benchmark aus
     */
    private void runBenchmark(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!apiCore.getPermissionManager().hasPermission(player, "apicore.admin.system.benchmark")) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Du hast keine Berechtigung, Benchmarks auszuführen!"));
                return;
            }
        } else if (!sender.hasPermission("apicore.admin.system.benchmark")) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Du hast keine Berechtigung, Benchmarks auszuführen!"));
            return;
        }
        
        String type = args[1].toLowerCase();
        
        // Asynchron ausführen, um den Hauptthread nicht zu blockieren
        if (type.equals("full") || type.equals("threads") || type.equals("cache") || 
            type.equals("modules") || type.equals("io")) {
            
            // Informiere den Sender über den Start des Benchmarks
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "#4DEEEBStarte Benchmark: #FFFFFF" + type + " #4DEEEB- Bitte warten..."));
            
            Bukkit.getScheduler().runTaskAsynchronously(apiCore, () -> {
                Map<String, Object> results;
                
                try {
                    switch (type) {
                        case "full":
                            results = apiCore.getPerformanceBenchmark().runFullBenchmark();
                            break;
                        case "threads":
                            results = apiCore.getPerformanceBenchmark().benchmarkThreadPool();
                            break;
                        case "cache":
                            results = apiCore.getPerformanceBenchmark().benchmarkCache();
                            break;
                        case "modules":
                            results = apiCore.getPerformanceBenchmark().benchmarkModuleLoading();
                            break;
                        case "io":
                            results = apiCore.getPerformanceBenchmark().benchmarkIO();
                            break;
                        default:
                            return;
                    }
                    
                    // Benchmark abgeschlossen
                    Bukkit.getScheduler().runTask(apiCore, () -> {
                        if (results.containsKey("error")) {
                            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                    "#FF5555Fehler beim Benchmark: #FFFFFF" + results.get("error")));
                            return;
                        }
                        
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                "#4DEEEBBenchmark abgeschlossen! #FFFFFFErgebnisse wurden gespeichert."));
                        
                        // Zeige einige wichtige Ergebnisse direkt an
                        if (type.equals("threads")) {
                            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                    "#4DEEEBEinfache Aufgaben/Sekunde: #FFFFFF" + 
                                    String.format("%.2f", (double) results.getOrDefault("tasks_per_second", 0.0))));
                        } else if (type.equals("cache")) {
                            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                    "#4DEEEBCache-Zugriffe/Sekunde: #FFFFFF" + 
                                    String.format("%.2f", (double) results.getOrDefault("method_cache_accesses_per_second", 0.0))));
                        }
                        
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                "#4DEEEBVerwende #FFFFFF/apicore benchmark results #4DEEEBfür detaillierte Ergebnisse."));
                    });
                } catch (Exception e) {
                    // Fehlerbehandlung
                    Bukkit.getScheduler().runTask(apiCore, () -> {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                "#FF5555Benchmark fehlgeschlagen: #FFFFFF" + e.getMessage()));
                        apiCore.getLogger().warning("Benchmark fehlgeschlagen: " + e.getMessage());
                        e.printStackTrace();
                    });
                }
            });
        } else if (type.equals("results")) {
            if (args.length == 2) {
                // Liste der verfügbaren Ergebnisse anzeigen
                List<String> results = apiCore.getPerformanceBenchmark().getAvailableResults();
                if (results.isEmpty()) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                            "#FF5555Keine Benchmark-Ergebnisse gefunden!"));
                    return;
                }
                
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "#4DEEEB◆ #38C6C3Verfügbare Benchmark-Ergebnisse #4DEEEB◆"));
                sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
                
                for (String result : results) {
                    String color = result.startsWith("benchmark_") ? "#4DEEEB" : "#74E8E5";
                    sender.sendMessage(apiCore.formatHex(color + "» #FFFFFF" + result));
                }
                
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "#4DEEEBVerwende #FFFFFF/apicore benchmark results <Datei> #4DEEEBfür Details."));
            } else {
                // Detaillierte Ergebnisse anzeigen
                String filename = args[2];
                List<String> lines = apiCore.getPerformanceBenchmark().getFormattedResults(filename);
                
                // Titel anzeigen
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "#4DEEEB◆ #38C6C3Detaillierte Benchmark-Ergebnisse #4DEEEB◆"));
                sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
                
                // Ergebnisse senden
                for (String line : lines) {
                    sender.sendMessage(apiCore.formatHex(line));
                }
                
                // Hinweis hinzufügen, wenn es sich um eine Vergleichsdatei handelt
                if (filename.startsWith("comparison_")) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                            "#4DEEEBHinweis: #FFFFFFDie Vergleichsergebnisse wurden kompakter gestaltet."));
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                            "#4DEEEBFür vollständige Details überprüfe die YAML-Datei im Benchmarks-Ordner."));
                }
            }
        } else if (type.equals("compare") && args.length >= 4) {
            // Vergleiche zwei Benchmark-Ergebnisse
            String file1 = args[2];
            String file2 = args[3];
            
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "#4DEEEBVergleiche Benchmarks: #FFFFFF" + file1 + " #4DEEEBund #FFFFFF" + file2));
            
            Bukkit.getScheduler().runTaskAsynchronously(apiCore, () -> {
                Map<String, Object> results = apiCore.getPerformanceBenchmark().compareConfigurations(file1, file2);
                
                Bukkit.getScheduler().runTask(apiCore, () -> {
                    if (results.containsKey("error")) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                "#FF5555Fehler beim Vergleich: #FFFFFF" + results.get("error")));
                        return;
                    }
                    
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                            "#4DEEEBVergleich abgeschlossen! #FFFFFFErgebnisse wurden gespeichert."));
                    
                    // Debug-Ausgabe für die Anzahl der gefundenen Unterschiede
                    int differenceCount = 0;
                    if (results.containsKey("difference_count")) {
                        differenceCount = (int) results.get("difference_count");
                    } else if (results.containsKey("differences")) {
                        Object differencesObj = results.get("differences");
                        if (differencesObj instanceof Map) {
                            differenceCount = ((Map<?, ?>) differencesObj).size();
                        }
                    }
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                            "#4DEEEBAnzahl der Unterschiede: #FFFFFF" + differenceCount));
                    
                    // Zeige die neuesten Vergleichsergebnisse
                    List<String> availableResults = apiCore.getPerformanceBenchmark().getAvailableResults();
                    boolean resultsShown = false;
                    
                    for (String result : availableResults) {
                        if (result.startsWith("comparison_")) {
                            List<String> lines = apiCore.getPerformanceBenchmark().getFormattedResults(result);
                            for (String line : lines) {
                                sender.sendMessage(apiCore.formatHex(line));
                            }
                            resultsShown = true;
                            break;
                        }
                    }
                    
                    // Wenn keine Ergebnisse angezeigt wurden, dem Benutzer einen Hinweis geben
                    if (!resultsShown) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                "#FF5555Keine Vergleichsergebnisse gefunden! Versuche den Befehl erneut."));
                    }
                });
            });
        } else if (type.equals("clear")) {
            // Lösche alle Benchmark-Dateien
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "#4DEEEBLösche alle Benchmark- und Performance-Monitoring-Dateien..."));
            
            Bukkit.getScheduler().runTaskAsynchronously(apiCore, () -> {
                int count = apiCore.getPerformanceBenchmark().clearAllBenchmarkFiles();
                
                Bukkit.getScheduler().runTask(apiCore, () -> {
                    if (count > 0) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                "#4DEEEB" + count + " #FFFFFFDateien wurden erfolgreich gelöscht."));
                    } else {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                "#FFFFFFKeine Dateien zum Löschen gefunden."));
                    }
                });
            });
        } else {
            showBenchmarkHelp(sender);
        }
    }

    private void reloadConfig(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        sender.sendMessage(apiCore.formatHex(prefix + "&7Lade Konfiguration neu..."));
        
        try {
            // Konfiguration neu laden
            apiCore.reloadConfig();
            
            // In der loadConfiguration()-Methode von ApiCore wird der ConfigManager bereits aktualisiert
            sender.sendMessage(apiCore.formatHex(prefix + "&aKonfiguration wurde erfolgreich neu geladen!"));
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Neuladen der Konfiguration: " + e.getMessage()));
            apiCore.getLogger().severe("Fehler beim Neuladen der Konfiguration: " + e.getMessage());
        }
    }
    
    private void reloadAllModules(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        sender.sendMessage(apiCore.formatHex(prefix + "&7Lade alle Module neu..."));
        
        // Liste der geladenen Module speichern
        List<String> moduleNames = new ArrayList<>(apiCore.getLoadedModules().keySet());
        if (moduleNames.isEmpty()) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cEs sind keine Module geladen!"));
            return;
        }
        
        // Zähler für erfolgreich/fehlgeschlagene Reloads
        int success = 0;
        int failed = 0;
        
        // Alle Module neu laden
        for (String moduleName : moduleNames) {
            try {
                // Modul-Info und JAR-Datei speichern
                ApiCore.ModuleInfo info = apiCore.getModuleInfo(moduleName);
                if (info == null) continue;
                
                File jarFile = info.getJarFile();
                
                // Modul entladen
                apiCore.getModuleManager().unloadModule(moduleName);
                
                // Modul neu laden
                apiCore.getModuleManager().loadModule(jarFile);
                
                if (apiCore.getModuleInfo(moduleName) != null) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                apiCore.getLogger().severe("Fehler beim Neuladen des Moduls " + moduleName + ": " + e.getMessage());
            }
        }
        
        // Ergebnis melden
        if (failed == 0) {
            sender.sendMessage(apiCore.formatHex(prefix + "&aAlle Module (" + success + ") wurden erfolgreich neu geladen!"));
        } else {
            sender.sendMessage(apiCore.formatHex(prefix + "&eModule neu geladen: &a" + success + " &eerfolgreich, &c" + 
                            failed + " &efehlgeschlagen"));
            sender.sendMessage(apiCore.formatHex(prefix + "&7Details wurden in der Konsole protokolliert."));
        }
    }

    private void showCacheHelp(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3Cache-Befehle #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lVerfügbare Befehle:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore cache clear all #FFFFFF- #A8A8A8Leert alle Caches"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore cache clear methods #FFFFFF- #A8A8A8Leert nur den Methoden-Cache"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore cache clear permissions #FFFFFF- #A8A8A8Leert nur den Berechtigungs-Cache"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore cache clear reflection #FFFFFF- #A8A8A8Leert nur den Reflection-Cache"));
        
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lInfos:"));
        sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore cache info #FFFFFF- #A8A8A8Zeigt Informationen über den aktuellen Cache-Status"));
        
        sender.sendMessage(apiCore.formatHex("#38C6C3┌─────────────────────────┐"));
        sender.sendMessage(apiCore.formatHex("#A8A8A8Cache leeren kann bei Speicherproblemen helfen oder wenn Änderungen nicht übernommen werden."));
    }
    
    private void handleCacheCommand(CommandSender sender, String[] args) {
        String prefix = apiCore.getMessagePrefix();
        
        // Berechtigungsprüfung
        if (!sender.hasPermission("apicore.admin.system.cache")) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDu hast keine Berechtigung, den Cache zu verwalten!"));
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        try {
            if (subCommand.equals("clear")) {
                if (args.length < 3) {
                    sender.sendMessage(apiCore.formatHex(prefix + "&cBitte gib an, welcher Cache geleert werden soll (all, methods, permissions, reflection)"));
                    return;
                }
                
                String cacheType = args[2].toLowerCase();
                
                switch (cacheType) {
                    case "all":
                        // Alle Caches leeren
                        int methodCount = clearMethodCache();
                        int permCount = clearPermissionCache();
                        int reflectionCount = clearReflectionCache();
                        
                        sender.sendMessage(apiCore.formatHex(prefix + "&aAlle Caches wurden geleert!"));
                        sender.sendMessage(apiCore.formatHex(prefix + "&7Methoden-Cache: &e" + methodCount + " &7Einträge entfernt"));
                        sender.sendMessage(apiCore.formatHex(prefix + "&7Berechtigungs-Cache: &e" + permCount + " &7Einträge entfernt"));
                        sender.sendMessage(apiCore.formatHex(prefix + "&7Reflection-Cache: &e" + reflectionCount + " &7Einträge entfernt"));
                        break;
                    case "methods":
                        // Nur Methoden-Cache leeren
                        int count = clearMethodCache();
                        sender.sendMessage(apiCore.formatHex(prefix + "&aMethoden-Cache wurde geleert! &e" + count + " &aEinträge entfernt."));
                        break;
                    case "permissions":
                        // Nur Permissions-Cache leeren
                        count = clearPermissionCache();
                        sender.sendMessage(apiCore.formatHex(prefix + "&aBerechtigungs-Cache wurde geleert! &e" + count + " &aEinträge entfernt."));
                        break;
                    case "reflection":
                        // Nur Reflection-Cache leeren
                        count = clearReflectionCache();
                        sender.sendMessage(apiCore.formatHex(prefix + "&aReflection-Cache wurde geleert! &e" + count + " &aEinträge entfernt."));
                        break;
                    default:
                        sender.sendMessage(apiCore.formatHex(prefix + "&cUnbekannter Cache-Typ: &e" + cacheType));
                        sender.sendMessage(apiCore.formatHex(prefix + "&7Verfügbare Typen: &eall, methods, permissions, reflection"));
                        break;
                }
            } else if (subCommand.equals("info")) {
                // Cache-Informationen anzeigen
                showCacheInfo(sender);
            } else {
                showCacheHelp(sender);
            }
        } catch (Exception e) {
            // Fehlerbehandlung, um sicherzustellen, dass der Server nicht hängenbleibt
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler bei der Cache-Verwaltung: " + e.getMessage()));
            apiCore.getLogger().log(Level.WARNING, "Fehler bei der Cache-Verwaltung", e);
        }
    }
    
    private int clearMethodCache() {
        try {
            // Prüfen, ob methodCache existiert
            java.lang.reflect.Field field = null;
            try {
                field = ApiCore.class.getDeclaredField("methodCache");
                field.setAccessible(true);
            } catch (Exception e) {
                apiCore.getLogger().warning("Methoden-Cache-Feld nicht gefunden: " + e.getMessage());
                return 0;
            }
            
            Object obj = field.get(apiCore);
            if (obj == null) {
                apiCore.getLogger().warning("Methoden-Cache ist null");
                return 0;
            }
            
            if (!(obj instanceof Map)) {
                apiCore.getLogger().warning("Methoden-Cache ist kein Map-Objekt");
                return 0;
            }
            
            Map<?, ?> cache = (Map<?, ?>) obj;
            int size = cache.size();
            cache.clear();
            
            return size;
        } catch (Exception e) {
            apiCore.getLogger().log(Level.WARNING, "Fehler beim Leeren des Methoden-Cache", e);
            return 0;
        }
    }
    
    private int clearPermissionCache() {
        try {
            // Prüfen, ob permissionExactCache existiert
            java.lang.reflect.Field field = null;
            int size = 0;
            
            try {
                field = ApiCore.class.getDeclaredField("permissionExactCache");
                field.setAccessible(true);
                
                Object obj = field.get(apiCore);
                if (obj != null && obj instanceof Map) {
                    Map<?, ?> cache = (Map<?, ?>) obj;
                    size = cache.size();
                    cache.clear();
                }
            } catch (Exception e) {
                apiCore.getLogger().warning("Berechtigungs-Cache-Feld nicht gefunden: " + e.getMessage());
            }
            
            // Versuchen, permissionCache (BloomFilter) zurückzusetzen
            try {
                field = ApiCore.class.getDeclaredField("permissionCache");
                field.setAccessible(true);
                
                Object bloomFilter = field.get(apiCore);
                if (bloomFilter != null) {
                    // Versuchen, die clear-Methode aufzurufen, wenn sie existiert
                    try {
                        Method clearMethod = bloomFilter.getClass().getMethod("clear");
                        clearMethod.invoke(bloomFilter);
                    } catch (NoSuchMethodException e) {
                        // Falls clear nicht verfügbar, ignorieren
                        apiCore.getLogger().info("BloomFilter hat keine clear-Methode, wird ignoriert");
                    }
                }
            } catch (Exception e) {
                // Ignorieren, falls permissionCache nicht existiert
                apiCore.getLogger().warning("BloomFilter-Cache konnte nicht zurückgesetzt werden: " + e.getMessage());
            }
            
            return size;
        } catch (Exception e) {
            apiCore.getLogger().log(Level.WARNING, "Fehler beim Leeren des Berechtigungs-Cache", e);
            return 0;
        }
    }
    
    private int clearReflectionCache() {
        try {
            // Prüfen, ob methodHandleCache existiert
            java.lang.reflect.Field field = null;
            try {
                field = ApiCore.class.getDeclaredField("methodHandleCache");
                field.setAccessible(true);
            } catch (Exception e) {
                apiCore.getLogger().warning("Reflection-Cache-Feld nicht gefunden: " + e.getMessage());
                return 0;
            }
            
            Object obj = field.get(apiCore);
            if (obj == null) {
                apiCore.getLogger().warning("Reflection-Cache ist null");
                return 0;
            }
            
            if (!(obj instanceof Map)) {
                apiCore.getLogger().warning("Reflection-Cache ist kein Map-Objekt");
                return 0;
            }
            
            Map<?, ?> cache = (Map<?, ?>) obj;
            int size = cache.size();
            cache.clear();
            
            return size;
        } catch (Exception e) {
            apiCore.getLogger().log(Level.WARNING, "Fehler beim Leeren des Reflection-Cache", e);
            return 0;
        }
    }
    
    private void showCacheInfo(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        
        sender.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3Cache-Informationen #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        try {
            int methodCacheSize = 0;
            int reflectionCacheSize = 0;
            int permCacheSize = 0;
            
            // Methodencache
            try {
                java.lang.reflect.Field methodField = ApiCore.class.getDeclaredField("methodCache");
                methodField.setAccessible(true);
                Object methodObj = methodField.get(apiCore);
                if (methodObj instanceof Map) {
                    Map<?, ?> methodCache = (Map<?, ?>) methodObj;
                    methodCacheSize = methodCache.size();
                }
            } catch (Exception e) {
                sender.sendMessage(apiCore.formatHex("#FF5555Methoden-Cache nicht verfügbar: " + e.getMessage()));
            }
            
            // Reflectioncache
            try {
                java.lang.reflect.Field reflectionField = ApiCore.class.getDeclaredField("methodHandleCache");
                reflectionField.setAccessible(true);
                Object reflectionObj = reflectionField.get(apiCore);
                if (reflectionObj instanceof Map) {
                    Map<?, ?> reflectionCache = (Map<?, ?>) reflectionObj;
                    reflectionCacheSize = reflectionCache.size();
                }
            } catch (Exception e) {
                sender.sendMessage(apiCore.formatHex("#FF5555Reflection-Cache nicht verfügbar: " + e.getMessage()));
            }
            
            // Permissioncache
            try {
                java.lang.reflect.Field permField = ApiCore.class.getDeclaredField("permissionExactCache");
                permField.setAccessible(true);
                Object permObj = permField.get(apiCore);
                if (permObj instanceof Map) {
                    Map<?, ?> permCache = (Map<?, ?>) permObj;
                    permCacheSize = permCache.size();
                }
            } catch (Exception e) {
                sender.sendMessage(apiCore.formatHex("#FF5555Berechtigungs-Cache nicht verfügbar: " + e.getMessage()));
            }
            
            // Cache-Größen aus Konfiguration lesen
            int maxMethodCache = apiCore.getConfig().getInt("performance.cache-size.methods", 500);
            int maxReflectionCache = apiCore.getConfig().getInt("performance.cache-size.reflection", 200);
            int maxPermCache = apiCore.getConfig().getInt("performance.cache-size.permissions", 2048);
            
            // Methoden-Cache
            double methodCachePercentage = maxMethodCache > 0 ? methodCacheSize * 100.0 / maxMethodCache : 0;
            String methodColor = methodCachePercentage < 70 ? "#00FF00" : (methodCachePercentage < 90 ? "#FFAA00" : "#FF5555");
            
            sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lMethoden-Cache:"));
            sender.sendMessage(apiCore.formatHex("  #74E8E5Einträge: " + methodColor + methodCacheSize + " #FFFFFF/ #74E8E5" + maxMethodCache));
            sender.sendMessage(apiCore.formatHex("  #74E8E5Auslastung: " + methodColor + String.format("%.1f", methodCachePercentage) + "%"));
            
            // Reflection-Cache
            double reflectionCachePercentage = maxReflectionCache > 0 ? reflectionCacheSize * 100.0 / maxReflectionCache : 0;
            String reflectionColor = reflectionCachePercentage < 70 ? "#00FF00" : (reflectionCachePercentage < 90 ? "#FFAA00" : "#FF5555");
            
            sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lReflection-Cache:"));
            sender.sendMessage(apiCore.formatHex("  #74E8E5Einträge: " + reflectionColor + reflectionCacheSize + " #FFFFFF/ #74E8E5" + maxReflectionCache));
            sender.sendMessage(apiCore.formatHex("  #74E8E5Auslastung: " + reflectionColor + String.format("%.1f", reflectionCachePercentage) + "%"));
            
            // Permission-Cache
            double permCachePercentage = maxPermCache > 0 ? permCacheSize * 100.0 / maxPermCache : 0;
            String permColor = permCachePercentage < 70 ? "#00FF00" : (permCachePercentage < 90 ? "#FFAA00" : "#FF5555");
            
            sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lBerechtigungs-Cache:"));
            sender.sendMessage(apiCore.formatHex("  #74E8E5Einträge: " + permColor + permCacheSize + " #FFFFFF/ #74E8E5" + maxPermCache));
            sender.sendMessage(apiCore.formatHex("  #74E8E5Auslastung: " + permColor + String.format("%.1f", permCachePercentage) + "%"));
            
            // Tipps
            sender.sendMessage(apiCore.formatHex("\n#A8A8A8Bei hoher Auslastung (>90%) kann das Leeren des jeweiligen Caches helfen."));
            sender.sendMessage(apiCore.formatHex("#A8A8A8Verwende #74E8E5/apicore cache clear <Typ> #A8A8A8zum Leeren."));
            
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Abrufen der Cache-Informationen: " + e.getMessage()));
            apiCore.getLogger().log(Level.WARNING, "Fehler beim Abrufen der Cache-Informationen", e);
        }
    }

    /**
     * Lädt nur die Modul-Ressourcen neu
     */
    private void reloadResources(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        sender.sendMessage(apiCore.formatHex(prefix + "&7Lade Modulressourcen neu..."));
        
        try {
            // Ressourcen extrahieren
            apiCore.extractModuleResources();
            
            // Erfolg melden
            sender.sendMessage(apiCore.formatHex(prefix + "&aModulressourcen wurden erfolgreich neu geladen! Bestehende Dateien wurden nicht überschrieben."));
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Neuladen der Modulressourcen: " + e.getMessage()));
            apiCore.getLogger().log(Level.SEVERE, "Fehler beim Neuladen der Modulressourcen", e);
        }
    }

    /**
     * Behandelt Befehle zum Testen und Verwalten von Berechtigungen
     */
    private void handlePermissionCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // Zeige Hilfe für Permission-Befehle
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#4DEEEB◆ #38C6C3Berechtigungen #4DEEEB◆"));
            sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
            sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lVerfügbare Befehle:"));
            sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore perm check <Spieler> <Berechtigung> #FFFFFF- #A8A8A8Prüft eine Berechtigung für einen Spieler"));
            sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore perm test <Berechtigung> #FFFFFF- #A8A8A8Testet eine Berechtigung für dich selbst"));
            sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore perm add player <Spieler> <Berechtigung> [true/false] #FFFFFF- #A8A8A8Fügt einem Spieler eine Berechtigung hinzu"));
            sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore perm add group <Gruppe> <Berechtigung> [true/false] #FFFFFF- #A8A8A8Fügt einer Gruppe eine Berechtigung hinzu"));
            sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore perm list groups #FFFFFF- #A8A8A8Zeigt alle verfügbaren Gruppen an"));
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "check":
                // /apicore perm check <player> <permission>
                if (args.length < 4) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Verwendung: /apicore perm check <Spieler> <Berechtigung>"));
                    return;
                }
                
                String playerName = args[2];
                String permission = args[3];
                
                Player target = Bukkit.getPlayer(playerName);
                if (target == null) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Spieler " + playerName + " nicht gefunden oder nicht online!"));
                    return;
                }
                
                // Prüfe die Berechtigung auf verschiedene Arten
                boolean nativeResult = apiCore.getPermissionManager().hasPermission(target, permission);
                boolean apiCoreResult = apiCore.hasPermission(target, permission);
                boolean permManagerResult = apiCore.getPermissionManager().hasPermission(target, permission);
                
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#4DEEEB◆ #38C6C3Berechtigungsprüfung #4DEEEB◆"));
                sender.sendMessage(apiCore.formatHex("#FFFFFF- Spieler: #74E8E5" + target.getName()));
                sender.sendMessage(apiCore.formatHex("#FFFFFF- UUID: #74E8E5" + target.getUniqueId()));
                sender.sendMessage(apiCore.formatHex("#FFFFFF- Berechtigung: #74E8E5" + permission));
                sender.sendMessage(apiCore.formatHex("#FFFFFF- OP: #74E8E5" + target.isOp()));
                sender.sendMessage(apiCore.formatHex("#FFFFFF- Ergebnisse:"));
                sender.sendMessage(apiCore.formatHex("  #FFFFFF- Bukkit: " + (nativeResult ? "#00FF00JA" : "#FF5555NEIN")));
                sender.sendMessage(apiCore.formatHex("  #FFFFFF- ApiCore: " + (apiCoreResult ? "#00FF00JA" : "#FF5555NEIN")));
                sender.sendMessage(apiCore.formatHex("  #FFFFFF- PermManager: " + (permManagerResult ? "#00FF00JA" : "#FF5555NEIN")));
                
                // Prüfe auch Wildcards
                if (!nativeResult && permission.contains(".")) {
                    String[] parts = permission.split("\\.");
                    StringBuilder wildcardBuilder = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++) {
                        wildcardBuilder.append(parts[i]).append(".");
                    }
                    wildcardBuilder.append("*");
                    String wildcardPerm = wildcardBuilder.toString();
                    
                    boolean wildcardResult = target.hasPermission(wildcardPerm);
                    sender.sendMessage(apiCore.formatHex("#FFFFFF- Wildcard-Prüfung:"));
                    sender.sendMessage(apiCore.formatHex("  #FFFFFF- " + wildcardPerm + ": " + (wildcardResult ? "#00FF00JA" : "#FF5555NEIN")));
                }
                
                // Prüfe LuckPerms-Verbindung
                sender.sendMessage(apiCore.formatHex("#FFFFFF- LuckPerms verbunden: " + 
                    (apiCore.isPermissionsHooked() ? "#00FF00JA" : "#FF5555NEIN")));
                break;
                
            case "test":
                // /apicore perm test <permission>
                if (!(sender instanceof Player)) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Dieser Befehl kann nur von Spielern ausgeführt werden!"));
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Verwendung: /apicore perm test <Berechtigung>"));
                    return;
                }
                
                Player player = (Player) sender;
                String permToTest = args[2];
                
                // Rufe den check-Befehl mit dem aktuellen Spieler auf
                handlePermissionCommand(sender, new String[]{"perm", "check", player.getName(), permToTest});
                break;
                
            case "add":
                // Überprüfe Berechtigung für den Add-Befehl
                if (!sender.hasPermission("apicore.admin.permissions")) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Du hast keine Berechtigung, Berechtigungen hinzuzufügen!"));
                    return;
                }
                
                if (args.length < 5) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Verwendung:"));
                    sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore perm add player <Spieler> <Berechtigung> [true/false]"));
                    sender.sendMessage(apiCore.formatHex("  #74E8E5/apicore perm add group <Gruppe> <Berechtigung> [true/false]"));
                    return;
                }
                
                String targetType = args[2].toLowerCase();
                String targetName = args[3];
                String permToAdd = args[4];
                boolean permValue = args.length > 5 ? Boolean.parseBoolean(args[5]) : true;
                
                if (targetType.equals("player")) {
                    // Berechtigungen zu einem Spieler hinzufügen
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer == null) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Spieler " + targetName + " nicht gefunden oder nicht online!"));
                        return;
                    }
                    
                    // Temporärer Parameter
                    boolean isTemporary = args.length > 6 && args[6].equalsIgnoreCase("temp");
                    
                    boolean success = apiCore.getPermissionManager().addPermissionToPlayer(targetPlayer, permToAdd, permValue, isTemporary);
                    if (success) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#00FF00Berechtigung " + permToAdd + 
                                         " wurde Spieler " + targetPlayer.getName() + " hinzugefügt!"));
                    } else {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Fehler beim Hinzufügen der Berechtigung! Siehe Konsole für Details."));
                    }
                } else if (targetType.equals("group")) {
                    // Berechtigungen zu einer Gruppe hinzufügen
                    boolean success = apiCore.getPermissionManager().addPermissionToGroup(targetName, permToAdd, permValue);
                    if (success) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#00FF00Berechtigung " + permToAdd + 
                                         " wurde Gruppe " + targetName + " hinzugefügt!"));
                    } else {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Fehler beim Hinzufügen der Berechtigung! Siehe Konsole für Details."));
                    }
                } else {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Ungültiger Zieltyp! Verwende 'player' oder 'group'."));
                }
                break;
                
            case "list":
                if (args.length < 3) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Verwendung: /apicore perm list groups"));
                    return;
                }
                
                String listType = args[2].toLowerCase();
                
                if (listType.equals("groups")) {
                    // Gruppen auflisten
                    java.util.List<String> groups = apiCore.getPermissionManager().getAvailableGroups();
                    
                    if (groups.isEmpty()) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Keine Gruppen gefunden oder kein Permissions-System verbunden!"));
                        return;
                    }
                    
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#4DEEEB◆ #38C6C3Verfügbare Gruppen #4DEEEB◆"));
                    sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
                    
                    for (String group : groups) {
                        sender.sendMessage(apiCore.formatHex("#74E8E5" + group));
                    }
                } else {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Ungültiger Listen-Typ! Verwende 'groups'."));
                }
                break;
                
            default:
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FF5555Unbekannter Unterbefehl! Verwende &f/apicore perm &cfür Hilfe."));
                break;
        }
    }

    /**
     * Tab-Completion für Permission-Befehle
     */
    private List<String> tabCompletePermissionCommand(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Der subcommand befindet sich an Position args[1]
        String subCommand = args.length > 1 ? args[1].toLowerCase() : "";
        
        if (args.length == 2) {
            // Unterbefehl-Vorschläge - Zweites Argument (perm ...)
            List<String> subCommands = Arrays.asList("check", "test", "add", "list", "help");
            for (String cmd : subCommands) {
                if (cmd.toLowerCase().startsWith(subCommand)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3) {
            // Dritter Parameter für alle Unterbefehle
            switch (subCommand) {
                case "check":
                    // Spielernamen für check
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                    break;
                case "add":
                    // player oder group für add
                    List<String> types = Arrays.asList("player", "group");
                    for (String type : types) {
                        if (type.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(type);
                        }
                    }
                    break;
                case "test":
                    // Berechtigungen für test
                    for (String perm : getCommonPermissions()) {
                        if (perm.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(perm);
                        }
                    }
                    break;
                case "list":
                    // list groups
                    if ("groups".startsWith(args[2].toLowerCase())) {
                        completions.add("groups");
                    }
                    break;
            }
        } else if (args.length == 4) {
            // Vierter Parameter für bestimmte Unterbefehle
            if (subCommand.equals("check")) {
                // Berechtigungen für check
                for (String perm : getCommonPermissions()) {
                    if (perm.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(perm);
                    }
                }
            } else if (subCommand.equals("add")) {
                String targetType = args[2].toLowerCase();
                
                if (targetType.equals("player")) {
                    // Spielernamen für add player
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                } else if (targetType.equals("group")) {
                    // Gruppennamen für add group
                    for (String group : apiCore.getPermissionManager().getAvailableGroups()) {
                        if (group.toLowerCase().startsWith(args[3].toLowerCase())) {
                            completions.add(group);
                        }
                    }
                }
            }
        } else if (args.length == 5) {
            // Fünfter Parameter für bestimmte Unterbefehle
            if (subCommand.equals("add")) {
                // Berechtigungen für add player/group <name>
                for (String perm : getCommonPermissions()) {
                    if (perm.toLowerCase().startsWith(args[4].toLowerCase())) {
                        completions.add(perm);
                    }
                }
            } else if (subCommand.equals("check")) {
                // Keine weiteren Vorschläge
            }
        } else if (args.length == 6) {
            // Sechster Parameter (nur für add)
            if (subCommand.equals("add")) {
                // true/false
                List<String> values = Arrays.asList("true", "false");
                for (String value : values) {
                    if (value.toLowerCase().startsWith(args[5].toLowerCase())) {
                        completions.add(value);
                    }
                }
            }
        } else if (args.length == 7) {
            // Siebter Parameter (nur für add player)
            if (subCommand.equals("add") && args[2].equalsIgnoreCase("player")) {
                // temp für temporäre Berechtigung
                if ("temp".startsWith(args[6].toLowerCase())) {
                    completions.add("temp");
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Gibt eine Liste häufiger Berechtigungen zurück, die für Tab-Completion verwendet werden können
     */
    private List<String> getCommonPermissions() {
        // Erstelle eine neue Liste für die gefilterten Berechtigungen
        List<String> permissions = new ArrayList<>();
        
        // Füge apicore Berechtigungen hinzu
        permissions.add("apicore.admin");
        permissions.add("apicore.admin.*");
        permissions.add("apicore.commands");
        permissions.add("apicore.debug");
        permissions.add("apicore.status");
        permissions.add("apicore.modules.*");
        permissions.add("apicore.reload");
        
        // Füge Modul-Berechtigungen hinzu
        for (String moduleName : apiCore.getLoadedModules().keySet()) {
            String moduleLower = moduleName.toLowerCase();
            
            // Standard-Modul-Berechtigungen
            permissions.add(moduleLower + ".admin");
            permissions.add(moduleLower + ".use");
            permissions.add(moduleLower + ".command.*");
            
            // Globale Präfixe mit essentialscore
            permissions.add("essentialscore." + moduleLower + ".admin");
        }
        
        // Optional: Füge nur Berechtigungen aus dem PermissionManager hinzu, die mit apicore oder einem Modulnamen beginnen
        java.util.Set<String> registeredPermissions = apiCore.getPermissionManager().getAllRegisteredPermissions();
        for (String permission : registeredPermissions) {
            String permLower = permission.toLowerCase();
            if (permLower.startsWith("apicore.") || modulePrefixMatch(permLower)) {
                permissions.add(permission);
            }
        }
        
        return permissions;
    }
    
    /**
     * Hilfsmethode um zu prüfen, ob eine Berechtigung mit einem Modulnamen beginnt
     */
    private boolean modulePrefixMatch(String permission) {
        for (String moduleName : apiCore.getLoadedModules().keySet()) {
            String moduleLower = moduleName.toLowerCase();
            if (permission.startsWith(moduleLower + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Zeigt Systeminformationen für einen Spieler an
     */
    private void showSystemInfo(Player player) {
        String prefix = apiCore.getMessagePrefix();
        
        player.sendMessage(apiCore.formatHex(prefix + "#4DEEEB◆ #38C6C3System-Informationen #4DEEEB◆"));
        player.sendMessage(apiCore.formatHex("#38C6C3└────────────────────────────┘"));
        
        // Server-Info
        player.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lServer:"));
        player.sendMessage(apiCore.formatHex("  #74E8E5Version: #FFFFFF" + Bukkit.getBukkitVersion()));
        player.sendMessage(apiCore.formatHex("  #74E8E5Online-Spieler: #FFFFFF" + Bukkit.getOnlinePlayers().size() + " / " + Bukkit.getMaxPlayers()));
        
        // Spieler-Info
        player.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lSpieler:"));
        player.sendMessage(apiCore.formatHex("  #74E8E5Name: #FFFFFF" + player.getName()));
        player.sendMessage(apiCore.formatHex("  #74E8E5UUID: #FFFFFF" + player.getUniqueId()));
        player.sendMessage(apiCore.formatHex("  #74E8E5OP: #FFFFFF" + player.isOp()));
        
        // Prüfen wichtiger Berechtigungen
        String[] keyPermissions = {
            "apicore.admin", 
            "apicore.admin.*", 
            "apicore.commands", 
            "apicore.debug", 
            "apicore.status"
        };
        
        player.sendMessage(apiCore.formatHex("#FFFFFF- Berechtigungen:"));
        for (String perm : keyPermissions) {
            boolean hasPerm = apiCore.getPermissionManager().hasPermission(player, perm);
            boolean hasPermApiCore = apiCore.hasPermission(player, perm);
            player.sendMessage(apiCore.formatHex("  #FFFFFF" + perm + ": #74E8E5" + hasPerm + 
                    " #FFFFFF(ApiCore: #74E8E5" + hasPermApiCore + "#FFFFFF)"));
        }
    }
} 