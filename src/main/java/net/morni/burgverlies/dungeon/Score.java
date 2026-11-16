package net.morni.burgverlies.dungeon;

/** Punkte eines Abstiegs: Räume, Sieg, Zeit, wie oft jemand gefallen ist – mal Schwierigkeit. */
public final class Score {

    public static final int PER_ROOM = 100;
    public static final int BOSS = 1500;
    public static final int PER_DOWN = 100;
    /** Bis zu dieser Zeit gibt es beim Sieg Zeit-Punkte (2 pro übrige Sekunde). */
    public static final int TIME_BONUS_SECONDS = 1800;

    private Score() {
    }

    public static int compute(boolean won, int rooms, int downs, long timeMs, Difficulty d) {
        double s = rooms * PER_ROOM - downs * PER_DOWN;
        if (won) {
            s += BOSS + 2 * Math.max(0, TIME_BONUS_SECONDS - timeMs / 1000);
        }
        return (int) Math.round(Math.max(0, s) * d.score());
    }
}
