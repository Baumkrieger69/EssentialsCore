package com.essentialscore.api.gui;

/**
 * Interface for GUI templates that provide predefined layouts.
 */
public interface GUITemplate {
    
    /**
     * Gets the template ID.
     *
     * @return The template ID
     */
    String getId();
    
    /**
     * Gets the template description.
     *
     * @return The template description
     */
    String getDescription();
    
    /**
     * Gets the recommended number of rows for this template.
     *
     * @return The recommended number of rows
     */
    int getRecommendedRows();
    
    /**
     * Applies this template to a GUI.
     *
     * @param gui The GUI to apply the template to
     */
    void apply(GUI gui);
} 
