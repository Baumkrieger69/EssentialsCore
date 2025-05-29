// Service Worker for EssentialsCore WebUI PWA - Advanced Version
const CACHE_NAME = 'essentialscore-webui-v2';
const API_CACHE_NAME = 'essentialscore-api-v1';
const STATIC_CACHE_NAME = 'essentialscore-static-v1';

// Cache-Strategien
const CACHE_STRATEGIES = {
    CACHE_FIRST: 'cache-first',
    NETWORK_FIRST: 'network-first',
    STALE_WHILE_REVALIDATE: 'stale-while-revalidate',
    NETWORK_ONLY: 'network-only',
    CACHE_ONLY: 'cache-only'
};

// URL-Patterns und ihre Cache-Strategien
const CACHE_PATTERNS = {
    // Statische Assets - Cache First
    '/css/': CACHE_STRATEGIES.CACHE_FIRST,
    '/js/': CACHE_STRATEGIES.CACHE_FIRST,
    '/images/': CACHE_STRATEGIES.CACHE_FIRST,
    '/fonts/': CACHE_STRATEGIES.CACHE_FIRST,
    
    // API-Endpoints - Network First
    '/api/': CACHE_STRATEGIES.NETWORK_FIRST,
    
    // HTML-Seiten - Stale While Revalidate
    '/': CACHE_STRATEGIES.STALE_WHILE_REVALIDATE,
    '/index.html': CACHE_STRATEGIES.STALE_WHILE_REVALIDATE,
    
    // WebSocket - Network Only
    '/ws/': CACHE_STRATEGIES.NETWORK_ONLY
};

const urlsToCache = [
    '/',
    '/index.html',
    '/manifest.json',
    '/css/main.css',
    '/css/RemoteManagement.css',
    '/css/charts.css',
    '/js/app.js',
    '/js/enhanced-features.js',
    '/js/security-settings.js',
    '/js/data-visualization.js',
    '/js/websocket-manager.js',
    'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css',
    'https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap',
    'https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.min.js'
];

// Offline-Fallback-Seite
const OFFLINE_PAGE = '/offline.html';

// Performance-Metriken
let performanceMetrics = {
    cacheHits: 0,
    cacheMisses: 0,
    networkRequests: 0,
    offlineRequests: 0
};

// Install Service Worker and cache resources
self.addEventListener('install', event => {
    console.log('Service Worker: Installing...');
    
    event.waitUntil(
        Promise.all([
            // Statische Ressourcen cachen
            caches.open(STATIC_CACHE_NAME).then(cache => {
                console.log('Service Worker: Caching static files');
                return cache.addAll(urlsToCache);
            }),
            
            // API-Cache vorbereiten
            caches.open(API_CACHE_NAME),
            
            // Offline-Seite erstellen
            createOfflinePage()
        ]).catch(error => {
            console.error('Service Worker: Installation failed:', error);
        })
    );
    
    // Sofort aktivieren
    self.skipWaiting();
});

// Activate Service Worker and clean old caches
self.addEventListener('activate', event => {
    console.log('Service Worker: Activating...');
    
    event.waitUntil(
        Promise.all([
            // Alte Caches löschen
            caches.keys().then(cacheNames => {
                return Promise.all(
                    cacheNames.map(cacheName => {
                        if (cacheName !== CACHE_NAME && 
                            cacheName !== API_CACHE_NAME && 
                            cacheName !== STATIC_CACHE_NAME) {
                            console.log('Service Worker: Deleting old cache:', cacheName);
                            return caches.delete(cacheName);
                        }
                    })
                );
            }),
            
            // Alle Clients übernehmen
            self.clients.claim()
        ])
    );
});

// Advanced Fetch Handler mit verschiedenen Cache-Strategien
self.addEventListener('fetch', event => {
    const url = new URL(event.request.url);
    const strategy = getCacheStrategy(url.pathname);
    
    event.respondWith(
        handleRequest(event.request, strategy)
    );
});

// Cache-Strategie bestimmen
function getCacheStrategy(pathname) {
    for (const [pattern, strategy] of Object.entries(CACHE_PATTERNS)) {
        if (pathname.startsWith(pattern)) {
            return strategy;
        }
    }
    return CACHE_STRATEGIES.STALE_WHILE_REVALIDATE; // Default
}

