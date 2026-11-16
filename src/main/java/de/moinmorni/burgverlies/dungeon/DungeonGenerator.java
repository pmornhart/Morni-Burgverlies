package de.moinmorni.burgverlies.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Würfelt eine Etage aus den Raumvorlagen: erst die Reihenfolge der Raumarten (mit festen Regeln), dann einen Weg
 * durchs Raster, der sich nie kreuzt (Suche mit Zurückgehen – findet immer einen Weg). Gleicher Startwert = gleiche
 * Etage.
 */
public final class DungeonGenerator {

    /** Abstand der Raummitten in Blöcken. */
    public static final int CELL = 32;
    /** Raster: x von −GRID_X bis +GRID_X, z von 0 bis GRID_Z. */
    public static final int GRID_X = 3;
    public static final int GRID_Z = 6;
    public static final int FLOORS = 3;
    public static final int MIN_MIDDLE = 3;
    public static final int MAX_MIDDLE = 8;

    private DungeonGenerator() {
    }

    /** Startwert einer Etage aus dem Startwert des Abstiegs. */
    public static long floorSeed(long runSeed, int floor) {
        return runSeed * 31 + floor * 0x9E3779B97F4A7C15L;
    }

    /**
     * @param middle Räume zwischen Ankunft und Ende (Krämerstube bzw. Rast + Thronsaal)
     */
    public static FloorPlan generate(long runSeed, int floor, int middle) {
        long seed = floorSeed(runSeed, floor);
        Random rnd = new Random(seed);
        int n = Math.max(MIN_MIDDLE, Math.min(MAX_MIDDLE, middle));
        List<RoomType> types = sequence(rnd, floor, n);
        List<Dir> path = path(rnd, types);
        List<RoomTemplate> templates = pickTemplates(rnd, floor, types, path);
        List<PlacedRoom> rooms = new ArrayList<>();
        int cx = 0;
        int cz = 0;
        for (int i = 0; i < types.size(); i++) {
            Dir entry = i == 0 ? null : path.get(i - 1).opposite();
            Dir exit = i < path.size() ? path.get(i) : null;
            int rotation = entry != null ? entry.ordinal() : exit.opposite().ordinal();
            rooms.add(new PlacedRoom(i, templates.get(i), cx, cz, entry, exit, rotation));
            if (exit != null) {
                cx += exit.dx();
                cz += exit.dz();
            }
        }
        return new FloorPlan(floor, seed, rooms);
    }

    /** Reihenfolge der Raumarten. */
    static List<RoomType> sequence(Random rnd, int floor, int n) {
        List<RoomType> middle = new ArrayList<>();
        middle.add(RoomType.TRAP);
        middle.add(RoomType.PUZZLE);
        if (n >= 4) {
            middle.add(RoomType.TREASURE);
        }
        if (floor == 1 && n >= 6) {
            middle.add(RoomType.REST);
        }
        if (n >= 8) {
            middle.add(RoomType.TREASURE);
        }
        while (middle.size() < n - 1) {
            middle.add(RoomType.FIGHT);
        }
        // Mischen, bis die Regeln passen (zwei Schätze oder zwei Fallen nicht direkt nacheinander).
        List<RoomType> order;
        int tries = 0;
        do {
            order = new ArrayList<>(middle);
            Collections.shuffle(order, rnd);
            order.add(0, RoomType.FIGHT);
            tries++;
        } while (!fine(order) && tries < 200);
        List<RoomType> out = new ArrayList<>();
        out.add(RoomType.START);
        out.addAll(order);
        if (floor >= FLOORS - 1) {
            out.add(RoomType.REST);
            out.add(RoomType.BOSS);
        } else {
            out.add(RoomType.SHOP);
        }
        return out;
    }

    private static boolean fine(List<RoomType> order) {
        for (int i = 1; i < order.size(); i++) {
            RoomType a = order.get(i - 1);
            RoomType b = order.get(i);
            if (a == b && a != RoomType.FIGHT) {
                return false;
            }
        }
        return true;
    }

