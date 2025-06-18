package com.essentialscore.api.gaming;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * System that manages seasonal events and temporary game content.
 */
public class SeasonalEventManager implements Listener {
    private static final Logger LOGGER = Logger.getLogger(SeasonalEventManager.class.getName());

    private final Plugin plugin;
    private final Map<String, SeasonalEvent> registeredEvents;
    private final Map<String, List<EventReward>> playerRewards;
    private final File dataFolder;
    private final File configFile;

    private boolean running;
    private BukkitTask checkTask;
    private YamlConfiguration config;

    /**
     * Creates a new seasonal event manager.
     *
     * @param plugin The plugin
     */
    public SeasonalEventManager(Plugin plugin) {
        this.plugin = plugin;
        this.registeredEvents = new ConcurrentHashMap<>();
        this.playerRewards = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "seasonal_events");
        this.configFile = new File(plugin.getDataFolder(), "seasonal_events.yml");
        this.running = false;
    }

    /**
     * Starts the seasonal event manager.
     */
    public void start() {
        if (running) return;

        LOGGER.info("Starting seasonal event manager");

        // Create data directory if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Load configuration
        loadConfig();

        // Load event data
        loadEvents();

        // Schedule event check task
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkEvents, 20L, 20L * 60 * 30); // Check every 30 minutes

        running = true;
    }

    /**
     * Stops the seasonal event manager.
     */
    public void stop() {
        if (!running) return;

        LOGGER.info("Stopping seasonal event manager");

        // Cancel scheduled tasks
        if (checkTask != null) {
            checkTask.cancel();
        }

        // End active events
        List<SeasonalEvent> activeEvents = getActiveEvents();
        for (SeasonalEvent event : activeEvents) {
            endEvent(event);
        }

        // Save event data
        saveEvents();

        running = false;
    }

    /**
     * Loads the configuration.
     */
    private void loadConfig() {
        try {
            if (!configFile.exists()) {
                // Create default config
                config = new YamlConfiguration();
                
                // Default predefined events
                ConfigurationSection events = config.createSection("predefined_events");
                
                // Winter Holiday
                ConfigurationSection winterHoliday = events.createSection("winter_holiday");
                winterHoliday.set("name", "Winter Holiday");
                winterHoliday.set("description", "Celebrate the winter season with special rewards and decorations!");
                winterHoliday.set("start_month", 12);
                winterHoliday.set("start_day", 15);
                winterHoliday.set("end_month", 1);
                winterHoliday.set("end_day", 5);
                
                // Summer Festival
                ConfigurationSection summerFestival = events.createSection("summer_festival");
                summerFestival.set("name", "Summer Festival");
                summerFestival.set("description", "Join the fun with summer activities and special items!");
                summerFestival.set("start_month", 6);
                summerFestival.set("start_day", 21);
                summerFestival.set("end_month", 7);
                summerFestival.set("end_day", 21);
                
                // Halloween
                ConfigurationSection halloween = events.createSection("halloween");
                halloween.set("name", "Halloween");
                halloween.set("description", "Spooky season is here with haunted challenges and rewards!");
                halloween.set("start_month", 10);
                halloween.set("start_day", 24);
                halloween.set("end_month", 11);
                halloween.set("end_day", 2);
                
                // Save the config
                config.save(configFile);
            } else {
                config = YamlConfiguration.loadConfiguration(configFile);
            }
            
            LOGGER.info("Loaded seasonal events configuration");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error creating default config", e);
            config = new YamlConfiguration();
        }
    }

    /**
     * Loads event data from disk.
     */
    private void loadEvents() {
        // Register predefined events from config
        if (config.contains("predefined_events")) {
            ConfigurationSection events = config.getConfigurationSection("predefined_events");
            
            for (String eventId : events.getKeys(false)) {
                ConfigurationSection eventConfig = events.getConfigurationSection(eventId);
                
                // Create event builder
                SeasonalEvent.Builder builder = new SeasonalEvent.Builder(eventId)
                        .name(eventConfig.getString("name"))
                        .description(eventConfig.getString("description"));
                
                // Set dates based on month/day
                int startMonth = eventConfig.getInt("start_month");
                int startDay = eventConfig.getInt("start_day");
                int endMonth = eventConfig.getInt("end_month");
                int endDay = eventConfig.getInt("end_day");
                
                // Set start and end for current year
                LocalDate now = LocalDate.now();
                int year = now.getYear();
                
                // If we're past this year's event, use next year's dates
                LocalDate eventStart = LocalDate.of(year, startMonth, startDay);
                if (now.isAfter(eventStart) && now.isAfter(LocalDate.of(year, endMonth, endDay))) {
                    eventStart = LocalDate.of(year + 1, startMonth, startDay);
                }
                
                // Handle events that span year boundary (e.g., Dec-Jan)
                LocalDate eventEnd;
                if (endMonth < startMonth || (endMonth == startMonth && endDay < startDay)) {
                    eventEnd = LocalDate.of(year + 1, endMonth, endDay);
                } else {
                    eventEnd = LocalDate.of(year, endMonth, endDay);
                }
                
                builder.startDate(eventStart);
                builder.endDate(eventEnd);
                
                // Add custom data if present
                if (eventConfig.contains("custom_data")) {
                    ConfigurationSection customData = eventConfig.getConfigurationSection("custom_data");
                    for (String key : customData.getKeys(false)) {
                        builder.customData(key, customData.get(key));
                    }
                }
                
                // Register the event
                SeasonalEvent event = builder.build();
                registerEvent(event);
                
                LOGGER.info("Registered predefined event: " + event.getName() + 
                        " (" + event.getStartDate() + " to " + event.getEndDate() + ")");
            }
        }
        
        // Load custom events and player rewards
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null) return;
        
        // Load player rewards
        File rewardsFile = new File(dataFolder, "player_rewards.yml");
        if (rewardsFile.exists()) {
            try {
                YamlConfiguration rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
                
                for (String playerUuid : rewardsConfig.getKeys(false)) {
                    ConfigurationSection playerSection = rewardsConfig.getConfigurationSection(playerUuid);
                    List<String> rewardStrings = playerSection.getStringList("rewards");
                    
                    List<EventReward> rewards = new ArrayList<>();
                    for (String rewardString : rewardStrings) {
                        String[] parts = rewardString.split(":");
                        if (parts.length >= 2) {
                            rewards.add(new EventReward(parts[0], parts[1]));
                        }
                    }
                    
                    playerRewards.put(playerUuid, rewards);
                }
                
                LOGGER.info("Loaded rewards for " + playerRewards.size() + " players");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error loading player rewards", e);
            }
        }
        
        // Load custom events
        for (File file : files) {
            if (file.getName().equals("player_rewards.yml")) continue;
            
            try {
                YamlConfiguration eventConfig = YamlConfiguration.loadConfiguration(file);
                String eventId = file.getName().replace(".yml", "");
                
                // Skip if this event ID is already registered as a predefined event
                if (registeredEvents.containsKey(eventId)) continue;
                
                // Create event from config
                SeasonalEvent.Builder builder = new SeasonalEvent.Builder(eventId)
                        .name(eventConfig.getString("name"))
                        .description(eventConfig.getString("description"));
                
                // Parse dates
                String startDateStr = eventConfig.getString("start_date");
                String endDateStr = eventConfig.getString("end_date");
                
                if (startDateStr != null && endDateStr != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
                    LocalDate startDate = LocalDate.parse(startDateStr, formatter);
                    LocalDate endDate = LocalDate.parse(endDateStr, formatter);
                    
                    builder.startDate(startDate);
                    builder.endDate(endDate);
                }
                
                // Add event handlers
                if (eventConfig.contains("on_start")) {
                    // In a real implementation, you would load and set actual event handlers
                    // Execute event handler logic
                    builder.onStart(event -> LOGGER.info("Event started: " + event.getName()));
                }
                
                if (eventConfig.contains("on_end")) {
                    builder.onEnd(event -> LOGGER.info("Event ended: " + event.getName()));
                }
                
                // Add custom data
                if (eventConfig.contains("custom_data")) {
                    ConfigurationSection customData = eventConfig.getConfigurationSection("custom_data");
                    for (String key : customData.getKeys(false)) {
                        builder.customData(key, customData.get(key));
                    }
                }
                
                // Register the event
                SeasonalEvent event = builder.build();
                registerEvent(event);
                
                LOGGER.info("Loaded custom event: " + event.getName());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error loading event: " + file.getName(), e);
            }
        }
    }

    /**
     * Saves event data to disk.
     */
    private void saveEvents() {
        // Save custom events
        for (SeasonalEvent event : registeredEvents.values()) {
            // Skip predefined events
            if (config.contains("predefined_events." + event.getId())) continue;
            
            try {
                YamlConfiguration eventConfig = new YamlConfiguration();
                
                eventConfig.set("name", event.getName());
                eventConfig.set("description", event.getDescription());
                eventConfig.set("start_date", event.getStartDate().toString());
                eventConfig.set("end_date", event.getEndDate().toString());
                eventConfig.set("active", event.isActive());
                
                // Save custom data
                ConfigurationSection customData = eventConfig.createSection("custom_data");
                for (Map.Entry<String, Object> entry : event.getCustomData().entrySet()) {
                    customData.set(entry.getKey(), entry.getValue());
                }
                
                // Save to file
                File eventFile = new File(dataFolder, event.getId() + ".yml");
                eventConfig.save(eventFile);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error saving event: " + event.getId(), e);
            }
        }
        
        // Save player rewards
        try {
            YamlConfiguration rewardsConfig = new YamlConfiguration();
            
            for (Map.Entry<String, List<EventReward>> entry : playerRewards.entrySet()) {
                String playerUuid = entry.getKey();
                List<EventReward> rewards = entry.getValue();
                
                ConfigurationSection playerSection = rewardsConfig.createSection(playerUuid);
                List<String> rewardStrings = rewards.stream()
                        .map(reward -> reward.getEventId() + ":" + reward.getRewardId())
                        .collect(Collectors.toList());
                
                playerSection.set("rewards", rewardStrings);
            }
            
            // Save to file
            File rewardsFile = new File(dataFolder, "player_rewards.yml");
            rewardsConfig.save(rewardsFile);
            
            LOGGER.info("Saved rewards for " + playerRewards.size() + " players");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error saving player rewards", e);
        }
    }

    /**
     * Checks for events that should start or end.
     */
    private void checkEvents() {
        if (!running) return;
        
        LocalDate today = LocalDate.now();
        
        for (SeasonalEvent event : registeredEvents.values()) {
            // Check if event should start
            if (!event.isActive() && !today.isBefore(event.getStartDate()) && today.isBefore(event.getEndDate())) {
                startEvent(event);
            }
            
            // Check if event should end
            if (event.isActive() && !today.isBefore(event.getEndDate())) {
                endEvent(event);
            }
        }
    }

    /**
     * Starts an event.
     *
     * @param event The event
     */
    private void startEvent(SeasonalEvent event) {
        if (event.isActive()) return;
        
        LOGGER.info("Starting event: " + event.getName());
        
        // Set active state
        event.setActive(true);
        
        // Announce to online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("§6[Event] §e" + event.getName() + " §6has begun! " + event.getDescription());
        }
        
        // Execute event start handler
        if (event.getOnStart() != null) {
            try {
                event.getOnStart().accept(event);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in event start handler: " + event.getId(), e);
            }
        }
    }

    /**
     * Ends an event.
     *
     * @param event The event
     */
    private void endEvent(SeasonalEvent event) {
        if (!event.isActive()) return;
        
        LOGGER.info("Ending event: " + event.getName());
        
        // Set inactive state
        event.setActive(false);
        
        // Announce to online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("§6[Event] §e" + event.getName() + " §6has ended! Thank you for participating.");
        }
        
        // Execute event end handler
        if (event.getOnEnd() != null) {
            try {
                event.getOnEnd().accept(event);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in event end handler: " + event.getId(), e);
            }
        }
        
        // Update event dates for next occurrence (for recurring events)
        if (event.isRecurring()) {
            updateEventDates(event);
        }
    }

    /**
     * Updates event dates for the next occurrence.
     *
     * @param event The event
     */
    private void updateEventDates(SeasonalEvent event) {
        // Get current dates
        LocalDate startDate = event.getStartDate();
        LocalDate endDate = event.getEndDate();
        
        // Calculate duration
        long duration = ChronoUnit.DAYS.between(startDate, endDate);
        
        // Update to next year
        LocalDate newStartDate = startDate.plusYears(1);
        LocalDate newEndDate = newStartDate.plusDays(duration);
        
        // Update event
        event.setStartDate(newStartDate);
        event.setEndDate(newEndDate);
        
        LOGGER.info("Updated event dates for: " + event.getName() + 
                " (Next: " + newStartDate + " to " + newEndDate + ")");
    }

    /**
     * Registers an event.
     *
     * @param event The event
     */
    public void registerEvent(SeasonalEvent event) {
        registeredEvents.put(event.getId(), event);
        LOGGER.info("Registered event: " + event.getName());
        
        // Check if the event should be active now
        LocalDate today = LocalDate.now();
        if (!today.isBefore(event.getStartDate()) && today.isBefore(event.getEndDate())) {
            startEvent(event);
        }
    }

    /**
     * Unregisters an event.
     *
     * @param eventId The event ID
     */
    public void unregisterEvent(String eventId) {
        SeasonalEvent event = registeredEvents.remove(eventId);
        
        if (event != null) {
            // End the event if it's active
            if (event.isActive()) {
                endEvent(event);
            }
            
            LOGGER.info("Unregistered event: " + event.getName());
        }
    }

    /**
     * Gets all active events.
     *
     * @return The active events
     */
    public List<SeasonalEvent> getActiveEvents() {
        return registeredEvents.values().stream()
                .filter(SeasonalEvent::isActive)
                .collect(Collectors.toList());
    }

    /**
     * Gets all registered events.
     *
     * @return The registered events
     */
    public Collection<SeasonalEvent> getAllEvents() {
        return new ArrayList<>(registeredEvents.values());
    }

    /**
     * Gets an event by ID.
     *
     * @param eventId The event ID
     * @return The event, or null if not found
     */
    public SeasonalEvent getEvent(String eventId) {
        return registeredEvents.get(eventId);
    }

    /**
     * Gets a player's reward history.
     *
     * @param playerUuid The player UUID
     * @return The reward history
     */
    public List<EventReward> getPlayerRewards(String playerUuid) {
        return new ArrayList<>(playerRewards.getOrDefault(playerUuid, Collections.emptyList()));
    }

    /**
     * Adds a reward to a player's history.
     *
     * @param playerUuid The player UUID
     * @param eventId The event ID
     * @param rewardId The reward ID
     */
    public void addPlayerReward(String playerUuid, String eventId, String rewardId) {
        List<EventReward> rewards = playerRewards.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        rewards.add(new EventReward(eventId, rewardId));
        
        LOGGER.fine("Added reward " + rewardId + " from event " + eventId + " to player " + playerUuid);
    }

    /**
     * Checks if a player has received a specific reward.
     *
     * @param playerUuid The player UUID
     * @param eventId The event ID
     * @param rewardId The reward ID
     * @return true if the player has received the reward
     */
    public boolean hasPlayerReceivedReward(String playerUuid, String eventId, String rewardId) {
        List<EventReward> rewards = playerRewards.getOrDefault(playerUuid, Collections.emptyList());
        
        return rewards.stream()
                .anyMatch(reward -> reward.getEventId().equals(eventId) && reward.getRewardId().equals(rewardId));
    }

    /**
     * Creates a new event builder.
     *
     * @param eventId The event ID
     * @return The event builder
     */
    public static SeasonalEvent.Builder createEvent(String eventId) {
        return new SeasonalEvent.Builder(eventId);
    }

    // Event handlers
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!running) return;
        
        // Notify player of active events
        Player player = event.getPlayer();
        List<SeasonalEvent> activeEvents = getActiveEvents();
        
        if (!activeEvents.isEmpty()) {
            player.sendMessage("§6===== §eActive Events §6=====");
            
            for (SeasonalEvent seasonalEvent : activeEvents) {
                player.sendMessage("§e• " + seasonalEvent.getName() + ": §f" + seasonalEvent.getDescription());
            }
        }
    }

    /**
     * Class representing a seasonal event.
     */
    public static class SeasonalEvent {
        private final String id;
        private final String name;
        private final String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private final boolean recurring;
        private final Consumer<SeasonalEvent> onStart;
        private final Consumer<SeasonalEvent> onEnd;
        private final Map<String, Object> customData;
        
        private boolean active;
        
        private SeasonalEvent(Builder builder) {
            this.id = builder.id;
            this.name = builder.name;
            this.description = builder.description;
            this.startDate = builder.startDate;
            this.endDate = builder.endDate;
            this.recurring = builder.recurring;
            this.onStart = builder.onStart;
            this.onEnd = builder.onEnd;
            this.customData = new HashMap<>(builder.customData);
            this.active = false;
        }
        
        /**
         * Gets the event ID.
         *
         * @return The event ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Gets the event name.
         *
         * @return The event name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Gets the event description.
         *
         * @return The event description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the start date.
         *
         * @return The start date
         */
        public LocalDate getStartDate() {
            return startDate;
        }
        
        /**
         * Sets the start date.
         *
         * @param startDate The start date
         */
        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }
        
        /**
         * Gets the end date.
         *
         * @return The end date
         */
        public LocalDate getEndDate() {
            return endDate;
        }
        
        /**
         * Sets the end date.
         *
         * @param endDate The end date
         */
        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
        
        /**
         * Checks if the event is recurring.
         *
         * @return true if the event is recurring
         */
        public boolean isRecurring() {
            return recurring;
        }
        
        /**
         * Checks if the event is active.
         *
         * @return true if the event is active
         */
        public boolean isActive() {
            return active;
        }
        
        /**
         * Sets the active state.
         *
         * @param active The active state
         */
        public void setActive(boolean active) {
            this.active = active;
        }
        
        /**
         * Gets the on-start handler.
         *
         * @return The on-start handler
         */
        public Consumer<SeasonalEvent> getOnStart() {
            return onStart;
        }
        
        /**
         * Gets the on-end handler.
         *
         * @return The on-end handler
         */
        public Consumer<SeasonalEvent> getOnEnd() {
            return onEnd;
        }
        
        /**
         * Gets custom data.
         *
         * @param key The data key
         * @return The data value
         */
        public Object getCustomData(String key) {
            return customData.get(key);
        }
        
        /**
         * Gets all custom data.
         *
         * @return The custom data
         */
        public Map<String, Object> getCustomData() {
            return new HashMap<>(customData);
        }
        
        /**
         * Builder for creating seasonal events.
         */
        public static class Builder {
            private final String id;
            private String name;
            private String description;
            private LocalDate startDate;
            private LocalDate endDate;
            private boolean recurring;
            private Consumer<SeasonalEvent> onStart;
            private Consumer<SeasonalEvent> onEnd;
            private final Map<String, Object> customData;
            
            /**
             * Creates a new builder.
             *
             * @param id The event ID
             */
            public Builder(String id) {
                this.id = id;
                this.name = id;
                this.description = "";
                this.startDate = LocalDate.now();
                this.endDate = LocalDate.now().plusDays(7);
                this.recurring = false;
                this.customData = new HashMap<>();
            }
            
            /**
             * Sets the event name.
             *
             * @param name The event name
             * @return The builder
             */
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            /**
             * Sets the event description.
             *
             * @param description The event description
             * @return The builder
             */
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            /**
             * Sets the start date.
             *
             * @param startDate The start date
             * @return The builder
             */
            public Builder startDate(LocalDate startDate) {
                this.startDate = startDate;
                return this;
            }
            
            /**
             * Sets the end date.
             *
             * @param endDate The end date
             * @return The builder
             */
            public Builder endDate(LocalDate endDate) {
                this.endDate = endDate;
                return this;
            }
            
            /**
             * Sets whether the event is recurring.
             *
             * @param recurring true if the event is recurring
             * @return The builder
             */
            public Builder recurring(boolean recurring) {
                this.recurring = recurring;
                return this;
            }
            
            /**
             * Sets the on-start handler.
             *
             * @param onStart The on-start handler
             * @return The builder
             */
            public Builder onStart(Consumer<SeasonalEvent> onStart) {
                this.onStart = onStart;
                return this;
            }
            
            /**
             * Sets the on-end handler.
             *
             * @param onEnd The on-end handler
             * @return The builder
             */
            public Builder onEnd(Consumer<SeasonalEvent> onEnd) {
                this.onEnd = onEnd;
                return this;
            }
            
            /**
             * Adds custom data.
             *
             * @param key The data key
             * @param value The data value
             * @return The builder
             */
            public Builder customData(String key, Object value) {
                this.customData.put(key, value);
                return this;
            }
            
            /**
             * Builds the seasonal event.
             *
             * @return The seasonal event
             */
            public SeasonalEvent build() {
                return new SeasonalEvent(this);
            }
        }
    }

    /**
     * Class representing an event reward.
     */
    public static class EventReward {
        private final String eventId;
        private final String rewardId;
        
        /**
         * Creates a new event reward.
         *
         * @param eventId The event ID
         * @param rewardId The reward ID
         */
        public EventReward(String eventId, String rewardId) {
            this.eventId = eventId;
            this.rewardId = rewardId;
        }
        
        /**
         * Gets the event ID.
         *
         * @return The event ID
         */
        public String getEventId() {
            return eventId;
        }
        
        /**
         * Gets the reward ID.
         *
         * @return The reward ID
         */
        public String getRewardId() {
            return rewardId;
        }
    }
} 