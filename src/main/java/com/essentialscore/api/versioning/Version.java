package com.essentialscore.api.versioning;

/**
 * Represents a semantic version following the SemVer 2.0.0 specification.
 * Format: MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
 */
public class Version implements Comparable<Version> {
    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String buildMetadata;

    /**
     * Creates a new Version object from a version string.
     *
     * @param version The version string in format MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
     * @throws IllegalArgumentException if the version string is invalid
     */
    public Version(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        // Split build metadata
        String[] versionAndBuild = version.split("\\+", 2);
        String versionPart = versionAndBuild[0];
        buildMetadata = versionAndBuild.length > 1 ? versionAndBuild[1] : "";

        // Split pre-release
        String[] versionAndPreRelease = versionPart.split("-", 2);
        String numericPart = versionAndPreRelease[0];
        preRelease = versionAndPreRelease.length > 1 ? versionAndPreRelease[1] : "";

        // Parse numeric parts
        String[] parts = numericPart.split("\\.");
        if (parts.length < 1 || parts.length > 3) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        try {
            major = Integer.parseInt(parts[0]);
            minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version number: " + version, e);
        }

        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components cannot be negative");
        }
    }

    /**
     * Creates a new Version with specific components.
     *
     * @param major Major version number
     * @param minor Minor version number
     * @param patch Patch version number
     * @param preRelease Pre-release identifier
     * @param buildMetadata Build metadata
     */
    public Version(int major, int minor, int patch, String preRelease, String buildMetadata) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components cannot be negative");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease != null ? preRelease : "";
        this.buildMetadata = buildMetadata != null ? buildMetadata : "";
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getPreRelease() {
        return preRelease;
    }

    public String getBuildMetadata() {
        return buildMetadata;
    }

    /**
     * Checks if this version is a pre-release version.
     *
     * @return true if this is a pre-release version
     */
    public boolean isPreRelease() {
        return !preRelease.isEmpty();
    }

    @Override
    public int compareTo(Version other) {
        // Compare major version
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;

        // Compare minor version
        result = Integer.compare(this.minor, other.minor);
        if (result != 0) return result;

        // Compare patch version
        result = Integer.compare(this.patch, other.patch);
        if (result != 0) return result;

        // Both are not pre-releases, they are equal
        if (this.preRelease.isEmpty() && other.preRelease.isEmpty()) {
            return 0;
        }

        // Pre-release versions have lower precedence
        if (this.preRelease.isEmpty()) return 1;
        if (other.preRelease.isEmpty()) return -1;

        // Compare pre-release identifiers
        String[] thisParts = this.preRelease.split("\\.");
        String[] otherParts = other.preRelease.split("\\.");

        int minLength = Math.min(thisParts.length, otherParts.length);
        for (int i = 0; i < minLength; i++) {
            boolean thisIsNumeric = thisParts[i].matches("\\d+");
            boolean otherIsNumeric = otherParts[i].matches("\\d+");

            // Numeric identifiers have lower precedence than non-numeric
            if (thisIsNumeric && !otherIsNumeric) {
                return -1;
            }
            if (!thisIsNumeric && otherIsNumeric) {
                return 1;
            }

            if (thisIsNumeric) {
                // Compare numeric identifiers numerically
                int thisNum = Integer.parseInt(thisParts[i]);
                int otherNum = Integer.parseInt(otherParts[i]);
                result = Integer.compare(thisNum, otherNum);
            } else {
                // Compare non-numeric identifiers lexically
                result = thisParts[i].compareTo(otherParts[i]);
            }

            if (result != 0) {
                return result;
            }
        }

        // If all identifiers match, the one with more identifiers has higher precedence
        return Integer.compare(thisParts.length, otherParts.length);
    }

    /**
     * Checks if this version is compatible with another version according to semver rules.
     * For MAJOR version 0, any change may be breaking, so only exact matches are compatible.
     * For MAJOR version > 0, MINOR and PATCH updates are compatible.
     *
     * @param other The version to check compatibility with
     * @return true if this version is compatible with the other version
     */
    public boolean isCompatibleWith(Version other) {
        if (this.major == 0 || other.major == 0) {
            // For 0.x.y, only exact matches are compatible
            return this.equals(other);
        } else {
            // For x.y.z where x > 0, minor and patch updates are compatible
            return this.major == other.major;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Version version = (Version) o;
        
        if (major != version.major) return false;
        if (minor != version.minor) return false;
        if (patch != version.patch) return false;
        return preRelease.equals(version.preRelease);
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        result = 31 * result + preRelease.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append(major)
                .append('.')
                .append(minor)
                .append('.')
                .append(patch);
        
        if (!preRelease.isEmpty()) {
            sb.append('-').append(preRelease);
        }
        
        if (!buildMetadata.isEmpty()) {
            sb.append('+').append(buildMetadata);
        }
        
        return sb.toString();
    }
} 
