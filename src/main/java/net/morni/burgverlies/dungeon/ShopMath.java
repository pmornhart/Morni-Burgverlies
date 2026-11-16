package net.morni.burgverlies.dungeon;

/** Preise und Kauf bei der Krämerin (rein rechnerisch, getestet). */
public final class ShopMath {

    private static final int ROUND = 5;

    /** Ergebnis eines Kaufs. */
    public record Purchase(Result result, int price, int walletAfter, int levelAfter) {
    }

    public enum Result { OK, TOO_POOR, MAXED }

    private ShopMath() {
    }

    /**
     * Preis für den nächsten Kauf.
     *
     * @param level bisherige Stufe (Verbesserungen) bzw. schon gekauft (Wiederbelebung)
     * @param floor Etage, auf der gekauft wird (0 = oberste); Verbrauchsware wird tiefer etwas teurer
     */
    public static int price(Offer offer, int level, int floor) {
        int p;
        if (offer.upgrade()) {
            p = offer.base() * (level + 1);
        } else if (offer == Offer.REVIVE) {
            p = offer.base() + 40 * level;
        } else {
            p = (int) Math.round(offer.base() * (1 + 0.25 * Math.max(0, floor)));
        }
        return Math.max(ROUND, Math.round(p / (float) ROUND) * ROUND);
    }

    public static boolean maxed(Offer offer, int level) {
        return offer.maxLevel() > 0 && level >= offer.maxLevel();
    }

    public static Purchase buy(int wallet, Offer offer, int level, int floor) {
        int price = price(offer, level, floor);
        if (maxed(offer, level)) {
            return new Purchase(Result.MAXED, price, wallet, level);
        }
        if (wallet < price) {
            return new Purchase(Result.TOO_POOR, price, wallet, level);
        }
        return new Purchase(Result.OK, price, wallet - price, level + 1);
    }
}
