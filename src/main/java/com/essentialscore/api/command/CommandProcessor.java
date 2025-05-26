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
        String name = annotation.name();
        if (name.isEmpty()) {
            name = method.getName();
        }
        
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
        boolean usesContext = paramTypes.length == 1 && CommandContext.class.isAssignableFrom(paramTypes[0]);
        boolean usesLegacy = paramTypes.length == 3 && 
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
            RegisterCommand defaultAnnotation = new RegisterCommand() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return RegisterCommand.class;
                }
                
                @Override
                public String name() {
                    return methodName;
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
     * Annotation for registering command methods.
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
     * Example usage of the command processor.
     */
    public static class Example {
        /**
         * Example command method.
         *
         * @param context The command context
         * @return true if the command was executed successfully
         */
        @RegisterCommand(
            name = "example",
            description = "An example command",
            usage = "[arg]",
            aliases = {"ex", "test"},
            permission = "example.command",
            minArgs = 0,
            maxArgs = 1,
            category = "Examples",
            detailedHelp = "This is an example command that shows how to use the command processor.",
            examples = {"/example", "/example arg"},
            cooldown = 5
        )
        public boolean exampleCommand(CommandContext context) {
            context.getSender().sendMessage("Example command executed!");
            
            if (!context.getArgs().isEmpty()) {
                context.getSender().sendMessage("Argument: " + context.getArgs().get(0));
            }
            
            return true;
        }
    }
} 