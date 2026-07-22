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
    public static final String TITLE_CF = ChatColor.YELLOW + "Coinflip Settings";
    public static final String TITLE_RPS = ChatColor.AQUA + "RPS Settings";

    public RkmgGuiManager(RumahKitaMinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBorder(inv);
        
        inv.setItem(19, createItem(Material.GOLD_NUGGET, "&e&lCoinflip Admin", 
            "&7Manage Coinflip settings,",
            "&7win chance, taxes, etc.",
            "",
            "&aLeft-Click to manage CF"
        ));
        
        inv.setItem(21, createItem(Material.PAPER, "&b&lRPS Admin", 
            "&7Manage Rock Paper Scissors",
            "&7settings and taxes.",
            "",
            "&aLeft-Click to manage RPS"
        ));

        inv.setItem(23, createItem(Material.RED_BANNER, "&c&lCapture The Flag", 
            "&7Manage CTF arenas,",
            "&7Red/Blue flag positions,",
            "&7and force start/stop.",
            "",
            "&aLeft-Click to open /ctf help"
        ));
        
        inv.setItem(25, createItem(Material.IRON_SWORD, "&e&lPvP 1v1 Manager", 
            "&7View PvP arena queues",
            "&7and manage setups.",
            "",
            "&aLeft-Click to open /pvp help"
        ));
        player.openInventory(inv);
    }

    public void openCfSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CF);
        fillBorder(inv);
        
        double tax = plugin.getConfig().getDouble("coinflip.tax_percentage", 5.0);
        inv.setItem(11, createItem(Material.GOLD_INGOT, "&e&lTax Percentage", 
            "&7Current Tax: &a" + tax + "%",
            "",
            "&aLeft-Click to +1%",
            "&cRight-Click to -1%",
            "&eShift-Click to reset to 5%"
        ));
        
        inv.setItem(15, createItem(Material.BARRIER, "&c&lBack", "&7Return to Dashboard"));
        player.openInventory(inv);
    }

    public void openRpsSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_RPS);
        fillBorder(inv);
        
        double tax = plugin.getConfig().getDouble("rps.tax_percentage", 5.0);
        inv.setItem(11, createItem(Material.EMERALD, "&a&lTax Percentage", 
            "&7Current Tax: &a" + tax + "%",
            "",
            "&aLeft-Click to +1%",
            "&cRight-Click to -1%",
            "&eShift-Click to reset to 5%"
        ));
        
        inv.setItem(15, createItem(Material.BARRIER, "&c&lBack", "&7Return to Dashboard"));
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
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.equals(TITLE)) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (slot == 19) {
                openCfSettings(player);
            } else if (slot == 21) {
                openRpsSettings(player);
            } else if (slot == 23) {
                player.closeInventory();
                Bukkit.dispatchCommand(player, "ctf help");
            } else if (slot == 25) {
                player.closeInventory();
                Bukkit.dispatchCommand(player, "pvp help");
            }
        } else if (title.equals(TITLE_CF)) {
            event.setCancelled(true);
            if (event.getSlot() == 11) {
                double tax = plugin.getConfig().getDouble("coinflip.tax_percentage", 5.0);
                if (event.isShiftClick()) tax = 5.0;
                else if (event.isLeftClick()) tax = Math.min(100.0, tax + 1.0);
                else if (event.isRightClick()) tax = Math.max(0.0, tax - 1.0);
                
                plugin.getConfig().set("coinflip.tax_percentage", tax);
                plugin.saveConfig();
                openCfSettings(player); // refresh
            } else if (event.getSlot() == 15) {
                open(player);
            }
        } else if (title.equals(TITLE_RPS)) {
            event.setCancelled(true);
            if (event.getSlot() == 11) {
                double tax = plugin.getConfig().getDouble("rps.tax_percentage", 5.0);
                if (event.isShiftClick()) tax = 5.0;
                else if (event.isLeftClick()) tax = Math.min(100.0, tax + 1.0);
                else if (event.isRightClick()) tax = Math.max(0.0, tax - 1.0);
                
                plugin.getConfig().set("rps.tax_percentage", tax);
                plugin.saveConfig();
                openRpsSettings(player); // refresh
            } else if (event.getSlot() == 15) {
                open(player);
            }
        }
    }
}
