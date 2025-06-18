package com.essentialscore.api.integration.placeholders;

import com.essentialscore.api.integration.AbstractPluginIntegration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic placeholder integration with support for custom placeholders.
 */
public class PlaceholderIntegration extends AbstractPluginIntegration {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    
    private final Map<String, BiFunction<Player, String, String>> playerPlaceholders;
    private final Map<String, Function<String, String>> globalPlaceholders;
    
    /**
     * Creates a new placeholder integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public PlaceholderIntegration(Plugin plugin) {
        super(plugin);
        this.playerPlaceholders = new ConcurrentHashMap<>();
        this.globalPlaceholders = new ConcurrentHashMap<>();
    }
    
    @Override
    protected void onInitialize() {
        // Register default placeholders
        registerDefaultPlaceholders();
    }
    
    @Override
    protected void onShutdown() {
        playerPlaceholders.clear();
        globalPlaceholders.clear();
    }
    
    @Override
    public String getName() {
        return "Basic Placeholders";
    }
    
    /**
     * Registers a player-specific placeholder.
     *
     * @param placeholder The placeholder without % symbols
     * @param replacer The function to replace the placeholder
     */
    public void registerPlayerPlaceholder(String placeholder, BiFunction<Player, String, String> replacer) {
        playerPlaceholders.put(placeholder.toLowerCase(), replacer);
    }
    
    /**
     * Registers a global placeholder.
     *
     * @param placeholder The placeholder without % symbols
     * @param replacer The function to replace the placeholder
     */
    public void registerGlobalPlaceholder(String placeholder, Function<String, String> replacer) {
        globalPlaceholders.put(placeholder.toLowerCase(), replacer);
    }
    
    /**
     * Unregisters a player-specific placeholder.
     *
     * @param placeholder The placeholder without % symbols
     */
    public void unregisterPlayerPlaceholder(String placeholder) {
        playerPlaceholders.remove(placeholder.toLowerCase());
    }
    
    /**
     * Unregisters a global placeholder.
     *
     * @param placeholder The placeholder without % symbols
     */
    public void unregisterGlobalPlaceholder(String placeholder) {
        globalPlaceholders.remove(placeholder.toLowerCase());
    }
    
    /**
     * Sets placeholders in a string for a player.
     *
     * @param player The player
     * @param text The text with placeholders
     * @return The text with placeholders replaced
     */
    public String setPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String replacement = replacePlaceholder(player, placeholder);
            
            // Escape $ and \ characters in the replacement string
            if (replacement != null) {
                replacement = Matcher.quoteReplacement(replacement);
                matcher.appendReplacement(buffer, replacement);
            }
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    /**
     * Sets placeholders in a string without a player.
     *
     * @param text The text with placeholders
     * @return The text with placeholders replaced
     */
    public String setPlaceholders(String text) {
        return setPlaceholders(null, text);
    }
    
    /**
     * Replaces a placeholder for a player.
     *
     * @param player The player
     * @param placeholder The placeholder without % symbols
     * @return The replacement string, or null if no placeholder was found
     */
    private String replacePlaceholder(Player player, String placeholder) {
        // Try player placeholders first if player is not null
        if (player != null && playerPlaceholders.containsKey(placeholder)) {
            try {
                return playerPlaceholders.get(placeholder).apply(player, placeholder);
            } catch (Exception e) {
                logger.warning("Error replacing player placeholder: " + placeholder + " - " + e.getMessage());
            }
        }
        
        // Try global placeholders
        if (globalPlaceholders.containsKey(placeholder)) {
            try {
                return globalPlaceholders.get(placeholder).apply(placeholder);
            } catch (Exception e) {
                logger.warning("Error replacing global placeholder: " + placeholder + " - " + e.getMessage());
            }
        }
        
        // Return the original placeholder if not found
        return "%" + placeholder + "%";
    }
    
    /**
     * Registers default placeholders.
     */
    private void registerDefaultPlaceholders() {
        // Player placeholders
        registerPlayerPlaceholder("player_displayname", (player, placeholder) -> PlainTextComponentSerializer.plainText().serialize(player.displayName()));
        registerPlayerPlaceholder("player_uuid", (player, placeholder) -> player.getUniqueId().toString());
        registerPlayerPlaceholder("player_max_health", (player, placeholder) -> {
            var healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
            return healthAttribute != null ? String.valueOf(healthAttribute.getValue()) : "20.0";
        });
        
        // Global placeholders
        registerGlobalPlaceholder("server_name", placeholder -> plugin.getServer().getName());
        registerGlobalPlaceholder("server_version", placeholder -> plugin.getServer().getVersion());
        registerGlobalPlaceholder("server_online", placeholder -> String.valueOf(plugin.getServer().getOnlinePlayers().size()));
        registerGlobalPlaceholder("server_max_players", placeholder -> String.valueOf(plugin.getServer().getMaxPlayers()));
    }
    
    /**
     * Gets all registered player placeholders.
     *
     * @return Map of placeholders to replacers
     */
    public Map<String, BiFunction<Player, String, String>> getPlayerPlaceholders() {
        return new HashMap<>(playerPlaceholders);
    }
    
    /**
     * Gets all registered global placeholders.
     *
     * @return Map of placeholders to replacers
     */
    public Map<String, Function<String, String>> getGlobalPlaceholders() {
        return new HashMap<>(globalPlaceholders);
    }
} 
