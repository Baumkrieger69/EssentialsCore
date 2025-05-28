package com.essentialscore.api.web.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for all API endpoints.
 */
public abstract class ApiEndpoint {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final JsonParser JSON_PARSER = new JsonParser();
    protected final Logger logger;
    protected final Plugin plugin;
    
    /**
     * Creates a new API endpoint
     * 
     * @param plugin The plugin instance
     */
    public ApiEndpoint(Plugin plugin) {
        this.plugin = plugin;
        this.logger = Logger.getLogger(getClass().getName());
    }
    
    /**
     * Gets the endpoint path
     * 
     * @return The path
     */
    public abstract String getPath();
    
    /**
     * Handles an API request
     * 
     * @param request The API request
     * @return The API response
     */
    public abstract ApiResponse handleRequest(ApiRequest request);
    
    /**
     * Checks if this endpoint requires authentication
     * 
     * @return true if authentication is required
     */
    public boolean requiresAuthentication() {
        return true;
    }
    
    /**
     * Gets the permission required to access this endpoint
     * 
     * @return The required permission, or null if no specific permission is required
     */
    public String getRequiredPermission() {
        return null;
    }
    
    /**
     * Parses a JSON request body
     * 
     * @param request The API request
     * @return The parsed JSON object, or null if parsing failed
     */
    protected JsonObject parseJsonBody(ApiRequest request) {
        try {
            if (request.getBody() == null || request.getBody().isEmpty()) {
                return null;
            }
            
            return JSON_PARSER.parse(request.getBody()).getAsJsonObject();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse JSON body: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Creates a success response with the given message
     * 
     * @param message The success message
     * @return The API response
     */
    protected ApiResponse success(String message) {
        return ApiResponse.ok(message);
    }
    
    /**
     * Creates a success response with the given data
     * 
     * @param data The response data
     * @return The API response
     */
    protected ApiResponse success(Object data) {
        return ApiResponse.ok(data);
    }
    
    /**
     * Creates an error response with the given message
     * 
     * @param message The error message
     * @return The API response
     */
    protected ApiResponse error(String message) {
        return ApiResponse.error(message);
    }
    
    /**
     * Creates an error response with the given exception
     * 
     * @param e The exception
     * @return The API response
     */
    protected ApiResponse error(Exception e) {
        logger.log(Level.WARNING, "API error: " + e.getMessage(), e);
        return ApiResponse.error(e);
    }
    
    /**
     * Creates a bad request response with the given message
     * 
     * @param message The error message
     * @return The API response
     */
    protected ApiResponse badRequest(String message) {
        return ApiResponse.badRequest(message);
    }
    
    /**
     * Creates a not found response with the given message
     * 
     * @param message The error message
     * @return The API response
     */
    protected ApiResponse notFound(String message) {
        return ApiResponse.notFound(message);
    }
    
    /**
     * Creates a forbidden response with the given message
     * 
     * @param message The error message
     * @return The API response
     */
    protected ApiResponse forbidden(String message) {
        return ApiResponse.forbidden(message);
    }
    
    /**
     * Creates an unauthorized response with the given message
     * 
     * @param message The error message
     * @return The API response
     */
    protected ApiResponse unauthorized(String message) {
        return ApiResponse.unauthorized(message);
    }
    
    /**
     * Creates a response for when a required parameter is missing
     * 
     * @param paramName The parameter name
     * @return The API response
     */
    protected ApiResponse missingRequiredParam(String paramName) {
        return badRequest("Missing required parameter: " + paramName);
    }
    
    /**
     * Gets a string from a JSON object
     * 
     * @param json The JSON object
     * @param key The key
     * @return The string value, or null if not found
     */
    protected String getJsonString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? 
                json.get(key).getAsString() : null;
    }
    
    /**
     * Gets an integer from a JSON object
     * 
     * @param json The JSON object
     * @param key The key
     * @param defaultValue The default value
     * @return The integer value, or the default value if not found
     */
    protected int getJsonInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? 
                json.get(key).getAsInt() : defaultValue;
    }
    
    /**
     * Gets a boolean from a JSON object
     * 
     * @param json The JSON object
     * @param key The key
     * @param defaultValue The default value
     * @return The boolean value, or the default value if not found
     */
    protected boolean getJsonBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? 
                json.get(key).getAsBoolean() : defaultValue;
    }
} 