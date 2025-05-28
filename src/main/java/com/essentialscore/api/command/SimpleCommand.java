package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Simple command implementation with function handlers for execution and tab completion.
 */
public class SimpleCommand extends AbstractCommand {
    
    private final BiFunction<CommandSender, String[], Boolean> executionHandler;
    private final BiFunction<CommandSender, String[], List<String>> tabCompletionHandler;
    
    /**
     * Creates a new simple command.
     *
     * @param name The command name
     * @param description The command description
     * @param usage The command usage
     * @param aliases The command aliases
     * @param permission The command permission
     * @param moduleId The module ID
     * @param executionHandler The execution handler
     */
    public SimpleCommand(String name, String description, String usage, List<String> aliases,
                         String permission, String moduleId,
                         BiFunction<CommandSender, String[], Boolean> executionHandler) {
        this(name, description, usage, aliases, permission, moduleId, null, executionHandler, null);
    }
    
    /**
     * Creates a new simple command.
     *
     * @param name The command name
     * @param description The command description
     * @param usage The command usage
     * @param aliases The command aliases
     * @param permission The command permission
     * @param moduleId The module ID
     * @param parent The parent command
     * @param executionHandler The execution handler
     * @param tabCompletionHandler The tab completion handler
     */
    public SimpleCommand(String name, String description, String usage, List<String> aliases,
                         String permission, String moduleId, Command parent,
                         BiFunction<CommandSender, String[], Boolean> executionHandler,
                         BiFunction<CommandSender, String[], List<String>> tabCompletionHandler) {
        super(name, description, usage, aliases, permission, moduleId, parent, 0, -1, 
             "General", "", Collections.emptyList(), false, 0);
        this.executionHandler = executionHandler;
        this.tabCompletionHandler = tabCompletionHandler;
    }
    
    /**
     * Creates a new simple command with all configuration options.
     *
     * @param name The command name
     * @param description The command description
     * @param usage The command usage
     * @param aliases The command aliases
     * @param permission The command permission
     * @param moduleId The module ID
     * @param parent The parent command
     * @param executionHandler The execution handler
     * @param tabCompletionHandler The tab completion handler
     * @param minArgs The minimum number of arguments
     * @param maxArgs The maximum number of arguments
     * @param category The command category
     * @param detailedHelp The detailed help text
     * @param examples Examples of how to use the command
     * @param hidden Whether the command is hidden from listings
     * @param cooldown The command cooldown in seconds
     */
    public SimpleCommand(String name, String description, String usage, List<String> aliases,
                         String permission, String moduleId, Command parent,
                         BiFunction<CommandSender, String[], Boolean> executionHandler,
                         BiFunction<CommandSender, String[], List<String>> tabCompletionHandler,
                         int minArgs, int maxArgs, String category, String detailedHelp,
                         List<String> examples, boolean hidden, int cooldown) {
        super(name, description, usage, aliases, permission, moduleId, parent, minArgs, maxArgs,
             category, detailedHelp, examples, hidden, cooldown);
        this.executionHandler = executionHandler;
        this.tabCompletionHandler = tabCompletionHandler;
    }
    
    /**
     * Creates a new builder for a simple command.
     *
     * @param name The command name
     * @param moduleId The module ID
     * @return The builder
     */
    public static Builder builder(String name, String moduleId) {
        return new Builder(name, moduleId);
    }
    
    @Override
    public boolean execute(CommandContext context) {
        if (executionHandler == null) {
            return false;
        }
        
        CommandSender sender = context.getSender();
        String[] args = context.getParsedArgs().getAllAsArray();
        
        return executionHandler.apply(sender, args);
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (tabCompletionHandler == null) {
            return new ArrayList<>();
        }
        
        return tabCompletionHandler.apply(sender, args);
    }
    
