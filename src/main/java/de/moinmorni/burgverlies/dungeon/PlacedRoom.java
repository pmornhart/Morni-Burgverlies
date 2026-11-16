package de.moinmorni.burgverlies.dungeon;

/**
 * Ein Raum an seinem Platz im Raster einer Etage.
 *
 * @param index    Nummer auf der Etage (0 = Ankunft)
 * @param template Vorlage
 * @param cellX    Rasterfeld (Osten)
 * @param cellZ    Rasterfeld (Süden)
 * @param entry    Wand, durch die man hereinkommt (Welt-Richtung), {@code null} beim Ankunftsraum
 * @param exit     Wand, durch die es weitergeht (Welt-Richtung), {@code null} beim letzten Raum
 * @param rotation Viertel im Uhrzeigersinn, um die die Vorlage gedreht wird (Vorlagen-Norden = Eingang)
 */
public record PlacedRoom(int index, RoomTemplate template, int cellX, int cellZ, Dir entry, Dir exit, int rotation) {

    public RoomType type() {
        return template.type();
    }

    /** Ausgang im Vorlagen-Rahmen (Süden = geradeaus). */
    public Dir localExit() {
        return exit == null ? null : exit.rotate(-rotation);
    }

    /** Vorlagen-Stelle → Stelle relativ zur Raummitte in der Welt. */
    public Pos toWorld(Pos local) {
        return local.rotate(rotation);
    }
}
