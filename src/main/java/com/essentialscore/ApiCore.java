package com.essentialscore;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import com.essentialscore.util.BloomFilter;
import com.essentialscore.util.JvmOptimizer;
import com.essentialscore.util.ObjectPool;
import com.essentialscore.util.OffHeapCache;
import com.essentialscore.threading.AdvancedWorkStealingPool;
import com.essentialscore.ConsoleFormatter;
import com.essentialscore.util.ModuleResourceManager;
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.api.ModuleEventListener;
import com.essentialscore.api.impl.CoreModuleAPI;

public class ApiCore extends JavaPlugin implements Listener {

    private File configDir;
    private File modulesDir;
    private File dataDir;
    private final Map<String, ModuleInfo> loadedModules = new ConcurrentHashMap<>(16, 0.75f, 2);
    private final Map<String, List<DynamicCommand>> moduleCommands = new ConcurrentHashMap<>(16, 0.75f, 2);
    private String messagePrefix;
    private boolean debugMode;
    
    // Manager-Instanzen
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private ThreadManager threadManager;
    private PermissionManager permissionManager;
    private CommandManager commandManager;
    private PerformanceMonitor performanceMonitor;
    private PerformanceBenchmark performanceBenchmark;
    private ModuleFileManager moduleFileManager;
    private ModuleResourceManager resourceManager; // Neue Instanzvariable für verbesserten Ressourcenmanager
    private ConsoleFormatter console;
    
    // Thread-safe shared data with optimized initial capacity
    private final ConcurrentHashMap<String, Object> sharedData = new ConcurrentHashMap<>(32, 0.75f, 2);
    
