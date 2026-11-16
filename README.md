# Morni Burgverlies

Der Koop-Modus **Burgverlies** (englisch *Castle Dungeon*) des Minecraft-Netzwerks **MoinMorni** (moinmorni.net):
1 bis 4 Leute steigen gemeinsam tief unter die Burg – durch Kampfräume, Fallengänge, Rätsel und Schatzkammern bis
zum Endgegner, dem **Kerkermeister**. Ein Paper-Plugin für Paper 26.x (Java 25). Es läuft im Netzwerk und auch ganz
allein.

## Was drin ist

- **Torhaus** – der Startplatz: ein Burghof auf einer schwebenden Insel, im Norden die Burgmauer mit Türmen und dem
  dunklen Abstieg (halb hochgezogenes Fallgitter, Feuerschalen, Banner), dazu Brunnen, Bänke, Fässer, Bäume.
  Schwebende Tafeln zeigen Begrüßung, die besten Abstiege und die schnellsten Siege – jede Person sieht sie in ihrer
  Sprache.
- **Schnellleiste:** Laterne = Abstieg wählen, Gold = Statistik & Bestenliste, Lohenpulver = Spuren,
  Tür = zurück zur Lobby. Wer zum Abstieg läuft, bekommt das Menü auch so. Alle Menüs sind Truhen (gehen auch mit
  Bedrock über Geyser).
- **Starten:** Schwierigkeit wählen (Leicht, Normal, Schwer), dann **Jetzt hinab** (allein oder mit der eigenen
  Party – die Party-Leitung nimmt alle mit, die im Torhaus sind) oder **Mitspieler suchen** (Teams bis 4, nach
  30 Sekunden geht es mit allen los, die da sind).
- **Drei Etagen**, jede für jeden Abstieg neu aus handgebauten Räumen zusammengewürfelt (fester Startwert, Weg kreuzt
  sich nie, Fallengänge immer geradeaus): **Kellergewölbe** (Steinziegel), **Kerker** (Tiefenschiefer),
  **Tiefe Gruft** (Schwarzstein). Zwischen den Räumen Gänge mit Laternen, an jeder Tür ein Fallgitter: es fällt
  hinter einem herunter und hebt sich erst, wenn der Raum geschafft ist.

  | Raum | Art | Was passiert |
  |---|---|---|
  | Torkammer, Treppenabsatz | Ankunft | Anfang jeder Etage |
  | Wachstube, Kerker, Weinkeller, Rüstkammer, Gruft | Kampf | 2–3 Wellen (ab Etage 3 mit Kettenknecht), Seelenfeuer zeigt vorher, wo sie kommen |
  | Fallengang, Brunnenschacht, Glutkammer | Fallen | Pfeile aus der Wand (Rauch + Klicken vorher), Glutdüsen im Boden (Zischen vorher, zwei Gruppen im Wechsel), bröckelnder Boden (gesprungene Steine wackeln erst) – immer mit sicherem Weg; geschafft, wenn alle am verzierten Rand am Ende stehen |
  | Hebelsaal, Siegelkammer | Rätsel | Farben leuchten in einer Reihenfolge auf (3/4/5 Schritte je Etage) – Hebel umlegen bzw. auf Platten treten; Fehler = von vorn und Spinnen kriechen heraus; die Glocke zeigt es noch einmal |
  | Schatzkammer, Zehntkeller | Schatz | Truhen: Kerkertaler für alle, dazu Pfeile, Heiltrank oder Goldapfel für den, der öffnet |
  | Wachfeuer | Rast | alle geheilt, wer raus war, ist wieder dabei (vor dem Kerkermeister immer) |
  | Krämerstube | Krämerin | Laden der Krämerin Walburga, Treppe hinab zur nächsten Etage |
  | Thronsaal des Kerkermeisters | Endgegner | siehe unten |

- **Gegner** mit eigenen Namen und Ausrüstung: Kerkerwache, Burgschütze, Kellerspinne, Gruftwächter, Kettenknecht.
  Leben und Schaden wachsen mit Schwierigkeit und Zahl der Spieler.
- **Der Kerkermeister** – groß, mit Boss-Leiste und drei Abschnitten. Jeder Angriff kündigt sich an:
  - Abschnitt 1: **Schockwelle** – ein roter Kreis wächst auf dem Boden, wer am Ende springt, bleibt verschont.
  - Abschnitt 2 (unter ⅔ Leben): **ruft Wachen** – Seelenfeuer zeigt die Stellen vorher.
  - Abschnitt 3 (unter ⅓): **Ketten** – eine graue Linie zielt auf jemanden, dann zieht ihn die Kette heran und
    bremst ihn; dazu alles andere, schneller.
  - Beim Wechsel ist er kurz unverwundbar.
