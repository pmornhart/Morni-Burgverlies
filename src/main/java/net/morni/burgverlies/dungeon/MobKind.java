package net.morni.burgverlies.dungeon;

/** Gegner im Burgverlies (eigene Namen; welches Minecraft-Wesen dahintersteckt, legt {@code run.Mobs} fest). */
public enum MobKind {
    KERKERWACHE(20, 6),
    BURGSCHUETZE(18, 6),
    KELLERSPINNE(12, 4),
    GRUFTWAECHTER(26, 8),
    KETTENKNECHT(32, 12);

    private final double health;
    private final int taler;

    MobKind(double health, int taler) {
        this.health = health;
        this.taler = taler;
    }

    /** Leben auf „normal“ bei einer Person. */
    public double health() {
        return health;
    }

    /** Kerkertaler für den Sieg über ihn (Grundwert). */
    public int taler() {
        return taler;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
