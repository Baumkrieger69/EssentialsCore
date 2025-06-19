package com.essentialscore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Moderner Permission Manager für EssentialsCore
 * Unterstützt LuckPerms und fallback zu Bukkit Permissions mit Caching
 */
public class PermissionManager implements com.essentialscore.api.permission.PermissionManager {
    private final ApiCore apiCore;
    private boolean permissionsHooked = false;
    private Object permissionsProvider = null;
    private ConsoleFormatter console;
    
    // Permission Cache für bessere Performance
    private final ConcurrentHashMap<String, Boolean> permissionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Map<String, Boolean>> playerPermissionCache = new ConcurrentHashMap<>();
    
    // Modul-Permissions Storage
    private final ConcurrentHashMap<String, List<Permission>> modulePermissions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Permission> registeredPermissions = new ConcurrentHashMap<>();
    
    // Cache-Konfiguration
    private long cacheExpireTime = 30000; // 30 Sekunden
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    public PermissionManager(ApiCore apiCore) {
        this.apiCore = apiCore;
        
        // Konsolen-Formatter initialisieren
        String rawPrefix = apiCore.getConfig().getString("console.prefixes.permission-manager", "&8[&5&lPermissionManager&8]");
        boolean useColors = apiCore.getConfig().getBoolean("console.use-colors", true);
        boolean showTimestamps = apiCore.getConfig().getBoolean("console.show-timestamps", false);
        boolean useUnicodeSymbols = apiCore.getConfig().getBoolean("console.use-unicode-symbols", true);
        String stylePreset = apiCore.getConfig().getString("console.style-preset", "default");
        
        console = new ConsoleFormatter(
            apiCore.getLogger(),
            rawPrefix,
            useColors, showTimestamps, useUnicodeSymbols, stylePreset
        );
        
        // Cache-Konfiguration laden
        this.cacheExpireTime = apiCore.getConfig().getLong("permission.cache-expire-time", 30000);
        
        // Permission Hook versuchen
        hookIntoPermissions();
    }
    
