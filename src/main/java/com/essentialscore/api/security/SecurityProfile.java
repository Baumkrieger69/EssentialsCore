package com.essentialscore.api.security;

import java.util.*;

/**
 * Security profile for users containing metrics and audit information.
 */
public class SecurityProfile {
    private final UUID userId;
    private final Map<String, Object> securityMetrics;
    private boolean requiresAudit;
    
    public SecurityProfile(UUID userId) {
        this.userId = userId;
        this.securityMetrics = new HashMap<>();
        this.requiresAudit = false;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public boolean requiresAudit() {
        return requiresAudit;
    }
    
    public void setRequiresAudit(boolean requiresAudit) {
        this.requiresAudit = requiresAudit;
    }
    
    public Map<String, Object> getSecurityMetrics() {
        return new HashMap<>(securityMetrics);
    }
    
    public void addMetric(String key, Object value) {
        securityMetrics.put(key, value);
    }
    
    public Object getMetric(String key) {
        return securityMetrics.get(key);
    }
}
