package de.moinmorni.burgverlies.run;

import de.moinmorni.burgverlies.dungeon.Difficulty;
import de.moinmorni.burgverlies.dungeon.FloorPlan;
import de.moinmorni.burgverlies.dungeon.Revives;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ein Abstieg: eine Gruppe (1–4) auf drei Etagen bis zum Kerkermeister. */
public final class Run {

    public enum Stage { BUILDING, PLAYING, ENDED }

    final int id;
    final int slot;
    final long seed;
    final Difficulty difficulty;
    /** Sprache für Namen über den Köpfen (die der Anführerin bzw. des Anführers). */
    final String lang;
    final Map<UUID, Hero> heroes = new LinkedHashMap<>();
    final Revives revives;
    final FloorPlan[] plans = new FloorPlan[3];
    @SuppressWarnings("unchecked")
    final List<Room>[] rooms = new List[3];
    final boolean[] built = new boolean[3];
    Stage stage = Stage.BUILDING;
    int floor;
    int current;
    long startMs;
    int roomsCleared;
    boolean bossKilled;
    final List<UUID> entities = new ArrayList<>();

    Run(int id, int slot, long seed, Difficulty difficulty, String lang, Map<UUID, String> players, int revives,
            int bleedSteps, int reviveSteps) {
        this.id = id;
        this.slot = slot;
        this.seed = seed;
        this.difficulty = difficulty;
        this.lang = lang;
        players.forEach((u, n) -> heroes.put(u, new Hero(u, n)));
        this.revives = new Revives(players.keySet(), revives, bleedSteps, reviveSteps);
    }

    public int id() {
        return id;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public Stage stage() {
        return stage;
    }

    public int floor() {
        return floor;
    }

    public Revives revives() {
        return revives;
    }

    public Hero hero(UUID u) {
        return heroes.get(u);
    }

    /** Alle, die noch dabei sind. */
    public List<Hero> active() {
        return heroes.values().stream().filter(h -> !h.left()).toList();
    }

    /** Zahl der Spieler beim Start (für die Stärke der Gegner). */
    public int size() {
        return heroes.size();
    }

    List<Room> floorRooms() {
        return rooms[floor];
    }

    Room room() {
        return rooms[floor].get(current);
    }

    long elapsed() {
        return startMs == 0 ? 0 : System.currentTimeMillis() - startMs;
    }
}
