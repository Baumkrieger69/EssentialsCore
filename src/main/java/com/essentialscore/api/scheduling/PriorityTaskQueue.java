package com.essentialscore.api.scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * A priority queue for tasks that orders them by priority and execution time.
 */
public class PriorityTaskQueue {
    private static final Logger LOGGER = Logger.getLogger(PriorityTaskQueue.class.getName());
    
    private final PriorityBlockingQueue<ScheduledTask> queue;
    private final ReentrantLock lock;
    private boolean running;
    
    /**
     * Creates a new priority task queue.
     */
    public PriorityTaskQueue() {
        this.queue = new PriorityBlockingQueue<>();
        this.lock = new ReentrantLock();
        this.running = false;
    }
    
    /**
     * Starts the queue.
     */
    public void start() {
        lock.lock();
        try {
            running = true;
            LOGGER.info("Priority task queue started");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Stops the queue.
     */
    public void stop() {
        lock.lock();
        try {
            running = false;
            LOGGER.info("Priority task queue stopped with " + queue.size() + " tasks remaining");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Adds a task to the queue.
     *
     * @param task The task to add
     */
    public void offer(ScheduledTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        
        lock.lock();
        try {
            if (!running) {
                LOGGER.warning("Attempted to offer task to stopped queue: " + task.getName());
                return;
            }
            
            // If this task is already in the queue, remove it first
            queue.remove(task);
            
            // Add the task to the queue
            queue.offer(task);
            
            LOGGER.fine("Added task to queue: " + task.getName() + " (Priority: " + task.getPriority() + ")");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Retrieves and removes the next task from the queue.
     * Only returns tasks that are ready to execute.
     *
     * @return The next task, or null if no tasks are ready
     */
    public ScheduledTask poll() {
        lock.lock();
        try {
            if (!running || queue.isEmpty()) {
                return null;
            }
            
            // Peek at the highest priority task
            ScheduledTask task = queue.peek();
            
            // Check if it's time to execute
            if (task != null && task.getNextExecutionTime() <= System.currentTimeMillis()) {
                // Remove and return the task
                queue.poll();
                LOGGER.fine("Retrieved task from queue: " + task.getName());
                return task;
            }
            
            // No tasks are ready to execute
            return null;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Removes a task from the queue.
     *
     * @param task The task to remove
     * @return true if the task was removed
     */
    public boolean remove(ScheduledTask task) {
        lock.lock();
        try {
            boolean removed = queue.remove(task);
            if (removed) {
                LOGGER.fine("Removed task from queue: " + task.getName());
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets the number of tasks in the queue.
     *
     * @return The number of tasks
     */
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty
     */
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Clears the queue.
     */
    public void clear() {
        lock.lock();
        try {
            int size = queue.size();
            queue.clear();
            LOGGER.info("Cleared priority task queue, removed " + size + " tasks");
        } finally {
            lock.unlock();
        }
    }
} 
