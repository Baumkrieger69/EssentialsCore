package com.essentialscore.api.web;

import com.essentialscore.ApiCore;
import com.essentialscore.ConsoleFormatter;
import com.essentialscore.api.security.SecurityManager;
import com.essentialscore.api.web.auth.AuthenticationManager;
import com.essentialscore.api.web.http.HttpServer;
import com.essentialscore.api.web.rest.RestApiManager;
import com.essentialscore.api.web.socket.WebSocketServer;
import com.essentialscore.api.web.ui.DashboardManager;
import com.essentialscore.api.web.ui.FileManagerService;
import com.essentialscore.api.web.ui.LiveConsoleManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main manager class for the Advanced WebUI and Remote Management system.
 * This class initializes and manages all web-related components.
 */
public class WebUIManager {
    private static final Logger LOGGER = Logger.getLogger(WebUIManager.class.getName());
    
    private final Plugin plugin;
    private final SecurityManager securityManager;
    private final ScheduledExecutorService scheduler;
    private final File configFile;
    private final File webUIDirectory;
    private YamlConfiguration config;
    private ConsoleFormatter console;
    
    // Web servers
    private HttpServer httpServer;
    private WebSocketServer webSocketServer;
    
    // Managers
    private AuthenticationManager authManager;
    private RestApiManager restApiManager;
    private DashboardManager dashboardManager;
    private LiveConsoleManager liveConsoleManager;
    private FileManagerService fileManagerService;
    
    // Runtime data
    private final Map<String, WebSession> activeSessions = new ConcurrentHashMap<>();
    private boolean isRunning = false;
    
    /**
     * Creates a new WebUI Manager
     * 
     * @param plugin The plugin instance
     * @param securityManager The security manager for authentication and authorization
     * @param scheduler Scheduler for background tasks
     */
    public WebUIManager(Plugin plugin, SecurityManager securityManager, ScheduledExecutorService scheduler) {
        this.plugin = plugin;
        this.securityManager = securityManager;
        this.scheduler = scheduler;
        this.configFile = new File(plugin.getDataFolder(), "webui/config.yml");
        this.webUIDirectory = new File(plugin.getDataFolder(), "webui");
        
        // Initialize console formatter with a raw prefix
        String rawPrefix = "&8[&b&lWebUI&8]";
        this.console = new ConsoleFormatter(
            plugin.getLogger(), 
            rawPrefix, 
            true  // useColors
        );
    }
    
