package id.rumahkita.essentials;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

public class EssentialsAdminCommand implements CommandExecutor, TabCompleter {
    private final RumahKitaEssentialsPlugin plugin;
    private final EssentialsAdminGui gui;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "gui", "bansos", "sleep", "help"
    );

    public EssentialsAdminCommand(RumahKitaEssentialsPlugin plugin, EssentialsAdminGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("bansos")) {
            if (plugin.utilitiesModule != null && plugin.utilitiesModule.bansosManager != null) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return plugin.utilitiesModule.bansosManager.onCommand(sender, command, label, subArgs);
            }
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("sleep")) {
            if (plugin.utilitiesModule != null && plugin.utilitiesModule.sleepManager != null) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return plugin.utilitiesModule.sleepManager.onCommand(sender, command, label, subArgs);
            }
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GREEN + "RumahKitaEssentials Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/rkes gui " + ChatColor.GRAY + "- Open Essentials GUI");
            sender.sendMessage(ChatColor.YELLOW + "/rkes bansos " + ChatColor.GRAY + "- Manage Friday Bansos");
            sender.sendMessage(ChatColor.YELLOW + "/rkes sleep " + ChatColor.GRAY + "- Manage Sleep Settings");
            sender.sendMessage(ChatColor.YELLOW + "/heal [player] " + ChatColor.GRAY + "- Heal a player");
            sender.sendMessage(ChatColor.YELLOW + "/fly [player] " + ChatColor.GRAY + "- Toggle flight");
            sender.sendMessage(ChatColor.YELLOW + "/god [player] " + ChatColor.GRAY + "- Toggle god mode");
            sender.sendMessage(ChatColor.YELLOW + "/vanish [player] " + ChatColor.GRAY + "- Toggle vanish");
            sender.sendMessage(ChatColor.YELLOW + "/invsee <player> " + ChatColor.GRAY + "- View player's inventory");
            sender.sendMessage(ChatColor.YELLOW + "/ec <player> " + ChatColor.GRAY + "- View player's enderchest");
            sender.sendMessage(ChatColor.YELLOW + "/smite <player> " + ChatColor.GRAY + "- Strike lightning on player");
            sender.sendMessage(ChatColor.YELLOW + "/speed <fly/walk> <1-10> " + ChatColor.GRAY + "- Set speed");
            sender.sendMessage(ChatColor.YELLOW + "/spy " + ChatColor.GRAY + "- Toggle command spy");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open the GUI.");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("rumahkita.essentials.admin")) {
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
        if (args.length == 2 && args[0].equalsIgnoreCase("bansos")) {
            if (plugin.utilitiesModule != null && plugin.utilitiesModule.bansosManager != null) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return plugin.utilitiesModule.bansosManager.onTabComplete(sender, command, alias, subArgs);
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sleep")) {
            if (plugin.utilitiesModule != null && plugin.utilitiesModule.sleepManager != null) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return plugin.utilitiesModule.sleepManager.onTabComplete(sender, command, alias, subArgs);
            }
        }
        return Collections.emptyList();
    }
}
