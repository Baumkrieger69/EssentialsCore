package com.essentialscore.api.web.rest.endpoints;

import com.essentialscore.api.web.rest.ApiEndpoint;
import com.essentialscore.api.web.rest.ApiRequest;
import com.essentialscore.api.web.rest.ApiResponse;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API endpoint for server information.
 */
public class ServerInfoEndpoint extends ApiEndpoint {
    
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final RuntimeMXBean runtimeBean;
    
    /**
     * Creates a new server info endpoint
     * 
     * @param plugin The plugin instance
     */
    public ServerInfoEndpoint(Plugin plugin) {
        super(plugin);
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
    }
    
    @Override
    public String getPath() {
        return "server";
    }
    
    @Override
    public ApiResponse handleRequest(ApiRequest request) {
        try {
            // Get subpath
            String subpath = request.getPathSegment(1);
            
            if (subpath == null) {
                // Return basic server info
                return success(getBasicServerInfo());
            } else if ("status".equalsIgnoreCase(subpath)) {
                // Return detailed server status
                return success(getServerStatus());
            } else if ("worlds".equalsIgnoreCase(subpath)) {
                // Return world info
                return success(getWorldInfo());
            } else if ("players".equalsIgnoreCase(subpath)) {
                // Return player info
                return success(getPlayerInfo());
            } else if ("plugins".equalsIgnoreCase(subpath)) {
                // Return plugin info
                return success(getPluginInfo());
            } else if ("memory".equalsIgnoreCase(subpath)) {
                // Return memory info
                return success(getMemoryInfo());
            } else if ("system".equalsIgnoreCase(subpath)) {
                // Return system info
                return success(getSystemInfo());
            } else {
                // Unknown subpath
                return notFound("Unknown endpoint: " + subpath);
            }
        } catch (Exception e) {
            return error(e);
        }
    }
    
    /**
     * Gets basic server information
     * 
     * @return The server information
     */
    private Map<String, Object> getBasicServerInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Server info
        info.put("name", Bukkit.getServer().getName());
        info.put("version", Bukkit.getServer().getVersion());
        info.put("bukkitVersion", Bukkit.getServer().getBukkitVersion());
        info.put("motd", Bukkit.getServer().getMotd());
        info.put("onlineMode", Bukkit.getServer().getOnlineMode());
        info.put("maxPlayers", Bukkit.getServer().getMaxPlayers());
        info.put("onlinePlayers", Bukkit.getServer().getOnlinePlayers().size());
        info.put("viewDistance", Bukkit.getServer().getViewDistance());
        info.put("ip", Bukkit.getServer().getIp());
        info.put("port", Bukkit.getServer().getPort());
        info.put("worldType", Bukkit.getServer().getWorldType());
        
