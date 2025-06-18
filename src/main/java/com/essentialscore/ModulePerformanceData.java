package com.essentialscore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Klasse zur Erfassung und Speicherung von Performance-Daten für Module
 */
public class ModulePerformanceData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String moduleName;
    private final Map<String, MethodPerformanceData> methodData;
    private long lastUpdateTime;
    private long totalMemoryUsage;
    private int invocationCount;
    
    public ModulePerformanceData(String moduleName) {
        this.moduleName = moduleName;
        this.methodData = new HashMap<>();
        this.lastUpdateTime = System.currentTimeMillis();
        this.totalMemoryUsage = 0;
        this.invocationCount = 0;
    }
    
    /**
     * Aktualisiert die Performance-Daten einer Methode
     * 
     * @param methodName Name der Methode
     * @param executionTimeMs Ausführungszeit in Millisekunden
     * @param memoryUsage Speicherverbrauch in Bytes (kann 0 sein, falls nicht gemessen)
     */
    public void updateMethodData(String methodName, double executionTimeMs, long memoryUsage) {
        MethodPerformanceData data = methodData.computeIfAbsent(methodName, 
                k -> new MethodPerformanceData(methodName));
        
        data.addExecution(executionTimeMs, memoryUsage);
        
        lastUpdateTime = System.currentTimeMillis();
        totalMemoryUsage += memoryUsage;
        invocationCount++;
    }
    
    /**
     * Gibt die durchschnittliche Ausführungszeit für eine Methode zurück
     * 
     * @param methodName Name der Methode
     * @return Durchschnittliche Ausführungszeit in Millisekunden oder -1 falls keine Daten vorhanden
     */
    public double getAverageExecutionTime(String methodName) {
        MethodPerformanceData data = methodData.get(methodName);
        return data != null ? data.getAverageExecutionTime() : -1;
    }
    
    /**
     * Gibt den Gesamtspeicherverbrauch des Moduls zurück
     */
    public long getTotalMemoryUsage() {
        return totalMemoryUsage;
    }
    
    /**
     * Berechnet die durchschnittliche Ausführungszeit für das gesamte Modul
     */
    public double getAverageExecutionTime() {
        if (invocationCount == 0) return 0;
        
        double sum = 0;
        for (MethodPerformanceData methodData : methodData.values()) {
            sum += methodData.getAverageExecutionTime() * methodData.getExecutionCount();
        }
        
        return sum / invocationCount;
    }
    
    /**
     * Gibt die Anzahl der Methodenaufrufe zurück
     */
    public int getInvocationCount() {
        return invocationCount;
    }
    
    /**
     * Gibt den Namen des Moduls zurück
     */
    public String getModuleName() {
        return moduleName;
    }
    
    /**
     * Gibt das Alter der Daten in Millisekunden zurück
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - lastUpdateTime;
    }
    
    /**
     * Gibt alle Methoden-Performance-Daten zurück
     */
    public Map<String, MethodPerformanceData> getMethodData() {
        return new HashMap<>(methodData);
    }
    
    /**
     * Innere Klasse zur Speicherung von Performance-Daten für einzelne Methoden
     */
    public static class MethodPerformanceData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String methodName;
        private double totalExecutionTimeMs;
        private long totalMemoryUsage;
        private int executionCount;
        private double maxExecutionTimeMs;
        private double minExecutionTimeMs;
        private long lastExecutionTimestamp;
        
        public MethodPerformanceData(String methodName) {
            this.methodName = methodName;
            this.totalExecutionTimeMs = 0;
            this.totalMemoryUsage = 0;
            this.executionCount = 0;
            this.maxExecutionTimeMs = 0;
            this.minExecutionTimeMs = Double.MAX_VALUE;
            this.lastExecutionTimestamp = System.currentTimeMillis();
        }
        
        /**
         * Fügt eine Ausführung hinzu
         * 
         * @param executionTimeMs Ausführungszeit in Millisekunden
         * @param memoryUsage Speicherverbrauch in Bytes
         */
        public void addExecution(double executionTimeMs, long memoryUsage) {
            totalExecutionTimeMs += executionTimeMs;
            totalMemoryUsage += memoryUsage;
            executionCount++;
            
            if (executionTimeMs > maxExecutionTimeMs) {
                maxExecutionTimeMs = executionTimeMs;
            }
            
            if (executionTimeMs < minExecutionTimeMs) {
                minExecutionTimeMs = executionTimeMs;
            }
            
            lastExecutionTimestamp = System.currentTimeMillis();
        }
        
        /**
         * Gibt die durchschnittliche Ausführungszeit zurück
         */
        public double getAverageExecutionTime() {
            return executionCount > 0 ? totalExecutionTimeMs / executionCount : 0;
        }
        
        /**
         * Gibt den durchschnittlichen Speicherverbrauch zurück
         */
        public long getAverageMemoryUsage() {
            return executionCount > 0 ? totalMemoryUsage / executionCount : 0;
        }
        
        /**
         * Gibt die maximale Ausführungszeit zurück
         */
        public double getMaxExecutionTime() {
            return maxExecutionTimeMs;
        }
        
        /**
         * Gibt die minimale Ausführungszeit zurück
         */
        public double getMinExecutionTime() {
            return minExecutionTimeMs == Double.MAX_VALUE ? 0 : minExecutionTimeMs;
        }
        
        /**
         * Gibt den Zeitstempel der letzten Ausführung zurück
         */
        public long getLastExecutionTimestamp() {
            return lastExecutionTimestamp;
        }
        
        /**
         * Gibt den Namen der Methode zurück
         */
        public String getMethodName() {
            return methodName;
        }
        
        /**
         * Gibt die Anzahl der Ausführungen zurück
         */
        public int getExecutionCount() {
            return executionCount;
        }
    }
} 
