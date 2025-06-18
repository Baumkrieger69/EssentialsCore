package com.essentialscore.api.module;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages file operations for modules.
 */
public class ModuleFileManager {
    private final Plugin plugin;
    private final ConcurrentHashMap<String, File> moduleDirectories;
    private final ConcurrentHashMap<String, File> moduleDataDirectories;
    private final File modulesDirectory;

    /**
     * Creates a new module file manager.
     *
     * @param plugin The plugin
     */
    public ModuleFileManager(Plugin plugin) {
        this.plugin = plugin;
        this.moduleDirectories = new ConcurrentHashMap<>();
        this.moduleDataDirectories = new ConcurrentHashMap<>();
        this.modulesDirectory = new File(plugin.getDataFolder(), "modules");
        
        if (!modulesDirectory.exists()) {
            modulesDirectory.mkdirs();
        }
    }

    /**
     * Gets the modules directory.
     *
     * @return The modules directory
     */
    public File getModulesDirectory() {
        return modulesDirectory;
    }

    /**
     * Gets the data directory for a module.
     *
     * @param moduleName The module name
     * @return The module's data directory
     */
    public File getModuleDataDirectory(String moduleName) {
        return moduleDataDirectories.computeIfAbsent(moduleName, name -> {
            File dataDir = new File(modulesDirectory, name + "/data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            return dataDir;
        });
    }

    /**
     * Gets the directory for a module.
     *
     * @param moduleName The module name
     * @return The module's directory
     */
    public File getModuleDirectory(String moduleName) {
        return moduleDirectories.computeIfAbsent(moduleName, name -> {
            File moduleDir = new File(modulesDirectory, name);
            if (!moduleDir.exists()) {
                moduleDir.mkdirs();
            }
            return moduleDir;
        });
    }

    /**
     * Gets a file in a module's directory.
     *
     * @param moduleName The module name
     * @param fileName The file name
     * @return The file
     */
    public File getModuleFile(String moduleName, String fileName) {
        return new File(getModuleDirectory(moduleName), fileName);
    }

    /**
     * Gets a file in a module's data directory.
     *
     * @param moduleName The module name
     * @param fileName The file name
     * @return The file
     */
    public File getModuleDataFile(String moduleName, String fileName) {
        return new File(getModuleDataDirectory(moduleName), fileName);
    }

    /**
     * Saves a resource from the plugin jar to a module's directory.
     *
     * @param moduleName The module name
     * @param resourcePath The resource path in the jar
     * @param replace Whether to replace an existing file
     */
    public void saveModuleResource(String moduleName, String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = plugin.getResource(resourcePath);
        
        if (in == null) {
            throw new IllegalArgumentException("The resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(getModuleDirectory(moduleName), resourcePath);
        File outDir = outFile.getParentFile();
        
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    /**
     * Deletes a module's directory.
     *
     * @param moduleName The module name
     * @return True if the directory was deleted successfully
     */
    public boolean deleteModuleDirectory(String moduleName) {
        File moduleDir = getModuleDirectory(moduleName);
        if (moduleDir.exists()) {
            return deleteDirectory(moduleDir);
        }
        return true;
    }

    /**
     * Recursively deletes a directory.
     *
     * @param directory The directory to delete
     * @return True if the directory was deleted successfully
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
} 
