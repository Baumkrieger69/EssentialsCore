package com.essentialscore.api.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * A simple command implementation for EssentialsCore modules
 */
public class SimpleCommand extends Command {
    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;

    public SimpleCommand(String name, CommandExecutor executor) {
        this(name, executor, null);
    }

    public SimpleCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        super(name);
        this.executor = executor;
        this.tabCompleter = tabCompleter;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (tabCompleter != null) {
            return tabCompleter.onTabComplete(sender, this, alias, args);
        }
        return super.tabComplete(sender, alias, args);
    }

    /**
     * Builder class for SimpleCommand
     */
    public static class Builder {
        private String name;
        private String description;
        private String usage;
        private List<String> aliases;

        public Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        /**
         * Build the command with executor
         * @param executor The command executor
         * @return The built command
         */
        public SimpleCommand build(CommandExecutor executor) {
            return build(executor, null);
        }

        /**
         * Build the command with executor and tab completer
         * @param executor The command executor
         * @param tabCompleter The tab completer
         * @return The built command
         */
        public SimpleCommand build(CommandExecutor executor, TabCompleter tabCompleter) {
            SimpleCommand command = new SimpleCommand(name, executor, tabCompleter);
            if (description != null) {
                command.setDescription(description);
            }
            if (usage != null) {
                command.setUsage(usage);
            }
            if (aliases != null) {
                command.setAliases(aliases);
            }
            return command;
        }
    }
}
