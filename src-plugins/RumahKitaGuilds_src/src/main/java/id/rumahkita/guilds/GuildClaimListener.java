package id.rumahkita.guilds;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import java.util.Iterator;

public class GuildClaimListener implements Listener {

    private final GuildManager guildManager;

    public GuildClaimListener(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    private boolean isProtected(Location loc, Player player) {
        Chunk chunk = loc.getChunk();
        String chunkKey = loc.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ();
        
        Guild claimGuild = guildManager.getGuildByChunk(chunkKey);
        if (claimGuild == null) {
            return false;
        }
        if (claimGuild.isMember(player.getUniqueId())) {
            return false;
        }
        if (guildManager.getBypassPlayers().contains(player.getUniqueId())) {
            return false;
        }

        player.sendMessage(ChatColor.RED + "You are not allowed to interact in guild territory " + ChatColor.GOLD + claimGuild.getName() + ChatColor.RED + "!");
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (isProtected(event.getBlock().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityChangeBlock(org.bukkit.event.entity.EntityChangeBlockEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player) return;
        if (event.getEntity() instanceof org.bukkit.entity.FallingBlock) return;
        Block b = event.getBlock();
        String chunkKey = b.getWorld().getName() + ";" + b.getChunk().getX() + ";" + b.getChunk().getZ();
        Guild g = guildManager.getGuildByChunk(chunkKey);
        if (g != null && !g.isMobGriefing()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        if (event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM 
            || event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER
            || event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }
        if (event.getEntity() instanceof org.bukkit.entity.Monster 
            || event.getEntity() instanceof org.bukkit.entity.Slime
            || event.getEntity() instanceof org.bukkit.entity.Phantom
            || event.getEntity() instanceof org.bukkit.entity.Ghast) {
            Location loc = event.getLocation();
            String chunkKey = loc.getWorld().getName() + ";" + loc.getChunk().getX() + ";" + loc.getChunk().getZ();
            Guild g = guildManager.getGuildByChunk(chunkKey);
            if (g != null && !g.isMobSpawning()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (isProtected(event.getBlock().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getClickedBlock() == null) return;
        
        Chunk chunk = event.getClickedBlock().getChunk();
        String chunkKey = chunk.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ();
        Guild claimGuild = guildManager.getGuildByChunk(chunkKey);
        
        if (claimGuild != null && claimGuild.isPublicInteraction()) {
            org.bukkit.event.block.Action action = event.getAction();
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                org.bukkit.Material mat = event.getClickedBlock().getType();
                String name = mat.name();
                if (name.contains("DOOR") || name.contains("GATE") || name.contains("BUTTON") || name.contains("LEVER") || name.contains("PLATE")) {
                    return; 
                }
            }
            if (action == org.bukkit.event.block.Action.PHYSICAL) {
                org.bukkit.Material mat = event.getClickedBlock().getType();
                if (mat.name().contains("PLATE")) {
                    return;
                }
            }
        }
        
        if (isProtected(event.getClickedBlock().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        
        if (isProtected(event.getEntity().getLocation(), player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;
        if (isProtected(event.getRightClicked().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;

        if (from.getChunk().getX() != to.getChunk().getX() || from.getChunk().getZ() != to.getChunk().getZ() || !from.getWorld().equals(to.getWorld())) {
            
            String fromChunkKey = from.getWorld().getName() + ";" + from.getChunk().getX() + ";" + from.getChunk().getZ();
            String toChunkKey = to.getWorld().getName() + ";" + to.getChunk().getX() + ";" + to.getChunk().getZ();
            
            Guild fromGuild = guildManager.getGuildByChunk(fromChunkKey);
            Guild toGuild = guildManager.getGuildByChunk(toChunkKey);
            
            if (toGuild != null && (fromGuild == null || !fromGuild.getTag().equals(toGuild.getTag()))) {
                Player player = event.getPlayer();
                
                String subtitle = ChatColor.GRAY + "Welcome to our territory";
                if (toGuild.isMember(player.getUniqueId())) {
                    subtitle = ChatColor.GREEN + "Welcome back to your guild!";
                }

                player.sendTitle(
                    ChatColor.GOLD + "" + ChatColor.BOLD + toGuild.getName(),
                    subtitle,
                    10, 70, 20
                );
            }

            if (fromGuild != null && (toGuild == null || !toGuild.getTag().equals(fromGuild.getTag()))) {
                Player player = event.getPlayer();
                
                String subtitle = ChatColor.GRAY + "Leaving territory";
                if (fromGuild.isMember(player.getUniqueId())) {
                    subtitle = ChatColor.GREEN + "Leaving your guild territory";
                }
                
                player.sendTitle(
                    ChatColor.GOLD + "" + ChatColor.BOLD + fromGuild.getName(),
                    subtitle,
                    10, 70, 20
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) return;
        if (isProtected(event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (event.isCancelled()) return;
        if (isProtected(event.getBlockClicked().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            String chunkKey = b.getWorld().getName() + ";" + b.getChunk().getX() + ";" + b.getChunk().getZ();
            Guild g = guildManager.getGuildByChunk(chunkKey);
            if (g != null && !g.isMobGriefing()) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled()) return;
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            Guild g = guildManager.getGuildByChunk(b.getWorld().getName() + ";" + b.getChunk().getX() + ";" + b.getChunk().getZ());
            if (g != null) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.isCancelled()) return;
        Guild sourceGuild = guildManager.getGuildByChunk(event.getBlock().getWorld().getName() + ";" + event.getBlock().getChunk().getX() + ";" + event.getBlock().getChunk().getZ());
        for (Block b : event.getBlocks()) {
            Block next = b.getRelative(event.getDirection());
            Guild targetGuild = guildManager.getGuildByChunk(next.getWorld().getName() + ";" + next.getChunk().getX() + ";" + next.getChunk().getZ());
            if (targetGuild != null && (sourceGuild == null || !sourceGuild.getTag().equals(targetGuild.getTag()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isCancelled()) return;
        Guild sourceGuild = guildManager.getGuildByChunk(event.getBlock().getWorld().getName() + ";" + event.getBlock().getChunk().getX() + ";" + event.getBlock().getChunk().getZ());
        for (Block b : event.getBlocks()) {
            Guild targetGuild = guildManager.getGuildByChunk(b.getWorld().getName() + ";" + b.getChunk().getX() + ";" + b.getChunk().getZ());
            if (targetGuild != null && (sourceGuild == null || !sourceGuild.getTag().equals(targetGuild.getTag()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.isCancelled()) return;
        Guild sourceGuild = guildManager.getGuildByChunk(event.getBlock().getWorld().getName() + ";" + event.getBlock().getChunk().getX() + ";" + event.getBlock().getChunk().getZ());
        Guild targetGuild = guildManager.getGuildByChunk(event.getToBlock().getWorld().getName() + ";" + event.getToBlock().getChunk().getX() + ";" + event.getToBlock().getChunk().getZ());
        
        if (targetGuild != null && (sourceGuild == null || !sourceGuild.getTag().equals(targetGuild.getTag()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.isCancelled()) return;
        if (event.getPlayer() != null) {
            if (isProtected(event.getBlock().getLocation(), event.getPlayer())) {
                event.setCancelled(true);
            }
        } else {
            Guild g = guildManager.getGuildByChunk(event.getBlock().getWorld().getName() + ";" + event.getBlock().getChunk().getX() + ";" + event.getBlock().getChunk().getZ());
            if (g != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.isCancelled()) return;
        if (event.getSource().getType() == org.bukkit.Material.FIRE) {
            Guild sourceGuild = guildManager.getGuildByChunk(event.getSource().getWorld().getName() + ";" + event.getSource().getChunk().getX() + ";" + event.getSource().getChunk().getZ());
            Guild targetGuild = guildManager.getGuildByChunk(event.getBlock().getWorld().getName() + ";" + event.getBlock().getChunk().getX() + ";" + event.getBlock().getChunk().getZ());
            if (targetGuild != null && (sourceGuild == null || !sourceGuild.getTag().equals(targetGuild.getTag()))) {
                event.setCancelled(true);
            }
        }
    }
}
