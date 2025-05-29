package com.essentialscore.api.web.rest.endpoints;

import com.essentialscore.ModuleManager;
import com.essentialscore.ModulePerformanceData;
import com.essentialscore.api.Module;
import com.essentialscore.api.web.rest.ApiEndpoint;
import com.essentialscore.api.web.rest.ApiRequest;
import com.essentialscore.api.web.rest.ApiResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API endpoint for module management operations.
 */
public class ModuleManagerEndpoint extends ApiEndpoint {
    
    private final ModuleManager moduleManager;
    private final Map<String, Long> moduleActionTimestamps;
    
    /**
     * Creates a new module manager endpoint
     * 
     * @param plugin The plugin instance
     * @param moduleManager The module manager
     */
    public ModuleManagerEndpoint(Plugin plugin, ModuleManager moduleManager) {
        super(plugin);
        this.moduleManager = moduleManager;
        this.moduleActionTimestamps = new ConcurrentHashMap<>();
    }
    
    @Override
    public String getPath() {
        return "modules";
    }
    
    @Override
    public boolean requiresAuthentication() {
        return true;
    }
    
    @Override
    public String getRequiredPermission() {
        return "essentials.webui.modules";
    }
    
    @Override
    public ApiResponse handleRequest(ApiRequest request) {
        try {
            String method = request.getMethod();
            String subpath = request.getPathSegment(1);
            
            if (subpath == null) {
                // Base modules endpoint
                switch (method) {
                    case "GET":
                        return getAllModules();
                    default:
                        return ApiResponse.methodNotAllowed("Method not allowed: " + method);
                }
            }
            
            switch (subpath) {
                case "list":
                    return getAllModules();
                case "enabled":
                    return getEnabledModules();
                case "disabled":
                    return getDisabledModules();
                case "performance":
                    return getModulePerformance();
                case "dependencies":
                    return getModuleDependencies();
                case "reload":
                    return reloadModules();
                default:
                    // Specific module operations
                    return handleModuleOperation(subpath, request);
            }
        } catch (Exception e) {
            return ApiResponse.error("Error processing module request: " + e.getMessage());
        }
    }
    
    /**
     * Handles operations on specific modules
     */
    private ApiResponse handleModuleOperation(String moduleName, ApiRequest request) {
        String action = request.getPathSegment(2);
        
        if (action == null) {
            // Get module info
            return getModuleInfo(moduleName);
        }
        
        switch (action) {
            case "enable":
                return enableModule(moduleName);
            case "disable":
                return disableModule(moduleName);
            case "reload":
                return reloadModule(moduleName);
            case "config":
                return getModuleConfig(moduleName);
            case "performance":
                return getModulePerformanceData(moduleName);
            case "dependencies":
                return getModuleDependencies(moduleName);
            default:
                return ApiResponse.notFound("Unknown module action: " + action);
        }
    }
    
