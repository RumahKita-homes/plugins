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
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof VaultHolder) {
            VaultHolder holder = (VaultHolder) inv.getHolder();
            Guild guild = holder.getGuild();
            guild.setVaultItems(inv.getContents());
            guildManager.save();
            event.getPlayer().sendMessage(ChatColor.GREEN + "Guild Vault saved.");
        }
    }
}
