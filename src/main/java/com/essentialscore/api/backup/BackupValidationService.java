package com.essentialscore.api.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
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
     * Validates file integrity.
     *
     * @param file The file to validate
     * @return true if the file is valid
     */
    private boolean validateFileIntegrity(File file) {
        try {
            // Simple check: verify file is readable and not empty
            if (file.length() == 0) {
                return false;
            }
            
            // For certain file types, perform more thorough validation
            String fileName = file.getName().toLowerCase();
            
            if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
                // TODO: Add ZIP file validation
                return true;
            } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml") || 
                    fileName.endsWith(".json") || fileName.endsWith(".xml")) {
                // TODO: Add format validation for config files
                return true;
            }
            
            // By default, just check that we can read the file
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                in.read(buffer);
                return true;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read file during validation: " + file.getPath(), e);
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error validating file integrity: " + file.getPath(), e);
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
    }
} 