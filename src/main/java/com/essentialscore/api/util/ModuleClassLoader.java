package com.essentialscore.api.util;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * Custom class loader for modules that provides isolation and dependency management.
 */
public class ModuleClassLoader extends URLClassLoader {
    private final Plugin plugin;
    private final String moduleId;
    private final Map<String, Class<?>> classCache;
    private final List<String> loadedClasses;
    private final File jarFile;

    // Static mapping of modules to their class loaders
    private static final Map<String, ModuleClassLoader> MODULE_LOADERS = new ConcurrentHashMap<>();

    /**
     * Creates a new module class loader.
     *
     * @param plugin The plugin
     * @param moduleId The module ID
     * @param file The jar file
     * @param parent The parent class loader
     * @throws IOException If an I/O error occurs
     */
    public ModuleClassLoader(Plugin plugin, String moduleId, File file, ClassLoader parent) throws IOException {
        super(new URL[]{file.toURI().toURL()}, parent);
        this.plugin = plugin;
        this.moduleId = moduleId;
        this.classCache = new ConcurrentHashMap<>();
        this.loadedClasses = new ArrayList<>();
        this.jarFile = file;
        
        // Register in the static map
        MODULE_LOADERS.put(moduleId, this);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Check cache first
        Class<?> cachedClass = classCache.get(name);
        if (cachedClass != null) {
            return cachedClass;
        }
        
        // Try to load from this class loader
        try {
            Class<?> clazz = super.findClass(name);
            classCache.put(name, clazz);
            loadedClasses.add(name);
            return clazz;
        } catch (ClassNotFoundException e) {
            // Try to find in other module loaders
            for (ModuleClassLoader loader : MODULE_LOADERS.values()) {
                if (loader != this) {
                    try {
                        Class<?> clazz = loader.findClassInThisLoader(name);
                        if (clazz != null) {
                            classCache.put(name, clazz);
                            return clazz;
                        }
                    } catch (ClassNotFoundException ignored) {
                        // Continue to next loader
                    }
                }
            }
            
            // Not found
            throw new ClassNotFoundException(name);
        }
    }

    /**
     * Tries to find a class in this loader only, without delegating to other loaders.
     *
     * @param name The class name
     * @return The class, or null if not found
     * @throws ClassNotFoundException If the class is not found
     */
    protected Class<?> findClassInThisLoader(String name) throws ClassNotFoundException {
        Class<?> cachedClass = classCache.get(name);
        if (cachedClass != null) {
            return cachedClass;
        }
        
        return super.findClass(name);
    }

    @Override
    public void close() throws IOException {
        // Unregister from the static map
        MODULE_LOADERS.remove(moduleId);
        
        // Close resources
        super.close();
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
     * Gets the jar file.
     *
     * @return The jar file
     */
    public File getJarFile() {
        return jarFile;
    }

    /**
     * Gets the classes loaded by this loader.
     *
     * @return The loaded classes
     */
    public List<String> getLoadedClasses() {
        return new ArrayList<>(loadedClasses);
    }

    /**
     * Gets a class loader for a module.
     *
     * @param moduleId The module ID
     * @return The module class loader, or null if not found
     */
    public static ModuleClassLoader getLoader(String moduleId) {
        return MODULE_LOADERS.get(moduleId);
    }

    /**
     * Creates a new module class loader.
     *
     * @param plugin The plugin
     * @param moduleId The module ID
     * @param file The jar file
     * @return The module class loader
     */
    public static ModuleClassLoader createLoader(Plugin plugin, String moduleId, File file) {
        try {
            return new ModuleClassLoader(plugin, moduleId, file, plugin.getClass().getClassLoader());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating class loader for module " + moduleId, e);
            return null;
        }
    }

    /**
     * Gets all module class loaders.
     *
     * @return A map of module IDs to class loaders
     */
    public static Map<String, ModuleClassLoader> getLoaders() {
        return new HashMap<>(MODULE_LOADERS);
    }

    /**
     * Gets the number of classes loaded by this loader.
     *
     * @return The number of loaded classes
     */
    public int getClassCount() {
        return loadedClasses.size();
    }

    /**
     * Gets all classes in the jar file.
     *
     * @return A list of class names
     */
    public List<String> getAllClasses() {
        List<String> classes = new ArrayList<>();
        
        try (JarFile jar = new JarFile(jarFile)) {
            jar.stream()
                .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"))
                .map(JarEntry::getName)
                .map(name -> name.substring(0, name.length() - 6).replace('/', '.'))
                .forEach(classes::add);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error scanning jar for classes: " + jarFile.getName(), e);
        }
        
        return classes;
    }
} 
