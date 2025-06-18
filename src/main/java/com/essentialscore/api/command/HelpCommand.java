package com.essentialscore.api.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command that provides help information for other commands.
 */
public class HelpCommand extends AbstractCommand {
    
    private static final int COMMANDS_PER_PAGE = 8;
    private final CommandManager commandManager;
    
    /**
     * Creates a new help command.
     *
     * @param moduleId The module ID
     * @param commandManager The command manager
     */
    public HelpCommand(String moduleId, CommandManager commandManager) {
        super("help", "Get help for commands", "<command|page|category>", 
              Collections.singletonList("?"), "essentials.command.help", 
              moduleId, null, 0, 1, "Help", 
              "Provides help for commands.\nUse /help <page> to view a specific page.\nUse /help <category> to view commands in a category.\nUse /help <command> to view detailed help for a command.", 
              Collections.emptyList(), false, 0);
        
        this.commandManager = commandManager;
    }
    
    @Override
    public boolean execute(CommandContext context) {
        CommandSender sender = context.getSender();
        List<String> args = Arrays.asList(context.getArgs());
        
        if (args.isEmpty()) {
            // Show first page of commands
            showCommandList(sender, 1);
            return true;
        }
        
        String arg = args.get(0);
        
        // Check if it's a page number
        try {
            int page = Integer.parseInt(arg);
            showCommandList(sender, page);
            return true;
        } catch (NumberFormatException ignored) {
            // Not a page number, continue
        }
        
        // Check if it's a category
        List<CommandManager.HelpTopic> categoryTopics = commandManager.getHelpTopicsByCategory(arg);
        if (!categoryTopics.isEmpty()) {
            showCategoryCommands(sender, arg, categoryTopics);
            return true;
        }
        
        // Check if it's a command
        CommandManager.HelpTopic helpTopic = commandManager.getHelpTopic(arg);
        if (helpTopic != null) {
            showCommandHelp(sender, helpTopic);
            return true;
        }
        
        // Try to find a command with a similar name
        List<String> suggestions = findSimilarCommands(arg);
        sender.sendMessage(Component.text("Unknown command or category: " + arg)
                .color(NamedTextColor.RED));
        
        if (!suggestions.isEmpty()) {
            sender.sendMessage(Component.text("Did you mean: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.join(
                    Component.text(", ").color(NamedTextColor.GRAY),
                    suggestions.stream()
                        .map(s -> Component.text(s).color(NamedTextColor.WHITE))
                        .collect(Collectors.toList())
                )));
        }
        
        sender.sendMessage(Component.text("Use ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("/help").color(NamedTextColor.WHITE))
                .append(Component.text(" to see all available commands.").color(NamedTextColor.YELLOW)));
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        
        String arg = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();
        
        // Add command names
        for (Command command : commandManager.getCommands()) {
            if (!command.isHidden() && hasPermissionForCommand(sender, command) && 
                startsWith(command.getName(), arg)) {
                completions.add(command.getName());
            }
        }
        
        // Add categories
        for (String category : commandManager.getHelpCategories()) {
            if (startsWith(category, arg)) {
                completions.add(category);
            }
        }
        
        // Add page numbers (1-5)
        for (int i = 1; i <= 5; i++) {
            if (startsWith(String.valueOf(i), arg)) {
                completions.add(String.valueOf(i));
            }
        }
        
        return completions;
    }
    
