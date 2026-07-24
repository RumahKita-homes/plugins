package id.rumahkita.utilities;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class PlayerSettingsGui implements Listener {
    
    public static final String TITLE = ChatColor.WHITE + "Player Settings";

    private static final Set<UUID> globalChatDisabled = new HashSet<>();

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        
        fillBorder(inv);
        
        inv.setItem(11, createItem(Material.OAK_SIGN, "&e&lPrivate Messages", 
            "&7Toggle incoming private",
            "&7messages (Whisper).",
            "",
            "&aLeft-Click to Toggle"
        ));
        
        inv.setItem(13, createItem(Material.ENDER_PEARL, "&b&lTeleport Requests (TPA)", 
            "&7Prevent others from sending",
            "&7teleport requests to you.",
            "",
            "&aLeft-Click to Toggle"
        ));
        
        inv.setItem(15, createItem(Material.SADDLE, "&c&lCarry Feature", 
            "&7Disable the Carry feature so",
            "&7others cannot pick you up.",
            "",
            "&aLeft-Click to Toggle"
        ));
        
        inv.setItem(29, createItem(Material.NAME_TAG, "&6&lScoreboard", 
            "&7Hide the scoreboard display",
            "&7on the right side of your screen.",
            "",
            "&aLeft-Click to Toggle"
        ));
        
        inv.setItem(31, createItem(Material.GOLDEN_CARROT, "&6&lNight Vision", 
            "&7Toggle permanent Night Vision",
            "&7to see clearly in the dark.",
            "",
            "&aLeft-Click to Toggle"
        ));
        
        inv.setItem(33, createItem(Material.BELL, "&c&lGlobal Chat", 
            "&7Hide global chat messages",
            "&7if you want a quiet screen.",
            "",
            "&aLeft-Click to Toggle"
        ));
        
        player.openInventory(inv);
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
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
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        if (slot == 11) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "msgtoggle");
        } else if (slot == 13) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "tptoggle");
        } else if (slot == 15) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "carry toggle");
        } else if (slot == 29) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "sb toggle");
        } else if (slot == 31) {
            player.closeInventory();
            if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.sendMessage(ChatColor.RED + "Night Vision disabled.");
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                player.sendMessage(ChatColor.GREEN + "Night Vision enabled.");
            }
        } else if (slot == 33) {
            player.closeInventory();
            UUID uuid = player.getUniqueId();
            if (globalChatDisabled.contains(uuid)) {
                globalChatDisabled.remove(uuid);
                player.sendMessage(ChatColor.GREEN + "Global Chat is now VISIBLE.");
            } else {
                globalChatDisabled.add(uuid);
                player.sendMessage(ChatColor.RED + "Global Chat is now HIDDEN.");
            }
        }
    }

    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        event.getRecipients().removeIf(p -> globalChatDisabled.contains(p.getUniqueId()));
    }
}
