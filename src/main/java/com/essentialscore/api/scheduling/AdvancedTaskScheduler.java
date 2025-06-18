package com.essentialscore.api.scheduling;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.time.Instant;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.Plugin;

/**
 * Erweitertes Task-Scheduling-System mit Prioritäten und Abhängigkeiten.
 */
public class AdvancedTaskScheduler {
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledTask> activeTasks;
    private final Map<String, Set<String>> taskDependencies;
    private final PriorityBlockingQueue<PrioritizedTask> taskQueue;
    private final Map<String, TaskMetadata> taskMetadata;
    private final CircuitBreaker circuitBreaker;
    private final Set<TaskExecutionListener> listeners;
    
    public AdvancedTaskScheduler(Plugin plugin, int threadPoolSize) {
        this.activeTasks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(threadPoolSize);
        this.taskDependencies = new ConcurrentHashMap<>();
        this.taskQueue = new PriorityBlockingQueue<>();
        this.taskMetadata = new ConcurrentHashMap<>();
        this.circuitBreaker = new CircuitBreaker();
        this.listeners = ConcurrentHashMap.newKeySet();
        
        // Starte Task-Processor
        startTaskProcessor();
    }
    
    /**
     * Plant einen Task mit Abhängigkeiten.
     * 
     * @param <T> the type of the task result
     * @param taskId the unique identifier for the task
     * @param task the task to execute
     * @param priority the priority of the task
     * @param dependencies the set of task dependencies
     * @return a future representing the task result
     */
    public <T> Future<T> scheduleTask(
            String taskId,
            Supplier<T> task,
            TaskPriority priority,
            Set<String> dependencies) {
        
        CompletableFuture<T> future = new CompletableFuture<>();
        
        PrioritizedTask prioritizedTask = new PrioritizedTask(
            taskId,
            () -> {
                try {
                    if (canExecuteTask(taskId)) {
                        T result = executeWithRetry(task);
                        future.complete(result);
                        return result;
                    } else {
                        throw new TaskExecutionException("Abhängigkeiten nicht erfüllt");
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    throw new RuntimeException(e);
                }
            },
            priority
        );
        
        if (dependencies != null && !dependencies.isEmpty()) {
            taskDependencies.put(taskId, new HashSet<>(dependencies));
        }
        
        taskQueue.offer(prioritizedTask);
        taskMetadata.put(taskId, new TaskMetadata());
        
        return future;
    }
    
    /**
     * Plant einen wiederkehrenden Task mit Cron-Expression.
     * 
     * @param taskId the unique identifier for the task
     * @param task the task to execute
     * @param cronExpression the cron expression for scheduling
     * @param priority the priority of the task
     * @return the task ID
     */
    public String scheduleCronTask(
            String taskId,
            Runnable task,
            String cronExpression,
            TaskPriority priority) {
        
        CronSchedule schedule = new CronSchedule(cronExpression);
        ScheduledTask scheduledTask = ScheduledTask.builder()
            .id(UUID.fromString(taskId))
            .name("CronTask_" + taskId)
            .runnable(task)
            .priority(convertPriority(priority))
            .cronExpression(cronExpression)
            .build();
        
        activeTasks.put(taskId, scheduledTask);
        schedule.scheduleNext(scheduledTask, scheduler);
        
        return taskId;
    }
    
    /**
     * Führt einen Task mit Retry-Mechanismus aus.
     */
    private <T> T executeWithRetry(Supplier<T> task) throws Exception {
        int maxRetries = 3;
        long backoffMs = 1000; // 1 Sekunde
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                if (circuitBreaker.isOpen()) {
                    throw new CircuitBreakerException("Circuit Breaker ist offen");
                }
                
                T result = task.get();
                circuitBreaker.recordSuccess();
                return result;
                
            } catch (Exception e) {
                lastException = e;
                circuitBreaker.recordFailure();
                
                if (attempt < maxRetries - 1) {
                    Thread.sleep(backoffMs * (long) Math.pow(2, attempt));
                }
            }
        }
        
