/**
 * EssentialsCore Advanced WebUI - Security & Settings Modules
 * Erweiterte Module für Sicherheit und Konfiguration
 */

// Security Module Implementation
window.loadSecurityModule = async function() {
    const moduleContent = document.getElementById('moduleContent');
    
    moduleContent.innerHTML = `
        <div class="security-module">
            <div class="security-header">
                <div class="security-tabs">
                    <button class="tab-btn active" data-tab="overview">🛡️ Overview</button>
                    <button class="tab-btn" data-tab="alerts">🚨 Security Alerts</button>
                    <button class="tab-btn" data-tab="firewall">🔥 Firewall</button>
                    <button class="tab-btn" data-tab="audit">📋 Audit Log</button>
                    <button class="tab-btn" data-tab="encryption">🔐 Encryption</button>
                </div>
                
                <div class="security-controls">
                    <button class="btn btn-primary" id="refreshSecurityBtn">
                        <i class="icon-refresh"></i> Refresh
                    </button>
                    <button class="btn btn-warning" id="emergencyLockdownBtn">
                        <i class="icon-lock"></i> Emergency Lockdown
                    </button>
                </div>
            </div>
            
            <div class="tab-content">
                <div id="overviewTab" class="tab-pane active">
                    <div class="security-overview">
                        <div class="security-metrics">
                            <div class="metric-card threat-level">
                                <div class="metric-icon">🎯</div>
                                <div class="metric-content">
                                    <h3>Threat Level</h3>
                                    <div class="threat-indicator low" id="threatLevel">LOW</div>
                                    <span class="threat-count" id="threatCount">0</span> aktive Bedrohungen
                                </div>
                            </div>
                            
                            <div class="metric-card login-attempts">
                                <div class="metric-icon">🔑</div>
                                <div class="metric-content">
                                    <h3>Login Attempts</h3>
                                    <div class="metric-value" id="loginAttempts">0</div>
                                    <span class="metric-change">Letzte 24h</span>
                                </div>
                            </div>
                            
                            <div class="metric-card failed-logins">
                                <div class="metric-icon">❌</div>
                                <div class="metric-content">
                                    <h3>Failed Logins</h3>
                                    <div class="metric-value" id="failedLogins">0</div>
                                    <span class="metric-change">Letzte 24h</span>
                                </div>
                            </div>
                            
                            <div class="metric-card banned-ips">
                                <div class="metric-icon">🚫</div>
                                <div class="metric-content">
                                    <h3>Banned IPs</h3>
                                    <div class="metric-value" id="bannedIps">0</div>
                                    <span class="metric-change">Aktiv gesperrt</span>
                                </div>
                            </div>
                        </div>
                        
                        <div class="security-status">
                            <div class="status-card">
                                <h3>🔐 Encryption Status</h3>
                                <div class="status-list">
                                    <div class="status-item enabled">
                                        <i class="icon-check"></i> SSL/TLS Encryption
                                    </div>
                                    <div class="status-item enabled">
                                        <i class="icon-check"></i> Database Encryption
                                    </div>
                                    <div class="status-item enabled">
                                        <i class="icon-check"></i> WebSocket Security
                                    </div>
                                </div>
                            </div>
                            
                            <div class="status-card">
                                <h3>🛡️ Protection Status</h3>
                                <div class="status-list">
                                    <div class="status-item enabled">
                                        <i class="icon-check"></i> DDoS Protection
                                    </div>
                                    <div class="status-item enabled">
                                        <i class="icon-check"></i> Rate Limiting
                                    </div>
                                    <div class="status-item warning">
                                        <i class="icon-alert"></i> 2FA Recommended
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div id="alertsTab" class="tab-pane">
                    <div class="security-alerts" id="securityAlerts">
                        <!-- Security alerts werden hier geladen -->
                    </div>
                </div>
                
                <div id="firewallTab" class="tab-pane">
                    <div class="firewall-config">
                        <div class="firewall-rules" id="firewallRules">
                            <!-- Firewall-Regeln werden hier geladen -->
                        </div>
                    </div>
                </div>
                
                <div id="auditTab" class="tab-pane">
                    <div class="audit-log" id="auditLog">
                        <!-- Audit-Log wird hier geladen -->
                    </div>
                </div>
                
                <div id="encryptionTab" class="tab-pane">
                    <div class="encryption-settings" id="encryptionSettings">
                        <!-- Verschlüsselungseinstellungen werden hier geladen -->
                    </div>
                </div>
            </div>
        </div>
    `;

    setupSecurityModule();
};

