package com.essentialscore.api.performance;

import com.essentialscore.api.command.CommandContext;
import com.essentialscore.api.command.CommandManager;
import com.essentialscore.api.SimpleCommand;
import com.essentialscore.api.module.ModuleRegistry;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * Module for performance monitoring and analytics.
 */
public class PerformanceModule {
    private static final Logger LOGGER = Logger.getLogger(PerformanceModule.class.getName());
    
    private final Plugin plugin;
    private final ModuleRegistry moduleRegistry;
    private final CommandManager commandManager;
    private final ProfilingSystem profilingSystem;
    
    /**
     * Creates a new performance module.
     *
     * @param plugin The plugin
     * @param moduleRegistry The module registry
     * @param commandManager The command manager
     */
    public PerformanceModule(Plugin plugin, ModuleRegistry moduleRegistry, CommandManager commandManager) {
        this.plugin = plugin;
        this.moduleRegistry = moduleRegistry;
        this.commandManager = commandManager;
        this.profilingSystem = new ProfilingSystem(plugin, moduleRegistry);
        
        // Start profiling
        profilingSystem.start();
        
        // Register commands
        registerCommands();
        
        // Initialize monitoring
        initializeMonitoring();
        
        LOGGER.info("Performance module initialized");
    }
    
    /**
     * Initializes performance monitoring for the plugin and modules.
     */
    private void initializeMonitoring() {
        // Use plugin field
        LOGGER.info("Initializing monitoring for plugin: " + plugin.getName());
        
        // Use moduleRegistry field
        LOGGER.info("Monitoring " + getLoadedModulesCount() + " modules");
    }
    
