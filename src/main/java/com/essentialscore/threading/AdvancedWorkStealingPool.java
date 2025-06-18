package com.essentialscore.threading;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ein erweiterter Work-Stealing-Thread-Pool mit priorisierter Task-Ausführung,
 * vorgeheizten Threads und dynamischer Ressourcenanpassung.
 */
public class AdvancedWorkStealingPool extends ForkJoinPool {
    private final Logger logger;
    private final ScheduledExecutorService healthMonitor;
    private final ConcurrentHashMap<Class<?>, TaskStatistics> taskStats = new ConcurrentHashMap<>();
    private final AtomicInteger overloadCount = new AtomicInteger(0);
    private volatile boolean isShuttingDown = false;
    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    private final AtomicInteger totalTasksSubmitted = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Long> taskExecutionTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> taskTypeCounter = new ConcurrentHashMap<>();
    private final boolean monitoringEnabled;
    
    /**
     * Erstellt einen neuen optimierten Work-Stealing-Thread-Pool.
     * 
     * @param parallelism Anzahl der parallelen Threads
     */
    public AdvancedWorkStealingPool(int parallelism) {
        this(parallelism, true, Logger.getLogger(AdvancedWorkStealingPool.class.getName()));
    }
    
    /**
     * Erstellt einen neuen optimierten Work-Stealing-Thread-Pool.
     * 
     * @param parallelism Anzahl der parallelen Threads
     * @param monitoringEnabled Ob das Monitoring aktiviert werden soll
     * @param logger Logger für Diagnose
     */
    public AdvancedWorkStealingPool(
            int parallelism,
            boolean monitoringEnabled,
            Logger logger) {
        super(parallelism, new MinecraftFriendlyForkJoinWorkerThreadFactory(), null, false);
        this.logger = logger;
        this.healthMonitor = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "WorkStealingPool-HealthMonitor");
                    t.setDaemon(true);
                    t.setPriority(Thread.MAX_PRIORITY - 1);
                    return t;
                }
        );
          // Eigener UncaughtExceptionHandler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            logger.log(Level.SEVERE, "Uncaught exception in AdvancedWorkStealingPool: " + e.getMessage(), e);
        });
        
        // Vorwärmen des Thread-Pools
        warmUp();
        
        // Gesundheitsüberwachung starten
        startHealthMonitoring();
        
        this.monitoringEnabled = monitoringEnabled;
    }

    /**
     * Vorwärmen des Thread-Pools durch Ausführung von Dummy-Tasks.
     */
    private void warmUp() {
        int warmupTasks = getParallelism();
        CountDownLatch latch = new CountDownLatch(warmupTasks);
        
        for (int i = 0; i < warmupTasks; i++) {
            execute(() -> {
                try {
                    // JIT-Warming für Threadpool-Kernfunktionen
                    Thread.sleep(1);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            // Warten bis alle Warmup-Tasks abgeschlossen sind
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Startet die Überwachung des Thread-Pool-Zustands.
     */
    private void startHealthMonitoring() {
        healthMonitor.scheduleAtFixedRate(() -> {
            if (isShuttingDown) return;
            
            try {
                double saturation = 1.0 * getActiveThreadCount() / getParallelism();
                long queuedSubmissions = getQueuedSubmissionCount();
                long stealCount = getStealCount();
                
                // Auto-Tuning bei anhaltender Überlastung
                if (saturation > 0.9 && queuedSubmissions > 50) {
                    if (overloadCount.incrementAndGet() > 5) {
                        increaseParallelism();
                        overloadCount.set(0);
                    }
                } else {
                    overloadCount.set(0);
                }
                
                // Logging für Performance-Monitoring
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format(
                            "WorkStealingPool Status: Parallelism=%d, Active=%d, Queued=%d, Steals=%d, Saturation=%.2f",
                            getParallelism(), getActiveThreadCount(), queuedSubmissions, stealCount, saturation));
                }
                
                // Ausgabe der Task-Statistiken
                if (logger.isLoggable(Level.FINER)) {
                    taskStats.forEach((cls, stats) -> {
                        logger.finer(String.format("Task [%s]: Count=%d, AvgTime=%.2fms, MaxTime=%.2fms",
                                cls.getSimpleName(), stats.getCount(), stats.getAverageTimeMs(), stats.getMaxTimeMs()));
                    });
                }
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "Fehler bei Thread-Pool-Überwachung", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Erhöht die Parallelität dynamisch basierend auf der Systemlast.
     */
    private synchronized void increaseParallelism() {
        int current = getParallelism();
        int coreCount = Runtime.getRuntime().availableProcessors();
        
        // Nicht mehr als Prozessorkerne + 2 zulassen
        if (current < coreCount + 2) {
            logger.info("Erhöhe Thread-Pool-Parallelität von " + current + " auf " + (current + 1));
            setParallelism(current + 1);
        }
    }
    
    /**
     * Übermittelt eine Aufgabe zum Ausführen
     * 
     * @param task Die auszuführende Aufgabe
     * @return Ein Future-Objekt für das Ergebnis
     */
    @Override
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        trackTaskSubmission(task);
        return super.submit(task);
    }
    
    /**
     * Übermittelt eine Runnable-Aufgabe zum Ausführen
     * 
     * @param task Die auszuführende Runnable-Aufgabe
     * @return Ein Future-Objekt
     */
    @Override
    public ForkJoinTask<?> submit(Runnable task) {
        trackTaskSubmission(task);
        return super.submit(task);
    }
    
    /**
     * Übermittelt eine Aufgabe zum Ausführen mit Rückgabewert
     * 
     * @param task Die auszuführende Aufgabe
     * @param result Das Ergebnis, das zurückgegeben werden soll
     * @return Ein Future-Objekt für das Ergebnis
     */
    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        trackTaskSubmission(task);
        return super.submit(task, result);
    }
    
    /**
     * Übermittelt eine Callable-Aufgabe zum Ausführen
     * 
     * @param task Die auszuführende Callable-Aufgabe
     * @return Ein Future-Objekt für das Ergebnis
     */
    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        trackTaskSubmission(task);
        return super.submit(task);
    }
    
    /**
     * Verfolgt die Übermittlung einer Aufgabe
     * 
     * @param task Die übermittelte Aufgabe
     */
    private void trackTaskSubmission(Object task) {
        activeTaskCount.incrementAndGet();
        totalTasksSubmitted.incrementAndGet();
        
        if (monitoringEnabled) {
            String taskType = task.getClass().getName();
            taskTypeCounter.computeIfAbsent(taskType, k -> new AtomicInteger(0))
                           .incrementAndGet();
            
            // Erstelle oder hole TaskStatistics für diesen Task-Typ
            taskStats.computeIfAbsent(task.getClass(), k -> new TaskStatistics());
        }
    }
    
    /**
     * Gibt die aktuelle Anzahl aktiver Aufgaben zurück
     * 
     * @return Anzahl aktiver Aufgaben
     */
    public int getActiveTaskCount() {
        return activeTaskCount.get();
    }
    
    /**
     * Gibt die Gesamtzahl der übermittelten Aufgaben zurück
     * 
     * @return Gesamtzahl der Aufgaben
     */
    public int getTotalTasksSubmitted() {
        return totalTasksSubmitted.get();
    }
    
    /**
     * Gibt die durchschnittliche Ausführungszeit für einen bestimmten Aufgabentyp zurück
     * 
     * @param taskType Der Aufgabentyp
     * @return Durchschnittliche Ausführungszeit in ms oder -1, wenn keine Daten verfügbar
     */
    public double getAverageExecutionTime(String taskType) {
        if (!monitoringEnabled) {
            return -1;
        }
        
        Long totalTime = taskExecutionTimes.get(taskType);
        AtomicInteger count = taskTypeCounter.get(taskType);
        
        if (totalTime == null || count == null || count.get() == 0) {
            return -1;
        }
        
        return totalTime / (double) count.get();
    }
    
    /**
     * Übermittelt eine Sammlung von Aufgaben zur Ausführung
     * 
     * @param tasks Die Aufgaben
     * @return Eine Liste von Future-Objekten für die Ergebnisse
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        if (tasks == null) {
            throw new NullPointerException();
        }
        
        List<ForkJoinTask<T>> forkJoinTasks = tasks.stream()
            .map(this::submit)
            .collect(java.util.stream.Collectors.toList());
        
        // Auf Fertigstellung aller Aufgaben warten
        for (ForkJoinTask<T> task : forkJoinTasks) {
            task.join();
        }
        
        return new java.util.ArrayList<>(forkJoinTasks);
    }
    
    /**
     * Übermittelt eine Sammlung von Aufgaben zur Ausführung mit Timeout
     * 
     * @param tasks Die Aufgaben
     * @param timeout Timeout-Wert
     * @param unit Timeout-Einheit
     * @return Eine Liste von Future-Objekten für die Ergebnisse
     * @throws InterruptedException Bei Unterbrechung
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, 
                                      long timeout, TimeUnit unit) throws InterruptedException {
        if (tasks == null || unit == null) {
            throw new NullPointerException();
        }
        
        long nanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + nanos;
        
        List<ForkJoinTask<T>> forkJoinTasks = tasks.stream()
            .map(this::submit)
            .collect(java.util.stream.Collectors.toList());
        
        for (ForkJoinTask<T> task : forkJoinTasks) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                break;
            }
            
            try {
                task.get(remaining, TimeUnit.NANOSECONDS);
            } catch (ExecutionException | TimeoutException e) {
                // Ignorieren und fortfahren
            }
        }
        
        return new java.util.ArrayList<>(forkJoinTasks);
    }
    
    /**
     * Übermittelt eine Sammlung von Aufgaben und gibt das Ergebnis der ersten fertiggestellten zurück
     * 
     * @param tasks Die Aufgaben
     * @return Das Ergebnis der ersten fertiggestellten Aufgabe
     * @throws InterruptedException Bei Unterbrechung
     * @throws ExecutionException Bei Ausführungsfehlern
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) 
            throws InterruptedException, ExecutionException {
        try {
            return invokeAny(tasks, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            // Sollte nicht auftreten, da Timeout auf unendlich gesetzt ist
            throw new InternalError(e);
        }
    }
    
    /**
     * Übermittelt eine Sammlung von Aufgaben und gibt das Ergebnis der ersten fertiggestellten mit Timeout zurück
     * 
     * @param tasks Die Aufgaben
     * @param timeout Timeout-Wert
     * @param unit Timeout-Einheit
     * @return Das Ergebnis der ersten fertiggestellten Aufgabe
     * @throws InterruptedException Bei Unterbrechung
     * @throws ExecutionException Bei Ausführungsfehlern
     * @throws TimeoutException Bei Timeout
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, 
                         long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null || unit == null) {
            throw new NullPointerException();
        }
        
        long nanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + nanos;
        // Erfolgreich ausgeführte Aufgabe
        final AtomicInteger completedCount = new AtomicInteger(0);
        
        // Ergebnis und Fehler
        final AtomicReference<T> result = new AtomicReference<>();
        final ExecutionException[] ee = new ExecutionException[1];
        
        // CountDownLatch für Synchronisation
        final CountDownLatch latch = new CountDownLatch(1);
        
        // Übermittle alle Aufgaben
        for (Callable<T> task : tasks) {
            submit(() -> {
                try {
                    T taskResult = task.call();
                    if (completedCount.getAndIncrement() == 0) {
                        result.set(taskResult);
                        latch.countDown();
                    }
                } catch (Exception e) {
                    if (completedCount.get() == 0) {
                        ee[0] = new ExecutionException(e);
                    }
                }
                return null;
            });
        }
        
        // Warte auf Fertigstellung oder Timeout
        long remaining = deadline - System.nanoTime();
        boolean timedOut = remaining <= 0 || !latch.await(remaining, TimeUnit.NANOSECONDS);
        
        if (timedOut && completedCount.get() == 0) {
            throw new TimeoutException();
        }
        if (result.get() != null) {
            return result.get();
        }
        
        if (ee[0] != null) {
            throw ee[0];
        }
        
        throw new ExecutionException("No task completed successfully", null);
    }
        
    /**
     * Verarbeitet beendete Aufgaben
     * 
     * @param task Die beendete Aufgabe
     * @param exception Die mögliche Ausnahme
     */
    public void handleCompletedTask(Runnable task, Throwable exception) {
        activeTaskCount.decrementAndGet();
        if (exception != null) {
            logger.log(Level.WARNING, "Fehler bei Task-Ausführung", exception);
        }
    }
    
    /**
     * Verfolgt die Beendigung einer Aufgabe mit Ausführungszeit
     * 
     * @param task Die beendete Aufgabe
     * @param executionTimeMs Die Ausführungszeit in Millisekunden
     */
    public void trackTaskCompletion(Object task, double executionTimeMs) {
        activeTaskCount.decrementAndGet();
        
        if (monitoringEnabled) {
            TaskStatistics stats = taskStats.get(task.getClass());
            if (stats != null) {
                stats.recordExecution(executionTimeMs);
            }
        }
    }
    
    /**
     * Factory für Worker-Threads, die für Minecraft-Server optimiert ist
     */
    static class MinecraftFriendlyForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private final AtomicInteger threadCount = new AtomicInteger(0);
        
        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName("MC-Async-Worker-" + threadCount.incrementAndGet());
            
            // Niedrigere Thread-Priorität für bessere Kompatibilität mit Minecraft-Hauptthread
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            
            // Setze Thread als Daemon, damit er den Shutdown nicht blockiert
            thread.setDaemon(true);
            
            return thread;
        }
    }
    
    /**
     * Innere Klasse für Task-Statistiken.
     */
    static class TaskStatistics {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile double totalTimeMs = 0;
        private volatile double maxTimeMs = 0;
        
        public void recordExecution(double executionTimeMs) {
            count.incrementAndGet();
            totalTimeMs += executionTimeMs;
            
            synchronized (this) {
                if (executionTimeMs > maxTimeMs) {
                    maxTimeMs = executionTimeMs;
                }
            }
        }
        
        public int getCount() {
            return count.get();
        }
        
        public double getAverageTimeMs() {
            int currentCount = count.get();
            return currentCount > 0 ? totalTimeMs / currentCount : 0;
        }
        
        public double getMaxTimeMs() {
            return maxTimeMs;
        }
    }
    
    /**
     * Task-Prioritäten für priorisierte Ausführung.
     */
    public enum TaskPriority {
        LOW(0),
        NORMAL(1),
        HIGH(2),
        CRITICAL(3);
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
      /**
     * Ein Task mit Priorität für optimierte Ausführungsreihenfolge.
     * 
     * @param <V> the return type of the task
     */
    public static class PrioritizedTask<V> extends RecursiveTask<V> {
        private final Runnable action;
        private final Callable<V> callable;
        private volatile TaskPriority priority = TaskPriority.NORMAL;
        
        public PrioritizedTask(Runnable action, TaskPriority priority) {
            this.action = action;
            this.callable = null;
            this.priority = priority;
        }
        
        public PrioritizedTask(Callable<V> callable, TaskPriority priority) {
            this.action = null;
            this.callable = callable;
            this.priority = priority;
        }
        
        public void setPriority(TaskPriority priority) {
            this.priority = priority;
        }
        
        public TaskPriority getPriority() {
            return priority;
        }
        
        @Override
        protected V compute() {
            // Priorität des aktuellen Threads anpassen
            Thread currentThread = Thread.currentThread();
            int originalPriority = currentThread.getPriority();
            
            try {
                // Thread-Priorität je nach Task-Priorität anpassen
                switch (priority) {
                    case LOW:
                        currentThread.setPriority(Thread.MIN_PRIORITY);
                        break;
                    case HIGH:
                        currentThread.setPriority(Thread.NORM_PRIORITY + 1);
                        break;
                    case CRITICAL:
                        currentThread.setPriority(Thread.MAX_PRIORITY);
                        break;
                    default:
                        // NORMAL - Standardpriorität beibehalten
                }
                
                // Task ausführen
                if (action != null) {
                    action.run();
                    return null;
                } else if (callable != null) {
                    return callable.call();
                }
                
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // Thread-Priorität zurücksetzen
                currentThread.setPriority(originalPriority);
            }
        }
    }
} 
