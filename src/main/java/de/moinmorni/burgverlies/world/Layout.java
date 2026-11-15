package de.moinmorni.burgverlies.world;

import de.moinmorni.burgverlies.dungeon.Dir;
import de.moinmorni.burgverlies.dungeon.DungeonGenerator;
import de.moinmorni.burgverlies.dungeon.PlacedRoom;
import de.moinmorni.burgverlies.dungeon.Pos;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Wo was in der Welt liegt. Jeder laufende Abstieg hat einen eigenen Platz („Verlies“ {@code slot}) weit weg vom
 * Torhaus; die drei Etagen liegen darin übereinander – je tiefer, desto weiter unten.
 */
public final class Layout {

    public static final int ORIGIN_X = 1000;
    public static final int SPACING = 400;
    public static final int ORIGIN_Z = 0;
    public static final int TOP_FLOOR_Y = 90;
    public static final int FLOOR_STEP = 22;

    private Layout() {
    }

    public static int floorY(int floor) {
        return TOP_FLOOR_Y - FLOOR_STEP * floor;
    }

    /** Raummitte (Bodenblock) in der Welt. */
    public static int[] center(int slot, int floor, PlacedRoom r) {
        return new int[] {ORIGIN_X + slot * SPACING + r.cellX() * DungeonGenerator.CELL, floorY(floor),
                ORIGIN_Z + r.cellZ() * DungeonGenerator.CELL};
    }

    /** Vorlagen-Stelle eines Raumes → Weltblock. */
    public static int[] block(int slot, int floor, PlacedRoom r, Pos local) {
        int[] c = center(slot, floor, r);
        Pos w = r.toWorld(local);
        return new int[] {c[0] + w.x(), c[1] + w.y(), c[2] + w.z()};
    }

    /** Mitte des Blocks als Ort (zum Hinstellen von Spielern und Gegnern). */
    public static Location at(World world, int slot, int floor, PlacedRoom r, Pos local, Dir facing) {
        int[] b = block(slot, floor, r, local);
        Location l = new Location(world, b[0] + 0.5, b[1], b[2] + 0.5);
        if (facing != null) {
            l.setYaw(yaw(facing));
        }
        return l;
    }

    /** Wohin man nach dem Betreten schaut: in den Raum hinein (Vorlagen-Süden). */
    public static Dir inward(PlacedRoom r) {
        return Dir.SOUTH.rotate(r.rotation());
    }

    public static float yaw(Dir d) {
        return switch (d) {
            case NORTH -> 180f;
            case EAST -> -90f;
            case SOUTH -> 0f;
            case WEST -> 90f;
        };
    }

    /** Liegt der Ort im Innenraum dieses Raumes (mit {@code margin} Blöcken Spiel)? */
    public static boolean inside(Location l, int slot, int floor, PlacedRoom r, double margin) {
        int[] c = center(slot, floor, r);
        double h = r.template().half() + 0.5 + margin;
        return Math.abs(l.getX() - (c[0] + 0.5)) <= h && Math.abs(l.getZ() - (c[2] + 0.5)) <= h
                && l.getY() >= c[1] - 4 && l.getY() <= c[1] + r.template().height() + 2;
    }

    /** Ort → Vorlagen-Stelle (Block, in dem die Füße stehen). */
    public static Pos toLocal(Location l, int slot, int floor, PlacedRoom r) {
        int[] c = center(slot, floor, r);
        Pos w = new Pos(l.getBlockX() - c[0], l.getBlockY() - c[1], l.getBlockZ() - c[2]);
        return w.rotate(-r.rotation());
    }
}
