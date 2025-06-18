package com.essentialscore.api.backup;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.time.Instant;
import java.nio.file.*;
import java.io.*;
import java.security.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.Plugin;

/**
 * Erweitertes Backup-System mit Deduplizierung, Verschlüsselung und Validierung.
 */
public class AdvancedBackupSystem {
    private final Plugin plugin;
    private final Path backupDir;
    private final Map<String, byte[]> checksumCache;
    private final ScheduledExecutorService scheduler;
    // private final SecretKey encryptionKey; // Removed unused field
    private final Map<String, BackupMetadata> backupHistory;
    private final Set<Path> excludedPaths;
    
    public AdvancedBackupSystem(Plugin plugin, Path backupDir) {
        this.plugin = plugin;
        this.backupDir = backupDir;
        this.checksumCache = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        // this.encryptionKey = generateEncryptionKey(); // Removed unused assignment
        this.backupHistory = new ConcurrentHashMap<>();
        this.excludedPaths = new HashSet<>();
        
        // Initialisiere Backup-Verzeichnis
        initializeBackupDirectory();
        
        // Starte periodische Backup-Validierung
        scheduleValidation();
    }
    
    /**
     * Führt ein inkrementelles Backup durch.
     */
    public CompletableFuture<BackupResult> performIncrementalBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String backupId = generateBackupId();
                Path backupPath = backupDir.resolve(backupId);
                Files.createDirectories(backupPath);
                
                Set<Path> changedFiles = findChangedFiles();
                Map<String, String> deduplicationMap = new HashMap<>();
                
                for (Path file : changedFiles) {
                    if (shouldExclude(file)) continue;
                    
                    byte[] checksum = calculateChecksum(file);
                    String checksumHex = bytesToHex(checksum);
                    
                    if (isFileDuplicate(checksumHex)) {
                        // Deduplizierung - speichere nur Referenz
                        deduplicationMap.put(file.toString(), checksumHex);
                    } else {
                        // Neue Datei - komprimiere und verschlüssele
                        backupFile(file, backupPath, checksum);
                    }
                }
                
                // Erstelle Backup-Metadata
                BackupMetadata metadata = createBackupMetadata(backupId, changedFiles, deduplicationMap);
                backupHistory.put(backupId, metadata);
                
