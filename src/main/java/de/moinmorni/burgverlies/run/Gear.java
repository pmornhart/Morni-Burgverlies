package de.moinmorni.burgverlies.run;

import de.moinmorni.burgverlies.Items;
import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.dungeon.Difficulty;
import de.moinmorni.burgverlies.dungeon.Loot;
import de.moinmorni.burgverlies.dungeon.Offer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/** Ausrüstung im Verlies: Startausrüstung, Verbesserungen der Krämerin und Fundstücke. */
public final class Gear {

    private static final Material[] SWORDS = {Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD,
        Material.DIAMOND_SWORD};
    private static final Material[][] ARMOUR = {
        {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS},
        {Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS},
        {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS},
        {Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS}};
    private static final int ARROWS = 16;

    private Gear() {
    }

    /** Erste Ausrüstung beim Start. */
    static void start(Player p, Hero h, Difficulty d, String lang) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        apply(p, h, lang);
        inv.setItem(8, arrows(d == Difficulty.LEICHT ? 2 * ARROWS : ARROWS));
        inv.setItem(2, potion(lang, d == Difficulty.LEICHT ? 2 : 1));
    }

    /** Waffen und Rüstung nach den gekauften Stufen (Verbrauchsware bleibt, wie sie ist). */
    static void apply(Player p, Hero h, String lang) {
        PlayerInventory inv = p.getInventory();
        int sword = h.level(Offer.SWORD);
        ItemStack s = unbreakable(new ItemStack(SWORDS[Math.min(sword, SWORDS.length - 1)]), "sword");
        if (sword >= 3) {
            s.addUnsafeEnchantment(Enchantment.SHARPNESS, 2);
        }
        inv.setItem(0, s);
        ItemStack bow = unbreakable(new ItemStack(Material.BOW), "bow");
        if (h.level(Offer.BOW) > 0) {
            bow.addUnsafeEnchantment(Enchantment.POWER, h.level(Offer.BOW));
        }
        inv.setItem(1, bow);
        Material[] set = ARMOUR[Math.min(h.level(Offer.ARMOR), ARMOUR.length - 1)];
        ItemStack[] armour = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            ItemStack a = new ItemStack(set[i]);
            if (h.level(Offer.ARMOR) == 0) {
                a = Mobs.leather(set[i], Color.fromRGB(0x4A3426));
            }
            armour[i] = unbreakable(a, "armour");
        }
        inv.setHelmet(armour[0]);
        inv.setChestplate(armour[1]);
        inv.setLeggings(armour[2]);
        inv.setBoots(armour[3]);
        if (h.level(Offer.SHIELD) > 0) {
            inv.setItemInOffHand(unbreakable(new ItemStack(Material.SHIELD), "shield"));
        }
    }

    static ItemStack arrows(int n) {
        return Items.kitTag(new ItemStack(Material.ARROW, n), "arrows");
    }

    static ItemStack potion(String lang, int n) {
        ItemStack item = new ItemStack(Material.POTION, Math.max(1, n));
        item.editMeta(meta -> {
            if (meta instanceof PotionMeta pm) {
                pm.setBasePotionType(PotionType.STRONG_HEALING);
                pm.displayName(Texts.text(lang, "item.potion"));
            }
        });
        return Items.kitTag(item, "potion");
    }

    static ItemStack loot(Loot.Item item, String lang) {
        return switch (item) {
            case ARROWS -> arrows(8);
            case POTION -> potion(lang, 1);
            case GOLDEN_APPLE -> new ItemStack(Material.GOLDEN_APPLE);
            case NONE -> null;
        };
    }

    /** Verbrauchsware aus dem Laden. */
    static ItemStack consumable(Offer o, String lang) {
        return switch (o) {
            case ARROWS -> arrows(ARROWS);
            case POTION -> potion(lang, 1);
            case GOLDEN_APPLE -> new ItemStack(Material.GOLDEN_APPLE);
            default -> null;
        };
    }

    private static ItemStack unbreakable(ItemStack item, String tag) {
        item.editMeta(meta -> meta.setUnbreakable(true));
        return Items.kitTag(item, tag);
    }
}
