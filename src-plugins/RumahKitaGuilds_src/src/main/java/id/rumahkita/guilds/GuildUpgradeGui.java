package id.rumahkita.guilds;

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
import java.util.Arrays;
import java.util.List;

public class GuildUpgradeGui implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;
    public static final String TITLE = ChatColor.WHITE + "Guild Upgrades";

    public GuildUpgradeGui(RumahKitaGuildsPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public void open(Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        
        int minMembers = plugin.getConfig().getInt("settings.upgrades.min-members", 3);
        boolean canUpgrade = guild.size() >= minMembers && guild.getBalance() >= 0;
        boolean inDebt = guild.getBalance() < 0;

        int vaultLevel = guild.getVaultLevel();
        if (vaultLevel >= 3) {
            inv.setItem(11, createItem(Material.ENDER_CHEST, "&6Guild Vault &8(&aMAX&8)", 
                "&7Your guild vault is at max level.",
                "&7Capacity: &f27 Slots (3 Rows)"
            ));
        } else {
            int nextLevel = vaultLevel + 1;
            double cost = plugin.getConfig().getDouble("settings.upgrades.vault.level-" + nextLevel, 500.0 * nextLevel);
            String action = vaultLevel == 0 ? "&eClick to Unlock" : "&eClick to Upgrade";
            int slots = nextLevel * 9;
            
            if (!canUpgrade) {
                if (inDebt) {
                    inv.setItem(11, createItem(Material.BARRIER, "&cLocked: Guild Vault",
                        "&7Your guild is in debt (&c$" + plugin.getEconomyManager().format(guild.getBalance()) + "&7)",
                        "&7Deposit money to unlock upgrades."
                    ));
                } else {
                    inv.setItem(11, createItem(Material.BARRIER, "&cLocked: Guild Vault",
                        "&7You need at least &a" + minMembers + " &7members",
                        "&7to unlock or upgrade the Vault.",
                        "", "&7Current Members: &c" + guild.size() + "/" + minMembers
                    ));
                }
            } else {
                inv.setItem(11, createItem(Material.CHEST, "&6Guild Vault &8(&eLvl " + nextLevel + "&8)", 
                    "&7Current Level: &f" + vaultLevel,
                    "&7Next Level Capacity: &f" + slots + " Slots",
                    "",
                    "&7Cost: &a" + plugin.getEconomyManager().format(cost) + " &7(From Guild Bank)",
                    "", action
                ));
            }
        }

        int memberLevel = guild.getMemberLevel();
        if (memberLevel >= 3) {
            inv.setItem(15, createItem(Material.DIAMOND_HELMET, "&bMax Members &8(&aMAX&8)", 
                "&7Your member limit is at max level.",
                "&7Capacity: &f25 Members"
            ));
        } else {
            int nextLevel = memberLevel + 1;
            double cost = plugin.getConfig().getDouble("settings.upgrades.members.level-" + nextLevel, 500.0 * nextLevel);
            String action = memberLevel == 0 ? "&eClick to Unlock" : "&eClick to Upgrade";
            int maxMembers = plugin.getConfig().getInt("settings.max-members.level-" + nextLevel, 10 + (nextLevel * 5));
            
            if (!canUpgrade) {
                if (inDebt) {
                    inv.setItem(15, createItem(Material.BARRIER, "&cLocked: Max Members",
                        "&7Your guild is in debt (&c$" + plugin.getEconomyManager().format(guild.getBalance()) + "&7)",
                        "&7Deposit money to unlock upgrades."
                    ));
                } else {
                    inv.setItem(15, createItem(Material.BARRIER, "&cLocked: Max Members",
                        "&7You need at least &a" + minMembers + " &7members",
                        "&7to upgrade member capacity.",
                        "", "&7Current Members: &c" + guild.size() + "/" + minMembers
                    ));
                }
            } else {
                inv.setItem(15, createItem(Material.PLAYER_HEAD, "&bMax Members &8(&eLvl " + nextLevel + "&8)", 
                    "&7Current Level: &f" + memberLevel,
                    "&7Next Level Capacity: &f" + maxMembers + " Members",
                    "",
                    "&7Cost: &a" + plugin.getEconomyManager().format(cost) + " &7(From Guild Bank)",
                    "", action
                ));
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(Arrays.stream(lore).map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        Guild guild = guildManager.getGuild(player);
        if (guild == null) {
            player.closeInventory();
            return;
        }

        GuildRole role = guild.getRole(player.getUniqueId());
        if (role == null || !role.atLeast(GuildRole.ADMIN)) {
            player.sendMessage(ChatColor.RED + "Only Guild Admin or Leader can buy upgrades.");
            return;
        }

        int slot = event.getSlot();

        int minMembers = plugin.getConfig().getInt("settings.upgrades.min-members", 3);

        if (slot == 11) {
            if (guild.getBalance() < 0) {
                player.sendMessage(ChatColor.RED + "You cannot upgrade while the guild is in debt!");
                return;
            }
            if (guild.size() < minMembers) {
                player.sendMessage(ChatColor.RED + "You need at least " + minMembers + " members to upgrade!");
                return;
            }
            int vaultLevel = guild.getVaultLevel();
            if (vaultLevel >= 3) return;
            
            int nextLevel = vaultLevel + 1;
            double cost = plugin.getConfig().getDouble("settings.upgrades.vault.level-" + nextLevel, 500.0 * nextLevel);
            
            if (!guild.withdrawBalance(cost)) {
                player.sendMessage(ChatColor.RED + "Your guild bank does not have enough money!");
                return;
            }
            guild.setVaultLevel(nextLevel);
            guildManager.save(guild);
            
            player.sendMessage(ChatColor.GREEN + "Successfully upgraded Guild Vault to Level " + nextLevel + "!");
            open(player, guild); 
        } 
        else if (slot == 15) {
            if (guild.getBalance() < 0) {
                player.sendMessage(ChatColor.RED + "You cannot upgrade while the guild is in debt!");
                return;
            }
            if (guild.size() < minMembers) {
                player.sendMessage(ChatColor.RED + "You need at least " + minMembers + " members to upgrade!");
                return;
            }
            int memberLevel = guild.getMemberLevel();
            if (memberLevel >= 3) return;
            
            int nextLevel = memberLevel + 1;
            double cost = plugin.getConfig().getDouble("settings.upgrades.members.level-" + nextLevel, 500.0 * nextLevel);
            
            if (!guild.withdrawBalance(cost)) {
                player.sendMessage(ChatColor.RED + "Your guild bank does not have enough money!");
                return;
            }
            guild.setMemberLevel(nextLevel);
            guildManager.save(guild);
            
            player.sendMessage(ChatColor.GREEN + "Successfully upgraded Max Members to Level " + nextLevel + "!");
            open(player, guild);
        }
    }
}
