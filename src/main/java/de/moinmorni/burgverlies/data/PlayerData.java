package de.moinmorni.burgverlies.data;

import de.moinmorni.burgverlies.dungeon.Cosmetics;
import de.moinmorni.burgverlies.dungeon.Difficulty;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Werte eines Spielers im Burgverlies (dauerhaft, {@code spieler/<uuid>.yml}). */
public final class PlayerData {

    private final UUID uuid;
    private String name;
    private int runs;
    private int wins;
    private int bosses;
    private int rooms;
    private int downs;
    private long taler;
    private int bestScore;
    private final Map<Difficulty, Integer> winsBy = new EnumMap<>(Difficulty.class);
    private final Map<Difficulty, Long> bestTime = new EnumMap<>(Difficulty.class);
    private Cosmetics.Trail trail = Cosmetics.Trail.NONE;
    private Difficulty chosen = Difficulty.NORMAL;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            this.name = n;
        }
    }

    public int runs() {
        return runs;
    }

    public int wins() {
        return wins;
    }

    public int bosses() {
        return bosses;
    }

    public int rooms() {
        return rooms;
    }

    public int downs() {
        return downs;
    }

    public long taler() {
        return taler;
    }

    public int bestScore() {
        return bestScore;
    }

    public int wins(Difficulty d) {
        return winsBy.getOrDefault(d, 0);
    }

    /** Beste Zeit eines Sieges auf dieser Schwierigkeit (0 = noch keine). */
    public long bestTime(Difficulty d) {
        return bestTime.getOrDefault(d, 0L);
    }

    public Cosmetics.Trail trail() {
        return trail;
    }

    public void trail(Cosmetics.Trail t) {
        this.trail = t == null ? Cosmetics.Trail.NONE : t;
    }

    public Difficulty chosen() {
        return chosen;
    }

    public void chosen(Difficulty d) {
        if (d != null) {
            this.chosen = d;
        }
    }

    public boolean unlocked(Cosmetics.Trail t) {
        return t.unlocked(runs, bosses, wins(Difficulty.SCHWER), rooms);
    }

    /**
     * Ein Abstieg ist vorbei.
     *
     * @return {@code true}, wenn die Zeit eine neue Bestzeit ist
     */
    public boolean record(Difficulty d, boolean won, boolean boss, int roomsCleared, int downCount, long earned,
            int score, long timeMs) {
        runs++;
        rooms += Math.max(0, roomsCleared);
        downs += Math.max(0, downCount);
        taler += Math.max(0, earned);
        bestScore = Math.max(bestScore, score);
        if (boss) {
            bosses++;
        }
        if (!won) {
            return false;
        }
        wins++;
        winsBy.merge(d, 1, Integer::sum);
        long old = bestTime(d);
        if (old == 0 || timeMs < old) {
            bestTime.put(d, timeMs);
            return true;
        }
        return false;
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("abstiege", runs);
        y.set("siege", wins);
        y.set("kerkermeister", bosses);
        y.set("raeume", rooms);
        y.set("gefallen", downs);
        y.set("taler", taler);
        y.set("beste-punkte", bestScore);
        for (Difficulty d : Difficulty.values()) {
            y.set("siege-" + d.id(), wins(d));
            y.set("bestzeit-" + d.id(), bestTime(d));
        }
        y.set("spur", trail.key());
        y.set("schwierigkeit", chosen.id());
        return y.saveToString();
    }

    public static PlayerData fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        PlayerData d = new PlayerData(uuid, y.getString("name", "?"));
        d.runs = y.getInt("abstiege");
        d.wins = y.getInt("siege");
        d.bosses = y.getInt("kerkermeister");
        d.rooms = y.getInt("raeume");
        d.downs = y.getInt("gefallen");
        d.taler = y.getLong("taler");
        d.bestScore = y.getInt("beste-punkte");
        for (Difficulty diff : Difficulty.values()) {
            int w = y.getInt("siege-" + diff.id());
            if (w > 0) {
                d.winsBy.put(diff, w);
            }
            long t = y.getLong("bestzeit-" + diff.id());
            if (t > 0) {
                d.bestTime.put(diff, t);
            }
        }
        for (Cosmetics.Trail t : Cosmetics.Trail.values()) {
            if (t.key().equals(y.getString("spur"))) {
                d.trail = t;
            }
        }
        d.chosen = Difficulty.parse(y.getString("schwierigkeit")).orElse(Difficulty.NORMAL);
        return d;
    }
}
