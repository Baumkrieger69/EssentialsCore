package com.essentialscore.api.impl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A static data store for CoreModuleAPI to use when core methods are not available.
 * This provides a fallback implementation for data storage.
 */
class CoreModuleAPIDataStore {
    
    private static final Map<String, Object> SHARED_DATA = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> MODULE_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Object>> PLAYER_DATA = new ConcurrentHashMap<>();
    
    /**
     * Gets the shared data map
     * @return The shared data map
     */
    public static Map<String, Object> getSharedDataMap() {
        return SHARED_DATA;
    }
    
    /**
     * Gets the module data map for a specific module
     * @param moduleName The module name
     * @return The module data map
     */
    public static Map<String, Object> getModuleDataMap(String moduleName) {
        return MODULE_DATA.computeIfAbsent(moduleName, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Gets the player data map for a specific player
     * @param playerUUID The player UUID
     * @return The player data map
     */
    public static Map<String, Object> getPlayerDataMap(UUID playerUUID) {
        return PLAYER_DATA.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Clears all data
     */
    public static void clearAll() {
        SHARED_DATA.clear();
        MODULE_DATA.clear();
        PLAYER_DATA.clear();
    }
    
    // Private constructor to prevent instantiation
    private CoreModuleAPIDataStore() {}
}
