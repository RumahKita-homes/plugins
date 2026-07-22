package id.rumahkita.guilds;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class VaultHolder implements InventoryHolder {
    private final Guild guild;
    private final int page;

    public VaultHolder(Guild guild, int page) {
        this.guild = guild;
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    public Guild getGuild() {
        return guild;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
