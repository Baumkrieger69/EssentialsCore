package com.essentialscore.api.backup;

/**
 * Represents types of backups.
 */
public enum BackupType {
    /**
     * A full backup containing all data.
     */
    FULL,
    
    /**
     * An incremental backup containing only changes since the last backup.
     */
    INCREMENTAL
} 
