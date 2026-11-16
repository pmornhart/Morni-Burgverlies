package net.morni.burgverlies.run;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.morni.burgverlies.dungeon.Offer;

/** Eine Person in einem Abstieg: Kerkertaler und was sie bei der Krämerin gekauft hat (nur für diesen Abstieg). */
public final class Hero {

    private final UUID uuid;
    private final String name;
    private int taler;
    private int earned;
    private final Map<Offer, Integer> levels = new EnumMap<>(Offer.class);
    private boolean left;

    Hero(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public int taler() {
        return taler;
    }

    /** Insgesamt in diesem Abstieg verdient (für die Statistik). */
    public int earned() {
        return earned;
    }

    public void earn(int amount) {
        if (amount > 0) {
            taler += amount;
            earned += amount;
        }
    }

    public void pay(int newBalance) {
        taler = Math.max(0, newBalance);
    }

    public int level(Offer o) {
        return levels.getOrDefault(o, 0);
    }

    public void level(Offer o, int level) {
        levels.put(o, level);
    }

    /** Hat den Abstieg verlassen (Verbindung weg oder /verlassen). */
    public boolean left() {
        return left;
    }

    void leave() {
        left = true;
    }
}
