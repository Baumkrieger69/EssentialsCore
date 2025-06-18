package com.essentialscore.api.gui;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of GUITheme with a clean, modern look.
 */
public class DefaultGUITheme implements GUITheme {
    
    private static final String NAME = "Default";
    private static final String DESCRIPTION = "The default EssentialsCore GUI theme";
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public TextColor getPrimaryColor() {
        return TextColor.color(0x3366CC); // Blue color
    }
    
    @Override
    public TextColor getSecondaryColor() {
        return TextColor.color(0x808080); // Gray color
    }
    
    @Override
    public TextColor getAccentColor() {
        return TextColor.color(0xFFAA00); // Gold color
    }
    
    @Override
    public Material getBorderMaterial() {
        return Material.BLACK_STAINED_GLASS_PANE;
    }
    
    @Override
    public Material getBackgroundMaterial() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }
    
    @Override
    public Material getButtonMaterial() {
        return Material.BLUE_STAINED_GLASS_PANE;
    }
    
    @Override
    public ItemStack getBorderItem() {
        ItemStack item = new ItemStack(getBorderMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public ItemStack getBackgroundItem() {
        ItemStack item = new ItemStack(getBackgroundMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public ItemStack createButton(String name, String... lore) {
        return createButton(getButtonMaterial(), name, lore);
    }
    
    @Override
    public ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        return styleItem(item, name, lore);
    }
    
    @Override
    public ItemStack styleItem(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set the display name with the primary color
            meta.displayName(Component.text(name).color(getPrimaryColor()));
            
            // Format the lore with the secondary color
            if (lore.length > 0) {
                List<Component> formattedLore = new ArrayList<>();
                
                for (String line : lore) {
                    formattedLore.add(Component.text(line).color(getSecondaryColor()));
                }
                
                meta.lore(formattedLore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public String formatText(String text) {
        // Format the text with the primary color's hex representation
        String hexColor = String.format("#%06x", getPrimaryColor().value() & 0xFFFFFF);
        return hexColor + text;
    }
    
    @Override
    public String getTitlePrefix() {
        // Format the prefix with the primary color's hex representation
        String hexColor = String.format("#%06x", getPrimaryColor().value() & 0xFFFFFF);
        return hexColor + "✦ ";
    }
    
    @Override
    public String getTitleSuffix() {
        // Format the suffix with the primary color's hex representation
        String hexColor = String.format("#%06x", getPrimaryColor().value() & 0xFFFFFF);
        return " " + hexColor + "✦";
    }
    
    @Override
    public String formatTitle(String title) {
        return getTitlePrefix() + getAccentColor() + title + getTitleSuffix();
    }
} 
