package de.moinmorni.burgverlies.run;

import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.Ui;
import de.moinmorni.burgverlies.dungeon.MobKind;
import de.moinmorni.burgverlies.dungeon.Pos;
import de.moinmorni.burgverlies.dungeon.Revives;
import de.moinmorni.burgverlies.dungeon.Scaling;
import de.moinmorni.burgverlies.dungeon.Waves;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * „Der Kerkermeister“: ein großer, eigener Endgegner mit drei Abschnitten. Jeder Angriff kündigt sich an (Kreis auf
 * dem Boden, Seelenfeuer, Kette in der Luft) – wer aufpasst, weicht aus.
 * <ul>
 *   <li>Abschnitt 1: Schockwelle (springen!)</li>
 *   <li>Abschnitt 2 (unter ⅔ Leben): ruft Wachen + Schockwelle</li>
 *   <li>Abschnitt 3 (unter ⅓): Ketten ziehen zu ihm heran und bremsen, dazu alles andere, schneller</li>
 * </ul>
 * Beim Wechsel ist er kurz unverwundbar.
 */
public final class Boss {

    enum Attack { SHOCKWAVE, SUMMON, CHAINS }

    private static final double SCALE = 1.7;
    private static final int TRANSITION = 50;
    private static final int[] COOLDOWN = {0, 150, 130, 95};
    private static final int SHOCK_WARN = 30;
    private static final int SUMMON_WARN = 30;
    private static final int CHAIN_WARN = 25;
    private static final double SHOCK_RADIUS = 7.5;
    private static final double MELEE = 7;
    /** So weit „sieht“ er (Vindicator sonst 12) – der ganze Thronsaal. */
    private static final double FOLLOW_RANGE = 40;
    /** Wie oft er nachschaut, ob er noch den Richtigen jagt. */
    private static final int HUNT_TICKS = 20;

    private final Runs runs;
    private final Run run;
    private final Room room;
    private final Random random;
    private Vindicator entity;
    private BossBar bar;
    private int phase = 1;
    private int invulnerable;
    private int cooldown = 80;
    private int rotation;
    private Attack pending;
    private int pendingTicks;
    private Player chainTarget;
    private int shockwaves;
    private int summons;
    private int chains;
    private int hunt;

    Boss(Runs runs, Run run, Room room) {
        this.runs = runs;
        this.run = run;
        this.room = room;
        this.random = new Random(run.seed ^ 0xB055L);
    }

    void spawn(Location at) {
        double hp = Scaling.bossHealth(run.difficulty, run.size());
        entity = at.getWorld().spawn(at, Vindicator.class, v -> {
            Mobs.mark(v, run, null);
            v.addScoreboardTag(Mobs.BOSS_TAG);
            v.customName(name());
            v.setCustomNameVisible(true);
            v.setRemoveWhenFarAway(false);
            v.setCanPickupItems(false);
            v.setPatrolLeader(false);
            AttributeInstance scale = v.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(SCALE);
            }
            AttributeInstance follow = v.getAttribute(Attribute.FOLLOW_RANGE);
            if (follow != null) {
                follow.setBaseValue(FOLLOW_RANGE);
            }
            AttributeInstance kb = v.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (kb != null) {
                kb.setBaseValue(0.6);
            }
            Mobs.health(v, hp);
            var eq = v.getEquipment();
            eq.setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
            eq.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            eq.setItemInMainHandDropChance(0f);
            eq.setHelmetDropChance(0f);
        });
        run.entities.add(entity.getUniqueId());
        hunt();
        bar = BossBar.bossBar(name(), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        for (Player p : runs.players(run)) {
            p.showBossBar(bar);
        }
        at.getWorld().spawnParticle(Particle.LARGE_SMOKE, at.clone().add(0, 1, 0), 60, 0.8, 1.2, 0.8, 0.02);
        runs.soundAll(run, "entity.warden.emerge", 0.8f);
    }

    private Component name() {
        Component n = Component.text(Texts.raw(run.lang, "boss.name"), TextColor.color(0xE0603A))
                .decoration(TextDecoration.BOLD, true);
        if (phase > 1) {
            n = n.append(Component.text(" · " + Texts.raw(run.lang, "boss.phase").replace("<n>",
                    String.valueOf(phase)), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false));
        }
        return n;
    }

    public boolean is(org.bukkit.entity.Entity e) {
        return entity != null && entity.getUniqueId().equals(e.getUniqueId());
    }

    public boolean alive() {
        return entity != null && entity.isValid() && !entity.isDead();
    }

    public int phase() {
        return phase;
    }

