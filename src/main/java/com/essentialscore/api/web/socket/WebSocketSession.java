package com.essentialscore.api.web.socket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Optional;

/**
 * Represents a WebSocket session with enhanced security and monitoring capabilities.
 */
public class WebSocketSession {
    private final String sessionId;
    private final String resourceDescriptor;
    private boolean authenticated;
    private String username;
    private final long createdAt;
    private Consumer<String> messageSender;
    
    // Erweiterte Sicherheitsfelder
    private final SecretKey sessionKey;
    private String authToken;
    private Instant lastAuthRefresh;
    private final Map<String, String> securityHeaders;
    private boolean encrypted;
    
    // Performance-Monitoring
    private final AtomicLong messagesSent;
    private final AtomicLong messagesReceived;
    private final AtomicLong bytesTransferred;
    private Instant lastActivityTime;
    
    // Rate Limiting
    private final AtomicLong messageCounter;
    private Instant rateWindowStart;
    private final int maxMessagesPerWindow;
    private final Duration rateWindowDuration;
    
    // Session State
    private final Map<String, Object> attributes;
    private String clientVersion;
    private String clientPlatform;
    private String ipAddress;
    private SessionState state;
    
    /**
     * Repräsentiert den Zustand einer WebSocket-Session.
     */
    public enum SessionState {
        CONNECTING,
        CONNECTED,
        AUTHENTICATING,
        AUTHENTICATED,
        RATE_LIMITED,
        CLOSING,
        CLOSED
    }
    
    /**
     * Erstellt eine neue WebSocket-Session mit erweiterten Sicherheits- und Monitoring-Funktionen.
     * 
     * @param sessionId Die Session-ID
     * @param resourceDescriptor Der Resource Descriptor
     */
    public WebSocketSession(String sessionId, String resourceDescriptor) {
        this.sessionId = sessionId;
        this.resourceDescriptor = resourceDescriptor;
        this.authenticated = false;
        this.username = null;
        this.createdAt = System.currentTimeMillis();
        
        // Initialisiere Sicherheitsfelder
        this.sessionKey = generateSessionKey();
        this.securityHeaders = new ConcurrentHashMap<>();
        this.encrypted = false;
        
        // Initialisiere Monitoring
        this.messagesSent = new AtomicLong(0);
        this.messagesReceived = new AtomicLong(0);
        this.bytesTransferred = new AtomicLong(0);
        this.lastActivityTime = Instant.now();
        
        // Initialisiere Rate Limiting
        this.messageCounter = new AtomicLong(0);
        this.rateWindowStart = Instant.now();
        this.maxMessagesPerWindow = 1000; // Konfigurierbar
        this.rateWindowDuration = Duration.ofMinutes(1);
        
        // Initialisiere Session State
        this.attributes = new ConcurrentHashMap<>();
        this.state = SessionState.CONNECTING;
    }
    
    /**
     * Gets the session ID
     * 
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Gets the resource descriptor
     * 
     * @return The resource descriptor
     */
    public String getResourceDescriptor() {
        return resourceDescriptor;
    }
    
    /**
     * Checks if the session is authenticated
     * 
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Sets whether the session is authenticated
     * 
     * @param authenticated true if authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    /**
     * Gets the username
     * 
     * @return The username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the username
     * 
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the time when this session was created
     * 
     * @return The creation time
     */
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the message sender function for this session
     * 
     * @param messageSender A function that sends messages to the client
     */
    public void setMessageSender(Consumer<String> messageSender) {
        this.messageSender = messageSender;
    }
    
    /**
     * Sends a message to the client
     * 
     * @param message The message to send
     */
    public void sendMessage(String message) {
        if (messageSender != null) {
            messageSender.accept(message);
        }
    }
    
    /**
     * Checks if the session is open
     * 
     * @return true if the session is open
     */
    public boolean isOpen() {
        return messageSender != null;
    }

