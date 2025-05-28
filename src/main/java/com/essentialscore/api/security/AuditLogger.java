package com.essentialscore.api.security;

<<<<<<< HEAD
import org.bukkit.entity.Player;
=======
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
<<<<<<< HEAD
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides audit logging capabilities to track critical system actions.
 * Maintains detailed logs for compliance and security tracking purposes.
=======
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logs security-related events for auditing purposes.
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
 */
public class AuditLogger {
    private static final Logger LOGGER = Logger.getLogger(AuditLogger.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    private final Plugin plugin;
<<<<<<< HEAD
    private final File logsDirectory;
    private final int retentionDays;
    private final BlockingQueue<LogEntry> logQueue;
    private boolean enabled;
    private Thread logWriterThread;
=======
    private final File logDirectory;
    private final BlockingQueue<LogEntry> logQueue;
    private final Thread loggerThread;
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
    private boolean running;
    
    /**
     * Creates a new audit logger.
     *
<<<<<<< HEAD
     * @param plugin The plugin
     * @param retentionDays Number of days to keep logs (0 for indefinite)
     */
    public AuditLogger(Plugin plugin, int retentionDays) {
        this.plugin = plugin;
        this.logsDirectory = new File(plugin.getDataFolder(), "audit-logs");
        this.retentionDays = retentionDays;
        this.logQueue = new LinkedBlockingQueue<>();
        this.enabled = true;
=======
     * @param plugin The EssentialsCore plugin
     */
    public AuditLogger(Plugin plugin) {
        this.plugin = plugin;
        this.logDirectory = new File(plugin.getDataFolder(), "security/logs");
        this.logQueue = new LinkedBlockingQueue<>();
        this.loggerThread = new Thread(this::processLogQueue, "AuditLogger");
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        this.running = false;
    }
    
    /**
     * Initializes the audit logger.
     */
    public void initialize() {
<<<<<<< HEAD
        if (running) return;
        
        // Create logs directory
        if (!logsDirectory.exists()) {
            logsDirectory.mkdirs();
        }
        
        // Start log writer thread
        running = true;
        logWriterThread = new Thread(this::processLogQueue);
        logWriterThread.setName("AuditLogger-Thread");
        logWriterThread.setDaemon(true);
        logWriterThread.start();
        
        // Schedule log cleanup if retention is set
        if (retentionDays > 0) {
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                    plugin,
                    this::cleanupOldLogs,
                    20 * 60 * 60, // 1 hour delay
                    20 * 60 * 60 * 24 // Run once per day
            );
        }
        
        LOGGER.info("Audit logger initialized. Retention policy: " + 
                    (retentionDays > 0 ? retentionDays + " days" : "indefinite"));
=======
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
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
    }
    
    /**
     * Shuts down the audit logger.
     */
    public void shutdown() {
<<<<<<< HEAD
        if (!running) return;
        
        running = false;
        logWriterThread.interrupt();
        
        // Process remaining logs
        processRemainingLogs();
=======
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
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        
        LOGGER.info("Audit logger shut down");
    }
    
    /**
<<<<<<< HEAD
     * Enables or disables the audit logger.
     *
     * @param enabled Whether the logger is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Checks if the audit logger is enabled.
     *
     * @return Whether the logger is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Logs a player action.
     *
     * @param category The action category
     * @param action The specific action
     * @param player The player
     * @param details Additional details
     */
    public void logPlayerAction(String category, String action, Player player, String details) {
        if (!enabled) return;
        
        LogEntry entry = new LogEntry(
                category,
                action,
                player.getUniqueId(),
                player.getName(),
                player.getAddress().getAddress().getHostAddress(),
                details
        );
        
        queueLog(entry);
    }
    
    /**
     * Logs a player action by UUID.
     *
     * @param category The action category
     * @param action The specific action
     * @param playerId The player's UUID
     * @param playerName The player's name
     * @param ipAddress The player's IP address
     * @param details Additional details
     */
    public void logPlayerAction(String category, String action, UUID playerId, 
                               String playerName, String ipAddress, String details) {
        if (!enabled) return;
        
        LogEntry entry = new LogEntry(
                category,
                action,
                playerId,
                playerName,
                ipAddress,
                details
        );
        
        queueLog(entry);
    }
    
    /**
     * Logs a system action.
     *
     * @param category The action category
     * @param action The specific action
     * @param details Additional details
     */
    public void logSystemAction(String category, String action, String details) {
        if (!enabled) return;
        
        LogEntry entry = new LogEntry(
                category,
                action,
                null,
                "SYSTEM",
                "127.0.0.1",
                details
        );
        
        queueLog(entry);
    }
    
    /**
     * Queues a log entry for writing.
     *
     * @param entry The log entry
     */
    private void queueLog(LogEntry entry) {
        try {
            logQueue.put(entry);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Failed to queue audit log entry", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Processes the log queue in a background thread.
     */
    private void processLogQueue() {
        while (running) {
            try {
                LogEntry entry = logQueue.take();
                writeLogToFile(entry);
            } catch (InterruptedException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "Audit logger thread interrupted", e);
                }
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error writing audit log", e);
            }
        }
    }
    
    /**
     * Processes any remaining logs in the queue during shutdown.
     */
    private void processRemainingLogs() {
        LogEntry entry;
        while ((entry = logQueue.poll()) != null) {
            try {
                writeLogToFile(entry);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error writing audit log during shutdown", e);
            }
        }
    }
    
    /**
     * Writes a log entry to the appropriate file.
     *
     * @param entry The log entry
     */
    private void writeLogToFile(LogEntry entry) {
        String date = FILE_DATE_FORMAT.format(entry.timestamp);
        File logFile = new File(logsDirectory, date + ".log");
        
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter out = new PrintWriter(fw)) {
            
            out.println(formatLogEntry(entry));
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write to audit log file: " + logFile.getPath(), e);
=======
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
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        }
    }
    
