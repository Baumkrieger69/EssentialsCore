package com.essentialscore.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.essentialscore.util.ObjectPool;

/**
 * Adaptiver Netzwerkmanager für optimierte Paketerstellung und -verarbeitung.
 * Passt Batchgrößen und Kompressionsstrategien dynamisch an die Netzwerklast an.
 */
public class AdaptiveNetworkManager {
    private static final Logger LOGGER = Logger.getLogger(AdaptiveNetworkManager.class.getName());
    
    // Konfigurationswerte
    private static final int MIN_BATCH_SIZE = 5;
    private static final int MAX_BATCH_SIZE = 200;
    private static final int BUFFER_POOL_SIZE = 64;
    private static final int DEFAULT_BUFFER_SIZE = 8192; // 8 KB
    
    // Statistik und Zustand
    private final AtomicInteger packetsProcessed = new AtomicInteger(0);
    private final AtomicInteger batchesProcessed = new AtomicInteger(0);
    private final AtomicInteger currentBatchSize = new AtomicInteger(MIN_BATCH_SIZE);
    private final AtomicInteger currentCompressionLevel = new AtomicInteger(6); // 0-9, 0=keine, 9=maximale Kompression
    private final AtomicInteger networkLoad = new AtomicInteger(0);
    private final AtomicInteger packetDropCount = new AtomicInteger(0);
    
    // Player-Session-Verwaltung
    private final Map<String, PlayerSession> playerSessions = new ConcurrentHashMap<>();
    
    // Warteschlangen für eingehende und ausgehende Pakete
    private final Queue<NetworkPacket> incomingQueue = new ConcurrentLinkedQueue<>();
    private final Queue<NetworkPacket> outgoingQueue = new ConcurrentLinkedQueue<>();
    
    // Callback-Handler
    private Consumer<NetworkPacket> packetProcessor;
    
