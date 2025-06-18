package com.essentialscore.util;

import com.essentialscore.ApiCore;
import com.essentialscore.ConsoleFormatter;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Optimierter ModuleResourceManager zur effizienten Verwaltung von Modulressourcen
 * Diese Klasse bietet erweiterte Funktionen für die Extraktion, Zwischenspeicherung und 
 * Verwaltung von Modulressourcen mit optimierter Performance.
 */
public class ModuleResourceManager {
    private final ApiCore core;
    private final ConsoleFormatter console;
    private final int bufferSize;
    
    // Cache-System für schnellen Ressourcenzugriff
    private final Map<String, Map<String, ResourceInfo>> resourceCache = new ConcurrentHashMap<>();
    
    // Extraktions-Konfiguration
    private final List<String> commonResourceExtensions;
    private final List<Pattern> resourcePathPatterns;
    private boolean overwriteExisting = false;
    private boolean backupOnOverwrite = true;
    private boolean verifyIntegrity = false;
    private boolean cacheEnabled = true;
    private long cacheTimeoutMinutes = 5;
    private List<String> defaultFileTypes = Arrays.asList(".yml", ".yaml", ".json", ".properties", ".txt", ".cfg");
    private List<String> extractSubdirectories = Arrays.asList("assets", "data", "translations");
    private List<String> ignorePatterns = Arrays.asList("*.temp", "draft_*");
    
    // Indizierung von Ressourcen für schnellen Zugriff
    private final Map<String, ResourceIndex> moduleResourceIndices = new ConcurrentHashMap<>();
    
    /**
     * Informationen über eine Ressourcendatei
     */
    private static class ResourceInfo {
        private final File file;
        private final long lastModified;
        private final long size;
        private byte[] cachedData;
        private long cacheTimestamp;
        
        public ResourceInfo(File file) {
            this.file = file;
            this.lastModified = file.lastModified();
            this.size = file.length();
        }
        
        public boolean isValid() {
            return file.exists() && file.lastModified() == lastModified && file.length() == size;
        }
        
        public void cacheData(byte[] data) {
            this.cachedData = data;
            this.cacheTimestamp = System.currentTimeMillis();
        }
        
        public byte[] getCachedData() {
            return cachedData;
        }
        
        public boolean isCacheValid(long timeoutMinutes) {
            return cachedData != null && 
                   System.currentTimeMillis() - cacheTimestamp < TimeUnit.MINUTES.toMillis(timeoutMinutes);
        }
        
        // The method is actually used when accessing the file outside the class
        // or might be used in future extensions
        @SuppressWarnings("unused")
        public File getFile() {
            return file;
        }
    }
    
    /**
     * Index für alle Ressourcen eines Moduls
     */
    private static class ResourceIndex {
        private final Map<String, Set<String>> extensionMap = new HashMap<>();
        private final Map<String, Set<String>> pathMap = new HashMap<>();
        private final Set<String> allResources = new HashSet<>();
        
        public void addResource(String path) {
            allResources.add(path);
            
            // Nach Dateiendung indizieren
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0) {
                String extension = path.substring(lastDot + 1).toLowerCase();
                extensionMap.computeIfAbsent(extension, k -> new HashSet<>()).add(path);
            }
            