    /** Für die Prüfung: Zustand als JSON. */
    String json() {
        double hp = alive() ? entity.getHealth() : 0;
        double max = alive() ? entity.getAttribute(Attribute.MAX_HEALTH).getValue() : 0;
        return "{\"health\":" + Math.round(hp) + ",\"max\":" + Math.round(max) + ",\"phase\":" + phase
                + ",\"invulnerable\":" + invulnerable + ",\"shockwaves\":" + shockwaves + ",\"summons\":" + summons
                + ",\"chains\":" + chains + ",\"pending\":\"" + (pending == null ? "" : pending.name())
                + "\",\"target\":\"" + (alive() && entity.getTarget() instanceof Player t ? t.getName() : "") + "\"}";
    }

    /** Schaden abfangen, solange er zwischen zwei Abschnitten ist. */
    boolean shielded() {
        return invulnerable > 0;
    }

    /** Wie stark er zuschlägt. */
    double melee() {
        return MELEE * Scaling.mobDamage(run.difficulty, run.size()) * (phase >= 3 ? 1.2 : 1);
    }

    void tick() {
        if (!alive()) {
            return;
        }
        double max = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        double share = entity.getHealth() / max;
        bar.progress((float) Math.max(0, Math.min(1, share)));
        // Nicht aus dem Saal laufen
        if (!runs.inside(run, room, entity.getLocation(), 0)) {
            entity.teleport(runs.at(run, room, new Pos(0, 1, 0), null));
        }
        int target = share <= 1.0 / 3 ? 3 : share <= 2.0 / 3 ? 2 : 1;
        if (target > phase) {
            phase = target;
            transition();
            return;
        }
        if (invulnerable > 0) {
            invulnerable--;
            entity.getWorld().spawnParticle(Particle.ENCHANT, entity.getLocation().add(0, 2, 0), 6, 0.6, 1, 0.6, 0.5);
            if (invulnerable == 0) {
                entity.setAware(true);
            }
            return;
        }
        if (pending != null) {
            telegraph();
            return;
        }
        if (--hunt <= 0) {
            hunt();
        }
        if (--cooldown <= 0) {
            choose();
        }
    }

