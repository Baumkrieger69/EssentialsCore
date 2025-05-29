package com.essentialscore.api.performance;

import org.bukkit.entity.Player;

/**
 * Interface for GUI representation in the module system
 */
public interface GUI {
    void open(Player player);
    void close();
}
