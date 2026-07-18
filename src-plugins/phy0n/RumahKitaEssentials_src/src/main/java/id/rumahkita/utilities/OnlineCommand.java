package id.rumahkita.utilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class OnlineCommand implements CommandExecutor {

    private final RumahKitaUtilitiesPlugin plugin;

    public OnlineCommand(RumahKitaUtilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> onlinePlayers = new ArrayList<>();
        VanishManager vanishManager = plugin.getVanishManager();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (vanishManager != null && vanishManager.isVanished(p.getUniqueId())) {
                continue;
            }
            if (p.hasMetadata("vanished")) {
                continue;
            }
            if (sender instanceof Player && !((Player) sender).canSee(p)) {
                continue;
            }
            onlinePlayers.add(p.getName());
        }

        int count = onlinePlayers.size();
        int max = Bukkit.getMaxPlayers();
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Online Players &7(" + count + "/" + max + "):"));
        if (count == 0) {
            sender.sendMessage(ChatColor.WHITE + "No players online.");
        } else {
            sender.sendMessage(ChatColor.WHITE + String.join(", ", onlinePlayers));
        }
        
        return true;
    }
}
