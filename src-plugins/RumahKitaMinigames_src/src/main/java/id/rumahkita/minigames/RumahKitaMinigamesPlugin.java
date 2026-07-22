package id.rumahkita.minigames;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import id.rumahkita.games.RumahKitaGamesPlugin;
import id.rumahkita.ctf.RumahKitaCaptureFlag;
import id.rumahkita.pvp.RumahKitaPvP1v1Plugin;

public class RumahKitaMinigamesPlugin extends JavaPlugin implements Listener {

    private RumahKitaGamesPlugin moduleRumahKitaGamesPlugin;
    private RumahKitaCaptureFlag moduleRumahKitaCaptureFlag;
    private RumahKitaPvP1v1Plugin moduleRumahKitaPvP1v1Plugin;

    @Override
    public void onEnable() {
        getLogger().info("Initializing RumahKita Minigames Modules...");

        extractConfig("RumahKitaGames_config.yml");
        extractConfig("RumahKitaCaptureFlag_config.yml");
        extractConfig("RumahKitaPvP1v1_config.yml");
        
        moduleRumahKitaGamesPlugin = new RumahKitaGamesPlugin(this);
        try { moduleRumahKitaGamesPlugin.onEnable(); } catch (Throwable e) { getLogger().severe("Failed to load Games: " + e.getMessage()); }
        moduleRumahKitaCaptureFlag = new RumahKitaCaptureFlag(this);
        try { moduleRumahKitaCaptureFlag.onEnable(); } catch (Throwable e) { getLogger().severe("Failed to load CTF: " + e.getMessage()); }
        moduleRumahKitaPvP1v1Plugin = new RumahKitaPvP1v1Plugin(this);
        try { moduleRumahKitaPvP1v1Plugin.onEnable(); } catch (Throwable e) { getLogger().severe("Failed to load PvP: " + e.getMessage()); }

        getServer().getPluginManager().registerEvents(this, this);
        RkmgGuiManager adminGui = new RkmgGuiManager(this);
        getServer().getPluginManager().registerEvents(adminGui, this);
                org.bukkit.command.PluginCommand adminCmd = getCommand("rkmg");
        if (adminCmd != null) {
            RkmgCommand rkmgExec = new RkmgCommand(this, adminGui);
            adminCmd.setExecutor(rkmgExec);
            adminCmd.setTabCompleter(rkmgExec);
        }
        
        getLogger().info("RumahKita Minigames Enabled.");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGlobalInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        Material mat = event.getItem().getType();
        if (mat == Material.ENDER_PEARL || mat == Material.FIREWORK_ROCKET) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    event.getPlayer().setCooldown(mat, 0);
                } catch (Exception ignored) {}
            }, 1L);
        }
    }
    
    private void extractConfig(String fileName) {
        if (!new java.io.File(getDataFolder(), fileName).exists()) {
            try { saveResource(fileName, false); } catch (Exception ignored) {}
        }
    }

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

        if (moduleRumahKitaGamesPlugin != null) { try { moduleRumahKitaGamesPlugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaCaptureFlag != null) { try { moduleRumahKitaCaptureFlag.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        if (moduleRumahKitaPvP1v1Plugin != null) { try { moduleRumahKitaPvP1v1Plugin.onDisable(); } catch (Exception e) { e.printStackTrace(); } }
        getLogger().info("RumahKita Minigames Disabled.");
    }
}
