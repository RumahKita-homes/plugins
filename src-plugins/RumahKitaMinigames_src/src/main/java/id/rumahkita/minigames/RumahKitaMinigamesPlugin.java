package id.rumahkita.minigames;

import org.bukkit.plugin.java.JavaPlugin;
import id.rumahkita.games.RumahKitaGamesPlugin;
import id.rumahkita.ctf.RumahKitaCaptureFlag;
import id.rumahkita.pvp.RumahKitaPvP1v1Plugin;
import id.rumahkita.fishing.RumahKitaFishingPlugin;

public class RumahKitaMinigamesPlugin extends JavaPlugin {

    private RumahKitaGamesPlugin moduleRumahKitaGamesPlugin;
    private RumahKitaCaptureFlag moduleRumahKitaCaptureFlag;
    private RumahKitaPvP1v1Plugin moduleRumahKitaPvP1v1Plugin;
    private RumahKitaFishingPlugin moduleRumahKitaFishingPlugin;

    @Override
    public void onEnable() {
        getLogger().info("Initializing RumahKita Minigames Modules...");
        this.saveDefaultConfig();
        
        moduleRumahKitaGamesPlugin = new RumahKitaGamesPlugin(this);
        try { moduleRumahKitaGamesPlugin.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        moduleRumahKitaCaptureFlag = new RumahKitaCaptureFlag(this);
        try { moduleRumahKitaCaptureFlag.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        moduleRumahKitaPvP1v1Plugin = new RumahKitaPvP1v1Plugin(this);
        try { moduleRumahKitaPvP1v1Plugin.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        moduleRumahKitaFishingPlugin = new RumahKitaFishingPlugin(this);
        try { moduleRumahKitaFishingPlugin.onEnable(); } catch (Exception e) { e.printStackTrace(); }
        
        getLogger().info("RumahKita Minigames Enabled.");
    }

    @Override
    public void onDisable() {
        if (moduleRumahKitaGamesPlugin != null) { try { moduleRumahKitaGamesPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaCaptureFlag != null) { try { moduleRumahKitaCaptureFlag.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaPvP1v1Plugin != null) { try { moduleRumahKitaPvP1v1Plugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaFishingPlugin != null) { try { moduleRumahKitaFishingPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        getLogger().info("RumahKita Minigames Disabled.");
    }
}
