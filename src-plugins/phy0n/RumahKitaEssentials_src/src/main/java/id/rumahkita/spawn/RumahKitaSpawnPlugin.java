package id.rumahkita.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class RumahKitaSpawnPlugin implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public RumahKitaSpawnPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Random Spawn & Respawn Module Enabled.");
    }
    
    public void onDisable() {
    }

    private Location getRandomSafeLocation(World world) {
        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(5000) - 2500;
            int z = random.nextInt(5000) - 2500;
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();

            if (type.isSolid() && type != Material.LAVA && type != Material.MAGMA_BLOCK && type != Material.CACTUS && type != Material.WATER) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return world.getSpawnLocation();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        // We let Vanilla/Essentials handle first join, so they spawn at the spawnpoint.
        // Returning players will naturally stay where they left off.
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!event.isBedSpawn() && !event.isAnchorSpawn()) {
            World world = Bukkit.getWorld("world");
            if (world != null) {
                event.setRespawnLocation(getRandomSafeLocation(world));
            }
        }
    }
}
