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

public class GuildSettingsGui implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;

    public GuildSettingsGui(RumahKitaGuildsPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public void open(Player player, Guild guild) {
        String title = ChatColor.WHITE + "Settings: " + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', guild.getTag()));
        if (title.length() > 32) title = title.substring(0, 32);
        Inventory inv = Bukkit.createInventory(new GuildSettingsHolder(guild), 27, title);
        
        inv.setItem(10, createToggleItem(Material.ZOMBIE_HEAD, "&eMob Spawning", guild.isMobSpawning(), 
            "&7Allow monsters to spawn naturally", "&7inside your guild claim."));
            
        inv.setItem(12, createToggleItem(Material.GUNPOWDER, "&cMob Griefing", guild.isMobGriefing(), 
            "&7Allow creepers/ghasts to destroy", "&7blocks inside your guild claim."));
            
        inv.setItem(14, createToggleItem(Material.OAK_DOOR, "&bPublic Interaction", guild.isPublicInteraction(), 
            "&7Allow non-members to interact with", "&7doors, levers, buttons, etc."));
            
        inv.setItem(16, createToggleItem(Material.IRON_SWORD, "&4Allow PVP", guild.isAllowPvp(), 
            "&7Allow players to fight each other", "&7inside your guild claim."));
            
        player.openInventory(inv);
    }

    private ItemStack createToggleItem(Material mat, String name, boolean state, String... description) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String line : description) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add("");
            if (state) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&a\u25b6 ENABLED"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8  DISABLED"));
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8  ENABLED"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&c\u25b6 DISABLED"));
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to toggle!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof GuildSettingsHolder) {
            event.setCancelled(true);
            
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            GuildSettingsHolder holder = (GuildSettingsHolder) event.getInventory().getHolder();
            Guild guild = holder.getGuild();
            
            GuildRole role = guild.getRole(player.getUniqueId());
            if (!player.hasPermission("rumahkitaguilds.admin") && !role.atLeast(GuildRole.ADMIN)) {
                player.sendMessage(ChatColor.RED + "You don't have permission to change these settings.");
                player.closeInventory();
                return;
            }

            int slot = event.getSlot();
            boolean changed = false;

            if (slot == 10) {
                guild.setMobSpawning(!guild.isMobSpawning());
                changed = true;
            } else if (slot == 12) {
                guild.setMobGriefing(!guild.isMobGriefing());
                changed = true;
            } else if (slot == 14) {
                guild.setPublicInteraction(!guild.isPublicInteraction());
                changed = true;
            } else if (slot == 16) {
                guild.setAllowPvp(!guild.isAllowPvp());
                changed = true;
            }

            if (changed) {
                guildManager.save(guild);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                open(player, guild); 
            }
        }
    }
    
    public static class GuildSettingsHolder implements org.bukkit.inventory.InventoryHolder {
        private final Guild guild;
        public GuildSettingsHolder(Guild guild) {
            this.guild = guild;
        }
        public Guild getGuild() {
            return this.guild;
        }
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
