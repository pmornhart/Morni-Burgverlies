package de.moinmorni.burgverlies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <em>} Smaragd,
 * {@code <au>} Gold, {@code <bl>} Weiß-Blau, {@code <gl>} Glut. Namen von Spielern kommen immer als
 * {@code Placeholder.unparsed} hinein.
 */
public final class Texts {

    public static final int EMERALD = 0x2BB673;
    public static final int GOLD = 0xF2B632;
    public static final int BLUE = 0x5DA9E9;
    public static final int EMBER = 0xE0603A;

    /** MiniMessage mit unseren Farben. */
    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE))),
                    TagResolver.resolver("gl", Tag.styling(TextColor.color(EMBER)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<gl>⚒</gl> <au>Burgverlies</au> <dark_gray>»</dark_gray> ",
                "<gl>⚒</gl> <au>Castle Dungeon</au> <dark_gray>»</dark_gray> ");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("help", "<gray><white>/bv start [leicht|normal|schwer]</white> hinab · <white>/bv suchen</white> Mitspieler suchen · <white>/verlassen</white> · <white>/bv stats</white> · <white>/bv spuren</white></gray>",
                "<gray><white>/bv start [leicht|normal|schwer]</white> go down · <white>/bv suchen</white> find teammates · <white>/verlassen</white> leave · <white>/bv stats</white> · <white>/bv spuren</white> trails</gray>");
        t("nothing-to-leave", "<gray>Du suchst gerade nicht und bist in keinem Abstieg.</gray>",
                "<gray>You're not searching and not in a dungeon run.</gray>");
        t("join.title", "<au>⚒ Burgverlies</au>", "<au>⚒ Castle Dungeon</au>");
        t("join.subtitle", "<gray>Willkommen im <gl>Torhaus</gl>!</gray>", "<gray>Welcome to the <gl>Gatehouse</gl>!</gray>");
        t("join.welcome", "<gray>Willkommen im <gl>Torhaus</gl>! Tief unter der Burg warten Fallen, Schätze und der <gl>Kerkermeister</gl>. <white>Rechtsklick auf die Laterne</white> oder geh zum Abstieg im Norden.</gray>",
                "<gray>Welcome to the <gl>Gatehouse</gl>! Deep below the castle wait traps, treasure and <gl>the Gaoler</gl>. <white>Right-click the lantern</white> or walk to the descent in the north.</gray>");
        t("lobby.kick", "<au>MoinMorni</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit dem Netzwerk.</gray>",
                "<au>MoinMorni</au>\n<gray>Back to the lobby: please reconnect to the network.</gray>");
        t("lobby.going", "<gray>Ab ins Dorf – bis bald!</gray>", "<gray>Off to the village – see you soon!</gray>");

        // ---------- Gegenstände im Torhaus ----------
        t("item.start", "<gl>⚒ Abstieg wählen</gl>", "<gl>⚒ Choose your descent</gl>");
        t("item.start.lore", "<gray>Rechtsklick: Schwierigkeit wählen,</gray>\n<gray>allein, mit Party oder mit Mitspielern hinab.</gray>",
                "<gray>Right-click: choose a difficulty,</gray>\n<gray>go down alone, with your party or with others.</gray>");
        t("item.leave-queue", "<red>✖ Suche beenden</red>", "<red>✖ Stop searching</red>");
        t("item.leave-queue.lore", "<gray>Rechtsklick: nicht mehr nach Mitspielern suchen.</gray>",
                "<gray>Right-click: stop looking for teammates.</gray>");
        t("item.stats", "<au>★ Statistik & Bestenliste</au>", "<au>★ Stats & leaderboard</au>");
        t("item.stats.lore", "<gray>Rechtsklick: deine Werte und die besten Abstiege.</gray>",
                "<gray>Right-click: your stats and the best runs.</gray>");
        t("item.trails", "<au>✦ Spuren</au>", "<au>✦ Trails</au>");
        t("item.trails.lore", "<gray>Rechtsklick: Funken und Glut hinter dir –</gray>\n<gray>nur zum Anschauen, durchs Spielen freigeschaltet.</gray>",
                "<gray>Right-click: sparks and embers behind you –</gray>\n<gray>just for looks, unlocked by playing.</gray>");
        t("item.lobby", "<bl>⌂ Zurück zur Lobby</bl>", "<bl>⌂ Back to the lobby</bl>");
        t("item.lobby.lore", "<gray>Rechtsklick: zurück ins Dorf.</gray>", "<gray>Right-click: back to the village.</gray>");
        t("item.potion", "<red>Heiltrank</red>", "<red>Healing Potion</red>");

        // ---------- Schwierigkeit ----------
        t("difficulty.leicht", "<green>Leicht</green>", "<green>Easy</green>");
        t("difficulty.normal", "<yellow>Normal</yellow>", "<yellow>Normal</yellow>");
        t("difficulty.schwer", "<red>Schwer</red>", "<red>Hard</red>");
        t("difficulty.leicht.desc", "<gray>Schwächere Gegner, mehr Pfeile und Tränke,</gray>\n<gray>40 Sekunden Zeit am Boden.</gray>",
                "<gray>Weaker enemies, more arrows and potions,</gray>\n<gray>40 seconds on the ground.</gray>");
        t("difficulty.normal.desc", "<gray>So ist das Verlies gedacht.</gray>\n<gray>Mehr Taler, mehr Punkte.</gray>",
                "<gray>The dungeon as intended.</gray>\n<gray>More coins, more points.</gray>");
        t("difficulty.schwer.desc", "<gray>Zähe Gegner, ein Gegner mehr pro Welle,</gray>\n<gray>nur 20 Sekunden am Boden. Doppelte Punkte!</gray>",
                "<gray>Tough enemies, one more per wave,</gray>\n<gray>only 20 seconds on the ground. Double points!</gray>");

        // ---------- Menüs ----------
        t("menu.start.title", "Abstieg ins Burgverlies", "Descent into the dungeon");
        t("menu.start.info", "<au>So geht's</au>", "<au>How it works</au>");
        t("menu.start.info.lore", "<gray>1 bis 4 Leute, drei Etagen tief.</gray>\n<gray>Kämpfe, Fallen, Rätsel und Schätze,</gray>\n<gray>zwischen den Etagen die Krämerin,</gray>\n<gray>ganz unten der <gl>Kerkermeister</gl>.</gray>\n<gray>Wer fällt, wird per <white>Schleichen</white> wiederbelebt.</gray>",
                "<gray>1 to 4 players, three floors deep.</gray>\n<gray>Fights, traps, puzzles and treasure,</gray>\n<gray>the trader between floors,</gray>\n<gray>and <gl>the Gaoler</gl> at the very bottom.</gray>\n<gray>Fallen friends are revived by <white>sneaking</white>.</gray>");
        t("menu.start.waiting", "<gray>Suchen gerade: <white><n></white></gray>", "<gray>Searching now: <white><n></white></gray>");
        t("menu.start.chosen", "<em>✔ Gewählt</em>", "<em>✔ Selected</em>");
        t("menu.start.choose", "<white>Klicken: wählen</white>", "<white>Click: select</white>");
        t("menu.start.now", "<gl>⚒ Jetzt hinab</gl>", "<gl>⚒ Go down now</gl>");
        t("menu.start.now.lore", "<gray>Allein oder mit deiner Party –</gray>\n<gray>es geht sofort los.</gray>",
                "<gray>Alone or with your party –</gray>\n<gray>starts right away.</gray>");
        t("menu.start.search", "<bl>⌕ Mitspieler suchen</bl>", "<bl>⌕ Find teammates</bl>");
        t("menu.start.search.lore", "<gray>Bis zu 4 zusammen. Nach <white><seconds> s</white></gray>\n<gray>geht es mit allen los, die da sind.</gray>",
                "<gray>Up to 4 together. After <white><seconds> s</white></gray>\n<gray>you start with whoever is there.</gray>");
        t("menu.stats.title", "Burgverlies · <name>", "Castle Dungeon · <name>");
        t("menu.trails.title", "Spuren", "Trails");

        // ---------- Statistik ----------
        t("stats.head", "<au><name></au>", "<au><name></au>");
        t("stats.runs", "<gray>Abstiege: <white><n></white></gray>", "<gray>Runs: <white><n></white></gray>");
        t("stats.wins", "<gray>Siege: <white><n></white></gray>", "<gray>Wins: <white><n></white></gray>");
        t("stats.bosses", "<gray>Kerkermeister besiegt: <white><n></white></gray>", "<gray>Gaoler defeated: <white><n></white></gray>");
        t("stats.rooms", "<gray>Räume geschafft: <white><n></white></gray>", "<gray>Rooms cleared: <white><n></white></gray>");
        t("stats.downs", "<gray>Gefallen: <white><n></white></gray>", "<gray>Times downed: <white><n></white></gray>");
        t("stats.taler", "<gray>Kerkertaler verdient: <white><n></white></gray>", "<gray>Dungeon coins earned: <white><n></white></gray>");
        t("stats.score", "<gray>Beste Punkte: <white><n></white></gray>", "<gray>Best score: <white><n></white></gray>");
        t("stats.best", "<gray>Bestzeit <difficulty>: <white><time></white></gray>", "<gray>Best time <difficulty>: <white><time></white></gray>");
        t("stats.unknown", "<gray><white><name></white> war noch nie im Burgverlies.</gray>", "<gray><white><name></white> has never been in the dungeon.</gray>");

        // ---------- Tafeln im Torhaus ----------
        t("board.welcome", "<gl><b>⚒ Burgverlies ⚒</b></gl>\n<gray>Tief unter der Burg: Fallen, Schätze</gray>\n<gray>und der <gl>Kerkermeister</gl>. 1 bis 4 Leute.</gray>\n<white>Geh durch das Tor oder klick die Laterne!</white>",
                "<gl><b>⚒ Castle Dungeon ⚒</b></gl>\n<gray>Deep below the castle: traps, treasure</gray>\n<gray>and <gl>the Gaoler</gl>. 1 to 4 players.</gray>\n<white>Walk through the gate or click the lantern!</white>");
        t("board.score", "<au><b>★ Beste Abstiege</b></au>", "<au><b>★ Best runs</b></au>");
        t("board.time", "<au><b>⌚ Schnellste Siege</b></au>", "<au><b>⌚ Fastest wins</b></au>");
        t("board.line", "<au><pos>.</au> <white><names></white> <gray><value> · </gray><difficulty>", "<au><pos>.</au> <white><names></white> <gray><value> · </gray><difficulty>");
        t("board.empty", "<gray>Noch frei – seid die Ersten!</gray>", "<gray>Still empty – be the first!</gray>");
        t("board.place", "<au>★ Platz <place> in der Bestenliste!</au>", "<au>★ Place <place> on the leaderboard!</au>");

        // ---------- Party und Suche ----------
        t("party.not-leader", "<gray>Du bist in einer Party – <white>die Party-Leitung</white> startet den Abstieg.</gray>",
                "<gray>You're in a party – <white>the party leader</white> starts the run.</gray>");
        t("party.too-big", "<red>Eure Party ist zu groß: höchstens <max> gehen zusammen hinab.</red>",
                "<red>Your party is too big: at most <max> can go down together.</red>");
        t("party.missing", "<gray><white><n></white> aus deiner Party sind nicht im Torhaus und bleiben oben.</gray>",
                "<gray><white><n></white> of your party aren't in the gatehouse and stay behind.</gray>");
        t("queue.joined", "<gray>Suche läuft (</gray><difficulty><gray>). Spätestens nach <white><seconds> s</white> geht es los.</gray>",
                "<gray>Searching (</gray><difficulty><gray>). You'll start within <white><seconds> s</white>.</gray>");
        t("queue.left", "<gray>Suche beendet.</gray>", "<gray>Search stopped.</gray>");
        t("queue.bar", "<gray>Suche Mitspieler … <white><n></white>/4 · noch <white><seconds> s</white></gray>",
                "<gray>Finding teammates … <white><n></white>/4 · <white><seconds> s</white> left</gray>");

        // ---------- Abstieg ----------
        t("run.preparing", "<gray>Das Verlies wird vorbereitet (</gray><difficulty><gray>) – gleich geht es hinab …</gray>",
                "<gray>Preparing the dungeon (</gray><difficulty><gray>) – going down in a moment …</gray>");
        t("run.preparing.title", "<gl>⚒</gl>", "<gl>⚒</gl>");
        t("run.preparing.subtitle", "<gray>Fackeln werden angezündet …</gray>", "<gray>Lighting the torches …</gray>");
        t("run.full", "<gray>Alle Verliese sind gerade belegt – du wartest, bis eins frei wird.</gray>",
                "<gray>All dungeons are busy right now – you'll wait until one is free.</gray>");
        t("run.already", "<gray>Du bist schon unterwegs. <white>/verlassen</white> bringt dich ins Torhaus.</gray>",
                "<gray>You're already on your way. <white>/verlassen</white> takes you back to the gatehouse.</gray>");
        t("run.left", "<gray><white><name></white> hat das Verlies verlassen.</gray>", "<gray><white><name></white> left the dungeon.</gray>");
        t("run.left.self", "<gray>Du bist zurück im Torhaus.</gray>", "<gray>You're back in the gatehouse.</gray>");

        t("floor.title", "<gl><b>Etage <n></b></gl>", "<gl><b>Floor <n></b></gl>");
        t("floor.0", "<gray>Kellergewölbe</gray>", "<gray>Cellar Vaults</gray>");
        t("floor.1", "<gray>Kerker</gray>", "<gray>Dungeon Cells</gray>");
        t("floor.2", "<gray>Tiefe Gruft</gray>", "<gray>Deep Crypt</gray>");

        t("room.torkammer", "<au>Torkammer</au>", "<au>Gate Chamber</au>");
        t("room.treppenabsatz", "<au>Treppenabsatz</au>", "<au>Stair Landing</au>");
        t("room.wachstube", "<au>Wachstube</au>", "<au>Guard Room</au>");
        t("room.kerker", "<au>Kerker</au>", "<au>Cell Block</au>");
        t("room.weinkeller", "<au>Weinkeller</au>", "<au>Wine Cellar</au>");
        t("room.ruestkammer", "<au>Rüstkammer</au>", "<au>Armoury</au>");
        t("room.gruft", "<au>Gruft</au>", "<au>Crypt</au>");
        t("room.fallengang", "<au>Fallengang</au>", "<au>Trap Corridor</au>");
        t("room.brunnenschacht", "<au>Brunnenschacht</au>", "<au>Well Shaft</au>");
        t("room.glutkammer", "<au>Glutkammer</au>", "<au>Ember Chamber</au>");
        t("room.hebelsaal", "<au>Hebelsaal</au>", "<au>Lever Hall</au>");
        t("room.siegelkammer", "<au>Siegelkammer</au>", "<au>Seal Chamber</au>");
        t("room.schatzkammer", "<au>Schatzkammer</au>", "<au>Treasury</au>");
        t("room.zehntkeller", "<au>Zehntkeller</au>", "<au>Tithe Cellar</au>");
        t("room.wachfeuer", "<au>Wachfeuer</au>", "<au>Watch Fire</au>");
        t("room.kraemerstube", "<au>Krämerstube</au>", "<au>Trader's Parlour</au>");
        t("room.thronsaal", "<gl>Thronsaal des Kerkermeisters</gl>", "<gl>The Gaoler's Throne Hall</gl>");

        t("room.cleared", "<em>✔ Raum geschafft!</em> <gray>Das Fallgitter hebt sich. <au>+<taler> Kerkertaler</au></gray>",
                "<em>✔ Room cleared!</em> <gray>The portcullis rises. <au>+<taler> dungeon coins</au></gray>");
        t("room.fight.start", "<gray>Das Fallgitter fällt! <white><waves> Wellen</white> – haltet zusammen.</gray>",
                "<gray>The portcullis drops! <white><waves> waves</white> – stick together.</gray>");
        t("room.fight.wave", "<gl>Welle <wave>/<waves></gl>", "<gl>Wave <wave>/<waves></gl>");
        t("room.fight.next", "<gray>Durchatmen … gleich kommt Welle <white><wave>/<waves></white></gray>",
                "<gray>Catch your breath … wave <white><wave>/<waves></white> is coming</gray>");
        t("room.trap.start", "<gray>Fallen! Rauch kündigt Pfeile an, Zischen die Glut, gesprungene Steine geben nach. <white>Alle müssen heil zum verzierten Rand am Ende.</white></gray>",
                "<gray>Traps! Smoke warns of arrows, hissing of embers, cracked stones give way. <white>Everyone must reach the decorated edge at the end.</white></gray>");
        t("room.trap.fell", "<gray>Abgestürzt! Zurück zum Eingang des Gangs.</gray>", "<gray>You fell! Back to the start of the corridor.</gray>");
        t("room.puzzle.start.levers", "<gray>Ein Rätsel: Schaut genau hin, in welcher Reihenfolge die Farben leuchten – dann legt die Hebel genauso um. <white>Die Glocke zeigt es noch einmal.</white></gray>",
                "<gray>A puzzle: watch the order in which the colours light up – then pull the levers the same way. <white>The bell shows it again.</white></gray>");
        t("room.puzzle.start.plates", "<gray>Ein Rätsel: Schaut genau hin, in welcher Reihenfolge die Siegel leuchten – dann tretet in derselben Reihenfolge auf die Platten. <white>Die Glocke zeigt es noch einmal.</white></gray>",
                "<gray>A puzzle: watch the order in which the seals light up – then step on the plates in the same order. <white>The bell shows it again.</white></gray>");
        t("room.puzzle.shown", "<gray>Jetzt ihr!</gray>", "<gray>Your turn!</gray>");
        t("room.puzzle.wait", "<gray>Erst zuschauen …</gray>", "<gray>Watch first …</gray>");
        t("room.puzzle.wrong", "<red>Falsch!</red> <gray>(<white><name></white>) Die Reihenfolge beginnt von vorn – und aus den Ritzen kriechen Spinnen.</gray>",
                "<red>Wrong!</red> <gray>(<white><name></white>) The order starts over – and spiders crawl out of the cracks.</gray>");
        t("room.puzzle.solved", "<em>Gelöst!</em> <gray><white><name></white> hat den letzten Schritt gemacht.</gray>",
                "<em>Solved!</em> <gray><white><name></white> made the final move.</gray>");
        t("room.treasure.start", "<au>Schätze!</au> <gray>Hier stehen <white><chests> Truhen</white> – jede gibt allen Kerkertaler.</gray>",
                "<au>Treasure!</au> <gray>There are <white><chests> chests</white> here – each one gives everyone coins.</gray>");
        t("room.treasure.opened", "<au><name></au> <gray>öffnet eine Truhe: <au>+<taler> Kerkertaler</au> für alle</gray> <item>",
                "<au><name></au> <gray>opens a chest: <au>+<taler> dungeon coins</au> for everyone</gray> <item>");
        t("room.treasure.empty", "<gray>Diese Truhe ist schon leer.</gray>", "<gray>This chest is already empty.</gray>");
        t("room.rest.title", "<em>Rast</em>", "<em>Rest</em>");
        t("room.rest.subtitle", "<gray>Am Wachfeuer heilen alle – wer raus war, ist wieder dabei.</gray>",
                "<gray>Everyone heals by the fire – whoever was out is back.</gray>");
        t("room.shop.start", "<gray>Die <au>Krämerin</au> tauscht Kerkertaler gegen bessere Ausrüstung (Rechtsklick auf sie). <white>Stellt euch alle auf die Treppe</white>, um tiefer zu gehen.</gray>",
                "<gray>The <au>trader</au> swaps dungeon coins for better gear (right-click her). <white>Everyone stand on the stairs</white> to go deeper.</gray>");
        t("room.shop.building", "<gray>Die Treppe wird noch freigeräumt …</gray>", "<gray>The stairs are still being cleared …</gray>");
        t("room.shop.descending", "<gray>Hinab … <white><bar></white></gray>", "<gray>Going down … <white><bar></white></gray>");
        t("kill.taler", "<au>+<taler> Kerkertaler</au>", "<au>+<taler> dungeon coins</au>");

        t("loot.arrows", "<gray>(+8 Pfeile)</gray>", "<gray>(+8 arrows)</gray>");
        t("loot.potion", "<gray>(+ Heiltrank)</gray>", "<gray>(+ healing potion)</gray>");
        t("loot.golden_apple", "<gray>(+ Goldapfel)</gray>", "<gray>(+ golden apple)</gray>");

        // ---------- Gegner ----------
        t("mob.kerkerwache", "Kerkerwache", "Dungeon Guard");
        t("mob.burgschuetze", "Burgschütze", "Castle Archer");
        t("mob.kellerspinne", "Kellerspinne", "Cellar Spider");
        t("mob.gruftwaechter", "Gruftwächter", "Crypt Warden");
        t("mob.kettenknecht", "Kettenknecht", "Chain Henchman");

        // ---------- Kerkermeister ----------
        t("boss.name", "Der Kerkermeister", "The Gaoler");
        t("boss.phase", "Abschnitt <n>", "Phase <n>");
        t("boss.intro.title", "<gl><b>Der Kerkermeister</b></gl>", "<gl><b>The Gaoler</b></gl>");
        t("boss.intro.subtitle", "<gray>Schlüssel klirren in der Dunkelheit …</gray>", "<gray>Keys rattle in the dark …</gray>");
        t("boss.phase2.title", "<gl>Er ruft seine Wachen!</gl>", "<gl>He calls his guards!</gl>");
        t("boss.phase2.subtitle", "<gray>Seelenfeuer zeigt, wo sie kommen.</gray>", "<gray>Soul fire shows where they appear.</gray>");
        t("boss.phase3.title", "<gl>Die Ketten!</gl>", "<gl>The chains!</gl>");
        t("boss.phase3.subtitle", "<gray>Eine graue Linie zielt auf jemanden – lauf weg!</gray>", "<gray>A grey line targets someone – run!</gray>");
        t("boss.warn.shockwave", "<red>⚠ Schockwelle – spring, wenn der Kreis voll ist!</red>", "<red>⚠ Shockwave – jump when the circle is full!</red>");
        t("boss.warn.summon", "<light_purple>⚠ Er ruft Wachen herbei!</light_purple>", "<light_purple>⚠ He's summoning guards!</light_purple>");
        t("boss.warn.chains", "<gray>⚠ Ketten fliegen – geh auf Abstand!</gray>", "<gray>⚠ Chains incoming – get away!</gray>");
        t("boss.dead.title", "<au><b>Besiegt!</b></au>", "<au><b>Defeated!</b></au>");
        t("boss.dead.subtitle", "<gray>Der Kerkermeister lässt seine Schlüssel fallen.</gray>", "<gray>The Gaoler drops his keys.</gray>");

        // ---------- Hinfallen und Wiederbeleben ----------
        t("down.title", "<red>Du liegst am Boden!</red>", "<red>You're down!</red>");
        t("down.subtitle", "<gray>Ein Mitspieler hilft dir: neben dir <white>Schleichen</white> halten.</gray>",
                "<gray>A teammate can help: hold <white>sneak</white> next to you.</gray>");
        t("down.subtitle.solo", "<gray>Du rappelst dich gleich wieder auf …</gray>", "<gray>You'll get back up in a moment …</gray>");
        t("down.bar", "<red>Am Boden</red> <gray>· noch <white><seconds> s</white> · Hilfe: Schleichen neben dir</gray>",
                "<red>Down</red> <gray>· <white><seconds> s</white> left · help: sneak next to you</gray>");
        t("down.bar.solo", "<gray>Du rappelst dich auf …</gray>", "<gray>Getting back up …</gray>");
        t("down.bar.help", "<em>Du wirst wiederbelebt</em> <white><bar></white>", "<em>Being revived</em> <white><bar></white>");
        t("down.bar.helper", "<em>Du hilfst <name></em> <white><bar></white>", "<em>Helping <name></em> <white><bar></white>");
        t("down.other", "<red><name> liegt am Boden!</red> <gray>Schleich neben sie oder ihn, um zu helfen.</gray>",
                "<red><name> is down!</red> <gray>Sneak next to them to help.</gray>");
        t("revived.title", "<em>Wieder auf den Beinen!</em>", "<em>Back on your feet!</em>");
        t("revived.subtitle", "<gray>Noch <white><left></white> Wiederbelebungen.</gray>", "<gray><white><left></white> revives left.</gray>");
        t("revived.other", "<em><name> ist wieder auf den Beinen!</em>", "<em><name> is back on their feet!</em>");
        t("out.title", "<dark_gray>Raus …</dark_gray>", "<dark_gray>Out …</dark_gray>");
        t("out.subtitle", "<gray>Bei der nächsten Rast oder Etage bist du wieder dabei.</gray>",
                "<gray>You'll be back at the next rest or floor.</gray>");
        t("out.other", "<gray><white><name></white> ist raus – bei der nächsten Rast ist sie oder er wieder dabei.</gray>",
                "<gray><white><name></white> is out – they'll be back at the next rest.</gray>");
        t("back.title", "<em>Wieder dabei!</em>", "<em>Back in!</em>");
        t("back.subtitle", "<gray>Weiter geht's.</gray>", "<gray>Let's go on.</gray>");

        // ---------- Ende ----------
        t("win.title", "<au><b>Sieg!</b></au>", "<au><b>Victory!</b></au>");
        t("win.subtitle", "<gray><time> · <white><score></white> Punkte</gray>", "<gray><time> · <white><score></white> points</gray>");
        t("win.summary", "<au>Geschafft!</au> <gray>Zeit <white><time></white> · Räume <white><rooms></white> · Punkte <white><score></white>. Gleich geht es zurück ins Torhaus.</gray>",
                "<au>Done!</au> <gray>Time <white><time></white> · rooms <white><rooms></white> · points <white><score></white>. Back to the gatehouse in a moment.</gray>");
        t("win.best", "<au>★ Neue Bestzeit: <time>!</au>", "<au>★ New best time: <time>!</au>");
        t("lose.title", "<red>Das Verlies hat euch verschluckt</red>", "<red>The dungeon swallowed you</red>");
        t("lose.subtitle", "<gray><rooms> Räume · <white><score></white> Punkte</gray>", "<gray><rooms> rooms · <white><score></white> points</gray>");
        t("lose.summary", "<gray>Alle liegen am Boden. Zeit <white><time></white> · Räume <white><rooms></white> · Punkte <white><score></white>. Beim nächsten Mal!</gray>",
                "<gray>Everyone is down. Time <white><time></white> · rooms <white><rooms></white> · points <white><score></white>. Next time!</gray>");

        // ---------- Krämerin ----------
        t("trader.name", "<au>Krämerin Walburga</au>", "<au>Walburga the Trader</au>");
        t("shop.title", "Krämerin Walburga", "Walburga the Trader");
        t("shop.wallet", "<au>Deine Kerkertaler: <taler></au>", "<au>Your dungeon coins: <taler></au>");
        t("shop.wallet.lore", "<gray>Gibt es für Gegner, Truhen und Räume.</gray>\n<gray>Gelten nur in diesem Abstieg.</gray>",
                "<gray>Earned from enemies, chests and rooms.</gray>\n<gray>Only valid for this run.</gray>");
        t("shop.level", "<gray>Stufe <white><level></white>/<max></gray>", "<gray>Level <white><level></white>/<max></gray>");
        t("shop.price", "<au>Preis: <price> Kerkertaler</au> <gray>· Klicken: kaufen</gray>", "<au>Price: <price> coins</au> <gray>· click to buy</gray>");
        t("shop.price.poor", "<red>Preis: <price> Kerkertaler</red> <gray>(zu wenig)</gray>", "<red>Price: <price> coins</red> <gray>(not enough)</gray>");
        t("shop.maxed", "<em>✔ Höchste Stufe</em>", "<em>✔ Maxed out</em>");
        t("shop.maxed.msg", "<gray>Mehr gibt es davon nicht.</gray>", "<gray>That's as good as it gets.</gray>");
        t("shop.poor", "<gray>Dafür brauchst du <au><price> Kerkertaler</au>.</gray>", "<gray>You need <au><price> coins</au> for that.</gray>");
        t("shop.bought", "<gray>Gekauft:</gray> <item> <gray>für <au><price></au>.</gray>", "<gray>Bought:</gray> <item> <gray>for <au><price></au>.</gray>");
        t("shop.sword", "<white>Schwert verbessern</white>", "<white>Upgrade sword</white>");
        t("shop.sword.desc", "<gray>Stein → Eisen → Diamant → Diamant mit Schärfe.</gray>", "<gray>Stone → iron → diamond → sharpened diamond.</gray>");
        t("shop.armor", "<white>Rüstung verbessern</white>", "<white>Upgrade armour</white>");
        t("shop.armor.desc", "<gray>Leder → Kette → Eisen → Diamant.</gray>", "<gray>Leather → chain → iron → diamond.</gray>");
        t("shop.bow", "<white>Bogen verbessern</white>", "<white>Upgrade bow</white>");
        t("shop.bow.desc", "<gray>Mehr Wucht für deine Pfeile.</gray>", "<gray>More punch for your arrows.</gray>");
        t("shop.shield", "<white>Schild</white>", "<white>Shield</white>");
        t("shop.shield.desc", "<gray>Fängt Pfeile und Hiebe ab (Linkshand).</gray>", "<gray>Blocks arrows and blows (off hand).</gray>");
        t("shop.arrows", "<white>16 Pfeile</white>", "<white>16 arrows</white>");
        t("shop.arrows.desc", "<gray>Nachschub für den Bogen.</gray>", "<gray>Refill for your bow.</gray>");
        t("shop.potion", "<white>Heiltrank</white>", "<white>Healing potion</white>");
        t("shop.potion.desc", "<gray>Heilt sofort 4 Herzen.</gray>", "<gray>Heals 4 hearts at once.</gray>");
        t("shop.golden_apple", "<white>Goldapfel</white>", "<white>Golden apple</white>");
        t("shop.golden_apple.desc", "<gray>Heilt langsam und schützt kurz.</gray>", "<gray>Heals slowly and protects briefly.</gray>");
        t("shop.revive", "<white>Wiederbelebung</white>", "<white>Extra revive</white>");
        t("shop.revive.desc", "<gray>Einmal mehr aufstehen dürfen.</gray>\n<gray>Höchstens zwei pro Abstieg.</gray>",
                "<gray>One more chance to get back up.</gray>\n<gray>At most two per run.</gray>");

        // ---------- Leiste ----------
        t("side.title", "<gl><b>⚒ Burgverlies</b></gl>", "<gl><b>⚒ Castle Dungeon</b></gl>");
        t("side.difficulty", "<gray>Schwierigkeit:</gray> <difficulty>", "<gray>Difficulty:</gray> <difficulty>");
        t("side.floor", "<gray>Etage <white><n>/3</white> · </gray><name>", "<gray>Floor <white><n>/3</white> · </gray><name>");
        t("side.room", "<gray>Raum <white><n>/<of></white> · </gray><name>", "<gray>Room <white><n>/<of></white> · </gray><name>");
        t("side.time", "<gray>Zeit: <white><time></white></gray>", "<gray>Time: <white><time></white></gray>");
        t("side.taler", "<au>⛃ <taler> Kerkertaler</au>", "<au>⛃ <taler> coins</au>");
        t("side.revives", "<gray>Wiederbelebungen: <white><n></white></gray>", "<gray>Revives: <white><n></white></gray>");
        t("side.member.up", "<em>♥</em> <white><name></white>", "<em>♥</em> <white><name></white>");
        t("side.member.down", "<red>✚</red> <red><name></red>", "<red>✚</red> <red><name></red>");
        t("side.member.out", "<dark_gray>✖ <name></dark_gray>", "<dark_gray>✖ <name></dark_gray>");
        t("side.hall.runs", "<gray>Abstiege: <white><n></white></gray>", "<gray>Runs: <white><n></white></gray>");
        t("side.hall.wins", "<gray>Siege: <white><n></white></gray>", "<gray>Wins: <white><n></white></gray>");
        t("side.hall.bosses", "<gray>Kerkermeister: <white><n></white></gray>", "<gray>Gaoler: <white><n></white></gray>");
        t("side.hall.best", "<gray>Bestzeit: <white><time></white></gray>", "<gray>Best time: <white><time></white></gray>");
        t("side.hall.chosen", "<gray>Gewählt:</gray> <difficulty>", "<gray>Selected:</gray> <difficulty>");
        t("side.hall.searching", "<bl>⌕ Suche läuft …</bl>", "<bl>⌕ Searching …</bl>");
        t("side.hall.inside", "<gray>Im Verlies: <white><n></white></gray>", "<gray>In the dungeon: <white><n></white></gray>");

        // ---------- Spuren ----------
        t("trail.none", "<white>Keine Spur</white>", "<white>No trail</white>");
        t("trail.none.how", "<gray>Schlicht und leise.</gray>", "<gray>Plain and quiet.</gray>");
        t("trail.glut", "<gl>Glut</gl>", "<gl>Embers</gl>");
        t("trail.glut.how", "<gray>Freigeschaltet nach dem ersten Abstieg.</gray>", "<gray>Unlocked after your first run.</gray>");
        t("trail.funken", "<bl>Funken</bl>", "<bl>Sparks</bl>");
        t("trail.funken.how", "<gray>Freigeschaltet nach 25 geschafften Räumen.</gray>", "<gray>Unlocked after 25 cleared rooms.</gray>");
        t("trail.seelenfeuer", "<bl>Seelenfeuer</bl>", "<bl>Soul fire</bl>");
        t("trail.seelenfeuer.how", "<gray>Freigeschaltet, wenn du den Kerkermeister besiegt hast.</gray>", "<gray>Unlocked once you've defeated the Gaoler.</gray>");
        t("trail.goldstaub", "<au>Goldstaub</au>", "<au>Gold dust</au>");
        t("trail.goldstaub.how", "<gray>Freigeschaltet mit einem Sieg auf „Schwer“.</gray>", "<gray>Unlocked with a win on “Hard”.</gray>");
        t("trail.chosen", "<em>✔ Gewählt</em>", "<em>✔ Selected</em>");
        t("trail.choose", "<white>Klicken: wählen</white>", "<white>Click: select</white>");
        t("trail.locked", "<dark_gray>Noch gesperrt</dark_gray>", "<dark_gray>Still locked</dark_gray>");

        // ---------- Verwaltung ----------
        t("admin.usage", "<gray>/bvadmin info · neubauen · neuladen · debug [spieler] · loesen [spieler] · taler [spieler] [anzahl]</gray>",
                "<gray>/bvadmin info · neubauen (rebuild) · neuladen (reload) · debug [player] · loesen (solve) [player] · taler [player] [amount]</gray>");
        t("admin.info", "<gray>Abstiege: <white><runs></white>/<white><max></white> · im Verlies: <white><inside></white> · suchen: <white><queued></white> · Netzwerk: <white><net></white></gray>",
                "<gray>Runs: <white><runs></white>/<white><max></white> · in the dungeon: <white><inside></white> · searching: <white><queued></white> · network: <white><net></white></gray>");
        t("admin.rebuilt", "<em>Torhaus neu gebaut.</em>", "<em>Gatehouse rebuilt.</em>");
        t("admin.reloaded", "<gray>Einstellungen neu geladen.</gray>", "<gray>Settings reloaded.</gray>");
        t("admin.no-player", "<gray>Wen meinst du? (Name eines Spielers, der online ist)</gray>", "<gray>Who do you mean? (name of an online player)</gray>");
        t("admin.no-run", "<gray>Diese Person ist in keinem Abstieg.</gray>", "<gray>That player isn't in a run.</gray>");
        t("admin.solved", "<em>Raum geschafft.</em>", "<em>Room cleared.</em>");
        t("admin.not-solved", "<gray>Gerade läuft kein Raum, der sich lösen lässt.</gray>", "<gray>No room that can be solved right now.</gray>");
        t("admin.taler", "<au>+<n> Kerkertaler.</au>", "<au>+<n> dungeon coins.</au>");
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; nicht kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „⚒ Burgverlies »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }
}
