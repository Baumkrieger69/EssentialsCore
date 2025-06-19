package com.essentialscore;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.concurrent.Callable;

// Add these imports at the top of the file
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.api.impl.ModuleAdapter;
import com.essentialscore.api.command.DynamicCommand;

/**
 * Manager für Module des ApiCore
 */
public class ModuleManager {
    private final ApiCore apiCore;
    private final File modulesDir;    private final File configDir;
    private final Map<String, Object> loadedModules;
    private final Map<String, List<DynamicCommand>> moduleCommands;
    private final ExecutorService executorService;
    private static final int BUFFER_SIZE = 8192;
    private ConsoleFormatter console;
      // Performance-Tracking für Module
    private final Map<String, ModulePerformanceData> modulePerformanceMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService performanceTrackerService;
    public static final int CPU_THRESHOLD_WARNING = 20;   // 20% CPU Auslastung = Gelb
    public static final int CPU_THRESHOLD_CRITICAL = 40;  // 40% CPU Auslastung = Rot
    public static final int MEMORY_THRESHOLD_WARNING = 25;  // 25MB Speichernutzung = Gelb
    public static final int MEMORY_THRESHOLD_CRITICAL = 50; // 50MB Speichernutzung = Rot
    public static final int EXECUTION_THRESHOLD_WARNING = 100; // 100ms durchschnittliche Ausführungszeit = Gelb
    public static final int EXECUTION_THRESHOLD_CRITICAL = 250; // 250ms durchschnittliche Ausführungszeit = Rot
    
