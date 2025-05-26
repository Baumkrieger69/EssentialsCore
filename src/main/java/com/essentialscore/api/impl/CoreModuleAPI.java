package com.essentialscore.api.impl;

import com.essentialscore.ApiCore;
<<<<<<< HEAD
import com.essentialscore.ModuleSandbox;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.api.ModuleEventListener;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
=======
import com.essentialscore.DynamicCommand;
import com.essentialscore.api.CommandDefinition;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.api.ModuleEventListener;
import org.bukkit.entity.Player;
>>>>>>> 1cd13da (Das ist Dumm)
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
<<<<<<< HEAD
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
=======
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
>>>>>>> 1cd13da (Das ist Dumm)
import java.util.logging.Level;

/**
 * Implementation of the ModuleAPI interface that bridges between the ApiCore and modules.
 * This class adapts calls from modules to the actual ApiCore implementation.
 */
public class CoreModuleAPI implements ModuleAPI {
    private final ApiCore core;
    private final String moduleName;
    
    public CoreModuleAPI(ApiCore core, String moduleName) {
        this.core = core;
        this.moduleName = moduleName;
    }
    
    @Override
<<<<<<< HEAD
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
        long start = System.currentTimeMillis();
        block.run();
        return System.currentTimeMillis() - start;
    }
    
    @Override
    public PerformanceResult checkModulePerformance(String moduleName) {
        return new PerformanceResult(0.0, 0L, 0.0, "OK");
    }
    
    @Override
    public void removePlayerData(UUID playerUUID, String key) {
        // Dummy-Implementierung
    }
    
    @Override
    public Object getPlayerData(UUID playerUUID, String key) {
        return null;
    }
    
    @Override
    public void setPlayerData(UUID playerUUID, String key, Object value) {
        // Dummy-Implementierung
    }
    
    @Override
    public ItemBuilder createItem(String material) {
        return new DummyItemBuilder();
    }
    
    @Override
    public InventoryBuilder createInventory(String title, int size) {
        return new DummyInventoryBuilder();
    }
    
    @Override
    public <T> CompletableFuture<T> runDatabaseOperation(String database, DatabaseOperation<T> operation) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Database operations not implemented"));
=======
    public Plugin getPlugin() {
        return core;
    }
    
    @Override
    public File getModuleDataFolder(String moduleName) {
        return core.getModuleDataFolder(moduleName != null ? moduleName : this.moduleName);
    }
    
    @Override
    public File getModuleConfigFile(String moduleName) {
        return core.getModuleConfigFile(moduleName != null ? moduleName : this.moduleName);
    }
    
    @Override
    public File getModuleResourcesFolder(String moduleName) {
        return core.getModuleResourcesFolder(moduleName != null ? moduleName : this.moduleName);
    }
    
    @Override
    public void registerModuleListener(String eventName, ModuleEventListener listener) {
        // We can now pass the listener directly without casting
        core.registerModuleListener(eventName, listener);
    }
    
    @Override
    public void unregisterModuleListener(String eventName, ModuleEventListener listener) {
        // We can now pass the listener directly without casting
        core.unregisterModuleListener(eventName, listener);
    }
    
    @Override
    public void fireModuleEvent(String eventName, Map<String, Object> data) {
        core.fireModuleEvent(eventName, data);
    }
    
    @Override
    public void registerCommands(List<? extends CommandDefinition> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        List<DynamicCommand> coreCommands = new ArrayList<>(commands.size());
        
        for (CommandDefinition command : commands) {
            DynamicCommand dynamicCommand = new DynamicCommand(
                command.getName(),
                command.getDescription(),
                command.getUsage(),
                command.getAliases(),
                moduleName,
                command.getPermission(),
                core
            );
            
            // Add tab completion options if available
            for (int i = 0; i < 10; i++) { // Assuming max 10 arguments
                List<String> options = command.getTabCompletionOptions(i);
                if (options != null && !options.isEmpty()) {
                    dynamicCommand.addTabCompletionOptions(i, options);
                }
            }
            
            coreCommands.add(dynamicCommand);
        }
        
        core.registerCommands(coreCommands);
    }
    
    @Override
    public void unregisterCommands(List<? extends CommandDefinition> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        List<DynamicCommand> coreCommands = new ArrayList<>(commands.size());
        
        for (CommandDefinition command : commands) {
            DynamicCommand dynamicCommand = new DynamicCommand(
                command.getName(),
                command.getDescription(),
                command.getUsage(),
                command.getAliases(),
                moduleName,
                command.getPermission(),
                core
            );
            coreCommands.add(dynamicCommand);
        }
        
        core.unregisterCommands(coreCommands);
    }
    
    @Override
    public boolean hasPermission(Player player, String permission) {
        return core.getPermissionManager().hasPermission(player, permission);
    }
    
    @Override
    public void setSharedData(String key, Object value) {
        // Use a prefix for module-specific data to avoid collisions
        String prefixedKey = moduleName + ":" + key;
        core.setSharedData(prefixedKey, value);
    }
    
    @Override
    public Object getSharedData(String key) {
        // Check for module-specific data first
        String prefixedKey = moduleName + ":" + key;
        Object data = core.getSharedData(prefixedKey);
        
        // If not found, try the global key (for cross-module shared data)
        if (data == null) {
            data = core.getSharedData(key);
        }
        
        return data;
    }
    
    @Override
    public String formatHex(String message) {
        return core.formatHex(message);
    }
    
    @Override
    public <T> CompletableFuture<T> runAsync(java.util.function.Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            try {
                T result = task.get();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
                core.getLogger().log(Level.WARNING, "Error in async task for module " + moduleName, e);
            }
        });
        
