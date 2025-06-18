package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import com.essentialscore.api.language.LanguageManager;
import com.essentialscore.PerformanceMonitor;
import com.essentialscore.PerformanceBenchmark;
import com.essentialscore.ModulePerformanceData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Moderner ApiCore-Hauptbefehl mit coolen Nachrichten und Unterkommandos
 */
public class ApiCoreMainCommand implements CommandExecutor, TabCompleter {
    
    private final ApiCore plugin;
    private final LanguageManager lang;
    private final DecimalFormat df = new DecimalFormat("#.##");
      // Alle verfügbaren Unterkommandos
    private final List<String> subCommands = Arrays.asList(
        "info", "modules", "language", "performance", /* "webui", */ "reload", "debug", "help",
        "threads", "memory", "config", "permissions", "security", "backup", "export"
    );
    
    // Erweiterte Module-Aktionen
    private final List<String> moduleActions = Arrays.asList(
        "list", "info", "enable", "disable", "reload", "load", "unload", "install", "remove"
    );
    
    // Language-Aktionen
    private final List<String> languageActions = Arrays.asList(
        "set", "change", "list", "available", "reload", "current"
    );
      // WebUI-Aktionen (MOVED TO webui-development)
    /*
    private final List<String> webuiActions = Arrays.asList(
        "start", "stop", "restart", "status", "config", "users", "sessions"
    );
    */
    
    public ApiCoreMainCommand(ApiCore plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Wenn keine Argumente, zeige Haupthilfe
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
                break;            case "performance":
            case "perf":
                handlePerformanceCommand(sender, subArgs);
                break;
            // case "webui": // MOVED TO webui-development
            // case "web":
            //     handleWebUICommand(sender, subArgs);
            //     break;
            case "reload":
                handleReloadCommand(sender, subArgs);
                break;
            case "debug":
                handleDebugCommand(sender, subArgs);
                break;
            case "backup":
                handleBackupCommand(sender, subArgs);
                break;
            case "memory":
                handleMemoryCommand(sender, subArgs);
                break;
            case "threads":
                handleThreadsCommand(sender, subArgs);
                break;
            case "security":
                handleSecurityCommand(sender, subArgs);
                break;
            case "export":
                handleExportCommand(sender, subArgs);
                break;            case "config":
                handleConfigCommand(sender, subArgs);
                break;
            case "permissions":
            case "perms":
                handlePermissionsCommand(sender, subArgs);
                break;
            case "help":
            default:
                showMainHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Zeigt die Haupthilfe mit coolen Nachrichten
     */    private void showMainHelp(CommandSender sender) {
        // Header senden
        lang.sendMessage(sender, "help.header");
        
        // Main commands
        lang.sendMessage(sender, "help.main-commands");
        lang.sendMessage(sender, "help.commands.apicore");
        lang.sendMessage(sender, "help.commands.info");
        lang.sendMessage(sender, "help.commands.modules");
        lang.sendMessage(sender, "help.commands.language");        lang.sendMessage(sender, "help.commands.performance");
        // lang.sendMessage(sender, "help.commands.webui"); // MOVED TO webui-development
        lang.sendMessage(sender, "help.commands.backup");
        lang.sendMessage(sender, "help.commands.memory");
        lang.sendMessage(sender, "help.commands.threads");
        lang.sendMessage(sender, "help.commands.security");
        lang.sendMessage(sender, "help.commands.export");
        lang.sendMessage(sender, "help.commands.config");
        lang.sendMessage(sender, "help.commands.reload");
        lang.sendMessage(sender, "help.commands.debug");
        lang.sendMessage(sender, "help.commands.help");
        
        // Footer senden
        lang.sendMessage(sender, "help.footer");
    }
      /**
     * Behandelt den Info-Befehl
     */
    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.info")) {
            lang.sendError(sender, "command.no-permission");
            return;        }
        
        lang.sendMessage(sender, "info.header");
        lang.sendMessage(sender, "info.version", "1.0.12");
        lang.sendMessage(sender, "info.author", "Baumkrieger69");
        lang.sendMessage(sender, "info.website", "https://github.com/Baumkrieger69/EssentialsCore");
        
        // Module-Info
        int moduleCount = plugin.getModuleManager() != null ? 
            plugin.getModuleManager().getLoadedModules().size() : 0;
        lang.sendMessage(sender, "info.modules-loaded", moduleCount);
        
        // Uptime
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptimeStr = formatUptime(uptime);
        lang.sendMessage(sender, "info.uptime", uptimeStr);
        
