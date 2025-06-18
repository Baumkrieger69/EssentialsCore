package com.essentialscore.api.performance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intelligently distributes load across modules based on performance metrics.
 */
public class LoadBalancer {
    private static final Logger LOGGER = Logger.getLogger(LoadBalancer.class.getName());
    
    private final ModuleOptimizer optimizer;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ThreadAllocation> threadAllocations;
    private final Map<String, Long> moduleLastExecutionTime;
    
    private boolean running = false;
    private int balancingInterval = 30; // seconds
    private int totalThreads = Runtime.getRuntime().availableProcessors();
    
    /**
     * Creates a new load balancer.
     *
     * @param optimizer The module optimizer
     */
    public LoadBalancer(ModuleOptimizer optimizer) {
        this.optimizer = optimizer;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.threadAllocations = new ConcurrentHashMap<>();
        this.moduleLastExecutionTime = new ConcurrentHashMap<>();
    }
    
    /**
     * Starts the load balancer.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting intelligent load balancing service");
        
        // Schedule load balancing
        scheduler.scheduleAtFixedRate(
            this::balanceLoad,
            0,
            balancingInterval,
            TimeUnit.SECONDS
        );
        
        running = true;
    }
    
    /**
     * Stops the load balancer.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping intelligent load balancing service");
        
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
     * Updates metrics with the latest data.
     *
     * @param serverMetrics The server metrics
     * @param moduleProfiles The module profiles
     */
    public void updateMetrics(ServerMetrics serverMetrics, Map<String, ModuleProfile> moduleProfiles) {
        // This method is called by the ModuleOptimizer to feed in new metrics
    }
    
    /**
     * Records that a module is about to execute a task.
     *
     * @param moduleId The module ID
     * @return true if the module should proceed with execution
     */
    public boolean beforeModuleExecution(String moduleId) {
        // Record last execution time
        moduleLastExecutionTime.put(moduleId, System.currentTimeMillis());
        
        // Get thread allocation for this module
        ThreadAllocation allocation = threadAllocations.getOrDefault(
            moduleId,
            new ThreadAllocation(1, 5) // Default allocation
        );
        
        // Check if module should be throttled based on allocation
        return allocation.canExecute();
    }
    
    /**
     * Records that a module has finished executing a task.
     *
     * @param moduleId The module ID
     * @param executionTimeMs The execution time in milliseconds
     */
    public void afterModuleExecution(String moduleId, long executionTimeMs) {
        // Update thread allocation statistics
        ThreadAllocation allocation = threadAllocations.get(moduleId);
        if (allocation != null) {
            allocation.recordExecution(executionTimeMs);
        }
    }
    
    /**
     * Balances load across modules.
     */
    private void balanceLoad() {
        try {
            LOGGER.fine("Balancing load across modules");
            
            // Get current profiles
            Map<String, ModuleProfile> profiles = optimizer.getModuleProfiles();
            if (profiles.isEmpty()) return;
            
            // Calculate priority scores for each module
            Map<String, Double> priorityScores = calculatePriorityScores(profiles);
            
            // Allocate threads based on priority
            allocateThreads(priorityScores);
            
            // Log allocations
            LOGGER.fine("Thread allocations: " + threadAllocations);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error balancing load", e);
        }
    }
    
    /**
     * Calculates priority scores for modules.
     *
     * @param profiles The module profiles
     * @return The priority scores
     */
    private Map<String, Double> calculatePriorityScores(Map<String, ModuleProfile> profiles) {
        Map<String, Double> scores = new HashMap<>();
        
        // Calculate scores based on various factors
        for (Map.Entry<String, ModuleProfile> entry : profiles.entrySet()) {
            String moduleId = entry.getKey();
            ModuleProfile profile = entry.getValue();
            
            // Start with a base score
            double score = 1.0;
            
            // Adjust based on response time (higher response time = higher priority)
            if (profile.getAverageResponseTime() > 0) {
                score *= Math.min(5.0, profile.getAverageResponseTime() / 10.0 + 1.0);
            }
            
            // Adjust based on CPU usage (higher CPU usage = higher priority)
            score *= Math.min(3.0, profile.getAverageCpuUsage() * 5.0 + 1.0);
            
            // Adjust based on last execution time (more recent = higher priority)
            Long lastExecution = moduleLastExecutionTime.get(moduleId);
            if (lastExecution != null) {
                long timeSinceLastExecution = System.currentTimeMillis() - lastExecution;
                if (timeSinceLastExecution < 5000) { // 5 seconds
                    score *= 1.5; // Boost recently active modules
                }
            }
            
            // Apply module-specific adjustments
            if (moduleId.contains("database")) {
                score *= 1.5; // Database operations are high priority
            } else if (moduleId.contains("backup")) {
                score *= 0.8; // Backup operations are lower priority
            } else if (moduleId.contains("ui")) {
                score *= 1.3; // UI operations need to be responsive
            }
            
            scores.put(moduleId, score);
        }
        
        return scores;
    }
    
