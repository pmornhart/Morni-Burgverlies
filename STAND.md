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

## Stand (16.11.2026)

- Version 0.1.3 (Branch `claude/moinmorni`, wartet auf Philips Go): **Name wieder MoinMorni** – 0.1.2 (16.11.
  veröffentlicht) hatte Burgverlies kurz an einen neuen Netzwerk-Namen angepasst; der war nicht verfügbar, darum ist
  der Code wieder wie in 0.1.1 (Umbenennung per `git revert` zurückgenommen): Paket `de.moinmorni.burgverlies`,
  Schnittstelle `de.moinmorni.api.MoinMorni`, Netzwerk-Plugin „MoinMorni“, Recht `moinmorni.burgverlies.admin`.
  Plugin-Name, Datenordner `plugins/MorniBurgverlies` und Torhaus waren nie anders (kein Umzug, kein Neubau).
  `test/run.sh` löscht alle alten Jars im Test-Server. Geprüft: 28 Unit-Tests, Paper 26.1 übersetzt, echt mit Paper
  26.1.2: zufällige Etagen **99/99**, `SEED=8` **99/99**, Burgverlies meldet sich beim Test-Netzwerk
  „MoinMorni“, Server-Log ohne Fehler.
- Version 0.1.1 (16.11. veröffentlicht, Release v0.1.1): Der Kerkermeister blieb am Thron stehen, solange alle
  mehr als ~12 Blöcke weg waren (beim Trailer-Dreh gesehen) – jetzt Sichtweite 40 (`FOLLOW_RANGE`), Ziel gleich beim
  Erscheinen und alle Sekunde neu, wenn sein Ziel am Boden liegt oder den Saal verlässt (`Boss.hunt`); `/bvadmin debug`
  zeigt `target`, der echte Test prüft es.
- Version 0.1.0 (15.11. veröffentlicht, Release v0.1.0): alles im README.
- Geprüft: 28 Unit-Tests (Etagen-Würfel mit 400 Startwerten × 3 Etagen × 3–8 Räume: verbunden, keine Überschneidung,
  Türen passen, Fallengänge geradeaus, Rätsel und bröckelnder Boden lösbar; Schwierigkeit/Spielerzahl, Wellen,
  Wiederbelebung, Preise, Beute, Punkte, Fallen-Takt, Suche, Spuren, Texte).
- Echter Test mit Paper 26.1.2 + ViaVersion/ViaBackwards und vier Test-Spielern (mineflayer): zufällige Etagen
  **101/101**, mit `SEED=8` (Siegelkammer, Brunnenschacht, Glutkammer, Gruft, Weinkeller …) **98/98**; Server-Log ohne
  Fehler. Geprüft: Torhaus, Sprache, Party (nur Leitung startet), Menü, drei Etagen bis zum Sieg, Fallgitter, Wellen,
  Fallen inkl. bröckelndem Stein und Grube, Rätsel mit Hebeln und Platten (Fehler → Spinnen), Truhen, Krämerin
  (Schwert, Wiederbelebung), Wiederbeleben per Schleichen, Ausbluten, Rückkehr auf neuer Etage, Rast, Kerkermeister
  (Leben, Leiste, drei Abschnitte, Unverwundbarkeit, Wachen, Ketten, Sieg), Statistik ans Netzwerk, Spielerdatei,
  Bestenliste, Spuren, Niederlage allein, Mitspieler-Suche, /verlassen, drei Abstiege nacheinander im selben Verlies.
- Funde im echten Test behoben: Kerkermeister begann Abschnitt 2 mit dem alten Angriff; Neubau eines Verlieses brach
  ab, wenn dort noch ein Lesepult aus einem früheren Abstieg stand; Tafeln im Torhaus verschwanden ohne Spieler.
- Einrichtung 2 (Coordinator-Wunsch): jeder Raum klar erkennbar eingerichtet (Fassregale, Stockbetten, Zellen mit
  Stroh und Ketten, Steinsärge, Rüstungsständer, Goldhaufen, Theke, großer Thronsaal mit Säulenreihen, Bannern und
  Feuerschalen), Wandsäulen mit Bögen, Laternen, Kerzen, Seelenfeuer, gedämpftes Licht. `dungeon/RoomSpace` legt fest,
  was fürs Spiel frei bleiben muss (Türen, Kreuz durch die Mitte, Gegner-Stellen, Fallen, Rätsel, Truhen, Krämerin,
  Treppe, Kampffläche) – Einrichtung wird dort nach dem Schmücken wieder geräumt (Unit-Test `RoomSpaceTest`).
  Echt: zufällig 94/94, `SEED=8` 98/98, Log ohne Fehler. Bilder v2 lokal unter `~/mm-cache/bilder-burgverlies-v2`.
- Bilder mit echtem Client 26.3 (über ViaBackwards, eigenes Xvfb `:97`) von Torhaus und allen Räumen einer Etagenfolge
  nur lokal unter `~/mm-cache/bilder-burgverlies` (nicht im Repo).

## Offen

- Philip: Bilder ansehen; Go für `main` (dann Release v0.1.3).
- Nicht echt geprüft: richtiger Kampf mit echtem Client (Treffer, Schockwelle ausweichen per Sprung), Bedrock-Spieler,
  Netzwerk mit echtem Hub.
- Einstellungen im Panel des Netzwerks (Einstellungs-Katalog) – gehört ins Netzwerk-Repo.
