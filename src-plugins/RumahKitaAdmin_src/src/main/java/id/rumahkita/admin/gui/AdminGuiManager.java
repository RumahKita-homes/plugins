package id.rumahkita.admin.gui;

import id.rumahkita.admin.RumahKitaAdmin;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public class AdminGuiManager implements Listener {

    private final RumahKitaAdmin plugin;

    public static final String TITLE_MAIN = ChatColor.DARK_RED + "" + ChatColor.BOLD + "RumahKita Admin";
    public static final String TITLE_PLAYERS = ChatColor.BLUE + "Player List";
    public static final String TITLE_SERVER = ChatColor.DARK_GRAY + "Server Management";
    public static final String TITLE_ACTION = ChatColor.RED + "Actions: ";
    public static final String TITLE_REASON = ChatColor.DARK_RED + "Select Reason";
    public static final String TITLE_TIME = ChatColor.GOLD + "Select Duration";

    // Track state for sub-menus
    private final Map<UUID, String> targetPlayerMap = new HashMap<>();
    private final Map<UUID, String> currentActionMap = new HashMap<>();

    public AdminGuiManager(RumahKitaAdmin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(11, createItem(Material.PLAYER_HEAD, ChatColor.AQUA + "Player Management", 
            ChatColor.GRAY + "Click to manage online players."));
        
        inv.setItem(15, createItem(Material.COMMAND_BLOCK, ChatColor.GOLD + "Server Management", 
            ChatColor.GRAY + "Click to manage server systems."));

        player.openInventory(inv);
    }

    public void openPlayerList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PLAYERS);
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(p);
                meta.setDisplayName(ChatColor.YELLOW + p.getName());
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to manage this player."));
                skull.setItemMeta(meta);
            }
            inv.addItem(skull);
        }
        player.openInventory(inv);
    }

    public void openServerManagement(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SERVER);
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(10, createItem(Material.REDSTONE_BLOCK, ChatColor.RED + "Toggle Maintenance", ChatColor.GRAY + "Runs: /rka maintenance"));
        inv.setItem(12, createItem(Material.IRON_BARS, ChatColor.YELLOW + "Toggle ChatLock", ChatColor.GRAY + "Runs: /rka chatlock"));
        inv.setItem(14, createItem(Material.PAPER, ChatColor.WHITE + "Clear Chat", ChatColor.GRAY + "Runs: /rka clearchat"));
        inv.setItem(16, createItem(Material.BARRIER, ChatColor.DARK_RED + "Restart Server", ChatColor.GRAY + "Opens restart menu."));

        player.openInventory(inv);
    }

    public void openPlayerActions(Player player, String targetName) {
        targetPlayerMap.put(player.getUniqueId(), targetName);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ACTION + targetName);
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(10, createItem(Material.PAPER, ChatColor.YELLOW + "Warn Player", ChatColor.GRAY + "Click to warn."));
        inv.setItem(11, createItem(Material.WOODEN_SWORD, ChatColor.RED + "Kick Player", ChatColor.GRAY + "Click to kick."));
        inv.setItem(12, createItem(Material.STRING, ChatColor.GOLD + "Mute Player", ChatColor.GRAY + "Click to mute."));
        inv.setItem(13, createItem(Material.IRON_BARS, ChatColor.DARK_GRAY + "Jail Player", ChatColor.GRAY + "Click to jail."));
        inv.setItem(14, createItem(Material.ICE, ChatColor.AQUA + "Freeze Player", ChatColor.GRAY + "Click to toggle freeze."));
        inv.setItem(15, createItem(Material.ENDER_EYE, ChatColor.LIGHT_PURPLE + "Spectate Player", ChatColor.GRAY + "Click to spectate."));
        inv.setItem(16, createItem(Material.CHEST, ChatColor.GREEN + "Inspect Player", ChatColor.GRAY + "Click to inspect."));

        player.openInventory(inv);
    }

    public void openReasonMenu(Player player, String action) {
        currentActionMap.put(player.getUniqueId(), action);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_REASON);
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(10, createItem(Material.PAPER, ChatColor.YELLOW + "Inappropriate Behavior"));
        inv.setItem(12, createItem(Material.IRON_SWORD, ChatColor.RED + "Cheating/Hacking"));
        inv.setItem(14, createItem(Material.ROTTEN_FLESH, ChatColor.GOLD + "Spamming/Toxicity"));
        inv.setItem(16, createItem(Material.TNT, ChatColor.DARK_RED + "Griefing"));

        player.openInventory(inv);
    }

    public void openTimeMenu(Player player, String action) {
        currentActionMap.put(player.getUniqueId(), action);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_TIME);
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(10, createItem(Material.CLOCK, ChatColor.GREEN + "10 Minutes", ChatColor.GRAY + "10m"));
        inv.setItem(12, createItem(Material.CLOCK, ChatColor.YELLOW + "1 Hour", ChatColor.GRAY + "1h"));
        inv.setItem(14, createItem(Material.CLOCK, ChatColor.GOLD + "1 Day", ChatColor.GRAY + "1d"));
        inv.setItem(16, createItem(Material.BARRIER, ChatColor.RED + "Permanent", ChatColor.GRAY + "permanent"));

        player.openInventory(inv);
    }

    public void openRestartMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Restart Options");
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(10, createItem(Material.REDSTONE, ChatColor.RED + "10 Seconds", ChatColor.GRAY + "10s"));
        inv.setItem(12, createItem(Material.REDSTONE, ChatColor.GOLD + "1 Minute", ChatColor.GRAY + "1m"));
        inv.setItem(14, createItem(Material.REDSTONE, ChatColor.YELLOW + "5 Minutes", ChatColor.GRAY + "5m"));
        inv.setItem(16, createItem(Material.BARRIER, ChatColor.DARK_RED + "Cancel Restart"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.equals(TITLE_MAIN) && !title.equals(TITLE_PLAYERS) && !title.equals(TITLE_SERVER) 
            && !title.startsWith(TITLE_ACTION) && !title.equals(TITLE_REASON) && !title.equals(TITLE_TIME)
            && !title.equals(ChatColor.DARK_RED + "Restart Options")) {
            return;
        }

        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR || e.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String name = ChatColor.stripColor(meta.getDisplayName());
        String target = targetPlayerMap.get(player.getUniqueId());

        if (title.equals(TITLE_MAIN)) {
            if (name.equals("Player Management")) {
                openPlayerList(player);
            } else if (name.equals("Server Management")) {
                openServerManagement(player);
            }
        } 
        else if (title.equals(TITLE_PLAYERS)) {
            if (item.getType() == Material.PLAYER_HEAD) {
                openPlayerActions(player, name);
            }
        }
        else if (title.startsWith(TITLE_ACTION)) {
            if (name.equals("Warn Player")) {
                openReasonMenu(player, "warn");
            } else if (name.equals("Kick Player")) {
                openReasonMenu(player, "kick");
            } else if (name.equals("Mute Player")) {
                openTimeMenu(player, "mute");
            } else if (name.equals("Jail Player")) {
                openTimeMenu(player, "jail");
            } else if (name.equals("Freeze Player")) {
                player.closeInventory();
                player.chat("/rka freeze " + target);
            } else if (name.equals("Spectate Player")) {
                player.closeInventory();
                player.chat("/rka spectate " + target);
            } else if (name.equals("Inspect Player")) {
                player.closeInventory();
                player.chat("/rka inspect " + target);
            }
        }
        else if (title.equals(TITLE_REASON)) {
            String action = currentActionMap.get(player.getUniqueId());
            String reason = name; // Inappropriate Behavior, etc.
            player.closeInventory();
            if ("warn".equals(action)) {
                player.chat("/rka warn " + target + " " + reason);
            } else if ("kick".equals(action)) {
                player.chat("/rka kick " + target + " " + reason);
            }
        }
        else if (title.equals(TITLE_TIME)) {
            String action = currentActionMap.get(player.getUniqueId());
            String timeStr = "permanent";
            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                timeStr = ChatColor.stripColor(lore.get(0));
            }
            player.closeInventory();
            
            if ("permanent".equalsIgnoreCase(timeStr)) {
                if ("mute".equals(action)) player.chat("/rka mute " + target + " permanent");
                else if ("jail".equals(action)) player.chat("/rka jail " + target + " permanent");
                return;
            }

            if ("mute".equals(action)) {
                player.chat("/rka mute " + target + " " + timeStr + " Violation");
            } else if ("jail".equals(action)) {
                player.chat("/rka jail " + target + " " + timeStr);
            }
        }
        else if (title.equals(TITLE_SERVER)) {
            player.closeInventory();
            if (name.equals("Toggle Maintenance")) {
                player.chat("/rka maintenance toggle");
            } else if (name.equals("Toggle ChatLock")) {
                player.chat("/rka chatlock");
            } else if (name.equals("Clear Chat")) {
                player.chat("/rka clearchat");
            } else if (name.equals("Restart Server")) {
                openRestartMenu(player);
            }
        }
        else if (title.equals(ChatColor.DARK_RED + "Restart Options")) {
            player.closeInventory();
            if (name.equals("Cancel Restart")) {
                player.chat("/rka restart cancel");
                return;
            }
            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                String timeStr = ChatColor.stripColor(lore.get(0));
                player.chat("/rka restart " + timeStr);
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