    /**
     * Functional interface for executing commands.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        /**
         * Executes a command.
         *
         * @param context The command context
         * @return true if the command was executed successfully
         */
        boolean execute(CommandContext context);
    }
    
    /**
     * Functional interface for tab completion.
     */
    @FunctionalInterface
    public interface TabCompleter {
        /**
         * Provides tab completion options for a command.
         *
         * @param sender The command sender
         * @param args The command arguments
         * @return List of tab completion options
         */
        List<String> tabComplete(CommandSender sender, String[] args);
    }
    
    /**
     * Builder for creating simple commands.
     */
    public static class Builder {
        private String name;
        private String description = "";
        private String usage = "";
        private List<String> aliases = new ArrayList<>();
        private String permission = "";
        private String moduleId;
        private Command parent = null;
        private String category = "General";
        private int minArgs = 0;
        private int maxArgs = -1;
        private String detailedHelp = "";
        private List<String> examples = Collections.emptyList();
        private boolean hidden = false;
        private int cooldown = 0;
        
        /**
         * Creates a new builder.
         *
         * @param name The command name
         * @param moduleId The module ID
         */
        public Builder(String name, String moduleId) {
            this.name = name;
            this.moduleId = moduleId;
        }
        
        /**
         * Sets the command description.
         *
         * @param description The description
         * @return This builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the command usage.
         *
         * @param usage The usage
         * @return This builder
         */
        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }
        
        /**
         * Sets the command aliases.
         *
         * @param aliases The aliases
         * @return This builder
         */
        public Builder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }
        
        /**
         * Sets the command permission.
         *
         * @param permission The permission
         * @return This builder
         */
        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }
        
        /**
         * Sets the parent command.
         *
         * @param parent The parent command
         * @return This builder
         */
        public Builder parent(Command parent) {
            this.parent = parent;
            return this;
        }
        
        /**
         * Sets the command category.
         *
         * @param category The category
         * @return This builder
         */
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        /**
         * Sets the minimum arguments.
         *
         * @param minArgs The minimum arguments
         * @return This builder
         */
        public Builder minArgs(int minArgs) {
            this.minArgs = minArgs;
            return this;
        }
        
        /**
         * Sets the maximum arguments.
         *
         * @param maxArgs The maximum arguments
         * @return This builder
         */
        public Builder maxArgs(int maxArgs) {
            this.maxArgs = maxArgs;
            return this;
        }
        
        /**
         * Sets the detailed help text.
         *
         * @param detailedHelp The detailed help
         * @return This builder
         */
        public Builder detailedHelp(String detailedHelp) {
            this.detailedHelp = detailedHelp;
            return this;
        }
        
        /**
         * Sets the command examples.
         *
         * @param examples The examples
         * @return This builder
         */
        public Builder examples(List<String> examples) {
            this.examples = examples;
            return this;
        }
        
        /**
         * Sets whether the command is hidden.
         *
         * @param hidden Whether the command is hidden
         * @return This builder
         */
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }
        
        /**
         * Sets the command cooldown in seconds.
         *
         * @param cooldown The cooldown in seconds
         * @return This builder
         */
        public Builder cooldown(int cooldown) {
            this.cooldown = cooldown;
            return this;
        }
        
        /**
         * Builds the command with the specified executor.
         *
         * @param executor The command executor
         * @return The built command
         */
        public SimpleCommand build(CommandExecutor executor) {
            BiFunction<CommandSender, String[], Boolean> executionHandler = (sender, args) -> {
                CommandContext context = new CommandContext(sender, name, args);
                return executor.execute(context);
            };
            
            return new SimpleCommand(name, description, usage, aliases, permission, moduleId, parent,
                executionHandler, null, minArgs, maxArgs, category, detailedHelp, examples, hidden, cooldown);
        }
        
        /**
         * Builds the command with the specified executor and tab completer.
         *
         * @param executor The command executor
         * @param tabCompleter The tab completer
         * @return The built command
         */
        public SimpleCommand build(CommandExecutor executor, TabCompleter tabCompleter) {
            BiFunction<CommandSender, String[], Boolean> executionHandler = (sender, args) -> {
                CommandContext context = new CommandContext(sender, name, args);
                return executor.execute(context);
            };
            
            BiFunction<CommandSender, String[], List<String>> tabCompletionHandler = null;
            if (tabCompleter != null) {
                tabCompletionHandler = (sender, args) -> {
                    CommandContext context = new CommandContext(sender, name, args);
                    return tabCompleter.tabComplete(sender, args);
                };
            }
            
            return new SimpleCommand(name, description, usage, aliases, permission, moduleId, parent,
                executionHandler, tabCompletionHandler, minArgs, maxArgs, category, detailedHelp, examples, hidden, cooldown);
        }
    }
} 