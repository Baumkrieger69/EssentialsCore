package com.essentialscore.api.scheduling;

import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Advanced task scheduler that supports cron expressions, priorities, 
 * dependencies, rate limiting, and distributed execution.
 */
public class TaskScheduler {
    private static final Logger LOGGER = Logger.getLogger(TaskScheduler.class.getName());
    
    private final Plugin plugin;
    private final ScheduledExecutorService scheduler;
    private final PriorityTaskQueue priorityQueue;
    private final RateLimiter rateLimiter;
    private final TaskPersistenceManager persistenceManager;
    private final DependencyManager dependencyManager;
    private final DistributedTaskExecutor distributedExecutor;
    private final RetryManager retryManager;
    private final Map<UUID, ScheduledTask> activeTasks;
    private final TaskMetricsCollector metricsCollector;
    
    private boolean running = false;
    
    /**
     * Creates a new task scheduler.
     *
     * @param plugin The plugin
     */
    public TaskScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors() * 2);
        this.priorityQueue = new PriorityTaskQueue();
        this.rateLimiter = new RateLimiter();
        this.persistenceManager = new TaskPersistenceManager(plugin);
        this.dependencyManager = new DependencyManager();
        this.distributedExecutor = new DistributedTaskExecutor(plugin);
        this.retryManager = new RetryManager();
        this.activeTasks = new HashMap<>();
        this.metricsCollector = new TaskMetricsCollector();
    }
    
    /**
     * Starts the task scheduler.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting advanced task scheduler");
        
        // Start components
        priorityQueue.start();
        distributedExecutor.start();
        persistenceManager.start();
        
        // Load persisted tasks
        loadPersistedTasks();
        
        // Start task processor
        startTaskProcessor();
        
        running = true;
    }
    
    /**
     * Stops the task scheduler.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping advanced task scheduler");
        
        // Persist active tasks
        persistActiveTasks();
        
        // Stop components
        priorityQueue.stop();
        distributedExecutor.stop();
        persistenceManager.stop();
        
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
     * Loads persisted tasks from storage.
     */
    private void loadPersistedTasks() {
        LOGGER.info("Loading persisted tasks");
        Map<UUID, ScheduledTask> tasks = persistenceManager.loadTasks();
        
        tasks.forEach((id, task) -> {
            // Only restore tasks that haven't expired
            if (!task.isExpired()) {
                scheduleTask(task);
            }
        });
        
        LOGGER.info("Loaded " + tasks.size() + " persisted tasks");
    }
    
    /**
     * Persists active tasks to storage.
     */
    private void persistActiveTasks() {
        LOGGER.info("Persisting " + activeTasks.size() + " active tasks");
        persistenceManager.saveTasks(activeTasks);
    }
    
    /**
     * Starts the task processor that pulls tasks from the priority queue.
     */
    private void startTaskProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Process tasks in the queue
                ScheduledTask task = priorityQueue.poll();
                if (task != null) {
                    processTask(task);
                }
            } catch (Exception e) {
                LOGGER.warning("Error in task processor: " + e.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Processes a task from the priority queue.
     *
     * @param task The task to process
     */
    private void processTask(ScheduledTask task) {
        // Check dependencies
        if (!dependencyManager.areDependenciesMet(task)) {
            // Requeue for later processing
            priorityQueue.offer(task);
            return;
        }
        
        // Check rate limits
        if (!rateLimiter.allowExecution(task)) {
            // Requeue for later processing with delay
            scheduler.schedule(() -> priorityQueue.offer(task), 
                               rateLimiter.getNextAllowedTime(task) - System.currentTimeMillis(), 
                               TimeUnit.MILLISECONDS);
            return;
        }
        
        // Execute the task
        executeTask(task);
    }
    
    /**
     * Executes a task.
     *
     * @param task The task to execute
     */
    private void executeTask(ScheduledTask task) {
        CompletableFuture<Void> future;
        
        // Check if task should be executed distributedly
        if (task.isDistributed()) {
            future = distributedExecutor.executeTask(task);
        } else {
            future = executeLocally(task);
        }
        
        // Handle completion
        future.whenComplete((result, error) -> {
            if (error != null) {
                // Task failed
                handleTaskFailure(task, error);
            } else {
                // Task succeeded
                handleTaskSuccess(task);
            }
        });
    }
    
    /**
     * Executes a task locally.
     *
     * @param task The task to execute
     * @return A future for the task execution
     */
    private CompletableFuture<Void> executeLocally(ScheduledTask task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            // Record metrics
            metricsCollector.recordTaskStart(task);
            
            // Execute on Bukkit main thread or async based on task configuration
            if (task.isAsync()) {
                // Execute asynchronously
                scheduler.execute(() -> {
                    try {
                        task.getRunnable().run();
                        future.complete(null);
                        
                        // Record metrics
                        metricsCollector.recordTaskEnd(task, true);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                        
                        // Record metrics
                        metricsCollector.recordTaskEnd(task, false);
                    }
                });
            } else {
                // Execute on main server thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        task.getRunnable().run();
                        future.complete(null);
                        
                        // Record metrics
                        metricsCollector.recordTaskEnd(task, true);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                        
                        // Record metrics
                        metricsCollector.recordTaskEnd(task, false);
                    }
                });
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
            
            // Record metrics
            metricsCollector.recordTaskEnd(task, false);
        }
        
        return future;
    }
    
    /**
     * Handles a successful task execution.
     *
     * @param task The task that succeeded
     */
    private void handleTaskSuccess(ScheduledTask task) {
        // Check if task should be rescheduled
        if (task.shouldReschedule()) {
            task.updateNextExecutionTime();
            scheduleTask(task);
        } else {
            // Task completed its lifecycle
            activeTasks.remove(task.getId());
        }
    }
    
    /**
     * Handles a failed task execution.
     *
     * @param task The task that failed
     * @param error The error that occurred
     */
    private void handleTaskFailure(ScheduledTask task, Throwable error) {
        LOGGER.warning("Task failed: " + task.getName() + " - " + error.getMessage());
        
        // Check if task should be retried
        if (retryManager.shouldRetry(task)) {
            // Schedule retry with backoff
            long delay = retryManager.getRetryDelayMillis(task);
            task.incrementRetryCount();
            
            scheduler.schedule(() -> priorityQueue.offer(task), delay, TimeUnit.MILLISECONDS);
            LOGGER.info("Scheduled retry #" + task.getRetryCount() + " for task " + task.getName() + " in " + delay + "ms");
        } else {
            // Task has exceeded retry limit
            LOGGER.severe("Task " + task.getName() + " failed permanently after " + task.getRetryCount() + " retries");
            activeTasks.remove(task.getId());
            
            // Execute failure callback if present
            if (task.getFailureCallback() != null) {
                try {
                    task.getFailureCallback().accept(error);
                } catch (Exception e) {
                    LOGGER.warning("Error in failure callback: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Schedules a task.
     *
     * @param task The task to schedule
     * @return The task ID
     */
    public UUID scheduleTask(ScheduledTask task) {
        // Register task
        activeTasks.put(task.getId(), task);
        
        // Calculate initial delay
        long initialDelay = task.getNextExecutionTime() - System.currentTimeMillis();
        initialDelay = Math.max(0, initialDelay);
        
        // Schedule initial execution
        scheduler.schedule(() -> priorityQueue.offer(task), initialDelay, TimeUnit.MILLISECONDS);
        
        LOGGER.info("Scheduled task: " + task.getName() + " (ID: " + task.getId() + ") to run in " + initialDelay + "ms");
        return task.getId();
    }
    
    /**
     * Creates and schedules a new simple task.
     *
     * @param name The task name
     * @param runnable The task runnable
     * @param delay The delay in milliseconds
     * @return The task ID
     */
    public UUID scheduleDelayed(String name, Runnable runnable, long delay) {
        ScheduledTask task = ScheduledTask.builder()
                .name(name)
                .runnable(runnable)
                .executeAt(System.currentTimeMillis() + delay)
                .build();
        
        return scheduleTask(task);
    }
    
    /**
     * Creates and schedules a new repeating task.
     *
     * @param name The task name
     * @param runnable The task runnable
     * @param delay The initial delay in milliseconds
     * @param period The period in milliseconds
     * @return The task ID
     */
    public UUID scheduleRepeating(String name, Runnable runnable, long delay, long period) {
        ScheduledTask task = ScheduledTask.builder()
                .name(name)
                .runnable(runnable)
                .executeAt(System.currentTimeMillis() + delay)
                .period(period)
                .build();
        
        return scheduleTask(task);
    }
    
    /**
     * Creates and schedules a new task with a cron expression.
     *
     * @param name The task name
     * @param runnable The task runnable
     * @param cronExpression The cron expression
     * @return The task ID
     */
    public UUID scheduleCron(String name, Runnable runnable, String cronExpression) {
        ScheduledTask task = ScheduledTask.builder()
                .name(name)
                .runnable(runnable)
                .cronExpression(cronExpression)
                .build();
        
        return scheduleTask(task);
    }
    
    /**
     * Creates and schedules a task with dependencies.
     *
     * @param name The task name
     * @param runnable The task runnable
     * @param delay The delay in milliseconds
     * @param dependencies The task dependencies
     * @return The task ID
     */
    public UUID scheduleWithDependencies(String name, Runnable runnable, long delay, UUID... dependencies) {
        ScheduledTask task = ScheduledTask.builder()
                .name(name)
                .runnable(runnable)
                .executeAt(System.currentTimeMillis() + delay)
                .dependencies(dependencies)
                .build();
        
        return scheduleTask(task);
    }
    
    /**
     * Creates and schedules a high-priority task.
     *
     * @param name The task name
     * @param runnable The task runnable
     * @param delay The delay in milliseconds
     * @return The task ID
     */
    public UUID scheduleHighPriority(String name, Runnable runnable, long delay) {
        ScheduledTask task = ScheduledTask.builder()
                .name(name)
                .runnable(runnable)
                .executeAt(System.currentTimeMillis() + delay)
                .priority(TaskPriority.HIGH)
                .build();
        
        return scheduleTask(task);
    }
    
    /**
     * Cancels a task.
     *
     * @param taskId The task ID
     * @return true if the task was cancelled
     */
    public boolean cancelTask(UUID taskId) {
        ScheduledTask task = activeTasks.remove(taskId);
        if (task != null) {
            LOGGER.info("Cancelled task: " + task.getName() + " (ID: " + taskId + ")");
            return true;
        }
        return false;
    }
    
    /**
     * Gets a task by ID.
     *
     * @param taskId The task ID
     * @return The task, or null if not found
     */
    public ScheduledTask getTask(UUID taskId) {
        return activeTasks.get(taskId);
    }
    
    /**
     * Gets all active tasks.
     *
     * @return The active tasks
     */
    public Map<UUID, ScheduledTask> getActiveTasks() {
        return new HashMap<>(activeTasks);
    }
    
    /**
     * Gets the task metrics collector.
     *
     * @return The task metrics collector
     */
    public TaskMetricsCollector getMetricsCollector() {
        return metricsCollector;
    }
    
    /**
     * Sets a rate limit for a resource.
     *
     * @param resourceId The resource ID
     * @param maxExecutions The maximum number of executions
     * @param duration The time duration
     */
    public void setRateLimit(String resourceId, int maxExecutions, Duration duration) {
        rateLimiter.setLimit(resourceId, maxExecutions, duration);
    }
    
    /**
     * Gets the plugin.
     *
     * @return The plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }
} 
