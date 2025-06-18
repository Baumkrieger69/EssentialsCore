package com.essentialscore.api.integration.economy;

import com.essentialscore.api.integration.AbstractPluginDependentIntegration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

/**
 * Integration with Vault economy.
 */
public class VaultEconomyIntegration extends AbstractPluginDependentIntegration {
    private Economy vaultEconomy;
    
    /**
     * Creates a new Vault economy integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public VaultEconomyIntegration(Plugin plugin) {
        super(plugin, "Vault");
    }
    
    @Override
    protected void onPluginInitialize() {
        // Get Vault economy
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        
        if (rsp == null) {
            throw new IllegalStateException("No Vault economy provider found");
        }
        
        vaultEconomy = rsp.getProvider();
        
        if (vaultEconomy == null) {
            throw new IllegalStateException("Vault economy provider is null");
        }
        
        logger.info("Using Vault economy provider: " + vaultEconomy.getName());
    }
    
    @Override
    protected void onPluginShutdown() {
        vaultEconomy = null;
    }
    
    @Override
    public String getName() {
        return "Vault Economy";
    }
    
    @Override
    public Plugin getPlugin() {
        return super.getPlugin();
    }
    
    /**
     * Gets the Vault economy provider.
     *
     * @return The Vault economy provider
     */
    public Economy getVaultEconomy() {
        return vaultEconomy;
    }
    
    /**
     * Gets a player's balance.
     *
     * @param player The player
     * @return The balance
     */
    public double getBalance(Player player) {
        if (!isAvailable() || player == null) {
            return 0.0;
        }
        
        try {
            return vaultEconomy.getBalance(player);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get balance for player: " + player.getName(), e);
            return 0.0;
        }
    }
    
    /**
     * Gets a player's balance.
     *
     * @param offlinePlayer The offline player
     * @return The balance
     */
    public double getBalance(OfflinePlayer offlinePlayer) {
        if (!isAvailable() || offlinePlayer == null) {
            return 0.0;
        }
        
        try {
            return vaultEconomy.getBalance(offlinePlayer);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get balance for offline player: " + offlinePlayer.getName(), e);
            return 0.0;
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
        if (!isAvailable() || player == null || amount < 0) {
            return false;
        }
        
        try {
            return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to deposit to player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Deposits money to a player.
     *
     * @param offlinePlayer The offline player
     * @param amount The amount
     * @return true if successful
     */
    public boolean deposit(OfflinePlayer offlinePlayer, double amount) {
        if (!isAvailable() || offlinePlayer == null || amount < 0) {
            return false;
        }
        
        try {
            return vaultEconomy.depositPlayer(offlinePlayer, amount).transactionSuccess();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to deposit to offline player: " + offlinePlayer.getName(), e);
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
        if (!isAvailable() || player == null || amount < 0) {
            return false;
        }
        
        try {
            return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to withdraw from player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Withdraws money from a player.
     *
     * @param offlinePlayer The offline player
     * @param amount The amount
     * @return true if successful
     */
    public boolean withdraw(OfflinePlayer offlinePlayer, double amount) {
        if (!isAvailable() || offlinePlayer == null || amount < 0) {
            return false;
        }
        
        try {
            return vaultEconomy.withdrawPlayer(offlinePlayer, amount).transactionSuccess();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to withdraw from offline player: " + offlinePlayer.getName(), e);
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
        if (!isAvailable() || player == null) {
            return false;
        }
        
        try {
            return vaultEconomy.has(player, amount);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check if player has enough money: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Checks if a player has enough money.
     *
     * @param offlinePlayer The offline player
     * @param amount The amount
     * @return true if the player has enough money
     */
    public boolean has(OfflinePlayer offlinePlayer, double amount) {
        if (!isAvailable() || offlinePlayer == null) {
            return false;
        }
        
        try {
            return vaultEconomy.has(offlinePlayer, amount);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check if offline player has enough money: " + offlinePlayer.getName(), e);
            return false;
        }
    }
    
    /**
     * Formats an amount with the currency name.
     *
     * @param amount The amount
     * @return The formatted amount
     */
    public String format(double amount) {
        if (!isAvailable()) {
            return String.valueOf(amount);
        }
        
        try {
            return vaultEconomy.format(amount);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to format amount: " + amount, e);
            return String.valueOf(amount);
        }
    }
    
    /**
     * Gets the singular currency name.
     *
     * @return The singular currency name
     */
    public String getCurrencyNameSingular() {
        if (!isAvailable()) {
            return "";
        }
        
        try {
            return vaultEconomy.currencyNameSingular();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get singular currency name", e);
            return "";
        }
    }
    
    /**
     * Gets the plural currency name.
     *
     * @return The plural currency name
     */
    public String getCurrencyNamePlural() {
        if (!isAvailable()) {
            return "";
        }
        
        try {
            return vaultEconomy.currencyNamePlural();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get plural currency name", e);
            return "";
        }
    }
    
    /**
     * Checks if the economy provider is enabled.
     *
     * @return true if the economy provider is enabled
     */
    public boolean isEnabled() {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            return vaultEconomy.isEnabled();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check if economy is enabled", e);
            return false;
        }
    }
    
    /**
     * Gets the economy provider name.
     *
     * @return The economy provider name
     */
    public String getProviderName() {
        if (!isAvailable()) {
            return "Unknown";
        }
        
        try {
            return vaultEconomy.getName();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get economy provider name", e);
            return "Unknown";
        }
    }
} 
