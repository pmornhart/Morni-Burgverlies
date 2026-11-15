package de.moinmorni.burgverlies.world;

import de.moinmorni.burgverlies.dungeon.Pos;
import de.moinmorni.burgverlies.dungeon.RoomTemplate;
import de.moinmorni.burgverlies.dungeon.TrapLayout;
import java.util.List;

/**
 * Baupläne der Räume im Vorlagen-Rahmen (Eingang im Norden). Jede Etage hat ihr eigenes Gestein: oben
 * Kellergewölbe aus Steinziegeln, darunter Kerker aus Tiefenschiefer, ganz unten die Gruft aus Schwarzstein.
 * Türen schneidet {@code run.Doors} erst beim Bauen einer Etage hinein.
 */
public final class RoomPlans {

    /** Farben der Rätsel-Hebel bzw. -Platten (Reihenfolge = Index). */
    public static final List<String> PUZZLE_COLOURS = List.of("red", "yellow", "lime", "light_blue");

    /** Gestein einer Etage. */
    public record Palette(String wall, String wallAlt, String crack, String floor, String floorAlt, String pillar,
            String beam, String lantern, String stairs, String slab, String wallBlock) {
    }

    public static final Palette[] PALETTES = {
        new Palette("stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "stone_bricks", "cobblestone",
                "chiseled_stone_bricks", "spruce_log", "lantern", "stone_brick_stairs", "stone_brick_slab",
                "stone_brick_wall"),
        new Palette("deepslate_bricks", "deepslate_tiles", "cracked_deepslate_bricks", "deepslate_tiles",
                "cobbled_deepslate", "chiseled_deepslate", "dark_oak_log", "lantern", "deepslate_brick_stairs",
                "deepslate_brick_slab", "deepslate_brick_wall"),
        new Palette("polished_blackstone_bricks", "blackstone", "cracked_polished_blackstone_bricks",
                "polished_blackstone", "blackstone", "chiseled_polished_blackstone", "crimson_stem", "soul_lantern",
                "polished_blackstone_brick_stairs", "polished_blackstone_brick_slab",
                "polished_blackstone_brick_wall")
    };

    private RoomPlans() {
    }

    public static Palette palette(int floor) {
        return PALETTES[Math.max(0, Math.min(PALETTES.length - 1, floor))];
    }