>>>>>>> 1cd13da (Das ist Dumm)
        return future;
    }
    
    @Override
<<<<<<< HEAD
    public void releaseDatabaseConnection(Connection connection) {
        // Dummy-Implementierung
    }
    
    @Override
    public Connection getDatabaseConnection(String database) {
        return null;
    }
    
    @Override
    public Plugin getPlugin() {
        return core;
    }

    @Override
    public boolean isDebugMode() {
        return false;
    }

=======
    public void runAsync(Runnable task) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            try {
                task.run();
            } catch (Exception e) {
                core.getLogger().log(Level.WARNING, "Error in async task for module " + moduleName, e);
            }
        });
    }
    
    @Override
    public int scheduleTask(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = core.getServer().getScheduler().runTaskLater(core, task, delayTicks);
        return bukkitTask.getTaskId();
    }
    
    @Override
    public int scheduleRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = core.getServer().getScheduler().runTaskTimer(core, task, delayTicks, periodTicks);
        return bukkitTask.getTaskId();
    }
    
    @Override
    public void cancelTask(int taskId) {
        core.getServer().getScheduler().cancelTask(taskId);
    }
    
>>>>>>> 1cd13da (Das ist Dumm)
    @Override
    public void logInfo(String message) {
        core.getLogger().info("[" + moduleName + "] " + message);
    }
<<<<<<< HEAD

=======
    
>>>>>>> 1cd13da (Das ist Dumm)
    @Override
    public void logWarning(String message) {
        core.getLogger().warning("[" + moduleName + "] " + message);
    }
