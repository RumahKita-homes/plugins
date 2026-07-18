package id.rumahkita.essentials;

import org.bukkit.plugin.java.JavaPlugin;

import id.rumahkita.admin.RumahKitaAdminPlugin;
import id.rumahkita.utilities.RumahKitaUtilitiesPlugin;
import id.rumahkita.warps.RumahKitaWarpsPlugin;

public class RumahKitaEssentialsPlugin extends JavaPlugin {

    private RumahKitaAdminPlugin adminModule;
    private RumahKitaUtilitiesPlugin utilitiesModule;
    private RumahKitaWarpsPlugin warpsModule;

    public void onEnable() {
        // Save default config from all modules if needed, or handle in modules
        this.saveDefaultConfig();

        getLogger().info("Initializing RumahKita Essentials Modules...");

        // Initialize modules
        adminModule = new RumahKitaAdminPlugin(this);
        adminModule.onEnable();

        utilitiesModule = new RumahKitaUtilitiesPlugin(this);
        utilitiesModule.onEnable();

        warpsModule = new RumahKitaWarpsPlugin(this);
        warpsModule.onEnable();

        getLogger().info("RumahKita Essentials v1.0.0 Enabled.");
    }

    public void onDisable() {
        if (adminModule != null) {
            adminModule.onDisable();
        }
        if (utilitiesModule != null) {
            utilitiesModule.onDisable();
        }
        if (warpsModule != null) {
            warpsModule.onDisable();
        }
        getLogger().info("RumahKita Essentials Disabled.");
    }
}
