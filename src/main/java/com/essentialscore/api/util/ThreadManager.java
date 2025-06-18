package com.essentialscore.api.util;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Manages threads for the plugin and modules.
 */
public class ThreadManager {
    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<String, ExecutorService> moduleExecutors;
    private final Map<Integer, String> taskOwners;
    private final Map<String, Map<Integer, BukkitTask>> moduleTasks;

    /**
     * Creates a new thread manager.
     *
     * @param plugin The plugin
     */
    public ThreadManager(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("EssentialsCore-Async-" + thread.threadId());
            return thread;
        });
        this.scheduledExecutorService = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r);
            thread.setName("EssentialsCore-Scheduled-" + thread.threadId());
            return thread;
        });
        this.moduleExecutors = new ConcurrentHashMap<>();
        this.taskOwners = new ConcurrentHashMap<>();
        this.moduleTasks = new ConcurrentHashMap<>();
    }

    /**
     * Runs a task asynchronously.
     *
     * @param moduleId The module ID
     * @param task The task to run
     * @return The task ID
     */
    public int runTaskAsync(String moduleId, Runnable task) {
        BukkitTask bukkitTask = scheduler.runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in async task for module " + moduleId, e);
            }
        });
        
        int taskId = bukkitTask.getTaskId();
        taskOwners.put(taskId, moduleId);
        moduleTasks.computeIfAbsent(moduleId, k -> new ConcurrentHashMap<>()).put(taskId, bukkitTask);
        
        return taskId;
    }

    /**
     * Runs a task asynchronously with a specific executor service.
     *
     * @param moduleId The module ID
     * @param task The task to run
     */
    public void runTaskWithExecutor(String moduleId, Runnable task) {
        ExecutorService executor = moduleExecutors.computeIfAbsent(moduleId, k -> Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("Module-" + moduleId + "-" + thread.threadId());
            return thread;
        }));
        
        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in executor task for module " + moduleId, e);
            }
        });
    }

    /**
     * Runs a task on the main server thread.
     *
     * @param moduleId The module ID
     * @param task The task to run
     * @return The task ID
     */
    public int runTask(String moduleId, Runnable task) {
        BukkitTask bukkitTask = scheduler.runTask(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in task for module " + moduleId, e);
            }
        });
        
        int taskId = bukkitTask.getTaskId();
        taskOwners.put(taskId, moduleId);
        moduleTasks.computeIfAbsent(moduleId, k -> new ConcurrentHashMap<>()).put(taskId, bukkitTask);
        
        return taskId;
    }

    /**
     * Runs a task after a delay.
     *
     * @param moduleId The module ID
     * @param task The task to run
     * @param delay The delay in ticks
     * @return The task ID
     */
    public int runTaskLater(String moduleId, Runnable task, long delay) {
        BukkitTask bukkitTask = scheduler.runTaskLater(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in delayed task for module " + moduleId, e);
            }
        }, delay);
        
        int taskId = bukkitTask.getTaskId();
        taskOwners.put(taskId, moduleId);
        moduleTasks.computeIfAbsent(moduleId, k -> new ConcurrentHashMap<>()).put(taskId, bukkitTask);
        
        return taskId;
    }

    /**
     * Runs a task after a delay asynchronously.
     *
     * @param moduleId The module ID
     * @param task The task to run
     * @param delay The delay in ticks
     * @return The task ID
     */
    public int runTaskLaterAsync(String moduleId, Runnable task, long delay) {
        BukkitTask bukkitTask = scheduler.runTaskLaterAsynchronously(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in delayed async task for module " + moduleId, e);
            }
        }, delay);
        
        int taskId = bukkitTask.getTaskId();
        taskOwners.put(taskId, moduleId);
        moduleTasks.computeIfAbsent(moduleId, k -> new ConcurrentHashMap<>()).put(taskId, bukkitTask);
        
        return taskId;
    }

    /**
     * Runs a repeating task.
     *
     * @param moduleId The module ID
     * @param task The task to run
     * @param delay The delay in ticks
     * @param period The period in ticks
     * @return The task ID
     */
    public int runTaskTimer(String moduleId, Runnable task, long delay, long period) {
        BukkitTask bukkitTask = scheduler.runTaskTimer(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in timer task for module " + moduleId, e);
            }
        }, delay, period);
        
        int taskId = bukkitTask.getTaskId();
        taskOwners.put(taskId, moduleId);
        moduleTasks.computeIfAbsent(moduleId, k -> new ConcurrentHashMap<>()).put(taskId, bukkitTask);
        
        return taskId;
    }

    /**
     * Runs a repeating task asynchronously.
     *
     * @param moduleId The module ID
     * @param task The task to run
     * @param delay The delay in ticks
     * @param period The period in ticks
     * @return The task ID
     */
    public int runTaskTimerAsync(String moduleId, Runnable task, long delay, long period) {
        BukkitTask bukkitTask = scheduler.runTaskTimerAsynchronously(plugin, () -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in timer async task for module " + moduleId, e);
            }
        }, delay, period);
        
        int taskId = bukkitTask.getTaskId();
        taskOwners.put(taskId, moduleId);
        moduleTasks.computeIfAbsent(moduleId, k -> new ConcurrentHashMap<>()).put(taskId, bukkitTask);
        
        return taskId;
    }

    /**
     * Runs a task with a scheduled executor service.
     *
     * @param moduleId The module ID
     * @param task The task to run
     * @param delay The delay in milliseconds
     * @param period The period in milliseconds
     * @return The scheduled future
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(String moduleId, Runnable task, long delay, long period) {
        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in scheduled task for module " + moduleId, e);
            }
        }, delay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancels a task.
     *
     * @param taskId The task ID
     * @return True if the task was cancelled
     */
    public boolean cancelTask(int taskId) {
        String moduleId = taskOwners.get(taskId);
        if (moduleId != null) {
            Map<Integer, BukkitTask> tasks = moduleTasks.get(moduleId);
            if (tasks != null) {
                BukkitTask task = tasks.remove(taskId);
                if (task != null) {
                    task.cancel();
                    taskOwners.remove(taskId);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cancels all tasks for a module.
     *
     * @param moduleId The module ID
     * @return The number of tasks cancelled
     */
    public int cancelTasks(String moduleId) {
        Map<Integer, BukkitTask> tasks = moduleTasks.get(moduleId);
        if (tasks != null) {
            int count = tasks.size();
            tasks.forEach((id, task) -> {
                task.cancel();
                taskOwners.remove(id);
            });
            tasks.clear();
            return count;
        }
        return 0;
    }

    /**
     * Runs a task asynchronously and returns a future for the result.
     *
     * @param <T> The result type
     * @param moduleId The module ID
     * @param supplier The supplier of the result
     * @return A future for the result
     */
    public <T> CompletableFuture<T> supplyAsync(String moduleId, Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        runTaskAsync(moduleId, () -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    /**
     * Shuts down the thread manager.
     */
    public void shutdown() {
        // Cancel all Bukkit tasks
        for (Map<Integer, BukkitTask> tasks : moduleTasks.values()) {
            tasks.forEach((id, task) -> task.cancel());
        }
        
        // Shutdown executor services
        executorService.shutdown();
        scheduledExecutorService.shutdown();
        moduleExecutors.values().forEach(ExecutorService::shutdown);
        
        // Try to await termination
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
            
            for (ExecutorService executor : moduleExecutors.values()) {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Force shutdown if needed
        executorService.shutdownNow();
        scheduledExecutorService.shutdownNow();
        moduleExecutors.values().forEach(ExecutorService::shutdownNow);
    }

    /**
     * Gets the executor service for a module.
     *
     * @param moduleId The module ID
     * @return The executor service
     */
    public ExecutorService getModuleExecutor(String moduleId) {
        return moduleExecutors.computeIfAbsent(moduleId, k -> Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("Module-" + moduleId + "-" + thread.threadId());
            return thread;
        }));
    }

    /**
     * Gets all tasks for a module.
     *
     * @param moduleId The module ID
     * @return A map of task IDs to tasks
     */
    public Map<Integer, BukkitTask> getModuleTasks(String moduleId) {
        return new HashMap<>(moduleTasks.getOrDefault(moduleId, new HashMap<>()));
    }

    /**
     * Gets the module ID for a task.
     *
     * @param taskId The task ID
     * @return The module ID, or null if not found
     */
    public String getTaskOwner(int taskId) {
        return taskOwners.get(taskId);
    }
} 
