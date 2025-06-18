package com.essentialscore.api.gui;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating paginated GUIs.
 */
public class PaginatedGUIFactory implements GUIFactory {
    
    private static final String ID = "paginated";
    private static final String DESCRIPTION = "Creates paginated GUIs for displaying large collections of items";
    
    private final GUIManager guiManager;
    
    /**
     * Creates a new paginated GUI factory.
     *
     * @param guiManager The GUI manager
     */
    public PaginatedGUIFactory(GUIManager guiManager) {
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
        
        // Get items from parameters
        List<ItemStack> items = new ArrayList<>();
        if (parameters.containsKey("items")) {
            @SuppressWarnings("unchecked")
            Map<Integer, ItemStack> itemMap = (Map<Integer, ItemStack>) parameters.get("items");
            
            // Convert to a list (ignoring null items)
            for (int i = 0; i < itemMap.size(); i++) {
                ItemStack item = itemMap.get(i);
                if (item != null) {
                    items.add(item);
                }
            }
        } else if (parameters.containsKey("itemList")) {
            @SuppressWarnings("unchecked")
            List<ItemStack> itemList = (List<ItemStack>) parameters.get("itemList");
            items.addAll(itemList);
        }
        
        // Get handlers from parameters
        List<GUIClickHandler> handlers = new ArrayList<>();
        if (parameters.containsKey("handlers")) {
            @SuppressWarnings("unchecked")
            Map<Integer, GUIClickHandler> handlerMap = (Map<Integer, GUIClickHandler>) parameters.get("handlers");
            
            // Convert to a list
            for (int i = 0; i < items.size(); i++) {
                handlers.add(handlerMap.getOrDefault(i, null));
            }
        } else if (parameters.containsKey("handlerList")) {
            @SuppressWarnings("unchecked")
            List<GUIClickHandler> handlerList = (List<GUIClickHandler>) parameters.get("handlerList");
            handlers.addAll(handlerList);
        }
        
        // Create the GUI
        PaginatedGUI gui = new PaginatedGUI(moduleId, title, rows, theme, items, handlers);
        
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
