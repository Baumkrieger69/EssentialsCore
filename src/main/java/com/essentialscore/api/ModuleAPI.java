package com.essentialscore.api;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Die erweiterte API-Schnittstelle für Module, die Zugriff auf alle Core-Funktionen bietet.
 * Diese Schnittstelle ermöglicht Modulen den Zugriff auf gemeinsame Ressourcen und Funktionen.
 */
public interface ModuleAPI {
    
    /**
     * Gibt den Namen des Moduls zurück
     * 
     * @return Modulname
     */
    String getModuleName();
    
    /**
     * Gibt die Konfiguration des Moduls zurück
     * 
     * @return Die Modulkonfiguration
     */
    FileConfiguration getConfig();
    
    /**
     * Speichert die Konfiguration des Moduls
     */
    void saveConfig();
    
    /**
     * Lädt die Konfiguration des Moduls neu
     */
    void reloadConfig();
    
    /**
     * Gibt das Datenverzeichnis des Moduls zurück
     * 
     * @return Das Datenverzeichnis
     */
    File getDataFolder();
    
    /**
     * Gibt das Ressourcenverzeichnis des Moduls zurück
     * 
     * @return Das Ressourcenverzeichnis
     */
    File getResourcesFolder();
    
    /**
     * Extrahiert eine Ressource aus dem Modul-Jar
     * 
     * @param resourcePath Der Pfad zur Ressource im Jar
     * @param replace Wenn true, wird eine bereits existierende Datei überschrieben
     * @return true, wenn die Extraktion erfolgreich war
     */
    boolean extractResource(String resourcePath, boolean replace);
    
    /**
     * Extrahiert alle Ressourcen aus einem bestimmten Pfad im Modul-Jar
     * 
     * @param directory Das Verzeichnis im Jar
     * @param replace Wenn true, werden bereits existierende Dateien überschrieben
     * @return Anzahl der extrahierten Dateien
     */
    int extractResources(String directory, boolean replace);
    
    /**
     * Sendet eine Nachricht an einen Spieler oder CommandSender mit automatischer Farbformatierung
     * 
     * @param target Der Empfänger der Nachricht
     * @param message Die zu sendende Nachricht
     */
    void sendMessage(CommandSender target, String message);
    
    /**
     * Formatiert eine Nachricht mit Hex-Farbcodes und Minecraft-Farben
     * 
     * @param message Die zu formatierende Nachricht
     * @return Die formatierte Nachricht
     */
    String formatColor(String message);
    
    /**
     * Führt eine Aufgabe im Hauptthread des Servers aus
     * 
     * @param task Die auszuführende Aufgabe
     * @return Das BukkitTask-Objekt für weitere Verwaltung
     */
    BukkitTask runTask(Runnable task);
    
    /**
     * Führt eine Aufgabe asynchron in einem separaten Thread aus
     * 
     * @param task Die auszuführende Aufgabe
     * @return Das BukkitTask-Objekt für weitere Verwaltung
     */
    BukkitTask runTaskAsync(Runnable task);
    
    /**
     * Führt eine Aufgabe später im Hauptthread aus
     * 
     * @param task Die auszuführende Aufgabe
     * @param delay Die Verzögerung in Ticks
     * @return Das BukkitTask-Objekt für weitere Verwaltung
     */
    BukkitTask runTaskLater(Runnable task, long delay);
    
    /**
     * Führt eine Aufgabe wiederholt im Hauptthread aus
     * 
     * @param task Die auszuführende Aufgabe
     * @param delay Die anfängliche Verzögerung in Ticks
     * @param period Die Periode zwischen Ausführungen in Ticks
     * @return Das BukkitTask-Objekt für weitere Verwaltung
     */
    BukkitTask runTaskTimer(Runnable task, long delay, long period);
    
    /**
     * Registriert einen Event-Listener für dieses Modul
     * 
     * @param listener Der zu registrierende Listener
     */
    void registerListener(Listener listener);
    
    /**
     * Registriert einen benutzerdefinierten Modulbefehl
     * 
     * @param command Der Name des Befehls
     * @param description Die Beschreibung des Befehls
     * @param usage Die Verwendung des Befehls
     * @param executor Der Befehlsausführer
     * @param tabCompleter Der Tab-Completer oder null, wenn nicht benötigt
     * @param permission Die erforderliche Berechtigung oder null, wenn keine
     * @return true, wenn der Befehl erfolgreich registriert wurde
     */
    boolean registerCommand(String command, String description, String usage, 
                           CommandExecutor executor, TabCompleter tabCompleter, String permission);
    
    /**
     * Prüft, ob ein Spieler eine Berechtigung hat
     * 
     * @param player Der zu prüfende Spieler
     * @param permission Die zu prüfende Berechtigung
     * @return true, wenn der Spieler die Berechtigung hat
     */
    boolean hasPermission(Player player, String permission);
    
