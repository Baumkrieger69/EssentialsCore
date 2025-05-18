package com.essentialscore.api.impl;

import com.essentialscore.ApiCore;
import com.essentialscore.DynamicCommand;
import com.essentialscore.api.CommandDefinition;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.api.ModuleEventListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
        
        return future;
    }
    
    @Override
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
    
    @Override
    public void logInfo(String message) {
        core.getLogger().info("[" + moduleName + "] " + message);
    }
    
    @Override
    public void logWarning(String message) {
        core.getLogger().warning("[" + moduleName + "] " + message);
    }
    
    @Override
    public void logError(String message, Throwable throwable) {
        if (throwable != null) {
            core.getLogger().log(Level.SEVERE, "[" + moduleName + "] " + message, throwable);
        } else {
            core.getLogger().severe("[" + moduleName + "] " + message);
        }
    }
    
    @Override
    public void logDebug(String message) {
        if (core.isDebugMode()) {
            core.getLogger().info("[" + moduleName + "] [DEBUG] " + message);
        }
    }
    
    @Override
    public boolean isDebugMode() {
        return core.isDebugMode();
    }
} 