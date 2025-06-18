package com.essentialscore.api.integration.economy;

import com.essentialscore.api.integration.AbstractPluginIntegration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Basic economy integration that provides in-memory economy functionality.
 */
public class EconomyIntegration extends AbstractPluginIntegration {
    private final Map<UUID, BigDecimal> playerBalances = new ConcurrentHashMap<>();
    private final String currencyNameSingular;
    private final String currencyNamePlural;
    private final int decimalPlaces;
    
    /**
     * Creates a new economy integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public EconomyIntegration(Plugin plugin) {
        this(plugin, "Dollar", "Dollars", 2);
    }
    
    /**
     * Creates a new economy integration with custom currency names.
     *
     * @param plugin The EssentialsCore plugin
     * @param currencyNameSingular The singular currency name
     * @param currencyNamePlural The plural currency name
     * @param decimalPlaces The number of decimal places to round to
     */
    public EconomyIntegration(Plugin plugin, String currencyNameSingular, String currencyNamePlural, int decimalPlaces) {
        super(plugin);
        this.currencyNameSingular = currencyNameSingular;
        this.currencyNamePlural = currencyNamePlural;
        this.decimalPlaces = decimalPlaces;
    }
    
    @Override
    protected void onInitialize() {
        // Nothing to initialize for basic economy
    }
    
    @Override
    protected void onShutdown() {
        playerBalances.clear();
    }
    
    @Override
    public String getName() {
        return "Basic Economy";
    }
    
    /**
     * Gets a player's balance.
     *
     * @param player The player
     * @return The balance
     */
    public double getBalance(Player player) {
        if (player == null) {
            return 0.0;
        }
        
        return getBalance(player.getUniqueId());
    }
    
    /**
     * Gets a player's balance.
     *
     * @param offlinePlayer The offline player
     * @return The balance
     */
    public double getBalance(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return 0.0;
        }
        
