package com.essentialscore.api.gaming;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * System that tracks player behavior and makes personalized content suggestions.
 */
public class PlayerBehaviorAnalytics implements Listener {
    private static final Logger LOGGER = Logger.getLogger(PlayerBehaviorAnalytics.class.getName());

    private final Plugin plugin;
    private final Map<UUID, PlayerProfile> playerProfiles;
    private final Map<UUID, PlayerSession> activeSessions;
    private final Map<String, ContentSuggester> contentSuggesters;
    private final Map<UUID, Set<String>> playerSuggestions;
    private final File dataFolder;

    private boolean running;
    private int saveTaskId;

    /**
     * Creates a new player behavior analytics system.
     *
     * @param plugin The plugin
     */
    public PlayerBehaviorAnalytics(Plugin plugin) {
        this.plugin = plugin;
        this.playerProfiles = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.contentSuggesters = new ConcurrentHashMap<>();
        this.playerSuggestions = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "player_analytics");
        this.running = false;
    }

    /**
     * Starts the player behavior analytics system.
     */
    public void start() {
        if (running) return;

        LOGGER.info("Starting player behavior analytics system");

        // Create data directory if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Load player profiles
        loadPlayerProfiles();

        // Create sessions for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            createSession(player);
        }

        // Schedule regular data saving
        saveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::savePlayerProfiles, 
                20L * 60 * 5, 20L * 60 * 5); // Save every 5 minutes

        running = true;
    }

    /**
     * Stops the player behavior analytics system.
     */
    public void stop() {
        if (!running) return;

        LOGGER.info("Stopping player behavior analytics system");

        // End all active sessions
        for (Player player : Bukkit.getOnlinePlayers()) {
            endSession(player);
        }

        // Save player profiles
        savePlayerProfiles();

        // Cancel scheduled tasks
        Bukkit.getScheduler().cancelTask(saveTaskId);

        running = false;
    }

    /**
     * Loads player profiles from disk.
     */
    private void loadPlayerProfiles() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null) return;
        
        for (File file : files) {
            try {
                // Extract UUID from filename
                String fileName = file.getName();
                String uuidStr = fileName.substring(0, fileName.length() - 4); // Remove .yml
                UUID playerId = UUID.fromString(uuidStr);
                
                // Load player profile
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                PlayerProfile profile = PlayerProfile.fromConfig(config);
                
                playerProfiles.put(playerId, profile);
                LOGGER.fine("Loaded player profile: " + playerId);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error loading player profile: " + file.getName(), e);
            }
        }
        
        LOGGER.info("Loaded " + playerProfiles.size() + " player profiles");
    }

    /**
     * Saves player profiles to disk.
     */
    private void savePlayerProfiles() {
        int savedCount = 0;
        
        for (Map.Entry<UUID, PlayerProfile> entry : playerProfiles.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerProfile profile = entry.getValue();
            
            try {
                // Create config from profile
                YamlConfiguration config = profile.toConfig();
                
                // Save to file
                File file = new File(dataFolder, playerId.toString() + ".yml");
                config.save(file);
                
                savedCount++;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error saving player profile: " + playerId, e);
            }
        }
        
        LOGGER.info("Saved " + savedCount + " player profiles");
    }

    /**
     * Creates a session for a player.
     *
     * @param player The player
     */
    private void createSession(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Get or create player profile
        PlayerProfile profile = playerProfiles.computeIfAbsent(playerId, 
                id -> new PlayerProfile(player.getName()));
        
        // Create new session
        PlayerSession session = new PlayerSession(player);
        activeSessions.put(playerId, session);
        
        // Initialize suggestions
        updateSuggestions(player);
        
        LOGGER.fine("Created session for player: " + player.getName());
    }

    /**
     * Ends a session for a player.
     *
     * @param player The player
     */
    private void endSession(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Get active session
        PlayerSession session = activeSessions.remove(playerId);
        
        if (session != null) {
            // Update player profile with session data
            PlayerProfile profile = playerProfiles.get(playerId);
            
            if (profile != null) {
                profile.totalPlayTime += session.getSessionDuration().toMillis();
                profile.lastSeen = Instant.now().toEpochMilli();
                profile.sessionCount++;
                
                // Merge activity counters
                session.getActivityCounters().forEach((activity, count) -> 
                        profile.activityCounters.merge(activity, count, Long::sum));
                
                // Merge visited locations
                session.getVisitedLocations().forEach(location -> 
                        profile.visitedLocations.add(location.toString()));
                
                // Update preferences based on activities
                updatePlayerPreferences(profile, session);
            }
            
            LOGGER.fine("Ended session for player: " + player.getName() + 
                    ", duration: " + session.getSessionDuration().toSeconds() + "s");
        }
    }

    /**
     * Updates player preferences based on session activities.
     *
     * @param profile The player profile
     * @param session The player session
     */
    private void updatePlayerPreferences(PlayerProfile profile, PlayerSession session) {
        Map<String, Long> activityCounters = session.getActivityCounters();
        
        // Calculate activity ratios
        long totalActivities = activityCounters.values().stream().mapToLong(Long::longValue).sum();
        
        if (totalActivities > 0) {
            // Update activity preferences
            for (Map.Entry<String, Long> entry : activityCounters.entrySet()) {
                String activity = entry.getKey();
                long count = entry.getValue();
                
                double ratio = (double) count / totalActivities;
                profile.activityPreferences.put(activity, ratio);
            }
            
            // Normalize preferences
            normalizePreferences(profile.activityPreferences);
        }
        
        // Update combat preferences
        updateCombatPreferences(profile, session);
        
        // Update building preferences
        updateBuildingPreferences(profile, session);
    }

    /**
     * Updates combat preferences.
     *
     * @param profile The player profile
     * @param session The player session
     */
    private void updateCombatPreferences(PlayerProfile profile, PlayerSession session) {
        // Extract combat-related activities
        Map<String, Long> combatActivities = session.getActivityCounters().entrySet().stream()
                .filter(e -> e.getKey().startsWith("combat."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        long totalCombat = combatActivities.values().stream().mapToLong(Long::longValue).sum();
        
        if (totalCombat > 0) {
            // Update combat preferences
            for (Map.Entry<String, Long> entry : combatActivities.entrySet()) {
                String target = entry.getKey().substring("combat.".length());
                long count = entry.getValue();
                
                double ratio = (double) count / totalCombat;
                profile.combatPreferences.put(target, ratio);
            }
            
            // Normalize preferences
            normalizePreferences(profile.combatPreferences);
        }
    }

    /**
     * Updates building preferences.
     *
     * @param profile The player profile
     * @param session The player session
     */
    private void updateBuildingPreferences(PlayerProfile profile, PlayerSession session) {
        // Extract building-related activities
        Map<String, Long> buildingActivities = session.getActivityCounters().entrySet().stream()
                .filter(e -> e.getKey().startsWith("block.place."))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring("block.place.".length()),
                        Map.Entry::getValue));
        
        long totalPlaced = buildingActivities.values().stream().mapToLong(Long::longValue).sum();
        
        if (totalPlaced > 0) {
            // Update building preferences
            for (Map.Entry<String, Long> entry : buildingActivities.entrySet()) {
                String material = entry.getKey();
                long count = entry.getValue();
                
                double ratio = (double) count / totalPlaced;
                profile.buildingPreferences.put(material, ratio);
            }
            
            // Normalize preferences
            normalizePreferences(profile.buildingPreferences);
        }
    }

    /**
     * Normalizes preference values to ensure they sum to 1.0.
     *
     * @param preferences The preferences to normalize
     */
    private void normalizePreferences(Map<String, Double> preferences) {
        double sum = preferences.values().stream().mapToDouble(Double::doubleValue).sum();
        
        if (sum > 0) {
            preferences.replaceAll((k, v) -> v / sum);
        }
    }

    /**
     * Updates content suggestions for a player.
     *
     * @param player The player
     */
    public void updateSuggestions(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerProfile profile = playerProfiles.get(playerId);
        
        if (profile == null) return;
        
        // Clear previous suggestions
        Set<String> suggestions = playerSuggestions.computeIfAbsent(playerId, k -> new HashSet<>());
        suggestions.clear();
        
        // Get suggestions from all registered suggesters
        for (ContentSuggester suggester : contentSuggesters.values()) {
            try {
                Set<String> contentIds = suggester.suggestContent(profile);
                suggestions.addAll(contentIds);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error getting suggestions from " + suggester.getId(), e);
            }
        }
        
        LOGGER.fine("Updated suggestions for " + player.getName() + ": " + suggestions.size() + " items");
    }

    /**
     * Gets content suggestions for a player.
     *
     * @param player The player
     * @return The suggested content IDs
     */
    public Set<String> getSuggestions(Player player) {
        UUID playerId = player.getUniqueId();
        return new HashSet<>(playerSuggestions.getOrDefault(playerId, Collections.emptySet()));
    }

    /**
     * Registers a content suggester.
     *
     * @param suggester The suggester
     */
    public void registerSuggester(ContentSuggester suggester) {
        contentSuggesters.put(suggester.getId(), suggester);
        LOGGER.info("Registered content suggester: " + suggester.getId());
        
        // Update suggestions for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateSuggestions(player);
        }
    }

    /**
     * Unregisters a content suggester.
     *
     * @param suggesterId The suggester ID
     */
    public void unregisterSuggester(String suggesterId) {
        contentSuggesters.remove(suggesterId);
        LOGGER.info("Unregistered content suggester: " + suggesterId);
        
        // Update suggestions for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateSuggestions(player);
        }
    }

    /**
     * Records an activity for a player.
     *
     * @param player The player
     * @param activity The activity
     * @param count The count
     */
    public void recordActivity(Player player, String activity, long count) {
        UUID playerId = player.getUniqueId();
        PlayerSession session = activeSessions.get(playerId);
        
        if (session != null) {
            session.recordActivity(activity, count);
        }
    }

    /**
     * Records a location visit for a player.
     *
     * @param player The player
     * @param location The location
     */
    public void recordLocationVisit(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        PlayerSession session = activeSessions.get(playerId);
        
        if (session != null) {
            session.recordLocationVisit(location);
        }
    }
    
    /**
     * Gets a player profile.
     *
     * @param playerId The player ID
     * @return The player profile, or null if not found
     */
    public PlayerProfile getPlayerProfile(UUID playerId) {
        return playerProfiles.get(playerId);
    }
    
    /**
     * Gets all player profiles.
     *
     * @return The player profiles
     */
    public Map<UUID, PlayerProfile> getAllPlayerProfiles() {
        return new HashMap<>(playerProfiles);
    }

    // Event handlers
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!running) return;
        
        createSession(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!running) return;
        
        endSession(event.getPlayer());
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!running) return;
        
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        
        recordActivity(player, "block.break." + material.name(), 1);
        recordActivity(player, "block.break", 1);
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!running) return;
        
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        
        recordActivity(player, "block.place." + material.name(), 1);
        recordActivity(player, "block.place", 1);
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!running) return;
        
        // Only track damage done by players
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        EntityType entityType = event.getEntityType();
        
        recordActivity(player, "combat." + entityType.name(), 1);
        recordActivity(player, "combat", 1);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!running) return;
        
        // Only track kills by players
        if (event.getEntity().getKiller() == null) return;
        
        Player player = event.getEntity().getKiller();
        EntityType entityType = event.getEntityType();
        
        recordActivity(player, "kill." + entityType.name(), 1);
        recordActivity(player, "kill", 1);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!running) return;
        
        // Only track significant movement (different block)
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null || (from.getBlockX() == to.getBlockX() && 
                from.getBlockY() == to.getBlockY() && 
                from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        
        Player player = event.getPlayer();
        recordLocationVisit(player, to);
    }
    
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!running) return;
        
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].substring(1).toLowerCase();
        
        recordActivity(player, "command." + command, 1);
        recordActivity(player, "command", 1);
    }

    /**
     * Class representing a player profile.
     */
    public static class PlayerProfile {
        private String playerName;
        private long totalPlayTime;
        private long lastSeen;
        private int sessionCount;
        private final Map<String, Long> activityCounters;
        private final Map<String, Double> activityPreferences;
        private final Map<String, Double> combatPreferences;
        private final Map<String, Double> buildingPreferences;
        private final Set<String> visitedLocations;
        
        /**
         * Creates a new player profile.
         *
         * @param playerName The player name
         */
        public PlayerProfile(String playerName) {
            this.playerName = playerName;
            this.totalPlayTime = 0;
            this.lastSeen = Instant.now().toEpochMilli();
            this.sessionCount = 0;
            this.activityCounters = new HashMap<>();
            this.activityPreferences = new HashMap<>();
            this.combatPreferences = new HashMap<>();
            this.buildingPreferences = new HashMap<>();
            this.visitedLocations = new HashSet<>();
        }
        
        /**
         * Creates a player profile from config.
         *
         * @param config The config
         * @return The player profile
         */
        public static PlayerProfile fromConfig(YamlConfiguration config) {
            PlayerProfile profile = new PlayerProfile(config.getString("playerName", "Unknown"));
            
            profile.totalPlayTime = config.getLong("totalPlayTime", 0);
            profile.lastSeen = config.getLong("lastSeen", 0);
            profile.sessionCount = config.getInt("sessionCount", 0);
            
            // Load activity counters
            if (config.contains("activityCounters")) {
                for (String key : config.getConfigurationSection("activityCounters").getKeys(false)) {
                    profile.activityCounters.put(key, config.getLong("activityCounters." + key));
                }
            }
            
            // Load preferences
            if (config.contains("activityPreferences")) {
                for (String key : config.getConfigurationSection("activityPreferences").getKeys(false)) {
                    profile.activityPreferences.put(key, config.getDouble("activityPreferences." + key));
                }
            }
            
            if (config.contains("combatPreferences")) {
                for (String key : config.getConfigurationSection("combatPreferences").getKeys(false)) {
                    profile.combatPreferences.put(key, config.getDouble("combatPreferences." + key));
                }
            }
            
            if (config.contains("buildingPreferences")) {
                for (String key : config.getConfigurationSection("buildingPreferences").getKeys(false)) {
                    profile.buildingPreferences.put(key, config.getDouble("buildingPreferences." + key));
                }
            }
            
            // Load visited locations
            profile.visitedLocations.addAll(config.getStringList("visitedLocations"));
            
            return profile;
        }
        
        /**
         * Converts the profile to config.
         *
         * @return The config
         */
        public YamlConfiguration toConfig() {
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("playerName", playerName);
            config.set("totalPlayTime", totalPlayTime);
            config.set("lastSeen", lastSeen);
            config.set("sessionCount", sessionCount);
            
            // Save activity counters
            for (Map.Entry<String, Long> entry : activityCounters.entrySet()) {
                config.set("activityCounters." + entry.getKey(), entry.getValue());
            }
            
            // Save preferences
            for (Map.Entry<String, Double> entry : activityPreferences.entrySet()) {
                config.set("activityPreferences." + entry.getKey(), entry.getValue());
            }
            
            for (Map.Entry<String, Double> entry : combatPreferences.entrySet()) {
                config.set("combatPreferences." + entry.getKey(), entry.getValue());
            }
            
            for (Map.Entry<String, Double> entry : buildingPreferences.entrySet()) {
                config.set("buildingPreferences." + entry.getKey(), entry.getValue());
            }
            
            // Save visited locations (limit to prevent excessive storage)
            List<String> locationsToSave = new ArrayList<>(visitedLocations);
            if (locationsToSave.size() > 100) {
                Collections.shuffle(locationsToSave);
                locationsToSave = locationsToSave.subList(0, 100);
            }
            config.set("visitedLocations", locationsToSave);
            
            return config;
        }
        
        /**
         * Gets the player name.
         *
         * @return The player name
         */
        public String getPlayerName() {
            return playerName;
        }
        
        /**
         * Gets the total play time.
         *
         * @return The total play time in milliseconds
         */
        public long getTotalPlayTime() {
            return totalPlayTime;
        }
        
        /**
         * Gets when the player was last seen.
         *
         * @return The last seen time in milliseconds since epoch
         */
        public long getLastSeen() {
            return lastSeen;
        }
        
        /**
         * Gets the number of sessions.
         *
         * @return The session count
         */
        public int getSessionCount() {
            return sessionCount;
        }
        
        /**
         * Gets the activity counters.
         *
         * @return The activity counters
         */
        public Map<String, Long> getActivityCounters() {
            return new HashMap<>(activityCounters);
        }
        
        /**
         * Gets the activity preferences.
         *
         * @return The activity preferences
         */
        public Map<String, Double> getActivityPreferences() {
            return new HashMap<>(activityPreferences);
        }
        
        /**
         * Gets the combat preferences.
         *
         * @return The combat preferences
         */
        public Map<String, Double> getCombatPreferences() {
            return new HashMap<>(combatPreferences);
        }
        
        /**
         * Gets the building preferences.
         *
         * @return The building preferences
         */
        public Map<String, Double> getBuildingPreferences() {
            return new HashMap<>(buildingPreferences);
        }
        
        /**
         * Gets the visited locations.
         *
         * @return The visited locations
         */
        public Set<String> getVisitedLocations() {
            return new HashSet<>(visitedLocations);
        }
    }

    /**
     * Class representing a player session.
     */
    private static class PlayerSession {
        private final Player player;
        private final Instant startTime;
        private final Map<String, Long> activityCounters;
        private final Set<String> visitedLocations;
        
        /**
         * Creates a new player session.
         *
         * @param player The player
         */
        public PlayerSession(Player player) {
            this.player = player;
            this.startTime = Instant.now();
            this.activityCounters = new HashMap<>();
            this.visitedLocations = new HashSet<>();
        }
        
        /**
         * Gets the session duration.
         *
         * @return The session duration
         */
        public Duration getSessionDuration() {
            return Duration.between(startTime, Instant.now());
        }
        
        /**
         * Records an activity.
         *
         * @param activity The activity
         * @param count The count
         */
        public void recordActivity(String activity, long count) {
            activityCounters.merge(activity, count, Long::sum);
        }
        
        /**
         * Records a location visit.
         *
         * @param location The location
         */
        public void recordLocationVisit(Location location) {
            // Round coordinates to reduce storage
            int x = (location.getBlockX() / 16) * 16;
            int y = (location.getBlockY() / 16) * 16;
            int z = (location.getBlockZ() / 16) * 16;
            
            String locString = location.getWorld().getName() + ":" + x + ":" + y + ":" + z;
            visitedLocations.add(locString);
        }
        
        /**
         * Gets the activity counters.
         *
         * @return The activity counters
         */
        public Map<String, Long> getActivityCounters() {
            return activityCounters;
        }
        
        /**
         * Gets the visited locations.
         *
         * @return The visited locations
         */
        public Set<String> getVisitedLocations() {
            return visitedLocations;
        }
    }

    /**
     * Interface for content suggesters.
     */
    public interface ContentSuggester {
        /**
         * Gets the suggester ID.
         *
         * @return The suggester ID
         */
        String getId();
        
        /**
         * Suggests content for a player.
         *
         * @param profile The player profile
         * @return The suggested content IDs
         */
        Set<String> suggestContent(PlayerProfile profile);
    }

    /**
     * Abstract implementation of a content suggester.
     */
    public abstract static class AbstractContentSuggester implements ContentSuggester {
        private final String id;
        
        /**
         * Creates a new content suggester.
         *
         * @param id The suggester ID
         */
        public AbstractContentSuggester(String id) {
            this.id = id;
        }
        
        @Override
        public String getId() {
            return id;
        }
    }
} 
