package id.rumahkita.essentials.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.entity.EntityType;
import java.util.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import id.rumahkita.essentials.RumahKitaEssentialsPlugin;

public class PlayerUtilityCommands implements CommandExecutor, Listener {
    private final RumahKitaEssentialsPlugin plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Map<UUID, org.bukkit.permissions.PermissionAttachment> vanishPerms = new HashMap<>();
    private final Set<UUID> godPlayers = new HashSet<>();
    private final Set<UUID> spyPlayers = new HashSet<>();
    
    public PlayerUtilityCommands(RumahKitaEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        switch (cmd) {
            case "heal": return handleHeal(sender, args);
            case "fly": return handleFly(sender, args);
            case "speed": return handleSpeed(sender, args);
            case "god": return handleGod(sender, args);
            case "smite": return handleSmite(sender, args);
            case "vanish": return handleVanish(sender, args);
            case "spy": return handleSpy(sender);
            case "invsee": return handleInvsee(sender, args);
            case "ec": return handleEc(sender, args);
            case "troll": return handleTroll(sender, args);
        }
        return false;
    }

    public boolean handleSpy(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();
        if (spyPlayers.contains(uuid)) {
            spyPlayers.remove(uuid);
            p.sendMessage(ChatColor.RED + "Command Spy disabled.");
        } else {
            spyPlayers.add(uuid);
            p.sendMessage(ChatColor.GREEN + "Command Spy enabled! You will see all commands typed by other players.");
        }
        return true;
    }

    public boolean handleGod(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length > 1) target = Bukkit.getPlayerExact(args[1]);
        else if (sender instanceof Player) target = (Player) sender;
        
