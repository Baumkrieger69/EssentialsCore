package com.essentialscore.api.gui;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Interface for defining GUI themes that provide consistent visual styling.
 */
public interface GUITheme {
    
    /**
     * Gets the theme name.
     *
     * @return The theme name
     */
    String getName();
    
    /**
     * Gets the theme description.
     *
     * @return The theme description
     */
    String getDescription();
    
    /**
     * Gets the primary color used by this theme.
     *
     * @return The primary color
     */
    TextColor getPrimaryColor();
    
    /**
     * Gets the secondary color used by this theme.
     *
     * @return The secondary color
     */
    TextColor getSecondaryColor();
    
    /**
     * Gets the accent color used by this theme.
     *
     * @return The accent color
     */
    TextColor getAccentColor();
    
    /**
     * Gets the border material used for GUI borders.
     *
     * @return The border material
     */
    Material getBorderMaterial();
    
    /**
     * Gets the background material used for empty slots.
     *
     * @return The background material
     */
    Material getBackgroundMaterial();
    
    /**
     * Gets the button material used for standard buttons.
     *
     * @return The button material
     */
    Material getButtonMaterial();
    
    /**
     * Gets a themed border item.
     *
     * @return The border item
     */
    ItemStack getBorderItem();
    
    /**
     * Gets a themed background item.
     *
     * @return The background item
     */
    ItemStack getBackgroundItem();
    
    /**
     * Creates a themed button with the specified name and lore.
     *
     * @param name The button name
     * @param lore The button lore lines
     * @return The button item
     */
    ItemStack createButton(String name, String... lore);
    
    /**
     * Creates a themed button with the specified material, name, and lore.
     *
     * @param material The button material
     * @param name The button name
     * @param lore The button lore lines
     * @return The button item
     */
    ItemStack createButton(Material material, String name, String... lore);
    
    /**
     * Applies the theme's style to an existing item.
     *
     * @param item The item to style
     * @param name The new name
     * @param lore The new lore
     * @return The styled item
     */
    ItemStack styleItem(ItemStack item, String name, String... lore);
    
    /**
     * Formats text according to the theme's style.
     *
     * @param text The text to format
     * @return The formatted text
     */
    String formatText(String text);
    
    /**
     * Gets the theme's default title prefix.
     *
     * @return The title prefix
     */
    String getTitlePrefix();
    
    /**
     * Gets the theme's default title suffix.
     *
     * @return The title suffix
     */
    String getTitleSuffix();
    
    /**
     * Formats a GUI title according to the theme's style.
     *
     * @param title The title to format
     * @return The formatted title
     */
    String formatTitle(String title);
} 
