package id.rumahkita.discordverify;

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

public class RkdvGuiManager implements Listener {
    private final RumahKitaDiscordVerifyPlugin plugin;
    public static final String TITLE = ChatColor.BLUE + "Discord Verify Admin";

    public RkdvGuiManager(RumahKitaDiscordVerifyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.WHITE + "Discord Verification");
        
        fillBorder(inv);
        
        inv.setItem(13, createItem(Material.PAPER, "&b&lVerification Status", 
            "&7Enabled: &f" + plugin.getConfig().getBoolean("enabled", true),
            "&7Verified Players: &f" + plugin.countSection("verified"),
            "&7Pending: &f" + plugin.pendingByUuid.size()
        ));
        
        inv.setItem(29, createItem(Material.RED_DYE, "&c&lClear Pending", 
            "&7Click to clear all", 
            "&7pending verification codes."
        ));
        
        inv.setItem(31, createItem(Material.ENDER_PEARL, "&d&lResync Discord", 
            "&7Click to resync discord", 
            "&7messages and fetch verify requests."
        ));
        
        inv.setItem(33, createItem(Material.REDSTONE, "&e&lReload Config", 
            "&7Click to reload", 
            "&7the plugin configuration."
        ));
        
        inv.setItem(40, createItem(Material.BOOK, "&6&lHelp", 
            "&7Run &e/rkdv help",
            "&7for more manual commands."
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
        if (slot == 29) { 
            player.closeInventory();
            Bukkit.dispatchCommand(player, "rkdv clearpending");
        } else if (slot == 31) { 
            player.closeInventory();
            Bukkit.dispatchCommand(player, "rkdv resyncdiscord");
        } else if (slot == 33) { 
            player.closeInventory();
            Bukkit.dispatchCommand(player, "rkdv reload");
        } else if (slot == 40) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "rkdv help");
        }
    }
}
