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
                          boolean showTimestamp, boolean useUnicodeSymbols, String stylePreset) {
        this.logger = logger;
        this.prefix = colorizePrefix(prefix);
        this.useColors = useColors;
        this.showTimestamp = showTimestamp;
        this.useUnicodeSymbols = useUnicodeSymbols;
        this.stylePreset = stylePreset;
        
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
     * Wandelt einen Rohpräfix in einen formatierten Präfix um
     * Verarbeitet & Farbcodes ähnlich wie Bukkit/Spigot
     * 
     * @param rawPrefix Das Roh-Präfix mit &-Farbcodes
     * @return Das formatierte Präfix mit ANSI-Farbcodes
     */
    private String colorizePrefix(String rawPrefix) {
        if (rawPrefix == null) {
            return "";
        }
        
        String result = rawPrefix;
        
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
        if (useColors) {
            logger.severe(formatWithPrefix(RED + "FEHLER: " + message + RESET));
        } else {
            logger.severe(formatWithPrefix("FEHLER: " + message));
        }
    }
    
    /**
     * Gibt eine Warnungsbenachrichtigung aus
     * 
     * @param message Die Nachricht
     */
    public void warning(String message) {
        if (useColors) {
            logger.warning(formatWithPrefix(YELLOW + "WARNUNG: " + message + RESET));
        } else {
            logger.warning(formatWithPrefix("WARNUNG: " + message));
        }
    }
    
    /**
     * Gibt eine Informationsbenachrichtigung aus
     * 
     * @param message Die Nachricht
     */
    public void info(String message) {
        if (useColors) {
            logger.info(formatWithPrefix(WHITE + message + RESET));
        } else {
            logger.info(formatWithPrefix(message));
        }
    }
    
    /**
     * Gibt eine Erfolgsbenachrichtigung aus
     * 
     * @param message Die Nachricht
     */
    public void success(String message) {
        if (useColors) {
            String symbol = useUnicodeSymbols ? "✅ " : "";
            logger.info(formatWithPrefix(GREEN + symbol + message + RESET));
        } else {
            logger.info(formatWithPrefix("[ERFOLG] " + message));
        }
    }
    
    /**
     * Gibt eine Debug-Benachrichtigung aus
     * 
     * @param message Die Nachricht
     * @param showDebug Ob die Nachricht angezeigt werden soll
     */
    public void debug(String message, boolean showDebug) {
        if (!showDebug) {
            return;
        }
        
        if (useColors) {
            logger.info(formatWithPrefix(BRIGHT_BLUE + "[DEBUG] " + BRIGHT_WHITE + message + RESET));
        } else {
            logger.info(formatWithPrefix("[DEBUG] " + message));
        }
    }
    
    /**
     * Gibt eine Überschrift aus
     * 
     * @param title Der Titel
     */
    public void header(String title) {
        if (useColors) {
            String line = BRIGHT_CYAN + "═══════════════════════════════════════════" + RESET;
            logger.info(line);
            logger.info(formatWithPrefix(BRIGHT_WHITE + BOLD + title + RESET));
            logger.info(line);
        } else {
            String line = "===============================================";
            logger.info(line);
            logger.info(formatWithPrefix(title));
            logger.info(line);
        }
    }
    
    /**
     * Gibt einen Sektionsheader aus
     * 
     * @param title Der Titel der Sektion
     */
    public void section(String title) {
        if (useColors) {
            logger.info(formatWithPrefix(CYAN + "» " + BRIGHT_WHITE + BOLD + title + RESET));
        } else {
            logger.info(formatWithPrefix(">> " + title));
        }
    }
    
    /**
     * Gibt eine Nachricht einer bestimmten Kategorie aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     */
    public void categoryInfo(MessageCategory category, String message) {
        CategoryStyle style = categoryStyles.get(category);
        
        if (useColors) {
            String formattedCategory = "[" + style.color + style.code + RESET + "] ";
            if (useUnicodeSymbols && !style.icon.isEmpty()) {
                formattedCategory += style.icon + " ";
            }
            
            logger.info(formatWithPrefix(formattedCategory + WHITE + message + RESET));
        } else {
            String formattedCategory = "[" + style.code + "] ";
            logger.info(formatWithPrefix(formattedCategory + message));
        }
    }
    
    /**
     * Gibt eine Erfolgsbenachrichtigung einer bestimmten Kategorie aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     */
    public void categorySuccess(MessageCategory category, String message) {
        CategoryStyle style = categoryStyles.get(category);
        
        if (useColors) {
            String formattedCategory = "[" + style.color + style.code + RESET + "] ";
            String successIcon = useUnicodeSymbols ? "✓ " : "";
            
            logger.info(formatWithPrefix(formattedCategory + GREEN + successIcon + message + RESET));
        } else {
            String formattedCategory = "[" + style.code + "] ";
            logger.info(formatWithPrefix(formattedCategory + "[ERFOLG] " + message));
        }
    }
    
    /**
     * Gibt eine Warnung einer bestimmten Kategorie aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     */
    public void categoryWarning(MessageCategory category, String message) {
        CategoryStyle style = categoryStyles.get(category);
        
        if (useColors) {
            String formattedCategory = "[" + style.color + style.code + RESET + "] ";
            String warningIcon = useUnicodeSymbols ? "⚠ " : "";
            
            logger.warning(formatWithPrefix(formattedCategory + YELLOW + warningIcon + message + RESET));
        } else {
            String formattedCategory = "[" + style.code + "] ";
            logger.warning(formatWithPrefix(formattedCategory + "[WARNUNG] " + message));
        }
    }
    
    /**
     * Gibt einen Fehler einer bestimmten Kategorie aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     */
    public void categoryError(MessageCategory category, String message) {
        CategoryStyle style = categoryStyles.get(category);
        
        if (useColors) {
            String formattedCategory = "[" + style.color + style.code + RESET + "] ";
            String errorIcon = useUnicodeSymbols ? "✗ " : "";
            
            logger.severe(formatWithPrefix(formattedCategory + RED + errorIcon + message + RESET));
        } else {
            String formattedCategory = "[" + style.code + "] ";
            logger.severe(formatWithPrefix(formattedCategory + "[FEHLER] " + message));
        }
    }
    
    /**
     * Gibt einen Debug-Eintrag einer bestimmten Kategorie aus
     * 
     * @param category Die Kategorie
     * @param message Die Nachricht
     * @param showDebug Ob die Nachricht angezeigt werden soll
     */
    public void categoryDebug(MessageCategory category, String message, boolean showDebug) {
        if (!showDebug) {
            return;
        }
        
        CategoryStyle style = categoryStyles.get(category);
        
        if (useColors) {
            String formattedCategory = "[" + style.color + style.code + RESET + "] ";
            
            logger.info(formatWithPrefix(formattedCategory + BRIGHT_BLUE + "[DEBUG] " + 
                                        BRIGHT_WHITE + message + RESET));
        } else {
            String formattedCategory = "[" + style.code + "] ";
            logger.info(formatWithPrefix(formattedCategory + "[DEBUG] " + message));
        }
    }
    
    /**
     * Zeigt eine Statusmeldung mit Farbkodierung basierend auf dem Status an
     * 
     * @param label Die Bezeichnung des Status
     * @param message Die eigentliche Nachricht
     * @param success Der Status (true = Erfolg, false = Fehler)
     */
    public void status(String label, String message, boolean success) {
        String statusSymbol;
        String statusColor;
        
        if (success) {
            // Erfolgreicher Status
            statusSymbol = useUnicodeSymbols ? "✓" : "+";
            statusColor = GREEN;
        } else {
            // Fehlerhafter Status
            statusSymbol = useUnicodeSymbols ? "✗" : "x";
            statusColor = RED;
        }
        
        if (useColors) {
            logger.info(formatWithPrefix(CYAN + "[" + statusColor + statusSymbol + CYAN + "] " + 
                                         WHITE + label + ": " + statusColor + message + RESET));
        } else {
            logger.info(formatWithPrefix("[" + (success ? "+" : "x") + "] " + 
                                         label + ": " + message));
        }
    }
    
    /**
     * Gibt eine Schrittbenachrichtigung aus
     * 
     * @param step Die Schrittnummer
     * @param total Die Gesamtzahl der Schritte
     * @param description Die Beschreibung
     */
    public void step(int step, int total, String description) {
        if (useColors) {
            logger.info(formatWithPrefix(BRIGHT_BLUE + "(" + step + "/" + total + ")" + 
                                        RESET + " " + WHITE + description + RESET));
        } else {
            logger.info(formatWithPrefix("(" + step + "/" + total + ") " + description));
        }
    }
    
    /**
     * Gibt eine leere Zeile aus
     */
    public void blank() {
        logger.info("");
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
     * Gibt eine kleine Überschrift ohne Rahmen aus
     * 
     * @param title Der Titel
     */
    public void subHeader(String title) {
        if (useColors) {
            logger.info("");
            logger.info(BRIGHT_CYAN + "◈ " + BRIGHT_WHITE + BOLD + title + RESET);
        } else {
            logger.info("");
            logger.info("-- " + title);
        }
    }
    
    /**
     * Hilfsdatenklasse für die Stile der verschiedenen Nachrichtenkategorien
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
     * Zeigt einen farbigen Fortschrittsbalken in der Konsole an
     * 
     * @param current Der aktuelle Fortschritt
     * @param total Die Gesamtmenge
     * @param width Die Breite des Balkens
     * @param label Die Beschriftung neben dem Balken
     */
    public void colorProgressBar(int current, int total, int width, String label) {
        if (total <= 0) return;
        
        StringBuilder bar = new StringBuilder();
        int percent = (int) ((current * 100.0f) / total);
        int completed = (int) ((current * width) / total);
        
        bar.append("[");
        
        if (useColors) {
            // Fortschrittsbalken mit Farbverlauf
            for (int i = 0; i < width; i++) {
                if (i < completed) {
                    // Farbverlauf von Grün zu Cyan
                    if (i < width / 3) bar.append(BRIGHT_GREEN + "█");
                    else if (i < width * 2 / 3) bar.append(GREEN + "█");
                    else bar.append(BRIGHT_CYAN + "█");
                } else {
                    bar.append(BRIGHT_BLACK + "░");
                }
            }
            bar.append(RESET);
        } else {
            // Einfacher ASCII-Fortschrittsbalken ohne Farben
            for (int i = 0; i < width; i++) {
                if (i < completed) bar.append("#");
                else bar.append("-");
            }
        }
        
        bar.append("] ").append(label);
        
        logger.info(bar.toString());
    }
    
    /**
     * Zeigt einen Schritt mit Fortschrittsanzeige und Statusfarbe an
     * 
     * @param step Der aktuelle Schritt
     * @param total Die Gesamtanzahl der Schritte
     * @param description Die Beschreibung des Schritts
     * @param success True, wenn erfolgreich, sonst false
     */
    public void richStep(int step, int total, String description, boolean success) {
        if (!useColors) {
            logger.info(String.format("(%d/%d) %s: %s", step, total, success ? "OK" : "FEHLER", description));
            return;
        }
        
        StringBuilder message = new StringBuilder();
        
        // Fortschrittszähler mit Farbe
        message.append(BRIGHT_BLACK).append("(");
        message.append(BRIGHT_CYAN).append(step);
        message.append(BRIGHT_BLACK).append("/");
        message.append(BRIGHT_WHITE).append(total);
        message.append(BRIGHT_BLACK).append(") ");
        
        // Status-Symbol und Text
        if (success) {
            message.append(BRIGHT_GREEN).append(SUCCESS_SYMBOL).append(" ");
            message.append(GREEN).append(description);
        } else {
            message.append(BRIGHT_RED).append(ERROR_SYMBOL).append(" ");
            message.append(RED).append(description);
        }
        
        message.append(RESET);
        logger.info(message.toString());
    }
    
    /**
     * Zeigt einen formatierten Textblock mit Titel und Inhalt an
     * 
     * @param title Der Titel des Blocks
     * @param content Der Inhalt des Blocks
     * @param success True, wenn erfolgreich, sonst false
     */
    public void textBlock(String title, String content, boolean success) {
        if (!useColors) {
            logger.info("=== " + title + " ===");
            logger.info(content);
            logger.info("=================");
            return;
        }
        
        String bgColor = success ? BG_GREEN : BG_RED;
        String titleColor = success ? BRIGHT_WHITE : BRIGHT_WHITE;
        String contentColor = success ? BRIGHT_GREEN : BRIGHT_RED;
        
        StringBuilder block = new StringBuilder();
        
        // Kopfzeile
        block.append(bgColor).append(" ");
        block.append(titleColor).append(BOLD).append(title).append(" ");
        block.append(RESET).append("\n");
        
        // Inhalt
        for (String line : content.split("\n")) {
            block.append(contentColor).append("  ").append(line).append(RESET).append("\n");
        }
        
        logger.info(block.toString());
    }
    
    /**
     * Zeigt einen Listeneintrag an
     *
     * @param symbol Das Symbol für den Eintrag
     * @param text Der Text des Eintrags
     */
    public void listItem(String symbol, String text) {
        if (useColors) {
            logger.info(formatWithPrefix(BRIGHT_CYAN + symbol + " " + WHITE + text + RESET));
        } else {
            logger.info(formatWithPrefix(symbol + " " + text));
        }
    }
    
    /**
     * Zeigt einen einfachen Fortschrittsbalken an
     * 
     * @param current Der aktuelle Fortschritt
     * @param total Die Gesamtmenge
     * @param width Die Breite des Balkens
     */
    public void progressBar(int current, int total, int width) {
        if (total <= 0) return;
        
        int completed = (int) ((current * width) / total);
        StringBuilder bar = new StringBuilder("[");
        
        for (int i = 0; i < width; i++) {
            if (i < completed) {
                bar.append("=");
            } else if (i == completed) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }
        
        bar.append("] ").append(current).append("/").append(total);
        logger.info(formatWithPrefix(bar.toString()));
    }
} 