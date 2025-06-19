package com.essentialscore.placeholder;

import com.essentialscore.ApiCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PlaceholderManager - Manages dynamic placeholders for server data
 * Supports multiple bracket types: {}, (), [], %%
 */
public class PlaceholderManager {
    
    private final ApiCore plugin;
    private final Map<String, PlaceholderProvider> providers;
    private final DecimalFormat decimalFormat;
    
    // Regex patterns for different bracket types
    private final Pattern curlyPattern = Pattern.compile("\\{([^}]+)\\}");
    private final Pattern roundPattern = Pattern.compile("\\(([^)]+)\\)");
    private final Pattern squarePattern = Pattern.compile("\\[([^\\]]+)\\]");
    private final Pattern percentPattern = Pattern.compile("%([^%]+)%");
    
    public PlaceholderManager(ApiCore plugin) {
        this.plugin = plugin;
        this.providers = new HashMap<>();
        
        // Setup decimal formatting
        int decimalPlaces = plugin.getConfig().getInt("placeholders.format.decimal-places", 2);
        StringBuilder pattern = new StringBuilder("#.");
        for (int i = 0; i < decimalPlaces; i++) {
            pattern.append("#");
        }
        this.decimalFormat = new DecimalFormat(pattern.toString());
        
        // Register default providers
        registerDefaultProviders();
    }
    
