package com.essentialscore.api.integration.worldguard;

import com.essentialscore.api.integration.AbstractPluginDependentIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Integration with WorldGuard for region protection.
 */
public class WorldGuardIntegration extends AbstractPluginDependentIntegration {
    // WorldGuard version checking
    private static final int MAJOR_VERSION_6 = 6;
    private static final int MAJOR_VERSION_7 = 7;
    
    // Reflection objects for 6.x and 7.x APIs
    private Object worldGuardPlugin;
    private Object regionContainer;
    private Method regionContainerGetMethod;
    private Method worldAdapterGetMethod;
    private Method hasRegionMethod;
    private Method regionCanBuildMethod;
    private Method createQueryMethod;
    private int worldGuardVersion;
    
    /**
     * Creates a new WorldGuard integration.
     *
     * @param plugin The EssentialsCore plugin
     */
    public WorldGuardIntegration(Plugin plugin) {
        super(plugin, "WorldGuard");
    }
    
    /**
     * Gets the plugin that this integration belongs to.
     * 
     * @return The plugin instance
     */
    @Override
    public Plugin getPlugin() {
        return plugin;
    }
    
    @Override
    protected void onPluginInitialize() {
        try {
            // Get WorldGuard plugin
            worldGuardPlugin = dependencyPlugin;
            
            // Determine WorldGuard version
            worldGuardVersion = determineWorldGuardVersion();
            
            // Initialize based on version
            if (worldGuardVersion == MAJOR_VERSION_6) {
                initializeWorldGuard6();
            } else if (worldGuardVersion == MAJOR_VERSION_7) {
                initializeWorldGuard7();
            } else {
                throw new IllegalStateException("Unsupported WorldGuard version: " + worldGuardVersion);
            }
            
            logger.info("WorldGuard integration initialized. Version: " + worldGuardVersion + ".x");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize WorldGuard integration", e);
            throw new RuntimeException("Failed to initialize WorldGuard integration", e);
        }
    }
    
    @Override
    protected void onPluginShutdown() {
        regionContainer = null;
        regionContainerGetMethod = null;
        worldAdapterGetMethod = null;
        hasRegionMethod = null;
        regionCanBuildMethod = null;
        createQueryMethod = null;
        regionCanBuildMethod = null;
    }
    
    @Override
    public String getName() {
        return "WorldGuard";
    }
    
    /**
     * Determines the WorldGuard version.
     *
     * @return The major version number
     */
    private int determineWorldGuardVersion() {
        try {
            // Check for WorldGuard 7.x (package: com.sk89q.worldguard.WorldGuard)
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return MAJOR_VERSION_7;
        } catch (ClassNotFoundException e) {
            try {
                // Check for WorldGuard 6.x (package: com.sk89q.worldguard.bukkit.WorldGuardPlugin)
                Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
                return MAJOR_VERSION_6;
            } catch (ClassNotFoundException e1) {
                throw new IllegalStateException("Could not determine WorldGuard version");
            }
        }
    }
    
    /**
     * Initializes for WorldGuard 6.x.
     */
    private void initializeWorldGuard6() throws Exception {
        // Get region container
        Class<?> worldGuardPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
        Method getRegionContainerMethod = worldGuardPluginClass.getMethod("getRegionContainer");
        regionContainer = getRegionContainerMethod.invoke(worldGuardPlugin);
        
        // Get required methods
        Class<?> regionContainerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
        regionContainerGetMethod = regionContainerClass.getMethod("get", Class.forName("org.bukkit.World"));
        
        Class<?> regionManagerClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
        hasRegionMethod = regionManagerClass.getMethod("hasRegion", String.class);
        Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
        regionCanBuildMethod = worldGuardClass.getMethod("canBuild", Player.class, Location.class);
    }
    
