package de.moinmorni.burgverlies.dungeon;

import java.util.Locale;

/** Was die Krämerin zwischen den Etagen verkauft. Verbesserungen haben Stufen, der Rest ist zum Verbrauchen. */
public enum Offer {
    /** Schwert: Stein → Eisen → Diamant → Diamant mit Schärfe. */
    SWORD(40, 3, true),
    /** Rüstung: Leder → Kette → Eisen → Diamant. */
    ARMOR(45, 3, true),
    /** Bogen: einfach → Stärke I → Stärke II. */
    BOW(30, 2, true),
    /** Schild (einmal). */
    SHIELD(25, 1, true),
    /** 16 Pfeile. */
    ARROWS(10, 0, false),
    /** Heiltrank. */
    POTION(20, 0, false),
    /** Goldapfel. */
    GOLDEN_APPLE(35, 0, false),
    /** Eine Wiederbelebung mehr (höchstens zwei pro Abstieg, jede teurer). */
    REVIVE(80, 2, false);

    private final int base;
    private final int maxLevel;
    private final boolean upgrade;

    Offer(int base, int maxLevel, boolean upgrade) {
        this.base = base;
        this.maxLevel = maxLevel;
        this.upgrade = upgrade;
    }

    public int base() {
        return base;
    }

    /** Höchste Stufe (Verbesserungen) bzw. höchstens so oft kaufbar (0 = unbegrenzt). */
    public int maxLevel() {
        return maxLevel;
    }

    public boolean upgrade() {
        return upgrade;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
