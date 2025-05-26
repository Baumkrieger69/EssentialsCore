package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * A simple implementation of the Command interface.
 * This class is designed for creating commands with straightforward execution logic.
 */
public class SimpleCommand extends AbstractCommand {
    
    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;
    
    /**
     * Creates a new simple command.
     *
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
    }
    
    @Override
    public boolean execute(CommandContext context) {
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
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // Try to delegate to the tab completer
        if (tabCompleter != null) {
            List<String> completions = tabCompleter.tabComplete(sender, args);
            if (completions != null) {
                return completions;
            }
        }
        
        // Fall back to the default tab completion
        return super.tabComplete(sender, args);
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
        /**
         * Creates a new builder.
         *
         * @param name The command name
         * @param moduleId The module ID that owns this command
         */
        public Builder(String name, String moduleId) {
            super(name, moduleId);
        }
        
        /**
         * Builds a simple command with an executor.
         *
         * @param executor The command executor
         * @return The built command
         */
        public SimpleCommand build(CommandExecutor executor) {
            return new SimpleCommand(this, executor);
        }
        
        /**
         * Builds a simple command with an executor and tab completer.
         *
         * @param executor The command executor
         * @param tabCompleter The tab completer
         * @return The built command
         */
        public SimpleCommand build(CommandExecutor executor, TabCompleter tabCompleter) {
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
        }
    }
} 