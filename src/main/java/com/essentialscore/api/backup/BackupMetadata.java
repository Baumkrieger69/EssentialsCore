package com.essentialscore.api.backup;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents metadata for a backup.
 */
public class BackupMetadata {
    private final String backupId;
    private final BackupType type;
    private final Instant timestamp;
    private final String description;
    private final String parentBackupId;
    
    private Map<String, Set<String>> backupFiles = new HashMap<>();
    private boolean validated = false;
    private boolean valid = false;
    private String validationMessage;
    
    /**
     * Creates new backup metadata.
     *
     * @param backupId The backup ID
     * @param type The backup type
     * @param timestamp The backup timestamp
     * @param description The backup description
     * @param parentBackupId The parent backup ID (for incremental backups)
     */
    public BackupMetadata(String backupId, BackupType type, Instant timestamp, String description, String parentBackupId) {
        this.backupId = backupId;
        this.type = type;
        this.timestamp = timestamp;
        this.description = description;
        this.parentBackupId = parentBackupId;
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
     * Gets the backup type.
     *
     * @return The backup type
     */
    public BackupType getType() {
        return type;
    }
    
    /**
     * Gets the backup timestamp.
     *
     * @return The backup timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the backup description.
     *
     * @return The backup description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the parent backup ID.
     *
     * @return The parent backup ID
     */
    public String getParentBackupId() {
        return parentBackupId;
    }
    
    /**
     * Gets the backup files.
     *
     * @return The backup files
     */
    public Map<String, Set<String>> getBackupFiles() {
        return backupFiles;
    }
    
    /**
     * Sets the backup files.
     *
     * @param backupFiles The backup files
     */
    public void setBackupFiles(Map<String, Set<String>> backupFiles) {
        this.backupFiles = backupFiles;
    }
    
    /**
     * Checks if the backup has been validated.
     *
     * @return true if validated
     */
    public boolean isValidated() {
        return validated;
    }
    
    /**
     * Sets if the backup has been validated.
     *
     * @param validated Whether the backup has been validated
     */
    public void setValidated(boolean validated) {
        this.validated = validated;
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
     * Sets if the backup is valid.
     *
     * @param valid Whether the backup is valid
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    /**
     * Gets the validation message.
     *
     * @return The validation message
     */
    public String getValidationMessage() {
        return validationMessage;
    }
    
    /**
     * Sets the validation message.
     *
     * @param validationMessage The validation message
     */
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
    
    @Override
    public String toString() {
        return "BackupMetadata{" +
               "backupId='" + backupId + '\'' +
               ", type=" + type +
               ", timestamp=" + timestamp +
               ", description='" + description + '\'' +
               ", parentBackupId='" + parentBackupId + '\'' +
               ", validated=" + validated +
               ", valid=" + valid +
               '}';
    }
} 
