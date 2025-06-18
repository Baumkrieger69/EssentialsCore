package com.essentialscore.api.performance;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Framework for running A/B tests on features and module updates.
 */
public class ABTestingFramework {
    private static final Logger LOGGER = Logger.getLogger(ABTestingFramework.class.getName());
    private static final Random RANDOM = new Random();
    
    private final ProfilingSystem profilingSystem;
    private final Map<String, ABTest> activeTests;
    private final Map<UUID, Map<String, String>> playerVariants;
    private final List<Consumer<ABTestResult>> resultListeners;
    
    /**
     * Creates a new A/B testing framework.
     *
     * @param profilingSystem The profiling system
     */
    public ABTestingFramework(ProfilingSystem profilingSystem) {
        this.profilingSystem = profilingSystem;
        this.activeTests = new ConcurrentHashMap<>();
        this.playerVariants = new ConcurrentHashMap<>();
        this.resultListeners = new ArrayList<>();
    }
    
    /**
     * Creates a new A/B test.
     *
     * @param testId The test ID
     * @param moduleId The module ID
     * @param description The test description
     * @param variants The test variants
     * @return The created test
     */
    public ABTest createTest(String testId, String moduleId, String description, Map<String, Integer> variants) {
        ABTest test = new ABTest(testId, moduleId, description, variants);
        activeTests.put(testId, test);
        LOGGER.info("Created A/B test: " + testId + " for module " + moduleId);
        return test;
    }
    
    /**
     * Gets an active test by ID.
     *
     * @param testId The test ID
     * @return The test, or null if not found
     */
    public ABTest getTest(String testId) {
        return activeTests.get(testId);
    }
    
    /**
     * Gets all active tests.
     *
     * @return A list of active tests
     */
    public List<ABTest> getActiveTests() {
        return new ArrayList<>(activeTests.values());
    }
    
    /**
     * Gets active tests for a module.
     *
     * @param moduleId The module ID
     * @return A list of active tests for the module
     */
    public List<ABTest> getModuleTests(String moduleId) {
        List<ABTest> tests = new ArrayList<>();
        for (ABTest test : activeTests.values()) {
            if (test.getModuleId().equals(moduleId)) {
                tests.add(test);
            }
        }
        return tests;
    }
    
    /**
     * Ends a test and analyzes the results.
     *
     * @param testId The test ID
     * @return The test results, or null if the test was not found
     */
    public ABTestResult endTest(String testId) {
        ABTest test = activeTests.remove(testId);
        if (test == null) {
            LOGGER.warning("Attempted to end non-existent test: " + testId);
            return null;
        }
        
        // Analyze test results
        ABTestResult result = analyzeTestResults(test);
        
        // Notify listeners
        for (Consumer<ABTestResult> listener : resultListeners) {
            try {
                listener.accept(result);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in A/B test result listener", e);
            }
        }
        
        // Remove test assignments for players
        for (Map<String, String> variants : playerVariants.values()) {
            variants.remove(testId);
        }
        
        LOGGER.info("Ended A/B test: " + testId + " with winner: " + result.getWinningVariant());
        return result;
    }
    
    /**
     * Analyzes test results.
     *
     * @param test The test to analyze
     * @return The test results
     */
    private ABTestResult analyzeTestResults(ABTest test) {
        Map<String, VariantMetrics> variantMetrics = new HashMap<>();
        
        // Initialize variant metrics
        for (String variant : test.getVariants().keySet()) {
            variantMetrics.put(variant, new VariantMetrics());
        }
        
        // Collect metrics for each variant
        String conversionMetric = test.getModuleId() + ".test." + test.getTestId() + ".conversion";
        String impressionMetric = test.getModuleId() + ".test." + test.getTestId() + ".impression";
        
        ProfilingSystem.Metric conversionMetricObj = profilingSystem.getMetric(conversionMetric);
        ProfilingSystem.Metric impressionMetricObj = profilingSystem.getMetric(impressionMetric);
        
        if (conversionMetricObj != null && impressionMetricObj != null) {
            // Get variant-specific metrics
            for (String variant : test.getVariants().keySet()) {
                String variantConversionMetric = conversionMetric + "." + variant;
                String variantImpressionMetric = impressionMetric + "." + variant;
                
                ProfilingSystem.Metric variantConversionMetricObj = profilingSystem.getMetric(variantConversionMetric);
                ProfilingSystem.Metric variantImpressionMetricObj = profilingSystem.getMetric(variantImpressionMetric);
                
                if (variantConversionMetricObj != null && variantImpressionMetricObj != null) {
                    VariantMetrics metrics = variantMetrics.get(variant);
                    metrics.conversions = (int) variantConversionMetricObj.getValue();
                    metrics.impressions = (int) variantImpressionMetricObj.getValue();
                    
                    if (metrics.impressions > 0) {
                        metrics.conversionRate = (double) metrics.conversions / metrics.impressions;
                    }
                }
            }
        }
        
        // Determine the winning variant
        String winningVariant = null;
        double highestConversionRate = -1;
        
        for (Map.Entry<String, VariantMetrics> entry : variantMetrics.entrySet()) {
            if (entry.getValue().conversionRate > highestConversionRate) {
                highestConversionRate = entry.getValue().conversionRate;
                winningVariant = entry.getKey();
            }
        }
        
        // If no clear winner, use the control variant
        if (winningVariant == null) {
            winningVariant = "control";
        }
        
        return new ABTestResult(test, variantMetrics, winningVariant);
    }
    
