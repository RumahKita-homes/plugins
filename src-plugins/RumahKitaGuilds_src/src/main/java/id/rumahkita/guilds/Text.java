/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
    private Text() {
    }

    public static String color(String text) {
        if (text == null) return "";
        java.util.regex.Matcher match = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(text);
        while (match.find()) {
            String hex = match.group(1);
            text = text.replace("&#" + hex, net.md_5.bungee.api.ChatColor.of("#" + hex) + "");
            match = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(text);
        }
        return ChatColor.translateAlternateColorCodes((char)'&', text);
    }

    public static void msg(CommandSender sender, String message) {
        sender.sendMessage(Text.color(message));
    }

    public static String prefixed(RumahKitaGuildsPlugin plugin, String path) {
        return plugin.getConfig().getString("messages." + path, path);
    }

    public static String replace(String text, String ... replacements) {
        String out = text == null ? "" : text;
        int i = 0;
        while (i + 1 < replacements.length) {
            out = out.replace(replacements[i], replacements[i + 1]);
            i += 2;
        }
        return out;
    }
}

