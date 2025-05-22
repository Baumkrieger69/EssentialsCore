package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
     */
    public CommandSender getSender() {
        return sender;
    }
    
    /**
     * Prüft ob der Absender ein Spieler ist
     * 
     * @return true wenn Spieler
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }
    
    /**
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
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Prüft ob der Absender eine Berechtigung hat
     * 
     * @param permission Die Berechtigung
     * @return true wenn berechtigt
     */
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }
} 