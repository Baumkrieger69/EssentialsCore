package com.essentialscore.util;

import com.essentialscore.ApiCore;
import com.essentialscore.ModuleManager;
import com.essentialscore.ModuleManager.ModulePerformanceData;
import com.essentialscore.ModuleManager.PerformanceStatus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet BossBars zur Anzeige von Modul-Performance-Daten
 */
public class ModulePerformanceBossBar {
    private final ApiCore apiCore;
    private final Map<UUID, ActiveBossBar> playerBossBars = new HashMap<>();
    private final Map<UUID, String> playerModuleSelections = new HashMap<>();
    private BukkitTask updateTask;
    private static final long UPDATE_INTERVAL_TICKS = 20; // 1 Sekunde
    
    /**
     * Erstellt eine neue ModulePerformanceBossBar-Instanz
     * 
     * @param apiCore Die ApiCore-Instanz
     */
    public ModulePerformanceBossBar(ApiCore apiCore) {
        this.apiCore = apiCore;
    }
    
    /**
     * Startet das regelmäßige Aktualisieren der BossBars
     */
    public void start() {
        if (updateTask != null) {
            return;
        }
        
        updateTask = Bukkit.getScheduler().runTaskTimer(apiCore, this::updateAllBossBars, 
            20L, UPDATE_INTERVAL_TICKS);
    }
    
    /**
     * Stoppt das Aktualisieren der BossBars
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        
        // Alle BossBars entfernen
        for (ActiveBossBar bossBar : new ArrayList<>(playerBossBars.values())) {
            bossBar.remove();
        }
        
        playerBossBars.clear();
        playerModuleSelections.clear();
    }
    
    /**
     * Zeigt einem Spieler eine BossBar für ein bestimmtes Modul an
     * 
     * @param player Der Spieler
     * @param moduleName Der Name des Moduls
     */
    public void showBossBarForModule(Player player, String moduleName) {
        if (player == null) {
            return;
        }
        
        // Falls bereits eine BossBar angezeigt wird, diese entfernen
        removeBossBar(player);
        
        // Modulauswahl speichern
        playerModuleSelections.put(player.getUniqueId(), moduleName);
        
        // BossBar erstellen und anzeigen
        ModuleManager moduleManager = apiCore.getModuleManager();
        ModulePerformanceData performanceData = moduleManager.getModulePerformanceData(moduleName);
        
        if (performanceData != null) {
            createBossBarForPlayer(player, performanceData);
        } else {
            // Modul nicht gefunden oder keine Performance-Daten
            BossBar infoBar = Bukkit.createBossBar(
                ChatColor.RED + "Keine Performance-Daten für Modul: " + moduleName,
                BarColor.WHITE, BarStyle.SOLID);
            infoBar.addPlayer(player);
            
            // Speichere BossBar und plane automatisches Entfernen
            ActiveBossBar activeBossBar = new ActiveBossBar(infoBar, null, null);
            playerBossBars.put(player.getUniqueId(), activeBossBar);
            
            // Nach 5 Sekunden automatisch entfernen
            Bukkit.getScheduler().runTaskLater(apiCore, () -> {
                if (playerBossBars.get(player.getUniqueId()) == activeBossBar) {
                    removeBossBar(player);
                }
            }, 5 * 20L);
        }
    }
    
