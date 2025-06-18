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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.essentialscore.api.integration.IIntegrationManager;
import com.essentialscore.api.integration.PluginIntegration;
import com.essentialscore.api.integration.bukkit.BukkitIntegration;
import com.essentialscore.api.integration.permissions.PermissionIntegration;
import com.essentialscore.api.integration.economy.EconomyIntegration;
import com.essentialscore.api.integration.placeholders.PlaceholderIntegration;
import com.essentialscore.api.integration.worldguard.WorldGuardIntegration;
import com.essentialscore.api.security.ModuleSandbox;
import com.essentialscore.api.security.SecurityManager;
import com.essentialscore.api.security.SecurityPermission;
import com.essentialscore.api.gui.GUI;
import com.essentialscore.api.gui.GUIManager;
import com.essentialscore.api.gui.GUIBuilder;

/**
 * Main interface that modules use to interact with the EssentialsCore system.
 * This interface abstracts the core functionality to make modules less dependent
 * on the actual implementation.
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
     * Formats a message with hex colors
     * @param message The message to format
     * @return The formatted message
     */
    String formatHex(String message);
    
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
     * Gets the plugin instance
     * @return The plugin instance
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
     * Gets the module's data folder
     * @param moduleName The name of the module
     * @return The module's data folder
     */
    File getModuleDataFolder(String moduleName);
    
    /**
     * Gets the module's config file
     * @param moduleName The name of the module
     * @return The module's config file
     */
    File getModuleConfigFile(String moduleName);
    
    /**
     * Gets the module's resources folder
     * @param moduleName The name of the module
     * @return The module's resources folder
     */
    File getModuleResourcesFolder(String moduleName);
    
    /**
     * Registers a module event listener
     * @param eventName The name of the event
     * @param listener The event listener
     */
    void registerModuleListener(String eventName, ModuleEventListener listener);
    
    /**
     * Unregisters a module event listener
     * @param eventName The name of the event
     * @param listener The event listener to remove
     */
    void unregisterModuleListener(String eventName, ModuleEventListener listener);
    
    /**
     * Registers commands for a module
     * @param commands The commands to register
     */
    void registerCommands(List<? extends CommandDefinition> commands);
    
    /**
     * Unregisters commands for a module
     * @param commands The commands to unregister
     */
    void unregisterCommands(List<? extends CommandDefinition> commands);
    
    /**
     * Gets the integration manager
     * @return The integration manager
     */
    IIntegrationManager getIntegrationManager();
    
    /**
     * Gets a plugin integration by class
     * @param integrationClass The integration class
     * @param <T> The integration type
     * @return The integration, or empty if not available
     */
    <T extends PluginIntegration> Optional<T> getIntegration(Class<T> integrationClass);
    
    /**
     * Gets a plugin integration by plugin name
     * @param pluginName The plugin name
     * @return The integration, or empty if not available
     */
    Optional<PluginIntegration> getIntegrationByPlugin(String pluginName);
    
    /**
     * Gets the Bukkit integration
     * @return The Bukkit integration, or empty if not available
     */
    default Optional<BukkitIntegration> getBukkitIntegration() {
        return getIntegration(BukkitIntegration.class);
    }
    
    /**
     * Gets the permission integration
     * @return The permission integration, or empty if not available
     */
    default Optional<PermissionIntegration> getPermissionIntegration() {
        return getIntegration(PermissionIntegration.class);
    }
    
    /**
     * Gets the economy integration
     * @return The economy integration, or empty if not available
     */
    default Optional<EconomyIntegration> getEconomyIntegration() {
        return getIntegration(EconomyIntegration.class);
    }
    
    /**
     * Gets the placeholder integration
     * @return The placeholder integration, or empty if not available
     */
    default Optional<PlaceholderIntegration> getPlaceholderIntegration() {
        return getIntegration(PlaceholderIntegration.class);
    }
    
    /**
     * Gets the WorldGuard integration
     * @return The WorldGuard integration, or empty if not available
     */
    default Optional<WorldGuardIntegration> getWorldGuardIntegration() {
        return getIntegration(WorldGuardIntegration.class);
    }
    
    /**
     * Registers a custom module permission
     * @param module The module
     * @param permissionName The permission name (without module prefix)
     * @param description The permission description
     * @return The full permission node
     */
    String registerModulePermission(Module module, String permissionName, String description);
    
    /**
     * Formats a string with placeholders
     * @param text The text to format
     * @param player The player for player-specific placeholders
     * @return The formatted text
     */
    String formatPlaceholders(String text, Player player);
    
    /**
     * Gets the security manager
     * @return The security manager
     */
    SecurityManager getSecurityManager();
    
    /**
     * Gets the sandbox for a module
     * @param moduleId The module ID
     * @return The module sandbox, or null if not found
     */
    ModuleSandbox getModuleSandbox(String moduleId);
    
    /**
     * Executes a task in a module's sandbox
     * @param moduleId The module ID
     * @param task The task to execute
     * @param <T> The task result type
     * @return The task result
     */
    <T> T executeInSandbox(String moduleId, ModuleSandbox.SandboxedTask<T> task);
    
    /**
     * Checks if a module has a security permission
     * @param moduleId The module ID
     * @param permission The permission
     * @param target The permission target
     * @return true if the module has the permission
     */
    boolean hasModuleSecurityPermission(String moduleId, SecurityPermission permission, String target);
    
    /**
     * Gets the GUI manager.
     * 
     * @return The GUI manager
     */
    GUIManager getGUIManager();
    
    /**
     * Creates a new GUI builder.
     * 
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows (1-6)
     * @return The GUI builder
     */
    GUIBuilder createGUI(String moduleId, String title, int rows);
    
    /**
     * Opens a GUI for a player.
     * 
     * @param player The player
     * @param gui The GUI to open
     */
    void openGUI(Player player, GUI gui);
    
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
     * 
     * @param <T> the return type of the database operation
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
