package com.essentialscore.listener;

import com.essentialscore.ApiCore;
import com.essentialscore.placeholder.PlaceholderManager;
import com.essentialscore.clickable.ClickableCommandManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

/**
 * Listener for processing chat messages with placeholders and clickable commands
 * Supports both modern Paper events and legacy Bukkit events
 */
public class ChatMessageListener implements Listener {
    
    private final ApiCore plugin;
    private final PlaceholderManager placeholderManager;
    private final ClickableCommandManager clickableCommandManager;
    
    public ChatMessageListener(ApiCore plugin) {
        this.plugin = plugin;
        this.placeholderManager = plugin.getPlaceholderManager();
        this.clickableCommandManager = plugin.getClickableCommandManager();
    }
    
    /**
     * Handle modern Paper AsyncChatEvent (for newer servers)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        try {
            Component message = event.message();
            if (!(message instanceof TextComponent)) {
                return; // Skip non-text components
            }
            
            Player player = event.getPlayer();
            String messageText = ((TextComponent) message).content();
            String originalMessage = messageText;
            
            // Process placeholders if enabled
            if (placeholderManager != null && placeholderManager.isEnabled()) {
                messageText = placeholderManager.processMessage(messageText, player);
            }
            
            // Process clickable commands if enabled
            if (clickableCommandManager != null && clickableCommandManager.isEnabled()) {
                messageText = clickableCommandManager.processClickableCommands(messageText, player);
            }
            
            // Only update the message if it actually changed
            if (!originalMessage.equals(messageText)) {
                event.message(Component.text(messageText));
            }
            
        } catch (Exception e) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("Error processing modern chat message for player " + event.getPlayer().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Handle legacy AsyncPlayerChatEvent (for older servers)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    @SuppressWarnings("deprecation") // Using deprecated AsyncPlayerChatEvent for compatibility
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String originalMessage = message;
        
        try {
            // Process placeholders if enabled
            if (placeholderManager != null && placeholderManager.isEnabled()) {
                message = placeholderManager.processMessage(message, player);
            }
            
            // Process clickable commands if enabled
            if (clickableCommandManager != null && clickableCommandManager.isEnabled()) {
                message = clickableCommandManager.processClickableCommands(message, player);
            }
            
            // Only update the message if it actually changed
            if (!originalMessage.equals(message)) {
                event.setMessage(message);
            }
            
        } catch (Exception e) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("Error processing legacy chat message for player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
