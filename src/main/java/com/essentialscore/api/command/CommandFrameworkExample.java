package com.essentialscore.api.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Example demonstrating how to use the advanced command framework.
 */
public class CommandFrameworkExample {
    
    private final Plugin plugin;
    private final CommandManager commandManager;
    private final CommandProcessor commandProcessor;
    
    /**
     * Creates a new command framework example.
     *
     * @param plugin The plugin
     */
    public CommandFrameworkExample(Plugin plugin) {
        this.plugin = plugin;
        this.commandManager = new CommandManager(plugin);
        this.commandProcessor = new CommandProcessor(plugin, commandManager);
        
        // Register the help command
        commandManager.registerCommand(new HelpCommand("core", commandManager));
        
        // Register methods with annotations
        commandProcessor.registerCommandClass("example", this);
        
        // Register a command manually
        SimpleCommand testCommand = CommandUtil.createCommandBuilder("test", "example")
            .description("A test command")
            .usage("[args...]")
            .aliases(Collections.singletonList("t"))
            .permission("example.test")
            .category("Examples")
            .build(context -> {
                CommandSender sender = context.getSender();
                sender.sendMessage(Component.text("Test command executed!", NamedTextColor.GREEN));
                
                if (context.getParsedArgs().size() > 0) {
                    sender.sendMessage(Component.text("Arguments: ", NamedTextColor.YELLOW)
                                     .append(Component.text(String.join(", ", context.getParsedArgs().getAll()))));
                }
                
                return true;
            });
        
        commandManager.registerCommand(testCommand);
        
        // Create a command with sub-commands
        SimpleCommand parentCommand = CommandUtil.createCommandBuilder("parent", "example")
            .description("A parent command with sub-commands")
            .usage("<sub-command> [args...]")
            .aliases(Collections.singletonList("p"))
            .permission("example.parent")
            .category("Examples")
            .build(context -> {
                // This will be called if no sub-command is specified
                if (context.getParsedArgs().isEmpty()) {
                    context.getSender().sendMessage(Component.text("Please specify a sub-command.", NamedTextColor.RED));
                    context.getSender().sendMessage(Component.text("Available sub-commands: ", NamedTextColor.YELLOW)
                                                 .append(Component.text("sub1, sub2", NamedTextColor.WHITE)));
                    return true;
                }
                
                // The AbstractCommand will handle sub-command routing
                return false;
            });
        
        // Create sub-commands
        SimpleCommand subCommand1 = CommandUtil.createCommandBuilder("sub1", "example")
            .description("Sub-command 1")
            .usage("[arg]")
            .parent(parentCommand)
            .permission("example.parent.sub1")
            .build(context -> {
                context.getSender().sendMessage(Component.text("Sub-command 1 executed!", NamedTextColor.GREEN));
                
                if (context.getParsedArgs().size() > 0) {
                    context.getSender().sendMessage(Component.text("Argument: ", NamedTextColor.YELLOW)
                                                 .append(Component.text(context.getParsedArgs().get(0))));
                }
                
                return true;
            });
        
        SimpleCommand subCommand2 = CommandUtil.createCommandBuilder("sub2", "example")
            .description("Sub-command 2")
            .usage("[arg]")
            .parent(parentCommand)
            .permission("example.parent.sub2")
            .build(context -> {
                context.getSender().sendMessage(Component.text("Sub-command 2 executed!", NamedTextColor.GREEN));
                
                if (context.getParsedArgs().size() > 0) {
                    context.getSender().sendMessage(Component.text("Argument: ", NamedTextColor.YELLOW)
                                                 .append(Component.text(context.getParsedArgs().get(0))));
                }
                
                return true;
            });
        
        // Add sub-commands to parent
        parentCommand.addSubCommand(subCommand1);
        parentCommand.addSubCommand(subCommand2);
        
        // Register the parent command
        commandManager.registerCommand(parentCommand);
        
        plugin.getLogger().info("Command framework example initialized with " + 
                             commandManager.getCommands().size() + " commands");
    }
    
    /**
     * Example command using annotations.
     *
     * @param context The command context
     * @return true if the command was executed successfully
     */
    @CommandProcessor.RegisterCommand(
        name = "example",
        description = "Example command using annotations",
        usage = "[arg]",
        aliases = {"ex", "demo"},
        permission = "example.command",
        category = "Examples",
        detailedHelp = "This command demonstrates how to use annotations to register commands.",
        examples = {"/example", "/example arg"},
        cooldown = 5
    )
    public boolean exampleCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        sender.sendMessage(Component.text("Example command executed!", NamedTextColor.GREEN));
        String[] args = context.getArgs();
        if (args.length > 0) {
            sender.sendMessage(Component.text("Argument: ", NamedTextColor.YELLOW)
                .append(Component.text(args[0], NamedTextColor.WHITE)));
        }
        return true;
    }
    
    /**
     * Example command with tab completion.
     *
     * @param sender The command sender
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was executed successfully
     */
    @CommandProcessor.RegisterCommand(
        name = "complete",
        description = "Example command with tab completion",
        usage = "<option>",
        permission = "example.complete",
        category = "Examples",
        minArgs = 1,
        maxArgs = 1
    )
    public boolean completeCommand(CommandSender sender, String label, String[] args) {
        String option = args[0];
        List<String> options = Arrays.asList("option1", "option2", "option3");
        if (options.contains(option)) {
            sender.sendMessage(Component.text("Selected option: ", NamedTextColor.GREEN)
                .append(Component.text(option, NamedTextColor.WHITE)));
        } else {
            sender.sendMessage(Component.text("Invalid option. Available options: ", NamedTextColor.RED)
                .append(Component.text(String.join(", ", options), NamedTextColor.YELLOW)));
        }
        return true;
    }
    
    /**
     * Tab completion for the complete command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return List of tab completion options
     */
    public List<String> completeTab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("option1", "option2", "option3");
            String arg = args[0].toLowerCase();
            
            return options.stream()
                .filter(option -> option.startsWith(arg))
                .collect(java.util.stream.Collectors.toList());
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Example command that's only available to players.
     *
     * @param context The command context
     * @return true if the command was executed successfully
     */
    @CommandProcessor.RegisterCommand(
        name = "player",
        description = "Example command for players only",
        permission = "example.player",
        category = "Examples"
    )
    public boolean playerCommand(CommandContext context) {
        if (!context.isPlayer()) {
            context.getSender().sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        Player player = context.getPlayer().get();
        player.sendMessage(Component.text("Hello, " + player.getName() + "!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Your location: ", NamedTextColor.YELLOW)
            .append(Component.text(player.getLocation().getBlockX() + ", " +
                player.getLocation().getBlockY() + ", " +
                player.getLocation().getBlockZ(), NamedTextColor.WHITE)));
        return true;
    }
    
    /**
     * Unregisters all commands.
     */
    public void unregisterAll() {
        commandProcessor.unregisterModuleCommands("example");
        commandManager.unregisterModuleCommands("core");
        
        plugin.getLogger().info("Command framework example unregistered all commands");
    }
}