    /**
     * Erstellt und zeigt eine BossBar für Modul-Performance an
     * 
     * @param player Der Spieler
     * @param performanceData Die Performance-Daten des Moduls
     */
    private void createBossBarForPlayer(Player player, ModulePerformanceData performanceData) {
        String moduleName = performanceData.getModuleName();
        PerformanceStatus status = performanceData.getPerformanceStatus();
        
        // Haupt-BossBar erstellen
        BarColor color = getBarColorForStatus(status);
        BossBar mainBar = Bukkit.createBossBar(
            formatMainBarTitle(moduleName, status),
            color, BarStyle.SEGMENTED_10);
        
        // CPU-BossBar erstellen
        BarColor cpuColor = getBarColorForCpuUsage(performanceData.getCpuUsagePercent());
        BossBar cpuBar = Bukkit.createBossBar(
            formatCpuBarTitle(performanceData.getCpuUsagePercent()),
            cpuColor, BarStyle.SEGMENTED_10);
        
        // Speicher-BossBar erstellen
        long memoryMB = performanceData.getMemoryUsageBytes() / (1024 * 1024);
        BarColor memoryColor = getBarColorForMemoryUsage(memoryMB);
        BossBar memoryBar = Bukkit.createBossBar(
            formatMemoryBarTitle(memoryMB, performanceData.getMemoryUsageFormatted()),
            memoryColor, BarStyle.SEGMENTED_10);
        
        // BossBar dem Spieler anzeigen
        mainBar.addPlayer(player);
        
        // BossBar speichern
        ActiveBossBar activeBossBar = new ActiveBossBar(mainBar, cpuBar, memoryBar);
        playerBossBars.put(player.getUniqueId(), activeBossBar);
        
        // Fortschritt initial setzen
        updateBossBarProgress(activeBossBar, performanceData);
    }
    
    /**
     * Formatiert den Titel der Haupt-BossBar
     * 
     * @param moduleName Der Modulname
     * @param status Der Performance-Status
     * @return Der formatierte Titel
     */
    private String formatMainBarTitle(String moduleName, PerformanceStatus status) {
        ChatColor statusColor;
        String statusText;
        
        switch (status) {
            case CRITICAL:
                statusColor = ChatColor.RED;
                statusText = "KRITISCH - Überprüfung empfohlen!";
                break;
            case WARNING:
                statusColor = ChatColor.YELLOW;
                statusText = "WARNUNG - Hohe Auslastung";
                break;
            default:
                statusColor = ChatColor.GREEN;
                statusText = "OK - Normale Auslastung";
                break;
        }
        
        return ChatColor.GOLD + "Modul: " + ChatColor.WHITE + moduleName + 
               ChatColor.GRAY + " | Status: " + statusColor + statusText;
    }
    
    /**
     * Formatiert den Titel der CPU-BossBar
     * 
     * @param cpuPercent Die CPU-Auslastung in Prozent
     * @return Der formatierte Titel
     */
    private String formatCpuBarTitle(double cpuPercent) {
        ChatColor color;
        
        if (cpuPercent >= CPU_THRESHOLD_CRITICAL) {
            color = ChatColor.RED;
        } else if (cpuPercent >= CPU_THRESHOLD_WARNING) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.GREEN;
        }
        
