package com.essentialscore.api.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A basic button component that can be clicked.
 */
public class ButtonComponent implements GUIComponent {
    
    private final String id;
    private final String label;
    private final ItemStack displayItem;
    private final GUIClickHandler clickHandler;
    private int preferredSlot = -1;
    
    /**
     * Creates a new button component.
     *
     * @param id The button ID
     * @param label The button label
     * @param displayItem The display item
     * @param clickHandler The click handler
     */
    public ButtonComponent(String id, String label, ItemStack displayItem, GUIClickHandler clickHandler) {
        this.id = id;
        this.label = label;
        this.displayItem = displayItem;
        this.clickHandler = clickHandler;
    }
    
    /**
     * Creates a new button component with default material.
     *
     * @param id The button ID
     * @param label The button label
     * @param clickHandler The click handler
     */
    public ButtonComponent(String id, String label, GUIClickHandler clickHandler) {
        this(id, label, new ItemStack(Material.STONE_BUTTON), clickHandler);
    }
    
    @Override
    public ComponentType getType() {
        return ComponentType.BUTTON;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public ItemStack getDisplayItem(Player player) {
        return displayItem;
    }
    
    @Override
    public int getPreferredSlot() {
        return preferredSlot;
    }
    
    @Override
    public void setPreferredSlot(int slot) {
        this.preferredSlot = slot;
    }
    
    @Override
    public boolean onClick(Player player, InventoryClickEvent event) {
        if (clickHandler != null) {
            return clickHandler.onClick(player, event.getSlot(), event);
        }
        return false;
    }
    
    @Override
    public void update(Player player) {
        // Buttons don't need updates by default
    }
    
    /**
     * Gets the button label.
     *
     * @return The label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Gets the click handler.
     *
     * @return The click handler
     */
    public GUIClickHandler getClickHandler() {
        return clickHandler;
    }
} 
