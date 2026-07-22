package id.rumahkita.guilds;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class GuildPvPListener implements Listener {
    
    private final GuildManager guildManager;

    public GuildPvPListener(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = getAttacker(event.getDamager());
        
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        Guild victimGuild = guildManager.getGuild(victim);
        Guild attackerGuild = guildManager.getGuild(attacker);
        
        String chunkKey = victim.getLocation().getWorld().getName() + ";" + victim.getLocation().getChunk().getX() + ";" + victim.getLocation().getChunk().getZ();
        Guild claimGuild = guildManager.getGuildByChunk(chunkKey);
        
        if (claimGuild != null && !claimGuild.isAllowPvp()) {
            if (victimGuild == null || !victimGuild.equals(claimGuild) || attackerGuild == null || !attackerGuild.equals(claimGuild)) {
                event.setCancelled(true);
                Text.msg(attacker, "&cYou cannot fight in " + claimGuild.getName() + "'s territory! &7(PvP is disabled)");
                return;
            }
        }

        if (victimGuild == null || attackerGuild == null) {
            return; 
        }

        if (victimGuild.equals(attackerGuild)) {
            if (!victimGuild.isFriendlyFire()) {
                event.setCancelled(true);
                Text.msg(attacker, "&cYou cannot hurt your guild members! &7(Friendly fire is disabled)");
            }
        } else if (victimGuild.isAlly(attackerGuild.getTag()) && attackerGuild.isAlly(victimGuild.getTag())) {
            if (!attackerGuild.isFriendlyFire() || !victimGuild.isFriendlyFire()) {
                event.setCancelled(true);
                Text.msg(attacker, "&cYou cannot hurt allied guild members! &7(Friendly fire is disabled)");
            }
        }
    }

    private Player getAttacker(Object damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        } else if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        return null;
    }
}
