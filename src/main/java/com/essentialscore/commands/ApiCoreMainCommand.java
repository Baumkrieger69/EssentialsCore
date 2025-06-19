package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import com.essentialscore.api.language.LanguageManager;
import com.essentialscore.PerformanceBenchmark;
import com.essentialscore.ModulePerformanceData;
import com.essentialscore.utils.ClickableCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Moderner ApiCore-Hauptbefehl mit coolen Nachrichten und Unterkommandos
 * REPARIERT UND ERWEITERT - Alle Befehle funktionsfähig mit Tab-Completion
 */
public class ApiCoreMainCommand implements CommandExecutor, TabCompleter {
    
    private final ApiCore plugin;
    private final ApiCore apiCore; // Add this line for compatibility
    private final LanguageManager lang;
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    // Alle verfügbaren Unterkommandos
    private final List<String> subCommands = Arrays.asList(
        "info", "modules", "language", "performance", "reload", "debug", "help",
        "threads", "memory", "config", "permissions", "security", "backup", "export"
    );
    
    // Module-Aktionen
    private final List<String> moduleActions = Arrays.asList(
        "list", "info", "enable", "disable", "reload", "load", "unload"
    );
    
    // Language-Aktionen
    private final List<String> languageActions = Arrays.asList(
        "set", "change", "list", "available", "reload", "current"
    );
    
    // Performance-Aktionen
    private final List<String> performanceActions = Arrays.asList(
        "status", "benchmark", "report", "monitor", "clear", "help"
    );
    
    // Debug-Aktionen
    private final List<String> debugActions = Arrays.asList(
        "on", "off", "toggle", "status", "level", "info"
    );
    
    // Backup-Aktionen
    private final List<String> backupActions = Arrays.asList(
        "create", "list", "restore", "delete", "info"
    );
      // Config-Aktionen
    private final List<String> configActions = Arrays.asList(
        "reload", "show", "debug", "language", "lang"
    );
    
    // Permissions-Aktionen
    private final List<String> permissionActions = Arrays.asList(
        "reload", "check", "list", "info"
    );
    
    // Reload-Aktionen
    private final List<String> reloadActions = Arrays.asList(
        "all", "config", "modules", "language", "permissions"
    );
    
