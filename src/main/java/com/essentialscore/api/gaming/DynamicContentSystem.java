package com.essentialscore.api.gaming;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * System that dynamically adjusts game content based on server population, time, and events.
 */
public class DynamicContentSystem {
    private static final Logger LOGGER = Logger.getLogger(DynamicContentSystem.class.getName());

    private final Plugin plugin;
    private final Map<String, DynamicContent> contentRegistry;
    private final Map<ContentTrigger, List<String>> triggerMap;
    private final Map<String, Object> serverState;

    private boolean running;
    private int schedulerTaskId;

    /**
     * Creates a new dynamic content system.
     *
     * @param plugin The plugin
     */
    public DynamicContentSystem(Plugin plugin) {
        this.plugin = plugin;
        this.contentRegistry = new ConcurrentHashMap<>();
        this.triggerMap = new ConcurrentHashMap<>();
        this.serverState = new ConcurrentHashMap<>();
        this.running = false;
    }

    /**
     * Starts the dynamic content system.
     */
    public void start() {
        if (running) return;

        LOGGER.info("Starting dynamic content system");

        // Start state monitoring
        updateServerState();

        // Schedule regular updates
        schedulerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            updateServerState();
            evaluateTriggers();
        }, 20L, 20L * 60); // Check every minute

        running = true;
    }

    /**
     * Stops the dynamic content system.
     */
    public void stop() {
        if (!running) return;

        LOGGER.info("Stopping dynamic content system");

        // Cancel scheduled task
        Bukkit.getScheduler().cancelTask(schedulerTaskId);

        running = false;
    }

    /**
     * Updates the server state.
     */
    private void updateServerState() {
        // Player count
        int playerCount = Bukkit.getOnlinePlayers().size();
        serverState.put("playerCount", playerCount);
        serverState.put("populationLevel", getPopulationLevel(playerCount));

        // Time of day
        LocalDateTime now = LocalDateTime.now();
        serverState.put("hour", now.getHour());
        serverState.put("minute", now.getMinute());
        serverState.put("dayOfWeek", now.getDayOfWeek().getValue());
        serverState.put("timeOfDay", getTimeOfDay(now.toLocalTime()));

        // Game world time
        for (World world : Bukkit.getWorlds()) {
            long time = world.getTime();
            serverState.put("worldTime_" + world.getName(), time);
            serverState.put("worldTimeOfDay_" + world.getName(), getWorldTimeOfDay(time));
        }

        // Weather
        for (World world : Bukkit.getWorlds()) {
            boolean isStorming = world.hasStorm();
            boolean isThundering = world.isThundering();
            serverState.put("isStorming_" + world.getName(), isStorming);
            serverState.put("isThundering_" + world.getName(), isThundering);
        }
    }

    /**
     * Gets the population level based on player count.
     *
     * @param playerCount The player count
     * @return The population level
     */
    private PopulationLevel getPopulationLevel(int playerCount) {
        int maxPlayers = Bukkit.getMaxPlayers();
        
        if (playerCount == 0) {
            return PopulationLevel.EMPTY;
        } else if (playerCount < maxPlayers * 0.25) {
            return PopulationLevel.LOW;
        } else if (playerCount < maxPlayers * 0.75) {
            return PopulationLevel.MEDIUM;
        } else {
            return PopulationLevel.HIGH;
        }
    }

    /**
     * Gets the time of day.
     *
     * @param time The time
     * @return The time of day
     */
    private TimeOfDay getTimeOfDay(LocalTime time) {
        int hour = time.getHour();
        
        if (hour >= 5 && hour < 12) {
            return TimeOfDay.MORNING;
        } else if (hour >= 12 && hour < 17) {
            return TimeOfDay.AFTERNOON;
        } else if (hour >= 17 && hour < 22) {
            return TimeOfDay.EVENING;
        } else {
            return TimeOfDay.NIGHT;
        }
    }

    /**
     * Gets the time of day in the game world.
     *
     * @param time The game world time
     * @return The time of day
     */
    private WorldTimeOfDay getWorldTimeOfDay(long time) {
        // Minecraft time: 0 = dawn, 6000 = noon, 12000 = dusk, 18000 = midnight
        if (time < 1000 || time > 23000) {
            return WorldTimeOfDay.DAWN;
        } else if (time < 6000) {
            return WorldTimeOfDay.MORNING;
        } else if (time < 11000) {
            return WorldTimeOfDay.AFTERNOON;
        } else if (time < 13000) {
            return WorldTimeOfDay.DUSK;
        } else if (time < 18000) {
            return WorldTimeOfDay.EVENING;
        } else {
            return WorldTimeOfDay.NIGHT;
        }
    }

    /**
     * Evaluates triggers and activates/deactivates content.
     */
    private void evaluateTriggers() {
        for (Map.Entry<ContentTrigger, List<String>> entry : triggerMap.entrySet()) {
            ContentTrigger trigger = entry.getKey();
            List<String> contentIds = entry.getValue();
            
            boolean shouldActivate = trigger.test(serverState);
            
            for (String contentId : contentIds) {
                DynamicContent content = contentRegistry.get(contentId);
                if (content != null) {
                    if (shouldActivate && !content.isActive()) {
                        content.activate();
                        LOGGER.info("Activated content: " + contentId);
                    } else if (!shouldActivate && content.isActive()) {
                        content.deactivate();
                        LOGGER.info("Deactivated content: " + contentId);
                    }
                }
            }
        }
    }

    /**
     * Registers dynamic content.
     *
     * @param content The content
     * @param trigger The trigger
     */
    public void registerContent(DynamicContent content, ContentTrigger trigger) {
        contentRegistry.put(content.getId(), content);
        
        // Add to trigger map
        List<String> contentIds = triggerMap.computeIfAbsent(trigger, k -> new ArrayList<>());
        contentIds.add(content.getId());
        
        LOGGER.info("Registered dynamic content: " + content.getId());
    }

    /**
     * Unregisters dynamic content.
     *
     * @param contentId The content ID
     */
    public void unregisterContent(String contentId) {
        DynamicContent content = contentRegistry.remove(contentId);
        
        if (content != null) {
            // Ensure content is deactivated
            if (content.isActive()) {
                content.deactivate();
            }
            
            // Remove from trigger map
            for (List<String> contentIds : triggerMap.values()) {
                contentIds.remove(contentId);
            }
            
            LOGGER.info("Unregistered dynamic content: " + contentId);
        }
    }

    /**
     * Gets dynamic content by ID.
     *
     * @param contentId The content ID
     * @return The content, or null if not found
     */
    public DynamicContent getContent(String contentId) {
        return contentRegistry.get(contentId);
    }

    /**
     * Gets all registered content.
     *
     * @return The registered content
     */
    public Map<String, DynamicContent> getAllContent() {
        return new HashMap<>(contentRegistry);
    }

    /**
     * Gets the server state.
     *
     * @return The server state
     */
    public Map<String, Object> getServerState() {
        return new HashMap<>(serverState);
    }
    
    /**
     * Creates a time-based trigger.
     * 
     * @param timeOfDay The time of day
     * @return The trigger
     */
    public static ContentTrigger createTimeTrigger(TimeOfDay timeOfDay) {
        return state -> timeOfDay == state.get("timeOfDay");
    }
    
    /**
     * Creates a population-based trigger.
     * 
     * @param populationLevel The population level
     * @return The trigger
     */
    public static ContentTrigger createPopulationTrigger(PopulationLevel populationLevel) {
        return state -> populationLevel == state.get("populationLevel");
    }
    
    /**
     * Creates a weather-based trigger.
     * 
     * @param worldName The world name
     * @param requiresStorm Whether storm is required
     * @param requiresThunder Whether thunder is required
     * @return The trigger
     */
    public static ContentTrigger createWeatherTrigger(String worldName, boolean requiresStorm, boolean requiresThunder) {
        return state -> {
            if (requiresStorm && !(boolean)state.getOrDefault("isStorming_" + worldName, false)) {
                return false;
            }
            if (requiresThunder && !(boolean)state.getOrDefault("isThundering_" + worldName, false)) {
                return false;
            }
            return true;
        };
    }
    
    /**
     * Creates a composite trigger that requires all conditions to be met.
     * 
     * @param triggers The triggers
     * @return The composite trigger
     */
    public static ContentTrigger and(ContentTrigger... triggers) {
        return state -> {
            for (ContentTrigger trigger : triggers) {
                if (!trigger.test(state)) {
                    return false;
                }
            }
            return true;
        };
    }
    
    /**
     * Creates a composite trigger that requires any condition to be met.
     * 
     * @param triggers The triggers
     * @return The composite trigger
     */
    public static ContentTrigger or(ContentTrigger... triggers) {
        return state -> {
            for (ContentTrigger trigger : triggers) {
                if (trigger.test(state)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Interface for dynamic content.
     */
    public interface DynamicContent {
        /**
         * Gets the content ID.
         *
         * @return The content ID
         */
        String getId();

        /**
         * Checks if the content is active.
         *
         * @return true if the content is active
         */
        boolean isActive();

        /**
         * Activates the content.
         */
        void activate();

        /**
         * Deactivates the content.
         */
        void deactivate();
    }

    /**
     * Interface for content triggers.
     */
    @FunctionalInterface
    public interface ContentTrigger {
        /**
         * Tests if the trigger condition is met.
         *
         * @param state The server state
         * @return true if the condition is met
         */
        boolean test(Map<String, Object> state);
    }

    /**
     * Enum for population levels.
     */
    public enum PopulationLevel {
        EMPTY, LOW, MEDIUM, HIGH
    }

    /**
     * Enum for times of day.
     */
    public enum TimeOfDay {
        MORNING, AFTERNOON, EVENING, NIGHT
    }

    /**
     * Enum for game world times of day.
     */
    public enum WorldTimeOfDay {
        DAWN, MORNING, AFTERNOON, DUSK, EVENING, NIGHT
    }
    
    /**
     * Abstract implementation of dynamic content.
     */
    public abstract static class AbstractDynamicContent implements DynamicContent {
        private final String id;
        private boolean active;
        
        /**
         * Creates new dynamic content.
         *
         * @param id The content ID
         */
        public AbstractDynamicContent(String id) {
            this.id = id;
            this.active = false;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
        
        @Override
        public void activate() {
            if (!active) {
                active = true;
                onActivate();
            }
        }
        
        @Override
        public void deactivate() {
            if (active) {
                active = false;
                onDeactivate();
            }
        }
        
        /**
         * Called when the content is activated.
         */
        protected abstract void onActivate();
        
        /**
         * Called when the content is deactivated.
         */
        protected abstract void onDeactivate();
    }
} 
