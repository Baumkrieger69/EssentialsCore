package com.essentialscore.api.web.rest;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an incoming API request.
 */
public class ApiRequest {
    private final String method;
    private final String[] pathSegments;
    private final Map<String, String> queryParams;
    private final String body;
    private final String username;
    private final String ipAddress;
    
    /**
     * Creates a new API request
     * 
     * @param method The HTTP method (GET, POST, PUT, DELETE)
     * @param pathSegments The path segments
     * @param queryParams The query parameters
     * @param body The request body (may be null)
     * @param username The authenticated username (may be null)
     * @param ipAddress The client IP address
     */
    public ApiRequest(String method, String[] pathSegments, Map<String, String> queryParams, 
                     String body, String username, String ipAddress) {
        this.method = method;
        this.pathSegments = pathSegments;
        this.queryParams = queryParams != null ? queryParams : new HashMap<>();
        this.body = body;
        this.username = username;
        this.ipAddress = ipAddress;
    }
    
    /**
     * Gets the HTTP method
     * 
     * @return The HTTP method
     */
    public String getMethod() {
        return method;
    }
    
    /**
     * Gets the path segments
     * 
     * @return The path segments
     */
    public String[] getPathSegments() {
        return pathSegments;
    }
    
    /**
     * Gets a path segment at the specified index
     * 
     * @param index The index
     * @return The path segment, or null if the index is out of bounds
     */
    public String getPathSegment(int index) {
        if (index < 0 || index >= pathSegments.length) {
            return null;
        }
        return pathSegments[index];
    }
    
    /**
     * Gets the query parameters
     * 
     * @return The query parameters
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }
    
    /**
     * Gets a query parameter
     * 
     * @param name The parameter name
     * @return The parameter value, or null if not found
     */
    public String getQueryParam(String name) {
        return queryParams.get(name);
    }
    
    /**
     * Gets a query parameter with a default value
     * 
     * @param name The parameter name
     * @param defaultValue The default value
     * @return The parameter value, or the default value if not found
     */
    public String getQueryParam(String name, String defaultValue) {
        return queryParams.getOrDefault(name, defaultValue);
    }

    /**
     * Gets a parameter (alias for getQueryParam)
     * 
     * @param name The parameter name
     * @return The parameter value, or null if not found
     */
    public String getParameter(String name) {
        return getQueryParam(name);
    }
    
    /**
     * Gets the request body
     * 
     * @return The request body, or null if none
     */
    public String getBody() {
        return body;
    }
    
    /**
     * Gets the authenticated username
     * 
     * @return The username, or null if not authenticated
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Checks if the request is authenticated
     * 
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return username != null;
    }
    
    /**
     * Gets the client IP address
     * 
     * @return The IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * Checks if the request is a GET request
     * 
     * @return true if GET
     */
    public boolean isGet() {
        return "GET".equalsIgnoreCase(method);
    }
    
    /**
     * Checks if the request is a POST request
     * 
     * @return true if POST
     */
    public boolean isPost() {
        return "POST".equalsIgnoreCase(method);
    }
    
    /**
     * Checks if the request is a PUT request
     * 
     * @return true if PUT
     */
    public boolean isPut() {
        return "PUT".equalsIgnoreCase(method);
    }
    
    /**
     * Checks if the request is a DELETE request
     * 
     * @return true if DELETE
     */
    public boolean isDelete() {
        return "DELETE".equalsIgnoreCase(method);
    }
}