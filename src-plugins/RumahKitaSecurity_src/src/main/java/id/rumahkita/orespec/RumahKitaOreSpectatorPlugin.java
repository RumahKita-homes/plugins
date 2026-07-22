/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Color
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerGameModeChangeEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package id.rumahkita.orespec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RumahKitaOreSpectatorPlugin
implements Listener,
TabExecutor {
    private static final String VERSION = "1.0.3";
    private final id.rumahkita.security.RumahKitaSecurityPlugin plugin;

    public RumahKitaOreSpectatorPlugin(id.rumahkita.security.RumahKitaSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private int taskId = -1;

    public void onEnable() {
        plugin.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents((Listener)this, this.plugin);
        if (plugin.getCommand("spec2") != null) {
            plugin.getCommand("spec2").setExecutor((CommandExecutor)this);
            plugin.getCommand("spec2").setTabCompleter((TabCompleter)this);
        }
        this.startTask();
        plugin.getLogger().info("RumahKitaOreSpectator v1.0.3 enabled.");
    }

    public void onDisable() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
        }
        for (UUID uuid : new ArrayList<UUID>(this.sessions.keySet())) {
            Player player = Bukkit.getPlayer((UUID)uuid);
            this.disableSession(uuid, player, false, false);
        }
        this.sessions.clear();
    }

    private void startTask() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
        }
        long interval = Math.max(20L, plugin.getConfig().getLong("general.update-interval-ticks", 60L));
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, this::tickSessions, interval, interval);
    }

    private void tickSessions() {
        if (!plugin.getConfig().getBoolean("general.enabled", true)) {
            return;
        }
        for (UUID uuid : new ArrayList<UUID>(this.sessions.keySet())) {
            Player player = Bukkit.getPlayer((UUID)uuid);
            if (player == null || !player.isOnline()) {
                Session removed = this.sessions.remove(uuid);
                if (removed == null) continue;
                this.removeMarkers(removed);
                continue;
            }
            this.applyStealthToAll(player);
            this.refreshMarkers(player, false);
        }
    }

    private void enableSession(Player player) {
        if (this.sessions.containsKey(player.getUniqueId())) {
            Session session = this.sessions.get(player.getUniqueId());
            this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, plugin.getConfig().getString("messages.already-enabled", "&eSpec2 is already active. Markers: &f%count%&e.")));
            return;
        }
        Session session = new Session();
        session.originalGameMode = player.getGameMode();
        session.radius = this.clampRadius(plugin.getConfig().getInt("general.default-radius", 48));
        session.verticalRadius = this.clampVerticalRadius(plugin.getConfig().getInt("general.default-vertical-radius", 96));
        session.filter = plugin.getConfig().getString("general.default-filter", "valuable").toLowerCase(Locale.ROOT);
        if (!this.isValidFilter(session.filter)) {
            session.filter = "valuable";
        }
        this.sessions.put(player.getUniqueId(), session);
        player.setGameMode(GameMode.SPECTATOR);
        this.applyStealthToAll(player);
        this.sendFakeQuit(player);
        this.refreshMarkers(player, true);
        this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, plugin.getConfig().getString("messages.enabled", "&aSpec2 active. Markers: &f%count% &7| Filter: &f%filter% &7| Radius: &f%radius%/%vradius%")));
        if (session.lastMarkerCount <= 0) {
            this.msg((CommandSender)player, this.pref() + plugin.getConfig().getString("messages.no-markers-tip", "&eMarkers 0. Fly closer to the ground/underground, or use &f/spec2 filter all &eand &f/spec2 radius 96&e."));
        }
    }

    private void disableSession(UUID uuid, Player player, boolean restoreGameMode, boolean sendJoin) {
        Session session = this.sessions.remove(uuid);
        if (session == null) {
            return;
        }
        this.removeMarkers(session);
        if (player != null && player.isOnline()) {
            this.revealToAll(player);
            if (restoreGameMode && plugin.getConfig().getBoolean("general.restore-gamemode-on-disable", true)) {
                player.setGameMode(session.originalGameMode == null ? GameMode.SURVIVAL : session.originalGameMode);
            }
            if (sendJoin) {
                this.sendFakeJoin(player);
            }
            this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, plugin.getConfig().getString("messages.disabled", "&cSpec2 deactivated. You have reappeared.")));
        }
    }

    private void refreshMarkers(Player player, boolean notifyIfDebug) {
        Session session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        this.removeMarkers(session);
        List<OreHit> hits = this.scanOres(player, session);
        int max = Math.max(1, plugin.getConfig().getInt("general.max-markers", 350));
        int count = 0;
        for (OreHit hit : hits) {
            if (count >= max) break;
            Entity marker = this.spawnMarker(player, hit);
            if (marker == null) continue;
            session.markers.add(marker);
            ++count;
        }
        session.lastMarkerCount = count;
        session.lastFoundCount = hits.size();
        if (notifyIfDebug && plugin.getConfig().getBoolean("general.debug-scan-on-enable", false)) {
            this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, "&7Debug scan: found=&f%found% &7shown=&f%count% &7filter=&f%filter% &7radius=&f%radius%/%vradius%"));
        }
    }

    private List<OreHit> scanOres(Player player, Session session) {
        ArrayList<OreHit> hits = new ArrayList<OreHit>();
        Set<Material> materials = this.getMaterialsForFilter(session.filter);
        if (materials.isEmpty()) {
            return hits;
        }
        Location center = player.getLocation();
        World world = player.getWorld();
        int radius = this.clampRadius(session.radius);
        int verticalRadius = this.clampVerticalRadius(session.verticalRadius);
        int radiusSq = radius * radius;
        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), baseY - verticalRadius);
        int maxY = Math.min(world.getMaxHeight() - 1, baseY + verticalRadius);
        for (int x = baseX - radius; x <= baseX + radius; ++x) {
            for (int z = baseZ - radius; z <= baseZ + radius; ++z) {
                int dx = x - baseX;
                int dz = z - baseZ;
                int horizSq = dx * dx + dz * dz;
                if (horizSq > radiusSq || !world.isChunkLoaded(x >> 4, z >> 4)) continue;
                for (int y = minY; y <= maxY; ++y) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (!materials.contains(type)) continue;
                    int dy = y - baseY;
                    int distSq = horizSq + dy * dy;
                    hits.add(new OreHit(block.getLocation(), type, distSq));
                }
            }
        }
        hits.sort(Comparator.comparingInt(h -> h.distSq));
        return hits;
    }

    private Entity spawnMarker(Player owner, OreHit hit) {
        try {
            Location loc = hit.location.clone().add(0.5, 0.5, 0.5);
            TextDisplay display = (TextDisplay)owner.getWorld().spawn(loc, TextDisplay.class, td -> {
                td.setText(this.color(this.buildMarkerText(hit)));
                td.setBillboard(Display.Billboard.CENTER);
                td.setSeeThrough(true);
                td.setShadowed(true);
                td.setDefaultBackground(false);
                td.setLineWidth(80);
                td.setGlowing(true);
                td.setGlowColorOverride(this.colorForMaterial(hit.material));
                td.setPersistent(false);
                td.setInvulnerable(true);
                td.setGravity(false);
                td.setSilent(true);
            });
            this.hideMarkerFromOthers(owner, (Entity)display);
            return display;
        }
        catch (Throwable t) {
            plugin.getLogger().warning("Failed to spawn ore marker: " + t.getMessage());
            return null;
        }
    }

    private String buildMarkerText(OreHit hit) {
        String mode = plugin.getConfig().getString("markers.text-mode", "short").toLowerCase(Locale.ROOT);
        String shortName = this.shortName(hit.material);
        int blocks = (int)Math.round(Math.sqrt(hit.distSq));
        if ("full".equals(mode)) {
            return this.colorCode(hit.material) + shortName + " &7" + blocks + "m";
        }
        if ("symbol".equals(mode)) {
            return this.colorCode(hit.material) + plugin.getConfig().getString("markers.symbol", "\u25c6");
        }
        return this.colorCode(hit.material) + plugin.getConfig().getString("markers.symbol", "\u25c6") + " " + shortName + " &7" + blocks + "m";
    }

    private String shortName(Material material) {
        String name;
        return switch (name = material.name()) {
            case "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE" -> "DIAMOND";
            case "ANCIENT_DEBRIS" -> "DEBRIS";
            case "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE" -> "EMERALD";
            case "GOLD_ORE", "DEEPSLATE_GOLD_ORE", "NETHER_GOLD_ORE" -> "GOLD";
            case "IRON_ORE", "DEEPSLATE_IRON_ORE" -> "IRON";
            case "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE" -> "REDSTONE";
            case "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE" -> "LAPIS";
            case "COAL_ORE", "DEEPSLATE_COAL_ORE" -> "COAL";
            case "COPPER_ORE", "DEEPSLATE_COPPER_ORE" -> "COPPER";
            case "NETHER_QUARTZ_ORE" -> "QUARTZ";
            default -> name.replace("DEEPSLATE_", "").replace("_ORE", "");
        };
    }

    private String colorCode(Material material) {
        String name = material.name();
        if (name.contains("DIAMOND")) {
            return "&b";
        }
        if (name.contains("ANCIENT_DEBRIS")) {
            return "&6";
        }
        if (name.contains("EMERALD")) {
            return "&a";
        }
        if (name.contains("GOLD")) {
            return "&e";
        }
        if (name.contains("REDSTONE")) {
            return "&c";
        }
        if (name.contains("LAPIS")) {
            return "&9";
        }
        if (name.contains("IRON")) {
            return "&f";
        }
        if (name.contains("COPPER")) {
            return "&6";
        }
        if (name.contains("COAL")) {
            return "&8";
        }
        if (name.contains("QUARTZ")) {
            return "&f";
        }
        return "&d";
    }

    private Color colorForMaterial(Material material) {
        String name = material.name();
        if (name.contains("DIAMOND")) {
            return Color.AQUA;
        }
        if (name.contains("ANCIENT_DEBRIS")) {
            return Color.ORANGE;
        }
        if (name.contains("EMERALD")) {
            return Color.LIME;
        }
        if (name.contains("GOLD")) {
            return Color.YELLOW;
        }
        if (name.contains("REDSTONE")) {
            return Color.RED;
        }
        if (name.contains("LAPIS")) {
            return Color.BLUE;
        }
        if (name.contains("IRON")) {
            return Color.WHITE;
        }
        if (name.contains("COPPER")) {
            return Color.ORANGE;
        }
        if (name.contains("COAL")) {
            return Color.GRAY;
        }
        return Color.FUCHSIA;
    }

    private void hideMarkerFromOthers(Player owner, Entity marker) {
        String seePerm = plugin.getConfig().getString("stealth.see-permission", "rumahkita.orespec.see");
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals((Object)owner)) {
                viewer.showEntity(this.plugin, marker);
                continue;
            }
            boolean staffCanSeeMarkers = viewer.hasPermission(seePerm) && plugin.getConfig().getBoolean("markers.staff-can-see-other-markers", false);
            if (staffCanSeeMarkers) continue;
            viewer.hideEntity(this.plugin, marker);
        }
    }

    private void removeMarkers(Session session) {
        for (Entity marker : session.markers) {
            if (marker == null || marker.isDead()) continue;
            marker.remove();
        }
        session.markers.clear();
        session.lastMarkerCount = 0;
    }

    private void applyStealthToAll(Player hidden) {
        if (!plugin.getConfig().getBoolean("stealth.enabled", true)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("stealth.hide-from-players", true)) {
            return;
        }
        String seePerm = plugin.getConfig().getString("stealth.see-permission", "rumahkita.orespec.see");
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals((Object)hidden)) continue;
            if (viewer.hasPermission(seePerm)) {
                viewer.showPlayer(this.plugin, hidden);
                continue;
            }
            viewer.hidePlayer(this.plugin, hidden);
        }
    }

    private void revealToAll(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals((Object)player)) continue;
            viewer.showPlayer(this.plugin, player);
        }
    }

    private void sendFakeQuit(Player player) {
        if (!plugin.getConfig().getBoolean("stealth.enabled", true)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("stealth.fake-quit-on-enable", true)) {
            return;
        }
        String text = plugin.getConfig().getString("stealth.fake-quit-message", "&e%player% left the game").replace("%player%", player.getName());
        this.sendToNonStaff(player, text);
    }

    private void sendFakeJoin(Player player) {
        if (!plugin.getConfig().getBoolean("stealth.enabled", true)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("stealth.fake-join-on-disable", true)) {
            return;
        }
        String text = plugin.getConfig().getString("stealth.fake-join-message", "&e%player% joined the game").replace("%player%", player.getName());
        this.sendToNonStaff(player, text);
    }

    private void sendToNonStaff(Player actor, String message) {
        String seePerm = plugin.getConfig().getString("stealth.see-permission", "rumahkita.orespec.see");
        String colored = this.color(message);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals((Object)actor) || viewer.hasPermission(seePerm)) continue;
            viewer.sendMessage(colored);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            String seePerm;
            Player joiner = event.getPlayer();
            if (joiner.hasPermission(seePerm = plugin.getConfig().getString("stealth.see-permission", "rumahkita.orespec.see"))) {
                return;
            }
            for (UUID uuid : this.sessions.keySet()) {
                Player hidden = Bukkit.getPlayer((UUID)uuid);
                if (hidden == null || !hidden.isOnline() || hidden.equals((Object)joiner)) continue;
                joiner.hidePlayer(this.plugin, hidden);
            }
        }, 5L);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (this.sessions.containsKey(uuid)) {
            event.setQuitMessage(null);
            Session removed = this.sessions.remove(uuid);
            if (removed != null) {
                this.removeMarkers(removed);
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (this.sessions.containsKey(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                this.applyStealthToAll(event.getPlayer());
                this.refreshMarkers(event.getPlayer(), true);
            }, 10L);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (this.sessions.containsKey(event.getPlayer().getUniqueId()) && event.getNewGameMode() != GameMode.SPECTATOR) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                Player player = event.getPlayer();
                if (player.isOnline() && player.getGameMode() != GameMode.SPECTATOR) {
                    this.disableSession(player.getUniqueId(), player, false, true);
                }
            }, 1L);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String seePerm;
        if (!plugin.getConfig().getBoolean("stealth.enabled", true)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("stealth.hide-from-online-commands", true)) {
            return;
        }
        Player sender = event.getPlayer();
        if (sender.hasPermission(seePerm = plugin.getConfig().getString("stealth.see-permission", "rumahkita.orespec.see"))) {
            return;
        }
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2) {
            return;
        }
        String first = raw.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        if (first.contains(":")) {
            first = first.substring(first.indexOf(58) + 1);
        }
        for (String alias : plugin.getConfig().getStringList("stealth.online-command-aliases")) {
            if (!first.equalsIgnoreCase(alias)) continue;
            event.setCancelled(true);
            this.sendFilteredOnline(sender);
            return;
        }
    }

    private void sendFilteredOnline(Player sender) {
        String seePerm = plugin.getConfig().getString("stealth.see-permission", "rumahkita.orespec.see");
        ArrayList<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.sessions.containsKey(player.getUniqueId()) && !sender.hasPermission(seePerm) || !sender.canSee(player)) continue;
            names.add(player.getName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        String header = plugin.getConfig().getString("stealth.online-message-header", "&6Online Players &7(&e%online%&7/&e%max%&7):").replace("%online%", String.valueOf(names.size())).replace("%max%", String.valueOf(Bukkit.getMaxPlayers()));
        this.msg((CommandSender)sender, header);
        if (names.isEmpty()) {
            this.msg((CommandSender)sender, plugin.getConfig().getString("stealth.online-message-empty", "&7No online players are visible."));
            return;
        }
        String nameColor = plugin.getConfig().getString("stealth.online-name-color", "&f");
        String separator = this.color(plugin.getConfig().getString("stealth.online-separator", "&7, "));
        ArrayList<String> colored = new ArrayList<String>();
        for (String name : names) {
            colored.add(this.color(nameColor + name));
        }
        sender.sendMessage(String.join((CharSequence)separator, colored));
    }

    private Set<Material> getMaterialsForFilter(String filter) {
        HashSet<Material> result = new HashSet<Material>();
        List<String> names = plugin.getConfig().getStringList("filters." + filter.toLowerCase(Locale.ROOT));
        for (String name : names) {
            try {
                result.add(Material.valueOf((String)name.toUpperCase(Locale.ROOT)));
            }
            catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown ore material in config: " + name);
            }
        }
        return result;
    }

    private boolean isValidFilter(String filter) {
        return plugin.getConfig().isList("filters." + filter.toLowerCase(Locale.ROOT));
    }

    private int clampRadius(int radius) {
        int max = Math.max(8, plugin.getConfig().getInt("general.max-radius", 128));
        return Math.max(4, Math.min(max, radius));
    }

    private int clampVerticalRadius(int radius) {
        int max = Math.max(16, plugin.getConfig().getInt("general.max-vertical-radius", 192));
        return Math.max(8, Math.min(max, radius));
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (!(sender instanceof Player)) {
            this.msg(sender, "&cThis command is only for in-game admin players.");
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("rumahkita.orespec.use")) {
            this.msg((CommandSender)player, this.pref() + plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            if (this.sessions.containsKey(player.getUniqueId())) {
                this.disableSession(player.getUniqueId(), player, true, true);
            } else {
                this.enableSession(player);
            }
            return true;
        }
        switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "on": {
                this.enableSession(player);
                break;
            }
            case "off": {
                this.disableSession(player.getUniqueId(), player, true, true);
                break;
            }
            case "scan": {
                if (!this.sessions.containsKey(player.getUniqueId())) {
                    this.enableSession(player);
                }
                this.refreshMarkers(player, true);
                Session session = this.sessions.get(player.getUniqueId());
                this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, plugin.getConfig().getString("messages.scan-done", "&aScan selesai. Marker: &f%count% &7/ ditemukan: &f%found%")));
                break;
            }
            case "radius": {
                Session session;
                int radius;
                if (args.length < 2) {
                    this.msg((CommandSender)player, this.pref() + "&e/spec2 radius <angka>");
                    return true;
                }
                try {
                    radius = this.clampRadius(Integer.parseInt(args[1]));
                }
                catch (NumberFormatException e) {
                    this.msg((CommandSender)player, this.pref() + "&cRadius must be a number.");
                    return true;
                }
                if (!this.sessions.containsKey(player.getUniqueId())) {
                    this.enableSession(player);
                }
                if ((session = this.sessions.get(player.getUniqueId())) == null) {
                    return true;
                }
                session.radius = radius;
                this.refreshMarkers(player, true);
                this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, plugin.getConfig().getString("messages.radius-set", "&aRadius changed to &f%radius%&a. Markers: &f%count%")));
                break;
            }
            case "vradius": 
            case "vertical": 
            case "yradius": {
                Session session;
                int radius;
                if (args.length < 2) {
                    this.msg((CommandSender)player, this.pref() + "&e/spec2 vradius <angka>");
                    return true;
                }
                try {
                    radius = this.clampVerticalRadius(Integer.parseInt(args[1]));
                }
                catch (NumberFormatException e) {
                    this.msg((CommandSender)player, this.pref() + "&cVertical radius must be a number.");
                    return true;
                }
                if (!this.sessions.containsKey(player.getUniqueId())) {
                    this.enableSession(player);
                }
                if ((session = this.sessions.get(player.getUniqueId())) == null) {
                    return true;
                }
                session.verticalRadius = radius;
                this.refreshMarkers(player, true);
                this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, plugin.getConfig().getString("messages.vradius-set", "&aVertical radius changed to &f%vradius%&a. Markers: &f%count%")));
                break;
            }
            case "filter": {
                Session session;
                if (args.length < 2) {
                    this.msg((CommandSender)player, this.pref() + "&e/spec2 filter <all|valuable|diamond|netherite|emerald>");
                    return true;
                }
                String filter = args[1].toLowerCase(Locale.ROOT);
                if (!this.isValidFilter(filter)) {
                    this.msg((CommandSender)player, this.pref() + "&cFilter not found. Try: all, valuable, diamond, netherite, emerald");
                    return true;
                }
                if (!this.sessions.containsKey(player.getUniqueId())) {
                    this.enableSession(player);
                }
                if ((session = this.sessions.get(player.getUniqueId())) == null) {
                    return true;
                }
                session.filter = filter;
                this.refreshMarkers(player, true);
                this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, plugin.getConfig().getString("messages.filter-set", "&aFilter changed to &f%filter%&a. Markers: &f%count%")));
                break;
            }
            case "deep": {
                Session session;
                if (!this.sessions.containsKey(player.getUniqueId())) {
                    this.enableSession(player);
                }
                if ((session = this.sessions.get(player.getUniqueId())) == null) {
                    return true;
                }
                session.radius = this.clampRadius(plugin.getConfig().getInt("presets.deep.radius", 64));
                session.verticalRadius = this.clampVerticalRadius(plugin.getConfig().getInt("presets.deep.vertical-radius", 192));
                session.filter = plugin.getConfig().getString("presets.deep.filter", "valuable").toLowerCase(Locale.ROOT);
                if (!this.isValidFilter(session.filter)) {
                    session.filter = "valuable";
                }
                this.refreshMarkers(player, true);
                this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, plugin.getConfig().getString("messages.deep-set", "&aDeep mode active. Markers: &f%count% &7| Radius: &f%radius%/%vradius%")));
                break;
            }
            case "list": {
                if (!player.hasPermission("rumahkita.orespec.admin") && !player.hasPermission("rumahkita.orespec.see")) {
                    this.msg((CommandSender)player, this.pref() + plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission."));
                    return true;
                }
                if (this.sessions.isEmpty()) {
                    this.msg((CommandSender)player, this.pref() + plugin.getConfig().getString("messages.list-empty", "&7No admins are currently in /spec2."));
                    return true;
                }
                this.msg((CommandSender)player, this.pref() + plugin.getConfig().getString("messages.list-header", "&bAdmins currently in /spec2:"));
                for (UUID uuid : this.sessions.keySet()) {
                    Player p = Bukkit.getPlayer((UUID)uuid);
                    Session s = this.sessions.get(uuid);
                    if (p == null) continue;
                    this.msg((CommandSender)player, "&7- &f" + p.getName() + " &7marker=&f" + (s == null ? 0 : s.lastMarkerCount));
                }
                break;
            }
            case "status": {
                Session session = this.sessions.get(player.getUniqueId());
                String text = plugin.getConfig().getString("messages.status", "&7Your Spec2: &f%active% &7| Markers: &f%count% &7| Filter: &f%filter% &7| Radius: &f%radius%/%vradius% &7| All sessions: &f%sessions%");
                this.msg((CommandSender)player, this.pref() + this.applyPlaceholders(player, session, text));
                break;
            }
            case "reload": {
                if (!player.hasPermission("rumahkita.orespec.admin")) {
                    this.msg((CommandSender)player, this.pref() + plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission."));
                    return true;
                }
                plugin.reloadConfig();
                this.startTask();
                for (UUID uuid : new ArrayList<UUID>(this.sessions.keySet())) {
                    Player p = Bukkit.getPlayer((UUID)uuid);
                    if (p == null) continue;
                    this.applyStealthToAll(p);
                    this.refreshMarkers(p, true);
                }
                this.msg((CommandSender)player, this.pref() + plugin.getConfig().getString("messages.reloaded", "&aConfig OreSpectator direload."));
                break;
            }
            default: {
                this.sendHelp(player);
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        this.msg((CommandSender)player, "&8&m-----------------------------");
        this.msg((CommandSender)player, "&bRumahKita OreSpectator &7v1.0.3");
        this.msg((CommandSender)player, "&e/spec2 &7- toggle stealth spectator");
        this.msg((CommandSender)player, "&e/spec2 scan &7- scan ulang marker");
        this.msg((CommandSender)player, "&e/spec2 filter all/valuable/diamond/netherite/emerald");
        this.msg((CommandSender)player, "&e/spec2 radius <4-128> &7- jarak horizontal");
        this.msg((CommandSender)player, "&e/spec2 vradius <8-192> &7- jarak atas/bawah");
        this.msg((CommandSender)player, "&e/spec2 deep &7- mode to search ores deeper down");
        this.msg((CommandSender)player, "&e/spec2 status");
        this.msg((CommandSender)player, "&8&m-----------------------------");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return this.startsWith(List.of("on", "off", "toggle", "scan", "radius", "vradius", "filter", "deep", "list", "reload", "status"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("filter")) {
            return this.startsWith(List.of("all", "valuable", "diamond", "netherite", "emerald"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("radius")) {
            return this.startsWith(List.of("16", "32", "48", "64", "96", "128"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("vradius") || args[0].equalsIgnoreCase("vertical") || args[0].equalsIgnoreCase("yradius"))) {
            return this.startsWith(List.of("64", "96", "128", "160", "192"), args[1]);
        }
        return List.of();
    }

    private List<String> startsWith(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> result = new ArrayList<String>();
        for (String value : values) {
            if (!value.toLowerCase(Locale.ROOT).startsWith(lower)) continue;
            result.add(value);
        }
        return result;
    }

    private String applyPlaceholders(Player player, Session session, String input) {
        String filter = session == null ? "-" : session.filter;
        int radius = session == null ? 0 : session.radius;
        int vradius = session == null ? 0 : session.verticalRadius;
        int count = session == null ? 0 : session.lastMarkerCount;
        int found = session == null ? 0 : session.lastFoundCount;
        boolean active = player != null && this.sessions.containsKey(player.getUniqueId());
        return input.replace("%player%", player == null ? "-" : player.getName()).replace("%filter%", filter).replace("%radius%", String.valueOf(radius)).replace("%vradius%", String.valueOf(vradius)).replace("%count%", String.valueOf(count)).replace("%found%", String.valueOf(found)).replace("%active%", String.valueOf(active)).replace("%sessions%", String.valueOf(this.sessions.size())).replace("%stealth%", String.valueOf(plugin.getConfig().getBoolean("stealth.enabled", true)));
    }

    private String pref() {
        return plugin.getConfig().getString("messages.prefix", "&8[&bOreSpec&8] ");
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(input == null ? "" : input));
    }

    private void msg(CommandSender sender, String input) {
        sender.sendMessage(this.color(input));
    }

    private static final class Session {
        GameMode originalGameMode = GameMode.SURVIVAL;
        int radius = 48;
        int verticalRadius = 96;
        String filter = "valuable";
        int lastMarkerCount = 0;
        int lastFoundCount = 0;
        final List<Entity> markers = new ArrayList<Entity>();

        private Session() {
        }
    }

    private static final class OreHit {
        final Location location;
        final Material material;
        final int distSq;

        OreHit(Location location, Material material, int distSq) {
            this.location = location;
            this.material = material;
            this.distSq = distSq;
        }
    }
}

