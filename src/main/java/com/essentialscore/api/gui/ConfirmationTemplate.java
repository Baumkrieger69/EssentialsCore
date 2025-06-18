package com.essentialscore.api.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Template for creating confirmation dialog GUIs.
 */
public class ConfirmationTemplate implements GUITemplate {
    
    private static final String ID = "confirmation";
    private static final String DESCRIPTION = "Template for confirmation dialog GUIs";
    private static final int RECOMMENDED_ROWS = 3;
    
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;
    
    private final String confirmText;
    private final String cancelText;
    private final GUIClickHandler confirmHandler;
    private final GUIClickHandler cancelHandler;
    
    /**
     * Creates a new confirmation template.
     */
    public ConfirmationTemplate() {
        this("Confirm", "Cancel", null, null);
    }
    
    /**
     * Creates a new confirmation template with custom text.
     *
     * @param confirmText The confirm button text
     * @param cancelText The cancel button text
     */
    public ConfirmationTemplate(String confirmText, String cancelText) {
        this(confirmText, cancelText, null, null);
    }
    
    /**
     * Creates a new confirmation template with custom text and handlers.
     *
     * @param confirmText The confirm button text
     * @param cancelText The cancel button text
     * @param confirmHandler The confirm click handler
     * @param cancelHandler The cancel click handler
     */
    public ConfirmationTemplate(String confirmText, String cancelText, 
                                GUIClickHandler confirmHandler, GUIClickHandler cancelHandler) {
        this.confirmText = confirmText;
        this.cancelText = cancelText;
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
        
        // Create confirm button
        ItemStack confirmButton = new ItemStack(Material.LIME_WOOL);
        confirmButton = theme.styleItem(confirmButton, confirmText, "Click to confirm");
        
        // Create cancel button
        ItemStack cancelButton = new ItemStack(Material.RED_WOOL);
        cancelButton = theme.styleItem(cancelButton, cancelText, "Click to cancel");
        
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
        
        // Add background
        ItemStack background = theme.getBackgroundItem();
        for (int i = 0; i < gui.getRows() * 9; i++) {
            if (i != CONFIRM_SLOT && i != CANCEL_SLOT && gui.getInventory().getItem(i) == null) {
                gui.setItem(i, background);
            }
        }
    }
} 
