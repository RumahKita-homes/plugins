/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.Sound
 *  org.bukkit.ChatColor
 *  me.clip.placeholderapi.PlaceholderAPI
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import me.clip.placeholderapi.PlaceholderAPI;

public final class GuildHomeManager
implements Listener {
    private final RumahKitaGuildsPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();
    private final Map<UUID, PendingTeleport> pending = new HashMap<UUID, PendingTeleport>();

    public GuildHomeManager(RumahKitaGuildsPlugin plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, Guild guild) {
        Location home = guild.getHome();
        if (home == null || home.getWorld() == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "home-not-set"));
            return;
        }
        long now = System.currentTimeMillis();
        long until = this.cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (until > now) {
            long seconds = Math.max(1L, (until - now) / 1000L);
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "teleport-cooldown"), "%seconds%", String.valueOf(seconds)));
            return;
        }
        
        boolean inCombat = false;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            String combatlogx = PlaceholderAPI.setPlaceholders(player, "%combatlogx_in_combat%");
            String deluxecombat = PlaceholderAPI.setPlaceholders(player, "%deluxecombat_in_combat%");
            if ("true".equalsIgnoreCase(combatlogx) || "yes".equalsIgnoreCase(combatlogx)) {
                inCombat = true;
            } else if ("true".equalsIgnoreCase(deluxecombat) || "yes".equalsIgnoreCase(deluxecombat)) {
                inCombat = true;
            }
        }
        
        int delay = inCombat ? this.plugin.getConfig().getInt("guild-home.teleport-delay-seconds", 5) : 0;
        if (delay <= 0) {
            this.doTeleport(player, home, guild);
            return;
        }
        PendingTeleport old = this.pending.remove(player.getUniqueId());
        if (old != null) {
            old.task.cancel();
        }
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "teleport-start"), "%seconds%", String.valueOf(delay)));
        Location start = player.getLocation().clone();
        BukkitTask task = new BukkitRunnable() {
            int countdown = delay;

            public void run() {
                if (!player.isOnline()) {
                    pending.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                if (countdown > 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&a&l" + countdown), ChatColor.YELLOW + "Do not move!", 0, 25, 0);
                    countdown--;
                } else {
                    pending.remove(player.getUniqueId());
                    doTeleport(player, home, guild);
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
        this.pending.put(player.getUniqueId(), new PendingTeleport(start, task));
    }

    private void doTeleport(Player player, Location home, Guild guild) {
        player.teleport(home);
        int cooldownSeconds = Math.max(0, this.plugin.getConfig().getInt("guild-home.cooldown-seconds", 60));
        this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)cooldownSeconds * 1000L);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&a&l" + guild.getName()), ChatColor.YELLOW + "Teleported to Guild Home!", 5, 20, 10);
        Text.msg((CommandSender)player, Text.prefixed(this.plugin, "teleport-done"));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!this.plugin.getConfig().getBoolean("guild-home.cancel-on-move", true)) {
            return;
        }
        PendingTeleport p = this.pending.get(event.getPlayer().getUniqueId());
        if (p == null || event.getTo() == null || p.start.getWorld() != event.getTo().getWorld()) {
            return;
        }
        double cancelDistance = this.plugin.getConfig().getDouble("guild-home.cancel-distance", 0.35);
        if (p.start.distanceSquared(event.getTo()) > cancelDistance * cancelDistance) {
            p.task.cancel();
            this.pending.remove(event.getPlayer().getUniqueId());
            Text.msg((CommandSender)event.getPlayer(), Text.prefixed(this.plugin, "teleport-cancelled"));
        }
    }

    public void cancelAllTeleports() {
        for (PendingTeleport p : this.pending.values()) {
            p.task.cancel();
        }
        this.pending.clear();
    }

    private record PendingTeleport(Location start, BukkitTask task) {
    }
}

