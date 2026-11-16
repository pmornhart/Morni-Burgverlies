package net.morni.burgverlies.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Alle Raumvorlagen – eigene Entwürfe für eine Burg mit Kellern, Kerkern und Grüften. Wie sie aussehen, baut
 * {@code world.RoomPlans}; hier stehen nur die Stellen, die fürs Spiel zählen.
 */
public final class RoomCatalog {

    public static final int HALF = 7;
    public static final int HEIGHT = 6;
    public static final int BOSS_HALF = 12;
    public static final int BOSS_HEIGHT = 10;

    private static final List<RoomTemplate> ALL = new ArrayList<>();

    private RoomCatalog() {
    }

    private static Pos p(int x, int z) {
        return new Pos(x, 1, z);
    }

    private static Pos p(int x, int y, int z) {
        return new Pos(x, y, z);
    }

    private static void add(RoomTemplate t) {
        if (byId(t.id()).isPresent()) {
            throw new IllegalStateException("Vorlage doppelt: " + t.id());
        }
        ALL.add(t);
    }

    private static RoomTemplate simple(String id, RoomType type, int minFloor, List<Pos> spawns, List<Pos> chests) {
        return new RoomTemplate(id, type, HALF, HEIGHT, false, minFloor, spawns, chests, List.of(), false, null, null,
                null);
    }

    static {
        // ---------- Ankunft ----------
        add(simple("torkammer", RoomType.START, 0, List.of(), List.of()));
        add(simple("treppenabsatz", RoomType.START, 1, List.of(), List.of()));

        // ---------- Kampf ----------
        List<Pos> ring = List.of(p(-5, -1), p(5, -1), p(-4, 4), p(4, 4), p(0, 5), p(-5, 2), p(5, 2));
        add(simple("wachstube", RoomType.FIGHT, 0, ring, List.of()));
        add(simple("kerker", RoomType.FIGHT, 0,
                List.of(p(-5, -4), p(5, -4), p(-5, 0), p(5, 0), p(-5, 4), p(5, 4), p(0, 3)), List.of()));
        add(simple("weinkeller", RoomType.FIGHT, 0,
                List.of(p(-4, -3), p(4, -3), p(-3, 3), p(3, 3), p(0, 5), p(-5, 0), p(5, 0)), List.of()));
        add(simple("ruestkammer", RoomType.FIGHT, 1, ring, List.of()));
        add(simple("gruft", RoomType.FIGHT, 1,
                List.of(p(-4, -4), p(4, -4), p(-4, 4), p(4, 4), p(0, 5), p(-2, 0), p(2, 0)), List.of()));

        // ---------- Fallen (nur geradeaus) ----------
        add(new RoomTemplate("fallengang", RoomType.TRAP, HALF, HEIGHT, true, 0, List.of(), List.of(), List.of(), false,
                null, null, new RoomTemplate.Trap(List.of(
                        new RoomTemplate.Emitter(p(-HALF, 1, -3), Dir.EAST, 0),
                        new RoomTemplate.Emitter(p(HALF, 2, -1), Dir.WEST, 20),
                        new RoomTemplate.Emitter(p(-HALF, 2, 2), Dir.EAST, 40),
                        new RoomTemplate.Emitter(p(HALF, 1, 4), Dir.WEST, 10)),
                        List.of(new RoomTemplate.JetRow(-4, 0), new RoomTemplate.JetRow(0, 1),
                                new RoomTemplate.JetRow(3, 0)),
                        0, -1)));
        add(new RoomTemplate("brunnenschacht", RoomType.TRAP, HALF, HEIGHT, true, 0, List.of(), List.of(), List.of(),
                false, null, null, new RoomTemplate.Trap(List.of(
                        new RoomTemplate.Emitter(p(-HALF, 2, -2), Dir.EAST, 0),
                        new RoomTemplate.Emitter(p(HALF, 2, 2), Dir.WEST, 30)),
                        List.of(), -5, 4)));
        add(new RoomTemplate("glutkammer", RoomType.TRAP, HALF, HEIGHT, true, 1, List.of(), List.of(), List.of(), false,
                null, null, new RoomTemplate.Trap(List.of(
                        new RoomTemplate.Emitter(p(HALF, 2, 0), Dir.WEST, 15)),
                        List.of(new RoomTemplate.JetRow(-4, 0), new RoomTemplate.JetRow(-2, 1),
                                new RoomTemplate.JetRow(0, 0), new RoomTemplate.JetRow(2, 1),
                                new RoomTemplate.JetRow(4, 0)),
                        0, -1)));

        // ---------- Rätsel ----------
        // Hebel an der Eingangswand (dort ist nie ein Ausgang), Farbe in der Wand darüber.
        List<Pos> levers = List.of(p(-5, 2, -HALF), p(-3, 2, -HALF), p(3, 2, -HALF), p(5, 2, -HALF));
        add(new RoomTemplate("hebelsaal", RoomType.PUZZLE, HALF, HEIGHT, false, 0,
                List.of(p(-4, 4), p(4, 4), p(0, 5)), List.of(), levers, false, p(0, 1, -4), null, null));
        add(new RoomTemplate("siegelkammer", RoomType.PUZZLE, HALF, HEIGHT, false, 0,
                List.of(p(-4, 5), p(4, 5), p(0, 5)), List.of(),
                List.of(p(-4, -2), p(4, -2), p(-4, 3), p(4, 3)), true, p(0, 1, -4), null, null));

        // ---------- Schatz ----------
        add(simple("schatzkammer", RoomType.TREASURE, 0, List.of(),
                List.of(p(-5, -4), p(5, -4), p(-5, 4), p(5, 4))));
        add(simple("zehntkeller", RoomType.TREASURE, 0, List.of(), List.of(p(-4, 5), p(4, 5), p(-5, -3))));

        // ---------- Rast und Krämerin ----------
        add(simple("wachfeuer", RoomType.REST, 0, List.of(), List.of()));
        add(new RoomTemplate("kraemerstube", RoomType.SHOP, HALF, HEIGHT, false, 0, List.of(), List.of(), List.of(),
                false, null, p(-5, -2), null));

        // ---------- Thronsaal des Kerkermeisters ----------
        int b = BOSS_HALF - 3;
        add(new RoomTemplate("thronsaal", RoomType.BOSS, BOSS_HALF, BOSS_HEIGHT, false, 2,
                List.of(p(-b, -b), p(b, -b), p(-b, b), p(b, b)), List.of(), List.of(), false, null, p(0, 1, 5),
                null));
    }

    public static List<RoomTemplate> all() {
        return List.copyOf(ALL);
    }

    public static Optional<RoomTemplate> byId(String id) {
        return ALL.stream().filter(t -> t.id().equals(id)).findFirst();
    }

    /** Vorlagen einer Art, die auf dieser Etage vorkommen dürfen (bei START nur die passende). */
    public static List<RoomTemplate> of(RoomType type, int floor) {
        List<RoomTemplate> out = new ArrayList<>();
        for (RoomTemplate t : ALL) {
            if (t.type() != type || t.minFloor() > floor) {
                continue;
            }
            if (type == RoomType.START && (floor == 0) != (t.minFloor() == 0)) {
                continue;
            }
            out.add(t);
        }
        return out;
    }
}
