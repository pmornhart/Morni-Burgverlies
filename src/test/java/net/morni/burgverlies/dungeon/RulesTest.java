package net.morni.burgverlies.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Schwierigkeit, Belohnung, Krämerin, Punkte, Fallen-Takt und Suche. */
class RulesTest {

    @Test
    void harderAndMorePlayersMeanStrongerEnemies() {
        for (int p = 1; p < 4; p++) {
            assertTrue(Scaling.mobHealth(20, Difficulty.NORMAL, p + 1) > Scaling.mobHealth(20, Difficulty.NORMAL, p));
            assertTrue(Scaling.waveSize(3, Difficulty.NORMAL, p + 1) >= Scaling.waveSize(3, Difficulty.NORMAL, p));
            assertTrue(Scaling.bossHealth(Difficulty.NORMAL, p + 1) >= Scaling.bossHealth(Difficulty.NORMAL, p));
        }
        assertTrue(Scaling.mobHealth(20, Difficulty.LEICHT, 2) < Scaling.mobHealth(20, Difficulty.NORMAL, 2));
        assertTrue(Scaling.mobHealth(20, Difficulty.NORMAL, 2) < Scaling.mobHealth(20, Difficulty.SCHWER, 2));
        assertTrue(Scaling.mobDamage(Difficulty.LEICHT, 1) < Scaling.mobDamage(Difficulty.SCHWER, 1));
        // Minecraft-Grenze und Ober-/Untergrenzen
        assertTrue(Scaling.bossHealth(Difficulty.SCHWER, 4) <= Scaling.MAX_HEALTH);
        assertEquals(Scaling.bossHealth(Difficulty.NORMAL, 4), Scaling.bossHealth(Difficulty.NORMAL, 9));
        assertEquals(Scaling.mobHealth(20, Difficulty.NORMAL, 1), Scaling.mobHealth(20, Difficulty.NORMAL, 0));
        assertTrue(Scaling.waveSize(50, Difficulty.SCHWER, 4) <= Scaling.MAX_WAVE);
        assertEquals(260, Scaling.bossHealth(Difficulty.NORMAL, 1));
    }

    @Test
    void wavesGrowWithFloorAndPlayers() {
        List<List<MobKind>> small = Waves.plan(new Random(1), 0, "wachstube", Difficulty.NORMAL, 1);
        List<List<MobKind>> big = Waves.plan(new Random(1), 2, "gruft", Difficulty.SCHWER, 4);
        assertEquals(2, small.size());
        assertEquals(3, big.size());
        int a = small.stream().mapToInt(List::size).sum();
        int b = big.stream().mapToInt(List::size).sum();
        assertTrue(b > a);
        assertEquals(MobKind.KETTENKNECHT, big.get(2).get(0), "unten führt ein Kettenknecht die letzte Welle");
        small.forEach(w -> assertFalse(w.isEmpty()));
        assertFalse(small.stream().flatMap(List::stream).anyMatch(k -> k == MobKind.KETTENKNECHT));
        assertEquals(Waves.plan(new Random(5), 1, "kerker", Difficulty.NORMAL, 2),
                Waves.plan(new Random(5), 1, "kerker", Difficulty.NORMAL, 2));
    }

    @Test
    void lootIsFairAndHarderPaysMore() {
        Random rnd = new Random(3);
        for (int i = 0; i < 200; i++) {
            int t = Loot.chest(rnd, 0, Difficulty.NORMAL);
            assertTrue(t >= Math.round(15 * 1.25) && t <= Math.round(25 * 1.25), "Truhe: " + t);
        }
        assertTrue(Loot.kill(MobKind.KERKERWACHE, 0, Difficulty.SCHWER) > Loot.kill(MobKind.KERKERWACHE, 0,
                Difficulty.LEICHT));
        assertTrue(Loot.kill(MobKind.KERKERWACHE, 2, Difficulty.NORMAL) > Loot.kill(MobKind.KERKERWACHE, 0,
                Difficulty.NORMAL));
        assertEquals(0, Loot.roomBonus(RoomType.TREASURE, 1, Difficulty.NORMAL));
        assertTrue(Loot.roomBonus(RoomType.BOSS, 2, Difficulty.NORMAL) > Loot.roomBonus(RoomType.FIGHT, 2,
                Difficulty.NORMAL));
        int[] counts = new int[Loot.Item.values().length];
        Random r = new Random(9);
        for (int i = 0; i < 10_000; i++) {
            counts[Loot.chestItem(r, 0).ordinal()]++;
        }
        for (int c : counts) {
            assertTrue(c > 500, "jede Sorte kommt vor");
        }
    }

