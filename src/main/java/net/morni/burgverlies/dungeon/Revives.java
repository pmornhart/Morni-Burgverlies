package net.morni.burgverlies.dungeon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Wer steht, wer liegt, wer ist raus. Jede Person hat ein paar Wiederbelebungen. Fällt jemand, liegt er am Boden;
 * hält ein Mitspieler daneben lange genug Schleichen gedrückt, steht er wieder auf (kostet eine Wiederbelebung).
 * Keine Wiederbelebung mehr oder zu lange gelegen = raus (bis zur nächsten Rast oder Etage). Allein rappelt man
 * sich selbst auf. Liegen alle oder sind raus, ist der Abstieg vorbei.
 *
 * <p>Gezählt wird in Schritten (das Plugin ruft {@link #tick} alle paar Spielticks auf).
 */
public final class Revives {

    public enum State { UP, DOWN, OUT }

    /** Was beim Hinfallen passiert ist. */
    public enum Fall { NONE, DOWN, OUT }

    /** Ereignis aus einem Schritt. */
    public record Event(UUID player, Kind kind) {
        public enum Kind { REVIVED, SELF_REVIVED, BLED_OUT }
    }

    private static final class Member {
        State state = State.UP;
        int revives;
        int bleed;
        int progress;
        int downs;
    }

    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final int bleedSteps;
    private final int reviveSteps;

    /**
     * @param revives     Wiederbelebungen pro Person
     * @param bleedSteps  Schritte am Boden, bis man raus ist
     * @param reviveSteps Schritte Schleichen, bis jemand wieder steht
     */
    public Revives(Collection<UUID> players, int revives, int bleedSteps, int reviveSteps) {
        this.bleedSteps = Math.max(1, bleedSteps);
        this.reviveSteps = Math.max(1, reviveSteps);
        for (UUID p : players) {
            Member m = new Member();
            m.revives = Math.max(0, revives);
            members.put(p, m);
        }
    }

    public State state(UUID p) {
        Member m = members.get(p);
        return m == null ? State.OUT : m.state;
    }

    public int revivesLeft(UUID p) {
        Member m = members.get(p);
        return m == null ? 0 : m.revives;
    }

    /** Wie oft jemand gefallen ist (für die Punkte). */
    public int downs(UUID p) {
        Member m = members.get(p);
        return m == null ? 0 : m.downs;
    }

    public int totalDowns() {
        return members.values().stream().mapToInt(m -> m.downs).sum();
    }

    /** Fortschritt der Wiederbelebung 0..1. */
    public double progress(UUID p) {
        Member m = members.get(p);
        return m == null ? 0 : (double) m.progress / reviveSteps;
    }

    /** Verbleibende Schritte am Boden. */
    public int bleedLeft(UUID p) {
        Member m = members.get(p);
        return m == null ? 0 : Math.max(0, bleedSteps - m.bleed);
    }

    public boolean solo() {
        return members.size() == 1;
    }

    public List<UUID> players() {
        return List.copyOf(members.keySet());
    }

    /** Jemand hat tödlichen Schaden bekommen. */
    public Fall fall(UUID p) {
        Member m = members.get(p);
        if (m == null || m.state != State.UP) {
            return Fall.NONE;
        }
        m.downs++;
        m.bleed = 0;
        m.progress = 0;
        if (m.revives <= 0) {
            m.state = State.OUT;
            return Fall.OUT;
        }
        m.state = State.DOWN;
        return Fall.DOWN;
    }

    /**
     * Ein Schritt. {@code helped} sagt für jede liegende Person, ob gerade jemand neben ihr Schleichen hält.
     */
    public List<Event> tick(Predicate<UUID> helped) {
        List<Event> events = new ArrayList<>();
        boolean solo = solo();
        members.forEach((id, m) -> {
            if (m.state != State.DOWN) {
                return;
            }
            if (solo || helped.test(id)) {
                m.progress++;
                if (m.progress >= reviveSteps) {
                    m.state = State.UP;
                    m.revives--;
                    m.progress = 0;
                    m.bleed = 0;
                    events.add(new Event(id, solo ? Event.Kind.SELF_REVIVED : Event.Kind.REVIVED));
                }
                return;
            }
            m.progress = 0;
            m.bleed++;
            if (m.bleed >= bleedSteps) {
                m.state = State.OUT;
                events.add(new Event(id, Event.Kind.BLED_OUT));
            }
        });
        return events;
    }

    /** Liegen alle oder sind raus? Dann ist der Abstieg vorbei. */
    public boolean allDown() {
        if (members.isEmpty()) {
            return true;
        }
        boolean anyUp = members.values().stream().anyMatch(m -> m.state == State.UP);
        if (anyUp) {
            return false;
        }
        // Allein darf man sich selbst aufrappeln, solange noch Wiederbelebungen da sind.
        return !(solo() && members.values().iterator().next().state == State.DOWN);
    }

    /** Rast oder neue Etage: alle stehen wieder (kostet nichts). Gibt zurück, wer zurückgeholt wurde. */
    public List<UUID> restoreAll() {
        List<UUID> back = new ArrayList<>();
        members.forEach((id, m) -> {
            if (m.state != State.UP) {
                back.add(id);
            }
            m.state = State.UP;
            m.bleed = 0;
            m.progress = 0;
        });
        return back;
    }

    /** Wiederbelebung gekauft. */
    public void addRevive(UUID p) {
        Member m = members.get(p);
        if (m != null) {
            m.revives++;
        }
    }

    /** Jemand hat den Abstieg verlassen. */
    public void remove(UUID p) {
        members.remove(p);
    }

    public long count(State s) {
        return members.values().stream().filter(m -> m.state == s).count();
    }
}
