package id.rumahkita.economy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class EconomyAdminCommand implements CommandExecutor, TabCompleter {

    public EconomyAdminCommand() {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
        sender.sendMessage(ChatColor.AQUA + "RumahKita Economy Admin");
        sender.sendMessage(ChatColor.YELLOW + "/rke give/take/set/balance <player> <amount>" + ChatColor.GRAY + " - Manage balances");
        sender.sendMessage(ChatColor.YELLOW + "/rke voucher give/giveall <player/percent> <amount>" + ChatColor.GRAY + " - Give vouchers");
        sender.sendMessage(ChatColor.YELLOW + "/rke reload/save/placeholders/demandupdate" + ChatColor.GRAY + " - Server tasks");
        sender.sendMessage(ChatColor.YELLOW + "/rke migratebalances/migratemysql" + ChatColor.GRAY + " - Migrate balances");
        sender.sendMessage(ChatColor.YELLOW + "/rke baltop" + ChatColor.GRAY + " - View baltop (bypass hidebal)");
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
