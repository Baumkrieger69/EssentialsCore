package com.essentialscore.clickable;

import com.essentialscore.ApiCore;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ClickableCommandManager - Manages clickable commands in chat messages
 * Supports multiple bracket types: {}, (), [], %%
 */
public class ClickableCommandManager {
    
    private final ApiCore plugin;
    private final Map<UUID, PendingConfirmation> pendingConfirmations;
    
    // Regex patterns for different bracket types
    private final Pattern curlyPattern = Pattern.compile("\\{(/[^}]+)\\}");
    private final Pattern roundPattern = Pattern.compile("\\((/[^)]+)\\)");
    private final Pattern squarePattern = Pattern.compile("\\[(/[^\\]]+)\\]");
    private final Pattern percentPattern = Pattern.compile("%(/[^%]+)%");
    
    public ClickableCommandManager(ApiCore plugin) {
        this.plugin = plugin;
        this.pendingConfirmations = new HashMap<>();
        
        // Start cleanup task for pending confirmations
        startConfirmationCleanupTask();
    }
    
    /**
     * Check if clickable command processing is enabled
     */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("clickable-commands.enabled", true);
    }
    
    /**
     * Process message and convert commands to clickable text (simplified version for chat)
     */
    public String processClickableCommands(String message, Player player) {
        if (!isEnabled()) {
            return message;
        }
        
        // For chat messages, we'll replace command brackets with clickable indicators
        // This is a simplified version - the full TextComponent version is in processMessage
        String processed = message;
        
        if (plugin.getConfig().getBoolean("clickable-commands.brackets.curly", true)) {
            processed = processed.replaceAll("\\{(/[^}]+)\\}", "§b§l[CLICK]§r$1");
        }
        if (plugin.getConfig().getBoolean("clickable-commands.brackets.round", true)) {
            processed = processed.replaceAll("\\((/[^)]+)\\)", "§b§l[CLICK]§r$1");
        }
        if (plugin.getConfig().getBoolean("clickable-commands.brackets.square", true)) {
            processed = processed.replaceAll("\\[(/[^\\]]+)\\]", "§b§l[CLICK]§r$1");
        }
        
        return processed;
    }
    
    /**
     * Process message and convert commands to clickable components
     */
    public TextComponent processMessage(String message, Player player) {
        if (!plugin.getConfig().getBoolean("clickable-commands.enabled", true)) {
            return new TextComponent(message);
        }
        
        ComponentBuilder builder = new ComponentBuilder("");
        String remaining = message;
        
        // Process each bracket type if enabled
        if (plugin.getConfig().getBoolean("clickable-commands.brackets.curly", true)) {
            remaining = processPattern(remaining, curlyPattern, builder, player);
        }
        if (plugin.getConfig().getBoolean("clickable-commands.brackets.round", true)) {
            remaining = processPattern(remaining, roundPattern, builder, player);
        }
        if (plugin.getConfig().getBoolean("clickable-commands.brackets.square", true)) {
            remaining = processPattern(remaining, squarePattern, builder, player);
        }
        if (plugin.getConfig().getBoolean("clickable-commands.brackets.percent", false)) {
            remaining = processPattern(remaining, percentPattern, builder, player);
        }
        
        // Add any remaining text
        if (!remaining.isEmpty()) {
            builder.append(remaining);
        }
        
        return new TextComponent(builder.create());
    }
    
    /**
     * Process a specific pattern and build clickable components
     */
    private String processPattern(String message, Pattern pattern, ComponentBuilder builder, Player player) {
        Matcher matcher = pattern.matcher(message);
        int lastEnd = 0;
        
        while (matcher.find()) {
            String beforeCommand = message.substring(lastEnd, matcher.start());
            String command = matcher.group(1);
            
            // Add text before command
            if (!beforeCommand.isEmpty()) {
                builder.append(beforeCommand);
            }
            
            // Check if command is allowed
            if (isCommandAllowed(command, player)) {
                // Create clickable component
                TextComponent clickableCommand = createClickableCommand(command, player);
                builder.append(clickableCommand);
            } else {
                // Add as normal text if not allowed
                builder.append(matcher.group(0));
            }
            
            lastEnd = matcher.end();
        }
        
        // Return remaining text after last match
        return message.substring(lastEnd);
    }
    
    /**
     * Create a clickable command component
     */
    private TextComponent createClickableCommand(String command, Player player) {
        String commandColor = plugin.getConfig().getString("clickable-commands.style.command-color", "&b");
        String commandStyle = plugin.getConfig().getString("clickable-commands.style.command-style", "UNDERLINE");
        boolean addIndicator = plugin.getConfig().getBoolean("clickable-commands.style.click-indicator", true);
        String indicator = plugin.getConfig().getString("clickable-commands.style.indicator-symbol", " ➤");
        
        // Create the display text
        String displayText = command;
        if (addIndicator) {
            displayText += indicator;
        }
        
        TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', commandColor + displayText));
        
        // Apply styling
        if (commandStyle.contains("BOLD")) {
            component.setBold(true);
        }
        if (commandStyle.contains("ITALIC")) {
            component.setItalic(true);
        }
        if (commandStyle.contains("UNDERLINE")) {
            component.setUnderlined(true);
        }
          // Set hover text
        String hoverText = plugin.getConfig().getString("clickable-commands.visual.hover-text", "&eClick to execute: &f{command}");
        hoverText = hoverText.replace("{command}", command);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new net.md_5.bungee.api.chat.hover.content.Text(ChatColor.translateAlternateColorCodes('&', hoverText))));
        
        // Set click action
        if (requiresConfirmation(command)) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                "/essentialscore:confirm-command " + command));
        } else {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        }
        
        return component;
    }
    
    /**
     * Check if a command is allowed for the player
     */
    private boolean isCommandAllowed(String command, Player player) {
        // Check if player has permission (if required)
        if (plugin.getConfig().getBoolean("clickable-commands.security.require-permission", true)) {
            String permissionPrefix = plugin.getConfig().getString("clickable-commands.security.permission-prefix", "essentialscore.clickable");
            if (player != null && !player.hasPermission(permissionPrefix + ".use")) {
                return false;
            }
        }
        
        // Check blacklist
        List<String> blacklist = plugin.getConfig().getStringList("clickable-commands.security.command-blacklist");
        for (String blocked : blacklist) {
            if (command.toLowerCase().startsWith(blocked.toLowerCase())) {
                return false;
            }
        }
        
        // Check whitelist (if not empty)
        List<String> whitelist = plugin.getConfig().getStringList("clickable-commands.security.command-whitelist");
        if (!whitelist.isEmpty()) {
            boolean found = false;
            for (String allowed : whitelist) {
                if (command.toLowerCase().startsWith(allowed.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if a command requires confirmation
     */
    private boolean requiresConfirmation(String command) {
        if (!plugin.getConfig().getBoolean("clickable-commands.visual.require-confirmation", true)) {
            return false;
        }
        
        List<String> confirmationCommands = plugin.getConfig().getStringList("clickable-commands.visual.confirmation-commands");
        for (String cmd : confirmationCommands) {
            if (command.toLowerCase().startsWith(cmd.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handle command confirmation
     */
    public void handleConfirmation(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        PendingConfirmation pending = pendingConfirmations.get(playerId);
        
        if (pending != null && pending.command.equals(command)) {
            // Execute the command
            pendingConfirmations.remove(playerId);
            player.performCommand(command.substring(1)); // Remove leading /
        } else {
            // Add to pending confirmations
            long timeout = plugin.getConfig().getLong("clickable-commands.visual.confirmation-timeout", 10) * 1000;
            pendingConfirmations.put(playerId, new PendingConfirmation(command, currentTime + timeout));
            
            // Send confirmation message
            String confirmationMessage = plugin.getConfig().getString("clickable-commands.visual.confirmation-message", 
                "&cAre you sure? Click again within 10 seconds to confirm.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', confirmationMessage));
        }
    }
    
    /**
     * Start cleanup task for expired confirmations
     */
    private void startConfirmationCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            pendingConfirmations.entrySet().removeIf(entry -> entry.getValue().expiresAt < currentTime);
        }, 20L, 20L); // Run every second
    }
    
    /**
     * Pending confirmation data class
     */
    private static class PendingConfirmation {
        final String command;
        final long expiresAt;
        
        PendingConfirmation(String command, long expiresAt) {
            this.command = command;
            this.expiresAt = expiresAt;
        }
    }
}
