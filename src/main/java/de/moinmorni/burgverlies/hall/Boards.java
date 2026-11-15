package de.moinmorni.burgverlies.hall;

import de.moinmorni.burgverlies.BurgverliesPlugin;
import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.Ui;
import de.moinmorni.burgverlies.data.Leaderboard;
import de.moinmorni.burgverlies.world.HallPlan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

/**
 * Schrift in der Luft im Torhaus: Begrüßung und Bestenliste, je Sprache eine eigene Tafel (jede Person sieht nur
 * die in ihrer Sprache). Die Tafeln werden nicht gespeichert, sondern bei jedem Start neu aufgestellt.
 */
public final class Boards {

    private static final String[] LANGS = {"de", "en"};
    private static final String TAG = "bv_board";

    private final BurgverliesPlugin plugin;
    private final Map<String, List<TextDisplay>> displays = new HashMap<>();

    public Boards(BurgverliesPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawn() {
        remove();
        // Reste eines Absturzes
        for (Entity e : plugin.world().getEntitiesByClass(TextDisplay.class)) {
            if (e.getScoreboardTags().contains(TAG)) {
                e.remove();
            }
        }
        int y = HallPlan.FLOOR_Y;
        for (String lang : LANGS) {
            List<TextDisplay> list = new ArrayList<>();
            list.add(display(new Location(plugin.world(), 0.5, y + 4.2, -9.0)));
            list.add(display(new Location(plugin.world(), -8.5, y + 3.2, -4.5)));
            list.add(display(new Location(plugin.world(), 9.5, y + 3.2, -4.5)));
            displays.put(lang, list);
        }
        update();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            showFor(p);
        }
    }

    private TextDisplay display(Location l) {
        return plugin.world().spawn(l, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(TAG);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(150, 20, 14, 10));
            d.setShadowed(true);
            d.setLineWidth(260);
            d.setVisibleByDefault(false);
        });
    }

    public void update() {
        for (String lang : LANGS) {
            List<TextDisplay> list = displays.get(lang);
            if (list == null) {
                continue;
            }
            list.get(0).text(Texts.text(lang, "board.welcome"));
            list.get(1).text(top(lang, plugin.board().byScore(), false));
            list.get(2).text(top(lang, plugin.board().byTime(), true));
        }
    }

    private Component top(String lang, List<Leaderboard.Entry> entries, boolean time) {
        Component c = Texts.text(lang, time ? "board.time" : "board.score");
        if (entries.isEmpty()) {
            return c.append(Component.newline()).append(Texts.text(lang, "board.empty"));
        }
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            Leaderboard.Entry e = entries.get(i);
            c = c.append(Component.newline()).append(Texts.text(lang, "board.line",
                    Placeholder.unparsed("pos", String.valueOf(i + 1)),
                    Placeholder.unparsed("names", e.names()),
                    Placeholder.unparsed("value", time ? Ui.time(e.timeMs()) : String.valueOf(e.score())),
                    Placeholder.component("difficulty", Texts.text(lang, "difficulty." + e.difficulty().id()))));
        }
        return c;
    }

    /** Nur die Tafeln in der eigenen Sprache zeigen. */
    public void showFor(Player p) {
        String mine = plugin.lang(p);
        displays.forEach((lang, list) -> {
            for (TextDisplay d : list) {
                if (lang.equals(mine)) {
                    p.showEntity(plugin, d);
                } else {
                    p.hideEntity(plugin, d);
                }
            }
        });
    }

    public void remove() {
        displays.values().forEach(l -> l.forEach(Entity::remove));
        displays.clear();
    }
}
