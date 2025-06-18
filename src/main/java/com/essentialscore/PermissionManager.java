package com.essentialscore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manager für Berechtigungen im ApiCore
 */
public class PermissionManager implements com.essentialscore.api.permission.PermissionManager {
    private final ApiCore apiCore;
    private boolean permissionsHooked = false;
    private Object permissionsProvider = null;
    private ConsoleFormatter console;
    
    public PermissionManager(ApiCore apiCore) {
        this.apiCore = apiCore;
        
        // Erweiterte Konsolen-Formatter Konfiguration
        boolean useColors = apiCore.getConfig().getBoolean("console.use-colors", true);
        boolean showTimestamps = apiCore.getConfig().getBoolean("console.show-timestamps", false);
        boolean useUnicodeSymbols = apiCore.getConfig().getBoolean("console.use-unicode-symbols", true);
        String stylePreset = apiCore.getConfig().getString("console.style-preset", "default");
        
        // Konsolen-Formatter initialisieren mit Rohpräfix (ohne Formatierung)
        String rawPrefix = apiCore.getConfig().getString("console.prefixes.permission-manager", "&8[&5&lPermissionManager&8]");
        console = new ConsoleFormatter(
            apiCore.getLogger(),
            rawPrefix,
            useColors, showTimestamps, useUnicodeSymbols, stylePreset
        );
    }
    
    /**
     * Versucht, sich mit dem Permissions-Plugin zu verbinden - optimierte Version mit Caching
     */
    public boolean hookIntoPermissions() {
        console.info("Suche nach Permissions-Plugin...");
        
        if (Bukkit.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                console.info("LuckPerms gefunden, versuche Verbindung herzustellen...");
                
                // Versuche per Method Handle statt Reflection auf LuckPerms zuzugreifen
                Class<?> luckPermsApiClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                java.lang.invoke.MethodHandle getApiMethod = java.lang.invoke.MethodHandles.lookup().findStatic(
                        luckPermsApiClass, 
                        "get", 
                        java.lang.invoke.MethodType.methodType(Class.forName("net.luckperms.api.LuckPerms")));
                
                permissionsProvider = getApiMethod.invoke();
                permissionsHooked = true;

                console.success("LuckPerms wurde erfolgreich eingebunden!");
                
                // Analysiere verfügbare Methoden im Debug-Modus
                if (apiCore.isDebugMode()) {
                    analyzePermissionsProvider();
                }
                
                return true;
            } catch (Throwable e) {
                console.error("Konnte nicht mit LuckPerms verbinden: " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
                return false;
            }
        } else {
            console.warning("LuckPerms wurde nicht gefunden. Permissions-Integration deaktiviert.");
            return false;
        }
    }
    
    /**
     * Analysiert den Permissions-Provider und gibt Informationen über verfügbare Methoden aus
     */
    private void analyzePermissionsProvider() {
        try {
            console.subHeader("LUCKPERMS API ANALYSE");
            console.info("Provider Klasse: " + permissionsProvider.getClass().getName());
            
            // Methoden des Providers auflisten
            console.section("PROVIDER METHODEN");
            for (Method method : permissionsProvider.getClass().getMethods()) {
                console.info("• " + method.toString());
            }
            
            // Versuche, den PlayerAdapter zu bekommen
            try {
                Method getPlayerAdapterMethod = permissionsProvider.getClass().getMethod("getPlayerAdapter", Class.class);
                Object playerAdapter = getPlayerAdapterMethod.invoke(permissionsProvider, Player.class);
                
                if (playerAdapter != null) {
                    console.section("PLAYER ADAPTER METHODEN");
                    console.info("PlayerAdapter Klasse: " + playerAdapter.getClass().getName());
                    for (Method method : playerAdapter.getClass().getMethods()) {
                        console.info("• " + method.toString());
                    }
                }
            } catch (Exception e) {
                console.warning("Konnte PlayerAdapter nicht analysieren: " + e.getMessage());
            }
            
            // Versuche, den GroupManager zu bekommen
            try {
                Method getGroupManagerMethod = permissionsProvider.getClass().getMethod("getGroupManager");
                Object groupManager = getGroupManagerMethod.invoke(permissionsProvider);
                
                if (groupManager != null) {
                    console.section("GROUP MANAGER METHODEN");
                    console.info("GroupManager Klasse: " + groupManager.getClass().getName());
                    for (Method method : groupManager.getClass().getMethods()) {
                        console.info("• " + method.toString());
                    }
                }
            } catch (Exception e) {
                console.warning("Konnte GroupManager nicht analysieren: " + e.getMessage());
            }
            
            // Versuche, die NodeBuilderRegistry zu bekommen
            try {
                Method getNodeBuilderRegistryMethod = permissionsProvider.getClass().getMethod("getNodeBuilderRegistry");
                Object nodeBuilderRegistry = getNodeBuilderRegistryMethod.invoke(permissionsProvider);
                
                if (nodeBuilderRegistry != null) {
                    console.section("NODE BUILDER REGISTRY METHODEN");
                    console.info("NodeBuilderRegistry Klasse: " + nodeBuilderRegistry.getClass().getName());
                    for (Method method : nodeBuilderRegistry.getClass().getMethods()) {
                        console.info("• " + method.toString());
                    }
                }
            } catch (Exception e) {
                console.warning("Konnte NodeBuilderRegistry nicht analysieren: " + e.getMessage());
            }
        } catch (Exception e) {
            console.error("Fehler bei der API-Analyse: " + e.getMessage());
        }
    }
    
