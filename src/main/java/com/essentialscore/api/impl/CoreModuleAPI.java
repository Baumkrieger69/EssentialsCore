package com.essentialscore.api.impl;

import com.essentialscore.ApiCore;
import com.essentialscore.api.*;
import com.essentialscore.api.integration.*;
import com.essentialscore.api.security.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Implementation of the ModuleAPI interface that bridges between the ApiCore and modules.
 * This class adapts calls from modules to the actual ApiCore implementation.
 */
public class CoreModuleAPI implements ModuleAPI {
    private final ApiCore core;
    private final String moduleName;
    private final Map<String, ModuleEventListener> eventListeners;
    private final ReentrantReadWriteLock eventLock;
    
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
        FileConfiguration config = core.getModuleConfig(moduleName);
        if (config == null) {
            logWarning("Configuration for module " + moduleName + " could not be loaded. Using empty configuration.");
            return new YamlConfiguration();
        }
        return config;
    }
    
    @Override
    public void saveConfig() {
        try {
            core.saveModuleConfig(moduleName);
        } catch (Exception e) {
            logError("Failed to save configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void reloadConfig() {
        try {
            core.reloadModuleConfig(moduleName);
        } catch (Exception e) {
            logError("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
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
    public void registerModuleListener(String eventName, ModuleEventListener listener) {
        if (eventName == null || eventName.isEmpty()) throw new IllegalArgumentException("Event name cannot be null or empty");
        if (listener == null) throw new IllegalArgumentException("Event listener cannot be null");
        
        eventLock.writeLock().lock();
        try {
            eventListeners.put(eventName, listener);
            core.registerModuleListener(eventName, listener);
        } finally {
            eventLock.writeLock().unlock();
        }
    }
    
    @Override
    public void unregisterModuleListener(String eventName, ModuleEventListener listener) {
        if (eventName == null || eventName.isEmpty()) return;
        if (listener == null) return;
        
        eventLock.writeLock().lock();
        try {
            if (eventListeners.remove(eventName, listener)) {
                core.unregisterModuleListener(eventName, listener);
            }
        } finally {
            eventLock.writeLock().unlock();
        }
    }
    
    @Override
    public <T> T executeInSandbox(String moduleId, ModuleSandbox.SandboxedTask<T> task) {
        if (moduleId == null || moduleId.isEmpty()) {
            throw new IllegalArgumentException("Module ID cannot be null or empty");
        }
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        
        ModuleSandbox sandbox = getModuleSandbox(moduleId);
        if (sandbox == null || !sandbox.isSandboxEnabled()) {
            logWarning("Sandbox disabled or not available, executing task without isolation");
            try {
                return task.execute();
            } catch (Exception e) {
                logError("Error executing task: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        
        try {
            return sandbox.executeTask(moduleId, task);
        } catch (SecurityException e) {
            logError("Security violation in sandbox execution: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logError("Error executing in sandbox: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void logInfo(String message) {
        if (message == null) return;
        core.getLogger().info("[" + moduleName + "] " + message);
    }
    
    @Override
    public void logWarning(String message) {
        if (message == null) return;
        core.getLogger().warning("[" + moduleName + "] " + message);
    }
    
    @Override
    public void logError(String message) {
        if (message == null) return;
        core.getLogger().severe("[" + moduleName + "] " + message);
    }
    
    @Override
    public void logDebug(String message) {
        if (message == null || !core.isDebugMode()) return;
        core.getLogger().info("[DEBUG: " + moduleName + "] " + message);
    }
    
    // Neue Hilfsmethode für Builder-Validierung
    private void validateBuilderParameters(String title, int size) {
        if (title == null) throw new IllegalArgumentException("Title cannot be null");
        if (size < 0 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Invalid inventory size: " + size + ". Must be a multiple of 9 between 0 and 54");
        }
    }
    
    @Override
    public void registerCommands(List<? extends CommandDefinition> commands) {
        core.getCommandManager().registerModuleCommands(moduleName, commands);
    }
    
    @Override
    public void unregisterCommands(List<? extends CommandDefinition> commands) {
        core.getCommandManager().unregisterModuleCommands(moduleName, commands);
    }
    
    @Override
    public IntegrationManager getIntegrationManager() {
        return core.getIntegrationManager();
    }
    
    @Override
    public <T extends PluginIntegration> Optional<T> getIntegration(Class<T> integrationClass) {
        return core.getIntegrationManager().getIntegration(integrationClass);
    }
    
    @Override
    public Optional<PluginIntegration> getIntegrationByPlugin(String pluginName) {
        return core.getIntegrationManager().getIntegrationByPlugin(pluginName);
    }
    
    @Override
    public String registerModulePermission(Module module, String permissionName, String description) {
        return core.getPermissionManager().registerModulePermission(moduleName, permissionName, description);
    }
    
    @Override
    public String formatPlaceholders(String text, Player player) {
        return core.getPlaceholderManager().formatPlaceholders(text, player);
    }
    
    @Override
    public SecurityManager getSecurityManager() {
        SecurityManager securityManager = core.getSecurityManager();
        if (securityManager == null) {
            logWarning("Security manager is not available");
            return null;
        }
        return securityManager;
    }
    
    @Override
    public ModuleSandbox getModuleSandbox(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) {
            throw new IllegalArgumentException("Module ID cannot be null or empty");
        }
        
        ModuleSandbox sandbox = core.getModuleSandbox();
        if (sandbox == null) {
            logWarning("Sandbox is not available for module: " + moduleId);
            return null;
        }
        return sandbox;
    }
    
    /**
     * Prüft, ob ein Modul eine bestimmte Sicherheitsberechtigung hat.
     */
    @Override 
    public boolean hasModuleSecurityPermission(String moduleId, SecurityPermission permission, String target) {
        if (moduleId == null || permission == null) {
            return false;
        }
        
        SecurityManager securityManager = getSecurityManager();
        if (securityManager == null) {
            logWarning("Security manager not available, denying permission: " + permission);
            return false;
        }
        
        try {
            return securityManager.hasPermission(moduleId, permission, target);
        } catch (Exception e) {
            logError("Error checking security permission: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public <T> T executeInSandbox(Callable<T> action, T defaultValue) {
        ModuleSandbox sandbox = core.getModuleSandbox();
        if (sandbox != null && sandbox.isSandboxEnabled()) {
            return sandbox.executeModuleFunction(moduleName, this, action, defaultValue);
        } else {
            try {
                return action.call();
            } catch (Exception e) {
                core.getLogger().warning("[" + moduleName + "] Error executing in sandbox: " + e.getMessage());
                return defaultValue;
            }
        }
    }
    
    @Override
    public long measurePerformance(Runnable block) {
        if (block == null) {
            throw new IllegalArgumentException("Block cannot be null");
        }
        
        long startTime = System.nanoTime();
        try {
            block.run();
            return (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
        } catch (Exception e) {
            logError("Error measuring performance: " + e.getMessage());
            return -1;
        }
    }
    
    @Override
    public PerformanceResult analyzePerformance(Runnable block) {
        if (block == null) {
            throw new IllegalArgumentException("Block cannot be null");
        }
        
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.nanoTime();
        double cpuTime = 0;
        
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            if (threadBean.isCurrentThreadCpuTimeSupported()) {
                cpuTime = threadBean.getCurrentThreadCpuTime();
            }
            
            block.run();
            
            long endTime = System.nanoTime();
            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            double executionTime = (endTime - startTime) / 1_000_000.0; // ms
            long memoryUsed = endMemory - startMemory;
            double cpuUsage = 0;
            
            if (threadBean.isCurrentThreadCpuTimeSupported()) {
                cpuUsage = (threadBean.getCurrentThreadCpuTime() - cpuTime) / 1_000_000.0;
            }
            
            String status = determinePerformanceStatus(executionTime, memoryUsed, cpuUsage);
            return new PerformanceResult(cpuUsage, memoryUsed, executionTime, status);
            
        } catch (Exception e) {
            logError("Error analyzing performance: " + e.getMessage());
            return new PerformanceResult(0, 0, 0, "ERROR");
        }
    }
    
    private String determinePerformanceStatus(double executionTime, long memoryUsed, double cpuUsage) {
        // Schwellenwerte für Warnungen und kritische Zustände
        final double CRITICAL_EXECUTION_TIME = 1000.0; // 1 Sekunde
        final double WARNING_EXECUTION_TIME = 500.0;   // 500ms
        final long CRITICAL_MEMORY = 50 * 1024 * 1024; // 50MB
        final long WARNING_MEMORY = 10 * 1024 * 1024;  // 10MB
        final double CRITICAL_CPU = 90.0; // 90%
        final double WARNING_CPU = 70.0;  // 70%
        
        if (executionTime > CRITICAL_EXECUTION_TIME ||
            memoryUsed > CRITICAL_MEMORY ||
            cpuUsage > CRITICAL_CPU) {
            return "CRITICAL";
        }
        
        if (executionTime > WARNING_EXECUTION_TIME ||
            memoryUsed > WARNING_MEMORY ||
            cpuUsage > WARNING_CPU) {
            return "WARNING";
        }
        
        return "OK";
    }
    
    @Override
    public GUIBuilder createGUI(String moduleId, String title, int rows) {
        return new DummyGUIBuilder();
    }
    
    @Override
    public void openGUI(Player player, GUI gui) {
        player.openInventory(gui.getInventory());
    }
    
    @Override
    public InventoryBuilder createInventory(String title, int size) {
        return new DummyInventoryBuilder(title, size);
    }
    
    @Override
    public ItemBuilder createItem(String material) {
        return new DummyItemBuilder();
    }
    
    // Dummy-Implementierungen der Builder-Klassen
    private class DummyGUIBuilder implements GUIBuilder {
        @Override
        public GUI build() {
            return null;
        }
    }
    
    private class DummyInventoryBuilder implements ModuleAPI.InventoryBuilder {
        private final String title;
        private final int size;
        private final Map<Integer, ItemStack> items = new HashMap<>();
        private Consumer<InventoryClickEvent> clickHandler;
        private Consumer<InventoryCloseEvent> closeHandler;
        
        public DummyInventoryBuilder(String title, int size) {
            validateBuilderParameters(title, size);
            this.title = title;
            this.size = size;
        }
        
        @Override
        public ModuleAPI.InventoryBuilder item(int slot, ItemStack item) {
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException("Invalid slot: " + slot);
            }
            items.put(slot, item);
            return this;
        }
        
        @Override
        public ModuleAPI.InventoryBuilder onClick(Consumer<InventoryClickEvent> handler) {
            this.clickHandler = handler;
            return this;
        }
        
        @Override
        public ModuleAPI.InventoryBuilder onClose(Consumer<InventoryCloseEvent> handler) {
            this.closeHandler = handler;
            return this;
        }
        
        @Override
        public ModuleAPI.InventoryBuilder fillBorder(ItemStack item) {
            for (int i = 0; i < size; i++) {
                if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                    items.put(i, item);
                }
            }
            return this;
        }
        
        @Override
        public ModuleAPI.InventoryBuilder fillEmpty(ItemStack item) {
            for (int i = 0; i < size; i++) {
                if (!items.containsKey(i)) {
                    items.put(i, item);
                }
            }
            return this;
        }
        
        @Override
        public ModuleAPI.InventoryBuilder paginated(boolean paginated) {
            // Pagination not implemented in dummy builder
            return this;
        }
        
        @Override
        public Inventory build() {
            Inventory inv = core.getServer().createInventory(null, size, title);
            items.forEach((slot, item) -> inv.setItem(slot, item));
            
            // Register click handler if provided
            if (clickHandler != null) {
                core.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onInventoryClick(InventoryClickEvent event) {
                        if (event.getInventory().equals(inv)) {
                            clickHandler.accept(event);
                        }
                    }
                }, core);
            }
            
            // Register close handler if provided
            if (closeHandler != null) {
                core.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onInventoryClose(InventoryCloseEvent event) {
                        if (event.getInventory().equals(inv)) {
                            closeHandler.accept(event);
                        }
                    }
                }, core);
            }
            
            return inv;
        }
    }
    
    private class DummyItemBuilder implements ModuleAPI.ItemBuilder {
        private final ItemStack item;
        private final ItemMeta meta;
        
        public DummyItemBuilder(String material) {
            if (material == null || material.isEmpty()) {
                throw new IllegalArgumentException("Material cannot be null or empty");
            }
            
            Material mat = Material.matchMaterial(material);
            if (mat == null) {
                throw new IllegalArgumentException("Invalid material: " + material);
            }
            
            this.item = new ItemStack(mat);
            this.meta = item.getItemMeta();
        }
        
        @Override
        public ModuleAPI.ItemBuilder name(String name) {
            if (meta != null && name != null) {
                meta.setDisplayName(name);
            }
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder lore(String... lore) {
            if (meta != null && lore != null) {
                meta.setLore(Arrays.asList(lore));
            }
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder amount(int amount) {
            if (amount > 0) {
                item.setAmount(amount);
            }
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder data(short data) {
            item.setDurability(data);
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder enchant(String enchantment, int level) {
            if (meta != null && enchantment != null) {
                try {
                    Enchantment ench = Enchantment.getByName(enchantment.toUpperCase());
                    if (ench != null) {
                        meta.addEnchant(ench, level, true);
                    }
                } catch (Exception e) {
                    core.getLogger().warning("Failed to add enchantment: " + enchantment);
                }
            }
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder glow(boolean glow) {
            if (meta != null) {
                if (glow) {
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    meta.removeEnchant(Enchantment.DURABILITY);
                    meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder unbreakable(boolean unbreakable) {
            if (meta != null) {
                meta.setUnbreakable(unbreakable);
            }
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder addFlag(String flag) {
            if (meta != null && flag != null) {
                try {
                    ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                    meta.addItemFlags(itemFlag);
                } catch (Exception e) {
                    core.getLogger().warning("Invalid item flag: " + flag);
                }
            }
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder removeFlag(String flag) {
            if (meta != null && flag != null) {
                try {
                    ItemFlag itemFlag = ItemFlag.valueOf(flag.toUpperCase());
                    meta.removeItemFlags(itemFlag);
                } catch (Exception e) {
                    core.getLogger().warning("Invalid item flag: " + flag);
                }
            }
            return this;
        }
        
        @Override
        public ModuleAPI.ItemBuilder addNBT(String key, Object value) {
            // NBT-Daten-Unterstützung ist in der Dummy-Implementierung nicht verfügbar
            logWarning("NBT data support not available in dummy implementation");
            return this;
        }
        
        @Override
        public ItemStack build() {
            if (meta != null) {
                item.setItemMeta(meta);
            }
            return item;
        }
    }
}