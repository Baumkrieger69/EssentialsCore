/**
 * EssentialsCore Advanced WebUI - Data Visualization Module
 * Chart.js Integration für erweiterte Datenvisualisierung
 */

class WebUIDataVisualization {
    constructor(webuiInstance) {
        this.webui = webuiInstance;
        this.charts = new Map();
        this.chartConfigs = new Map();
        this.updateIntervals = new Map();
        this.dataBuffers = new Map();
        this.init();
    }

    /**
     * Initialisiert das Datenvisualisierungs-Modul
     */
    init() {
        this.loadChartJS();
        this.setupChartConfigurations();
        this.registerChartTypes();
    }

    /**
     * Lädt Chart.js dynamisch
     */
    async loadChartJS() {
        if (typeof Chart !== 'undefined') {
            this.onChartJSLoaded();
            return;
        }

        try {
            // Chart.js von CDN laden
            const script = document.createElement('script');
            script.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js';
            script.onload = () => this.onChartJSLoaded();
            script.onerror = () => this.handleChartJSLoadError();
            document.head.appendChild(script);
        } catch (error) {
            console.error('Fehler beim Laden von Chart.js:', error);
            this.handleChartJSLoadError();
        }
    }

    /**
     * Chart.js erfolgreich geladen
     */
    onChartJSLoaded() {
        console.log('Chart.js erfolgreich geladen');
        
        // Chart.js Plugins registrieren
        if (typeof Chart !== 'undefined') {
            Chart.register(...Chart.registerables);
            this.setupDefaultChartStyles();
            this.initializeDefaultCharts();
        }
    }

    /**
     * Fehlerbehandlung für Chart.js Laden
     */
    handleChartJSLoadError() {
        console.warn('Chart.js konnte nicht geladen werden - verwende Fallback-Visualisierung');
        this.useFallbackVisualization();
    }

    /**
     * Standard Chart-Styles einrichten
     */
    setupDefaultChartStyles() {
        Chart.defaults.font.family = 'Inter, -apple-system, BlinkMacSystemFont, sans-serif';
        Chart.defaults.font.size = 12;
        Chart.defaults.color = '#e2e8f0';
        Chart.defaults.borderColor = '#334155';
        Chart.defaults.backgroundColor = 'rgba(59, 130, 246, 0.1)';
    }

    /**
     * Chart-Konfigurationen einrichten
     */
    setupChartConfigurations() {
        this.chartConfigs.set('performance', {
            type: 'line',
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: {
                    duration: 750,
                    easing: 'easeInOutQuart'
                },
                scales: {
                    x: {
                        type: 'time',
                        time: {
                            unit: 'minute',
                            displayFormats: {
                                minute: 'HH:mm'
                            }
                        },
                        grid: {
                            color: '#334155'
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: '#334155'
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            usePointStyle: true,
                            padding: 20
                        }
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false,
                        backgroundColor: '#1e293b',
                        titleColor: '#f1f5f9',
                        bodyColor: '#e2e8f0',
                        borderColor: '#3b82f6',
                        borderWidth: 1
                    }
                },
                elements: {
                    line: {
                        tension: 0.4,
                        borderWidth: 2
                    },
                    point: {
                        radius: 3,
                        hoverRadius: 6
                    }
                }
            }
        });

