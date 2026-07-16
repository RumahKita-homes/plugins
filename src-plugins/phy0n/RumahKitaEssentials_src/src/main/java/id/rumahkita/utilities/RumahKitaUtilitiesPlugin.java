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
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaUtilitiesPlugin
{
    private SleepManager sleepManager;
    private CarryManager carryManager;
    private BansosManager bansosManager;
    private VanishManager vanishManager;

    public void onEnable() {
        plugin.saveDefaultConfig();
        this.saveResourceIfMissing("data.yml");
        this.sleepManager = new SleepManager(this);
        this.carryManager = new CarryManager(this);
        this.bansosManager = new BansosManager(this);
        this.vanishManager = new VanishManager(this);
        Bukkit.getPluginManager().registerEvents((Listener)this.sleepManager, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.carryManager, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)this.vanishManager, (Plugin)this);
        plugin.getCommand("rksleep").setExecutor((CommandExecutor)this.sleepManager);
        plugin.getCommand("rksleep").setTabCompleter((TabCompleter)this.sleepManager);
        plugin.getCommand("carry").setExecutor((CommandExecutor)this.carryManager);
        plugin.getCommand("carry").setTabCompleter((TabCompleter)this.carryManager);
        plugin.getCommand("rkcarry").setExecutor((CommandExecutor)this.carryManager);
        plugin.getCommand("rkcarry").setTabCompleter((TabCompleter)this.carryManager);
        plugin.getCommand("rkbansos").setExecutor((CommandExecutor)this.bansosManager);
        plugin.getCommand("rkbansos").setTabCompleter((TabCompleter)this.bansosManager);
        plugin.getCommand("rkvanish").setExecutor((CommandExecutor)this.vanishManager);
        plugin.getCommand("rkvanish").setTabCompleter((TabCompleter)this.vanishManager);
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
                plugin.getLogger().warning("Gagal membuat " + name + ": " + e.getMessage());
            }
        }
    }
}

