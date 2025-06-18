package com.essentialscore.api.command;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base implementation of the Command interface.
 * Provides common functionality for all commands.
 */
public abstract class AbstractCommand implements Command {
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;
    private final String permission;
    private final String moduleId;
    private final Command parent;
    private final Map<String, Command> subCommands;
    private final int minArgs;
    private final int maxArgs;
    private final String category;
    private final String detailedHelp;
    private final List<String> examples;
    private final boolean hidden;
    private final int cooldown;
    
    /**
     * Creates a new abstract command.
     *
     * @param name The command name
     * @param description The command description
     * @param usage The command usage
     * @param aliases The command aliases
     * @param permission The permission required to use the command
     * @param moduleId The module ID that owns this command
     * @param parent The parent command, or null if this is a root command
     */
    protected AbstractCommand(String name, String description, String usage, List<String> aliases,
                             String permission, String moduleId, Command parent) {
        this(name, description, usage, aliases, permission, moduleId, parent, 0, -1, "General", "", Collections.emptyList(), false, 0);
    }
    
    /**
     * Creates a new abstract command with all parameters.
     *
     * @param name The command name
     * @param description The command description
     * @param usage The command usage
     * @param aliases The command aliases
     * @param permission The permission required to use the command
     * @param moduleId The module ID that owns this command
     * @param parent The parent command, or null if this is a root command
     * @param minArgs The minimum number of arguments required
     * @param maxArgs The maximum number of arguments allowed, or -1 for unlimited
     * @param category The command category
     * @param detailedHelp The detailed help text
     * @param examples Examples of how to use the command
     * @param hidden Whether the command is hidden from help listings
     * @param cooldown The cooldown time in seconds, or 0 for no cooldown
     */
    protected AbstractCommand(String name, String description, String usage, List<String> aliases,
                             String permission, String moduleId, Command parent, int minArgs, int maxArgs,
                             String category, String detailedHelp, List<String> examples, boolean hidden, int cooldown) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.aliases = aliases != null ? new ArrayList<>(aliases) : new ArrayList<>();
        this.permission = permission;
        this.moduleId = moduleId;
        this.parent = parent;
        this.subCommands = new LinkedHashMap<>();
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.category = category;
        this.detailedHelp = detailedHelp;
        this.examples = examples != null ? new ArrayList<>(examples) : new ArrayList<>();
        this.hidden = hidden;
        this.cooldown = cooldown;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getUsage() {
        return usage;
    }
    