    /**
     * Gets the count of loaded modules.
     * @return Number of loaded modules
     */
    private int getLoadedModulesCount() {
        try {
            // Try to get loaded modules count from moduleRegistry
            return moduleRegistry.getClass().getDeclaredMethods().length; // Fallback to method count
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Registers commands for the performance module.
     */
    private void registerCommands() {
        // Create command methods that match the expected signature
        BiFunction<CommandContext, String[], Boolean> helpHandler = (context, args) -> {
            showHelp(context.getSender());
            return true;
        };
        
        // Main performance command
        SimpleCommand performanceCommand = SimpleCommand.builder("performance", "core")
            .description("Performance monitoring and analytics commands")
            .permission("essentials.performance")
            .build(helpHandler);
        
        // Metrics command
        SimpleCommand metricsCommand = SimpleCommand.builder("metrics", "core")
            .description("View performance metrics")
            .permission("essentials.performance.metrics")
            .parent(performanceCommand)
            .build((context, args) -> handleMetricsCommand(context, args));
        
        // Anomalies command
        SimpleCommand anomaliesCommand = SimpleCommand.builder("anomalies", "core")
            .description("View detected anomalies")
            .permission("essentials.performance.anomalies")
            .parent(performanceCommand)
            .build((context, args) -> handleAnomaliesCommand(context, args));
        
        // Predictions command
        SimpleCommand predictionsCommand = SimpleCommand.builder("predictions", "core")
            .description("View performance predictions")
            .permission("essentials.performance.predictions")
            .parent(performanceCommand)
            .build((context, args) -> handlePredictionsCommand(context, args));
        
        // Tests command
        SimpleCommand testsCommand = SimpleCommand.builder("tests", "core")
            .description("Manage A/B tests")
            .permission("essentials.performance.tests")
            .parent(performanceCommand)
            .build((context, args) -> handleTestsCommand(context, args));
        
        // Analytics command
        SimpleCommand analyticsCommand = SimpleCommand.builder("analytics", "core")
            .description("View usage analytics")
            .permission("essentials.performance.analytics")
            .parent(performanceCommand)
            .build((context, args) -> handleAnalyticsCommand(context, args));
        
        // Add sub-commands to the main command
        // Register subcommands
        List<SimpleCommand> subCommands = Arrays.asList(
            metricsCommand, anomaliesCommand, predictionsCommand, testsCommand, analyticsCommand
        );
        
        // Register commands using CommandManager.registerCommandDefinition
        try {
            commandManager.registerCommandDefinition(performanceCommand);
            for (SimpleCommand subCommand : subCommands) {
                commandManager.registerCommandDefinition(subCommand);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to register performance commands: " + e.getMessage());
        }
    }
    
    /**
     * Shows help for the performance commands.
     *
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Performance Monitoring Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/performance metrics " + ChatColor.WHITE + "- View performance metrics");
        sender.sendMessage(ChatColor.YELLOW + "/performance anomalies " + ChatColor.WHITE + "- View detected anomalies");
        sender.sendMessage(ChatColor.YELLOW + "/performance predictions " + ChatColor.WHITE + "- View performance predictions");
        sender.sendMessage(ChatColor.YELLOW + "/performance tests " + ChatColor.WHITE + "- Manage A/B tests");
        sender.sendMessage(ChatColor.YELLOW + "/performance analytics " + ChatColor.WHITE + "- View usage analytics");
    }
    
    /**
     * Handles the metrics command.
     *
     * @param context The command context
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleMetricsCommand(CommandContext context, String[] args) {
        CommandSender sender = context.getSender();
        Map<String, ProfilingSystem.Metric> metrics = profilingSystem.getMetrics();
        
        if (metrics.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No metrics available.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Performance Metrics ===");
        for (Map.Entry<String, ProfilingSystem.Metric> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            ProfilingSystem.Metric metric = entry.getValue();
            
            if (metric.getType() == ProfilingSystem.MetricType.TIMER) {
                sender.sendMessage(ChatColor.YELLOW + metricName + ": " + 
                                 ChatColor.WHITE + String.format("%.2f ms (avg), %.2f ms (p95)", 
                                 metric.getSampleAverage() / 1_000_000.0,
                                 metric.getSamplePercentile(95) / 1_000_000.0));
            } else if (metric.getType() == ProfilingSystem.MetricType.COUNTER) {
                sender.sendMessage(ChatColor.YELLOW + metricName + ": " + 
                                 ChatColor.WHITE + String.format("%d", (long)metric.getValue()));
            } else if (metric.getType() == ProfilingSystem.MetricType.GAUGE) {
                sender.sendMessage(ChatColor.YELLOW + metricName + ": " + 
                                 ChatColor.WHITE + String.format("%.2f", metric.getValue()));
            }
        }
        
        return true;
    }
    
    /**
     * Handles the anomalies command.
     *
     * @param context The command context
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleAnomaliesCommand(CommandContext context, String[] args) {
        CommandSender sender = context.getSender();
        
        // Get recent anomalies from the last hour
        List<AnomalyDetector.Anomaly> anomalies = profilingSystem.getAnomalyDetector().getRecentAnomalies(Duration.ofHours(1));
        
        if (anomalies.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No anomalies detected in the last hour.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Recent Anomalies ===");
        for (AnomalyDetector.Anomaly anomaly : anomalies) {
            // Get a color based on severity
            ChatColor severityColor;
            switch (anomaly.getSeverity()) {
                case LOW:
                    severityColor = ChatColor.GREEN;
                    break;
                case MEDIUM:
                    severityColor = ChatColor.YELLOW;
                    break;
                case HIGH:
                    severityColor = ChatColor.GOLD;
                    break;
                case CRITICAL:
                    severityColor = ChatColor.RED;
                    break;
                default:
                    severityColor = ChatColor.WHITE;
            }
            
            sender.sendMessage(severityColor + anomaly.getMetricName() + ": " + anomaly.getDescription());
        }
        
        return true;
    }
    
    /**
     * Handles the predictions command.
     *
     * @param context The command context
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handlePredictionsCommand(CommandContext context, String[] args) {
        CommandSender sender = context.getSender();
        
        // Get predictions for the next 24 hours
        List<PerformancePredictor.PredictedIssue> predictions = 
            profilingSystem.getPerformancePredictor().getRecentPredictedIssues(Duration.ofHours(24));
        
        if (predictions.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No performance issues predicted for the next 24 hours.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Performance Predictions ===");
        for (PerformancePredictor.PredictedIssue prediction : predictions) {
            // Get a color based on severity
            ChatColor severityColor;
            switch (prediction.getSeverity()) {
                case LOW:
                    severityColor = ChatColor.GREEN;
                    break;
                case MEDIUM:
                    severityColor = ChatColor.YELLOW;
                    break;
                case HIGH:
                    severityColor = ChatColor.GOLD;
                    break;
                case CRITICAL:
                    severityColor = ChatColor.RED;
                    break;
                default:
                    severityColor = ChatColor.WHITE;
            }
            
            sender.sendMessage(severityColor + prediction.getTitle() + ": " + prediction.getDescription());
        }
        
        return true;
    }
    
    /**
     * Handles the tests command.
     *
     * @param context The command context
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleTestsCommand(CommandContext context, String[] args) {
        CommandSender sender = context.getSender();
        
        // List active tests
        List<ABTestingFramework.ABTest> tests = profilingSystem.getAbTestingFramework().getActiveTests();
        
        sender.sendMessage(ChatColor.GOLD + "=== A/B Tests ===");
        if (tests.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No active A/B tests.");
        } else {
            for (ABTestingFramework.ABTest test : tests) {
                sender.sendMessage(ChatColor.YELLOW + test.getTestId() + ": " + 
                                 ChatColor.WHITE + test.getDescription());
            }
        }
        
        return true;
    }
    
    /**
     * Handles the analytics command.
     *
     * @param context The command context
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleAnalyticsCommand(CommandContext context, String[] args) {
        CommandSender sender = context.getSender();
        
        if (args.length == 0) {
            showGeneralAnalytics(sender);
        } else if (args.length == 1) {
            showModuleAnalytics(sender, args[0]);
        } else if (args.length == 2) {
            showFeatureAnalytics(sender, args[0], args[1]);
        } else if ("report".equals(args[0])) {
            showAnalyticsReport(sender);
        } else {
            showGeneralAnalytics(sender);
        }
        
        return true;
    }
    
    /**
     * Shows general usage analytics.
     *
     * @param sender The command sender
     */
    private void showGeneralAnalytics(CommandSender sender) {
        List<UsageAnalytics.FeatureUsage> topFeatures = profilingSystem.getUsageAnalytics().getTopFeatures(5);
        
        sender.sendMessage(ChatColor.GOLD + "=== Usage Analytics Overview ===");
        
        if (topFeatures.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No usage data available.");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Top 5 Features:");
        
        for (UsageAnalytics.FeatureUsage usage : topFeatures) {
            sender.sendMessage(ChatColor.GRAY + "  " + usage.getFullKey() + 
                              ChatColor.WHITE + ": " + usage.getCount() + " uses");
        }
    }
    
    /**
     * Shows usage analytics for a module.
     *
     * @param sender The command sender
     * @param moduleId The module ID
     */
    private void showModuleAnalytics(CommandSender sender, String moduleId) {
        Map<String, UsageAnalytics.FeatureUsage> moduleUsages = profilingSystem.getUsageAnalytics().getModuleFeatureUsages(moduleId);
        
        sender.sendMessage(ChatColor.GOLD + "=== Module Usage: " + moduleId + " ===");
        
        if (moduleUsages.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No usage data available for this module.");
            return;
        }
        
        long totalUsage = moduleUsages.values().stream().mapToLong(UsageAnalytics.FeatureUsage::getCount).sum();
        sender.sendMessage(ChatColor.YELLOW + "Total Usage: " + ChatColor.WHITE + totalUsage);
        
        List<UsageAnalytics.FeatureUsage> topFeatures = profilingSystem.getUsageAnalytics().getTopModuleFeatures(moduleId, 5);
        
        sender.sendMessage(ChatColor.YELLOW + "Top 5 Features:");
        
        for (UsageAnalytics.FeatureUsage usage : topFeatures) {
            double percentage = (double) usage.getCount() / totalUsage * 100;
            sender.sendMessage(ChatColor.GRAY + "  " + usage.getFeatureId() + 
                              ChatColor.WHITE + ": " + usage.getCount() + 
                              " uses (" + String.format("%.1f%%", percentage) + ")");
        }
    }
    
    /**
     * Shows usage analytics for a feature.
     *
     * @param sender The command sender
     * @param moduleId The module ID
     * @param featureId The feature ID
     */
    private void showFeatureAnalytics(CommandSender sender, String moduleId, String featureId) {
        UsageAnalytics.FeatureUsage usage = profilingSystem.getUsageAnalytics().getFeatureUsage(moduleId, featureId);
        
        sender.sendMessage(ChatColor.GOLD + "=== Feature Usage: " + moduleId + "." + featureId + " ===");
        
        if (usage == null) {
            sender.sendMessage(ChatColor.YELLOW + "No usage data available for this feature.");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Total Usage: " + ChatColor.WHITE + usage.getCount());
        sender.sendMessage(ChatColor.YELLOW + "First Used: " + ChatColor.WHITE + formatTimestamp(usage.getFirstUsed()));
        sender.sendMessage(ChatColor.YELLOW + "Last Used: " + ChatColor.WHITE + formatTimestamp(usage.getLastUsed()));
    }
    
    /**
     * Shows an analytics report.
     *
     * @param sender The command sender
     */
    private void showAnalyticsReport(CommandSender sender) {
        LocalDate today = LocalDate.now();
        LocalDate lastWeek = today.minusDays(7);
        
        UsageAnalytics.UsageReport report = profilingSystem.getUsageAnalytics().generateReport(lastWeek, today);
        
        sender.sendMessage(ChatColor.GOLD + "=== Usage Report: " + report.getPeriod() + " ===");
        
        // Show sample duration using formatDuration
        long sampleDuration = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
        sender.sendMessage(ChatColor.YELLOW + "Report Period: " + ChatColor.WHITE + formatDuration(sampleDuration));
        
        if (report.getTotalCount() == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No usage data available for this period.");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Total Usage: " + ChatColor.WHITE + report.getTotalCount());
        
        Map<String, Integer> topModules = report.getTopModules(3);
        
        if (!topModules.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Top Modules:");
            
            for (Map.Entry<String, Integer> entry : topModules.entrySet()) {
                double percentage = (double) entry.getValue() / report.getTotalCount() * 100;
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + 
                                  ChatColor.WHITE + ": " + entry.getValue() + 
                                  " uses (" + String.format("%.1f%%", percentage) + ")");
            }
        }
        
        Map<String, Integer> topFeatures = report.getTopFeatures(5);
        
        if (!topFeatures.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Top Features:");
            
            for (Map.Entry<String, Integer> entry : topFeatures.entrySet()) {
                double percentage = (double) entry.getValue() / report.getTotalCount() * 100;
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + 
                                  ChatColor.WHITE + ": " + entry.getValue() + 
                                  " uses (" + String.format("%.1f%%", percentage) + ")");
            }
        }
    }
    
    /**
     * Formats a duration for display.
     *
     * @param millis The duration in milliseconds
     * @return The formatted duration
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " days, " + (hours % 24) + " hours";
        } else if (hours > 0) {
            return hours + " hours, " + (minutes % 60) + " minutes";
        } else if (minutes > 0) {
            return minutes + " minutes, " + (seconds % 60) + " seconds";
        } else {
            return seconds + " seconds";
        }
    }
    
    /**
     * Formats a timestamp for display.
     *
     * @param timestamp The timestamp in milliseconds
     * @return The formatted timestamp
     */
    private String formatTimestamp(long timestamp) {
        // Formatter implementation
        java.util.Date date = new java.util.Date(timestamp);
        return date.toString();
    }
    
    /**
     * Shuts down the performance module.
     */
    public void shutdown() {
        // Unregister commands
        commandManager.unregisterModuleCommands("core");
        
        // Stop the profiling system
        profilingSystem.stop();
        
        LOGGER.info("Performance module shutdown");
    }
    
    /**
     * Gets the profiling system.
     *
     * @return The profiling system
     */
    public ProfilingSystem getProfilingSystem() {
        return profilingSystem;
    }
}
