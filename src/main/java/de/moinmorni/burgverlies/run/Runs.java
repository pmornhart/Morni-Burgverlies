package de.moinmorni.burgverlies.run;

import de.moinmorni.burgverlies.BurgverliesPlugin;
import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.Ui;
import de.moinmorni.burgverlies.data.Leaderboard;
import de.moinmorni.burgverlies.data.PlayerData;
import de.moinmorni.burgverlies.dungeon.Cosmetics;
import de.moinmorni.burgverlies.dungeon.Difficulty;
import de.moinmorni.burgverlies.dungeon.Dir;
import de.moinmorni.burgverlies.dungeon.DungeonGenerator;
import de.moinmorni.burgverlies.dungeon.FloorPlan;
import de.moinmorni.burgverlies.dungeon.Loot;
import de.moinmorni.burgverlies.dungeon.MobKind;
import de.moinmorni.burgverlies.dungeon.PlacedRoom;
import de.moinmorni.burgverlies.dungeon.Pos;
import de.moinmorni.burgverlies.dungeon.Revives;
import de.moinmorni.burgverlies.dungeon.RoomType;
import de.moinmorni.burgverlies.dungeon.Score;
import de.moinmorni.burgverlies.dungeon.TrapLayout;
import de.moinmorni.burgverlies.world.Builder;
import de.moinmorni.burgverlies.world.Doors;
import de.moinmorni.burgverlies.world.Layout;
import de.moinmorni.burgverlies.world.Plan;
import de.moinmorni.burgverlies.world.RoomPlans;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.StringJoiner;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

/**
 * Alle laufenden Abstiege: starten, Etagen bauen, Räume betreten und schaffen, nächste Etage, Sieg oder
 * Niederlage, Statistik. Jeder Abstieg hat seinen eigenen Platz in der Welt.
 */
public final class Runs {

    private final BurgverliesPlugin plugin;
    private final RoomLogic logic;
    private final Downs downs;
    private final ShopMenu shop;
    private final Map<Integer, Run> runs = new LinkedHashMap<>();
    private final Map<UUID, Run> byPlayer = new HashMap<>();
    private final Random random = new Random();
    private int nextId = 1 + new Random().nextInt(1_000_000) * 100;
    private long tick;

    public Runs(BurgverliesPlugin plugin) {
        this.plugin = plugin;
        this.logic = new RoomLogic(this);
        this.downs = new Downs(this);
        this.shop = new ShopMenu(this);
    }

    public BurgverliesPlugin plugin() {
        return plugin;
    }

    Downs downs() {
        return downs;
    }

    public ShopMenu shop() {
        return shop;
    }

    public Optional<Run> of(Player p) {
        return Optional.ofNullable(byPlayer.get(p.getUniqueId()));
    }

    public Optional<Run> of(UUID u) {
        return Optional.ofNullable(byPlayer.get(u));
    }

    public Run byId(int id) {
        return runs.get(id);
    }

    public Collection<Run> all() {
        return List.copyOf(runs.values());
    }

    public int running() {
        return runs.size();
    }

    public int playersInside() {
        return byPlayer.size();
    }

    public boolean hasFreeSlot() {
        return freeSlot() >= 0;
    }

    private int freeSlot() {
        for (int s = 0; s < plugin.settings().maxRuns(); s++) {
            final int slot = s;
            if (runs.values().stream().noneMatch(r -> r.slot == slot)) {
                return s;
            }
        }
        return -1;
    }

    String lang(Player p) {
        return plugin.lang(p);
    }

    // ---------- Start ----------