        return info;
    }
    
    /**
     * Gets detailed server status
     * 
     * @return The server status
     */
    private Map<String, Object> getServerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Basic info
        status.putAll(getBasicServerInfo());
        
        // Add memory info
        status.putAll(getMemoryInfo());
        
        // Add system info
        status.putAll(getSystemInfo());
        
        // Add counts
        status.put("worldCount", Bukkit.getServer().getWorlds().size());
        status.put("pluginCount", Bukkit.getServer().getPluginManager().getPlugins().length);
        
        return status;
    }
    
    /**
     * Gets world information
     * 
     * @return The world information
     */
    private Map<String, Object> getWorldInfo() {
        Map<String, Object> info = new HashMap<>();
        List<Map<String, Object>> worlds = new ArrayList<>();
        
        for (World world : Bukkit.getServer().getWorlds()) {
            Map<String, Object> worldInfo = new HashMap<>();
            worldInfo.put("name", world.getName());
            worldInfo.put("uuid", world.getUID().toString());
            worldInfo.put("environment", world.getEnvironment().name());
            worldInfo.put("seed", world.getSeed());
            worldInfo.put("time", world.getTime());
            worldInfo.put("fullTime", world.getFullTime());
            worldInfo.put("difficulty", world.getDifficulty().name());
            worldInfo.put("loadedChunks", world.getLoadedChunks().length);
            worldInfo.put("entityCount", world.getEntities().size());
            worldInfo.put("livingEntityCount", world.getLivingEntities().size());
            worldInfo.put("playerCount", world.getPlayers().size());
            worldInfo.put("keepSpawnInMemory", world.getKeepSpawnInMemory());
            worldInfo.put("pvp", world.getPVP());
            worldInfo.put("allowAnimals", world.getAllowAnimals());
            worldInfo.put("allowMonsters", world.getAllowMonsters());
            worldInfo.put("spawnLocation", formatLocation(world.getSpawnLocation()));
            worldInfo.put("hasStorm", world.hasStorm());
            worldInfo.put("isThundering", world.isThundering());
            worldInfo.put("gameRules", world.getGameRules());
            
            worlds.add(worldInfo);
        }
        
        info.put("worlds", worlds);
        info.put("count", worlds.size());
        
        return info;
    }
    
    /**
     * Gets player information
     * 
     * @return The player information
     */
    private Map<String, Object> getPlayerInfo() {
        Map<String, Object> info = new HashMap<>();
        List<Map<String, Object>> players = new ArrayList<>();
        
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("name", player.getName());
            playerInfo.put("uuid", player.getUniqueId().toString());
            playerInfo.put("displayName", player.getDisplayName());
            playerInfo.put("address", player.getAddress().toString());
            playerInfo.put("world", player.getWorld().getName());
            playerInfo.put("location", formatLocation(player.getLocation()));
            playerInfo.put("gameMode", player.getGameMode().name());
            playerInfo.put("health", player.getHealth());
            playerInfo.put("maxHealth", player.getMaxHealth());
            playerInfo.put("foodLevel", player.getFoodLevel());
            playerInfo.put("exhaustion", player.getExhaustion());
            playerInfo.put("saturation", player.getSaturation());
            playerInfo.put("level", player.getLevel());
            playerInfo.put("exp", player.getExp());
            playerInfo.put("flying", player.isFlying());
            playerInfo.put("op", player.isOp());
            playerInfo.put("whitelisted", player.isWhitelisted());
            playerInfo.put("banned", player.isBanned());
            playerInfo.put("ping", player.getPing());
            
            players.add(playerInfo);
        }
        
        info.put("players", players);
        info.put("count", players.size());
        info.put("maxPlayers", Bukkit.getServer().getMaxPlayers());
        
        return info;
    }
    
    /**
     * Gets plugin information
     * 
     * @return The plugin information
     */
    private Map<String, Object> getPluginInfo() {
        Map<String, Object> info = new HashMap<>();
        List<Map<String, Object>> plugins = new ArrayList<>();
        
        for (Plugin plugin : Bukkit.getServer().getPluginManager().getPlugins()) {
            Map<String, Object> pluginInfo = new HashMap<>();
            pluginInfo.put("name", plugin.getName());
            pluginInfo.put("version", plugin.getDescription().getVersion());
            pluginInfo.put("description", plugin.getDescription().getDescription());
            pluginInfo.put("authors", plugin.getDescription().getAuthors());
            pluginInfo.put("website", plugin.getDescription().getWebsite());
            pluginInfo.put("enabled", plugin.isEnabled());
            pluginInfo.put("main", plugin.getDescription().getMain());
            pluginInfo.put("commands", plugin.getDescription().getCommands() != null 
                    ? plugin.getDescription().getCommands().keySet() : new ArrayList<>());
            pluginInfo.put("dependencies", plugin.getDescription().getDepend());
            pluginInfo.put("softDependencies", plugin.getDescription().getSoftDepend());
            
            plugins.add(pluginInfo);
        }
        
        info.put("plugins", plugins);
        info.put("count", plugins.size());
        
        return info;
    }
    
    /**
     * Gets memory information
     * 
     * @return The memory information
     */
    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Memory info
        info.put("memoryMax", memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024)); // MB
        info.put("memoryUsed", memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024)); // MB
        info.put("memoryAllocated", memoryBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024)); // MB
        info.put("memoryFree", (memoryBean.getHeapMemoryUsage().getMax() - memoryBean.getHeapMemoryUsage().getUsed()) / (1024 * 1024)); // MB
        
        // Usage percentage
        double usagePercentage = (double) memoryBean.getHeapMemoryUsage().getUsed() / memoryBean.getHeapMemoryUsage().getMax() * 100.0;
        info.put("memoryUsagePercentage", Math.round(usagePercentage * 100.0) / 100.0); // Round to 2 decimal places
        
        return info;
    }
    
    /**
     * Gets system information
     * 
     * @return The system information
     */
    private Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // System info
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("osArch", System.getProperty("os.arch"));
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("availableProcessors", osBean.getAvailableProcessors());
        
        // Runtime info
        info.put("uptime", runtimeBean.getUptime() / 1000); // Seconds
        info.put("startTime", runtimeBean.getStartTime()); // Milliseconds since epoch
        
        // CPU usage (if available)
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            double cpuLoad = sunOsBean.getProcessCpuLoad() * 100.0;
            info.put("cpuUsage", Math.round(cpuLoad * 100.0) / 100.0); // Round to 2 decimal places
            info.put("systemCpuLoad", Math.round(sunOsBean.getSystemCpuLoad() * 100.0) / 100.0); // Round to 2 decimal places
        }
        
        return info;
    }
    
    /**
     * Formats a location as a map
     * 
     * @param location The location
     * @return The formatted location
     */
    private Map<String, Object> formatLocation(org.bukkit.Location location) {
        Map<String, Object> locMap = new HashMap<>();
        locMap.put("world", location.getWorld().getName());
        locMap.put("x", location.getX());
        locMap.put("y", location.getY());
        locMap.put("z", location.getZ());
        locMap.put("yaw", location.getYaw());
        locMap.put("pitch", location.getPitch());
        return locMap;
    }
} 