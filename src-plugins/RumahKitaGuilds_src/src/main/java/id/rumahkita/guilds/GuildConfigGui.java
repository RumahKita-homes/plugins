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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuildConfigGui implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    public static final String TITLE = ChatColor.DARK_GRAY + "Guild Admin Settings";
    
    private final Map<UUID, ConfigInput> pendingInputs = new HashMap<>();

    private static class ConfigInput {
        String key;
        boolean isInt;
        ConfigInput(String key, boolean isInt) {
            this.key = key;
            this.isInt = isInt;
        }
    }

    public GuildConfigGui(RumahKitaGuildsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        
        inv.setItem(11, createItem(Material.GOLD_INGOT, "&eGuild Creation Cost", 
            "&7Current: &a" + plugin.getConfig().getDouble("settings.create-cost.money", 1000.0),
            "", "&fClick to edit in chat"
        ));

        inv.setItem(13, createItem(Material.DIAMOND_SWORD, "&cGuild War Bet Min", 
            "&7Current: &a" + plugin.getConfig().getDouble("guild-war.reward-emerald", 2.0),
            "", "&fClick to edit in chat"
        ));

        inv.setItem(15, createItem(Material.PLAYER_HEAD, "&bMax Members Default", 
            "&7Current: &a" + plugin.getConfig().getInt("settings.max-members.default", 10),
            "", "&fClick to edit in chat"
        ));

        inv.setItem(19, createItem(Material.CHEST, "&6Vault Upgrade Lvl 1", 
            "&7Cost: &a" + plugin.getConfig().getDouble("settings.upgrades.vault.level-1", 500.0),
            "", "&fClick to edit in chat"
        ));
        inv.setItem(20, createItem(Material.CHEST, "&6Vault Upgrade Lvl 2", 
            "&7Cost: &a" + plugin.getConfig().getDouble("settings.upgrades.vault.level-2", 1500.0),
            "", "&fClick to edit in chat"
        ));
        inv.setItem(21, createItem(Material.CHEST, "&6Vault Upgrade Lvl 3", 
            "&7Cost: &a" + plugin.getConfig().getDouble("settings.upgrades.vault.level-3", 3000.0),
            "", "&fClick to edit in chat"
        ));

        inv.setItem(23, createItem(Material.ARMOR_STAND, "&dMember Upgrade Lvl 1", 
            "&7Cost: &a" + plugin.getConfig().getDouble("settings.upgrades.members.level-1", 500.0),
            "", "&fClick to edit in chat"
        ));
        inv.setItem(24, createItem(Material.ARMOR_STAND, "&dMember Upgrade Lvl 2", 
            "&7Cost: &a" + plugin.getConfig().getDouble("settings.upgrades.members.level-2", 1500.0),
            "", "&fClick to edit in chat"
        ));
        inv.setItem(25, createItem(Material.ARMOR_STAND, "&dMember Upgrade Lvl 3", 
            "&7Cost: &a" + plugin.getConfig().getDouble("settings.upgrades.members.level-3", 3000.0),
            "", "&fClick to edit in chat"
        ));

        inv.setItem(29, createItem(Material.IRON_DOOR, "&aMin Members Required", 
            "&7Required to use/upgrade Vault",
            "&7Current: &a" + plugin.getConfig().getInt("settings.upgrades.min-members", 3),
            "", "&fClick to edit in chat"
        ));

        boolean upkeepEnabled = plugin.getConfig().getBoolean("settings.upkeep.enabled", true);
        inv.setItem(31, createItem(upkeepEnabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, "&cGuild Upkeep (Tax)", 
            "&7Current: " + (upkeepEnabled ? "&aEnabled" : "&cDisabled"),
            "", "&fClick to Toggle"
        ));

        inv.setItem(33, createItem(Material.GOLD_NUGGET, "&eUpkeep Amount / Day", 
            "&7Current: &a" + plugin.getConfig().getDouble("settings.upkeep.amount", 500.0),
            "", "&fClick to edit in chat"
        ));

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
        
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        int slot = event.getSlot();

        ConfigInput targetInput = null;

        if (slot == 11) {
            targetInput = new ConfigInput("settings.create-cost.money", false);
        } else if (slot == 13) {
            targetInput = new ConfigInput("guild-war.reward-emerald", false);
        } else if (slot == 15) {
            targetInput = new ConfigInput("settings.max-members.default", true);
        } else if (slot >= 19 && slot <= 21) {
            targetInput = new ConfigInput("settings.upgrades.vault.level-" + (slot - 18), false);
        } else if (slot >= 23 && slot <= 25) {
            targetInput = new ConfigInput("settings.upgrades.members.level-" + (slot - 22), false);
        } else if (slot == 29) {
            targetInput = new ConfigInput("settings.upgrades.min-members", true);
        } else if (slot == 31) {
            boolean current = plugin.getConfig().getBoolean("settings.upkeep.enabled", true);
            plugin.getConfig().set("settings.upkeep.enabled", !current);
            plugin.saveConfig();
            open(player);
            return;
        } else if (slot == 33) {
            targetInput = new ConfigInput("settings.upkeep.amount", false);
        }

        if (targetInput != null) {
            player.closeInventory();
            pendingInputs.put(player.getUniqueId(), targetInput);
            player.sendMessage(ChatColor.YELLOW + "Please type the new value in chat. Type 'cancel' to abort.");
        }
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!pendingInputs.containsKey(player.getUniqueId())) return;
        
        event.setCancelled(true);
        String message = event.getMessage().trim();
        ConfigInput input = pendingInputs.remove(player.getUniqueId());
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Configuration edit cancelled.");
                open(player);
                return;
            }
            
            try {
                if (input.isInt) {
                    int val = Integer.parseInt(message);
                    plugin.getConfig().set(input.key, val);
                } else {
                    double val = Double.parseDouble(message);
                    plugin.getConfig().set(input.key, val);
                }
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + "Configuration updated successfully!");
                open(player);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number format! Edit cancelled.");
                open(player);
            }
        });
    }
}
