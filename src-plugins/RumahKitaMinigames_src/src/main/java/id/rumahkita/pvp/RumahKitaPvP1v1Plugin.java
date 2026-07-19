package id.rumahkita.pvp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import id.rumahkita.minigames.RumahKitaMinigamesPlugin;

public final class RumahKitaPvP1v1Plugin implements Listener, TabExecutor {
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    public RumahKitaPvP1v1Plugin(org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private org.bukkit.configuration.file.FileConfiguration customConfig;
    private java.io.File customConfigFile;

    public void reloadConfig() {
        if (customConfigFile == null) {
            customConfigFile = new java.io.File(plugin.getDataFolder(), "RumahKitaPvP1v1_config.yml");
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

    private final Map<UUID, Duel> activeDuels = new HashMap<UUID, Duel>();
    private final Map<UUID, Invite> invitesByTarget = new HashMap<UUID, Invite>();
    private final Queue<UUID> quickQueue = new ArrayDeque<UUID>();
    private int cleanupTask = -1;

    private id.rumahkita.pvp.view.PvPAdminUI adminUI;
    private id.rumahkita.pvp.view.PvPKitManagerUI kitUI;

    public void onEnable() {
        // plugin.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        if (plugin.getCommand("pvp") != null) {
            plugin.getCommand("pvp").setExecutor(this);
            plugin.getCommand("pvp").setTabCompleter(this);
        }
        this.cleanupTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, this::cleanup, 20L, 20L);
        this.adminUI = new id.rumahkita.pvp.view.PvPAdminUI((RumahKitaMinigamesPlugin) this.plugin, this);
        this.kitUI = new id.rumahkita.pvp.view.PvPKitManagerUI((RumahKitaMinigamesPlugin) this.plugin, this);
        plugin.getLogger().info("RumahKitaPvP1v1 v1.2.0 enabled with Custom Kit System.");
    }

    public void onDisable() {
        if (this.cleanupTask != -1) {
            Bukkit.getScheduler().cancelTask(this.cleanupTask);
        }
        HashSet<Duel> duels = new HashSet<Duel>(this.activeDuels.values());
        for (Duel duel : duels) {
            this.forceEnd(duel, null, "Server reload/restart");
        }
        this.quickQueue.clear();
        this.invitesByTarget.clear();
    }

    public id.rumahkita.pvp.view.PvPKitManagerUI getKitUI() {
        return kitUI;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        int expire = Math.max(5, getConfig().getInt("queue.invite-expire-seconds", 60));
        this.invitesByTarget.entrySet().removeIf(entry -> now - entry.getValue().createdAt > (long)expire * 1000L);
        this.quickQueue.removeIf(uuid -> Bukkit.getPlayer(uuid) == null || this.activeDuels.containsKey(uuid));
        int maxDuration = Math.max(30, getConfig().getInt("match.max-duration-seconds", 300));
        boolean timerEnabled = getConfig().getBoolean("match.timer-enabled", false);
        HashSet<Duel> duels = new HashSet<Duel>(this.activeDuels.values());
        for (Duel duel : duels) {
            if (!timerEnabled || now - duel.startedAt <= (long)maxDuration * 1000L || duel.ending) continue;
            Player p1 = Bukkit.getPlayer(duel.p1);
            Player p2 = Bukkit.getPlayer(duel.p2);
            this.broadcastToDuel(duel, this.cc(this.pref() + "&eDuel time is over. It is a tie."));
            this.endDuel(duel, null, null, false);
            if (p1 != null) {
                this.msg(p1, this.pref() + "&eDuel ended because time ran out.");
            }
            if (p2 == null) continue;
            this.msg(p2, this.pref() + "&eDuel ended because time ran out.");
        }
    }

    private void invite(Player inviter, Player target, String kit) {
        if (!this.canUse(inviter)) {
            return;
        }
        if (inviter.equals(target)) {
            this.msg(inviter, this.pref() + "&cYou cannot invite yourself.");
            return;
        }
        if (this.activeDuels.containsKey(inviter.getUniqueId())) {
            this.msg(inviter, this.pref() + getConfig().getString("messages.already-in-duel"));
            return;
        }
        if (this.activeDuels.containsKey(target.getUniqueId())) {
            this.msg(inviter, this.pref() + getConfig().getString("messages.target-busy"));
            return;
        }
        if (this.isArenaBusy()) {
            this.msg(inviter, this.pref() + getConfig().getString("messages.arena-busy"));
            return;
        }
        
        String finalKit = (kit == null || kit.isEmpty()) ? "NOKIT" : kit.toUpperCase();
        boolean valid = getConfig().contains("kits." + finalKit) || Arrays.asList("DIAMOND", "IRON", "UHC", "NODEBUFF", "NETHERITE", "CRYSTAL", "SUMO", "BOW", "AXE", "TRIDENT", "NOKIT").contains(finalKit);
        
        if (!valid) {
            this.msg(inviter, this.pref() + "&cInvalid kit.");
            return;
        }
        
        boolean disabled = false;
        if (getConfig().getConfigurationSection("disabled-kits") != null) {
            for (String key : getConfig().getConfigurationSection("disabled-kits").getKeys(false)) {
                if (key.equalsIgnoreCase(finalKit) && getConfig().getBoolean("disabled-kits." + key, false)) {
                    disabled = true;
                    break;
                }
            }
        }
        boolean deleted = false;
        if (getConfig().getConfigurationSection("deleted-default-kits") != null) {
            for (String key : getConfig().getConfigurationSection("deleted-default-kits").getKeys(false)) {
                if (key.equalsIgnoreCase(finalKit) && getConfig().getBoolean("deleted-default-kits." + key, false)) {
                    deleted = true;
                    break;
                }
            }
        }

        if (disabled || deleted) {
            this.msg(inviter, this.pref() + "&cKit " + finalKit + " is currently disabled or deleted by admin.");
            return;
        }

        this.invitesByTarget.put(target.getUniqueId(), new Invite(inviter.getUniqueId(), target.getUniqueId(), finalKit, System.currentTimeMillis()));
        this.msg(inviter, this.pref() + this.replace(getConfig().getString("messages.invite-sent"), "%target%", target.getName()) + " (Kit: &a" + finalKit + "&f)");
        this.msg(target, this.pref() + this.replace(getConfig().getString("messages.invite-received"), "%player%", inviter.getName()) + " (Kit: &a" + finalKit + "&f)");
    }

    private void accept(Player target, String inviterName) {
        if (!this.canUse(target)) {
            return;
        }
        Invite invite = this.invitesByTarget.get(target.getUniqueId());
        if (invite == null) {
            this.msg(target, this.pref() + "&cNo active PvP invite.");
            return;
        }
        Player inviter = Bukkit.getPlayer(invite.inviter);
        if (inviter == null) {
            this.invitesByTarget.remove(target.getUniqueId());
            this.msg(target, this.pref() + "&cThe inviting player is offline.");
            return;
        }
        if (inviterName != null && !inviterName.isEmpty() && !inviter.getName().equalsIgnoreCase(inviterName)) {
            this.msg(target, this.pref() + "&cYour active invite is not from that player.");
            return;
        }
        int expire = Math.max(5, getConfig().getInt("queue.invite-expire-seconds", 60));
        if (System.currentTimeMillis() - invite.createdAt > (long)expire * 1000L) {
            this.invitesByTarget.remove(target.getUniqueId());
            this.msg(target, this.pref() + getConfig().getString("messages.invite-expired"));
            return;
        }
        this.invitesByTarget.remove(target.getUniqueId());
        this.startDuel(inviter, target, invite.kit);
    }

    private void quickJoin(Player player) {
        if (!this.canUse(player)) {
            return;
        }
        if (!getConfig().getBoolean("queue.quickjoin-enabled", true)) {
            this.msg(player, this.pref() + "&cQuickjoin is currently disabled.");
            return;
        }
        if (this.activeDuels.containsKey(player.getUniqueId())) {
            this.msg(player, this.pref() + getConfig().getString("messages.already-in-duel"));
            return;
        }
        if (this.isArenaBusy()) {
            this.msg(player, this.pref() + getConfig().getString("messages.arena-busy"));
            return;
        }
        this.quickQueue.remove(player.getUniqueId());
        while (!this.quickQueue.isEmpty()) {
            UUID otherId = this.quickQueue.poll();
            Player other = Bukkit.getPlayer(otherId);
            if (other == null || other.equals(player) || this.activeDuels.containsKey(otherId)) continue;
            this.msg(player, this.pref() + getConfig().getString("messages.quickjoin-found"));
            this.msg(other, this.pref() + getConfig().getString("messages.quickjoin-found"));
            this.startDuel(other, player, "NOKIT");
            return;
        }
        this.quickQueue.add(player.getUniqueId());
        this.msg(player, this.pref() + getConfig().getString("messages.quickjoin-waiting"));
    }

    private boolean startDuel(Player p1, Player p2, String kit) {
        if (!getConfig().getBoolean("general.enabled", true)) {
            this.msg(p1, this.pref() + "&cPvP system is currently disabled.");
            return false;
        }
        if (this.isArenaBusy()) {
            this.msg(p1, this.pref() + getConfig().getString("messages.arena-busy"));
            this.msg(p2, this.pref() + getConfig().getString("messages.arena-busy"));
            return false;
        }
        if (this.activeDuels.containsKey(p1.getUniqueId()) || this.activeDuels.containsKey(p2.getUniqueId())) {
            this.msg(p1, this.pref() + getConfig().getString("messages.target-busy"));
            this.msg(p2, this.pref() + getConfig().getString("messages.target-busy"));
            return false;
        }
        Location s1 = this.readLocation("arena.spawn1");
        Location s2 = this.readLocation("arena.spawn2");
        if (s1 == null || s2 == null) {
            this.msg(p1, this.pref() + "&cArena spawn is invalid. Admin must check config.yml.");
            this.msg(p2, this.pref() + "&cArena spawn is invalid. Admin must check config.yml.");
            return false;
        }
        
        Duel duel = new Duel(p1.getUniqueId(), p2.getUniqueId(), p1.getLocation().clone(), p2.getLocation().clone(), kit);
        
        // Save inventories
        duel.p1Inv = p1.getInventory().getContents();
        duel.p1Armor = p1.getInventory().getArmorContents();
        duel.p2Inv = p2.getInventory().getContents();
        duel.p2Armor = p2.getInventory().getArmorContents();
        
        getConfig().set("backup." + p1.getUniqueId().toString() + ".inventory", java.util.Arrays.asList(duel.p1Inv));
        getConfig().set("backup." + p1.getUniqueId().toString() + ".armor", java.util.Arrays.asList(duel.p1Armor));
        getConfig().set("backup." + p1.getUniqueId().toString() + ".location", duel.p1Return);
        
        getConfig().set("backup." + p2.getUniqueId().toString() + ".inventory", java.util.Arrays.asList(duel.p2Inv));
        getConfig().set("backup." + p2.getUniqueId().toString() + ".armor", java.util.Arrays.asList(duel.p2Armor));
        getConfig().set("backup." + p2.getUniqueId().toString() + ".location", duel.p2Return);
        saveConfig();
        
        this.activeDuels.put(p1.getUniqueId(), duel);
        this.activeDuels.put(p2.getUniqueId(), duel);
        
        this.prepPlayer(p1, kit);
        this.prepPlayer(p2, kit);
        
        p1.teleport(s1);
        p2.teleport(s2);
        
        p1.setNoDamageTicks(60);
        p2.setNoDamageTicks(60);
        
        int countdown = Math.max(0, getConfig().getInt("match.countdown-seconds", 3));
        this.msg(p1, this.pref() + this.replace(getConfig().getString("messages.match-starting"), "%seconds%", String.valueOf(countdown)));
        this.msg(p2, this.pref() + this.replace(getConfig().getString("messages.match-starting"), "%seconds%", String.valueOf(countdown)));
        
        new org.bukkit.scheduler.BukkitRunnable() {
            int time = countdown;
            @Override
            public void run() {
                if (duel.ending || !activeDuels.containsKey(p1.getUniqueId())) {
                    this.cancel();
                    return;
                }
                if (time > 0) {
                    p1.sendTitle(cc("&c" + time), cc("&eOpponent: &f" + p2.getName()), 0, 25, 5);
                    p2.sendTitle(cc("&c" + time), cc("&eOpponent: &f" + p1.getName()), 0, 25, 5);
                    p1.playSound(p1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    p2.playSound(p2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    time--;
                } else {
                    duel.canDamage = true;
                    msg(p1, pref() + replace(getConfig().getString("messages.match-started"), "%opponent%", p2.getName()));
                    msg(p2, pref() + replace(getConfig().getString("messages.match-started"), "%opponent%", p1.getName()));
                    p1.sendTitle(cc("&aFIGHT!"), cc("&fDefeat &e" + p2.getName()), 5, 25, 5);
                    p2.sendTitle(cc("&aFIGHT!"), cc("&fDefeat &e" + p1.getName()), 5, 25, 5);
                    p1.playSound(p1.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                    p2.playSound(p2.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
        return true;
    }

    private void setContentsSafely(Player player, List<?> invList, List<?> armorList) {
        if (invList != null) {
            ItemStack[] contents = new ItemStack[player.getInventory().getContents().length];
            for (int i = 0; i < invList.size() && i < contents.length; i++) {
                Object obj = invList.get(i);
                if (obj instanceof ItemStack) contents[i] = (ItemStack) obj;
            }
            player.getInventory().setContents(contents);
        }
        if (armorList != null) {
            ItemStack[] armor = new ItemStack[4];
            for (int i = 0; i < armorList.size() && i < armor.length; i++) {
                Object obj = armorList.get(i);
                if (obj instanceof ItemStack) armor[i] = (ItemStack) obj;
            }
            player.getInventory().setArmorContents(armor);
        }
        player.updateInventory();
    }

    private void prepPlayer(Player player, String kit) {
        if (getConfig().getBoolean("match.heal-on-start", true)) {
            player.setHealth(Math.max(1.0, player.getMaxHealth()));
        }
        if (getConfig().getBoolean("match.clear-fire-on-start", true)) {
            player.setFireTicks(0);
        }
        int food = getConfig().getInt("match.food-level-on-start", 20);
        player.setFoodLevel(Math.max(0, Math.min(20, food)));
        player.setSaturation(20.0f);
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        
        if ("NOKIT".equals(kit)) {
            return; // Do not clear or set anything
        }
        
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        
        // Load custom kit if exists
        if (getConfig().contains("kits." + kit)) {
            List<?> contentList = getConfig().getList("kits." + kit + ".contents");
            List<?> armorList = getConfig().getList("kits." + kit + ".armor");
            setContentsSafely(player, contentList, armorList);
            return;
        }
        
        // Give hardcoded kit fallback
        if (kit.equals("NETHERITE")) {
            player.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            player.getInventory().setItem(0, new ItemStack(Material.NETHERITE_SWORD));
            player.getInventory().setItem(1, new ItemStack(Material.GOLDEN_APPLE, 16));
            player.getInventory().setItem(2, new ItemStack(Material.COOKED_BEEF, 64));
            player.getInventory().setItem(3, new ItemStack(Material.SHIELD));
        } else if (kit.equals("CRYSTAL")) {
            ItemStack helm = new ItemStack(Material.NETHERITE_HELMET);
            helm.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            helm.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
            player.getInventory().setHelmet(helm);
            
            ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
            chest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            chest.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
            player.getInventory().setChestplate(chest);
            
            ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
            legs.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            legs.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
            player.getInventory().setLeggings(legs);
            
            ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
            boots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            boots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
            player.getInventory().setBoots(boots);
            
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 5);
            sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
            player.getInventory().setItem(0, sword);
            
            player.getInventory().setItem(1, new ItemStack(Material.END_CRYSTAL, 64));
            player.getInventory().setItem(2, new ItemStack(Material.OBSIDIAN, 64));
            player.getInventory().setItem(3, new ItemStack(Material.RESPAWN_ANCHOR, 64));
            player.getInventory().setItem(4, new ItemStack(Material.GLOWSTONE, 64));
            player.getInventory().setItem(5, new ItemStack(Material.COBWEB, 16));
            player.getInventory().setItem(6, new ItemStack(Material.TOTEM_OF_UNDYING));
            player.getInventory().setItem(7, new ItemStack(Material.TOTEM_OF_UNDYING));
            player.getInventory().setItem(8, new ItemStack(Material.GOLDEN_APPLE, 64));
            
            ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
            pickaxe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DIG_SPEED, 5);
            pickaxe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
            player.getInventory().setItem(9, pickaxe);
            
            player.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        } else if (kit.equals("SUMO")) {
            player.getInventory().setItem(0, new ItemStack(Material.STICK));
        } else if (kit.equals("DIAMOND")) {
            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
            player.getInventory().setItem(1, new ItemStack(Material.GOLDEN_APPLE, 16));
            player.getInventory().setItem(2, new ItemStack(Material.COOKED_BEEF, 64));
            player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        } else if (kit.equals("IRON")) {
            player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
            player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
            player.getInventory().setItem(1, new ItemStack(Material.GOLDEN_APPLE, 8));
            player.getInventory().setItem(2, new ItemStack(Material.COOKED_BEEF, 64));
            player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        } else if (kit.equals("UHC")) {
            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
            player.getInventory().setItem(1, new ItemStack(Material.BOW));
            player.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 16));
            player.getInventory().setItem(3, new ItemStack(Material.LAVA_BUCKET));
            player.getInventory().setItem(4, new ItemStack(Material.WATER_BUCKET));
            player.getInventory().setItem(5, new ItemStack(Material.WATER_BUCKET));
            player.getInventory().setItem(6, new ItemStack(Material.OAK_PLANKS, 64));
            player.getInventory().setItem(7, new ItemStack(Material.COBBLESTONE, 64));
            player.getInventory().setItem(8, new ItemStack(Material.ARROW, 64));
        } else if (kit.equals("BOW")) {
            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            ItemStack bow = new ItemStack(Material.BOW);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.ARROW_INFINITE, 1);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.ARROW_DAMAGE, 2);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.ARROW_KNOCKBACK, 1);
            player.getInventory().setItem(0, bow);
            player.getInventory().setItem(1, new ItemStack(Material.GOLDEN_APPLE, 16));
            ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
            sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.KNOCKBACK, 1);
            player.getInventory().setItem(2, sword);
            player.getInventory().setItem(8, new ItemStack(Material.ARROW));
        } else if (kit.equals("AXE")) {
            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            ItemStack axe = new ItemStack(Material.IRON_AXE);
            axe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 2);
            player.getInventory().setItem(0, axe);
            player.getInventory().setItem(1, new ItemStack(Material.CROSSBOW));
            player.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 8));
            player.getInventory().setItem(3, new ItemStack(Material.COOKED_BEEF, 64));
            player.getInventory().setItem(8, new ItemStack(Material.ARROW, 32));
            player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        } else if (kit.equals("TRIDENT")) {
            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            ItemStack trident = new ItemStack(Material.TRIDENT);
            trident.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LOYALTY, 3);
            player.getInventory().setItem(0, trident);
            player.getInventory().setItem(1, new ItemStack(Material.WATER_BUCKET));
            player.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 16));
        } else if (kit.equals("NODEBUFF")) {
            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
            player.getInventory().setItem(1, new ItemStack(Material.ENDER_PEARL, 16));
            player.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 16));
            
            ItemStack pot = new ItemStack(Material.SPLASH_POTION, 1);
            org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) pot.getItemMeta();
            pm.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HEAL, 1, 1), true);
            pot.setItemMeta(pm);
            for (int i = 3; i < 36; i++) {
                player.getInventory().setItem(i, pot.clone());
            }
        }
        
        // Add default enchantments for longer fights
        if (!kit.equals("SUMO")) {
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    if (!item.containsEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL)) {
                        item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 2);
                    }
                    if (!item.containsEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY)) {
                        item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
                    }
                }
            }
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    String name = item.getType().name();
                    if (name.endsWith("_SWORD") || name.endsWith("_AXE")) {
                        if (!item.containsEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ALL)) {
                            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 1);
                        }
                        if (!item.containsEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY)) {
                            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
                        }
                    }
                }
            }
        }
        
        player.updateInventory();
    }

    private void endDuel(Duel duel, Player winner, Player loser, boolean forfeit) {
        if (duel == null || duel.ending) {
            return;
        }
        duel.ending = true;
        duel.canDamage = false;
        
        Player p1 = Bukkit.getPlayer(duel.p1);
        Player p2 = Bukkit.getPlayer(duel.p2);
        
        // Heal immediately to prevent dying to fire/poison during the 2-second delay
        if (p1 != null) {
            p1.setHealth(Math.max(1.0, p1.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
            p1.setFireTicks(0);
        }
        if (p2 != null) {
            p2.setHealth(Math.max(1.0, p2.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
            p2.setFireTicks(0);
        }

        this.activeDuels.remove(duel.p1);
        this.activeDuels.remove(duel.p2);
        if (winner != null && loser != null) {
            this.msg(winner, this.pref() + this.replace(getConfig().getString("messages.match-win"), "%loser%", loser.getName()));
            this.msg(loser, this.pref() + this.replace(getConfig().getString("messages.match-lose"), "%winner%", winner.getName()));
            winner.sendTitle(this.cc("&aVICTORY!"), this.cc("&fAgainst &e" + loser.getName()), 5, 35, 10);
            loser.sendTitle(this.cc("&cDEFEAT"), this.cc("&fAgainst &e" + winner.getName()), 5, 35, 10);
        }
        if (getConfig().getBoolean("protection.remove-projectiles-on-end", true)) {
            this.removeNearbyArenaEntities();
        }
        
        // Rollback placed blocks
        for (int i = duel.placedBlocks.size() - 1; i >= 0; i--) {
            duel.placedBlocks.get(i).update(true, true); // applyPhysics = true to make water recede
        }
        duel.placedBlocks.clear();
        
        long delay = Math.max(0L, getConfig().getLong("arena.end-delay-ticks", 40L));
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.restorePlayer(p1, duel.p1Return, duel.p1Inv, duel.p1Armor);
            this.restorePlayer(p2, duel.p2Return, duel.p2Inv, duel.p2Armor);
        }, delay);
    }

    private void forceEnd(Duel duel, Player winner, String reason) {
        if (duel == null) {
            return;
        }
        Player p1 = Bukkit.getPlayer(duel.p1);
        Player p2 = Bukkit.getPlayer(duel.p2);
        if (p1 != null) {
            this.msg(p1, this.pref() + "&eDuel force stopped. " + reason);
        }
        if (p2 != null) {
            this.msg(p2, this.pref() + "&eDuel force stopped. " + reason);
        }
        this.endDuel(duel, null, null, false);
    }

    private void restorePlayer(Player player, Location returnLocation, ItemStack[] oldInv, ItemStack[] oldArmor) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.getInventory().clear();
        if (oldInv != null) player.getInventory().setContents(oldInv);
        if (oldArmor != null) player.getInventory().setArmorContents(oldArmor);
        player.updateInventory();

        if (getConfig().getBoolean("match.heal-on-end", true)) {
            player.setHealth(Math.max(1.0, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
        }
        if (getConfig().getBoolean("match.clear-fire-on-end", true)) {
            player.setFireTicks(0);
        }
        int food = getConfig().getInt("match.food-level-on-end", 20);
        player.setFoodLevel(Math.max(0, Math.min(20, food)));
        player.setSaturation(20.0f);
        player.setNoDamageTicks(60);
        if (getConfig().getBoolean("arena.return-to-original-location", true) && returnLocation != null && returnLocation.getWorld() != null) {
            player.teleport(returnLocation);
        }
        
        getConfig().set("backup." + player.getUniqueId().toString(), null);
        saveConfig();
    }

    private void removeNearbyArenaEntities() {
        World world = Bukkit.getWorld(getConfig().getString("arena.world", "world"));
        if (world == null) {
            return;
        }
        int minX = Math.min(getConfig().getInt("arena.pos1.x"), getConfig().getInt("arena.pos2.x")) - 8;
        int maxX = Math.max(getConfig().getInt("arena.pos1.x"), getConfig().getInt("arena.pos2.x")) + 8;
        int minZ = Math.min(getConfig().getInt("arena.pos1.z"), getConfig().getInt("arena.pos2.z")) - 8;
        int maxZ = Math.max(getConfig().getInt("arena.pos1.z"), getConfig().getInt("arena.pos2.z")) + 8;
        int minY = Math.min(getConfig().getInt("arena.pos1.y"), getConfig().getInt("arena.pos2.y")) - 8;
        int maxY = Math.max(getConfig().getInt("arena.pos1.y"), getConfig().getInt("arena.pos2.y")) + 8;
        
        org.bukkit.util.BoundingBox box = new org.bukkit.util.BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        for (Entity entity : world.getNearbyEntities(box)) {
            if (entity instanceof Player) continue;
            if (entity instanceof Projectile || 
                entity instanceof org.bukkit.entity.Item || 
                entity instanceof org.bukkit.entity.EnderCrystal ||
                entity instanceof org.bukkit.entity.ExperienceOrb ||
                entity instanceof org.bukkit.entity.AreaEffectCloud) {
                entity.remove();
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player)event.getEntity();
        Duel duel = this.activeDuels.get(victim.getUniqueId());
        if (duel == null) {
            return;
        }
        if (!duel.canDamage || duel.ending) {
            event.setCancelled(true);
            return;
        }
        if (!getConfig().getBoolean("match.prevent-real-death", true)) {
            return;
        }
        double finalHealth = victim.getHealth() - event.getFinalDamage();
        if (finalHealth > 0.0) {
            return;
        }
        if (victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING || 
            victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
            return;
        }
        event.setCancelled(true);
        Player killer = this.getAttacker(event);
        Player winner = null;
        if (killer != null && duel.isParticipant(killer.getUniqueId()) && !killer.getUniqueId().equals(victim.getUniqueId())) {
            winner = killer;
        } else {
            UUID otherId = duel.other(victim.getUniqueId());
            winner = Bukkit.getPlayer(otherId);
        }
        Player loser = victim;
        this.endDuel(duel, winner, loser, false);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player)event.getEntity();
        Player attacker = this.getAttacker(event);
        if (attacker == null) {
            return;
        }
        Duel victimDuel = this.activeDuels.get(victim.getUniqueId());
        Duel attackerDuel = this.activeDuels.get(attacker.getUniqueId());
        boolean onlyDuel = getConfig().getBoolean("protection.only-duel-participants-can-pvp-in-arena", true);
        if (victimDuel != null || attackerDuel != null) {
            if (victimDuel == null || attackerDuel == null || victimDuel != attackerDuel) {
                event.setCancelled(true);
                this.msg(attacker, this.pref() + getConfig().getString("messages.cannot-hit"));
                return;
            }
            if (!victimDuel.canDamage) {
                event.setCancelled(true);
            }
            return;
        }
        if (onlyDuel && (this.isInArena(victim.getLocation()) || this.isInArena(attacker.getLocation()))) {
            event.setCancelled(true);
            this.msg(attacker, this.pref() + getConfig().getString("messages.cannot-hit"));
        }
    }

    private Player getAttacker(EntityDamageEvent event) {
        ProjectileSource source;
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return null;
        }
        Entity damager = ((EntityDamageByEntityEvent)event).getDamager();
        if (damager instanceof Player) {
            return (Player)damager;
        }
        if (damager instanceof Projectile && (source = ((Projectile)damager).getShooter()) instanceof Player) {
            return (Player)source;
        }
        return null;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        if (getConfig().contains("backup." + uuid)) {
            List<?> invList = getConfig().getList("backup." + uuid + ".inventory");
            List<?> armorList = getConfig().getList("backup." + uuid + ".armor");
            Location loc = getConfig().getLocation("backup." + uuid + ".location");
            
            player.getInventory().clear();
            setContentsSafely(player, invList, armorList);
            if (loc != null) player.teleport(loc);
            
            getConfig().set("backup." + uuid, null);
            saveConfig();
            this.msg(player, this.pref() + "&aYour inventory has been restored after disconnecting during a PvP duel.");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        Duel duel = this.activeDuels.get(event.getPlayer().getUniqueId());
        if (duel != null && !duel.canDamage) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.getPlayer().teleport(event.getFrom());
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        this.quickQueue.remove(quitter.getUniqueId());
        this.invitesByTarget.remove(quitter.getUniqueId());
        Duel duel = this.activeDuels.get(quitter.getUniqueId());
        if (duel != null) {
            Player winner = Bukkit.getPlayer(duel.other(quitter.getUniqueId()));
            if (winner != null) {
                this.msg(winner, this.pref() + this.replace(getConfig().getString("messages.match-forfeit"), "%player%", quitter.getName()));
            }
            this.endDuel(duel, winner, quitter, true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        Duel duel = this.activeDuels.get(event.getPlayer().getUniqueId());
        if (duel != null && duel.canDamage) {
            boolean wasPlaced = false;
            for (org.bukkit.block.BlockState state : duel.placedBlocks) {
                if (state.getLocation().equals(event.getBlock().getLocation())) {
                    wasPlaced = true;
                    break;
                }
            }
            if (wasPlaced) {
                event.setCancelled(false);
            } else if (getConfig().getBoolean("protection.block-break-place-in-arena", true)) {
                event.setCancelled(true);
            }
        } else {
            if (!event.isCancelled() && getConfig().getBoolean("protection.block-break-place-in-arena", true) && this.isInArena(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("rumahkita.pvp.bypass")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        Duel duel = this.activeDuels.get(event.getPlayer().getUniqueId());
        if (duel != null && duel.canDamage) {
            event.setCancelled(false);
            duel.placedBlocks.add(event.getBlockReplacedState());
        } else {
            if (!event.isCancelled() && getConfig().getBoolean("protection.block-break-place-in-arena", true) && this.isInArena(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("rumahkita.pvp.bypass")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Duel duel = this.activeDuels.get(event.getPlayer().getUniqueId());
        if (duel != null && duel.canDamage) {
            event.setCancelled(false);
            duel.placedBlocks.add(event.getBlock().getState());
        } else {
            if (!event.isCancelled() && getConfig().getBoolean("protection.block-buckets-in-arena", true) && this.isInArena(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("rumahkita.pvp.bypass")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Duel duel = this.activeDuels.get(event.getPlayer().getUniqueId());
        if (duel != null && duel.canDamage) {
            boolean wasPlaced = false;
            for (org.bukkit.block.BlockState state : duel.placedBlocks) {
                if (state.getLocation().equals(event.getBlock().getLocation())) {
                    wasPlaced = true;
                    break;
                }
            }
            if (wasPlaced) {
                event.setCancelled(false);
            } else if (getConfig().getBoolean("protection.block-buckets-in-arena", true)) {
                event.setCancelled(true);
            }
        } else {
            if (!event.isCancelled() && getConfig().getBoolean("protection.block-buckets-in-arena", true) && this.isInArena(event.getBlock().getLocation()) && !event.getPlayer().hasPermission("rumahkita.pvp.bypass")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockForm(org.bukkit.event.block.BlockFormEvent event) {
        if (!this.isInArena(event.getBlock().getLocation())) return;
        if (this.activeDuels.isEmpty()) return;
        Duel duel = this.activeDuels.values().iterator().next();
        if (duel.canDamage) {
            duel.placedBlocks.add(event.getBlock().getState());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Duel duel = this.activeDuels.get(event.getPlayer().getUniqueId());
        if (duel != null && duel.canDamage) {
            event.setCancelled(false);
            event.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onProjectileLaunch(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            Duel duel = this.activeDuels.get(player.getUniqueId());
            if (duel != null && duel.canDamage) {
                event.setCancelled(false);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onMobSpawn(EntitySpawnEvent event) {
        if (!getConfig().getBoolean("protection.deny-mob-spawn-in-arena", true)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof Player || 
            entity instanceof Projectile || 
            entity instanceof org.bukkit.entity.Item ||
            entity instanceof org.bukkit.entity.ExperienceOrb ||
            entity instanceof org.bukkit.entity.EnderCrystal) {
            return;
        }
        if (this.isInArena(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!getConfig().getBoolean("protection.deny-explosions-in-arena", true)) {
            return;
        }
        if (this.isInArena(event.getLocation())) {
            event.setCancelled(true);
        } else {
            event.blockList().removeIf(block -> this.isInArena(block.getLocation()));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String root;
        if (!getConfig().getBoolean("commands.block-during-duel", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.activeDuels.containsKey(player.getUniqueId())) {
            return;
        }
        String raw = event.getMessage().toLowerCase(Locale.ROOT).trim();
        if (raw.startsWith("/")) {
            raw = raw.substring(1);
        }
        if ((root = raw.split(" ")[0]).equals("pvp") || root.equals("duel") || root.equals("rkduel") || root.equals("rkpvp")) {
            return;
        }
        List<String> blocked = getConfig().getStringList("commands.blocked-during-duel");
        for (String b : blocked) {
            if (!root.equalsIgnoreCase(b)) continue;
            event.setCancelled(true);
            this.msg(player, this.pref() + getConfig().getString("messages.command-blocked"));
            return;
        }
    }

    private boolean isInArena(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        String worldName = getConfig().getString("arena.world", "world");
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }
        int x1 = getConfig().getInt("arena.pos1.x");
        int y1 = getConfig().getInt("arena.pos1.y");
        int z1 = getConfig().getInt("arena.pos1.z");
        int x2 = getConfig().getInt("arena.pos2.x");
        int y2 = getConfig().getInt("arena.pos2.y");
        int z2 = getConfig().getInt("arena.pos2.z");
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        return location.getBlockX() >= minX && location.getBlockX() <= maxX && location.getBlockY() >= minY && location.getBlockY() <= maxY && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    private boolean isArenaBusy() {
        return !this.activeDuels.isEmpty();
    }

    private Location readLocation(String path) {
        String worldName = getConfig().getString("arena.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        ConfigurationSection s = getConfig().getConfigurationSection(path);
        if (s == null) {
            return null;
        }
        return new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"), (float)s.getDouble("yaw", 0.0), (float)s.getDouble("pitch", 0.0));
    }

    private void setLocation(String path, Location loc, boolean includeYawPitch) {
        getConfig().set(path + ".x", loc.getX());
        getConfig().set(path + ".y", loc.getY());
        getConfig().set(path + ".z", loc.getZ());
        if (includeYawPitch) {
            getConfig().set(path + ".yaw", Float.valueOf(loc.getYaw()));
            getConfig().set(path + ".pitch", Float.valueOf(loc.getPitch()));
        }
        getConfig().set("arena.world", loc.getWorld().getName());
        saveConfig();
    }

    private boolean canUse(Player player) {
        if (!player.hasPermission("rumahkita.pvp.use")) {
            this.msg(player, this.pref() + getConfig().getString("messages.no-permission"));
            return false;
        }
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.cc(this.pref() + getConfig().getString("messages.not-player")));
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            this.sendHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("admin")) {
            if (player.hasPermission("rumahkita.pvp.admin")) {
                if (this.adminUI != null) this.adminUI.open(player);
            } else {
                this.msg(player, this.pref() + getConfig().getString("messages.no-permission"));
            }
            return true;
        }
        switch (sub) {
            case "invite": {
                if (args.length < 2) {
                    this.msg(player, this.pref() + "&cUsage: /pvp invite <player> [kit]");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    this.msg(player, this.pref() + "&cPlayer is not online.");
                    return true;
                }
                String kit = args.length >= 3 ? args[2] : "NOKIT";
                this.invite(player, target, kit);
                return true;
            }
            case "accept": {
                this.accept(player, args.length >= 2 ? args[1] : null);
                return true;
            }
            case "deny": 
            case "cancel": {
                this.invitesByTarget.remove(player.getUniqueId());
                this.quickQueue.remove(player.getUniqueId());
                this.msg(player, this.pref() + "&ePvP invite/queue cancelled.");
                return true;
            }
            case "quickjoin": 
            case "quick": {
                this.quickJoin(player);
                return true;
            }
            case "leave": 
            case "forfeit": {
                Duel duel = this.activeDuels.get(player.getUniqueId());
                if (duel == null) {
                    this.msg(player, this.pref() + getConfig().getString("messages.not-in-duel"));
                    return true;
                }
                Player winner = Bukkit.getPlayer(duel.other(player.getUniqueId()));
                if (winner != null) {
                    this.msg(winner, this.pref() + this.replace(getConfig().getString("messages.match-forfeit"), "%player%", player.getName()));
                }
                this.endDuel(duel, winner, player, true);
                return true;
            }
            case "createkit": {
                if (!player.hasPermission("rumahkita.pvp.admin")) {
                    this.msg(player, this.pref() + getConfig().getString("messages.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    this.msg(player, this.pref() + "&cUsage: /pvp createkit <name>");
                    return true;
                }
                String kitName = args[1].toUpperCase();
                getConfig().set("kits." + kitName + ".contents", player.getInventory().getContents());
                getConfig().set("kits." + kitName + ".armor", player.getInventory().getArmorContents());
                saveConfig();
                this.msg(player, this.pref() + "&aKit '" + kitName + "' saved from your current inventory!");
                return true;
            }
            case "deletekit": {
                if (!player.hasPermission("rumahkita.pvp.admin")) {
                    this.msg(player, this.pref() + getConfig().getString("messages.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    this.msg(player, this.pref() + "&cUsage: /pvp deletekit <name>");
                    return true;
                }
                String kitName = args[1].toUpperCase();
                getConfig().set("kits." + kitName, null);
                if (Arrays.asList("NETHERITE", "CRYSTAL", "DIAMOND", "IRON", "UHC", "NODEBUFF", "SUMO", "BOW", "AXE", "TRIDENT", "NOKIT").contains(kitName)) {
                    getConfig().set("deleted-default-kits." + kitName, true);
                }
                saveConfig();
                this.msg(player, this.pref() + "&aKit '" + kitName + "' deleted.");
                return true;
            }
            case "status": {
                if (!player.hasPermission("rumahkita.pvp.admin")) {
                    this.msg(player, this.pref() + getConfig().getString("messages.no-permission"));
                    return true;
                }
                this.msg(player, this.pref() + "&eArena busy: &f" + this.isArenaBusy());
                this.msg(player, this.pref() + "&eQueue: &f" + this.quickQueue.size());
                this.msg(player, this.pref() + "&eRegion: &f" + getConfig().getString("arena.world") + " " + getConfig().getInt("arena.pos1.x") + "," + getConfig().getInt("arena.min-y") + "," + getConfig().getInt("arena.pos1.z") + " -> " + getConfig().getInt("arena.pos2.x") + "," + getConfig().getInt("arena.max-y") + "," + getConfig().getInt("arena.pos2.z"));
                return true;
            }
            case "reload": {
                if (!player.hasPermission("rumahkita.pvp.admin")) {
                    this.msg(player, this.pref() + getConfig().getString("messages.no-permission"));
                    return true;
                }
                reloadConfig();
                this.msg(player, this.pref() + "&aConfig PvP reloaded.");
                return true;
            }
            case "setspawn1": 
            case "setspawn2": 
            case "setpos1": 
            case "setpos2": {
                if (!player.hasPermission("rumahkita.pvp.admin")) {
                    this.msg(player, this.pref() + getConfig().getString("messages.no-permission"));
                    return true;
                }
                Location loc = player.getLocation();
                if (sub.startsWith("setspawn")) {
                    this.setLocation("arena." + sub.substring(3), loc, true);
                    this.msg(player, this.pref() + "&a" + sub + " saved.");
                } else {
                    String key = "arena." + sub.substring(3);
                    getConfig().set(key + ".x", loc.getBlockX());
                    getConfig().set(key + ".y", loc.getBlockY());
                    getConfig().set(key + ".z", loc.getBlockZ());
                    getConfig().set("arena.world", loc.getWorld().getName());
                    saveConfig();
                    this.msg(player, this.pref() + "&a" + sub + " saved. Do not forget to check min-y/max-y.");
                }
                return true;
            }
        }
        this.sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        this.msg(player, "&c&lRumahKita PvP 1v1");
        this.msg(player, "&f/pvp invite <player> [kit] &7- invite a player to duel");
        this.msg(player, "&f/pvp accept <player> &7- accept a duel");
        this.msg(player, "&f/pvp quickjoin &7- find an opponent automatically");
        this.msg(player, "&f/pvp leave &7- forfeit / leave duel");
        this.msg(player, "&f/pvp cancel &7- cancel queue/invite");
        if (player.hasPermission("rumahkita.pvp.admin")) {
            this.msg(player, "&eAdmin: &f/pvp createkit <name> &7- create custom kit from inventory");
            this.msg(player, "&eAdmin: &f/pvp deletekit <name> &7- delete custom kit");
            this.msg(player, "&eAdmin: &f/pvp setpos1, setpos2, setspawn1, setspawn2, status, reload");
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> base = new ArrayList<String>(Arrays.asList("invite", "accept", "deny", "cancel", "quickjoin", "leave"));
            if (sender.hasPermission("rumahkita.pvp.admin")) {
                base.addAll(Arrays.asList("createkit", "deletekit", "status", "reload", "setpos1", "setpos2", "setspawn1", "setspawn2"));
            }
            return this.filter(base, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("accept"))) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return this.filter(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("invite")) {
            List<String> kits = new ArrayList<>(Arrays.asList("NETHERITE", "CRYSTAL", "DIAMOND", "IRON", "UHC", "NODEBUFF", "SUMO", "BOW", "AXE", "TRIDENT", "NOKIT"));
            if (getConfig().getConfigurationSection("kits") != null) {
                for (String k : getConfig().getConfigurationSection("kits").getKeys(false)) {
                    if (!kits.contains(k.toUpperCase())) kits.add(k.toUpperCase());
                }
            }
            // Remove deleted or disabled kits
            kits.removeIf(k -> {
                boolean disabled = false;
                if (getConfig().getConfigurationSection("disabled-kits") != null) {
                    for (String key : getConfig().getConfigurationSection("disabled-kits").getKeys(false)) {
                        if (key.equalsIgnoreCase(k) && getConfig().getBoolean("disabled-kits." + key, false)) {
                            disabled = true;
                            break;
                        }
                    }
                }
                boolean deleted = false;
                if (getConfig().getConfigurationSection("deleted-default-kits") != null) {
                    for (String key : getConfig().getConfigurationSection("deleted-default-kits").getKeys(false)) {
                        if (key.equalsIgnoreCase(k) && getConfig().getBoolean("deleted-default-kits." + key, false)) {
                            deleted = true;
                            break;
                        }
                    }
                }
                return disabled || deleted;
            });
            return this.filter(kits, args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("deletekit")) {
            List<String> kits = new ArrayList<>();
            if (getConfig().getConfigurationSection("kits") != null) {
                kits.addAll(getConfig().getConfigurationSection("kits").getKeys(false));
            }
            return this.filter(kits, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> input, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String s : input) {
            if (!s.toLowerCase(Locale.ROOT).startsWith(p)) continue;
            out.add(s);
        }
        return out;
    }

    private void broadcastToDuel(Duel duel, String message) {
        Player p1 = Bukkit.getPlayer(duel.p1);
        Player p2 = Bukkit.getPlayer(duel.p2);
        if (p1 != null) {
            p1.sendMessage(message);
        }
        if (p2 != null) {
            p2.sendMessage(message);
        }
    }

    private String pref() {
        return getConfig().getString("general.prefix", "&8[&cPvP&8] &f");
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(this.cc(text == null ? "" : text));
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private String replace(String src, String key, String value) {
        return (src == null ? "" : src).replace(key, value == null ? "" : value);
    }

    private static final class Duel {
        final UUID p1;
        final UUID p2;
        final Location p1Return;
        final Location p2Return;
        ItemStack[] p1Inv;
        ItemStack[] p1Armor;
        ItemStack[] p2Inv;
        ItemStack[] p2Armor;
        final String kit;
        final long startedAt = System.currentTimeMillis();
        boolean canDamage = false;
        boolean ending = false;
        final java.util.List<org.bukkit.block.BlockState> placedBlocks = new java.util.ArrayList<>();

        Duel(UUID p1, UUID p2, Location p1Return, Location p2Return, String kit) {
            this.p1 = p1;
            this.p2 = p2;
            this.p1Return = p1Return;
            this.p2Return = p2Return;
            this.kit = kit;
        }

        boolean isParticipant(UUID uuid) {
            return this.p1.equals(uuid) || this.p2.equals(uuid);
        }

        UUID other(UUID uuid) {
            return this.p1.equals(uuid) ? this.p2 : this.p1;
        }
    }

    private static final class Invite {
        final UUID inviter;
        final UUID target;
        final String kit;
        final long createdAt;

        Invite(UUID inviter, UUID target, String kit, long createdAt) {
            this.inviter = inviter;
            this.target = target;
            this.kit = kit;
            this.createdAt = createdAt;
        }
    }
}
