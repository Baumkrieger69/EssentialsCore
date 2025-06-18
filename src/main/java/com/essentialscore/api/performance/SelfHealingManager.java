package com.essentialscore.api.performance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages self-healing mechanisms to automatically fix module problems.
 */
public class SelfHealingManager {
    private static final Logger LOGGER = Logger.getLogger(SelfHealingManager.class.getName());
    
    private final ModuleOptimizer optimizer;
    private final ScheduledExecutorService scheduler;
    private final Map<String, List<HealingAction>> healingHistory;
    private final Map<String, ModuleHealthStatus> moduleHealthStatus;
    private final List<HealingListener> listeners;
    
    private boolean running = false;
    private int healthCheckInterval = 60; // seconds
    
    /**
     * Creates a new self-healing manager.
     *
     * @param optimizer The module optimizer
     */
    public SelfHealingManager(ModuleOptimizer optimizer) {
        this.optimizer = optimizer;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.healingHistory = new ConcurrentHashMap<>();
        this.moduleHealthStatus = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }
    
    /**
     * Starts the self-healing manager.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting self-healing manager");
        
        // Schedule regular health checks
        scheduler.scheduleAtFixedRate(
            this::performHealthChecks,
            0,
            healthCheckInterval,
            TimeUnit.SECONDS
        );
        
        running = true;
    }
    
    /**
     * Stops the self-healing manager.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping self-healing manager");
        
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
     * Adds a healing listener.
     *
     * @param listener The listener to add
     */
    public void addListener(HealingListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a healing listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(HealingListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Performs health checks on all modules.
     */
    private void performHealthChecks() {
        try {
            LOGGER.fine("Performing module health checks");
            
            // Get the current server metrics
            ServerMetrics serverMetrics = optimizer.getPerformanceMonitor().getCurrentMetrics();
            
            // Check each module's health
            optimizer.getModuleRegistry().getModules().forEach((moduleId, module) -> {
                try {
                    // Get module metrics
                    ModuleMetrics moduleMetrics = optimizer.getPerformanceMonitor().getModuleMetrics(moduleId);
                    if (moduleMetrics == null) return;
                    
                    // Check module health
                    ModuleHealthStatus health = checkModuleHealth(moduleId, moduleMetrics, serverMetrics);
                    
                    // Store health status
                    moduleHealthStatus.put(moduleId, health);
                    
                    // Take action if unhealthy
                    if (health.getHealthState() == HealthState.UNHEALTHY) {
                        triggerHealing(moduleId, health.getIssues());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error checking health for module " + moduleId, e);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error performing health checks", e);
        }
    }
    
    /**
     * Checks the health of a module.
     *
     * @param moduleId The module ID
     * @param moduleMetrics The module metrics
     * @param serverMetrics The server metrics
     * @return The module health status
     */
    private ModuleHealthStatus checkModuleHealth(String moduleId, ModuleMetrics moduleMetrics, ServerMetrics serverMetrics) {
        List<HealthIssue> issues = new ArrayList<>();
        
        // Check for high error rate
        if (moduleMetrics.getErrorCount() > 0) {
            issues.add(new HealthIssue(
                HealthIssueType.HIGH_ERROR_RATE,
                "Module has reported errors: " + moduleMetrics.getErrorCount(),
                IssueSeverity.HIGH
            ));
        }
        
        // Check for high warning count
        if (moduleMetrics.getWarningCount() > 5) {
            issues.add(new HealthIssue(
                HealthIssueType.HIGH_WARNING_RATE,
                "Module has high warning count: " + moduleMetrics.getWarningCount(),
                IssueSeverity.MEDIUM
            ));
        }
        
        // Check for slow response time
        double avgResponseTime = moduleMetrics.getAverageResponseTime();
        if (avgResponseTime > 500) { // 500ms threshold
            issues.add(new HealthIssue(
                HealthIssueType.SLOW_RESPONSE,
                "Module has slow response time: " + avgResponseTime + "ms",
                avgResponseTime > 1000 ? IssueSeverity.HIGH : IssueSeverity.MEDIUM
            ));
        }
        
        // Check for high CPU usage
        double cpuUsage = moduleMetrics.getCpuUsage();
        if (cpuUsage > 0.8) { // 80% threshold
            issues.add(new HealthIssue(
                HealthIssueType.HIGH_CPU_USAGE,
                "Module has high CPU usage: " + (cpuUsage * 100) + "%",
                IssueSeverity.HIGH
            ));
        }
        
        // Check for high memory usage
        long memoryUsage = moduleMetrics.getMemoryUsage();
        if (memoryUsage > 1024 * 1024 * 100) { // 100MB threshold
            issues.add(new HealthIssue(
                HealthIssueType.HIGH_MEMORY_USAGE,
                "Module has high memory usage: " + (memoryUsage / (1024 * 1024)) + "MB",
                IssueSeverity.MEDIUM
            ));
        }
        
        // Check for deadlock (simplified detection)
        boolean potentialDeadlock = false; // In real implementation, would detect thread contention
        if (potentialDeadlock) {
            issues.add(new HealthIssue(
                HealthIssueType.DEADLOCK,
                "Potential deadlock detected in module",
                IssueSeverity.CRITICAL
            ));
        }
        
        // Determine overall health state
        HealthState state = issues.isEmpty() ? HealthState.HEALTHY : HealthState.UNHEALTHY;
        
        return new ModuleHealthStatus(moduleId, state, issues);
    }
    
    /**
     * Triggers healing actions for a module with the given issues.
     *
     * @param moduleId The module ID
     * @param issues The health issues
     */
    private void triggerHealing(String moduleId, List<HealthIssue> issues) {
        LOGGER.info("Triggering healing actions for module " + moduleId + " with " + issues.size() + " issues");
        
        // Create a list of healing actions to take
        List<HealingAction> actions = new ArrayList<>();
        
        // Determine actions based on issues
        for (HealthIssue issue : issues) {
            switch (issue.getType()) {
                case HIGH_ERROR_RATE:
                case HIGH_WARNING_RATE:
                    actions.add(new HealingAction(
                        moduleId,
                        HealingActionType.RESET_MODULE,
                        "Resetting module due to high error/warning rate",
                        issue
                    ));
                    break;
                    
                case SLOW_RESPONSE:
                    actions.add(new HealingAction(
                        moduleId,
                        HealingActionType.OPTIMIZE_CONFIGURATION,
                        "Optimizing configuration due to slow response time",
                        issue
                    ));
                    break;
                    
                case HIGH_CPU_USAGE:
                    actions.add(new HealingAction(
                        moduleId,
                        HealingActionType.THROTTLE_RESOURCES,
                        "Throttling resources due to high CPU usage",
                        issue
                    ));
                    break;
                    
                case HIGH_MEMORY_USAGE:
                    actions.add(new HealingAction(
                        moduleId,
                        HealingActionType.CLEAR_CACHES,
                        "Clearing caches due to high memory usage",
                        issue
                    ));
                    break;
                    
                case DEADLOCK:
                    actions.add(new HealingAction(
                        moduleId,
                        HealingActionType.RESTART_MODULE,
                        "Restarting module due to potential deadlock",
                        issue
                    ));
                    break;
                    
                case UNKNOWN:
                default:
                    actions.add(new HealingAction(
                        moduleId,
                        HealingActionType.LOG_ONLY,
                        "Unknown issue detected: " + issue.getDescription(),
                        issue
                    ));
                    break;
            }
        }
        
        // Execute healing actions
        for (HealingAction action : actions) {
            executeHealingAction(action);
            
            // Add to history
            healingHistory.computeIfAbsent(moduleId, k -> new ArrayList<>()).add(action);
            
            // Notify listeners
            notifyListeners(action);
        }
    }
    
    /**
     * Triggers healing based on an anomaly.
     *
     * @param anomaly The anomaly
     */
    public void triggerHealing(AnomalyDetector.Anomaly anomaly) {
        String moduleId = anomaly.getModuleId();
        HealthIssue issue;
        
        // Convert anomaly to health issue
        switch (anomaly.getType()) {
            case RESPONSE_TIME_SPIKE:
                issue = new HealthIssue(
                    HealthIssueType.SLOW_RESPONSE,
                    "Response time spike detected: " + anomaly.getDetails().get("currentValue") + "ms",
                    convertSeverity(anomaly.getSeverity())
                );
                break;
                
            case MEMORY_LEAK:
                issue = new HealthIssue(
                    HealthIssueType.HIGH_MEMORY_USAGE,
                    "Memory leak detected: " + anomaly.getDetails().get("averageGrowthRate") + " bytes/sec",
                    convertSeverity(anomaly.getSeverity())
                );
                break;
                
            default:
                issue = new HealthIssue(
                    HealthIssueType.UNKNOWN,
                    "Unknown anomaly: " + anomaly.getType(),
                    convertSeverity(anomaly.getSeverity())
                );
                break;
        }
        
        // Create list with single issue
        List<HealthIssue> issues = new ArrayList<>();
        issues.add(issue);
        
        // Trigger healing
        triggerHealing(moduleId, issues);
    }
    
    /**
     * Converts anomaly severity to issue severity.
     *
     * @param anomalySeverity The anomaly severity
     * @return The issue severity
     */
    private IssueSeverity convertSeverity(AnomalyDetector.Anomaly.Severity anomalySeverity) {
        switch (anomalySeverity) {
            case HIGH:
                return IssueSeverity.HIGH;
            case MEDIUM:
                return IssueSeverity.MEDIUM;
            case LOW:
                return IssueSeverity.LOW;
            default:
                return IssueSeverity.MEDIUM;
        }
    }
    
    /**
     * Executes a healing action.
     *
     * @param action The healing action
     */
    private void executeHealingAction(HealingAction action) {
        LOGGER.info("Executing healing action: " + action);
        
        try {
            String moduleId = action.getModuleId();
            
            switch (action.getType()) {
                case RESET_MODULE:
                    resetModule(moduleId);
                    break;
                    
                case RESTART_MODULE:
                    restartModule(moduleId);
                    break;
                    
                case OPTIMIZE_CONFIGURATION:
                    optimizeModuleConfiguration(moduleId);
                    break;
                    
                case THROTTLE_RESOURCES:
                    throttleModuleResources(moduleId);
                    break;
                    
                case CLEAR_CACHES:
                    clearModuleCaches(moduleId);
                    break;
                    
                case LOG_ONLY:
                    LOGGER.info("Logging issue only: " + action.getDescription());
                    break;
            }
            
            // Mark action as executed
            action.setExecuted(true);
            action.setExecutionTime(System.currentTimeMillis());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error executing healing action: " + action, e);
            action.setError(e.toString());
        }
    }
    
    /**
     * Resets a module to its initial state.
     *
     * @param moduleId The module ID
     */
    private void resetModule(String moduleId) {
        // Call into the module to reset its state
        LOGGER.info("Resetting module: " + moduleId);
        
        // Module reset implementation:
        // 1. Get the module instance
        // 2. Call a reset method if available
        // 3. Reinitialize any state that could be corrupted
    }
    
    /**
     * Restarts a module.
     *
     * @param moduleId The module ID
     */
    private void restartModule(String moduleId) {
        // Stop and start the module
        LOGGER.info("Restarting module: " + moduleId);
        
        // Module restart implementation:
        // 1. Get the module instance
        // 2. Call shutdown/disable method
        // 3. Call startup/enable method
    }
    
    /**
     * Optimizes a module's configuration.
     *
     * @param moduleId The module ID
     */
    private void optimizeModuleConfiguration(String moduleId) {
        // In a real implementation, this would adjust the module's configuration
        LOGGER.info("Optimizing configuration for module: " + moduleId);
        
        // Use the optimizer to generate optimized configuration
        ModuleProfile profile = optimizer.getModuleProfiles().get(moduleId);
        if (profile == null) return;
        
        // Get current server load
        ServerMetrics metrics = optimizer.getPerformanceMonitor().getCurrentMetrics();
        double currentLoad = metrics.getCpuLoad();
        
        // Generate optimized configuration
        Map<String, Object> currentConfig = new HashMap<>(); // Would get from actual module
        OptimizationModel model = optimizer.getOptimizationModels().get(moduleId);
        if (model != null && model.isTrained()) {
            Map<String, Object> optimizedConfig = model.generateOptimizedConfig(profile, currentConfig, currentLoad);
            
            // Apply optimized configuration
            LOGGER.info("Applying optimized configuration to module " + moduleId + ": " + optimizedConfig);
            // Would actually apply to the module here
        }
    }
    
    /**
     * Throttles a module's resource usage.
     *
     * @param moduleId The module ID
     */
    private void throttleModuleResources(String moduleId) {
        // Restrict the module's resource usage
        LOGGER.info("Throttling resources for module: " + moduleId);
        
        // Resource throttling implementation:
        // 1. Get the load balancer
        // 2. Reduce thread allocation for this module
        // 3. Set lower priority
        
        // Get current allocation
        LoadBalancer loadBalancer = optimizer.getLoadBalancer();
        Map<String, LoadBalancer.ThreadAllocation> allocations = loadBalancer.getThreadAllocations();
        LoadBalancer.ThreadAllocation allocation = allocations.get(moduleId);
        
        if (allocation != null) {
            // Reduce by 50%
            int newThreads = Math.max(1, allocation.getAllocatedThreads() / 2);
            int newTasks = Math.max(2, allocation.getMaxConcurrentTasks() / 2);
            
            allocation.setAllocatedThreads(newThreads);
            allocation.setMaxConcurrentTasks(newTasks);
            
            LOGGER.info("Throttled resources for module " + moduleId + 
                       ": threads=" + newThreads + ", tasks=" + newTasks);
        }
    }
    
    /**
     * Clears a module's caches.
     *
     * @param moduleId The module ID
     */
    private void clearModuleCaches(String moduleId) {
        // Clear the module's caches
        LOGGER.info("Clearing caches for module: " + moduleId);
        
        // Cache clearing implementation:
        // 1. Get the module instance
        // 2. Call cache clearing methods if available
    }
    
    /**
     * Notifies listeners of a healing action.
     *
     * @param action The healing action
     */
    private void notifyListeners(HealingAction action) {
        for (HealingListener listener : listeners) {
            try {
                listener.onHealingAction(action);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying healing listener", e);
            }
        }
    }
    
    /**
     * Gets the healing history for a module.
     *
     * @param moduleId The module ID
     * @return The healing history
     */
    public List<HealingAction> getHealingHistory(String moduleId) {
        return healingHistory.getOrDefault(moduleId, new ArrayList<>());
    }
    
    /**
     * Gets all healing history.
     *
     * @return The healing history
     */
    public Map<String, List<HealingAction>> getAllHealingHistory() {
        return healingHistory;
    }
    
    /**
     * Gets the health status for a module.
     *
     * @param moduleId The module ID
     * @return The health status
     */
    public ModuleHealthStatus getHealthStatus(String moduleId) {
        return moduleHealthStatus.getOrDefault(
            moduleId,
            new ModuleHealthStatus(moduleId, HealthState.UNKNOWN, new ArrayList<>())
        );
    }
    
    /**
     * Gets all health statuses.
     *
     * @return The health statuses
     */
    public Map<String, ModuleHealthStatus> getAllHealthStatuses() {
        return moduleHealthStatus;
    }
    
    /**
     * Interface for healing listeners.
     */
    public interface HealingListener {
        /**
         * Called when a healing action is executed.
         *
         * @param action The healing action
         */
        void onHealingAction(HealingAction action);
    }
    
    /**
     * Enum for health states.
     */
    public enum HealthState {
        HEALTHY,
        UNHEALTHY,
        UNKNOWN
    }
    
    /**
     * Enum for health issue types.
     */
    public enum HealthIssueType {
        HIGH_ERROR_RATE,
        HIGH_WARNING_RATE,
        SLOW_RESPONSE,
        HIGH_CPU_USAGE,
        HIGH_MEMORY_USAGE,
        DEADLOCK,
        UNKNOWN
    }
    
    /**
     * Enum for issue severities.
     */
    public enum IssueSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Enum for healing action types.
     */
    public enum HealingActionType {
        RESET_MODULE,
        RESTART_MODULE,
        OPTIMIZE_CONFIGURATION,
        THROTTLE_RESOURCES,
        CLEAR_CACHES,
        LOG_ONLY
    }
    
    /**
     * Class representing a module health status.
     */
    public static class ModuleHealthStatus {
        private final String moduleId;
        private final HealthState healthState;
        private final List<HealthIssue> issues;
        private final long timestamp;
        
        /**
         * Creates a new module health status.
         *
         * @param moduleId The module ID
         * @param healthState The health state
         * @param issues The issues
         */
        public ModuleHealthStatus(String moduleId, HealthState healthState, List<HealthIssue> issues) {
            this.moduleId = moduleId;
            this.healthState = healthState;
            this.issues = issues;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Gets the module ID.
         *
         * @return The module ID
         */
        public String getModuleId() {
            return moduleId;
        }
        
        /**
         * Gets the health state.
         *
         * @return The health state
         */
        public HealthState getHealthState() {
            return healthState;
        }
        
        /**
         * Gets the issues.
         *
         * @return The issues
         */
        public List<HealthIssue> getIssues() {
            return issues;
        }
        
        /**
         * Gets the timestamp.
         *
         * @return The timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "ModuleHealthStatus{" +
                   "moduleId='" + moduleId + '\'' +
                   ", healthState=" + healthState +
                   ", issues=" + issues.size() +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
    
    /**
     * Class representing a health issue.
     */
    public static class HealthIssue {
        private final HealthIssueType type;
        private final String description;
        private final IssueSeverity severity;
        private final long detectedTime;
        
        /**
         * Creates a new health issue.
         *
         * @param type The issue type
         * @param description The issue description
         * @param severity The issue severity
         */
        public HealthIssue(HealthIssueType type, String description, IssueSeverity severity) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.detectedTime = System.currentTimeMillis();
        }
        
        /**
         * Gets the issue type.
         *
         * @return The issue type
         */
        public HealthIssueType getType() {
            return type;
        }
        
        /**
         * Gets the issue description.
         *
         * @return The issue description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the issue severity.
         *
         * @return The issue severity
         */
        public IssueSeverity getSeverity() {
            return severity;
        }
        
        /**
         * Gets the detected time.
         *
         * @return The detected time
         */
        public long getDetectedTime() {
            return detectedTime;
        }
        
        @Override
        public String toString() {
            return "HealthIssue{" +
                   "type=" + type +
                   ", severity=" + severity +
                   ", description='" + description + '\'' +
                   '}';
        }
    }
    
    /**
     * Class representing a healing action.
     */
    public static class HealingAction {
        private final String moduleId;
        private final HealingActionType type;
        private final String description;
        private final HealthIssue relatedIssue;
        private final long creationTime;
        
        private boolean executed;
        private long executionTime;
        private String error;
        
        /**
         * Creates a new healing action.
         *
         * @param moduleId The module ID
         * @param type The action type
         * @param description The action description
         * @param relatedIssue The related issue
         */
        public HealingAction(String moduleId, HealingActionType type, String description, HealthIssue relatedIssue) {
            this.moduleId = moduleId;
            this.type = type;
            this.description = description;
            this.relatedIssue = relatedIssue;
            this.creationTime = System.currentTimeMillis();
            this.executed = false;
        }
        
        /**
         * Gets the module ID.
         *
         * @return The module ID
         */
        public String getModuleId() {
            return moduleId;
        }
        
        /**
         * Gets the action type.
         *
         * @return The action type
         */
        public HealingActionType getType() {
            return type;
        }
        
        /**
         * Gets the action description.
         *
         * @return The action description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the related issue.
         *
         * @return The related issue
         */
        public HealthIssue getRelatedIssue() {
            return relatedIssue;
        }
        
        /**
         * Gets the creation time.
         *
         * @return The creation time
         */
        public long getCreationTime() {
            return creationTime;
        }
        
        /**
         * Checks if the action was executed.
         *
         * @return true if executed
         */
        public boolean isExecuted() {
            return executed;
        }
        
        /**
         * Sets whether the action was executed.
         *
         * @param executed Whether the action was executed
         */
        public void setExecuted(boolean executed) {
            this.executed = executed;
        }
        
        /**
         * Gets the execution time.
         *
         * @return The execution time
         */
        public long getExecutionTime() {
            return executionTime;
        }
        
        /**
         * Sets the execution time.
         *
         * @param executionTime The execution time
         */
        public void setExecutionTime(long executionTime) {
            this.executionTime = executionTime;
        }
        
        /**
         * Gets the error.
         *
         * @return The error
         */
        public String getError() {
            return error;
        }
        
        /**
         * Sets the error.
         *
         * @param error The error
         */
        public void setError(String error) {
            this.error = error;
        }
        
        @Override
        public String toString() {
            return "HealingAction{" +
                   "moduleId='" + moduleId + '\'' +
                   ", type=" + type +
                   ", description='" + description + '\'' +
                   ", executed=" + executed +
                   (error != null ? ", error='" + error + '\'' : "") +
                   '}';
        }
    }
} 
