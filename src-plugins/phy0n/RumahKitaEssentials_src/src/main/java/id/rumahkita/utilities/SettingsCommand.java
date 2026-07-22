package id.rumahkita.utilities;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SettingsCommand implements CommandExecutor {
    private final PlayerSettingsGui gui;

    public SettingsCommand(PlayerSettingsGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the Settings menu.");
            return true;
        }
        
        Player player = (Player) sender;
        gui.open(player);
        return true;
    }
}
