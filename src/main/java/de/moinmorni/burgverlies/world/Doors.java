package de.moinmorni.burgverlies.world;

import de.moinmorni.burgverlies.dungeon.Dir;
import de.moinmorni.burgverlies.dungeon.DungeonGenerator;
import de.moinmorni.burgverlies.dungeon.PlacedRoom;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.block.data.BlockData;

/** Türen (mit Fallgitter) und Gänge zwischen den Räumen – in Welt-Koordinaten. */
public final class Doors {

    public static final int WIDTH = 1;
    public static final int HEIGHT = 4;

    private Doors() {
    }

    /** Blöcke einer Tür in der Wand {@code side} (Welt-Richtung). */
    public static List<int[]> blocks(int slot, int floor, PlacedRoom r, Dir side) {
        int[] c = Layout.center(slot, floor, r);
        int off = r.template().half() + 1;
        Dir across = side.rotate(1);
        List<int[]> out = new ArrayList<>();
        for (int a = -WIDTH; a <= WIDTH; a++) {
            for (int y = 1; y <= HEIGHT; y++) {
                out.add(new int[] {c[0] + side.dx() * off + across.dx() * a, c[1] + y,
                        c[2] + side.dz() * off + across.dz() * a});
            }
        }
        return out;
    }

    /** Fallgitter auf oder zu. */
    public static List<Builder.Put> gate(int slot, int floor, PlacedRoom r, Dir side, boolean closed) {
        BlockData bars = Plan.data(side.dx() == 0 ? "iron_bars[east=true,west=true]" : "iron_bars[north=true,south=true]");
        BlockData air = Plan.data("cave_air");
        List<Builder.Put> out = new ArrayList<>();
        for (int[] b : blocks(slot, floor, r, side)) {
            out.add(new Builder.Put(b[0], b[1], b[2], closed ? bars : air));
        }
        return out;
    }

    /** Gang von Raum {@code a} zu Raum {@code b} (b liegt in Richtung {@code a.exit()}). */
    public static List<Builder.Put> corridor(int slot, int floor, PlacedRoom a, PlacedRoom b, long seed) {
        RoomPlans.Palette pal = RoomPlans.palette(floor);
        Dir d = a.exit();
        Dir across = d.rotate(1);
        int[] c = Layout.center(slot, floor, a);
        int from = a.template().half() + 2;
        int to = DungeonGenerator.CELL - b.template().half() - 2;
        List<Builder.Put> out = new ArrayList<>();
        for (int t = from; t <= to; t++) {
            for (int w = -2; w <= 2; w++) {
                int x = c[0] + d.dx() * t + across.dx() * w;
                int z = c[2] + d.dz() * t + across.dz() * w;
                for (int y = 0; y <= HEIGHT + 1; y++) {
                    String spec;
                    boolean side = Math.abs(w) == 2;
                    if (y == 0) {
                        int n = RoomPlans.noise(x, y, z, seed) % 100;
                        spec = side ? pal.wall() : n < 25 ? pal.floorAlt() : pal.floor();
                    } else if (y == HEIGHT + 1 || side) {
                        int n = RoomPlans.noise(x, y, z, seed) % 100;
                        spec = n < 20 ? pal.wallAlt() : pal.wall();
                    } else if (w == 0 && y == HEIGHT && Math.floorMod(t, 5) == 0) {
                        spec = pal.lantern() + "[hanging=true]";
                    } else if (w == 1 && y == HEIGHT && Math.floorMod(t, 5) == 2) {
                        spec = "light[level=12]";
                    } else {
                        spec = "cave_air";
                    }
                    out.add(new Builder.Put(x, c[1] + y, z, Plan.data(spec)));
                }
            }
        }
        return out;
    }
}
