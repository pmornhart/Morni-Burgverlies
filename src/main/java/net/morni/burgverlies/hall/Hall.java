package net.morni.burgverlies.hall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.burgverlies.BurgverliesPlugin;
import net.morni.burgverlies.Items;
import net.morni.burgverlies.Texts;
import net.morni.burgverlies.Ui;
import net.morni.burgverlies.data.PlayerData;
import net.morni.burgverlies.dungeon.Difficulty;
import net.morni.burgverlies.world.HallPlan;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

/** Das Torhaus: Ankunft, Gegenstände in der Hand, Leiste rechts. */
public final class Hall {

    public static final String START = "start";
    public static final String STATS = "stats";
    public static final String TRAILS = "trails";
    public static final String LOBBY = "lobby";
    public static final String LEAVE_QUEUE = "leave-queue";

    private final BurgverliesPlugin plugin;
    private final Map<UUID, String> shownLang = new HashMap<>();

    public Hall(BurgverliesPlugin plugin) {
        this.plugin = plugin;
    }

    public Location spawn() {
        return HallPlan.spawn(plugin.world());
    }

    /** Ins Torhaus (nach dem Betreten, nach einem Abstieg, nach /verlassen). */
    public void enter(Player p) {
        p.closeInventory();
        p.teleport(spawn());
        p.setGameMode(GameMode.ADVENTURE);
        reset(p);
        p.setFallDistance(0);
        p.setLevel(0);
        p.setExp(0);
        items(p);
        sidebar(p);
        plugin.boards().showFor(p);
    }

    static void reset(Player p) {
        p.setWalkSpeed(0.2f);
        AttributeInstance jump = p.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) {
            jump.setBaseValue(jump.getDefaultValue());
        }
        p.setGlowing(false);
        p.setPose(Pose.STANDING, false);
        p.setFireTicks(0);
        for (PotionEffect e : p.getActivePotionEffects()) {
            p.removePotionEffect(e.getType());
        }
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(5);
    }

    public boolean inHall(Player p) {
        return plugin.runs().of(p).isEmpty() && p.getWorld() == plugin.world();
    }

    /** Gegenstände in der Hand je nach Zustand (wartet oder nicht). */
    public void items(Player p) {
        String lang = plugin.lang(p);
        shownLang.put(p.getUniqueId(), lang);
        PlayerInventory inv = p.getInventory();
        inv.clear();
        boolean waiting = plugin.queue().contains(p.getUniqueId());
        if (waiting) {
            inv.setItem(0, Items.hallTag(Items.named(Material.BARRIER, 1, Texts.text(lang, "item.leave-queue"),
                    Texts.lines(lang, "item.leave-queue.lore")), LEAVE_QUEUE));
        } else {
            inv.setItem(0, Items.hallTag(Items.named(Material.LANTERN, 1, Texts.text(lang, "item.start"),
                    Texts.lines(lang, "item.start.lore")), START));
        }
        inv.setItem(4, Items.hallTag(Items.named(Material.GOLD_INGOT, 1, Texts.text(lang, "item.stats"),
                Texts.lines(lang, "item.stats.lore")), STATS));
        inv.setItem(6, Items.hallTag(Items.named(Material.BLAZE_POWDER, 1, Texts.text(lang, "item.trails"),
                Texts.lines(lang, "item.trails.lore")), TRAILS));
        inv.setItem(8, Items.hallTag(Items.named(Material.OAK_DOOR, 1, Texts.text(lang, "item.lobby"),
                Texts.lines(lang, "item.lobby.lore")), LOBBY));
        inv.setHeldItemSlot(0);
    }

    /** Leiste rechts im Torhaus. */
    public void sidebar(Player p) {
        String lang = plugin.lang(p);
        PlayerData d = plugin.data(p);
        List<Component> lines = new ArrayList<>();
        lines.add(Texts.text(lang, "side.hall.runs", Placeholder.unparsed("n", String.valueOf(d.runs()))));
        lines.add(Texts.text(lang, "side.hall.wins", Placeholder.unparsed("n", String.valueOf(d.wins()))));
        lines.add(Texts.text(lang, "side.hall.bosses", Placeholder.unparsed("n", String.valueOf(d.bosses()))));
        long best = 0;
        for (Difficulty diff : Difficulty.values()) {
            long t = d.bestTime(diff);
            if (t > 0 && (best == 0 || t < best)) {
                best = t;
            }
        }
        lines.add(Texts.text(lang, "side.hall.best", Placeholder.unparsed("time", best == 0 ? "–" : Ui.time(best))));
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "side.hall.chosen", Placeholder.component("difficulty",
                Texts.text(lang, "difficulty." + d.chosen().id()))));
        if (plugin.queue().contains(p.getUniqueId())) {
            lines.add(Texts.text(lang, "side.hall.searching"));
        }
        lines.add(Component.text(" "));
        lines.add(Texts.text(lang, "side.hall.inside", Placeholder.unparsed("n",
                String.valueOf(plugin.runs().playersInside()))));
        plugin.sidebar().show(p, Texts.text(lang, "side.title"), lines);
    }

    /** Sprache kann nach dem Betreten erst später feststehen (Client meldet sie spät). */
    public void checkLanguage(Player p) {
        String lang = plugin.lang(p);
        if (!lang.equals(shownLang.get(p.getUniqueId()))) {
            shownLang.put(p.getUniqueId(), lang);
            plugin.boards().showFor(p);
            if (inHall(p)) {
                items(p);
                sidebar(p);
            }
        }
    }

    public void forget(UUID u) {
        shownLang.remove(u);
    }

    /** Im Startbereich vor dem Abstieg? */
    public static boolean atDescent(Location l) {
        int x = l.getBlockX();
        int z = l.getBlockZ();
        int[] zone = HallPlan.DESCENT_ZONE;
        return x >= zone[0] && x <= zone[2] && z >= zone[1] && z <= zone[3]
                && Math.abs(l.getY() - (HallPlan.FLOOR_Y + 1)) < 2.5;
    }
}
