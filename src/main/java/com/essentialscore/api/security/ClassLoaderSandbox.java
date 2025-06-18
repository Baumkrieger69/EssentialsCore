package com.essentialscore.api.security;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A sandboxed ClassLoader implementation that restricts access to classes based on security policies.
 */
public class ClassLoaderSandbox implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ClassLoaderSandbox.class.getName());
    
    private final String moduleId;
    private final SandboxClassLoader classLoader;
    private final Set<String> restrictedPackages;
    private final Set<String> allowedPackages;
    
    /**
     * Creates a new class loader sandbox.
     *
     * @param parent The parent class loader
     * @param moduleId The module ID
     */
    public ClassLoaderSandbox(ClassLoader parent, String moduleId) {
        this.moduleId = moduleId;
        this.classLoader = new SandboxClassLoader(parent, moduleId);
        this.restrictedPackages = new HashSet<>();
        this.allowedPackages = new HashSet<>();
        
        // Initialize restricted and allowed packages
        initializeRestrictedPackages();
        initializeAllowedPackages();
    }
    
    /**
     * Gets the sandboxed class loader.
     *
     * @return The class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    /**
     * Adds a URL to the classpath.
     *
     * @param url The URL to add
     */
    public void addURL(URL url) {
        classLoader.addURL(url);
    }
    
    /**
     * Adds a restricted package.
     *
     * @param packageName The package name
     */
    public void addRestrictedPackage(String packageName) {
        restrictedPackages.add(packageName);
    }
    
    /**
     * Adds an allowed package.
     *
     * @param packageName The package name
     */
    public void addAllowedPackage(String packageName) {
        allowedPackages.add(packageName);
    }
    
    /**
     * Checks if a class is restricted.
     *
     * @param className The class name
     * @return true if the class is restricted
     */
    public boolean isClassRestricted(String className) {
        // Check if the class is in a restricted package
        for (String pkg : restrictedPackages) {
            if (className.startsWith(pkg)) {
                // Check if it's explicitly allowed
                for (String allowedPkg : allowedPackages) {
                    if (className.startsWith(allowedPkg)) {
                        return false;
                    }
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Closes the sandbox class loader.
     */
    @Override
    public void close() {
        try {
            classLoader.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing sandbox class loader for module: " + moduleId, e);
        }
    }
    
    /**
     * Initializes the restricted packages.
     */
    private void initializeRestrictedPackages() {
        // System packages
        restrictedPackages.add("java.lang.reflect.");
        restrictedPackages.add("java.lang.System");
        restrictedPackages.add("java.lang.Runtime");
        restrictedPackages.add("java.lang.SecurityManager");
        restrictedPackages.add("java.security.");
        restrictedPackages.add("sun.");
        restrictedPackages.add("com.sun.");
        
        // Bukkit sensitive packages
        restrictedPackages.add("org.bukkit.Server");
        restrictedPackages.add("org.bukkit.plugin.java.JavaPluginLoader");
    }
    
    /**
     * Initializes the allowed packages.
     */
    private void initializeAllowedPackages() {
        // Bukkit allowed packages
        allowedPackages.add("org.bukkit.entity.");
        allowedPackages.add("org.bukkit.inventory.");
        allowedPackages.add("org.bukkit.material.");
        allowedPackages.add("org.bukkit.block.");
        
        // EssentialsCore API packages
        allowedPackages.add("com.essentialscore.api.");
    }
    
    /**
     * Sandboxed URL class loader implementation.
     */
    private class SandboxClassLoader extends URLClassLoader {
        private final String moduleId;
        
        /**
         * Creates a new sandbox class loader.
         *
         * @param parent The parent class loader
         * @param moduleId The module ID
         */
        public SandboxClassLoader(ClassLoader parent, String moduleId) {
            super(new URL[0], parent);
            this.moduleId = moduleId;
        }
        
        /**
         * Adds a URL to the classpath.
         *
         * @param url The URL to add
         */
        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }
        
        /**
         * Loads a class with the specified name.
         *
         * @param name The class name
         * @param resolve Whether to resolve the class
         * @return The loaded class
         * @throws ClassNotFoundException If the class cannot be found
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // Check if the class is restricted
            if (isClassRestricted(name)) {
                // Get the current sandbox from context
                ModuleSandbox sandbox = SandboxContext.getCurrentSandbox();
                
                if (sandbox != null) {
                    // Check if the module has the reflection permission
                    if (!sandbox.isOperationAllowed("reflection", name)) {
                        LOGGER.warning("Module " + moduleId + " attempted to access restricted class: " + name);
                        throw new SecurityException("Access to restricted class denied: " + name);
                    }
                }
            }
            
            return super.loadClass(name, resolve);
        }
        
        /**
         * Finds a resource with the specified name.
         *
         * @param name The resource name
         * @return The resource URL, or null if not found
         */
        @Override
        public URL getResource(String name) {
            // Check if the resource access is allowed
            if (name.startsWith("META-INF/services/java.") || 
                name.startsWith("META-INF/services/javax.") ||
                name.startsWith("META-INF/services/sun.")) {
                
                // Get the current sandbox from context
                ModuleSandbox sandbox = SandboxContext.getCurrentSandbox();
                
                if (sandbox != null) {
                    // Check if the module has the reflection permission
                    if (!sandbox.isOperationAllowed("reflection", name)) {
                        LOGGER.warning("Access to restricted resource denied: " + name);
                        return null;
                    }
                }
            }
            
            return super.getResource(name);
        }
        
        /**
         * Finds all resources with the specified name.
         *
         * @param name The resource name
         * @return An enumeration of resource URLs
         * @throws IOException If an I/O error occurs
         */
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            // Apply the same restrictions as getResource
            if (name.startsWith("META-INF/services/java.") || 
                name.startsWith("META-INF/services/javax.") ||
                name.startsWith("META-INF/services/sun.")) {
                
                // Get the current sandbox from context
                ModuleSandbox sandbox = SandboxContext.getCurrentSandbox();
                
                if (sandbox != null) {
                    // Check if the module has the reflection permission
                    if (!sandbox.isOperationAllowed("reflection", name)) {
                        LOGGER.warning("Access to restricted resources denied: " + name);
                        return java.util.Collections.emptyEnumeration();
                    }
                }
            }
            
            return super.getResources(name);
        }
    }
} 
