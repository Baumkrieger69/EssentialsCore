package com.essentialscore.api.security;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logs security-related events for auditing purposes.
 */
public class AuditLogger {
    private static final Logger LOGGER = Logger.getLogger(AuditLogger.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    private final Plugin plugin;
    private final File logDirectory;
    private final BlockingQueue<LogEntry> logQueue;
    private final Thread loggerThread;
    private boolean running;
    
    /**
     * Creates a new audit logger.
     *
     * @param plugin The EssentialsCore plugin
     */
    public AuditLogger(Plugin plugin) {
        this.plugin = plugin;
        this.logDirectory = new File(plugin.getDataFolder(), "security/logs");
        this.logQueue = new LinkedBlockingQueue<>();
        this.loggerThread = new Thread(this::processLogQueue, "AuditLogger");
        this.running = false;
    }
    
    /**
     * Initializes the audit logger.
     */
    public void initialize() {
        LOGGER.info("Initializing audit logger...");
        
        // Create log directory if it doesn't exist
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
        
        // Start logger thread
        running = true;
        loggerThread.setDaemon(true);
        loggerThread.start();
        
        LOGGER.info("Audit logger initialized");
    }
    
    /**
     * Shuts down the audit logger.
     */
    public void shutdown() {
        LOGGER.info("Shutting down audit logger...");
        
        // Stop logger thread
        running = false;
        loggerThread.interrupt();
        
        // Wait for thread to finish
        try {
            loggerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Process remaining logs
        LogEntry entry;
        while ((entry = logQueue.poll()) != null) {
            writeLogEntry(entry);
        }
        
        LOGGER.info("Audit logger shut down");
    }
    
    /**
     * Logs a security event.
     *
     * @param moduleId The module ID
     * @param message The message
     */
    public void logSecurity(String moduleId, String message) {
        logQueue.add(new LogEntry(moduleId, "SECURITY", message));
    }
    
    /**
     * Logs an access event.
     *
     * @param moduleId The module ID
     * @param message The message
     */
    public void logAccess(String moduleId, String message) {
        logQueue.add(new LogEntry(moduleId, "ACCESS", message));
    }
    
    /**
     * Logs a permission event.
     *
     * @param moduleId The module ID
     * @param message The message
     */
    public void logPermission(String moduleId, String message) {
        logQueue.add(new LogEntry(moduleId, "PERMISSION", message));
    }
    
    /**
     * Logs a vulnerability event.
     *
     * @param moduleId The module ID
     * @param message The message
     */
    public void logVulnerability(String moduleId, String message) {
        logQueue.add(new LogEntry(moduleId, "VULNERABILITY", message));
    }
    
    /**
     * Logs a module event.
     *
     * @param moduleId The module ID
     * @param message The message
     */
    public void logModule(String moduleId, String message) {
        logQueue.add(new LogEntry(moduleId, "MODULE", message));
    }
    
    /**
     * Processes the log queue.
     */
    private void processLogQueue() {
        LOGGER.info("Audit logger thread started");
        
        while (running) {
            try {
                LogEntry entry = logQueue.take();
                writeLogEntry(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing log entry", e);
            }
        }
        
        LOGGER.info("Audit logger thread stopped");
    }
    
    /**
     * Writes a log entry to the log file.
     *
     * @param entry The log entry
     */
    private void writeLogEntry(LogEntry entry) {
        File logFile = getLogFile();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(formatLogEntry(entry));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing to audit log", e);
        }
    }
    
    /**
     * Gets the log file for the current date.
     *
     * @return The log file
     */
    private File getLogFile() {
        String fileName = "audit-" + FILE_DATE_FORMAT.format(new Date()) + ".log";
        return new File(logDirectory, fileName);
    }
    
    /**
     * Formats a log entry.
     *
     * @param entry The log entry
     * @return The formatted log entry
     */
    private String formatLogEntry(LogEntry entry) {
        return String.format("[%s] [%s] [%s] %s",
            DATE_FORMAT.format(entry.timestamp),
            entry.moduleId,
            entry.type,
            entry.message);
    }
    
    /**
     * Represents a log entry.
     */
    private static class LogEntry {
        private final Date timestamp;
        private final String moduleId;
        private final String type;
        private final String message;
        
        /**
         * Creates a new log entry.
         *
         * @param moduleId The module ID
         * @param type The entry type
         * @param message The message
         */
        public LogEntry(String moduleId, String type, String message) {
            this.timestamp = new Date();
            this.moduleId = moduleId;
            this.type = type;
            this.message = message;
        }
    }
} 