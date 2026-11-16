package net.morni.burgverlies.dungeon;

import java.util.List;

/**
 * Handgebaute Raumvorlage. Alle Stellen liegen im Raum-Rahmen: Mitte = 0/0, Boden y = 0, man steht auf y = 1,
 * Eingang im Norden (z = −half−1), geradeaus geht es nach Süden. Die Türen liegen mittig in den Wänden (3 breit,
 * 4 hoch) – vor allen Türen bleibt der Weg frei, damit der Raum in jede Richtung weiterführen kann.
 *
 * @param id           Name der Vorlage (auch Text-Schlüssel {@code room.<id>})
 * @param half         halbe Innenbreite (Innenraum von −half bis +half)
 * @param height       Innenhöhe (Luft von y = 1 bis y = height)
 * @param straightOnly nur verwenden, wenn der Weg geradeaus weitergeht (Fallengänge)
 * @param minFloor     erst ab dieser Etage (0 = oberste)
 * @param spawns       Stellen, an denen Gegner auftauchen
 * @param chests       Truhen (Schatz)
 * @param inputs       Hebel bzw. Druckplatten (Rätsel), in Farb-Reihenfolge
 * @param plates       {@code true}: Rätsel mit Druckplatten statt Hebeln
 * @param bell         Glocke, die die Reihenfolge noch einmal zeigt (Rätsel), sonst {@code null}
 * @param npc          Krämerin (Krämerstube), sonst {@code null}
 * @param trap         Fallen (Fallengang), sonst {@code null}
 */
public record RoomTemplate(
        String id,
        RoomType type,
        int half,
        int height,
        boolean straightOnly,
        int minFloor,
        List<Pos> spawns,
        List<Pos> chests,
        List<Pos> inputs,
        boolean plates,
        Pos bell,
        Pos npc,
        Trap trap) {

    /** Schusslöcher in der Wand: Pfeil fliegt quer durch den Gang; {@code phase} versetzt den Takt. */
    public record Emitter(Pos at, Dir shoots, int phase) {
    }

    /** Reihe von Glutdüsen im Boden quer über den Raum (z), Gruppe 0 oder 1 wechseln sich ab. */
    public record JetRow(int z, int group) {
    }

    /**
     * Fallen eines Fallengangs.
     *
     * @param arrows     Pfeil-Schusslöcher
     * @param jets       Glutdüsen-Reihen
     * @param crumbleMin bröckelnder Boden ab dieser Reihe (z) – {@code crumbleMax < crumbleMin} = keiner
     * @param crumbleMax bis zu dieser Reihe
     */
    public record Trap(List<Emitter> arrows, List<JetRow> jets, int crumbleMin, int crumbleMax) {

        public boolean crumbles() {
            return crumbleMax >= crumbleMin;
        }
    }

    public RoomTemplate {
        spawns = List.copyOf(spawns);
        chests = List.copyOf(chests);
        inputs = List.copyOf(inputs);
    }

    /** Ab dieser Reihe (z) ist man am Ziel eines Fallengangs. */
    public int goalZ() {
        return half - 1;
    }

    /** Liegt die Stelle im Innenraum? */
    public boolean inside(Pos p) {
        return Math.abs(p.x()) <= half && Math.abs(p.z()) <= half && p.y() >= 1 && p.y() <= height;
    }
}