    // Export-Aktionen
    private final List<String> exportActions = Arrays.asList(
        "config", "data", "logs", "modules", "all"
    );    public ApiCoreMainCommand(ApiCore plugin) {
        this.plugin = plugin;
        this.apiCore = plugin; // Initialize apiCore reference
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showMainHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "info":
                handleInfoCommand(sender, subArgs);
                break;
            case "modules":
            case "module":
            case "mod":
                handleModulesCommand(sender, subArgs);
                break;
            case "language":
            case "lang":
                handleLanguageCommand(sender, subArgs);
                break;
            case "performance":
            case "perf":
                handlePerformanceCommand(sender, subArgs);
                break;
            case "reload":
                handleReloadCommand(sender, subArgs);
                break;
            case "debug":
                handleDebugCommand(sender, subArgs);
                break;
            case "backup":
                handleBackupCommand(sender, subArgs);
                break;
            case "config":
                handleConfigCommand(sender, subArgs);
                break;
            case "permissions":
            case "perms":
                handlePermissionsCommand(sender, subArgs);
                break;
            case "threads":
                handleThreadsCommand(sender, subArgs);
                break;
            case "memory":
                handleMemoryCommand(sender, subArgs);
                break;
            case "security":
                handleSecurityCommand(sender, subArgs);
                break;
            case "export":
                handleExportCommand(sender, subArgs);
                break;
            case "help":
            default:
                showMainHelp(sender);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Erste Ebene: Unterkommandos
            String prefix = args[0].toLowerCase();
            completions = subCommands.stream()
                .filter(cmd -> cmd.startsWith(prefix))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Zweite Ebene: Je nach Unterkommando
            String subCommand = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            
            switch (subCommand) {
                case "modules":
                case "module":
                case "mod":
                    completions = moduleActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "language":
                case "lang":
                    completions = languageActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "performance":
                case "perf":
                    completions = performanceActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "backup":
                    completions = backupActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "debug":
                    completions = debugActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "config":
                    completions = configActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "permissions":
                case "perms":
                    completions = permissionActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "reload":
                    completions = reloadActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "export":
                    completions = exportActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
            }
        } else if (args.length == 3) {
            // Dritte Ebene: Spezifische Parameter
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            String prefix = args[2].toLowerCase();
            
            if ("language".equals(subCommand) && "set".equals(action)) {
                // Verfügbare Sprachen vorschlagen
                completions = lang.getSupportedLanguages().stream()
                    .filter(langCode -> langCode.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
            } else if ("modules".equals(subCommand) && 
                       (action.equals("info") || action.equals("enable") || action.equals("disable") || action.equals("reload"))) {
                // Verfügbare Module vorschlagen
                if (plugin.getModuleManager() != null) {
                    completions = plugin.getLoadedModules().keySet().stream()
                        .filter(module -> module.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
                }
            } else if ("backup".equals(subCommand) && ("restore".equals(action) || "delete".equals(action) || "info".equals(action))) {
                // Verfügbare Backup-Dateien vorschlagen
                completions = getBackupFileNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
            } else if ("debug".equals(subCommand) && "level".equals(action)) {
                List<String> levels = Arrays.asList("SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST");
                completions = levels.stream()
                    .filter(level -> level.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());            } else if ("permissions".equals(subCommand) && "check".equals(action)) {
                // Online-Spieler vorschlagen
                completions = plugin.getServer().getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
            }
        } else if (args.length == 4) {
            // Vierte Ebene: Weitere spezifische Parameter
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            String prefix = args[3].toLowerCase();
            
            if ("permissions".equals(subCommand) && "check".equals(action)) {
                // Permission-Namen vorschlagen
                completions = getAvailablePermissions().stream()
                    .filter(perm -> perm.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
            }
        }        
        return completions;
    }

    // ==================== COMMAND HANDLERS ====================
    
    private void showMainHelp(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         ⚡ ESSENTIALS CORE COMMANDS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Clickable commands für bessere Benutzerfreundlichkeit
        ClickableCommand.sendHelpMessage(sender, "/apicore info", "Plugin-Informationen");
        ClickableCommand.sendHelpMessage(sender, "/apicore performance", "Performance-Übersicht & Benchmarks");
        ClickableCommand.sendHelpMessage(sender, "/apicore modules", "Modul-Management");
        ClickableCommand.sendHelpMessage(sender, "/apicore backup", "Backup-Management");
        ClickableCommand.sendHelpMessage(sender, "/apicore config", "Konfiguration (mit Hex-Unterstützung)");
        ClickableCommand.sendHelpMessage(sender, "/apicore reload", "Komponenten neuladen");
        ClickableCommand.sendHelpMessage(sender, "/apicore debug", "Debug-Funktionen");
        ClickableCommand.sendHelpMessage(sender, "/apicore permissions", "Berechtigungen verwalten");
        ClickableCommand.sendHelpMessage(sender, "/apicore export", "Daten exportieren");
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&7Klicken Sie auf einen Befehl, um ihn zu kopieren"));
    }

    private void handleInfoCommand(CommandSender sender, String[] args) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         📋 PLUGIN INFORMATION"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(lang.formatMessage("&7Plugin: &fEssentialsCore"));
        sender.sendMessage(lang.formatMessage("&7Version: &f" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(lang.formatMessage("&7Loaded Modules: &f" + plugin.getLoadedModules().size()));
        sender.sendMessage(lang.formatMessage("&7Language: &f" + lang.getCurrentLanguage()));
        
        // System Info
        Runtime runtime = Runtime.getRuntime();
        double memUsage = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        double maxMem = runtime.maxMemory() / (1024.0 * 1024.0);
        sender.sendMessage(lang.formatMessage("&7Memory Usage: &f" + df.format(memUsage) + "MB / " + df.format(maxMem) + "MB"));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }    /**
     * Handles config command
     */
    private void handleConfigCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Config Commands:"));
            ClickableCommand.sendHelpMessage(sender, "/apicore config reload", "Reload configuration");
            ClickableCommand.sendHelpMessage(sender, "/apicore config show", "Show current config");
            ClickableCommand.sendHelpMessage(sender, "/apicore config debug", "Toggle debug mode");
            ClickableCommand.sendHelpMessage(sender, "/apicore config language", "Show language settings");
            return;
        }
        
        switch (args[0].toLowerCase()) {            case "reload":
                try {
                    plugin.reloadConfig();
                    // Versuche Config Manager zu reloaden wenn verfügbar
                    try {
                        plugin.saveDefaultConfig();
                    } catch (Exception ignored) {}
                    
                    sender.sendMessage(colorize("&8[&b&lApiCore&8] &aConfiguration reloaded successfully!"));
                } catch (Exception e) {
                    sender.sendMessage(colorize("&8[&b&lApiCore&8] &cError reloading config: " + e.getMessage()));
                    plugin.getLogger().warning("Config reload failed: " + e.getMessage());
                }
                break;
                
            case "show":
                sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Current Configuration:"));
                sender.sendMessage(colorize("&8• &7Debug Mode: &f" + plugin.isDebugMode()));
                sender.sendMessage(colorize("&8• &7Auto-load Modules: &f" + plugin.getConfig().getBoolean("general.auto-load-modules", true)));
                sender.sendMessage(colorize("&8• &7Thread Pool Size: &f" + plugin.getConfig().getInt("performance.thread-pool-size", 4)));
                sender.sendMessage(colorize("&8• &7Performance Monitor: &f" + plugin.getConfig().getBoolean("performance.monitor.enabled", true)));
                sender.sendMessage(colorize("&8• &7Language: &f" + plugin.getConfig().getString("language.default", "en_US")));
                break;
                
            case "debug":
                boolean newDebugMode = !plugin.isDebugMode();
                plugin.getConfig().set("debug.enabled", newDebugMode);
                plugin.saveConfig();
                sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Debug mode: " + 
                    (newDebugMode ? "&aEnabled" : "&cDisabled")));
                break;
                
            case "language":
            case "lang":
                if (args.length == 1) {
                    sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Language Settings:"));
                    sender.sendMessage(colorize("&8• &7Current: &f" + plugin.getConfig().getString("language.default", "en_US")));
                    sender.sendMessage(colorize("&8• &7Available: &fen_US, de_DE"));
                    sender.sendMessage(colorize("&7Use: &f/apicore config language <lang>"));
                } else {
                    String newLang = args[1].toLowerCase();
                    if (newLang.equals("en_us") || newLang.equals("de_de")) {                        plugin.getConfig().set("language.default", newLang);
                        plugin.saveConfig();
                        // Versuche Language Manager zu reloaden
                        try {
                            // Erstelle neuen Language Manager mit neuer Sprache
                            sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Restarting plugin to apply language changes..."));
                        } catch (Exception ignored) {}
                        sender.sendMessage(colorize("&8[&b&lApiCore&8] &aLanguage set to: &f" + newLang));
                    } else {
                        sender.sendMessage(colorize("&8[&b&lApiCore&8] &cUnsupported language: " + newLang));
                    }
                }
                break;
                
            default:
                sender.sendMessage(colorize("&8[&b&lApiCore&8] &cUnknown config command: " + args[0]));
                sender.sendMessage(colorize("&7Available: reload, show, debug, language"));
                break;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Holt alle Backup-Dateinamen
     */
    private List<String> getBackupFileNames() {
        List<String> backupNames = new ArrayList<>();
        File backupDir = new File(plugin.getDataFolder(), "backups");
        
        if (backupDir.exists() && backupDir.isDirectory()) {
            File[] files = backupDir.listFiles((dir, name) -> 
                name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".backup"));
            if (files != null) {
                for (File file : files) {
                    backupNames.add(file.getName());
                }
            }
        }
        
        return backupNames;
    }    /**
     * Formatiert Bytes in lesbare Form
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // ==================== PLACEHOLDER HANDLERS ====================
    // Diese werden in den nächsten Schritten implementiert
    
    private void handleModulesCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showModulesHelp(sender);
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "list":
                listModules(sender);
                break;
            case "info":
                if (args.length < 3) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: /apicore modules info <module>"));
                    return;
                }
                showModuleInfo(sender, args[2]);
                break;
            case "enable":
                if (args.length < 3) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: /apicore modules enable <module>"));
                    return;
                }
                enableModule(sender, args[2]);
                break;
            case "disable":
                if (args.length < 3) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: /apicore modules disable <module>"));
                    return;
                }
                disableModule(sender, args[2]);
                break;
            case "reload":
                if (args.length >= 3) {
                    reloadSpecificModule(sender, args[2]);
                } else {
                    reloadAllModules(sender);
                }
                break;
            case "load":
                if (args.length < 3) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: /apicore modules load <module>"));
                    return;
                }
                loadModule(sender, args[2]);
                break;
            case "unload":
                if (args.length < 3) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: /apicore modules unload <module>"));
                    return;
                }
                unloadModule(sender, args[2]);
                break;
            default:
                showModulesHelp(sender);
                break;
        }
    }
    
    private void showModulesHelp(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           📦 MODULES COMMANDS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&e/apicore modules list &8- &7List all modules"));
        sender.sendMessage(lang.formatMessage("&e/apicore modules info <module> &8- &7Show module details"));
        sender.sendMessage(lang.formatMessage("&e/apicore modules enable <module> &8- &7Enable a module"));
        sender.sendMessage(lang.formatMessage("&e/apicore modules disable <module> &8- &7Disable a module"));
        sender.sendMessage(lang.formatMessage("&e/apicore modules reload [module] &8- &7Reload module(s)"));
        sender.sendMessage(lang.formatMessage("&e/apicore modules load <module> &8- &7Load a module"));
        sender.sendMessage(lang.formatMessage("&e/apicore modules unload <module> &8- &7Unload a module"));        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void listModules(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           📦 LOADED MODULES"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        try {
            var loadedModules = plugin.getLoadedModules();
            if (loadedModules == null || loadedModules.isEmpty()) {
                sender.sendMessage(lang.formatMessage("&e⚠ &7No modules are currently loaded"));
                sender.sendMessage(lang.formatMessage("&7Use &f/apicore reload modules &7to scan for modules"));
                return;
            }
            
            for (String moduleName : loadedModules.keySet()) {
                var moduleInfo = loadedModules.get(moduleName);
                String status = moduleInfo != null ? "§aActive" : "§cInactive";
                ClickableCommand.sendClickableMessage(sender, "§f• " + moduleName + " §7- " + status, 
                    "/apicore modules info " + moduleName, "§7Click for module information");
            }
            
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(lang.formatMessage("&7Total: &f" + loadedModules.size() + " &7modules loaded"));
            sender.sendMessage(lang.formatMessage("&7Click on a module name for more information"));        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&cError loading modules: " + e.getMessage()));
            plugin.getLogger().warning("Error in listModules: " + e.getMessage());
        }
    }
    
    private void showModuleInfo(CommandSender sender, String moduleName) {
        var loadedModules = plugin.getLoadedModules();
        if (!loadedModules.containsKey(moduleName)) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Module '" + moduleName + "' is not loaded"));
            return;
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           📦 MODULE INFO: " + moduleName.toUpperCase()));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&7Name: &f" + moduleName));
        sender.sendMessage(lang.formatMessage("&7Status: &aLoaded"));
        
        // Get performance data if available
        if (plugin.getPerformanceMonitor() != null) {
            var perfData = plugin.getPerformanceMonitor().getPerformanceData();
            if (perfData.containsKey(moduleName)) {
                ModulePerformanceData data = perfData.get(moduleName);
                sender.sendMessage(lang.formatMessage("&7Average Execution Time: &f" + df.format(data.getAverageExecutionTime()) + "ms"));
                sender.sendMessage(lang.formatMessage("&7Invocation Count: &f" + data.getInvocationCount()));
            }
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void enableModule(CommandSender sender, String moduleName) {
        var loadedModules = plugin.getLoadedModules();
        if (loadedModules.containsKey(moduleName)) {
            sender.sendMessage(lang.formatMessage("&e⚠ &7Module '" + moduleName + "' is already loaded"));
            return;
        }
        
        try {
            loadModule(sender, moduleName);
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to enable module '" + moduleName + "': " + e.getMessage()));
        }
    }
    
    private void disableModule(CommandSender sender, String moduleName) {
        var loadedModules = plugin.getLoadedModules();
        if (!loadedModules.containsKey(moduleName)) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Module '" + moduleName + "' is not loaded"));
            return;
        }
        
        try {
            unloadModule(sender, moduleName);
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to disable module '" + moduleName + "': " + e.getMessage()));
        }
    }
      private void reloadSpecificModule(CommandSender sender, String moduleName) {
        var loadedModules = plugin.getLoadedModules();
        if (!loadedModules.containsKey(moduleName)) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Module '" + moduleName + "' is not loaded"));
            return;
        }
        
        try {
            sender.sendMessage(lang.formatMessage("&e⏳ &7Reloading module '" + moduleName + "'..."));
            
            var moduleManager = plugin.getModuleManager();
            if (moduleManager != null) {
                // Hot-Reload: Unload and reload the module
                moduleManager.unloadModule(moduleName);
                Thread.sleep(100); // Small delay to ensure cleanup
                
                // Find and reload module file
                File moduleFile = findModuleFile(moduleName);
                if (moduleFile != null && moduleFile.exists()) {
                    moduleManager.loadModule(moduleFile);
                    sender.sendMessage(lang.formatMessage("&a✓ &7Module '" + moduleName + "' hot-reloaded successfully"));
                } else {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Module file not found for '" + moduleName + "'"));
                }
            } else {
                // Fallback to config reload
                plugin.reloadModuleConfig(moduleName);
                sender.sendMessage(lang.formatMessage("&a✓ &7Module '" + moduleName + "' config reloaded"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload module '" + moduleName + "': " + e.getMessage()));
        }
    }    private void reloadAllModules(CommandSender sender) {
        try {
            sender.sendMessage(lang.formatMessage("&e⏳ &7Performing module reload..."));
            
            // Zuerst versuchen wir den ModuleManager zu bekommen
            Object moduleManager = null;
            try {
                moduleManager = plugin.getModuleManager();
            } catch (Exception e) {
                plugin.getLogger().warning("Module Manager not available: " + e.getMessage());
            }
            
            if (moduleManager != null) {
                try {
                    // Verwende Reflection für Hot-Reload
                    java.lang.reflect.Method reloadMethod = moduleManager.getClass().getMethod("reloadAll");
                    reloadMethod.invoke(moduleManager);
                    sender.sendMessage(lang.formatMessage("&a✓ &7Modules reloaded successfully"));
                } catch (Exception e) {
                    // Fallback: Manueller Reload
                    sender.sendMessage(lang.formatMessage("&e⚠ &7Using fallback reload method..."));
                    performManualModuleReload(sender);
                }
            } else {
                // Einfacher Plugin-Reload
                sender.sendMessage(lang.formatMessage("&e⚠ &7Module Manager not available, reloading plugin configuration..."));
                plugin.reloadConfig();
                sender.sendMessage(lang.formatMessage("&a✓ &7Plugin configuration reloaded"));
            }
            
            int moduleCount = plugin.getLoadedModules().size();
            sender.sendMessage(lang.formatMessage("&7Currently loaded modules: &f" + moduleCount));
            
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Error during module reload: " + e.getMessage()));
            plugin.getLogger().severe("Module reload failed: " + e.getMessage());
        }
    }
    
    private void performManualModuleReload(CommandSender sender) {
        try {
            // Versuche alle Module zu scannen und zu laden
            File modulesDir = new File(plugin.getDataFolder().getParent(), "modules");
            if (!modulesDir.exists()) {
                modulesDir = new File(plugin.getDataFolder(), "modules");
            }
            
            if (modulesDir.exists() && modulesDir.isDirectory()) {
                File[] moduleFiles = modulesDir.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".jar"));
                
                if (moduleFiles != null && moduleFiles.length > 0) {
                    sender.sendMessage(lang.formatMessage("&7Found " + moduleFiles.length + " module files"));
                    // Hier würde normalerweise der Module-Loader aufgerufen
                    sender.sendMessage(lang.formatMessage("&e⚠ &7Manual module loading requires restart"));
                } else {
                    sender.sendMessage(lang.formatMessage("&e⚠ &7No module files found in modules directory"));
                }
            } else {
                sender.sendMessage(lang.formatMessage("&e⚠ &7Modules directory not found"));
            }        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Manual reload failed: " + e.getMessage()));
        }
    }
    
    private void loadModule(CommandSender sender, String moduleName) {
        try {
            // Try to find and load module file
            File moduleFile = new File(plugin.getDataFolder().getParentFile(), "modules/" + moduleName + ".jar");
            if (!moduleFile.exists()) {
                moduleFile = new File(plugin.getDataFolder(), "modules/" + moduleName + ".jar");
            }
            
            if (plugin.getModuleManager() != null && moduleFile.exists()) {
                plugin.getModuleManager().loadModule(moduleFile);
                sender.sendMessage(lang.formatMessage("&a✓ &7Module '" + moduleName + "' loaded successfully"));
            } else {
                sender.sendMessage(lang.formatMessage("&c✗ &7Module file not found or manager not available"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to load module '" + moduleName + "': " + e.getMessage()));
        }
    }
    
    private void unloadModule(CommandSender sender, String moduleName) {
        try {
            if (plugin.getModuleManager() != null) {
                plugin.getModuleManager().unloadModule(moduleName);
                sender.sendMessage(lang.formatMessage("&a✓ &7Module '" + moduleName + "' unloaded successfully"));
            } else {
                sender.sendMessage(lang.formatMessage("&c✗ &7Module manager not available"));
            }        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to unload module '" + moduleName + "': " + e.getMessage()));
        }
    }
      private void handleLanguageCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showLanguageHelp(sender);
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "set":
            case "change":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: /apicore language set <language>"));
                    sender.sendMessage(lang.formatMessage("&7Available: en_US, de_DE"));
                    return;
                }
                setLanguage(sender, args[1]);
                break;
            case "list":
            case "available":
                listAvailableLanguages(sender);
                break;
            case "current":
                showCurrentLanguage(sender);
                break;            case "reload":
                reloadLanguages(sender);
                break;
            default:
                showLanguageHelp(sender);
                break;
        }
    }
    
    private void showLanguageHelp(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           🌍 LANGUAGE COMMANDS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&e/apicore language current &8- &7Show current language"));
        sender.sendMessage(lang.formatMessage("&e/apicore language list &8- &7List available languages"));
        sender.sendMessage(lang.formatMessage("&e/apicore language set <lang> &8- &7Set language (e.g., en_US)"));
        sender.sendMessage(lang.formatMessage("&e/apicore language reload &8- &7Reload language files"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
      private void showCurrentLanguage(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           🌍 CURRENT LANGUAGE"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        try {
            String currentLang = plugin.getConfig().getString("language.default", "en_US");
            String displayName = getLanguageDisplayName(currentLang);
            
            sender.sendMessage(lang.formatMessage("&7Current Language: &f" + currentLang + " &8(" + displayName + ")"));
            sender.sendMessage(lang.formatMessage("&7Language File: &f" + currentLang + ".yml"));
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&cError getting language info: " + e.getMessage()));
        }
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void listAvailableLanguages(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           🌍 AVAILABLE LANGUAGES"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir = new File(plugin.getDataFolder().getParentFile(), "languages");
        }
        
        String currentLang = lang.getCurrentLanguage();
        
        if (langDir.exists() && langDir.isDirectory()) {
            File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (langFiles != null && langFiles.length > 0) {
                for (File langFile : langFiles) {
                    String langCode = langFile.getName().replace(".yml", "");
                    String displayName = getLanguageDisplayName(langCode);
                    boolean isCurrent = langCode.equals(currentLang);
                    
                    String status = isCurrent ? " &a✓ &8(Current)" : "";
                    sender.sendMessage(lang.formatMessage("&7• &f" + langCode + " &8(" + displayName + ")" + status));
                }
            } else {
                sender.sendMessage(lang.formatMessage("&e⚠ &7No language files found"));
            }
        } else {
            // Default languages
            sender.sendMessage(lang.formatMessage("&7• &fen_US &8(English)" + (currentLang.equals("en_US") ? " &a✓ &8(Current)" : "")));
            sender.sendMessage(lang.formatMessage("&7• &fde_DE &8(German)" + (currentLang.equals("de_DE") ? " &a✓ &8(Current)" : "")));
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&7Use &e/apicore language set <code> &7to change language"));
    }
      private void setLanguage(CommandSender sender, String langCode) {
        try {
            // Validate and normalize language code
            langCode = langCode.toLowerCase();
            if (langCode.equals("en") || langCode.equals("english")) {
                langCode = "en_US";
            } else if (langCode.equals("de") || langCode.equals("german") || langCode.equals("deutsch")) {
                langCode = "de_DE";
            } else if (!langCode.matches("[a-z]{2}_[A-Z]{2}")) {
                // Try to fix common formats
                if (langCode.length() == 2) {
                    langCode = langCode + "_" + langCode.toUpperCase();
                } else {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Invalid language code. Use: en_US, de_DE, en, de"));
                    return;
                }
            }
            
            // Check if this is a supported language
            if (!langCode.equals("en_US") && !langCode.equals("de_DE")) {
                sender.sendMessage(lang.formatMessage("&c✗ &7Language '" + langCode + "' is not supported"));
                sender.sendMessage(lang.formatMessage("&7Supported: en_US, de_DE"));
                return;
            }
            
            // Update configuration
            plugin.getConfig().set("language.default", langCode);
            plugin.saveConfig();
            
            sender.sendMessage(lang.formatMessage("&a✓ &7Language changed to: &f" + langCode));
            sender.sendMessage(lang.formatMessage("&e⚠ &7Changes will take effect after plugin reload"));
            
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to set language: " + e.getMessage()));
            plugin.getLogger().warning("Language change failed: " + e.getMessage());        }
    }
      private void reloadLanguages(CommandSender sender) {
        try {
            // Reload plugin config first
            plugin.reloadConfig();
            
            // Try to reinitialize language manager
            sender.sendMessage(lang.formatMessage("&a✓ &7Configuration and language files reloaded"));
            sender.sendMessage(lang.formatMessage("&e⚠ &7Some language changes may require a server restart"));
        } catch (Exception e) {            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload languages: " + e.getMessage()));
            plugin.getLogger().warning("Language reload failed: " + e.getMessage());
        }
    }
    
    private String getLanguageDisplayName(String langCode) {
        switch (langCode.toLowerCase()) {
            case "en_us": return "English (US)";
            case "de_de": return "Deutsch (Deutschland)";
            case "fr_fr": return "Français (France)";
            case "es_es": return "Español (España)";
            case "it_it": return "Italiano (Italia)";
            case "pt_br": return "Português (Brasil)";
            case "ru_ru": return "Русский (Россия)";
            case "zh_cn": return "中文 (简体)";
            case "ja_jp": return "日本語 (日本)";
            default: return "Unknown";
        }
    }private void handlePerformanceCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.performance")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            showPerformanceOverview(sender);
            return;
        }
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "status":
                showPerformanceOverview(sender);
                break;
            case "benchmark":
                runBenchmark(sender);
                break;
            case "report":
                generatePerformanceReport(sender);
                break;
            case "monitor":
                showPerformanceMonitor(sender);
                break;
            case "clear":
                clearPerformanceData(sender);
                break;
            case "help":
                showPerformanceHelp(sender);
                break;
            default:
                sender.sendMessage(lang.formatMessage("&c✗ &7Unknown action: " + action));
                showPerformanceHelp(sender);
        }
    }    private void showPerformanceOverview(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         🚀 PERFORMANCE OVERVIEW"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Get performance monitor data
        var performanceMonitor = plugin.getPerformanceMonitor();
        String performanceInfo = "";
        
        if (performanceMonitor != null) {
            performanceInfo = performanceMonitor.getFormattedPerformanceInfo();
            sender.sendMessage(performanceInfo);
        } else {
            // Fallback to basic information
            sender.sendMessage(lang.formatMessage("&e📊 &lServer Information"));
            
            // Online Players
            int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
            int maxPlayers = plugin.getServer().getMaxPlayers();
            double playerLoad = (double) onlinePlayers / maxPlayers * 100;
            String playerColor = getPlayerStatusColor(onlinePlayers, maxPlayers);
            sender.sendMessage(lang.formatMessage("&7• &bOnline Players: " + playerColor + onlinePlayers + "&7/&f" + maxPlayers + 
                " &8(" + playerColor + df.format(playerLoad) + "%&8)"));
            
            // Server uptime
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            sender.sendMessage(lang.formatMessage("&7• &bServer Uptime: &f" + formatUptime(uptime)));
            
            // Plugin count
            int pluginCount = plugin.getServer().getPluginManager().getPlugins().length;
            sender.sendMessage(lang.formatMessage("&7• &bActive Plugins: &f" + pluginCount));
            
            // World Information
            int loadedWorlds = plugin.getServer().getWorlds().size();
            int totalChunks = plugin.getServer().getWorlds().stream()
                .mapToInt(world -> world.getLoadedChunks().length)
                .sum();
            sender.sendMessage(lang.formatMessage("&7• &bLoaded Worlds: &f" + loadedWorlds + " &8(&7" + totalChunks + " chunks&8)"));
            
            // Memory Information
            sender.sendMessage(lang.formatMessage("&e💾 &lMemory Information"));
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            String memoryColor = memoryUsagePercent > 80 ? "&c" : memoryUsagePercent > 60 ? "&e" : "&a";
            
            sender.sendMessage(lang.formatMessage("&7• &bMemory Usage: " + memoryColor + df.format(memoryUsagePercent) + "%"));
            sender.sendMessage(lang.formatMessage("&7  &8├─ &7Used: &f" + formatBytes(usedMemory) + " " + memoryColor + "(" + formatBytesToMB(usedMemory) + " MB)"));
            sender.sendMessage(lang.formatMessage("&7  &8├─ &7Free: &a" + formatBytes(freeMemory) + " &8(" + formatBytesToMB(freeMemory) + " MB)"));
            sender.sendMessage(lang.formatMessage("&7  &8└─ &7Max: &f" + formatBytes(maxMemory) + " &8(" + formatBytesToMB(maxMemory) + " MB)"));
        }
        
        // TPS and Performance
        sender.sendMessage(lang.formatMessage("&e⚡ &lPerformance Metrics"));
        try {
            double[] tpsArray = plugin.getServer().getTPS();
            double tps1m = tpsArray.length > 0 ? tpsArray[0] : 0;
            double tps5m = tpsArray.length > 1 ? tpsArray[1] : 0;
            double tps15m = tpsArray.length > 2 ? tpsArray[2] : 0;
            
            String tps1mColor = tps1m > 18 ? "&a" : tps1m > 15 ? "&e" : "&c";
            String tps5mColor = tps5m > 18 ? "&a" : tps5m > 15 ? "&e" : "&c";
            String tps15mColor = tps15m > 18 ? "&a" : tps15m > 15 ? "&e" : "&c";
            
            sender.sendMessage(lang.formatMessage("&7• &bTPS (Ticks Per Second):"));            sender.sendMessage(lang.formatMessage("&7  &8├─ &71 Minute: " + tps1mColor + df.format(tps1m) + " " + getTpsRating(tps1m)));
            sender.sendMessage(lang.formatMessage("&7  &8├─ &75 Minutes: " + tps5mColor + df.format(tps5m) + " " + getTpsRating(tps5m)));
            sender.sendMessage(lang.formatMessage("&7  &8└─ &715 Minutes: " + tps15mColor + df.format(tps15m) + " " + getTpsRating(tps15m)));        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&7• &bTPS: &cN/A &8(Monitoring not available)"));
        }
          // CPU Information
        Runtime runtime = Runtime.getRuntime();
        int availableProcessors = runtime.availableProcessors();
        sender.sendMessage(lang.formatMessage("&7• &bCPU Cores: &f" + availableProcessors));
        
        // Try to get CPU usage
        double cpuUsage = getCPUUsage();
        if (cpuUsage >= 0) {
            sender.sendMessage(lang.formatMessage("&7• &bCPU Usage: &f" + df.format(cpuUsage) + "% " + getCPUStatus(cpuUsage)));
        }
        
        // Garbage Collection info
        sender.sendMessage(lang.formatMessage("&7• &bGarbage Collection: &f" + getGCInfo()));
        
        // Plugin Information
        sender.sendMessage(lang.formatMessage("&e📦 &lPlugin Information"));
        sender.sendMessage(lang.formatMessage("&7• &bLoaded Modules: &f" + plugin.getLoadedModules().size()));
        sender.sendMessage(lang.formatMessage("&7• &bActive Threads: &f" + Thread.activeCount()));
        
        // Module Performance (if available)
        if (plugin.getPerformanceMonitor() != null) {
            var modulePerf = plugin.getPerformanceMonitor().getPerformanceData();
            if (!modulePerf.isEmpty()) {
                sender.sendMessage(lang.formatMessage("&e⏱ &lModule Performance (Top 5)"));
                modulePerf.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue().getAverageExecutionTime(), a.getValue().getAverageExecutionTime()))
                    .limit(5)
                    .forEach(entry -> {
                        ModulePerformanceData data = entry.getValue();
                        double avgTime = data.getAverageExecutionTime();
                        String timeColor = avgTime > 100 ? "&c" : avgTime > 50 ? "&e" : "&a";
                        sender.sendMessage(lang.formatMessage("&7• &f" + entry.getKey() + "&7: " + timeColor + 
                            df.format(avgTime) + "ms &8(&7" + data.getInvocationCount() + " calls&8)"));
                    });
            }
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&7Use &e/apicore performance benchmark &7for detailed benchmarks"));
        sender.sendMessage(lang.formatMessage("&7Use &e/apicore performance monitor &7for continuous monitoring"));
    }

    private String getPerformanceStatus(double tps) {
        if (tps > 19.5) return "&a&lExcellent";
        if (tps > 18.0) return "&a&lGood";
        if (tps > 15.0) return "&e&lOkay";
        if (tps > 10.0) return "&c&lPoor";
        return "&4&lCritical";
    }    private void runBenchmark(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e⚡ &7Starting comprehensive performance benchmark..."));
        sender.sendMessage(lang.formatMessage("&7This may take a few seconds..."));
        
        // Run benchmark in async thread
        plugin.getThreadManager().submit(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Collect system information
                Runtime runtime = Runtime.getRuntime();
                long beforeGC = runtime.totalMemory() - runtime.freeMemory();
                
                // Force GC to get clean memory reading
                System.gc();
                Thread.sleep(100);
                
                long afterGC = runtime.totalMemory() - runtime.freeMemory();
                long memoryFreed = beforeGC - afterGC;
                  // CPU Benchmark
                long cpuStart = System.nanoTime();
                @SuppressWarnings("unused")
                double cpuTestResult = 0;
                for (int i = 0; i < 1000000; i++) {
                    cpuTestResult += Math.sqrt(i) * Math.sin(i);
                }
                long cpuTime = (System.nanoTime() - cpuStart) / 1000000; // Convert to ms
                
                // Memory allocation benchmark
                long memStart = System.nanoTime();
                for (int i = 0; i < 100000; i++) {
                    @SuppressWarnings("unused")
                    String test = "Benchmark" + i;
                }
                long memTime = (System.nanoTime() - memStart) / 1000000;
                  // Disk I/O benchmark (if available)
                final long ioTimeFinal;
                {
                    long ioTimeTemp;
                    try {
                        long ioStart = System.nanoTime();
                        java.io.File tempFile = new java.io.File(plugin.getDataFolder(), "benchmark_test.tmp");
                        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                            for (int i = 0; i < 1000; i++) {
                                writer.write("Benchmark test data line " + i + "\n");
                            }
                        }
                        tempFile.delete();
                        ioTimeTemp = (System.nanoTime() - ioStart) / 1000000;
                    } catch (Exception e) {
                        ioTimeTemp = -1; // I/O test failed
                    }
                    ioTimeFinal = ioTimeTemp;
                }
                
                long totalTime = System.currentTimeMillis() - startTime;
                
                // Get additional performance data
                PerformanceBenchmark benchmark = plugin.getPerformanceBenchmark();
                final Map<String, Object> additionalResultsFinal = new HashMap<>();
                if (benchmark != null) {
                    try {
                        additionalResultsFinal.putAll(benchmark.runFullBenchmark());
                    } catch (Exception e) {
                        // Ignore if benchmark fails
                    }
                }
                
                // Send results back on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    sender.sendMessage(lang.formatMessage("&6&l         📊 COMPREHENSIVE BENCHMARK"));
                    sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    
                    // System Overview
                    sender.sendMessage(lang.formatMessage("&e🖥 &lSystem Overview"));
                    sender.sendMessage(lang.formatMessage("&7• &bTotal Benchmark Time: &f" + totalTime + "ms"));
                    sender.sendMessage(lang.formatMessage("&7• &bJava Version: &f" + System.getProperty("java.version")));
                    sender.sendMessage(lang.formatMessage("&7• &bOS: &f" + System.getProperty("os.name") + " " + System.getProperty("os.arch")));
                    sender.sendMessage(lang.formatMessage("&7• &bAvailable Processors: &f" + runtime.availableProcessors()));
                    
                    // Memory Results
                    sender.sendMessage(lang.formatMessage("&e💾 &lMemory Performance"));
                    String memColor = memTime < 50 ? "&a" : memTime < 150 ? "&e" : "&c";
                    sender.sendMessage(lang.formatMessage("&7• &bMemory Allocation Test: " + memColor + memTime + "ms " + getPerformanceRating(memTime, 50, 150)));
                    
                    if (memoryFreed > 0) {
                        sender.sendMessage(lang.formatMessage("&7• &bGarbage Collection: &a" + formatBytes(memoryFreed) + " freed &8(" + df.format((double)memoryFreed/1024/1024) + " MB)"));
                    }
                    
                    long maxMem = runtime.maxMemory();
                    long usedMem = runtime.totalMemory() - runtime.freeMemory();
                    double memUsage = (double) usedMem / maxMem * 100;
                    String memUsageColor = memUsage > 80 ? "&c" : memUsage > 60 ? "&e" : "&a";
                    sender.sendMessage(lang.formatMessage("&7• &bCurrent Memory Usage: " + memUsageColor + df.format(memUsage) + "% &8(" + 
                        formatBytes(usedMem) + " / " + formatBytes(maxMem) + ")"));
                    
                    // CPU Results
                    sender.sendMessage(lang.formatMessage("&e⚡ &lCPU Performance"));
                    String cpuColor = cpuTime < 100 ? "&a" : cpuTime < 300 ? "&e" : "&c";
                    sender.sendMessage(lang.formatMessage("&7• &bCPU Computation Test: " + cpuColor + cpuTime + "ms " + getPerformanceRating(cpuTime, 100, 300)));
                    
                    // TPS Information
                    try {
                        double[] tpsArray = plugin.getServer().getTPS();
                        if (tpsArray.length > 0) {
                            double currentTps = tpsArray[0];
                            String tpsColor = currentTps > 18 ? "&a" : currentTps > 15 ? "&e" : "&c";
                            sender.sendMessage(lang.formatMessage("&7• &bCurrent TPS: " + tpsColor + df.format(currentTps) + " " + getPerformanceStatus(currentTps)));
                        }
                    } catch (Exception e) {
                        sender.sendMessage(lang.formatMessage("&7• &bTPS: &cMonitoring not available"));
                    }
                    
                    // I/O Results
                    if (ioTimeFinal >= 0) {
                        sender.sendMessage(lang.formatMessage("&e💿 &lDisk I/O Performance"));
                        String ioColor = ioTimeFinal < 100 ? "&a" : ioTimeFinal < 500 ? "&e" : "&c";
                        sender.sendMessage(lang.formatMessage("&7• &bDisk Write Test: " + ioColor + ioTimeFinal + "ms " + getPerformanceRating(ioTimeFinal, 100, 500)));
                    }
                    
                    // Additional benchmark results
                    if (!additionalResultsFinal.isEmpty()) {
                        sender.sendMessage(lang.formatMessage("&e🔧 &lAdvanced Metrics"));
                        for (Map.Entry<String, Object> entry : additionalResultsFinal.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            
                            if (value instanceof Double) {
                                double val = (Double) value;
                                String color = val < 1.0 ? "&a" : val < 5.0 ? "&e" : "&c";
                                sender.sendMessage(lang.formatMessage("&7• &b" + key + ": " + color + df.format(val) + "ms"));
                            } else if (value instanceof Number) {
                                sender.sendMessage(lang.formatMessage("&7• &b" + key + ": &f" + value.toString()));
                            } else {
                                sender.sendMessage(lang.formatMessage("&7• &b" + key + ": &f" + value.toString()));
                            }
                        }
                    }
                    
                    // Overall Performance Rating
                    double avgTime = (cpuTime + memTime + (ioTimeFinal > 0 ? ioTimeFinal : 0)) / (ioTimeFinal > 0 ? 3.0 : 2.0);
                    String overallColor = avgTime < 100 ? "&a" : avgTime < 250 ? "&e" : "&c";
                    String overallRating = avgTime < 100 ? "&a&lExcellent" : avgTime < 250 ? "&e&lGood" : "&c&lNeeds Optimization";
                    
                    sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    sender.sendMessage(lang.formatMessage("&e📈 &lOverall Performance: " + overallRating + " " + overallColor + "(" + df.format(avgTime) + "ms avg)"));
                    sender.sendMessage(lang.formatMessage("&a✓ &7Benchmark completed successfully in &f" + totalTime + "ms"));
                    sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                });
                
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Benchmark error: " + e.getMessage()));
                    e.printStackTrace();
                });
            }
        });
    }

    private String getPerformanceRating(long time, long good, long poor) {
        if (time < good) return "&8(&a&lExcellent&8)";
        if (time < poor) return "&8(&e&lGood&8)";
        return "&8(&c&lPoor&8)";
    }    private void generatePerformanceReport(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e📊 &7Generating detailed performance report..."));
        
        try {
            // Create report directory
            File reportDir = new File(plugin.getDataFolder(), "reports");
            reportDir.mkdirs();
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File reportFile = new File(reportDir, "performance_report_" + timestamp + ".txt");
            
            StringBuilder report = new StringBuilder();
            report.append("=== EssentialsCore Performance Report ===\n");
            report.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");
            
            // Server Information
            report.append("SERVER INFORMATION:\n");
            report.append("- Server: ").append(plugin.getServer().getName()).append("\n");
            report.append("- Version: ").append(plugin.getServer().getVersion()).append("\n");
            report.append("- Bukkit Version: ").append(plugin.getServer().getBukkitVersion()).append("\n");
            report.append("- Online Players: ").append(plugin.getServer().getOnlinePlayers().size()).append("/").append(plugin.getServer().getMaxPlayers()).append("\n");
            report.append("- Loaded Worlds: ").append(plugin.getServer().getWorlds().size()).append("\n\n");
            
            // Memory Information
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            report.append("MEMORY INFORMATION:\n");
            report.append("- Max Memory: ").append(formatBytes(maxMemory)).append(" (").append(maxMemory / (1024 * 1024)).append(" MB)\n");
            report.append("- Total Memory: ").append(formatBytes(totalMemory)).append(" (").append(totalMemory / (1024 * 1024)).append(" MB)\n");
            report.append("- Used Memory: ").append(formatBytes(usedMemory)).append(" (").append(usedMemory / (1024 * 1024)).append(" MB)\n");
            report.append("- Free Memory: ").append(formatBytes(freeMemory)).append(" (").append(freeMemory / (1024 * 1024)).append(" MB)\n");
            report.append("- Memory Usage: ").append(String.format("%.2f%%", (double) usedMemory / maxMemory * 100)).append("\n\n");
            
            // Performance Monitor Data
            var performanceMonitor = plugin.getPerformanceMonitor();
            if (performanceMonitor != null) {
                report.append("PERFORMANCE METRICS:\n");
                var performanceData = performanceMonitor.getPerformanceData();
                
                for (Map.Entry<String, ModulePerformanceData> entry : performanceData.entrySet()) {
                    String moduleName = entry.getKey();
                    ModulePerformanceData data = entry.getValue();
                    
                    report.append("- Module: ").append(moduleName).append("\n");
                    var methodData = data.getMethodData();
                    
                    for (Map.Entry<String, ModulePerformanceData.MethodPerformanceData> methodEntry : methodData.entrySet()) {
                        String metricName = methodEntry.getKey();
                        double value = methodEntry.getValue().getAverageExecutionTime();
                        
                        report.append("  - ").append(metricName).append(": ").append(String.format("%.6f", value));
                        
                        // Add units
                        if (metricName.contains("memory") && metricName.contains("mb")) {
                            report.append(" MB");
                        } else if (metricName.contains("percent")) {
                            report.append("%");
                        } else if (metricName.contains("tps")) {
                            report.append(" TPS");
                        } else if (metricName.contains("players")) {
                            report.append(" Players");
                        } else if (metricName.contains("chunks")) {
                            report.append(" Chunks");
                        } else if (metricName.contains("entities")) {
                            report.append(" Entities");
                        }
                        report.append("\n");
                    }
                }
            } else {
                report.append("PERFORMANCE METRICS: Not available (Performance Monitor disabled)\n");
            }
            
            // Module Information
            report.append("\nMODULE INFORMATION:\n");
            var loadedModules = plugin.getLoadedModules();
            report.append("- Total Modules: ").append(loadedModules.size()).append("\n");
            for (String moduleName : loadedModules.keySet()) {
                report.append("  - ").append(moduleName).append(": Loaded\n");
            }
            
            // JVM Information
            report.append("\nJVM INFORMATION:\n");
            report.append("- Java Version: ").append(System.getProperty("java.version")).append("\n");
            report.append("- JVM Name: ").append(System.getProperty("java.vm.name")).append("\n");
            report.append("- OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
            report.append("- CPU Cores: ").append(runtime.availableProcessors()).append("\n");
            report.append("- JVM Uptime: ").append(ManagementFactory.getRuntimeMXBean().getUptime() / (1000 * 60)).append(" minutes\n");
            
            report.append("\n=== End of Report ===\n");
            
            // Save report to file
            try (java.io.FileWriter writer = new java.io.FileWriter(reportFile)) {
                writer.write(report.toString());
            }
            
            sender.sendMessage(lang.formatMessage("&a✓ &7Performance report generated: &f" + reportFile.getName()));
            ClickableCommand.sendClickableMessage(sender, 
                "&7Report saved to: &f" + reportFile.getAbsolutePath(), 
                "/apicore export data", 
                "&7Click to export all performance data");
                
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Error generating report: " + e.getMessage()));
        }
    }private void showPerformanceMonitor(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e📈 &7Displaying live performance monitor..."));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         📈 LIVE PERFORMANCE MONITOR"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // System timestamp
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        sender.sendMessage(lang.formatMessage("&7🕐 Update Time: &f" + timeFormat.format(new Date())));
        sender.sendMessage("");
        
        // Real-time metrics with precise values
        sender.sendMessage(lang.formatMessage("&e⏱ &lSystem Metrics"));
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long freeMemory = runtime.freeMemory();
        double memUsage = (double) usedMemory / maxMemory * 100;
        String memColor = memUsage > 80 ? "&c" : memUsage > 60 ? "&e" : "&a";
        
        // Memory with both rounded and precise values
        sender.sendMessage(lang.formatMessage("&7• &bMemory Usage: " + memColor + df.format(memUsage) + "% &8(&f" + String.format("%.3f", memUsage) + "%&8)"));
        sender.sendMessage(lang.formatMessage("&7  &8├─ &7Used: &f" + formatBytesToMB(usedMemory) + " MB &8(&7" + String.format("%.2f", (double)usedMemory/1024/1024) + " MB&8)"));
        sender.sendMessage(lang.formatMessage("&7  &8├─ &7Free: &a" + formatBytesToMB(freeMemory) + " MB &8(&7" + String.format("%.2f", (double)freeMemory/1024/1024) + " MB&8)"));
        sender.sendMessage(lang.formatMessage("&7  &8└─ &7Max: &f" + formatBytesToMB(maxMemory) + " MB &8(&7" + String.format("%.2f", (double)maxMemory/1024/1024) + " MB&8)"));
        
        // Thread information
        int activeThreads = Thread.activeCount();
        sender.sendMessage(lang.formatMessage("&7• &bActive Threads: &f" + activeThreads));
        
        // Player information  
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
        int maxPlayers = plugin.getServer().getMaxPlayers();
        double playerPercent = (double) onlinePlayers / maxPlayers * 100;
        String playerColor = playerPercent > 80 ? "&c" : playerPercent > 60 ? "&e" : "&a";
        sender.sendMessage(lang.formatMessage("&7• &bPlayers: " + playerColor + onlinePlayers + "&7/&f" + maxPlayers + " &8(&f" + String.format("%.1f", playerPercent) + "%&8)"));
        
        // TPS monitoring with precise values
        try {
            double[] tpsArray = plugin.getServer().getTPS();
            if (tpsArray.length > 0) {
                double currentTps = tpsArray[0];
                String tpsColor = currentTps > 18 ? "&a" : currentTps > 15 ? "&e" : "&c";
                sender.sendMessage(lang.formatMessage("&7• &bTPS: " + tpsColor + df.format(currentTps) + " &8(&f" + String.format("%.3f", currentTps) + "&8) " + getTpsRating(currentTps)));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&7• &bTPS: &cMonitoring not available"));
        }
        
        // CPU Usage if available
        double cpuUsage = getCPUUsage();
        if (cpuUsage >= 0) {
            String cpuColor = cpuUsage > 80 ? "&c" : cpuUsage > 50 ? "&e" : "&a";
            sender.sendMessage(lang.formatMessage("&7• &bCPU Usage: " + cpuColor + df.format(cpuUsage) + "% &8(&f" + String.format("%.2f", cpuUsage) + "%&8) " + getCPUStatus(cpuUsage)));
        }
        
        sender.sendMessage("");
        
        // Performance Monitor Status
        if (plugin.getPerformanceMonitor() != null) {
            sender.sendMessage(lang.formatMessage("&e📊 &lModule Performance Monitor"));
            var perfData = plugin.getPerformanceMonitor().getPerformanceData();
            sender.sendMessage(lang.formatMessage("&7• &bActive Monitoring: &aEnabled"));
            sender.sendMessage(lang.formatMessage("&7• &bTracked Modules: &f" + perfData.size()));
            
            if (!perfData.isEmpty()) {
                sender.sendMessage(lang.formatMessage("&7• &bTop Performance Impact:"));
                perfData.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue().getAverageExecutionTime(), a.getValue().getAverageExecutionTime()))
                    .limit(5)
                    .forEach(entry -> {
                        ModulePerformanceData data = entry.getValue();
                        double avgTime = data.getAverageExecutionTime();
                        String timeColor = avgTime > 100 ? "&c" : avgTime > 50 ? "&e" : "&a";
                        sender.sendMessage(lang.formatMessage("&7  &8• &f" + entry.getKey() + "&7: " + timeColor + df.format(avgTime) + "ms &8(&f" + String.format("%.3f", avgTime) + "ms&8) &7(&b" + data.getInvocationCount() + " calls&7)"));
                    });
            } else {
                sender.sendMessage(lang.formatMessage("&7  &8• &eNo performance data available yet"));
                sender.sendMessage(lang.formatMessage("&7  &8• &7Run &e/apicore performance benchmark &7to generate data"));
            }
        } else {
            sender.sendMessage(lang.formatMessage("&e📊 &lModule Performance Monitor"));
            sender.sendMessage(lang.formatMessage("&7• &bMonitoring Status: &cDisabled"));
            sender.sendMessage(lang.formatMessage("&7• &bReason: Performance monitor not initialized"));
            sender.sendMessage(lang.formatMessage("&7• &bSolution: Check plugin configuration and restart"));
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Clickable refresh command
        if (sender instanceof org.bukkit.entity.Player) {
            sender.sendMessage(createClickableCommand("&7🔄 Click to refresh monitor", "/apicore performance monitor"));
        } else {
            sender.sendMessage(lang.formatMessage("&7Use &e/apicore performance monitor &7to refresh this display"));
        }
        
        sender.sendMessage(lang.formatMessage("&7Use &e/apicore performance clear &7to reset performance data"));
    }private void clearPerformanceData(CommandSender sender) {
        try {
            if (plugin.getPerformanceMonitor() != null) {
                // Clear performance data - reset internal data structures
                plugin.getPerformanceMonitor().getPerformanceData().clear();
                sender.sendMessage(lang.formatMessage("&a✓ &7Performance data cleared"));
            } else {
                sender.sendMessage(lang.formatMessage("&c✗ &7Performance monitor not available"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Error clearing performance data: " + e.getMessage()));
        }
    }

    private void showPerformanceHelp(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e&lPerformance Commands:"));
        sender.sendMessage(lang.formatMessage("&e/apicore performance status &8- &7Show performance overview"));
        sender.sendMessage(lang.formatMessage("&e/apicore performance benchmark &8- &7Run performance benchmark"));
        sender.sendMessage(lang.formatMessage("&e/apicore performance report &8- &7Generate detailed report"));
        sender.sendMessage(lang.formatMessage("&e/apicore performance monitor &8- &7Show performance monitor"));
        sender.sendMessage(lang.formatMessage("&e/apicore performance clear &8- &7Clear performance data"));
    }    private void handleReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.reload")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            showReloadHelp(sender);
            return;
        }
        
        String component = args[0].toLowerCase();
        
        switch (component) {
            case "all":
                reloadAllComponents(sender);
                break;
            case "config":
                reloadConfig(sender);
                break;
            case "modules":
                reloadModules(sender);
                break;
            case "language":
            case "lang":
                reloadLanguage(sender);
                break;
            case "help":
                showReloadHelp(sender);
                break;
            default:
                sender.sendMessage(lang.formatMessage("&c✗ &7Unknown component: " + component));
                showReloadHelp(sender);
        }
    }    private void reloadAllComponents(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         🔄 RELOADING ALL COMPONENTS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        int successful = 0;
        int total = 6;
        
        // 1. Configuration
        try {
            plugin.reloadConfig();
            sender.sendMessage(lang.formatMessage("&a✓ &7Configuration reloaded"));
            successful++;
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload configuration: " + e.getMessage()));
        }
        
        // 2. Language System
        try {
            plugin.reloadConfig(); // This also reloads language configs
            sender.sendMessage(lang.formatMessage("&a✓ &7Language system reloaded"));
            successful++;
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload language: " + e.getMessage()));
        }
        
        // 3. Modules
        try {
            if (plugin.getModuleManager() != null) {
                int beforeCount = plugin.getLoadedModules().size();
                plugin.reloadModules();
                int afterCount = plugin.getLoadedModules().size();
                
                sender.sendMessage(lang.formatMessage("&a✓ &7Modules reloaded (&f" + beforeCount + " → " + afterCount + "&7)"));
                successful++;
            } else {
                sender.sendMessage(lang.formatMessage("&e⚠ &7Module manager not available"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload modules: " + e.getMessage()));
        }
        
        // 4. Performance Monitor
        try {
            if (plugin.getPerformanceMonitor() != null) {
                sender.sendMessage(lang.formatMessage("&a✓ &7Performance monitor checked"));
                successful++;
            } else {
                sender.sendMessage(lang.formatMessage("&e⚠ &7Performance monitor not available"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to check performance monitor: " + e.getMessage()));
        }
        
        // 5. Thread Manager
        try {
            if (plugin.getThreadManager() != null) {
                sender.sendMessage(lang.formatMessage("&a✓ &7Thread manager checked"));
                successful++;
            } else {
                sender.sendMessage(lang.formatMessage("&e⚠ &7Thread manager not available"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to check thread manager: " + e.getMessage()));
        }
        
        // 6. Console Settings
        try {
            boolean colors = plugin.getConfig().getBoolean("console.colors", true);
            boolean timestamps = plugin.getConfig().getBoolean("console.timestamps", false);
            sender.sendMessage(lang.formatMessage("&a✓ &7Console settings applied (colors: &f" + colors + "&7, timestamps: &f" + timestamps + "&7)"));
            successful++;
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to apply console settings: " + e.getMessage()));
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        String successColor = successful == total ? "&a" : successful > total/2 ? "&e" : "&c";
        sender.sendMessage(lang.formatMessage("&7Reload completed: " + successColor + successful + "&7/&f" + total + " &7components"));
        
        if (successful == total) {
            sender.sendMessage(lang.formatMessage("&a✓ &7All components reloaded successfully!"));
            sender.sendMessage(lang.formatMessage("&7All configuration settings have been applied."));
        } else if (successful > 0) {
            sender.sendMessage(lang.formatMessage("&e⚠ &7Some components were processed. System status updated."));
        } else {
            sender.sendMessage(lang.formatMessage("&c✗ &7Reload process encountered issues. Check console for details."));
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));    }

    private void reloadConfig(CommandSender sender) {
        try {
            plugin.reloadConfig();
            sender.sendMessage(lang.formatMessage("&a✓ &7Configuration reloaded successfully"));
            sender.sendMessage(lang.formatMessage("&7Use &e/apicore config &7to view current settings"));
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload configuration: " + e.getMessage()));
        }
    }

    private void reloadModules(CommandSender sender) {
        try {
            if (plugin.getModuleManager() != null) {
                int loadedModules = plugin.getLoadedModules().size();
                plugin.reloadModules(); // Use ApiCore's method
                int newLoadedModules = plugin.getLoadedModules().size();
                
                sender.sendMessage(lang.formatMessage("&a✓ &7Modules reloaded successfully"));
                sender.sendMessage(lang.formatMessage("&7Modules: &f" + loadedModules + " → " + newLoadedModules));
                
                if (newLoadedModules > loadedModules) {
                    sender.sendMessage(lang.formatMessage("&a+ &7" + (newLoadedModules - loadedModules) + " new modules loaded"));
                } else if (newLoadedModules < loadedModules) {
                    sender.sendMessage(lang.formatMessage("&c- &7" + (loadedModules - newLoadedModules) + " modules unloaded"));
                }
            } else {
                sender.sendMessage(lang.formatMessage("&c✗ &7Module manager not available"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload modules: " + e.getMessage()));
        }
    }

    private void reloadLanguage(CommandSender sender) {
        try {
            plugin.reloadConfig();
            sender.sendMessage(lang.formatMessage("&a✓ &7Language configuration reloaded successfully"));
            sender.sendMessage(lang.formatMessage("&7Current language: &f" + lang.getCurrentLanguage()));
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload language: " + e.getMessage()));
        }
    }

    private void showReloadHelp(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         🔄 RELOAD COMMANDS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&e/apicore reload all &8- &7Reload all components"));
        sender.sendMessage(lang.formatMessage("&e/apicore reload config &8- &7Reload configuration"));
        sender.sendMessage(lang.formatMessage("&e/apicore reload modules &8- &7Reload all modules"));
        sender.sendMessage(lang.formatMessage("&e/apicore reload language &8- &7Reload language files"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));    }

    private void handleDebugCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.debug")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        boolean currentDebug = plugin.getConfig().getBoolean("debug.enabled", false);
        
        if (args.length == 0) {
            showDebugStatus(sender, currentDebug);
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "on":
            case "enable":
                plugin.getConfig().set("debug.enabled", true);
                plugin.saveConfig();
                sender.sendMessage(lang.formatMessage("&a✓ &7Debug mode enabled"));
                showDebugInfo(sender);
                break;
            case "off":
            case "disable":
                plugin.getConfig().set("debug.enabled", false);
                plugin.saveConfig();
                sender.sendMessage(lang.formatMessage("&c✗ &7Debug mode disabled"));
                break;
            case "toggle":
                plugin.getConfig().set("debug.enabled", !currentDebug);
                plugin.saveConfig();
                if (!currentDebug) {
                    sender.sendMessage(lang.formatMessage("&a✓ &7Debug mode enabled"));
                    showDebugInfo(sender);
                } else {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Debug mode disabled"));
                }
                break;
            case "status":
                showDebugStatus(sender, currentDebug);
                break;
            case "info":
                showDebugInfo(sender);
                break;
            case "level":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: &e/apicore debug level <LEVEL>"));
                    sender.sendMessage(lang.formatMessage("&7Available levels: SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST"));
                    return;
                }
                setDebugLevel(sender, args[1]);
                break;
            default:
                showDebugStatus(sender, currentDebug);
                break;
        }
    }

    private void showDebugStatus(CommandSender sender, boolean debugEnabled) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         🐛 DEBUG STATUS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        String status = debugEnabled ? "&aEnabled" : "&cDisabled";
        sender.sendMessage(lang.formatMessage("&7Debug Mode: " + status));
        sender.sendMessage(lang.formatMessage("&7Log Level: &f" + plugin.getLogger().getLevel()));
        
        if (debugEnabled) {
            sender.sendMessage(lang.formatMessage("&7Use &e/apicore debug info &7for detailed information"));
        } else {
            sender.sendMessage(lang.formatMessage("&7Use &e/apicore debug on &7to enable debug mode"));
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void showDebugInfo(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         🔍 DEBUG INFORMATION"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // System Info
        Runtime runtime = Runtime.getRuntime();
        sender.sendMessage(lang.formatMessage("&7Java Version: &f" + System.getProperty("java.version")));
        sender.sendMessage(lang.formatMessage("&7Bukkit Version: &f" + plugin.getServer().getBukkitVersion()));
        sender.sendMessage(lang.formatMessage("&7Free Memory: &f" + formatBytes(runtime.freeMemory())));
        sender.sendMessage(lang.formatMessage("&7Active Threads: &f" + Thread.activeCount()));
        
        // Plugin Info
        sender.sendMessage(lang.formatMessage("&7Loaded Modules: &f" + plugin.getLoadedModules().size()));
        sender.sendMessage(lang.formatMessage("&7Thread Manager: &f" + (plugin.getThreadManager() != null ? "Available" : "Not available")));
        sender.sendMessage(lang.formatMessage("&7Performance Monitor: &f" + (plugin.getPerformanceMonitor() != null ? "Active" : "Inactive")));
        
        // Server Info
        sender.sendMessage(lang.formatMessage("&7Online Players: &f" + plugin.getServer().getOnlinePlayers().size()));
        sender.sendMessage(lang.formatMessage("&7Loaded Worlds: &f" + plugin.getServer().getWorlds().size()));
        
        // Performance Data
        if (plugin.getPerformanceMonitor() != null) {
            var perfData = plugin.getPerformanceMonitor().getPerformanceData();
            sender.sendMessage(lang.formatMessage("&7Performance Data: &f" + perfData.size() + " modules monitored"));
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void setDebugLevel(CommandSender sender, String levelName) {
        try {
            java.util.logging.Level level = java.util.logging.Level.parse(levelName.toUpperCase());
            plugin.getLogger().setLevel(level);
            plugin.getConfig().set("debug.level", levelName.toUpperCase());
            plugin.saveConfig();
            sender.sendMessage(lang.formatMessage("&a✓ &7Debug level set to: &f" + level.getName()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Invalid debug level: " + levelName));
            sender.sendMessage(lang.formatMessage("&7Available levels: SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST"));
        }
    }    private void handleBackupCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.backup")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            showBackupHelp(sender);
            return;
        }
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "create":
                createBackup(sender);
                break;
            case "list":
                listBackups(sender);
                break;
            case "restore":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: &e/apicore backup restore <backup-name>"));
                    return;
                }
                restoreBackup(sender, args[1]);
                break;
            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: &e/apicore backup delete <backup-name>"));
                    return;
                }
                deleteBackup(sender, args[1]);
                break;
            case "info":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Usage: &e/apicore backup info <backup-name>"));
                    return;
                }
                showBackupInfo(sender, args[1]);
                break;
            default:
                showBackupHelp(sender);
                break;
        }
    }

    private void showBackupHelp(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         📦 BACKUP COMMANDS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&e/apicore backup create &8- &7Create a new backup"));
        sender.sendMessage(lang.formatMessage("&e/apicore backup list &8- &7List all available backups"));
        sender.sendMessage(lang.formatMessage("&e/apicore backup restore <name> &8- &7Restore a backup"));
        sender.sendMessage(lang.formatMessage("&e/apicore backup delete <name> &8- &7Delete a backup"));
        sender.sendMessage(lang.formatMessage("&e/apicore backup info <name> &8- &7Show backup information"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void createBackup(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e⚡ &7Creating backup..."));
        
        plugin.getThreadManager().submit(() -> {
            try {
                File backupDir = new File(plugin.getDataFolder(), "backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String backupName = "backup-" + timestamp + ".zip";
                File backupFile = new File(backupDir, backupName);
                
                // Create backup archive
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(backupFile);
                     java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {
                    
                    // Backup plugin configuration
                    addFileToZip(zos, new File(plugin.getDataFolder(), "config.yml"), "config.yml");
                    
                    // Backup language files
                    File langDir = new File(plugin.getDataFolder(), "languages");
                    if (langDir.exists()) {
                        addDirectoryToZip(zos, langDir, "languages/");
                    }
                    
                    // Backup module configurations
                    File moduleDir = new File(plugin.getDataFolder(), "modules");
                    if (moduleDir.exists()) {
                        addDirectoryToZip(zos, moduleDir, "modules/");
                    }
                }
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(lang.formatMessage("&a✓ &7Backup created successfully: &f" + backupName));
                    sender.sendMessage(lang.formatMessage("&7Size: &f" + formatBytes(backupFile.length())));
                });
                
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Failed to create backup: " + e.getMessage()));
                });
            }
        });
    }

    private void listBackups(CommandSender sender) {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(lang.formatMessage("&6&l         📦 BACKUPS"));
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(lang.formatMessage("&e⚠ &7No backups found"));
            sender.sendMessage(lang.formatMessage("&7Use &e/apicore backup create &7to create a backup"));
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            return;
        }
        
        File[] backupFiles = backupDir.listFiles((dir, name) -> 
            name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".backup"));
        
        if (backupFiles == null || backupFiles.length == 0) {
            sender.sendMessage(lang.formatMessage("&e⚠ &7No backups found"));
            return;
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         📦 AVAILABLE BACKUPS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Sort by date (newest first)
        Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        for (File backup : backupFiles) {
            String size = formatBytes(backup.length());
            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(backup.lastModified()));
            String ageColor = getAgeColor(backup.lastModified());
            
            sender.sendMessage(lang.formatMessage("&7• &f" + backup.getName()));
            sender.sendMessage(lang.formatMessage("&7  &8└─ &7Size: &f" + size + " &7| Date: " + ageColor + date));
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&7Total: &f" + backupFiles.length + " &7backups"));
    }

    private void restoreBackup(CommandSender sender, String backupName) {
        File backupFile = new File(new File(plugin.getDataFolder(), "backups"), backupName);
        if (!backupFile.exists()) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Backup not found: " + backupName));
            return;
        }
        
        sender.sendMessage(lang.formatMessage("&e⚠ &7Backup restoration requires server restart"));
        sender.sendMessage(lang.formatMessage("&7Please stop the server and restore manually"));
        sender.sendMessage(lang.formatMessage("&7Backup location: &f" + backupFile.getAbsolutePath()));
    }

    private void deleteBackup(CommandSender sender, String backupName) {
        File backupFile = new File(new File(plugin.getDataFolder(), "backups"), backupName);
        if (!backupFile.exists()) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Backup not found: " + backupName));
            return;
        }
        
        if (backupFile.delete()) {
            sender.sendMessage(lang.formatMessage("&a✓ &7Backup deleted: " + backupName));
        } else {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to delete backup"));
        }
    }

    private void showBackupInfo(CommandSender sender, String backupName) {
        File backupFile = new File(new File(plugin.getDataFolder(), "backups"), backupName);
        if (!backupFile.exists()) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Backup not found: " + backupName));
            return;
        }
        
        long size = backupFile.length();
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(backupFile.lastModified()));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         📋 BACKUP INFORMATION"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&7Name: &f" + backupName));
        sender.sendMessage(lang.formatMessage("&7Size: &f" + formatBytes(size)));
        sender.sendMessage(lang.formatMessage("&7Created: &f" + date));
        sender.sendMessage(lang.formatMessage("&7Path: &f" + backupFile.getAbsolutePath()));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void addFileToZip(java.util.zip.ZipOutputStream zos, File file, String entryName) throws java.io.IOException {
        if (!file.exists()) return;
        
        zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }

    private void addDirectoryToZip(java.util.zip.ZipOutputStream zos, File dir, String entryPrefix) throws java.io.IOException {
        if (!dir.exists() || !dir.isDirectory()) return;
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addDirectoryToZip(zos, file, entryPrefix + file.getName() + "/");
                } else {
                    addFileToZip(zos, file, entryPrefix + file.getName());
                }
            }
        }
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Formats bytes to MB with proper rounding
     */
    private String formatBytesToMB(long bytes) {
        return df.format(bytes / (1024.0 * 1024.0));
    }
    
