package id.rumahkita.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EconomyAdminGui implements Listener {
    private final RumahKitaEconomyRupiahPlugin plugin;

    public static final String MAIN_TITLE = ChatColor.DARK_GREEN + "Economy Dashboard";
    public static final String BROWSER_TITLE = ChatColor.DARK_BLUE + "Player Bank Browser";
    public static final String DETAILS_TITLE = ChatColor.DARK_PURPLE + "Financial Editor: ";

    private final Map<UUID, Integer> browserPages = new HashMap<>();
    private final Map<UUID, UUID> viewingDetails = new HashMap<>();

    public EconomyAdminGui(RumahKitaEconomyRupiahPlugin plugin) {
        this.plugin = plugin;
    }

        public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);
        fillBorder(inv);
        
        long totalWealth = 0;
        ConfigurationSection balances = plugin.getBalancesCfg().getConfigurationSection("balances");
        if (balances != null) {
            for (String key : balances.getKeys(false)) {
                totalWealth += balances.getLong(key);
            }
        }
        
        inv.setItem(20, createItem(Material.PLAYER_HEAD, "&b&lPlayer Bank Browser", "&7List of all player bank", "&7accounts on the server."));
        inv.setItem(24, createItem(Material.GOLD_INGOT, "&6&lMarket Manager", "&7Force refresh market stock,", "&7edit prices and items."));
        inv.setItem(31, createItem(Material.EMERALD_BLOCK, "&a&lServer Wealth Stats", "&7Total Money in Circulation:", "&e" + plugin.formatRp(totalWealth)));
        inv.setItem(38, createItem(Material.CHEST, "&d&lSellHand / SellAll", "&7Toggle global sellhand", "&7and sellall availability."));
        inv.setItem(42, createItem(Material.PAPER, "&c&lEconomy Config", "&7Economy settings,", "&7starting money, taxes, etc."));
        inv.setItem(49, createItem(Material.BARRIER, "&c&lClose Menu", "&7Close this dashboard."));
        
        player.openInventory(inv);
    }

    public void openBrowser(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, BROWSER_TITLE);
        browserPages.put(player.getUniqueId(), page);
        
        List<Map.Entry<UUID, Long>> accounts = new ArrayList<>();
        ConfigurationSection balances = plugin.getBalancesCfg().getConfigurationSection("balances");
        if (balances != null) {
            for (String key : balances.getKeys(false)) {
                try {
                    accounts.add(new AbstractMap.SimpleEntry<>(UUID.fromString(key), balances.getLong(key)));
                } catch (Exception ignored) {}
            }
        }
        
        accounts.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        int start = page * 45;
        int end = Math.min(start + 45, accounts.size());
        
        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Long> entry = accounts.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            inv.setItem(i - start, createItem(Material.PLAYER_HEAD, "&e" + (op.getName() != null ? op.getName() : "Unknown"),
                "&7Balance: &a" + plugin.formatRp(entry.getValue()),
                "",
                "&aClick to manage this player's funds"
            ));
        }
        
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "&aPrevious Page"));
        }
        inv.setItem(49, createItem(Material.BARRIER, "&cBack to Dashboard"));
        if (end < accounts.size()) {
            inv.setItem(53, createItem(Material.ARROW, "&aNext Page"));
        }
        
        player.openInventory(inv);
    }

    public void openDetails(Player player, UUID targetUuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
        Inventory inv = Bukkit.createInventory(null, 54, DETAILS_TITLE + (op.getName() != null ? op.getName() : "Unknown"));
        viewingDetails.put(player.getUniqueId(), targetUuid);
        
        fillBorder(inv);
        
        long balance = plugin.getBalance(targetUuid);
        
        inv.setItem(13, createItem(Material.PAPER, "&bAccount Information", 
            "&7Owner: &f" + op.getName(),
            "&7Balance: &a" + plugin.formatRp(balance)
        ));
        
        inv.setItem(29, createItem(Material.EMERALD, "&aDeposit (Add Funds)", 
            "&7Left-Click: &a+ Rp 10.000",
            "&7Right-Click: &a+ Rp 100.000"
        ));
        
        inv.setItem(33, createItem(Material.REDSTONE, "&cWithdraw (Remove Funds)", 
            "&7Left-Click: &c- Rp 10.000",
            "&7Right-Click: &c- Rp 100.000"
        ));
        
        inv.setItem(40, createItem(Material.TNT, "&4&lReset Balance", "&7Click to set this player's", "&7balance to Rp 0!"));
        
        inv.setItem(49, createItem(Material.ARROW, "&cBack to Browser"));
        
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
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (title.equals(MAIN_TITLE)) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (slot == 29) {
                openBrowser(player, 0);
            } else if (slot == 31) {
                player.closeInventory();
                player.performCommand("market admin");
            }
        } 
        else if (title.equals(BROWSER_TITLE)) {
            event.setCancelled(true);
            int slot = event.getSlot();
            int page = browserPages.getOrDefault(player.getUniqueId(), 0);
            
            if (slot == 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                openBrowser(player, page - 1);
            } else if (slot == 53 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                openBrowser(player, page + 1);
            } else if (slot == 49) {
                openMain(player);
            } else if (slot < 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                // Get sorted accounts
                List<Map.Entry<UUID, Long>> accounts = new ArrayList<>();
                ConfigurationSection balances = plugin.getBalancesCfg().getConfigurationSection("balances");
                if (balances != null) {
                    for (String key : balances.getKeys(false)) {
                        try {
                            accounts.add(new AbstractMap.SimpleEntry<>(UUID.fromString(key), balances.getLong(key)));
                        } catch (Exception ignored) {}
                    }
                }
                accounts.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
                int index = (page * 45) + slot;
                if (index < accounts.size()) {
                    openDetails(player, accounts.get(index).getKey());
                }
            }
        }
        else if (title.startsWith(DETAILS_TITLE)) {
            event.setCancelled(true);
            UUID target = viewingDetails.get(player.getUniqueId());
            if (target == null) {
                player.closeInventory();
                return;
            }
            
            int slot = event.getSlot();
            if (slot == 49) {
                openBrowser(player, browserPages.getOrDefault(player.getUniqueId(), 0));
            } else if (slot == 29) { // Deposit
                long amount = event.isLeftClick() ? 10000 : 100000;
                plugin.addBalance(target, amount);
                player.sendMessage(ChatColor.GREEN + "Successfully deposited " + plugin.formatRp(amount));
                openDetails(player, target);
            } else if (slot == 33) { // Withdraw
                long amount = event.isLeftClick() ? 10000 : 100000;
                long bal = plugin.getBalance(target);
                if (bal >= amount) {
                    plugin.takeBalance(target, amount);
                    player.sendMessage(ChatColor.GREEN + "Successfully withdrew " + plugin.formatRp(amount));
                } else {
                    player.sendMessage(ChatColor.RED + "Insufficient balance.");
                }
                openDetails(player, target);
            } else if (slot == 40) { // Reset
                long bal = plugin.getBalance(target);
                plugin.takeBalance(target, bal);
                player.sendMessage(ChatColor.GREEN + "Successfully reset balance.");
                openDetails(player, target);
            }
        }
    }
}