    /**
     * Assigns a variant to a player.
     *
     * @param player The player
     * @param testId The test ID
     * @return The assigned variant
     */
    public String assignVariant(Player player, String testId) {
        UUID playerId = player.getUniqueId();
        ABTest test = activeTests.get(testId);
        
        if (test == null) {
            LOGGER.warning("Attempted to assign variant for non-existent test: " + testId);
            return "control";
        }
        
        // Check if player already has an assignment
        Map<String, String> playerTestVariants = playerVariants.computeIfAbsent(
            playerId,
            k -> new ConcurrentHashMap<>()
        );
        
        String variant = playerTestVariants.get(testId);
        if (variant != null) {
            return variant;
        }
        
        // Assign a variant based on weights
        variant = selectWeightedVariant(test.getVariants());
        playerTestVariants.put(testId, variant);
        
        // Record impression
        String impressionMetric = test.getModuleId() + ".test." + test.getTestId() + ".impression";
        profilingSystem.incrementCounter(impressionMetric);
        profilingSystem.incrementCounter(impressionMetric + "." + variant);
        
        return variant;
    }
    
    /**
     * Gets the variant assigned to a player.
     *
     * @param player The player
     * @param testId The test ID
     * @return The assigned variant, or null if not assigned
     */
    public String getPlayerVariant(Player player, String testId) {
        UUID playerId = player.getUniqueId();
        Map<String, String> playerTestVariants = playerVariants.get(playerId);
        
        if (playerTestVariants == null) {
            return null;
        }
        
        return playerTestVariants.get(testId);
    }
    
    /**
     * Records a conversion for a player.
     *
     * @param player The player
     * @param testId The test ID
     */
    public void recordConversion(Player player, String testId) {
        String variant = getPlayerVariant(player, testId);
        if (variant == null) {
            return;
        }
        
        ABTest test = activeTests.get(testId);
        if (test == null) {
            return;
        }
        
        // Record conversion
        String conversionMetric = test.getModuleId() + ".test." + test.getTestId() + ".conversion";
        profilingSystem.incrementCounter(conversionMetric);
        profilingSystem.incrementCounter(conversionMetric + "." + variant);
    }
    
    /**
     * Selects a variant based on weights.
     *
     * @param variants The variants and their weights
     * @return The selected variant
     */
    private String selectWeightedVariant(Map<String, Integer> variants) {
        int totalWeight = 0;
        for (int weight : variants.values()) {
            totalWeight += weight;
        }
        
        int randomValue = RANDOM.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (Map.Entry<String, Integer> entry : variants.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                return entry.getKey();
            }
        }
        
