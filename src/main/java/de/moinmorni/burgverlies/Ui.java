package de.moinmorni.burgverlies;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/** Kleine Helfer für Titel, Töne und Zahlen. */
public final class Ui {

    private static final long FADE_IN_MS = 100;
    private static final long FADE_OUT_MS = 250;

    private Ui() {
    }

    public static void title(Player p, Component main, Component sub, long stayMs) {
        p.showTitle(Title.title(main, sub, Title.Times.times(Duration.ofMillis(FADE_IN_MS), Duration.ofMillis(stayMs),
                Duration.ofMillis(FADE_OUT_MS))));
    }

    public static void sound(Player p, String key, float pitch) {
        p.playSound(Sound.sound(Key.key(key), Sound.Source.MASTER, 1f, pitch));
    }

    /** Sekunden als „1:05“. */
    public static String clock(long seconds) {
        long s = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /** Herzen mit einer Nachkommastelle, deutsch mit Komma. */
    public static String hearts(double health, String lang) {
        String s = String.format(Locale.ROOT, "%.1f", Math.max(0, health) / 2.0);
        return "en".equals(lang) ? s : s.replace('.', ',');
    }

    /** Sekunden mit einer Nachkommastelle. */
    public static String seconds(long millis, String lang) {
        String s = String.format(Locale.ROOT, "%.1f", Math.max(0, millis) / 1000.0);
        return "en".equals(lang) ? s : s.replace('.', ',');
    }

    /** Bedrock-Spieler (über Geyser/Floodgate) haben UUIDs, die mit lauter Nullen beginnen. */
    public static boolean bedrock(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }

    /** Fortschrittsbalken aus Blöcken, z. B. „▮▮▮▯▯“. */
    public static String bar(double share, int width) {
        int full = (int) Math.round(Math.max(0, Math.min(1, share)) * width);
        return "▮".repeat(full) + "▯".repeat(width - full);
    }

    /** Millisekunden als „12:05“ bzw. „1:02:03“. */
    public static String time(long millis) {
        long s = Math.max(0, millis / 1000);
        return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : clock(s);
    }
}