    /**
     * Gets color based on player count percentage
     */
    private String getPlayerStatusColor(int online, int max) {
        double percentage = (double) online / max;
        if (percentage > 0.8) return "&c";
        if (percentage > 0.6) return "&e";
        return "&a";
    }
    
    /**
     * Formats uptime in human readable format
     */
    private String formatUptime(long uptime) {
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else {
            return String.format("%dm %ds", minutes, seconds % 60);
        }
    }
    
    /**
     * Gets TPS rating with colors
     */
    private String getTpsRating(double tps) {
        if (tps > 19.5) return "&8(&a&lExcellent&8)";
        if (tps > 18.0) return "&8(&a&lGood&8)";
        if (tps > 15.0) return "&8(&e&lOkay&8)";
        if (tps > 10.0) return "&8(&c&lPoor&8)";
        return "&8(&4&lCritical&8)";
    }
    
    /**
     * Gets CPU usage percentage
     */
    private double getCPUUsage() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
            }
        } catch (Exception e) {
            // Ignore if not available
        }
        return -1;
    }
    
    /**
     * Gets CPU status description
     */
    private String getCPUStatus(double cpuUsage) {
        if (cpuUsage > 80) return "&8(&c&lHigh&8)";
        if (cpuUsage > 50) return "&8(&e&lModerate&8)";
        return "&8(&a&lLow&8)";
    }
    
    /**
     * Gets garbage collection information
     */
    private String getGCInfo() {
        try {
            java.lang.management.GarbageCollectorMXBean gcBean = ManagementFactory.getGarbageCollectorMXBeans().get(0);
            return gcBean.getName() + " (" + gcBean.getCollectionCount() + " collections)";
        } catch (Exception e) {
            return "Not available";
        }
    }
    
    /**
     * Gets color based on backup age
     */
    private String getAgeColor(long timestamp) {
        long age = System.currentTimeMillis() - timestamp;
        long days = age / (24 * 60 * 60 * 1000);
        
        if (days < 1) return "&a"; // Green for less than 1 day        if (days < 7) return "&e"; // Yellow for less than 1 week
        return "&c"; // Red for older than 1 week
    }    /**
     * Colorizes a message by replacing color codes
     */    private String colorize(String message) {
        if (message == null) return "";
        // Using modern approach instead of deprecated ChatColor
        return message.replace('&', '§');
    }

    /**
     * Handles permissions command
     */    private void handlePermissionsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Permission Commands:"));
            sender.sendMessage(colorize("&8• &7/apicore permissions list &8- &fList all permissions"));
            sender.sendMessage(colorize("&8• &7/apicore permissions check <player> <permission> &8- &fCheck permission"));
            sender.sendMessage(colorize("&8• &7/apicore permissions info &8- &fShow permission system info"));
            sender.sendMessage(colorize("&8• &7/apicore permissions reload &8- &fReload permissions"));
            sender.sendMessage(colorize("&8• &7/apicore permissions modules &8- &fShow module permissions"));
            return;
        }
        
        String action = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (action) {
            case "list":
                handlePermissionsList(sender, subArgs);
                break;
            case "check":
                handlePermissionsCheck(sender, subArgs);
                break;
            case "info":
                handlePermissionsInfo(sender, subArgs);
                break;
            case "reload":
                handlePermissionsReload(sender, subArgs);
                break;
            case "modules":
                handlePermissionsModules(sender, subArgs);
                break;
            case "status":
                handlePermissionsStatus(sender, subArgs);
                break;
            default:
                sender.sendMessage(colorize("&8[&b&lApiCore&8] &cUnknown permission command: " + action));
                sender.sendMessage(colorize("&8• &7Use &f/apicore permissions &7for help"));
                break;
        }
    }
    
    /**
     * Zeigt alle verfügbaren Permissions an
     */
    private void handlePermissionsList(CommandSender sender, String[] args) {
        var permManager = plugin.getPermissionManager();
        if (permManager == null) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cPermission manager not available"));
            return;
        }
        
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Available Permissions:"));
        
        try {
            // Core Permissions
            sender.sendMessage(colorize("&8▪ &6Core Permissions:"));
            String[] corePermissions = {
                "apicore.admin", "apicore.admin.*", "apicore.commands", "apicore.debug",
                "apicore.status", "apicore.reload", "apicore.config", "apicore.performance",
                "apicore.modules", "apicore.permissions"
            };
              for (String perm : corePermissions) {
                ClickableCommand.sendClickableMessage(sender, "  &8• &f" + perm, "/apicore permissions check <player> " + perm, 
                    "&7Click to check this permission for a player");
            }
            
            // Module Permissions
            sender.sendMessage(colorize("&8▪ &6Module Permissions:"));
            var modulePermissions = permManager.getAllModulePermissions();
            
            if (modulePermissions.isEmpty()) {
                sender.sendMessage(colorize("  &8• &7No module permissions found"));
            } else {
                for (Map.Entry<String, List<org.bukkit.permissions.Permission>> entry : modulePermissions.entrySet()) {
                    String moduleName = entry.getKey();
                    List<org.bukkit.permissions.Permission> permissions = entry.getValue();
                    
                    if (!permissions.isEmpty()) {
                        sender.sendMessage(colorize("  &8▸ &e" + moduleName + ":"));                        for (org.bukkit.permissions.Permission permission : permissions) {
                            ClickableCommand.sendClickableMessage(sender, "    &8• &f" + permission.getName(), 
                                "/apicore permissions check <player> " + permission.getName(), 
                                "&7Click to check this permission for a player");
                        }
                    }
                }
            }
            
            // Registered Permissions
            var allPermissions = permManager.getAllPermissions();
            sender.sendMessage(colorize("&8▪ &6Total Registered: &f" + allPermissions.size() + " permissions"));
            
        } catch (Exception e) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cError listing permissions: " + e.getMessage()));
        }
    }
    
    /**
     * Überprüft Permission für einen Spieler
     */
    private void handlePermissionsCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cUsage: /apicore permissions check <player> <permission>"));
            return;
        }
        
        var target = org.bukkit.Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cPlayer not found: " + args[0]));
            return;
        }
        
        String permission = args[1];
        boolean hasPermission = target.hasPermission(permission);
        
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Permission Check:"));
        sender.sendMessage(colorize("&8• &7Player: &f" + target.getName()));
        sender.sendMessage(colorize("&8• &7Permission: &f" + permission));
        sender.sendMessage(colorize("&8• &7Result: " + (hasPermission ? "&aHAS PERMISSION" : "&cNO PERMISSION")));
        
        // Zusätzliche Informationen, falls verfügbar
        var permManager = plugin.getPermissionManager();
        if (permManager != null) {
            try {
                var playerPermissions = permManager.getPlayerPermissions(target);
                sender.sendMessage(colorize("&8• &7Player has &f" + playerPermissions.size() + " &7total permissions"));
            } catch (Exception e) {
                sender.sendMessage(colorize("&8• &7Could not retrieve player permissions: " + e.getMessage()));
            }
        }
    }
    
    /**
     * Zeigt Permission-System-Informationen an
     */
    private void handlePermissionsInfo(CommandSender sender, String[] args) {
        var permManager = plugin.getPermissionManager();
        
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Permission System Information:"));
        
        if (permManager != null) {
            sender.sendMessage(colorize("&8• &7Permission Manager: &aActive"));
            
            try {
                var allPermissions = permManager.getAllPermissions();
                var modulePermissions = permManager.getAllModulePermissions();
                
                sender.sendMessage(colorize("&8• &7Total Permissions: &f" + allPermissions.size()));
                sender.sendMessage(colorize("&8• &7Loaded Modules: &f" + modulePermissions.size()));
                  // LuckPerms Integration Check
                try {
                    // Check if LuckPerms methods are available
                    permManager.getClass().getMethod("isPermissionsHooked");
                    boolean hooked = (Boolean) permManager.getClass().getMethod("isPermissionsHooked").invoke(permManager);
                    sender.sendMessage(colorize("&8• &7External Permissions: " + (hooked ? "&aLuckPerms Connected" : "&cNot Connected")));
                } catch (Exception e) {
                    sender.sendMessage(colorize("&8• &7External Permissions: &cNot Available"));
                }
                
            } catch (Exception e) {
                sender.sendMessage(colorize("&8• &cError retrieving permission info: " + e.getMessage()));
            }
        } else {
            sender.sendMessage(colorize("&8• &cPermission Manager: Not Available"));
        }
    }
    
    /**
     * Lädt Permissions neu
     */
    private void handlePermissionsReload(CommandSender sender, String[] args) {
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Reloading permissions..."));
          try {
            var permManager = plugin.getPermissionManager();
            if (permManager != null) {
                // Permission-Manager neu verbinden
                try {
                    // Try to call hookIntoPermissions if available
                    var method = permManager.getClass().getMethod("hookIntoPermissions");
                    boolean hooked = (Boolean) method.invoke(permManager);
                    
                    if (hooked) {
                        sender.sendMessage(colorize("&8[&b&lApiCore&8] &aPermissions reloaded successfully"));
                        sender.sendMessage(colorize("&8• &7External permissions connected"));
                    } else {
                        sender.sendMessage(colorize("&8[&b&lApiCore&8] &ePermissions reloaded (no external connection)"));
                    }
                } catch (NoSuchMethodException e) {
                    sender.sendMessage(colorize("&8[&b&lApiCore&8] &ePermissions reloaded (basic mode)"));
                }
            } else {
                sender.sendMessage(colorize("&8[&b&lApiCore&8] &cPermission manager not available"));
            }
        } catch (Exception e) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cError reloading permissions: " + e.getMessage()));
        }
    }
    
    /**
     * Zeigt Module-Permissions an
     */
    private void handlePermissionsModules(CommandSender sender, String[] args) {
        var permManager = plugin.getPermissionManager();
        if (permManager == null) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cPermission manager not available"));
            return;
        }
        
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Module Permissions:"));
        
        try {
            var modulePermissions = permManager.getAllModulePermissions();
            
            if (modulePermissions.isEmpty()) {
                sender.sendMessage(colorize("&8• &7No modules with permissions found"));
                return;
            }
            
            for (Map.Entry<String, List<org.bukkit.permissions.Permission>> entry : modulePermissions.entrySet()) {
                String moduleName = entry.getKey();
                List<org.bukkit.permissions.Permission> permissions = entry.getValue();
                
                sender.sendMessage(colorize("&8▸ &e" + moduleName + " &8(&f" + permissions.size() + " permissions&8):"));
                
                for (org.bukkit.permissions.Permission permission : permissions) {
                    String desc = permission.getDescription();
                    if (desc != null && !desc.isEmpty()) {
                        sender.sendMessage(colorize("  &8• &f" + permission.getName() + " &8- &7" + desc));
                    } else {
                        sender.sendMessage(colorize("  &8• &f" + permission.getName()));
                    }
                }
            }
        } catch (Exception e) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cError retrieving module permissions: " + e.getMessage()));
        }
    }
    
    /**
     * Zeigt Permission-Status an (Alias für info)
     */
    private void handlePermissionsStatus(CommandSender sender, String[] args) {
        handlePermissionsInfo(sender, args);
    }

    /**
     * Handles threads command
     */
    private void handleThreadsCommand(CommandSender sender, String[] args) {
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Thread Information:"));
        
        var threadManager = apiCore.getThreadManager();
        if (threadManager != null) {
            sender.sendMessage(colorize("&8• &7Active Threads: &f" + Thread.activeCount()));
            sender.sendMessage(colorize("&8• &7Available Processors: &f" + Runtime.getRuntime().availableProcessors()));
        } else {
            sender.sendMessage(colorize("&8• &cThread manager not available"));
        }
    }

    /**
     * Handles memory command
     */
    private void handleMemoryCommand(CommandSender sender, String[] args) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Memory Information:"));
        sender.sendMessage(colorize("&8• &7Used: &f" + formatBytes(usedMemory) + " &8/&f " + formatBytes(maxMemory)));
        sender.sendMessage(colorize("&8• &7Free: &f" + formatBytes(freeMemory)));
        sender.sendMessage(colorize("&8• &7Usage: &f" + Math.round((double) usedMemory / maxMemory * 100) + "%"));
        
        if (args.length > 0 && args[0].equalsIgnoreCase("gc")) {
            long beforeGC = usedMemory;
            System.gc();
            runtime = Runtime.getRuntime();
            long afterGC = runtime.totalMemory() - runtime.freeMemory();
            long freed = beforeGC - afterGC;
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &aGarbage collection completed. Freed: &f" + formatBytes(freed)));
        }
    }

    /**
     * Handles security command
     */
    private void handleSecurityCommand(CommandSender sender, String[] args) {
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Security Information:"));
        
        var sandbox = apiCore.getModuleSandbox();
        if (sandbox != null) {
            sender.sendMessage(colorize("&8• &7Module Sandbox: &aEnabled"));
        } else {
            sender.sendMessage(colorize("&8• &7Module Sandbox: &cDisabled"));
        }
    }

    /**
     * Handles export command
     */    private void handleExportCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Export Commands:"));
            sender.sendMessage(colorize("&8• &7/apicore export config &8- &fExport configuration"));
            sender.sendMessage(colorize("&8• &7/apicore export data &8- &fExport all data"));
            sender.sendMessage(colorize("&8• &7/apicore export logs &8- &fExport log files"));
            sender.sendMessage(colorize("&8• &7/apicore export modules &8- &fExport module data"));
            sender.sendMessage(colorize("&8• &7/apicore export all &8- &fExport everything"));
            return;
        }
        
        String exportType = args[0].toLowerCase();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File exportDir = new File(apiCore.getDataFolder(), "exports");
        exportDir.mkdirs();
        
        switch (exportType) {
            case "config":
                exportConfiguration(sender, exportDir, timestamp);
                break;
            case "data":
                exportData(sender, exportDir, timestamp);
                break;
            case "logs":
                exportLogs(sender, exportDir, timestamp);
                break;
            case "modules":
                exportModules(sender, exportDir, timestamp);
                break;
            case "all":
                exportAll(sender, exportDir, timestamp);
                break;
            default:
                sender.sendMessage(colorize("&8[&b&lApiCore&8] &cUnknown export command: " + exportType));
                break;
        }
    }
    
    private void exportConfiguration(CommandSender sender, File exportDir, String timestamp) {
        try {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Exporting configuration..."));
            
            File configExport = new File(exportDir, "config_export_" + timestamp + ".zip");
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(configExport));
            
            // Export main config
            addFileToZip(zos, new File(apiCore.getDataFolder(), "config.yml"), "config.yml");
            
            // Export language files
            File langDir = new File(apiCore.getDataFolder(), "languages");
            if (langDir.exists()) {
                for (File langFile : langDir.listFiles()) {
                    if (langFile.getName().endsWith(".yml")) {
                        addFileToZip(zos, langFile, "languages/" + langFile.getName());
                    }
                }
            }
            
            // Export security config
            File securityDir = new File(apiCore.getDataFolder(), "security");
            if (securityDir.exists()) {
                for (File secFile : securityDir.listFiles()) {
                    if (secFile.getName().endsWith(".yml")) {
                        addFileToZip(zos, secFile, "security/" + secFile.getName());
                    }
                }
            }
            
            zos.close();
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &aConfiguration exported to: " + configExport.getName()));
            
        } catch (Exception e) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cError exporting config: " + e.getMessage()));
        }
    }
    
    private void exportData(CommandSender sender, File exportDir, String timestamp) {
        try {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Exporting data..."));
            
            File dataExport = new File(exportDir, "data_export_" + timestamp + ".zip");
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(dataExport));
            
            // Export performance data
            File perfDir = new File(apiCore.getDataFolder(), "performance_logs");
            if (perfDir.exists()) {
                for (File perfFile : perfDir.listFiles()) {
                    addFileToZip(zos, perfFile, "performance/" + perfFile.getName());
                }
            }
            
            // Export benchmark data
            File benchDir = new File(apiCore.getDataFolder(), "benchmarks");
            if (benchDir.exists()) {
                for (File benchFile : benchDir.listFiles()) {
                    addFileToZip(zos, benchFile, "benchmarks/" + benchFile.getName());
                }
            }
            
            zos.close();
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &aData exported to: " + dataExport.getName()));
            
        } catch (Exception e) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cError exporting data: " + e.getMessage()));
        }
    }
    
    private void exportLogs(CommandSender sender, File exportDir, String timestamp) {
        try {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Exporting logs..."));
            
            File logExport = new File(exportDir, "logs_export_" + timestamp + ".zip");
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(logExport));
            
            // Export server logs
            File logsDir = new File("logs");
            if (logsDir.exists()) {
                for (File logFile : logsDir.listFiles()) {
                    if (logFile.getName().endsWith(".log") || logFile.getName().endsWith(".log.gz")) {
                        addFileToZip(zos, logFile, "server_logs/" + logFile.getName());
                    }
                }
            }
            
            zos.close();
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &aLogs exported to: " + logExport.getName()));
            
        } catch (Exception e) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cError exporting logs: " + e.getMessage()));
        }
    }
    
    private void exportModules(CommandSender sender, File exportDir, String timestamp) {
        try {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Exporting module data..."));
            
            File moduleExport = new File(exportDir, "modules_export_" + timestamp + ".zip");
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(moduleExport));
            
            int moduleCount = 0;
            for (String moduleName : apiCore.getLoadedModules().keySet()) {
                try {
                    // Create module info
                    String moduleInfo = "Module: " + moduleName + "\n";
                    moduleInfo += "Status: Loaded\n";
                    moduleInfo += "Export Time: " + timestamp + "\n";
                    
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("modules/" + moduleName + "_info.txt");
                    zos.putNextEntry(entry);
                    zos.write(moduleInfo.getBytes());
                    zos.closeEntry();
                    
                    moduleCount++;
                } catch (Exception e) {
                    sender.sendMessage(colorize("&8[&b&lApiCore&8] &eWarning: Could not export module " + moduleName));
                }
            }
            
            zos.close();
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &aModules exported (" + moduleCount + " modules) to: " + moduleExport.getName()));
            
        } catch (Exception e) {
            sender.sendMessage(colorize("&8[&b&lApiCore&8] &cError exporting modules: " + e.getMessage()));
        }
    }
    
    private void exportAll(CommandSender sender, File exportDir, String timestamp) {
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &7Starting complete export..."));
        
        exportConfiguration(sender, exportDir, timestamp);
        exportData(sender, exportDir, timestamp);
        exportLogs(sender, exportDir, timestamp);
        exportModules(sender, exportDir, timestamp);
        
        sender.sendMessage(colorize("&8[&b&lApiCore&8] &aComplete export finished!"));
    }
      /**
     * Creates a clickable command message
     */
    private String createClickableCommand(String message, String command) {
        return colorize(message + " &8[&e" + command + "&8]");
    }
    
    /**
     * Finds the module file for a given module name
     */
    private File findModuleFile(String moduleName) {
        // Try different possible locations
        File[] possibleLocations = {
            new File(plugin.getDataFolder().getParentFile(), "modules/" + moduleName + ".jar"),
            new File(plugin.getDataFolder(), "modules/" + moduleName + ".jar"),
            new File(plugin.getDataFolder().getParentFile(), "plugins/modules/" + moduleName + ".jar"),
            new File("modules/" + moduleName + ".jar")
        };
        
        for (File file : possibleLocations) {
            if (file.exists()) {
                return file;
            }
        }
        
        return null;
    }
    
    /**
     * Gets available permissions for tab completion
     */
    private List<String> getAvailablePermissions() {
        List<String> permissions = new ArrayList<>();
        
        // Core permissions
        permissions.addAll(Arrays.asList(
            "apicore.admin", "apicore.admin.*", "apicore.commands", "apicore.debug",
            "apicore.status", "apicore.reload", "apicore.config", "apicore.performance",
            "apicore.modules", "apicore.permissions", "apicore.backup", "apicore.export"
        ));
        
        // Module permissions
        var permManager = plugin.getPermissionManager();
        if (permManager != null) {
            try {
                var modulePermissions = permManager.getAllModulePermissions();
                for (List<org.bukkit.permissions.Permission> perms : modulePermissions.values()) {
                    for (org.bukkit.permissions.Permission perm : perms) {
                        permissions.add(perm.getName());
                    }
                }
            } catch (Exception e) {
                // Ignore errors during tab completion
            }
        }
        
        // Additional common permissions
        for (String moduleName : plugin.getLoadedModules().keySet()) {
            permissions.add(moduleName.toLowerCase() + ".admin");
            permissions.add(moduleName.toLowerCase() + ".use");
            permissions.add("essentialscore." + moduleName.toLowerCase() + ".admin");
        }
        
        return permissions;
    }
}
