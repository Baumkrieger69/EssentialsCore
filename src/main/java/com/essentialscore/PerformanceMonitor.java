package com.essentialscore;

import org.bukkit.World;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Performance-Monitor für den Core und Module
 */
public class PerformanceMonitor {
    private final ApiCore core;
    private final Map<String, ModulePerformanceData> performanceData = new ConcurrentHashMap<>();
    private final File performanceLogDir;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
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
        
        // Performance-Monitoring starten
            startPerformanceMonitoring();
    }
    
    /**
     * Startet das Performance-Monitoring mit regelmäßigen Überprüfungen
     */
    private void startPerformanceMonitoring() {
        // Alle 60 Sekunden Performance-Daten sammeln
        core.getServer().getScheduler().runTaskTimerAsynchronously(core, this::collectPerformanceData, 
            20 * 10, 20 * 60);
        
        // Alle 5 Minuten Performance-Daten protokollieren
        core.getServer().getScheduler().runTaskTimerAsynchronously(core, this::logPerformanceData,
            20 * 20, 20 * 60 * 5);
    }
    
    /**
     * Sammelt Performance-Daten für den Core und alle Module
     */
    private void collectPerformanceData() {
        // System-Performance-Daten sammeln
        collectSystemPerformanceData();
        
        // Modul-Performance-Daten sammeln
        for (Map.Entry<String, ApiCore.ModuleInfo> entry : core.getLoadedModules().entrySet()) {
            String moduleName = entry.getKey();
            
            // Erstelle oder aktualisiere Performance-Daten für das Modul
            ModulePerformanceData data = performanceData.computeIfAbsent(
                moduleName, k -> new ModulePerformanceData(moduleName));
            
            // TODO: Hier könnten spezifische Modul-Performance-Daten gesammelt werden
            // wie z.B. Anzahl der Befehle, Handler-Ausführungen, etc.
            
            // Die aktuelle Zeit als letzten Sammelzeitpunkt setzen
            data.updateMethodData("collection", 0.0, 0);
        }
    }
    
    /**
     * Sammelt System-Performance-Daten
     */
    private void collectSystemPerformanceData() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Speichern der Systemdaten in einem speziellen Modul-Eintrag
        ModulePerformanceData systemData = performanceData.computeIfAbsent(
            "system", k -> new ModulePerformanceData("system"));
        
        systemData.updateMethodData("total_memory", totalMemory, 0);
        systemData.updateMethodData("free_memory", freeMemory, 0);
        systemData.updateMethodData("used_memory", usedMemory, 0);
        systemData.updateMethodData("players_online", core.getServer().getOnlinePlayers().size(), 0);
        systemData.updateMethodData("chunks_loaded", getLoadedChunksCount(), 0);
        
        // CPU-Last messen (approximiert durch Thread-Aktivität)
        systemData.updateMethodData("active_threads", Thread.activeCount(), 0);
    }
    
    /**
     * Anzahl der geladenen Chunks im Server ermitteln
     */
    private int getLoadedChunksCount() {
        try {
            int count = 0;
            for (org.bukkit.World world : core.getServer().getWorlds()) {
                count += world.getLoadedChunks().length;
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Protokolliert die Performance-Daten in eine Datei
     */
    private void logPerformanceData() {
        String timestamp = dateFormat.format(new Date());
        File logFile = new File(performanceLogDir, "performance_" + timestamp + ".csv");
        
        try (FileWriter writer = new FileWriter(logFile, true)) {
            // CSV-Header schreiben, falls die Datei neu ist
            if (logFile.length() == 0) {
                writer.write("Timestamp,Module,Metric,Value\n");
            }
            
            // Aktuellen Zeitstempel als String
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            // Daten aller Module schreiben
            for (ModulePerformanceData data : performanceData.values()) {
                Map<String, ModulePerformanceData.MethodPerformanceData> methodDataMap = data.getMethodData();
                for (Map.Entry<String, ModulePerformanceData.MethodPerformanceData> entry : methodDataMap.entrySet()) {
                    writer.write(String.format("%s,%s,%s,%.2f\n", 
                        now, data.getModuleName(), entry.getKey(), entry.getValue().getAverageExecutionTime()));
                }
            }
        } catch (IOException e) {
            core.getLogger().log(Level.WARNING, "Fehler beim Speichern der Performance-Daten: " + e.getMessage(), e);
        }
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
     * Löscht alle gesammelten Performance-Daten
     */
    public void clearAllPerformanceData() {
        performanceData.clear();
    }
} 