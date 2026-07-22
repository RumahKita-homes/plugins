package id.rumahkita.guilds;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final RumahKitaGuildsPlugin plugin;
    private Economy econ = null;

    public EconomyManager(RumahKitaGuildsPlugin plugin) {
        this.plugin = plugin;
        if (!setupEconomy()) {
            plugin.getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", plugin.getDescription().getName()));
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }
        plugin.getLogger().info("Vault Economy successfully hooked!");
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return econ.has(player, amount);
    }

    public void withdraw(OfflinePlayer player, double amount) {
        econ.withdrawPlayer(player, amount);
    }

    public void deposit(OfflinePlayer player, double amount) {
        econ.depositPlayer(player, amount);
    }
    
    public String format(double amount) {
        return econ.format(amount);
    }
}
