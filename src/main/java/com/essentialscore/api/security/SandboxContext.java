package com.essentialscore.api.security;

/**
 * Provides thread-local access to the current sandbox context.
 */
public class SandboxContext {
    private static final ThreadLocal<ModuleSandbox> CURRENT_SANDBOX = new ThreadLocal<>();
    
    /**
     * Gets the current sandbox.
     *
     * @return The current sandbox, or null if not in a sandbox
     */
    public static ModuleSandbox getCurrentSandbox() {
        return CURRENT_SANDBOX.get();
    }
    
    /**
     * Sets the current sandbox.
     *
     * @param sandbox The sandbox to set
     */
    public static void setCurrentSandbox(ModuleSandbox sandbox) {
        CURRENT_SANDBOX.set(sandbox);
    }
    
    /**
     * Clears the current sandbox.
     */
    public static void clearCurrentSandbox() {
        CURRENT_SANDBOX.remove();
    }
    
    /**
     * Checks if the current thread is running in a sandbox.
     *
     * @return true if the current thread is running in a sandbox
     */
    public static boolean isInSandbox() {
        return getCurrentSandbox() != null;
    }
    
    /**
     * Gets the current module ID.
     *
     * @return The current module ID, or null if not in a sandbox
     */
    public static String getCurrentModuleId() {
        ModuleSandbox sandbox = getCurrentSandbox();
        return sandbox != null ? sandbox.getModuleId() : null;
    }
    
    /**
     * Checks if a file operation is allowed in the current sandbox.
     *
     * @param file The file
     * @param operation The operation (read, write, execute)
     * @return true if the file operation is allowed
     * @throws SecurityException If not in a sandbox or the operation is not allowed
     */
    public static boolean checkFileOperation(java.io.File file, String operation) {
        ModuleSandbox sandbox = getCurrentSandbox();
        
        if (sandbox == null) {
            throw new SecurityException("Not in a sandbox");
        }
        
        if (!sandbox.isFileOperationAllowed(file, operation)) {
            throw new SecurityException("File operation not allowed: " + operation + " -> " + file.getPath());
        }
        
        return true;
    }
    
    /**
     * Checks if a network operation is allowed in the current sandbox.
     *
     * @param host The host
     * @param port The port
     * @param operation The operation (connect, listen)
     * @return true if the network operation is allowed
     * @throws SecurityException If not in a sandbox or the operation is not allowed
     */
    public static boolean checkNetworkOperation(String host, int port, String operation) {
        ModuleSandbox sandbox = getCurrentSandbox();
        
        if (sandbox == null) {
            throw new SecurityException("Not in a sandbox");
        }
        
        if (!sandbox.isNetworkOperationAllowed(host, port, operation)) {
            throw new SecurityException("Network operation not allowed: " + operation + " -> " + host + ":" + port);
        }
        
        return true;
    }
    
    /**
     * Checks if a plugin interaction is allowed in the current sandbox.
     *
     * @param pluginName The plugin name
     * @return true if the plugin interaction is allowed
     * @throws SecurityException If not in a sandbox or the interaction is not allowed
     */
    public static boolean checkPluginInteraction(String pluginName) {
        ModuleSandbox sandbox = getCurrentSandbox();
        
        if (sandbox == null) {
            throw new SecurityException("Not in a sandbox");
        }
        
        if (!sandbox.isPluginInteractionAllowed(pluginName)) {
            throw new SecurityException("Plugin interaction not allowed: " + pluginName);
        }
        
        return true;
    }
    
    /**
     * Checks if an operation is allowed in the current sandbox.
     *
     * @param operationType The operation type
     * @param target The operation target
     * @return true if the operation is allowed
     * @throws SecurityException If not in a sandbox or the operation is not allowed
     */
    public static boolean checkOperation(String operationType, String target) {
        ModuleSandbox sandbox = getCurrentSandbox();
        
        if (sandbox == null) {
            throw new SecurityException("Not in a sandbox");
        }
        
        if (!sandbox.isOperationAllowed(operationType, target)) {
            throw new SecurityException("Operation not allowed: " + operationType + " -> " + target);
        }
        
        return true;
    }
} 
