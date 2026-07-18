package id.rumahkita.utilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLinksSendEvent;
import org.bukkit.ServerLinks;
import org.bukkit.ServerLinks.ServerLink;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class ServerLinksManager implements Listener {

    private final RumahKitaUtilitiesPlugin plugin;

    public ServerLinksManager(RumahKitaUtilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLinksSend(PlayerLinksSendEvent event) {
        ServerLinks links = event.getLinks();
        
        List<Map<?, ?>> customLinks = plugin.getConfig().getMapList("server-links");
        if (customLinks != null && !customLinks.isEmpty()) {
            for (Map<?, ?> map : customLinks) {
                if (map.containsKey("label") && map.containsKey("url")) {
                    String label = ChatColor.translateAlternateColorCodes('&', String.valueOf(map.get("label")));
                    String urlString = String.valueOf(map.get("url"));
                    try {
                        URI uri = new URI(urlString);
                        links.addLink(label, uri);
                    } catch (URISyntaxException e) {
                        plugin.getLogger().warning("Invalid Server Link URL: " + urlString);
                    }
                }
            }
        }
    }
}
