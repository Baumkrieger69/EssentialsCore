package com.essentialscore.api.web.socket;

import com.essentialscore.api.web.auth.AuthenticationManager;
import com.essentialscore.api.web.ui.DashboardManager;
import com.essentialscore.api.web.ui.LiveConsoleManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket server for real-time communication with the WebUI.
 * This is a simplified implementation that doesn't depend on external WebSocket libraries.
 */
public class WebSocketServer {
    private static final Logger LOGGER = Logger.getLogger(WebSocketServer.class.getName());
    private static final Gson GSON = new GsonBuilder().create();
    
    private final Plugin plugin;
    private final String bindAddress;
    private final int port;
    private final AuthenticationManager authManager;
    private final LiveConsoleManager liveConsoleManager;
    private final DashboardManager dashboardManager;
    private final Map<String, WebSocketSession> sessions;
    private final Map<String, WebSocketMessageHandler> messageHandlers;
    
    private boolean running = false;
    
    /**
     * Creates a new WebSocket server
     * 
     * @param plugin The plugin instance
     * @param bindAddress The address to bind to
     * @param port The port to listen on
     * @param authManager The authentication manager
     * @param liveConsoleManager The live console manager
     * @param dashboardManager The dashboard manager
     */
    public WebSocketServer(Plugin plugin, String bindAddress, int port, AuthenticationManager authManager,
                          LiveConsoleManager liveConsoleManager, DashboardManager dashboardManager) {
        this.plugin = plugin;
        this.bindAddress = bindAddress;
        this.port = port;
        this.authManager = authManager;
        this.liveConsoleManager = liveConsoleManager;
        this.dashboardManager = dashboardManager;
        this.sessions = new ConcurrentHashMap<>();
        this.messageHandlers = new ConcurrentHashMap<>();
        
        // Register message handlers
        registerMessageHandlers();
    }
    
