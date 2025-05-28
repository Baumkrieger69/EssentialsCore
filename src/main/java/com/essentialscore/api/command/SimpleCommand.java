package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;

<<<<<<< HEAD
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
=======
import java.util.List;

/**
 * A simple implementation of the Command interface.
 * This class is designed for creating commands with straightforward execution logic.
 */
public class SimpleCommand extends AbstractCommand {
    
    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
    
    /**
     * Creates a new simple command.
     *
<<<<<<< HEAD
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
=======
     * @param builder The command builder
     * @param executor The command executor
     */
    public SimpleCommand(Builder builder, CommandExecutor executor) {
        this(builder, executor, null);
    }
    
    /**
     * Creates a new simple command with a tab completer.
     *
     * @param builder The command builder
     * @param executor The command executor
     * @param tabCompleter The tab completer
     */
    public SimpleCommand(Builder builder, CommandExecutor executor, TabCompleter tabCompleter) {
        super(builder.getName(), builder.getDescription(), builder.getUsage(), builder.getAliases(),
              builder.getPermission(), builder.getModuleId(), builder.getParent(), builder.getMinArgs(),
              builder.getMaxArgs(), builder.getCategory(), builder.getDetailedHelp(), builder.getExamples(),
              builder.isHidden(), builder.getCooldown());
        
        this.executor = executor != null ? executor : context -> false;
        this.tabCompleter = tabCompleter;
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
    }
    
    @Override
    public boolean execute(CommandContext context) {
<<<<<<< HEAD
        if (executionHandler == null) {
            return false;
        }
        
        CommandSender sender = context.getSender();
        String[] args = context.getParsedArgs().getAllAsArray();
        
        return executionHandler.apply(sender, args);
=======
        // Check for sub-commands first
        if (context.getArgs().size() > 0) {
            Command subCommand = getSubCommand(context.getArgs().get(0));
            if (subCommand != null && hasPermissionForCommand(context.getSender(), subCommand)) {
                String[] subArgs = new String[context.getRawArgs().length - 1];
                System.arraycopy(context.getRawArgs(), 1, subArgs, 0, subArgs.length);
                
                CommandContext subContext = new CommandContext(
                    context.getSender(), 
                    subCommand.getName(), 
                    subArgs
                );
                
                // Copy metadata to the sub-context
                Object moduleName = context.getMetadata("moduleName");
                if (moduleName != null) {
                    subContext.setMetadata("moduleName", moduleName);
                }
                
                return subCommand.execute(subContext);
            }
        }
        
        // Execute the command
        return executor.execute(context);
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
<<<<<<< HEAD
        if (tabCompletionHandler == null) {
            return new ArrayList<>();
        }
        
        return tabCompletionHandler.apply(sender, args);
=======
        // Try to delegate to the tab completer
        if (tabCompleter != null) {
            List<String> completions = tabCompleter.tabComplete(sender, args);
            if (completions != null) {
                return completions;
            }
        }
        
        // Fall back to the default tab completion
        return super.tabComplete(sender, args);
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
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
<<<<<<< HEAD
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
        
=======
     * Creates a new simple command builder.
     *
     * @param name The command name
     * @param moduleId The module ID that owns this command
     * @return The command builder
     */
    public static Builder builder(String name, String moduleId) {
        return new Builder(name, moduleId);
    }
    
    /**
     * Builder for creating simple commands.
     */
    public static class Builder extends AbstractCommand.Builder {
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        /**
         * Creates a new builder.
         *
         * @param name The command name
<<<<<<< HEAD
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
=======
         * @param moduleId The module ID that owns this command
         */
        public Builder(String name, String moduleId) {
            super(name, moduleId);
        }
        
        /**
         * Builds a simple command with an executor.
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
         *
         * @param executor The command executor
         * @return The built command
         */
        public SimpleCommand build(CommandExecutor executor) {
<<<<<<< HEAD
            BiFunction<CommandSender, String[], Boolean> executionHandler = (sender, args) -> {
                CommandContext context = new CommandContext(sender, name, args);
                return executor.execute(context);
            };
            
            return new SimpleCommand(name, description, usage, aliases, permission, moduleId, parent,
                executionHandler, null, minArgs, maxArgs, category, detailedHelp, examples, hidden, cooldown);
        }
        
        /**
         * Builds the command with the specified executor and tab completer.
=======
            return new SimpleCommand(this, executor);
        }
        
        /**
         * Builds a simple command with an executor and tab completer.
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
         *
         * @param executor The command executor
         * @param tabCompleter The tab completer
         * @return The built command
         */
        public SimpleCommand build(CommandExecutor executor, TabCompleter tabCompleter) {
<<<<<<< HEAD
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
=======
            return new SimpleCommand(this, executor, tabCompleter);
        }
        
        // Override method from parent for proper return type
        @Override
        public Builder description(String description) {
            super.description(description);
            return this;
        }
        
        @Override
        public Builder usage(String usage) {
            super.usage(usage);
            return this;
        }
        
        @Override
        public Builder aliases(List<String> aliases) {
            super.aliases(aliases);
            return this;
        }
        
        @Override
        public Builder permission(String permission) {
            super.permission(permission);
            return this;
        }
        
        @Override
        public Builder parent(Command parent) {
            super.parent(parent);
            return this;
        }
        
        @Override
        public Builder minArgs(int minArgs) {
            super.minArgs(minArgs);
            return this;
        }
        
        @Override
        public Builder maxArgs(int maxArgs) {
            super.maxArgs(maxArgs);
            return this;
        }
        
        @Override
        public Builder category(String category) {
            super.category(category);
            return this;
        }
        
        @Override
        public Builder detailedHelp(String detailedHelp) {
            super.detailedHelp(detailedHelp);
            return this;
        }
        
        @Override
        public Builder examples(List<String> examples) {
            super.examples(examples);
            return this;
        }
        
        @Override
        public Builder hidden(boolean hidden) {
            super.hidden(hidden);
            return this;
        }
        
        @Override
        public Builder cooldown(int cooldown) {
            super.cooldown(cooldown);
            return this;
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        }
    }
} 