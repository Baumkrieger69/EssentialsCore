package com.essentialscore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ModuleSandbox verwaltet die sichere Ausführung von Modulen in einer isolierten Umgebung.
 * Diese Klasse bietet Schutz vor Modul-Abstürzen und verhindert so, dass ein fehlerhaftes
 * Modul den gesamten Server zum Absturz bringt.
 * Note: This class uses deprecated Bukkit APIs that are still commonly used.
 */
@SuppressWarnings("deprecation")
public class ModuleSandbox {
    private final ApiCore apiCore;
    private final ConsoleFormatter console;
    private boolean sandboxEnabled;
    private long maxExecutionTime;
    private boolean saveDataOnCrash;
    private boolean notifyAdminOnCrash;
    private boolean notifyConsole;
    private boolean notifyIngame;
    private boolean notifyDiscord;
    private String discordWebhookUrl;
    private boolean autoRestartModules;
    private int maxRestartAttempts;
    private boolean detailedErrorLogging;
    private String crashReportDirectory;
    
    // Speichere Neustartversuche pro Modul
    private final Map<String, Integer> restartAttempts = new ConcurrentHashMap<>();
    
    // Speichere Liste der vertrauenswürdigen Module
    private final CopyOnWriteArrayList<String> trustedModules = new CopyOnWriteArrayList<>();
    
    // Thread-Pool für isolierte Modulausführung
    private final ExecutorService executorService;
    
