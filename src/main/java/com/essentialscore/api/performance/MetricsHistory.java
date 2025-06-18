package com.essentialscore.api.performance;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Maintains a history of metrics for trend analysis.
 */
public class MetricsHistory {
    private final int maxSize;
    private final Deque<ServerMetrics> serverMetricsHistory;
    private final Map<String, Deque<ModuleMetrics>> moduleMetricsHistory;
    
    /**
     * Creates a new metrics history.
     *
     * @param maxSize The maximum number of data points to store
     */
    public MetricsHistory(int maxSize) {
        this.maxSize = maxSize;
        this.serverMetricsHistory = new LinkedList<>();
        this.moduleMetricsHistory = new ConcurrentHashMap<>();
    }
    
    /**
     * Adds server metrics to the history.
     *
     * @param metrics The server metrics
     */
    public synchronized void addServerMetrics(ServerMetrics metrics) {
        serverMetricsHistory.addLast(metrics);
        
        // Remove oldest if we've exceeded maxSize
        while (serverMetricsHistory.size() > maxSize) {
            serverMetricsHistory.removeFirst();
        }
    }
    
    /**
     * Adds module metrics to the history.
     *
     * @param moduleId The module ID
     * @param metrics The module metrics
     */
    public synchronized void addModuleMetrics(String moduleId, ModuleMetrics metrics) {
        Deque<ModuleMetrics> history = moduleMetricsHistory.computeIfAbsent(
            moduleId,
            k -> new LinkedList<>()
        );
        
        history.addLast(metrics);
        
        // Remove oldest if we've exceeded maxSize
        while (history.size() > maxSize) {
            history.removeFirst();
        }
    }
    
    /**
     * Gets the server metrics history.
     *
     * @return The server metrics history
     */
    public List<ServerMetrics> getServerMetricsHistory() {
        return new ArrayList<>(serverMetricsHistory);
    }
    
    /**
     * Gets the module metrics history.
     *
     * @param moduleId The module ID
     * @return The module metrics history
     */
    public List<ModuleMetrics> getModuleMetricsHistory(String moduleId) {
        Deque<ModuleMetrics> history = moduleMetricsHistory.get(moduleId);
        return history != null ? new ArrayList<>(history) : new ArrayList<>();
    }
    
    /**
     * Gets the server metrics history for a time range.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @return The server metrics history
     */
    public List<ServerMetrics> getServerMetricsHistory(long startTime, long endTime) {
        return serverMetricsHistory.stream()
            .filter(m -> m.getTimestamp() >= startTime && m.getTimestamp() <= endTime)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the module metrics history for a time range.
     *
     * @param moduleId The module ID
     * @param startTime The start time
     * @param endTime The end time
     * @return The module metrics history
     */
    public List<ModuleMetrics> getModuleMetricsHistory(String moduleId, long startTime, long endTime) {
        Deque<ModuleMetrics> history = moduleMetricsHistory.get(moduleId);
        if (history == null) {
            return new ArrayList<>();
        }
        
        return history.stream()
            .filter(m -> m.getLastUpdateTime() >= startTime && m.getLastUpdateTime() <= endTime)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the average CPU load for a time range.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @return The average CPU load
     */
    public double getAverageCpuLoad(long startTime, long endTime) {
        List<ServerMetrics> metrics = getServerMetricsHistory(startTime, endTime);
        if (metrics.isEmpty()) {
            return 0.0;
        }
        
        return metrics.stream()
            .mapToDouble(ServerMetrics::getCpuLoad)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Gets the average memory usage for a time range.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @return The average memory usage
     */
    public double getAverageMemoryUsage(long startTime, long endTime) {
        List<ServerMetrics> metrics = getServerMetricsHistory(startTime, endTime);
        if (metrics.isEmpty()) {
            return 0.0;
        }
        
        return metrics.stream()
            .mapToDouble(ServerMetrics::getMemoryUsagePercentage)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Gets the average TPS for a time range.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @return The average TPS
     */
    public double getAverageTps(long startTime, long endTime) {
        List<ServerMetrics> metrics = getServerMetricsHistory(startTime, endTime);
        if (metrics.isEmpty()) {
            return 20.0; // Default to ideal
        }
        
        return metrics.stream()
            .mapToDouble(ServerMetrics::getTps)
            .average()
            .orElse(20.0);
    }
    
    /**
     * Gets the average response time for a module for a time range.
     *
     * @param moduleId The module ID
     * @param startTime The start time
     * @param endTime The end time
     * @return The average response time
     */
    public double getAverageResponseTime(String moduleId, long startTime, long endTime) {
        List<ModuleMetrics> metrics = getModuleMetricsHistory(moduleId, startTime, endTime);
        if (metrics.isEmpty()) {
            return 0.0;
        }
        
        return metrics.stream()
            .mapToDouble(ModuleMetrics::getAverageResponseTime)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Gets the player count trend for a time range.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @param intervals The number of intervals
     * @return The player count trend
     */
    public List<Integer> getPlayerCountTrend(long startTime, long endTime, int intervals) {
        List<ServerMetrics> metrics = getServerMetricsHistory(startTime, endTime);
        if (metrics.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Integer> trend = new ArrayList<>();
        long intervalDuration = (endTime - startTime) / intervals;
        
        for (int i = 0; i < intervals; i++) {
            long intervalStart = startTime + (i * intervalDuration);
            long intervalEnd = intervalStart + intervalDuration;
            
            int averageCount = (int) metrics.stream()
                .filter(m -> m.getTimestamp() >= intervalStart && m.getTimestamp() < intervalEnd)
                .mapToInt(ServerMetrics::getPlayerCount)
                .average()
                .orElse(0.0);
            
            trend.add(averageCount);
        }
        
        return trend;
    }
    
    /**
     * Gets the CPU load trend for a time range.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @param intervals The number of intervals
     * @return The CPU load trend
     */
    public List<Double> getCpuLoadTrend(long startTime, long endTime, int intervals) {
        List<ServerMetrics> metrics = getServerMetricsHistory(startTime, endTime);
        if (metrics.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Double> trend = new ArrayList<>();
        long intervalDuration = (endTime - startTime) / intervals;
        
        for (int i = 0; i < intervals; i++) {
            long intervalStart = startTime + (i * intervalDuration);
            long intervalEnd = intervalStart + intervalDuration;
            
            double averageLoad = metrics.stream()
                .filter(m -> m.getTimestamp() >= intervalStart && m.getTimestamp() < intervalEnd)
                .mapToDouble(ServerMetrics::getCpuLoad)
                .average()
                .orElse(0.0);
            
            trend.add(averageLoad);
        }
        
        return trend;
    }
    
    /**
     * Gets the last server metrics.
     *
     * @return The last server metrics, or null if none
     */
    public ServerMetrics getLastServerMetrics() {
        return serverMetricsHistory.isEmpty() ? null : serverMetricsHistory.getLast();
    }
    
    /**
     * Gets the last module metrics.
     *
     * @param moduleId The module ID
     * @return The last module metrics, or null if none
     */
    public ModuleMetrics getLastModuleMetrics(String moduleId) {
        Deque<ModuleMetrics> history = moduleMetricsHistory.get(moduleId);
        return history != null && !history.isEmpty() ? history.getLast() : null;
    }
} 