// Settings Module Implementation
window.loadSettingsModule = async function() {
    const moduleContent = document.getElementById('moduleContent');
    
    moduleContent.innerHTML = `
        <div class="settings-module">
            <div class="settings-sidebar">
                <div class="settings-nav">
                    <div class="nav-group">
                        <h3>🎛️ Allgemein</h3>
                        <a href="#general" class="nav-item active" data-settings="general">
                            <i class="icon-settings"></i> Grundeinstellungen
                        </a>
                        <a href="#appearance" class="nav-item" data-settings="appearance">
                            <i class="icon-palette"></i> Erscheinungsbild
                        </a>
                        <a href="#notifications" class="nav-item" data-settings="notifications">
                            <i class="icon-bell"></i> Benachrichtigungen
                        </a>
                    </div>
                    
                    <div class="nav-group">
                        <h3>🔧 Server</h3>
                        <a href="#server" class="nav-item" data-settings="server">
                            <i class="icon-server"></i> Server-Konfiguration
                        </a>
                        <a href="#performance" class="nav-item" data-settings="performance">
                            <i class="icon-zap"></i> Performance
                        </a>
                        <a href="#backup" class="nav-item" data-settings="backup">
                            <i class="icon-database"></i> Backup & Restore
                        </a>
                    </div>
                    
                    <div class="nav-group">
                        <h3>🔐 Sicherheit</h3>
                        <a href="#security" class="nav-item" data-settings="security">
                            <i class="icon-shield"></i> Sicherheit
                        </a>
                        <a href="#users" class="nav-item" data-settings="users">
                            <i class="icon-users"></i> Benutzerverwaltung
                        </a>
                        <a href="#api" class="nav-item" data-settings="api">
                            <i class="icon-code"></i> API-Einstellungen
                        </a>
                    </div>
                </div>
            </div>
            
            <div class="settings-content">
                <div class="settings-header">
                    <h2 id="settingsTitle">Grundeinstellungen</h2>
                    <div class="settings-actions">
                        <button class="btn btn-secondary" id="resetSettingsBtn">
                            <i class="icon-refresh"></i> Zurücksetzen
                        </button>
                        <button class="btn btn-primary" id="saveSettingsBtn">
                            <i class="icon-save"></i> Speichern
                        </button>
                    </div>
                </div>
                
                <div class="settings-panel" id="settingsPanel">
                    <!-- Settings content wird hier geladen -->
                </div>
            </div>
        </div>
    `;

    setupSettingsModule();
    loadSettingsPanel('general');
};

// Security Module Setup
function setupSecurityModule() {
    // Tab-Navigation
    document.querySelectorAll('.security-tabs .tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.dataset.tab;
            switchSecurityTab(tab);
        });
    });

    // Controls
    document.getElementById('refreshSecurityBtn').addEventListener('click', () => {
        loadSecurityData();
    });

    document.getElementById('emergencyLockdownBtn').addEventListener('click', () => {
        showEmergencyLockdownDialog();
    });

    // Initiale Daten laden
    loadSecurityData();
}

// Settings Module Setup
function setupSettingsModule() {
    // Navigation
    document.querySelectorAll('.settings-nav .nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const settings = item.dataset.settings;
            
            // Aktive Klasse aktualisieren
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            item.classList.add('active');
            
            // Panel laden
            loadSettingsPanel(settings);
        });
    });

    // Actions
    document.getElementById('saveSettingsBtn').addEventListener('click', () => {
        saveCurrentSettings();
    });

    document.getElementById('resetSettingsBtn').addEventListener('click', () => {
        resetCurrentSettings();
    });
}