    /**
     * Check if placeholder processing is enabled
     */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("placeholders.enabled", true);
    }
    
    /**
     * Process message and replace all placeholders
     */
    public String processMessage(String message, Player player) {
        if (!plugin.getConfig().getBoolean("placeholders.enabled", true)) {
            return message;
        }
        
        String processed = message;
        
        // Process each bracket type if enabled
        if (plugin.getConfig().getBoolean("placeholders.brackets.curly", true)) {
            processed = processPattern(processed, curlyPattern, player);
        }
        if (plugin.getConfig().getBoolean("placeholders.brackets.round", true)) {
            processed = processPattern(processed, roundPattern, player);
        }
        if (plugin.getConfig().getBoolean("placeholders.brackets.square", true)) {
            processed = processPattern(processed, squarePattern, player);
        }
        if (plugin.getConfig().getBoolean("placeholders.brackets.percent", true)) {
            processed = processPattern(processed, percentPattern, player);
        }
        
        return processed;
    }
    
    /**
     * Process a specific pattern in the message
     */
    private String processPattern(String message, Pattern pattern, Player player) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String replacement = getPlaceholderValue(placeholder, player);
            
            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // Keep original if no replacement found
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Get the value for a specific placeholder
     */
    private String getPlaceholderValue(String placeholder, Player player) {
        // Check if the placeholder category is enabled
        if (placeholder.startsWith("tps") && !plugin.getConfig().getBoolean("placeholders.data.performance", true)) {
            return null;
        }
        if (placeholder.startsWith("memory") && !plugin.getConfig().getBoolean("placeholders.data.memory", true)) {
            return null;
        }
        if (placeholder.startsWith("player") && !plugin.getConfig().getBoolean("placeholders.data.players", true)) {
            return null;
        }
        
        // Get provider and value
        for (Map.Entry<String, PlaceholderProvider> entry : providers.entrySet()) {
            if (placeholder.startsWith(entry.getKey())) {
                return entry.getValue().getValue(placeholder, player);
            }
        }
        
        return null;
    }
    
    /**
     * Register default placeholder providers
     */
    private void registerDefaultProviders() {
        // Performance placeholders
        registerProvider("tps", new PerformancePlaceholderProvider());
        registerProvider("memory", new MemoryPlaceholderProvider());
        registerProvider("cpu", new PerformancePlaceholderProvider());
        
        // Player placeholders
        registerProvider("players", new PlayerPlaceholderProvider());
        registerProvider("player", new PlayerPlaceholderProvider());
        
        // World placeholders
        registerProvider("world", new WorldPlaceholderProvider());
        
        // Time placeholders
        registerProvider("time", new TimePlaceholderProvider());
        registerProvider("date", new TimePlaceholderProvider());
        
        // Plugin placeholders
        registerProvider("plugin", new PluginPlaceholderProvider());
        registerProvider("essentialscore", new PluginPlaceholderProvider());
    }
    
    /**
     * Register a custom placeholder provider
     */
    public void registerProvider(String prefix, PlaceholderProvider provider) {
        providers.put(prefix.toLowerCase(), provider);
    }
    
    /**
     * Format a number with color coding based on thresholds
     */
    public String formatWithColor(double value, String type) {
        if (!plugin.getConfig().getBoolean("placeholders.format.color-coded", true)) {
            return decimalFormat.format(value);
        }
        
        String color = "&f"; // Default white
        
        if (type.equals("tps")) {
            if (value >= 18.0) {
                color = plugin.getConfig().getString("placeholders.format.tps-colors.good", "&a");
            } else if (value >= 15.0) {
                color = plugin.getConfig().getString("placeholders.format.tps-colors.warning", "&e");
            } else {
                color = plugin.getConfig().getString("placeholders.format.tps-colors.critical", "&c");
            }
        } else if (type.equals("memory")) {
            if (value < 70.0) {
                color = plugin.getConfig().getString("placeholders.format.memory-colors.good", "&a");
            } else if (value < 90.0) {
                color = plugin.getConfig().getString("placeholders.format.memory-colors.warning", "&e");
            } else {
                color = plugin.getConfig().getString("placeholders.format.memory-colors.critical", "&c");
            }
        }
        
        return color + decimalFormat.format(value);
    }
    
    /**
     * Performance placeholder provider
     */
    private class PerformancePlaceholderProvider implements PlaceholderProvider {
        @Override
        public String getValue(String placeholder, Player player) {
            switch (placeholder.toLowerCase()) {
                case "tps":
                    double tps = getTPS();
                    return formatWithColor(tps, "tps");
                case "tps_raw":
                    return decimalFormat.format(getTPS());
                case "cpu":
                    return decimalFormat.format(getCPUUsage()) + "%";
                default:
                    return null;
            }
        }
        
        private double getTPS() {
            try {
                return Bukkit.getServer().getTPS()[0];
            } catch (Exception e) {
                return 20.0; // Fallback
            }
        }
        
        private double getCPUUsage() {
            try {
                com.sun.management.OperatingSystemMXBean osBean = 
                    (com.sun.management.OperatingSystemMXBean) 
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                return osBean.getProcessCpuLoad() * 100.0;
            } catch (Exception e) {
                return 0.0;
            }
        }
    }
    
    /**
     * Memory placeholder provider
     */
    private class MemoryPlaceholderProvider implements PlaceholderProvider {
        @Override
        public String getValue(String placeholder, Player player) {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            switch (placeholder.toLowerCase()) {
                case "memory":
                case "memory_used":
                    return formatBytes(usedMemory);
                case "memory_max":
                    return formatBytes(maxMemory);
                case "memory_free":
                    return formatBytes(freeMemory);
                case "memory_total":
                    return formatBytes(totalMemory);
                case "memory_percent":
                case "memory_percentage":
                    double percentage = (double) usedMemory / maxMemory * 100.0;
                    return formatWithColor(percentage, "memory") + "%";
                case "memory_percent_raw":
                    return decimalFormat.format((double) usedMemory / maxMemory * 100.0);
                default:
                    return null;
            }
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "";
            return decimalFormat.format(bytes / Math.pow(1024, exp)) + " " + pre + "B";
        }
    }
    
    /**
     * Player placeholder provider
     */
    private class PlayerPlaceholderProvider implements PlaceholderProvider {
        @Override
        public String getValue(String placeholder, Player player) {
            switch (placeholder.toLowerCase()) {
                case "players":
                case "players_online":
                    return String.valueOf(Bukkit.getOnlinePlayers().size());
                case "players_max":
                    return String.valueOf(Bukkit.getMaxPlayers());                case "player_name":
                    return player != null ? player.getName() : "Console";
                case "player_display_name":
                    return player != null ? player.getName() : "Console";
                case "player_world":
                    return player != null ? player.getWorld().getName() : "N/A";
                case "player_health":
                    return player != null ? decimalFormat.format(player.getHealth()) : "N/A";
                case "player_food":
                    return player != null ? String.valueOf(player.getFoodLevel()) : "N/A";
                case "player_level":
                    return player != null ? String.valueOf(player.getLevel()) : "N/A";
                case "player_exp":
                    return player != null ? decimalFormat.format(player.getExp() * 100) + "%" : "N/A";
                default:
                    return null;
            }
        }
    }
    
    /**
     * World placeholder provider
     */
    private class WorldPlaceholderProvider implements PlaceholderProvider {
        @Override
        public String getValue(String placeholder, Player player) {
            if (player == null) return "N/A";
            
            switch (placeholder.toLowerCase()) {
                case "world":
                case "world_name":
                    return player.getWorld().getName();
                case "world_time":
                    return String.valueOf(player.getWorld().getTime());
                case "world_players":
                    return String.valueOf(player.getWorld().getPlayers().size());
                case "world_seed":
                    return String.valueOf(player.getWorld().getSeed());
                default:
                    return null;
            }
        }
    }
    
    /**
     * Time placeholder provider
     */
    private class TimePlaceholderProvider implements PlaceholderProvider {
        @Override
        public String getValue(String placeholder, Player player) {
            java.util.Date now = new java.util.Date();
            java.text.SimpleDateFormat format;
            
            switch (placeholder.toLowerCase()) {
                case "time":
                    format = new java.text.SimpleDateFormat("HH:mm:ss");
                    return format.format(now);
                case "date":
                    format = new java.text.SimpleDateFormat("dd.MM.yyyy");
                    return format.format(now);
                case "datetime":
                    format = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    return format.format(now);
                case "timestamp":
                    return String.valueOf(System.currentTimeMillis());
                default:
                    return null;
            }
        }
    }
    
    /**
     * Plugin placeholder provider
     */
    private class PluginPlaceholderProvider implements PlaceholderProvider {
        @Override
        public String getValue(String placeholder, Player player) {
            switch (placeholder.toLowerCase()) {
                case "plugin_version":                case "essentialscore_version":
                    return plugin.getPluginVersion();
                case "plugin_name":
                case "essentialscore_name":
                    return "EssentialsCore";
                case "modules_loaded":
                    return String.valueOf(plugin.getModuleManager().getLoadedModules().size());
                case "modules_total":
                    return String.valueOf(plugin.getModuleManager().getLoadedModules().size());
                default:
                    return null;
            }
        }
    }
}