    @Override
    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }
    
    @Override
    public String getPermission() {
        return permission;
    }
    
    @Override
    public String getModuleId() {
        return moduleId;
    }
    
    @Override
    public Command getParent() {
        return parent;
    }
    
    @Override
    public List<Command> getSubCommands() {
        return Collections.unmodifiableList(new ArrayList<>(subCommands.values()));
    }
    
    @Override
    public void addSubCommand(Command subCommand) {
        if (subCommand == null) return;
        
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }
    
    @Override
    public boolean removeSubCommand(Command subCommand) {
        if (subCommand == null) return false;
        
        boolean removed = false;
        if (subCommands.get(subCommand.getName().toLowerCase()) == subCommand) {
            subCommands.remove(subCommand.getName().toLowerCase());
            removed = true;
        }
        
        for (String alias : subCommand.getAliases()) {
            if (subCommands.get(alias.toLowerCase()) == subCommand) {
                subCommands.remove(alias.toLowerCase());
                removed = true;
            }
        }
        
        return removed;
    }
    
    @Override
    public Command getSubCommand(String name) {
        return name != null ? subCommands.get(name.toLowerCase()) : null;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            String arg = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            
            // Add matching sub-commands
            for (Command subCommand : getSubCommands()) {
                if (hasPermissionForCommand(sender, subCommand) && startsWith(subCommand.getName(), arg) && !subCommand.isHidden()) {
                    completions.add(subCommand.getName());
                }
            }
            
            return completions;
        } else {
            // Try to delegate to a sub-command
            Command subCommand = getSubCommand(args[0]);
            if (subCommand != null && hasPermissionForCommand(sender, subCommand)) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }
        
        return Collections.emptyList();
    }
    
    @Override
    public int getMinArgs() {
        return minArgs;
    }
    
    @Override
    public int getMaxArgs() {
        return maxArgs;
    }
    
    @Override
    public String getCategory() {
        return category;
    }
    
    @Override
    public String getDetailedHelp() {
        return detailedHelp;
    }
    
    @Override
    public List<String> getExamples() {
        return Collections.unmodifiableList(examples);
    }
    
    @Override
    public boolean isHidden() {
        return hidden;
    }
    
    @Override
    public int getCooldown() {
        return cooldown;
    }
    
    /**
     * Gets the full name of the command, including parent commands.
     *
     * @return The full command name
     */
    public String getFullName() {
        if (parent instanceof AbstractCommand) {
            return ((AbstractCommand) parent).getFullName() + " " + name;
        } else if (parent != null) {
            return parent.getName() + " " + name;
        }
        return name;
    }
    
    /**
     * Gets the full usage of the command, including the full name.
     *
     * @return The full command usage
     */
    public String getFullUsage() {
        return "/" + getFullName() + (usage.isEmpty() ? "" : " " + usage);
    }
    
    /**
     * Checks if the sender has permission to use a command.
     *
     * @param sender The command sender
     * @param command The command to check
     * @return true if the sender has permission
     */
    protected boolean hasPermissionForCommand(CommandSender sender, Command command) {
        String permission = command.getPermission();
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }
    
    /**
     * Checks if a string starts with a prefix, ignoring case.
     *
     * @param str The string to check
     * @param prefix The prefix to check for
     * @return true if the string starts with the prefix
     */
    protected boolean startsWith(String str, String prefix) {
        return str.toLowerCase().startsWith(prefix.toLowerCase());
    }
    
    /**
     * Builder for creating commands.
     */
    public static class Builder {
        private String name;
        private String description = "";
        private String usage = "";
        private List<String> aliases = new ArrayList<>();
        private String permission = "";
        private String moduleId;
        private Command parent;
        private int minArgs = 0;
        private int maxArgs = -1;
        private String category = "General";
        private String detailedHelp = "";
        private List<String> examples = new ArrayList<>();
        private boolean hidden = false;
        private int cooldown = 0;
        
        /**
         * Creates a new builder.
         *
         * @param name The command name
         * @param moduleId The module ID that owns this command
         */
        public Builder(String name, String moduleId) {
            this.name = name;
            this.moduleId = moduleId;
        }
        
        /**
         * Sets the command description.
         *
         * @param description The description
         * @return This builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the command usage.
         *
         * @param usage The usage
         * @return This builder
         */
        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }
        
        /**
         * Sets the command aliases.
         *
         * @param aliases The aliases
         * @return This builder
         */
        public Builder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }
        
        /**
         * Sets the permission required to use the command.
         *
         * @param permission The permission
         * @return This builder
         */
        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }
        
        /**
         * Sets the parent command.
         *
         * @param parent The parent command
         * @return This builder
         */
        public Builder parent(Command parent) {
            this.parent = parent;
            return this;
        }
        
        /**
         * Sets the minimum number of arguments required.
         *
         * @param minArgs The minimum arguments
         * @return This builder
         */
        public Builder minArgs(int minArgs) {
            this.minArgs = minArgs;
            return this;
        }
        
        /**
         * Sets the maximum number of arguments allowed.
         *
         * @param maxArgs The maximum arguments
         * @return This builder
         */
        public Builder maxArgs(int maxArgs) {
            this.maxArgs = maxArgs;
            return this;
        }
        
        /**
         * Sets the command category.
         *
         * @param category The category
         * @return This builder
         */
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        /**
         * Sets the detailed help text.
         *
         * @param detailedHelp The detailed help
         * @return This builder
         */
        public Builder detailedHelp(String detailedHelp) {
            this.detailedHelp = detailedHelp;
            return this;
        }
        
        /**
         * Sets the command examples.
         *
         * @param examples The examples
         * @return This builder
         */
        public Builder examples(List<String> examples) {
            this.examples = examples;
            return this;
        }
        
        /**
         * Sets whether the command is hidden from help listings.
         *
         * @param hidden Whether the command is hidden
         * @return This builder
         */
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }
        
        /**
         * Sets the cooldown time in seconds.
         *
         * @param cooldown The cooldown time
         * @return This builder
         */
        public Builder cooldown(int cooldown) {
            this.cooldown = cooldown;
            return this;
        }
        
        /**
         * Gets the command name.
         *
         * @return The command name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Gets the command description.
         *
         * @return The command description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the command usage.
         *
         * @return The command usage
         */
        public String getUsage() {
            return usage;
        }
        
        /**
         * Gets the command aliases.
         *
         * @return The command aliases
         */
        public List<String> getAliases() {
            return aliases;
        }
        
        /**
         * Gets the permission required to use the command.
         *
         * @return The command permission
         */
        public String getPermission() {
            return permission;
        }
        
        /**
         * Gets the module ID that owns this command.
         *
         * @return The module ID
         */
        public String getModuleId() {
            return moduleId;
        }
        
        /**
         * Gets the parent command.
         *
         * @return The parent command
         */
        public Command getParent() {
            return parent;
        }
        
        /**
         * Gets the minimum number of arguments required.
         *
         * @return The minimum arguments
         */
        public int getMinArgs() {
            return minArgs;
        }
        
        /**
         * Gets the maximum number of arguments allowed.
         *
         * @return The maximum arguments
         */
        public int getMaxArgs() {
            return maxArgs;
        }
        
        /**
         * Gets the command category.
         *
         * @return The command category
         */
        public String getCategory() {
            return category;
        }
        
        /**
         * Gets the detailed help text.
         *
         * @return The detailed help
         */
        public String getDetailedHelp() {
            return detailedHelp;
        }
        
        /**
         * Gets the command examples.
         *
         * @return The command examples
         */
        public List<String> getExamples() {
            return examples;
        }
        
        /**
         * Checks if the command is hidden from help listings.
         *
         * @return Whether the command is hidden
         */
        public boolean isHidden() {
            return hidden;
        }
        
        /**
         * Gets the cooldown time in seconds.
         *
         * @return The cooldown time
         */
        public int getCooldown() {
            return cooldown;
        }
    }
} 
