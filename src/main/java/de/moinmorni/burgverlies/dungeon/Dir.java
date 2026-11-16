package de.moinmorni.burgverlies.dungeon;

/** Himmelsrichtung im Raster der Räume (x nach Osten, z nach Süden – wie in Minecraft). */
public enum Dir {
    NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

    private final int dx;
    private final int dz;

    Dir(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    public Dir opposite() {
        return values()[(ordinal() + 2) % 4];
    }

    /** Um {@code quarters} Viertel im Uhrzeigersinn gedreht (Norden → Osten → Süden → Westen). */
    public Dir rotate(int quarters) {
        return values()[Math.floorMod(ordinal() + quarters, 4)];
    }
}
