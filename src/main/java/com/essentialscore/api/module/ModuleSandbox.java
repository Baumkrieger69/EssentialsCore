package com.essentialscore.api.module;

import org.bukkit.plugin.Plugin;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Provides a sandboxed environment for executing module code safely.
 */
public class ModuleSandbox {
    private final Plugin plugin;
    private final ExecutorService executor;
    private final long defaultTimeoutMs;
    private final boolean strictMode;

    /**
     * Creates a new module sandbox.
     *
     * @param plugin The plugin
     */
    public ModuleSandbox(Plugin plugin) {
        this(plugin, 5000, true);
    }

    /**
     * Creates a new module sandbox with a custom timeout.
     *
     * @param plugin The plugin
     * @param defaultTimeoutMs The default timeout in milliseconds
     * @param strictMode Whether to use strict mode (more restrictive)
     */
    public ModuleSandbox(Plugin plugin, long defaultTimeoutMs, boolean strictMode) {
        this.plugin = plugin;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.strictMode = strictMode;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("ModuleSandbox-" + thread.getId());
            return thread;
        });
    }

    /**
     * Executes a task in the sandbox.
     *
     * @param <T> The return type
     * @param moduleId The module ID
     * @param task The task to execute
     * @return The result of the task
     * @throws Exception If an error occurs
     */
    public <T> T execute(String moduleId, Callable<T> task) throws Exception {
        return execute(moduleId, task, defaultTimeoutMs);
    }

    /**
     * Executes a task in the sandbox with a custom timeout.
     *
     * @param <T> The return type
     * @param moduleId The module ID
     * @param task The task to execute
     * @param timeoutMs The timeout in milliseconds
     * @return The result of the task
     * @throws Exception If an error occurs
     */
    public <T> T execute(String moduleId, Callable<T> task, long timeoutMs) throws Exception {
        try {
            Future<T> future = executor.submit(() -> {
                try {
                    if (strictMode) {
                        // Run with access control in strict mode
                        return AccessController.doPrivileged((PrivilegedExceptionAction<T>) task::call);
                    } else {
                        // Run normally in non-strict mode
                        return task.call();
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in module sandbox for " + moduleId, e);
                    throw e;
                }
            });
            
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException("Module " + moduleId + " timed out after " + timeoutMs + "ms");
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Executes a task in the sandbox asynchronously.
     *
     * @param <T> The return type
     * @param moduleId The module ID
     * @param task The task to execute
     * @return A CompletableFuture for the result
     */
    public <T> CompletableFuture<T> executeAsync(String moduleId, Callable<T> task) {
        return executeAsync(moduleId, task, defaultTimeoutMs);
    }

    /**
     * Executes a task in the sandbox asynchronously with a custom timeout.
     *
     * @param <T> The return type
     * @param moduleId The module ID
     * @param task The task to execute
     * @param timeoutMs The timeout in milliseconds
     * @return A CompletableFuture for the result
     */
    public <T> CompletableFuture<T> executeAsync(String moduleId, Callable<T> task, long timeoutMs) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        executor.submit(() -> {
            try {
                T result = execute(moduleId, task, timeoutMs);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    /**
     * Executes a runnable in the sandbox.
     *
     * @param moduleId The module ID
     * @param task The task to execute
     * @throws Exception If an error occurs
     */
    public void execute(String moduleId, Runnable task) throws Exception {
        execute(moduleId, task, defaultTimeoutMs);
    }

    /**
     * Executes a runnable in the sandbox with a custom timeout.
     *
     * @param moduleId The module ID
     * @param task The task to execute
     * @param timeoutMs The timeout in milliseconds
     * @throws Exception If an error occurs
     */
    public void execute(String moduleId, Runnable task, long timeoutMs) throws Exception {
        execute(moduleId, () -> {
            task.run();
            return null;
        }, timeoutMs);
    }

    /**
     * Executes a runnable in the sandbox asynchronously.
     *
     * @param moduleId The module ID
     * @param task The task to execute
     * @return A CompletableFuture for when the task is complete
     */
    public CompletableFuture<Void> executeAsync(String moduleId, Runnable task) {
        return executeAsync(moduleId, task, defaultTimeoutMs);
    }

    /**
     * Executes a runnable in the sandbox asynchronously with a custom timeout.
     *
     * @param moduleId The module ID
     * @param task The task to execute
     * @param timeoutMs The timeout in milliseconds
     * @return A CompletableFuture for when the task is complete
     */
    public CompletableFuture<Void> executeAsync(String moduleId, Runnable task, long timeoutMs) {
        return executeAsync(moduleId, () -> {
            task.run();
            return null;
        }, timeoutMs);
    }

    /**
     * Checks if the sandbox is in strict mode.
     *
     * @return True if the sandbox is in strict mode
     */
    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Gets the default timeout.
     *
     * @return The default timeout in milliseconds
     */
    public long getDefaultTimeout() {
        return defaultTimeoutMs;
    }

    /**
     * Shuts down the sandbox.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 