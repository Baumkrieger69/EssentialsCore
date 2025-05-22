package com.essentialscore.api.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility-Klasse für Datei-Operationen
 */
public class FileUtils {
    
    private FileUtils() {
        // Privater Konstruktor um Instanziierung zu verhindern
    }
    
    /**
     * Liest eine Datei als String
     * 
     * @param file Die Datei
     * @return Der Inhalt der Datei
     * @throws IOException Bei Fehler
     */
    public static String readFileToString(File file) throws IOException {
        if (!file.exists()) {
            return "";
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }
    
    /**
     * Schreibt einen String in eine Datei
     * 
     * @param file Die Datei
     * @param content Der Inhalt
     * @throws IOException Bei Fehler
     */
    public static void writeStringToFile(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(content);
        }
    }
    
    /**
     * Kopiert eine Datei
     * 
     * @param source Die Quelldatei
     * @param target Die Zieldatei
     * @throws IOException Bei Fehler
     */
    public static void copyFile(File source, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * Entpackt eine ZIP-Datei
     * 
     * @param zipFile Die ZIP-Datei
     * @param destDir Das Zielverzeichnis
     * @throws IOException Bei Fehler
     */
    public static void unzip(File zipFile, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            while (entry != null) {
                File filePath = new File(destDir, entry.getName());
                
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    filePath.mkdirs();
                }
                
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }
    
    /**
     * Hilfsmethode zum Extrahieren einer Datei aus einem ZipInputStream
     */
    private static void extractFile(ZipInputStream zipIn, File filePath) throws IOException {
        File parent = filePath.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
    
    /**
     * Liest alle Zeilen einer Datei
     * 
     * @param file Die Datei
     * @return Liste mit Zeilen
     * @throws IOException Bei Fehler
     */
    public static List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        
        if (!file.exists()) {
            return lines;
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        
        return lines;
    }
    
    /**
     * Iteriert über alle Zeilen einer Datei
     * 
     * @param file Die Datei
     * @param lineConsumer Der Consumer für jede Zeile
     * @throws IOException Bei Fehler
     */
    public static void forEachLine(File file, Consumer<String> lineConsumer) throws IOException {
        if (!file.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineConsumer.accept(line);
            }
        }
    }
} 