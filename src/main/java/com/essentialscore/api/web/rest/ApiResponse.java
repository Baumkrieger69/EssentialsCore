package com.essentialscore.api.web.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an outgoing API response.
 */
public class ApiResponse {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final int statusCode;
    private final String body;
    
    /**
     * Creates a new API response
     * 
     * @param statusCode The HTTP status code
     * @param body The response body
     */
    public ApiResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }
    
    /**
     * Creates a new API response from an object that will be converted to JSON
     * 
     * @param statusCode The HTTP status code
     * @param object The object to convert to JSON
     */
    public ApiResponse(int statusCode, Object object) {
        this.statusCode = statusCode;
        this.body = GSON.toJson(object);
    }
    
    /**
     * Gets the HTTP status code
     * 
     * @return The status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Gets the response body
     * 
     * @return The body
     */
    public String getBody() {
        return body;
    }
    
    /**
     * Creates a success response with a status code of 200
     * 
     * @param object The response data
     * @return The API response
     */
    public static ApiResponse ok(Object object) {
        return new ApiResponse(200, object);
    }
    
    /**
     * Creates a success response with a message
     * 
     * @param message The success message
     * @return The API response
     */
    public static ApiResponse ok(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return new ApiResponse(200, response);
    }
    
    /**
     * Creates a created response with a status code of 201
     * 
     * @param object The created object
     * @return The API response
     */
    public static ApiResponse created(Object object) {
        return new ApiResponse(201, object);
    }
    
    /**
     * Creates a no content response with a status code of 204
     * 
     * @return The API response
     */
    public static ApiResponse noContent() {
        return new ApiResponse(204, "");
    }
    
    /**
     * Creates a bad request response with a status code of 400
     * 
     * @param message The error message
     * @return The API response
     */
    public static ApiResponse badRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return new ApiResponse(400, response);
    }
    
    /**
     * Creates an unauthorized response with a status code of 401
     * 
     * @param message The error message
     * @return The API response
     */
    public static ApiResponse unauthorized(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return new ApiResponse(401, response);
    }
    
    /**
     * Creates a forbidden response with a status code of 403
     * 
     * @param message The error message
     * @return The API response
     */
    public static ApiResponse forbidden(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return new ApiResponse(403, response);
    }
    
    /**
     * Creates a not found response with a status code of 404
     * 
     * @param message The error message
     * @return The API response
     */
    public static ApiResponse notFound(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return new ApiResponse(404, response);
    }
    
    /**
     * Creates a method not allowed response with a status code of 405
     * 
     * @param message The error message
     * @return The API response
     */
    public static ApiResponse methodNotAllowed(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return new ApiResponse(405, response);
    }
    
    /**
     * Creates an internal server error response with a status code of 500
     * 
     * @param message The error message
     * @return The API response
     */
    public static ApiResponse error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return new ApiResponse(500, response);
    }
    
    /**
     * Creates an internal server error response with a status code of 500
     * 
     * @param throwable The exception
     * @return The API response
     */
    public static ApiResponse error(Throwable throwable) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", throwable.getMessage());
        return new ApiResponse(500, response);
    }
} 