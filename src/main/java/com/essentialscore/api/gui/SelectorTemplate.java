package com.essentialscore.api.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Template for creating selector GUIs.
 */
public class SelectorTemplate implements GUITemplate {
    
    private static final String ID = "selector";
    private static final String DESCRIPTION = "Template for selection GUIs";
    private static final int RECOMMENDED_ROWS = 4;
    
    private static final int CONFIRM_SLOT = 31; // Bottom middle
    private static final int CANCEL_SLOT = 27; // Bottom left
    
    private final GUIClickHandler confirmHandler;
    private final GUIClickHandler cancelHandler;
    
    /**
     * Creates a new selector template.
     */
    public SelectorTemplate() {
        this(null, null);
    }
    
    /**
     * Creates a new selector template with custom handlers.
     *
     * @param confirmHandler The confirm button handler
     * @param cancelHandler The cancel button handler
     */
    public SelectorTemplate(GUIClickHandler confirmHandler, GUIClickHandler cancelHandler) {
        this.confirmHandler = confirmHandler;
        this.cancelHandler = cancelHandler;
    }
    
    @Override
    public String getId() {
        return ID;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public int getRecommendedRows() {
        return RECOMMENDED_ROWS;
    }
    
    @Override
    public void apply(GUI gui) {
        GUITheme theme = gui.getTheme();
        
        // Add a border
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, theme.getBorderItem()); // Top row
        }
        
        for (int i = 0; i < gui.getRows(); i++) {
            gui.setItem(i * 9, theme.getBorderItem()); // Left column
            gui.setItem(i * 9 + 8, theme.getBorderItem()); // Right column
        }
        
        // Add bottom row background
        int lastRowStart = (gui.getRows() - 1) * 9;
        for (int i = 0; i < 9; i++) {
            gui.setItem(lastRowStart + i, theme.getBackgroundItem());
        }
        
        // Create confirm button
        ItemStack confirmButton = new ItemStack(Material.EMERALD);
        confirmButton = theme.styleItem(confirmButton, "Confirm Selection", "Click to confirm your selection");
        
        // Create cancel button
        ItemStack cancelButton = new ItemStack(Material.BARRIER);
        cancelButton = theme.styleItem(cancelButton, "Cancel", "Click to cancel");
        
        // Add buttons to the GUI
        if (confirmHandler != null) {
            gui.setItem(CONFIRM_SLOT, confirmButton, confirmHandler);
        } else {
            gui.setItem(CONFIRM_SLOT, confirmButton);
        }
        
        if (cancelHandler != null) {
            gui.setItem(CANCEL_SLOT, cancelButton, cancelHandler);
        } else {
            gui.setItem(CANCEL_SLOT, cancelButton);
        }
    }
} 
