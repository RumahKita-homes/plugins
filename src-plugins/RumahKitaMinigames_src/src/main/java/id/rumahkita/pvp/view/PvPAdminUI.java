package id.rumahkita.pvp.view;

import id.rumahkita.minigames.RumahKitaMinigamesPlugin;
import id.rumahkita.pvp.RumahKitaPvP1v1Plugin;
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
import org.bukkit.configuration.ConfigurationSection;

public class PvPAdminUI implements Listener {
    private final RumahKitaMinigamesPlugin plugin;
    private final RumahKitaPvP1v1Plugin pvp;
    private final String UI_TITLE = ChatColor.WHITE + "PvP Settings Admin";

    public PvPAdminUI(RumahKitaMinigamesPlugin plugin, RumahKitaPvP1v1Plugin pvp) {
        this.plugin = plugin;
        this.pvp = pvp;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, UI_TITLE);

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        inv.setItem(10, createItem(Material.DIAMOND_SWORD, "&b&lSpawn 1 (Player 1)", 
                "&7Current: " + getPos("arena.spawn1"),
                "&fClick &7to set at your location."));
        inv.setItem(11, createItem(Material.IRON_SWORD, "&f&lSpawn 2 (Player 2)", 
                "&7Current: " + getPos("arena.spawn2"),
                "&fClick &7to set at your location."));
                
        inv.setItem(15, createItem(Material.WOODEN_AXE, "&c&lBoundary Pos 1", 
                "&7Current: " + getPos("arena.pos1"),
                "&fClick &7to set at your location."));
        inv.setItem(16, createItem(Material.STONE_AXE, "&c&lBoundary Pos 2", 
                "&7Current: " + getPos("arena.pos2"),
                "&fClick &7to set at your location."));

        boolean healOnEnd = pvp.getConfig().getBoolean("match.heal-on-end", true);
        inv.setItem(28, createItem(healOnEnd ? Material.LIME_DYE : Material.RED_DYE, "&a&lHeal On End", 
                "&7Status: " + (healOnEnd ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));
                
        boolean preventDeath = pvp.getConfig().getBoolean("match.prevent-real-death", true);
        inv.setItem(29, createItem(preventDeath ? Material.LIME_DYE : Material.RED_DYE, "&e&lPrevent Real Death", 
                "&7Status: " + (preventDeath ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));
                
        boolean clearFire = pvp.getConfig().getBoolean("match.clear-fire-on-end", true);
        inv.setItem(30, createItem(clearFire ? Material.LIME_DYE : Material.RED_DYE, "&6&lClear Fire On End", 
                "&7Status: " + (clearFire ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));

        boolean timerEnabled = pvp.getConfig().getBoolean("match.timer-enabled", false);
        inv.setItem(31, createItem(timerEnabled ? Material.LIME_DYE : Material.RED_DYE, "&b&lTimer Enabled", 
                "&7Status: " + (timerEnabled ? "&aENABLED" : "&cDISABLED"),
                "&fClick &7to toggle."));

        int countdown = pvp.getConfig().getInt("match.countdown-seconds", 3);
        inv.setItem(33, createItem(Material.CLOCK, "&b&lStart Countdown", 
                "&7Current: &e" + countdown + "s",
                "",
                "&fLeft-Click &7to add +1s",
                "&fRight-Click &7to subtract -1s"));
                
        long endDelay = pvp.getConfig().getLong("arena.end-delay-ticks", 40);
        inv.setItem(34, createItem(Material.COMPASS, "&c&lEnd Delay", 
                "&7Current: &e" + (endDelay/20) + "s &8(" + endDelay + " ticks)",
                "",
                "&fLeft-Click &7to add +1s",
                "&fRight-Click &7to subtract -1s"));

        int totalKits = 0;
        ConfigurationSection kitSec = pvp.getConfig().getConfigurationSection("kits");
        if (kitSec != null) totalKits = kitSec.getKeys(false).size();
        
        inv.setItem(49, createItem(Material.CHEST, "&d&lManage Kits", 
                "&7Total Loaded Kits: &a" + totalKits,
                "",
                "&7Click here to open the",
                "&7Advanced Kit Manager GUI.",
                "&fClick &7to execute."));

        player.openInventory(inv);
    }
    
    private String getPos(String path) {
        if (pvp.getConfig().contains("arena.world") && pvp.getConfig().contains(path + ".x")) {
            return "&aSET (" + pvp.getConfig().getInt(path + ".x") + ", " + pvp.getConfig().getInt(path + ".y") + ", " + pvp.getConfig().getInt(path + ".z") + ")";
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
        
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (clicked.getType()) {
            case DIAMOND_SWORD:
                player.performCommand("pvp setspawn1");
                player.closeInventory();
                break;
            case IRON_SWORD:
                player.performCommand("pvp setspawn2");
                player.closeInventory();
                break;
            case WOODEN_AXE:
                player.performCommand("pvp setpos1");
                player.closeInventory();
                break;
            case STONE_AXE:
                player.performCommand("pvp setpos2");
                player.closeInventory();
                break;
            case CHEST:
                pvp.getKitUI().openKitList(player);
                break;
            case CLOCK:
                int cDown = pvp.getConfig().getInt("match.countdown-seconds", 3);
                if (event.getClick() == ClickType.LEFT) cDown += 1;
                else if (event.getClick() == ClickType.RIGHT) cDown = Math.max(0, cDown - 1);
                pvp.getConfig().set("match.countdown-seconds", cDown);
                pvp.saveConfig();
                open(player);
                break;
            case COMPASS:
                long eDelay = pvp.getConfig().getLong("arena.end-delay-ticks", 40);
                if (event.getClick() == ClickType.LEFT) eDelay += 20;
                else if (event.getClick() == ClickType.RIGHT) eDelay = Math.max(0, eDelay - 20);
                pvp.getConfig().set("arena.end-delay-ticks", eDelay);
                pvp.saveConfig();
                open(player);
                break;
            case LIME_DYE:
            case RED_DYE:
                if (name.contains("Heal On End")) {
                    pvp.getConfig().set("match.heal-on-end", !pvp.getConfig().getBoolean("match.heal-on-end", true));
                } else if (name.contains("Prevent Real Death")) {
                    pvp.getConfig().set("match.prevent-real-death", !pvp.getConfig().getBoolean("match.prevent-real-death", true));
                } else if (name.contains("Clear Fire On End")) {
                    pvp.getConfig().set("match.clear-fire-on-end", !pvp.getConfig().getBoolean("match.clear-fire-on-end", true));
                } else if (name.contains("Timer Enabled")) {
                    pvp.getConfig().set("match.timer-enabled", !pvp.getConfig().getBoolean("match.timer-enabled", false));
                }
                pvp.saveConfig();
                open(player);
                break;
            default:
                break;
        }
    }
}
