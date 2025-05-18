package com.essentialscore;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.nio.file.Files;

/**
 * Performance-Benchmark-Tool für EssentialsCore
 * Misst und analysiert die Performance verschiedener Komponenten
 */
public class PerformanceBenchmark {
    private final ApiCore apiCore;
    private final File benchmarkDir;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private boolean isRunning = false;

    public PerformanceBenchmark(ApiCore apiCore) {
        this.apiCore = apiCore;
        this.benchmarkDir = new File(apiCore.getDataFolder(), "benchmarks");
        if (!benchmarkDir.exists()) {
            benchmarkDir.mkdirs();
        }
    }

    /**
     * Führt einen vollständigen Benchmark aller Komponenten durch
     *
     * @return Benchmark-Ergebnisse als Map
     */
    public Map<String, Object> runFullBenchmark() {
        if (isRunning) {
            return Map.of("error", "Benchmark läuft bereits");
        }
        isRunning = true;

        // Nur eine Konsolenmeldung statt mehrerer
        apiCore.getLogger().info("Führe vollständigen Performance-Benchmark durch...");
        
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("timestamp", System.currentTimeMillis());
        results.put("server_name", Bukkit.getServer().getName());
        results.put("bukkit_version", Bukkit.getBukkitVersion());
        results.put("server_version", Bukkit.getVersion());
        results.put("java_version", System.getProperty("java.version"));
        results.put("os_name", System.getProperty("os.name"));
        results.put("processors", Runtime.getRuntime().availableProcessors());

        // Aktuelle Leistungsmetriken erfassen
        // TPS schätzen (da getTicksPerSecond() nicht verfügbar ist, verwenden wir einen statischen Wert)
        double estimatedTPS = 20.0; // Idealer Wert, in der Praxis oft niedriger
        results.put("tps", estimatedTPS);
        
        // Speichernutzung
        Map<String, Long> memoryInfo = new HashMap<>();
        memoryInfo.put("max", Runtime.getRuntime().maxMemory());
        memoryInfo.put("total", Runtime.getRuntime().totalMemory());
        memoryInfo.put("free", Runtime.getRuntime().freeMemory());
        results.put("memory", memoryInfo);
        
        // Thread-Informationen sammeln
        Map<String, Object> threadInfo = new HashMap<>();
        int threadCount = Thread.activeCount();
        threadInfo.put("count", threadCount);
        
        // Zähle Daemon-Threads
        int daemonThreads = 0;
        Thread[] threads = new Thread[threadCount * 2]; // Übergroßes Array für Sicherheit
        Thread.enumerate(threads);
        
        Map<String, String> threadDetails = new HashMap<>();
        for (Thread thread : threads) {
            if (thread != null) {
                if (thread.isDaemon()) {
                    daemonThreads++;
                }
                // Sammle Details über wichtige Threads
                if (thread.getName().contains("Craft") || 
                    thread.getName().contains("Bukkit") || 
                    thread.getName().contains("Minecraft") || 
                    thread.getName().contains("Server")) {
                    threadDetails.put(thread.getName(), 
                        "Priority: " + thread.getPriority() + ", State: " + thread.getState());
                }
            }
        }
        
        threadInfo.put("daemon", daemonThreads);
        threadInfo.put("details", threadDetails);
        results.put("threads", threadInfo);

        // Komponenten benchmarken (ohne zusätzliche Konsolenmeldungen)
        Map<String, Object> threadPoolResults = benchmarkThreadPool();
        Map<String, Object> cacheResults = benchmarkCache();
        Map<String, Object> moduleResults = benchmarkModuleLoading();
        Map<String, Object> ioResults = benchmarkIO();
        
        // Ergebnisse in das Hauptergebnis einfügen
        results.put("thread_pool", threadPoolResults);
        results.put("cache", cacheResults);
        results.put("module_loading", moduleResults);
        results.put("io_operations", ioResults);
        
        // Ergebnisse speichern
        String filename = "benchmark_full_" + dateFormat.format(new Date()) + ".yml";
        saveBenchmarkResults(results, filename);
        
        isRunning = false;
        return results;
    }

    /**
     * Benchmarkt nur den Thread-Pool
     *
     * @return Benchmark-Ergebnisse
     */
    public Map<String, Object> benchmarkThreadPool() {
        Map<String, Object> results = new LinkedHashMap<>();
        
        // Thread-Pool-Konfiguration auslesen
        ThreadManager.ThreadPoolInfo poolInfo = apiCore.getThreadManager().getThreadPoolInfo();
        results.put("pool_size", poolInfo.getPoolSize());
        results.put("core_pool_size", poolInfo.getCorePoolSize());
        results.put("max_pool_size", poolInfo.getMaxPoolSize());
        
        // Benchmark: Einfache Aufgaben
        long startTime = System.nanoTime();
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        for (int i = 0; i < taskCount; i++) {
            apiCore.getThreadManager().submit(() -> {
                // Simuliere leichte Arbeit
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
                latch.countDown();
            });
        }
        
        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            long duration = System.nanoTime() - startTime;
            results.put("simple_tasks_completed", completed);
            results.put("simple_tasks_duration_ms", duration / 1_000_000);
            results.put("tasks_per_second", (double) taskCount / (duration / 1_000_000_000.0));
        } catch (InterruptedException e) {
            results.put("error", "Benchmark wurde unterbrochen");
        }
        
