package id.rumahkita.games;

import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class RumahKitaGamesPlugin { 
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    public RumahKitaGamesPlugin(org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private org.bukkit.configuration.file.FileConfiguration customConfig;
    private java.io.File customConfigFile;

    public void reloadConfig() {
        if (customConfigFile == null) {
            customConfigFile = new java.io.File(plugin.getDataFolder(), "RumahKitaGames_config.yml");
        }
        customConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(customConfigFile);
    }
    
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        if (customConfig == null) {
            reloadConfig();
        }
        return customConfig;
    }
    
    public void saveConfig() {
        if (customConfig == null || customConfigFile == null) return;
        try {
            getConfig().save(customConfigFile);
        } catch (java.io.IOException ex) {
            getLogger().log(java.util.logging.Level.SEVERE, "Could not save config to " + customConfigFile, ex);
        }
    }
    
    public void saveDefaultConfig() {
        if (customConfigFile == null) {
            customConfigFile = new java.io.File(plugin.getDataFolder(), "RumahKitaGames_config.yml");
        }
        if (!customConfigFile.exists()) {
            plugin.saveResource("RumahKitaGames_config.yml", false);
        }
    }
    
    public java.util.logging.Logger getLogger() { return plugin.getLogger(); }
    public org.bukkit.Server getServer() { return plugin.getServer(); }
    public org.bukkit.command.PluginCommand getCommand(String name) { return plugin.getCommand(name); }
    public org.bukkit.plugin.java.JavaPlugin getPlugin() { return plugin; }


    
    private static RumahKitaGamesPlugin instance;
    private CoinflipManager coinflipManager;
    private RpsManager rpsManager;
    private Economy econ = null;

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
        
        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", "RumahKitaGames"));
            getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        getLogger().info("RumahKitaGames successfully hooked to Vault Economy!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
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
    
    public Economy getEconomy() {
        return econ;
    }
}
