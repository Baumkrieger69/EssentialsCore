package com.essentialscore.api.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility-Klasse für String-Operationen
 */
public class StringUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    private StringUtils() {
        // Privater Konstruktor um Instanziierung zu verhindern
    }
    
    /**
     * Formatiert einen String mit Minecraft-Farbcodes
     * 
     * @param text Der zu formatierende Text
     * @return Der formatierte Text
     */
    public static String formatColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Formatiert einen String mit Minecraft-Farbcodes inkl. Hex-Farben
     * 
     * @param text Der zu formatierende Text
     * @return Der formatierte Text
     */
    public static String formatHexColors(String text) {
        if (text == null) {
            return "";
        }
        
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString());
        }
        
        matcher.appendTail(buffer);
        return formatColors(buffer.toString());
    }
    
    /**
     * Entfernt alle Farbcodes aus einem String
     * 
     * @param text Der Text
     * @return Der Text ohne Farbcodes
     */
    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }
    
    /**
     * Teilt einen String in mehrere Zeilen auf
     * 
     * @param text Der Text
     * @param maxLineLength Die maximale Länge pro Zeile
     * @return Eine Liste mit Zeilen
     */
    public static List<String> splitIntoLines(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return lines;
        }
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLineLength) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder(word + " ");
            } else {
                currentLine.append(word).append(" ");
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        
        return lines;
    }
    
    /**
     * Capitalisiert den ersten Buchstaben eines Strings
     * 
     * @param text Der Text
     * @return Der Text mit großem Anfangsbuchstaben
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
} 