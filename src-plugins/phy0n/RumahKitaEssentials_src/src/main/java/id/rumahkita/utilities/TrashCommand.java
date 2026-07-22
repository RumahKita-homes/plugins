package id.rumahkita.utilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TrashCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        
        // 36 slots is usually enough for a trash bin.
        Inventory trashBin = Bukkit.createInventory(null, 36, ChatColor.DARK_RED + "Trash Bin");
        
        player.openInventory(trashBin);
        player.sendMessage(ChatColor.YELLOW + "Place items to discard here. They will be deleted when closed.");
        
        return true;
    }
}
