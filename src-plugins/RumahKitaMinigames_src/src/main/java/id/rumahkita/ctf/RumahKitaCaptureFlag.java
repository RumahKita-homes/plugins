/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.scoreboard.DisplaySlot
 *  org.bukkit.scoreboard.Objective
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.ScoreboardManager
 */
package id.rumahkita.ctf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public final class RumahKitaCaptureFlag
implements Listener,
CommandExecutor,
TabCompleter {
    private final id.rumahkita.minigames.RumahKitaMinigamesPlugin plugin;

    public RumahKitaCaptureFlag(id.rumahkita.minigames.RumahKitaMinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    private id.rumahkita.ctf.view.CtfAdminUI adminUI;

    private org.bukkit.configuration.file.FileConfiguration customConfig;
    private java.io.File customConfigFile;

    public void reloadConfig() {
        if (customConfigFile == null) {
            customConfigFile = new java.io.File(plugin.getDataFolder(), "RumahKitaCaptureFlag_config.yml");
        }
        customConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(customConfigFile);
    }
    
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        if (customConfig == null) {
            reloadConfig();
        }
        return customConfig;
    }
    
    public void saveConfig() {
        if (customConfig == null || customConfigFile == null) return;
        try {
            getConfig().save(customConfigFile);
        } catch (java.io.IOException ex) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save config to " + customConfigFile, ex);
        }
    }

    private GameState state = GameState.IDLE;
    private final Map<UUID, Participant> participants = new LinkedHashMap<UUID, Participant>();
    private final Map<UUID, Location> deathRespawnTargets = new HashMap<UUID, Location>();
    private BukkitTask countdownTask;
    private BukkitTask gameTask;
    private BukkitTask particleTask;
    private BukkitTask scoreboardTask;
    private BukkitTask captureRotationTask;
    private CtfPlaceholderExpansion placeholderExpansion;
    private int countdownLeft;
    private int timeLeft;
    private boolean enabled;
    private int minPlayers;
    private int countdownSeconds;
    private int durationSeconds;
    private int pointsPerSecond;
    private boolean freezeBeforeStart;
    private boolean teleportToExitAfterEvent;
    private boolean restoreScoreboardAfterEvent;
    private boolean announceEveryMinute;
    private boolean allowJoinWhileCountdown;
    private boolean allowJoinWhileRunning;
    private boolean onlyAliveCanScore;
    private boolean winnersOnlyAlive;
    private boolean soundsEnabled;
    private boolean particlesEnabled;
    private boolean showCaptureRing;
    private boolean rewardsEnabled;
    private boolean arenaBoundaryEnabled;
    private boolean forceScoreboardEnabled;
    private int forceScoreboardTicks;
    private boolean showCaptureStatusInScoreboard;
    private boolean zoneActionBarEnabled;
    private boolean zoneChatMessageEnabled;
    private String captureShape;
    private final List<CapturePoint> capturePoints = new ArrayList<CapturePoint>();
    private int activeCaptureIndex = 0;
    private int captureRotateSeconds;
    private int captureNextRotateSeconds;
    private boolean captureRotationEnabled;
    private boolean captureRotationRandom;
    private boolean captureAnnounceOnRotate;
    private boolean inventorySystemEnabled;
    private boolean restoreInventoryAfterEvent;
    private boolean clearInventoryOnJoin;
    private boolean restoreBackupOnJoin;
    private boolean preventItemDrop;
    private boolean preventItemPickup;
    private boolean preventInventoryClick;
    private boolean clearDeathDrops;
    private boolean preventBlockBreak;
    private boolean preventBlockPlace;
    private boolean resetHealthFoodOnJoin;
    private boolean kitEnabled;
    private boolean kitKnockbackStick;
    private int kitStickSlot;
    private int kitKnockbackLevel;
    private String kitStickName;
    private List<String> kitStickLore;
    private String prefix;

    public void onEnable() {
        this.loadSettings();
        plugin.getServer().getPluginManager().registerEvents((Listener)this, this.plugin);
        if (plugin.getCommand("rkctf") != null) {
            plugin.getCommand("rkctf").setExecutor((CommandExecutor)this);
            plugin.getCommand("rkctf").setTabCompleter((TabCompleter)this);
        }
        this.startParticleTask();
        this.startScoreboardTask();
        this.registerPlaceholderExpansion();
        this.adminUI = new id.rumahkita.ctf.view.CtfAdminUI(this.plugin, this);
        plugin.getLogger().info("RumahKitaCaptureFlag v1.5.0 enabled.");
    }

    public void onDisable() {
        this.stopAllTasks();
        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
            this.placeholderExpansion = null;
        }
        this.restoreAllPlayers(true);
        this.participants.clear();
        plugin.getLogger().info("RumahKitaCaptureFlag disabled.");
    }

    public void loadSettings() {
        reloadConfig();
        this.enabled = getConfig().getBoolean("enabled", true);
        this.minPlayers = Math.max(1, getConfig().getInt("settings.min-players", 2));
        this.countdownSeconds = Math.max(3, getConfig().getInt("settings.countdown-seconds", 15));
        this.durationSeconds = Math.max(10, getConfig().getInt("settings.duration-seconds", 300));
        this.pointsPerSecond = Math.max(1, getConfig().getInt("settings.points-per-second", 1));
        this.freezeBeforeStart = getConfig().getBoolean("settings.freeze-before-start", true);
        this.teleportToExitAfterEvent = getConfig().getBoolean("settings.teleport-to-exit-after-event", true);
        this.restoreScoreboardAfterEvent = getConfig().getBoolean("settings.restore-scoreboard-after-event", true);
        this.announceEveryMinute = getConfig().getBoolean("settings.announce-every-minute", true);
        this.allowJoinWhileCountdown = getConfig().getBoolean("settings.allow-join-while-countdown", true);
        this.allowJoinWhileRunning = getConfig().getBoolean("settings.allow-join-while-running", false);
        this.onlyAliveCanScore = getConfig().getBoolean("settings.only-alive-can-score", true);
        this.winnersOnlyAlive = getConfig().getBoolean("settings.winners-only-alive", true);
        this.arenaBoundaryEnabled = getConfig().getBoolean("arena.enabled", false);
        this.soundsEnabled = getConfig().getBoolean("sounds.enabled", true);
        this.particlesEnabled = getConfig().getBoolean("particles.enabled", true);
        this.showCaptureRing = getConfig().getBoolean("particles.show-capture-ring", true);
        this.rewardsEnabled = getConfig().getBoolean("rewards.enabled", true);
        this.forceScoreboardEnabled = getConfig().getBoolean("scoreboard.force-update.enabled", true);
        this.forceScoreboardTicks = Math.max(20, getConfig().getInt("scoreboard.force-update.interval-ticks", 20));
        this.showCaptureStatusInScoreboard = getConfig().getBoolean("scoreboard.show-capture-status", true);
        this.zoneActionBarEnabled = getConfig().getBoolean("capture.actionbar.enabled", true);
        this.zoneChatMessageEnabled = getConfig().getBoolean("capture.chat-message-every-second", false);
        this.captureShape = getConfig().getString("capture.shape", "CIRCLE").toUpperCase(Locale.ROOT);
        this.captureRotationEnabled = getConfig().getBoolean("capture.rotation.enabled", true);
        this.captureRotationRandom = getConfig().getBoolean("capture.rotation.random", false);
        this.captureAnnounceOnRotate = getConfig().getBoolean("capture.rotation.announce", true);
        this.captureNextRotateSeconds = this.captureRotateSeconds = Math.max(5, getConfig().getInt("capture.rotation.interval-seconds", 25));
        this.loadCapturePoints();
        this.inventorySystemEnabled = getConfig().getBoolean("inventory.enabled", true);
        this.restoreInventoryAfterEvent = getConfig().getBoolean("inventory.restore-after-event", true);
        this.clearInventoryOnJoin = getConfig().getBoolean("inventory.clear-on-join", true);
        this.restoreBackupOnJoin = getConfig().getBoolean("inventory.restore-backup-on-join", true);
        this.preventItemDrop = getConfig().getBoolean("inventory.prevent-item-drop", true);
        this.preventItemPickup = getConfig().getBoolean("inventory.prevent-item-pickup", true);
        this.preventInventoryClick = getConfig().getBoolean("inventory.prevent-inventory-click", false);
        this.clearDeathDrops = getConfig().getBoolean("inventory.clear-death-drops", true);
        this.preventBlockBreak = getConfig().getBoolean("inventory.prevent-block-break", true);
        this.preventBlockPlace = getConfig().getBoolean("inventory.prevent-block-place", true);
        this.resetHealthFoodOnJoin = getConfig().getBoolean("inventory.reset-health-food-on-join", true);
        this.kitEnabled = getConfig().getBoolean("kit.enabled", true);
        this.kitKnockbackStick = getConfig().getBoolean("kit.knockback-stick.enabled", true);
        this.kitStickSlot = Math.max(0, Math.min(8, getConfig().getInt("kit.knockback-stick.slot", 0)));
        this.kitKnockbackLevel = Math.max(1, getConfig().getInt("kit.knockback-stick.knockback-level", 2));
        this.kitStickName = getConfig().getString("kit.knockback-stick.name", "&c&lCTF Stick &7(Knockback II)");
        this.kitStickLore = getConfig().getStringList("kit.knockback-stick.lore");
        this.prefix = "";
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().warning("PlaceholderAPI not found. Placeholder %rumahkitactf_*% is inactive.");
            return;
        }
        this.placeholderExpansion = new CtfPlaceholderExpansion(this);
        this.placeholderExpansion.register();
        plugin.getLogger().info("PlaceholderAPI hooked. Placeholders %rumahkitactf_*% are active.");
    }

    private void stopAllTasks() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
            this.countdownTask = null;
        }
        if (this.gameTask != null) {
            this.gameTask.cancel();
            this.gameTask = null;
        }
        if (this.captureRotationTask != null) {
            this.captureRotationTask.cancel();
            this.captureRotationTask = null;
        }
        if (this.particleTask != null) {
            this.particleTask.cancel();
            this.particleTask = null;
        }
        if (this.scoreboardTask != null) {
            this.scoreboardTask.cancel();
            this.scoreboardTask = null;
        }
    }

    private void startScoreboardTask() {
        if (this.scoreboardTask != null) {
            this.scoreboardTask.cancel();
        }
        this.scoreboardTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!this.enabled || !this.forceScoreboardEnabled) {
                return;
            }
            if (this.state == GameState.IDLE || this.participants.isEmpty()) {
                return;
            }
            this.updateAllScoreboards();
        }, 5L, (long)this.forceScoreboardTicks);
    }

    private void startParticleTask() {
        if (this.particleTask != null) {
            this.particleTask.cancel();
        }
        this.particleTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!(this.enabled && this.particlesEnabled && this.showCaptureRing)) {
                return;
            }
            if (this.state == GameState.IDLE || this.participants.isEmpty()) {
                return;
            }
            this.showActiveCaptureMarker();
        }, 20L, 10L);
    }

    private void loadCapturePoints() {
        this.capturePoints.clear();
        if (getConfig().isConfigurationSection("capture.points")) {
            for (String id : getConfig().getConfigurationSection("capture.points").getKeys(false)) {
                if (!getConfig().getBoolean("capture.points." + id + ".enabled", true)) continue;
                String path = "capture.points." + id;
                String world = getConfig().getString(path + ".world", getConfig().getString("capture.world", "world"));
                String name = getConfig().getString(path + ".name", id);
                String shape = getConfig().getString(path + ".shape", getConfig().getString("capture.shape", "CIRCLE"));
                double radius = getConfig().getDouble(path + ".radius", getConfig().getDouble("capture.radius", 13.0));
                double radiusX = getConfig().getDouble(path + ".radius-x", radius);
                double radiusZ = getConfig().getDouble(path + ".radius-z", radius);
                double x = getConfig().getDouble(path + ".x", getConfig().getDouble("capture.center-x", 4286.0));
                double z = getConfig().getDouble(path + ".z", getConfig().getDouble("capture.center-z", 2394.0));
                double minY = getConfig().getDouble(path + ".min-y", getConfig().getDouble("capture.min-y", 120.0));
                double maxY = getConfig().getDouble(path + ".max-y", getConfig().getDouble("capture.max-y", 150.0));
                this.capturePoints.add(new CapturePoint(id, name, world, shape, x, z, radius, radiusX, radiusZ, minY, maxY));
            }
        }
        if (this.capturePoints.isEmpty()) {
            this.capturePoints.add(new CapturePoint("legacy_center", getConfig().getString("capture.name", "Tengah"), getConfig().getString("capture.world", "world"), getConfig().getString("capture.shape", "CIRCLE"), getConfig().getDouble("capture.center-x", 4286.0), getConfig().getDouble("capture.center-z", 2394.0), getConfig().getDouble("capture.radius", 13.0), getConfig().getDouble("capture.radius-x", getConfig().getDouble("capture.radius", 13.0)), getConfig().getDouble("capture.radius-z", getConfig().getDouble("capture.radius", 13.0)), getConfig().getDouble("capture.min-y", 120.0), getConfig().getDouble("capture.max-y", 150.0)));
        }
        if (this.activeCaptureIndex < 0 || this.activeCaptureIndex >= this.capturePoints.size()) {
            this.activeCaptureIndex = 0;
        }
    }

    private CapturePoint getActiveCapture() {
        if (this.capturePoints.isEmpty()) {
            this.loadCapturePoints();
        }
        if (this.capturePoints.isEmpty()) {
            return null;
        }
        if (this.activeCaptureIndex < 0 || this.activeCaptureIndex >= this.capturePoints.size()) {
            this.activeCaptureIndex = 0;
        }
        return this.capturePoints.get(this.activeCaptureIndex);
    }

    private void startCaptureRotationTask() {
        if (this.captureRotationTask != null) {
            this.captureRotationTask.cancel();
            this.captureRotationTask = null;
        }
        this.captureNextRotateSeconds = this.captureRotateSeconds;
        if (!this.captureRotationEnabled || this.capturePoints.size() <= 1) {
            return;
        }
        this.captureRotationTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.state != GameState.RUNNING) {
                return;
            }
            --this.captureNextRotateSeconds;
            if (this.captureNextRotateSeconds <= 0) {
                this.rotateCapturePoint(true);
                this.captureNextRotateSeconds = this.captureRotateSeconds;
            }
        }, 20L, 20L);
    }

    private void rotateCapturePoint(boolean announce) {
        if (this.capturePoints.size() <= 1) {
            this.activeCaptureIndex = 0;
            return;
        }
        int old = this.activeCaptureIndex;
        if (this.captureRotationRandom) {
            int next = old;
            int guard = 0;
            while (next == old && guard++ < 20) {
                next = ThreadLocalRandom.current().nextInt(this.capturePoints.size());
            }
            this.activeCaptureIndex = next;
        } else {
            this.activeCaptureIndex = (this.activeCaptureIndex + 1) % this.capturePoints.size();
        }
        CapturePoint point = this.getActiveCapture();
        if (point == null) {
            return;
        }
        for (Participant p : this.participants.values()) {
            p.lastInsideCapture = false;
        }
        
        Location targetLoc = new Location(Bukkit.getWorld((String)point.world), point.x, point.minY, point.z);
        
        if (announce && this.captureAnnounceOnRotate) {
            this.broadcast(getConfig().getString("messages.capture-moved", "&eCapture point moved to &d%point%&e! Follow your compass.").replace("%point%", point.name));
            for (Player player : this.getOnlineParticipants()) {
                player.sendTitle(this.color("&d&lCAPTURE MOVED"), this.color("&fActive location: &e" + point.name), 5, 35, 8);
                this.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
                player.setCompassTarget(targetLoc);
            }
        } else {
            for (Player player : this.getOnlineParticipants()) {
                player.setCompassTarget(targetLoc);
            }
        }
    }

    private void showActiveCaptureMarker() {
        CapturePoint point = this.getActiveCapture();
        if (point == null) {
            return;
        }
        World world = Bukkit.getWorld((String)point.world);
        if (world == null) {
            return;
        }
        double y = (point.minY + point.maxY) / 2.0;
        double radius = Math.max(1.0, point.radius);
        int circlePoints = Math.max(40, (int)(radius * 10.0));
        for (int i = 0; i < circlePoints; ++i) {
            double angle = Math.PI * 2 * (double)i / (double)circlePoints;
            double x = point.x + Math.cos(angle) * radius;
            double z = point.z + Math.sin(angle) * radius;
            world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.01);
            if (i % 4 != 0) continue;
            world.spawnParticle(Particle.FLAME, x, y + 0.35, z, 1, 0.0, 0.0, 0.0, 0.01);
        }
        for (double yy = point.minY; yy <= point.maxY + 8.0; yy += 0.75) {
            world.spawnParticle(Particle.END_ROD, point.x, yy, point.z, 2, 0.08, 0.02, 0.08, 0.01);
        }
        world.spawnParticle(Particle.FLAME, point.x, y + 1.0, point.z, 12, 0.6, 0.6, 0.6, 0.02);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("admin")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (player.hasPermission("rumahkita.ctf.admin")) {
                if (this.adminUI != null) this.adminUI.open(player);
            } else {
                player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "No permission.");
            }
            return true;
        }
        if (sub.equals("join")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(this.prefix + "This command is only for players.");
                return true;
            }
            Player player = (Player)sender;
            this.joinEvent(player);
            return true;
        }
        if (sub.equals("leave")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(this.prefix + "This command is only for players.");
                return true;
            }
            Player player = (Player)sender;
            this.leaveEvent(player, true);
            return true;
        }
        if (sub.equals("status")) {
            this.sendStatus(sender);
            return true;
        }
        if (!sender.hasPermission("rumahkita.ctf.admin")) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "You do not have CTF admin permission.");
            return true;
        }
        switch (sub) {
            case "start": {
                this.startCountdown(sender, false);
                break;
            }
            case "forcestart": {
                this.startCountdown(sender, true);
                break;
            }
            case "stop": {
                this.stopEvent(sender);
                break;
            }
            case "reload": {
                this.loadSettings();
                this.startParticleTask();
                this.startScoreboardTask();
                sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Config successfully reloaded.");
                break;
            }
            case "list": {
                this.sendParticipantList(sender);
                break;
            }
            case "setside1": {
                this.setLocation(sender, "spawns.side1");
                break;
            }
            case "setside2": {
                this.setLocation(sender, "spawns.side2");
                break;
            }
            case "setexit": {
                this.setLocation(sender, "spawns.exit");
                break;
            }
            case "setarena": {
                this.setArena(sender, args);
                break;
            }
            case "setcapture": {
                this.setCapture(sender, args);
                break;
            }
            case "setcapturebox": {
                this.setCaptureBox(sender, args);
                break;
            }
            case "addcapturebox": {
                this.addCaptureBox(sender, args);
                break;
            }
            case "clearcaptures": {
                this.clearCaptures(sender);
                break;
            }
            case "nextcapture": {
                this.rotateCapturePoint(true);
                sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Capture point manually moved to: " + this.getActiveCaptureName());
                break;
            }
            case "listcaptures": {
                this.listCapturePoints(sender);
                break;
            }
            case "check": {
                this.checkPosition(sender);
                break;
            }
            case "setduration": {
                this.setDuration(sender, args);
                break;
            }
            case "reset": {
                this.resetEvent(sender);
                break;
            }
            case "restoreitems": {
                this.restoreItemsCommand(sender, args);
                break;
            }
            case "backupstatus": {
                this.backupStatusCommand(sender, args);
                break;
            }
            case "clearbackup": {
                this.clearBackupCommand(sender, args);
                break;
            }
            default: {
                this.sendHelp(sender);
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(this.color("&8&m------------------------------"));
        sender.sendMessage(this.color("&d&lRumahKita Capture Flag"));
        sender.sendMessage(this.color("&e/rkctf join &7- join event"));
        sender.sendMessage(this.color("&e/rkctf leave &7- leave event"));
        sender.sendMessage(this.color("&e/rkctf status &7- view event status"));
        if (sender.hasPermission("rumahkita.ctf.admin")) {
            sender.sendMessage(this.color("&cAdmin:"));
            sender.sendMessage(this.color("&e/rkctf start &7- start countdown"));
            sender.sendMessage(this.color("&e/rkctf forcestart &7- force start"));
            sender.sendMessage(this.color("&e/rkctf stop &7- stop event"));
            sender.sendMessage(this.color("&e/rkctf setside1 &7- set spawn side 1"));
            sender.sendMessage(this.color("&e/rkctf setside2 &7- set spawn side 2"));
            sender.sendMessage(this.color("&e/rkctf setexit &7- set exit location"));
            sender.sendMessage(this.color("&e/rkctf setarena <radius> &7- set arena center"));
            sender.sendMessage(this.color("&e/rkctf setcapture <radius> <minY> <maxY> &7- set circle capture"));
            sender.sendMessage(this.color("&e/rkctf setcapturebox <radiusX> <radiusZ> <minY> <maxY> &7- set primary box capture"));
            sender.sendMessage(this.color("&e/rkctf addcapturebox <radiusX> <radiusZ> <minY> <maxY> &7- add new capture"));
            sender.sendMessage(this.color("&e/rkctf clearcaptures &7- clear all extra captures"));
            sender.sendMessage(this.color("&e/rkctf nextcapture &7- manually move active capture point"));
            sender.sendMessage(this.color("&e/rkctf listcaptures &7- view all capture points"));
            sender.sendMessage(this.color("&e/rkctf check &7- check if you are in capture zone"));
            sender.sendMessage(this.color("&e/rkctf setduration <seconds> &7- set event duration"));
            sender.sendMessage(this.color("&e/rkctf restoreitems <player> &7- manually restore backup item"));
            sender.sendMessage(this.color("&e/rkctf backupstatus <player> &7- check item backup"));
            sender.sendMessage(this.color("&e/rkctf clearbackup <player> &7- clear item backup"));
            sender.sendMessage(this.color("&e/rkctf admin &7- open admin GUI menu"));
            sender.sendMessage(this.color("&e/rkctf reload &7- reload config"));
        }
        sender.sendMessage(this.color("&8&m------------------------------"));
    }

    private void joinEvent(Player player) {
        if (!this.enabled) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event is currently disabled.");
            return;
        }
        if (!player.hasPermission("rumahkita.ctf.use")) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "You do not have permission to join the event.");
            return;
        }
        if (this.participants.containsKey(player.getUniqueId())) {
            player.sendMessage(this.prefix + this.msg("messages.already-joined"));
            return;
        }
        if (this.state == GameState.RUNNING && !this.allowJoinWhileRunning) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event is already running, cannot join right now.");
            return;
        }
        if (this.state == GameState.COUNTDOWN && !this.allowJoinWhileCountdown) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event is in countdown, cannot join right now.");
            return;
        }
        if (this.state == GameState.ENDING) {
            player.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event is ending, wait for reset.");
            return;
        }
        if (this.restoreBackupOnJoin && this.hasBackup(player.getUniqueId()) && !this.participants.containsKey(player.getUniqueId())) {
            this.restoreInventoryBackup(player, true, false);
            player.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Old event backup items found and restored before joining.");
        }
        String team = this.chooseTeam();
        Participant participant = new Participant(player, team);
        this.participants.put(player.getUniqueId(), participant);
        this.preparePlayerForEvent(player, participant);
        Location spawn = this.getTeamSpawn(team);
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.setGameMode(GameMode.SURVIVAL);
        this.updateScoreboard(player);
        this.broadcast(this.applyPlaceholders(this.msg("messages.join"), player, participant, 0));
    }

    private String chooseTeam() {
        int side1 = 0;
        int side2 = 0;
        for (Participant p : this.participants.values()) {
            if (p.team.equalsIgnoreCase("Side 1")) {
                ++side1;
                continue;
            }
            ++side2;
        }
        return side1 <= side2 ? "Side 1" : "Side 2";
    }

    private void preparePlayerForEvent(Player player, Participant participant) {
        if (participant.prepared) {
            return;
        }
        participant.originalLocation = player.getLocation().clone();
        participant.previousScoreboard = player.getScoreboard();
        if (this.inventorySystemEnabled) {
            this.saveInventoryBackup(player, participant.originalLocation);
            if (this.clearInventoryOnJoin) {
                PlayerInventory inv = player.getInventory();
                inv.setStorageContents(new ItemStack[inv.getStorageContents().length]);
                inv.setArmorContents(new ItemStack[4]);
                inv.setItemInOffHand(null);
                player.updateInventory();
            }
            if (this.resetHealthFoodOnJoin) {
                try {
                    player.setHealth(Math.max(1.0, player.getMaxHealth()));
                }
                catch (Exception ignored) {
                    try {
                        player.setHealth(20.0);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                player.setFoodLevel(20);
                player.setSaturation(8.0f);
                player.setFireTicks(0);
                player.setFallDistance(0.0f);
            }
            this.giveEventKit(player);
        }
        participant.prepared = true;
    }

    private void giveEventKit(Player player) {
        if (!this.kitEnabled) {
            return;
        }
        if (this.kitKnockbackStick) {
            player.getInventory().setItem(this.kitStickSlot, this.createKnockbackStick());
        }
        List<String> commands = getConfig().getStringList("kit.commands");
        for (String raw : commands) {
            String cmd = raw.replace("%player%", player.getName());
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)cmd);
        }
        player.updateInventory();
    }

    private ItemStack createKnockbackStick() {
        ItemStack item = new ItemStack(Material.STICK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Enchantment knockback;
            meta.setDisplayName(this.color(this.kitStickName));
            if (!this.kitStickLore.isEmpty()) {
                ArrayList<String> lore = new ArrayList<String>();
                for (String line : this.kitStickLore) {
                    lore.add(this.color(line));
                }
                meta.setLore(lore);
            }
            if ((knockback = Enchantment.getByKey((NamespacedKey)NamespacedKey.minecraft((String)"knockback"))) != null) {
                meta.addEnchant(knockback, this.kitKnockbackLevel, true);
            }
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            item.setItemMeta(meta);
        }
        return item;
    }

    private void leaveEvent(Player player, boolean announce) {
        Participant participant = this.participants.remove(player.getUniqueId());
        if (participant == null) {
            player.sendMessage(this.prefix + this.msg("messages.not-joined"));
            return;
        }
        this.restorePlayer(player, participant, true, true);
        if (announce) {
            this.broadcast(this.applyPlaceholders(this.msg("messages.leave"), player, participant, 0));
        }
        if (this.participants.isEmpty() && this.state != GameState.IDLE) {
            this.forceStopNoPlayers();
        }
    }

    private void startCountdown(CommandSender sender, boolean force) {
        if (this.state != GameState.IDLE) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Event is already running or in countdown.");
            return;
        }
        if (!force && this.participants.size() < this.minPlayers) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Minimum players: " + this.minPlayers + ". Current: " + this.participants.size());
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Use /rkctf forcestart to force start.");
            return;
        }
        if (this.participants.isEmpty()) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "No players have joined yet.");
            return;
        }
        this.state = GameState.COUNTDOWN;
        this.countdownLeft = this.countdownSeconds;
        this.teleportAllToTeams();
        this.updateAllScoreboards();
        this.broadcast("&eCapture Flag is starting. Players are locked in their sides.");
        this.countdownTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.countdownLeft <= 0) {
                if (this.countdownTask != null) {
                    this.countdownTask.cancel();
                    this.countdownTask = null;
                }
                this.startGame();
                return;
            }
            this.broadcast(this.applyTime(this.msg("messages.countdown"), this.countdownLeft));
            for (Player player : this.getOnlineParticipants()) {
                player.sendTitle(this.color("&d&lCapture Flag"), this.color("&eStarts in &c" + this.countdownLeft + " &eseconds"), 5, 20, 5);
                this.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
            }
            --this.countdownLeft;
        }, 0L, 20L);
    }

    private void startGame() {
        this.state = GameState.RUNNING;
        this.timeLeft = this.durationSeconds;
        this.activeCaptureIndex = 0;
        this.captureNextRotateSeconds = this.captureRotateSeconds;
        for (Participant p : this.participants.values()) {
            p.alive = true;
            p.points = 0;
        }
        this.teleportAllToTeams();
        this.updateAllScoreboards();
        this.startCaptureRotationTask();
        this.broadcast(this.msg("messages.started"));
        CapturePoint active = this.getActiveCapture();
        if (active != null) {
            this.broadcast(getConfig().getString("messages.capture-active", "&eActive capture point: &d%point%&e. Follow your compass.").replace("%point%", active.name));
            Location targetLoc = new Location(Bukkit.getWorld((String)active.world), active.x, active.minY, active.z);
            for (Player player : this.getOnlineParticipants()) {
                player.setCompassTarget(targetLoc);
            }
        }
        for (Player player : this.getOnlineParticipants()) {
            player.sendTitle(this.color("&a&lFIGHT!"), this.color("&fCapture the zone and collect points!"), 10, 40, 10);
            this.playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.0f);
        }
        this.gameTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.state != GameState.RUNNING) {
                return;
            }
            this.tickGame();
        }, 20L, 20L);
    }

    private void tickGame() {
        this.processPlayers();
        if (this.announceEveryMinute && this.timeLeft > 0 && this.timeLeft % 60 == 0) {
            this.broadcast(this.applyTime(this.msg("messages.time-left"), this.timeLeft));
        }
        if (this.timeLeft == 30 || this.timeLeft == 10 || this.timeLeft == 5) {
            this.broadcast(this.applyTime(this.msg("messages.time-left"), this.timeLeft));
            for (Player p : this.getOnlineParticipants()) {
                this.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.0f);
            }
        }
        if (this.timeLeft <= 0 || this.getAliveCount() <= 0) {
            this.endEvent(false);
            return;
        }
        --this.timeLeft;
    }

    private void processPlayers() {
        for (Participant participant : this.participants.values()) {
            boolean insideCapture;
            Player player = Bukkit.getPlayer((UUID)participant.uuid);
            if (player == null || !player.isOnline() || this.onlyAliveCanScore && !participant.alive || player.isDead()) continue;
            boolean wasInsideCapture = participant.lastInsideCapture;
            participant.lastInsideCapture = insideCapture = this.isInsideCapture(player.getLocation());
            if (insideCapture) {
                participant.points += this.pointsPerSecond;
                if (this.zoneChatMessageEnabled) {
                    player.sendMessage(this.prefix + this.applyPlaceholders(this.msg("messages.zone-score"), player, participant, this.pointsPerSecond));
                }
                if (this.zoneActionBarEnabled) {
                    this.sendActionBar(player, this.applyPlaceholders(this.msg("messages.zone-actionbar"), player, participant, this.pointsPerSecond));
                }
                this.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.25f, 1.8f);
                continue;
            }
            if (this.zoneActionBarEnabled) {
                CapturePoint cp = this.getActiveCapture();
                String distStr = "?";
                if (cp != null && cp.world.equals(player.getWorld().getName())) {
                    int dist = (int) player.getLocation().distance(new Location(player.getWorld(), cp.x, player.getLocation().getY(), cp.z));
                    distStr = String.valueOf(dist);
                }
                this.sendActionBar(player, this.color("&7CTF: &fRun to &d" + this.getActiveCaptureName() + " &e(" + distStr + "m) &8| &fPoints: &a" + participant.points));
            }
            if (wasInsideCapture && this.zoneActionBarEnabled) {
                this.sendActionBar(player, this.color("&7Left capture zone. Your points: &a" + participant.points));
            }
            if (!this.arenaBoundaryEnabled || this.isInsideArena(player.getLocation())) continue;
            Location spawn = this.getTeamSpawn(participant.team);
            if (spawn != null) {
                player.teleport(spawn);
            }
            player.sendMessage(this.prefix + this.msg("messages.out-of-arena"));
        }
    }

    private void stopEvent(CommandSender sender) {
        if (this.state == GameState.IDLE) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "The event is not running.");
            return;
        }
        this.endEvent(true);
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Event force stopped.");
    }

    private void forceStopNoPlayers() {
        this.stopAllGameTasksOnly();
        this.state = GameState.IDLE;
    }

    private void endEvent(boolean forced) {
        if (this.state == GameState.ENDING) {
            return;
        }
        this.state = GameState.ENDING;
        this.stopAllGameTasksOnly();
        List<Participant> winners = this.getWinners();
        this.broadcast(this.msg("messages.ended"));
        this.broadcast("&8&m----------------------------------");
        this.broadcast("&d&lRUMAHKITA CTF S2 RESULTS");
        if (winners.isEmpty()) {
            this.broadcast(this.msg("messages.no-winners"));
        } else {
            int rank = 1;
            for (Participant winner : winners) {
                String line = this.msg("messages.winner-line").replace("%rank%", String.valueOf(rank)).replace("%player%", winner.name).replace("%points%", String.valueOf(winner.points));
                this.broadcast(line);
                ++rank;
            }
        }
        this.broadcast("&8&m----------------------------------");
        for (Player player : this.getOnlineParticipants()) {
            this.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
            player.sendTitle(this.color("&d&lCTF SELESAI"), this.color("&fCheck winners in chat!"), 10, 60, 10);
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.restoreAllPlayers(this.teleportToExitAfterEvent);
            this.giveRewardsAfterRestore(winners);
            this.participants.clear();
            this.state = GameState.IDLE;
        }, 60L);
    }

    private void stopAllGameTasksOnly() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
            this.countdownTask = null;
        }
        if (this.gameTask != null) {
            this.gameTask.cancel();
            this.gameTask = null;
        }
        if (this.captureRotationTask != null) {
            this.captureRotationTask.cancel();
            this.captureRotationTask = null;
        }
    }

    private List<Participant> getWinners() {
        ArrayList<Participant> list = new ArrayList<Participant>();
        for (Participant p2 : this.participants.values()) {
            if (this.winnersOnlyAlive && !p2.alive) continue;
            list.add(p2);
        }
        list.sort(Comparator.comparingInt((Participant p) -> p.points).reversed().thenComparing((Participant p) -> p.name));
        if (list.size() > 3) {
            return new ArrayList<Participant>(list.subList(0, 3));
        }
        return list;
    }

    private void giveRewardsAfterRestore(List<Participant> winners) {
        if (!this.rewardsEnabled) {
            return;
        }
        int rank = 1;
        for (Participant winner : winners) {
            this.giveReward(winner, rank);
            ++rank;
        }
    }

    private void giveReward(Participant participant, int rank) {
        Player player = Bukkit.getPlayer((UUID)participant.uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        List<String> commands = getConfig().getStringList("rewards.rank" + rank);
        for (String raw : commands) {
            String cmd = raw.replace("%player%", player.getName()).replace("%rank%", String.valueOf(rank)).replace("%points%", String.valueOf(participant.points));
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)cmd);
        }
    }

    private void handleEventDeath(Player player) {
        Participant participant = this.participants.get(player.getUniqueId());
        if (participant == null) {
            return;
        }
        participant.alive = true;
        participant.lastInsideCapture = false;
        this.broadcast(this.applyPlaceholders(this.msg("messages.death-respawn"), player, participant, 0));
        this.playSound(player, Sound.ENTITY_PLAYER_HURT, 0.7f, 1.2f);
        Location teamSpawn = this.getTeamSpawn(participant.team);
        if (teamSpawn != null) {
            this.deathRespawnTargets.put(player.getUniqueId(), teamSpawn);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (this.state != GameState.RUNNING) {
            return;
        }
        if (!this.participants.containsKey(event.getEntity().getUniqueId())) {
            return;
        }
        if (this.clearDeathDrops) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
        this.handleEventDeath(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location target = this.deathRespawnTargets.remove(event.getPlayer().getUniqueId());
        if (target != null) {
            event.setRespawnLocation(target);
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Player player = event.getPlayer();
            Participant participant = this.participants.get(player.getUniqueId());
            if (participant != null && this.state == GameState.RUNNING) {
                Location teamSpawn;
                participant.alive = true;
                participant.lastInsideCapture = false;
                player.setGameMode(GameMode.SURVIVAL);
                if (this.inventorySystemEnabled && this.clearInventoryOnJoin) {
                    player.getInventory().clear();
                    player.getInventory().setArmorContents(new ItemStack[4]);
                    player.getInventory().setItemInOffHand(null);
                    this.giveEventKit(player);
                }
                if ((teamSpawn = this.getTeamSpawn(participant.team)) != null) {
                    player.teleport(teamSpawn);
                }
                try {
                    player.setHealth(Math.max(1.0, player.getMaxHealth()));
                }
                catch (Exception ignored) {
                    try {
                        player.setHealth(20.0);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                player.setFoodLevel(20);
                player.setSaturation(8.0f);
                player.setFireTicks(0);
                player.setFallDistance(0.0f);
                player.sendTitle(this.color("&a&lRESPAWN"), this.color("&7You have returned to team spawn to continue playing."), 5, 35, 10);
                this.updateScoreboard(player);
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Participant p = this.participants.get(event.getPlayer().getUniqueId());
        if (p == null) {
            return;
        }
        if (this.state == GameState.RUNNING) {
            p.alive = false;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Participant participant = this.participants.get(player.getUniqueId());
        if (participant != null) {
            this.updateScoreboard(player);
            if (!participant.alive && this.state == GameState.RUNNING) {
                participant.alive = true;
                Location teamSpawn = this.getTeamSpawn(participant.team);
                if (teamSpawn != null) {
                    player.teleport(teamSpawn);
                }
                player.setGameMode(GameMode.SURVIVAL);
                this.giveEventKit(player);
            }
            return;
        }
        if (this.restoreBackupOnJoin && this.hasBackup(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (!this.participants.containsKey(player.getUniqueId()) && this.hasBackup(player.getUniqueId())) {
                    this.restoreInventoryBackup(player, true, false);
                    player.sendMessage(this.prefix + this.color("&aItem backup from previous CTF event has been automatically restored."));
                }
            }, 20L);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!this.freezeBeforeStart) {
            return;
        }
        if (this.state == GameState.RUNNING || this.state == GameState.ENDING) {
            return;
        }
        Participant participant = this.participants.get(event.getPlayer().getUniqueId());
        if (participant == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            if (this.participants.containsKey(event.getPlayer().getUniqueId()) && this.state == GameState.RUNNING) {
                org.bukkit.block.Block b = event.getClickedBlock();
                if (b != null) {
                    String type = b.getType().name();
                    if (type.endsWith("_STAIRS") || type.endsWith("_SLAB")) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (this.preventItemDrop && this.participants.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(this.prefix + this.color("&cEvent items cannot be dropped."));
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Player player;
        LivingEntity entity = event.getEntity();
        if (this.preventItemPickup && entity instanceof Player && this.participants.containsKey((player = (Player)entity).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player;
        if (!this.preventInventoryClick) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player && this.participants.containsKey((player = (Player)humanEntity).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.preventBlockBreak && this.participants.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (this.preventBlockPlace && this.participants.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void teleportAllToTeams() {
        for (Participant p : this.participants.values()) {
            Player player = Bukkit.getPlayer((UUID)p.uuid);
            if (player == null || !player.isOnline()) continue;
            Location spawn = this.getTeamSpawn(p.team);
            if (spawn != null) {
                player.teleport(spawn);
            }
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private int getAliveCount() {
        int count = 0;
        for (Participant p : this.participants.values()) {
            Player player;
            if (!p.alive || (player = Bukkit.getPlayer((UUID)p.uuid)) == null || !player.isOnline()) continue;
            ++count;
        }
        return count;
    }

    private List<Player> getOnlineParticipants() {
        ArrayList<Player> players = new ArrayList<Player>();
        for (Participant p : this.participants.values()) {
            Player player = Bukkit.getPlayer((UUID)p.uuid);
            if (player == null || !player.isOnline()) continue;
            players.add(player);
        }
        return players;
    }

    private boolean isInsideArena(Location loc) {
        double dz;
        World world = Bukkit.getWorld((String)getConfig().getString("arena.world", "world"));
        if (world == null || loc.getWorld() == null || !loc.getWorld().equals((Object)world)) {
            return false;
        }
        double cx = getConfig().getDouble("arena.center-x", 4283.0);
        double cz = getConfig().getDouble("arena.center-z", 2334.0);
        double radius = getConfig().getDouble("arena.radius", 120.0);
        double dx = loc.getX() - cx;
        return dx * dx + (dz = loc.getZ() - cz) * dz <= radius * radius;
    }

    private boolean isInsideCapture(Location loc) {
        return this.isInsideCapturePoint(loc, this.getActiveCapture());
    }

    private boolean isInsideCapturePoint(Location loc, CapturePoint point) {
        if (point == null || loc == null || loc.getWorld() == null) {
            return false;
        }
        World world = Bukkit.getWorld((String)point.world);
        if (world == null || !loc.getWorld().equals((Object)world)) {
            return false;
        }
        double y = loc.getY();
        boolean ignoreY = getConfig().getBoolean("capture.ignore-y", false);
        if (!ignoreY && (y < point.minY || y > point.maxY)) {
            return false;
        }
        double dx = loc.getX() - point.x;
        double dz = loc.getZ() - point.z;
        String shape = point.shape.toUpperCase(Locale.ROOT);
        if (shape.equals("BOX") || shape.equals("RECTANGLE") || shape.equals("SQUARE")) {
            return Math.abs(dx) <= point.radiusX && Math.abs(dz) <= point.radiusZ;
        }
        return dx * dx + dz * dz <= point.radius * point.radius;
    }

    private String getActiveCaptureName() {
        CapturePoint point = this.getActiveCapture();
        return point == null ? "-" : point.name;
    }

    private String getActiveCaptureCoordText() {
        CapturePoint point = this.getActiveCapture();
        if (point == null) {
            return "-";
        }
        double y = (point.minY + point.maxY) / 2.0;
        return "X " + this.round(point.x) + " Y " + this.round(y) + " Z " + this.round(point.z);
    }

    private void listCapturePoints(CommandSender sender) {
        sender.sendMessage(this.color("&8&m------------------------------"));
        sender.sendMessage(this.color("&d&lCTF Capture Points"));
        int i = 0;
        for (CapturePoint point : this.capturePoints) {
            String active = i == this.activeCaptureIndex ? " &a(ACTIVE)" : "";
            sender.sendMessage(this.color("&7- &e" + point.name + active + " &8| &f" + point.world + " X " + this.round(point.x) + " Z " + this.round(point.z) + " R " + this.round(point.radius) + " Y " + this.round(point.minY) + "-" + this.round(point.maxY)));
            ++i;
        }
        sender.sendMessage(this.color("&8&m------------------------------"));
    }

    private String captureDebug(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return this.color("&cInvalid location.");
        }
        CapturePoint point = this.getActiveCapture();
        if (point == null) {
            return this.color("&cCapture point not found.");
        }
        double dx = loc.getX() - point.x;
        double dz = loc.getZ() - point.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        boolean inside = this.isInsideCapture(loc);
        return this.color("&7World: &e" + loc.getWorld().getName() + " &8(target " + point.world + ")\n&7Active point: &d" + point.name + "\n&7Shape: &e" + point.shape + "\n&7Your position: &eX " + this.round(loc.getX()) + " Y " + this.round(loc.getY()) + " Z " + this.round(loc.getZ()) + "\n&7Center: &eX " + this.round(point.x) + " Z " + this.round(point.z) + "\n&7Horizontal distance: &e" + this.round(distance) + " &8(radius " + this.round(point.radius) + ")\n&7Valid Y: &e" + this.round(point.minY) + " - " + this.round(point.maxY) + "\n&7Next rotate: &e" + Math.max(0, this.captureNextRotateSeconds) + "s\n&7Inside capture: " + (inside ? "&aYES" : "&cNO"));
    }

    private Location getTeamSpawn(String team) {
        if (team.equalsIgnoreCase("Side 1")) {
            return this.getConfiguredLocation("spawns.side1");
        }
        return this.getConfiguredLocation("spawns.side2");
    }

    private Location getConfiguredLocation(String path) {
        String worldName = getConfig().getString(path + ".world", "world");
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            return null;
        }
        double x = getConfig().getDouble(path + ".x");
        double y = getConfig().getDouble(path + ".y");
        double z = getConfig().getDouble(path + ".z");
        float yaw = (float)getConfig().getDouble(path + ".yaw", 0.0);
        float pitch = (float)getConfig().getDouble(path + ".pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void setLocation(CommandSender sender, String path) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "This command must be run by a player.");
            return;
        }
        Player player = (Player)sender;
        Location loc = player.getLocation();
        getConfig().set(path + ".world", (Object)loc.getWorld().getName());
        getConfig().set(path + ".x", (Object)this.round(loc.getX()));
        getConfig().set(path + ".y", (Object)this.round(loc.getY()));
        getConfig().set(path + ".z", (Object)this.round(loc.getZ()));
        getConfig().set(path + ".yaw", (Object)this.round(loc.getYaw()));
        getConfig().set(path + ".pitch", (Object)this.round(loc.getPitch()));
        saveConfig();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Location " + path + " successfully set.");
    }

    private void setArena(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "This command must be run by a player.");
            return;
        }
        Player player = (Player)sender;
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Usage: /rkctf setarena <radius>");
            return;
        }
        double radius = this.parseDouble(args[1], 120.0);
        Location loc = player.getLocation();
        getConfig().set("arena.world", (Object)loc.getWorld().getName());
        getConfig().set("arena.center-x", (Object)this.round(loc.getX()));
        getConfig().set("arena.center-y", (Object)this.round(loc.getY()));
        getConfig().set("arena.center-z", (Object)this.round(loc.getZ()));
        getConfig().set("arena.radius", (Object)radius);
        getConfig().set("arena.enabled", (Object)true);
        saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Arena center set with radius " + radius + ". Arena boundary is now active.");
    }

    private void setCapture(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "This command must be run by a player.");
            return;
        }
        Player player = (Player)sender;
        if (args.length < 4) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Usage: /rkctf setcapture <radius> <minY> <maxY>");
            return;
        }
        double radius = this.parseDouble(args[1], 13.0);
        double minY = this.parseDouble(args[2], player.getLocation().getY() - 5.0);
        double maxY = this.parseDouble(args[3], player.getLocation().getY() + 5.0);
        Location loc = player.getLocation();
        getConfig().set("capture.world", (Object)loc.getWorld().getName());
        getConfig().set("capture.shape", (Object)"CIRCLE");
        getConfig().set("capture.center-x", (Object)this.round(loc.getX()));
        getConfig().set("capture.center-z", (Object)this.round(loc.getZ()));
        getConfig().set("capture.radius", (Object)radius);
        getConfig().set("capture.min-y", (Object)Math.min(minY, maxY));
        getConfig().set("capture.max-y", (Object)Math.max(minY, maxY));
        saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "CIRCLE capture zone set. Radius " + radius + ", Y " + minY + " to " + maxY + ".");
    }

    private void setCaptureBox(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "This command must be run by a player.");
            return;
        }
        Player player = (Player)sender;
        if (args.length < 5) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Usage: /rkctf setcapturebox <radiusX> <radiusZ> <minY> <maxY>");
            return;
        }
        double radiusX = this.parseDouble(args[1], 13.0);
        double radiusZ = this.parseDouble(args[2], 13.0);
        double minY = this.parseDouble(args[3], player.getLocation().getY() - 5.0);
        double maxY = this.parseDouble(args[4], player.getLocation().getY() + 5.0);
        Location loc = player.getLocation();
        getConfig().set("capture.world", (Object)loc.getWorld().getName());
        getConfig().set("capture.shape", (Object)"BOX");
        getConfig().set("capture.center-x", (Object)this.round(loc.getX()));
        getConfig().set("capture.center-z", (Object)this.round(loc.getZ()));
        getConfig().set("capture.radius-x", (Object)Math.max(1.0, radiusX));
        getConfig().set("capture.radius-z", (Object)Math.max(1.0, radiusZ));
        getConfig().set("capture.radius", (Object)Math.max(radiusX, radiusZ));
        getConfig().set("capture.min-y", (Object)Math.min(minY, maxY));
        getConfig().set("capture.max-y", (Object)Math.max(minY, maxY));
        saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Primary BOX capture set. RadiusX " + radiusX + ", RadiusZ " + radiusZ + ", Y " + minY + " to " + maxY + ".");
    }

    private void addCaptureBox(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player)sender;
        if (args.length < 5) return;
        double radiusX = this.parseDouble(args[1], 13.0);
        double radiusZ = this.parseDouble(args[2], 13.0);
        double minY = this.parseDouble(args[3], player.getLocation().getY() - 5.0);
        double maxY = this.parseDouble(args[4], player.getLocation().getY() + 5.0);
        Location loc = player.getLocation();
        
        int nextId = 1;
        if (getConfig().isConfigurationSection("capture.points")) {
            nextId = getConfig().getConfigurationSection("capture.points").getKeys(false).size() + 1;
        }
        
        String path = "capture.points.point_" + nextId;
        getConfig().set(path + ".world", loc.getWorld().getName());
        getConfig().set(path + ".shape", "BOX");
        getConfig().set(path + ".x", this.round(loc.getX()));
        getConfig().set(path + ".z", this.round(loc.getZ()));
        getConfig().set(path + ".radius-x", Math.max(1.0, radiusX));
        getConfig().set(path + ".radius-z", Math.max(1.0, radiusZ));
        getConfig().set(path + ".radius", Math.max(radiusX, radiusZ));
        getConfig().set(path + ".min-y", Math.min(minY, maxY));
        getConfig().set(path + ".max-y", Math.max(minY, maxY));
        getConfig().set(path + ".name", "Point " + nextId);
        getConfig().set(path + ".enabled", true);
        
        saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Added new Capture Point " + nextId + ".");
    }

    private void clearCaptures(CommandSender sender) {
        getConfig().set("capture.points", null);
        saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Cleared all extra Capture Points.");
    }

    private void checkPosition(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.prefix + "This command must be run by a player.");
            return;
        }
        Player player = (Player)sender;
        sender.sendMessage(this.color("&8&m------------------------------"));
        sender.sendMessage(this.color("&d&lCTF Capture Check"));
        for (String line : this.captureDebug(player.getLocation()).split("\\n")) {
            sender.sendMessage(line);
        }
        Participant participant = this.participants.get(player.getUniqueId());
        if (participant != null) {
            sender.sendMessage(this.color("&7Your points: &a" + participant.points));
            sender.sendMessage(this.color("&7State: &e" + String.valueOf((Object)this.state)));
            if (this.state != GameState.RUNNING) {
                sender.sendMessage(this.color("&cPoints only increase during RUNNING state. Use /rkctf forcestart to test."));
            }
        } else {
            sender.sendMessage(this.color("&7You have not joined the CTF event."));
        }
        sender.sendMessage(this.color("&8&m------------------------------"));
    }

    private void setDuration(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Usage: /rkctf setduration <seconds>");
            return;
        }
        int seconds = (int)this.parseDouble(args[1], 300.0);
        seconds = Math.max(10, seconds);
        getConfig().set("settings.duration-seconds", (Object)seconds);
        saveConfig();
        this.loadSettings();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Event duration set to " + seconds + " seconds.");
    }

    private void resetEvent(CommandSender sender) {
        if (this.state != GameState.IDLE) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Stop the event before resetting.");
            return;
        }
        this.participants.clear();
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Event participants have been reset.");
    }

    private void restoreItemsCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Usage: /rkctf restoreitems <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Player must be online for manual restore.");
            return;
        }
        if (!this.hasBackup(target.getUniqueId())) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "That player''s item backup was not found.");
            return;
        }
        this.restoreInventoryBackup(target, true, false);
        sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Item backup " + target.getName() + " successfully restored.");
    }

    private void backupStatusCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Usage: /rkctf backupstatus <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target != null) {
            sender.sendMessage(this.prefix + (this.hasBackup(target.getUniqueId()) ? String.valueOf(ChatColor.GREEN) + "Backup exists for " + target.getName() : String.valueOf(ChatColor.YELLOW) + "No backup exists for " + target.getName()));
        } else {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Player is offline. Check backup files manually if needed.");
        }
    }

    private void clearBackupCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Usage: /rkctf clearbackup <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.RED) + "Player must be online for safe backup clear.");
            return;
        }
        File file = this.getBackupFile(target.getUniqueId());
        if (file.exists() && file.delete()) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.GREEN) + "Backup " + target.getName() + " has been deleted.");
        } else {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "Backup does not exist or failed to delete.");
        }
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double round(double value) {
        return (double)Math.round(value * 100.0) / 100.0;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(this.color("&8&m------------------------------"));
        sender.sendMessage(this.color("&d&lRumahKita CTF Status"));
        sender.sendMessage(this.color("&7State: &e" + String.valueOf((Object)this.state)));
        if (this.state != GameState.RUNNING) {
            sender.sendMessage(this.color("&cPoints only increase during RUNNING state. Use /rkctf forcestart to test."));
        }
        sender.sendMessage(this.color("&7Players: &e" + this.participants.size()));
        sender.sendMessage(this.color("&7Alive: &a" + this.getAliveCount()));
        sender.sendMessage(this.color("&7Time left: &e" + this.formatTime(this.timeLeft)));
        sender.sendMessage(this.color("&7Inventory backup: &e" + this.getBackupFolder().getAbsolutePath()));
        sender.sendMessage(this.color("&7Capture active: &d" + this.getActiveCaptureName() + " &8| &f" + this.getActiveCaptureCoordText()));
        sender.sendMessage(this.color("&7Rotasi: &e" + (String)(this.captureRotationEnabled ? this.captureRotateSeconds + "s" : "OFF") + " &8| &7Next: &e" + Math.max(0, this.captureNextRotateSeconds) + "s"));
        sender.sendMessage(this.color("&8&m------------------------------"));
    }

    private void sendParticipantList(CommandSender sender) {
        if (this.participants.isEmpty()) {
            sender.sendMessage(this.prefix + String.valueOf(ChatColor.YELLOW) + "No participants yet.");
            return;
        }
        sender.sendMessage(this.color("&dParticipant CTF:"));
        for (Participant p : this.participants.values()) {
            sender.sendMessage(this.color("&7- &e" + p.name + " &8| &f" + p.team + " &8| &a" + p.points + " point &8| " + (p.alive ? "&aAlive" : "&cEliminated")));
        }
    }

    private void updateAllScoreboards() {
        for (Player player : this.getOnlineParticipants()) {
            this.updateScoreboard(player);
        }
    }

    private void updateScoreboard(Player player) {
        if (!this.forceScoreboardEnabled) {
            return;
        }
        Participant participant = this.participants.get(player.getUniqueId());
        if (participant == null) {
            return;
        }
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
        if (board == null || board.equals(manager.getMainScoreboard())) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }
        
        org.bukkit.scoreboard.Objective objective = board.getObjective("rkctf");
        if (objective == null) {
            objective = board.registerNewObjective("rkctf", "dummy", this.color(getConfig().getString("scoreboard.title", "&d&lRumahKita CTF")));
            objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(this.color(getConfig().getString("scoreboard.title", "&d&lRumahKita CTF")));
        }
        
        ArrayList<String> lines = new ArrayList<String>();
        lines.add(this.color(getConfig().getString("scoreboard.line", "&8&m----------")));
        lines.add(this.color("&fStatus: &e" + String.valueOf((Object)this.state)));
        lines.add(this.color("&fTime: &c" + this.formatTime(this.timeLeft)));
        lines.add(this.color("&fTeam: &b" + participant.team));
        lines.add(this.color("&fYour points: &a" + participant.points));
        if (this.showCaptureStatusInScoreboard) {
            lines.add(this.color("&fCapture: &d" + this.getActiveCaptureName()));
            lines.add(this.color("&fZone: " + (this.isInsideCapture(player.getLocation()) ? "&aIN ZONE" : "&7outside")));
        }
        lines.add(this.color("&fAlive: &a" + this.getAliveCount() + "&7/&f" + this.participants.size()));
        lines.add(this.color("&7"));
        lines.add(this.color("&dTop 3 Point"));
        int rank = 1;
        for (Participant top : this.getTopParticipants()) {
            lines.add(this.color("&6#" + rank + " &e" + top.name + " &7- &a" + top.points));
            if (++rank <= 3) continue;
            break;
        }
        while (rank <= 3) {
            lines.add(this.color("&6#" + rank + " &7-"));
            ++rank;
        }
        lines.add(this.color("&8&m----------"));
        
        // Anti-flicker & Anti-lag Team based update
        for (int i = 0; i < lines.size(); i++) {
            String teamName = "rkctf_" + i;
            org.bukkit.scoreboard.Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            String entry = ChatColor.values()[i].toString() + ChatColor.RESET;
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
            
            String text = lines.get(i);
            team.setPrefix(text.length() <= 64 ? text : text.substring(0, 64));
            
            objective.getScore(entry).setScore(lines.size() - i);
        }
        
        // Clean up remaining scores if any
        for (int i = lines.size(); i < 15; i++) {
            String entry = ChatColor.values()[i].toString() + ChatColor.RESET;
            board.resetScores(entry);
        }
    }

    private List<Participant> getTopParticipants() {
        ArrayList<Participant> list = new ArrayList<Participant>(this.participants.values());
        list.sort(Comparator.comparingInt((Participant p) -> p.points).reversed().thenComparing((Participant p) -> p.name));
        return list;
    }

    private void restoreAllPlayers(boolean teleportExit) {
        for (Participant participant : this.participants.values()) {
            Player player = Bukkit.getPlayer((UUID)participant.uuid);
            if (player == null || !player.isOnline()) continue;
            this.restorePlayer(player, participant, teleportExit, this.restoreInventoryAfterEvent);
        }
    }

    private void restorePlayer(Player player, Participant participant, boolean teleportExit, boolean restoreInventory) {
        Location exit;
        if (this.restoreScoreboardAfterEvent && participant.previousScoreboard != null) {
            player.setScoreboard(participant.previousScoreboard);
        }
        if (restoreInventory && this.inventorySystemEnabled) {
            this.restoreInventoryBackup(player, true, false);
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        if (teleportExit && (exit = this.getConfiguredLocation("spawns.exit")) != null) {
            player.teleport(exit);
        }
    }

    private File getBackupFolder() {
        File folder = new File(plugin.getDataFolder(), "backups");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private File getBackupFile(UUID uuid) {
        return new File(this.getBackupFolder(), uuid.toString() + ".yml");
    }

    private boolean hasBackup(UUID uuid) {
        return this.getBackupFile(uuid).exists();
    }

    private void saveInventoryBackup(Player player, Location originalLocation) {
        File file = this.getBackupFile(player.getUniqueId());
        YamlConfiguration yaml = new YamlConfiguration();
        PlayerInventory inv = player.getInventory();
        yaml.set("player-name", (Object)player.getName());
        yaml.set("created", (Object)System.currentTimeMillis());
        yaml.set("gamemode", (Object)player.getGameMode().name());
        yaml.set("health", (Object)player.getHealth());
        yaml.set("food", (Object)player.getFoodLevel());
        yaml.set("saturation", (Object)Float.valueOf(player.getSaturation()));
        yaml.set("exp", (Object)Float.valueOf(player.getExp()));
        yaml.set("level", (Object)player.getLevel());
        yaml.set("total-exp", (Object)player.getTotalExperience());
        yaml.set("fire-ticks", (Object)player.getFireTicks());
        if (originalLocation != null && originalLocation.getWorld() != null) {
            yaml.set("location.world", (Object)originalLocation.getWorld().getName());
            yaml.set("location.x", (Object)originalLocation.getX());
            yaml.set("location.y", (Object)originalLocation.getY());
            yaml.set("location.z", (Object)originalLocation.getZ());
            yaml.set("location.yaw", (Object)Float.valueOf(originalLocation.getYaw()));
            yaml.set("location.pitch", (Object)Float.valueOf(originalLocation.getPitch()));
        }
        ItemStack[] storage = inv.getStorageContents();
        yaml.set("inventory.storage-size", (Object)storage.length);
        for (int i = 0; i < storage.length; ++i) {
            yaml.set("inventory.storage." + i, (Object)storage[i]);
        }
        ItemStack[] armor = inv.getArmorContents();
        yaml.set("inventory.armor-size", (Object)armor.length);
        for (int i = 0; i < armor.length; ++i) {
            yaml.set("inventory.armor." + i, (Object)armor[i]);
        }
        yaml.set("inventory.offhand", (Object)inv.getItemInOffHand());
        try {
            yaml.save(file);
        }
        catch (IOException e) {
            plugin.getLogger().warning("Failed to save inventory backup for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(this.prefix + this.color("&cFailed to backup inventory. For safety, it is not recommended to continue."));
        }
    }

    private boolean restoreInventoryBackup(Player player, boolean deleteAfterRestore, boolean teleportOriginal) {
        Location loc;
        File file = this.getBackupFile(player.getUniqueId());
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)file);
        PlayerInventory inv = player.getInventory();
        int storageSize = yaml.getInt("inventory.storage-size", inv.getStorageContents().length);
        ItemStack[] storage = new ItemStack[inv.getStorageContents().length];
        for (int i = 0; i < Math.min(storage.length, storageSize); ++i) {
            storage[i] = yaml.getItemStack("inventory.storage." + i);
        }
        inv.setStorageContents(storage);
        int armorSize = yaml.getInt("inventory.armor-size", 4);
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < Math.min(armor.length, armorSize); ++i) {
            armor[i] = yaml.getItemStack("inventory.armor." + i);
        }
        inv.setArmorContents(armor);
        inv.setItemInOffHand(yaml.getItemStack("inventory.offhand"));
        try {
            GameMode gm = GameMode.valueOf((String)yaml.getString("gamemode", "SURVIVAL"));
            player.setGameMode(gm);
        }
        catch (Exception ignored) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        try {
            double health = yaml.getDouble("health", 20.0);
            double max = Math.max(1.0, player.getMaxHealth());
            player.setHealth(Math.max(1.0, Math.min(max, health)));
        }
        catch (Exception health) {
            // empty catch block
        }
        player.setFoodLevel(Math.max(0, Math.min(20, yaml.getInt("food", 20))));
        player.setSaturation((float)yaml.getDouble("saturation", 5.0));
        player.setExp((float)yaml.getDouble("exp", 0.0));
        player.setLevel(yaml.getInt("level", 0));
        player.setTotalExperience(yaml.getInt("total-exp", 0));
        player.setFireTicks(yaml.getInt("fire-ticks", 0));
        player.updateInventory();
        if (teleportOriginal && (loc = this.readLocation(yaml, "location")) != null) {
            player.teleport(loc);
        }
        if (deleteAfterRestore && file.exists() && !file.delete()) {
            plugin.getLogger().warning("Failed to delete backup file: " + file.getName());
        }
        return true;
    }

    private Location readLocation(YamlConfiguration yaml, String path) {
        String worldName = yaml.getString(path + ".world", null);
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            return null;
        }
        double x = yaml.getDouble(path + ".x");
        double y = yaml.getDouble(path + ".y");
        double z = yaml.getDouble(path + ".z");
        float yaw = (float)yaml.getDouble(path + ".yaw", 0.0);
        float pitch = (float)yaml.getDouble(path + ".pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void broadcast(String message) {
        String finalMessage = this.prefix + this.color(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(finalMessage);
        }
        Bukkit.getConsoleSender().sendMessage(finalMessage);
    }

    private String msg(String path) {
        return this.color(getConfig().getString(path, ""));
    }

    private String applyPlaceholders(String message, Player player, Participant participant, int rank) {
        return this.color(message.replace("%player%", player.getName()).replace("%team%", participant.team).replace("%points%", String.valueOf(this.pointsPerSecond)).replace("%rank%", String.valueOf(rank)).replace("%score%", String.valueOf(participant.points)));
    }

    private String applyTime(String message, int seconds) {
        return this.color(message.replace("%time%", this.formatTime(seconds)).replace("%seconds%", String.valueOf(seconds)));
    }

    private String formatTime(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(text == null ? "" : text));
    }

    private void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)this.color(message)));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void playSound(Player player, Sound sound, float volume, float pitch) {
        if (!this.soundsEnabled) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> base = new ArrayList<String>();
            Collections.addAll(base, "join", "leave", "status", "help");
            if (sender.hasPermission("rumahkita.ctf.admin")) {
                Collections.addAll(base, "start", "forcestart", "stop", "reload", "list", "setside1", "setside2", "setexit", "setarena", "setcapture", "setcapturebox", "nextcapture", "listcaptures", "check", "setduration", "reset", "restoreitems", "backupstatus", "clearbackup");
            }
            return this.filter(base, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setarena")) {
            return this.filter(java.util.Arrays.asList("120", "100", "80"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setduration")) {
            return this.filter(java.util.Arrays.asList("300", "180", "600"), args[1]);
        }
        if (args[0].equalsIgnoreCase("setcapture")) {
            if (args.length == 2) {
                return this.filter(java.util.Arrays.asList("13", "15", "20"), args[1]);
            }
            if (args.length == 3) {
                return this.filter(java.util.Arrays.asList("120", "125", "130"), args[2]);
            }
            if (args.length == 4) {
                return this.filter(java.util.Arrays.asList("150", "145", "140"), args[3]);
            }
        }
        if (args[0].equalsIgnoreCase("setcapturebox")) {
            if (args.length == 2) {
                return this.filter(java.util.Arrays.asList("13", "20", "25"), args[1]);
            }
            if (args.length == 3) {
                return this.filter(java.util.Arrays.asList("13", "20", "25"), args[2]);
            }
            if (args.length == 4) {
                return this.filter(java.util.Arrays.asList("120", "125", "130"), args[3]);
            }
            if (args.length == 5) {
                return this.filter(java.util.Arrays.asList("150", "145", "140"), args[4]);
            }
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("restoreitems") || args[0].equalsIgnoreCase("backupstatus") || args[0].equalsIgnoreCase("clearbackup"))) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return this.filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        ArrayList<String> result = new ArrayList<String>();
        for (String option : options) {
            if (!option.toLowerCase(Locale.ROOT).startsWith(lower)) continue;
            result.add(option);
        }
        return result;
    }

    private String placeholderValue(Player player, String identifier) {
        String id = identifier == null ? "" : identifier.toLowerCase(Locale.ROOT);
        Participant participant = player == null ? null : this.participants.get(player.getUniqueId());
        switch (id) {
            case "active":
            case "in_event":
                if (participant != null && this.state != GameState.IDLE) {
                    return "true";
                }
                return "false";
            case "global_active":
            case "event_active":
                if (this.state != GameState.IDLE) {
                    return "true";
                }
                return "false";
            case "running":
                if (this.state == GameState.RUNNING) {
                    return "true";
                }
                return "false";
            case "state": return this.state.name();
            case "state_display": return this.getStateDisplay();
            case "time":
            case "time_left": return this.formatTime(Math.max(0, this.timeLeft));
            case "time_seconds": return String.valueOf(Math.max(0, this.timeLeft));
            case "countdown": return String.valueOf(Math.max(0, this.countdownLeft));
            case "players":
            case "participants": return String.valueOf(this.participants.size());
            case "alive": return String.valueOf(this.getAliveCount());
            case "score":
            case "points":
                if (participant == null) {
                    return "0";
                }
                return String.valueOf(participant.points);
            case "rank":
                if (participant == null) {
                    return "-";
                }
                return String.valueOf(this.getRank(participant));
            case "team":
                if (participant == null) {
                    return "-";
                }
                return participant.team;
            case "is_alive":
                if (participant != null && participant.alive) {
                    return "true";
                }
                return "false";
            case "capture_inside":
                if (player != null && participant != null && this.isInsideCapture(player.getLocation())) {
                    return "true";
                }
                return "false";
            case "capture_status":
                if (player != null && participant != null && this.isInsideCapture(player.getLocation())) {
                    return this.color("&aIN ZONE");
                }
                return this.color("&7outside");
            case "active_capture":
            case "capture_name": return this.getActiveCaptureName();
            case "active_capture_coord":
            case "capture_coord": return this.getActiveCaptureCoordText();
            case "next_capture_seconds":
            case "next_rotation": return String.valueOf(Math.max(0, this.captureNextRotateSeconds));
            case "top_1": return this.getTopLine(1);
            case "top_2": return this.getTopLine(2);
            case "top_3": return this.getTopLine(3);
            case "top_1_name": return this.getTopName(1);
            case "top_2_name": return this.getTopName(2);
            case "top_3_name": return this.getTopName(3);
            case "top_1_points": return this.getTopPoints(1);
            case "top_2_points": return this.getTopPoints(2);
            case "top_3_points": return this.getTopPoints(3);
            default: return "";
        }
    }

    private String getStateDisplay() {
        switch (this.state.ordinal()) {
            case 0: return getConfig().getString("placeholder.state.idle", "Waiting");
            case 1: return getConfig().getString("placeholder.state.countdown", "Countdown");
            case 2: return getConfig().getString("placeholder.state.running", "Running");
            case 3: return getConfig().getString("placeholder.state.ending", "Ended");
            default: return "Waiting";
        }
    }

    private int getRank(Participant participant) {
        List<Participant> top = this.getTopParticipants();
        for (int i = 0; i < top.size(); ++i) {
            if (!top.get((int)i).uuid.equals(participant.uuid)) continue;
            return i + 1;
        }
        return 0;
    }

    private String getTopLine(int rank) {
        List<Participant> top = this.getTopParticipants();
        if (rank < 1 || rank > top.size()) {
            return getConfig().getString("placeholder.no-top", "-");
        }
        Participant p = top.get(rank - 1);
        return getConfig().getString("placeholder.top-format", "%player% - %points%").replace("%rank%", String.valueOf(rank)).replace("%player%", p.name).replace("%points%", String.valueOf(p.points));
    }

    private String getTopName(int rank) {
        List<Participant> top = this.getTopParticipants();
        if (rank < 1 || rank > top.size()) {
            return "-";
        }
        return top.get((int)(rank - 1)).name;
    }

    private String getTopPoints(int rank) {
        List<Participant> top = this.getTopParticipants();
        if (rank < 1 || rank > top.size()) {
            return "0";
        }
        return String.valueOf(top.get((int)(rank - 1)).points);
    }

    private static enum GameState {
        IDLE,
        COUNTDOWN,
        RUNNING,
        ENDING;

    }

    private static final class CtfPlaceholderExpansion
    extends PlaceholderExpansion {
        private final RumahKitaCaptureFlag plugin;

        private CtfPlaceholderExpansion(RumahKitaCaptureFlag plugin) {
            this.plugin = plugin;
        }

        public String getIdentifier() {
            return "rumahkitactf";
        }

        public String getAuthor() {
            return "HansM x ChatGPT";
        }

        public String getVersion() {
            return "1.5.0";
        }

        public boolean persist() {
            return true;
        }

        public String onPlaceholderRequest(Player player, String identifier) {
            return this.plugin.placeholderValue(player, identifier);
        }
    }

    private static final class CapturePoint {
        private final String id;
        private final String name;
        private final String world;
        private final String shape;
        private final double x;
        private final double z;
        private final double radius;
        private final double radiusX;
        private final double radiusZ;
        private final double minY;
        private final double maxY;

        private CapturePoint(String id, String name, String world, String shape, double x, double z, double radius, double radiusX, double radiusZ, double minY, double maxY) {
            this.id = id;
            this.name = name;
            this.world = world;
            this.shape = shape == null ? "CIRCLE" : shape.toUpperCase(Locale.ROOT);
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.radiusX = radiusX;
            this.radiusZ = radiusZ;
            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);
        }
    }

    private static final class Participant {
        private final UUID uuid;
        private final String name;
        private String team;
        private int points;
        private boolean alive;
        private Location originalLocation;
        private Scoreboard previousScoreboard;
        private boolean prepared;
        private boolean lastInsideCapture;

        private Participant(Player player, String team) {
            this.uuid = player.getUniqueId();
            this.name = player.getName();
            this.team = team;
            this.points = 0;
            this.alive = true;
            this.originalLocation = player.getLocation().clone();
            this.previousScoreboard = player.getScoreboard();
            this.prepared = false;
            this.lastInsideCapture = false;
        }
    }
}

