package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes and registers annotation-based commands.
 */
public class CommandProcessor {
    private static final Logger LOGGER = Logger.getLogger(CommandProcessor.class.getName());
    
    @SuppressWarnings("unused")
    private final Plugin plugin;
    private final CommandManager commandManager;
    private final Map<String, Object> commandObjects;
    
    /**
     * Creates a new command processor.
     *
     * @param plugin The plugin
     * @param commandManager The command manager
     */
    public CommandProcessor(Plugin plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.commandObjects = new HashMap<>();
    }
    
    /**
     * Registers a class with command methods.
     *
     * @param moduleId The module ID
     * @param commandObject The object containing command methods
     * @return The number of commands registered
     */
    public int registerCommandClass(String moduleId, Object commandObject) {
        if (commandObject == null) return 0;
        
        Class<?> clazz = commandObject.getClass();
        int count = 0;
        
        // Look for command methods
        for (Method method : clazz.getDeclaredMethods()) {
            RegisterCommand annotation = method.getAnnotation(RegisterCommand.class);
            if (annotation != null) {
                Command command = registerCommandMethod(moduleId, commandObject, method, annotation);
                if (command != null) {
                    count++;
                }
            }
        }
        
        if (count > 0) {
            commandObjects.put(moduleId, commandObject);
        }
        
        return count;
    }
    
    /**
     * Unregisters commands from a module.
     *
     * @param moduleId The module ID
     * @return The number of commands unregistered
     */
    public int unregisterModuleCommands(String moduleId) {
        if (moduleId == null) return 0;
        
        int count = commandManager.unregisterModuleCommands(moduleId);
        commandObjects.remove(moduleId);
        
        return count;
    }
    
    /**
     * Registers a command method.
     *
     * @param moduleId The module ID
     * @param commandObject The object containing the method
     * @param method The command method
     * @param annotation The command annotation
     * @return The registered command, or null if registration failed
     */
    private Command registerCommandMethod(String moduleId, Object commandObject, Method method, RegisterCommand annotation) {
        final String name = annotation.name().isEmpty() ? method.getName() : annotation.name();
        
        // Create a simple command
        SimpleCommand.Builder builder = SimpleCommand.builder(name, moduleId)
            .description(annotation.description())
            .usage(annotation.usage())
            .aliases(Arrays.asList(annotation.aliases()))
            .permission(annotation.permission())
            .minArgs(annotation.minArgs())
            .maxArgs(annotation.maxArgs())
            .category(annotation.category());
        
        // Set detailed help if specified
        if (!annotation.detailedHelp().isEmpty()) {
            builder.detailedHelp(annotation.detailedHelp());
        }
        
        // Set examples if specified
        if (annotation.examples().length > 0) {
            builder.examples(Arrays.asList(annotation.examples()));
        }
        
        // Set hidden and cooldown
        builder.hidden(annotation.hidden());
        builder.cooldown(annotation.cooldown());
        
        // Check method parameters
        Class<?>[] paramTypes = method.getParameterTypes();
        final boolean usesContext = paramTypes.length == 1 && CommandContext.class.isAssignableFrom(paramTypes[0]);
        final boolean usesLegacy = paramTypes.length == 3 && 
                             CommandSender.class.isAssignableFrom(paramTypes[0]) && 
                             String.class.isAssignableFrom(paramTypes[1]) && 
                             String[].class.isAssignableFrom(paramTypes[2]);
        
        if (!usesContext && !usesLegacy) {
            LOGGER.warning("Invalid command method signature: " + method);
            return null;
        }
        
        // Create the command with an executor that calls the method
        SimpleCommand command = builder.build(context -> {
            try {
                if (usesContext) {
                    return (boolean) method.invoke(commandObject, context);
                } else {
                    return (boolean) method.invoke(commandObject, context.getSender(), context.getLabel(), context.getRawArgs());
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error executing command: " + name, e);
                context.getSender().sendMessage("§cAn error occurred while executing this command.");
                return false;
            }
        });
        
        // Register the command
        if (commandManager.registerCommand(command)) {
            return command;
        }
        
        return null;
    }
    
    /**
     * Gets the command object for a module.
     *
     * @param moduleId The module ID
     * @return The command object, or null if not found
     */
    public Object getCommandObject(String moduleId) {
        return commandObjects.get(moduleId);
    }
    
    /**
     * Gets all registered command objects.
     *
     * @return A map of module IDs to command objects
     */
    public Map<String, Object> getCommandObjects() {
        return Collections.unmodifiableMap(commandObjects);
    }
    
    /**
     * Registers a specific method as a command.
     *
     * @param moduleId The module ID
     * @param commandObject The object containing the method
     * @param methodName The method name
     * @return The registered command, or null if registration failed
     */
    public Command registerCommandMethod(String moduleId, Object commandObject, String methodName) {
        if (commandObject == null || methodName == null) return null;
        
        try {
            // Find the method
            Method method = null;
            for (Method m : commandObject.getClass().getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    method = m;
                    break;
                }
            }
            
            if (method == null) {
                LOGGER.warning("Method not found: " + methodName);
                return null;
            }
            
            // Check if the method has the annotation
            RegisterCommand annotation = method.getAnnotation(RegisterCommand.class);
            if (annotation != null) {
                return registerCommandMethod(moduleId, commandObject, method, annotation);
            }
            
            // Create a default annotation
            final String finalMethodName = methodName;
            RegisterCommand defaultAnnotation = new RegisterCommand() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return RegisterCommand.class;
                }
                
                @Override
                public String name() {
                    return finalMethodName;
                }
                
                @Override
                public String description() {
                    return "";
                }
                
                @Override
                public String usage() {
                    return "";
                }
                
                @Override
                public String[] aliases() {
                    return new String[0];
                }
                
                @Override
                public String permission() {
                    return "";
                }
                
                @Override
                public int minArgs() {
                    return 0;
                }
                
                @Override
                public int maxArgs() {
                    return -1;
                }
                
                @Override
                public String category() {
                    return "General";
                }
                
                @Override
                public String detailedHelp() {
                    return "";
                }
                
                @Override
                public String[] examples() {
                    return new String[0];
                }
                
                @Override
                public boolean hidden() {
                    return false;
                }
                
                @Override
                public int cooldown() {
                    return 0;
                }
            };
            
