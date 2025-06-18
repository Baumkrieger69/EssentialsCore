package com.essentialscore.api.scheduling;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

/**
 * Simple cron schedule implementation for task scheduling.
 */
public class CronSchedule {
    private final String cronExpression;
    
    // Simple cron pattern: minute hour day month weekday
    private static final Pattern CRON_VALIDATION = Pattern.compile(
        "^\\s*(\\*|[0-5]?\\d)(\\s+(\\*|[01]?\\d|2[0-3]))(\\s+(\\*|[12]?\\d|3[01]))(\\s+(\\*|[1-9]|1[0-2]))(\\s+(\\*|[0-6]))\\s*$"
    );
    
    /**
     * Creates a new cron schedule.
     *
     * @param cronExpression The cron expression (minute hour day month weekday)
     */
    public CronSchedule(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty");
        }
        
        this.cronExpression = cronExpression.trim();
        
        if (!CRON_VALIDATION.matcher(this.cronExpression).matches()) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
        }
    }
    /**
     * Gets the cron expression.
     *
     * @return The cron expression
     */
    public String getCronExpression() {
        return cronExpression;
    }
    
    /**
     * Checks if the schedule matches the given time.
     *
     * @param dateTime The date time to check
     * @return true if the schedule matches
     */
    public boolean matches(LocalDateTime dateTime) {
        String[] parts = cronExpression.split("\\s+");
        if (parts.length != 5) {
            return false;
        }
        
        try {
            // Check minute
            if (!matchesField(parts[0], dateTime.getMinute(), 0, 59)) {
                return false;
            }
            
            // Check hour
            if (!matchesField(parts[1], dateTime.getHour(), 0, 23)) {
                return false;
            }
            
            // Check day of month
            if (!matchesField(parts[2], dateTime.getDayOfMonth(), 1, 31)) {
                return false;
            }
            
            // Check month
            if (!matchesField(parts[3], dateTime.getMonthValue(), 1, 12)) {
                return false;
            }
            
            // Check day of week (Sunday = 0, Monday = 1, etc.)
            int dayOfWeek = dateTime.getDayOfWeek().getValue() % 7; // Convert to Sunday = 0
            if (!matchesField(parts[4], dayOfWeek, 0, 6)) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if a cron field matches the given value.
     *
     * @param field The cron field
     * @param value The value to check
     * @param min The minimum allowed value
     * @param max The maximum allowed value
     * @return true if the field matches
     */
    private boolean matchesField(String field, int value, int min, int max) {
        if ("*".equals(field)) {
            return true;
        }
        
        try {
            int fieldValue = Integer.parseInt(field);
            return fieldValue == value && fieldValue >= min && fieldValue <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Gets the next execution time for this schedule.
     *
     * @param from The time to calculate from
     * @return The next execution time
     */
    public LocalDateTime getNextExecution(LocalDateTime from) {
        LocalDateTime next = from.withSecond(0).withNano(0).plusMinutes(1);
        
        // Simple implementation: check each minute until we find a match
        for (int i = 0; i < 60 * 24 * 7; i++) { // Check up to a week ahead
            if (matches(next)) {
                return next;
            }
            next = next.plusMinutes(1);
        }
        
        throw new IllegalStateException("Could not find next execution time for cron expression: " + cronExpression);
    }
    
    /**
     * Schedules the next execution of a task based on this cron schedule.
     *
     * @param task The scheduled task to execute
     * @param scheduler The scheduler to use
     */
    public void scheduleNext(ScheduledTask task, ScheduledExecutorService scheduler) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecution = getNextExecution(now);
        Duration delay = Duration.between(now, nextExecution);
        
        scheduler.schedule(
            task.getRunnable(),
            delay.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }
    
    @Override
    public String toString() {
        return "CronSchedule{" +
                "cronExpression='" + cronExpression + '\'' +
                '}';
    }
}
