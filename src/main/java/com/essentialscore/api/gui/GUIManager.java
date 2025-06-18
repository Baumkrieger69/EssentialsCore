package com.essentialscore.api.gui;

import com.essentialscore.api.security.SecurityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for handling GUI creation, interaction, and management.
 * This class provides a unified way for modules to create interactive inventory GUIs.
 */
public class GUIManager {
    private static final Logger LOGGER = Logger.getLogger(GUIManager.class.getName());
    
    private final Plugin plugin;
    private final SecurityManager securityManager;
    private final Map<String, GUITheme> themes;
    private final Map<UUID, GUI> activeGUIs;
    private final Map<String, GUIFactory> registeredFactories;
    private final Map<String, GUITemplate> templates;
    private GUITheme defaultTheme;
    
    /**
     * Creates a new GUI manager.
     *
     * @param plugin The EssentialsCore plugin
     * @param securityManager The security manager
     */
    public GUIManager(Plugin plugin, SecurityManager securityManager) {
        this.plugin = plugin;
        this.securityManager = securityManager;
        this.themes = new ConcurrentHashMap<>();
        this.activeGUIs = new ConcurrentHashMap<>();
        this.registeredFactories = new ConcurrentHashMap<>();
        this.templates = new ConcurrentHashMap<>();
        
        // Create and register the default theme
        this.defaultTheme = new DefaultGUITheme();
        themes.put("default", this.defaultTheme);
        
        // Register listeners for inventory events
        registerEventListeners();
    }
    
    /**
     * Initializes the GUI manager.
     */
    public void initialize() {
        LOGGER.info("Initializing GUI manager...");
        
        // Register built-in factories
        registerFactory("standard", new StandardGUIFactory(this));
        registerFactory("paginated", new PaginatedGUIFactory(this));
        registerFactory("form", new FormGUIFactory(this));
        
        // Register built-in templates
        registerBuiltInTemplates();
        
        LOGGER.info("GUI manager initialized");
    }
    
    /**
     * Shuts down the GUI manager.
     */
    public void shutdown() {
        LOGGER.info("Shutting down GUI manager...");
        
        // Close all active GUIs
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (activeGUIs.containsKey(player.getUniqueId())) {
                player.closeInventory();
            }
        }
        
        // Clear collections
        activeGUIs.clear();
        registeredFactories.clear();
        templates.clear();
        themes.clear();
        