            return registerCommandMethod(moduleId, commandObject, method, defaultAnnotation);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error registering command method: " + methodName, e);
            return null;
        }
    }
    
    /**
     * Command annotation for registering methods as commands.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface RegisterCommand {
        /**
         * The command name.
         * If empty, the method name will be used.
         *
         * @return The command name
         */
        String name() default "";
        
        /**
         * The command description.
         *
         * @return The command description
         */
        String description() default "";
        
        /**
         * The command usage.
         *
         * @return The command usage
         */
        String usage() default "";
        
        /**
         * The command aliases.
         *
         * @return The command aliases
         */
        String[] aliases() default {};
        
        /**
         * The permission required to use the command.
         *
         * @return The permission
         */
        String permission() default "";
        
        /**
         * The minimum number of arguments required.
         *
         * @return The minimum arguments
         */
        int minArgs() default 0;
        
        /**
         * The maximum number of arguments allowed.
         *
         * @return The maximum arguments, or -1 for unlimited
         */
        int maxArgs() default -1;
        
        /**
         * The command category.
         *
         * @return The category
         */
        String category() default "General";
        
        /**
         * Detailed help information.
         *
         * @return The detailed help
         */
        String detailedHelp() default "";
        
        /**
         * Examples of how to use the command.
         *
         * @return The examples
         */
        String[] examples() default {};
        
        /**
         * Whether the command is hidden from help listings.
         *
         * @return true if the command is hidden
         */
        boolean hidden() default false;
        
        /**
         * The cooldown time in seconds.
         *
         * @return The cooldown time
         */
        int cooldown() default 0;
    }
    
    /**
     * SimpleCommand implementation.
     */
    public static class SimpleCommand implements Command {
        private final String name;
        private final String moduleId;
        private final String description;
        private final String usage;
        private final List<String> aliases;
        private final String permission;
        private final int minArgs;
        private final int maxArgs;
        private final String category;
        private final String detailedHelp;
        private final List<String> examples;
        private final boolean hidden;
        private final int cooldown;
        private final CommandExecutor executor;
        private Command parent;
        private final Map<String, Command> subCommands = new HashMap<>();
        
        private SimpleCommand(Builder builder) {
            this.name = builder.name;
            this.moduleId = builder.moduleId;
            this.description = builder.description;
            this.usage = builder.usage;
            this.aliases = builder.aliases;
            this.permission = builder.permission;
            this.minArgs = builder.minArgs;
            this.maxArgs = builder.maxArgs;
            this.category = builder.category;
            this.detailedHelp = builder.detailedHelp;
            this.examples = builder.examples;
            this.hidden = builder.hidden;
            this.cooldown = builder.cooldown;
            this.executor = builder.executor;
            this.parent = null;
        }
        
        public static Builder builder(String name, String moduleId) {
            return new Builder(name, moduleId);
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getModuleId() {
            return moduleId;
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
        
        @Override
        public String getPermission() {
            return permission;
        }
        
        @Override
        public int getMinArgs() {
            return minArgs;
        }
        
        @Override
        public int getMaxArgs() {
            return maxArgs;
        }
        
        @Override
        public String getCategory() {
            return category;
        }
        
        @Override
        public String getDetailedHelp() {
            return detailedHelp;
        }
        
        @Override
        public List<String> getExamples() {
            return examples;
        }
        
        @Override
        public boolean isHidden() {
            return hidden;
        }
        
        @Override
        public int getCooldown() {
            return cooldown;
        }
        
        @Override
        public boolean execute(CommandContext context) {
            return executor.execute(context);
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length > 0 && !subCommands.isEmpty()) {
                String subCommandName = args[0].toLowerCase();
                Command subCommand = getSubCommand(subCommandName);
                
                if (subCommand != null && args.length > 1) {
                    // Forward tab completion to sub-command
                    String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                    return subCommand.tabComplete(sender, subArgs);
                } else if (args.length == 1) {
                    // Suggest sub-commands
                    List<String> suggestions = new ArrayList<>();
                    String prefix = args[0].toLowerCase();
                    
                    for (String name : subCommands.keySet()) {
                        if (name.toLowerCase().startsWith(prefix)) {
                            suggestions.add(name);
                        }
                    }
                    
                    return suggestions;
                }
            }
            
            return Collections.emptyList();
        }
        
        @Override
        public Command getParent() {
            return parent;
        }
        
        @Override
        public void addSubCommand(Command command) {
            if (command != null) {
                subCommands.put(command.getName().toLowerCase(), command);
                // Set parent relationship
                try {
                    // Use reflection to set parent if needed
                    if (command instanceof SimpleCommand) {
                        ((SimpleCommand) command).parent = this;
                    }
                } catch (Exception e) {
                    // Ignore if we can't set parent
                }
            }
        }
        
        @Override
        public boolean removeSubCommand(Command command) {
            if (command != null) {
                return subCommands.remove(command.getName().toLowerCase()) != null;
            }
            return false;
        }
        
        @Override
        public Command getSubCommand(String name) {
            return name != null ? subCommands.get(name.toLowerCase()) : null;
        }
        
        @Override
        public List<Command> getSubCommands() {
            return new ArrayList<>(subCommands.values());
        }
        
        /**
         * Builder for SimpleCommand.
         */
        public static class Builder {
            private final String name;
            private final String moduleId;
            private String description = "";
            private String usage = "";
            private List<String> aliases = new ArrayList<>();
            private String permission = "";
            private int minArgs = 0;
            private int maxArgs = -1;
            private String category = "General";
            private String detailedHelp = "";
            private List<String> examples = new ArrayList<>();
            private boolean hidden = false;
            private int cooldown = 0;
            private CommandExecutor executor;
            
            private Builder(String name, String moduleId) {
                this.name = name;
                this.moduleId = moduleId;
            }
            
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            public Builder usage(String usage) {
                this.usage = usage;
                return this;
            }
            
            public Builder aliases(List<String> aliases) {
                this.aliases = aliases;
                return this;
            }
            
            public Builder permission(String permission) {
                this.permission = permission;
                return this;
            }
            
            public Builder minArgs(int minArgs) {
                this.minArgs = minArgs;
                return this;
            }
            
            public Builder maxArgs(int maxArgs) {
                this.maxArgs = maxArgs;
                return this;
            }
            
            public Builder category(String category) {
                this.category = category;
                return this;
            }
            
            public Builder detailedHelp(String detailedHelp) {
                this.detailedHelp = detailedHelp;
                return this;
            }
            
            public Builder examples(List<String> examples) {
                this.examples = examples;
                return this;
            }
            
            public Builder hidden(boolean hidden) {
                this.hidden = hidden;
                return this;
            }
            
            public Builder cooldown(int cooldown) {
                this.cooldown = cooldown;
                return this;
            }
            
            public SimpleCommand build(CommandExecutor executor) {
                this.executor = executor;
                return new SimpleCommand(this);
            }
        }
    }
    
    /**
     * Interface for command executors.
     */
    public interface CommandExecutor {
        boolean execute(CommandContext context);
    }
} 
