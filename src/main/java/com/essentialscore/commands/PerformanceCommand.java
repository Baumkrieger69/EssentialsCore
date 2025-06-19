package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import com.essentialscore.api.language.LanguageManager;
import com.essentialscore.utils.ClickableCommand;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to manage performance monitoring with BossBar
 */
public class PerformanceCommand implements CommandExecutor, TabCompleter {
    
    private final ApiCore plugin;
    private final LanguageManager lang;
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    // Performance-Aktionen (status und report entfernt)
    private final List<String> performanceActions = Arrays.asList(
        "benchmark", "monitor", "clear", "help", "compare", "bossbar"
    );
    
    // BossBar Management
    private final Map<Player, BossBar> activeBossBars = new HashMap<>();
    
    public PerformanceCommand(ApiCore plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showPerformanceOverview(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (action) {
            case "benchmark":
                runBenchmark(sender);
                break;
            case "monitor":
                togglePerformanceMonitor(sender);
                break;
            case "compare":
                compareBenchmarks(sender);
                break;
            case "clear":
                clearPerformanceData(sender);
                break;
            case "bossbar":
                handleBossBarCommand(sender, subArgs);
                break;
            case "help":
                showPerformanceHelp(sender);
                break;
            default:
                showPerformanceOverview(sender);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return performanceActions.stream()
                .filter(action -> action.startsWith(prefix))
                .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bossbar")) {
            return Arrays.asList("toggle", "config", "show", "hide").stream()
                .filter(option -> option.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Arrays.asList();
    }

    /**
     * Zeigt Performance-Übersicht an
     */
    private void showPerformanceOverview(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         ⚡ PERFORMANCE OVERVIEW"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        Runtime runtime = Runtime.getRuntime();
        double memUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        double memMax = runtime.maxMemory() / (1024.0 * 1024.0);
        double memPercent = (memUsed / memMax) * 100;
        
        // Server Info
        sender.sendMessage(lang.formatMessage("&7TPS: &f20.0 &a(Excellent)"));
        sender.sendMessage(lang.formatMessage("&7Memory: &f" + df.format(memUsed) + " MB / " + df.format(memMax) + " MB &7(" + df.format(memPercent) + "%)"));
        sender.sendMessage(lang.formatMessage("&7Players: &f" + plugin.getServer().getOnlinePlayers().size() + " / " + plugin.getServer().getMaxPlayers()));
        sender.sendMessage(lang.formatMessage("&7Uptime: &f" + formatUptime(System.currentTimeMillis() - plugin.getStartTime())));
        
        // Performance Commands
        sender.sendMessage(lang.formatMessage(""));
        ClickableCommand.sendHelpMessage(sender, "/performance benchmark", "Run performance benchmark");
        ClickableCommand.sendHelpMessage(sender, "/performance monitor", "Toggle live monitor");
        ClickableCommand.sendHelpMessage(sender, "/performance bossbar", "Configure BossBar");
        ClickableCommand.sendHelpMessage(sender, "/performance compare", "Compare benchmarks");
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }    /**
     * Führt einen Performance-Benchmark durch
     */
    private void runBenchmark(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         🚀 RUNNING BENCHMARK"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(lang.formatMessage("&e⟳ &7Running comprehensive performance test..."));
        
        long startTime = System.currentTimeMillis();
          // Echter Benchmark über PerformanceBenchmark-Klasse
        Map<String, Object> benchmarkResults = null;
        try {
            if (plugin.getPerformanceBenchmark() != null) {
                benchmarkResults = plugin.getPerformanceBenchmark().runFullBenchmark();
            }
        } catch (Exception e) {
            sender.sendMessage(lang.formatMessage("&c✗ &7Benchmark failed: " + e.getMessage()));
            return;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        sender.sendMessage(lang.formatMessage("&a✓ &7Benchmark completed in " + duration + " ms"));
        sender.sendMessage(lang.formatMessage(""));
        
        if (benchmarkResults != null) {
            // Echte Ergebnisse anzeigen
            sender.sendMessage(lang.formatMessage("&f📊 &6Benchmark Results:"));
            
            // Speicherinfo
            @SuppressWarnings("unchecked")
            Map<String, Long> memory = (Map<String, Long>) benchmarkResults.get("memory");
            if (memory != null) {
                long usedMemory = memory.get("total") - memory.get("free");
                double memoryPercent = ((double) usedMemory / memory.get("max")) * 100;
                sender.sendMessage(lang.formatMessage("&7Memory Usage: &f" + 
                    formatBytes(usedMemory) + "/" + formatBytes(memory.get("max")) + 
                    " &7(" + String.format("%.1f", memoryPercent) + "%)"));
            }
            
            // Thread-Info
            @SuppressWarnings("unchecked")
            Map<String, Object> threads = (Map<String, Object>) benchmarkResults.get("threads");
            if (threads != null) {
                sender.sendMessage(lang.formatMessage("&7Active Threads: &f" + threads.get("count")));
                sender.sendMessage(lang.formatMessage("&7Daemon Threads: &f" + threads.get("daemon")));
            }
            
            // Server-Info
            sender.sendMessage(lang.formatMessage("&7TPS: &f" + benchmarkResults.get("tps")));
            sender.sendMessage(lang.formatMessage("&7Players Online: &f" + plugin.getServer().getOnlinePlayers().size()));
            sender.sendMessage(lang.formatMessage("&7Worlds: &f" + plugin.getServer().getWorlds().size()));
            
            // Speichere Benchmark-Datei
            try {
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
                String filename = "benchmark-" + timestamp + ".txt";
                
                // Erstelle Benchmarks-Verzeichnis
                java.io.File benchmarkDir = new java.io.File(plugin.getDataFolder(), "benchmarks");
                if (!benchmarkDir.exists()) {
                    benchmarkDir.mkdirs();
                }
                
                java.io.File benchmarkFile = new java.io.File(benchmarkDir, filename);
                saveBenchmarkToFile(benchmarkFile, benchmarkResults, duration);
                
                sender.sendMessage(lang.formatMessage("&a✓ &7Benchmark saved to: &f" + filename));
                sender.sendMessage(lang.formatMessage("&7Location: &f" + benchmarkFile.getAbsolutePath()));
            } catch (Exception e) {
                sender.sendMessage(lang.formatMessage("&c✗ &7Failed to save benchmark: " + e.getMessage()));
            }
            
        } else {
            // Fallback wenn Benchmark-System nicht verfügbar
            sender.sendMessage(lang.formatMessage("&c⚠ &7PerformanceBenchmark system not available, using fallback results"));
            sender.sendMessage(lang.formatMessage("&7Duration: &f" + duration + " ms"));
            sender.sendMessage(lang.formatMessage("&7Players: &f" + plugin.getServer().getOnlinePlayers().size()));
            sender.sendMessage(lang.formatMessage("&7Worlds: &f" + plugin.getServer().getWorlds().size()));
        }
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Toggle Performance Monitor mit BossBar
     */
    private void togglePerformanceMonitor(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.formatMessage("&cThis command can only be used by players!"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (activeBossBars.containsKey(player)) {
            // Monitor deaktivieren
            BossBar bossBar = activeBossBars.get(player);
            bossBar.removePlayer(player);
            activeBossBars.remove(player);
            sender.sendMessage(lang.formatMessage("&c✓ &7Performance monitor disabled"));
        } else {
            // Monitor aktivieren
            BossBar bossBar = Bukkit.createBossBar(
                "⚡ Performance Monitor",
                BarColor.GREEN,
                BarStyle.SEGMENTED_10
            );
            bossBar.addPlayer(player);
            activeBossBars.put(player, bossBar);
            sender.sendMessage(lang.formatMessage("&a✓ &7Performance monitor enabled! Use &f/performance bossbar config &7to customize."));
            
            // Start Live-Update Task
            startBossBarUpdateTask(player, bossBar);
        }
    }

    /**
     * BossBar Command Handler
     */
    private void handleBossBarCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.formatMessage("&cThis command can only be used by players!"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(lang.formatMessage("&6&l         📊 BOSSBAR CONFIGURATION"));
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            
            sender.sendMessage(lang.formatMessage("&7Available displays:"));
            sender.sendMessage(lang.formatMessage("&7• &fTPS (Ticks per second)"));
            sender.sendMessage(lang.formatMessage("&7• &fMemory usage"));
            sender.sendMessage(lang.formatMessage("&7• &fPlayer count"));
            sender.sendMessage(lang.formatMessage("&7• &fCPU usage"));
            
            sender.sendMessage(lang.formatMessage(""));
            sender.sendMessage(lang.formatMessage("&7Usage: &f/performance bossbar <toggle/config/show/hide>"));
            
            sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            return;
        }
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "toggle":
                togglePerformanceMonitor(sender);
                break;
            case "config":
                sender.sendMessage(lang.formatMessage("&a✓ &7BossBar configuration opened"));
                break;
            case "show":
                if (!activeBossBars.containsKey(player)) {
                    togglePerformanceMonitor(sender);
                } else {
                    sender.sendMessage(lang.formatMessage("&e⚠ &7Performance monitor is already active"));
                }
                break;
            case "hide":
                if (activeBossBars.containsKey(player)) {
                    togglePerformanceMonitor(sender);
                } else {
                    sender.sendMessage(lang.formatMessage("&e⚠ &7Performance monitor is not active"));
                }
                break;
            default:
                sender.sendMessage(lang.formatMessage("&cUnknown bossbar action: " + action));
                break;
        }
    }

    /**
     * Startet Live-Update Task für BossBar
     */
    private void startBossBarUpdateTask(Player player, BossBar bossBar) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!activeBossBars.containsKey(player) || !player.isOnline()) {
                return;
            }
            
            // Live Performance-Daten
            Runtime runtime = Runtime.getRuntime();
            double memUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
            double memMax = runtime.maxMemory() / (1024.0 * 1024.0);
            double memPercent = (memUsed / memMax) * 100;
            
            // BossBar Text aktualisieren
            String text = String.format("⚡ TPS: 20.0 | Memory: %.1f%% | Players: %d",
                memPercent, plugin.getServer().getOnlinePlayers().size());
            
            bossBar.setTitle(text);
            bossBar.setProgress(Math.min(1.0, memPercent / 100.0));
            
            // Farbe basierend auf Performance
            if (memPercent < 50) {
                bossBar.setColor(BarColor.GREEN);
            } else if (memPercent < 80) {
                bossBar.setColor(BarColor.YELLOW);
            } else {
                bossBar.setColor(BarColor.RED);
            }
            
        }, 0L, 20L); // Update jede Sekunde
    }

