package com.essentialscore.api.command;

import com.essentialscore.api.CommandDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages command registration, execution, and delegation.
 */
public class CommandManager {
    private static final Logger LOGGER = Logger.getLogger(CommandManager.class.getName());
    
    private final Plugin plugin;
    private final Map<String, Command> registeredCommands;
    private final Map<String, Set<String>> moduleCommands;
    private final Map<String, Long> commandCooldowns;
    private final Map<String, HelpTopic> helpTopics;
    private CommandMap bukkitCommandMap;
    
    /**
     * Creates a new command manager.
     *
     * @param plugin The plugin
     */
    public CommandManager(Plugin plugin) {
        this.plugin = plugin;
        this.registeredCommands = new ConcurrentHashMap<>();
        this.moduleCommands = new ConcurrentHashMap<>();
        this.commandCooldowns = new ConcurrentHashMap<>();
        this.helpTopics = new ConcurrentHashMap<>();
        
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            bukkitCommandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to access Bukkit command map", e);
        }
    }
    
    /**
     * Registers a command.
     *
     * @param command The command to register
     * @return true if the command was registered successfully
     */
    public boolean registerCommand(Command command) {
        if (command == null) return false;
        
        // Don't register sub-commands directly
        if (command.getParent() != null) {
            LOGGER.warning("Attempted to register a sub-command directly: " + command.getName());
            return false;
        }
        
        String name = command.getName().toLowerCase();
        
        // Check if the command is already registered
        if (registeredCommands.containsKey(name)) {
            LOGGER.warning("Command already registered: " + name);
            return false;
        }
        
        // Register the command with Bukkit
        BukkitCommand bukkitCommand = new BukkitCommandWrapper(command, this);
        bukkitCommandMap.register(plugin.getName(), bukkitCommand);
        
        // Register the command internally
        registeredCommands.put(name, command);
        
        // Register command aliases
        for (String alias : command.getAliases()) {
            String aliasLower = alias.toLowerCase();
            if (!registeredCommands.containsKey(aliasLower)) {
                registeredCommands.put(aliasLower, command);
            }
        }
        
        // Track commands by module
        String moduleId = command.getModuleId();
        moduleCommands.computeIfAbsent(moduleId, k -> new HashSet<>()).add(name);
        
        // Create help topic
        helpTopics.put(name, new HelpTopic(command));
        
        LOGGER.info("Registered command: " + name + " from module " + moduleId);
        return true;
    }
    
    /**
     * Unregisters a command.
     *
     * @param command The command to unregister
     * @return true if the command was unregistered successfully
     */
    public boolean unregisterCommand(Command command) {
        if (command == null) return false;
        
        String name = command.getName().toLowerCase();
        
        // Check if the command is registered
        if (!registeredCommands.containsKey(name)) {
            LOGGER.warning("Command not registered: " + name);
            return false;
        }
        
        // Unregister the command from Bukkit
        try {
            Field knownCommandsField = bukkitCommandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(bukkitCommandMap);
            
            knownCommands.remove(name);
            for (String alias : command.getAliases()) {
                knownCommands.remove(alias.toLowerCase());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to unregister command from Bukkit", e);
        }
        
        // Unregister the command internally
        registeredCommands.remove(name);
        
        // Unregister command aliases
        for (String alias : command.getAliases()) {
            registeredCommands.remove(alias.toLowerCase());
        }
        
        // Untrack command by module
        String moduleId = command.getModuleId();
        if (moduleCommands.containsKey(moduleId)) {
            moduleCommands.get(moduleId).remove(name);
            if (moduleCommands.get(moduleId).isEmpty()) {
                moduleCommands.remove(moduleId);
            }
        }
        
        // Remove help topic
        helpTopics.remove(name);
        
        LOGGER.info("Unregistered command: " + name + " from module " + moduleId);
        return true;
    }
    
    /**
     * Unregisters all commands from a module.
     *
     * @param moduleId The module ID
     * @return The number of commands unregistered
     */
    public int unregisterModuleCommands(String moduleId) {
        if (moduleId == null || !moduleCommands.containsKey(moduleId)) {
            return 0;
        }
        
        Set<String> commands = new HashSet<>(moduleCommands.get(moduleId));
        int count = 0;
        
        for (String name : commands) {
            Command command = registeredCommands.get(name);
            if (command != null && unregisterCommand(command)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Registers a legacy command.
     *
     * @param commandDef The legacy command definition
     * @return The registered command
     */
    public Command registerLegacyCommand(CommandDefinition commandDef) {
        if (commandDef == null) return null;
        
        // Create a command from the legacy definition
        SimpleCommand command = SimpleCommand.builder(commandDef.getName(), commandDef.getModuleName())
            .description(commandDef.getDescription())
            .usage(commandDef.getUsage())
            .aliases(commandDef.getAliases())
            .permission(commandDef.getPermission())
            .build((context) -> {
                // Delegate to the legacy command
                return commandDef.execute(
                    context.getSender(),
                    context.getLabel(),
                    context.getRawArgs()
                );
            }, (sender, args) -> {
                // Delegate to the legacy tab completion
                return commandDef.tabComplete(sender, args);
            });
        
        // Register the command
        registerCommand(command);
        
        return command;
    }
    
    /**
     * Registers multiple legacy commands.
     *
     * @param commandDefs The legacy command definitions
     * @return The number of commands registered
     */
    public int registerLegacyCommands(List<CommandDefinition> commandDefs) {
        if (commandDefs == null) return 0;
        
        int count = 0;
        for (CommandDefinition commandDef : commandDefs) {
            if (registerLegacyCommand(commandDef) != null) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Gets a registered command by name.
     *
     * @param name The command name
     * @return The command, or null if not found
     */
    public Command getCommand(String name) {
        return name != null ? registeredCommands.get(name.toLowerCase()) : null;
    }
    
    /**
     * Gets all registered commands.
     *
     * @return An unmodifiable collection of commands
     */
    public List<Command> getCommands() {
        return new ArrayList<>(new HashSet<>(registeredCommands.values()));
    }
    
    /**
     * Gets all commands registered by a module.
     *
     * @param moduleId The module ID
     * @return A list of commands
     */
    public List<Command> getModuleCommands(String moduleId) {
        if (moduleId == null || !moduleCommands.containsKey(moduleId)) {
            return Collections.emptyList();
        }
        
        return moduleCommands.get(moduleId).stream()
            .map(registeredCommands::get)
            .collect(Collectors.toList());
    }
    
    /**
     * Executes a command.
     *
     * @param sender The command sender
     * @param commandLabel The command label
     * @param args The command arguments
     * @return true if the command was executed successfully
     */
    public boolean executeCommand(CommandSender sender, String commandLabel, String[] args) {
        Command command = getCommand(commandLabel);
        if (command == null) {
            return false;
        }
        
        // Check permission
        String permission = command.getPermission();
        if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        // Check cooldown
        if (sender instanceof Player && command.getCooldown() > 0) {
            Player player = (Player) sender;
            String cooldownKey = player.getUniqueId() + ":" + command.getName();
            
            long now = System.currentTimeMillis();
            Long lastUsed = commandCooldowns.get(cooldownKey);
            
            if (lastUsed != null) {
                long cooldownTime = command.getCooldown() * 1000L;
                long elapsed = now - lastUsed;
                
                if (elapsed < cooldownTime) {
                    long remaining = (cooldownTime - elapsed) / 1000;
                    sender.sendMessage("§cYou must wait " + remaining + " seconds before using this command again.");
                    return true;
                }
            }
            
            // Update cooldown
            commandCooldowns.put(cooldownKey, now);
        }
        
        // Create context and execute
        CommandContext context = new CommandContext(sender, commandLabel, args);
        return command.execute(context);
    }
    
    /**
     * Gets tab completions for a command.
     *
     * @param sender The command sender
     * @param commandLabel The command label
     * @param args The command arguments
     * @return A list of tab completions
     */
    public List<String> tabCompleteCommand(CommandSender sender, String commandLabel, String[] args) {
        Command command = getCommand(commandLabel);
        if (command == null) {
            return Collections.emptyList();
        }
        
        // Check permission
        String permission = command.getPermission();
        if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        
        return command.tabComplete(sender, args);
    }
    
    /**
     * Gets help information for a command.
     *
     * @param name The command name
     * @return The help topic, or null if not found
     */
    public HelpTopic getHelpTopic(String name) {
        return name != null ? helpTopics.get(name.toLowerCase()) : null;
    }
    
    /**
     * Gets all help topics.
     *
     * @return An unmodifiable collection of help topics
     */
    public List<HelpTopic> getHelpTopics() {
        return new ArrayList<>(helpTopics.values());
    }
    
    /**
     * Gets help topics by category.
     *
     * @param category The category
     * @return A list of help topics in the category
     */
    public List<HelpTopic> getHelpTopicsByCategory(String category) {
        if (category == null) {
            return Collections.emptyList();
        }
        
        return helpTopics.values().stream()
            .filter(topic -> category.equalsIgnoreCase(topic.getCategory()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all help categories.
     *
     * @return A list of categories
     */
    public List<String> getHelpCategories() {
        return helpTopics.values().stream()
            .map(HelpTopic::getCategory)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Represents a help topic for a command.
     */
    public static class HelpTopic {
        private final String name;
        private final String description;
        private final String usage;
        private final String permission;
        private final String category;
        private final String detailedHelp;
        private final List<String> examples;
        private final List<HelpTopic> subTopics;
        
        /**
         * Creates a help topic from a command.
         *
         * @param command The command
         */
        public HelpTopic(Command command) {
            this.name = command.getName();
            this.description = command.getDescription();
            this.usage = command instanceof AbstractCommand
                ? ((AbstractCommand) command).getFullUsage()
                : "/" + command.getName() + " " + command.getUsage();
            this.permission = command.getPermission();
            this.category = command.getCategory();
            this.detailedHelp = command.getDetailedHelp();
            this.examples = command.getExamples();
            
            // Create sub-topics for sub-commands
            this.subTopics = command.getSubCommands().stream()
                .filter(sub -> !sub.isHidden())
                .map(HelpTopic::new)
                .collect(Collectors.toList());
        }
        
        /**
         * Gets the topic name.
         *
         * @return The name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Gets the topic description.
         *
         * @return The description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the command usage.
         *
         * @return The usage
         */
        public String getUsage() {
            return usage;
        }
        
        /**
         * Gets the permission required to use the command.
         *
         * @return The permission
         */
        public String getPermission() {
            return permission;
        }
        
        /**
         * Gets the topic category.
         *
         * @return The category
         */
        public String getCategory() {
            return category;
        }
        
        /**
         * Gets the detailed help text.
         *
         * @return The detailed help
         */
        public String getDetailedHelp() {
            return detailedHelp;
        }
        
        /**
         * Gets the command examples.
         *
         * @return The examples
         */
        public List<String> getExamples() {
            return examples;
        }
        
        /**
         * Gets the sub-topics.
         *
         * @return The sub-topics
         */
        public List<HelpTopic> getSubTopics() {
            return subTopics;
        }
        
        /**
         * Formats the help text for display.
         *
         * @return The formatted help text
         */
        public String format() {
            StringBuilder builder = new StringBuilder();
            
            builder.append("§6§l").append(name).append("§r§7: §f").append(description).append("\n");
            builder.append("§7Usage: §f").append(usage).append("\n");
            
            if (permission != null && !permission.isEmpty()) {
                builder.append("§7Permission: §f").append(permission).append("\n");
            }
            
            if (detailedHelp != null && !detailedHelp.isEmpty()) {
                builder.append("\n§7").append(detailedHelp).append("\n");
            }
            
            if (examples != null && !examples.isEmpty()) {
                builder.append("\n§7Examples:§f\n");
                for (String example : examples) {
                    builder.append("  ").append(example).append("\n");
                }
            }
            
            if (!subTopics.isEmpty()) {
                builder.append("\n§7Sub-commands:§f\n");
                for (HelpTopic subTopic : subTopics) {
                    builder.append("  §6").append(subTopic.getName()).append("§f: §7").append(subTopic.getDescription()).append("\n");
                }
            }
            
            return builder.toString();
        }
    }
    
    /**
     * A wrapper for Bukkit commands to use our command framework.
     */
    private static class BukkitCommandWrapper extends BukkitCommand {
        private final Command command;
        private final CommandManager manager;
        
        /**
         * Creates a new Bukkit command wrapper.
         *
         * @param command The command to wrap
         * @param manager The command manager
         */
        public BukkitCommandWrapper(Command command, CommandManager manager) {
            super(command.getName(), command.getDescription(), command.getUsage(), command.getAliases());
            
            this.command = command;
            this.manager = manager;
            
            if (command.getPermission() != null && !command.getPermission().isEmpty()) {
                setPermission(command.getPermission());
            }
        }
        
        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return manager.executeCommand(sender, commandLabel, args);
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
            return manager.tabCompleteCommand(sender, alias, args);
        }
    }
} 