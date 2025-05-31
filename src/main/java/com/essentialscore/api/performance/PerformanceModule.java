package com.essentialscore.api.performance;

import com.essentialscore.api.command.CommandContext;
import com.essentialscore.api.command.CommandManager;
import com.essentialscore.api.command.CommandProcessor;
import com.essentialscore.api.command.SimpleCommand;
import com.essentialscore.api.module.ModuleRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
        
        LOGGER.info("Performance module initialized");
    }
    
    /**
     * Registers commands for the performance module.
     */
    private void registerCommands() {
        // Main performance command
        SimpleCommand performanceCommand = SimpleCommand.builder("performance", "core")
            .description("Performance monitoring and analytics commands")
            .permission("essentials.performance")
            .build(context -> {
                showHelp(context.getSender());
                return true;
            });
        
        // Metrics command
        SimpleCommand metricsCommand = SimpleCommand.builder("metrics", "core")
            .description("View performance metrics")
            .permission("essentials.performance.metrics")
            .parent(performanceCommand)
            .build(this::handleMetricsCommand);
        
        // Anomalies command
        SimpleCommand anomaliesCommand = SimpleCommand.builder("anomalies", "core")
            .description("View detected anomalies")
            .permission("essentials.performance.anomalies")
            .parent(performanceCommand)
            .build(this::handleAnomaliesCommand);
        
        // Predictions command
        SimpleCommand predictionsCommand = SimpleCommand.builder("predictions", "core")
            .description("View performance predictions")
            .permission("essentials.performance.predictions")
            .parent(performanceCommand)
            .build(this::handlePredictionsCommand);
        
        // Tests command
        SimpleCommand testsCommand = SimpleCommand.builder("tests", "core")
            .description("Manage A/B tests")
            .permission("essentials.performance.tests")
            .parent(performanceCommand)
            .build(this::handleTestsCommand);
        
        // Analytics command
        SimpleCommand analyticsCommand = SimpleCommand.builder("analytics", "core")
            .description("View usage analytics")
            .permission("essentials.performance.analytics")
            .parent(performanceCommand)
            .build(this::handleAnalyticsCommand);
        
        // Add sub-commands to the main command
        performanceCommand.addSubCommand(metricsCommand);
        performanceCommand.addSubCommand(anomaliesCommand);
        performanceCommand.addSubCommand(predictionsCommand);
        performanceCommand.addSubCommand(testsCommand);
        performanceCommand.addSubCommand(analyticsCommand);
        
        // Register the main command
        commandManager.registerCommand(performanceCommand);
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
     * @return true if the command was handled
     */
    private boolean handleMetricsCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        
        // Get top metrics
        List<ProfilingSystem.Metric> topMetrics = getTopMetrics(10);
        
        sender.sendMessage(ChatColor.GOLD + "=== Top Performance Metrics ===");
        
        if (topMetrics.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No metrics available.");
            return true;
        }
        
        for (ProfilingSystem.Metric metric : topMetrics) {
            String value;
            switch (metric.getType()) {
                case COUNTER:
                    value = String.format("%.0f", metric.getValue());
                    break;
                case GAUGE:
                    value = String.format("%.2f", metric.getValue());
                    break;
                case TIMER:
                    value = String.format("%.2f ms", metric.getSampleAverage());
                    break;
                default:
                    value = String.valueOf(metric.getValue());
                    break;
            }
            
            sender.sendMessage(ChatColor.YELLOW + metric.getName() + ": " + ChatColor.WHITE + value);
        }
        
        return true;
    }
    
    /**
     * Gets the top metrics by value.
     *
     * @param limit The maximum number of metrics to return
     * @return The top metrics
     */
    private List<ProfilingSystem.Metric> getTopMetrics(int limit) {
        // In a real implementation, you would sort metrics by value
        // Return all metrics up to the specified limit
        return Arrays.asList(profilingSystem.getMetrics().values().toArray(new ProfilingSystem.Metric[0])).subList(
            0,
            Math.min(limit, profilingSystem.getMetrics().size())
        );
    }
    
    /**
     * Handles the anomalies command.
     *
     * @param context The command context
     * @return true if the command was handled
     */
    private boolean handleAnomaliesCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        
        // Get recent anomalies
        List<AnomalyDetector.Anomaly> anomalies = profilingSystem.getAnomalyDetector().getRecentAnomalies(Duration.ofHours(1));
        
        sender.sendMessage(ChatColor.GOLD + "=== Recent Anomalies (Last Hour) ===");
        
        if (anomalies.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No anomalies detected in the last hour.");
            return true;
        }
        
        for (AnomalyDetector.Anomaly anomaly : anomalies) {
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
                    break;
            }
            
            sender.sendMessage(severityColor + "[" + anomaly.getSeverity() + "] " + 
                              ChatColor.YELLOW + anomaly.getMetricName() + ": " + 
                              ChatColor.WHITE + anomaly.getDescription());
        }
        
        return true;
    }
    
    /**
     * Handles the predictions command.
     *
     * @param context The command context
     * @return true if the command was handled
     */
    private boolean handlePredictionsCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        
        // Get recent predictions
        List<PerformancePredictor.PredictedIssue> issues = profilingSystem.getPerformancePredictor().getRecentPredictedIssues(Duration.ofHours(24));
        
        sender.sendMessage(ChatColor.GOLD + "=== Performance Predictions (Next 24 Hours) ===");
        
        if (issues.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No performance issues predicted for the next 24 hours.");
            return true;
        }
        
        for (PerformancePredictor.PredictedIssue issue : issues) {
            ChatColor severityColor;
            switch (issue.getSeverity()) {
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
                    break;
            }
            
            Duration timeUntil = issue.getTimeUntilOccurrence();
            String timeUntilStr = String.format("%d hours %d minutes", 
                                              timeUntil.toHours(), 
                                              timeUntil.toMinutesPart());
            
            sender.sendMessage(severityColor + "[" + issue.getSeverity() + "] " + 
                              ChatColor.YELLOW + issue.getTitle() + " " + 
                              ChatColor.WHITE + "(" + timeUntilStr + ")");
            sender.sendMessage(ChatColor.GRAY + "  " + issue.getDescription());
        }
        
        return true;
    }
    
    /**
     * Handles the tests command.
     *
     * @param context The command context
     * @return true if the command was handled
     */
    private boolean handleTestsCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        List<String> args = Arrays.asList(context.getArgs());
        
        if (args.isEmpty()) {
            // List active tests
            listActiveTests(sender);
            return true;
        }
        
        String subCommand = args.get(0).toLowerCase();
        
        switch (subCommand) {
            case "list":
                listActiveTests(sender);
                break;
            case "info":
                if (args.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /performance tests info <test-id>");
                    return true;
                }
                showTestInfo(sender, args.get(1));
                break;
            case "end":
                if (args.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /performance tests end <test-id>");
                    return true;
                }
                endTest(sender, args.get(1));
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown sub-command: " + subCommand);
                sender.sendMessage(ChatColor.YELLOW + "Available sub-commands: list, info, end");
                break;
        }
        
        return true;
    }
    
    /**
     * Lists active A/B tests.
     *
     * @param sender The command sender
     */
    private void listActiveTests(CommandSender sender) {
        List<ABTestingFramework.ABTest> tests = profilingSystem.getAbTestingFramework().getActiveTests();
        
        sender.sendMessage(ChatColor.GOLD + "=== Active A/B Tests ===");
        
        if (tests.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No active A/B tests.");
            return;
        }
        
        for (ABTestingFramework.ABTest test : tests) {
            sender.sendMessage(ChatColor.YELLOW + test.getTestId() + 
                              ChatColor.GRAY + " (" + test.getModuleId() + "): " + 
                              ChatColor.WHITE + test.getDescription());
        }
    }
    
    /**
     * Shows information about an A/B test.
     *
     * @param sender The command sender
     * @param testId The test ID
     */
    private void showTestInfo(CommandSender sender, String testId) {
        ABTestingFramework.ABTest test = profilingSystem.getAbTestingFramework().getTest(testId);
        
        if (test == null) {
            sender.sendMessage(ChatColor.RED + "Test not found: " + testId);
            return;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== A/B Test: " + test.getTestId() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Module: " + ChatColor.WHITE + test.getModuleId());
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + test.getDescription());
        sender.sendMessage(ChatColor.YELLOW + "Duration: " + ChatColor.WHITE + formatDuration(test.getDuration()));
        
        sender.sendMessage(ChatColor.YELLOW + "Variants:");
        for (String variant : test.getVariants().keySet()) {
            int weight = test.getVariants().get(variant);
            int playerCount = test.getVariantPlayers(variant).size();
            
            sender.sendMessage(ChatColor.GRAY + "  " + variant + 
                              ChatColor.WHITE + " (Weight: " + weight + 
                              ", Players: " + playerCount + ")");
        }
    }
    
    /**
     * Ends an A/B test.
     *
     * @param sender The command sender
     * @param testId The test ID
     */
    private void endTest(CommandSender sender, String testId) {
        ABTestingFramework.ABTest test = profilingSystem.getAbTestingFramework().getTest(testId);
        
        if (test == null) {
            sender.sendMessage(ChatColor.RED + "Test not found: " + testId);
            return;
        }
        
        ABTestingFramework.ABTestResult result = profilingSystem.getAbTestingFramework().endTest(testId);
        
        sender.sendMessage(ChatColor.GOLD + "=== A/B Test Ended: " + test.getTestId() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Winner: " + ChatColor.GREEN + result.getWinningVariant());
        
        if (result.isStatisticallySignificant()) {
            sender.sendMessage(ChatColor.YELLOW + "Improvement: " + ChatColor.GREEN + 
                              String.format("%.2f%%", result.getImprovementPercentage()) + 
                              ChatColor.YELLOW + " (Statistically significant)");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Improvement: " + ChatColor.RED + 
                              String.format("%.2f%%", result.getImprovementPercentage()) + 
                              ChatColor.YELLOW + " (Not statistically significant)");
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Variant Results:");
        for (String variant : result.getVariantMetrics().keySet()) {
            ABTestingFramework.VariantMetrics metrics = result.getMetricsForVariant(variant);
            
            sender.sendMessage(ChatColor.GRAY + "  " + variant + 
                              ChatColor.WHITE + " (Impressions: " + metrics.getImpressions() + 
                              ", Conversions: " + metrics.getConversions() + 
                              ", Rate: " + String.format("%.2f%%", metrics.getConversionRate() * 100) + ")");
        }
    }
    
    /**
     * Handles the analytics command.
     *
     * @param context The command context
     * @return true if the command was handled
     */
    private boolean handleAnalyticsCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        List<String> args = Arrays.asList(context.getArgs());
        
        if (args.isEmpty()) {
            // Show general analytics
            showGeneralAnalytics(sender);
            return true;
        }
        
        String subCommand = args.get(0).toLowerCase();
        
        switch (subCommand) {
            case "general":
                showGeneralAnalytics(sender);
                break;
            case "module":
                if (args.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /performance analytics module <module-id>");
                    return true;
                }
                showModuleAnalytics(sender, args.get(1));
                break;
            case "feature":
                if (args.size() < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /performance analytics feature <module-id> <feature-id>");
                    return true;
                }
                showFeatureAnalytics(sender, args.get(1), args.get(2));
                break;
            case "report":
                showAnalyticsReport(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown sub-command: " + subCommand);
                sender.sendMessage(ChatColor.YELLOW + "Available sub-commands: general, module, feature, report");
                break;
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