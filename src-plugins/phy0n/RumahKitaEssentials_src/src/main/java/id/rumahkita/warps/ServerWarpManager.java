package id.rumahkita.warps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerWarpManager implements Listener {
    private final RumahKitaWarpsPlugin plugin;
    private File file;
    private FileConfiguration config;
    private final Map<String, ServerWarp> warps = new HashMap<>();

    public ServerWarpManager(RumahKitaWarpsPlugin plugin) {
        this.plugin = plugin;
        loadFile();
    }

    public void loadFile() {
        file = new File(plugin.getDataFolder(), "server_warps.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadWarps();
    }

    public void loadWarps() {
        warps.clear();
        if (config.contains("warps")) {
            for (String key : config.getConfigurationSection("warps").getKeys(false)) {
                String path = "warps." + key + ".";
                World w = Bukkit.getWorld(config.getString(path + "world"));
                if (w == null) continue;
                double x = config.getDouble(path + "x");
                double y = config.getDouble(path + "y");
                double z = config.getDouble(path + "z");
                float yaw = (float) config.getDouble(path + "yaw");
                float pitch = (float) config.getDouble(path + "pitch");
                int slot = config.getInt(path + "slot", 0);
                String iconStr = config.getString(path + "icon", "ENDER_PEARL");
                Material icon = Material.matchMaterial(iconStr);
                if (icon == null) icon = Material.ENDER_PEARL;
                List<String> lore = config.getStringList(path + "lore");
                
                Location loc = new Location(w, x, y, z, yaw, pitch);
                warps.put(key.toLowerCase(), new ServerWarp(key, loc, slot, icon, lore));
            }
        }
    }

    public void saveWarps() {
        config.set("warps", null);
        for (ServerWarp w : warps.values()) {
            String path = "warps." + w.name.toLowerCase() + ".";
            config.set(path + "world", w.loc.getWorld().getName());
            config.set(path + "x", w.loc.getX());
            config.set(path + "y", w.loc.getY());
            config.set(path + "z", w.loc.getZ());
            config.set(path + "yaw", w.loc.getYaw());
            config.set(path + "pitch", w.loc.getPitch());
            config.set(path + "slot", w.slot);
            config.set(path + "icon", w.icon.name());
            config.set(path + "lore", w.lore);
        }
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(this.plugin.getPlugin(), () -> {
            try {
                config.save(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setWarp(String name, Location loc) {
        String key = name.toLowerCase();
        if (warps.containsKey(key)) {
            warps.get(key).loc = loc;
        } else {
            warps.put(key, new ServerWarp(name, loc, getNextSlot(), Material.ENDER_PEARL, new ArrayList<>()));
        }
        saveWarps();
    }

    public boolean delWarp(String name) {
        if (warps.remove(name.toLowerCase()) != null) {
            saveWarps();
            return true;
        }
        return false;
    }

    public ServerWarp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }
    
    public Map<String, ServerWarp> getWarps() {
        return warps;
    }

    private int getNextSlot() {
        int max = -1;
        for (ServerWarp w : warps.values()) {
            if (w.slot > max) max = w.slot;
        }
        return Math.min(max + 1, 26);
    }

    public void openWarpMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "Server Warps");
        
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(ChatColor.BLACK + "");
            bg.setItemMeta(bgMeta);
        }
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }
        
        for (ServerWarp w : warps.values()) {
            ItemStack item = new ItemStack(w.icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&l" + w.name.toUpperCase()));
                List<String> formatLore = new ArrayList<>();
                for (String l : w.lore) {
                    formatLore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                formatLore.add("");
                formatLore.add(ChatColor.YELLOW + "Click to Teleport!");
                meta.setLore(formatLore);
                item.setItemMeta(meta);
            }
            
            if (w.slot >= 0 && w.slot < 27) {
                inv.setItem(w.slot, item);
            } else {
                for (int j = 0; j < 27; j++) {
                    if (inv.getItem(j) != null && inv.getItem(j).getType() == Material.BLACK_STAINED_GLASS_PANE) {
                        inv.setItem(j, item);
                        break;
                    }
                }
            }
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Server Warps")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            Player p = (Player) event.getWhoClicked();
            String nameStr = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            if (nameStr == null) return;
            
            ServerWarp w = getWarp(nameStr.toLowerCase());
            if (w != null) {
                p.closeInventory();
                teleportToWarp(p, w.name);
            }
        }
    }

    public void teleportToWarp(Player p, String name) {
        String prefix = "";
        ServerWarp w = getWarp(name.toLowerCase());
        if (w == null) {
            p.sendMessage(prefix + ChatColor.RED + "Warp '" + name + "' not found.");
            return;
        }

        p.teleport(w.loc);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        p.sendMessage(prefix + ChatColor.GREEN + "Teleported to " + ChatColor.YELLOW + w.name + ChatColor.GREEN + " successfully!");
    }

    public static class ServerWarp {
        public String name;
        public Location loc;
        public int slot;
        public Material icon;
        public List<String> lore;

        public ServerWarp(String name, Location loc, int slot, Material icon, List<String> lore) {
            this.name = name;
            this.loc = loc;
            this.slot = slot;
            this.icon = icon;
            this.lore = lore;
        }
    }
}
