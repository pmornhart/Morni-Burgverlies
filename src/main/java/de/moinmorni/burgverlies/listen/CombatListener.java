package de.moinmorni.burgverlies.listen;

import de.moinmorni.burgverlies.BurgverliesPlugin;
import de.moinmorni.burgverlies.dungeon.Revives;
import de.moinmorni.burgverlies.run.Mobs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

/** Schaden, Hinfallen statt Sterben, Gegner und Kerkermeister. */
public final class CombatListener implements Listener {

    private final BurgverliesPlugin plugin;

    public CombatListener(BurgverliesPlugin plugin) {
        this.plugin = plugin;
    }

    /** Wer hat eigentlich getroffen (bei Pfeilen der Schütze)? */
    private static Entity source(Entity damager) {
        if (damager instanceof Projectile pr) {
            ProjectileSource s = pr.getShooter();
            if (s instanceof Entity e) {
                return e;
            }
        }
        return damager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageBy(EntityDamageByEntityEvent e) {
        Entity src = source(e.getDamager());
        // Spieler gegeneinander: nie
        if (e.getEntity() instanceof Player && src instanceof Player) {
            e.setCancelled(true);
            return;
        }
        // Wer liegt oder raus ist, kann nicht zuschlagen
        if (src instanceof Player p && plugin.runs().state(p) != null && plugin.runs().state(p) != Revives.State.UP) {
            e.setCancelled(true);
            return;
        }
        if (e.getEntity() instanceof Player) {
            if (src.getScoreboardTags().contains(Mobs.BOSS_TAG)) {
                e.setDamage(plugin.runs().bossMelee(src));
            } else if (src.getScoreboardTags().contains(Mobs.TAG)) {
                e.setDamage(e.getDamage() * plugin.runs().damageFactor(src));
            }
        }
        if (e.getEntity().getScoreboardTags().contains("bv_trader")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        Entity ent = e.getEntity();
        if (ent.getScoreboardTags().contains(Mobs.BOSS_TAG) && plugin.runs().bossShielded(ent)) {
            e.setCancelled(true);
            return;
        }
        if (!(ent instanceof Player p) || p.getWorld() != plugin.world()) {
            return;
        }
        Revives.State state = plugin.runs().state(p);
        if (state == null) {
            // Im Torhaus tut nichts weh
            e.setCancelled(true);
            if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                p.teleport(plugin.hall().spawn());
            }
            return;
        }
        if (state != Revives.State.UP) {
            e.setCancelled(true);
            return;
        }
        if (e.getFinalDamage() >= p.getHealth()) {
            e.setCancelled(true);
            plugin.runs().fall(p);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() instanceof Player p && p.getWorld() == plugin.world()) {
            Revives.State s = plugin.runs().state(p);
            if (s != Revives.State.UP) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity ent = e.getEntity();
        if (ent instanceof Player || ent.getWorld() != plugin.world()) {
            return;
        }
        e.getDrops().clear();
        e.setDroppedExp(0);
        if (Mobs.runOf(ent) != 0) {
            plugin.runs().mobDied(ent, ent.getKiller());
        }
    }

    /** Falls doch jemand stirbt (z. B. /kill): wie hinfallen ohne Wiederbelebung. */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld() != plugin.world()) {
            return;
        }
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
        e.deathMessage(null);
        if (plugin.runs().state(p) == Revives.State.UP) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.runs().fall(p));
        }
    }

    @EventHandler
    public void onCombust(EntityCombustEvent e) {
        if (Mobs.runOf(e.getEntity()) != 0) {
            e.setCancelled(true);
        }
    }
}