    /**
     * Registers message handlers for different types of WebSocket messages
     */
    private void registerMessageHandlers() {
        // Authentication handler
        registerMessageHandler("auth", (session, message) -> {
            String token = message.has("token") ? message.get("token").getAsString() : null;
            
            if (token == null || token.isEmpty()) {
                sendError(session, "Authentication failed: Missing token");
                return;
            }
            
            // Validate token
            String username = authManager.validateToken(token);
            if (username == null) {
                sendError(session, "Authentication failed: Invalid token");
                return;
            }
            
            // Update session
            session.setAuthenticated(true);
            session.setUsername(username);
            
            // Send success response
            JsonObject response = new JsonObject();
            response.addProperty("event", "auth_success");
            response.addProperty("username", username);
            send(session, response.toString());
            
            LOGGER.info("WebSocket authenticated: " + username);
        });
        
        // Subscribe to console handler
        registerMessageHandler("subscribe_console", (session, message) -> {
            if (!session.isAuthenticated()) {
                sendError(session, "Authentication required");
                return;
            }
            
            // Check permission
            if (!authManager.hasPermission(session.getUsername(), "console.view")) {
                sendError(session, "Permission denied");
                return;
            }
            
            // Subscribe to console
            liveConsoleManager.subscribe(session.getSessionId(), line -> {
                JsonObject response = new JsonObject();
                response.addProperty("event", "console_line");
                response.addProperty("line", line);
                send(session, response.toString());
            });
            
            // Send success response
            JsonObject response = new JsonObject();
            response.addProperty("event", "subscribed");
            response.addProperty("target", "console");
            send(session, response.toString());
            
            // Send initial console history
            String[] history = liveConsoleManager.getConsoleHistory();
            JsonObject historyResponse = new JsonObject();
            historyResponse.addProperty("event", "console_history");
            historyResponse.add("lines", GSON.toJsonTree(history));
            send(session, historyResponse.toString());
        });
        
        // Unsubscribe from console handler
        registerMessageHandler("unsubscribe_console", (session, message) -> {
            if (!session.isAuthenticated()) {
                sendError(session, "Authentication required");
                return;
            }
            
            // Unsubscribe from console
            liveConsoleManager.unsubscribe(session.getSessionId());
            
            // Send success response
            JsonObject response = new JsonObject();
            response.addProperty("event", "unsubscribed");
            response.addProperty("target", "console");
            send(session, response.toString());
        });
        
        // Subscribe to dashboard handler
        registerMessageHandler("subscribe_dashboard", (session, message) -> {
            if (!session.isAuthenticated()) {
                sendError(session, "Authentication required");
                return;
            }
            
            // Check permission
            if (!authManager.hasPermission(session.getUsername(), "dashboard.view")) {
                sendError(session, "Permission denied");
                return;
            }
            
            // Subscribe to dashboard updates
            dashboardManager.subscribe(session.getSessionId(), data -> {
                JsonObject response = new JsonObject();
                response.addProperty("event", "dashboard_update");
                response.add("data", GSON.toJsonTree(data));
                send(session, response.toString());
            });
            
            // Send success response
            JsonObject response = new JsonObject();
            response.addProperty("event", "subscribed");
            response.addProperty("target", "dashboard");
            send(session, response.toString());
            
            // Send initial dashboard data
            Map<String, Object> data = dashboardManager.getDashboardData();
            JsonObject dataResponse = new JsonObject();
            dataResponse.addProperty("event", "dashboard_data");
            dataResponse.add("data", GSON.toJsonTree(data));
            send(session, dataResponse.toString());
        });
        
        // Unsubscribe from dashboard handler
        registerMessageHandler("unsubscribe_dashboard", (session, message) -> {
            if (!session.isAuthenticated()) {
                sendError(session, "Authentication required");
                return;
            }
            
            // Unsubscribe from dashboard
            dashboardManager.unsubscribe(session.getSessionId());
            
            // Send success response
            JsonObject response = new JsonObject();
            response.addProperty("event", "unsubscribed");
            response.addProperty("target", "dashboard");
            send(session, response.toString());
        });
        
        // Execute console command handler
        registerMessageHandler("execute_command", (session, message) -> {
            if (!session.isAuthenticated()) {
                sendError(session, "Authentication required");
                return;
            }
            
            // Check permission
            if (!authManager.hasPermission(session.getUsername(), "console.execute")) {
                sendError(session, "Permission denied");
                return;
            }
            
            // Get command
            String command = message.has("command") ? message.get("command").getAsString() : null;
            if (command == null || command.isEmpty()) {
                sendError(session, "Missing command");
                return;
            }
            
            // Execute command
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                boolean success = plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(), command);
                
                // Send response
                JsonObject response = new JsonObject();
                response.addProperty("event", "command_result");
                response.addProperty("success", success);
                send(session, response.toString());
            });
        });
        
        // Ping handler
        registerMessageHandler("ping", (session, message) -> {
            JsonObject response = new JsonObject();
            response.addProperty("event", "pong");
            response.addProperty("time", System.currentTimeMillis());
            send(session, response.toString());
        });
    }
    
    /**
     * Registers a message handler
     * 
     * @param type The message type
     * @param handler The handler
     */
    public void registerMessageHandler(String type, WebSocketMessageHandler handler) {
        messageHandlers.put(type, handler);
    }
    
    /**
     * Starts the WebSocket server
     * Note: This is a simplified implementation that doesn't actually start a real WebSocket server.
     */
    public void start() {
        running = true;
        LOGGER.info("WebSocket server started on " + bindAddress + ":" + port);
    }
    
    /**
     * Stops the WebSocket server
     */
    public void stop() {
        running = false;
        
        // Close all sessions
        for (WebSocketSession session : sessions.values()) {
            JsonObject response = new JsonObject();
            response.addProperty("event", "server_shutdown");
            response.addProperty("message", "Server is shutting down");
            send(session, response.toString());
        }
        
        sessions.clear();
        LOGGER.info("WebSocket server stopped");
    }
    
    /**
     * Sends a message to a WebSocket client
     * 
     * @param session The WebSocket session
     * @param message The message
     */
    private void send(WebSocketSession session, String message) {
        if (session.isOpen()) {
            session.sendMessage(message);
        }
    }
    
    /**
     * Sends an error message to a WebSocket client
     * 
     * @param session The WebSocket session
     * @param error The error message
     */
    private void sendError(WebSocketSession session, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("event", "error");
        response.addProperty("message", error);
        send(session, response.toString());
    }
    
    /**
     * Broadcasts a message to all authenticated clients
     * 
     * @param message The message
     */
    public void broadcast(String message) {
        for (WebSocketSession session : sessions.values()) {
            if (session.isAuthenticated()) {
                send(session, message);
            }
        }
    }
    
    /**
     * Registers a new WebSocket session
     * 
     * @param sessionId The session ID
     * @return The created session
     */
    public WebSocketSession createSession(String sessionId) {
        WebSocketSession session = new WebSocketSession(sessionId, "/");
        sessions.put(sessionId, session);
        return session;
    }
    
    /**
     * Handles a WebSocket message
     * 
     * @param sessionId The session ID
     * @param message The message
     */
    public void handleMessage(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null) {
            LOGGER.warning("Session not found: " + sessionId);
            return;
        }
        
        try {
            // Parse message
            JsonObject json = new JsonParser().parse(message).getAsJsonObject();
            
            // Get message type
            String type = json.has("type") ? json.get("type").getAsString() : null;
            if (type == null || type.isEmpty()) {
                sendError(session, "Missing message type");
                return;
            }
            
            // Get message data
            JsonObject data = json.has("data") ? json.get("data").getAsJsonObject() : new JsonObject();
            
            // Handle message
            WebSocketMessageHandler handler = messageHandlers.get(type);
            if (handler != null) {
                handler.handle(session, data);
            } else {
                sendError(session, "Unknown message type: " + type);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error handling WebSocket message", e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
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