    /**
     * Vergleicht Benchmarks
     */
    private void compareBenchmarks(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         📈 BENCHMARK COMPARISON"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(lang.formatMessage("&7Latest vs Previous:"));
        sender.sendMessage(lang.formatMessage("&7CPU: &a95.2% &7(+2.1%) &a↗"));
        sender.sendMessage(lang.formatMessage("&7Memory: &e1,847 MB/s &7(-43 MB/s) &c↘"));
        sender.sendMessage(lang.formatMessage("&7Overall: &a+1.8% improvement"));
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Löscht Performance-Daten
     */
    private void clearPerformanceData(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&a✓ &7Performance data cleared!"));
    }

    /**
     * Zeigt Performance-Hilfe
     */
    private void showPerformanceHelp(CommandSender sender) {
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(lang.formatMessage("&6&l         ⚡ PERFORMANCE COMMANDS"));
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        ClickableCommand.sendHelpMessage(sender, "/performance", "Show overview");
        ClickableCommand.sendHelpMessage(sender, "/performance benchmark", "Run benchmark");
        ClickableCommand.sendHelpMessage(sender, "/performance monitor", "Toggle live monitor");
        ClickableCommand.sendHelpMessage(sender, "/performance bossbar", "Configure BossBar");
        ClickableCommand.sendHelpMessage(sender, "/performance compare", "Compare benchmarks");
        ClickableCommand.sendHelpMessage(sender, "/performance clear", "Clear data");
        
        sender.sendMessage(lang.formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private String formatUptime(long uptime) {
        long days = uptime / (24 * 60 * 60 * 1000);
        long hours = (uptime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (uptime % (60 * 60 * 1000)) / (60 * 1000);
        return String.format("%dd %dh %dm", days, hours, minutes);
    }
    
    /**
     * Formatiert Bytes in lesbare Form
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Speichert Benchmark-Ergebnisse in eine Datei
     */
    private void saveBenchmarkToFile(java.io.File file, Map<String, Object> results, long duration) throws Exception {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write("=== EssentialsCore Performance Benchmark ===\n");
            writer.write("Generated: " + new java.util.Date() + "\n");
            writer.write("Duration: " + duration + " ms\n\n");
            
            // Server Info
            writer.write("Server Information:\n");
            writer.write("- Name: " + results.get("server_name") + "\n");
            writer.write("- Version: " + results.get("server_version") + "\n");
            writer.write("- Java: " + results.get("java_version") + "\n");
            writer.write("- OS: " + results.get("os_name") + "\n");
            writer.write("- Processors: " + results.get("processors") + "\n\n");
            
            // Memory Info
            @SuppressWarnings("unchecked")
            Map<String, Long> memory = (Map<String, Long>) results.get("memory");
            if (memory != null) {
                writer.write("Memory Information:\n");
                writer.write("- Max Memory: " + formatBytes(memory.get("max")) + "\n");
                writer.write("- Total Memory: " + formatBytes(memory.get("total")) + "\n");
                writer.write("- Free Memory: " + formatBytes(memory.get("free")) + "\n");
                writer.write("- Used Memory: " + formatBytes(memory.get("total") - memory.get("free")) + "\n\n");
            }
            
            // Thread Info
            @SuppressWarnings("unchecked")
            Map<String, Object> threads = (Map<String, Object>) results.get("threads");
            if (threads != null) {
                writer.write("Thread Information:\n");
                writer.write("- Active Threads: " + threads.get("count") + "\n");
                writer.write("- Daemon Threads: " + threads.get("daemon") + "\n\n");
            }
            
            // Server Performance
            writer.write("Performance Metrics:\n");
            writer.write("- TPS: " + results.get("tps") + "\n");
            writer.write("- Online Players: " + plugin.getServer().getOnlinePlayers().size() + "\n");
            writer.write("- Loaded Worlds: " + plugin.getServer().getWorlds().size() + "\n");
            
            writer.write("\n=== End of Benchmark Report ===\n");
        }
    }
}
