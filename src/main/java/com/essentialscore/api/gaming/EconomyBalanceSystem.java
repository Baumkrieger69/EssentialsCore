package com.essentialscore.api.gaming;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * System that automatically balances the in-game economy to prevent inflation or deflation.
 */
public class EconomyBalanceSystem implements Listener {
    private static final Logger LOGGER = Logger.getLogger(EconomyBalanceSystem.class.getName());
    
    // Default price adjustment parameters
    private static final double DEFAULT_ADJUSTMENT_FACTOR = 0.05;
    private static final double MIN_PRICE_MULTIPLIER = 0.5;
    private static final double MAX_PRICE_MULTIPLIER = 2.0;
    private static final long ANALYSIS_INTERVAL_TICKS = 20L * 60 * 15; // 15 minutes
    
    private final Plugin plugin;
    private final Map<String, ItemEconomyData> itemEconomyData;
    private final Map<String, ShopItem> registeredItems;
    private final List<EconomicIndicator> economicIndicators;
    private final File dataFile;
    
    private boolean running;
    private BukkitTask analysisTask;
    private double globalInflationRate;
    private net.milkbowl.vault.economy.Economy vaultEconomy;
    
    /**
     * Creates a new economy balance system.
     *
     * @param plugin The plugin
     */
    public EconomyBalanceSystem(Plugin plugin) {
        this.plugin = plugin;
        this.itemEconomyData = new ConcurrentHashMap<>();
        this.registeredItems = new ConcurrentHashMap<>();
        this.economicIndicators = new ArrayList<>();
        this.dataFile = new File(plugin.getDataFolder(), "economy_data.yml");
        this.running = false;
        this.globalInflationRate = 0.0;
    }
    
    /**
     * Starts the economy balance system.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting economy balance system");
        
        // Setup Vault integration
        if (!setupEconomy()) {
            LOGGER.warning("Vault economy not found! Economy balance system requires Vault.");
            return;
        }
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Load data
        loadData();
        
        // Register standard economic indicators
        registerDefaultIndicators();
        
        // Schedule regular analysis
        analysisTask = Bukkit.getScheduler().runTaskTimer(plugin, this::analyzeEconomy, 
                ANALYSIS_INTERVAL_TICKS, ANALYSIS_INTERVAL_TICKS);
        
        running = true;
    }
    
    /**
     * Stops the economy balance system.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping economy balance system");
        
        // Cancel scheduled tasks
        if (analysisTask != null) {
            analysisTask.cancel();
        }
        
        // Save data
        saveData();
        
        running = false;
    }
    
    /**
     * Sets up the Vault economy integration.
     *
     * @return true if successful
     */
    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = 
                plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        
        if (rsp == null) {
            return false;
        }
        