- **Leben:** Niemand stirbt wirklich. Wer fällt, liegt am Boden (leuchtet, Gegner lassen ihn in Ruhe). Ein Mitspieler
  hält **3 Sekunden Schleichen** daneben → wieder auf den Beinen (kostet eine von **2 Wiederbelebungen**). Wer zu
  lange liegt (40/30/20 s je Schwierigkeit) oder keine Wiederbelebung mehr hat, ist raus und schaut zu – bis zur
  nächsten Rast oder Etage. Allein rappelt man sich selbst auf. Liegen alle, ist der Abstieg vorbei.
- **Kerkertaler** gibt es für Gegner, Truhen und geschaffte Räume – nur für diesen Abstieg. Die Krämerin verkauft
  bessere Schwerter (Stein → Eisen → Diamant → Schärfe), Rüstung (Leder → Kette → Eisen → Diamant), Bogen (Stärke I/II),
  Schild, Pfeile, Heiltränke, Goldäpfel und Wiederbelebungen (höchstens zwei).
- **Dauerhaft** nur zum Anschauen, nie stärker, nie zu kaufen: **Spuren** hinter einem im Verlies (Glut, Funken,
  Seelenfeuer, Goldstaub) – freigeschaltet durchs Spielen.
- **Punkte:** Räume × 100, Sieg + 1500, Zeit-Bonus beim Sieg, −100 je Hinfallen, mal Schwierigkeit (1 / 1,5 / 2).
  Bestenliste nach Punkten und nach Zeit.
- **Leiste rechts** im Abstieg: Schwierigkeit, Etage, Raum, Zeit, Kerkertaler, Wiederbelebungen, wer steht/liegt/raus
  ist. Titel, Töne und Aktionsleiste zu allem.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/burgverlies` (`/bv`) | Menü „Abstieg wählen“ |
| `/bv start [leicht\|normal\|schwer]` | gleich hinab (allein oder mit der eigenen Party) |
| `/bv suchen [leicht\|normal\|schwer]` | Mitspieler suchen |
| `/verlassen` | Suche oder Abstieg verlassen |
| `/bv stats [spieler]` · `/bv top` · `/bv spuren` | Statistik, Bestenliste, Spuren |
| `/bvadmin info\|neubauen\|neuladen` | Verwaltung (`moinmorni.burgverlies.admin`) |
| `/bvadmin start <spieler> [schwierigkeit]` · `debug` · `loesen` · `taler` | für Team und Tests |

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Burgverlies nutzt sie,
wenn sie da ist (`softdepend`): Sprache, Party, Statistik, Zustand für Lobby-Figur und Webseite, zurück zur Lobby. Ohne
Netzwerk: deutsche Texte, Statistik nur lokal, „zur Lobby“ trennt die Verbindung mit einem Hinweis.

Statistik nach jedem Abstieg (`stats("burgverlies", …)`):

- hochgezählt: `runs`, `wins`, `bosses`, `rooms`
- Bestwert (kleiner ist besser): `best_time_ms` – nur bei einem Sieg
- Bestwert (größer ist besser, eigener Aufruf): `best_score`

Zustand: `waiting` (kein Abstieg) oder `running`, dazu die Spielerzahl.

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Burgverlies.jar` (Version aus `pom.xml`); das Netzwerk
holt sich das neueste.

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Burgverlies.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
SEED=8 test/run.sh               # dasselbe mit festen Etagen (Siegelkammer, Brunnenschacht, Glutkammer, Gruft …)
```

`test/run.sh` baut das Plugin und ein Test-Netzwerk (`test/fake-network`: tut so, als wäre es das Netzwerk-Plugin –
Bert spricht Englisch, Anna und Bert sind eine Party, alle Meldungen landen in `calls.log`), richtet einen
Paper-26.1.2-Server mit ViaVersion/ViaBackwards ein (offline, Port 25780, RCON 25880) und schickt vier Test-Spieler
durch alle drei Etagen bis zum Sieg über den Kerkermeister, dazu eine Niederlage allein und die Mitspieler-Suche
(`test/burgverlies-test.cjs`). Test-Spieler können nicht richtig kämpfen: Gegner besiegt die Konsole, dem
Kerkermeister schadet `damage` – geprüft wird, wie das Spiel darauf reagiert.

## Einstellungen

`plugins/MorniBurgverlies/config.yml`: Welt, Abstiege gleichzeitig, Räume pro Etage, Wartezeit der Suche,
Wiederbelebungen (Anzahl, Schleich-Zeit, Ausbluten), Treppe, Ende, Wellen-Pause – mit Erklärungen in der Datei.
Befehle zum Einstellen gibt es bewusst nicht: im Netzwerk stellt man alles im Panel ein.

---

Verfasst mit Claude 🤖
