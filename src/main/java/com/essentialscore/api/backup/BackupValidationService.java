package com.essentialscore.api.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for validating backups and detecting corruption.
 */
public class BackupValidationService {
    private static final Logger LOGGER = Logger.getLogger(BackupValidationService.class.getName());
    
    private final Map<String, BackupValidationResult> validationCache = new ConcurrentHashMap<>();
    
    /**
     * Validates a backup.
     *
     * @param backupSystem The backup system
     * @param metadata The backup metadata
     * @return true if the backup is valid
     */
    public boolean validateBackup(BackupSystem backupSystem, BackupMetadata metadata) {
        String backupId = metadata.getBackupId();
        LOGGER.info("Validating backup: " + backupId);
        
        try {
            File backupDir = backupSystem.getBackupDirectory(backupId);
            if (!backupDir.exists()) {
                LOGGER.warning("Backup directory not found: " + backupDir.getPath());
                metadata.setValidationMessage("Backup directory not found");
                return false;
            }
            
            // Check each provider's files
            boolean valid = true;
            StringBuilder validationMessage = new StringBuilder();
            
            for (Map.Entry<String, Set<String>> entry : metadata.getBackupFiles().entrySet()) {
                String providerId = entry.getKey();
                Set<String> files = entry.getValue();
                
                if (files == null || files.isEmpty()) {
                    continue;
                }
                
                // Check if files exist
                for (String file : files) {
                    File backupFile = new File(backupDir, file);
                    if (!backupFile.exists()) {
                        LOGGER.warning("Missing backup file: " + backupFile.getPath());
                        if (validationMessage.length() > 0) {
                            validationMessage.append("; ");
                        }
                        validationMessage.append("Missing file for provider ").append(providerId).append(": ").append(file);
                        valid = false;
                    } else {
                        // Check file integrity
                        if (!validateFileIntegrity(backupFile)) {
                            LOGGER.warning("Corrupted backup file: " + backupFile.getPath());
                            if (validationMessage.length() > 0) {
                                validationMessage.append("; ");
                            }
                            validationMessage.append("Corrupted file for provider ").append(providerId).append(": ").append(file);
                            valid = false;
                        }
                    }
                }
            }
            
            // If this is an incremental backup, validate parent exists
            if (metadata.getType() == BackupType.INCREMENTAL) {
                String parentId = metadata.getParentBackupId();
                if (parentId == null) {
                    LOGGER.warning("Incremental backup without parent: " + backupId);
                    if (validationMessage.length() > 0) {
                        validationMessage.append("; ");
                    }
                    validationMessage.append("Incremental backup without parent");
                    valid = false;
                } else {
                    BackupMetadata parentMetadata = backupSystem.getMetadataManager().getMetadata(parentId);
                    if (parentMetadata == null) {
                        LOGGER.warning("Parent backup not found: " + parentId);
                        if (validationMessage.length() > 0) {
                            validationMessage.append("; ");
                        }
                        validationMessage.append("Parent backup not found: ").append(parentId);
                        valid = false;
                    }
                }
            }
            
            // Save validation result
            metadata.setValidationMessage(valid ? "Backup is valid" : validationMessage.toString());
            
            BackupValidationResult result = new BackupValidationResult(
                backupId,
                valid,
                metadata.getValidationMessage()
            );
            
            validationCache.put(backupId, result);
            
            LOGGER.info("Backup validation completed for " + backupId + ": " + (valid ? "VALID" : "INVALID"));
            return valid;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating backup: " + backupId, e);
            metadata.setValidationMessage("Validation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates the integrity of a backup file using SHA-256 checksum.
     *
     * @param file The file to validate
     * @return true if the file is valid
     */
    private boolean validateFileIntegrity(File file) {
        try {
            // Check if we have a cached result
            String filePath = file.getAbsolutePath();
            BackupValidationResult cachedResult = validationCache.get(filePath);
            
            if (cachedResult != null && cachedResult.isValid()) {
                long lastModified = file.lastModified();
                if (lastModified == cachedResult.getLastModified()) {
                    return true;
                }
            }
            
            // Calculate file checksum
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            try (FileInputStream fis = new FileInputStream(file)) {
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] checksum = digest.digest();
            
            // Store result in cache
            BackupValidationResult result = new BackupValidationResult(
                true,
                file.lastModified(),
                checksum
            );
            validationCache.put(filePath, result);
            
            return true;
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.log(Level.WARNING, "Failed to validate file integrity: " + file.getPath(), e);
            return false;
        }
    }
    
    /**
     * Gets the validation result for a backup.
     *
     * @param backupId The backup ID
     * @return The validation result, or null if not validated
     */
    public BackupValidationResult getValidationResult(String backupId) {
        return validationCache.get(backupId);
    }
    
    /**
     * Clears the validation cache.
     */
    public void clearCache() {
        validationCache.clear();
    }
    
    /**
     * Represents a backup validation result.
     */
    public static class BackupValidationResult {
        private final String backupId;
        private final boolean valid;
        private final String message;
        private final long lastModified;
        private final byte[] checksum;
        
        /**
         * Creates a new backup validation result.
         *
         * @param backupId The backup ID
         * @param valid Whether the backup is valid
         * @param message The validation message
         */
        public BackupValidationResult(String backupId, boolean valid, String message) {
            this.backupId = backupId;
            this.valid = valid;
            this.message = message;
            this.lastModified = 0;
            this.checksum = null;
        }
        
        /**
         * Creates a new backup validation result for file integrity.
         *
         * @param valid Whether the file is valid
         * @param lastModified The last modified timestamp
         * @param checksum The file checksum
         */
        public BackupValidationResult(boolean valid, long lastModified, byte[] checksum) {
            this.backupId = null;
            this.valid = valid;
            this.message = null;
            this.lastModified = lastModified;
            this.checksum = checksum;
        }
        
        /**
         * Gets the backup ID.
         *
         * @return The backup ID
         */
        public String getBackupId() {
            return backupId;
        }
        
        /**
         * Checks if the backup is valid.
         *
         * @return true if valid
         */
        public boolean isValid() {
            return valid;
        }
        
        /**
         * Gets the validation message.
         *
         * @return The validation message
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Gets the last modified timestamp.
         *
         * @return The last modified timestamp
         */
        public long getLastModified() {
            return lastModified;
        }
        
        /**
         * Gets the file checksum.
         *
         * @return The file checksum
         */
        public byte[] getChecksum() {
            return checksum;
        }
    }
}
