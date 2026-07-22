package id.rumahkita.essentials;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EssentialsAdminCommand implements CommandExecutor {
    private final EssentialsAdminGui gui;

    public EssentialsAdminCommand(EssentialsAdminGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
}
