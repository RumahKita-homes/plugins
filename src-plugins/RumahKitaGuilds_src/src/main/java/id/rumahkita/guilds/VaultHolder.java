package id.rumahkita.guilds;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class VaultHolder implements InventoryHolder {
    private final Guild guild;

    public VaultHolder(Guild guild) {
        this.guild = guild;
    }

    public Guild getGuild() {
        return guild;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
