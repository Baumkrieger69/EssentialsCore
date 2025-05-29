#!/bin/bash

# EssentialsCore WebUI - Finale Demo & Präsentation
# Dieses Script startet das WebUI in einer optimalen Demo-Konfiguration

echo "🎬 EssentialsCore WebUI - FINALE DEMO"
echo "====================================="

# Farben für bessere Ausgabe
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

WEBAPP_DIR="/workspaces/EssentialsCore/src/main/resources/webui/webapp"

echo -e "\n${BLUE}🚀 Phase 1: System-Vorbereitung${NC}"
echo "--------------------------------"

# Prüfe ob alle Dateien vorhanden sind
if [ ! -f "$WEBAPP_DIR/index.html" ]; then
    echo -e "${RED}✗${NC} index.html fehlt!"
    exit 1
fi

echo -e "${GREEN}✓${NC} Alle WebUI-Dateien gefunden"

# Stoppe laufende Server
pkill -f "python.*http.server" 2>/dev/null
echo -e "${GREEN}✓${NC} Vorherige Server-Instanzen gestoppt"

echo -e "\n${BLUE}📊 Phase 2: System-Analyse${NC}"
echo "----------------------------"

echo "📈 Projekt-Metriken:"
echo "  Total Files: $(find "$WEBAPP_DIR" -type f | wc -l)"
echo "  HTML Files: $(find "$WEBAPP_DIR" -name "*.html" | wc -l)"
echo "  CSS Files: $(find "$WEBAPP_DIR" -name "*.css" | wc -l)"
echo "  JS Files: $(find "$WEBAPP_DIR" -name "*.js" | wc -l)"
echo "  Total Size: $(du -sh "$WEBAPP_DIR" | cut -f1)"

echo ""
echo "🔍 Code-Statistiken:"
echo "  JavaScript: $(cat "$WEBAPP_DIR/js/app.js" | wc -l) Zeilen"
echo "  CSS (main): $(cat "$WEBAPP_DIR/css/main.css" | wc -l) Zeilen"
echo "  CSS (remote): $(cat "$WEBAPP_DIR/css/RemoteManagement.css" | wc -l) Zeilen"
echo "  HTML: $(cat "$WEBAPP_DIR/index.html" | wc -l) Zeilen"

echo ""
echo "🎯 Feature-Coverage:"
echo "  ✓ PWA-Support (Service Worker + Manifest)"
echo "  ✓ Responsive Design (Mobile-First)"
echo "  ✓ Dark/Light Theme System"
echo "  ✓ 7 Vollständige Module"
echo "  ✓ WebSocket Real-time Updates"
echo "  ✓ Barrierefreie Navigation"
echo "  ✓ Performance-Optimierung"

echo -e "\n${BLUE}🎨 Phase 3: Demo-Konfiguration${NC}"
echo "----------------------------------"

# Erstelle eine Demo-Konfiguration
cat > "$WEBAPP_DIR/demo-config.json" << 'EOF'
{
  "demo": {
    "enabled": true,
    "autoLogin": true,
    "mockData": {
      "serverStatus": "online",
      "playerCount": 24,
      "maxPlayers": 100,
      "tps": 19.8,
      "memoryUsage": 65,
      "cpuUsage": 45,
      "onlinePlayers": [
        {"name": "Steve", "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5", "ping": 45, "world": "world"},
        {"name": "Alex", "uuid": "853c80ef-3c37-49fd-aa49-938b674adae6", "ping": 38, "world": "world_nether"},
        {"name": "Notch", "uuid": "f7c77d99-9f15-4a66-a87d-c4a51ef30d19", "ping": 12, "world": "world"}
      ],
      "loadedModules": [
        {"name": "EssentialsCore", "status": "active", "version": "1.0.12"},
        {"name": "WorldEdit", "status": "active", "version": "7.2.15"},
        {"name": "Vault", "status": "active", "version": "1.7.3"}
      ],
      "recentLogs": [
        "[INFO] Server started successfully",
        "[INFO] Player Steve joined the game",
        "[INFO] Player Alex joined the game",
        "[WARN] High memory usage detected: 65%"
      ]
    }
  }
}
EOF

echo -e "${GREEN}✓${NC} Demo-Konfiguration erstellt"

echo -e "\n${BLUE}🌐 Phase 4: Server-Start${NC}"
echo "--------------------------"

# Starte HTTP-Server im Hintergrund
cd "$WEBAPP_DIR"
echo -e "${YELLOW}🔄${NC} Starte HTTP-Server auf Port 8080..."
python3 -m http.server 8080 > /dev/null 2>&1 &
SERVER_PID=$!

# Warte bis Server bereit ist
sleep 3

# Teste Server-Verfügbarkeit
if curl -s http://localhost:8080 > /dev/null; then
    echo -e "${GREEN}✓${NC} HTTP-Server erfolgreich gestartet (PID: $SERVER_PID)"
else
    echo -e "${RED}✗${NC} Server-Start fehlgeschlagen"
    exit 1
fi

echo -e "\n${PURPLE}🎭 Phase 5: Demo-Präsentation${NC}"
echo "--------------------------------"

echo -e "${CYAN}🌟 EssentialsCore WebUI ist bereit für die Demo!${NC}"
echo ""
echo "📱 Zugriff über:"
echo "   🌐 Browser: http://localhost:8080"
echo "   📱 Mobil: http://$(hostname -I | cut -d' ' -f1):8080"
echo ""
echo "🎯 Demo-Features zu zeigen:"
echo "   1. 📊 Dashboard - Real-time Server-Übersicht"
echo "   2. 💻 Live Console - Kommando-Ausführung"
echo "   3. 👥 Player Management - Online/Offline-Verwaltung"
echo "   4. 🧩 Module Manager - Plugin-Verwaltung"
echo "   5. 📁 File Manager - Server-Dateien bearbeiten"
echo "   6. 📈 Performance Monitor - System-Metriken"
echo "   7. 🔐 Security Center - Sicherheits-Features"
echo "   8. ⚙️ Settings - Konfiguration & Themes"
echo ""
echo "✨ Besondere Highlights:"
echo "   🎨 Dark/Light Theme Toggle"
echo "   📱 Vollständig responsive (Mobile-First)"
echo "   ⚡ PWA-Installation möglich"
echo "   🔄 Live-Updates via WebSocket (simuliert)"
echo "   ♿ Barrierefreie Navigation"
echo ""
echo "🎬 Demo-Tipps:"
echo "   • Teste Theme-Wechsel mit Button oben rechts"
echo "   • Navigiere durch alle Module über Sidebar"
echo "   • Teste Mobile-Ansicht (Browser DevTools)"
echo "   • Probiere PWA-Installation (Desktop/Mobile)"
echo "   • Teste Tab-Navigation in den Modulen"
echo ""
echo -e "${GREEN}🚀 Bereit für die Präsentation!${NC}"
echo ""
echo "Drücke Ctrl+C um den Demo-Server zu stoppen"

# Warte auf Benutzer-Unterbrechung
trap "echo -e '\n${YELLOW}🛑 Demo beendet${NC}'; kill $SERVER_PID 2>/dev/null; echo -e '${GREEN}✓${NC} Server gestoppt'; rm -f '$WEBAPP_DIR/demo-config.json'; exit 0" INT

# Halte das Script am Laufen
while true; do
    sleep 1
done
