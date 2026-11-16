package de.moinmorni.burgverlies.data;

import de.moinmorni.burgverlies.dungeon.Difficulty;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Bestenliste: die besten Abstiege nach Punkten und die schnellsten Siege ({@code bestenliste.yml}). */
public final class Leaderboard {

    public static final int SIZE = 10;

    /** Ein Abstieg auf der Liste ({@code names} = alle, die dabei waren). */
    public record Entry(String names, int score, long timeMs, Difficulty difficulty, int players, boolean won) {
    }

    private final Logger log;
    private final Path file;
    private final List<Entry> byScore = new ArrayList<>();
    private final List<Entry> byTime = new ArrayList<>();

    public Leaderboard(Logger log, Path dataFolder) {
        this.log = log;
        this.file = dataFolder.resolve("bestenliste.yml");
    }

    public Path file() {
        return file;
    }

    public List<Entry> byScore() {
        return List.copyOf(byScore);
    }

    public List<Entry> byTime() {
        return List.copyOf(byTime);
    }

    /** Abstieg eintragen; gibt den Platz nach Punkten zurück (1 …) oder 0. */
    public int add(Entry e) {
        byScore.add(e);
        byScore.sort(Comparator.comparingInt(Entry::score).reversed());
        trim(byScore);
        if (e.won()) {
            byTime.add(e);
            byTime.sort(Comparator.comparingLong(Entry::timeMs));
            trim(byTime);
        }
        return byScore.indexOf(e) + 1;
    }

    private static void trim(List<Entry> l) {
        while (l.size() > SIZE) {
            l.remove(l.size() - 1);
        }
    }

    public void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException e) {
            log.warning("bestenliste.yml ließ sich nicht lesen: " + e.getMessage());
            return;
        }
        read(y.getConfigurationSection("punkte"), byScore);
        read(y.getConfigurationSection("zeit"), byTime);
    }

    private static void read(ConfigurationSection s, List<Entry> into) {
        into.clear();
        if (s == null) {
            return;
        }
        for (String k : s.getKeys(false)) {
            ConfigurationSection e = s.getConfigurationSection(k);
            if (e == null) {
                continue;
            }
            into.add(new Entry(e.getString("namen", "?"), e.getInt("punkte"), e.getLong("zeit"),
                    Difficulty.parse(e.getString("schwierigkeit")).orElse(Difficulty.NORMAL), e.getInt("spieler", 1),
                    e.getBoolean("sieg")));
        }
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        write(y, "punkte", byScore);
        write(y, "zeit", byTime);
        return y.saveToString();
    }

    private static void write(YamlConfiguration y, String key, List<Entry> list) {
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            String p = key + "." + (i + 1);
            y.set(p + ".namen", e.names());
            y.set(p + ".punkte", e.score());
            y.set(p + ".zeit", e.timeMs());
            y.set(p + ".schwierigkeit", e.difficulty().id());
            y.set(p + ".spieler", e.players());
            y.set(p + ".sieg", e.won());
        }
    }
}
