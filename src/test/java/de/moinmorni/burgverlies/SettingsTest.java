package de.moinmorni.burgverlies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SettingsTest {

    private static Settings of(String yaml) throws Exception {
        YamlConfiguration c = new YamlConfiguration();
        c.loadFromString(yaml);
        return Settings.load(c);
    }

    @Test
    void shippedConfigHoldsThePlaceTwoMinutes() throws Exception {
        try (var in = new InputStreamReader(SettingsTest.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            assertEquals(120, Settings.load(YamlConfiguration.loadConfiguration(in)).returnSeconds());
        }
    }

    @Test
    void returnSecondsDefaultOffAndLimits() throws Exception {
        assertEquals(120, of("welt: burgverlies").returnSeconds(), "fehlt der Wert, gilt die Vorgabe");
        assertEquals(0, of("rueckkehr-sekunden: 0").returnSeconds(), "0 = aus");
        assertEquals(0, of("rueckkehr-sekunden: -3").returnSeconds());
        assertEquals(600, of("rueckkehr-sekunden: 9999").returnSeconds());
        assertEquals(12, of("rueckkehr-sekunden: 12").returnSeconds());
    }
}
