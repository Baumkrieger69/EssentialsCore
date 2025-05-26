package com.essentialscore.api.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
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
    public ChatColor getPrimaryColor() {
        return ChatColor.BLUE;
    }
    
    @Override
    public ChatColor getSecondaryColor() {
        return ChatColor.GRAY;
    }
    
    @Override
    public ChatColor getAccentColor() {
        return ChatColor.GOLD;
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
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public ItemStack getBackgroundItem() {
        ItemStack item = new ItemStack(getBackgroundMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(" ");
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
            meta.setDisplayName(formatText(name));
            
            // Format the lore with the secondary color
            if (lore.length > 0) {
                List<String> formattedLore = new ArrayList<>();
                
                for (String line : lore) {
                    formattedLore.add(getSecondaryColor() + line);
                }
                
                meta.setLore(formattedLore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @Override
    public String formatText(String text) {
        return getPrimaryColor() + text;
    }
    
    @Override
    public String getTitlePrefix() {
        return getPrimaryColor() + "✦ ";
    }
    
    @Override
    public String getTitleSuffix() {
        return " " + getPrimaryColor() + "✦";
    }
    
    @Override
    public String formatTitle(String title) {
        return getTitlePrefix() + getAccentColor() + title + getTitleSuffix();
    }
} 