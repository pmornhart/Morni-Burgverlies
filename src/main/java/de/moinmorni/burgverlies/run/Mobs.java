package de.moinmorni.burgverlies.run;

import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.dungeon.MobKind;
import de.moinmorni.burgverlies.dungeon.Scaling;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Gegner bauen: eigenes Aussehen (Rüstung, Waffen, Name über dem Kopf), Leben nach Schwierigkeit und Gruppengröße.
 * Jeder Gegner trägt die Nummer seines Abstiegs – so findet das Plugin ihn wieder und räumt Reste nach einem
 * Neustart weg.
 */
public final class Mobs {

    public static final String TAG = "bv_mob";
    public static final String BOSS_TAG = "bv_boss";

    private static NamespacedKey runKey;
    private static NamespacedKey kindKey;

    private Mobs() {
    }

    public static void init(Plugin plugin) {
        runKey = new NamespacedKey(plugin, "run");
        kindKey = new NamespacedKey(plugin, "kind");
    }

    /** Nummer des Abstiegs, zu dem das Wesen gehört (0 = keiner). */
    public static int runOf(Entity e) {
        Integer id = e.getPersistentDataContainer().get(runKey, PersistentDataType.INTEGER);
        return id == null ? 0 : id;
    }

    public static MobKind kindOf(Entity e) {
        String k = e.getPersistentDataContainer().get(kindKey, PersistentDataType.STRING);
        if (k == null) {
            return null;
        }
        try {
            return MobKind.valueOf(k.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    static void mark(Entity e, Run run, String kind) {
        e.getPersistentDataContainer().set(runKey, PersistentDataType.INTEGER, run.id);
        if (kind != null) {
            e.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, kind);
        }
        e.setPersistent(true);
    }

    static Mob spawn(Run run, MobKind kind, Location at) {
        Class<? extends Mob> type = switch (kind) {
            case KERKERWACHE -> Zombie.class;
            case BURGSCHUETZE -> Skeleton.class;
            case KELLERSPINNE -> CaveSpider.class;
            case GRUFTWAECHTER -> Husk.class;
            case KETTENKNECHT -> Vindicator.class;
        };
        TextColor colour = switch (kind) {
            case KETTENKNECHT -> TextColor.color(0xE0603A);
            case GRUFTWAECHTER -> TextColor.color(0xC9B98A);
            default -> TextColor.color(0xD0D0D0);
        };
        Mob mob = at.getWorld().spawn(at, type, m -> {
            mark(m, run, kind.key());
            m.addScoreboardTag(TAG);
            m.customName(Component.text(Texts.raw(run.lang, "mob." + kind.key()), colour));
            m.setCustomNameVisible(true);
            m.setRemoveWhenFarAway(false);
            m.setCanPickupItems(false);
            if (m instanceof Zombie z) {
                z.setShouldBurnInDay(false);
                z.setAdult();
            }
            if (m instanceof Skeleton s) {
                s.setShouldBurnInDay(false);
            }
            dress(m, kind, run.floor);
            health(m, Scaling.mobHealth(kind.health(), run.difficulty, run.size()));
        });
        run.entities.add(mob.getUniqueId());
        return mob;
    }

    /** Rüstungsständer als Schmuck (Rüstkammer, Wachstube) – gehört zum Abstieg und verschwindet mit ihm. */
    static void stand(Run run, org.bukkit.World world, de.moinmorni.burgverlies.world.Plan.Stand s) {
        Location l = new Location(world, s.x() + 0.5, s.y(), s.z() + 0.5,
                de.moinmorni.burgverlies.world.Layout.yaw(s.facing()), 0f);
        org.bukkit.entity.ArmorStand a = world.spawn(l, org.bukkit.entity.ArmorStand.class, st -> {
            mark(st, run, null);
            st.addScoreboardTag("bv_deko");
            st.setInvulnerable(true);
            st.setArms(true);
            st.setBasePlate(false);
            st.setGravity(false);
            st.setCanPickupItems(false);
            EntityEquipment eq = st.getEquipment();
            Material[] set = switch (s.kit()) {
                case "chain" -> new Material[] {Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE,
                    Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS, Material.STONE_SWORD};
                case "gold" -> new Material[] {Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
                    Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS, Material.GOLDEN_SWORD};
                case "diamond" -> new Material[] {Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.SHIELD};
                case "leather" -> new Material[] {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                    Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS, Material.BOW};
                default -> new Material[] {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS,
                    Material.IRON_BOOTS, Material.IRON_SWORD};
            };
            eq.setHelmet(new ItemStack(set[0]));
            eq.setChestplate(new ItemStack(set[1]));
            eq.setLeggings(new ItemStack(set[2]));
            eq.setBoots(new ItemStack(set[3]));
            eq.setItemInMainHand(new ItemStack(set[4]));
        });
        run.entities.add(a.getUniqueId());
    }

    static void health(LivingEntity e, double hp) {
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) {
            max.setBaseValue(hp);
        }
        e.setHealth(hp);
    }

    private static void dress(Mob m, MobKind kind, int floor) {
        EntityEquipment eq = m.getEquipment();
        if (eq == null) {
            return;
        }
        switch (kind) {
            case KERKERWACHE -> {
                eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                eq.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                eq.setItemInMainHand(new ItemStack(floor >= 1 ? Material.IRON_SWORD : Material.STONE_SWORD));
            }
            case BURGSCHUETZE -> {
                eq.setHelmet(leather(Material.LEATHER_HELMET, Color.fromRGB(0x5A3A22)));
                eq.setChestplate(leather(Material.LEATHER_CHESTPLATE, Color.fromRGB(0x7A2E2E)));
                eq.setItemInMainHand(new ItemStack(Material.BOW));
            }
            case GRUFTWAECHTER -> {
                eq.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                eq.setItemInMainHand(new ItemStack(Material.IRON_AXE));
            }
            case KETTENKNECHT -> eq.setItemInMainHand(new ItemStack(Material.IRON_AXE));
            default -> {
                // Spinnen tragen nichts
            }
        }
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
    }

    static ItemStack leather(Material m, Color c) {
        ItemStack item = new ItemStack(m);
        item.editMeta(meta -> {
            if (meta instanceof LeatherArmorMeta lm) {
                lm.setColor(c);
            }
        });
        return item;
    }
}
