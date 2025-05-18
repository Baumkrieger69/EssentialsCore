# EssentialsCore - Erweiterte Konsolenformatierung

Dieses Dokument erklärt die erweiterte Konsolenformatierung von EssentialsCore, die für eine übersichtlichere und visuell ansprechendere Ausgabe sorgt.

## Überblick

Die erweiterte Konsolenformatierung bietet folgende Vorteile:

- **Farbcodierte Nachrichten** für verschiedene Nachrichtentypen (Info, Erfolg, Warnung, Fehler, etc.)
- **Visuelle Hierarchie** durch formatierte Überschriften, Abschnitte und Trennlinien
- **Fortschrittsanzeigen** für langwierige Operationen
- **Unicode-Symbole** für bessere Lesbarkeit
- **Flexible Anpassung** über die Konfigurationsdatei
- **Zeitstempel** für präzise Nachrichtenprotokollierung
- **Tabellarische Darstellung** für strukturierte Daten
- **Textboxen** für besonders wichtige Informationen

## Konfiguration

Die Konsolenformatierung kann in der `config.yml` angepasst werden:

```yaml
console:
  # Farbige Konsolenausgabe aktivieren
  use-colors: true
  
  # Zeitstempel in Konsolenausgaben anzeigen
  show-timestamps: false
  
  # Unicode-Symbole für Nachrichtentypen verwenden
  use-unicode-symbols: true
  
  # Stil-Preset für die Konsolenausgabe (default, minimal, fancy)
  style-preset: "default"
  
  # Animation beim Start anzeigen
  show-startup-animation: true
  
  # Detaillierungsgrad der Konsolenausgabe (verbose, normal, minimal)
  verbosity: "normal"
  
  # Farbschema (classic, modern, dark, light)
  color-scheme: "modern"
  
  # Komponenten-Prefixe
  prefixes:
    core: "&8[&b&lApiCore&8]"
    module-manager: "&8[&d&lModuleManager&8]"
    file-manager: "&8[&6&lFileManager&8]"
    thread-manager: "&8[&a&lThreadManager&8]"
    resource-manager: "&8[&3&lResourceManager&8]"
    permission-manager: "&8[&5&lPermissionManager&8]"
    command-manager: "&8[&e&lCommandManager&8]"
    config-manager: "&8[&9&lConfigManager&8]"
    performance-monitor: "&8[&2&lPerformance&8]"
```

## Nachrichtentypen

Die Konsolenformatierung unterstützt verschiedene Nachrichtentypen, die jeweils unterschiedlich formatiert sind:

- **Info**: Standard-Informationen (cyan)
- **Success**: Erfolgsmeldungen (grün)
- **Warning**: Warnungen (gelb)
- **Error**: Fehlermeldungen (rot)
- **Debug**: Debug-Informationen (nur im Debug-Modus, lila)
- **Important**: Wichtige Hinweise (hervorgehoben mit gelbem Hintergrund)

## Strukturelemente

Für eine bessere visuelle Strukturierung stehen folgende Elemente zur Verfügung:

- **Header**: Große Überschriften mit Rahmen
- **SubHeader**: Kleinere Überschriften ohne Rahmen
- **Section**: Abschnittsüberschriften
- **Line**: Horizontale Trennlinien
- **DoubleLine**: Doppelte horizontale Trennlinien
- **ListItem**: Eingerückte Listeneinträge mit Punkt
- **DataSection**: Gruppierte Key-Value-Paare mit Überschrift
- **Blank**: Leere Zeilen für bessere Übersicht

## Fortschrittsanzeigen

Für langwierige Operationen stehen verschiedene Fortschrittsanzeigen zur Verfügung:

- **ProgressBar**: Standard-Fortschrittsbalken
- **ColorProgressBar**: Farbiger Fortschrittsbalken, der die Farbe je nach Fortschritt ändert
- **Step**: Nummerierte Schritte
- **RichStep**: Erweiterte nummerierte Schritte mit Status-Icon

## Tabellen und Boxen

Für strukturierte Daten und hervorgehobene Informationen:

- **TableHeader/TableRow**: Formatierte Tabellen mit Spaltenüberschriften
- **Box**: Text in einer Box für besondere Hervorhebung
- **TextBlock**: Mehrere Zeilen Text mit Überschrift

## Beispiel-Ausgabe

Die erweiterte Konsolenformatierung erzeugt eine Ausgabe ähnlich wie folgt:

```
╔════════════════════════════════════╗
║   ESSENTIALS CORE 1.0.0            ║
╚════════════════════════════════════╝
  Optimiert für Performance & Modularität

► SYSTEM-INFORMATION
  • Version: 1.0.0
  • Java: 17.0.2
  • OS: Windows 10 x64
  • Bukkit: 1.19.3
  • Speicher: 2048MB

◈ Initialisierung ──────────────────────────
ℹ Starte Thread-Manager...
✓ Thread-Pool erfolgreich erstellt mit 4 Threads
ℹ Initialisiere Komponenten...
✓ Alle Kernkomponenten initialisiert

◈ MODULE LADEN ────────────────────────────
[███████████████░░░░░░░░░░░] 69.5% (9/13)

✓ EssentialsCore wurde erfolgreich gestartet!
```

## Verwendung für Plugin-Entwickler

Plugin-Entwickler können die erweiterte Konsolenformatierung über die APICore-Klasse verwenden:

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

// Fortschrittsanzeigen
console.progressBar(current, max, length);
console.colorProgressBar(current, max, length, "Label");
console.step(current, max, "Beschreibung");
console.richStep(current, max, "Beschreibung", isSuccess);

// Tabellen und Boxen
console.tableHeader("Spalte 1", "Spalte 2", "Spalte 3");
console.tableRow("Wert 1", "Wert 2", "Wert 3");
console.box("Wichtige Information", 50);
console.textBlock("Titel", "Mehrzeiliger Text...", isImportant);
``` 