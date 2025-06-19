package com.essentialscore;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.essentialscore.api.Module;

/**
 * Optimierter dynamischer Befehl mit geringer Overhead
 */
public class DynamicCommand extends Command implements TabCompleter {
    private final String moduleName;
    private final String permission;
    private final Map<Integer, List<String>> tabCompletionOptions;
    private final ApiCore apiCore;
    
    // Leistungsoptimierungen durch Flywight-Pattern für häufig verwendete Strings
    private static final String NO_PERMISSION_MESSAGE = 
            ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl zu nutzen.";
    private static final String MODULE_DISABLED_MESSAGE =
            ChatColor.RED + "Der Befehl ist deaktiviert, da das zugehörige Modul nicht aktiv ist.";
    
    // Präkompilierte RegEx für häufige Befehlsmuster
    private static final Pattern COMMAND_ARGS_PATTERN = Pattern.compile("\"([^\"]*)\"|([^\\s]+)");
    
    // Command executor implementation
    private org.bukkit.command.CommandExecutor executor;
    private org.bukkit.command.TabCompleter customTabCompleter;
    
    /**
     * Sets the executor for this command
     * 
     * @param executor The executor to use
     * @return This command instance for method chaining
     */
    public DynamicCommand setExecutor(org.bukkit.command.CommandExecutor executor) {
        this.executor = executor;
        return this;
    }
    
    /**
     * Gets the current executor
     * 
     * @return The current executor or null if none is set
     */
    public org.bukkit.command.CommandExecutor getExecutor() {
        return executor;
    }
    
    /**
     * Sets the tab completer for this command
     * 
     * @param tabCompleter The tab completer to use
     * @return This command instance for method chaining
     */
    public DynamicCommand setTabCompleter(org.bukkit.command.TabCompleter tabCompleter) {
        this.customTabCompleter = tabCompleter;
        return this;
    }
    
