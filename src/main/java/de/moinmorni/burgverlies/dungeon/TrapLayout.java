package de.moinmorni.burgverlies.dungeon;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Bröckelnder Boden eines Fallengangs: gesprungene Steinziegel geben nach, wenn man darauf steht (sichtbar, also
 * fair). Es gibt immer einen sicheren Weg vom Eingang zum Ziel – er wird zuerst gewürfelt, der Rest bröckelt zum
 * Teil.
 */
public final class TrapLayout {

    private final Set<Long> crumbling = new HashSet<>();
    private final int half;
    private final int zMin;
    private final int zMax;

    private TrapLayout(int half, int zMin, int zMax) {
        this.half = half;
        this.zMin = zMin;
        this.zMax = zMax;
    }

    public static TrapLayout create(Random rnd, int half, int zMin, int zMax, double share) {
        TrapLayout t = new TrapLayout(half, zMin, zMax);
        Set<Long> safe = new HashSet<>();
        int x = rnd.nextInt(2 * half - 1) - (half - 1);
        for (int z = zMin; z <= zMax; z++) {
            safe.add(key(x, z));
            // Ein paar Schritte seitwärts, damit der Weg nicht gerade ist.
            int side = rnd.nextInt(5) - 2;
            int target = Math.max(-half + 1, Math.min(half - 1, x + side));
            while (x != target) {
                x += Integer.signum(target - x);
                safe.add(key(x, z));
            }
        }
        for (int z = zMin; z <= zMax; z++) {
            for (int xx = -half; xx <= half; xx++) {
                if (!safe.contains(key(xx, z)) && rnd.nextDouble() < share) {
                    t.crumbling.add(key(xx, z));
                }
            }
        }
        return t;
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public boolean crumbles(int x, int z) {
        return crumbling.contains(key(x, z));
    }

    /** Erste bröckelnde Stelle {x, z} in Lauf-Richtung (für Tests), sonst {@code null}. */
    public int[] first() {
        for (int z = zMin; z <= zMax; z++) {
            for (int x = -half; x <= half; x++) {
                if (crumbles(x, z)) {
                    return new int[] {x, z};
                }
            }
        }
        return null;
    }

    public int count() {
        return crumbling.size();
    }

    public int zMin() {
        return zMin;
    }

    public int zMax() {
        return zMax;
    }

    /** Gibt es einen Weg über festen Boden von der Reihe vor dem Fallenteil bis hinter ihn? */
    public boolean solvable() {
        Deque<long[]> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        for (int x = -half; x <= half; x++) {
            queue.add(new long[] {x, zMin - 1});
            seen.add(key(x, zMin - 1));
        }
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            long[] c = queue.poll();
            int cx = (int) c[0];
            int cz = (int) c[1];
            if (cz > zMax) {
                return true;
            }
            for (int[] s : steps) {
                int nx = cx + s[0];
                int nz = cz + s[1];
                if (Math.abs(nx) > half || nz < zMin - 1 || nz > zMax + 1 || crumbles(nx, nz)
                        || !seen.add(key(nx, nz))) {
                    continue;
                }
                queue.add(new long[] {nx, nz});
            }
        }
        return false;
    }
}
