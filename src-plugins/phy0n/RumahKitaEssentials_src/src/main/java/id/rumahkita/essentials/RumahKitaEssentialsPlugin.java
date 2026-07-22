package id.rumahkita.essentials;

import org.bukkit.plugin.java.JavaPlugin;

import id.rumahkita.admin.RumahKitaAdminPlugin;
import id.rumahkita.utilities.RumahKitaUtilitiesPlugin;
import id.rumahkita.warps.RumahKitaWarpsPlugin;
import id.rumahkita.spawn.RumahKitaSpawnPlugin;
import id.rumahkita.essentials.EssentialsAdminGui;
import id.rumahkita.essentials.EssentialsAdminCommand;

public class RumahKitaEssentialsPlugin extends JavaPlugin {

    private RumahKitaAdminPlugin adminModule;
    private RumahKitaUtilitiesPlugin utilitiesModule;
    private RumahKitaWarpsPlugin warpsModule;
    private RumahKitaSpawnPlugin spawnModule;

    public void onEnable() {

        getLogger().info("Initializing RumahKita Essentials Modules...");

        org.bukkit.Bukkit.getPluginManager().registerEvents(new AnvilColorListener(), this);

        EssentialsAdminGui gui = new EssentialsAdminGui(this);
        org.bukkit.Bukkit.getPluginManager().registerEvents(gui, this);
        org.bukkit.command.PluginCommand adminCmd = getCommand("rkessentials");
        if (adminCmd != null) {
            adminCmd.setExecutor(new EssentialsAdminCommand(gui));
        }

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