    /** Abstieg mit dieser Gruppe beginnen; {@code false}, wenn gerade alle Verliese belegt sind. */
    public boolean start(List<Player> team, Difficulty diff) {
        int slot = freeSlot();
        if (slot < 0 || team.isEmpty()) {
            return false;
        }
        Map<UUID, String> players = new LinkedHashMap<>();
        for (Player p : team) {
            players.put(p.getUniqueId(), p.getName());
        }
        int bleedSeconds = plugin.settings().bleedSeconds() > 0 ? plugin.settings().bleedSeconds()
                : diff.bleedSeconds();
        Run run = new Run(nextId++, slot, random.nextLong(), diff, lang(team.get(0)), players,
                plugin.settings().revives(), bleedSeconds * 20 / Downs.STEP,
                plugin.settings().reviveSeconds() * 20 / Downs.STEP);
        runs.put(run.id, run);
        for (Player p : team) {
            byPlayer.put(p.getUniqueId(), run);
            plugin.queue().remove(p.getUniqueId(), false);
            p.closeInventory();
            p.getInventory().clear();
            p.sendMessage(Texts.msg(lang(p), "run.preparing", Placeholder.component("difficulty",
                    Texts.text(lang(p), "difficulty." + diff.id()))));
            Ui.title(p, Texts.text(lang(p), "run.preparing.title"), Texts.text(lang(p), "run.preparing.subtitle"), 2500);
        }
        for (int f = 0; f < run.plans.length; f++) {
            run.plans[f] = DungeonGenerator.generate(run.seed, f, plugin.settings().roomsPerFloor());
            List<Room> rooms = new ArrayList<>();
            for (PlacedRoom pr : run.plans[f].rooms()) {
                Room room = new Room(pr);
                var trap = pr.template().trap();
                if (trap != null && trap.crumbles()) {
                    room.crumble = TrapLayout.create(new Random(run.plans[f].seed() + pr.index()), pr.template().half(),
                            trap.crumbleMin(), trap.crumbleMax(), 0.6);
                }
                rooms.add(room);
            }
            run.rooms[f] = rooms;
        }
        for (int f = 0; f < run.plans.length; f++) {
            final int floor = f;
            buildFloor(run, f, () -> {
                run.built[floor] = true;
                if (floor == 0) {
                    begin(run);
                }
            });
        }
        plugin.stateChanged();
        return true;
    }

    private void buildFloor(Run run, int floor, Runnable done) {
        FloorPlan plan = run.plans[floor];
        Builder b = plugin.builder();
        List<Builder.Put> puts = new ArrayList<>();
        List<Plan.Banner> banners = new ArrayList<>();
        for (Room room : run.rooms[floor]) {
            PlacedRoom pr = room.placed;
            Plan p = RoomPlans.build(pr.template(), floor, plan.seed() + pr.index(), room.crumble);
            int[] c = Layout.center(run.slot, floor, pr);
            puts.addAll(b.transform(p, c[0], c[1], c[2], pr.rotation(), banners));
        }
        for (int i = 0; i + 1 < plan.size(); i++) {
            puts.addAll(Doors.corridor(run.slot, floor, plan.room(i), plan.room(i + 1), plan.seed()));
        }
        for (PlacedRoom pr : plan.rooms()) {
            if (pr.entry() != null) {
                puts.addAll(Doors.gate(run.slot, floor, pr, pr.entry(), false));
            }
            if (pr.exit() != null) {
                puts.addAll(Doors.gate(run.slot, floor, pr, pr.exit(), true));
            }
        }
        b.place(plugin.world(), puts, banners, () -> {
            if (run.stage != Run.Stage.ENDED) {
                done.run();
            }
        });
    }

    private void begin(Run run) {
        if (run.stage == Run.Stage.ENDED) {
            return;
        }
        run.stage = Run.Stage.PLAYING;
        run.startMs = System.currentTimeMillis();
        run.floor = 0;
        run.current = 0;
        Location start = startPoint(run);
        for (Player p : players(run)) {
            Hero h = run.hero(p.getUniqueId());
            p.teleport(start);
            p.setGameMode(GameMode.ADVENTURE);
            Downs.reset(p);
            RoomLogic.heal(p);
            Gear.start(p, h, run.difficulty, lang(p));
        }
        arrive(run);
        plugin.stateChanged();
    }

    private Location startPoint(Run run) {
        Room r = run.rooms[run.floor].get(0);
        return at(run, r, new Pos(0, 1, -3), Layout.inward(r.placed));
    }

    /** Ankunft auf einer Etage: Titel, Ankunftsraum offen. */
    private void arrive(Run run) {
        Room r = run.rooms[run.floor].get(0);
        logic.start(run, r);
        for (Player p : players(run)) {
            String lang = lang(p);
            Ui.title(p, Texts.text(lang, "floor.title", Placeholder.unparsed("n", String.valueOf(run.floor + 1))),
                    Texts.text(lang, "floor." + run.floor), 3000);
            Ui.sound(p, "block.iron_door.open", 0.6f);
        }
        sidebars(run);
    }

    // ---------- Jeder Tick ----------

