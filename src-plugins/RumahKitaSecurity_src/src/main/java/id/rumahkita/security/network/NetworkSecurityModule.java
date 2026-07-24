package id.rumahkita.security.network;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.io.*;
import java.net.*;
import id.rumahkita.security.RumahKitaSecurityPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class NetworkSecurityModule implements CommandExecutor, Listener {
    private final RumahKitaSecurityPlugin plugin;
    private final Set<String> blockedIps = new HashSet<>();
    private final Map<String, Long> vpnCache = new HashMap<>();
    
    public NetworkSecurityModule(RumahKitaSecurityPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        createDataConfig();
    }

    
    private File dataFile;
    private FileConfiguration dataConfig;

    private void createDataConfig() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("Could not create data.yml!"); }
        }
        dataConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveDataConfig() {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { dataConfig.save(dataFile); } catch (Exception e) { plugin.getLogger().severe("Could not save data.yml!"); }
        });
    }

    private void logModeration(String message) {
        try {
            File logFile = new File(plugin.getDataFolder(), "moderation.log");
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String logMsg = "[" + dtf.format(now) + "] " + message + "\n";
            java.nio.file.Files.write(logFile.toPath(), logMsg.getBytes(), java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write to moderation.log: " + e.getMessage());
        }
    }

    private FileConfiguration getConfig() { return plugin.getConfig(); }
    private void saveConfig() { plugin.saveConfig(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "checkip": return handleCheckIp(sender, args);
            case "checkalts": return handleCheckAlts(sender, args);
            case "allowalt": return handleAllowAlt(sender, args);
            case "blockalt": return handleBlockAlt(sender, args);
            case "unblockalt": return handleUnblockAlt(sender, args);
            case "setmainaccount": return handleSetMain(sender, args);
            case "blockip": return handleBlockIp(sender, args);
            case "unblockip": return handleUnblockIp(sender, args);
            case "vpn": return handleVpn(sender, args);
        }
        return false;
    }

    private String getMsg(String path, String def) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(path, def));
    }

    

    private boolean handleCheckIp(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            String offlineIp = dataConfig.getString("player_ips." + args[1]);
            if (offlineIp != null) {
                sender.sendMessage(ChatColor.GREEN + "IP " + args[1] + " (Offline): " + ChatColor.YELLOW + offlineIp);
                return true;
            }
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "IP " + target.getName() + " (Online): " + ChatColor.YELLOW + target.getAddress().getAddress().getHostAddress());
        return true;
    }

private boolean handleCheckAlts(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka checkalts <player>");
            return true;
        }
        
        String targetName = args[1];
        String targetIp = dataConfig.getString("player_ips." + targetName);
        
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            targetIp = targetPlayer.getAddress().getAddress().getHostAddress();
            targetName = targetPlayer.getName();
        }

        if (targetIp == null) {
            sender.sendMessage(ChatColor.RED + "No IP data found for player: " + targetName);
            return true;
        }

        List<String> alts = new ArrayList<>();
        String ipKey = targetIp.replace(".", "_");
        List<String> accounts = dataConfig.getStringList("ip_players." + ipKey);
        
        for (String name : accounts) {
            if (name.equalsIgnoreCase(targetName)) continue;
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) {
                alts.add(ChatColor.GREEN + name + ChatColor.GRAY + " (Online)");
            } else {
                alts.add(ChatColor.GRAY + name + " (Offline)");
            }
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
        String mainAcc = dataConfig.getString("main_accounts." + ipKey);
        if (mainAcc != null && mainAcc.equalsIgnoreCase(targetName)) {
            sender.sendMessage(ChatColor.AQUA + "Alt Accounts for " + ChatColor.YELLOW + ChatColor.BOLD + targetName + ChatColor.AQUA + " (IP: " + targetIp + ") " + ChatColor.GOLD + "[MAIN]");
        } else if (mainAcc != null) {
            sender.sendMessage(ChatColor.AQUA + "Alt Accounts for " + ChatColor.YELLOW + targetName + ChatColor.AQUA + " (IP: " + targetIp + ") | " + ChatColor.GOLD + "Main Account: " + mainAcc);
        } else {
            sender.sendMessage(ChatColor.AQUA + "Alt Accounts for " + ChatColor.YELLOW + targetName + ChatColor.AQUA + " (IP: " + targetIp + ")");
        }
        if (alts.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No alt accounts found on this IP.");
        } else {
            sender.sendMessage(ChatColor.WHITE + String.join(ChatColor.DARK_GRAY + ", ", alts));
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");

        return true;
    }

