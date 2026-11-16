package net.morni.burgverlies.hall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.burgverlies.BurgverliesPlugin;
import net.morni.burgverlies.Texts;
import net.morni.burgverlies.Ui;
import net.morni.burgverlies.dungeon.Difficulty;
import net.morni.burgverlies.dungeon.Matchmaker;
import org.bukkit.entity.Player;

/**
 * Mitspieler-Suche: Gruppen mit derselben Schwierigkeit werden zu Teams bis 4 zusammengelegt. Ein Team startet,
 * wenn es voll ist oder lange genug gewartet hat – oder sobald wieder ein Verlies frei ist.
 */
public final class Queue {

    /** Eine wartende Gruppe (Anführer zuerst). */
    private record Group(UUID leader, List<UUID> members, Difficulty difficulty, long since) {
    }

    private final BurgverliesPlugin plugin;
    private final Map<UUID, Group> groups = new LinkedHashMap<>();

    public Queue(BurgverliesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean contains(UUID u) {
        return groups.values().stream().anyMatch(g -> g.members().contains(u));
    }

    public int waiting() {
        return groups.values().stream().mapToInt(g -> g.members().size()).sum();
    }

    public int waiting(Difficulty d) {
        return groups.values().stream().filter(g -> g.difficulty() == d).mapToInt(g -> g.members().size()).sum();
    }

    /** Gruppe anmelden (vorher abmelden, falls schon da). */
    public void join(List<Player> group, Difficulty d) {
        for (Player p : group) {
            remove(p.getUniqueId(), false);
        }
        List<UUID> ids = group.stream().map(Player::getUniqueId).toList();
        groups.put(ids.get(0), new Group(ids.get(0), ids, d, System.currentTimeMillis()));
        for (Player p : group) {
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "queue.joined", Placeholder.component("difficulty",
                    Texts.text(lang, "difficulty." + d.id())),
                    Placeholder.unparsed("seconds", String.valueOf(plugin.settings().searchSeconds()))));
            Ui.sound(p, "block.note_block.hat", 1.2f);
            plugin.hall().items(p);
            plugin.hall().sidebar(p);
        }
    }

    /** Jemand will nicht mehr suchen (ganze Gruppe raus). */
    public void remove(UUID u, boolean tell) {
        Group g = groups.values().stream().filter(x -> x.members().contains(u)).findFirst().orElse(null);
        if (g == null) {
            return;
        }
        groups.remove(g.leader());
        for (UUID m : g.members()) {
            Player p = plugin.getServer().getPlayer(m);
            if (p == null) {
                continue;
            }
            if (tell) {
                p.sendMessage(Texts.msg(plugin.lang(p), "queue.left"));
            }
            if (plugin.hall().inHall(p)) {
                plugin.hall().items(p);
                plugin.hall().sidebar(p);
            }
        }
    }

    /** Jede Sekunde: Teams bilden und starten. */
    public void tick() {
        if (groups.isEmpty()) {
            return;
        }
        List<Matchmaker.Group> waiting = new ArrayList<>();
        for (Group g : groups.values()) {
            waiting.add(new Matchmaker.Group(g.leader(), g.members().size(), g.difficulty(), g.since()));
        }
        long now = System.currentTimeMillis();
        for (List<Matchmaker.Group> team : Matchmaker.ready(waiting, now, plugin.settings().searchSeconds() * 1000L)) {
            if (!plugin.runs().hasFreeSlot()) {
                return;
            }
            List<Player> players = new ArrayList<>();
            Difficulty d = team.get(0).difficulty();
            for (Matchmaker.Group mg : team) {
                Group g = groups.remove(mg.id());
                if (g == null) {
                    continue;
                }
                for (UUID u : g.members()) {
                    Player p = plugin.getServer().getPlayer(u);
                    if (p != null && plugin.runs().of(p).isEmpty()) {
                        players.add(p);
                    }
                }
            }
            if (!players.isEmpty()) {
                plugin.runs().start(players, d);
            }
        }
        // Wer wartet, sieht die Zeit
        for (Group g : groups.values()) {
            long left = Math.max(0, plugin.settings().searchSeconds() - (now - g.since()) / 1000);
            for (UUID u : g.members()) {
                Player p = plugin.getServer().getPlayer(u);
                if (p != null) {
                    p.sendActionBar(Texts.text(plugin.lang(p), "queue.bar", Placeholder.unparsed("n",
                            String.valueOf(waiting(g.difficulty()))), Placeholder.unparsed("seconds",
                            String.valueOf(left))));
                }
            }
        }
    }
}