    /**
     * Gets the custom tab completer
     * 
     * @return The custom tab completer or null if none is set
     */
    public org.bukkit.command.TabCompleter getCustomTabCompleter() {
        return customTabCompleter;
    }
    // Optimierte Command-Instance-Erstellung
    public DynamicCommand(String name, String description, String usageMessage, List<String> aliases, 
                          String moduleName, String permission, ApiCore apiCore) {
        super(name, 
              description != null ? description : "",
              usageMessage != null ? usageMessage : "/" + name,
              aliases != null ? aliases : Collections.emptyList());
              
        this.moduleName = moduleName;
        this.permission = permission;
        this.apiCore = apiCore;
        // Optimierte Map-Implementierung für kleine Datensätze
        this.tabCompletionOptions = new ConcurrentHashMap<>(4, 0.75f, 1);

        if (permission != null && !permission.isEmpty()) {
            setPermission(permission);
        }
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getPermission() {
        return permission;
    }
    
    /**
     * Fügt Tab-Completion-Optionen für ein bestimmtes Argument hinzu
     * @param argIndex Index des Arguments (0-basiert)
     * @param options Liste der verfügbaren Optionen
     */
    public void addTabCompletionOptions(int argIndex, List<String> options) {
        // Defensive Kopie für Thread-Sicherheit
        tabCompletionOptions.put(argIndex, new ArrayList<>(options));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Check if we have a custom tab completer
        if (customTabCompleter != null) {
            return customTabCompleter.onTabComplete(sender, command, alias, args);
        }
        // Schnellprüfung für wesentliche Bedingungen
        com.essentialscore.api.module.ModuleManager.ModuleInfo moduleInfoObj = apiCore.getModuleInfo(moduleName);
        if (moduleInfoObj == null || !hasPermission(sender)) {
            return Collections.emptyList();
        }
        
        // Kurzreferenz für optimierte Zugriffe
        final int argIndex = args.length - 1;
        
        // Schnellpfad für statische Komplettierungen
        if (tabCompletionOptions.containsKey(argIndex)) {
            final String currentArg = args[argIndex].toLowerCase();
            
            // Vorfilterung mit Stream für optimierte Verarbeitung
            return tabCompletionOptions.get(argIndex).stream()
                    .filter(option -> option.toLowerCase().startsWith(currentArg))
                    .limit(10) // Limitieren für bessere Performance
                    .collect(Collectors.toList());
        }
        
        // Versuche, dynamische Tab-Completion vom Modul zu bekommen
        try {
            Object moduleInstance = moduleInfoObj.getInstance();
            
            // Optimierter Tab-Complete mit Interface
            if (moduleInstance instanceof Module) {
                List<String> result = ((Module) moduleInstance).onTabComplete(getName(), sender, args);
                return result != null ? result : Collections.emptyList();
            }
            
            // Methode "onTabComplete" aufrufen, falls vorhanden, mit Performance-Tracking
            long startTime = System.nanoTime();
            try {
                Object result = apiCore.invokeMethod(moduleInstance, "onTabComplete", 
                        new Class[]{String.class, CommandSender.class, String[].class}, 
                        getName(), sender, args);
                
                if (result instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> completions = (List<String>) result;
                      // Performance-Tracking
                    if (apiCore.isDebugMode()) {
                        apiCore.trackMethodTime(moduleName, "onTabComplete", startTime);
                    }
                    
                    return completions;
                }
            } catch (Exception ignored) {
                // Andere Fehler (einschließlich NoSuchMethodException falls von apiCore.invokeMethod geworfen)
            }
        } catch (Exception e) {
            if (apiCore.isDebugMode()) {
                apiCore.getLogger().warning("Fehler bei Tab-Completion für " + getName() + ": " + e.getMessage());
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Schnellprüfung für Berechtigungen mit optimierter Verarbeitung
     */
    private boolean hasPermission(CommandSender sender) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        
        // Prüfen auf OP-Status vor der Permission-Prüfung
        if (sender.isOp()) {
            return true;
        }
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return apiCore.getPermissionManager().hasPermission(player, permission);
        }
        
        // Console hat immer alle Rechte
        return true;
    }    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        long startTime = System.nanoTime();
        
        try {
            // Use the executor if it's set
            if (executor != null) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
            
            // Otherwise, use the module-based execution
            if (moduleName != null && !moduleName.equalsIgnoreCase("apicore")) {
                com.essentialscore.api.module.ModuleManager.ModuleInfo moduleInfoObj = apiCore.getModuleInfo(moduleName);
                if (moduleInfoObj != null) {
                    try {
                        Object moduleInstance = moduleInfoObj.getInstance();
                        if (moduleInstance != null) {
                            // Versuche, die Methode onCommand aufzurufen
                            if (moduleInstance instanceof Module) {
                                ((Module) moduleInstance).onCommand(commandLabel, sender, args);
                            } else {
                                // Fallback auf Reflection
                                try {
                                    Class<?> moduleClass = moduleInstance.getClass();
                                    moduleClass.getMethod("onCommand", String.class, CommandSender.class, String[].class)
                                        .invoke(moduleInstance, commandLabel, sender, args);
                                } catch (NoSuchMethodException e) {
                                    // Kein Handler gefunden - führe Standard-Verhalten aus
                                    return executeDefault(sender, commandLabel, args);
                                }
                            }
                        }
                    } catch (Exception e) {
                        apiCore.getLogger().warning("Fehler beim Ausführen des Befehls " + commandLabel + " im Modul " + moduleName + ": " + e.getMessage());
                        return false;
                    }
                    
                    return true;
                }
            }
            
            // Standard-Verhalten für Core-Befehle oder wenn kein Modul gefunden wurde
            return executeDefault(sender, commandLabel, args);
        } finally {
            // Record execution time
            long executionTime = System.nanoTime() - startTime;
            if (apiCore.isDebugMode()) {
                apiCore.getLogger().info("Command " + commandLabel + " executed in " + (executionTime / 1000000.0) + "ms");
            }        }
    }
    
    /**
     * Parst Befehlsargumente mit Unterstützung für Anführungszeichen und Escape-Zeichen
     * @param argsString Die Befehlsargumente als einzelner String
     * @return Ein Array mit den verarbeiteten Argumenten
     */
    public static String[] parseArgs(String argsString) {
        if (argsString == null || argsString.isEmpty()) {
            return new String[0];
        }
        
        List<String> matchList = new ArrayList<>();
        Matcher regexMatcher = COMMAND_ARGS_PATTERN.matcher(argsString);
        
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Mit Anführungszeichen
                matchList.add(regexMatcher.group(1));
            } else {
                // Ohne Anführungszeichen
                matchList.add(regexMatcher.group(2));
            }
        }
        