    @Test
    void shopPricesAndLimits() {
        assertEquals(40, ShopMath.price(Offer.SWORD, 0, 0));
        assertEquals(80, ShopMath.price(Offer.SWORD, 1, 0));
        assertEquals(120, ShopMath.price(Offer.SWORD, 2, 1), "Verbesserungen kosten auf jeder Etage gleich");
        assertEquals(20, ShopMath.price(Offer.POTION, 0, 0));
        assertEquals(25, ShopMath.price(Offer.POTION, 0, 1), "tiefer wird Verbrauchsware teurer");
        assertEquals(120, ShopMath.price(Offer.REVIVE, 1, 0));
        ShopMath.Purchase ok = ShopMath.buy(100, Offer.SWORD, 0, 0);
        assertEquals(ShopMath.Result.OK, ok.result());
        assertEquals(60, ok.walletAfter());
        assertEquals(1, ok.levelAfter());
        ShopMath.Purchase poor = ShopMath.buy(30, Offer.SWORD, 0, 0);
        assertEquals(ShopMath.Result.TOO_POOR, poor.result());
        assertEquals(30, poor.walletAfter());
        assertEquals(ShopMath.Result.MAXED, ShopMath.buy(9999, Offer.SWORD, 3, 0).result());
        assertEquals(ShopMath.Result.MAXED, ShopMath.buy(9999, Offer.REVIVE, 2, 0).result());
        assertEquals(ShopMath.Result.OK, ShopMath.buy(9999, Offer.POTION, 50, 2).result(), "Tränke unbegrenzt");
        for (Offer o : Offer.values()) {
            for (int lvl = 0; lvl < 3; lvl++) {
                assertEquals(0, ShopMath.price(o, lvl, 1) % 5, "glatte Preise");
            }
        }
    }

    @Test
    void scoreRewardsSpeedAndCaution() {
        int fast = Score.compute(true, 20, 0, 600_000, Difficulty.NORMAL);
        int slow = Score.compute(true, 20, 0, 1_500_000, Difficulty.NORMAL);
        int clumsy = Score.compute(true, 20, 4, 600_000, Difficulty.NORMAL);
        int hard = Score.compute(true, 20, 0, 600_000, Difficulty.SCHWER);
        int lost = Score.compute(false, 7, 3, 300_000, Difficulty.NORMAL);
        assertTrue(fast > slow);
        assertTrue(fast > clumsy);
        assertTrue(hard > fast);
        assertTrue(fast > lost);
        assertEquals(0, Score.compute(false, 0, 9, 1000, Difficulty.SCHWER), "nie unter null");
        assertEquals((20 * 100 + 1500 + 2 * 1200) * 1.5, fast, 0.5);
    }

    @Test
    void puzzleResetsOnMistakes() {
        Puzzle p = new Puzzle(List.of(2, 0, 3));
        assertEquals(Puzzle.Step.PROGRESS, p.press(2));
        assertEquals(Puzzle.Step.WRONG, p.press(1));
        assertEquals(0, p.done());
        assertEquals(1, p.mistakes());
        assertEquals(Puzzle.Step.PROGRESS, p.press(2));
        assertEquals(Puzzle.Step.PROGRESS, p.press(0));
        assertEquals(Puzzle.Step.SOLVED, p.press(3));
        assertEquals(Puzzle.Step.IGNORED, p.press(3));
        for (int seed = 0; seed < 300; seed++) {
            Puzzle q = Puzzle.create(new Random(seed), 4, 5);
            assertEquals(5, q.order().size());
            for (int i = 1; i < q.order().size(); i++) {
                assertTrue(!q.order().get(i).equals(q.order().get(i - 1)), "nie zweimal derselbe Hebel");
            }
            assertEquals(4, Puzzle.create(new Random(seed), 4, 4).order().stream().distinct().count(),
                    "bei vier Schritten kommt jeder Hebel vor");
        }
    }