    /** Weg durchs Raster: Richtung von Raum i zu Raum i+1. Fallengänge (Typ TRAP) gehen geradeaus. */
    static List<Dir> path(Random rnd, List<RoomType> types) {
        List<Dir> out = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        used.add(cell(0, 0));
        if (!walk(rnd, types, 0, 0, 0, null, used, out)) {
            throw new IllegalStateException("Kein Weg für " + types.size() + " Räume");
        }
        return out;
    }

    private static boolean walk(Random rnd, List<RoomType> types, int i, int x, int z, Dir came, Set<Long> used,
            List<Dir> out) {
        if (i == types.size() - 1) {
            return true;
        }
        List<Dir> options = new ArrayList<>(List.of(Dir.values()));
        Collections.shuffle(options, rnd);
        // Erster Schritt immer nach Süden: tiefer in die Burg.
        if (i == 0) {
            options = List.of(Dir.SOUTH);
        } else if (types.get(i) == RoomType.TRAP || types.get(i + 1) == RoomType.BOSS) {
            options = List.of(came);
        } else {
            // Geradeaus etwas häufiger, das liest sich wie ein Gang.
            options.remove(came);
            options.add(0, came);
            if (rnd.nextInt(3) == 0) {
                Collections.swap(options, 0, options.size() - 1);
            }
        }
        for (Dir d : options) {
            if (came != null && d == came.opposite()) {
                continue;
            }
            int nx = x + d.dx();
            int nz = z + d.dz();
            if (Math.abs(nx) > GRID_X || nz < 0 || nz > GRID_Z || used.contains(cell(nx, nz))) {
                continue;
            }
            // Der Thronsaal ist größer: um ihn herum muss das Raster frei bleiben.
            if (types.get(i + 1) == RoomType.BOSS && !bossRoom(nx, nz, d, used)) {
                continue;
            }
            used.add(cell(nx, nz));
            out.add(d);
            if (walk(rnd, types, i + 1, nx, nz, d, used, out)) {
                return true;
            }
            out.remove(out.size() - 1);
            used.remove(cell(nx, nz));
        }
        return false;
    }

    private static boolean bossRoom(int x, int z, Dir came, Set<Long> used) {
        for (Dir d : Dir.values()) {
            if (d == came.opposite()) {
                continue;
            }
            if (used.contains(cell(x + d.dx(), z + d.dz()))) {
                return false;
            }
        }
        return true;
    }

    private static List<RoomTemplate> pickTemplates(Random rnd, int floor, List<RoomType> types, List<Dir> path) {
        Map<RoomType, List<RoomTemplate>> pools = new HashMap<>();
        Map<RoomType, Integer> next = new HashMap<>();
        List<RoomTemplate> out = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            RoomType type = types.get(i);
            boolean straight = i > 0 && i < path.size() && path.get(i) == path.get(i - 1);
            List<RoomTemplate> pool = pools.computeIfAbsent(type, t -> {
                List<RoomTemplate> l = new ArrayList<>(RoomCatalog.of(t, floor));
                Collections.shuffle(l, rnd);
                return l;
            });
            if (pool.isEmpty()) {
                throw new IllegalStateException("Keine Vorlage für " + type + " auf Etage " + floor);
            }
            RoomTemplate chosen = null;
            int start = next.getOrDefault(type, 0);
            for (int k = 0; k < pool.size(); k++) {
                RoomTemplate t = pool.get((start + k) % pool.size());
                if (t.straightOnly() && !straight) {
                    continue;
                }
                if (!out.isEmpty() && out.get(out.size() - 1).id().equals(t.id()) && pool.size() > 1) {
                    continue;
                }
                chosen = t;
                next.put(type, start + k + 1);
                break;
            }
            if (chosen == null) {
                throw new IllegalStateException("Keine passende Vorlage für " + type);
            }
            out.add(chosen);
        }
        return out;
    }

    private static long cell(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
