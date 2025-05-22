# EssentialsCore Modul-Entwicklung

## Inhaltsverzeichnis
1. [Einführung](#einführung)
2. [Projekt-Setup](#projekt-setup)
3. [Modul-Struktur](#modul-struktur)
4. [API-Nutzung](#api-nutzung)
5. [Befehle und Events](#befehle-und-events)
6. [Konfiguration](#konfiguration)
7. [Performance-Optimierung](#performance-optimierung)
8. [Ressourcen-Management](#ressourcen-management)
9. [Konsolen-Formatierung](#konsolen-formatierung)
10. [API-Imports und Funktionen](#api-imports-und-funktionen)
11. [Best Practices](#best-practices)
12. [Beispiele](#beispiele)
13. [FAQ & Fehlerbehebung](#faq--fehlerbehebung)

## Einführung

EssentialsCore ist ein modulares Plugin-System für Minecraft Server, das die Entwicklung von Modulen vereinfacht. Die API wurde überarbeitet, um die Entwicklung zu vereinfachen und die Abhängigkeit vom Core zu reduzieren.

### Vorteile
- **Modularität**: Funktionalität kann in unabhängige Module aufgeteilt werden
- **API-Abstraktion**: Module interagieren nur mit der stabilen API
- **Versionsstabilität**: Updates des Cores beeinflussen Module nicht
- **Unabhängige Entwicklung**: Module können ohne tiefes Core-Wissen entwickelt werden

## Projekt-Setup

### Gradle (Empfohlen)

```gradle
plugins {
    id 'java'
    id 'maven-publish'
}

group = 'com.your.group'
version = '1.1.0'

repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
    maven {
        name = "papermc-repo"
        url = "https://repo.papermc.io/repository/maven-public/"
    }
}

dependencies {
    implementation 'com.github.Baumkrieger69:EssentialsCore:Tag'
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
}

def targetJavaVersion = 21
java {
    def javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Baumkrieger69</groupId>
        <artifactId>EssentialsCore</artifactId>
        <version>Tag</version>
    </dependency>
</dependencies>
```

## Modul-Struktur

### Basis-Modul

```java
import com.essentialscore.api.EssentialsCore;
import com.essentialscore.api.module.Module;
import com.essentialscore.api.module.ModuleInfo;

@ModuleInfo(
    name = "DeinModul",
    version = "1.1.0",
    description = "Beschreibung deines Moduls"
)
public class DeinModul extends Module {
    @Override
    public void onEnable() {
        // Modul wird aktiviert
    }

    @Override
    public void onDisable() {
        // Modul wird deaktiviert
    }
}
```

### module.yml

```yaml
name: DeinModul
version: 1.1.0
description: Beschreibung deines Moduls
main: com.your.package.DeinModul
author: DeinName
created: 2024-04-29 17:02:56

# Optional
api-version: 1.1
core-version: '>=2.0.0'

dependencies: []
soft-dependencies: []

permissions:
  deinmodul.befehl:
    description: Erlaubt die Nutzung des Hauptbefehls
    default: op
  deinmodul.admin:
    description: Erlaubt administrative Funktionen
    default: op

commands:
  - name: deinbefehl
    description: Führt eine Aktion aus
    usage: /deinbefehl [parameter]
    permission: deinmodul.befehl
    aliases: [db]
```

## API-Nutzung

### Module verwalten

```java
import com.essentialscore.api.EssentialsCore;
import com.essentialscore.api.module.ModuleManager;

ModuleManager moduleManager = EssentialsCore.getModuleManager();

// Modul abrufen
Module deinModul = moduleManager.getModule("DeinModul");

// Modul aktivieren/deaktivieren
moduleManager.enableModule("DeinModul");
moduleManager.disableModule("DeinModul");

// Alle Module auflisten
List<Module> modules = moduleManager.getModules();
```

### Konfiguration

```java
import com.essentialscore.api.config.Configuration;
import com.essentialscore.api.config.ConfigurationManager;

public class DeinModul extends Module {
    private Configuration config;

    @Override
    public void onEnable() {
        // Konfiguration laden
        config = ConfigurationManager.loadConfiguration(this, "config.yml");
        
        // Werte lesen
        boolean debug = config.getBoolean("settings.debug");
        String prefix = config.getString("settings.prefix");
        
        // Werte setzen
        config.set("settings.debug", true);
        config.save();
    }
}
```

## Befehle und Events

### Befehl registrieren

```java
import com.essentialscore.api.command.Command;
import com.essentialscore.api.command.CommandContext;

public class DeinModul extends Module {
    @Command(
        name = "deinbefehl",
        permission = "deinmodul.befehl",
        description = "Beschreibung deines Befehls",
        usage = "/deinbefehl <arg1> <arg2>"
    )
    public void onCommand(CommandContext context) {
        Player sender = context.getSender();
        String[] args = context.getArgs();
        
        if (args.length < 2) {
            sender.sendMessage("§cBitte gib alle Argumente an!");
            return;
        }
        
        // Befehl ausführen
    }
}
```

### Event-Handler

```java
import com.essentialscore.api.event.EventHandler;
import com.essentialscore.api.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventPriority;

@Listener
public class DeinModul extends Module {
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("§aWillkommen auf dem Server!");
    }
}
```

## Performance-Optimierung

### Asynchrone Verarbeitung

```java
// Asynchron ausführen
api.runAsync(() -> {
    // Zeitintensive Operationen
});

// Geplante Aufgabe
api.scheduleTask(() -> {
    // Einmalige Aufgabe
}, 20L); // 1 Sekunde Verzögerung

// Wiederkehrende Aufgabe
api.scheduleRepeatingTask(() -> {
    // Periodische Aufgabe
}, 20L, 20L); // Alle 1 Sekunde
```

### Caching

```java
private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();

// Daten cachen
public void cachePlayerData(UUID playerId, PlayerData data) {
    playerCache.put(playerId, data);
}

// Daten aus Cache lesen
public PlayerData getPlayerData(UUID playerId) {
    return playerCache.get(playerId);
}

// Cache leeren
public void clearCache() {
    playerCache.clear();
}
```

## Ressourcen-Management

### Ressourcen extrahieren

```java
// Bei Bedarf Ressourcen extrahieren
boolean extracted = core.extractModuleResourcesIfNeeded("MeinModul");
```

### Ressourcen lesen

```java
// Als Byte-Array
byte[] configData = core.getResourceAsBytes("MeinModul", "config/default.yml");

// Als String
String configText = core.getResourceAsString("MeinModul", "config/default.yml");

// Als InputStream
try (InputStream stream = core.getResourceAsStream("MeinModul", "images/logo.png")) {
    // Mit Stream arbeiten...
}
```

### Ressourcen speichern

```java
// Binärdaten speichern
byte[] imageData = /* Bilddaten */;
core.saveResource("MeinModul", "images/generated.png", imageData);

// Text speichern
String configContent = "key: value\nother: data";
core.saveResourceAsString("MeinModul", "config/generated.yml", configContent);
```

## Konsolen-Formatierung

### Nachrichtentypen

```java
// Zugriff auf die Konsolenformatierung
ConsoleFormatter console = apiCore.getConsoleFormatter();

// Nachrichtentypen
console.info("Standard-Information");
console.success("Erfolgsmeldung");
console.warning("Warnung");
console.error("Fehlermeldung");
console.debug("Debug-Information", isDebugMode);
console.important("Wichtige Information");
```

### Strukturelemente

```java
// Strukturelemente
console.header("Große Überschrift");
console.subHeader("Kleine Überschrift");
console.section("Abschnittsüberschrift");
console.line();
console.doubleLine();
console.blank();

// Listenelemente
console.listItem("Schlüssel", "Wert");
console.dataSection("Abschnitt", "Key1", "Value1", "Key2", "Value2");
```

## API-Imports und Funktionen

### Verfügbare API-Packages und Klassen

**Wichtig:** Aufgrund der Struktur des ClassLoaders können Module die folgenden API-Packages und Klassen importieren. Diese sind nun alle verfügbar und stabil:

```java
// Core API-Komponenten
import com.essentialscore.api.Module;                  // Hauptinterface für alle Module
import com.essentialscore.api.ModuleAPI;               // API für Zugriff auf Core-Funktionen
import com.essentialscore.api.BaseModule;              // Abstrakte Basisimplementierung von Module
import com.essentialscore.api.ModuleEventListener;     // Interface für modulübergreifende Events
import com.essentialscore.api.ModuleClassHelper;       // Hilfsklasse für API-Zugriff
import com.essentialscore.api.SimpleCommand;           // Einfache Befehlsimplementierung
import com.essentialscore.api.CommandDefinition;       // Interface für Befehlsdefinitionen

// Implementierungsklassen
import com.essentialscore.api.impl.CoreModuleAPI;      // Konkrete Implementierung des ModuleAPI-Interfaces
import com.essentialscore.api.impl.ModuleAdapter;      // Adapter für Module zur Kompatibilität

// Konfiguration
import com.essentialscore.api.config.Configuration;    // Interface für Konfigurationszugriff
import com.essentialscore.api.config.ConfigurationManager; // Manager für Konfigurationsdateien

// Events
import com.essentialscore.api.event.EventHandler;      // Annotation für Event-Handler
import com.essentialscore.api.event.Listener;          // Annotation für Event-Listener-Klassen
import com.essentialscore.api.event.EventPriority;     // Enum für Event-Prioritäten

// Befehle
import com.essentialscore.api.command.Command;         // Annotation für Befehle
import com.essentialscore.api.command.CommandContext;  // Kontext für Befehlsausführung

// Utility-Klassen
import com.essentialscore.api.util.StringUtils;        // String-Hilfsfunktionen
import com.essentialscore.api.util.FileUtils;          // Datei-Hilfsfunktionen

// Konsolen-Formatierung
import com.essentialscore.api.console.ConsoleFormatter; // Interface für Konsolenformatierung
```

### Anmerkungen zur API-Struktur

Der ModuleClassLoader ist so konfiguriert, dass er die folgenden Packages korrekt lädt:
- `com.essentialscore.api.*` (Basis-API)
- `com.essentialscore.api.impl.*` (Implementierungsklassen)
- `com.essentialscore.api.config.*` (Konfigurationsklassen)
- `com.essentialscore.api.event.*` (Event-bezogene Klassen)
- `com.essentialscore.api.command.*` (Befehlsbezogene Klassen)
- `com.essentialscore.api.util.*` (Utility-Klassen)
- `com.essentialscore.api.console.*` (Konsolenformatierung)

Diese Packages werden vom Haupt-Plugin-ClassLoader bereitgestellt und korrekt an Module weitergegeben. Alle anderen Packages im Plugin sind nicht verfügbar und sollten nicht direkt aus Modulen heraus referenziert werden.

### Funktionsübersicht der verfügbaren Klassen

| Import | Beschreibung | Hauptmethoden |
|--------|--------------|---------------|
| `Module` | Basisinterface für alle Module | `init()`, `onEnable()`, `onDisable()`, `onCommand()`, `onTabComplete()` |
| `ModuleAPI` | Hauptschnittstelle zum Core | `getPlugin()`, `registerCommands()`, `runAsync()`, `scheduleTask()`, `logInfo()` |
| `BaseModule` | Abstrakte Basisimplementierung | Implementiert Module-Interface mit nützlichen Hilfsmethoden |
| `ModuleEventListener` | Interface für modulübergreifende Events | `onModuleEvent()` |
| `CommandDefinition` | Interface für Befehlsdefinitionen | `getName()`, `getAliases()`, `getTabCompletionOptions()` |
| `SimpleCommand` | Einfache Befehlsimplementierung | `execute()`, `tabComplete()` |
| `ModuleClassHelper` | Hilfsklasse für API-Zugriff | Hilft beim Identifizieren verfügbarer API-Klassen |
| `Configuration` | Interface für Konfigurationszugriff | `get()`, `set()`, `save()`, `reload()` |
| `ConfigurationManager` | Manager für Konfigurationsdateien | `loadConfiguration()`, `saveConfiguration()` |
| `Command` | Annotation für Befehle | `name`, `permission`, `description`, `usage` |
| `CommandContext` | Kontext für Befehlsausführung | `getSender()`, `getArgs()`, `hasPermission()` |
| `EventHandler` | Annotation für Event-Handler | `priority()`, `ignoreCancelled()` |
| `EventPriority` | Enum für Event-Prioritäten | `LOWEST`, `LOW`, `NORMAL`, `HIGH`, `HIGHEST`, `MONITOR` |
| `Listener` | Annotation für Listener-Klassen | - |
| `StringUtils` | String-Hilfsfunktionen | `formatColors()`, `stripColors()`, `splitIntoLines()`, `capitalize()` |
| `FileUtils` | Datei-Hilfsfunktionen | `readFileToString()`, `writeStringToFile()`, `copyFile()`, `unzip()` |
| `ConsoleFormatter` | Konsolenformatierung | `info()`, `success()`, `warning()`, `error()`, `debug()`, `header()` |

### Beispiel: Korrektes Modul mit funktionierenden Imports

```java
package com.meinplugin.module;

import com.essentialscore.api.Module;
import com.essentialscore.api.ModuleAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Arrays;

public class MeinModul implements Module {
    private ModuleAPI api;
    private FileConfiguration config;
    private String name = "MeinModul";
    private String version = "1.1.0";
    private String description = "Ein Beispielmodul";

    @Override
    public void init(ModuleAPI api, FileConfiguration config) {
        this.api = api;
        this.config = config;
        api.logInfo("Modul " + name + " wurde initialisiert");
    }

    @Override
    public void onDisable() {
        api.logInfo("Modul " + name + " wurde deaktiviert");
    }
    
    @Override
    public boolean onCommand(String commandName, CommandSender sender, String[] args) {
        if (commandName.equalsIgnoreCase("meinbefehl")) {
            sender.sendMessage("Befehl ausgeführt!");
            return true;
        }
        return false;
    }
    
    @Override
    public List<String> onTabComplete(String commandName, CommandSender sender, String[] args) {
        if (commandName.equalsIgnoreCase("meinbefehl") && args.length == 1) {
            return Arrays.asList("option1", "option2", "option3");
        }
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
```

### Empfohlene Praxis für Module

1. Implementieren Sie das `Module`-Interface oder erweitern Sie `BaseModule`
2. Nutzen Sie die Methoden von `ModuleAPI` für alle Interaktionen mit dem Core
3. Registrieren Sie Befehle über `ModuleAPI.registerCommands()`
4. Verarbeiten Sie Befehle in der `onCommand()`-Methode
5. Führen Sie asynchrone Aufgaben mit `ModuleAPI.runAsync()` aus

## Best Practices

### 1. Modul-Struktur
- Halte Module klein und fokussiert
- Nutze klare, beschreibende Namen
- Dokumentiere deine Module
- Trenne Logik in separate Klassen
- Nutze Interfaces für Abstraktion

### 2. Konfiguration
- Nutze YAML für Konfigurationen
- Stelle Standardwerte bereit
- Validiere Konfigurationswerte
- Nutze Kommentare in Konfigurationsdateien
- Implementiere Konfigurations-Reload

### 3. Befehle
- Nutze aussagekräftige Befehl-Namen
- Implementiere Hilfe-Befehle
- Prüfe Berechtigungen
- Validiere Argumente
- Nutze Unterbefehle für komplexe Befehle

### 4. Events
- Nutze Event-Prioritäten sinnvoll
- Behandle Events effizient
- Vermeide Blocking-Operationen
- Nutze Event-Cancelling vorsichtig
- Dokumentiere Event-Abhängigkeiten

### 5. Fehlerbehandlung
- Implementiere try-catch Blöcke
- Logge Fehler angemessen
- Stelle benutzerfreundliche Fehlermeldungen bereit
- Validiere Eingaben
- Nutze Assertions für Debugging

## Beispiele

### Welcome-Modul

```java
@ModuleInfo(
    name = "WelcomeModule",
    version = "1.1.0",
    description = "Ein einfaches Welcome-Modul"
)
@Listener
public class WelcomeModule extends Module {
    private Configuration config;

    @Override
    public void onEnable() {
        config = ConfigurationManager.loadConfiguration(this, "config.yml");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = config.getString("messages.welcome", "&aWillkommen!");
        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
```

### Konfigurationsdatei

```yaml
# config.yml
messages:
  welcome: "&aWillkommen auf dem Server!"
  error: "&cEin Fehler ist aufgetreten!"
```

## FAQ & Fehlerbehebung

### Häufige Probleme

1. **Modul wird nicht geladen**
   - Prüfe die `module.yml` auf Syntaxfehler
   - Stelle sicher, dass die Hauptklasse korrekt angegeben ist
   - Überprüfe die Abhängigkeiten

2. **Befehle funktionieren nicht**
   - Prüfe die Berechtigungen
   - Stelle sicher, dass die Befehl-Namen korrekt sind
   - Überprüfe die Command-Annotationen

3. **Events werden nicht ausgelöst**
   - Prüfe die `@Listener` Annotation
   - Stelle sicher, dass die Event-Priorität korrekt ist
   - Überprüfe die Event-Handler-Methoden

### Debugging

```java
// Debug-Modus aktivieren
api.setDebugMode(true);

// Logging
api.getLogger().info("Info-Nachricht");
api.getLogger().warning("Warnung");
api.getLogger().severe("Fehler");
```

## Support

Bei Fragen oder Problemen:
- Erstelle ein Issue auf GitHub
- Nutze die Diskussions-Sektion
- Kontaktiere uns über Discord

## Lizenz

Dieses Projekt ist unter der MIT-Lizenz lizenziert. Siehe [LICENSE](LICENSE) für Details. 