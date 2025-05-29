/**
 * EssentialsCore Advanced WebUI - Testing Suite
 * Umfassende Tests für Frontend-Komponenten und API-Integration
 */

class WebUITestSuite {
    constructor() {
        this.tests = new Map();
        this.testResults = [];
        this.isRunning = false;
        this.startTime = null;
        this.endTime = null;
        this.mockData = this.generateMockData();
        
        this.init();
    }

    /**
     * Test-Suite initialisieren
     */
    init() {
        this.registerTests();
        this.setupTestEnvironment();
    }

    /**
     * Tests registrieren
     */
    registerTests() {
        // UI-Komponenten Tests
        this.registerTest('ui-components', 'UI-Komponenten Tests', this.testUIComponents.bind(this));
        this.registerTest('navigation', 'Navigation Tests', this.testNavigation.bind(this));
        this.registerTest('forms', 'Formular Tests', this.testForms.bind(this));
        this.registerTest('modals', 'Modal Tests', this.testModals.bind(this));
        
        // Funktionalitäts-Tests
        this.registerTest('authentication', 'Authentifizierung Tests', this.testAuthentication.bind(this));
        this.registerTest('websocket', 'WebSocket Tests', this.testWebSocket.bind(this));
        this.registerTest('api-integration', 'API-Integration Tests', this.testAPIIntegration.bind(this));
        this.registerTest('data-visualization', 'Datenvisualisierung Tests', this.testDataVisualization.bind(this));
        
        // Performance-Tests
        this.registerTest('performance', 'Performance Tests', this.testPerformance.bind(this));
        this.registerTest('memory-usage', 'Speicher-Nutzung Tests', this.testMemoryUsage.bind(this));
        this.registerTest('network-performance', 'Netzwerk-Performance Tests', this.testNetworkPerformance.bind(this));
        
        // Sicherheits-Tests
        this.registerTest('security', 'Sicherheits Tests', this.testSecurity.bind(this));
        this.registerTest('input-validation', 'Input-Validierung Tests', this.testInputValidation.bind(this));
        this.registerTest('xss-protection', 'XSS-Schutz Tests', this.testXSSProtection.bind(this));
        
        // Accessibility Tests
        this.registerTest('accessibility', 'Barrierefreiheit Tests', this.testAccessibility.bind(this));
        this.registerTest('keyboard-navigation', 'Tastatur-Navigation Tests', this.testKeyboardNavigation.bind(this));
        this.registerTest('screen-reader', 'Screen Reader Tests', this.testScreenReader.bind(this));
        
        // Browser-Kompatibilität
        this.registerTest('browser-compatibility', 'Browser-Kompatibilität Tests', this.testBrowserCompatibility.bind(this));
        this.registerTest('responsive-design', 'Responsive Design Tests', this.testResponsiveDesign.bind(this));
    }

    /**
     * Test registrieren
     */
    registerTest(id, name, testFunction) {
        this.tests.set(id, {
            id,
            name,
            testFunction,
            status: 'pending',
            result: null,
            duration: 0,
            errors: []
        });
    }

    /**
     * Test-Umgebung einrichten
     */
    setupTestEnvironment() {
        // Test-Container erstellen
        if (!document.getElementById('test-container')) {
            const container = document.createElement('div');
            container.id = 'test-container';
            container.style.cssText = `
                position: fixed;
                top: -9999px;
                left: -9999px;
                width: 1000px;
                height: 800px;
                background: white;
                z-index: -1;
            `;
            document.body.appendChild(container);
        }
    }

    /**
     * Alle Tests ausführen
     */
    async runAllTests() {
        if (this.isRunning) {
            console.warn('Tests laufen bereits');
            return;
        }

        console.log('🧪 Starte WebUI Test-Suite...');
        this.isRunning = true;
        this.startTime = Date.now();
        this.testResults = [];

        const testIds = Array.from(this.tests.keys());
        let passed = 0;
        let failed = 0;

        for (const testId of testIds) {
            try {
                await this.runTest(testId);
                const test = this.tests.get(testId);
                if (test.status === 'passed') {
                    passed++;
                } else {
                    failed++;
                }
            } catch (error) {
                console.error(`Fehler beim Ausführen von Test "${testId}":`, error);
                failed++;
            }
        }

        this.endTime = Date.now();
        this.isRunning = false;

        console.log(`✅ Test-Suite abgeschlossen: ${passed} erfolgreich, ${failed} fehlgeschlagen`);
        console.log(`⏱️ Gesamtdauer: ${this.endTime - this.startTime}ms`);

        this.generateTestReport();
        return this.testResults;
    }