// Request-Handler basierend auf Strategie
async function handleRequest(request, strategy) {
    const url = new URL(request.url);
    
    try {
        switch (strategy) {
            case CACHE_STRATEGIES.CACHE_FIRST:
                return await cacheFirst(request);
                
            case CACHE_STRATEGIES.NETWORK_FIRST:
                return await networkFirst(request);
                
            case CACHE_STRATEGIES.STALE_WHILE_REVALIDATE:
                return await staleWhileRevalidate(request);
                
            case CACHE_STRATEGIES.NETWORK_ONLY:
                return await fetch(request);
                
            case CACHE_STRATEGIES.CACHE_ONLY:
                return await cacheOnly(request);
                
            default:
                return await staleWhileRevalidate(request);
        }
    } catch (error) {
        console.error('Service Worker: Request failed:', error);
        return await handleOffline(request);
    }
}

// Cache First Strategie
async function cacheFirst(request) {
    const cache = await caches.open(STATIC_CACHE_NAME);
    const cachedResponse = await cache.match(request);
    
    if (cachedResponse) {
        performanceMetrics.cacheHits++;
        return cachedResponse;
    }
    
    performanceMetrics.cacheMisses++;
    const networkResponse = await fetch(request);
    
    if (networkResponse.ok) {
        cache.put(request, networkResponse.clone());
    }
    
    return networkResponse;
}

// Network First Strategie
async function networkFirst(request) {
    const cache = await caches.open(API_CACHE_NAME);
    
    try {
        performanceMetrics.networkRequests++;
        const networkResponse = await fetch(request);
        
        if (networkResponse.ok) {
            // API-Responses mit Timestamp cachen
            const responseToCache = networkResponse.clone();
            const headers = new Headers(responseToCache.headers);
            headers.set('sw-cached-at', Date.now().toString());
            
            const cachedResponse = new Response(responseToCache.body, {
                status: responseToCache.status,
                statusText: responseToCache.statusText,
                headers: headers
            });
            
            cache.put(request, cachedResponse);
        }
        
        return networkResponse;
    } catch (error) {
        const cachedResponse = await cache.match(request);
        if (cachedResponse) {
            performanceMetrics.cacheHits++;
            return cachedResponse;
        }
        throw error;
    }
}

// Stale While Revalidate Strategie
async function staleWhileRevalidate(request) {
    const cache = await caches.open(STATIC_CACHE_NAME);
    const cachedResponse = await cache.match(request);
    
    // Fetch im Hintergrund
    const fetchPromise = fetch(request).then(networkResponse => {
        if (networkResponse.ok) {
            cache.put(request, networkResponse.clone());
        }
        return networkResponse;
    }).catch(() => {
        // Netzwerkfehler ignorieren
    });
    
    if (cachedResponse) {
        performanceMetrics.cacheHits++;
        return cachedResponse;
    }
    
    performanceMetrics.cacheMisses++;
    return await fetchPromise;
}

// Cache Only Strategie
async function cacheOnly(request) {
    const cache = await caches.open(STATIC_CACHE_NAME);
    const cachedResponse = await cache.match(request);
    
    if (cachedResponse) {
        performanceMetrics.cacheHits++;
        return cachedResponse;
    }
    
    throw new Error('Resource not found in cache');
}

// Offline-Handler
async function handleOffline(request) {
    performanceMetrics.offlineRequests++;
    
    const url = new URL(request.url);
    
    // HTML-Seiten -> Offline-Page
    if (request.destination === 'document') {
        const cache = await caches.open(STATIC_CACHE_NAME);
        return await cache.match(OFFLINE_PAGE) || new Response('Offline');
    }
    
    // API-Requests -> Cached Response oder Fehler
    if (url.pathname.startsWith('/api/')) {
        const cache = await caches.open(API_CACHE_NAME);
        const cachedResponse = await cache.match(request);
        
        if (cachedResponse) {
            return cachedResponse;
        }
        
        return new Response(JSON.stringify({
            error: 'Offline',
            message: 'No network connection available',
            cached: false
        }), {
            status: 503,
            headers: { 'Content-Type': 'application/json' }
        });
    }
    
    // Andere Ressourcen -> 404
    return new Response('Not Found', { status: 404 });
}

