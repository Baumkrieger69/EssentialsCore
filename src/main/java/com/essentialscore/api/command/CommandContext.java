package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stores context information for command execution.
 * Provides a rich context for commands with parsed arguments, flags, and more.
 */
public class CommandContext {
    private final CommandSender sender;
    private final String label;
    private final String[] rawArgs;
    private final ParsedArguments parsedArgs;
    private final Map<String, Object> metadata;
    
    /**
     * Creates a new command context.
     *
     * @param sender The command sender
     * @param label The command label used
     * @param args The raw command arguments
     */
    public CommandContext(CommandSender sender, String label, String[] args) {
        this.sender = sender;
        this.label = label;
        this.rawArgs = args.clone();
        this.parsedArgs = new ParsedArguments(args);
        this.metadata = new HashMap<>();
    }
    
    /**
     * Gets the command sender.
     *
     * @return The command sender
     */
    public CommandSender getSender() {
        return sender;
    }
    
    /**
     * Gets the command sender as a player, if applicable.
     *
     * @return An optional containing the player, or empty if the sender is not a player
     */
    public Optional<Player> getPlayer() {
        return sender instanceof Player ? Optional.of((Player) sender) : Optional.empty();
    }
    
    /**
     * Gets the sender as a Player, or null if not a player.
     * 
     * @return The player or null
     */
    public Player getPlayerOrNull() {
        return isPlayer() ? (Player) sender : null;
    }
    
    /**
     * Checks if the sender is a player.
     *
     * @return true if the sender is a player
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }
    
    /**
     * Gets the command label used.
     *
     * @return The command label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Gets the raw command arguments.
     *
     * @return The raw arguments
     */
    public String[] getRawArgs() {
        return rawArgs.clone();
    }
    
    /**
     * Gets the arguments.
     * This is a compatibility method for older code.
     *
     * @return The raw arguments
     */
    public String[] getArgs() {
        return rawArgs.clone();
    }
    
    /**
     * Gets a specific argument.
     * This is a compatibility method for older code.
     *
     * @param index The index of the argument
     * @return The argument or null if index is out of bounds
     */
    public String getArg(int index) {
        return index < rawArgs.length ? rawArgs[index] : null;
    }
    
    /**
     * Gets the number of arguments.
     * This is a compatibility method for older code.
     *
     * @return The number of arguments
     */
    public int getArgCount() {
        return rawArgs.length;
    }
    
    /**
     * Gets the parsed arguments.
     *
     * @return The parsed arguments
     */
    public ParsedArguments getParsedArgs() {
        return parsedArgs;
    }
    
    /**
     * Checks if the sender has a permission.
     *
     * @param permission The permission to check
     * @return true if the sender has the permission
     */
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }
    
    /**
     * Gets a metadata value.
     *
     * @param key The metadata key
     * @param <T> The expected value type
     * @return The metadata value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }
    
    /**
     * Gets a metadata value with a default.
     *
     * @param key The metadata key
     * @param defaultValue The default value if not found
     * @param <T> The expected value type
     * @return The metadata value, or the default if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        return (T) metadata.getOrDefault(key, defaultValue);
    }
    
    /**
     * Sets a metadata value.
     *
     * @param key The metadata key
     * @param value The metadata value
     * @return This context for chaining
     */
    public CommandContext setMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }
    
    /**
     * Checks if a metadata key exists.
     *
     * @param key The metadata key
     * @return true if the key exists
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * Removes a metadata value.
     *
     * @param key The metadata key
     * @return This context for chaining
     */
    public CommandContext removeMetadata(String key) {
        metadata.remove(key);
        return this;
    }
    
    @Override
    public String toString() {
        return "CommandContext{" +
                "sender=" + sender.getName() +
                ", label='" + label + '\'' +
                ", args=" + Arrays.toString(rawArgs) +
                ", metadata=" + metadata +
                '}';
    }
} 
