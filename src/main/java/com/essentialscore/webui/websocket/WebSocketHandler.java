package com.essentialscore.webui.websocket;

import com.google.gson.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket-Server für Real-time Kommunikation mit der WebUI
 * Unterstützt bidirektionale Kommunikation, Broadcasting und Event-Management
 */
public class WebSocketHandler extends WebSocketServer {
    
    private static final Logger logger = Logger.getLogger(WebSocketHandler.class.getName());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Client-Management
    private final Map<WebSocket, ClientSession> clients = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocket>> channels = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Event-Handler
    private final Map<String, EventHandler> eventHandlers = new ConcurrentHashMap<>();
    private final Queue<WebSocketMessage> messageQueue = new ConcurrentLinkedQueue<>();
    
    // Konfiguration
    private final long heartbeatInterval = 30000; // 30 Sekunden
    private final long sessionTimeout = 300000; // 5 Minuten
    
    public WebSocketHandler(int port) {
        super(new InetSocketAddress(port));
        this.initializeEventHandlers();
        this.startHeartbeatTask();
        this.startMessageProcessor();
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            ClientSession session = new ClientSession(conn);
            clients.put(conn, session);
            
            logger.info("WebSocket-Verbindung geöffnet: " + conn.getRemoteSocketAddress());
            
            // Willkommensnachricht senden
            sendToClient(conn, createMessage("connection", "welcome", Map.of(
                "sessionId", session.getId(),
                "serverTime", System.currentTimeMillis(),
                "features", Arrays.asList("realtime", "monitoring", "management")
            )));
            
            // Initial-Daten senden
            sendInitialData(conn);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fehler beim Öffnen der WebSocket-Verbindung", e);
        }
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        try {
            ClientSession session = clients.remove(conn);
            if (session != null) {
                // Aus allen Channels entfernen
                channels.values().forEach(channelClients -> channelClients.remove(conn));
                logger.info("WebSocket-Verbindung geschlossen: " + conn.getRemoteSocketAddress() + 
                          " (Code: " + code + ", Grund: " + reason + ")");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fehler beim Schließen der WebSocket-Verbindung", e);
        }
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            WebSocketMessage wsMessage = gson.fromJson(message, WebSocketMessage.class);
            handleMessage(conn, wsMessage);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fehler beim Verarbeiten der WebSocket-Nachricht: " + message, e);
            sendError(conn, "INVALID_MESSAGE", "Nachricht konnte nicht verarbeitet werden");
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.log(Level.SEVERE, "WebSocket-Fehler für " + 
                  (conn != null ? conn.getRemoteSocketAddress() : "unbekannte Verbindung"), ex);
    }
    
    @Override
    public void onStart() {
        logger.info("WebSocket-Server gestartet auf Port " + getPort());
    }
    
    /**
     * Nachricht an einen spezifischen Client senden
     */
    public void sendToClient(WebSocket client, WebSocketMessage message) {
        if (client != null && client.isOpen()) {
            try {
                client.send(gson.toJson(message));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Fehler beim Senden an Client", e);
            }
        }
    }
    
    /**
     * Nachricht an alle Clients in einem Channel senden
     */
    public void broadcast(String channel, WebSocketMessage message) {
        Set<WebSocket> channelClients = channels.get(channel);
        if (channelClients != null) {
            for (WebSocket client : new HashSet<>(channelClients)) {
                sendToClient(client, message);
            }
        }
    }
    
    /**
     * Nachricht an alle verbundenen Clients senden
     */
    public void broadcastToAll(WebSocketMessage message) {
        for (WebSocket client : new HashSet<>(clients.keySet())) {
            sendToClient(client, message);
        }
    }
    
    /**
     * Server-Performance-Daten senden
     */
    public void sendPerformanceUpdate() {
        try {
            Map<String, Object> performanceData = Map.of(
                "timestamp", System.currentTimeMillis(),
                "cpu", getCurrentCPUUsage(),
                "memory", getMemoryInfo(),
                "tps", getCurrentTPS(),
                "players", getOnlinePlayerCount(),
                "uptime", getServerUptime()
            );
            
            WebSocketMessage message = createMessage("performance", "update", performanceData);
            broadcast("performance", message);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fehler beim Senden der Performance-Daten", e);
        }
    }
    
    /**
     * Player-Update senden
     */
    public void sendPlayerUpdate(String action, Map<String, Object> playerData) {
        WebSocketMessage message = createMessage("players", action, playerData);
        broadcast("players", message);
    }
    
    /**
     * Console-Output weiterleiten
     */
    public void sendConsoleOutput(String line, String level) {
        WebSocketMessage message = createMessage("console", "output", Map.of(
            "line", line,
            "level", level,
            "timestamp", System.currentTimeMillis()
        ));
        broadcast("console", message);
    }
    
