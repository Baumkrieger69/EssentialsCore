package com.essentialscore.api.web.rest.endpoints;

import com.essentialscore.api.web.rest.ApiEndpoint;
import com.essentialscore.api.web.rest.ApiRequest;
import com.essentialscore.api.web.rest.ApiResponse;
import com.essentialscore.api.web.ui.FileManagerService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API endpoint for file management operations.
 */
public class FileManagerEndpoint extends ApiEndpoint {
    
    // Removed unused field
    private final Path serverRoot;
    private final Set<String> allowedExtensions;
    private final Set<String> restrictedPaths;
    
    /**
     * Creates a new file manager endpoint
     * 
     * @param plugin The plugin instance
     * @param fileManagerService The file manager service
     */
    public FileManagerEndpoint(Plugin plugin, FileManagerService fileManagerService) {
        super(plugin);
        // Not storing the fileManagerService since it's not used
        this.serverRoot = plugin.getDataFolder().getParentFile().getParentFile().toPath().normalize();
        
        // Initialize allowed file extensions for editing
        this.allowedExtensions = new HashSet<>(Arrays.asList(
            "yml", "yaml", "txt", "properties", "json", "log", "cfg", "config", "md", "xml"
        ));
        
        // Initialize restricted paths (security)
        this.restrictedPaths = new HashSet<>(Arrays.asList(
            "bin", "lib", "jre", "system32", "windows", "program files", "users"
        ));
    }
    
    @Override
    public String getPath() {
        return "files";
    }
    
    @Override
    public boolean requiresAuthentication() {
        return true;
    }
    
    @Override
    public String getRequiredPermission() {
        return "essentials.webui.files";
    }
    
    @Override
    public ApiResponse handleRequest(ApiRequest request) {
        try {
            String method = request.getMethod();
            String subpath = request.getPathSegment(1);
            
            if (subpath == null) {
                // Base files endpoint - list root directory
                switch (method) {
                    case "GET":
                        return listDirectory("");
                    default:
                        return ApiResponse.methodNotAllowed("Method not allowed: " + method);
                }
            }
            
            switch (subpath) {
                case "list":
                    return handleListRequest(request);
                case "read":
                    return handleReadRequest(request);
                case "write":
                    return handleWriteRequest(request);
                case "create":
                    return handleCreateRequest(request);
                case "delete":
                    return handleDeleteRequest(request);
                case "rename":
                    return handleRenameRequest(request);
                case "copy":
                    return handleCopyRequest(request);
                case "upload":
                    return handleUploadRequest(request);
                case "download":
                    return handleDownloadRequest(request);
                case "search":
                    return handleSearchRequest(request);
                default:
                    return ApiResponse.notFound("Unknown file operation: " + subpath);
            }
        } catch (Exception e) {
            return ApiResponse.error("Error processing file request: " + e.getMessage());
        }
    }
    
    /**
     * Handles directory listing requests
     */
    private ApiResponse handleListRequest(ApiRequest request) {
        try {
            String path = request.getParameter("path");
            if (path == null) {
                path = "";
            }
            
            return listDirectory(path);
        } catch (Exception e) {
            return ApiResponse.error("Error listing directory: " + e.getMessage());
        }
    }
    
    /**
     * Handles file read requests
     */
    private ApiResponse handleReadRequest(ApiRequest request) {
        try {
            String filePath = request.getParameter("path");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ApiResponse.badRequest("File path is required");
            }
            
            Path targetPath = resolvePath(filePath);
            if (!isValidPath(targetPath) || !Files.exists(targetPath)) {
                return ApiResponse.notFound("File not found or access denied");
            }
            
            if (Files.isDirectory(targetPath)) {
                return ApiResponse.badRequest("Cannot read directory as file");
            }
            
            if (!isEditableFile(targetPath)) {
                return ApiResponse.forbidden("File type not allowed for editing");
            }
            
            if (Files.size(targetPath) > 5 * 1024 * 1024) { // 5MB limit
                return ApiResponse.badRequest("File too large for editing (max 5MB)");
            }
            
            String content = Files.readString(targetPath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", content);
            response.put("path", filePath);
            response.put("size", Files.size(targetPath));
            response.put("lastModified", Files.getLastModifiedTime(targetPath).toMillis());
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error reading file: " + e.getMessage());
        }
    }
    
