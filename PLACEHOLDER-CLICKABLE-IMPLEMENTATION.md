# EssentialsCore - Placeholder & Clickable Commands Implementation

## Übersicht

Die EssentialsCore-Plugin wurde erfolgreich mit einem flexiblen Placeholder-System und einem Clickable-Command-System erweitert.

## Implementierte Features

### 1. Placeholder-System (`PlaceholderManager`)

**Unterstützte Bracket-Typen:**
- `{placeholder}` - Geschweifte Klammern
- `(placeholder)` - Runde Klammern  
- `[placeholder]` - Eckige Klammern
- `%placeholder%` - Prozentzeichen

**Verfügbare Platzhalter:**
- `{player}` - Spielername
- `{displayname}` - Anzeigename des Spielers
- `{world}` - Aktuelle Welt
- `{x}`, `{y}`, `{z}` - Koordinaten
- `{health}` - Gesundheit
- `{food}` - Hunger
- `{level}` - Erfahrungslevel
- `{gamemode}` - Spielmodus
- `{online}` - Online-Spieler-Anzahl
- `{max}` - Maximale Spieler-Anzahl
- `{tps}` - Server TPS
- `{memory}` - Speicherverbrauch
- `{uptime}` - Server-Laufzeit
- `{version}` - Plugin-Version
- `{plugin}` - Plugin-Name
- `{modules}` - Anzahl Module
- `{time}` - Aktuelle Zeit
- `{date}` - Aktuelles Datum

**Konfiguration in `config.yml`:**
```yaml
placeholders:
  enabled: true
  brackets:
    curly: true      # {placeholder}
    round: true      # (placeholder)
    square: true     # [placeholder]
    percent: true    # %placeholder%
  data:
    server-performance: true
    player-info: true
    world-info: true
    plugin-info: true
  formatting:
    numbers:
      decimal-places: 2
      thousands-separator: true
    time:
      format: "HH:mm:ss"
    date:
      format: "dd.MM.yyyy"
```

### 2. Clickable-Command-System (`ClickableCommandManager`)

**Unterstützte Bracket-Typen:**
- `{/command}` - Geschweifte Klammern
- `(/command)` - Runde Klammern
- `[/command]` - Eckige Klammern
- `%/command%` - Prozentzeichen (optional)

**Features:**
- Befehle werden im Chat klickbar dargestellt
- Bestätigungsmechanismus für gefährliche Befehle
- Hover-Text zeigt Befehlsinformationen
- Konfigurierbare Sicherheitseinstellungen

**Konfiguration in `config.yml`:**
```yaml
clickable-commands:
  enabled: true
  brackets:
    curly: true      # {/command}
    round: true      # (/command)
    square: true     # [/command]
    percent: false   # %/command%
  security:
    require-confirmation: true
    dangerous-commands:
      - "stop"
      - "restart"
      - "ban"
      - "kick"
      - "op"
      - "deop"
    confirmation-timeout: 30
  visual:
    hover-text: "§7Klicken zum Ausführen: §b%command%"
    confirmation-text: "§7Bestätigung erforderlich! Nutze: §a/confirm %code%"
    color-scheme: "§b"
```

### 3. Chat-Integration (`ChatMessageListener`)

- Automatische Verarbeitung von Chat-Nachrichten
- Ersetzt Platzhalter in Echtzeit
- Konvertiert Befehle zu klickbaren Elementen
- Funktioniert transparent im Hintergrund

### 4. Neue Befehle

**`/confirm <code>`**
- Bestätigt gefährliche Clickable Commands
- Wird automatisch bei riskanten Befehlen angezeigt
- Zeitbasierte Codes für Sicherheit

## Verwendung

### Beispiele für Platzhalter:

**Chat-Nachricht:**
```
"Hallo {player}! Du bist in Welt {world} bei {x},{y},{z}. Server TPS: {tps}"
```

**Wird zu:**
```
"Hallo Spielername! Du bist in Welt world_nether bei 123,64,-456. Server TPS: 19.8"
```

### Beispiele für Clickable Commands:

**Chat-Nachricht:**
```
"Nutze {/spawn} um zum Spawn zu teleportieren oder {/home} für dein Zuhause!"
```

**Wird zu klickbaren Links mit:**
- Hover-Text: "Klicken zum Ausführen: /spawn"
- Bei Klick: Befehl wird ausgeführt
- Bei gefährlichen Befehlen: Bestätigung erforderlich

## Technische Details

### Klassen-Struktur:
- `PlaceholderManager` - Hauptverwaltung der Platzhalter
- `PlaceholderProvider` - Interface für Platzhalter-Provider
- `ClickableCommandManager` - Verwaltung klickbarer Befehle
- `ConfirmCommandExecutor` - Bestätigungsbefehl-Handler
- `ChatMessageListener` - Chat-Event-Verarbeitung

### Integration in ApiCore:
- Manager werden in `onEnable()` initialisiert
- Chat-Listener wird automatisch registriert
- Getter-Methoden für andere Module verfügbar
- Vollständig konfigurierbar über `config.yml`

## Kompatibilität

- Minecraft 1.16+ (durch api-version)
- Bukkit/Spigot/Paper
- Rückwärtskompatibel mit bestehenden Plugins
- Keine externen Abhängigkeiten erforderlich

## Erweiterbarkeitsbereitung

Das System ist vollständig erweiterbar:
- Neue Platzhalter können einfach hinzugefügt werden
- Custom PlaceholderProvider können registriert werden
- Bracket-Typen sind konfigurierbar
- Clickable-Command-Verhalten ist anpassbar
