/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.fishing;

import id.rumahkita.fishing.command.FishAdminCommand;
import id.rumahkita.fishing.command.FishCommand;
import id.rumahkita.fishing.listener.FishingListener;
import id.rumahkita.fishing.listener.GuiListener;
import id.rumahkita.fishing.listener.PlayerListener;
import id.rumahkita.fishing.manager.AntiAfkManager;
import id.rumahkita.fishing.manager.DailyLimitManager;
import id.rumahkita.fishing.manager.EconomyManager;
import id.rumahkita.fishing.manager.FishItemFactory;
import id.rumahkita.fishing.manager.FishManager;
import id.rumahkita.fishing.manager.LeaderboardManager;
import id.rumahkita.fishing.manager.PlayerDataManager;
import id.rumahkita.fishing.manager.SellManager;
import id.rumahkita.fishing.menu.FishMarketMenu;
import id.rumahkita.fishing.util.ConfigFile;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaFishingPlugin
{
    public org.bukkit.configuration.file.FileConfiguration getConfig() { return plugin.getConfig(); }
    public void saveConfig() { plugin.saveConfig(); }
    public void saveDefaultConfig() { plugin.saveDefaultConfig(); }
    public void reloadConfig() { plugin.reloadConfig(); }
    public java.util.logging.Logger getLogger() { return plugin.getLogger(); }
    public org.bukkit.Server getServer() { return plugin.getServer(); }
    public org.bukkit.command.PluginCommand getCommand(String name) { return plugin.getCommand(name); }
    public org.bukkit.plugin.java.JavaPlugin getPlugin() { return plugin; }
    public java.io.File getDataFolder() { return plugin.getDataFolder(); }

    
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    public RumahKitaFishingPlugin(org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private ConfigFile fishesConfig;
    private ConfigFile messagesConfig;
    private ConfigFile guiConfig;
    private FishManager fishManager;
    private FishItemFactory fishItemFactory;
    private EconomyManager economyManager;
    private PlayerDataManager playerDataManager;
    private DailyLimitManager dailyLimitManager;
    private LeaderboardManager leaderboardManager;
    private AntiAfkManager antiAfkManager;
    private FishMarketMenu fishMarketMenu;
    private SellManager sellManager;
    private int autoSaveTaskId = -1;

    public void onEnable() {
        plugin.saveDefaultConfig();
        this.setupFiles();
        this.setupManagers();
        this.setupCommands();
        this.setupListeners();
        this.startAutoSaveTask();
        plugin.getLogger().info("RumahKitaFishing enabled with " + this.fishManager.fishes().size() + " custom fishes.");
    }

    public void onDisable() {
        if (this.autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.autoSaveTaskId);
        }
        if (this.playerDataManager != null) {
            this.playerDataManager.saveAll();
        }
        if (this.dailyLimitManager != null) {
            this.dailyLimitManager.save();
        }
        plugin.getLogger().info("RumahKitaFishing disabled.");
    }

    public void reloadEverything() {
        plugin.reloadConfig();
        this.fishesConfig.reload();
        this.messagesConfig.reload();
        this.guiConfig.reload();
        this.fishManager = new FishManager(this, this.fishesConfig.get());
        this.fishManager.load();
        this.economyManager.setup();
        this.dailyLimitManager.reload();
    }

    private void setupFiles() {
        this.fishesConfig = new ConfigFile(this.plugin, "fishes.yml");
        this.messagesConfig = new ConfigFile(this.plugin, "messages.yml");
        this.guiConfig = new ConfigFile(this.plugin, "gui.yml");
        this.fishesConfig.setup();
        this.messagesConfig.setup();
        this.guiConfig.setup();
    }

    private void setupManagers() {
        this.fishManager = new FishManager(this, this.fishesConfig.get());
        this.fishManager.load();
        this.playerDataManager = new PlayerDataManager(this);
        this.dailyLimitManager = new DailyLimitManager(this);
        this.economyManager = new EconomyManager(this);
        this.economyManager.setup();
        this.fishItemFactory = new FishItemFactory(this);
        this.antiAfkManager = new AntiAfkManager(this);
        this.leaderboardManager = new LeaderboardManager(this);
        this.sellManager = new SellManager(this);
        this.fishMarketMenu = new FishMarketMenu(this);
    }

    private void setupCommands() {
        FishCommand fishCommand = new FishCommand(this);
        PluginCommand fish = plugin.getCommand("fish");
        if (fish != null) {
            fish.setExecutor((CommandExecutor)fishCommand);
            fish.setTabCompleter((TabCompleter)fishCommand);
        }
        FishAdminCommand adminCommand = new FishAdminCommand(this);
        PluginCommand fishAdmin = plugin.getCommand("fishadmin");
        if (fishAdmin != null) {
            fishAdmin.setExecutor((CommandExecutor)adminCommand);
            fishAdmin.setTabCompleter((TabCompleter)adminCommand);
        }
    }

    private void setupListeners() {
        Bukkit.getPluginManager().registerEvents((Listener)new FishingListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents((Listener)new GuiListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents((Listener)new PlayerListener(this), this.plugin);
    }

    private void startAutoSaveTask() {
        long minutes = Math.max(1L, plugin.getConfig().getLong("settings.auto-save-interval-minutes", 10L));
        this.autoSaveTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            this.playerDataManager.saveAll();
            this.dailyLimitManager.save();
        }, minutes * 60L * 20L, minutes * 60L * 20L).getTaskId();
    }

    public ConfigFile messagesConfig() {
        return this.messagesConfig;
    }

    public ConfigFile guiConfig() {
        return this.guiConfig;
    }

    public FishManager fishManager() {
        return this.fishManager;
    }

    public FishItemFactory fishItemFactory() {
        return this.fishItemFactory;
    }

    public EconomyManager economyManager() {
        return this.economyManager;
    }

    public PlayerDataManager playerDataManager() {
        return this.playerDataManager;
    }

    public DailyLimitManager dailyLimitManager() {
        return this.dailyLimitManager;
    }

    public LeaderboardManager leaderboardManager() {
        return this.leaderboardManager;
    }

    public AntiAfkManager antiAfkManager() {
        return this.antiAfkManager;
    }

    public FishMarketMenu fishMarketMenu() {
        return this.fishMarketMenu;
    }

    public SellManager sellManager() {
        return this.sellManager;
    }
}

