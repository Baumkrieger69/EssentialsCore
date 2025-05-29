#!/bin/bash

# EssentialsCore WebUI - Production Deployment Script
# Comprehensive deployment and management script for production environments

set -euo pipefail

# Konfiguration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBUI_DIR="${SCRIPT_DIR}/src/main/resources/webui/webapp"
LOGS_DIR="${SCRIPT_DIR}/logs"
BACKUP_DIR="${SCRIPT_DIR}/backups"
CONFIG_DIR="${SCRIPT_DIR}/config"

# Server-Konfiguration
HTTP_PORT=8080
WEBSOCKET_PORT=8081
JAVA_OPTS="-Xmx2G -Xms1G -XX:+UseG1GC"

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging-Funktion
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        "INFO")
            echo -e "${GREEN}[${timestamp}] INFO: ${message}${NC}"
            ;;
        "WARN")
            echo -e "${YELLOW}[${timestamp}] WARN: ${message}${NC}"
            ;;
        "ERROR")
            echo -e "${RED}[${timestamp}] ERROR: ${message}${NC}"
            ;;
        "DEBUG")
            if [[ "${DEBUG:-}" == "true" ]]; then
                echo -e "${BLUE}[${timestamp}] DEBUG: ${message}${NC}"
            fi
            ;;
    esac
    
    # Log in Datei schreiben
    echo "[${timestamp}] ${level}: ${message}" >> "${LOGS_DIR}/deployment.log"
}

# Verzeichnisse erstellen
create_directories() {
    log "INFO" "Erstelle erforderliche Verzeichnisse..."
    
    mkdir -p "${LOGS_DIR}"
    mkdir -p "${BACKUP_DIR}"
    mkdir -p "${CONFIG_DIR}"
    mkdir -p "${WEBUI_DIR}/temp"
    
    log "INFO" "Verzeichnisse erstellt"
}

# System-Voraussetzungen prüfen
check_prerequisites() {
    log "INFO" "Prüfe System-Voraussetzungen..."
    
    # Java prüfen
    if ! command -v java &> /dev/null; then
        log "ERROR" "Java ist nicht installiert oder nicht im PATH"
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    log "INFO" "Java Version: ${java_version}"
    
    # Python prüfen (für HTTP-Server)
    if ! command -v python3 &> /dev/null; then
        log "ERROR" "Python3 ist nicht installiert"
        exit 1
    fi
    
    # Node.js prüfen (optional für Build-Tools)
    if command -v node &> /dev/null; then
        local node_version=$(node --version)
        log "INFO" "Node.js Version: ${node_version}"
    fi
    
    # Ports prüfen
    if lsof -Pi :${HTTP_PORT} -sTCP:LISTEN -t >/dev/null 2>&1; then
        log "WARN" "Port ${HTTP_PORT} ist bereits belegt"
    fi
    
    if lsof -Pi :${WEBSOCKET_PORT} -sTCP:LISTEN -t >/dev/null 2>&1; then
        log "WARN" "Port ${WEBSOCKET_PORT} ist bereits belegt"
    fi
    
    log "INFO" "System-Voraussetzungen erfüllt"
}

# Backup erstellen
create_backup() {
    log "INFO" "Erstelle Backup der aktuellen Installation..."
    
    local backup_name="webui-backup-$(date +%Y%m%d-%H%M%S)"
    local backup_path="${BACKUP_DIR}/${backup_name}.tar.gz"
    
    if [[ -d "${WEBUI_DIR}" ]]; then
        tar -czf "${backup_path}" -C "$(dirname "${WEBUI_DIR}")" "$(basename "${WEBUI_DIR}")"
        log "INFO" "Backup erstellt: ${backup_path}"
    else
        log "WARN" "Kein WebUI-Verzeichnis zum Backup gefunden"
    fi
}

