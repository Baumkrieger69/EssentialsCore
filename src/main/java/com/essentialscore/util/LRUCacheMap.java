package com.essentialscore.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A helper class for creating LRU caches with a configurable maximum size.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public class LRUCacheMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public LRUCacheMap(int initialCapacity, float loadFactor, int maxSize) {
        super(initialCapacity, loadFactor, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
