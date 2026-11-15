package de.moinmorni.burgverlies.dungeon;

import java.util.Locale;
import java.util.Optional;

/** Schwierigkeit eines Abstiegs. Die Werte sind Faktoren für Gegner, Belohnung und Punkte. */
public enum Difficulty {
    LEICHT("leicht", 0.75, 0.7, 0, 1.0, 1.0, 40),
    NORMAL("normal", 1.0, 1.0, 0, 1.25, 1.5, 30),
    SCHWER("schwer", 1.4, 1.35, 1, 1.5, 2.0, 20);

    private final String id;
    private final double health;
    private final double damage;
    private final int extraMobs;
    private final double taler;
    private final double score;
    private final int bleedSeconds;

    Difficulty(String id, double health, double damage, int extraMobs, double taler, double score, int bleedSeconds) {
        this.id = id;
        this.health = health;
        this.damage = damage;
        this.extraMobs = extraMobs;
        this.taler = taler;
        this.score = score;
        this.bleedSeconds = bleedSeconds;
    }

    public String id() {
        return id;
    }

    /** Faktor auf die Lebenspunkte der Gegner. */
    public double health() {
        return health;
    }

    /** Faktor auf den Schaden der Gegner. */
    public double damage() {
        return damage;
    }

    /** Gegner mehr pro Welle. */
    public int extraMobs() {
        return extraMobs;
    }

    /** Faktor auf Kerkertaler. */
    public double taler() {
        return taler;
    }

    /** Faktor auf die Punkte. */
    public double score() {
        return score;
    }

    /** So lange kann man am Boden liegen, bevor man raus ist. */
    public int bleedSeconds() {
        return bleedSeconds;
    }

    /** „leicht“, „normal“, „schwer“ – auch englisch („easy“, „hard“). */
    public static Optional<Difficulty> parse(String s) {
        if (s == null) {
            return Optional.empty();
        }
        String k = s.toLowerCase(Locale.ROOT);
        for (Difficulty d : values()) {
            if (d.id.equals(k)) {
                return Optional.of(d);
            }
        }
        return switch (k) {
            case "easy" -> Optional.of(LEICHT);
            case "hard" -> Optional.of(SCHWER);
            default -> Optional.empty();
        };
    }
}
