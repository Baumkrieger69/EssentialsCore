package com.essentialscore.api.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard implementation of the GUI interface.
 */
public class StandardGUI implements GUI {
    
    private final String moduleId;
    private final String title;
    private final int rows;
    private final GUITheme theme;
    private final Inventory inventory;
    private final Map<Integer, GUIClickHandler> clickHandlers;
    private final Map<Integer, GUIComponent> components;
    
    /**
     * Creates a new standard GUI.
     *
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows
     * @param theme The GUI theme
     */
    public StandardGUI(String moduleId, String title, int rows, GUITheme theme) {
        this.moduleId = moduleId;
        this.title = title;
        this.rows = rows;
        this.theme = theme;
        this.inventory = Bukkit.createInventory(null, rows * 9, theme.formatTitle(title));
        this.clickHandlers = new HashMap<>();
        this.components = new HashMap<>();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public int getRows() {
        return rows;
    }
    
    @Override
    public String getModuleId() {
        return moduleId;
    }
    
    @Override
    public GUITheme getTheme() {
        return theme;
    }
    
    @Override
    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < rows * 9) {
            inventory.setItem(slot, item);
        }
    }
    
    @Override
    public void setItem(int slot, ItemStack item, GUIClickHandler handler) {
        if (slot >= 0 && slot < rows * 9) {
            inventory.setItem(slot, item);
            clickHandlers.put(slot, handler);
        }
    }
    
    @Override
    public int addComponent(GUIComponent component) {
        int slot = component.getPreferredSlot();
        
        // If no preferred slot or slot is already taken, find an empty slot
        if (slot < 0 || components.containsKey(slot)) {
            slot = findEmptySlot();
        }
        
        if (slot >= 0) {
            components.put(slot, component);
            component.setPreferredSlot(slot);
            setItem(slot, component.getDisplayItem(null));
        }
        
        return slot;
    }
    
    @Override
    public GUIComponent getComponent(int slot) {
        return components.get(slot);
    }
    
    @Override
    public void update(Player player) {
        // Update components
        for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
            int slot = entry.getKey();
            GUIComponent component = entry.getValue();
            
            // Update the component and its display item
            component.update(player);
            inventory.setItem(slot, component.getDisplayItem(player));
        }
    }
    
    @Override
    public void onOpen(Player player) {
        // Do nothing by default
    }
    
    @Override
    public void onClose(Player player) {
        // Do nothing by default
    }
    
    @Override
    public void onClick(Player player, int slot, InventoryClickEvent event) {
        // First check if the slot has a component
        GUIComponent component = components.get(slot);
        
        if (component != null) {
            // Let the component handle the click
            boolean handled = component.onClick(player, event);
            
            if (handled) {
                // Update the component's display item
                inventory.setItem(slot, component.getDisplayItem(player));
                return;
            }
        }
        
        // Then check if the slot has a click handler
        GUIClickHandler handler = clickHandlers.get(slot);
        
        if (handler != null) {
            handler.onClick(player, slot, event);
        }
    }
    
    @Override
    public void clear() {
        inventory.clear();
        clickHandlers.clear();
        components.clear();
    }
    
    /**
     * Finds an empty slot in the GUI.
     *
     * @return The first empty slot, or -1 if the GUI is full
     */
    private int findEmptySlot() {
        for (int slot = 0; slot < rows * 9; slot++) {
            if (inventory.getItem(slot) == null && !components.containsKey(slot)) {
                return slot;
            }
        }
        
        return -1;
    }
} 