    public void tick() {
        tick++;
        for (Run run : List.copyOf(runs.values())) {
            if (run.stage != Run.Stage.PLAYING) {
                continue;
            }
            try {
                logic.tick(run, run.room());
                if (run.stage != Run.Stage.PLAYING) {
                    continue;
                }
                if (tick % Downs.STEP == 0) {
                    detectEnter(run);
                    downs.tick(run);
                    trails(run);
                }
                if (tick % 10 == 0) {
                    sidebars(run);
                }
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Fehler im Abstieg " + run.id, e);
            }
        }
    }

    private void detectEnter(Run run) {
        Room cur = run.room();
        if (cur.phase != Room.Phase.CLEARED || run.current + 1 >= run.floorRooms().size()) {
            return;
        }
        Room next = run.floorRooms().get(run.current + 1);
        for (Player p : players(run)) {
            if (run.revives.state(p.getUniqueId()) == Revives.State.UP && inside(run, next, p.getLocation(), 0)) {
                enter(run, next);
                return;
            }
        }
    }

    private void enter(Run run, Room room) {
        run.current = room.index();
        Location entry = entry(run, room);
        for (Player p : players(run)) {
            if (!inside(run, room, p.getLocation(), 0)) {
                p.teleport(p.getGameMode() == GameMode.SPECTATOR ? roomCenter(run, room) : entry);
                p.setFallDistance(0);
            }
            String lang = lang(p);
            Ui.title(p, Component.empty(), Texts.text(lang, "room." + room.placed.template().id()), 1500);
        }
        if (room.placed.type().locks()) {
            Builder.now(plugin.world(), Doors.gate(run.slot, run.floor, room.placed, room.placed.entry(), true));
            soundAll(run, "block.iron_door.close", 0.5f);
        }
        logic.start(run, room);
        sidebars(run);
    }

    /** Raum geschafft: Tore auf, Belohnung, beim Kerkermeister: Sieg. */
    void cleared(Run run, Room room) {
        if (room.phase == Room.Phase.CLEARED) {
            return;
        }
        room.phase = Room.Phase.CLEARED;
        RoomType type = room.placed.type();
        if (type != RoomType.START && type != RoomType.SHOP) {
            run.roomsCleared++;
        }
        for (UUID m : List.copyOf(room.mobs)) {
            Entity e = plugin.getServer().getEntity(m);
            if (e != null) {
                e.getWorld().spawnParticle(Particle.POOF, e.getLocation().add(0, 0.5, 0), 8, 0.2, 0.3, 0.2, 0.02);
                e.remove();
            }
        }
        room.mobs.clear();
        if (room.crumble != null) {
            logic.restoreCrumble(run, room);
        }
        int bonus = Loot.roomBonus(type, run.floor, run.difficulty);
        for (Hero h : run.active()) {
            h.earn(bonus);
        }
        if (type.locks() && room.placed.entry() != null) {
            Builder.now(plugin.world(), Doors.gate(run.slot, run.floor, room.placed, room.placed.entry(), false));
        }
        if (room.placed.exit() != null) {
            Builder.now(plugin.world(), Doors.gate(run.slot, run.floor, room.placed, room.placed.exit(), false));
        }
        if (type.locks()) {
            for (Player p : players(run)) {
                String lang = lang(p);
                p.sendMessage(Texts.msg(lang, "room.cleared", Placeholder.unparsed("taler", String.valueOf(bonus))));
                Ui.sound(p, "block.iron_door.open", 0.8f);
                Ui.sound(p, "entity.player.levelup", 1.2f);
            }
        }
        if (type == RoomType.BOSS) {
            run.bossKilled = true;
            if (room.boss != null) {
                room.boss.hideBar();
            }
            finish(run, true);
        }
        sidebars(run);
    }

    /** Alle stehen auf der Treppe der Krämerstube: eine Etage tiefer. */
    void nextFloor(Run run) {
        int next = run.floor + 1;
        if (next >= run.plans.length || !run.built[next]) {
            return;
        }
        Room shopRoom = run.room();
        if (shopRoom.npc != null) {
            Entity npc = plugin.getServer().getEntity(shopRoom.npc);
            if (npc != null) {
                npc.remove();
            }
        }
        for (Player p : players(run)) {
            p.closeInventory();
        }
        run.floor = next;
        run.current = 0;
        Location start = startPoint(run);
        downs.restoreAll(run, start);
        for (Player p : players(run)) {
            p.teleport(start);
            p.setFallDistance(0);
        }
        arrive(run);
    }

