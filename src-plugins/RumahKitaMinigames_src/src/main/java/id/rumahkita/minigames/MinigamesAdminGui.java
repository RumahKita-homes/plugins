package id.rumahkita.minigames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MinigamesAdminGui implements Listener {
    private final RumahKitaMinigamesPlugin plugin;
    public static final String TITLE = ChatColor.GOLD + "Minigames Admin Dashboard";

    public MinigamesAdminGui(RumahKitaMinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fillBorder(inv);
        
        inv.setItem(10, createItem(Material.RED_BANNER, "&c&lCapture The Flag", 
            "&7Manage CTF arenas,",
            "&7Red/Blue flag positions,",
            "&7and force start/stop.",
            "",
            "&aLeft-Click to open /ctf help"
        ));
        
        inv.setItem(12, createItem(Material.IRON_SWORD, "&e&lPvP 1v1 Manager", 
            "&7View PvP arena queues",
            "&7and manage setups.",
            "",
            "&aLeft-Click to open /pvp help"
        ));
        
        inv.setItem(14, createItem(Material.TARGET, "&b&lGames Settings", 
            "&7General minigames settings,",
            "&7such as rewards and maps.",
            "",
            "&aLeft-Click to open /games help"
        ));
        
        inv.setItem(16, createItem(Material.CLOCK, "&a&lGlobal Anti-Cooldown", 
            "&7Status: &aActive",
            "&7Prevents cooldown for Ender Pearls",
            "&7and Firework Rockets globally."
        ));
        
        player.openInventory(inv);
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, pane);
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> loreList = new ArrayList<>();
            for (String l : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        if (slot == 10) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "ctf help");
        } else if (slot == 12) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "pvp help");
        } else if (slot == 14) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "games help");
        } else if (slot == 16) {
            // Future toggle feature
            player.sendMessage(ChatColor.YELLOW + "The Anti-Cooldown feature is currently active by default.");
            player.closeInventory();
        }
    }
}
