package net.morni.burgverlies.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Welche Gegner in welcher Welle kommen – je nach Etage, Raum, Schwierigkeit und Zahl der Spieler. */
public final class Waves {

    private Waves() {
    }

    /** Gegner, die auf einer Etage vorkommen (tiefer = gefährlicher). */
    static List<MobKind> pool(int floor, String templateId) {
        List<MobKind> pool = new ArrayList<>(List.of(MobKind.KERKERWACHE, MobKind.KERKERWACHE, MobKind.BURGSCHUETZE));
        if (floor >= 1) {
            pool.add(MobKind.GRUFTWAECHTER);
        }
        switch (templateId) {
            case "weinkeller" -> {
                pool.add(MobKind.KELLERSPINNE);
                pool.add(MobKind.KELLERSPINNE);
            }
            case "gruft" -> {
                pool.add(MobKind.GRUFTWAECHTER);
                pool.add(MobKind.BURGSCHUETZE);
            }
            case "ruestkammer" -> pool.add(MobKind.KERKERWACHE);
            default -> pool.add(MobKind.KELLERSPINNE);
        }
        return pool;
    }

    /** Wellen für einen Kampfraum. */
    public static List<List<MobKind>> plan(Random rnd, int floor, String templateId, Difficulty d, int players) {
        List<MobKind> pool = pool(floor, templateId);
        List<List<MobKind>> waves = new ArrayList<>();
        int count = Scaling.waves(floor);
        for (int w = 0; w < count; w++) {
            int size = Scaling.waveSize(2 + floor + (w == count - 1 ? 1 : 0), d, players);
            List<MobKind> wave = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                wave.add(pool.get(rnd.nextInt(pool.size())));
            }
            // Auf der untersten Etage führt ein Kettenknecht die letzte Welle an.
            if (floor >= 2 && w == count - 1) {
                wave.set(0, MobKind.KETTENKNECHT);
            }
            waves.add(List.copyOf(wave));
        }
        return waves;
    }

    /** Wachen, die der Kerkermeister ruft. */
    public static List<MobKind> summons(Random rnd, Difficulty d, int players, int phase) {
        int n = Scaling.bossSummons(d, players);
        List<MobKind> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(phase >= 3 && i == 0 ? MobKind.KETTENKNECHT : rnd.nextBoolean() ? MobKind.KERKERWACHE
                    : MobKind.BURGSCHUETZE);
        }
        return out;
    }
}
