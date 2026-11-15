package de.moinmorni.burgverlies.menu;

import de.moinmorni.burgverlies.BurgverliesPlugin;
import de.moinmorni.burgverlies.Items;
import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.Ui;
import de.moinmorni.burgverlies.data.Leaderboard;
import de.moinmorni.burgverlies.data.PlayerData;
import de.moinmorni.burgverlies.dungeon.Cosmetics;
import de.moinmorni.burgverlies.dungeon.Difficulty;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/** Menüs im Torhaus: Abstieg wählen, Statistik und Bestenliste, Spuren. */
public final class Menus implements Listener {

    private static final Material[] DIFF_ICON = {Material.LIME_CANDLE, Material.YELLOW_CANDLE, Material.RED_CANDLE};
    private static final int[] DIFF_SLOT = {11, 13, 15};
    public static final int START_NOW = 20;
    public static final int SEARCH = 24;

    private final BurgverliesPlugin plugin;

    public Menus(BurgverliesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder(false);
        if (holder instanceof Menu menu) {
            menu.click(e);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder holder = e.getInventory().getHolder(false);
        if (holder instanceof Menu menu) {
            menu.drag(e);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        InventoryHolder holder = e.getInventory().getHolder(false);
        if (holder instanceof Menu menu && e.getPlayer() instanceof Player p) {
            menu.closed(p);
        }
    }

    /** Offene Menüs mit Live-Zahlen neu zeichnen (jede Sekunde). */
    public void refreshOpen() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder(false);
            if (holder instanceof Menu menu) {
                menu.refresh();
            }
        }
    }

    private static ItemStack frame() {
        return Items.pane(Material.BROWN_STAINED_GLASS_PANE);
    }

    // ---------- Abstieg wählen ----------

    public void start(Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.start.title"));
        Runnable draw = () -> drawStart(menu, p);
        draw.run();
        menu.refresher(draw);
        menu.open(p);
        Ui.sound(p, "block.chest.open", 0.8f);
    }

