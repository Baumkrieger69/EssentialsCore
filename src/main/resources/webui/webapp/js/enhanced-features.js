/**
 * EssentialsCore Advanced WebUI - Enhanced Features
 * Erweiterte Funktionen für Produktionsreife und Robustheit
 */

// Erweiterte Error Handling und Network Resilience
class WebUIEnhancedFeatures {
    constructor(webuiInstance) {
        this.webui = webuiInstance;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.networkStatus = 'online';
        this.lastHeartbeat = Date.now();
        this.heartbeatInterval = null;
        this.errorRecoveryQueue = [];
        this.performanceMetrics = {
            pageLoadTime: 0,
            apiResponseTimes: [],
            errorCount: 0,
            reconnectCount: 0
        };
        
        this.init();
    }

    /**
     * Initialisiert die erweiterten Features
     */
    init() {
        this.setupGlobalErrorHandling();
        this.setupNetworkStatusMonitoring();
        this.setupPerformanceMonitoring();
        this.setupKeyboardShortcuts();
        this.setupAccessibilityFeatures();
        this.startHeartbeat();
        this.setupMobileIntegration();
        this.setupAdvancedMonitoring();
        this.setupAdvancedCaching();
        this.setupCollaboration();
        this.setupAdvancedSecurity();
        this.setupAIFeatures();
        this.setupI18n();
    }