// Security Functions
function switchSecurityTab(tab) {
    // Tab-Buttons aktualisieren
    document.querySelectorAll('.security-tabs .tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tab);
    });
    
    // Tab-Panes aktualisieren
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.toggle('active', pane.id === `${tab}Tab`);
    });
    
    // Daten für aktiven Tab laden
    switch (tab) {
        case 'overview':
            loadSecurityOverview();
            break;
        case 'alerts':
            loadSecurityAlerts();
            break;
        case 'firewall':
            loadFirewallConfig();
            break;
        case 'audit':
            loadAuditLog();
            break;
        case 'encryption':
            loadEncryptionSettings();
            break;
    }
}

async function loadSecurityData() {
    try {
        const response = await fetch('/api/security/overview');
        const data = await response.json();
        
        // Update Security Metrics
        document.getElementById('threatLevel').textContent = data.threatLevel || 'LOW';
        document.getElementById('threatCount').textContent = data.activeThreats || 0;
        document.getElementById('loginAttempts').textContent = data.loginAttempts || 0;
        document.getElementById('failedLogins').textContent = data.failedLogins || 0;
        document.getElementById('bannedIps').textContent = data.bannedIps || 0;
        
        // Update Threat Level Class
        const threatElement = document.getElementById('threatLevel');
        threatElement.className = `threat-indicator ${(data.threatLevel || 'low').toLowerCase()}`;
        
    } catch (error) {
        console.error('Fehler beim Laden der Security-Daten:', error);
    }
}

