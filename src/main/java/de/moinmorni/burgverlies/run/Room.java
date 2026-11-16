package de.moinmorni.burgverlies.run;

import de.moinmorni.burgverlies.dungeon.MobKind;
import de.moinmorni.burgverlies.dungeon.PlacedRoom;
import de.moinmorni.burgverlies.dungeon.Puzzle;
import de.moinmorni.burgverlies.dungeon.TrapLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Ein Raum während eines Abstiegs: Zustand und was gerade darin passiert. */
public final class Room {

    public enum Phase { WAITING, ACTIVE, CLEARED }

    final PlacedRoom placed;
    Phase phase = Phase.WAITING;
    /** Spielticks, seit der Raum läuft. */
    long ticks;

    // Kampf (auch Strafe im Rätsel und Wachen des Kerkermeisters)
    List<List<MobKind>> waves = List.of();
    int wave = -1;
    int waveDelay = -1;
    final Set<UUID> mobs = new LinkedHashSet<>();

    // Rätsel
    Puzzle puzzle;
    final Map<Long, Integer> inputs = new HashMap<>();
    final Map<UUID, Long> plateCooldown = new HashMap<>();
    int showStep = -1;
    long bellReady;

    // Fallen
    TrapLayout crumble;
    final Map<Long, Integer> crumbling = new HashMap<>();
    int trapFires;
    int falls;

    // Schatz
    final Set<Long> opened = new HashSet<>();

    // Krämerin
    UUID npc;
    int stairsTicks;

    // Kerkermeister
    Boss boss;
    int intro = -1;

    final List<UUID> extras = new ArrayList<>();

    Room(PlacedRoom placed) {
        this.placed = placed;
    }

    public PlacedRoom placed() {
        return placed;
    }

    public Phase phase() {
        return phase;
    }

    public int index() {
        return placed.index();
    }

    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
