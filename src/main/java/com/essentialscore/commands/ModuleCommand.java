package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import com.essentialscore.ConsoleFormatter;
import com.essentialscore.api.module.ModuleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to manage modules
 */
public class ModuleCommand implements CommandExecutor, TabCompleter {
    private final ApiCore apiCore;
    private final ConsoleFormatter console;
    
    public ModuleCommand(ApiCore apiCore) {
        this.apiCore = apiCore;
        this.console = new ConsoleFormatter(
            apiCore.getLogger(),
            apiCore.getConfig().getString("console.prefixes.module-command", "&8[&a&lModules&8]"),
            apiCore.getConfig().getBoolean("console.use-colors", true)
        );
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show module list when no arguments
            listModules(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("list")) {
            listModules(sender);
            return true;
        } else if (subCommand.equals("info") || subCommand.equals("details")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a module name."));
                return true;
            }
            
            String moduleName = args[1];
            showModuleInfo(sender, moduleName);
            return true;
        } else if (subCommand.equals("enable")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a module name."));
                return true;
            }
            
            String moduleName = args[1];
            enableModule(sender, moduleName);
            return true;
        } else if (subCommand.equals("disable")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a module name."));
                return true;
            }
            
            String moduleName = args[1];
            disableModule(sender, moduleName);
            return true;
        } else if (subCommand.equals("reload")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a module name or 'all'."));
                return true;
            }
            
            String moduleName = args[1];
            if (moduleName.equalsIgnoreCase("all")) {
                reloadAllModules(sender);
            } else {
                reloadModule(sender, moduleName);
            }
            return true;
        }
        
        // Unknown subcommand, show help
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fModule Command Help:"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module list &8- &7List all modules"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module info <name> &8- &7Show module info"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module enable <name> &8- &7Enable a module"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module disable <name> &8- &7Disable a module"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/module reload <name|all> &8- &7Reload module(s)"));
        
        return true;
    }
    
    /**
     * Lists all loaded modules
     */
    private void listModules(CommandSender sender) {
        Map<String, ModuleManager.ModuleInfo> modules = apiCore.getLoadedModules();
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fLoaded modules (&e" + modules.size() + "&f):"));
        
        if (modules.isEmpty()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7No modules loaded."));
            return;
        }
        
        for (Map.Entry<String, ModuleManager.ModuleInfo> entry : modules.entrySet()) {
            String moduleName = entry.getKey();
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &a" + moduleName));
        }
    }
    
    /**
     * Shows detailed information about a module
     */
    private void showModuleInfo(CommandSender sender, String moduleName) {
        Object moduleInfo = apiCore.getModuleInfo(moduleName);
        
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModule not found: &e" + moduleName));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fModule Information:"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &fName: &a" + moduleName));
        
        // Get version and description using reflection since we don't know the exact type
        try {
            Object version = moduleInfo.getClass().getMethod("getVersion").invoke(moduleInfo);
            Object description = moduleInfo.getClass().getMethod("getDescription").invoke(moduleInfo);
            Object jarFile = moduleInfo.getClass().getMethod("getJarFile").invoke(moduleInfo);
            
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &fVersion: &7" + version));
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &fDescription: &7" + description));
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &fJAR File: &7" + jarFile.toString()));
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cError retrieving module details: &7" + e.getMessage()));
        }
    }
    
    /**
     * Enables a module
     */
    private void enableModule(CommandSender sender, String moduleName) {
        // Check if module is already loaded
        if (apiCore.getModuleInfo(moduleName) != null) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModule is already enabled: &e" + moduleName));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Enabling module &e" + moduleName + "&7..."));
        
        try {
            boolean success = false;
            
            // Try to find an enableModule method
            try {
                success = (boolean) apiCore.getModuleManager().getClass().getMethod("enableModule", String.class)
                    .invoke(apiCore.getModuleManager(), moduleName);
            } catch (NoSuchMethodException e) {
                // If no specific method exists, try to load the module from the modules directory
                java.io.File modulesDir = new java.io.File(apiCore.getDataFolder(), "modules");
                java.io.File moduleFile = new java.io.File(modulesDir, moduleName + ".jar");
                
                if (moduleFile.exists()) {
                    apiCore.getModuleManager().getClass().getMethod("loadModule", java.io.File.class)
                        .invoke(apiCore.getModuleManager(), moduleFile);
                    success = apiCore.getModuleInfo(moduleName) != null;
                } else {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModule file not found: &e" + moduleName + ".jar"));
                    return;
                }
            }
            
            if (success) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aSuccessfully enabled module: &e" + moduleName));
            } else {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cFailed to enable module: &e" + moduleName));
            }
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cError enabling module: &e" + e.getMessage()));
            console.error("Error enabling module " + moduleName + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Disables a module
     */
    private void disableModule(CommandSender sender, String moduleName) {
        // Check if module is loaded
        if (apiCore.getModuleInfo(moduleName) == null) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModule is not enabled: &e" + moduleName));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Disabling module &e" + moduleName + "&7..."));
        
        try {
            boolean success = apiCore.disableModule(moduleName);
            
            if (success) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aSuccessfully disabled module: &e" + moduleName));
            } else {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cFailed to disable module: &e" + moduleName));
            }
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cError disabling module: &e" + e.getMessage()));
            console.error("Error disabling module " + moduleName + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Reloads a module
     */
    private void reloadModule(CommandSender sender, String moduleName) {
        // Check if module is loaded
        Object moduleInfo = apiCore.getModuleInfo(moduleName);
        if (moduleInfo == null) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cModule is not enabled: &e" + moduleName));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Reloading module &e" + moduleName + "&7..."));
        
        try {
            // Store JAR file reference
            java.io.File jarFile = (java.io.File) moduleInfo.getClass().getMethod("getJarFile").invoke(moduleInfo);
            
            // Unload module
            apiCore.getModuleManager().getClass().getMethod("unloadModule", String.class)
                .invoke(apiCore.getModuleManager(), moduleName);
            
            // Load module again
            apiCore.getModuleManager().getClass().getMethod("loadModule", java.io.File.class)
                .invoke(apiCore.getModuleManager(), jarFile);
            
            // Check if reload was successful
            if (apiCore.getModuleInfo(moduleName) != null) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aSuccessfully reloaded module: &e" + moduleName));
            } else {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cFailed to reload module: &e" + moduleName));
            }
        } catch (Exception e) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cError reloading module: &e" + e.getMessage()));
            console.error("Error reloading module " + moduleName + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Reloads all modules
     */
    private void reloadAllModules(CommandSender sender) {
        Map<String, ModuleManager.ModuleInfo> modules = apiCore.getLoadedModules();
        
        if (modules.isEmpty()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7No modules to reload."));
            return;
        }
        
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Reloading all modules..."));
        
        int success = 0;
        int failed = 0;
        
        for (Map.Entry<String, ModuleManager.ModuleInfo> entry : modules.entrySet()) {
            String moduleName = entry.getKey();
            Object moduleInfo = entry.getValue();
            
            try {
                // Store JAR file reference
                java.io.File jarFile = (java.io.File) moduleInfo.getClass().getMethod("getJarFile").invoke(moduleInfo);
                
                // Unload module
                apiCore.getModuleManager().getClass().getMethod("unloadModule", String.class)
                    .invoke(apiCore.getModuleManager(), moduleName);
                
                // Load module again
                apiCore.getModuleManager().getClass().getMethod("loadModule", java.io.File.class)
                    .invoke(apiCore.getModuleManager(), jarFile);
                
                // Check if reload was successful
                if (apiCore.getModuleInfo(moduleName) != null) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                console.error("Error reloading module " + moduleName + ": " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        
        if (failed == 0) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                "&aSuccessfully reloaded all modules (&e" + success + "&a)."));
        } else {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                "&eReloaded modules: &a" + success + " &esucceeded, &c" + failed + " &efailed."));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("list", "info", "enable", "disable", "reload");
            String prefix = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(prefix)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            
            if (subCommand.equals("info") || subCommand.equals("disable") || subCommand.equals("reload")) {
                // Suggest loaded module names
                for (String moduleName : apiCore.getLoadedModules().keySet()) {
                    if (moduleName.toLowerCase().startsWith(prefix)) {
                        completions.add(moduleName);
                    }
                }
                
                // Add 'all' option for reload
                if (subCommand.equals("reload") && "all".startsWith(prefix)) {
                    completions.add("all");
                }
            } else if (subCommand.equals("enable")) {
                try {
                    // Try to get available but not loaded modules list
                    Object moduleManager = apiCore.getModuleManager();
                    java.lang.reflect.Method method = moduleManager.getClass().getMethod("getAvailableModules");
                    List<String> availableModules = (List<String>) method.invoke(moduleManager);
                    
                    if (availableModules != null) {
                        for (String moduleName : availableModules) {
                            if (moduleName.toLowerCase().startsWith(prefix)) {
                                completions.add(moduleName);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fall back to scanning modules directory
                    java.io.File modulesDir = new java.io.File(apiCore.getDataFolder(), "modules");
                    if (modulesDir.exists() && modulesDir.isDirectory()) {
                        java.io.File[] files = modulesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                        if (files != null) {
                            for (java.io.File file : files) {
                                String moduleName = file.getName().replace(".jar", "");
                                if (moduleName.toLowerCase().startsWith(prefix)) {
                                    completions.add(moduleName);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return completions;
    }
} 