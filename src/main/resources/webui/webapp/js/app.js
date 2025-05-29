/**
 * EssentialsCore Advanced WebUI - Main Application
 * Modernes Dashboard für Server-Management und Monitoring
 */

class EssentialsCoreWebUI {
    constructor() {
        this.isAuthenticated = false;
        this.currentUser = null;
        this.websocket = null;
        this.modules = {};
        this.theme = 'dark';
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.networkStatus = 'online';
        this.lastHeartbeat = Date.now();
        this.heartbeatInterval = null;
        this.errorRecoveryQueue = [];
        this.init();
    }

    /**
     * Initialisiert die WebUI
     */
    async init() {
        try {
            this.setupGlobalErrorHandling();
            this.setupNetworkStatusMonitoring();
            this.setupTheme();
            this.setupRouter();
            this.checkAuthentication();
            
            if (this.isAuthenticated) {
                await this.setupWebSocket();
                this.showDashboard();
                this.startHeartbeat();
            } else {
                this.showLogin();
            }
        } catch (error) {
            console.error('Fehler beim Initialisieren der WebUI:', error);
            this.showError('Initialisierungsfehler', error.message);
        }
    }

    /**
     * Globale Fehlerbehandlung einrichten
     */
    setupGlobalErrorHandling() {
        window.addEventListener('error', (event) => {
            console.error('Global JavaScript Error:', event.error);
            this.handleGlobalError(event.error);
        });

        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled Promise Rejection:', event.reason);
            this.handleGlobalError(event.reason);
        });

        // Registriere globale Instanz für Error Handling
        window.webui = this;
    }

    /**
     * Netzwerkstatus-Überwachung
     */
    setupNetworkStatusMonitoring() {
        window.addEventListener('online', () => {
            this.networkStatus = 'online';
            this.showNotification('Netzwerkverbindung wiederhergestellt', 'success');
            this.retryFailedRequests();
        });

        window.addEventListener('offline', () => {
            this.networkStatus = 'offline';
            this.showNotification('Netzwerkverbindung verloren', 'warning');
        });
    }

    /**
     * Heartbeat-System für Verbindungsüberwachung
     */
    startHeartbeat() {
        this.heartbeatInterval = setInterval(async () => {
            try {
                const response = await fetch('/api/heartbeat', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + localStorage.getItem('webui_session_token')
                    },
                    timeout: 5000
                });

                if (response.ok) {
                    this.lastHeartbeat = Date.now();
                    this.reconnectAttempts = 0;
                } else {
                    throw new Error('Heartbeat failed');
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
        
        if (this.reconnectAttempts <= this.maxReconnectAttempts) {
            console.warn(`Heartbeat failed, attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
            this.showNotification(`Verbindungsproblem erkannt (Versuch ${this.reconnectAttempts})`, 'warning');
            
            // Exponential backoff für Reconnect
            setTimeout(() => {
                this.attemptReconnect();
            }, Math.pow(2, this.reconnectAttempts) * 1000);
        } else {
            this.showError('Verbindung verloren', 'Die Verbindung zum Server konnte nicht wiederhergestellt werden.');
        }
    }

    /**
     * Versucht Wiederverbindung
     */
    async attemptReconnect() {
        try {
            await this.validateToken(localStorage.getItem('webui_session_token'));
            
            if (this.isAuthenticated) {
                this.showNotification('Verbindung wiederhergestellt', 'success');
                this.reconnectAttempts = 0;
                
                // WebSocket neu verbinden
                if (this.websocket && this.websocket.readyState !== WebSocket.OPEN) {
                    await this.setupWebSocket();
                }
            }
        } catch (error) {
            console.error('Reconnect failed:', error);
        }
    }

    /**
     * Behandelt globale Fehler
     */
    handleGlobalError(error) {
        console.error('Global Error Handler:', error);
        
        // Bestimmte Fehlertypen ignorieren
        if (error.message && error.message.includes('Script error')) {
            return; // Cross-origin Script-Fehler ignorieren
        }

        // Fehler zur Recovery-Queue hinzufügen
        this.errorRecoveryQueue.push({
            error: error,
            timestamp: Date.now(),
            stack: error.stack
        });

        // Benachrichtigung anzeigen
        this.showNotification(
            `Unerwarteter Fehler: ${error.message || 'Unbekannter Fehler'}`, 
            'error'
        );
    }

    /**
     * Retry-Mechanismus für fehlgeschlagene Requests
     */
    async retryFailedRequests() {
        if (this.errorRecoveryQueue.length === 0) return;

        console.log(`Retrying ${this.errorRecoveryQueue.length} failed operations...`);
        
        // Aktuelle Moduldaten neu laden
        const currentModule = document.querySelector('.nav-link.active')?.dataset.module;
        if (currentModule) {
            await this.loadModule(currentModule);
        }

        // Error Recovery Queue leeren
        this.errorRecoveryQueue = [];
    }

    /**
     * Initialisiert die WebUI
     */
        constructor() {
            this.isAuthenticated = false;
            this.currentUser = null;
            this.websocket = null;
            this.modules = {};
            this.theme = 'dark';
            this.reconnectAttempts = 0;
            this.maxReconnectAttempts = 5;
            this.networkStatus = 'online';
            this.lastHeartbeat = Date.now();
            this.heartbeatInterval = null;
            this.errorRecoveryQueue = [];
            this.init();
        }

        /**
         * Initialisiert die WebUI
         */
        async init() {
            try {
                this.setupGlobalErrorHandling();
                this.setupNetworkStatusMonitoring();
                this.setupTheme();
                this.setupRouter();
                this.checkAuthentication();
                
                if (this.isAuthenticated) {
                    await this.setupWebSocket();
                    this.showDashboard();
                    this.startHeartbeat();
                } else {
                    this.showLogin();
                }
            } catch (error) {
                console.error('Fehler beim Initialisieren der WebUI:', error);
                this.showError('Initialisierungsfehler', error.message);
            }
        }

        /**
         * Globale Fehlerbehandlung einrichten
         */
        setupGlobalErrorHandling() {
            window.addEventListener('error', (event) => {
                console.error('Global JavaScript Error:', event.error);
                this.handleGlobalError(event.error);
            });

            window.addEventListener('unhandledrejection', (event) => {
                console.error('Unhandled Promise Rejection:', event.reason);
                this.handleGlobalError(event.reason);
            });

            // Registriere globale Instanz für Error Handling
            window.webui = this;
        }

        /**
         * Netzwerkstatus-Überwachung
         */
        setupNetworkStatusMonitoring() {
            window.addEventListener('online', () => {
                this.networkStatus = 'online';
                this.showNotification('Netzwerkverbindung wiederhergestellt', 'success');
                this.retryFailedRequests();
            });

            window.addEventListener('offline', () => {
                this.networkStatus = 'offline';
                this.showNotification('Netzwerkverbindung verloren', 'warning');
            });
        }

        /**
         * Heartbeat-System für Verbindungsüberwachung
         */
        startHeartbeat() {
            this.heartbeatInterval = setInterval(async () => {
                try {
                    const response = await fetch('/api/heartbeat', {
                        method: 'GET',
                        headers: {
                            'Authorization': 'Bearer ' + localStorage.getItem('webui_session_token')
                        },
                        timeout: 5000
                    });

                    if (response.ok) {
                        this.lastHeartbeat = Date.now();
                        this.reconnectAttempts = 0;
                    } else {
                        throw new Error('Heartbeat failed');
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
            
            if (this.reconnectAttempts <= this.maxReconnectAttempts) {
                console.warn(`Heartbeat failed, attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
                this.showNotification(`Verbindungsproblem erkannt (Versuch ${this.reconnectAttempts})`, 'warning');
                
                // Exponential backoff für Reconnect
                setTimeout(() => {
                    this.attemptReconnect();
                }, Math.pow(2, this.reconnectAttempts) * 1000);
            } else {
                this.showError('Verbindung verloren', 'Die Verbindung zum Server konnte nicht wiederhergestellt werden.');
            }
        }

        /**
         * Versucht Wiederverbindung
         */
        async attemptReconnect() {
            try {
                await this.validateToken(localStorage.getItem('webui_session_token'));
                
                if (this.isAuthenticated) {
                    this.showNotification('Verbindung wiederhergestellt', 'success');
                    this.reconnectAttempts = 0;
                    
                    // WebSocket neu verbinden
                    if (this.websocket && this.websocket.readyState !== WebSocket.OPEN) {
                        await this.setupWebSocket();
                    }
                }
            } catch (error) {
                console.error('Reconnect failed:', error);
            }
        }

        /**
         * Behandelt globale Fehler
         */
        handleGlobalError(error) {
            console.error('Global Error Handler:', error);
            
            // Bestimmte Fehlertypen ignorieren
            if (error.message && error.message.includes('Script error')) {
                return; // Cross-origin Script-Fehler ignorieren
            }

            // Fehler zur Recovery-Queue hinzufügen
            this.errorRecoveryQueue.push({
                error: error,
                timestamp: Date.now(),
                stack: error.stack
            });

            // Benachrichtigung anzeigen
            this.showNotification(
                `Unerwarteter Fehler: ${error.message || 'Unbekannter Fehler'}`, 
                'error'
            );
        }

        /**
         * Retry-Mechanismus für fehlgeschlagene Requests
         */
        async retryFailedRequests() {
            if (this.errorRecoveryQueue.length === 0) return;

            console.log(`Retrying ${this.errorRecoveryQueue.length} failed operations...`);
            
            // Aktuelle Moduldaten neu laden
            const currentModule = document.querySelector('.nav-link.active')?.dataset.module;
            if (currentModule) {
                await this.loadModule(currentModule);
            }

            // Error Recovery Queue leeren
            this.errorRecoveryQueue = [];
        }

        /**
         * Initialisiert die WebUI
         */
        async init() {
            try {
                this.setupTheme();
                this.setupRouter();
                this.checkAuthentication();
                
                if (this.isAuthenticated) {
                    await this.setupWebSocket();
                    this.showDashboard();
                } else {
                    this.showLogin();
                }
            } catch (error) {
                console.error('Fehler beim Initialisieren der WebUI:', error);
                this.showError('Initialisierungsfehler', error.message);
            }
        }

        /**
         * Prüft den Authentifizierungsstatus
         */
        checkAuthentication() {
            const token = localStorage.getItem('webui_session_token');
            if (token) {
                // Validiere Token mit Server
                this.validateToken(token);
            }
        }

        /**
         * Validiert den Session-Token
         */
        async validateToken(token) {
            try {
                const response = await fetch('/api/auth/validate', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    }
                });

                if (response.ok) {
                    const data = await response.json();
                    this.isAuthenticated = true;
                    this.currentUser = data.user;
                } else {
                    localStorage.removeItem('webui_session_token');
                    this.isAuthenticated = false;
                }
            } catch (error) {
                console.error('Token-Validierung fehlgeschlagen:', error);
                this.isAuthenticated = false;
            }
        }

        /**
         * Zeigt den Login-Bildschirm
         */
        showLogin() {
            const root = document.getElementById('root');
            root.innerHTML = `
                <div class="login-container">
                    <div class="login-card">
                        <div class="login-header">
                            <img src="assets/logo.png" alt="EssentialsCore" class="logo" onerror="this.style.display='none'">
                            <h1>EssentialsCore WebUI</h1>
                            <p>Advanced Server Management Portal</p>
                        </div>
                        
                        <form id="loginForm" class="login-form">
                            <div class="form-group">
                                <label for="username">Benutzername</label>
                                <input type="text" id="username" name="username" required 
                                       placeholder="Geben Sie Ihren Benutzernamen ein">
                            </div>
                            
                            <div class="form-group">
                                <label for="password">Passwort</label>
                                <input type="password" id="password" name="password" required 
                                       placeholder="Geben Sie Ihr Passwort ein">
                            </div>
                            
                            <div class="form-group">
                                <label class="checkbox-container">
                                    <input type="checkbox" id="rememberMe" name="rememberMe">
                                    <span class="checkmark"></span>
                                    Angemeldet bleiben
                                </label>
                            </div>
                            
                            <button type="submit" class="btn btn-primary btn-block" id="loginBtn">
                                <span class="btn-text">Anmelden</span>
                                <span class="spinner" style="display: none;"></span>
                            </button>
                        </form>
                        
                        <div id="loginError" class="error-message" style="display: none;"></div>
                        
                        <div class="login-footer">
                            <p>EssentialsCore v${this.getVersion()} | Powered by Advanced WebUI</p>
                        </div>
                    </div>
                </div>
            `;

            this.setupLoginForm();
        }

        /**
         * Richtet das Login-Formular ein
         */
        setupLoginForm() {
            const form = document.getElementById('loginForm');
            const loginBtn = document.getElementById('loginBtn');
            const spinner = loginBtn.querySelector('.spinner');
            const btnText = loginBtn.querySelector('.btn-text');

            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                
                const username = form.username.value.trim();
                const password = form.password.value;
                const rememberMe = form.rememberMe.checked;

                if (!username || !password) {
                    this.showLoginError('Bitte geben Sie Benutzername und Passwort ein.');
                    return;
                }

                // Loading-Zustand
                loginBtn.disabled = true;
                spinner.style.display = 'inline-block';
                btnText.textContent = 'Wird angemeldet...';

                try {
                    await this.performLogin(username, password, rememberMe);
                } catch (error) {
                    this.showLoginError(error.message);
                } finally {
                    loginBtn.disabled = false;
                    spinner.style.display = 'none';
                    btnText.textContent = 'Anmelden';
                }
            });
        }

        /**
         * Führt die Anmeldung durch
         */
        async performLogin(username, password, rememberMe) {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    username,
                    password,
                    rememberMe,
                    clientInfo: {
                        userAgent: navigator.userAgent,
                        timestamp: Date.now()
                    }
                })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                localStorage.setItem('webui_session_token', data.token);
                this.isAuthenticated = true;
                this.currentUser = data.user;
                
                await this.setupWebSocket();
                this.showDashboard();
            } else {
                throw new Error(data.message || 'Anmeldung fehlgeschlagen');
            }
        }

        /**
         * Zeigt Login-Fehler an
         */
        showLoginError(message) {
            const errorDiv = document.getElementById('loginError');
            errorDiv.textContent = message;
            errorDiv.style.display = 'block';
            
            setTimeout(() => {
                errorDiv.style.display = 'none';
            }, 5000);
        }

        /**
         * Zeigt das Haupt-Dashboard
         */
        showDashboard() {
            const root = document.getElementById('root');
            root.innerHTML = `
                <div class="app-layout">
                    <aside class="sidebar">
                        <div class="sidebar-header">
                            <img src="assets/logo.png" alt="EssentialsCore" class="sidebar-logo" onerror="this.style.display='none'">
                            <h2>EssentialsCore</h2>
                        </div>
                        
                        <nav class="sidebar-nav">
                            <ul class="nav-menu">
                                <li><a href="#dashboard" class="nav-link active" data-module="dashboard">
                                    <i class="icon-dashboard"></i> Dashboard
                            </a></li>
                            <li><a href="#console" class="nav-link" data-module="console">
                                <i class="icon-terminal"></i> Live Console
                            </a></li>
                            <li><a href="#players" class="nav-link" data-module="players">
                                <i class="icon-users"></i> Player Management
                            </a></li>
                            <li><a href="#modules" class="nav-link" data-module="modules">
                                <i class="icon-puzzle"></i> Module Manager
                            </a></li>
                            <li><a href="#files" class="nav-link" data-module="files">
                                <i class="icon-folder"></i> File Manager
                            </a></li>
                            <li><a href="#performance" class="nav-link" data-module="performance">
                                <i class="icon-chart"></i> Performance
                            </a></li>
                            <li><a href="#security" class="nav-link" data-module="security">
                                <i class="icon-shield"></i> Security
                            </a></li>
                            <li><a href="#settings" class="nav-link" data-module="settings">
                                <i class="icon-settings"></i> Settings
                            </a></li>
                        </ul>
                    </nav>
                    
                    <div class="sidebar-footer">
                        <div class="user-info">
                            <span class="user-name">${this.currentUser?.username || 'Admin'}</span>
                            <button id="logoutBtn" class="btn-logout" title="Abmelden">
                                <i class="icon-logout"></i>
                            </button>
                        </div>
                    </div>
                </aside>
                
                <main class="main-content">
                    <header class="content-header">
                        <div class="header-left">
                            <h1 id="pageTitle">Dashboard</h1>
                            <div class="breadcrumb">
                                <span id="breadcrumbPath">Home / Dashboard</span>
                            </div>
                        </div>
                        
                        <div class="header-right">
                            <div class="server-status" id="serverStatus">
                                <span class="status-indicator online"></span>
                                <span class="status-text">Server Online</span>
                            </div>
                            
                            <button id="themeToggle" class="btn-icon" title="Theme wechseln">
                                <i class="icon-theme"></i>
                            </button>
                            
                            <div class="notification-center" id="notificationCenter">
                                <button class="btn-icon notification-btn">
                                    <i class="icon-bell"></i>
                                    <span class="notification-badge" style="display: none;">0</span>
                                </button>
                            </div>
                        </div>
                    </header>
                    
                    <div id="moduleContent" class="module-content">
                        <!-- Module content wird hier geladen -->
                    </div>
                </main>
            </div>
            
            <!-- Notification Container -->
            <div id="notifications" class="notifications-container"></div>
            
            <!-- Modal Container -->
            <div id="modalContainer" class="modal-container" style="display: none;"></div>
        `;

        this.setupDashboard();
        this.loadModule('dashboard');
    }

    /**
     * Richtet das Dashboard ein
     */
    setupDashboard() {
        // Navigation
        const navLinks = document.querySelectorAll('.nav-link');
        navLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const module = link.dataset.module;
                this.loadModule(module);
                
                // Aktive Klasse aktualisieren
                navLinks.forEach(l => l.classList.remove('active'));
                link.classList.add('active');
            });
        });

        // Logout-Button
        document.getElementById('logoutBtn').addEventListener('click', () => {
            this.logout();
        });

        // Theme-Toggle
        document.getElementById('themeToggle').addEventListener('click', () => {
            this.toggleTheme();
        });
    }

    /**
     * Lädt ein Modul
     */
    async loadModule(moduleName) {
        const moduleContent = document.getElementById('moduleContent');
        const pageTitle = document.getElementById('pageTitle');
        const breadcrumbPath = document.getElementById('breadcrumbPath');
        
        // Loading-Zustand
        moduleContent.innerHTML = '<div class="loading-spinner">Loading...</div>';
        
        try {
            switch (moduleName) {
                case 'dashboard':
                    await this.loadDashboardModule();
                    pageTitle.textContent = 'Dashboard';
                    breadcrumbPath.textContent = 'Home / Dashboard';
                    break;
                    
                case 'console':
                    await this.loadConsoleModule();
                    pageTitle.textContent = 'Live Console';
                    breadcrumbPath.textContent = 'Home / Live Console';
                    break;
                    
                case 'players':
                    await this.loadPlayersModule();
                    pageTitle.textContent = 'Player Management';
                    breadcrumbPath.textContent = 'Home / Player Management';
                    break;
                    
                case 'modules':
                    await this.loadModulesModule();
                    pageTitle.textContent = 'Module Manager';
                    breadcrumbPath.textContent = 'Home / Module Manager';
                    break;
                    
                case 'files':
                    await this.loadFilesModule();
                    pageTitle.textContent = 'File Manager';
                    breadcrumbPath.textContent = 'Home / File Manager';
                    break;
                    
                case 'performance':
                    await this.loadPerformanceModule();
                    pageTitle.textContent = 'Performance Monitor';
                    breadcrumbPath.textContent = 'Home / Performance';
                    break;
                    
                case 'security':
                    await this.loadSecurityModule();
                    pageTitle.textContent = 'Security Center';
                    breadcrumbPath.textContent = 'Home / Security';
                    break;
                    
                case 'settings':
                    await this.loadSettingsModule();
                    pageTitle.textContent = 'Settings';
                    breadcrumbPath.textContent = 'Home / Settings';
                    break;
                    
                default:
                    moduleContent.innerHTML = '<div class="error">Modul nicht gefunden</div>';
            }
        } catch (error) {
            console.error('Fehler beim Laden des Moduls:', error);
            moduleContent.innerHTML = '<div class="error">Fehler beim Laden des Moduls</div>';
        }
    }

    /**
     * Lädt das Dashboard-Modul
     */
    async loadDashboardModule() {
        const moduleContent = document.getElementById('moduleContent');
        
        moduleContent.innerHTML = `
            <div class="dashboard-grid">
                <div class="dashboard-row">
                    <div class="stat-card">
                        <div class="stat-icon server-icon"></div>
                        <div class="stat-content">
                            <h3>Server Status</h3>
                            <p class="stat-value" id="serverStatusValue">Online</p>
                            <p class="stat-change positive">+99.9% Uptime</p>
                        </div>
                    </div>
                    
                    <div class="stat-card">
                        <div class="stat-icon users-icon"></div>
                        <div class="stat-content">
                            <h3>Online Players</h3>
                            <p class="stat-value" id="onlinePlayersValue">0</p>
                            <p class="stat-change neutral">von 100 max</p>
                        </div>
                    </div>
                    
                    <div class="stat-card">
                        <div class="stat-icon performance-icon"></div>
                        <div class="stat-content">
                            <h3>Server TPS</h3>
                            <p class="stat-value" id="serverTpsValue">20.0</p>
                            <p class="stat-change positive">Excellent</p>
                        </div>
                    </div>
                    
                    <div class="stat-card">
                        <div class="stat-icon memory-icon"></div>
                        <div class="stat-content">
                            <h3>Memory Usage</h3>
                            <p class="stat-value" id="memoryUsageValue">0%</p>
                            <p class="stat-change neutral">0 MB / 0 MB</p>
                        </div>
                    </div>
                </div>
                
                <div class="dashboard-row">
                    <div class="card chart-card">
                        <div class="card-header">
                            <h3>Performance Overview</h3>
                            <div class="card-actions">
                                <select id="chartTimeRange">
                                    <option value="1h">Last Hour</option>
                                    <option value="6h">Last 6 Hours</option>
                                    <option value="24h" selected>Last 24 Hours</option>
                                    <option value="7d">Last Week</option>
                                </select>
                            </div>
                        </div>
                        <div class="card-content">
                            <canvas id="performanceChart" width="400" height="200"></canvas>
                        </div>
                    </div>
                </div>
                
                <div class="dashboard-row">
                    <div class="card activity-card">
                        <div class="card-header">
                            <h3>Recent Activity</h3>
                            <button class="btn btn-sm" id="refreshActivity">Refresh</button>
                        </div>
                        <div class="card-content">
                            <div id="activityFeed" class="activity-feed">
                                <!-- Activity items werden hier geladen -->
                            </div>
                        </div>
                    </div>
                    
                    <div class="card modules-card">
                        <div class="card-header">
                            <h3>Module Status</h3>
                            <a href="#modules" class="btn btn-sm">Manage All</a>
                        </div>
                        <div class="card-content">
                            <div id="moduleStatusList" class="module-status-list">
                                <!-- Module status wird hier geladen -->
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Dashboard-Daten laden
        this.loadDashboardData();
        this.startDashboardUpdates();
    }

    /**
     * Lädt das Console-Modul
     */
    async loadConsoleModule() {
        const moduleContent = document.getElementById('moduleContent');
        
        moduleContent.innerHTML = `
            <div class="console-module">
                <div class="console-container">
                    <div class="console-header">
                        <div class="console-controls">
                            <button class="btn btn-secondary" id="clearConsoleBtn">
                                <i class="icon-trash"></i> Clear
                            </button>
                            <button class="btn btn-secondary" id="exportConsoleBtn">
                                <i class="icon-download"></i> Export
                            </button>
                            <label class="toggle-switch">
                                <input type="checkbox" id="autoScrollToggle" checked>
                                <span class="slider"></span>
                                <span class="label">Auto-scroll</span>
                            </label>
                        </div>
                    </div>
                    
                    <div class="console-output" id="consoleOutput"></div>
                    
                    <div class="console-input">
                        <form id="commandForm" class="command-form">
                            <input type="text" id="commandInput" placeholder="Enter server command..." 
                                   class="command-input" autocomplete="off">
                            <button type="submit" class="btn btn-primary">
                                <i class="icon-send"></i> Execute
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        `;

        this.setupConsoleModule();
    }

    /**
     * Richtet das Console-Modul ein
     */
    setupConsoleModule() {
        // Clear Console
        document.getElementById('clearConsoleBtn').addEventListener('click', () => {
            this.clearConsole();
        });

        // Export Console
        document.getElementById('exportConsoleBtn').addEventListener('click', () => {
            this.exportConsole();
        });

        // Command Form
        document.getElementById('commandForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.executeCommand();
        });

        // WebSocket für Console-Updates einrichten
        this.subscribeToConsole();
        
        // Console-History laden
        this.loadConsoleHistory();
    }

    /**
     * Lädt das Players-Modul
     */
    async loadPlayersModule() {
        const moduleContent = document.getElementById('moduleContent');
        
        moduleContent.innerHTML = `
            <div class="players-module">
                <div class="players-header">
                    <div class="players-tabs">
                        <button class="tab-btn active" data-tab="online">Online Players</button>
                        <button class="tab-btn" data-tab="offline">Recent Players</button>
                        <button class="tab-btn" data-tab="bans">Bans & Kicks</button>
                    </div>
                    
                    <div class="players-controls">
                        <input type="text" id="playerSearch" placeholder="Search players..." class="search-input">
                        <button class="btn btn-primary" id="refreshPlayersBtn">
                            <i class="icon-refresh"></i> Refresh
                        </button>
                    </div>
                </div>
                
                <div class="tab-content">
                    <div id="onlineTab" class="tab-pane active">
                        <div class="players-actions">
                            <button class="btn btn-secondary" id="kickAllBtn">Kick All</button>
                            <button class="btn btn-warning" id="messageAllBtn">Message All</button>
                        </div>
                        <div class="players-list" id="onlinePlayersList"></div>
                    </div>
                    
                    <div id="offlineTab" class="tab-pane">
                        <div class="players-list" id="offlinePlayersList"></div>
                    </div>
                    
                    <div id="bansTab" class="tab-pane">
                        <div class="bans-actions">
                            <button class="btn btn-success" id="addBanBtn">Add Ban</button>
                            <button class="btn btn-warning" id="clearExpiredBansBtn">Clear Expired</button>
                        </div>
                        <div class="players-list" id="bansList"></div>
                    </div>
                </div>
            </div>
        `;

        this.setupPlayersModule();
    }

    /**
     * Richtet das Players-Modul ein
     */
    setupPlayersModule() {
        // Tab-Navigation
        const tabBtns = document.querySelectorAll('.tab-btn');
        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.dataset.tab;
                this.switchPlayersTab(tab);
            });
        });

        // Search
        document.getElementById('playerSearch').addEventListener('input', (e) => {
            this.searchPlayers(e.target.value);
        });

        // Refresh
        document.getElementById('refreshPlayersBtn').addEventListener('click', () => {
            this.refreshPlayers();
        });

        // Bulk Actions
        document.getElementById('kickAllBtn').addEventListener('click', () => {
            this.kickAllPlayers();
        });
        
        document.getElementById('messageAllBtn').addEventListener('click', () => {
            this.messageAllPlayers();
        });

        // Load initial data
        this.loadOnlinePlayers();
    }

    /**
     * Lädt das Modules-Modul
     */
    async loadModulesModule() {
        const moduleContent = document.getElementById('moduleContent');
        
        moduleContent.innerHTML = `
            <div class="modules-module">
                <div class="modules-header">
                    <div class="modules-tabs">
                        <button class="tab-btn active" data-tab="loaded">Loaded Modules</button>
                        <button class="tab-btn" data-tab="available">Available Modules</button>
                        <button class="tab-btn" data-tab="performance">Performance</button>
                    </div>
                    
                    <div class="modules-controls">
                        <button class="btn btn-primary" id="reloadAllModulesBtn">
                            <i class="icon-refresh"></i> Reload All
                        </button>
                        <button class="btn btn-success" id="loadModuleBtn">
                            <i class="icon-plus"></i> Load Module
                        </button>
                    </div>
                </div>
                
                <div class="tab-content">
                    <div id="loadedTab" class="tab-pane active">
                        <div class="modules-list" id="loadedModulesList"></div>
                    </div>
                    
                    <div id="availableTab" class="tab-pane">
                        <div class="modules-list" id="availableModulesList"></div>
                    </div>
                    
                    <div id="performanceTab" class="tab-pane">
                        <div class="performance-overview" id="modulePerformanceOverview"></div>
                    </div>
                </div>
            </div>
        `;

        this.setupModulesModule();
    }

    /**
     * Richtet das Modules-Modul ein
     */
    setupModulesModule() {
        // Tab-Navigation
        const tabBtns = document.querySelectorAll('.tab-btn');
        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.dataset.tab;
                this.switchModulesTab(tab);
            });
        });

        // Controls
        document.getElementById('reloadAllModulesBtn').addEventListener('click', () => {
            this.reloadAllModules();
        });
        
        document.getElementById('loadModuleBtn').addEventListener('click', () => {
            this.showLoadModuleDialog();
        });

        // Load initial data
        this.loadModules();
    }

    /**
     * Lädt das Files-Modul
     */
    async loadFilesModule() {
        const moduleContent = document.getElementById('moduleContent');
        
        moduleContent.innerHTML = `
            <div class="files-module">
                <div class="files-header">
                    <div class="breadcrumb-nav" id="fileBreadcrumb">
                        <span class="breadcrumb-item active">Server Root</span>
                    </div>
                    
                    <div class="files-controls">
                        <div class="view-toggle">
                            <button class="btn btn-secondary active" id="listViewBtn">
                                <i class="icon-list"></i> List
                            </button>
                            <button class="btn btn-secondary" id="gridViewBtn">
                                <i class="icon-grid"></i> Grid
                            </button>
                        </div>
                        
                        <input type="text" id="fileSearch" placeholder="Search files..." class="search-input">
                        
                        <button class="btn btn-primary" id="uploadFileBtn">
                            <i class="icon-upload"></i> Upload
                        </button>
                        <button class="btn btn-success" id="newFolderBtn">
                            <i class="icon-folder-plus"></i> New Folder
                        </button>
                    </div>
                </div>
                
                <div class="files-layout">
                    <div class="files-sidebar">
                        <h3>Quick Access</h3>
                        <ul class="quick-access-list">
                            <li><a href="#" data-path="/">Server Root</a></li>
                            <li><a href="#" data-path="/plugins">Plugins</a></li>
                            <li><a href="#" data-path="/worlds">Worlds</a></li>
                            <li><a href="#" data-path="/logs">Logs</a></li>
                            <li><a href="#" data-path="/config">Config</a></li>
                        </ul>
                    </div>
                    
                    <div class="files-content">
                        <div class="files-list" id="filesList"></div>
                    </div>
                </div>
            </div>
        `;

        this.setupFilesModule();
    }

    /**
     * Richtet das Files-Modul ein
     */
    setupFilesModule() {
        this.currentPath = '/';
        
        // View Toggle
        document.getElementById('listViewBtn').addEventListener('click', () => {
            this.setFilesView('list');
        });
        
        document.getElementById('gridViewBtn').addEventListener('click', () => {
            this.setFilesView('grid');
        });

        // Search
        document.getElementById('fileSearch').addEventListener('input', (e) => {
            this.searchFiles(e.target.value);
        });

        // Controls
        document.getElementById('uploadFileBtn').addEventListener('click', () => {
            this.showUploadDialog();
        });
        
        document.getElementById('newFolderBtn').addEventListener('click', () => {
            this.showNewFolderDialog();
        });

        // Quick Access
        const quickAccessLinks = document.querySelectorAll('.quick-access-list a');
        quickAccessLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                this.navigateToPath(link.dataset.path);
            });
        });

        // Load initial directory
        this.loadDirectory(this.currentPath);
    }

    /**
     * Lädt das Performance-Modul
     */
    async loadPerformanceModule() {
        const moduleContent = document.getElementById('moduleContent');
        
        moduleContent.innerHTML = `
            <div class="performance-module">
                <div class="performance-header">
                    <div class="performance-tabs">
                        <button class="tab-btn active" data-tab="overview">Overview</button>
                        <button class="tab-btn" data-tab="server">Server Metrics</button>
                        <button class="tab-btn" data-tab="modules">Module Performance</button>
                        <button class="tab-btn" data-tab="alerts">Alerts</button>
                    </div>
                    
                    <div class="performance-controls">
                        <select id="performanceTimeRange" class="select-input">
                            <option value="1h">Last Hour</option>
                            <option value="24h" selected>Last 24 Hours</option>
                            <option value="7d">Last 7 Days</option>
                            <option value="30d">Last 30 Days</option>
                        </select>
                        
                        <button class="btn btn-primary" id="refreshPerformanceBtn">
                            <i class="icon-refresh"></i> Refresh
                        </button>
                    </div>
                </div>
                
                <div class="tab-content">
                    <div id="overviewTab" class="tab-pane active">
                        <div class="metrics-grid">
                            <div class="metric-card">
                                <h3>CPU Usage</h3>
                                <div class="metric-value" id="cpuUsage">0%</div>
                                <div class="metric-chart" id="cpuChart"></div>
                            </div>
                            
                            <div class="metric-card">
                                <h3>Memory Usage</h3>
                                <div class="metric-value" id="memoryUsage">0%</div>
                                <div class="metric-chart" id="memoryChart"></div>
                            </div>
                            
                            <div class="metric-card">
                                <h3>Server TPS</h3>
                                <div class="metric-value" id="serverTps">20.0</div>
                                <div class="metric-chart" id="tpsChart"></div>
                            </div>
                            
                            <div class="metric-card">
                                <h3>Active Threads</h3>
                                <div class="metric-value" id="activeThreads">0</div>
                                <div class="metric-chart" id="threadsChart"></div>
                            </div>
                        </div>
                    </div>
                    
                    <div id="serverTab" class="tab-pane">
                        <div class="server-metrics" id="serverMetrics"></div>
                    </div>
                    
                    <div id="modulesTab" class="tab-pane">
                        <div class="module-performance-list" id="modulePerformanceList"></div>
                    </div>
                    
                    <div id="alertsTab" class="tab-pane">
                        <div class="performance-alerts" id="performanceAlerts"></div>
                    </div>
                </div>
            </div>
        `;

        this.setupPerformanceModule();
    }

    /**
     * Richtet das Performance-Modul ein
     */
    setupPerformanceModule() {
        // Tab-Navigation
        const tabBtns = document.querySelectorAll('.tab-btn');
        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.dataset.tab;
                this.switchPerformanceTab(tab);
            });
        });

        // Time Range
        document.getElementById('performanceTimeRange').addEventListener('change', (e) => {
            this.updatePerformanceTimeRange(e.target.value);
        });

        // Refresh
        document.getElementById('refreshPerformanceBtn').addEventListener('click', () => {
            this.refreshPerformanceData();
        });

        // Load initial data
        this.loadPerformanceOverview();
    }

    // ===== HELPER FUNCTIONS =====

    /**
     * Lädt Online-Spieler
     */
    async loadOnlinePlayers() {
        try {
            const response = await fetch('/api/players/online');
            const players = await response.json();
            
            const playersList = document.getElementById('onlinePlayersList');
            if (!playersList) return;
            
            playersList.innerHTML = players.map(player => `
                <div class="player-card">
                    <div class="player-avatar">
                        <img src="https://crafatar.com/avatars/${player.uuid}?size=32" alt="${player.name}">
                    </div>
                    <div class="player-info">
                        <h4>${player.name}</h4>
                        <p>Playing for ${this.formatDuration(player.playtime)}</p>
                        <span class="player-status">${player.world}</span>
                    </div>
                    <div class="player-actions">
                        <button class="btn btn-sm btn-secondary" onclick="webui.messagePlayer('${player.uuid}')">
                            <i class="icon-message"></i>
                        </button>
                        <button class="btn btn-sm btn-warning" onclick="webui.kickPlayer('${player.uuid}')">
                            <i class="icon-kick"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="webui.banPlayer('${player.uuid}')">
                            <i class="icon-ban"></i>
                        </button>
                    </div>
                </div>
            `).join('');
            
        } catch (error) {
            console.error('Fehler beim Laden der Online-Spieler:', error);
        }
    }

    /**
     * Lädt Verzeichnis-Inhalt
     */
    async loadDirectory(path) {
        try {
            const response = await fetch(`/api/files/directory?path=${encodeURIComponent(path)}`);
            const data = await response.json();
            
            this.currentPath = path;
            this.updateBreadcrumb(path);
            
            const filesList = document.getElementById('filesList');
            if (!filesList) return;
            
            filesList.innerHTML = data.files.map(file => `
                <div class="file-item ${file.type}" data-name="${file.name}">
                    <div class="file-icon">
                        <i class="icon-${file.type === 'directory' ? 'folder' : 'file'}"></i>
                    </div>
                    <div class="file-info">
                        <h4>${file.name}</h4>
                        <p>${file.type === 'file' ? this.formatFileSize(file.size) : 'Directory'}</p>
                        <span class="file-date">${this.formatDate(file.lastModified)}</span>
                    </div>
                    <div class="file-actions">
                        ${file.type === 'file' ? `
                            <button class="btn btn-sm btn-secondary" onclick="webui.downloadFile('${file.path}')">
                                <i class="icon-download"></i>
                            </button>
                            <button class="btn btn-sm btn-primary" onclick="webui.editFile('${file.path}')">
                                <i class="icon-edit"></i>
                            </button>
                        ` : `
                            <button class="btn btn-sm btn-primary" onclick="webui.navigateToPath('${file.path}')">
                                <i class="icon-folder-open"></i>
                            </button>
                        `}
                        <button class="btn btn-sm btn-danger" onclick="webui.deleteFile('${file.path}')">
                            <i class="icon-trash"></i>
                        </button>
                    </div>
                </div>
            `).join('');
            
        } catch (error) {
            console.error('Fehler beim Laden des Verzeichnisses:', error);
        }
    }

    /**
     * Lädt Performance-Übersicht
     */
    async loadPerformanceOverview() {
        try {
            const response = await fetch('/api/performance/overview');
            const data = await response.json();
            
            // Update Metric Cards
            document.getElementById('cpuUsage').textContent = `${data.cpu.toFixed(1)}%`;
            document.getElementById('memoryUsage').textContent = `${data.memory.toFixed(1)}%`;
            document.getElementById('serverTps').textContent = data.tps.toFixed(1);
            document.getElementById('activeThreads').textContent = data.threads;
            
            // Update Charts (simplified)
            this.updatePerformanceCharts(data);
            
        } catch (error) {
            console.error('Fehler beim Laden der Performance-Daten:', error);
        }
    }

    /**
     * Führt Server-Kommando aus
     */
    async executeCommand() {
        const input = document.getElementById('commandInput');
        const command = input.value.trim();
        
        if (!command) return;
        
        try {
            const response = await fetch('/api/console/execute', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + localStorage.getItem('webui_session_token')
                },
                body: JSON.stringify({ command })
            });
            
            if (response.ok) {
                input.value = '';
                this.addConsoleMessage(`> ${command}`, 'command');
            } else {
                this.addConsoleMessage('Failed to execute command', 'error');
            }
            
        } catch (error) {
            console.error('Fehler beim Ausführen des Kommandos:', error);
            this.addConsoleMessage('Error executing command', 'error');
        }
    }

    /**
     * Lädt Module-Informationen
     */
    async loadModules() {
        try {
            const response = await fetch('/api/modules/list');
            const data = await response.json();
            
            const loadedList = document.getElementById('loadedModulesList');
            if (loadedList && data.loaded) {
                loadedList.innerHTML = data.loaded.map(module => `
                    <div class="module-card">
                        <div class="module-info">
                            <h4>${module.name}</h4>
                            <p>Version: ${module.version}</p>
                            <span class="module-status loaded">Loaded</span>
                        </div>
                        <div class="module-actions">
                            <button class="btn btn-sm btn-warning" onclick="webui.reloadModule('${module.id}')">
                                <i class="icon-refresh"></i> Reload
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="webui.unloadModule('${module.id}')">
                                <i class="icon-stop"></i> Unload
                            </button>
                        </div>
                    </div>
                `).join('');
            }
            
        } catch (error) {
            console.error('Fehler beim Laden der Module:', error);
        }
    }

    /**
     * Lädt Console-History
     */
    async loadConsoleHistory() {
        try {
            const response = await fetch('/api/console/history');
            const history = await response.json();
            
            const consoleOutput = document.getElementById('consoleOutput');
            if (consoleOutput && history.lines) {
                consoleOutput.innerHTML = history.lines.map(line => 
                    `<div class="console-line ${line.level}">${this.escapeHtml(line.message)}</div>`
                ).join('');
                
                this.scrollConsoleToBottom();
            }
            
        } catch (error) {
            console.error('Fehler beim Laden der Console-History:', error);
        }
    }

    /**
     * Wechselt Players-Tab
     */
    switchPlayersTab(tab) {
        // Tab-Buttons aktualisieren
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tab);
        });
        
        // Tab-Panes aktualisieren
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.toggle('active', pane.id === `${tab}Tab`);
        });
        
        // Daten für aktiven Tab laden
        switch (tab) {
            case 'online':
                this.loadOnlinePlayers();
                break;
            case 'offline':
                this.loadOfflinePlayers();
                break;
            case 'bans':
                this.loadBansList();
                break;
        }
    }

    /**
     * Wechselt Modules-Tab
     */
    switchModulesTab(tab) {
        // Tab-Buttons aktualisieren
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tab);
        });
        
        // Tab-Panes aktualisieren
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.toggle('active', pane.id === `${tab}Tab`);
        });
        
        // Daten für aktiven Tab laden
        switch (tab) {
            case 'loaded':
                this.loadModules();
                break;
            case 'available':
                this.loadAvailableModules();
                break;
            case 'performance':
                this.loadModulePerformance();
                break;
        }
    }

    /**
     * Wechselt Performance-Tab
     */
    switchPerformanceTab(tab) {
        // Tab-Buttons aktualisieren
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tab);
        });
        
        // Tab-Panes aktualisieren
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.toggle('active', pane.id === `${tab}Tab`);
        });
        
        // Daten für aktiven Tab laden
        switch (tab) {
            case 'overview':
                this.loadPerformanceOverview();
                break;
            case 'server':
                this.loadServerMetrics();
                break;
            case 'modules':
                this.loadModulePerformance();
                break;
            case 'alerts':
                this.loadPerformanceAlerts();
                break;
        }
    }

    /**
     * Behandelt Console-Output vom WebSocket
     */
    handleConsoleOutput(data) {
        this.addConsoleMessage(data.message, data.level || 'info');
    }

    /**
     * Behandelt Player-Events vom WebSocket
     */
    handlePlayerEvent(message) {
        // Player-Liste aktualisieren wenn Online-Tab aktiv ist
        const activeTab = document.querySelector('.tab-pane.active');
        if (activeTab && activeTab.id === 'onlineTab') {
            this.loadOnlinePlayers();
        }
        
        // Dashboard-Stats aktualisieren
        this.updateServerStats({ playerCount: message.data.playerCount });
    }

    /**
     * Behandelt Performance-Updates vom WebSocket
     */
    handlePerformanceUpdate(data) {
        // Performance-Charts aktualisieren falls sichtbar
        const performanceModule = document.querySelector('.performance-module');
        if (performanceModule) {
            this.updatePerformanceCharts(data);
        }
        
        // Dashboard-Stats aktualisieren
        this.updateServerStats(data);
    }

    /**
     * Behandelt Module-Events vom WebSocket
     */
    handleModuleEvent(data) {
        // Module-Liste aktualisieren falls sichtbar
        const modulesModule = document.querySelector('.modules-module');
        if (modulesModule) {
            this.loadModules();
        }
    }

    /**
     * Aktualisiert Server-Statistiken im Dashboard
     */
    updateServerStats(data) {
        // Update Dashboard-Werte
        const serverStatusValue = document.getElementById('serverStatusValue');
        const onlinePlayersValue = document.getElementById('onlinePlayersValue');
        const serverTpsValue = document.getElementById('serverTpsValue');
        const memoryUsageValue = document.getElementById('memoryUsageValue');

        if (data.status && serverStatusValue) {
            serverStatusValue.textContent = data.status;
        }

        if (data.playerCount !== undefined && onlinePlayersValue) {
            onlinePlayersValue.textContent = data.playerCount;
        }

        if (data.tps !== undefined && serverTpsValue) {
            serverTpsValue.textContent = data.tps.toFixed(1);
            
            // Update TPS color based on performance
            const tpsCard = serverTpsValue.closest('.stat-card');
            if (tpsCard) {
                const changeElement = tpsCard.querySelector('.stat-change');
                if (data.tps >= 19.5) {
                    changeElement.textContent = 'Excellent';
                    changeElement.className = 'stat-change positive';
                } else if (data.tps >= 18.0) {
                    changeElement.textContent = 'Good';
                    changeElement.className = 'stat-change neutral';
                } else {
                    changeElement.textContent = 'Poor';
                    changeElement.className = 'stat-change negative';
                }
            }
        }

        if (data.memory !== undefined && memoryUsageValue) {
            const memoryPercent = data.memory.used / data.memory.max * 100;
            memoryUsageValue.textContent = `${memoryPercent.toFixed(1)}%`;
            
            // Update memory details
            const memoryCard = memoryUsageValue.closest('.stat-card');
            if (memoryCard) {
                const changeElement = memoryCard.querySelector('.stat-change');
                changeElement.textContent = `${this.formatFileSize(data.memory.used)} / ${this.formatFileSize(data.memory.max)}`;
                
                if (memoryPercent > 90) {
                    changeElement.className = 'stat-change negative';
                } else if (memoryPercent > 70) {
                    changeElement.className = 'stat-change neutral';
                } else {
                    changeElement.className = 'stat-change positive';
                }
            }
        }

        if (data.cpu !== undefined) {
            // Update CPU-related displays if present
            const cpuElements = document.querySelectorAll('[data-metric="cpu"]');
            cpuElements.forEach(element => {
                element.textContent = `${data.cpu.toFixed(1)}%`;
            });
        }
    }

    /**
     * Startet Dashboard-Updates
     */
    startDashboardUpdates() {
        // Initiale Daten laden
        this.loadDashboardStats();
        
        // Periodische Updates alle 5 Sekunden
        this.dashboardInterval = setInterval(() => {
            this.loadDashboardStats();
        }, 5000);
    }

    /**
     * Lädt Dashboard-Statistiken
     */
    async loadDashboardStats() {
        try {
            const response = await fetch('/api/dashboard/stats', {
                headers: {
                    'Authorization': 'Bearer ' + localStorage.getItem('webui_session_token')
                }
            });
            
            if (response.ok) {
                const stats = await response.json();
                this.updateServerStats(stats);
            }
        } catch (error) {
            console.error('Fehler beim Laden der Dashboard-Statistiken:', error);
        }
    }

    /**
     * Erweiterte WebSocket-Nachrichtenbehandlung
     */
    handleWebSocketMessage(message) {
        switch (message.type) {
            case 'server_stats':
                this.updateServerStats(message.data);
                break;
                
            case 'console_output':
                this.handleConsoleOutput(message.data);
                break;
                
            case 'player_join':
                this.handlePlayerJoin(message.data);
                break;
                
            case 'player_leave':
                this.handlePlayerLeave(message.data);
                break;
                
            case 'performance_update':
                this.handlePerformanceUpdate(message.data);
                break;
                
            case 'module_event':
                this.handleModuleEvent(message.data);
                break;
                
            case 'security_alert':
                this.handleSecurityAlert(message.data);
                break;
                
            case 'notification':
                this.handleNotification(message.data);
                break;
                
            case 'auth_expired':
                this.handleAuthExpired();
                break;
                
            default:
                console.log('Unbekannte WebSocket-Nachricht:', message);
        }
    }

    /**
     * Behandelt Player-Join-Events
     */
    handlePlayerJoin(data) {
        // Dashboard aktualisieren
        this.updateServerStats({ playerCount: data.newPlayerCount });
        
        // Online-Player-Liste aktualisieren falls sichtbar
        const onlineTab = document.getElementById('onlineTab');
        if (onlineTab && onlineTab.classList.contains('active')) {
            this.loadOnlinePlayers();
        }
        
        // Benachrichtigung anzeigen falls aktiviert
        if (this.getNotificationSetting('player_events')) {
            this.showNotification(`${data.playerName} joined the server`, 'info');
        }
    }

    /**
     * Behandelt Player-Leave-Events
     */
    handlePlayerLeave(data) {
        // Dashboard aktualisieren
        this.updateServerStats({ playerCount: data.newPlayerCount });
        
        // Online-Player-Liste aktualisieren falls sichtbar
        const onlineTab = document.getElementById('onlineTab');
        if (onlineTab && onlineTab.classList.contains('active')) {
            this.loadOnlinePlayers();
        }
        
        // Benachrichtigung anzeigen falls aktiviert
        if (this.getNotificationSetting('player_events')) {
            this.showNotification(`${data.playerName} left the server`, 'info');
        }
    }

    /**
     * Behandelt Security-Alerts
     */
    handleSecurityAlert(data) {
        // Security-Badge im Navigation aktualisieren
        this.updateSecurityBadge(data.severity);
        
        // Alert-Benachrichtigung
        this.showNotification(`Security Alert: ${data.message}`, 'warning');
        
        // Falls Security-Modul aktiv ist, Alert-Liste aktualisieren
        const securityModule = document.querySelector('.security-module');
        if (securityModule) {
            this.loadSecurityAlerts();
        }
    }

    /**
     * Behandelt allgemeine Benachrichtigungen
     */
    handleNotification(data) {
        this.showNotification(data.message, data.type || 'info');
    }

    /**
     * Behandelt Auth-Expired-Events
     */
    handleAuthExpired() {
        localStorage.removeItem('webui_session_token');
        this.isAuthenticated = false;
        this.showLogin();
        this.showNotification('Session expired. Please log in again.', 'warning');
    }

    /**
     * Aktualisiert Security-Badge
     */
    updateSecurityBadge(severity) {
        const securityNavLink = document.querySelector('[data-module="security"]');
        if (securityNavLink) {
            let badge = securityNavLink.querySelector('.nav-badge');
            if (!badge) {
                badge = document.createElement('span');
                badge.className = 'nav-badge';
                securityNavLink.appendChild(badge);
            }
            badge.className = `nav-badge ${severity}`;
            badge.textContent = '!';
        }
    }

    /**
     * Lädt Security-Alerts
     */
    async loadSecurityAlerts() {
        try {
            const response = await fetch('/api/security/alerts');
            const alerts = await response.json();
            
            // Update threat count in security module
            const threatCountElements = document.querySelectorAll('.threat-count');
            if (threatCountElements.length > 0) {
                threatCountElements[0].textContent = alerts.active.length;
            }
            
        } catch (error) {
            console.error('Error loading security alerts:', error);
        }
    }

    /**
     * Holt Benachrichtigungseinstellung
     */
    getNotificationSetting(type) {
        // Default: alle Benachrichtigungen aktiviert
        const settings = JSON.parse(localStorage.getItem('webui_notifications') || '{}');
        return settings[type] !== false;
    }

    /**
     * Erweiterte Theme-Funktionalität
     */
    toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        
        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('webui_theme', newTheme);
        
        // Theme-Button-Icon aktualisieren
        const themeToggle = document.getElementById('themeToggle');
        if (themeToggle) {
            themeToggle.innerHTML = newTheme === 'dark' 
                ? '<i class="icon-sun"></i>' 
                : '<i class="icon-moon"></i>';
        }
    }

    /**
     * Theme beim Start einrichten
     */
    setupTheme() {
        const savedTheme = localStorage.getItem('webui_theme') || 'dark';
        document.documentElement.setAttribute('data-theme', savedTheme);
        this.theme = savedTheme;
    }

    /**
     * Router für URL-Handling einrichten
     */
    setupRouter() {
        // Hash-Change-Event für Navigation
        window.addEventListener('hashchange', () => {
            const hash = window.location.hash.substring(1);
            if (hash && this.isAuthenticated) {
                this.loadModule(hash);
                
                // Navigation-Links aktualisieren
                document.querySelectorAll('.nav-link').forEach(link => {
                    link.classList.toggle('active', link.dataset.module === hash);
                });
            }
        });
        
        // Initial route laden
        const initialHash = window.location.hash.substring(1);
        if (initialHash && this.isAuthenticated) {
            this.loadModule(initialHash);
        }
    }

    /**
     * Erweiterte Fehlerbehandlung
     */
    showError(title, message, details = null) {
        const root = document.getElementById('root');
        root.innerHTML = `
            <div class="error-container">
                <div class="error-card">
                    <div class="error-icon">
                        <i class="icon-alert-triangle"></i>
                    </div>
                    <h1>${title}</h1>
                    <p>${message}</p>
                    ${details ? `<details><summary>Technical Details</summary><pre>${details}</pre></details>` : ''}
                    <div class="error-actions">
                        <button onclick="location.reload()" class="btn btn-primary">
                            <i class="icon-refresh"></i> Reload Page
                        </button>
                        <button onclick="webui.showLogin()" class="btn btn-secondary">
                            <i class="icon-user"></i> Back to Login
                        </button>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Performance-optimierte Benachrichtigungen
     */
    showNotification(message, type = 'info', duration = 5000) {
        // Create notification container if it doesn't exist
        let container = document.getElementById('notification-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'notification-container';
            container.className = 'notification-container';
            document.body.appendChild(container);
        }
        
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <i class="notification-icon icon-${this.getNotificationIcon(type)}"></i>
                <span class="notification-message">${message}</span>
                <button class="notification-close" onclick="this.parentElement.parentElement.remove()">
                    <i class="icon-x"></i>
                </button>
            </div>
        `;
        
        // Add to container
        container.appendChild(notification);
        
        // Auto-remove after duration
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, duration);
    }

    /**
     * Holt Benachrichtigungs-Icon basierend auf Typ
     */
    getNotificationIcon(type) {
        const icons = {
            success: 'check-circle',
            error: 'alert-circle',
            warning: 'alert-triangle',
            info: 'info'
        };
        return icons[type] || 'info';
    }

    /**
     * Cleanup beim Logout
     */
    cleanup() {
        // WebSocket schließen
        if (this.websocket) {
            this.websocket.close();
            this.websocket = null;
        }
        
        // Intervals clearen
        if (this.dashboardInterval) {
            clearInterval(this.dashboardInterval);
            this.dashboardInterval = null;
        }
        
        // Event-Listener entfernen
        window.removeEventListener('hashchange', this.hashChangeHandler);
        
        // Module-spezifische Cleanup
        this.modules = {};
    }
}