        return getBalance(offlinePlayer.getUniqueId());
    }
    
    /**
     * Gets a player's balance.
     *
     * @param playerUuid The player UUID
     * @return The balance
     */
    public double getBalance(UUID playerUuid) {
        if (playerUuid == null) {
            return 0.0;
        }
        
        BigDecimal balance = playerBalances.getOrDefault(playerUuid, BigDecimal.ZERO);
        return balance.doubleValue();
    }
    
    /**
     * Sets a player's balance.
     *
     * @param player The player
     * @param amount The amount
     * @return true if successful
     */
    public boolean setBalance(Player player, double amount) {
        if (player == null) {
            return false;
        }
        
        return setBalance(player.getUniqueId(), amount);
    }
    
    /**
     * Sets a player's balance.
     *
     * @param offlinePlayer The offline player
     * @param amount The amount
     * @return true if successful
     */
    public boolean setBalance(OfflinePlayer offlinePlayer, double amount) {
        if (offlinePlayer == null) {
            return false;
        }
        
        return setBalance(offlinePlayer.getUniqueId(), amount);
    }
    
    /**
     * Sets a player's balance.
     *
     * @param playerUuid The player UUID
     * @param amount The amount
     * @return true if successful
     */
    public boolean setBalance(UUID playerUuid, double amount) {
        if (playerUuid == null) {
            return false;
        }
        
        try {
            BigDecimal bdAmount = BigDecimal.valueOf(amount).setScale(decimalPlaces, RoundingMode.HALF_UP);
            playerBalances.put(playerUuid, bdAmount);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to set balance for player: " + playerUuid, e);
            return false;
        }
    }
    
    /**
     * Deposits money to a player.
     *
     * @param player The player
     * @param amount The amount
     * @return true if successful
     */
    public boolean deposit(Player player, double amount) {
        if (player == null || amount < 0) {
            return false;
        }
        
        return deposit(player.getUniqueId(), amount);
    }
    
    /**
     * Deposits money to a player.
     *
     * @param offlinePlayer The offline player
     * @param amount The amount
     * @return true if successful
     */
    public boolean deposit(OfflinePlayer offlinePlayer, double amount) {
        if (offlinePlayer == null || amount < 0) {
            return false;
        }
        
        return deposit(offlinePlayer.getUniqueId(), amount);
    }
    
    /**
     * Deposits money to a player.
     *
     * @param playerUuid The player UUID
     * @param amount The amount
     * @return true if successful
     */
    public boolean deposit(UUID playerUuid, double amount) {
        if (playerUuid == null || amount < 0) {
            return false;
        }
        
        try {
            BigDecimal current = playerBalances.getOrDefault(playerUuid, BigDecimal.ZERO);
            BigDecimal bdAmount = BigDecimal.valueOf(amount).setScale(decimalPlaces, RoundingMode.HALF_UP);
            BigDecimal newAmount = current.add(bdAmount).setScale(decimalPlaces, RoundingMode.HALF_UP);
            
            playerBalances.put(playerUuid, newAmount);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to deposit to player: " + playerUuid, e);
            return false;
        }
    }
    
    /**
     * Withdraws money from a player.
     *
     * @param player The player
     * @param amount The amount
     * @return true if successful
     */
    public boolean withdraw(Player player, double amount) {
        if (player == null || amount < 0) {
            return false;
        }
        
        return withdraw(player.getUniqueId(), amount);
    }
    
    /**
     * Withdraws money from a player.
     *
     * @param offlinePlayer The offline player
     * @param amount The amount
     * @return true if successful
     */
    public boolean withdraw(OfflinePlayer offlinePlayer, double amount) {
        if (offlinePlayer == null || amount < 0) {
            return false;
        }
        
        return withdraw(offlinePlayer.getUniqueId(), amount);
    }
    
    /**
     * Withdraws money from a player.
     *
     * @param playerUuid The player UUID
     * @param amount The amount
     * @return true if successful
     */
    public boolean withdraw(UUID playerUuid, double amount) {
        if (playerUuid == null || amount < 0) {
            return false;
        }
        
        try {
            BigDecimal current = playerBalances.getOrDefault(playerUuid, BigDecimal.ZERO);
            BigDecimal bdAmount = BigDecimal.valueOf(amount).setScale(decimalPlaces, RoundingMode.HALF_UP);
            
            // Check if player has enough money
            if (current.compareTo(bdAmount) < 0) {
                return false;
            }
            
            BigDecimal newAmount = current.subtract(bdAmount).setScale(decimalPlaces, RoundingMode.HALF_UP);
            playerBalances.put(playerUuid, newAmount);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to withdraw from player: " + playerUuid, e);
            return false;
        }
    }
    
    /**
     * Checks if a player has enough money.
     *
     * @param player The player
     * @param amount The amount
     * @return true if the player has enough money
     */
    public boolean has(Player player, double amount) {
        if (player == null) {
            return false;
        }
        
        return has(player.getUniqueId(), amount);
    }
    
    /**
     * Checks if a player has enough money.
     *
     * @param offlinePlayer The offline player
     * @param amount The amount
     * @return true if the player has enough money
     */
    public boolean has(OfflinePlayer offlinePlayer, double amount) {
        if (offlinePlayer == null) {
            return false;
        }
        
        return has(offlinePlayer.getUniqueId(), amount);
    }
    
    /**
     * Checks if a player has enough money.
     *
     * @param playerUuid The player UUID
     * @param amount The amount
     * @return true if the player has enough money
     */
    public boolean has(UUID playerUuid, double amount) {
        if (playerUuid == null) {
            return false;
        }
        
        try {
            BigDecimal current = playerBalances.getOrDefault(playerUuid, BigDecimal.ZERO);
            BigDecimal bdAmount = BigDecimal.valueOf(amount).setScale(decimalPlaces, RoundingMode.HALF_UP);
            
            return current.compareTo(bdAmount) >= 0;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check if player has enough money: " + playerUuid, e);
            return false;
        }
    }
    
    /**
     * Gets the currency name based on the amount.
     *
     * @param amount The amount
     * @return The currency name
     */
    public String getCurrencyName(double amount) {
        return Math.abs(amount) == 1.0 ? currencyNameSingular : currencyNamePlural;
    }
    
    /**
     * Formats an amount with the currency name.
     *
     * @param amount The amount
     * @return The formatted amount
     */
    public String format(double amount) {
        BigDecimal bdAmount = BigDecimal.valueOf(amount).setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bdAmount.toPlainString() + " " + getCurrencyName(amount);
    }
    
    /**
     * Gets all player balances.
     *
     * @return Map of player UUIDs to balances
     */
    public Map<UUID, Double> getAllBalances() {
        Map<UUID, Double> balances = new HashMap<>();
        
        for (Map.Entry<UUID, BigDecimal> entry : playerBalances.entrySet()) {
            balances.put(entry.getKey(), entry.getValue().doubleValue());
        }
        
        return balances;
    }
    
    /**
     * Gets the singular currency name.
     *
     * @return The singular currency name
     */
    public String getCurrencyNameSingular() {
        return currencyNameSingular;
    }
    
    /**
     * Gets the plural currency name.
     *
     * @return The plural currency name
     */
    public String getCurrencyNamePlural() {
        return currencyNamePlural;
    }
    
    /**
     * Gets the number of decimal places.
     *
     * @return The number of decimal places
     */
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
} 
