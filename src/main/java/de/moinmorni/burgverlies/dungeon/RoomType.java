package de.moinmorni.burgverlies.dungeon;

/** Arten von Räumen. */
public enum RoomType {
    /** Ankunft auf einer Etage. */
    START(false),
    /** Wellen von Gegnern. */
    FIGHT(true),
    /** Fallen-Parcours: alle müssen heil ans Ende. */
    TRAP(true),
    /** Rätsel: Hebel oder Platten in der richtigen Reihenfolge. */
    PUZZLE(true),
    /** Truhen mit Kerkertalern. */
    TREASURE(false),
    /** Rast: alle werden geheilt, wer raus war, ist wieder dabei. */
    REST(false),
    /** Krämerstube mit Treppe zur nächsten Etage. */
    SHOP(false),
    /** Der Kerkermeister. */
    BOSS(true);

    private final boolean locks;

    RoomType(boolean locks) {
        this.locks = locks;
    }

    /** Fällt beim Betreten das Fallgitter hinter einem herunter (bis der Raum geschafft ist)? */
    public boolean locks() {
        return locks;
    }
}
