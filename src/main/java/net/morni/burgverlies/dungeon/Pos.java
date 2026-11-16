package net.morni.burgverlies.dungeon;

/**
 * Stelle in einem Raum, relativ zur Raummitte: y = 1 ist die Höhe, auf der man steht. In Raumvorlagen liegt der
 * Eingang immer im Norden (kleines z), man läuft nach Süden.
 */
public record Pos(int x, int y, int z) {

    /** Um {@code quarters} Viertel im Uhrzeigersinn um die Raummitte gedreht (passend zu {@link Dir#rotate}). */
    public Pos rotate(int quarters) {
        int q = Math.floorMod(quarters, 4);
        int nx = x;
        int nz = z;
        for (int i = 0; i < q; i++) {
            int t = nx;
            nx = -nz;
            nz = t;
        }
        return new Pos(nx, y, nz);
    }

    public Pos add(int dx, int dy, int dz) {
        return new Pos(x + dx, y + dy, z + dz);
    }
}
