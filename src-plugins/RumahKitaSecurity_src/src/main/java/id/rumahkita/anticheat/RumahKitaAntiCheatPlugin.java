/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.anticheat;

import id.rumahkita.anticheat.AntiCheatCommand;
import id.rumahkita.anticheat.AntiCheatListener;
import id.rumahkita.anticheat.ExemptManager;
import id.rumahkita.anticheat.LogManager;
import id.rumahkita.anticheat.Text;
import id.rumahkita.anticheat.ViolationTracker;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaAntiCheatPlugin
{
    private final id.rumahkita.security.RumahKitaSecurityPlugin plugin;

    public RumahKitaAntiCheatPlugin(id.rumahkita.security.RumahKitaSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    public org.bukkit.configuration.file.FileConfiguration getConfig() { return plugin.getConfig(); }
    public void saveConfig() { plugin.saveConfig(); }
    public void saveDefaultConfig() { plugin.saveDefaultConfig(); }
    public void reloadConfig() { plugin.reloadConfig(); }
    public java.util.logging.Logger getLogger() { return plugin.getLogger(); }
    public org.bukkit.Server getServer() { return plugin.getServer(); }
    public org.bukkit.command.PluginCommand getCommand(String name) { return plugin.getCommand(name); }
    public org.bukkit.plugin.java.JavaPlugin getPlugin() { return plugin; }
    public java.io.File getDataFolder() { return plugin.getDataFolder(); }

    private ExemptManager exemptManager;
    private ViolationTracker violationTracker;
    private LogManager logManager;

    public void onEnable() {
        plugin.saveDefaultConfig();
        this.exemptManager = new ExemptManager();
        this.violationTracker = new ViolationTracker();
        this.logManager = new LogManager(this);
        AntiCheatListener listener = new AntiCheatListener(this, this.exemptManager, this.violationTracker);
        Bukkit.getPluginManager().registerEvents((Listener)listener, this.plugin);
        listener.startAuditTask();
        AntiCheatCommand command = new AntiCheatCommand(this, this.exemptManager, this.violationTracker);
        plugin.getCommand("rkac").setExecutor((CommandExecutor)command);
        plugin.getCommand("rkac").setTabCompleter((TabCompleter)command);
        Bukkit.getScheduler().runTaskTimer(this.plugin, () -> this.exemptManager.cleanup(), 600L, 600L);
        plugin.getLogger().info("RumahKitaAntiCheat v1.2.0 enabled.");
    }

    public void onDisable() {
        plugin.getLogger().info("RumahKitaAntiCheat disabled.");
    }

    public boolean isEnabledInConfig() {
        return plugin.getConfig().getBoolean("settings.enabled", true);
    }

    public void handleViolation(Player player, String type, String detail, int vl) {
        String alert = plugin.getConfig().getString("settings.prefix", "&8[&cRumahKitaAC&8] ") + Text.replace(plugin.getConfig().getString("messages.alert"), "%player%", player.getName(), "%type%", type, "%detail%", detail, "%vl%", String.valueOf(vl));
        if (plugin.getConfig().getBoolean("actions.staff-alerts", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission(plugin.getConfig().getString("settings.notify-permission", "rumahkita.anticheat.notify"))) continue;
                Text.msg((CommandSender)online, alert);
            }
        }
        this.logManager.log(player.getName() + " detected=" + type + " VL=" + vl + " detail=" + detail + " ping=" + player.getPing() + " gm=" + String.valueOf(player.getGameMode()) + " loc=" + this.loc(player));
    }

    public void handleKick(Player player, String type) {
        String msg = plugin.getConfig().getString("settings.prefix", "&8[&cRumahKitaAC&8] ") + Text.replace(plugin.getConfig().getString("messages.kicked"), "%player%", player.getName(), "%type%", type);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(plugin.getConfig().getString("settings.notify-permission", "rumahkita.anticheat.notify"))) continue;
            Text.msg((CommandSender)online, msg);
        }
        this.logManager.log("KICK " + player.getName() + " reason=" + type + " loc=" + this.loc(player));
    }

    public void staffAlert(Player player, String type, String detail) {
        String alert = plugin.getConfig().getString("settings.prefix", "&8[&cRumahKitaAC&8] ") + Text.replace(plugin.getConfig().getString("messages.alert"), "%player%", player.getName(), "%type%", type, "%detail%", detail, "%vl%", "-");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(plugin.getConfig().getString("settings.notify-permission", "rumahkita.anticheat.notify"))) continue;
            Text.msg((CommandSender)online, alert);
        }
        this.logManager.log(player.getName() + " alert=" + type + " detail=" + detail + " loc=" + this.loc(player));
    }

    private String loc(Player p) {
        return p.getWorld().getName() + " " + String.format(Locale.US, "%.2f %.2f %.2f", p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ());
    }
}

