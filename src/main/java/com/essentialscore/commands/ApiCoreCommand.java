package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Set;

public class ApiCoreCommand implements CommandExecutor, TabCompleter {

    private final ApiCore plugin;

    public ApiCoreCommand(JavaPlugin plugin) {
        this.plugin = (ApiCore) plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /apicore <subcommand>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "loadmodule":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /apicore loadmodule <module>");
                    return true;
                }
                File moduleFile = new File(plugin.getDataFolder(), "modules/" + args[1] + ".jar");
                if (moduleFile.exists()) {
                    plugin.getModuleManager().loadModule(moduleFile);
                    sender.sendMessage("§aModul geladen: " + args[1]);
                } else {
                    sender.sendMessage("§cModul-Datei nicht gefunden: " + args[1]);
                }
                break;            case "reloadmodules":
                plugin.getModuleManager().reloadModules();
                sender.sendMessage("§aAlle Module wurden neu geladen!");
                break;            case "listmodules":
                Set<String> moduleNames = plugin.getModuleManager().getLoadedModules().keySet();
                if (moduleNames.isEmpty()) {
                    sender.sendMessage("§e⚠ Keine Module geladen");
                } else {
                    sender.sendMessage("§7=== Geladene Module ===");
                    moduleNames.stream().forEach(moduleName -> {
                        Object moduleInfo = plugin.getModuleManager().getLoadedModules().get(moduleName);
                        if (moduleInfo != null) {
                            sender.sendMessage("§a✓ §7" + moduleName + " §8- §aAktiviert");
                        } else {
                            sender.sendMessage("§c✗ §7" + moduleName + " §8- §cFehler");
                        }
                    });
                }
                break;

            case "version":
                sender.sendMessage("§7Plugin: §f" + plugin.getPluginMeta().getName());
                sender.sendMessage("§7Version: §f" + plugin.getPluginMeta().getVersion());
                sender.sendMessage("§7Description: §f" + plugin.getPluginMeta().getDescription());
                break;            case "debug":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /apicore debug <true|false>");
                    return true;
                }
                plugin.setDebugMode(Boolean.parseBoolean(args[1]));
                sender.sendMessage("§aDebug-Modus " + (Boolean.parseBoolean(args[1]) ? "aktiviert" : "deaktiviert"));
                break;

            default:
                sender.sendMessage("§cUnknown subcommand: " + args[0]);
                break;
        }

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return null;
    }
}