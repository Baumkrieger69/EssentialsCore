package com.essentialscore.api.web.rest.endpoints;

import com.essentialscore.api.web.rest.ApiEndpoint;
import com.essentialscore.api.web.rest.ApiRequest;
import com.essentialscore.api.web.rest.ApiResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * API endpoint for player management operations.
 */
public class PlayerManagementEndpoint extends ApiEndpoint {
    
    /**
     * Creates a new player management endpoint
     * 
     * @param plugin The plugin instance
     */
    public PlayerManagementEndpoint(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getPath() {
        return "players";
    }
    
    @Override
    public boolean requiresAuthentication() {
        return true;
    }
    
    @Override
    public String getRequiredPermission() {
        return "essentials.webui.players";
    }
    
    @Override
    public ApiResponse handleRequest(ApiRequest request) {
        try {
            String method = request.getMethod();
            String subpath = request.getPathSegment(1);
            
            if (subpath == null) {
                // Base players endpoint
                switch (method) {
                    case "GET":
                        return getPlayersList();
                    default:
                        return ApiResponse.methodNotAllowed("Method not allowed: " + method);
                }
            }
            
            if ("online".equals(subpath)) {
                return getOnlinePlayers();
            }
            
            if ("offline".equals(subpath)) {
                return getOfflinePlayers();
            }
            
            // Specific player operations
            String playerName = subpath;
            String action = request.getPathSegment(2);
            
            if (action == null) {
                // Player info
                switch (method) {
                    case "GET":
                        return getPlayerInfo(playerName);
                    default:
                        return ApiResponse.methodNotAllowed("Method not allowed: " + method);
                }
            }
            
            // Player actions
            switch (action) {
                case "kick":
                    return kickPlayer(playerName, request.getBody());
                case "ban":
                    return banPlayer(playerName, request.getBody());
                case "unban":
                    return unbanPlayer(playerName);
                case "teleport":
                    return teleportPlayer(playerName, request.getBody());
                case "gamemode":
                    return setGameMode(playerName, request.getBody());
                case "heal":
                    return healPlayer(playerName);
                case "feed":
                    return feedPlayer(playerName);
                case "message":
                    return sendMessage(playerName, request.getBody());
                default:
                    return ApiResponse.notFound("Unknown action: " + action);
            }
        } catch (Exception e) {
            return ApiResponse.error("Error processing player request: " + e.getMessage());
        }
    }
    
    /**
     * Gets a list of all players (online and offline)
     */
    private ApiResponse getPlayersList() {
        Map<String, Object> response = new HashMap<>();
        
        // Online players
        List<Map<String, Object>> onlinePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayers.add(createPlayerInfo(player));
        }
        
        response.put("online", onlinePlayers);
        response.put("onlineCount", onlinePlayers.size());
        response.put("maxPlayers", Bukkit.getMaxPlayers());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets online players
     */
    private ApiResponse getOnlinePlayers() {
        List<Map<String, Object>> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(createPlayerInfo(player));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("players", players);
        response.put("count", players.size());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets offline players (limited to last 100)
     */
    private ApiResponse getOfflinePlayers() {
        List<Map<String, Object>> players = new ArrayList<>();
        OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
        
        // Sort by last played (most recent first) and limit to 100
        Arrays.sort(offlinePlayers, (a, b) -> Long.compare(b.getLastPlayed(), a.getLastPlayed()));
        
        int count = Math.min(100, offlinePlayers.length);
        for (int i = 0; i < count; i++) {
            OfflinePlayer player = offlinePlayers[i];
            if (player.getName() != null && !player.isOnline()) {
                players.add(createOfflinePlayerInfo(player));
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("players", players);
        response.put("count", players.size());
        response.put("total", offlinePlayers.length);
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets detailed information about a specific player
     */
    private ApiResponse getPlayerInfo(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return ApiResponse.ok(createDetailedPlayerInfo(player));
        }
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return ApiResponse.ok(createDetailedOfflinePlayerInfo(offlinePlayer));
        }
        
        return ApiResponse.notFound("Player not found: " + playerName);
    }
    
    /**
     * Kicks a player from the server
     */
    private ApiResponse kickPlayer(String playerName, String body) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return ApiResponse.notFound("Player not online: " + playerName);
        }
        
        String reason = "Kicked by admin";
        if (body != null && !body.trim().isEmpty()) {
            try {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("reason")) {
                    reason = json.get("reason").getAsString();
                }
            } catch (Exception e) {
                // Use body as reason if JSON parsing fails
                reason = body;
            }
        }
        
        final String finalReason = reason;
        Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(finalReason));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Player " + playerName + " has been kicked");
        response.put("reason", reason);
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Bans a player
     */
    private ApiResponse banPlayer(String playerName, String body) {
        String reason = "Banned by admin";
        Date expiration = null;
        
        if (body != null && !body.trim().isEmpty()) {
            try {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("reason")) {
                    reason = json.get("reason").getAsString();
                }
                if (json.has("duration")) {
                    long duration = json.get("duration").getAsLong();
                    expiration = new Date(System.currentTimeMillis() + duration * 1000);
                }
            } catch (Exception e) {
                reason = body;
            }
        }
        
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(playerName, reason, expiration, "WebUI Admin");
        
