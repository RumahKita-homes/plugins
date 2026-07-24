package id.rumahkita.ctf.view;

import id.rumahkita.minigames.RumahKitaMinigamesPlugin;
import id.rumahkita.ctf.RumahKitaCaptureFlag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CtfAdminUI implements Listener {
    private final RumahKitaMinigamesPlugin plugin;
    private final RumahKitaCaptureFlag ctf;
    private final String UI_TITLE = ChatColor.WHITE + "CTF Settings Admin";

    public CtfAdminUI(RumahKitaMinigamesPlugin plugin, RumahKitaCaptureFlag ctf) {
        this.plugin = plugin;
        this.ctf = ctf;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, UI_TITLE);

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        inv.setItem(10, createItem(Material.RED_WOOL, "&c&lTeam 1 Spawn", 
                "&7Current: " + getPos("spawns.side1"),
                "&fClick &7to set at your location."));
        inv.setItem(12, createItem(Material.BLUE_WOOL, "&b&lTeam 2 Spawn", 
                "&7Current: " + getPos("spawns.side2"),
                "&fClick &7to set at your location."));
        inv.setItem(14, createItem(Material.COMPASS, "&e&lExit Location", 
                "&7Current: " + getPos("spawns.exit"),
                "&fClick &7to set at your location."));
        inv.setItem(16, createItem(Material.BEACON, "&d&lCapture Zone (BOX)", 
                "&7Radius X: &a" + ctf.getConfig().getDouble("capture.radius-x", 13.0),
                "&7Radius Z: &a" + ctf.getConfig().getDouble("capture.radius-z", 13.0),
                "&fLeft-Click &7to set Primary Box here.",
                "&fRight-Click &7to ADD a new Capture Point.",
                "&fShift-Right-Click &7to CLEAR extra points."));

        int minP = ctf.getConfig().getInt("settings.min-players", 2);
        inv.setItem(28, createItem(Material.PLAYER_HEAD, "&a&lMinimum Players", 
                "&7Current: &e" + minP,
                "",
                "&fLeft-Click &7to add +1",
                "&fRight-Click &7to subtract -1"));
                
        int maxP = ctf.getConfig().getInt("settings.max-players", 20);
        inv.setItem(29, createItem(Material.ZOMBIE_HEAD, "&c&lMaximum Players", 
                "&7Current: &e" + maxP,
                "",
                "&fLeft-Click &7to add +1",
                "&fRight-Click &7to subtract -1"));
                
        int duration = ctf.getConfig().getInt("settings.duration-seconds", 300);
        inv.setItem(30, createItem(Material.CLOCK, "&e&lMatch Duration", 
                "&7Current: &e" + duration + "s",
                "",
                "&fLeft-Click &7to add +30s",
                "&fRight-Click &7to subtract -30s"));

        boolean preventDrop = ctf.getConfig().getBoolean("inventory.prevent-item-drop", true);
        inv.setItem(38, createItem(preventDrop ? Material.LIME_DYE : Material.RED_DYE, "&6&lPrevent Item Drop", 
                "&7Status: " + (preventDrop ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));
                
        boolean preventBreak = ctf.getConfig().getBoolean("inventory.prevent-block-break", true);
        inv.setItem(39, createItem(preventBreak ? Material.LIME_DYE : Material.RED_DYE, "&6&lPrevent Block Break", 
                "&7Status: " + (preventBreak ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));
                
        boolean freeze = ctf.getConfig().getBoolean("settings.freeze-before-start", true);
        inv.setItem(40, createItem(freeze ? Material.LIME_DYE : Material.RED_DYE, "&b&lFreeze Before Start", 
                "&7Status: " + (freeze ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));
                
        boolean kit = ctf.getConfig().getBoolean("kit.enabled", true);
        inv.setItem(41, createItem(kit ? Material.LIME_DYE : Material.RED_DYE, "&d&lKit Enabled", 
                "&7Status: " + (kit ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));
                
        boolean rewards = ctf.getConfig().getBoolean("rewards.enabled", true);
        inv.setItem(42, createItem(rewards ? Material.LIME_DYE : Material.RED_DYE, "&e&lRewards Enabled", 
                "&7Status: " + (rewards ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));

        inv.setItem(48, createItem(Material.EMERALD_BLOCK, "&a&lSTART EVENT", "&7Start the event immediately."));
        inv.setItem(50, createItem(Material.REDSTONE_BLOCK, "&c&lSTOP EVENT", "&7Force stop the event."));

        player.openInventory(inv);
    }
    
    private String getPos(String path) {
        if (ctf.getConfig().contains(path + ".world")) {
            return "&aSET (" + ctf.getConfig().getInt(path + ".x") + ", " + ctf.getConfig().getInt(path + ".y") + ", " + ctf.getConfig().getInt(path + ".z") + ")";
        }
        return "&cNOT SET";
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                java.util.List<String> loreList = new java.util.ArrayList<>();
                for (String l : lore) {
                    loreList.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(UI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        
        switch (clicked.getType()) {
            case RED_WOOL:
                player.performCommand("rkctf setside1");
                player.closeInventory();
                break;
            case BLUE_WOOL:
                player.performCommand("rkctf setside2");
                player.closeInventory();
                break;
            case COMPASS:
                player.performCommand("rkctf setexit");
                player.closeInventory();
                break;
            case BEACON:
                if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) {
                    player.performCommand("rkctf addcapturebox 13 13 " + (player.getLocation().getBlockY() - 5) + " " + (player.getLocation().getBlockY() + 5));
                } else if (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                    player.performCommand("rkctf clearcaptures");
                } else {
                    player.performCommand("rkctf setcapturebox 13 13 " + (player.getLocation().getBlockY() - 5) + " " + (player.getLocation().getBlockY() + 5));
                }
                player.closeInventory();
                break;
            case EMERALD_BLOCK:
                player.performCommand("rkctf forcestart");
                player.closeInventory();
                break;
            case REDSTONE_BLOCK:
                player.performCommand("rkctf stop");
                player.closeInventory();
                break;
            case PLAYER_HEAD:
                int minP = ctf.getConfig().getInt("settings.min-players", 2);
                if (event.getClick() == ClickType.LEFT) minP += 1;
                else if (event.getClick() == ClickType.RIGHT) minP = Math.max(2, minP - 1);
                ctf.getConfig().set("settings.min-players", minP);
                ctf.saveConfig();
                ctf.loadSettings();
                open(player);
                break;
            case ZOMBIE_HEAD:
                int maxP = ctf.getConfig().getInt("settings.max-players", 20);
                if (event.getClick() == ClickType.LEFT) maxP += 1;
                else if (event.getClick() == ClickType.RIGHT) maxP = Math.max(2, maxP - 1);
                ctf.getConfig().set("settings.max-players", maxP);
                ctf.saveConfig();
                ctf.loadSettings();
                open(player);
                break;
            case CLOCK:
                int dur = ctf.getConfig().getInt("settings.duration-seconds", 300);
                if (event.getClick() == ClickType.LEFT) dur += 30;
                else if (event.getClick() == ClickType.RIGHT) dur = Math.max(30, dur - 30);
                ctf.getConfig().set("settings.duration-seconds", dur);
                ctf.saveConfig();
                ctf.loadSettings();
                open(player);
                break;
            case LIME_DYE:
            case RED_DYE:
                String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                if (name.contains("Prevent Item Drop")) {
                    ctf.getConfig().set("inventory.prevent-item-drop", !ctf.getConfig().getBoolean("inventory.prevent-item-drop", true));
                } else if (name.contains("Prevent Block Break")) {
                    ctf.getConfig().set("inventory.prevent-block-break", !ctf.getConfig().getBoolean("inventory.prevent-block-break", true));
                } else if (name.contains("Freeze Before Start")) {
                    ctf.getConfig().set("settings.freeze-before-start", !ctf.getConfig().getBoolean("settings.freeze-before-start", true));
                } else if (name.contains("Kit Enabled")) {
                    ctf.getConfig().set("kit.enabled", !ctf.getConfig().getBoolean("kit.enabled", true));
                } else if (name.contains("Rewards Enabled")) {
                    ctf.getConfig().set("rewards.enabled", !ctf.getConfig().getBoolean("rewards.enabled", true));
                }
                ctf.saveConfig();
                ctf.loadSettings();
                open(player);
                break;
            default:
                break;
        }
    }
}
