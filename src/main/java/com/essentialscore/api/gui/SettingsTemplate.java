package com.essentialscore.api.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Template for creating settings/configuration GUIs.
 */
public class SettingsTemplate implements GUITemplate {
    
    private static final String ID = "settings";
    private static final String DESCRIPTION = "Template for settings and configuration GUIs";
    private static final int RECOMMENDED_ROWS = 5;
    
    private static final int SAVE_SLOT = 40; // Bottom middle
    private static final int BACK_SLOT = 36; // Bottom left
    
    private final GUIClickHandler saveHandler;
    private final GUIClickHandler backHandler;
    
    /**
     * Creates a new settings template.
     */
    public SettingsTemplate() {
        this(null, null);
    }
    
    /**
     * Creates a new settings template with custom handlers.
     *
     * @param saveHandler The save button handler
     * @param backHandler The back button handler
     */
    public SettingsTemplate(GUIClickHandler saveHandler, GUIClickHandler backHandler) {
        this.saveHandler = saveHandler;
        this.backHandler = backHandler;
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
        
        // Create save button
        ItemStack saveButton = new ItemStack(Material.EMERALD);
        saveButton = theme.styleItem(saveButton, "Save Settings", "Click to save your settings");
        
        // Create back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        backButton = theme.styleItem(backButton, "Back", "Click to go back");
        
        // Add buttons to the GUI
        if (saveHandler != null) {
            gui.setItem(SAVE_SLOT, saveButton, saveHandler);
        } else {
            gui.setItem(SAVE_SLOT, saveButton);
        }
        
        if (backHandler != null) {
            gui.setItem(BACK_SLOT, backButton, backHandler);
        } else {
            gui.setItem(BACK_SLOT, backButton);
        }
    }
}
