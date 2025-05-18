# GitHub Packages für EssentialsCore

Diese Anleitung erklärt, wie du EssentialsCore als GitHub Package veröffentlichst und als Abhängigkeit in anderen Projekten verwendest.

## Veröffentlichung als GitHub Package

### Voraussetzungen

1. GitHub-Konto
2. Ein Personal Access Token mit den Berechtigungen `read:packages` und `write:packages`
   - Gehe zu [GitHub Settings > Developer settings > Personal access tokens](https://github.com/settings/tokens)
   - Erstelle einen neuen Token mit den entsprechenden Berechtigungen

### Konfiguration

1. Kopiere die `gradle.properties.example` Datei zu `gradle.properties`
2. Aktualisiere die Datei mit deinen GitHub-Anmeldeinformationen:
   ```properties
   gpr.user=DEIN_GITHUB_BENUTZERNAME
   gpr.key=DEIN_GITHUB_TOKEN
   ```

### Veröffentlichung

Um das Paket zu veröffentlichen, führe den folgenden Befehl aus:

```bash
./gradlew publish
```

Dies veröffentlicht zwei Pakete:
- `com.EssentialsCore:essentials-core`: Das vollständige Plugin
- `com.EssentialsCore:essentials-core-api`: Nur die API-Klassen für Module

## Verwendung in anderen Projekten

Um EssentialsCore in deinem Projekt zu verwenden, musst du das GitHub Packages Repository zu deiner `build.gradle`-Datei hinzufügen:

```groovy
repositories {
    mavenCentral()
    // Andere Repositories
    
    maven {
        name = "GitHubPackages"
        url = "https://maven.pkg.github.com/Baumkrieger69/EssentialsCore"
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Verwende nur die API für Module
    compileOnly 'com.EssentialsCore:essentials-core-api:1.0.0'
    
    // ODER verwende das vollständige Plugin (nicht empfohlen für Module)
    // compileOnly 'com.EssentialsCore:essentials-core:1.0.0'
}
```

Denke daran, dass du auch in diesem Projekt eine `gradle.properties`-Datei mit den GitHub-Anmeldeinformationen benötigst.

## Verwendung mit JitPack (Alternative)

Wenn du keine GitHub-Anmeldeinformationen teilen möchtest, kannst du auch JitPack verwenden:

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Baumkrieger69:EssentialsCore:1.0.0'
}
```

## GitHub Actions für automatische Veröffentlichung

Du kannst eine GitHub Action einrichten, um automatisch bei jedem Release zu veröffentlichen:

```yaml
name: Publish to GitHub Packages

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Build with Gradle
        uses: gradle/gradle-build-action@v2
        with:
          arguments: build
      
      - name: Publish to GitHub Packages
        uses: gradle/gradle-build-action@v2
        with:
          arguments: publish
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

Dadurch wird das Paket automatisch veröffentlicht, wenn du einen neuen Release auf GitHub erstellst. 