            // Nach Verzeichnis indizieren
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                String directory = path.substring(0, lastSlash);
                pathMap.computeIfAbsent(directory, k -> new HashSet<>()).add(path);
            }
        }
        
        public Set<String> findByExtension(String extension) {
            return extensionMap.getOrDefault(extension.toLowerCase(), Collections.emptySet());
        }
        
        public Set<String> findInDirectory(String directory) {
            return pathMap.getOrDefault(directory, Collections.emptySet());
        }
        
        public Set<String> getAll() {
            return Collections.unmodifiableSet(allResources);
        }
        
        public boolean hasResource(String path) {
            return allResources.contains(path);
        }
    }
    
    /**
     * Konstruktor für den ModuleResourceManager
     * 
     * @param core Die ApiCore-Instanz
     */
    public ModuleResourceManager(ApiCore core) {
        this.core = core;
        
        // Erweiterte Konsolen-Formatter Konfiguration
        boolean useColors = core.getConfig().getBoolean("console.use-colors", true);
        boolean showTimestamps = core.getConfig().getBoolean("console.show-timestamps", false);
        boolean useUnicodeSymbols = core.getConfig().getBoolean("console.use-unicode-symbols", true);
        String stylePreset = core.getConfig().getString("console.style-preset", "default");
        
        this.console = new ConsoleFormatter(
            core.getLogger(),
            core.getConfig().getString("console.prefixes.resource-manager", "&8[&3&lResourceManager&8]"),
            useColors, showTimestamps, useUnicodeSymbols, stylePreset
        );
        
        // Konfiguration laden
        this.bufferSize = core.getConfig().getInt("resources.buffer-size", 8192);
        this.overwriteExisting = core.getConfig().getBoolean("resources.overwrite-existing", false);
        this.backupOnOverwrite = core.getConfig().getBoolean("resources.backup-on-overwrite", true);
        this.verifyIntegrity = core.getConfig().getBoolean("resources.verify-integrity", false);
        this.cacheEnabled = core.getConfig().getBoolean("resources.enable-cache", true);
        this.cacheTimeoutMinutes = core.getConfig().getLong("resources.cache-timeout-minutes", 5);
        
        // Standard-Ressourcenendungen
        List<String> configExtensions = core.getConfig().getStringList("resources.common-extensions");
        if (configExtensions.isEmpty()) {
            configExtensions = Arrays.asList(
                "yml", "yaml", "json", "properties", "txt", "md", "sql", 
                "html", "css", "js", "png", "jpg", "jpeg", "gif", "svg", 
                "ttf", "otf", "woff", "woff2", "eot"
            );
        }
        this.commonResourceExtensions = configExtensions;
        
        // Ressourcenpfadmuster
        List<String> pathPatterns = core.getConfig().getStringList("resources.path-patterns");
        if (pathPatterns.isEmpty()) {
            pathPatterns = Arrays.asList(
                "resources/.*", 
                "assets/.*", 
                "data/.*", 
                "templates/.*",
                "lang/.*",
                "i18n/.*",
                "locales/.*",
                "config/.*"
            );
        }
        
        this.resourcePathPatterns = pathPatterns.stream()
            .map(Pattern::compile)
            .collect(Collectors.toList());
        
        console.info("ModuleResourceManager initialisiert mit " + 
                    configExtensions.size() + " Dateiendungen und " + 
                    pathPatterns.size() + " Pfadmustern");
    }
    
    /**
     * Extrahiert alle Ressourcen für ein Modul mit intelligenter Pfaderkennung
     * 
     * @param moduleName Der Name des Moduls
     * @return true, wenn die Extraktion erfolgreich war
     */
    public boolean extractModuleResources(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            console.error("Modulname darf nicht leer sein!");
            return false;
        }
        
        // Hole die Modul-JAR
        com.essentialscore.api.module.ModuleManager.ModuleInfo moduleInfo = core.getModuleInfo(moduleName);
        if (moduleInfo == null) {
            console.error("Modul nicht gefunden: " + moduleName);
            return false;
        }
        
        File jarFile = moduleInfo.getJarFile();
        if (jarFile == null || !jarFile.exists()) {
            console.error("JAR-Datei nicht gefunden für Modul: " + moduleName);
            return false;
        }
        
        // Hole oder erstelle Ressourcenverzeichnis
        File resourcesDir = getModuleResourceDirectory(moduleName);
        if (resourcesDir == null) {
            console.error("Konnte Ressourcenverzeichnis nicht erstellen für: " + moduleName);
            return false;
        }
        
        console.subHeader("RESSOURCEN-EXTRAKTION: " + moduleName);
        console.info("JAR-Datei: " + jarFile.getName());
        console.info("Ziel-Verzeichnis: " + resourcesDir.getAbsolutePath());
        console.blank();
        
        try {
            // Zähler für die Statistik
            int extractedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;
            
            // Neuen Ressourcenindex erstellen
            ResourceIndex resourceIndex = new ResourceIndex();
            
            // JAR-Struktur analysieren
            console.section("PHASE 1: JAR-ANALYSE");
            List<String> resourceBasePaths = detectResourceBasePaths(jarFile);
            
            // Formatierte Anzeige der gefundenen Basispfade
            console.info("Gefundene Ressourcenpfade:");
            for (int i = 0; i < resourceBasePaths.size(); i++) {
                String path = resourceBasePaths.get(i);
                console.listItem((i+1) + ".", path.isEmpty() ? "(Wurzelverzeichnis)" : path);
            }
            
            // JAR öffnen und Ressourcen extrahieren
            try (JarFile jar = new JarFile(jarFile)) {
                // Phase 1: Extraktionsplan erstellen
                console.section("PHASE 2: EXTRAKTION");
                console.info("Erstelle Extraktionsplan...");
                Map<JarEntry, File> extractionPlan = new HashMap<>();
                
                Enumeration<JarEntry> entries = jar.entries();
                // Total-Zähler für Fortschrittsbalken
                int totalEntries = 0;
                while (entries.hasMoreElements()) {
                    entries.nextElement();
                    totalEntries++;
                }
                
                // Reset Enumeration für die eigentliche Verarbeitung
                entries = jar.entries();
                int processedEntries = 0;
                int planSize = 0;
                
                console.progressBar(0, totalEntries, 40);
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    processedEntries++;
                    
                    // Fortschrittsanzeige aktualisieren (nur jedes 20. Element zur Performanceoptimierung)
                    if (processedEntries % 20 == 0 || processedEntries == totalEntries) {
                        console.progressBar(processedEntries, totalEntries, 40);
                    }
                    
                    // Verzeichnisse und Klassendateien überspringen
                    if (entry.isDirectory() || entryName.isEmpty() || entryName.endsWith(".class")) {
                        continue;
                    }
                    
                    // Bestimme, ob es sich um eine Ressourcendatei handelt
                    if (!isResourceFile(entryName)) {
                        continue;
                    }
                    
                    // Bestimme den relativen Pfad basierend auf den erkannten Ressourcenpfaden
                    String relativePath = null;
                    for (String basePath : resourceBasePaths) {
                        if (entryName.startsWith(basePath)) {
                            relativePath = entryName.substring(basePath.length());
                            if (relativePath.startsWith("/")) {
                                relativePath = relativePath.substring(1);
                            }
                            break;
                        }
                    }
                    
                    // Wenn kein Basispfad passt, aber die Datei eine Ressourcenendung hat oder einem Muster entspricht
                    if (relativePath == null && 
                        (hasResourceExtension(entryName) || matchesResourcePattern(entryName))) {
                        // Extrahiere den Dateinamen
                        int lastSlash = entryName.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            relativePath = entryName.substring(lastSlash + 1);
                        } else {
                            relativePath = entryName;
                        }
                    }
                    
                    // Wenn ein gültiger Pfad gefunden wurde, Datei extrahieren
                    if (relativePath != null && !relativePath.isEmpty()) {
                        File targetFile = new File(resourcesDir, relativePath);
                        extractionPlan.put(entry, targetFile);
                        planSize++;
                    }
                }
                
                console.success("Analyse abgeschlossen: " + planSize + " extrahierbare Ressourcen gefunden");
                
                // Phase 2: Extraktionsplan ausführen
                console.section("PHASE 3: EXTRAKTION DURCHFÜHREN");
                console.info("Extrahiere " + planSize + " Ressourcen...");
                
                int currentStep = 0;
                console.colorProgressBar(0, planSize, 40, "0% abgeschlossen");
                
                for (Map.Entry<JarEntry, File> extract : extractionPlan.entrySet()) {
                    JarEntry entry = extract.getKey();
                    File targetFile = extract.getValue();
                    String entryName = entry.getName();
                    currentStep++;
                    
                    // Fortschrittsanzeige aktualisieren
                    if (currentStep % 5 == 0 || currentStep == planSize) {
                        int percent = (int)((currentStep / (double)planSize) * 100);
                        console.colorProgressBar(currentStep, planSize, 40, percent + "% abgeschlossen");
                    }
                    
                    // Status für wichtige Dateitypen
                    boolean isImportantFile = entryName.endsWith(".yml") || entryName.endsWith(".json") || 
                                            entryName.endsWith(".properties") || entryName.endsWith(".sql");
                    
                    // Überprüfen, ob die Datei bereits existiert und ob sie überschrieben werden soll
                    if (targetFile.exists()) {
                        if (!overwriteExisting) {
                            skippedCount++;
                            
                            // Zeige Fortschritt für wichtige überspungene Dateien
                            if (isImportantFile && core.isDebugMode()) {
                                console.richStep(currentStep, planSize, "Übersprungen: " + getRelativePath(resourcesDir, targetFile), false);
                            }
                            
                            // Trotzdem zum Index hinzufügen
                            String relativePath = getRelativePath(resourcesDir, targetFile);
                            resourceIndex.addResource(relativePath);
                            continue;
                        }
                        
                        // Backup erstellen, falls konfiguriert
                        if (backupOnOverwrite) {
                            if (!backupFile(targetFile)) {
                                errorCount++;
                                continue;
                            }
                        }
                    }
                    
                    // Stelle sicher, dass das Elternverzeichnis existiert
                    File parent = targetFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        console.error("Konnte Verzeichnis nicht erstellen: " + parent.getAbsolutePath());
                        errorCount++;
                        continue;
                    }
                    
                    // Extrahiere die Datei
                    try (InputStream in = jar.getInputStream(entry);
                         ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
                        
                        byte[] buffer = new byte[bufferSize];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            byteStream.write(buffer, 0, bytesRead);
                        }
                        
                        byte[] data = byteStream.toByteArray();
                        
                        // Schreibe die Daten in die Datei
                        try (FileOutputStream out = new FileOutputStream(targetFile)) {
                            out.write(data);
                        }
                        
                        // Überprüfe die Integrität wenn aktiviert
                        if (verifyIntegrity) {
                            boolean integrityValid = verifyFileIntegrity(data, targetFile);
                            if (!integrityValid) {
                                console.warning("Integritätsprüfung fehlgeschlagen für: " + targetFile.getName());
                                errorCount++;
                                continue;
                            }
                        }
                        
                        // Zum Index hinzufügen
                        String relativePath = getRelativePath(resourcesDir, targetFile);
                        resourceIndex.addResource(relativePath);
                        
                        // Nur wichtige Dateien explizit loggen um die Konsole nicht zu überfüllen
                        if (isImportantFile && core.isDebugMode()) {
                            console.richStep(currentStep, planSize, "Extrahiert: " + relativePath, true);
                        }
                        
                        extractedCount++;
                    } catch (IOException e) {
                        console.richStep(currentStep, planSize, "Fehler: " + entryName, false);
                        errorCount++;
                    }
                }
                
                // Index speichern
                moduleResourceIndices.put(moduleName, resourceIndex);
                
                // Abschlussstatistik mit Box anzeigen
                console.blank();
                console.success("Extraktion abgeschlossen!");
                String statsText = 
                    "Extrahiert: " + extractedCount + "\n" +
                    "Übersprungen: " + skippedCount + "\n" + 
                    "Fehler: " + errorCount + "\n" +
                    "Indiziert: " + resourceIndex.getAll().size();
                
                // Unterschiedliche Boxformatierung basierend auf Erfolg
                if (errorCount > 0) {
                    console.textBlock("EXTRAKTION MIT FEHLERN", statsText, false);
                } else {
                    console.textBlock("EXTRAKTION ERFOLGREICH", statsText, true);
                }
                
                return true;
            }
        } catch (Exception e) {
            console.error("Fehler beim Extrahieren der Ressourcen für " + moduleName + ": " + e.getMessage());
            if (core.isDebugMode()) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Liest eine Ressourcendatei als Byte-Array
     * 
     * @param moduleName Der Name des Moduls
     * @param resourcePath Der Pfad zur Ressource
     * @return Die Ressource als Byte-Array oder null, wenn sie nicht gefunden wurde
     */
    public byte[] getResourceAsBytes(String moduleName, String resourcePath) {
        if (moduleName == null || resourcePath == null) {
            return null;
        }
        
        // Pfad normalisieren
        resourcePath = normalizeResourcePath(resourcePath);
        
        // Prüfe Cache zuerst
        if (cacheEnabled) {
            ResourceInfo cachedInfo = getCachedResourceInfo(moduleName, resourcePath);
            if (cachedInfo != null && cachedInfo.isCacheValid(cacheTimeoutMinutes)) {
                return cachedInfo.getCachedData();
            }
        }
        
        // Hole die Ressourcendatei
        File resourceFile = getResourceFile(moduleName, resourcePath);
        if (resourceFile == null || !resourceFile.exists() || !resourceFile.isFile()) {
            if (core.isDebugMode()) {
                console.debug("Ressource nicht gefunden: " + resourcePath + " (Modul: " + moduleName + ")", true);
            }
            
            // Versuche die Ressource direkt aus der JAR zu laden, falls sie nicht extrahiert wurde
            return loadResourceDirectlyFromJar(moduleName, resourcePath);
        }
        
        try {
            // Erstelle oder aktualisiere ResourceInfo
            ResourceInfo resourceInfo = new ResourceInfo(resourceFile);
            
            // Lese die Datei
            byte[] data;
            try (FileInputStream in = new FileInputStream(resourceFile);
                 ByteArrayOutputStream out = new ByteArrayOutputStream((int)resourceFile.length())) {
                
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                
                data = out.toByteArray();
            }
            
            // Cache aktualisieren, falls aktiviert
            if (cacheEnabled) {
                resourceInfo.cacheData(data);
                putCachedResourceInfo(moduleName, resourcePath, resourceInfo);
            }
            
            return data;
        } catch (IOException e) {
            console.warning("Fehler beim Lesen der Ressource " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Liest eine Ressourcendatei als String
     * 
     * @param moduleName Der Name des Moduls
     * @param resourcePath Der Pfad zur Ressource
     * @return Die Ressource als String oder null, wenn sie nicht gefunden wurde
     */
    public String getResourceAsString(String moduleName, String resourcePath) {
        byte[] data = getResourceAsBytes(moduleName, resourcePath);
        if (data == null) {
            return null;
        }
        
        return new String(data, StandardCharsets.UTF_8);
    }
    
    /**
     * Erstellt einen InputStream für eine Ressource
     * 
     * @param moduleName Der Name des Moduls
     * @param resourcePath Der Pfad zur Ressource
     * @return Ein InputStream für die Ressource oder null, wenn sie nicht gefunden wurde
     */
    public InputStream getResourceAsStream(String moduleName, String resourcePath) {
        if (moduleName == null || resourcePath == null) {
            return null;
        }
        
        // Pfad normalisieren
        resourcePath = normalizeResourcePath(resourcePath);
        
        // Hole die Ressourcendatei
        File resourceFile = getResourceFile(moduleName, resourcePath);
        if (resourceFile == null || !resourceFile.exists() || !resourceFile.isFile()) {
            // Versuche die Ressource direkt aus der JAR zu laden
            return getModuleJarResourceStream(moduleName, resourcePath);
        }
        
        try {
            return new FileInputStream(resourceFile);
        } catch (FileNotFoundException e) {
            console.warning("Fehler beim Öffnen der Ressource " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Speichert eine Ressourcendatei
     * 
     * @param moduleName Der Name des Moduls
     * @param resourcePath Der Pfad zur Ressource
     * @param data Die zu speichernden Daten
     * @return true, wenn die Ressource erfolgreich gespeichert wurde
     */
    public boolean saveResource(String moduleName, String resourcePath, byte[] data) {
        if (moduleName == null || resourcePath == null || data == null) {
            return false;
        }
        
        // Pfad normalisieren
        resourcePath = normalizeResourcePath(resourcePath);
        
        // Hole das Ressourcenverzeichnis
        File resourcesDir = getModuleResourceDirectory(moduleName);
        if (resourcesDir == null) {
            console.error("Ressourcenverzeichnis für Modul " + moduleName + " nicht gefunden!");
            return false;
        }
        
        // Zieldatei erstellen
        File resourceFile = new File(resourcesDir, resourcePath);
        
        // Stelle sicher, dass das Elternverzeichnis existiert
        File parent = resourceFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            console.error("Konnte Verzeichnis nicht erstellen: " + parent.getAbsolutePath());
            return false;
        }
        
        // Backup erstellen, falls die Datei bereits existiert und Backups aktiviert sind
        if (resourceFile.exists() && backupOnOverwrite) {
            if (!backupFile(resourceFile)) {
                return false;
            }
        }
        
        // Datei speichern
        try (FileOutputStream out = new FileOutputStream(resourceFile)) {
            out.write(data);
            
            // Cache aktualisieren, falls aktiviert
            if (cacheEnabled) {
                ResourceInfo resourceInfo = new ResourceInfo(resourceFile);
                resourceInfo.cacheData(data);
                putCachedResourceInfo(moduleName, resourcePath, resourceInfo);
            }
            
            // Index aktualisieren
            ResourceIndex index = moduleResourceIndices.computeIfAbsent(moduleName, k -> new ResourceIndex());
            index.addResource(resourcePath);
            
            if (core.isDebugMode()) {
                console.debug("Ressource gespeichert: " + resourceFile.getAbsolutePath(), true);
            }
            
            return true;
        } catch (IOException e) {
            console.error("Fehler beim Speichern der Ressource " + resourcePath + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Speichert eine Ressourcendatei als String
     * 
     * @param moduleName Der Name des Moduls
     * @param resourcePath Der Pfad zur Ressource
     * @param content Der zu speichernde Inhalt
     * @return true, wenn die Ressource erfolgreich gespeichert wurde
     */
    public boolean saveResourceAsString(String moduleName, String resourcePath, String content) {
        if (content == null) {
            return false;
        }
        
        return saveResource(moduleName, resourcePath, content.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Prüft, ob eine Ressource existiert
     * 
     * @param moduleName Der Name des Moduls
     * @param resourcePath Der Pfad zur Ressource
     * @return true, wenn die Ressource existiert
     */
    public boolean resourceExists(String moduleName, String resourcePath) {
        if (moduleName == null || resourcePath == null) {
            return false;
        }
        
        // Pfad normalisieren
        resourcePath = normalizeResourcePath(resourcePath);
        
        // Prüfe im Index nach
        ResourceIndex index = moduleResourceIndices.get(moduleName);
        if (index != null && index.hasResource(resourcePath)) {
            return true;
        }
        
        // Prüfe Dateisystem
        File resourceFile = getResourceFile(moduleName, resourcePath);
        return resourceFile != null && resourceFile.exists() && resourceFile.isFile();
    }
    
    /**
     * Baut den Ressourcenindex für ein Modul auf
     * 
     * @param moduleName Der Name des Moduls
     * @return Anzahl der indizierten Ressourcen
     */
    public int buildResourceIndex(String moduleName) {
        if (moduleName == null) {
            return 0;
        }
        
        File resourcesDir = getModuleResourceDirectory(moduleName);
        if (resourcesDir == null || !resourcesDir.exists()) {
            return 0;
        }
        
        ResourceIndex index = new ResourceIndex();
        int count = indexResourceDirectory(resourcesDir, resourcesDir, index);
        
        if (count > 0) {
            moduleResourceIndices.put(moduleName, index);
        }
        
        return count;
    }
    
    /**
     * Indiziert rekursiv ein Verzeichnis und seine Unterverzeichnisse
     */
    private int indexResourceDirectory(File baseDir, File currentDir, ResourceIndex index) {
        File[] files = currentDir.listFiles();
        if (files == null) {
            return 0;
        }
        
        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += indexResourceDirectory(baseDir, file, index);
            } else if (file.isFile()) {
                String relativePath = getRelativePath(baseDir, file);
                index.addResource(relativePath);
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Findet alle Ressourcen in einem bestimmten Verzeichnis
     * 
     * @param moduleName Der Name des Moduls
     * @param directory Das Verzeichnis innerhalb der Ressourcen
     * @return Eine Liste mit den gefundenen Ressourcen
     */
    public List<String> listResources(String moduleName, String directory) {
        if (moduleName == null) {
            return Collections.emptyList();
        }
        
        // Normalisiere den Pfad
        if (directory != null) {
            directory = normalizeResourcePath(directory);
            if (directory.endsWith("/")) {
                directory = directory.substring(0, directory.length() - 1);
            }
        }
        
        // Prüfe zuerst im Index
        ResourceIndex index = moduleResourceIndices.get(moduleName);
        if (index != null) {
            if (directory == null || directory.isEmpty()) {
                return new ArrayList<>(index.getAll());
            }
            
            return new ArrayList<>(index.findInDirectory(directory));
        }
        
        // Wenn kein Index, baue ihn zuerst auf
        buildResourceIndex(moduleName);
        index = moduleResourceIndices.get(moduleName);
        
        if (index != null) {
            if (directory == null || directory.isEmpty()) {
                return new ArrayList<>(index.getAll());
            }
            
            return new ArrayList<>(index.findInDirectory(directory));
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Findet alle Ressourcen mit einer bestimmten Dateiendung
     * 
     * @param moduleName Der Name des Moduls
     * @param extension Die Dateiendung
     * @return Eine Liste mit den gefundenen Ressourcen
     */
    public List<String> findResourcesByExtension(String moduleName, String extension) {
        if (moduleName == null || extension == null) {
            return Collections.emptyList();
        }
        
        // Normalisiere die Endung
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        
        // Prüfe zuerst im Index
        ResourceIndex index = moduleResourceIndices.get(moduleName);
        if (index != null) {
            return new ArrayList<>(index.findByExtension(extension));
        }
        
        // Wenn kein Index, baue ihn zuerst auf
        buildResourceIndex(moduleName);
        index = moduleResourceIndices.get(moduleName);
        
        if (index != null) {
            return new ArrayList<>(index.findByExtension(extension));
        }
        
        return Collections.emptyList();
    }
    
    // ---------------------------- Hilfsmethoden ----------------------------
    
    /**
     * Holt das Ressourcenverzeichnis für ein Modul
     */
    private File getModuleResourceDirectory(String moduleName) {
        File moduleDataDir = core.getModuleDataFolder(moduleName);
        if (moduleDataDir == null) {
            return null;
        }
        
        File resourcesDir = new File(moduleDataDir, "resources");
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }
        
        return resourcesDir.exists() ? resourcesDir : null;
    }
    
    /**
     * Holt die Ressourcendatei für einen bestimmten Pfad
     */
    private File getResourceFile(String moduleName, String resourcePath) {
        File resourcesDir = getModuleResourceDirectory(moduleName);
        if (resourcesDir == null) {
            return null;
        }
        
        return new File(resourcesDir, resourcePath);
    }
    
    /**
     * Normalisiert einen Ressourcenpfad
     */
    private String normalizeResourcePath(String resourcePath) {
        if (resourcePath == null) {
            return "";
        }
        
        resourcePath = resourcePath.replace('\\', '/');
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        
        return resourcePath;
    }
    
    /**
     * Bestimmt den relativen Pfad einer Datei zu einem Basisverzeichnis
     */
    private String getRelativePath(File baseDir, File file) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        
        if (filePath.startsWith(basePath)) {
            String relativePath = filePath.substring(basePath.length());
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(1);
            }
            
            // Normalisiere den Pfad für konsistente Indizierung
            return relativePath.replace('\\', '/');
        }
        
        return file.getName();
    }
    
    /**
     * Prüft, ob eine Datei eine Ressourcendatei ist
     */
    private boolean isResourceFile(String fileName) {
        // Typische Nicht-Ressourcendateien ausschließen
        if (fileName.endsWith(".class") || 
            fileName.equals("module.yml") || 
            fileName.equals("plugin.yml") || 
            fileName.equals("META-INF/MANIFEST.MF")) {
            return false;
        }
        
        // Prüfe auf bekannte Ressourcenendungen
        return hasResourceExtension(fileName) || matchesResourcePattern(fileName);
    }
    
    /**
     * Prüft, ob eine Datei eine der bekannten Ressourcenendungen hat
     */
    private boolean hasResourceExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            String extension = fileName.substring(lastDot + 1).toLowerCase();
            return commonResourceExtensions.contains(extension);
        }
        
        return false;
    }
    
    /**
     * Prüft, ob ein Pfad einem Ressourcenmuster entspricht
     */
    private boolean matchesResourcePattern(String path) {
        for (Pattern pattern : resourcePathPatterns) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Erkennt Basispfade für Ressourcen in einer JAR-Datei
     */
    private List<String> detectResourceBasePaths(File jarFile) {
        List<String> basePaths = new ArrayList<>();
        basePaths.add(""); // Leerer Pfad als Fallback
        
        try (JarFile jar = new JarFile(jarFile)) {
            // Typische Ressourcenpfade prüfen
            String[] commonPaths = {
                "resources/",
                "assets/",
                "src/main/resources/",
                "main/resources/",
                "META-INF/resources/"
            };
            
            for (String path : commonPaths) {
                if (jar.getJarEntry(path) != null) {
                    basePaths.add(path);
                }
            }
            
            // Prüfe, ob ein module.yml vorhanden ist, und bestimme den Pfad relativ dazu
            String moduleYmlPath = findModuleYmlPath(jar);
            if (moduleYmlPath != null) {
                String basePath = moduleYmlPath.substring(0, moduleYmlPath.lastIndexOf("module.yml"));
                if (!basePaths.contains(basePath)) {
                    basePaths.add(basePath);
                }
            }
            
            // Analyse der JAR-Struktur
            Map<String, Integer> dirCounts = new HashMap<>();
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                if (entry.isDirectory()) {
                    // Zähle Dateien in diesem Verzeichnis
                    dirCounts.put(name, dirCounts.getOrDefault(name, 0));
                } else {
                    // Inkrementiere Zähler für Elternverzeichnisse
                    int lastSlash = name.lastIndexOf('/');
                    if (lastSlash > 0) {
                        String dir = name.substring(0, lastSlash + 1);
                        dirCounts.put(dir, dirCounts.getOrDefault(dir, 0) + 1);
                    }
                }
            }
            
            // Füge Verzeichnisse mit vielen Dateien hinzu (potenzielle Ressourcenverzeichnisse)
            dirCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 3) // Mindestens 3 Dateien
                .filter(e -> !e.getKey().contains("META-INF")) // Ignoriere META-INF
                .filter(e -> hasResourceFiles(jar, e.getKey())) // Prüfe auf Ressourcendateien
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()) // Nach Anzahl der Dateien sortieren
                .limit(3) // Maximal 3 Verzeichnisse hinzufügen
                .forEach(e -> {
                    if (!basePaths.contains(e.getKey())) {
                        basePaths.add(e.getKey());
                    }
                });
        } catch (IOException e) {
            console.warning("Fehler beim Analysieren der JAR-Struktur: " + e.getMessage());
        }
        
        return basePaths;
    }
    
    /**
     * Sucht nach der module.yml in einer JAR-Datei
     */
    private String findModuleYmlPath(JarFile jar) {
        // Typische Pfade für module.yml
        String[] commonPaths = {
            "module.yml",
            "src/main/resources/module.yml",
            "main/resources/module.yml",
            "resources/module.yml",
            "META-INF/module.yml"
        };
        
        for (String path : commonPaths) {
            if (jar.getJarEntry(path) != null) {
                return path;
            }
        }
        
        // Durchsuche alle Einträge
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            
            if (name.endsWith("/module.yml") || name.equals("module.yml")) {
                return name;
            }
        }
        
        return null;
    }
    
    /**
     * Prüft, ob ein Verzeichnis in einer JAR Ressourcendateien enthält
     */
    private boolean hasResourceFiles(JarFile jar, String dirPath) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            
            if (name.startsWith(dirPath) && !entry.isDirectory() && isResourceFile(name)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Lädt eine Ressource direkt aus der JAR, ohne sie vorher zu extrahieren
     */
    private byte[] loadResourceDirectlyFromJar(String moduleName, String resourcePath) {
        com.essentialscore.api.module.ModuleManager.ModuleInfo moduleInfo = core.getModuleInfo(moduleName);
        if (moduleInfo == null) {
            return null;
        }
        
        File jarFile = moduleInfo.getJarFile();
        if (jarFile == null || !jarFile.exists()) {
            return null;
        }
        
        try (JarFile jar = new JarFile(jarFile)) {
            // Normalisiere den Pfad
            resourcePath = normalizeResourcePath(resourcePath);
            
            // Erkenne Ressourcenpfade
            List<String> basePaths = detectResourceBasePaths(jarFile);
            
            // Versuche verschiedene Kombinationen von Basispfaden und Ressourcenpfaden
            for (String basePath : basePaths) {
                String fullPath = basePath.isEmpty() ? resourcePath : basePath + resourcePath;
                JarEntry entry = jar.getJarEntry(fullPath);
                
                if (entry != null) {
                    try (InputStream in = jar.getInputStream(entry);
                         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        
                        byte[] buffer = new byte[bufferSize];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        
                        if (core.isDebugMode()) {
                            console.debug("Ressource direkt aus JAR geladen: " + fullPath, true);
                        }
                        
                        return out.toByteArray();
                    }
                }
            }
            
            // Wenn direkter Pfad nicht funktioniert, versuche alle Einträge zu durchsuchen
            Enumeration<JarEntry> entries = jar.entries();
            String fileName = new File(resourcePath).getName();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // Prüfe, ob der Eintrag mit dem gesuchten Dateinamen endet
                if (entryName.endsWith("/" + fileName) && isResourceFile(entryName)) {
                    try (InputStream in = jar.getInputStream(entry);
                         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        
                        byte[] buffer = new byte[bufferSize];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        
                        if (core.isDebugMode()) {
                            console.debug("Ressource durch Suche in JAR gefunden: " + entryName, true);
                        }
                        
                        return out.toByteArray();
                    }
                }
            }
        } catch (IOException e) {
            if (core.isDebugMode()) {
                console.debug("Fehler beim direkten Laden aus JAR: " + e.getMessage(), true);
            }
        }
        
        return null;
    }
    
    /**
     * Erstellt einen InputStream für eine Ressource direkt aus der JAR
     */
    private InputStream getModuleJarResourceStream(String moduleName, String resourcePath) {
        com.essentialscore.api.module.ModuleManager.ModuleInfo moduleInfo = core.getModuleInfo(moduleName);
        if (moduleInfo == null) {
            return null;
        }
        
        File jarFile = moduleInfo.getJarFile();
        if (jarFile == null || !jarFile.exists()) {
            return null;
        }
        
        try {
            final JarFile jar = new JarFile(jarFile);
            
            // Normalisiere den Pfad
            resourcePath = normalizeResourcePath(resourcePath);
            
            // Erkenne Ressourcenpfade
            List<String> basePaths = detectResourceBasePaths(jarFile);
            
            // Versuche verschiedene Kombinationen von Basispfaden und Ressourcenpfaden
            for (String basePath : basePaths) {
                String fullPath = basePath.isEmpty() ? resourcePath : basePath + resourcePath;
                JarEntry entry = jar.getJarEntry(fullPath);
                
                if (entry != null) {
                    final InputStream originalStream = jar.getInputStream(entry);
                    
                    // Wir erstellen einen speziellen InputStream, der auch die JAR schließt
                    return new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return originalStream.read();
                        }
                        
                        @Override
                        public int read(byte[] b, int off, int len) throws IOException {
                            return originalStream.read(b, off, len);
                        }
                        
                        @Override
                        public void close() throws IOException {
                            originalStream.close();
                            jar.close();
                        }
                    };
                }
            }
            
            // Wenn direkter Pfad nicht funktioniert, versuche alle Einträge zu durchsuchen
            Enumeration<JarEntry> entries = jar.entries();
            String fileName = new File(resourcePath).getName();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // Prüfe, ob der Eintrag mit dem gesuchten Dateinamen endet
                if (entryName.endsWith("/" + fileName) && isResourceFile(entryName)) {
                    final InputStream originalStream = jar.getInputStream(entry);
                    
                    return new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return originalStream.read();
                        }
                        
                        @Override
                        public int read(byte[] b, int off, int len) throws IOException {
                            return originalStream.read(b, off, len);
                        }
                        
                        @Override
                        public void close() throws IOException {
                            originalStream.close();
                            jar.close();
                        }
                    };
                }
            }
            
            // Keine passende Ressource gefunden
            jar.close();
        } catch (IOException e) {
            if (core.isDebugMode()) {
                console.debug("Fehler beim Erstellen des InputStreams: " + e.getMessage(), true);
            }
        }
        
        return null;
    }
    
    // ---------------------------- Cache-Verwaltung ----------------------------
    
    /**
     * Holt eine ResourceInfo aus dem Cache
     */
    private ResourceInfo getCachedResourceInfo(String moduleName, String resourcePath) {
        Map<String, ResourceInfo> moduleCache = resourceCache.get(moduleName);
        if (moduleCache != null) {
            ResourceInfo info = moduleCache.get(resourcePath);
            if (info != null && info.isValid()) {
                return info;
            } else if (info != null) {
                // Ungültige Einträge entfernen
                moduleCache.remove(resourcePath);
            }
        }
        
        return null;
    }
    
    /**
     * Speichert eine ResourceInfo im Cache
     */
    private void putCachedResourceInfo(String moduleName, String resourcePath, ResourceInfo info) {
        Map<String, ResourceInfo> moduleCache = resourceCache.computeIfAbsent(moduleName, k -> new ConcurrentHashMap<>());
        moduleCache.put(resourcePath, info);
    }
    
    /**
     * Leert den Cache für ein Modul
     */
    public void clearModuleCache(String moduleName) {
        if (moduleName != null) {
            resourceCache.remove(moduleName);
            if (core.isDebugMode()) {
                console.debug("Cache für Modul " + moduleName + " geleert", true);
            }
        }
    }
    
    /**
     * Leert den gesamten Cache
     */
    public void clearAllCaches() {
        resourceCache.clear();
        if (core.isDebugMode()) {
            console.debug("Alle Ressourcen-Caches geleert", true);
        }
    }

    /**
     * Setzt, ob vorhandene Dateien überschrieben werden sollen
     *
     * @param overwriteExisting True, wenn vorhandene Dateien überschrieben werden sollen
     */
    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    /**
     * Setzt, ob Sicherungskopien erstellt werden sollen, wenn Dateien überschrieben werden
     *
     * @param backupOnOverwrite True, wenn Sicherungskopien erstellt werden sollen
     */
    public void setBackupOnOverwrite(boolean backupOnOverwrite) {
        this.backupOnOverwrite = backupOnOverwrite;
    }

    /**
     * Setzt, ob die Integrität der extrahierten Dateien überprüft werden soll
     *
     * @param verifyIntegrity True, wenn die Integrität überprüft werden soll
     */
    public void setVerifyIntegrity(boolean verifyIntegrity) {
        this.verifyIntegrity = verifyIntegrity;
    }

    /**
     * Setzt die Standard-Dateitypen, die extrahiert werden sollen
     *
     * @param defaultFileTypes Liste der Dateitypen (z.B. ".yml", ".json")
     */
    public void setDefaultFileTypes(List<String> defaultFileTypes) {
        if (defaultFileTypes != null && !defaultFileTypes.isEmpty()) {
            this.defaultFileTypes = new ArrayList<>(defaultFileTypes);
        }
    }

    /**
     * Setzt die zusätzlichen Unterverzeichnisse, die extrahiert werden sollen
     *
     * @param extractSubdirectories Liste der Unterverzeichnisse
     */
    public void setExtractSubdirectories(List<String> extractSubdirectories) {
        if (extractSubdirectories != null) {
            this.extractSubdirectories = new ArrayList<>(extractSubdirectories);
        }
    }

    /**
     * Setzt die Muster für Dateien, die ignoriert werden sollen
     *
     * @param ignorePatterns Liste der Ignorier-Muster
     */
    public void setIgnorePatterns(List<String> ignorePatterns) {
        if (ignorePatterns != null) {
            this.ignorePatterns = new ArrayList<>(ignorePatterns);
        }
    }

    /**
     * Prüft, ob eine Datei basierend auf ihrem Dateityp extrahiert werden soll
     *
     * @param fileName Der Dateiname
     * @return True, wenn die Datei extrahiert werden soll
     */
    @SuppressWarnings("unused")
    private boolean shouldExtractFile(String fileName) {
        // Prüfe, ob die Datei einen der Standard-Dateitypen hat
        boolean isDefaultType = defaultFileTypes.stream()
                .anyMatch(fileName::endsWith);

        // Prüfe, ob die Datei einem Ignorier-Muster entspricht
        boolean isIgnored = ignorePatterns.stream()
                .anyMatch(pattern -> matchesGlobPattern(fileName, pattern));

        return isDefaultType && !isIgnored;
    }

    /**
     * Prüft, ob ein Unterverzeichnis extrahiert werden soll
     *
     * @param dirName Der Verzeichnisname
     * @return True, wenn das Verzeichnis extrahiert werden soll
     */
    @SuppressWarnings("unused")
    private boolean shouldExtractDirectory(String dirName) {
        return extractSubdirectories.contains(dirName);
    }

    /**
     * Erstellt eine Sicherungskopie einer Datei
     *
     * @param file Die zu sichernde Datei
     * @return True, wenn die Sicherung erfolgreich war
     */
    private boolean backupFile(File file) {
        if (!file.exists()) {
            return false;
        }

        try {
            File backupFile = new File(file.getAbsolutePath() + ".bak");
            
            // Lösche alte Sicherung, falls vorhanden
            if (backupFile.exists()) {
                backupFile.delete();
            }
            
            // Kopiere aktuelle Datei zur Sicherung
            Files.copy(file.toPath(), backupFile.toPath());
            return true;
        } catch (Exception e) {
            console.warning("Fehler beim Erstellen der Sicherungskopie für " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Überprüft die Integrität einer extrahierten Datei durch Berechnung und Vergleich einer Prüfsumme
     *
     * @param sourceData Die Quelldaten
     * @param targetFile Die Zieldatei
     * @return True, wenn die Integrität gewährleistet ist
     */
    private boolean verifyFileIntegrity(byte[] sourceData, File targetFile) {
        if (!targetFile.exists() || sourceData == null) {
            return false;
        }

        try {
            byte[] targetData = Files.readAllBytes(targetFile.toPath());
            
            // Vergleiche Länge
            if (sourceData.length != targetData.length) {
                return false;
            }
            
            // Berechne Prüfsummen
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] sourceHash = md.digest(sourceData);
            md.reset();
            byte[] targetHash = md.digest(targetData);
            
            // Vergleiche Prüfsummen
            return java.util.Arrays.equals(sourceHash, targetHash);
        } catch (Exception e) {
            console.warning("Fehler bei der Integritätsprüfung für " + targetFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Prüft, ob ein Dateiname einem Glob-Muster entspricht
     *
     * @param fileName Der Dateiname
     * @param pattern Das Glob-Muster
     * @return True, wenn der Dateiname dem Muster entspricht
     */
    private boolean matchesGlobPattern(String fileName, String pattern) {
        // Konvertiere Glob-Muster in ein Regex-Muster
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        
        return fileName.matches(regex);
    }

    /**
     * Lädt eine Yaml-Konfigurationsdatei aus einer JAR-Datei
     * 
     * @param jar JAR-Datei
     * @param entryName Name des Eintrags
     * @return YamlConfiguration oder null bei Fehler
     */
    public YamlConfiguration loadYamlFromJar(JarFile jar, String entryName) {
        try {
            JarEntry entry = jar.getJarEntry(entryName);
            if (entry == null) {
                return null;
            }
            
            try (InputStream is = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                
                YamlConfiguration config = new YamlConfiguration();
                config.load(reader);
                return config;
            }
        } catch (Exception e) {
            console.warning("Fehler beim Laden der YAML-Datei aus JAR: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Listet alle extrahierten Ressourcen für ein Modul auf
     * 
     * @param moduleName Name des Moduls
     * @return Liste der extrahierten Ressourcendateien
     */
    public List<File> listExtractedResources(String moduleName) {
        List<File> result = new ArrayList<>();
        
        File resourcesDir = new File(core.getModuleDataFolder(moduleName), "resources");
        if (!resourcesDir.exists()) {
            return result;
        }
        
        collectFiles(resourcesDir, result);
        return result;
    }
    
    /**
     * Sammelt rekursiv Dateien aus einem Verzeichnis
     * 
     * @param directory Verzeichnis
     * @param files Liste zum Sammeln der Dateien
     */
    private void collectFiles(File directory, List<File> files) {
        File[] contents = directory.listFiles();
        if (contents == null) {
            return;
        }
        
        for (File file : contents) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                collectFiles(file, files);
            }
        }
    }
} 
