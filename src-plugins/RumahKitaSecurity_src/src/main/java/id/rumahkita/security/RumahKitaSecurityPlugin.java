package id.rumahkita.security;

import org.bukkit.plugin.java.JavaPlugin;
import id.rumahkita.anticheat.RumahKitaAntiCheatPlugin;
import id.rumahkita.antixray.RumahKitaAntiXrayPlugin;
import id.rumahkita.securityban.RumahKitaSecurityBanPlugin;
import id.rumahkita.ramguard.RumahKitaRamGuardPlugin;
import id.rumahkita.orespec.RumahKitaOreSpectatorPlugin;

public class RumahKitaSecurityPlugin extends JavaPlugin {

    private RumahKitaAntiCheatPlugin moduleRumahKitaAntiCheatPlugin;
    private RumahKitaAntiXrayPlugin moduleRumahKitaAntiXrayPlugin;
    private RumahKitaSecurityBanPlugin moduleRumahKitaSecurityBanPlugin;
    private RumahKitaRamGuardPlugin moduleRumahKitaRamGuardPlugin;
    private RumahKitaOreSpectatorPlugin moduleRumahKitaOreSpectatorPlugin;
    private id.rumahkita.security.network.NetworkSecurityModule networkSecurityModule;

    @Override
    public void onEnable() {
        getLogger().info("Initializing RumahKita Security Modules...");
        this.saveDefaultConfig();
        
        moduleRumahKitaAntiCheatPlugin = new RumahKitaAntiCheatPlugin(this);
        try { moduleRumahKitaAntiCheatPlugin.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        moduleRumahKitaAntiXrayPlugin = new RumahKitaAntiXrayPlugin(this);
        try { moduleRumahKitaAntiXrayPlugin.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        moduleRumahKitaSecurityBanPlugin = new RumahKitaSecurityBanPlugin(this);
        try { moduleRumahKitaSecurityBanPlugin.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        moduleRumahKitaRamGuardPlugin = new RumahKitaRamGuardPlugin(this);
        try { moduleRumahKitaRamGuardPlugin.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        moduleRumahKitaOreSpectatorPlugin = new RumahKitaOreSpectatorPlugin(this);
        try { moduleRumahKitaOreSpectatorPlugin.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        
        networkSecurityModule = new id.rumahkita.security.network.NetworkSecurityModule(this);
        
        RksCommand rksCommand = new RksCommand(this);
        if (getCommand("rks") != null) {
            getCommand("rks").setExecutor(rksCommand);
            getCommand("rks").setTabCompleter(rksCommand);
        }
        
        getLogger().info("RumahKita Security Enabled.");
    }

    public RumahKitaAntiCheatPlugin getAntiCheat() { return moduleRumahKitaAntiCheatPlugin; }
    public RumahKitaAntiXrayPlugin getAntiXray() { return moduleRumahKitaAntiXrayPlugin; }
    public RumahKitaSecurityBanPlugin getSecurityBan() { return moduleRumahKitaSecurityBanPlugin; }
    public RumahKitaRamGuardPlugin getRamGuard() { return moduleRumahKitaRamGuardPlugin; }
    public RumahKitaOreSpectatorPlugin getOreSpectator() { return moduleRumahKitaOreSpectatorPlugin; }
    public id.rumahkita.security.network.NetworkSecurityModule getNetworkSecurity() { return networkSecurityModule; }

    @Override
    public void onDisable() {
        try {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                p.closeInventory();
            }
            org.bukkit.Bukkit.getServicesManager().unregisterAll((org.bukkit.plugin.Plugin)this);
            org.bukkit.Bukkit.getScheduler().cancelTasks((org.bukkit.plugin.Plugin)this);
            org.bukkit.event.HandlerList.unregisterAll((org.bukkit.plugin.Plugin)this);
        } catch (Exception ignored) {}

        if (moduleRumahKitaAntiCheatPlugin != null) { try { moduleRumahKitaAntiCheatPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaAntiXrayPlugin != null) { try { moduleRumahKitaAntiXrayPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaSecurityBanPlugin != null) { try { moduleRumahKitaSecurityBanPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaRamGuardPlugin != null) { try { moduleRumahKitaRamGuardPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaOreSpectatorPlugin != null) { try { moduleRumahKitaOreSpectatorPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        getLogger().info("RumahKita Security Disabled.");
    }
}