    /**
<<<<<<< HEAD
     * Formats a log entry as a string.
=======
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
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
     *
     * @param entry The log entry
     * @return The formatted log entry
     */
    private String formatLogEntry(LogEntry entry) {
<<<<<<< HEAD
        StringBuilder sb = new StringBuilder();
        
        // Format: [timestamp] [category] [action] [userId] [userName] [ipAddress] [details]
        sb.append('[').append(DATE_FORMAT.format(entry.timestamp)).append(']');
        sb.append(" [").append(entry.category).append(']');
        sb.append(" [").append(entry.action).append(']');
        sb.append(" [").append(entry.userId != null ? entry.userId : "N/A").append(']');
        sb.append(" [").append(entry.userName).append(']');
        sb.append(" [").append(entry.ipAddress).append(']');
        sb.append(" ").append(entry.details);
        
        return sb.toString();
    }
    
    /**
     * Cleans up old log files based on retention policy.
     */
    private void cleanupOldLogs() {
        if (retentionDays <= 0) return;
        
        try {
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
            
            File[] logFiles = logsDirectory.listFiles((dir, name) -> name.endsWith(".log"));
            if (logFiles == null) return;
            
            int deletedCount = 0;
            for (File logFile : logFiles) {
                if (logFile.lastModified() < cutoffTime) {
                    if (logFile.delete()) {
                        deletedCount++;
                    } else {
                        LOGGER.warning("Failed to delete old log file: " + logFile.getPath());
                    }
                }
            }
            
            if (deletedCount > 0) {
                LOGGER.info("Cleaned up " + deletedCount + " old audit log files");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error cleaning up old audit logs", e);
        }
    }
    
    /**
     * Retrieves log entries for a specified date.
     *
     * @param date The date in yyyy-MM-dd format
     * @return An array of log entries as strings, or null if the file doesn't exist
     */
    public String[] getLogEntries(String date) {
        File logFile = new File(logsDirectory, date + ".log");
        
        if (!logFile.exists()) {
            return null;
        }
        
        try {
            return Files.readAllLines(logFile.toPath()).toArray(new String[0]);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read audit log file: " + logFile.getPath(), e);
            return null;
        }
    }
    
    /**
     * Gets a list of available log dates.
     *
     * @return An array of dates in yyyy-MM-dd format
     */
    public String[] getAvailableDates() {
        File[] logFiles = logsDirectory.listFiles((dir, name) -> name.endsWith(".log"));
        
        if (logFiles == null) {
            return new String[0];
        }
        
        String[] dates = new String[logFiles.length];
        for (int i = 0; i < logFiles.length; i++) {
            dates[i] = logFiles[i].getName().replace(".log", "");
        }
        
        return dates;
    }
    
    /**
     * Class representing a log entry.
     */
    private static class LogEntry {
        private final Date timestamp;
        private final String category;
        private final String action;
        private final UUID userId;
        private final String userName;
        private final String ipAddress;
        private final String details;
=======
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
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
        
        /**
         * Creates a new log entry.
         *
<<<<<<< HEAD
         * @param category The action category
         * @param action The specific action
         * @param userId The user's UUID
         * @param userName The user's name
         * @param ipAddress The user's IP address
         * @param details Additional details
         */
        public LogEntry(String category, String action, UUID userId, 
                        String userName, String ipAddress, String details) {
            this.timestamp = new Date();
            this.category = category;
            this.action = action;
            this.userId = userId;
            this.userName = userName;
            this.ipAddress = ipAddress;
            this.details = details;
        }
    }
    
    /**
     * Standard categories for audit logs.
     */
    public static class Categories {
        public static final String SECURITY = "SECURITY";
        public static final String ADMIN = "ADMIN";
        public static final String USER = "USER";
        public static final String PERMISSIONS = "PERMISSIONS";
        public static final String ECONOMY = "ECONOMY";
        public static final String MODERATION = "MODERATION";
        public static final String DATA = "DATA";
        public static final String AUTHENTICATION = "AUTHENTICATION";
        public static final String GDPR = "GDPR";
    }
    
    /**
     * Standard actions for audit logs.
     */
    public static class Actions {
        public static final String LOGIN = "LOGIN";
        public static final String LOGOUT = "LOGOUT";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String PERMISSION_GRANTED = "PERMISSION_GRANTED";
        public static final String PERMISSION_REVOKED = "PERMISSION_REVOKED";
        public static final String ROLE_ASSIGNED = "ROLE_ASSIGNED";
        public static final String ROLE_REMOVED = "ROLE_REMOVED";
        public static final String ROLE_CREATED = "ROLE_CREATED";
        public static final String ROLE_DELETED = "ROLE_DELETED";
        public static final String ADMIN_COMMAND = "ADMIN_COMMAND";
        public static final String USER_BANNED = "USER_BANNED";
        public static final String USER_UNBANNED = "USER_UNBANNED";
        public static final String USER_KICKED = "USER_KICKED";
        public static final String USER_MUTED = "USER_MUTED";
        public static final String USER_UNMUTED = "USER_UNMUTED";
        public static final String ECONOMY_TRANSACTION = "ECONOMY_TRANSACTION";
        public static final String DATA_EXPORTED = "DATA_EXPORTED";
        public static final String DATA_DELETED = "DATA_DELETED";
        public static final String DATA_ACCESSED = "DATA_ACCESSED";
        public static final String SETTINGS_CHANGED = "SETTINGS_CHANGED";
        public static final String TWO_FACTOR_ENABLED = "TWO_FACTOR_ENABLED";
        public static final String TWO_FACTOR_DISABLED = "TWO_FACTOR_DISABLED";
        public static final String TWO_FACTOR_VERIFIED = "TWO_FACTOR_VERIFIED";
        public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
    }
=======
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
>>>>>>> 1cd13dada4735d9fd6a061a32e5e9d93533588ac
} 