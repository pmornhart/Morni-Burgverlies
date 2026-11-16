package net.morni.burgverlies.world;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Das Torhaus: ein Burghof auf einer schwebenden Insel. Im Norden die Burgmauer mit dem dunklen Abstieg ins
 * Verlies (Fallgitter, Feuerschalen), im Süden das Tor, durch das man kommt. Eigener Entwurf.
 */
public final class HallPlan {

    public static final int FLOOR_Y = 100;
    public static final int RADIUS = 17;
    /** Vor dem Abstieg: wer hier hineinläuft, bekommt das Startmenü. */
    public static final int[] DESCENT_ZONE = {-2, -12, 2, -9};

    private HallPlan() {
    }

    public static Location spawn(World world) {
        return new Location(world, 0.5, FLOOR_Y + 1, 9.5, 180f, 0f);
    }

    public static Plan build() {
        Plan p = new Plan();
        island(p);
        keep(p);
        gate(p);
        yard(p);
        p.connect();
        return p;
    }

    private static void island(Plan p) {
        for (int x = -RADIUS - 2; x <= RADIUS + 2; x++) {
            for (int z = -RADIUS - 2; z <= RADIUS + 2; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > RADIUS + 1.5) {
                    continue;
                }
                int n = RoomPlans.noise(x, 0, z, 7) % 100;
                String top;
                if (Math.abs(x) <= 2 || Math.abs(z) <= 1 && Math.abs(x) <= 10) {
                    top = n < 30 ? "cobblestone" : n < 40 ? "mossy_cobblestone" : "stone_bricks";
                } else if (r < 9) {
                    top = n < 20 ? "cobblestone" : n < 30 ? "andesite" : "stone_bricks";
                } else {
                    top = n < 8 ? "coarse_dirt" : n < 14 ? "moss_block" : "grass_block";
                }
                p.set(x, 0, z, top);
                int depth = (int) Math.max(1, (RADIUS + 2 - r) * 0.7 + n % 3);
                for (int y = -1; y >= -depth; y--) {
                    p.set(x, y, z, y > -3 ? "dirt" : n % 4 == 0 ? "andesite" : "stone");
                }
            }
        }
    }

    /** Burgmauer im Norden mit Türmen und dem Abstieg. */
    private static void keep(Plan p) {
        for (int x = -15; x <= 15; x++) {
            for (int z = -16; z <= -12; z++) {
                for (int y = 1; y <= 10; y++) {
                    boolean shell = z == -12 || z == -16 || Math.abs(x) == 15 || y == 10;
                    if (shell) {
                        int n = RoomPlans.noise(x, y, z, 11) % 100;
                        p.set(x, y, z, n < 15 ? "mossy_stone_bricks" : n < 22 ? "cracked_stone_bricks" : "stone_bricks");
                    }
                }
                p.set(x, 0, z, "stone_bricks");
            }
            if (x % 2 == 0) {
                p.set(x, 11, -12, "stone_bricks");
                p.set(x, 11, -16, "stone_bricks");
            }
        }
        // Türme
        for (int s : new int[] {-1, 1}) {
            int cx = 13 * s;
            for (int x = cx - 3; x <= cx + 3; x++) {
                for (int z = -15; z <= -9; z++) {
                    boolean edge = Math.abs(x - cx) == 3 || z == -15 || z == -9;
                    for (int y = 1; y <= 15; y++) {
                        if (edge) {
                            p.set(x, y, z, (y + x + z) % 7 == 0 ? "mossy_stone_bricks" : "stone_bricks");
                        }
                    }
                    p.set(x, 15, z, "stone_bricks");
                    if (edge && (x + z) % 2 == 0) {
                        p.set(x, 16, z, "stone_bricks");
                    }
                }
            }
            for (int y = 4; y <= 12; y += 4) {
                p.set(cx, y, -9, "iron_bars");
                p.set(cx, y + 1, -9, "iron_bars");
            }
            p.set(cx, 17, -12, "spruce_fence");
            p.set(cx, 18, -12, "red_wool");
            p.set(cx + s, 18, -12, "red_wool");
            p.set(cx + s, 17, -12, "yellow_wool");
        }
        // Der Abstieg: Torbogen, halb hochgezogenes Fallgitter, Treppe ins Dunkel
        for (int x = -2; x <= 2; x++) {
            for (int y = 1; y <= 5; y++) {
                p.set(x, y, -12, "cave_air");
            }
            p.set(x, 6, -12, "chiseled_stone_bricks");
            p.set(x, 5, -12, "iron_bars");
            for (int i = 0; i < 4; i++) {
                int z = -13 - i;
                p.set(x, -i, z, "stone_brick_stairs[facing=south]");
                for (int y = 1 - i; y <= 5; y++) {
                    p.set(x, y, z, "cave_air");
                }
                p.set(x, -i - 1, z, "stone_bricks");
            }
        }
        p.fill(-2, -4, -17, 2, 6, -17, "black_concrete");
        p.fill(-3, -4, -16, -3, 6, -13, "stone_bricks");
        p.fill(3, -4, -16, 3, 6, -13, "stone_bricks");
        p.fill(-2, 6, -16, 2, 6, -13, "stone_bricks");
        p.set(0, 5, -15, "soul_lantern[hanging=true]");
        for (int s : new int[] {-1, 1}) {
            p.fill(3 * s, 1, -12, 3 * s, 6, -12, "chiseled_stone_bricks");
            p.set(4 * s, 1, -10, "cobblestone_wall");
            p.set(4 * s, 2, -10, "campfire[lit=true]");
            p.set(6 * s, 4, -11, "red_wall_banner[facing=south]");
            p.set(6 * s, 3, -11, "red_wall_banner[facing=south]");
            p.set(3 * s, 7, -11, "lantern[hanging=false]");
            p.set(3 * s, 6, -11, "stone_brick_slab[type=top]");
        }
        p.set(0, 7, -11, "chiseled_stone_bricks");
    }

    /** Tor im Süden, durch das man hereinkommt. */
    private static void gate(Plan p) {
        for (int x = -6; x <= 6; x++) {
            for (int z = 15; z <= 16; z++) {
                for (int y = 1; y <= 7; y++) {
                    boolean opening = Math.abs(x) <= 2 && y <= 4;
                    p.set(x, y, z, opening ? (z == 16 ? "iron_bars" : "cave_air")
                            : (x + y) % 6 == 0 ? "mossy_stone_bricks" : "stone_bricks");
                }
                p.set(x, 0, z, "stone_bricks");
                if (x % 2 == 0) {
                    p.set(x, 8, z, "stone_bricks");
                }
            }
        }
        for (int s : new int[] {-1, 1}) {
            p.set(3 * s, 3, 14, "lantern");
            p.set(3 * s, 2, 14, "cobblestone_wall");
            p.set(3 * s, 1, 14, "cobblestone_wall");
        }
    }

    /** Hof: Brunnen, Bänke, Fässer, Bäume, Laternen. */
    private static void yard(Plan p) {
        // Brunnen
        int wx = -9;
        int wz = 2;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                boolean mid = x == 0 && z == 0;
                p.set(wx + x, 1, wz + z, mid ? "water" : "mossy_cobblestone");
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    p.set(wx + x, 2, wz + z, "spruce_fence");
                    p.set(wx + x, 3, wz + z, "spruce_fence");
                }
            }
        }
        p.fill(wx - 1, 4, wz - 1, wx + 1, 4, wz + 1, "spruce_slab[type=bottom]");
        p.set(wx, 3, wz, "iron_chain[axis=y]");
        // Bänke und Tisch
        for (int x = 7; x <= 10; x++) {
            p.set(x, 1, 1, "spruce_stairs[facing=south]");
            p.set(x, 1, 4, "spruce_stairs[facing=north]");
            p.set(x, 1, 2, "spruce_slab[type=top]");
            p.set(x, 1, 3, "spruce_slab[type=top]");
        }
        p.set(8, 2, 2, "candle[candles=3,lit=true]");
        // Fässer und Heu
        p.set(12, 1, -6, "barrel[facing=up]");
        p.set(12, 2, -6, "barrel[facing=up]");
        p.set(11, 1, -6, "barrel[facing=up]");
        p.set(-12, 1, -6, "hay_block");
        p.set(-12, 2, -6, "hay_block");
        p.set(-11, 1, -6, "hay_block");
        // Laternen-Pfosten am Weg
        for (int z = -6; z <= 12; z += 6) {
            for (int s : new int[] {-1, 1}) {
                p.set(4 * s, 1, z, "spruce_fence");
                p.set(4 * s, 2, z, "spruce_fence");
                p.set(4 * s, 3, z, "lantern");
            }
        }
        // Zwei Bäume
        tree(p, -11, 9);
        tree(p, 11, 10);
    }

    private static void tree(Plan p, int x, int z) {
        for (int y = 1; y <= 5; y++) {
            p.set(x, y, z, "oak_log[axis=y]");
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int y = 4; y <= 7; y++) {
                    int r = Math.abs(dx) + Math.abs(dz) + (y == 7 ? 2 : 0);
                    if (r <= 3 && !(dx == 0 && dz == 0 && y < 6)) {
                        p.setIfEmpty(x + dx, y, z + dz, "oak_leaves[persistent=true]");
                    }
                }
            }
        }
    }
}
