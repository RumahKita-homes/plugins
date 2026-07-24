package id.rumahkita.economy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.util.Base64;

public class PointsShopManager implements Listener {
    private RumahKitaEconomyRupiahPlugin plugin;
    private File file;
    private FileConfiguration config;
    private Map<String, ShopItem> items = new LinkedHashMap<>();

    public PointsShopManager(RumahKitaEconomyRupiahPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/points_shop.yml");
        this.reload();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            try {
                this.file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);
        this.items.clear();
        ConfigurationSection sec = this.config.getConfigurationSection("items");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    ItemStack item = this.itemFromBase64(sec.getString(key + ".item"));
                    long price = sec.getLong(key + ".price");
                    int slot = sec.getInt(key + ".slot", -1);
                    ShopItem si = new ShopItem(key, item, price, slot);
                    this.items.put(key, si);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load points shop item: " + key);
                }
            }
        }
    }

    public void save() {
        this.config.set("items", null);
        for (ShopItem si : this.items.values()) {
            this.config.set("items." + si.id + ".item", this.itemToBase64(si.item));
            this.config.set("items." + si.id + ".price", si.price);
            this.config.set("items." + si.id + ".slot", si.slot);
        }
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addItem(ItemStack item, long price, Player admin) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        ShopItem si = new ShopItem(id, item.clone(), price, -1);
        this.items.put(id, si);
        this.save();
        admin.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aItem added to Points Shop for &e" + plugin.formatNumber(price) + " Points&a!"));
    }

    public void openShop(Player p) {
        Inventory inv = Bukkit.createInventory(new PointsShopHolder(), 54, ChatColor.WHITE + "Points Shop");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r"));
        border.setItemMeta(bm);
        
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int i : borderSlots) {
            inv.setItem(i, border.clone());
        }
        
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lYour Information"));
        java.util.List<String> infoLore = new java.util.ArrayList<>();
        infoLore.add(ChatColor.translateAlternateColorCodes('&', "&7Points: &a" + plugin.formatNumber(plugin.getPoints(p.getUniqueId()))));
        im.setLore(infoLore);
        info.setItemMeta(im);
        inv.setItem(49, info);
        
        for (ShopItem si : this.items.values()) {
            ItemStack display = si.item.clone();
            ItemMeta meta = display.getItemMeta();
            java.util.List<String> lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m------------------------"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Price: &e" + plugin.formatNumber(si.price) + " Points"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to purchase!"));
            if (p.hasPermission("market.admin")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&c[Admin] Shift-Right-Click to remove"));
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
            
            if (si.slot >= 0 && si.slot < 54) {
                inv.setItem(si.slot, display);
            } else {
                int next = -1;
                for (int i = 0; i < 54; i++) {
                    if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                        next = i; break;
                    }
                }
                if (next != -1) inv.setItem(next, display);
            }
        }
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof PointsShopHolder) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            
            if (e.getRawSlot() < 0 || e.getRawSlot() >= e.getInventory().getSize()) return;
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE || clicked.getType() == Material.PAPER) return;
            
            ShopItem target = null;
            for (ShopItem si : this.items.values()) {
                int expectedSlot = si.slot;
                if (expectedSlot < 0 || expectedSlot >= 54) {
                }
                if (expectedSlot == e.getRawSlot()) {
                    target = si;
                    break;
                }
            }
            
            if (target == null) {
                for (ShopItem si : this.items.values()) {
                    if (si.slot < 0 || si.slot >= 54) {
                        if (si.item.getType() == clicked.getType()) {
                            target = si;
                            break;
                        }
                    }
                }
            }
            
            if (target == null) return;
            
            if (e.isShiftClick() && e.isRightClick() && p.hasPermission("market.admin")) {
                this.items.remove(target.id);
                this.save();
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cItem removed from shop."));
                this.openShop(p);
                return;
            }
            
            long pts = plugin.getPoints(p.getUniqueId());
            if (pts < target.price) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou don't have enough Points! Need: &e" + plugin.formatNumber(target.price)));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            
            plugin.takePoints(p.getUniqueId(), target.price);
            HashMap<Integer, ItemStack> left = p.getInventory().addItem(target.item.clone());
            for (ItemStack drop : left.values()) {
                p.getWorld().dropItem(p.getLocation(), drop);
            }
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYou successfully purchased this item for &e" + plugin.formatNumber(target.price) + " Points&a!"));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        }
    }

    public void openEditor(Player p) {
        Inventory inv = Bukkit.createInventory(new PointsShopEditorHolder(), 54, ChatColor.WHITE + "Points Shop Editor");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c[Border Dummy]"));
        border.setItemMeta(bm);
        
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int i : borderSlots) {
            inv.setItem(i, border.clone());
        }
        
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c[Info Dummy]"));
        info.setItemMeta(im);
        inv.setItem(49, info);
        
        for (ShopItem si : this.items.values()) {
            ItemStack display = si.item.clone();
            ItemMeta meta = display.getItemMeta();
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_id"), org.bukkit.persistence.PersistentDataType.STRING, si.id);
            display.setItemMeta(meta);
            
            if (si.slot >= 0 && si.slot < 54) {
                inv.setItem(si.slot, display);
            } else {
                int next = -1;
                for (int i = 0; i < 54; i++) {
                    if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                        next = i; break;
                    }
                }
                if (next != -1) inv.setItem(next, display);
            }
        }
        
        p.openInventory(inv);
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eYou are now editing the Points Shop layout. Drag and drop items. Close to save."));
    }

    @EventHandler
    public void onEditorClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof PointsShopEditorHolder) {
            for (ShopItem si : this.items.values()) si.slot = -1;
            
            for (int i = 0; i < 54; i++) {
                ItemStack item = e.getInventory().getItem(i);
                if (item != null && item.hasItemMeta()) {
                    String id = item.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "shop_id"), org.bukkit.persistence.PersistentDataType.STRING);
                    if (id != null) {
                        ShopItem si = this.items.get(id);
                        if (si != null) si.slot = i;
                    }
                }
            }
            this.save();
            e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&aPoints Shop layout saved!"));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PointsShopHolder) {
            for (int slot : e.getRawSlots()) {
                if (slot < e.getInventory().getSize()) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    private String itemToBase64(ItemStack item) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    private ItemStack itemFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static class PointsShopHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    public static class PointsShopEditorHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    public static class ShopItem {
        public String id;
        public ItemStack item;
        public long price;
        public int slot;
        public ShopItem(String id, ItemStack item, long price, int slot) {
            this.id = id;
            this.item = item;
            this.price = price;
            this.slot = slot;
        }
    }
}