// Offline-Seite erstellen
async function createOfflinePage() {
    const offlineHTML = `
    <!DOCTYPE html>
    <html lang="de">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>EssentialsCore WebUI - Offline</title>
        <style>
            body { 
                font-family: Inter, sans-serif; 
                background: #1a1d23; 
                color: #fff; 
                display: flex; 
                align-items: center; 
                justify-content: center; 
                height: 100vh; 
                margin: 0; 
                text-align: center;
            }
            .offline-container {
                max-width: 400px;
                padding: 2rem;
            }
            .offline-icon {
                font-size: 4rem;
                margin-bottom: 1rem;
                color: #f59e0b;
            }
            .offline-title {
                font-size: 1.5rem;
                margin-bottom: 1rem;
                color: #f59e0b;
            }
            .offline-message {
                margin-bottom: 2rem;
                color: #9ca3af;
            }
            .retry-button {
                background: #3b82f6;
                color: white;
                border: none;
                padding: 0.75rem 1.5rem;
                border-radius: 0.5rem;
                cursor: pointer;
                font-size: 1rem;
            }
            .retry-button:hover {
                background: #2563eb;
            }
        </style>
    </head>
    <body>
        <div class="offline-container">
            <div class="offline-icon">📡</div>
            <h1 class="offline-title">Keine Verbindung</h1>
            <p class="offline-message">
                Die WebUI ist offline. Überprüfen Sie Ihre Internetverbindung 
                und versuchen Sie es erneut.
            </p>
            <button class="retry-button" onclick="window.location.reload()">
                Erneut versuchen
            </button>
        </div>
    </body>
    </html>
    `;
    
    const cache = await caches.open(STATIC_CACHE_NAME);
    const response = new Response(offlineHTML, {
        headers: { 'Content-Type': 'text/html' }
    });
    
    await cache.put(OFFLINE_PAGE, response);
}

// Handle background sync for offline actions
self.addEventListener('sync', event => {
    console.log('Service Worker: Background sync triggered:', event.tag);
    
    if (event.tag === 'background-sync-commands') {
        event.waitUntil(
            syncPendingCommands()
        );
    }
    
    if (event.tag === 'background-sync-analytics') {
        event.waitUntil(
            syncAnalyticsData()
        );
    }
});

// Handle push notifications
self.addEventListener('push', event => {
    console.log('Service Worker: Push message received');
    
    const options = {
        body: 'EssentialsCore Server Update',
        icon: '/icons/icon-192x192.png',
        badge: '/icons/badge-72x72.png',
        vibrate: [100, 50, 100],
        data: {
            dateOfArrival: Date.now(),
            primaryKey: 1
        },
        actions: [
            {
                action: 'explore',
                title: 'Zum Dashboard',
                icon: '/icons/checkmark.png'
            },
            {
                action: 'close',
                title: 'Schließen',
                icon: '/icons/xmark.png'
            }
        ]
    };
    
    if (event.data) {
        const data = event.data.json();
        options.body = data.message || options.body;
        options.data = { ...options.data, ...data };
    }
    
    event.waitUntil(
        self.registration.showNotification('EssentialsCore WebUI', options)
    );
});

// Notification Click Handler
self.addEventListener('notificationclick', event => {
    console.log('Service Worker: Notification click received');
    
    event.notification.close();
    
    if (event.action === 'explore') {
        event.waitUntil(
            clients.openWindow('/#/dashboard')
        );
    } else if (event.action === 'close') {
        // Notification already closed
    } else {
        // Default action - open app
        event.waitUntil(
            clients.matchAll({ type: 'window' }).then(clientList => {
                for (const client of clientList) {
                    if (client.url === '/' && 'focus' in client) {
                        return client.focus();
                    }
                }
                if (clients.openWindow) {
                    return clients.openWindow('/');
                }
            })
        );
    }
});