    /**
     * Verbindet sich mit dem Permissions-Plugin (LuckPerms)
     */
    public boolean hookIntoPermissions() {
        console.info("Suche nach Permissions-Plugin...");
        
        if (Bukkit.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                console.info("LuckPerms gefunden, stelle Verbindung her...");
                
                // Verwende moderne LuckPerms API
                Class<?> luckPermsApiClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getApiMethod = luckPermsApiClass.getMethod("get");
                
                permissionsProvider = getApiMethod.invoke(null);
                permissionsHooked = true;

                console.success("LuckPerms erfolgreich verbunden!");
                
                if (apiCore.isDebugMode()) {
                    console.debug("LuckPerms API-Klasse: " + permissionsProvider.getClass().getName(), true);
                }
                
                return true;
            } catch (Throwable e) {
                console.warning("LuckPerms-Verbindung fehlgeschlagen: " + e.getMessage());
                console.info("Verwende Bukkit-Permissions als Fallback");
                permissionsHooked = false;
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
                return false;
            }
        } else {
            console.info("LuckPerms nicht gefunden - verwende Bukkit-Permissions");
            permissionsHooked = false;
            return false;
        }
    }
    
    /**
     * Registriert Permissions im System
     */
    public void registerPermissions(Map<String, String[]> permissions) {
        if (permissions.isEmpty()) return;
        
        console.info("Registriere " + permissions.size() + " Berechtigungen...");
        
        PluginManager pluginManager = Bukkit.getPluginManager();
        int successful = 0;
        
        for (Map.Entry<String, String[]> entry : permissions.entrySet()) {
            String permission = entry.getKey();
            String description = entry.getValue().length > 0 ? entry.getValue()[0] : "No description";
            PermissionDefault defaultValue = entry.getValue().length > 1 ? 
                PermissionDefault.valueOf(entry.getValue()[1].toUpperCase()) : PermissionDefault.OP;
            
            try {
                Permission bukkitPerm = new Permission(permission, description, defaultValue);
                pluginManager.addPermission(bukkitPerm);
                successful++;
                
                if (apiCore.isDebugMode()) {
                    console.debug("Berechtigung registriert: " + permission, true);
                }
            } catch (IllegalArgumentException e) {
                // Berechtigung existiert bereits
                if (apiCore.isDebugMode()) {
                    console.debug("Berechtigung existiert bereits: " + permission, true);
                }
                successful++;
            } catch (Exception e) {
                console.warning("Fehler bei Berechtigung " + permission + ": " + e.getMessage());
            }
        }
        
        console.success(successful + " von " + permissions.size() + " Berechtigungen erfolgreich registriert");
    }
    
    /**
     * Überprüft eine Berechtigung mit Caching
     */
    @Override
    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null || permission == null) return false;
        
        // Console hat immer alle Berechtigungen
        if (!(sender instanceof Player)) {
            return true;
        }
        
        Player player = (Player) sender;
        String cacheKey = player.getUniqueId().toString() + ":" + permission;
        
        // Cache prüfen
        if (isCacheValid(cacheKey)) {
            Boolean cachedResult = permissionCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }
        
        // Berechtigung prüfen
        boolean hasPermission = checkPermissionDirect(player, permission);
        
        // Im Cache speichern
        cachePermission(cacheKey, hasPermission);
        
        return hasPermission;
    }
    
    /**
     * Direkte Berechtigungsprüfung ohne Cache
     */
    private boolean checkPermissionDirect(Player player, String permission) {
        try {
            if (permissionsHooked && permissionsProvider != null) {
                // LuckPerms verwenden
                return checkLuckPermsPermission(player, permission);
            } else {
                // Bukkit Permissions verwenden
                return player.hasPermission(permission);
            }
        } catch (Exception e) {
            console.warning("Fehler bei Berechtigungsprüfung für " + player.getName() + ": " + e.getMessage());
            // Fallback zu Bukkit
            return player.hasPermission(permission);
        }
    }
    
    /**
     * LuckPerms Berechtigungsprüfung mit Reflection
     */
    private boolean checkLuckPermsPermission(Player player, String permission) {
        try {
            // UserManager holen
            Method getUserManagerMethod = permissionsProvider.getClass().getMethod("getUserManager");
            Object userManager = getUserManagerMethod.invoke(permissionsProvider);
            
            // User laden
            Method loadUserMethod = userManager.getClass().getMethod("loadUser", UUID.class);
            CompletableFuture<?> userFuture = (CompletableFuture<?>) loadUserMethod.invoke(userManager, player.getUniqueId());
            Object user = userFuture.get();
            
            if (user != null) {
                // Berechtigung prüfen
                Method getCachedDataMethod = user.getClass().getMethod("getCachedData");
                Object cachedData = getCachedDataMethod.invoke(user);
                
                Method getPermissionDataMethod = cachedData.getClass().getMethod("getPermissionData");
                Object permissionData = getPermissionDataMethod.invoke(cachedData);
                
                Method checkPermissionMethod = permissionData.getClass().getMethod("checkPermission", String.class);
                Object result = checkPermissionMethod.invoke(permissionData, permission);
                
                // TriState zu boolean konvertieren
                String triStateValue = result.toString();
                return "TRUE".equals(triStateValue);
            }
        } catch (Exception e) {
            console.debug("LuckPerms Berechtigungsprüfung fehlgeschlagen: " + e.getMessage(), apiCore.isDebugMode());
        }
        
        // Fallback zu Bukkit
        return player.hasPermission(permission);
    }
    
    /**
     * Überprüft mehrere Berechtigungen gleichzeitig
     */
    @Override
    public boolean hasAnyPermission(CommandSender sender, String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(sender, permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Überprüft ob alle Berechtigungen vorhanden sind
     */
    @Override
    public boolean hasAllPermissions(CommandSender sender, String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(sender, permission)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Cache-Validierung
     */
    private boolean isCacheValid(String cacheKey) {
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp == null) return false;
        
        return (System.currentTimeMillis() - timestamp) < cacheExpireTime;
    }
    
    /**
     * Berechtigung im Cache speichern
     */
    private void cachePermission(String cacheKey, boolean hasPermission) {
        permissionCache.put(cacheKey, hasPermission);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }
    
    /**
     * Cache für einen Spieler leeren
     */
    public void clearPlayerCache(Player player) {
        String playerPrefix = player.getUniqueId().toString() + ":";
        permissionCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
        cacheTimestamps.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
        
        console.debug("Permission-Cache für " + player.getName() + " geleert", apiCore.isDebugMode());
    }
    
    /**
     * Gesamten Cache leeren
     */
    public void clearCache() {
        permissionCache.clear();
        cacheTimestamps.clear();
        playerPermissionCache.clear();
        
        console.info("Permission-Cache komplett geleert");
    }
    
    /**
     * Cache-Statistiken
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached-permissions", permissionCache.size());
        stats.put("cache-expire-time", cacheExpireTime);
        stats.put("luckperms-hooked", permissionsHooked);
        
        // Veraltete Cache-Einträge zählen
        long now = System.currentTimeMillis();
        long expiredEntries = cacheTimestamps.values().stream()
            .mapToLong(timestamp -> (now - timestamp) > cacheExpireTime ? 1 : 0)
            .sum();
        stats.put("expired-entries", expiredEntries);
        
        return stats;
    }
    
    /**
     * Automatische Cache-Bereinigung (sollte regelmäßig aufgerufen werden)
     */
    public void cleanupCache() {
        long now = System.currentTimeMillis();
        int removedEntries = 0;
        
        // Veraltete Einträge entfernen
        cacheTimestamps.entrySet().removeIf(entry -> {
            if ((now - entry.getValue()) > cacheExpireTime) {
                permissionCache.remove(entry.getKey());
                return true;
            }
            return false;
        });
        
        console.debug("Cache-Bereinigung: " + removedEntries + " veraltete Einträge entfernt", apiCore.isDebugMode());
    }
    
    /**
     * Gibt Spieler-Gruppen zurück (LuckPerms)
     */
    public List<String> getPlayerGroups(Player player) {
        List<String> groups = new ArrayList<>();
        
        if (!permissionsHooked || permissionsProvider == null) {
            return groups; // Bukkit hat keine Gruppen-API
        }
        
        try {
            // UserManager holen
            Method getUserManagerMethod = permissionsProvider.getClass().getMethod("getUserManager");
            Object userManager = getUserManagerMethod.invoke(permissionsProvider);
            
            // User laden
            Method loadUserMethod = userManager.getClass().getMethod("loadUser", UUID.class);
            CompletableFuture<?> userFuture = (CompletableFuture<?>) loadUserMethod.invoke(userManager, player.getUniqueId());
            Object user = userFuture.get();
            
            if (user != null) {
                // Gruppen holen
                Method getInheritedGroupsMethod = user.getClass().getMethod("getInheritedGroups", 
                    Class.forName("net.luckperms.api.query.QueryOptions"));
                
                // QueryOptions erstellen
                Class<?> queryOptionsClass = Class.forName("net.luckperms.api.query.QueryOptions");
                Method defaultQueryOptionsMethod = queryOptionsClass.getMethod("defaultContextualOptions");
                Object queryOptions = defaultQueryOptionsMethod.invoke(null);
                
                @SuppressWarnings("unchecked")
                List<Object> groupList = (List<Object>) getInheritedGroupsMethod.invoke(user, queryOptions);
                
                for (Object group : groupList) {
                    Method getNameMethod = group.getClass().getMethod("getName");
                    String groupName = (String) getNameMethod.invoke(group);
                    groups.add(groupName);
                }
            }
        } catch (Exception e) {
            console.debug("Fehler beim Laden der Spieler-Gruppen: " + e.getMessage(), apiCore.isDebugMode());
        }
        
        return groups;
    }
    
    /**
     * Prüft ob Spieler in einer bestimmten Gruppe ist
     */
    public boolean isInGroup(Player player, String groupName) {
        return getPlayerGroups(player).contains(groupName.toLowerCase());
    }
    
    /**
     * Reload der Permission-Konfiguration
     */
    public void reload() {
        clearCache();
        this.cacheExpireTime = apiCore.getConfig().getLong("permission.cache-expire-time", 30000);
        
        // Permission-Hook erneut versuchen falls beim ersten Mal fehlgeschlagen
        if (!permissionsHooked) {
            hookIntoPermissions();
        }
        
        console.info("PermissionManager neu geladen");
    }
    
    /**
     * Debug-Informationen ausgeben
     */
    public void printDebugInfo(CommandSender sender) {
        Map<String, Object> stats = getCacheStats();
        
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§6§lPermission Manager Debug Info");
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§7LuckPerms verbunden: " + (permissionsHooked ? "§aJa" : "§cNein"));
        sender.sendMessage("§7Gecachte Berechtigungen: §b" + stats.get("cached-permissions"));
        sender.sendMessage("§7Cache-Ablaufzeit: §b" + stats.get("cache-expire-time") + "ms");
        sender.sendMessage("§7Veraltete Einträge: §e" + stats.get("expired-entries"));
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            List<String> groups = getPlayerGroups(player);
            sender.sendMessage("§7Deine Gruppen: §a" + String.join("§7, §a", groups));
        }
        
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    // API Interface Implementation
    
    /**
     * Registriert eine einzelne Berechtigung
     */
    @Override
    public boolean registerPermission(Permission permission) {
        try {
            Bukkit.getPluginManager().addPermission(permission);
            registeredPermissions.put(permission.getName(), permission);
            return true;
        } catch (IllegalArgumentException e) {
            // Permission already exists
            return false;
        } catch (Exception e) {
            console.warning("Fehler beim Registrieren der Berechtigung " + permission.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Registriert eine Modul-Berechtigung
     */
    @Override
    public boolean registerModulePermission(String moduleId, Permission permission) {
        boolean success = registerPermission(permission);
        if (success) {
            modulePermissions.computeIfAbsent(moduleId, k -> new ArrayList<>()).add(permission);
        }
        return success;
    }
    
    /**
     * Registriert eine Modul-Berechtigung über Parameter
     */
    @Override
    public boolean registerModulePermission(String moduleId, String name, String description, PermissionDefault defaultValue) {
        Permission permission = new Permission(name, description, defaultValue);
        return registerModulePermission(moduleId, permission);
    }
    
    /**
     * Deregistriert eine Berechtigung
     */
    @Override
    public boolean unregisterPermission(String permissionName) {
        try {
            Bukkit.getPluginManager().removePermission(permissionName);
            registeredPermissions.remove(permissionName);
            
            // Aus Modul-Permissions entfernen
            modulePermissions.values().forEach(permissions -> 
                permissions.removeIf(perm -> perm.getName().equals(permissionName))
            );
            
            return true;
        } catch (Exception e) {
            console.warning("Fehler beim Deregistrieren der Berechtigung " + permissionName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Deregistriert alle Berechtigungen eines Moduls
     */
    @Override
    public int unregisterModulePermissions(String moduleId) {
        List<Permission> permissions = modulePermissions.get(moduleId);
        if (permissions == null) return 0;
        
        int count = 0;
        for (Permission permission : new ArrayList<>(permissions)) {
            if (unregisterPermission(permission.getName())) {
                count++;
            }
        }
        
        modulePermissions.remove(moduleId);
        return count;
    }
    
    /**
     * Holt alle Berechtigungen eines Moduls
     */
    @Override
    public List<Permission> getModulePermissions(String moduleId) {
        return new ArrayList<>(modulePermissions.getOrDefault(moduleId, new ArrayList<>()));
    }
    
    /**
     * Holt alle registrierten Berechtigungen
     */
    @Override
    public List<Permission> getAllPermissions() {
        return new ArrayList<>(registeredPermissions.values());
    }
    
    /**
     * Holt alle Modul-Berechtigungen
     */
    @Override
    public Map<String, List<Permission>> getAllModulePermissions() {
        Map<String, List<Permission>> result = new HashMap<>();
        modulePermissions.forEach((moduleId, permissions) -> 
            result.put(moduleId, new ArrayList<>(permissions))
        );
        return result;
    }
    
    /**
     * Überprüft Spieler-Berechtigung (Player statt CommandSender)
     */
    @Override
    public boolean hasPermission(Player player, String permission) {
        return hasPermission((CommandSender) player, permission);
    }
    
    /**
     * Überprüft Modul-spezifische Berechtigung
     */
    @Override
    public boolean hasModulePermission(Player player, String moduleId, String permission) {
        String fullPermission = moduleId + "." + permission;
        return hasPermission(player, fullPermission);
    }
    
    /**
     * Fügt einem Spieler eine Berechtigung hinzu (temporär)
     */
    @Override
    public boolean addPermission(Player player, String permission) {
        try {
            // Temporäre Berechtigung über Bukkit's Attachment System
            player.addAttachment(apiCore, permission, true);
            clearPlayerCache(player); // Cache invalidieren
            return true;
        } catch (Exception e) {
            console.warning("Fehler beim Hinzufügen der Berechtigung " + permission + " für " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Entfernt eine Berechtigung von einem Spieler
     */
    @Override
    public boolean removePermission(Player player, String permission) {
        try {
            // Alle Attachments durchgehen und die Berechtigung entfernen
            player.getEffectivePermissions().stream()
                .filter(permInfo -> permInfo.getPermission().equals(permission))
                .forEach(permInfo -> {
                    if (permInfo.getAttachment() != null) {
                        permInfo.getAttachment().remove();
                    }
                });
            
            clearPlayerCache(player); // Cache invalidieren
            return true;
        } catch (Exception e) {
            console.warning("Fehler beim Entfernen der Berechtigung " + permission + " von " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Holt alle Berechtigungen eines Spielers
     */
    @Override
    public List<String> getPlayerPermissions(Player player) {
        List<String> permissions = new ArrayList<>();
        
        try {
            player.getEffectivePermissions().forEach(permInfo -> {
                if (permInfo.getValue()) { // Nur positive Berechtigungen
                    permissions.add(permInfo.getPermission());
                }
            });
        } catch (Exception e) {
            console.warning("Fehler beim Abrufen der Berechtigungen für " + player.getName() + ": " + e.getMessage());
        }
        
        return permissions;
    }
    
    /**
     * Überprüft ob eine Berechtigung registriert ist
     */
    @Override
    public boolean isPermissionRegistered(String permissionName) {
        return registeredPermissions.containsKey(permissionName) || 
               Bukkit.getPluginManager().getPermission(permissionName) != null;
    }
    
    /**
     * Holt eine Berechtigung nach Namen
     */
    @Override
    public Permission getPermission(String permissionName) {
        Permission permission = registeredPermissions.get(permissionName);
        if (permission != null) return permission;
        
        return Bukkit.getPluginManager().getPermission(permissionName);
    }}
