package com.essentialscore.webui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler für statische Dateien des WebUI
 * Unterstützt Caching, Kompression und MIME-Type-Detection
 */
public class StaticFileHandler implements HttpHandler {
    
    private static final Logger logger = Logger.getLogger(StaticFileHandler.class.getName());
    
    private final String rootPath;
    private final Map<String, String> mimeTypes;
    private final Map<String, byte[]> cache;
    private final boolean cacheEnabled;
    
    public StaticFileHandler(String rootPath) {
        this.rootPath = rootPath;
        this.mimeTypes = initializeMimeTypes();
        this.cache = new HashMap<>();
        this.cacheEnabled = true; // In Produktion könnte dies konfigurierbar sein
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        
        try {
            // Default zu index.html wenn Root-Path angefragt wird
            if ("/".equals(requestPath)) {
                requestPath = "/index.html";
            }
            
            // Sicherheitscheck: Pfad-Traversal verhindern
            if (requestPath.contains("..")) {
                sendResponse(exchange, 403, "text/plain", "Forbidden".getBytes());
                return;
            }
            
            // Datei-Pfad erstellen
            String filePath = rootPath + requestPath;
            Path path = Paths.get(filePath);
            
            // Prüfen ob Datei existiert
            if (!Files.exists(path) || Files.isDirectory(path)) {
                // Fallback zu index.html für SPA-Routing
                if (!requestPath.startsWith("/api/") && !requestPath.startsWith("/ws/")) {
                    path = Paths.get(rootPath + "/index.html");
                    if (!Files.exists(path)) {
                        sendResponse(exchange, 404, "text/plain", "File not found".getBytes());
                        return;
                    }
                } else {
                    sendResponse(exchange, 404, "text/plain", "File not found".getBytes());
                    return;
                }
            }
            
            // Datei aus Cache oder Dateisystem laden
            byte[] content = loadFile(path);
            
            // MIME-Type bestimmen
            String mimeType = getMimeType(path.toString());
            
            // CORS-Header hinzufügen
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            // Cache-Header für statische Ressourcen
            if (isStaticResource(requestPath)) {
                exchange.getResponseHeaders().add("Cache-Control", "public, max-age=3600");
            }
            
            // Security-Header
            exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponseHeaders().add("X-XSS-Protection", "1; mode=block");
            
            // Content-Security-Policy für WebUI
            if (mimeType.equals("text/html")) {
                exchange.getResponseHeaders().add("Content-Security-Policy", 
                    "default-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net wss://localhost:8081 ws://localhost:8081; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self' ws://localhost:8081 wss://localhost:8081");
            }
            
            sendResponse(exchange, 200, mimeType, content);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fehler beim Verarbeiten der Anfrage: " + requestPath, e);
            sendResponse(exchange, 500, "text/plain", "Internal Server Error".getBytes());
        }
    }
    
    private byte[] loadFile(Path path) throws IOException {
        String filePath = path.toString();
        
        // Aus Cache laden wenn verfügbar
        if (cacheEnabled && cache.containsKey(filePath)) {
            return cache.get(filePath);
        }
        
        // Datei lesen
        byte[] content = Files.readAllBytes(path);
        
        // In Cache speichern (nur kleinere Dateien)
        if (cacheEnabled && content.length < 1024 * 1024) { // < 1MB
            cache.put(filePath, content);
        }
        
        return content;
    }
    
    private String getMimeType(String filePath) {
        String extension = "";
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = filePath.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return mimeTypes.getOrDefault(extension, "application/octet-stream");
    }
    
    private boolean isStaticResource(String path) {
        return path.endsWith(".css") || 
               path.endsWith(".js") || 
               path.endsWith(".png") || 
               path.endsWith(".jpg") || 
               path.endsWith(".jpeg") || 
               path.endsWith(".gif") || 
               path.endsWith(".ico") ||
               path.endsWith(".woff") ||
               path.endsWith(".woff2") ||
               path.endsWith(".ttf") ||
               path.endsWith(".svg");
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, content.length);
        
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(content);
        }
    }
    
    private Map<String, String> initializeMimeTypes() {
        Map<String, String> types = new HashMap<>();
        
        // Text-Formate
        types.put("html", "text/html; charset=utf-8");
        types.put("htm", "text/html; charset=utf-8");
        types.put("css", "text/css; charset=utf-8");
        types.put("js", "application/javascript; charset=utf-8");
        types.put("json", "application/json; charset=utf-8");
        types.put("xml", "application/xml; charset=utf-8");
        types.put("txt", "text/plain; charset=utf-8");
        
        // Bilder
        types.put("png", "image/png");
        types.put("jpg", "image/jpeg");
        types.put("jpeg", "image/jpeg");
        types.put("gif", "image/gif");
        types.put("svg", "image/svg+xml");
        types.put("ico", "image/x-icon");
        types.put("webp", "image/webp");
        
        // Fonts
        types.put("woff", "font/woff");
        types.put("woff2", "font/woff2");
        types.put("ttf", "font/ttf");
        types.put("eot", "application/vnd.ms-fontobject");
        types.put("otf", "font/otf");
        
        // Audio/Video
        types.put("mp3", "audio/mpeg");
        types.put("wav", "audio/wav");
        types.put("mp4", "video/mp4");
        types.put("webm", "video/webm");
        
        // Archive
        types.put("zip", "application/zip");
        types.put("gz", "application/gzip");
        types.put("tar", "application/x-tar");
        
        // Dokumente
        types.put("pdf", "application/pdf");
        
        return types;
    }
}
