package de.moinmorni.burgverlies.dungeon;

import java.util.Random;

/** Kerkertaler und Fundstücke (rein rechnerisch, getestet). */
public final class Loot {

    /** Was außer Talern in einer Truhe liegen kann. */
    public enum Item { NONE, ARROWS, POTION, GOLDEN_APPLE }

    private Loot() {
    }

    private static int scale(double v, Difficulty d) {
        return (int) Math.round(v * d.taler());
    }

    /** Taler für einen besiegten Gegner (bekommt, wer ihn besiegt hat). */
    public static int kill(MobKind kind, int floor, Difficulty d) {
        return scale(kind.taler() * (1 + 0.25 * floor), d);
    }

    /** Taler aus einer Truhe – jede Person im Abstieg bekommt so viel. */
    public static int chest(Random rnd, int floor, Difficulty d) {
        return scale(15 + 10 * floor + rnd.nextInt(11), d);
    }

    /** Fundstück aus einer Truhe (für die Person, die sie öffnet). */
    public static Item chestItem(Random rnd, int floor) {
        int roll = rnd.nextInt(100);
        int apple = 10 + 5 * floor;
        if (roll < apple) {
            return Item.GOLDEN_APPLE;
        }
        if (roll < apple + 30) {
            return Item.POTION;
        }
        if (roll < apple + 60) {
            return Item.ARROWS;
        }
        return Item.NONE;
    }

    /** Taler für jeden, wenn ein Raum geschafft ist. */
    public static int roomBonus(RoomType type, int floor, Difficulty d) {
        int base = switch (type) {
            case FIGHT -> 10;
            case TRAP, PUZZLE -> 15;
            case BOSS -> 100;
            default -> 0;
        };
        return base == 0 ? 0 : scale(base * (1 + 0.5 * floor), d);
    }
}
