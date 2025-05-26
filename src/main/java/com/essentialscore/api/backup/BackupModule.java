package com.essentialscore.api.backup;

import com.essentialscore.api.command.CommandContext;
import com.essentialscore.api.command.CommandManager;
import com.essentialscore.api.command.SimpleCommand;
import com.essentialscore.api.module.ModuleRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
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
public class BackupModule {
    private static final Logger LOGGER = Logger.getLogger(BackupModule.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    
    private final Plugin plugin;
    private final ModuleRegistry moduleRegistry;
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
        this.moduleRegistry = moduleRegistry;
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
        SimpleCommand backupCommand = SimpleCommand.builder("backup", "core")
            .description("Backup and disaster recovery commands")
            .permission("essentials.backup")
            .build(context -> {
                showHelp(context.getSender());
                return true;
            });
        
        // Create backup command
        SimpleCommand createCommand = SimpleCommand.builder("create", "core")
            .description("Create a backup")
            .permission("essentials.backup.create")
            .parent(backupCommand)
            .build(this::handleCreateCommand);
        
        // List backups command
        SimpleCommand listCommand = SimpleCommand.builder("list", "core")
            .description("List available backups")
            .permission("essentials.backup.list")
            .parent(backupCommand)
            .build(this::handleListCommand);
        
        // Restore backup command
        SimpleCommand restoreCommand = SimpleCommand.builder("restore", "core")
            .description("Restore a backup")
            .permission("essentials.backup.restore")
            .parent(backupCommand)
            .build(this::handleRestoreCommand);
        
        // Validate backup command
        SimpleCommand validateCommand = SimpleCommand.builder("validate", "core")
            .description("Validate a backup")
            .permission("essentials.backup.validate")
            .parent(backupCommand)
            .build(this::handleValidateCommand);
        
        // Add sub-commands to the main command
        backupCommand.addSubCommand(createCommand);
        backupCommand.addSubCommand(listCommand);
        backupCommand.addSubCommand(restoreCommand);
        backupCommand.addSubCommand(validateCommand);
        
        // Register the main command
        commandManager.registerCommand(backupCommand);
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
     * @param context The command context
     * @return true if the command was handled
     */
    private boolean handleCreateCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        List<String> args = context.getArgs().getAll();
        
        String type = "full";
        String description = "Manual backup";
        
        if (args.size() >= 1) {
            type = args.get(0).toLowerCase();
            
            if (!type.equals("full") && !type.equals("incremental")) {
                sender.sendMessage(ChatColor.RED + "Invalid backup type. Use 'full' or 'incremental'.");
                return true;
            }
        }
        
        if (args.size() >= 2) {
            description = String.join(" ", args.subList(1, args.size()));
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Creating " + type + " backup: " + description);
        
        // Execute backup in a separate thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BackupMetadata metadata;
                
                if (type.equals("full")) {
                    metadata = backupSystem.createFullBackup(description);
                } else {
                    metadata = backupSystem.createIncrementalBackup(description);
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
     * @param context The command context
     * @return true if the command was handled
     */
    private boolean handleListCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        List<String> args = context.getArgs().getAll();
        
        int page = 1;
        if (args.size() >= 1) {
            try {
                page = Integer.parseInt(args.get(0));
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number: " + args.get(0));
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
     * @param context The command context
     * @return true if the command was handled
     */
    private boolean handleRestoreCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        List<String> args = context.getArgs().getAll();
        
        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Usage: /backup restore <id|time> [component1,component2,...]");
            return true;
        }
        
        String idOrTime = args.get(0);
        Set<String> components = new HashSet<>();
        
        if (args.size() >= 2) {
            components.addAll(Arrays.asList(args.get(1).split(",")));
        }
        
        // Confirm restoration
        sender.sendMessage(ChatColor.RED + "WARNING: Restoring a backup will replace current data!");
        sender.sendMessage(ChatColor.RED + "Type '/backup restore " + idOrTime + " confirm' to proceed.");
        
        if (args.size() >= 2 && args.get(1).equalsIgnoreCase("confirm")) {
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
     * @param context The command context
     * @return true if the command was handled
     */
    private boolean handleValidateCommand(CommandContext context) {
        CommandSender sender = context.getSender();
        List<String> args = context.getArgs().getAll();
        
        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Usage: /backup validate <id|all>");
            return true;
        }
        
        String idOrAll = args.get(0);
        
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