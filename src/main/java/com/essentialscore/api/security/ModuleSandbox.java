package com.essentialscore.api.security;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ModuleSandbox provides an isolated execution environment for modules,
 * restricting their access to system resources based on security policies.
 */
public class ModuleSandbox {
    private static final Logger LOGGER = Logger.getLogger(ModuleSandbox.class.getName());
    
    private final Plugin plugin;
    private final String moduleId;
    private final String moduleName;
    private final SecurityPolicy securityPolicy;
    private final ExecutorService executorService;
    private final ClassLoaderSandbox classLoaderSandbox;
    private boolean active = false;
    
    /**
     * Creates a new module sandbox.
     *
     * @param plugin The EssentialsCore plugin
     * @param moduleId The module ID
     * @param moduleName The module name
     * @param securityPolicy The security policy
     */
    public ModuleSandbox(Plugin plugin, String moduleId, String moduleName, SecurityPolicy securityPolicy) {
        this.plugin = plugin;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.securityPolicy = securityPolicy;
        
        // Create class loader sandbox
        this.classLoaderSandbox = new ClassLoaderSandbox(plugin.getClass().getClassLoader(), moduleId);
        
        // Create sandboxed thread factory
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r, "Sandbox-" + moduleId);
            thread.setContextClassLoader(classLoaderSandbox.getClassLoader());
            return thread;
        };
        
        // Create executor service for sandboxed tasks
        this.executorService = Executors.newCachedThreadPool(threadFactory);
        
        this.active = true;
        LOGGER.info("Created sandbox for module: " + moduleName);
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
     * Gets the module name.
     *
     * @return The module name
     */
    public String getModuleName() {
        return moduleName;
    }
    
    /**
     * Gets the security policy.
     *
     * @return The security policy
     */
    public SecurityPolicy getSecurityPolicy() {
        return securityPolicy;
    }
    
    /**
     * Gets the plugin instance
     * 
     * @return The plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }
    
    /**
     * Executes a task in the sandbox.
     *
     * @param task The task to execute
     * @param <T> The task result type
     * @return The task result
     */
    public <T> T execute(SandboxedTask<T> task) {
        if (!active) {
            throw new IllegalStateException("Sandbox is not active");
        }
        
        try {
            // Set current sandbox in thread local
            SandboxContext.setCurrentSandbox(this);
            
            // Execute the task
            return task.execute();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error executing task in sandbox: " + moduleName, e);
            throw new SandboxExecutionException("Error executing task in sandbox: " + moduleName, e);
        } finally {
            // Clear current sandbox
            SandboxContext.clearCurrentSandbox();
        }
    }
    
    /**
     * Executes a task asynchronously in the sandbox.
     *
     * @param task The task to execute
     * @param <T> The task result type
     * @return The task future
     */
    public <T> java.util.concurrent.Future<T> executeAsync(SandboxedTask<T> task) {
        if (!active) {
            throw new IllegalStateException("Sandbox is not active");
        }
        
        return executorService.submit(() -> {
            try {
                // Set current sandbox in thread local
                SandboxContext.setCurrentSandbox(this);
                
                // Execute the task
                return task.execute();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error executing async task in sandbox: " + moduleName, e);
                throw new SandboxExecutionException("Error executing async task in sandbox: " + moduleName, e);
            } finally {
                // Clear current sandbox
                SandboxContext.clearCurrentSandbox();
            }
        });
    }    /**
     * Checks if a file operation is allowed.
     *
     * @param file The file
     * @param operation The operation (read, write, execute)
     * @return true if the file operation is allowed
     */
    public boolean isFileOperationAllowed(File file, String operation) {
        return securityPolicy.isFileOperationAllowed(file, operation);
    }    /**
     * Checks if a network operation is allowed.
     *
     * @param host The host
     * @param port The port
     * @param operation The operation (connect, listen)
     * @return true if the network operation is allowed
     */
    public boolean isNetworkOperationAllowed(String host, int port, String operation) {
        return securityPolicy.isNetworkOperationAllowed(host, port, operation);
    }    /**
     * Checks if a plugin interaction is allowed.
     *
     * @param pluginName The plugin name
     * @return true if the plugin interaction is allowed
     */
    public boolean isPluginInteractionAllowed(String pluginName) {
        return securityPolicy.isPluginInteractionAllowed(pluginName);
    }    /**
     * Checks if an operation is allowed.
     *
     * @param operationType The operation type
     * @param target The operation target
     * @return true if the operation is allowed
     */
    public boolean isOperationAllowed(String operationType, String target) {
        return securityPolicy.isOperationAllowed(operationType, target);
    }
    
    /**
     * Shuts down the sandbox.
     */
    public void shutdown() {
        if (!active) {
            return;
        }
        
        active = false;
        
        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Shutdown class loader sandbox
        classLoaderSandbox.close();
        
        LOGGER.info("Shut down sandbox for module: " + moduleName);
    }
    
    /**
     * Checks if the sandbox is active.
     *
     * @return true if the sandbox is active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Gets the class loader sandbox.
     *
     * @return The class loader sandbox
     */
    public ClassLoaderSandbox getClassLoaderSandbox() {
        return classLoaderSandbox;
    }

    /**
     * Sets strict mode for the sandbox.
     *
     * @param strict true to enable strict mode, false to disable
     */
    public void setStrictMode(boolean strict) {
        if (securityPolicy instanceof StrictModePolicy) {
            ((StrictModePolicy) securityPolicy).setStrictMode(strict);
        }
    }
    
    /**
     * Interface for sandboxed tasks.
     *
     * @param <T> The task result type
     */
    public interface SandboxedTask<T> {
        /**
         * Executes the task.
         *
         * @return The task result
         * @throws Exception If an error occurs
         */
        T execute() throws Exception;
    }
    
    /**
     * Exception thrown when a sandboxed task fails.
     */
    public static class SandboxExecutionException extends RuntimeException {
        /**
         * Creates a new sandbox execution exception.
         *
         * @param message The exception message
         * @param cause The exception cause
         */
        public SandboxExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
