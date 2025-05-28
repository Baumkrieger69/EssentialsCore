package com.essentialscore.api.web.ui;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Manages the dashboard UI and provides real-time server statistics.
 */
public class DashboardManager {
    private static final Logger LOGGER = Logger.getLogger(DashboardManager.class.getName());
    
    private final Plugin plugin;
    private final Map<String, Consumer<Map<String, Object>>> subscribers;
    private BukkitTask updateTask;
    
    // Performance metrics
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final RuntimeMXBean runtimeBean;
    private long lastUpdateTime;
    private double tps;
    private double cpuUsage;
    
    /**
     * Creates a new dashboard manager
     * 
     * @param plugin The plugin instance
     * @param config The configuration
     */
    public DashboardManager(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.subscribers = new ConcurrentHashMap<>();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.lastUpdateTime = System.currentTimeMillis();
        this.tps = 20.0; // Default TPS
        this.cpuUsage = 0.0;
        
        // Start update task
        startUpdateTask(config.getInt("ui.dashboard.refresh-interval-seconds", 5));
    }
    
    /**
     * Starts the update task to periodically calculate and broadcast server statistics
     * 
     * @param refreshIntervalSeconds How often to refresh the dashboard
     */
    private void startUpdateTask(int refreshIntervalSeconds) {
        // Update regularly
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Update server stats
            updateServerStats();
            
            // Notify subscribers
            notifySubscribers();
        }, 20, 20 * refreshIntervalSeconds);
    }
    
    /**
     * Updates server statistics
     */
    private void updateServerStats() {
        // Calculate TPS
        long now = System.currentTimeMillis();
        long timeDiff = now - lastUpdateTime;
        
        if (timeDiff > 0) {
            // This is a simple approximation - in a real implementation, 
            // you would use more sophisticated TPS calculation
            tps = Math.min(20.0, 20.0 * (1000.0 / timeDiff));
        }
        
        lastUpdateTime = now;
        
        // Estimate CPU usage
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            cpuUsage = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100.0;
        }
    }
    
    /**
     * Notifies all subscribers with updated dashboard data
     */
    private void notifySubscribers() {
        Map<String, Object> data = getDashboardData();
        
        for (Map.Entry<String, Consumer<Map<String, Object>>> entry : subscribers.entrySet()) {
            Consumer<Map<String, Object>> callback = entry.getValue();
            callback.accept(data);
        }
    }
    
    /**
     * Gets current dashboard data
     * 
     * @return The dashboard data
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        
        // Server info
        data.put("serverName", Bukkit.getServer().getName());
        data.put("serverVersion", Bukkit.getServer().getVersion());
        data.put("serverMotd", Bukkit.getServer().getMotd());
        data.put("onlineMode", Bukkit.getServer().getOnlineMode());
        data.put("maxPlayers", Bukkit.getServer().getMaxPlayers());
        
        // Performance metrics
        data.put("tps", Math.round(tps * 100.0) / 100.0); // Round to 2 decimal places
        data.put("cpuUsage", Math.round(cpuUsage * 100.0) / 100.0); // Round to 2 decimal places
        data.put("memoryUsed", memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024)); // MB
        data.put("memoryMax", memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024)); // MB
        data.put("memoryAllocated", memoryBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024)); // MB
        data.put("uptime", runtimeBean.getUptime() / 1000); // Seconds
        
        // Player info
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        data.put("onlinePlayers", onlinePlayers.size());
        
        List<Map<String, Object>> playerList = new ArrayList<>();
        for (Player player : onlinePlayers) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("name", player.getName());
            playerData.put("uuid", player.getUniqueId().toString());
            playerData.put("op", player.isOp());
            playerData.put("world", player.getWorld().getName());
            playerData.put("ping", player.getPing());
            
            playerList.add(playerData);
        }
        data.put("players", playerList);
        
        // World info
        List<Map<String, Object>> worldList = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            Map<String, Object> worldData = new HashMap<>();
            worldData.put("name", world.getName());
            worldData.put("environment", world.getEnvironment().name());
            worldData.put("loadedChunks", world.getLoadedChunks().length);
            worldData.put("entityCount", world.getEntities().size());
            worldData.put("time", world.getTime());
            worldData.put("storming", world.hasStorm());
            worldData.put("thundering", world.isThundering());
            
            worldList.add(worldData);
        }
        data.put("worlds", worldList);
        
        // Plugin info
        List<Map<String, Object>> pluginList = new ArrayList<>();
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            Map<String, Object> pluginData = new HashMap<>();
            pluginData.put("name", p.getName());
            pluginData.put("version", p.getDescription().getVersion());
            pluginData.put("enabled", p.isEnabled());
            pluginData.put("authors", p.getDescription().getAuthors());
            
            pluginList.add(pluginData);
        }
        data.put("plugins", pluginList);
        
        return data;
    }
    
    /**
     * Subscribes a client to dashboard updates
     * 
     * @param clientId The client ID
     * @param callback The callback to invoke with updated data
     */
    public void subscribe(String clientId, Consumer<Map<String, Object>> callback) {
        subscribers.put(clientId, callback);
    }
    
    /**
     * Unsubscribes a client from dashboard updates
     * 
     * @param clientId The client ID
     */
    public void unsubscribe(String clientId) {
        subscribers.remove(clientId);
    }
    
    /**
     * Cleans up resources
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        
        subscribers.clear();
    }
} 