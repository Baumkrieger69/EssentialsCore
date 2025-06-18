package com.essentialscore.api;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of the Module interface that provides default implementations
 * and helper methods for common module operations.
 */
public abstract class BaseModule implements Module {
    protected ModuleAPI api;
    protected FileConfiguration config;
    private final String name;
    private final String version;
    private final String description;
    private final Map<String, String> dependencies;
    private final Map<String, String> optionalDependencies;
    
    /**
     * Constructs a new base module with the specified information
     * 
     * @param name The module name
     * @param version The module version
     * @param description The module description
     */
    public BaseModule(String name, String version, String description) {
        this(name, version, description, Collections.emptyMap(), Collections.emptyMap());
    }
    
    /**
     * Constructs a new base module with the specified information
     * 
     * @param name The module name
     * @param version The module version
     * @param description The module description
     * @param dependencies Map of module dependencies with version ranges
     */
    public BaseModule(String name, String version, String description, Map<String, String> dependencies) {
        this(name, version, description, dependencies, Collections.emptyMap());
    }
    
    /**
     * Constructs a new base module with the specified information
     * 
     * @param name The module name
     * @param version The module version
     * @param description The module description
     * @param dependencies Map of module dependencies with version ranges
     * @param optionalDependencies Map of optional module dependencies with version ranges
     */
    public BaseModule(String name, String version, String description, 
                     Map<String, String> dependencies, 
                     Map<String, String> optionalDependencies) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.dependencies = dependencies != null ? 
            Collections.unmodifiableMap(new HashMap<>(dependencies)) : 
            Collections.emptyMap();
        this.optionalDependencies = optionalDependencies != null ? 
            Collections.unmodifiableMap(new HashMap<>(optionalDependencies)) : 
            Collections.emptyMap();
    }
    
    @Override
    public void onPreLoad(ModuleAPI api) {
        this.api = api;
        // Default implementation, can be overridden by subclasses
    }
    
    @Override
    public void init(ModuleAPI api, FileConfiguration config) {
        this.api = api;
        this.config = config;
        onInitialize();
    }
    
    /**
     * Called during module initialization. Override this method to perform module-specific initialization.
     */
    protected void onInitialize() {
        // Default implementation, can be overridden by subclasses
    }
    
    @Override
    public void onPostLoad() {
        // Default implementation, can be overridden by subclasses
    }
    
    @Override
    public void onDisable() {
        // Default implementation, can be overridden by subclasses
    }
    
    @Override
    public boolean onReload(FileConfiguration config) {
        this.config = config;
        return true;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public Map<String, String> getOptionalDependencies() {
        return optionalDependencies;
    }
    
    /**
     * Logs an informational message
     * 
     * @param message The message to log
     */
    protected void logInfo(String message) {
        if (api != null) {
            api.logInfo(message);
        }
    }
    
    /**
     * Logs a warning message
     * 
     * @param message The message to log
     */
    protected void logWarning(String message) {
        if (api != null) {
            api.logWarning(message);
        }
    }
    
    /**
     * Logs an error message
     * 
     * @param message The message to log
     */
    protected void logError(String message) {
        if (api != null) {
            api.logError(message, null);
        }
    }
    
    /**
     * Logs an error message with an exception
     * 
     * @param message The message to log
     * @param throwable The exception to log
     */
    protected void logError(String message, Throwable throwable) {
        if (api != null) {
            api.logError(message, throwable);
        }
    }
    
    /**
     * Logs a debug message
     * 
     * @param message The message to log
     */
    protected void logDebug(String message) {
        if (api != null) {
            api.logDebug(message);
        }
    }
    
    /**
     * Creates a new command definition
     * 
     * @param name The command name
     * @param description The command description
     * @param usage The command usage
     * @param permission The command permission
     * @return A new command definition
     */
    protected CommandDefinition createCommand(String name, String description, String usage, String permission) {
        return new SimpleCommandDefinition(name, description, usage, permission, getName());
    }
    
    /**
     * A simple implementation of CommandDefinition that modules can use
     */
    protected class SimpleCommandDefinition implements CommandDefinition {
        private final String name;
        private final String description;
        private final String usage;
        private final List<String> aliases;
        private final String permission;
        private final String moduleName;
        
        /**
         * Creates a new simple command definition
         * 
         * @param name The command name
         * @param description The command description
         * @param usage The command usage
         * @param permission The permission required
         * @param moduleName The module name
         */
        public SimpleCommandDefinition(String name, String description, String usage, 
                                      String permission, String moduleName) {
            this.name = name;
            this.description = description;
            this.usage = usage;
            this.aliases = new ArrayList<>();
            this.permission = permission;
            this.moduleName = moduleName;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getDescription() {
            return description;
        }
        
        @Override
        public String getUsage() {
            return usage;
        }
        
        @Override
        public List<String> getAliases() {
            return aliases;
        }
        
        /**
         * Adds an alias to this command
         * 
         * @param alias The alias to add
         * @return This command definition for method chaining
         */
        public SimpleCommandDefinition addAlias(String alias) {
            if (alias != null && !alias.isEmpty()) {
                aliases.add(alias);
            }
            return this;
        }
        
        @Override
        public String getPermission() {
            return permission;
        }
        
        @Override
        public String getModuleName() {
            return moduleName;
        }
        
        @Override
        public List<String> getTabCompletionOptions(int argIndex) {
            return null;
        }
        
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return BaseModule.this.onCommand(getName(), sender, args);
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            return BaseModule.this.onTabComplete(getName(), sender, args);
        }
    }
}
