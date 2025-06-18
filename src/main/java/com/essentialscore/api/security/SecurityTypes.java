package com.essentialscore.api.security;

import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Security types and enums that don't conflict with standalone classes
 */

enum SecurityLevel {
    LOW(1, false),
    NORMAL(2, false),
    HIGH(3, true),
    CRITICAL(4, true);
    
    private final int level;
    private final boolean requiresAdditionalAuth;
    
    SecurityLevel(int level, boolean requiresAdditionalAuth) {
        this.level = level;
        this.requiresAdditionalAuth = requiresAdditionalAuth;
    }
    
    public int getLevel() { return level; }
    public boolean requiresAdditionalAuth() { return requiresAdditionalAuth; }
}

class TwoFactorConfig {
    private final UUID userId;
    private final String secret;
    private final boolean enabled;
    private final Duration timeout;
    private final int maxAttempts;
    private final String algorithm;
    private final Instant createdAt;
    
    public TwoFactorConfig(boolean enabled, Duration timeout, int maxAttempts, String algorithm) {
        this.userId = null;
        this.secret = null;
        this.enabled = enabled;
        this.timeout = timeout;
        this.maxAttempts = maxAttempts;
        this.algorithm = algorithm;
        this.createdAt = Instant.now();
    }
    
    public UUID getUserId() { return userId; }
    public String getSecret() { return secret; }
    public boolean isEnabled() { return enabled; }
    public Duration getTimeout() { return timeout; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getAlgorithm() { return algorithm; }
    public Instant getCreatedAt() { return createdAt; }
}

class TOTPGenerator {
    public String generateSecret() {
        return UUID.randomUUID().toString().substring(0, 16);
    }
    
    public String generateCode(String secret) {
        return String.valueOf(System.currentTimeMillis() % 1000000);
    }
    
    public boolean validateTOTP(String secret, String code) {
        return code != null && code.length() == 6;
    }
}

class ComplianceRule {
    private final String id;
    private final String name;
    private final String description;
    private final boolean required;
    private final boolean mandatory;
    
    public ComplianceRule(String id, String description, boolean required) {
        this.id = id;
        this.name = id;
        this.description = description;
        this.required = required;
        this.mandatory = required;
    }
    
    public ComplianceRule(String name, String description, boolean mandatory, boolean isNameConstructor) {
        this.id = name;
        this.name = name;
        this.description = description;
        this.required = mandatory;
        this.mandatory = mandatory;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
    public boolean isMandatory() { return mandatory; }
}

class UserDataConsent {
    private final UUID userId;
    private final Map<String, Boolean> consents;
    private final boolean dataProcessingConsent;
    private final boolean marketingConsent;
    private final Instant timestamp;
    private final Instant consentGiven;
    
    public UserDataConsent(UUID userId) {
        this.userId = userId;
        this.consents = new HashMap<>();
        this.dataProcessingConsent = false;
        this.marketingConsent = false;
        this.timestamp = Instant.now();
        this.consentGiven = Instant.now();
    }
    
    public UserDataConsent(UUID userId, boolean dataProcessing, boolean marketing) {
        this.userId = userId;
        this.consents = new HashMap<>();
        this.dataProcessingConsent = dataProcessing;
        this.marketingConsent = marketing;
        this.timestamp = Instant.now();
        this.consentGiven = Instant.now();
    }
    
    public boolean hasDataProcessingConsent() { return dataProcessingConsent; }
    public boolean hasMarketingConsent() { return marketingConsent; }
    public UUID getUserId() { return userId; }
    public Instant getConsentGiven() { return consentGiven; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Boolean> getConsents() { return consents; }
}

class DataRetentionPolicy {
    private final Duration retentionPeriod;
    private final boolean autoDelete;
    
    public DataRetentionPolicy(Duration retentionPeriod, boolean autoDelete) {
        this.retentionPeriod = retentionPeriod;
        this.autoDelete = autoDelete;
    }
    
    public Duration getRetentionPeriod() { return retentionPeriod; }
    public boolean isAutoDelete() { return autoDelete; }
    public boolean isAutoDeleteEnabled() { return autoDelete; }
    public boolean isAutoDeletionEnabled() { return autoDelete; }
}

class ComplianceViolation {
    private final String ruleId;
    private final String description;
    private final Instant timestamp;
    private final String severity;
    private final boolean autoCorrectable;
    
    public ComplianceViolation(String ruleId, String description, String severity) {
        this.ruleId = ruleId;
        this.description = description;
        this.severity = severity;
        this.autoCorrectable = false;
        this.timestamp = Instant.now();
    }
    
    public ComplianceViolation(String ruleId, String description, boolean autoCorrectable) {
        this.ruleId = ruleId;
        this.description = description;
        this.autoCorrectable = autoCorrectable;
        this.severity = null;
        this.timestamp = Instant.now();
    }
    
    public String getRuleId() { return ruleId; }
    public String getDescription() { return description; }
    public boolean isAutoCorrectable() { return autoCorrectable; }
    public Instant getTimestamp() { return timestamp; }
    public String getSeverity() { return severity; }
}

// Role class is defined in Role.java - removed duplicate definition

class WebUISecurityPolicy {
    private final String name;
    private final Map<String, Object> settings;
    private final boolean enforceIpWhitelist;
    private final boolean requireTwoFactor;
    private final Duration sessionTimeout;
    
    public WebUISecurityPolicy() {
        this("default", new HashMap<>(), false, false, Duration.ofMinutes(30));
    }
    
    public WebUISecurityPolicy(String name, Map<String, Object> settings, 
                         boolean enforceIpWhitelist, boolean requireTwoFactor, 
                         Duration sessionTimeout) {
        this.name = name;
        this.settings = new HashMap<>(settings);
        this.enforceIpWhitelist = enforceIpWhitelist;
        this.requireTwoFactor = requireTwoFactor;
        this.sessionTimeout = sessionTimeout;
    }
    
    public String getName() { return name; }
    public Map<String, Object> getSettings() { return settings; }
    public boolean isEnforceIpWhitelist() { return enforceIpWhitelist; }
    public boolean isRequireTwoFactor() { return requireTwoFactor; }
    public Duration getSessionTimeout() { return sessionTimeout; }
}
