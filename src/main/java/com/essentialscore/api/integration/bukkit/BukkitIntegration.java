package com.essentialscore.api.integration.bukkit;

import com.essentialscore.api.integration.AbstractPluginIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Integration with Bukkit API, providing safer access to common Bukkit functionality.
 */
public class BukkitIntegration extends AbstractPluginIntegration {
    private final PlayerManager playerManager;
    private final WorldManager worldManager;
    private final ServerManager serverManager;
    private final EntityManager entityManager;
    
    /**
     * Creates a new Bukkit integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public BukkitIntegration(Plugin plugin) {
        super(plugin);
        this.playerManager = new PlayerManager(plugin);
        this.worldManager = new WorldManager(plugin);
        this.serverManager = new ServerManager(plugin);
        this.entityManager = new EntityManager(plugin);
    }
    
    @Override
    protected void onInitialize() {
        // Nothing to initialize for Bukkit integration
    }
    
    @Override
    protected void onShutdown() {
        // Nothing to shut down for Bukkit integration
    }
    
    @Override
    public String getName() {
        return "Bukkit";
    }
    
    /**
     * Gets the player manager.
     *
     * @return The player manager
     */
    public PlayerManager getPlayerManager() {
        return playerManager;
    }
    
    /**
     * Gets the world manager.
     *
     * @return The world manager
     */
    public WorldManager getWorldManager() {
        return worldManager;
    }
    
    /**
     * Gets the server manager.
     *
     * @return The server manager
     */
    public ServerManager getServerManager() {
        return serverManager;
    }
    
    /**
     * Gets the entity manager.
     *
     * @return The entity manager
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }
    
    /**
     * Manager class for player-related operations.
     */
    public class PlayerManager {
        
        /**
         * Creates a new player manager.
         *
         * @param plugin The EssentialsCore plugin
         */
        public PlayerManager(Plugin plugin) {
            // Plugin parameter kept for consistency with other managers
        }
        
        /**
         * Gets a player by name.
         *
         * @param name The player name
         * @return The player, or empty if not found
         */
        public Optional<Player> getPlayer(String name) {
            try {
                return Optional.ofNullable(Bukkit.getPlayer(name));
            } catch (Exception e) {
                logger.warning("Failed to get player by name: " + name);
                return Optional.empty();
            }
        }
        
        /**
         * Gets a player by UUID.
         *
         * @param uuid The player UUID
         * @return The player, or empty if not found
         */
        public Optional<Player> getPlayer(UUID uuid) {
            try {
                return Optional.ofNullable(Bukkit.getPlayer(uuid));
            } catch (Exception e) {
                logger.warning("Failed to get player by UUID: " + uuid);
                return Optional.empty();
            }
        }
        
        /**
         * Gets all online players.
         *
         * @return Collection of online players
         */
        public Collection<? extends Player> getOnlinePlayers() {
            try {
                return Bukkit.getOnlinePlayers();
            } catch (Exception e) {
                logger.warning("Failed to get online players");
                return Collections.emptyList();
            }
        }
        
        /**
         * Gets the player exact name from a partial match.
         *
         * @param name The partial name
         * @return The player name, or empty if not found
         */
        public Optional<String> getPlayerExactName(String name) {
            try {
                Player player = Bukkit.getPlayerExact(name);
                return player != null ? Optional.of(player.getName()) : Optional.empty();
            } catch (Exception e) {
                logger.warning("Failed to get player exact name: " + name);
                return Optional.empty();
            }
        }
        
        /**
         * Gets players that match a partial name.
         *
         * @param name The partial name
         * @return List of matching players
         */
        public List<Player> getMatchingPlayers(String name) {
            try {
                return Bukkit.matchPlayer(name);
            } catch (Exception e) {
                logger.warning("Failed to get matching players for: " + name);
                return Collections.emptyList();
            }
        }
        
        /**
         * Broadcasts a message to all players.
         *
         * @param message The message to broadcast
         */
        public void broadcastMessage(String message) {
            try {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(message);
                }
            } catch (Exception e) {
                logger.warning("Failed to broadcast message: " + message);
            }
        }
        