    /**
     * System-Alert senden
     */
    public void sendAlert(String type, String title, String message, String severity) {
        WebSocketMessage alertMessage = createMessage("alerts", "new", Map.of(
            "type", type,
            "title", title,
            "message", message,
            "severity", severity,
            "timestamp", System.currentTimeMillis()
        ));
        broadcastToAll(alertMessage);
    }
    
    // Private Methoden
    
    private void handleMessage(WebSocket conn, WebSocketMessage message) {
        ClientSession session = clients.get(conn);
        if (session != null) {
            session.updateLastActivity();
        }
        
        EventHandler handler = eventHandlers.get(message.getType());
        if (handler != null) {
            handler.handle(conn, message);
        } else {
            logger.warning("Unbekannter Message-Type: " + message.getType());
            sendError(conn, "UNKNOWN_TYPE", "Unbekannter Nachrichtentyp: " + message.getType());
        }
    }
    
    private void initializeEventHandlers() {
        // Authentifizierung
        eventHandlers.put("auth", (conn, msg) -> handleAuthentication(conn, msg));
        
        // Channel-Subscription
        eventHandlers.put("subscribe", (conn, msg) -> handleSubscription(conn, msg));
        eventHandlers.put("unsubscribe", (conn, msg) -> handleUnsubscription(conn, msg));
        
        // Heartbeat
        eventHandlers.put("ping", (conn, msg) -> handlePing(conn, msg));
        
        // Server-Commands
        eventHandlers.put("command", (conn, msg) -> handleServerCommand(conn, msg));
        
        // Request-Response Pattern
        eventHandlers.put("request", (conn, msg) -> handleRequest(conn, msg));
    }
    
    private void handleAuthentication(WebSocket conn, WebSocketMessage message) {
        try {
            JsonObject data = message.getData().getAsJsonObject();
            String token = data.get("token").getAsString();
            
            // Token validieren (hier würde echte Authentifizierung stattfinden)
            boolean isValid = validateAuthToken(token);
            
            if (isValid) {
                ClientSession session = clients.get(conn);
                session.setAuthenticated(true);
                session.setUserId(data.get("userId").getAsString());
                
                sendToClient(conn, createMessage("auth", "success", Map.of(
                    "authenticated", true,
                    "permissions", getUserPermissions(session.getUserId())
                )));
                
                logger.info("Client authentifiziert: " + session.getUserId());
            } else {
                sendError(conn, "AUTH_FAILED", "Authentifizierung fehlgeschlagen");
            }
        } catch (Exception e) {
            sendError(conn, "AUTH_ERROR", "Fehler bei der Authentifizierung");
        }
    }
    
    private void handleSubscription(WebSocket conn, WebSocketMessage message) {
        try {
            String channel = message.getData().getAsJsonObject().get("channel").getAsString();
            channels.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(conn);
            
            sendToClient(conn, createMessage("subscription", "success", Map.of(
                "channel", channel,
                "subscribed", true
            )));
            
            logger.info("Client zu Channel hinzugefügt: " + channel);
        } catch (Exception e) {
            sendError(conn, "SUBSCRIPTION_ERROR", "Fehler beim Abonnieren des Channels");
        }
    }
    
    private void handleUnsubscription(WebSocket conn, WebSocketMessage message) {
        try {
            String channel = message.getData().getAsJsonObject().get("channel").getAsString();
            Set<WebSocket> channelClients = channels.get(channel);
            if (channelClients != null) {
                channelClients.remove(conn);
            }
            
            sendToClient(conn, createMessage("subscription", "removed", Map.of(
                "channel", channel,
                "subscribed", false
            )));
        } catch (Exception e) {
            sendError(conn, "UNSUBSCRIPTION_ERROR", "Fehler beim Abbestellen des Channels");
        }
    }
    
    private void handlePing(WebSocket conn, WebSocketMessage message) {
        sendToClient(conn, createMessage("pong", "response", Map.of(
            "timestamp", System.currentTimeMillis(),
            "requestId", message.getData().getAsJsonObject().get("requestId").getAsString()
        )));
    }
    
    private void handleServerCommand(WebSocket conn, WebSocketMessage message) {
        ClientSession session = clients.get(conn);
        if (session == null || !session.isAuthenticated()) {
            sendError(conn, "UNAUTHORIZED", "Nicht authentifiziert");
            return;
        }
        
        try {
            JsonObject data = message.getData().getAsJsonObject();
            String command = data.get("command").getAsString();
            
            // Command-Ausführung (hier würde die echte Implementierung stehen)
            boolean success = executeServerCommand(command);
            
            if (success) {
                sendToClient(conn, createMessage("command", "executed", Map.of(
                    "command", command,
                    "success", true,
                    "timestamp", System.currentTimeMillis()
                )));
            } else {
                sendError(conn, "COMMAND_FAILED", "Command konnte nicht ausgeführt werden");
            }
        } catch (Exception e) {
            sendError(conn, "COMMAND_ERROR", "Fehler bei der Command-Ausführung");
        }
    }
    