    // Caching reflection methods for performance with LRU eviction policy
    private final Map<String, Method> methodCache = Collections.synchronizedMap(new LinkedHashMap<String, Method>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Method> eldest) {
            return size() > getConfig().getInt("performance.cache-size.methods", 500);
        }
    });
    
    private final Map<String, MethodHandle> methodHandleCache = Collections.synchronizedMap(new LinkedHashMap<String, MethodHandle>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, MethodHandle> eldest) {
            return size() > getConfig().getInt("performance.cache-size.reflection", 200);
        }
    });
    
    @SuppressWarnings("unused")
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    
    // Thread pool for asynchronous operations
    private ExecutorService executorService;
    @SuppressWarnings("unused")
    private final int THREAD_POOL_SIZE = 4;

    // Lazy-initialized static pattern for better performance
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    
    // Map to store method execution timings with optimized structure
    private final ConcurrentHashMap<String, Map<String, Double>> methodTimings = new ConcurrentHashMap<>(16, 0.75f, 1);
    
    // Optimized byte buffers for file operations
    private static final int BUFFER_SIZE = 8192;
    @SuppressWarnings("unused")
    private final ThreadLocal<byte[]> bufferCache = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[BUFFER_SIZE];
        }
    };
    
    // String formatting cache for commonly used messages
    private final ConcurrentHashMap<String, String> formattedMessageCache = new ConcurrentHashMap<>(64, 0.75f, 1);

    // Optimierte Performance-Komponenten
    private final BloomFilter<String> permissionCache = BloomFilter.create(1000);
    private final ConcurrentHashMap<String, Boolean> permissionExactCache = new ConcurrentHashMap<>(128);
    @SuppressWarnings("unused")
    private OffHeapCache largeDataCache;
    @SuppressWarnings("unused")
    private AdvancedWorkStealingPool advancedExecutor;

    // Event-System für Module
    private final Map<String, List<com.essentialscore.api.ModuleEventListener>> moduleListeners = new ConcurrentHashMap<>(8, 0.75f, 1);
    
    // API instances for modules
    private final Map<String, ModuleAPI> moduleAPIs = new ConcurrentHashMap<>(16, 0.75f, 1);
    
    /**
     * Wrapper interface for ModuleEventListener to help IDE resolve references
     * during refactoring. This simply wraps the standalone interface.
     * @deprecated Use com.essentialscore.api.ModuleEventListener directly
     */
    @Deprecated
    public interface ModuleEventListener extends com.essentialscore.api.ModuleEventListener {
        // No additional methods needed - this is just a wrapper
    }
    
    /**
     * Registriert einen Modul-Event-Listener für ein bestimmtes Event
     * 
     * @param eventName Name des Events
     * @param listener Event-Listener des Moduls
     */
    public void registerModuleListener(String eventName, com.essentialscore.api.ModuleEventListener listener) {
        moduleListeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    
    /**
     * Deregistriert einen Modul-Event-Listener
     * 
     * @param eventName Name des Events
     * @param listener Zu entfernender Event-Listener
     */
    public void unregisterModuleListener(String eventName, com.essentialscore.api.ModuleEventListener listener) {
        List<com.essentialscore.api.ModuleEventListener> listeners = moduleListeners.get(eventName);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Löst ein Modul-Event aus und informiert alle registrierten Listener
     * 
     * @param eventName Name des Events
     * @param data Eventdaten
     */
    public void fireModuleEvent(String eventName, Map<String, Object> data) {
        List<com.essentialscore.api.ModuleEventListener> listeners = moduleListeners.get(eventName);
        if (listeners != null) {
            for (com.essentialscore.api.ModuleEventListener listener : listeners) {
                try {
                    listener.onModuleEvent(eventName, data);
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Fehler bei der Verarbeitung des Events " + eventName + " durch einen Listener", e);
                }
            }
        }
    }

    // ModuleInfo-Klasse public machen
    public static class ModuleInfo {
        private final String name;
        private final String version;
        private final String description;
        private final File jarFile;
        private final URLClassLoader loader;
        private final Object instance;
        private boolean hasPlayerJoinHandler;

        public ModuleInfo(String name, String version, String description, File jarFile, URLClassLoader loader, Object instance) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.jarFile = jarFile;
            this.loader = loader;
            this.instance = instance;
            
            // Sichere Prüfung, ob die Instanz null ist
            if (instance != null) {
                try {
                    // Check if it implements the Module interface
                    if (instance instanceof Module) {
                        this.hasPlayerJoinHandler = true;
                    } else {
                        // Check for onPlayerJoin method
                        this.hasPlayerJoinHandler = hasMethod(instance.getClass(), "onPlayerJoin", Player.class);
                    }
                } catch (Exception e) {
                    this.hasPlayerJoinHandler = false;
                    // Silent catch - kein Logging möglich in statischer Klasse
                }
            } else {
                this.hasPlayerJoinHandler = false;
            }
        }

        private boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
            try {
                clazz.getMethod(methodName, paramTypes);
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public File getJarFile() {
            return jarFile;
        }

        public URLClassLoader getLoader() {
            return loader;
        }

        public Object getInstance() {
            return instance;
        }

        public boolean hasPlayerJoinHandler() {
            return hasPlayerJoinHandler;
        }
    }

    /**
     * Formatiert einen String mit Hex-Farbcodes
     * 
     * @param message Die zu formatierende Nachricht
     * @return Die formatierte Nachricht
     */
    public String formatHex(String message) {
        if (message == null) return "";
        
        // Zuerst Hex-Farben ersetzen (z.B. #RRGGBB)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 32);
        
        while (matcher.find()) {
            String hex = matcher.group();
            
            // Konvertiere #RRGGBB zu §x§r§r§g§g§b§b
            char[] chars = hex.substring(1).toCharArray();
            StringBuilder builder = new StringBuilder("§x");
            for (char c : chars) {
                builder.append("§").append(c);
            }
            
            matcher.appendReplacement(buffer, builder.toString());
        }
        matcher.appendTail(buffer);
        
        // Dann die & Farbcodes ersetzen
        String result = buffer.toString();
        result = ChatColor.translateAlternateColorCodes('&', result);
        
        return result;
    }

    @Override
    public void onEnable() {
        try {
            // Konfigurationsdateien initialisieren
            saveDefaultConfig();
            
            // Verzeichnisse initialisieren und sicherstellen, dass sie existieren
            setupDirectories();

            // Lade Konfiguration
            messagePrefix = getConfig().getString("general.message-prefix", "&8[&b&lApiCore&8] &7");
            debugMode = getConfig().getBoolean("general.debug-mode", false);
            
            // Erweiterte Konsolen-Formatter Konfiguration
            boolean useColors = getConfig().getBoolean("console.use-colors", true);
            boolean showTimestamps = getConfig().getBoolean("console.show-timestamps", false);
            boolean useUnicodeSymbols = getConfig().getBoolean("console.use-unicode-symbols", true);
            String stylePreset = getConfig().getString("console.style-preset", "default");
            
            // Konsolen-Formatter initialisieren mit bereits bereinigtem Präfix
            // Hier verwenden wir nicht formatHex, um Minecraft-Farbcodes zu vermeiden
            String rawPrefix = getConfig().getString("console.prefixes.core", "&8[&b&lApiCore&8]");
            console = new ConsoleFormatter(
                getLogger(),
                rawPrefix,
                useColors, showTimestamps, useUnicodeSymbols, stylePreset
            );
            
            // Lade Nachrichtenkategorien aus der Konfiguration
            console.loadCategoryConfigs(this);
            
            // Willkommensnachricht anzeigen
            console.header("ESSENTIALS CORE " + getDescription().getVersion());
            console.blank();
            
            // Konfiguration laden
            loadConfiguration();

            // Event-Listener registrieren
            registerListeners();
            
            // Module initialisieren
            initializeModules();
            
            // In Permissions einhaken, falls vorhanden
            if (permissionManager != null) {
                permissionManager.hookIntoPermissions();
                boolean permHookSuccess = permissionManager.isPermissionsHooked();
                console.status("Permissions-Hook", permHookSuccess ? "Erfolgreich" : "Nicht verfügbar", permHookSuccess);
            } else {
                console.categoryWarning(ConsoleFormatter.MessageCategory.SECURITY, "PermissionManager nicht initialisiert - Permission-Hook wird übersprungen");
            }
            
            // Befehle registrieren
            registerCommands();
            
            // Performance-Monitoring initialisieren
            initializePerformanceMonitoring();
            
            // Starte Modul-Watcher, falls konfiguriert
            startModuleWatcher();
            
            // Startnachricht
            console.doubleLine();
            console.categorySuccess(ConsoleFormatter.MessageCategory.SYSTEM, "EssentialsCore v" + getDescription().getVersion() + " wurde erfolgreich gestartet!");
            
            // Debug-Info ausgeben
            if (debugMode) {
                console.categoryDebug(ConsoleFormatter.MessageCategory.SYSTEM, "Debug-Modus ist aktiviert", true);
            }
            
            // Export API packages
            exportApiPackages();
        } catch (Exception e) {
            getLogger().severe("Fehler beim Starten des Plugins: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registriert die Event-Listener für das Plugin
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(this, this);
        console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Event-Listener registriert");
    }
    
    /**
     * Initialisiert die Module des Plugins
     */
    private void initializeModules() {
        // Thread-Manager initialisieren
        threadManager = new ThreadManager(this);
        executorService = threadManager.getExecutorService();
        console.categoryInfo(ConsoleFormatter.MessageCategory.THREAD, "Thread-Manager initialisiert");
        
        // Manager initialisieren
        configManager = new ConfigManager(this);
        permissionManager = new PermissionManager(this);
        moduleFileManager = new ModuleFileManager(this);
        resourceManager = new ModuleResourceManager(this);
        
        // ModuleManager initialisieren
        moduleManager = new ModuleManager(this, modulesDir, configDir, 
            (Map<String, Object>)(Map<String, ?>)loadedModules, 
            moduleCommands, executorService);
        
        // CommandManager initialisieren
        commandManager = new CommandManager(this);
        
        console.categorySuccess(ConsoleFormatter.MessageCategory.SYSTEM, "Alle Module erfolgreich initialisiert");
        
        // Module laden, falls aktiviert
        if (getConfig().getBoolean("general.auto-load-modules", true)) {
            console.subHeader("MODULE LADEN");
            moduleManager.loadModulesWithDependencyResolution();
            
            // Ressourcen extrahieren, falls konfiguriert
            if (getConfig().getBoolean("general.extract-module-resources", true)) {
                extractModuleResources();
            }
        } else {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Automatisches Laden der Module ist deaktiviert");
        }
    }
    
    /**
     * Registriert die Core-Befehle des Plugins
     */
    private void registerCommands() {
        // Core-Befehle über CommandManager registrieren
        if (commandManager != null) {
            commandManager.registerCoreCommands();
            console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Core-Befehle registriert");
        } else {
            console.categoryWarning(ConsoleFormatter.MessageCategory.SYSTEM, "CommandManager ist nicht initialisiert!");
        }
    }

    @Override
    public void onDisable() {
        console.subHeader("DEAKTIVIERUNG VON APICORE");
        // Code für die Deaktivierung hier einfügen
        console.categorySuccess(ConsoleFormatter.MessageCategory.SYSTEM, "EssentialsCore wurde deaktiviert");
    }

    /**
     * Initialisiert die Plugin-Verzeichnisse
     */
    private void setupDirectories() {
        configDir = new File(getDataFolder(), "config");
        modulesDir = new File(getDataFolder(), "modules");
        dataDir = new File(getDataFolder(), "data");
        
        // Verzeichnisse erstellen, falls sie nicht existieren
        getDataFolder().mkdirs();
        configDir.mkdirs();
        modulesDir.mkdirs();
        dataDir.mkdirs();
    }
    
    /**
     * Lädt die Konfiguration des Plugins
     */
    private void loadConfiguration() {
        saveDefaultConfig();
        reloadConfig();
        
        // Lade Konfigurationswerte
        debugMode = getConfig().getBoolean("general.debug-mode", false);
        messagePrefix = getConfig().getString("general.message-prefix", "&8[&b&lApiCore&8] &7");
        
        // Lade erweiterte Konfigurationswerte
        initializeAdvancedConfig();
    }
    
    /**
     * Initialisiert erweiterte Konfigurationswerte
     */
    private void initializeAdvancedConfig() {
        // Thread-Pool Konfiguration
        int threadPoolSize = getConfig().getInt("performance.thread-pool-size", 4);
        boolean threadMonitoring = getConfig().getBoolean("performance.thread-monitoring", true);
        
        // Cache-Größen konfigurieren
        int cacheSize = getConfig().getInt("performance.cache-size", 32);
        if (methodCache instanceof LinkedHashMap) {
            // Setze Cache-Kapazität wenn möglich
            try {
                java.lang.reflect.Field thresholdField = LinkedHashMap.class.getDeclaredField("threshold");
                thresholdField.setAccessible(true);
                thresholdField.set(methodCache, cacheSize);
                if (debugMode) {
                    console.debug("Methoden-Cache-Größe auf " + cacheSize + " gesetzt", true);
                }
            } catch (Exception e) {
                if (debugMode) {
                    console.debug("Konnte Cache-Größe nicht setzen: " + e.getMessage(), true);
                }
            }
        }
        
        // Speicheroptimierung
        String memoryOptimization = getConfig().getString("performance.memory-optimization", "medium");
        initializeMemoryOptimization(memoryOptimization);
        
        // Permissions-Cache konfigurieren
        boolean permCacheEnabled = getConfig().getBoolean("permissions.cache-enabled", true);
        if (!permCacheEnabled) {
            permissionExactCache.clear();
            if (debugMode) {
                console.debug("Permissions-Cache wurde deaktiviert", true);
            }
        }
        
        // Logging-Konfiguration
        configureLogging();
    }
    
    /**
     * Konfiguriert die Speicheroptimierungen basierend auf dem gewählten Level
     * 
     * @param level Das Optimierungslevel (low, medium, high)
     */
    private void initializeMemoryOptimization(String level) {
        switch (level.toLowerCase()) {
            case "high":
                // Aggressive Optimierungen
                largeDataCache = new OffHeapCache(256 * 1024 * 1024); // 256 MB
                formattedMessageCache.clear(); // Deaktiviere Message-Caching für Speichereffizienz
                System.gc(); // Fordere GC explizit an
                if (debugMode) {
                    console.debug("High-Memory-Optimierung aktiviert (aggressive Einstellungen)", true);
                }
                break;
            case "medium":
                // Ausgewogene Optimierungen
                largeDataCache = new OffHeapCache(128 * 1024 * 1024); // 128 MB
                if (debugMode) {
                    console.debug("Medium-Memory-Optimierung aktiviert (ausgewogene Einstellungen)", true);
                }
                break;
            case "low":
            default:
                // Minimale Optimierungen
                largeDataCache = new OffHeapCache(64 * 1024 * 1024); // 64 MB
                if (debugMode) {
                    console.debug("Low-Memory-Optimierung aktiviert (minimale Einstellungen)", true);
                }
                break;
        }
    }
    
    /**
     * Konfiguriert das Logging basierend auf den Einstellungen
     */
    private void configureLogging() {
        String logLevel = getConfig().getString("logging.level", "INFO");
        boolean writeToFile = getConfig().getBoolean("logging.write-to-file", true);
        
        try {
            // Setze Log-Level
            java.util.logging.Level level = java.util.logging.Level.parse(logLevel);
            getLogger().setLevel(level);
            
            if (debugMode) {
                console.debug("Log-Level auf " + logLevel + " gesetzt", true);
            }
            
            // Konfiguriere File-Logging wenn aktiviert
            if (writeToFile) {
                setupFileLogging();
            }
        } catch (IllegalArgumentException e) {
            console.warning("Ungültiges Log-Level: " + logLevel + ", verwende INFO");
            getLogger().setLevel(java.util.logging.Level.INFO);
        }
    }
    
    /**
     * Richtet das Logging in Dateien ein
     */
    private void setupFileLogging() {
        int maxLogSize = getConfig().getInt("logging.max-log-size", 10); // In MB
        int maxLogFiles = getConfig().getInt("logging.max-log-files", 5);
        
        try {
            File logDir = new File(getDataFolder(), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // Konfiguriere einen FileHandler mit Rotation
            java.util.logging.FileHandler fileHandler = new java.util.logging.FileHandler(
                logDir.getAbsolutePath() + "/apicore.log", 
                maxLogSize * 1024 * 1024, // Umrechnung in Bytes
                maxLogFiles, 
                true // Append-Modus
            );
            
            fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
            getLogger().addHandler(fileHandler);
            
            if (debugMode) {
                console.debug("File-Logging konfiguriert: " + logDir.getAbsolutePath() + "/apicore.log", true);
            }
        } catch (Exception e) {
            console.warning("Konnte File-Logging nicht konfigurieren: " + e.getMessage());
        }
    }
    
    /**
     * Startet den Modul-Watcher basierend auf den Konfigurationseinstellungen
     */
    private void startModuleWatcher() {
        boolean autoCheckModules = getConfig().getBoolean("modules.auto-check-modules", true);
        String loadMode = getConfig().getString("modules.load-mode", "MANUAL").toUpperCase();
        boolean fileWatcherEnabled = getConfig().getBoolean("modules.file-watcher-enabled", true);
        
        if (fileWatcherEnabled && moduleManager != null) {
            int interval = getConfig().getInt("modules.watcher-interval", 5);
            moduleManager.startModuleFileWatcher();
            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Modul-Dateiwatcher gestartet (Intervall: " + interval + "s)");
        }
        
        if (autoCheckModules && !loadMode.equals("MANUAL")) {
            scheduleModuleChecker();
        }
    }
    
    /**
     * Plant den regelmäßigen Modul-Checker ein
     */
    private void scheduleModuleChecker() {
        int checkInterval = getConfig().getInt("modules.check-interval", 300);
        boolean hotReload = getConfig().getBoolean("modules.hot-reload", true);
        
        // Konvertiere in Ticks (20 Ticks = 1 Sekunde)
        long intervalTicks = checkInterval * 20L;
        
        BukkitTask task = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (debugMode) {
                console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Prüfe auf neue Module...", true);
            }
            
            // Führe die Modulprüfung asynchron aus
            try {
                List<String> newModules = moduleManager.getAvailableButNotLoadedModules();
                
                if (!newModules.isEmpty()) {
                    console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Neue Module gefunden: " + String.join(", ", newModules));
                    
                    // Lade neue Module, falls Hot-Reload aktiviert ist
                    if (hotReload) {
                        for (String moduleName : newModules) {
                            try {
                                File moduleFile = new File(modulesDir, moduleName + ".jar");
                                if (moduleFile.exists()) {
                                    getServer().getScheduler().runTask(this, () -> {
                                        try {
                                            moduleManager.loadModule(moduleFile);
                                            console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Modul " + moduleName + " wurde automatisch geladen");
                                        } catch (Exception e) {
                                            console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim automatischen Laden von " + moduleName + ": " + e.getMessage());
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Fehler bei der Modulprüfung für " + moduleName + ": " + e.getMessage());
                            }
                        }
                    } else {
                        console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Hot-Reload ist deaktiviert. Verwende /apicore reload zum Laden der neuen Module.");
                    }
                } else if (debugMode) {
                    console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Keine neuen Module gefunden", true);
                }
            } catch (Exception e) {
                console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Prüfen auf neue Module: " + e.getMessage());
            }
        }, intervalTicks, intervalTicks);
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, "Automatische Modulprüfung geplant (Intervall: " + checkInterval + "s)");
    }
    
    /**
     * Initialisiert das Performance-Monitoring
     */
    private void initializePerformanceMonitoring() {
        // Erstelle die Performance-Monitor-Instanz
        performanceMonitor = new PerformanceMonitor(this);
        
        // Erstelle die Benchmark-Instanz
        performanceBenchmark = new PerformanceBenchmark(this);
        
        // Initialisiere optimierten Thread-Pool wenn konfiguriert
        boolean useAdvancedPool = getConfig().getBoolean("performance.use-advanced-pool", false);
        int threadPoolSize = getConfig().getInt("performance.thread-pool-size", 4);
        
        if (useAdvancedPool) {
            advancedExecutor = new AdvancedWorkStealingPool(threadPoolSize);
            console.categoryInfo(ConsoleFormatter.MessageCategory.PERFORMANCE, "Advanced WorkStealing Pool initialisiert mit " + threadPoolSize + " Threads");
        }
        
        // Starte Thread-Monitoring falls aktiviert
        boolean threadMonitoring = getConfig().getBoolean("performance.thread-monitoring", true);
        if (threadMonitoring) {
            startThreadMonitoring();
        }
    }
    
    /**
     * Startet das Thread-Monitoring für Deadlock-Erkennung
     */
    private void startThreadMonitoring() {
        int monitoringInterval = getConfig().getInt("performance.monitoring-interval", 60);
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
            
            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                console.categoryWarning(ConsoleFormatter.MessageCategory.THREAD, "Deadlock erkannt in " + deadlockedThreads.length + " Threads!");
                
                // Log detaillierte Thread-Informationen
                for (long threadId : deadlockedThreads) {
                    ThreadInfo threadInfo = threadMXBean.getThreadInfo(new long[]{threadId}, true, true)[0];
                    console.categoryWarning(ConsoleFormatter.MessageCategory.THREAD, "Thread in Deadlock: " + threadInfo.getThreadName());
                    console.categoryWarning(ConsoleFormatter.MessageCategory.THREAD, "Status: " + threadInfo.getThreadState());
                    console.categoryWarning(ConsoleFormatter.MessageCategory.THREAD, "Stack-Trace: " + Arrays.toString(threadInfo.getStackTrace()));
                }
                
                // Automatische Behebung versuchen, falls konfiguriert
                boolean autoFix = getConfig().getBoolean("performance.auto-fix-deadlocks", false);
                if (autoFix) {
                    attemptDeadlockRecovery(deadlockedThreads);
                }
            }
            
            // Sammle und protokolliere Speichernutzung
            collectMemoryUsageStats();
        }, 20L * monitoringInterval, 20L * monitoringInterval);
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.THREAD, "Thread-Monitoring aktiviert (Intervall: " + monitoringInterval + "s)");
    }
    
    /**
     * Versucht, einen Deadlock zu beheben
     * 
     * @param deadlockedThreads IDs der Threads im Deadlock
     */
    private void attemptDeadlockRecovery(long[] deadlockedThreads) {
        console.categoryWarning(ConsoleFormatter.MessageCategory.THREAD, "Versuche Deadlock zu beheben...");
        
        // Sammle betroffene Thread-Objekte
        List<Thread> threadsToInterrupt = new ArrayList<>();
        
        for (long threadId : deadlockedThreads) {
            ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(threadId);
            
            // Finde Thread-Objekt
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread.getId() == threadId || thread.getName().equals(threadInfo.getThreadName())) {
                    threadsToInterrupt.add(thread);
                    break;
                }
            }
        }
        
        // Versuche Interruption
        for (Thread thread : threadsToInterrupt) {
            try {
                console.categoryWarning(ConsoleFormatter.MessageCategory.THREAD, "Unterbreche Thread: " + thread.getName());
                thread.interrupt();
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.THREAD, "Konnte Thread nicht unterbrechen: " + e.getMessage());
            }
        }
    }
    
    /**
     * Sammelt und protokolliert Speichernutzungsstatistiken
     */
    private void collectMemoryUsageStats() {
        if (!debugMode) return;
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        
        double memoryUsagePercentage = ((double) usedMemory / maxMemory) * 100;
        
        console.categoryDebug(ConsoleFormatter.MessageCategory.PERFORMANCE, "Speichernutzung: " + usedMemory + "MB / " + maxMemory + 
                   "MB (" + String.format("%.1f", memoryUsagePercentage) + "%)", true);
        
        // Warnung bei hoher Speichernutzung
        if (memoryUsagePercentage > 85) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.PERFORMANCE, "Hohe Speichernutzung: " + String.format("%.1f", memoryUsagePercentage) + "%");
        }
    }
    
    /**
     * Extrahiert Ressourcen aus den Modulen mit erweiterter Konfiguration
     */
    public void extractModuleResources() {
        if (resourceManager == null) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.RESOURCE, "ResourceManager nicht initialisiert - Ressourcen können nicht extrahiert werden");
            return;
        }
        
        // Lade die Konfigurationseinstellungen für die Ressourcenextraktion
        String extractionMode = getConfig().getString("modules.resources.extraction-mode", "AUTO").toUpperCase();
        boolean overwriteExisting = getConfig().getBoolean("modules.resources.overwrite-existing", false);
        boolean backupOnOverwrite = getConfig().getBoolean("modules.resources.backup-on-overwrite", true);
        boolean verifyIntegrity = getConfig().getBoolean("modules.resources.verify-integrity", false);
        List<String> selectiveModules = getConfig().getStringList("modules.resources.selective-modules");
        List<String> defaultFileTypes = getConfig().getStringList("modules.resources.default-file-types");
        List<String> extractSubdirectories = getConfig().getStringList("modules.resources.extract-subdirectories");
        List<String> ignorePatterns = getConfig().getStringList("modules.resources.ignore-patterns");
        
        // Konfiguration auf den ResourceManager anwenden
        resourceManager.setOverwriteExisting(overwriteExisting);
        resourceManager.setBackupOnOverwrite(backupOnOverwrite);
        resourceManager.setVerifyIntegrity(verifyIntegrity);
        
        if (!defaultFileTypes.isEmpty()) {
            resourceManager.setDefaultFileTypes(defaultFileTypes);
        }
        
        if (!extractSubdirectories.isEmpty()) {
            resourceManager.setExtractSubdirectories(extractSubdirectories);
        }
        
        if (!ignorePatterns.isEmpty()) {
            resourceManager.setIgnorePatterns(ignorePatterns);
        }
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.RESOURCE, "Extrahiere Ressourcen aus Modulen im Modus: " + extractionMode);
        
        switch (extractionMode) {
            case "AUTO":
                // Alle geladenen Module extrahieren
            for (String moduleName : loadedModules.keySet()) {
                resourceManager.extractModuleResources(moduleName);
            }
                break;
                
            case "SELECTIVE":
                // Nur bestimmte Module extrahieren
                if (selectiveModules.isEmpty()) {
                    console.categoryWarning(ConsoleFormatter.MessageCategory.RESOURCE, "Selective-Modus aktiviert, aber keine Module konfiguriert!");
        } else {
                    for (String moduleName : selectiveModules) {
                        if (loadedModules.containsKey(moduleName)) {
                            resourceManager.extractModuleResources(moduleName);
                        } else {
                            console.categoryWarning(ConsoleFormatter.MessageCategory.RESOURCE, "Selektives Modul " + moduleName + " ist nicht geladen!");
                        }
                    }
                }
                break;
                
            case "MANUAL":
                // Keine automatische Extraktion
                console.categoryInfo(ConsoleFormatter.MessageCategory.RESOURCE, "Modulressourcen-Extraktion ist auf MANUAL gesetzt - keine automatische Extraktion");
                break;
                
            case "NONE":
                // Keine Extraktion
                console.categoryInfo(ConsoleFormatter.MessageCategory.RESOURCE, "Modulressourcen-Extraktion ist deaktiviert");
                break;
                
            default:
                console.categoryWarning(ConsoleFormatter.MessageCategory.RESOURCE, "Unbekannter Extraktionsmodus: " + extractionMode);
                break;
        }
    }

    /**
     * Setzt einen Wert im SharedData-Cache
     * 
     * @param key Der Schlüssel
     * @param value Der zu speichernde Wert
     */
    public void setSharedData(String key, Object value) {
        if (key != null && !key.isEmpty()) {
            if (value == null) {
                sharedData.remove(key);
            } else {
                sharedData.put(key, value);
            }
        }
    }

    /**
     * Gibt zurück, ob der Debug-Modus aktiviert ist
     * 
     * @return true, wenn der Debug-Modus aktiviert ist
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Gibt den Message-Prefix zurück
     * 
     * @return der konfigurierte Message-Prefix
     */
    public String getMessagePrefix() {
        return messagePrefix;
    }
    
    /**
     * Gibt die geladenen Module zurück
     * 
     * @return eine Map mit den geladenen Modulen
     */
    public Map<String, ModuleInfo> getLoadedModules() {
        return Collections.unmodifiableMap(loadedModules);
    }
    
    /**
     * Gibt Informationen zu einem Modul zurück
     * 
     * @param moduleName Der Name des Moduls
     * @return die ModuleInfo oder null, wenn das Modul nicht geladen ist
     */
    public ModuleInfo getModuleInfo(String moduleName) {
        return loadedModules.get(moduleName);
    }
    
    /**
     * Prüft, ob ein Spieler eine Berechtigung hat
     * 
     * @param player Der Spieler
     * @param permission Die zu prüfende Berechtigung
     * @return true, wenn der Spieler die Berechtigung hat
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null || permission.isEmpty()) {
            return true;
        }
        
        return permissionManager.hasPermission(player, permission);
    }
    
    /**
     * Ruft eine Methode auf einem Objekt auf
     * 
     * @param instance Die Instanz
     * @param methodName Der Methodenname
     * @param parameterTypes Die Parametertypen
     * @param args Die Argumente
     * @return Das Ergebnis des Methodenaufrufs
     * @throws Exception Wenn ein Fehler auftritt
     */
    public Object invokeMethod(Object instance, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        if (instance == null) {
            throw new IllegalArgumentException("Instanz darf nicht null sein");
        }
        
        Method method = instance.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(instance, args);
    }
    
    /**
     * Speichert eine Zeitmessung für eine Methode
     * 
     * @param moduleName Der Modulname
     * @param methodName Der Methodenname
     * @param startTime Die Startzeit in Nanosekunden
     */
    public void trackMethodTime(String moduleName, String methodName, long startTime) {
        long duration = System.nanoTime() - startTime;
        
        methodTimings.computeIfAbsent(moduleName, k -> new ConcurrentHashMap<>())
            .merge(methodName, duration / 1_000_000.0, Double::sum);
    }
    
    /**
     * Gibt den Datenordner eines Moduls zurück
     * 
     * @param moduleName Der Name des Moduls
     * @return Der Datenordner des Moduls
     */
    public File getModuleDataFolder(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return null;
        }
        
        File moduleDataDir = new File(dataDir, moduleName);
        if (!moduleDataDir.exists()) {
            moduleDataDir.mkdirs();
        }
        
        return moduleDataDir;
    }
    
    /**
     * Speichert eine Berechtigung im Cache
     * 
     * @param permissionName Die zu speichernde Berechtigung
     */
    public void cachePermission(String permissionName) {
        if (permissionName != null && !permissionName.isEmpty()) {
            permissionCache.add(permissionName);
        }
    }
    
    /**
     * Gibt die Größe des Berechtigungscaches zurück
     * 
     * @return Die Größe des Caches
     */
    public int getPermissionCacheSize() {
        return permissionExactCache.size();
    }

    /**
     * Gibt die Konfigurationsdatei eines Moduls zurück
     * 
     * @param moduleName Der Name des Moduls
     * @return Die Konfigurationsdatei des Moduls
     */
    public File getModuleConfigFile(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return null;
        }
        
        File configFile = new File(configDir, moduleName + ".yml");
        return configFile;
    }

    /**
     * Gibt den Ressourcen-Ordner eines Moduls zurück
     * 
     * @param moduleName Der Name des Moduls
     * @return Der Ressourcen-Ordner des Moduls
     */
    public File getModuleResourcesFolder(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return null;
        }
        
        File resourcesFolder = new File(getModuleDataFolder(moduleName), "resources");
        if (!resourcesFolder.exists()) {
            resourcesFolder.mkdirs();
        }
        
        return resourcesFolder;
    }

    /**
     * Initialisiert ein Modul
     * 
     * @param moduleName Der Name des Moduls
     * @param moduleInstance Die Modul-Instanz
     * @param config Die Modul-Konfiguration
     */
    public void initializeModule(String moduleName, Object moduleInstance, FileConfiguration config) {
        // Create the ModuleAPI instance for this module if it doesn't exist
        ModuleAPI moduleAPI = moduleAPIs.computeIfAbsent(moduleName, k -> new CoreModuleAPI(this, k));
        
        if (moduleInstance instanceof Module) {
            // New API interface
            ((Module) moduleInstance).init(moduleAPI, config);
        } else {
            // Use reflection for legacy modules
            try {
                Method initMethod = moduleInstance.getClass().getMethod("init", ApiCore.class, FileConfiguration.class);
                initMethod.invoke(moduleInstance, this, config);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to initialize module " + moduleName, e);
            }
        }
    }

    /**
     * Registriert Befehle für ein Modul
     * 
     * @param commands Die zu registrierenden Befehle
     */
    public void registerCommands(List<DynamicCommand> commands) {
        if (commandManager != null) {
            commandManager.registerCommands(commands);
        }
    }

    /**
     * Deregistriert Befehle für ein Modul
     * 
     * @param commands Die zu deregistrierenden Befehle
     */
    public void unregisterCommands(List<DynamicCommand> commands) {
        if (commandManager != null) {
            commandManager.unregisterCommands(commands);
        }
    }

    /**
     * Deaktiviert ein Modul
     * 
     * @param moduleName Der Name des Moduls
     * @return true, wenn das Modul erfolgreich deaktiviert wurde
     */
    public boolean disableModule(String moduleName) {
        if (moduleManager != null) {
            try {
                moduleManager.unloadModule(moduleName);
                return true;
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Fehler beim Deaktivieren des Moduls " + moduleName, e);
            }
        }
        return false;
    }

    /**
     * Leert den Methoden-Cache für bestimmte Methoden
     * 
     * @param methodPrefix Der Präfix der zu löschenden Methoden
     */
    public void cleanMethodCache(String methodPrefix) {
        if (methodPrefix == null || methodPrefix.isEmpty()) {
            methodCache.clear();
            methodHandleCache.clear();
            return;
        }
        
        // Entferne Methoden, die mit dem angegebenen Präfix beginnen
        methodCache.entrySet().removeIf(entry -> entry.getKey().startsWith(methodPrefix));
        methodHandleCache.entrySet().removeIf(entry -> entry.getKey().startsWith(methodPrefix));
    }

    /**
     * Gibt zurück, ob der PermissionsManager erfolgreich eingehakt wurde
     * 
     * @return true, wenn die Permissions erfolgreich eingehakt wurden
     */
    public boolean isPermissionsHooked() {
        return permissionManager != null && permissionManager.isPermissionsHooked();
    }

    /**
     * Gibt ein geteiltes Datenobjekt zurück
     * 
     * @param key Der Schlüssel des Datenobjekts
     * @return Das geteilte Datenobjekt oder null, wenn es nicht existiert
     */
    public Object getSharedData(String key) {
        return sharedData.get(key);
    }

    /**
     * Gibt den ThreadManager zurück
     * 
     * @return ThreadManager-Instanz
     */
    public ThreadManager getThreadManager() {
        return threadManager;
    }
    
    /**
     * Gibt den PermissionManager zurück
     * 
     * @return PermissionManager-Instanz
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    /**
     * Gibt den ModuleManager zurück
     * @return Der ModuleManager
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * Gibt den ModuleFileManager zurück
     * @return Der ModuleFileManager
     */
    public ModuleFileManager getModuleFileManager() {
        return moduleFileManager;
    }
    
    /**
     * Gibt den PerformanceMonitor zurück
     * 
     * @return PerformanceMonitor-Instanz
     */
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    /**
     * Gibt den PerformanceBenchmark zurück
     * 
     * @return PerformanceBenchmark-Instanz
     */
    public PerformanceBenchmark getPerformanceBenchmark() {
        return performanceBenchmark;
    }

    /**
     * Gets the ModuleAPI for a specific module
     * 
     * @param moduleName The name of the module
     * @return The ModuleAPI instance for the module
     */
    public ModuleAPI getModuleAPI(String moduleName) {
        return moduleAPIs.computeIfAbsent(moduleName, k -> new CoreModuleAPI(this, k));
    }

    /**
     * This method ensures all API packages are properly exported to modules.
     * It will be called during plugin initialization to make all API classes accessible.
     */
    private void exportApiPackages() {
        // Log that we're exporting API packages
        getLogger().info("Exporting API packages for modules...");
        
        // Pre-load all API classes to ensure they're available from the plugin classloader
        try {
            // Core API interfaces
            Class.forName("com.essentialscore.api.Module");
            Class.forName("com.essentialscore.api.ModuleAPI");
            Class.forName("com.essentialscore.api.ModuleEventListener");
            Class.forName("com.essentialscore.api.CommandDefinition");
            Class.forName("com.essentialscore.api.BaseModule");
            Class.forName("com.essentialscore.api.ModuleClassHelper");
            
            // Implementation classes
            Class.forName("com.essentialscore.api.impl.CoreModuleAPI");
            Class.forName("com.essentialscore.api.impl.ModuleAdapter");
            Class.forName("com.essentialscore.api.SimpleCommand");
            
            // Explicitly call the helper to ensure all classes are loaded
            com.essentialscore.api.ModuleClassHelper.ensureApiClassesLoaded();
            
            getLogger().info("Successfully exported API packages for modules");
        } catch (ClassNotFoundException e) {
            getLogger().severe("Failed to export API packages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lädt alle Module neu
     * Diese Methode verwendet den ModuleManager, um alle Module sauber neu zu laden
     */
    public void reloadModules() {
        console.header("MODULE NEU LADEN");
        
        if (moduleManager != null) {
            // Rufe die verbesserte Reload-Methode des ModuleManagers auf
            moduleManager.reloadAllModules();
            
            // Ressourcen nach dem Neuladen extrahieren, falls konfiguriert
            if (getConfig().getBoolean("general.extract-module-resources", true)) {
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    console.info("Extrahiere Modul-Ressourcen nach Reload...");
                    extractModuleResources();
                });
            }
            
            // Alle Caches leeren, die auf Module verweisen könnten
            formattedMessageCache.clear();
            permissionCache.clear();
            permissionExactCache.clear();
            
            // Stelle sicher, dass alle Module korrekt registriert sind
            console.info("Module wurden neu geladen. Anzahl: " + loadedModules.size());
        } else {
            console.error("ModuleManager ist nicht initialisiert!");
        }
    }
}
        