    // Puffer-Pool für NetworkPackets
    private final ObjectPool<ByteBuffer> bufferPool = new ObjectPool<>(
            () -> ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE),
            ByteBuffer::clear,
            BUFFER_POOL_SIZE
    );
    
    // Scheduled Tasks
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> processingTask;
    private ScheduledFuture<?> tuningTask;
    
    /**
     * Erstellt einen neuen adaptiven Netzwerkmanager.
     * 
     * @param scheduler Der Scheduler für regelmäßige Aufgaben
     * @param packetProcessor Ein Callback für die Paketverarbeitung
     */
    public AdaptiveNetworkManager(ScheduledExecutorService scheduler, Consumer<NetworkPacket> packetProcessor) {
        this.scheduler = scheduler;
        this.packetProcessor = packetProcessor;
        
        // Puffer-Pool vorwärmen
        bufferPool.preload(BUFFER_POOL_SIZE / 2);
        
        // Verarbeitungs-Tasks starten
        startProcessingTasks();
    }
    
    /**
     * Startet die regelmäßigen Verarbeitungs- und Tuning-Tasks.
     */
    private void startProcessingTasks() {
        // Paketverarbeitungs-Task (häufig)
        processingTask = scheduler.scheduleWithFixedDelay(
                this::processBatches,
                0, 50, TimeUnit.MILLISECONDS);
        
        // Auto-Tuning-Task (seltener)
        tuningTask = scheduler.scheduleWithFixedDelay(
                this::tuneParameters,
                1000, 5000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Verarbeitet Pakete in Batches für bessere Effizienz.
     */
    private void processBatches() {
        try {
            processBatch(incomingQueue, "eingehend");
            processBatch(outgoingQueue, "ausgehend");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei der Batch-Verarbeitung", e);
        }
    }
    
    /**
     * Verarbeitet einen Batch von Paketen aus einer Warteschlange.
     */
    private void processBatch(Queue<NetworkPacket> queue, String direction) {
        int currentBatch = currentBatchSize.get();
        List<NetworkPacket> batch = new ArrayList<>(currentBatch);
        NetworkPacket packet;
        
        // Bis zu currentBatchSize Pakete aus der Warteschlange holen
        int count = 0;
        while ((packet = queue.poll()) != null && count < currentBatch) {
            batch.add(packet);
            count++;
        }
        
        if (!batch.isEmpty()) {
            // Batch verarbeiten
            for (NetworkPacket p : batch) {
                try {
                    if (packetProcessor != null) {
                        packetProcessor.accept(p);
                    }
                    
                    // Puffer zurück in den Pool geben
                    if (p.shouldRecycleBuffer()) {
                        bufferPool.release(p.getBuffer());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Fehler bei der Verarbeitung von " + direction + "em Paket", e);
                }
            }
            
            // Statistik aktualisieren
            packetsProcessed.addAndGet(batch.size());
            batchesProcessed.incrementAndGet();
            
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(direction + "e Pakete verarbeitet: " + batch.size());
            }
        }
    }
    
    /**
     * Passt Batch-Größe und Kompression basierend auf Netzwerklast und Verarbeitungsstatistik an.
     */
    private void tuneParameters() {
        try {
            int currentLoad = networkLoad.get();
            
            // Batch-Größe anpassen basierend auf Netzwerklast
            int optimalBatchSize;
            if (currentLoad > 80) {
                // Hohe Last: größere Batches für höheren Durchsatz
                optimalBatchSize = MAX_BATCH_SIZE;
            } else if (currentLoad > 50) {
                // Mittlere Last: moderate Batch-Größe
                optimalBatchSize = MIN_BATCH_SIZE + (MAX_BATCH_SIZE - MIN_BATCH_SIZE) / 2;
            } else {
                // Niedrige Last: kleinere Batches für geringere Latenz
                optimalBatchSize = MIN_BATCH_SIZE;
            }
            
            // Batch-Größe anpassen (nicht zu abrupt)
            int currentSize = currentBatchSize.get();
            int newSize = currentSize < optimalBatchSize ? 
                    Math.min(currentSize + 5, optimalBatchSize) : 
                    Math.max(currentSize - 5, optimalBatchSize);
            
            currentBatchSize.set(newSize);
            
            // Kompressionsebene anpassen basierend auf Last
            int compressionLevel;
            if (currentLoad > 70) {
                compressionLevel = 9; // Maximale Kompression bei hoher Last
            } else if (currentLoad > 40) {
                compressionLevel = 6; // Moderate Kompression
            } else if (currentLoad > 20) {
                compressionLevel = 3; // Leichte Kompression
            } else {
                compressionLevel = 0; // Keine Kompression bei niedriger Last
            }
            
            currentCompressionLevel.set(compressionLevel);
            
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format(
                        "Netzwerk-Tuning: Last=%d%%, BatchSize=%d, Kompression=%d, Pakete=%d, Batches=%d, Drops=%d",
                        currentLoad, newSize, compressionLevel, 
                        packetsProcessed.get(), batchesProcessed.get(), packetDropCount.get()));
            }
            
            // Statistik zurücksetzen für den nächsten Intervall
            packetsProcessed.set(0);
            batchesProcessed.set(0);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler im Netzwerk-Tuning", e);
        }
    }
    
    /**
     * Erstellt ein Paket und gibt es in die ausgehende Warteschlange.
     * 
     * @param sessionId ID der Spielersitzung
     * @param packetType Typ des Pakets
     * @param data Die Paketdaten
     */
    public void sendPacket(String sessionId, int packetType, byte[] data) {
        try {
            PlayerSession session = playerSessions.get(sessionId);
            if (session == null || !session.isActive()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Sitzung nicht aktiv: " + sessionId);
                }
                return;
            }
            
            // Daten-Limit prüfen, um DoS zu vermeiden
            if (data.length > DEFAULT_BUFFER_SIZE - 8) {
                LOGGER.warning("Paket zu groß: " + data.length + " bytes für Sitzung " + sessionId);
                packetDropCount.incrementAndGet();
                return;
            }
            
            // Komprimieren, wenn Kompression aktiviert ist und Größe sinnvoll
            byte[] packetData = data;
            boolean compressed = false;
            int compressionLevel = currentCompressionLevel.get();
            
            if (compressionLevel > 0 && data.length > 512) {
                try {
                    packetData = compressData(data, compressionLevel);
                    compressed = true;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Kompressionsfehler", e);
                    packetData = data;
                }
            }
            
            // ByteBuffer aus dem Pool holen und Paket erstellen
            ByteBuffer buffer = bufferPool.borrow();
            NetworkPacket packet = new NetworkPacket(sessionId, packetType, packetData, buffer, compressed);
            
            // In die Warteschlange stellen
            outgoingQueue.offer(packet);
            
            // Netzwerklast aktualisieren
            updateNetworkLoad(1);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Senden des Pakets", e);
            packetDropCount.incrementAndGet();
        }
    }
    
    /**
     * Empfängt ein eingehendes Paket und stellt es in die Warteschlange.
     * 
     * @param sessionId ID der Spielersitzung
     * @param packetType Typ des Pakets
     * @param data Die Paketdaten
     * @param compressed Gibt an, ob die Daten komprimiert sind
     */
    public void receivePacket(String sessionId, int packetType, byte[] data, boolean compressed) {
        try {
            PlayerSession session = playerSessions.get(sessionId);
            if (session == null) {
                session = new PlayerSession(sessionId);
                playerSessions.put(sessionId, session);
            }
            
            // Sitzungsaktivität aktualisieren
            session.updateLastActivity();
            
            // Dekomprimieren, falls nötig
            byte[] packetData = data;
            if (compressed) {
                try {
                    packetData = decompressData(data);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Dekompressionsfehler für Sitzung " + sessionId, e);
                    packetDropCount.incrementAndGet();
                    return;
                }
            }
            
            // ByteBuffer aus dem Pool holen und Paket erstellen
            ByteBuffer buffer = bufferPool.borrow();
            NetworkPacket packet = new NetworkPacket(sessionId, packetType, packetData, buffer, false);
            
            // In die Warteschlange stellen
            incomingQueue.offer(packet);
            
            // Netzwerklast aktualisieren
            updateNetworkLoad(1);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Empfangen des Pakets", e);
            packetDropCount.incrementAndGet();
        }
    }
    
    /**
     * Aktualisiert die Netzwerklast (0-100%).
     */
    private void updateNetworkLoad(int additionalPackets) {
        // Einfaches exponentielles gleitendes Mittel (EMA)
        double alpha = 0.3; // Gewichtungsfaktor
        
        int queueSize = incomingQueue.size() + outgoingQueue.size();
        int currentLoad = networkLoad.get();
        
        // Neue Last berechnen basierend auf Warteschlangengröße und aktueller Last
        int calculatedLoad = Math.min(100, queueSize * 100 / (MAX_BATCH_SIZE * 4));
        int newLoad = (int) (currentLoad * (1 - alpha) + calculatedLoad * alpha);
        
        networkLoad.set(newLoad);
    }
    
    /**
     * Komprimiert Daten mit dem konfigurierten Kompressionsgrad.
     */
    private byte[] compressData(byte[] data, int level) {
        // Hier würde eine echte Implementierung stehen
        // Zum Beispiel mit java.util.zip.Deflater
        return data; // Platzhalter
    }
    
    /**
     * Dekomprimiert Daten.
     */
    private byte[] decompressData(byte[] data) {
        // Hier würde eine echte Implementierung stehen
        // Zum Beispiel mit java.util.zip.Inflater
        return data; // Platzhalter
    }
    
    /**
     * Entfernt inaktive Spielersitzungen.
     */
    public void pruneInactiveSessions() {
        long now = System.currentTimeMillis();
        int inactiveThresholdMs = 5 * 60 * 1000; // 5 Minuten
        
        playerSessions.entrySet().removeIf(entry -> {
            PlayerSession session = entry.getValue();
            return now - session.getLastActivity() > inactiveThresholdMs;
        });
    }
    
    /**
     * Schließt den NetworkManager und gibt Ressourcen frei.
     */
    public void shutdown() {
        if (processingTask != null) {
            processingTask.cancel(false);
        }
        
        if (tuningTask != null) {
            tuningTask.cancel(false);
        }
        
        // Warteschlangen leeren
        incomingQueue.clear();
        outgoingQueue.clear();
        
        // Sessions schließen
        playerSessions.clear();
    }
    
    /**
     * Holt Statistiken des NetworkManagers.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("batchSize", currentBatchSize.get());
        stats.put("compressionLevel", currentCompressionLevel.get());
        stats.put("networkLoad", networkLoad.get());
        stats.put("packetDrops", packetDropCount.get());
        stats.put("activeSessions", playerSessions.size());
        stats.put("incomingQueueSize", incomingQueue.size());
        stats.put("outgoingQueueSize", outgoingQueue.size());
        return stats;
    }
    
    /**
     * Innere Klasse zur Verwaltung von Spielersitzungen.
     */
    private static class PlayerSession {
        private final String sessionId;
        private volatile long lastActivity;
        private volatile boolean active;
        
        public PlayerSession(String sessionId) {
            this.sessionId = sessionId;
            this.lastActivity = System.currentTimeMillis();
            this.active = true;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public long getLastActivity() {
            return lastActivity;
        }
        
        public boolean isActive() {
            return active;
        }
        
        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void setActive(boolean active) {
            this.active = active;
        }
    }
    
    /**
     * Netzwerkpaket mit optimierter Speichernutzung.
     */
    public static class NetworkPacket {
        private final String sessionId;
        private final int packetType;
        private final byte[] data;
        private final ByteBuffer buffer;
        private final boolean compressed;
        private final long timestamp;
        private boolean shouldRecycleBuffer = true;
        
        public NetworkPacket(String sessionId, int packetType, byte[] data, ByteBuffer buffer, boolean compressed) {
            this.sessionId = sessionId;
            this.packetType = packetType;
            this.data = data;
            this.buffer = buffer;
            this.compressed = compressed;
            this.timestamp = System.currentTimeMillis();
            
            // Daten in den Buffer schreiben
            if (buffer != null) {
                buffer.clear();
                buffer.putInt(packetType);
                buffer.putInt(data.length);
                buffer.put(data);
                buffer.flip();
            }
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public int getPacketType() {
            return packetType;
        }
        
        public byte[] getData() {
            return data;
        }
        
        public ByteBuffer getBuffer() {
            return buffer;
        }
        
        public boolean isCompressed() {
            return compressed;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean shouldRecycleBuffer() {
            return shouldRecycleBuffer;
        }
        
        public void setShouldRecycleBuffer(boolean shouldRecycle) {
            this.shouldRecycleBuffer = shouldRecycle;
        }
    }
} 