    /**
     * Initializes for WorldGuard 7.x.
     */
    private void initializeWorldGuard7() throws Exception {
        // Get WorldGuard
        Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Method getInstance = worldGuardClass.getMethod("getInstance");
        Object worldGuard = getInstance.invoke(null);
        
        // Get platform manager and region container
        Method getPlatform = worldGuardClass.getMethod("getPlatform");
        Object platform = getPlatform.invoke(worldGuard);
        
        Method getRegionContainer = platform.getClass().getMethod("getRegionContainer");
        regionContainer = getRegionContainer.invoke(platform);
        
        // Get methods for adapter and region manager
        Class<?> regionContainerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
        Class<?> worldEditWorldClass = Class.forName("com.sk89q.worldedit.world.World");
        regionContainerGetMethod = regionContainerClass.getMethod("get", worldEditWorldClass);
        
        Class<?> bukkit = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        worldAdapterGetMethod = bukkit.getMethod("adapt", Class.forName("org.bukkit.World"));
        
        // Get region methods
        Class<?> regionManagerClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
        hasRegionMethod = regionManagerClass.getMethod("hasRegion", String.class);
        
        Class<?> regionQueryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
        // Store createQuery method as a class field for later use in canBuild method
        this.createQueryMethod = regionContainerClass.getMethod("createQuery");
        
        Class<?> locClass = Class.forName("com.sk89q.worldedit.util.Location");
        regionCanBuildMethod = regionQueryClass.getMethod("testBuild", locClass, Class.forName("com.sk89q.worldguard.LocalPlayer"));
        regionCanBuildMethod = regionQueryClass.getMethod("testBuild", locClass, Class.forName("com.sk89q.worldguard.LocalPlayer"));
    }
    
    /**
     * Checks if a region exists in a world.
     *
     * @param worldName The world name
     * @param regionId The region ID
     * @return true if the region exists
     */
    public boolean hasRegion(String worldName, String regionId) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                return false;
            }
            
            Object regionManager = getRegionManager(world);
            if (regionManager == null) {
                return false;
            }
            
            return (Boolean) hasRegionMethod.invoke(regionManager, regionId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check if region exists: " + regionId, e);
            return false;
        }
    }
    
    /**
     * Checks if a player can build at a location.
     *
     * @param player The player
     * @param location The location
     * @return true if the player can build
     */
    public boolean canBuild(Player player, Location location) {
        if (!isAvailable() || player == null || location == null) {
            return true; // Default to allowing if not available
        }
        
        try {
            if (worldGuardVersion == MAJOR_VERSION_6) {
                return (Boolean) regionCanBuildMethod.invoke(worldGuardPlugin, player, location);
            } else {
                // WorldGuard 7.x uses different API
                Class<?> bukkit = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Method adaptPlayer = bukkit.getMethod("adapt", Player.class);
                Object localPlayer = adaptPlayer.invoke(null, player);
                
                Method adaptLoc = bukkit.getMethod("adapt", Location.class);
                Object loc = adaptLoc.invoke(null, location);
                Object regionQuery = createQueryMethod.invoke(regionContainer);
                
                Method testBuild = regionQuery.getClass().getMethod("testBuild", loc.getClass(), localPlayer.getClass());
                return (Boolean) testBuild.invoke(regionQuery, loc, localPlayer);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check if player can build: " + player.getName(), e);
            return true; // Default to allowing on error
        }
    }
    
    /**
     * Gets the region manager for a world.
     *
     * @param world The world
     * @return The region manager, or null if not available
     */
    private Object getRegionManager(org.bukkit.World world) {
        try {
            if (worldGuardVersion == MAJOR_VERSION_6) {
                return regionContainerGetMethod.invoke(regionContainer, world);
            } else {
                // WorldGuard 7.x needs to adapt the world first
                Object worldEditWorld = worldAdapterGetMethod.invoke(null, world);
                return regionContainerGetMethod.invoke(regionContainer, worldEditWorld);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get region manager for world: " + world.getName(), e);
            return null;
        }
    }
} 
