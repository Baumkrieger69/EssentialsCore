package com.essentialscore.api.security;

/**
 * Interface for security policies that support strict mode.
 */
public interface StrictModePolicy {
    /**
     * Sets strict mode for the policy.
     *
     * @param strict true to enable strict mode, false to disable
     */
    void setStrictMode(boolean strict);
    
    /**
     * Checks if strict mode is enabled.
     *
     * @return true if strict mode is enabled
     */
    boolean isStrictMode();
}
