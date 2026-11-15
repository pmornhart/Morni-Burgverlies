package de.moinmorni.burgverlies;

import org.bukkit.configuration.file.FileConfiguration;

/** Werte aus der config.yml (Erklärungen stehen dort). */
public record Settings(
        String world,
        int maxRuns,
        int roomsPerFloor,
        int searchSeconds,
        int revives,
        int reviveSeconds,
        int bleedSeconds,
        int stairsSeconds,
        int endSeconds,
        int waveDelayTicks) {

    private static final int MAX_RUNS = 20;

    static Settings load(FileConfiguration c) {
        return new Settings(
                c.getString("welt", "burgverlies"),
                clamp(c.getInt("max-abstiege", 6), 1, MAX_RUNS),
                clamp(c.getInt("raeume-pro-etage", 5), 3, 8),
                clamp(c.getInt("suche.warten-sekunden", 30), 3, 600),
                clamp(c.getInt("wiederbelebung.anzahl", 2), 0, 9),
                clamp(c.getInt("wiederbelebung.schleichen-sekunden", 3), 1, 20),
                clamp(c.getInt("wiederbelebung.ausbluten-sekunden", 0), 0, 600),
                clamp(c.getInt("treppe-sekunden", 3), 1, 30),
                clamp(c.getInt("ende-sekunden", 8), 2, 60),
                clamp(c.getInt("wellen-pause-ticks", 30), 10, 200));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
