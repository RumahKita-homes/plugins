package id.rumahkita.pvp.view;

import id.rumahkita.minigames.RumahKitaMinigamesPlugin;
import id.rumahkita.pvp.RumahKitaPvP1v1Plugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class PvPKitManagerUI implements Listener {
    private final RumahKitaMinigamesPlugin plugin;
    private final RumahKitaPvP1v1Plugin pvp;
    private final String LIST_TITLE = ChatColor.WHITE + "PvP Kit Manager";
    private final String EDIT_TITLE_PREFIX = ChatColor.WHITE + "Editing Kit: ";
    
    private final List<String> DEFAULT_KITS = Arrays.asList("NETHERITE", "CRYSTAL", "DIAMOND", "IRON", "UHC", "NODEBUFF", "SUMO", "BOW", "AXE", "TRIDENT");

    public PvPKitManagerUI(RumahKitaMinigamesPlugin plugin, RumahKitaPvP1v1Plugin pvp) {
        this.plugin = plugin;
        this.pvp = pvp;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openKitList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LIST_TITLE);

        List<String> allKits = new ArrayList<>(DEFAULT_KITS);
        ConfigurationSection kitSec = pvp.getConfig().getConfigurationSection("kits");
        if (kitSec != null) {
            for (String custom : kitSec.getKeys(false)) {
                if (!allKits.contains(custom.toUpperCase())) {
                    allKits.add(custom.toUpperCase());
                }
            }
        }

        int slot = 0;
        for (String kitName : allKits) {
            if (slot >= 45) break;
            
            if (pvp.getConfig().getBoolean("deleted-default-kits." + kitName, false)) {
                continue; // Skip if it's a deleted default kit
            }
            
            boolean isCustom = kitSec != null && kitSec.contains(kitName);
            boolean isDisabled = pvp.getConfig().getBoolean("disabled-kits." + kitName, false);
            
            Material icon = isCustom ? Material.CHEST : Material.DIAMOND_SWORD;
            if (isDisabled) icon = Material.BARRIER;
            
            inv.setItem(slot, createItem(icon, "&d&l" + kitName, 
                    "&7Type: " + (isCustom ? "&eCustom/Modified" : "&bDefault Built-in"),
                    "&7Status: " + (isDisabled ? "&cDISABLED" : "&aENABLED"),
                    "",
                    "&fLeft-Click &7to Edit Items",
                    "&fRight-Click &7to Toggle Enable/Disable"
            ));
            slot++;
        }

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        
        inv.setItem(49, createItem(Material.EMERALD_BLOCK, "&a&lCreate New Kit", "&7Create a new custom kit", "&7using your current inventory."));
        inv.setItem(53, createItem(Material.ARROW, "&c&lBack", "&7Return to main settings"));

        player.openInventory(inv);
    }

    public void openKitEditor(Player player, String kitName) {
        Inventory inv = Bukkit.createInventory(null, 54, EDIT_TITLE_PREFIX + kitName);
        
        if (pvp.getConfig().contains("kits." + kitName + ".inventory")) {
            List<?> invList = pvp.getConfig().getList("kits." + kitName + ".inventory");
            if (invList != null) {
                for (int i = 0; i < Math.min(36, invList.size()); i++) {
                    Object obj = invList.get(i);
                    if (obj instanceof ItemStack) inv.setItem(i, (ItemStack) obj);
                }
            }
            List<?> armorList = pvp.getConfig().getList("kits." + kitName + ".armor");
            if (armorList != null) {
                if (armorList.size() > 0 && armorList.get(0) instanceof ItemStack) inv.setItem(36, (ItemStack) armorList.get(0)); // Boots (index 0 usually in getArmorContents)
                if (armorList.size() > 1 && armorList.get(1) instanceof ItemStack) inv.setItem(37, (ItemStack) armorList.get(1)); // Legs
                if (armorList.size() > 2 && armorList.get(2) instanceof ItemStack) inv.setItem(38, (ItemStack) armorList.get(2)); // Chest
                if (armorList.size() > 3 && armorList.get(3) instanceof ItemStack) inv.setItem(39, (ItemStack) armorList.get(3)); // Helmet
            }
        } else {
        }

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 40; i < 45; i++) inv.setItem(i, glass); // separators
        for (int i = 45; i < 54; i++) inv.setItem(i, glass); // bottom row
        
        inv.setItem(40, createItem(Material.LEATHER_BOOTS, "&eArmor: Boots", "&7Place boots in slot 36 (left)"));
        inv.setItem(41, createItem(Material.LEATHER_LEGGINGS, "&eArmor: Leggings", "&7Place leggings in slot 37"));
        inv.setItem(42, createItem(Material.LEATHER_CHESTPLATE, "&eArmor: Chestplate", "&7Place chestplate in slot 38"));
        inv.setItem(43, createItem(Material.LEATHER_HELMET, "&eArmor: Helmet", "&7Place helmet in slot 39"));

        inv.setItem(48, createItem(Material.LIME_DYE, "&a&lSAVE KIT", "&7Save these items to the kit"));
        inv.setItem(49, createItem(Material.RED_DYE, "&c&lDELETE CUSTOM KIT", "&7Delete this kit (reverts to default if applicable)"));
        inv.setItem(50, createItem(Material.BARRIER, "&c&lCANCEL", "&7Discard changes"));

        player.openInventory(inv);
    }

    private void saveKitFromEditor(Player player, Inventory inv, String kitName) {
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) contents[i] = inv.getItem(i);
        
        ItemStack[] armor = new ItemStack[4];
        armor[0] = inv.getItem(36); // Boots
        armor[1] = inv.getItem(37); // Legs
        armor[2] = inv.getItem(38); // Chest
        armor[3] = inv.getItem(39); // Helmet
        
        String path = "kits." + kitName;
        pvp.getConfig().set(path + ".inventory", contents);
        pvp.getConfig().set(path + ".armor", armor);
        pvp.saveConfig();
        
        player.sendMessage(ChatColor.GREEN + "Kit " + kitName + " saved successfully!");
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
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(LIST_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            if (clicked.getType() == Material.ARROW) {
                player.performCommand("pvp admin");
                return;
            }
            if (clicked.getType() == Material.EMERALD_BLOCK) {
                player.performCommand("pvp createkit CUSTOM");
                openKitList(player);
                return;
            }
            
            if (event.getClick() == ClickType.RIGHT) {
                boolean isDis = pvp.getConfig().getBoolean("disabled-kits." + name, false);
                pvp.getConfig().set("disabled-kits." + name, !isDis);
                pvp.saveConfig();
                openKitList(player);
            } else if (event.getClick() == ClickType.LEFT) {
                openKitEditor(player, name);
            }
        } 
        else if (title.startsWith(EDIT_TITLE_PREFIX)) {
            String kitName = title.substring(EDIT_TITLE_PREFIX.length());
            
            if (event.getRawSlot() >= 40 && event.getRawSlot() < 54) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null) return;
                
                if (clicked.getType() == Material.LIME_DYE) {
                    saveKitFromEditor(player, event.getInventory(), kitName);
                    openKitList(player);
                } else if (clicked.getType() == Material.RED_DYE) {
                    pvp.getConfig().set("kits." + kitName, null);
                    if (DEFAULT_KITS.contains(kitName)) {
                        pvp.getConfig().set("deleted-default-kits." + kitName, true);
                        player.sendMessage(ChatColor.RED + "Default kit " + kitName + " has been permanently hidden/deleted!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Custom kit " + kitName + " deleted!");
                    }
                    pvp.saveConfig();
                    openKitList(player);
                } else if (clicked.getType() == Material.BARRIER) {
                    openKitList(player);
                }
            }
        }
    }
}
