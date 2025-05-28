package com.essentialscore.api.web.rest;

import com.essentialscore.api.security.SecurityManager;
import com.essentialscore.api.web.auth.AuthenticationManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Simplified REST API manager implementation.
 */
public class RestApiManager {
    private static final Logger LOGGER = Logger.getLogger(RestApiManager.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Plugin plugin;
    private final SecurityManager securityManager;
    private final AuthenticationManager authManager;
    private final Map<String, ApiEndpoint> endpoints;
    
    /**
     * Creates a new REST API manager
     * 
     * @param plugin The plugin instance
     * @param securityManager The security manager
     * @param authManager The authentication manager
     */
    public RestApiManager(Plugin plugin, SecurityManager securityManager, AuthenticationManager authManager) {
        this.plugin = plugin;
        this.securityManager = securityManager;
        this.authManager = authManager;
        this.endpoints = new ConcurrentHashMap<>();
        
        // Register default endpoints
        registerDefaultEndpoints();
    }
    
    /**
     * Registers default API endpoints
     */
    private void registerDefaultEndpoints() {
        // Register a minimal set of endpoints for demonstration
        registerEndpoint(new ApiEndpoint(plugin) {
            @Override
            public String getPath() {
                return "server";
            }
            
            @Override
            public ApiResponse handleRequest(ApiRequest request) {
                Map<String, Object> serverInfo = new HashMap<>();
                serverInfo.put("name", plugin.getServer().getName());
                serverInfo.put("version", plugin.getServer().getVersion());
                serverInfo.put("maxPlayers", plugin.getServer().getMaxPlayers());
                serverInfo.put("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
                return ApiResponse.ok(serverInfo);
            }
        });
        
        LOGGER.info("Registered default REST API endpoints");
    }
    
    /**
     * Registers an API endpoint
     * 
     * @param endpoint The endpoint to register
     */
    public void registerEndpoint(ApiEndpoint endpoint) {
        endpoints.put(endpoint.getPath(), endpoint);
    }
    
    /**
     * Unregisters an API endpoint
     * 
     * @param path The endpoint path
     */
    public void unregisterEndpoint(String path) {
        endpoints.remove(path);
    }
    
    /**
     * Handles an API request (simplified for mock implementation)
     * 
     * @param path The request path
     * @param method The request method
     * @param body The request body
     * @param token The authentication token
     * @return The API response
     */
    public ApiResponse handleRequest(String path, String method, String body, String token) {
        try {
            // Remove leading slashes
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // Split path into segments
            String[] segments = path.split("/");
            if (segments.length == 0) {
                return ApiResponse.notFound("API endpoint not found");
            }
            
            // Find endpoint
            ApiEndpoint endpoint = endpoints.get(segments[0]);
            if (endpoint == null) {
                return ApiResponse.notFound("API endpoint not found: " + segments[0]);
            }
            
            // Check if authentication is required
            if (endpoint.requiresAuthentication()) {
                if (token == null || token.isEmpty()) {
                    return ApiResponse.unauthorized("Authentication required");
                }
                
                // Validate token
                String username = authManager.validateToken(token);
                if (username == null) {
                    return ApiResponse.unauthorized("Invalid or expired token");
                }
                
                // Check permission if required
                if (endpoint.getRequiredPermission() != null) {
                    if (!authManager.hasPermission(username, endpoint.getRequiredPermission())) {
                        return ApiResponse.forbidden("Insufficient permissions");
                    }
                }
            }
            
            // Create mock request
            ApiRequest request = new ApiRequest(method, segments, new HashMap<>(), body, null, "127.0.0.1");
            
            // Process request
            return endpoint.handleRequest(request);
        } catch (Exception e) {
            LOGGER.warning("Error processing API request: " + path + " - " + e.getMessage());
            return ApiResponse.error("Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Gets all registered API endpoints
     * 
     * @return The API endpoints
     */
    public Map<String, ApiEndpoint> getEndpoints() {
        return new HashMap<>(endpoints);
    }
} 