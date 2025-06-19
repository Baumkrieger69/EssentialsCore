package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import com.essentialscore.clickable.ClickableCommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to handle clickable command confirmations
 */
public class ConfirmCommandExecutor implements CommandExecutor {
    
    @SuppressWarnings("unused") // Field reserved for future use (logging, permissions, etc.)
    private final ApiCore plugin;
    private final ClickableCommandManager clickableManager;
    
    public ConfirmCommandExecutor(ApiCore plugin, ClickableCommandManager clickableManager) {
        this.plugin = plugin;
        this.clickableManager = clickableManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("Usage: /confirm-command <command>");
            return true;
        }
        
        // Reconstruct the full command
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) commandBuilder.append(" ");
            commandBuilder.append(args[i]);
        }
        
        String fullCommand = commandBuilder.toString();
        if (!fullCommand.startsWith("/")) {
            fullCommand = "/" + fullCommand;
        }
        
        clickableManager.handleConfirmation((Player) sender, fullCommand);
        return true;
    }
}