private boolean handleAllowAlt(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka allowalt <main account> <alt account>");
            return true;
        }
        String mainAcc = args[1];
        String newAlt = args[2];
        
        String ip = dataConfig.getString("player_ips." + mainAcc);
        if (ip != null) {
            dataConfig.set("player_ips." + newAlt, ip);
            String ipKey = ip.replace(".", "_");
            List<String> accounts = dataConfig.getStringList("ip_players." + ipKey);
            if (!accounts.contains(newAlt)) {
                accounts.add(newAlt);
                dataConfig.set("ip_players." + ipKey, accounts);
            }
            saveDataConfig();
            sender.sendMessage(ChatColor.GREEN + "Successfully granted alt bypass access!");
            sender.sendMessage(ChatColor.GRAY + "Akun " + ChatColor.YELLOW + newAlt + ChatColor.GRAY + " is now registered to the IP of " + ChatColor.YELLOW + mainAcc + ChatColor.GRAY + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "IP Data not found for account: " + mainAcc);
        }
        return true;
    }

private boolean handleBlockAlt(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka blockalt <main account> <alt account>");
            return true;
        }
        String mainAcc = args[1];
        String altAcc = args[2];
        
        List<String> blocked = dataConfig.getStringList("blocked_alts");
        if (!blocked.contains(altAcc.toLowerCase())) {
            blocked.add(altAcc.toLowerCase());
            dataConfig.set("blocked_alts", blocked);
            saveDataConfig();
        }
        
        sender.sendMessage(ChatColor.GREEN + "Successfully blocked alt!");
        sender.sendMessage(ChatColor.GRAY + "Akun " + ChatColor.RED + altAcc + ChatColor.GRAY + " will now be permanently blocked (Suspected as 3rd alt of " + ChatColor.YELLOW + mainAcc + ChatColor.GRAY + ").");
        
        logModeration("BLOCKALT: " + sender.getName() + " blocked alt " + altAcc + " (Main: " + mainAcc + ")");
        
        Player p = Bukkit.getPlayerExact(altAcc);
        if (p != null) {
            p.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&cLogin Failed!\n\n&fThis account is detected as your 3rd or more account.\n&7Maximum allowed is 2 accounts."));
        }
        
        return true;
    }

private boolean handleUnblockAlt(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka unblockalt <alt account>");
            return true;
        }
        String altAcc = args[1].toLowerCase();
        
        List<String> blocked = dataConfig.getStringList("blocked_alts");
        if (blocked.contains(altAcc)) {
            blocked.remove(altAcc);
            dataConfig.set("blocked_alts", blocked);
            saveDataConfig();
            sender.sendMessage(ChatColor.GREEN + "Successfully unblocked alt!");
            sender.sendMessage(ChatColor.GRAY + "Akun " + ChatColor.YELLOW + altAcc + ChatColor.GRAY + " has been removed from the blocklist and can now join.");
            logModeration("UNBLOCKALT: " + sender.getName() + " unblocked alt " + altAcc);
        } else {
            sender.sendMessage(ChatColor.RED + "Akun " + altAcc + " was not found in the blocklist.");
        }
        
        return true;
    }

private boolean handleSetMain(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka setmainaccount <account>");
            return true;
        }
        String targetName = args[1];
        String ip = dataConfig.getString("player_ips." + targetName);
        if (ip == null) {
            sender.sendMessage(ChatColor.RED + "IP Data not found for account: " + targetName);
            return true;
        }
        
        String ipKey = ip.replace(".", "_");
        dataConfig.set("main_accounts." + ipKey, targetName);
        saveDataConfig();
        
        sender.sendMessage(ChatColor.GREEN + "Successfully set " + ChatColor.YELLOW + targetName + ChatColor.GREEN + " as the Main Account for IP " + ip);
        logModeration("SETMAIN: " + sender.getName() + " set " + targetName + " as main account for " + ip);
        return true;
    }

private boolean handleBlockIp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka blockip <ip>");
            return true;
        }
        String ip = args[1];
        List<String> blockedIps = dataConfig.getStringList("blocked_ips");
        if (!blockedIps.contains(ip)) {
            blockedIps.add(ip);
            dataConfig.set("blocked_ips", blockedIps);
            saveDataConfig();
            sender.sendMessage(ChatColor.GREEN + "IP " + ip + " successfully blocked.");
            logModeration("BLOCKIP: " + sender.getName() + " blocked IP " + ip);
        } else {
            sender.sendMessage(ChatColor.RED + "The IP is already in the blocklist.");
        }
        return true;
    }