    /**
     * Handles file write requests
     */
    private ApiResponse handleWriteRequest(ApiRequest request) {
        try {
            if (request.getBody() == null || request.getBody().trim().isEmpty()) {
                return ApiResponse.badRequest("Request body is required");
            }
            
            JsonObject json = new JsonParser().parse(request.getBody()).getAsJsonObject();
            String filePath = json.get("path").getAsString();
            String content = json.get("content").getAsString();
            
            Path targetPath = resolvePath(filePath);
            if (!isValidPath(targetPath)) {
                return ApiResponse.forbidden("File path not allowed");
            }
            
            if (!isEditableFile(targetPath)) {
                return ApiResponse.forbidden("File type not allowed for editing");
            }
            
            // Create parent directories if they don't exist
            Files.createDirectories(targetPath.getParent());
            
            // Create backup if file exists
            if (Files.exists(targetPath)) {
                Path backupPath = targetPath.resolveSibling(targetPath.getFileName() + ".backup");
                Files.copy(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            Files.writeString(targetPath, content);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File saved successfully");
            response.put("path", filePath);
            response.put("size", Files.size(targetPath));
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error writing file: " + e.getMessage());
        }
    }
    
    /**
     * Handles file/directory creation requests
     */
    private ApiResponse handleCreateRequest(ApiRequest request) {
        try {
            JsonObject json = new JsonParser().parse(request.getBody()).getAsJsonObject();
            String path = json.get("path").getAsString();
            String type = json.get("type").getAsString(); // "file" or "directory"
            
            Path targetPath = resolvePath(path);
            if (!isValidPath(targetPath)) {
                return ApiResponse.forbidden("Path not allowed");
            }
            
            if (Files.exists(targetPath)) {
                return ApiResponse.badRequest("File or directory already exists");
            }
            
            if ("directory".equals(type)) {
                Files.createDirectories(targetPath);
            } else {
                Files.createDirectories(targetPath.getParent());
                Files.createFile(targetPath);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", type + " created successfully");
            response.put("path", path);
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error creating " + request.getParameter("type") + ": " + e.getMessage());
        }
    }
    
    /**
     * Handles file/directory deletion requests
     */
    private ApiResponse handleDeleteRequest(ApiRequest request) {
        try {
            JsonObject json = new JsonParser().parse(request.getBody()).getAsJsonObject();
            String path = json.get("path").getAsString();
            
            Path targetPath = resolvePath(path);
            if (!isValidPath(targetPath) || !Files.exists(targetPath)) {
                return ApiResponse.notFound("File or directory not found");
            }
            
            // Prevent deletion of critical files/directories
            if (isCriticalPath(targetPath)) {
                return ApiResponse.forbidden("Cannot delete critical system files");
            }
            
            if (Files.isDirectory(targetPath)) {
                // Delete directory recursively
                Files.walkFileTree(targetPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.delete(targetPath);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deleted successfully");
            response.put("path", path);
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error deleting: " + e.getMessage());
        }
    }
    
    /**
     * Handles file/directory rename requests
     */
    private ApiResponse handleRenameRequest(ApiRequest request) {
        try {
            JsonObject json = new JsonParser().parse(request.getBody()).getAsJsonObject();
            String oldPath = json.get("oldPath").getAsString();
            String newPath = json.get("newPath").getAsString();
            
            Path sourcePath = resolvePath(oldPath);
            Path targetPath = resolvePath(newPath);
            
            if (!isValidPath(sourcePath) || !isValidPath(targetPath)) {
                return ApiResponse.forbidden("Path not allowed");
            }
            
            if (!Files.exists(sourcePath)) {
                return ApiResponse.notFound("Source file not found");
            }
            
            if (Files.exists(targetPath)) {
                return ApiResponse.badRequest("Target already exists");
            }
            
            Files.move(sourcePath, targetPath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Renamed successfully");
            response.put("oldPath", oldPath);
            response.put("newPath", newPath);
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error renaming: " + e.getMessage());
        }
    }
    
    /**
     * Handles file/directory copy requests
     */
    private ApiResponse handleCopyRequest(ApiRequest request) {
        try {
            JsonObject json = new JsonParser().parse(request.getBody()).getAsJsonObject();
            String sourcePath = json.get("sourcePath").getAsString();
            String targetPath = json.get("targetPath").getAsString();
            
            Path source = resolvePath(sourcePath);
            Path target = resolvePath(targetPath);
            
            if (!isValidPath(source) || !isValidPath(target)) {
                return ApiResponse.forbidden("Path not allowed");
            }
            
            if (!Files.exists(source)) {
                return ApiResponse.notFound("Source file not found");
            }
            
            if (Files.exists(target)) {
                return ApiResponse.badRequest("Target already exists");
            }
            
            if (Files.isDirectory(source)) {
                copyDirectory(source, target);
            } else {
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Copied successfully");
            response.put("sourcePath", sourcePath);
            response.put("targetPath", targetPath);
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error copying: " + e.getMessage());
        }
    }
    
    /**
     * Handles file upload requests (placeholder)
     */
    private ApiResponse handleUploadRequest(ApiRequest request) {
        return ApiResponse.error("File upload not implemented in this simplified version");
    }
    
    /**
     * Handles file download requests (placeholder)
     */
    private ApiResponse handleDownloadRequest(ApiRequest request) {
        return ApiResponse.error("File download not implemented in this simplified version");
    }
    
    /**
     * Handles file search requests
     */
    private ApiResponse handleSearchRequest(ApiRequest request) {
        try {
            String query = request.getParameter("query");
            String path = request.getParameter("path");
            
            if (query == null || query.trim().isEmpty()) {
                return ApiResponse.badRequest("Search query is required");
            }
            
            Path searchPath = resolvePath(path != null ? path : "");
            if (!isValidPath(searchPath)) {
                return ApiResponse.forbidden("Search path not allowed");
            }
            
            List<Map<String, Object>> results = searchFiles(searchPath, query);
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", results);
            response.put("count", results.size());
            response.put("query", query);
            response.put("searchPath", path);
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error searching files: " + e.getMessage());
        }
    }
    
    /**
     * Lists files and directories in the specified path
     */
    private ApiResponse listDirectory(String path) {
        try {
            Path targetPath = resolvePath(path);
            if (!isValidPath(targetPath) || !Files.exists(targetPath)) {
                return ApiResponse.notFound("Directory not found or access denied");
            }
            
            if (!Files.isDirectory(targetPath)) {
                return ApiResponse.badRequest("Path is not a directory");
            }
            
            List<Map<String, Object>> files = Files.list(targetPath)
                .map(this::createFileInfo)
                .collect(Collectors.toList());
            
            // Sort directories first, then files
            files.sort((a, b) -> {
                boolean aIsDir = (Boolean) a.get("isDirectory");
                boolean bIsDir = (Boolean) b.get("isDirectory");
                if (aIsDir != bIsDir) {
                    return aIsDir ? -1 : 1;
                }
                return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("files", files);
            response.put("path", path);
            response.put("count", files.size());
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error listing directory: " + e.getMessage());
        }
    }
    
    /**
     * Creates file information map
     */
    private Map<String, Object> createFileInfo(Path path) {
        Map<String, Object> info = new HashMap<>();
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            
            info.put("name", path.getFileName().toString());
            info.put("path", serverRoot.relativize(path).toString().replace('\\', '/'));
            info.put("isDirectory", attrs.isDirectory());
            info.put("size", attrs.size());
            info.put("lastModified", attrs.lastModifiedTime().toMillis());
            info.put("created", attrs.creationTime().toMillis());
            info.put("readable", Files.isReadable(path));
            info.put("writable", Files.isWritable(path));
            
            if (!attrs.isDirectory()) {
                String fileName = path.getFileName().toString();
                String extension = "";
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot > 0) {
                    extension = fileName.substring(lastDot + 1).toLowerCase();
                }
                info.put("extension", extension);
                info.put("editable", isEditableFile(path));
            }
        } catch (Exception e) {
            info.put("error", "Unable to read file attributes");
        }
        
        return info;
    }
    
    /**
     * Resolves a relative path to an absolute path within the server directory
     */
    private Path resolvePath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return serverRoot;
        }
        
        // Normalize and resolve path
        Path resolved = serverRoot.resolve(relativePath).normalize();
        
        // Ensure the path is within the server root
        if (!resolved.startsWith(serverRoot)) {
            throw new SecurityException("Path traversal attempt detected");
        }
        
        return resolved;
    }
    
    /**
     * Checks if a path is valid and safe to access
     */
    private boolean isValidPath(Path path) {
        try {
            // Must be within server root
            if (!path.startsWith(serverRoot)) {
                return false;
            }
            
            // Check for restricted paths
            String pathStr = path.toString().toLowerCase();
            for (String restricted : restrictedPaths) {
                if (pathStr.contains(restricted)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if a file is editable (based on extension)
     */
    private boolean isEditableFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) {
            return false;
        }
        
        String extension = fileName.substring(lastDot + 1);
        return allowedExtensions.contains(extension);
    }
    
    /**
     * Checks if a path is critical and should not be deleted
     */
    private boolean isCriticalPath(Path path) {
        String pathStr = path.toString().toLowerCase();
        return pathStr.contains("plugins") && pathStr.contains("essentialscore") ||
               pathStr.endsWith("server.jar") ||
               pathStr.endsWith("bukkit.yml") ||
               pathStr.endsWith("server.properties");
    }
    
    /**
     * Copies a directory recursively
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Searches for files matching the query
     */
    private List<Map<String, Object>> searchFiles(Path searchPath, String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        try {
            Files.walkFileTree(searchPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString().toLowerCase();
                    if (fileName.contains(lowerQuery)) {
                        results.add(createFileInfo(file));
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // Continue on access errors
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            // Handle search errors gracefully
        }
        
        return results;
    }
}
