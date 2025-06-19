package com.essentialscore.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * Modern ban management system that handles deprecated API calls gracefully
 * and provides fallbacks for older Minecraft versions.
 */
public class ModernBanManager {
    private final Plugin plugin;
    private final Logger logger;
    
    public ModernBanManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Bans a player with modern API if available, falls back to legacy
     * 
     * @param playerName Name of the player to ban
     * @param reason Reason for the ban
     * @return true if successful
     */
    public boolean banPlayer(String playerName, String reason) {
        try {
            // Try modern PlayerBanList API first
            return banPlayerModern(playerName, reason);
        } catch (Exception e) {
            // Fallback to legacy API
            logger.warning("Modern ban API not available, using legacy method: " + e.getMessage());
            return banPlayerLegacy(playerName, reason);
        }
    }    /**
     * Modern ban implementation using newer APIs
     */
    @SuppressWarnings("deprecation")
    private boolean banPlayerModern(String playerName, String reason) {
        try {
            // Try to get player by name or UUID
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            Player onlinePlayer = Bukkit.getPlayer(playerName);
            
            // Use the simplest working approach - command execution
            boolean success = Bukkit.getServer().dispatchCommand(
                Bukkit.getConsoleSender(), 
                "ban " + playerName + " " + reason
            );
            
            // If command failed, try direct API
            if (!success) {
                try {
                    // Use OfflinePlayer.banPlayer() if available (newer versions)
                    offlinePlayer.banPlayer(reason);
                    success = true;
                } catch (Exception ex) {
                    // Final fallback to BanList
                    org.bukkit.BanList<?> banList = Bukkit.getServer().getBanList(org.bukkit.BanList.Type.NAME);
                    banList.addBan(playerName, reason);
                    success = true;
                }
            }
            
            // Kick online player using modern method
            if (success && onlinePlayer != null && onlinePlayer.isOnline()) {
                kickPlayerModern(onlinePlayer, reason);
            }
            
            return success;
        } catch (Exception e) {
            logger.severe("Failed to ban player with modern API: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Legacy ban implementation for older versions
     */
    @SuppressWarnings("deprecation")
    private boolean banPlayerLegacy(String playerName, String reason) {
        try {
            // Use console command as most reliable method
            boolean success = Bukkit.getServer().dispatchCommand(
                Bukkit.getConsoleSender(), 
                "ban " + playerName + " " + reason
            );
            
            if (!success) {
                // Fallback to BanList API
                org.bukkit.BanList<?> banList = Bukkit.getServer().getBanList(org.bukkit.BanList.Type.NAME);
                banList.addBan(playerName, reason);
                success = true;
            }
            
            // Kick player if online
            Player player = Bukkit.getPlayer(playerName);
            if (success && player != null && player.isOnline()) {
                player.kickPlayer(reason);
            }
            
            return true;
        } catch (Exception e) {
            logger.severe("Failed to ban player with legacy API: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Kicks a player using the best available method
     */
    private void kickPlayerModern(Player player, String reason) {
        try {
            // Try Adventure Component API first
            Class.forName("net.kyori.adventure.text.Component");
            net.kyori.adventure.text.Component component = net.kyori.adventure.text.Component.text(reason);
            player.kick(component);
        } catch (ClassNotFoundException | NoSuchMethodError e) {
            // Fallback to legacy kick
            try {
                // Try Spigot Component
                net.md_5.bungee.api.chat.TextComponent textComponent = new net.md_5.bungee.api.chat.TextComponent(reason);
                player.spigot().disconnect(textComponent);
            } catch (Exception ex) {
                // Final fallback to deprecated method
                @SuppressWarnings("deprecation")
                String finalReason = reason;
                player.kickPlayer(finalReason);
            }
        }
    }
    
    /**
     * Unbans a player with modern API support
     */
    public boolean unbanPlayer(String playerName) {
        try {
            return unbanPlayerModern(playerName);
        } catch (Exception e) {
            logger.warning("Modern unban API not available, using legacy method: " + e.getMessage());
            return unbanPlayerLegacy(playerName);
        }
    }
    
    @SuppressWarnings("deprecation")
    private boolean unbanPlayerModern(String playerName) {
        try {
            org.bukkit.BanList banList = Bukkit.getServer().getBanList(org.bukkit.BanList.Type.NAME);
            banList.pardon(playerName);
            return true;
        } catch (Exception e) {
            logger.severe("Failed to unban player: " + e.getMessage());
            return false;
        }
    }
    
    @SuppressWarnings("deprecation")
    private boolean unbanPlayerLegacy(String playerName) {
        try {
            org.bukkit.BanList banList = Bukkit.getServer().getBanList(org.bukkit.BanList.Type.NAME);
            banList.pardon(playerName);
            return true;
        } catch (Exception e) {
            logger.severe("Failed to unban player with legacy API: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets list of banned players with fallback support
     */
    @SuppressWarnings("deprecation")
    public List<String> getBannedPlayers() {
        List<String> bannedPlayers = new ArrayList<>();
        
        try {
            org.bukkit.BanList banList = Bukkit.getServer().getBanList(org.bukkit.BanList.Type.NAME);
            Set<org.bukkit.BanEntry> banEntries = banList.getBanEntries();
            
            for (org.bukkit.BanEntry entry : banEntries) {
                try {
                    // Try modern method first
                    String target = entry.getTarget();
                    bannedPlayers.add(target);
                } catch (Exception e) {
                    // Fallback for very old versions
                    bannedPlayers.add(entry.toString());
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
    @SuppressWarnings("deprecation")
    public List<BanInfo> getBanInformation() {
        List<BanInfo> banInfos = new ArrayList<>();
        
        try {
            org.bukkit.BanList banList = Bukkit.getServer().getBanList(org.bukkit.BanList.Type.NAME);
            Set<org.bukkit.BanEntry> banEntries = banList.getBanEntries();
            
            for (org.bukkit.BanEntry entry : banEntries) {
                try {
                    BanInfo info = new BanInfo(
                        entry.getTarget(),
                        entry.getReason() != null ? entry.getReason() : "No reason specified",
                        entry.getSource() != null ? entry.getSource() : "Unknown",
                        entry.getCreated(),
                        entry.getExpiration()
                    );
                    banInfos.add(info);
                } catch (Exception e) {
                    // Skip problematic entries
                    logger.warning("Skipped problematic ban entry: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to get ban information: " + e.getMessage());
        }
        
        return banInfos;
    }
    
    /**
     * Clears all bans
     */
    @SuppressWarnings("deprecation")
    public int clearAllBans() {
        int cleared = 0;
        
        try {
            org.bukkit.BanList banList = Bukkit.getServer().getBanList(org.bukkit.BanList.Type.NAME);
            Set<org.bukkit.BanEntry> banEntries = new HashSet<>(banList.getBanEntries());
            
            for (org.bukkit.BanEntry entry : banEntries) {
                try {
                    String target = entry.getTarget();
                    banList.pardon(target);
                    cleared++;
                } catch (Exception e) {
                    logger.warning("Failed to clear ban for entry: " + e.getMessage());
                }
            }
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
