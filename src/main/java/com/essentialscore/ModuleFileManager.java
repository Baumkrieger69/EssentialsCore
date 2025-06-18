package com.essentialscore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Manager für Modul-Dateien
 */
public class ModuleFileManager {
    private final ApiCore core;
    private final Map<String, ModuleFileInfo> moduleFiles = new HashMap<>();
    private final String[] CONSOLE_COLORS = {
        "§b", "§d", "§e", "§a", "§6", "§3", "§5", "§9"
    };
    private int colorIndex = 0;
    private ConsoleFormatter console;

    public ModuleFileManager(ApiCore core) {
        this.core = core;
        
        // Erweiterte Konsolen-Formatter Konfiguration
        boolean useColors = core.getConfig().getBoolean("console.use-colors", true);
        boolean showTimestamps = core.getConfig().getBoolean("console.show-timestamps", false);
        boolean useUnicodeSymbols = core.getConfig().getBoolean("console.use-unicode-symbols", true);
        String stylePreset = core.getConfig().getString("console.style-preset", "default");
        
        this.console = new ConsoleFormatter(
            core.getLogger(),
            core.getConfig().getString("console.prefixes.file-manager", "&8[&6&lFileManager&8]"),
            useColors, showTimestamps, useUnicodeSymbols, stylePreset
        );
    }

    public static class ModuleFileInfo {
        private final String moduleName;
        private final File jarFile;
        private final File dataDir;
        private final File configFile;
        private final File resourcesDir;
        private final Map<String, String> paths;

        public ModuleFileInfo(String moduleName, File jarFile, File dataDir, File configFile, File resourcesDir) {
            this.moduleName = moduleName;
            this.jarFile = jarFile;
            this.dataDir = dataDir;
            this.configFile = configFile;
            this.resourcesDir = resourcesDir;
            this.paths = new HashMap<>();
            updatePaths();
        }

        private void updatePaths() {
            paths.put("jarFile", jarFile.getAbsolutePath());
            paths.put("dataDir", dataDir.getAbsolutePath());
            paths.put("configFile", configFile.getAbsolutePath());
            paths.put("resourcesDir", resourcesDir.getAbsolutePath());
        }

        public String getModuleName() { return moduleName; }
        public File getJarFile() { return jarFile; }
        public File getDataDir() { return dataDir; }
        public File getConfigFile() { return configFile; }
        public File getResourcesDir() { return resourcesDir; }
        public Map<String, String> getPaths() { return paths; }
    }

    /**
     * Initialisiert die Verzeichnisstruktur für ein Modul
     */
    public ModuleFileInfo initializeModuleFiles(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            console.error("Modulname darf nicht leer sein!");
            return null;
        }

        // Prüfe, ob bereits initialisiert
        ModuleFileInfo existingInfo = moduleFiles.get(moduleName);
        if (existingInfo != null) {
            return existingInfo;
        }

        try {
            // Hole die JAR-Datei des Moduls
            com.essentialscore.api.module.ModuleManager.ModuleInfo moduleInfo = core.getModuleInfo(moduleName);
            if (moduleInfo == null) {
                console.error("Modul " + moduleName + " nicht gefunden!");
                return null;
            }
            
            File jarFile = moduleInfo.getJarFile();
            if (jarFile == null || !jarFile.exists()) {
                console.error("JAR-Datei für Modul " + moduleName + " nicht gefunden!");
                return null;
            }
            
            // Erstelle Verzeichnisstruktur
            File dataFolder = new File(core.getDataFolder(), "data/" + moduleName);
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                console.error("Konnte Datenverzeichnis für Modul " + moduleName + " nicht erstellen!");
                return null;
            }
            
            File configFile = new File(core.getDataFolder(), "config/" + moduleName + ".yml");
            File resourcesDir = new File(dataFolder, "resources");
            if (!resourcesDir.exists() && !resourcesDir.mkdirs()) {
                console.error("Konnte Ressourcenverzeichnis für Modul " + moduleName + " nicht erstellen!");
                return null;
            }
            
            // Erstelle und speichere ModuleFileInfo
            ModuleFileInfo fileInfo = new ModuleFileInfo(moduleName, jarFile, dataFolder, configFile, resourcesDir);
            moduleFiles.put(moduleName, fileInfo);
            
