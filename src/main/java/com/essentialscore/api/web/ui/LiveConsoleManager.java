package com.essentialscore.api.web.ui;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Manages the live console output and command execution.
 */
public class LiveConsoleManager {
    private static final Logger LOGGER = Logger.getLogger(LiveConsoleManager.class.getName());
    
    private final Plugin plugin;
    private final Map<String, Consumer<String>> subscribers;
    private final ConcurrentLinkedQueue<String> consoleHistory;
    private ConsoleLogHandler logHandler;
    private final int maxHistorySize;
    
    /**
     * Creates a new live console manager
     * 
     * @param plugin The plugin instance
     * @param config The configuration
     */
    public LiveConsoleManager(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.subscribers = new ConcurrentHashMap<>();
        this.consoleHistory = new ConcurrentLinkedQueue<>();
        this.maxHistorySize = config.getInt("ui.console.max-lines", 1000);
        
        // Setup log capturing
        setupLogCapture();
    }
    
    /**
     * Sets up log capturing to intercept console output
     */
    private void setupLogCapture() {
        try {
            // Create and register our log handler
            Logger rootLogger = Logger.getLogger("");
            logHandler = new ConsoleLogHandler();
            rootLogger.addHandler(logHandler);
            
            LOGGER.info("Console log capture started");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set up console log capturing", e);
        }
    }
    
    /**
     * Adds a console line to history and notifies subscribers
     * 
     * @param line The console line
     */
    public void addConsoleLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        
        // Add to history
        consoleHistory.add(line);
        
        // Trim history if needed
        while (consoleHistory.size() > maxHistorySize) {
            consoleHistory.poll();
        }
        
        // Notify subscribers
        for (Consumer<String> callback : subscribers.values()) {
            try {
                callback.accept(line);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying console subscriber", e);
            }
        }
    }
    
    /**
     * Gets the console history
     * 
     * @return The console history as an array
     */
    public String[] getConsoleHistory() {
        return consoleHistory.toArray(new String[0]);
    }
    
    /**
     * Subscribes a client to console updates
     * 
     * @param clientId The client ID
     * @param callback The callback to invoke with console lines
     */
    public void subscribe(String clientId, Consumer<String> callback) {
        subscribers.put(clientId, callback);
    }
    
    /**
     * Unsubscribes a client from console updates
     * 
     * @param clientId The client ID
     */
    public void unsubscribe(String clientId) {
        subscribers.remove(clientId);
    }
    
    /**
     * Executes a console command
     * 
     * @param command The command to execute
     * @return true if the command was executed successfully
     */
    public boolean executeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Log the command execution
            addConsoleLine("> " + command);
            
            // Execute the command
            return plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(), command);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing command: " + command, e);
            return false;
        }
    }
    
    /**
     * Cleans up resources
     */
    public void shutdown() {
        // Remove log handler
        if (logHandler != null) {
            Logger rootLogger = Logger.getLogger("");
            rootLogger.removeHandler(logHandler);
        }
        
        // Clear subscribers and history
        subscribers.clear();
        consoleHistory.clear();
    }
    
    /**
     * Custom log handler to capture console output
     */
    private class ConsoleLogHandler extends Handler {
        
        @Override
        public void publish(LogRecord record) {
            if (record == null) {
                return;
            }
            
            try {
                // Format the log message
                String message = getFormatter().format(record);
                
                // Add to console history and notify subscribers
                addConsoleLine(message);
            } catch (Exception e) {
                // Don't log this to avoid potential infinite recursion
                System.err.println("Error in console log handler: " + e.getMessage());
            }
        }
        
        @Override
        public void flush() {
            // Nothing to do
        }
        
        @Override
        public void close() throws SecurityException {
            // Nothing to do
        }
    }
} 