        this.chartConfigs.set('system-resources', {
            type: 'doughnut',
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'right',
                        labels: {
                            padding: 20,
                            usePointStyle: true
                        }
                    }
                },
                cutout: '60%'
            }
        });

        this.chartConfigs.set('server-activity', {
            type: 'bar',
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        grid: {
                            color: '#334155'
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: '#334155'
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: false
                    }
                }
            }
        });

        this.chartConfigs.set('network-traffic', {
            type: 'line',
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        type: 'time',
                        time: {
                            unit: 'second',
                            displayFormats: {
                                second: 'HH:mm:ss'
                            }
                        },
                        grid: {
                            color: '#334155'
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: '#334155'
                        }
                    }
                },
                elements: {
                    line: {
                        tension: 0.1,
                        fill: true
                    }
                }
            }
        });
    }

    /**
     * Standard-Charts initialisieren
     */
    initializeDefaultCharts() {
        // Performance-Chart
        this.createChart('server-performance', 'performance', {
            labels: [],
            datasets: [
                {
                    label: 'CPU %',
                    data: [],
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)'
                },
                {
                    label: 'RAM %',
                    data: [],
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)'
                },
                {
                    label: 'TPS',
                    data: [],
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)'
                }
            ]
        });

        // System-Ressourcen Chart
        this.createChart('system-resources', 'system-resources', {
            labels: ['Verwendet', 'Verfügbar'],
            datasets: [{
                data: [65, 35],
                backgroundColor: [
                    '#3b82f6',
                    '#64748b'
                ],
                borderWidth: 0
            }]
        });

        // Server-Aktivität Chart
        this.createChart('server-activity', 'server-activity', {
            labels: ['Spieler', 'Chunks', 'Entities', 'Blöcke/s'],
            datasets: [{
                data: [12, 145, 892, 1567],
                backgroundColor: [
                    '#3b82f6',
                    '#10b981',
                    '#f59e0b',
                    '#ef4444'
                ]
            }]
        });

        // Netzwerk-Traffic Chart
        this.createChart('network-traffic', 'network-traffic', {
            labels: [],
            datasets: [
                {
                    label: 'Upload (KB/s)',
                    data: [],
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    fill: true
                },
                {
                    label: 'Download (KB/s)',
                    data: [],
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    fill: true
                }
            ]
        });

        this.startDataUpdates();
    }

    /**
     * Chart erstellen
     */
    createChart(elementId, configKey, data) {
        const canvas = document.getElementById(elementId);
        if (!canvas) {
            console.warn(`Canvas-Element mit ID "${elementId}" nicht gefunden`);
            return null;
        }

        const config = this.chartConfigs.get(configKey);
        if (!config) {
            console.error(`Chart-Konfiguration "${configKey}" nicht gefunden`);
            return null;
        }

        try {
            const chart = new Chart(canvas, {
                type: config.type,
                data: data,
                options: config.options
            });

            this.charts.set(elementId, chart);
            this.dataBuffers.set(elementId, []);
            
            console.log(`Chart "${elementId}" erfolgreich erstellt`);
            return chart;
        } catch (error) {
            console.error(`Fehler beim Erstellen des Charts "${elementId}":`, error);
            return null;
        }
    }

    /**
     * Chart-Daten aktualisieren
     */
    updateChart(chartId, newData) {
        const chart = this.charts.get(chartId);
        if (!chart) return;

        try {
            if (chartId === 'server-performance' || chartId === 'network-traffic') {
                // Zeit-basierte Charts
                const now = new Date();
                
                // Neue Datenpunkte hinzufügen
                chart.data.labels.push(now);
                chart.data.datasets.forEach((dataset, index) => {
                    dataset.data.push(newData[index] || 0);
                });

                // Alte Datenpunkte entfernen (max. 20 Punkte)
                const maxPoints = 20;
                if (chart.data.labels.length > maxPoints) {
                    chart.data.labels.shift();
                    chart.data.datasets.forEach(dataset => {
                        dataset.data.shift();
                    });
                }
            } else {
                // Statische Charts
                chart.data.datasets[0].data = newData;
            }

            chart.update('none'); // Ohne Animation für Performance
        } catch (error) {
            console.error(`Fehler beim Aktualisieren des Charts "${chartId}":`, error);
        }
    }

    /**
     * Daten-Updates starten
     */
    startDataUpdates() {
        // Performance-Daten alle 5 Sekunden
        this.updateIntervals.set('performance', setInterval(() => {
            this.updatePerformanceData();
        }, 5000));

        // System-Ressourcen alle 10 Sekunden
        this.updateIntervals.set('resources', setInterval(() => {
            this.updateResourceData();
        }, 10000));

        // Netzwerk-Traffic alle 2 Sekunden
        this.updateIntervals.set('network', setInterval(() => {
            this.updateNetworkData();
        }, 2000));
    }

    /**
     * Performance-Daten aktualisieren
     */
    async updatePerformanceData() {
        try {
            // Simulierte Daten - in Produktion von API abrufen
            const cpuUsage = Math.random() * 100;
            const ramUsage = Math.random() * 100;
            const tps = 18 + Math.random() * 4; // 18-22 TPS

            this.updateChart('server-performance', [cpuUsage, ramUsage, tps]);
        } catch (error) {
            console.error('Fehler beim Aktualisieren der Performance-Daten:', error);
        }
    }

    /**
     * Ressourcen-Daten aktualisieren
     */
    async updateResourceData() {
        try {
            // Simulierte Daten
            const used = 60 + Math.random() * 20;
            const available = 100 - used;

            this.updateChart('system-resources', [used, available]);
        } catch (error) {
            console.error('Fehler beim Aktualisieren der Ressourcen-Daten:', error);
        }
    }

    /**
     * Netzwerk-Daten aktualisieren
     */
    async updateNetworkData() {
        try {
            // Simulierte Daten
            const upload = Math.random() * 1000;
            const download = Math.random() * 2000;

            this.updateChart('network-traffic', [upload, download]);
        } catch (error) {
            console.error('Fehler beim Aktualisieren der Netzwerk-Daten:', error);
        }
    }

    /**
     * Fallback-Visualisierung ohne Chart.js
     */
    useFallbackVisualization() {
        console.log('Verwende Fallback-Visualisierung mit CSS und SVG');
        
        // Einfache CSS-basierte Balkendiagramme
        const charts = document.querySelectorAll('[id$="-chart"]');
        charts.forEach(chart => {
            this.createFallbackChart(chart);
        });
    }

    /**
     * Einfaches CSS-Chart erstellen
     */
    createFallbackChart(container) {
        container.innerHTML = `
            <div class="fallback-chart">
                <div class="chart-title">Performance Monitor</div>
                <div class="chart-bars">
                    <div class="bar" style="height: 65%">
                        <span class="bar-label">CPU</span>
                        <span class="bar-value">65%</span>
                    </div>
                    <div class="bar" style="height: 45%">
                        <span class="bar-label">RAM</span>
                        <span class="bar-value">45%</span>
                    </div>
                    <div class="bar" style="height: 20%">
                        <span class="bar-label">TPS</span>
                        <span class="bar-value">20</span>
                    </div>
                </div>
            </div>
        `;

        // CSS-Styles hinzufügen
        if (!document.getElementById('fallback-chart-styles')) {
            const style = document.createElement('style');
            style.id = 'fallback-chart-styles';
            style.textContent = `
                .fallback-chart {
                    padding: 20px;
                    background: #1e293b;
                    border-radius: 8px;
                    height: 100%;
                }
                .chart-title {
                    color: #f1f5f9;
                    font-weight: 600;
                    margin-bottom: 20px;
                }
                .chart-bars {
                    display: flex;
                    align-items: flex-end;
                    height: 200px;
                    gap: 10px;
                }
                .bar {
                    flex: 1;
                    background: linear-gradient(to top, #3b82f6, #60a5fa);
                    border-radius: 4px 4px 0 0;
                    position: relative;
                    min-height: 20px;
                    transition: height 0.3s ease;
                }
                .bar-label {
                    position: absolute;
                    bottom: -25px;
                    left: 50%;
                    transform: translateX(-50%);
                    color: #94a3b8;
                    font-size: 12px;
                }
                .bar-value {
                    position: absolute;
                    top: -25px;
                    left: 50%;
                    transform: translateX(-50%);
                    color: #f1f5f9;
                    font-size: 12px;
                    font-weight: 600;
                }
            `;
            document.head.appendChild(style);
        }
    }

    /**
     * Chart exportieren
     */
    exportChart(chartId, format = 'png') {
        const chart = this.charts.get(chartId);
        if (!chart) {
            console.error(`Chart "${chartId}" nicht gefunden`);
            return;
        }

        try {
            const url = chart.toBase64Image();
            const link = document.createElement('a');
            link.download = `${chartId}-${new Date().toISOString().split('T')[0]}.${format}`;
            link.href = url;
            link.click();
        } catch (error) {
            console.error(`Fehler beim Exportieren des Charts "${chartId}":`, error);
        }
    }

    /**
     * Alle Charts zerstören
     */
    destroy() {
        // Update-Intervalle stoppen
        this.updateIntervals.forEach(interval => clearInterval(interval));
        this.updateIntervals.clear();

        // Charts zerstören
        this.charts.forEach(chart => chart.destroy());
        this.charts.clear();

        // Buffer leeren
        this.dataBuffers.clear();
    }

    /**
     * Chart-Typen registrieren
     */
    registerChartTypes() {
        // Hier können custom Chart-Typen registriert werden
        console.log('Chart-Typen registriert');
    }

    /**
     * Theme ändern
     */
    updateTheme(theme) {
        if (typeof Chart === 'undefined') return;

        const isDark = theme === 'dark';
        Chart.defaults.color = isDark ? '#e2e8f0' : '#374151';
        Chart.defaults.borderColor = isDark ? '#334155' : '#d1d5db';

        // Alle Charts mit neuen Theme-Farben aktualisieren
        this.charts.forEach(chart => {
            chart.update();
        });
    }
}

// Globale Instanz für Zugriff von außen
window.WebUIDataVisualization = WebUIDataVisualization;