    private void handleRequest(WebSocket conn, WebSocketMessage message) {
        try {
            JsonObject data = message.getData().getAsJsonObject();
            String requestType = data.get("requestType").getAsString();
            String requestId = data.get("requestId").getAsString();
            
            Object responseData = processRequest(requestType, data);
            
            sendToClient(conn, createMessage("response", requestType, Map.of(
                "requestId", requestId,
                "data", responseData,
                "success", true
            )));
        } catch (Exception e) {
            sendError(conn, "REQUEST_ERROR", "Fehler bei der Request-Verarbeitung");
        }
    }
    
    private void sendInitialData(WebSocket conn) {
        // Server-Status
        sendToClient(conn, createMessage("server", "status", Map.of(
            "online", true,
            "version", "1.0.0",
            "players", getOnlinePlayerCount(),
            "uptime", getServerUptime()
        )));
    }
    
    private void sendError(WebSocket conn, String errorCode, String errorMessage) {
        sendToClient(conn, createMessage("error", errorCode, Map.of(
            "code", errorCode,
            "message", errorMessage,
            "timestamp", System.currentTimeMillis()
        )));
    }
    
    private WebSocketMessage createMessage(String type, String action, Object data) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType(type);
        message.setAction(action);
        message.setData(gson.toJsonTree(data));
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    private void startHeartbeatTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                
                // Heartbeat an alle Clients senden
                WebSocketMessage heartbeat = createMessage("heartbeat", "ping", Map.of(
                    "timestamp", currentTime
                ));
                broadcastToAll(heartbeat);
                
                // Timeout-Sessions entfernen
                clients.entrySet().removeIf(entry -> {
                    ClientSession session = entry.getValue();
                    if (currentTime - session.getLastActivity() > sessionTimeout) {
                        entry.getKey().close();
                        return true;
                    }
                    return false;
                });
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "Fehler im Heartbeat-Task", e);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }
    
    private void startMessageProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            while (!messageQueue.isEmpty()) {
                WebSocketMessage message = messageQueue.poll();
                if (message != null) {
                    broadcastToAll(message);
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }
    
    // Placeholder-Methoden für Server-Integration
    private boolean validateAuthToken(String token) {
        // Hier würde echte Token-Validierung stattfinden
        return "valid-token".equals(token);
    }
    
    private List<String> getUserPermissions(String userId) {
        // Hier würden echte Benutzerberechtigungen abgerufen
        return Arrays.asList("admin", "console", "players");
    }
    
    private boolean executeServerCommand(String command) {
        // Hier würde der echte Command ausgeführt
        logger.info("Executing command: " + command);
        return true;
    }
    
    private Object processRequest(String requestType, JsonObject data) {
        // Hier würden echte Requests verarbeitet
        return Map.of("processed", true, "type", requestType);
    }
    
    private double getCurrentCPUUsage() {
        return Math.random() * 100;
    }
    
    private Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        return Map.of(
            "used", runtime.totalMemory() - runtime.freeMemory(),
            "total", runtime.totalMemory(),
            "max", runtime.maxMemory()
        );
    }
    
    private double getCurrentTPS() {
        return 20.0; // Placeholder
    }
    
    private int getOnlinePlayerCount() {
        return (int) (Math.random() * 100);
    }
    
    private long getServerUptime() {
        return System.currentTimeMillis() - 1000000; // Placeholder
    }
    
    // Event-Handler Interface
    @FunctionalInterface
    private interface EventHandler {
        void handle(WebSocket conn, WebSocketMessage message);
    }
    
    // Client-Session Klasse
    private static class ClientSession {
        private final String id;
        private final WebSocket connection;
        private final long connectTime;
        private long lastActivity;
        private boolean authenticated;
        private String userId;
        
        public ClientSession(WebSocket connection) {
            this.id = UUID.randomUUID().toString();
            this.connection = connection;
            this.connectTime = System.currentTimeMillis();
            this.lastActivity = connectTime;
            this.authenticated = false;
        }
        
        // Getter und Setter
        public String getId() { return id; }
        public WebSocket getConnection() { return connection; }
        public long getConnectTime() { return connectTime; }
        public long getLastActivity() { return lastActivity; }
        public void updateLastActivity() { this.lastActivity = System.currentTimeMillis(); }
        public boolean isAuthenticated() { return authenticated; }
        public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
