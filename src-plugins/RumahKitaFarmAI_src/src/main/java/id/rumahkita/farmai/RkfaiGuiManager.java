package id.rumahkita.farmai;

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

public class RkfaiGuiManager implements Listener {
    private final RumahKitaFarmAI plugin;
    public static final String TITLE = ChatColor.GREEN + "Farm AI Admin Dashboard";

    public RkfaiGuiManager(RumahKitaFarmAI plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.WHITE + "Farm AI Configuration");
        
        fillBorder(inv);
        
        boolean enabled = plugin.getConfig().getBoolean("enabled", true);
        
        inv.setItem(13, createItem(Material.ZOMBIE_HEAD, "&b&lStatus: " + (enabled ? "&a&lON" : "&c&lOFF"), 
            "&7Click to toggle",
            "&7Farm AI functionality."
        ));
        
        inv.setItem(29, createItem(Material.DIAMOND_SWORD, "&e&lForce Scan Now", 
            "&7Force scan all worlds", 
            "&7for hostile mobs to fix."
        ));
        
        inv.setItem(31, createItem(Material.OAK_FENCE, "&c&lZone Builder", 
            "&7Use &e/rkfai zonehere <name> <rad>", 
            "&7to build new zones."
        ));
        
        inv.setItem(33, createItem(Material.REDSTONE, "&a&lReload Config", 
            "&7Click to reload", 
            "&7the plugin configuration."
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
        if (slot == 13) { 
            player.closeInventory();
            boolean enabled = plugin.getConfig().getBoolean("enabled", true);
            Bukkit.dispatchCommand(player, enabled ? "rkfai off" : "rkfai on");
        } else if (slot == 29) { 
            player.closeInventory();
            Bukkit.dispatchCommand(player, "rkfai scan");
        } else if (slot == 31) { 
            player.closeInventory();
            Bukkit.dispatchCommand(player, "rkfai help");
        } else if (slot == 33) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "rkfai reload");
        }
    }
}
