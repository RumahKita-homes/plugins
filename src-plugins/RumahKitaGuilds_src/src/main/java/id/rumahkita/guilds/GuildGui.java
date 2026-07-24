/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.GuildRole;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuildGui
implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;

    public GuildGui(RumahKitaGuildsPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public void open(Player player) {
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder("main", ""), 27, (String)Text.color(this.plugin.getConfig().getString("gui.title", "&8RumahKita Guild")));
            inv.setItem(11, this.item(Material.DIAMOND, "&aCreate Guild", List.of("&7Usage:", "&e/guild create <TAG> <Name>")));
            inv.setItem(13, this.item(Material.BOOK, "&bGuild List", List.of("&7Click to view all guilds.")));
            inv.setItem(15, this.item(Material.PAPER, "&eInfo", List.of("&7You don't have a guild.", "&7Join a friend's guild or create your own.")));
            player.openInventory(inv);
            return;
        }

        Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder("main", ""), 27, (String)Text.color(this.plugin.getConfig().getString("gui.title", "&8Guild Menu")));
        GuildRole role = guild.getRole(player.getUniqueId());
        
        int totalKills = 0;
        int totalDeaths = 0;
        for (UUID memberUuid : guild.getMembers().keySet()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
            try {
                totalKills += op.getStatistic(org.bukkit.Statistic.PLAYER_KILLS);
                totalDeaths += op.getStatistic(org.bukkit.Statistic.DEATHS);
            } catch (Exception ex) {}
        }
        double guildKda = totalDeaths == 0 ? totalKills : (double) totalKills / totalDeaths;
        
        inv.setItem(10, this.item(Material.PAPER, "&b&lGuild Info", List.of(
            "&7Name: &f" + guild.getName(), 
            "&7Tag: &b" + guild.getTag(), 
            "&7Role: &e" + role.displayName(this.plugin), 
            "&7Members: &f" + guild.size() + "/" + this.guildManager.getMaxMembers(guild),
            "&7Balance: &e$" + this.plugin.getEconomyManager().format(guild.getBalance()),
            "&7Guild KDA: &e" + String.format(java.util.Locale.US, "%.2f", guildKda),
            "&7Vault Level: &f" + guild.getVaultLevel()
        )));
        
        inv.setItem(11, this.item(Material.PLAYER_HEAD, "&e&lMembers", List.of("&7Click to view guild members.")));
        
        inv.setItem(12, this.item(Material.COMPASS, "&a&lGuild Home", List.of("&7Click to teleport to guild home.")));
        
        inv.setItem(13, this.item(Material.GOLD_INGOT, "&e&lGuild Bank", List.of("&7Click to open Guild Bank.")));
        
        inv.setItem(14, this.item(Material.CHEST, "&6&lGuild Vault", List.of("&7Click to open vault.")));
        
        inv.setItem(15, this.item(Material.ENCHANTING_TABLE, "&d&lGuild Upgrades", List.of("&7Click to open upgrades.")));
        
        inv.setItem(16, this.item(Material.BOOK, "&b&lGuild List", List.of("&7Click to view all guilds on server.")));
        
        if (role == GuildRole.LEADER) {
            inv.setItem(21, this.item(Material.EMERALD, "&2&lManage Guild", List.of(
                "&7Admin Commands:",
                "&e/guild invite <player>",
                "&e/guild kick <player>",
                "&e/guild deposit <amount>",
                "&e/guild withdraw <amount>"
            )));
            inv.setItem(23, this.item(Material.NAME_TAG, "&c&lLeader Actions", List.of(
                "&7Leader Commands:",
                "&e/guild rename <name>",
                "&e/guild settag <tag>",
                "&e/guild disband"
            )));
        } else if (role.atLeast(GuildRole.ADMIN)) {
            inv.setItem(22, this.item(Material.EMERALD, "&2&lManage Guild", List.of(
                "&7Admin Commands:",
                "&e/guild invite <player>",
                "&e/guild kick <player>",
                "&e/guild deposit <amount>",
                "&e/guild withdraw <amount>"
            )));
        }

        player.openInventory(inv);
    }

    public void openGuildList(Player player) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder("list", ""), (int)54, (String)Text.color(this.plugin.getConfig().getString("gui.list-title", "&8Guild List")));
        int slot = 0;
        
        List<Guild> guilds = new ArrayList<>(this.guildManager.getGuilds());
        guilds.sort(java.util.Comparator.comparingLong(Guild::getCreatedAt));
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        for (Guild guild : guilds) {
            if (slot >= 45) break;
            List<String> lore = new ArrayList<>();
            lore.add("&7Leader: &f" + this.guildManager.getOfflineName(guild.getLeader()));
            lore.add("&7Members: &f" + guild.size() + "/" + this.guildManager.getMaxMembers(guild));
            lore.add("&7Created: &e" + sdf.format(new java.util.Date(guild.getCreatedAt())));
            lore.add("");
            lore.add("&eClick to view members");
            
            inv.setItem(slot++, this.item(Material.WHITE_BANNER, "&b" + guild.getName() + " &8[&f" + guild.getTag() + "&8]", lore));
        }
        inv.setItem(49, this.item(Material.ARROW, "&cBack", List.of("&7Back to guild menu.")));
        player.openInventory(inv);
    }

    public void openBank(Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder("bank", ""), (int)27, (String)Text.color("&8Guild Bank"));
        
        List<String> balLore = new ArrayList<>();
        balLore.add("&7Total Kekayaan Guild:");
        if (this.plugin.getEconomyManager() != null) {
            balLore.add("&a" + this.plugin.getEconomyManager().format(guild.getBalance()));
        } else {
            balLore.add("&cEconomy disabled");
        }
        inv.setItem(13, this.item(Material.GOLD_BLOCK, "&6&lGuild Wealth", balLore));
        
        inv.setItem(11, this.item(Material.EMERALD, "&a&lDeposit", List.of("&7Click to deposit money", "&7ke dalam Guild Bank.")));
        
        GuildRole role = guild.getRole(player.getUniqueId());
        if (role == GuildRole.MEMBER) {
            inv.setItem(15, this.item(Material.BARRIER, "&c&lWithdraw", List.of("&7Hanya Leader dan Admin", "&7who can withdraw money", "&7dari Guild Bank.")));
        } else {
            inv.setItem(15, this.item(Material.DIAMOND, "&b&lWithdraw", List.of("&7Click to withdraw money", "&7dari Guild Bank.")));
        }
        
        inv.setItem(26, this.item(Material.ARROW, "&cBack", List.of("&7Back to guild menu.")));
        player.openInventory(inv);
    }

    public void openMembers(Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory((InventoryHolder)new Holder("members", guild.getTag()), (int)54, (String)Text.color(this.plugin.getConfig().getString("gui.members-title", "&8Guild Members")));
        int slot = 0;
        
        List<Map.Entry<UUID, GuildRole>> members = new ArrayList<>(guild.getMembers().entrySet());
        members.sort(java.util.Comparator.comparingLong(e -> guild.getJoinedAt(e.getKey())));
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        for (Map.Entry<UUID, GuildRole> entry : members) {
            if (slot >= 45) break;
            UUID uuid = entry.getKey();
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : "Unknown";
            
            List<String> lore = new ArrayList<>();
            lore.add("&7Role: &e" + entry.getValue().displayName(this.plugin));
            long joinedAt = guild.getJoinedAt(uuid);
            if (joinedAt > 0) lore.add("&7Joined: &e" + sdf.format(new java.util.Date(joinedAt)));
            
            try {
                int kills = op.getStatistic(org.bukkit.Statistic.PLAYER_KILLS);
                int deaths = op.getStatistic(org.bukkit.Statistic.DEATHS);
                double kda = deaths == 0 ? kills : (double) kills / deaths;
                lore.add("&7Kills: &c" + kills + " &8| &7Deaths: &c" + deaths);
                lore.add("&7KDA Ratio: &e" + String.format(java.util.Locale.US, "%.2f", kda));
            } catch (Exception ex) {}
            
            lore.add("&7Guild: &b" + guild.getTag());
            
            inv.setItem(slot++, this.item(Material.PLAYER_HEAD, "&f" + name, lore));
        }
        inv.setItem(49, this.item(Material.ARROW, "&cBack", List.of("&7Back to guild menu.")));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (!(inventoryHolder instanceof Holder)) {
            return;
        }
        Holder holder = (Holder)inventoryHolder;
        event.setCancelled(true);
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        if (holder.type.equals("main")) {
            player.closeInventory();
            Guild guild = this.guildManager.getGuild(player);
            if (guild == null) {
                if (slot == 13) this.openGuildList(player);
                return;
            }
            
            GuildRole role = guild.getRole(player.getUniqueId());
            
            if (slot == 11) this.openMembers(player, guild);
            else if (slot == 12) Bukkit.dispatchCommand((CommandSender)player, "guild home");
            else if (slot == 13) this.openBank(player, guild);
            else if (slot == 14) Bukkit.dispatchCommand((CommandSender)player, "guild vault");
            else if (slot == 15) Bukkit.dispatchCommand((CommandSender)player, "guild upgrade");
            else if (slot == 16) this.openGuildList(player);
            
        } else if (holder.type.equals("list")) {
            if (slot == 49) {
                this.open(player);
                return;
            }
            List<Guild> guilds = new ArrayList<>(this.guildManager.getGuilds());
            guilds.sort(java.util.Comparator.comparingLong(Guild::getCreatedAt));
            if (slot < guilds.size()) {
                this.openMembers(player, guilds.get(slot));
            }
        } else if (holder.type.equals("members") && slot == 49) {
            this.open(player);
        } else if (holder.type.equals("bank")) {
            if (slot == 26) {
                this.open(player);
                return;
            }
            if (slot == 11) {
                player.closeInventory();
                Text.msg((CommandSender)player, "&eGunakan command &b/guild deposit <amount> &euntuk menyetor.");
            } else if (slot == 15) {
                Guild guild = this.guildManager.getGuild(player);
                if (guild != null && guild.getRole(player.getUniqueId()) != GuildRole.MEMBER) {
                    player.closeInventory();
                    Text.msg((CommandSender)player, "&eGunakan command &b/guild withdraw <amount> &euntuk menarik.");
                }
            }
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            ArrayList<String> out = new ArrayList<String>();
            for (String line : lore) {
                out.add(Text.color(line));
            }
            meta.setLore(out);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static final class Holder
    implements InventoryHolder {
        private final String type;
        private final String tag;

        private Holder(String type, String tag) {
            this.type = type;
            this.tag = tag;
        }

        public Inventory getInventory() {
            return null;
        }
    }
}

