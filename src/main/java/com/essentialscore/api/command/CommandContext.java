package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

<<<<<<< HEAD
/**
 * Kontext für einen Befehl
 */
public class CommandContext {
    private final CommandSender sender;
    private final String[] args;
    private final String label;
    
    /**
     * Erstellt einen neuen CommandContext
     * 
     * @param sender Der Absender des Befehls
     * @param args Die Argumente des Befehls
     * @param label Das verwendete Label des Befehls
     */
    public CommandContext(CommandSender sender, String[] args, String label) {
        this.sender = sender;
        this.args = args;
        this.label = label;
    }
    
    /**
     * Holt den Absender des Befehls
     * 
     * @return Der Absender
=======
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
>>>>>>> 1cd13da (Das ist Dumm)
     */
    public CommandSender getSender() {
        return sender;
    }
    
    /**
<<<<<<< HEAD
     * Prüft ob der Absender ein Spieler ist
     * 
     * @return true wenn Spieler
=======
     * Gets the command sender as a player, if applicable.
     *
     * @return An optional containing the player, or empty if the sender is not a player
     */
    public Optional<Player> getPlayer() {
        return sender instanceof Player ? Optional.of((Player) sender) : Optional.empty();
    }
    
    /**
     * Checks if the sender is a player.
     *
     * @return true if the sender is a player
>>>>>>> 1cd13da (Das ist Dumm)
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }
    
    /**
<<<<<<< HEAD
     * Holt den Absender als Spieler
     * 
     * @return Der Spieler oder null
     */
    public Player getPlayer() {
        return isPlayer() ? (Player) sender : null;
    }
    
    /**
     * Holt die Argumente des Befehls
     * 
     * @return Die Argumente
     */
    public String[] getArgs() {
        return args;
    }
    
    /**
     * Holt ein Argument an einer bestimmten Position
     * 
     * @param index Der Index
     * @return Das Argument oder null
     */
    public String getArg(int index) {
        return index < args.length ? args[index] : null;
    }
    
    /**
     * Holt die Anzahl der Argumente
     * 
     * @return Die Anzahl
     */
    public int getArgCount() {
        return args.length;
    }
    
    /**
     * Holt das verwendete Label des Befehls
     * 
     * @return Das Label
=======
     * Gets the command label used.
     *
     * @return The command label
>>>>>>> 1cd13da (Das ist Dumm)
     */
    public String getLabel() {
        return label;
    }
    
    /**
<<<<<<< HEAD
     * Prüft ob der Absender eine Berechtigung hat
     * 
     * @param permission Die Berechtigung
     * @return true wenn berechtigt
     */
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
=======
     * Gets the raw command arguments.
     *
     * @return The raw arguments
     */
    public String[] getRawArgs() {
        return rawArgs.clone();
    }
    
    /**
     * Gets the parsed arguments.
     *
     * @return The parsed arguments
     */
    public ParsedArguments getArgs() {
        return parsedArgs;
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
>>>>>>> 1cd13da (Das ist Dumm)
    }
} 