    /**
     * Shows a list of commands.
     *
     * @param sender The command sender
     * @param page The page number
     */
    private void showCommandList(CommandSender sender, int page) {
        List<Command> commands = commandManager.getCommands().stream()
            .filter(cmd -> !cmd.isHidden() && hasPermissionForCommand(sender, cmd))
            .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
            .collect(Collectors.toList());
        
        int totalPages = (int) Math.ceil((double) commands.size() / COMMANDS_PER_PAGE);
        page = Math.max(1, Math.min(page, totalPages));
        
        int startIndex = (page - 1) * COMMANDS_PER_PAGE;
        int endIndex = Math.min(startIndex + COMMANDS_PER_PAGE, commands.size());
        
        sender.sendMessage(Component.text("=== ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("Help: Commands ").color(NamedTextColor.YELLOW))
                .append(Component.text("(").color(NamedTextColor.GOLD))
                .append(Component.text(page).color(NamedTextColor.YELLOW))
                .append(Component.text("/").color(NamedTextColor.GOLD))
                .append(Component.text(totalPages).color(NamedTextColor.YELLOW))
                .append(Component.text(") ===").color(NamedTextColor.GOLD)));
        
        for (int i = startIndex; i < endIndex; i++) {
            Command command = commands.get(i);
            sender.sendMessage(Component.text("/")
                .color(NamedTextColor.GOLD)
                .append(Component.text(command.getName()).color(NamedTextColor.GOLD))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(command.getDescription()).color(NamedTextColor.WHITE)));
        }
        
        sender.sendMessage(Component.text("Use ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("/help <command>").color(NamedTextColor.WHITE))
                .append(Component.text(" for details on a command.").color(NamedTextColor.YELLOW)));
        
        if (totalPages > 1) {
            sender.sendMessage(Component.text("Use ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("/help <page>").color(NamedTextColor.WHITE))
                    .append(Component.text(" to see other pages.").color(NamedTextColor.YELLOW)));
        }
        
        // Show categories
        List<String> categories = commandManager.getHelpCategories();
        if (categories.size() > 1) {
                sender.sendMessage(Component.text("Categories: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.join(
                        net.kyori.adventure.text.JoinConfiguration.separator(Component.text(", ").color(NamedTextColor.GRAY)),
                        categories.stream()
                            .map(cat -> Component.text(cat).color(NamedTextColor.WHITE))
                            .collect(Collectors.toList())
                    )));
            sender.sendMessage(Component.text("Use ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("/help <category>").color(NamedTextColor.WHITE))
                    .append(Component.text(" to see commands in a category.").color(NamedTextColor.YELLOW)));
        }
    }
    
    /**
     * Shows commands in a category.
     *
     * @param sender The command sender
     * @param category The category name
     * @param topics The help topics in the category
     */
    private void showCategoryCommands(CommandSender sender, String category, List<CommandManager.HelpTopic> topics) {
        // Filter by permission
        topics = topics.stream()
            .filter(topic -> {
                String permission = topic.getPermission();
                return permission == null || permission.isEmpty() || sender.hasPermission(permission);
            })
            .collect(Collectors.toList());
        
        sender.sendMessage(Component.text("=== ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("Help: " + category + " Commands ").color(NamedTextColor.YELLOW))
                .append(Component.text("(").color(NamedTextColor.GOLD))
                .append(Component.text(topics.size()).color(NamedTextColor.YELLOW))
                .append(Component.text(") ===").color(NamedTextColor.GOLD)));
        
        for (CommandManager.HelpTopic topic : topics) {
            sender.sendMessage(Component.text("/")
                .color(NamedTextColor.GOLD)
                .append(Component.text(topic.getName()).color(NamedTextColor.GOLD))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(topic.getDescription()).color(NamedTextColor.WHITE)));
        }
        
        sender.sendMessage(Component.text("Use ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("/help <command>").color(NamedTextColor.WHITE))
                .append(Component.text(" for details on a command.").color(NamedTextColor.YELLOW)));
    }
    
    /**
     * Shows detailed help for a command.
     *
     * @param sender The command sender
     * @param topic The help topic
     */
    private void showCommandHelp(CommandSender sender, CommandManager.HelpTopic topic) {
        // Check permission
        String permission = topic.getPermission();
        if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You don't have permission to view help for this command.")
                    .color(NamedTextColor.RED));
            return;
        }
        
        if (sender instanceof Player) {
            // For players, use a more compact format
            sender.sendMessage(Component.text("=== ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text("Help: " + topic.getName()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" ===").color(NamedTextColor.GOLD)));
            sender.sendMessage(Component.text("Description: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(topic.getDescription()).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Usage: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(topic.getUsage()).color(NamedTextColor.WHITE)));
            
            if (permission != null && !permission.isEmpty()) {
                sender.sendMessage(Component.text("Permission: ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(permission).color(NamedTextColor.WHITE)));
            }
            
            if (topic.getDetailedHelp() != null && !topic.getDetailedHelp().isEmpty()) {
                sender.sendMessage(Component.text("Details:").color(NamedTextColor.GRAY));
                for (String line : topic.getDetailedHelp().split("\n")) {
                    sender.sendMessage(Component.text("  " + line).color(NamedTextColor.WHITE));
                }
            }
            
            if (!topic.getExamples().isEmpty()) {
                sender.sendMessage(Component.text("Examples:").color(NamedTextColor.GRAY));
                for (String example : topic.getExamples()) {
                    sender.sendMessage(Component.text("  " + example).color(NamedTextColor.WHITE));
                }
            }
            
            if (!topic.getSubTopics().isEmpty()) {
                sender.sendMessage(Component.text("Sub-commands:").color(NamedTextColor.GRAY));
                for (CommandManager.HelpTopic subTopic : topic.getSubTopics()) {
                    sender.sendMessage(Component.text("  ")
                            .append(Component.text(subTopic.getName()).color(NamedTextColor.GOLD))
                            .append(Component.text(": ").color(NamedTextColor.GRAY))
                            .append(Component.text(subTopic.getDescription()).color(NamedTextColor.WHITE)));
                }
            }
        } else {
            // For console, convert the pre-formatted help to components
            sender.sendMessage(Component.text(topic.format()));
        }
    }
    
    /**
     * Finds commands with similar names.
     *
     * @param name The name to check
     * @return A list of similar command names
     */
    private List<String> findSimilarCommands(String name) {
        String lowerName = name.toLowerCase();
        Map<String, Integer> similarities = new HashMap<>();
        
        for (Command command : commandManager.getCommands()) {
            String cmdName = command.getName().toLowerCase();
            
            if (cmdName.contains(lowerName) || lowerName.contains(cmdName)) {
                // Contains relationship, higher similarity
                similarities.put(command.getName(), 3);
            } else {
                // Check Levenshtein distance
                int distance = levenshteinDistance(lowerName, cmdName);
                if (distance <= 2) {
                    similarities.put(command.getName(), 2 - distance);
                }
            }
        }
        
        // Return top 3 matches
        return similarities.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates the Levenshtein distance between two strings.
     *
     * @param s1 The first string
     * @param s2 The second string
     * @return The Levenshtein distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}