        // Kick if online
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            final String finalReason = reason;
            Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer("You have been banned: " + finalReason));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Player " + playerName + " has been banned");
        response.put("reason", reason);
        response.put("expiration", expiration != null ? expiration.getTime() : null);
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Unbans a player
     */
    private ApiResponse unbanPlayer(String playerName) {
        boolean wasBanned = Bukkit.getBanList(org.bukkit.BanList.Type.NAME).isBanned(playerName);
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(playerName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", wasBanned ? 
            "Player " + playerName + " has been unbanned" : 
            "Player " + playerName + " was not banned");
        response.put("wasBanned", wasBanned);
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Teleports a player
     */
    private ApiResponse teleportPlayer(String playerName, String body) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return ApiResponse.notFound("Player not online: " + playerName);
        }
        
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            
            if (json.has("target")) {
                // Teleport to another player
                String targetName = json.get("target").getAsString();
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    return ApiResponse.notFound("Target player not online: " + targetName);
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> player.teleport(target));
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", playerName + " teleported to " + targetName);
                return ApiResponse.ok(response);
            } else {
                // Teleport to coordinates
                double x = json.get("x").getAsDouble();
                double y = json.get("y").getAsDouble();
                double z = json.get("z").getAsDouble();
                String worldName = json.has("world") ? json.get("world").getAsString() : player.getWorld().getName();
                
                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    return ApiResponse.notFound("World not found: " + worldName);
                }
                
                Location location = new Location(world, x, y, z);
                Bukkit.getScheduler().runTask(plugin, () -> player.teleport(location));
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", String.format("%s teleported to %.2f, %.2f, %.2f in %s", 
                    playerName, x, y, z, worldName));
                return ApiResponse.ok(response);
            }
        } catch (Exception e) {
            return ApiResponse.badRequest("Invalid teleport data: " + e.getMessage());
        }
    }
    
    /**
     * Sets a player's game mode
     */
    private ApiResponse setGameMode(String playerName, String body) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return ApiResponse.notFound("Player not online: " + playerName);
        }
        
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String mode = json.get("gamemode").getAsString().toUpperCase();
            
            GameMode gameMode = GameMode.valueOf(mode);
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(gameMode));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", playerName + "'s game mode set to " + gameMode.name());
            response.put("gamemode", gameMode.name());
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.badRequest("Invalid game mode: " + e.getMessage());
        }
    }
    
    /**
     * Heals a player
     */
    private ApiResponse healPlayer(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return ApiResponse.notFound("Player not online: " + playerName);
        }
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", playerName + " has been healed");
        return ApiResponse.ok(response);
    }
    
    /**
     * Feeds a player
     */
    private ApiResponse feedPlayer(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return ApiResponse.notFound("Player not online: " + playerName);
        }
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setFoodLevel(20);
            player.setSaturation(20f);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", playerName + " has been fed");
        return ApiResponse.ok(response);
    }
    
    /**
     * Sends a message to a player
     */
    private ApiResponse sendMessage(String playerName, String body) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return ApiResponse.notFound("Player not online: " + playerName);
        }
        
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String message = json.get("message").getAsString();
            
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("[WebUI Admin] " + message));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message sent to " + playerName);
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.badRequest("Invalid message data: " + e.getMessage());
        }
    }
    
    /**
     * Creates basic player info map
     */
    private Map<String, Object> createPlayerInfo(Player player) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", player.getName());
        info.put("displayName", player.getDisplayName());
        info.put("uuid", player.getUniqueId().toString());
        info.put("online", true);
        info.put("health", player.getHealth());
        info.put("maxHealth", player.getMaxHealth());
        info.put("foodLevel", player.getFoodLevel());
        info.put("level", player.getLevel());
        info.put("gameMode", player.getGameMode().name());
        info.put("world", player.getWorld().getName());
        
        Location loc = player.getLocation();
        Map<String, Object> location = new HashMap<>();
        location.put("x", loc.getX());
        location.put("y", loc.getY());
        location.put("z", loc.getZ());
        location.put("world", loc.getWorld().getName());
        info.put("location", location);
        
        return info;
    }
    
    /**
     * Creates detailed player info map
     */
    private Map<String, Object> createDetailedPlayerInfo(Player player) {
        Map<String, Object> info = createPlayerInfo(player);
        
        // Add detailed information
        info.put("exp", player.getExp());
        info.put("totalExperience", player.getTotalExperience());
        info.put("allowFlight", player.getAllowFlight());
        info.put("isFlying", player.isFlying());
        info.put("walkSpeed", player.getWalkSpeed());
        info.put("flySpeed", player.getFlySpeed());
        info.put("playerTime", player.getPlayerTime());
        info.put("firstPlayed", player.getFirstPlayed());
        info.put("lastPlayed", player.getLastPlayed());
        info.put("hasPlayedBefore", player.hasPlayedBefore());
        info.put("isBanned", player.isBanned());
        info.put("isWhitelisted", player.isWhitelisted());
        info.put("isOp", player.isOp());
        
        return info;
    }
    
    /**
     * Creates offline player info map
     */
    private Map<String, Object> createOfflinePlayerInfo(OfflinePlayer player) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", player.getName());
        info.put("uuid", player.getUniqueId().toString());
        info.put("online", false);
        info.put("firstPlayed", player.getFirstPlayed());
        info.put("lastPlayed", player.getLastPlayed());
        info.put("hasPlayedBefore", player.hasPlayedBefore());
        info.put("isBanned", player.isBanned());
        info.put("isWhitelisted", player.isWhitelisted());
        info.put("isOp", player.isOp());
        
        return info;
    }
    
    /**
     * Creates detailed offline player info map
     */
    private Map<String, Object> createDetailedOfflinePlayerInfo(OfflinePlayer player) {
        return createOfflinePlayerInfo(player);
    }
}
