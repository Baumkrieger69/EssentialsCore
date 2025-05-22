package com.essentialscore.api.event;

/**
 * Enum für Event-Prioritäten
 */
public enum EventPriority {
    /**
     * Event wird zuerst aufgerufen
     */
    LOWEST,
    
    /**
     * Event wird früh aufgerufen
     */
    LOW,
    
    /**
     * Event wird mit normaler Priorität aufgerufen
     */
    NORMAL,
    
    /**
     * Event wird spät aufgerufen
     */
    HIGH,
    
    /**
     * Event wird als letztes aufgerufen
     */
    HIGHEST,
    
    /**
     * Event wird nur zum Beobachten aufgerufen, keine Änderungen erlaubt
     */
    MONITOR
} 