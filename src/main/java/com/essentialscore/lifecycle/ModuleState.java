package com.essentialscore.lifecycle;

/**
 * Repräsentiert die verschiedenen Zustände, die ein Modul während seines Lebenszyklus durchläuft.
 * Stellt sicher, dass Zustandsübergänge valide sind und bietet detaillierte Statusinformationen.
 */
public enum ModuleState {
    DISCOVERED(-2, "Entdeckt"),
    UNLOADED(-1, "Nicht geladen"),
    LOADING(0, "Wird geladen"),
    PRE_INITIALIZED(1, "Vor-Initialisiert"),
    INITIALIZING(2, "Wird initialisiert"),
    INITIALIZED(3, "Initialisiert"),
    ENABLING(4, "Wird aktiviert"),
    ENABLED(5, "Aktiv"),
    DISABLING(6, "Wird deaktiviert"),
    DISABLED(7, "Deaktiviert"),
    RELOADING(8, "Wird neu geladen"),
    UNLOADING(9, "Wird entladen"),
    ERROR(-3, "Fehler");

    private final int order;
    private final String description;

    ModuleState(int order, String description) {
        this.order = order;
        this.description = description;
    }

    /**
     * Prüft, ob dieser Zustand ein aktiver Zustand ist.
     */
    public boolean isActive() {
        return this == ENABLED;
    }

    /**
     * Prüft, ob dieser Zustand ein geladener Zustand ist.
     */
    public boolean isLoaded() {
        return order >= LOADING.order && order <= DISABLED.order;
    }

    /**
     * Prüft, ob dieser Zustand ein terminaler Zustand ist.
     */
    public boolean isTerminal() {
        return this == ERROR || this == UNLOADED || this == DISABLED;
    }

    /**
     * Prüft, ob dieser Zustand ein Übergangszustand ist.
     */
    public boolean isTransitionalState() {
        return this == LOADING || this == INITIALIZING || 
               this == ENABLING || this == DISABLING || 
               this == RELOADING || this == UNLOADING;
    }

    /**
     * Gibt den nächsten erwarteten Zustand im normalen Lebenszyklus zurück.
     */
    public ModuleState getNextState() {
        switch (this) {
            case DISCOVERED:
                return LOADING;
            case UNLOADED:
                return LOADING;
            case LOADING:
                return PRE_INITIALIZED;
            case PRE_INITIALIZED:
                return INITIALIZING;
            case INITIALIZING:
                return INITIALIZED;
            case INITIALIZED:
                return ENABLING;
            case ENABLING:
                return ENABLED;
            case ENABLED:
                return DISABLING;
            case DISABLING:
                return DISABLED;
            case DISABLED:
                return null; // Terminal state unless explicitly re-enabled
            case RELOADING:
                return INITIALIZING;
            case UNLOADING:
                return UNLOADED;
            case ERROR:
                return null; // Terminal state unless explicitly recovered
            default:
                return null;
        }
    }

    /**
     * Prüft, ob ein Übergang in den angegebenen Zielzustand erlaubt ist.
     */
    public boolean canTransitionTo(ModuleState target) {
        if (target == null) return false;
        if (target == ERROR) return true; // Jeder Zustand kann in ERROR übergehen
        
        switch (this) {
            case DISCOVERED:
                return target == LOADING;
            case UNLOADED:
                return target == LOADING;
            case LOADING:
                return target == PRE_INITIALIZED || target == ERROR;
            case PRE_INITIALIZED:
                return target == INITIALIZING || target == ERROR;
            case INITIALIZING:
                return target == INITIALIZED || target == ERROR;
            case INITIALIZED:
                return target == ENABLING || target == ERROR;
            case ENABLING:
                return target == ENABLED || target == ERROR;
            case ENABLED:
                return target == DISABLING || target == RELOADING || target == ERROR;
            case DISABLING:
                return target == DISABLED || target == ERROR;
            case DISABLED:
                return target == ENABLING || target == UNLOADING || target == ERROR;
            case RELOADING:
                return target == INITIALIZING || target == ERROR;
            case UNLOADING:
                return target == UNLOADED || target == ERROR;
            case ERROR:
                return target == UNLOADED || target == RELOADING;
            default:
                return false;
        }
    }

    /**
     * Gibt eine benutzerfreundliche Beschreibung des Zustands zurück.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gibt die Ordnungszahl des Zustands zurück.
     */
    public int getOrder() {
        return order;
    }

    /**
     * Gibt eine textuelle Repräsentation des Zustands zurück.
     */
    @Override
    public String toString() {
        return description;
    }
}
