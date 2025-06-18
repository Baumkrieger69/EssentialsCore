package com.essentialscore.api.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Interface defining a GUI that modules can create and interact with.
 */
public interface GUI {
    
    /**
     * Gets the GUI's inventory.
     *
     * @return The inventory
     */
    Inventory getInventory();
    
    /**
     * Gets the GUI's title.
     *
     * @return The title
     */
    String getTitle();
    
    /**
     * Gets the number of rows in the GUI.
     *
     * @return The number of rows
     */
    int getRows();
    
    /**
     * Gets the module ID that created this GUI.
     *
     * @return The module ID
     */
    String getModuleId();
    
    /**
     * Gets the GUI's theme.
     *
     * @return The theme
     */
    GUITheme getTheme();
    
    /**
     * Sets an item in the GUI.
     *
     * @param slot The slot
     * @param item The item
     */
    void setItem(int slot, ItemStack item);
    
    /**
     * Sets an item in the GUI with a click handler.
     *
     * @param slot The slot
     * @param item The item
     * @param handler The click handler
     */
    void setItem(int slot, ItemStack item, GUIClickHandler handler);
    
    /**
     * Adds a component to the GUI.
     *
     * @param component The component
     * @return The component's slot index
     */
    int addComponent(GUIComponent component);
    
    /**
     * Gets a component at a slot.
     *
     * @param slot The slot
     * @return The component, or null if none
     */
    GUIComponent getComponent(int slot);
    
    /**
     * Updates the GUI contents.
     *
     * @param player The player viewing the GUI
     */
    void update(Player player);
    
    /**
     * Called when the GUI is opened.
     *
     * @param player The player opening the GUI
     */
    void onOpen(Player player);
    
    /**
     * Called when the GUI is closed.
     *
     * @param player The player closing the GUI
     */
    void onClose(Player player);
    
    /**
     * Called when a player clicks in the GUI.
     *
     * @param player The player
     * @param slot The clicked slot
     * @param event The click event
     */
    void onClick(Player player, int slot, InventoryClickEvent event);
    
    /**
     * Clears the GUI.
     */
    void clear();
} 
