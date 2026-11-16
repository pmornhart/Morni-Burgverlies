package de.moinmorni.burgverlies.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dauerhafte Freischaltungen – nur zum Anschauen (Spuren hinter einem im Verlies), nie stärker. Kein Kauf, nur
 * durchs Spielen.
 */
public final class Cosmetics {

    /** Spur und was man dafür geschafft haben muss. */
    public enum Trail {
        NONE(0, 0, 0, 0),
        GLUT(1, 0, 0, 0),
        FUNKEN(0, 0, 0, 25),
        SEELENFEUER(0, 1, 0, 0),
        GOLDSTAUB(0, 0, 1, 0);

        private final int runs;
        private final int bosses;
        private final int hardWins;
        private final int rooms;

        Trail(int runs, int bosses, int hardWins, int rooms) {
            this.runs = runs;
            this.bosses = bosses;
            this.hardWins = hardWins;
            this.rooms = rooms;
        }

        public boolean unlocked(int r, int b, int h, int rm) {
            return r >= runs && b >= bosses && h >= hardWins && rm >= rooms;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private Cosmetics() {
    }

    public static List<Trail> unlocked(int runs, int bosses, int hardWins, int rooms) {
        List<Trail> out = new ArrayList<>();
        for (Trail t : Trail.values()) {
            if (t.unlocked(runs, bosses, hardWins, rooms)) {
                out.add(t);
            }
        }
        return out;
    }
}
