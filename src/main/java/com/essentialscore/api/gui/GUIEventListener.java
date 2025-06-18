package com.essentialscore.api.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bukkit event listener for handling GUI-related events.
 */
public class GUIEventListener implements Listener {
    
    private final GUIManager guiManager;
    
    /**
     * Creates a new GUI event listener.
     *
     * @param guiManager The GUI manager
     */
    public GUIEventListener(GUIManager guiManager) {
        this.guiManager = guiManager;
    }
    
    /**
     * Handles inventory click events.
     *
     * @param event The click event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            
            if (guiManager.hasActiveGUI(player)) {
                guiManager.handleGUIClick(event);
            }
        }
    }
    
    /**
     * Handles inventory drag events.
     *
     * @param event The drag event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            
            if (guiManager.hasActiveGUI(player)) {
                // Cancel dragging in GUI inventories to prevent item manipulation
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handles inventory close events.
     *
     * @param event The close event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            
            if (guiManager.hasActiveGUI(player)) {
                guiManager.handleGUIClose(player);
            }
        }
    }
    
    /**
     * Handles player quit events.
     *
     * @param event The quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (guiManager.hasActiveGUI(player)) {
            guiManager.handleGUIClose(player);
        }
    }
} 
