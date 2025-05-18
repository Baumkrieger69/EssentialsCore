# EssentialsCore
Minecraft Module strukture Plugin ( IN DEVELOPMENT (MANY BUGS))

## Funktionen

- Modulares System für Minecraft-Plugins
- API für Modulentwicklung
- Einfache Berechtigungsverwaltung
- System für Modulübergreifende Kommunikation

## Verwendung mit JitPack

Du kannst dieses Projekt als Abhängigkeit in deinen Projekten mit JitPack verwenden:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // Nur API für Module (empfohlen)
    compileOnly 'com.github.EssentialsCore:EssentialsCore-API:1.0.0'
    
    // Oder das vollständige Plugin
    // compileOnly 'com.github.EssentialsCore:EssentialsCore:1.0.0'
}
```

## Eigene Module entwickeln

Alle Informationen zur Entwicklung von Modulen findest du in unserer [umfassenden Modul-Entwicklungsanleitung](docs/comprehensive-module-guide.md).

Diese Anleitung enthält:
- Projekteinrichtung mit Gradle
- Modulstruktur und API-Nutzung
- Berechtigungssystem und Befehle
- Veröffentlichung eigener Module
- Beispiele und Best Practices
