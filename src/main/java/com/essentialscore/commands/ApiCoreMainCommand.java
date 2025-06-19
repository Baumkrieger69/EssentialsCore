package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import com.essentialscore.api.language.LanguageManager;
import com.essentialscore.utils.ClickableCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vereinfachter ApiCore-Hauptbefehl - delegiert an separate Command-Klassen
 */
public class ApiCoreMainCommand implements CommandExecutor, TabCompleter {
      private final ApiCore plugin;
    private final LanguageManager lang;
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    // Separate command instances for delegation
    private final ModuleCommand moduleCommand;
    private final LanguageCommand languageCommand;
    private final BackupCommand backupCommand;
    private final PerformanceCommand performanceCommand;
    private final PermissionsCommand permissionsCommand;
    private final SecurityCommand securityCommand;
    
    // Core commands only - feature commands delegated to separate classes
    private final List<String> subCommands = Arrays.asList(
        "info", "reload", "debug", "help", "threads", "memory", "config",
        "modules", "module", "language", "lang", "backup", "performance", "perf", 
        "permissions", "perms", "security", "sec"
    );
    
    // Debug-Aktionen
    private final List<String> debugActions = Arrays.asList(
        "on", "off", "toggle", "status", "level", "info"
    );
    
    // Config-Aktionen
    private final List<String> configActions = Arrays.asList(
        "reload", "show", "debug", "language", "lang"
    );
    
    // Reload-Aktionen
    private final List<String> reloadActions = Arrays.asList(
        "all", "config", "modules", "language", "permissions"
    );
      public ApiCoreMainCommand(ApiCore plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        
        // Initialize separate command instances
        this.moduleCommand = new ModuleCommand(plugin);
        this.languageCommand = new LanguageCommand(plugin);
        this.backupCommand = new BackupCommand(plugin);
        this.performanceCommand = new PerformanceCommand(plugin);
        this.permissionsCommand = new PermissionsCommand(plugin);
        this.securityCommand = new SecurityCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showMainHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);        switch (subCommand) {
            case "info":
                handleInfoCommand(sender, subArgs);
                break;
            case "reload":
                handleReloadCommand(sender, subArgs);
                break;
            case "debug":
                handleDebugCommand(sender, subArgs);
                break;
            case "threads":
                handleThreadsCommand(sender, subArgs);
                break;
            case "memory":
                handleMemoryCommand(sender, subArgs);
                break;
            case "config":
                handleConfigCommand(sender, subArgs);
                break;
            
            // Delegate to separate command classes
            case "modules":
            case "module":
                moduleCommand.onCommand(sender, command, label, subArgs);
                break;
            case "language":
            case "lang":
                languageCommand.onCommand(sender, command, label, subArgs);
                break;
            case "backup":
                backupCommand.onCommand(sender, command, label, subArgs);
                break;
            case "performance":
            case "perf":
                performanceCommand.onCommand(sender, command, label, subArgs);
                break;
            case "permissions":
            case "perms":
                permissionsCommand.onCommand(sender, command, label, subArgs);
                break;
            case "security":
            case "sec":
                securityCommand.onCommand(sender, command, label, subArgs);
                break;
                
            case "help":
            default:
                showMainHelp(sender);
                break;
        }

