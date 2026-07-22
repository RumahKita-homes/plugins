package id.rumahkita.essentials;

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

public class EssentialsAdminGui implements Listener {
    private final RumahKitaEssentialsPlugin plugin;
    public static final String TITLE = ChatColor.DARK_AQUA + "Essentials Admin Dashboard";

    public EssentialsAdminGui(RumahKitaEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        
        fillBorder(inv);
        
        inv.setItem(13, createItem(Material.COMPASS, "&b&lWarp Browser", 
            "&7View the list of all available", 
            "&7warps on the server."
        ));
        
        inv.setItem(29, createItem(Material.RED_BED, "&a&lSpawn Manager", 
            "&7Left-Click: &fTeleport to Spawn", 
            "&7Right-Click: &cSet Spawn at your location"
        ));
        
        inv.setItem(31, createItem(Material.REDSTONE, "&c&lUtility Toggles", 
            "&7Left-Click: &fToggle Sleep",
            "&7Right-Click: &fToggle Carry",
            "&7Shift-Click: &fToggle Bansos"
        ));
        
        inv.setItem(33, createItem(Material.DIAMOND_SWORD, "&e&lAdmin Toolkits", 
            "&7Click to open menu:",
            "&f- Vanish",
            "&f- Fly",
            "&f- God Mode"
        ));
        
        inv.setItem(40, createItem(Material.CLOCK, "&6&lPerformance Monitor", 
            "&7Check server TPS & Ping",
            "&7Left-Click: &fTPS",
            "&7Right-Click: &fPing"
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
            if (event.isLeftClick()) {
                Bukkit.dispatchCommand(player, "spawn");
            } else if (event.isRightClick()) {
                Bukkit.dispatchCommand(player, "setspawn");
            }
        } else if (slot == 13) { 
            player.closeInventory();
            Bukkit.dispatchCommand(player, "warps");
            player.sendMessage(ChatColor.YELLOW + "Type /setwarp <name> to create a new warp.");
        } else if (slot == 31) { 
            player.closeInventory();
            if (event.isShiftClick()) {
                Bukkit.dispatchCommand(player, "rkbansos toggle");
            } else if (event.isRightClick()) {
                Bukkit.dispatchCommand(player, "rkcarry toggle");
            } else {
                Bukkit.dispatchCommand(player, "rksleep toggle");
            }
        } else if (slot == 33) { 
            player.closeInventory();
            openAdminToolkit(player);
        } else if (slot == 40) {
            player.closeInventory();
            if (event.isRightClick()) {
                Bukkit.dispatchCommand(player, "ping");
            } else {
                Bukkit.dispatchCommand(player, "tps");
            }
        }
    }
    
    private void openAdminToolkit(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Admin Toolkit");
        fillBorder(inv);
        inv.setItem(11, createItem(Material.FEATHER, "&bToggle Fly", "&7Click to /fly"));
        inv.setItem(13, createItem(Material.GLASS, "&aToggle Vanish", "&7Click to /vanish"));
        inv.setItem(15, createItem(Material.GOLDEN_APPLE, "&eToggle God Mode", "&7Click to /god"));
        inv.setItem(26, createItem(Material.ARROW, "&cBack"));
        player.openInventory(inv);
    }
    
    @EventHandler
    public void onToolkitClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GOLD + "Admin Toolkit")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        int slot = event.getSlot();
        if (slot == 11) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "fly");
        } else if (slot == 13) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "vanish");
        } else if (slot == 15) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "god");
        } else if (slot == 26) {
            open(player);
        }
    }
}
