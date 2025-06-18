package com.essentialscore.api.performance;

/**
 * Stores server performance metrics.
 */
public class ServerMetrics {
    private double cpuLoad;
    private long usedMemory;
    private long maxMemory;
    private int threadCount;
    private int playerCount;
    private int chunkCount;
    private int entityCount;
    private long timestamp;
    private double tps;
    
    /**
     * Creates a new server metrics object with default values.
     */
    public ServerMetrics() {
        this(0.0, 0L, 0L, 0, 0, 0, 0, System.currentTimeMillis());
    }
    
    /**
     * Creates a new server metrics object.
     *
     * @param cpuLoad The CPU load
     * @param usedMemory The used memory in bytes
     * @param maxMemory The maximum memory in bytes
     * @param threadCount The thread count
     * @param playerCount The player count
     * @param chunkCount The chunk count
     * @param entityCount The entity count
     * @param timestamp The timestamp
     */
    public ServerMetrics(double cpuLoad, long usedMemory, long maxMemory, int threadCount,
                        int playerCount, int chunkCount, int entityCount, long timestamp) {
        this.cpuLoad = cpuLoad;
        this.usedMemory = usedMemory;
        this.maxMemory = maxMemory;
        this.threadCount = threadCount;
        this.playerCount = playerCount;
        this.chunkCount = chunkCount;
        this.entityCount = entityCount;
        this.timestamp = timestamp;
        this.tps = 20.0; // Default to ideal TPS
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
     * Sets the CPU load.
     *
     * @param cpuLoad The CPU load
     */
    public void setCpuLoad(double cpuLoad) {
        this.cpuLoad = cpuLoad;
    }
    
    /**
     * Gets the used memory in bytes.
     *
     * @return The used memory
     */
    public long getUsedMemory() {
        return usedMemory;
    }
    
    /**
     * Sets the used memory in bytes.
     *
     * @param usedMemory The used memory
     */
    public void setUsedMemory(long usedMemory) {
        this.usedMemory = usedMemory;
    }
    
    /**
     * Gets the maximum memory in bytes.
     *
     * @return The maximum memory
     */
    public long getMaxMemory() {
        return maxMemory;
    }
    
    /**
     * Sets the maximum memory in bytes.
     *
     * @param maxMemory The maximum memory
     */
    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }
    
    /**
     * Gets the memory usage as a percentage.
     *
     * @return The memory usage percentage
     */
    public double getMemoryUsagePercentage() {
        return maxMemory > 0 ? (double) usedMemory / maxMemory * 100.0 : 0.0;
    }
    
    /**
     * Gets the memory usage in bytes.
     *
     * @return The memory usage in bytes
     */
    public double getMemoryUsage() {
        return (double) usedMemory;
    }
    
    /**
     * Gets the thread count.
     *
     * @return The thread count
     */
    public int getThreadCount() {
        return threadCount;
    }
    
    /**
     * Sets the thread count.
     *
     * @param threadCount The thread count
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
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
     * Sets the player count.
     *
     * @param playerCount The player count
     */
    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }
    
    /**
     * Gets the chunk count.
     *
     * @return The chunk count
     */
    public int getChunkCount() {
        return chunkCount;
    }
    
    /**
     * Sets the chunk count.
     *
     * @param chunkCount The chunk count
     */
    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }
    
    /**
     * Gets the entity count.
     *
     * @return The entity count
     */
    public int getEntityCount() {
        return entityCount;
    }
    
    /**
     * Sets the entity count.
     *
     * @param entityCount The entity count
     */
    public void setEntityCount(int entityCount) {
        this.entityCount = entityCount;
    }
    
    /**
     * Gets the timestamp.
     *
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the TPS (ticks per second).
     *
     * @return The TPS
     */
    public double getTps() {
        return tps;
    }
    
    /**
     * Sets the TPS (ticks per second).
     *
     * @param tps The TPS
     */
    public void setTps(double tps) {
        this.tps = tps;
    }
    
    @Override
    public String toString() {
        return "ServerMetrics{" +
               "cpuLoad=" + cpuLoad +
               ", memoryUsage=" + getMemoryUsagePercentage() + "%" +
               ", threadCount=" + threadCount +
               ", playerCount=" + playerCount +
               ", chunkCount=" + chunkCount +
               ", entityCount=" + entityCount +
               ", tps=" + tps +
               '}';
    }
}
