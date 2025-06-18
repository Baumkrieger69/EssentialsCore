package com.essentialscore.api.versioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves module dependencies, detects conflicts, and determines optimal load order.
 */
public class DependencyResolver {
    
    /**
     * Represents a module with its version and dependencies
     */
    public static class ModuleInfo {
        private final String name;
        private final Version version;
        private final Map<String, VersionRange> dependencies;
        private final Map<String, VersionRange> optionalDependencies;
        
        public ModuleInfo(String name, String version, 
                         Map<String, String> dependencies, 
                         Map<String, String> optionalDependencies) {
            this.name = name;
            this.version = new Version(version);
            
            this.dependencies = new HashMap<>();
            if (dependencies != null) {
                for (Map.Entry<String, String> entry : dependencies.entrySet()) {
                    this.dependencies.put(entry.getKey(), new VersionRange(entry.getValue()));
                }
            }
            
            this.optionalDependencies = new HashMap<>();
            if (optionalDependencies != null) {
                for (Map.Entry<String, String> entry : optionalDependencies.entrySet()) {
                    this.optionalDependencies.put(entry.getKey(), new VersionRange(entry.getValue()));
                }
            }
        }
        
        public String getName() {
            return name;
        }
        
        public Version getVersion() {
            return version;
        }
        
        public Map<String, VersionRange> getDependencies() {
            return Collections.unmodifiableMap(dependencies);
        }
        
        public Map<String, VersionRange> getOptionalDependencies() {
            return Collections.unmodifiableMap(optionalDependencies);
        }
        
        @Override
        public String toString() {
            return name + "@" + version;
        }
    }
    
    /**
     * Result of dependency resolution.
     */
    public static class ResolutionResult {
        private final List<ModuleInfo> loadOrder;
        private final List<String> missingDependencies;
        private final List<String> circularDependencies;
        private final Map<String, List<String>> versionConflicts;
        
        public ResolutionResult(List<ModuleInfo> loadOrder, 
                              List<String> missingDependencies,
                              List<String> circularDependencies,
                              Map<String, List<String>> versionConflicts) {
            this.loadOrder = loadOrder;
            this.missingDependencies = missingDependencies;
            this.circularDependencies = circularDependencies;
            this.versionConflicts = versionConflicts;
        }
        
        public List<ModuleInfo> getLoadOrder() {
            return loadOrder;
        }
        
        public List<String> getMissingDependencies() {
            return missingDependencies;
        }
        
        public List<String> getCircularDependencies() {
            return circularDependencies;
        }
        
        public Map<String, List<String>> getVersionConflicts() {
            return versionConflicts;
        }
        
        public boolean hasErrors() {
            return !missingDependencies.isEmpty() || 
                   !circularDependencies.isEmpty() || 
                   !versionConflicts.isEmpty();
        }
    }
    
    /**
     * Resolves dependencies and determines the optimal load order.
     * 
     * @param modules List of modules to resolve
     * @return Resolution result containing load order and any errors
     */
    public ResolutionResult resolve(List<ModuleInfo> modules) {
        // Create maps for quick lookup
        Map<String, ModuleInfo> moduleMap = new HashMap<>();
        for (ModuleInfo module : modules) {
            moduleMap.put(module.getName(), module);
        }
        
        // Check for missing dependencies
        List<String> missingDependencies = new ArrayList<>();
        for (ModuleInfo module : modules) {
            for (String dependency : module.getDependencies().keySet()) {
                if (!moduleMap.containsKey(dependency)) {
                    missingDependencies.add(module.getName() + " requires " + dependency + " but it was not found");
                }
            }
        }
        
        // Check for version conflicts
        Map<String, List<String>> versionConflicts = new HashMap<>();
        for (ModuleInfo module : modules) {
            for (Map.Entry<String, VersionRange> entry : module.getDependencies().entrySet()) {
                String depName = entry.getKey();
                VersionRange versionRange = entry.getValue();
                
                ModuleInfo depModule = moduleMap.get(depName);
                if (depModule != null && !versionRange.isSatisfiedBy(depModule.getVersion())) {
                    versionConflicts
                        .computeIfAbsent(depName, k -> new ArrayList<>())
                        .add(module.getName() + " requires " + depName + " " + versionRange + 
                             " but found " + depModule.getVersion());
                }
            }
        }
        
        // Build dependency graph and detect cycles
        Map<String, Set<String>> graph = new HashMap<>();
        for (ModuleInfo module : modules) {
            graph.put(module.getName(), new HashSet<>(module.getDependencies().keySet()));
        }
        
        List<String> circularDependencies = detectCycles(graph);
        
        // Sort modules by dependencies (topological sort)
        List<ModuleInfo> loadOrder = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (ModuleInfo module : modules) {
            visit(module.getName(), moduleMap, graph, visited, visiting, loadOrder);
        }
        
        // Reverse to get correct load order (dependencies first)
        Collections.reverse(loadOrder);
        
        return new ResolutionResult(loadOrder, missingDependencies, circularDependencies, versionConflicts);
    }
    
