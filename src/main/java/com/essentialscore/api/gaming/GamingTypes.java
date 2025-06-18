package com.essentialscore.api.gaming;

import java.util.*;
import java.time.Instant;

/**
 * Additional gaming types that are not duplicated elsewhere
 */

class SeasonalEvent {
    private final String name;
    private final Instant startTime;
    private final Instant endTime;
    private final Map<String, Object> rewards;
    
    public SeasonalEvent(String name, Instant startTime, Instant endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rewards = new HashMap<>();
    }
    
    public String getName() { return name; }
    public boolean isActive() {
        Instant now = Instant.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }
    public Map<String, Object> getRewards() { return rewards; }
}

class Calendar {
    private final Map<String, List<String>> events;
    
    public Calendar() {
        this.events = new HashMap<>();
    }
    
    public List<String> getEventsForDate(String date) {
        return events.getOrDefault(date, new ArrayList<>());
    }
}
