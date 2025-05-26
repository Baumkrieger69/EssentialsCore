package com.essentialscore.api;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A simple command implementation that modules can use directly.
 */
public class SimpleCommand implements CommandDefinition {
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;
    private final String permission;
    private final String moduleName;
    private final Map<Integer, List<String>> tabCompletionOptions;
    
    private BiFunction<CommandSender, String[], Boolean> executor;
    private BiFunction<CommandSender, String[], List<String>> tabCompleter;
    
    /**
     * Creates a new simple command
     * 
     * @param name The command name
     * @param description The command description
     * @param usage The usage message
     * @param permission The permission required
     * @param moduleName The module name
     */
    public SimpleCommand(String name, String description, String usage, String permission, String moduleName) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.aliases = new ArrayList<>();
        this.permission = permission;
        this.moduleName = moduleName;
        this.tabCompletionOptions = new HashMap<>();
    }
    
    /**
     * Sets the function that executes this command
     * 
     * @param executor The function that takes a CommandSender and String[] args and returns a boolean
     * @return This command for method chaining
     */
    public SimpleCommand setExecutor(BiFunction<CommandSender, String[], Boolean> executor) {
        this.executor = executor;
        return this;
    }
    
    /**
     * Sets the function that provides tab completions for this command
     * 
     * @param tabCompleter The function that takes a CommandSender and String[] args and returns a List&lt;String&gt;
     * @return This command for method chaining
     */
    public SimpleCommand setTabCompleter(BiFunction<CommandSender, String[], List<String>> tabCompleter) {
        this.tabCompleter = tabCompleter;
        return this;
    }
    
    /**
     * Adds an alias for this command
     * 
     * @param alias The alias to add
     * @return This command for method chaining
     */
    public SimpleCommand addAlias(String alias) {
        if (alias != null && !alias.isEmpty()) {
            aliases.add(alias);
        }
        return this;
    }
    
    /**
     * Adds tab completion options for a specific argument index
     * 
     * @param argIndex The argument index (0-based)
     * @param options The list of options
     * @return This command for method chaining
     */
    public SimpleCommand addTabCompletionOptions(int argIndex, List<String> options) {
        if (options != null && !options.isEmpty()) {
            tabCompletionOptions.put(argIndex, new ArrayList<>(options));
        }
        return this;
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
        return Collections.unmodifiableList(aliases);
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
        return tabCompletionOptions.get(argIndex);
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (executor != null) {
            return executor.apply(sender, args);
        }
        return false;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (tabCompleter != null) {
            return tabCompleter.apply(sender, args);
        }
        
        // Use static tab completions if available
        int argIndex = args.length - 1;
        List<String> options = tabCompletionOptions.get(argIndex);
        
        if (options != null && !options.isEmpty()) {
            String currentArg = args[argIndex].toLowerCase();
            List<String> filtered = new ArrayList<>();
            
            for (String option : options) {
                if (option.toLowerCase().startsWith(currentArg)) {
                    filtered.add(option);
                }
            }
            
            return filtered;
        }
        
        return Collections.emptyList();
    }
} 