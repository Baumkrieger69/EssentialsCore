package com.essentialscore.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Eine Cache-Implementierung, die Off-Heap-Speicher nutzt, um große Datenmengen
 * außerhalb des Java-Heaps zu speichern.
 */
public class OffHeapCache implements AutoCloseable {
    private final ByteBuffer directBuffer;
    private final Map<String, CacheEntry> entries;
    private final AtomicLong offset;
    private final int capacity;
    private final Logger logger;
    
    /**
     * Erstellt einen neuen OffHeapCache mit der angegebenen Größe
     * 
     * @param capacityBytes Kapazität in Bytes
     */
    public OffHeapCache(int capacityBytes) {
        this(capacityBytes, Logger.getLogger(OffHeapCache.class.getName()));
    }
    
    /**
     * Erstellt einen neuen OffHeapCache mit der angegebenen Größe
     * 
     * @param capacityBytes Kapazität in Bytes
     * @param logger Logger für Diagnose
     */
    public OffHeapCache(int capacityBytes, Logger logger) {
        this.capacity = capacityBytes;
        this.directBuffer = ByteBuffer.allocateDirect(capacityBytes);
        this.entries = new ConcurrentHashMap<>();
        this.offset = new AtomicLong(0);
        this.logger = logger;
    }

    /**
     * Speichert ein Objekt im Cache
     * 
     * @param key Der Schlüssel
     * @param value Das zu speichernde Objekt
     * @return true, wenn der Eintrag gespeichert wurde, ansonsten false
     */
    public boolean put(String key, Serializable value) {
        try {
            byte[] bytes = serialize(value);
            
            // Prüfe, ob genug Platz vorhanden ist
            if (bytes.length > capacity) {
                logger.warning("Nicht genügend Off-Heap-Speicher für " + key + 
                        " (" + (bytes.length / 1024) + " KB)");
                return false;
            }
            
            synchronized (directBuffer) {
                // Prüfe, ob ein vorhandener Eintrag überschrieben werden kann
                CacheEntry existingEntry = entries.get(key);
                if (existingEntry != null) {
                    if (existingEntry.size >= bytes.length) {
                        // Vorhandenen Speicherbereich wiederverwenden
                        directBuffer.position((int) existingEntry.position);
                        directBuffer.put(bytes);
                        existingEntry.size = bytes.length;
                        return true;
                    } else {
                        // Alten Eintrag entfernen
                        entries.remove(key);
                    }
                }
                
                // Check, ob genug Platz im Buffer verfügbar ist
                long currentOffset = offset.get();
                if (currentOffset + bytes.length > capacity) {
                    // Nicht genug Platz - Garbage Collection durchführen
                    compactBuffer();
                    
                    // Erneut prüfen
                    currentOffset = offset.get();
                    if (currentOffset + bytes.length > capacity) {
                        return false;
                    }
                }
                
                // Schreibe Daten in den Buffer
                directBuffer.position((int) currentOffset);
                directBuffer.put(bytes);
                
                // Speichere Metadaten
                entries.put(key, new CacheEntry(currentOffset, bytes.length));
                
                // Aktualisiere Offset
                offset.addAndGet(bytes.length);
                
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fehler beim Cachen von " + key, e);
            return false;
        }
    }

    /**
     * Holt ein Objekt aus dem Cache
     * 
     * @param key Der Schlüssel
     * @return Das gespeicherte Objekt oder null, falls nicht vorhanden
     */
    public Object get(String key) {
        CacheEntry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        
        synchronized (directBuffer) {
            try {
                byte[] data = new byte[entry.size];
                directBuffer.position((int) entry.position);
                directBuffer.get(data);
                
                return deserialize(data);
            } catch (Exception e) {
                // Bei Fehler Eintrag entfernen
                entries.remove(key);
                return null;
            }
        }
    }

    /**
     * Entfernt einen Eintrag aus dem Cache
     * 
     * @param key Der zu entfernende Schlüssel
     * @return true, wenn der Eintrag entfernt wurde
     */
    public boolean remove(String key) {
        CacheEntry entry = entries.remove(key);
        return entry != null;
    }

    /**
     * Leert den Cache
     */
    public void clear() {
        synchronized (directBuffer) {
            entries.clear();
            directBuffer.clear();
            offset.set(0);
        }
    }

    /**
     * Gibt die Anzahl der Einträge im Cache zurück
     * 
     * @return Anzahl der Einträge
     */
    public int size() {
        return entries.size();
    }

    /**
     * Gibt den genutzten Speicherplatz in Bytes zurück
     * 
     * @return Genutzter Speicherplatz in Bytes
     */
    public long usedMemory() {
        return offset.get();
    }

    /**
     * Gibt den verfügbaren Speicherplatz in Bytes zurück
     * 
     * @return Verfügbarer Speicherplatz in Bytes
     */
    public long availableMemory() {
        return capacity - offset.get();
    }

    /**
     * Kompaktiert den Cache-Buffer, indem ungenutzter Speicherplatz freigegeben wird
     */
    private void compactBuffer() {
        synchronized (directBuffer) {
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(capacity);
            long newOffset = 0;
            
            // Übertrage alle aktiven Einträge in den neuen Buffer
            for (Map.Entry<String, CacheEntry> mapEntry : entries.entrySet()) {
                CacheEntry entry = mapEntry.getValue();
                
                byte[] data = new byte[entry.size];
                directBuffer.position((int) entry.position);
                directBuffer.get(data);
                
                newBuffer.position((int) newOffset);
                newBuffer.put(data);
                
                // Aktualisiere Position im Eintrag
                entry.position = newOffset;
                newOffset += entry.size;
            }
            
            // Ersetze alten Buffer durch neuen
            directBuffer.clear();
            System.arraycopy(newBuffer.array(), 0, directBuffer.array(), 0, (int) newOffset);
            
            // Setze Offset zurück
            offset.set(newOffset);
        }
    }

    /**
     * Serialisiert ein Objekt in ein Byte-Array
     * 
     * @param object Das zu serialisierende Objekt
     * @return Das serialisierte Byte-Array
     * @throws IOException Bei Serialisierungsfehlern
     */
    private byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * Deserialisiert ein Byte-Array zurück in ein Objekt
     * 
     * @param bytes Das zu deserialisierende Byte-Array
     * @return Das deserialisierte Objekt
     * @throws IOException Bei Deserialisierungsfehlern
     * @throws ClassNotFoundException Wenn die Klasse nicht gefunden wurde
     */
    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    /**
     * Repräsentiert einen Eintrag im Cache mit Position und Größe
     */
    private static class CacheEntry {
        private long position;
        private int size;

        /**
         * Erstellt einen neuen Cache-Eintrag
         * 
         * @param position Position im Buffer
         * @param size Größe in Bytes
         */
        CacheEntry(long position, int size) {
            this.position = position;
            this.size = size;
        }
    }

    /**
     * Schließt den Cache und gibt allen Speicher frei.
     */
    @Override
    public void close() {
        clear();
    }
} 