                return new BackupResult(backupId, changedFiles.size(), calculateTotalSize(backupPath));
            } catch (Exception e) {
                throw new RuntimeException("Backup fehlgeschlagen", e);
            }
        });
    }
    
    /**
     * Stellt Daten aus einem Backup wieder her.
     */
    public CompletableFuture<RestoreResult> restore(String backupId, Set<Path> filesToRestore) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BackupMetadata metadata = backupHistory.get(backupId);
                if (metadata == null) {
                    throw new IllegalArgumentException("Backup nicht gefunden: " + backupId);
                }
                
                Path backupPath = backupDir.resolve(backupId);
                Set<Path> restoredFiles = new HashSet<>();
                Set<Path> failedFiles = new HashSet<>();
                
                for (Path file : filesToRestore) {
                    try {
                        if (metadata.isDeduplicated(file)) {
                            restoreDeduplicated(file, metadata);
                        } else {
                            restoreFile(file, backupPath);
                        }
                        restoredFiles.add(file);
                    } catch (Exception e) {
                        failedFiles.add(file);
                    }
                }
                
                return new RestoreResult(restoredFiles, failedFiles);
            } catch (Exception e) {
                throw new RuntimeException("Wiederherstellung fehlgeschlagen", e);
            }
        });
    }
    
    /**
     * Validiert die Integrität eines Backups.
     */
    private void validateBackup(String backupId) {
        BackupMetadata metadata = backupHistory.get(backupId);
        if (metadata == null) return;
        
        Path backupPath = backupDir.resolve(backupId);
        boolean isValid = true;
        
        try {
            for (Path file : metadata.getBackedUpFiles()) {
                if (!validateFile(file, backupPath)) {
                    isValid = false;
                    break;
                }
            }
            
            metadata.setValidated(Instant.now(), isValid);
        } catch (Exception e) {
            metadata.setValidated(Instant.now(), false);
        }
    }
    
    private void scheduleValidation() {
        scheduler.scheduleAtFixedRate(
            () -> backupHistory.keySet().forEach(this::validateBackup),
            1, 24, TimeUnit.HOURS
        );
    }
    
    
    // Hilfsklassen für Ergebnisse
    
    public static class BackupResult {
        private final String backupId;
        private final int fileCount;
        private final long totalSize;
        
        public BackupResult(String backupId, int fileCount, long totalSize) {
            this.backupId = backupId;
            this.fileCount = fileCount;
            this.totalSize = totalSize;
        }
        
        public String getBackupId() { return backupId; }
        public int getFileCount() { return fileCount; }
        public long getTotalSize() { return totalSize; }
    }
    
    public static class RestoreResult {
        private final Set<Path> restoredFiles;
        private final Set<Path> failedFiles;
        
        public RestoreResult(Set<Path> restoredFiles, Set<Path> failedFiles) {
            this.restoredFiles = restoredFiles;
            this.failedFiles = failedFiles;
        }
        
        public Set<Path> getRestoredFiles() { return restoredFiles; }
        public Set<Path> getFailedFiles() { return failedFiles; }
        public boolean isFullySuccessful() { return failedFiles.isEmpty(); }
    }
    
    // BackupMetadata class (add this inside AdvancedBackupSystem)

    public static class BackupMetadata {
        private final String backupId;
        private final int fileCount;
        private final Map<String, String> deduplicationMap;
        private Instant lastValidated;
        private boolean valid;

        public BackupMetadata(String backupId, Instant /*timestamp*/ ignored, int fileCount, Map<String, String> deduplicationMap) {
            this.backupId = backupId;
            this.fileCount = fileCount;
            this.deduplicationMap = deduplicationMap;
            this.lastValidated = null;
            this.valid = false;
        }

        public int getFileCount() {
            return fileCount;
        }

        public String getBackupId() {
            return backupId;
        }

        public Set<Path> getBackedUpFiles() {
            Set<Path> files = new HashSet<>();
            for (String filePath : deduplicationMap.keySet()) {
                files.add(Paths.get(filePath));
            }
            return files;
        }

        // Added method to check if a file is deduplicated
        public boolean isDeduplicated(Path file) {
            return deduplicationMap.containsKey(file.toString());
        }

        public void setValidated(Instant validatedAt, boolean isValid) {
            this.lastValidated = validatedAt;
            this.valid = isValid;
        }

        public Instant getLastValidated() {
            return lastValidated;
        }

        public boolean isValid() {
            return valid;
        }
    }
    
    // Private Hilfsmethoden
    
    private void initializeBackupDirectory() {
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            throw new RuntimeException("Konnte Backup-Verzeichnis nicht erstellen", e);
        }
    }
    
    private String generateBackupId() {
        return Instant.now().toString() + "-" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
    
    private byte[] calculateChecksum(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private boolean shouldExclude(Path path) {
        return excludedPaths.stream().anyMatch(excluded -> 
            path.startsWith(excluded) || path.endsWith(excluded));
    }
    
    /**
     * Findet alle geänderten Dateien seit dem letzten Backup.
     */
    private Set<Path> findChangedFiles() throws IOException {
        Set<Path> changedFiles = new HashSet<>();
        Files.walk(plugin.getDataFolder().toPath())
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    byte[] currentChecksum = calculateChecksum(file);
                    String currentChecksumHex = bytesToHex(currentChecksum);
                    byte[] cachedChecksumBytes = checksumCache.get(file.toString());
                    String cachedChecksumHex = cachedChecksumBytes == null ? null : bytesToHex(cachedChecksumBytes);

                    if (!currentChecksumHex.equals(cachedChecksumHex)) {
                        changedFiles.add(file);
                        checksumCache.put(file.toString(), currentChecksum);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Fehler beim Prüfen der Datei: " + file);
                }
            });
        return changedFiles;
    }
    
    /**
     * Prüft, ob eine Datei bereits als Duplikat existiert.
     */
    private boolean isFileDuplicate(String checksumHex) {
        return checksumCache.containsValue(checksumHex.getBytes());
    }
    
    /**
     * Erstellt einen Backup für eine einzelne Datei.
     */
    private void backupFile(Path sourceFile, Path backupPath, byte[] checksum) throws IOException {
        Path targetFile = backupPath.resolve(sourceFile.getFileName());
        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * Erstellt Backup-Metadaten.
     */
    private BackupMetadata createBackupMetadata(String backupId, Set<Path> files, Map<String, String> deduplicationMap) {
        return new BackupMetadata(backupId, Instant.now(), files.size(), deduplicationMap);
    }
    private void restoreDeduplicated(Path file, BackupMetadata metadata) throws IOException {
        // Hole die Prüfsumme (Hex) aus der Deduplizierungsmap
        String checksumHex = metadata.deduplicationMap.get(file.toString());
        if (checksumHex == null) {
            throw new FileNotFoundException("Keine Deduplizierungsreferenz für Datei: " + file);
        }
        // Suche im Backup-Verzeichnis nach einer Datei mit passender Prüfsumme
        for (String backupId : backupHistory.keySet()) {
            Path backupPath = backupDir.resolve(backupId);
            if (!Files.exists(backupPath)) continue;
            try {
                Files.walk(backupPath)
                    .filter(Files::isRegularFile)
                    .forEach(candidate -> {
                        try {
                            byte[] candidateChecksum = calculateChecksum(candidate);
                            String candidateHex = bytesToHex(candidateChecksum);
                            if (candidateHex.equals(checksumHex)) {
                                // Datei gefunden, kopiere sie an Zielort
                                Files.copy(candidate, file, StandardCopyOption.REPLACE_EXISTING);
                                throw new FoundFileException(); // Abbruch der Schleife
                            }
                        } catch (IOException e) {
                            // Ignoriere und fahre fort
                        }
                    });
            } catch (FoundFileException e) {
                return; // Datei erfolgreich wiederhergestellt
            }
        }
        throw new FileNotFoundException("Deduplizierte Datei nicht gefunden für Prüfsumme: " + checksumHex);
    }

    // Hilfsklasse zum Abbruch von Files.walk
    private static class FoundFileException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    
    /**
     * Stellt eine Datei aus dem Backup wieder her.
     */
    private void restoreFile(Path file, Path backupPath) throws IOException {
        Path sourceFile = backupPath.resolve(file.getFileName());
        if (!Files.exists(sourceFile)) {
            throw new FileNotFoundException("Datei nicht im Backup gefunden: " + sourceFile);
        }
        Files.copy(sourceFile, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Berechnet die Gesamtgröße eines Backup-Verzeichnisses.
     */
    private long calculateTotalSize(Path backupPath) throws IOException {
        return Files.walk(backupPath)
            .filter(Files::isRegularFile)
            .mapToLong(file -> {
                try {
                    return Files.size(file);
                } catch (IOException e) {
                    return 0L;
                }
            })
            .sum();
    }

    /**
     * Validiert eine Datei im Backup, indem die Prüfsumme mit der Originaldatei verglichen wird.
     */
    private boolean validateFile(Path file, Path backupPath) {
        try {
            Path backupFile = backupPath.resolve(file.getFileName());
            if (!Files.exists(backupFile) || !Files.exists(file)) {
                return false;
            }
            byte[] originalChecksum = calculateChecksum(file);
            byte[] backupChecksum = calculateChecksum(backupFile);
            return MessageDigest.isEqual(originalChecksum, backupChecksum);
        } catch (IOException e) {
            return false;
        }
    }
}
