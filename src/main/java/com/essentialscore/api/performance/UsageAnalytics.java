package com.essentialscore.api.performance;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Tracks and analyzes feature usage across modules.
 */
public class UsageAnalytics {
    private static final Logger LOGGER = Logger.getLogger(UsageAnalytics.class.getName());
    
    private final ProfilingSystem profilingSystem;
    private final Map<String, FeatureUsage> featureUsages;
    private final Map<UUID, Map<String, Long>> playerUsages;
    private final Map<String, Map<String, Integer>> dailyUsageStats;
    private final List<Consumer<UsageReport>> reportListeners;
    private final ScheduledExecutorService scheduledExecutor;
    
    /**
     * Creates a new usage analytics system.
     *
     * @param profilingSystem The profiling system
     */
    public UsageAnalytics(ProfilingSystem profilingSystem) {
        this.profilingSystem = profilingSystem;
        this.featureUsages = new ConcurrentHashMap<>();
        this.playerUsages = new ConcurrentHashMap<>();
        this.dailyUsageStats = new ConcurrentHashMap<>();
        this.reportListeners = new ArrayList<>();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Schedule daily usage report generation
        scheduleDailyReports();
    }
    
    /**
     * Schedules daily usage report generation.
     */
    private void scheduleDailyReports() {
        scheduledExecutor.scheduleAtFixedRate(
            this::generateDailyReport,
            calculateInitialDelay(),
            24 * 60 * 60,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Calculates the initial delay until the next report.
     *
     * @return The initial delay in seconds
     */
    private long calculateInitialDelay() {
        // Schedule for midnight
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Instant midnight = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Duration duration = Duration.between(Instant.now(), midnight);
        return duration.getSeconds();
    }
    
    /**
     * Shuts down the usage analytics system.
     */
    public void shutdown() {
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Records a feature usage.
     *
     * @param moduleId The module ID
     * @param featureId The feature ID
     */
    public void recordFeatureUsage(String moduleId, String featureId) {
        String key = moduleId + "." + featureId;
        
        // Update feature usage
        FeatureUsage usage = featureUsages.computeIfAbsent(
            key,
            k -> new FeatureUsage(moduleId, featureId)
        );
        usage.incrementCount();
        
        // Record metric
        profilingSystem.incrementCounter("feature.usage." + key);
        
        // Update daily stats
        String today = LocalDate.now().toString();
        Map<String, Integer> dailyStats = dailyUsageStats.computeIfAbsent(
            today,
            k -> new ConcurrentHashMap<>()
        );
        dailyStats.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
    }
    
    /**
     * Records a feature usage by a player.
     *
     * @param player The player
     * @param moduleId The module ID
     * @param featureId The feature ID
     */
    public void recordPlayerFeatureUsage(Player player, String moduleId, String featureId) {
        UUID playerId = player.getUniqueId();
        String key = moduleId + "." + featureId;
        
        // Update player usage
        Map<String, Long> playerFeatureUsages = playerUsages.computeIfAbsent(
            playerId,
            k -> new ConcurrentHashMap<>()
        );
        playerFeatureUsages.put(key, System.currentTimeMillis());
        
        // Record general feature usage
        recordFeatureUsage(moduleId, featureId);
    }
    
    /**
     * Gets a feature usage.
     *
     * @param moduleId The module ID
     * @param featureId The feature ID
     * @return The feature usage, or null if not found
     */
    public FeatureUsage getFeatureUsage(String moduleId, String featureId) {
        return featureUsages.get(moduleId + "." + featureId);
    }
    
    /**
     * Gets all feature usages.
     *
     * @return An unmodifiable map of feature usages
     */
    public Map<String, FeatureUsage> getAllFeatureUsages() {
        return Collections.unmodifiableMap(featureUsages);
    }
    
    /**
     * Gets feature usages for a module.
     *
     * @param moduleId The module ID
     * @return A map of feature usages for the module
     */
    public Map<String, FeatureUsage> getModuleFeatureUsages(String moduleId) {
        Map<String, FeatureUsage> moduleUsages = new HashMap<>();
        
        for (Map.Entry<String, FeatureUsage> entry : featureUsages.entrySet()) {
            if (entry.getValue().getModuleId().equals(moduleId)) {
                moduleUsages.put(entry.getKey(), entry.getValue());
            }
        }
        
        return moduleUsages;
    }
    
    /**
     * Gets a player's feature usages.
     *
     * @param playerId The player ID
     * @return A map of feature IDs to last usage times
     */
    public Map<String, Long> getPlayerFeatureUsages(UUID playerId) {
        Map<String, Long> usages = playerUsages.get(playerId);
        return usages != null ? Collections.unmodifiableMap(usages) : Collections.emptyMap();
    }
    
    /**
     * Gets the top features by usage count.
     *
     * @param limit The maximum number of features to return
     * @return A list of feature usages sorted by count
     */
    public List<FeatureUsage> getTopFeatures(int limit) {
        return featureUsages.values().stream()
            .sorted(Comparator.comparingLong(FeatureUsage::getCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the top features for a module by usage count.
     *
     * @param moduleId The module ID
     * @param limit The maximum number of features to return
     * @return A list of feature usages sorted by count
     */
    public List<FeatureUsage> getTopModuleFeatures(String moduleId, int limit) {
        return featureUsages.values().stream()
            .filter(usage -> usage.getModuleId().equals(moduleId))
            .sorted(Comparator.comparingLong(FeatureUsage::getCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Adds a report listener.
     *
     * @param listener The listener to add
     */
    public void addReportListener(Consumer<UsageReport> listener) {
        reportListeners.add(listener);
    }
    
    /**
     * Removes a report listener.
     *
     * @param listener The listener to remove
     */
    public void removeReportListener(Consumer<UsageReport> listener) {
        reportListeners.remove(listener);
    }
    
    /**
     * Generates a daily usage report.
     */
    private void generateDailyReport() {
        try {
            String yesterday = LocalDate.now().minusDays(1).toString();
            Map<String, Integer> yesterdayStats = dailyUsageStats.get(yesterday);
            
            if (yesterdayStats == null || yesterdayStats.isEmpty()) {
                LOGGER.info("No usage data for yesterday");
                return;
            }
            
            // Create report
            UsageReport report = new UsageReport(yesterday, yesterdayStats);
            
            // Notify listeners
            for (Consumer<UsageReport> listener : reportListeners) {
                try {
                    listener.accept(report);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error in usage report listener", e);
                }
            }
            
            LOGGER.info("Generated usage report for " + yesterday);
            
            // Clean up old daily stats (keep 30 days)
            LocalDate cutoff = LocalDate.now().minusDays(30);
            List<String> keysToRemove = new ArrayList<>();
            
            for (String date : dailyUsageStats.keySet()) {
                try {
                    LocalDate dateObj = LocalDate.parse(date);
                    if (dateObj.isBefore(cutoff)) {
                        keysToRemove.add(date);
                    }
                } catch (Exception e) {
                    // Ignore invalid dates
                }
            }
            
            for (String key : keysToRemove) {
                dailyUsageStats.remove(key);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating daily report", e);
        }
    }
    
    /**
     * Generates a usage report for a specified date range.
     *
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return The usage report
     */
    public UsageReport generateReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> combinedStats = new HashMap<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.toString();
            Map<String, Integer> dailyStats = dailyUsageStats.get(dateStr);
            
            if (dailyStats != null) {
                for (Map.Entry<String, Integer> entry : dailyStats.entrySet()) {
                    combinedStats.compute(entry.getKey(), (k, v) -> (v == null) ? entry.getValue() : v + entry.getValue());
                }
            }
        }
        
        return new UsageReport(startDate.toString() + " to " + endDate.toString(), combinedStats);
    }
    
    /**
     * Represents usage data for a feature.
     */
    public static class FeatureUsage {
        private final String moduleId;
        private final String featureId;
        private volatile long count;
        private volatile long firstUsed;
        private volatile long lastUsed;
        
        /**
         * Creates a new feature usage.
         *
         * @param moduleId The module ID
         * @param featureId The feature ID
         */
        public FeatureUsage(String moduleId, String featureId) {
            this.moduleId = moduleId;
            this.featureId = featureId;
            this.count = 0;
            this.firstUsed = System.currentTimeMillis();
            this.lastUsed = this.firstUsed;
        }
        
        /**
         * Gets the module ID.
         *
         * @return The module ID
         */
        public String getModuleId() {
            return moduleId;
        }
        
        /**
         * Gets the feature ID.
         *
         * @return The feature ID
         */
        public String getFeatureId() {
            return featureId;
        }
        
        /**
         * Gets the usage count.
         *
         * @return The usage count
         */
        public long getCount() {
            return count;
        }
        
        /**
         * Gets the first used timestamp.
         *
         * @return The first used timestamp
         */
        public long getFirstUsed() {
            return firstUsed;
        }
        
        /**
         * Gets the last used timestamp.
         *
         * @return The last used timestamp
         */
        public long getLastUsed() {
            return lastUsed;
        }
        
        /**
         * Increments the usage count.
         */
        public void incrementCount() {
            count++;
            lastUsed = System.currentTimeMillis();
        }
        
        /**
         * Gets the full feature key.
         *
         * @return The full feature key
         */
        public String getFullKey() {
            return moduleId + "." + featureId;
        }
    }
    
    /**
     * Represents a usage report.
     */
    public static class UsageReport {
        private final String period;
        private final Map<String, Integer> featureCounts;
        private final Map<String, Map<String, Integer>> moduleFeatureCounts;
        private final Map<String, Integer> moduleTotals;
        private final int totalCount;
        
        /**
         * Creates a new usage report.
         *
         * @param period The report period
         * @param featureCounts The feature counts
         */
        public UsageReport(String period, Map<String, Integer> featureCounts) {
            this.period = period;
            this.featureCounts = new LinkedHashMap<>(featureCounts);
            this.moduleFeatureCounts = new HashMap<>();
            this.moduleTotals = new HashMap<>();
            
            // Calculate module totals and organize features by module
            int total = 0;
            for (Map.Entry<String, Integer> entry : featureCounts.entrySet()) {
                String key = entry.getKey();
                int count = entry.getValue();
                total += count;
                
                String[] parts = key.split("\\.", 2);
                if (parts.length == 2) {
                    String moduleId = parts[0];
                    String featureId = parts[1];
                    
                    // Update module total
                    moduleTotals.compute(moduleId, (k, v) -> (v == null) ? count : v + count);
                    
                    // Add to module features
                    moduleFeatureCounts.computeIfAbsent(moduleId, k -> new HashMap<>())
                        .put(featureId, count);
                }
            }
            
            this.totalCount = total;
        }
        
        /**
         * Gets the report period.
         *
         * @return The report period
         */
        public String getPeriod() {
            return period;
        }
        
        /**
         * Gets the feature counts.
         *
         * @return The feature counts
         */
        public Map<String, Integer> getFeatureCounts() {
            return Collections.unmodifiableMap(featureCounts);
        }
        
        /**
         * Gets the module feature counts.
         *
         * @return The module feature counts
         */
        public Map<String, Map<String, Integer>> getModuleFeatureCounts() {
            return Collections.unmodifiableMap(moduleFeatureCounts);
        }
        
        /**
         * Gets the module totals.
         *
         * @return The module totals
         */
        public Map<String, Integer> getModuleTotals() {
            return Collections.unmodifiableMap(moduleTotals);
        }
        
        /**
         * Gets the total count.
         *
         * @return The total count
         */
        public int getTotalCount() {
            return totalCount;
        }
        
        /**
         * Gets the top features by usage count.
         *
         * @param limit The maximum number of features to return
         * @return A map of feature keys to counts
         */
        public Map<String, Integer> getTopFeatures(int limit) {
            return featureCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
        
        /**
         * Gets the top modules by usage count.
         *
         * @param limit The maximum number of modules to return
         * @return A map of module IDs to counts
         */
        public Map<String, Integer> getTopModules(int limit) {
            return moduleTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
        
        /**
         * Gets the top features for a module by usage count.
         *
         * @param moduleId The module ID
         * @param limit The maximum number of features to return
         * @return A map of feature IDs to counts
         */
        public Map<String, Integer> getTopModuleFeatures(String moduleId, int limit) {
            Map<String, Integer> features = moduleFeatureCounts.get(moduleId);
            if (features == null) {
                return Collections.emptyMap();
            }
            
            return features.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
    }
} 
