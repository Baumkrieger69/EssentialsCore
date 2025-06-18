package com.essentialscore.api.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum defining the types of security permissions that can be granted to modules.
 */
public enum SecurityPermission {
    /**
     * Permission to read files.
     */
    FILE_READ("file.read"),
    
    /**
     * Permission to write files.
     */
    FILE_WRITE("file.write"),
    
    /**
     * Permission to execute files.
     */
    FILE_EXECUTE("file.execute"),
    
    /**
     * Permission to connect to network resources.
     */
    NETWORK_CONNECT("network.connect"),
    
    /**
     * Permission to listen on network ports.
     */
    NETWORK_LISTEN("network.listen"),
    
    /**
     * Permission to interact with other plugins.
     */
    PLUGIN_INTERACT("plugin.interact"),
    
    /**
     * Permission to use command blocks.
     */
    COMMAND_BLOCK("command.block"),
    
    /**
     * Permission to run Bukkit commands.
     */
    COMMAND_RUN("command.run"),
    
    /**
     * Permission to access the database.
     */
    DATABASE_ACCESS("database.access"),
    
    /**
     * Permission to use reflection.
     */
    REFLECTION("reflection"),
    
    /**
     * Permission to create threads.
     */
    THREAD_CREATE("thread.create"),
    
    /**
     * Permission to use environment variables.
     */
    ENV_ACCESS("env.access"),
    
    /**
     * Permission to create servers or listeners.
     */
    SERVER_CREATE("server.create"),
    
    /**
     * Permission to use the Bukkit API.
     */
    BUKKIT_API("bukkit.api"),
    
    /**
     * Permission to use the Java APIs.
     */
    JAVA_API("java.api");
    
    private final String value;
    private static final Map<String, SecurityPermission> STRING_TO_ENUM = new HashMap<>();
    
    static {
        for (SecurityPermission permission : values()) {
            STRING_TO_ENUM.put(permission.getValue(), permission);
        }
    }
    
    /**
     * Creates a new security permission.
     *
     * @param value The permission value
     */
    SecurityPermission(String value) {
        this.value = value;
    }
    
    /**
     * Gets the permission value.
     *
     * @return The permission value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Gets a permission from its string value.
     *
     * @param value The permission value
     * @return The permission, or null if not found
     */
    public static SecurityPermission fromString(String value) {
        return STRING_TO_ENUM.get(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
} 
