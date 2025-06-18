package com.essentialscore.api.gui;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Factory for creating standard GUIs.
 */
public class StandardGUIFactory implements GUIFactory {
    
    private static final String ID = "standard";
    private static final String DESCRIPTION = "Creates standard GUIs with basic functionality";
    
    private final GUIManager guiManager;
    
    /**
     * Creates a new standard GUI factory.
     *
     * @param guiManager The GUI manager
     */
    public StandardGUIFactory(GUIManager guiManager) {
        this.guiManager = guiManager;
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
    public GUI create(String moduleId, String title, int rows, Map<String, Object> parameters) {
        // Get the theme from parameters or use the default
        GUITheme theme = parameters.containsKey("theme") ? 
                (GUITheme) parameters.get("theme") : 
                guiManager.getDefaultTheme();
        
        // Create the GUI
        StandardGUI gui = new StandardGUI(moduleId, title, rows, theme);
        
        // Add items if provided
        if (parameters.containsKey("items")) {
            @SuppressWarnings("unchecked")
            Map<Integer, ItemStack> items = (Map<Integer, ItemStack>) parameters.get("items");
            
            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                gui.setItem(entry.getKey(), entry.getValue());
            }
        }
        
        // Add click handlers if provided
        if (parameters.containsKey("handlers")) {
            @SuppressWarnings("unchecked")
            Map<Integer, GUIClickHandler> handlers = (Map<Integer, GUIClickHandler>) parameters.get("handlers");
            
            for (Map.Entry<Integer, GUIClickHandler> entry : handlers.entrySet()) {
                int slot = entry.getKey();
                GUIClickHandler handler = entry.getValue();
                ItemStack item = gui.getInventory().getItem(slot);
                
                if (item != null) {
                    gui.setItem(slot, item, handler);
                }
            }
        }
        
        // Apply template if specified
        if (parameters.containsKey("template")) {
            String templateId = (String) parameters.get("template");
            GUITemplate template = guiManager.getTemplate(templateId);
            
            if (template != null) {
                template.apply(gui);
            }
        }
        
        return gui;
    }
    
    @Override
    public GUIManager getGUIManager() {
        return guiManager;
    }
} 