    private void transition() {
        invulnerable = TRANSITION;
        pending = null;
        // Der neue Abschnitt beginnt mit seinem eigenen Angriff (Wachen bzw. Ketten).
        rotation = 0;
        cooldown = 40;
        entity.setAware(false);
        bar.name(name());
        bar.color(phase == 2 ? BossBar.Color.PURPLE : BossBar.Color.YELLOW);
        if (phase == 3) {
            AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * 1.2);
            }
        }
        for (Player p : runs.players(run)) {
            String lang = runs.lang(p);
            Ui.title(p, Texts.text(lang, "boss.phase" + phase + ".title"), Texts.text(lang, "boss.phase" + phase
                    + ".subtitle"), 2500);
        }
        runs.soundAll(run, "entity.ravager.roar", 0.6f);
        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
    }

    private void choose() {
        List<Attack> order = switch (phase) {
            case 1 -> List.of(Attack.SHOCKWAVE);
            case 2 -> List.of(Attack.SUMMON, Attack.SHOCKWAVE);
            default -> List.of(Attack.CHAINS, Attack.SHOCKWAVE, Attack.SUMMON);
        };
        Attack a = order.get(rotation++ % order.size());
        if (a == Attack.CHAINS) {
            List<Player> up = upInRoom();
            if (up.isEmpty()) {
                a = Attack.SHOCKWAVE;
            } else {
                chainTarget = up.get(random.nextInt(up.size()));
            }
        }
        pending = a;
        pendingTicks = 0;
        String key = "boss.warn." + a.name().toLowerCase(java.util.Locale.ROOT);
        for (Player p : runs.players(run)) {
            p.sendActionBar(Texts.text(runs.lang(p), key));
        }
        switch (a) {
            case SHOCKWAVE -> {
                entity.setAware(false);
                runs.soundAll(run, "entity.ravager.roar", 0.5f);
            }
            case SUMMON -> runs.soundAll(run, "block.bell.use", 0.6f);
            case CHAINS -> runs.soundAll(run, "block.chain.place", 0.5f);
        }
    }

    private void telegraph() {
        pendingTicks++;
        Location c = entity.getLocation();
        switch (pending) {
            case SHOCKWAVE -> {
                double r = SHOCK_RADIUS * pendingTicks / SHOCK_WARN;
                ring(c, r, Color.fromRGB(0xE0303A));
                ring(c, SHOCK_RADIUS, Color.fromRGB(0x802020));
                if (pendingTicks >= SHOCK_WARN) {
                    shockwave(c);
                    done();
                }
            }
            case SUMMON -> {
                for (Pos s : room.placed.template().spawns()) {
                    Location l = runs.at(run, room, s, null).add(0, 0.2, 0);
                    l.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, l, 4, 0.3, 0.1, 0.3, 0.02);
                }
                if (pendingTicks >= SUMMON_WARN) {
                    summon();
                    done();
                }
            }
            case CHAINS -> {
                if (chainTarget == null || !chainTarget.isOnline()) {
                    done();
                    return;
                }
                if (pendingTicks % 2 == 0) {
                    line(c.clone().add(0, 2, 0), chainTarget.getLocation().add(0, 1, 0));
                }
                if (pendingTicks >= CHAIN_WARN) {
                    chain(c);
                    done();
                }
            }
        }
    }

    private void done() {
        pending = null;
        chainTarget = null;
        cooldown = COOLDOWN[phase];
        entity.setAware(true);
    }

    private void shockwave(Location c) {
        shockwaves++;
        runs.soundAll(run, "entity.generic.explode", 0.7f);
        c.getWorld().spawnParticle(Particle.EXPLOSION, c, 6, 2, 0.2, 2, 0);
        double dmg = 5 * Scaling.mobDamage(run.difficulty, run.size());
        for (Player p : upInRoom()) {
            Location l = p.getLocation();
            double dx = l.getX() - c.getX();
            double dz = l.getZ() - c.getZ();
            boolean grounded = l.getY() - c.getY() < 0.6;
            if (dx * dx + dz * dz <= SHOCK_RADIUS * SHOCK_RADIUS && grounded) {
                p.damage(dmg, entity);
                Vector away = new Vector(dx, 0, dz);
                if (away.lengthSquared() < 0.01) {
                    away = new Vector(1, 0, 0);
                }
                p.setVelocity(away.normalize().multiply(1.1).setY(0.55));
            }
        }
    }

    private void summon() {
        summons++;
        List<MobKind> kinds = Waves.summons(random, run.difficulty, run.size(), phase);
        List<Pos> spots = room.placed.template().spawns();
        for (int i = 0; i < kinds.size(); i++) {
            Location l = runs.at(run, room, spots.get(i % spots.size()), null);
            Mob m = Mobs.spawn(run, kinds.get(i), l);
            room.mobs.add(m.getUniqueId());
            l.getWorld().spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 15, 0.3, 0.6, 0.3, 0.02);
        }
    }

    private void chain(Location c) {
        chains++;
        Player p = chainTarget;
        if (p == null || run.revives.state(p.getUniqueId()) != Revives.State.UP
                || p.getLocation().distanceSquared(c) > 18 * 18) {
            return;
        }
        Vector pull = c.toVector().subtract(p.getLocation().toVector());
        if (pull.lengthSquared() > 0.01) {
            p.setVelocity(pull.normalize().multiply(1.3).setY(0.35));
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        p.damage(3 * Scaling.mobDamage(run.difficulty, run.size()), entity);
        runs.soundAll(run, "block.chain.break", 0.6f);
    }

    /** Er geht von selbst auf den Nächsten los – auch wenn alle Abstand halten oder sein Ziel am Boden liegt. */
    private void hunt() {
        hunt = HUNT_TICKS;
        List<Player> up = upInRoom();
        if (entity.getTarget() instanceof Player t && up.contains(t)) {
            return;
        }
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : up) {
            double d = p.getLocation().distanceSquared(entity.getLocation());
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        entity.setTarget(best);
    }

    private List<Player> upInRoom() {
        List<Player> out = new ArrayList<>();
        for (Player p : runs.players(run)) {
            if (run.revives.state(p.getUniqueId()) == Revives.State.UP && runs.inside(run, room, p.getLocation(), 1)) {
                out.add(p);
            }
        }
        return out;
    }

    private static void ring(Location c, double r, Color colour) {
        Particle.DustOptions dust = new Particle.DustOptions(colour, 1.4f);
        int points = Math.max(8, (int) (r * 6));
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            c.getWorld().spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * r, c.getY() + 0.15,
                    c.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0, dust);
        }
    }

    private static void line(Location from, Location to) {
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0x8A8F98), 1.2f);
        Vector step = to.toVector().subtract(from.toVector());
        double len = step.length();
        if (len < 0.1) {
            return;
        }
        step.multiply(0.5 / len);
        Location l = from.clone();
        for (double d = 0; d < len; d += 0.5) {
            l.getWorld().spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, dust);
            l.add(step);
        }
    }

    void hideBar() {
        if (bar == null) {
            return;
        }
        for (Player p : runs.plugin().getServer().getOnlinePlayers()) {
            p.hideBossBar(bar);
        }
    }

    void hideFor(Player p) {
        if (bar != null) {
            p.hideBossBar(bar);
        }
    }

    void showBar(Player p) {
        if (bar != null && alive()) {
            p.showBossBar(bar);
        }
    }
}
