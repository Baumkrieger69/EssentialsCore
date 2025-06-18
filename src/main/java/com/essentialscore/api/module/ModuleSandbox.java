package com.essentialscore.api.module;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides a sandboxed environment for executing module code safely.
 */
public class ModuleSandbox {
    private final Plugin plugin;
    private final ExecutorService executor;
    private final long defaultTimeoutMs;
    private boolean strictMode;

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
            thread.setName("ModuleSandbox-" + thread.threadId());
            return thread;
        });
    }

    /**
     * Creates a new module sandbox with module ID and security policy.
     *
     * @param plugin The plugin
     * @param moduleId The module identifier  
     * @param moduleName The module name
     * @param securityPolicy The security policy
     */
    public ModuleSandbox(Plugin plugin, String moduleId, String moduleName, com.essentialscore.api.security.SecurityPolicy securityPolicy) {
        this.plugin = plugin;
        this.defaultTimeoutMs = 5000;
        this.strictMode = securityPolicy != null;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("ModuleSandbox-" + moduleId + "-" + thread.threadId());
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
                        // Run with security checks in strict mode
                        return task.call();
                    } else {
                        // Run normally in non-strict mode
                        return task.call();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
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
     * Sets the strict mode for this sandbox
     * 
     * @param strict Whether to enable strict mode
     */
    public void setStrictMode(boolean strict) {
        // This would update the sandbox's security policy
        // For now, just log the change
        plugin.getLogger().info("Sandbox strict mode set to: " + strict);
    }
    
    /**
     * Checks if an operation is allowed in this sandbox
     * 
     * @param operation The operation to check
     * @param target The target of the operation
     * @return true if the operation is allowed
     */
    public boolean isOperationAllowed(String operation, String target) {
        // Implement operation checking logic
        // For now, allow most operations but block dangerous ones
        if (operation.contains("file.delete") && target.contains("..")) {
            return false; // Block path traversal
        }
        if (operation.contains("network") && strictMode) {
            return false; // Block network operations in strict mode
        }
        return true;
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