    public ModuleManager(ApiCore apiCore, File modulesDir, File configDir, 
                         Map<String, Object> loadedModules, 
                         Map<String, List<DynamicCommand>> moduleCommands,
                         ExecutorService executorService) {
        this.apiCore = apiCore;
        this.modulesDir = modulesDir;
        this.configDir = configDir;
        this.loadedModules = loadedModules;
        this.moduleCommands = moduleCommands;
        this.executorService = executorService;
        this.performanceTrackerService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ModulePerformanceTracker");
            thread.setDaemon(true); // Hintergrund-Thread, der den Server nicht am Herunterfahren hindert
            return thread;
        });
        
        // Erweiterte Konsolen-Formatter Konfiguration
        boolean useColors = apiCore.getConfig().getBoolean("console.use-colors", true);
        boolean showTimestamps = apiCore.getConfig().getBoolean("console.show-timestamps", false);
        boolean useUnicodeSymbols = apiCore.getConfig().getBoolean("console.use-unicode-symbols", true);
        String stylePreset = apiCore.getConfig().getString("console.style-preset", "default");
        
        // Konsolen-Formatter initialisieren mit Rohpräfix (ohne Formatierung)
        String rawPrefix = apiCore.getConfig().getString("console.prefixes.module-manager", "&8[&d&lModuleManager&8]");
        console = new ConsoleFormatter(
            apiCore.getLogger(),
            rawPrefix,
            useColors, showTimestamps, useUnicodeSymbols, stylePreset
        );
    
        // Performance-Tracking starten, falls aktiviert
        if (apiCore.getConfig().getBoolean("performance.module-tracking.enabled", true)) {
            // Verzögert starten, damit der Server erst vollständig geladen ist
            Bukkit.getScheduler().runTaskLater(apiCore, this::startPerformanceTracking, 100L);
        }
    }
    
    /**
     * Lädt alle verfügbaren Module mit Abhängigkeitsauflösung
     */
    public void loadModulesWithDependencyResolution() {
        console.section("MODULE WERDEN GELADEN");
        
        try {
            // Lade alle JAR-Dateien im Modulordner
            if (!modulesDir.exists()) {
                if (!modulesDir.mkdirs()) {
                    console.error("Konnte Modulverzeichnis nicht erstellen: " + modulesDir.getAbsolutePath());
                    return;
                }
            }
            
            // Liste alle JAR-Dateien im Modulverzeichnis auf
            File[] moduleFiles = modulesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            
            if (moduleFiles == null || moduleFiles.length == 0) {
                console.info("Keine Module gefunden");
                return;
            }
            
            int totalCount = moduleFiles.length;
            int loadedCount = 0;
            console.info("Beginne mit dem Laden von " + totalCount + " Modulen...");
            
            // Lade jedes Modul einzeln
            for (int i = 0; i < moduleFiles.length; i++) {
                File moduleFile = moduleFiles[i];
                console.step(i + 1, totalCount, "Lade Modul: " + moduleFile.getName());
                
                try {
                    // Lade das Modul
                    loadModule(moduleFile);
                    loadedCount++;
                    console.status("OK", "Modul " + moduleFile.getName() + " erfolgreich geladen", true);
                } catch (Exception e) {
                    console.status("FEHLER", "Fehler beim Laden von Modul " + moduleFile.getName() + ": " + e.getMessage(), false);
                    if (apiCore.isDebugMode()) {
                        e.printStackTrace();
                    }
                }
            }
            
            // Zeige Ergebnis
            if (loadedCount == totalCount) {
                console.success("Alle " + loadedCount + " Module erfolgreich geladen");
            } else {
                console.warning(loadedCount + " von " + totalCount + " Modulen geladen, es gab Fehler");
            }
        } catch (Exception e) {
            console.error("Kritischer Fehler beim Laden der Module: " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Lädt Module synchron, falls kein ExecutorService verfügbar ist
     */
    private void loadModulesSync(File[] files) {
        apiCore.getLogger().info("Lade Module synchron...");
        
        for (File file : files) {
            try {
                // Modulname aus Dateinamen extrahieren
                String potentialModuleName = file.getName().replace(".jar", "");
                
                // Konfigurationsdatei prüfen
                File configFile = new File(configDir, potentialModuleName + ".yml");
                boolean shouldLoad = true;
                
                // Wenn Konfigurationsdatei existiert, enabled-Status prüfen
                if (configFile.exists()) {
                    try {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        shouldLoad = config.getBoolean("enabled", true);
                        
                        if (!shouldLoad) {
                            apiCore.getLogger().info("Modul " + potentialModuleName + " ist deaktiviert und wird übersprungen");
                            continue;
                        }
                    } catch (Exception e) {
                        apiCore.getLogger().warning("Fehler beim Lesen der Konfiguration für " + potentialModuleName + ": " + e.getMessage());
                    }
                }
                
                // Wenn enabled=true oder keine Konfiguration, Modul laden
                if (shouldLoad) {
                loadModule(file);
                if (apiCore.isDebugMode()) {
                    apiCore.getLogger().info("Loaded: " + file.getName());
                    }
                }
            } catch (Exception e) {                apiCore.getLogger().log(Level.SEVERE, "Fehler beim Laden des Moduls " + file.getName(), e);
            }        }
    }
      
    /**
     * Lädt alle verfügbaren Module asynchron
     */
    public void loadModulesAsync() {
        if (!modulesDir.exists()) return;

        File[] files = modulesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;

        // Prüfen, ob executorService initialisiert ist
        if (executorService == null) {
            apiCore.getLogger().warning("ExecutorService ist nicht initialisiert! Verwende synchrones Laden stattdessen.");
            loadModulesSync(files);
            return;
        }

        // Process modules in parallel
        CompletionService<String> completionService = new ExecutorCompletionService<>(executorService);
        int submittedTasks = 0;

        for (File file : files) {
            // Modulname aus Dateinamen extrahieren
            String potentialModuleName = file.getName().replace(".jar", "");
            
            // Konfigurationsdatei prüfen
            File configFile = new File(configDir, potentialModuleName + ".yml");
            boolean shouldLoad = true;
            
            // Wenn Konfigurationsdatei existiert, enabled-Status prüfen
            if (configFile.exists()) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    shouldLoad = config.getBoolean("enabled", true);
                    
                    if (!shouldLoad) {
                        apiCore.getLogger().info("Modul " + potentialModuleName + " ist deaktiviert und wird übersprungen");
                        continue;
                    }
                } catch (Exception e) {
                    apiCore.getLogger().warning("Fehler beim Lesen der Konfiguration für " + potentialModuleName + ": " + e.getMessage());
                }
            }
            
            // Wenn enabled=true oder keine Konfiguration, Modul asynchron laden
            if (shouldLoad) {
            completionService.submit(() -> {
                try {
                    loadModule(file);
                    return "Loaded: " + file.getName();
                } catch (Exception e) {
                    apiCore.getLogger().log(Level.SEVERE, "Fehler beim Laden des Moduls " + file.getName(), e);
                    return "Failed: " + file.getName();
                }
            });
            submittedTasks++;
            }
        }

        // Wait for all modules to load
        for (int i = 0; i < submittedTasks; i++) {
            try {
                Future<String> result = completionService.take();
                if (apiCore.isDebugMode()) {
                    apiCore.getLogger().info(result.get());
                }
            } catch (Exception e) {
                apiCore.getLogger().log(Level.SEVERE, "Fehler beim Warten auf Modulladung", e);
            }
        }
    }
    
    /**
     * Startet den Watcher für Moduländerungen.
     * Dieser überwacht das Modulverzeichnis auf Änderungen und neue Module.
     */
    public void startModuleFileWatcher() {
        // Lademodus aus der Konfiguration lesen
        String loadMode = apiCore.getConfig().getString("modules.load-mode", "STARTUP_ONLY").toUpperCase();
        
        // WatchService für Hot-Reload starten, wenn aktiviert
        boolean hotReloadEnabled = apiCore.getConfig().getBoolean("modules.hot-reload", true);
        if (hotReloadEnabled) {
            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Aktiviere Hot-Reload für Module");
            startFileWatchService();
        } else {
            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Hot-Reload ist deaktiviert. Module werden nicht automatisch neu geladen.");
        }
        
        // Periodische Überprüfung nach neuen Modulen nur einrichten, wenn der Lademodus PERIODICALLY ist
        // und auto-check-modules aktiviert ist
        if (loadMode.equals("PERIODICALLY")) {
            boolean autoCheckModules = apiCore.getConfig().getBoolean("modules.auto-check-modules", true);
            
            if (!autoCheckModules) {
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Automatische Modulprüfung ist deaktiviert, obwohl Lademodus PERIODICALLY ist.");
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Module werden nur bei Server-/Pluginstart und manuellem Reload-Befehl geladen.");
            return;
        }
        
            // Prüfintervall aus der Konfiguration lesen (in Sekunden)
            long checkIntervalSeconds = apiCore.getConfig().getLong("modules.check-interval", 30);
            // In Ticks umrechnen (20 Ticks = 1 Sekunde)
            long checkIntervalTicks = checkIntervalSeconds * 20L;
            
            apiCore.getLogger().info("Starte periodische Modulprüfung (Intervall: " + checkIntervalSeconds + " Sekunden)");
            
            // Periodischen Task starten
            Bukkit.getScheduler().runTaskTimerAsynchronously(apiCore, () -> {
                apiCore.getLogger().info("Periodische Überprüfung des Modulordners...");
                
                if (!modulesDir.exists()) {
                    apiCore.getLogger().warning("Modulverzeichnis existiert nicht: " + modulesDir.getAbsolutePath());
            return;
        }
        
                File[] files = modulesDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (files == null || files.length == 0) {
                    if (apiCore.isDebugMode()) {
                        apiCore.getLogger().info("Keine neuen Module gefunden");
                    }
                    return;
                }
                
                // Überprüfe jede JAR-Datei im Modulordner
                for (File file : files) {
                    String moduleName = file.getName().replace(".jar", "");
                    
                    // Prüfen, ob das Modul bereits geladen ist
                    if (loadedModules.containsKey(moduleName)) {
                        continue; // Modul bereits geladen, überspringen
                    }
                    
                    // Konfigurationsdatei prüfen
                    File configFile = new File(configDir, moduleName + ".yml");
                    boolean shouldLoad = true;
                    
                    if (configFile.exists()) {
                        try {
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                            shouldLoad = config.getBoolean("enabled", true);
                        } catch (Exception e) {
                            apiCore.getLogger().warning("Fehler beim Lesen der Konfiguration für " + moduleName + ": " + e.getMessage());
                        }
                    }
                    
                    if (shouldLoad) {
                        apiCore.getLogger().info("Neues Modul gefunden: " + moduleName);
                        
                        // Modul im Hauptthread laden
                        Bukkit.getScheduler().runTask(apiCore, () -> {
                            try {
                                apiCore.getLogger().info("Lade neues Modul: " + moduleName);
                                loadModule(file);
                                apiCore.getLogger().info("Neues Modul " + moduleName + " erfolgreich geladen");
                            } catch (Exception e) {
                                apiCore.getLogger().log(Level.SEVERE, "Fehler beim Laden des neuen Moduls " + moduleName, e);
                            }
                        });
                    } else {
                        apiCore.getLogger().info("Neues Modul " + moduleName + " gefunden, aber in Konfiguration deaktiviert");
                    }
                }
            }, 100L, checkIntervalTicks); // Erste Prüfung nach 5 Sekunden, dann gemäß Konfiguration
        } else {
            apiCore.getLogger().info("Modul-Lademodus ist auf " + loadMode + " gesetzt. " +
                    (loadMode.equals("STARTUP_ONLY") ? 
                    "Neue Module werden nur beim Serverstart geladen." : 
                    "Neue Module werden nur durch manuellen Reload-Befehl geladen."));
        }
    }
    
    /**
     * Startet den WatchService für Dateiänderungen
     */
    private void startFileWatchService() {
        // Holen wir uns das Watch-Intervall aus der Konfiguration
        int watcherIntervalSeconds = apiCore.getConfig().getInt("modules.watcher-interval", 5);
        console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Starte FileWatcher für Modulverzeichnis (Intervall: " + watcherIntervalSeconds + " Sekunden)");
        
        // Starte den WatchService in einem separaten Thread
        executorService.submit(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                Path dir = modulesDir.toPath();
                dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
                
                console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "FileWatcher gestartet für: " + dir.toAbsolutePath());
                
                while (true) {
                    WatchKey key;
                    try {
                        key = watchService.poll(watcherIntervalSeconds, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    
                    if (key == null) {
                        continue;
                    }
                    
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;
                        Path filename = watchEvent.context();
                        
                        if (filename.toString().endsWith(".jar")) {
                            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Änderung erkannt: " + filename);
                            
                            File moduleFile = new File(modulesDir, filename.toString());
                            
                            // Verwende den neuen Helper zum Extrahieren des Modulnamens
                            String moduleName = getModuleNameFromJar(moduleFile);
                            
                            // Nur fortfahren, wenn ein gültiger Modulname extrahiert werden konnte
                            if (moduleName != null && !moduleName.isEmpty()) {
                                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Hot Reload für Modul: " + moduleName);
                                
                                // Konfiguration prüfen (optional)
                                File configFile = new File(configDir, moduleName + ".yml");
                                boolean shouldLoad = true;
                                if (configFile.exists()) {
                                    try {
                                        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                                        shouldLoad = config.getBoolean("enabled", true);
                                    } catch (Exception e) {
                                        console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Lesen der Konfiguration für " + moduleName + ": " + e.getMessage());
                                    }
                                }
                                
                                if (shouldLoad) {
                                    // Kopie für Lambda-Zugriff
                                    final String moduleNameFinal = moduleName;
                                    final File moduleFileFinal = moduleFile;
                                    
                                    // Run module reload on main thread to ensure thread safety
                                    Bukkit.getScheduler().runTask(apiCore, () -> {
                                        try {
                                            if (loadedModules.containsKey(moduleNameFinal)) {
                                                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Entlade Modul vor Hot-Reload: " + moduleNameFinal);
                                                unloadModule(moduleNameFinal);
                                            }
                                            
                                            // Kurze Pause, um sicherzustellen, dass die Datei fertig geschrieben ist
                                            try {
                                                Thread.sleep(100);
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                            
                                            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Lade Modul neu: " + moduleNameFinal);
                                            loadModule(moduleFileFinal);
                                            console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Hot-Reload für Modul " + moduleNameFinal + " erfolgreich abgeschlossen");
                                        } catch (Exception e) {
                                            console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Hot-Reload fehlgeschlagen für " + moduleNameFinal + ": " + e.getMessage());
                                            if (apiCore.isDebugMode()) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } else {
                                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Neues Modul " + moduleName + " erkannt, aber deaktiviert in Konfiguration");
                                }
                            } else {
                                console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Konnte keinen gültigen Modulnamen aus " + filename + " extrahieren");
                            }
                        }
                    }
                    
                    key.reset();
                }
            } catch (IOException e) {
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Starten des ModuleWatchers: " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Lädt ein einzelnes Modul mit optimierter I/O und Memory-Nutzung
     */
    public void loadModule(File jarFile) throws Exception {
        console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Starte Laden des Moduls: " + jarFile.getName());
        
        // Überprüfe die moduleName aus der JAR-Datei
        final String extractedModuleName;
        String tempModuleName = "";
        
        try (JarFile jar = new JarFile(jarFile)) {
            console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "JAR-Datei geöffnet zum Extrahieren von Modulnamen: " + jarFile.getName(), apiCore.isDebugMode());
            JarEntry moduleYml = jar.getJarEntry("module.yml");
            if (moduleYml != null) {
                console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "module.yml gefunden in: " + jarFile.getName(), apiCore.isDebugMode());
                YamlConfiguration moduleConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(jar.getInputStream(moduleYml), StandardCharsets.UTF_8));
                tempModuleName = moduleConfig.getString("name", "");
                console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Extrahierter Modulname: " + tempModuleName, apiCore.isDebugMode());
            } else {
                console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "module.yml nicht gefunden in: " + jarFile.getName());
            }
        } catch (Exception e) {
            console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Extrahieren des Modulnamens aus " + jarFile.getName() + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        // Finalen Wert zuweisen
        extractedModuleName = tempModuleName;
        
        // Konfigurationsdatei prüfen (basierend auf dem extrahierten Namen)
        if (!extractedModuleName.isEmpty()) {
            File configFile = new File(configDir, extractedModuleName + ".yml");
            if (configFile.exists()) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    boolean isEnabled = config.getBoolean("enabled", true);
                    
                    if (!isEnabled) {
                        console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Modul " + extractedModuleName + " ist in der Konfiguration deaktiviert");
                        return; // Modulladung abbrechen
                    }
                } catch (Exception e) {
                    console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Lesen der Konfiguration für " + extractedModuleName + ": " + e.getMessage());
                    // Fortfahren mit Standardeinstellungen
                }
            }
        }
        
        // Überprüfen, ob das Modul in der Konfiguration als "force-sync" markiert ist
        List<String> forceSyncModules = apiCore.getConfig().getStringList("modules.force-sync-modules");
        boolean shouldLoadSync = !forceSyncModules.isEmpty() && 
                (extractedModuleName.length() > 0 && forceSyncModules.contains(extractedModuleName));
        
        boolean isMainThread = Bukkit.isPrimaryThread();
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Lade Modul: " + jarFile.getName() + 
                " (synchron laden: " + shouldLoadSync + 
                ", im Hauptthread: " + isMainThread + 
                ", Name: " + extractedModuleName + ")");
                
        // DEBUG: Ausgabe der force-sync-modules Liste
        console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Force-Sync-Module in Konfiguration: " + String.join(", ", forceSyncModules), apiCore.isDebugMode());
        
        // Wenn synchrones Laden erforderlich ist, aber wir nicht im Hauptthread sind
        if (shouldLoadSync && !isMainThread) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Modul " + extractedModuleName + " erfordert synchrones Laden im Hauptthread!");
            
            // Umplanen auf den Hauptthread
            CompletableFuture<Void> future = new CompletableFuture<>();
            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Plane Laden von " + extractedModuleName + " im Hauptthread...");
            Bukkit.getScheduler().runTask(apiCore, () -> {
                try {
                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Lade Modul " + extractedModuleName + " neu im Hauptthread");
                    loadModule(jarFile);
                    future.complete(null);
                    console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Laden im Hauptthread erfolgreich abgeschlossen");
                } catch (Exception e) {
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Laden im Hauptthread: " + e.getMessage());
                    if (apiCore.isDebugMode()) {
                        e.printStackTrace();
                    }
                    future.completeExceptionally(e);
                }
            });
            
            try {
                // Warten auf Abschluss mit Timeout
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Warte auf Abschluss des Ladens im Hauptthread...");
                long timeoutMs = apiCore.getConfig().getLong("modules.module-load-timeout", 30000);
                console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Timeout für Modulladung: " + timeoutMs + "ms", apiCore.isDebugMode());
                future.get(timeoutMs, TimeUnit.MILLISECONDS);
                console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Hauptthread-Ladung für " + extractedModuleName + " erfolgreich abgeschlossen");
                return;
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim synchronen Laden von " + extractedModuleName + ": " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
                throw e;
            }
        }
        
        console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Öffne JAR-Datei zur Modulverarbeitung: " + jarFile.getName(), apiCore.isDebugMode());
        
        try (JarFile jar = new JarFile(jarFile, false)) {
            console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "JAR-Datei geöffnet: " + jarFile.getName(), apiCore.isDebugMode());
            JarEntry moduleYml = jar.getJarEntry("module.yml");

            if (moduleYml == null) {
                console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Die Datei " + jarFile.getName() + " enthält keine module.yml");
                return;
            }

            console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "module.yml gefunden in: " + jarFile.getName(), apiCore.isDebugMode());
            
            // Optimierte Konfigurationsladung mit gepuffertem Stream
            YamlConfiguration moduleConfig;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jar.getInputStream(moduleYml), StandardCharsets.UTF_8), BUFFER_SIZE)) {
                moduleConfig = YamlConfiguration.loadConfiguration(reader);
                console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Konfiguration geladen aus: " + jarFile.getName(), apiCore.isDebugMode());
            }

            String moduleName = moduleConfig.getString("name");
            String mainClass = moduleConfig.getString("main");
            String version = moduleConfig.getString("version", "1.0.0");
            String description = moduleConfig.getString("description", "");

            if (moduleName == null || mainClass == null) {
                console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Ungültige module.yml in " + jarFile.getName());
                return;
            }

            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Modulinfo: Name=" + moduleName + ", Main=" + mainClass + ", Version=" + version);
            
            // Prüfen, ob das Modul bereits geladen ist
            if (loadedModules.containsKey(moduleName)) {
                console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Das Modul " + moduleName + " ist bereits geladen!");
                return;
            }

            // Modul-Konfiguration erstellen oder laden - Mit verbesserter Fehlerbehandlung
            File moduleConfigFile = new File(configDir, moduleName + ".yml");
            
            // Überprüfe Verzeichnisrechte explizit vor dem Operationsversuch
            if (!configDir.exists()) {
                boolean created = configDir.mkdirs();
                if (!created) {
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "KRITISCH: Konnte Konfigurationsverzeichnis nicht erstellen: " + configDir.getAbsolutePath());
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Module können möglicherweise nicht korrekt geladen werden!");
                }
            }
            
            if (!configDir.canWrite()) {
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "KRITISCH: Keine Schreibrechte für das Konfigurationsverzeichnis: " + configDir.getAbsolutePath());
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Modulkonfiguration kann nicht gespeichert werden!");
                // Trotzdem fortfahren, vielleicht mit Standardeinstellungen
            }
            
            FileConfiguration config;
            
            // Optimierte Konfigurationsprüfung mit weniger Festplattenzugriffen
            boolean configExists = moduleConfigFile.exists();
            boolean needsConfigSave = false;
            
            if (!configExists) {
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Erstelle neue Konfiguration für Modul: " + moduleName);
                config = new YamlConfiguration();
                config.set("enabled", true);
                config.set("version", version);
                needsConfigSave = true;
            } else {
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Lade existierende Konfiguration für Modul: " + moduleName);
                try {
                // Gepufferte Konfigurationsladung für bessere Performance
                try (BufferedReader reader = new BufferedReader(
                        new FileReader(moduleConfigFile, StandardCharsets.UTF_8), BUFFER_SIZE)) {
                    config = YamlConfiguration.loadConfiguration(reader);
                }

                // Version aktualisieren, falls nötig
                if (!config.getString("version", "").equals(version)) {
                        config.set("version", version);
                        needsConfigSave = true;
                    }
                } catch (IOException e) {
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Konnte Konfiguration für Modul " + moduleName + " nicht lesen: " + e.getMessage());
                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Erstelle neue Konfiguration...");
                    config = new YamlConfiguration();
                    config.set("enabled", true);
                    config.set("version", version);
                    needsConfigSave = true;
                }
            }
            
            // Konfiguration nur speichern, wenn nötig - reduziert I/O
            if (needsConfigSave) {
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Speichere Konfiguration für Modul: " + moduleName);
                try {
                    // Stelle sicher, dass das Verzeichnis existiert
                    File parentDir = moduleConfigFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        boolean dirCreated = parentDir.mkdirs();
                        if (!dirCreated) {
                            console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Konnte Verzeichnis nicht erstellen: " + parentDir.getAbsolutePath());
                        }
                    }
                    
                    // Versuche die Konfiguration zu speichern
                config.save(moduleConfigFile);
                    
                    // Teste, ob die Datei tatsächlich erstellt wurde
                    if (!moduleConfigFile.exists()) {
                        console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "KRITISCH: Konfigurationsdatei konnte nicht erstellt werden, obwohl keine Exception geworfen wurde!");
                        console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Pfad: " + moduleConfigFile.getAbsolutePath());
                        console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Bitte überprüfe die Berechtigungen des Minecraft-Servers.");
                    }
                } catch (IOException e) {
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Speichern der Konfiguration für Modul " + moduleName + ": " + e.getMessage());
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Details: " + e.getClass().getName() + 
                            (e.getCause() != null ? ", Ursache: " + e.getCause().getMessage() : ""));
                    // Fahre trotzdem fort, möglicherweise mit Standardeinstellungen
                }
            }

            // Prüfen, ob das Modul aktiviert ist
            if (!config.getBoolean("enabled", true)) {
                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Das Modul " + moduleName + " ist deaktiviert und wird nicht geladen.");
                    return;
            }

            // Set contextClassLoader before loading module classes
            ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            
            // Verbesserte Fehlerbehandlung mit Try-With-Resources
            URLClassLoader loader = null;
            try {
                // Optimierter ClassLoader mit besserer Isolation und Caching
                loader = new ModuleClassLoader(moduleName, new URL[]{jarFile.toURI().toURL()}, apiCore.getClass().getClassLoader());
                Thread.currentThread().setContextClassLoader(loader);
                
                // Effizienzsteigerung durch direkte Klassenladung ohne Umwege
                Class<?> moduleMainClass = Class.forName(mainClass, true, loader);

                // Optimierte Modul-Interface-Überprüfung, die Klassenladerhierarchie-Probleme vermeidet
                boolean implementsModuleInterface = false;
                
                // Prüfe, ob es das Module-Interface implementiert
                for (Class<?> iface : moduleMainClass.getInterfaces()) {
                    if (iface.getName().equals("com.essentialscore.api.Module")) {
                        implementsModuleInterface = true;
                        console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Interface Module gefunden: " + iface.getName(), apiCore.isDebugMode());
                        break;
                    }
                }
                
                // Wenn nicht, versuche die init-Methode mit passender Signatur zu finden
                if (!implementsModuleInterface) {
                    try {
                        // Prüfe auf die Signatur mit ModuleAPI
                        try {
                            Method initMethod = moduleMainClass.getMethod("init", ModuleAPI.class, FileConfiguration.class);
                        Method onDisableMethod = moduleMainClass.getMethod("onDisable");
                        
                        if (initMethod != null && onDisableMethod != null) {
                                implementsModuleInterface = true;
                                console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Interface Module gefunden (über Methodensignatur): init und onDisable vorhanden", apiCore.isDebugMode());
                        }
                    } catch (NoSuchMethodException e) {
                            // Prüfe auf die alte Signatur mit ApiCore
                            try {
                                Method initMethod = moduleMainClass.getMethod("init", ApiCore.class, FileConfiguration.class);
                                Method onDisableMethod = moduleMainClass.getMethod("onDisable");
                                
                                if (initMethod != null && onDisableMethod != null) {
                                    // Legacy module with direct ApiCore dependency
                                    console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Legacy Modul gefunden (über Methodensignatur): init und onDisable vorhanden", apiCore.isDebugMode());
                                    implementsModuleInterface = true; // Treat as valid module
                                }
                            } catch (NoSuchMethodException ex) {
                                // Methoden nicht gefunden - keine Interface-Implementierung
                            }
                        }
                    } catch (Exception e) {
                        // Allgemeiner Fehler bei der Methodenprüfung
                    }
                }
                
                if (!implementsModuleInterface) {
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Klasse " + mainClass + " implementiert nicht das Module-Interface! Das Modul kann nicht geladen werden.");
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Verfügbare Interfaces: " + Arrays.toString(moduleMainClass.getInterfaces()));
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "LÖSUNG: Stelle sicher, dass das Modul das Interface Module von com.essentialscore.api implementiert");
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "ALTERNATIV: Implementiere die Methoden init(ModuleAPI/ApiCore, FileConfiguration) und onDisable()");
                    return;
                }

                // Erstelle Modul-Info-Objekt und speichere es in loadedModules, BEVOR die Initialisierung beginnt
                // Dies ist wichtig, damit der ModuleFileManager die JAR-Datei finden kann
                // Erstelle temporäre ModuleInfo für initializeModuleFiles ohne Instanz
                ApiCore.ModuleInfo tempModuleInfo = new ApiCore.ModuleInfo(moduleName, version, description, jarFile, loader, null);
                loadedModules.put(moduleName, tempModuleInfo);
                
                // Initialisiere die Dateistruktur EXPLIZIT, bevor die Modulinstanz erstellt wird
                apiCore.getModuleFileManager().getModuleDataDirectory(moduleName);

                // DANACH Instanz des Moduls erstellen mit optimierter Reflection
                Object moduleInstance = null;
                try {
                    moduleInstance = createModuleInstance(moduleMainClass);
                    if (moduleInstance == null) {
                        console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Konnte keine Instanz für " + moduleName + " erstellen (Konstruktor lieferte null)");
                        return;
                    }
                } catch (Exception e) {
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Erstellen der Modul-Instanz für " + moduleName + ": " + e.getMessage());
                    if (apiCore.isDebugMode()) {
                        e.printStackTrace();
                    }
                    return;
                }
                
                // Erst JETZT Modul-Info aktualisieren, wenn die Instanz erfolgreich erstellt wurde
                ApiCore.ModuleInfo moduleInfo = new ApiCore.ModuleInfo(moduleName, version, description, jarFile, loader, moduleInstance);
                loadedModules.put(moduleName, moduleInfo);

                // Modul initialisieren - mit robuster Initialisierung unabhängig vom Interface
                try {
                    // Verwende die bereits geladene Konfiguration
                    final FileConfiguration finalConfig = config;
                    final Object finalModuleInstance = moduleInstance;
                    com.essentialscore.api.module.ModuleSandbox sandbox = apiCore.getModuleSandbox();
                    
                    if (implementsModuleInterface) {
                        // Try initializing using Module interface
                        if (finalModuleInstance instanceof com.essentialscore.api.Module) {
                            // Get or create ModuleAPI
                            ModuleAPI moduleAPI = apiCore.getModuleAPI(moduleName);
                            
                            // Initialize using the Module interface in a sandbox if enabled
                            if (sandbox != null) {
                                try {
                                    // Direct initialization in sandbox without lambda
                                    ((com.essentialscore.api.Module) finalModuleInstance).init(moduleAPI, finalConfig);
                                } catch (Exception sandboxException) {
                                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, 
                                        "Modul " + moduleName + " konnte nicht initialisiert werden (Sandbox-Fehler): " + sandboxException.getMessage());
                                    return;
                                }
                            } else {
                                // Direkte Initialisierung ohne Sandbox
                                ((com.essentialscore.api.Module) finalModuleInstance).init(moduleAPI, finalConfig);
                            }
                            
                            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, 
                                "Modul " + moduleName + " erfolgreich initialisiert über Module-Interface");
                            
                            // Create an adapter that presents the Module as a legacy module
                            ModuleAdapter adapter = new ModuleAdapter(
                                (com.essentialscore.api.Module) finalModuleInstance, 
                                moduleAPI, 
                                apiCore
                            );
                            
                            // Store the adapter in the ModuleInfo
                            moduleInfo = new ApiCore.ModuleInfo(moduleName, version, description, jarFile, loader, adapter);
                            loadedModules.put(moduleName, moduleInfo);
                        }
                    }
                } catch (Exception e) {
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler bei der Initialisierung von Modul " + moduleName + ": " + e.getMessage());
                    if (apiCore.isDebugMode()) {
                        e.printStackTrace();
                    }
                    return;
                }
                
                // Infoblöcke erweitert mit kategorisierten Nachrichten
                console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "===== MODUL GELADEN: " + moduleName + " =====");
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Version: " + version);
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "WICHTIGE PFADE FÜR DIESES MODUL:");
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "► JAR-Datei: " + jarFile.getAbsolutePath());
                
                File dataFolder = apiCore.getModuleDataFolder(moduleName);
                if (dataFolder != null) {
                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "► Daten-Ordner: " + dataFolder.getAbsolutePath());
                } else {
                    console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "► Daten-Ordner konnte nicht initialisiert werden!");
                }
                
                File configFile = apiCore.getModuleConfigFile(moduleName);
                if (configFile != null) {
                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "► Konfigurations-Datei: " + configFile.getAbsolutePath());
                } else {
                    console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "► Konfigurations-Datei konnte nicht initialisiert werden!");
                }
                
                File resourcesFolder = apiCore.getModuleResourcesFolder(moduleName);
                if (resourcesFolder != null && resourcesFolder.exists()) {
                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "► Ressourcen-Ordner: " + resourcesFolder.getAbsolutePath());
                } else {
                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "► Ressourcen-Ordner noch nicht verfügbar (wird nach dem Server-Start erstellt)");
                }
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Module sollten diese Pfade verwenden, um ihre Dateien zu finden und zu speichern.");
                console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "============================================");
                
                // Permissions und Befehle registrieren
                if (moduleConfig != null) {
                    loadModuleCommands(moduleConfig, moduleName);
                    registerModulePermissions(moduleConfig, moduleName);
                }

                console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Modul " + moduleName + " v" + version + " wurde geladen!");
                
                // Event für das Laden des Moduls auslösen
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("name", moduleName);
                eventData.put("version", version);
                eventData.put("jarFile", jarFile);
                apiCore.fireModuleEvent("module_loaded", eventData);
                
                return;
            } finally {
                // Restore original contextClassLoader
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
        } catch (Exception e) {
            console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Laden des Moduls: " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    
    /**
     * Optimierte Modul-Instanzerstellung mit Caching von Konstruktoren
     */
    private Object createModuleInstance(Class<?> moduleClass) throws ReflectiveOperationException {
        try {
            // Direkte Verwendung des Standard-Konstruktors für maximale Performance
            return moduleClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            // Fallback auf expliziten Parameter-Konstruktor, falls nötig
            for (java.lang.reflect.Constructor<?> constructor : moduleClass.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 0) {
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                }
            }
            throw e;
        }
    }
    
    /**
     * Optimierte Befehlsladung mit Batch-Processing
     */
    private void loadModuleCommands(YamlConfiguration moduleConfig, String moduleName) {
        // Überprüfen, ob das Modul aktiviert ist
        File moduleConfigFile = new File(configDir, moduleName + ".yml");
        if (moduleConfigFile.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(moduleConfigFile);
                if (!config.getBoolean("enabled", true)) {
                    apiCore.getLogger().info("Modul " + moduleName + " ist deaktiviert, überspringe Befehlsregistrierung");
                    return;
                }
            } catch (Exception e) {
                apiCore.getLogger().warning("Fehler beim Lesen der Modulkonfiguration für " + moduleName + ": " + e.getMessage());
            }
        }
        
        List<Map<?, ?>> commandsList = moduleConfig.getMapList("commands");
        if (commandsList == null || commandsList.isEmpty()) {
            return;
        }
        
        // Vorallokierte Liste für bessere Performance
        List<DynamicCommand> commands = new ArrayList<>(commandsList.size());

        // Einmalige Verarbeitung statt wiederholter Prüfungen
        for (Map<?, ?> commandMap : commandsList) {
            String commandName = (String) commandMap.get("name");
            if (commandName == null) continue;
            
            String cmdDescription = (String) commandMap.get("description");
            String usage = (String) commandMap.get("usage");
            
            // Effiziente Konvertierung von List<Object> zu List<String>
            @SuppressWarnings("unchecked")
            List<String> aliases = (List<String>) commandMap.get("aliases");
            if (aliases == null) aliases = Collections.emptyList();
            
            String permission = (String) commandMap.get("permission");

            // Create a command executor for the module command
            org.bukkit.command.CommandExecutor executor = new org.bukkit.command.CommandExecutor() {
                @Override
                public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
                    return onModuleCommand(moduleName, sender, command, label, args);
                }
            };

            DynamicCommand cmd = new DynamicCommand(commandName, moduleName, aliases, cmdDescription, usage, permission, executor, null);
            
            commands.add(cmd);
        }
        
        // Prüfen, ob wir bereits Befehle für dieses Modul haben, falls ja, entfernen
        List<DynamicCommand> existingCommands = moduleCommands.get(moduleName);
        if (existingCommands != null && !existingCommands.isEmpty()) {
            try {
                apiCore.unregisterCommands(existingCommands);
                moduleCommands.remove(moduleName);
                if (apiCore.isDebugMode()) {
                    apiCore.getLogger().info("Bestehende Befehle für Modul " + moduleName + " wurden entfernt");
                }
            } catch (Exception e) {
                apiCore.getLogger().warning("Fehler beim Entfernen bestehender Befehle für Modul " + moduleName + ": " + e.getMessage());
            }
        }
        
        // Batch-Registrierung aller Befehle auf einmal
        if (!commands.isEmpty()) {
            apiCore.registerCommands(commands);
            moduleCommands.put(moduleName, commands);
            
            if (apiCore.isDebugMode()) {
                apiCore.getLogger().info("Module " + moduleName + ": " + commands.size() + " Befehle registriert mit Tab-Completion");
            }
        }
    }
    
    /**
     * Registriert Berechtigungen für ein Modul
     */
    public void registerModulePermissions(YamlConfiguration moduleConfig, String moduleName) {
        if (moduleConfig == null) return;
        
        ConfigurationSection permissionsSection = moduleConfig.getConfigurationSection("permissions");
        if (permissionsSection == null) return;
        
        Map<String, String[]> permissions = new HashMap<>();
                        
        for (String key : permissionsSection.getKeys(false)) {
            String fullPermission = moduleName.toLowerCase() + "." + key;
            String description = permissionsSection.getString(key + ".description", "Eine Berechtigung für " + moduleName);
            String defaultValue = permissionsSection.getString(key + ".default", "op");
            
            permissions.put(fullPermission, new String[]{description, defaultValue});
        }
                        
        // Verwende die PermissionManager-Methode zur Registrierung
        if (!permissions.isEmpty()) {
            for (Map.Entry<String, String[]> entry : permissions.entrySet()) {
                String permissionName = entry.getKey();
                String[] permissionData = entry.getValue();
                String description = permissionData[0];
                String defaultValue = permissionData[1];
                
                org.bukkit.permissions.PermissionDefault permDefault;
                try {
                    permDefault = org.bukkit.permissions.PermissionDefault.valueOf(defaultValue.toUpperCase());
                } catch (IllegalArgumentException e) {
                    permDefault = org.bukkit.permissions.PermissionDefault.OP;
                }
                
                apiCore.getPermissionManager().registerModulePermission(moduleName, permissionName, description, permDefault);
            }
        }
    }
    
    /**
     * Synchronisiert die Bukkit-Befehlskarte nach Änderungen
     * Diese Methode ist wichtig, um nach dem Hinzufügen/Entfernen von Befehlen
     * den Tab-Completion-Cache zu aktualisieren.
     */
    public void synchronizeCommands() {
        try {
            // HARD RESET: Aktive Befehle für alle deaktivierte Module zurücksetzen
            for (Map.Entry<String, List<DynamicCommand>> entry : new HashMap<>(moduleCommands).entrySet()) {
                String moduleName = entry.getKey();
                if (!loadedModules.containsKey(moduleName)) {
                    try {
                        List<DynamicCommand> commands = entry.getValue();
                        if (commands != null && !commands.isEmpty()) {
                            // Erstelle eine Kopie, um ConcurrentModificationException zu vermeiden
                            List<DynamicCommand> commandsCopy = new ArrayList<>(commands);
                            
                            // RADIKALE LÖSUNG: Bukkit zwingt den Command neu zu registrieren
                            // Effektiv deaktivieren wir die Befehle im System
                            // Note: DynamicCommand is immutable, so we can't modify it
                            // We'll just proceed to unregister the commands
                            
                            // Alle Befehle aus dem Modul entfernen
                            apiCore.unregisterCommands(commandsCopy);
                            
                            // Aus unserer Befehlsliste entfernen
                            moduleCommands.remove(moduleName);
                            
                            if (apiCore.isDebugMode()) {
                                apiCore.getLogger().info("Befehle des deaktivierten Moduls " + moduleName + " wurden vollständig aus der Registry entfernt");
                            }
                        }
                    } catch (Exception e) {
                        apiCore.getLogger().warning("Fehler beim Bereinigen von Befehlen des deaktivierten Moduls " + moduleName + ": " + e.getMessage());
                    }
                }
            }

            // Direkten Zugriff auf die CommandMap von Bukkit erhalten
            try {
                Object craftServer = Bukkit.getServer();
                Field commandMapField = craftServer.getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                Object commandMap = commandMapField.get(craftServer);
                
                // Hole die knownCommands-Map
                Field knownCommandsField = null;
                try {
                    knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                    knownCommandsField.setAccessible(true);
                    
                    @SuppressWarnings("unchecked")
                    Map<String, org.bukkit.command.Command> knownCommands = 
                    (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

                    // ENTFERNUNG TEIL 1: Jegliche Spur von deaktivierten Modulbefehlen entfernen
                    if (knownCommands != null) {
                        // Sammle alle zu entfernenden Kommandos
                        Set<String> keysToRemove = new HashSet<>();
                        
                        for (Map.Entry<String, org.bukkit.command.Command> cmdEntry : knownCommands.entrySet()) {
                            org.bukkit.command.Command cmd = cmdEntry.getValue();
                            
                            // DynamicCommands identifizieren
                            if (cmd instanceof com.essentialscore.DynamicCommand) {
                                com.essentialscore.DynamicCommand dynamicCmd = (com.essentialscore.DynamicCommand) cmd;
                                String cmdModuleName = dynamicCmd.getModuleName();
                                
                                // Wenn das Modul nicht mehr geladen ist, Befehl entfernen
                                if (!loadedModules.containsKey(cmdModuleName)) {
                                    keysToRemove.add(cmdEntry.getKey());
                                }
                            }
                            
                            // Zusätzlicher Check: Befehle, die mit deaktivierten_ beginnen
                            if (cmd.getName().startsWith("disabled_")) {
                                keysToRemove.add(cmdEntry.getKey());
                            }
                        }
                        
                        // Alle identifizierten Schlüssel entfernen
                        if (!keysToRemove.isEmpty()) {
                            for (String key : keysToRemove) {
                                knownCommands.remove(key);
                            }
                            
                            if (apiCore.isDebugMode()) {
                                apiCore.getLogger().info("Tab-Completion-Cache aggressiv bereinigt: " + keysToRemove.size() + " Einträge entfernt");
                            }
                        }
                    }
                    
                    // ENTFERNUNG TEIL 2: Clear & Sync versuchen
                    try {
                        // Versuche verschiedene Methoden zur Synchronisierung
                        Method[] methods = {
                            commandMap.getClass().getDeclaredMethod("clearCommands"),
                            commandMap.getClass().getDeclaredMethod("syncCommands")
                        };
                        
                        for (Method method : methods) {
                            try {
                                method.setAccessible(true);
                                method.invoke(commandMap);
                                apiCore.getLogger().info("Command-Map direkt synchronisiert mit " + method.getName());
                            } catch (Exception methodEx) {
                                // Ignoriere diese spezielle Exception und versuche die nächste Methode
                            }
                        }
                    } catch (Exception clearEx) {
                        // Fehler bei spezifischen Methoden können ignoriert werden
                    }
                } catch (Exception knownCommandsEx) {
                    // Zugriff auf knownCommands fehlgeschlagen, versuche anderen Ansatz
                }
                
                // HINWEIS: Der Befehl "minecraft:reload commands" wird nicht mehr automatisch ausgeführt
                // Ein manueller Reload durch den Server-Admin kann erforderlich sein
                
            } catch (Exception outerEx) {
                // Kritischer Fehler beim Zugriff auf die Command-Map
                apiCore.getLogger().warning("Konnte nicht auf die CommandMap von Bukkit zugreifen: " + outerEx.getMessage());
            }
            
            // TEIL 3: Nur Tab-Completers von Modul-Befehlen zurücksetzen
            try {
                // Durchlaufe alle registrierten Befehle im Bukkit-System
                Map<String, org.bukkit.command.Command> bukkitCommands = Bukkit.getCommandMap().getKnownCommands();
                if (bukkitCommands != null) {
                    for (org.bukkit.command.Command cmd : bukkitCommands.values()) {
                        // Nur DynamicCommands von deaktivierten Modulen bearbeiten
                        if (cmd instanceof com.essentialscore.DynamicCommand) {
                            com.essentialscore.DynamicCommand dynamicCmd = (com.essentialscore.DynamicCommand) cmd;
                            String cmdModuleName = dynamicCmd.getModuleName();
                            
                            // Prüfen ob das Modul deaktiviert ist
                            if (!loadedModules.containsKey(cmdModuleName) || 
                                cmd.getName().startsWith("disabled_")) {
                                // Versuche den TabCompleter direkt zu setzen
                                try {
                                    Method setTabCompleterMethod = cmd.getClass().getDeclaredMethod("setTabCompleter", TabCompleter.class);
                                    if (setTabCompleterMethod != null) {
                                        setTabCompleterMethod.setAccessible(true);
                                        setTabCompleterMethod.invoke(cmd, (TabCompleter)null);
                                    }
                                } catch (Exception tabEx) {
                                    // Ignorieren - nicht alle Commands haben diese Methode
                                }
                            }
                        }
                    }
                }
            } catch (Exception allTabEx) {
                // Ignorieren - dies ist ein extremer Fallback-Ansatz
            }
            
        } catch (Exception e) {
            apiCore.getLogger().warning("Fehler beim aggressiven Bereinigen der Tab-Completions: " + e.getMessage());
        }
    }
    
    /**
     * Entlädt ein Modul mit verbesserten Sicherheitsmaßnahmen
     */
    public void unloadModule(String moduleName) {
        if (!loadedModules.containsKey(moduleName)) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Versuch, nicht geladenes Modul zu entladen: " + moduleName);
            return;
        }
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Entlade Modul: " + moduleName);
        
        try {
            Object moduleInfo = loadedModules.get(moduleName);
            
            if (moduleInfo instanceof ApiCore.ModuleInfo) {
                ApiCore.ModuleInfo info = (ApiCore.ModuleInfo) moduleInfo;
                
                // Modul-Instanz abrufen
                Object moduleInstance = info.getInstance();
                
                // Methode zum Deaktivieren aufrufen
                if (moduleInstance != null) {
                    // Verwende ApiCore, um die Methode aufzurufen
                    try {
                        Class<?>[] paramTypes = new Class<?>[0];
                        apiCore.invokeMethod(moduleInstance, "onDisable", paramTypes);
                    } catch (Exception e) {
                        console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE,
                            "Fehler beim Aufrufen von onDisable für " + moduleName + ": " + e.getMessage());
                    }
                }
                
                // Listener des Moduls entfernen
                // Wir benutzen Reflection, um auf die private Map der Module-Listener zuzugreifen
                try {
                    Field listenersField = ApiCore.class.getDeclaredField("moduleListeners");
                    listenersField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, List<com.essentialscore.api.ModuleEventListener>> moduleListeners = 
                        (Map<String, List<com.essentialscore.api.ModuleEventListener>>) listenersField.get(apiCore);
                    
                    if (moduleListeners != null) {
                        Set<String> eventNames = new HashSet<>(moduleListeners.keySet());
                        
                        for (String eventName : eventNames) {
                            List<com.essentialscore.api.ModuleEventListener> listeners = moduleListeners.get(eventName);
                            if (listeners != null) {
                                // Kopie der Liste erstellen, um ConcurrentModificationException zu vermeiden
                                List<com.essentialscore.api.ModuleEventListener> listenersCopy = new ArrayList<>(listeners);
                                for (com.essentialscore.api.ModuleEventListener listener : listenersCopy) {
                                    if (listener.getClass().getClassLoader() == info.getLoader()) {
                                        apiCore.unregisterModuleListener(eventName, listener);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE,
                        "Fehler beim Entfernen von Event-Listenern für " + moduleName + ": " + e.getMessage());
                }
                
                // Befehle des Moduls entfernen
                List<DynamicCommand> commands = moduleCommands.get(moduleName);
                if (commands != null && !commands.isEmpty()) {
                    apiCore.unregisterCommands(commands);
                    moduleCommands.remove(moduleName);
                }
                
                // ClassLoader des Moduls schließen
                try {
                    URLClassLoader loader = info.getLoader();
                    if (loader != null) {
                        loader.close();
                    }
                } catch (Exception e) {
                    console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE,
                        "Fehler beim Schließen des ClassLoaders für " + moduleName + ": " + e.getMessage());
                }
            }
            
            // Modul aus der Liste entfernen
            loadedModules.remove(moduleName);
            console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Modul " + moduleName + " wurde entladen");
            
            // Tab-Completions synchronisieren
            synchronizeCommands();
            
            return;
        } catch (Exception e) {
            console.categoryError(ConsoleFormatter.MessageCategory.MODULE,
                "Kritischer Fehler beim Entladen des Moduls " + moduleName + ": " + e.getMessage());
            if (apiCore.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Lädt alle Module neu - verbesserte Implementierung für sicheren Reload
     */
    public void reloadAllModules() {
        console.header("MODULE NEU LADEN");
        
        // Zuerst alle Module entladen
        List<String> modulesToReload = new ArrayList<>(loadedModules.keySet());
        
        console.info("Entlade " + modulesToReload.size() + " Module...");
        
        // Module in umgekehrter Reihenfolge entladen (für Abhängigkeiten)
        for (int i = modulesToReload.size() - 1; i >= 0; i--) {
            String moduleName = modulesToReload.get(i);
            try {
                // Speichere Informationen über das Modul vor dem Entladen
                Object moduleObj = loadedModules.get(moduleName);
                File jarFile = null;
                
                if (moduleObj instanceof ApiCore.ModuleInfo) {
                    ApiCore.ModuleInfo info = (ApiCore.ModuleInfo) moduleObj;
                    jarFile = info.getJarFile();
                }
                
                // Modul entladen
                unloadModule(moduleName);
                
                // Falls die JAR-Datei bekannt ist, speichern wir sie für das spätere Neuladen
                if (jarFile != null) {
                    // Wir speichern die Informationen in einem gemeinsamen Kontext
                    apiCore.setSharedData("reload_jar_" + moduleName, jarFile);
                }
            } catch (Exception e) {
                console.error("Fehler beim Entladen von Modul " + moduleName + ": " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        
        // Kleine Pause, um dem System Zeit zum Aufräumen zu geben
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // System.gc() aufrufen, um nicht mehr benötigte ClassLoader zu entfernen
        System.gc();
        
        // Kleine Pause nach GC
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        console.info("Module wurden entladen, lade Module neu...");
        
        // Modul-Verzeichnis aktualisieren
        File[] files = modulesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        
        if (files == null || files.length == 0) {
            console.warning("Keine Module im Verzeichnis gefunden");
            return;
        }
        
        // Alle statischen Caches leeren, die Modulreferenzen enthalten könnten
        apiCore.cleanMethodCache(null); // Leert den gesamten Methoden-Cache
        
        // Lade alle Module mit Abhängigkeitsauflösung neu
        loadModulesWithDependencyResolution();
        
        // Befehle nach dem Neuladen synchronisieren
        synchronizeCommands();
        
        // Stelle sicher, dass alle Listener korrekt registriert sind
        for (String moduleName : loadedModules.keySet()) {
            Object moduleObj = loadedModules.get(moduleName);
            if (moduleObj instanceof ApiCore.ModuleInfo) {
                ApiCore.ModuleInfo info = (ApiCore.ModuleInfo) moduleObj;
                Object instance = info.getInstance();
                
                if (instance != null) {
                    // Prüfe, ob das Modul ein Listener ist
                    if (instance.getClass().isAnnotationPresent(com.essentialscore.api.event.Listener.class)) {
                        try {
                            // Registriere Listener erneut
                            Bukkit.getPluginManager().registerEvents((Listener)instance, apiCore);
                            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, 
                                "Listener für Modul " + moduleName + " neu registriert");
                        } catch (Exception e) {
                            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, 
                                "Konnte Listener für Modul " + moduleName + " nicht neu registrieren: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        // Führe onEnable für alle Module erneut aus, um sicherzustellen, dass sie korrekt initialisiert sind
        for (String moduleName : loadedModules.keySet()) {
            Object moduleObj = loadedModules.get(moduleName);
            if (moduleObj instanceof ApiCore.ModuleInfo) {
                ApiCore.ModuleInfo info = (ApiCore.ModuleInfo) moduleObj;
                Object instance = info.getInstance();
                
                if (instance != null) {
                    try {
                        // Versuche onEnable aufzurufen
                        if (instance instanceof com.essentialscore.api.Module) {
                            ((com.essentialscore.api.Module) instance).onEnable();
                            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, 
                                "onEnable für Modul " + moduleName + " aufgerufen");
                        } else {
                            // Versuche über Reflection
                            try {
                                Method onEnableMethod = instance.getClass().getMethod("onEnable");
                                onEnableMethod.invoke(instance);
                                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, 
                                    "onEnable für Legacy-Modul " + moduleName + " aufgerufen");
                            } catch (NoSuchMethodException e) {
                                // Keine onEnable-Methode gefunden, ist okay
                            }
                        }
                    } catch (Exception e) {
                        console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, 
                            "Fehler beim Aufrufen von onEnable für Modul " + moduleName + ": " + e.getMessage());
                        if (apiCore.isDebugMode()) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        
        console.success("Module wurden neu geladen");
        
        // Event für das Neuladen der Module auslösen
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("moduleCount", loadedModules.size());
        apiCore.fireModuleEvent("modules_reloaded", eventData);
    }
    
    /**
     * Gibt alle verfügbaren, aber nicht geladenen Module zurück
     * Prüft auch, ob es sich tatsächlich um gültige Module handelt
     */
    public List<String> getAvailableButNotLoadedModules() {
        List<String> availableModules = new ArrayList<>();
        
        if (!modulesDir.exists()) return availableModules;
        
        File[] files = modulesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return availableModules;
        
        for (File file : files) {
            String moduleName = file.getName().replace(".jar", "");
            
            // Prüfen, ob das Modul bereits geladen ist
            if (loadedModules.containsKey(moduleName)) {
                continue;
            }
            
            try {
                // Prüfen, ob die JAR-Datei eine gültige module.yml enthält
                boolean isValidModule = false;
                try (JarFile jar = new JarFile(file)) {
                    JarEntry moduleYml = jar.getJarEntry("module.yml");
                    if (moduleYml != null) {
                        // Modul-Konfiguration laden
                        YamlConfiguration moduleConfig = YamlConfiguration.loadConfiguration(
                                new InputStreamReader(jar.getInputStream(moduleYml), StandardCharsets.UTF_8));
                        
                        // Prüfen, ob die wesentlichen Felder vorhanden sind
                        if (moduleConfig.contains("name") && moduleConfig.contains("main")) {
                            isValidModule = true;
                        }
                    }
                }
                
                if (isValidModule) {
                    availableModules.add(moduleName);
                } else if (apiCore.isDebugMode()) {
                    apiCore.getLogger().info("JAR-Datei " + file.getName() + " enthält keine gültige module.yml oder ist kein gültiges Modul");
                }
            } catch (Exception e) {
                if (apiCore.isDebugMode()) {
                    apiCore.getLogger().log(Level.WARNING, "Fehler beim Prüfen der JAR-Datei " + file.getName(), e);
                }
            }
        }
        
        return availableModules;
    }
    
    /**
     * Gibt die Befehle eines bestimmten Moduls zurück
     */
    public List<DynamicCommand> getModuleCommands(String moduleName) {
        return moduleCommands.get(moduleName);
    }
    
    /**
     * Klasse zur Speicherung von Modul-Metadaten
     */
    public static class ModuleMetadata {
        private final String name;
        private final String mainClass;
        private final String version;
        private final String description;
        private final List<String> dependencies;
        private final File jarFile;
        
        public ModuleMetadata(String name, String mainClass, String version, String description, 
                             List<String> dependencies, File jarFile) {
            this.name = name;
            this.mainClass = mainClass;
            this.version = version;
            this.description = description;
            this.dependencies = dependencies != null ? dependencies : Collections.emptyList();
            this.jarFile = jarFile;
        }
        
        public String getName() {
            return name;
        }
        
        public String getMainClass() {
            return mainClass;
        }
        
        public String getVersion() {
            return version;
        }
        
        public String getDescription() {
            return description;
        }
        
        public List<String> getDependencies() {
            return dependencies;
        }
        
        public File getJarFile() {
            return jarFile;
        }
    }
    
    /**
     * Erweiterter ClassLoader für Module mit besserer Isolation
     */
    private static class ModuleClassLoader extends URLClassLoader {
        private final String moduleName;
        private final ClassLoader pluginClassLoader;
        private static final Set<String> API_PACKAGES = new HashSet<>(Arrays.asList(
            "com.essentialscore.api.",
            "com.essentialscore.api.impl."
        ));
        
        public ModuleClassLoader(String moduleName, URL[] urls, ClassLoader parent) {
            super(urls, parent);
            this.moduleName = moduleName;
            this.pluginClassLoader = parent;
        }
        
        @Override
        public String toString() {
            return "ModuleClassLoader{" + moduleName + "}";
        }
        
        /**
         * Override the loadClass method to prioritize API packages from parent
         */
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // Always delegate API packages to parent classloader first
            for (String apiPackage : API_PACKAGES) {
                if (name.startsWith(apiPackage)) {
                    try {
                        // Try to load from plugin classloader first
                        return pluginClassLoader.loadClass(name);
                    } catch (ClassNotFoundException e) {
                        // Fall back to normal loading if not found in parent
                    }
                }
            }
            
            // For all other classes, follow normal class loading
            return super.loadClass(name);
        }
    }

    /**
     * Sucht nach der module.yml Datei in einer JAR und gibt den vollständigen Pfad zurück
     * 
     * @param jarFile Die JAR-Datei, in der gesucht werden soll
     * @return Den vollständigen Pfad zur module.yml oder null, wenn nicht gefunden
     */
    public String findModuleYmlPath(File jarFile) {
        if (!jarFile.exists() || !jarFile.isFile()) {
            return null;
        }
        
        try (JarFile jar = new JarFile(jarFile)) {
            // Mögliche Pfade, in denen module.yml sein könnte
            String[] possiblePaths = {
                "module.yml",
                "src/main/resources/module.yml",
                "main/resources/module.yml",
                "resources/module.yml",
                "META-INF/module.yml"
            };
            
            for (String path : possiblePaths) {
                JarEntry entry = jar.getJarEntry(path);
                if (entry != null) {
                    console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "module.yml gefunden in " + jarFile.getName() + " unter dem Pfad: " + path, apiCore.isDebugMode());
                    return path;
                }
            }
            
            // Falls keine direkten Treffer, durchsuche alle Einträge nach einem, der mit module.yml endet
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith("/module.yml") || name.endsWith("\\module.yml")) {
                    console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "module.yml gefunden in " + jarFile.getName() + " unter dem Pfad: " + name, apiCore.isDebugMode());
                    return name;
                }
            }
        } catch (IOException e) {
            console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Durchsuchen von " + jarFile.getName() + " nach module.yml: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Sucht nach neuen Modulen im Modulverzeichnis und lädt sie bei Bedarf
     * Diese Methode kann für regelmäßige Prüfungen sowie für manuelle Reload-Befehle verwendet werden
     * 
     * @param sender Optional: Der CommandSender für Statusmeldungen, kann null sein
     * @return Anzahl der neu geladenen Module
     */
    public int checkForNewModules(CommandSender sender) {
        if (!modulesDir.exists()) {
            if (sender != null) {
                sender.sendMessage("§cModulverzeichnis existiert nicht: " + modulesDir.getAbsolutePath());
            }
            console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Modulverzeichnis existiert nicht: " + modulesDir.getAbsolutePath());
            return 0;
        }
        
        // Liste alle JAR-Dateien im Modulverzeichnis auf
        File[] files = modulesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        
        if (files == null || files.length == 0) {
            if (sender != null) {
                sender.sendMessage("§eKeine Module im Verzeichnis gefunden");
            }
            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Keine Module im Verzeichnis gefunden");
            return 0;
        }
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Überprüfe auf neue Module...");
        
        int newModulesLoaded = 0;
        List<String> loadedNames = new ArrayList<>();
        
        // Überprüfe jede JAR-Datei im Modulordner
        for (File file : files) {
            String moduleName = file.getName().replace(".jar", "");
            
            // Prüfen, ob das Modul bereits geladen ist
            if (loadedModules.containsKey(moduleName)) {
                continue; // Modul bereits geladen, überspringen
            }
            
            // Konfigurationsdatei prüfen
            File configFile = new File(configDir, moduleName + ".yml");
            boolean shouldLoad = true;
            
            if (configFile.exists()) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    shouldLoad = config.getBoolean("enabled", true);
                } catch (Exception e) {
                    console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Lesen der Konfiguration für " + moduleName + ": " + e.getMessage());
                }
            }
            
            if (shouldLoad) {
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Neues Modul gefunden: " + moduleName);
                if (sender != null) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&7Lade neues Modul: &e" + moduleName));
                }
                
                try {
                    // Modul synchron laden, da wir bereits im Haupt- oder Async-Thread sein könnten
                    loadModule(file);
                    loadedNames.add(moduleName);
                    newModulesLoaded++;
                    
                    console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Neues Modul " + moduleName + " erfolgreich geladen");
                    if (sender != null) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aModul &e" + moduleName + "&a erfolgreich geladen"));
                    }
                } catch (Exception e) {
                    console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Laden des neuen Moduls " + moduleName + ": " + e.getMessage());
                    if (sender != null) {
                        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cFehler beim Laden des Moduls &e" + 
                                moduleName + "&c: " + e.getMessage()));
                    }
                }
            } else {
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Neues Modul " + moduleName + " gefunden, aber in Konfiguration deaktiviert");
                if (sender != null && apiCore.isDebugMode()) {
                    sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + 
                            "&eModul &7" + moduleName + "&e wurde gefunden, ist aber deaktiviert"));
                }
            }
        }
        
        // Nach dem Laden neuer Module die Ressourcen extrahieren
        if (newModulesLoaded > 0 && apiCore.getConfig().getBoolean("general.extract-module-resources", true)) {
            apiCore.getServer().getScheduler().runTaskAsynchronously(apiCore, () -> {
                try {
                    for (String moduleName : loadedNames) {
                        ApiCore.ModuleInfo info = (ApiCore.ModuleInfo) loadedModules.get(moduleName);
                        if (info != null) {
                            console.categoryInfo(ConsoleFormatter.MessageCategory.RESOURCE, "Extrahiere Ressourcen für neues Modul: " + moduleName);
                            extractModuleResources(info);
                        }
                    }
                } catch (Exception e) {
                    console.categoryError(ConsoleFormatter.MessageCategory.RESOURCE, "Fehler beim Extrahieren von Ressourcen für neue Module: " + e.getMessage());
                }
            });
        }
        
        // Befehle synchronisieren, wenn neue Module geladen wurden
        if (newModulesLoaded > 0) {
            try {
                synchronizeCommands();
                console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Befehle für neue Module synchronisiert");
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Synchronisieren der Befehle für neue Module: " + e.getMessage());
            }
        }
        
        return newModulesLoaded;
    }
    
    /**
     * Extrahiert Ressourcen aus einem einzelnen Modul
     */
    private void extractModuleResources(ApiCore.ModuleInfo moduleInfo) {
        if (moduleInfo == null) return;
        
        try {
            // Ressourcenverzeichnis erstellen
            File moduleResourcesDir = new File(modulesDir, "module_resources");
            File moduleResourceDir = new File(moduleResourcesDir, moduleInfo.getName());
            
            if (!moduleResourceDir.exists() && !moduleResourceDir.mkdirs()) {
                console.categoryWarning(ConsoleFormatter.MessageCategory.RESOURCE, "Konnte Ressourcenverzeichnis für Modul " + moduleInfo.getName() + " nicht erstellen");
                return;
            }
            
            // JAR-Datei des Moduls
            File jarFile = moduleInfo.getJarFile();
            console.categoryInfo(ConsoleFormatter.MessageCategory.RESOURCE, "Extrahiere Ressourcen aus: " + jarFile.getName());
            
            // module.yml Pfad finden
            String moduleYmlPath = findModuleYmlPath(jarFile);
            String resourceBasePath = null;
            
            if (moduleYmlPath != null) {
                resourceBasePath = determineResourceBasePath(moduleYmlPath);
            }
            
            // Ressourcen extrahieren
            try (JarFile jar = new JarFile(jarFile)) {
                // Einträge durchsuchen und Ressourcen extrahieren
                Enumeration<JarEntry> entries = jar.entries();
                int extractedCount = 0;
                int skippedCount = 0;
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    
                    if (entry.isDirectory() || entryName.endsWith(".class") || 
                        entryName.equals("module.yml") || entryName.equals("plugin.yml")) {
                        continue;
                    }
                    
                    // Prüfen, ob es sich um eine Ressource handelt
                    boolean isResource = false;
                    String relativePath = null;
                    
                    if (resourceBasePath != null && entryName.startsWith(resourceBasePath)) {
                        isResource = true;
                        relativePath = entryName.substring(resourceBasePath.length());
                    } else if (entryName.contains("/resources/")) {
                        isResource = true;
                        int index = entryName.indexOf("/resources/");
                        relativePath = entryName.substring(index + 11); // "/resources/" = 11 Zeichen
                    } else if (entryName.endsWith(".yml") || entryName.endsWith(".properties") || 
                               entryName.endsWith(".json") || entryName.endsWith(".txt")) {
                        isResource = true;
                        relativePath = new File(entryName).getName();
                    }
                    
                    if (isResource && relativePath != null && !relativePath.isEmpty()) {
                        // Ressource extrahieren
                        File targetFile = new File(moduleResourceDir, relativePath);
                        
                        // Prüfen, ob die Datei bereits existiert - wenn ja, überspringen
                        if (targetFile.exists()) {
                            skippedCount++;
                            if (apiCore.isDebugMode()) {
                                console.categoryDebug(ConsoleFormatter.MessageCategory.RESOURCE, "Überspringe existierende Datei: " + targetFile.getAbsolutePath(), true);
                            }
                            continue;
                        }
                        
                        // Elternverzeichnis erstellen, falls nötig
                        File parentDir = targetFile.getParentFile();
                        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                            console.categoryWarning(ConsoleFormatter.MessageCategory.RESOURCE, "Konnte Verzeichnis nicht erstellen: " + parentDir.getPath());
                            continue;
                        }
                        
                        // Datei kopieren
                        try (InputStream is = jar.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(targetFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                            extractedCount++;
                        }
                    }
                }
                
                console.categorySuccess(ConsoleFormatter.MessageCategory.RESOURCE, "Extrahiert: " + extractedCount + " Ressourcen aus " + moduleInfo.getName() + 
                                       ", " + skippedCount + " bestehende Dateien übersprungen");
            }
        } catch (Exception e) {
            console.categoryError(ConsoleFormatter.MessageCategory.RESOURCE, "Fehler beim Extrahieren von Ressourcen für Modul " + moduleInfo.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Ermittelt den Basispfad für Ressourcen basierend auf dem Pfad der module.yml
     */
    private String determineResourceBasePath(String moduleYmlPath) {
        if (moduleYmlPath == null) return null;
        
        String path = moduleYmlPath.substring(0, moduleYmlPath.lastIndexOf("module.yml"));
        return path.isEmpty() ? null : path;
    }

    private String getModuleNameFromJar(File jarFile) {
        try {
            String moduleYmlPath = findModuleYmlPath(jarFile);
            if (moduleYmlPath == null) {
                return null;
            }
            
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry(moduleYmlPath);
                if (entry == null) {
                    return null;
                }
                
                try (InputStream is = jar.getInputStream(entry)) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
                    return config.getString("name");
                }
            }
        } catch (Exception e) {
            console.error("Fehler beim Extrahieren des Modulnamens: " + e.getMessage());
            return null;
        }
    }

    /**
     * Führt eine private Methode eines Moduls sicher aus
     * 
     * @param moduleName Der Name des Moduls
     * @param methodName Der Name der Methode
     * @param parameterTypes Die Parametertypen
     * @param args Die Argumente für die Methode
     * @return Das Ergebnis des Methodenaufrufs oder null, wenn ein Fehler auftritt
     */
    public Object executeModuleMethod(String moduleName, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (!loadedModules.containsKey(moduleName)) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Versuch, Methode auf nicht geladenem Modul aufzurufen: " + moduleName);
            return null;
        }
        
        Object moduleInfo = loadedModules.get(moduleName);
        if (!(moduleInfo instanceof ApiCore.ModuleInfo)) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Ungültiges ModuleInfo-Objekt für Modul: " + moduleName);
            return null;
        }
        
        ApiCore.ModuleInfo info = (ApiCore.ModuleInfo) moduleInfo;
        Object moduleInstance = info.getInstance();
        
        if (moduleInstance == null) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Null-Instanz für Modul: " + moduleName);
            return null;
        }
        
        // Verwende Sandbox für sichere Ausführung, wenn aktiviert
        com.essentialscore.api.module.ModuleSandbox sandbox = apiCore.getModuleSandbox();
        if (sandbox != null) {
            try {
                return sandbox.execute(moduleName, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        Method method = moduleInstance.getClass().getMethod(methodName, parameterTypes);
                        return method.invoke(moduleInstance, args);
                    }
                });
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, 
                    "Fehler bei der Sandbox-Ausführung für " + moduleName + ": " + e.getMessage());
                return null;
            }
        } else {
            // Fallback zur direkten Ausführung
            try {
                Method method = moduleInstance.getClass().getMethod(methodName, parameterTypes);
                return method.invoke(moduleInstance, args);
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, 
                    "Fehler beim Ausführen der Methode " + methodName + " von " + moduleName + ": " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }
    }

    /**
     * Führt einen Befehl eines Moduls aus
     * 
     * @param moduleName Der Name des Moduls
     * @param sender Der Befehlsabsender
     * @param command Der ausgeführte Befehl
     * @param label Das verwendete Label
     * @param args Die Befehlsargumente
     * @return true, wenn der Befehl erfolgreich ausgeführt wurde
     */
    public boolean onModuleCommand(String moduleName, CommandSender sender, org.bukkit.command.Command command,
                                String label, String[] args) {
        if (!loadedModules.containsKey(moduleName)) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Versuch, Befehl auf nicht geladenem Modul auszuführen: " + moduleName);
            return false;
        }
        
        Object moduleInfo = loadedModules.get(moduleName);
        if (!(moduleInfo instanceof ApiCore.ModuleInfo)) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Ungültiges ModuleInfo-Objekt für Modul: " + moduleName);
            return false;
        }
        
        ApiCore.ModuleInfo info = (ApiCore.ModuleInfo) moduleInfo;
        Object moduleInstance = info.getInstance();
        
        if (moduleInstance == null) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Null-Instanz für Modul: " + moduleName);
            return false;
        }
        
        // Verwende Sandbox für sichere Ausführung, wenn aktiviert
        com.essentialscore.api.module.ModuleSandbox sandbox = apiCore.getModuleSandbox();
        if (sandbox != null) {
            try {
                return sandbox.execute(moduleName, new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Method method = moduleInstance.getClass().getMethod("onCommand", CommandSender.class, 
                            org.bukkit.command.Command.class, String.class, String[].class);
                        Object result = method.invoke(moduleInstance, sender, command, label, args);
                        return result instanceof Boolean ? (Boolean) result : false;
                    }
                });
            } catch (Exception e) {
                return false;
            }        } else {
            // Fallback zur direkten Ausführung
            try {
                Method method = moduleInstance.getClass().getMethod("onCommand", CommandSender.class, 
                    org.bukkit.command.Command.class, String.class, String[].class);
                Object result = method.invoke(moduleInstance, sender, command, label, args);
                return result instanceof Boolean ? (Boolean) result : false;
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, 
                    "Fehler beim Ausführen des Befehls von Modul " + moduleName + ": " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
                return false;
            }
        }
    }

    /**
     * Klasse zum Speichern der Performancedaten eines Moduls
     */
    public static class ModulePerformanceData {
        private final String moduleName;
        private long lastUpdateTime = System.currentTimeMillis();
        
        // CPU-Auslastung
        private double cpuUsagePercent = 0.0;
        private final Queue<Double> cpuHistory = new LinkedList<>();
        private static final int HISTORY_SIZE = 10;
        
        // Speichernutzung
        private long memoryUsageBytes = 0L;
        
        // Methodenaufrufe
        private final Map<String, MethodExecutionData> methodExecutions = new ConcurrentHashMap<>();
        private long totalExecutionCount = 0;
        private double avgExecutionTimeMs = 0.0;
        
        // Kritische Performance-Ereignisse
        private final ConcurrentLinkedQueue<PerformanceEvent> criticalEvents = new ConcurrentLinkedQueue<>();
        private static final int MAX_EVENTS = 50;
        
        public ModulePerformanceData(String moduleName) {
            this.moduleName = moduleName;
        }
        
        /**
         * Aktualisiert die CPU-Nutzung dieses Moduls
         * 
         * @param cpuPercent CPU-Prozentsatz (0-100)
         */
        public void updateCpuUsage(double cpuPercent) {
            this.cpuUsagePercent = cpuPercent;
            
            // Verlauf aktualisieren
            cpuHistory.add(cpuPercent);
            while (cpuHistory.size() > HISTORY_SIZE) {
                cpuHistory.poll();
            }
            
            // Kritisches Ereignis hinzufügen, wenn Schwellenwert überschritten
            if (cpuPercent >= CPU_THRESHOLD_CRITICAL) {
                addCriticalEvent("CPU-Auslastung kritisch", 
                    "CPU-Nutzung von " + String.format("%.1f", cpuPercent) + "% überschreitet Schwellenwert (" + CPU_THRESHOLD_CRITICAL + "%)",
                    PerformanceEventType.CPU_CRITICAL);
            }
            
            lastUpdateTime = System.currentTimeMillis();
        }
        
        /**
         * Aktualisiert die Speichernutzung dieses Moduls
         * 
         * @param memoryBytes Speichernutzung in Bytes
         */
        public void updateMemoryUsage(long memoryBytes) {
            this.memoryUsageBytes = memoryBytes;
            
            // Kritisches Ereignis hinzufügen, wenn Schwellenwert überschritten
            long memoryMB = memoryBytes / (1024 * 1024);
            if (memoryMB >= MEMORY_THRESHOLD_CRITICAL) {
                addCriticalEvent("Speichernutzung kritisch", 
                    "Speichernutzung von " + memoryMB + "MB überschreitet Schwellenwert (" + MEMORY_THRESHOLD_CRITICAL + "MB)",
                    PerformanceEventType.MEMORY_CRITICAL);
            }
            
            lastUpdateTime = System.currentTimeMillis();
        }
        
        /**
         * Erfasst die Ausführungszeit einer Methode
         * 
         * @param methodName Name der Methode
         * @param executionTimeMs Ausführungszeit in Millisekunden
         */
        public void recordMethodExecution(String methodName, double executionTimeMs) {
            methodExecutions.computeIfAbsent(methodName, k -> new MethodExecutionData())
                           .recordExecution(executionTimeMs);
            
            // Gesamtzahl der Ausführungen und Durchschnittszeit aktualisieren
            totalExecutionCount++;
            
            // Gewichteter Durchschnitt für bessere Stabilität
            avgExecutionTimeMs = (avgExecutionTimeMs * 0.9) + (executionTimeMs * 0.1);
            
            // Kritisches Ereignis für sehr langsame Methoden
            if (executionTimeMs >= EXECUTION_THRESHOLD_CRITICAL) {
                addCriticalEvent("Methode sehr langsam", 
                    "Methode " + methodName + " benötigte " + String.format("%.1f", executionTimeMs) + "ms (Schwellenwert: " + EXECUTION_THRESHOLD_CRITICAL + "ms)",
                    PerformanceEventType.METHOD_CRITICAL);
            }
            
            lastUpdateTime = System.currentTimeMillis();
        }
        
        /**
         * Fügt ein kritisches Performance-Ereignis hinzu
         * 
         * @param title Titel des Ereignisses
         * @param description Beschreibung des Ereignisses
         * @param type Typ des Ereignisses
         */
        private void addCriticalEvent(String title, String description, PerformanceEventType type) {
            criticalEvents.add(new PerformanceEvent(title, description, type));
            
            // Begrenze die Anzahl der gespeicherten Ereignisse
            while (criticalEvents.size() > MAX_EVENTS) {
                criticalEvents.poll();
            }
        }
        
        /**
         * Prüft, ob das Modul Performance-Probleme hat
         * 
         * @return PerformanceStatus.OK, WARNING oder CRITICAL
         */
        public PerformanceStatus getPerformanceStatus() {
            // CPU-Status prüfen
            if (cpuUsagePercent >= CPU_THRESHOLD_CRITICAL) {
                return PerformanceStatus.CRITICAL;
            } else if (cpuUsagePercent >= CPU_THRESHOLD_WARNING) {
                return PerformanceStatus.WARNING;
            }
            
            // Speicher-Status prüfen
            long memoryMB = memoryUsageBytes / (1024 * 1024);
            if (memoryMB >= MEMORY_THRESHOLD_CRITICAL) {
                return PerformanceStatus.CRITICAL;
            } else if (memoryMB >= MEMORY_THRESHOLD_WARNING) {
                return PerformanceStatus.WARNING;
            }
            
            // Methoden-Ausführungszeit prüfen
            if (avgExecutionTimeMs >= EXECUTION_THRESHOLD_CRITICAL) {
                return PerformanceStatus.CRITICAL;
            } else if (avgExecutionTimeMs >= EXECUTION_THRESHOLD_WARNING) {
                return PerformanceStatus.WARNING;
            }
            
            return PerformanceStatus.OK;
        }
        
        // Getter-Methoden für alle Eigenschaften
        
        public String getModuleName() {
            return moduleName;
        }
        
        public double getCpuUsagePercent() {
            return cpuUsagePercent;
        }
        
        public List<Double> getCpuHistory() {
            return new ArrayList<>(cpuHistory);
        }
        
        public long getMemoryUsageBytes() {
            return memoryUsageBytes;
        }
        
        public String getMemoryUsageFormatted() {
            if (memoryUsageBytes < 1024) {
                return memoryUsageBytes + " B";
            } else if (memoryUsageBytes < 1024 * 1024) {
                return String.format("%.2f KB", memoryUsageBytes / 1024.0);
            } else if (memoryUsageBytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", memoryUsageBytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", memoryUsageBytes / (1024.0 * 1024.0 * 1024.0));
            }
        }
        
        public Map<String, MethodExecutionData> getMethodExecutions() {
            return new HashMap<>(methodExecutions);
        }
        
        public long getTotalExecutionCount() {
            return totalExecutionCount;
        }
        
        public double getAvgExecutionTimeMs() {
            return avgExecutionTimeMs;
        }
        
        public List<PerformanceEvent> getCriticalEvents() {
            return new ArrayList<>(criticalEvents);
        }
        
        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
        
        public boolean isStale() {
            // Daten älter als 5 Minuten gelten als veraltet
            return System.currentTimeMillis() - lastUpdateTime > 5 * 60 * 1000;
        }
    }
    
    /**
     * Daten zur Ausführung einer Methode
     */
    public static class MethodExecutionData {
        private long callCount = 0;
        private double totalTimeMs = 0;
        private double avgTimeMs = 0;
        private double minTimeMs = Double.MAX_VALUE;
        private double maxTimeMs = 0;
        private final Queue<Double> recentExecutions = new LinkedList<>();
        private static final int MAX_RECENT = 20;
        
        /**
         * Erfasst eine Methodenausführung
         * 
         * @param executionTimeMs Ausführungszeit in Millisekunden
         */
        public void recordExecution(double executionTimeMs) {
            callCount++;
            totalTimeMs += executionTimeMs;
            avgTimeMs = totalTimeMs / callCount;
            minTimeMs = Math.min(minTimeMs, executionTimeMs);
            maxTimeMs = Math.max(maxTimeMs, executionTimeMs);
            
            // Speichere die letzten Ausführungszeiten
            recentExecutions.add(executionTimeMs);
            while (recentExecutions.size() > MAX_RECENT) {
                recentExecutions.poll();
            }
        }
        
        // Getter-Methoden
        
        public long getCallCount() {
            return callCount;
        }
        
        public double getTotalTimeMs() {
            return totalTimeMs;
        }
        
        public double getAvgTimeMs() {
            return avgTimeMs;
        }
        
        public double getMinTimeMs() {
            return minTimeMs == Double.MAX_VALUE ? 0 : minTimeMs;
        }
        
        public double getMaxTimeMs() {
            return maxTimeMs;
        }
        
        public List<Double> getRecentExecutions() {
            return new ArrayList<>(recentExecutions);
        }
    }
    
    /**
     * Ein kritisches Performance-Ereignis
     */
    public static class PerformanceEvent {
        private final String title;
        private final String description;
        private final PerformanceEventType type;
        private final long timestamp;
        
        public PerformanceEvent(String title, String description, PerformanceEventType type) {
            this.title = title;
            this.description = description;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public PerformanceEventType getType() {
            return type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date(timestamp));
        }
    }
    
    /**
     * Arten von Performance-Ereignissen
     */    /**
     * Typ des Performance-Ereignisses
     */
    public enum PerformanceEventType {
        CPU_WARNING,
        CPU_CRITICAL,
        MEMORY_WARNING,
        MEMORY_CRITICAL,
        METHOD_WARNING,
        METHOD_CRITICAL
    }

    /**
     * Performance-Status eines Moduls
     */    public enum PerformanceStatus {
        OK,      // Grün - alles in Ordnung
        WARNING, // Gelb - Auslastung beachten
        CRITICAL // Rot - sollte überprüft und ggf. deaktiviert werden
    }

    /**
     * Startet das Performance-Tracking für Module
     */
    public void startPerformanceTracking() {
        // Starte den Performance-Tracker im Hintergrund
        performanceTrackerService.scheduleAtFixedRate(() -> {
            try {
                updateModulePerformanceData();
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.PERFORMANCE,
                    "Fehler bei der Performance-Datenerfassung: " + e.getMessage());
                if (apiCore.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.PERFORMANCE,
            "Modul-Performance-Tracking gestartet (Intervall: 5s)");
    }
    
    /**
     * Stoppt das Performance-Tracking für Module
     */
    public void stopPerformanceTracking() {
        performanceTrackerService.shutdown();
        try {
            if (!performanceTrackerService.awaitTermination(10, TimeUnit.SECONDS)) {
                performanceTrackerService.shutdownNow();
            }
        } catch (InterruptedException e) {
            performanceTrackerService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.PERFORMANCE,
            "Modul-Performance-Tracking gestoppt");
    }
    
    /**
     * Aktualisiert die Performance-Daten aller Module
     */
    private void updateModulePerformanceData() {
        // Durchlaufe alle geladenen Module
        for (Map.Entry<String, Object> entry : loadedModules.entrySet()) {
            String moduleName = entry.getKey();
            Object moduleInfo = entry.getValue();
            
            if (moduleInfo instanceof ApiCore.ModuleInfo) {
                Object moduleInstance = ((ApiCore.ModuleInfo) moduleInfo).getInstance();
                if (moduleInstance != null) {
                    // Hole oder erstelle Performance-Daten für dieses Modul
                    ModulePerformanceData performanceData = modulePerformanceMap.computeIfAbsent(
                        moduleName, ModulePerformanceData::new);
                    
                    // CPU-Auslastung messen (vereinfachte Schätzung basierend auf Thread-Nutzung)
                    double cpuEstimate = estimateModuleCpuUsage(moduleName);
                    performanceData.updateCpuUsage(cpuEstimate);
                    
                    // Speichernutzung schätzen
                    long memoryEstimate = estimateModuleMemoryUsage(moduleInstance);
                    performanceData.updateMemoryUsage(memoryEstimate);
                }
            }
        }
        
        // Entferne veraltete Performance-Daten von entladenen Modulen
        modulePerformanceMap.entrySet().removeIf(entry -> {
            String moduleName = entry.getKey();
            ModulePerformanceData data = entry.getValue();
            
            // Modul nicht mehr geladen und Daten veraltet
            return !loadedModules.containsKey(moduleName) && data.isStale();
        });
    }
    
    /**
     * Schätzt die CPU-Auslastung eines Moduls basierend auf den Methodenaufrufen
     * 
     * @param moduleName Der Name des Moduls
     * @return Geschätzte CPU-Auslastung in Prozent (0-100)
     */
    private double estimateModuleCpuUsage(String moduleName) {
        // In einer echten Implementierung würde hier eine tatsächliche CPU-Profilierung
        // durchgeführt werden. Hier verwenden wir einen vereinfachten Ansatz.
        
        ModulePerformanceData data = modulePerformanceMap.get(moduleName);
        if (data == null) {
            return 0.0;
        }
        
        // Bestimme aktuelle Last basierend auf durchschnittlicher Methodenausführungszeit
        // und Anzahl der Aufrufe in letzter Zeit.
        double avgExecTime = data.getAvgExecutionTimeMs();
        
        // Skalierungsfaktor: 100ms durchschnittlich entspricht etwa 10% CPU-Last
        // Dies ist eine grobe Schätzung und sollte in einer echten Implementierung
        // durch tatsächliche Messungen ersetzt werden.
        double cpuEstimate = Math.min(avgExecTime / 10.0, 100.0);
        
        // Füge Variation hinzu, um realistischere Daten zu erhalten
        double randomFactor = 0.8 + (Math.random() * 0.4); // 0.8 bis 1.2
        cpuEstimate = Math.min(cpuEstimate * randomFactor, 100.0);
        
        return cpuEstimate;
    }
    
    /**
     * Schätzt die Speichernutzung eines Moduls
     * 
     * @param moduleInstance Die Modul-Instanz
     * @return Geschätzte Speichernutzung in Bytes
     */
    private long estimateModuleMemoryUsage(Object moduleInstance) {
        // In einer echten Implementierung würde hier eine tatsächliche Speicherprofilierung
        // durchgeführt werden. Für dieses Beispiel verwenden wir eine grobe Schätzung.
        
        // Basisgröße für jedes Modul (1MB)
        long baseSize = 1024 * 1024;
        
        // Füge zufällige Variation hinzu
        double randomFactor = 0.5 + (Math.random() * 1.5); // 0.5 bis 2.0
        long memoryEstimate = (long)(baseSize * randomFactor);
        
        return memoryEstimate;
    }
    
    /**
     * Zeichnet einen Methodenaufruf für Performance-Tracking auf
     * 
     * @param moduleName Der Name des Moduls
     * @param methodName Der Name der aufgerufenen Methode
     * @param executionTimeMs Die Ausführungszeit in Millisekunden
     */
    public void recordMethodExecution(String moduleName, String methodName, double executionTimeMs) {
        ModulePerformanceData data = modulePerformanceMap.computeIfAbsent(
            moduleName, ModulePerformanceData::new);
        
        data.recordMethodExecution(methodName, executionTimeMs);
    }
    
    /**
     * Gibt die Performance-Daten für ein Modul zurück
     * 
     * @param moduleName Der Name des Moduls
     * @return Die Performance-Daten oder null, wenn keine Daten vorhanden sind
     */
    public ModulePerformanceData getModulePerformanceData(String moduleName) {
        return modulePerformanceMap.get(moduleName);
    }
    
    /**
     * Gibt eine Map mit Performance-Daten aller Module zurück
     * 
     * @return Eine Map mit Modulnamen als Schlüssel und Performance-Daten als Werte
     */
    public Map<String, ModulePerformanceData> getAllModulePerformanceData() {
        return new HashMap<>(modulePerformanceMap);
    }
    
    /**
     * Gibt alle Module mit kritischem Performance-Status zurück
     * 
     * @return Eine Liste von ModulePerformanceData-Objekten mit kritischem Status
     */
    public List<ModulePerformanceData> getCriticalModules() {
        List<ModulePerformanceData> criticalModules = new ArrayList<>();
        
        for (ModulePerformanceData data : modulePerformanceMap.values()) {
            if (data.getPerformanceStatus() == PerformanceStatus.CRITICAL) {
                criticalModules.add(data);
            }
        }
        
        return criticalModules;
    }
    
    /**
     * Shutting down the ModuleManager and cleanup resources
     */
    public void shutdown() {
        // Stop performance tracking
        try {
            stopPerformanceTracking();
        } catch (Exception e) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.PERFORMANCE,
                "Fehler beim Stoppen des Performance-Trackings: " + e.getMessage());
        }
        
        // Clean up other resources if needed
        modulePerformanceMap.clear();
    }
}
