package com.essentialscore.api;

import org.bukkit.command.CommandSender;
import com.essentialscore.api.command.CommandContext;

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
    private Map<String, SimpleCommand> subCommands;
    private SimpleCommand parent;
    
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
     * Creates a new builder for a simple command
     * 
     * @param name The command name
     * @param moduleName The module name
     * @return A new Builder instance
     */
    public static Builder builder(String name, String moduleName) {
        return new Builder(name, moduleName);
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
    
    /**
     * Adds a sub-command to this command
     * 
     * @param subCommand The sub-command to add
     * @return This command for method chaining
     */    public SimpleCommand addSubCommand(SimpleCommand subCommand) {
        if (subCommands == null) {
            subCommands = new HashMap<>();
        }
        subCommands.put(subCommand.getName(), subCommand);
        subCommand.setParent(this);
        return this;
    }
    
    /**
     * Sets the description for this command
     * 
     * @param description The description
     * @return This command for method chaining
     */
    public SimpleCommand description(String description) {
        return new SimpleCommand(this.name, description, this.usage, this.permission, this.moduleName);
    }
    
    /**
     * Sets the usage for this command
     * 
     * @param usage The usage message
     * @return This command for method chaining
     */
    public SimpleCommand usage(String usage) {
        return new SimpleCommand(this.name, this.description, usage, this.permission, this.moduleName);
    }
    
    /**
     * Sets aliases for this command
     * 
     * @param aliases The aliases list
     * @return This command for method chaining
     */
    public SimpleCommand aliases(List<String> aliases) {
        SimpleCommand cmd = new SimpleCommand(this.name, this.description, this.usage, this.permission, this.moduleName);
        if (aliases != null) {
            cmd.aliases.addAll(aliases);
        }
        return cmd;
    }
    
    /**
     * Sets the permission for this command
     * 
     * @param permission The permission
     * @return This command for method chaining
     */
    public SimpleCommand permission(String permission) {
        return new SimpleCommand(this.name, this.description, this.usage, permission, this.moduleName);
    }
    
    /**
     * Builds the command with an executor and tab completer
     * 
     * @param executor The command executor
     * @param tabCompleter The tab completer
     * @return This command
     */
    public SimpleCommand build(BiFunction<CommandSender, String[], Boolean> executor, 
                               BiFunction<CommandSender, String[], List<String>> tabCompleter) {
        this.executor = executor;
        this.tabCompleter = tabCompleter;
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
    
    /**
     * Sets the parent command for this command
     * 
     * @param parent The parent command
     */
    public void setParent(SimpleCommand parent) {
        this.parent = parent;
    }
    
    /**
     * Gets the parent command
     * 
     * @return The parent command, or null if this is a root command
     */
    public SimpleCommand getParent() {
        return parent;
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
    
    /**
     * Builder class for SimpleCommand
     */    public static class Builder {
        private final String name;
        private final String moduleName;
        private String description = "";
        private String permission = null;
        private SimpleCommand parent = null;
        private BiFunction<CommandContext, String[], Boolean> executor = null;
        
        public Builder(String name, String moduleName) {
            this.name = name;
            this.moduleName = moduleName;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }
          public Builder parent(SimpleCommand parent) {
            this.parent = parent;
            return this;
        }
          public SimpleCommand build(BiFunction<CommandContext, String[], Boolean> executor) {
            this.executor = executor;
            SimpleCommand command = new SimpleCommand(name, description, "/" + name, permission, moduleName);
            if (this.executor != null) {
                command.setExecutor((sender, args) -> {
                    CommandContext context = new CommandContext(sender, command.getName(), args);
                    return this.executor.apply(context, args);
                });
            }
            if (this.parent != null) {
                command.setParent(this.parent);
            }
            return command;
        }
        
        /**
         * Builds the command with an executor and tab completer
         * 
         * @param executor The command executor
         * @param tabCompleter The tab completer
         * @return This command
         */
        public SimpleCommand build(BiFunction<CommandContext, String[], Boolean> executor, 
                                 BiFunction<CommandSender, String[], List<String>> tabCompleter) {
            SimpleCommand command = build(executor);
            command.setTabCompleter(tabCompleter);
            return command;
        }
    }
}
