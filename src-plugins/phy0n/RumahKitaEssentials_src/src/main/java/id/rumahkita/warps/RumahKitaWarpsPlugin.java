package id.rumahkita.warps;

import org.bukkit.plugin.java.JavaPlugin;

public class RumahKitaWarpsPlugin  {
    private final id.rumahkita.essentials.RumahKitaEssentialsPlugin plugin;
    public RumahKitaWarpsPlugin(id.rumahkita.essentials.RumahKitaEssentialsPlugin plugin) {
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

    
    private static RumahKitaWarpsPlugin instance;
    private WarpManager warpManager;
    private RtpManager rtpManager;
    private ServerWarpManager serverWarpManager;

    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        getLogger().info("RumahKitaWarps v1.0.0 is enabling...");
        
        warpManager = new WarpManager(this);
        warpManager.loadWarps();
        
        getServer().getPluginManager().registerEvents(warpManager, this.getPlugin());
        WarpCommand warpCmd = new WarpCommand(this);
        getCommand("pwarp").setExecutor(warpCmd);
        getCommand("pwarp").setTabCompleter(warpCmd);
        
        rtpManager = new RtpManager(this);
        RtpCommand rtpCmd = new RtpCommand(rtpManager);
        getCommand("rtp").setExecutor(rtpCmd);
        
        serverWarpManager = new ServerWarpManager(this);
        getServer().getPluginManager().registerEvents(serverWarpManager, this.getPlugin());
        ServerWarpCommand swCmd = new ServerWarpCommand(serverWarpManager);
        getCommand("warp").setExecutor(swCmd);
        getCommand("warp").setTabCompleter(swCmd);
        getCommand("setwarp").setExecutor(swCmd);
        getCommand("delwarp").setExecutor(swCmd);
        getCommand("delwarp").setTabCompleter(swCmd);
        getCommand("editwarp").setExecutor(swCmd);
        getCommand("editwarp").setTabCompleter(swCmd);
        
        
        BackManager backManager = new BackManager(this.getPlugin());
        getServer().getPluginManager().registerEvents(backManager, this.getPlugin());
        if (getCommand("back") != null) {
            getCommand("back").setExecutor(new BackCommand(backManager));
        }
        
        getLogger().info("RumahKitaWarps successfully enabled!");
    }

    public void onDisable() {
        if (warpManager != null) {
            warpManager.saveWarps();
        }
        if (serverWarpManager != null) {
            serverWarpManager.saveWarps();
        }
        getLogger().info("RumahKitaWarps disabled.");
    }

    public static RumahKitaWarpsPlugin getInstance() {
        return instance;
    }
    
    public WarpManager getWarpManager() {
        return warpManager;
    }
    
    public ServerWarpManager getServerWarpManager() {
        return serverWarpManager;
    }
}
