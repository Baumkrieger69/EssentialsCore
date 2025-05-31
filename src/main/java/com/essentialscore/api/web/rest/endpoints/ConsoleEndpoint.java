package com.essentialscore.api.web.rest.endpoints;

import com.essentialscore.api.web.rest.ApiEndpoint;
import com.essentialscore.api.web.rest.ApiRequest;
import com.essentialscore.api.web.rest.ApiResponse;
import com.essentialscore.api.web.ui.LiveConsoleManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * API endpoint for console management operations.
 */
public class ConsoleEndpoint extends ApiEndpoint {
    
    private final LiveConsoleManager consoleManager;
    private final Queue<String> commandHistory;
    private final int MAX_HISTORY_SIZE = 100;
    private final long startTime;
    
    /**
     * Creates a new console endpoint
     * 
     * @param plugin The plugin instance
     * @param consoleManager The live console manager
     */
    public ConsoleEndpoint(Plugin plugin, LiveConsoleManager consoleManager) {
        super(plugin);
        this.consoleManager = consoleManager;
        this.commandHistory = new ConcurrentLinkedQueue<>();
        this.startTime = System.currentTimeMillis();
    }
    
    @Override
    public String getPath() {
        return "console";
    }
    
    @Override
    public boolean requiresAuthentication() {
        return true;
    }
    
    @Override
    public String getRequiredPermission() {
        return "essentials.webui.console";
    }
    
    @Override
    public ApiResponse handleRequest(ApiRequest request) {
        try {
            String method = request.getMethod();
            String subpath = request.getPathSegment(1);
            
            if (subpath == null) {
                // Base console endpoint
                switch (method) {
                    case "GET":
                        return getConsoleStatus();
                    default:
                        return ApiResponse.methodNotAllowed("Method not allowed: " + method);
                }
            }
            
            switch (subpath) {
                case "logs":
                    return getConsoleLogs(request);
                case "execute":
                    return executeCommand(request);
                case "history":
                    return getCommandHistory();
                case "clear":
                    return clearConsole();
                case "players":
                    return getPlayerCommands();
                case "plugins":
                    return getPluginCommands();
                default:
                    return ApiResponse.notFound("Unknown console operation: " + subpath);
            }
        } catch (Exception e) {
            return ApiResponse.error("Error processing console request: " + e.getMessage());
        }
    }
    
    /**
     * Gets console status and basic information
     */
    private ApiResponse getConsoleStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Server information
        status.put("serverName", Bukkit.getServer().getName());
        status.put("serverVersion", Bukkit.getServer().getVersion());
        status.put("bukkitVersion", Bukkit.getServer().getBukkitVersion());
        status.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        status.put("maxPlayers", Bukkit.getMaxPlayers());
        status.put("worlds", Bukkit.getWorlds().stream().map(w -> w.getName()).toArray());
        
        // Console state
        status.put("consoleActive", true);
        status.put("commandHistorySize", commandHistory.size());
        status.put("serverTime", System.currentTimeMillis());
        status.put("serverUptime", System.currentTimeMillis() - startTime);
        
        // Performance info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("total", runtime.totalMemory());
        memory.put("max", runtime.maxMemory());
        status.put("memory", memory);
        
