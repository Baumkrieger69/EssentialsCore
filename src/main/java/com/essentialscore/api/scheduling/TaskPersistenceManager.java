package com.essentialscore.api.scheduling;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages persistence of tasks to survive server restarts.
 */
public class TaskPersistenceManager {
    private static final Logger LOGGER = Logger.getLogger(TaskPersistenceManager.class.getName());
    
    private final Plugin plugin;
    private final File storageFile;
    private boolean running;
    
    /**
     * Creates a new task persistence manager.
     *
     * @param plugin The plugin
     */
    public TaskPersistenceManager(Plugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "scheduled_tasks.yml");
        this.running = false;
    }
    
    /**
     * Starts the persistence manager.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting task persistence manager");
        
        // Create parent directory if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        running = true;
    }
    
    /**
     * Stops the persistence manager.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping task persistence manager");
        running = false;
    }
    
    /**
     * Saves tasks to persistent storage.
     *
     * @param tasks The tasks to save
     */
    public void saveTasks(Map<UUID, ScheduledTask> tasks) {
        if (!running) {
            LOGGER.warning("Attempted to save tasks while persistence manager is stopped");
            return;
        }
        
        try {
            FileConfiguration config = new YamlConfiguration();
            ConfigurationSection tasksSection = config.createSection("tasks");
            
            for (Map.Entry<UUID, ScheduledTask> entry : tasks.entrySet()) {
                UUID taskId = entry.getKey();
                ScheduledTask task = entry.getValue();
                
                // Skip transient tasks (those marked as not persistable)
                if (task.isExpired()) {
                    continue;
                }
                
                ConfigurationSection taskSection = tasksSection.createSection(taskId.toString());
                
                // Save basic task properties
                taskSection.set("name", task.getName());
                taskSection.set("priority", task.getPriority().name());
                taskSection.set("nextExecutionTime", task.getNextExecutionTime());
                taskSection.set("cronExpression", task.getCronExpression());
                taskSection.set("periodMillis", task.getPeriodMillis());
                taskSection.set("async", task.isAsync());
                taskSection.set("distributed", task.isDistributed());
                taskSection.set("resourceId", task.getResourceId());
                taskSection.set("maxRetries", task.getMaxRetries());
                taskSection.set("retryStrategy", task.getRetryStrategy().name());
                taskSection.set("retryCount", task.getRetryCount());
                taskSection.set("state", task.getState().name());
                taskSection.set("expirationTime", task.getExpirationTime());
                
                // Save dependencies as a list of UUIDs
                if (task.getDependencies() != null && !task.getDependencies().isEmpty()) {
                    taskSection.set("dependencies", task.getDependencies().stream()
                            .map(UUID::toString).toArray(String[]::new));
                }
                
                // We can't save the Runnable, so tasks will need to be reconnected with their
                // implementation when loaded
            }
            
            // Save to file
            config.save(storageFile);
            LOGGER.info("Saved " + tasks.size() + " tasks to " + storageFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving tasks to file", e);
        }
    }
    
    /**
     * Loads tasks from persistent storage.
     *
     * @return The loaded tasks
     */
    public Map<UUID, ScheduledTask> loadTasks() {
        Map<UUID, ScheduledTask> tasks = new HashMap<>();
        
        if (!running) {
            LOGGER.warning("Attempted to load tasks while persistence manager is stopped");
            return tasks;
        }
        
        if (!storageFile.exists()) {
            LOGGER.info("No tasks file found at " + storageFile.getAbsolutePath());
            return tasks;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
            ConfigurationSection tasksSection = config.getConfigurationSection("tasks");
            
            if (tasksSection == null) {
                LOGGER.info("No tasks found in " + storageFile.getAbsolutePath());
                return tasks;
            }
            
            for (String taskIdString : tasksSection.getKeys(false)) {
                try {
                    UUID taskId = UUID.fromString(taskIdString);
                    ConfigurationSection taskSection = tasksSection.getConfigurationSection(taskIdString);
                    
                    if (taskSection == null) continue;
                    
                    // Check if task has expired
                    long expirationTime = taskSection.getLong("expirationTime", 0);
                    if (expirationTime > 0 && System.currentTimeMillis() > expirationTime) {
                        LOGGER.fine("Skipping expired task: " + taskIdString);
                        continue;
                    }
                    
                    // Load task properties
                    String name = taskSection.getString("name", "unknown-task");
                    TaskPriority priority = TaskPriority.valueOf(
                            taskSection.getString("priority", TaskPriority.NORMAL.name()));
                    long nextExecutionTime = taskSection.getLong("nextExecutionTime", 0);
                    String cronExpression = taskSection.getString("cronExpression");
                    long periodMillis = taskSection.getLong("periodMillis", 0);
                    boolean async = taskSection.getBoolean("async", false);
                    boolean distributed = taskSection.getBoolean("distributed", false);
                    String resourceId = taskSection.getString("resourceId");
                    int maxRetries = taskSection.getInt("maxRetries", 3);
                    RetryStrategy retryStrategy = RetryStrategy.valueOf(
                            taskSection.getString("retryStrategy", RetryStrategy.EXPONENTIAL_BACKOFF.name()));
                    int retryCount = taskSection.getInt("retryCount", 0);
                    TaskState state = TaskState.valueOf(
                            taskSection.getString("state", TaskState.SCHEDULED.name()));
                    
                    // Load dependencies
                    java.util.List<String> dependencyStrings = taskSection.getStringList("dependencies");
                    UUID[] dependencies = dependencyStrings.stream()
                            .map(UUID::fromString)
                            .toArray(UUID[]::new);
                    
                    // Create placeholder task with a dummy Runnable
                    // The actual implementation will need to be reconnected by the application
                    ScheduledTask task = ScheduledTask.builder()
                            .id(taskId)
                            .name(name)
                            .runnable(() -> LOGGER.warning("Placeholder runnable executed for task: " + name))
                            .priority(priority)
                            .executeAt(nextExecutionTime)
                            .cronExpression(cronExpression)
                            .period(periodMillis)
                            .async(async)
                            .distributed(distributed)
                            .resourceId(resourceId)
                            .maxRetries(maxRetries)
                            .retryStrategy(retryStrategy)
                            .dependencies(dependencies)
                            .expiresAt(expirationTime)
                            .build();
                    
                    // Set non-builder properties
                    for (int i = 0; i < retryCount; i++) {
                        task.incrementRetryCount();
                    }
                    task.setState(state);
                    
                    tasks.put(taskId, task);
                    
                    LOGGER.fine("Loaded task: " + name + " (ID: " + taskId + ")");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error loading task " + taskIdString, e);
                }
            }
            
            LOGGER.info("Loaded " + tasks.size() + " tasks from " + storageFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading tasks from file", e);
        }
        
        return tasks;
    }
    
    /**
     * Deletes expired tasks from storage.
     */
    public void cleanupExpiredTasks() {
        if (!running || !storageFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
            ConfigurationSection tasksSection = config.getConfigurationSection("tasks");
            
            if (tasksSection == null) {
                return;
            }
            
            int removedCount = 0;
            long now = System.currentTimeMillis();
            
            for (String taskIdString : tasksSection.getKeys(false)) {
                ConfigurationSection taskSection = tasksSection.getConfigurationSection(taskIdString);
                
                if (taskSection == null) continue;
                
                long expirationTime = taskSection.getLong("expirationTime", 0);
                if (expirationTime > 0 && now > expirationTime) {
                    tasksSection.set(taskIdString, null);
                    removedCount++;
                }
            }
            
            if (removedCount > 0) {
                config.save(storageFile);
                LOGGER.info("Removed " + removedCount + " expired tasks from storage");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error cleaning up expired tasks", e);
        }
    }
} 
