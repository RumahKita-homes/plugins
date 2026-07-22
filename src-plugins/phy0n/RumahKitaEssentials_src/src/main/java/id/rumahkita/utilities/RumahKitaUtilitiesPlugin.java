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
package id.rumahkita.utilities;

import id.rumahkita.utilities.BansosManager;
import id.rumahkita.utilities.CarryManager;
import id.rumahkita.utilities.SleepManager;
import id.rumahkita.utilities.VanishManager;
import id.rumahkita.utilities.InfoManager;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaUtilitiesPlugin
{
    private final id.rumahkita.essentials.RumahKitaEssentialsPlugin plugin;

    public RumahKitaUtilitiesPlugin(id.rumahkita.essentials.RumahKitaEssentialsPlugin plugin) {
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
    public SleepManager sleepManager;
    private CarryManager carryManager;
    public BansosManager bansosManager;
    private VanishManager vanishManager;
    private InfoManager infoManager;
    
    public VanishManager getVanishManager() {
        return this.vanishManager;
    }

    public void onEnable() {
        plugin.saveDefaultConfig();
        this.saveResourceIfMissing("data.yml");
        this.sleepManager = new SleepManager(this);
        this.carryManager = new CarryManager(this);
        this.bansosManager = new BansosManager(this);
        this.vanishManager = new VanishManager(this);
        this.infoManager = new InfoManager(this);
        Bukkit.getPluginManager().registerEvents((Listener)this.sleepManager, plugin);
        Bukkit.getPluginManager().registerEvents((Listener)this.carryManager, plugin);
        Bukkit.getPluginManager().registerEvents((Listener)this.vanishManager, plugin);
        Bukkit.getPluginManager().registerEvents(new ServerLinksManager(this), plugin);
        StatsCommand statsCommand = new StatsCommand(this);
        Bukkit.getPluginManager().registerEvents(statsCommand, plugin);
        
        PlayerSettingsGui settingsGui = new PlayerSettingsGui();
        Bukkit.getPluginManager().registerEvents(settingsGui, plugin);
        
        plugin.getCommand("carry").setExecutor((CommandExecutor)this.carryManager);
        plugin.getCommand("carry").setTabCompleter((TabCompleter)this.carryManager);
        plugin.getCommand("rkcarry").setExecutor((CommandExecutor)this.carryManager);
        plugin.getCommand("rkcarry").setTabCompleter((TabCompleter)this.carryManager);
        plugin.getCommand("ping").setExecutor(this.infoManager);
        plugin.getCommand("tps").setExecutor(this.infoManager);
        
        if (plugin.getCommand("online") != null) {
            plugin.getCommand("online").setExecutor(new OnlineCommand(this));
        }
        
        if (plugin.getCommand("stat") != null) {
            plugin.getCommand("stat").setExecutor(statsCommand);
        }
        
        if (plugin.getCommand("settings") != null) {
            plugin.getCommand("settings").setExecutor(new SettingsCommand(settingsGui));
        }
        
        if (plugin.getCommand("trash") != null) {
            plugin.getCommand("trash").setExecutor(new TrashCommand());
        }
        
        this.bansosManager.startScheduler();
        plugin.getLogger().info("RumahKitaUtilities v1.2.0 enabled.");
    }

    public void onDisable() {
        if (this.carryManager != null) {
            this.carryManager.dropAll();
        }
        if (this.vanishManager != null) {
            this.vanishManager.showAllOnDisable();
        }
        if (this.bansosManager != null) {
            this.bansosManager.saveData();
        }
        if (this.vanishManager != null) {
            this.vanishManager.saveData();
        }
    }

    public void reloadAll() {
        plugin.reloadConfig();
        if (this.bansosManager != null) {
            this.bansosManager.reloadData();
        }
        if (this.vanishManager != null) {
            this.vanishManager.reloadData();
        }
    }

    private void saveResourceIfMissing(String name) {
        File file;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!(file = new File(plugin.getDataFolder(), name)).exists()) {
            try {
                file.createNewFile();
            }
            catch (Exception e) {
                plugin.getLogger().warning("Failed to create " + name + ": " + e.getMessage());
            }
        }
    }
}