        LOGGER.info("GUI manager shut down");
    }
    
    /**
     * Registers a GUI factory.
     *
     * @param id The factory ID
     * @param factory The factory
     */
    public void registerFactory(String id, GUIFactory factory) {
        registeredFactories.put(id.toLowerCase(), factory);
    }
    
    /**
     * Gets a registered GUI factory.
     *
     * @param id The factory ID
     * @return The factory, or null if not found
     */
    public GUIFactory getFactory(String id) {
        return registeredFactories.get(id.toLowerCase());
    }
    
    /**
     * Registers a GUI template.
     *
     * @param id The template ID
     * @param template The template
     */
    public void registerTemplate(String id, GUITemplate template) {
        templates.put(id.toLowerCase(), template);
    }
    
    /**
     * Gets a registered GUI template.
     *
     * @param id The template ID
     * @return The template, or null if not found
     */
    public GUITemplate getTemplate(String id) {
        return templates.get(id.toLowerCase());
    }
    
    /**
     * Registers a GUI theme.
     *
     * @param id The theme ID
     * @param theme The theme
     */
    public void registerTheme(String id, GUITheme theme) {
        themes.put(id.toLowerCase(), theme);
    }
    
    /**
     * Gets a registered GUI theme.
     *
     * @param id The theme ID
     * @return The theme, or the default theme if not found
     */
    public GUITheme getTheme(String id) {
        return themes.getOrDefault(id.toLowerCase(), defaultTheme);
    }
    
    /**
     * Sets the default GUI theme.
     *
     * @param theme The theme to set as default
     */
    public void setDefaultTheme(GUITheme theme) {
        this.defaultTheme = theme;
    }
    
    /**
     * Gets the default GUI theme.
     *
     * @return The default theme
     */
    public GUITheme getDefaultTheme() {
        return defaultTheme;
    }
    
    /**
     * Creates a new GUI builder.
     *
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows (1-6)
     * @return The GUI builder
     */
    public GUIBuilder createGUI(String moduleId, String title, int rows) {
        // Validate rows
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6");
        }
        
        // Check if the module has permission to create GUIs
        if (!securityManager.isOperationAllowed(moduleId, "gui.create", title)) {
            throw new SecurityException("Module " + moduleId + " is not allowed to create GUIs");
        }
        
        return new GUIBuilder(this, moduleId, title, rows);
    }
    
    /**
     * Opens a GUI for a player.
     *
     * @param player The player
     * @param gui The GUI to open
     */
    public void openGUI(Player player, GUI gui) {
        if (player == null || gui == null) {
            return;
        }
        
        // Close any existing GUI
        if (activeGUIs.containsKey(player.getUniqueId())) {
            player.closeInventory();
        }
        
        // Open the GUI
        player.openInventory(gui.getInventory());
        activeGUIs.put(player.getUniqueId(), gui);
        
        // Call the open handler
        gui.onOpen(player);
    }
    
    /**
     * Closes a GUI for a player.
     *
     * @param player The player
     */
    public void closeGUI(Player player) {
        if (player == null) {
            return;
        }
        
        // Close the inventory
        player.closeInventory();
    }
    
    /**
     * Handles a player closing a GUI.
     *
     * @param player The player
     */
    public void handleGUIClose(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        GUI gui = activeGUIs.remove(playerId);
        
        if (gui != null) {
            // Call the close handler
            gui.onClose(player);
        }
    }
    
    /**
     * Handles a GUI click event.
     *
     * @param event The click event
     */
    public void handleGUIClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            
            if (activeGUIs.containsKey(playerId)) {
                GUI gui = activeGUIs.get(playerId);
                Inventory clickedInventory = event.getClickedInventory();
                
                // Only handle clicks in the GUI inventory, not the player inventory
                if (clickedInventory != null && clickedInventory.equals(gui.getInventory())) {
                    int slot = event.getSlot();
                    
                    // Cancel the event by default to prevent item taking
                    event.setCancelled(true);
                    
                    try {
                        // Call the click handler
                        gui.onClick(player, slot, event);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error handling GUI click for " + player.getName(), e);
                    }
                }
            }
        }
    }
    
    /**
     * Updates a GUI for a player.
     *
     * @param player The player
     */
    public void updateGUI(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        if (activeGUIs.containsKey(playerId)) {
            GUI gui = activeGUIs.get(playerId);
            gui.update(player);
        }
    }
    
    /**
     * Gets the active GUI for a player.
     *
     * @param player The player
     * @return The active GUI, or null if none
     */
    public GUI getActiveGUI(Player player) {
        if (player == null) {
            return null;
        }
        
        return activeGUIs.get(player.getUniqueId());
    }
    
    /**
     * Checks if a player has an active GUI.
     *
     * @param player The player
     * @return true if the player has an active GUI
     */
    public boolean hasActiveGUI(Player player) {
        if (player == null) {
            return false;
        }
        
        return activeGUIs.containsKey(player.getUniqueId());
    }
    
    /**
     * Registers the built-in templates.
     */
    private void registerBuiltInTemplates() {
        // Register common templates
        registerTemplate("main_menu", new MainMenuTemplate());
        registerTemplate("confirmation", new ConfirmationTemplate());
        registerTemplate("settings", new SettingsTemplate());
        registerTemplate("selector", new SelectorTemplate());
    }
    
    /**
     * Registers event listeners for inventory events.
     */
    private void registerEventListeners() {
        plugin.getServer().getPluginManager().registerEvents(new GUIEventListener(this), plugin);
    }
} 
