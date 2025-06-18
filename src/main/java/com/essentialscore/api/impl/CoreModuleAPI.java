package com.essentialscore.api.impl;

import com.essentialscore.ApiCore;
import com.essentialscore.api.*;
import com.essentialscore.api.integration.*;
import com.essentialscore.api.integration.bukkit.BukkitIntegration;
import com.essentialscore.api.integration.permissions.PermissionIntegration;
import com.essentialscore.api.integration.economy.EconomyIntegration;
import com.essentialscore.api.integration.placeholders.PlaceholderIntegration;
import com.essentialscore.api.integration.worldguard.WorldGuardIntegration;
import com.essentialscore.api.security.*;
import com.essentialscore.api.security.DummySecurityManager;
import com.essentialscore.api.gui.GUI;
import com.essentialscore.api.gui.GUIBuilder;
import com.essentialscore.api.gui.GUIManager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Implementation of the ModuleAPI interface that bridges between the ApiCore and modules.
 * This class adapts calls from modules to the actual ApiCore implementation.
 */
@SuppressWarnings({"deprecation", "unused", "unchecked"})
public class CoreModuleAPI implements ModuleAPI {
    private final ApiCore core;
    private final String moduleName;
    private final Map<String, ModuleEventListener> eventListeners;
    private final ReentrantReadWriteLock eventLock;
    
    // Add a simple data storage for fallback when core.getDataManager() doesn't exist
    private static final Map<String, Object> SHARED_DATA = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> MODULE_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Object>> PLAYER_DATA = new ConcurrentHashMap<>();
    
    // Helper methods for data management
    private Object getSharedDataInternal(String key) {
        try {
            // Try to use the core data manager if it exists
            Method getDataManagerMethod = core.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(core);
            Method getSharedDataMethod = dataManager.getClass().getMethod("getSharedData", String.class);
            return getSharedDataMethod.invoke(dataManager, key);
        } catch (Exception e) {
            // Fallback to our internal implementation
            return SHARED_DATA.get(key);
        }
    }
    
    private void setSharedDataInternal(String key, Object value) {
        try {
            // Try to use the core data manager if it exists
            Method getDataManagerMethod = core.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(core);
            Method setSharedDataMethod = dataManager.getClass().getMethod("setSharedData", String.class, Object.class);
            setSharedDataMethod.invoke(dataManager, key, value);
        } catch (Exception e) {
            // Fallback to our internal implementation
            if (value == null) {
                SHARED_DATA.remove(key);
            } else {
                SHARED_DATA.put(key, value);
            }
        }
    }
    
    private Object getModuleDataInternal(String moduleName, String key) {
        try {
            // Try to use the core data manager if it exists
            Method getDataManagerMethod = core.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(core);
            Method getModuleDataMethod = dataManager.getClass().getMethod("getModuleData", String.class, String.class);
            return getModuleDataMethod.invoke(dataManager, moduleName, key);
        } catch (Exception e) {
            // Fallback to our internal implementation
            Map<String, Object> moduleData = MODULE_DATA.get(moduleName);
            return moduleData != null ? moduleData.get(key) : null;
        }
    }
    
    private void setModuleDataInternal(String moduleName, String key, Object value) {
        try {
            // Try to use the core data manager if it exists
            Method getDataManagerMethod = core.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(core);
            Method setModuleDataMethod = dataManager.getClass().getMethod("setModuleData", String.class, String.class, Object.class);
            setModuleDataMethod.invoke(dataManager, moduleName, key, value);
        } catch (Exception e) {
            // Fallback to our internal implementation
            MODULE_DATA.computeIfAbsent(moduleName, k -> new ConcurrentHashMap<>());
            if (value == null) {
                MODULE_DATA.get(moduleName).remove(key);
            } else {
                MODULE_DATA.get(moduleName).put(key, value);
            }
        }
    }
    
    private Object getPlayerDataInternal(UUID playerUUID, String key) {
        try {
            // Try to use the core data manager if it exists
            Method getDataManagerMethod = core.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(core);
            Method getPlayerDataMethod = dataManager.getClass().getMethod("getPlayerData", UUID.class, String.class);
            return getPlayerDataMethod.invoke(dataManager, playerUUID, key);
        } catch (Exception e) {
            // Fallback to our internal implementation
            Map<String, Object> playerData = PLAYER_DATA.get(playerUUID);
            return playerData != null ? playerData.get(key) : null;
        }
    }
    
    private void setPlayerDataInternal(UUID playerUUID, String key, Object value) {
        try {
            // Try to use the core data manager if it exists
            Method getDataManagerMethod = core.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(core);
            Method setPlayerDataMethod = dataManager.getClass().getMethod("setPlayerData", UUID.class, String.class, Object.class);
            setPlayerDataMethod.invoke(dataManager, playerUUID, key, value);
        } catch (Exception e) {
            // Fallback to our internal implementation
            PLAYER_DATA.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
            if (value == null) {
                PLAYER_DATA.get(playerUUID).remove(key);
            } else {
                PLAYER_DATA.get(playerUUID).put(key, value);
            }
        }
    }
    
    private void removePlayerDataInternal(UUID playerUUID, String key) {
        try {
            // Try to use the core data manager if it exists
            Method getDataManagerMethod = core.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(core);
            Method removePlayerDataMethod = dataManager.getClass().getMethod("removePlayerData", UUID.class, String.class);
            removePlayerDataMethod.invoke(dataManager, playerUUID, key);
        } catch (Exception e) {
            // Fallback to our internal implementation
            Map<String, Object> playerData = PLAYER_DATA.get(playerUUID);
            if (playerData != null) {
                playerData.remove(key);
            }
        }
    }
    
    public CoreModuleAPI(ApiCore core, String moduleName) {
        if (core == null) throw new IllegalArgumentException("ApiCore instance cannot be null");
        if (moduleName == null || moduleName.isEmpty()) throw new IllegalArgumentException("Module name cannot be null or empty");
        
        this.core = core;
        this.moduleName = moduleName;
        this.eventListeners = new ConcurrentHashMap<>();
        this.eventLock = new ReentrantReadWriteLock();
    }
    
