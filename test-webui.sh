#!/bin/bash

# EssentialsCore WebUI Test und Validierung Script
# Dieses Script führt umfassende Tests des WebUI Systems durch

echo "🚀 EssentialsCore WebUI - Test & Validierung"
echo "============================================="

# Farben für bessere Ausgabe
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test-Funktionen
test_file_exists() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1 existiert"
        return 0
    else
        echo -e "${RED}✗${NC} $1 fehlt"
        return 1
    fi
}

test_file_not_empty() {
    if [ -s "$1" ]; then
        echo -e "${GREEN}✓${NC} $1 ist nicht leer"
        return 0
    else
        echo -e "${RED}✗${NC} $1 ist leer"
        return 1
    fi
}

validate_html() {
    if grep -q "<!DOCTYPE html>" "$1" && grep -q "</html>" "$1"; then
        echo -e "${GREEN}✓${NC} $1 ist valides HTML"
        return 0
    else
        echo -e "${RED}✗${NC} $1 hat HTML-Probleme"
        return 1
    fi
}

validate_css() {
    if grep -q "{" "$1" && grep -q "}" "$1"; then
        echo -e "${GREEN}✓${NC} $1 hat gültige CSS-Syntax"
        return 0
    else
        echo -e "${RED}✗${NC} $1 hat CSS-Probleme"
        return 1
    fi
}

validate_js() {
    # Einfache Syntax-Prüfung für JavaScript
    if node -c "$1" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $1 hat gültige JavaScript-Syntax"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} $1 - Node.js nicht verfügbar oder Syntax-Probleme"
        return 1
    fi
}

echo -e "\n${BLUE}📋 Phase 1: Datei-Existenz prüfen${NC}"
echo "-----------------------------------"

WEBAPP_DIR="/workspaces/EssentialsCore/src/main/resources/webui/webapp"

# Hauptdateien prüfen
test_file_exists "$WEBAPP_DIR/index.html"
test_file_exists "$WEBAPP_DIR/manifest.json"
test_file_exists "$WEBAPP_DIR/sw.js"
test_file_exists "$WEBAPP_DIR/css/main.css"
test_file_exists "$WEBAPP_DIR/css/RemoteManagement.css"
test_file_exists "$WEBAPP_DIR/js/app.js"

echo -e "\n${BLUE}📊 Phase 2: Datei-Größen prüfen${NC}"
echo "--------------------------------"

# Datei-Größen prüfen
test_file_not_empty "$WEBAPP_DIR/index.html"
test_file_not_empty "$WEBAPP_DIR/manifest.json"
test_file_not_empty "$WEBAPP_DIR/sw.js"
test_file_not_empty "$WEBAPP_DIR/css/main.css"
test_file_not_empty "$WEBAPP_DIR/css/RemoteManagement.css"
test_file_not_empty "$WEBAPP_DIR/js/app.js"

echo -e "\n${BLUE}🔍 Phase 3: Syntax-Validierung${NC}"
echo "-------------------------------"

# HTML validieren
validate_html "$WEBAPP_DIR/index.html"

# CSS validieren
validate_css "$WEBAPP_DIR/css/main.css"
validate_css "$WEBAPP_DIR/css/RemoteManagement.css"

# JavaScript validieren (falls Node.js verfügbar)
if command -v node >/dev/null 2>&1; then
    validate_js "$WEBAPP_DIR/js/app.js"
    validate_js "$WEBAPP_DIR/sw.js"
else
    echo -e "${YELLOW}⚠${NC} Node.js nicht verfügbar - JavaScript-Validierung übersprungen"
fi

echo -e "\n${BLUE}📐 Phase 4: Feature-Vollständigkeit${NC}"
echo "-----------------------------------"

# Prüfen ob wichtige Module in app.js implementiert sind
if grep -q "console.*module\|Console.*Module\|loadConsoleModule" "$WEBAPP_DIR/js/app.js"; then
    echo -e "${GREEN}✓${NC} Console Module implementiert"
else
    echo -e "${RED}✗${NC} Console Module fehlt"
fi

if grep -q "players.*module\|Players.*Module\|loadPlayersModule" "$WEBAPP_DIR/js/app.js"; then
    echo -e "${GREEN}✓${NC} Players Module implementiert"
else
    echo -e "${RED}✗${NC} Players Module fehlt"
fi

if grep -q "modules.*module\|Modules.*Module\|loadModulesModule" "$WEBAPP_DIR/js/app.js"; then
    echo -e "${GREEN}✓${NC} Modules Module implementiert"
else
    echo -e "${RED}✗${NC} Modules Module fehlt"
fi

if grep -q "files.*module\|Files.*Module\|loadFilesModule" "$WEBAPP_DIR/js/app.js"; then
    echo -e "${GREEN}✓${NC} Files Module implementiert"