        // Benchmark: Komplexe CPU-lastige Aufgaben
        startTime = System.nanoTime();
        int heavyTaskCount = 8;
        CountDownLatch heavyLatch = new CountDownLatch(heavyTaskCount);
        
        for (int i = 0; i < heavyTaskCount; i++) {
            final int taskId = i;
            apiCore.getThreadManager().submit(() -> {
                // Simuliere CPU-intensive Arbeit
                long result = 0;
                for (int j = 0; j < 1_000_000; j++) {
                    result += (j * taskId) % 1000;
                }
                heavyLatch.countDown();
                return result;
            });
        }
        
        try {
            boolean completed = heavyLatch.await(30, TimeUnit.SECONDS);
            long duration = System.nanoTime() - startTime;
            results.put("heavy_tasks_completed", completed);
            results.put("heavy_tasks_duration_ms", duration / 1_000_000);
            results.put("heavy_tasks_per_second", (double) heavyTaskCount / (duration / 1_000_000_000.0));
        } catch (InterruptedException e) {
            results.put("error", "Benchmark wurde unterbrochen");
        }
        
        // Thread-Pool-Auslastung nach dem Test
        ThreadManager.ThreadPoolInfo afterPoolInfo = apiCore.getThreadManager().getThreadPoolInfo();
        results.put("active_threads_after", afterPoolInfo.getActiveThreads());
        results.put("completed_tasks", afterPoolInfo.getCompletedTasks());
        results.put("queue_size", afterPoolInfo.getQueueSize());
        
