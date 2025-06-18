package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import com.essentialscore.ConsoleFormatter;
import com.essentialscore.api.module.ModuleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Command to manage modules
 */
public class ModuleCommand implements CommandExecutor, TabCompleter {
    private final ApiCore apiCore;
    private final ConsoleFormatter console;
    
    public ModuleCommand(ApiCore apiCore) {
        if (apiCore == null) {
            throw new IllegalArgumentException("ApiCore darf nicht null sein");
        }
        this.apiCore = apiCore;
        
        String prefix = apiCore.getConfig().getString("console.prefixes.module-command", "&8[&a&lModules&8]");
        boolean useColors = apiCore.getConfig().getBoolean("console.use-colors", true);
        boolean showTimestamps = apiCore.getConfig().getBoolean("console.show-timestamps", true);
        boolean useUnicodeSymbols = apiCore.getConfig().getBoolean("console.use-unicode-symbols", true);
        String stylePreset = apiCore.getConfig().getString("console.style-preset", "default");
        
        this.console = new ConsoleFormatter(
            apiCore.getLogger(),
            prefix,
            useColors,
            showTimestamps,
            useUnicodeSymbols,
            stylePreset
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show module list when no arguments
            listModules(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("list")) {
            listModules(sender);
            return true;
        } else if (subCommand.equals("info") || subCommand.equals("details")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a module name."));
                return true;
            }
            
            String moduleName = args[1];
            showModuleInfo(sender, moduleName);
            return true;
        } else if (subCommand.equals("enable")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a module name."));
                return true;
            }
            
            String moduleName = args[1];
            enableModule(sender, moduleName);
            return true;
        } else if (subCommand.equals("disable")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a module name."));
                return true;
            }
            