    /**
     * Allocates threads based on priority scores.
     *
     * @param priorityScores The priority scores
     */
    private void allocateThreads(Map<String, Double> priorityScores) {
        if (priorityScores.isEmpty()) return;
        
        // Calculate total priority score
        double totalScore = priorityScores.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // Allocate threads proportionally to priority scores
        for (Map.Entry<String, Double> entry : priorityScores.entrySet()) {
            String moduleId = entry.getKey();
            double score = entry.getValue();
            
            // Calculate thread allocation (minimum 1 thread)
            int allocatedThreads = Math.max(1, (int) Math.round((score / totalScore) * totalThreads));
            
            // Calculate max concurrent tasks based on thread allocation
            int maxConcurrentTasks = allocatedThreads * 2; // Allow some oversubscription
            
            // Update thread allocation
            ThreadAllocation allocation = threadAllocations.computeIfAbsent(
                moduleId,
                k -> new ThreadAllocation(allocatedThreads, maxConcurrentTasks)
            );
            
            allocation.setAllocatedThreads(allocatedThreads);
            allocation.setMaxConcurrentTasks(maxConcurrentTasks);
        }
    }
    
    /**
     * Gets the thread allocations.
     *
     * @return The thread allocations
     */
    public Map<String, ThreadAllocation> getThreadAllocations() {
        return threadAllocations;
    }
    
    /**
     * Class representing a thread allocation for a module.
     */
    public static class ThreadAllocation {
        private int allocatedThreads;
        private int maxConcurrentTasks;
        private int currentTasks;
        private long totalExecutionTime;
        private int executionCount;
        
        /**
         * Creates a new thread allocation.
         *
         * @param allocatedThreads The allocated threads
         * @param maxConcurrentTasks The maximum concurrent tasks
         */
        public ThreadAllocation(int allocatedThreads, int maxConcurrentTasks) {
            this.allocatedThreads = allocatedThreads;
            this.maxConcurrentTasks = maxConcurrentTasks;
            this.currentTasks = 0;
            this.totalExecutionTime = 0;
            this.executionCount = 0;
        }
        
        /**
         * Checks if the module can execute a task.
         *
         * @return true if the module can execute
         */
        public synchronized boolean canExecute() {
            if (currentTasks < maxConcurrentTasks) {
                currentTasks++;
                return true;
            }
            return false;
        }
        
        /**
         * Records an execution.
         *
         * @param executionTimeMs The execution time in milliseconds
         */
        public synchronized void recordExecution(long executionTimeMs) {
            currentTasks = Math.max(0, currentTasks - 1);
            totalExecutionTime += executionTimeMs;
            executionCount++;
        }
        
        /**
         * Gets the allocated threads.
         *
         * @return The allocated threads
         */
        public int getAllocatedThreads() {
            return allocatedThreads;
        }
        
        /**
         * Sets the allocated threads.
         *
         * @param allocatedThreads The allocated threads
         */
        public void setAllocatedThreads(int allocatedThreads) {
            this.allocatedThreads = allocatedThreads;
        }
        
        /**
         * Gets the maximum concurrent tasks.
         *
         * @return The maximum concurrent tasks
         */
        public int getMaxConcurrentTasks() {
            return maxConcurrentTasks;
        }
        
        /**
         * Sets the maximum concurrent tasks.
         *
         * @param maxConcurrentTasks The maximum concurrent tasks
         */
        public void setMaxConcurrentTasks(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
        }
        
        /**
         * Gets the current tasks.
         *
         * @return The current tasks
         */
        public int getCurrentTasks() {
            return currentTasks;
        }
        
        /**
         * Gets the average execution time.
         *
         * @return The average execution time
         */
        public double getAverageExecutionTime() {
            return executionCount > 0 ? (double) totalExecutionTime / executionCount : 0;
        }
        
        @Override
        public String toString() {
            return "ThreadAllocation{" +
                   "threads=" + allocatedThreads +
                   ", maxTasks=" + maxConcurrentTasks +
                   ", currentTasks=" + currentTasks +
                   ", avgExecTime=" + getAverageExecutionTime() +
                   "ms}";
        }
    }
} 