async function loadSecurityAlerts() {
    try {
        const response = await fetch('/api/security/alerts');
        const alerts = await response.json();
        
        const alertsContainer = document.getElementById('securityAlerts');
        alertsContainer.innerHTML = alerts.map(alert => `
            <div class="alert-item ${alert.severity}">
                <div class="alert-icon">
                    ${getAlertIcon(alert.type)}
                </div>
                <div class="alert-content">
                    <h4>${alert.title}</h4>
                    <p>${alert.description}</p>
                    <span class="alert-time">${formatTime(alert.timestamp)}</span>
                </div>
                <div class="alert-actions">
                    <button class="btn btn-sm btn-primary" onclick="acknowledgeAlert('${alert.id}')">
                        Acknowledge
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="dismissAlert('${alert.id}')">
                        Dismiss
                    </button>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Fehler beim Laden der Security-Alerts:', error);
    }
}

// Settings Functions
function loadSettingsPanel(type) {
    const panel = document.getElementById('settingsPanel');
    const title = document.getElementById('settingsTitle');
    
    switch (type) {
        case 'general':
            title.textContent = 'Grundeinstellungen';
            panel.innerHTML = getGeneralSettingsHTML();
            break;
        case 'appearance':
            title.textContent = 'Erscheinungsbild';
            panel.innerHTML = getAppearanceSettingsHTML();
            break;
        case 'notifications':
            title.textContent = 'Benachrichtigungen';
            panel.innerHTML = getNotificationSettingsHTML();
            break;
        case 'server':
            title.textContent = 'Server-Konfiguration';
            panel.innerHTML = getServerSettingsHTML();
            break;
        case 'performance':
            title.textContent = 'Performance-Einstellungen';
            panel.innerHTML = getPerformanceSettingsHTML();
            break;
        case 'backup':
            title.textContent = 'Backup & Restore';
            panel.innerHTML = getBackupSettingsHTML();
            break;
        case 'security':
            title.textContent = 'Sicherheitseinstellungen';
            panel.innerHTML = getSecuritySettingsHTML();
            break;
        case 'users':
            title.textContent = 'Benutzerverwaltung';
            panel.innerHTML = getUserSettingsHTML();
            break;
        case 'api':
            title.textContent = 'API-Einstellungen';
            panel.innerHTML = getAPISettingsHTML();
            break;
    }
    
    // Setup event listeners für das aktuelle Panel
    setupPanelEventListeners(type);
}

// Settings HTML Generators
function getGeneralSettingsHTML() {
    return `
        <div class="settings-section">
            <h3>🌐 Server-Informationen</h3>
            <div class="form-group">
                <label for="serverName">Server-Name</label>
                <input type="text" id="serverName" value="EssentialsCore Server" class="form-input">
            </div>
            <div class="form-group">
                <label for="serverDescription">Beschreibung</label>
                <textarea id="serverDescription" class="form-input" rows="3">Ein moderner Minecraft-Server mit EssentialsCore</textarea>
            </div>
            <div class="form-group">
                <label for="maxPlayers">Maximale Spieler</label>
                <input type="number" id="maxPlayers" value="100" class="form-input" min="1" max="1000">
            </div>
        </div>
        
        <div class="settings-section">
            <h3>🕐 Zeit & Datum</h3>
            <div class="form-group">
                <label for="timezone">Zeitzone</label>
                <select id="timezone" class="form-input">
                    <option value="Europe/Berlin">Europe/Berlin (GMT+1)</option>
                    <option value="UTC">UTC (GMT+0)</option>
                    <option value="America/New_York">America/New_York (GMT-5)</option>
                </select>
            </div>
            <div class="form-group">
                <label for="dateFormat">Datumsformat</label>
                <select id="dateFormat" class="form-input">
                    <option value="DD.MM.YYYY">DD.MM.YYYY</option>
                    <option value="MM/DD/YYYY">MM/DD/YYYY</option>
                    <option value="YYYY-MM-DD">YYYY-MM-DD</option>
                </select>
            </div>
        </div>
    `;
}

function getAppearanceSettingsHTML() {
    return `
        <div class="settings-section">
            <h3>🎨 Theme & Design</h3>
            <div class="form-group">
                <label>Theme</label>
                <div class="theme-selector">
                    <div class="theme-option" data-theme="dark">
                        <div class="theme-preview dark"></div>
                        <span>Dark Theme</span>
                    </div>
                    <div class="theme-option" data-theme="light">
                        <div class="theme-preview light"></div>
                        <span>Light Theme</span>
                    </div>
                    <div class="theme-option" data-theme="auto">
                        <div class="theme-preview auto"></div>
                        <span>Auto (System)</span>
                    </div>
                </div>
            </div>
            
            <div class="form-group">
                <label for="accentColor">Akzentfarbe</label>
                <div class="color-picker">
                    <input type="color" id="accentColor" value="#3498db" class="color-input">
                    <div class="color-presets">
                        <button class="color-preset" data-color="#3498db" style="background: #3498db"></button>
                        <button class="color-preset" data-color="#2ecc71" style="background: #2ecc71"></button>
                        <button class="color-preset" data-color="#e74c3c" style="background: #e74c3c"></button>
                        <button class="color-preset" data-color="#f39c12" style="background: #f39c12"></button>
                        <button class="color-preset" data-color="#9b59b6" style="background: #9b59b6"></button>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="settings-section">
            <h3>📐 Layout & Verhalten</h3>
            <div class="form-group">
                <label class="checkbox-container">
                    <input type="checkbox" id="compactMode">
                    <span class="checkmark"></span>
                    Kompakter Modus
                </label>
            </div>
            <div class="form-group">
                <label class="checkbox-container">
                    <input type="checkbox" id="animationsEnabled" checked>
                    <span class="checkmark"></span>
                    Animationen aktivieren
                </label>
            </div>
            <div class="form-group">
                <label class="checkbox-container">
                    <input type="checkbox" id="soundEffects">
                    <span class="checkmark"></span>
                    Sound-Effekte
                </label>
            </div>
        </div>
    `;
}

function getNotificationSettingsHTML() {
    return `
        <div class="settings-section">
            <h3>🔔 Benachrichtigungstypen</h3>
            
            <div class="notification-setting">
                <div class="setting-info">
                    <h4>Player Events</h4>
                    <p>Benachrichtigungen bei Spieler-Join/Leave</p>
                </div>
                <label class="toggle-switch">
                    <input type="checkbox" id="notifyPlayerEvents" checked>
                    <span class="slider"></span>
                </label>
            </div>
            
            <div class="notification-setting">
                <div class="setting-info">
                    <h4>Security Alerts</h4>
                    <p>Wichtige Sicherheitswarnungen</p>
                </div>
                <label class="toggle-switch">
                    <input type="checkbox" id="notifySecurityAlerts" checked>
                    <span class="slider"></span>
                </label>
            </div>
            
            <div class="notification-setting">
                <div class="setting-info">
                    <h4>Performance Warnings</h4>
                    <p>Warnungen bei Performance-Problemen</p>
                </div>
                <label class="toggle-switch">
                    <input type="checkbox" id="notifyPerformance" checked>
                    <span class="slider"></span>
                </label>
            </div>
            
            <div class="notification-setting">
                <div class="setting-info">
                    <h4>Module Events</h4>
                    <p>Informationen über Module-Änderungen</p>
                </div>
                <label class="toggle-switch">
                    <input type="checkbox" id="notifyModuleEvents">
                    <span class="slider"></span>
                </label>
            </div>
        </div>
        
        <div class="settings-section">
            <h3>⏱️ Anzeigedauer</h3>
            <div class="form-group">
                <label for="notificationDuration">Standard-Anzeigedauer (Sekunden)</label>
                <input type="range" id="notificationDuration" min="1" max="30" value="5" class="range-input">
                <span class="range-value">5s</span>
            </div>
        </div>
    `;
}

// Utility Functions
function getAlertIcon(type) {
    const icons = {
        'intrusion': '🚨',
        'ddos': '⚡',
        'malware': '🦠',
        'suspicious': '🕵️',
        'unauthorized': '🚫'
    };
    return icons[type] || '⚠️';
}

function formatTime(timestamp) {
    return new Date(timestamp).toLocaleString('de-DE');
}

function setupPanelEventListeners(type) {
    switch (type) {
        case 'appearance':
            setupThemeSelector();
            setupColorPicker();
            break;
        case 'notifications':
            setupNotificationSlider();
            break;
    }
}

function setupThemeSelector() {
    document.querySelectorAll('.theme-option').forEach(option => {
        option.addEventListener('click', () => {
            document.querySelectorAll('.theme-option').forEach(o => o.classList.remove('selected'));
            option.classList.add('selected');
        });
    });
}

function setupColorPicker() {
    const colorInput = document.getElementById('accentColor');
    document.querySelectorAll('.color-preset').forEach(preset => {
        preset.addEventListener('click', () => {
            const color = preset.dataset.color;
            colorInput.value = color;
            // Apply color immediately
            document.documentElement.style.setProperty('--primary-color', color);
        });
    });
}

function setupNotificationSlider() {
    const slider = document.getElementById('notificationDuration');
    const valueDisplay = slider.nextElementSibling;
    
    slider.addEventListener('input', (e) => {
        valueDisplay.textContent = `${e.target.value}s`;
    });
}

async function saveCurrentSettings() {
    // Settings sammeln und speichern
    try {
        const settings = {};
        // Hier würden die aktuellen Settings gesammelt
        
        const response = await fetch('/api/settings/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + localStorage.getItem('webui_session_token')
            },
            body: JSON.stringify(settings)
        });
        
        if (response.ok) {
            webui.showNotification('✅ Einstellungen erfolgreich gespeichert', 'success');
        } else {
            throw new Error('Failed to save settings');
        }
    } catch (error) {
        console.error('Fehler beim Speichern der Einstellungen:', error);
        webui.showNotification('❌ Fehler beim Speichern der Einstellungen', 'error');
    }
}

console.log('🔐 Security & Settings modules loaded successfully!');
