package id.rumahkita.guilds;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AdminDashboardGui implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;
    private final GuildConfigGui configGui;

    public static final String MAIN_TITLE = ChatColor.DARK_RED + "Admin Dashboard";
    public static final String BROWSER_TITLE = ChatColor.DARK_BLUE + "Guild Browser";
    public static final String DETAILS_TITLE = ChatColor.DARK_PURPLE + "Guild Details: ";

    private final Map<UUID, Integer> browserPages = new HashMap<>();
    private final Map<UUID, Guild> viewingDetails = new HashMap<>();

    public AdminDashboardGui(RumahKitaGuildsPlugin plugin, GuildManager guildManager, GuildConfigGui configGui) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.configGui = configGui;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);
        
        fillBorder(inv);
        
        inv.setItem(11, createItem(Material.COMMAND_BLOCK, "&c&lGlobal Settings", "&7Configure guild creation cost,", "&7pajak, batas maksimal,", "&7and general configurations."));
        inv.setItem(15, createItem(Material.COMPASS, "&b&lGuild Browser", "&7View all existing guilds", "&7di server, cek log, member,", "&7dan manajemen kas guild."));
        
        player.openInventory(inv);
    }

    public void openBrowser(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, BROWSER_TITLE);
        browserPages.put(player.getUniqueId(), page);
        
        List<Guild> guilds = new ArrayList<>(guildManager.getGuilds());
        guilds.sort(Comparator.comparingLong(Guild::getCreatedAt).reversed());
        
        int start = page * 45;
        int end = Math.min(start + 45, guilds.size());
        
        for (int i = start; i < end; i++) {
            Guild g = guilds.get(i);
            String leaderName = guildManager.getOfflineName(g.getLeader());
            inv.setItem(i - start, createItem(Material.WHITE_BANNER, "&e" + g.getName(),
                "&7Tag: &b" + g.getTag(),
                "&7Leader: &f" + leaderName,
                "&7Members: &f" + g.size(),
                "&7Balance: &a" + plugin.getEconomyManager().format(g.getBalance()),
                "",
                "&aClick to monitor this guild!"
            ));
        }

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "&aPrevious Page"));
        }
        inv.setItem(49, createItem(Material.BARRIER, "&cBack to Dashboard"));
        if (end < guilds.size()) {
            inv.setItem(53, createItem(Material.ARROW, "&aNext Page"));
        }
        
        player.openInventory(inv);
    }

    public void openDetails(Player player, Guild guild) {
        String cleanTag = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', guild.getTag()));
        Inventory inv = Bukkit.createInventory(null, 27, DETAILS_TITLE + cleanTag);
        viewingDetails.put(player.getUniqueId(), guild);
        
        fillBorder(inv);
        
        inv.setItem(10, createItem(Material.PAPER, "&bGeneral Information", 
            "&7Nama: &f" + guild.getName(),
            "&7Leader: &f" + guildManager.getOfflineName(guild.getLeader()),
            "&7Total Member: &f" + guild.size(),
            "&7Balance: &a" + plugin.getEconomyManager().format(guild.getBalance())
        ));
        
        inv.setItem(12, createItem(Material.GOLD_INGOT, "&eManajemen Kas (Bank)", 
            "&7Klik Kiri: &aForce Deposit (+1000)",
            "&7Klik Kanan: &cForce Withdraw (-1000)"
        ));
        
        inv.setItem(14, createItem(Material.PLAYER_HEAD, "&aDaftar Member", "&7(Click to view all members)"));
        
        inv.setItem(16, createItem(Material.BOOK, "&dActivity Logs", "&7(Click to print the 10", "&7latest guild logs in chat)"));
        
        inv.setItem(19, createItem(Material.WOODEN_AXE, "&aSet Pos 1", "&7Set Pos 1 to your location", "&7currently."));
        inv.setItem(20, createItem(Material.WOODEN_AXE, "&aSet Pos 2", "&7Set Pos 2 to your location", "&7currently."));
        inv.setItem(21, createItem(Material.GRASS_BLOCK, "&eAdmin Claim", "&7Claim area from Pos 1 to Pos 2", "&7for this guild."));
        
        inv.setItem(24, createItem(Material.TNT, "&c&lFORCE DISBAND", "&7Forcefully disband this guild!"));
        
        inv.setItem(26, createItem(Material.ARROW, "&cBack to Browser"));
        
        player.openInventory(inv);
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
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
            if (slot == 11) {
                configGui.open(player);
            } else if (slot == 15) {
                openBrowser(player, 0);
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
            } else if (slot < 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.WHITE_BANNER) {
                List<Guild> guilds = new ArrayList<>(guildManager.getGuilds());
                guilds.sort(Comparator.comparingLong(Guild::getCreatedAt).reversed());
                int index = (page * 45) + slot;
                if (index < guilds.size()) {
                    openDetails(player, guilds.get(index));
                }
            }
        }
        else if (title.startsWith(DETAILS_TITLE)) {
            event.setCancelled(true);
            Guild guild = viewingDetails.get(player.getUniqueId());
            if (guild == null) {
                player.closeInventory();
                return;
            }
            
            int slot = event.getSlot();
            if (slot == 26) {
                openBrowser(player, browserPages.getOrDefault(player.getUniqueId(), 0));
            } else if (slot == 12) {
                if (event.isLeftClick()) {
                    guild.addBalance(1000);
                    player.sendMessage(ChatColor.GREEN + "Successfully deposited 1000 to " + guild.getName());
                } else if (event.isRightClick()) {
                    if (guild.getBalance() >= 1000) {
                        guild.withdrawBalance(1000);
                        player.sendMessage(ChatColor.RED + "Successfully withdrew 1000 from " + guild.getName());
                    } else {
                        player.sendMessage(ChatColor.RED + "Insufficient guild funds!");
                    }
                }
                openDetails(player, guild);
            } else if (slot == 16) {
                player.closeInventory();
                player.sendMessage(ChatColor.GOLD + "=== Activity Log: " + guild.getName() + " ===");
                List<String> logs = guild.getLogs();
                if (logs.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "No activity logs.");
                } else {
                    int count = 0;
                    for (int i = logs.size() - 1; i >= 0 && count < 10; i--, count++) {
                        player.sendMessage(ChatColor.GRAY + "" + ChatColor.WHITE + logs.get(i));
                    }
                }
            } else if (slot == 19) {
                player.performCommand("rkg pos1");
            } else if (slot == 20) {
                player.performCommand("rkg pos2");
            } else if (slot == 21) {
                String cleanTag = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', guild.getTag()));
                player.performCommand("rkg claim " + cleanTag);
            } else if (slot == 24) {
                player.closeInventory();
                Bukkit.dispatchCommand(player, "rkg disband " + guild.getTag() + " confirm");
                player.sendMessage(ChatColor.RED + "Guild successfully disbanded.");
            } else if (slot == 14) {
                player.closeInventory();
                player.sendMessage(ChatColor.AQUA + "=== Members " + guild.getName() + " ===");
                for (Map.Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                    player.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + op.getName() + ChatColor.YELLOW + " (" + entry.getValue().name() + ")");
                }
                player.sendMessage(ChatColor.GREEN + "To manage specific members, use the /rkg members command");
            }
        }
    }
}
