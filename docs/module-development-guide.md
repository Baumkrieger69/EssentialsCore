# EssentialsCore Modul-Entwicklungshandbuch

## Inhaltsverzeichnis

1. [Einführung](#einführung)
2. [Neue API-Struktur](#neue-api-struktur)
3. [module.yml – Die Modul-Definitionsdatei](#moduleyml--die-modul-definitionsdatei)
4. [Module erstellen](#module-erstellen)
   - [Das Module-Interface](#das-module-interface)
   - [BaseModule verwenden](#basemodule-verwenden)
5. [ModuleAPI verwenden](#moduleapi-verwenden)
   - [Wichtige Methoden](#wichtige-methoden)
   - [Ereignisverarbeitung](#ereignisverarbeitung)
6. [Befehle registrieren](#befehle-registrieren)
   - [CommandDefinition-Interface](#commanddefinition-interface)
   - [SimpleCommand verwenden](#simplecommand-verwenden)
7. [Konfiguration und Daten](#konfiguration-und-daten)
8. [Best Practices](#best-practices)
9. [Migration von alten Modulen](#migration-von-alten-modulen)
10. [Beispiele](#beispiele)
11. [FAQ & Fehlerbehebung](#faq--fehlerbehebung)

## Einführung

Die EssentialsCore-API wurde überarbeitet, um die Entwicklung von Modulen zu vereinfachen und die Abhängigkeit vom Core zu reduzieren. Die neue API-Struktur verwendet Interfaces und Abstraktionen, um eine saubere Trennung zwischen dem Core und den Modulen zu gewährleisten.

Dieses Handbuch erklärt, wie du mit der neuen API Module entwickeln kannst und wie du bestehende Module migrieren kannst.

## Neue API-Struktur

Die neue API besteht aus mehreren Kernkomponenten:

- **Module**: Das Hauptinterface für Module
- **ModuleAPI**: Interface für den Zugriff auf Core-Funktionalitäten
- **CommandDefinition**: Interface für die Definition von Befehlen
- **ModuleEventListener**: Interface für die Verarbeitung von Ereignissen
- **BaseModule**: Abstrakte Implementierung von Module mit Hilfsmethoden

Diese Komponenten befinden sich im Package `com.essentialscore.api`.

## module.yml – Die Modul-Definitionsdatei

Jedes EssentialsCore-Modul benötigt eine Datei `module.yml` im Wurzelverzeichnis des Moduls (im JAR). Diese Datei beschreibt das Modul, seine Metadaten und Abhängigkeiten. Sie ist für das Laden, die Verwaltung und die Kompatibilität des Moduls unerlässlich.

### Aufbau und Felder

Eine typische `module.yml`-Struktur sieht so aus:

```yaml
name: <Modulname>                        # (String, Pflicht) Eindeutiger Name des Moduls
main: <vollqualifizierte.Hauptklasse>    # (String, Pflicht) z.B. com.example.MyModule
description: <Beschreibung>              # (String, Pflicht) Kurzbeschreibung für Admins
version: <Versionsnummer>                # (String, Pflicht) z.B. 1.0.0
author: <Autor oder Team>                # (String, Pflicht) z.B. Max Mustermann
created: <Datum und Zeit>                # (String, empfohlen) z.B. 2025-04-29 17:02:56

# Optional:
api-version: <API-Version>               # (String, empfohlen) z.B. 1.1
core-version: <Core-Versionsbereich>     # (String, empfohlen) z.B. '>=2.0.0'
dependencies:                            # (Liste, optional) Module, die zwingend geladen sein müssen
  - <ModulA>
  - <ModulB>
soft-dependencies:                       # (Liste, optional) Module, die optional geladen werden
  - <ModulC>
  - <ModulD>

commands:                                # (Liste von Objekten, optional)
  - name: <befehl>
    description: <Beschreibung>
    usage: </befehl [argumente]>
    permission: <benötigte.berechtigung>
    aliases: [<alias1>, <alias2>]
  # ... weitere Befehle ...
```

#### Feldübersicht (Tabelle)

| Feld             | Typ      | Pflicht?   | Beschreibung                                                                                 | Beispiel                        |
|------------------|----------|------------|---------------------------------------------------------------------------------------------|----------------------------------|
| name             | String   | Ja         | Eindeutiger Modulname, sollte mit der Hauptklasse übereinstimmen                            | `TeleportationModule`           |
| main             | String   | Ja         | Vollqualifizierter Name der Hauptklasse (mit Package)                                       | `com.example.MyModule`          |
| description      | String   | Ja         | Kurzbeschreibung, wird im Admin-Panel/Hilfe angezeigt                                       | `Erweiterte Teleport-Funktionen`|
| version          | String   | Ja         | Modulversion, am besten nach [SemVer](https://semver.org/lang/de/)                          | `1.0.0`                         |
| author           | String   | Ja         | Name des Autors oder Teams                                                                  | `Baumkrieger69`                 |
| created          | String   | Empfohlen  | Erstellungsdatum und -zeit, z.B. für Support und Nachverfolgung                             | `2025-04-29 17:02:56`           |
| api-version      | String   | Empfohlen  | Version der EssentialsCore-API, für die das Modul entwickelt wurde                          | `1.1`                           |
| core-version     | String   | Empfohlen  | Minimale oder empfohlene Core-Version, z.B. `'>=2.0.0'`                                     | `'>=2.0.0'`                     |
| dependencies     | Liste    | Optional   | Liste zwingend benötigter Module (werden vor diesem geladen)                                | `['BaseModule']`                |
| soft-dependencies| Liste    | Optional   | Liste optionaler Module (werden geladen, falls vorhanden)                                   | `['Vault']`                     |
| commands         | Liste    | Optional   | Liste aller Befehle, die das Modul bereitstellt (siehe unten)                               | Siehe unten                     |

##### Command-Objekte

Jeder Eintrag in `commands` ist ein Objekt mit:

| Feld        | Typ    | Pflicht? | Beschreibung                                  | Beispiel                |
|-------------|--------|----------|-----------------------------------------------|-------------------------|
| name        | String | Ja       | Name des Befehls (ohne `/`)                   | `home`                  |
| description | String | Ja       | Beschreibung für die Hilfe                    | `Teleportiert dich ...` |
| usage       | String | Ja       | Syntax/Verwendung, wie sie Spielern angezeigt wird | `/home [name]`     |
| permission  | String | Ja       | Benötigte Berechtigung (kann leer sein)       | `teleport.home`         |
| aliases     | Liste  | Nein     | Alternative Namen für den Befehl              | `[h, heim]`             |

**Beispiel:**
```yaml
commands:
  - name: home
    description: Teleportiert dich zu deinem Zuhause
    usage: /home [name]
    permission: teleport.home
    aliases: [h]
```

##### Tipps & Fehlerquellen

- **Einrückungen:** YAML ist sehr empfindlich! Immer 2 Leerzeichen pro Ebene, keine Tabs.
- **Listen:** Immer mit `-` beginnen, z.B. bei `commands`, `dependencies`, `aliases`.
- **Pflichtfelder:** Fehlen Pflichtfelder, wird das Modul nicht geladen.
- **main:** Muss exakt mit dem Klassennamen (inkl. Package) übereinstimmen.
- **Versionen:** Halte dich an SemVer, damit Abhängigkeiten sauber aufgelöst werden können.
- **Aliases:** Können leer sein (`[]`), aber nicht weglassen, wenn das Feld erwartet wird.
- **YAML-Validator:** Nutze Tools wie https://yamlchecker.com/ um Syntaxfehler zu vermeiden.

---

## Performance-Optimierung und Core-Performance-Features

EssentialsCore bietet zahlreiche Funktionen, um performante Module zu ermöglichen und Performance-Probleme frühzeitig zu erkennen und zu vermeiden.

### 1. Asynchrone Verarbeitung & Threading

**Wann asynchron arbeiten?**
- Immer bei: Datenbankzugriffen, großen Dateioperationen, Webrequests, langen Berechnungen.
- Niemals im Hauptthread blockieren! Das führt zu Lags für alle Spieler.

**Wie?**
```java
api.runAsync(() -> {
    // Zeitintensive Operationen
});
```
- Nutze auch `api.scheduleTask` und `api.scheduleRepeatingTask` für geplante/periodische Aufgaben.

**ThreadPool:**  
EssentialsCore stellt einen optimierten ThreadPool bereit, der für alle Module genutzt wird. Du musst dich nicht um Thread-Management kümmern.

**Anti-Pattern:**
```java
// Falsch! Blockiert den Server-Thread:
public void onCommand(...) {
    doHeavyDatabaseQuery(); // Niemals direkt!
}
```

### 2. Caching

**Warum?**  
Wiederholte Zugriffe auf langsame Ressourcen (Dateien, DB) vermeiden.

**Wie?**
- Nutze eigene Caches (z.B. `ConcurrentHashMap`) für häufig genutzte Daten.
- EssentialsCore bietet einen OffHeapCache für große Datenmengen und einen Permission-Cache.

**Beispiel:**
```java
private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();
```

**Achte auf:**
- Entferne Daten aus dem Cache, wenn sie nicht mehr gebraucht werden (z.B. bei Spieler-Logout).

### 3. Performance-Messung & Debugging

**Methodenzeit messen:**
```java
long start = System.nanoTime();
// ... Code ...
api.trackMethodTime(getName(), "meineMethode", start);
```
- So kannst du Engpässe identifizieren.

**Debug-Modus aktivieren:**  
In der Core-Konfiguration:  
```yaml
general:
  debug-mode: true
```
- Liefert detaillierte Logs, Warnungen bei hoher Speicherauslastung, Deadlocks, etc.

**Server-Konsole beobachten:**
- EssentialsCore gibt Warnungen bei hoher Speicherauslastung oder Deadlocks aus.

### 4. Deadlock-Erkennung & Thread-Monitoring

- Der Core überwacht regelmäßig alle Threads und erkennt Deadlocks automatisch.
- Bei Problemen werden Warnungen in die Konsole geschrieben.
- Du kannst das Intervall und das Verhalten in der Core-Konfiguration anpassen:
```yaml
performance:
  thread-monitoring: true
  monitoring-interval: 60 # Sekunden
  auto-fix-deadlocks: false
```

### 5. Events & Listener performant nutzen

- **Vermeide teure Operationen in Event-Handlern:**
  - Prüfe, ob der Event wirklich für dein Modul relevant ist.
  - Führe aufwändige Berechnungen asynchron aus.
- **Nutze das Modul-Event-System für lose Kopplung:**
  - So können Module effizient miteinander kommunizieren, ohne sich gegenseitig zu blockieren.

**Beispiel:**
```java
ModuleEventListener listener = (eventName, data) -> {
    if ("player.levelup".equals(eventName)) {
        api.runAsync(() -> {
            // Aufwändige Verarbeitung
        });
    }
};
api.registerModuleListener("player.levelup", listener);
```

### 6. Skalierung & Parallelisierung

- **Verteile Aufgaben auf mehrere Threads:**
  - Nutze den ThreadPool des Cores für parallele Aufgaben.
- **Vermeide Synchronisationsprobleme:**
  - Greife nicht gleichzeitig von mehreren Threads auf dieselben Datenstrukturen zu (nutze z.B. `ConcurrentHashMap`).

### 7. Best Practices für performante Module
- Halte Methoden kurz und übersichtlich.
- Vermeide große Schleifen im Hauptthread.
- Nutze die Core-API für alle wiederkehrenden Aufgaben (Logging, Caching, Async, Events).
- Teste dein Modul mit vielen Spielern und simulierten Lasten.
- Überwache die Auswirkungen deines Moduls auf die Tick-Zeit und den Speicher.

### 8. Typische Performance-Fallen
- Große Datenmengen synchron laden/speichern.
- Unnötige Iterationen über alle Spieler/Objekte.
- Häufige Zugriffe auf langsame Ressourcen (z.B. Netzwerk, Festplatte).
- Nicht entfernte Listener oder Tasks nach Modul-Deaktivierung.

### 9. Beispiel: Performantes Modul-Grundgerüst
```java
public class FastModule extends BaseModule {
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    @Override
    protected void onInitialize() {
        // Lade Daten asynchron
        api.runAsync(this::loadAllData);
        // Plane regelmäßiges Speichern
        api.scheduleRepeatingTask(this::saveAllData, 20*60*5, 20*60*5); // alle 5 Minuten
    }

    private void loadAllData() {
        // ... Datenbank- oder Dateizugriff ...
    }

    private void saveAllData() {
        // ... Datenbank- oder Dateizugriff ...
    }

    @Override
    public void onPlayerJoin(Player player) {
        api.runAsync(() -> loadPlayerData(player));
    }

    private void loadPlayerData(Player player) {
        // ...
    }

    @Override
    public void onDisable() {
        saveAllData();
    }
}
```

## Module erstellen

### Das Module-Interface

Um ein Modul zu erstellen, implementiere das `Module`-Interface:

```java
package com.mymodule;

import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class MyModule implements Module {
    private ModuleAPI api;
    private FileConfiguration config;
    
    @Override
    public void init(ModuleAPI api, FileConfiguration config) {
        this.api = api;
        this.config = config;
        // Initialisierungscode hier
    }
    
    @Override
    public String getName() {
        return "MyModule";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Ein Beispielmodul für EssentialsCore";
    }
    
    @Override
    public List<String> getDependencies() {
        return Collections.emptyList();
    }
    
    @Override
    public void onDisable() {
        // Aufräumcode hier
    }
    
    @Override
    public void onPlayerJoin(Player player) {
        // Wird aufgerufen, wenn ein Spieler den Server betritt
    }
    
    @Override
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        // Befehlsverarbeitung
        return true;
    }
    
    @Override
    public List<String> onTabComplete(String commandName, CommandSender sender, String[] args) {
        // Tab-Vervollständigung
        return null;
    }
}
```

### BaseModule verwenden

Für eine einfachere Implementierung kannst du die abstrakte Klasse `BaseModule` verwenden, die viele Standardmethoden bereits implementiert:

```java
package com.mymodule;

import com.essentialscore.api.BaseModule;
import com.essentialscore.api.CommandDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class MyModule extends BaseModule {
    
    public MyModule() {
        super("MyModule", "1.0.0", "Ein Beispielmodul für EssentialsCore");
    }
    
    @Override
    protected void onInitialize() {
        // Wird nach dem Setzen von api und config aufgerufen
        logInfo("MyModule wurde initialisiert!");
        
        // Befehle registrieren
        CommandDefinition command = createCommand("mycommand", 
            "Ein Beispielbefehl", 
            "/mycommand <parameter>", 
            "mymodule.command");
            
        api.registerCommands(Arrays.asList(command));
    }
    
    @Override
    public void onDisable() {
        logInfo("MyModule wird deaktiviert...");
    }
    
    @Override
    public void onPlayerJoin(Player player) {
        player.sendMessage("Willkommen auf dem Server!");
    }
    
    @Override
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        if ("mycommand".equals(commandName)) {
            sender.sendMessage(api.formatHex("#4DEEEB[MyModule] #FFFFFFBefehl ausgeführt!"));
            return true;
        }
        return false;
    }
}
```

## ModuleAPI verwenden

Die `ModuleAPI` ist die Hauptschnittstelle für den Zugriff auf Core-Funktionalitäten. Sie wird deinem Modul während der Initialisierung übergeben.

### Wichtige Methoden

```java
// Logging
api.logInfo("Eine Informationsnachricht");
api.logWarning("Eine Warnmeldung");
api.logError("Eine Fehlermeldung", exception);
api.logDebug("Eine Debug-Nachricht");

// Dateizugriff
File dataFolder = api.getModuleDataFolder("MyModule");
File configFile = api.getModuleConfigFile("MyModule");
File resourcesFolder = api.getModuleResourcesFolder("MyModule");

// Berechtigungen
boolean hasPermission = api.hasPermission(player, "mymodule.permission");

// Geteilte Daten
api.setSharedData("mymodule.key", value);
Object value = api.getSharedData("mymodule.key");

// Formatierung
String formattedMessage = api.formatHex("#4DEEEB[MyModule] #FFFFFFHallo Welt!");

// Asynchrone Aufgaben
api.runAsync(() -> {
    // Code, der asynchron ausgeführt werden soll
});

// Geplante Aufgaben
int taskId = api.scheduleTask(() -> {
    // Code, der später ausgeführt werden soll
}, 20L); // 20 Ticks = 1 Sekunde

// Wiederholende Aufgaben
int repeatingTaskId = api.scheduleRepeatingTask(() -> {
    // Code, der wiederholt ausgeführt werden soll
}, 20L, 100L); // Starte nach 1 Sekunde, wiederhole alle 5 Sekunden

// Aufgabe abbrechen
api.cancelTask(taskId);
```

### Ereignisverarbeitung

Die neue API bietet ein flexibles Ereignissystem, das nicht auf Bukkits Ereignissystem beschränkt ist:

```java
// Ereignis-Listener erstellen
ModuleEventListener listener = (eventName, data) -> {
    if ("player.levelup".equals(eventName)) {
        Player player = (Player) data.get("player");
        int newLevel = (int) data.get("level");
        player.sendMessage("Glückwunsch! Du hast Level " + newLevel + " erreicht!");
    }
};

// Listener registrieren
api.registerModuleListener("player.levelup", listener);

// Ereignis auslösen (in einem anderen Modul)
Map<String, Object> eventData = new HashMap<>();
eventData.put("player", player);
eventData.put("level", newLevel);
api.fireModuleEvent("player.levelup", eventData);

// Listener entfernen
api.unregisterModuleListener("player.levelup", listener);
```

## Befehle registrieren

### CommandDefinition-Interface

Die neue API verwendet das `CommandDefinition`-Interface für die Definition von Befehlen:

```java
public class MyCommand implements CommandDefinition {
    @Override
    public String getName() {
        return "mycommand";
    }
    
    @Override
    public String getDescription() {
        return "Ein Beispielbefehl";
    }
    
    @Override
    public String getUsage() {
        return "/mycommand <parameter>";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("mc", "mycmd");
    }
    
    @Override
    public String getPermission() {
        return "mymodule.command";
    }
    
    @Override
    public String getModuleName() {
        return "MyModule";
    }
    
    @Override
    public List<String> getTabCompletionOptions(int argIndex) {
        if (argIndex == 0) {
            return Arrays.asList("option1", "option2", "option3");
        }
        return null;
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        sender.sendMessage("Befehl ausgeführt!");
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("option1", "option2", "option3").stream()
                .filter(option -> option.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
```

### SimpleCommand verwenden

Die `BaseModule`-Klasse bietet eine einfachere Möglichkeit, Befehle zu erstellen:

```java
// In deiner BaseModule-Unterklasse
CommandDefinition command = createCommand("mycommand", 
    "Ein Beispielbefehl", 
    "/mycommand <parameter>", 
    "mymodule.command")
    .addAlias("mc")
    .addAlias("mycmd");

api.registerCommands(Arrays.asList(command));
```

Die Befehlsausführung und Tab-Vervollständigung werden dann über die `onCommand`- und `onTabComplete`-Methoden deines Moduls verarbeitet.

## Konfiguration und Daten

Die Konfiguration wird deinem Modul während der Initialisierung übergeben:

```java
@Override
public void init(ModuleAPI api, FileConfiguration config) {
    this.api = api;
    this.config = config;
    
    // Konfigurationswerte lesen
    boolean featureEnabled = config.getBoolean("features.myfeature", true);
    String message = config.getString("messages.welcome", "Willkommen!");
    int cooldown = config.getInt("settings.cooldown", 60);
    
    // ...
}
```

Für den Zugriff auf Datendateien:

```java
// Datenordner des Moduls
File dataFolder = api.getModuleDataFolder("MyModule");
File customDataFile = new File(dataFolder, "data.yml");

// Konfigurationsdatei laden
FileConfiguration data = YamlConfiguration.loadConfiguration(customDataFile);

// Daten lesen und schreiben
data.set("players." + player.getUniqueId() + ".lastLogin", System.currentTimeMillis());
data.save(customDataFile);
```

## Best Practices

### Allgemeine Empfehlungen
1. **Verwende die neue API**: Nutze die neuen Interfaces und Klassen anstatt direkt auf den Core zuzugreifen.
2. **Minimiere Abhängigkeiten**: Vermeide direkte Abhängigkeiten zu anderen Modulen. Verwende stattdessen das Ereignissystem.
3. **Fehlerbehandlung**: Fange Ausnahmen ab und protokolliere sie mit den API-Logging-Methoden.
4. **Ressourcenmanagement**: Gib Ressourcen frei, wenn dein Modul deaktiviert wird.
5. **Asynchrone Verarbeitung**: Führe aufwändige Operationen asynchron aus, um die Serverleistung nicht zu beeinträchtigen.
6. **Konfiguration validieren**: Überprüfe Konfigurationswerte auf Gültigkeit und setze sinnvolle Standardwerte.
7. **Nutze module.yml sinnvoll**: Definiere alle relevanten Felder, um Kompatibilität und Wartbarkeit zu erhöhen.
8. **Dokumentiere deinen Code**: Kommentiere wichtige Methoden und Abläufe für andere Entwickler.
9. **Nutze Logging gezielt**: Verwende die Logging-Methoden der API für Debugging und Fehleranalyse.
10. **Teste dein Modul isoliert**: Teste neue Module zunächst auf einem Testserver.

### Typische Fehlerquellen
- Falscher oder fehlender `main`-Klasseneintrag in der `module.yml`.
- Nicht implementiertes `Module`-Interface.
- Fehlende Abhängigkeiten (Dependencies) nicht korrekt angegeben.
- Fehlerhafte YAML-Syntax (z.B. Einrückungen, Tabs statt Leerzeichen).
- Vergessene Ressourcen im JAR (z.B. `module.yml` nicht im Wurzelverzeichnis).

### Debugging & Performance
- Aktiviere den Debug-Modus in der Core-Konfiguration für ausführlichere Logs.
- Nutze asynchrone Methoden für langlaufende Aufgaben.
- Überwache Speicher- und Thread-Nutzung bei komplexen Modulen.

### Sicherheit
- Überprüfe alle Eingaben von Spielern sorgfältig.
- Setze sinnvolle Berechtigungen für Befehle und Aktionen.
- Vermeide Reflection, wenn es nicht unbedingt nötig ist.

## Migration von alten Modulen

Um ein bestehendes Modul auf die neue API zu migrieren:

1. Ändere die Implementierung von `ApiCore.ApiModule` zu `com.essentialscore.api.Module`.

2. Ersetze direkte Aufrufe von `ApiCore`-Methoden durch entsprechende `ModuleAPI`-Methoden.

3. Aktualisiere die Befehlsregistrierung, um das neue `CommandDefinition`-Interface zu verwenden.

4. Ersetze die Verwendung von `ApiCore.ModuleEventListener` durch `com.essentialscore.api.ModuleEventListener`.

5. Erwäge die Verwendung von `BaseModule` für eine einfachere Implementierung.

Beispiel für die Migration:

**Alt:**
```java
public class MyModule implements ApiCore.ApiModule {
    private ApiCore core;
    
    @Override
    public void init(ApiCore core, FileConfiguration config) {
        this.core = core;
        // ...
    }
    
    // ...
}
```

**Neu:**
```java
public class MyModule implements Module {
    private ModuleAPI api;
    
    @Override
    public void init(ModuleAPI api, FileConfiguration config) {
        this.api = api;
        // ...
    }
    
    // ...
}
```

## Beispiele

### Einfaches Modul

```java
package com.example;

import com.essentialscore.api.BaseModule;
import com.essentialscore.api.CommandDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SimpleModule extends BaseModule {
    
    public SimpleModule() {
        super("SimpleModule", "1.0.0", "Ein einfaches Beispielmodul");
    }
    
    @Override
    protected void onInitialize() {
        logInfo("SimpleModule wird initialisiert...");
        
        // Befehl registrieren
        CommandDefinition helloCommand = createCommand("hello", 
            "Sendet eine Begrüßungsnachricht", 
            "/hello [name]", 
            "simplemodule.hello");
            
        api.registerCommands(Collections.singletonList(helloCommand));
    }
    
    @Override
    public void onDisable() {
        logInfo("SimpleModule wird deaktiviert...");
    }
    
    @Override
    public void onPlayerJoin(Player player) {
        player.sendMessage(api.formatHex("#4DEEEB[SimpleModule] #FFFFFFWillkommen auf dem Server!"));
    }
    
    @Override
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        if ("hello".equals(commandName)) {
            String target = args.length > 0 ? args[0] : sender.getName();
            sender.sendMessage(api.formatHex("#4DEEEB[SimpleModule] #FFFFFFHallo, " + target + "!"));
            return true;
        }
        return false;
    }
    
    @Override
    public List<String> onTabComplete(String commandName, CommandSender sender, String[] args) {
        if ("hello".equals(commandName) && args.length == 1) {
            // Liste aller Spielernamen zurückgeben, die mit dem eingegebenen Text beginnen
            String prefix = args[0].toLowerCase();
            return api.getPlugin().getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
    }
}
```

### Ereignisbasiertes Modul

```java
package com.example;

import com.essentialscore.api.BaseModule;
import com.essentialscore.api.ModuleEventListener;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class EventModule extends BaseModule {
    
    public EventModule() {
        super("EventModule", "1.0.0", "Ein ereignisbasiertes Modul");
    }
    
    @Override
    protected void onInitialize() {
        logInfo("EventModule wird initialisiert...");
        
        // Ereignis-Listener registrieren
        ModuleEventListener playerEventListener = (eventName, data) -> {
            if ("player.achievement".equals(eventName)) {
                Player player = (Player) data.get("player");
                String achievement = (String) data.get("achievement");
                
                player.sendMessage(api.formatHex("#4DEEEB[EventModule] #FFFFFFGlückwunsch zum Erreichen von: " + achievement));
                
                // Ereignis auslösen, dass ein Spieler eine Errungenschaft erreicht hat
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("player", player);
                notificationData.put("message", "hat die Errungenschaft " + achievement + " erreicht!");
                
                api.fireModuleEvent("notification.broadcast", notificationData);
            }
        };
        
        api.registerModuleListener("player.achievement", playerEventListener);
    }
    
    @Override
    public void onDisable() {
        logInfo("EventModule wird deaktiviert...");
        // Hier könnten wir die Listener deregistrieren, wenn nötig
    }
    
    @Override
    public void onPlayerJoin(Player player) {
        // Beispiel für das Auslösen eines Ereignisses
        Map<String, Object> joinData = new HashMap<>();
        joinData.put("player", player);
        joinData.put("timestamp", System.currentTimeMillis());
        
        api.fireModuleEvent("player.joined", joinData);
    }
}
```

### Datenverarbeitungsmodul

```java
package com.example;

import com.essentialscore.api.BaseModule;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataModule extends BaseModule {
    private Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;
    
    public DataModule() {
        super("DataModule", "1.0.0", "Ein Datenverarbeitungsmodul");
    }
    
    @Override
    protected void onInitialize() {
        logInfo("DataModule wird initialisiert...");
        
        // Datenordner und -datei initialisieren
        File dataFolder = api.getModuleDataFolder(getName());
        dataFile = new File(dataFolder, "playerdata.yml");
        
        // Daten laden
        loadData();
        
        // Regelmäßiges Speichern der Daten planen
        api.scheduleRepeatingTask(this::saveData, 20 * 60 * 5, 20 * 60 * 5); // Alle 5 Minuten
    }
    
    private void loadData() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Daten aus der Konfiguration in den Cache laden
        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String name = dataConfig.getString("players." + uuidStr + ".name");
                    int logins = dataConfig.getInt("players." + uuidStr + ".logins");
                    long lastLogin = dataConfig.getLong("players." + uuidStr + ".lastLogin");
                    
                    playerDataCache.put(uuid, new PlayerData(uuid, name, logins, lastLogin));
                } catch (IllegalArgumentException e) {
                    logWarning("Ungültige UUID in der Datendatei: " + uuidStr);
                }
            }
        }
        
        logInfo("Spielerdaten für " + playerDataCache.size() + " Spieler geladen.");
    }
    
    private void saveData() {
        // Cache in die Konfiguration schreiben
        for (PlayerData data : playerDataCache.values()) {
            String path = "players." + data.getUuid().toString();
            dataConfig.set(path + ".name", data.getName());
            dataConfig.set(path + ".logins", data.getLogins());
            dataConfig.set(path + ".lastLogin", data.getLastLogin());
        }
        
        // Konfiguration speichern
        try {
            dataConfig.save(dataFile);
            logInfo("Spielerdaten gespeichert.");
        } catch (IOException e) {
            logError("Fehler beim Speichern der Spielerdaten", e);
        }
    }
    
    @Override
    public void onDisable() {
        logInfo("DataModule wird deaktiviert...");
        saveData(); // Daten beim Deaktivieren speichern
    }
    
    @Override
    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerDataCache.get(uuid);
        
        if (data == null) {
            // Neuer Spieler
            data = new PlayerData(uuid, player.getName(), 1, System.currentTimeMillis());
            playerDataCache.put(uuid, data);
            
            logInfo("Neuer Spieler: " + player.getName());
        } else {
            // Existierender Spieler
            data.setName(player.getName()); // Name aktualisieren, falls er sich geändert hat
            data.setLogins(data.getLogins() + 1);
            data.setLastLogin(System.currentTimeMillis());
            
            logInfo("Spieler " + player.getName() + " hat sich zum " + data.getLogins() + ". Mal eingeloggt.");
        }
    }
    
    // Innere Klasse für Spielerdaten
    private static class PlayerData {
        private final UUID uuid;
        private String name;
        private int logins;
        private long lastLogin;
        
        public PlayerData(UUID uuid, String name, int logins, long lastLogin) {
            this.uuid = uuid;
            this.name = name;
            this.logins = logins;
            this.lastLogin = lastLogin;
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getLogins() {
            return logins;
        }
        
        public void setLogins(int logins) {
            this.logins = logins;
        }
        
        public long getLastLogin() {
            return lastLogin;
        }
        
        public void setLastLogin(long lastLogin) {
            this.lastLogin = lastLogin;
        }
    }
}
```

## FAQ & Fehlerbehebung

### Häufige Probleme

**Fehler: `module.yml not found`**
- Stelle sicher, dass die Datei im Wurzelverzeichnis des Moduls liegt und korrekt benannt ist.

**Fehler: `main class not found`**
- Prüfe, ob der Eintrag `main:` in der `module.yml` exakt dem vollqualifizierten Klassennamen entspricht.

**Fehler: `NoClassDefFoundError` oder `ClassNotFoundException`**
- Prüfe, ob alle Abhängigkeiten korrekt im Modul-JAR enthalten sind oder als Dependency deklariert wurden.

**Fehlerhafte YAML-Syntax**
- Nutze einen YAML-Validator (z.B. https://yamlchecker.com/), um Einrückungs- und Formatierungsfehler zu finden.

**Befehle werden nicht erkannt**
- Prüfe, ob die Befehle in der `module.yml` und/oder im Code korrekt registriert wurden.

**Modul wird nicht geladen**
- Prüfe die EssentialsCore-Logausgabe auf Hinweise und Fehler.
- Überprüfe die Kompatibilität von `api-version` und `core-version`.

### Weitere Hilfe
- Siehe die [EssentialsCore GitHub Issues](https://github.com/DeinProjekt/EssentialsCore/issues) für bekannte Probleme.
- Stelle Fragen im Entwickler-Discord oder Forum.

---

# EssentialsCore: Architektur, Hauptfunktionen & Modul-Integration

## Überblick über die Architektur

EssentialsCore ist modular aufgebaut und bietet eine Vielzahl von Schnittstellen und Diensten, die Module nutzen können. Die wichtigsten Komponenten sind:

| Komponente           | Zweck                                                                                 |
|----------------------|--------------------------------------------------------------------------------------|
| ApiCore              | Hauptklasse, Einstiegspunkt, verwaltet Manager, Module, Events, Commands, Ressourcen |
| ModuleManager        | Lädt, initialisiert, deaktiviert und überwacht Module                                 |
| ConfigManager        | Verwalten und Laden von Konfigurationsdateien                                        |
| ThreadManager        | Stellt ThreadPool und Async-API bereit                                               |
| PermissionManager    | Verwaltung und Prüfung von Berechtigungen                                            |
| CommandManager       | Registrierung und Ausführung von Befehlen                                            |
| PerformanceMonitor   | Überwacht Performance, erkennt Engpässe und Deadlocks                                |
| ModuleResourceManager| Verwaltung und Extraktion von Ressourcen aus Modulen                                 |
| Shared Data          | Ermöglicht Modulen, Daten untereinander auszutauschen                                |

### Wichtige Core-Funktionen

- **Event-System:** Ermöglicht lose gekoppelte Kommunikation zwischen Modulen und dem Core.
- **API-Schicht:** Über `ModuleAPI` erhalten Module Zugriff auf alle wichtigen Core-Funktionen.
- **Threading & Async:** Zentraler ThreadPool, Async-API für performante Module.
- **Caching:** OffHeapCache, Permission-Cache, Methoden-Cache für schnelle Zugriffe.
- **Command-System:** Dynamische Registrierung und Verwaltung von Befehlen.
- **Performance-Monitoring:** Überwachung von Methodenlaufzeiten, Deadlocks, Speicher.
- **Ressourcenmanagement:** Automatische Extraktion und Verwaltung von Ressourcen aus Modulen.
- **Logging:** Einheitliches, farbiges Logging mit verschiedenen Kategorien.

## Wie Module mit dem Core interagieren

### 1. Über das `ModuleAPI`-Interface
Jedes Modul erhält beim Start eine Instanz von `ModuleAPI`, die alle wichtigen Core-Funktionen kapselt:

| Methode                                 | Zweck                                                                 |
|------------------------------------------|-----------------------------------------------------------------------|
| `logInfo(String)`                        | Loggt eine Info-Nachricht                                             |
| `logWarning(String)`                     | Loggt eine Warnung                                                    |
| `logError(String, Throwable)`            | Loggt einen Fehler mit Exception                                      |
| `logDebug(String)`                       | Loggt eine Debug-Nachricht                                            |
| `runAsync(Runnable)`                     | Führt Code asynchron im Core-ThreadPool aus                           |
| `scheduleTask(Runnable, long)`           | Plant eine Aufgabe verzögert                                          |
| `scheduleRepeatingTask(Runnable, long, long)` | Plant eine wiederholende Aufgabe                                 |
| `cancelTask(int)`                        | Bricht eine geplante Aufgabe ab                                       |
| `getModuleDataFolder(String)`            | Gibt den Datenordner eines Moduls zurück                              |
| `getModuleConfigFile(String)`            | Gibt die Konfigurationsdatei eines Moduls zurück                      |
| `getModuleResourcesFolder(String)`       | Gibt den Ressourcenordner eines Moduls zurück                         |
| `setSharedData(String, Object)`          | Setzt einen Wert im globalen Shared-Data-Cache                        |
| `getSharedData(String)`                  | Holt einen Wert aus dem Shared-Data-Cache                             |
| `hasPermission(Player, String)`          | Prüft, ob ein Spieler eine bestimmte Berechtigung hat                 |
| `registerModuleListener(String, ModuleEventListener)` | Registriert einen Listener für ein benutzerdefiniertes Event |
| `unregisterModuleListener(String, ModuleEventListener)` | Entfernt einen Event-Listener                                 |
| `fireModuleEvent(String, Map<String, Object>)` | Löst ein benutzerdefiniertes Event aus                         |
| `registerCommands(List<CommandDefinition>)` | Registriert Befehle für das Modul                                 |
| `unregisterCommands(List<CommandDefinition>)` | Deregistriert Befehle                                             |
| `formatHex(String)`                      | Formatiert Nachrichten mit Hex-Farbcodes                              |
| `trackMethodTime(String, String, long)`  | Zeichnet die Laufzeit einer Methode auf                               |

### 2. Über Events
- Module können eigene Events definieren und auf Events anderer Module/Core reagieren.
- Beispiel:
```java
ModuleEventListener listener = (eventName, data) -> {
    if ("player.levelup".equals(eventName)) {
        // ...
    }
};
api.registerModuleListener("player.levelup", listener);
```

### 3. Über Befehle
- Module können beliebig viele Befehle über das Command-System registrieren.
- Die Definition erfolgt in der `module.yml` und/oder im Code (siehe CommandDefinition).

### 4. Über Shared Data
- Ermöglicht den Austausch von Daten zwischen Modulen ohne direkte Abhängigkeit.
- Beispiel:
```java
api.setSharedData("my.key", myObject);
Object value = api.getSharedData("my.key");
```

### 5. Über Ressourcen und Konfiguration
- Jedes Modul hat einen eigenen Daten-, Ressourcen- und Konfigurationsordner.
- Zugriff über die API:
```java
File dataFolder = api.getModuleDataFolder(getName());
File configFile = api.getModuleConfigFile(getName());
File resourcesFolder = api.getModuleResourcesFolder(getName());
```

### 6. Über Permissions
- Der Core bietet ein zentrales Permission-Management.
- Prüfe Berechtigungen immer über die API:
```java
if (api.hasPermission(player, "mymodule.admin")) {
    // ...
}
```

## Schritt-für-Schritt: Ein neues Modul für EssentialsCore entwickeln

1. **Projektstruktur anlegen**
   - Erstelle ein neues Java-Projekt mit Package-Struktur, z.B. `com.example.teleport`.
   - Lege die Datei `module.yml` im Wurzelverzeichnis an (siehe oben für Struktur).

2. **Hauptklasse schreiben**
   - Implementiere das `Module`-Interface oder erweitere `BaseModule`.
   - Beispiel:
```java
public class TeleportModule extends BaseModule {
    public TeleportModule() {
        super("TeleportModule", "1.0.0", "Teleport-Funktionen");
    }
    @Override
    protected void onInitialize() {
        logInfo("TeleportModule initialisiert!");
        // Befehle registrieren, Listener setzen, etc.
    }
    @Override
    public void onDisable() {
        logInfo("TeleportModule wird deaktiviert...");
    }
}
```

3. **Befehle definieren**
   - In der `module.yml` und/oder als `CommandDefinition`-Klassen.
   - Beispiel für einen einfachen Command:
```java
public class HomeCommand implements CommandDefinition {
    // ... siehe CommandDefinition-Abschnitt ...
}
```

4. **Events nutzen**
   - Eigene Events auslösen oder auf Core-/Modul-Events reagieren (siehe oben).

5. **Async, Caching, Performance nutzen**
   - Async-API für alles, was lange dauert.
   - Caching für häufig genutzte Daten.
   - Methodenlaufzeiten messen, Debug-Mode nutzen.

6. **Ressourcen und Konfiguration**
   - Nutze die API, um auf Daten- und Ressourcenordner zuzugreifen.
   - Lade und speichere Konfigurationen mit Bukkit-API oder Core-Methoden.

7. **Testing & Troubleshooting**
   - Teste das Modul auf einem Testserver.
   - Aktiviere den Debug-Mode für detaillierte Logs.
   - Prüfe die EssentialsCore-Konsole auf Fehler und Warnungen.

## Best Practices & Fehlerquellen

- **Immer die API nutzen, nicht direkt auf Core-Klassen zugreifen!**
- **Async für alles, was länger dauert!**
- **Befehle und Events sauber deregistrieren, wenn das Modul deaktiviert wird.**
- **YAML-Syntax in der module.yml prüfen!**
- **Permissions immer prüfen, bevor Aktionen ausgeführt werden.**
- **Nutze Logging für alle wichtigen Aktionen und Fehler.**
- **Teste mit vielen Spielern und unter Last.**

## Troubleshooting

| Problem                        | Ursache & Lösung                                                                 |
|--------------------------------|---------------------------------------------------------------------------------|
| Modul wird nicht geladen       | Pflichtfelder in module.yml fehlen, main-Klasse falsch, Syntaxfehler             |
| Befehle funktionieren nicht    | Nicht korrekt in module.yml oder Code registriert                                |
| Events werden nicht ausgelöst  | Listener nicht registriert, Event-Name falsch                                    |
| Performance-Probleme           | Blockierende Operationen im Main-Thread, kein Async, kein Caching                |
| Deadlocks/Fehler im Threading  | Zu viele parallele Zugriffe, keine Thread-sicheren Strukturen, Core-Logs prüfen   |
| Permissions greifen nicht      | Falscher Permission-String, Permission nicht gesetzt, Core-API nicht genutzt      |

---

Diese Dokumentation bietet einen umfassenden Überblick über die neue API und wie sie für die Modulentwicklung verwendet werden kann. Für weitere Informationen oder bei Fragen wende dich bitte an das Entwicklerteam. 