// Message Handler für Client-Kommunikation
self.addEventListener('message', event => {
    console.log('Service Worker: Message received:', event.data);
    
    if (event.data && event.data.type) {
        switch (event.data.type) {
            case 'SKIP_WAITING':
                self.skipWaiting();
                break;
                
            case 'GET_CACHE_STATUS':
                getCacheStatus().then(status => {
                    event.ports[0].postMessage(status);
                });
                break;
                
            case 'CLEAR_CACHE':
                clearCache().then(result => {
                    event.ports[0].postMessage(result);
                });
                break;
                
            case 'PERFORMANCE_METRICS':
                event.ports[0].postMessage(performanceMetrics);
                break;
                
            case 'QUEUE_COMMAND':
                queueOfflineCommand(event.data.command);
                break;
        }
    }
});

// Utility Functions

async function syncPendingCommands() {
    // Implementierung für das Synchronisieren von Offline-Commands
    console.log('Service Worker: Syncing pending commands...');
    
    try {
        const cache = await caches.open(API_CACHE_NAME);
        const offlineCommands = await cache.match('/offline-commands');
        
        if (offlineCommands) {
            const commands = await offlineCommands.json();
            
            for (const command of commands) {
                try {
                    await fetch('/api/command', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(command)
                    });
                } catch (error) {
                    console.error('Failed to sync command:', error);
                }
            }
            
            // Commands nach erfolgreichem Sync löschen
            await cache.delete('/offline-commands');
        }
    } catch (error) {
        console.error('Error syncing commands:', error);
    }
}

async function syncAnalyticsData() {
    // Analytics-Daten synchronisieren
    console.log('Service Worker: Syncing analytics data...');
    
    try {
        const data = {
            performanceMetrics: performanceMetrics,
            timestamp: Date.now(),
            userAgent: navigator.userAgent
        };
        
        await fetch('/api/analytics', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        // Metriken zurücksetzen
        performanceMetrics = {
            cacheHits: 0,
            cacheMisses: 0,
            networkRequests: 0,
            offlineRequests: 0
        };
    } catch (error) {
        console.error('Error syncing analytics:', error);
    }
}

async function getCacheStatus() {
    const cacheNames = await caches.keys();
    const status = {};
    
    for (const cacheName of cacheNames) {
        const cache = await caches.open(cacheName);
        const keys = await cache.keys();
        status[cacheName] = {
            itemCount: keys.length,
            items: keys.map(request => request.url)
        };
    }
    
    return status;
}

async function clearCache() {
    try {
        const cacheNames = await caches.keys();
        await Promise.all(
            cacheNames.map(cacheName => caches.delete(cacheName))
        );
        return { success: true, message: 'All caches cleared' };
    } catch (error) {
        return { success: false, error: error.message };
    }
}

async function queueOfflineCommand(command) {
    try {
        const cache = await caches.open(API_CACHE_NAME);
        const existing = await cache.match('/offline-commands');
        
        let commands = [];
        if (existing) {
            commands = await existing.json();
        }
        
        commands.push({
            ...command,
            timestamp: Date.now(),
            id: Math.random().toString(36).substr(2, 9)
        });
        
        const response = new Response(JSON.stringify(commands), {
            headers: { 'Content-Type': 'application/json' }
        });
        
        await cache.put('/offline-commands', response);
        
        // Background Sync registrieren
        if (self.registration.sync) {
            await self.registration.sync.register('background-sync-commands');
        }
    } catch (error) {
        console.error('Error queueing offline command:', error);
    }
}

// Periodische Cache-Bereinigung
setInterval(() => {
    cleanExpiredCache();
}, 60000 * 60); // Jede Stunde

async function cleanExpiredCache() {
    try {
        const cache = await caches.open(API_CACHE_NAME);
        const requests = await cache.keys();
        const now = Date.now();
        const maxAge = 60000 * 60 * 24; // 24 Stunden
        
        for (const request of requests) {
            const response = await cache.match(request);
            const cachedAt = response.headers.get('sw-cached-at');
            
            if (cachedAt && (now - parseInt(cachedAt)) > maxAge) {
                await cache.delete(request);
                console.log('Service Worker: Expired cache entry removed:', request.url);
            }
        }
    } catch (error) {
        console.error('Error cleaning expired cache:', error);
    }
}

console.log('Service Worker: Initialized');
