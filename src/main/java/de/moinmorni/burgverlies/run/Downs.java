package de.moinmorni.burgverlies.run;

import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.Ui;
import de.moinmorni.burgverlies.dungeon.Revives;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.potion.PotionEffect;

/**
 * Hinfallen und Wiederbeleben im Spiel: wer fällt, liegt am Boden (kann sich nicht bewegen, leuchtet, nimmt keinen
 * Schaden, Gegner lassen ihn in Ruhe). Wer daneben Schleichen hält, hilft ihm auf. Raus = zuschauen bis zur
 * nächsten Rast oder Etage.
 */
final class Downs {

    /** Alle so viele Spielticks wird gezählt. */
    static final int STEP = 5;
    private static final double HELP_RANGE = 2.5;
    private static final double REVIVE_HEALTH = 0.4;
    private static final float WALK = 0.2f;

    private final Runs runs;

    Downs(Runs runs) {
        this.runs = runs;
    }

    /** Tödlicher Treffer für jemanden, der steht. */
    void fall(Run run, Player p) {
        Revives.Fall f = run.revives.fall(p.getUniqueId());
        String name = p.getName();
        switch (f) {
            case DOWN -> {
                applyDown(p);
                String lang = runs.lang(p);
                Ui.title(p, Texts.text(lang, "down.title"),
                        Texts.text(lang, run.revives.solo() ? "down.subtitle.solo" : "down.subtitle"), 2500);
                Ui.sound(p, "entity.player.hurt", 0.6f);
                runs.broadcastExcept(run, p, "down.other", Placeholder.unparsed("name", name));
            }
            case OUT -> {
                applyOut(run, p);
                Ui.title(p, Texts.text(runs.lang(p), "out.title"), Texts.text(runs.lang(p), "out.subtitle"), 3000);
                runs.broadcastExcept(run, p, "out.other", Placeholder.unparsed("name", name));
            }
            default -> {
                return;
            }
        }
        if (run.revives.allDown()) {
            runs.lose(run);
        }
    }

