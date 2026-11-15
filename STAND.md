# Stand – Morni Burgverlies

**Was es ist:** Der Koop-Modus „Burgverlies“ (EN „Castle Dungeon“) des MoinMorni-Netzwerks (Paper-Plugin
`MorniBurgverlies`, Java 25, Paper 26.x): 1–4 Leute, drei Etagen aus handgebauten Räumen (Kampf, Fallen, Rätsel,
Schatz, Rast, Krämerin), ganz unten der Kerkermeister. Läuft im Netzwerk und allein.

## Aufbau

- `src/main/java/de/moinmorni/burgverlies/`: `BurgverliesPlugin` (Start, Welt, Aufgaben, Gruppe/Party, Zustand),
  `Settings` (config.yml), `Texts` (de/en), `Ui`, `Items`, `Commands` (/burgverlies, /verlassen, /bvadmin).
- `dungeon/` (ohne Bukkit, getestet): `Dir`, `Pos`, `RoomType`, `RoomTemplate`, `RoomCatalog` (17 Vorlagen),
  `DungeonGenerator` (Etage würfeln: Reihenfolge nach Regeln, Weg mit Zurückgehen), `PlacedRoom`, `FloorPlan`,
  `Difficulty`, `Scaling`, `MobKind`, `Waves`, `Revives`, `Offer`, `ShopMath`, `Loot`, `Score`, `Puzzle`, `TrapLayout`,
  `TrapTiming`, `Matchmaker`, `Cosmetics`.
- `world/`: `VoidGenerator`, `Plan` (wie Duels), `Builder` (Blöcke Stück für Stück, gedreht), `Layout` (Plätze der
  Verliese, Etagen übereinander), `RoomPlans` (Aussehen je Raum und Etage), `Doors` (Fallgitter, Gänge), `HallPlan`
  (Torhaus).
- `run/`: `Run`, `Hero`, `Room`, `Runs` (Start, Etagen bauen, Räume betreten/schaffen, Treppe, Sieg/Niederlage,
  Statistik, `/bvadmin debug`), `RoomLogic` (je Raumart), `Boss` (Kerkermeister), `Downs` (Hinfallen/Wiederbeleben),
  `Mobs`, `Gear`, `ShopMenu`, `Sidebar`.
- `hall/`: `Hall`, `Queue` (Mitspieler-Suche), `Boards` (Tafeln je Sprache). `menu/`: `Menu`, `Menus`.
- `listen/`: `PlayerListener`, `CombatListener`, `GuardListener`.
- `src/api/java/de/moinmorni/api/MoinMorni.java`: Kopie der Netzwerk-Schnittstelle (nicht in der Jar).
- `test/`: `run.sh` (echter Server), `burgverlies-test.cjs` (mineflayer), `rcon.cjs`, `fake-network/`.

## Stand (15.11.2026)

- Version 0.1.0 (erster Stand, Branch `claude/erster-stand`, noch nicht veröffentlicht): alles im README.
- Geprüft: 26 Unit-Tests (Etagen-Würfel mit 400 Startwerten × 3 Etagen × 3–8 Räume: verbunden, keine Überschneidung,
  Türen passen, Fallengänge geradeaus, Rätsel und bröckelnder Boden lösbar; Schwierigkeit/Spielerzahl, Wellen,
  Wiederbelebung, Preise, Beute, Punkte, Fallen-Takt, Suche, Spuren, Texte).
- Echter Test mit Paper 26.1.2 + ViaVersion/ViaBackwards und vier Test-Spielern (mineflayer), zufällige Etagen: 92/94
  im ersten Lauf (zwei Fehler behoben: Zähler im Test, Kerkermeister begann Abschnitt 2 mit dem alten Angriff);
  mit `SEED=8` (Siegelkammer, Brunnenschacht, Glutkammer, Gruft, Weinkeller …): 98/98. Geprüft: Torhaus, Sprache,
  Party (nur Leitung startet), Menü, drei Etagen bis zum Sieg, Fallgitter, Wellen, Fallen inkl. bröckelndem Stein und
  Grube, Rätsel mit Hebeln und Platten (Fehler → Spinnen), Truhen, Krämerin (Schwert, Wiederbelebung), Wiederbeleben
  per Schleichen, Ausbluten, Rückkehr auf neuer Etage, Rast, Kerkermeister (Leben, Leiste, drei Abschnitte,
  Unverwundbarkeit, Wachen, Ketten, Sieg), Statistik ans Netzwerk, Spielerdatei, Bestenliste, Spuren, Niederlage
  allein, Mitspieler-Suche, /verlassen.
- Bilder mit echtem Client 26.3 (über ViaBackwards) nur lokal unter `~/mm-cache/bilder-burgverlies`.

## Offen

- Philip: Bilder ansehen; Go für `main` (dann erstes Release v0.1.0).
- Nicht echt geprüft: richtiger Kampf mit echtem Client (Treffer, Schockwelle ausweichen per Sprung), Bedrock-Spieler,
  Netzwerk mit echtem Hub.
- Einstellungen im Panel des Netzwerks (Einstellungs-Katalog) – gehört ins Netzwerk-Repo.
