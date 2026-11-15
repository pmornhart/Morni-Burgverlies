package de.moinmorni.burgverlies.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DungeonGeneratorTest {

    private static final int SEEDS = 400;

    @Test
    void sameSeedGivesTheSameFloor() {
        for (int floor = 0; floor < 3; floor++) {
            FloorPlan a = DungeonGenerator.generate(42, floor, 5);
            FloorPlan b = DungeonGenerator.generate(42, floor, 5);
            assertEquals(a, b);
        }
        assertNotEquals(DungeonGenerator.generate(1, 0, 5), DungeonGenerator.generate(2, 0, 5));
    }

    @Test
    void roomsAreConnectedAndNeverOverlap() {
        for (long seed = 0; seed < SEEDS; seed++) {
            for (int floor = 0; floor < 3; floor++) {
                for (int middle = DungeonGenerator.MIN_MIDDLE; middle <= DungeonGenerator.MAX_MIDDLE; middle++) {
                    FloorPlan plan = DungeonGenerator.generate(seed, floor, middle);
                    Set<Long> cells = new HashSet<>();
                    for (int i = 0; i < plan.size(); i++) {
                        PlacedRoom r = plan.room(i);
                        assertEquals(i, r.index());
                        assertTrue(cells.add(((long) r.cellX() << 32) ^ (r.cellZ() & 0xffffffffL)),
                                "Feld doppelt: seed " + seed);
                        assertTrue(Math.abs(r.cellX()) <= DungeonGenerator.GRID_X && r.cellZ() >= 0
                                && r.cellZ() <= DungeonGenerator.GRID_Z, "außerhalb des Rasters");
                        if (i == 0) {
                            assertNull(r.entry());
                            assertEquals(Dir.SOUTH, r.exit(), "erst einmal tiefer hinein");
                        } else {
                            PlacedRoom prev = plan.room(i - 1);
                            assertNotNull(prev.exit());
                            assertEquals(prev.exit().opposite(), r.entry(), "Tür passt zur Tür davor");
                            assertEquals(prev.cellX() + prev.exit().dx(), r.cellX());
                            assertEquals(prev.cellZ() + prev.exit().dz(), r.cellZ());
                            assertNotEquals(r.entry(), r.exit(), "Ein- und Ausgang in derselben Wand");
                        }
                        // Vorlage gedreht: Vorlagen-Norden = Eingang (bzw. gegenüber vom Ausgang)
                        Dir north = Dir.NORTH.rotate(r.rotation());
                        assertEquals(r.entry() != null ? r.entry() : r.exit().opposite(), north);
                        if (r.exit() != null) {
                            assertNotEquals(Dir.NORTH, r.localExit(), "Ausgang nie in der Eingangswand");
                        }
                        if (r.template().straightOnly()) {
                            assertEquals(Dir.SOUTH, r.localExit(), "Fallengang nur geradeaus");
                        }
                        assertTrue(r.template().minFloor() <= floor);
                    }
                    assertNull(plan.last().exit());
                }
            }
        }
    }

    @Test
    void everyFloorHasTheRightRooms() {
        for (long seed = 0; seed < SEEDS; seed++) {
            for (int floor = 0; floor < 3; floor++) {
                int middle = 3 + (int) (seed % 6);
                FloorPlan plan = DungeonGenerator.generate(seed, floor, middle);
                List<RoomType> types = plan.rooms().stream().map(PlacedRoom::type).toList();
                assertEquals(RoomType.START, types.get(0));
                assertEquals(RoomType.FIGHT, types.get(1), "erst ein Kampf");
                assertTrue(types.contains(RoomType.TRAP));
                assertTrue(types.contains(RoomType.PUZZLE));
                if (middle >= 4) {
                    assertTrue(types.contains(RoomType.TREASURE));
                }
                if (floor == 2) {
                    assertEquals(RoomType.BOSS, types.get(types.size() - 1));
                    assertEquals(RoomType.REST, types.get(types.size() - 2), "Rast vor dem Kerkermeister");
                    assertEquals(middle + 3, types.size());
                } else {
                    assertEquals(RoomType.SHOP, types.get(types.size() - 1), "Krämerin vor der Treppe");
                    assertFalse(types.contains(RoomType.BOSS));
                    assertEquals(middle + 2, types.size());
                }
                for (int i = 1; i < types.size(); i++) {
                    RoomType a = types.get(i - 1);
                    if (a == types.get(i)) {
                        assertEquals(RoomType.FIGHT, a, "zweimal " + a + " hintereinander");
                    }
                    assertNotEquals(plan.room(i - 1).template().id(), plan.room(i).template().id(),
                            "dieselbe Vorlage zweimal hintereinander");
                }
            }
        }
    }

    @Test
    void puzzlesAndTrapsInEveryGeneratedFloorAreSolvable() {
        for (long seed = 0; seed < SEEDS; seed++) {
            for (int floor = 0; floor < 3; floor++) {
                for (PlacedRoom r : DungeonGenerator.generate(seed, floor, 6).rooms()) {
                    RoomTemplate t = r.template();
                    if (t.type() == RoomType.PUZZLE) {
                        Puzzle p = Puzzle.create(new java.util.Random(seed), t.inputs().size(),
                                Puzzle.lengthFor(floor));
                        for (int input : p.order()) {
                            assertTrue(input >= 0 && input < t.inputs().size());
                            p.press(input);
                        }
                        assertTrue(p.solved());
                    }
                    if (t.trap() != null && t.trap().crumbles()) {
                        TrapLayout l = TrapLayout.create(new java.util.Random(seed), t.half(),
                                t.trap().crumbleMin(), t.trap().crumbleMax(), 0.6);
                        assertTrue(l.solvable(), "Fallengang ohne sicheren Weg: seed " + seed);
                    }
                }
            }
        }
    }

    @Test
    void templatesKeepTheDoorsFree() {
        for (RoomTemplate t : RoomCatalog.all()) {
            List<Pos> blocking = new java.util.ArrayList<>(t.chests());
            if (t.npc() != null && t.type() != RoomType.BOSS) {
                blocking.add(t.npc());
            }
            if (t.bell() != null) {
                blocking.add(t.bell());
            }
            for (Pos p : blocking) {
                assertTrue(t.inside(p), t.id() + " " + p);
                boolean nearDoor = (Math.abs(p.x()) <= 1 && Math.abs(p.z()) >= t.half() - 1)
                        || (Math.abs(p.z()) <= 1 && Math.abs(p.x()) >= t.half() - 1);
                assertFalse(nearDoor, t.id() + ": " + p + " steht vor einer Tür");
            }
            for (Pos p : t.spawns()) {
                assertTrue(t.inside(p), t.id() + " Gegner außerhalb: " + p);
            }
            if (t.type() == RoomType.PUZZLE) {
                assertTrue(t.inputs().size() >= 2);
                assertNotNull(t.bell());
            }
        }
        for (RoomType type : RoomType.values()) {
            for (int floor = 0; floor < 3; floor++) {
                if (type == RoomType.BOSS && floor < 2) {
                    continue;
                }
                assertFalse(RoomCatalog.of(type, floor).isEmpty(), type + " auf Etage " + floor);
            }
        }
    }

    @Test
    void rotationTurnsNorthTheSameWayAsDirections() {
        Pos door = new Pos(0, 1, -8);
        for (int q = 0; q < 4; q++) {
            Dir d = Dir.NORTH.rotate(q);
            Pos turned = door.rotate(q);
            assertEquals(d.dx() * 8, turned.x());
            assertEquals(d.dz() * 8, turned.z());
        }
        assertEquals(new Pos(3, 1, 5), new Pos(3, 1, 5).rotate(4));
    }
}
