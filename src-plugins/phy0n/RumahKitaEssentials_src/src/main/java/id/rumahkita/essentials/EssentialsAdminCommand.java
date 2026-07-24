package id.rumahkita.essentials;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class EssentialsAdminCommand implements CommandExecutor, TabCompleter {
    private final RumahKitaEssentialsPlugin plugin;

    public EssentialsAdminCommand(RumahKitaEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
        sender.sendMessage(ChatColor.AQUA + "RumahKita Essentials Admin");
        sender.sendMessage(ChatColor.YELLOW + "/setwarp <name>" + ChatColor.GRAY + " - Set a warp");
        sender.sendMessage(ChatColor.YELLOW + "/delwarp <name>" + ChatColor.GRAY + " - Delete a warp");
        sender.sendMessage(ChatColor.YELLOW + "/heal [player]" + ChatColor.GRAY + " - Heal a player");
        sender.sendMessage(ChatColor.YELLOW + "/fly [player]" + ChatColor.GRAY + " - Toggle fly mode");
        sender.sendMessage(ChatColor.YELLOW + "/vanish" + ChatColor.GRAY + " - Toggle vanish");
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
