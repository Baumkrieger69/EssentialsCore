package com.essentialscore;

import java.util.List;

/**
 * Manager für Befehle im ApiCore
 */
public class CommandManager {
    private final ApiCore apiCore;
    private ConsoleFormatter console;
    
    public CommandManager(ApiCore apiCore) {
        this.apiCore = apiCore;
        
        // Erweiterte Konsolen-Formatter Konfiguration
        boolean useColors = apiCore.getConfig().getBoolean("console.use-colors", true);
        
        // Konsolen-Formatter initialisieren mit Rohpräfix (ohne Formatierung)
        // Konsolen-Formatter initialisieren mit Rohpräfix (ohne Formatierung)
        String rawPrefix = apiCore.getConfig().getString("console.prefixes.command-manager", "&8[&3&lCommand&b&lManager&8]");
        console = new ConsoleFormatter(apiCore.getLogger(), rawPrefix, useColors);
    }
    
    /**
     * Überprüft, ob ein Befehl durch die Konfiguration deaktiviert ist
     * 
     * @param command Der zu prüfende Befehl
     * @return true, wenn der Befehl durch die Konfiguration deaktiviert ist
     */
    public boolean isConfigDisabled(String command) {
        // Normalisiere den Befehlsnamen
        command = command.toLowerCase();
        
        // Prüfe deaktivierte Befehle
        List<String> configDisabled = apiCore.getConfig().getStringList("commands.disabled");
        if (configDisabled.contains(command)) {
            return true;
        }
        
        // Prüfe individuell deaktivierte Befehle über die permissions-Sektion
        if (apiCore.getConfig().contains("commands.permissions." + command)) {
            boolean enabled = apiCore.getConfig().getBoolean("commands.permissions." + command, true);
            return !enabled; // Wenn enabled=false, dann ist der Befehl deaktiviert
        }
        
        return false;
    }
    
    /**
     * Überprüft vor der Ausführung eines Befehls, ob dieser deaktiviert ist
     * 
     * @param command Der auszuführende Befehl
     * @return true, wenn der Befehl deaktiviert ist und nicht ausgeführt werden soll
     */
    public boolean shouldBlockCommand(String command) {
        // Extrahiere den Hauptbefehl (erster Teil)
        String mainCommand = command.split(" ")[0].toLowerCase();
        
        // Prüfe, ob der Befehl in der Konfiguration deaktiviert ist
        if (isConfigDisabled(mainCommand)) {
            if (apiCore.isDebugMode()) {
                console.debug("Befehl ist deaktiviert: " + mainCommand, false);
            }
            return true;
        }
    
        return false;
    }
    
} 
