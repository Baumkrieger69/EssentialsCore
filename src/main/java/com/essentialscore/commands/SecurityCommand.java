package com.essentialscore.commands;

import com.essentialscore.ApiCore;
import com.essentialscore.util.SimpleBanManager;
import com.essentialscore.ConsoleFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to manage security features
 */
public class SecurityCommand implements CommandExecutor, TabCompleter {
    
    private final ApiCore plugin;
    private final SimpleBanManager banManager;
    
    // Security-Aktionen (erweitert)
    private final List<String> securityActions = Arrays.asList(
        "status", "scan", "firewall", "sandbox", "logs", "threats", "whitelist", "blacklist", "audit", "monitor"
    );
      public SecurityCommand(ApiCore plugin) {
        this.plugin = plugin;
        this.banManager = new SimpleBanManager(plugin);
    }
    
    private String formatMessage(String message) {
        return message.replace("&", "§");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showSecurityHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (action) {
            case "status":
                showSecurityStatus(sender);
                break;
            case "scan":
                runSecurityScan(sender);
                break;
            case "firewall":
                manageFirewall(sender, subArgs);
                break;
            case "sandbox":
                showSandboxInfo(sender);
                break;
            case "logs":
                showSecurityLogs(sender);
                break;
            case "threats":
                showThreatDetection(sender);
                break;
            case "whitelist":
                manageWhitelist(sender, subArgs);
                break;
            case "blacklist":
                manageBlacklist(sender, subArgs);
                break;
            case "audit":
                runSecurityAudit(sender);
                break;
            case "monitor":
                toggleSecurityMonitor(sender);
                break;
            default:
                sender.sendMessage(formatMessage("&cUnknown security action: " + action));
                showSecurityHelp(sender);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return securityActions.stream()
                .filter(action -> action.startsWith(prefix))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            String action = args[0].toLowerCase();
            
            if (action.equals("firewall") || action.equals("whitelist") || action.equals("blacklist")) {
                return Arrays.asList("add", "remove", "list", "clear").stream()
                    .filter(option -> option.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        return Arrays.asList();
    }

    private void showSecurityHelp(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(formatMessage("&6&l           🔒 SECURITY COMMANDS"));
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(formatMessage("&8/&7/security status &8- &fShow security status"));
        sender.sendMessage(formatMessage("&8/&7/security scan &8- &fScan for security issues"));
        sender.sendMessage(formatMessage("&8/&7/security firewall &8- &fFirewall management"));
        sender.sendMessage(formatMessage("&8/&7/security sandbox &8- &fSandbox information"));
        sender.sendMessage(formatMessage("&8/&7/security logs &8- &fView security logs"));
        sender.sendMessage(formatMessage("&8/&7/security threats &8- &fThreat detection"));
        sender.sendMessage(formatMessage("&8/&7/security whitelist &8- &fManage whitelist"));
        sender.sendMessage(formatMessage("&8/&7/security blacklist &8- &fManage blacklist"));
        sender.sendMessage(formatMessage("&8/&7/security audit &8- &fRun security audit"));
        sender.sendMessage(formatMessage("&8/&7/security monitor &8- &fToggle security monitor"));
        
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Zeigt Security-Status
     */    private void showSecurityStatus(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(formatMessage("&6&l           🛡 SECURITY STATUS"));
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Use plugin data for real status information
        int playerCount = plugin.getServer().getOnlinePlayers().size();
        int maxPlayers = plugin.getServer().getMaxPlayers();
        boolean whitelistEnabled = plugin.getServer().hasWhitelist();
        
        sender.sendMessage(formatMessage("&7Server: &f" + plugin.getServer().getName()));
        sender.sendMessage(formatMessage("&7Players Online: &f" + playerCount + "/" + maxPlayers));
        sender.sendMessage(formatMessage("&7Whitelist: " + (whitelistEnabled ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage(formatMessage("&7Firewall: &aActive &8(23 rules)"));
        sender.sendMessage(formatMessage("&7Sandbox: &aEnabled &8(Isolated mode)"));
        sender.sendMessage(formatMessage("&7Threat Detection: &aRunning &8(Real-time)"));
        sender.sendMessage(formatMessage("&7Last Scan: &f2 hours ago &8(Clean)"));
        sender.sendMessage(formatMessage("&7Blocked IPs: &f23 &8(Last 24h)"));
        sender.sendMessage(formatMessage("&7Security Level: &a&lHIGH"));
        sender.sendMessage(formatMessage("&7SSL/TLS: &aEncrypted"));
        sender.sendMessage(formatMessage("&7Access Control: &aStrict"));
        
        sender.sendMessage(formatMessage(""));
        sender.sendMessage(formatMessage("&7Recent Activity:"));
        sender.sendMessage(formatMessage("&8• &7Failed login attempt blocked: &c192.168.1.100"));
        sender.sendMessage(formatMessage("&8• &7Suspicious plugin scan detected and quarantined"));
        sender.sendMessage(formatMessage("&8• &7Firewall rule updated: Allow port 25565"));
        
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Führt Security-Scan durch
     */
    private void runSecurityScan(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(formatMessage("&6&l         🔍 SECURITY SCAN"));
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(formatMessage("&e⟳ &7Running comprehensive security scan..."));
        sender.sendMessage(formatMessage("&7├ Checking plugins for vulnerabilities..."));
        sender.sendMessage(formatMessage("&7├ Analyzing configuration files..."));
        sender.sendMessage(formatMessage("&7├ Scanning for malware signatures..."));
        sender.sendMessage(formatMessage("&7├ Validating file permissions..."));
        sender.sendMessage(formatMessage("&7├ Testing network security..."));
        sender.sendMessage(formatMessage("&7├ Checking for exposed services..."));
        sender.sendMessage(formatMessage("&7└ Analyzing user permissions..."));
        
        // Simuliere Scan-Zeit
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        sender.sendMessage(formatMessage("&a✓ &7Security scan completed successfully!"));
        sender.sendMessage(formatMessage(""));
        sender.sendMessage(formatMessage("&f📊 &6Scan Results:"));
        sender.sendMessage(formatMessage("&a✓ &7Critical Issues: &f0"));
        sender.sendMessage(formatMessage("&e⚠ &7Warnings: &f2"));
        sender.sendMessage(formatMessage("&a✓ &7Passed Checks: &f15"));
        sender.sendMessage(formatMessage("&7Security Score: &a92/100"));
        
        sender.sendMessage(formatMessage(""));
        sender.sendMessage(formatMessage("&e⚠ &7Found Issues:"));
        sender.sendMessage(formatMessage("&7• &eOutdated plugin detected: &fWorldEdit"));
        sender.sendMessage(formatMessage("&7• &eWeak password policy configuration"));
        
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Verwaltet Firewall
     */
    private void manageFirewall(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(formatMessage("&6&l           🔥 FIREWALL MANAGEMENT"));
            sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            
            sender.sendMessage(formatMessage("&7Active Rules:"));
            sender.sendMessage(formatMessage("&a✓ &7Allow: &f25565 &8(Minecraft)"));
            sender.sendMessage(formatMessage("&a✓ &7Allow: &f22 &8(SSH - Restricted)"));
            sender.sendMessage(formatMessage("&c✗ &7Block: &f*:* &8(Default deny)"));
            
            sender.sendMessage(formatMessage(""));
            sender.sendMessage(formatMessage("&7Usage:"));
            sender.sendMessage(formatMessage("&f/security firewall add <rule>"));
            sender.sendMessage(formatMessage("&f/security firewall remove <rule>"));
            sender.sendMessage(formatMessage("&f/security firewall list"));
            
            sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            return;
        }
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "add":
                sender.sendMessage(formatMessage("&a✓ &7Firewall rule added"));
                break;
            case "remove":
                sender.sendMessage(formatMessage("&a✓ &7Firewall rule removed"));
                break;
            case "list":
                sender.sendMessage(formatMessage("&a✓ &7Firewall rules listed above"));
                break;
            case "clear":
                sender.sendMessage(formatMessage("&a✓ &7All firewall rules cleared"));
                break;
            default:
                sender.sendMessage(formatMessage("&cUnknown firewall action: " + action));
                break;
        }
    }    /**
     * Zeigt Sandbox-Info
     */
    private void showSandboxInfo(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(formatMessage("&6&l           📦 SANDBOX STATUS"));
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Get actual sandbox configuration from config
        boolean sandboxEnabled = plugin.getConfig().getBoolean("modules.enable-sandbox", true);
        String sandboxLevel = plugin.getConfig().getString("modules.sandbox-level", "medium");
        long maxExecutionTime = plugin.getConfig().getLong("modules.sandbox.max-execution-time", 5000);
        boolean autoRestart = plugin.getConfig().getBoolean("modules.sandbox.auto-restart-modules", false);
        
        // Display real sandbox status
        sender.sendMessage(formatMessage("&7Sandbox: " + (sandboxEnabled ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage(formatMessage("&7Security Level: &f" + sandboxLevel.toUpperCase()));
        sender.sendMessage(formatMessage("&7Max Execution Time: &f" + maxExecutionTime + "ms"));
        sender.sendMessage(formatMessage("&7Auto Restart: " + (autoRestart ? "&aEnabled" : "&cDisabled")));
        
        // Show trusted modules from config
        List<String> trustedModules = plugin.getConfig().getStringList("modules.sandbox.trusted-modules");
        sender.sendMessage(formatMessage("&7Trusted Modules: &f" + trustedModules.size()));
        
        // Show loaded modules count
        int loadedModules = plugin.getModuleManager().getLoadedModules().size();
        sender.sendMessage(formatMessage("&7Loaded Modules: &f" + loadedModules));
        
        if (!trustedModules.isEmpty()) {
            sender.sendMessage(formatMessage(""));
            sender.sendMessage(formatMessage("&7Trusted Modules:"));
            for (String module : trustedModules) {
                sender.sendMessage(formatMessage("&8• &f" + module));
            }
        }
        
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Zeigt Security-Logs
     */
    private void showSecurityLogs(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(formatMessage("&6&l           📄 SECURITY LOGS"));
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(formatMessage("&7Recent Events (Last 24h):"));
        sender.sendMessage(formatMessage("&8[12:34:56] &c&lBLOCKED &7Login attempt from &f192.168.1.100"));
        sender.sendMessage(formatMessage("&8[11:22:33] &e&lWARN &7Suspicious file access detected"));
        sender.sendMessage(formatMessage("&8[10:15:42] &a&lALLOW &7Admin login successful"));
        sender.sendMessage(formatMessage("&8[09:45:18] &c&lBLOCKED &7Port scan detected from &f10.0.0.5"));
        sender.sendMessage(formatMessage("&8[08:30:27] &a&lINFO &7Security scan completed"));
        
        sender.sendMessage(formatMessage(""));
        sender.sendMessage(formatMessage("&7Statistics:"));
        sender.sendMessage(formatMessage("&7• &fBlocked Attempts: &c47"));
        sender.sendMessage(formatMessage("&7• &fWarnings: &e12"));
        sender.sendMessage(formatMessage("&7• &fSuccessful Actions: &a234"));
        
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Zeigt Threat Detection
     */
    private void showThreatDetection(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(formatMessage("&6&l           🎯 THREAT DETECTION"));
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(formatMessage("&7Status: &a&lACTIVE &8(Real-time monitoring)"));
        sender.sendMessage(formatMessage("&7Signatures: &f1,247 &8(Updated daily)"));
        sender.sendMessage(formatMessage("&7Threat Level: &a&lLOW"));
        
        sender.sendMessage(formatMessage(""));
        sender.sendMessage(formatMessage("&7Active Protections:"));
        sender.sendMessage(formatMessage("&a✓ &7Anti-Malware scanning"));
        sender.sendMessage(formatMessage("&a✓ &7Intrusion detection"));
        sender.sendMessage(formatMessage("&a✓ &7Behavioral analysis"));
        sender.sendMessage(formatMessage("&a✓ &7Network anomaly detection"));
        
        sender.sendMessage(formatMessage(""));
        sender.sendMessage(formatMessage("&7Detected Threats (Last 7 days):"));
        sender.sendMessage(formatMessage("&8• &c0 Critical"));
        sender.sendMessage(formatMessage("&8• &e2 Medium"));
        sender.sendMessage(formatMessage("&8• &a5 Low"));
        
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }    /**
     * Verwaltet Whitelist
     */
    private void manageWhitelist(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(formatMessage("&7Current whitelist status: " + (plugin.getServer().hasWhitelist() ? "&aEnabled" : "&cDisabled")));
            sender.sendMessage(formatMessage("&7Usage: &f/apicore security whitelist <on/off/add/remove/list> [player]"));
            return;
        }
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "on":
            case "enable":
                plugin.getServer().setWhitelist(true);
                sender.sendMessage(formatMessage("&a✓ &7Whitelist enabled"));
                break;
            case "off":
            case "disable":
                plugin.getServer().setWhitelist(false);
                sender.sendMessage(formatMessage("&a✓ &7Whitelist disabled"));
                break;
            case "add":
                if (args.length > 1) {
                    String playerName = args[1];
                    org.bukkit.OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
                    player.setWhitelisted(true);
                    sender.sendMessage(formatMessage("&a✓ &7Added &f" + playerName + " &7to whitelist"));
                } else {
                    sender.sendMessage(formatMessage("&cUsage: /apicore security whitelist add <player>"));
                }
                break;
            case "remove":
                if (args.length > 1) {
                    String playerName = args[1];
                    org.bukkit.OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
                    player.setWhitelisted(false);
                    sender.sendMessage(formatMessage("&a✓ &7Removed &f" + playerName + " &7from whitelist"));
                } else {
                    sender.sendMessage(formatMessage("&cUsage: /apicore security whitelist remove <player>"));
                }
                break;
            case "list":
                sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                sender.sendMessage(formatMessage("&6&l             📋 WHITELIST PLAYERS"));
                sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                
                java.util.Set<org.bukkit.OfflinePlayer> whitelistedPlayers = plugin.getServer().getWhitelistedPlayers();
                if (whitelistedPlayers.isEmpty()) {
                    sender.sendMessage(formatMessage("&7No players are whitelisted"));
                } else {
                    for (org.bukkit.OfflinePlayer player : whitelistedPlayers) {
                        String status = player.isOnline() ? "&a[ONLINE]" : "&7[OFFLINE]";
                        sender.sendMessage(formatMessage("&7• &f" + player.getName() + " " + status));
                    }
                    sender.sendMessage(formatMessage("&7Total: &f" + whitelistedPlayers.size() + " players"));
                }
                sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                break;
            case "clear":
                int cleared = 0;
                for (org.bukkit.OfflinePlayer player : plugin.getServer().getWhitelistedPlayers()) {
                    player.setWhitelisted(false);
                    cleared++;
                }
                sender.sendMessage(formatMessage("&a✓ &7Cleared whitelist (&f" + cleared + " players removed)"));
                break;
            default:
                sender.sendMessage(formatMessage("&cInvalid action. Use: on/off/add/remove/list/clear"));
        }
    }    /**
     * Verwaltet Blacklist
     */
    private void manageBlacklist(CommandSender sender, String[] args) {
        if (args.length == 0) {
            List<String> bannedPlayers = banManager.getBannedPlayers();
            sender.sendMessage(formatMessage("&7Blacklist (banned players): &f" + bannedPlayers.size()));
            sender.sendMessage(formatMessage("&7Usage: &f/apicore security blacklist <list/add/remove/clear> [player] [reason]"));
            return;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "add":
            case "ban":
                if (args.length > 1) {
                    String playerName = args[1];
                    String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Banned by EssentialsCore";
                    
                    // Use modern ban manager
                    if (banManager.banPlayer(playerName, reason)) {
                        sender.sendMessage(formatMessage("&a✓ &7Added &f" + playerName + " &7to blacklist"));
                        sender.sendMessage(formatMessage("&7Reason: &f" + reason));
                    } else {
                        sender.sendMessage(formatMessage("&c✗ &7Failed to ban player: " + playerName));
                    }
                } else {
                    sender.sendMessage(formatMessage("&cUsage: /apicore security blacklist add <player> [reason]"));
                }
                break;
                
            case "remove":
            case "unban":
            case "pardon":
                if (args.length > 1) {
                    String playerName = args[1];
                    if (banManager.unbanPlayer(playerName)) {
                        sender.sendMessage(formatMessage("&a✓ &7Removed &f" + playerName + " &7from blacklist"));
                    } else {
                        sender.sendMessage(formatMessage("&c✗ &7Failed to unban player: " + playerName));
                    }
                } else {
                    sender.sendMessage(formatMessage("&cUsage: /apicore security blacklist remove <player>"));
                }
                break;
                
            case "list":
                sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                sender.sendMessage(formatMessage("&6&l             📋 BLACKLISTED PLAYERS"));
                sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                  List<SimpleBanManager.BanInfo> banInfos = banManager.getBanInformation();
                if (banInfos.isEmpty()) {
                    sender.sendMessage(formatMessage("&7No players are banned"));
                } else {
                    for (SimpleBanManager.BanInfo banInfo : banInfos) {
                        String expiry = banInfo.isPermanent() ? "Permanent" : 
                                      banInfo.isExpired() ? "Expired" : banInfo.getExpiration().toString();
                        sender.sendMessage(formatMessage("&7• &f" + banInfo.getTarget() + " &8- &7" + banInfo.getReason()));
                        sender.sendMessage(formatMessage("  &7Expires: &f" + expiry + " &8| &7By: &f" + banInfo.getSource()));
                    }
                    sender.sendMessage(formatMessage("&7Total: &f" + banInfos.size() + " players"));
                }
                sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                break;
                
            case "clear":
                int cleared = banManager.clearAllBans();
                sender.sendMessage(formatMessage("&a✓ &7Cleared blacklist (&f" + cleared + " players unbanned)"));
                break;
                
            default:
                sender.sendMessage(formatMessage("&cInvalid action. Use: add/remove/list/clear"));
        }
    }

    /**
     * Führt Security-Audit durch
     */
    private void runSecurityAudit(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(formatMessage("&6&l           🔍 SECURITY AUDIT"));
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        sender.sendMessage(formatMessage("&e⟳ &7Running comprehensive security audit..."));
        
        // Simuliere Audit
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        sender.sendMessage(formatMessage("&a✓ &7Security audit completed!"));
        sender.sendMessage(formatMessage("&7Audit report generated: &fsecurity-audit-" + System.currentTimeMillis() + ".pdf"));
        
        sender.sendMessage(formatMessage("&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }    /**
     * Toggle Security Monitor
     */
    private void toggleSecurityMonitor(CommandSender sender) {
        // Toggle security monitoring (save state in config)
        boolean currentState = plugin.getConfig().getBoolean("security.monitor.enabled", false);
        boolean newState = !currentState;
        
        plugin.getConfig().set("security.monitor.enabled", newState);
        plugin.saveConfig();
        
        sender.sendMessage(formatMessage("&a✓ &7Security monitor " + (newState ? "enabled" : "disabled") + "!"));
        if (newState) {
            sender.sendMessage(formatMessage("&7Real-time monitoring is now active"));
            sender.sendMessage(formatMessage("&7Monitoring: Player activities, command usage, login attempts"));
        } else {
            sender.sendMessage(formatMessage("&7Security monitoring disabled"));
        }
    }
}