        if (target != null) {
            UUID uuid = target.getUniqueId();
            if (godPlayers.contains(uuid)) {
                godPlayers.remove(uuid);
                sender.sendMessage(ChatColor.GREEN + "God Mode disabled for " + target.getName());
            } else {
                godPlayers.add(uuid);
                sender.sendMessage(ChatColor.GREEN + "God Mode enabled for " + target.getName());
            }
        }
        return true;
    }

    public boolean handleEc(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /rka ec <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            p.openInventory(target.getEnderChest());
            p.sendMessage(ChatColor.GREEN + "Opening Enderchest of " + target.getName());
        } else {
            p.sendMessage(ChatColor.RED + "Player is not online.");
        }
        return true;
    }

    public boolean handleTroll(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka troll <player> <type>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }

        String type = args[2].toLowerCase();
        switch (type) {
            case "launch":
                target.setVelocity(new Vector(0, 3, 0));
                sender.sendMessage(ChatColor.GREEN + "Launching " + target.getName());
                break;
            case "fakeop":
                target.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "[Server: Made " + target.getName() + " a server operator]");
                sender.sendMessage(ChatColor.GREEN + "Sending Fake OP message to " + target.getName());
                break;
            case "spin":
                Location loc = target.getLocation();
                loc.setYaw(loc.getYaw() + 180f);
                target.teleport(loc);
                sender.sendMessage(ChatColor.GREEN + "Spinning " + target.getName());
                break;
            case "blind":
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));
                sender.sendMessage(ChatColor.GREEN + "Blinding " + target.getName());
                break;
            case "drop":
                if (target.getInventory().getItemInMainHand().getType() != Material.AIR) {
                    target.getWorld().dropItemNaturally(target.getLocation(), target.getInventory().getItemInMainHand());
                    target.getInventory().setItemInMainHand(null);
                }
                sender.sendMessage(ChatColor.GREEN + "Dropping items of " + target.getName());
                break;
            case "scare":
                target.playSound(target.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
                sender.sendMessage(ChatColor.GREEN + "Jumpscaring " + target.getName());
                break;
            case "fakeban":
                target.kickPlayer(ChatColor.RED + "You are banned from this server.\n" + ChatColor.WHITE + "Reason: " + ChatColor.YELLOW + "Hacking / Cheating" + ChatColor.WHITE + "\n\nAppeal at our discord.");
                sender.sendMessage(ChatColor.GREEN + "Fake Banning " + target.getName());
                break;
            case "cobweb":
                target.getLocation().getBlock().setType(Material.COBWEB);
                sender.sendMessage(ChatColor.GREEN + "Trapping " + target.getName() + " in cobweb.");
                break;
            case "shuffle":
                List<ItemStack> items = new ArrayList<>();
                for (ItemStack item : target.getInventory().getContents()) {
                    items.add(item);
                }
                Collections.shuffle(items);
                target.getInventory().setContents(items.toArray(new ItemStack[0]));
                sender.sendMessage(ChatColor.GREEN + "Shuffled inventory of " + target.getName());
                break;
            case "potato":
                for (int i = 0; i < target.getInventory().getSize(); i++) {
                    if (target.getInventory().getItem(i) == null || target.getInventory().getItem(i).getType() == Material.AIR) {
                        target.getInventory().setItem(i, new ItemStack(Material.POISONOUS_POTATO));
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Filled empty slots of " + target.getName() + " with potatoes.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown troll type.");
        }
        return true;
    }

    public boolean handleInvsee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            ((Player) sender).openInventory(target.getInventory());
            sender.sendMessage(ChatColor.GREEN + "Opening inventory of " + target.getName());
        }
        return true;
    }

    public boolean handleVanish(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player target = (Player) sender;
        UUID uuid = target.getUniqueId();
        if (vanishedPlayers.contains(uuid)) {
            vanishedPlayers.remove(uuid);
            for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(plugin, target);
            if (target.hasMetadata("vanished")) target.removeMetadata("vanished", plugin);
            if (vanishPerms.containsKey(uuid)) {
                target.removeAttachment(vanishPerms.get(uuid));
                vanishPerms.remove(uuid);
            }
            Bukkit.broadcastMessage(ChatColor.YELLOW + target.getName() + " joined the game");
            sender.sendMessage(ChatColor.GREEN + "You are now visible (Unvanished).");
        } else {
            vanishedPlayers.add(uuid);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("rumahkita.admin")) p.hidePlayer(plugin, target);
            }
            target.setMetadata("vanished", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            org.bukkit.permissions.PermissionAttachment attachment = target.addAttachment(plugin);
            attachment.setPermission("essentials.afk.auto", false);
            vanishPerms.put(uuid, attachment);
            Bukkit.broadcastMessage(ChatColor.YELLOW + target.getName() + " left the game");
            sender.sendMessage(ChatColor.GREEN + "You are now hidden from normal players (Vanished).");
        }
        return true;
    }

    public boolean handleHeal(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length > 1) target = Bukkit.getPlayerExact(args[1]);
        else if (sender instanceof Player) target = (Player) sender;
        
        if (target != null) {
            target.setHealth(target.getMaxHealth());
            target.setFoodLevel(20);
            target.setFireTicks(0);
            for (PotionEffect effect : target.getActivePotionEffects()) target.removePotionEffect(effect.getType());
            sender.sendMessage(ChatColor.GREEN + target.getName() + " has been healed.");
        }
        return true;
    }

    public boolean handleFly(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length > 1) target = Bukkit.getPlayerExact(args[1]);
        else if (sender instanceof Player) target = (Player) sender;
        
        if (target != null) {
            target.setAllowFlight(!target.getAllowFlight());
            sender.sendMessage(ChatColor.GREEN + "Fly " + target.getName() + " : " + target.getAllowFlight());
        }
        return true;
    }

    public boolean handleSpeed(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) return true;
        Player p = (Player) sender;
        try {
            float speed = Float.parseFloat(args[1]) / 10f;
            if (speed < 0.1f || speed > 1f) throw new NumberFormatException();
            if (p.isFlying()) p.setFlySpeed(speed);
            else p.setWalkSpeed(speed);
            p.sendMessage(ChatColor.GREEN + "Speed changed to " + args[1]);
        } catch (Exception e) {
            p.sendMessage(ChatColor.RED + "Invalid number (1-10).");
        }
        return true;
    }

    public boolean handleSmite(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            target.getWorld().strikeLightningEffect(target.getLocation());
            target.setHealth(Math.max(0.5, target.getHealth() - 4.0));
            sender.sendMessage(ChatColor.GREEN + "Smiting " + target.getName() + " with lightning!");
        }
        return true;
    }
}
