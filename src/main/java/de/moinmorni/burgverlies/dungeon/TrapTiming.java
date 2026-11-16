package de.moinmorni.burgverlies.dungeon;

/**
 * Takt der Fallen (in Spielticks, 20 = 1 Sekunde). Jede Falle warnt vorher (Rauch, Klicken, Zischen) – wer
 * hinschaut, kommt durch.
 */
public final class TrapTiming {

    public enum Phase { IDLE, WARN, ACTIVE }

    /** Pfeile: alle 60 Ticks ein Schuss, 20 Ticks vorher Rauch und Klicken. */
    public static final int ARROW_PERIOD = 60;
    public static final int ARROW_WARN = 20;
    /** Glutdüsen: zwei Gruppen im Wechsel, je 40 Ticks Glut, 30 Ticks Warnung. */
    public static final int JET_PERIOD = 100;
    public static final int JET_ACTIVE = 40;
    public static final int JET_WARN = 30;
    /** Bröckelnder Boden: so lange wackelt er, bevor er nachgibt, und so lange bleibt er weg. */
    public static final int CRUMBLE_WARN = 12;
    public static final int CRUMBLE_GONE = 60;

    private TrapTiming() {
    }

    /** Schießt dieses Schussloch genau jetzt? */
    public static boolean arrowFires(long tick, int phase) {
        return Math.floorMod(tick - phase, ARROW_PERIOD) == 0;
    }

    public static boolean arrowWarns(long tick, int phase) {
        return Math.floorMod(tick - phase + ARROW_WARN, ARROW_PERIOD) == 0;
    }

    /** Zustand einer Düsen-Gruppe (0 oder 1). */
    public static Phase jet(long tick, int group) {
        long t = Math.floorMod(tick - (long) group * (JET_PERIOD / 2), JET_PERIOD);
        if (t < JET_ACTIVE) {
            return Phase.ACTIVE;
        }
        if (t >= JET_PERIOD - JET_WARN) {
            return Phase.WARN;
        }
        return Phase.IDLE;
    }
}