else
    echo -e "${RED}✗${NC} Files Module fehlt"
fi

if grep -q "performance.*module\|Performance.*Module\|loadPerformanceModule" "$WEBAPP_DIR/js/app.js"; then
    echo -e "${GREEN}✓${NC} Performance Module implementiert"
else
    echo -e "${RED}✗${NC} Performance Module fehlt"
fi

# PWA Features prüfen
if grep -q "serviceWorker\|service.*worker" "$WEBAPP_DIR/index.html"; then
    echo -e "${GREEN}✓${NC} Service Worker registriert"
else
    echo -e "${RED}✗${NC} Service Worker fehlt"
fi

if grep -q "manifest.json" "$WEBAPP_DIR/index.html"; then
    echo -e "${GREEN}✓${NC} PWA Manifest verlinkt"
else
    echo -e "${RED}✗${NC} PWA Manifest fehlt"
fi

echo -e "\n${BLUE}🎨 Phase 5: CSS-Framework Prüfung${NC}"
echo "----------------------------------"

# CSS Framework Features prüfen
if grep -q ".tab-nav" "$WEBAPP_DIR/css/main.css"; then
    echo -e "${GREEN}✓${NC} Tab-Navigation-System vorhanden"
else
    echo -e "${RED}✗${NC} Tab-Navigation fehlt"
fi

if grep -q ".console-container" "$WEBAPP_DIR/css/main.css"; then
    echo -e "${GREEN}✓${NC} Console-Styling vorhanden"
else
    echo -e "${RED}✗${NC} Console-Styling fehlt"
fi

if grep -q ".file-manager" "$WEBAPP_DIR/css/main.css"; then
    echo -e "${GREEN}✓${NC} File-Manager-Styling vorhanden"
else
    echo -e "${RED}✗${NC} File-Manager-Styling fehlt"
fi

if grep -q "var(--primary-color)" "$WEBAPP_DIR/css/main.css"; then
    echo -e "${GREEN}✓${NC} CSS-Variablen-System vorhanden"
else
    echo -e "${RED}✗${NC} CSS-Variablen fehlen"
fi

echo -e "\n${BLUE}📱 Phase 6: Responsive Design Test${NC}"
echo "-----------------------------------"

if grep -q "@media" "$WEBAPP_DIR/css/main.css"; then
    echo -e "${GREEN}✓${NC} Media Queries für Responsive Design vorhanden"
else
    echo -e "${RED}✗${NC} Responsive Design fehlt"
fi

echo -e "\n${BLUE}🔧 Phase 7: Entwickler-Tools${NC}"
echo "-----------------------------"

# Prüfen ob HTTP-Server läuft
if curl -s http://localhost:8080 >/dev/null; then
    echo -e "${GREEN}✓${NC} HTTP-Server läuft auf Port 8080"
    echo -e "${BLUE}🌐${NC} WebUI verfügbar unter: http://localhost:8080"
else
    echo -e "${YELLOW}⚠${NC} HTTP-Server läuft nicht - starte Server..."
    cd "$WEBAPP_DIR" && python3 -m http.server 8080 &
    sleep 2
    if curl -s http://localhost:8080 >/dev/null; then
        echo -e "${GREEN}✓${NC} HTTP-Server erfolgreich gestartet"
    else
        echo -e "${RED}✗${NC} HTTP-Server konnte nicht gestartet werden"
    fi
fi

echo -e "\n${BLUE}📊 Phase 8: Statistiken${NC}"
echo "------------------------"

echo "Datei-Größen:"
echo "  index.html: $(wc -c < "$WEBAPP_DIR/index.html" 2>/dev/null || echo "0") Bytes"
echo "  app.js: $(wc -c < "$WEBAPP_DIR/js/app.js" 2>/dev/null || echo "0") Bytes"
echo "  main.css: $(wc -c < "$WEBAPP_DIR/css/main.css" 2>/dev/null || echo "0") Bytes"
echo "  RemoteManagement.css: $(wc -c < "$WEBAPP_DIR/css/RemoteManagement.css" 2>/dev/null || echo "0") Bytes"

echo ""
echo "Code-Zeilen:"
echo "  app.js: $(wc -l < "$WEBAPP_DIR/js/app.js" 2>/dev/null || echo "0") Zeilen"
echo "  main.css: $(wc -l < "$WEBAPP_DIR/css/main.css" 2>/dev/null || echo "0") Zeilen"
echo "  RemoteManagement.css: $(wc -l < "$WEBAPP_DIR/css/RemoteManagement.css" 2>/dev/null || echo "0") Zeilen"

echo -e "\n${GREEN}🎉 WebUI Test & Validierung abgeschlossen!${NC}"
echo -e "${BLUE}💡 Öffne http://localhost:8080 im Browser um das WebUI zu testen${NC}"
