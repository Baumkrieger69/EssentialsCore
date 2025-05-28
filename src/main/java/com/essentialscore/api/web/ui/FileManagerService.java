package com.essentialscore.api.web.ui;

import com.essentialscore.api.security.SecurityManager;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Manages file operations for the WebUI.
 */
public class FileManagerService {
    private static final Logger LOGGER = Logger.getLogger(FileManagerService.class.getName());
    
    private final Plugin plugin;
    private final Path rootPath;
    private final Set<String> allowedRoots;
    private final Set<String> blockedExtensions;
    
    /**
     * Creates a new file manager service
     * 
     * @param plugin The plugin instance
     * @param securityManager The security manager
     */
    public FileManagerService(Plugin plugin, SecurityManager securityManager) {
        this.plugin = plugin;
        this.rootPath = plugin.getDataFolder().getParentFile().toPath();
        
        // Setup security constraints
        this.allowedRoots = new HashSet<>();
        allowedRoots.add(plugin.getDataFolder().getParentFile().getAbsolutePath());
        
        this.blockedExtensions = new HashSet<>();
        blockedExtensions.add("jar");
        blockedExtensions.add("exe");
        blockedExtensions.add("bat");
        blockedExtensions.add("sh");
        blockedExtensions.add("dll");
        blockedExtensions.add("so");
        blockedExtensions.add("dylib");
    }
    
    /**
     * Lists files in a directory
     * 
     * @param relativePath The path relative to the server root
     * @return A list of file metadata
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public List<Map<String, Object>> listFiles(String relativePath) throws IOException, SecurityException {
        // Resolve and validate path
        Path targetPath = resolvePath(relativePath);
        
        // Check if directory exists
        if (!Files.exists(targetPath)) {
            throw new FileNotFoundException("Directory not found: " + relativePath);
        }
        
        // Check if path is a directory
        if (!Files.isDirectory(targetPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + relativePath);
        }
        
        // List files
        List<Map<String, Object>> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath)) {
            for (Path entry : stream) {
                Map<String, Object> fileInfo = createFileInfo(entry, targetPath);
                result.add(fileInfo);
            }
        }
        
        // Sort by type (directory first) and then by name
        result.sort((a, b) -> {
            boolean aIsDir = (boolean) a.get("isDirectory");
            boolean bIsDir = (boolean) b.get("isDirectory");
            
            if (aIsDir && !bIsDir) {
                return -1;
            } else if (!aIsDir && bIsDir) {
                return 1;
            } else {
                return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
            }
        });
        
        return result;
    }
    
    /**
     * Creates file metadata for a path
     * 
     * @param path The file path
     * @param parentPath The parent directory path
     * @return The file metadata
     * @throws IOException If an I/O error occurs
     */
    private Map<String, Object> createFileInfo(Path path, Path parentPath) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("name", path.getFileName().toString());
        fileInfo.put("path", rootPath.relativize(path).toString().replace('\\', '/'));
        fileInfo.put("isDirectory", Files.isDirectory(path));
        fileInfo.put("size", attrs.size());
        fileInfo.put("lastModified", attrs.lastModifiedTime().toMillis());
        
        // Add additional info for files
        if (!Files.isDirectory(path)) {
            String extension = getFileExtension(path.toString());
            fileInfo.put("extension", extension);
            fileInfo.put("isEditable", isFileEditable(extension));
        }
        
