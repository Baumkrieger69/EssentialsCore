package com.essentialscore.api.security;

import java.util.*;

/**
 * Role manager for RBAC (Role-Based Access Control).
 */
public class RoleManager {
    private final Map<UUID, Set<Role>> userRoles;
    private final Map<String, Role> availableRoles;
    
    public RoleManager() {
        this.userRoles = new HashMap<>();
        this.availableRoles = new HashMap<>();
    }
    
    public void loadRoles() {
        // Load roles from configuration or database
        // This is a stub implementation
    }
    
    public Set<Role> getRoles(UUID userId) {
        return userRoles.getOrDefault(userId, new HashSet<>());
    }
    
    public void assignRole(UUID userId, Role role) {
        userRoles.computeIfAbsent(userId, k -> new HashSet<>()).add(role);
    }
    
    public void removeRole(UUID userId, Role role) {
        Set<Role> roles = userRoles.get(userId);
        if (roles != null) {
            roles.remove(role);
        }
    }
    
    public Role getRole(String roleName) {
        return availableRoles.get(roleName);
    }
    
    public void addRole(Role role) {
        availableRoles.put(role.getName(), role);
    }
}