    /**
     * Generiert einen sicheren Session-Key.
     * 
     * @return Der generierte Session-Key
     */
    private SecretKey generateSessionKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Aktualisiert die Aktivitätszeit und überprüft Rate-Limiting.
     * 
     * @return true wenn die Nachricht innerhalb der Rate-Limits ist
     */
    public boolean checkAndUpdateRateLimit() {
        Instant now = Instant.now();
        long messageCount = messageCounter.incrementAndGet();
        
        if (now.isAfter(rateWindowStart.plus(rateWindowDuration))) {
            // Neues Zeitfenster starten
            messageCounter.set(1);
            rateWindowStart = now;
            return true;
        }
        
        if (messageCount > maxMessagesPerWindow) {
            state = SessionState.RATE_LIMITED;
            return false;
        }
        
        return true;
    }

    /**
     * Sendet eine verschlüsselte Nachricht an den Client.
     * 
     * @param message Die zu sendende Nachricht
     * @param encrypt Ob die Nachricht verschlüsselt werden soll
     */
    public void sendSecureMessage(String message, boolean encrypt) {
        if (messageSender != null) {
            String finalMessage = message;
            if (encrypt && encrypted) {
                // Implementiere hier die Verschlüsselung
                finalMessage = encryptMessage(message);
            }
            messageSender.accept(finalMessage);
            messagesSent.incrementAndGet();
            bytesTransferred.addAndGet(finalMessage.getBytes().length);
            lastActivityTime = Instant.now();
        }
    }

    /**
     * Verschlüsselt eine Nachricht mit dem Session-Key.
     * 
     * @param message Die zu verschlüsselnde Nachricht
     * @return Die verschlüsselte Nachricht
     */
    private String encryptMessage(String message) {
        try {
            // Implementiere hier die tatsächliche Verschlüsselung
            // Dies ist nur ein Platzhalter
            return Base64.getEncoder().encodeToString(message.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Verschlüsselungsfehler", e);
        }
    }

    /**
     * Fügt ein Sicherheits-Header hinzu.
     * 
     * @param name Header-Name
     * @param value Header-Wert
     */
    public void addSecurityHeader(String name, String value) {
        securityHeaders.put(name, value);
    }

    /**
     * Setzt die Client-Informationen.
     * 
     * @param version Client-Version
     * @param platform Client-Plattform
     * @param ip IP-Adresse
     */
    public void setClientInfo(String version, String platform, String ip) {
        this.clientVersion = version;
        this.clientPlatform = platform;
        this.ipAddress = ip;
    }

    /**
     * Speichert ein Session-Attribut.
     * 
     * @param key Attribut-Schlüssel
     * @param value Attribut-Wert
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Holt ein Session-Attribut.
     * 
     * @param key Attribut-Schlüssel
     * @return Optional mit dem Attribut-Wert
     */
    public Optional<Object> getAttribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    /**
     * Überprüft ob die Session abgelaufen ist.
     * 
     * @param timeout Timeout-Dauer
     * @return true wenn die Session abgelaufen ist
     */
    public boolean isExpired(Duration timeout) {
        return lastActivityTime.plus(timeout).isBefore(Instant.now());
    }

    /**
     * Aktualisiert den Auth-Token.
     * 
     * @param token Neuer Auth-Token
     */
    public void refreshAuthToken(String token) {
        this.authToken = token;
        this.lastAuthRefresh = Instant.now();
    }

    // Getter für neue Felder
    public long getMessagesSent() { return messagesSent.get(); }
    public long getMessagesReceived() { return messagesReceived.get(); }
    public long getBytesTransferred() { return bytesTransferred.get(); }
    public Instant getLastActivityTime() { return lastActivityTime; }
    public SessionState getState() { return state; }
    public String getClientVersion() { return clientVersion; }
    public String getClientPlatform() { return clientPlatform; }
    public String getIpAddress() { return ipAddress; }
    public Map<String, String> getSecurityHeaders() { return new ConcurrentHashMap<>(securityHeaders); }
    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
}