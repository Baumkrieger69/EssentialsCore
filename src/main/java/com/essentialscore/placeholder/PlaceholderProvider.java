package com.essentialscore.placeholder;

import org.bukkit.entity.Player;

/**
 * Interface for placeholder providers
 */
public interface PlaceholderProvider {
    
    /**
     * Get the value for a specific placeholder
     * 
     * @param placeholder The placeholder identifier (without brackets)
     * @param player The player requesting the placeholder (can be null for console)
     * @return The replacement value, or null if not handled by this provider
     */
    String getValue(String placeholder, Player player);
}
