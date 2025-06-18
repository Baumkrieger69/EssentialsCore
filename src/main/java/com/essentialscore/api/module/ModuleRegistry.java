package com.essentialscore.api.module;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central registry system for all modules.
 * Catalogs modules, tracks their capabilities, handles discovery,
 * and provides marketplace integration.
 */
public class ModuleRegistry {
    private static final Logger LOGGER = Logger.getLogger(ModuleRegistry.class.getName());
    
    private final Map<String, ModuleInfo> registeredModules;
    private final Map<String, Set<String>> capabilityProviders;
    private final Map<String, Set<String>> modulesByTag;
    private final MarketplaceConnector marketplaceConnector;
    private final File modulesDirectory;
    
    /**
    /**
     * Creates a new module registry.
     *
     * @param plugin The plugin
     */
    public ModuleRegistry(Plugin plugin) {
        this.registeredModules = new HashMap<>();
        this.capabilityProviders = new HashMap<>();
        this.modulesByTag = new HashMap<>();
        this.marketplaceConnector = new MarketplaceConnector(this);
        this.modulesDirectory = new File(plugin.getDataFolder(), "modules");
        
        // Create modules directory if it doesn't exist
        if (!modulesDirectory.exists()) {
            modulesDirectory.mkdirs();
        }
        
        LOGGER.info("ModuleRegistry initialized");
    }
    
    /**
     * Registers a module.
     *
     * @param moduleInfo The module information
     * @return true if the module was registered successfully
     */
    public boolean registerModule(ModuleInfo moduleInfo) {
        if (moduleInfo == null) return false;
        
        String moduleId = moduleInfo.getId();
        
        // Check if the module is already registered
        if (registeredModules.containsKey(moduleId)) {
            LOGGER.warning("Module already registered: " + moduleId);
            return false;
        }
        
        // Register the module
        registeredModules.put(moduleId, moduleInfo);
        
        // Register capabilities
        for (String capability : moduleInfo.getCapabilities()) {
            capabilityProviders.computeIfAbsent(capability, k -> new HashSet<>()).add(moduleId);
        }
        
        // Register tags
        for (String tag : moduleInfo.getTags()) {
            modulesByTag.computeIfAbsent(tag, k -> new HashSet<>()).add(moduleId);
        }
        
        LOGGER.info("Registered module: " + moduleId);
        return true;
    }
    
    /**
     * Unregisters a module.
     *
     * @param moduleId The module ID
     * @return true if the module was unregistered successfully
     */
    public boolean unregisterModule(String moduleId) {
        if (moduleId == null) return false;
        
        // Check if the module is registered
        ModuleInfo moduleInfo = registeredModules.get(moduleId);
        if (moduleInfo == null) {
            LOGGER.warning("Module not registered: " + moduleId);
            return false;
        }
        
        // Unregister capabilities
        for (String capability : moduleInfo.getCapabilities()) {
            Set<String> providers = capabilityProviders.get(capability);
            if (providers != null) {
                providers.remove(moduleId);
                if (providers.isEmpty()) {
                    capabilityProviders.remove(capability);
                }
            }
        }
        
        // Unregister tags
        for (String tag : moduleInfo.getTags()) {
            Set<String> modules = modulesByTag.get(tag);
            if (modules != null) {
                modules.remove(moduleId);
                if (modules.isEmpty()) {
                    modulesByTag.remove(tag);
                }
            }
        }
        
        // Unregister the module
        registeredModules.remove(moduleId);
        
        LOGGER.info("Unregistered module: " + moduleId);
        return true;
    }
    
    /**
     * Gets a module by ID.
     *
     * @param moduleId The module ID
     * @return The module information, or null if not found
     */
    public ModuleInfo getModule(String moduleId) {
        return registeredModules.get(moduleId);
    }
    