        return matchList.toArray(new String[0]);
    }

    /**
     * Diese Methode setzt den internen Zustand des Befehls zurück,
     * was für eine saubere erneute Registrierung wichtig ist
     */
    public void resetCommand() {
        // EXTREME MAßNAHME: Wir deaktivieren den Befehl effektiv durch Umbenennung
        // Dies stellt sicher, dass der Befehl nicht mehr über Tab-Completion gefunden werden kann
        String originalName = getName();
        String disabledName = "disabled_" + System.currentTimeMillis() + "_" + originalName;
        setName(disabledName);
        setLabel(disabledName);
        
        // Aliase entfernen, um zu verhindern, dass der Befehl über sie aufgerufen werden kann
        getAliases().clear();
        
        // Tab-Completion explizit deaktivieren
        resetTabCompleter();
        
        // Permission auf unmöglichen Wert setzen - dies verhindert zusätzlich die Nutzung
        setPermission("disabled.command." + System.currentTimeMillis() + "." + originalName);
        
        // Stelle sicher, dass die Usage nicht mehr auf den Original-Befehl hinweist
        setUsage("/disabled_command_" + System.currentTimeMillis());
        
        // Alle Tab-Completion-Optionen löschen
        tabCompletionOptions.clear();
        
        // Registriert-Status zurücksetzen
        try {
            // Setze das registered-Flag auf false
            java.lang.reflect.Field registeredField = Command.class.getDeclaredField("registered");
            registeredField.setAccessible(true);
            registeredField.set(this, false);
            
            // Für eine vollständige Wiederherstellung des Befehls später die Originaldaten speichern
            this.setDescription("DEAKTIVIERT: " + getDescription());
        } catch (Exception ignored) {
            // Ignorieren, falls die Reflection fehlschlägt
        }
    }
    
    /**
     * Setzt den TabCompleter des Befehls zurück
     * (Dies ist eine Alternative zur setTabCompleter-Methode, die in Command nicht existiert)
     */
    public void resetTabCompleter() {
        // Tabcompletion-Optionen löschen
        tabCompletionOptions.clear();
        
        try {
            // Direkte Bearbeitung des TabCompleters
            java.lang.reflect.Field tabCompleterField = Command.class.getDeclaredField("tabCompleter");
            tabCompleterField.setAccessible(true);
            
            // Setze einen leeren TabCompleter, der immer eine leere Liste zurückgibt
            tabCompleterField.set(this, new TabCompleter() {
                @Override
                public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
                    return Collections.emptyList();
                }
            });
        } catch (Exception ignored) {
            // Ignorieren, falls die Reflection fehlschlägt
        }
    }
    
    /**
     * Erweiterte unregister-Methode, die sicherstellt, dass der Befehl
     * vollständig aus der CommandMap entfernt wird
     */
    @Override
    public boolean unregister(CommandMap commandMap) {
        boolean result = false;
        
        try {
            // Versuche zunächst über die Standard-Methode zu deregistrieren
            result = super.unregister(commandMap);
            
            // Zusätzliche aggressive Entfernung aus der CommandMap
            if (commandMap != null) {
                try {
                    // Hole die knownCommands-Map über Reflection
                    java.lang.reflect.Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                    knownCommandsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, org.bukkit.command.Command> knownCommands = 
                        (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);
                    
                    if (knownCommands != null) {
                        // Suche nach diesem Command und entferne alle Vorkommen
                        List<String> keysToRemove = new ArrayList<>();
                        for (Map.Entry<String, org.bukkit.command.Command> entry : knownCommands.entrySet()) {
                            if (entry.getValue() == this || 
                                (entry.getValue() instanceof DynamicCommand && 
                                 ((DynamicCommand)entry.getValue()).getName().equals(getName()) &&
                                 ((DynamicCommand)entry.getValue()).getModuleName().equals(moduleName))) {
                                keysToRemove.add(entry.getKey());
                            }
                        }
                        
                        // Entferne alle gefundenen Einträge
                        for (String key : keysToRemove) {
                            knownCommands.remove(key);
                        }
                    }
                } catch (Exception e) {
                    // Ignorieren - wir haben es zumindest versucht
                }
            }
        } catch (Exception e) {
            // Ignorieren - wir haben es zumindest versucht
        }
        
        // Befehl zurücksetzen
        resetCommand();
        
        return result;
    }
    
    /**
     * Überschreibe die onTabComplete-Methode, um sicherzustellen, dass deaktivierte Befehle
     * niemals Tab-Completion-Vorschläge anbieten
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        // WICHTIG: Wir prüfen, ob der Befehl zu einem deaktivierten Modul gehört
        if (getName().startsWith("disabled_") || 
            (!"apicore".equals(moduleName) && apiCore.getModuleInfo(moduleName) == null)) {
            return Collections.emptyList();
        }
        
        // Ansonsten die Standard-Implementierung verwenden
        return super.tabComplete(sender, alias, args);
    }

    /**
     * Führt das Standard-Verhalten für Befehle aus, wenn kein Modul-Handler vorhanden ist
     * 
     * @param sender Der Befehlsabsender
     * @param commandLabel Das Befehlslabel
     * @param args Die Befehlsargumente
     * @return true, wenn der Befehl erfolgreich ausgeführt wurde
     */
    private boolean executeDefault(CommandSender sender, String commandLabel, String[] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(NO_PERMISSION_MESSAGE);
            return true;
        }
        
        // Check if the module is disabled
        com.essentialscore.api.module.ModuleManager.ModuleInfo moduleInfo = apiCore.getModuleInfo(moduleName);
        if (moduleInfo == null && !moduleName.equalsIgnoreCase("apicore")) {
            sender.sendMessage(MODULE_DISABLED_MESSAGE);
            return true;
        }
        
        // Zeige Verwendung an
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#4DEEEB/" + commandLabel + " #A8A8A8- " + getDescription()));
        if (getUsage() != null && !getUsage().isEmpty()) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "#FFFFFF" + getUsage()));
        }
        
        return true;
    }

    /**
     * Setzt mehrere Befehlsinformationen in einem Schritt
     */
    public void setCommandInfo(String newName, String newDescription, String newUsage, List<String> newAliases, String newPermission) {
        if (newName != null && !newName.isEmpty()) {
            // Sicherer, die ursprünglichen Werte temporär zu speichern
            String originalName = this.getName();
            
            try {
                // Name überschreiben
                updateField(getClass().getSuperclass(), "name", newName);
                
                if (newAliases != null) {
                    @SuppressWarnings("unchecked")
                    List<String> aliasList = (List<String>) getField(getClass().getSuperclass(), "aliases");
                    aliasList.clear();
                    aliasList.addAll(newAliases);
                }
            } catch (Exception e) {
                apiCore.getLogger().warning("Fehler beim Aktualisieren des Befehlsnamens " + originalName + " zu " + newName + ": " + e.getMessage());
            }
        }
          if (newDescription != null) {
            this.setDescription(newDescription);
        }
        
        if (newUsage != null) {
            this.setUsage(newUsage);
        }
        
        if (newPermission != null) {
            this.setPermission(newPermission);
        }
    }

    // Add these helper methods for reflection

    /**
     * Aktualisiert ein Feld per Reflection
     * 
     * @param clazz Die Klasse, die das Feld enthält
     * @param fieldName Der Name des Felds
     * @param value Der neue Wert für das Feld
     * @throws Exception Bei Reflection-Fehlern
     */
    private void updateField(Class<?> clazz, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(this, value);
    }
    
    /**
     * Holt ein Feld per Reflection
     * 
     * @param clazz Die Klasse, die das Feld enthält
     * @param fieldName Der Name des Felds
     * @return Der Wert des Felds
     * @throws Exception Bei Reflection-Fehlern
     */
    private Object getField(Class<?> clazz, String fieldName) throws Exception {
        java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(this);
    }
} 
