package com.essentialscore.api.versioning;

/**
 * Base interface for version-specific adapters.
 * All version adapters should extend this interface.
 */
public interface VersionAdapter {
    /**
     * Gets the minimum supported version for this adapter.
     *
     * @return The minimum supported version in format "1.x"
     */
    String getMinimumSupportedVersion();
    
    /**
     * Gets the maximum supported version for this adapter.
     *
     * @return The maximum supported version in format "1.x", or "latest" if no maximum
     */
    String getMaximumSupportedVersion();
    
    /**
     * Checks if this adapter supports the current server version.
     *
     * @return true if this adapter supports the current server version
     */
    default boolean supportsCurrentVersion() {
        String minVersion = getMinimumSupportedVersion();
        String maxVersion = getMaximumSupportedVersion();
        
        // Parse versions
        String[] minParts = minVersion.split("\\.");
        int minMajor = Integer.parseInt(minParts[0]);
        int minMinor = Integer.parseInt(minParts[1]);
        
        // Check minimum version
        if (!ServerVersion.isAtLeast(minMajor, minMinor)) {
            return false;
        }
        
        // Check maximum version if not "latest"
        if (!"latest".equals(maxVersion)) {
            String[] maxParts = maxVersion.split("\\.");
            int maxMajor = Integer.parseInt(maxParts[0]);
            int maxMinor = Integer.parseInt(maxParts[1]);
            
            if (ServerVersion.getMajorVersion() > maxMajor) {
                return false;
            }
            
            if (ServerVersion.getMajorVersion() == maxMajor && 
                ServerVersion.getMinorVersion() > maxMinor) {
                return false;
            }
        }
        
        return true;
    }
} 