    /**
     * Initializes the WebUI system
     */
    public void initialize() {
        console.section("INITIALIZING WEBUI");
        
        try {
            // Create directories
            if (!webUIDirectory.exists() && !webUIDirectory.mkdirs()) {
                console.error("Could not create WebUI directory: " + webUIDirectory.getAbsolutePath());
                return;
            }
            
            // Extract default web resources if needed
            extractDefaultResources();
            
            // Load configuration
            loadConfiguration();
            
            // Initialize components
            initializeComponents();
            
            // Start servers
            startServers();
            
            isRunning = true;
            console.success("WebUI successfully initialized and running on port " + config.getInt("http.port", 8080));
            
        } catch (Exception e) {
            console.error("Failed to initialize WebUI: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "WebUI initialization error", e);
        }
    }
    
    /**
     * Loads configuration from file or creates default configuration
     */
    private void loadConfiguration() throws IOException {
        if (!configFile.exists()) {
            // Create default configuration
            config = new YamlConfiguration();
            
            // HTTP settings
            config.set("http.enabled", true);
            config.set("http.port", 8080);
            config.set("http.bind-address", "0.0.0.0");
            config.set("http.ssl.enabled", false);
            config.set("http.ssl.key-store", "keystore.jks");
            config.set("http.ssl.key-store-password", "changeit");
            
            // WebSocket settings
            config.set("websocket.enabled", true);
            config.set("websocket.port", 8081);
            
            // Security settings
            config.set("security.session-timeout-minutes", 30);
            config.set("security.max-login-attempts", 5);
            config.set("security.lockout-minutes", 15);
            config.set("security.cors.enabled", false);
            config.set("security.cors.allowed-origins", "http://localhost:3000,https://admin.yourdomain.com");
            
            // UI settings
            config.set("ui.dashboard.refresh-interval-seconds", 5);
            config.set("ui.console.max-lines", 1000);
            config.set("ui.console.auto-scroll", true);
            
            // Save default configuration
            config.save(configFile);
            console.info("Created default WebUI configuration");
        } else {
            // Load existing configuration
            config = YamlConfiguration.loadConfiguration(configFile);
            console.info("Loaded WebUI configuration");
        }
    }
    
    /**
     * Initializes all WebUI components
     */
    private void initializeComponents() {
        console.info("Initializing authentication manager");
        authManager = new AuthenticationManager(plugin, securityManager, config);
        
        console.info("Initializing REST API manager");
        restApiManager = new RestApiManager(plugin, securityManager, authManager);
        
        console.info("Initializing dashboard manager");
        dashboardManager = new DashboardManager(plugin, config);
        
        console.info("Initializing live console manager");
        liveConsoleManager = new LiveConsoleManager(plugin, config);
        
        console.info("Initializing file manager service");
        fileManagerService = new FileManagerService(plugin, securityManager);
    }
    
    /**
     * Starts the HTTP and WebSocket servers
     */
    private void startServers() {
        // Initialize HTTP server
        if (config.getBoolean("http.enabled", true)) {
            int httpPort = config.getInt("http.port", 8080);
            String bindAddress = config.getString("http.bind-address", "0.0.0.0");
            
            httpServer = new HttpServer(
                plugin, 
                bindAddress,
                httpPort, 
                restApiManager, 
                authManager,
                webUIDirectory,
                config.getConfigurationSection("http.ssl")
            );
            
            httpServer.start();
            console.info("HTTP server started on " + bindAddress + ":" + httpPort);
        }
        
        // Initialize WebSocket server
        if (config.getBoolean("websocket.enabled", true)) {
            int wsPort = config.getInt("websocket.port", 8081);
            String bindAddress = config.getString("http.bind-address", "0.0.0.0");
            
            webSocketServer = new WebSocketServer(
                plugin,
                bindAddress,
                wsPort,
                authManager,
                liveConsoleManager,
                dashboardManager
            );
            
            webSocketServer.start();
            console.info("WebSocket server started on " + bindAddress + ":" + wsPort);
        }
    }
    
    /**
     * Extracts default web resources to the plugin directory
     */
    private void extractDefaultResources() {
        File webappDir = new File(webUIDirectory, "webapp");
        if (!webappDir.exists()) {
            webappDir.mkdirs();
            console.info("Extracting default WebUI resources...");
            
            // Extract webapp files from plugin resources
            // This would be implemented to extract the packaged web application files
            extractResource("webapp/index.html", new File(webappDir, "index.html"));
            extractResource("webapp/css/main.css", new File(webappDir, "css/main.css"));
            extractResource("webapp/js/app.js", new File(webappDir, "js/app.js"));
            
            console.info("Default WebUI resources extracted");
        }
    }
    
    /**
     * Extracts a resource from the plugin jar to a file
     */
    private void extractResource(String resourcePath, File destination) {
        try {
            // Ensure parent directories exist
            if (!destination.getParentFile().exists()) {
                destination.getParentFile().mkdirs();
            }
            
            // Copy resource to file
            plugin.saveResource(resourcePath, false);
            console.info("Extracted resource: " + resourcePath);
        } catch (Exception e) {
            console.warning("Failed to extract resource " + resourcePath + ": " + e.getMessage());
        }
    }
    
    /**
     * Shuts down the WebUI system
     */
    public void shutdown() {
        if (!isRunning) return;
        
        console.section("SHUTTING DOWN WEBUI");
        
        // Stop WebSocket server
        if (webSocketServer != null) {
            webSocketServer.stop();
            console.info("WebSocket server stopped");
        }
        
        // Stop HTTP server
        if (httpServer != null) {
            httpServer.stop();
            console.info("HTTP server stopped");
        }
        
        // Clean up resources
        activeSessions.clear();
        isRunning = false;
        
        console.success("WebUI successfully shut down");
    }
    
    /**
     * Registers a new WebUI session
     * 
     * @param session The session to register
     */
    public void registerSession(WebSession session) {
        activeSessions.put(session.getSessionId(), session);
        console.info("Registered WebUI session: " + session.getSessionId() + " (" + activeSessions.size() + " active)");
    }
    
    /**
     * Unregisters a WebUI session
     * 
     * @param sessionId The session ID to unregister
     */
    public void unregisterSession(String sessionId) {
        activeSessions.remove(sessionId);
        console.info("Unregistered WebUI session: " + sessionId + " (" + activeSessions.size() + " active)");
    }
    
    /**
     * Gets the REST API manager
     * 
     * @return The REST API manager
     */
    public RestApiManager getRestApiManager() {
        return restApiManager;
    }
    
    /**
     * Gets the dashboard manager
     * 
     * @return The dashboard manager
     */
    public DashboardManager getDashboardManager() {
        return dashboardManager;
    }
    
    /**
     * Gets the live console manager
     * 
     * @return The live console manager
     */
    public LiveConsoleManager getLiveConsoleManager() {
        return liveConsoleManager;
    }
    
    /**
     * Gets the file manager service
     * 
     * @return The file manager service
     */
    public FileManagerService getFileManagerService() {
        return fileManagerService;
    }
    
    /**
     * Gets the WebUI configuration
     * 
     * @return The WebUI configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Broadcasts a message to all active WebUI sessions
     * 
     * @param event The event name
     * @param data The event data
     */
    public void broadcastToAll(String event, Map<String, Object> data) {
        // Simplified implementation
    }
    
    /**
     * Checks if the WebUI is running
     * 
     * @return true if the WebUI is running
     */
    public boolean isRunning() {
        return isRunning;
    }
} 