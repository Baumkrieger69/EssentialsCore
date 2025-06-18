package com.essentialscore.api.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a command that can be registered and unregistered dynamically.
 */
public class DynamicCommand {
    private final String name;
    private final String moduleId;
    private final List<String> aliases;
    private final String description;
    private final String usage;
    private final String permission;
    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;

    /**
     * Creates a new dynamic command.
     *
     * @param name The command name
     * @param moduleId The module ID
     * @param executor The command executor
     */
    public DynamicCommand(String name, String moduleId, CommandExecutor executor) {
        this(name, moduleId, new ArrayList<>(), "", "/" + name, null, executor, null);
    }

    /**
     * Creates a new dynamic command.
     *
     * @param name The command name
     * @param moduleId The module ID
     * @param aliases The command aliases
     * @param description The command description
     * @param usage The command usage
     * @param permission The command permission
     * @param executor The command executor
     * @param tabCompleter The tab completer
     */
    public DynamicCommand(String name, String moduleId, List<String> aliases, String description, String usage,
                         String permission, CommandExecutor executor, TabCompleter tabCompleter) {
        this.name = name.toLowerCase();
        this.moduleId = moduleId;
        this.aliases = aliases != null ? aliases : new ArrayList<>();
        this.description = description != null ? description : "";
        this.usage = usage != null ? usage : "/" + name;
        this.permission = permission;
        this.executor = Objects.requireNonNull(executor, "Executor cannot be null");
        this.tabCompleter = tabCompleter;
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
     * Gets the module ID.
     *
     * @return The module ID
     */
    public String getModuleId() {
        return moduleId;
    }

    /**
     * Gets the command aliases.
     *
     * @return The command aliases
     */
    public List<String> getAliases() {
        return new ArrayList<>(aliases);
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
     * Gets the command permission.
     *
     * @return The command permission, or null if none
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the command executor.
     *
     * @return The command executor
     */
    public CommandExecutor getExecutor() {
        return executor;
    }

    /**
     * Gets the tab completer.
     *
     * @return The tab completer, or null if none
     */
    public TabCompleter getTabCompleter() {
        return tabCompleter;
    }

    /**
     * Executes the command.
     *
     * @param sender The command sender
     * @param label The command label
     * @param args The command arguments
     * @return True if the command was executed successfully
     */
    public boolean execute(CommandSender sender, String label, String[] args) {
        return executor.onCommand(sender, null, label, args);
    }

    /**
     * Provides tab completions for the command.
     *
     * @param sender The command sender
     * @param label The command label
     * @param args The command arguments
     * @return A list of tab completions, or null if none
     */
    public List<String> tabComplete(CommandSender sender, String label, String[] args) {
        if (tabCompleter != null) {
            return tabCompleter.onTabComplete(sender, null, label, args);
        }
        return null;
    }

    /**
     * Creates a builder for a dynamic command.
     *
     * @param name The command name
     * @param moduleId The module ID
     * @return The builder
     */
    public static Builder builder(String name, String moduleId) {
        return new Builder(name, moduleId);
    }

    /**
     * Builder for dynamic commands.
     */
    public static class Builder {
        private final String name;
        private final String moduleId;
        private List<String> aliases = new ArrayList<>();
        private String description = "";
        private String usage = null;
        private String permission = null;
        private CommandExecutor executor = null;
        private TabCompleter tabCompleter = null;

        private Builder(String name, String moduleId) {
            this.name = name;
            this.moduleId = moduleId;
        }

        /**
         * Sets the command aliases.
         *
         * @param aliases The command aliases
         * @return This builder
         */
        public Builder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        /**
         * Sets the command description.
         *
         * @param description The command description
         * @return This builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the command usage.
         *
         * @param usage The command usage
         * @return This builder
         */
        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        /**
         * Sets the command permission.
         *
         * @param permission The command permission
         * @return This builder
         */
        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Sets the tab completer.
         *
         * @param tabCompleter The tab completer
         * @return This builder
         */
        public Builder tabCompleter(TabCompleter tabCompleter) {
            this.tabCompleter = tabCompleter;
            return this;
        }

        /**
         * Sets the command executor.
         *
         * @param executor The command executor
         * @return This builder
         */
        public Builder executor(CommandExecutor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Builds the command.
         *
         * @return The dynamic command
         * @throws IllegalStateException if executor is null
         */
        public DynamicCommand build() {
            if (usage == null) {
                usage = "/" + name;
            }
            if (executor == null) {
                throw new IllegalStateException("Command executor cannot be null");
            }
            return new DynamicCommand(name, moduleId, aliases, description, usage, permission, executor, tabCompleter);
        }
    }
} 
