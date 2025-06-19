package com.essentialscore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Enhanced Performance-Monitor für den Core und Module
 * Jetzt mit verbesserter Anzeige und MB-Angaben
 */
public class PerformanceMonitor {
    private final ApiCore core;    private final Map<String, ModulePerformanceData> performanceData = new ConcurrentHashMap<>();
    private final File performanceLogDir;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    // Enhanced metrics tracking
    private double currentTPS = 20.0;
    private int currentPlayers = 0;
    private int currentChunks = 0;
    private int currentEntities = 0;
    private double cpuUsage = 0.0;
    
    /**
     * Erstellt einen neuen Performance-Monitor
     * 
     * @param core Referenz auf den ApiCore
     */
    public PerformanceMonitor(ApiCore core) {
        this.core = core;
          // Performance-Log-Verzeichnis erstellen
        performanceLogDir = new File(core.getDataFolder(), "performance_logs");
        if (!performanceLogDir.exists() && !performanceLogDir.mkdirs()) {
            core.getLogger().warning("Konnte Performance-Log-Verzeichnis nicht erstellen!");
        }
        
        // Initiale Datensammlung auf dem Main Thread
        core.getServer().getScheduler().runTask(core, this::collectPerformanceData);
        
        // Performance-Monitoring starten
        startPerformanceMonitoring();
    }
      /**
     * Startet das Performance-Monitoring mit regelmäßigen Überprüfungen
     */
    private void startPerformanceMonitoring() {
        // Alle 30 Sekunden Performance-Daten sammeln (auf Main Thread für World-Zugriff)
        core.getServer().getScheduler().runTaskTimer(core, this::collectPerformanceData, 
            20 * 5, 20 * 30);
        
        // Alle 5 Minuten Performance-Daten protokollieren (kann async bleiben)
        core.getServer().getScheduler().runTaskTimerAsynchronously(core, this::logPerformanceData,
            20 * 60, 20 * 60 * 5);
    }
    
    /**
     * Sammelt Performance-Daten für den Core und alle Module
     */
    private void collectPerformanceData() {
        // System-Performance-Daten sammeln
        collectSystemPerformanceData();
        
        // Modul-Performance-Daten sammeln
        for (Map.Entry<String, com.essentialscore.api.module.ModuleManager.ModuleInfo> entry : core.getLoadedModules().entrySet()) {
            String moduleName = entry.getKey();
            // Erstelle oder aktualisiere Performance-Daten für das Modul
            ModulePerformanceData data = performanceData.computeIfAbsent(
                moduleName, k -> new ModulePerformanceData(moduleName));
            
            // Sammle spezifische Modul-Performance-Daten
            // Erfasse Basis-Metriken wie Arbeitsspeicher und CPU-Nutzung
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double memoryUsage = ((double) (totalMemory - freeMemory) / totalMemory) * 100;
            
            data.updateMethodData("memory_usage", memoryUsage, 1);
            
            // Die aktuelle Zeit als letzten Sammelzeitpunkt setzen
            data.updateMethodData("collection", 0.0, 0);
        }
    }
      /**
     * Sammelt System-Performance-Daten mit verbesserter Metriken
     */    private void collectSystemPerformanceData() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        // Aktuelle Werte für spätere Verwendung speichern
        currentPlayers = core.getServer().getOnlinePlayers().size();
        currentChunks = getLoadedChunksCount();
        currentEntities = getLoadedEntitiesCount();
        
        // CPU-Nutzung schätzen (basierend auf Thread-Aktivität)
        cpuUsage = Math.min(100.0, (Thread.activeCount() / 20.0) * 100.0);
        
        // TPS approximieren (vereinfacht)
        currentTPS = Math.max(1.0, Math.min(20.0, 20.0 - (cpuUsage / 10.0)));
        
        // Speichern der Systemdaten in einem speziellen Modul-Eintrag
        ModulePerformanceData systemData = performanceData.computeIfAbsent(
            "system", k -> new ModulePerformanceData("system"));
        
