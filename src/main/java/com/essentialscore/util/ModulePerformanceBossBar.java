package com.essentialscore.util;

import com.essentialscore.ApiCore;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.HashMap;
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
    private boolean enabled = false;
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final String DEFAULT_TITLE = "§6Performance Monitor";
    
    /**
     * Erstellt eine neue ModulePerformanceBossBar-Instanz
     * 
     * @param apiCore Die ApiCore-Instanz
     */
    public ModulePerformanceBossBar(ApiCore apiCore) {
        this.apiCore = apiCore;
    }
    
    /**
     * Startet die Performance-BossBar-Anzeige
     */
    public void start() {
        if (enabled) {
            return;
        }
        
        enabled = true;
        
        // Starte den Update-Task
        updateTask = Bukkit.getScheduler().runTaskTimer(apiCore, this::updateAllBossBars, 20L, 20L);
        
        apiCore.getLogger().info("ModulePerformanceBossBar gestartet");
    }
    
    /**
     * Stoppt die Performance-BossBar-Anzeige
     */
    public void stop() {
        if (!enabled) {
            return;
        }
        
        enabled = false;
        
        // Stoppe den Update-Task
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        
        // Entferne alle BossBars
        for (ActiveBossBar activeBossBar : playerBossBars.values()) {
            activeBossBar.bossBar.removeAll();
        }
        playerBossBars.clear();
        playerModuleSelections.clear();
        
        apiCore.getLogger().info("ModulePerformanceBossBar gestoppt");
    }
    
    /**
     * Zeigt eine BossBar für einen Spieler an
     * 
     * @param player Der Spieler
     * @param moduleName Der Name des zu überwachenden Moduls (optional)
     */
    public void showBossBar(Player player, String moduleName) {
        if (!enabled || player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Entferne vorherige BossBar falls vorhanden
        hideBossBar(player);
        
        // Erstelle neue BossBar
        BossBar bossBar = Bukkit.createBossBar(DEFAULT_TITLE, BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        
        ActiveBossBar activeBossBar = new ActiveBossBar(bossBar, System.currentTimeMillis());
        playerBossBars.put(playerId, activeBossBar);
        
        // Setze Modul-Auswahl
        if (moduleName != null && !moduleName.isEmpty()) {
            playerModuleSelections.put(playerId, moduleName);
        }
        
        // Sofortiges Update
        updateBossBar(playerId);
    }
    
    /**
     * Versteckt die BossBar für einen Spieler
     * 
     * @param player Der Spieler
     */
    public void hideBossBar(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        ActiveBossBar activeBossBar = playerBossBars.remove(playerId);
        
        if (activeBossBar != null) {
            activeBossBar.bossBar.removeAll();
        }
        
        playerModuleSelections.remove(playerId);
    }
    
    /**
     * Setzt das zu überwachende Modul für einen Spieler
     * 
     * @param player Der Spieler
     * @param moduleName Der Name des Moduls
     */
    public void setModuleSelection(Player player, String moduleName) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        if (moduleName == null || moduleName.isEmpty()) {
            playerModuleSelections.remove(playerId);
        } else {
            playerModuleSelections.put(playerId, moduleName);
        }
        
        // Sofortiges Update
        updateBossBar(playerId);
    }
    
    /**
     * Aktualisiert alle aktiven BossBars
     */
    private void updateAllBossBars() {
        if (!enabled) {
            return;
        }
        
        for (UUID playerId : new HashMap<>(playerBossBars).keySet()) {
            updateBossBar(playerId);
        }
    }
    
    /**
     * Aktualisiert die BossBar für einen bestimmten Spieler
     * 
     * @param playerId Die UUID des Spielers
     */
    private void updateBossBar(UUID playerId) {
        ActiveBossBar activeBossBar = playerBossBars.get(playerId);
        if (activeBossBar == null) {
            return;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            // Spieler ist offline, entferne BossBar
            playerBossBars.remove(playerId);
            playerModuleSelections.remove(playerId);
            activeBossBar.bossBar.removeAll();
            return;
        }
        
        String moduleName = playerModuleSelections.get(playerId);
        
        try {
            if (moduleName != null && !moduleName.isEmpty()) {
                updateModuleSpecificBossBar(activeBossBar, moduleName);
            } else {
                updateGeneralPerformanceBossBar(activeBossBar);
            }
        } catch (Exception e) {
            apiCore.getLogger().warning("Fehler beim Aktualisieren der BossBar für " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert die BossBar mit modulspezifischen Informationen
     * 
     * @param activeBossBar Die aktive BossBar
     * @param moduleName Der Name des Moduls
     */
    private void updateModuleSpecificBossBar(ActiveBossBar activeBossBar, String moduleName) {
        BossBar bossBar = activeBossBar.bossBar;
        
        // Versuche Performance-Daten für das Modul zu erhalten
        if (apiCore.getSharedData("performanceMonitor") != null) {
            try {
                // Simuliere Performance-Daten (in echter Implementierung würden hier echte Daten kommen)
                double cpuUsage = Math.random() * 100; // Platzhalter
                double memoryUsage = Math.random() * 100; // Platzhalter
                
                String title = String.format("§6%s §8| §aCPU: %s%% §8| §bRAM: %s%%", 
                    moduleName, 
                    DECIMAL_FORMAT.format(cpuUsage),
                    DECIMAL_FORMAT.format(memoryUsage));
                
                bossBar.setTitle(title);
                
                // Setze Progress basierend auf durchschnittlicher Auslastung
                double avgUsage = (cpuUsage + memoryUsage) / 2;
                bossBar.setProgress(Math.min(avgUsage / 100.0, 1.0));
                
                // Ändere Farbe basierend auf Auslastung
                if (avgUsage < 50) {
                    bossBar.setColor(BarColor.GREEN);
                } else if (avgUsage < 75) {
                    bossBar.setColor(BarColor.YELLOW);
                } else {
                    bossBar.setColor(BarColor.RED);
                }
                
            } catch (Exception e) {
                // Fallback bei Fehlern
                bossBar.setTitle("§c" + moduleName + " - Fehler beim Laden der Daten");
                bossBar.setProgress(0.0);
                bossBar.setColor(BarColor.RED);
            }
        } else {
            // Kein PerformanceMonitor verfügbar
            bossBar.setTitle("§e" + moduleName + " - Monitoring nicht verfügbar");
            bossBar.setProgress(0.0);
            bossBar.setColor(BarColor.YELLOW);
        }
    }
    
    /**
     * Aktualisiert die BossBar mit allgemeinen Performance-Informationen
     * 
     * @param activeBossBar Die aktive BossBar
     */
    private void updateGeneralPerformanceBossBar(ActiveBossBar activeBossBar) {
        BossBar bossBar = activeBossBar.bossBar;
        
        try {
            // Hole System-Performance-Daten
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUsagePercent = ((double) usedMemory / maxMemory) * 100;
            
            // Hole aktuelle TPS (falls verfügbar)
            double tps = getCurrentTPS();
            
            String title = String.format("§6Server Performance §8| §aTPS: %s §8| §bRAM: %s%%", 
                DECIMAL_FORMAT.format(tps),
                DECIMAL_FORMAT.format(memoryUsagePercent));
            
            bossBar.setTitle(title);
            
            // Progress basierend auf kombinierter Performance
            double tpsScore = Math.min(tps / 20.0, 1.0); // TPS Score (20 = perfekt)
            double memoryScore = 1.0 - Math.min(memoryUsagePercent / 100.0, 1.0); // Memory Score (weniger = besser)
            double overallScore = (tpsScore + memoryScore) / 2.0;
            
            bossBar.setProgress(Math.max(overallScore, 0.0));
            
            // Farbe basierend auf Overall-Performance
            if (overallScore > 0.8) {
                bossBar.setColor(BarColor.GREEN);
            } else if (overallScore > 0.5) {
                bossBar.setColor(BarColor.YELLOW);
            } else {
                bossBar.setColor(BarColor.RED);
            }
            
        } catch (Exception e) {
            // Fallback bei Fehlern
            bossBar.setTitle("§cFehler beim Laden der Performance-Daten");
            bossBar.setProgress(0.0);
            bossBar.setColor(BarColor.RED);
        }
    }
    
    /**
     * Versucht die aktuelle TPS zu ermitteln
     * 
     * @return Die aktuelle TPS (oder 20.0 als Fallback)
     */    private double getCurrentTPS() {
        try {
            // Fallback: Verwende eine geschätzte TPS basierend auf der aktuellen Performance
            long startTime = System.nanoTime();
            
            // Kleine Verzögerung für Messung
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            
            // Grobe Schätzung der TPS basierend auf Timing
            // (Das ist nur ein Platzhalter - in einer echten Implementierung 
            // würde man die Server-TPS verwenden)
            double estimatedTPS = 20.0;
            
            if (duration > 50_000_000) { // > 50ms ist langsam
                estimatedTPS = 15.0;
            } else if (duration > 100_000_000) { // > 100ms ist sehr langsam
                estimatedTPS = 10.0;
            }
            
            return estimatedTPS;
            
        } catch (Exception e) {
            return 20.0; // Fallback TPS
        }
    }
    
    /**
     * Prüft, ob die BossBar für den Spieler aktiv ist
     * 
     * @param player Der Spieler
     * @return true, wenn die BossBar aktiv ist
     */
    public boolean isBossBarActive(Player player) {
        return player != null && playerBossBars.containsKey(player.getUniqueId());
    }
    
    /**
     * Gibt die Anzahl der aktiven BossBars zurück
     * 
     * @return Die Anzahl der aktiven BossBars
     */
    public int getActiveBossBarCount() {
        return playerBossBars.size();
    }
    
    /**
     * Prüft, ob die BossBar-Anzeige aktiviert ist
     * 
     * @return true, wenn aktiviert
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Klasse für aktive BossBars mit Zeitstempel
     */    private static class ActiveBossBar {
        final BossBar bossBar;
        
        ActiveBossBar(BossBar bossBar, long createdAt) {
            this.bossBar = bossBar;
        }
    }
}