package com.essentialscore.api.scheduling;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses cron expressions and calculates the next execution time.
 */
public class CronParser {
    private static final Pattern CRON_PATTERN = Pattern.compile(
            "^(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)(?:\\s+(\\S+))?$");
    
    /**
     * Gets the next execution time for a cron expression.
     *
     * @param cronExpression The cron expression
     * @return The next execution time in milliseconds
     */
    public static long getNextExecutionTime(String cronExpression) {
        try {
            CronFields fields = parseCronExpression(cronExpression);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next = calculateNextExecution(now, fields);
            return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }
    
    /**
     * Parses a cron expression.
     *
     * @param cronExpression The cron expression
     * @return The cron fields
     */
    private static CronFields parseCronExpression(String cronExpression) {
        Matcher matcher = CRON_PATTERN.matcher(cronExpression);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid cron expression format: " + cronExpression);
        }
        
        String minuteExpr = matcher.group(1);
        String hourExpr = matcher.group(2);
        String dayOfMonthExpr = matcher.group(3);
        String monthExpr = matcher.group(4);
        String dayOfWeekExpr = matcher.group(5);
        
        return new CronFields(
            parseField(minuteExpr, 0, 59),
            parseField(hourExpr, 0, 23),
            parseField(dayOfMonthExpr, 1, 31),
            parseField(monthExpr, 1, 12),
            parseDayOfWeek(dayOfWeekExpr)
        );
    }
    
    /**
     * Parses a cron field.
     *
     * @param fieldExpr The field expression
     * @param min The minimum value
     * @param max The maximum value
     * @return The parsed values
     */
    private static List<Integer> parseField(String fieldExpr, int min, int max) {
        List<Integer> values = new ArrayList<>();
        
        // Handle special cases
        if (fieldExpr.equals("*")) {
            // All values
            for (int i = min; i <= max; i++) {
                values.add(i);
            }
            return values;
        }
        
        // Handle comma-separated values
        String[] parts = fieldExpr.split(",");
        for (String part : parts) {
            if (part.contains("-")) {
                // Range (e.g., 1-5)
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                
                if (start < min || end > max) {
                    throw new IllegalArgumentException("Invalid range: " + part);
                }
                
                for (int i = start; i <= end; i++) {
                    values.add(i);
                }
            } else if (part.contains("/")) {
                // Step (e.g., */5, 1-30/5)
                String[] stepParts = part.split("/");
                int step = Integer.parseInt(stepParts[1]);
                
                List<Integer> baseValues = new ArrayList<>();
                if (stepParts[0].equals("*")) {
                    for (int i = min; i <= max; i++) {
                        baseValues.add(i);
                    }
                } else if (stepParts[0].contains("-")) {
                    String[] range = stepParts[0].split("-");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    
                    if (start < min || end > max) {
                        throw new IllegalArgumentException("Invalid range: " + stepParts[0]);
                    }
                    
                    for (int i = start; i <= end; i++) {
                        baseValues.add(i);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid step expression: " + part);
                }
                
                for (int i = 0; i < baseValues.size(); i += step) {
                    values.add(baseValues.get(i));
                }
            } else {
                // Single value
                int value = Integer.parseInt(part);
                
                if (value < min || value > max) {
                    throw new IllegalArgumentException("Invalid value: " + part);
                }
                
                values.add(value);
            }
        }
        
        return values;
    }
    
    /**
     * Parses the day of week field.
     *
     * @param dayOfWeekExpr The day of week expression
     * @return The parsed values
     */
    private static List<Integer> parseDayOfWeek(String dayOfWeekExpr) {
        // Replace text representations with numbers
        dayOfWeekExpr = dayOfWeekExpr.replace("SUN", "0").replace("MON", "1")
                .replace("TUE", "2").replace("WED", "3")
                .replace("THU", "4").replace("FRI", "5").replace("SAT", "6");
        
        List<Integer> values = parseField(dayOfWeekExpr, 0, 6);
        
        // Convert Sunday from 0 to 7 if needed
        if (values.contains(0) && !values.contains(7)) {
            values.add(7);
        }
        
        return values;
    }
    
    /**
     * Calculates the next execution time.
     *
     * @param from The time to calculate from
     * @param fields The cron fields
     * @return The next execution time
     */
    private static LocalDateTime calculateNextExecution(LocalDateTime from, CronFields fields) {
        // Start with the next minute
        LocalDateTime candidate = from.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        
        // Find the next valid time
        while (true) {
            // Check if the candidate time matches all fields
            if (!fields.minutes.contains(candidate.getMinute())) {
                candidate = candidate.withMinute(findNext(fields.minutes, candidate.getMinute()))
                        .truncatedTo(ChronoUnit.MINUTES);
                if (candidate.getMinute() < from.getMinute()) {
                    candidate = candidate.plusHours(1);
                }
                continue;
            }
            
            if (!fields.hours.contains(candidate.getHour())) {
                candidate = candidate.withHour(findNext(fields.hours, candidate.getHour()))
                        .withMinute(fields.minutes.get(0))
                        .truncatedTo(ChronoUnit.MINUTES);
                if (candidate.getHour() < from.getHour()) {
                    candidate = candidate.plusDays(1);
                }
                continue;
            }
            
            if (!fields.daysOfMonth.contains(candidate.getDayOfMonth()) &&
                    !fields.daysOfWeek.contains(candidate.getDayOfWeek().getValue())) {
                // Move to the next valid day
                candidate = candidate.plusDays(1)
                        .withHour(fields.hours.get(0))
                        .withMinute(fields.minutes.get(0))
                        .truncatedTo(ChronoUnit.MINUTES);
                continue;
            }
            
            if (!fields.months.contains(candidate.getMonthValue())) {
                candidate = candidate.with(TemporalAdjusters.firstDayOfNextMonth())
                        .withMonth(findNext(fields.months, candidate.getMonthValue()))
                        .withDayOfMonth(fields.daysOfMonth.get(0))
                        .withHour(fields.hours.get(0))
                        .withMinute(fields.minutes.get(0))
                        .truncatedTo(ChronoUnit.MINUTES);
                if (candidate.getMonthValue() < from.getMonthValue()) {
                    candidate = candidate.plusYears(1);
                }
                continue;
            }
            
            // All fields match
            break;
        }
        
        return candidate;
    }
    
    /**
     * Finds the next value in a list that is greater than or equal to the given value.
     *
     * @param values The values
     * @param current The current value
     * @return The next value
     */
    private static int findNext(List<Integer> values, int current) {
        for (int value : values) {
            if (value >= current) {
                return value;
            }
        }
        return values.get(0); // Wrap around
    }
    
    /**
     * Class representing the parsed cron fields.
     */
    private static class CronFields {
        final List<Integer> minutes;
        final List<Integer> hours;
        final List<Integer> daysOfMonth;
        final List<Integer> months;
        final List<Integer> daysOfWeek;
        
        CronFields(List<Integer> minutes, List<Integer> hours, List<Integer> daysOfMonth,
                   List<Integer> months, List<Integer> daysOfWeek) {
            this.minutes = minutes;
            this.hours = hours;
            this.daysOfMonth = daysOfMonth;
            this.months = months;
            this.daysOfWeek = daysOfWeek;
        }
    }
} 
