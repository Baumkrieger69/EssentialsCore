package com.essentialscore.api.performance;

import com.essentialscore.api.module.ModuleRegistry;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * AI-powered module that analyzes performance metrics and automatically optimizes
 * module configurations based on usage patterns and server load.
 */
public class ModuleOptimizer {
    private static final Logger LOGGER = Logger.getLogger(ModuleOptimizer.class.getName());
    
    private final Plugin plugin;
    private final ModuleRegistry moduleRegistry;
    private final ScheduledExecutorService scheduler;
    private final PerformanceMonitor performanceMonitor;
    private final Map<String, ModuleProfile> moduleProfiles;
    private final Map<String, OptimizationModel> optimizationModels;
    private final AnomalyDetector anomalyDetector;
    private final PredictiveScaler predictiveScaler;
    private final LoadBalancer loadBalancer;
    private final SelfHealingManager selfHealingManager;
    
    private boolean running = false;
    private int monitoringInterval = 60; // seconds
    private int optimizationInterval = 1800; // seconds (30 minutes)
    private int modelTrainingInterval = 86400; // seconds (24 hours)
    private boolean autoTuningEnabled = true;
    private boolean anomalyDetectionEnabled = true;
    private boolean predictiveScalingEnabled = true;
    private boolean loadBalancingEnabled = true;
    private boolean selfHealingEnabled = true;
    
    /**
     * Creates a new module optimizer.
     *
     * @param plugin The plugin
     * @param moduleRegistry The module registry
     */
    public ModuleOptimizer(Plugin plugin, ModuleRegistry moduleRegistry) {
        this.plugin = plugin;
        this.moduleRegistry = moduleRegistry;
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.moduleProfiles = new ConcurrentHashMap<>();
        this.optimizationModels = new ConcurrentHashMap<>();
        this.performanceMonitor = new PerformanceMonitor();
        this.anomalyDetector = new AnomalyDetector();
        this.predictiveScaler = new PredictiveScaler(plugin); // Use plugin parameter
        this.loadBalancer = new LoadBalancer(this);
        this.selfHealingManager = new SelfHealingManager(this);
        
        loadConfiguration();
    }
    
    /**
     * Loads configuration settings.
     */
    private void loadConfiguration() {
        // Load from plugin config
        monitoringInterval = plugin.getConfig().getInt("performance.monitoring-interval", monitoringInterval);
        optimizationInterval = plugin.getConfig().getInt("performance.optimization-interval", optimizationInterval);
        modelTrainingInterval = plugin.getConfig().getInt("performance.model-training-interval", modelTrainingInterval);
        autoTuningEnabled = plugin.getConfig().getBoolean("performance.auto-tuning-enabled", autoTuningEnabled);
        anomalyDetectionEnabled = plugin.getConfig().getBoolean("performance.anomaly-detection-enabled", anomalyDetectionEnabled);
        predictiveScalingEnabled = plugin.getConfig().getBoolean("performance.predictive-scaling-enabled", predictiveScalingEnabled);
        loadBalancingEnabled = plugin.getConfig().getBoolean("performance.load-balancing-enabled", loadBalancingEnabled);
        selfHealingEnabled = plugin.getConfig().getBoolean("performance.self-healing-enabled", selfHealingEnabled);
    }
    