    /**
     * Registriert eine Modulberechtigung für Dokumentation und Abfrage
     * 
     * @param permission Die zu registrierende Berechtigung
     * @param description Die Beschreibung der Berechtigung
     * @param defaultValue Der Standardwert der Berechtigung
     */
    void registerPermission(String permission, String description, PermissionDefault defaultValue);
    
    /**
     * Legt einen gemeinsam genutzten Datenwert fest, der zwischen Modulen geteilt wird
     * 
     * @param key Der Schlüssel für den Wert
     * @param value Der zu speichernde Wert
     */
    void setSharedData(String key, Object value);
    
    /**
     * Holt einen gemeinsam genutzten Datenwert
     * 
     * @param key Der Schlüssel für den Wert
     * @return Der Wert oder null, wenn er nicht existiert
     */
    Object getSharedData(String key);
    
    /**
     * Legt einen modulspezifischen Datenwert fest
     * 
     * @param key Der Schlüssel für den Wert
     * @param value Der zu speichernde Wert
     */
    void setModuleData(String key, Object value);
    
    /**
     * Holt einen modulspezifischen Datenwert
     * 
     * @param key Der Schlüssel für den Wert
     * @return Der Wert oder null, wenn er nicht existiert
     */
    Object getModuleData(String key);
    
    /**
     * Holt eine Liste aller geladenen Module
     * 
     * @return Eine Liste der Modulnamen
     */
    List<String> getLoadedModules();
    
    /**
     * Prüft, ob ein Modul geladen ist
     * 
     * @param moduleName Der Name des Moduls
     * @return true, wenn das Modul geladen ist
     */
    boolean isModuleLoaded(String moduleName);
    
    /**
     * Registriert einen ModuleEventListener für ein bestimmtes Event
     * 
     * @param eventName Der Name des Events
     * @param listener Der Event-Listener
     */
    void registerModuleEventListener(String eventName, ModuleEventListener listener);
    
    /**
     * Deregistriert einen ModuleEventListener
     * 
     * @param eventName Der Name des Events
     * @param listener Der zu entfernende Listener
     */
    void unregisterModuleEventListener(String eventName, ModuleEventListener listener);
    
    /**
     * Löst ein Modul-Event aus und benachrichtigt alle Listener
     * 
     * @param eventName Der Name des Events
     * @param data Die Event-Daten als Map
     */
    void fireModuleEvent(String eventName, Map<String, Object> data);
    
    /**
     * Loggt eine Nachricht mit INFO-Level
     * 
     * @param message Die zu loggende Nachricht
     */
    void log(String message);
    
    /**
     * Loggt eine Nachricht mit einem bestimmten Level
     * 
     * @param level Das Log-Level
     * @param message Die zu loggende Nachricht
     */
    void log(LogLevel level, String message);
    
    /**
     * Loggt eine Fehlermeldung mit Stack-Trace
     * 
     * @param message Die Fehlermeldung
     * @param throwable Der Fehler
     */
    void logError(String message, Throwable throwable);
    
    /**
     * Loggt eine Informationsmeldung
     * 
     * @param message Die zu loggende Nachricht
     */
    void logInfo(String message);
    
    /**
     * Loggt eine Warnmeldung
     * 
     * @param message Die zu loggende Nachricht
     */
    void logWarning(String message);
    
    /**
     * Loggt eine Debug-Meldung
     * Diese wird nur angezeigt, wenn der Debug-Modus aktiviert ist
     * 
     * @param message Die zu loggende Nachricht
     */
    void logDebug(String message);
    
    /**
     * Gibt zurück, ob der Debug-Modus aktiviert ist
     * 
     * @return true, wenn der Debug-Modus aktiviert ist
     */
    boolean isDebugMode();
    
    /**
     * Holt die Plugin-Instanz der Hauptklasse
     * 
     * @return Die Plugin-Instanz
     */
    Plugin getPlugin();
    
    /**
     * Holt eine Datenbankverbindung aus dem Verbindungspool
     * 
     * @param database Der Name der Datenbank
     * @return Eine Datenbankverbindung oder null, wenn nicht konfiguriert
     */
    Connection getDatabaseConnection(String database);
    
    /**
     * Gibt eine Datenbankverbindung zurück in den Pool
     * 
     * @param connection Die zurückzugebende Verbindung
     */
    void releaseDatabaseConnection(Connection connection);
    
    /**
     * Führt eine asynchrone Datenbankoperation aus
     * 
     * @param <T> Der Rückgabetyp der Operation
     * @param database Der Name der Datenbank
     * @param operation Die auszuführende Operation
     * @return Ein Future mit dem Ergebnis der Operation
     */
    <T> CompletableFuture<T> runDatabaseOperation(String database, DatabaseOperation<T> operation);
    
    /**
     * Erstellt ein Plugin-übergreifendes Inventar
     * 
     * @param title Der Titel des Inventars
     * @param size Die Größe des Inventars (muss ein Vielfaches von 9 sein, max 54)
     * @return Ein InventoryBuilder für weitere Konfiguration
     */
    InventoryBuilder createInventory(String title, int size);
    
