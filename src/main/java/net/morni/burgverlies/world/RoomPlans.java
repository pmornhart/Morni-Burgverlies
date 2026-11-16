package net.morni.burgverlies.world;

import java.util.List;
import java.util.Locale;
import net.morni.burgverlies.dungeon.Dir;
import net.morni.burgverlies.dungeon.Pos;
import net.morni.burgverlies.dungeon.RoomSpace;
import net.morni.burgverlies.dungeon.RoomTemplate;
import net.morni.burgverlies.dungeon.TrapLayout;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

/**
 * Baupläne der Räume im Vorlagen-Rahmen (Eingang im Norden). Jede Etage hat ihr eigenes Gestein: oben
 * Kellergewölbe aus Steinziegeln, darunter Kerker aus Tiefenschiefer, ganz unten die Gruft aus Schwarzstein.
 *
 * <p>Gebaut wird in vier Schritten: Hülle (Wände mit Säulen und Bögen, Balken, Spinnweben) → Einrichtung des Raumes →
 * alles, was fürs Spiel frei bleiben muss, wieder leer räumen ({@link RoomSpace}) → Spiel-Blöcke (Fallen, Hebel,
 * Platten, Glocke, Truhen, Treppe). So kann die Einrichtung nie einen Weg, eine Tür oder eine Falle versperren.
 * Türen schneidet {@code world.Doors} erst beim Bauen einer Etage hinein.
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

    /**
     * Bau-Version der Räume. Räume werden bei jedem Abstieg frisch gebaut; die Zahl steht in {@code /bvadmin info}
     * und zeigt, welche Einrichtung gerade läuft.
     */
    public static final int VERSION = 2;
    private static final int LIGHT = 12;

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
        shell(p, t, pal, seed);
        if (t.half() == net.morni.burgverlies.dungeon.RoomCatalog.HALF) {
            arches(p, t, pal);
        }
        switch (t.id()) {
            case "torkammer" -> torkammer(p, pal);
            case "treppenabsatz" -> treppenabsatz(p, pal);
            case "wachstube" -> wachstube(p, pal);
            case "kerker" -> kerker(p, pal);
            case "weinkeller" -> weinkeller(p, pal);
            case "ruestkammer" -> ruestkammer(p, pal);
            case "gruft" -> gruft(p, pal);
            case "fallengang", "brunnenschacht", "glutkammer" -> trapDecor(p, t, pal);
            case "hebelsaal" -> hebelsaal(p, pal);
            case "siegelkammer" -> siegelkammer(p, pal);
            case "schatzkammer" -> schatzkammer(p, pal);
            case "zehntkeller" -> zehntkeller(p, pal);
            case "wachfeuer" -> wachfeuer(p, pal);
            case "kraemerstube" -> kraemerstube(p, pal);
            case "thronsaal" -> thronsaal(p, t, pal);
            default -> {
                // nur die Hülle
            }
        }
        clearReserved(p, t);
        switch (t.type()) {
            case TRAP -> trap(p, t, pal, crumble);
            case PUZZLE -> puzzle(p, t, pal);
            case TREASURE -> chests(p, t);
            case REST -> p.set(0, 1, 0, "campfire[lit=true]");
            case SHOP -> stairwell(p, pal);
            default -> {
                // keine Spiel-Blöcke
            }
        }
        lights(p, t.half(), t.height());
        p.connect();
        return p;
    }

    // ---------- Hülle ----------

    private static void shell(Plan p, RoomTemplate t, Palette pal, long seed) {
        int h = t.half();
        int height = t.height();
        int w = h + 1;
        for (int x = -w; x <= w; x++) {
            for (int z = -w; z <= w; z++) {
                int n = noise(x, 0, z, seed) % 100;
                p.set(x, 0, z, n < 12 ? pal.floorAlt() : n < 17 ? pal.crack() : pal.floor());
                p.set(x, height + 1, z, noise(x, 9, z, seed) % 100 < 18 ? pal.wallAlt() : pal.wall());
                boolean edge = Math.abs(x) == w || Math.abs(z) == w;
                for (int y = 1; y <= height; y++) {
                    if (!edge) {
                        p.set(x, y, z, "cave_air");
                        continue;
                    }
                    int m = noise(x, y, z, seed) % 100;
                    String spec;
                    if (y == 1) {
                        spec = m < 50 ? pal.floorAlt() : pal.wallAlt();
                    } else if (y == height) {
                        spec = pal.pillar();
                    } else {
                        spec = m < 20 ? pal.wallAlt() : m < 30 ? pal.crack() : pal.wall();
                    }
                    p.set(x, y, z, spec);
                }
            }
        }
        // Säulen in den Ecken und Balken unter der Decke
        for (int sx : new int[] {-h, h}) {
            for (int sz : new int[] {-h, h}) {
                for (int y = 1; y <= height; y++) {
                    p.set(sx, y, sz, y == 1 || y == height ? pal.pillar() : pal.wallBlock());
                }
            }
        }
        for (int z = -h + 3; z <= h - 3; z += 5) {
            for (int x = -h; x <= h; x++) {
                p.set(x, height, z, pal.beam() + "[axis=x]");
            }
        }
        // Spinnweben oben in den Ecken
        for (int sx : new int[] {-1, 1}) {
            for (int sz : new int[] {-1, 1}) {
                p.set(sx * (h - 1), height, sz * h, "cobweb");
                p.set(sx * h, height - 1, sz * (h - 1), "cobweb");
            }
        }
    }

    /**
     * Wandsäulen und Bögen: an jeder Wand zwei Säulen (bei ±4), darüber Bögen aus umgedrehten Treppen und Stufen,
     * unter den Bögen hängen Laternen. Liegt über den Türen (die sind 4 hoch).
     */
    private static void arches(Plan p, RoomTemplate t, Palette pal) {
        int h = t.half();
        int top = t.height() - 1;
        for (Dir wall : Dir.values()) {
            Dir along = wall.rotate(1);
            for (int a = -h + 1; a <= h - 1; a++) {
                int x = wall.dx() * h + along.dx() * a;
                int z = wall.dz() * h + along.dz() * a;
                boolean pillar = Math.abs(a) == 4;
                if (pillar && t.type() == net.morni.burgverlies.dungeon.RoomType.TRAP) {
                    // Im Fallengang nur oben ein Kragstein – unten fliegen Pfeile
                    p.set(x, top, z, pal.pillar());
                    p.set(x, top - 1, z, pal.wallBlock());
                    continue;
                }
                if (pillar) {
                    for (int y = 1; y <= top; y++) {
                        p.set(x, y, z, y == 1 || y == top ? pal.pillar() : pal.wallAlt());
                    }
                    continue;
                }
                // nächster Pfeiler: Ecke (±h) oder Wandsäule (±4)
                int toward = Math.abs(a) > 4 ? Integer.signum(a) : (Math.abs(a) == 3 ? Integer.signum(a) : 0);
                if (Math.abs(a) == h - 1) {
                    toward = Integer.signum(a);
                } else if (Math.abs(a) == 5) {
                    toward = -Integer.signum(a);
                }
                if (toward != 0) {
                    Dir f = toward > 0 ? along : along.opposite();
                    p.set(x, top, z, pal.stairs() + "[half=top,facing=" + name(f) + "]");
                } else {
                    p.set(x, top, z, pal.slab() + "[type=top]");
                }
                if (Math.abs(a) == 2) {
                    p.set(x, top - 1, z, pal.lantern() + "[hanging=true]");
                }
            }
        }
    }

    private static String name(Dir d) {
        return d.name().toLowerCase(Locale.ROOT);
    }

    /** Unsichtbares Licht unter der Decke – gedämpft, damit es unheimlich bleibt; den Rest machen Laternen und Kerzen. */
    private static void lights(Plan p, int h, int height) {
        for (int x = -h + 2; x <= h - 2; x += 4) {
            for (int z = -h + 2; z <= h - 2; z += 4) {
                for (int y = height - 2; y >= 3; y--) {
                    BlockData d = p.get(x, y, z);
                    if (d == null || d.getMaterial() == Material.CAVE_AIR) {
                        p.set(x, y, z, "light[level=" + LIGHT + "]");
                        break;
                    }
                }
            }
        }
    }

    /** Wege, Türen, Gegner-Stellen, Fallen und Rätsel freiräumen (Teppiche dürfen liegen bleiben). */
    private static void clearReserved(Plan p, RoomTemplate t) {
        int h = t.half();
        for (int x = -h; x <= h; x++) {
            for (int z = -h; z <= h; z++) {
                for (int y = 1; y <= t.height(); y++) {
                    if (!RoomSpace.reserved(t, x, y, z)) {
                        continue;
                    }
                    BlockData d = p.get(x, y, z);
                    if (d == null) {
                        continue;
                    }
                    Material m = d.getMaterial();
                    if (m != Material.CAVE_AIR && !m.name().endsWith("_CARPET")) {
                        p.set(x, y, z, "cave_air");
                    }
                }
            }
        }
        p.stands().removeIf(s -> RoomSpace.reserved(t, s.x(), s.y(), s.z()));
    }

    // ---------- kleine Bausteine ----------

    private static void hang(Plan p, String lantern, int x, int z, int height, int length) {
        for (int i = 0; i < length; i++) {
            p.set(x, height - i, z, "iron_chain[axis=y]");
        }
        p.set(x, height - length, z, lantern + "[hanging=true]");
    }

    private static void table(Plan p, int x1, int z1, int x2, int z2) {
        p.fill(x1, 1, z1, x2, 1, z2, "spruce_slab[type=top]");
    }

    private static void candles(Plan p, int x, int y, int z, int n) {
        p.set(x, y, z, "candle[candles=" + n + ",lit=true]");
    }

    private static void skull(Plan p, int x, int y, int z, int rotation) {
        p.set(x, y, z, "skeleton_skull[rotation=" + rotation + "]");
    }

    /** Liegendes Fass (Boden zeigt zur Raummitte). */
    private static void cask(Plan p, int x, int y, int z, Dir face) {
        p.set(x, y, z, "barrel[facing=" + name(face) + "]");
    }

    // ---------- Räume ----------

    private static void torkammer(Plan p, Palette pal) {
        // Großes Fallgitter in der Nordwand, durch das man hereingekommen ist
        p.fill(-2, 1, -7, 2, 4, -7, "iron_bars");
        p.fill(-3, 5, -7, 3, 5, -7, pal.pillar());
        for (int x : new int[] {-3, 3}) {
            p.set(x, 1, -6, pal.wallBlock());
            p.set(x, 2, -6, "campfire[lit=true]");
        }
        for (int x = 3; x <= 6; x++) {
            p.set(x, 1, -7, "red_carpet");
        }
        // Vorräte der Torwache
        cask(p, -7, 1, 6, Dir.EAST);
        cask(p, -7, 2, 6, Dir.EAST);
        cask(p, -7, 1, 5, Dir.EAST);
        p.set(-6, 1, 6, "barrel[facing=up]");
        p.set(-6, 1, 7, "hay_block[axis=y]");
        p.set(-7, 1, 7, "hay_block[axis=y]");
        p.set(7, 1, 6, "barrel[facing=up]");
        p.set(7, 2, 6, "barrel[facing=up]");
        p.set(6, 1, 7, "spruce_planks");
        p.set(6, 2, 7, "spruce_slab[type=bottom]");
        p.set(7, 1, 5, "grindstone[face=floor,facing=west]");
        // Banner der Burg und Wappenwand
        for (int z : new int[] {-3, 3}) {
            p.set(-7, 3, z, "red_wall_banner[facing=east]");
            p.set(7, 3, z, "red_wall_banner[facing=west]");
        }
        hang(p, pal.lantern(), 0, 0, 6, 2);
        hang(p, pal.lantern(), -4, 4, 6, 1);
        hang(p, pal.lantern(), 4, 4, 6, 1);
    }

    private static void treppenabsatz(Plan p, Palette pal) {
        // Treppe, die aus der Decke kommt (von der Etage darüber), mit Geländer
        for (int i = 0; i < 5; i++) {
            int x = -6 + i;
            for (int y = 1; y <= 5 - i; y++) {
                p.set(x, y, -7, pal.wall());
            }
            p.set(x, 6 - i, -7, pal.stairs() + "[facing=west]");
            p.set(x, 6 - i, -6, pal.wallBlock());
        }
        p.set(5, 1, -7, "barrel[facing=up]");
        p.set(6, 1, -7, "barrel[facing=up]");
        p.set(6, 2, -7, "barrel[facing=up]");
        p.set(6, 1, -6, "spruce_planks");
        candles(p, 6, 2, -6, 3);
        // Knochen und Ketten – hier unten wird es ungemütlich
        p.set(-7, 1, 6, "bone_block[axis=y]");
        skull(p, -7, 2, 6, 6);
        p.set(7, 1, 6, "bone_block[axis=x]");
        skull(p, 6, 1, 7, 10);
        for (int y = 3; y <= 6; y++) {
            p.set(7, y, 3, "iron_chain[axis=y]");
            p.set(-7, y, 3, "iron_chain[axis=y]");
        }
        candles(p, -6, 1, 3, 2);
        hang(p, pal.lantern(), 0, 0, 6, 2);
        hang(p, pal.lantern(), 4, -4, 6, 1);
    }

    private static void wachstube(Plan p, Palette pal) {
        // Tisch der Wache mit Bänken, Würfeln (Kerzen) und Krügen
        table(p, -5, -5, -3, -4);
        for (int x = -5; x <= -3; x++) {
            p.set(x, 1, -6, "spruce_stairs[facing=south]");
            p.set(x, 1, -3, "spruce_stairs[facing=north]");
        }
        candles(p, -4, 2, -5, 3);
        p.set(-5, 2, -4, "flower_pot");
        p.set(-3, 2, -5, "brewing_stand");
        // Stockbetten an der Ostwand
        for (int z : new int[] {-6, -3}) {
            p.set(7, 1, z, "red_bed[facing=east,part=head]");
            p.set(6, 1, z, "red_bed[facing=east,part=foot]");
            p.set(7, 2, z, "spruce_slab[type=top]");
            p.set(6, 2, z, "spruce_slab[type=top]");
            p.set(7, 3, z, "blue_bed[facing=east,part=head]");
            p.set(6, 3, z, "blue_bed[facing=east,part=foot]");
            p.set(5, 1, z, "spruce_fence");
            p.set(5, 2, z, "spruce_fence");
            p.set(5, 3, z, "spruce_fence");
        }
        p.set(7, 1, -5, "barrel[facing=up]");
        p.set(7, 1, -4, "barrel[facing=up]");
        // Waffenständer an der Westwand: Rüstungen und Speere
        p.stand(-6, 1, 5, Dir.EAST, "iron");
        p.stand(-6, 1, 3, Dir.EAST, "chain");
        for (int z = 2; z <= 6; z++) {
            p.set(-7, 1, z, "spruce_planks");
            p.set(-7, 2, z, z % 2 == 0 ? "lightning_rod[facing=up]" : "spruce_fence");
        }
        p.set(-7, 3, 4, "red_wall_banner[facing=east]");
        // Vorräte
        p.set(6, 1, 6, "barrel[facing=up]");
        p.set(7, 1, 6, "barrel[facing=up]");
        p.set(7, 2, 6, "barrel[facing=up]");
        p.set(6, 1, 7, "hay_block[axis=y]");
        p.set(7, 1, 3, "smithing_table");
        p.set(7, 3, 2, "red_wall_banner[facing=west]");
        hang(p, pal.lantern(), 0, 0, 6, 1);
        hang(p, pal.lantern(), -4, -4, 6, 2);
        hang(p, pal.lantern(), 4, 4, 6, 2);
    }

    private static void kerker(Plan p, Palette pal) {
        for (int s : new int[] {-1, 1}) {
            int front = 4 * s;
            // Gitter mit offener Zellentür (bei z = ±3)
            for (int z : new int[] {-7, -6, -5, -4, -2, 2, 4, 5, 6, 7}) {
                p.fill(front, 1, z, front, 4, z, "iron_bars");
            }
            p.set(front, 4, -3, "iron_bars");
            p.set(front, 4, 3, "iron_bars");
            for (int x = 5; x <= 7; x++) {
                p.fill(x * s, 1, -2, x * s, 4, -2, "iron_bars");
                p.fill(x * s, 1, 2, x * s, 4, 2, "iron_bars");
            }
            // Stroh, Fesseln, Eimer, Knochen in jeder Zelle
            for (int sz : new int[] {-1, 1}) {
                p.set(7 * s, 1, 6 * sz, "hay_block[axis=y]");
                p.set(6 * s, 1, 6 * sz, "brown_carpet");
                p.set(7 * s, 1, 5 * sz, "brown_carpet");
                p.set(6 * s, 1, 7 * sz, "brown_carpet");
                p.set(7 * s, 3, 4 * sz, "iron_chain[axis=y]");
                p.set(7 * s, 2, 4 * sz, "iron_chain[axis=y]");
                p.set(7 * s, 1, 3 * sz, "cauldron");
                skull(p, 6 * s, 1, 3 * sz, s > 0 ? 12 : 4);
                p.set(5 * s, 1, 7 * sz, "bone_block[axis=x]");
            }
            // Ketten von der Decke im Gang
            for (int y = 4; y <= 6; y++) {
                p.set(2 * s, y, -5, "iron_chain[axis=y]");
                p.set(2 * s, y, 5, "iron_chain[axis=y]");
            }
        }
        // Tisch des Kerkermeisters mit Schlüsselbund (Haken) und Kerze
        table(p, -3, -6, -2, -6);
        p.set(-3, 1, -7, "spruce_stairs[facing=south]");
        candles(p, -2, 2, -6, 1);
        p.set(-3, 2, -6, "tripwire_hook[facing=south]");
        p.set(3, 1, 6, "barrel[facing=up]");
        hang(p, pal.lantern(), 0, -3, 6, 2);
        hang(p, pal.lantern(), 0, 3, 6, 2);
    }

    private static void weinkeller(Plan p, Palette pal) {
        // Fassregale an beiden Längswänden: zwei Lagen liegende Fässer, oben ein Brett
        for (int s : new int[] {-1, 1}) {
            Dir face = s < 0 ? Dir.EAST : Dir.WEST;
            for (int[] range : new int[][] {{-6, -2}, {2, 6}}) {
                for (int z = range[0]; z <= range[1]; z++) {
                    cask(p, 7 * s, 1, z, face);
                    cask(p, 7 * s, 2, z, face);
                    p.set(7 * s, 3, z, "spruce_slab[type=bottom]");
                }
                p.set(6 * s, 1, range[0], "spruce_log[axis=y]");
                p.set(6 * s, 1, range[1], "spruce_log[axis=y]");
                p.set(6 * s, 2, range[0], "spruce_fence");
                p.set(6 * s, 2, range[1], "spruce_fence");
            }
        }
        // Weinregale an Nord- und Südwand: Bretter mit Flaschen (Brauständen) und Krügen
        for (int sz : new int[] {-1, 1}) {
            for (int x = 2; x <= 5; x++) {
                for (int sx : new int[] {-1, 1}) {
                    int wx = x * sx;
                    p.set(wx, 1, 7 * sz, "spruce_planks");
                    p.set(wx, 2, 7 * sz, x % 2 == 0 ? "brewing_stand" : "decorated_pot");
                    p.set(wx, 3, 7 * sz, "spruce_slab[type=top]");
                    p.set(wx, 4, 7 * sz, x % 2 == 0 ? "flower_pot" : "candle[candles=1,lit=true]");
                }
            }
        }
        // Großes Fass mit Zapfhahn in den Ecken
        for (int sx : new int[] {-1, 1}) {
            for (int sz : new int[] {-1, 1}) {
                p.set(6 * sx, 1, 6 * sz, "barrel[facing=up]");
                p.set(6 * sx, 2, 6 * sz, "barrel[facing=up]");
                p.set(6 * sx, 3, 6 * sz, "spruce_slab[type=bottom]");
            }
        }
        // Holzboden im Gang, Probiertisch
        p.fill(-2, 0, -6, 2, 0, 6, "spruce_planks");
        table(p, 3, -5, 4, -5);
        candles(p, 3, 2, -5, 2);
        p.set(4, 2, -5, "brewing_stand");
        p.set(-4, 1, 5, "hay_block[axis=x]");
        hang(p, pal.lantern(), 0, -4, 6, 2);
        hang(p, pal.lantern(), 0, 4, 6, 2);
    }

    private static void ruestkammer(Plan p, Palette pal) {
        // Schmiede an der Nordwand
        p.set(-6, 1, -7, "anvil[facing=east]");
        p.set(-5, 1, -7, "smithing_table");
        p.set(-3, 1, -7, "grindstone[face=floor,facing=south]");
        p.set(-7, 1, -6, "blast_furnace[facing=east]");
        p.set(-7, 1, -5, "anvil[facing=north]");
        p.set(5, 1, -7, "stonecutter[facing=south]");
        p.set(6, 1, -7, "cauldron");
        p.set(7, 1, -6, "barrel[facing=up]");
        // Rüstungen in Reih und Glied
        p.stand(-6, 1, 3, Dir.EAST, "iron");
        p.stand(-6, 1, 5, Dir.EAST, "chain");
        p.stand(-6, 1, 7, Dir.EAST, "gold");
        p.stand(6, 1, -4, Dir.WEST, "diamond");
        p.stand(6, 1, -2, Dir.WEST, "iron");
        // Speer- und Schwertständer an der Ostwand
        for (int z = 3; z <= 6; z++) {
            p.set(7, 1, z, "spruce_planks");
            p.set(7, 2, z, z % 2 == 0 ? "lightning_rod[facing=up]" : "spruce_fence");
            p.set(7, 3, z, "spruce_slab[type=bottom]");
        }
        p.set(6, 1, 6, "barrel[facing=up]");
        p.set(5, 1, 7, "barrel[facing=up]");
        // Wappen und Schilde
        for (int z : new int[] {-3, 3}) {
            p.set(-7, 3, z, "red_wall_banner[facing=east]");
        }
        p.set(7, 4, -3, "red_wall_banner[facing=west]");
        p.set(7, 4, 3, "yellow_wall_banner[facing=west]");
        hang(p, pal.lantern(), 0, 0, 6, 1);
        hang(p, pal.lantern(), -4, -4, 6, 2);
        hang(p, pal.lantern(), 4, 4, 6, 2);
    }

    private static void gruft(Plan p, Palette pal) {
        // Vier Steinsärge mit Deckel, Totenkopf und Kerzen
        for (int sx : new int[] {-1, 1}) {
            for (int sz : new int[] {-1, 1}) {
                for (int i = 5; i <= 7; i++) {
                    p.set(i * sx, 1, 6 * sz, "polished_andesite");
                    p.set(i * sx, 2, 6 * sz, pal.slab() + "[type=bottom]");
                }
                skull(p, 7 * sx, 3, 6 * sz, sx > 0 ? 12 : 4);
                candles(p, 5 * sx, 3, 6 * sz, 3);
                p.set(6 * sx, 1, 7 * sz, "candle[candles=4,lit=true]");
            }
        }
        // Knochennischen und Kerzen an den Wänden
        for (int s : new int[] {-1, 1}) {
            p.set(7 * s, 1, 2, "bone_block[axis=y]");
            skull(p, 7 * s, 2, 2, s > 0 ? 12 : 4);
            p.set(7 * s, 1, -2, "bone_block[axis=y]");
            candles(p, 7 * s, 2, -2, 4);
            p.set(3 * s, 1, -7, pal.pillar());
            skull(p, 3 * s, 2, -7, 8);
            p.set(3 * s, 1, 7, pal.pillar());
            candles(p, 3 * s, 2, 7, 2);
        }
        // Verziertes Grabmal in der Mitte des Bodens (nur Boden, man läuft darüber)
        for (int x = -2; x <= 2; x++) {
            p.set(x, 0, -2, pal.pillar());
            p.set(x, 0, 2, pal.pillar());
            p.set(-2, 0, x, pal.pillar());
            p.set(2, 0, x, pal.pillar());
        }
        p.set(0, 0, 0, "chiseled_tuff");
        hang(p, "soul_lantern", -3, 0, 6, 2);
        hang(p, "soul_lantern", 3, 0, 6, 2);
        hang(p, pal.lantern(), 0, -4, 6, 1);
    }

    /** Fallengänge: nur Wände, Decke und die Ecken – im Gang muss alles frei bleiben. */
    private static void trapDecor(Plan p, RoomTemplate t, Palette pal) {
        int h = t.half();
        for (int x = -4; x <= 4; x += 4) {
            for (int z = -4; z <= 4; z += 4) {
                p.set(x, t.height(), z, "iron_chain[axis=y]");
                p.set(x, t.height() - 1, z, "iron_chain[axis=y]");
            }
        }
        for (int s : new int[] {-1, 1}) {
            p.set(6 * s, 1, -h, "bone_block[axis=y]");
            skull(p, 6 * s, 2, -h, 0);
            p.set(5 * s, 1, h, "bone_block[axis=x]");
            candles(p, 6 * s, 1, h, 3);
        }
        switch (t.id()) {
            case "glutkammer" -> {
                for (int z = -5; z <= 5; z += 2) {
                    p.set(-h - 1, 3, z, "magma_block");
                    p.set(h + 1, 3, z, "magma_block");
                }
                for (int s : new int[] {-1, 1}) {
                    p.set(5 * s, 1, -h, "soul_campfire[lit=true]");
                }
            }
            case "brunnenschacht" -> {
                p.set(-5, 1, -h, "water_cauldron[level=3]");
                p.set(5, 1, h, "cauldron");
                for (int y = 4; y <= t.height(); y++) {
                    p.set(-h, y, 0, "iron_chain[axis=y]");
                    p.set(h, y, 0, "iron_chain[axis=y]");
                }
            }
            default -> {
                for (int s : new int[] {-1, 1}) {
                    p.set(5 * s, 1, -h, "skeleton_skull[rotation=0]");
                }
            }
        }
    }

    private static void trap(Plan p, RoomTemplate t, Palette pal, TrapLayout crumble) {
        RoomTemplate.Trap trap = t.trap();
        int h = t.half();
        for (RoomTemplate.Emitter e : trap.arrows()) {
            int wx = e.at().x() < 0 ? -h - 1 : h + 1;
            p.set(wx, e.at().y(), e.at().z(), "dispenser[facing=" + name(e.shoots()) + "]");
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
        }
        // Ziel: verzierter Rand
        for (int x = -h; x <= h; x++) {
            p.set(x, 0, t.goalZ(), "chiseled_stone_bricks");
        }
    }

    private static void hebelsaal(Plan p, Palette pal) {
        // Bücherwände links und rechts, Leseplatz
        for (int s : new int[] {-1, 1}) {
            for (int z = -6; z <= -2; z++) {
                for (int y = 1; y <= 3; y++) {
                    p.set(7 * s, y, z, "bookshelf");
                }
            }
            for (int z = 2; z <= 3; z++) {
                for (int y = 1; y <= 3; y++) {
                    p.set(7 * s, y, z, "bookshelf");
                }
            }
            candles(p, 7 * s, 4, -4, 3);
        }
        table(p, 5, 6, 6, 6);
        candles(p, 5, 2, 6, 2);
        p.set(6, 2, 6, "flower_pot");
        p.set(6, 1, 7, "spruce_stairs[facing=north]");
        p.set(-6, 1, 6, "cartography_table");
        p.set(-6, 1, 7, "barrel[facing=up]");
        // Zeichen im Boden rund um die Glocke
        for (int x = -2; x <= 2; x++) {
            p.set(x, 0, -6, pal.pillar());
            p.set(x, 0, -2, pal.pillar());
        }
        hang(p, pal.lantern(), -4, 0, 6, 2);
        hang(p, pal.lantern(), 4, 0, 6, 2);
    }

    private static void siegelkammer(Plan p, Palette pal) {
        // Kreis aus verzierten Steinen auf dem Boden, Kerzen auf Sockeln
        for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 4}, {3, 4}, {-5, 0}, {5, 0}}) {
            p.set(c[0], 0, c[1], pal.pillar());
        }
        for (int[] c : new int[][] {{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            p.set(c[0], 1, c[1], pal.pillar());
            p.set(c[0], 2, c[1], pal.wallBlock());
            candles(p, c[0], 3, c[1], 4);
        }
        for (int s : new int[] {-1, 1}) {
            for (int z : new int[] {-3, 3}) {
                p.set(7 * s, 3, z, (z < 0 ? "purple" : "blue") + "_wall_banner[facing=" + (s < 0 ? "east" : "west") + "]");
            }
            p.set(7 * s, 1, 6, "decorated_pot");
        }
        hang(p, "soul_lantern", -3, 0, 6, 2);
        hang(p, "soul_lantern", 3, 0, 6, 2);
        hang(p, pal.lantern(), 0, 2, 6, 1);
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
    }

    private static void schatzkammer(Plan p, Palette pal) {
        // Goldhaufen neben den Truhen und in den Ecken
        for (int sx : new int[] {-1, 1}) {
            for (int sz : new int[] {-1, 1}) {
                p.set(7 * sx, 1, 6 * sz, "gold_block");
                p.set(7 * sx, 2, 6 * sz, "raw_gold_block");
                p.set(6 * sx, 1, 7 * sz, "raw_gold_block");
                p.set(6 * sx, 1, 6 * sz, "gold_block");
                p.set(7 * sx, 1, 5 * sz, "raw_gold_block");
                p.set(6 * sx, 2, 6 * sz, "candle[candles=3,lit=true]");
                p.set(7 * sx, 1, 3 * sz, "yellow_carpet");
                p.set(6 * sx, 1, 2 * sz, "yellow_carpet");
            }
            // Sockel mit Schmuck
            p.set(3 * sx, 1, -7, pal.pillar());
            p.set(3 * sx, 2, -7, "amethyst_cluster[facing=up]");
            p.set(3 * sx, 1, 7, pal.pillar());
            p.set(3 * sx, 2, 7, "decorated_pot");
            p.set(7 * sx, 3, 0, "yellow_wall_banner[facing=" + (sx < 0 ? "east" : "west") + "]");
        }
        // Roter Teppich mit Goldrand
        for (int z = -6; z <= 6; z++) {
            p.set(0, 1, z, "red_carpet");
            p.set(-1, 1, z, "yellow_carpet");
            p.set(1, 1, z, "yellow_carpet");
        }
        p.set(0, 0, 0, "gold_block");
        hang(p, pal.lantern(), 0, 0, 6, 2);
        hang(p, pal.lantern(), -4, -4, 6, 1);
        hang(p, pal.lantern(), 4, 4, 6, 1);
    }

    private static void zehntkeller(Plan p, Palette pal) {
        // Säcke (Wolle, Heu), Kisten, Fässer: der Zehnt der Bauern
        for (int s : new int[] {-1, 1}) {
            for (int z = -6; z <= -3; z++) {
                p.set(7 * s, 1, z, z % 2 == 0 ? "brown_wool" : "hay_block[axis=y]");
                if (z <= -5) {
                    p.set(7 * s, 2, z, "brown_wool");
                }
            }
            p.set(6 * s, 1, -6, "hay_block[axis=x]");
            for (int z = 2; z <= 3; z++) {
                p.set(7 * s, 1, z, "barrel[facing=up]");
                p.set(7 * s, 2, z, "barrel[facing=up]");
            }
            p.set(6 * s, 1, 7, "spruce_planks");
            p.set(6 * s, 2, 7, "spruce_trapdoor[half=bottom,open=false,facing=north]");
            p.set(7 * s, 1, 6, "composter");
        }
        p.set(3, 1, -7, "barrel[facing=up]");
        p.set(-3, 1, -7, "barrel[facing=up]");
        p.set(5, 1, -7, "loom[facing=south]");
        hang(p, pal.lantern(), 0, 0, 6, 2);
        hang(p, pal.lantern(), 4, -4, 6, 1);
    }

    private static void wachfeuer(Plan p, Palette pal) {
        // Steinring ums Feuer, Bänke aus Stämmen
        for (int[] c : new int[][] {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}}) {
            p.set(c[0], 0, c[1], "cobblestone");
        }
        p.set(0, 0, 0, "cobblestone");
        for (int s : new int[] {-1, 1}) {
            p.set(3 * s, 1, 2, "spruce_log[axis=z]");
            p.set(3 * s, 1, 3, "spruce_log[axis=z]");
            p.set(3 * s, 1, -2, "spruce_log[axis=z]");
            p.set(3 * s, 1, -3, "spruce_log[axis=z]");
            // Schlaflager
            p.set(7 * s, 1, -6, "white_bed[facing=north,part=head]");
            p.set(7 * s, 1, -5, "white_bed[facing=north,part=foot]");
            p.set(7 * s, 1, 6, "white_bed[facing=south,part=head]");
            p.set(7 * s, 1, 5, "white_bed[facing=south,part=foot]");
            p.set(6 * s, 1, -6, "white_carpet");
            p.set(6 * s, 1, 6, "white_carpet");
        }
        p.set(-7, 1, 2, "smoker[facing=east]");
        p.set(-7, 1, 3, "barrel[facing=up]");
        p.set(7, 1, 2, "crafting_table");
        p.set(7, 1, 3, "hay_block[axis=y]");
        p.set(-3, 1, -7, "flower_pot");
        p.set(3, 1, -7, "barrel[facing=up]");
        candles(p, 3, 2, -7, 2);
        hang(p, pal.lantern(), -4, -4, 6, 1);
        hang(p, pal.lantern(), 4, 4, 6, 1);
        hang(p, pal.lantern(), 4, -4, 6, 1);
        hang(p, pal.lantern(), -4, 4, 6, 1);
    }

    private static void kraemerstube(Plan p, Palette pal) {
        // Theke vor der Krämerin mit Waren
        for (int z = -4; z <= 0; z++) {
            p.set(-3, 1, z, "spruce_slab[type=top]");
        }
        p.set(-3, 2, -4, "brewing_stand");
        candles(p, -3, 2, -3, 2);
        p.set(-3, 2, 0, "flower_pot");
        // Regale hinter ihr
        for (int z = -6; z <= 2; z++) {
            if (z == -4) {
                continue;
            }
            p.set(-7, 1, z, "barrel[facing=east]");
            p.set(-7, 2, z, z % 2 == 0 ? "bookshelf" : "barrel[facing=east]");
            p.set(-7, 3, z, "spruce_slab[type=bottom]");
        }
        p.set(-6, 1, -6, "decorated_pot");
        p.set(-6, 1, 1, "decorated_pot");
        p.set(-7, 4, -2, "green_wall_banner[facing=east]");
        // Waren im Raum: Kisten, Fässer, Webstuhl, Kartentisch
        p.set(5, 1, -7, "barrel[facing=up]");
        p.set(6, 1, -7, "barrel[facing=up]");
        p.set(6, 2, -7, "barrel[facing=up]");
        p.set(7, 1, -6, "loom[facing=west]");
        p.set(7, 1, -5, "cartography_table");
        p.set(7, 1, -3, "spruce_planks");
        p.set(7, 2, -3, "flower_pot");
        p.set(6, 1, 2, "hay_block[axis=y]");
        p.set(7, 1, 2, "barrel[facing=up]");
        for (int x = 3; x <= 5; x++) {
            p.set(x, 1, -5, "red_carpet");
        }
        hang(p, pal.lantern(), -2, -2, 6, 1);
        hang(p, pal.lantern(), 4, -2, 6, 2);
    }

    /** Treppe hinab zur nächsten Etage: eine Stufe pro Reihe, dahinter wird es dunkel. */
    private static void stairwell(Plan p, Palette pal) {
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
    }

    private static void chests(Plan p, RoomTemplate t) {
        for (Pos c : t.chests()) {
            String facing = Math.abs(c.x()) >= Math.abs(c.z()) ? (c.x() < 0 ? "east" : "west")
                    : (c.z() < 0 ? "south" : "north");
            p.set(c.x(), c.y(), c.z(), "chest[facing=" + facing + "]");
        }
    }

    private static void thronsaal(Plan p, RoomTemplate t, Palette pal) {
        int h = t.half();
        int top = t.height();
        // Vier große Säulen mit Ketten und Laternen
        for (int sx : new int[] {-7, 6}) {
            for (int sz : new int[] {-7, 6}) {
                p.fill(sx, 1, sz, sx + 1, top, sz + 1, pal.pillar());
                p.fill(sx, 1, sz, sx + 1, 1, sz + 1, "gilded_blackstone");
                for (int y = top - 3; y <= top; y++) {
                    p.set(sx, y, sz - 1, "iron_chain[axis=y]");
                }
                p.set(sx, top - 4, sz - 1, pal.lantern() + "[hanging=true]");
            }
        }
        // Säulenreihen an den Langseiten mit Feuerschalen
        for (int s : new int[] {-1, 1}) {
            for (int z = -8; z <= 8; z += 4) {
                if (z == 0) {
                    continue;
                }
                p.fill(10 * s, 1, z, 10 * s, top, z, pal.wall());
                p.set(10 * s, 1, z, pal.pillar());
                p.set(10 * s, top, z, pal.pillar());
                p.set(11 * s, 1, z, pal.pillar());
                p.set(11 * s, 2, z, "soul_campfire[lit=true]");
            }
            // Banner zwischen den Säulen
            for (int z : new int[] {-6, -2, 2, 6}) {
                p.set(h * s, 6, z, "red_wall_banner[facing=" + (s < 0 ? "east" : "west") + "]");
                p.set(h * s, 5, z, "red_wall_banner[facing=" + (s < 0 ? "east" : "west") + "]");
            }
            // Käfige oben an den Seiten
            int x = (h - 1) * s;
            p.fill(x, 6, -1, x, 8, -1, "iron_bars");
            p.fill(x, 6, 1, x, 8, 1, "iron_bars");
            p.set(x, 5, 0, pal.slab() + "[type=top]");
            p.set(x, 9, 0, "iron_chain[axis=y]");
            p.set(x, 10, 0, "iron_chain[axis=y]");
        }
        // Thron im Süden auf einem Podest
        p.fill(-3, 1, h - 2, 3, 1, h, pal.wall());
        for (int x = -3; x <= 3; x++) {
            p.set(x, 1, h - 3, pal.stairs() + "[facing=north]");
        }
        p.set(0, 2, h - 1, pal.stairs() + "[facing=north]");
        p.fill(-1, 2, h, 1, 5, h, "chiseled_polished_blackstone");
        p.set(0, 6, h, "gilded_blackstone");
        p.set(-1, 6, h, pal.stairs() + "[facing=east,half=top]");
        p.set(1, 6, h, pal.stairs() + "[facing=west,half=top]");
        p.set(-1, 2, h - 1, "gilded_blackstone");
        p.set(1, 2, h - 1, "gilded_blackstone");
        p.set(-3, 2, h - 1, "soul_campfire[lit=true]");
        p.set(3, 2, h - 1, "soul_campfire[lit=true]");
        p.set(-2, 3, h, "red_wall_banner[facing=north]");
        p.set(2, 3, h, "red_wall_banner[facing=north]");
        // Roter Läufer mit Goldrand vom Eingang zum Thron
        for (int z = -h; z <= h - 4; z++) {
            p.set(0, 1, z, "red_carpet");
            p.set(-1, 1, z, "red_carpet");
            p.set(1, 1, z, "red_carpet");
            p.set(-2, 1, z, "yellow_carpet");
            p.set(2, 1, z, "yellow_carpet");
        }
        // Kronleuchter
        for (int[] c : new int[][] {{-4, -4}, {4, -4}, {-4, 3}, {4, 3}}) {
            for (int y = top - 2; y <= top; y++) {
                p.set(c[0], y, c[1], "iron_chain[axis=y]");
            }
            p.set(c[0], top - 3, c[1], "soul_lantern[hanging=true]");
        }
        // Knochen und Kerzen in den Ecken
        for (int sx : new int[] {-1, 1}) {
            for (int sz : new int[] {-1, 1}) {
                p.set(11 * sx, 1, 11 * sz, "bone_block[axis=y]");
                skull(p, 11 * sx, 2, 11 * sz, sx > 0 ? 12 : 4);
                candles(p, 10 * sx, 1, 11 * sz, 4);
            }
        }
        for (int z = -h + 2; z <= h - 2; z += 4) {
            p.set(-h, 3, z, "soul_lantern");
            p.set(h, 3, z, "soul_lantern");
        }
    }
}
