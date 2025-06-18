package com.essentialscore.api.backup;

import com.essentialscore.api.command.CommandManager;
import com.essentialscore.api.command.CommandProcessor;
import com.essentialscore.api.module.ModuleRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Module for backup and disaster recovery functionality.
 */
@SuppressWarnings("deprecation") // Suppress ChatColor deprecation warnings
public class BackupModule {
    private static final Logger LOGGER = Logger.getLogger(BackupModule.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    
    private final Plugin plugin;
    private final CommandManager commandManager;
    private final BackupSystem backupSystem;
    
    /**
     * Creates a new backup module.
     *
     * @param plugin The plugin
     * @param moduleRegistry The module registry
     * @param commandManager The command manager
     */
    public BackupModule(Plugin plugin, ModuleRegistry moduleRegistry, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.backupSystem = new BackupSystem(plugin, moduleRegistry);
        // Start backup system
        backupSystem.start();
        
        // Register commands
        registerCommands();
        
        LOGGER.info("Backup module initialized");
    }
    
    /**
     * Registers commands for the backup module.
     */
    private void registerCommands() {
        // Main backup command
        CommandProcessor.SimpleCommand backupCommand = CommandProcessor.SimpleCommand.builder("backup", "core")
            .description("Backup and disaster recovery commands")
            .build(context -> {
                showHelp(context.getSender());
                return true;
            });

        // Create command
        CommandProcessor.SimpleCommand createCommand = CommandProcessor.SimpleCommand.builder("backup.create", "core")
            .description("Create a backup")
            .build(context -> {
                return handleCreateCommand(context.getSender(), null, context.getLabel(), context.getArgs());
            });
            
        // Restore command
        CommandProcessor.SimpleCommand restoreCommand = CommandProcessor.SimpleCommand.builder("backup.restore", "core")
            .description("Restore a backup")
            .build(context -> {
                return handleRestoreCommand(context.getSender(), null, context.getLabel(), context.getArgs());
            });
            
        // List command
        CommandProcessor.SimpleCommand listCommand = CommandProcessor.SimpleCommand.builder("backup.list", "core")
            .description("List available backups")
            .build(context -> {
                return handleListCommand(context.getSender(), null, context.getLabel(), context.getArgs());
            });
            
        // Validate command
        CommandProcessor.SimpleCommand validateCommand = CommandProcessor.SimpleCommand.builder("backup.validate", "core")
            .description("Validate a backup")
            .build(context -> {
                return handleValidateCommand(context.getSender(), null, context.getLabel(), context.getArgs());
            });
            
        // Register commands
        commandManager.registerCommand(backupCommand);
        commandManager.registerCommand(createCommand);
        commandManager.registerCommand(restoreCommand);
        commandManager.registerCommand(listCommand);
        commandManager.registerCommand(validateCommand);
    }
    
    /**
     * Shows help for the backup commands.
     *
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Backup and Disaster Recovery Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/backup create [full|incremental] [description]" + ChatColor.WHITE + " - Create a backup");
        sender.sendMessage(ChatColor.YELLOW + "/backup list [page]" + ChatColor.WHITE + " - List available backups");
        sender.sendMessage(ChatColor.YELLOW + "/backup restore <id|time>" + ChatColor.WHITE + " - Restore a backup");
        sender.sendMessage(ChatColor.YELLOW + "/backup restore <id> <component1,component2,...>" + ChatColor.WHITE + " - Restore specific components");
        sender.sendMessage(ChatColor.YELLOW + "/backup validate <id>" + ChatColor.WHITE + " - Validate a backup");
    }
    
    /**
     * Handles the create backup command.
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleCreateCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        final String[] typeRef = {args.length >= 1 ? args[0].toLowerCase() : "full"};
        final String[] descriptionRef = {args.length >= 2 ? String.join(" ", Arrays.asList(args).subList(1, args.length)) : "Manual backup"};
        
        if (!typeRef[0].equals("full") && !typeRef[0].equals("incremental")) {
            sender.sendMessage(ChatColor.RED + "Invalid backup type. Use 'full' or 'incremental'.");
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Creating " + typeRef[0] + " backup: " + descriptionRef[0]);
        
        // Execute backup in a separate thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BackupMetadata metadata;
                
                if (typeRef[0].equals("full")) {
                    metadata = backupSystem.createFullBackup(descriptionRef[0]);
                } else {
                    metadata = backupSystem.createIncrementalBackup(descriptionRef[0]);
                }
                
                if (metadata != null) {
                    sender.sendMessage(ChatColor.GREEN + "Backup created successfully: " + metadata.getBackupId());
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to create backup. Check server logs for details.");
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Error creating backup: " + e.getMessage());
                LOGGER.severe("Error creating backup: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return true;
    }
    
    /**
     * Handles the list backups command.
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleListCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number: " + args[0]);
                return true;
            }
        }
        
        List<BackupMetadata> backups = backupSystem.getMetadataManager().getAllMetadata();
        
        // Sort backups by timestamp (newest first)
        backups.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        
        int pageSize = 10;
        int totalPages = (backups.size() + pageSize - 1) / pageSize;
        
        if (page > totalPages) page = totalPages;
        
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, backups.size());
        
        sender.sendMessage(ChatColor.GOLD + "=== Backups (Page " + page + "/" + totalPages + ") ===");
        
        if (backups.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No backups available.");
            return true;
        }
        
        DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (int i = start; i < end; i++) {
            BackupMetadata backup = backups.get(i);
            
            LocalDateTime timestamp = LocalDateTime.ofInstant(backup.getTimestamp(), ZoneId.systemDefault());
            String formattedTime = displayFormat.format(timestamp);
            
            ChatColor typeColor = backup.getType() == BackupType.FULL ? ChatColor.GREEN : ChatColor.AQUA;
            ChatColor validColor = (backup.isValidated() && backup.isValid()) ? ChatColor.GREEN : 
                                 (backup.isValidated() ? ChatColor.RED : ChatColor.GRAY);
            
            sender.sendMessage(
                typeColor + "[" + backup.getType() + "] " +
                ChatColor.YELLOW + backup.getBackupId() + " " +
                ChatColor.WHITE + formattedTime + " " +
                validColor + (backup.isValidated() ? (backup.isValid() ? "✓" : "✗") : "?") + " " +
                ChatColor.GRAY + backup.getDescription()
            );
        }
        
        if (totalPages > 1) {
            sender.sendMessage(ChatColor.YELLOW + "Use '/backup list <page>' to see more backups.");
        }
        
        return true;
    }
    
    /**
     * Handles the restore backup command.
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleRestoreCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /backup restore <id|time> [component1,component2,...]");
            return true;
        }
        
        String idOrTime = args[0];
        Set<String> components = new HashSet<>();
        
        if (args.length >= 2) {
            components.addAll(Arrays.asList(args[1].split(",")));
        }
        
        // Confirm restoration
        sender.sendMessage(ChatColor.RED + "WARNING: Restoring a backup will replace current data!");
        sender.sendMessage(ChatColor.RED + "Type '/backup restore " + idOrTime + " confirm' to proceed.");
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            // Execute restore in a separate thread
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    boolean success;
                    
                    // Check if the argument is a timestamp
                    if (idOrTime.contains("-")) {
                        try {
                            // Try to parse as a date-time
                            LocalDateTime dateTime = LocalDateTime.parse(idOrTime, DATE_FORMAT);
                            Instant pointInTime = dateTime.atZone(ZoneId.systemDefault()).toInstant();
                            
                            success = backupSystem.restoreToPointInTime(pointInTime);
                            
                            if (success) {
                                sender.sendMessage(ChatColor.GREEN + "Successfully restored to point in time: " + idOrTime);
                            } else {
                                sender.sendMessage(ChatColor.RED + "Failed to restore to point in time: " + idOrTime);
                            }
                        } catch (DateTimeParseException e) {
                            // Not a valid date-time, treat as backup ID
                            if (components.isEmpty() || components.contains("confirm")) {
                                success = backupSystem.restoreBackup(idOrTime);
                                
                                if (success) {
                                    sender.sendMessage(ChatColor.GREEN + "Successfully restored backup: " + idOrTime);
                                } else {
                                    sender.sendMessage(ChatColor.RED + "Failed to restore backup: " + idOrTime);
                                }
                            } else {
                                success = backupSystem.restoreComponents(idOrTime, components);
                                
                                if (success) {
                                    sender.sendMessage(ChatColor.GREEN + "Successfully restored components from backup: " + idOrTime);
                                } else {
                                    sender.sendMessage(ChatColor.RED + "Failed to restore components from backup: " + idOrTime);
                                }
                            }
                        }
                    } else {
                        // Treat as backup ID
                        if (components.isEmpty() || components.contains("confirm")) {
                            success = backupSystem.restoreBackup(idOrTime);
                            
                            if (success) {
                                sender.sendMessage(ChatColor.GREEN + "Successfully restored backup: " + idOrTime);
                            } else {
                                sender.sendMessage(ChatColor.RED + "Failed to restore backup: " + idOrTime);
                            }
                        } else {
                            success = backupSystem.restoreComponents(idOrTime, components);
                            
                            if (success) {
                                sender.sendMessage(ChatColor.GREEN + "Successfully restored components from backup: " + idOrTime);
                            } else {
                                sender.sendMessage(ChatColor.RED + "Failed to restore components from backup: " + idOrTime);
                            }
                        }
                    }
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Error restoring backup: " + e.getMessage());
                    LOGGER.severe("Error restoring backup: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
        
        return true;
    }
    
    /**
     * Handles the validate backup command.
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleValidateCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /backup validate <id>");
            return true;
        }
        
        String idOrAll = args[0];
        
        // Execute validation in a separate thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (idOrAll.equalsIgnoreCase("all")) {
                    sender.sendMessage(ChatColor.YELLOW + "Validating all backups...");
                    backupSystem.validateBackups();
                    sender.sendMessage(ChatColor.GREEN + "Validation complete. Check server logs for details.");
                } else {
                    BackupMetadata metadata = backupSystem.getMetadataManager().getMetadata(idOrAll);
                    
                    if (metadata == null) {
                        sender.sendMessage(ChatColor.RED + "Backup not found: " + idOrAll);
                        return;
                    }
                    
                    sender.sendMessage(ChatColor.YELLOW + "Validating backup: " + idOrAll);
                    boolean isValid = backupSystem.getValidationService().validateBackup(backupSystem, metadata);
                    
                    if (isValid) {
                        sender.sendMessage(ChatColor.GREEN + "Backup is valid: " + idOrAll);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Backup validation failed: " + idOrAll);
                        sender.sendMessage(ChatColor.RED + "Reason: " + metadata.getValidationMessage());
                    }
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Error validating backup: " + e.getMessage());
                LOGGER.severe("Error validating backup: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return true;
    }
    
    /**
     * Shuts down the backup module.
     */
    public void shutdown() {
        // Unregister commands
        commandManager.unregisterModuleCommands("core");
        
        // Stop the backup system
        backupSystem.stop();
        
        LOGGER.info("Backup module shutdown");
    }
    
    /**
     * Gets the backup system.
     *
     * @return The backup system
     */
    public BackupSystem getBackupSystem() {
        return backupSystem;
    }
} 
