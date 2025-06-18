package com.essentialscore.api.backup;

import java.io.File;
import java.util.Set;

/**
 * Interface for backup providers that handle backing up specific components.
 */
public interface BackupProvider {
    /**
     * Gets the provider ID.
     *
     * @return The provider ID
     */
    String getId();
    
    /**
     * Gets the provider display name.
     *
     * @return The provider display name
     */
    String getDisplayName();
    
    /**
     * Performs a full backup.
     *
     * @param backupSystem The backup system
     * @param backupDir The backup directory
     * @param context Additional context (null for full backups)
     * @return The set of files that were backed up
     * @throws Exception If an error occurs during backup
     */
    Set<String> performBackup(BackupSystem backupSystem, File backupDir, Object context) throws Exception;
    
    /**
     * Performs an incremental backup.
     *
     * @param backupSystem The backup system
     * @param backupDir The backup directory
     * @param previousBackupDir The previous backup directory
     * @return The set of files that were backed up
     * @throws Exception If an error occurs during backup
     */
    Set<String> performIncrementalBackup(BackupSystem backupSystem, File backupDir, File previousBackupDir) throws Exception;
    
    /**
     * Performs a restore operation.
     *
     * @param backupSystem The backup system
     * @param backupDir The backup directory
     * @param files The files to restore
     * @throws Exception If an error occurs during restore
     */
    void performRestore(BackupSystem backupSystem, File backupDir, Set<String> files) throws Exception;
} 