    void tick(Run run) {
        List<Player> online = runs.players(run);
        List<Revives.Event> events = run.revives.tick(id -> helped(run, online, id));
        for (Player p : online) {
            UUID u = p.getUniqueId();
            String lang = runs.lang(p);
            if (run.revives.state(u) == Revives.State.DOWN) {
                double pr = run.revives.progress(u);
                long left = (long) run.revives.bleedLeft(u) * STEP / 20;
                p.sendActionBar(pr > 0 ? Texts.text(lang, "down.bar.help", Placeholder.unparsed("bar", Ui.bar(pr, 10)))
                        : Texts.text(lang, run.revives.solo() ? "down.bar.solo" : "down.bar",
                                Placeholder.unparsed("seconds", String.valueOf(left))));
                p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 0.6, 0), 1, 0.3, 0.2, 0.3, 0);
                if (!p.getPose().equals(Pose.SWIMMING)) {
                    p.setPose(Pose.SWIMMING, true);
                }
            }
        }
        for (Player helper : online) {
            if (!helper.isSneaking() || run.revives.state(helper.getUniqueId()) != Revives.State.UP) {
                continue;
            }
            for (Player d : online) {
                if (d != helper && run.revives.state(d.getUniqueId()) == Revives.State.DOWN
                        && near(helper, d)) {
                    helper.sendActionBar(Texts.text(runs.lang(helper), "down.bar.helper",
                            Placeholder.unparsed("name", d.getName()),
                            Placeholder.unparsed("bar", Ui.bar(run.revives.progress(d.getUniqueId()), 10))));
                }
            }
        }
        for (Revives.Event e : events) {
            Player p = runs.plugin().getServer().getPlayer(e.player());
            if (p == null) {
                continue;
            }
            switch (e.kind()) {
                case REVIVED, SELF_REVIVED -> {
                    applyUp(p, REVIVE_HEALTH);
                    Ui.title(p, Texts.text(runs.lang(p), "revived.title"), Texts.text(runs.lang(p), "revived.subtitle",
                            Placeholder.unparsed("left", String.valueOf(run.revives.revivesLeft(p.getUniqueId())))),
                            1500);
                    Ui.sound(p, "entity.player.levelup", 1.4f);
                    p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 30, 0.4, 0.6,
                            0.4, 0.2);
                    runs.broadcastExcept(run, p, "revived.other", Placeholder.unparsed("name", p.getName()));
                }
                case BLED_OUT -> {
                    applyOut(run, p);
                    Ui.title(p, Texts.text(runs.lang(p), "out.title"), Texts.text(runs.lang(p), "out.subtitle"), 3000);
                    runs.broadcastExcept(run, p, "out.other", Placeholder.unparsed("name", p.getName()));
                }
            }
        }
        if (!events.isEmpty() && run.revives.allDown()) {
            runs.lose(run);
        }
    }

    private boolean helped(Run run, List<Player> online, UUID downed) {
        Player d = runs.plugin().getServer().getPlayer(downed);
        if (d == null) {
            return false;
        }
        for (Player h : online) {
            if (h != d && h.isSneaking() && run.revives.state(h.getUniqueId()) == Revives.State.UP && near(h, d)) {
                return true;
            }
        }
        return false;
    }

    private static boolean near(Player a, Player b) {
        return a.getWorld() == b.getWorld() && a.getLocation().distanceSquared(b.getLocation()) <= HELP_RANGE * HELP_RANGE;
    }

    static void applyDown(Player p) {
        p.setHealth(Math.min(p.getHealth(), 2.0));
        p.setWalkSpeed(0f);
        jump(p, 0);
        p.setGlowing(true);
        p.setSprinting(false);
        p.setFireTicks(0);
        p.setPose(Pose.SWIMMING, true);
        for (Mob m : p.getWorld().getNearbyEntitiesByType(Mob.class, p.getLocation(), 32)) {
            if (p.equals(m.getTarget())) {
                m.setTarget(null);
            }
        }
    }

    static void applyUp(Player p, double share) {
        p.setWalkSpeed(WALK);
        jump(p, -1);
        p.setGlowing(false);
        p.setPose(Pose.STANDING, false);
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double m = max == null ? 20 : max.getValue();
        p.setHealth(Math.max(1, Math.min(m, m * share)));
    }

    void applyOut(Run run, Player p) {
        p.setPose(Pose.STANDING, false);
        p.setWalkSpeed(WALK);
        jump(p, -1);
        p.setGlowing(false);
        p.setGameMode(GameMode.SPECTATOR);
        Location at = runs.roomCenter(run, run.room());
        p.teleport(at);
    }

    /** Alle wieder auf den Beinen (Rast, neue Etage). */
    void restoreAll(Run run, Location to) {
        List<UUID> back = run.revives.restoreAll();
        for (UUID u : back) {
            Player p = runs.plugin().getServer().getPlayer(u);
            if (p == null) {
                continue;
            }
            p.setGameMode(GameMode.ADVENTURE);
            applyUp(p, 1.0);
            if (to != null) {
                p.teleport(to);
            }
            Ui.title(p, Texts.text(runs.lang(p), "back.title"), Texts.text(runs.lang(p), "back.subtitle"), 2000);
        }
    }

    /** Beim Verlassen und am Ende: alles zurücksetzen. */
    static void reset(Player p) {
        p.setWalkSpeed(WALK);
        jump(p, -1);
        p.setGlowing(false);
        p.setPose(Pose.STANDING, false);
        p.setFireTicks(0);
        for (PotionEffect e : p.getActivePotionEffects()) {
            p.removePotionEffect(e.getType());
        }
    }

    /** Sprungkraft: {@code -1} = normal. */
    private static void jump(Player p, double value) {
        AttributeInstance a = p.getAttribute(Attribute.JUMP_STRENGTH);
        if (a != null) {
            a.setBaseValue(value < 0 ? a.getDefaultValue() : value);
        }
    }
}
