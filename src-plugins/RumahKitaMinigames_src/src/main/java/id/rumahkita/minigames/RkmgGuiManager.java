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

public class RkmgGuiManager implements Listener {
    private final RumahKitaMinigamesPlugin plugin;
    public static final String TITLE = ChatColor.GOLD + "Minigames Admin Dashboard";

    public RkmgGuiManager(RumahKitaMinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBorder(inv);
        
        inv.setItem(20, createItem(Material.RED_BANNER, "&c&lCapture The Flag", 
            "&7Manage CTF arenas,",
            "&7Red/Blue flag positions,",
            "&7and force start/stop.",
            "",
            "&aLeft-Click to open /ctf help"
        ));
        
        inv.setItem(22, createItem(Material.IRON_SWORD, "&e&lPvP 1v1 Manager", 
            "&7View PvP arena queues",
            "&7and manage setups.",
            "",
            "&aLeft-Click to open /pvp help"
        ));
        
        inv.setItem(24, createItem(Material.FISHING_ROD, "&b&lFishing Admin", 
            "&7Manage custom fishing rates,",
            "&7rewards, and fish weights.",
            "",
            "&aLeft-Click to open /fishadmin"
        ));
        
        inv.setItem(39, createItem(Material.TARGET, "&d&lGames Settings", 
            "&7General minigames settings,",
            "&7such as rewards and maps.",
            "",
            "&aLeft-Click to open /games help"
        ));
        
        inv.setItem(41, createItem(Material.CLOCK, "&a&lGlobal Anti-Cooldown", 
            "&7Status: &aActive",
            "&7Prevents cooldown for Ender Pearls",
            "&7and Firework Rockets globally."
        ));
        
        player.openInventory(inv);
    }
    
    private void fillBorder(Inventory inv) {
        ItemStack pane = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
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
        
        if (slot == 20) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "ctf help");
        } else if (slot == 22) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "pvp help");
        } else if (slot == 24) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "fishadmin");
        } else if (slot == 39) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "games help");
        } else if (slot == 41) {
            player.sendMessage(ChatColor.YELLOW + "The Anti-Cooldown feature is currently active by default.");
            player.closeInventory();
        }
    }
}