    private void drawStart(Menu menu, Player p) {
        String lang = plugin.lang(p);
        PlayerData d = plugin.data(p);
        for (int i = 0; i < 27; i++) {
            menu.set(i, frame(), null);
        }
        menu.set(4, Items.named(Material.WRITABLE_BOOK, 1, Texts.text(lang, "menu.start.info"),
                Texts.lines(lang, "menu.start.info.lore")), null);
        Difficulty[] diffs = Difficulty.values();
        for (int i = 0; i < diffs.length; i++) {
            Difficulty diff = diffs[i];
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "difficulty." + diff.id() + ".desc"));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "menu.start.waiting", Placeholder.unparsed("n",
                    String.valueOf(plugin.queue().waiting(diff)))));
            lore.add(Texts.text(lang, d.chosen() == diff ? "menu.start.chosen" : "menu.start.choose"));
            ItemStack icon = Items.named(DIFF_ICON[i], 1, Texts.text(lang, "difficulty." + diff.id()), lore);
            Items.glow(icon, d.chosen() == diff);
            menu.set(DIFF_SLOT[i], icon, who -> {
                plugin.data(who).chosen(diff);
                Ui.sound(who, "ui.button.click", 1.2f);
                drawStart(menu, who);
                plugin.hall().sidebar(who);
            });
        }
        menu.set(START_NOW, Items.named(Material.IRON_DOOR, 1, Texts.text(lang, "menu.start.now"),
                Texts.lines(lang, "menu.start.now.lore")), who -> {
                    who.closeInventory();
                    plugin.startNow(who, plugin.data(who).chosen());
                });
        menu.set(SEARCH, Items.named(Material.SPYGLASS, 1, Texts.text(lang, "menu.start.search"),
                Texts.lines(lang, "menu.start.search.lore", Placeholder.unparsed("seconds",
                        String.valueOf(plugin.settings().searchSeconds())))), who -> {
                    who.closeInventory();
                    plugin.search(who, plugin.data(who).chosen());
                });
    }

    // ---------- Statistik ----------

    public void stats(Player p, PlayerData d) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(4, Texts.text(lang, "menu.stats.title", Placeholder.unparsed("name", d.name())));
        for (int i = 0; i < 36; i++) {
            menu.set(i, frame(), null);
        }
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(meta -> {
            if (meta instanceof SkullMeta sm) {
                sm.setOwningPlayer(plugin.getServer().getOfflinePlayer(d.uuid()));
            }
        });
        List<Component> lore = new ArrayList<>();
        lore.add(Texts.text(lang, "stats.runs", Placeholder.unparsed("n", String.valueOf(d.runs()))));
        lore.add(Texts.text(lang, "stats.wins", Placeholder.unparsed("n", String.valueOf(d.wins()))));
        lore.add(Texts.text(lang, "stats.bosses", Placeholder.unparsed("n", String.valueOf(d.bosses()))));
        lore.add(Texts.text(lang, "stats.rooms", Placeholder.unparsed("n", String.valueOf(d.rooms()))));
        lore.add(Texts.text(lang, "stats.downs", Placeholder.unparsed("n", String.valueOf(d.downs()))));
        lore.add(Texts.text(lang, "stats.taler", Placeholder.unparsed("n", String.valueOf(d.taler()))));
        lore.add(Texts.text(lang, "stats.score", Placeholder.unparsed("n", String.valueOf(d.bestScore()))));
        for (Difficulty diff : Difficulty.values()) {
            long t = d.bestTime(diff);
            lore.add(Texts.text(lang, "stats.best", Placeholder.component("difficulty",
                    Texts.text(lang, "difficulty." + diff.id())), Placeholder.unparsed("time",
                    t == 0 ? "–" : Ui.time(t))));
        }
        head.editMeta(meta -> {
            meta.displayName(Texts.text(lang, "stats.head", Placeholder.unparsed("name", d.name())));
            meta.lore(lore);
        });
        menu.set(4, head, null);
        menu.set(20, Items.named(Material.GOLD_BLOCK, 1, Texts.text(lang, "board.score"),
                board(lang, plugin.board().byScore(), false)), null);
        menu.set(24, Items.named(Material.CLOCK, 1, Texts.text(lang, "board.time"),
                board(lang, plugin.board().byTime(), true)), null);
        menu.open(p);
    }

    private static List<Component> board(String lang, List<Leaderboard.Entry> entries, boolean time) {
        List<Component> out = new ArrayList<>();
        if (entries.isEmpty()) {
            out.add(Texts.text(lang, "board.empty"));
        }
        for (int i = 0; i < entries.size(); i++) {
            Leaderboard.Entry e = entries.get(i);
            out.add(Texts.text(lang, "board.line", Placeholder.unparsed("pos", String.valueOf(i + 1)),
                    Placeholder.unparsed("names", e.names()),
                    Placeholder.unparsed("value", time ? Ui.time(e.timeMs()) : String.valueOf(e.score())),
                    Placeholder.component("difficulty", Texts.text(lang, "difficulty." + e.difficulty().id()))));
        }
        return out;
    }

    // ---------- Spuren ----------

    public void trails(Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.trails.title"));
        Runnable draw = () -> drawTrails(menu, p);
        draw.run();
        menu.open(p);
    }

    private void drawTrails(Menu menu, Player p) {
        String lang = plugin.lang(p);
        PlayerData d = plugin.data(p);
        for (int i = 0; i < 27; i++) {
            menu.set(i, frame(), null);
        }
        Material[] icons = {Material.BARRIER, Material.TORCH, Material.LIGHTNING_ROD, Material.SOUL_TORCH,
            Material.GOLD_NUGGET};
        Cosmetics.Trail[] all = Cosmetics.Trail.values();
        for (int i = 0; i < all.length; i++) {
            Cosmetics.Trail t = all[i];
            boolean open = d.unlocked(t);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "trail." + t.key() + ".how"));
            lore.add(Texts.text(lang, d.trail() == t ? "trail.chosen" : open ? "trail.choose" : "trail.locked"));
            ItemStack icon = Items.named(open ? icons[i] : Material.GRAY_DYE, 1, Texts.text(lang, "trail." + t.key()),
                    lore);
            Items.glow(icon, d.trail() == t);
            menu.set(9 + 2 * i, icon, who -> {
                if (!plugin.data(who).unlocked(t)) {
                    Ui.sound(who, "entity.villager.no", 1f);
                    return;
                }
                plugin.data(who).trail(t);
                plugin.store().save(plugin.data(who));
                Ui.sound(who, "ui.button.click", 1.2f);
                drawTrails(menu, who);
            });
        }
    }
}
