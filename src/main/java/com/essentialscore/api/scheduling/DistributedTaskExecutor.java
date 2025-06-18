package com.essentialscore.api.scheduling;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes tasks across multiple servers in a distributed environment.
 */
public class DistributedTaskExecutor implements PluginMessageListener {
    private static final Logger LOGGER = Logger.getLogger(DistributedTaskExecutor.class.getName());
    
    // Channel for plugin messaging
    private static final String CHANNEL_NAMESPACE = "essentials:tasks";
    
    // Message types
    private static final byte MSG_TASK_EXECUTION = 0;
    private static final byte MSG_TASK_RESULT = 1;
    private static final byte MSG_SERVER_LOAD = 2;
    private static final byte MSG_LOAD_QUERY = 3;
      private final Plugin plugin;
    private final Map<UUID, CompletableFuture<Void>> pendingTasks;
    private final Map<String, ServerInfo> serverInfoMap;
    private final String serverId;
    private final AtomicInteger messageIdCounter;
    
    private boolean running;
    private DistributionStrategy distributionStrategy;
    
    /**
     * Creates a new distributed task executor.
     *
     * @param plugin The plugin
     */    public DistributedTaskExecutor(Plugin plugin) {
        this.plugin = plugin;
        this.pendingTasks = new ConcurrentHashMap<>();
        this.serverInfoMap = new ConcurrentHashMap<>();
        this.serverId = plugin.getConfig().getString("server-id", UUID.randomUUID().toString());
        this.messageIdCounter = new AtomicInteger(0);
        this.distributionStrategy = DistributionStrategy.LOAD_BALANCED;
    }
    