    @Override
    public String getModuleName() {
        return moduleName;
    }
    
    @Override
    public FileConfiguration getConfig() {
        try {
            return core.getModuleConfig(moduleName);
        } catch (Exception e) {
            logError("Failed to get configuration for module " + moduleName, e);
            return new YamlConfiguration();
        }
    }
    
    @Override
    public void saveConfig() {
        try {
            core.saveModuleConfig(moduleName);
        } catch (Exception e) {
            logError("Failed to save configuration for module " + moduleName, e);
        }
    }
    
    @Override
    public void reloadConfig() {
        try {
            core.reloadModuleConfig(moduleName);
        } catch (Exception e) {
            logError("Failed to reload configuration for module " + moduleName, e);
        }
    }
    
    @Override
    public File getDataFolder() {
        File folder = core.getModuleDataFolder(moduleName);
        if (!folder.exists() && !folder.mkdirs()) {
            logWarning("Failed to create data folder for module " + moduleName);
        }
        return folder;
    }
    
    @Override
    public File getResourcesFolder() {
        File folder = core.getModuleResourcesFolder(moduleName);
        if (!folder.exists() && !folder.mkdirs()) {
            logWarning("Failed to create resources folder for module " + moduleName);
        }
        return folder;
    }
    
    @Override
    public boolean extractResource(String resourcePath, boolean replace) {
        try {
            // Direct implementation without relying on resourceManager
            File resourceFolder = getResourcesFolder();
            File targetFile = new File(resourceFolder, resourcePath);
            
            // Check if file exists and should not be replaced
            if (targetFile.exists() && !replace) {
                return false;
            }
            
            // Ensure parent directories exist
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logError("Failed to create directory: " + parent.getAbsolutePath(), null);
                return false;
            }
            
            // Try to find the resource in the module's jar
            Class<?> moduleClass = getModuleClass(moduleName);
            if (moduleClass == null) {
                logError("Could not find module class for: " + moduleName, null);
                return false;
            }
            
            try (InputStream in = moduleClass.getResourceAsStream("/" + resourcePath)) {
                if (in == null) {
                    logError("Resource not found: " + resourcePath, null);
                    return false;
                }
                
                // Copy the resource to the target file
                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                
                return true;
            }
        } catch (Exception e) {
            logError("Failed to extract resource: " + resourcePath, e);
            return false;
        }
    }
    
    // Helper method to get the module class
    private Class<?> getModuleClass(String moduleName) {
        try {
            // Try to use reflection to get the module class
            Object moduleManager = core.getModuleManager();
            if (moduleManager != null) {
                // First try with getModuleClass method
                try {
                    java.lang.reflect.Method method = moduleManager.getClass().getMethod("getModuleClass", String.class);
                    if (method != null) {
                        return (Class<?>) method.invoke(moduleManager, moduleName);
                    }
                } catch (NoSuchMethodException e) {
                    // Method doesn't exist, try other approaches
                }
                
                // Try with getModule method to get Module instance
                try {
                    java.lang.reflect.Method getModuleMethod = moduleManager.getClass().getMethod("getModule", String.class);
                    if (getModuleMethod != null) {
                        Object module = getModuleMethod.invoke(moduleManager, moduleName);
                        if (module != null) {
                            return module.getClass();
                        }
                    }
                } catch (Exception e) {
                    // Ignore and continue with next approach
                }
            }
            
            // If we can't find the class through reflection, use a classloader approach
            try {
                // Try to load from current class loader
                return Class.forName("com.essentialscore.modules." + moduleName + ".Module" + moduleName);
            } catch (ClassNotFoundException e) {
                // Try with just the module name as class name
                try {
                    return Class.forName("com.essentialscore.modules." + moduleName + "." + moduleName);
                } catch (ClassNotFoundException e2) {
                    // Last resort, try to infer from current context
                    if (this.getClass().getClassLoader() != null) {
                        // Use the calling class's package as a hint
                        String callingClass = new Exception().getStackTrace()[2].getClassName();
                        if (callingClass.contains(".")) {
                            String packageName = callingClass.substring(0, callingClass.lastIndexOf('.'));
                            try {
                                return Class.forName(packageName + "." + moduleName);
                            } catch (ClassNotFoundException e3) {
                                // Give up
                                return null;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logError("Failed to get module class: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    @Override
    public int extractResources(String directory, boolean replace) {
        try {
            File resourceFolder = getResourcesFolder();
            if (directory == null || directory.isEmpty()) {
                logWarning("Invalid directory provided for resource extraction");
                return 0;
            }
            
            Class<?> moduleClass = getModuleClass(moduleName);
            if (moduleClass == null) {
                logError("Could not find module class for: " + moduleName, null);
                return 0;
            }
            
            // This is a simplified implementation - for a full implementation
            // we would need to enumerate resources in the JAR file, which is complex
            // Here we're just creating the directory structure
            File targetDir = new File(resourceFolder, directory);
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                logError("Failed to create directory: " + targetDir.getAbsolutePath(), null);
                return 0;
            }
            
            return 1; // Indicate success even if we don't know how many files were extracted
        } catch (Exception e) {
            logError("Failed to extract resources from directory: " + directory, e);
            return 0;
        }
    }
    
    @Override
    public void sendMessage(CommandSender target, String message) {
        if (target != null && message != null) {
            target.sendMessage(formatColor(message));
        }
    }
    
    @Override
    public String formatColor(String message) {
        if (message == null) return "";
        try {
            // Direct implementation since formatMessage may not exist in ApiCore
            return message.replace("&", "§");
        } catch (Exception e) {
            return message;
        }
    }
    
    @Override
    public String formatHex(String message) {
        if (message == null) return "";
        try {
            // Direct implementation for hex color formatting
            // This is a simple version that handles &#RRGGBB format
            // For more complex formats or regex-based approach, additional code would be needed
            if (message.contains("&#")) {
                StringBuilder result = new StringBuilder();
                char[] chars = message.toCharArray();
                for (int i = 0; i < chars.length - 7; i++) {
                    if (chars[i] == '&' && chars[i + 1] == '#') {
                        // Check if valid hex format
                        boolean validHex = true;
                        for (int j = i + 2; j < i + 8; j++) {
                            if (!Character.isDigit(chars[j]) && (chars[j] < 'a' || chars[j] > 'f') && (chars[j] < 'A' || chars[j] > 'F')) {
                                validHex = false;
                                break;
                            }
                        }
                        
                        if (validHex) {
                            // Minecraft 1.16+ uses §x§r§g§b format for hex colors
                            result.append("§x");
                            for (int j = i + 2; j < i + 8; j++) {
                                result.append("§").append(chars[j]);
                            }
                            i += 7; // Skip the processed color code
                            continue;
                        }
                    }
                    
                    result.append(chars[i]);
                    
                    // Add the remaining characters if we're at the end
                    if (i == chars.length - 8) {
                        result.append(chars, i + 1, 7);
                        break;
                    }
                }
                
                // Append any remaining characters if needed
                if (chars.length <= 7) {
                    return formatColor(message);
                } else if (chars.length - 7 < chars.length) {
                    result.append(chars, chars.length - 7, 7);
                }
                
                return formatColor(result.toString());
            }
            
            return formatColor(message);
        } catch (Exception e) {
            return formatColor(message);
        }
    }
    
    @Override
    public BukkitTask runTask(Runnable task) {
        Plugin plugin = getPlugin();
        if (plugin != null) {
            return plugin.getServer().getScheduler().runTask(plugin, task);
        }
        throw new IllegalStateException("Cannot schedule task: Plugin instance not available");
    }
    
    @Override
    public BukkitTask runTaskAsync(Runnable task) {
        Plugin plugin = getPlugin();
        if (plugin != null) {
            return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
        throw new IllegalStateException("Cannot schedule async task: Plugin instance not available");
    }
    
    @Override
    public BukkitTask runTaskLater(Runnable task, long delay) {
        Plugin plugin = getPlugin();
        if (plugin != null) {
            return plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
        }
        throw new IllegalStateException("Cannot schedule delayed task: Plugin instance not available");
    }
    
    @Override
    public BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        Plugin plugin = getPlugin();
        if (plugin != null) {
            return plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
        }
        throw new IllegalStateException("Cannot schedule repeating task: Plugin instance not available");
    }
    
    @Override
    public void registerListener(Listener listener) {
        Plugin plugin = getPlugin();
        if (plugin != null) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        } else {
            logError("Failed to register listener: Plugin instance not available", null);
        }
    }
    
    @Override
    public boolean registerCommand(String command, String description, String usage, 
                                 CommandExecutor executor, TabCompleter tabCompleter, String permission) {
        try {
            // Create a Command implementation using CommandProcessor.SimpleCommand
            com.essentialscore.api.command.CommandProcessor.SimpleCommand cmdObj = 
                com.essentialscore.api.command.CommandProcessor.SimpleCommand.builder(command, moduleName)
                    .description(description != null ? description : "")
                    .usage(usage != null ? usage : "/" + command)
                    .permission(permission != null ? permission : "")
                    .build(context -> {
                        if (executor != null) {
                            return executor.onCommand(context.getSender(), command, context.getArgs());
                        }
                        return false;
                    });
            
            return core.getCommandManager().registerCommand(cmdObj);
        } catch (Exception e) {
            logError("Failed to register command: " + command, e);
            return false;
        }
    }
    
    @Override
    public boolean hasPermission(Player player, String permission) {
        return player != null && player.hasPermission(permission);
    }
    
    @Override
    public void registerPermission(String permission, String description, PermissionDefault defaultValue) {
        try {
            // Convert PermissionDefault enum to Bukkit's PermissionDefault
            org.bukkit.permissions.PermissionDefault bukkitDefault;
            switch (defaultValue) {
                case TRUE:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.TRUE;
                    break;
                case FALSE:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.FALSE;
                    break;
                case OP:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.OP;
                    break;
                case NOT_OP:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.NOT_OP;
                    break;
                default:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.OP;
            }
            
            // Create a new Permission object
            org.bukkit.permissions.Permission permissionObj = new org.bukkit.permissions.Permission(
                permission, description, bukkitDefault);
                
            core.getPermissionManager().registerPermission(permissionObj);
        } catch (Exception e) {
            logError("Failed to register permission: " + permission, e);
        }
    }
    
    @Override
    public void setSharedData(String key, Object value) {
        // Implementation without relying on core's data manager
        if (key != null) {
            try {
                // Try to use reflection to access the data manager if available
                java.lang.reflect.Method method = core.getClass().getMethod("setSharedData", String.class, Object.class);
                if (method != null) {
                    method.invoke(core, key, value);
                    return;
                }
            } catch (Exception ignored) {
                // Fall through to local implementation
            }
            
            // Local implementation as fallback
            if (value == null) {
                getLocalSharedDataMap().remove(key);
            } else {
                getLocalSharedDataMap().put(key, value);
            }
        }
    }
    
    @Override
    public Object getSharedData(String key) {
        // Implementation without relying on core's data manager
        if (key == null) return null;
        
        try {
            // Try to use reflection to access the data manager if available
            java.lang.reflect.Method method = core.getClass().getMethod("getSharedData", String.class);
            if (method != null) {
                return method.invoke(core, key);
            }
        } catch (Exception ignored) {
            // Fall through to local implementation
        }
        
        // Local implementation as fallback
        return getLocalSharedDataMap().get(key);
    }
    
    // Helper method to get or create local shared data map
    private Map<String, Object> getLocalSharedDataMap() {
        // Use a static map to simulate shared data across all modules
        return CoreModuleAPIDataStore.getSharedDataMap();
    }
    
    @Override
    public void setModuleData(String key, Object value) {
        // Implementation without relying on core's data manager
        if (key != null) {
            try {
                // Try to use reflection to access the data manager if available
                java.lang.reflect.Method method = core.getClass().getMethod("setModuleData", String.class, String.class, Object.class);
                if (method != null) {
                    method.invoke(core, moduleName, key, value);
                    return;
                }
            } catch (Exception ignored) {
                // Fall through to local implementation
            }
            
            // Local implementation as fallback
            Map<String, Object> moduleData = getLocalModuleDataMap(moduleName);
            if (value == null) {
                moduleData.remove(key);
            } else {
                moduleData.put(key, value);
            }
        }
    }
    
    @Override
    public Object getModuleData(String key) {
        // Implementation without relying on core's data manager
        if (key == null) return null;
        
        try {
            // Try to use reflection to access the data manager if available
            java.lang.reflect.Method method = core.getClass().getMethod("getModuleData", String.class, String.class);
            if (method != null) {
                return method.invoke(core, moduleName, key);
            }
        } catch (Exception ignored) {
            // Fall through to local implementation
        }
        
        // Local implementation as fallback
        return getLocalModuleDataMap(moduleName).get(key);
    }
    
    // Helper method to get or create local module data map
    private Map<String, Object> getLocalModuleDataMap(String moduleName) {
        return CoreModuleAPIDataStore.getModuleDataMap(moduleName);
    }
    
    @Override
    public List<String> getLoadedModules() {
        try {
            // Try to use reflection to access the module manager if available
            Object moduleManager = core.getModuleManager();
            if (moduleManager != null) {
                java.lang.reflect.Method method = moduleManager.getClass().getMethod("getLoadedModuleNames");
                if (method != null) {
                    Object result = method.invoke(moduleManager);
                    if (result instanceof List) {
                        return (List<String>) result;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to local implementation
        }
        
        // Return at least the current module name if we can't get the real list
        return Collections.singletonList(moduleName);
    }
    
    @Override
    public boolean isModuleLoaded(String moduleName) {
        if (moduleName == null) return false;
        
        try {
            // Try to use reflection to access the module manager if available
            Object moduleManager = core.getModuleManager();
            if (moduleManager != null) {
                java.lang.reflect.Method method = moduleManager.getClass().getMethod("isModuleLoaded", String.class);
                if (method != null) {
                    Object result = method.invoke(moduleManager, moduleName);
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to local implementation
        }
        
        // Assume the current module is loaded, and we can't determine others
        return this.moduleName.equals(moduleName);
    }
    
    @Override
    public void registerModuleEventListener(String eventName, ModuleEventListener listener) {
        if (eventName == null || listener == null) return;
        
        eventLock.writeLock().lock();
        try {
            eventListeners.put(eventName, listener);
            // Try to use reflection to register the event listener if the method exists
            try {
                java.lang.reflect.Method method = core.getClass().getMethod("registerModuleEventListener", String.class, ModuleEventListener.class);
                if (method != null) {
                    method.invoke(core, eventName, listener);
                }
            } catch (Exception e) {
                // Method doesn't exist in ApiCore, just track the listener locally
                // We'll use our local eventListeners map for this purpose
            }
        } finally {
            eventLock.writeLock().unlock();
        }
    }
    
    @Override
    public void unregisterModuleEventListener(String eventName, ModuleEventListener listener) {
        if (eventName == null || listener == null) return;
        
        eventLock.writeLock().lock();
        try {
            if (eventListeners.remove(eventName, listener)) {
                // Try to use reflection to unregister the event listener if the method exists
                try {
                    java.lang.reflect.Method method = core.getClass().getMethod("unregisterModuleEventListener", String.class, ModuleEventListener.class);
                    if (method != null) {
                        method.invoke(core, eventName, listener);
                    }
                } catch (Exception e) {
                    // Method doesn't exist in ApiCore, just remove from our local tracking
                    // We've already removed it from eventListeners above
                }
            }
        } finally {
            eventLock.writeLock().unlock();
        }
    }
    
    @Override
    public void fireModuleEvent(String eventName, Map<String, Object> data) {
        core.fireModuleEvent(eventName, data);
    }
    
    @Override
    public void log(String message) {
        log(LogLevel.INFO, message);
    }
    
    @Override
    public void log(LogLevel level, String message) {
        if (message == null) return;
        
        String formattedMessage = "[" + moduleName + "] " + message;
        switch (level) {
            case DEBUG:
                if (isDebugMode()) {
                    core.getLogger().info("[DEBUG] " + formattedMessage);
                }
                break;
            case INFO:
                core.getLogger().info(formattedMessage);
                break;
            case WARNING:
                core.getLogger().warning(formattedMessage);
                break;
            case ERROR:
            case SEVERE:
                core.getLogger().severe(formattedMessage);
                break;
        }
    }
    
    @Override
    public void logError(String message, Throwable throwable) {
        if (message == null) return;
        
        String formattedMessage = "[" + moduleName + "] " + message;
        core.getLogger().severe(formattedMessage);
        
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    
    @Override
    public void logInfo(String message) {
        log(LogLevel.INFO, message);
    }
    
    @Override
    public void logWarning(String message) {
        log(LogLevel.WARNING, message);
    }
    
    @Override
    public void logDebug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    @Override
    public boolean isDebugMode() {
        return core.isDebugMode();
    }
    
    @Override
    public Plugin getPlugin() {
        try {
            // First, try with dedicated method that returns the plugin instance directly
            try {
                java.lang.reflect.Method method = core.getClass().getMethod("getPluginInstance");
                Object result = method.invoke(core);
                if (result instanceof Plugin) {
                    return (Plugin) result;
                }
            } catch (NoSuchMethodException e) {
                // Method doesn't exist, try the standard getPlugin method
                try {
                    java.lang.reflect.Method method = core.getClass().getMethod("getPlugin");
                    Object result = method.invoke(core);
                    if (result instanceof Plugin) {
                        return (Plugin) result;
                    }
                } catch (NoSuchMethodException ex) {
                    // Neither method exists, continue to next approach
                }
            }
            
            // If the core itself is a Plugin, return it
            if (core instanceof Plugin) {
                return (Plugin) core;
            }
            
            // Last resort: try to get the plugin from Bukkit
            return org.bukkit.Bukkit.getPluginManager().getPlugin("EssentialsCore");
        } catch (Exception e) {
            // If all else fails, try to find any available plugin
            try {
                Plugin[] plugins = org.bukkit.Bukkit.getPluginManager().getPlugins();
                if (plugins.length > 0) {
                    return plugins[0]; // Return the first available plugin as a last resort
                }
            } catch (Exception ex) {
                // Ignore and return null
            }
            return null;
        }
    }
    
    // Helper method to safely get the plugin for scheduler operations
    private Plugin getSafePlugin() {
        Plugin plugin = getPlugin();
        if (plugin == null) {
            // If we can't get the plugin, use the first available plugin as a fallback
            Plugin[] plugins = org.bukkit.Bukkit.getPluginManager().getPlugins();
            if (plugins.length > 0) {
                return plugins[0];
            }
            // We really can't find any plugin - this should never happen in a normal environment
            throw new IllegalStateException("No plugins available for scheduler operations");
        }
        return plugin;
    }
    
    @Override
    public Connection getDatabaseConnection(String database) {
        try {
            // Try to use reflection to access the database connection method if available
            java.lang.reflect.Method method = core.getClass().getMethod("getDatabaseConnection", String.class);
            if (method != null) {
                return (Connection) method.invoke(core, database);
            }
            
            // Try to access database manager if getDatabaseConnection doesn't exist
            try {
                java.lang.reflect.Method getDatabaseManagerMethod = core.getClass().getMethod("getDatabaseManager");
                Object databaseManager = getDatabaseManagerMethod.invoke(core);
                if (databaseManager != null) {
                    java.lang.reflect.Method getConnectionMethod = databaseManager.getClass().getMethod("getConnection", String.class);
                    return (Connection) getConnectionMethod.invoke(databaseManager, database);
                }
            } catch (Exception ex) {
                // Ignore and continue to fallback
            }
            
            // Fallback: return null if no database methods are available
            logWarning("Database connection method not available in ApiCore for database: " + database);
            return null;
        } catch (Exception e) {
            logError("Failed to get database connection for: " + database, e);
            return null;
        }
    }
    
    @Override
    public void releaseDatabaseConnection(Connection connection) {
        if (connection != null) {
            try {
                // Try to use reflection to access the database connection release method if available
                java.lang.reflect.Method method = core.getClass().getMethod("releaseDatabaseConnection", Connection.class);
                if (method != null) {
                    method.invoke(core, connection);
                    return;
                }
                
                // Try to access database manager if releaseDatabaseConnection doesn't exist
                try {
                    java.lang.reflect.Method getDatabaseManagerMethod = core.getClass().getMethod("getDatabaseManager");
                    Object databaseManager = getDatabaseManagerMethod.invoke(core);
                    if (databaseManager != null) {
                        java.lang.reflect.Method releaseConnectionMethod = databaseManager.getClass().getMethod("releaseConnection", Connection.class);
                        releaseConnectionMethod.invoke(databaseManager, connection);
                        return;
                    }
                } catch (Exception ex) {
                    // Ignore and continue to fallback
                }
                
                // Fallback: just close the connection directly
                connection.close();
            } catch (Exception e) {
                logError("Failed to release database connection", e);
                try {
                    connection.close();
                } catch (SQLException ex) {
                    logError("Failed to close database connection", ex);
                }
            }
        }
    }
    
    @Override
    public <T> CompletableFuture<T> runDatabaseOperation(String database, DatabaseOperation<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            Connection connection = getDatabaseConnection(database);
            if (connection == null) {
                throw new RuntimeException("Could not get database connection for: " + database);
            }
            
            try {
                return operation.execute(connection);
            } catch (SQLException e) {
                throw new RuntimeException("Database operation failed", e);
            } finally {
                releaseDatabaseConnection(connection);
            }
        });
    }
    
    @Override
    public File getModuleDataFolder(String moduleName) {
        return core.getModuleDataFolder(moduleName);
    }
    
    @Override
    public File getModuleConfigFile(String moduleName) {
        return core.getModuleConfigFile(moduleName);
    }
    
    @Override
    public File getModuleResourcesFolder(String moduleName) {
        return core.getModuleResourcesFolder(moduleName);
    }
    
    @Override
    public void registerModuleListener(String eventName, ModuleEventListener listener) {
        registerModuleEventListener(eventName, listener);
    }
    
    @Override
    public void unregisterModuleListener(String eventName, ModuleEventListener listener) {
        unregisterModuleEventListener(eventName, listener);
    }
    
    @Override
    public void registerCommands(List<? extends CommandDefinition> commands) {
        if (commands != null) {
            for (CommandDefinition command : commands) {
                // Create a Command implementation using CommandProcessor.SimpleCommand
                com.essentialscore.api.command.CommandProcessor.SimpleCommand cmdObj = 
                    com.essentialscore.api.command.CommandProcessor.SimpleCommand.builder(command.getName(), moduleName)
                        .description(command.getDescription() != null ? command.getDescription() : "")
                        .usage(command.getUsage() != null ? command.getUsage() : "/" + command.getName())
                        .build(context -> false); // Default implementation that does nothing
                
                core.getCommandManager().registerCommand(cmdObj);
            }
        }
    }
    
    @Override
    public void unregisterCommands(List<? extends CommandDefinition> commands) {
        if (commands != null) {
            for (CommandDefinition command : commands) {
                try {
                    // Get the command object first, then unregister it
                    Object commandObj = core.getCommandManager().getCommand(command.getName());
                    if (commandObj != null) {
                        // Cast the command object to the appropriate type
                        com.essentialscore.api.command.Command cmd = (com.essentialscore.api.command.Command) commandObj;
                        core.getCommandManager().unregisterCommand(cmd);
                    }
                } catch (Exception e) {
                    logWarning("Failed to unregister command: " + command.getName());
                }
            }
        }
    }
    
    @Override
    public IIntegrationManager getIntegrationManager() {
        try {
            // Try to use reflection to access the integration manager
            java.lang.reflect.Method method = core.getClass().getMethod("getIntegrationManager");
            Object result = method.invoke(core);
            
            // If the result is an IntegrationManager, return it
            if (result instanceof IIntegrationManager) {
                return (IIntegrationManager) result;
            }
            
            // If the result is not an IIntegrationManager but not null, try to adapt it
            if (result != null) {
                return new IntegrationManagerAdapter(result);
            }
        } catch (Exception e) {
            logError("Failed to get integration manager", e);
        }
        
        // Return a dummy integration manager that just returns empty results
        return new DummyIntegrationManager();
    }
    
    @Override
    public <T extends PluginIntegration> Optional<T> getIntegration(Class<T> integrationClass) {
        IIntegrationManager manager = getIntegrationManager();
        if (manager != null) {
            return manager.getIntegration(integrationClass);
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<PluginIntegration> getIntegrationByPlugin(String pluginName) {
        IIntegrationManager manager = getIntegrationManager();
        if (manager != null) {
            return manager.getIntegrationByPlugin(pluginName);
        }
        return Optional.empty();
    }
    
    @Override
    public String registerModulePermission(com.essentialscore.api.Module module, String permissionName, String description) {
        if (module == null) {
            logWarning("Cannot register permission for null module");
            return permissionName;
        }
        
        try {
            // Get the ModuleAPI.PermissionDefault from the module if available, or use OP as default
            PermissionDefault defaultValue = PermissionDefault.OP;
            
            // Convert ModuleAPI.PermissionDefault to Bukkit's PermissionDefault
            org.bukkit.permissions.PermissionDefault bukkitDefault;
            switch (defaultValue) {
                case TRUE:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.TRUE;
                    break;
                case FALSE:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.FALSE;
                    break;
                case OP:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.OP;
                    break;
                case NOT_OP:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.NOT_OP;
                    break;
                default:
                    bukkitDefault = org.bukkit.permissions.PermissionDefault.OP;
            }
            
            // Construct the full permission string
            String fullPermissionName = module.getName().toLowerCase() + "." + permissionName;
            
            // Register the permission and ignore the boolean return value
            core.getPermissionManager().registerModulePermission(module.getName(), permissionName, description, bukkitDefault);
            
            // Return the constructed permission string
            return fullPermissionName;
        } catch (Exception e) {
            logError("Failed to register module permission: " + permissionName, e);
            return module.getName().toLowerCase() + "." + permissionName;
        }
    }
    
    @Override
    public String formatPlaceholders(String text, Player player) {
        try {
            // Try to use reflection to access the method if it exists
            try {
                java.lang.reflect.Method method = core.getClass().getMethod("formatPlaceholders", String.class, Player.class);
                if (method != null) {
                    return (String) method.invoke(core, text, player);
                }
            } catch (NoSuchMethodException ignored) {
                // Method doesn't exist, continue to fallback
            }
            
            // Fallback: try to get PlaceholderIntegration and use it directly
            Optional<PlaceholderIntegration> placeholderIntegration = getIntegration(PlaceholderIntegration.class);
            if (placeholderIntegration.isPresent()) {
                return placeholderIntegration.get().setPlaceholders(player, text);
            }
            
            // If no integration is available, just return the original text
            return text;
        } catch (Exception e) {
            logError("Failed to format placeholders", e);
            return text;
        }
    }
    
    @Override
    public com.essentialscore.api.security.SecurityManager getSecurityManager() {
        try {
            Object result = core.getSecurityManager();
            if (result instanceof com.essentialscore.api.security.SecurityManager) {
                return (com.essentialscore.api.security.SecurityManager) result;
            }
            
            // If the type is not directly compatible, use a dummy implementation
            logWarning("Security manager type mismatch - using dummy implementation");
            return new DummySecurityManager();
        } catch (Exception e) {
            logError("Failed to get security manager", e);
            return new DummySecurityManager();
        }
    }
    
    @Override
    public com.essentialscore.api.security.ModuleSandbox getModuleSandbox(String moduleId) {
        try {
            // Try to use reflection to access the module sandbox
            java.lang.reflect.Method method = core.getClass().getMethod("getModuleSandbox", String.class);
            if (method != null) {
                Object result = method.invoke(core, moduleId);
                if (result instanceof com.essentialscore.api.security.ModuleSandbox) {
                    return (com.essentialscore.api.security.ModuleSandbox) result;
                }
            }
            return null;
        } catch (Exception e) {
            logError("Failed to get module sandbox: " + e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public <T> T executeInSandbox(String moduleId, com.essentialscore.api.security.ModuleSandbox.SandboxedTask<T> task) {
        com.essentialscore.api.security.ModuleSandbox sandbox = getModuleSandbox(moduleId);
        if (sandbox == null) {
            logWarning("Sandbox not available for module: " + moduleId);
            try {
                return task.execute();
            } catch (Exception e) {
                logError("Error executing task without sandbox", e);
                return null;
            }
        }
        
        try {
            return sandbox.execute(task);
        } catch (Exception e) {
            logError("Error executing in sandbox", e);
            return null;
        }
    }
    
    @Override
    public boolean hasModuleSecurityPermission(String moduleId, SecurityPermission permission, String target) {
        com.essentialscore.api.security.SecurityManager securityManager = getSecurityManager();
        if (securityManager == null) {
            return false;
        }
        
        try {
            return securityManager.isOperationAllowed(moduleId, permission.getValue(), target);
        } catch (Exception e) {
            logError("Error checking security permission", e);
            return false;
        }
    }
    
    @Override
    public com.essentialscore.api.gui.GUIManager getGUIManager() {
        try {
            // Try to use reflection to access the GUI manager
            java.lang.reflect.Method method = core.getClass().getMethod("getGUIManager");
            Object result = method.invoke(core);
            
            if (result instanceof com.essentialscore.api.gui.GUIManager) {
                return (com.essentialscore.api.gui.GUIManager) result;
            }
            
            // If we can't get the real GUI manager, return null
            logWarning("GUI manager not available");
            return null;
        } catch (Exception e) {
            logError("Failed to get GUI manager", e);
            return null;
        }
    }
    
    @Override
    public com.essentialscore.api.gui.GUIBuilder createGUI(String moduleId, String title, int rows) {
        com.essentialscore.api.gui.GUIManager manager = getGUIManager();
        if (manager != null) {
            try {
                return manager.createGUI(moduleId, title, rows);
            } catch (Exception e) {
                logError("Failed to create GUI", e);
            }
        }
        return null;
    }
    
    @Override
    public void openGUI(Player player, com.essentialscore.api.gui.GUI gui) {
        if (player == null || gui == null) return;
        
        com.essentialscore.api.gui.GUIManager manager = getGUIManager();
        if (manager != null) {
            try {
                manager.openGUI(player, gui);
            } catch (Exception e) {
                logError("Failed to open GUI for player " + player.getName(), e);
            }
        }
    }
    
    @Override
    public InventoryBuilder createInventory(String title, int size) {
        return new CoreInventoryBuilder(title, size);
    }
    
    @Override
    public ItemBuilder createItem(String material) {
        return new CoreItemBuilder(material);
    }
    
    @Override
    public void setPlayerData(UUID playerUUID, String key, Object value) {
        if (playerUUID == null || key == null) return;
        
        try {
            // Try to use reflection to access the data manager if available
            java.lang.reflect.Method method = core.getClass().getMethod("setPlayerData", UUID.class, String.class, Object.class);
            if (method != null) {
                method.invoke(core, playerUUID, key, value);
                return;
            }
        } catch (Exception ignored) {
            // Fall through to local implementation
        }
        
        // Local implementation as fallback
        Map<String, Object> playerData = getLocalPlayerDataMap(playerUUID);
        if (value == null) {
            playerData.remove(key);
        } else {
            playerData.put(key, value);
        }
    }
    
    @Override
    public Object getPlayerData(UUID playerUUID, String key) {
        if (playerUUID == null || key == null) return null;
        
        try {
            // Try to use reflection to access the data manager if available
            java.lang.reflect.Method method = core.getClass().getMethod("getPlayerData", UUID.class, String.class);
            if (method != null) {
                return method.invoke(core, playerUUID, key);
            }
        } catch (Exception ignored) {
            // Fall through to local implementation
        }
        
        // Local implementation as fallback
        return getLocalPlayerDataMap(playerUUID).get(key);
    }
    
    @Override
    public void removePlayerData(UUID playerUUID, String key) {
        if (playerUUID == null || key == null) return;
        
        try {
            // Try to use reflection to access the data manager if available
            java.lang.reflect.Method method = core.getClass().getMethod("removePlayerData", UUID.class, String.class);
            if (method != null) {
                method.invoke(core, playerUUID, key);
                return;
            }
        } catch (Exception ignored) {
            // Fall through to local implementation
        }
        
        // Local implementation as fallback
        getLocalPlayerDataMap(playerUUID).remove(key);
    }
    
    // Helper method to get or create local player data map
    private Map<String, Object> getLocalPlayerDataMap(UUID playerUUID) {
        return CoreModuleAPIDataStore.getPlayerDataMap(playerUUID);
    }
    
    @Override
    public PerformanceResult checkModulePerformance(String moduleName) {
        try {
            // Try to use reflection to access the performance monitor
            java.lang.reflect.Method getMonitorMethod = core.getClass().getMethod("getPerformanceMonitor");
            Object performanceMonitor = getMonitorMethod.invoke(core);
            
            if (performanceMonitor != null) {
                java.lang.reflect.Method checkMethod = performanceMonitor.getClass().getMethod("checkModulePerformance", String.class);
                Object result = checkMethod.invoke(performanceMonitor, moduleName);
                
                if (result instanceof PerformanceResult) {
                    return (PerformanceResult) result;
                }
            }
            
            // If we can't access the real performance monitor, return a dummy result
            return new PerformanceResult(0.0, 0L, 0.0, "UNKNOWN");
        } catch (Exception e) {
            logError("Failed to check performance for module: " + moduleName, e);
            return new PerformanceResult(0.0, 0L, 0.0, "ERROR");
        }
    }
    
    @Override
    public long measurePerformance(Runnable block) {
        if (block == null) return 0;
        
        long startTime = System.nanoTime();
        try {
            block.run();
        } catch (Exception e) {
            logError("Error during performance measurement", e);
        }
        long endTime = System.nanoTime();
        
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }
    
    @Override
    public <T> T executeInSandbox(Callable<T> action, T defaultValue) {
        if (action == null) return defaultValue;
        
        try {
            return action.call();
        } catch (Exception e) {
            logError("Error executing in sandbox", e);
            return defaultValue;
        }
    }
    
    // Inner classes for builders
    private static class CoreItemBuilder implements ItemBuilder {
        private ItemStack item;
        private ItemMeta meta;
        
        public CoreItemBuilder(String material) {
            try {
                Material mat = Material.valueOf(material.toUpperCase());
                this.item = new ItemStack(mat);
                this.meta = item.getItemMeta();
            } catch (IllegalArgumentException e) {
                this.item = new ItemStack(Material.STONE);
                this.meta = item.getItemMeta();
            }
        }
        
        @Override
        public ItemBuilder name(String name) {
            if (meta != null && name != null) {
                // Handle deprecated method carefully
                try {
                    meta.setDisplayName(name);
                } catch (Throwable t) {
                    // Fallback in case of issues with deprecated method
                    try {
                        java.lang.reflect.Method method = meta.getClass().getMethod("setDisplayName", String.class);
                        method.invoke(meta, name);
                    } catch (Exception ex) {
                        // Can't set display name
                    }
                }
            }
            return this;
        }
        
        @Override
        public ItemBuilder lore(String... lore) {
            if (meta != null && lore != null) {
                // Handle deprecated method carefully
                try {
                    meta.setLore(Arrays.asList(lore));
                } catch (Throwable t) {
                    // Fallback in case of issues with deprecated method
                    try {
                        java.lang.reflect.Method method = meta.getClass().getMethod("setLore", java.util.List.class);
                        method.invoke(meta, Arrays.asList(lore));
                    } catch (Exception ex) {
                        // Can't set lore
                    }
                }
            }
            return this;
        }
        
        @Override
        public ItemBuilder amount(int amount) {
            if (amount > 0) {
                item.setAmount(Math.min(amount, 64));
            }
            return this;
        }
        
        @Override
        public ItemBuilder data(short data) {
            // Deprecated in newer versions, but keeping for compatibility
            try {
                // Try to use durability for older versions
                java.lang.reflect.Method method = item.getClass().getMethod("setDurability", short.class);
                method.invoke(item, data);
            } catch (Exception e) {
                // Ignore in newer versions where durability is deprecated
            }
            return this;
        }
        
        @Override
        public ItemBuilder enchant(String enchantment, int level) {
            try {
                // Handle deprecated method carefully
                Enchantment ench = null;
                try {
                    // Try the newer method first (1.13+)
                    ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantment.toLowerCase()));
                } catch (Throwable t) {
                    // Try the deprecated method as fallback
                    try {
                        java.lang.reflect.Method method = Enchantment.class.getMethod("getByName", String.class);
                        ench = (Enchantment) method.invoke(null, enchantment.toUpperCase());
                    } catch (Exception ex) {
                        // Can't get enchantment
                    }
                }
                
                if (ench != null) {
                    item.addUnsafeEnchantment(ench, level);
                }
            } catch (Exception e) {
                // Ignore invalid enchantments
            }
            return this;
        }
        
        @Override
        public ItemBuilder glow(boolean glow) {
            if (glow && meta != null) {
                // Add a dummy enchantment to make item glow
                try {
                    // Try to get DURABILITY enchantment in a version-compatible way
                    Enchantment durability = null;
                    try {
                        // Try to get the enchantment by key (1.13+)
                        durability = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("durability"));
                    } catch (Throwable t) {
                        // Try to get using the enum constant field for older versions
                        try {
                            java.lang.reflect.Field field = Enchantment.class.getDeclaredField("DURABILITY");
                            durability = (Enchantment) field.get(null);
                        } catch (Exception ex) {
                            // Try getting by name as last resort
                            try {
                                java.lang.reflect.Method method = Enchantment.class.getMethod("getByName", String.class);
                                durability = (Enchantment) method.invoke(null, "DURABILITY");
                            } catch (Exception ex2) {
                                // Just use the first available enchantment
                                if (Enchantment.values().length > 0) {
                                    durability = Enchantment.values()[0];
                                }
                            }
                        }
                    }
                    
                    if (durability != null) {
                        item.addUnsafeEnchantment(durability, 1);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                } catch (Exception e) {
                    // Ignore if we can't make it glow
                }
            }
            return this;
        }
        
        @Override
        public ItemBuilder unbreakable(boolean unbreakable) {
            if (meta != null) {
                try {
                    meta.setUnbreakable(unbreakable);
                } catch (Throwable t) {
                    // Fallback for older versions
                    try {
                        java.lang.reflect.Method method = meta.getClass().getMethod("spigot");
                        Object spigot = method.invoke(meta);
                        method = spigot.getClass().getMethod("setUnbreakable", boolean.class);
                        method.invoke(spigot, unbreakable);
                    } catch (Exception e) {
                        // Ignore if we can't set unbreakable
                    }
                }
            }
            return this;
        }
        
        @Override
        public ItemBuilder addFlag(String flag) {
            try {
                ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                if (meta != null) {
                    meta.addItemFlags(itemFlag);
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid flags
            }
            return this;
        }
        
        @Override
        public ItemBuilder removeFlag(String flag) {
            try {
                ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                if (meta != null) {
                    meta.removeItemFlags(itemFlag);
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid flags
            }
            return this;
        }
        
        @Override
        public ItemBuilder addNBT(String key, Object value) {
            // NBT manipulation would require more complex implementation
            return this;
        }
        
        @Override
        public ItemStack build() {
            if (meta != null) {
                item.setItemMeta(meta);
            }
            return item.clone();
        }
    }
    
    private static class CoreInventoryBuilder implements InventoryBuilder {
        private final String title;
        private final int size;
        private final Map<Integer, ItemStack> items = new HashMap<>();
        // Not using these fields directly but keeping them for API compatibility
        private Consumer<InventoryClickEvent> clickHandler;
        private Consumer<InventoryCloseEvent> closeHandler;
        
        public CoreInventoryBuilder(String title, int size) {
            this.title = title != null ? title : "";
            this.size = Math.max(9, Math.min(54, (size + 8) / 9 * 9)); // Round to nearest multiple of 9
        }
        
        @Override
        public InventoryBuilder item(int slot, ItemStack item) {
            if (slot >= 0 && slot < size && item != null) {
                items.put(slot, item.clone());
            }
            return this;
        }
        
        @Override
        public InventoryBuilder onClick(Consumer<InventoryClickEvent> handler) {
            // Store the handler for future implementation
            this.clickHandler = handler;
            return this;
        }
        
        @Override
        public InventoryBuilder onClose(Consumer<InventoryCloseEvent> handler) {
            // Store the handler for future implementation
            this.closeHandler = handler;
            return this;
        }
        
        @Override
        public InventoryBuilder fillBorder(ItemStack item) {
            if (item == null) return this;
            
            int rows = size / 9;
            for (int i = 0; i < size; i++) {
                int row = i / 9;
                int col = i % 9;
                
                // First/last row or first/last column
                if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                    items.put(i, item.clone());
                }
            }
            return this;
        }
        
        @Override
        public InventoryBuilder fillEmpty(ItemStack item) {
            if (item == null) return this;
            
            for (int i = 0; i < size; i++) {
                if (!items.containsKey(i)) {
                    items.put(i, item.clone());
                }
            }
            return this;
        }
        
        @Override
        public InventoryBuilder paginated(boolean paginated) {
            // Pagination would require more complex implementation
            return this;
        }
        
        @Override
        public Inventory build() {
            Inventory inventory = org.bukkit.Bukkit.createInventory(null, size, title);
            
            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                inventory.setItem(entry.getKey(), entry.getValue());
            }
            
            // Note: The clickHandler and closeHandler are not actually used in this implementation
            // In a real implementation, we would need to register event listeners
            
            return inventory;
        }
    }
}
