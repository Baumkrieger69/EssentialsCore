package com.essentialscore.api.module;

import com.essentialscore.api.BasePlugin;
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

/**
 * Manages module loading, unloading, and lifecycle.
 */
public class ModuleManager {
    private final Plugin plugin;
    private final BasePlugin basePlugin;
    private final File modulesDirectory;
    private final Map<String, ModuleInfo> loadedModules;
    private final Map<String, ModuleLogger> moduleLoggers;
    private final Map<String, Boolean> moduleEnableStatus;
    private final List<String> loadOrder;
    private boolean isShuttingDown;

    /**
     * Creates a new module manager.
     *
     * @param plugin The plugin
     * @param basePlugin The base plugin
     * @param modulesDirectory The modules directory
     */
    public ModuleManager(Plugin plugin, BasePlugin basePlugin, File modulesDirectory) {
        this.plugin = plugin;
        this.basePlugin = basePlugin;
        this.modulesDirectory = modulesDirectory;
        this.loadedModules = new ConcurrentHashMap<>();
        this.moduleLoggers = new ConcurrentHashMap<>();
        this.moduleEnableStatus = new ConcurrentHashMap<>();
        this.loadOrder = new ArrayList<>();
        this.isShuttingDown = false;

        if (!modulesDirectory.exists()) {
            modulesDirectory.mkdirs();
        }
    }

