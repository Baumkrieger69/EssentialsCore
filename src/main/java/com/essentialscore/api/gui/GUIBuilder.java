package com.essentialscore.api.gui;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for constructing GUIs with a fluent API.
 */
public class GUIBuilder {
    
    private final GUIManager guiManager;
    private final String moduleId;
    private final String title;
    private final int rows;
    private GUITheme theme;
    private String factoryId = "standard";
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Map<Integer, GUIClickHandler> handlers = new HashMap<>();
    private final Map<String, Object> parameters = new HashMap<>();
    
    /**
     * Creates a new GUI builder.
     *
     * @param guiManager The GUI manager
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows
     */
    public GUIBuilder(GUIManager guiManager, String moduleId, String title, int rows) {
        this.guiManager = guiManager;
        this.moduleId = moduleId;
        this.title = title;
        this.rows = rows;
        this.theme = guiManager.getDefaultTheme();
    }
    
    /**
     * Sets the GUI theme.
     *
     * @param theme The theme
     * @return This builder
     */
    public GUIBuilder theme(GUITheme theme) {
        this.theme = theme;
        return this;
    }
    
    /**
     * Sets the GUI theme by ID.
     *
     * @param themeId The theme ID
     * @return This builder
     */
    public GUIBuilder theme(String themeId) {
        this.theme = guiManager.getTheme(themeId);
        return this;
    }
    
    /**
     * Sets the GUI factory.
     *
     * @param factoryId The factory ID
     * @return This builder
     */
    public GUIBuilder factory(String factoryId) {
        this.factoryId = factoryId;
        return this;
    }
    
    /**
     * Sets an item in the GUI.
     *
     * @param slot The slot
     * @param item The item
     * @return This builder
     */
    public GUIBuilder item(int slot, ItemStack item) {
        items.put(slot, item);
        return this;
    }
    
    /**
     * Sets an item with a click handler in the GUI.
     *
     * @param slot The slot
     * @param item The item
     * @param handler The click handler
     * @return This builder
     */
    public GUIBuilder item(int slot, ItemStack item, GUIClickHandler handler) {
        items.put(slot, item);
        handlers.put(slot, handler);
        return this;
    }
    
    /**
     * Creates a button with the theme's style and adds it to the GUI.
     *
     * @param slot The slot
     * @param name The button name
     * @param handler The click handler
     * @param lore The button lore
     * @return This builder
     */
    public GUIBuilder button(int slot, String name, GUIClickHandler handler, String... lore) {
        ItemStack button = theme.createButton(name, lore);
        return item(slot, button, handler);
    }
    
    /**
     * Fills a range of slots with the same item.
     *
     * @param startSlot The start slot (inclusive)
     * @param endSlot The end slot (inclusive)
     * @param item The item
     * @return This builder
     */
    public GUIBuilder fill(int startSlot, int endSlot, ItemStack item) {
        for (int slot = startSlot; slot <= endSlot; slot++) {
            items.put(slot, item);
        }
        return this;
    }
    
    /**
     * Fills all empty slots with the background item from the theme.
     *
     * @return This builder
     */
    public GUIBuilder fillEmpty() {
        ItemStack background = theme.getBackgroundItem();
        
        for (int slot = 0; slot < rows * 9; slot++) {
            if (!items.containsKey(slot)) {
                items.put(slot, background);
            }
        }
        
        return this;
    }
    
    /**
     * Adds a border around the GUI using the theme's border item.
     *
     * @return This builder
     */
    public GUIBuilder border() {
        ItemStack border = theme.getBorderItem();
        
        // Top and bottom rows
        for (int slot = 0; slot < 9; slot++) {
            items.put(slot, border);
            items.put((rows - 1) * 9 + slot, border);
        }
        
        // Left and right columns
        for (int row = 1; row < rows - 1; row++) {
            items.put(row * 9, border);
            items.put(row * 9 + 8, border);
        }
        
        return this;
    }
    
    /**
     * Applies a template to the GUI.
     *
     * @param templateId The template ID
     * @return This builder
     */
    public GUIBuilder template(String templateId) {
        parameters.put("template", templateId);
        return this;
    }
    
    /**
     * Adds a parameter for the GUI factory.
     *
     * @param key The parameter key
     * @param value The parameter value
     * @return This builder
     */
    public GUIBuilder parameter(String key, Object value) {
        parameters.put(key, value);
        return this;
    }
    
    /**
     * Builds the GUI.
     *
     * @return The built GUI
     */
    public GUI build() {
        // Store items and handlers in parameters
        parameters.put("items", items);
        parameters.put("handlers", handlers);
        parameters.put("theme", theme);
        
        // Get the factory
        GUIFactory factory = guiManager.getFactory(factoryId);
        
        if (factory == null) {
            throw new IllegalStateException("Unknown GUI factory: " + factoryId);
        }
        
        // Create the GUI
        return factory.create(moduleId, title, rows, parameters);
    }
} 
