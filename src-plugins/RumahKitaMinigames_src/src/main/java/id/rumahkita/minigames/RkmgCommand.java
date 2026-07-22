package id.rumahkita.minigames;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RkmgCommand implements CommandExecutor, TabCompleter {
    private final RumahKitaMinigamesPlugin plugin;
    private final RkmgGuiManager gui;

    private static final List<String> SUBCOMMANDS = Arrays.asList("gui", "help");

    public RkmgCommand(RumahKitaMinigamesPlugin plugin, RkmgGuiManager gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GREEN + "RumahKita Minigames Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " gui " + ChatColor.GRAY + "- Open RumahKita Minigames Admin Dashboard");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open the GUI.");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("rumahkita.minigames.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        
        gui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