        throw new RetryExhaustedException("Maximale Anzahl von Versuchen erreicht", lastException);
    }
    
    private void startTaskProcessor() {
        Thread processor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PrioritizedTask task = taskQueue.take();
                    processTask(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        processor.setName("Task-Processor");
        processor.start();
    }
    
    private void processTask(PrioritizedTask task) {
        if (canExecuteTask(task.getId())) {
            TaskMetadata metadata = taskMetadata.get(task.getId());
            metadata.markStarted();
            
            notifyListeners(TaskEvent.STARTED, task.getId());
            
            try {
                Object result = task.getTask().get();
                metadata.markCompleted(result);
                notifyListeners(TaskEvent.COMPLETED, task.getId());
                
            } catch (Exception e) {
                metadata.markFailed(e);
                notifyListeners(TaskEvent.FAILED, task.getId());
                
                if (shouldRetryTask(task.getId())) {
                    requeueTask(task);
                }
            }
        } else {
            // Task kann noch nicht ausgeführt werden, wieder in Queue
            taskQueue.offer(task);
        }
    }
    
    private boolean canExecuteTask(String taskId) {
        Set<String> dependencies = taskDependencies.get(taskId);
        if (dependencies == null || dependencies.isEmpty()) {
            return true;
        }
        
        return dependencies.stream()
            .map(taskMetadata::get)
            .filter(metadata -> metadata != null)
            .allMatch(TaskMetadata::isCompleted);
    }
    
    private boolean shouldRetryTask(String taskId) {
        TaskMetadata metadata = taskMetadata.get(taskId);
        return metadata.getFailureCount() < 3; // Max 3 Versuche
    }
    
    private void requeueTask(PrioritizedTask task) {
        // Exponential Backoff
        long delay = (long) Math.pow(2, taskMetadata.get(task.getId()).getFailureCount());
        scheduler.schedule(
            () -> taskQueue.offer(task),
            delay,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Task-Prioritäten.
     */
    public enum TaskPriority {
        HIGH(0),
        MEDIUM(5),
        LOW(10);
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Task-Events für Listener.
     */
    public enum TaskEvent {
        SCHEDULED,
        STARTED,
        COMPLETED,
        FAILED
    }
    
    /**
     * Interface für Task-Execution-Listener.
     */
    public interface TaskExecutionListener {
        void onTaskEvent(TaskEvent event, String taskId);
    }
    
    /**
     * Repräsentiert einen priorisierten Task.
     */
    private static class PrioritizedTask implements Comparable<PrioritizedTask> {
        private final String id;
        private final Supplier<?> task;
        private final TaskPriority priority;
        private final Instant createdAt;
        
        public PrioritizedTask(String id, Supplier<?> task, TaskPriority priority) {
            this.id = id;
            this.task = task;
            this.priority = priority;
            this.createdAt = Instant.now();
        }
        
        @Override
        public int compareTo(PrioritizedTask other) {
            int priorityCompare = Integer.compare(
                this.priority.getValue(),
                other.priority.getValue()
            );
            
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            
            return this.createdAt.compareTo(other.createdAt);
        }
        
        public String getId() { return id; }
        public Supplier<?> getTask() { return task; }
    }
    
    /**
     * Speichert Metadaten für Tasks.
     */
    private static class TaskMetadata {
        private int failureCount;
        private TaskStatus status;
        
        public TaskMetadata() {
            this.status = TaskStatus.SCHEDULED;
            this.failureCount = 0;
        }
        
        
        public void markStarted() {
            this.status = TaskStatus.RUNNING;
        }
        
        public void markCompleted(Object result) {
            this.status = TaskStatus.COMPLETED;
        }
        
        public void markFailed(Exception error) {
            this.failureCount++;
            this.status = TaskStatus.FAILED;
        }
        
        public boolean isCompleted() {
            return status == TaskStatus.COMPLETED;
        }
        
        public int getFailureCount() {
            return failureCount;
        }
    }
    
    /**
     * Status eines Tasks.
     */
    private enum TaskStatus {
        SCHEDULED,
        RUNNING,
        COMPLETED,
        FAILED
    }
    
    /**
     * Circuit Breaker für Fehlerbehandlung.
     */
    private static class CircuitBreaker {
        private static final int FAILURE_THRESHOLD = 5;
        private static final Duration RESET_TIMEOUT = Duration.ofMinutes(1);
        
        private volatile CircuitState state;
        private final AtomicInteger failureCount;
        private volatile Instant lastStateChange;
        
        public CircuitBreaker() {
            this.state = CircuitState.CLOSED;
            this.failureCount = new AtomicInteger(0);
            this.lastStateChange = Instant.now();
        }
        
        public boolean isOpen() {
            if (state == CircuitState.HALF_OPEN) {
                return false;
            }
            
            if (state == CircuitState.OPEN &&
                java.time.Duration.between(lastStateChange, Instant.now())
                    .compareTo(RESET_TIMEOUT) > 0) {
                setState(CircuitState.HALF_OPEN);
                return false;
            }
            
            return state == CircuitState.OPEN;
        }
        
        public void recordSuccess() {
            failureCount.set(0);
            if (state == CircuitState.HALF_OPEN) {
                setState(CircuitState.CLOSED);
            }
        }
        
        public void recordFailure() {
            if (state == CircuitState.CLOSED &&
                failureCount.incrementAndGet() >= FAILURE_THRESHOLD) {
                setState(CircuitState.OPEN);
            }
        }
        
        private void setState(CircuitState newState) {
            this.state = newState;
            this.lastStateChange = Instant.now();
        }
        
        private enum CircuitState {
            CLOSED,
            OPEN,
            HALF_OPEN
        }
    }
    
    public void addListener(TaskExecutionListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(TaskExecutionListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners(TaskEvent event, String taskId) {
        for (TaskExecutionListener listener : listeners) {
            try {
                listener.onTaskEvent(event, taskId);
            } catch (Exception e) {
                // Log Listener-Fehler
            }
        }
    }
    
    private com.essentialscore.api.scheduling.TaskPriority convertPriority(TaskPriority priority) {
        switch (priority) {
            case HIGH:
                return com.essentialscore.api.scheduling.TaskPriority.HIGH;
            case MEDIUM:
                return com.essentialscore.api.scheduling.TaskPriority.LOW; // Use LOW as fallback
            case LOW:
                return com.essentialscore.api.scheduling.TaskPriority.LOW;
            default:
                return com.essentialscore.api.scheduling.TaskPriority.LOW; // Use LOW as default
        }
    }
}
