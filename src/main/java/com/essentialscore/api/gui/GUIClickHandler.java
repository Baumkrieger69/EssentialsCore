package com.essentialscore.api.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Functional interface for handling GUI click events.
 */
@FunctionalInterface
public interface GUIClickHandler {
    
    /**
     * Handles a click in a GUI.
     *
     * @param player The player who clicked
     * @param slot The clicked slot
     * @param event The click event
     * @return true if the click was handled
     */
    boolean onClick(Player player, int slot, InventoryClickEvent event);
} 