private boolean handleUnblockIp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka unblockip <ip>");
            return true;
        }
        String ip = args[1];
        List<String> blockedIps = dataConfig.getStringList("blocked_ips");
        if (blockedIps.contains(ip)) {
            blockedIps.remove(ip);
            dataConfig.set("blocked_ips", blockedIps);
            saveDataConfig();
            sender.sendMessage(ChatColor.GREEN + "IP " + ip + " successfully unblocked.");
            logModeration("UNBLOCKIP: " + sender.getName() + " unblocked IP " + ip);
        } else {
            sender.sendMessage(ChatColor.RED + "The IP is not in the blocklist.");
        }
        return true;
    }

private boolean handleVpn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka vpn <on/off/allow/remove> [ip]");
            return true;
        }
        String action = args[1].toLowerCase();
        
        if (action.equals("on")) {
            getConfig().set("settings.anti-vpn.enabled", true);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Anti-VPN system has been ENABLED!");
            logModeration("VPN TOGGLE: " + sender.getName() + " enabled Anti-VPN");
            return true;
        } else if (action.equals("off")) {
            getConfig().set("settings.anti-vpn.enabled", false);
            saveConfig();
            sender.sendMessage(ChatColor.RED + "Anti-VPN system has been DISABLED!");
            logModeration("VPN TOGGLE: " + sender.getName() + " disabled Anti-VPN");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka vpn <allow/remove> <ip>");
            return true;
        }
        
        String ip = args[2];
        List<String> cleanIps = dataConfig.getStringList("vpn_cache.clean_ips");
        
        if (action.equals("allow")) {
            if (!cleanIps.contains(ip)) {
                cleanIps.add(ip);
                dataConfig.set("vpn_cache.clean_ips", cleanIps);
                saveDataConfig();
                sender.sendMessage(ChatColor.GREEN + "IP " + ip + " added to VPN whitelist.");
                logModeration("VPN ALLOW: " + sender.getName() + " whitelisted IP " + ip);
            } else {
                sender.sendMessage(ChatColor.RED + "The IP is already in the VPN whitelist.");
            }
        } else if (action.equals("remove")) {
            if (cleanIps.contains(ip)) {
                cleanIps.remove(ip);
                dataConfig.set("vpn_cache.clean_ips", cleanIps);
                saveDataConfig();
                sender.sendMessage(ChatColor.GREEN + "IP " + ip + " removed from VPN whitelist.");
                logModeration("VPN REMOVE: " + sender.getName() + " removed IP " + ip + " from VPN whitelist");
            } else {
                sender.sendMessage(ChatColor.RED + "The IP is not in the VPN whitelist.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown action. Use: on, off, allow, or remove.");
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(org.bukkit.event.player.AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        
        List<String> blockedIps = dataConfig.getStringList("blocked_ips");
        if (blockedIps.contains(ip)) {
            event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED, 
                ChatColor.translateAlternateColorCodes('&', "&cConnection Refused!\n\n&fYour IP Address has been permanently blocked from this server."));
            return;
        }

        if (!getConfig().getBoolean("settings.anti-vpn.enabled", true)) return;
        if (ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("10.")) return;
        
        List<String> cleanIps = dataConfig.getStringList("vpn_cache.clean_ips");
        if (cleanIps.contains(ip)) return;
        
        List<String> proxyIps = dataConfig.getStringList("vpn_cache.proxy_ips");
        String kickMsg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.anti-vpn.kick-message", "&cConnection Refused!\n\n&fSystem detected VPN / Proxy usage."));
        
        if (proxyIps.contains(ip)) {
            event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMsg);
            return;
        }

        try {
            java.net.URL url = new java.net.URL("http://ip-api.com/json/" + ip + "?fields=proxy,hosting");
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            
            String json = content.toString();
            if (json.contains("\"proxy\":true") || json.contains("\"hosting\":true")) {
                event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMsg);
                plugin.getLogger().warning("Anti-VPN memblokir login dari: " + event.getName() + " (" + ip + ")");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    logModeration("ANTI-VPN: Blocked " + event.getName() + " (" + ip + ")");
                    List<String> bads = dataConfig.getStringList("vpn_cache.proxy_ips");
                    if (!bads.contains(ip)) {
                        bads.add(ip);
                        dataConfig.set("vpn_cache.proxy_ips", bads);
                        saveDataConfig();
                    }
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<String> ips = dataConfig.getStringList("vpn_cache.clean_ips");
                    if (!ips.contains(ip)) {
                        ips.add(ip);
                        dataConfig.set("vpn_cache.clean_ips", ips);
                        saveDataConfig();
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Anti-VPN API Timeout for IP: " + ip);
        }
    }

    
}