        return fileInfo;
    }
    
    /**
     * Gets the file extension
     * 
     * @param filename The filename
     * @return The extension, or an empty string if none
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * Checks if a file is editable based on its extension
     * 
     * @param extension The file extension
     * @return true if editable
     */
    private boolean isFileEditable(String extension) {
        Set<String> editableExtensions = new HashSet<>(Arrays.asList(
                "txt", "log", "yml", "yaml", "json", "properties", "xml", "html", "css", "js",
                "md", "csv", "conf", "cfg", "ini", "sql", "mcmeta"
        ));
        
        return editableExtensions.contains(extension.toLowerCase());
    }
    
    /**
     * Reads a file
     * 
     * @param relativePath The path relative to the server root
     * @return The file content
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public String readFile(String relativePath) throws IOException, SecurityException {
        // Resolve and validate path
        Path targetPath = resolvePath(relativePath);
        
        // Check if file exists
        if (!Files.exists(targetPath)) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }
        
        // Check if path is a file
        if (Files.isDirectory(targetPath)) {
            throw new IllegalArgumentException("Path is a directory: " + relativePath);
        }
        
        // Check if file is too large (>10MB)
        if (Files.size(targetPath) > 10 * 1024 * 1024) {
            throw new IOException("File is too large to read (>10MB): " + relativePath);
        }
        
        // Check if file is editable
        String extension = getFileExtension(targetPath.toString());
        if (!isFileEditable(extension)) {
            throw new SecurityException("File type is not supported for reading: " + extension);
        }
        
        // Read file
        return Files.readString(targetPath, StandardCharsets.UTF_8);
    }
    
    /**
     * Writes a file
     * 
     * @param relativePath The path relative to the server root
     * @param content The file content
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public void writeFile(String relativePath, String content) throws IOException, SecurityException {
        // Resolve and validate path
        Path targetPath = resolvePath(relativePath);
        
        // Check if parent directory exists
        Path parentPath = targetPath.getParent();
        if (!Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
        }
        
        // Check if file extension is blocked
        String extension = getFileExtension(targetPath.toString());
        if (blockedExtensions.contains(extension.toLowerCase())) {
            throw new SecurityException("File extension is blocked: " + extension);
        }
        
        // Write file
        Files.writeString(targetPath, content, StandardCharsets.UTF_8);
    }
    
    /**
     * Creates a directory
     * 
     * @param relativePath The path relative to the server root
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public void createDirectory(String relativePath) throws IOException, SecurityException {
        // Resolve and validate path
        Path targetPath = resolvePath(relativePath);
        
        // Create directory
        Files.createDirectories(targetPath);
    }
    
    /**
     * Deletes a file or directory
     * 
     * @param relativePath The path relative to the server root
     * @param recursive Whether to delete directories recursively
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public void delete(String relativePath, boolean recursive) throws IOException, SecurityException {
        // Resolve and validate path
        Path targetPath = resolvePath(relativePath);
        
        // Check if path exists
        if (!Files.exists(targetPath)) {
            throw new FileNotFoundException("Path not found: " + relativePath);
        }
        
        // Delete
        if (Files.isDirectory(targetPath)) {
            if (recursive) {
                Files.walk(targetPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "Failed to delete: " + path, e);
                            }
                        });
            } else {
                Files.delete(targetPath);
            }
        } else {
            Files.delete(targetPath);
        }
    }
    
    /**
     * Renames a file or directory
     * 
     * @param relativePath The path relative to the server root
     * @param newName The new name
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public void rename(String relativePath, String newName) throws IOException, SecurityException {
        // Resolve and validate path
        Path targetPath = resolvePath(relativePath);
        
        // Check if path exists
        if (!Files.exists(targetPath)) {
            throw new FileNotFoundException("Path not found: " + relativePath);
        }
        
        // Validate new name
        if (newName.contains("/") || newName.contains("\\")) {
            throw new IllegalArgumentException("New name cannot contain path separators");
        }
        
        // Check if file extension is blocked
        if (!Files.isDirectory(targetPath)) {
            String extension = getFileExtension(newName);
            if (blockedExtensions.contains(extension.toLowerCase())) {
                throw new SecurityException("File extension is blocked: " + extension);
            }
        }
        
        // Create new path
        Path newPath = targetPath.getParent().resolve(newName);
        
        // Check if target already exists
        if (Files.exists(newPath)) {
            throw new FileAlreadyExistsException("Target already exists: " + newName);
        }
        
        // Rename
        Files.move(targetPath, newPath);
    }
    
    /**
     * Copies a file or directory
     * 
     * @param sourcePath The source path relative to the server root
     * @param targetPath The target path relative to the server root
     * @param overwrite Whether to overwrite existing files
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public void copy(String sourcePath, String targetPath, boolean overwrite) 
            throws IOException, SecurityException {
        // Resolve and validate paths
        Path source = resolvePath(sourcePath);
        Path target = resolvePath(targetPath);
        
        // Check if source exists
        if (!Files.exists(source)) {
            throw new FileNotFoundException("Source not found: " + sourcePath);
        }
        
        // Check if target parent exists
        Path targetParent = target.getParent();
        if (!Files.exists(targetParent)) {
            Files.createDirectories(targetParent);
        }
        
        // Copy
        if (Files.isDirectory(source)) {
            // Copy directory recursively
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) 
                        throws IOException {
                    Path targetDir = target.resolve(source.relativize(dir));
                    if (!Files.exists(targetDir)) {
                        Files.createDirectory(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
                        throws IOException {
                    Path targetFile = target.resolve(source.relativize(file));
                    copyFile(file, targetFile, overwrite);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            // Copy single file
            copyFile(source, target, overwrite);
        }
    }
    
    /**
     * Copies a single file
     * 
     * @param source The source file
     * @param target The target file
     * @param overwrite Whether to overwrite existing files
     * @throws IOException If an I/O error occurs
     */
    private void copyFile(Path source, Path target, boolean overwrite) throws IOException {
        if (overwrite) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else if (!Files.exists(target)) {
            Files.copy(source, target);
        } else {
            throw new FileAlreadyExistsException("Target file already exists: " + target);
        }
    }
    
    /**
     * Creates a zip archive
     * 
     * @param sourcePaths The source paths to include in the archive
     * @param targetPath The target zip file path
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public void createZip(List<String> sourcePaths, String targetPath) throws IOException, SecurityException {
        // Resolve and validate target path
        Path target = resolvePath(targetPath);
        
        // Check if target parent exists
        Path targetParent = target.getParent();
        if (!Files.exists(targetParent)) {
            Files.createDirectories(targetParent);
        }
        
        // Check if target has .zip extension
        if (!target.toString().toLowerCase().endsWith(".zip")) {
            target = Paths.get(target.toString() + ".zip");
        }
        
        // Create zip file
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(target))) {
            for (String sourcePath : sourcePaths) {
                Path source = resolvePath(sourcePath);
                
                if (!Files.exists(source)) {
                    throw new FileNotFoundException("Source not found: " + sourcePath);
                }
                
                if (Files.isDirectory(source)) {
                    // Add directory contents
                    Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
                                throws IOException {
                            String entryName = source.getFileName() + "/" + source.relativize(file);
                            addToZip(zos, file, entryName);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    // Add single file
                    addToZip(zos, source, source.getFileName().toString());
                }
            }
        }
    }
    
    /**
     * Adds a file to a zip archive
     * 
     * @param zos The zip output stream
     * @param file The file to add
     * @param entryName The entry name in the archive
     * @throws IOException If an I/O error occurs
     */
    private void addToZip(ZipOutputStream zos, Path file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName.replace('\\', '/'));
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
    }
    
    /**
     * Extracts a zip archive
     * 
     * @param zipPath The zip file path
     * @param targetPath The target directory path
     * @param overwrite Whether to overwrite existing files
     * @throws IOException If an I/O error occurs
     * @throws SecurityException If the operation is not allowed
     */
    public void extractZip(String zipPath, String targetPath, boolean overwrite) 
            throws IOException, SecurityException {
        // Resolve and validate paths
        Path source = resolvePath(zipPath);
        Path target = resolvePath(targetPath);
        
        // Check if source exists
        if (!Files.exists(source)) {
            throw new FileNotFoundException("Zip file not found: " + zipPath);
        }
        
        // Check if source is a zip file
        if (!source.toString().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Source is not a zip file: " + zipPath);
        }
        
        // Create target directory if it doesn't exist
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }
        
        // Extract zip
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // Ignore directory entries
                if (entry.isDirectory()) {
                    continue;
                }
                
                // Check for zip slip vulnerability
                Path entryPath = target.resolve(entryName);
                if (!entryPath.normalize().startsWith(target.normalize())) {
                    throw new SecurityException("Zip entry outside target directory: " + entryName);
                }
                
                // Check if file extension is blocked
                String extension = getFileExtension(entryName);
                if (blockedExtensions.contains(extension.toLowerCase())) {
                    LOGGER.warning("Skipping blocked file extension: " + entryName);
                    continue;
                }
                
                // Create parent directories
                Files.createDirectories(entryPath.getParent());
                
                // Extract file
                if (overwrite || !Files.exists(entryPath)) {
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                zis.closeEntry();
            }
        }
    }
    
    /**
     * Resolves a path relative to the server root
     * 
     * @param relativePath The relative path
     * @return The resolved path
     * @throws SecurityException If the path is outside the allowed roots
     */
    private Path resolvePath(String relativePath) throws SecurityException {
        // Normalize path
        String normalizedPath = relativePath.replace('\\', '/');
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // Resolve path
        Path path = rootPath.resolve(normalizedPath).normalize();
        
        // Security check: path must be within allowed roots
        boolean allowed = false;
        for (String root : allowedRoots) {
            if (path.startsWith(root)) {
                allowed = true;
                break;
            }
        }
        
        if (!allowed) {
            throw new SecurityException("Access to path outside allowed roots: " + relativePath);
        }
        
        return path;
    }
    
    /**
     * Adds an allowed root path
     * 
     * @param root The root path to allow
     */
    public void addAllowedRoot(String root) {
        allowedRoots.add(new File(root).getAbsolutePath());
    }
    
    /**
     * Gets the allowed root paths
     * 
     * @return The allowed root paths
     */
    public Set<String> getAllowedRoots() {
        return Collections.unmodifiableSet(allowedRoots);
    }
    
    /**
     * Adds a blocked file extension
     * 
     * @param extension The extension to block
     */
    public void addBlockedExtension(String extension) {
        blockedExtensions.add(extension.toLowerCase());
    }
    
    /**
     * Gets the blocked file extensions
     * 
     * @return The blocked extensions
     */
    public Set<String> getBlockedExtensions() {
        return Collections.unmodifiableSet(blockedExtensions);
    }
} 