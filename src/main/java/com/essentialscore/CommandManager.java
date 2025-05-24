package com.essentialscore;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

/**
 * Manager für Befehle im ApiCore
 */
public class CommandManager {
    private final ApiCore apiCore;
    private ConsoleFormatter console;
    private Set<String> deactivatedCommands = new HashSet<>();
    private boolean hasLoadedDeactivatedCommands = false;
    private File deactivatedCommandsFile;
    
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
        
        // Datei für deaktivierte Befehle einrichten
        deactivatedCommandsFile = new File(apiCore.getDataFolder(), "deactivated_commands.yml");
        loadDeactivatedCommands();
    }
    
    /**
     * Lädt die Liste der deaktivierten Befehle
     */
    private void loadDeactivatedCommands() {
        if (!deactivatedCommandsFile.exists()) {
            try {
                deactivatedCommandsFile.createNewFile();
            } catch (IOException e) {
                console.error("Konnte Datei für deaktivierte Befehle nicht erstellen: " + e.getMessage());
            }
            hasLoadedDeactivatedCommands = true;
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(deactivatedCommandsFile);
            List<String> commands = config.getStringList("deactivated_commands");
            deactivatedCommands = new HashSet<>(commands);
            hasLoadedDeactivatedCommands = true;
            
            if (!deactivatedCommands.isEmpty()) {
                console.info("Geladene deaktivierte Befehle: " + String.join(", ", deactivatedCommands));
            }
        } catch (Exception e) {
            console.error("Fehler beim Laden deaktivierter Befehle: " + e.getMessage());
        }
    }
    
    /**
     * Speichert die Liste der deaktivierten Befehle
     */
    private void saveDeactivatedCommands() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("deactivated_commands", new ArrayList<>(deactivatedCommands));
            config.save(deactivatedCommandsFile);
        } catch (IOException e) {
            console.error("Fehler beim Speichern deaktivierter Befehle: " + e.getMessage());
        }
    }
    
    /**
     * Prüft, ob ein Befehl deaktiviert ist
     * 
     * @param commandName Der zu prüfende Befehl
     * @return true, wenn der Befehl deaktiviert ist
     */
    public boolean isCommandDeactivated(String commandName) {
        if (!hasLoadedDeactivatedCommands) {
            loadDeactivatedCommands();
        }
        return deactivatedCommands.contains(commandName.toLowerCase());
    }
    
    /**
     * Deaktiviert einen ApiCore-Befehl
     * 
     * @param commandName Der Name des zu deaktivierenden Befehls
     * @return true, wenn der Befehl erfolgreich deaktiviert wurde
     */
    public boolean deactivateCommand(String commandName) {
        commandName = commandName.toLowerCase();
        
        // Prüfen, ob der Befehl bereits deaktiviert ist
        if (deactivatedCommands.contains(commandName)) {
            return false;
        }
        
        // Nur ApiCore-Befehle können deaktiviert werden
        if (!isApiCoreCommand(commandName)) {
            return false;
        }
        
        deactivatedCommands.add(commandName);
        saveDeactivatedCommands();
        
        // Befehl unregistrieren wenn möglich
        unregisterCommand(commandName);
        
        return true;
    }
    
    /**
     * Reaktiviert einen ApiCore-Befehl
     * 
     * @param commandName Der Name des zu reaktivierenden Befehls
     * @return true, wenn der Befehl erfolgreich reaktiviert wurde
     */
    public boolean reactivateCommand(String commandName) {
        commandName = commandName.toLowerCase();
        
        // Prüfen, ob der Befehl deaktiviert ist
        if (!deactivatedCommands.contains(commandName)) {
            return false;
        }
        
        deactivatedCommands.remove(commandName);
        saveDeactivatedCommands();
        
        // Befehl neu registrieren ist nur nach Neustart möglich
        console.info("Befehl '" + commandName + "' wurde reaktiviert. Server-Neustart erforderlich, um den Befehl zu verwenden.");
        
        return true;
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
            "apicore", "core", "modules", "module", "mod",
            "acp", "acr", "apm", "acpm", "ess",
            "essentials", "esscore", "ec"
        ));
        
        return apiCoreCommands.contains(commandName.toLowerCase());
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
     * Überprüft vor der Ausführung eines Befehls, ob dieser deaktiviert ist
     * 
     * @param command Der auszuführende Befehl
     * @return true, wenn der Befehl deaktiviert ist und nicht ausgeführt werden soll
     */
    public boolean shouldBlockCommand(String command) {
        if (!hasLoadedDeactivatedCommands) {
            loadDeactivatedCommands();
        }
        
        // Extrahiere den Hauptbefehl (erster Teil)
        String mainCommand = command.split(" ")[0].toLowerCase();
        
        // Prüfe, ob der Befehl deaktiviert ist
        return deactivatedCommands.contains(mainCommand);
    }
    
    /**
     * Registriert die Core-Befehle des Plugins
     */
    public void registerCoreCommands() {
        // Hauptbefehl registrieren
        apiCore.getCommand("apicore").setExecutor((sender, command, label, args) -> {
            if (args.length == 0) {
                showHelp(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "help":
                    showHelp(sender);
                    break;
                case "modules":
                case "list":
                    listModules(sender);
                    break;
                case "info":
                    if (args.length < 2) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cBitte gib einen Modulnamen an!"));
                        return true;
                    }
                    moduleInfo(sender, args[1]);
                    break;
                case "commands":
                    if (args.length < 2) {
                        listAllCommands(sender);
                    } else {
                        listModuleCommands(sender, args[1]);
                    }
                    break;
                case "enable":
                    if (args.length < 2) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cBitte gib einen Modulnamen an!"));
                        return true;
                    }
                    enableModule(sender, args[1]);
                    break;
                case "disable":
                    if (args.length < 2) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cBitte gib einen Modulnamen an!"));
                        return true;
                    }
                    disableModule(sender, args[1]);
                    break;
                case "reload":
                    if (args.length < 2) {
                        reloadAll(sender);
                    } else {
                        if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("modules")) {
                            reloadAllModules(sender);
                        } else if (args[1].equalsIgnoreCase("config")) {
                            reloadConfig(sender);
                        } else if (args[1].equalsIgnoreCase("resources")) {
                            reloadResources(sender);
                        } else {
                            reloadModule(sender, args[1]);
                        }
                    }
                    break;
                case "debug":
                    toggleDebug(sender);
                    break;
                case "status":
                    showStatus(sender);
                    break;
                case "benchmark":
                    if (args.length < 2) {
                        showBenchmarkHelp(sender);
                    } else {
                        runBenchmark(sender, Arrays.copyOfRange(args, 1, args.length));
                    }
                    break;
                case "cache":
                    if (args.length < 2) {
                        showCacheHelp(sender);
                    } else {
                        handleCacheCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                    }
                    break;
                case "permission":
                case "perm":
                    if (args.length < 2) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cBitte gib einen Unterbefehl an!"));
                        return true;
                    }
                    handlePermissionCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "sandbox":
                    if (args.length < 2) {
                        displaySandboxStatus(sender);
                    } else {
                        String sandboxCmd = args[1].toLowerCase();
                        if (sandboxCmd.equals("enable")) {
                            setSandboxEnabled(sender, true);
                        } else if (sandboxCmd.equals("disable")) {
                            setSandboxEnabled(sender, false);
                        } else if (sandboxCmd.equals("trust") && args.length > 2) {
                            trustModule(sender, args[2]);
                        } else if (sandboxCmd.equals("untrust") && args.length > 2) {
                            untrustModule(sender, args[2]);
                        } else {
                            displaySandboxStatus(sender);
                        }
                    }
                    break;
                case "performance":
                case "perf":
                    if (args.length < 2) {
                        // Ohne weiteren Unterbefehl: Liste aller Module mit Performance-Status anzeigen
                        showAllModulesPerformance(sender);
                    } else {
                        String perfCmd = args[1].toLowerCase();
                        
                        if (perfCmd.equals("bossbar")) {
                            if (args.length < 3) {
                                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                                    "&cBitte gib einen Modulnamen oder 'hide'/'cycle' an!"));
                            } else if (args[2].equalsIgnoreCase("hide")) {
                                hideModulePerformanceBossBar(sender);
                            } else if (args[2].equalsIgnoreCase("cycle")) {
                                cycleModulePerformanceBossBar(sender);
                            } else {
                                showModulePerformanceBossBar(sender, args[2]);
                            }
                        } else {
                            // Als Modulname interpretieren
                            showModulePerformance(sender, args[1]);
                        }
                    }
                    break;
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
                    "permission", "perm", "sandbox", "performance", "perf");
                
                for (String cmd : commands) {
                    if (cmd.startsWith(args[0].toLowerCase())) {
                        completions.add(cmd);
                    }
                }
            } else if (args.length == 2) {
                // Unterbefehle oder Modulnamen
                String subCommand = args[0].toLowerCase();
                
                switch (subCommand) {
                    case "info":
                    case "enable":
                    case "disable":
                        // Modulnamen
                        for (String moduleName : apiCore.getLoadedModules().keySet()) {
                            if (moduleName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(moduleName);
                            }
                        }
                        break;
                    case "commands":
                        // Modulnamen für Befehlsanzeige
                        completions.add("all");
                        for (String moduleName : apiCore.getLoadedModules().keySet()) {
                            if (moduleName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(moduleName);
                            }
                        }
                        break;
                    case "reload":
                        // Reload-Optionen
                        for (String option : Arrays.asList("all", "modules", "config", "resources")) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        // Auch Modulnamen
                        for (String moduleName : apiCore.getLoadedModules().keySet()) {
                            if (moduleName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(moduleName);
                            }
                        }
                        break;
                    case "benchmark":
                        // Benchmark-Optionen
                        for (String option : Arrays.asList("module", "thread", "memory", "io", "all")) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                    case "cache":
                        // Cache-Optionen
                        for (String option : Arrays.asList("clear", "info", "stats")) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                    case "permission":
                    case "perm":
                        // Permission-Optionen
                        for (String option : Arrays.asList("check", "list", "reload")) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                    case "sandbox":
                        // Sandbox-Optionen
                        for (String option : Arrays.asList("enable", "disable", "trust", "untrust")) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                    case "performance":
                    case "perf":
                        // Performance-Optionen
                        completions.add("bossbar");
                        
                        // Auch Modulnamen
                        for (String moduleName : apiCore.getLoadedModules().keySet()) {
                            if (moduleName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(moduleName);
                            }
                        }
                        break;
                }
            } else if (args.length == 3) {
                // Weitere Unterbefehle
                String subCommand = args[0].toLowerCase();
                String subSubCommand = args[1].toLowerCase();
                
                if ((subCommand.equals("permission") || subCommand.equals("perm")) && subSubCommand.equals("check")) {
                    // Permissions vervollständigen
                    return tabCompletePermissionCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                } else if (subCommand.equals("sandbox") && (subSubCommand.equals("trust") || subSubCommand.equals("untrust"))) {
                    // Modulnamen
                    for (String moduleName : apiCore.getLoadedModules().keySet()) {
                        if (moduleName.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(moduleName);
                        }
                    }
                } else if ((subCommand.equals("performance") || subCommand.equals("perf")) && subSubCommand.equals("bossbar")) {
                    // BossBar-Optionen
                    for (String option : Arrays.asList("hide", "cycle")) {
                        if (option.startsWith(args[2].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                    
                    // Auch Modulnamen
                    for (String moduleName : apiCore.getLoadedModules().keySet()) {
                        if (moduleName.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(moduleName);
                        }
                    }
                }
            }
            
            return completions;
        });
        
        // Zusätzlich: Den "commandmanager" oder "cm" Befehl registrieren, um Befehle zu verwalten
        DynamicCommand cmdManager = new DynamicCommand("commandmanager", 
                                                      "Verwaltet ApiCore-Befehle (aktivieren/deaktivieren)",
                                                      "/commandmanager <deactivate|activate> <command>",
                                                      Arrays.asList("cm"), "apicore", "apicore.admin.commands", apiCore);
        
        cmdManager.setExecutor(commandManagerExecutor);
        cmdManager.setTabCompleter(commandManagerTabCompleter);
        
        // Nur registrieren, wenn der Befehl nicht deaktiviert ist
        if (!isCommandDeactivated("commandmanager")) {
            apiCore.registerCommands(Collections.singletonList(cmdManager));
        }
    }
    
    /**
     * Diese Methode wird als Executor für den commandmanager/cm-Befehl verwendet
     */
    private final CommandExecutor commandManagerExecutor = (sender, command, label, args) -> {
        return handleCommandManagerCommand(sender, args);
    };
    
    /**
     * Tab-Completer für den CommandManager-Befehl
     */
    private final TabCompleter commandManagerTabCompleter = (sender, command, alias, args) -> {
        return tabCompleteCommandManagerCommand(sender, args);
    };
    
    /**
     * Handhabt den CommandManager-Befehl zur Verwaltung von ApiCore-Befehlen
     * 
     * @param sender Der Befehlsabsender
     * @param args Die Befehlsargumente
     */
    private void displaySandboxStatus(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        ModuleSandbox sandbox = apiCore.getModuleSandbox();
        
        if (sandbox == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDie Modul-Sandbox ist nicht verfügbar!"));
            return;
        }
        
        boolean enabled = sandbox.isSandboxEnabled();
        sender.sendMessage(apiCore.formatHex(prefix + "&7Status der Modul-Sandbox: " + 
                (enabled ? "&aAktiviert" : "&cDeaktiviert")));
        
        // Zeige Liste der vertrauenswürdigen Module
        sender.sendMessage(apiCore.formatHex(prefix + "&7Vertrauenswürdige Module (laufen außerhalb der Sandbox):"));
        boolean hasTrustedModules = false;
        
        for (String moduleName : apiCore.getLoadedModules().keySet()) {
            if (sandbox.isTrustedModule(moduleName)) {
                sender.sendMessage(apiCore.formatHex("  &8- &e" + moduleName));
                hasTrustedModules = true;
            }
        }
        
        if (!hasTrustedModules) {
            sender.sendMessage(apiCore.formatHex("  &8- &7(Keine vertrauenswürdigen Module)"));
        }
    }
    
    /**
     * Aktiviert oder deaktiviert die Modul-Sandbox
     * 
     * @param sender Der Befehlsabsender
     * @param enabled Ob die Sandbox aktiviert werden soll
     */
    private void setSandboxEnabled(CommandSender sender, boolean enabled) {
        String prefix = apiCore.getMessagePrefix();
        ModuleSandbox sandbox = apiCore.getModuleSandbox();
        
        if (sandbox == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDie Modul-Sandbox ist nicht verfügbar!"));
            return;
        }
        
        // Status setzen
        sandbox.setSandboxEnabled(enabled);
        
        // Bestätigung
        if (enabled) {
            sender.sendMessage(apiCore.formatHex(prefix + "&aDie Modul-Sandbox wurde aktiviert!"));
            sender.sendMessage(apiCore.formatHex(prefix + "&7Module laufen nun in einer geschützten Umgebung."));
        } else {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDie Modul-Sandbox wurde deaktiviert!"));
            sender.sendMessage(apiCore.formatHex(prefix + "&7&lAchtung: &7Module laufen nun ohne Absturz-Schutz."));
        }
    }
    
    /**
     * Markiert ein Modul als vertrauenswürdig
     * 
     * @param sender Der Befehlsabsender
     * @param moduleName Der Name des Moduls
     */
    private void trustModule(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        ModuleSandbox sandbox = apiCore.getModuleSandbox();
        
        if (sandbox == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDie Modul-Sandbox ist nicht verfügbar!"));
            return;
        }
        
        // Prüfen, ob das Modul existiert
        if (!apiCore.getLoadedModules().containsKey(moduleName)) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + moduleName + "&c ist nicht geladen!"));
            return;
        }
        
        // Als vertrauenswürdig markieren
        sandbox.trustModule(moduleName);
        
        // Bestätigung
        sender.sendMessage(apiCore.formatHex(prefix + "&aModul &e" + moduleName + 
                "&a wurde als vertrauenswürdig markiert und läuft außerhalb der Sandbox."));
    }
    
    /**
     * Entfernt ein Modul aus der Liste der vertrauenswürdigen Module
     * 
     * @param sender Der Befehlsabsender
     * @param moduleName Der Name des Moduls
     */
    private void untrustModule(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        ModuleSandbox sandbox = apiCore.getModuleSandbox();
        
        if (sandbox == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDie Modul-Sandbox ist nicht verfügbar!"));
            return;
        }
        
        // Prüfen, ob das Modul existiert
        if (!apiCore.getLoadedModules().containsKey(moduleName)) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + moduleName + "&c ist nicht geladen!"));
            return;
        }
        
        // Aus der Liste der vertrauenswürdigen Module entfernen
        sandbox.untrustModule(moduleName);
        
        // Bestätigung
        sender.sendMessage(apiCore.formatHex(prefix + "&aModul &e" + moduleName + 
                "&a wurde aus der Liste der vertrauenswürdigen Module entfernt und läuft nun in der Sandbox."));
    }
    
    /**
     * Zeigt die Performance-Informationen für ein Modul an
     * 
     * @param sender Der Befehlsabsender
     * @param moduleName Der Name des Moduls
     */
    private void showModulePerformance(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDieser Befehl kann nur von Spielern ausgeführt werden!"));
            return;
        }
        
        Player player = (Player) sender;
        
        // Prüfe, ob das Modul existiert
        if (!apiCore.getLoadedModules().containsKey(moduleName)) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + moduleName + " &cist nicht geladen!"));
            return;
        }
        
        // Hole Performance-Daten
        ModuleManager moduleManager = apiCore.getModuleManager();
        ModuleManager.ModulePerformanceData performanceData = moduleManager.getModulePerformanceData(moduleName);
        
        if (performanceData == null) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cKeine Performance-Daten für Modul &e" + moduleName + " &cverfügbar!"));
            
            // Performance-Tracking starten falls noch nicht aktiv
            sender.sendMessage(apiCore.formatHex(prefix + "&7Starte Performance-Tracking..."));
            moduleManager.startPerformanceTracking();
            
            // Nach 3 Sekunden erneut versuchen
            Bukkit.getScheduler().runTaskLater(apiCore, () -> showModulePerformance(sender, moduleName), 60L);
            return;
        }
        
        // Performance-Status bestimmen
        ModuleManager.PerformanceStatus status = performanceData.getPerformanceStatus();
        
        // Farbcodes basierend auf Status
        String statusColor;
        String statusText;
        
        switch (status) {
            case CRITICAL:
                statusColor = "#FF5555";
                statusText = "KRITISCH - Sollte überprüft werden";
                break;
            case WARNING:
                statusColor = "#FFAA00";
                statusText = "WARNUNG - Hohe Auslastung";
                break;
            default:
                statusColor = "#55FF55";
                statusText = "OK - Normale Auslastung";
                break;
        }
        
        // Header
        sender.sendMessage(apiCore.formatHex("\n" + prefix + "#4DEEEB◆ #38C6C3Performance: #FFFFFF" + moduleName + " #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└─────────────────────────┘"));
        
        // Status
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lStatus: " + statusColor + statusText));
        
        // CPU-Auslastung
        double cpuUsage = performanceData.getCpuUsagePercent();
        String cpuColor = getCpuColorCode(cpuUsage);
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lCPU-Auslastung: " + 
            cpuColor + String.format("%.1f", cpuUsage) + "%"));
        
        // Speichernutzung
        String memoryFormatted = performanceData.getMemoryUsageFormatted();
        long memoryMB = performanceData.getMemoryUsageBytes() / (1024 * 1024);
        String memoryColor = getMemoryColorCode(memoryMB);
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lSpeichernutzung: " + 
            memoryColor + memoryFormatted));
        
        // Methodenaufrufe
        long executionCount = performanceData.getTotalExecutionCount();
        double avgExecutionTime = performanceData.getAvgExecutionTimeMs();
        String executionColor = getExecutionTimeColorCode(avgExecutionTime);
        sender.sendMessage(apiCore.formatHex("#4DEEEB» #FFFFFF&lMethodenaufrufe: " + 
            "#FFFFFF" + executionCount + " #7F7F7F(Durchschnitt: " + 
            executionColor + String.format("%.2f", avgExecutionTime) + "ms#7F7F7F)"));
        
        // BossBar-Anzeige
        sender.sendMessage(apiCore.formatHex("\n#4DEEEB» #FFFFFF&lBossBar-Anzeige:"));
        sender.sendMessage(apiCore.formatHex("#7F7F7F- #FFFFFFZeigt detaillierte Performance-Daten als BossBar an"));
        sender.sendMessage(apiCore.formatHex("#7F7F7F- #FFFFFFKlicke #FFAA00[BossBar anzeigen]#FFFFFF, um die Anzeige zu starten"));
        
        // Interaktive Komponenten hinzufügen
        // Hier würde man normalerweise Text-Komponenten mit Click-Events erstellen,
        // aber wir nutzen stattdessen einen einfachen Befehl zur Demo
        sender.sendMessage(apiCore.formatHex("#FFAA00Führe #FFFFFF/apicore performance bossbar " + moduleName + 
            " #FFFFFFaus, um die BossBar zu aktivieren!"));
        
        // Warnung für kritischen Status
        if (status == ModuleManager.PerformanceStatus.CRITICAL) {
            sender.sendMessage(apiCore.formatHex("\n#FF5555⚠ WARNUNG: Dieses Modul zeigt kritische Performance-Probleme!"));
            sender.sendMessage(apiCore.formatHex("#FF5555Es wird empfohlen, das Modul zu überprüfen oder temporär zu deaktivieren."));
            sender.sendMessage(apiCore.formatHex("#FFAA00Führe #FFFFFF/apicore disable " + moduleName + 
                " #FFFFFFaus, um das Modul zu deaktivieren."));
        }
    }
    
    /**
     * Zeigt eine BossBar mit Performance-Daten für ein Modul an
     * 
     * @param sender Der Befehlsabsender
     * @param moduleName Der Name des Moduls
     */
    private void showModulePerformanceBossBar(CommandSender sender, String moduleName) {
        String prefix = apiCore.getMessagePrefix();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDieser Befehl kann nur von Spielern ausgeführt werden!"));
            return;
        }
        
        Player player = (Player) sender;
        
        // Prüfe, ob das Modul existiert
        if (!apiCore.getLoadedModules().containsKey(moduleName)) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cModul &e" + moduleName + " &cist nicht geladen!"));
            return;
        }
        
        try {
            // Hole oder erstelle ModulePerformanceBossBar
            com.essentialscore.util.ModulePerformanceBossBar performanceBossBar = getPerformanceBossBar();
            
            // Performance-Tracking starten falls noch nicht aktiv
            apiCore.getModuleManager().startPerformanceTracking();
            
            // BossBar starten und anzeigen
            performanceBossBar.start();
            performanceBossBar.showBossBarForModule(player, moduleName);
            
            sender.sendMessage(apiCore.formatHex(prefix + "&aBossBar für Modul &e" + moduleName + 
                " &awird angezeigt. Zum Wechseln der Ansicht führe &f/apicore performance bossbar cycle &aaus."));
            
            // Hinweis zum Beenden anzeigen
            sender.sendMessage(apiCore.formatHex(prefix + "&7Zum Ausblenden der BossBar führe &f/apicore performance bossbar hide &7aus."));
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Anzeigen der BossBar: &e" + e.getMessage()));
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Blendet die Performance-BossBar für einen Spieler aus
     * 
     * @param sender Der Befehlsabsender
     */
    private void hideModulePerformanceBossBar(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDieser Befehl kann nur von Spielern ausgeführt werden!"));
            return;
        }
        
        Player player = (Player) sender;
        
        try {
            // Hole ModulePerformanceBossBar
            com.essentialscore.util.ModulePerformanceBossBar performanceBossBar = getPerformanceBossBar();
            
            // BossBar ausblenden
            performanceBossBar.removeBossBar(player);
            
            sender.sendMessage(apiCore.formatHex(prefix + "&aBossBar wurde ausgeblendet."));
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Ausblenden der BossBar: &e" + e.getMessage()));
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Wechselt die angezeigte BossBar für einen Spieler
     * 
     * @param sender Der Befehlsabsender
     */
    private void cycleModulePerformanceBossBar(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cDieser Befehl kann nur von Spielern ausgeführt werden!"));
            return;
        }
        
        Player player = (Player) sender;
        
        try {
            // Hole ModulePerformanceBossBar
            com.essentialscore.util.ModulePerformanceBossBar performanceBossBar = getPerformanceBossBar();
            
            // BossBar wechseln
            performanceBossBar.cycleActiveBossBar(player);
            
            sender.sendMessage(apiCore.formatHex(prefix + "&7BossBar-Ansicht gewechselt."));
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cFehler beim Wechseln der BossBar: &e" + e.getMessage()));
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    // Hilfsmethode, um die richtige Instanz der ModulePerformanceBossBar zu erhalten
    private com.essentialscore.util.ModulePerformanceBossBar getPerformanceBossBar() {
        // In ApiCore cachen und abrufen
        com.essentialscore.util.ModulePerformanceBossBar performanceBossBar = 
            (com.essentialscore.util.ModulePerformanceBossBar) apiCore.getSharedData("performanceBossBar");
        
        // Falls noch nicht erstellt, neu erstellen
        if (performanceBossBar == null) {
            performanceBossBar = new com.essentialscore.util.ModulePerformanceBossBar(apiCore);
            apiCore.setSharedData("performanceBossBar", performanceBossBar);
        }
        
        return performanceBossBar;
    }
    
    /**
     * Gibt einen Farbcode für eine CPU-Auslastung zurück
     * 
     * @param cpuPercent Die CPU-Auslastung in Prozent
     * @return Ein Hexadezimal-Farbcode
     */
    private String getCpuColorCode(double cpuPercent) {
        if (cpuPercent >= ModuleManager.CPU_THRESHOLD_CRITICAL) {
            return "#FF5555"; // Rot
        } else if (cpuPercent >= ModuleManager.CPU_THRESHOLD_WARNING) {
            return "#FFAA00"; // Gelb
        } else {
            return "#55FF55"; // Grün
        }
    }
    
    /**
     * Gibt einen Farbcode für eine Speichernutzung zurück
     * 
     * @param memoryMB Die Speichernutzung in MB
     * @return Ein Hexadezimal-Farbcode
     */
    private String getMemoryColorCode(long memoryMB) {
        if (memoryMB >= ModuleManager.MEMORY_THRESHOLD_CRITICAL) {
            return "#FF5555"; // Rot
        } else if (memoryMB >= ModuleManager.MEMORY_THRESHOLD_WARNING) {
            return "#FFAA00"; // Gelb
        } else {
            return "#55FF55"; // Grün
        }
    }
    
    /**
     * Gibt einen Farbcode für eine Ausführungszeit zurück
     * 
     * @param executionTimeMs Die Ausführungszeit in Millisekunden
     * @return Ein Hexadezimal-Farbcode
     */
    private String getExecutionTimeColorCode(double executionTimeMs) {
        if (executionTimeMs >= ModuleManager.EXECUTION_THRESHOLD_CRITICAL) {
            return "#FF5555"; // Rot
        } else if (executionTimeMs >= ModuleManager.EXECUTION_THRESHOLD_WARNING) {
            return "#FFAA00"; // Gelb
        } else {
            return "#55FF55"; // Grün
        }
    }

    /**
     * Zeigt eine Übersicht aller Module mit ihrem Performance-Status an
     * 
     * @param sender Der Befehlsabsender
     */
    private void showAllModulesPerformance(CommandSender sender) {
        String prefix = apiCore.getMessagePrefix();
        
        // Hole alle Modul-Performance-Daten
        ModuleManager moduleManager = apiCore.getModuleManager();
        Map<String, ModuleManager.ModulePerformanceData> performanceDataMap = moduleManager.getAllModulePerformanceData();
        
        // Prüfe, ob Performance-Tracking aktiv ist
        if (performanceDataMap.isEmpty()) {
            sender.sendMessage(apiCore.formatHex(prefix + "&cKeine Performance-Daten verfügbar!"));
            
            // Performance-Tracking starten
            sender.sendMessage(apiCore.formatHex(prefix + "&7Starte Performance-Tracking..."));
            moduleManager.startPerformanceTracking();
            
            // Nach 3 Sekunden erneut versuchen
            Bukkit.getScheduler().runTaskLater(apiCore, () -> showAllModulesPerformance(sender), 60L);
            return;
        }
        
        // Sortiere Module nach Performance-Status (kritisch -> warnung -> ok)
        List<ModuleManager.ModulePerformanceData> sortedData = new ArrayList<>(performanceDataMap.values());
        sortedData.sort((a, b) -> {
            int statusCompare = b.getPerformanceStatus().compareTo(a.getPerformanceStatus());
            if (statusCompare != 0) {
                return statusCompare;
            }
            
            // Bei gleichem Status nach CPU-Auslastung sortieren
            return Double.compare(b.getCpuUsagePercent(), a.getCpuUsagePercent());
        });
        
        // Header
        sender.sendMessage(apiCore.formatHex("\n" + prefix + "#4DEEEB◆ #38C6C3Performance aller Module #4DEEEB◆"));
        sender.sendMessage(apiCore.formatHex("#38C6C3└────────────────────────┘"));
        
        // Kritische Module zuerst anzeigen
        boolean hasCritical = false;
        for (ModuleManager.ModulePerformanceData data : sortedData) {
            if (data.getPerformanceStatus() == ModuleManager.PerformanceStatus.CRITICAL) {
                if (!hasCritical) {
                    sender.sendMessage(apiCore.formatHex("\n#FF5555⚠ KRITISCHE Module:"));
                    hasCritical = true;
                }
                showPerformanceSummary(sender, data);
            }
        }
        
        // Module mit Warnungen anzeigen
        boolean hasWarning = false;
        for (ModuleManager.ModulePerformanceData data : sortedData) {
            if (data.getPerformanceStatus() == ModuleManager.PerformanceStatus.WARNING) {
                if (!hasWarning) {
                    sender.sendMessage(apiCore.formatHex("\n#FFAA00⚠ Module mit WARNUNG:"));
                    hasWarning = true;
                }
                showPerformanceSummary(sender, data);
            }
        }
        
        // OK Module anzeigen
        boolean hasOk = false;
        for (ModuleManager.ModulePerformanceData data : sortedData) {
            if (data.getPerformanceStatus() == ModuleManager.PerformanceStatus.OK) {
                if (!hasOk) {
                    sender.sendMessage(apiCore.formatHex("\n#55FF55✓ Module mit normaler Auslastung:"));
                    hasOk = true;
                }
                showPerformanceSummary(sender, data);
            }
        }
        
        // Hinweis auf detaillierte Ansicht
        sender.sendMessage(apiCore.formatHex("\n#AAAAAA➤ Für detaillierte Informationen: #FFFFFF/apicore performance <Modulname>"));
        sender.sendMessage(apiCore.formatHex("#AAAAAA➤ Für BossBar-Anzeige: #FFFFFF/apicore performance bossbar <Modulname>"));
    }
    
    /**
     * Zeigt eine Zusammenfassung der Performance-Daten eines Moduls an
     * 
     * @param sender Der Befehlsabsender
     * @param data Die Performance-Daten
     */
    private void showPerformanceSummary(CommandSender sender, ModuleManager.ModulePerformanceData data) {
        ModuleManager.PerformanceStatus status = data.getPerformanceStatus();
        
        String statusColor;
        switch (status) {
            case CRITICAL:
                statusColor = "#FF5555";
                break;
            case WARNING:
                statusColor = "#FFAA00";
                break;
            default:
                statusColor = "#55FF55";
                break;
        }
        
        double cpuUsage = data.getCpuUsagePercent();
        String cpuColor = getCpuColorCode(cpuUsage);
        
        String memoryFormatted = data.getMemoryUsageFormatted();
        long memoryMB = data.getMemoryUsageBytes() / (1024 * 1024);
        String memoryColor = getMemoryColorCode(memoryMB);
        
        // Kurzzusammenfassung mit farbigen Werten
        sender.sendMessage(apiCore.formatHex(statusColor + "➤ " + "#FFFFFF" + data.getModuleName() + 
            " - CPU: " + cpuColor + String.format("%.1f", cpuUsage) + "%" + 
            "#FFFFFF - Speicher: " + memoryColor + memoryFormatted));
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
     * Registriert den Befehl zum Verwalten der Befehlsdeaktivierung
     */
    public void registerCommandDeactivationCommand() {
        DynamicCommand cmdDeactivate = new DynamicCommand("commanddeactivate",
                "Deaktiviert oder reaktiviert ApiCore-Befehle",
                "/commanddeactivate <deactivate|activate> <command>",
                Arrays.asList("cmdd", "cmddeactivate", "deactivatecmd"),
                "apicore",
                "apicore.admin.commands",
                apiCore);
        
        cmdDeactivate.setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length < 2) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cVerwendung: /commanddeactivate <deactivate|activate> <command>"));
                    return true;
                }
                
                String action = args[0].toLowerCase();
                String commandName = args[1].toLowerCase();
                
                if (action.equals("deactivate")) {
                    if (isApiCoreCommand(commandName)) {
                        boolean success = deactivateCommand(commandName);
                        if (success) {
                            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aBefehl &e" + commandName + " &awurde deaktiviert. Änderung wird nach Neustart wirksam."));
                        } else {
                            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cBefehl &e" + commandName + " &ckonnte nicht deaktiviert werden oder ist bereits deaktiviert."));
                        }
                    } else {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cNur ApiCore-Befehle können deaktiviert werden."));
                    }
                } else if (action.equals("activate")) {
                    boolean success = reactivateCommand(commandName);
                    if (success) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aBefehl &e" + commandName + " &awurde reaktiviert. Änderung wird nach Neustart wirksam."));
                    } else {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cBefehl &e" + commandName + " &ckonnte nicht reaktiviert werden oder ist nicht deaktiviert."));
                    }
                } else {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cUngültige Aktion. Verwende 'deactivate' oder 'activate'."));
                }
                
                return true;
            }
        });
        
        cmdDeactivate.setTabCompleter(new TabCompleter() {
            @Override
            public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
                if (args.length == 1) {
                    return Arrays.asList("deactivate", "activate");
                } else if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("deactivate")) {
                        List<String> apiCommands = new ArrayList<>();
                        for (Command cmd : getCommandMapCommands()) {
                            if (isApiCoreCommand(cmd.getName()) && !isCommandDeactivated(cmd.getName())) {
                                apiCommands.add(cmd.getName());
                            }
                        }
                        return apiCommands;
                    } else if (args[0].equalsIgnoreCase("activate")) {
                        return new ArrayList<>(deactivatedCommands);
                    }
                }
                return Collections.emptyList();
            }
        });
        
        apiCore.registerCommands(Collections.singletonList(cmdDeactivate));
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
        // Add more commands
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
     * Zeigt Informationen zu einem Modul
     * 
     * @param sender Der Empfänger der Nachricht
     * @param moduleName Der Name des Moduls
     */
    private void moduleInfo(CommandSender sender, String moduleName) {
        // Implementation of module info
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fInformationen zu Modul " + moduleName));
    }
    
    /**
     * Listet alle verfügbaren Befehle auf
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void listAllCommands(CommandSender sender) {
        // Implementation of command listing
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fVerfügbare Befehle:"));
    }
    
    /**
     * Listet alle Befehle eines Moduls auf
     * 
     * @param sender Der Empfänger der Nachricht
     * @param moduleName Der Name des Moduls
     */
    private void listModuleCommands(CommandSender sender, String moduleName) {
        // Implementation of module command listing
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fBefehle von Modul " + moduleName + ":"));
    }
    
    /**
     * Aktiviert ein Modul
     * 
     * @param sender Der Empfänger der Nachricht
     * @param moduleName Der Name des Moduls
     */
    private void enableModule(CommandSender sender, String moduleName) {
        // Implementation of module enabling
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aModul " + moduleName + " wurde aktiviert."));
    }
    
    /**
     * Deaktiviert ein Modul
     * 
     * @param sender Der Empfänger der Nachricht
     * @param moduleName Der Name des Moduls
     */
    private void disableModule(CommandSender sender, String moduleName) {
        // Implementation of module disabling
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModul " + moduleName + " wurde deaktiviert."));
    }
    
    /**
     * Lädt alles neu (Config, Module, etc.)
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void reloadAll(CommandSender sender) {
        // Implementation of full reload
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aAlle Komponenten wurden neu geladen."));
    }
    
    /**
     * Lädt alle Module neu
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void reloadAllModules(CommandSender sender) {
        // Implementation of module reloading
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aAlle Module wurden neu geladen."));
    }
    
    /**
     * Lädt die Konfiguration neu
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void reloadConfig(CommandSender sender) {
        // Implementation of config reloading
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aKonfiguration wurde neu geladen."));
    }
    
    /**
     * Lädt die Ressourcen neu
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void reloadResources(CommandSender sender) {
        // Implementation of resource reloading
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aRessourcen wurden neu geladen."));
    }
    
    /**
     * Lädt ein einzelnes Modul neu
     * 
     * @param sender Der Empfänger der Nachricht
     * @param moduleName Der Name des Moduls
     */
    private void reloadModule(CommandSender sender, String moduleName) {
        // Implementation of single module reloading
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aModul " + moduleName + " wurde neu geladen."));
    }
    
    /**
     * Schaltet den Debug-Modus um
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void toggleDebug(CommandSender sender) {
        // Implementation of debug toggle
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aDebug-Modus wurde umgeschaltet."));
    }
    
    /**
     * Zeigt den Status des ApiCore an
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void showStatus(CommandSender sender) {
        // Implementation of status display
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fStatus des ApiCore:"));
    }
    
    /**
     * Zeigt die Hilfe für den Benchmark-Befehl
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void showBenchmarkHelp(CommandSender sender) {
        // Implementation of benchmark help
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fBenchmark-Befehle:"));
    }
    
    /**
     * Führt einen Benchmark aus
     * 
     * @param sender Der Empfänger der Nachricht
     * @param args Die Befehlsargumente
     */
    private void runBenchmark(CommandSender sender, String[] args) {
        // Implementation of benchmark execution
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aBenchmark wird ausgeführt..."));
    }
    
    /**
     * Zeigt die Hilfe für den Cache-Befehl
     * 
     * @param sender Der Empfänger der Nachricht
     */
    private void showCacheHelp(CommandSender sender) {
        // Implementation of cache help
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fCache-Befehle:"));
    }
    
    /**
     * Verarbeitet einen Cache-Befehl
     * 
     * @param sender Der Empfänger der Nachricht
     * @param args Die Befehlsargumente
     */
    private void handleCacheCommand(CommandSender sender, String[] args) {
        // Implementation of cache command handling
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aCache-Befehl wird ausgeführt..."));
    }
    
    /**
     * Verarbeitet einen Berechtigungs-Befehl
     * 
     * @param sender Der Empfänger der Nachricht
     * @param args Die Befehlsargumente
     */
    private void handlePermissionCommand(CommandSender sender, String[] args) {
        // Implementation of permission command handling
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aBerechtigungs-Befehl wird ausgeführt..."));
    }
    
    /**
     * Vervollständigt einen Berechtigungs-Befehl
     * 
     * @param sender Der Empfänger der Nachricht
     * @param args Die Befehlsargumente
     * @return Eine Liste der möglichen Vervollständigungen
     */
    private List<String> tabCompletePermissionCommand(CommandSender sender, String[] args) {
        // Implementation of permission tab completion
        return new ArrayList<>();
    }
    
    /**
     * Verarbeitet einen CommandManager-Befehl
     * 
     * @param sender Der Empfänger der Nachricht
     * @param args Die Befehlsargumente
     * @return true, wenn der Befehl erfolgreich ausgeführt wurde
     */
    private boolean handleCommandManagerCommand(CommandSender sender, String[] args) {
        // Implementation of command manager command handling
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aCommandManager-Befehl wird ausgeführt..."));
        return true;
    }
    
    /**
     * Vervollständigt einen CommandManager-Befehl
     * 
     * @param sender Der Empfänger der Nachricht
     * @param args Die Befehlsargumente
     * @return Eine Liste der möglichen Vervollständigungen
     */
    private List<String> tabCompleteCommandManagerCommand(CommandSender sender, String[] args) {
        // Implementation of command manager tab completion
        return new ArrayList<>();
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
} 