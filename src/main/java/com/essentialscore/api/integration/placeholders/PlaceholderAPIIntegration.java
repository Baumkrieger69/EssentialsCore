package com.essentialscore.api.integration.placeholders;

import com.essentialscore.api.integration.AbstractPluginDependentIntegration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integration with PlaceholderAPI for enhanced placeholder support.
 */
public class PlaceholderAPIIntegration extends AbstractPluginDependentIntegration {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    
    private Object placeholderAPIPlugin;
    private Object expansionManager;
    private Method setPlaceholdersMethod;
    private Method registerExpansionMethod;
    private Method unregisterExpansionMethod;
    
    /**
     * Creates a new PlaceholderAPI integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public PlaceholderAPIIntegration(Plugin plugin) {
        super(plugin, "PlaceholderAPI");
    }
    
    @Override
    protected void onPluginInitialize() {
        try {
            placeholderAPIPlugin = dependencyPlugin;
            
            // Get PlaceholderAPI methods via reflection to avoid hard dependency
            Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            setPlaceholdersMethod = placeholderAPIClass.getMethod("setPlaceholders", Player.class, String.class);
            
            Class<?> placeholderExpansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            Class<?> expansionManagerClass = Class.forName("me.clip.placeholderapi.expansion.manager.LocalExpansionManager");
            // Get expansion manager instance
            Method getExpansionManagerMethod = placeholderAPIPlugin.getClass().getMethod("getLocalExpansionManager");
            this.expansionManager = getExpansionManagerMethod.invoke(placeholderAPIPlugin);
            
            registerExpansionMethod = expansionManagerClass.getMethod("register", placeholderExpansionClass);
            unregisterExpansionMethod = expansionManagerClass.getMethod("unregister", placeholderExpansionClass);
            unregisterExpansionMethod = expansionManagerClass.getMethod("unregister", placeholderExpansionClass);
            
            logger.info("PlaceholderAPI integration initialized");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize PlaceholderAPI integration", e);
            throw new RuntimeException("Failed to initialize PlaceholderAPI integration", e);
        }
    }
    @Override
    protected void onPluginShutdown() {
        placeholderAPIPlugin = null;
        expansionManager = null;
        setPlaceholdersMethod = null;
        registerExpansionMethod = null;
        unregisterExpansionMethod = null;
    }
    
    @Override
    public String getName() {
        return "PlaceholderAPI";
    }
    
    @Override
    public Plugin getPlugin() {
        return super.getPlugin();
    }
    
    /**
     * Sets PlaceholderAPI placeholders in a string.
     *
     * @param player The player
     * @param text The text with placeholders
     * @return The text with placeholders replaced
     */
    public String setPlaceholders(Player player, String text) {
        if (!isAvailable() || text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            return (String) setPlaceholdersMethod.invoke(null, player, text);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to set PlaceholderAPI placeholders", e);
            return text;
        }
    }
    
    /**
     * Registers a custom placeholder expansion.
     *
     * @param expansion The expansion object (must be a PlaceholderExpansion instance)
     * @return true if registration was successful
     */
    public boolean registerExpansion(Object expansion) {
        if (!isAvailable() || expansion == null) {
            return false;
        }
        
        try {
            Object result = registerExpansionMethod.invoke(expansionManager, expansion);
            return result instanceof Boolean ? (Boolean) result : false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to register PlaceholderAPI expansion", e);
            return false;
        }
    }
    
    /**
     * Unregisters a custom placeholder expansion.
     *
     * @param expansion The expansion object (must be a PlaceholderExpansion instance)
     * @return true if unregistration was successful
     */
    public boolean unregisterExpansion(Object expansion) {
        if (!isAvailable() || expansion == null) {
            return false;
        }
        
        try {
            Object result = unregisterExpansionMethod.invoke(expansionManager, expansion);
            return result instanceof Boolean ? (Boolean) result : false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to unregister PlaceholderAPI expansion", e);
            return false;
        }
    }
    
    /**
     * Creates a simple placeholder expansion.
     *
     * @param identifier The expansion identifier
     * @param author The expansion author
     * @param version The expansion version
     * @param placeholders Map of placeholder names to functions that return values
     * @return The expansion object, or null if creation failed
     */
    public Object createSimpleExpansion(String identifier, String author, String version, 
                                       Map<String, BiFunction<Player, String, String>> placeholders) {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            // Create a custom class extending PlaceholderExpansion
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            
            // Dynamically create a subclass using internal class loading
            // For simplicity, we'll use reflection to create an instance of a custom class
            
            // In a real implementation, you would create a proper class extending PlaceholderExpansion
            // This is a simplified approach to avoid direct dependencies
            
            Class<?> expansionClass = createPlaceholderExpansionClass(classLoader, 
                                                                    identifier, author, version, 
                                                                    new HashMap<>(placeholders));
            
            if (expansionClass != null) {
                return expansionClass.getDeclaredConstructor().newInstance();
            }
            
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create PlaceholderAPI expansion", e);
            return null;
        }
    }
    
    /**
     * Creates a class that extends PlaceholderExpansion.
     * This is a placeholder implementation; in a real scenario, you would use a proper subclass.
     */
    private Class<?> createPlaceholderExpansionClass(ClassLoader classLoader, 
                                                  String identifier, 
                                                  String author, 
                                                  String version, 
                                                  Map<String, BiFunction<Player, String, String>> placeholders) {
        // In a real implementation, this would create or load a class that extends PlaceholderExpansion
        // This method is not implemented - PlaceholderAPI integration required
        throw new UnsupportedOperationException(
            "Direct creation of PlaceholderExpansion classes requires bytecode manipulation. " + 
            "For real usage, create a proper class that extends PlaceholderExpansion."
        );
    }
    
    /**
     * Extracts placeholders from a text.
     *
     * @param text The text with placeholders
     * @return Array of placeholders without the % symbols
     */
    public String[] extractPlaceholders(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        Map<String, String> placeholders = new HashMap<>();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            placeholders.put(placeholder, placeholder);
        }
        
        return placeholders.keySet().toArray(new String[0]);
    }
} 
