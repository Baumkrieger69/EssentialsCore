package com.essentialscore.api.web.http;

import com.essentialscore.api.web.auth.AuthenticationManager;
import com.essentialscore.api.web.rest.RestApiManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simplified HTTP server implementation for the WebUI.
 * This is a mock implementation that doesn't actually serve HTTP requests.
 */
public class HttpServer {
    private static final Logger LOGGER = Logger.getLogger(HttpServer.class.getName());
    
    private final Plugin plugin;
    private final String bindAddress;
    private final int port;
    private final RestApiManager apiManager;
    private final AuthenticationManager authManager;
    private final File webRoot;
    private final boolean sslEnabled;
    
    private boolean running = false;
    private final Map<String, String> mimeTypes;
    
    /**
     * Creates a new HTTP server
     * 
     * @param plugin The plugin instance
     * @param bindAddress The address to bind to
     * @param port The port to listen on
     * @param apiManager The REST API manager
     * @param authManager The authentication manager
     * @param webUIDirectory The WebUI directory
     * @param sslConfig SSL configuration (may be null)
     */
    public HttpServer(Plugin plugin, String bindAddress, int port, RestApiManager apiManager, 
                     AuthenticationManager authManager, File webUIDirectory, ConfigurationSection sslConfig) {
        this.plugin = plugin;
        this.bindAddress = bindAddress;
        this.port = port;
        this.apiManager = apiManager;
        this.authManager = authManager;
        this.webRoot = new File(webUIDirectory, "webapp");
        this.sslEnabled = sslConfig != null && sslConfig.getBoolean("enabled", false);
        
        // Initialize MIME types map
        this.mimeTypes = initMimeTypes();
    }
    
    /**
     * Initializes a map of file extensions to MIME types
     * 
     * @return The MIME types map
     */
    private Map<String, String> initMimeTypes() {
        Map<String, String> types = new HashMap<>();
        
        // Text formats
        types.put("html", "text/html");
        types.put("htm", "text/html");
        types.put("css", "text/css");
        types.put("js", "application/javascript");
        types.put("json", "application/json");
        types.put("xml", "application/xml");
        types.put("txt", "text/plain");
        
        // Image formats
        types.put("png", "image/png");
        types.put("jpg", "image/jpeg");
        types.put("jpeg", "image/jpeg");
        types.put("gif", "image/gif");
        types.put("ico", "image/x-icon");
        types.put("svg", "image/svg+xml");
        
        // Font formats
        types.put("ttf", "font/ttf");
        types.put("woff", "font/woff");
        types.put("woff2", "font/woff2");
        
        // Other formats
        types.put("pdf", "application/pdf");
        types.put("zip", "application/zip");
        
        return types;
    }
    
    /**
     * Starts the HTTP server
     */
    public void start() {
        running = true;
        LOGGER.info("HTTP server started on " + bindAddress + ":" + port + 
                (sslEnabled ? " (HTTPS)" : ""));
    }
    
    /**
     * Stops the HTTP server
     */
    public void stop() {
        running = false;
        LOGGER.info("HTTP server stopped");
    }
    
    /**
     * Checks if the server is running
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }
} 