            String moduleName = args[1];
            disableModule(sender, moduleName);
            return true;
        } else if (subCommand.equals("reload")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a module name or 'all'."));
                return true;
            }
            
            String moduleName = args[1];
            if (moduleName.equalsIgnoreCase("all")) {
                reloadAllModules(sender);
            } else {
                reloadModule(sender, moduleName);
            }
            return true;
        }
        
        // Unknown subcommand, show help
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fModule Command Help:"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module list &8- &7List all modules"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module info <name> &8- &7Show module info"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module enable <name> &8- &7Enable a module"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module disable <name> &8- &7Disable a module"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module reload <name|all> &8- &7Reload module(s)"));
        
        return true;
    }
    
    /**
     * Lists all loaded modules
     */
    private void listModules(CommandSender sender) {
        Map<String, ModuleManager.ModuleInfo> modules = apiCore.getLoadedModules();
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fLoaded modules (&e" + modules.size() + "&f):"));
        
        if (modules.isEmpty()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7No modules loaded."));
            return;
        }
        
        for (Map.Entry<String, ModuleManager.ModuleInfo> entry : modules.entrySet()) {
            String moduleName = entry.getKey();
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &a" + moduleName));
        }
    }
    
    /**
     * Shows detailed information about a module
     */
    private void showModuleInfo(CommandSender sender, String moduleName) {
        Object moduleInfo = apiCore.getModuleInfo(moduleName);
        
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModule not found: &e" + moduleName));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fModule Information:"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &fName: &a" + moduleName));
        
        // Get version and description using reflection since we don't know the exact type
        try {
            Object version = moduleInfo.getClass().getMethod("getVersion").invoke(moduleInfo);
            Object description = moduleInfo.getClass().getMethod("getDescription").invoke(moduleInfo);
            Object jarFile = moduleInfo.getClass().getMethod("getJarFile").invoke(moduleInfo);
            
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &fVersion: &7" + version));
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &fDescription: &7" + description));
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &fJAR File: &7" + jarFile.toString()));
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cError retrieving module details: &7" + e.getMessage()));
        }
    }
    
    /**
     * Enables a module
     */
    private void enableModule(CommandSender sender, String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cUngültiger Modulname"));
            return;
        }

        // Prüfe ob das Modul bereits geladen ist
        if (apiCore.getModuleInfo(moduleName) != null) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModul ist bereits aktiviert: &e" + moduleName));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Aktiviere Modul &e" + moduleName + "&7..."));
        
        try {
            Object moduleManager = apiCore.getModuleManager();
            boolean success = false;
            
            // Versuche zuerst die enableModule Methode
            try {
                success = (boolean) moduleManager.getClass()
                    .getMethod("enableModule", String.class)
                    .invoke(moduleManager, moduleName);
            } catch (NoSuchMethodException e) {
                // Fallback: Versuche das Modul direkt zu laden
                java.io.File modulesDir = new java.io.File(apiCore.getDataFolder(), "modules");
                java.io.File moduleFile = new java.io.File(modulesDir, moduleName + ".jar");
                
                if (!moduleFile.exists()) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "&cModuldatei nicht gefunden: &e" + moduleName + ".jar"));
                    return;
                }
                
                if (!moduleFile.canRead()) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "&cKeine Leserechte für Moduldatei: &e" + moduleName + ".jar"));
                    return;
                }
                
                moduleManager.getClass()
                    .getMethod("loadModule", java.io.File.class)
                    .invoke(moduleManager, moduleFile);
                    
                success = apiCore.getModuleInfo(moduleName) != null;
            }
            
            if (success) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "&aModul erfolgreich aktiviert: &e" + moduleName));
                
                // Registriere Befehle und Listener neu
                try {
                    moduleManager.getClass()
                        .getMethod("synchronizeCommands")
                        .invoke(moduleManager);
                } catch (Exception e) {
                    console.warning("Fehler beim Synchronisieren der Befehle: " + e.getMessage());
                }
            } else {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "&cModul konnte nicht aktiviert werden: &e" + moduleName));
            }
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                "&cFehler beim Aktivieren des Moduls: &e" + errorMsg));
            console.error("Fehler beim Aktivieren von Modul " + moduleName + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Disables a module
     */
    private void disableModule(CommandSender sender, String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cUngültiger Modulname"));
            return;
        }

        // Prüfe ob das Modul geladen ist
        Object moduleInfo = apiCore.getModuleInfo(moduleName);
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModul ist nicht aktiviert: &e" + moduleName));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Deaktiviere Modul &e" + moduleName + "&7..."));
        
        try {
            // Entlade das Modul
            Object moduleManager = apiCore.getModuleManager();
            moduleManager.getClass()
                .getMethod("unloadModule", String.class)
                .invoke(moduleManager, moduleName);
            
            // Prüfe ob das Entladen erfolgreich war
            boolean success = apiCore.getModuleInfo(moduleName) == null;
            
            if (success) {
                // Aktualisiere Konfiguration
                java.io.File configFile = new java.io.File(apiCore.getDataFolder(), "modules/" + moduleName + ".yml");
                if (configFile.exists()) {
                    try {
                        org.bukkit.configuration.file.YamlConfiguration config = 
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
                        config.set("enabled", false);
                        config.save(configFile);
                    } catch (Exception e) {
                        console.warning("Fehler beim Aktualisieren der Modulkonfiguration: " + e.getMessage());
                    }
                }
                
                // Synchronisiere Befehle
                try {
                    moduleManager.getClass()
                        .getMethod("synchronizeCommands")
                        .invoke(moduleManager);
                } catch (Exception e) {
                    console.warning("Fehler beim Synchronisieren der Befehle: " + e.getMessage());
                }
                
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "&aModul erfolgreich deaktiviert: &e" + moduleName));
            } else {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "&cModul konnte nicht deaktiviert werden: &e" + moduleName));
            }
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                "&cFehler beim Deaktivieren des Moduls: &e" + errorMsg));
            console.error("Fehler beim Deaktivieren von Modul " + moduleName + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Reloads a module
     */
    private void reloadModule(CommandSender sender, String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cUngültiger Modulname"));
            return;
        }

        // Prüfe ob das Modul geladen ist
        Object moduleInfo = apiCore.getModuleInfo(moduleName);
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModul ist nicht aktiviert: &e" + moduleName));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Lade Modul &e" + moduleName + "&7 neu..."));
        
        try {
            // Speichere wichtige Informationen vor dem Neuladen
            java.io.File jarFile = (java.io.File) moduleInfo.getClass()
                .getMethod("getJarFile")
                .invoke(moduleInfo);
                
            // Hole aktuelle Version für Vergleich
            String oldVersion = (String) moduleInfo.getClass()
                .getMethod("getVersion")
                .invoke(moduleInfo);
            
            Object moduleManager = apiCore.getModuleManager();
            
            // Entlade das Modul
            moduleManager.getClass()
                .getMethod("unloadModule", String.class)
                .invoke(moduleManager, moduleName);
            
            // Kurze Pause für Ressourcenfreigabe
            Thread.sleep(100);
            
            // Lade das Modul neu
            moduleManager.getClass()
                .getMethod("loadModule", java.io.File.class)
                .invoke(moduleManager, jarFile);
            
            // Prüfe ob das Neuladen erfolgreich war
            moduleInfo = apiCore.getModuleInfo(moduleName);
            if (moduleInfo != null) {
                // Prüfe auf Versionsänderung
                String newVersion = (String) moduleInfo.getClass()
                    .getMethod("getVersion")
                    .invoke(moduleInfo);
                    
                if (!oldVersion.equals(newVersion)) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "&aModul erfolgreich neu geladen: &e" + moduleName + 
                        " &7(&e" + oldVersion + " &7→ &e" + newVersion + "&7)"));
                } else {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "&aModul erfolgreich neu geladen: &e" + moduleName));
                }
                
                // Synchronisiere Befehle
                try {
                    moduleManager.getClass()
                        .getMethod("synchronizeCommands")
                        .invoke(moduleManager);
                } catch (Exception e) {
                    console.warning("Fehler beim Synchronisieren der Befehle: " + e.getMessage());
                }
            } else {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "&cModul konnte nicht neu geladen werden: &e" + moduleName));
            }
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                "&cFehler beim Neuladen des Moduls: &e" + errorMsg));
            console.error("Fehler beim Neuladen von Modul " + moduleName + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
            
            // Versuche das Modul im Fehlerfall wiederherzustellen
            if (moduleInfo != null) {
                try {
                    Object moduleManager = apiCore.getModuleManager();
                    moduleManager.getClass()
                        .getMethod("loadModule", java.io.File.class)
                        .invoke(moduleManager, moduleInfo.getClass()
                            .getMethod("getJarFile")
                            .invoke(moduleInfo));
                            
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "&eModul wurde wiederhergestellt"));
                } catch (Exception recovery) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                        "&cModul konnte nicht wiederhergestellt werden!"));
                }
            }
        }
    }
    
    /**
     * Reloads all modules
     */
    private void reloadAllModules(CommandSender sender) {
        Map<String, ModuleManager.ModuleInfo> modules = apiCore.getLoadedModules();
        
        if (modules.isEmpty()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Keine Module zum Neuladen."));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
            "&7Lade &e" + modules.size() + " &7Module neu..."));
            
        // Erstelle eine Kopie der Module-Map um ConcurrentModification zu vermeiden
        Map<String, ModuleManager.ModuleInfo> modulesCopy = new HashMap<>(modules);
        int success = 0;
        int failed = 0;
        List<String> failedModules = new ArrayList<>();
        
        // Sammle Abhängigkeitsinformationen
        Map<String, List<String>> moduleDependencies = new HashMap<>();
        for (Map.Entry<String, ModuleManager.ModuleInfo> entry : modulesCopy.entrySet()) {
            try {
                ModuleManager.ModuleInfo info = entry.getValue();
                String moduleName = entry.getKey();
                
                // Versuche Abhängigkeiten zu laden
                java.lang.reflect.Method getDependenciesMethod = info.getClass()
                    .getMethod("getDependencies");
                Object dependenciesResult = getDependenciesMethod.invoke(info);
                List<String> dependencies = null;
                
                if (dependenciesResult instanceof List<?>) {
                    List<?> rawList = (List<?>) dependenciesResult;
                    dependencies = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item instanceof String) {
                            dependencies.add((String) item);
                        }
                    }
                }
                
                if (dependencies != null && !dependencies.isEmpty()) {
                    moduleDependencies.put(moduleName, dependencies);
                }
            } catch (Exception e) {
                console.warning("Konnte Abhängigkeiten für Modul " + entry.getKey() + 
                    " nicht laden: " + e.getMessage());
            }
        }
        
        // Sortiere Module basierend auf Abhängigkeiten
        List<String> orderedModules = new ArrayList<>();
        Set<String> processed = new java.util.HashSet<>();
        
        // Hilfsfunktion zum rekursiven Verarbeiten von Modulen
        class DependencyResolver {
            void resolve(String moduleName) {
                if (processed.contains(moduleName)) return;
                processed.add(moduleName);
                
                List<String> dependencies = moduleDependencies.get(moduleName);
                if (dependencies != null) {
                    for (String dep : dependencies) {
                        if (modulesCopy.containsKey(dep)) {
                            resolve(dep);
                        }
                    }
                }
                orderedModules.add(moduleName);
            }
        }
        
        // Verarbeite alle Module in der richtigen Reihenfolge
        DependencyResolver resolver = new DependencyResolver();
        for (String moduleName : modulesCopy.keySet()) {
            resolver.resolve(moduleName);
        }
        
        // Lade Module in der sortierten Reihenfolge neu
        Object moduleManager = apiCore.getModuleManager();
        
        for (String moduleName : orderedModules) {
            ModuleManager.ModuleInfo moduleInfo = modulesCopy.get(moduleName);
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                "&7Verarbeite Modul: &e" + moduleName));
                
            try {
                // Speichere JAR-Datei-Referenz
                java.io.File jarFile = (java.io.File) moduleInfo.getClass()
                    .getMethod("getJarFile")
                    .invoke(moduleInfo);
                
                // Entlade Modul
                moduleManager.getClass()
                    .getMethod("unloadModule", String.class)
                    .invoke(moduleManager, moduleName);
                
                // Kurze Pause für Ressourcenfreigabe
                Thread.sleep(50);
                
                // Lade Modul neu
                moduleManager.getClass()
                    .getMethod("loadModule", java.io.File.class)
                    .invoke(moduleManager, jarFile);
                
                // Prüfe Erfolg
                if (apiCore.getModuleInfo(moduleName) != null) {
                    success++;
                } else {
                    failed++;
                    failedModules.add(moduleName);
                    console.error("Modul " + moduleName + " konnte nicht neu geladen werden");
                }
            } catch (Exception e) {
                failed++;
                failedModules.add(moduleName);
                console.error("Fehler beim Neuladen von Modul " + moduleName + ": " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        
        // Synchronisiere Befehle nach dem Neuladen
        try {
            moduleManager.getClass()
                .getMethod("synchronizeCommands")
                .invoke(moduleManager);
        } catch (Exception e) {
            console.warning("Fehler beim Synchronisieren der Befehle: " + e.getMessage());
        }
        
        // Zeige Ergebnis
        if (failed == 0) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                "&aAlle Module erfolgreich neu geladen (&e" + success + "&a)."));
        } else {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                "&eModule neu geladen: &a" + success + " &eerfolgreich, &c" + failed + " &efehlgeschlagen."));
                
            if (!failedModules.isEmpty()) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                    "&cFehlgeschlagene Module: &e" + String.join(", ", failedModules)));
            }
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();
        
        try {
            if (args.length == 1) {
                // Cache der Unterbefehle für bessere Performance
                final List<String> SUB_COMMANDS = Arrays.asList("list", "info", "enable", "disable", "reload");
                String prefix = args[0].toLowerCase();
                
                return SUB_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(prefix))
                    .collect(Collectors.toList());
            } 
            
            if (args.length == 2) {
                String subCommand = args[0].toLowerCase();
                String prefix = args[1].toLowerCase();
                
                switch (subCommand) {
                    case "info":
                    case "disable":
                    case "reload":
                        // Vorgeladene Module vorschlagen
                        completions.addAll(apiCore.getLoadedModules().keySet().stream()
                            .filter(name -> name.toLowerCase().startsWith(prefix))
                            .collect(Collectors.toList()));
                        
                        // 'all' Option für reload
                        if (subCommand.equals("reload") && "all".startsWith(prefix)) {
                            completions.add("all");
                        }
                        break;
                        
                    case "enable":
                        try {
                            // Versuche verfügbare aber nicht geladene Module zu finden
                            Object moduleManager = apiCore.getModuleManager();
                            Object result = moduleManager.getClass()
                                .getMethod("getAvailableButNotLoadedModules")
                                .invoke(moduleManager);
                            
                            @SuppressWarnings("unchecked")
                            List<String> availableModules = (result instanceof List) ? (List<String>) result : null;
                            
                            if (availableModules != null) {
                                completions.addAll(availableModules.stream()
                                    .filter(name -> name.toLowerCase().startsWith(prefix))
                                    .collect(Collectors.toList()));
                            }
                        } catch (Exception e) {
                            // Fallback: Durchsuche das Module-Verzeichnis
                            java.io.File modulesDir = new java.io.File(apiCore.getDataFolder(), "modules");
                            if (modulesDir.exists() && modulesDir.isDirectory()) {
                                java.io.File[] files = modulesDir.listFiles((dir, name) -> 
                                    name.toLowerCase().endsWith(".jar") && 
                                    name.toLowerCase().replace(".jar", "").startsWith(prefix));
                                
                                if (files != null) {
                                    completions.addAll(Arrays.stream(files)
                                        .map(f -> f.getName().replace(".jar", ""))
                                        .collect(Collectors.toList()));
                                }
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            console.error("Fehler bei Tab-Completion: " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        return completions;
    }
}
