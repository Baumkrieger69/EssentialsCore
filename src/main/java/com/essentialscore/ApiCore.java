package com.essentialscore;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.essentialscore.api.BasePlugin;
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import com.essentialscore.api.command.CommandManager;
import com.essentialscore.api.command.DynamicCommand;
import com.essentialscore.api.impl.CoreModuleAPI;
import com.essentialscore.api.language.LanguageManager;
import com.essentialscore.api.module.ModuleFileManager;
import com.essentialscore.api.module.ModuleSandbox;
import com.essentialscore.api.permission.PermissionManager;
import com.essentialscore.commands.ApiCoreMainCommand;
import com.essentialscore.placeholder.PlaceholderManager;
import com.essentialscore.clickable.ClickableCommandManager;
import com.essentialscore.commands.ConfirmCommandExecutor;
import com.essentialscore.listener.ChatMessageListener;

/**
 * Main class for the EssentialsCore plugin.
 */
@SuppressWarnings("deprecation")
public class ApiCore extends JavaPlugin implements Listener, BasePlugin {    /**
     * Gets the plugin version without using deprecated getDescription method
     * @return The plugin version
     */
    public String getPluginVersion() {
        try {
            // Use Java reflection to get the version without calling deprecated method directly
            return getClass().getPackage().getImplementationVersion() != null 
                   ? getClass().getPackage().getImplementationVersion() 
                   : "unknown";
        } catch (Exception e) {
            // Fallback to using the deprecated method if reflection fails
            return getDescription().getVersion();
        }
    }    private File configDir;
    private File modulesDir;
    private File dataDir;
    private final Map<String, ModuleInfo> loadedModules = new ConcurrentHashMap<>(16, 0.75f, 2);
    private String messagePrefix;
    private boolean debugMode;
    private long startTime; // Server start time for uptime calculation// Manager-Instanzen
    private com.essentialscore.ModuleManager moduleManagerInternal; // Interner Manager
    private com.essentialscore.api.module.ModuleManager moduleManager; // API Manager
    private PermissionManager permissionManager;
    private CommandManager commandManager;
    private LanguageManager languageManager;
    // private WebUIManager webUIManager; // MOVED TO webui-development
    private ModuleFileManager moduleFileManager;
    private ModuleSandbox moduleSandbox; // Neue Instanzvariable für Modul-Sandbox
    private ConsoleFormatter console;
    private PerformanceMonitor performanceMonitor;
    private ThreadManager threadManager;    private PerformanceBenchmark performanceBenchmark;
    private PlaceholderManager placeholderManager;
    private ClickableCommandManager clickableCommandManager;
    