<<<<<<< HEAD

    @Override
    public void logError(String message, Throwable throwable) {
        core.getLogger().severe("[" + moduleName + "] " + message + ": " + (throwable != null ? throwable.getMessage() : "null"));
=======
    
    @Override
    public void logError(String message, Throwable throwable) {
        if (throwable != null) {
            core.getLogger().log(Level.SEVERE, "[" + moduleName + "] " + message, throwable);
        } else {
            core.getLogger().severe("[" + moduleName + "] " + message);
        }
>>>>>>> 1cd13da (Das ist Dumm)
    }
    
    @Override
    public void logDebug(String message) {
<<<<<<< HEAD
        if (isDebugMode()) {
=======
        if (core.isDebugMode()) {
>>>>>>> 1cd13da (Das ist Dumm)
            core.getLogger().info("[" + moduleName + "] [DEBUG] " + message);
        }
    }
    
    @Override
<<<<<<< HEAD
    public void log(LogLevel level, String message) {
        switch (level) {
            case INFO:
                logInfo(message);
                break;
            case WARNING:
                logWarning(message);
                break;
            case ERROR:
                logError(message, null);
                break;
            case DEBUG:
                logDebug(message);
                break;
            default:
                logInfo(message);
        }
    }
    
    @Override
    public void log(String message) {
        logInfo(message);
    }

    // Die korrekte Signatur aus dem ModuleAPI-Interface
    @Override
    public boolean registerCommand(String command, String description, String usage, CommandExecutor executor, TabCompleter tabCompleter, String permission) {
        // Dummy-Implementierung
        return false;
    }
    
    // Diese Methode ist wahrscheinlich nicht Teil des Interfaces
    public void registerCommand(String name, Object definition) {
        // Dummy-Implementierung
    }

    @Override
    public void registerListener(Listener listener) {
        // Dummy-Implementierung
    }

    @Override
    public void registerPermission(String permission, String description, PermissionDefault defaultValue) {
        // Dummy-Implementierung
    }
    
    @Override
    public boolean hasPermission(Player player, String permission) {
        return player != null && player.hasPermission(permission);
    }

    @Override
    public void sendMessage(CommandSender target, String message) {
        if (target != null) {
            target.sendMessage(message);
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return null;
    }

    @Override
    public void saveConfig() {
        // Dummy-Implementierung
    }

    @Override
    public void reloadConfig() {
        // Dummy-Implementierung
    }

    // Diese Methode ist in der Linterfehlermeldung als nicht überschreibend markiert
    // Wir behalten sie, aber es könnte sein, dass sie in der Interface-Definition anders benannt ist
    public String formatHex(String message) {
        return message;
    }

    @Override
    public String formatColor(String message) {
        return message;
    }

    @Override
    public BukkitTask runTask(Runnable runnable) {
        return core.getServer().getScheduler().runTask(core, runnable);
    }

    @Override
    public BukkitTask runTaskAsync(Runnable runnable) {
        return core.getServer().getScheduler().runTaskAsynchronously(core, runnable);
    }

    @Override
    public BukkitTask runTaskLater(Runnable runnable, long delay) {
        return core.getServer().getScheduler().runTaskLater(core, runnable, delay);
    }

    @Override
    public BukkitTask runTaskTimer(Runnable runnable, long delay, long period) {
        return core.getServer().getScheduler().runTaskTimer(core, runnable, delay, period);
    }

    @Override
    public File getDataFolder() {
        return new File(core.getDataFolder(), "modules/" + moduleName);
    }

    @Override
    public File getResourcesFolder() {
        return new File(getDataFolder(), "resources");
    }

    @Override
    public int extractResources(String directory, boolean replace) {
        return 0;
    }

    @Override
    public boolean extractResource(String resourcePath, boolean replace) {
        return false;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public List<String> getLoadedModules() {
        return new ArrayList<>();
    }

    @Override
    public boolean isModuleLoaded(String moduleName) {
        return false;
    }

    @Override
    public Object getModuleData(String key) {
        return null;
    }
    
    @Override
    public void setModuleData(String key, Object value) {
        // Dummy-Implementierung
    }

    @Override
    public void setSharedData(String key, Object value) {
        // Dummy-Implementierung
    }
    
    @Override
    public Object getSharedData(String key) {
        return null;
    }

    // Diese Methode ist in der Linterfehlermeldung als nicht überschreibend markiert
    // Wir behalten sie, aber es könnte sein, dass sie in der Interface-Definition anders benannt ist
    public Map<String, Object> getSharedData() {
        return Collections.emptyMap();
    }
    
    @Override
    public void fireModuleEvent(String eventName, Map<String, Object> data) {
        // Dummy-Implementierung
    }

    @Override
    public void registerModuleEventListener(String eventName, ModuleEventListener listener) {
        // Dummy-Implementierung
    }

    @Override
    public void unregisterModuleEventListener(String eventName, ModuleEventListener listener) {
        // Dummy-Implementierung
    }

    // Innere Dummy-Klassen für Builder
    
    private static class DummyItemBuilder implements ItemBuilder {
        @Override
        public ItemBuilder name(String name) {
            return this;
        }
        
        @Override
        public ItemBuilder lore(String... lore) {
            return this;
        }
        
        @Override
        public ItemBuilder amount(int amount) {
            return this;
        }
        
        @Override
        public ItemBuilder data(short data) {
            return this;
        }
        
        @Override
        public ItemBuilder enchant(String enchantment, int level) {
            return this;
        }
        
        @Override
        public ItemBuilder glow(boolean glow) {
            return this;
        }
        
        @Override
        public ItemBuilder unbreakable(boolean unbreakable) {
            return this;
        }
        
        @Override
        public ItemBuilder addFlag(String flag) {
            return this;
        }
        
        @Override
        public ItemBuilder removeFlag(String flag) {
            return this;
        }
        
        @Override
        public ItemBuilder addNBT(String key, Object value) {
            return this;
        }
        
        @Override
        public ItemStack build() {
            return new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE);
        }
    }
    
    private class DummyInventoryBuilder implements InventoryBuilder {
        @Override
        public InventoryBuilder item(int slot, ItemStack item) {
            return this;
        }
        
        @Override
        public InventoryBuilder onClick(Consumer<InventoryClickEvent> handler) {
            return this;
        }
        
        @Override
        public InventoryBuilder onClose(Consumer<InventoryCloseEvent> handler) {
            return this;
        }
        
        @Override
        public InventoryBuilder fillBorder(ItemStack item) {
            return this;
        }
        
        @Override
        public InventoryBuilder fillEmpty(ItemStack item) {
            return this;
        }
        
        @Override
        public InventoryBuilder paginated(boolean paginated) {
            return this;
        }
        
        @Override
        public Inventory build() {
            return core.getServer().createInventory(null, 9, "Dummy Inventory");
        }
=======
    public boolean isDebugMode() {
        return core.isDebugMode();
>>>>>>> 1cd13da (Das ist Dumm)
    }
} 