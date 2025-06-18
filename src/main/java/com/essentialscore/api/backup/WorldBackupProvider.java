package com.essentialscore.api.backup;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Provider for backing up world data.
 */
public class WorldBackupProvider implements BackupProvider {
    private static final Logger LOGGER = Logger.getLogger(WorldBackupProvider.class.getName());
    
    // Directories to exclude from backup
    private static final Set<String> EXCLUDED_DIRS = Set.of(
        "session.lock",  // Lock file
        "playerdata",    // Handled by PlayerDataBackupProvider
        "stats",         // Player statistics (can be regenerated)
        "advancements",  // Player advancements (can be regenerated)
        "temp"           // Temporary files
    );
    
    @Override
    public String getId() {
        return "world";
    }
    
    @Override
    public String getDisplayName() {
        return "World Data";
    }
    
    @Override
    public Set<String> performBackup(BackupSystem backupSystem, File backupDir, Object context) throws Exception {
        LOGGER.info("Starting world backup");
        Set<String> backedUpFiles = new HashSet<>();
        
        // Create directory for world backups
        File worldsBackupDir = new File(backupDir, "worlds");
        worldsBackupDir.mkdirs();
        
        // Get all loaded worlds
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            File worldDir = world.getWorldFolder();
            
            // Save world before backup
            world.save();
            
            // Create backup directory for this world
            File worldBackupDir = new File(worldsBackupDir, worldName);
            worldBackupDir.mkdirs();
            
            // Back up the world directory
            Set<String> worldFiles = backupDirectory(worldDir, worldBackupDir, "worlds/" + worldName + "/");
            backedUpFiles.addAll(worldFiles);
            
            LOGGER.info("Backed up world: " + worldName + " (" + worldFiles.size() + " files)");
        }
        