    /**
     * Detects circular dependencies in the dependency graph.
     * 
     * @param graph Dependency graph
     * @return List of circular dependencies
     */
    private List<String> detectCycles(Map<String, Set<String>> graph) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();
        
        for (String module : graph.keySet()) {
            detectCyclesRecursive(module, graph, visited, stack, new ArrayList<>(), result);
        }
        
        return result;
    }
    
    private void detectCyclesRecursive(String module, Map<String, Set<String>> graph, 
                                    Set<String> visited, Set<String> stack, 
                                    List<String> path, List<String> result) {
        if (stack.contains(module)) {
            // Found a cycle
            int cycleStart = path.indexOf(module);
            if (cycleStart >= 0) {
                List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                cycle.add(module);
                result.add(String.join(" -> ", cycle));
            }
            return;
        }
        
        if (visited.contains(module)) {
            return;
        }
        
        visited.add(module);
        stack.add(module);
        path.add(module);
        
        Set<String> dependencies = graph.getOrDefault(module, Collections.emptySet());
        for (String dependency : dependencies) {
            detectCyclesRecursive(dependency, graph, visited, stack, path, result);
        }
        
        stack.remove(module);
        path.remove(path.size() - 1);
    }
    
    /**
     * Performs topological sort to determine load order.
     */
    private void visit(String moduleName, Map<String, ModuleInfo> moduleMap, 
                     Map<String, Set<String>> graph, Set<String> visited, 
                     Set<String> visiting, List<ModuleInfo> result) {
        if (visited.contains(moduleName)) {
            return;
        }
        
        if (visiting.contains(moduleName)) {
            // Cyclic dependency - we'll handle these separately
            return;
        }
        
        visiting.add(moduleName);
        
        Set<String> dependencies = graph.getOrDefault(moduleName, Collections.emptySet());
        for (String dependency : dependencies) {
            if (moduleMap.containsKey(dependency)) {
                visit(dependency, moduleMap, graph, visited, visiting, result);
            }
        }
        
        visiting.remove(moduleName);
        visited.add(moduleName);
        
        ModuleInfo module = moduleMap.get(moduleName);
        if (module != null) {
            result.add(module);
        }
    }
    
    /**
     * Checks if a module can be downgraded to an older version.
     * 
     * @param currentModule The current module
     * @param newModule The module to potentially downgrade to
     * @return true if downgrade is allowed, false if it should be prevented
     */
    public boolean allowDowngrade(ModuleInfo currentModule, ModuleInfo newModule) {
        if (!currentModule.getName().equals(newModule.getName())) {
            throw new IllegalArgumentException("Cannot compare different modules");
        }
        
        // Don't allow downgrading to an older version
        if (newModule.getVersion().compareTo(currentModule.getVersion()) < 0) {
            // Check if it's a compatible version (same major version)
            return newModule.getVersion().isCompatibleWith(currentModule.getVersion());
        }
        
        // Not a downgrade
        return true;
    }
} 
