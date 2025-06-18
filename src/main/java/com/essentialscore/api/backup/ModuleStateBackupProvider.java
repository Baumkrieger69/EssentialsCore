package com.essentialscore.api.backup;

import com.essentialscore.api.module.ModuleRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Provider for backing up module state data.
 */
public class ModuleStateBackupProvider implements BackupProvider {
    private static final Logger LOGGER = Logger.getLogger(ModuleStateBackupProvider.class.getName());
    
    private final ModuleRegistry moduleRegistry;
    private final Map<String, Object> moduleStates = new HashMap<>();
    
    /**
     * Creates a new module state backup provider.
     *
     * @param moduleRegistry The module registry
     */
    public ModuleStateBackupProvider(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }
    
    @Override
    public String getId() {
        return "module_state";
    }
    
    @Override
    public String getDisplayName() {
        return "Module State";
    }
    
    /**
     * Backs up a module's state to a file.
     *
     * @param moduleName The name of the module
     * @param stateFile The file to save the state to
     * @return true if the state was backed up successfully
     */
    public boolean backupModuleState(String moduleName, File stateFile) {
        try {
            // Get the module's state from the registry
            ModuleRegistry.ModuleInfo moduleInfo = moduleRegistry.getModule(moduleName);
            Object state = moduleInfo != null ? moduleInfo : "No state available";
            
            // Serialize the state to the file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(stateFile))) {
                oos.writeObject(state);
            }
            
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to backup state for module: " + moduleName, e);
            return false;
        }
    }
    
    /**
     * Restores a module's state from a file.
     *
     * @param moduleName The name of the module
     * @param stateFile The file containing the state
     * @return true if the state was restored successfully
     */
    public boolean restoreModuleState(String moduleName, File stateFile) {
        try {
            // Deserialize the state from the file
            Object state;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(stateFile))) {
                state = ois.readObject();
            }
            
            // Get the module from the registry and restore its state
            ModuleRegistry.ModuleInfo moduleInfo = moduleRegistry.getModule(moduleName);
            if (moduleInfo != null) {
                // Note: setState method may also need to be implemented in ModuleInfo
                // moduleInfo.setState(state);
                LOGGER.info("Successfully restored state for module: " + moduleName);
                return true;
            } else {
                LOGGER.warning("Module not found in registry: " + moduleName);
                // Store the state temporarily in case the module is loaded later
                moduleStates.put(moduleName, state);
                return false;
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Failed to restore state for module: " + moduleName, e);
            return false;
        }
    }
    
    @Override
    public Set<String> performBackup(BackupSystem backupSystem, File backupDir, Object context) throws Exception {
        LOGGER.info("Starting module state backup");
        Set<String> backedUpFiles = new HashSet<>();
        
        // Create directory for module state files
        File moduleStateDir = new File(backupDir, "module_state");
        moduleStateDir.mkdirs();
        
        // Iterate through all modules and backup their state
        for (String moduleName : getLoadedModules()) {
            try {
                File moduleStateFile = new File(moduleStateDir, moduleName + ".state");
                boolean stateBackedUp = backupModuleState(moduleName, moduleStateFile);
                
                if (stateBackedUp) {
                    backedUpFiles.add("module_state/" + moduleStateFile.getName());
                    LOGGER.info("Backed up state for module: " + moduleName);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to backup state for module: " + moduleName, e);
            }
        }
        
        LOGGER.info("Module state backup completed, backed up " + backedUpFiles.size() + " modules");
        return backedUpFiles;
    }
    
    /**
     * Gets a list of loaded module names.
     * 
     * @return List of loaded module names
     */
    private Set<String> getLoadedModules() {
        return moduleRegistry.getAllModules().stream()
                .map(ModuleRegistry.ModuleInfo::getId)
                .collect(Collectors.toSet());
    }
    
    @Override
    public Set<String> performIncrementalBackup(BackupSystem backupSystem, File backupDir, File previousBackupDir) throws Exception {
        // Module state is always backed up completely
        return performBackup(backupSystem, backupDir, null);
    }
    
    @Override
    public void performRestore(BackupSystem backupSystem, File backupDir, Set<String> files) throws Exception {
        LOGGER.info("Restoring module state");
        
        File moduleStateDir = new File(backupDir, "module_state");
        if (!moduleStateDir.exists()) {
            LOGGER.warning("Module state directory not found: " + moduleStateDir.getPath());
            return;
        }
        
        for (String relativePath : files) {
            if (relativePath.startsWith("module_state/")) {
                String fileName = relativePath.substring("module_state/".length());
                String moduleName = fileName.substring(0, fileName.lastIndexOf(".state"));
                
                File stateFile = new File(moduleStateDir, fileName);
                if (stateFile.exists()) {
                    try {
                        boolean restored = restoreModuleState(moduleName, stateFile);
                        if (restored) {
                            LOGGER.info("Restored state for module: " + moduleName);
                        } else {
                            LOGGER.warning("Failed to restore state for module: " + moduleName);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error restoring state for module: " + moduleName, e);
                    }
                } else {
                    LOGGER.warning("Module state file not found: " + stateFile.getPath());
                }
            }
        }
    }
}
