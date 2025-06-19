package com.essentialscore;

import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.lang.reflect.Field;

/**
 * Formatiert Konsolennachrichten mit Farben und verbesserten visuellen Elementen
 * Diese erweiterte Version bietet zahlreiche Formatierungsoptionen für eine
 * übersichtlichere und ansprechendere Konsolenausgabe.
 */
public class ConsoleFormatter {
      /**
     * Gibt eine Nachricht an die Konsole aus.
     * 
     * @param message Die auszugebende Nachricht
     */
    private void log(String message) {
        logger.info(message);
    }

    // ANSI Farbcodes
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    // Helle Farben
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_PURPLE = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";
    
    // Formatierungen
    public static final String BOLD = "\u001B[1m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String BLINK = "\u001B[5m";
    public static final String REVERSE = "\u001B[7m";
    
    // Hintergrundfarben
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_PURPLE = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_WHITE = "\u001B[47m";
    
    // Helle Hintergrundfarben
    public static final String BG_BRIGHT_BLACK = "\u001B[100m";
    public static final String BG_BRIGHT_RED = "\u001B[101m";
    public static final String BG_BRIGHT_GREEN = "\u001B[102m";
    public static final String BG_BRIGHT_YELLOW = "\u001B[103m";
    public static final String BG_BRIGHT_BLUE = "\u001B[104m";
    public static final String BG_BRIGHT_PURPLE = "\u001B[105m";
    public static final String BG_BRIGHT_CYAN = "\u001B[106m";
    public static final String BG_BRIGHT_WHITE = "\u001B[107m";
    
    // Symbole für verschiedene Messagetypen
    private static final String INFO_SYMBOL = "ℹ";
    private static final String SUCCESS_SYMBOL = "✓";
    private static final String WARNING_SYMBOL = "⚠";
    private static final String ERROR_SYMBOL = "✗";
    private static final String DEBUG_SYMBOL = "⚙";
    private static final String IMPORTANT_SYMBOL = "!";
    
    // Zeitformat
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
      private final Logger logger;
    private final String prefix;
    private final boolean useColors;
    private final boolean showTimestamp;
    private final boolean useUnicodeSymbols;
    
    // Konfiguration für die Kategorien
    private boolean categoriesEnabledGlobal = true;
    private final Map<MessageCategory, CategoryConfig> categoryConfigs = new EnumMap<>(MessageCategory.class);
    private final Map<MessageCategory, CategoryStyle> categoryStyles = new EnumMap<>(MessageCategory.class);
    
    /**
     * Konfiguration für eine Nachrichtenkategorie
     */
    private static class CategoryConfig {
        private final boolean enabled;
        private final String prefix;
        private final String color;
        private final String icon;
        
        public CategoryConfig(boolean enabled, String prefix, String color, String icon) {
            this.enabled = enabled;
            this.prefix = prefix;
            this.color = color;
            this.icon = icon;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public String getPrefix() {
            return prefix;
        }
        
        public String getColor() {
            return color;
        }
        
        public String getIcon() {
            return icon;
        }
    }
    
    /**
     * Erstellt einen neuen ConsoleFormatter
     * 
     * @param logger Der Logger für die Ausgabe
     * @param prefix Das Präfix für alle Nachrichten
     * @param useColors Ob Farben verwendet werden sollen
     */
    public ConsoleFormatter(Logger logger, String prefix, boolean useColors) {
        this(logger, prefix, useColors, false, true, "default");
    }
    
    /**
     * Erstellt einen neuen ConsoleFormatter mit erweiterten Optionen
     * 
     * @param logger Der Logger für die Ausgabe
     * @param prefix Das Präfix für alle Nachrichten
     * @param useColors Ob Farben verwendet werden sollen
     * @param showTimestamp Ob ein Zeitstempel angezeigt werden soll
     * @param useUnicodeSymbols Ob Unicode-Symbole verwendet werden sollen
     * @param stylePreset Das zu verwendende Stil-Preset
     */
    public ConsoleFormatter(Logger logger, String prefix, boolean useColors, 
                          boolean showTimestamp, boolean useUnicodeSymbols, String stylePreset) {        this.logger = logger;
        // Process prefix to convert Minecraft color codes to ANSI codes
        this.prefix = useColors ? formatHexCodes(prefix) : stripMinecraftColors(prefix);
        this.useColors = useColors;
        this.showTimestamp = showTimestamp;
        this.useUnicodeSymbols = useUnicodeSymbols;
        
        initializeCategoryStyles();
    }
    
    /**
     * Initialisiert die Stile für die verschiedenen Nachrichtenkategorien
     */
    private void initializeCategoryStyles() {
        for (MessageCategory category : MessageCategory.values()) {
            CategoryStyle style = new CategoryStyle(
                category.getColor(),
                category.getCode(),
                category.getIcon()
            );
            categoryStyles.put(category, style);
        }
    }
    
    /**
     * Entfernt alle Minecraft-Farbcodes aus einem String
     * Diese Methode wird verwendet, um sicherzustellen, dass keine Farbcodes in der Konsole angezeigt werden
     * 
     * @param input Der Text mit Minecraft-Farbcodes
     * @return Der Text ohne Minecraft-Farbcodes
     */
    private String stripMinecraftColors(String input) {
        if (input == null) return "";
        
        // Entferne alle § und & Farbcodes
        return input.replaceAll("§[0-9a-fklmnorx]", "")
                   .replaceAll("§x§[0-9a-f]§[0-9a-f]§[0-9a-f]§[0-9a-f]§[0-9a-f]§[0-9a-f]", "")
                   .replaceAll("&[0-9a-fklmnorx]", "")
                   .replaceAll("#[a-fA-F0-9]{6}", "");
    }
    
    /**
     * Formatiert Hex-Farbcodes im Format #RRGGBB
     * 
     * @param input Der Text mit Hex-Farbcodes
     * @return Der Text mit ANSI-Farbcodes
     */
    private String formatHexCodes(String input) {
        if (input == null) return "";
        
        String result = input;
        
        // Farbcodes ersetzen
        result = result.replace("&0", BLACK)
                       .replace("&1", BLUE)
                       .replace("&2", GREEN)
                       .replace("&3", CYAN)
                       .replace("&4", RED)
                       .replace("&5", PURPLE)
                       .replace("&6", YELLOW)
                       .replace("&7", WHITE)
                       .replace("&8", BRIGHT_BLACK)
                       .replace("&9", BRIGHT_BLUE)
                       .replace("&a", BRIGHT_GREEN)
                       .replace("&b", BRIGHT_CYAN)
                       .replace("&c", BRIGHT_RED)
                       .replace("&d", BRIGHT_PURPLE)
                       .replace("&e", BRIGHT_YELLOW)
                       .replace("&f", BRIGHT_WHITE)
                       .replace("&l", BOLD)
                       .replace("&n", UNDERLINE)
                       .replace("&o", ITALIC)
                       .replace("&r", RESET);
        
        // Sicherstellen, dass das Präfix mit einem Reset endet
        if (!result.endsWith(RESET)) {
            result += RESET;
        }
        
        return result;
    }
    
    /**
     * Formatiert eine Nachricht mit dem Präfix
     * 
     * @param message Die zu formatierende Nachricht
     * @return Die formatierte Nachricht
     */
    private String formatWithPrefix(String message) {
        StringBuilder result = new StringBuilder();
        
        // Zeitstempel hinzufügen, wenn aktiviert
        if (showTimestamp) {
            result.append(getTimeString()).append(" ");
        }
        
        // Präfix hinzufügen
        result.append(prefix).append(" ").append(message);
        
        return result.toString();
    }
    
    /**
     * Gibt den aktuellen Zeitstempel zurück
     */
    private String getTimeString() {
        return BRIGHT_BLACK + "[" + LocalDateTime.now().format(TIME_FORMATTER) + "]" + RESET;
    }
    
    /**
     * Gibt eine Fehlerbenachrichtigung aus
     * 
     * @param message Die Nachricht
     */
    public void error(String message) {
        String symbol = useUnicodeSymbols ? ERROR_SYMBOL + " " : "";
        if (useColors) {
            logger.severe(formatWithPrefix(symbol + RED + "FEHLER: " + message + RESET));
        } else {
            logger.severe(formatWithPrefix(symbol + "FEHLER: " + message));
        }
    }
    
    /**
     * Gibt eine Warnungsbenachrichtigung aus
     * 
     * @param message Die Nachricht
     */
    public void warning(String message) {
        String symbol = useUnicodeSymbols ? WARNING_SYMBOL + " " : "";
        if (useColors) {
            logger.warning(formatWithPrefix(symbol + YELLOW + "WARNUNG: " + message + RESET));
        } else {
            logger.warning(formatWithPrefix(symbol + "WARNUNG: " + message));
        }
    }
    
    /**
     * Gibt eine Informationsbenachrichtigung aus
     * 
     * @param message Die Nachricht
     */
    public void info(String message) {
        String symbol = useUnicodeSymbols ? INFO_SYMBOL + " " : "";
        if (useColors) {
            logger.info(formatWithPrefix(symbol + BRIGHT_WHITE + message + RESET));
        } else {
            logger.info(formatWithPrefix(symbol + message));
        }
    }
    
    /**
     * Gibt eine Debug-Benachrichtigung aus
     * 
     * @param message Die Nachricht
     * @param condition Die Bedingung, unter der die Nachricht ausgegeben wird
     */
    public void debug(String message, boolean condition) {
        if (condition) {
            String symbol = useUnicodeSymbols ? DEBUG_SYMBOL + " " : "";
            if (useColors) {
                logger.info(formatWithPrefix(symbol + BRIGHT_CYAN + "DEBUG: " + message + RESET));
            } else {
                logger.info(formatWithPrefix(symbol + "DEBUG: " + message));
            }
        }
    }
    
    /**
     * Gibt eine wichtige Nachricht aus
     * 
     * @param message Die Nachricht
     */
    public void important(String message) {
        String symbol = useUnicodeSymbols ? IMPORTANT_SYMBOL + " " : "";
        if (useColors) {
            logger.info(formatWithPrefix(symbol + BRIGHT_YELLOW + "WICHTIG: " + message + RESET));
        } else {
            logger.info(formatWithPrefix(symbol + "WICHTIG: " + message));
        }
    }
    
    /**
     * Gibt eine Erfolgsmeldung aus
     * 
     * @param message Die Nachricht
     */
    public void success(String message) {
        String symbol = useUnicodeSymbols ? SUCCESS_SYMBOL + " " : "";
        if (useColors) {
            logger.info(formatWithPrefix(symbol + BRIGHT_GREEN + "ERFOLG: " + message + RESET));
        } else {
            logger.info(formatWithPrefix(symbol + "ERFOLG: " + message));
        }
    }
    
    /**
     * Gibt eine Kategorie-Meldung aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     * @param condition Die Bedingung, unter der die Nachricht ausgegeben wird
     */
    public void categoryDebug(MessageCategory category, String message, boolean condition) {
        if (condition && categoriesEnabledGlobal && isCategoryEnabled(category)) {
        if (useColors) {
                CategoryStyle style = categoryStyles.get(category);
                String icon = useUnicodeSymbols ? style.icon + " " : "";
                String formattedCategory = style.color + "[" + style.code + "] " + RESET;
                
                logger.info(formatWithPrefix(BRIGHT_CYAN + "DEBUG: " + formattedCategory + icon + message + RESET));
        } else {
                String formattedCategory = "[" + category.getCode() + "] ";
                logger.info(formatWithPrefix("DEBUG: " + formattedCategory + message));
            }
        }
    }
    
    /**
     * Gibt eine Kategorie-Meldung (Info) aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     */
    public void categoryInfo(MessageCategory category, String message) {
        if (categoriesEnabledGlobal && isCategoryEnabled(category)) {
            if (useColors) {
        CategoryStyle style = categoryStyles.get(category);
                String icon = useUnicodeSymbols ? style.icon + " " : "";
                String formattedCategory = style.color + "[" + style.code + "] " + RESET;
                
                logger.info(formatWithPrefix(formattedCategory + icon + message + RESET));
        } else {
                String formattedCategory = "[" + category.getCode() + "] ";
            logger.info(formatWithPrefix(formattedCategory + message));
            }
        }
    }
    
    /**
     * Gibt eine Kategorie-Meldung (Erfolg) aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     */
    public void categorySuccess(MessageCategory category, String message) {
        if (categoriesEnabledGlobal && isCategoryEnabled(category)) {
            if (useColors) {
        CategoryStyle style = categoryStyles.get(category);
                String icon = useUnicodeSymbols ? style.icon + " " : "";
                String formattedCategory = style.color + "[" + style.code + "] " + RESET;
                
                logger.info(formatWithPrefix(formattedCategory + icon + BRIGHT_GREEN + message + RESET));
        } else {
                String formattedCategory = "[" + category.getCode() + "] ";
                logger.info(formatWithPrefix(formattedCategory + "ERFOLG: " + message));
            }
        }
    }
    
    /**
     * Gibt eine Kategorie-Meldung (Warnung) aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     */
    public void categoryWarning(MessageCategory category, String message) {
        if (categoriesEnabledGlobal && isCategoryEnabled(category)) {
            if (useColors) {
        CategoryStyle style = categoryStyles.get(category);
                String icon = useUnicodeSymbols ? style.icon + " " : "";
                String formattedCategory = style.color + "[" + style.code + "] " + RESET;
                
                logger.warning(formatWithPrefix(formattedCategory + icon + YELLOW + "WARNUNG: " + message + RESET));
        } else {
                String formattedCategory = "[" + category.getCode() + "] ";
                logger.warning(formatWithPrefix(formattedCategory + "WARNUNG: " + message));
            }
        }
    }
    
    /**
     * Gibt eine Kategorie-Meldung (Fehler) aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     */
    public void categoryError(MessageCategory category, String message) {
        if (categoriesEnabledGlobal && isCategoryEnabled(category)) {
        if (useColors) {
        CategoryStyle style = categoryStyles.get(category);
                String icon = useUnicodeSymbols ? style.icon + " " : "";
                String formattedCategory = style.color + "[" + style.code + "] " + RESET;
                
                logger.severe(formatWithPrefix(formattedCategory + icon + RED + "FEHLER: " + message + RESET));
        } else {
                String formattedCategory = "[" + category.getCode() + "] ";
                logger.severe(formatWithPrefix(formattedCategory + "FEHLER: " + message));
            }
        }
    }
    
    /**
     * Gibt einen Titel aus
     * 
     * @param title Der Titel
     */
    public void title(String title) {
        if (useColors) {
            String line = "═".repeat(Math.max(20, title.length() + 4));
            logger.info("");
            logger.info(BRIGHT_CYAN + "╔" + line + "╗" + RESET);
            logger.info(BRIGHT_CYAN + "║" + " ".repeat((line.length() - title.length()) / 2) + 
                     BRIGHT_WHITE + BOLD + title + RESET + BRIGHT_CYAN + 
                     " ".repeat((line.length() - title.length() + 1) / 2) + "║" + RESET);
            logger.info(BRIGHT_CYAN + "╚" + line + "╝" + RESET);
            logger.info("");
        } else {
            String line = "=".repeat(Math.max(20, title.length() + 4));
            logger.info("");
            logger.info(line);
            logger.info("|" + " ".repeat((line.length() - title.length()) / 2) + 
                     title + " ".repeat((line.length() - title.length() + 1) / 2) + "|");
            logger.info(line);
            logger.info("");
        }
    }
    
    /**
     * Kategorie-Einstellungen
     */
    public enum MessageCategory {
        SYSTEM(BRIGHT_CYAN, "SYS", "⚡"),
        CONFIG(BRIGHT_BLUE, "CFG", "⚙"),
        MODULE(BRIGHT_GREEN, "MOD", "📦"),
        PERFORMANCE(BRIGHT_YELLOW, "PERF", "📊"),
        SECURITY(BRIGHT_RED, "SEC", "🔒"),
        NETWORK(BRIGHT_PURPLE, "NET", "🌐"),
        DATABASE(BRIGHT_WHITE, "DB", "💾"),
        USER(BLUE, "USER", "👤"),
        THREAD(YELLOW, "THREAD", "🧵"),
        RESOURCE(GREEN, "RES", "📄"),
        UNKNOWN(WHITE, "", "");
        
        private final String color;
        private final String code;
        private final String icon;
        
        MessageCategory(String color, String code, String icon) {
            this.color = color;
            this.code = code;
            this.icon = icon;
        }
        
        public String getColor() {
            return color;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getIcon() {
            return icon;
        }
    }
    
    /**
     * Speichert Stil-Informationen für eine Kategorie
     */
    private static class CategoryStyle {
        final String color;
        final String code;
        final String icon;
        
        CategoryStyle(String color, String code, String icon) {
            this.color = color;
            this.code = code;
            this.icon = icon;
        }
    }
    
    /**
     * Prüft, ob eine Kategorie aktiviert ist
     * 
     * @param category Die zu prüfende Kategorie
     * @return true, wenn die Kategorie aktiviert ist
     */
    public boolean isCategoryEnabled(MessageCategory category) {
        if (!categoriesEnabledGlobal) return false;
        
        CategoryConfig config = categoryConfigs.get(category);
        return config == null || config.isEnabled();
    }
    
    /**
     * Holt den konfigurierten Kategorie-Präfix
     * 
     * @param category Die Kategorie
     * @return Der konfigurierte Präfix oder der Standard-Präfix
     */
    public String getCategoryPrefix(MessageCategory category) {
        CategoryConfig config = categoryConfigs.get(category);
        return config != null ? config.getPrefix() : category.getCode();
    }
    
    /**
     * Holt den konfigurierten Kategorie-Farbcode
     * 
     * @param category Die Kategorie
     * @return Der konfigurierte Farbcode oder der Standard-Farbcode
     */
    public String getCategoryColor(MessageCategory category) {
        CategoryConfig config = categoryConfigs.get(category);
        return config != null ? config.getColor() : category.getColor();
    }
    
    /**
     * Holt das konfigurierte Kategorie-Icon
     * 
     * @param category Die Kategorie
     * @return Das konfigurierte Icon oder das Standard-Icon
     */
    public String getCategoryIcon(MessageCategory category) {
        CategoryConfig config = categoryConfigs.get(category);
        return config != null ? config.getIcon() : category.getIcon();
    }
    
    /**
     * Lädt die Kategorie-Konfigurationen aus der config.yml
     * 
     * @param apiCore Die ApiCore-Instanz
     */
    public void loadCategoryConfigs(ApiCore apiCore) {
        if (apiCore == null || apiCore.getConfig() == null) return;
        
        // Globale Einstellung
        categoriesEnabledGlobal = apiCore.getConfig().getBoolean("console.categories.enable-all", true);
        
        // Lade Einstellungen für jede Kategorie
        for (MessageCategory category : MessageCategory.values()) {
            String path = "console.categories." + category.name().toLowerCase();
            if (!apiCore.getConfig().isConfigurationSection(path)) continue;
            
            boolean enabled = apiCore.getConfig().getBoolean(path + ".enabled", true);
            String prefix = apiCore.getConfig().getString(path + ".prefix", category.getCode());
            String colorStr = apiCore.getConfig().getString(path + ".color", "WHITE");
            String icon = apiCore.getConfig().getString(path + ".icon", category.getIcon());
            
            String colorCode = getColorCodeByName(colorStr);
            
            categoryConfigs.put(category, new CategoryConfig(enabled, prefix, colorCode, icon));
        }
    }
    
    /**
     * Konvertiert einen Farbnamen in den entsprechenden ANSI-Farbcode
     * 
     * @param colorName Name der Farbe (z.B. BRIGHT_CYAN)
     * @return ANSI-Farbcode oder WHITE, wenn die Farbe nicht gefunden wurde
     */
    private String getColorCodeByName(String colorName) {
        if (colorName == null) return WHITE;
        
        try {
            // Versuche, das Farbfeld über Reflection zu bekommen
            Field field = ConsoleFormatter.class.getDeclaredField(colorName);
            return (String) field.get(null);
        } catch (Exception e) {
            // Fallback: Standard-Weiß
            return WHITE;
        }
    }
    
    // Fehlende Methoden für ApiCore und andere Klassen
    
    /**
     * Erstellt einen Header mit hervorgehobenem Text
     */
    public void header(String text) {
        String line = BRIGHT_CYAN + "═".repeat(Math.max(50, text.length() + 10)) + RESET;
        log(line);
        log(BRIGHT_CYAN + "  " + BOLD + text + RESET);
        log(line);
    }
    
    /**
     * Fügt eine Leerzeile hinzu
     */
    public void blank() {
        log("");
    }
    
    /**
     * Erstellt eine doppelte Linie
     */
    public void doubleLine() {
        log(BRIGHT_CYAN + "═".repeat(60) + RESET);
    }
    
    /**
     * Erstellt einen Subheader
     */
    public void subHeader(String text) {
        log(BRIGHT_YELLOW + "▶ " + BOLD + text + RESET);
        log(BRIGHT_YELLOW + "─".repeat(Math.max(30, text.length() + 5)) + RESET);
    }
    
    /**
     * Erstellt eine Sektion
     */
    public void section(String text) {
        log(BRIGHT_GREEN + "┌─ " + BOLD + text + RESET);
    }
    
    /**
     * Erstellt einen Listeneintrag
     */
    public void listItem(String marker, String text) {
        log(BRIGHT_WHITE + "  " + marker + " " + RESET + text);
    }
    
    /**
     * Erstellt eine einfache Fortschrittsanzeige
     */
    public void progressBar(int current, int total, int width) {
        if (total <= 0) return;
        
        int filledWidth = (int) ((double) current / total * width);
        String filled = "█".repeat(filledWidth);
        String empty = "░".repeat(width - filledWidth);
        
        int percentage = (int) ((double) current / total * 100);
        
        log(BRIGHT_CYAN + "[" + BRIGHT_GREEN + filled + BRIGHT_BLACK + empty + BRIGHT_CYAN + "] " + 
            BRIGHT_WHITE + percentage + "%" + RESET);
    }
    
    /**
     * Erstellt eine farbige Fortschrittsanzeige mit Text
     */
    public void colorProgressBar(int current, int total, int width, String statusText) {
        if (total <= 0) return;
        
        int filledWidth = (int) ((double) current / total * width);
        String filled = "█".repeat(filledWidth);
        String empty = "░".repeat(width - filledWidth);
        
        // Farbauswahl basierend auf Fortschritt
        String color = current >= total ? BRIGHT_GREEN : 
                      current >= total * 0.7 ? BRIGHT_YELLOW : BRIGHT_RED;
        
        log(BRIGHT_CYAN + "[" + color + filled + BRIGHT_BLACK + empty + BRIGHT_CYAN + "] " + 
            BRIGHT_WHITE + statusText + RESET);
    }
    
    /**
     * Erstellt einen einfachen Schritt-Indikator
     * 
     * @param current Der aktuelle Schritt
     * @param total Die Gesamtanzahl der Schritte
     * @param text Der Beschreibungstext
     */
    public void step(int current, int total, String text) {
        if (useColors) {
            String prefix = BRIGHT_CYAN + "[" + current + "/" + total + "] " + RESET;
            logger.info(formatWithPrefix(prefix + text));
        } else {
            logger.info(formatWithPrefix("[" + current + "/" + total + "] " + text));
        }
    }
    
    /**
     * Zeigt eine Status-Nachricht mit Erfolg oder Fehler an
     * 
     * @param statusType Der Status-Typ (z.B. "OK", "FEHLER")
     * @param message Die Nachricht
     * @param success Ob es ein Erfolg oder Fehler ist
     */
    public void status(String statusType, String message, boolean success) {
        String symbol = useUnicodeSymbols ? (success ? SUCCESS_SYMBOL : ERROR_SYMBOL) + " " : "";
        if (useColors) {
            String color = success ? BRIGHT_GREEN : BRIGHT_RED;
            logger.info(formatWithPrefix(symbol + color + statusType + ": " + message + RESET));
        } else {
            logger.info(formatWithPrefix(symbol + statusType + ": " + message));
        }
    }

    /**
     * Erstellt einen detaillierten Schritt mit Status
     */
    public void richStep(int current, int total, String text, boolean success) {
        String status = success ? BRIGHT_GREEN + "✓" : BRIGHT_RED + "✗";
        String prefix = BRIGHT_CYAN + "[" + current + "/" + total + "] " + status + " ";
        log(prefix + RESET + text);
    }
    
    /**
     * Erstellt einen Textblock mit Rahmen
     */
    public void textBlock(String title, String content, boolean success) {
        String color = success ? BRIGHT_GREEN : BRIGHT_RED;
        String icon = success ? "✓" : "✗";
        
        log(color + "┌─ " + icon + " " + BOLD + title + RESET);
        
        // Content in Zeilen aufteilen und einrücken
        String[] lines = content.split("\n");
        for (String line : lines) {
            log(color + "│ " + RESET + line);
        }
        
        log(color + "└" + "─".repeat(Math.max(30, title.length() + 10)) + RESET);
    }
}