    /**
     * Starts the distributed task executor.
     */
    public void start() {
        if (running) return;
        
        LOGGER.info("Starting distributed task executor");
        
        // Register plugin messaging channels
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_NAMESPACE);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_NAMESPACE, this);
        
        // Schedule server load broadcasts
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::broadcastServerLoad, 20L, 600L); // Every 30 seconds
        
        // Query other servers for their load
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::queryServerLoads, 40L);
        
        running = true;
    }
    
    /**
     * Stops the distributed task executor.
     */
    public void stop() {
        if (!running) return;
        
        LOGGER.info("Stopping distributed task executor");
        
        // Unregister plugin messaging channels
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_NAMESPACE);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_NAMESPACE, this);
        
        // Fail all pending tasks
        for (Map.Entry<UUID, CompletableFuture<Void>> entry : pendingTasks.entrySet()) {
            entry.getValue().completeExceptionally(
                new IllegalStateException("Distributed task executor stopped")
            );
        }
        pendingTasks.clear();
        
        running = false;
    }
    
    /**
     * Executes a task in a distributed manner.
     *
     * @param task The task to execute
     * @return A future for the task execution
     */
    public CompletableFuture<Void> executeTask(ScheduledTask task) {
        if (!running) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Distributed task executor not running"));
            return future;
        }
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            // Determine if we should execute locally or remotely
            String targetServer = selectTargetServer(task);
            
            if (targetServer == null || targetServer.equals(serverId)) {
                // Execute locally
                LOGGER.fine("Executing distributed task locally: " + task.getName());
                return executeLocally(task);
            } else {
                // Execute remotely
                LOGGER.fine("Sending distributed task to server " + targetServer + ": " + task.getName());
                pendingTasks.put(task.getId(), future);
                
                // Attempt to send the task to the target server
                boolean sent = sendTaskToServer(task, targetServer);
                
                if (!sent) {
                    // If we couldn't send it, execute locally
                    pendingTasks.remove(task.getId());
                    LOGGER.warning("Could not send task to server " + targetServer + ", executing locally: " + task.getName());
                    return executeLocally(task);
                }
                
                // Set a timeout for the remote execution
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    CompletableFuture<Void> pendingFuture = pendingTasks.remove(task.getId());
                    if (pendingFuture != null && !pendingFuture.isDone()) {
                        pendingFuture.completeExceptionally(
                            new TimeoutException("Remote task execution timed out")
                        );
                        LOGGER.warning("Remote task execution timed out: " + task.getName());
                    }
                }, 1200L); // 60 second timeout
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Executes a task locally.
     *
     * @param task The task to execute
     * @return A future for the task execution
     */
    private CompletableFuture<Void> executeLocally(ScheduledTask task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            if (task.isAsync()) {
                // Execute asynchronously
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        task.getRunnable().run();
                        future.complete(null);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            } else {
                // Execute on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        task.getRunnable().run();
                        future.complete(null);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Selects a target server for a task.
     *
     * @param task The task
     * @return The target server ID
     */
    private String selectTargetServer(ScheduledTask task) {
        // If there are no other servers, execute locally
        if (serverInfoMap.isEmpty()) {
            return serverId;
        }
        
        switch (distributionStrategy) {
            case LOAD_BALANCED:
                return selectLeastLoadedServer();
            case ROUND_ROBIN:
                return selectNextServer();
            case RANDOM:
                return selectRandomServer();
            case STICKY:
                return getServerForTask(task);
            default:
                return serverId;
        }
    }
    
    /**
     * Selects the least loaded server.
     *
     * @return The server ID
     */
    private String selectLeastLoadedServer() {
        String leastLoadedServer = serverId;
        double lowestLoad = getCurrentServerLoad();
        
        for (Map.Entry<String, ServerInfo> entry : serverInfoMap.entrySet()) {
            ServerInfo info = entry.getValue();
            
            // Skip servers that haven't reported their load recently
            if (System.currentTimeMillis() - info.lastUpdate > 60000) {
                continue;
            }
            
            if (info.cpuLoad < lowestLoad) {
                lowestLoad = info.cpuLoad;
                leastLoadedServer = entry.getKey();
            }
        }
        
        return leastLoadedServer;
    }
    
    /**
     * Selects the next server in round-robin fashion.
     *
     * @return The server ID
     */
    private String selectNextServer() {
        // Simple round-robin implementation
        List<String> servers = new ArrayList<>(serverInfoMap.keySet());
        if (servers.isEmpty()) {
            return serverId;
        }
        
        // Add this server to the list
        servers.add(serverId);
        
        // Sort for consistent ordering
        Collections.sort(servers);
        
        // Find current index
        int currentIndex = servers.indexOf(serverId);
        
        // Get next index
        int nextIndex = (currentIndex + 1) % servers.size();
        
        return servers.get(nextIndex);
    }
    
    /**
     * Selects a random server.
     *
     * @return The server ID
     */
    private String selectRandomServer() {
        List<String> servers = new ArrayList<>(serverInfoMap.keySet());
        
        // Add this server to the list
        servers.add(serverId);
        
        // Pick a random server
        int index = new Random().nextInt(servers.size());
        return servers.get(index);
    }
    
    /**
     * Gets the server for a specific task (sticky distribution).
     *
     * @param task The task
     * @return The server ID
     */
    private String getServerForTask(ScheduledTask task) {
        // Use consistent hashing to always send the same task to the same server
        int hash = task.getId().hashCode();
        List<String> servers = new ArrayList<>(serverInfoMap.keySet());
        
        // Add this server to the list
        servers.add(serverId);
        
        // Sort for consistent ordering
        Collections.sort(servers);
        
        // Get index based on hash
        int index = Math.abs(hash % servers.size());
        return servers.get(index);
    }
    
    /**
     * Gets the current server load.
     *
     * @return The current load
     */
    private double getCurrentServerLoad() {
        // Get CPU usage from JVM
        return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    }
    
    /**
     * Sends a task to a server.
     *
     * @param task The task
     * @param serverId The server ID
     * @return true if the task was sent
     */
    private boolean sendTaskToServer(ScheduledTask task, String serverId) {
        try {
            // Find a player to send the message through
            Player player = getRandomPlayer();
            if (player == null) {
                LOGGER.warning("Cannot send distributed task: No players online");
                return false;
            }
            
            // Serialize the task execution request
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            
            // Message type: task execution
            out.writeByte(MSG_TASK_EXECUTION);
            
            // Message ID for tracking
            int messageId = messageIdCounter.incrementAndGet();
            out.writeInt(messageId);
            
            // Target server
            out.writeUTF(serverId);
            
            // Source server (this server)
            out.writeUTF(this.serverId);
            
            // Task details
            out.writeUTF(task.getId().toString());
            out.writeUTF(task.getName());
            out.writeBoolean(task.isAsync());
            
            // Send through plugin messaging
            player.sendPluginMessage(plugin, CHANNEL_NAMESPACE, baos.toByteArray());
            
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error sending distributed task", e);
            return false;
        }
    }
    
    /**
     * Broadcasts this server's load to other servers.
     */
    private void broadcastServerLoad() {
        try {
            // Find a player to send the message through
            Player player = getRandomPlayer();
            if (player == null) {
                return; // Silently fail if no players
            }
            
            // Get current load
            double load = getCurrentServerLoad();
            
            // Serialize the load info
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            
            // Message type: server load
            out.writeByte(MSG_SERVER_LOAD);
            
            // Server ID
            out.writeUTF(serverId);
            
            // CPU load
            out.writeDouble(load);
            
            // Memory usage
            long memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            out.writeLong(memoryUsage);
            
            // Player count
            out.writeInt(Bukkit.getOnlinePlayers().size());
            
            // Send through plugin messaging
            player.sendPluginMessage(plugin, CHANNEL_NAMESPACE, baos.toByteArray());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error broadcasting server load", e);
        }
    }
    
    /**
     * Queries other servers for their load.
     */
    private void queryServerLoads() {
        try {
            // Find a player to send the message through
            Player player = getRandomPlayer();
            if (player == null) {
                return; // Silently fail if no players
            }
            
            // Serialize the query
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            
            // Message type: load query
            out.writeByte(MSG_LOAD_QUERY);
            
            // Server ID
            out.writeUTF(serverId);
            
            // Send through plugin messaging
            player.sendPluginMessage(plugin, CHANNEL_NAMESPACE, baos.toByteArray());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error querying server loads", e);
        }
    }
    
    /**
     * Gets a random online player.
     *
     * @return A random player, or null if none
     */
    private Player getRandomPlayer() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return null;
        }
        
        // Return the first player (or a random one if we wanted to distribute)
        return players.iterator().next();
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL_NAMESPACE)) {
            return;
        }
        
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            byte messageType = in.readByte();
            
            switch (messageType) {
                case MSG_TASK_EXECUTION:
                    handleTaskExecution(in);
                    break;
                case MSG_TASK_RESULT:
                    handleTaskResult(in);
                    break;
                case MSG_SERVER_LOAD:
                    handleServerLoad(in);
                    break;
                case MSG_LOAD_QUERY:
                    handleLoadQuery(in);
                    break;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error processing plugin message", e);
        }
    }
    
    /**
     * Handles a task execution message.
     *
     * @param in The input stream
     */
    private void handleTaskExecution(DataInputStream in) throws IOException {
        int messageId = in.readInt();
        String targetServer = in.readUTF();
        String sourceServer = in.readUTF();
        String taskId = in.readUTF();
        String taskName = in.readUTF();
        boolean async = in.readBoolean();
        
        // Only process if we're the target
        if (!targetServer.equals(serverId)) {
            return;
        }
        
        LOGGER.fine("Received distributed task execution request: " + taskName + " from " + sourceServer);
        
        // Create a dummy task for execution
        Runnable runnable = () -> {
            // This would be replaced with actual task logic, which would be pre-registered
            LOGGER.info("Executing distributed task: " + taskName);
        };
        
        // Execute the task
        try {
            if (async) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            } else {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
            
            // Send success result
            sendTaskResult(UUID.fromString(taskId), messageId, sourceServer, true, null);
        } catch (Exception e) {
            // Send failure result
            sendTaskResult(UUID.fromString(taskId), messageId, sourceServer, false, e.getMessage());
        }
    }
    
    /**
     * Sends a task execution result.
     *
     * @param taskId The task ID
     * @param messageId The message ID
     * @param targetServer The target server
     * @param success Whether the execution was successful
     * @param errorMessage The error message, or null if successful
     */
    private void sendTaskResult(UUID taskId, int messageId, String targetServer, boolean success, String errorMessage) {
        try {
            // Find a player to send the message through
            Player player = getRandomPlayer();
            if (player == null) {
                LOGGER.warning("Cannot send task result: No players online");
                return;
            }
            
            // Serialize the result
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            
            // Message type: task result
            out.writeByte(MSG_TASK_RESULT);
            
            // Message ID
            out.writeInt(messageId);
            
            // Target server
            out.writeUTF(targetServer);
            
            // Source server (this server)
            out.writeUTF(serverId);
            
            // Task ID
            out.writeUTF(taskId.toString());
            
            // Success flag
            out.writeBoolean(success);
            
            // Error message (if any)
            out.writeBoolean(errorMessage != null);
            if (errorMessage != null) {
                out.writeUTF(errorMessage);
            }
            
            // Send through plugin messaging
            player.sendPluginMessage(plugin, CHANNEL_NAMESPACE, baos.toByteArray());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error sending task result", e);
        }
    }
    
    /**
     * Handles a task result message.
     *
     * @param in The input stream
     */    private void handleTaskResult(DataInputStream in) throws IOException {
        // Skip message ID - not used in current implementation
        in.readInt();
        String targetServer = in.readUTF();
        String sourceServer = in.readUTF();
        String taskId = in.readUTF();
        boolean success = in.readBoolean();
        boolean hasError = in.readBoolean();
        String errorMessage = hasError ? in.readUTF() : null;
        
        // Only process if we're the target
        if (!targetServer.equals(serverId)) {
            return;
        }
        
        LOGGER.fine("Received task result for task " + taskId + " from " + sourceServer + ": " + (success ? "success" : "failure"));
        
        // Find the pending task
        CompletableFuture<Void> future = pendingTasks.remove(UUID.fromString(taskId));
        if (future != null) {
            if (success) {
                future.complete(null);
            } else {
                future.completeExceptionally(new RemoteTaskException(errorMessage));
            }
        }
    }
    
    /**
     * Handles a server load message.
     *
     * @param in The input stream
     */
    private void handleServerLoad(DataInputStream in) throws IOException {
        String sourceServer = in.readUTF();
        double cpuLoad = in.readDouble();
        long memoryUsage = in.readLong();
        int playerCount = in.readInt();
        
        // Skip if it's our own server
        if (sourceServer.equals(serverId)) {
            return;
        }
        
        // Update server info
        ServerInfo info = serverInfoMap.computeIfAbsent(sourceServer, s -> new ServerInfo(sourceServer));
        info.cpuLoad = cpuLoad;
        info.memoryUsage = memoryUsage;
        info.playerCount = playerCount;
        info.lastUpdate = System.currentTimeMillis();
        
        LOGGER.fine("Updated server info for " + sourceServer + ": CPU=" + cpuLoad + ", Memory=" + memoryUsage + ", Players=" + playerCount);
    }
    
    /**
     * Handles a load query message.
     *
     * @param in The input stream
     */
    private void handleLoadQuery(DataInputStream in) throws IOException {
        String sourceServer = in.readUTF();
        
        // Respond with our load
        LOGGER.fine("Received load query from server: " + sourceServer);
        broadcastServerLoad();
    }
    
    /**
     * Sets the distribution strategy.
     *
     * @param strategy The strategy
     */
    public void setDistributionStrategy(DistributionStrategy strategy) {
        this.distributionStrategy = strategy;
        LOGGER.info("Set distribution strategy to: " + strategy);
    }
    
    /**
     * Gets the distribution strategy.
     *
     * @return The strategy
     */
    public DistributionStrategy getDistributionStrategy() {
        return distributionStrategy;
    }
    
    /**
     * Gets server information.
     *
     * @return The server information
     */
    public Map<String, ServerInfo> getServerInfo() {
        return new HashMap<>(serverInfoMap);
    }
    
    /**
     * Class representing server information.
     */
    public static class ServerInfo {
        private final String serverId;
        private double cpuLoad;
        private long memoryUsage;
        private int playerCount;
        private long lastUpdate;
        
        /**
         * Creates new server information.
         *
         * @param serverId The server ID
         */
        public ServerInfo(String serverId) {
            this.serverId = serverId;
            this.lastUpdate = System.currentTimeMillis();
        }
        
        /**
         * Gets the server ID.
         *
         * @return The server ID
         */
        public String getServerId() {
            return serverId;
        }
        
        /**
         * Gets the CPU load.
         *
         * @return The CPU load
         */
        public double getCpuLoad() {
            return cpuLoad;
        }
        
        /**
         * Gets the memory usage.
         *
         * @return The memory usage
         */
        public long getMemoryUsage() {
            return memoryUsage;
        }
        
        /**
         * Gets the player count.
         *
         * @return The player count
         */
        public int getPlayerCount() {
            return playerCount;
        }
        
        /**
         * Gets the last update time.
         *
         * @return The last update time
         */
        public long getLastUpdate() {
            return lastUpdate;
        }
        
        /**
         * Checks if the server info is stale.
         *
         * @return true if the info is stale
         */
        public boolean isStale() {
            return System.currentTimeMillis() - lastUpdate > 60000; // 60 seconds
        }
    }
    
    /**
     * Enum for distribution strategies.
     */
    public enum DistributionStrategy {
        /**
         * Distributes tasks to the server with the lowest load.
         */
        LOAD_BALANCED,
        
        /**
         * Distributes tasks in a round-robin fashion.
         */
        ROUND_ROBIN,
        
        /**
         * Distributes tasks randomly.
         */
        RANDOM,
        
        /**
         * Always sends the same task to the same server.
         */
        STICKY
    }
    
    /**
     * Exception for remote task execution failures.
     */
    public static class RemoteTaskException extends Exception {
        /**
         * Creates a new remote task exception.
         *
         * @param message The message
         */
        public RemoteTaskException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception for task execution timeouts.
     */
    public static class TimeoutException extends Exception {
        /**
         * Creates a new timeout exception.
         *
         * @param message The message
         */
        public TimeoutException(String message) {
            super(message);
        }
    }
} 
