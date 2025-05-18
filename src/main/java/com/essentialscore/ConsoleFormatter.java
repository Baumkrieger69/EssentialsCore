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
    @SuppressWarnings("unused")
    private final String stylePreset;
    
    // Konfiguration für die Kategorien
    private boolean categoriesEnabledGlobal = true;
    private final Map<MessageCategory, CategoryConfig> categoryConfigs = new EnumMap<>(MessageCategory.class);
    
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
    
    // Kategorien für Nachrichten
    public enum MessageCategory {
        SYSTEM(BRIGHT_CYAN, "SYS", "⚡"),
        CONFIG(BRIGHT_BLUE, "CFG", "⚙"),
        MODULE(BRIGHT_GREEN, "MOD", "📦"),
        PERFORMANCE(BRIGHT_YELLOW, "PERF", "⚡"),
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
                            boolean showTimestamp, boolean useUnicodeSymbols, 
                            String stylePreset) {
        this.logger = logger;
        this.prefix = prefix;
        this.useColors = useColors;
        this.showTimestamp = showTimestamp;
        this.useUnicodeSymbols = useUnicodeSymbols;
        this.stylePreset = stylePreset;
    }
    
    /**
     * Gibt eine Info-Nachricht aus
     * 
     * @param message Die Nachricht
     */
    public void info(String message) {
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? INFO_SYMBOL + " " : "";
            logger.info(timeStr + BRIGHT_CYAN + prefix + RESET + " " + CYAN + symbol + WHITE + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? INFO_SYMBOL + " " : "";
            logger.info(timeStr + prefix + " " + symbol + message);
        }
    }
    
    /**
     * Gibt eine Erfolgsnachricht aus
     * 
     * @param message Die Nachricht
     */
    public void success(String message) {
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? SUCCESS_SYMBOL + " " : "";
            logger.info(timeStr + BRIGHT_GREEN + prefix + RESET + " " + GREEN + symbol + WHITE + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? SUCCESS_SYMBOL + " " : "";
            logger.info(timeStr + prefix + " " + symbol + message);
        }
    }
    
    /**
     * Gibt eine Warnmeldung aus
     * 
     * @param message Die Nachricht
     */
    public void warning(String message) {
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? WARNING_SYMBOL + " " : "";
            logger.warning(timeStr + BRIGHT_YELLOW + prefix + RESET + " " + YELLOW + symbol + WHITE + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? WARNING_SYMBOL + " " : "";
            logger.warning(timeStr + prefix + " " + symbol + message);
        }
    }
    
    /**
     * Gibt eine Fehlermeldung aus
     * 
     * @param message Die Nachricht
     */
    public void error(String message) {
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? ERROR_SYMBOL + " " : "";
            logger.severe(timeStr + BRIGHT_RED + prefix + RESET + " " + RED + symbol + WHITE + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? ERROR_SYMBOL + " " : "";
            logger.severe(timeStr + prefix + " " + symbol + message);
        }
    }
    
    /**
     * Gibt eine wichtige Nachricht hervorgehoben aus
     * 
     * @param message Die Nachricht
     */
    public void important(String message) {
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? IMPORTANT_SYMBOL + " " : "";
            logger.info(timeStr + BRIGHT_YELLOW + prefix + RESET + " " + 
                     BOLD + BG_YELLOW + BLACK + " " + symbol + message + " " + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? IMPORTANT_SYMBOL + " " : "";
            logger.info(timeStr + prefix + " [" + symbol + "WICHTIG] " + message);
        }
    }
    
    /**
     * Gibt eine Debug-Nachricht aus (nur wenn Debug-Modus aktiviert)
     * 
     * @param message Die Nachricht
     * @param isDebugMode Ob der Debug-Modus aktiviert ist
     */
    public void debug(String message, boolean isDebugMode) {
        if (!isDebugMode) return;
        
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? DEBUG_SYMBOL + " " : "";
            logger.info(timeStr + BRIGHT_PURPLE + prefix + " [DEBUG] " + RESET + PURPLE + symbol + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String symbol = useUnicodeSymbols ? DEBUG_SYMBOL + " " : "";
            logger.info(timeStr + prefix + " [DEBUG] " + symbol + message);
        }
    }
    
    /**
     * Gibt eine Überschrift mit Trennlinien und Rahmen aus
     * 
     * @param title Der Titel
     */
    public void header(String title) {
        if (useColors) {
            String line = "═".repeat(Math.max(20, title.length() + 4));
            logger.info(BRIGHT_CYAN + "╔" + line + "╗" + RESET);
            logger.info(BRIGHT_CYAN + "║" + " ".repeat((line.length() - title.length()) / 2) + 
                     BRIGHT_WHITE + BOLD + title + RESET + BRIGHT_CYAN + 
                     " ".repeat((line.length() - title.length() + 1) / 2) + "║" + RESET);
            logger.info(BRIGHT_CYAN + "╚" + line + "╝" + RESET);
        } else {
            String line = "=".repeat(Math.max(20, title.length() + 4));
            logger.info(line);
            logger.info("|" + " ".repeat((line.length() - title.length()) / 2) + 
                     title + " ".repeat((line.length() - title.length() + 1) / 2) + "|");
            logger.info(line);
        }
    }
    
    /**
     * Gibt eine kleine Überschrift ohne Rahmen aus
     * 
     * @param title Der Titel
     */
    public void subHeader(String title) {
        if (useColors) {
            logger.info(BRIGHT_CYAN + "◈ " + BRIGHT_WHITE + BOLD + title + RESET + 
                     BRIGHT_CYAN + " " + "─".repeat(Math.max(10, 30 - title.length())) + RESET);
        } else {
            logger.info("-- " + title + " " + "-".repeat(Math.max(10, 30 - title.length())));
        }
    }
    
    /**
     * Gibt eine Abschnittsüberschrift aus
     * 
     * @param section Der Abschnittstitel
     */
    public void section(String section) {
        if (useColors) {
            logger.info(BRIGHT_BLUE + "► " + BOLD + BRIGHT_WHITE + section + RESET);
        } else {
            logger.info("► " + section);
        }
    }
    
    /**
     * Gibt einen Listeneintrag mit Einrückung aus
     * 
     * @param key Der Schlüssel oder Name
     * @param value Der Wert
     */
    public void listItem(String key, String value) {
        if (useColors) {
            logger.info("  " + BRIGHT_GREEN + "• " + BRIGHT_WHITE + key + RESET + ": " + WHITE + value + RESET);
        } else {
            logger.info("  • " + key + ": " + value);
        }
    }
    
    /**
     * Gibt eine strukturierte Nachricht mit Key-Value-Paaren aus
     * 
     * @param title Der Titel der Sektion
     * @param data Key-Value-Paare als abwechselnde Strings (key1, value1, key2, value2, ...)
     */
    public void dataSection(String title, String... data) {
        section(title);
        
        for (int i = 0; i < data.length - 1; i += 2) {
            listItem(data[i], data[i + 1]);
        }
    }
    
    /**
     * Gibt eine leere Zeile aus
     */
    public void blank() {
        logger.info("");
    }
    
    /**
     * Gibt eine horizontale Linie aus
     */
    public void line() {
        if (useColors) {
            logger.info(BRIGHT_BLACK + "──────────────────────────────────────────────────────" + RESET);
        } else {
            logger.info("--------------------------------------------------");
        }
    }
    
    /**
     * Gibt eine doppelte horizontale Linie aus
     */
    public void doubleLine() {
        if (useColors) {
            logger.info(BRIGHT_BLACK + "══════════════════════════════════════════════════════" + RESET);
        } else {
            logger.info("=================================================");
        }
    }
    
    /**
     * Gibt einen formatierten Fortschrittsbalken aus
     * 
     * @param current Aktueller Wert
     * @param max Maximaler Wert
     * @param length Länge des Balkens
     */
    public void progressBar(int current, int max, int length) {
        int progressChars = (int) ((double) current / max * length);
        StringBuilder bar = new StringBuilder("[");
        
        for (int i = 0; i < length; i++) {
            if (i < progressChars) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        
        double percentage = (double) current / max * 100;
        
        if (useColors) {
            logger.info(BRIGHT_CYAN + bar.toString() + RESET + " " + 
                     BRIGHT_WHITE + String.format("%.1f%%", percentage) + RESET + " " +
                     BRIGHT_BLACK + "(" + current + "/" + max + ")" + RESET);
        } else {
            logger.info(bar.toString() + " " + String.format("%.1f%%", percentage) + 
                     " (" + current + "/" + max + ")");
        }
    }
    
    /**
     * Gibt einen farbigen Fortschrittsbalken aus, dessen Farbe sich basierend auf dem Fortschritt ändert
     * 
     * @param current Aktueller Wert
     * @param max Maximaler Wert
     * @param length Länge des Balkens
     * @param label Beschriftung für den Fortschrittsbalken
     */
    public void colorProgressBar(int current, int max, int length, String label) {
        int progressChars = (int) ((double) current / max * length);
        double percentage = (double) current / max * 100;
        StringBuilder bar = new StringBuilder();
        
        // Balkenfarbe basierend auf Fortschritt
        String barColor;
        if (percentage < 30) {
            barColor = RED;
        } else if (percentage < 70) {
            barColor = YELLOW;
        } else {
            barColor = GREEN;
        }
        
        if (useColors) {
            bar.append(BRIGHT_WHITE).append("[").append(RESET);
            
            for (int i = 0; i < length; i++) {
                if (i < progressChars) {
                    bar.append(barColor).append("█").append(RESET);
                } else {
                    bar.append(BRIGHT_BLACK).append("▒").append(RESET);
                }
            }
            
            bar.append(BRIGHT_WHITE).append("]").append(RESET);
            
            logger.info(bar.toString() + " " + 
                     BRIGHT_WHITE + String.format("%.1f%%", percentage) + RESET + " " +
                     (label != null ? BRIGHT_BLACK + label + RESET : ""));
        } else {
            bar.append("[");
            
            for (int i = 0; i < length; i++) {
                if (i < progressChars) {
                    bar.append("#");
                } else {
                    bar.append("-");
                }
            }
            
            bar.append("]");
            
            logger.info(bar.toString() + " " + String.format("%.1f%%", percentage) + 
                     (label != null ? " " + label : ""));
        }
    }
    
    /**
     * Gibt eine schrittweise Information aus
     * 
     * @param step Die Schrittnummer
     * @param total Die Gesamtzahl der Schritte
     * @param description Die Beschreibung
     */
    public void step(int step, int total, String description) {
        if (useColors) {
            logger.info(BRIGHT_BLUE + "(" + step + "/" + total + ")" + RESET + " " + WHITE + description + RESET);
        } else {
            logger.info("(" + step + "/" + total + ") " + description);
        }
    }
    
    /**
     * Gibt einen formatierten Schritt mit Icon aus
     * 
     * @param step Die Schrittnummer
     * @param total Die Gesamtzahl der Schritte
     * @param description Die Beschreibung
     * @param isSuccess Ob der Schritt erfolgreich war
     */
    public void richStep(int step, int total, String description, boolean isSuccess) {
        String icon = isSuccess ? (useUnicodeSymbols ? "✓ " : "") : (useUnicodeSymbols ? "× " : "");
        String color = isSuccess ? GREEN : RED;
        String brightColor = isSuccess ? BRIGHT_GREEN : BRIGHT_RED;
        
        if (useColors) {
            double percentage = (double) step / total * 100;
            String progress = String.format("%.0f%%", percentage);
            
            logger.info(brightColor + "[" + step + "/" + total + "] " + progress + RESET + " " + 
                     color + icon + WHITE + description + RESET);
        } else {
            logger.info("[" + step + "/" + total + "] " + icon + description);
        }
    }
    
    /**
     * Zeigt eine Statusmeldung mit Farbkodierung basierend auf dem Status an
     * 
     * @param label Die Bezeichnung des Status
     * @param message Die eigentliche Nachricht
     * @param success Der Status (true = Erfolg, false = Fehler, null = neutral)
     */
    public void status(String label, String message, Boolean success) {
        String statusSymbol;
        String statusColor;
        
        if (success == null) {
            // Neutraler Status
            statusSymbol = useUnicodeSymbols ? "○" : "-";
            statusColor = RESET;
        } else if (success) {
            // Erfolgreicher Status
            statusSymbol = useUnicodeSymbols ? "✓" : "+";
            statusColor = GREEN;
        } else {
            // Fehlerhafter Status
            statusSymbol = useUnicodeSymbols ? "✗" : "x";
            statusColor = RED;
        }
        
        logger.info(formatWithPrefix(CYAN + "[" + statusColor + statusSymbol + CYAN + "] " + 
                      WHITE + label + ": " + statusColor + message + RESET));
    }
    
    /**
     * Gibt einen formatierten Block mit Text aus
     * 
     * @param title Der Titel des Blocks
     * @param content Der Inhalt des Blocks
     * @param isImportant Ob der Block wichtig ist
     */
    public void textBlock(String title, String content, boolean isImportant) {
        if (useColors) {
            String titleBg = isImportant ? BG_BRIGHT_YELLOW : BG_BRIGHT_BLUE;
            String titleFg = isImportant ? BLACK : WHITE;
            String contentBg = isImportant ? BG_BLACK : "";
            String contentFg = isImportant ? BRIGHT_YELLOW : WHITE;
            
            logger.info(titleBg + " " + BOLD + titleFg + title + " " + RESET);
            
            if (content.contains("\n")) {
                for (String line : content.split("\n")) {
                    logger.info((contentBg.isEmpty() ? "" : contentBg + " ") + 
                             contentFg + line + RESET);
                }
            } else {
                logger.info((contentBg.isEmpty() ? "" : contentBg + " ") + 
                         contentFg + content + RESET);
            }
        } else {
            logger.info("--- " + title + " ---");
            logger.info(content);
        }
    }
    
    /**
     * Gibt eine Tabelle mit Spaltenüberschriften aus
     * 
     * @param headers Die Spaltenüberschriften
     */
    public void tableHeader(String... headers) {
        if (useColors) {
            StringBuilder sb = new StringBuilder();
            for (String header : headers) {
                sb.append(BRIGHT_WHITE).append(BOLD).append(header).append(RESET).append("\t");
            }
            logger.info(sb.toString());
            
            // Trennlinie
            sb = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                sb.append(BRIGHT_BLACK).append("───────────").append(RESET).append("\t");
            }
            logger.info(sb.toString());
        } else {
            StringBuilder sb = new StringBuilder();
            for (String header : headers) {
                sb.append(header).append("\t");
            }
            logger.info(sb.toString());
            
            // Trennlinie
            sb = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                sb.append("----------").append("\t");
            }
            logger.info(sb.toString());
        }
    }
    
    /**
     * Gibt eine Tabellenzeile aus
     * 
     * @param cells Die Zelleninhalte
     */
    public void tableRow(String... cells) {
        if (useColors) {
            StringBuilder sb = new StringBuilder();
            for (String cell : cells) {
                sb.append(WHITE).append(cell).append(RESET).append("\t");
            }
            logger.info(sb.toString());
        } else {
            StringBuilder sb = new StringBuilder();
            for (String cell : cells) {
                sb.append(cell).append("\t");
            }
            logger.info(sb.toString());
        }
    }
    
    /**
     * Gibt eine Box mit Text aus
     * 
     * @param text Der Text
     * @param boxWidth Die Breite der Box
     */
    public void box(String text, int boxWidth) {
        if (boxWidth < 10) boxWidth = 10;
        if (useColors) {
            String topLine = "╭" + "─".repeat(boxWidth - 2) + "╮";
            String bottomLine = "╰" + "─".repeat(boxWidth - 2) + "╯";
            String middle = text.length() > boxWidth - 4 
                ? text.substring(0, boxWidth - 7) + "..." 
                : text + " ".repeat(boxWidth - text.length() - 4);
            
            logger.info(BRIGHT_CYAN + topLine + RESET);
            logger.info(BRIGHT_CYAN + "│ " + RESET + WHITE + middle + RESET + BRIGHT_CYAN + " │" + RESET);
            logger.info(BRIGHT_CYAN + bottomLine + RESET);
        } else {
            String topLine = "+" + "-".repeat(boxWidth - 2) + "+";
            String bottomLine = "+" + "-".repeat(boxWidth - 2) + "+";
            String middle = text.length() > boxWidth - 4 
                ? text.substring(0, boxWidth - 7) + "..." 
                : text + " ".repeat(boxWidth - text.length() - 4);
            
            logger.info(topLine);
            logger.info("| " + middle + " |");
            logger.info(bottomLine);
        }
    }
    
    /**
     * Gibt den aktuellen Zeitstempel zurück
     */
    private String getTimeString() {
        return BRIGHT_BLACK + "[" + LocalDateTime.now().format(TIME_FORMATTER) + "]" + RESET;
    }
    
    /**
     * Formatiert einen String mit dem konfigurierten Prefix
     * 
     * @param message Die zu formatierende Nachricht
     * @return Die Nachricht mit Prefix und optionalem Timestamp
     */
    private String formatWithPrefix(String message) {
        StringBuilder result = new StringBuilder();
        
        // Timestamp hinzufügen, falls aktiviert
        if (showTimestamp) {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            result.append(BRIGHT_BLACK).append("[").append(timestamp).append("] ").append(RESET);
        }
        
        // Prefix hinzufügen
        if (prefix != null && !prefix.isEmpty()) {
            // Ersetze & mit ANSI-Farbcodes
            String formattedPrefix = formatHexCodes(prefix);
            result.append(formattedPrefix).append(" ");
        }
        
        // Nachricht hinzufügen
        result.append(message);
        
        return result.toString();
    }
    
    /**
     * Konvertiert Minecraft-Style Farbcodes (&a, &b, etc.) in ANSI-Farbcodes
     * 
     * @param input Der Text mit Minecraft-Farbcodes
     * @return Der Text mit ANSI-Farbcodes
     */
    private String formatHexCodes(String input) {
        if (input == null) return "";
        
        // Farbcode-Map
        java.util.Map<Character, String> colorMap = new java.util.HashMap<>();
        colorMap.put('0', BLACK);
        colorMap.put('1', BLUE);
        colorMap.put('2', GREEN);
        colorMap.put('3', CYAN);
        colorMap.put('4', RED);
        colorMap.put('5', PURPLE);
        colorMap.put('6', YELLOW);
        colorMap.put('7', WHITE);
        colorMap.put('8', BRIGHT_BLACK);
        colorMap.put('9', BRIGHT_BLUE);
        colorMap.put('a', BRIGHT_GREEN);
        colorMap.put('b', BRIGHT_CYAN);
        colorMap.put('c', BRIGHT_RED);
        colorMap.put('d', BRIGHT_PURPLE);
        colorMap.put('e', BRIGHT_YELLOW);
        colorMap.put('f', BRIGHT_WHITE);
        colorMap.put('r', RESET);
        colorMap.put('l', BOLD);
        colorMap.put('n', UNDERLINE);
        
        StringBuilder result = new StringBuilder();
        boolean skipNext = false;
        
        for (int i = 0; i < input.length(); i++) {
            if (skipNext) {
                skipNext = false;
                continue;
            }
            
            char current = input.charAt(i);
            
            if (current == '&' && i + 1 < input.length()) {
                char colorCode = Character.toLowerCase(input.charAt(i + 1));
                String ansiCode = colorMap.get(colorCode);
                
                if (ansiCode != null) {
                    result.append(ansiCode);
                    skipNext = true;
                } else {
                    result.append(current);
                }
            } else {
                result.append(current);
            }
        }
        
        // Stelle sicher, dass der Text mit Reset endet
        result.append(RESET);
        
        return result.toString();
    }
    
    /**
     * Gibt eine kategorisierte Info-Nachricht aus
     * 
     * @param category Die Nachrichtenkategorie
     * @param message Die Nachricht
     */
    public void categoryInfo(MessageCategory category, String message) {
        if (!isCategoryEnabled(category)) return;
        
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String categoryColor = getCategoryColor(category);
            String categoryIcon = useUnicodeSymbols ? getCategoryIcon(category) + " " : "";
            String categoryStr = "[" + categoryColor + categoryPrefix + RESET + "] ";
            logger.info(timeStr + BRIGHT_CYAN + prefix + RESET + " " + categoryStr + 
                     categoryColor + categoryIcon + WHITE + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String categoryIcon = useUnicodeSymbols ? getCategoryIcon(category) + " " : "";
            logger.info(timeStr + prefix + " [" + categoryPrefix + "] " + categoryIcon + message);
        }
    }
    
    /**
     * Gibt eine kategorisierte Erfolgsnachricht aus
     * 
     * @param category Die Nachrichtenkategorie
     * @param message Die Nachricht
     */
    public void categorySuccess(MessageCategory category, String message) {
        if (!isCategoryEnabled(category)) return;
        
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String categoryColor = getCategoryColor(category);
            String symbol = useUnicodeSymbols ? SUCCESS_SYMBOL + " " : "";
            String categoryStr = "[" + categoryColor + categoryPrefix + RESET + "] ";
            logger.info(timeStr + BRIGHT_GREEN + prefix + RESET + " " + categoryStr + 
                     GREEN + symbol + WHITE + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String symbol = useUnicodeSymbols ? SUCCESS_SYMBOL + " " : "";
            logger.info(timeStr + prefix + " [" + categoryPrefix + "] " + symbol + message);
        }
    }
    
    /**
     * Gibt eine kategorisierte Warnmeldung aus
     * 
     * @param category Die Nachrichtenkategorie
     * @param message Die Nachricht
     */
    public void categoryWarning(MessageCategory category, String message) {
        if (!isCategoryEnabled(category)) return;
        
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String categoryColor = getCategoryColor(category);
            String symbol = useUnicodeSymbols ? WARNING_SYMBOL + " " : "";
            String categoryStr = "[" + categoryColor + categoryPrefix + RESET + "] ";
            logger.warning(timeStr + BRIGHT_YELLOW + prefix + RESET + " " + categoryStr + 
                       YELLOW + symbol + WHITE + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String symbol = useUnicodeSymbols ? WARNING_SYMBOL + " " : "";
            logger.warning(timeStr + prefix + " [" + categoryPrefix + "] " + symbol + message);
        }
    }
    
    /**
     * Gibt eine kategorisierte Fehlermeldung aus
     * 
     * @param category Die Nachrichtenkategorie
     * @param message Die Nachricht
     */
    public void categoryError(MessageCategory category, String message) {
        if (!isCategoryEnabled(category)) return;
        
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String categoryColor = getCategoryColor(category);
            String symbol = useUnicodeSymbols ? ERROR_SYMBOL + " " : "";
            String categoryStr = "[" + categoryColor + categoryPrefix + RESET + "] ";
            logger.severe(timeStr + BRIGHT_RED + prefix + RESET + " " + categoryStr + 
                      RED + symbol + WHITE + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String symbol = useUnicodeSymbols ? ERROR_SYMBOL + " " : "";
            logger.severe(timeStr + prefix + " [" + categoryPrefix + "] " + symbol + message);
        }
    }
    
    /**
     * Gibt eine kategorisierte Debug-Nachricht aus (nur wenn Debug-Modus aktiviert)
     * 
     * @param category Die Nachrichtenkategorie
     * @param message Die Nachricht
     * @param isDebugMode Ob der Debug-Modus aktiviert ist
     */
    public void categoryDebug(MessageCategory category, String message, boolean isDebugMode) {
        if (!isDebugMode || !isCategoryEnabled(category)) return;
        
        if (useColors) {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String categoryColor = getCategoryColor(category);
            String symbol = useUnicodeSymbols ? DEBUG_SYMBOL + " " : "";
            String categoryStr = "[" + categoryColor + categoryPrefix + RESET + "] ";
            logger.info(timeStr + BRIGHT_PURPLE + prefix + " [DEBUG] " + RESET + categoryStr + 
                     PURPLE + symbol + message + RESET);
        } else {
            String timeStr = showTimestamp ? getTimeString() + " " : "";
            String categoryPrefix = getCategoryPrefix(category);
            String symbol = useUnicodeSymbols ? DEBUG_SYMBOL + " " : "";
            logger.info(timeStr + prefix + " [DEBUG] [" + categoryPrefix + "] " + symbol + message);
        }
    }
    
    /**
     * Gibt einen kategorisierten Tabellenheader aus
     * 
     * @param category Die Nachrichtenkategorie
     * @param headers Die Tabellenspalten
     */
    public void categoryTableHeader(MessageCategory category, String... headers) {
        if (useColors) {
            String categoryStr = "[" + category.getColor() + category.getCode() + RESET + "] ";
            String headerStr = BRIGHT_WHITE + BOLD;
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) headerStr += " | ";
                headerStr += headers[i];
            }
            logger.info(CYAN + prefix + RESET + " " + categoryStr + headerStr + RESET);
            
            String dividerStr = "";
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) dividerStr += "─┼─";
                dividerStr += "─".repeat(headers[i].length());
            }
            logger.info(CYAN + prefix + RESET + " " + categoryStr + CYAN + dividerStr + RESET);
        } else {
            String headerStr = "";
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) headerStr += " | ";
                headerStr += headers[i];
            }
            logger.info(prefix + " [" + category.getCode() + "] " + headerStr);
            
            String dividerStr = "";
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) dividerStr += "-+-";
                dividerStr += "-".repeat(headers[i].length());
            }
            logger.info(prefix + " [" + category.getCode() + "] " + dividerStr);
        }
    }
    
    /**
     * Gibt eine kategorisierte Tabellenzeile aus
     * 
     * @param category Die Nachrichtenkategorie
     * @param cells Die Tabellenzellen
     */
    public void categoryTableRow(MessageCategory category, String... cells) {
        if (useColors) {
            String categoryStr = "[" + category.getColor() + category.getCode() + RESET + "] ";
            String rowStr = WHITE;
            for (int i = 0; i < cells.length; i++) {
                if (i > 0) rowStr += CYAN + " | " + WHITE;
                rowStr += cells[i];
            }
            logger.info(CYAN + prefix + RESET + " " + categoryStr + rowStr + RESET);
        } else {
            String rowStr = "";
            for (int i = 0; i < cells.length; i++) {
                if (i > 0) rowStr += " | ";
                rowStr += cells[i];
            }
            logger.info(prefix + " [" + category.getCode() + "] " + rowStr);
        }
    }
} 