package com.essentialscore.api.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI implementation that represents a form with input fields.
 */
public class FormGUI extends StandardGUI {
    
    private static final int SUBMIT_SLOT = 49; // Middle slot in the bottom row
    private static final int CANCEL_SLOT = 45; // First slot in the bottom row
    
    private final Map<String, FormField> fields;
    private final Consumer<Map<String, Object>> submitHandler;
    private final Runnable cancelHandler;
    private final List<FormValidator> validators;
    
    /**
     * Creates a new form GUI.
     *
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows
     * @param theme The GUI theme
     * @param submitHandler The handler for form submission
     * @param cancelHandler The handler for form cancellation
     */
    public FormGUI(String moduleId, String title, int rows, GUITheme theme, 
                   Consumer<Map<String, Object>> submitHandler, Runnable cancelHandler) {
        super(moduleId, title, rows, theme);
        
        this.fields = new HashMap<>();
        this.submitHandler = submitHandler;
        this.cancelHandler = cancelHandler;
        this.validators = new ArrayList<>();
        
        // Initialize buttons
        initializeButtons();
    }
    
    /**
     * Adds a field to the form.
     *
     * @param id The field ID
     * @param field The form field
     * @return This form
     */
    public FormGUI addField(String id, FormField field) {
        fields.put(id, field);
        addComponent(field.getComponent());
        return this;
    }
    
    /**
     * Adds a validator to the form.
     *
     * @param validator The validator
     * @return This form
     */
    public FormGUI addValidator(FormValidator validator) {
        validators.add(validator);
        return this;
    }
    
    /**
     * Gets the field values.
     *
     * @return The field values
     */
    public Map<String, Object> getValues() {
        Map<String, Object> values = new HashMap<>();
        
        for (Map.Entry<String, FormField> entry : fields.entrySet()) {
            String id = entry.getKey();
            FormField field = entry.getValue();
            values.put(id, field.getValue());
        }
        
        return values;
    }
    
    /**
     * Sets the field values.
     *
     * @param values The values to set
     */
    public void setValues(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String id = entry.getKey();
            Object value = entry.getValue();
            
            FormField field = fields.get(id);
            if (field != null) {
                field.setValue(value);
            }
        }
        
        // Update the display
        update(null);
    }
    
    /**
     * Validates the form.
     *
     * @return A list of validation errors, or an empty list if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // Validate each field
        for (Map.Entry<String, FormField> entry : fields.entrySet()) {
            FormField field = entry.getValue();
            
            if (field.isRequired() && !field.isValid()) {
                errors.add("Field '" + field.getLabel() + "' is required");
            }
        }
        
        // Run custom validators
        for (FormValidator validator : validators) {
            String error = validator.validate(getValues());
            if (error != null) {
                errors.add(error);
            }
        }
        
        return errors;
    }
    
    /**
     * Submits the form.
     *
     * @param player The player submitting the form
     */
    public void submit(Player player) {
        List<String> errors = validate();
        
        if (errors.isEmpty()) {
            // Form is valid, handle submission
            if (submitHandler != null) {
                submitHandler.accept(getValues());
            }
        } else {
            // Form is invalid, show errors
            showErrors(player, errors);
        }
    }
    
    /**
     * Cancels the form.
     */
    public void cancel() {
        if (cancelHandler != null) {
            cancelHandler.run();
        }
    }
    
    @Override
    public void onClick(Player player, int slot, InventoryClickEvent event) {
        if (slot == SUBMIT_SLOT) {
            submit(player);
            return;
        } else if (slot == CANCEL_SLOT) {
            cancel();
            return;
        }
        
        // Let the parent handle other clicks
        super.onClick(player, slot, event);
    }
    
    /**
     * Initializes the buttons.
     */
    private void initializeButtons() {
        // Submit button
        ItemStack submitButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta submitMeta = submitButton.getItemMeta();
        if (submitMeta != null) {
            submitMeta.setDisplayName(getTheme().formatText("Submit"));
            submitButton.setItemMeta(submitMeta);
        }
        setItem(SUBMIT_SLOT, submitButton);
        
        // Cancel button
        ItemStack cancelButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(getTheme().formatText("Cancel"));
            cancelButton.setItemMeta(cancelMeta);
        }
        setItem(CANCEL_SLOT, cancelButton);
    }
    
    /**
     * Shows validation errors to the player.
     *
     * @param player The player
     * @param errors The errors to show
     */
    private void showErrors(Player player, List<String> errors) {
        // Create an item to show errors
        ItemStack errorItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = errorItem.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(getTheme().formatText("Validation Errors"));
            
            List<String> lore = new ArrayList<>();
            for (String error : errors) {
                lore.add(getTheme().getSecondaryColor() + "• " + error);
            }
            
            meta.setLore(lore);
            errorItem.setItemMeta(meta);
        }
        
        // Show errors in the middle of the inventory
        int middleSlot = (getRows() * 9) / 2;
        setItem(middleSlot, errorItem);
    }
    
    /**
     * Interface for form fields.
     */
    public interface FormField {
        /**
         * Gets the field label.
         *
         * @return The label
         */
        String getLabel();
        
        /**
         * Gets the field component.
         *
         * @return The component
         */
        GUIComponent getComponent();
        
        /**
         * Gets the field value.
         *
         * @return The value
         */
        Object getValue();
        
        /**
         * Sets the field value.
         *
         * @param value The value to set
         */
        void setValue(Object value);
        
        /**
         * Checks if the field is required.
         *
         * @return true if the field is required
         */
        boolean isRequired();
        
        /**
         * Checks if the field is valid.
         *
         * @return true if the field is valid
         */
        boolean isValid();
    }
    
    /**
     * Interface for form validators.
     */
    @FunctionalInterface
    public interface FormValidator {
        /**
         * Validates the form values.
         *
         * @param values The form values
         * @return An error message, or null if valid
         */
        String validate(Map<String, Object> values);
    }
} 
