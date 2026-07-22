package id.rumahkita.guilds;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class GuildVaultListener implements Listener {
    private final GuildManager guildManager;

    public GuildVaultListener(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getHolder() instanceof VaultHolder) {
            VaultHolder holder = (VaultHolder) event.getClickedInventory().getHolder();
            Guild guild = holder.getGuild();
            
            org.bukkit.inventory.ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == org.bukkit.Material.ARROW) {
                if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                    String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                    org.bukkit.entity.Player p = (org.bukkit.entity.Player) event.getWhoClicked();
                    
                    if (name.equals("Next Page") || name.equals("Previous Page")) {
                        event.setCancelled(true);
                        
                        guild.updateVaultItems(holder.getPage(), event.getInventory().getContents());
                        
                        int newPage = holder.getPage() == 1 ? 2 : 1;
                        Inventory newInv = guild.getVaultInventory(newPage);
                        p.openInventory(newInv);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof VaultHolder) {
            VaultHolder holder = (VaultHolder) inv.getHolder();
            Guild guild = holder.getGuild();
            guild.updateVaultItems(holder.getPage(), inv.getContents());
            guildManager.save(guild);
            event.getPlayer().sendMessage(ChatColor.GREEN + "Guild Vault saved.");
        }
    }
}