    /**
     * Erstellt eine neue ModuleSandbox-Instanz
     * 
     * @param apiCore Die ApiCore-Instanz
     */
    public ModuleSandbox(ApiCore apiCore) {
        this.apiCore = apiCore;
        this.console = new ConsoleFormatter(
            apiCore.getLogger(),
            apiCore.getConfig().getString("console.prefixes.module-manager", "&8[&d&lModuleManager&8]"),
            apiCore.getConfig().getBoolean("console.use-colors", true),
            apiCore.getConfig().getBoolean("console.show-timestamps", false),
            apiCore.getConfig().getBoolean("console.use-unicode-symbols", true),
            apiCore.getConfig().getString("console.style-preset", "default")
        );
        
        // Thread-Pool für isolierte Ausführung erstellen
        executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "ModuleSandbox-Worker");
            thread.setDaemon(true);
            return thread;
        });
        
        // Konfiguration laden
        loadConfiguration();
        
        // Crash-Verzeichnis vorbereiten
        prepareCrashDirectory();
    }
    
    /**
     * Lädt die Sandboxkonfiguration
     */
    private void loadConfiguration() {
        FileConfiguration config = apiCore.getConfig();
        
        sandboxEnabled = config.getBoolean("modules.sandbox.enabled", true);
        maxExecutionTime = config.getLong("modules.sandbox.max-execution-time", 5000);
        saveDataOnCrash = config.getBoolean("modules.sandbox.save-data-on-crash", true);
        notifyAdminOnCrash = config.getBoolean("modules.sandbox.notify-admin-on-crash", true);
        
        // Benachrichtigungsmethoden
        notifyConsole = config.getBoolean("modules.sandbox.notify-methods.console", true);
        notifyIngame = config.getBoolean("modules.sandbox.notify-methods.ingame", true);
        notifyDiscord = config.getBoolean("modules.sandbox.notify-methods.discord", false);
        discordWebhookUrl = config.getString("modules.sandbox.notify-methods.discord-webhook-url", "");
        
        autoRestartModules = config.getBoolean("modules.sandbox.auto-restart-modules", false);
        maxRestartAttempts = config.getInt("modules.sandbox.max-restart-attempts", 3);
        detailedErrorLogging = config.getBoolean("modules.sandbox.detailed-error-logging", true);
        crashReportDirectory = config.getString("modules.sandbox.crash-report-directory", "crashes");
        
        // Vertrauenswürdige Module laden
        trustedModules.clear();
        trustedModules.addAll(config.getStringList("modules.sandbox.trusted-modules"));
        
        // Log-Info
        if (sandboxEnabled) {
            console.categoryInfo(ConsoleFormatter.MessageCategory.SECURITY, "Modul-Sandbox aktiviert");
        } else {
            console.categoryWarning(ConsoleFormatter.MessageCategory.SECURITY, "Modul-Sandbox ist deaktiviert - Module laufen ohne Isolierung");
        }
    }
    
    /**
     * Bereitet das Verzeichnis für Absturzberichte vor
     */
    private void prepareCrashDirectory() {
        if (!sandboxEnabled) return;
        
        File crashDir = new File(apiCore.getDataFolder(), crashReportDirectory);
        if (!crashDir.exists() && !crashDir.mkdirs()) {
            console.categoryWarning(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Konnte das Absturzbericht-Verzeichnis nicht erstellen: " + crashDir.getAbsolutePath()
            );
        }
    }
    
    /**
     * Führt eine Modulaktion in einer Sandbox aus
     * 
     * @param moduleName Der Name des Moduls
     * @param moduleInstance Die Modulinstanz
     * @param action Die auszuführende Aktion
     * @return true, wenn die Aktion erfolgreich ausgeführt wurde, false wenn ein Fehler auftrat
     */
    public boolean executeModuleAction(String moduleName, Object moduleInstance, Runnable action) {
        // Wenn Sandbox deaktiviert ist oder Modul vertrauenswürdig ist, direkt ausführen
        if (!sandboxEnabled || trustedModules.contains(moduleName)) {
            try {
                action.run();
                return true;
            } catch (Throwable t) {
                handleModuleException(moduleName, moduleInstance, t);
                return false;
            }
        }
        
        // Sandbox-Ausführung mit Timeout
        Future<?> future = executorService.submit(action);
        
        try {
            // Warte maximal die konfigurierte Zeit
            if (maxExecutionTime > 0) {
                future.get(maxExecutionTime, TimeUnit.MILLISECONDS);
            } else {
                future.get(); // Kein Timeout
            }
            return true;
        } catch (TimeoutException e) {
            future.cancel(true);
            handleModuleTimeout(moduleName, moduleInstance);
            return false;
        } catch (ExecutionException e) {
            handleModuleException(moduleName, moduleInstance, e.getCause());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Führt eine Modulaktion mit Rückgabewert in einer Sandbox aus
     * 
     * @param <T> Der Rückgabetyp
     * @param moduleName Der Name des Moduls
     * @param moduleInstance Die Modulinstanz
     * @param action Die auszuführende Aktion
     * @param defaultValue Der Standardwert, der bei einem Fehler zurückgegeben wird
     * @return Das Ergebnis der Aktion oder der Standardwert bei einem Fehler
     */
    public <T> T executeModuleFunction(String moduleName, Object moduleInstance, Callable<T> action, T defaultValue) {
        // Wenn Sandbox deaktiviert ist oder Modul vertrauenswürdig ist, direkt ausführen
        if (!sandboxEnabled || trustedModules.contains(moduleName)) {
            try {
                return action.call();
            } catch (Throwable t) {
                handleModuleException(moduleName, moduleInstance, t);
                return defaultValue;
            }
        }
        
        // Sandbox-Ausführung mit Timeout
        Future<T> future = executorService.submit(action);
        
        try {
            // Warte maximal die konfigurierte Zeit
            if (maxExecutionTime > 0) {
                return future.get(maxExecutionTime, TimeUnit.MILLISECONDS);
            } else {
                return future.get(); // Kein Timeout
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            handleModuleTimeout(moduleName, moduleInstance);
            return defaultValue;
        } catch (ExecutionException e) {
            handleModuleException(moduleName, moduleInstance, e.getCause());
            return defaultValue;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return defaultValue;
        }
    }
    
    /**
     * Behandelt eine Zeitüberschreitung bei der Modulausführung
     * 
     * @param moduleName Der Name des Moduls
     * @param moduleInstance Die Modulinstanz
     */
    private void handleModuleTimeout(String moduleName, Object moduleInstance) {
        String errorMessage = "Zeitüberschreitung bei der Ausführung von Modul " + moduleName;
        console.categoryError(ConsoleFormatter.MessageCategory.SECURITY, errorMessage);
        
        // Erstelle einen Absturzbericht
        createCrashReport(moduleName, moduleInstance, new TimeoutException("Module execution exceeded time limit of " + maxExecutionTime + "ms"));
        
        // Speichere Moduldaten, falls konfiguriert
        if (saveDataOnCrash) {
            saveModuleData(moduleName, moduleInstance);
        }
        
        // Deaktiviere das Modul
        disableModule(moduleName);
        
        // Benachrichtige Administratoren
        if (notifyAdminOnCrash) {
            notifyAdmins(moduleName, errorMessage);
        }
    }
    
    /**
     * Behandelt einen Fehler bei der Modulausführung
     * 
     * @param moduleName Der Name des Moduls
     * @param moduleInstance Die Modulinstanz
     * @param throwable Der aufgetretene Fehler
     */
    private void handleModuleException(String moduleName, Object moduleInstance, Throwable throwable) {
        String errorMessage = "Fehler bei der Ausführung von Modul " + moduleName + ": " + throwable.getMessage();
        console.categoryError(ConsoleFormatter.MessageCategory.SECURITY, errorMessage);
        
        // Detailliertes Fehler-Logging
        if (detailedErrorLogging) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            console.categoryError(ConsoleFormatter.MessageCategory.SECURITY, "Stack-Trace: " + sw.toString());
        }
        
        // Erstelle einen Absturzbericht
        createCrashReport(moduleName, moduleInstance, throwable);
        
        // Speichere Moduldaten, falls konfiguriert
        if (saveDataOnCrash) {
            saveModuleData(moduleName, moduleInstance);
        }
        
        // Deaktiviere das Modul
        disableModule(moduleName);
        
        // Benachrichtige Administratoren
        if (notifyAdminOnCrash) {
            notifyAdmins(moduleName, errorMessage);
        }
    }
    
    /**
     * Erstellt einen Absturzbericht für ein Modul
     * 
     * @param moduleName Der Name des Moduls
     * @param moduleInstance Die Modulinstanz
     * @param throwable Der aufgetretene Fehler
     */
    private void createCrashReport(String moduleName, Object moduleInstance, Throwable throwable) {
        if (!sandboxEnabled) return;
        
        try {
            // Erstelle Absturzbericht-Datei
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            File crashFile = new File(apiCore.getDataFolder(), crashReportDirectory + "/" + moduleName + "_" + timestamp + ".yml");
            
            YamlConfiguration crashReport = new YamlConfiguration();
            
            // Allgemeine Informationen
            crashReport.set("timestamp", System.currentTimeMillis());
            crashReport.set("module.name", moduleName);
            crashReport.set("module.class", moduleInstance.getClass().getName());
            
            // API-Informationen
            crashReport.set("api.version", apiCore.getDescription().getVersion());
            crashReport.set("server.version", Bukkit.getVersion());
            
            // Fehlerinformationen
            crashReport.set("error.type", throwable.getClass().getName());
            crashReport.set("error.message", throwable.getMessage());
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            crashReport.set("error.stacktrace", sw.toString());
            
            // Speichern
            crashReport.save(crashFile);
            
            console.categoryInfo(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Absturzbericht erstellt: " + crashFile.getName()
            );
        } catch (IOException e) {
            console.categoryWarning(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Fehler beim Erstellen des Absturzberichts: " + e.getMessage()
            );
        }
    }
    
    /**
     * Speichert alle Daten eines Moduls
     * 
     * @param moduleName Der Name des Moduls
     * @param moduleInstance Die Modulinstanz
     */
    private void saveModuleData(String moduleName, Object moduleInstance) {
        try {
            // Versuche saveData-Methode aufzurufen, falls vorhanden
            Method saveDataMethod = null;
            
            try {
                saveDataMethod = moduleInstance.getClass().getMethod("saveData");
            } catch (NoSuchMethodException ignored) {
                // Versuche onDisable - wird oft für Datenspeicherung verwendet
                try {
                    saveDataMethod = moduleInstance.getClass().getMethod("onDisable");
                } catch (NoSuchMethodException ignored2) {
                    // Keine Speichermethode gefunden
                }
            }
            
            if (saveDataMethod != null) {
                // Führe Speichermethode in Sandbox aus, mit kürzerem Timeout
                final Method finalSaveDataMethod = saveDataMethod;
                Future<?> future = executorService.submit(() -> {
                    try {
                        finalSaveDataMethod.invoke(moduleInstance);
                    } catch (Exception e) {
                        throw new RuntimeException("Error saving data", e);
                    }
                });
                
                try {
                    // Kurzer Timeout für Datenspeicherung
                    future.get(2000, TimeUnit.MILLISECONDS);
                    console.categorySuccess(
                        ConsoleFormatter.MessageCategory.SECURITY,
                        "Daten für Modul " + moduleName + " erfolgreich gespeichert"
                    );
                } catch (Exception e) {
                    future.cancel(true);
                    console.categoryWarning(
                        ConsoleFormatter.MessageCategory.SECURITY,
                        "Fehler beim Speichern der Daten für Modul " + moduleName + ": " + e.getMessage()
                    );
                }
            }
        } catch (Exception e) {
            console.categoryWarning(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Fehler beim Speichern der Daten für Modul " + moduleName + ": " + e.getMessage()
            );
        }
    }
    
    /**
     * Deaktiviert ein Modul
     * 
     * @param moduleName Der Name des Moduls
     */
    private void disableModule(String moduleName) {
        try {
            // Deaktiviere das Modul über den ModuleManager
            apiCore.disableModule(moduleName);
            
            console.categoryInfo(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Modul " + moduleName + " wurde nach Absturz deaktiviert"
            );
            
            // Prüfe, ob das Modul automatisch neu gestartet werden soll
            if (autoRestartModules) {
                int attempts = restartAttempts.getOrDefault(moduleName, 0);
                
                if (attempts < maxRestartAttempts) {
                    restartAttempts.put(moduleName, attempts + 1);
                    
                    // Verzögerter Neustart
                    Bukkit.getScheduler().runTaskLater(apiCore, () -> {
                        try {
                            // Lade das Modul über den ModuleManager neu
                            if (apiCore.getModuleManager() != null) {
                                File moduleFile = new File(apiCore.getDataFolder(), "modules/" + moduleName + ".jar");
                                if (moduleFile.exists()) {
                                    apiCore.getModuleManager().loadModule(moduleFile);
                                    console.categorySuccess(
                                        ConsoleFormatter.MessageCategory.SECURITY,
                                        "Modul " + moduleName + " wurde nach Absturz neu geladen (Versuch " + (attempts + 1) + "/" + maxRestartAttempts + ")"
                                    );
                                } else {
                                    console.categoryWarning(
                                        ConsoleFormatter.MessageCategory.SECURITY,
                                        "Modul-Datei für " + moduleName + " nicht gefunden, Neustart nicht möglich"
                                    );
                                }
                            }
                        } catch (Exception e) {
                            console.categoryError(
                                ConsoleFormatter.MessageCategory.SECURITY,
                                "Fehler beim Neustarten von Modul " + moduleName + ": " + e.getMessage()
                            );
                        }
                    }, 20 * 5); // 5 Sekunden Verzögerung
                } else {
                    console.categoryWarning(
                        ConsoleFormatter.MessageCategory.SECURITY,
                        "Maximale Neustart-Versuche für Modul " + moduleName + " erreicht (" + maxRestartAttempts + ")"
                    );
                }
            }
        } catch (Exception e) {
            console.categoryError(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Fehler beim Deaktivieren von Modul " + moduleName + ": " + e.getMessage()
            );
        }
    }
    
    /**
     * Benachrichtigt Administratoren über einen Modulabsturz
     * 
     * @param moduleName Der Name des Moduls
     * @param errorMessage Die Fehlermeldung
     */
    private void notifyAdmins(String moduleName, String errorMessage) {
        // Konsolenbenachrichtigung
        if (notifyConsole) {
            console.doubleLine();
            console.categoryError(
                ConsoleFormatter.MessageCategory.SECURITY,
                "MODUL-ABSTURZ ERKANNT: " + moduleName
            );
            console.categoryError(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Fehler: " + errorMessage
            );
            console.categoryInfo(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Das Modul wurde deaktiviert, um Serverstabilität zu gewährleisten."
            );
            console.doubleLine();
        }
        
        // Ingame-Benachrichtigung
        if (notifyIngame) {
            String message = ChatColor.RED + "[ApiCore] " + ChatColor.DARK_RED + "MODUL-ABSTURZ: " + 
                             ChatColor.RED + moduleName + " - " + errorMessage;
            
            // Benachrichtige alle Spieler mit Admin-Rechten
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp() || player.hasPermission("apicore.admin.notifications")) {
                    player.sendMessage(message);
                }
            }
        }
        
        // Discord-Webhook-Benachrichtigung
        if (notifyDiscord && !discordWebhookUrl.isEmpty()) {
            sendDiscordNotification(moduleName, errorMessage);
        }
    }
    
    /**
     * Sendet eine Discord-Webhook-Benachrichtigung
     * 
     * @param moduleName Der Name des Moduls
     * @param errorMessage Die Fehlermeldung
     */
    private void sendDiscordNotification(String moduleName, String errorMessage) {
        // Asynchron ausführen, um den Hauptthread nicht zu blockieren
        Bukkit.getScheduler().runTaskAsynchronously(apiCore, () -> {
            try {
                URL url = new URL(discordWebhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "ApiCore/" + apiCore.getDescription().getVersion());
                connection.setDoOutput(true);
                
                // Erstelle JSON-Payload für Discord
                String serverName = Bukkit.getServer().getName();
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                
                String json = "{\"embeds\":[{" +
                    "\"title\":\"⚠️ Modul-Absturz erkannt\"," +
                    "\"description\":\"Ein Modul ist abgestürzt und wurde deaktiviert.\"," +
                    "\"color\":16711680," + // Rot
                    "\"fields\":[" +
                        "{\"name\":\"Server\",\"value\":\"" + serverName + "\",\"inline\":true}," +
                        "{\"name\":\"Modul\",\"value\":\"" + moduleName + "\",\"inline\":true}," +
                        "{\"name\":\"Zeitpunkt\",\"value\":\"" + timestamp + "\",\"inline\":true}," +
                        "{\"name\":\"Fehler\",\"value\":\"" + errorMessage.replace("\"", "\\\"") + "\",\"inline\":false}" +
                    "]," +
                    "\"footer\":{\"text\":\"ApiCore Module Sandbox\"}" +
                "}]}";
                
                // Sende Nachricht
                connection.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
                
                // Lese Antwort
                int responseCode = connection.getResponseCode();
                if (responseCode == 204) {
                    console.categorySuccess(
                        ConsoleFormatter.MessageCategory.SECURITY,
                        "Discord-Benachrichtigung für Modulabsturz erfolgreich gesendet"
                    );
                } else {
                    console.categoryWarning(
                        ConsoleFormatter.MessageCategory.SECURITY,
                        "Fehler beim Senden der Discord-Benachrichtigung: " + responseCode
                    );
                }
                
                connection.disconnect();
            } catch (Exception e) {
                console.categoryWarning(
                    ConsoleFormatter.MessageCategory.SECURITY,
                    "Fehler beim Senden der Discord-Benachrichtigung: " + e.getMessage()
                );
            }
        });
    }
    
    /**
     * Markiert ein Modul als vertrauenswürdig
     * 
     * @param moduleName Der Name des Moduls
     */
    public void trustModule(String moduleName) {
        if (!trustedModules.contains(moduleName)) {
            trustedModules.add(moduleName);
            console.categoryInfo(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Modul " + moduleName + " wurde als vertrauenswürdig markiert und läuft außerhalb der Sandbox"
            );
        }
    }
    
    /**
     * Entfernt ein Modul aus der Liste der vertrauenswürdigen Module
     * 
     * @param moduleName Der Name des Moduls
     */
    public void untrustModule(String moduleName) {
        if (trustedModules.contains(moduleName)) {
            trustedModules.remove(moduleName);
            console.categoryInfo(
                ConsoleFormatter.MessageCategory.SECURITY,
                "Modul " + moduleName + " wurde aus der Liste der vertrauenswürdigen Module entfernt"
            );
        }
    }
    
    /**
     * Prüft, ob ein Modul als vertrauenswürdig markiert ist
     * 
     * @param moduleName Der Name des Moduls
     * @return true, wenn das Modul vertrauenswürdig ist
     */
    public boolean isTrustedModule(String moduleName) {
        return trustedModules.contains(moduleName);
    }
    
    /**
     * Gibt zurück, ob der Sandbox-Modus aktiviert ist
     * 
     * @return true, wenn der Sandbox-Modus aktiviert ist
     */
    public boolean isSandboxEnabled() {
        return sandboxEnabled;
    }
    
    /**
     * Setzt den Sandbox-Modus aktiv oder inaktiv
     * 
     * @param enabled true, um den Sandbox-Modus zu aktivieren
     */
    public void setSandboxEnabled(boolean enabled) {
        this.sandboxEnabled = enabled;
        if (enabled) {
            console.categoryInfo(ConsoleFormatter.MessageCategory.SECURITY, "Modul-Sandbox wurde aktiviert");
        } else {
            console.categoryWarning(ConsoleFormatter.MessageCategory.SECURITY, "Modul-Sandbox wurde deaktiviert - Module laufen ohne Isolation");
        }
    }
    
    /**
     * Bereinige Ressourcen bei Plugin-Deaktivierung
     */
    public void shutdown() {
        executorService.shutdownNow();
        console.categoryInfo(ConsoleFormatter.MessageCategory.SECURITY, "ModuleSandbox wurde heruntergefahren");
    }    
} 
