package id.rumahkita.utilities;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InfoManager implements CommandExecutor {

    private final RumahKitaUtilitiesPlugin plugin;

    public InfoManager(RumahKitaUtilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        
        if (cmd.equals("ping")) {
            if (args.length > 0 && sender.hasPermission("rumahkita.admin")) {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    Text.msg(sender, "&cPlayer not found.");
                    return true;
                }
                Text.msg(sender, "&aPing &f" + target.getName() + "&a: &e" + target.getPing() + "ms");
                return true;
            }
            
            if (!(sender instanceof Player)) {
                Text.msg(sender, "&cThis command can only be used by players.");
                return true;
            }
            Player p = (Player) sender;
            Text.msg(p, "&aYour Ping: &e" + p.getPing() + "ms");
            return true;
        }
        
        if (cmd.equals("tps")) {
            try {
                java.lang.reflect.Method getTpsMethod = Bukkit.getServer().getClass().getMethod("getTPS");
                double[] tps = (double[]) getTpsMethod.invoke(Bukkit.getServer());
                String tps1 = formatTps(tps[0]);
                String tps5 = formatTps(tps[1]);
                String tps15 = formatTps(tps[2]);
                Text.msg(sender, "&aTPS from last 1m, 5m, 15m: " + tps1 + "&a, " + tps5 + "&a, " + tps15);
            } catch (Exception e) {
                Text.msg(sender, "&cThis server does not support TPS commands via API (Paper only).");
            }
            return true;
        }

        return false;
    }

    private String formatTps(double tps) {
        if (tps > 20.0) tps = 20.0;
        String color = (tps >= 18.0) ? "&a" : (tps >= 15.0) ? "&e" : "&c";
        return color + String.format("%.2f", tps);
    }
}
