package id.rumahkita.utilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatsCommand implements CommandExecutor, Listener {

    private final RumahKitaUtilitiesPlugin plugin;

    public StatsCommand(RumahKitaUtilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            if (!player.hasPermission("rumahkita.admin")) {
                player.sendMessage(ChatColor.RED + "You can only view your own statistics with /" + label);
                return true;
            }
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || !target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            openStatsGui(player, target);
            return true;
        }

        openStatsGui(player, player);
        return true;
    }

    private void openStatsGui(Player viewer, org.bukkit.OfflinePlayer target) {
        String title = ChatColor.DARK_GRAY + (target.getName() != null ? target.getName() + "'s Stats" : "Statistics");
        if (title.length() > 32) title = title.substring(0, 32);
        Inventory inv = Bukkit.createInventory(null, 27, title);

        long ticksPlayed = 0;
        int fishCaught = 0;
        int itemsEnchanted = 0;
        
        int kills = 0;
        int deaths = 0;
        int mobKills = 0;
        int damageDealt = 0;
        int damageTaken = 0;
        
        long walk = 0;
        long sprint = 0;
        long jump = 0;

        try {
            ticksPlayed = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            fishCaught = target.getStatistic(Statistic.FISH_CAUGHT);
            itemsEnchanted = target.getStatistic(Statistic.ITEM_ENCHANTED);
            
            kills = target.getStatistic(Statistic.PLAYER_KILLS);
            deaths = target.getStatistic(Statistic.DEATHS);
            mobKills = target.getStatistic(Statistic.MOB_KILLS);
            damageDealt = target.getStatistic(Statistic.DAMAGE_DEALT) / 10;
            damageTaken = target.getStatistic(Statistic.DAMAGE_TAKEN) / 10;
            
            walk = target.getStatistic(Statistic.WALK_ONE_CM) / 100;
            sprint = target.getStatistic(Statistic.SPRINT_ONE_CM) / 100;
            jump = target.getStatistic(Statistic.JUMP);
        } catch (Exception ex) {}

        long hoursPlayed = ticksPlayed / (20 * 60 * 60);
        long minutesPlayed = (ticksPlayed / (20 * 60)) % 60;
        
        inv.setItem(11, createItem(Material.CLOCK, "&e&lGeneral Stats", Arrays.asList(
            "&7Time Played: &f" + hoursPlayed + "h " + minutesPlayed + "m",
            "&7Fish Caught: &f" + fishCaught,
            "&7Items Enchanted: &f" + itemsEnchanted
        )));
        double kda = deaths == 0 ? kills : (double) kills / deaths;

        inv.setItem(13, createItem(Material.IRON_SWORD, "&c&lCombat Stats", Arrays.asList(
            "&7Player Kills: &c" + kills,
            "&7Deaths: &c" + deaths,
            "&7KDA Ratio: &e" + String.format(java.util.Locale.US, "%.2f", kda),
            "&7Mob Kills: &f" + mobKills,
            "&7Damage Dealt: &f" + damageDealt + " hearts",
            "&7Damage Taken: &f" + damageTaken + " hearts"
        )));
        inv.setItem(15, createItem(Material.LEATHER_BOOTS, "&b&lMovement Stats", Arrays.asList(
            "&7Distance Walked: &f" + walk + "m",
            "&7Distance Sprinted: &f" + sprint + "m",
            "&7Total Jumps: &f" + jump
        )));
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<>());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        viewer.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("Stats") || event.getView().getTitle().contains("Statistics")) {
            event.setCancelled(true);
        }
    }
}