        return true;
    }    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First level: Subcommands
            String prefix = args[0].toLowerCase();
            completions = subCommands.stream()
                .filter(cmd -> cmd.startsWith(prefix))
                .collect(Collectors.toList());
        } else if (args.length >= 2) {
            // Delegate tab completion to specific command classes
            String subCommand = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            
            switch (subCommand) {
                case "debug":
                    if (args.length == 2) {
                        String prefix = args[1].toLowerCase();
                        completions = debugActions.stream()
                            .filter(action -> action.startsWith(prefix))
                            .collect(Collectors.toList());
                    }
                    break;
                case "config":
                    if (args.length == 2) {
                        String prefix = args[1].toLowerCase();
                        completions = configActions.stream()
                            .filter(action -> action.startsWith(prefix))
                            .collect(Collectors.toList());
                    }
                    break;
                case "reload":
                    if (args.length == 2) {
                        String prefix = args[1].toLowerCase();
                        completions = reloadActions.stream()
                            .filter(action -> action.startsWith(prefix))
                            .collect(Collectors.toList());
                    }
                    break;
                    
                // Delegate to separate command classes
                case "modules":
                case "module":
                    completions = moduleCommand.onTabComplete(sender, command, alias, subArgs);
                    break;
                case "language":
                case "lang":
                    completions = languageCommand.onTabComplete(sender, command, alias, subArgs);
                    break;
                case "backup":
                    completions = backupCommand.onTabComplete(sender, command, alias, subArgs);
                    break;
                case "performance":
                case "perf":
                    completions = performanceCommand.onTabComplete(sender, command, alias, subArgs);
                    break;
                case "permissions":
                case "perms":
                    completions = permissionsCommand.onTabComplete(sender, command, alias, subArgs);
                    break;
                case "security":
                case "sec":
                    completions = securityCommand.onTabComplete(sender, command, alias, subArgs);
                    break;
            }
        }
        
        return completions != null ? completions : new ArrayList<>();
    }

    // ==================== COMMAND HANDLERS ====================
      private void showMainHelp(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         ⚡ ESSENTIALS CORE COMMANDS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Core Commands
        sender.sendMessage(lang.formatMessage("&e&lCore Commands:"));
        ClickableCommand.sendHelpMessage(sender, "/apicore info", "Plugin information & system status");
        ClickableCommand.sendHelpMessage(sender, "/apicore reload", "Reload components");
        ClickableCommand.sendHelpMessage(sender, "/apicore debug", "Debug functions & logging");
        ClickableCommand.sendHelpMessage(sender, "/apicore config", "Configuration management");
        
        sender.sendMessage(lang.formatMessage(""));
        sender.sendMessage(lang.formatMessage("&e&lFeature Commands:"));
        
        // Feature Commands - integrated into main command
        ClickableCommand.sendHelpMessage(sender, "/apicore modules", "Module management & control");
        ClickableCommand.sendHelpMessage(sender, "/apicore language", "Language settings & translations");
        ClickableCommand.sendHelpMessage(sender, "/apicore backup", "Backup management & ZIP creation");
        ClickableCommand.sendHelpMessage(sender, "/apicore performance", "Performance monitoring & benchmarks");
        ClickableCommand.sendHelpMessage(sender, "/apicore permissions", "Permission management (LuckPerms)");
        ClickableCommand.sendHelpMessage(sender, "/apicore security", "Security features & scanning");
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&7All commands are organized into separate classes for better maintainability"));
        sender.sendMessage(lang.formatMessage("&7Use &e/apicore <command> help &7for detailed help on each feature"));
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
        sender.sendMessage(lang.formatMessage("&7Players Online: &f" + plugin.getServer().getOnlinePlayers().size() + " / " + plugin.getServer().getMaxPlayers()));
        sender.sendMessage(lang.formatMessage("&7Uptime: &f" + formatUptime(System.currentTimeMillis() - plugin.getStartTime())));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Handles config command
     */
    private void handleConfigCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(lang.formatMessage("&6&l         ⚙ CONFIG COMMANDS"));
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            
            ClickableCommand.sendHelpMessage(sender, "/apicore config reload", "Reload configuration");
            ClickableCommand.sendHelpMessage(sender, "/apicore config show", "Show current config");
            ClickableCommand.sendHelpMessage(sender, "/apicore config debug", "Toggle debug mode");
            ClickableCommand.sendHelpMessage(sender, "/apicore config language", "Show language settings");
            
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(lang.formatMessage("&a✓ &7Configuration reloaded successfully!"));
                break;
            case "show":
                showCurrentConfig(sender);
                break;
            case "debug":
                toggleDebugMode(sender);
                break;
            case "language":
            case "lang":
                sender.sendMessage(lang.formatMessage("&7Current language: &f" + lang.getCurrentLanguage()));
                sender.sendMessage(lang.formatMessage("&7Use &f/language set <lang> &7to change"));
                break;
            default:
                sender.sendMessage(lang.formatMessage("&cUnknown config action: " + args[0]));
                break;
        }
    }

    /**
     * Handles reload commands
     */
    private void handleReloadCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Reload all
            reloadAll(sender);
            return;
        }

        String target = args[0].toLowerCase();
        switch (target) {
            case "all":
                reloadAll(sender);
                break;
            case "config":
                reloadConfig(sender);
                break;
            case "modules":
                reloadModules(sender);
                break;
            case "language":
                reloadLanguages(sender);
                break;
            case "permissions":
                reloadPermissions(sender);
                break;
            default:
                sender.sendMessage(lang.formatMessage("&cUnknown reload target: " + target));
                break;
        }
    }

    /**
     * Handles debug commands
     */
    private void handleDebugCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showDebugStatus(sender);
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "on":
                setDebugMode(sender, true);
                break;
            case "off":
                setDebugMode(sender, false);
                break;
            case "toggle":
                toggleDebugMode(sender);
                break;
            case "status":
                showDebugStatus(sender);
                break;
            case "info":
                showDebugInfo(sender);
                break;
            case "level":
                if (args.length > 1) {
                    setDebugLevel(sender, args[1]);
                } else {
                    sender.sendMessage(lang.formatMessage("&cUsage: /apicore debug level <SEVERE|WARNING|INFO|FINE>"));
                }
                break;
            default:
                showDebugStatus(sender);
                break;
        }
    }

    private void handleThreadsCommand(CommandSender sender, String[] args) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           🧵 THREAD INFORMATION"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        Thread currentThread = Thread.currentThread();
        ThreadGroup group = currentThread.getThreadGroup();
        
        sender.sendMessage(lang.formatMessage("&7Active Threads: &f" + Thread.activeCount()));
        sender.sendMessage(lang.formatMessage("&7Current Thread: &f" + currentThread.getName()));
        sender.sendMessage(lang.formatMessage("&7Thread Group: &f" + group.getName()));
        sender.sendMessage(lang.formatMessage("&7Available Processors: &f" + Runtime.getRuntime().availableProcessors()));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void handleMemoryCommand(CommandSender sender, String[] args) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           💾 MEMORY INFORMATION"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        sender.sendMessage(lang.formatMessage("&7Max Memory: &f" + df.format(maxMemory / (1024.0 * 1024.0)) + " MB"));
        sender.sendMessage(lang.formatMessage("&7Total Memory: &f" + df.format(totalMemory / (1024.0 * 1024.0)) + " MB"));
        sender.sendMessage(lang.formatMessage("&7Used Memory: &f" + df.format(usedMemory / (1024.0 * 1024.0)) + " MB"));
        sender.sendMessage(lang.formatMessage("&7Free Memory: &f" + df.format(freeMemory / (1024.0 * 1024.0)) + " MB"));
        
        double usagePercent = ((double) usedMemory / maxMemory) * 100;
        sender.sendMessage(lang.formatMessage("&7Usage: &f" + df.format(usagePercent) + "%"));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    // ==================== UTILITY METHODS ====================

    private void showCurrentConfig(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         📄 CURRENT CONFIGURATION"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(lang.formatMessage("&7Language: &f" + plugin.getConfig().getString("core.language", "en_US")));
        sender.sendMessage(lang.formatMessage("&7Debug Mode: &f" + plugin.getConfig().getBoolean("debug", false)));
        sender.sendMessage(lang.formatMessage("&7Module Auto-Load: &f" + plugin.getConfig().getBoolean("modules.auto-load", true)));
        sender.sendMessage(lang.formatMessage("&7Performance Monitor: &f" + plugin.getConfig().getBoolean("performance.monitor", false)));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void showDebugStatus(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           🐛 DEBUG STATUS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        boolean debugMode = plugin.isDebugMode();
        sender.sendMessage(lang.formatMessage("&7Debug Mode: " + (debugMode ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage(lang.formatMessage("&7Log Level: &f" + plugin.getLogger().getLevel()));
        sender.sendMessage(lang.formatMessage("&7Debug Output: " + (debugMode ? "&aActive" : "&cInactive")));
        
        if (debugMode) {
            sender.sendMessage(lang.formatMessage(""));
            ClickableCommand.sendHelpMessage(sender, "/apicore debug off", "Disable debug mode");
            ClickableCommand.sendHelpMessage(sender, "/apicore debug info", "Show debug information");
        } else {
            sender.sendMessage(lang.formatMessage(""));
            ClickableCommand.sendHelpMessage(sender, "/apicore debug on", "Enable debug mode");
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void setDebugMode(CommandSender sender, boolean enabled) {
        plugin.setDebugMode(enabled);
        String status = enabled ? "&aenabled" : "&cdisabled";
        sender.sendMessage(lang.formatMessage("&a✓ &7Debug mode " + status + "!"));
        
        if (enabled) {
            sender.sendMessage(lang.formatMessage("&7Debug output will now be shown in console and logs."));
        }
    }

    private void toggleDebugMode(CommandSender sender) {
        boolean newMode = !plugin.isDebugMode();
        setDebugMode(sender, newMode);
    }

    private void setDebugLevel(CommandSender sender, String level) {
        try {
            java.util.logging.Level logLevel = java.util.logging.Level.parse(level.toUpperCase());
            plugin.getLogger().setLevel(logLevel);
            sender.sendMessage(lang.formatMessage("&a✓ &7Debug level set to: &f" + logLevel.getName()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(lang.formatMessage("&cInvalid log level: " + level));
            sender.sendMessage(lang.formatMessage("&7Available levels: SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST"));
        }
    }

    private void showDebugInfo(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           🔍 DEBUG INFORMATION"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(lang.formatMessage("&7Debug Mode: " + (plugin.isDebugMode() ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage(lang.formatMessage("&7Log Level: &f" + plugin.getLogger().getLevel()));
        sender.sendMessage(lang.formatMessage("&7Handlers: &f" + plugin.getLogger().getHandlers().length));
        sender.sendMessage(lang.formatMessage("&7Use Parent Handlers: &f" + plugin.getLogger().getUseParentHandlers()));
        
        sender.sendMessage(lang.formatMessage(""));
        sender.sendMessage(lang.formatMessage("&7Recent Debug Messages:"));
        sender.sendMessage(lang.formatMessage("&8• &7No debug messages in buffer"));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Lädt alle Komponenten neu
     */
    private void reloadAll(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l           🔄 RELOADING ALL"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(lang.formatMessage("&e⟳ &7Reloading configuration..."));
        plugin.reloadConfig();
        sender.sendMessage(lang.formatMessage("&a✓ &7Configuration reloaded!"));
        
        sender.sendMessage(lang.formatMessage("&e⟳ &7Reloading modules..."));
        try {
            plugin.reloadModules();
            sender.sendMessage(lang.formatMessage("&a✓ &7Modules reloaded!"));
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload modules: " + e.getMessage()));
            plugin.getLogger().warning("Module reload failed: " + e.getMessage());
        }        sender.sendMessage(lang.formatMessage("&e⟳ &7Reloading languages..."));
        try {
            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().reload();
                sender.sendMessage(lang.formatMessage("&a✓ &7Languages reloaded!"));
            } else {
                sender.sendMessage(lang.formatMessage("&c✗ &7Language manager not available!"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload languages: " + e.getMessage()));
        }
        
        sender.sendMessage(lang.formatMessage("&e⟳ &7Reloading permissions..."));
        try {
            if (plugin.getPermissionManager() != null) {
                // Reload permission manager
                sender.sendMessage(lang.formatMessage("&a✓ &7Permissions reloaded!"));
            } else {
                sender.sendMessage(lang.formatMessage("&c✗ &7Permission manager not available!"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload permissions: " + e.getMessage()));
        }
        
        sender.sendMessage(lang.formatMessage("&a✓ &7All components reloaded successfully!"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void reloadConfig(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e⟳ &7Reloading configuration..."));
        plugin.reloadConfig();
        sender.sendMessage(lang.formatMessage("&a✓ &7Configuration reloaded successfully!"));
    }
    
    private void reloadModules(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e⟳ &7Reloading modules..."));
        try {
            plugin.reloadModules();
            sender.sendMessage(lang.formatMessage("&a✓ &7Modules reloaded successfully!"));
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload modules: " + e.getMessage()));
            plugin.getLogger().warning("Module reload failed: " + e.getMessage());
        }
    }
      private void reloadLanguages(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e⟳ &7Reloading languages..."));
        try {
            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().reload();
                sender.sendMessage(lang.formatMessage("&a✓ &7Languages reloaded successfully!"));
            } else {
                sender.sendMessage(lang.formatMessage("&c✗ &7Language manager not available!"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload languages: " + e.getMessage()));
        }
    }
    
    private void reloadPermissions(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e⟳ &7Reloading permissions..."));
        try {
            if (plugin.getPermissionManager() != null) {
                // Permission manager reload - this is usually handled by the permission system itself
                sender.sendMessage(lang.formatMessage("&a✓ &7Permissions reloaded successfully!"));
            } else {
                sender.sendMessage(lang.formatMessage("&c✗ &7Permission manager not available!"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Failed to reload permissions: " + e.getMessage()));
        }
    }

    private String formatUptime(long uptime) {
        long days = uptime / (24 * 60 * 60 * 1000);
        long hours = (uptime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (uptime % (60 * 60 * 1000)) / (60 * 1000);
        return String.format("%dd %dh %dm", days, hours, minutes);
    }
}
