# Morni Burgverlies

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo;
  auch keine Test-Server, Welten, Spielerdaten oder RCON-Passwörter).
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen); `TextsTest` prüft Platzhalter, Zeichen und dass jeder Schlüssel aus dem
  Code existiert.
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (nichts von Mojang, Nintendo, Hypixel oder anderen Servern
  und Spielen): eigene Räume, eigene Namen (Kerkermeister, Krämerin Walburga …), eigene Bauten.
- Die Netzwerk-Schnittstelle `src/api/java/net/morni/api/MorniNet.java` ist eine Kopie aus `pmornhart/morni.net`
  (`plugins/morninet-network/src/main/java/net/morni/api/MorniNet.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar (die echten Klassen bringt das Netzwerk-Plugin mit).
- Burgverlies muss auch ohne Netzwerk laufen (`softdepend: [MorniNet]`; nur `net/Connected` kennt die Netzwerk-Klassen).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
- Regeln, die sich ohne Server prüfen lassen (Etagen-Würfel, Schwierigkeit, Wiederbelebung, Preise, Beute, Punkte,
  Rätsel, Fallen-Takt, Suche), gehören ins Paket `dungeon` – ohne Bukkit – und haben Unit-Tests.
- Neue Raumvorlage: in `RoomCatalog` (Stellen) und `world/RoomPlans` (Aussehen), Türen mittig frei lassen (Test
  `templatesKeepTheDoorsFree`), Text `room.<id>` anlegen. Blocknamen von 26.x benutzen (z. B. `iron_chain`, nicht `chain`)
  – beim Start werden alle Pläne einmal gebaut, ein falscher Name fällt sofort auf.
- Ändert sich das Torhaus: `BurgverliesPlugin.HALL_VERSION` erhöhen – dann baut der Server es beim nächsten Start neu.
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Burgverlies.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echter Client aus `pmornhart/morni.net`, `tools/mc-client`;
  Texturen und Bilder nur lokal, nie ins Repo).
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum hat Burgverlies keine
  Einstellungs-Befehle, nur die `config.yml`; die Anbindung an den Einstellungs-Katalog gehört ins Netzwerk-Repo.
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (und `SEED=8 test/run.sh`), Ports 25780/25880.
- `STAND.md` nach jedem größeren Schritt aktualisieren.
