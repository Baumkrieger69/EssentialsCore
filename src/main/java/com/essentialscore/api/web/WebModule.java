package com.essentialscore.api.web;

import com.essentialscore.api.BasePlugin;
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.api.ModuleLogger;
import com.essentialscore.api.security.SecurityManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Module that provides the WebUI functionality.
 */
public class WebModule implements Module {
    private static final Logger LOGGER = Logger.getLogger(WebModule.class.getName());
    
    private WebUIManager webUIManager;
    private FileConfiguration config;
    private File configFile;
    private ModuleAPI api;
    private ScheduledExecutorService scheduler;
    
    /**
     * Creates a new WebModule
     */
    public WebModule() {
        // Default constructor needed for module loading
    }
    
    @Override
    public void init(ModuleAPI api, FileConfiguration config) {
        this.api = api;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    @Override
    public String getName() {
        return "WebUI";
    }
    
    @Override
    public String getDescription() {
        return "Provides a web-based user interface for server administration";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    public boolean isEnabled() {
        return config != null && config.getBoolean("enabled", false);
    }
    
    @Override
    public void onEnable() {
        if (!isEnabled()) {
            LOGGER.info("WebUI module is disabled in configuration");
            return;
        }
        
        try {
            // Ensure web directories exist
            setupDirectories();
            
            // Extract default web files if necessary
            extractDefaultWebFiles();
            
            // Get the security manager
            Object securityManagerObj = api.getSharedData("SecurityManager");
            if (!(securityManagerObj instanceof SecurityManager)) {
                LOGGER.severe("Failed to get SecurityManager, WebUI module requires it");
                return;
            }
            
            SecurityManager securityManager = (SecurityManager) securityManagerObj;
            
            // Initialize web UI manager
            Plugin plugin = api.getPlugin();
            webUIManager = new WebUIManager(plugin, securityManager, scheduler);
            webUIManager.initialize();
            
            LOGGER.info("WebUI module enabled");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to enable WebUI module", e);
        }
    }
    
    @Override
    public void onDisable() {
        if (webUIManager != null) {
            webUIManager.shutdown();
            webUIManager = null;
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        LOGGER.info("WebUI module disabled");
    }
    
    /**
     * Gets the WebUI manager
     * 
     * @return The WebUI manager
     */
    public WebUIManager getWebUIManager() {
        return webUIManager;
    }
    
    /**
     * Loads the module configuration
     */
    private void loadConfig() {
        try {
            // Create data directory if it doesn't exist
            File dataFolder = api.getDataFolder();
            File webUIFolder = new File(dataFolder, "webui");
            if (!webUIFolder.exists()) {
                webUIFolder.mkdirs();
            }
            
            // Create config file
            configFile = new File(webUIFolder, "config.yml");
            
            if (!configFile.exists()) {
                // Copy default config
                api.extractResource("webui/config.yml", true);
                
                if (!configFile.exists()) {
                    // Create default config
                    configFile.createNewFile();
                    
                    // Set default values
                    FileConfiguration defaultConfig = new YamlConfiguration();
                    defaultConfig.set("enabled", true);
                    defaultConfig.set("http.enabled", true);
                    defaultConfig.set("http.bind-address", "0.0.0.0");
                    defaultConfig.set("http.port", 8080);
                    defaultConfig.set("websocket.enabled", true);
                    defaultConfig.set("websocket.bind-address", "0.0.0.0");
                    defaultConfig.set("websocket.port", 8081);
                    defaultConfig.set("auth.enabled", true);
                    defaultConfig.set("auth.session-timeout", 3600);
                    defaultConfig.set("ssl.enabled", false);
                    defaultConfig.set("ssl.key-store", "keystore.jks");
                    defaultConfig.set("ssl.key-store-password", "changeit");
                    defaultConfig.save(configFile);
                }
            }
            
            // Load config
            config = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load WebUI configuration", e);
            config = new YamlConfiguration();
            config.set("enabled", false); // Disable by default on error
        }
    }
    
    /**
     * Sets up directories for the WebUI
     */
    private void setupDirectories() {
        File dataFolder = api.getDataFolder();
        File webUIFolder = new File(dataFolder, "webui");
        
        // Create main directory
        if (!webUIFolder.exists()) {
            webUIFolder.mkdirs();
        }
        
        // Create web app directory
        File webappDir = new File(webUIFolder, "webapp");
        if (!webappDir.exists()) {
            webappDir.mkdirs();
        }
        
        // Create logs directory
        File logsDir = new File(webUIFolder, "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        
        // Create data directory
        File dataDir = new File(webUIFolder, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    /**
     * Extracts default web files from the plugin jar
     */
    private void extractDefaultWebFiles() {
        try {
            File dataFolder = api.getDataFolder();
            File webappDir = new File(dataFolder, "webui/webapp");
            
            // Check if webapp directory is empty
            if (webappDir.list() == null || webappDir.list().length == 0) {
                // Extract default web files
                extractResourceDirectory("webui/webapp", webappDir);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to extract default web files", e);
        }
    }
    
    /**
     * Extracts a resource directory from the plugin jar
     * 
     * @param resourcePath The resource path
     * @param targetDir The target directory
     * @throws IOException If an I/O error occurs
     */
    private void extractResourceDirectory(String resourcePath, File targetDir) throws IOException {
        // Create target directory if it doesn't exist
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        // Get list of resources
        String[] resources = getResourceListing(resourcePath);
        
        if (resources == null) {
            LOGGER.warning("No resources found at " + resourcePath);
            return;
        }
        
        // Extract each resource
        for (String resource : resources) {
            String fullPath = resourcePath + "/" + resource;
            
            // Skip directories (they will be created when extracting files)
            if (resource.endsWith("/")) {
                extractResourceDirectory(fullPath, new File(targetDir, resource));
                continue;
            }
            
            // Extract file
            try (InputStream is = api.getPlugin().getResource(fullPath)) {
                if (is != null) {
                    File targetFile = new File(targetDir, resource);
                    
                    // Create parent directories
                    if (!targetFile.getParentFile().exists()) {
                        targetFile.getParentFile().mkdirs();
                    }
                    
                    // Copy file
                    Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    LOGGER.warning("Resource not found: " + fullPath);
                }
            }
        }
    }
    
    /**
     * Gets a listing of resources in a directory
     * 
     * @param path The resource path
     * @return An array of resource names
     */
    private String[] getResourceListing(String path) {
        // This is a best-effort method, as there's no standard way to list resources in a JAR
        // In a real implementation, you might want to use a hardcoded list or more advanced methods
        
        try {
            // Return a list of known web files
            // In a real implementation, you would need to determine this list
            if (path.equals("webui/webapp")) {
                return new String[]{
                        "index.html",
                        "css/",
                        "css/main.css",
                        "js/",
                        "js/app.js",
                        "js/api.js",
                        "js/dashboard.js",
                        "js/console.js",
                        "js/file-manager.js",
                        "images/",
                        "images/logo.png",
                        "favicon.ico"
                };
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get resource listing for " + path, e);
        }
        
        return null;
    }
    
    /**
     * Reloads the module configuration
     */
    public void reloadConfig() {
        // Stop the WebUI manager
        if (webUIManager != null) {
            webUIManager.shutdown();
        }
        
        // Reload config
        loadConfig();
        
        // Restart if enabled
        if (isEnabled()) {
            SecurityManager securityManager = (SecurityManager) api.getSharedData("SecurityManager");
            if (securityManager != null) {
                webUIManager = new WebUIManager(api.getPlugin(), securityManager, scheduler);
                webUIManager.initialize();
            }
        }
    }

    public void onLoad(BasePlugin plugin, ModuleLogger logger, FileConfiguration config) {
        this.config = config;
        loadConfig();
    }

    @Override
    public String getId() {
        return "webui";
    }

    @Override
    public String getAuthor() {
        return "EssentialsCore Team";
    }

    public BasePlugin getPlugin() {
        return (BasePlugin) api.getPlugin();
    }

    public ModuleLogger getLogger() {
        return new ModuleLogger(LOGGER, getName(), false);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    @Override
    public Map<String, String> getDependencies() {
        return Map.of("security", "1.0");
    }

    public String[] getSoftDependencies() {
        return new String[0];
    }

    public void onReload() {
        // Reload configuration
        loadConfig();
        
        // Reinitialize the WebUI if it's running
        if (webUIManager != null) {
            // First shut down existing manager
            webUIManager.shutdown();
            
            // Then initialize again with new settings
            try {
                webUIManager.initialize();
                LOGGER.info("WebUI module reloaded");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to reload WebUI module", e);
            }
        }
    }
} 