package com.essentialscore.webui;

import com.essentialscore.webui.api.EnhancedAPIEndpoints;
import com.essentialscore.webui.websocket.WebSocketHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Haupt-Controller für das EssentialsCore WebUI System
 * Verwaltet HTTP-Server, WebSocket-Server und API-Integration
 */
public class WebUIController {
    
    private static final Logger logger = Logger.getLogger(WebUIController.class.getName());
    
    // Server-Konfiguration
    private static final int HTTP_PORT = 8080;
    private static final int WEBSOCKET_PORT = 8081;
    private static final String WEBUI_PATH = "/workspaces/EssentialsCore/src/main/resources/webui/webapp";
    
    // Server-Instanzen
    private HttpServer httpServer;
    private WebSocketHandler webSocketServer;
    private EnhancedAPIEndpoints apiEndpoints;
    
    // Status
    private boolean isRunning = false;
    private long startTime;
    
    /**
     * Startet das komplette WebUI-System
     */
    public void start() {
        try {
            logger.info("Starte EssentialsCore WebUI System...");
            startTime = System.currentTimeMillis();
            
            // HTTP-Server starten
            startHttpServer();
            
            // WebSocket-Server starten
            startWebSocketServer();
            
            // API-Endpoints initialisieren
            initializeApiEndpoints();
            
            // System-Monitoring starten
            startSystemMonitoring();
            
            isRunning = true;
            logger.info("EssentialsCore WebUI System erfolgreich gestartet!");
            logger.info("HTTP-Server: http://localhost:" + HTTP_PORT);
            logger.info("WebSocket-Server: ws://localhost:" + WEBSOCKET_PORT);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fehler beim Starten des WebUI-Systems", e);
            stop();
        }
    }
    
    /**
     * Stoppt das WebUI-System
     */
    public void stop() {
        try {
            logger.info("Stoppe EssentialsCore WebUI System...");
            
            if (webSocketServer != null) {
                try {
                    webSocketServer.stop();
                    logger.info("WebSocket-Server gestoppt");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Fehler beim Stoppen des WebSocket-Servers", e);
                }
            }
            
            if (httpServer != null) {
                httpServer.stop(0);
                logger.info("HTTP-Server gestoppt");
            }
            
            isRunning = false;
            logger.info("EssentialsCore WebUI System gestoppt");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fehler beim Stoppen des WebUI-Systems", e);
        }
    }
    
    /**
     * Neustart des Systems
     */
    public void restart() {
        logger.info("Starte EssentialsCore WebUI System neu...");
        stop();
        try {
            Thread.sleep(2000); // Kurze Pause für sauberen Neustart
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        start();
    }
    
    /**
     * HTTP-Server starten
     */
    private void startHttpServer() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        
        // Statische Dateien
        httpServer.createContext("/", new StaticFileHandler(WEBUI_PATH));
        
        // Thread-Pool für HTTP-Server
        httpServer.setExecutor(Executors.newFixedThreadPool(10));
        httpServer.start();
        
        logger.info("HTTP-Server gestartet auf Port " + HTTP_PORT);
    }
    
    /**
     * WebSocket-Server starten
     */
    private void startWebSocketServer() throws Exception {
        webSocketServer = new WebSocketHandler(WEBSOCKET_PORT);
        webSocketServer.start();
        
        logger.info("WebSocket-Server gestartet auf Port " + WEBSOCKET_PORT);
    }
    
    /**
     * API-Endpoints initialisieren
     */
    private void initializeApiEndpoints() {
        try {
            apiEndpoints = new EnhancedAPIEndpoints();
            
            // API-Routen zu HTTP-Server hinzufügen
            httpServer.createContext("/api/performance", apiEndpoints::handlePerformanceRequest);
            httpServer.createContext("/api/players", apiEndpoints::handlePlayersRequest);
            httpServer.createContext("/api/server", apiEndpoints::handleServerRequest);
            httpServer.createContext("/api/plugins", apiEndpoints::handlePluginsRequest);
            httpServer.createContext("/api/backup", apiEndpoints::handleBackupRequest);
            
            logger.info("API-Endpoints initialisiert");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fehler beim Initialisieren der API-Endpoints", e);
        }
    }
    
    /**
     * System-Monitoring starten
     */
    private void startSystemMonitoring() {
        // Performance-Updates alle 5 Sekunden
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            if (webSocketServer != null && isRunning) {
                webSocketServer.sendPerformanceUpdate();
            }
        }, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
        
        logger.info("System-Monitoring gestartet");
    }
    
    // Event-Handler für Server-Events
    
    /**
     * Player-Join Event
     */
    public void onPlayerJoin(String playerName, String uuid) {
        if (webSocketServer != null) {
            java.util.Map<String, Object> playerData = java.util.Map.of(
                "name", playerName,
                "uuid", uuid,
                "timestamp", System.currentTimeMillis()
            );
            webSocketServer.sendPlayerUpdate("join", playerData);
        }
    }
    
    /**
     * Player-Leave Event
     */
    public void onPlayerLeave(String playerName, String uuid) {
        if (webSocketServer != null) {
            java.util.Map<String, Object> playerData = java.util.Map.of(
                "name", playerName,
                "uuid", uuid,
                "timestamp", System.currentTimeMillis()
            );
            webSocketServer.sendPlayerUpdate("leave", playerData);
        }
    }
    
    /**
     * Console-Output Event
     */
    public void onConsoleOutput(String line, String level) {
        if (webSocketServer != null) {
            webSocketServer.sendConsoleOutput(line, level);
        }
    }
    
    /**
     * Server-Alert Event
     */
    public void sendAlert(String type, String title, String message, String severity) {
        if (webSocketServer != null) {
            webSocketServer.sendAlert(type, title, message, severity);
        }
    }
    
    // Getter für Status-Informationen
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public long getUptime() {
        return isRunning ? System.currentTimeMillis() - startTime : 0;
    }
    
    public int getHttpPort() {
        return HTTP_PORT;
    }
    
    public int getWebSocketPort() {
        return WEBSOCKET_PORT;
    }
    
    public String getWebUIPath() {
        return WEBUI_PATH;
    }
    
    /**
     * WebSocket-Server-Referenz für direkte Nutzung
     */
    public WebSocketHandler getWebSocketServer() {
        return webSocketServer;
    }
    
    /**
     * API-Endpoints-Referenz für direkte Nutzung
     */
    public EnhancedAPIEndpoints getApiEndpoints() {
        return apiEndpoints;
    }
}