        LOGGER.info("World backup completed, backed up " + backedUpFiles.size() + " files");
        return backedUpFiles;
    }
    
    @Override
    public Set<String> performIncrementalBackup(BackupSystem backupSystem, File backupDir, File previousBackupDir) throws Exception {
        LOGGER.info("Starting incremental world backup");
        Set<String> backedUpFiles = new HashSet<>();
        
        // Create directory for world backups
        File worldsBackupDir = new File(backupDir, "worlds");
        worldsBackupDir.mkdirs();
        
        // Get previous backup directory
        File previousWorldsBackupDir = new File(previousBackupDir, "worlds");
        
        // Get all loaded worlds
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            File worldDir = world.getWorldFolder();
            
            // Save world before backup
            world.save();
            
            // Create backup directory for this world
            File worldBackupDir = new File(worldsBackupDir, worldName);
            worldBackupDir.mkdirs();
            
            // Get previous world backup directory
            File previousWorldBackupDir = new File(previousWorldsBackupDir, worldName);
            
            // Back up the world directory incrementally
            Set<String> worldFiles = backupDirectoryIncremental(worldDir, worldBackupDir, previousWorldBackupDir, "worlds/" + worldName + "/");
            backedUpFiles.addAll(worldFiles);
            
            LOGGER.info("Incrementally backed up world: " + worldName + " (" + worldFiles.size() + " files)");
        }
        
        LOGGER.info("Incremental world backup completed, backed up " + backedUpFiles.size() + " files");
        return backedUpFiles;
    }
    
    @Override
    public void performRestore(BackupSystem backupSystem, File backupDir, Set<String> files) throws Exception {
        LOGGER.info("Restoring world data");
        
        // Get worlds backup directory
        File worldsBackupDir = new File(backupDir, "worlds");
        if (!worldsBackupDir.exists() || !worldsBackupDir.isDirectory()) {
            LOGGER.warning("Worlds backup directory not found: " + worldsBackupDir.getPath());
            return;
        }
        
        // Filter files by world prefix
        Set<String> worldFiles = files.stream()
            .filter(path -> path.startsWith("worlds/"))
            .collect(Collectors.toSet());
        
        // Group files by world
        Map<String, Set<String>> filesByWorld = new HashMap<>();
        for (String path : worldFiles) {
            // Extract world name from path (format: "worlds/{worldName}/...")
            String[] parts = path.split("/", 3);
            if (parts.length >= 3) {
                String worldName = parts[1];
                String relativePath = parts[2];
                filesByWorld.computeIfAbsent(worldName, k -> new HashSet<>()).add(relativePath);
            }
        }
        
        // Restore each world
        for (Map.Entry<String, Set<String>> entry : filesByWorld.entrySet()) {
            String worldName = entry.getKey();
            Set<String> worldRelativePaths = entry.getValue();
            
            // Get world directory
            World world = Bukkit.getWorld(worldName);
            File worldDir;
            
            if (world != null) {
                // World is loaded, unload it first
                LOGGER.info("Unloading world before restore: " + worldName);
                Bukkit.unloadWorld(world, true);
                worldDir = world.getWorldFolder();
            } else {
                // World is not loaded, get directory from server
                worldDir = new File(Bukkit.getWorldContainer(), worldName);
            }
            
            // Source directory in the backup
            File worldBackupDir = new File(worldsBackupDir, worldName);
            
            // Restore world files
            for (String relativePath : worldRelativePaths) {
                File sourceFile = new File(worldBackupDir, relativePath);
                File targetFile = new File(worldDir, relativePath);
                
                if (sourceFile.exists()) {
                    // Create parent directories if needed
                    targetFile.getParentFile().mkdirs();
                    
                    try {
                        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to restore world file: " + relativePath, e);
                    }
                }
            }
            
            LOGGER.info("Restored world: " + worldName + " (" + worldRelativePaths.size() + " files)");
            
            // Reload the world if it was loaded before
            if (world != null) {
                LOGGER.info("Reloading world: " + worldName);
                Bukkit.createWorld(new WorldCreator(worldName));
            }
        }
    }
    
    /**
     * Backs up a directory and its contents.
     *
     * @param sourceDir The source directory
     * @param targetDir The target directory
     * @param pathPrefix The path prefix for the backed up files
     * @return The set of backed up file paths
     * @throws IOException If an I/O error occurs
     */
    private Set<String> backupDirectory(File sourceDir, File targetDir, String pathPrefix) throws IOException {
        final Set<String> backedUpFiles = new HashSet<>();
        
        Files.walkFileTree(sourceDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirName = dir.getFileName().toString();
                
                // Skip excluded directories
                if (dir.equals(sourceDir.toPath())) {
                    return FileVisitResult.CONTINUE;
                } else if (EXCLUDED_DIRS.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                // Create corresponding directory in target
                Path targetPath = targetDir.toPath().resolve(sourceDir.toPath().relativize(dir));
                Files.createDirectories(targetPath);
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                
                // Skip excluded files
                if (EXCLUDED_DIRS.contains(fileName)) {
                    return FileVisitResult.CONTINUE;
                }
                
                // Create relative path
                Path relativePath = sourceDir.toPath().relativize(file);
                Path targetPath = targetDir.toPath().resolve(relativePath);
                
                // Copy file
                Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Add to backed up files
                backedUpFiles.add(pathPrefix + relativePath.toString().replace('\\', '/'));
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return backedUpFiles;
    }
    
    /**
     * Backs up a directory and its contents incrementally.
     *
     * @param sourceDir The source directory
     * @param targetDir The target directory
     * @param previousDir The previous backup directory
     * @param pathPrefix The path prefix for the backed up files
     * @return The set of backed up file paths
     * @throws IOException If an I/O error occurs
     */
    private Set<String> backupDirectoryIncremental(File sourceDir, File targetDir, File previousDir, String pathPrefix) throws IOException {
        final Set<String> backedUpFiles = new HashSet<>();
        
        Files.walkFileTree(sourceDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirName = dir.getFileName().toString();
                
                // Skip excluded directories
                if (dir.equals(sourceDir.toPath())) {
                    return FileVisitResult.CONTINUE;
                } else if (EXCLUDED_DIRS.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                // Create corresponding directory in target
                Path targetPath = targetDir.toPath().resolve(sourceDir.toPath().relativize(dir));
                Files.createDirectories(targetPath);
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                
                // Skip excluded files
                if (EXCLUDED_DIRS.contains(fileName)) {
                    return FileVisitResult.CONTINUE;
                }
                
                // Create relative path
                Path relativePath = sourceDir.toPath().relativize(file);
                Path targetPath = targetDir.toPath().resolve(relativePath);
                
                // Check if file has changed since previous backup
                if (previousDir != null && previousDir.exists()) {
                    Path previousPath = previousDir.toPath().resolve(relativePath);
                    
                    if (Files.exists(previousPath) && 
                        Files.getLastModifiedTime(previousPath).equals(Files.getLastModifiedTime(file)) &&
                        Files.size(previousPath) == Files.size(file)) {
                        // File hasn't changed, copy from previous backup
                        Files.copy(previousPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        backedUpFiles.add(pathPrefix + relativePath.toString().replace('\\', '/'));
                        return FileVisitResult.CONTINUE;
                    }
                }
                
                // File has changed or doesn't exist in previous backup, copy from source
                Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Add to backed up files
                backedUpFiles.add(pathPrefix + relativePath.toString().replace('\\', '/'));
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return backedUpFiles;
    }
} 
