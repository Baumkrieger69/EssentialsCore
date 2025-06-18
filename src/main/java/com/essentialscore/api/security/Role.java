package com.essentialscore.api.security;

import java.util.*;

/**
 * Represents a security role with permissions.
 */
public class Role {
    private final String name;
    private final Set<String> permissions;
    private final int priority;
    
    public Role(String name, int priority) {
        this.name = name;
        this.priority = priority;
        this.permissions = new HashSet<>();
    }
    
    public String getName() {
        return name;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("*");
    }
    
    public void addPermission(String permission) {
        permissions.add(permission);
    }
    
    public void removePermission(String permission) {
        permissions.remove(permission);
    }
    
    public Set<String> getPermissions() {
        return new HashSet<>(permissions);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Role role = (Role) obj;
        return Objects.equals(name, role.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
