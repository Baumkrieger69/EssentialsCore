package com.essentialscore.api.performance;

import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Monitors performance metrics for the server and individual modules.
 */
public class PerformanceMonitor {
    private static final Logger LOGGER = Logger.getLogger(PerformanceMonitor.class.getName());
    
    private final ScheduledExecutorService scheduler;
    private final Map<String, ModuleMetrics> moduleMetricsMap;
    private final Map<String, AtomicLong> moduleCallCounts;
    private final Map<String, AtomicLong> moduleExecutionTimes;
    private final OperatingSystemMXBean osMXBean;
    private final ThreadMXBean threadMXBean;
    private final MetricsHistory metricsHistory;
    
    private ServerMetrics currentMetrics;
    private boolean running = false;
    private int samplingInterval = 5; // seconds
    
    /**    /**
     * Creates a new performance monitor.
     */
    public PerformanceMonitor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.moduleMetricsMap = new ConcurrentHashMap<>();
        this.moduleCallCounts = new ConcurrentHashMap<>();
        this.moduleExecutionTimes = new ConcurrentHashMap<>();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.metricsHistory = new MetricsHistory(1000); // Store 1000 data points
        this.currentMetrics = new ServerMetrics();
    }
    
    /**
     * Creates a new performance monitor.
     * @param plugin The plugin instance
     */
    public PerformanceMonitor(Object plugin) {
        this(); // Call the parameterless constructor
    }
    
    /**
     * Starts monitoring.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting performance monitoring");
        
        // Schedule metrics collection
        scheduler.scheduleAtFixedRate(
            this::collectMetrics,
            0,
            samplingInterval,
            TimeUnit.SECONDS
        );
        
        running = true;
    }
    
    /**
     * Stops monitoring.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping performance monitoring");
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        running = false;
    }
    
    /**
     * Collects server and module metrics.
     */
    private void collectMetrics() {
        try {
            // Collect server metrics
            double cpuLoad = osMXBean.getSystemLoadAverage();
            if (cpuLoad < 0) {
                // Not available on some platforms, use process CPU time as fallback
                try {
                    if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                        cpuLoad = ((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuLoad();
                    } else {
                        cpuLoad = 0.0; // Fallback to 0 if not available
                    }
                } catch (Exception e) {
                    cpuLoad = 0.0; // Fallback to 0 if there's any issue
                }
            }
            
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = Runtime.getRuntime().maxMemory();
            
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            final int[] counters = {0, 0}; // [loadedChunks, loadedEntities]
            
            // Count loaded chunks and entities across all worlds
            Bukkit.getWorlds().forEach(world -> {
                counters[0] += world.getLoadedChunks().length;
                counters[1] += world.getEntities().size();
            });
            
            int loadedChunks = counters[0];
            int loadedEntities = counters[1];
            
            // Create server metrics object
            ServerMetrics metrics = new ServerMetrics(
                cpuLoad,
                usedMemory,
                maxMemory,
                threadMXBean.getThreadCount(),
                onlinePlayers,
                loadedChunks,
                loadedEntities,
                System.currentTimeMillis()
            );
            
            // Update current metrics
            this.currentMetrics = metrics;
            
            // Add to history
            metricsHistory.addServerMetrics(metrics);
            
            // Calculate TPS (ticks per second)
            calculateTPS();
            
            // Reset per-interval counters for modules
            resetIntervalCounters();
            
        } catch (Exception e) {
            LOGGER.warning("Error collecting performance metrics: " + e.getMessage());
        }
    }
    
    /**
     * Calculates the server's TPS (ticks per second).
     */
    private void calculateTPS() {
        // This would ideally use the server's actual tick rate
        // For now, we'll use a placeholder
        double tps = 20.0; // Minecraft aims for 20 TPS
        currentMetrics.setTps(tps);
    }
    
    /**
     * Resets the per-interval counters for modules.
     */
    private void resetIntervalCounters() {
        // Reset call counts and execution times for this interval
        // but preserve the module metrics objects
    }
    
    /**
     * Records the start of a module method execution.
     *
     * @param moduleId The module ID
     * @param methodName The method name
     * @return The start time in nanoseconds
     */
    public long recordMethodStart(String moduleId, String methodName) {
        // Get or create module metrics
        moduleMetricsMap.computeIfAbsent(
            moduleId,
            k -> new ModuleMetrics(moduleId)
        );
        
        // Increment call count
        moduleCallCounts.computeIfAbsent(
            moduleId + "." + methodName,
            k -> new AtomicLong(0)
        ).incrementAndGet();
        
        // Return current time for later calculation
        return System.nanoTime();
    }
    
    /**
     * Records the end of a module method execution.
     *
     * @param moduleId The module ID
     * @param methodName The method name
     * @param startTime The start time in nanoseconds
     */
    public void recordMethodEnd(String moduleId, String methodName, long startTime) {
        long executionTime = System.nanoTime() - startTime;
        
        // Get or create execution time counter
        moduleExecutionTimes.computeIfAbsent(
            moduleId + "." + methodName,
            k -> new AtomicLong(0)
        ).addAndGet(executionTime);
        
        // Update module metrics
        ModuleMetrics metrics = moduleMetricsMap.get(moduleId);
        if (metrics != null) {
            metrics.recordMethodExecution(methodName, executionTime);
        }
    }
    
    /**
     * Gets the current server metrics.
     *
     * @return The current server metrics
     */
    public ServerMetrics getCurrentMetrics() {
        return currentMetrics;
    }
    
    /**
     * Gets metrics for a specific module.
     *
     * @param moduleId The module ID
     * @return The module metrics
     */
    public ModuleMetrics getModuleMetrics(String moduleId) {
        return moduleMetricsMap.computeIfAbsent(
            moduleId,
            k -> new ModuleMetrics(moduleId)
        );
    }
    
    /**
     * Gets the metrics history.
     *
     * @return The metrics history
     */
    public MetricsHistory getMetricsHistory() {
        return metricsHistory;
    }
} 
