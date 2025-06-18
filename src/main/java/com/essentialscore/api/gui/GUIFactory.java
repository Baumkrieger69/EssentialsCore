package com.essentialscore.api.gui;

import java.util.Map;

/**
 * Interface for factories that create specialized GUI types.
 */
public interface GUIFactory {
    
    /**
     * Gets the factory ID.
     *
     * @return The factory ID
     */
    String getId();
    
    /**
     * Gets the factory description.
     *
     * @return The factory description
     */
    String getDescription();
    
    /**
     * Creates a GUI from the specified parameters.
     *
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows
     * @param parameters Additional parameters for this factory
     * @return The created GUI
     */
    GUI create(String moduleId, String title, int rows, Map<String, Object> parameters);
    
    /**
     * Gets the GUI manager.
     *
     * @return The GUI manager
     */
    GUIManager getGUIManager();
} 
