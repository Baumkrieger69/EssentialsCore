package com.essentialscore.api.versioning;

import org.bukkit.Bukkit;

/**
 * Utility class for checking the server version.
 */
public class ServerVersion {
    private static final String VERSION_PATTERN = "\\(MC: (\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-[\\w.]+)?\\)";
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    private static final int PATCH_VERSION;
    
    static {
        String bukkitVersion = Bukkit.getBukkitVersion();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(VERSION_PATTERN);
        java.util.regex.Matcher matcher = pattern.matcher(Bukkit.getVersion());
        
        if (matcher.find()) {
            MAJOR_VERSION = Integer.parseInt(matcher.group(1));
            MINOR_VERSION = Integer.parseInt(matcher.group(2));
            PATCH_VERSION = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        } else {
            // Fallback for older versions or unexpected formats
            String[] versionParts = bukkitVersion.split("-")[0].split("\\.");
            MAJOR_VERSION = versionParts.length > 0 ? Integer.parseInt(versionParts[0]) : 1;
            MINOR_VERSION = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : 0;
            PATCH_VERSION = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;
        }
    }
    
    /**
     * Gets the major version (e.g. 1 in 1.16.5).
     *
     * @return The major version
     */
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }
    
    /**
     * Gets the minor version (e.g. 16 in 1.16.5).
     *
     * @return The minor version
     */
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }
    
    /**
     * Gets the patch version (e.g. 5 in 1.16.5).
     *
     * @return The patch version
     */
    public static int getPatchVersion() {
        return PATCH_VERSION;
    }
    
    /**
     * Checks if the server version is at least the specified version.
     *
     * @param major The major version
     * @param minor The minor version
     * @return true if the server version is at least the specified version
     */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }
    
    /**
     * Checks if the server version is at least the specified version.
     *
     * @param major The major version
     * @param minor The minor version
     * @param patch The patch version
     * @return true if the server version is at least the specified version
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (MAJOR_VERSION > major) {
            return true;
        }
        if (MAJOR_VERSION < major) {
            return false;
        }
        if (MINOR_VERSION > minor) {
            return true;
        }
        if (MINOR_VERSION < minor) {
            return false;
        }
        return PATCH_VERSION >= patch;
    }
    
    /**
     * Checks if the server version is below the specified version.
     *
     * @param major The major version
     * @param minor The minor version
     * @return true if the server version is below the specified version
     */
    public static boolean isBelow(int major, int minor) {
        return isBelow(major, minor, 0);
    }
    
    /**
     * Checks if the server version is below the specified version.
     *
     * @param major The major version
     * @param minor The minor version
     * @param patch The patch version
     * @return true if the server version is below the specified version
     */
    public static boolean isBelow(int major, int minor, int patch) {
        return !isAtLeast(major, minor, patch);
    }
    
    /**
     * Gets the server version as a string.
     *
     * @return The server version
     */
    public static String getVersion() {
        return MAJOR_VERSION + "." + MINOR_VERSION + "." + PATCH_VERSION;
    }
} 
