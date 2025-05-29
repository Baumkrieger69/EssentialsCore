/**
 * EssentialsCore Advanced WebUI - WebSocket Integration
 * Real-time Updates und bidirektionale Kommunikation
 */

class WebUIWebSocketManager {
    constructor(webuiInstance) {
        this.webui = webuiInstance;
        this.websocket = null;
        this.isConnected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000;
        this.heartbeatInterval = null;
        this.messageQueue = [];
        this.subscriptions = new Map();
        this.eventHandlers = new Map();
        
        this.init();
    }

    /**
     * WebSocket-Manager initialisieren
     */
    init() {
        this.setupEventHandlers();
        this.connect();
    }

    /**
     * Event-Handler einrichten
     */
    setupEventHandlers() {
        // Standard Event-Handler registrieren
        this.registerEventHandler('server-status', (data) => {
            this.handleServerStatusUpdate(data);
        });

        this.registerEventHandler('player-update', (data) => {
            this.handlePlayerUpdate(data);
        });

        this.registerEventHandler('performance-data', (data) => {
            this.handlePerformanceData(data);
        });

        this.registerEventHandler('console-output', (data) => {
            this.handleConsoleOutput(data);
        });

        this.registerEventHandler('system-alert', (data) => {
            this.handleSystemAlert(data);
        });

        this.registerEventHandler('notification', (data) => {
            this.handleNotification(data);
        });
    }

    /**
     * WebSocket-Verbindung herstellen
     */
    connect() {
        try {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const host = window.location.host;
            const wsUrl = `${protocol}//${host}/api/websocket`;

            console.log(`Verbinde zu WebSocket: ${wsUrl}`);
            
            this.websocket = new WebSocket(wsUrl);
            this.setupWebSocketEvents();
            
        } catch (error) {
            console.error('Fehler beim Erstellen der WebSocket-Verbindung:', error);
            this.handleConnectionError(error);
        }
    }

