package com.essentialscore.utils;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Utility-Klasse für anklickbare Chat-Befehle
 * Kompatibel mit Spigot/Paper APIs
 */
public class ClickableCommand {
    
    /**
     * Sendet eine anklickbare Nachricht mit Hover-Text
     * 
     * @param sender    Der Empfänger der Nachricht
     * @param message   Die Hauptnachricht 
     * @param command   Der auszuführende Befehl
     * @param hoverText Der Hover-Text (optional)
     */
    @SuppressWarnings("deprecation")
    public static void sendClickableMessage(CommandSender sender, String message, String command, String hoverText) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(translateColorCodes(message + " " + command));
            return;
        }
        
        Player player = (Player) sender;
        
        TextComponent baseMessage = new TextComponent(translateColorCodes(message));
        
        TextComponent clickablePart = new TextComponent(translateColorCodes(" §8[§e§l" + command + "§8]"));
        clickablePart.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        
        if (hoverText != null && !hoverText.isEmpty()) {
            clickablePart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text(translateColorCodes(hoverText))));
        } else {
            clickablePart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(translateColorCodes("§7Click to copy command to chat"))));
        }
        
        baseMessage.addExtra(clickablePart);
        player.spigot().sendMessage(baseMessage);
    }
    
    /**
     * Sendet eine anklickbare Nachricht ohne Hover-Text
     */
    public static void sendClickableMessage(CommandSender sender, String message, String command) {
        sendClickableMessage(sender, message, command, null);
    }
    
    /**
     * Erstellt eine anklickbare Hilfe-Nachricht
     * 
     * @param sender     Der Empfänger
     * @param command    Der Befehl
     * @param description Die Beschreibung
     */
    @SuppressWarnings("deprecation")
    public static void sendHelpMessage(CommandSender sender, String command, String description) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(translateColorCodes("§f" + command + " §7- " + description));
            return;
        }
        
        Player player = (Player) sender;
        
        TextComponent helpMessage = new TextComponent(translateColorCodes("§f" + command + " §7- " + description));
        helpMessage.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        helpMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new Text(translateColorCodes("§7Click to copy: " + command))));
        
        player.spigot().sendMessage(helpMessage);
    }
    
    /**
     * Sendet eine anklickbare Liste mit Paginierung
     * 
     * @param sender      Der Empfänger
     * @param items       Die Elemente der Liste
     * @param currentPage Die aktuelle Seite
     * @param itemsPerPage Elemente pro Seite
     * @param baseCommand Der Basis-Befehl für die Paginierung
     */
    public static void sendClickableList(CommandSender sender, List<String> items, int currentPage, 
                                       int itemsPerPage, String baseCommand) {
        if (items.isEmpty()) {
            sender.sendMessage(translateColorCodes("§cNo items found."));
            return;
        }
        
        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        
        // Sende Elemente
        for (int i = startIndex; i < endIndex; i++) {
            String item = items.get(i);
            sendClickableMessage(sender, "§f" + (i + 1) + ". ", item, "§7Click to copy: " + item);
        }
        
        // Sende Navigation
        sendPaginationNavigation(sender, currentPage, totalPages, baseCommand);
    }
    
    /**
     * Sendet die Paginierungs-Navigation
     */
    @SuppressWarnings("deprecation")
    public static void sendPaginationNavigation(CommandSender sender, int currentPage, int totalPages, String baseCommand) {
        if (totalPages <= 1) {
            return;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(translateColorCodes("§7Page " + currentPage + " of " + totalPages));
            return;
        }
        
        Player player = (Player) sender;
        
        TextComponent navigation = new TextComponent(translateColorCodes("§7Page §f" + currentPage + "§7/§f" + totalPages + " "));
        
        // Previous Page Button
        if (currentPage > 1) {
            TextComponent prevButton = new TextComponent(translateColorCodes("§8[§c◀ Previous§8]"));
            prevButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, baseCommand + " " + (currentPage - 1)));
            prevButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text(translateColorCodes("§7Go to page " + (currentPage - 1)))));
            navigation.addExtra(prevButton);
            navigation.addExtra(new TextComponent(" "));
        }
        
        // Next Page Button
        if (currentPage < totalPages) {
            TextComponent nextButton = new TextComponent(translateColorCodes("§8[§aNext ▶§8]"));
            nextButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, baseCommand + " " + (currentPage + 1)));
            nextButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text(translateColorCodes("§7Go to page " + (currentPage + 1)))));
            navigation.addExtra(nextButton);
        }
        
        player.spigot().sendMessage(navigation);
    }
    
    /**
     * Übersetzt Color-Codes
     */
    private static String translateColorCodes(String message) {
        return message.replace('&', '§');
    }
}