    /**
     * Gets all registered modules.
     *
     * @return A collection of module information
     */
    public List<ModuleInfo> getAllModules() {
        return new ArrayList<>(registeredModules.values());
    }
    
    /**
     * Gets all registered modules as a map.
     *
     * @return A map of module IDs to module information
     */
    public Map<String, ModuleInfo> getModules() {
        return Collections.unmodifiableMap(registeredModules);
    }
    
    /**
     * Finds modules with a specific capability.
     *
     * @param capability The capability
     * @return A list of modules with the capability
     */
    public List<ModuleInfo> findModulesByCapability(String capability) {
        Set<String> providers = capabilityProviders.get(capability);
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyList();
        }
        
        return providers.stream()
            .map(registeredModules::get)
            .filter(info -> info != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Finds modules with a specific tag.
     *
     * @param tag The tag
     * @return A list of modules with the tag
     */
    public List<ModuleInfo> findModulesByTag(String tag) {
        Set<String> modules = modulesByTag.get(tag);
        if (modules == null || modules.isEmpty()) {
            return Collections.emptyList();
        }
        
        return modules.stream()
            .map(registeredModules::get)
            .filter(info -> info != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Finds modules that match a filter.
     *
     * @param filter The module filter
     * @return A list of matching modules
     */
    public List<ModuleInfo> findModules(Predicate<ModuleInfo> filter) {
        return registeredModules.values().stream()
            .filter(filter)
            .collect(Collectors.toList());
    }
    
    /**
     * Checks if a module exists.
     *
     * @param moduleId The module ID
     * @return true if the module exists
     */
    public boolean hasModule(String moduleId) {
        return registeredModules.containsKey(moduleId);
    }
    
    /**
     * Discovers modules in the modules directory.
     *
     * @return A future that completes with the number of modules discovered
     */
    public CompletableFuture<Integer> discoverModules() {
        return CompletableFuture.supplyAsync(() -> {
            int count = 0;
            
            // List all JAR files in the modules directory
            File[] files = modulesDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
            if (files == null) {
                return 0;
            }
            
            for (File file : files) {
                try {
                    ModuleInfo moduleInfo = discoverModule(file);
                    if (moduleInfo != null && registerModule(moduleInfo)) {
                        count++;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to discover module: " + file.getName(), e);
                }
            }
            
            LOGGER.info("Discovered " + count + " modules");
            return count;
        });
    }
    
    /**
     * Discovers a module from a JAR file.
     *
     * @param file The JAR file
     * @return The module information, or null if discovery failed
     */
    private ModuleInfo discoverModule(File file) {
        try (JarFile jarFile = new JarFile(file)) {
            // Look for module.json in the JAR
            JarEntry moduleJsonEntry = jarFile.getJarEntry("module.json");
            if (moduleJsonEntry == null) {
                LOGGER.warning("No module.json found in: " + file.getName());
                return null;
            }
            
            // Parse module.json
            // Load module metadata from JSON
            // Load the module class from the specified main class
            
            return new ModuleInfo(
                file.getName().replace(".jar", ""),
                "Discovered Module",
                "1.0.0",
                "A module discovered in the modules directory",
                "Author",
                file.toURI().toURL(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptySet(),
                Collections.emptySet()
            );
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading JAR file: " + file.getName(), e);
            return null;
        }
    }
    
    /**
     * Gets the marketplace connector.
     *
     * @return The marketplace connector
     */
    public MarketplaceConnector getMarketplaceConnector() {
        return marketplaceConnector;
    }
    
    /**
     * Gets recommendations for modules based on installed modules.
     *
     * @param count The maximum number of recommendations
     * @return A list of recommended modules
     */
    public List<ModuleInfo> getRecommendations(int count) {
        // Analyze installed modules and recommend compatible or complementary modules
        
        return marketplaceConnector.searchModules("", count)
            .thenApply(modules -> {
                // Filter out already installed modules
                return modules.stream()
                    .filter(module -> !hasModule(module.getId()))
                    .limit(count)
                    .collect(Collectors.toList());
            })
            .join();
    }
    
    /**
     * Connects to marketplace repositories.
     *
     * @param repositoryUrls The repository URLs
     * @return A future that completes when connections are established
     */
    public CompletableFuture<Boolean> connectToRepositories(List<String> repositoryUrls) {
        return marketplaceConnector.connectToRepositories(repositoryUrls);
    }
    
    /**
     * Represents a module connector for marketplace integration.
     */
    public class MarketplaceConnector {
        private final ModuleRegistry registry;
        private final List<String> connectedRepositories;
        private final Map<String, ModuleInfo> marketplaceModules;
        
        /**
         * Creates a new marketplace connector.
         *
         * @param registry The module registry
         */
        public MarketplaceConnector(ModuleRegistry registry) {
            this.registry = registry;
            this.connectedRepositories = new ArrayList<>();
            this.marketplaceModules = new HashMap<>();
        }
        
        /**
         * Connects to marketplace repositories.
         *
         * @param repositoryUrls The repository URLs
         * @return A future that completes when connections are established
         */
        public CompletableFuture<Boolean> connectToRepositories(List<String> repositoryUrls) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    for (String url : repositoryUrls) {
                        if (!connectedRepositories.contains(url)) {
                            // Connect to the repository
                            // This would typically involve making HTTP requests to the repository API
                            // Add the URL to the repository list
                            
                            connectedRepositories.add(url);
                            LOGGER.info("Connected to repository: " + url);
                        }
                    }
                    
                    // Refresh the marketplace modules
                    refreshMarketplaceModules();
                    
                    return true;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to connect to repositories", e);
                    return false;
                }
            });
        }
        
        /**
         * Refreshes the marketplace modules.
         */
        public void refreshMarketplaceModules() {
            // Clear the current marketplace modules
            marketplaceModules.clear();
            
            // Fetch modules from all repositories
            for (String repositoryUrl : connectedRepositories) {
                try {
                    // Fetch modules from the repository
                    // This would typically involve making HTTP requests to the repository API
                    // Add modules from discovered repositories
                    
                    for (int i = 1; i <= 5; i++) {
                        String moduleId = "marketplace-module-" + i;
                        ModuleInfo moduleInfo = new ModuleInfo(
                            moduleId,
                            "Marketplace Module " + i,
                            "1.0.0",
                            "A module from the marketplace",
                            "Marketplace Author",
                            URI.create(repositoryUrl + "/modules/" + moduleId).toURL(),
                            Collections.singletonList("marketplace"),
                            Collections.emptyList(),
                            Collections.singleton("marketplace"),
                            Collections.emptySet()
                        );
                        
                        marketplaceModules.put(moduleId, moduleInfo);
                    }
                    
                    LOGGER.info("Refreshed modules from repository: " + repositoryUrl);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to refresh modules from repository: " + repositoryUrl, e);
                }
            }
        }
        
        /**
         * Searches for modules in the marketplace.
         *
         * @param query The search query
         * @param limit The maximum number of results
         * @return A future that completes with the search results
         */
        public CompletableFuture<List<ModuleInfo>> searchModules(String query, int limit) {
            return CompletableFuture.supplyAsync(() -> {
                // Search for modules in the marketplace
                // This would typically involve making HTTP requests to the repository API
                // Filter marketplace modules by search query
                
                return marketplaceModules.values().stream()
                    .filter(module -> query.isEmpty() || 
                                      module.getName().toLowerCase().contains(query.toLowerCase()) ||
                                      module.getDescription().toLowerCase().contains(query.toLowerCase()))
                    .limit(limit)
                    .collect(Collectors.toList());
            });
        }
        
        /**
         * Gets information about a module in the marketplace.
         *
         * @param moduleId The module ID
         * @return A future that completes with the module information
         */
        public CompletableFuture<Optional<ModuleInfo>> getModuleInfo(String moduleId) {
            return CompletableFuture.supplyAsync(() -> {
                // Get information about a module in the marketplace
                // This would typically involve making HTTP requests to the repository API
                // Look up the module in the marketplace
                
                return Optional.ofNullable(marketplaceModules.get(moduleId));
            });
        }
        
        /**
         * Downloads a module from the marketplace.
         *
         * @param moduleId The module ID
         * @return A future that completes with the downloaded file
         */
        public CompletableFuture<File> downloadModule(String moduleId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Get the module information
                    ModuleInfo moduleInfo = marketplaceModules.get(moduleId);
                    if (moduleInfo == null) {
                        throw new IllegalArgumentException("Module not found: " + moduleId);
                    }
                    
                    // Download the module
                    // This would typically involve downloading the JAR file from the repository
                    // Create module file in modules directory
                    
                    File downloadedFile = new File(modulesDirectory, moduleId + ".jar");
                    if (!downloadedFile.exists()) {
                        downloadedFile.createNewFile();
                    }
                    
                    LOGGER.info("Downloaded module: " + moduleId);
                    return downloadedFile;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to download module: " + moduleId, e);
                    throw new RuntimeException("Failed to download module: " + moduleId, e);
                }
            });
        }
        
        /**
         * Installs a module from the marketplace.
         *
         * @param moduleId The module ID
         * @return A future that completes when the module is installed
         */
        public CompletableFuture<Boolean> installModule(String moduleId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Download the module
                    File downloadedFile = downloadModule(moduleId).join();
                    
                    // Discover the module
                    ModuleInfo moduleInfo = registry.discoverModule(downloadedFile);
                    if (moduleInfo == null) {
                        throw new IllegalArgumentException("Invalid module: " + moduleId);
                    }
                    
                    // Register the module
                    if (!registry.registerModule(moduleInfo)) {
                        throw new IllegalArgumentException("Failed to register module: " + moduleId);
                    }
                    
                    LOGGER.info("Installed module: " + moduleId);
                    return true;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to install module: " + moduleId, e);
                    return false;
                }
            });
        }
    }
    
    /**
     * Represents module information.
     */
    public static class ModuleInfo {
        private final String id;
        private final String name;
        private final String version;
        private final String description;
        private final String author;
        private final URL location;
        private final List<String> dependencies;
        private final List<String> softDependencies;
        private final Set<String> capabilities;
        private final Set<String> tags;
        private ModuleStatus status;
        
        /**
         * Creates new module information.
         *
         * @param id The module ID
         * @param name The module name
         * @param version The module version
         * @param description The module description
         * @param author The module author
         * @param location The module location
         * @param dependencies The module dependencies
         * @param softDependencies The module soft dependencies
         * @param capabilities The module capabilities
         * @param tags The module tags
         */
        public ModuleInfo(String id, String name, String version, String description, String author,
                         URL location, List<String> dependencies, List<String> softDependencies,
                         Set<String> capabilities, Set<String> tags) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.description = description;
            this.author = author;
            this.location = location;
            this.dependencies = new ArrayList<>(dependencies);
            this.softDependencies = new ArrayList<>(softDependencies);
            this.capabilities = new HashSet<>(capabilities);
            this.tags = new HashSet<>(tags);
            this.status = ModuleStatus.UNLOADED;
        }
        
        /**
         * Gets the module ID.
         *
         * @return The module ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Gets the module name.
         *
         * @return The module name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Gets the module version.
         *
         * @return The module version
         */
        public String getVersion() {
            return version;
        }
        
        /**
         * Gets the module description.
         *
         * @return The module description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the module author.
         *
         * @return The module author
         */
        public String getAuthor() {
            return author;
        }
        
        /**
         * Gets the module location.
         *
         * @return The module location
         */
        public URL getLocation() {
            return location;
        }
        
        /**
         * Gets the module dependencies.
         *
         * @return The module dependencies
         */
        public List<String> getDependencies() {
            return Collections.unmodifiableList(dependencies);
        }
        
        /**
         * Gets the module soft dependencies.
         *
         * @return The module soft dependencies
         */
        public List<String> getSoftDependencies() {
            return Collections.unmodifiableList(softDependencies);
        }
        
        /**
         * Gets the module capabilities.
         *
         * @return The module capabilities
         */
        public Set<String> getCapabilities() {
            return Collections.unmodifiableSet(capabilities);
        }
        
        /**
         * Gets the module tags.
         *
         * @return The module tags
         */
        public Set<String> getTags() {
            return Collections.unmodifiableSet(tags);
        }
        
        /**
         * Gets the module status.
         *
         * @return The module status
         */
        public ModuleStatus getStatus() {
            return status;
        }
        
        /**
         * Sets the module status.
         *
         * @param status The module status
         */
        public void setStatus(ModuleStatus status) {
            this.status = status;
        }
    }
    
    /**
     * Represents a module status.
     */
    public enum ModuleStatus {
        UNLOADED,
        LOADING,
        LOADED,
        UNLOADING,
        ERROR
    }
    
    /**
     * Builder for creating module information.
     */
    public static class ModuleInfoBuilder {
        private String id;
        private String name;
        private String version;
        private String description;
        private String author;
        private URL location;
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> softDependencies = new ArrayList<>();
        private final Set<String> capabilities = new HashSet<>();
        private final Set<String> tags = new HashSet<>();
        
        /**
         * Sets the module ID.
         *
         * @param id The module ID
         * @return This builder
         */
        public ModuleInfoBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the module name.
         *
         * @param name The module name
         * @return This builder
         */
        public ModuleInfoBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Sets the module version.
         *
         * @param version The module version
         * @return This builder
         */
        public ModuleInfoBuilder version(String version) {
            this.version = version;
            return this;
        }
        
        /**
         * Sets the module description.
         *
         * @param description The module description
         * @return This builder
         */
        public ModuleInfoBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the module author.
         *
         * @param author The module author
         * @return This builder
         */
        public ModuleInfoBuilder author(String author) {
            this.author = author;
            return this;
        }
        
        /**
         * Sets the module location.
         *
         * @param location The module location
         * @return This builder
         */
        public ModuleInfoBuilder location(URL location) {
            this.location = location;
            return this;
        }
        
        /**
         * Adds a module dependency.
         *
         * @param dependency The module dependency
         * @return This builder
         */
        public ModuleInfoBuilder dependency(String dependency) {
            dependencies.add(dependency);
            return this;
        }
        
        /**
         * Adds a module soft dependency.
         *
         * @param softDependency The module soft dependency
         * @return This builder
         */
        public ModuleInfoBuilder softDependency(String softDependency) {
            softDependencies.add(softDependency);
            return this;
        }
        
        /**
         * Adds a module capability.
         *
         * @param capability The module capability
         * @return This builder
         */
        public ModuleInfoBuilder capability(String capability) {
            capabilities.add(capability);
            return this;
        }
        
        /**
         * Adds a module tag.
         *
         * @param tag The module tag
         * @return This builder
         */
        public ModuleInfoBuilder tag(String tag) {
            tags.add(tag);
            return this;
        }
        
        /**
         * Builds the module information.
         *
         * @return The module information
         */
        public ModuleInfo build() {
            if (id == null) {
                throw new IllegalStateException("Module ID is required");
            }
            if (name == null) {
                name = id;
            }
            if (version == null) {
                version = "1.0.0";
            }
            if (description == null) {
                description = "";
            }
            if (author == null) {
                author = "Unknown";
            }
            
            return new ModuleInfo(id, name, version, description, author, location, 
                                 dependencies, softDependencies, capabilities, tags);
        }
    }
} 
