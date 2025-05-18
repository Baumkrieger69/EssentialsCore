package com.essentialscore;

import com.essentialscore.threading.AdvancedWorkStealingPool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verwaltet alle Threads und Thread-Pools für EssentialsCore.
 * Bietet eine zentrale Schnittstelle für Thread-Operationen und Monitoring.
 */
public class ThreadManager {
    private final ApiCore core;
    private final Logger logger;
    private ExecutorService threadPool;
    private AdvancedWorkStealingPool advancedPool;
    private ScheduledExecutorService scheduledPool;
    private final String poolType;
    private final int poolSize;
    private final boolean monitoringEnabled;
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    private final AtomicInteger completedTaskCounter = new AtomicInteger(0);
    
    /**
     * Erstellt einen neuen ThreadManager
     * 
     * @param core Die ApiCore-Instanz
     */
    public ThreadManager(ApiCore core) {
        this.core = core;
        this.logger = core.getLogger();
        
        // Konfiguration laden
        poolType = core.getConfig().getString("performance.thread-pool-type", "CACHED").toUpperCase();
        poolSize = core.getConfig().getInt("performance.thread-pool-size", 
                Runtime.getRuntime().availableProcessors());
        monitoringEnabled = core.getConfig().getBoolean("performance.monitoring.enabled", true);
        
        // Thread-Pool initialisieren
        initializeThreadPools();
        
        logger.info("ThreadManager initialisiert mit " + poolType + " Pool (Größe: " + poolSize + ")");
    }
    