    /**
     * WebSocket-Events einrichten
     */
    setupWebSocketEvents() {
        this.websocket.onopen = (event) => {
            console.log('WebSocket-Verbindung hergestellt');
            this.isConnected = true;
            this.reconnectAttempts = 0;
            this.reconnectDelay = 1000;
            
            this.authenticate();
            this.processMessageQueue();
            this.startHeartbeat();
            this.notifyConnectionStatus('connected');
        };

        this.websocket.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                this.handleMessage(message);
            } catch (error) {
                console.error('Fehler beim Parsen der WebSocket-Nachricht:', error);
            }
        };

        this.websocket.onclose = (event) => {
            console.log('WebSocket-Verbindung geschlossen:', event.code, event.reason);
            this.isConnected = false;
            this.stopHeartbeat();
            
            if (!event.wasClean) {
                this.handleConnectionLoss();
            }
            
            this.notifyConnectionStatus('disconnected');
        };

        this.websocket.onerror = (error) => {
            console.error('WebSocket-Fehler:', error);
            this.handleConnectionError(error);
        };
    }

    /**
     * Authentifizierung über WebSocket
     */
    authenticate() {
        const authToken = localStorage.getItem('webui-auth-token');
        if (authToken) {
            this.send({
                type: 'authenticate',
                token: authToken
            });
        }
    }

    /**
     * Nachricht senden
     */
    send(message) {
        if (!this.isConnected) {
            console.log('WebSocket nicht verbunden - Nachricht in Warteschlange eingereiht');
            this.messageQueue.push(message);
            return false;
        }

        try {
            const messageString = JSON.stringify(message);
            this.websocket.send(messageString);
            return true;
        } catch (error) {
            console.error('Fehler beim Senden der WebSocket-Nachricht:', error);
            return false;
        }
    }

    /**
     * Warteschlange abarbeiten
     */
    processMessageQueue() {
        while (this.messageQueue.length > 0 && this.isConnected) {
            const message = this.messageQueue.shift();
            this.send(message);
        }
    }

    /**
     * Eingehende Nachricht verarbeiten
     */
    handleMessage(message) {
        const { type, data, id } = message;

        // Response auf gesendete Nachricht
        if (id && this.pendingRequests && this.pendingRequests.has(id)) {
            const { resolve, reject } = this.pendingRequests.get(id);
            this.pendingRequests.delete(id);
            
            if (message.error) {
                reject(new Error(message.error));
            } else {
                resolve(data);
            }
            return;
        }

        // Event-Handler aufrufen
        const handler = this.eventHandlers.get(type);
        if (handler) {
            try {
                handler(data);
            } catch (error) {
                console.error(`Fehler im Event-Handler für "${type}":`, error);
            }
        } else {
            console.warn(`Unbekannter Message-Type: ${type}`);
        }
    }

    /**
     * Server-Status Update
     */
    handleServerStatusUpdate(data) {
        const { status, players, tps, uptime } = data;
        
        // Status-Anzeige aktualisieren
        const statusElement = document.getElementById('server-status');
        if (statusElement) {
            statusElement.textContent = status;
            statusElement.className = `status ${status.toLowerCase()}`;
        }

        // Player-Count aktualisieren
        const playersElement = document.getElementById('player-count');
        if (playersElement) {
            playersElement.textContent = players;
        }

        // TPS aktualisieren
        const tpsElement = document.getElementById('tps-display');
        if (tpsElement) {
            tpsElement.textContent = `${tps.toFixed(1)} TPS`;
            tpsElement.className = tps >= 18 ? 'tps-good' : tps >= 15 ? 'tps-warning' : 'tps-critical';
        }

        // Uptime aktualisieren
        const uptimeElement = document.getElementById('uptime-display');
        if (uptimeElement) {
            uptimeElement.textContent = this.formatUptime(uptime);
        }
    }

    /**
     * Spieler-Update verarbeiten
     */
    handlePlayerUpdate(data) {
        const { action, player, playerList } = data;
        
        if (action === 'join') {
            this.showNotification(`${player.name} ist dem Server beigetreten`, 'info');
        } else if (action === 'leave') {
            this.showNotification(`${player.name} hat den Server verlassen`, 'info');
        }

        // Spielerliste aktualisieren
        if (playerList) {
            this.updatePlayerList(playerList);
        }
    }

    /**
     * Performance-Daten verarbeiten
     */
    handlePerformanceData(data) {
        const { cpu, memory, tps, entities, chunks } = data;
        
        // Charts aktualisieren (falls Datenvisualisierung verfügbar)
        if (window.webuiDataViz) {
            window.webuiDataViz.updateChart('server-performance', [cpu, memory, tps]);
        }

        // Dashboard-Widgets aktualisieren
        this.updatePerformanceWidgets({
            cpu: cpu,
            memory: memory,
            tps: tps,
            entities: entities,
            chunks: chunks
        });
    }

    /**
     * Konsolen-Output verarbeiten
     */
    handleConsoleOutput(data) {
        const { level, message, timestamp } = data;
        
        const consoleContainer = document.getElementById('console-output');
        if (consoleContainer) {
            const logEntry = document.createElement('div');
            logEntry.className = `console-entry level-${level.toLowerCase()}`;
            logEntry.innerHTML = `
                <span class="timestamp">${new Date(timestamp).toLocaleTimeString()}</span>
                <span class="level">[${level}]</span>
                <span class="message">${this.escapeHtml(message)}</span>
            `;
            
            consoleContainer.appendChild(logEntry);
            
            // Auto-scroll
            consoleContainer.scrollTop = consoleContainer.scrollHeight;
            
            // Alte Einträge entfernen (max. 1000)
            const entries = consoleContainer.children;
            if (entries.length > 1000) {
                consoleContainer.removeChild(entries[0]);
            }
        }
    }

    /**
     * System-Alert verarbeiten
     */
    handleSystemAlert(data) {
        const { level, title, message, dismissible = true } = data;
        
        this.showAlert(title, message, level, dismissible);
        
        // Kritische Alerts zusätzlich als Notification
        if (level === 'critical' || level === 'error') {
            this.showNotification(title, 'error');
        }
    }

    /**
     * Benachrichtigung verarbeiten
     */
    handleNotification(data) {
        const { title, message, type = 'info', duration = 5000 } = data;
        this.showNotification(title, type, duration);
    }

    /**
     * Event-Handler registrieren
     */
    registerEventHandler(type, handler) {
        this.eventHandlers.set(type, handler);
    }

    /**
     * Event-Handler entfernen
     */
    unregisterEventHandler(type) {
        this.eventHandlers.delete(type);
    }

    /**
     * Channel abonnieren
     */
    subscribe(channel) {
        this.send({
            type: 'subscribe',
            channel: channel
        });
        
        this.subscriptions.set(channel, true);
    }

    /**
     * Channel-Abo beenden
     */
    unsubscribe(channel) {
        this.send({
            type: 'unsubscribe',
            channel: channel
        });
        
        this.subscriptions.delete(channel);
    }

    /**
     * Heartbeat starten
     */
    startHeartbeat() {
        this.heartbeatInterval = setInterval(() => {
            this.send({ type: 'ping' });
        }, 30000); // Alle 30 Sekunden
    }

    /**
     * Heartbeat stoppen
     */
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
    }

    /**
     * Verbindungsverlust behandeln
     */
    handleConnectionLoss() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            console.log(`Reconnect-Versuch ${this.reconnectAttempts + 1}/${this.maxReconnectAttempts} in ${this.reconnectDelay}ms`);
            
            setTimeout(() => {
                this.reconnectAttempts++;
                this.reconnectDelay *= 2; // Exponential backoff
                this.connect();
            }, this.reconnectDelay);
        } else {
            console.error('Maximale Anzahl Reconnect-Versuche erreicht');
            this.showAlert(
                'Verbindung verloren',
                'Die Verbindung zum Server konnte nicht wiederhergestellt werden. Bitte aktualisieren Sie die Seite.',
                'error',
                false
            );
        }
    }

    /**
     * Verbindungsfehler behandeln
     */
    handleConnectionError(error) {
        console.error('WebSocket-Verbindungsfehler:', error);
        this.notifyConnectionStatus('error');
    }

    /**
     * Verbindungsstatus benachrichtigen
     */
    notifyConnectionStatus(status) {
        // Status-Indikator aktualisieren
        const indicator = document.getElementById('connection-status');
        if (indicator) {
            indicator.className = `connection-status ${status}`;
            indicator.title = this.getConnectionStatusText(status);
        }

        // Event für andere Module
        if (this.webui && this.webui.emit) {
            this.webui.emit('connection-status-changed', status);
        }
    }

    /**
     * Verbindungsstatus-Text
     */
    getConnectionStatusText(status) {
        switch (status) {
            case 'connected': return 'Verbunden';
            case 'disconnected': return 'Nicht verbunden';
            case 'error': return 'Verbindungsfehler';
            default: return 'Unbekannt';
        }
    }

    /**
     * Spielerliste aktualisieren
     */
    updatePlayerList(players) {
        const playerList = document.getElementById('player-list');
        if (!playerList) return;

        playerList.innerHTML = '';
        
        players.forEach(player => {
            const playerElement = document.createElement('div');
            playerElement.className = 'player-item';
            playerElement.innerHTML = `
                <img src="/api/player/${player.uuid}/avatar" alt="${player.name}" class="player-avatar">
                <span class="player-name">${this.escapeHtml(player.name)}</span>
                <span class="player-status ${player.status}">${player.status}</span>
            `;
            playerList.appendChild(playerElement);
        });
    }

    /**
     * Performance-Widgets aktualisieren
     */
    updatePerformanceWidgets(data) {
        // CPU Widget
        const cpuWidget = document.getElementById('cpu-widget');
        if (cpuWidget) {
            const percentage = cpuWidget.querySelector('.percentage');
            const progressBar = cpuWidget.querySelector('.progress-bar');
            if (percentage) percentage.textContent = `${data.cpu.toFixed(1)}%`;
            if (progressBar) progressBar.style.width = `${data.cpu}%`;
        }

        // Memory Widget
        const memoryWidget = document.getElementById('memory-widget');
        if (memoryWidget) {
            const percentage = memoryWidget.querySelector('.percentage');
            const progressBar = memoryWidget.querySelector('.progress-bar');
            if (percentage) percentage.textContent = `${data.memory.toFixed(1)}%`;
            if (progressBar) progressBar.style.width = `${data.memory}%`;
        }
    }

    /**
     * Benachrichtigung anzeigen
     */
    showNotification(message, type = 'info', duration = 5000) {
        if (this.webui && this.webui.showNotification) {
            this.webui.showNotification(message, type, duration);
        }
    }

    /**
     * Alert anzeigen
     */
    showAlert(title, message, level, dismissible = true) {
        if (this.webui && this.webui.showAlert) {
            this.webui.showAlert(title, message, level, dismissible);
        }
    }

    /**
     * HTML escapen
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Uptime formatieren
     */
    formatUptime(seconds) {
        const days = Math.floor(seconds / 86400);
        const hours = Math.floor((seconds % 86400) / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        
        if (days > 0) {
            return `${days}d ${hours}h ${minutes}m`;
        } else if (hours > 0) {
            return `${hours}h ${minutes}m`;
        } else {
            return `${minutes}m`;
        }
    }

    /**
     * Request mit Promise senden
     */
    async request(type, data = {}) {
        return new Promise((resolve, reject) => {
            const id = Date.now() + Math.random();
            
            if (!this.pendingRequests) {
                this.pendingRequests = new Map();
            }
            
            this.pendingRequests.set(id, { resolve, reject });
            
            this.send({
                type: type,
                data: data,
                id: id
            });
            
            // Timeout nach 30 Sekunden
            setTimeout(() => {
                if (this.pendingRequests.has(id)) {
                    this.pendingRequests.delete(id);
                    reject(new Error('Request timeout'));
                }
            }, 30000);
        });
    }

    /**
     * WebSocket-Manager zerstören
     */
    destroy() {
        this.stopHeartbeat();
        
        if (this.websocket) {
            this.websocket.close();
            this.websocket = null;
        }
        
        this.eventHandlers.clear();
        this.subscriptions.clear();
        this.messageQueue = [];
        
        if (this.pendingRequests) {
            this.pendingRequests.clear();
        }
    }
}

// Globale Instanz für Zugriff von außen
window.WebUIWebSocketManager = WebUIWebSocketManager;
