package com.essentialscore.api.web.rest.endpoints;

import com.essentialscore.PerformanceMonitor;
import com.essentialscore.api.web.rest.ApiEndpoint;
import com.essentialscore.api.web.rest.ApiRequest;
import com.essentialscore.api.web.rest.ApiResponse;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * API endpoint for performance monitoring and system information.
 */
public class PerformanceEndpoint extends ApiEndpoint {
    
    private final PerformanceMonitor performanceMonitor;
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final RuntimeMXBean runtimeBean;
    private final ThreadMXBean threadBean;
    private final GarbageCollectorMXBean[] gcBeans;
    private final Queue<Map<String, Object>> performanceHistory;
    private final int MAX_HISTORY_SIZE = 100;
    
    /**
     * Creates a new performance endpoint
     * 
     * @param plugin The plugin instance
     * @param performanceMonitor The performance monitor
     */
    public PerformanceEndpoint(Plugin plugin, PerformanceMonitor performanceMonitor) {
        super(plugin);
        this.performanceMonitor = performanceMonitor;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans().toArray(new GarbageCollectorMXBean[0]);
        this.performanceHistory = new ConcurrentLinkedQueue<>();
    }
    
    @Override
    public String getPath() {
        return "performance";
    }
    
    @Override
    public boolean requiresAuthentication() {
        return true;
    }
    
    @Override
    public String getRequiredPermission() {
        return "essentials.webui.performance";
    }
    
    @Override
    public ApiResponse handleRequest(ApiRequest request) {
        try {
            String method = request.getMethod();
            String subpath = request.getPathSegment(1);
            
            if (subpath == null) {
                // Base performance endpoint
                switch (method) {
                    case "GET":
                        return getCurrentPerformance();
                    default:
                        return ApiResponse.methodNotAllowed("Method not allowed: " + method);
                }
            }
            
            switch (subpath) {
                case "current":
                    return getCurrentPerformance();
                case "memory":
                    return getMemoryInfo();
                case "system":
                    return getSystemInfo();
                case "threads":
                    return getThreadInfo();
                case "gc":
                    return getGarbageCollectionInfo();
                case "server":
                    return getServerPerformance();
                case "worlds":
                    return getWorldPerformance();
                case "plugins":
                    return getPluginPerformance();
                case "history":
                    return getPerformanceHistory();
                case "benchmark":
                    return runPerformanceBenchmark();
                default:
                    return ApiResponse.notFound("Unknown performance operation: " + subpath);
            }
        } catch (Exception e) {
            return ApiResponse.error("Error processing performance request: " + e.getMessage());
        }
    }
    
    /**
     * Gets current overall performance metrics
     */
    private ApiResponse getCurrentPerformance() {
        Map<String, Object> performance = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        
        // Add to history
        Map<String, Object> snapshot = createPerformanceSnapshot();
        addToHistory(snapshot);
        
        performance.put("timestamp", timestamp);
        performance.put("uptime", runtimeBean.getUptime());
        performance.put("memory", getMemoryData());
        performance.put("system", getSystemData());
        performance.put("server", getServerData());
        performance.put("performance", snapshot);
        
        return ApiResponse.ok(performance);
    }
    
    /**
     * Gets detailed memory information
     */
    private ApiResponse getMemoryInfo() {
        Map<String, Object> memory = new HashMap<>();
        
        // Heap memory
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        Map<String, Object> heap = new HashMap<>();
        heap.put("init", heapMemory.getInit());
        heap.put("used", heapMemory.getUsed());
        heap.put("committed", heapMemory.getCommitted());
        heap.put("max", heapMemory.getMax());
        heap.put("usagePercent", heapMemory.getMax() > 0 ? 
            (double) heapMemory.getUsed() / heapMemory.getMax() * 100 : 0);
        
        // Non-heap memory
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("init", nonHeapMemory.getInit());
        nonHeap.put("used", nonHeapMemory.getUsed());
        nonHeap.put("committed", nonHeapMemory.getCommitted());
        nonHeap.put("max", nonHeapMemory.getMax());
        
        // Memory pools
        List<Map<String, Object>> pools = new ArrayList<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            Map<String, Object> poolInfo = new HashMap<>();
            poolInfo.put("name", pool.getName());
            poolInfo.put("type", pool.getType().toString());
            
            MemoryUsage usage = pool.getUsage();
            if (usage != null) {
                poolInfo.put("init", usage.getInit());
                poolInfo.put("used", usage.getUsed());
                poolInfo.put("committed", usage.getCommitted());
                poolInfo.put("max", usage.getMax());
            }
            
            pools.add(poolInfo);
        }
        
