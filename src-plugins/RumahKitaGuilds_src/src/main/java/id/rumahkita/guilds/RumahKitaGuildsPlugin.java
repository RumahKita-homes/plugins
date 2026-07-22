/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.GuildChatListener;
import id.rumahkita.guilds.GuildChatManager;
import id.rumahkita.guilds.GuildCommand;
import id.rumahkita.guilds.GuildGui;
import id.rumahkita.guilds.GuildHomeManager;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.GuildPlaceholderExpansion;
import id.rumahkita.guilds.GuildWarManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaGuildsPlugin
extends JavaPlugin {
    private GuildManager guildManager;
    private GuildHomeManager homeManager;
    private GuildChatManager chatManager;
    private GuildGui gui;
    private GuildWarManager warManager;
    private GuildPlaceholderExpansion placeholderExpansion;
    private EconomyManager economyManager;
    private GuildConfigGui configGui;
    private AdminDashboardGui adminDashboardGui;
    private GuildUpgradeGui upgradeGui;
    private GuildSettingsGui settingsGui;

    public void onEnable() {
        this.saveDefaultConfig();
        this.economyManager = new EconomyManager(this);
        this.guildManager = new GuildManager(this);
        this.guildManager.load();
        this.homeManager = new GuildHomeManager(this);
        this.chatManager = new GuildChatManager(this, this.guildManager);
        this.gui = new GuildGui(this, this.guildManager);
        this.warManager = new GuildWarManager(this, this.guildManager, this.economyManager);
        this.settingsGui = new GuildSettingsGui(this, this.guildManager);
        GuildCommand guildCommand = new GuildCommand(this, this.guildManager, this.homeManager, this.chatManager, this.gui, this.warManager, this.economyManager, this.settingsGui);
        this.getCommand("guild").setExecutor((CommandExecutor)guildCommand);
        this.getCommand("guild").setTabCompleter((TabCompleter)guildCommand);
        this.getCommand("guildchat").setExecutor((CommandExecutor)guildCommand);
        this.getCommand("guildchat").setTabCompleter((TabCompleter)guildCommand);
        Bukkit.getPluginManager().registerEvents((Listener)this.homeManager, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.gui, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new GuildChatListener(this, this.guildManager, this.chatManager), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.warManager, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new GuildPvPListener(this.guildManager), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new GuildClaimListener(this.guildManager), (Plugin)this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderExpansion = new GuildPlaceholderExpansion(this, this.guildManager);
            this.placeholderExpansion.register();
            this.getLogger().info("PlaceholderAPI hooked. Guild placeholders registered.");
        } else {
            this.getLogger().warning("PlaceholderAPI not found. Guild placeholders will not work until PlaceholderAPI is installed.");
        }
        this.configGui = new GuildConfigGui(this);
        this.adminDashboardGui = new AdminDashboardGui(this, this.guildManager, this.configGui);
        this.upgradeGui = new GuildUpgradeGui(this, this.guildManager);
        Bukkit.getPluginManager().registerEvents((Listener)this.configGui, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.adminDashboardGui, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.upgradeGui, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.settingsGui, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new GuildVaultListener(this.guildManager), (Plugin)this);
        new GuildAdminCommand(this, this.adminDashboardGui, this.guildManager);
        
        startUpkeepTask();
        
        this.getLogger().info("RumahKitaGuilds v2.3.3 GuildWar enabled.");
    }
    
    private void startUpkeepTask() {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!getConfig().getBoolean("settings.upkeep.enabled", true)) return;
                
                long lastUpkeep = getConfig().getLong("data.last-upkeep", 0);
                long now = System.currentTimeMillis();

                if (now - lastUpkeep >= 86400000) {
                    double amount = getConfig().getDouble("settings.upkeep.amount", 500.0);
                    int processed = 0;
                    
                    for (Guild guild : guildManager.getGuilds()) {
                        double oldBalance = guild.getBalance();
                        guild.setBalance(oldBalance - amount);
                        
                        if (oldBalance >= amount) {
                            guild.addLog("Paid daily upkeep tax: $" + economyManager.format(amount));
                        } else {
                            guild.addLog("Guild is in debt! Failed to fully pay daily upkeep of $" + economyManager.format(amount));
                        }
                        processed++;
                    }
                    
                    if (processed > 0) {
                        guildManager.save();
                    }
                    
                    getConfig().set("data.last-upkeep", now);
                    saveConfig();
                    getLogger().info("Processed daily upkeep tax for " + processed + " guilds.");
                }
            }
        }.runTaskTimer((Plugin)this, 1200L, 36000L); 
    }

    public void onDisable() {
        try {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                p.closeInventory();
            }
            org.bukkit.Bukkit.getServicesManager().unregisterAll((org.bukkit.plugin.Plugin)this);
            org.bukkit.Bukkit.getScheduler().cancelTasks((org.bukkit.plugin.Plugin)this);
            org.bukkit.event.HandlerList.unregisterAll((org.bukkit.plugin.Plugin)this);
        } catch (Exception ignored) {}

        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
        }
        Bukkit.getServicesManager().unregisterAll((Plugin)this);
        Bukkit.getScheduler().cancelTasks((Plugin)this);
        if (this.homeManager != null) {
            this.homeManager.cancelAllTeleports();
        }
        if (this.warManager != null) {
            this.warManager.shutdown();
        }
        if (this.guildManager != null) {
            this.guildManager.save();
        }
    }

    public void reloadAll() {
        this.reloadConfig();
        this.guildManager.load();
        if (this.warManager != null) {
            this.warManager.reload();
        }
    }
    public EconomyManager getEconomyManager() {
        return this.economyManager;
    }

    public GuildConfigGui getConfigGui() {
        return this.configGui;
    }

    public GuildUpgradeGui getUpgradeGui() {
        return this.upgradeGui;
    }
}

