# Module ohne Core-Abhängigkeit entwickeln

Dieses Dokument erklärt, wie EssentialsCore-Module entwickelt werden können, ohne dass sie direkt vom Core abhängen. Dies verbessert die Modularität und reduziert Probleme bei Core-Updates.

## Vorteile der Unabhängigkeit

- **Stabilität**: Module sind von Änderungen im Core-Code nicht mehr direkt betroffen
- **Einfacheres Testing**: Module können leichter einzeln getestet werden
- **Bessere Kompatibilität**: Reduziert Versionskompatibilitätsprobleme
- **Sauberere Struktur**: Klare Trennung zwischen API und Implementierung

## Die API-Schnittstelle verwenden

Um unabhängige Module zu erstellen, verwendet man ausschließlich die Klassen und Interfaces aus dem `com.essentialscore.api`-Paket:

### 1. Module-Interface implementieren

Statt den Core direkt zu referenzieren, implementiere das `Module`-Interface:

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
    
    // Weitere Methoden implementieren...
}
```

### 2. Vom BaseModule erben

Für einfachere Implementierung kannst du auch von `BaseModule` erben:

```java
import com.essentialscore.api.BaseModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MeinModul extends BaseModule {
    
    public MeinModul() {
        super("MeinModul", "1.0.0", "Ein Beispielmodul ohne Core-Abhängigkeit");
    }
    
    @Override
    protected void onInitialize() {
        // Code nach der Initialisierung
        logInfo("Modul wurde initialisiert");
        
        // Befehl registrieren (mit CommandDefinition aus dem API-Paket)
        api.registerCommands(Collections.singletonList(
            createCommand("meinbefehl", "Beschreibung", "/meinbefehl", "meinmodul.befehl")
        ));
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

### 3. Berechtigungsprüfung nutzen

Um Berechtigungen zu prüfen, verwende die API-Methode:

```java
// Korrekte Methode für Berechtigungsprüfung
if (api.hasPermission(player, "meinmodul.permission")) {
    // Zugriff erlauben
}
```

Die API leitet die Anfrage automatisch an den Core-PermissionManager weiter, der Wildcards und LuckPerms-Integration unterstützt.

### 4. Commands definieren

Verwende die `CommandDefinition`-Schnittstelle oder `SimpleCommand` für Befehle:

```java
// Einfache Befehle mit BaseModule-Hilfsmethode erstellen
CommandDefinition command = createCommand(
    "meinbefehl", 
    "Führt eine Aktion aus", 
    "/meinbefehl [parameter]", 
    "meinmodul.befehl"
);

// Befehlsdefinition registrieren
api.registerCommands(Collections.singletonList(command));
```

## Module.yml konfigurieren

Definiere in der `module.yml` deines Moduls:

```yaml
name: MeinModul
main: com.deinplugin.MeinModul
description: Ein unabhängiges Modul
version: 1.0.0
author: DeinName

# Optional
permissions:
  befehl:
    description: Erlaubt die Nutzung des Hauptbefehls
    default: op
  admin:
    description: Erlaubt administrative Funktionen
    default: op
```

## Gemeinsamen Code teilen

Um Funktionalität zwischen Modulen zu teilen, ohne vom Core abzuhängen:

1. Erstelle ein separates Utility-Modul mit gemeinsamen Methoden
2. Verwende die `getSharedData()` und `setSharedData()`-Methoden der API

```java
// Daten mit anderen Modulen teilen
api.setSharedData("meinmodul.wichtigerWert", meinWert);

// Daten aus anderen Modulen abrufen
Object sharedValue = api.getSharedData("anderesmodul.sharedValue");
```

## Abhängigkeiten zu anderen Modulen

Definiere Abhängigkeiten zu anderen Modulen in der `module.yml`:

```yaml
dependencies:
  - AnderesModul  # Muss geladen sein, bevor dieses Modul geladen wird
  
soft-dependencies:
  - OptionalModul  # Wird geladen, falls verfügbar, aber nicht erforderlich
```

## Ereignisse zwischen Modulen

Verwende das Ereignissystem, um mit anderen Modulen zu kommunizieren:

```java
// Event-Listener registrieren
api.registerModuleListener("anderesmodul.event", (eventName, data) -> {
    // Auf Ereignisse reagieren
    logInfo("Ereignis empfangen: " + eventName);
});

// Eigenes Ereignis auslösen
Map<String, Object> eventData = new HashMap<>();
eventData.put("spieler", player);
eventData.put("wert", 123);
api.fireModuleEvent("meinmodul.aktion", eventData);
```

Durch die Verwendung dieser Methoden können Module vollständig unabhängig vom Core entwickelt werden und kommunizieren ausschließlich über die stabile API-Schnittstelle. 