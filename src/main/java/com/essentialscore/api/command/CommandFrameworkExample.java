package com.essentialscore.api.command;

<<<<<<< HEAD
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
=======
import org.bukkit.ChatColor;
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
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
<<<<<<< HEAD
        SimpleCommand testCommand = CommandUtil.createCommandBuilder("test", "example")
=======
        SimpleCommand testCommand = SimpleCommand.builder("test", "example")
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
            .description("A test command")
            .usage("[args...]")
            .aliases(Collections.singletonList("t"))
            .permission("example.test")
            .category("Examples")
            .build(context -> {
                CommandSender sender = context.getSender();
<<<<<<< HEAD
                sender.sendMessage(Component.text("Test command executed!", NamedTextColor.GREEN));
                
                if (context.getParsedArgs().size() > 0) {
                    sender.sendMessage(Component.text("Arguments: ", NamedTextColor.YELLOW)
                                       .append(Component.text(String.join(", ", context.getParsedArgs().getAll()))));
=======
                sender.sendMessage(ChatColor.GREEN + "Test command executed!");
                
                if (context.getArgs().size() > 0) {
                    sender.sendMessage(ChatColor.YELLOW + "Arguments: " + 
                                      String.join(", ", context.getArgs().getAll()));
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
                }
                
                return true;
            });
        
        commandManager.registerCommand(testCommand);
        
        // Create a command with sub-commands
<<<<<<< HEAD
        SimpleCommand parentCommand = CommandUtil.createCommandBuilder("parent", "example")
=======
        SimpleCommand parentCommand = SimpleCommand.builder("parent", "example")
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
            .description("A parent command with sub-commands")
            .usage("<sub-command> [args...]")
            .aliases(Collections.singletonList("p"))
            .permission("example.parent")
            .category("Examples")
            .build(context -> {
                // This will be called if no sub-command is specified
<<<<<<< HEAD
                if (context.getParsedArgs().isEmpty()) {
                    context.getSender().sendMessage(Component.text("Please specify a sub-command.", NamedTextColor.RED));
                    context.getSender().sendMessage(Component.text("Available sub-commands: ", NamedTextColor.YELLOW)
                                                   .append(Component.text("sub1, sub2", NamedTextColor.WHITE)));
=======
                if (context.getArgs().isEmpty()) {
                    context.getSender().sendMessage(ChatColor.RED + "Please specify a sub-command.");
                    context.getSender().sendMessage(ChatColor.YELLOW + "Available sub-commands: " + 
                                                  ChatColor.WHITE + "sub1, sub2");
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
                    return true;
                }
                
                // The AbstractCommand will handle sub-command routing
                return false;
            });
        
        // Create sub-commands
<<<<<<< HEAD
        SimpleCommand subCommand1 = CommandUtil.createCommandBuilder("sub1", "example")
=======
        SimpleCommand subCommand1 = SimpleCommand.builder("sub1", "example")
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
            .description("Sub-command 1")
            .usage("[arg]")
            .parent(parentCommand)
            .permission("example.parent.sub1")
            .build(context -> {
<<<<<<< HEAD
                context.getSender().sendMessage(Component.text("Sub-command 1 executed!", NamedTextColor.GREEN));
                
                if (context.getParsedArgs().size() > 0) {
                    context.getSender().sendMessage(Component.text("Argument: ", NamedTextColor.YELLOW)
                                                   .append(Component.text(context.getParsedArgs().get(0))));
=======
                context.getSender().sendMessage(ChatColor.GREEN + "Sub-command 1 executed!");
                
                if (context.getArgs().size() > 0) {
                    context.getSender().sendMessage(ChatColor.YELLOW + "Argument: " + 
                                                  context.getArgs().get(0));
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
                }
                
                return true;
            });
        
<<<<<<< HEAD
        SimpleCommand subCommand2 = CommandUtil.createCommandBuilder("sub2", "example")
=======
        SimpleCommand subCommand2 = SimpleCommand.builder("sub2", "example")
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
            .description("Sub-command 2")
            .usage("[arg]")
            .parent(parentCommand)
            .permission("example.parent.sub2")
            .build(context -> {
<<<<<<< HEAD
                context.getSender().sendMessage(Component.text("Sub-command 2 executed!", NamedTextColor.GREEN));
                
                if (context.getParsedArgs().size() > 0) {
                    context.getSender().sendMessage(Component.text("Argument: ", NamedTextColor.YELLOW)
                                                   .append(Component.text(context.getParsedArgs().get(0))));
=======
                context.getSender().sendMessage(ChatColor.GREEN + "Sub-command 2 executed!");
                
                if (context.getArgs().size() > 0) {
                    context.getSender().sendMessage(ChatColor.YELLOW + "Argument: " + 
                                                  context.getArgs().get(0));
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
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
<<<<<<< HEAD
     * Example method to show how to handle commands without sub-commands.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled
     */
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Please specify a sub-command.", NamedTextColor.RED));
            sender.sendMessage(Component.text("Available sub-commands: ", NamedTextColor.YELLOW)
                             .append(Component.text("subcommand1, subcommand2")));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (subCommand) {
            case "subcommand1":
                sender.sendMessage(Component.text("Sub-command 1 executed!", NamedTextColor.GREEN));
                
                if (subArgs.length > 0) {
                    sender.sendMessage(Component.text("Argument: ", NamedTextColor.YELLOW)
                                     .append(Component.text(subArgs[0])));
                }
                return true;
                
            case "subcommand2":
                sender.sendMessage(Component.text("Sub-command 2 executed!", NamedTextColor.GREEN));
                
                if (subArgs.length > 0) {
                    sender.sendMessage(Component.text("Argument: ", NamedTextColor.YELLOW)
                                     .append(Component.text(subArgs[0])));
                }
                return true;
                
            default:
                sender.sendMessage(Component.text("Unknown sub-command: " + subCommand, NamedTextColor.RED));
                return false;
        }
    }
    
    /**
=======
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
     * Example command using annotations.
     *
     * @param context The command context
     * @return true if the command was executed successfully
     */
<<<<<<< HEAD
    @RegisterCommand(
=======
    @CommandProcessor.RegisterCommand(
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
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
<<<<<<< HEAD
        sender.sendMessage(Component.text("Example command executed!", NamedTextColor.GREEN));
        
        if (context.getParsedArgs().size() > 0) {
            sender.sendMessage(Component.text("Argument: ", NamedTextColor.YELLOW)
                              .append(Component.text(context.getParsedArgs().get(0))));
=======
        sender.sendMessage(ChatColor.GREEN + "Example command executed!");
        
        if (context.getArgs().size() > 0) {
            sender.sendMessage(ChatColor.YELLOW + "Argument: " + context.getArgs().get(0));
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
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
<<<<<<< HEAD
    @RegisterCommand(
=======
    @CommandProcessor.RegisterCommand(
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
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
<<<<<<< HEAD
            sender.sendMessage(Component.text("Selected option: " + option, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Invalid option. Available options: ", NamedTextColor.RED)
                              .append(Component.text(String.join(", ", options))));
=======
            sender.sendMessage(ChatColor.GREEN + "Selected option: " + option);
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid option. Available options: " + 
                              String.join(", ", options));
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        }
        
        return true;
    }
    
    /**
<<<<<<< HEAD
     * Example command for players only.
=======
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
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
     *
     * @param context The command context
     * @return true if the command was executed successfully
     */
<<<<<<< HEAD
    @RegisterCommand(
        name = "player",
        description = "Example command for players only",
        usage = "[message]",
=======
    @CommandProcessor.RegisterCommand(
        name = "player",
        description = "Example command for players only",
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        permission = "example.player",
        category = "Examples"
    )
    public boolean playerCommand(CommandContext context) {
        if (!context.isPlayer()) {
<<<<<<< HEAD
            context.getSender().sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return false;
        }
        
        Player player = context.getPlayerOrNull();
        
        if (context.getParsedArgs().size() > 0) {
            player.sendMessage(Component.text("Your message: ", NamedTextColor.YELLOW)
                              .append(Component.text(context.getParsedArgs().get(0))));
        } else {
            player.sendMessage(Component.text("Hello, " + player.getName() + "!", NamedTextColor.GREEN));
        }
=======
            context.getSender().sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = context.getPlayer().get();
        player.sendMessage(ChatColor.GREEN + "Hello, " + player.getName() + "!");
        player.sendMessage(ChatColor.YELLOW + "Your location: " + 
                          player.getLocation().getBlockX() + ", " + 
                          player.getLocation().getBlockY() + ", " + 
                          player.getLocation().getBlockZ());
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        
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