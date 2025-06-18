package com.essentialscore.api.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Interface for reusable GUI components that can be added to GUIs.
 */
public interface GUIComponent {
    
    /**
     * Gets the component's type.
     *
     * @return The component type
     */
    ComponentType getType();
    
    /**
     * Gets the component's ID.
     *
     * @return The component ID
     */
    String getId();
    
    /**
     * Gets the component's display item.
     *
     * @param player The player viewing the component
     * @return The display item
     */
    ItemStack getDisplayItem(Player player);
    
    /**
     * Gets the component's preferred slot.
     *
     * @return The preferred slot, or -1 if no preference
     */
    int getPreferredSlot();
    
    /**
     * Sets the component's preferred slot.
     *
     * @param slot The preferred slot
     */
    void setPreferredSlot(int slot);
    
    /**
     * Handles a click on this component.
     *
     * @param player The player who clicked
     * @param event The click event
     * @return true if the click was handled
     */
    boolean onClick(Player player, InventoryClickEvent event);
    
    /**
     * Updates the component's state.
     *
     * @param player The player viewing the component
     */
    void update(Player player);
    
    /**
     * Enum defining component types.
     */
    enum ComponentType {
        BUTTON,
        TOGGLE,
        SLIDER,
        SELECTOR,
        TEXT_INPUT,
        PAGINATOR,
        CUSTOM
    }
} 
