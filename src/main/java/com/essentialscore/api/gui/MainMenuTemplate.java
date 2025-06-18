package com.essentialscore.api.gui;

/**
 * Template for creating main menu GUIs.
 */
public class MainMenuTemplate implements GUITemplate {
    
    private static final String ID = "main_menu";
    private static final String DESCRIPTION = "Template for main menu GUIs";
    private static final int RECOMMENDED_ROWS = 6;
    
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
        
        // Add a border around the GUI
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, theme.getBorderItem()); // Top row
            gui.setItem((gui.getRows() - 1) * 9 + i, theme.getBorderItem()); // Bottom row
        }
        
        for (int i = 1; i < gui.getRows() - 1; i++) {
            gui.setItem(i * 9, theme.getBorderItem()); // Left column
            gui.setItem(i * 9 + 8, theme.getBorderItem()); // Right column
        }
        
        // Fill corners with different color for aesthetics
        gui.setItem(0, theme.createButton(theme.getAccentColor() + "◆", ""));
        gui.setItem(8, theme.createButton(theme.getAccentColor() + "◆", ""));
        gui.setItem((gui.getRows() - 1) * 9, theme.createButton(theme.getAccentColor() + "◆", ""));
        gui.setItem((gui.getRows() - 1) * 9 + 8, theme.createButton(theme.getAccentColor() + "◆", ""));
    }
} 