    /**
     * Erstellt einen benutzerdefinierten ItemStack
     * 
     * @param material Das Material für den ItemStack
     * @return Ein ItemBuilder für weitere Konfiguration
     */
    ItemBuilder createItem(String material);
    
    /**
     * Speichert modulspezifische Spielerdaten
     * 
     * @param playerUUID Die UUID des Spielers
     * @param key Der Schlüssel für die Daten
     * @param value Der zu speichernde Wert
     */
    void setPlayerData(UUID playerUUID, String key, Object value);
    
    /**
     * Holt modulspezifische Spielerdaten
     * 
     * @param playerUUID Die UUID des Spielers
     * @param key Der Schlüssel für die Daten
     * @return Der Wert oder null, wenn er nicht existiert
     */
    Object getPlayerData(UUID playerUUID, String key);
    
    /**
     * Entfernt modulspezifische Spielerdaten
     * 
     * @param playerUUID Die UUID des Spielers
     * @param key Der Schlüssel für die Daten
     */
    void removePlayerData(UUID playerUUID, String key);
    
    /**
     * Prüft die Performance-Metrik eines Moduls
     * 
     * @param moduleName Der Name des Moduls
     * @return Ein PerformanceResult-Objekt mit den Performance-Metriken
     */
    PerformanceResult checkModulePerformance(String moduleName);
    
    /**
     * Misst die Performance eines Codeblocks
     * 
     * @param block Der auszuführende Codeblock
     * @return Die Ausführungszeit in Millisekunden
     */
    long measurePerformance(Runnable block);
    
    /**
     * Führt eine Methode in einer sicheren Sandbox-Umgebung aus
     * 
     * @param <T> Der Rückgabetyp der Methode
     * @param action Die auszuführende Aktion
     * @param defaultValue Der Standardrückgabewert bei Fehler
     * @return Das Ergebnis der Aktion oder der Standardwert bei Fehler
     */
    <T> T executeInSandbox(Callable<T> action, T defaultValue);
    
    /**
     * Log-Level für die Logger-Methoden
     */
    enum LogLevel {
        DEBUG, INFO, WARNING, ERROR, SEVERE
    }
    
    /**
     * Standardeinstellungen für Berechtigungen
     */
    enum PermissionDefault {
        TRUE, FALSE, OP, NOT_OP
    }
    
    /**
     * Interface für Datenbankoperationen
     */
    interface DatabaseOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
    
    /**
     * Interface für einen Befehlsausführer
     */
    interface CommandExecutor {
        boolean onCommand(CommandSender sender, String command, String[] args);
    }
    
    /**
     * Interface für einen Tab-Completer
     */
    interface TabCompleter {
        List<String> onTabComplete(CommandSender sender, String command, String[] args);
    }
    
    /**
     * Interface für einen anpassbaren ItemStack-Builder
     */
    interface ItemBuilder {
        ItemBuilder name(String name);
        ItemBuilder lore(String... lore);
        ItemBuilder amount(int amount);
        ItemBuilder data(short data);
        ItemBuilder enchant(String enchantment, int level);
        ItemBuilder glow(boolean glow);
        ItemBuilder unbreakable(boolean unbreakable);
        ItemBuilder addFlag(String flag);
        ItemBuilder removeFlag(String flag);
        ItemBuilder addNBT(String key, Object value);
        ItemStack build();
    }
    
    /**
     * Interface für einen anpassbaren Inventory-Builder
     */
    interface InventoryBuilder {
        InventoryBuilder item(int slot, ItemStack item);
        InventoryBuilder onClick(Consumer<InventoryClickEvent> handler);
        InventoryBuilder onClose(Consumer<InventoryCloseEvent> handler);
        InventoryBuilder fillBorder(ItemStack item);
        InventoryBuilder fillEmpty(ItemStack item);
        InventoryBuilder paginated(boolean paginated);
        Inventory build();
    }
    
    /**
     * Klasse für Performance-Ergebnisse
     */
    class PerformanceResult {
        private final double cpuUsage;
        private final long memoryUsage;
        private final double avgExecutionTime;
        private final String status;
        
        public PerformanceResult(double cpuUsage, long memoryUsage, double avgExecutionTime, String status) {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.avgExecutionTime = avgExecutionTime;
            this.status = status;
        }
        
        public double getCpuUsage() {
            return cpuUsage;
        }
        
        public long getMemoryUsage() {
            return memoryUsage;
        }
        
        public double getAvgExecutionTime() {
            return avgExecutionTime;
        }
        
        public String getStatus() {
            return status;
        }
        
        public boolean isCritical() {
            return "CRITICAL".equals(status);
        }
        
        public boolean isWarning() {
            return "WARNING".equals(status);
        }
        
        public boolean isOk() {
            return "OK".equals(status);
        }
    }
} 