    /**
     * Loads all modules from the modules directory.
     *
     * @return The number of modules loaded
     */
    public int loadModules() {
        if (isShuttingDown) {
            return 0;
        }

        int count = 0;
        File[] files = modulesDirectory.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (loadModule(file)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Loads a module from a jar file.
     *
     * @param file The jar file
     * @return True if the module was loaded successfully
     */
    public boolean loadModule(File file) {
        if (isShuttingDown) {
            return false;
        }

        if (!file.exists() || !file.isFile() || !file.getName().endsWith(".jar")) {
            plugin.getLogger().warning("Invalid module file: " + file.getName());
            return false;
        }

        String moduleName = file.getName().substring(0, file.getName().length() - 4);
        
        // Check if already loaded
        if (loadedModules.containsKey(moduleName)) {
            plugin.getLogger().info("Module already loaded: " + moduleName);
            return false;
        }

        try {
            // Read module info
            JarFile jarFile = new JarFile(file);
            ZipEntry configEntry = jarFile.getEntry("module.yml");
            
            if (configEntry == null) {
                plugin.getLogger().warning("Missing module.yml in " + file.getName());
                jarFile.close();
                return false;
            }

            InputStream configStream = jarFile.getInputStream(configEntry);
            YamlConfiguration moduleConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(configStream, StandardCharsets.UTF_8));
            configStream.close();

            String mainClass = moduleConfig.getString("main");
            String version = moduleConfig.getString("version", "1.0");
            String description = moduleConfig.getString("description", "No description");
            
            if (mainClass == null) {
                plugin.getLogger().warning("Missing main class in module.yml for " + file.getName());
                jarFile.close();
                return false;
            }

            // Create class loader and load main class
            URL[] urls = new URL[]{ file.toURI().toURL() };
            URLClassLoader classLoader = new URLClassLoader(urls, plugin.getClass().getClassLoader());
            Class<?> moduleClass = classLoader.loadClass(mainClass);
            Object moduleInstance = moduleClass.getDeclaredConstructor().newInstance();

            // Create module info
            ModuleInfo moduleInfo = new ModuleInfo(moduleName, version, description, moduleInstance, classLoader);
            loadedModules.put(moduleName, moduleInfo);
            loadOrder.add(moduleName);

            // Create module logger
            ModuleLogger moduleLogger = new ModuleLogger(plugin.getLogger(), moduleName, basePlugin.isDebugMode());
            moduleLoggers.put(moduleName, moduleLogger);

            // Initialize module
            initializeModule(moduleName, moduleInstance, moduleConfig);
            moduleEnableStatus.put(moduleName, true);

            plugin.getLogger().info("Loaded module: " + moduleName + " v" + version);
            jarFile.close();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading module: " + moduleName, e);
            return false;
        }
    }

    /**
     * Initializes a module with its configuration.
     *
     * @param moduleName The module name
     * @param moduleInstance The module instance
     * @param moduleConfig The module configuration
     */
    private void initializeModule(String moduleName, Object moduleInstance, FileConfiguration moduleConfig) {
        try {
            if (moduleInstance instanceof Module) {
                Module module = (Module) moduleInstance;
                module.onLoad(basePlugin, moduleLoggers.get(moduleName), moduleConfig);
            } else {
                // Try reflection for older modules
                try {
                    Method onLoadMethod = moduleInstance.getClass().getMethod("onLoad", BasePlugin.class, Logger.class, FileConfiguration.class);
                    onLoadMethod.invoke(moduleInstance, basePlugin, moduleLoggers.get(moduleName), moduleConfig);
                } catch (NoSuchMethodException e) {
                    // No onLoad method, try a simpler init method
                    try {
                        Method initMethod = moduleInstance.getClass().getMethod("init", Plugin.class);
                        initMethod.invoke(moduleInstance, plugin);
                    } catch (NoSuchMethodException ex) {
                        plugin.getLogger().warning("Module " + moduleName + " has no initialization method");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error initializing module: " + moduleName, e);
            moduleEnableStatus.put(moduleName, false);
        }
    }

    /**
     * Unloads a module.
     *
     * @param moduleName The module name
     * @return True if the module was unloaded successfully
     */
    public boolean unloadModule(String moduleName) {
        if (!loadedModules.containsKey(moduleName)) {
            return false;
        }

        try {
            ModuleInfo moduleInfo = loadedModules.get(moduleName);
            Object moduleInstance = moduleInfo.getInstance();

            // Call disable method
            if (moduleInstance instanceof Module) {
                ((Module) moduleInstance).onDisable();
            } else {
                try {
                    Method onDisableMethod = moduleInstance.getClass().getMethod("onDisable");
                    onDisableMethod.invoke(moduleInstance);
                } catch (NoSuchMethodException e) {
                    // No onDisable method, that's okay
                }
            }

            // Close class loader
            moduleInfo.getClassLoader().close();

            // Remove from maps
            loadedModules.remove(moduleName);
            moduleLoggers.remove(moduleName);
            moduleEnableStatus.remove(moduleName);
            loadOrder.remove(moduleName);

            plugin.getLogger().info("Unloaded module: " + moduleName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error unloading module: " + moduleName, e);
            return false;
        }
    }

    /**
     * Enables a module.
     *
     * @param moduleName The module name
     * @return True if the module was enabled successfully
     */
    public boolean enableModule(String moduleName) {
        if (!loadedModules.containsKey(moduleName) || moduleEnableStatus.getOrDefault(moduleName, false)) {
            return false;
        }

        try {
            ModuleInfo moduleInfo = loadedModules.get(moduleName);
            Object moduleInstance = moduleInfo.getInstance();

            // Call enable method
            if (moduleInstance instanceof Module) {
                ((Module) moduleInstance).onEnable();
            } else {
                try {
                    Method onEnableMethod = moduleInstance.getClass().getMethod("onEnable");
                    onEnableMethod.invoke(moduleInstance);
                } catch (NoSuchMethodException e) {
                    // No onEnable method, that's okay
                }
            }

            moduleEnableStatus.put(moduleName, true);
            plugin.getLogger().info("Enabled module: " + moduleName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error enabling module: " + moduleName, e);
            return false;
        }
    }

    /**
     * Disables a module.
     *
     * @param moduleName The module name
     * @return True if the module was disabled successfully
     */
    public boolean disableModule(String moduleName) {
        if (!loadedModules.containsKey(moduleName) || !moduleEnableStatus.getOrDefault(moduleName, true)) {
            return false;
        }

        try {
            ModuleInfo moduleInfo = loadedModules.get(moduleName);
            Object moduleInstance = moduleInfo.getInstance();

            // Call disable method
            if (moduleInstance instanceof Module) {
                ((Module) moduleInstance).onDisable();
            } else {
                try {
                    Method onDisableMethod = moduleInstance.getClass().getMethod("onDisable");
                    onDisableMethod.invoke(moduleInstance);
                } catch (NoSuchMethodException e) {
                    // No onDisable method, that's okay
                }
            }

            moduleEnableStatus.put(moduleName, false);
            plugin.getLogger().info("Disabled module: " + moduleName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error disabling module: " + moduleName, e);
            return false;
        }
    }

    /**
     * Reloads a module.
     *
     * @param moduleName The module name
     * @return True if the module was reloaded successfully
     */
    public boolean reloadModule(String moduleName) {
        if (!loadedModules.containsKey(moduleName)) {
            return false;
        }

        try {
            ModuleInfo moduleInfo = loadedModules.get(moduleName);
            Object moduleInstance = moduleInfo.getInstance();

            // Call reload method
            if (moduleInstance instanceof Module) {
                ((Module) moduleInstance).onReload();
                return true;
            } else {
                try {
                    Method onReloadMethod = moduleInstance.getClass().getMethod("onReload");
                    onReloadMethod.invoke(moduleInstance);
                    return true;
                } catch (NoSuchMethodException e) {
                    // No onReload method, try disable + enable
                    disableModule(moduleName);
                    return enableModule(moduleName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reloading module: " + moduleName, e);
            return false;
        }
    }

    /**
     * Reloads all modules.
     */
    public void reloadModules() {
        for (String moduleName : new ArrayList<>(loadOrder)) {
            reloadModule(moduleName);
        }
    }

    /**
     * Unloads all modules.
     */
    public void unloadModules() {
        isShuttingDown = true;
        
        // Unload in reverse order
        List<String> reverseOrder = new ArrayList<>(loadOrder);
        Collections.reverse(reverseOrder);
        
        for (String moduleName : reverseOrder) {
            unloadModule(moduleName);
        }
    }

    /**
     * Gets a module logger.
     *
     * @param moduleName The module name
     * @return The module logger, or null if not found
     */
    public ModuleLogger getModuleLogger(String moduleName) {
        return moduleLoggers.get(moduleName);
    }

    /**
     * Gets information about a module.
     *
     * @param moduleName The module name
     * @return The module information, or null if not found
     */
    public ModuleInfo getModuleInfo(String moduleName) {
        return loadedModules.get(moduleName);
    }

    /**
     * Gets all loaded modules.
     *
     * @return A map of module names to module information
     */
    public Map<String, ModuleInfo> getLoadedModules() {
        return Collections.unmodifiableMap(loadedModules);
    }

    /**
     * Gets the load order of modules.
     *
     * @return The load order
     */
    public List<String> getLoadOrder() {
        return Collections.unmodifiableList(loadOrder);
    }

    /**
     * Gets the enabled status of modules.
     *
     * @return A map of module names to enabled status
     */
    public Map<String, Boolean> getModuleEnableStatus() {
        return Collections.unmodifiableMap(moduleEnableStatus);
    }

    /**
     * Checks if a module is enabled.
     *
     * @param moduleName The module name
     * @return True if the module is enabled
     */
    public boolean isModuleEnabled(String moduleName) {
        return moduleEnableStatus.getOrDefault(moduleName, false);
    }

    /**
     * Checks if a module is loaded.
     *
     * @param moduleName The module name
     * @return True if the module is loaded
     */
    public boolean isModuleLoaded(String moduleName) {
        return loadedModules.containsKey(moduleName);
    }

    /**
     * Information about a module.
     */
    public static class ModuleInfo {
        private final String name;
        private final String version;
        private final String description;
        private final Object instance;
        private final URLClassLoader classLoader;

        /**
         * Creates new module information.
         *
         * @param name The module name
         * @param version The module version
         * @param description The module description
         * @param instance The module instance
         * @param classLoader The module class loader
         */
        public ModuleInfo(String name, String version, String description, Object instance, URLClassLoader classLoader) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.instance = instance;
            this.classLoader = classLoader;
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
         * Gets the module instance.
         *
         * @return The module instance
         */
        public Object getInstance() {
            return instance;
        }

        /**
         * Gets the module class loader.
         *
         * @return The module class loader
         */
        public URLClassLoader getClassLoader() {
            return classLoader;
        }
    }
} 