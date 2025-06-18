package com.essentialscore.api.performance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.util.function.Function;
import java.util.Optional;
import java.lang.ref.SoftReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Erweiterte Cache-Implementierung mit verschiedenen Strategien und Prefetching.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public class AdvancedCache<K, V> {
    private final Map<K, CacheEntry<V>> cache;
    private final CacheStrategy strategy;
    private final Duration defaultTtl;
    private final int maxSize;
    private final ReentrantReadWriteLock lock;
    private final Executor prefetchExecutor;
    private final Map<K, CompletableFuture<V>> loadingFutures;
    private Function<K, V> loader;
    
    public AdvancedCache(CacheStrategy strategy, Duration ttl, int maxSize) {
        this.cache = new ConcurrentHashMap<>();
        this.strategy = strategy;
        this.defaultTtl = ttl;
        this.maxSize = maxSize;
        this.lock = new ReentrantReadWriteLock();
        this.prefetchExecutor = Executors.newFixedThreadPool(2);
        this.loadingFutures = new ConcurrentHashMap<>();
    }
      /**
     * Setzt die Loader-Funktion für Cache-Misses.
     * 
     * @param loader the loader function to use for cache misses
     */
    public void setLoader(Function<K, V> loader) {
        this.loader = loader;
    }
      /**
     * Holt einen Wert aus dem Cache mit automatischem Loading.
     * 
     * @param key the key to retrieve
     * @return the cached value or empty if not found
     */
    public Optional<V> get(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            
            if (entry != null && !entry.isExpired()) {
                entry.incrementHits();
                return Optional.of(entry.getValue());
            }
        } finally {
            lock.readLock().unlock();
        }
        
        // Cache Miss - Load im Hintergrund
        if (loader != null) {
            return Optional.of(loadValue(key));
        }
        
        return Optional.empty();
    }
    
    /**
     * Lädt einen Wert asynchron.
     */
    private V loadValue(K key) {
        CompletableFuture<V> future = loadingFutures.computeIfAbsent(key,
            k -> CompletableFuture.supplyAsync(() -> {
                V value = loader.apply(k);
                put(k, value);
                return value;
            }, prefetchExecutor));
        
        try {
            return future.get(); // Blockiert bis der Wert geladen ist
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden des Wertes", e);
        } finally {
            loadingFutures.remove(key);
        }
    }
      /**
     * Fügt einen Wert in den Cache ein.
     * 
     * @param key the key to store
     * @param value the value to store
     */
    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }
      /**
     * Fügt einen Wert mit spezifischer TTL in den Cache ein.
     * 
     * @param key the key to store
     * @param value the value to store
     * @param ttl the time to live for this entry
     */
    public void put(K key, V value, Duration ttl) {
        lock.writeLock().lock();
        try {
            evictIfNeeded();
            cache.put(key, new CacheEntry<>(value, ttl));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Räumt den Cache basierend auf der gewählten Strategie auf.
     */
    private void evictIfNeeded() {
        if (cache.size() >= maxSize) {
            switch (strategy) {
                case LRU:
                    evictLRU();
                    break;
                case LFU:
                    evictLFU();
                    break;
                case FIFO:
                    evictFIFO();
                    break;
            }
        }
    }
    
    private void evictLRU() {
        K oldestKey = cache.entrySet().stream()
            .min((e1, e2) -> Long.compare(
                e1.getValue().getLastAccessTime(),
                e2.getValue().getLastAccessTime()))
            .map(Map.Entry::getKey)
            .orElse(null);
            
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }
    
    private void evictLFU() {
        K leastUsedKey = cache.entrySet().stream()
            .min((e1, e2) -> Long.compare(
                e1.getValue().getHits(),
                e2.getValue().getHits()))
            .map(Map.Entry::getKey)
            .orElse(null);
            
        if (leastUsedKey != null) {
            cache.remove(leastUsedKey);
        }
    }
    
    private void evictFIFO() {
        K firstKey = cache.entrySet().stream()
            .min((e1, e2) -> Long.compare(
                e1.getValue().getCreationTime(),
                e2.getValue().getCreationTime()))
            .map(Map.Entry::getKey)
            .orElse(null);
            
        if (firstKey != null) {
            cache.remove(firstKey);
        }
    }
    
    /**
     * Cache-Strategien.
     */
    public enum CacheStrategy {
        LRU,  // Least Recently Used
        LFU,  // Least Frequently Used
        FIFO  // First In First Out
    }
    
    /**
     * Cache-Eintrag mit Metadaten.
     */
    private static class CacheEntry<V> {
        private final SoftReference<V> value;
        private final long creationTime;
        private long lastAccessTime;
        private final long expirationTime;
        private long hits;
        
        public CacheEntry(V value, Duration ttl) {
            this.value = new SoftReference<>(value);
            this.creationTime = System.currentTimeMillis();
            this.lastAccessTime = creationTime;
            this.expirationTime = creationTime + ttl.toMillis();
            this.hits = 0;
        }
        
        public V getValue() {
            lastAccessTime = System.currentTimeMillis();
            return value.get();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
        
        public void incrementHits() {
            hits++;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public long getCreationTime() {
            return creationTime;
        }
        
        public long getHits() {
            return hits;
        }
    }
    
    /**
     * Bereinigt abgelaufene Einträge.
     */
    public void cleanup() {
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        } finally {
            lock.writeLock().unlock();
        }
    }
}
