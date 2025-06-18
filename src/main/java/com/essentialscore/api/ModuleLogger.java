package com.essentialscore.api;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Custom logger for modules that prefixes all log messages with the module name.
 */
public class ModuleLogger extends Logger {
    private final String modulePrefix;
    private final boolean debugEnabled;

    /**
     * Creates a new module logger.
     *
     * @param parent The parent logger
     * @param moduleName The module name
     * @param debugEnabled Whether debug logging is enabled
     */
    public ModuleLogger(Logger parent, String moduleName, boolean debugEnabled) {
        super(moduleName, null);
        setParent(parent);
        this.modulePrefix = "[" + moduleName + "] ";
        this.debugEnabled = debugEnabled;
    }

    @Override
    public void log(LogRecord record) {
        // Skip debug messages if debug mode is disabled
        if (record.getLevel() == Level.FINE && !debugEnabled) {
            return;
        }
        
        // Add module prefix to the message
        record.setMessage(modulePrefix + record.getMessage());
        
        // Pass to parent logger
        super.log(record);
    }

    /**
     * Logs a debug message. These are only logged if debug mode is enabled.
     *
     * @param message The message to log
     */
    public void debug(String message) {
        if (debugEnabled) {
            log(Level.FINE, message);
        }
    }

    /**
     * Logs a debug message with an exception. These are only logged if debug mode is enabled.
     *
     * @param message The message to log
     * @param thrown The exception to log
     */
    public void debug(String message, Throwable thrown) {
        if (debugEnabled) {
            log(Level.FINE, message, thrown);
        }
    }
    
    /**
     * Checks if debug logging is enabled.
     *
     * @return True if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
} 
