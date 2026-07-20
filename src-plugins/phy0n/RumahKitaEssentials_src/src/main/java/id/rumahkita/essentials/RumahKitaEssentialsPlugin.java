package id.rumahkita.essentials;

import org.bukkit.plugin.java.JavaPlugin;

import id.rumahkita.admin.RumahKitaAdminPlugin;
import id.rumahkita.utilities.RumahKitaUtilitiesPlugin;
import id.rumahkita.warps.RumahKitaWarpsPlugin;
import id.rumahkita.spawn.RumahKitaSpawnPlugin;

public class RumahKitaEssentialsPlugin extends JavaPlugin {

    private RumahKitaAdminPlugin adminModule;
    private RumahKitaUtilitiesPlugin utilitiesModule;
    private RumahKitaWarpsPlugin warpsModule;
    private RumahKitaSpawnPlugin spawnModule;

    public void onEnable() {
        // Save default config from all modules if needed, or handle in modules


        getLogger().info("Initializing RumahKita Essentials Modules...");

        // Initialize modules
        adminModule = new RumahKitaAdminPlugin(this);
        adminModule.onEnable();

        utilitiesModule = new RumahKitaUtilitiesPlugin(this);
        utilitiesModule.onEnable();

        warpsModule = new RumahKitaWarpsPlugin(this);
        warpsModule.onEnable();

        spawnModule = new RumahKitaSpawnPlugin(this);
        spawnModule.onEnable();

        getLogger().info("RumahKita Essentials v1.0.0 Enabled.");
    }

    public void onDisable() {
        // PlugManX Compatibility Cleanup
        try {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                p.closeInventory();
            }
            org.bukkit.Bukkit.getServicesManager().unregisterAll((org.bukkit.plugin.Plugin)this);
            org.bukkit.Bukkit.getScheduler().cancelTasks((org.bukkit.plugin.Plugin)this);
            org.bukkit.event.HandlerList.unregisterAll((org.bukkit.plugin.Plugin)this);
        } catch (Exception ignored) {}

        if (adminModule != null) {
            adminModule.onDisable();
        }
        if (utilitiesModule != null) {
            utilitiesModule.onDisable();
        }
        if (warpsModule != null) {
            warpsModule.onDisable();
        }
        if (spawnModule != null) {
            spawnModule.onDisable();
        }
        getLogger().info("RumahKita Essentials Disabled.");
    }
}