            if (core.isDebugMode()) {
                console.debug("Dateistruktur für Modul " + moduleName + " initialisiert", true);
                console.debug("  Datenverzeichnis: " + dataFolder.getAbsolutePath(), true);
                console.debug("  Konfigurationsdatei: " + configFile.getAbsolutePath(), true);
                console.debug("  Ressourcenverzeichnis: " + resourcesDir.getAbsolutePath(), true);
            }
            
            return fileInfo;
        } catch (Exception e) {
            console.error("Fehler bei der Initialisierung der Dateistruktur für Modul " + moduleName + ": " + e.getMessage());
            if (core.isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Extrahiert Ressourcen für ein Modul
     */
    public boolean extractResources(String moduleName) {
        ModuleFileInfo fileInfo = getModuleFileInfo(moduleName);
        if (fileInfo == null) {
            console.error("Keine Dateiinformationen für Modul " + moduleName + " gefunden!");
            return false;
        }
        
        File jarFile = fileInfo.getJarFile();
        File resourcesDir = fileInfo.getResourcesDir();
        
        if (jarFile == null || !jarFile.exists()) {
            console.error("JAR-Datei für Modul " + moduleName + " nicht gefunden!");
            return false;
        }
        
        if (!resourcesDir.exists() && !resourcesDir.mkdirs()) {
            console.error("Konnte Ressourcenverzeichnis für Modul " + moduleName + " nicht erstellen!");
            return false;
        }
        
        try {
            console.info("Extrahiere Ressourcen für Modul " + moduleName + "...");
            
            try (JarFile jar = new JarFile(jarFile)) {
                int extractedCount = 0;
                Enumeration<JarEntry> entries = jar.entries();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    
                    // Ignoriere Verzeichnisse und Klassendateien
                    if (entry.isDirectory() || name.endsWith(".class") || name.equals("module.yml")) {
                        continue;
                    }
                    
                    // Ignoriere Nicht-Ressourcendateien
                    if (!isResourceFile(name)) {
                        continue;
                    }
                    
                    // Bestimme Zieldatei
                    String relativePath = getRelativeResourcePath(name);
                    File targetFile = new File(resourcesDir, relativePath);
                    
                    // Stelle sicher, dass Verzeichnisse existieren
                    File parent = targetFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        console.warning("Konnte Verzeichnis nicht erstellen: " + parent.getAbsolutePath());
                        continue;
                    }
                    
                    // Überspringe existierende Dateien
                    if (targetFile.exists() && !core.getConfig().getBoolean("resources.overwrite-existing", false)) {
                        continue;
                    }
                    
                    // Extrahiere Datei
                    try (InputStream in = jar.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(targetFile)) {
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        
                        extractedCount++;
                    } catch (IOException e) {
                        console.warning("Fehler beim Extrahieren von " + name + ": " + e.getMessage());
                    }
                }
                
                if (extractedCount > 0) {
                    console.success(extractedCount + " Ressourcen für Modul " + moduleName + " extrahiert");
                } else if (core.isDebugMode()) {
                    console.info("Keine Ressourcen für Modul " + moduleName + " gefunden oder alle bereits extrahiert");
                }
                
                return true;
            }
        } catch (Exception e) {
            console.error("Fehler beim Extrahieren der Ressourcen für Modul " + moduleName + ": " + e.getMessage());
            if (core.isDebugMode()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Speichert eine Ressource 
     */
    public boolean saveResource(String moduleName, String resourcePath, byte[] data) {
        if (moduleName == null || resourcePath == null || data == null) {
            return false;
        }
        
        ModuleFileInfo fileInfo = getModuleFileInfo(moduleName);
        if (fileInfo == null) {
            console.error("Keine Dateiinformationen für Modul " + moduleName + " gefunden!");
            return false;
        }
        
        try {
            // Normalisiere den Pfad
            resourcePath = resourcePath.replace('\\', '/');
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            
            // Erstelle Zieldatei
            File resourceFile = new File(fileInfo.getResourcesDir(), resourcePath);
            
            // Stelle sicher, dass das Elternverzeichnis existiert
            File parent = resourceFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                console.error("Konnte Verzeichnis nicht erstellen: " + parent.getAbsolutePath());
                return false;
            }
            
            // Speichere Datei
            try (FileOutputStream out = new FileOutputStream(resourceFile)) {
                out.write(data);
            }
            
            if (core.isDebugMode()) {
                console.debug("Ressource gespeichert: " + resourceFile.getAbsolutePath(), true);
            }
            
            return true;
        } catch (Exception e) {
            console.error("Fehler beim Speichern der Ressource " + resourcePath + " für Modul " + moduleName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Lädt eine Ressource
     */
    public byte[] loadResource(String moduleName, String resourcePath) {
        if (moduleName == null || resourcePath == null) {
            return null;
        }
        
        ModuleFileInfo fileInfo = getModuleFileInfo(moduleName);
        if (fileInfo == null) {
            console.error("Keine Dateiinformationen für Modul " + moduleName + " gefunden!");
            return null;
        }
        
        try {
            // Normalisiere den Pfad
            resourcePath = resourcePath.replace('\\', '/');
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            
            // Erstelle Zieldatei
            File resourceFile = new File(fileInfo.getResourcesDir(), resourcePath);
            
            // Prüfe, ob die Datei existiert
            if (!resourceFile.exists() || !resourceFile.isFile()) {
                if (core.isDebugMode()) {
                    console.debug("Ressource nicht gefunden: " + resourceFile.getAbsolutePath(), true);
                }
                return null;
            }
            
            // Lade Datei
            try (FileInputStream in = new FileInputStream(resourceFile);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                
                return out.toByteArray();
            }
        } catch (Exception e) {
            console.error("Fehler beim Laden der Ressource " + resourcePath + " für Modul " + moduleName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Hilfsmethoden für die Konsolenausgabe
     */
    private void logInfo(String message) {
        String color = getNextColor();
        core.getLogger().info(color + "[" + getTimestamp() + "] " + message);
    }

    /**
     * Protokolliert eine Erfolgsmeldung
     */
    @SuppressWarnings("unused")
    private void logSuccess(String message) {
        console.success(message);
    }
    
    /**
     * Protokolliert eine Warnung
     */
    @SuppressWarnings("unused")
    private void logWarning(String message) {
        console.warning(message);
    }
    
    /**
     * Protokolliert einen Fehler
     */
    @SuppressWarnings("unused")
    private void logError(String message) {
        console.error(message);
    }

    private String getNextColor() {
        String color = CONSOLE_COLORS[colorIndex];
        colorIndex = (colorIndex + 1) % CONSOLE_COLORS.length;
        return color;
    }

    private String getTimestamp() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    /**
     * Hilfsmethoden für Dateioperationen
     */
    /**
     * Erstellt mehrere Verzeichnisse auf einmal
     */
    @SuppressWarnings("unused")
    private boolean createDirectories(File... directories) {
        boolean allSuccess = true;
        
        for (File dir : directories) {
            if (!dir.exists() && !dir.mkdirs()) {
                console.error("Konnte Verzeichnis nicht erstellen: " + dir.getAbsolutePath());
                allSuccess = false;
            }
        }
        
        return allSuccess;
    }

    private boolean isResourceFile(String fileName) {
        return !fileName.endsWith(".class") && 
               !fileName.equals("module.yml") && 
               !fileName.equals("plugin.yml") && 
               !fileName.equals("META-INF/MANIFEST.MF");
    }

    private String getRelativeResourcePath(String name) {
        // Implementieren Sie die Logik zur Berechnung des relativen Pfads
        // Dies ist nur ein Beispiel und sollte an Ihre spezifischen Anforderungen angepasst werden
        return name.replace('\\', '/');
    }

    /**
     * Getter für ModuleFileInfo
     */
    public ModuleFileInfo getModuleFileInfo(String moduleName) {
        return moduleFiles.get(moduleName);
    }

    /**
     * Entfernt ein Modul aus dem Dateimanager
     */
    public void removeModule(String moduleName) {
        moduleFiles.remove(moduleName);
        logInfo("Modul " + moduleName + " aus dem Dateimanager entfernt");
    }
} 
