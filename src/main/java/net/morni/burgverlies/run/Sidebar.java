package net.morni.burgverlies.run;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/** Leiste rechts: jede Person hat ihre eigene (ohne rote Zahlen). */
public final class Sidebar {

    private static final String OBJECTIVE = "bv_leiste";
    private static final int MAX_LINES = 15;

    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public void show(Player p, Component title, List<Component> lines) {
        Scoreboard sb = boards.computeIfAbsent(p.getUniqueId(), u -> {
            Scoreboard s = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective o = s.registerNewObjective(OBJECTIVE, Criteria.DUMMY, title);
            o.setDisplaySlot(DisplaySlot.SIDEBAR);
            o.numberFormat(NumberFormat.blank());
            return s;
        });
        Objective o = sb.getObjective(OBJECTIVE);
        if (o == null) {
            return;
        }
        o.displayName(title);
        int n = Math.min(MAX_LINES, lines.size());
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = entry(i);
            if (i < n) {
                Score s = o.getScore(entry);
                s.setScore(n - i);
                s.customName(lines.get(i));
            } else {
                sb.resetScores(entry);
            }
        }
        if (p.getScoreboard() != sb) {
            p.setScoreboard(sb);
        }
    }

    public void remove(UUID u) {
        boards.remove(u);
    }

    private static String entry(int i) {
        return "§" + Integer.toHexString(i) + "§r";
    }
}
