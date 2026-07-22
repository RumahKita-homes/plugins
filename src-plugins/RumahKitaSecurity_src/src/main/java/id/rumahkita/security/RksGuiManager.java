package id.rumahkita.security;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class RksGuiManager implements Listener {
    private final RumahKitaSecurityPlugin plugin;
    public static final String GUI_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "RumahKita Security";

    public RksGuiManager(RumahKitaSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }
        inv.setItem(11, createItem(Material.IRON_SWORD, ChatColor.RED + "AntiCheat", 
            ChatColor.GRAY + "Click to check AntiCheat status",
            ChatColor.YELLOW + "Runs: /rks ac status"));
        inv.setItem(15, createItem(Material.DIAMOND_ORE, ChatColor.AQUA + "AntiXray", 
            ChatColor.GRAY + "Click to check AntiXray status",
            ChatColor.YELLOW + "Runs: /rks xray status"));
        inv.setItem(20, createItem(Material.ENDER_EYE, ChatColor.LIGHT_PURPLE + "OreSpectator (Enable)", 
            ChatColor.GRAY + "Click to enable Spec2",
            ChatColor.YELLOW + "Runs: /rks spec on"));
        inv.setItem(24, createItem(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "OreSpectator (Disable)", 
            ChatColor.GRAY + "Click to disable Spec2",
            ChatColor.YELLOW + "Runs: /rks spec off"));
        inv.setItem(29, createItem(Material.REDSTONE, ChatColor.RED + "RamGuard Status", 
            ChatColor.GRAY + "Click to view RamGuard metrics",
            ChatColor.YELLOW + "Runs: /rks ramguard status"));
        inv.setItem(33, createItem(Material.WATER_BUCKET, ChatColor.BLUE + "RamGuard GC", 
            ChatColor.GRAY + "Click to run Garbage Collection",
            ChatColor.YELLOW + "Runs: /rks ramguard gc"));
        inv.setItem(38, createItem(Material.BARRIER, ChatColor.DARK_RED + "SecurityBan Status", 
            ChatColor.GRAY + "Click to view Ban Manager status",
            ChatColor.YELLOW + "Runs: /rks sec status"));
        inv.setItem(42, createItem(Material.REDSTONE_TORCH, ChatColor.GOLD + "Ban/TempBan Player", 
            ChatColor.GRAY + "Click for instructions to ban players"));
        inv.setItem(49, createItem(Material.COMMAND_BLOCK, ChatColor.GREEN + "Reload RKS", 
            ChatColor.GRAY + "Click to reload all RKS modules",
            ChatColor.YELLOW + "Runs: /rks reload"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

            int slot = event.getRawSlot();
                        switch (slot) {
                case 11:
                    player.closeInventory();
                    player.performCommand("rks ac status");
                    break;
                case 15:
                    player.closeInventory();
                    player.performCommand("rks xray status");
                    break;
                case 20:
                    player.closeInventory();
                    player.performCommand("rks spec on");
                    break;
                case 24:
                    player.closeInventory();
                    player.performCommand("rks spec off");
                    break;
                case 29:
                    player.closeInventory();
                    player.performCommand("rks ramguard status");
                    break;
                case 33:
                    player.closeInventory();
                    player.performCommand("rks ramguard gc");
                    break;
                case 38:
                    player.closeInventory();
                    player.performCommand("rks sec status");
                    break;
                case 42:
                    player.closeInventory();
                    player.sendMessage(ChatColor.GOLD + "To ban a player, use:");
                    player.sendMessage(ChatColor.YELLOW + "  /rks ban <player> [reason]");
                    player.sendMessage(ChatColor.YELLOW + "  /rks tempban <player> <duration> [reason]");
                    player.sendMessage(ChatColor.YELLOW + "  /rks sec gui <player>");
                    break;
                case 49:
                    player.closeInventory();
                    player.performCommand("rks reload");
                    break;
            }
        }
    }
}
