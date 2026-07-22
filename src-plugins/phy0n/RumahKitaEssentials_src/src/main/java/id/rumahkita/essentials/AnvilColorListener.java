package id.rumahkita.essentials;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnvilColorListener implements Listener {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getResult() == null) return;

        if (event.getViewers().isEmpty() || !(event.getViewers().get(0) instanceof Player)) return;
        
        Player player = (Player) event.getViewers().get(0);

        if (!player.hasPermission("rumahkita.anvil.color") && !player.hasPermission("essentials.itemname.color")) {
            return;
        }

        ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta()) return;
        
        ItemMeta meta = result.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String rawName = event.getInventory().getRenameText();
            if (rawName != null && !rawName.isEmpty()) {
                String coloredName = colorize(rawName);
                meta.setDisplayName(coloredName);
                result.setItemMeta(meta);
                event.setResult(result);
            }
        }
    }

    private String colorize(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String color = matcher.group(1);
            text = text.replace("&#" + color, net.md_5.bungee.api.ChatColor.of("#" + color) + "");
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
