# EssentialsCore
<<<<<<< HEAD

Ein modulares Minecraft Plugin-System für Paper/Spigot Server.

## Inhaltsverzeichnis
1. [Installation](#installation)
2. [Modul-Entwicklung](#modul-entwicklung)
3. [Core-Funktionalität](#core-funktionalität)
4. [API-Referenz](#api-referenz)
5. [Ressourcen-Management](#ressourcen-management)
6. [Konsolen-Formatierung](#konsolen-formatierung)
7. [Performance-Optimierung](#performance-optimierung)
8. [Best Practices](#best-practices)
9. [Beispiele](#beispiele)
10. [Support](#support)

## Installation

### Gradle (Empfohlen)
=======
Minecraft Module structure Plugin

## Features

- Modulares System für Minecraft-Plugins
- API für Modulentwicklung
- Einfache Berechtigungsverwaltung
- Event-basierte Modulkommunikation
- Java 21 Unterstützung
- Paper 1.21.x Kompatibilität

## Installation mit JitPack

Füge EssentialsCore als Abhängigkeit in dein Projekt ein:
>>>>>>> 1dbe3842cb620a5d096f474bd0b2ab86863b873a

Füge JitPack zu deinem `build.gradle` hinzu:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Füge die Abhängigkeit hinzu:

```gradle
dependencies {
<<<<<<< HEAD
    implementation 'com.github.Baumkrieger69:EssentialsCore:Tag'
=======
    implementation 'com.github.Baumkrieger69:EssentialsCore:1.0.2'
>>>>>>> 1dbe3842cb620a5d096f474bd0b2ab86863b873a
}
Module Entwickeln

<<<<<<< HEAD
Ersetze `Tag` mit:
- `main-SNAPSHOT` für die neueste Version
- `v1.0.0` für eine spezifische Version
- `commit-hash` für eine spezifische Commit-Version

### Maven

Füge JitPack zu deiner `pom.xml` hinzu:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Füge die Abhängigkeit hinzu:

```xml
<dependency>
    <groupId>com.github.Baumkrieger69</groupId>
    <artifactId>EssentialsCore</artifactId>
    <version>Tag</version>
</dependency>
```

## Modul-Entwicklung

### Projekt-Setup

```gradle
plugins {
    id 'java'
    id 'maven-publish'
}

group = 'com.your.group'
version = '1.0.0'

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

### Modul-Grundstruktur

```java
import com.essentialscore.api.EssentialsCore;
import com.essentialscore.api.module.Module;
import com.essentialscore.api.module.ModuleInfo;

@ModuleInfo(
    name = "DeinModul",
    version = "1.0.0",
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
version: 1.0.0
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

## Core-Funktionalität

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

## API-Referenz

### Befehle

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

### Events

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
    version = "1.0.0",
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

## Support

Bei Fragen oder Problemen:
- Erstelle ein Issue auf GitHub
- Nutze die Diskussions-Sektion
- Kontaktiere uns über Discord

## Lizenz

Dieses Projekt ist unter der MIT-Lizenz lizenziert. Siehe [LICENSE](LICENSE) für Details.
=======
Grundstruktur
>>>>>>> 1dbe3842cb620a5d096f474bd0b2ab86863b873a
