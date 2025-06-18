package com.essentialscore.util;

/**
 * Semantic version implementation for dependency management
 */
public class SemanticVersion implements Comparable<SemanticVersion> {
    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    
    public SemanticVersion(int major, int minor, int patch) {
        this(major, minor, patch, null);
    }
    
    public SemanticVersion(int major, int minor, int patch, String preRelease) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
    }
    
    public static SemanticVersion parse(String version) {
        // Simple parsing implementation
        String[] parts = version.split("\\.");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }
        
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        
        String patchPart = parts[2];
        int dashIndex = patchPart.indexOf('-');
        
        int patch;
        String preRelease = null;
        
        if (dashIndex >= 0) {
            patch = Integer.parseInt(patchPart.substring(0, dashIndex));
            preRelease = patchPart.substring(dashIndex + 1);
        } else {
            patch = Integer.parseInt(patchPart);
        }
        
        return new SemanticVersion(major, minor, patch, preRelease);
    }
    
    @Override
    public int compareTo(SemanticVersion other) {
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;
        
        result = Integer.compare(this.minor, other.minor);
        if (result != 0) return result;
        
        return Integer.compare(this.patch, other.patch);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append(".").append(minor).append(".").append(patch);
        if (preRelease != null) {
            sb.append("-").append(preRelease);
        }
        return sb.toString();
    }
    
    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }
    public String getPreRelease() { return preRelease; }
}
