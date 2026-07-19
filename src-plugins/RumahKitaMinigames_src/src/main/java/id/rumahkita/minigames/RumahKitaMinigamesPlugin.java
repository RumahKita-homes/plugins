package id.rumahkita.minigames;

import org.bukkit.plugin.java.JavaPlugin;
import id.rumahkita.games.RumahKitaGamesPlugin;
import id.rumahkita.ctf.RumahKitaCaptureFlag;
import id.rumahkita.pvp.RumahKitaPvP1v1Plugin;

public class RumahKitaMinigamesPlugin extends JavaPlugin {

    private RumahKitaGamesPlugin moduleRumahKitaGamesPlugin;
    private RumahKitaCaptureFlag moduleRumahKitaCaptureFlag;
    private RumahKitaPvP1v1Plugin moduleRumahKitaPvP1v1Plugin;

    @Override
    public void onEnable() {
        getLogger().info("Initializing RumahKita Minigames Modules...");
        
        // Extract individual module configs if they don't exist
        extractConfig("RumahKitaGames_config.yml");
        extractConfig("RumahKitaCaptureFlag_config.yml");
        extractConfig("RumahKitaPvP1v1_config.yml");
        
        moduleRumahKitaGamesPlugin = new RumahKitaGamesPlugin(this);
        try { moduleRumahKitaGamesPlugin.onEnable(); } catch (Throwable e) { getLogger().severe("Failed to load Games: " + e.getMessage()); }
        moduleRumahKitaCaptureFlag = new RumahKitaCaptureFlag(this);
        try { moduleRumahKitaCaptureFlag.onEnable(); } catch (Throwable e) { getLogger().severe("Failed to load CTF: " + e.getMessage()); }
        moduleRumahKitaPvP1v1Plugin = new RumahKitaPvP1v1Plugin(this);
        try { moduleRumahKitaPvP1v1Plugin.onEnable(); } catch (Throwable e) { getLogger().severe("Failed to load PvP: " + e.getMessage()); }
        
        getLogger().info("RumahKita Minigames Enabled.");
    }
    
    private void extractConfig(String fileName) {
        if (!new java.io.File(getDataFolder(), fileName).exists()) {
            try { saveResource(fileName, false); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDisable() {
        if (moduleRumahKitaGamesPlugin != null) { try { moduleRumahKitaGamesPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaCaptureFlag != null) { try { moduleRumahKitaCaptureFlag.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaPvP1v1Plugin != null) { try { moduleRumahKitaPvP1v1Plugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        getLogger().info("RumahKita Minigames Disabled.");
    }
}
