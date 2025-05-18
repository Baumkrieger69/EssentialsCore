# EssentialsCore - Verbesserte Ressourcenverwaltung

Dieses Dokument erklärt die Funktionsweise des optimierten Ressourcenmanagementsystems von EssentialsCore.

## Überblick

Das neue Ressourcenmanagementsystem bietet folgende Vorteile:

- **Intelligente Erkennung** von Ressourcenpfaden in JAR-Dateien
- **Leistungsoptimierte** Extraktion und Zugriff auf Ressourcen
- **Caching** für häufig genutzte Ressourcen
- **Zuverlässige Fallback-Mechanismen** (direktes Laden aus JAR, wenn Ressource nicht extrahiert)
- **Indizierung** für schnelle Suche nach Ressourcen
- **Konsistente API** für Modul-Entwickler

## Ressourcen in Modulen

Ressourcen sind statische Dateien wie:
- Konfigurationsdateien (YAML, JSON, Properties)
- Textdateien (TXT, MD)
- Webressourcen (HTML, CSS, JS)
- Bilder (PNG, JPG, GIF, SVG)
- Schriftarten (TTF, OTF, WOFF)
- Sprachdateien (JSON, YML)
- Sonstige Dateien (SQL, etc.)

Diese Ressourcen können in verschiedenen Strukturen in der JAR-Datei des Moduls organisiert sein.

## Anpassung der Konfiguration

Die Ressourcenverwaltung kann in der `config.yml` angepasst werden:

```yaml
resources:
  # Puffer-Größe für Ressourcenzugriff
  buffer-size: 8192
  
  # Ressourcen überschreiben, wenn sie bereits existieren
  overwrite-existing: false
  
  # Sicherungen erstellen, wenn Ressourcen überschrieben werden
  create-backups: true
  
  # Cache für Ressourcenzugriff aktivieren
  enable-cache: true
  
  # Cache-Timeout in Minuten
  cache-timeout-minutes: 5
  
  # Häufige Ressourcendatei-Endungen
  common-extensions:
    - yml
    - yaml
    # weitere Endungen...
  
  # Pfadmuster für Ressourcenerkennung
  path-patterns:
    - "resources/.*"
    - "assets/.*"
    # weitere Pfadmuster...
```

## Verwendung im Modul-Code

### Ressourcen einfach extrahieren

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

### Ressourcen auflisten und finden

```java
// Alle Ressourcen in einem Verzeichnis auflisten
List<String> configFiles = core.listResources("MeinModul", "config");

// Ressourcen nach Dateierweiterung finden
List<String> imageFiles = core.findResourcesByExtension("MeinModul", "png");
```

### Ressourcen prüfen

```java
// Prüfen, ob eine Ressource existiert
if (core.resourceExists("MeinModul", "templates/email.html")) {
    // Ressource ist vorhanden
}
```

## Fortgeschrittene Verwendung

### Direkter Zugriff auf den ResourceManager

```java
ModuleResourceManager resourceManager = core.getResourceManager();

// Ressourcenindex für ein Modul erstellen
int count = resourceManager.buildResourceIndex("MeinModul");
```

### Cache-Verwaltung

```java
// Cache für ein Modul leeren
resourceManager.clearModuleCache("MeinModul");

// Gesamten Cache leeren
resourceManager.clearAllCaches();
```

## Empfohlene Ordnerstruktur für Module

Die folgende Ordnerstruktur wird für Module empfohlen:

```
moduleName.jar
  └── src/
      └── main/
          └── resources/
              ├── config/           # Konfigurationsdateien
              ├── lang/             # Sprachdateien
              ├── templates/        # Vorlagen
              ├── assets/           # Bilder, Sounds, etc.
              └── default-data/     # Standarddaten
```

Alternativ:

```
moduleName.jar
  └── resources/
      ├── config/           # Konfigurationsdateien
      ├── lang/             # Sprachdateien
      ├── templates/        # Vorlagen
      ├── assets/           # Bilder, Sounds, etc.
      └── default-data/     # Standarddaten
```

Das System erkennt automatisch, welche Ordnerstruktur verwendet wird und extrahiert die Ressourcen entsprechend.

## Beispiel für ein Modul

```java
public class MeinModul implements ApiCore.ApiModule {
    private ApiCore core;
    private FileConfiguration config;

    @Override
    public void init(ApiCore core, FileConfiguration config) {
        this.core = core;
        this.config = config;
        
        // Ressourcen extrahieren oder überprüfen
        if (!core.extractModuleResourcesIfNeeded("MeinModul")) {
            core.getLogger().warning("Konnte Ressourcen nicht extrahieren!");
        }
        
        // Konfiguration aus Ressource laden
        String defaultConfig = core.getResourceAsString("MeinModul", "config/default.yml");
        if (defaultConfig != null) {
            // Verarbeite Standard-Konfiguration...
        }
        
        // Alle Sprachdateien auflisten
        List<String> langFiles = core.findResourcesByExtension("MeinModul", "yml");
        for (String langFile : langFiles) {
            if (langFile.startsWith("lang/")) {
                // Verarbeite Sprachdatei...
            }
        }
    }
    
    @Override
    public void onDisable() {
        // Aufräumen
    }
}
```

## Fehlerbehandlung

Das System versucht, Ressourcen immer so zuverlässig wie möglich zu laden:

1. Zuerst wird im extrahierten Ressourcenverzeichnis gesucht
2. Falls nicht gefunden, wird versucht, die Ressource direkt aus der JAR zu laden
3. Falls immer noch nicht gefunden, wird ein intelligenter Pfad-Matching-Algorithmus verwendet

Bei Problemen können Sie den Debug-Modus aktivieren:

```yaml
general:
  debug-mode: true
```

Dies gibt ausführliche Informationen über den Ressourcenzugriff in der Konsole aus. 