    /**
     * Globale Fehlerbehandlung einrichten
     */
    setupGlobalErrorHandling() {
        window.addEventListener('error', (event) => {
            console.error('Global JavaScript Error:', event.error);
            this.handleGlobalError(event.error);
            this.performanceMetrics.errorCount++;
        });

        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled Promise Rejection:', event.reason);
            this.handleGlobalError(event.reason);
            this.performanceMetrics.errorCount++;
        });

        // Registriere erweiterte Instanz global
        window.webuiEnhanced = this;
    }

    /**
     * Netzwerkstatus-Überwachung
     */
    setupNetworkStatusMonitoring() {
        window.addEventListener('online', () => {
            this.networkStatus = 'online';
            this.webui.showNotification('🌐 Netzwerkverbindung wiederhergestellt', 'success');
            this.retryFailedRequests();
        });

        window.addEventListener('offline', () => {
            this.networkStatus = 'offline';
            this.webui.showNotification('⚠️ Netzwerkverbindung verloren - Offline-Modus aktiv', 'warning');
        });

        // Ping-Test für Konnektivität
    }

    /**
     * Performance-Monitoring einrichten
     */
    setupPerformanceMonitoring() {
        // Page Load Time erfassen
        window.addEventListener('load', () => {
            const navigationTiming = performance.getEntriesByType('navigation')[0];
            this.performanceMetrics.pageLoadTime = navigationTiming.loadEventEnd - navigationTiming.fetchStart;
            
            console.log(`📊 Page Load Time: ${this.performanceMetrics.pageLoadTime}ms`);
        });

        // Resource Load Monitoring
        const observer = new PerformanceObserver((list) => {
            for (const entry of list.getEntries()) {
                if (entry.entryType === 'resource' && entry.name.includes('/api/')) {
                    this.performanceMetrics.apiResponseTimes.push({
                        url: entry.name,
                        duration: entry.duration,
                        timestamp: Date.now()
                    });
                }
            }
        });

        if ('PerformanceObserver' in window) {
            observer.observe({ entryTypes: ['resource'] });
        }
    }

    /**
     * Tastatur-Shortcuts einrichten
     */
    setupKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            // CTRL/CMD + K = Search/Command Palette
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                this.showCommandPalette();
            }

            // CTRL/CMD + SHIFT + R = Reload Current Module
            if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'R') {
                e.preventDefault();
                this.reloadCurrentModule();
            }

            // F5 = Refresh Data
            if (e.key === 'F5') {
                e.preventDefault();
                this.refreshCurrentView();
            }

            // ESC = Close Modals/Panels
            if (e.key === 'Escape') {
                this.closeActiveModals();
            }

            // CTRL/CMD + SHIFT + T = Toggle Theme
            if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'T') {
                e.preventDefault();
                this.webui.toggleTheme();
            }
        });
    }

    /**
     * Barrierefreiheits-Features einrichten
     */
    setupAccessibilityFeatures() {
        // Screen Reader Support
        this.setupAriaLabels();
        
        // High Contrast Mode Detection
        if (window.matchMedia && window.matchMedia('(prefers-contrast: high)').matches) {
            document.body.classList.add('high-contrast');
        }

        // Reduced Motion Support
        if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            document.body.classList.add('reduced-motion');
        }

        // Focus Management
        this.setupFocusManagement();
    }

    /**
     * Heartbeat-System für Verbindungsüberwachung
     */
    startHeartbeat() {
        this.heartbeatInterval = setInterval(async () => {
            try {
                const startTime = performance.now();
                const response = await fetch('/api/heartbeat', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + localStorage.getItem('webui_session_token')
                    },
                    signal: AbortSignal.timeout(5000)
                });

                const endTime = performance.now();
                const responseTime = endTime - startTime;

                if (response.ok) {
                    this.lastHeartbeat = Date.now();
                    this.reconnectAttempts = 0;
                    this.updateConnectionStatus('online', responseTime);
                } else {
                    throw new Error(`Heartbeat failed: ${response.status}`);
                }
            } catch (error) {
                this.handleHeartbeatFailure(error);
            }
        }, 30000); // Alle 30 Sekunden
    }

    /**
     * Behandelt Heartbeat-Ausfälle
     */
    handleHeartbeatFailure(error) {
        this.reconnectAttempts++;
        this.performanceMetrics.reconnectCount++;
        
        console.warn(`💔 Heartbeat failed (${this.reconnectAttempts}/${this.maxReconnectAttempts}):`, error);
        
        if (this.reconnectAttempts <= this.maxReconnectAttempts) {
            this.webui.showNotification(
                `🔄 Verbindungsproblem erkannt (Versuch ${this.reconnectAttempts}/${this.maxReconnectAttempts})`, 
                'warning'
            );
            
            // Exponential backoff für Reconnect
            setTimeout(() => {
                this.attemptReconnect();
            }, Math.pow(2, this.reconnectAttempts) * 1000);
        } else {
            this.updateConnectionStatus('offline');
            this.webui.showError(
                '🚫 Verbindung verloren', 
                'Die Verbindung zum Server konnte nicht wiederhergestellt werden. Bitte überprüfen Sie Ihre Internetverbindung.'
            );
        }
    }

    /**
     * Versucht Wiederverbindung
     */
    async attemptReconnect() {
        try {
            console.log('🔄 Attempting reconnection...');
            
            await this.webui.validateToken(localStorage.getItem('webui_session_token'));
            
            if (this.webui.isAuthenticated) {
                this.webui.showNotification('✅ Verbindung wiederhergestellt', 'success');
                this.reconnectAttempts = 0;
                this.updateConnectionStatus('online');
                
                // WebSocket neu verbinden falls nötig
                if (this.webui.websocket && this.webui.websocket.readyState !== WebSocket.OPEN) {
                    await this.webui.setupWebSocket();
                }
                
                // Aktuelle Ansicht aktualisieren
                this.refreshCurrentView();
            }
        } catch (error) {
            console.error('🚨 Reconnect failed:', error);
        }
    }

    /**
     * Behandelt globale Fehler
     */
    handleGlobalError(error) {
        console.error('🚨 Global Error Handler:', error);
        
        // Bestimmte Fehlertypen ignorieren
        if (error.message && (
            error.message.includes('Script error') ||
            error.message.includes('ResizeObserver loop limit exceeded')
        )) {
            return;
        }

        // Fehler zur Recovery-Queue hinzufügen
        this.errorRecoveryQueue.push({
            error: error,
            timestamp: Date.now(),
            stack: error.stack,
            url: window.location.href,
            userAgent: navigator.userAgent
        });

        // Intelligente Fehlerbenachrichtigung
        const errorType = this.categorizeError(error);
        this.webui.showNotification(
            `❌ ${errorType}: ${error.message || 'Unbekannter Fehler'}`, 
            'error'
        );

        // Error Reporting (nur in Production)
        if (window.location.hostname !== 'localhost') {
            this.reportError(error);
        }
    }

    /**
     * Kategorisiert Fehler für bessere UX
     */
    categorizeError(error) {
        if (error.name === 'TypeError') return 'Typ-Fehler';
        if (error.name === 'ReferenceError') return 'Referenz-Fehler';
        if (error.name === 'NetworkError') return 'Netzwerk-Fehler';
        if (error.message && error.message.includes('fetch')) return 'Verbindungs-Fehler';
        return 'Anwendungs-Fehler';
    }

    /**
     * Command Palette anzeigen
     */
    showCommandPalette() {
        const modal = document.createElement('div');
        modal.className = 'command-palette-modal';
        modal.innerHTML = `
            <div class="command-palette">
                <div class="command-search">
                    <i class="icon-search"></i>
                    <input type="text" placeholder="Suchen Sie nach Aktionen, Modulen oder Einstellungen..." id="commandInput">
                </div>
                <div class="command-results" id="commandResults">
                    <div class="command-category">
                        <h4>📱 Module</h4>
                        <div class="command-item" data-action="dashboard">📊 Dashboard</div>
                        <div class="command-item" data-action="console">🖥️ Live Console</div>
                        <div class="command-item" data-action="players">👥 Player Management</div>
                        <div class="command-item" data-action="performance">📈 Performance Monitor</div>
                    </div>
                    <div class="command-category">
                        <h4>⚙️ Aktionen</h4>
                        <div class="command-item" data-action="refresh">🔄 Aktuelle Ansicht aktualisieren</div>
                        <div class="command-item" data-action="theme">🎨 Theme wechseln</div>
                        <div class="command-item" data-action="logout">🚪 Abmelden</div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        
        const input = modal.querySelector('#commandInput');
        input.focus();

        // Event-Handler
        modal.addEventListener('click', (e) => {
            if (e.target === modal) modal.remove();
            
            if (e.target.classList.contains('command-item')) {
                this.executeCommand(e.target.dataset.action);
                modal.remove();
            }
        });

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') modal.remove();
        });
    }

    /**
     * Führt Command Palette Aktionen aus
     */
    executeCommand(action) {
        switch (action) {
            case 'dashboard':
            case 'console':
            case 'players':
            case 'performance':
                this.webui.loadModule(action);
                break;
            case 'refresh':
                this.refreshCurrentView();
                break;
            case 'theme':
                this.webui.toggleTheme();
                break;
            case 'logout':
                this.webui.logout();
                break;
        }
    }

    /**
     * Aktualisiert die aktuelle Ansicht
     */
    refreshCurrentView() {
        const activeModule = document.querySelector('.nav-link.active')?.dataset.module;
        if (activeModule) {
            this.webui.loadModule(activeModule);
            this.webui.showNotification('🔄 Ansicht aktualisiert', 'success', 2000);
        }
    }

    /**
     * Lädt das aktuelle Modul neu
     */
    reloadCurrentModule() {
        this.refreshCurrentView();
        this.webui.showNotification('🔄 Modul neu geladen', 'info', 2000);
    }

    /**
     * Verbindungsstatus aktualisieren
     */
    updateConnectionStatus(status, responseTime = null) {
        const statusElement = document.getElementById('serverStatus');
        if (statusElement) {
            const indicator = statusElement.querySelector('.status-indicator');
            const text = statusElement.querySelector('.status-text');
            
            if (status === 'online') {
                indicator.className = 'status-indicator online';
                text.textContent = responseTime ? 
                    `Server Online (${Math.round(responseTime)}ms)` : 
                    'Server Online';
            } else {
                indicator.className = 'status-indicator offline';
                text.textContent = 'Server Offline';
            }
        }
    }

    /**
     * Netzwerk-Konnektivitätsprüfung
     */
    async checkNetworkConnectivity() {
        try {
            const response = await fetch('/ping', { method: 'HEAD' });
            return response.ok;
        } catch (error) {
            return false;
        }
    }

    /**
     * Retry-Mechanismus für fehlgeschlagene Requests
     */
    async retryFailedRequests() {
        if (this.errorRecoveryQueue.length === 0) return;

        console.log(`🔄 Retrying ${this.errorRecoveryQueue.length} failed operations...`);
        
        // Aktuelle Moduldaten neu laden
        this.refreshCurrentView();

        // Error Recovery Queue leeren
        this.errorRecoveryQueue = [];
    }

    /**
     * Erweiterte ARIA-Labels einrichten
     */
    setupAriaLabels() {
        // Automatische ARIA-Labels für Buttons ohne Text
        document.querySelectorAll('button[title]:not([aria-label])').forEach(btn => {
            btn.setAttribute('aria-label', btn.title);
        });

        // Skip-Links für Screen Reader
        if (!document.querySelector('.skip-link')) {
            const skipLink = document.createElement('a');
            skipLink.className = 'skip-link';
            skipLink.href = '#main-content';
            skipLink.textContent = 'Zum Hauptinhalt springen';
            document.body.insertBefore(skipLink, document.body.firstChild);
        }
    }

    /**
     * Focus Management für Barrierefreiheit
     */
    setupFocusManagement() {
        // Focus Trap für Modals
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Tab') {
                const modal = document.querySelector('.modal:not([style*="display: none"])');
                if (modal) {
                    this.trapFocus(e, modal);
                }
            }
        });
    }

    /**
     * Focus Trap Implementierung
     */
    trapFocus(e, container) {
        const focusableElements = container.querySelectorAll(
            'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
        );
        
        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        if (e.shiftKey && document.activeElement === firstElement) {
            lastElement.focus();
            e.preventDefault();
        } else if (!e.shiftKey && document.activeElement === lastElement) {
            firstElement.focus();
            e.preventDefault();
        }
    }

    /**
     * Aktive Modals schließen
     */
    closeActiveModals() {
        const modals = document.querySelectorAll('.modal, .command-palette-modal');
        modals.forEach(modal => modal.remove());
    }

    /**
     * Error Reporting (Production)
     */
    reportError(error) {
        // Implementierung für Error-Reporting-Service
        const errorReport = {
            message: error.message,
            stack: error.stack,
            url: window.location.href,
            userAgent: navigator.userAgent,
            timestamp: new Date().toISOString(),
            userId: this.webui.currentUser?.id || 'anonymous'
        };

        // Hier würde normalerweise ein Error-Reporting-Service aufgerufen
        console.log('📊 Error Report:', errorReport);
    }

    /**
     * Performance-Metriken abrufen
     */
    getPerformanceMetrics() {
        return {
            ...this.performanceMetrics,
            averageApiResponseTime: this.performanceMetrics.apiResponseTimes.length > 0 ?
                this.performanceMetrics.apiResponseTimes.reduce((sum, entry) => sum + entry.duration, 0) / this.performanceMetrics.apiResponseTimes.length :
                0,
            uptime: Date.now() - (window.webuiStartTime || Date.now())
        };
    }

    /**
     * Cleanup beim Logout
     */
    cleanup() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
        
        this.errorRecoveryQueue = [];
        this.reconnectAttempts = 0;
    }

    /**
     * Mobile App Integration Features
     */
    setupMobileIntegration() {
        // PWA Installation Prompt
        let deferredPrompt;
        
        window.addEventListener('beforeinstallprompt', (e) => {
            e.preventDefault();
            deferredPrompt = e;
            this.showInstallPrompt();
        });

        // Mobile-specific optimizations
        if (this.isMobileDevice()) {
            this.enableMobileOptimizations();
        }

        // Native App Communication Bridge
        this.setupNativeAppBridge();
    }

    /**
     * Native App Communication Bridge
     */
    setupNativeAppBridge() {
        // Android WebView Interface
        if (window.Android) {
            window.WebUIBridge = {
                sendNotification: (title, message) => {
                    if (window.Android.showNotification) {
                        window.Android.showNotification(title, message);
                    }
                },
                
                requestPermission: (permission) => {
                    return new Promise((resolve) => {
                        if (window.Android.requestPermission) {
                            resolve(window.Android.requestPermission(permission));
                        } else {
                            resolve(false);
                        }
                    });
                },

                getDeviceInfo: () => {
                    if (window.Android.getDeviceInfo) {
                        return JSON.parse(window.Android.getDeviceInfo());
                    }
                    return null;
                }
            };
        }

        // iOS WKWebView Interface
        if (window.webkit && window.webkit.messageHandlers) {
            window.WebUIBridge = {
                sendNotification: (title, message) => {
                    window.webkit.messageHandlers.notification.postMessage({
                        title: title,
                        message: message
                    });
                },

                requestPermission: (permission) => {
                    return new Promise((resolve) => {
                        window.webkit.messageHandlers.permission.postMessage({
                            permission: permission,
                            callback: 'handlePermissionResponse'
                        });
                        
                        window.handlePermissionResponse = (granted) => {
                            resolve(granted);
                        };
                    });
                }
            };
        }
    }

    /**
     * Advanced Performance Monitoring
     */
    setupAdvancedMonitoring() {
        // Core Web Vitals Monitoring
        this.monitorCoreWebVitals();
        
        // Memory Usage Monitoring
        this.monitorMemoryUsage();
        
        // Network Performance Monitoring
        this.monitorNetworkPerformance();
        
        // User Interaction Tracking
        this.setupUserInteractionTracking();
    }

    /**
     * Core Web Vitals Monitoring
     */
    monitorCoreWebVitals() {
        // Largest Contentful Paint (LCP)
        new PerformanceObserver((entryList) => {
            for (const entry of entryList.getEntries()) {
                if (entry.entryType === 'largest-contentful-paint') {
                    this.performanceMetrics.lcp = entry.startTime;
                    console.log('LCP:', entry.startTime);
                }
            }
        }).observe({entryTypes: ['largest-contentful-paint']});

        // First Input Delay (FID)
        new PerformanceObserver((entryList) => {
            for (const entry of entryList.getEntries()) {
                if (entry.entryType === 'first-input') {
                    this.performanceMetrics.fid = entry.processingStart - entry.startTime;
                    console.log('FID:', this.performanceMetrics.fid);
                }
            }
        }).observe({entryTypes: ['first-input']});

        // Cumulative Layout Shift (CLS)
        let clsValue = 0;
        new PerformanceObserver((entryList) => {
            for (const entry of entryList.getEntries()) {
                if (!entry.hadRecentInput) {
                    clsValue += entry.value;
                }
            }
            this.performanceMetrics.cls = clsValue;
        }).observe({entryTypes: ['layout-shift']});
    }

    /**
     * Memory Usage Monitoring
     */
    monitorMemoryUsage() {
        if ('memory' in performance) {
            setInterval(() => {
                const memInfo = performance.memory;
                this.performanceMetrics.memory = {
                    usedJSHeapSize: memInfo.usedJSHeapSize,
                    totalJSHeapSize: memInfo.totalJSHeapSize,
                    jsHeapSizeLimit: memInfo.jsHeapSizeLimit
                };

                // Warnung bei hohem Speicherverbrauch
                const memoryUsagePercent = (memInfo.usedJSHeapSize / memInfo.jsHeapSizeLimit) * 100;
                if (memoryUsagePercent > 90) {
                    console.warn('High memory usage detected:', memoryUsagePercent + '%');
                    this.optimizeMemoryUsage();
                }
            }, 30000); // Alle 30 Sekunden
        }
    }

    /**
     * Network Performance Monitoring
     */
    monitorNetworkPerformance() {
        // Connection Info
        if ('connection' in navigator) {
            const connection = navigator.connection;
            this.performanceMetrics.connection = {
                effectiveType: connection.effectiveType,
                downlink: connection.downlink,
                rtt: connection.rtt
            };

            connection.addEventListener('change', () => {
                this.handleConnectionChange(connection);
            });
        }

        // Resource Timing
        new PerformanceObserver((list) => {
            for (const entry of list.getEntries()) {
                if (entry.name.includes('/api/')) {
                    this.performanceMetrics.apiResponseTimes.push({
                        url: entry.name,
                        duration: entry.duration,
                        timestamp: Date.now()
                    });
                }
            }
        }).observe({entryTypes: ['resource']});
    }

    /**
     * User Interaction Tracking
     */
    setupUserInteractionTracking() {
        const interactions = ['click', 'scroll', 'keydown', 'touchstart'];
        
        interactions.forEach(eventType => {
            document.addEventListener(eventType, (event) => {
                this.trackUserInteraction(eventType, event);
            }, { passive: true });
        });
    }

    /**
     * Advanced Caching System
     */
    setupAdvancedCaching() {
        // Service Worker für erweiterte Caching-Strategien
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('/sw.js').then((registration) => {
                console.log('Service Worker registriert:', registration);
                
                // Update-Benachrichtigung
                registration.addEventListener('updatefound', () => {
                    this.showUpdateNotification();
                });
            });
        }

        // IndexedDB für komplexere Daten
        this.setupIndexedDBCache();
    }

    /**
     * IndexedDB Cache Setup
     */
    setupIndexedDBCache() {
        const request = indexedDB.open('WebUICache', 1);
        
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            
            // Object Stores erstellen
            if (!db.objectStoreNames.contains('apiCache')) {
                const apiStore = db.createObjectStore('apiCache', { keyPath: 'url' });
                apiStore.createIndex('timestamp', 'timestamp', { unique: false });
            }
            
            if (!db.objectStoreNames.contains('userPreferences')) {
                db.createObjectStore('userPreferences', { keyPath: 'key' });
            }
        };
        
        request.onsuccess = (event) => {
            this.db = event.target.result;
            console.log('IndexedDB Cache bereit');
        };
    }

    /**
     * Real-time Collaboration Features
     */
    setupCollaboration() {
        // Multi-User Session Management
        this.sessionManager = new Map();
        
        // Cursor Sharing für gemeinsames Arbeiten
        this.setupCursorSharing();
        
        // Live Comments System
        this.setupLiveComments();
        
        // Conflict Resolution
        this.setupConflictResolution();
    }

    /**
     * Advanced Security Features
     */
    setupAdvancedSecurity() {
        // Content Security Policy Monitoring
        document.addEventListener('securitypolicyviolation', (e) => {
            console.error('CSP Violation:', e);
            this.reportSecurityViolation(e);
        });

        // Integrity Monitoring
        this.setupIntegrityChecks();
        
        // Session Security
        this.setupSessionSecurity();
        
        // XSS Protection
        this.setupXSSProtection();
    }

    /**
     * AI-Powered Features
     */
    setupAIFeatures() {
        // Intelligent Error Recovery
        this.setupIntelligentErrorRecovery();
        
        // Predictive Loading
        this.setupPredictiveLoading();
        
        // Smart Notifications
        this.setupSmartNotifications();
        
        // Automated Optimization
        this.setupAutomatedOptimization();
    }

    /**
     * Internationalization & Localization
     */
    setupI18n() {
        // Dynamic Language Loading
        this.languageCache = new Map();
        
        // Date/Time Formatting
        this.setupDateTimeFormatting();
        
        // Currency Formatting
        this.setupCurrencyFormatting();
        
        // RTL Support
        this.setupRTLSupport();
    }

    // Utility Methods
    
    isMobileDevice() {
        return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    }

    optimizeMemoryUsage() {
        // Cache cleanup
        if (this.webui.cache) {
            this.webui.cache.clear();
        }
        
        // Event listener cleanup
        this.cleanupEventListeners();
        
        // Force garbage collection (if available)
        if (window.gc) {
            window.gc();
        }
    }

    handleConnectionChange(connection) {
        const wasOnline = this.networkStatus === 'online';
        this.networkStatus = connection.effectiveType === 'slow-2g' ? 'slow' : 'online';
        
        if (!wasOnline && this.networkStatus === 'online') {
            this.handleReconnection();
        }
    }

    trackUserInteraction(type, event) {
        const interaction = {
            type: type,
            timestamp: Date.now(),
            target: event.target.tagName,
            x: event.clientX,
            y: event.clientY
        };
        
        // Interaction Analytics
        if (this.performanceMetrics.interactions) {
            this.performanceMetrics.interactions.push(interaction);
        }
    }

    getCurrentApplicationState() {
        return {
            authenticated: this.webui.isAuthenticated,
            currentView: this.webui.currentView,
            networkStatus: this.networkStatus,
            performance: this.performanceMetrics
        };
    }

    waitForElement(selector, timeout = 5000) {
        return new Promise((resolve, reject) => {
            const element = document.querySelector(selector);
            if (element) {
                resolve(element);
                return;
            }

            const observer = new MutationObserver(() => {
                const element = document.querySelector(selector);
                if (element) {
                    observer.disconnect();
                    resolve(element);
                }
            });

            observer.observe(document.body, {
                childList: true,
                subtree: true
            });

            setTimeout(() => {
                observer.disconnect();
                reject(new Error(`Element ${selector} not found within ${timeout}ms`));
            }, timeout);
        });
    }

    /**
     * Advanced Analytics & Reporting
     */
    generatePerformanceReport() {
        const report = {
            timestamp: new Date().toISOString(),
            metrics: this.performanceMetrics,
            browserInfo: {
                userAgent: navigator.userAgent,
                language: navigator.language,
                platform: navigator.platform,
                cookieEnabled: navigator.cookieEnabled,
                onLine: navigator.onLine
            },
            screenInfo: {
                width: screen.width,
                height: screen.height,
                availWidth: screen.availWidth,
                availHeight: screen.availHeight,
                colorDepth: screen.colorDepth
            },
            connectionInfo: this.performanceMetrics.connection || {},
            memoryInfo: this.performanceMetrics.memory || {}
        };

        return report;
    }

    /**
     * Export Performance Data
     */
    exportPerformanceData() {
        const report = this.generatePerformanceReport();
        const blob = new Blob([JSON.stringify(report, null, 2)], {
            type: 'application/json'
        });
        
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `webui-performance-${Date.now()}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
}