        /**
         * Broadcasts a message to players with a specific permission.
         *
         * @param message The message to broadcast
         * @param permission The permission required to receive the message
         * @return The number of players who received the message
         */
        public int broadcastMessage(String message, String permission) {
            try {
                int count = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission(permission)) {
                        player.sendMessage(message);
                        count++;
                    }
                }
                return count;
            } catch (Exception e) {
                logger.warning("Failed to broadcast message with permission: " + message + ", " + permission);
                return 0;
            }
        }
    }
    
    /**
     * Manager class for world-related operations.
     */
    public class WorldManager {
        
        /**
         * Creates a new world manager.
         *
         * @param plugin The EssentialsCore plugin
         */
        public WorldManager(Plugin plugin) {
            // Plugin parameter kept for consistency with other managers
        }
        
        /**
         * Gets a world by name.
         *
         * @param name The world name
         * @return The world, or empty if not found
         */
        public Optional<World> getWorld(String name) {
            try {
                return Optional.ofNullable(Bukkit.getWorld(name));
            } catch (Exception e) {
                logger.warning("Failed to get world by name: " + name);
                return Optional.empty();
            }
        }
        
        /**
         * Gets a world by UUID.
         *
         * @param uuid The world UUID
         * @return The world, or empty if not found
         */
        public Optional<World> getWorld(UUID uuid) {
            try {
                return Optional.ofNullable(Bukkit.getWorld(uuid));
            } catch (Exception e) {
                logger.warning("Failed to get world by UUID: " + uuid);
                return Optional.empty();
            }
        }
        
        /**
         * Gets all worlds.
         *
         * @return List of worlds
         */
        public List<World> getWorlds() {
            try {
                return Bukkit.getWorlds();
            } catch (Exception e) {
                logger.warning("Failed to get worlds");
                return Collections.emptyList();
            }
        }
        
        /**
         * Creates a location.
         *
         * @param world The world
         * @param x The X coordinate
         * @param y The Y coordinate
         * @param z The Z coordinate
         * @return The location
         */
        public Location createLocation(World world, double x, double y, double z) {
            return new Location(world, x, y, z);
        }
        
        /**
         * Creates a location with yaw and pitch.
         *
         * @param world The world
         * @param x The X coordinate
         * @param y The Y coordinate
         * @param z The Z coordinate
         * @param yaw The yaw
         * @param pitch The pitch
         * @return The location
         */
        public Location createLocation(World world, double x, double y, double z, float yaw, float pitch) {
            return new Location(world, x, y, z, yaw, pitch);
        }
    }
    
    /**
     * Manager class for server-related operations.
     */
    public class ServerManager {
        
        /**
         * Creates a new server manager.
         *
         * @param plugin The EssentialsCore plugin
         */
        public ServerManager(Plugin plugin) {
            // Plugin parameter kept for consistency with other managers
        }
        
        /**
         * Gets the server name.
         *
         * @return The server name
         */
        public String getServerName() {
            return Bukkit.getServer().getName();
        }
        
        /**
         * Gets the server version.
         *
         * @return The server version
         */
        public String getServerVersion() {
            return Bukkit.getServer().getVersion();
        }
        
        /**
         * Gets the Bukkit version.
         *
         * @return The Bukkit version
         */
        public String getBukkitVersion() {
            return Bukkit.getServer().getBukkitVersion();
        }
        
        /**
         * Gets the maximum number of players allowed on the server.
         *
         * @return The maximum players
         */
        public int getMaxPlayers() {
            return Bukkit.getServer().getMaxPlayers();
        }
        
        /**
         * Gets the port the server is running on.
         *
         * @return The port
         */
        public int getPort() {
            return Bukkit.getServer().getPort();
        }
        
        /**
         * Gets the view distance.
         *
         * @return The view distance
         */
        public int getViewDistance() {
            return Bukkit.getServer().getViewDistance();
        }
        
        /**
         * Checks if the server is in online mode.
         *
         * @return true if the server is in online mode
         */
        public boolean isOnlineMode() {
            return Bukkit.getServer().getOnlineMode();
        }
        
        /**
         * Gets the server TPS (ticks per second).
         *
         * @return The server TPS
         */
        public double[] getTPS() {
            try {
                return Bukkit.getServer().getTPS();
            } catch (Exception e) {
                logger.warning("Failed to get server TPS");
                return new double[] { 20.0, 20.0, 20.0 };
            }
        }
    }
    
    /**
     * Manager class for entity-related operations.
     */
    public class EntityManager {
        
        /**
         * Creates a new entity manager.
         *
         * @param plugin The EssentialsCore plugin
         */
        public EntityManager(Plugin plugin) {
            // Plugin parameter kept for consistency with other managers
        }
        
        /**
         * Gets an entity by UUID.
         *
         * @param uuid The entity UUID
         * @return The entity, or empty if not found
         */
        public Optional<Entity> getEntity(UUID uuid) {
            try {
                return Optional.ofNullable(Bukkit.getEntity(uuid));
            } catch (Exception e) {
                logger.warning("Failed to get entity by UUID: " + uuid);
                return Optional.empty();
            }
        }
        
        /**
         * Gets all entities in a world.
         *
         * @param world The world
         * @return List of entities in the world
         */
        public List<Entity> getEntities(World world) {
            try {
                return world.getEntities();
            } catch (Exception e) {
                logger.warning("Failed to get entities in world: " + world.getName());
                return Collections.emptyList();
            }
        }
        
        /**
         * Gets entities of a specific type in a world.
         *
         * @param world The world
         * @param entityClass The entity class
         * @param <T> The entity type
         * @return List of entities of the specified type
         */
        public <T extends Entity> List<T> getEntitiesByType(World world, Class<T> entityClass) {
            try {
                return new ArrayList<>(world.getEntitiesByClass(entityClass));
            } catch (Exception e) {
                logger.warning("Failed to get entities by type in world: " + world.getName());
                return Collections.emptyList();
            }
        }
        
        /**
         * Gets entities near a location.
         *
         * @param location The location
         * @param radius The radius to search
         * @return List of entities near the location
         */
        public List<Entity> getNearbyEntities(Location location, double radius) {
            try {
                return location.getWorld().getNearbyEntities(location, radius, radius, radius)
                    .stream()
                    .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warning("Failed to get nearby entities at location: " + location);
                return Collections.emptyList();
            }
        }
    }
} 
