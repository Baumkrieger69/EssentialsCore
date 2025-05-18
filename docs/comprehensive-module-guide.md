# Umfassende Anleitung zur EssentialsCore-Modulentwicklung

Dieses Dokument ist eine vollständige Referenz für die Entwicklung von Modulen für EssentialsCore. Es erklärt alle Aspekte von der Einrichtung bis zur Veröffentlichung eigener Module.

## Inhaltsverzeichnis

1. [Grundprinzipien](#grundprinzipien)
2. [Projekteinrichtung](#projekteinrichtung)
3. [Modulstruktur](#modulstruktur)
4. [API-Nutzung](#api-nutzung)
5. [Berechtigungssystem](#berechtigungssystem)
6. [Befehle und Events](#befehle-und-events)
7. [Konfigurationen](#konfigurationen)
8. [Tests und Debugging](#tests-und-debugging)
9. [Performance-Optimierung](#performance-optimierung)
10. [Veröffentlichung](#veröffentlichung)
11. [Module ohne Core-Abhängigkeit](#module-ohne-core-abhängigkeit)
12. [Migration von alten Modulen](#migration-von-alten-modulen)
13. [FAQ & Fehlerbehebung](#faq--fehlerbehebung)
14. [Beispiele](#beispiele)

## Grundprinzipien

EssentialsCore bietet eine modulare Architektur mit folgenden Vorteilen:

- **Modularität**: Funktionalität kann in unabhängige Module aufgeteilt werden
- **API-Abstraktion**: Module interagieren nur mit der stabilen API, nicht mit Core-Implementierungen
- **Versionsstabilität**: Updates des Cores beeinflussen Module nicht, solange die API stabil bleibt
- **Unabhängige Entwicklung**: Module können ohne tiefes Wissen über den Core-Code entwickelt werden

## Projekteinrichtung

### Gradle-Setup

Hier ist ein typisches `build.gradle` Setup für ein Modul:

```groovy
plugins {
    id 'java'
    id 'maven-publish'
}

group = 'com.deinplugin'
version = '1.0.0'
description = 'Mein EssentialsCore-Modul'

repositories {
    mavenCentral()
    // Für Bukkit/Spigot APIs
    maven { url = 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/' }
    // Für EssentialsCore
    maven { url = 'https://jitpack.io' }
}

dependencies {
    // NUR die API verwenden, nicht den gesamten Core
    compileOnly 'com.github.EssentialsCore:EssentialsCore-API:1.0.0'
    
    // Spigot/Bukkit API
    compileOnly 'org.spigotmc:spigot-api:1.19.2-R0.1-SNAPSHOT'
}

// Java 8 oder höher
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Ressourcen richtig verarbeiten
processResources {
    // Platzhalter in module.yml ersetzen
    filesMatching('module.yml') {
        expand(
            'name': project.name,
            'version': project.version,
            'description': project.description
        )
    }
}

// Aufgabe zum Erstellen des Moduls
task buildModule(type: Jar) {
    from sourceSets.main.output
    archiveFileName = "${project.name}-${project.version}.jar"
    
    // module.yml muss im Hauptverzeichnis liegen
    filesMatching('module.yml') {
        path = 'module.yml'
    }
    
    // Metadaten in Manifest
    manifest {
        attributes(
            'Built-By': System.properties['user.name'],
            'Created-By': "Gradle ${gradle.gradleVersion}",
            'Built-Date': new Date().format("yyyy-MM-dd HH:mm:ss"),
            'Module-Version': project.version
        )
    }
}

// JAR-Task anpassen
jar {
    finalizedBy buildModule
}

// OPTIONAL: In Entwicklungsserver kopieren
task copyToServer(type: Copy, dependsOn: buildModule) {
    from buildModule.archiveFile
    into "C:/Server/plugins/EssentialsCore/modules"
    // Passe den Pfad an deinen Entwicklungsserver an
}

// OPTIONAL: Build-Task automatisch zum Server kopieren
// build.finalizedBy copyToServer
```

### Verzeichnisstruktur

Eine typische Modulstruktur sieht so aus:

```
MeinModul/
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── deinplugin/
│   │   │           ├── MeinModul.java
│   │   │           ├── commands/
│   │   │           │   └── MeinBefehl.java
│   │   │           ├── listeners/
│   │   │           │   └── MeinListener.java
│   │   │           └── utils/
│   │   │               └── MeineHilfsmethoden.java
│   │   └── resources/
│   │       ├── module.yml
│   │       └── config.yml
│   └── test/
│       └── java/
│           └── com/
│               └── deinplugin/
│                   └── MeinModulTest.java
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

### Module.yml-Konfiguration

Die `module.yml`-Datei ist die wichtigste Konfigurationsdatei für dein Modul:

```yaml
name: ${name}
version: ${version}
description: ${description}
main: com.deinplugin.MeinModul
author: DeinName
created: 2023-07-15 17:02:56

# API und Core-Version (optional, aber empfohlen)
api-version: 1.1
core-version: '>=2.0.0'

# Abhängigkeiten
dependencies: []
soft-dependencies: []

# Berechtigungen
permissions:
  meinmodul.befehl:
    description: Erlaubt die Nutzung des Hauptbefehls
    default: op
  meinmodul.admin:
    description: Erlaubt administrative Funktionen
    default: op

# Befehle
commands:
  - name: meinbefehl
    description: Führt eine Aktion aus
    usage: /meinbefehl [parameter]
    permission: meinmodul.befehl
    aliases: [mb]
```

#### Feldübersicht

| Feld             | Typ      | Pflicht?   | Beschreibung                                                                                 | Beispiel                        |
|------------------|----------|------------|---------------------------------------------------------------------------------------------|----------------------------------|
| name             | String   | Ja         | Eindeutiger Modulname, sollte mit der Hauptklasse übereinstimmen                            | `TeleportationModule`           |
| main             | String   | Ja         | Vollqualifizierter Name der Hauptklasse (mit Package)                                       | `com.example.MyModule`          |
| description      | String   | Ja         | Kurzbeschreibung, wird im Admin-Panel/Hilfe angezeigt                                       | `Erweiterte Teleport-Funktionen`|
| version          | String   | Ja         | Modulversion, am besten nach [SemVer](https://semver.org/lang/de/)                          | `1.0.0`                         |
| author           | String   | Ja         | Name des Autors oder Teams                                                                  | `Baumkrieger69`                 |
| created          | String   | Empfohlen  | Erstellungsdatum und -zeit, z.B. für Support und Nachverfolgung                             | `2023-07-15 17:02:56`           |
| api-version      | String   | Empfohlen  | Version der EssentialsCore-API, für die das Modul entwickelt wurde                          | `1.1`                           |
| core-version     | String   | Empfohlen  | Minimale oder empfohlene Core-Version, z.B. `'>=2.0.0'`                                     | `'>=2.0.0'`                     |
| dependencies     | Liste    | Optional   | Liste zwingend benötigter Module (werden vor diesem geladen)                                | `['BaseModule']`                |
| soft-dependencies| Liste    | Optional   | Liste optionaler Module (werden geladen, falls vorhanden)                                   | `['Vault']`                     |
| commands         | Liste    | Optional   | Liste aller Befehle, die das Modul bereitstellt                                             | Siehe Beispiel oben             |

## Modulstruktur

### Das Module-Interface

Der wichtigste Weg, ein Modul zu erstellen, ist das `Module`-Interface zu implementieren:

```java
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import org.bukkit.configuration.file.FileConfiguration;

public class MeinModul implements Module {
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
        return "MeinModul";
    }
    
    @Override
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        // Befehlslogik
        return true;
    }
    
    @Override
    public List<String> onTabComplete(String commandName, CommandSender sender, String[] args) {
        // Tab-Completion-Logik
        return null;
    }
    
    @Override
    public void onModuleEvent(String eventName, Map<String, Object> data) {
        // Event-Handling-Logik
    }
}
```

### Die BaseModule-Klasse

Alternativ kannst du die `BaseModule`-Klasse erweitern für einfachere Implementierung:

```java
import com.essentialscore.api.BaseModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MeinModul extends BaseModule {
    
    public MeinModul() {
        super("MeinModul", "1.0.0", "Ein Beispielmodul");
    }
    
    @Override
    protected void onInitialize() {
        // Code nach der Initialisierung
        logInfo("Modul wurde initialisiert");
        
        // Befehle registrieren
        api.registerCommands(Arrays.asList(
            createCommand("meinbefehl", "Beschreibung", "/meinbefehl", "meinmodul.befehl")
        ));
        
        // Event-Listener registrieren
        api.registerModuleListener("anderesmodul.event", this::onCustomEvent);
    }
    
    private void onCustomEvent(String eventName, Map<String, Object> data) {
        logInfo("Event empfangen: " + eventName);
    }
    
    @Override
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        if ("meinbefehl".equals(commandName)) {
            sender.sendMessage(api.formatHex("#00FF00Mein Befehl wurde ausgeführt!"));
            return true;
        }
        return false;
    }
}
```

## API-Nutzung

Die ModuleAPI bietet zahlreiche Methoden:

### Befehle

```java
// Befehl erstellen
CommandDefinition command = createCommand(
    "meinbefehl", 
    "Führt eine Aktion aus", 
    "/meinbefehl [parameter]", 
    "meinmodul.befehl"
);

// Befehl registrieren
api.registerCommands(Collections.singletonList(command));
```

### Berechtigungen

```java
// Berechtigungsprüfung
if (api.hasPermission(player, "meinmodul.permission")) {
    // Zugriff erlauben
}
```

### Logger

```java
// Logging-Funktionen
api.logInfo("Informationsmeldung");
api.logWarning("Warnmeldung");
api.logError("Fehlermeldung", exception);
api.logDebug("Debug-Meldung"); // Nur im Debug-Modus sichtbar
```

### Daten teilen

```java
// Daten mit anderen Modulen teilen
api.setSharedData("meinmodul.wichtigerWert", meinWert);

// Daten aus anderen Modulen abrufen
Object sharedValue = api.getSharedData("anderesmodul.sharedValue");
```

### Formatierung

```java
// Hex-Farben in Nachrichten
String message = api.formatHex("#FF5555Dieser Text ist rot! #00FF00Dieser Text ist grün!");
player.sendMessage(message);
```

### Asynchrone Aufgaben

```java
// Asynchrone Aufgaben ausführen
api.runAsync(() -> {
    // Code der asynchron ausgeführt wird
    return "Ergebnis";
}).thenAccept(result -> {
    // Code der wieder im Hauptthread ausgeführt wird
    logInfo("Ergebnis: " + result);
});
```

### Geplante Aufgaben

```java
// Verzögerte Aufgabe
int taskId = api.scheduleTask(() -> {
    logInfo("Diese Aufgabe wurde verzögert ausgeführt");
}, 20); // 20 Ticks = 1 Sekunde

// Wiederholte Aufgabe
int repeatingTaskId = api.scheduleRepeatingTask(() -> {
    logInfo("Diese Aufgabe wird wiederholt ausgeführt");
}, 20, 100); // Nach 1 Sekunde starten, alle 5 Sekunden wiederholen

// Aufgabe abbrechen
api.cancelTask(taskId);
```

## Berechtigungssystem

### Berechtigungen in module.yml definieren

```yaml
permissions:
  meinmodul.use:
    description: Erlaubt die grundlegende Nutzung des Moduls
    default: true
  meinmodul.admin:
    description: Erlaubt administrative Funktionen
    default: op
  meinmodul.command.special:
    description: Erlaubt die Nutzung spezieller Befehle
    default: op
```

### Berechtigungen prüfen

```java
// IMMER die API-Methode verwenden, nicht player.hasPermission direkt!
if (api.hasPermission(player, "meinmodul.admin")) {
    // Admin-Funktionalität ausführen
} else {
    player.sendMessage(api.formatHex("#FF5555Du hast keine Berechtigung!"));
}
```

### Wildcards

Das Berechtigungssystem unterstützt Wildcards:

- `meinmodul.*` gewährt alle Berechtigungen des Moduls
- `meinmodul.command.*` gewährt alle Befehlsberechtigungen

## Befehle und Events

### Befehle definieren

```java
// Mit BaseModule
CommandDefinition command = createCommand(
    "meinbefehl",              // Name
    "Ein Beispielbefehl",      // Beschreibung
    "/meinbefehl [argument]",  // Verwendung
    "meinmodul.command.basic"  // Berechtigung
);

// Tab-Completion hinzufügen
command.addTabCompletionOptions(0, Arrays.asList("option1", "option2"));

// Befehl registrieren
api.registerCommands(Collections.singletonList(command));
```

### Befehle verarbeiten

```java
@Override
public boolean onCommand(String commandName, CommandSender sender, String[] args) {
    if ("meinbefehl".equals(commandName)) {
        if (args.length == 0) {
            sender.sendMessage(api.formatHex("#FF5555Bitte gib ein Argument an!"));
            return true;
        }
        
        String argument = args[0];
        sender.sendMessage(api.formatHex("#00FF00Befehl mit Argument " + argument + " ausgeführt!"));
        return true;
    }
    return false;
}
```

### Event-System

EssentialsCore bietet ein eigenes Event-System für modulübergreifende Kommunikation:

```java
// Event-Listener registrieren
api.registerModuleListener("economy.transaction", (eventName, data) -> {
    Player player = (Player) data.get("player");
    double amount = (Double) data.get("amount");
    logInfo("Transaktion: " + player.getName() + " hat " + amount + " erhalten");
});

// Event auslösen
Map<String, Object> eventData = new HashMap<>();
eventData.put("player", player);
eventData.put("amount", 100.0);
api.fireModuleEvent("meinmodul.aktion", eventData);
```

## Konfigurationen

### Konfigurationen laden

```java
// Konfiguration über die init-Methode
@Override
public void init(ModuleAPI api, FileConfiguration config) {
    this.api = api;
    this.config = config;
    
    // Konfigurationswerte laden
    String message = config.getString("messages.welcome", "Willkommen!");
    boolean featureEnabled = config.getBoolean("features.special", true);
}
```

### Benutzerdefinierte Konfigurationen

```java
// Eigene Konfigurationsdatei laden
File customConfigFile = new File(api.getModuleDataFolder(null), "custom-config.yml");
if (!customConfigFile.exists()) {
    try {
        customConfigFile.createNewFile();
    } catch (IOException e) {
        api.logError("Konnte Konfigurationsdatei nicht erstellen", e);
    }
}

YamlConfiguration customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

// Werte setzen und speichern
customConfig.set("custom-value", "Wert");
try {
    customConfig.save(customConfigFile);
} catch (IOException e) {
    api.logError("Konnte Konfiguration nicht speichern", e);
}
```

## Tests und Debugging

### Debug-Modus

```java
// Debug-Logging (nur sichtbar, wenn Debug-Modus aktiviert ist)
api.logDebug("Detaillierte Debug-Information");

// Debug-Modus prüfen
if (api.isDebugMode()) {
    // Zusätzliche Debugging-Ausgaben oder -Funktionalität
}
```

### Fehlerbehandlung

```java
try {
    // Riskanter Code
} catch (Exception e) {
    api.logError("Ein Fehler ist aufgetreten", e);
    // Saubere Fehlerbehandlung
}
```

## Performance-Optimierung

### Asynchrone Verarbeitung & Threading

**Wann asynchron arbeiten?**
- Immer bei: Datenbankzugriffen, großen Dateioperationen, Webrequests, langen Berechnungen
- Niemals im Hauptthread blockieren! Das führt zu Lags für alle Spieler

**Wie?**
```java
api.runAsync(() -> {
    // Zeitintensive Operationen
});
```

### Caching

Wiederholte Zugriffe auf langsame Ressourcen (Dateien, DB) vermeiden:

```java
private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();
```

Achte darauf, Daten aus dem Cache zu entfernen, wenn sie nicht mehr gebraucht werden (z.B. bei Spieler-Logout).

### Performance-Messung

Du kannst Methodenlaufzeiten messen:

```java
long start = System.nanoTime();
// ... Code ...
api.trackMethodTime(getName(), "meineMethode", start);
```

### Deadlock-Erkennung

Der Core überwacht regelmäßig alle Threads und erkennt Deadlocks automatisch. Bei Problemen werden Warnungen in die Konsole geschrieben.

## Veröffentlichung

### Verwendung mit JitPack

Du kannst dein Modul über JitPack verfügbar machen:

1. Veröffentliche deinen Code auf GitHub
2. Erstelle einen Git-Tag für deine Version:
   ```bash
   git tag -a v1.0.0 -m "Version 1.0.0"
   git push origin v1.0.0
   ```
3. Nutzer können dein Modul dann in ihren Projekten verwenden:
   ```groovy
   repositories {
       maven { url 'https://jitpack.io' }
   }
   
   dependencies {
       compileOnly 'com.github.DeinName:DeinRepository:v1.0.0'
   }
   ```

### Installation in Minecraft-Servern

Um dein Modul in einem Server zu installieren:

1. Erstelle die JAR-Datei mit `./gradlew build`
2. Kopiere die JAR-Datei in den `plugins/EssentialsCore/modules`-Ordner
3. Starte den Server neu oder lade das Modul mit `/apicore reload <modulname>`

## Module ohne Core-Abhängigkeit

### Vorteile der Unabhängigkeit

- **Stabilität**: Module sind von Änderungen im Core-Code nicht mehr direkt betroffen
- **Einfacheres Testing**: Module können leichter einzeln getestet werden
- **Bessere Kompatibilität**: Reduziert Versionskompatibilitätsprobleme
- **Sauberere Struktur**: Klare Trennung zwischen API und Implementierung

### Die API-Schnittstelle verwenden

Um unabhängige Module zu erstellen, verwendest du ausschließlich die Klassen und Interfaces aus dem `com.essentialscore.api`-Paket:

```java
import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import org.bukkit.configuration.file.FileConfiguration;

public class MeinModul implements Module {
    private ModuleAPI api;
    private FileConfiguration config;
    
    @Override
    public void init(ModuleAPI api, FileConfiguration config) {
        this.api = api;
        this.config = config;
        // Initialisierungscode hier
    }
    
    // Weitere Methoden implementieren...
}
```

### Berechtigungsprüfung nutzen

Verwende immer die API-Methode für Berechtigungsprüfungen:

```java
// Korrekte Methode für Berechtigungsprüfung
if (api.hasPermission(player, "meinmodul.permission")) {
    // Zugriff erlauben
}
```

Die API leitet die Anfrage automatisch an den Core-PermissionManager weiter, der Wildcards und LuckPerms-Integration unterstützt.

### Gemeinsamen Code teilen

Um Funktionalität zwischen Modulen zu teilen, ohne vom Core abzuhängen:

1. Erstelle ein separates Utility-Modul mit gemeinsamen Methoden
2. Verwende die `getSharedData()` und `setSharedData()`-Methoden der API

```java
// Daten mit anderen Modulen teilen
api.setSharedData("meinmodul.wichtigerWert", meinWert);

// Daten aus anderen Modulen abrufen
Object sharedValue = api.getSharedData("anderesmodul.sharedValue");
```

## Migration von alten Modulen

Wenn du bestehende Module auf die neue API umstellen möchtest:

1. Ersetze direkte Core-Aufrufe durch API-Methodenaufrufe
2. Implementiere das `Module`-Interface oder erweitere `BaseModule`
3. Ersetze direkte Berechtigungsprüfungen (`player.hasPermission()`) durch `api.hasPermission()`
4. Ersetze direkte Befehlsregistrierung durch die API-Methoden
5. Aktualisiere die `module.yml` auf das neue Format

## FAQ & Fehlerbehebung

### Häufige Probleme und Lösungen

**Modul wird nicht geladen:**
- Überprüfe die `module.yml` auf Syntaxfehler
- Stelle sicher, dass der Hauptklassenname exakt mit dem in `main` angegebenen übereinstimmt
- Prüfe, ob alle Abhängigkeiten verfügbar sind

**Berechtigungen funktionieren nicht:**
- Stelle sicher, dass du `api.hasPermission()` verwendest, nicht `player.hasPermission()`
- Überprüfe, ob die Berechtigungen korrekt in der `module.yml` definiert sind

**Befehle werden nicht registriert:**
- Stelle sicher, dass du `api.registerCommands()` aufrufst
- Prüfe, ob die Befehle bereits von einem anderen Plugin registriert wurden

## Beispiele

### Einfaches Modul

```java
import com.essentialscore.api.BaseModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EinfachesModul extends BaseModule {
    
    public EinfachesModul() {
        super("EinfachesModul", "1.0.0", "Ein einfaches Beispielmodul");
    }
    
    @Override
    protected void onInitialize() {
        logInfo("Modul wurde initialisiert");
        
        api.registerCommands(Arrays.asList(
            createCommand("hello", "Zeigt eine Begrüßung", "/hello", "einfachesmodul.hello")
        ));
    }
    
    @Override
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        if ("hello".equals(commandName)) {
            sender.sendMessage(api.formatHex("#00FF00Hallo von EinfachesModul!"));
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (api.hasPermission(player, "einfachesmodul.admin")) {
                    sender.sendMessage(api.formatHex("#FFAA00Du bist ein Administrator!"));
                }
            }
            
            return true;
        }
        return false;
    }
}
```

### Komplexeres Modul mit Events

```java
import com.essentialscore.api.BaseModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class KomplexesModul extends BaseModule {
    private Map<UUID, Integer> spielerPunkte = new HashMap<>();
    
    public KomplexesModul() {
        super("KomplexesModul", "1.0.0", "Ein komplexeres Beispielmodul");
    }
    
    @Override
    protected void onInitialize() {
        logInfo("Komplexes Modul wird initialisiert...");
        
        // Befehle registrieren
        api.registerCommands(Arrays.asList(
            createCommand("punkte", "Verwalte Punkte", "/punkte <add|remove|get> [spieler] [menge]", "komplexesmodul.punkte"),
            createCommand("event", "Löst ein Event aus", "/event", "komplexesmodul.event")
        ));
        
        // Event-Listener registrieren
        api.registerModuleListener("spielermodule.levelup", this::onSpielerLevelUp);
        
        // Geplante Aufgabe einrichten
        api.scheduleRepeatingTask(() -> {
            // Alle 5 Minuten Statistiken loggen
            logInfo("Aktive Spieler mit Punkten: " + spielerPunkte.size());
        }, 6000, 6000); // 5 Minuten (in Ticks)
    }
    
    private void onSpielerLevelUp(String eventName, Map<String, Object> data) {
        Player player = (Player) data.get("player");
        int level = (Integer) data.get("level");
        
        // Belohne Spieler bei Level-Up
        addPunkte(player.getUniqueId(), level * 10);
        player.sendMessage(api.formatHex("#00FFAA" + level * 10 + " Punkte für Level-Up erhalten!"));
        
        // Event-Daten loggen
        logDebug("Spieler " + player.getName() + " hat Level " + level + " erreicht");
    }
    
    private void addPunkte(UUID playerId, int amount) {
        spielerPunkte.put(playerId, spielerPunkte.getOrDefault(playerId, 0) + amount);
        
        // Speichere in SharedData für andere Module
        api.setSharedData("komplexesmodul.punkte." + playerId, spielerPunkte.get(playerId));
    }
    
    @Override
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        if ("punkte".equals(commandName)) {
            if (args.length < 1) {
                sender.sendMessage(api.formatHex("#FF5555Verwendung: /punkte <add|remove|get> [spieler] [menge]"));
                return true;
            }
            
            String aktion = args[0].toLowerCase();
            
            if ("get".equals(aktion)) {
                if (!(sender instanceof Player) && args.length < 2) {
                    sender.sendMessage(api.formatHex("#FF5555Du musst einen Spieler angeben!"));
                    return true;
                }
                
                UUID playerId;
                String playerName;
                
                if (args.length >= 2) {
                    Player target = api.getPlugin().getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(api.formatHex("#FF5555Spieler nicht gefunden!"));
                        return true;
                    }
                    playerId = target.getUniqueId();
                    playerName = target.getName();
                } else {
                    playerId = ((Player) sender).getUniqueId();
                    playerName = sender.getName();
                }
                
                int punkte = spielerPunkte.getOrDefault(playerId, 0);
                sender.sendMessage(api.formatHex("#00FF00" + playerName + " hat " + punkte + " Punkte!"));
                return true;
            }
            
            // Rest der Befehlslogik...
            return true;
        } else if ("event".equals(commandName)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(api.formatHex("#FF5555Dieser Befehl kann nur von Spielern ausgeführt werden!"));
                return true;
            }
            
            Player player = (Player) sender;
            
            // Event auslösen
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", player);
            eventData.put("timestamp", System.currentTimeMillis());
            api.fireModuleEvent("komplexesmodul.event", eventData);
            
            sender.sendMessage(api.formatHex("#00FF00Event wurde ausgelöst!"));
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<String> onTabComplete(String commandName, CommandSender sender, String[] args) {
        if ("punkte".equals(commandName)) {
            if (args.length == 1) {
                List<String> completions = Arrays.asList("add", "remove", "get");
                return filterTabCompletions(completions, args[0]);
            } else if (args.length == 2 && !args[0].equalsIgnoreCase("get")) {
                return null; // Standardliste aller Spieler
            }
        }
        
        return Collections.emptyList();
    }
    
    private List<String> filterTabCompletions(List<String> options, String current) {
        if (current.isEmpty()) {
            return options;
        }
        
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList());
    }
} 