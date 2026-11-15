package de.moinmorni.burgverlies.dungeon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Mitspieler-Suche: Gruppen (allein oder als Party) mit derselben Schwierigkeit werden zu Teams bis
 * {@link Scaling#MAX_PLAYERS} zusammengelegt – die am längsten wartende Gruppe zuerst. Ein Team startet, sobald es
 * voll ist oder seine älteste Gruppe lange genug gewartet hat.
 */
public final class Matchmaker {

    /** Eine wartende Gruppe: {@code id} = Anführer, {@code since} = Wartebeginn (ms). */
    public record Group(UUID id, int size, Difficulty difficulty, long since) {
    }

    private Matchmaker() {
    }

    /** Teams, die jetzt starten sollen (die übrigen warten weiter). */
    public static List<List<Group>> ready(List<Group> waiting, long now, long maxWaitMs) {
        List<List<Group>> out = new ArrayList<>();
        for (Difficulty d : Difficulty.values()) {
            List<Group> open = new ArrayList<>(waiting.stream().filter(g -> g.difficulty() == d)
                    .sorted(Comparator.comparingLong(Group::since)).toList());
            while (!open.isEmpty()) {
                List<Group> team = new ArrayList<>();
                int size = 0;
                for (Group g : List.copyOf(open)) {
                    if (size + g.size() <= Scaling.MAX_PLAYERS) {
                        team.add(g);
                        size += g.size();
                        open.remove(g);
                    }
                }
                if (team.isEmpty()) {
                    break;
                }
                boolean full = size >= Scaling.MAX_PLAYERS;
                boolean waited = now - team.get(0).since() >= maxWaitMs;
                if (full || waited) {
                    out.add(team);
                }
            }
        }
        return out;
    }
}