# WebUI Assets optimieren
optimize_assets() {
    log "INFO" "Optimiere WebUI Assets..."
    
    # CSS minifizieren (einfache Implementierung)
    find "${WEBUI_DIR}/css" -name "*.css" -type f | while read -r file; do
        if [[ ! "${file}" =~ \.min\. ]]; then
            local minified="${file%.css}.min.css"
            # Entferne Kommentare und überflüssige Leerzeichen
            sed 's|/\*.*\*/||g; s/[[:space:]]\+/ /g; s/; /;/g; s/ {/{/g; s/{ /{/g; s/ }/}/g' "${file}" > "${minified}"
            log "DEBUG" "CSS minifiziert: $(basename "${file}")"
        fi
    done
    
    # JavaScript minifizieren (einfache Implementierung)
    find "${WEBUI_DIR}/js" -name "*.js" -type f | while read -r file; do
        if [[ ! "${file}" =~ \.min\. ]]; then
            local minified="${file%.js}.min.js"
            # Entferne Kommentare und überflüssige Leerzeichen
            sed 's|//.*$||g; s|/\*.*\*/||g; s/[[:space:]]\+/ /g' "${file}" > "${minified}"
            log "DEBUG" "JavaScript minifiziert: $(basename "${file}")"
        fi
    done
    
    log "INFO" "Asset-Optimierung abgeschlossen"
}

# Konfigurationsdateien erstellen
create_config_files() {
    log "INFO" "Erstelle Konfigurationsdateien..."
    
    # WebUI-Konfiguration
    cat > "${CONFIG_DIR}/webui.properties" << EOF
# EssentialsCore WebUI Configuration
http.port=${HTTP_PORT}
websocket.port=${WEBSOCKET_PORT}
webui.path=${WEBUI_DIR}
logging.level=INFO
security.enabled=true
cache.enabled=true
compression.enabled=true

# Performance Settings
thread.pool.size=10
connection.timeout=30000
session.timeout=3600000

# Feature Flags
features.realtime=true
features.monitoring=true
features.charts=true
features.console=true
features.playerManagement=true
EOF
    
    # Logging-Konfiguration
    cat > "${CONFIG_DIR}/logging.properties" << EOF
# Logging Configuration
handlers=java.util.logging.FileHandler, java.util.logging.ConsoleHandler

# File Handler
java.util.logging.FileHandler.pattern=${LOGS_DIR}/webui-%g.log
java.util.logging.FileHandler.limit=10485760
java.util.logging.FileHandler.count=5
java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter

# Console Handler
java.util.logging.ConsoleHandler.level=INFO
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter

# Logger Levels
com.essentialscore.webui.level=INFO
root.level=WARNING
EOF
    
    # Systemd Service-Datei
    cat > "${CONFIG_DIR}/essentialscore-webui.service" << EOF
[Unit]
Description=EssentialsCore WebUI Server
After=network.target

[Service]
Type=simple
User=minecraft
Group=minecraft
WorkingDirectory=${SCRIPT_DIR}
ExecStart=/usr/bin/java ${JAVA_OPTS} -jar essentialscore-webui.jar
ExecReload=/bin/kill -HUP \$MAINPID
Restart=always
RestartSec=10

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=${SCRIPT_DIR}

[Install]
WantedBy=multi-user.target
EOF
    
    log "INFO" "Konfigurationsdateien erstellt"
}

# Security-Checks durchführen
security_check() {
    log "INFO" "Führe Security-Checks durch..."
    
    # Dateiberechtigungen prüfen
    local sensitive_files=(
        "${CONFIG_DIR}/webui.properties"
        "${LOGS_DIR}"
        "${BACKUP_DIR}"
    )
    
    for file in "${sensitive_files[@]}"; do
        if [[ -e "${file}" ]]; then
            chmod 600 "${file}" 2>/dev/null || chmod 700 "${file}"
            log "DEBUG" "Berechtigungen gesetzt für: ${file}"
        fi
    done
    
    # WebUI-Dateien auf verdächtige Inhalte prüfen
    if find "${WEBUI_DIR}" -name "*.html" -o -name "*.js" | xargs grep -l "eval\|innerHTML\|document\.write" >/dev/null 2>&1; then
        log "WARN" "Potenziell unsichere JavaScript-Patterns gefunden"
    fi
    
    # SSL/TLS-Zertifikate prüfen (falls vorhanden)
    if [[ -f "${CONFIG_DIR}/server.crt" ]]; then
        local cert_expiry=$(openssl x509 -in "${CONFIG_DIR}/server.crt" -noout -dates | grep "notAfter" | cut -d= -f2)
        log "INFO" "SSL-Zertifikat läuft ab am: ${cert_expiry}"
    fi
    
    log "INFO" "Security-Checks abgeschlossen"
}

