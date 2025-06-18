package com.essentialscore.api.backup;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provider for backing up player data.
 */
public class PlayerDataBackupProvider implements BackupProvider {
    private static final Logger LOGGER = Logger.getLogger(PlayerDataBackupProvider.class.getName());
    
    @Override
    public String getId() {
        return "player_data";
    }
    
    @Override
    public String getDisplayName() {
        return "Player Data";
    }
    
    @Override
    public Set<String> performBackup(BackupSystem backupSystem, File backupDir, Object context) throws Exception {
        LOGGER.info("Starting player data backup");
        Set<String> backedUpFiles = new HashSet<>();
        
        // Create directory for player data
        File playerDataDir = new File(backupDir, "playerdata");
        playerDataDir.mkdirs();
        
        // Get Minecraft server world directory
        File worldDir = getWorldDirectory();
        File playerDataSourceDir = new File(worldDir, "playerdata");
        
        if (!playerDataSourceDir.exists() || !playerDataSourceDir.isDirectory()) {
            LOGGER.warning("Player data directory not found: " + playerDataSourceDir.getPath());
            return backedUpFiles;
        }
        
        // Backup player data files
        File[] playerFiles = playerDataSourceDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                try {
                    File destFile = new File(playerDataDir, playerFile.getName());
                    Files.copy(playerFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    backedUpFiles.add("playerdata/" + playerFile.getName());
                    
                    // Get player name for logging if possible
                    String playerName = getPlayerNameFromUUID(playerFile.getName());
                    LOGGER.info("Backed up player data for " + (playerName != null ? playerName : playerFile.getName()));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to backup player data: " + playerFile.getName(), e);
                }
            }
        }
        
        LOGGER.info("Player data backup completed, backed up " + backedUpFiles.size() + " files");
        return backedUpFiles;
    }
    
    @Override
    public Set<String> performIncrementalBackup(BackupSystem backupSystem, File backupDir, File previousBackupDir) throws Exception {
        // For simplicity, we'll do a full backup of changed files only
        Set<String> backedUpFiles = new HashSet<>();
        
        // Create directory for player data
        File playerDataDir = new File(backupDir, "playerdata");
        playerDataDir.mkdirs();
        
        // Get previous backup directory
        File previousPlayerDataDir = new File(previousBackupDir, "playerdata");
        
        // Get Minecraft server world directory
        File worldDir = getWorldDirectory();
        File playerDataSourceDir = new File(worldDir, "playerdata");
        
        if (!playerDataSourceDir.exists() || !playerDataSourceDir.isDirectory()) {
            LOGGER.warning("Player data directory not found: " + playerDataSourceDir.getPath());
            return backedUpFiles;
        }
        
        // Backup player data files that have changed since last backup
        File[] playerFiles = playerDataSourceDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                File previousFile = new File(previousPlayerDataDir, playerFile.getName());
                
                // If file doesn't exist in previous backup or has been modified, back it up
                if (!previousFile.exists() || previousFile.lastModified() < playerFile.lastModified()) {
                    try {
                        File destFile = new File(playerDataDir, playerFile.getName());
                        Files.copy(playerFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        backedUpFiles.add("playerdata/" + playerFile.getName());
                        
                        // Get player name for logging if possible
                        String playerName = getPlayerNameFromUUID(playerFile.getName());
                        LOGGER.info("Backed up player data for " + (playerName != null ? playerName : playerFile.getName()));
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to backup player data: " + playerFile.getName(), e);
                    }
                }
            }
        }
        
        LOGGER.info("Player data incremental backup completed, backed up " + backedUpFiles.size() + " files");
        return backedUpFiles;
    }
    
    @Override
    public void performRestore(BackupSystem backupSystem, File backupDir, Set<String> files) throws Exception {
        LOGGER.info("Restoring player data");
        
        File playerDataDir = new File(backupDir, "playerdata");
        if (!playerDataDir.exists() || !playerDataDir.isDirectory()) {
            LOGGER.warning("Player data backup directory not found: " + playerDataDir.getPath());
            return;
        }
        
        // Get Minecraft server world directory
        File worldDir = getWorldDirectory();
        File playerDataTargetDir = new File(worldDir, "playerdata");
        
        if (!playerDataTargetDir.exists()) {
            playerDataTargetDir.mkdirs();
        }
        
        // Restore player data files
        for (String relativePath : files) {
            if (relativePath.startsWith("playerdata/")) {
                String fileName = relativePath.substring("playerdata/".length());
                File sourceFile = new File(playerDataDir, fileName);
                
                if (sourceFile.exists()) {
                    try {
                        File targetFile = new File(playerDataTargetDir, fileName);
                        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        
                        // Get player name for logging if possible
                        String playerName = getPlayerNameFromUUID(fileName);
                        LOGGER.info("Restored player data for " + (playerName != null ? playerName : fileName));
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to restore player data: " + fileName, e);
                    }
                } else {
                    LOGGER.warning("Player data file not found in backup: " + sourceFile.getPath());
                }
            }
        }
    }
    
    /**
     * Gets the Minecraft world directory.
     *
     * @return The world directory
     */
    private File getWorldDirectory() {
        // This would normally be obtained from the Bukkit API
        // Use standard directory structure
        return new File(Bukkit.getWorldContainer(), Bukkit.getWorlds().get(0).getName());
    }
    
    /**
     * Gets a player name from a UUID filename.
     *
     * @param fileName The UUID filename (e.g., "12345678-1234-1234-1234-123456789012.dat")
     * @return The player name or null if not found
     */
    private String getPlayerNameFromUUID(String fileName) {
        try {
            // Extract UUID from filename
            String uuidStr = fileName;
            if (uuidStr.endsWith(".dat")) {
                uuidStr = uuidStr.substring(0, uuidStr.length() - 4);
            }
            
            UUID uuid = UUID.fromString(uuidStr);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            
            return player.getName();
        } catch (Exception e) {
            return null;
        }
    }
} 