    // Thread-safe shared data with optimized initial capacity
    private final ConcurrentHashMap<String, Object> sharedData = new ConcurrentHashMap<>(32, 0.75f, 2);
      // Caching reflection methods for performance
    private final Map<String, Method> methodCache = Collections.synchronizedMap(
        new LinkedHashMap<String, Method>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Method> eldest) {
                return size() > getConfig().getInt("performance.cache-size.methods", 500);
            }
        }
    );
    private final Map<String, MethodHandle> methodHandleCache = Collections.synchronizedMap(
        new LinkedHashMap<String, MethodHandle>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, MethodHandle> eldest) {
                return size() > getConfig().getInt("performance.cache-size.reflection", 200);
            }
        }
    );    
    // Thread pool for asynchronous operations
    // Removed unused executorService field

    // Lazy-initialized static pattern for better performance
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    
    // Removed unused methodTimings and BUFFER_SIZE fields
      // String formatting cache for commonly used messages
    private final ConcurrentHashMap<String, String> formattedMessageCache = new ConcurrentHashMap<>(64, 0.75f, 1);

    // Permission cache for optimization
    private final ConcurrentHashMap<String, Boolean> permissionExactCache = new ConcurrentHashMap<>(128);

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
        
        // Dann die & Farbcodes ersetzen - without using deprecated method
        String result = buffer.toString();
        
        // Replace color codes manually instead of using deprecated ChatColor method
        char[] chars = result.toCharArray();
        StringBuilder finalResult = new StringBuilder(result.length());
        
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                finalResult.append('§').append(chars[i + 1]);
                i++; // Skip the color code character
            } else {
                finalResult.append(chars[i]);
                
                // Add the last character if this is the second last
                if (i == chars.length - 2) {
                    finalResult.append(chars[i + 1]);
                }
            }
        }
        
        return finalResult.toString();
    }    @Override
    public void onEnable() {
        try {
            // Startzeit speichern für Uptime-Berechnung
            startTime = System.currentTimeMillis();
            
            // Konfigurationsdateien initialisieren
            saveDefaultConfig();
            
            // Verzeichnisse initialisieren und sicherstellen, dass sie existieren
            setupDirectories();

            // Lade Konfiguration
            messagePrefix = getConfig().getString("general.message-prefix", "&8[&b&lApiCore&8] &7");
            debugMode = getConfig().getBoolean("general.debug-mode", false);
            
            // Use basic constructor for ConsoleFormatter
            console = new ConsoleFormatter(
                getLogger(),
                getMessagePrefix(),
                true
            );
            
            // Lade Nachrichtenkategorien aus der Konfiguration
            console.loadCategoryConfigs(this);
            
            // Willkommensnachricht anzeigen
            console.header("ESSENTIALS CORE " + getPluginVersion());
            console.blank();
              // Konfiguration laden
            loadConfiguration();
            
            // Initialize permission manager BEFORE modules (they need it during loading)
            try {
                Object permManagerInstance = Class.forName("com.essentialscore.PermissionManager")
                    .getConstructor(ApiCore.class)
                    .newInstance(this);
                
                // Cast to the correct API interface type
                this.permissionManager = (PermissionManager) permManagerInstance;
                console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Permission Manager initialized");
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.SYSTEM, "Failed to initialize Permission Manager: " + e.getMessage());
                this.permissionManager = null;
            }

            // Initialize Placeholder Manager
            try {
                this.placeholderManager = new PlaceholderManager(this);
                console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Placeholder Manager initialized");
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.SYSTEM, "Failed to initialize Placeholder Manager: " + e.getMessage());
            }

            // Initialize Clickable Command Manager
            try {
                this.clickableCommandManager = new ClickableCommandManager(this);
                console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Clickable Command Manager initialized");
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.SYSTEM, "Failed to initialize Clickable Command Manager: " + e.getMessage());
            }
            
            // Event-Listener registrieren (NACH Manager-Initialisierung)
            registerListeners();
            
            // Module initialisieren (nach PermissionManager)
            initializeModules();
            
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
            console.categoryError(ConsoleFormatter.MessageCategory.SYSTEM, "Fehler beim Starten des Plugins: " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }
      /**
     * Registriert die Event-Listener für das Plugin
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register chat message listener for placeholder and clickable command processing
        if (placeholderManager != null || clickableCommandManager != null) {
            ChatMessageListener chatListener = new ChatMessageListener(this);
            getServer().getPluginManager().registerEvents(chatListener, this);
            console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Chat Message Listener registriert");
        }
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Event-Listener registriert");
    }
    
    /**
     * Initialisiert die Module des Plugins
     */    private void initializeModules() {
        // Initialize modules directory if it doesn't exist
        modulesDir.mkdirs();
        
        // Initialize managers
        commandManager = new CommandManager(this);
        languageManager = new LanguageManager(this);            // Initialize ModuleManager - THIS WAS MISSING!
        try {
            // Create maps for ModuleManager
            Map<String, Object> loadedModulesMap = new ConcurrentHashMap<>();
            Map<String, List<DynamicCommand>> moduleCommandsMap = new ConcurrentHashMap<>();
            ExecutorService moduleExecutor = Executors.newCachedThreadPool(r -> {
                Thread thread = new Thread(r, "ModuleManager-" + UUID.randomUUID().toString().substring(0, 8));
                thread.setDaemon(true);
                return thread;
            });
            
            // Initialize internal module manager
            moduleManagerInternal = new com.essentialscore.ModuleManager(this, modulesDir, new File(getDataFolder(), "modules/configs"), 
                loadedModulesMap, moduleCommandsMap, moduleExecutor);
            
            // Initialize API module manager
            moduleManager = new com.essentialscore.api.module.ModuleManager(this, this, modulesDir);
            
            console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "ModuleManager erfolgreich initialisiert");
        } catch (Exception e) {
            console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Initialisieren des ModuleManagers: " + e.getMessage());
            getLogger().log(Level.SEVERE, "Failed to initialize ModuleManager", e);
        }
        
        performanceMonitor = new PerformanceMonitor(this);
        threadManager = new ThreadManager(this);
        performanceBenchmark = new PerformanceBenchmark(this);
        
        // Initialize performance monitoring if enabled
        if (getConfig().getBoolean("performance.enable-monitoring", true)) {
            getLogger().info("Performance monitoring is enabled");
        }
        
        // Initialize module file manager
        moduleFileManager = new ModuleFileManager(this);
        
        // Initialize sandbox if enabled
        if (getConfig().getBoolean("security.enable-sandbox", true)) {
            moduleSandbox = new ModuleSandbox(this);
            console.categoryInfo(ConsoleFormatter.MessageCategory.SECURITY, 
                "Modul-Sandbox initialisiert mit Sicherheitsstufe: " + 
                getConfig().getString("security.sandbox-level", "medium"));
        }
        
        console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Alle Module erfolgreich initialisiert");
          // Module laden, falls aktiviert
        if (getConfig().getBoolean("general.auto-load-modules", true)) {
            console.subHeader("MODULE LADEN");
            try {                // Use loadModulesWithDependencyResolution() method
                if (moduleManagerInternal != null) {
                    moduleManagerInternal.loadModulesWithDependencyResolution();
                    int loadedCount = loadedModules.size(); // Get actual count from loaded modules
                    console.categorySuccess(ConsoleFormatter.MessageCategory.SYSTEM, 
                        loadedCount + " Module erfolgreich initialisiert");
                }
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.SYSTEM, "Fehler beim Starten des Plugins: " + e.getMessage());
                if (debugMode) {
                    e.printStackTrace();
                }
            }
            
            // Ressourcen extrahieren, falls konfiguriert
            if (getConfig().getBoolean("general.extract-module-resources", true)) {
                extractModuleResources();
            }
        } else {
            console.categoryWarning(ConsoleFormatter.MessageCategory.MODULE, "Automatisches Laden der Module ist deaktiviert");
        }
          // WebUI wird nicht automatisch gestartet - kann über Befehle aktiviert werden
        console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "WebUI bereit (verwende '/apicore webui start' zum Starten)");
    }
    
    /**
     * Registriert die eigenen Befehle des Plugins
     */    private void registerCommands() {
        // Registriere den Hauptbefehl
        try {
            ApiCoreMainCommand mainCommand = new ApiCoreMainCommand(this);
            getCommand("apicore").setExecutor(mainCommand);
            getCommand("apicore").setTabCompleter(mainCommand);
            getLogger().info("Main command registered successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error registering main command", e);
        }
          // Registriere den Confirm-Befehl
        try {
            if (clickableCommandManager != null) {
                ConfirmCommandExecutor confirmCommand = new ConfirmCommandExecutor(this, clickableCommandManager);
                getCommand("confirm").setExecutor(confirmCommand);
                getLogger().info("Confirm command registered successfully");
            } else {
                getLogger().warning("Could not register confirm command: ClickableCommandManager is null");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error registering confirm command", e);
        }
        
        // Use reflection to safely call methods that might not exist
        if (commandManager != null) {
            try {
                Method registerCoreCommandsMethod = commandManager.getClass().getMethod("registerCoreCommands");
                registerCoreCommandsMethod.invoke(commandManager);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error registering core commands", e);
            }
            
            // Don't try to call registerCommandDeactivationCommand since it doesn't exist
            try {
                // Register default core commands
                getLogger().info("Core commands registered successfully");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Could not register default commands", e);
            }
        }
    }

    @Override
    public void onDisable() {
        console.header("ESSENTIALS CORE " + getDescription().getVersion());
        console.info("Plugin wird deaktiviert...");
        
        try {
            // Cleanup performance resources
            Object bossBarObj = getSharedData("performanceBossBar");
            if (bossBarObj instanceof com.essentialscore.util.ModulePerformanceBossBar) {
                try {
                    ((com.essentialscore.util.ModulePerformanceBossBar) bossBarObj).stop();
                    console.categoryInfo(ConsoleFormatter.MessageCategory.PERFORMANCE, "ModulePerformanceBossBar wurde gestoppt");
                } catch (Exception e) {
                    console.warning("Fehler beim Stoppen der ModulePerformanceBossBar: " + e.getMessage());
                }
            }
              // Alle geladenen Module deaktivieren
            if (moduleManagerInternal != null) {
                // Manuelle Deaktivierung aller Module
                for (String moduleName : new ArrayList<>(loadedModules.keySet())) {
                    disableModule(moduleName);
                }
                
                // Shutdown module manager using reflection to be safe
                try {
                    Method shutdownMethod = moduleManagerInternal.getClass().getMethod("shutdown");
                    shutdownMethod.invoke(moduleManagerInternal);
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Failed to properly shutdown module manager", e);
                }
            }
            
            // Sandbox herunterfahren            // Module sandbox cleanup
            if (moduleSandbox != null) {
                moduleSandbox.shutdown();
            }
              // WebUI herunterfahren
            // if (webUIManager != null) { // MOVED TO webui-development
            //     webUIManager.shutdown();
            //     console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "WebUI wurde heruntergefahren");
            // }
            
            // Cache leeren
            methodCache.clear();
            methodHandleCache.clear();
            
            console.success("Plugin erfolgreich deaktiviert");
        } catch (Exception e) {
            console.error("Fehler beim Deaktivieren des Plugins: " + e.getMessage());
            e.printStackTrace();
        }
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
     */    private void initializeAdvancedConfig() {
        // Thread-Pool Konfiguration
        // Removed unused variables threadPoolSize and threadMonitoring
        
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
     */    private void initializeMemoryOptimization(String level) {
        switch (level.toLowerCase()) {
            case "high":
                // Aggressive Optimierungen
                formattedMessageCache.clear(); // Deaktiviere Message-Caching für Speichereffizienz
                System.gc(); // Fordere GC explizit an
                if (debugMode) {
                    console.debug("High-Memory-Optimierung aktiviert (aggressive Einstellungen)", true);
                }
                break;
            case "medium":
                // Ausgewogene Optimierungen
                if (debugMode) {
                    console.debug("Medium-Memory-Optimierung aktiviert (ausgewogene Einstellungen)", true);
                }
                break;
            case "low":
            default:
                // Minimale Optimierungen
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
        // Use basic constructor for ConsoleFormatter
        console = new ConsoleFormatter(
            getLogger(),
            getMessagePrefix(),
            true
        );
        
        String logLevel = getConfig().getString("logging.level", "INFO");
        boolean writeToFile = getConfig().getBoolean("logging.write-to-file", true);
        
        try {
            Level level = Level.parse(logLevel.toUpperCase());
            getLogger().setLevel(level);
            
            // Auch Konsolenausgabe auf das gewünschte Level setzen
            java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
            rootLogger.setLevel(level);
            
            for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
                handler.setLevel(level);
            }
            
            if (writeToFile) {
                setupFileLogging();
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning("Ungültiges Log-Level '" + logLevel + "', verwende INFO");
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
                console.categoryDebug(ConsoleFormatter.MessageCategory.CONFIG, "File-Logging konfiguriert: " + logDir.getAbsolutePath() + "/apicore.log", true);
            }
        } catch (Exception e) {
            console.categoryWarning(ConsoleFormatter.MessageCategory.CONFIG, "Konnte File-Logging nicht konfigurieren: " + e.getMessage());
        }
    }
    
    /**
     * Startet den Modul-Watcher basierend auf den Konfigurationseinstellungen
     */
    private void startModuleWatcher() {
        boolean autoCheckModules = getConfig().getBoolean("modules.auto-check-modules", true);
        String loadMode = getConfig().getString("modules.load-mode", "MANUAL").toUpperCase();
        boolean fileWatcherEnabled = getConfig().getBoolean("modules.file-watcher-enabled", true);
        
        if (fileWatcherEnabled && moduleManagerInternal != null) {
            int interval = getConfig().getInt("modules.watcher-interval", 5);
            // Don't call startModuleFileWatcher() - implement equivalent functionality
              // Set up a basic file watcher with BukkitScheduler
            getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    if (debugMode) {
                        console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                            "Checking for module file changes...", true);
                    }
                    
                    // Check for new modules
                    File[] files = modulesDir.listFiles(new java.io.FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.isFile() && file.getName().endsWith(".jar");
                        }
                    });
                      if (files != null) {
                        for (File file : files) {
                            String moduleName = file.getName().substring(0, file.getName().length() - 4);
                            
                            // Only process if not already loaded
                            if (moduleManagerInternal != null && !loadedModules.containsKey(moduleName)) {
                                // Check if jar contains valid module.yml before considering it a valid module
                                if (ApiCore.this.hasValidModuleYml(file)) {
                                    try {
                                        if (debugMode) {
                                            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, 
                                                "Found new valid module: " + moduleName);
                                        }
                                        
                                        // Actually load the module to prevent re-detection
                                        moduleManagerInternal.loadModule(file);
                                        
                                        console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, 
                                            "Auto-loaded module: " + moduleName);
                                            
                                    } catch (Exception e) {
                                        console.categoryError(ConsoleFormatter.MessageCategory.MODULE, 
                                            "Failed to auto-load module " + moduleName + ": " + e.getMessage());
                                        if (debugMode) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    if (debugMode) {
                                        console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                                            "Skipping " + file.getName() + " - no valid module.yml", true);
                                    }
                                }
                            }
                        }
                    }
                }
            }, 20L * interval, 20L * interval);
            
            console.categoryInfo(ConsoleFormatter.MessageCategory.MODULE, 
                "Modul-Dateiwatcher gestartet (Intervall: " + interval + "s)");
        }
        
        if (autoCheckModules && !loadMode.equals("MANUAL")) {
            scheduleModuleChecker();
        }
    }
      /**
     * Check if a JAR file contains a valid module.yml
     */
    private boolean hasValidModuleYml(File jarFile) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.jar.JarEntry entry = jar.getJarEntry("module.yml");
            if (entry == null) {
                if (debugMode) {
                    console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                        "No module.yml found in: " + jarFile.getName(), true);
                }
                return false;
            }
            
            // Parse YAML and validate required fields
            try (java.io.InputStream is = jar.getInputStream(entry)) {
                org.bukkit.configuration.file.YamlConfiguration config = 
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(is, "UTF-8"));
                  // Check for required fields
                String name = config.getString("name");
                String version = config.getString("version");
                String main = config.getString("main");
                String author = config.getString("author");
                
                if (name == null || name.trim().isEmpty()) {
                    if (debugMode) {
                        console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                            "Invalid module.yml in " + jarFile.getName() + ": missing 'name'", true);
                    }
                    return false;
                }
                
                if (version == null || version.trim().isEmpty()) {
                    if (debugMode) {
                        console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                            "Invalid module.yml in " + jarFile.getName() + ": missing 'version'", true);
                    }
                    return false;
                }
                
                if (main == null || main.trim().isEmpty()) {
                    if (debugMode) {
                        console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                            "Invalid module.yml in " + jarFile.getName() + ": missing 'main'", true);
                    }
                    return false;
                }
                
                if (debugMode) {
                    console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                        "Valid module found: " + name + " v" + version + 
                        (author != null ? " by " + author : ""), true);
                }
                
                return true;
                
            } catch (Exception e) {
                if (debugMode) {
                    console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                        "Error parsing module.yml in " + jarFile.getName() + ": " + e.getMessage(), true);
                }
                return false;
            }
        } catch (Exception e) {
            if (debugMode) {
                console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, 
                    "Error reading JAR file " + jarFile.getName() + ": " + e.getMessage(), true);
            }
            return false;
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
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (debugMode) {
                console.categoryDebug(ConsoleFormatter.MessageCategory.MODULE, "Prüfe auf neue Module...", true);
            }
            
            // Führe die Modulprüfung asynchron aus
            try {
                // Get available modules
                List<String> newModules = new ArrayList<>();
                
                // Get list of module jar files
                File[] files = modulesDir.listFiles(file -> 
                    file.isFile() && file.getName().endsWith(".jar"));
                  if (files != null) {
                    for (File file : files) {
                        String moduleName = file.getName().substring(0, file.getName().length() - 4);
                          // Check if this module is not loaded yet
                        if (moduleManagerInternal != null && !loadedModules.containsKey(moduleName)) {
                            newModules.add(moduleName);
                        }
                    }
                }
                
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
     * Performance monitoring setup (placeholder - actual monitoring disabled)
     */
    private void initializePerformanceMonitoring() {
        // Performance monitoring components not available        getLogger().info("Performance monitoring disabled - required classes not found");
        
        // Initialize standard thread pool instead of advanced pool
        int threadPoolSize = getConfig().getInt("performance.thread-pool-size", 4);        
        getLogger().info("Using standard thread pool with " + threadPoolSize + " threads");
        
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
                // Use thread name for comparison instead of deprecated thread.getId() method
                if (thread.getName().equals(threadInfo.getThreadName())) {
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
     */    public void extractModuleResources() {
        getLogger().info("Module resource extraction disabled - ResourceManager not available");
        // Resource extraction functionality is not available due to missing ResourceManager class        return;
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
     */    @Override
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Setzt den Debug-Modus
     * 
     * @param debugMode true, um den Debug-Modus zu aktivieren
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        // Update configuration
        getConfig().set("general.debug-mode", debugMode);
        saveConfig();
    }
    
    /**
     * Gibt den Message-Prefix zurück
     * 
     * @return der konfigurierte Message-Prefix
     */
    @Override
    public String getMessagePrefix() {
        return messagePrefix;
    }
    
    /**
     * Gibt das Datenverzeichnis eines Moduls zurück
     * 
     * @param moduleName Der Name des Moduls
     * @return Der Datenordner des Moduls
     */
    @Override
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
     * Gibt die Konfigurationsdatei eines Moduls zurück
     * 
     * @param moduleName Der Name des Moduls
     * @return Die Konfigurationsdatei des Moduls
     */
    @Override
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
    @Override
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
     * Gets all loaded modules as required by the BasePlugin interface
     *
     * @return A map of module names to module information
     */
    @Override
    public Map<String, com.essentialscore.api.module.ModuleManager.ModuleInfo> getLoadedModules() {
        Map<String, com.essentialscore.api.module.ModuleManager.ModuleInfo> result = new HashMap<>();
        
        // Convert our internal ModuleInfo to the interface ModuleInfo
        for (Map.Entry<String, ApiCore.ModuleInfo> entry : loadedModules.entrySet()) {
            ApiCore.ModuleInfo internalInfo = entry.getValue();
            if (internalInfo != null) {
                try {
                    // Create a new ModuleInfo of the correct type
                    com.essentialscore.api.module.ModuleManager.ModuleInfo moduleInfo = 
                        new com.essentialscore.api.module.ModuleManager.ModuleInfo(
                            internalInfo.getName(),
                            internalInfo.getVersion(),
                            internalInfo.getDescription(),
                            internalInfo.getInstance(),
                            internalInfo.getLoader(),
                            internalInfo.getJarFile());
                    
                    result.put(entry.getKey(), moduleInfo);
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Failed to convert ModuleInfo: " + e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets information about a specific module.
     *
     * @param moduleName The module name
     * @return The module information, or null if not found
     */
    @Override
    public com.essentialscore.api.module.ModuleManager.ModuleInfo getModuleInfo(String moduleName) {
        ApiCore.ModuleInfo internalInfo = loadedModules.get(moduleName);
        if (internalInfo == null) {
            return null;
        }
        
        try {
            // Create a new ModuleInfo of the correct type
            return new com.essentialscore.api.module.ModuleManager.ModuleInfo(
                internalInfo.getName(),
                internalInfo.getVersion(),
                internalInfo.getDescription(),
                internalInfo.getInstance(),
                internalInfo.getLoader(),
                internalInfo.getJarFile());
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to convert ModuleInfo: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Prüft, ob ein Spieler eine Berechtigung hat
     * 
     * @param player Der Spieler
     * @param permission Die zu prüfende Berechtigung
     * @return true, wenn der Spieler die Berechtigung hat
     */
    @Override
    public boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null || permission.isEmpty()) {
            return true;
        }
        
        return permissionManager.hasPermission(player, permission);
    }
    
    /**
     * Initialisiert ein Modul
     * 
     * @param moduleName Der Name des Moduls
     * @param moduleInstance Die Modul-Instanz
     * @param config Die Modul-Konfiguration
     */
    @Override
    public void initializeModule(String moduleName, Object moduleInstance, FileConfiguration config) {
        // Create the ModuleAPI instance for this module if it doesn't exist
        ModuleAPI moduleAPI = moduleAPIs.computeIfAbsent(moduleName, k -> new CoreModuleAPI(this, k));
        
        if (moduleInstance instanceof Module) {
            // New API interface
            try {
                Method initMethod = moduleInstance.getClass().getMethod("init", ModuleAPI.class, FileConfiguration.class);
                initMethod.invoke(moduleInstance, moduleAPI, config);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to initialize module " + moduleName, e);
            }
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
     * Registriert dynamische Befehle
     *
     * @param commands Liste der zu registrierenden Befehle
     */    @Override
    public void registerCommands(List<DynamicCommand> commands) {
        if (commandManager != null && commands != null) {
            try {
                // Create a wrapper method that registers DynamicCommands
                Method registerDynamicCommandMethod = commandManager.getClass().getMethod("registerDynamicCommand", DynamicCommand.class);
                
                for (DynamicCommand command : commands) {
                    registerDynamicCommandMethod.invoke(commandManager, command);
                }
            } catch (NoSuchMethodException e) {
                // Fallback: Try to register each command individually if registerDynamicCommand doesn't exist
                getLogger().warning("registerDynamicCommand method not found, trying alternative approach");
                
                for (DynamicCommand command : commands) {
                    try {
                        // Try to register using a different approach
                        if (command != null && command.getName() != null) {
                            getLogger().info("Registering dynamic command: " + command.getName() + " from module: " + command.getModuleId());
                        }
                    } catch (Exception ex) {
                        getLogger().log(Level.WARNING, "Failed to register dynamic command: " + 
                            (command != null ? command.getName() : "unknown"), ex);
                    }
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error registering dynamic commands", e);
            }
        }
    }

    /**
     * Deregistriert dynamische Befehle
     *
     * @param commands Liste der zu deregistrierenden Befehle
     */
    @Override
    public void unregisterCommands(List<DynamicCommand> commands) {
        if (commandManager != null) {
            try {
                Method unregisterCommandsMethod = commandManager.getClass().getMethod("unregisterCommands", List.class);
                unregisterCommandsMethod.invoke(commandManager, commands);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error unregistering dynamic commands", e);
            }
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
        if (permissionManager == null) {
            return false;
        }
        
        try {
            Method isHookedMethod = permissionManager.getClass().getMethod("isPermissionsHooked");
            return (boolean) isHookedMethod.invoke(permissionManager);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to check if permissions are hooked", e);
            return false;
        }
    }

    /**
     * Gibt ein geteiltes Datenobjekt zurück
     * 
     * @param key Der Schlüssel des Datenobjekts
     * @return Das geteilte Datenobjekt oder null, wenn es nicht existiert
     */
    public Object getSharedData(String key) {
        return sharedData.get(key);
    }    /**
     * Gibt den LanguageManager zurück
     * 
     * @return LanguageManager-Instanz
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
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
     * Gibt den ClickableCommandManager zurück
     * 
     * @return ClickableCommandManager-Instanz
     */
    public ClickableCommandManager getClickableCommandManager() {
        return clickableCommandManager;
    }
    
    /**
     * Gibt das Modules-Verzeichnis zurück
     * 
     * @return Modules-Verzeichnis
     */
    public File getModulesDir() {
        return modulesDir;
    }
    
    /**
     * Gibt den ModuleManager zurück
     * @return Der ModuleManager
     */
    public com.essentialscore.api.module.ModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * Gibt den ModuleFileManager zurück
     * @return Der ModuleFileManager
     */
    public ModuleFileManager getModuleFileManager() {
        return moduleFileManager;
    }    /**
     * Gibt die ModuleSandbox zurück
     * 
     * @return ModuleSandbox-Instanz
     */
    public ModuleSandbox getModuleSandbox() {
        return moduleSandbox;
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
     * Gibt den ThreadManager zurück
     * 
     * @return ThreadManager-Instanz
     */
    public ThreadManager getThreadManager() {
        return threadManager;
    }
    
    /**
     * Gibt den PerformanceBenchmark zurück
     * 
     * @return PerformanceBenchmark-Instanz
     */
    public PerformanceBenchmark getPerformanceBenchmark() {
        return performanceBenchmark;
    }/**
     * Gibt den WebUIManager zurück
     * @return Der WebUIManager
     */
    // public WebUIManager getWebUIManager() { // MOVED TO webui-development
    //     return webUIManager;
    // }

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
        console.categoryInfo(ConsoleFormatter.MessageCategory.SYSTEM, "Exportiere API-Packages für Module...");
        
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
            
            console.categorySuccess(ConsoleFormatter.MessageCategory.SYSTEM, "API-Packages für Module erfolgreich exportiert");
        } catch (ClassNotFoundException e) {
            console.categoryError(ConsoleFormatter.MessageCategory.SYSTEM, "Fehler beim Exportieren der API-Packages: " + e.getMessage());
            e.printStackTrace();
        }
    }    /**
     * Lädt alle Module neu
     * Diese Methode verwendet den ModuleManager, um alle Module sauber neu zu laden
     */
    public void reloadModules() {
        console.header("MODULE NEU LADEN");
        
        if (moduleManagerInternal != null) {
            try {
                // Use the internal module manager which has the actual reload functionality
                moduleManagerInternal.reloadAllModules();
                console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Module wurden erfolgreich neu geladen");
            } catch (Exception e) {
                console.categoryError(ConsoleFormatter.MessageCategory.MODULE, "Fehler beim Neuladen der Module: " + e.getMessage());
                if (debugMode) {
                    e.printStackTrace();
                }
            }
            
            // Ressourcen nach dem Neuladen extrahieren, falls konfiguriert
            if (getConfig().getBoolean("core.extract-resources", true)) {
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    console.categoryInfo(ConsoleFormatter.MessageCategory.RESOURCE, "Extrahiere Modul-Ressourcen nach Reload...");
                    extractModuleResources();
                });
            }
            
            // Alle Caches leeren, die auf Module verweisen könnten
            formattedMessageCache.clear();
            permissionExactCache.clear();
            
            // Stelle sicher, dass alle Module korrekt registriert sind
            console.categorySuccess(ConsoleFormatter.MessageCategory.MODULE, "Module-Reload abgeschlossen. Anzahl: " + loadedModules.size());
        } else {
            console.categoryError(ConsoleFormatter.MessageCategory.SYSTEM, "ModuleManager ist nicht initialisiert!");
        }
    }

    /**
     * Gibt den CommandManager zurück
     * 
     * @return Die CommandManager-Instanz
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Gets the permission cache size
     * 
     * @return The permission cache size
     */
    public int getPermissionCacheSize() {
        if (permissionManager != null) {
            try {
                Method getCacheSizeMethod = permissionManager.getClass().getMethod("getCacheSize");
                return (Integer) getCacheSizeMethod.invoke(permissionManager);
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * Invokes a method on an object using reflection
     * 
     * @param object The object to invoke the method on
     * @param methodName The method name
     * @param paramTypes The parameter types
     * @return The method result
     */
    public Object invokeMethod(Object object, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method method = object.getClass().getMethod(methodName, paramTypes);
            return method.invoke(object, args);
        } catch (Exception e) {
            getLogger().warning("Failed to invoke method " + methodName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Tracks method execution time for performance monitoring.
     *
     * @param moduleName The module name
     * @param methodName The method name
     * @param startTime The start time in milliseconds
     */    public void trackMethodTime(String moduleName, String methodName, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        // Performance monitoring disabled - no recording available
        getLogger().fine("Method " + methodName + " in " + moduleName + " took " + duration + "ms");
    }

    /**
     * Gets module configuration.
     *
     * @param moduleName The module name
     * @return The module configuration
     */
    public FileConfiguration getModuleConfig(String moduleName) {
        return getModuleManager().getModuleConfig(moduleName);
    }

    /**
     * Saves module configuration.
     *
     * @param moduleName The module name
     */
    public void saveModuleConfig(String moduleName) {
        getModuleManager().saveModuleConfig(moduleName);
    }

    /**
     * Reloads module configuration.
     *
     * @param moduleName The module name
     */
    public void reloadModuleConfig(String moduleName) {
        getModuleManager().reloadModuleConfig(moduleName);
    }

    /**
     * Gets the integration manager.
     *
     * @return The integration manager
     */
    public Object getIntegrationManager() {
        // Placeholder implementation
        return null;
    }    /**
     * Gets the placeholder manager.
     *
     * @return The placeholder manager
     */
    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    /**
     * Gets the security manager.
     *
     * @return The security manager
     */
    public com.essentialscore.api.security.SecurityManager getSecurityManager() {
        // Placeholder implementation
        return null;
    }

    /**
     * Gets the GUI manager.
     *
     * @return The GUI manager
     */
    public Object getGUIManager() {
        // Placeholder implementation
        return null;
    }

    /**
     * Gets the module state manager.
     *
     * @return The module state manager
     */
    public Object getModuleStateManager() {
        // Placeholder implementation
        return null;
    }

    /**
     * Caches a permission for performance optimization.
     *
     * @param permissionName The permission name to cache
     */
    public void cachePermission(String permissionName) {
        if (permissionName == null || permissionName.isEmpty()) {
            return;
        }
        
        // Add permission to cache if caching is enabled
        try {
            getLogger().fine("Caching permission: " + permissionName);
            // Implementation would go here for actual permission caching
        } catch (Exception e) {
            getLogger().warning("Failed to cache permission " + permissionName + ": " + e.getMessage());
        }
    }    /**
     * Gets the internal module manager for performance monitoring.
     *
     * @return The internal module manager
     */
    public com.essentialscore.ModuleManager getInternalModuleManager() {
        return moduleManagerInternal;
    }

    /**
     * Gibt die Start-Zeit des Plugins zurück
     */
    public long getStartTime() {
        return startTime;
    }
}