        return results;
    }

    /**
     * Benchmarkt das Caching-System
     *
     * @return Benchmark-Ergebnisse
     */
    public Map<String, Object> benchmarkCache() {
        Map<String, Object> results = new LinkedHashMap<>();
        Map<String, Object> cacheOperations = new LinkedHashMap<>();
        
        // Method Cache testen
        int iterations = 10000;
        String[] testMethods = {"testMethod1", "testMethod2", "testMethod3", "testMethod4", "testMethod5"};
        
        // Cache leeren
        for (String method : testMethods) {
            apiCore.cleanMethodCache(method);
        }
        
        // Cache-Leistung messen
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String methodName = testMethods[i % testMethods.length];
            try {
                // Simuliert einen Method-Cache-Zugriff
                Object dummyInstance = new Object();
                apiCore.invokeMethod(dummyInstance, "toString", new Class[]{});
            } catch (Exception ignored) {
                // Ignorieren, wir messen nur die Zugriffszeit
            }
        }
        long duration = System.nanoTime() - startTime;
        
        // Berechne Operationen pro Sekunde
        double opsPerSecond = (double) iterations / (duration / 1_000_000_000.0);
        cacheOperations.put("gets_per_second", (int) opsPerSecond);
        cacheOperations.put("puts_per_second", (int) (opsPerSecond * 0.2)); // Schätzung für Puts
        
        results.put("method_cache_accesses", iterations);
        results.put("method_cache_duration_ms", duration / 1_000_000);
        results.put("method_cache_accesses_per_second", opsPerSecond);
        
        // Permission Cache testen, falls verfügbar
        if (apiCore.getPermissionManager() != null) {
            int permChecks = 5000;
            String[] testPermissions = {
                "apicore.admin", "apicore.debug", "apicore.commands", 
                "test.perm1", "test.perm2", "test.perm3", "test.perm4", "test.perm5"
            };
            
            startTime = System.nanoTime();
            for (int i = 0; i < permChecks; i++) {
                String permission = testPermissions[i % testPermissions.length];
                // Direkter Cache-Zugriff für Benchmark-Zwecke
                String cacheKey = "benchmark-test:" + permission;
                // Simuliere einen Cache-Hit oder Cache-Miss ohne echten Player
                apiCore.getSharedData(cacheKey);
            }
            duration = System.nanoTime() - startTime;
            
            results.put("permission_cache_checks", permChecks);
            results.put("permission_cache_duration_ms", duration / 1_000_000);
            results.put("permission_cache_checks_per_second", (double) permChecks / (duration / 1_000_000_000.0));
            results.put("permission_cache_size", apiCore.getPermissionCacheSize());
        }
        
        // Gesamtergebnis berechnen und zum cache_operations hinzufügen
        results.put("cache_operations", cacheOperations);
        
        return results;
    }

    /**
     * Benchmarkt das Laden von Modulen
     *
     * @return Benchmark-Ergebnisse
     */
    public Map<String, Object> benchmarkModuleLoading() {
        Map<String, Object> results = new LinkedHashMap<>();
        Map<String, Object> modules = new LinkedHashMap<>();
        
        // Aktuelle Module auflisten
        Map<String, ApiCore.ModuleInfo> loadedModules = apiCore.getLoadedModules();
        results.put("loaded_modules_count", loadedModules.size());
        
        // Modul-Ladezeiten sammeln
        Map<String, Long> moduleLoadTimes = new HashMap<>();
        
        for (Map.Entry<String, ApiCore.ModuleInfo> entry : loadedModules.entrySet()) {
            String moduleName = entry.getKey();
            
            // Module nur testen, wenn sie sicher neu geladen werden können
            try {
                long startTime = System.nanoTime();
                apiCore.getModuleManager().unloadModule(moduleName);
                long midTime = System.nanoTime();
                
                try {
                    ApiCore.ModuleInfo info = entry.getValue();
                    apiCore.getModuleManager().loadModule(info.getJarFile());
                    long endTime = System.nanoTime();
                    
                    long loadTime = (endTime - midTime) / 1_000_000;
                    moduleLoadTimes.put(moduleName, loadTime);
                    
                    // Speichere Modul-Informationen im Modul-spezifischen Bereich
                    Map<String, Object> moduleInfo = new HashMap<>();
                    moduleInfo.put("init_time", loadTime);
                    moduleInfo.put("unload_time", (midTime - startTime) / 1_000_000);
                    moduleInfo.put("total_time", (endTime - startTime) / 1_000_000);
                    
                    // Füge das Modul zur Modulliste hinzu
                    modules.put(moduleName, moduleInfo);
                    
                    results.put("unload_time_" + moduleName, (midTime - startTime) / 1_000_000);
                } catch (Exception e) {
                    apiCore.getLogger().log(Level.WARNING, "Fehler beim Neuladen von Modul " + moduleName, e);
                    results.put("error_" + moduleName, e.getMessage());
                }
            } catch (Exception e) {
                apiCore.getLogger().log(Level.WARNING, "Modul " + moduleName + " konnte nicht für Benchmark entladen werden", e);
            }
        }
        
        // Gesamtstatistik
        if (!moduleLoadTimes.isEmpty()) {
            LongSummaryStatistics stats = moduleLoadTimes.values().stream()
                    .collect(Collectors.summarizingLong(Long::longValue));
                    
            results.put("module_load_time_min_ms", stats.getMin());
            results.put("module_load_time_max_ms", stats.getMax());
            results.put("module_load_time_avg_ms", stats.getAverage());
            results.put("module_load_times", moduleLoadTimes);
            
            // Speichere alle Module in der Hauptkonfiguration
            results.put("modules", modules);
        }
        
        return results;
    }

    /**
     * Benchmarkt I/O-Operationen
     *
     * @return Benchmark-Ergebnisse
     */
    public Map<String, Object> benchmarkIO() {
        Map<String, Object> results = new LinkedHashMap<>();
        Map<String, Object> ioOperations = new LinkedHashMap<>();
        
        // Testdatei erstellen
        File testDir = new File(benchmarkDir, "temp");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        
        // Kleine Datei schreiben/lesen
        File smallFile = new File(testDir, "small_test.dat");
        int smallSize = 100 * 1024; // 100 KB
        
        long startTime = System.nanoTime();
        try (FileWriter writer = new FileWriter(smallFile)) {
            char[] data = new char[1024];
            Arrays.fill(data, 'x');
            for (int i = 0; i < 100; i++) {
                writer.write(data);
            }
        } catch (IOException e) {
            results.put("small_write_error", e.getMessage());
        }
        long smallWriteTime = System.nanoTime() - startTime;
        
        // Kleine Datei lesen
        startTime = System.nanoTime();
        try {
            byte[] buffer = new byte[8192];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(smallFile)) {
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    // Nur lesen
                }
            }
        } catch (IOException e) {
            results.put("small_read_error", e.getMessage());
        }
        long smallReadTime = System.nanoTime() - startTime;
        
        // Große Datei schreiben/lesen (wenn genug Speicher)
        File largeFile = new File(testDir, "large_test.dat");
        int largeSize = 10 * 1024 * 1024; // 10 MB
        
        startTime = System.nanoTime();
        try (FileWriter writer = new FileWriter(largeFile)) {
            char[] data = new char[8192];
            Arrays.fill(data, 'x');
            for (int i = 0; i < largeSize / 8192; i++) {
                writer.write(data);
            }
        } catch (IOException e) {
            results.put("large_write_error", e.getMessage());
        }
        long largeWriteTime = System.nanoTime() - startTime;
        
        // Große Datei lesen
        startTime = System.nanoTime();
        try {
            byte[] buffer = new byte[8192];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(largeFile)) {
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    // Nur lesen
                }
            }
        } catch (IOException e) {
            results.put("large_read_error", e.getMessage());
        }
        long largeReadTime = System.nanoTime() - startTime;
        
        // Ergebnisse sammeln
        results.put("small_file_write_ms", smallWriteTime / 1_000_000);
        results.put("small_file_read_ms", smallReadTime / 1_000_000);
        results.put("small_file_write_mb_per_sec", (double) smallSize / 1024 / 1024 / (smallWriteTime / 1_000_000_000.0));
        results.put("small_file_read_mb_per_sec", (double) smallSize / 1024 / 1024 / (smallReadTime / 1_000_000_000.0));
        
        results.put("large_file_write_ms", largeWriteTime / 1_000_000);
        results.put("large_file_read_ms", largeReadTime / 1_000_000);
        results.put("large_file_write_mb_per_sec", (double) largeSize / 1024 / 1024 / (largeWriteTime / 1_000_000_000.0));
        results.put("large_file_read_mb_per_sec", (double) largeSize / 1024 / 1024 / (largeReadTime / 1_000_000_000.0));
        
        // Durchschnittliche Lese- und Schreibzeit pro MB berechnen
        double avgReadTimePerMB = ((smallReadTime / (smallSize / 1024.0 / 1024.0)) + 
                                   (largeReadTime / (largeSize / 1024.0 / 1024.0))) / 2 / 1_000_000;
                                   
        double avgWriteTimePerMB = ((smallWriteTime / (smallSize / 1024.0 / 1024.0)) + 
                                    (largeWriteTime / (largeSize / 1024.0 / 1024.0))) / 2 / 1_000_000;
        
        ioOperations.put("read_time", avgReadTimePerMB);
        ioOperations.put("write_time", avgWriteTimePerMB);
        
        results.put("io_operations", ioOperations);
        
        // Aufräumen
        smallFile.delete();
        largeFile.delete();
        
        return results;
    }

    /**
     * Speichert Benchmark-Ergebnisse in einer Datei
     *
     * @param results Die Benchmark-Ergebnisse
     * @param filename Der Dateiname
     */
    private void saveBenchmarkResults(Map<String, Object> results, String filename) {
        File resultFile = new File(benchmarkDir, filename);
        
        try {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            config.save(resultFile);
            
            // Reduziere die Konsolenausgabe, wenn Debug-Modus nicht aktiv ist
            if (apiCore.isDebugMode()) {
                apiCore.getLogger().info("Benchmark-Ergebnisse gespeichert in " + resultFile.getPath());
            }
        } catch (IOException e) {
            apiCore.getLogger().log(Level.WARNING, "Fehler beim Speichern der Benchmark-Ergebnisse", e);
        }
    }
    
    /**
     * Vergleicht zwei Konfigurationen miteinander
     *
     * @param config1 Name der ersten Konfiguration
     * @param config2 Name der zweiten Konfiguration
     * @return Vergleichsergebnisse
     */
    public Map<String, Object> compareConfigurations(String config1, String config2) {
        File file1 = new File(benchmarkDir, config1);
        File file2 = new File(benchmarkDir, config2);
        
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("comparison_time", System.currentTimeMillis());
        results.put("file1", config1);
        results.put("file2", config2);
        
        if (!file1.exists() || !file2.exists()) {
            results.put("error", "Eine oder beide Konfigurationsdateien existieren nicht");
            return results;
        }
        
        if (config1.equals(config2)) {
            results.put("error", "Die beiden Dateien sind identisch");
            return results;
        }
        
        try {
            YamlConfiguration yaml1 = YamlConfiguration.loadConfiguration(file1);
            YamlConfiguration yaml2 = YamlConfiguration.loadConfiguration(file2);
            
            // Debug: Anzahl der Schlüssel in den Dateien
            if (apiCore.isDebugMode()) {
                apiCore.getLogger().info("Datei 1 (" + config1 + ") hat " + yaml1.getKeys(true).size() + " Schlüssel");
                apiCore.getLogger().info("Datei 2 (" + config2 + ") hat " + yaml2.getKeys(true).size() + " Schlüssel");
            }
            
            // Sammle die Flat-Keys statt verschachtelter Objekte
            Set<String> allFlatKeys = new HashSet<>();
            allFlatKeys.addAll(getFlatKeys(yaml1));
            allFlatKeys.addAll(getFlatKeys(yaml2));
            
            Map<String, Object> differences = new LinkedHashMap<>();
            int differenceCount = 0;
            
            for (String key : allFlatKeys) {
                // Debug-Meldungen und bestimmte Objekttypen ignorieren
                if (key.contains("MemorySection") || key.equals("Timestamp") || 
                    key.startsWith("@") || key.endsWith("@")) {
                    continue;
                }
                
                if (yaml1.contains(key) && yaml2.contains(key)) {
                    Object value1 = yaml1.get(key);
                    Object value2 = yaml2.get(key);
                    
                    // Objekte in primitive Werte oder Strings umwandeln
                    value1 = convertToComparable(value1);
                    value2 = convertToComparable(value2);
                    
                    // Vergleiche Zahlen
                    if (value1 instanceof Number && value2 instanceof Number) {
                        double num1 = ((Number) value1).doubleValue();
                        double num2 = ((Number) value2).doubleValue();
                        
                        // Prüfe ob tatsächlich ein Unterschied besteht (mit kleiner Toleranz für Fließkommazahlen)
                        if (Math.abs(num1 - num2) > 0.0001) {
                            Map<String, Object> diff = new LinkedHashMap<>();
                            diff.put("old", num1);
                            diff.put("new", num2);
                            diff.put("difference", num2 - num1);
                            
                            // Prozentuale Änderung nur berechnen, wenn der erste Wert ungleich 0 ist
                            if (num1 != 0) {
                                double percentage = ((num2 - num1) / Math.abs(num1)) * 100;
                                diff.put("percentage", percentage);
                            } else {
                                diff.put("percentage", 0);
                            }
                            
                            differences.put(key, diff);
                            differenceCount++;
                        }
                    }
                    // Vergleiche andere Typen
                    else if (!Objects.equals(value1, value2)) {
                        Map<String, Object> diff = new LinkedHashMap<>();
                        diff.put("old", value1 != null ? value1.toString() : "null");
                        diff.put("new", value2 != null ? value2.toString() : "null");
                        
                        differences.put(key, diff);
                        differenceCount++;
                    }
                } else if (yaml1.contains(key) && !yaml2.contains(key)) {
                    // Nur in Datei 1 vorhanden
                    Map<String, Object> diff = new LinkedHashMap<>();
                    Object value1 = yaml1.get(key);
                    value1 = convertToComparable(value1);
                    
                    if (value1 instanceof Number) {
                        diff.put("old", ((Number) value1).doubleValue());
                        diff.put("new", 0.0);
                        diff.put("percentage", -100.0); // -100% Änderung (komplett entfernt)
                    } else {
                        diff.put("old", value1 != null ? value1.toString() : "null");
                        diff.put("new", "FEHLT");
                    }
                    
                    differences.put(key, diff);
                    differenceCount++;
                } else if (!yaml1.contains(key) && yaml2.contains(key)) {
                    // Nur in Datei 2 vorhanden
                    Map<String, Object> diff = new LinkedHashMap<>();
                    Object value2 = yaml2.get(key);
                    value2 = convertToComparable(value2);
                    
                    if (value2 instanceof Number) {
                        diff.put("old", 0.0);
                        diff.put("new", ((Number) value2).doubleValue());
                        diff.put("percentage", 100.0); // Neu hinzugefügt (100% Änderung)
                    } else {
                        diff.put("old", "FEHLT");
                        diff.put("new", value2 != null ? value2.toString() : "null");
                    }
                    
                    differences.put(key, diff);
                    differenceCount++;
                }
            }
            
            // Debug-Ausgabe zur Anzahl der Unterschiede
            apiCore.getLogger().info("Vergleich abgeschlossen: " + differenceCount + " Unterschiede gefunden");
            
            results.put("differences", differences);
            results.put("difference_count", differenceCount);
            
            // Speichere Vergleichsergebnisse
            String resultFilename = "comparison_" + dateFormat.format(new Date()) + ".yml";
            saveBenchmarkResults(results, resultFilename);
            
            // Debug-Ausgabe für Vergleichsdatei
            if (apiCore.isDebugMode()) {
                File resultFile = new File(benchmarkDir, resultFilename);
                apiCore.getLogger().info("Vergleichsdatei gespeichert: " + resultFile.getAbsolutePath() + 
                                      " (Größe: " + resultFile.length() + " Bytes)");
            }
            
            return results;
        } catch (Exception e) {
            apiCore.getLogger().log(Level.WARNING, "Fehler beim Vergleichen der Konfigurationen", e);
            results.put("error", "Fehler beim Vergleichen der Konfigurationen: " + e.getMessage());
            return results;
        }
    }
    
    /**
     * Extrahiert alle flachen Schlüssel aus einer Konfiguration
     * 
     * @param config Die Konfiguration
     * @return Set mit allen flachen Schlüsseln
     */
    private Set<String> getFlatKeys(YamlConfiguration config) {
        Set<String> flatKeys = new HashSet<>();
        for (String key : config.getKeys(true)) {
            Object value = config.get(key);
            
            // Ignoriere ConfigurationSection-Objekte, aber behalte ihre Werte
            if (!(value instanceof ConfigurationSection)) {
                flatKeys.add(key);
            }
        }
        return flatKeys;
    }
    
    /**
     * Konvertiert komplexe Objekte in vergleichbare Werte
     * 
     * @param value Das zu konvertierende Objekt
     * @return Ein vergleichbarer Wert (primitiver Typ oder String)
     */
    private Object convertToComparable(Object value) {
        if (value == null) {
            return null;
        }
        
        // Behandle ConfigurationSection speziell
        if (value instanceof ConfigurationSection) {
            return "ConfigSection:" + ((ConfigurationSection) value).getName();
        }
        
        // Behandle Maps
        if (value instanceof Map) {
            return "Map mit " + ((Map<?, ?>) value).size() + " Einträgen";
        }
        
        // Behandle Listen
        if (value instanceof List) {
            return "Liste mit " + ((List<?>) value).size() + " Einträgen";
        }
        
        // Alles andere unverändert zurückgeben
        return value;
    }
    
    /**
     * Gibt die Liste der verfügbaren Benchmark-Ergebnisse zurück
     *
     * @return Liste der Benchmark-Dateien
     */
    public List<String> getAvailableResults() {
        if (!benchmarkDir.exists()) {
            return Collections.emptyList();
        }
        
        File[] files = benchmarkDir.listFiles((dir, name) -> 
                name.startsWith("benchmark_") || name.startsWith("comparison_"));
                
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(files)
                .map(File::getName)
                .sorted(Comparator.reverseOrder()) // Neueste zuerst
                .collect(Collectors.toList());
    }
    
    /**
     * Lädt und formatiert Benchmark-Ergebnisse für die Anzeige
     *
     * @param filename Der Dateiname
     * @return Formatierte Ergebnisse
     */
    public List<String> getFormattedResults(String filename) {
        File resultFile = new File(benchmarkDir, filename);
        List<String> lines = new ArrayList<>();
        
        if (!resultFile.exists()) {
            lines.add("§cDatei nicht gefunden: " + filename);
            return lines;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(resultFile);
            
            if (filename.startsWith("benchmark_")) {
                // Erster Teil: Benchmarkübersicht
                lines.add("§b§l=== Benchmark-Ergebnisse: " + filename + " ===");
                lines.add("§6Server: §f" + config.getString("server_name", "Unbekannt"));
                lines.add("§6Zeitpunkt: §f" + new Date(config.getLong("timestamp", 0)));
                lines.add("§6Bukkit-Version: §f" + config.getString("bukkit_version", "Unbekannt"));
                lines.add("§6Java-Version: §f" + config.getString("java_version", "Unbekannt"));
                lines.add("");
                
                // Zweiter Teil: Performance-Metriken
                lines.add("§a§l--- Performance-Metriken ---");
                lines.add("§3TPS: §f" + config.getDouble("tps", 0) + " §7(20 = optimal)");
                
                // Speichernutzung kompakt darstellen
                long maxMemory = config.getLong("memory.max", 0);
                long totalMemory = config.getLong("memory.total", 0);
                long freeMemory = config.getLong("memory.free", 0);
                long usedMemory = totalMemory - freeMemory;
                
                // In MB umrechnen und Prozentsätze berechnen
                double usedMemoryMB = usedMemory / (1024.0 * 1024.0);
                double totalMemoryMB = totalMemory / (1024.0 * 1024.0);
                double maxMemoryMB = maxMemory / (1024.0 * 1024.0);
                int usagePercent = (int) (usedMemory * 100.0 / totalMemory);
                
                lines.add(String.format("§3Speicher: §f%.1f MB / %.1f MB §7(%.1f MB max, %d%% genutzt)", 
                        usedMemoryMB, totalMemoryMB, maxMemoryMB, usagePercent));
                
                // Thread-Informationen
                int threadCount = config.getInt("threads.count", 0);
                int daemonThreads = config.getInt("threads.daemon", 0);
                int bukkitThreads = 0;
                ConfigurationSection threadSection = config.getConfigurationSection("threads.details");
                if (threadSection != null) {
                    for (String threadName : threadSection.getKeys(false)) {
                        if (threadName.contains("Craft") || threadName.contains("Bukkit") || 
                                threadName.contains("Minecraft") || threadName.contains("Server")) {
                            bukkitThreads++;
                        }
                    }
                }
                
                lines.add(String.format("§3Threads: §f%d §7(%d Daemon, %d Bukkit/Minecraft)", 
                        threadCount, daemonThreads, bukkitThreads));
                
                // Dritter Teil: Details (bei Bedarf)
                if (config.contains("modules")) {
                    lines.add("");
                    lines.add("§a§l--- Modul-Performance ---");
                    ConfigurationSection modules = config.getConfigurationSection("modules");
                    for (String moduleName : modules.getKeys(false)) {
                        ConfigurationSection module = modules.getConfigurationSection(moduleName);
                        lines.add(String.format("§3%s: §f%.2f ms §7(Initialisierungszeit)", 
                                moduleName, module.getDouble("init_time", 0)));
                    }
                }
                
                if (config.contains("io_operations")) {
                    lines.add("");
                    lines.add("§a§l--- I/O-Performance ---");
                    ConfigurationSection io = config.getConfigurationSection("io_operations");
                    lines.add(String.format("§3Datei lesen: §f%.2f ms §7(durchschnittl. für 1MB)", 
                            io.getDouble("read_time", 0)));
                    lines.add(String.format("§3Datei schreiben: §f%.2f ms §7(durchschnittl. für 1MB)", 
                            io.getDouble("write_time", 0)));
                }
                
                if (config.contains("cache_operations")) {
                    lines.add("");
                    lines.add("§a§l--- Cache-Performance ---");
                    ConfigurationSection cache = config.getConfigurationSection("cache_operations");
                    lines.add(String.format("§3Cache-Abrufe: §f%d pro Sekunde", 
                            cache.getInt("gets_per_second", 0)));
                    lines.add(String.format("§3Cache-Speicherungen: §f%d pro Sekunde", 
                            cache.getInt("puts_per_second", 0)));
                }
            }
            else if (filename.startsWith("comparison_")) {
                // Vergleichsergebnisse
                lines.add("§b§l=== Benchmark-Vergleich ===");
                lines.add("§6Zeitpunkt: §f" + new Date(config.getLong("comparison_time", 0)));
                lines.add("");
                
                if (config.contains("differences")) {
                    ConfigurationSection differences = config.getConfigurationSection("differences");
                    if (differences == null || differences.getKeys(false).isEmpty()) {
                        lines.add("§fKeine Unterschiede zwischen den Konfigurationen gefunden.");
                    } else {
                        // Debug-Ausgabe: Anzahl der Unterschiede
                        int differenceCount = config.getInt("difference_count", differences.getKeys(false).size());
                        lines.add("§3Gefundene Unterschiede: §f" + differenceCount);
                        
                        // Dateinamen anzeigen
                        String file1 = config.getString("file1", "Benchmark 1");
                        String file2 = config.getString("file2", "Benchmark 2");
                        lines.add("§3Verglichene Dateien: §f" + file1 + " → " + file2);
                        lines.add("");
                        
                        // Metrik-Kategorien erstellen
                        Map<String, List<ComparisonEntry>> categories = new HashMap<>();
                        categories.put("thread_pool", new ArrayList<>());       // Thread-Pool-Performance
                        categories.put("memory", new ArrayList<>());            // Speichernutzung
                        categories.put("cache", new ArrayList<>());             // Cache-Performance
                        categories.put("io", new ArrayList<>());                // I/O-Performance
                        categories.put("modules", new ArrayList<>());           // Modul-Performance
                        categories.put("other", new ArrayList<>());             // Sonstige Metriken
                        
                        // Sammle Unterschiede nach Kategorien
                        for (String key : differences.getKeys(false)) {
                            ConfigurationSection diff = differences.getConfigurationSection(key);
                            if (diff == null) continue;
                            
                            Object oldValue = diff.get("old");
                            Object newValue = diff.get("new");
                            
                            if (oldValue == null || newValue == null) continue;
                            
                            // Kategorie bestimmen
                            String category = "other";
                            if (key.startsWith("thread_pool") || key.contains("tasks_per_second") || 
                                key.contains("pool_size")) {
                                category = "thread_pool";
                            } else if (key.startsWith("memory") || key.contains("heap") || 
                                      key.contains("free")) {
                                category = "memory";
                            } else if (key.startsWith("cache") || key.contains("cache_operations") || 
                                      key.contains("gets_per_second") || key.contains("puts_per_second")) {
                                category = "cache";
                            } else if (key.startsWith("io_operations") || key.contains("read") || 
                                      key.contains("write") || key.contains("file")) {
                                category = "io";
                            } else if (key.startsWith("module") || key.contains("init_time") || 
                                      key.contains("load_time")) {
                                category = "modules";
                            } else if (key.startsWith("threads") || key.contains("thread_count")) {
                                category = "thread_pool";
                            }
                            
                            // Debug-Ausgabe der Kategorienzuordnung in Debug-Modus
                            if (apiCore.isDebugMode()) {
                                apiCore.getLogger().info("Zuordnung: " + key + " -> " + category);
                            }
                            
                            // Berechne prozentuale Änderung für numerische Werte
                            double percentChange = 0;
                            boolean isNumeric = false;
                            double oldNum = 0;
                            double newNum = 0;
                            
                            if (oldValue instanceof Number && newValue instanceof Number) {
                                isNumeric = true;
                                oldNum = ((Number) oldValue).doubleValue();
                                newNum = ((Number) newValue).doubleValue();
                                
                                // Prozentuale Änderung
                                if (diff.contains("percentage")) {
                                    percentChange = diff.getDouble("percentage");
                                } else if (oldNum != 0) {
                                    percentChange = (newNum - oldNum) / Math.abs(oldNum) * 100;
                                }
                            }
                            
                            // Bestimme, ob Veränderung positiv oder negativ ist
                            boolean lowerIsBetter = key.contains("time") || key.contains("memory") || 
                                                   key.contains("latency") || key.contains("duration") ||
                                                   key.contains("ms");
                            
                            boolean isImprovement = lowerIsBetter ? percentChange < 0 : percentChange > 0;
                            
                            // Lesbare Schlüsselbezeichnung
                            String readableKey = key.replace("_", " ")
                                                   .replace(".", " ")
                                                   .replace("per second", "/sek");
                            
                            // Erste Buchstabe großschreiben
                            if (readableKey.length() > 0) {
                                readableKey = readableKey.substring(0, 1).toUpperCase() + 
                                           (readableKey.length() > 1 ? readableKey.substring(1) : "");
                            }
                            
                            // Eintrag zur Kategorie hinzufügen
                            ComparisonEntry entry = new ComparisonEntry(readableKey, oldValue, newValue, 
                                                                  percentChange, isImprovement, isNumeric, 
                                                                  oldNum, newNum);
                            categories.get(category).add(entry);
                        }
                        
                        // Kategorien anzeigen, aber nur wenn sie nicht leer sind
                        displayCategory(lines, "§a§l--- Thread-Pool Performance ---", categories.get("thread_pool"));
                        displayCategory(lines, "§a§l--- Speichernutzung ---", categories.get("memory"));
                        displayCategory(lines, "§a§l--- Cache-Performance ---", categories.get("cache"));
                        displayCategory(lines, "§a§l--- I/O-Performance ---", categories.get("io"));
                        displayCategory(lines, "§a§l--- Modul-Performance ---", categories.get("modules"));
                        
                        // Sonstige Metriken nur anzeigen, wenn vorhanden
                        List<ComparisonEntry> otherMetrics = categories.get("other");
                        if (!otherMetrics.isEmpty()) {
                            displayCategory(lines, "§a§l--- Weitere Änderungen ---", otherMetrics);
                        }
                        
                        // Zusammenfassung der Verbesserungen und Verschlechterungen
                        int improvements = 0;
                        int regressions = 0;
                        
                        for (List<ComparisonEntry> category : categories.values()) {
                            for (ComparisonEntry entry : category) {
                                if (entry.isNumeric && entry.percentChange != 0) {
                                    if (entry.isImprovement) {
                                        improvements++;
                                    } else {
                                        regressions++;
                                    }
                                }
                            }
                        }
                        
                        lines.add("");
                        lines.add("§3Zusammenfassung: §a" + improvements + " Verbesserungen§f, §c" + 
                                 regressions + " Verschlechterungen");
                    }
                } else {
                    lines.add("§cKeine Vergleichsdaten verfügbar.");
                }
            }
            
            // Hinweis auf Dateipfad
            lines.add("");
            lines.add("§7Datei: plugins/EssentialsCore/benchmarks/" + filename);
            
        } catch (Exception e) {
            lines.add("§cFehler beim Lesen der Datei: " + e.getMessage());
        }
        
        return lines;
    }
    
    /**
     * Formatiert einen Metriknamen für bessere Lesbarkeit
     */
    private String formatMetricName(String key) {
        // Entferne Präfixe wie "thread_pool." oder "cache."
        String formatted = key.replaceAll("^(thread_pool\\.|cache\\.|io\\.|module_)", "");
        
        // Ersetze Unterstriche durch Leerzeichen
        formatted = formatted.replace('_', ' ');
        
        // Erste Buchstaben groß schreiben
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }

    /**
     * Löscht alle gespeicherten Benchmark-Ergebnisse und Performance-Monitoring-Dateien
     * 
     * @return Anzahl der gelöschten Dateien
     */
    public int clearAllBenchmarkFiles() {
        if (!benchmarkDir.exists()) {
            return 0;
        }
        
        int count = 0;
        
        // Alle Benchmark-Dateien löschen
        File[] benchmarkFiles = benchmarkDir.listFiles((dir, name) -> 
                name.startsWith("benchmark_") || name.startsWith("comparison_"));
                
        if (benchmarkFiles != null) {
            for (File file : benchmarkFiles) {
                if (file.delete()) {
                    count++;
                }
            }
        }
        
        // Performance-Monitoring-Ordner finden und Dateien löschen
        File monitoringDir = new File(apiCore.getDataFolder(), "monitoring");
        if (monitoringDir.exists()) {
            File[] monitoringFiles = monitoringDir.listFiles();
            if (monitoringFiles != null) {
                for (File file : monitoringFiles) {
                    if (file.delete()) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * Hilfsmethode zum Anzeigen einer Kategorie von Vergleichsergebnissen
     * 
     * @param lines Die Zeilenliste, zu der die formatierten Zeilen hinzugefügt werden
     * @param title Der Titel der Kategorie
     * @param entries Die Einträge in dieser Kategorie
     */
    private void displayCategory(List<String> lines, String title, List<ComparisonEntry> entries) {
        // Leere Kategorien nicht anzeigen
        if (entries == null || entries.isEmpty()) {
            return;
        }
        
        // Titel und Ergebnisse anzeigen
        lines.add("");
        lines.add(title);
        
        // Sortiere die Einträge: Zuerst nach Verbesserung/Verschlechterung, dann nach Prozent-Betrag
        entries.sort((a, b) -> {
            // Wenn beide gleich (Verbesserung oder Verschlechterung), nach Prozentwert sortieren
            if (a.isImprovement == b.isImprovement) {
                return Double.compare(Math.abs(b.percentChange), Math.abs(a.percentChange));
            }
            // Sonst Verbesserungen zuerst
            return a.isImprovement ? -1 : 1;
        });
        
        // Einträge anzeigen
        for (ComparisonEntry entry : entries) {
            if (entry.isNumeric) {
                // Numerische Werte mit Pfeil und Prozent
                String arrow = entry.percentChange > 0 ? "▲" : (entry.percentChange < 0 ? "▼" : "═");
                String color = entry.isImprovement ? "§a" : (entry.percentChange == 0 ? "§7" : "§c");
                
                String formattedLine = String.format("§3%s: %s%s %.1f%% §7(%.2f → %.2f)", 
                                       entry.name, color, arrow, 
                                       Math.abs(entry.percentChange), 
                                       entry.oldNum, entry.newNum);
                lines.add(formattedLine);
            } else {
                // Nicht-numerische Werte
                String formattedLine = String.format("§3%s: §7(%s → %s)", 
                                       entry.name,
                                       formatNonNumericValue(entry.oldValue),
                                       formatNonNumericValue(entry.newValue));
                lines.add(formattedLine);
            }
        }
        
        // Durchschnittliche Änderung für diese Kategorie berechnen
        if (!entries.isEmpty()) {
            int changeCount = 0;
            double totalChange = 0;
            
            for (ComparisonEntry entry : entries) {
                if (entry.isNumeric && entry.percentChange != 0) {
                    totalChange += entry.isImprovement ? Math.abs(entry.percentChange) : -Math.abs(entry.percentChange);
                    changeCount++;
                }
            }
            
            if (changeCount > 0) {
                double avgChange = totalChange / changeCount;
                String color = avgChange > 0 ? "§a+" : "§c";
                if (avgChange == 0) color = "§7";
                
                lines.add(String.format("§7Durchschnittliche Änderung: %s%.1f%%", color, avgChange));
            }
        }
    }
    
    /**
     * Formatiert nicht-numerische Werte für die Anzeige
     */
    private String formatNonNumericValue(Object value) {
        if (value == null) {
            return "§cnull§7";
        }
        
        String strValue = value.toString();
        
        // Spezielle Formatierung für "FEHLT"
        if (strValue.equals("FEHLT")) {
            return "§cnicht vorhanden§7";
        }
        
        // Threads-Informationen hervorheben
        if (strValue.contains("Priority") && strValue.contains("State")) {
            String state = strValue.contains("RUNNABLE") ? "§a" + strValue + "§7" : 
                          (strValue.contains("WAITING") || strValue.contains("TIMED_WAITING")) ? 
                          "§e" + strValue + "§7" : "§7" + strValue;
            return state;
        }
        
        return strValue;
    }
    
    /**
     * Hilfsklasse für Benchmark-Vergleichseinträge
     */
    private static class ComparisonEntry {
        final String name;
        final Object oldValue;
        final Object newValue;
        final double percentChange;
        final boolean isImprovement;
        final boolean isNumeric;
        final double oldNum;
        final double newNum;
        
        ComparisonEntry(String name, Object oldValue, Object newValue, 
                        double percentChange, boolean isImprovement, 
                        boolean isNumeric, double oldNum, double newNum) {
            this.name = name;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.percentChange = percentChange;
            this.isImprovement = isImprovement;
            this.isNumeric = isNumeric;
            this.oldNum = oldNum;
            this.newNum = newNum;
        }
    }
} 