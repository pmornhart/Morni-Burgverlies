package net.morni.burgverlies.dungeon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoomSpaceTest {

    @Test
    void doorsSpawnsAndInputsStayFree() {
        for (RoomTemplate t : RoomCatalog.all()) {
            int h = t.half();
            for (int a = -1; a <= 1; a++) {
                for (int y = 1; y <= 4; y++) {
                    assertTrue(RoomSpace.reserved(t, a, y, -h), t.id());
                    assertTrue(RoomSpace.reserved(t, a, y, h), t.id());
                    assertTrue(RoomSpace.reserved(t, h, y, a), t.id());
                    assertTrue(RoomSpace.reserved(t, -h, y, a), t.id());
                }
            }
            for (Pos s : t.spawns()) {
                assertTrue(RoomSpace.reserved(t, s.x(), 1, s.z()) && RoomSpace.reserved(t, s.x(), 2, s.z()), t.id());
            }
            for (Pos c : t.chests()) {
                assertTrue(RoomSpace.reserved(t, c.x(), 1, c.z()), t.id());
            }
            for (Pos in : t.inputs()) {
                assertTrue(RoomSpace.reserved(t, in.x(), in.y(), in.z()), t.id());
            }
            // Mitte immer frei, Ecken oben frei für Schmuck
            assertTrue(RoomSpace.reserved(t, 0, 1, 0), t.id());
            assertFalse(RoomSpace.reserved(t, h, 5, h), t.id());
            // Außerhalb des Innenraums nie
            assertFalse(RoomSpace.reserved(t, h + 1, 1, 0));
        }
    }

    @Test
    void trapCorridorAndShopStairsStayFree() {
        RoomTemplate trap = RoomCatalog.byId("fallengang").orElseThrow();
        for (int x = -7; x <= 7; x++) {
            for (int z = -6; z <= 6; z++) {
                assertTrue(RoomSpace.reserved(trap, x, 2, z));
            }
        }
        RoomTemplate shop = RoomCatalog.byId("kraemerstube").orElseThrow();
        assertTrue(RoomSpace.reserved(shop, 0, 1, 5));
        assertTrue(RoomSpace.reserved(shop, -2, 1, -2), "Platz vor der Theke");
        assertFalse(RoomSpace.reserved(shop, -7, 1, -5), "Regal an der Wand");
        RoomTemplate boss = RoomCatalog.byId("thronsaal").orElseThrow();
        assertTrue(RoomSpace.reserved(boss, 0, 1, 5));
        assertTrue(RoomSpace.reserved(boss, 5, 1, 5), "Kampffläche");
        assertFalse(RoomSpace.reserved(boss, 6, 1, 6), "Säule");
        assertFalse(RoomSpace.reserved(boss, 11, 1, 11), "Ecke für Schmuck");
    }
}