    /**
     * Initialisiert die Thread-Pools basierend auf der Konfiguration
     */
    private void initializeThreadPools() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "ApiCore-Worker-" + counter.getAndIncrement());
                thread.setDaemon(true);
                
                // Priorität setzen, falls konfiguriert
                if (core.getConfig().getBoolean("performance.high-priority-threads", false)) {
                    thread.setPriority(Thread.MAX_PRIORITY);
                }
                
                return thread;
            }
        };
        
        // Thread-Pool basierend auf Konfiguration erstellen
        switch (poolType) {
            case "FIXED":
                threadPool = Executors.newFixedThreadPool(poolSize, threadFactory);
                break;
            case "WORK_STEALING":
                threadPool = Executors.newWorkStealingPool(poolSize);
                break;
            case "SCHEDULED":
                ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(poolSize, threadFactory);
                scheduler.setRemoveOnCancelPolicy(true);
                scheduledPool = scheduler;
                threadPool = scheduler;
                break;
            case "ADVANCED":
                advancedPool = new AdvancedWorkStealingPool(poolSize, 
                        monitoringEnabled, logger);
                threadPool = advancedPool;
                break;
            case "CACHED":
            default:
                threadPool = Executors.newCachedThreadPool(threadFactory);
                break;
        }
    }
    
    /**
     * Übermittelt eine Aufgabe zur asynchronen Ausführung
     * 
     * @param task Die auszuführende Aufgabe
     * @return Ein Future-Objekt, das das Ergebnis repräsentiert
     */
    public Future<?> submit(Runnable task) {
        taskCounter.incrementAndGet();
        
        return threadPool.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Fehler bei Task-Ausführung", e);
            } finally {
                completedTaskCounter.incrementAndGet();
            }
        });
    }
    
    /**
     * Übermittelt eine Aufgabe mit Rückgabewert zur asynchronen Ausführung
     * 
     * @param task Die auszuführende Aufgabe
     * @return Ein Future-Objekt, das das Ergebnis repräsentiert
     */
    public <T> Future<T> submit(Callable<T> task) {
        taskCounter.incrementAndGet();
        
        return threadPool.submit(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Fehler bei Task-Ausführung", e);
                throw e;
            } finally {
                completedTaskCounter.incrementAndGet();
            }
        });
    }
    
    /**
     * Plant eine Aufgabe für regelmäßige Ausführung
     * 
     * @param task Die auszuführende Aufgabe
     * @param initialDelay Anfangsverzögerung in Millisekunden
     * @param period Periode in Millisekunden
     * @return Ein ScheduledFuture-Objekt, das die geplante Aufgabe repräsentiert
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period) {
        if (scheduledPool == null) {
            scheduledPool = Executors.newScheduledThreadPool(poolSize);
        }
        
        return scheduledPool.scheduleAtFixedRate(() -> {
            try {
                taskCounter.incrementAndGet();
                task.run();
                completedTaskCounter.incrementAndGet();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Fehler bei geplanter Task-Ausführung", e);
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Fährt den ThreadManager und alle verwalteten Threads herunter
     */
    public void shutdown() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                // Warte auf ordnungsgemäße Beendigung für max. 5 Sekunden
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (scheduledPool != null && !scheduledPool.isShutdown()) {
            scheduledPool.shutdown();
        }
        
        logger.info("ThreadManager heruntergefahren");
    }
    
    /**
     * Gibt Informationen über den Thread-Pool zurück
     * 
     * @return Thread-Pool-Informationen
     */
    public ThreadPoolInfo getThreadPoolInfo() {
        ThreadPoolInfo info = new ThreadPoolInfo();
        
        if (threadPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) threadPool;
            info.poolSize = executor.getPoolSize();
            info.corePoolSize = executor.getCorePoolSize();
            info.maxPoolSize = executor.getMaximumPoolSize();
            info.activeThreads = executor.getActiveCount();
            info.queueSize = executor.getQueue().size();
            info.completedTasks = executor.getCompletedTaskCount();
        } else if (advancedPool != null) {
            info.poolSize = advancedPool.getParallelism();
            info.corePoolSize = advancedPool.getParallelism();
            info.maxPoolSize = advancedPool.getParallelism();
            info.activeThreads = advancedPool.getActiveThreadCount();
            info.queueSize = advancedPool.getQueuedSubmissionCount();
            info.completedTasks = completedTaskCounter.get();
        } else {
            info.poolSize = poolSize;
            info.corePoolSize = poolSize;
            info.maxPoolSize = poolSize;
            info.activeThreads = -1; // Nicht verfügbar
            info.queueSize = -1;     // Nicht verfügbar
            info.completedTasks = completedTaskCounter.get();
        }
        
        return info;
    }
    
    /**
     * Gibt die Gesamtzahl der übermittelten Aufgaben zurück
     * 
     * @return Anzahl der übermittelten Aufgaben
     */
    public int getTotalTaskCount() {
        return taskCounter.get();
    }
    
    /**
     * Gibt die Anzahl der abgeschlossenen Aufgaben zurück
     * 
     * @return Anzahl der abgeschlossenen Aufgaben
     */
    public int getCompletedTaskCount() {
        return completedTaskCounter.get();
    }
    
    /**
     * Gibt den ExecutorService zurück
     * 
     * @return Der verwendete ExecutorService
     */
    public ExecutorService getExecutorService() {
        return threadPool;
    }
    
    /**
     * Klasse für Thread-Pool-Informationen
     */
    public static class ThreadPoolInfo {
        private int poolSize;
        private int corePoolSize;
        private int maxPoolSize;
        private int activeThreads;
        private long queueSize;
        private long completedTasks;
        
        /**
         * Gibt die aktuelle Pool-Größe zurück
         * 
         * @return Pool-Größe
         */
        public int getPoolSize() {
            return poolSize;
        }
        
        /**
         * Gibt die Kern-Pool-Größe zurück
         * 
         * @return Kern-Pool-Größe
         */
        public int getCorePoolSize() {
            return corePoolSize;
        }
        
        /**
         * Gibt die maximale Pool-Größe zurück
         * 
         * @return Maximale Pool-Größe
         */
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        /**
         * Gibt die Anzahl der aktiven Threads zurück
         * 
         * @return Anzahl der aktiven Threads
         */
        public int getActiveThreads() {
            return activeThreads;
        }
        
        /**
         * Gibt die Größe der Warteschlange zurück
         * 
         * @return Größe der Warteschlange
         */
        public long getQueueSize() {
            return queueSize;
        }
        
        /**
         * Gibt die Anzahl der abgeschlossenen Aufgaben zurück
         * 
         * @return Anzahl der abgeschlossenen Aufgaben
         */
        public long getCompletedTasks() {
            return completedTasks;
        }
    }
} 