        // Memory Usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        lang.sendMessage(sender, "info.memory-usage", usedMemory, totalMemory);
    }
    
    /**
     * Behandelt den Module-Befehl
     */
    private void handleModulesCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.modules")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            // Liste alle Module
            if (plugin.getModuleManager() == null) {
                lang.sendError(sender, "modules.no-modules");
                return;
            }
              List<String> modules = new ArrayList<>(plugin.getModuleManager().getLoadedModules().keySet());
            lang.sendMessage(sender, "modules.list-header", modules.size());
            
            if (modules.isEmpty()) {
                lang.sendMessage(sender, "modules.no-modules");} else {                for (String moduleName : modules) {
                    // Get actual module info
                    var moduleInfo = plugin.getModuleManager().getModuleInfo(moduleName);
                    String version = moduleInfo != null ? moduleInfo.getVersion() : "Unknown";
                    boolean enabled = plugin.getModuleManager().isModuleEnabled(moduleName);
                    
                    if (enabled) {
                        sender.sendMessage(lang.formatMessage(lang.getMessage("modules.list-item-enabled", moduleName, version)));
                    } else {
                        sender.sendMessage(lang.formatMessage(lang.getMessage("modules.list-item-disabled", moduleName, version)));
                    }
                }
            }
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "list":
                handleModulesCommand(sender, new String[0]); // Rekursiv für List
                break;
            case "info":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore modules info <module>");
                    return;
                }
                showModuleInfo(sender, args[1]);
                break;
            case "enable":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore modules enable <module>");
                    return;
                }
                enableModule(sender, args[1]);
                break;
            case "disable":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore modules disable <module>");
                    return;
                }
                disableModule(sender, args[1]);
                break;
            case "reload":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore modules reload <module>");
                    return;
                }
                reloadModule(sender, args[1]);
                break;
            case "load":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore modules load <module>");
                    return;
                }
                loadModule(sender, args[1]);
                break;
            case "unload":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore modules unload <module>");
                    return;
                }
                unloadModule(sender, args[1]);
                break;
            case "install":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore modules install <module>");
                    return;
                }
                installModule(sender, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore modules remove <module>");
                    return;
                }
                removeModule(sender, args[1]);
                break;
            default:
                lang.sendError(sender, "command.unknown-command");
                break;
        }
    }
    
    /**
     * Behandelt den Language-Befehl
     */
    private void handleLanguageCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.language")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
          if (args.length == 0) {
            // Aktuelle Sprache anzeigen
            lang.sendMessage(sender, "language.current", lang.getCurrentLanguage());
            String available = String.join(", ", lang.getSupportedLanguages());
            lang.sendMessage(sender, "language.available", available);
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "set":
            case "change":
                if (args.length < 2) {
                    lang.sendError(sender, "error.invalid-syntax", "/apicore language set <language>");
                    return;
                }
                setLanguage(sender, args[1]);
                break;            case "list":
            case "available":
                String available = String.join(", ", lang.getSupportedLanguages());
                lang.sendMessage(sender, "language.available", available);
                break;
            case "reload":
                lang.reload();
                lang.sendSuccess(sender, "language.reloaded");
                break;
            case "current":
                lang.sendMessage(sender, "language.current", lang.getCurrentLanguage());
                break;
            default:
                lang.sendError(sender, "command.unknown-command");
                break;
        }
    }
      /**
     * Behandelt den Performance-Befehl
     */
    private void handlePerformanceCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.performance")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            // Show basic performance status
            showPerformanceStatus(sender);
            return;
        }
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "status":
                showPerformanceStatus(sender);
                break;
            case "benchmark":
                runPerformanceBenchmark(sender);
                break;
            case "report":
                generatePerformanceReport(sender);
                break;
            case "monitor":
                showPerformanceMonitor(sender);
                break;
            default:
                lang.sendError(sender, "command.unknown-action", action);
                showPerformanceHelp(sender);
                break;
        }
    }
    
    private void showPerformanceStatus(CommandSender sender) {
        lang.sendMessage(sender, "performance.header");
        
        // TPS (falls verfügbar)
        try {
            double[] tps = plugin.getServer().getTPS();
            String tpsStr = df.format(tps[0]);
            String tpsColor = getTpsColor(tps[0]);
            lang.sendMessage(sender, "performance.tps", tpsColor + tpsStr);
        } catch (Exception e) {
            lang.sendMessage(sender, "performance.tps", "§cN/A");
        }
        
        // Memory
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        double percentage = (double) usedMemory / totalMemory * 100;
        lang.sendMessage(sender, "performance.memory", usedMemory, totalMemory, df.format(percentage));
        
        // Threads
        int threadCount = Thread.activeCount();
        lang.sendMessage(sender, "performance.threads", threadCount);
        
        // Uptime
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptimeStr = formatUptime(uptime);
        lang.sendMessage(sender, "performance.uptime", uptimeStr);
    }
      private void runPerformanceBenchmark(CommandSender sender) {
        lang.sendMessage(sender, "performance.benchmark.starting");
        
        // Run benchmark in async thread to not block the main thread
        plugin.getThreadManager().submit(() -> {
            try {
                PerformanceBenchmark benchmark = plugin.getPerformanceBenchmark();
                if (benchmark != null) {
                    Map<String, Object> results = benchmark.runFullBenchmark();
                    
                    // Send results back on main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        lang.sendMessage(sender, "performance.benchmark.header");
                        for (Map.Entry<String, Object> entry : results.entrySet()) {
                            lang.sendMessage(sender, "performance.benchmark.result", 
                                entry.getKey(), entry.getValue().toString());
                        }
                        lang.sendMessage(sender, "performance.benchmark.complete");
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        lang.sendError(sender, "performance.benchmark.unavailable");
                    });
                }
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    lang.sendError(sender, "performance.benchmark.error", e.getMessage());
                });
            }
        });
    }
      private void generatePerformanceReport(CommandSender sender) {
        try {
            PerformanceMonitor monitor = plugin.getPerformanceMonitor();
            if (monitor != null) {
                // Generate a simple performance report since generateReport() doesn't exist
                Map<String, ModulePerformanceData> perfData = monitor.getPerformanceData();
                lang.sendMessage(sender, "performance.report.header");
                for (Map.Entry<String, ModulePerformanceData> entry : perfData.entrySet()) {
                    String moduleName = entry.getKey();
                    ModulePerformanceData data = entry.getValue();                    sender.sendMessage("§e" + moduleName + ": " + 
                        String.format("%.2fms avg, %d calls", 
                            data.getAverageExecutionTime(), data.getInvocationCount()));
                }
            } else {
                lang.sendError(sender, "performance.report.unavailable");
            }
        } catch (Exception e) {
            lang.sendError(sender, "performance.report.error", e.getMessage());
        }
    }
    
    private void showPerformanceMonitor(CommandSender sender) {
        PerformanceMonitor monitor = plugin.getPerformanceMonitor();
        if (monitor != null) {
            var performanceData = monitor.getPerformanceData();
            lang.sendMessage(sender, "performance.monitor.header");
            
            for (Map.Entry<String, ModulePerformanceData> entry : performanceData.entrySet()) {
                String moduleName = entry.getKey();
                ModulePerformanceData data = entry.getValue();                lang.sendMessage(sender, "performance.monitor.module", 
                    moduleName, data.getAverageExecutionTime(), data.getInvocationCount());
            }
        } else {
            lang.sendError(sender, "performance.monitor.unavailable");
        }
    }
    
    private void showPerformanceHelp(CommandSender sender) {
        lang.sendMessage(sender, "performance.help.header");
        lang.sendMessage(sender, "performance.help.status");
        lang.sendMessage(sender, "performance.help.benchmark");
        lang.sendMessage(sender, "performance.help.report");
        lang.sendMessage(sender, "performance.help.monitor");
    }
    /**
     * WebUI command handler - MOVED TO webui-development
     */
    /*
    private void handleWebUICommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.webui")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
          if (args.length == 0) {
            // Status anzeigen
            boolean running = plugin.getWebUIManager() != null && plugin.getWebUIManager().isRunning();
            if (running) {
                int port = plugin.getWebUIManager().getConfig().getInt("http.port", 8080);
                lang.sendMessage(sender, "webui.status.running", port);
                
                // Additional info
                String url = "http://" + plugin.getWebUIManager().getConfig().getString("http.bind-address", "localhost") + ":" + port;
                lang.sendMessage(sender, "webui.info.url", url);
            } else {
                lang.sendMessage(sender, "webui.status.stopped");
            }
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "start":
                startWebUI(sender);
                break;
            case "stop":
                stopWebUI(sender);
                break;
            case "restart":
                restartWebUI(sender);
                break;
            case "status":
                handleWebUICommand(sender, new String[0]); // Rekursiv für Status
                break;
            case "config":
                showWebUIConfig(sender);
                break;
            case "users":
                showWebUIUsers(sender);
                break;
            case "sessions":
                showWebUISessions(sender);
                break;
            default:
                lang.sendError(sender, "command.unknown-command");
                break;
        }    }
    */
    
    /*
    // ALL WebUI METHODS MOVED TO webui-development
    private void startWebUI(CommandSender sender) {
        try {
            if (plugin.getWebUIManager() != null && plugin.getWebUIManager().isRunning()) {
                lang.sendMessage(sender, "webui.errors.already-running", 
                    plugin.getWebUIManager().getConfig().getInt("http.port", 8080));
                return;
            }
            
            lang.sendMessage(sender, "webui.status.starting", 
                plugin.getWebUIManager().getConfig().getInt("http.port", 8080));
            
            // Use the new startWebUI method that includes chat notifications
            if (plugin.getWebUIManager().startWebUI()) {
                int port = plugin.getWebUIManager().getConfig().getInt("http.port", 8080);
                lang.sendMessage(sender, "webui.actions.started", port);
            } else {
                lang.sendMessage(sender, "webui.errors.start-failed", "Siehe Logs für Details");
            }
        } catch (Exception e) {
            lang.sendMessage(sender, "webui.errors.start-failed", e.getMessage());
        }
    }    private void stopWebUI(CommandSender sender) {
        try {
            if (plugin.getWebUIManager() == null || !plugin.getWebUIManager().isRunning()) {
                lang.sendMessage(sender, "webui.errors.not-running");
                return;
            }
            
            lang.sendMessage(sender, "webui.status.stopping");
            if (plugin.getWebUIManager().stopWebUI()) {
                lang.sendMessage(sender, "webui.actions.stopped");
            } else {
                lang.sendMessage(sender, "webui.errors.stop-failed", "Siehe Logs für Details");
            }
        } catch (Exception e) {
            lang.sendMessage(sender, "webui.errors.stop-failed", e.getMessage());
        }
    }    private void restartWebUI(CommandSender sender) {
        try {
            if (plugin.getWebUIManager() != null && plugin.getWebUIManager().isRunning()) {
                lang.sendMessage(sender, "webui.status.stopping");
                plugin.getWebUIManager().stopWebUI();
            }
            
            lang.sendMessage(sender, "webui.status.starting", 
                plugin.getWebUIManager().getConfig().getInt("http.port", 8080));
            
            if (plugin.getWebUIManager().startWebUI()) {
                lang.sendMessage(sender, "webui.actions.restarted");
            } else {
                lang.sendMessage(sender, "webui.errors.start-failed", "Siehe Logs für Details");
            }        } catch (Exception e) {
            lang.sendMessage(sender, "webui.errors.start-failed", e.getMessage());
        }
    }
    
    private void showWebUIConfig(CommandSender sender) {
        sender.sendMessage(lang.getMessage("webui.config-header"));
        sender.sendMessage("§7Port: §e" + plugin.getConfig().getInt("webui.port", 8080));
        sender.sendMessage("§7Bind Address: §e" + plugin.getConfig().getString("webui.bind-address", "0.0.0.0"));
        sender.sendMessage("§7SSL Enabled: §e" + plugin.getConfig().getBoolean("webui.ssl.enabled", false));
    }
      private void showWebUIUsers(CommandSender sender) {
        if (plugin.getWebUIManager() == null || !plugin.getWebUIManager().isRunning()) {
            lang.sendMessage(sender, "webui.errors.not-running");
            return;
        }
        
        sender.sendMessage(lang.formatMessage(lang.getMessage("webui.header")));
        
        try {
            var stats = plugin.getWebUIManager().getStatistics();
            Object activeUsers = stats.get("activeUsers");
            if (activeUsers != null) {
                sender.sendMessage(lang.formatMessage(lang.getMessage("webui.info.users", activeUsers)));
            } else {
                sender.sendMessage(lang.formatMessage("&7Active users: &e0"));
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&7Active users: &cError retrieving data"));
        }
    }
      private void showWebUISessions(CommandSender sender) {
        if (plugin.getWebUIManager() == null || !plugin.getWebUIManager().isRunning()) {
            lang.sendMessage(sender, "webui.errors.not-running");
            return;
        }
        
        sender.sendMessage(lang.formatMessage(lang.getMessage("webui.header")));
        
        try {
            var authManager = plugin.getWebUIManager().getAuthManager();
            if (authManager != null) {
                // Get session count - this would need to be implemented in AuthenticationManager
                sender.sendMessage(lang.formatMessage("&7Active sessions: &e" + "Unknown"));
                sender.sendMessage(lang.formatMessage("&7Session timeout: &e" + 
                    plugin.getWebUIManager().getConfig().getInt("auth.session-timeout", 3600) + "s"));
            } else {
                sender.sendMessage(lang.formatMessage("&7Authentication manager not available"));
            }        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&7Active sessions: &cError retrieving data"));
        }
    }
    */
    
    private void showModuleInfo(CommandSender sender, String moduleName) {
        if (plugin.getModuleManager() == null) {
            lang.sendError(sender, "modules.errors.not-found", moduleName);
            return;
        }
        
        var loadedModules = plugin.getLoadedModules();
        if (loadedModules.containsKey(moduleName)) {
            sender.sendMessage(lang.formatMessage(lang.getMessage("modules.info.header", moduleName)));
            sender.sendMessage(lang.formatMessage(lang.getMessage("modules.info.name", moduleName)));
            sender.sendMessage(lang.formatMessage(lang.getMessage("modules.info.version", "1.0.0")));
            sender.sendMessage(lang.formatMessage(lang.getMessage("modules.info.status", 
                lang.getMessage("modules.status.enabled"))));
            sender.sendMessage(lang.formatMessage(lang.getMessage("modules.info.description", "Module description")));
        } else {
            lang.sendMessage(sender, "modules.errors.not-found", moduleName);
        }
    }    private void enableModule(CommandSender sender, String moduleName) {
        if (plugin.getModuleManager() == null) {
            lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            return;
        }
        
        try {
            boolean success = plugin.getModuleManager().enableModule(moduleName);
            if (success) {
                lang.sendMessage(sender, "modules.actions.enabled", moduleName);
            } else {
                lang.sendMessage(sender, "modules.errors.already-enabled", moduleName);
            }
        } catch (Exception e) {
            lang.sendMessage(sender, "modules.errors.load-failed", moduleName, e.getMessage());
        }
    }
      private void disableModule(CommandSender sender, String moduleName) {
        if (plugin.getModuleManager() == null) {
            lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            return;
        }
        
        try {
            boolean success = plugin.getModuleManager().disableModule(moduleName);
            if (success) {
                lang.sendMessage(sender, "modules.actions.disabled", moduleName);
            } else {
                lang.sendMessage(sender, "modules.errors.already-disabled", moduleName);
            }
        } catch (Exception e) {
            lang.sendMessage(sender, "modules.errors.unload-failed", moduleName, e.getMessage());
        }
    }
      private void reloadModule(CommandSender sender, String moduleName) {
        if (plugin.getModuleManager() == null) {
            lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            return;
        }
        
        try {
            boolean success = plugin.getModuleManager().reloadModule(moduleName);
            if (success) {
                lang.sendMessage(sender, "modules.actions.reloaded", moduleName);
            } else {
                lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            }
        } catch (Exception e) {
            lang.sendMessage(sender, "modules.errors.load-failed", moduleName, e.getMessage());
        }
    }
      private void loadModule(CommandSender sender, String moduleName) {
        if (plugin.getModuleManager() == null) {
            lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            return;
        }
        
        try {
            // Load module from file
            File moduleFile = new File(plugin.getDataFolder(), "modules/" + moduleName + ".jar");
            if (!moduleFile.exists()) {
                lang.sendMessage(sender, "modules.errors.not-found", moduleName);
                return;
            }
            
            boolean success = plugin.getModuleManager().loadModule(moduleFile);
            if (success) {
                lang.sendMessage(sender, "modules.actions.loaded", moduleName);
            } else {
                lang.sendMessage(sender, "modules.errors.load-failed", moduleName, "Module already loaded or invalid");
            }
        } catch (Exception e) {
            lang.sendMessage(sender, "modules.errors.load-failed", moduleName, e.getMessage());
        }
    }
      private void unloadModule(CommandSender sender, String moduleName) {
        if (plugin.getModuleManager() == null) {
            lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            return;
        }
        
        try {
            boolean success = plugin.getModuleManager().unloadModule(moduleName);
            if (success) {
                lang.sendMessage(sender, "modules.actions.unloaded", moduleName);
            } else {
                lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            }
        } catch (Exception e) {
            lang.sendMessage(sender, "modules.errors.unload-failed", moduleName, e.getMessage());
        }
    }
      private void installModule(CommandSender sender, String moduleName) {
        // For now, installModule is the same as loadModule since modules need to be placed manually
        lang.sendMessage(sender, "general.info", "Use /apicore modules load <module> to load an existing module file");
        loadModule(sender, moduleName);
    }
      private void removeModule(CommandSender sender, String moduleName) {
        if (plugin.getModuleManager() == null) {
            lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            return;
        }
        
        try {
            // First unload the module
            boolean unloaded = plugin.getModuleManager().unloadModule(moduleName);
            if (unloaded) {
                // Remove the module file
                File moduleFile = new File(plugin.getDataFolder(), "modules/" + moduleName + ".jar");
                if (moduleFile.exists() && moduleFile.delete()) {
                    lang.sendMessage(sender, "modules.actions.unloaded", moduleName);
                    lang.sendMessage(sender, "general.info", "Module file removed from modules directory");
                } else {
                    lang.sendMessage(sender, "modules.actions.unloaded", moduleName);
                    lang.sendMessage(sender, "general.warning", "Could not remove module file - please remove manually");
                }
            } else {
                lang.sendMessage(sender, "modules.errors.not-found", moduleName);
            }
        } catch (Exception e) {
            lang.sendMessage(sender, "modules.errors.unload-failed", moduleName, e.getMessage());
        }
    }
    
    private void setLanguage(CommandSender sender, String language) {
        if (lang.setLanguage(language)) {
            lang.sendSuccess(sender, "language.changed", language);
        } else {
            lang.sendError(sender, "language.invalid", language);
        }
    }
      private void handleReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.reload")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            // Show reload options
            sender.sendMessage(lang.formatMessage("&e&lReload-Optionen:"));
            sender.sendMessage(lang.formatMessage("&7/apicore reload all &8- &7Alles neu laden"));
            sender.sendMessage(lang.formatMessage("&7/apicore reload config &8- &7Konfiguration neu laden"));
            sender.sendMessage(lang.formatMessage("&7/apicore reload modules &8- &7Module neu laden"));
            sender.sendMessage(lang.formatMessage("&7/apicore reload language &8- &7Sprache neu laden"));
            sender.sendMessage(lang.formatMessage("&7/apicore reload permissions &8- &7Berechtigungen neu laden"));
            return;
        }
        
        String component = args[0].toLowerCase();
        long startTime = System.currentTimeMillis();
        
        try {
            switch (component) {
                case "all":
                    reloadAll(sender, startTime);
                    break;
                case "config":
                    reloadConfig(sender, startTime);
                    break;
                case "modules":
                    reloadModules(sender, startTime);
                    break;
                case "language":
                    reloadLanguage(sender, startTime);
                    break;
                case "permissions":
                    reloadPermissions(sender, startTime);
                    break;
                default:
                    sender.sendMessage(lang.formatMessage("&c✗ &7Unbekannte Komponente: " + component));
                    return;
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Fehler beim Neuladen: " + e.getMessage()));
        }
    }
    
    private void reloadAll(CommandSender sender, long startTime) {
        lang.sendMessage(sender, "reload.starting");
        
        // Reload config
        lang.sendMessage(sender, "reload.config");
        plugin.reloadConfig();
        
        // Reload language files
        lang.sendMessage(sender, "reload.language");
        lang.reload();
        
        // Reload modules if available
        if (plugin.getModuleManager() != null) {
            lang.sendMessage(sender, "reload.modules");
            var loadedModules = plugin.getLoadedModules();
            for (String moduleName : loadedModules.keySet()) {
                try {
                    plugin.getModuleManager().reloadModule(moduleName);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to reload module " + moduleName + ": " + e.getMessage());
                }
            }
        }        // Reload permissions - reinitialize permission system
        if (plugin.getPermissionManager() != null) {
            try {
                // Re-hook into permissions system
                ((com.essentialscore.PermissionManager) plugin.getPermissionManager()).hookIntoPermissions();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to refresh permissions: " + e.getMessage());
            }
        }
        
        long endTime = System.currentTimeMillis();
        lang.sendMessage(sender, "reload.complete", (endTime - startTime));
    }
    
    private void reloadConfig(CommandSender sender, long startTime) {
        lang.sendMessage(sender, "reload.config");
        plugin.reloadConfig();
        long endTime = System.currentTimeMillis();
        sender.sendMessage(lang.formatMessage("&a✓ &7Konfiguration neu geladen (&f" + (endTime - startTime) + "ms&7)"));
    }
    
    private void reloadModules(CommandSender sender, long startTime) {
        if (plugin.getModuleManager() == null) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Module-Manager nicht verfügbar"));
            return;
        }
        
        lang.sendMessage(sender, "reload.modules");
        var loadedModules = plugin.getLoadedModules();
        int successful = 0;
        int failed = 0;
        
        for (String moduleName : loadedModules.keySet()) {
            try {
                plugin.getModuleManager().reloadModule(moduleName);
                successful++;
            } catch (Exception e) {
                failed++;
                plugin.getLogger().warning("Failed to reload module " + moduleName + ": " + e.getMessage());
            }
        }
        
        long endTime = System.currentTimeMillis();
        sender.sendMessage(lang.formatMessage("&a✓ &7Module neu geladen: &f" + successful + "&7 erfolgreich, &f" + failed + "&7 fehlgeschlagen (&f" + (endTime - startTime) + "ms&7)"));
    }
    
    private void reloadLanguage(CommandSender sender, long startTime) {
        lang.sendMessage(sender, "reload.language");
        lang.reload();
        long endTime = System.currentTimeMillis();
        sender.sendMessage(lang.formatMessage("&a✓ &7Sprache neu geladen (&f" + (endTime - startTime) + "ms&7)"));
    }
      private void reloadPermissions(CommandSender sender, long startTime) {
        if (plugin.getPermissionManager() == null) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Permission-Manager nicht verfügbar"));
            return;
        }
        
        try {
            ((com.essentialscore.PermissionManager) plugin.getPermissionManager()).hookIntoPermissions();
            long endTime = System.currentTimeMillis();
            sender.sendMessage(lang.formatMessage("&a✓ &7Berechtigungen neu geladen (&f" + (endTime - startTime) + "ms&7)"));
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Fehler beim Neuladen der Berechtigungen: " + e.getMessage()));
        }
    }
    
    /**
     * Behandelt den Debug-Befehl
     */    private void handleDebugCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.debug")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        boolean currentDebug = plugin.getConfig().getBoolean("debug-mode", false);
          if (args.length == 0) {
            showDebugStatus(sender, currentDebug);
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "on":
            case "enable":
                plugin.getConfig().set("debug-mode", true);
                plugin.saveConfig();
                lang.sendSuccess(sender, "debug.enabled");
                // Show debug information immediately
                showDebugInfo(sender);
                break;
            case "off":
            case "disable":
                plugin.getConfig().set("debug-mode", false);
                plugin.saveConfig();
                lang.sendSuccess(sender, "debug.disabled");
                break;
            case "toggle":
                plugin.getConfig().set("debug-mode", !currentDebug);
                plugin.saveConfig();
                if (!currentDebug) {
                    lang.sendSuccess(sender, "debug.enabled");
                    showDebugInfo(sender);
                } else {
                    lang.sendSuccess(sender, "debug.disabled");
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
                    sender.sendMessage(lang.formatMessage("&c✗ &7Verwendung: &e/apicore debug level <LEVEL>"));
                    sender.sendMessage(lang.formatMessage("&7Verfügbare Level: SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST"));
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
        sender.sendMessage(lang.formatMessage("&e&lDebug-Status:"));
        String status = debugEnabled ? "§aAktiviert" : "§cDeaktiviert";
        sender.sendMessage(lang.formatMessage("&7Debug-Modus: " + status));
        sender.sendMessage(lang.formatMessage("&7Log-Level: &f" + plugin.getLogger().getLevel()));
        
        if (debugEnabled) {
            sender.sendMessage(lang.formatMessage("&7Verwenden Sie &e/apicore debug info &7für Details"));
        }
    }
    
    private void showDebugInfo(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&e&lDebug-Informationen:"));
        
        // System Info
        Runtime runtime = Runtime.getRuntime();
        sender.sendMessage(lang.formatMessage("&7Java Version: &f" + System.getProperty("java.version")));
        sender.sendMessage(lang.formatMessage("&7Bukkit Version: &f" + plugin.getServer().getBukkitVersion()));
        sender.sendMessage(lang.formatMessage("&7Freier Speicher: &f" + (runtime.freeMemory() / 1024 / 1024) + "MB"));
        sender.sendMessage(lang.formatMessage("&7Aktive Threads: &f" + Thread.activeCount()));
        
        // Plugin Info
        sender.sendMessage(lang.formatMessage("&7Geladene Module: &f" + plugin.getLoadedModules().size()));
        sender.sendMessage(lang.formatMessage("&7Thread Pool Größe: &f" + ((plugin.getThreadManager() != null) ? "Verfügbar" : "Nicht verfügbar")));
        sender.sendMessage(lang.formatMessage("&7Performance Monitor: &f" + ((plugin.getPerformanceMonitor() != null) ? "Aktiv" : "Inaktiv")));
        
        // Server Info
        sender.sendMessage(lang.formatMessage("&7Online Spieler: &f" + plugin.getServer().getOnlinePlayers().size()));
        sender.sendMessage(lang.formatMessage("&7Geladene Welten: &f" + plugin.getServer().getWorlds().size()));
        
        // Performance Data
        if (plugin.getPerformanceMonitor() != null) {
            var perfData = plugin.getPerformanceMonitor().getPerformanceData();
            sender.sendMessage(lang.formatMessage("&7Performance-Daten: &f" + perfData.size() + " Module überwacht"));
        }
    }
    
    private void setDebugLevel(CommandSender sender, String levelName) {
        try {
            java.util.logging.Level level = java.util.logging.Level.parse(levelName.toUpperCase());
            plugin.getLogger().setLevel(level);
            sender.sendMessage(lang.formatMessage("&a✓ &7Debug-Level gesetzt auf: &f" + level.getName()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Ungültiger Debug-Level: " + levelName));
            sender.sendMessage(lang.formatMessage("&7Verfügbare Level: SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST"));
        }
    }
    
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    private String getTpsColor(double tps) {
        if (tps >= 19.0) return "§a";
        if (tps >= 15.0) return "§e";
        return "§c";
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
                    List<String> perfActions = Arrays.asList("status", "benchmark", "report", "monitor", "clear", "help");
                    completions = perfActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "backup":
                    List<String> backupActions = Arrays.asList("create", "list", "restore", "delete", "info");
                    completions = backupActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "debug":
                    List<String> debugActions = Arrays.asList("on", "off", "toggle", "status", "level");
                    completions = debugActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "threads":
                    List<String> threadActions = Arrays.asList("list", "status", "info", "pool");
                    completions = threadActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "memory":
                    List<String> memoryActions = Arrays.asList("gc", "status", "usage", "dump");
                    completions = memoryActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "config":
                    List<String> configActions = Arrays.asList("reload", "set", "get", "list", "save", "reset");
                    completions = configActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "permissions":
                case "perms":
                    List<String> permActions = Arrays.asList("list", "check", "reload", "user", "group");
                    completions = permActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "security":
                    List<String> securityActions = Arrays.asList("sandbox", "check", "scan", "report");
                    completions = securityActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "export":
                    List<String> exportActions = Arrays.asList("config", "data", "logs", "modules", "all");
                    completions = exportActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                case "reload":
                    List<String> reloadActions = Arrays.asList("config", "modules", "language", "permissions", "all");
                    completions = reloadActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                /*
                case "webui":
                case "web":
                    completions = webuiActions.stream()
                        .filter(action -> action.startsWith(prefix))
                        .collect(Collectors.toList());
                    break;
                */
            }
        } else if (args.length == 3) {
            // Dritte Ebene: Spezifische Parameter
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            String prefix = args[2].toLowerCase();
            
            if ("language".equals(subCommand) && "set".equals(action)) {
                // Verfügbare Sprachen vorschlagen
                completions = lang.getSupportedLanguages().stream()
                    .filter(lang -> lang.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
            } else if ("modules".equals(subCommand) && 
                       (action.equals("info") || action.equals("enable") || action.equals("disable") || action.equals("reload"))) {
                // Verfügbare Module vorschlagen
                if (plugin.getModuleManager() != null) {
                    completions = plugin.getModuleManager().getLoadedModules().keySet().stream()
                        .filter(module -> module.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
                }
            } else if ("backup".equals(subCommand) && ("restore".equals(action) || "delete".equals(action) || "info".equals(action))) {
                // Verfügbare Backup-Dateien vorschlagen
                File backupDir = new File(plugin.getDataFolder(), "backups");
                if (backupDir.exists() && backupDir.isDirectory()) {
                    File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));
                    if (backupFiles != null) {
                        completions = Arrays.stream(backupFiles)
                            .map(File::getName)
                            .filter(name -> name.toLowerCase().startsWith(prefix))
                            .collect(Collectors.toList());
                    }
                }
            } else if ("debug".equals(subCommand) && "level".equals(action)) {
                List<String> levels = Arrays.asList("SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST");
                completions = levels.stream()
                    .filter(level -> level.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
            }
        }
          return completions;
    }
      /**
     * Behandelt den Backup-Befehl
     */
    private void handleBackupCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.backup")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
          if (args.length == 0) {
            lang.sendMessage(sender, "backup.header");
            sender.sendMessage(lang.formatMessage("&7Verfügbare Aktionen:"));
            sender.sendMessage(lang.formatMessage("&e/apicore backup create &8- &7Erstellt ein Backup"));
            sender.sendMessage(lang.formatMessage("&e/apicore backup list &8- &7Zeigt alle Backups"));
            sender.sendMessage(lang.formatMessage("&e/apicore backup restore <name> &8- &7Stellt ein Backup wieder her"));
            sender.sendMessage(lang.formatMessage("&e/apicore backup delete <name> &8- &7Löscht ein Backup"));
            sender.sendMessage(lang.formatMessage("&e/apicore backup info <name> &8- &7Zeigt Backup-Informationen"));
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
                    sender.sendMessage(lang.formatMessage("&c✗ &7Verwendung: &e/apicore backup restore <backup-name>"));
                    return;
                }
                restoreBackup(sender, args[1]);
                break;
            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Verwendung: &e/apicore backup delete <backup-name>"));
                    return;
                }
                deleteBackup(sender, args[1]);
                break;
            case "info":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Verwendung: &e/apicore backup info <backup-name>"));
                    return;
                }
                showBackupInfo(sender, args[1]);
                break;
            default:
                sender.sendMessage(lang.formatMessage("&c✗ &7Unbekannte Aktion: " + action));
                break;
        }
    }
    
    private void createBackup(CommandSender sender) {
        plugin.getThreadManager().submit(() -> {
            try {
                lang.sendMessage(sender, "backup.creating");
                
                File backupDir = new File(plugin.getDataFolder(), "backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
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
                    lang.sendMessage(sender, "backup.created", backupName);
                });
                
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Fehler beim Erstellen des Backups: " + e.getMessage()));
                });
            }
        });
    }
    
    private void listBackups(CommandSender sender) {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            sender.sendMessage(lang.formatMessage("&e&lKeine Backups gefunden"));
            return;
        }
        
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (backupFiles == null || backupFiles.length == 0) {
            sender.sendMessage(lang.formatMessage("&e&lKeine Backups gefunden"));
            return;
        }
        
        sender.sendMessage(lang.formatMessage("&e&lVerfügbare Backups:"));
        for (File backup : backupFiles) {
            long size = backup.length() / 1024; // KB
            String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(backup.lastModified()));
            sender.sendMessage(lang.formatMessage("&7• &f" + backup.getName() + " &8(&7" + size + "KB, " + date + "&8)"));
        }
    }
    
    private void restoreBackup(CommandSender sender, String backupName) {
        File backupFile = new File(new File(plugin.getDataFolder(), "backups"), backupName);
        if (!backupFile.exists()) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Backup nicht gefunden: " + backupName));
            return;
        }
        
        sender.sendMessage(lang.formatMessage("&e⚠ &7Backup-Wiederherstellung ist nur im Wartungsmodus verfügbar"));
        sender.sendMessage(lang.formatMessage("&7Bitte stoppen Sie den Server und führen Sie die Wiederherstellung manuell durch"));
    }
    
    private void deleteBackup(CommandSender sender, String backupName) {
        File backupFile = new File(new File(plugin.getDataFolder(), "backups"), backupName);
        if (!backupFile.exists()) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Backup nicht gefunden: " + backupName));
            return;
        }
        
        if (backupFile.delete()) {
            sender.sendMessage(lang.formatMessage("&a✓ &7Backup gelöscht: " + backupName));
        } else {
            sender.sendMessage(lang.formatMessage("&c✗ &7Fehler beim Löschen des Backups"));
        }
    }
    
    private void showBackupInfo(CommandSender sender, String backupName) {
        File backupFile = new File(new File(plugin.getDataFolder(), "backups"), backupName);
        if (!backupFile.exists()) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Backup nicht gefunden: " + backupName));
            return;
        }
        
        long size = backupFile.length();
        String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(backupFile.lastModified()));
        
        sender.sendMessage(lang.formatMessage("&e&lBackup-Informationen:"));
        sender.sendMessage(lang.formatMessage("&7Name: &f" + backupName));
        sender.sendMessage(lang.formatMessage("&7Größe: &f" + (size / 1024) + " KB"));
        sender.sendMessage(lang.formatMessage("&7Erstellt: &f" + date));
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
    
    /**
     * Behandelt den Memory-Befehl
     */
    private void handleMemoryCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.memory")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
          lang.sendMessage(sender, "memory.header");
        lang.sendMessage(sender, "memory.used", usedMemory);
        lang.sendMessage(sender, "memory.free", freeMemory);
        lang.sendMessage(sender, "memory.total", totalMemory);
        lang.sendMessage(sender, "memory.max", maxMemory);
        
        double usagePercent = (double) usedMemory / totalMemory * 100;
        lang.sendMessage(sender, "memory.usage-percent", df.format(usagePercent));
        
        if (args.length > 0 && "gc".equals(args[0].toLowerCase())) {
            lang.sendMessage(sender, "memory.gc-started");
            System.gc();
            lang.sendMessage(sender, "memory.gc-completed");
        }
    }
    
    /**
     * Behandelt den Threads-Befehl
     */
    private void handleThreadsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.threads")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        sender.sendMessage(lang.getMessage("threads.header"));
        sender.sendMessage(lang.getMessage("threads.active", Thread.activeCount()));
        sender.sendMessage(lang.getMessage("threads.daemon", countDaemonThreads()));
        
        if (args.length > 0 && "list".equals(args[0].toLowerCase())) {
            sender.sendMessage(lang.getMessage("threads.list-header"));
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            for (Thread thread : threads) {
                if (thread != null) {
                    String status = thread.isDaemon() ? "&8[Daemon]" : "&a[Normal]";
                    sender.sendMessage(lang.formatMessage("&7• " + status + " &f" + thread.getName()));
                }
            }
        }
    }
    
    private int countDaemonThreads() {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        int count = 0;
        for (Thread thread : threads) {
            if (thread != null && thread.isDaemon()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Behandelt den Security-Befehl
     */
    private void handleSecurityCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.security")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        sender.sendMessage(lang.getMessage("security.header"));
        sender.sendMessage(lang.getMessage("security.debug-mode", plugin.isDebugMode() ? "&aAktiviert" : "&cDeaktiviert"));
        sender.sendMessage(lang.getMessage("security.sandbox", plugin.getModuleSandbox() != null ? "&aAktiviert" : "&cDeaktiviert"));
        sender.sendMessage(lang.getMessage("security.webui-security", "&aAktiviert"));
        sender.sendMessage(lang.getMessage("security.permissions", plugin.getPermissionManager() != null ? "&aAktiviert" : "&cDeaktiviert"));
        
        if (args.length > 0) {
            String action = args[0].toLowerCase();
            switch (action) {
                case "scan":
                    sender.sendMessage(lang.getMessage("security.scan-started"));
                    sender.sendMessage(lang.getMessage("security.scan-completed"));
                    break;
                default:
                    sender.sendMessage(lang.formatMessage("&c✗ &7Unbekannte Aktion: " + action));
                    break;
            }
        }
    }
    
    /**
     * Behandelt den Export-Befehl
     */    private void handleExportCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.export")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            sender.sendMessage(lang.getMessage("export.header"));
            sender.sendMessage(lang.formatMessage("&e/apicore export config &8- &7Exportiert die Konfiguration"));
            sender.sendMessage(lang.formatMessage("&e/apicore export data &8- &7Exportiert Plugin-Daten"));
            sender.sendMessage(lang.formatMessage("&e/apicore export modules &8- &7Exportiert Modul-Informationen"));
            sender.sendMessage(lang.formatMessage("&e/apicore export logs &8- &7Exportiert Logs"));
            sender.sendMessage(lang.formatMessage("&e/apicore export all &8- &7Exportiert alles"));
            return;
        }
        
        String type = args[0].toLowerCase();
        plugin.getThreadManager().submit(() -> {
            try {
                File exportDir = new File(plugin.getDataFolder(), "exports");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }
                
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
                
                switch (type) {
                    case "config":
                        exportConfig(sender, exportDir, timestamp);
                        break;
                    case "data":
                        exportData(sender, exportDir, timestamp);
                        break;
                    case "modules":
                        exportModules(sender, exportDir, timestamp);
                        break;
                    case "logs":
                        exportLogs(sender, exportDir, timestamp);
                        break;
                    case "all":
                        exportAll(sender, exportDir, timestamp);
                        break;
                    default:
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(lang.formatMessage("&c✗ &7Unbekannter Export-Typ: " + type));
                        });
                        return;
                }
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Fehler beim Export: " + e.getMessage()));
                });
            }
        });
    }
    
    private void exportConfig(CommandSender sender, File exportDir, String timestamp) throws Exception {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.getMessage("export.config-exporting"));
        });
        
        String fileName = "config-export-" + timestamp + ".zip";
        File exportFile = new File(exportDir, fileName);
        
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(exportFile);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {
            
            // Export main config
            addFileToZip(zos, new File(plugin.getDataFolder(), "config.yml"), "config.yml");
            
            // Export language files
            File langDir = new File(plugin.getDataFolder(), "languages");
            if (langDir.exists()) {
                addDirectoryToZip(zos, langDir, "languages/");
            }
        }
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.formatMessage("&a✓ &7Konfiguration exportiert: &f" + fileName));
        });
    }
    
    private void exportData(CommandSender sender, File exportDir, String timestamp) throws Exception {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.formatMessage("&e⚡ &7Exportiere Plugin-Daten..."));
        });
        
        String fileName = "data-export-" + timestamp + ".txt";
        File exportFile = new File(exportDir, fileName);
        
        try (java.io.FileWriter writer = new java.io.FileWriter(exportFile)) {
            writer.write("EssentialsCore Data Export\n");
            writer.write("Timestamp: " + new java.util.Date() + "\n\n");
              // Plugin info
            writer.write("=== Plugin Information ===\n");
            var description = plugin.getPluginMeta();
            writer.write("Version: " + description.getVersion() + "\n");
            writer.write("Authors: " + String.join(", ", description.getAuthors()) + "\n");
            writer.write("Description: " + description.getDescription() + "\n\n");
            
            // System info
            writer.write("=== System Information ===\n");
            writer.write("Java Version: " + System.getProperty("java.version") + "\n");
            writer.write("Bukkit Version: " + plugin.getServer().getBukkitVersion() + "\n");
            writer.write("Online Players: " + plugin.getServer().getOnlinePlayers().size() + "\n");
            writer.write("Loaded Worlds: " + plugin.getServer().getWorlds().size() + "\n\n");
            
            // Module info
            writer.write("=== Module Information ===\n");
            var modules = plugin.getLoadedModules();
            writer.write("Loaded Modules: " + modules.size() + "\n");
            for (String moduleName : modules.keySet()) {
                writer.write("- " + moduleName + "\n");
            }
            writer.write("\n");
            
            // Performance data
            if (plugin.getPerformanceMonitor() != null) {
                writer.write("=== Performance Data ===\n");
                var perfData = plugin.getPerformanceMonitor().getPerformanceData();
                for (Map.Entry<String, ModulePerformanceData> entry : perfData.entrySet()) {
                    ModulePerformanceData data = entry.getValue();
                    writer.write(String.format("%s: %.2fms avg, %d calls\n", 
                        entry.getKey(), data.getAverageExecutionTime(), data.getInvocationCount()));
                }
            }
        }
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.formatMessage("&a✓ &7Plugin-Daten exportiert: &f" + fileName));
        });
    }
    
    private void exportModules(CommandSender sender, File exportDir, String timestamp) throws Exception {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.getMessage("export.modules-exporting"));
        });
        
        String fileName = "modules-export-" + timestamp + ".json";
        File exportFile = new File(exportDir, fileName);
        
        try (java.io.FileWriter writer = new java.io.FileWriter(exportFile)) {
            writer.write("{\n");
            writer.write("  \"timestamp\": \"" + new java.util.Date() + "\",\n");
            writer.write("  \"modules\": [\n");
            
            var modules = plugin.getLoadedModules();
            int count = 0;
            for (Map.Entry<String, com.essentialscore.api.module.ModuleManager.ModuleInfo> entry : modules.entrySet()) {
                if (count > 0) writer.write(",\n");
                writer.write("    {\n");
                writer.write("      \"name\": \"" + entry.getKey() + "\",\n");
                writer.write("      \"enabled\": " + entry.getValue().isEnabled() + "\n");
                writer.write("    }");
                count++;
            }
            
            writer.write("\n  ]\n");
            writer.write("}\n");
        }
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.formatMessage("&a✓ &7Modul-Informationen exportiert: &f" + fileName));
        });
    }
    
    private void exportLogs(CommandSender sender, File exportDir, String timestamp) throws Exception {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.getMessage("export.logs-exporting"));
        });
        
        String fileName = "logs-export-" + timestamp + ".zip";
        File exportFile = new File(exportDir, fileName);
        
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(exportFile);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {
            
            // Export server logs
            File serverLogsDir = new File("logs");
            if (serverLogsDir.exists()) {
                addDirectoryToZip(zos, serverLogsDir, "server-logs/");
            }
            
            // Export plugin logs
            File pluginLogsDir = new File(plugin.getDataFolder(), "logs");
            if (pluginLogsDir.exists()) {
                addDirectoryToZip(zos, pluginLogsDir, "plugin-logs/");
            }
            
            // Export performance logs
            File perfLogsDir = new File(plugin.getDataFolder(), "performance_logs");
            if (perfLogsDir.exists()) {
                addDirectoryToZip(zos, perfLogsDir, "performance-logs/");
            }
        }
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.formatMessage("&a✓ &7Logs exportiert: &f" + fileName));
        });
    }
    
    private void exportAll(CommandSender sender, File exportDir, String timestamp) throws Exception {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.getMessage("export.all-exporting"));
        });
        
        String fileName = "full-export-" + timestamp + ".zip";
        File exportFile = new File(exportDir, fileName);
        
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(exportFile);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {
            
            // Export everything
            addDirectoryToZip(zos, plugin.getDataFolder(), "plugin-data/");
            
            // Export server logs if accessible
            File serverLogsDir = new File("logs");
            if (serverLogsDir.exists()) {
                addDirectoryToZip(zos, serverLogsDir, "server-logs/");
            }
        }
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            sender.sendMessage(lang.formatMessage("&a✓ &7Vollständiger Export erstellt: &f" + fileName));
        });
    }
    
    /**
     * Behandelt den Config-Befehl
     */
    private void handleConfigCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.config")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            sender.sendMessage(lang.getMessage("config.header"));
            sender.sendMessage(lang.formatMessage("&e/apicore config reload &8- &7Lädt die Konfiguration neu"));
            sender.sendMessage(lang.formatMessage("&e/apicore config get <pfad> &8- &7Zeigt einen Konfigurationswert"));
            sender.sendMessage(lang.formatMessage("&e/apicore config set <pfad> <wert> &8- &7Setzt einen Konfigurationswert"));
            sender.sendMessage(lang.formatMessage("&e/apicore config list &8- &7Zeigt alle Konfigurationsschlüssel"));
            return;
        }
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "reload":
                sender.sendMessage(lang.getMessage("config.reloading"));
                plugin.reloadConfig();
                sender.sendMessage(lang.getMessage("config.reloaded"));
                break;
            case "get":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Verwendung: &e/apicore config get <pfad>"));
                    return;
                }
                String value = plugin.getConfig().getString(args[1], "nicht gefunden");
                sender.sendMessage(lang.getMessage("config.value", args[1], value));
                break;
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Verwendung: &e/apicore config set <pfad> <wert>"));
                    return;
                }
                plugin.getConfig().set(args[1], args[2]);
                plugin.saveConfig();
                sender.sendMessage(lang.getMessage("config.set", args[1], args[2]));
                break;
            case "list":
                sender.sendMessage(lang.getMessage("config.list-header"));
                plugin.getConfig().getKeys(true).stream()
                    .limit(10)
                    .forEach(key -> sender.sendMessage(lang.formatMessage("&7• " + key)));
                break;
            default:
                sender.sendMessage(lang.formatMessage("&c✗ &7Unbekannte Aktion: " + action));
                break;
        }
    }
    
    /**
     * Behandelt den Permissions-Befehl
     */
    private void handlePermissionsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialscore.admin.permissions")) {
            lang.sendError(sender, "command.no-permission");
            return;
        }
        
        if (args.length == 0) {
            lang.sendMessage(sender, "permissions.header");
            sender.sendMessage(lang.formatMessage("&7Verfügbare Aktionen:"));
            sender.sendMessage(lang.formatMessage("&e/apicore permissions reload &8- &7Berechtigungen neu laden"));
            sender.sendMessage(lang.formatMessage("&e/apicore permissions check <player> <permission> &8- &7Berechtigung prüfen"));
            sender.sendMessage(lang.formatMessage("&e/apicore permissions list <player> &8- &7Berechtigungen auflisten"));
            sender.sendMessage(lang.formatMessage("&e/apicore permissions info &8- &7System-Informationen"));
            return;
        }
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "reload":
                plugin.getThreadManager().submit(() -> {
                    try {
                        ((com.essentialscore.PermissionManager) plugin.getPermissionManager()).hookIntoPermissions();
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(lang.formatMessage("&a✓ &7Berechtigungen erfolgreich neu geladen!"));
                        });
                    } catch (Exception e) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(lang.formatMessage("&c✗ &7Fehler beim Neu-Laden der Berechtigungen: " + e.getMessage()));
                        });
                    }
                });
                break;
            case "check":
                if (args.length < 3) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Verwendung: &e/apicore permissions check <player> <permission>"));
                    return;
                }
                org.bukkit.entity.Player targetPlayer = plugin.getServer().getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Spieler nicht gefunden: " + args[1]));
                    return;
                }
                boolean hasPermission = targetPlayer.hasPermission(args[2]);
                sender.sendMessage(lang.formatMessage("&7Spieler &e" + targetPlayer.getName() + " &7hat die Berechtigung &e" + args[2] + "&7: " + 
                    (hasPermission ? "&a✓ Ja" : "&c✗ Nein")));
                break;
            case "list":
                if (args.length < 2) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Verwendung: &e/apicore permissions list <player>"));
                    return;
                }
                targetPlayer = plugin.getServer().getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(lang.formatMessage("&c✗ &7Spieler nicht gefunden: " + args[1]));
                    return;
                }
                sender.sendMessage(lang.formatMessage("&e&lBerechtigungen für " + targetPlayer.getName() + ":"));
                targetPlayer.getEffectivePermissions().stream()
                    .limit(10)
                    .forEach(perm -> sender.sendMessage(lang.formatMessage("&7• " + perm.getPermission() + " &8- " + 
                        (perm.getValue() ? "&a✓" : "&c✗"))));
                break;
            case "info":
                sender.sendMessage(lang.formatMessage("&e&lBerechtigungssystem-Informationen:"));
                sender.sendMessage(lang.formatMessage("&7Status: " + (plugin.getPermissionManager() != null ? "&aAktiviert" : "&cDeaktiviert")));
                if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                    sender.sendMessage(lang.formatMessage("&7Vault: &aGefunden"));
                } else {
                    sender.sendMessage(lang.formatMessage("&7Vault: &cNicht gefunden"));
                }
                break;
            default:
                sender.sendMessage(lang.formatMessage("&c✗ &7Unbekannte Aktion: " + action));
                break;
        }
    }
}
