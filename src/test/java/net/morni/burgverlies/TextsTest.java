package net.morni.burgverlies;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.morni.burgverlies.dungeon.Cosmetics;
import net.morni.burgverlies.dungeon.Difficulty;
import net.morni.burgverlies.dungeon.MobKind;
import net.morni.burgverlies.dungeon.Offer;
import net.morni.burgverlies.dungeon.RoomCatalog;
import net.morni.burgverlies.dungeon.RoomTemplate;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "em", "au", "bl", "gl", "red", "green",
            "dark_gray", "b", "bold", "gold", "yellow", "light_purple");
    // Tags wie <name> oder </gray> – aber nicht \<spieler> (maskiert).
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E%20%5C%3Cspieler%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");
    // Text-Schlüssel im Quelltext, z. B. "room.cleared" – Klänge (sound(…, "entity.…")) werden ausgelassen.
    // https://regex101.com/?flavor=java&regex=%22(%5Ba-z%5D%5Ba-z0-9_-%5D*(%3F%3A%5C.%5Ba-z0-9_-%5D%2B)%2B)%22&testString=Texts.msg(lang%2C%20%22room.cleared%22)
    private static final Pattern KEY = Pattern.compile("\"([a-z][a-z0-9_-]*(?:\\.[a-z0-9_-]+)+)\"");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("bl", Tag.styling(TextColor.color(Texts.BLUE))),
                        TagResolver.resolver("gl", Tag.styling(TextColor.color(Texts.EMBER)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wie 🏆 wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    /** Jeder Schlüssel, den der Code benutzt, gibt es auch – sonst stünde im Spiel nur „room.cleared“. */
    @Test
    void everyKeyInTheCodeExists() throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = Files.readString(f);
                Matcher m = KEY.matcher(src);
                while (m.find()) {
                    String key = m.group(1);
                    String before = src.substring(Math.max(0, m.start() - 60), m.start());
                    String line = before.substring(before.lastIndexOf('\n') + 1);
                    if (line.contains("ound") || line.contains("Key(") || line.contains("createBlockData")
                            || line.contains("info(") || line.contains("warning(") || key.startsWith("minecraft.")
                            || src.contains("\"" + key + "\" +") || !looksLikeText(key)) {
                        continue;
                    }
                    if (!Texts.keys().contains(key)) {
                        missing.add(f.getFileName() + ": " + key);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Fehlende Texte: " + missing);
    }

    private static boolean looksLikeText(String key) {
        String first = key.substring(0, key.indexOf('.'));
        return Texts.keys().stream().anyMatch(k -> k.startsWith(first + "."));
    }

    /** Schlüssel, die der Code zusammensetzt („room." + id …). */
    @Test
    void composedKeysExist() {
        List<String> keys = new ArrayList<>();
        for (RoomTemplate t : RoomCatalog.all()) {
            keys.add("room." + t.id());
        }
        for (MobKind k : MobKind.values()) {
            keys.add("mob." + k.key());
        }
        for (Offer o : Offer.values()) {
            keys.add("shop." + o.key());
            keys.add("shop." + o.key() + ".desc");
        }
        for (Difficulty d : Difficulty.values()) {
            keys.add("difficulty." + d.id());
            keys.add("difficulty." + d.id() + ".desc");
        }
        for (Cosmetics.Trail t : Cosmetics.Trail.values()) {
            keys.add("trail." + t.key());
            keys.add("trail." + t.key() + ".how");
        }
        for (int f = 0; f < 3; f++) {
            keys.add("floor." + f);
        }
        for (int phase = 2; phase <= 3; phase++) {
            keys.add("boss.phase" + phase + ".title");
            keys.add("boss.phase" + phase + ".subtitle");
        }
        for (String a : new String[] {"shockwave", "summon", "chains"}) {
            keys.add("boss.warn." + a);
        }
        for (String l : new String[] {"arrows", "potion", "golden_apple"}) {
            keys.add("loot." + l);
        }
        for (String s : new String[] {"up", "down", "out"}) {
            keys.add("side.member." + s);
        }
        for (String k : keys) {
            assertTrue(Texts.keys().contains(k), "fehlt: " + k);
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "win.title"), Texts.raw("en", "win.title"));
        assertEquals(Texts.raw("de", "boss.name"), Texts.raw("fr", "boss.name"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
        assertEquals("Der Kerkermeister", Texts.raw("de", "boss.name"));
        assertTrue(Texts.raw("en", "room.thronsaal").toLowerCase(Locale.ROOT).contains("gaoler"));
    }

    @Test
    void numbersAndBars() {
        assertEquals("3,5", Ui.hearts(7.0, "de"));
        assertEquals("1:05", Ui.clock(65));
        assertEquals("12:05", Ui.time(725_000));
        assertEquals("1:02:03", Ui.time(3_723_000));
        assertEquals("▮▮▮▯▯", Ui.bar(0.6, 5));
        assertEquals("▯▯▯", Ui.bar(-1, 3));
    }
}
