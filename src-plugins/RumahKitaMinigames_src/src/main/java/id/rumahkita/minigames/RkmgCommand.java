package id.rumahkita.minigames;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class RkmgCommand implements CommandExecutor, TabCompleter {
    private final RumahKitaMinigamesPlugin plugin;

    public RkmgCommand(RumahKitaMinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rumahkita.minigames.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this.");
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage(ChatColor.GREEN + "All minigames configs reloaded!");
            return true;
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
        sender.sendMessage(ChatColor.AQUA + "RumahKita Minigames Admin");
        sender.sendMessage(ChatColor.YELLOW + "/rkmg reload" + ChatColor.GRAY + " - Reload all configs");
        sender.sendMessage(ChatColor.YELLOW + "/rkctf help" + ChatColor.GRAY + " - Capture the Flag admin");
        sender.sendMessage(ChatColor.YELLOW + "/fishadmin" + ChatColor.GRAY + " - Fishing admin");
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("rumahkita.minigames.admin")) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                return Collections.singletonList("reload");
            }
        }
        return Collections.emptyList();
    }
}