        return ChatColor.AQUA + "CPU: " + color + String.format("%.1f", cpuPercent) + "%" +
               ChatColor.GRAY + " | " + getUsageDescription(cpuPercent, CPU_THRESHOLD_WARNING, CPU_THRESHOLD_CRITICAL);
    }
    
    /**
     * Formatiert den Titel der Speicher-BossBar
     * 
     * @param memoryMB Die Speichernutzung in MB
     * @param formattedMemory Die formatierte Speichernutzung
     * @return Der formatierte Titel
     */
    private String formatMemoryBarTitle(long memoryMB, String formattedMemory) {
        ChatColor color;
        
        if (memoryMB >= MEMORY_THRESHOLD_CRITICAL) {
            color = ChatColor.RED;
        } else if (memoryMB >= MEMORY_THRESHOLD_WARNING) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.GREEN;
        }
        
        return ChatColor.LIGHT_PURPLE + "Speicher: " + color + formattedMemory +
               ChatColor.GRAY + " | " + getUsageDescription(memoryMB, MEMORY_THRESHOLD_WARNING, MEMORY_THRESHOLD_CRITICAL);
    }
    
    /**
     * Erzeugt eine textuelle Beschreibung der Auslastung
     * 
     * @param value Der aktuelle Wert
     * @param warningThreshold Der Schwellenwert für Warnungen
     * @param criticalThreshold Der Schwellenwert für kritische Auslastung
     * @return Eine textuelle Beschreibung
     */
    private String getUsageDescription(double value, double warningThreshold, double criticalThreshold) {
        if (value >= criticalThreshold) {
            return ChatColor.RED + "Kritische Auslastung";
        } else if (value >= warningThreshold) {
            return ChatColor.YELLOW + "Hohe Auslastung";
        } else {
            return ChatColor.GREEN + "Normale Auslastung";
        }
    }
    
    /**
     * Aktualisiert den Fortschritt einer BossBar basierend auf Performance-Daten
     * 
     * @param bossBar Die zu aktualisierende BossBar
     * @param performanceData Die Performance-Daten
     */
    private void updateBossBarProgress(ActiveBossBar bossBar, ModulePerformanceData performanceData) {
        // Haupt-BossBar aktualisieren - zeigt Status als Ganzes
        PerformanceStatus status = performanceData.getPerformanceStatus();
        double mainProgress;
        switch (status) {
            case CRITICAL:
                mainProgress = 1.0; // Voll für kritischen Status
                break;
            case WARNING:
                mainProgress = 0.6; // 60% für Warnungsstatus
                break;
            default:
                mainProgress = 0.3; // 30% für normalen Status
                break;
        }
        bossBar.mainBar.setProgress(mainProgress);
        
        // CPU-BossBar aktualisieren
        if (bossBar.cpuBar != null) {
            double cpuProgress = Math.min(performanceData.getCpuUsagePercent() / 100.0, 1.0);
            bossBar.cpuBar.setProgress(cpuProgress);
        }
        
        // Speicher-BossBar aktualisieren
        if (bossBar.memoryBar != null) {
            // Speichernutzung auf einer Skala von 0 bis MEMORY_THRESHOLD_CRITICAL * 2
            double memoryMB = performanceData.getMemoryUsageBytes() / (1024.0 * 1024.0);
            double memoryProgress = Math.min(memoryMB / (MEMORY_THRESHOLD_CRITICAL * 2.0), 1.0);
            bossBar.memoryBar.setProgress(memoryProgress);
        }
        
        // Farben entsprechend des Status aktualisieren
        BarColor mainColor = getBarColorForStatus(status);
        bossBar.mainBar.setColor(mainColor);
        
        if (bossBar.cpuBar != null) {
            BarColor cpuColor = getBarColorForCpuUsage(performanceData.getCpuUsagePercent());
            bossBar.cpuBar.setColor(cpuColor);
        }
        
        if (bossBar.memoryBar != null) {
            long memoryMB = performanceData.getMemoryUsageBytes() / (1024 * 1024);
            BarColor memoryColor = getBarColorForMemoryUsage(memoryMB);
            bossBar.memoryBar.setColor(memoryColor);
        }
    }
    
    /**
     * Entfernt die BossBar für einen Spieler
     * 
     * @param player Der Spieler
     */
    public void removeBossBar(Player player) {
        if (player == null) {
            return;
        }
        
        ActiveBossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.remove();
        }
    }
    
    /**
     * Zeigt dem Spieler die nächste BossBar an (wechselt zwischen Haupt-, CPU- und Speicher-BossBar)
     * 
     * @param player Der Spieler
     */
    public void cycleActiveBossBar(Player player) {
        if (player == null) {
            return;
        }
        
        ActiveBossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.cycle(player);
        }
    }
    
    /**
     * Aktualisiert alle aktiven BossBars
     */
    private void updateAllBossBars() {
        ModuleManager moduleManager = apiCore.getModuleManager();
        
        // Aktualisiere für jeden Spieler mit aktiver BossBar
        for (Map.Entry<UUID, ActiveBossBar> entry : new HashMap<>(playerBossBars).entrySet()) {
            UUID playerId = entry.getKey();
            ActiveBossBar bossBar = entry.getValue();
            String moduleName = playerModuleSelections.get(playerId);
            
            if (moduleName == null) {
                continue;
            }
            
            // Performance-Daten holen
            ModulePerformanceData performanceData = moduleManager.getModulePerformanceData(moduleName);
            if (performanceData != null) {
                // BossBar-Titel aktualisieren
                bossBar.mainBar.setTitle(formatMainBarTitle(moduleName, performanceData.getPerformanceStatus()));
                
                // CPU-Bar aktualisieren
                if (bossBar.cpuBar != null) {
                    bossBar.cpuBar.setTitle(formatCpuBarTitle(performanceData.getCpuUsagePercent()));
                }
                
                // Speicher-Bar aktualisieren
                if (bossBar.memoryBar != null) {
                    long memoryMB = performanceData.getMemoryUsageBytes() / (1024 * 1024);
                    bossBar.memoryBar.setTitle(formatMemoryBarTitle(memoryMB, performanceData.getMemoryUsageFormatted()));
                }
                
                // Fortschritt aktualisieren
                updateBossBarProgress(bossBar, performanceData);
            }
        }
    }
    
    /**
     * Gibt die passende BarColor für einen Performance-Status zurück
     * 
     * @param status Der Performance-Status
     * @return Die entsprechende BarColor
     */
    private BarColor getBarColorForStatus(PerformanceStatus status) {
        switch (status) {
            case CRITICAL:
                return BarColor.RED;
            case WARNING:
                return BarColor.YELLOW;
            default:
                return BarColor.GREEN;
        }
    }
    
    /**
     * Gibt die passende BarColor für eine CPU-Auslastung zurück
     * 
     * @param cpuPercent Die CPU-Auslastung in Prozent
     * @return Die entsprechende BarColor
     */
    private BarColor getBarColorForCpuUsage(double cpuPercent) {
        if (cpuPercent >= CPU_THRESHOLD_CRITICAL) {
            return BarColor.RED;
        } else if (cpuPercent >= CPU_THRESHOLD_WARNING) {
            return BarColor.YELLOW;
        } else {
            return BarColor.GREEN;
        }
    }
    
    /**
     * Gibt die passende BarColor für eine Speichernutzung zurück
     * 
     * @param memoryMB Die Speichernutzung in MB
     * @return Die entsprechende BarColor
     */
    private BarColor getBarColorForMemoryUsage(long memoryMB) {
        if (memoryMB >= MEMORY_THRESHOLD_CRITICAL) {
            return BarColor.RED;
        } else if (memoryMB >= MEMORY_THRESHOLD_WARNING) {
            return BarColor.YELLOW;
        } else {
            return BarColor.GREEN;
        }
    }
    
    /**
     * Repräsentiert eine aktive BossBar mit allen zugehörigen Komponenten
     */
    private class ActiveBossBar {
        private final BossBar mainBar;
        private final BossBar cpuBar;
        private final BossBar memoryBar;
        private BossBar currentBar;
        
        public ActiveBossBar(BossBar mainBar, BossBar cpuBar, BossBar memoryBar) {
            this.mainBar = mainBar;
            this.cpuBar = cpuBar;
            this.memoryBar = memoryBar;
            this.currentBar = mainBar;
        }
        
        /**
         * Wechselt zur nächsten BossBar
         * 
         * @param player Der Spieler
         */
        public void cycle(Player player) {
            if (currentBar == mainBar) {
                if (cpuBar != null) {
                    mainBar.removePlayer(player);
                    cpuBar.addPlayer(player);
                    currentBar = cpuBar;
                } else if (memoryBar != null) {
                    mainBar.removePlayer(player);
                    memoryBar.addPlayer(player);
                    currentBar = memoryBar;
                }
            } else if (currentBar == cpuBar) {
                if (memoryBar != null) {
                    cpuBar.removePlayer(player);
                    memoryBar.addPlayer(player);
                    currentBar = memoryBar;
                } else {
                    cpuBar.removePlayer(player);
                    mainBar.addPlayer(player);
                    currentBar = mainBar;
                }
            } else if (currentBar == memoryBar) {
                memoryBar.removePlayer(player);
                mainBar.addPlayer(player);
                currentBar = mainBar;
            }
        }
        
        /**
         * Entfernt alle BossBars
         */
        public void remove() {
            mainBar.removeAll();
            if (cpuBar != null) cpuBar.removeAll();
            if (memoryBar != null) memoryBar.removeAll();
        }
    }
    
    // Schwellenwerte aus ModuleManager
    private static final int CPU_THRESHOLD_WARNING = ModuleManager.CPU_THRESHOLD_WARNING;
    private static final int CPU_THRESHOLD_CRITICAL = ModuleManager.CPU_THRESHOLD_CRITICAL;
    private static final int MEMORY_THRESHOLD_WARNING = ModuleManager.MEMORY_THRESHOLD_WARNING;
    private static final int MEMORY_THRESHOLD_CRITICAL = ModuleManager.MEMORY_THRESHOLD_CRITICAL;
} 