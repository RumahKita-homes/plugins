package id.rumahkita.games;

import org.bukkit.plugin.java.JavaPlugin;

public class RumahKitaGamesPlugin { 
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    public RumahKitaGamesPlugin(org.bukkit.plugin.java.JavaPlugin plugin) {
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


    
    private static RumahKitaGamesPlugin instance;
    private CoinflipManager coinflipManager;
    private RpsManager rpsManager;

    public void onEnable() {
        instance = this;
        
        getLogger().info("RumahKitaGames v1.0.0 is enabling...");
        
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        coinflipManager = new CoinflipManager(this);
        getServer().getPluginManager().registerEvents(coinflipManager, this.plugin);
        CoinflipCommand cfCmd = new CoinflipCommand(this);
        getCommand("coinflip").setExecutor(cfCmd);
        getCommand("coinflip").setTabCompleter(cfCmd);
        
        rpsManager = new RpsManager(this);
        getServer().getPluginManager().registerEvents(rpsManager, this.plugin);
        RpsCommand rpsCmd = new RpsCommand(this);
        getCommand("rps").setExecutor(rpsCmd);
        getCommand("rps").setTabCompleter(rpsCmd);
        
        RkgCommand rkgCmd = new RkgCommand(this);
        getCommand("rkg").setExecutor(rkgCmd);
        getCommand("rkg").setTabCompleter(rkgCmd);
        
        getLogger().info("RumahKitaGames successfully hooked to RumahKitaEconomyV2!");
    }

    public void onDisable() {
        getLogger().info("RumahKitaGames disabled.");
    }

    public static RumahKitaGamesPlugin getInstance() {
        return instance;
    }
    
    public CoinflipManager getCoinflipManager() {
        return coinflipManager;
    }

    public RpsManager getRpsManager() {
        return rpsManager;
    }
}