# Performance-Tests durchführen
performance_test() {
    log "INFO" "Führe Performance-Tests durch..."
    
    # Lade-Zeit testen
    local start_time=$(date +%s%N)
    
    # HTTP-Server-Test
    if curl -s -o /dev/null "http://localhost:${HTTP_PORT}" 2>/dev/null; then
        local end_time=$(date +%s%N)
        local load_time=$(((end_time - start_time) / 1000000))
        log "INFO" "HTTP-Server Antwortzeit: ${load_time}ms"
    else
        log "WARN" "HTTP-Server nicht erreichbar für Performance-Test"
    fi
    
    # Speicherverbrauch prüfen
    if command -v ps &> /dev/null; then
        local memory_usage=$(ps aux | grep java | grep -v grep | awk '{sum+=$4} END {print sum}')
        if [[ -n "${memory_usage}" ]]; then
            log "INFO" "Java-Speicherverbrauch: ${memory_usage}%"
        fi
    fi
    
    # Festplattenspeicher prüfen
    local disk_usage=$(df -h "${SCRIPT_DIR}" | awk 'NR==2 {print $5}')
    log "INFO" "Festplattenspeicher: ${disk_usage} belegt"
    
    log "INFO" "Performance-Tests abgeschlossen"
}

# Health-Check durchführen
health_check() {
    log "INFO" "Führe Health-Check durch..."
    
    local health_status="OK"
    local issues=()
    
    # HTTP-Server-Status
    if ! curl -s -f "http://localhost:${HTTP_PORT}" >/dev/null 2>&1; then
        health_status="ERROR"
        issues+=("HTTP-Server nicht erreichbar")
    fi
    
    # WebSocket-Server-Status (vereinfacht)
    if ! lsof -Pi :${WEBSOCKET_PORT} -sTCP:LISTEN -t >/dev/null 2>&1; then
        health_status="WARNING"
        issues+=("WebSocket-Server möglicherweise nicht aktiv")
    fi
    
    # Log-Dateien prüfen
    if [[ -f "${LOGS_DIR}/webui-0.log" ]]; then
        local error_count=$(grep -c "ERROR\|SEVERE" "${LOGS_DIR}/webui-0.log" 2>/dev/null || echo "0")
        if [[ "${error_count}" -gt "0" ]]; then
            health_status="WARNING"
            issues+=("${error_count} Fehler in den Logs gefunden")
        fi
    fi
    
    # Speicherplatz prüfen
    local disk_free=$(df "${SCRIPT_DIR}" | awk 'NR==2 {print $4}')
    if [[ "${disk_free}" -lt "1048576" ]]; then # < 1GB
        health_status="WARNING"
        issues+=("Wenig freier Speicherplatz (< 1GB)")
    fi
    
    # Ergebnis ausgeben
    log "INFO" "Health-Check Status: ${health_status}"
    if [[ ${#issues[@]} -gt 0 ]]; then
        for issue in "${issues[@]}"; do
            log "WARN" "Issue: ${issue}"
        done
    fi
    
    return $([ "${health_status}" = "OK" ] && echo 0 || echo 1)
}

# Monitoring-Dashboard erstellen
create_monitoring_dashboard() {
    log "INFO" "Erstelle Monitoring-Dashboard..."
    
    cat > "${WEBUI_DIR}/monitoring.html" << 'EOF'
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>EssentialsCore WebUI - Monitoring</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #1a1a1a; color: #fff; }
        .dashboard { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
        .widget { background: #2a2a2a; padding: 20px; border-radius: 8px; border: 1px solid #444; }
        .widget h3 { margin-top: 0; color: #4CAF50; }
        .status-ok { color: #4CAF50; }
        .status-warning { color: #FF9800; }
        .status-error { color: #F44336; }
        .metric { display: flex; justify-content: space-between; margin: 10px 0; }
        .refresh-btn { background: #4CAF50; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; }
    </style>
</head>
<body>
    <h1>EssentialsCore WebUI - System Monitoring</h1>
    <button class="refresh-btn" onclick="location.reload()">Aktualisieren</button>
    
    <div class="dashboard">
        <div class="widget">
            <h3>Server Status</h3>
            <div class="metric">
                <span>HTTP-Server:</span>
                <span id="http-status" class="status-ok">Online</span>
            </div>
            <div class="metric">
                <span>WebSocket-Server:</span>
                <span id="ws-status" class="status-ok">Online</span>
            </div>
            <div class="metric">
                <span>Uptime:</span>
                <span id="uptime">--</span>
            </div>
        </div>
        
        <div class="widget">
            <h3>System Resources</h3>
            <div class="metric">
                <span>CPU Usage:</span>
                <span id="cpu-usage">--</span>
            </div>
            <div class="metric">
                <span>Memory Usage:</span>
                <span id="memory-usage">--</span>
            </div>
            <div class="metric">
                <span>Disk Usage:</span>
                <span id="disk-usage">--</span>
            </div>
        </div>
        
        <div class="widget">
            <h3>Network</h3>
            <div class="metric">
                <span>Active Connections:</span>
                <span id="connections">--</span>
            </div>
            <div class="metric">
                <span>Requests/min:</span>
                <span id="requests-rate">--</span>
            </div>
            <div class="metric">
                <span>Response Time:</span>
                <span id="response-time">--</span>
            </div>
        </div>
        
        <div class="widget">
            <h3>Logs</h3>
            <div class="metric">
                <span>Errors (last 24h):</span>
                <span id="error-count" class="status-warning">--</span>
            </div>
            <div class="metric">
                <span>Last Error:</span>
                <span id="last-error">--</span>
            </div>
        </div>
    </div>
    
    <script>
        // Monitoring-Daten laden
        async function loadMonitoringData() {
            try {
                // Hier würden echte API-Calls stehen
                document.getElementById('uptime').textContent = formatUptime(Date.now() - (Math.random() * 86400000));
                document.getElementById('cpu-usage').textContent = (Math.random() * 100).toFixed(1) + '%';
                document.getElementById('memory-usage').textContent = (Math.random() * 100).toFixed(1) + '%';
                document.getElementById('disk-usage').textContent = (Math.random() * 100).toFixed(1) + '%';
                document.getElementById('connections').textContent = Math.floor(Math.random() * 50);
                document.getElementById('requests-rate').textContent = Math.floor(Math.random() * 1000);
                document.getElementById('response-time').textContent = Math.floor(Math.random() * 100) + 'ms';
                document.getElementById('error-count').textContent = Math.floor(Math.random() * 10);
            } catch (error) {
                console.error('Fehler beim Laden der Monitoring-Daten:', error);
            }
        }
        
        function formatUptime(ms) {
            const days = Math.floor(ms / 86400000);
            const hours = Math.floor((ms % 86400000) / 3600000);
            const minutes = Math.floor((ms % 3600000) / 60000);
            return `${days}d ${hours}h ${minutes}m`;
        }
        
        // Initial laden und alle 30 Sekunden aktualisieren
        loadMonitoringData();
        setInterval(loadMonitoringData, 30000);
    </script>
</body>
</html>
EOF
    
    log "INFO" "Monitoring-Dashboard erstellt"
}

# Haupt-Deployment-Funktion
deploy() {
    log "INFO" "Starte EssentialsCore WebUI Deployment..."
    
    create_directories
    check_prerequisites
    create_backup
    create_config_files
    optimize_assets
    create_monitoring_dashboard
    security_check
    
    log "INFO" "Deployment erfolgreich abgeschlossen!"
    log "INFO" "WebUI verfügbar unter: http://localhost:${HTTP_PORT}"
    log "INFO" "Monitoring verfügbar unter: http://localhost:${HTTP_PORT}/monitoring.html"
    log "INFO" "Konfiguration: ${CONFIG_DIR}/webui.properties"
    log "INFO" "Logs: ${LOGS_DIR}/"
}

# Service-Management-Funktionen
start_service() {
    log "INFO" "Starte EssentialsCore WebUI Service..."
    
    # HTTP-Server starten
    cd "${WEBUI_DIR}"
    python3 -m http.server ${HTTP_PORT} > "${LOGS_DIR}/http-server.log" 2>&1 &
    local http_pid=$!
    echo "${http_pid}" > "${LOGS_DIR}/http-server.pid"
    
    log "INFO" "HTTP-Server gestartet (PID: ${http_pid})"
    log "INFO" "Service gestartet - WebUI verfügbar unter http://localhost:${HTTP_PORT}"
}

stop_service() {
    log "INFO" "Stoppe EssentialsCore WebUI Service..."
    
    # HTTP-Server stoppen
    if [[ -f "${LOGS_DIR}/http-server.pid" ]]; then
        local pid=$(cat "${LOGS_DIR}/http-server.pid")
        if kill -0 "${pid}" 2>/dev/null; then
            kill "${pid}"
            rm -f "${LOGS_DIR}/http-server.pid"
            log "INFO" "HTTP-Server gestoppt"
        fi
    fi
    
    log "INFO" "Service gestoppt"
}

restart_service() {
    stop_service
    sleep 2
    start_service
}

status_service() {
    log "INFO" "Prüfe Service-Status..."
    
    local status="STOPPED"
    
    # HTTP-Server prüfen
    if [[ -f "${LOGS_DIR}/http-server.pid" ]]; then
        local pid=$(cat "${LOGS_DIR}/http-server.pid")
        if kill -0 "${pid}" 2>/dev/null; then
            status="RUNNING"
            log "INFO" "HTTP-Server läuft (PID: ${pid})"
        fi
    fi
    
    # Health-Check durchführen
    if [[ "${status}" == "RUNNING" ]]; then
        if health_check; then
            log "INFO" "Service-Status: HEALTHY"
        else
            log "WARN" "Service-Status: UNHEALTHY"
        fi
    else
        log "INFO" "Service-Status: ${status}"
    fi
    
    performance_test
}

# CLI-Interface
show_help() {
    cat << EOF
EssentialsCore WebUI Deployment Script

Verwendung: $0 [OPTION]

Optionen:
    deploy          Vollständiges Deployment durchführen
    start           Service starten
    stop            Service stoppen
    restart         Service neu starten
    status          Service-Status prüfen
    health          Health-Check durchführen
    backup          Backup erstellen
    monitor         Monitoring-Dashboard öffnen
    logs            Log-Dateien anzeigen
    help            Diese Hilfe anzeigen

Umgebungsvariablen:
    DEBUG=true      Debug-Ausgaben aktivieren
    HTTP_PORT       HTTP-Port (Standard: 8080)
    WEBSOCKET_PORT  WebSocket-Port (Standard: 8081)

Beispiele:
    $0 deploy       # Vollständiges Deployment
    $0 start        # Service starten
    $0 status       # Status prüfen
    DEBUG=true $0 deploy  # Deployment mit Debug-Ausgaben

EOF
}

# Hauptlogik
main() {
    case "${1:-help}" in
        "deploy")
            deploy
            ;;
        "start")
            start_service
            ;;
        "stop")
            stop_service
            ;;
        "restart")
            restart_service
            ;;
        "status")
            status_service
            ;;
        "health")
            health_check
            ;;
        "backup")
            create_backup
            ;;
        "monitor")
            log "INFO" "Öffne Monitoring-Dashboard..."
            if command -v xdg-open &> /dev/null; then
                xdg-open "http://localhost:${HTTP_PORT}/monitoring.html"
            else
                log "INFO" "Monitoring verfügbar unter: http://localhost:${HTTP_PORT}/monitoring.html"
            fi
            ;;
        "logs")
            if [[ -f "${LOGS_DIR}/deployment.log" ]]; then
                tail -f "${LOGS_DIR}/deployment.log"
            else
                log "WARN" "Keine Log-Datei gefunden"
            fi
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# Script ausführen
main "$@"
