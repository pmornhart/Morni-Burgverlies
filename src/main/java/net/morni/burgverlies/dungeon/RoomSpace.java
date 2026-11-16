package net.morni.burgverlies.dungeon;

/**
 * Was in einem Raum fürs Spiel frei bleiben muss – Schmuck darf dort nicht stehen: Wege zu allen vier Türen (ein
 * Kreuz durch die Mitte), Stellen für Gegner (mit Kopffreiheit), Hebel, Platten, Glocke, Truhen, die Krämerin mit
 * ihrem Platz davor, die Treppe der Krämerstube, im Fallengang der ganze Lauf und im Thronsaal die Kampffläche.
 * {@code world.RoomPlans} räumt diese Stellen nach dem Schmücken wieder leer (rein rechnerisch, getestet).
 */
public final class RoomSpace {

    /** Bis zu dieser Höhe (y) gilt eine Stelle als Weg. Darüber dürfen Ketten und Laternen hängen. */
    public static final int CLEAR_HEIGHT = 3;

    private RoomSpace() {
    }

    public static boolean reserved(RoomTemplate t, int x, int y, int z) {
        int h = t.half();
        if (y < 1 || y > t.height() || Math.abs(x) > h || Math.abs(z) > h) {
            return false;
        }
        boolean low = y <= CLEAR_HEIGHT;
        // Türen: 3 breit, 4 hoch, in jeder Wand; dazu das Kreuz durch die Mitte
        if (y <= 4 && ((Math.abs(x) <= 1 && Math.abs(z) >= h - 1) || (Math.abs(z) <= 1 && Math.abs(x) >= h - 1))) {
            return true;
        }
        if (low && (Math.abs(x) <= 1 || Math.abs(z) <= 1)) {
            return true;
        }
        for (Pos s : t.spawns()) {
            if (s.x() == x && s.z() == z && low) {
                return true;
            }
        }
        for (Pos c : t.chests()) {
            if (c.x() == x && c.z() == z && low) {
                return true;
            }
        }
        for (Pos in : t.inputs()) {
            if (in.x() == x && in.z() == z && y <= in.y() + 1) {
                return true;
            }
        }
        if (t.bell() != null && t.bell().x() == x && t.bell().z() == z && low) {
            return true;
        }
        switch (t.type()) {
            case TRAP -> {
                // Pfeile fliegen quer durch, man läuft überall: nur ganz vorn und ganz hinten in den Ecken Platz
                if (low && Math.abs(z) <= h - 1) {
                    return true;
                }
                if (low && Math.abs(x) <= 4) {
                    return true;
                }
            }
            case SHOP -> {
                Pos n = t.npc();
                if (low && Math.abs(x - n.x()) <= 1 && z == n.z()) {
                    return true;
                }
                // vor der Theke stehen, um mit ihr zu reden
                if (low && x >= n.x() + 3 && x <= n.x() + 4 && Math.abs(z - n.z()) <= 1) {
                    return true;
                }
                if (Math.abs(x) <= 3 && z >= 3) {
                    return true;
                }
            }
            case BOSS -> {
                Pos b = t.npc();
                boolean pillar = (Math.abs(x) == 6 || Math.abs(x) == 7) && (Math.abs(z) == 6 || Math.abs(z) == 7);
                if (low && (Math.abs(x) <= 2 || (Math.abs(x - b.x()) <= 3 && Math.abs(z - b.z()) <= 3)
                        || (Math.max(Math.abs(x), Math.abs(z)) <= h - 4 && !pillar))) {
                    return true;
                }
            }
            case PUZZLE -> {
                // Platz vor den Hebeln an der Eingangswand
                if (!t.plates() && low && z <= -h + 1 && Math.abs(x) <= 6) {
                    return true;
                }
            }
            default -> {
                // nichts weiter
            }
        }
        return false;
    }
}