        memory.put("heap", heap);
        memory.put("nonHeap", nonHeap);
        memory.put("pools", pools);
        memory.put("objectPendingFinalization", memoryBean.getObjectPendingFinalizationCount());
        
        return ApiResponse.ok(memory);
    }
    
    /**
     * Gets system information
     */
    private ApiResponse getSystemInfo() {
        Map<String, Object> system = new HashMap<>();
        
        // Operating system
        system.put("name", osBean.getName());
        system.put("version", osBean.getVersion());
        system.put("architecture", osBean.getArch());
        system.put("availableProcessors", osBean.getAvailableProcessors());
        
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            
            system.put("processCpuLoad", sunOsBean.getProcessCpuLoad() * 100);
            system.put("systemCpuLoad", sunOsBean.getSystemCpuLoad() * 100);
            system.put("processCpuTime", sunOsBean.getProcessCpuTime());
            system.put("totalPhysicalMemory", sunOsBean.getTotalPhysicalMemorySize());
            system.put("freePhysicalMemory", sunOsBean.getFreePhysicalMemorySize());
            system.put("totalSwapSpace", sunOsBean.getTotalSwapSpaceSize());
            system.put("freeSwapSpace", sunOsBean.getFreeSwapSpaceSize());
        }
        
        // Runtime
        Map<String, Object> runtime = new HashMap<>();
        runtime.put("name", runtimeBean.getName());
        runtime.put("vmName", runtimeBean.getVmName());
        runtime.put("vmVendor", runtimeBean.getVmVendor());
        runtime.put("vmVersion", runtimeBean.getVmVersion());
        runtime.put("specName", runtimeBean.getSpecName());
        runtime.put("specVendor", runtimeBean.getSpecVendor());
        runtime.put("specVersion", runtimeBean.getSpecVersion());
        runtime.put("uptime", runtimeBean.getUptime());
        runtime.put("startTime", runtimeBean.getStartTime());
        runtime.put("inputArguments", runtimeBean.getInputArguments());
        
        system.put("runtime", runtime);
        
        return ApiResponse.ok(system);
    }
    
    /**
     * Gets thread information
     */
    private ApiResponse getThreadInfo() {
        Map<String, Object> threads = new HashMap<>();
        
        threads.put("threadCount", threadBean.getThreadCount());
        threads.put("peakThreadCount", threadBean.getPeakThreadCount());
        threads.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        threads.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());
        
        // Dead locked threads
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        threads.put("deadlockedThreads", deadlockedThreads != null ? deadlockedThreads.length : 0);
        
        // Thread details
        long[] allThreadIds = threadBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadBean.getThreadInfo(allThreadIds);
        
        List<Map<String, Object>> threadDetails = new ArrayList<>();
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo != null) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", threadInfo.getThreadId());
                detail.put("name", threadInfo.getThreadName());
                detail.put("state", threadInfo.getThreadState().toString());
                detail.put("cpuTime", threadBean.getThreadCpuTime(threadInfo.getThreadId()));
                detail.put("blockedCount", threadInfo.getBlockedCount());
                detail.put("blockedTime", threadInfo.getBlockedTime());
                detail.put("waitedCount", threadInfo.getWaitedCount());
                detail.put("waitedTime", threadInfo.getWaitedTime());
                threadDetails.add(detail);
            }
        }
        
        threads.put("details", threadDetails);
        
        return ApiResponse.ok(threads);
    }
    
    /**
     * Gets garbage collection information
     */
    private ApiResponse getGarbageCollectionInfo() {
        Map<String, Object> gc = new HashMap<>();
        List<Map<String, Object>> collectors = new ArrayList<>();
        
        long totalCollections = 0;
        long totalCollectionTime = 0;
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            Map<String, Object> collector = new HashMap<>();
            collector.put("name", gcBean.getName());
            collector.put("collectionCount", gcBean.getCollectionCount());
            collector.put("collectionTime", gcBean.getCollectionTime());
            collector.put("memoryPoolNames", Arrays.asList(gcBean.getMemoryPoolNames()));
            
            totalCollections += gcBean.getCollectionCount();
            totalCollectionTime += gcBean.getCollectionTime();
            
            collectors.add(collector);
        }
        
        gc.put("collectors", collectors);
        gc.put("totalCollections", totalCollections);
        gc.put("totalCollectionTime", totalCollectionTime);
        
        return ApiResponse.ok(gc);
    }
    
    /**
     * Gets server performance metrics
     */
    private ApiResponse getServerPerformance() {
        Map<String, Object> server = new HashMap<>();
        
        server.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        server.put("maxPlayers", Bukkit.getMaxPlayers());
        server.put("viewDistance", Bukkit.getViewDistance());
        server.put("serverName", Bukkit.getServer().getName());
        server.put("version", Bukkit.getVersion());
        server.put("bukkitVersion", Bukkit.getBukkitVersion());
        
        // World count and entities
        int totalEntities = 0;
        int totalChunks = 0;
        List<Map<String, Object>> worlds = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            Map<String, Object> worldInfo = new HashMap<>();
            worldInfo.put("name", world.getName());
            worldInfo.put("entities", world.getEntities().size());
            worldInfo.put("loadedChunks", world.getLoadedChunks().length);
            worldInfo.put("players", world.getPlayers().size());
            worldInfo.put("environment", world.getEnvironment().toString());
            
            totalEntities += world.getEntities().size();
            totalChunks += world.getLoadedChunks().length;
            worlds.add(worldInfo);
        }
        
        server.put("worlds", worlds);
        server.put("totalEntities", totalEntities);
        server.put("totalLoadedChunks", totalChunks);
        
        // Plugin count
        server.put("pluginCount", Bukkit.getPluginManager().getPlugins().length);
        
        return ApiResponse.ok(server);
    }
    
    /**
     * Gets world-specific performance data
     */
    private ApiResponse getWorldPerformance() {
        List<Map<String, Object>> worlds = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            Map<String, Object> worldData = new HashMap<>();
            worldData.put("name", world.getName());
            worldData.put("environment", world.getEnvironment().toString());
            worldData.put("entities", world.getEntities().size());
            worldData.put("loadedChunks", world.getLoadedChunks().length);
            worldData.put("players", world.getPlayers().size());
            worldData.put("time", world.getTime());
            worldData.put("weatherDuration", world.getWeatherDuration());
            worldData.put("hasStorm", world.hasStorm());
            worldData.put("isThundering", world.isThundering());
            
            // Spawn location
            Map<String, Object> spawn = new HashMap<>();
            spawn.put("x", world.getSpawnLocation().getX());
            spawn.put("y", world.getSpawnLocation().getY());
            spawn.put("z", world.getSpawnLocation().getZ());
            worldData.put("spawn", spawn);
            
            worlds.add(worldData);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("worlds", worlds);
        response.put("count", worlds.size());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets plugin performance data
     */
    private ApiResponse getPluginPerformance() {
        List<Map<String, Object>> plugins = new ArrayList<>();
        
        for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            Map<String, Object> pluginData = new HashMap<>();
            pluginData.put("name", plugin.getName());
            pluginData.put("version", plugin.getDescription().getVersion());
            pluginData.put("enabled", plugin.isEnabled());
            pluginData.put("authors", plugin.getDescription().getAuthors());
            pluginData.put("description", plugin.getDescription().getDescription());
            pluginData.put("website", plugin.getDescription().getWebsite());
            
            plugins.add(pluginData);
        }
        
        // Sort by name
        plugins.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("plugins", plugins);
        response.put("count", plugins.size());
        response.put("enabled", plugins.stream().mapToLong(p -> (Boolean) p.get("enabled") ? 1 : 0).sum());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets performance history
     */
    private ApiResponse getPerformanceHistory() {
        Map<String, Object> response = new HashMap<>();
        response.put("history", new ArrayList<>(performanceHistory));
        response.put("count", performanceHistory.size());
        response.put("maxSize", MAX_HISTORY_SIZE);
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Runs a performance benchmark
     */
    private ApiResponse runPerformanceBenchmark() {
        try {
            Map<String, Object> benchmark = new HashMap<>();
            long startTime = System.nanoTime();
            
            // Simple CPU benchmark
            long cpuStart = System.nanoTime();
            double result = 0;
            for (int i = 0; i < 1000000; i++) {
                result += Math.sqrt(i);
            }
            long cpuTime = System.nanoTime() - cpuStart;
            
            // Memory allocation benchmark
            long memStart = System.nanoTime();
            List<String> strings = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                strings.add("Benchmark string " + i);
            }
            long memTime = System.nanoTime() - memStart;
            
            // Cleanup
            strings.clear();
            strings = null;
            System.gc();
            
            long totalTime = System.nanoTime() - startTime;
            
            benchmark.put("cpuBenchmark", cpuTime / 1_000_000.0); // Convert to milliseconds
            benchmark.put("memoryBenchmark", memTime / 1_000_000.0);
            benchmark.put("totalTime", totalTime / 1_000_000.0);
            benchmark.put("timestamp", System.currentTimeMillis());
            benchmark.put("result", result); // To prevent optimization
            
            return ApiResponse.ok(benchmark);
        } catch (Exception e) {
            return ApiResponse.error("Error running benchmark: " + e.getMessage());
        }
    }
    
    /**
     * Creates a performance snapshot
     */
    private Map<String, Object> createPerformanceSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("timestamp", System.currentTimeMillis());
        snapshot.put("memory", getMemoryData());
        snapshot.put("system", getSystemData());
        snapshot.put("server", getServerData());
        
        return snapshot;
    }
    
    /**
     * Gets memory data
     */
    private Map<String, Object> getMemoryData() {
        Map<String, Object> memory = new HashMap<>();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        
        memory.put("used", heapMemory.getUsed());
        memory.put("max", heapMemory.getMax());
        memory.put("usagePercent", heapMemory.getMax() > 0 ? 
            (double) heapMemory.getUsed() / heapMemory.getMax() * 100 : 0);
        
        return memory;
    }
    
    /**
     * Gets system data
     */
    private Map<String, Object> getSystemData() {
        Map<String, Object> system = new HashMap<>();
        
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            
            system.put("cpuLoad", sunOsBean.getProcessCpuLoad() * 100);
            system.put("systemCpuLoad", sunOsBean.getSystemCpuLoad() * 100);
        }
        
        system.put("availableProcessors", osBean.getAvailableProcessors());
        
        return system;
    }
    
    /**
     * Gets server data
     */
    private Map<String, Object> getServerData() {
        Map<String, Object> server = new HashMap<>();
        
        server.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        server.put("maxPlayers", Bukkit.getMaxPlayers());
        
        int totalEntities = 0;
        for (World world : Bukkit.getWorlds()) {
            totalEntities += world.getEntities().size();
        }
        server.put("totalEntities", totalEntities);
        
        return server;
    }
    
    /**
     * Adds a performance snapshot to history
     */
    private void addToHistory(Map<String, Object> snapshot) {
        performanceHistory.offer(snapshot);
        
        // Maintain max history size
        while (performanceHistory.size() > MAX_HISTORY_SIZE) {
            performanceHistory.poll();
        }
    }
}
