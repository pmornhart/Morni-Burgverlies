package de.moinmorni.burgverlies.run;

import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

/**
 * Stand eines Spielers in dem Augenblick, in dem er mitten im Abstieg die Verbindung verliert: Ort, Spielmodus,
 * Inventar (mit Rüstung und zweiter Hand), Leben, Hunger, Effekte. Kommt er rechtzeitig zurück, steht er genau so wieder
 * da.
 */
public record PlayerState(Location location, GameMode mode, ItemStack[] contents, int heldSlot, double health,
        double absorption, int food, float saturation, float exhaustion, List<PotionEffect> effects, int fireTicks,
        float fallDistance, int arrowsInBody) {

    /** Stand jetzt (Gegenstände als Kopie). */
    public static PlayerState of(Player p) {
        return new PlayerState(p.getLocation().clone(), p.getGameMode(), copy(p.getInventory().getContents()),
                p.getInventory().getHeldItemSlot(), p.getHealth(), p.getAbsorptionAmount(), p.getFoodLevel(),
                p.getSaturation(), p.getExhaustion(), List.copyOf(p.getActivePotionEffects()), p.getFireTicks(),
                p.getFallDistance(), p.getArrowsInBody());
    }

    /** Spieler wieder genau so hinstellen. */
    public void restore(Player p) {
        p.closeInventory();
        if (p.getGameMode() != mode) {
            p.setGameMode(mode);
        }
        p.teleport(location);
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setContents(copy(contents));
        inv.setHeldItemSlot(heldSlot);
        p.setItemOnCursor(null);
        for (PotionEffect e : p.getActivePotionEffects()) {
            p.removePotionEffect(e.getType());
        }
        p.addPotionEffects(effects);
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20.0 : max.getValue();
        p.setHealth(Math.max(0.5, Math.min(cap, health)));
        p.setAbsorptionAmount(absorption);
        p.setFoodLevel(food);
        p.setSaturation(saturation);
        p.setExhaustion(exhaustion);
        p.setFireTicks(fireTicks);
        p.setFallDistance(fallDistance);
        p.setArrowsInBody(arrowsInBody);
    }

    private static ItemStack[] copy(ItemStack[] items) {
        ItemStack[] out = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            out[i] = items[i] == null ? null : items[i].clone();
        }
        return out;
    }
}
