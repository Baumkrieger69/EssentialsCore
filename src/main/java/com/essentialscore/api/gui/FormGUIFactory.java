package com.essentialscore.api.gui;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Factory for creating form GUIs.
 */
public class FormGUIFactory implements GUIFactory {
    
    private static final String ID = "form";
    private static final String DESCRIPTION = "Creates form GUIs for user input";
    
    private final GUIManager guiManager;
    
    /**
     * Creates a new form GUI factory.
     *
     * @param guiManager The GUI manager
     */
    public FormGUIFactory(GUIManager guiManager) {
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
        
        // Get submit and cancel handlers
        @SuppressWarnings("unchecked")
        Consumer<Map<String, Object>> submitHandler = parameters.containsKey("submitHandler") ? 
                (Consumer<Map<String, Object>>) parameters.get("submitHandler") : 
                null;
                
        Runnable cancelHandler = parameters.containsKey("cancelHandler") ? 
                (Runnable) parameters.get("cancelHandler") : 
                null;
        
        // Create the GUI
        FormGUI gui = new FormGUI(moduleId, title, rows, theme, submitHandler, cancelHandler);
        
        // Add fields if provided
        if (parameters.containsKey("fields")) {
            @SuppressWarnings("unchecked")
            Map<String, FormGUI.FormField> fields = (Map<String, FormGUI.FormField>) parameters.get("fields");
            
            for (Map.Entry<String, FormGUI.FormField> entry : fields.entrySet()) {
                gui.addField(entry.getKey(), entry.getValue());
            }
        }
        
        // Add validators if provided
        if (parameters.containsKey("validators")) {
            @SuppressWarnings("unchecked")
            Iterable<FormGUI.FormValidator> validators = (Iterable<FormGUI.FormValidator>) parameters.get("validators");
            
            for (FormGUI.FormValidator validator : validators) {
                gui.addValidator(validator);
            }
        }
        
        // Set initial values if provided
        if (parameters.containsKey("values")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) parameters.get("values");
            gui.setValues(values);
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
