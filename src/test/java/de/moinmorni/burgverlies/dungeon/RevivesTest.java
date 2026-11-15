package de.moinmorni.burgverlies.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RevivesTest {

    private final UUID anna = UUID.randomUUID();
    private final UUID bert = UUID.randomUUID();

    @Test
    void teammateRevivesByCrouching() {
        Revives r = new Revives(List.of(anna, bert), 2, 10, 4);
        assertEquals(Revives.Fall.DOWN, r.fall(bert));
        assertEquals(Revives.State.DOWN, r.state(bert));
        assertEquals(Revives.Fall.NONE, r.fall(bert), "wer liegt, fällt nicht noch einmal");
        assertFalse(r.allDown());
        // drei Schritte geholfen, dann losgelassen → von vorn
        for (int i = 0; i < 3; i++) {
            assertTrue(r.tick(id -> true).isEmpty());
        }
        assertEquals(0.75, r.progress(bert), 1e-9);
        r.tick(id -> false);
        assertEquals(0, r.progress(bert), 1e-9, "Loslassen setzt zurück");
        List<Revives.Event> events = List.of();
        for (int i = 0; i < 4; i++) {
            events = r.tick(id -> true);
        }
        assertEquals(List.of(new Revives.Event(bert, Revives.Event.Kind.REVIVED)), events);
        assertEquals(Revives.State.UP, r.state(bert));
        assertEquals(1, r.revivesLeft(bert));
        assertEquals(1, r.downs(bert));
    }

    @Test
    void bleedingOutAndRunningOutOfRevives() {
        Revives r = new Revives(List.of(anna, bert), 1, 5, 3);
        r.fall(bert);
        List<Revives.Event> last = List.of();
        for (int i = 0; i < 5; i++) {
            last = r.tick(id -> false);
        }
        assertEquals(List.of(new Revives.Event(bert, Revives.Event.Kind.BLED_OUT)), last);
        assertEquals(Revives.State.OUT, r.state(bert));
        assertFalse(r.allDown(), "Anna steht noch");
        // Anna hat noch eine Wiederbelebung, aber niemand kann helfen → alle unten → vorbei
        assertEquals(Revives.Fall.DOWN, r.fall(anna));
        assertTrue(r.allDown());
        // Rast holt alle zurück, ohne Wiederbelebungen zu kosten
        assertEquals(2, r.restoreAll().size());
        assertEquals(Revives.State.UP, r.state(bert));
        assertEquals(1, r.revivesLeft(bert));
        // Ohne Wiederbelebungen ist man sofort raus
        Revives none = new Revives(List.of(anna, bert), 0, 5, 3);
        assertEquals(Revives.Fall.OUT, none.fall(anna));
        none.addRevive(bert);
        assertEquals(Revives.Fall.DOWN, none.fall(bert));
        assertTrue(none.allDown());
    }

    @Test
    void aloneYouGetUpByYourself() {
        Revives r = new Revives(List.of(anna), 2, 100, 3);
        assertEquals(Revives.Fall.DOWN, r.fall(anna));
        assertFalse(r.allDown(), "allein rappelt man sich auf");
        List<Revives.Event> events = List.of();
        for (int i = 0; i < 3; i++) {
            events = r.tick(id -> false);
        }
        assertEquals(List.of(new Revives.Event(anna, Revives.Event.Kind.SELF_REVIVED)), events);
        r.fall(anna);
        for (int i = 0; i < 3; i++) {
            r.tick(id -> false);
        }
        assertEquals(0, r.revivesLeft(anna));
        assertEquals(Revives.Fall.OUT, r.fall(anna));
        assertTrue(r.allDown());
        assertEquals(3, r.downs(anna));
    }

    @Test
    void leavingPlayersDoNotBlockTheRun() {
        Revives r = new Revives(List.of(anna, bert), 2, 10, 3);
        r.fall(anna);
        r.remove(bert);
        assertTrue(r.solo());
        assertFalse(r.allDown());
        r.remove(anna);
        assertTrue(r.allDown());
    }
}
