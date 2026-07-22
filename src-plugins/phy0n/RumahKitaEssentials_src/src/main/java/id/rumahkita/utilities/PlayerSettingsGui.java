package id.rumahkita.utilities;

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

public class PlayerSettingsGui implements Listener {
    
    public static final String TITLE = ChatColor.DARK_AQUA + "Player Settings";

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        
        fillBorder(inv);
        
        inv.setItem(10, createItem(Material.PAPER, "&e&lPrivate Messages", 
            "&7Toggle incoming private",
            "&7messages (Whisper).",
            "",
            "&aLeft-Click to Toggle (/msgtoggle)"
        ));
        
        inv.setItem(12, createItem(Material.ENDER_PEARL, "&b&lTeleport Requests (TPA)", 
            "&7Prevent others from sending",
            "&7teleport requests to you.",
            "",
            "&aLeft-Click to Toggle (/tptoggle)"
        ));
        
        inv.setItem(14, createItem(Material.MINECART, "&c&lCarry Feature", 
            "&7Disable the Carry feature so",
            "&7others cannot pick you up.",
            "",
            "&aLeft-Click to Toggle (/carry toggle)"
        ));
        
        inv.setItem(16, createItem(Material.NAME_TAG, "&6&lScoreboard", 
            "&7Hide the scoreboard display",
            "&7on the right side of your screen.",
            "",
            "&aLeft-Click to Toggle (/sb toggle)"
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
        
        if (slot == 10) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "msgtoggle");
        } else if (slot == 12) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "tptoggle");
        } else if (slot == 14) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "carry toggle");
        } else if (slot == 16) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "sb toggle");
        }
    }
}
