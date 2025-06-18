package com.essentialscore.api.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * A toggle component that switches between on and off states.
 */
public class ToggleComponent implements GUIComponent {
    
    private final String id;
    private final String label;
    private boolean state;
    private final Consumer<Boolean> stateChangeHandler;
    private int preferredSlot = -1;
    private final Material enabledMaterial;
    private final Material disabledMaterial;
    private final String[] description;
    
    /**
     * Creates a new toggle component.
     *
     * @param id The toggle ID
     * @param label The toggle label
     * @param initialState The initial state
     * @param stateChangeHandler The state change handler
     * @param description Optional description lines
     */
    public ToggleComponent(String id, String label, boolean initialState, Consumer<Boolean> stateChangeHandler, String... description) {
        this(id, label, initialState, Material.LIME_WOOL, Material.RED_WOOL, stateChangeHandler, description);
    }
    
    /**
     * Creates a new toggle component with custom materials.
     *
     * @param id The toggle ID
     * @param label The toggle label
     * @param initialState The initial state
     * @param enabledMaterial The material to use when enabled
     * @param disabledMaterial The material to use when disabled
     * @param stateChangeHandler The state change handler
     * @param description Optional description lines
     */
    public ToggleComponent(String id, String label, boolean initialState, Material enabledMaterial, 
                           Material disabledMaterial, Consumer<Boolean> stateChangeHandler, String... description) {
        this.id = id;
        this.label = label;
        this.state = initialState;
        this.enabledMaterial = enabledMaterial;
        this.disabledMaterial = disabledMaterial;
        this.stateChangeHandler = stateChangeHandler;
        this.description = description;
    }
    
    @Override
    public ComponentType getType() {
        return ComponentType.TOGGLE;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public ItemStack getDisplayItem(Player player) {
        Material material = state ? enabledMaterial : disabledMaterial;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(label);
            
            List<String> lore = new ArrayList<>();
            lore.add(state ? "§aEnabled" : "§cDisabled");
            lore.add("§7Click to toggle");
            
            if (description != null && description.length > 0) {
                lore.add("");
                lore.addAll(Arrays.asList(description));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
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
        // Toggle state
        state = !state;
        
        // Notify handler
        if (stateChangeHandler != null) {
            stateChangeHandler.accept(state);
        }
        
        return true; // Handled, update display
    }
    
    @Override
    public void update(Player player) {
        // Nothing to update automatically
    }
    
    /**
     * Gets the toggle label.
     *
     * @return The label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Gets the current state.
     *
     * @return The state
     */
    public boolean getState() {
        return state;
    }
    
    /**
     * Sets the current state.
     *
     * @param state The state to set
     */
    public void setState(boolean state) {
        this.state = state;
        
        // Notify handler
        if (stateChangeHandler != null) {
            stateChangeHandler.accept(state);
        }
    }
} 
