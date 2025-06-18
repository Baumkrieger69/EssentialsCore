package com.essentialscore.api.scheduling;

/**
 * Simple duration class for scheduling
 */
public class Duration {
    private final long milliseconds;
    
    private Duration(long milliseconds) {
        this.milliseconds = milliseconds;
    }
    
    public static Duration ofMinutes(long minutes) {
        return new Duration(minutes * 60 * 1000);
    }
    
    public static Duration ofSeconds(long seconds) {
        return new Duration(seconds * 1000);
    }
    
    public static Duration ofMilliseconds(long milliseconds) {
        return new Duration(milliseconds);
    }
    
    public long toMillis() {
        return milliseconds;
    }
    
    public long toSeconds() {
        return milliseconds / 1000;
    }
    
    @Override
    public String toString() {
        return milliseconds + "ms";
    }
}
