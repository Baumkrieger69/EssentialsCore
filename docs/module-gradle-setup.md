# Gradle-Konfiguration für unabhängige Module

Diese Anleitung zeigt, wie du Gradle für die Entwicklung von Modulen einrichtest, die nur von der API abhängen, nicht vom gesamten Core.

## build.gradle

Hier ist eine Beispiel-`build.gradle`-Datei für ein unabhängiges Modul:

```groovy
plugins {
    id 'java'
    id 'maven-publish'
}

group = 'com.deinplugin'
version = '1.0.0'
description = 'Mein unabhängiges EssentialsCore-Modul'

repositories {
    mavenCentral()
    // SpigotMC Repository für Bukkit/Spigot-APIs
    maven { url = 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/' }
    
    // GitHub Packages Repository für EssentialsCore-API
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Baumkrieger69/EssentialsCore")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Verwende die API aus GitHub Packages
    compileOnly 'com.essentialscore:essentialscore-api:1.0.0'
    
    // Spigot/Bukkit API
    compileOnly 'org.spigotmc:spigot-api:1.19.2-R0.1-SNAPSHOT'
    
    // Weitere Abhängigkeiten nach Bedarf
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.1'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.1'
}

// Java 8 oder höher verwenden
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Stellt sicher, dass die Ressourcen korrekt verarbeitet werden
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
    
    // Stelle sicher, dass module.yml im Hauptverzeichnis liegt
    filesMatching('module.yml') {
        path = 'module.yml'
    }
    
    // Weiterer Konfiguration für Abhängigkeiten, falls notwendig
    manifest {
        attributes(
            'Built-By': System.properties['user.name'],
            'Created-By': "Gradle ${gradle.gradleVersion}",
            'Built-Date': new Date().format("yyyy-MM-dd HH:mm:ss"),
            'Module-Version': project.version
        )
    }
}

// Anpassen des Standard-JAR-Tasks
jar {
    // Standard-JAR ist identisch zum Modul-JAR
    finalizedBy buildModule
}

// Konfiguration für die Veröffentlichung als GitHub Package
publishing {
    publications {
        maven(MavenPublication) {
            groupId = project.group
            artifactId = project.name.toLowerCase()
            version = project.version
            
            from components.java
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Baumkrieger69/EssentialsCore")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

test {
    useJUnitPlatform()
}
```

## GitHub Packages Authentifizierung einrichten

Um GitHub Packages zu verwenden, musst du dich authentifizieren. Dafür gibt es zwei Möglichkeiten:

### 1. Über Umgebungsvariablen

Setze diese Umgebungsvariablen in deinem System:
```
GITHUB_USERNAME=dein-github-benutzername
GITHUB_TOKEN=dein-persönliches-access-token
```

### 2. Über gradle.properties

Erstelle eine Datei `~/.gradle/gradle.properties` mit folgendem Inhalt:
```properties
gpr.user=dein-github-benutzername
gpr.key=dein-persönliches-access-token
```

Du benötigst ein Personal Access Token mit den Berechtigungen `read:packages` und `write:packages`. Du kannst ein Token erstellen unter [GitHub Settings > Developer settings > Personal access tokens](https://github.com/settings/tokens).

## Modul als GitHub Package veröffentlichen

Um dein Modul als GitHub Package zu veröffentlichen, führe diesen Befehl aus:

```bash
./gradlew publish
```

Andere Entwickler können dann dein Modul in ihren Projekten nutzen, indem sie die entsprechenden Repository- und Dependency-Einträge in ihrer `build.gradle` hinzufügen.

## settings.gradle

```groovy
rootProject.name = 'MeinModul'
```

## module.yml Vorlage

Erstelle eine Datei unter `src/main/resources/module.yml`:

```yaml
name: ${name}
version: ${version}
description: ${description}
main: com.deinplugin.MeinModul
author: DeinName

# Abhängigkeiten
dependencies: []
soft-dependencies: []

# Berechtigungen
permissions:
  meinmodul.befehl:
    description: Erlaubt die Nutzung des Hauptbefehls
    default: op
```

## Verzeichnisstruktur

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
│   │   │           └── commands/
│   │   │               └── MeinBefehl.java
│   │   └── resources/
│   │       └── module.yml
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

## Installation in den Entwicklungsserver

Um dein Modul einfach in einem Entwicklungsserver zu testen, füge diese Aufgabe zu deiner `build.gradle` hinzu:

```groovy
task copyToServer(type: Copy, dependsOn: buildModule) {
    from buildModule.archiveFile
    into "C:/Server/plugins/EssentialsCore/modules"
    // Passe den Pfad an deinen Entwicklungsserver an
}

build.finalizedBy copyToServer
```

## Abhängigkeit auf lokalen Core

Wenn du während der Entwicklung gegen eine lokale Version des Cores testen möchtest:

```groovy
repositories {
    mavenLocal() // Lokales Maven-Repository zuerst durchsuchen
    // Andere Repositories...
}

dependencies {
    // Lokale Version der API verwenden
    compileOnly 'com.essentialscore:essentialscore-api:1.0.0'
    
    // Alternative: Direkter Dateipfad zur API-JAR
    // compileOnly files('lib/essentialscore-api.jar')
}
```

## Best Practices

1. **Isoliere Abhängigkeiten**: Verwende nur die API-Abhängigkeit, nicht den gesamten Core
2. **Versions-Kompatibilität**: Dokumentiere, mit welchen Core-Versionen dein Modul getestet wurde
3. **Einbetten von Abhängigkeiten**: Wenn dein Modul externe Bibliotheken benötigt, verwende den `shadow`-Gradle-Plugin:

```groovy
plugins {
    id 'com.github.johnrengelman.shadow' version '7.1.2'
}

// Umbenenne Pakete, um Konflikte zu vermeiden
shadowJar {
    relocate 'com.externe.bibliothek', 'com.deinplugin.shadow.bibliothek'
}

// Verwende shadowJar statt buildModule
task buildModule(type: Copy, dependsOn: shadowJar) {
    from shadowJar.archiveFile
    into "${buildDir}/libs"
    rename "${shadowJar.archiveFileName.get()}", "${project.name}-${project.version}.jar"
}
```

## Verwendung von GitHub Packages in anderen Projekten

Um dein veröffentlichtes Modul in anderen Projekten zu verwenden:

```groovy
repositories {
    // Andere Repositories...
    
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Baumkrieger69/EssentialsCore")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Ersetze 'MeinModul' mit dem tatsächlichen artifactId und 'com.deinplugin' mit der groupId
    implementation 'com.deinplugin:meinmodul:1.0.0'
}
```

Mit dieser Konfiguration kannst du Module entwickeln, die nur von der stabilen API abhängen, nicht vom gesamten EssentialsCore, und sie als GitHub Packages veröffentlichen und verwenden. 