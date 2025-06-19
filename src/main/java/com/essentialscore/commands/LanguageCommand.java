package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command to manage language settings
 */
public class LanguageCommand implements CommandExecutor, TabCompleter {
    private final ApiCore apiCore;
    private final List<String> AVAILABLE_LANGUAGES = Arrays.asList("en_US", "de_DE");
    
    public LanguageCommand(ApiCore apiCore) {
        this.apiCore = apiCore;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show current language
            String currentLang = apiCore.getLanguageManager().getCurrentLanguage();
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fCurrent language: &e" + currentLang));
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fAvailable languages: &e" + String.join("&f, &e", AVAILABLE_LANGUAGES)));
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fUse &b/apicore language set <lang> &fto change the language."));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("set") || subCommand.equals("change")) {
            if (args.length < 2) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cPlease specify a language code."));
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fAvailable languages: &e" + String.join("&f, &e", AVAILABLE_LANGUAGES)));
                return true;
            }
            
            String langCode = args[1];
            
            if (!AVAILABLE_LANGUAGES.contains(langCode)) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cInvalid language code: &e" + langCode));
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fAvailable languages: &e" + String.join("&f, &e", AVAILABLE_LANGUAGES)));
                return true;
            }
            
            boolean success = apiCore.getLanguageManager().setLanguage(langCode);
            
            if (success) {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aLanguage changed to: &e" + langCode));
            } else {
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&cFailed to change language to: &e" + langCode));
            }
            
            return true;
        } else if (subCommand.equals("reload")) {
            apiCore.getLanguageManager().reload();
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&aLanguage files reloaded."));
            return true;        } else if (subCommand.equals("list")) {
            sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fAvailable languages:"));
            for (String lang : AVAILABLE_LANGUAGES) {
                String currentMarker = apiCore.getLanguageManager().getCurrentLanguage().equals(lang) ? " &a(current)" : "";
                sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&8- &e" + lang + currentMarker));
            }
            return true;
        }
        
        // Unknown subcommand, show help
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&fLanguage Command Help:"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/apicore language &8- &7Show current language"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/apicore language set <lang> &8- &7Change language"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/apicore language list &8- &7List available languages"));
        sender.sendMessage(apiCore.formatHex(apiCore.getMessagePrefix() + "&b/apicore language reload &8- &7Reload language files"));
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("set", "list", "reload");
            String prefix = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(prefix)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String prefix = args[1].toLowerCase();
            
            for (String lang : AVAILABLE_LANGUAGES) {
                if (lang.toLowerCase().startsWith(prefix)) {
                    completions.add(lang);
                }
            }
        }
        
        return completions;
    }
} 