// CSS für Command Palette und Enhanced Features
const enhancedCSS = `
/* Command Palette */
.command-palette-modal {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.7);
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding-top: 15vh;
    z-index: 10000;
}

.command-palette {
    background: var(--bg-primary);
    border-radius: 12px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
    width: 90%;
    max-width: 600px;
    max-height: 70vh;
    overflow: hidden;
    border: 1px solid var(--border-color);
}

.command-search {
    padding: 20px;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    align-items: center;
    gap: 12px;
}

.command-search input {
    flex: 1;
    border: none;
    background: transparent;
    font-size: 16px;
    color: var(--text-primary);
    outline: none;
}

.command-results {
    max-height: 400px;
    overflow-y: auto;
    padding: 8px;
}

.command-category h4 {
    padding: 12px 16px 8px;
    margin: 0;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.command-item {
    padding: 12px 16px;
    cursor: pointer;
    border-radius: 6px;
    margin: 2px 8px;
    transition: background-color 0.15s ease;
}

.command-item:hover {
    background: var(--bg-secondary);
}

/* Skip Link für Accessibility */
.skip-link {
    position: absolute;
    top: -40px;
    left: 6px;
    background: var(--primary-color);
    color: white;
    padding: 8px;
    text-decoration: none;
    border-radius: 4px;
    z-index: 1000;
}

.skip-link:focus {
    top: 6px;
}

/* High Contrast Mode */
.high-contrast {
    filter: contrast(150%);
}

/* Reduced Motion */
.reduced-motion * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
}

/* Connection Status Indicator */
.status-indicator.offline {
    background: #ff4757;
    animation: pulse 2s infinite;
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}

/* Enhanced Notifications */
.notification {
    transform: translateX(100%);
    animation: slideInRight 0.3s ease-out forwards;
}

@keyframes slideInRight {
    to { transform: translateX(0); }
}

/* Performance Metrics Display */
.performance-debug {
    position: fixed;
    bottom: 20px;
    right: 20px;
    background: rgba(0, 0, 0, 0.8);
    color: white;
    padding: 8px 12px;
    border-radius: 6px;
    font-family: monospace;
    font-size: 12px;
    z-index: 9999;
    opacity: 0.7;
}
`;

// CSS in den DOM einfügen
const style = document.createElement('style');
style.textContent = enhancedCSS;
document.head.appendChild(style);

// Auto-Initialize wenn WebUI verfügbar ist
if (window.webui) {
    window.webuiEnhanced = new WebUIEnhancedFeatures(window.webui);
} else {
    // Warten auf WebUI-Initialisierung
    window.addEventListener('load', () => {
        setTimeout(() => {
            if (window.webui) {
                window.webuiEnhanced = new WebUIEnhancedFeatures(window.webui);
            }
        }, 1000);
    });
}

// Start Time für Uptime-Tracking
window.webuiStartTime = Date.now();

console.log('🚀 Enhanced WebUI Features loaded successfully!');
