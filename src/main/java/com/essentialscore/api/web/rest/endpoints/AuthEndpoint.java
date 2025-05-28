package com.essentialscore.api.web.rest.endpoints;

import com.essentialscore.api.web.auth.AuthenticationManager;
import com.essentialscore.api.web.auth.AuthenticationManager.AuthResult;
import com.essentialscore.api.web.rest.ApiEndpoint;
import com.essentialscore.api.web.rest.ApiRequest;
import com.essentialscore.api.web.rest.ApiResponse;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication endpoint for login and token validation.
 */
public class AuthEndpoint extends ApiEndpoint {
    
    private final AuthenticationManager authManager;
    
    /**
     * Creates a new authentication endpoint
     * 
     * @param authManager The authentication manager
     */
    public AuthEndpoint(AuthenticationManager authManager) {
        super(null);
        this.authManager = authManager;
    }
    
    @Override
    public String getPath() {
        return "auth";
    }
    
    @Override
    public boolean requiresAuthentication() {
        return false; // Auth endpoint is public
    }
    
    @Override
    public ApiResponse handleRequest(ApiRequest request) {
        try {
            // Get subpath
            String subpath = request.getPathSegment(1);
            
            if (subpath == null || "login".equalsIgnoreCase(subpath)) {
                // Login request
                if (!request.isPost()) {
                    return badRequest("Login requires POST method");
                }
                
                // Parse request body
                JsonObject body = parseJsonBody(request);
                if (body == null) {
                    return badRequest("Invalid request format");
                }
                
                // Get credentials
                String username = getJsonString(body, "username");
                String password = getJsonString(body, "password");
                
                if (username == null || password == null) {
                    return badRequest("Missing username or password");
                }
                
                // Authenticate
                AuthResult result = authManager.authenticate(username, password, request.getIpAddress());
                
                if (result.isSuccess()) {
                    // Create response with token
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("token", result.getSession().getSessionId());
                    response.put("username", result.getSession().getUsername());
                    
                    return success(response);
                } else {
                    return unauthorized(result.getMessage());
                }
            } else if ("logout".equalsIgnoreCase(subpath)) {
                // Logout request
                if (!request.isPost()) {
                    return badRequest("Logout requires POST method");
                }
                
                // Get token from headers
                String token = request.getQueryParam("token");
                if (token == null) {
                    // Try to get token from body
                    JsonObject body = parseJsonBody(request);
                    if (body != null) {
                        token = getJsonString(body, "token");
                    }
                }
                
                if (token == null) {
                    return badRequest("Missing token");
                }
                
                // Invalidate token
                authManager.invalidateToken(token);
                
                return success("Logged out successfully");
            } else if ("validate".equalsIgnoreCase(subpath)) {
                // Token validation request
                String token = request.getQueryParam("token");
                if (token == null) {
                    return badRequest("Missing token");
                }
                
                // Validate token
                String username = authManager.validateToken(token);
                
                if (username != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("username", username);
                    
                    return success(response);
                } else {
                    return unauthorized("Invalid token");
                }
            } else {
                return notFound("Unknown endpoint: " + subpath);
            }
        } catch (Exception e) {
            return error(e);
        }
    }
} 