        return ApiResponse.ok(status);
    }
    
    /**
     * Gets recent console logs
     */
    private ApiResponse getConsoleLogs(ApiRequest request) {
        try {
            String limitStr = request.getParameter("limit");
            String sinceStr = request.getParameter("since");
            
            int limit = 100; // Default limit
            if (limitStr != null) {
                try {
                    limit = Math.min(1000, Math.max(1, Integer.parseInt(limitStr))); // Max 1000, min 1
                } catch (NumberFormatException e) {
                    // Use default
                }
            }
            
            long since = 0;
            if (sinceStr != null) {
                try {
                    since = Long.parseLong(sinceStr);
                } catch (NumberFormatException e) {
                    // Use default
                }
            }
            
            List<Map<String, Object>> logs = consoleManager.getRecentLogs(limit, since);
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs);
            response.put("count", logs.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("hasMore", logs.size() >= limit);
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error retrieving console logs: " + e.getMessage());
        }
    }
    
    /**
     * Executes a console command
     */
    private ApiResponse executeCommand(ApiRequest request) {
        try {
            if (request.getBody() == null || request.getBody().trim().isEmpty()) {
                return ApiResponse.badRequest("Command is required");
            }
            
            JsonObject json = new JsonParser().parse(request.getBody()).getAsJsonObject();
            String command = json.get("command").getAsString().trim();
            
            if (command.isEmpty()) {
                return ApiResponse.badRequest("Command cannot be empty");
            }
            
            // Security check - prevent certain dangerous commands
            if (isDangerousCommand(command)) {
                return ApiResponse.forbidden("Command not allowed for security reasons");
            }
            
            // Add to command history
            addToHistory(command);
            
            // Execute command
            boolean success = executeServerCommand(command);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("command", command);
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", success ? "Command executed successfully" : "Command execution failed");
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error executing command: " + e.getMessage());
        }
    }
    
    /**
     * Gets command history
     */
    private ApiResponse getCommandHistory() {
        Map<String, Object> response = new HashMap<>();
        response.put("history", new ArrayList<>(commandHistory));
        response.put("count", commandHistory.size());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Clears console (placeholder - actual implementation would depend on logging framework)
     */
    private ApiResponse clearConsole() {
        // In a real implementation, this would clear the console buffer
        // For now, we'll just indicate success
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Console cleared");
        response.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets available player-related commands
     */
    private ApiResponse getPlayerCommands() {
        List<Map<String, Object>> commands = new ArrayList<>();
        
        // Common player commands
        commands.add(createCommandInfo("list", "List online players", "list"));
        commands.add(createCommandInfo("kick", "Kick a player", "kick <player> [reason]"));
        commands.add(createCommandInfo("ban", "Ban a player", "ban <player> [reason]"));
        commands.add(createCommandInfo("pardon", "Unban a player", "pardon <player>"));
        commands.add(createCommandInfo("op", "Give operator status", "op <player>"));
        commands.add(createCommandInfo("deop", "Remove operator status", "deop <player>"));
        commands.add(createCommandInfo("tp", "Teleport player", "tp <player1> <player2>"));
        commands.add(createCommandInfo("gamemode", "Change gamemode", "gamemode <mode> <player>"));
        commands.add(createCommandInfo("give", "Give items", "give <player> <item> [amount]"));
        commands.add(createCommandInfo("weather", "Change weather", "weather <clear|rain|thunder>"));
        commands.add(createCommandInfo("time", "Change time", "time set <value>"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("commands", commands);
        response.put("category", "player");
        response.put("count", commands.size());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets available plugin-related commands
     */
    private ApiResponse getPluginCommands() {
        List<Map<String, Object>> commands = new ArrayList<>();
        
        // Plugin management commands
        commands.add(createCommandInfo("plugins", "List plugins", "plugins"));
        commands.add(createCommandInfo("pl", "List plugins (short)", "pl"));
        commands.add(createCommandInfo("reload", "Reload server", "reload"));
        commands.add(createCommandInfo("version", "Show version", "version [plugin]"));
        commands.add(createCommandInfo("help", "Show help", "help [command]"));
        
        // Add plugin-specific commands
        Arrays.stream(Bukkit.getPluginManager().getPlugins())
            .forEach(plugin -> {
                String pluginName = plugin.getName();
                commands.add(createCommandInfo(
                    pluginName.toLowerCase(), 
                    "Commands for " + pluginName, 
                    pluginName.toLowerCase() + " <subcommand>"
                ));
            });
        
        Map<String, Object> response = new HashMap<>();
        response.put("commands", commands);
        response.put("category", "plugin");
        response.put("count", commands.size());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Creates command information map
     */
    private Map<String, Object> createCommandInfo(String name, String description, String usage) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("description", description);
        info.put("usage", usage);
        return info;
    }
    
    /**
     * Executes a server command
     */
    private boolean executeServerCommand(String command) {
        try {
            // Execute the command on the main server thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error executing command '" + command + "': " + e.getMessage());
                }
            });
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to schedule command execution: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a command is dangerous and should be blocked
     */
    private boolean isDangerousCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();
        
        // Block certain dangerous commands
        String[] dangerousCommands = {
            "stop", "restart", "shutdown", "halt",
            "rm ", "del ", "delete ", "format",
            "sudo ", "su ", "chmod ", "chown ",
            "kill ", "killall", "pkill",
            "systemctl", "service",
            "wget ", "curl ", "nc ", "netcat",
            "iptables", "ufw", "firewall",
            "crontab", "at ", "batch"
        };
        
        for (String dangerous : dangerousCommands) {
            if (lowerCommand.startsWith(dangerous) || lowerCommand.contains(" " + dangerous)) {
                return true;
            }
        }
        
        // Block commands that might execute arbitrary code
        if (lowerCommand.contains("eval") || 
            lowerCommand.contains("exec") || 
            lowerCommand.contains("system") ||
            lowerCommand.contains("runtime") ||
            lowerCommand.contains("script") ||
            lowerCommand.contains("javascript") ||
            lowerCommand.contains("groovy")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Adds a command to the command history
     */
    private void addToHistory(String command) {
        commandHistory.offer(command);
        
        // Maintain max history size
        while (commandHistory.size() > MAX_HISTORY_SIZE) {
            commandHistory.poll();
        }
    }
}