        // Default to the first variant if something goes wrong
        return variants.keySet().iterator().next();
    }
    
    /**
     * Adds a result listener.
     *
     * @param listener The listener to add
     */
    public void addResultListener(Consumer<ABTestResult> listener) {
        resultListeners.add(listener);
    }
    
    /**
     * Removes a result listener.
     *
     * @param listener The listener to remove
     */
    public void removeResultListener(Consumer<ABTestResult> listener) {
        resultListeners.remove(listener);
    }
    
    /**
     * Represents an A/B test.
     */
    public static class ABTest {
        private final String testId;
        private final String moduleId;
        private final String description;
        private final Map<String, Integer> variants;
        private final long startTime;
        private final Map<String, Set<UUID>> variantPlayers;
        
        /**
         * Creates a new A/B test.
         *
         * @param testId The test ID
         * @param moduleId The module ID
         * @param description The test description
         * @param variants The test variants and their weights
         */
        public ABTest(String testId, String moduleId, String description, Map<String, Integer> variants) {
            this.testId = testId;
            this.moduleId = moduleId;
            this.description = description;
            this.variants = new HashMap<>(variants);
            this.startTime = System.currentTimeMillis();
            this.variantPlayers = new HashMap<>();
            
            // Initialize variant player sets
            for (String variant : variants.keySet()) {
                variantPlayers.put(variant, new HashSet<>());
            }
        }
        
        /**
         * Gets the test ID.
         *
         * @return The test ID
         */
        public String getTestId() {
            return testId;
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
         * Gets the test description.
         *
         * @return The test description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the test variants and their weights.
         *
         * @return The variants
         */
        public Map<String, Integer> getVariants() {
            return Collections.unmodifiableMap(variants);
        }
        
        /**
         * Gets the test start time.
         *
         * @return The start time
         */
        public long getStartTime() {
            return startTime;
        }
        
        /**
         * Gets the duration of the test.
         *
         * @return The test duration in milliseconds
         */
        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
        
        /**
         * Assigns a player to a variant.
         *
         * @param playerId The player ID
         * @param variant The variant
         */
        void assignPlayer(UUID playerId, String variant) {
            Set<UUID> players = variantPlayers.get(variant);
            if (players != null) {
                players.add(playerId);
            }
        }
        
        /**
         * Gets the players assigned to a variant.
         *
         * @param variant The variant
         * @return The assigned players
         */
        public Set<UUID> getVariantPlayers(String variant) {
            Set<UUID> players = variantPlayers.get(variant);
            return players != null ? Collections.unmodifiableSet(players) : Collections.emptySet();
        }
    }
    
    /**
     * Represents metrics for a test variant.
     */
    public static class VariantMetrics {
        private int impressions;
        private int conversions;
        private double conversionRate;
        
        /**
         * Creates new variant metrics.
         */
        public VariantMetrics() {
            this.impressions = 0;
            this.conversions = 0;
            this.conversionRate = 0.0;
        }
        
        /**
         * Gets the number of impressions.
         *
         * @return The impressions
         */
        public int getImpressions() {
            return impressions;
        }
        
        /**
         * Gets the number of conversions.
         *
         * @return The conversions
         */
        public int getConversions() {
            return conversions;
        }
        
        /**
         * Gets the conversion rate.
         *
         * @return The conversion rate
         */
        public double getConversionRate() {
            return conversionRate;
        }
    }
    
    /**
     * Represents the results of an A/B test.
     */
    public static class ABTestResult {
        private final ABTest test;
        private final Map<String, VariantMetrics> variantMetrics;
        private final String winningVariant;
        
        /**
         * Creates new A/B test results.
         *
         * @param test The test
         * @param variantMetrics The variant metrics
         * @param winningVariant The winning variant
         */
        public ABTestResult(ABTest test, Map<String, VariantMetrics> variantMetrics, String winningVariant) {
            this.test = test;
            this.variantMetrics = new HashMap<>(variantMetrics);
            this.winningVariant = winningVariant;
        }
        
        /**
         * Gets the test.
         *
         * @return The test
         */
        public ABTest getTest() {
            return test;
        }
        
        /**
         * Gets the variant metrics.
         *
         * @return The variant metrics
         */
        public Map<String, VariantMetrics> getVariantMetrics() {
            return Collections.unmodifiableMap(variantMetrics);
        }
        
        /**
         * Gets the winning variant.
         *
         * @return The winning variant
         */
        public String getWinningVariant() {
            return winningVariant;
        }
        
        /**
         * Gets the metrics for a variant.
         *
         * @param variant The variant
         * @return The metrics, or null if not found
         */
        public VariantMetrics getMetricsForVariant(String variant) {
            return variantMetrics.get(variant);
        }
        
        /**
         * Gets the improvement percentage of the winning variant over the control.
         *
         * @return The improvement percentage, or 0 if there's no improvement
         */
        public double getImprovementPercentage() {
            VariantMetrics controlMetrics = variantMetrics.get("control");
            VariantMetrics winningMetrics = variantMetrics.get(winningVariant);
            
            if (controlMetrics == null || winningMetrics == null || "control".equals(winningVariant)) {
                return 0.0;
            }
            
            if (controlMetrics.getConversionRate() == 0) {
                return 0.0;
            }
            
            return ((winningMetrics.getConversionRate() - controlMetrics.getConversionRate()) / 
                    controlMetrics.getConversionRate()) * 100.0;
        }
        
        /**
         * Determines if the result is statistically significant.
         *
         * @return true if the result is statistically significant
         */
        public boolean isStatisticallySignificant() {
            // This is a simple implementation that could be improved with proper statistical tests
            VariantMetrics controlMetrics = variantMetrics.get("control");
            VariantMetrics winningMetrics = variantMetrics.get(winningVariant);
            
            if (controlMetrics == null || winningMetrics == null || "control".equals(winningVariant)) {
                return false;
            }
            
            // Require a minimum number of impressions and conversions
            return controlMetrics.getImpressions() >= 100 && 
                   winningMetrics.getImpressions() >= 100 &&
                   controlMetrics.getConversions() >= 10 &&
                   winningMetrics.getConversions() >= 10 &&
                   getImprovementPercentage() >= 10.0; // At least 10% improvement
        }
    }
} 