    /**
     * Starts the module optimizer.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting Module Optimizer with AI-powered performance tuning");
        
        // Initialize performance monitoring
        performanceMonitor.start();
        
        // Schedule regular performance monitoring
        scheduler.scheduleAtFixedRate(
            this::collectPerformanceData,
            monitoringInterval,
            monitoringInterval,
            TimeUnit.SECONDS
        );
        
        // Schedule optimization runs
        scheduler.scheduleAtFixedRate(
            this::runOptimization,
            optimizationInterval,
            optimizationInterval,
            TimeUnit.SECONDS
        );
        
        // Schedule model training
        scheduler.scheduleAtFixedRate(
            this::trainModels,
            modelTrainingInterval,
            modelTrainingInterval,
            TimeUnit.SECONDS
        );
        
        // Start components based on configuration
        if (anomalyDetectionEnabled) {
            // AnomalyDetector doesn't require explicit start
        }
        
        if (predictiveScalingEnabled) {
            // predictiveScaler.start(); // PredictiveScaler doesn't have a start() method
        }
        
        if (loadBalancingEnabled) {
            loadBalancer.start();
        }
        
        if (selfHealingEnabled) {
            selfHealingManager.start();
        }
        
        running = true;
        LOGGER.info("Module Optimizer started");
    }
    
    /**
     * Stops the module optimizer.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping Module Optimizer");
        
        // Stop components
        // anomalyDetector.stop(); // AnomalyDetector doesn't have a stop() method
        // predictiveScaler.stop(); // PredictiveScaler doesn't have a stop() method
        loadBalancer.stop();
        selfHealingManager.stop();
        
        // Stop performance monitoring
        performanceMonitor.stop();
        
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
        LOGGER.info("Module Optimizer stopped");
    }
    
    /**
     * Collects performance data for all modules.
     */
    private void collectPerformanceData() {
        try {
            LOGGER.fine("Collecting performance data for modules");
            
            // Get current server metrics
            ServerMetrics serverMetrics = performanceMonitor.getCurrentMetrics();
            
            // Collect data for each module
            moduleRegistry.getModules().forEach((moduleId, module) -> {
                try {
                    // Get module-specific metrics
                    ModuleMetrics metrics = performanceMonitor.getModuleMetrics(moduleId);
                    
                    // Get or create module profile
                    ModuleProfile profile = moduleProfiles.computeIfAbsent(
                        moduleId, 
                        k -> new ModuleProfile(moduleId)
                    );
                    
                    // Add data point to profile
                    profile.addDataPoint(metrics, serverMetrics);
                    
                    // Check for anomalies if enabled
                    if (anomalyDetectionEnabled) {
                        anomalyDetector.detectAnomalies(moduleId, metrics.getCpuUsage(), serverMetrics.toString());
                    }
                } catch (Exception e) {
                    LOGGER.warning("Error collecting performance data for module " + moduleId + ": " + e.getMessage());
                }
            });
            
            // Update load balancer with latest metrics
            if (loadBalancingEnabled) {
                loadBalancer.updateMetrics(serverMetrics, moduleProfiles);
            }
            
            // Update predictive scaler with latest metrics
            if (predictiveScalingEnabled) {
                predictiveScaler.updateMetrics(serverMetrics, moduleProfiles);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Error in performance data collection: " + e.getMessage());
        }
    }
    
    /**
     * Runs the optimization process for all modules.
     */
    private void runOptimization() {
        if (!autoTuningEnabled) return;
        
        try {
            LOGGER.info("Running module optimization");
            
            // Get current server load
            ServerMetrics currentMetrics = performanceMonitor.getCurrentMetrics();
            double currentLoad = currentMetrics.getCpuLoad();
            
            // Optimize each module
            moduleRegistry.getModules().forEach((moduleId, module) -> {
                try {
                    ModuleProfile profile = moduleProfiles.get(moduleId);
                    if (profile == null || profile.getDataPointCount() < 10) {
                        // Not enough data yet
                        return;
                    }
                    
                    // Get optimization model for this module
                    OptimizationModel model = optimizationModels.get(moduleId);
                    if (model == null) {
                        LOGGER.fine("Creating new optimization model for module: " + moduleId);
                        model = new OptimizationModel(moduleId);
                        optimizationModels.put(moduleId, model);
                    }
                    
                    // Generate optimization suggestions
                    Map<String, Object> currentConfig = getCurrentModuleConfig(moduleId);
                    Map<String, Object> optimizedConfig = model.generateOptimizedConfig(
                        profile,
                        currentConfig,
                        currentLoad
                    );
                    
                    // Apply changes if auto-tuning is enabled
                    if (autoTuningEnabled && !optimizedConfig.isEmpty()) {
                        applyConfigChanges(moduleId, optimizedConfig);
                    }
                } catch (Exception e) {
                    LOGGER.warning("Error optimizing module " + moduleId + ": " + e.getMessage());
                }
            });
            
            LOGGER.info("Module optimization completed");
        } catch (Exception e) {
            LOGGER.warning("Error in optimization process: " + e.getMessage());
        }
    }
    
    /**
     * Trains all optimization models.
     */
    private void trainModels() {
        try {
            LOGGER.info("Training optimization models");
            
            moduleRegistry.getModules().forEach((moduleId, module) -> {
                try {
                    ModuleProfile profile = moduleProfiles.get(moduleId);
                    if (profile == null || profile.getDataPointCount() < 30) {
                        // Not enough data yet
                        return;
                    }
                    
                    // Get or create optimization model
                    OptimizationModel model = optimizationModels.computeIfAbsent(
                        moduleId,
                        k -> new OptimizationModel(moduleId)
                    );
                    
                    // Train the model
                    model.train(profile);
                    LOGGER.fine("Trained optimization model for module: " + moduleId);
                } catch (Exception e) {
                    LOGGER.warning("Error training model for module " + moduleId + ": " + e.getMessage());
                }
            });
            
            LOGGER.info("Model training completed");
        } catch (Exception e) {
            LOGGER.warning("Error in model training process: " + e.getMessage());
        }
    }
    
    /**
     * Gets the current configuration for a module.
     *
     * @param moduleId The module ID
     * @return The current configuration
     */
    private Map<String, Object> getCurrentModuleConfig(String moduleId) {
        // This would be implemented based on how module configs are stored
        // For now, return an empty map as a placeholder
        return new HashMap<>();
    }
    
    /**
     * Applies configuration changes to a module.
     *
     * @param moduleId The module ID
     * @param changes The configuration changes
     */
    private void applyConfigChanges(String moduleId, Map<String, Object> changes) {
        if (changes.isEmpty()) return;
        
        LOGGER.info("Applying optimized configuration to module " + moduleId + ": " + changes);
        
        // This would be implemented based on how module configs are stored and applied
        // For now, just log the changes
        changes.forEach((key, value) -> {
            LOGGER.info("Setting " + moduleId + "." + key + " = " + value);
        });
    }
    
    /**
     * Gets the plugin.
     *
     * @return The plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }
    
    /**
     * Gets the module registry.
     *
     * @return The module registry
     */
    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }
    
    /**
     * Gets the performance monitor.
     *
     * @return The performance monitor
     */
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    /**
     * Gets the module profiles.
     *
     * @return The module profiles
     */
    public Map<String, ModuleProfile> getModuleProfiles() {
        return moduleProfiles;
    }
    
    /**
     * Gets the optimization models.
     *
     * @return The optimization models
     */
    public Map<String, OptimizationModel> getOptimizationModels() {
        return optimizationModels;
    }
    
    /**
     * Gets the anomaly detector.
     *
     * @return The anomaly detector
     */
    public AnomalyDetector getAnomalyDetector() {
        return anomalyDetector;
    }
    
    /**
     * Gets the predictive scaler.
     *
     * @return The predictive scaler
     */
    public PredictiveScaler getPredictiveScaler() {
        return predictiveScaler;
    }
    
    /**
     * Gets the load balancer.
     *
     * @return The load balancer
     */
    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }
    
    /**
     * Gets the self-healing manager.
     *
     * @return The self-healing manager
     */
    public SelfHealingManager getSelfHealingManager() {
        return selfHealingManager;
    }
} 