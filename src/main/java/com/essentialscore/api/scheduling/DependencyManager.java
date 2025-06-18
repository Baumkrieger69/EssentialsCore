package com.essentialscore.api.scheduling;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages dependencies between tasks.
 */
public class DependencyManager {
    private static final Logger LOGGER = Logger.getLogger(DependencyManager.class.getName());
    
    // Map of task IDs to their completion status
    private final Map<UUID, Boolean> completionStatus;
    
    /**
     * Creates a new dependency manager.
     */
    public DependencyManager() {
        this.completionStatus = new ConcurrentHashMap<>();
    }
    
    /**
     * Checks if all dependencies for a task are met.
     *
     * @param task The task
     * @return true if all dependencies are met
     */
    public boolean areDependenciesMet(ScheduledTask task) {
        Set<UUID> dependencies = task.getDependencies();
        
        // If there are no dependencies, they are automatically met
        if (dependencies == null || dependencies.isEmpty()) {
            return true;
        }
        
        // Check each dependency
        for (UUID dependencyId : dependencies) {
            // If the dependency doesn't exist in our status map or is not completed,
            // the dependencies are not met
            Boolean status = completionStatus.get(dependencyId);
            if (status == null || !status) {
                // Set task state to waiting for dependencies
                task.setState(TaskState.WAITING_FOR_DEPENDENCIES);
                return false;
            }
        }
        
        // All dependencies are met
        return true;
    }
    
    /**
     * Marks a task as completed.
     *
     * @param taskId The task ID
     */
    public void markCompleted(UUID taskId) {
        completionStatus.put(taskId, true);
        LOGGER.fine("Marked task as completed: " + taskId);
    }
    
    /**
     * Marks a task as not completed.
     *
     * @param taskId The task ID
     */
    public void markNotCompleted(UUID taskId) {
        completionStatus.put(taskId, false);
        LOGGER.fine("Marked task as not completed: " + taskId);
    }
    
    /**
     * Removes a task from the dependency manager.
     *
     * @param taskId The task ID
     */
    public void removeTask(UUID taskId) {
        completionStatus.remove(taskId);
        LOGGER.fine("Removed task from dependency manager: " + taskId);
    }
    
    /**
     * Checks if a task is completed.
     *
     * @param taskId The task ID
     * @return true if the task is completed, false otherwise
     */
    public boolean isCompleted(UUID taskId) {
        Boolean status = completionStatus.get(taskId);
        return status != null && status;
    }
    
    /**
     * Gets all tasks with their completion status.
     *
     * @return The tasks with their completion status
     */
    public Map<UUID, Boolean> getCompletionStatus() {
        return new HashMap<>(completionStatus);
    }
    
    /**
     * Clears all completion status.
     */
    public void clear() {
        completionStatus.clear();
        LOGGER.info("Cleared all dependency completion status");
    }
} 