        vaultEconomy = rsp.getProvider();
        return vaultEconomy != null;
    }
    
    /**
     * Loads economy data from disk.
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            // Load global inflation rate
            globalInflationRate = config.getDouble("globalInflationRate", 0.0);
            
            // Load item data
            if (config.contains("items")) {
                for (String itemId : config.getConfigurationSection("items").getKeys(false)) {
                    String path = "items." + itemId;
                    
                    ItemEconomyData data = new ItemEconomyData(itemId);
                    data.basePrice = config.getDouble(path + ".basePrice", 1.0);
                    data.currentPrice = config.getDouble(path + ".currentPrice", data.basePrice);
                    data.priceMultiplier = config.getDouble(path + ".priceMultiplier", 1.0);
                    data.demandLevel = config.getDouble(path + ".demandLevel", 1.0);
                    data.transactionCount = config.getInt(path + ".transactionCount", 0);
                    data.lastTransactionTime = config.getLong(path + ".lastTransactionTime", 0);
                    data.totalValueTraded = config.getDouble(path + ".totalValueTraded", 0.0);
                    
                    itemEconomyData.put(itemId, data);
                }
            }
            
            LOGGER.info("Loaded economy data for " + itemEconomyData.size() + " items");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading economy data", e);
        }
    }
    
    /**
     * Saves economy data to disk.
     */
    private void saveData() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            // Save global inflation rate
            config.set("globalInflationRate", globalInflationRate);
            
            // Save item data
            for (Map.Entry<String, ItemEconomyData> entry : itemEconomyData.entrySet()) {
                String itemId = entry.getKey();
                ItemEconomyData data = entry.getValue();
                String path = "items." + itemId;
                
                config.set(path + ".basePrice", data.basePrice);
                config.set(path + ".currentPrice", data.currentPrice);
                config.set(path + ".priceMultiplier", data.priceMultiplier);
                config.set(path + ".demandLevel", data.demandLevel);
                config.set(path + ".transactionCount", data.transactionCount);
                config.set(path + ".lastTransactionTime", data.lastTransactionTime);
                config.set(path + ".totalValueTraded", data.totalValueTraded);
            }
            
            // Save to file
            config.save(dataFile);
            LOGGER.info("Saved economy data");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error saving economy data", e);
        }
    }
    
    /**
     * Registers the default economic indicators.
     */
    private void registerDefaultIndicators() {
        // Money supply indicator (total money in circulation)
        registerEconomicIndicator(new EconomicIndicator("money_supply") {
            @Override
            public double calculateValue() {
                double totalMoney = 0.0;
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    totalMoney += vaultEconomy.getBalance(player);
                }
                
                return totalMoney;
            }
        });
        
        // Transaction volume indicator
        registerEconomicIndicator(new EconomicIndicator("transaction_volume") {
            private double lastTotalTransactions = 0.0;
            
            @Override
            public double calculateValue() {
                double currentTotal = itemEconomyData.values().stream()
                        .mapToDouble(data -> data.totalValueTraded)
                        .sum();
                
                double volume = currentTotal - lastTotalTransactions;
                lastTotalTransactions = currentTotal;
                
                return volume;
            }
        });
        
        // Price index indicator (weighted average of prices)
        registerEconomicIndicator(new EconomicIndicator("price_index") {
            private double initialValue = -1.0;
            
            @Override
            public double calculateValue() {
                if (itemEconomyData.isEmpty()) {
                    return 1.0;
                }
                
                double weightedSum = 0.0;
                double totalWeight = 0.0;
                
                for (ItemEconomyData data : itemEconomyData.values()) {
                    double weight = Math.max(1.0, data.transactionCount);
                    weightedSum += data.currentPrice * weight;
                    totalWeight += weight;
                }
                
                double currentValue = totalWeight > 0 ? weightedSum / totalWeight : 1.0;
                
                // Initialize on first calculation
                if (initialValue < 0) {
                    initialValue = currentValue;
                    return 1.0;
                }
                
                // Return relative to initial value
                return currentValue / initialValue;
            }
        });
    }
    
    /**
     * Registers an economic indicator.
     *
     * @param indicator The indicator
     */
    public void registerEconomicIndicator(EconomicIndicator indicator) {
        economicIndicators.add(indicator);
        LOGGER.info("Registered economic indicator: " + indicator.getId());
    }
    
    /**
     * Analyzes the economy and makes adjustments.
     */
    private void analyzeEconomy() {
        if (!running) return;
        
        LOGGER.info("Analyzing economy...");
        
        // Calculate all indicators
        for (EconomicIndicator indicator : economicIndicators) {
            try {
                double value = indicator.calculateValue();
                indicator.addHistoricalValue(value);
                
                LOGGER.fine("Economic indicator " + indicator.getId() + ": " + value);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error calculating economic indicator: " + indicator.getId(), e);
            }
        }
        
        // Calculate inflation rate
        calculateInflationRate();
        
        // Adjust prices based on inflation and individual item demand
        adjustPrices();
        
        // Save data after analysis
        saveData();
    }
    
    /**
     * Calculates the global inflation rate.
     */
    private void calculateInflationRate() {
        // Find the price index indicator
        Optional<EconomicIndicator> priceIndexOpt = economicIndicators.stream()
                .filter(i -> i.getId().equals("price_index"))
                .findFirst();
        
        if (!priceIndexOpt.isPresent() || priceIndexOpt.get().getHistoricalValues().size() < 2) {
            globalInflationRate = 0.0;
            return;
        }
        
        EconomicIndicator priceIndex = priceIndexOpt.get();
        List<Double> values = priceIndex.getHistoricalValues();
        
        // Calculate rate of change
        double current = values.get(values.size() - 1);
        double previous = values.get(values.size() - 2);
        
        if (previous == 0.0) {
            globalInflationRate = 0.0;
        } else {
            globalInflationRate = (current / previous) - 1.0;
        }
        
        LOGGER.info("Current inflation rate: " + (globalInflationRate * 100) + "%");
    }
    
    /**
     * Adjusts prices based on inflation and individual item demand.
     */
    private void adjustPrices() {
        // Global inflation adjustment factor (negative to counteract inflation)
        double globalAdjustment = -globalInflationRate * DEFAULT_ADJUSTMENT_FACTOR;
        
        for (ItemEconomyData data : itemEconomyData.values()) {
            // Skip items with no transactions
            if (data.transactionCount == 0) {
                continue;
            }
            
            // Calculate individual item adjustment based on demand
            double demandAdjustment = (data.demandLevel - 1.0) * DEFAULT_ADJUSTMENT_FACTOR;
            
            // Combine adjustments
            double totalAdjustment = globalAdjustment + demandAdjustment;
            
            // Apply adjustment to price multiplier
            data.priceMultiplier = Math.max(MIN_PRICE_MULTIPLIER, 
                    Math.min(MAX_PRICE_MULTIPLIER, data.priceMultiplier * (1.0 + totalAdjustment)));
            
            // Calculate new price
            data.currentPrice = data.basePrice * data.priceMultiplier;
            
            // Reset demand to neutral for next period
            data.demandLevel = 1.0;
            
            LOGGER.fine("Adjusted price for " + data.itemId + ": " + data.currentPrice + 
                    " (multiplier: " + data.priceMultiplier + ")");
        }
    }
    
    /**
     * Records a transaction for an item.
     *
     * @param itemId The item ID
     * @param quantity The quantity
     * @param price The price per unit
     */
    public void recordTransaction(String itemId, int quantity, double price) {
        if (!running) return;
        
        ItemEconomyData data = itemEconomyData.computeIfAbsent(itemId, id -> {
            ItemEconomyData newData = new ItemEconomyData(id);
            newData.basePrice = price;
            newData.currentPrice = price;
            return newData;
        });
        
        // Update transaction data
        data.transactionCount += quantity;
        data.lastTransactionTime = System.currentTimeMillis();
        data.totalValueTraded += price * quantity;
        
        // Update demand level (increases with higher transaction volume)
        double demandFactor = 1.0 + (0.01 * quantity);
        data.demandLevel *= demandFactor;
        
        LOGGER.fine("Recorded transaction: " + itemId + " x" + quantity + " at " + price);
    }
    
    /**
     * Gets the current price for an item.
     *
     * @param itemId The item ID
     * @return The current price
     */
    public double getCurrentPrice(String itemId) {
        ItemEconomyData data = itemEconomyData.get(itemId);
        return data != null ? data.currentPrice : -1.0;
    }
    
    /**
     * Sets the base price for an item.
     *
     * @param itemId The item ID
     * @param basePrice The base price
     */
    public void setBasePrice(String itemId, double basePrice) {
        ItemEconomyData data = itemEconomyData.computeIfAbsent(itemId, ItemEconomyData::new);
        data.basePrice = basePrice;
        data.currentPrice = basePrice * data.priceMultiplier;
    }
    
    /**
     * Registers a shop item.
     *
     * @param item The shop item
     */
    public void registerShopItem(ShopItem item) {
        registeredItems.put(item.getId(), item);
        
        // Initialize economy data if not exists
        itemEconomyData.computeIfAbsent(item.getId(), id -> {
            ItemEconomyData newData = new ItemEconomyData(id);
            newData.basePrice = item.getBasePrice();
            newData.currentPrice = item.getBasePrice();
            return newData;
        });
        
        LOGGER.info("Registered shop item: " + item.getId() + " at base price " + item.getBasePrice());
    }
    
    /**
     * Gets a registered shop item.
     *
     * @param itemId The item ID
     * @return The shop item, or null if not found
     */
    public ShopItem getShopItem(String itemId) {
        return registeredItems.get(itemId);
    }
    
    /**
     * Gets all registered shop items.
     *
     * @return The shop items
     */
    public Collection<ShopItem> getAllShopItems() {
        return new ArrayList<>(registeredItems.values());
    }
    
    /**
     * Gets the current inflation rate.
     *
     * @return The inflation rate
     */
    public double getInflationRate() {
        return globalInflationRate;
    }
    
    /**
     * Gets all economic indicators.
     *
     * @return The economic indicators
     */
    public List<EconomicIndicator> getEconomicIndicators() {
        return new ArrayList<>(economicIndicators);
    }
    
    // Event handlers
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Update economy analysis when players join
        // (more accurate representation of money supply)
        if (running && Bukkit.getOnlinePlayers().size() == 1) {
            // Only trigger on first player join
            Bukkit.getScheduler().runTaskLater(plugin, this::analyzeEconomy, 20L * 10);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Detect and analyze economy transactions
        // In a real implementation, you would need to check if this is a shop inventory
        // and validate the transaction
        if (!running || event.isCancelled() || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        // Check if the clicked item is a shop item
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // In a real implementation, you would have a way to identify shop items
        // Store transaction data using persistent storage
        // Implementation using persistent data containers or item metadata
        String itemId = clickedItem.getType().name().toLowerCase();
        ShopItem shopItem = getShopItem(itemId);
        
        if (shopItem != null) {
            // Record the transaction
            recordTransaction(itemId, clickedItem.getAmount(), shopItem.getCurrentPrice());
        }
    }
    
    /**
     * Class representing item economy data.
     */
    private static class ItemEconomyData {
        private final String itemId;
        private double basePrice;
        private double currentPrice;
        private double priceMultiplier;
        private double demandLevel;
        private int transactionCount;
        private long lastTransactionTime;
        private double totalValueTraded;
        
        /**
         * Creates new item economy data.
         *
         * @param itemId The item ID
         */
        public ItemEconomyData(String itemId) {
            this.itemId = itemId;
            this.basePrice = 1.0;
            this.currentPrice = 1.0;
            this.priceMultiplier = 1.0;
            this.demandLevel = 1.0;
            this.transactionCount = 0;
            this.lastTransactionTime = 0;
            this.totalValueTraded = 0.0;
        }
    }
    
    /**
     * Class representing a shop item.
     */
    public static class ShopItem {
        private final String id;
        private final String displayName;
        private final Material material;
        private final double basePrice;
        private final Map<String, Object> metadata;
        
        /**
         * Creates a new shop item.
         *
         * @param id The item ID
         * @param displayName The display name
         * @param material The material
         * @param basePrice The base price
         */
        public ShopItem(String id, String displayName, Material material, double basePrice) {
            this.id = id;
            this.displayName = displayName;
            this.material = material;
            this.basePrice = basePrice;
            this.metadata = new HashMap<>();
        }
        
        /**
         * Gets the item ID.
         *
         * @return The item ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Gets the display name.
         *
         * @return The display name
         */
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * Gets the material.
         *
         * @return The material
         */
        public Material getMaterial() {
            return material;
        }
        
        /**
         * Gets the base price.
         *
         * @return The base price
         */
        public double getBasePrice() {
            return basePrice;
        }
        
        /**
         * Gets the current price from the economy system.
         *
         * @return The current price
         */
        public double getCurrentPrice() {
            // This method would need to access the EconomyBalanceSystem instance
            // For simplicity, we're returning the base price here
            return basePrice;
        }
        
        /**
         * Gets metadata.
         *
         * @param key The metadata key
         * @return The metadata value
         */
        public Object getMetadata(String key) {
            return metadata.get(key);
        }
        
        /**
         * Sets metadata.
         *
         * @param key The metadata key
         * @param value The metadata value
         */
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }
    
    /**
     * Class representing an economic indicator.
     */
    public abstract static class EconomicIndicator {
        private final String id;
        private final List<Double> historicalValues;
        
        /**
         * Creates a new economic indicator.
         *
         * @param id The indicator ID
         */
        public EconomicIndicator(String id) {
            this.id = id;
            this.historicalValues = new ArrayList<>();
        }
        
        /**
         * Gets the indicator ID.
         *
         * @return The indicator ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Calculates the current indicator value.
         *
         * @return The indicator value
         */
        public abstract double calculateValue();
        
        /**
         * Adds a historical value.
         *
         * @param value The value
         */
        public void addHistoricalValue(double value) {
            historicalValues.add(value);
            
            // Keep only the last 10 values
            if (historicalValues.size() > 10) {
                historicalValues.remove(0);
            }
        }
        
        /**
         * Gets the historical values.
         *
         * @return The historical values
         */
        public List<Double> getHistoricalValues() {
            return new ArrayList<>(historicalValues);
        }
    }
} 