    // ---------- Ende ----------

    void lose(Run run) {
        if (run.stage == Run.Stage.ENDED) {
            return;
        }
        finish(run, false);
    }

    private void finish(Run run, boolean won) {
        if (run.stage == Run.Stage.ENDED) {
            return;
        }
        run.stage = Run.Stage.ENDED;
        for (Room r : run.floorRooms()) {
            if (r.boss != null) {
                r.boss.hideBar();
            }
        }
        long time = run.elapsed();
        int score = Score.compute(won, run.roomsCleared, run.revives.totalDowns(), time, run.difficulty);
        List<Player> online = players(run);
        StringJoiner names = new StringJoiner(", ");
        for (Hero h : run.active()) {
            names.add(h.name());
        }
        int place = 0;
        if (run.startMs > 0) {
            place = plugin.board().add(new Leaderboard.Entry(names.toString(), score, time, run.difficulty,
                    run.size(), won));
            plugin.store().write(plugin.board().file(), plugin.board().toYaml());
        }
        for (Hero h : run.active()) {
            record(run, h, won, score, time);
        }
        for (Player p : online) {
            String lang = lang(p);
            Downs.reset(p);
            if (p.getGameMode() == GameMode.SPECTATOR) {
                p.setGameMode(GameMode.ADVENTURE);
                p.teleport(roomCenter(run, run.room()));
            }
            TagResolver r = TagResolver.resolver(
                    Placeholder.unparsed("time", Ui.time(time)),
                    Placeholder.unparsed("score", String.valueOf(score)),
                    Placeholder.unparsed("rooms", String.valueOf(run.roomsCleared)),
                    Placeholder.unparsed("place", String.valueOf(place)));
            if (won) {
                Ui.title(p, Texts.text(lang, "win.title"), Texts.text(lang, "win.subtitle", r), 5000);
                Ui.sound(p, "ui.toast.challenge_complete", 1f);
            } else {
                Ui.title(p, Texts.text(lang, "lose.title"), Texts.text(lang, "lose.subtitle", r), 4000);
                Ui.sound(p, "entity.wither.death", 0.6f);
            }
            p.sendMessage(Texts.msg(lang, won ? "win.summary" : "lose.summary", r));
            if (place > 0 && place <= 3) {
                p.sendMessage(Texts.msg(lang, "board.place", r));
            }
        }
        if (won && !online.isEmpty()) {
            fireworks(online.get(0).getLocation());
        }
        plugin.boards().update();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : players(run)) {
                byPlayer.remove(p.getUniqueId());
                plugin.hall().enter(p);
            }
            cleanup(run);
        }, plugin.settings().endSeconds() * 20L);
        plugin.stateChanged();
    }

    private void record(Run run, Hero h, boolean won, int score, long time) {
        if (run.startMs == 0) {
            return;
        }
        UUID u = h.uuid();
        PlayerData d = plugin.store().read(u).orElseGet(() -> new PlayerData(u, h.name()));
        boolean wasBest = d.record(run.difficulty, won, won && run.bossKilled, run.roomsCleared,
                run.revives.downs(u), h.earned(), score, time);
        plugin.store().save(d);
        Map<String, Long> add = new LinkedHashMap<>();
        add.put("runs", 1L);
        add.put("wins", won ? 1L : 0L);
        add.put("bosses", won && run.bossKilled ? 1L : 0L);
        add.put("rooms", (long) run.roomsCleared);
        Map<String, Long> best = won ? Map.of("best_time_ms", time) : Map.of();
        plugin.net().stats(u, add, best, true);
        plugin.net().stats(u, Map.of(), Map.of("best_score", (long) score), false);
        Player p = plugin.getServer().getPlayer(u);
        if (p != null && wasBest) {
            p.sendMessage(Texts.msg(lang(p), "win.best", Placeholder.unparsed("time", Ui.time(time))));
        }
    }

    private void fireworks(Location l) {
        Firework fw = l.getWorld().spawn(l.clone().add(0, 1, 0), Firework.class, f -> {
            var meta = f.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.fromRGB(0xF2B632), Color.fromRGB(0xE0603A)).withFade(Color.WHITE).build());
            meta.setPower(0);
            f.setFireworkMeta(meta);
        });
        fw.addScoreboardTag("bv_firework");
    }

    private void cleanup(Run run) {
        for (UUID u : run.entities) {
            Entity e = plugin.getServer().getEntity(u);
            if (e != null) {
                e.remove();
            }
        }
        run.entities.clear();
        byPlayer.values().removeIf(r -> r == run);
        runs.remove(run.id);
        plugin.stateChanged();
    }

    /** Jemand verlässt den Abstieg ({@code quit} = Verbindung weg). */
    public void leave(Player p, boolean quit) {
        Run run = byPlayer.remove(p.getUniqueId());
        if (run == null) {
            return;
        }
        Hero h = run.hero(p.getUniqueId());
        if (run.stage != Run.Stage.ENDED && h != null && !h.left()) {
            if (run.stage == Run.Stage.PLAYING) {
                record(run, h, false, Score.compute(false, run.roomsCleared, run.revives.downs(p.getUniqueId()),
                        run.elapsed(), run.difficulty), run.elapsed());
            }
            h.leave();
            run.revives.remove(p.getUniqueId());
            broadcast(run, "run.left", Placeholder.unparsed("name", p.getName()));
        }
        for (Room r : run.floorRooms()) {
            if (r.boss != null) {
                r.boss.hideFor(p);
            }
        }
        Downs.reset(p);
        if (!quit) {
            plugin.hall().enter(p);
        }
        if (run.stage == Run.Stage.ENDED) {
            return;
        }
        if (players(run).isEmpty()) {
            run.stage = Run.Stage.ENDED;
            for (Room r : run.floorRooms()) {
                if (r.boss != null) {
                    r.boss.hideBar();
                }
            }
            cleanup(run);
        } else if (run.stage == Run.Stage.PLAYING && run.revives.allDown()) {
            lose(run);
        }
        plugin.stateChanged();
    }

    /** Server fährt herunter: alle zurück ins Torhaus, keine Wertung. */
    public void abortAll() {
        for (Run run : List.copyOf(runs.values())) {
            run.stage = Run.Stage.ENDED;
            for (Room r : run.floorRooms() == null ? List.<Room>of() : run.floorRooms()) {
                if (r.boss != null) {
                    r.boss.hideBar();
                }
            }
            for (Player p : players(run)) {
                Downs.reset(p);
                plugin.hall().enter(p);
            }
            cleanup(run);
        }
        byPlayer.clear();
    }

    // ---------- Ereignisse ----------

    /** Gegner besiegt. */
    public void mobDied(Entity e, Player killer) {
        Run run = runs.get(Mobs.runOf(e));
        if (run == null || run.stage != Run.Stage.PLAYING) {
            return;
        }
        run.entities.remove(e.getUniqueId());
        for (Room room : run.floorRooms()) {
            if (room.boss != null && room.boss.is(e)) {
                room.boss.hideBar();
                for (Player p : players(run)) {
                    Ui.title(p, Texts.text(lang(p), "boss.dead.title"), Texts.text(lang(p), "boss.dead.subtitle"),
                            3000);
                }
                cleared(run, room);
                return;
            }
            if (room.mobs.contains(e.getUniqueId())) {
                logic.mobDied(run, room, e);
            }
        }
        MobKind kind = Mobs.kindOf(e);
        if (killer != null && kind != null) {
            Hero h = run.hero(killer.getUniqueId());
            if (h != null) {
                int t = Loot.kill(kind, run.floor, run.difficulty);
                h.earn(t);
                killer.sendActionBar(Texts.text(lang(killer), "kill.taler", Placeholder.unparsed("taler",
                        String.valueOf(t))));
                Ui.sound(killer, "entity.experience_orb.pickup", 1.2f);
            }
        }
    }

    /** Ist das Wesen der Kerkermeister, der gerade unverwundbar ist? */
    public boolean bossShielded(Entity e) {
        Run run = runs.get(Mobs.runOf(e));
        if (run == null) {
            return false;
        }
        for (Room r : run.floorRooms()) {
            if (r.boss != null && r.boss.is(e)) {
                return r.boss.shielded();
            }
        }
        return false;
    }

    /** Schlag des Kerkermeisters (fester Wert statt Axt-Schaden). */
    public double bossMelee(Entity e) {
        Run run = runs.get(Mobs.runOf(e));
        if (run != null) {
            for (Room r : run.floorRooms()) {
                if (r.boss != null && r.boss.is(e)) {
                    return r.boss.melee();
                }
            }
        }
        return 6;
    }

    /** Schaden-Faktor der Gegner dieses Abstiegs. */
    public double damageFactor(Entity mob) {
        Run run = runs.get(Mobs.runOf(mob));
        return run == null ? 1 : de.moinmorni.burgverlies.dungeon.Scaling.mobDamage(run.difficulty, run.size());
    }

    /** Tödlicher Treffer: hinfallen statt sterben. */
    public void fall(Player p) {
        of(p).ifPresent(run -> {
            if (run.stage == Run.Stage.PLAYING) {
                downs.fall(run, p);
            }
        });
    }

    public Revives.State state(Player p) {
        Run run = byPlayer.get(p.getUniqueId());
        return run == null ? null : run.revives.state(p.getUniqueId());
    }

    /** Klick auf einen Block; {@code true} = erledigt. */
    public boolean interact(Player p, Block b, Action a) {
        Run run = byPlayer.get(p.getUniqueId());
        if (run == null || run.stage != Run.Stage.PLAYING) {
            return false;
        }
        return logic.interact(run, p, b, a);
    }

    /** Klick auf ein Wesen (Krämerin). */
    public boolean interactEntity(Player p, Entity e) {
        Run run = byPlayer.get(p.getUniqueId());
        if (run == null || !e.getScoreboardTags().contains("bv_trader")) {
            return false;
        }
        if (run.revives.state(p.getUniqueId()) == Revives.State.UP) {
            shop.open(p, run);
        }
        return true;
    }

    /** Kerkermeister-Leiste wieder zeigen (nach Sprachwechsel o. Ä.). */
    public void rejoin(Player p) {
        of(p).ifPresent(run -> run.floorRooms().forEach(r -> {
            if (r.boss != null) {
                r.boss.showBar(p);
            }
        }));
    }

    // ---------- Orte ----------

    int[] center(Run run, Room room) {
        return Layout.center(run.slot, run.floor, room.placed);
    }

    int[] block(Run run, Room room, Pos local) {
        return Layout.block(run.slot, run.floor, room.placed, local);
    }

    Location at(Run run, Room room, Pos local, Dir facing) {
        return Layout.at(plugin.world(), run.slot, run.floor, room.placed, local, facing);
    }

    boolean inside(Run run, Room room, Location l, double margin) {
        return l.getWorld() == plugin.world() && Layout.inside(l, run.slot, run.floor, room.placed, margin);
    }

    Pos local(Run run, Room room, Location l) {
        return Layout.toLocal(l, run.slot, run.floor, room.placed);
    }

    /** Gleich hinter der Eingangstür, Blick in den Raum. */
    Location entry(Run run, Room room) {
        int h = room.placed.template().half();
        return at(run, room, new Pos(0, 1, -h + 1), Layout.inward(room.placed));
    }

    Location roomCenter(Run run, Room room) {
        return at(run, room, new Pos(0, 2, 0), Layout.inward(room.placed));
    }

    // ---------- Mitteilungen ----------

    /** Alle, die noch dabei und online sind. */
    List<Player> players(Run run) {
        List<Player> out = new ArrayList<>();
        for (Hero h : run.heroes.values()) {
            if (h.left()) {
                continue;
            }
            Player p = plugin.getServer().getPlayer(h.uuid());
            if (p != null && byPlayer.get(h.uuid()) == run) {
                out.add(p);
            }
        }
        return out;
    }

    void broadcast(Run run, String key, TagResolver... r) {
        for (Player p : players(run)) {
            p.sendMessage(Texts.msg(lang(p), key, r));
        }
    }

    void broadcastExcept(Run run, Player except, String key, TagResolver... r) {
        for (Player p : players(run)) {
            if (p != except) {
                p.sendMessage(Texts.msg(lang(p), key, r));
            }
        }
    }

    void soundAll(Run run, String key, float pitch) {
        for (Player p : players(run)) {
            Ui.sound(p, key, pitch);
        }
    }

    private void trails(Run run) {
        for (Player p : players(run)) {
            if (run.revives.state(p.getUniqueId()) != Revives.State.UP) {
                continue;
            }
            Cosmetics.Trail t = plugin.store().loaded(p.getUniqueId()).map(PlayerData::trail)
                    .orElse(Cosmetics.Trail.NONE);
            Location l = p.getLocation().add(0, 0.1, 0);
            switch (t) {
                case GLUT -> l.getWorld().spawnParticle(Particle.SMALL_FLAME, l, 2, 0.15, 0.02, 0.15, 0.005);
                case FUNKEN -> l.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, l, 2, 0.15, 0.05, 0.15, 0.02);
                case SEELENFEUER -> l.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, l, 2, 0.15, 0.02, 0.15, 0.005);
                case GOLDSTAUB -> l.getWorld().spawnParticle(Particle.DUST, l, 3, 0.2, 0.05, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(0xF2B632), 0.9f));
                default -> {
                    // keine Spur
                }
            }
        }
    }

    /** Leiste rechts für alle im Abstieg. */
    void sidebars(Run run) {
        for (Player p : players(run)) {
            String lang = lang(p);
            List<Component> lines = new ArrayList<>();
            Room room = run.room();
            lines.add(Texts.text(lang, "side.difficulty", Placeholder.component("difficulty",
                    Texts.text(lang, "difficulty." + run.difficulty.id()))));
            lines.add(Texts.text(lang, "side.floor", Placeholder.unparsed("n", String.valueOf(run.floor + 1)),
                    Placeholder.component("name", Texts.text(lang, "floor." + run.floor))));
            lines.add(Texts.text(lang, "side.room", Placeholder.unparsed("n", String.valueOf(run.current + 1)),
                    Placeholder.unparsed("of", String.valueOf(run.floorRooms().size())),
                    Placeholder.component("name", Texts.text(lang, "room." + room.placed.template().id()))));
            lines.add(Texts.text(lang, "side.time", Placeholder.unparsed("time", Ui.time(run.elapsed()))));
            lines.add(Component.empty());
            Hero me = run.hero(p.getUniqueId());
            lines.add(Texts.text(lang, "side.taler", Placeholder.unparsed("taler",
                    String.valueOf(me == null ? 0 : me.taler()))));
            lines.add(Texts.text(lang, "side.revives", Placeholder.unparsed("n",
                    String.valueOf(run.revives.revivesLeft(p.getUniqueId())))));
            lines.add(Component.text(" "));
            for (Hero h : run.active()) {
                String key = switch (run.revives.state(h.uuid())) {
                    case UP -> "side.member.up";
                    case DOWN -> "side.member.down";
                    case OUT -> "side.member.out";
                };
                lines.add(Texts.text(lang, key, Placeholder.unparsed("name", h.name())));
            }
            plugin.sidebar().show(p, Texts.text(lang, "side.title"), lines);
        }
    }

    // ---------- Prüfen (/bvadmin debug) ----------

    /** Zustand eines Abstiegs als eine Zeile JSON (für Tests und Fehlersuche). */
    public String debug(Player p) {
        Run run = byPlayer.get(p.getUniqueId());
        if (run == null) {
            return "{\"run\":null}";
        }
        StringBuilder s = new StringBuilder("{\"run\":").append(run.id).append(",\"stage\":\"").append(run.stage)
                .append("\",\"difficulty\":\"").append(run.difficulty.id()).append("\",\"floor\":").append(run.floor)
                .append(",\"built\":[").append(run.built[0]).append(',').append(run.built[1]).append(',')
                .append(run.built[2]).append("],\"room\":").append(run.current).append(",\"rooms\":")
                .append(run.floorRooms().size()).append(",\"cleared\":").append(run.roomsCleared);
        Room r = run.room();
        s.append(",\"cur\":").append(roomJson(run, r));
        if (run.current + 1 < run.floorRooms().size()) {
            s.append(",\"next\":").append(roomJson(run, run.floorRooms().get(run.current + 1)));
        }
        s.append(",\"heroes\":{");
        StringJoiner hj = new StringJoiner(",");
        for (Hero h : run.heroes.values()) {
            hj.add("\"" + h.name() + "\":{\"state\":\"" + run.revives.state(h.uuid()) + "\",\"revives\":"
                    + run.revives.revivesLeft(h.uuid()) + ",\"downs\":" + run.revives.downs(h.uuid()) + ",\"taler\":"
                    + h.taler() + ",\"sword\":" + h.level(de.moinmorni.burgverlies.dungeon.Offer.SWORD) + ",\"left\":"
                    + h.left() + "}");
        }
        s.append(hj).append("}}");
        return s.toString();
    }

    private String roomJson(Run run, Room r) {
        StringBuilder s = new StringBuilder("{\"i\":").append(r.index()).append(",\"type\":\"")
                .append(r.placed.type()).append("\",\"id\":\"").append(r.placed.template().id())
                .append("\",\"phase\":\"").append(r.phase).append('"')
                .append(",\"entry\":").append(xyz(entry(run, r))).append(",\"center\":")
                .append(xyz(at(run, r, new Pos(0, 1, 0), null)));
        if (r.placed.entry() != null) {
            s.append(",\"gateIn\":\"").append(gate(run, r, r.placed.entry())).append('"');
        }
        if (r.placed.exit() != null) {
            s.append(",\"gateOut\":\"").append(gate(run, r, r.placed.exit())).append('"');
        }
        switch (r.placed.type()) {
            case FIGHT -> s.append(",\"wave\":").append(r.wave).append(",\"waves\":").append(r.waves.size())
                    .append(",\"mobs\":").append(r.mobs.size());
            case TRAP -> s.append(",\"fires\":").append(r.trapFires).append(",\"falls\":").append(r.falls)
                    .append(",\"goal\":").append(xyz(at(run, r, new Pos(0, 1, r.placed.template().goalZ()), null)))
                    .append(",\"crumble\":").append(r.crumble == null ? 0 : r.crumble.count());
            case PUZZLE -> {
                s.append(",\"plates\":").append(r.placed.template().plates()).append(",\"mobs\":").append(r.mobs.size());
                if (r.puzzle != null) {
                    s.append(",\"order\":").append(r.puzzle.order()).append(",\"done\":").append(r.puzzle.done())
                            .append(",\"mistakes\":").append(r.puzzle.mistakes()).append(",\"showing\":")
                            .append(r.showStep >= 0);
                }
                StringJoiner in = new StringJoiner(",", "[", "]");
                for (Pos p : r.placed.template().inputs()) {
                    int[] b = block(run, r, p);
                    in.add("[" + b[0] + "," + b[1] + "," + b[2] + "]");
                }
                s.append(",\"inputs\":").append(in).append(",\"stand\":")
                        .append(xyz(at(run, r, new Pos(0, 1, -r.placed.template().half() + 3), null)));
            }
            case TREASURE -> {
                StringJoiner in = new StringJoiner(",", "[", "]");
                for (Pos p : r.placed.template().chests()) {
                    int[] b = block(run, r, p);
                    in.add("[" + b[0] + "," + b[1] + "," + b[2] + "]");
                }
                s.append(",\"chests\":").append(in).append(",\"opened\":").append(r.opened.size());
            }
            case SHOP -> s.append(",\"npc\":\"").append(r.npc == null ? "" : r.npc.toString()).append("\",\"stairs\":")
                    .append(xyz(at(run, r, new Pos(0, 1, 4), null))).append(",\"trader\":")
                    .append(xyz(at(run, r, r.placed.template().npc().add(2, 0, 0), null)));
            case BOSS -> s.append(",\"intro\":").append(r.intro).append(",\"mobs\":").append(r.mobs.size())
                    .append(",\"boss\":").append(r.boss == null ? "null" : r.boss.json());
            default -> {
                // nichts weiter
            }
        }
        return s.append('}').toString();
    }

    private String gate(Run run, Room r, Dir side) {
        int[] b = Doors.blocks(run.slot, run.floor, r.placed, side).get(1);
        return plugin.world().getBlockAt(b[0], b[1], b[2]).getType() == Material.IRON_BARS ? "closed" : "open";
    }

    private static String xyz(Location l) {
        return "[" + fmt(l.getX()) + "," + fmt(l.getY()) + "," + fmt(l.getZ()) + "]";
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    /** /bvadmin loesen: aktuellen Raum sofort schaffen (Gegner weg, Rätsel gelöst). */
    public boolean solve(Player p) {
        Run run = byPlayer.get(p.getUniqueId());
        if (run == null || run.stage != Run.Stage.PLAYING) {
            return false;
        }
        Room r = run.room();
        if (r.phase != Room.Phase.ACTIVE) {
            return false;
        }
        if (r.boss != null && r.boss.alive()) {
            return false;
        }
        cleared(run, r);
        return true;
    }

    /** /bvadmin taler */
    public boolean giveTaler(Player p, int n) {
        Run run = byPlayer.get(p.getUniqueId());
        if (run == null) {
            return false;
        }
        run.hero(p.getUniqueId()).earn(n);
        sidebars(run);
        return true;
    }

    World world() {
        return plugin.world();
    }
}
