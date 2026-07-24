package id.rumahkita.warps;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WarpManager implements Listener {

    private final RumahKitaWarpsPlugin plugin;
    private File warpsFile;
    private FileConfiguration warpsConfig;
    private final Map<String, PlayerWarp> warps = new HashMap<>();

    public WarpManager(RumahKitaWarpsPlugin plugin) {
        this.plugin = plugin;
        setupFiles();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            try {
                warpsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
    }

    public void loadWarps() {
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        warps.clear();
        ConfigurationSection section = warpsConfig.getConfigurationSection("warps");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String name = section.getString(key + ".name");
                String ownerIdStr = section.getString(key + ".owner");
                if (ownerIdStr == null) continue;
                UUID owner = UUID.fromString(ownerIdStr);
                String world = section.getString(key + ".world");
                double x = section.getDouble(key + ".x");
                double y = section.getDouble(key + ".y");
                double z = section.getDouble(key + ".z");
                float yaw = (float) section.getDouble(key + ".yaw");
                float pitch = (float) section.getDouble(key + ".pitch");
                
                Location loc = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                warps.put(name.toLowerCase(), new PlayerWarp(name, owner, loc));
            }
        }
        plugin.getLogger().info("Loaded " + warps.size() + " warps.");
    }

    public void saveWarps() {
        warpsConfig.set("warps", null);
        for (PlayerWarp warp : warps.values()) {
            String path = "warps." + warp.name.toLowerCase();
            warpsConfig.set(path + ".name", warp.name);
            warpsConfig.set(path + ".owner", warp.owner.toString());
            warpsConfig.set(path + ".world", warp.location.getWorld().getName());
            warpsConfig.set(path + ".x", warp.location.getX());
            warpsConfig.set(path + ".y", warp.location.getY());
            warpsConfig.set(path + ".z", warp.location.getZ());
            warpsConfig.set(path + ".yaw", warp.location.getYaw());
            warpsConfig.set(path + ".pitch", warp.location.getPitch());
        }
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(this.plugin.getPlugin(), () -> {
            try {
                warpsConfig.save(warpsFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not save warps.yml!");
            }
        });
    }

    public boolean createWarp(Player p, String name) {
        String key = name.toLowerCase();
        if (warps.containsKey(key)) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Warp name '" + name + "' is already taken! Please choose another name.");
            return false;
        }

        for (PlayerWarp w : warps.values()) {
            if (w.owner.equals(p.getUniqueId())) {
                p.sendMessage(getPrefix() + ChatColor.RED + "You already have a warp! Delete it first using /pwarp delete " + w.name);
                return false;
            }
        }

        long cost = plugin.getConfig().getLong("cost.create", 50000);
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Economy is not setup!");
            return false;
        }
        Economy economy = rsp.getProvider();
        
        if (economy.getBalance(p) < cost) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Not enough money! Cost to create pwarp is " + economy.format(cost));
            return false;
        }

        economy.withdrawPlayer(p, cost);
        warps.put(key, new PlayerWarp(name, p.getUniqueId(), p.getLocation()));
        saveWarps();

        p.sendMessage(getPrefix() + ChatColor.GREEN + "Successfully created warp " + ChatColor.YELLOW + name + ChatColor.GREEN + " for a cost of " + economy.format(cost) + "!");
        return true;
    }

    public boolean deleteWarp(Player p, String name) {
        String key = name.toLowerCase();
        PlayerWarp warp = warps.get(key);
        
        if (warp == null) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Warp not found.");
            return false;
        }

        if (!warp.owner.equals(p.getUniqueId()) && !p.hasPermission("pwarp.admin")) {
            p.sendMessage(getPrefix() + ChatColor.RED + "This is not your warp.");
            return false;
        }

        warps.remove(key);
        saveWarps();
        p.sendMessage(getPrefix() + ChatColor.GREEN + "Warp " + ChatColor.YELLOW + warp.name + ChatColor.GREEN + " has been deleted.");
        return true;
    }

    public void openWarpMenu(Player p) {
        int size = 54;
        String title = ChatColor.WHITE + "Player Warps";
        Inventory inv = Bukkit.createInventory(null, size, title);

        List<PlayerWarp> warpList = new ArrayList<>(warps.values());
        warpList.sort((w1, w2) -> w1.name.compareToIgnoreCase(w2.name));

        int maxItems = 54;
        int index = 0;
        
        for (PlayerWarp warp : warpList) {
            if (index >= maxItems) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.owner);
                meta.setOwningPlayer(owner);
                meta.setDisplayName(ChatColor.YELLOW + warp.name);
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + (owner.getName() != null ? owner.getName() : "Unknown"));
                lore.add("");
                lore.add(ChatColor.GREEN + "Click to teleport!");
                
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(index, head);
            index++;
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(ChatColor.WHITE + "Player Warps")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            
            Player p = (Player) e.getWhoClicked();
            Material type = e.getCurrentItem().getType();
            
            if (type == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String warpName = ChatColor.stripColor(meta.getDisplayName());
                    PlayerWarp warp = warps.get(warpName.toLowerCase());
                    if (warp != null) {
                        p.closeInventory();
                        teleportToWarp(p, warpName);
                    } else {
                        p.sendMessage(getPrefix() + ChatColor.RED + "Warp not found.");
                        p.closeInventory();
                    }
                }
            }
        }
    }

    public void teleportToWarp(Player p, String name) {
        String key = name.toLowerCase();
        PlayerWarp warp = warps.get(key);
        
        if (warp == null) {
            p.sendMessage(getPrefix() + ChatColor.RED + "Warp " + name + " not found.");
            return;
        }

        p.teleport(warp.location);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        OfflinePlayer owner = Bukkit.getOfflinePlayer(warp.owner);
        String ownerName = owner.getName() != null ? owner.getName() : "Unknown";
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aWelcome to &e" + warp.name + "&a!"));
        
        if (owner.isOnline() && !owner.getUniqueId().equals(p.getUniqueId())) {
            Player onlineOwner = owner.getPlayer();
            if (onlineOwner != null) {
                onlineOwner.sendMessage(ChatColor.YELLOW + p.getName() + ChatColor.GREEN + " has teleported to your pwarp " + ChatColor.YELLOW + warp.name + ChatColor.GREEN + ".");
            }
        }
    }

    public String getPrefix() {
        return "";
    }

    public List<String> getWarpNames() {
        List<String> names = new ArrayList<>();
        for (PlayerWarp warp : warps.values()) {
            names.add(warp.name);
        }
        return names;
    }

    private static class PlayerWarp {
        public String name;
        public UUID owner;
        public Location location;

        public PlayerWarp(String name, UUID owner, Location location) {
            this.name = name;
            this.owner = owner;
            this.location = location;
        }
    }
}
