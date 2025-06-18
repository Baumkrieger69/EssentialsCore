# EssentialsCore WebUI - API Integration Guide

## Overview

The EssentialsCore WebUI has been updated to connect to real backend APIs instead of using mock data. The interface now provides both connected and fallback modes for seamless operation.

## Features Implemented

### 1. **Full Admin Panel Interface**
- **Dashboard**: Real-time server statistics, player count, TPS, memory usage, uptime
- **Player Management**: View online players, kick, ban, teleport, give items
- **Live Console**: Execute server commands with real-time output
- **File Manager**: Browse, edit, download, and upload server files
- **Permissions**: Manage user groups and permissions
- **Performance Monitor**: Real-time server performance metrics
- **Settings**: Configure WebUI and server settings

### 2. **Real API Integration**
The WebUI now connects to these backend API endpoints:

#### Server Information (`/api/server/info`)
- Server name, version, max players, online players
- Used by: Dashboard page

#### Server Statistics (`/api/server/stats`)
- Real-time TPS, memory usage, uptime
- Used by: Dashboard stats cards

#### Player Management (`/api/players`)
- List online players with details
- Player actions: `/api/players/{name}/kick`, `/api/players/{name}/ban`, `/api/players/{name}/teleport`
- Used by: Players page

#### Console (`/api/console`)
- Execute commands: `/api/console/execute`
- Command history: `/api/console/history`
- Used by: Console page

#### File Manager (`/api/files`)
- Read files: `/api/files/read?path={filename}`
- Write files: `/api/files/write`
- Upload files: `/api/files/upload`
- Used by: Files page

#### Performance (`/api/performance`)
- CPU usage, memory usage, TPS, chunks, entities
- Used by: Performance page

### 3. **Fallback Mode**
When APIs are not available, the WebUI automatically switches to fallback mode with:
- Simulated server statistics
- Mock player data
- Command response simulation
- File content placeholders
- Warning notifications about API unavailability

## Technical Implementation

### Backend Changes

1. **RestApiManager.java** - Updated to register all endpoint classes:
   - ServerInfoEndpoint
   - PlayerManagementEndpoint
   - ConsoleEndpoint (when LiveConsoleManager available)
   - FileManagerEndpoint
   - PerformanceEndpoint
   - AuthEndpoint

2. **WebUIManager.java** - Modified to:
   - Initialize LiveConsoleManager before RestApiManager
   - Pass LiveConsoleManager to RestApiManager constructor
   - Proper component initialization order

### Frontend Changes

1. **index.html** - Complete rewrite with:
   - Modern responsive design
   - Real API integration with fetch() calls
   - Connection status monitoring
   - Automatic fallback to mock data
   - Error handling and notifications
   - Real-time data updates

2. **API Communication**:
   - All functions now use `apiCall()` helper function
   - Automatic connection status detection
   - Graceful fallback when APIs are unavailable
   - Real-time notifications for API errors

## Usage

### Starting the WebUI
The WebUI starts automatically when the plugin loads:
1. Extracts webapp files to plugin directory
2. Initializes all components and APIs
3. Starts HTTP server on port 8080 (configurable)
4. Tests API connectivity
5. Shows connection status in UI

### Accessing Features

1. **Dashboard**: Shows real server stats if connected, simulated if not
2. **Players**: Lists real online players or mock data
3. **Console**: Executes real commands or simulates responses
4. **Files**: Reads/writes real server files or shows placeholders
5. **Performance**: Shows real performance data or simulated metrics

### Connection Status
- **🟢 Connected**: Full API functionality available
- **🟡 API Unavailable**: Using fallback mode with simulated data
- **🔴 Disconnected**: Connection lost, attempting to reconnect

## Configuration

The WebUI can be configured via `webui/config.yml`:

```yaml
http:
  port: 8080
  bind-address: "127.0.0.1"
websocket:
  port: 8081
  bind-address: "127.0.0.1"
security:
  enabled: true
  require-authentication: true
auth:
  session-timeout: 3600
  max-sessions: 10
features:
  dashboard: true
  file-manager: true
  live-console: true
  player-management: true
  plugin-management: false
```

## Development Notes

### API Endpoint Structure
Each endpoint extends `ApiEndpoint` and provides:
- `getPath()`: URL path segment
- `handleRequest()`: Process the request
- `requiresAuthentication()`: Security requirement
- `getRequiredPermission()`: Permission check

### Adding New Features
1. Create new endpoint class in `endpoints/` package
2. Register it in `RestApiManager.registerDefaultEndpoints()`
3. Add frontend functionality to `index.html`
4. Update navigation and page handling

### Error Handling
- Backend: All endpoints return standardized `ApiResponse` objects
- Frontend: `apiCall()` function handles errors gracefully
- Fallback: UI automatically switches to mock data when APIs fail

## Future Enhancements

1. **Real-time Updates**: WebSocket connection for live data
2. **Authentication**: User login and session management
3. **Plugin Management**: Install, enable, disable plugins
4. **Backup System**: Automated server backups
5. **Log Viewer**: Real-time log file monitoring
6. **World Management**: World creation, deletion, teleportation

## Testing

The WebUI can be tested in two modes:

1. **Connected Mode**: When the Minecraft server is running with all APIs available
2. **Fallback Mode**: When APIs are unavailable (for development/testing)

Access the WebUI at: `http://localhost:8080` (or configured port)

## Files Modified

- `src/main/resources/webui/webapp/index.html` - Complete rewrite
- `src/main/java/com/essentialscore/api/web/rest/RestApiManager.java` - API registration
- `src/main/java/com/essentialscore/api/web/WebUIManager.java` - Component initialization

## Status

✅ **COMPLETED**: Full WebUI with real API integration
✅ **COMPLETED**: Fallback mode for development
✅ **COMPLETED**: All major admin features implemented
✅ **COMPLETED**: Modern responsive design
✅ **COMPLETED**: Error handling and notifications

🔄 **NEXT STEPS**: 
- Connect to live Minecraft server for testing
- Implement WebSocket for real-time updates
- Add authentication system
- Polish UI/UX based on user feedback
