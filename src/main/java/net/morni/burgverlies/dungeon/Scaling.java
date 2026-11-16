package net.morni.burgverlies.dungeon;

/**
 * Wie stark das Verlies mit Schwierigkeit und Zahl der Spieler (1–4) wächst. Alles rein rechnerisch (getestet).
 */
public final class Scaling {

    /** Höchstes Leben, das Minecraft für ein Wesen erlaubt. */
    public static final double MAX_HEALTH = 1024;
    /** Lebenspunkte des Kerkermeisters bei einer Person auf „normal“. */
    public static final double BOSS_BASE = 260;
    public static final int MAX_WAVE = 8;
    public static final int MAX_PLAYERS = 4;

    private Scaling() {
    }

    private static int players(int n) {
        return Math.max(1, Math.min(MAX_PLAYERS, n));
    }

    /** Leben eines Gegners. */
    public static double mobHealth(double base, Difficulty d, int players) {
        return Math.min(MAX_HEALTH, base * d.health() * (1 + 0.25 * (players(players) - 1)));
    }

    /** Schaden-Faktor der Gegner (mehr Spieler → etwas mehr, damit es spannend bleibt). */
    public static double mobDamage(Difficulty d, int players) {
        return d.damage() * (1 + 0.1 * (players(players) - 1));
    }

    /** Gegner pro Welle. */
    public static int waveSize(int base, Difficulty d, int players) {
        int n = (int) Math.ceil(base * (1 + 0.5 * (players(players) - 1))) + d.extraMobs();
        return Math.max(1, Math.min(MAX_WAVE, n));
    }

    /** Wellen in einem Kampfraum: zwei, ab der dritten Etage drei. */
    public static int waves(int floor) {
        return floor >= 2 ? 3 : 2;
    }

    /** Leben des Kerkermeisters (gedeckelt bei Minecrafts Grenze). */
    public static double bossHealth(Difficulty d, int players) {
        return Math.min(MAX_HEALTH, Math.round(BOSS_BASE * d.health() * (1 + 0.6 * (players(players) - 1))));
    }

    /** Wachen, die der Kerkermeister ruft. */
    public static int bossSummons(Difficulty d, int players) {
        return Math.max(1, Math.min(6, players(players) + d.extraMobs()));
    }
}
