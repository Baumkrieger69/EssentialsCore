package com.essentialscore.api.security;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Provides GDPR compliance features for data privacy and protection.
 */
public class GDPRComplianceManager {
    private static final Logger LOGGER = Logger.getLogger(GDPRComplianceManager.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    private final Plugin plugin;
    private final File dataExportDir;
    private final Set<UUID> dataDeletionRequests;
    private final AuditLogger auditLogger;
    private final Gson gson;
    
    // Data handlers for different types of player data
    private final List<DataHandler> dataHandlers;
    
    /**
     * Creates a new GDPR compliance manager.
     *
     * @param plugin The plugin
     * @param auditLogger The audit logger
     */
    public GDPRComplianceManager(Plugin plugin, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.dataExportDir = new File(plugin.getDataFolder(), "gdpr/exports");
        this.dataDeletionRequests = new HashSet<>();
        this.auditLogger = auditLogger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataHandlers = new ArrayList<>();
    }
    
    /**
     * Initializes the GDPR compliance manager.
     */
    public void initialize() {
        // Create export directory
        if (!dataExportDir.exists()) {
            dataExportDir.mkdirs();
        }
        
        // Load pending deletion requests
        loadDeletionRequests();
        
        LOGGER.info("GDPR compliance manager initialized");
    }
    
    /**
     * Registers a data handler.
     *
     * @param handler The data handler
     */
    public void registerDataHandler(DataHandler handler) {
        dataHandlers.add(handler);
        LOGGER.info("Registered GDPR data handler: " + handler.getDataType());
    }
    
    /**
     * Unregisters a data handler.
     *
     * @param handler The data handler
     */
    public void unregisterDataHandler(DataHandler handler) {
        dataHandlers.remove(handler);
    }
    
    /**
     * Creates a data export for a player.
     *
     * @param player The player
     * @return A future that completes when the export is done
     */
    public CompletableFuture<File> exportPlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        
        // Log the export request
        if (auditLogger != null) {
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.GDPR,
                    AuditLogger.Actions.DATA_EXPORTED,
                    player,
                    "Player data export requested"
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create export file
                String timestamp = DATE_FORMAT.format(new Date());
                File exportFile = new File(dataExportDir, playerName + "_" + timestamp + ".zip");
                
                // Create temp directory for export files
                File tempDir = new File(dataExportDir, "temp_" + playerId.toString());
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                
                // Collect data from all handlers
                Map<String, JsonObject> dataMap = new HashMap<>();
                
                for (DataHandler handler : dataHandlers) {
                    try {
                        JsonObject data = handler.exportData(playerId);
                        if (data != null && data.size() > 0) {
                            dataMap.put(handler.getDataType(), data);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error exporting data from handler: " + handler.getDataType(), e);
                    }
                }
                
                // Write each data type to a separate file
                for (Map.Entry<String, JsonObject> entry : dataMap.entrySet()) {
                    File dataFile = new File(tempDir, entry.getKey() + ".json");
                    try (FileWriter writer = new FileWriter(dataFile)) {
                        gson.toJson(entry.getValue(), writer);
                    }
                }
                
                // Create summary file with metadata
                JsonObject summary = new JsonObject();
                summary.addProperty("playerName", playerName);
                summary.addProperty("uuid", playerId.toString());
                summary.addProperty("exportDate", timestamp);
                summary.addProperty("exportVersion", "1.0");
                summary.addProperty("server", plugin.getServer().getName());
                summary.addProperty("plugin", plugin.getName());
                summary.addProperty("pluginVersion", plugin.getPluginMeta().getVersion());
                
                File summaryFile = new File(tempDir, "summary.json");
                try (FileWriter writer = new FileWriter(summaryFile)) {
                    gson.toJson(summary, writer);
                }
                
                // Create README file
                File readmeFile = new File(tempDir, "README.txt");
                try (FileWriter writer = new FileWriter(readmeFile)) {
                    writer.write("DATA EXPORT FOR " + playerName + "\n");
                    writer.write("Generated on " + timestamp + "\n\n");
                    writer.write("This export contains all personal data stored by " + plugin.getName() + ".\n");
                    writer.write("Each file represents a different type of data.\n\n");
                    writer.write("For questions about your data, please contact the server administrator.\n");
                }
                
                // Create ZIP file containing all export files
                createZipFile(tempDir, exportFile);
                
                // Clean up temp directory
                deleteDirectory(tempDir);
                
                // Log success
                if (auditLogger != null) {
                    auditLogger.logPlayerAction(
                            AuditLogger.Categories.GDPR,
                            AuditLogger.Actions.DATA_EXPORTED,
                            player,
                            "Player data export completed: " + exportFile.getName()
                    );
                }
                
                return exportFile;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error creating data export for player: " + playerName, e);
                
                // Log failure
                if (auditLogger != null) {
                    auditLogger.logPlayerAction(
                            AuditLogger.Categories.GDPR,
                            AuditLogger.Actions.DATA_EXPORTED,
                            player,
                            "Player data export failed: " + e.getMessage()
                    );
                }
                
                throw new RuntimeException("Failed to create data export", e);
            }
        });
    }
    
    /**
     * Submits a data deletion request for a player.
     *
     * @param player The player
     * @return True if the request was submitted
     */
    public boolean requestDataDeletion(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Add to deletion requests
        dataDeletionRequests.add(playerId);
        
        // Save deletion requests
        saveDeletionRequests();
        
        // Log the deletion request
        if (auditLogger != null) {
            auditLogger.logPlayerAction(
                    AuditLogger.Categories.GDPR,
                    AuditLogger.Actions.DATA_DELETED,
                    player,
                    "Player data deletion requested"
            );
        }
        
        return true;
    }
    
    /**
     * Cancels a data deletion request for a player.
     *
     * @param playerId The player ID
     * @return True if the request was canceled
     */
    public boolean cancelDataDeletionRequest(UUID playerId) {
        // Remove from deletion requests
        boolean canceled = dataDeletionRequests.remove(playerId);
        
        if (canceled) {
            // Save deletion requests
            saveDeletionRequests();
            
            // Log the cancellation
            if (auditLogger != null) {
                auditLogger.logSystemAction(
                        AuditLogger.Categories.GDPR,
                        "DELETION_CANCELED",
                        "Player data deletion request canceled for " + playerId
                );
            }
        }
        
        return canceled;
    }
    
    /**
     * Processes all pending data deletion requests.
     */
    public void processDeletionRequests() {
        Set<UUID> processed = new HashSet<>();
        
        for (UUID playerId : dataDeletionRequests) {
            // Get player info
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
            String playerName = player.getName() != null ? player.getName() : playerId.toString();
            
            LOGGER.info("Processing data deletion request for player: " + playerName);
            
            // Process all handlers
            boolean success = true;
            for (DataHandler handler : dataHandlers) {
                try {
                    boolean result = handler.deleteData(playerId);
                    if (!result) {
                        LOGGER.warning("Handler failed to delete data: " + handler.getDataType());
                        success = false;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error deleting data from handler: " + handler.getDataType(), e);
                    success = false;
                }
            }
            
            if (success) {
                // Mark as processed
                processed.add(playerId);
                
                // Log successful deletion
                if (auditLogger != null) {
                    auditLogger.logSystemAction(
                            AuditLogger.Categories.GDPR,
                            AuditLogger.Actions.DATA_DELETED,
                            "Player data deleted for " + playerName + " (" + playerId + ")"
                    );
                }
                
                LOGGER.info("Completed data deletion for player: " + playerName);
            } else {
                LOGGER.warning("Could not complete all data deletion for player: " + playerName);
                
                // Log partial deletion
                if (auditLogger != null) {
                    auditLogger.logSystemAction(
                            AuditLogger.Categories.GDPR,
                            "DATA_DELETION_PARTIAL",
                            "Player data partially deleted for " + playerName + " (" + playerId + ")"
                    );
                }
            }
        }
        
        // Remove processed requests
        dataDeletionRequests.removeAll(processed);
        
        // Save updated requests
        saveDeletionRequests();
    }
    
    /**
     * Checks if a player has a pending data deletion request.
     *
     * @param playerId The player ID
     * @return True if the player has a pending request
     */
    public boolean hasDeletionRequest(UUID playerId) {
        return dataDeletionRequests.contains(playerId);
    }
    
    /**
     * Gets all pending data deletion requests.
     *
     * @return The pending requests
     */
    public Set<UUID> getPendingDeletionRequests() {
        return new HashSet<>(dataDeletionRequests);
    }
    
    /**
     * Loads pending deletion requests from disk.
     */
    private void loadDeletionRequests() {
        File requestsFile = new File(plugin.getDataFolder(), "gdpr/deletion_requests.yml");
        
        if (!requestsFile.exists()) {
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(requestsFile);
            List<String> uuidStrings = config.getStringList("requests");
            
            for (String uuidString : uuidStrings) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    dataDeletionRequests.add(playerId);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Invalid UUID in deletion requests: " + uuidString);
                }
            }
            
            LOGGER.info("Loaded " + dataDeletionRequests.size() + " pending data deletion requests");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading deletion requests", e);
        }
    }
    
    /**
     * Saves pending deletion requests to disk.
     */
    private void saveDeletionRequests() {
        // Create directory if it doesn't exist
        File gdprDir = new File(plugin.getDataFolder(), "gdpr");
        if (!gdprDir.exists()) {
            gdprDir.mkdirs();
        }
        
        File requestsFile = new File(gdprDir, "deletion_requests.yml");
        
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            List<String> uuidStrings = new ArrayList<>();
            for (UUID playerId : dataDeletionRequests) {
                uuidStrings.add(playerId.toString());
            }
            
            config.set("requests", uuidStrings);
            config.save(requestsFile);
            
            LOGGER.fine("Saved " + dataDeletionRequests.size() + " pending data deletion requests");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving deletion requests", e);
        }
    }
    
    /**
     * Creates a ZIP file from a directory.
     *
     * @param sourceDir The source directory
     * @param zipFile The output ZIP file
     * @throws IOException If an I/O error occurs
     */
    private void createZipFile(File sourceDir, File zipFile) throws IOException {
        Path sourcePath = sourceDir.toPath();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            String relativePath = sourcePath.relativize(path).toString().replace('\\', '/');
                            ZipEntry entry = new ZipEntry(relativePath);
                            zos.putNextEntry(entry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Error adding file to ZIP: " + path, e);
                        }
                    });
        }
    }
    
    /**
     * Recursively deletes a directory.
     *
     * @param directory The directory to delete
     * @return True if deletion was successful
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
    
    /**
     * Gets metadata about stored personal data.
     *
     * @return A map of data types and descriptions
     */
    public Map<String, String> getDataTypeDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        
        for (DataHandler handler : dataHandlers) {
            descriptions.put(handler.getDataType(), handler.getDescription());
        }
        
        return descriptions;
    }
    
    /**
     * Interface for handling different types of player data.
     */
    public interface DataHandler {
        /**
         * Gets the type of data handled.
         *
         * @return The data type
         */
        String getDataType();
        
        /**
         * Gets a description of the data handled.
         *
         * @return The description
         */
        String getDescription();
        
        /**
         * Exports data for a player.
         *
         * @param playerId The player ID
         * @return The exported data as a JSON object
         * @throws Exception If an error occurs
         */
        JsonObject exportData(UUID playerId) throws Exception;
        
        /**
         * Deletes data for a player.
         *
         * @param playerId The player ID
         * @return True if deletion was successful
         * @throws Exception If an error occurs
         */
        boolean deleteData(UUID playerId) throws Exception;
    }
    
    /**
     * Abstract base class for data handlers.
     */
    public abstract static class AbstractDataHandler implements DataHandler {
        private final String dataType;
        private final String description;
        
        /**
         * Creates a new abstract data handler.
         *
         * @param dataType The data type
         * @param description The description
         */
        public AbstractDataHandler(String dataType, String description) {
            this.dataType = dataType;
            this.description = description;
        }
        
        @Override
        public String getDataType() {
            return dataType;
        }
        
        @Override
        public String getDescription() {
            return description;
        }
    }
} 