    @Test
    void trapsAlwaysLeaveAGap() {
        for (long t = 0; t < TrapTiming.JET_PERIOD * 3; t++) {
            boolean a = TrapTiming.jet(t, 0) == TrapTiming.Phase.ACTIVE;
            boolean b = TrapTiming.jet(t, 1) == TrapTiming.Phase.ACTIVE;
            assertFalse(a && b, "beide Düsen-Gruppen gleichzeitig bei Tick " + t);
        }
        int idle = 0;
        for (long t = 0; t < TrapTiming.JET_PERIOD; t++) {
            if (TrapTiming.jet(t, 0) == TrapTiming.Phase.IDLE) {
                idle++;
            }
        }
        assertTrue(idle >= TrapTiming.JET_PERIOD / 4, "Zeit zum Durchlaufen");
        // Jede Glut kündigt sich an
        for (long t = 1; t < TrapTiming.JET_PERIOD * 2; t++) {
            if (TrapTiming.jet(t, 1) == TrapTiming.Phase.ACTIVE && TrapTiming.jet(t - 1, 1) != TrapTiming.Phase.ACTIVE) {
                assertEquals(TrapTiming.Phase.WARN, TrapTiming.jet(t - 1, 1));
            }
        }
        int shots = 0;
        for (long t = 0; t < TrapTiming.ARROW_PERIOD * 4; t++) {
            if (TrapTiming.arrowFires(t, 10)) {
                shots++;
                assertTrue(TrapTiming.arrowWarns(t - TrapTiming.ARROW_WARN, 10), "Rauch vor dem Schuss");
            }
        }
        assertEquals(4, shots);
        for (int seed = 0; seed < 500; seed++) {
            TrapLayout l = TrapLayout.create(new Random(seed), 7, -5, 4, 0.9);
            assertTrue(l.solvable());
            assertTrue(l.count() > 30, "es bröckelt auch etwas");
        }
    }

    @Test
    void matchmakerFillsTeamsOldestFirst() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        List<Matchmaker.Group> waiting = List.of(
                new Matchmaker.Group(a, 2, Difficulty.NORMAL, 0),
                new Matchmaker.Group(b, 1, Difficulty.NORMAL, 100),
                new Matchmaker.Group(c, 3, Difficulty.NORMAL, 200),
                new Matchmaker.Group(d, 1, Difficulty.SCHWER, 0));
        // Noch keiner lange genug gewartet und kein Team voll → niemand startet
        assertTrue(Matchmaker.ready(waiting, 1000, 30_000).isEmpty());
        // a (2) + b (1) = 3, c (3) passt nicht dazu; d allein auf „schwer“
        List<List<Matchmaker.Group>> teams = Matchmaker.ready(waiting, 31_000, 30_000);
        assertEquals(3, teams.size());
        assertEquals(List.of(a, b), teams.get(0).stream().map(Matchmaker.Group::id).toList());
        // Volles Team startet sofort
        List<Matchmaker.Group> four = List.of(new Matchmaker.Group(a, 2, Difficulty.LEICHT, 0),
                new Matchmaker.Group(b, 2, Difficulty.LEICHT, 10));
        assertEquals(1, Matchmaker.ready(four, 20, 30_000).size());
    }

    @Test
    void cosmeticsAreEarnedNotBought() {
        assertEquals(List.of(Cosmetics.Trail.NONE), Cosmetics.unlocked(0, 0, 0, 0));
        assertTrue(Cosmetics.unlocked(1, 0, 0, 0).contains(Cosmetics.Trail.GLUT));
        assertTrue(Cosmetics.unlocked(3, 1, 0, 30).contains(Cosmetics.Trail.SEELENFEUER));
        assertFalse(Cosmetics.unlocked(3, 1, 0, 30).contains(Cosmetics.Trail.GOLDSTAUB));
        assertTrue(Cosmetics.unlocked(3, 1, 1, 30).contains(Cosmetics.Trail.GOLDSTAUB));
    }
}