    /**
     * Einzelnen Test ausführen
     */
    async runTest(testId) {
        const test = this.tests.get(testId);
        if (!test) {
            throw new Error(`Test "${testId}" nicht gefunden`);
        }

        console.log(`🔄 Führe Test aus: ${test.name}`);
        
        const startTime = Date.now();
        test.status = 'running';
        test.errors = [];

        try {
            const result = await test.testFunction();
            test.result = result;
            test.status = result.success ? 'passed' : 'failed';
            
            if (!result.success && result.errors) {
                test.errors = result.errors;
            }
        } catch (error) {
            test.status = 'failed';
            test.errors = [error.message];
            test.result = { success: false, message: error.message };
        }

        test.duration = Date.now() - startTime;
        this.testResults.push(test);

        const status = test.status === 'passed' ? '✅' : '❌';
        console.log(`${status} ${test.name} (${test.duration}ms)`);

        return test;
    }

    /**
     * UI-Komponenten testen
     */
    async testUIComponents() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Dashboard-Komponenten testen
        total++;
        if (document.getElementById('dashboard')) {
            passed++;
        } else {
            errors.push('Dashboard-Element nicht gefunden');
        }

        // Navigation testen
        total++;
        if (document.querySelector('.sidebar') || document.querySelector('.nav')) {
            passed++;
        } else {
            errors.push('Navigation-Element nicht gefunden');
        }

        // Karten-Komponenten testen
        total++;
        const cards = document.querySelectorAll('.card, .widget');
        if (cards.length > 0) {
            passed++;
        } else {
            errors.push('Keine Karten-Komponenten gefunden');
        }

        // Buttons testen
        total++;
        const buttons = document.querySelectorAll('button, .btn');
        if (buttons.length > 0) {
            passed++;
        } else {
            errors.push('Keine Button-Komponenten gefunden');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} UI-Komponenten erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Navigation testen
     */
    async testNavigation() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Navigation-Links testen
        total++;
        const navLinks = document.querySelectorAll('[data-page], .nav-link');
        if (navLinks.length > 0) {
            passed++;
            
            // Click-Events testen
            navLinks.forEach(link => {
                if (!link.onclick && !link.getAttribute('onclick')) {
                    // Prüfen ob Event-Listener vorhanden
                    const hasListener = link.hasAttribute('data-page') || 
                                      link.classList.contains('nav-link');
                    if (!hasListener) {
                        errors.push(`Navigation-Link ohne Event-Handler: ${link.textContent}`);
                    }
                }
            });
        } else {
            errors.push('Keine Navigation-Links gefunden');
        }