        // Memory in MB for better readability
        systemData.updateMethodData("total_memory_mb", bytesToMB(totalMemory), 0);
        systemData.updateMethodData("free_memory_mb", bytesToMB(freeMemory), 0);
        systemData.updateMethodData("used_memory_mb", bytesToMB(usedMemory), 0);
        systemData.updateMethodData("max_memory_mb", bytesToMB(maxMemory), 0);
        systemData.updateMethodData("memory_usage_percent", ((double) usedMemory / maxMemory) * 100, 0);
        
        // Server metrics
        systemData.updateMethodData("players_online", currentPlayers, 0);
        systemData.updateMethodData("chunks_loaded", currentChunks, 0);
        systemData.updateMethodData("entities_loaded", currentEntities, 0);
        systemData.updateMethodData("tps", currentTPS, 0);
        systemData.updateMethodData("cpu_usage_percent", cpuUsage, 0);
        
        // Thread information
        systemData.updateMethodData("active_threads", Thread.activeCount(), 0);
        
        // JVM information
        systemData.updateMethodData("jvm_uptime_minutes", getJVMUptimeMinutes(), 0);
    }
    
    /**
     * Konvertiert Bytes zu MB
     */
    private double bytesToMB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }
      /**
     * Anzahl der geladenen Chunks im Server ermitteln
     * MUSS auf dem Main Thread aufgerufen werden!
     */
    private int getLoadedChunksCount() {
        try {
            // Thread-Safety Check
            if (!core.getServer().isPrimaryThread()) {
                core.getLogger().warning("getLoadedChunksCount() called from async thread! Returning cached value.");
                return currentChunks > 0 ? currentChunks : 0;
            }
            
            int count = 0;
            for (org.bukkit.World world : core.getServer().getWorlds()) {
                count += world.getLoadedChunks().length;
            }
            return count;
        } catch (Exception e) {
            core.getLogger().warning("Error getting loaded chunks count: " + e.getMessage());
            return currentChunks > 0 ? currentChunks : 0;
        }
    }
    
    /**
     * Anzahl der geladenen Entities im Server ermitteln
     * MUSS auf dem Main Thread aufgerufen werden!
     */
    private int getLoadedEntitiesCount() {
        try {
            // Thread-Safety Check
            if (!core.getServer().isPrimaryThread()) {
                core.getLogger().warning("getLoadedEntitiesCount() called from async thread! Returning cached value.");
                return currentEntities > 0 ? currentEntities : 0;
            }
            
            int count = 0;
            for (org.bukkit.World world : core.getServer().getWorlds()) {
                count += world.getEntities().size();
            }
            return count;
        } catch (Exception e) {
            core.getLogger().warning("Error getting loaded entities count: " + e.getMessage());
            return currentEntities > 0 ? currentEntities : 0;
        }
    }
    
    /**
     * JVM Uptime in Minuten
     */
    private double getJVMUptimeMinutes() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / (1000.0 * 60.0);
    }
      /**
     * Protokolliert die Performance-Daten in eine Datei mit verbesserter Formatierung
     */
    private void logPerformanceData() {
        String timestamp = dateFormat.format(new Date());
        File logFile = new File(performanceLogDir, "performance_" + timestamp + ".csv");
        
        try (FileWriter writer = new FileWriter(logFile, true)) {
            // CSV-Header schreiben, falls die Datei neu ist
            if (logFile.length() == 0) {
                writer.write("Timestamp,Module,Metric,Value,Unit,Raw_Value\n");
            }
            
            // Aktuellen Zeitstempel als String
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            // Daten aller Module schreiben
            for (ModulePerformanceData data : performanceData.values()) {
                Map<String, ModulePerformanceData.MethodPerformanceData> methodDataMap = data.getMethodData();
                for (Map.Entry<String, ModulePerformanceData.MethodPerformanceData> entry : methodDataMap.entrySet()) {
                    String metricName = entry.getKey();
                    double rawValue = entry.getValue().getAverageExecutionTime();
                    String formattedValue = formatMetricValue(metricName, rawValue);
                    String unit = getMetricUnit(metricName);
                    
                    writer.write(String.format("%s,%s,%s,%s,%s,%.6f\n", 
                        now, data.getModuleName(), metricName, formattedValue, unit, rawValue));
                }
            }
        } catch (IOException e) {
            core.getLogger().log(Level.WARNING, "Fehler beim Speichern der Performance-Daten: " + e.getMessage(), e);
        }
    }
    
    /**
     * Formatiert Metrik-Werte entsprechend ihrem Typ
     */
    private String formatMetricValue(String metricName, double value) {
        if (metricName.contains("memory") && metricName.contains("mb")) {
            return String.format("%.1f", value);
        } else if (metricName.contains("percent")) {
            return String.format("%.1f", value);
        } else if (metricName.contains("tps")) {
            return String.format("%.1f", value);
        } else if (metricName.contains("players") || metricName.contains("chunks") || 
                   metricName.contains("entities") || metricName.contains("threads")) {
            return String.format("%.0f", value);
        } else if (metricName.contains("uptime") && metricName.contains("minutes")) {
            return String.format("%.1f", value);
        }
        return String.format("%.2f", value);
    }
    
    /**
     * Gibt die Einheit für eine Metrik zurück
     */
    private String getMetricUnit(String metricName) {
        if (metricName.contains("memory") && metricName.contains("mb")) {
            return "MB";
        } else if (metricName.contains("percent")) {
            return "%";
        } else if (metricName.contains("tps")) {
            return "TPS";
        } else if (metricName.contains("players")) {
            return "Players";
        } else if (metricName.contains("chunks")) {
            return "Chunks";
        } else if (metricName.contains("entities")) {
            return "Entities";
        } else if (metricName.contains("threads")) {
            return "Threads";
        } else if (metricName.contains("uptime") && metricName.contains("minutes")) {
            return "Minutes";
        }
        return "";
    }
    
    /**
     * Registriert eine Methodenausführungszeit für ein Modul
     * 
     * @param moduleName Name des Moduls
     * @param methodName Name der Methode
     * @param executionTimeMs Ausführungszeit in Millisekunden
     */
    public void registerMethodExecution(String moduleName, String methodName, double executionTimeMs) {
        ModulePerformanceData data = performanceData.computeIfAbsent(
            moduleName, k -> new ModulePerformanceData(moduleName));
        
        data.updateMethodData(methodName, executionTimeMs, 0);
    }
    
    /**
     * Zeichnet eine Methodenausführung auf (Alias für registerMethodExecution)
     * 
     * @param moduleName Name des Moduls
     * @param methodName Name der Methode
     * @param durationMs Ausführungszeit in Millisekunden
     */
    public void recordMethodExecution(String moduleName, String methodName, long durationMs) {
        registerMethodExecution(moduleName, methodName, (double) durationMs);
    }
      /**
     * Gibt die Performance-Daten aller Module zurück
     * 
     * @return Map mit Modul-Performance-Daten
     */
    public Map<String, ModulePerformanceData> getPerformanceData() {
        return new HashMap<>(performanceData);
    }
    
    /**
     * Gibt die Performance-Daten eines bestimmten Moduls zurück
     * 
     * @param moduleName Name des Moduls
     * @return Performance-Daten oder null, wenn keine Daten verfügbar sind
     */
    public ModulePerformanceData getModulePerformanceData(String moduleName) {
        return performanceData.get(moduleName);
    }
    
    /**
     * Gibt formatierte Performance-Daten für die Chat-Ausgabe zurück
     * 
     * @return Formatierte Performance-String
     */
    public String getFormattedPerformanceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("§8[§b§lPerformance§8] §7Server Metrics:\n");
        
        ModulePerformanceData systemData = performanceData.get("system");
        if (systemData != null) {
            Map<String, ModulePerformanceData.MethodPerformanceData> methodData = systemData.getMethodData();
            
            // Memory Information mit MB-Anzeige
            double usedMemory = getMethodValue(methodData, "used_memory_mb");
            double maxMemory = getMethodValue(methodData, "max_memory_mb");
            double memoryPercent = getMethodValue(methodData, "memory_usage_percent");
            
            info.append(String.format("§8• §7Memory: §f%.1f MB §8/ §f%.1f MB §8(§f%.1f%%§8)\n", 
                usedMemory, maxMemory, memoryPercent));
            
            // TPS und CPU
            double tps = getMethodValue(methodData, "tps");
            double cpu = getMethodValue(methodData, "cpu_usage_percent");
            
            info.append(String.format("§8• §7TPS: §f%.1f §8| §7CPU: §f%.1f%%\n", tps, cpu));
            
            // Server Information
            int players = (int) getMethodValue(methodData, "players_online");
            int chunks = (int) getMethodValue(methodData, "chunks_loaded");
            int entities = (int) getMethodValue(methodData, "entities_loaded");
            
            info.append(String.format("§8• §7Players: §f%d §8| §7Chunks: §f%d §8| §7Entities: §f%d\n", 
                players, chunks, entities));
            
            // JVM Information
            double uptime = getMethodValue(methodData, "jvm_uptime_minutes");
            int threads = (int) getMethodValue(methodData, "active_threads");
            
            info.append(String.format("§8• §7Uptime: §f%.1f min §8| §7Threads: §f%d", uptime, threads));
        } else {
            info.append("§c No system performance data available");
        }
        
        return info.toString();
    }
    
    /**
     * Gibt eine kompakte Performance-Übersicht zurück
     */
    public String getCompactPerformanceInfo() {
        ModulePerformanceData systemData = performanceData.get("system");
        if (systemData == null) return "§c No data available";
        
        Map<String, ModulePerformanceData.MethodPerformanceData> methodData = systemData.getMethodData();
        
        double memoryPercent = getMethodValue(methodData, "memory_usage_percent");
        double tps = getMethodValue(methodData, "tps");
        int players = (int) getMethodValue(methodData, "players_online");
        
        return String.format("§7Memory: §f%.1f%% §8| §7TPS: §f%.1f §8| §7Players: §f%d", 
            memoryPercent, tps, players);
    }
    
    /**
     * Gibt detaillierte ungerundete Werte zurück
     */
    public String getDetailedPerformanceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("§8[§b§lDetailed Performance§8] §7Raw Values:\n");
        
        ModulePerformanceData systemData = performanceData.get("system");
        if (systemData != null) {
            Map<String, ModulePerformanceData.MethodPerformanceData> methodData = systemData.getMethodData();
            
            for (Map.Entry<String, ModulePerformanceData.MethodPerformanceData> entry : methodData.entrySet()) {
                String key = entry.getKey();
                double value = entry.getValue().getAverageExecutionTime();
                
                info.append(String.format("§8• §7%s: §f%.6f\n", formatMetricName(key), value));
            }
        }
        
        return info.toString();
    }
    
    /**
     * Hilfsmethode zum Abrufen von Methodenwerten
     */
    private double getMethodValue(Map<String, ModulePerformanceData.MethodPerformanceData> methodData, String key) {
        ModulePerformanceData.MethodPerformanceData data = methodData.get(key);
        return data != null ? data.getAverageExecutionTime() : 0.0;
    }
    
    /**
     * Formatiert Metrik-Namen für bessere Lesbarkeit
     */
    private String formatMetricName(String key) {
        return key.replace("_", " ").toUpperCase();
    }
    
    /**
     * Löscht alle gesammelten Performance-Daten
     */
    public void clearAllPerformanceData() {
        performanceData.clear();
    }
}