    /**
     * Gets all modules with their status
     */
    private ApiResponse getAllModules() {
        List<Map<String, Object>> modules = new ArrayList<>();
        
        Set<String> allModuleNames = moduleManager.getAllModuleNames();
        for (String moduleName : allModuleNames) {
            modules.add(createModuleInfo(moduleName));
        }
        
        // Sort by name
        modules.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("modules", modules);
        response.put("count", modules.size());
        response.put("enabled", modules.stream().mapToLong(m -> (Boolean) m.get("enabled") ? 1 : 0).sum());
        response.put("disabled", modules.stream().mapToLong(m -> (Boolean) m.get("enabled") ? 0 : 1).sum());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets only enabled modules
     */
    private ApiResponse getEnabledModules() {
        List<Map<String, Object>> modules = new ArrayList<>();
        
        Set<String> enabledModules = moduleManager.getEnabledModules();
        for (String moduleName : enabledModules) {
            modules.add(createModuleInfo(moduleName));
        }
        
        modules.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("modules", modules);
        response.put("count", modules.size());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets only disabled modules
     */
    private ApiResponse getDisabledModules() {
        List<Map<String, Object>> modules = new ArrayList<>();
        
        Set<String> allModules = moduleManager.getAllModuleNames();
        Set<String> enabledModules = moduleManager.getEnabledModules();
        
        for (String moduleName : allModules) {
            if (!enabledModules.contains(moduleName)) {
                modules.add(createModuleInfo(moduleName));
            }
        }
        
        modules.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("modules", modules);
        response.put("count", modules.size());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets performance data for all modules
     */
    private ApiResponse getModulePerformance() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> performanceData = new ArrayList<>();
        
        Set<String> enabledModules = moduleManager.getEnabledModules();
        for (String moduleName : enabledModules) {
            ModulePerformanceData perfData = moduleManager.getPerformanceData(moduleName);
            if (perfData != null) {
                performanceData.add(createPerformanceInfo(moduleName, perfData));
            }
        }
        
        // Sort by average execution time (descending)
        performanceData.sort((a, b) -> 
            Double.compare((Double) b.get("averageExecutionTime"), (Double) a.get("averageExecutionTime")));
        
        response.put("performance", performanceData);
        response.put("count", performanceData.size());
        response.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets module dependencies for all modules
     */
    private ApiResponse getModuleDependencies() {
        Map<String, Object> response = new HashMap<>();
        Map<String, List<String>> dependencies = new HashMap<>();
        
        Set<String> allModules = moduleManager.getAllModuleNames();
        for (String moduleName : allModules) {
            Module module = moduleManager.getModule(moduleName);
            if (module != null) {
                List<String> deps = moduleManager.getDependencies(moduleName);
                if (deps != null && !deps.isEmpty()) {
                    dependencies.put(moduleName, deps);
                }
            }
        }
        
        response.put("dependencies", dependencies);
        response.put("count", dependencies.size());
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets detailed information about a specific module
     */
    private ApiResponse getModuleInfo(String moduleName) {
        Module module = moduleManager.getModule(moduleName);
        if (module == null) {
            return ApiResponse.notFound("Module not found: " + moduleName);
        }
        
        Map<String, Object> info = createDetailedModuleInfo(moduleName, module);
        return ApiResponse.ok(info);
    }
    
    /**
     * Enables a specific module
     */
    private ApiResponse enableModule(String moduleName) {
        try {
            if (moduleManager.isModuleEnabled(moduleName)) {
                return ApiResponse.badRequest("Module is already enabled: " + moduleName);
            }
            
            boolean success = moduleManager.enableModule(moduleName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("module", moduleName);
            response.put("message", success ? 
                "Module enabled successfully" : 
                "Failed to enable module");
            response.put("timestamp", System.currentTimeMillis());
            
            if (success) {
                moduleActionTimestamps.put(moduleName + "_enabled", System.currentTimeMillis());
            }
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error enabling module: " + e.getMessage());
        }
    }
    
    /**
     * Disables a specific module
     */
    private ApiResponse disableModule(String moduleName) {
        try {
            if (!moduleManager.isModuleEnabled(moduleName)) {
                return ApiResponse.badRequest("Module is already disabled: " + moduleName);
            }
            
            boolean success = moduleManager.disableModule(moduleName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("module", moduleName);
            response.put("message", success ? 
                "Module disabled successfully" : 
                "Failed to disable module");
            response.put("timestamp", System.currentTimeMillis());
            
            if (success) {
                moduleActionTimestamps.put(moduleName + "_disabled", System.currentTimeMillis());
            }
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error disabling module: " + e.getMessage());
        }
    }
    
    /**
     * Reloads a specific module
     */
    private ApiResponse reloadModule(String moduleName) {
        try {
            boolean wasEnabled = moduleManager.isModuleEnabled(moduleName);
            boolean success = moduleManager.reloadModule(moduleName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("module", moduleName);
            response.put("wasEnabled", wasEnabled);
            response.put("message", success ? 
                "Module reloaded successfully" : 
                "Failed to reload module");
            response.put("timestamp", System.currentTimeMillis());
            
            if (success) {
                moduleActionTimestamps.put(moduleName + "_reloaded", System.currentTimeMillis());
            }
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error reloading module: " + e.getMessage());
        }
    }
    
    /**
     * Reloads all modules
     */
    private ApiResponse reloadModules() {
        try {
            int successCount = 0;
            int totalCount = 0;
            List<String> failures = new ArrayList<>();
            
            Set<String> enabledModules = new HashSet<>(moduleManager.getEnabledModules());
            for (String moduleName : enabledModules) {
                totalCount++;
                try {
                    if (moduleManager.reloadModule(moduleName)) {
                        successCount++;
                    } else {
                        failures.add(moduleName);
                    }
                } catch (Exception e) {
                    failures.add(moduleName + " (" + e.getMessage() + ")");
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", failures.isEmpty());
            response.put("successCount", successCount);
            response.put("totalCount", totalCount);
            response.put("failures", failures);
            response.put("message", String.format("Reloaded %d/%d modules successfully", 
                successCount, totalCount));
            response.put("timestamp", System.currentTimeMillis());
            
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("Error reloading modules: " + e.getMessage());
        }
    }
    
    /**
     * Gets module configuration
     */
    private ApiResponse getModuleConfig(String moduleName) {
        Module module = moduleManager.getModule(moduleName);
        if (module == null) {
            return ApiResponse.notFound("Module not found: " + moduleName);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("module", moduleName);
        response.put("config", "Configuration not available in simplified implementation");
        response.put("message", "Module configuration access requires extended implementation");
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets performance data for a specific module
     */
    private ApiResponse getModulePerformanceData(String moduleName) {
        ModulePerformanceData perfData = moduleManager.getPerformanceData(moduleName);
        if (perfData == null) {
            return ApiResponse.notFound("Performance data not found for module: " + moduleName);
        }
        
        Map<String, Object> response = createPerformanceInfo(moduleName, perfData);
        return ApiResponse.ok(response);
    }
    
    /**
     * Gets dependencies for a specific module
     */
    private ApiResponse getModuleDependencies(String moduleName) {
        Module module = moduleManager.getModule(moduleName);
        if (module == null) {
            return ApiResponse.notFound("Module not found: " + moduleName);
        }
        
        List<String> dependencies = moduleManager.getDependencies(moduleName);
        List<String> dependents = moduleManager.getDependents(moduleName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("module", moduleName);
        response.put("dependencies", dependencies != null ? dependencies : new ArrayList<>());
        response.put("dependents", dependents != null ? dependents : new ArrayList<>());
        response.put("dependencyCount", dependencies != null ? dependencies.size() : 0);
        response.put("dependentCount", dependents != null ? dependents.size() : 0);
        
        return ApiResponse.ok(response);
    }
    
    /**
     * Creates basic module information
     */
    private Map<String, Object> createModuleInfo(String moduleName) {
        Map<String, Object> info = new HashMap<>();
        Module module = moduleManager.getModule(moduleName);
        boolean enabled = moduleManager.isModuleEnabled(moduleName);
        
        info.put("name", moduleName);
        info.put("enabled", enabled);
        info.put("loaded", module != null);
        
        if (module != null) {
            info.put("version", module.getVersion());
            info.put("description", module.getDescription());
            info.put("author", module.getAuthor());
        }
        
        // Add action timestamps
        Long enabledTime = moduleActionTimestamps.get(moduleName + "_enabled");
        Long disabledTime = moduleActionTimestamps.get(moduleName + "_disabled");
        Long reloadedTime = moduleActionTimestamps.get(moduleName + "_reloaded");
        
        if (enabledTime != null) {
            info.put("lastEnabled", enabledTime);
        }
        if (disabledTime != null) {
            info.put("lastDisabled", disabledTime);
        }
        if (reloadedTime != null) {
            info.put("lastReloaded", reloadedTime);
        }
        
        return info;
    }
    
    /**
     * Creates detailed module information
     */
    private Map<String, Object> createDetailedModuleInfo(String moduleName, Module module) {
        Map<String, Object> info = createModuleInfo(moduleName);
        
        // Add detailed information
        List<String> dependencies = moduleManager.getDependencies(moduleName);
        List<String> dependents = moduleManager.getDependents(moduleName);
        ModulePerformanceData perfData = moduleManager.getPerformanceData(moduleName);
        
        info.put("dependencies", dependencies != null ? dependencies : new ArrayList<>());
        info.put("dependents", dependents != null ? dependents : new ArrayList<>());
        info.put("dependencyCount", dependencies != null ? dependencies.size() : 0);
        info.put("dependentCount", dependents != null ? dependents.size() : 0);
        
        if (perfData != null) {
            info.put("performance", createPerformanceInfo(moduleName, perfData));
        }
        
        return info;
    }
    
    /**
     * Creates performance information
     */
    private Map<String, Object> createPerformanceInfo(String moduleName, ModulePerformanceData perfData) {
        Map<String, Object> info = new HashMap<>();
        info.put("module", moduleName);
        info.put("executionCount", perfData.getExecutionCount());
        info.put("totalExecutionTime", perfData.getTotalExecutionTime());
        info.put("averageExecutionTime", perfData.getAverageExecutionTime());
        info.put("minExecutionTime", perfData.getMinExecutionTime());
        info.put("maxExecutionTime", perfData.getMaxExecutionTime());
        info.put("memoryUsage", perfData.getMemoryUsage());
        info.put("lastExecution", perfData.getLastExecutionTime());
        
        return info;
    }
}