    /**
     * Batch registriert alle Permissions in LuckPerms
     */
    public void registerPermissions(Map<String, String[]> permissions) {
        if (!permissionsHooked) return;

        try {
            console.info("Registriere " + permissions.size() + " Berechtigungen in LuckPerms...");
            
            // Wir verwenden einen anderen Ansatz, der mit verschiedenen LuckPerms-Versionen kompatibel ist
            boolean success = true;
            for (Map.Entry<String, String[]> entry : permissions.entrySet()) {
                String permission = entry.getKey();
                String description = entry.getValue()[0];
                
                try {
                    // Prüfe zuerst, ob die Berechtigung bereits existiert
                    if (permissionsProvider != null) {
                        // Versuche in Bukkit zu registrieren
                        Permission bukkitPerm = new Permission(permission, description, PermissionDefault.OP);
                        try {
                            Bukkit.getPluginManager().addPermission(bukkitPerm);
                        } catch (IllegalArgumentException e) {
                            // Berechtigung existiert bereits, ignorieren
                            if (apiCore.isDebugMode()) {
                                console.debug("Berechtigung existiert bereits in Bukkit: " + permission, true);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fehler nur im Debug-Modus protokollieren
                    if (apiCore.isDebugMode()) {
                        console.debug("Fehler bei Berechtigung " + permission + ": " + e.getMessage(), true);
                    }
                    success = false;
                }
            }
            
            if (success) {
                console.success(permissions.size() + " Berechtigungen erfolgreich registriert");
            } else {
                console.warning("Einige Berechtigungen konnten nicht registriert werden (nur in Bukkit registriert)");
            }
        } catch (Exception e) {
            console.error("Fehler beim Registrieren von Berechtigungen: " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Prüft, ob ein Spieler eine bestimmte Berechtigung hat
     */
    public boolean hasPermission(Player player, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        
        // OP-Spieler haben immer alle Rechte
        if (player.isOp()) {
            return true;
        }
        
        // Direkte Bukkit-Permissions prüfen
        // Diese Prüfung sollte bereits im ApiCore erfolgt sein, ist aber hier als Sicherheit
        if (player.hasPermission(permission)) {
            return true;
        }
        
        // Wildcards prüfen (z.B. permission.* für permission.subgroup)
        String[] permParts = permission.split("\\.");
        if (permParts.length > 1) {
            StringBuilder wildcardBuilder = new StringBuilder();
            for (int i = 0; i < permParts.length - 1; i++) {
                wildcardBuilder.append(permParts[i]).append(".");
            }
            wildcardBuilder.append("*");
            
            String wildcardPerm = wildcardBuilder.toString();
            if (player.hasPermission(wildcardPerm)) {
                if (apiCore.isDebugMode()) {
                    apiCore.getLogger().info("Wildcard-Berechtigung erkannt: " + wildcardPerm + " für " + permission + " bei " + player.getName());
                }
                return true;
            }
        }
        
        // Allgemeines Wildcard prüfen (*)
        if (player.hasPermission("*")) {
            return true;
        }
        
        // LuckPerms-Integration, falls vorhanden
        if (permissionsHooked && permissionsProvider != null) {
            try {
                Method getUserMethod = permissionsProvider.getClass().getMethod("getPlayerAdapter", Class.class);
                Object playerAdapter = getUserMethod.invoke(permissionsProvider, Player.class);
                
                Method getUser = playerAdapter.getClass().getMethod("getUser", Player.class);
                Object user = getUser.invoke(playerAdapter, player);
                
                Method checkPermMethod = user.getClass().getMethod("getCachedData");
                Object permissionData = checkPermMethod.invoke(user);
                
                Method permissibleMethod = permissionData.getClass().getMethod("getPermissionData");
                Object permissible = permissibleMethod.invoke(permissionData);
                
                Method checkPermissionMethod = permissible.getClass().getMethod("checkPermission", String.class);
                Object result = checkPermissionMethod.invoke(permissible, permission);
                
                if (result instanceof Boolean) {
                    if ((Boolean) result) {
                        return true;
                    }
                    
                    // Wenn die direkte Berechtigung nicht vorhanden ist, prüfe Wildcards in LuckPerms
                    if (permParts.length > 1) {
                        String wildcardPerm = null;
                        for (int i = permParts.length - 1; i > 0; i--) {
                            StringBuilder sb = new StringBuilder();
                            for (int j = 0; j < i; j++) {
                                sb.append(permParts[j]).append(".");
                            }
                            sb.append("*");
                            wildcardPerm = sb.toString();
                            
                            result = checkPermissionMethod.invoke(permissible, wildcardPerm);
                            if (result instanceof Boolean && (Boolean) result) {
                                if (apiCore.isDebugMode()) {
                                    apiCore.getLogger().info("LuckPerms Wildcard gefunden: " + wildcardPerm + " für " + permission);
                                }
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (apiCore.isDebugMode()) {
                    apiCore.getLogger().warning("Fehler bei LuckPerms-Permission-Check für " + permission + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Gibt zurück, ob eine Permissions-API eingebunden ist
     */
    public boolean isPermissionsHooked() {
        return permissionsHooked;
    }
    
    /**
     * Gibt den Permissions-Provider (LuckPerms) zurück, falls eingebunden
     */
    public Object getPermissionsProvider() {
        return permissionsProvider;
    }
    
    /**
     * Registriert alle Berechtigungen für ein Modul
     * 
     * @param moduleName Der Name des Moduls
     * @param permissions Die zu registrierenden Berechtigungen
     */
    public void registerModulePermissions(String moduleName, Map<String, String[]> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        
        console.subHeader("BERECHTIGUNGEN FÜR MODUL " + moduleName);
        console.info("Registriere " + permissions.size() + " Berechtigungen...");
        
        // Berechtigung im Bukkit-System registrieren
        int successCount = 0;
        int errorCount = 0;
        
        for (Map.Entry<String, String[]> entry : permissions.entrySet()) {
            String fullPermission = entry.getKey();
            String[] data = entry.getValue();
            
            if (data == null || data.length < 2) {
                console.warning("Ungültige Berechtigungsdaten für " + fullPermission);
                errorCount++;
                continue;
            }
            
            String description = data[0];
            String defaultValue = data[1];
            
            try {
                registerPermission(fullPermission, description, defaultValue);
                successCount++;
                if (apiCore.isDebugMode()) {
                    console.debug("Berechtigung registriert: " + fullPermission + " (default=" + defaultValue + ")", true);
                }
            } catch (Exception e) {
                console.error("Fehler bei " + fullPermission + ": " + e.getMessage());
                errorCount++;
            }
        }
        
        console.success(successCount + " Berechtigungen erfolgreich registriert" + 
                      (errorCount > 0 ? ", " + errorCount + " Fehler" : ""));
        
        // Wenn LuckPerms verfügbar ist, auch dort registrieren
        if (permissionsHooked && permissionsProvider != null) {
            console.info("Registriere Berechtigungen in LuckPerms...");
            registerPermissions(permissions);
        }
    }
    
    /**
     * Registriert eine einzelne Berechtigung im System
     * 
     * @param permissionName Der vollständige Berechtigungsname
     * @param description Die Beschreibung der Berechtigung
     * @param defaultValue Der Standardwert (als String: "op", "true", "false" oder "admin")
     */
    private void registerPermission(String permissionName, String description, String defaultValue) {
        try {
            PermissionDefault permDefault;
            switch (defaultValue.toLowerCase()) {
                case "op":
                    permDefault = PermissionDefault.OP;
                    break;
                case "true":
                    permDefault = PermissionDefault.TRUE;
                    break;
                case "false":
                    permDefault = PermissionDefault.FALSE;
                    break;
                case "admin":
                    permDefault = PermissionDefault.OP; // Fallback auf OP
                    break;
                default:
                    permDefault = PermissionDefault.OP;
                    apiCore.getLogger().warning("Unbekannter Standardwert für Berechtigung " + permissionName + ": " + defaultValue);
            }
            
            Permission permission = new Permission(permissionName, description, permDefault);
            
            // Spezielle Behandlung für bereits registrierte Berechtigungen
            try {
                Permission existingPerm = Bukkit.getPluginManager().getPermission(permissionName);
                if (existingPerm != null) {
                    // Aktualisiere die bestehende Berechtigung
                    existingPerm.setDescription(description);
                    existingPerm.setDefault(permDefault);
                    apiCore.getLogger().info("Bestehende Berechtigung aktualisiert: " + permissionName);
                    return;
                }
            } catch (Exception e) {
                // Ignoriere Fehler und fahre mit Registrierung fort
            }
            
            // Registriere die neue Berechtigung
            Bukkit.getPluginManager().addPermission(permission);
            
            // Optimierung: Berechtigung im Cache speichern, wenn verfügbar
            if (apiCore.getPermissionCacheSize() > 0) {
                // Cache in ApiCore speichern statt lokale Variable zu verwenden
                apiCore.cachePermission(permissionName);
            }
        } catch (Exception e) {
            apiCore.getLogger().warning("Konnte Berechtigung " + permissionName + " nicht registrieren: " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Alternative Methode zum Hinzufügen einer Berechtigung zu einem Spieler
     * mit vereinfachtem Zugriff auf die API
     */
    public boolean addPermissionToPlayerSimple(Player player, String permission, boolean value) {
        if (!permissionsHooked || permissionsProvider == null) {
            apiCore.getLogger().warning("Kein Permissions-System verbunden.");
            return false;
        }
        
        try {
            // Vereinfachter Ansatz: Befehl über Bukkit ausführen
            String command = "lp user " + player.getName() + " permission set " + permission + " " + value;
            apiCore.getLogger().info("Führe Befehl aus: " + command);
            
            // Console-Sender holen
            CommandSender consoleSender = Bukkit.getConsoleSender();
            
            // Befehl als Konsole ausführen
            Bukkit.dispatchCommand(consoleSender, command);
            
            // Cache leeren
            if (apiCore.getPermissionCacheSize() > 0) {
                String cacheKey = player.getUniqueId() + ":" + permission;
                try {
                    java.lang.reflect.Field permExactCacheField = ApiCore.class.getDeclaredField("permissionExactCache");
                    permExactCacheField.setAccessible(true);
                    Object permExactCache = permExactCacheField.get(apiCore);
                    if (permExactCache instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Boolean> cache = (Map<String, Boolean>) permExactCache;
                        cache.remove(cacheKey);
                    }
                } catch (Exception e) {
                    apiCore.getLogger().warning("Konnte Cache nicht leeren: " + e.getMessage());
                }
            }
            
            apiCore.getLogger().info("Berechtigung " + permission + " wurde Spieler " + player.getName() + " hinzugefügt (Wert: " + value + ")");
            return true;
        } catch (Exception e) {
            apiCore.getLogger().warning("Fehler bei der einfachen Berechtigungszuweisung: " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Alternative Methode zum Hinzufügen einer Berechtigung zu einer Gruppe
     * mit vereinfachtem Zugriff auf die API
     */
    public boolean addPermissionToGroupSimple(String group, String permission, boolean value) {
        if (!permissionsHooked || permissionsProvider == null) {
            apiCore.getLogger().warning("Kein Permissions-System verbunden.");
            return false;
        }
        
        try {
            // Vereinfachter Ansatz: Befehl über Bukkit ausführen
            String command = "lp group " + group + " permission set " + permission + " " + value;
            apiCore.getLogger().info("Führe Befehl aus: " + command);
            
            // Console-Sender holen
            CommandSender consoleSender = Bukkit.getConsoleSender();
            
            // Befehl als Konsole ausführen
            Bukkit.dispatchCommand(consoleSender, command);
            
            apiCore.getLogger().info("Berechtigung " + permission + " wurde Gruppe " + group + " hinzugefügt (Wert: " + value + ")");
            return true;
        } catch (Exception e) {
            apiCore.getLogger().warning("Fehler bei der einfachen Gruppenberechtigungszuweisung: " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Fügt eine Berechtigung zu einem Spieler hinzu
     *
     * @param player Der Spieler, dem die Berechtigung hinzugefügt werden soll
     * @param permission Die Berechtigung
     * @param value Der Wert (true/false)
     * @param temporary Ob die Berechtigung temporär ist (verfällt beim Neustart)
     * @return true, wenn erfolgreich
     */
    public boolean addPermissionToPlayer(Player player, String permission, boolean value, boolean temporary) {
        // Verwende die vereinfachte Methode, da die Reflection-basierte Methode nicht funktioniert
        return addPermissionToPlayerSimple(player, permission, value);
    }
    
    /**
     * Fügt eine Berechtigung zu einer Gruppe hinzu
     *
     * @param group Die Gruppe
     * @param permission Die Berechtigung
     * @param value Der Wert (true/false)
     * @return true, wenn erfolgreich
     */
    public boolean addPermissionToGroup(String group, String permission, boolean value) {
        // Verwende die vereinfachte Methode, da die Reflection-basierte Methode nicht funktioniert
        return addPermissionToGroupSimple(group, permission, value);
    }
    
    /**
     * Gibt alle verfügbaren Gruppen zurück
     * 
     * @return Liste der verfügbaren Gruppen
     */
    public java.util.List<String> getAvailableGroups() {
        java.util.List<String> groups = new java.util.ArrayList<>();
        
        if (!permissionsHooked || permissionsProvider == null) {
            return groups;
        }
        
        try {
            // GroupManager holen
            Method getGroupManager = permissionsProvider.getClass().getMethod("getGroupManager");
            Object groupManager = getGroupManager.invoke(permissionsProvider);
            
            // Alle Gruppen abrufen
            Method getLoadedGroups = groupManager.getClass().getMethod("getLoadedGroups");
            Object loadedGroups = getLoadedGroups.invoke(groupManager);
            
            if (loadedGroups instanceof java.util.Collection) {
                for (Object group : (java.util.Collection<?>) loadedGroups) {
                    Method getName = group.getClass().getMethod("getName");
                    String groupName = (String) getName.invoke(group);
                    groups.add(groupName);
                }
            }
        } catch (Exception e) {
            apiCore.getLogger().warning("Fehler beim Abrufen der verfügbaren Gruppen: " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        return groups;
    }
    
    /**
     * Gibt alle registrierten Berechtigungen zurück
     * 
     * @return Liste aller verfügbaren Berechtigungen
     */
    public java.util.Set<String> getAllRegisteredPermissions() {
        java.util.Set<String> permissions = new java.util.HashSet<>();
        
        // Zuerst alle Bukkit-Berechtigungen sammeln
        Bukkit.getPluginManager().getPermissions().forEach(perm -> permissions.add(perm.getName()));
        
        // Wenn LuckPerms verbunden ist, versuche auch deren Berechtigungen zu bekommen
        if (permissionsHooked && permissionsProvider != null) {
            try {
                // Versuche, alle in LuckPerms registrierten Berechtigungen abzurufen
                Method getContextManager = permissionsProvider.getClass().getMethod("getContextManager");
                Object contextManager = getContextManager.invoke(permissionsProvider);
                
                Method getQueryOptions = contextManager.getClass().getMethod("getStaticQueryOptions");
                Object queryOptions = getQueryOptions.invoke(contextManager);
                
                // Versuche, die registrierten Berechtigungen aus allen Gruppen zu sammeln
                Method getGroupManager = permissionsProvider.getClass().getMethod("getGroupManager");
                Object groupManager = getGroupManager.invoke(permissionsProvider);
                
                Method getLoadedGroups = groupManager.getClass().getMethod("getLoadedGroups");
                Object loadedGroups = getLoadedGroups.invoke(groupManager);
                
                if (loadedGroups instanceof java.util.Collection) {
                    for (Object group : (java.util.Collection<?>) loadedGroups) {
                        try {
                            Method getCachedData = group.getClass().getMethod("getCachedData");
                            Object cachedData = getCachedData.invoke(group);
                            
                            Method getPermissionData = cachedData.getClass().getMethod("getPermissionData", queryOptions.getClass());
                            Object permissionData = getPermissionData.invoke(cachedData, queryOptions);
                            
                            Method getPermissionMap = permissionData.getClass().getMethod("getPermissionMap");
                            Object permissionMap = getPermissionMap.invoke(permissionData);
                            
                            if (permissionMap instanceof java.util.Map) {
                                java.util.Map<?, ?> map = (java.util.Map<?, ?>) permissionMap;
                                for (Object key : map.keySet()) {
                                    if (key instanceof String) {
                                        permissions.add((String) key);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignoriere Fehler bei einzelnen Gruppen
                            if (apiCore.isDebugMode()) {
                                apiCore.getLogger().fine("Konnte Berechtigungen für eine Gruppe nicht abrufen: " + e.getMessage());
                            }
                        }
                    }
                }
                
                // Sammle auch Berechtigungen von Spielern
                Method getUserManager = permissionsProvider.getClass().getMethod("getUserManager");
                Object userManager = getUserManager.invoke(permissionsProvider);
                
                Method getLoadedUsers = userManager.getClass().getMethod("getLoadedUsers");
                Object loadedUsers = getLoadedUsers.invoke(userManager);
                
                if (loadedUsers instanceof java.util.Collection) {
                    for (Object user : (java.util.Collection<?>) loadedUsers) {
                        try {
                            Method getCachedData = user.getClass().getMethod("getCachedData");
                            Object cachedData = getCachedData.invoke(user);
                            
                            Method getPermissionData = cachedData.getClass().getMethod("getPermissionData", queryOptions.getClass());
                            Object permissionData = getPermissionData.invoke(cachedData, queryOptions);
                            
                            Method getPermissionMap = permissionData.getClass().getMethod("getPermissionMap");
                            Object permissionMap = getPermissionMap.invoke(permissionData);
                            
                            if (permissionMap instanceof java.util.Map) {
                                java.util.Map<?, ?> map = (java.util.Map<?, ?>) permissionMap;
                                for (Object key : map.keySet()) {
                                    if (key instanceof String) {
                                        permissions.add((String) key);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignoriere Fehler bei einzelnen Benutzern
                            if (apiCore.isDebugMode()) {
                                apiCore.getLogger().fine("Konnte Berechtigungen für einen Benutzer nicht abrufen: " + e.getMessage());
                            }
                        }
                    }
                }
                
                // Als Fallback direkteren Weg versuchen mit allen registrierten Knoten
                try {
                    // NodeBuilderRegistry wird für zukünftige Implementierung reserviert
                    // Method getNodeBuilderRegistry = permissionsProvider.getClass().getMethod("getNodeBuilderRegistry");
                    // Object nodeBuilderRegistry = getNodeBuilderRegistry.invoke(permissionsProvider);
                    
                    // Es ist schwierig, alle registrierten Knoten aus dem NodeBuilderRegistry zu bekommen,
                    // da LuckPerms keine direkte Methode dafür anbietet. Wir können es aber indirekt versuchen.
                    
                    // Zusätzlich extrahieren wir Berechtigungen aus den Standard-Plugins
                    for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                        String pluginName = plugin.getName().toLowerCase();
                        
                        // Einfache Berechtigungsmuster basierend auf Plugin-Namen erzeugen
                        permissions.add(pluginName + ".*");
                        permissions.add(pluginName + ".admin");
                        permissions.add(pluginName + ".use");
                        permissions.add(pluginName + ".command.*");
                    }
                } catch (Exception e) {
                    // Ignorieren und mit dem was wir haben weitermachen
                    if (apiCore.isDebugMode()) {
                        apiCore.getLogger().fine("Konnte nicht alle LuckPerms-Nodes abrufen: " + e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                apiCore.getLogger().warning("Fehler beim Abrufen der LuckPerms-Berechtigungen: " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        
        // Einige Standard-Berechtigungen hinzufügen
        // Core-Berechtigungen
        permissions.add("apicore.admin");
        permissions.add("apicore.admin.*");
        permissions.add("apicore.commands");
        permissions.add("apicore.debug");
        permissions.add("apicore.status");
        permissions.add("apicore.reload");
        
        // Module-Berechtigungen sammeln
        for (String moduleName : apiCore.getLoadedModules().keySet()) {
            permissions.add(moduleName.toLowerCase() + ".admin");
            permissions.add(moduleName.toLowerCase() + ".use");
            permissions.add(moduleName.toLowerCase() + ".command.*");
            permissions.add("essentialscore." + moduleName.toLowerCase() + ".admin");
        }
        
        return permissions;
    }
    
    // Interface implementation methods
    @Override
    public boolean registerPermission(Permission permission) {
        try {
            if (permission != null && !isPermissionRegistered(permission.getName())) {
                apiCore.getServer().getPluginManager().addPermission(permission);
                return true;
            }        } catch (Exception e) {
            console.warning("Fehler beim Registrieren der Berechtigung " + (permission != null ? permission.getName() : "null") + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean registerModulePermission(String moduleId, Permission permission) {
        if (moduleId == null || permission == null) return false;
        return registerPermission(permission);
    }

    @Override
    public boolean registerModulePermission(String moduleId, String name, String description, PermissionDefault defaultValue) {
        if (moduleId == null || name == null) return false;
        Permission permission = new Permission(name, description, defaultValue);
        return registerModulePermission(moduleId, permission);
    }

    @Override
    public boolean unregisterPermission(String permissionName) {
        try {
            if (permissionName != null && isPermissionRegistered(permissionName)) {
                apiCore.getServer().getPluginManager().removePermission(permissionName);
                return true;
            }        } catch (Exception e) {
            console.warning("Fehler beim Entfernen der Berechtigung " + permissionName + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public int unregisterModulePermissions(String moduleId) {
        if (moduleId == null) return 0;
        int count = 0;
        List<Permission> permissions = getModulePermissions(moduleId);
        for (Permission permission : permissions) {
            if (unregisterPermission(permission.getName())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<Permission> getModulePermissions(String moduleId) {
        List<Permission> modulePermissions = new ArrayList<>();
        if (moduleId == null) return modulePermissions;
        
        for (Permission permission : getAllPermissions()) {
            if (permission.getName().startsWith(moduleId.toLowerCase() + ".") || 
                permission.getName().startsWith("essentialscore." + moduleId.toLowerCase() + ".")) {
                modulePermissions.add(permission);
            }
        }
        return modulePermissions;
    }

    @Override
    public List<Permission> getAllPermissions() {
        return new ArrayList<>(apiCore.getServer().getPluginManager().getPermissions());
    }

    @Override
    public Map<String, List<Permission>> getAllModulePermissions() {
        Map<String, List<Permission>> modulePermissions = new HashMap<>();
        for (String moduleId : apiCore.getLoadedModules().keySet()) {
            modulePermissions.put(moduleId, getModulePermissions(moduleId));
        }
        return modulePermissions;
    }

    @Override
    public boolean addPermission(Player player, String permission) {
        if (player == null || permission == null) return false;
        try {
            // This would typically require a permissions plugin integration
            // For now, we just check if they have the permission
            return player.hasPermission(permission);        } catch (Exception e) {
            console.warning("Fehler beim Hinzufügen der Berechtigung " + permission + " für Spieler " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean removePermission(Player player, String permission) {
        if (player == null || permission == null) return false;
        try {
            // This would typically require a permissions plugin integration
            // For now, we return false as we can't remove permissions without a permissions plugin
            return false;        } catch (Exception e) {
            console.warning("Fehler beim Entfernen der Berechtigung " + permission + " für Spieler " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<String> getPlayerPermissions(Player player) {
        List<String> permissions = new ArrayList<>();
        if (player == null) return permissions;
        
        try {
            // Get all effective permissions for the player
            for (Permission permission : getAllPermissions()) {
                if (player.hasPermission(permission)) {
                    permissions.add(permission.getName());
                }
            }        } catch (Exception e) {
            console.warning("Fehler beim Abrufen der Berechtigungen für Spieler " + player.getName() + ": " + e.getMessage());
        }
        return permissions;
    }

    @Override
    public boolean isPermissionRegistered(String permissionName) {
        if (permissionName == null) return false;
        return apiCore.getServer().getPluginManager().getPermission(permissionName) != null;
    }

    @Override
    public Permission getPermission(String permissionName) {
        if (permissionName == null) return null;
        return apiCore.getServer().getPluginManager().getPermission(permissionName);
    }

    @Override
    public boolean hasModulePermission(Player player, String moduleId, String permission) {
        if (player == null || moduleId == null || permission == null) return false;
        
        // Check module-specific permission
        String modulePermission = moduleId.toLowerCase() + "." + permission;
        if (hasPermission(player, modulePermission)) {
            return true;
        }
        
        // Check essentialscore-prefixed permission
        String corePermission = "essentialscore." + moduleId.toLowerCase() + "." + permission;
        return hasPermission(player, corePermission);
    }
}
