package de.moinmorni.burgverlies.dungeon;

import java.util.List;

/** Eine Etage: Räume in der Reihenfolge, in der man sie durchquert. */
public record FloorPlan(int floor, long seed, List<PlacedRoom> rooms) {

    public FloorPlan {
        rooms = List.copyOf(rooms);
    }

    public PlacedRoom room(int i) {
        return rooms.get(i);
    }

    public int size() {
        return rooms.size();
    }

    public PlacedRoom last() {
        return rooms.get(rooms.size() - 1);
    }
}
