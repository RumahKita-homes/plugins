package id.rumahkita.security;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RksCommand implements TabExecutor {
    private final RumahKitaSecurityPlugin plugin;
    public RksCommand(RumahKitaSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkita.security.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&bRumahKita Security Admin"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/rks ac/xray &7- AntiCheat/AntiXray admin"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/rks ban/unban &7- Security bans"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/rks ramguard &7- RAM Guard admin"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/rks spec &7- Ore spectator mode"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "ac":
                if (plugin.getAntiCheat() != null && plugin.getAntiCheat().getCommandExecutor() != null) {
                    return plugin.getAntiCheat().getCommandExecutor().onCommand(sender, command, "rkac", subArgs);
                }
                break;
            case "xray":
                if (plugin.getAntiXray() != null) {
                    return plugin.getAntiXray().onCommand(sender, command, "rkxray", subArgs);
                }
                break;
            case "sec":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onCommand(sender, command, "rksec", subArgs);
                }
                break;
            case "ban":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onCommand(sender, command, "rkban", subArgs);
                }
                break;
            case "tempban":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onCommand(sender, command, "rktempban", subArgs);
                }
                break;
            case "ipban":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onCommand(sender, command, "rkipban", subArgs);
                }
                break;
            case "unban":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onCommand(sender, command, "rkunban", subArgs);
                }
                break;
            case "ramguard":
                if (plugin.getRamGuard() != null) {
                    return plugin.getRamGuard().onCommand(sender, command, "ramguard", subArgs);
                }
                break;
            case "checkip":
            case "checkalts":
            case "allowalt":
            case "blockalt":
            case "unblockalt":
            case "setmainaccount":
            case "blockip":
            case "unblockip":
            case "vpn":
                if (plugin.getNetworkSecurity() != null) {
                    return plugin.getNetworkSecurity().onCommand(sender, command, label, args);
                }
                break;
            case "spec":
                if (plugin.getOreSpectator() != null) {
                    return plugin.getOreSpectator().onCommand(sender, command, "spec2", subArgs);
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("ac", "xray", "sec", "ban", "ipban", "unban", "ramguard", "spec");
            List<String> matches = new ArrayList<>();
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    matches.add(sub);
                }
            }
            return matches;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "ac":
                if (plugin.getAntiCheat() != null && plugin.getAntiCheat().getCommandExecutor() != null) {
                    return plugin.getAntiCheat().getCommandExecutor().onTabComplete(sender, command, "rkac", subArgs);
                }
                break;
            case "xray":
                if (plugin.getAntiXray() != null) {
                    return plugin.getAntiXray().onTabComplete(sender, command, "rkxray", subArgs);
                }
                break;
            case "sec":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onTabComplete(sender, command, "rksec", subArgs);
                }
                break;
            case "ban":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onTabComplete(sender, command, "rkban", subArgs);
                }
                break;
            case "ipban":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onTabComplete(sender, command, "rkipban", subArgs);
                }
                break;
            case "unban":
                if (plugin.getSecurityBan() != null) {
                    return plugin.getSecurityBan().onTabComplete(sender, command, "rkunban", subArgs);
                }
                break;
            case "ramguard":
                if (plugin.getRamGuard() != null) {
                    return plugin.getRamGuard().onTabComplete(sender, command, "ramguard", subArgs);
                }
                break;
            case "spec":
                if (plugin.getOreSpectator() != null) {
                    return plugin.getOreSpectator().onTabComplete(sender, command, "spec2", subArgs);
                }
                break;
        }
        return new ArrayList<>();
    }
}