    /** Fester „Zufall“ je Block – gleiche Etage, gleiches Muster. */
    static int noise(int x, int y, int z, long seed) {
        long h = seed ^ (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (int) (h & 0x7fffffff);
    }

    /**
     * Plan für einen Raum.
     *
     * @param crumble bröckelnder Boden (nur Fallengänge mit bröckelndem Boden), sonst {@code null}
     */
    public static Plan build(RoomTemplate t, int floor, long seed, TrapLayout crumble) {
        Plan p = new Plan();
        Palette pal = palette(floor);
        shell(p, t.half(), t.height(), pal, seed);
        switch (t.id()) {
            case "torkammer" -> torkammer(p, pal);
            case "treppenabsatz" -> treppenabsatz(p, pal);
            case "wachstube" -> wachstube(p, pal);
            case "kerker" -> kerker(p, pal);
            case "weinkeller" -> weinkeller(p, pal);
            case "ruestkammer" -> ruestkammer(p, pal);
            case "gruft" -> gruft(p, pal);
            case "fallengang", "brunnenschacht", "glutkammer" -> trap(p, t, pal, seed, crumble);
            case "hebelsaal", "siegelkammer" -> puzzle(p, t, pal);
            case "schatzkammer", "zehntkeller" -> treasure(p, t, pal);
            case "wachfeuer" -> rest(p, pal);
            case "kraemerstube" -> shop(p, pal);
            case "thronsaal" -> throne(p, t, pal);
            default -> {
                // nur die Hülle
            }
        }
        lights(p, t.half(), t.height());
        p.connect();
        return p;
    }

    // ---------- Hülle ----------

    private static void shell(Plan p, int h, int height, Palette pal, long seed) {
        int w = h + 1;
        for (int x = -w; x <= w; x++) {
            for (int z = -w; z <= w; z++) {
                int n = noise(x, 0, z, seed) % 100;
                p.set(x, 0, z, n < 12 ? pal.floorAlt() : n < 16 ? pal.crack() : pal.floor());
                p.set(x, height + 1, z, noise(x, 9, z, seed) % 100 < 15 ? pal.wallAlt() : pal.wall());
                boolean edge = Math.abs(x) == w || Math.abs(z) == w;
                for (int y = 1; y <= height; y++) {
                    if (edge) {
                        int m = noise(x, y, z, seed) % 100;
                        p.set(x, y, z, y == 1 ? pal.pillar() : m < 18 ? pal.wallAlt() : m < 24 ? pal.crack() : pal.wall());
                    } else {
                        p.set(x, y, z, "cave_air");
                    }
                }
            }
        }
        // Säulen in den Ecken und Balken unter der Decke
        for (int sx : new int[] {-h, h}) {
            for (int sz : new int[] {-h, h}) {
                for (int y = 1; y <= height; y++) {
                    p.set(sx, y, sz, y == height ? pal.pillar() : pal.wallBlock());
                }
            }
        }
        for (int z = -h + 3; z <= h - 3; z += 5) {
            for (int x = -h; x <= h; x++) {
                p.set(x, height, z, pal.beam() + "[axis=x]");
            }
        }
    }

    /** Unsichtbares Licht unter der Decke (die Lampen sind Schmuck, das Licht kommt von hier). */
    private static void lights(Plan p, int h, int height) {
        for (int x = -h + 2; x <= h - 2; x += 4) {
            for (int z = -h + 2; z <= h - 2; z += 4) {
                for (int y = height - 1; y >= 3; y--) {
                    var d = p.get(x, y, z);
                    if (d == null || d.getMaterial() == org.bukkit.Material.CAVE_AIR) {
                        p.set(x, y, z, "light[level=15]");
                        break;
                    }
                }
            }
        }
    }

    private static void hangingLantern(Plan p, Palette pal, int x, int z, int height) {
        p.set(x, height, z, "iron_chain[axis=y]");
        p.set(x, height - 1, z, pal.lantern() + "[hanging=true]");
    }

    private static void barrel(Plan p, int x, int y, int z, String facing) {
        p.set(x, y, z, "barrel[facing=" + facing + "]");
    }

    private static void table(Plan p, int x1, int z1, int x2, int z2) {
        p.fill(x1, 1, z1, x2, 1, z2, "spruce_slab[type=top]");
    }

    private static void cobwebs(Plan p, int h, int height) {
        p.set(-h + 1, height, -h, "cobweb");
        p.set(h - 1, height, h, "cobweb");
        p.set(-h, height, h - 1, "cobweb");
        p.set(h, height - 1, -h + 1, "cobweb");
    }

    // ---------- Räume ----------

    private static void torkammer(Plan p, Palette pal) {
        // Großes Fallgitter in der Nordwand, durch das man hereingekommen ist
        p.fill(-2, 1, -7, 2, 4, -7, "iron_bars");
        p.fill(-3, 5, -7, 3, 5, -7, pal.pillar());
        for (int x : new int[] {-5, 5}) {
            p.set(x, 1, -6, "spruce_fence");
            p.set(x, 2, -6, pal.lantern());
        }
        barrel(p, -7, 1, 6, "up");
        barrel(p, -7, 2, 6, "up");
        barrel(p, -5, 1, 6, "north");
        p.set(7, 1, 6, "hay_block");
        p.set(7, 1, 5, "hay_block");
        p.set(5, 1, 6, "barrel[facing=up]");
        hangingLantern(p, pal, 0, 0, 6);
        p.set(-7, 1, -4, "red_wall_banner[facing=east]");
        p.set(7, 1, -4, "red_wall_banner[facing=west]");
    }

    private static void treppenabsatz(Plan p, Palette pal) {
        // Treppe, die aus der Decke kommt (von der Etage darüber)
        for (int i = 0; i < 5; i++) {
            int x = -6 + i;
            for (int y = 1; y <= 5 - i; y++) {
                p.set(x, y, -6, pal.wall());
            }
            p.set(x, 6 - i, -6, pal.stairs() + "[facing=west]");
        }
        p.set(5, 1, -6, "barrel[facing=up]");
        p.set(6, 1, -6, "barrel[facing=up]");
        p.set(6, 1, 6, "skeleton_skull[rotation=6]");
        hangingLantern(p, pal, 0, 0, 6);
        cobwebs(p, 7, 6);
    }

    private static void wachstube(Plan p, Palette pal) {
        table(p, -3, -4, -2, -3);
        p.set(-4, 1, -4, "spruce_stairs[facing=east]");
        p.set(-1, 1, -3, "spruce_stairs[facing=west]");
        p.set(-3, 2, -4, "candle[candles=2,lit=true]");
        // Stockbetten an der Ostwand
        p.set(7, 1, -6, "red_bed[facing=north,part=head]");
        p.set(7, 1, -5, "red_bed[facing=north,part=foot]");
        p.set(7, 1, 6, "red_bed[facing=south,part=head]");
        p.set(7, 1, 5, "red_bed[facing=south,part=foot]");
        p.set(-7, 1, 5, "grindstone[face=floor,facing=north]");
        p.set(-7, 1, 6, "anvil[facing=east]");
        p.set(-7, 1, -6, "barrel[facing=up]");
        p.set(-5, 1, -6, "barrel[facing=up]");
        p.set(-7, 2, -6, "barrel[facing=up]");
        hangingLantern(p, pal, 0, 0, 6);
        hangingLantern(p, pal, 3, 3, 6);
    }

    private static void kerker(Plan p, Palette pal) {
        for (int s : new int[] {-1, 1}) {
            int front = 4 * s;
            for (int z : new int[] {-6, -5, -4, -2, 2, 4, 5, 6}) {
                p.fill(front, 1, z, front, 3, z, "iron_bars");
            }
            for (int x = 5; x <= 7; x++) {
                p.fill(x * s, 1, -2, x * s, 3, -2, "iron_bars");
                p.fill(x * s, 1, 2, x * s, 3, 2, "iron_bars");
            }
            p.set(7 * s, 1, -6, "brown_carpet");
            p.set(7 * s, 1, 6, "brown_carpet");
            p.set(7 * s, 1, -5, "brown_carpet");
            p.set(7 * s, 3, -6, "iron_chain[axis=y]");
            p.set(7 * s, 1, 5, "skeleton_skull[rotation=" + (s > 0 ? 12 : 4) + "]");
        }
        hangingLantern(p, pal, 0, -3, 6);
        hangingLantern(p, pal, 0, 3, 6);
        cobwebs(p, 7, 6);
    }

    private static void weinkeller(Plan p, Palette pal) {
        for (int s : new int[] {-1, 1}) {
            String face = s < 0 ? "east" : "west";
            for (int[] range : new int[][] {{-6, -3}, {3, 6}}) {
                for (int z = range[0]; z <= range[1]; z++) {
                    barrel(p, 7 * s, 1, z, face);
                    barrel(p, 7 * s, 2, z, face);
                    p.set(7 * s, 3, z, "spruce_slab[type=bottom]");
                }
            }
        }
        p.fill(-2, 0, -6, 2, 0, 6, "spruce_planks");
        table(p, -1, 1, 1, 1);
        p.set(0, 2, 1, "candle[candles=3,lit=true]");
        hangingLantern(p, pal, 0, -4, 6);
        hangingLantern(p, pal, 0, 4, 6);
        cobwebs(p, 7, 6);
    }

    private static void ruestkammer(Plan p, Palette pal) {
        p.set(-7, 1, -6, "anvil[facing=north]");
        p.set(-7, 1, -5, "smithing_table");
        p.set(-7, 1, -4, "grindstone[face=floor,facing=east]");
        p.set(7, 1, -6, "stonecutter[facing=west]");
        p.set(7, 1, -5, "blast_furnace[facing=west]");
        p.set(7, 1, 6, "bookshelf");
        for (int z = 4; z <= 6; z++) {
            p.set(-7, 1, z, "spruce_fence");
            p.set(-7, 2, z, "lightning_rod[facing=up]");
        }
        p.set(-7, 3, -3, "red_wall_banner[facing=east]");
        p.set(7, 3, 3, "red_wall_banner[facing=west]");
        hangingLantern(p, pal, 0, 0, 6);
    }

    private static void gruft(Plan p, Palette pal) {
        for (int s : new int[] {-1, 1}) {
            for (int[] r : new int[][] {{-6, -4}, {4, 6}}) {
                p.fill(7 * s, 1, r[0], 7 * s, 1, r[1], "polished_andesite");
                p.set(7 * s, 2, r[0] + 1, "candle[candles=3,lit=true]");
            }
            p.set(7 * s, 1, 0, "bone_block[axis=y]");
            p.set(7 * s, 2, 0, "skeleton_skull[rotation=" + (s > 0 ? 12 : 4) + "]");
        }
        p.set(0, 1, 0, "candle[candles=4,lit=true]");
        hangingLantern(p, pal, -3, 0, 6);
        hangingLantern(p, pal, 3, 0, 6);
        cobwebs(p, 7, 6);
    }

    private static void trap(Plan p, RoomTemplate t, Palette pal, long seed, TrapLayout crumble) {
        RoomTemplate.Trap trap = t.trap();
        int h = t.half();
        for (RoomTemplate.Emitter e : trap.arrows()) {
            int wx = e.at().x() < 0 ? -h - 1 : h + 1;
            p.set(wx, e.at().y(), e.at().z(), "dispenser[facing=" + e.shoots().name().toLowerCase(java.util.Locale.ROOT)
                    + "]");
        }
        for (RoomTemplate.JetRow row : trap.jets()) {
            for (int x = -h; x <= h; x++) {
                p.set(x, 0, row.z(), "chiseled_polished_blackstone");
            }
        }
        if (crumble != null) {
            for (int z = crumble.zMin(); z <= crumble.zMax(); z++) {
                for (int x = -h; x <= h; x++) {
                    p.set(x, 0, z, crumble.crumbles(x, z) ? "cracked_stone_bricks" : pal.floor());
                    p.set(x, -1, z, "cave_air");
                    p.set(x, -2, z, "cave_air");
                    p.set(x, -3, z, "soul_soil");
                }
            }
            for (int z = crumble.zMin() - 1; z <= crumble.zMax() + 1; z++) {
                for (int y = -3; y <= -1; y++) {
                    p.set(-h - 1, y, z, pal.wall());
                    p.set(h + 1, y, z, pal.wall());
                }
            }
            for (int x = -h; x <= h; x++) {
                for (int y = -3; y <= -1; y++) {
                    p.set(x, y, crumble.zMin() - 1, pal.wall());
                    p.set(x, y, crumble.zMax() + 1, pal.wall());
                }
            }
            p.set(0, -2, 0, "light[level=8]");
            p.set(-h, 4, -h + 1, "iron_chain[axis=y]");
            p.set(h, 4, h - 1, "iron_chain[axis=y]");
        }
        // Ziel: goldener Rand
        for (int x = -h; x <= h; x++) {
            p.set(x, 0, t.goalZ(), "chiseled_stone_bricks");
        }
        if ("glutkammer".equals(t.id())) {
            for (int z = -5; z <= 5; z += 2) {
                p.set(-h - 1, 3, z, "magma_block");
                p.set(h + 1, 3, z, "magma_block");
            }
        }
        if ("brunnenschacht".equals(t.id())) {
            p.set(-h + 1, 1, h - 1, "water_cauldron[level=3]");
            p.set(h - 1, 1, -h + 1, "cauldron");
        }
        cobwebs(p, h, t.height());
    }

    private static void puzzle(Plan p, RoomTemplate t, Palette pal) {
        int h = t.half();
        for (int i = 0; i < t.inputs().size(); i++) {
            Pos in = t.inputs().get(i);
            String colour = PUZZLE_COLOURS.get(i % PUZZLE_COLOURS.size());
            if (t.plates()) {
                p.set(in.x(), 0, in.z(), colour + "_concrete");
                p.set(in.x(), 1, in.z(), "stone_pressure_plate");
            } else {
                p.set(in.x(), in.y() + 1, -h - 1, colour + "_concrete");
                p.set(in.x(), in.y(), in.z(), "lever[face=wall,facing=south,powered=false]");
            }
        }
        Pos bell = t.bell();
        p.set(bell.x(), 0, bell.z(), pal.pillar());
        p.set(bell.x(), bell.y(), bell.z(), "bell[attachment=floor,facing=east]");
        if ("hebelsaal".equals(t.id())) {
            for (int z = -6; z <= -3; z++) {
                p.set(-7, 1, z, "bookshelf");
                p.set(7, 1, z, "bookshelf");
                p.set(-7, 2, z, "bookshelf");
                p.set(7, 2, z, "bookshelf");
            }
            p.set(-7, 1, 6, "bookshelf");
        } else {
            for (int[] c : new int[][] {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
                p.set(c[0], 1, c[1], pal.pillar());
                p.set(c[0], 2, c[1], "candle[candles=4,lit=true]");
            }
        }
        hangingLantern(p, pal, 0, 2, t.height());
    }

    private static void treasure(Plan p, RoomTemplate t, Palette pal) {
        for (Pos c : t.chests()) {
            String facing = Math.abs(c.x()) >= Math.abs(c.z()) ? (c.x() < 0 ? "east" : "west")
                    : (c.z() < 0 ? "south" : "north");
            p.set(c.x(), c.y(), c.z(), "chest[facing=" + facing + "]");
        }
        if ("schatzkammer".equals(t.id())) {
            p.set(-6, 1, 0, "gold_block");
            p.set(-6, 1, -1, "raw_gold_block");
            p.set(6, 1, 0, "raw_gold_block");
            p.set(6, 1, 1, "gold_block");
            p.set(-6, 2, 0, "candle[candles=3,lit=true]");
            p.set(0, 0, 0, "chiseled_stone_bricks");
            p.set(-6, 1, 6, "amethyst_cluster[facing=up]");
            p.set(6, 1, -6, "amethyst_cluster[facing=up]");
        } else {
            for (int x = -6; x <= -4; x++) {
                p.set(x, 1, -6, "hay_block");
            }
            p.set(6, 1, -6, "barrel[facing=up]");
            p.set(6, 2, -6, "barrel[facing=up]");
            p.set(6, 1, -5, "composter");
        }
        hangingLantern(p, pal, 0, 0, t.height());
    }

    private static void rest(Plan p, Palette pal) {
        p.set(0, 1, 0, "campfire[lit=true]");
        p.set(0, 0, 0, "cobblestone");
        p.set(-2, 1, -1, "spruce_stairs[facing=east]");
        p.set(-2, 1, 1, "spruce_stairs[facing=east]");
        p.set(2, 1, -1, "spruce_stairs[facing=west]");
        p.set(2, 1, 1, "spruce_stairs[facing=west]");
        p.set(-7, 1, -6, "white_bed[facing=north,part=head]");
        p.set(-7, 1, -5, "white_bed[facing=north,part=foot]");
        p.set(7, 1, 6, "white_bed[facing=south,part=head]");
        p.set(7, 1, 5, "white_bed[facing=south,part=foot]");
        p.set(7, 1, -6, "barrel[facing=up]");
        p.set(-7, 1, 6, "flower_pot");
        hangingLantern(p, pal, -4, -4, 6);
        hangingLantern(p, pal, 4, 4, 6);
    }

    private static void shop(Plan p, Palette pal) {
        // Theke vor der Krämerin, Regale dahinter
        for (int z = -4; z <= 0; z++) {
            p.set(-3, 1, z, "spruce_slab[type=top]");
        }
        for (int z = -5; z <= 1; z++) {
            p.set(-7, 1, z, "barrel[facing=east]");
            p.set(-7, 2, z, z % 2 == 0 ? "bookshelf" : "barrel[facing=east]");
        }
        p.set(-3, 2, -3, "candle[candles=2,lit=true]");
        // Treppe hinab zur nächsten Etage: eine Stufe pro Reihe, dahinter wird es dunkel
        for (int x = -2; x <= 2; x++) {
            p.set(x, 0, 3, pal.stairs() + "[facing=north]");
            for (int z = 4; z <= 7; z++) {
                int step = z - 3;
                for (int y = -step + 1; y <= 0; y++) {
                    p.set(x, y, z, "cave_air");
                }
                p.set(x, -step, z, pal.stairs() + "[facing=north]");
                for (int y = -5; y < -step; y++) {
                    p.set(x, y, z, pal.wall());
                }
            }
            for (int y = -5; y <= 0; y++) {
                p.set(x, y, 8, "black_concrete");
            }
        }
        for (int z = 3; z <= 7; z++) {
            for (int y = -5; y <= 0; y++) {
                p.set(-3, y, z, pal.wall());
                p.set(3, y, z, pal.wall());
            }
            p.set(-3, 1, z, pal.wallBlock());
            p.set(3, 1, z, pal.wallBlock());
        }
        p.set(-3, 2, 3, pal.lantern());
        p.set(3, 2, 3, pal.lantern());
        p.set(5, 1, -6, "barrel[facing=up]");
        p.set(6, 1, -6, "barrel[facing=up]");
        hangingLantern(p, pal, 0, -2, 6);
    }

    private static void throne(Plan p, RoomTemplate t, Palette pal) {
        int h = t.half();
        int top = t.height();
        // Säulen mit Ketten
        for (int sx : new int[] {-7, 6}) {
            for (int sz : new int[] {-7, 6}) {
                p.fill(sx, 1, sz, sx + 1, top, sz + 1, pal.pillar());
                p.set(sx, top - 1, sz - 1, "iron_chain[axis=y]");
                p.set(sx, top - 2, sz - 1, pal.lantern() + "[hanging=true]");
            }
        }
        // Thron im Süden
        p.fill(-2, 1, h - 1, 2, 1, h, pal.wall());
        p.set(0, 2, h - 1, pal.stairs() + "[facing=north]");
        p.fill(-1, 2, h, 1, 5, h, "chiseled_polished_blackstone");
        p.set(0, 6, h, "gilded_blackstone");
        p.set(-2, 2, h - 1, "soul_campfire[lit=true]");
        p.set(2, 2, h - 1, "soul_campfire[lit=true]");
        // Roter Läufer vom Eingang zum Thron
        for (int z = -h; z <= h - 2; z++) {
            p.set(0, 1, z, "red_carpet");
        }
        // Käfige an den Seiten
        for (int s : new int[] {-1, 1}) {
            int x = (h - 1) * s;
            p.fill(x, 4, -2, x, 6, -2, "iron_bars");
            p.fill(x, 4, 2, x, 6, 2, "iron_bars");
            p.set(x, top, 0, "iron_chain[axis=y]");
            p.set(h * s, 1, -10, "soul_campfire[lit=true]");
            p.set(h * s, 1, 10, "soul_campfire[lit=true]");
        }
        for (int z = -h + 2; z <= h - 2; z += 4) {
            p.set(-h, 3, z, "soul_lantern");
            p.set(h, 3, z, "soul_lantern");
        }
    }
}
