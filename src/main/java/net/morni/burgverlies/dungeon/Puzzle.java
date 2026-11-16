package net.morni.burgverlies.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Reihenfolge-Rätsel: Hebel/Platten in der gezeigten Reihenfolge betätigen; ein Fehler setzt alles zurück. */
public final class Puzzle {

    public enum Step { PROGRESS, WRONG, SOLVED, IGNORED }

    private final List<Integer> order;
    private int done;
    private int mistakes;

    public Puzzle(List<Integer> order) {
        if (order.isEmpty()) {
            throw new IllegalArgumentException("leeres Rätsel");
        }
        this.order = List.copyOf(order);
    }

    /**
     * Neue Reihenfolge: {@code length} Schritte aus {@code inputs} Hebeln, nie zweimal direkt derselbe, und jeder
     * Hebel kommt mindestens einmal vor, wenn die Länge es zulässt.
     */
    public static Puzzle create(Random rnd, int inputs, int length) {
        if (inputs < 2) {
            throw new IllegalArgumentException("mindestens zwei Hebel");
        }
        int n = Math.max(2, length);
        List<Integer> order = new ArrayList<>();
        List<Integer> bag = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (bag.isEmpty()) {
                for (int k = 0; k < inputs; k++) {
                    bag.add(k);
                }
                java.util.Collections.shuffle(bag, rnd);
            }
            int pick = 0;
            if (!order.isEmpty() && bag.get(0).equals(order.get(order.size() - 1)) && bag.size() > 1) {
                pick = 1;
            }
            int v = bag.remove(pick);
            if (!order.isEmpty() && order.get(order.size() - 1) == v) {
                v = (v + 1) % inputs;
            }
            order.add(v);
        }
        return new Puzzle(order);
    }

    /** Länge der Reihenfolge je Etage: 3, 4, 5. */
    public static int lengthFor(int floor) {
        return 3 + Math.max(0, floor);
    }

    public List<Integer> order() {
        return order;
    }

    public int done() {
        return done;
    }

    public int mistakes() {
        return mistakes;
    }

    public boolean solved() {
        return done >= order.size();
    }

    /** Hebel {@code input} wurde betätigt. */
    public Step press(int input) {
        if (solved()) {
            return Step.IGNORED;
        }
        if (order.get(done) == input) {
            done++;
            return solved() ? Step.SOLVED : Step.PROGRESS;
        }
        done = 0;
        mistakes++;
        return Step.WRONG;
    }
}
