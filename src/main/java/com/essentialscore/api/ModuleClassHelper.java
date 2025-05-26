package com.essentialscore.api;

import com.essentialscore.api.impl.CoreModuleAPI;
import com.essentialscore.api.impl.ModuleAdapter;

/**
 * This utility class serves as a reference for modules to understand which API classes
 * are available for use. This helps IDE auto-completion and provides a clear API surface.
 * <p>
 * Modules should only use classes and interfaces from the com.essentialscore.api package,
 * as these are guaranteed to be accessible and stable across versions.
 */
public final class ModuleClassHelper {
    
    private ModuleClassHelper() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Lists all core API interfaces that modules can use.
     * 
     * @return An array of Class objects representing available interfaces
     */
    @SuppressWarnings("unchecked")
    public static Class<?>[] getAvailableInterfaces() {
        return new Class<?>[] {
            Module.class,
            ModuleAPI.class,
            ModuleEventListener.class,
            CommandDefinition.class
        };
    }
    
    /**
     * Lists all utility/implementation classes available to modules.
     * 
     * @return An array of Class objects representing available classes
     */
    public static Class<?>[] getAvailableClasses() {
        return new Class<?>[] {
            BaseModule.class,
            SimpleCommand.class,
            CoreModuleAPI.class,
            ModuleAdapter.class
        };
    }
    
    /**
     * Ensures all API classes are loaded and accessible to the module.
     * This method can be called by modules to force class loading if needed.
     */
    public static void ensureApiClassesLoaded() {
        // Force load all API classes
        for (Class<?> clazz : getAvailableInterfaces()) {
            try {
                Class.forName(clazz.getName());
            } catch (ClassNotFoundException e) {
                // Should never happen since we're using Class objects
            }
        }
        
        for (Class<?> clazz : getAvailableClasses()) {
            try {
                Class.forName(clazz.getName());
            } catch (ClassNotFoundException e) {
                // Should never happen since we're using Class objects
            }
        }
    }
} 