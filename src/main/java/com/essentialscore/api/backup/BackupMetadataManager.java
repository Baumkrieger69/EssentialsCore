package com.essentialscore.api.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages backup metadata storage and retrieval.
 */
public class BackupMetadataManager {
    private static final Logger LOGGER = Logger.getLogger(BackupMetadataManager.class.getName());
    
    private final File metadataDir;
    private final Gson gson;
    private final Map<String, BackupMetadata> metadataCache;
    
    /**
     * Creates a new backup metadata manager.
     *
     * @param metadataDir The metadata directory
     */
    public BackupMetadataManager(File metadataDir) {
        this.metadataDir = metadataDir;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.metadataCache = new ConcurrentHashMap<>();
        
        if (!metadataDir.exists()) {
            metadataDir.mkdirs();
        }
        
        // Load existing metadata
        loadAllMetadata();
    }
    
    /**
     * Loads all metadata from disk.
     */
    private void loadAllMetadata() {
        File[] metadataFiles = metadataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (metadataFiles != null) {
            for (File file : metadataFiles) {
                try {
                    BackupMetadata metadata = loadMetadataFromFile(file);
                    if (metadata != null) {
                        metadataCache.put(metadata.getBackupId(), metadata);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load metadata file: " + file.getName(), e);
                }
            }
        }
        
        LOGGER.info("Loaded " + metadataCache.size() + " backup metadata records");
    }
    
    /**
     * Loads metadata from a file.
     *
     * @param file The metadata file
     * @return The backup metadata
     * @throws IOException If an error occurs
     */
    private BackupMetadata loadMetadataFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(reader, mapType);
            
            String backupId = (String) data.get("backupId");
            String typeStr = (String) data.get("type");
            String timestampStr = (String) data.get("timestamp");
            String description = (String) data.get("description");
            String parentBackupId = (String) data.get("parentBackupId");
            
            BackupType type = BackupType.valueOf(typeStr);
            Instant timestamp = Instant.parse(timestampStr);
            
            BackupMetadata metadata = new BackupMetadata(backupId, type, timestamp, description, parentBackupId);
            
            // Load backup files
            if (data.containsKey("backupFiles")) {
                Map<String, Set<String>> backupFiles = new HashMap<>();
                
                @SuppressWarnings("unchecked")
                Map<String, List<String>> rawBackupFiles = (Map<String, List<String>>) data.get("backupFiles");
                
                for (Map.Entry<String, List<String>> entry : rawBackupFiles.entrySet()) {
                    Set<String> fileSet = new HashSet<>(entry.getValue());
                    backupFiles.put(entry.getKey(), fileSet);
                }
                
                metadata.setBackupFiles(backupFiles);
            }
            
            // Load validation status
            if (data.containsKey("validated")) {
                metadata.setValidated((Boolean) data.get("validated"));
            }
            
            if (data.containsKey("valid")) {
                metadata.setValid((Boolean) data.get("valid"));
            }
            
            if (data.containsKey("validationMessage")) {
                metadata.setValidationMessage((String) data.get("validationMessage"));
            }
            
            return metadata;
        }
    }
    
    /**
     * Saves metadata to disk.
     *
     * @param metadata The backup metadata
     * @throws IOException If an error occurs
     */
    public void saveMetadata(BackupMetadata metadata) throws IOException {
        // Update cache
        metadataCache.put(metadata.getBackupId(), metadata);
        
        // Convert to map for JSON serialization
        Map<String, Object> data = new HashMap<>();
        data.put("backupId", metadata.getBackupId());
        data.put("type", metadata.getType().name());
        data.put("timestamp", metadata.getTimestamp().toString());
        data.put("description", metadata.getDescription());
        data.put("parentBackupId", metadata.getParentBackupId());
        data.put("backupFiles", metadata.getBackupFiles());
        data.put("validated", metadata.isValidated());
        data.put("valid", metadata.isValid());
        data.put("validationMessage", metadata.getValidationMessage());
        
        // Save to file
        File metadataFile = new File(metadataDir, metadata.getBackupId() + ".json");
        try (FileWriter writer = new FileWriter(metadataFile)) {
            gson.toJson(data, writer);
        }
    }
    
    /**
     * Gets metadata by backup ID.
     *
     * @param backupId The backup ID
     * @return The backup metadata, or null if not found
     */
    public BackupMetadata getMetadata(String backupId) {
        return metadataCache.get(backupId);
    }
    
    /**
     * Deletes metadata.
     *
     * @param backupId The backup ID
     */
    public void deleteMetadata(String backupId) {
        // Remove from cache
        metadataCache.remove(backupId);
        
        // Delete file
        File metadataFile = new File(metadataDir, backupId + ".json");
        if (metadataFile.exists()) {
            metadataFile.delete();
        }
    }
    
    /**
     * Gets all metadata.
     *
     * @return A list of all backup metadata
     */
    public List<BackupMetadata> getAllMetadata() {
        return new ArrayList<>(metadataCache.values());
    }
    
    /**
     * Gets the latest backup.
     *
     * @return The latest backup metadata, or null if none exist
     */
    public BackupMetadata getLatestBackup() {
        if (metadataCache.isEmpty()) {
            return null;
        }
        
        return metadataCache.values().stream()
            .max((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .orElse(null);
    }
    
    /**
     * Gets the latest full backup.
     *
     * @return The latest full backup metadata, or null if none exist
     */
    public BackupMetadata getLatestFullBackup() {
        if (metadataCache.isEmpty()) {
            return null;
        }
        
        return metadataCache.values().stream()
            .filter(m -> m.getType() == BackupType.FULL)
            .max((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .orElse(null);
    }
    
    /**
     * Gets backups by type.
     *
     * @param type The backup type
     * @return A list of backups of the specified type
     */
    public List<BackupMetadata> getBackupsByType(BackupType type) {
        return metadataCache.values().stream()
            .filter(m -> m.getType() == type)
            .collect(Collectors.toList());
    }
    
    /**
     * Finds the backup closest to a point in time.
     *
     * @param pointInTime The point in time
     * @return The closest backup metadata, or null if none exist
     */
    public BackupMetadata findBackupClosestTo(Instant pointInTime) {
        if (metadataCache.isEmpty()) {
            return null;
        }
        
        // Get all backups before or at the point in time
        List<BackupMetadata> eligibleBackups = metadataCache.values().stream()
            .filter(m -> !m.getTimestamp().isAfter(pointInTime))
            .collect(Collectors.toList());
        
        if (eligibleBackups.isEmpty()) {
            // If no backups are before the point in time, get the earliest backup
            return metadataCache.values().stream()
                .min((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .orElse(null);
        }
        
        // Find the backup closest to the point in time
        return eligibleBackups.stream()
            .max((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .orElse(null);
    }
    
    /**
     * Gets the metadata directory.
     *
     * @return The metadata directory
     */
    public File getMetadataDir() {
        return metadataDir;
    }
} 