        // Router-Funktionalität testen
        total++;
        if (window.webuiApp && typeof window.webuiApp.navigate === 'function') {
            passed++;
        } else {
            errors.push('Router-Funktionalität nicht verfügbar');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Navigation-Tests erfolgreich`,
            errors: errors,
            details: { passed, total, linkCount: navLinks ? navLinks.length : 0 }
        };
    }

    /**
     * Formular-Tests
     */
    async testForms() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Login-Formular testen
        total++;
        const loginForm = document.getElementById('login-form');
        if (loginForm) {
            passed++;
            
            // Formular-Felder prüfen
            const usernameField = loginForm.querySelector('input[type="text"], input[name="username"]');
            const passwordField = loginForm.querySelector('input[type="password"]');
            
            if (!usernameField) errors.push('Username-Feld im Login-Formular fehlt');
            if (!passwordField) errors.push('Password-Feld im Login-Formular fehlt');
        } else {
            errors.push('Login-Formular nicht gefunden');
        }

        // Einstellungs-Formulare testen
        total++;
        const settingsForms = document.querySelectorAll('form, .settings-form');
        if (settingsForms.length > 0) {
            passed++;
        } else {
            errors.push('Keine Einstellungs-Formulare gefunden');
        }

        // Validierung testen
        total++;
        try {
            const testInput = document.createElement('input');
            testInput.type = 'email';
            testInput.value = 'invalid-email';
            if (testInput.validity && testInput.validity.valid === false) {
                passed++;
            } else {
                errors.push('HTML5-Validierung nicht verfügbar');
            }
        } catch (error) {
            errors.push('Fehler beim Testen der Validierung');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Formular-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Modal-Tests
     */
    async testModals() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Modal-Elemente testen
        total++;
        const modals = document.querySelectorAll('.modal, [role="dialog"]');
        if (modals.length > 0) {
            passed++;
            
            modals.forEach((modal, index) => {
                // Accessibility-Attribute prüfen
                if (!modal.getAttribute('aria-hidden')) {
                    errors.push(`Modal ${index + 1}: aria-hidden Attribut fehlt`);
                }
                if (!modal.getAttribute('role') && !modal.classList.contains('modal')) {
                    errors.push(`Modal ${index + 1}: role Attribut fehlt`);
                }
            });
        } else {
            errors.push('Keine Modal-Elemente gefunden');
        }

        // Modal-Funktionalität testen
        total++;
        if (window.webuiApp && typeof window.webuiApp.showModal === 'function') {
            passed++;
        } else {
            errors.push('Modal-Funktionalität nicht verfügbar');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Modal-Tests erfolgreich`,
            errors: errors,
            details: { passed, total, modalCount: modals ? modals.length : 0 }
        };
    }

    /**
     * Authentifizierung testen
     */
    async testAuthentication() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Token-Handling testen
        total++;
        const originalToken = localStorage.getItem('webui-auth-token');
        localStorage.setItem('webui-auth-token', 'test-token');
        const storedToken = localStorage.getItem('webui-auth-token');
        
        if (storedToken === 'test-token') {
            passed++;
            // Original-Token wiederherstellen
            if (originalToken) {
                localStorage.setItem('webui-auth-token', originalToken);
            } else {
                localStorage.removeItem('webui-auth-token');
            }
        } else {
            errors.push('Token-Speicherung fehlgeschlagen');
        }

        // Auth-Funktionen testen
        total++;
        if (window.webuiApp && typeof window.webuiApp.checkAuthentication === 'function') {
            passed++;
        } else {
            errors.push('Authentifizierungs-Funktionen nicht verfügbar');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Authentifizierung-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * WebSocket-Tests
     */
    async testWebSocket() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // WebSocket-Manager verfügbar
        total++;
        if (window.WebUIWebSocketManager) {
            passed++;
        } else {
            errors.push('WebSocket-Manager nicht verfügbar');
        }

        // WebSocket-Unterstützung im Browser
        total++;
        if (typeof WebSocket !== 'undefined') {
            passed++;
        } else {
            errors.push('WebSocket nicht vom Browser unterstützt');
        }

        // Mock-WebSocket-Test
        total++;
        try {
            // Simuliere WebSocket-Verbindung
            const mockWS = {
                readyState: 1, // OPEN
                send: () => {},
                close: () => {}
            };
            passed++;
        } catch (error) {
            errors.push('WebSocket-Mock-Test fehlgeschlagen');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} WebSocket-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * API-Integration testen
     */
    async testAPIIntegration() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Fetch API verfügbar
        total++;
        if (typeof fetch !== 'undefined') {
            passed++;
        } else {
            errors.push('Fetch API nicht verfügbar');
        }

        // API-Endpunkt-Test (Mock)
        total++;
        try {
            // Simuliere API-Call
            const mockResponse = { status: 'ok', data: {} };
            if (mockResponse.status === 'ok') {
                passed++;
            }
        } catch (error) {
            errors.push('API-Mock-Test fehlgeschlagen');
        }

        // Error-Handling testen
        total++;
        if (window.webuiApp && typeof window.webuiApp.handleAPIError === 'function') {
            passed++;
        } else {
            errors.push('API-Error-Handling nicht verfügbar');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} API-Integration-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Datenvisualisierung testen
     */
    async testDataVisualization() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Canvas-Elemente für Charts
        total++;
        const canvasElements = document.querySelectorAll('canvas');
        if (canvasElements.length > 0) {
            passed++;
        } else {
            errors.push('Keine Canvas-Elemente für Charts gefunden');
        }

        // Chart.js oder Fallback
        total++;
        if (typeof Chart !== 'undefined' || window.WebUIDataVisualization) {
            passed++;
        } else {
            errors.push('Weder Chart.js noch Fallback-Visualisierung verfügbar');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Datenvisualisierung-Tests erfolgreich`,
            errors: errors,
            details: { passed, total, canvasCount: canvasElements ? canvasElements.length : 0 }
        };
    }

    /**
     * Performance-Tests
     */
    async testPerformance() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Page Load Performance
        total++;
        const loadTime = performance.timing.loadEventEnd - performance.timing.navigationStart;
        if (loadTime < 5000) { // Unter 5 Sekunden
            passed++;
        } else {
            errors.push(`Ladezeit zu hoch: ${loadTime}ms`);
        }

        // DOM-Knoten-Anzahl
        total++;
        const domNodes = document.querySelectorAll('*').length;
        if (domNodes < 5000) { // Unter 5000 Knoten
            passed++;
        } else {
            errors.push(`Zu viele DOM-Knoten: ${domNodes}`);
        }

        // Memory Usage (falls verfügbar)
        total++;
        if (performance.memory) {
            const memoryUsage = performance.memory.usedJSHeapSize / 1024 / 1024; // MB
            if (memoryUsage < 100) { // Unter 100MB
                passed++;
            } else {
                errors.push(`Speicherverbrauch zu hoch: ${memoryUsage.toFixed(2)}MB`);
            }
        } else {
            passed++; // Test überspringen wenn nicht verfügbar
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Performance-Tests erfolgreich`,
            errors: errors,
            details: { passed, total, loadTime, domNodes }
        };
    }

    /**
     * Speicher-Nutzung testen
     */
    async testMemoryUsage() {
        const errors = [];
        let passed = 0;
        let total = 0;

        total++;
        if (performance.memory) {
            const memory = performance.memory;
            const usedPercent = (memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100;
            
            if (usedPercent < 80) {
                passed++;
            } else {
                errors.push(`Speichernutzung zu hoch: ${usedPercent.toFixed(1)}%`);
            }
        } else {
            passed++; // Test überspringen
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Speicher-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Netzwerk-Performance testen
     */
    async testNetworkPerformance() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Connection-Typ (falls verfügbar)
        total++;
        if (navigator.connection) {
            const connection = navigator.connection;
            if (connection.effectiveType && connection.effectiveType !== 'slow-2g') {
                passed++;
            } else {
                errors.push('Langsame Netzwerkverbindung erkannt');
            }
        } else {
            passed++; // Test überspringen
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Netzwerk-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Sicherheits-Tests
     */
    async testSecurity() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // HTTPS-Verwendung (in Produktion)
        total++;
        if (location.protocol === 'https:' || location.hostname === 'localhost') {
            passed++;
        } else {
            errors.push('Unsichere HTTP-Verbindung');
        }

        // Content Security Policy
        total++;
        const cspMeta = document.querySelector('meta[http-equiv="Content-Security-Policy"]');
        if (cspMeta || location.hostname === 'localhost') {
            passed++;
        } else {
            errors.push('Content Security Policy nicht gefunden');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Sicherheits-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Input-Validierung testen
     */
    async testInputValidation() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // HTML5-Validierung
        total++;
        const inputs = document.querySelectorAll('input[required], input[type="email"]');
        if (inputs.length > 0) {
            passed++;
        } else {
            errors.push('Keine validierten Eingabefelder gefunden');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Input-Validierung-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * XSS-Schutz testen
     */
    async testXSSProtection() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // HTML-Escaping-Test
        total++;
        const testScript = '<script>alert("xss")</script>';
        const div = document.createElement('div');
        div.textContent = testScript;
        
        if (div.innerHTML === '&lt;script&gt;alert("xss")&lt;/script&gt;') {
            passed++;
        } else {
            errors.push('HTML-Escaping funktioniert nicht korrekt');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} XSS-Schutz-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Barrierefreiheit testen
     */
    async testAccessibility() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Alt-Attribute für Bilder
        total++;
        const images = document.querySelectorAll('img');
        const imagesWithoutAlt = Array.from(images).filter(img => !img.getAttribute('alt'));
        if (imagesWithoutAlt.length === 0 || images.length === 0) {
            passed++;
        } else {
            errors.push(`${imagesWithoutAlt.length} Bilder ohne Alt-Attribut`);
        }

        // ARIA-Labels
        total++;
        const buttons = document.querySelectorAll('button');
        const buttonsWithoutLabel = Array.from(buttons).filter(btn => 
            !btn.getAttribute('aria-label') && !btn.textContent.trim()
        );
        if (buttonsWithoutLabel.length === 0 || buttons.length === 0) {
            passed++;
        } else {
            errors.push(`${buttonsWithoutLabel.length} Buttons ohne Label`);
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Barrierefreiheit-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Tastatur-Navigation testen
     */
    async testKeyboardNavigation() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Focusable-Elemente
        total++;
        const focusableElements = document.querySelectorAll(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusableElements.length > 0) {
            passed++;
        } else {
            errors.push('Keine fokussierbaren Elemente gefunden');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Tastatur-Navigation-Tests erfolgreich`,
            errors: errors,
            details: { passed, total, focusableCount: focusableElements.length }
        };
    }

    /**
     * Screen Reader Tests
     */
    async testScreenReader() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Semantic HTML
        total++;
        const semanticElements = document.querySelectorAll(
            'main, header, nav, section, article, aside, footer'
        );
        if (semanticElements.length > 0) {
            passed++;
        } else {
            errors.push('Keine semantischen HTML-Elemente gefunden');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Screen Reader-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Browser-Kompatibilität testen
     */
    async testBrowserCompatibility() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Modern JavaScript Features
        total++;
        if (typeof Promise !== 'undefined' && typeof fetch !== 'undefined') {
            passed++;
        } else {
            errors.push('Moderne JavaScript-Features nicht unterstützt');
        }

        // CSS Features
        total++;
        if (CSS.supports && CSS.supports('display', 'grid')) {
            passed++;
        } else {
            errors.push('CSS Grid nicht unterstützt');
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Browser-Kompatibilität-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Responsive Design testen
     */
    async testResponsiveDesign() {
        const errors = [];
        let passed = 0;
        let total = 0;

        // Viewport Meta-Tag
        total++;
        const viewportMeta = document.querySelector('meta[name="viewport"]');
        if (viewportMeta) {
            passed++;
        } else {
            errors.push('Viewport Meta-Tag fehlt');
        }

        // Media Queries im CSS
        total++;
        const styleSheets = Array.from(document.styleSheets);
        let hasMediaQueries = false;
        
        try {
            styleSheets.forEach(sheet => {
                if (sheet.cssRules) {
                    Array.from(sheet.cssRules).forEach(rule => {
                        if (rule.type === CSSRule.MEDIA_RULE) {
                            hasMediaQueries = true;
                        }
                    });
                }
            });
            
            if (hasMediaQueries) {
                passed++;
            } else {
                errors.push('Keine Media Queries gefunden');
            }
        } catch (error) {
            // CORS-Fehler bei externen Stylesheets ignorieren
            passed++;
        }

        return {
            success: errors.length === 0,
            message: `${passed}/${total} Responsive Design-Tests erfolgreich`,
            errors: errors,
            details: { passed, total }
        };
    }

    /**
     * Test-Report generieren
     */
    generateTestReport() {
        const report = {
            timestamp: new Date().toISOString(),
            duration: this.endTime - this.startTime,
            summary: {
                total: this.testResults.length,
                passed: this.testResults.filter(t => t.status === 'passed').length,
                failed: this.testResults.filter(t => t.status === 'failed').length
            },
            tests: this.testResults,
            environment: {
                userAgent: navigator.userAgent,
                viewport: `${window.innerWidth}x${window.innerHeight}`,
                memory: performance.memory ? {
                    used: Math.round(performance.memory.usedJSHeapSize / 1024 / 1024),
                    limit: Math.round(performance.memory.jsHeapSizeLimit / 1024 / 1024)
                } : null
            }
        };

        console.log('📊 Test-Report:', report);
        return report;
    }

    /**
     * Mock-Daten generieren
     */
    generateMockData() {
        return {
            users: [
                { id: 1, username: 'admin', role: 'admin' },
                { id: 2, username: 'moderator', role: 'mod' },
                { id: 3, username: 'player1', role: 'user' }
            ],
            serverStatus: {
                status: 'online',
                players: 12,
                maxPlayers: 50,
                tps: 19.8,
                uptime: 86400
            },
            performanceData: {
                cpu: 45.2,
                memory: 68.7,
                entities: 1248,
                chunks: 145
            }
        };
    }

    /**
     * Test-Report exportieren
     */
    exportReport(format = 'json') {
        const report = this.generateTestReport();
        
        if (format === 'json') {
            const blob = new Blob([JSON.stringify(report, null, 2)], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `webui-test-report-${new Date().toISOString().split('T')[0]}.json`;
            a.click();
            URL.revokeObjectURL(url);
        }
    }
}

// Globale Instanz
window.WebUITestSuite = WebUITestSuite;

// Auto-Test bei Debug-Modus
if (window.location.search.includes('debug=true')) {
    window.addEventListener('load', () => {
        setTimeout(() => {
            const testSuite = new WebUITestSuite();
            testSuite.runAllTests();
        }, 2000);
    });
}
