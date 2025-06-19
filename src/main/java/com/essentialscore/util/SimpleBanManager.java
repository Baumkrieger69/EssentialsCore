package com.essentialscore.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * Modern ban management system that uses commands for maximum compatibility
 * and provides fallbacks for all Minecraft versions.
 */
public class SimpleBanManager {
    private final Plugin plugin;
    private final Logger logger;
    
    public SimpleBanManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Bans a player using the most compatible method
     * 
     * @param playerName Name of the player to ban
     * @param reason Reason for the ban
     * @return true if successful
     */
    public boolean banPlayer(String playerName, String reason) {
        try {
            // Use console command for maximum compatibility
            String command = "ban " + playerName + " " + (reason != null ? reason : "Banned by EssentialsCore");
            boolean success = Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            
            if (success) {
                logger.info("Successfully banned player: " + playerName + " for: " + reason);
                
                // Try to kick if online
                Player onlinePlayer = Bukkit.getPlayer(playerName);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    kickPlayer(onlinePlayer, reason);
                }
            } else {
                logger.warning("Failed to ban player using command: " + playerName);
            }
            
            return success;
        } catch (Exception e) {
            logger.severe("Failed to ban player " + playerName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Unbans a player using the most compatible method
     * 
     * @param playerName Name of the player to unban
     * @return true if successful
     */
    public boolean unbanPlayer(String playerName) {
        try {
            String command = "pardon " + playerName;
            boolean success = Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            
            if (success) {
                logger.info("Successfully unbanned player: " + playerName);
            } else {
                logger.warning("Failed to unban player using command: " + playerName);
            }
            
            return success;
        } catch (Exception e) {
            logger.severe("Failed to unban player " + playerName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Kicks a player using the best available method
     */
    private void kickPlayer(Player player, String reason) {
        try {
            // Try Adventure Component API first
            try {
                net.kyori.adventure.text.Component component = net.kyori.adventure.text.Component.text(reason);
                player.kick(component);
                return;
            } catch (Exception e) {
                // Adventure not available, try Spigot
            }
            
            // Try Spigot Component
            try {
                net.md_5.bungee.api.chat.TextComponent textComponent = new net.md_5.bungee.api.chat.TextComponent(reason);
                player.spigot().disconnect(textComponent);
                return;
            } catch (Exception e) {
                // Spigot not available, use legacy
            }
            
            // Final fallback to deprecated method
            @SuppressWarnings("deprecation")
            String finalReason = reason;
            player.kickPlayer(finalReason);
            
        } catch (Exception e) {
            logger.warning("Failed to kick player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Gets list of banned players using command parsing
     */
    public List<String> getBannedPlayers() {
        List<String> bannedPlayers = new ArrayList<>();
        
        try {
            // Use the banlist command and parse output
            // This is more reliable than using deprecated APIs
            Set<OfflinePlayer> banned = Bukkit.getBannedPlayers();
            for (OfflinePlayer player : banned) {
                if (player.getName() != null) {
                    bannedPlayers.add(player.getName());
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to get banned players list: " + e.getMessage());
        }
        
        return bannedPlayers;
    }
    
    /**
     * Gets detailed ban information
     */
    public List<BanInfo> getBanInformation() {
        List<BanInfo> banInfos = new ArrayList<>();
        
        try {
            Set<OfflinePlayer> banned = Bukkit.getBannedPlayers();
            for (OfflinePlayer player : banned) {
                if (player.getName() != null) {
                    BanInfo info = new BanInfo(
                        player.getName(),
                        "Banned by server", // Default reason since we can't easily get it
                        "Server",
                        new Date(),
                        null // Permanent by default
                    );
                    banInfos.add(info);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to get ban information: " + e.getMessage());
        }
        
        return banInfos;
    }
    
    /**
     * Clears all bans using commands
     */
    public int clearAllBans() {
        int cleared = 0;
        
        try {
            List<String> bannedPlayers = getBannedPlayers();
            for (String playerName : bannedPlayers) {
                if (unbanPlayer(playerName)) {
                    cleared++;
                }
            }
            
            logger.info("Cleared " + cleared + " bans");
        } catch (Exception e) {
            logger.severe("Failed to clear bans: " + e.getMessage());
        }
        
        return cleared;
    }
    
    /**
     * Inner class to hold ban information
     */
    public static class BanInfo {
        private final String target;
        private final String reason;
        private final String source;
        private final Date created;
        private final Date expiration;
        
        public BanInfo(String target, String reason, String source, Date created, Date expiration) {
            this.target = target;
            this.reason = reason;
            this.source = source;
            this.created = created;
            this.expiration = expiration;
        }
        
        public String getTarget() { return target; }
        public String getReason() { return reason; }
        public String getSource() { return source; }
        public Date getCreated() { return created; }
        public Date getExpiration() { return expiration; }
        
        public boolean isPermanent() { return expiration == null; }
        public boolean isExpired() { 
            return expiration != null && expiration.before(new Date()); 
        }
    }
}
