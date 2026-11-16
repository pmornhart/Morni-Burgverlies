package net.morni.burgverlies.run;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.burgverlies.Texts;
import net.morni.burgverlies.Ui;
import net.morni.burgverlies.dungeon.Dir;
import net.morni.burgverlies.dungeon.Loot;
import net.morni.burgverlies.dungeon.MobKind;
import net.morni.burgverlies.dungeon.Pos;
import net.morni.burgverlies.dungeon.Puzzle;
import net.morni.burgverlies.dungeon.Revives;
import net.morni.burgverlies.dungeon.RoomTemplate;
import net.morni.burgverlies.dungeon.RoomType;
import net.morni.burgverlies.dungeon.Scaling;
import net.morni.burgverlies.dungeon.TrapTiming;
import net.morni.burgverlies.dungeon.Waves;
import net.morni.burgverlies.world.Layout;
import net.morni.burgverlies.world.Plan;
import net.morni.burgverlies.world.RoomPlans;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Was in den einzelnen Raumarten passiert: Wellen, Fallen, Rätsel, Schätze, Rast, Krämerin, Kerkermeister. */
final class RoomLogic {

    private static final int SHOW_EVERY = 15;
    private static final int PLATE_COOLDOWN_MS = 1500;
    private static final int BELL_COOLDOWN = 60;
    private static final int BOSS_INTRO = 60;
    private static final int[] DUST = {0xE04040, 0xF2D032, 0x6CD44A, 0x5DA9E9};
    private static final Pos[] STAIRS = {new Pos(-2, 1, 3), new Pos(2, 1, 6)};

    private final Runs runs;

    RoomLogic(Runs runs) {
        this.runs = runs;
    }

    private World world() {
        return runs.plugin().world();
    }

    private Random rnd(Run run, Room room, int salt) {
        return new Random(run.seed ^ (run.floor * 1000L + room.index() * 37L + salt));
    }

    // ---------- Start ----------

    void start(Run run, Room room) {
        room.ticks = 0;
        switch (room.placed.type()) {
            case FIGHT -> {
                room.phase = Room.Phase.ACTIVE;
                room.waves = Waves.plan(rnd(run, room, 1), run.floor, room.placed.template().id(), run.difficulty,
                        run.size());
                room.wave = -1;
                room.waveDelay = runs.plugin().settings().waveDelayTicks() + 20;
                runs.broadcast(run, "room.fight.start", Placeholder.unparsed("waves", String.valueOf(room.waves.size())));
            }
            case TRAP -> {
                room.phase = Room.Phase.ACTIVE;
                runs.broadcast(run, "room.trap.start");
            }
            case PUZZLE -> {
                room.phase = Room.Phase.ACTIVE;
                RoomTemplate t = room.placed.template();
                room.puzzle = Puzzle.create(rnd(run, room, 2), t.inputs().size(), Puzzle.lengthFor(run.floor));
                room.inputs.clear();
                for (int i = 0; i < t.inputs().size(); i++) {
                    int[] b = runs.block(run, room, t.inputs().get(i));
                    room.inputs.put(Room.key(b[0], b[1], b[2]), i);
                }
                room.showStep = 0;
                runs.broadcast(run, t.plates() ? "room.puzzle.start.plates" : "room.puzzle.start.levers");
            }
            case TREASURE -> {
                runs.broadcast(run, "room.treasure.start",
                        Placeholder.unparsed("chests", String.valueOf(room.placed.template().chests().size())));
                runs.cleared(run, room);
            }
            case REST -> {
                for (Player p : runs.players(run)) {
                    if (run.revives.state(p.getUniqueId()) == Revives.State.UP) {
                        heal(p);
                    }
                }
                runs.downs().restoreAll(run, runs.at(run, room, new Pos(0, 1, 2), Layout.inward(room.placed)));
                for (Player p : runs.players(run)) {
                    heal(p);
                    Ui.title(p, Texts.text(runs.lang(p), "room.rest.title"), Texts.text(runs.lang(p),
                            "room.rest.subtitle"), 2000);
                }
                runs.soundAll(run, "block.campfire.crackle", 1f);
                runs.cleared(run, room);
            }
            case SHOP -> {
                spawnTrader(run, room);
                runs.broadcast(run, "room.shop.start");
                runs.cleared(run, room);
            }
            case BOSS -> {
                room.phase = Room.Phase.ACTIVE;
                room.intro = BOSS_INTRO;
                for (Player p : runs.players(run)) {
                    Ui.title(p, Texts.text(runs.lang(p), "boss.intro.title"), Texts.text(runs.lang(p),
                            "boss.intro.subtitle"), 3000);
                }
                runs.soundAll(run, "entity.elder_guardian.curse", 0.6f);
            }
            case START -> runs.cleared(run, room);
        }
    }

    static void heal(Player p) {
        var max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFireTicks(0);
        p.setFoodLevel(20);
    }

    // ---------- Jeder Tick ----------

    void tick(Run run, Room room) {
        room.ticks++;
        switch (room.placed.type()) {
            case FIGHT -> fightTick(run, room, true);
            case TRAP -> {
                if (room.phase == Room.Phase.ACTIVE) {
                    trapTick(run, room);
                }
            }
            case PUZZLE -> {
                if (room.phase == Room.Phase.ACTIVE) {
                    puzzleTick(run, room);
                    fightTick(run, room, false);
                }
            }
            case SHOP -> shopTick(run, room);
            case BOSS -> bossTick(run, room);
            default -> {
                // nichts
            }
        }
    }

    // ---------- Kampf ----------

    private void fightTick(Run run, Room room, boolean waves) {
        if (room.ticks % 40 == 0) {
            keepInside(run, room);
        }
        if (!waves || room.phase != Room.Phase.ACTIVE) {
            return;
        }
        if (room.waveDelay > 0) {
            room.waveDelay--;
            if (room.waveDelay % 4 == 0) {
                for (Pos s : room.placed.template().spawns()) {
                    Location l = runs.at(run, room, s, null).add(0, 0.1, 0);
                    world().spawnParticle(Particle.SOUL_FIRE_FLAME, l, 3, 0.25, 0.05, 0.25, 0.01);
                    world().spawnParticle(Particle.SMOKE, l, 2, 0.25, 0.2, 0.25, 0.01);
                }
            }
            if (room.waveDelay == 0) {
                spawnWave(run, room);
            }
            return;
        }
        if (room.waveDelay == 0 && room.mobs.isEmpty()) {
            if (room.wave + 1 < room.waves.size()) {
                room.waveDelay = runs.plugin().settings().waveDelayTicks();
                room.waveDelay = Math.max(1, room.waveDelay);
                for (Player p : runs.players(run)) {
                    p.sendActionBar(Texts.text(runs.lang(p), "room.fight.next",
                            Placeholder.unparsed("wave", String.valueOf(room.wave + 2)),
                            Placeholder.unparsed("waves", String.valueOf(room.waves.size()))));
                }
            } else {
                runs.cleared(run, room);
            }
        }
    }

    private void spawnWave(Run run, Room room) {
        room.wave++;
        List<MobKind> kinds = room.waves.get(room.wave);
        List<Pos> spots = room.placed.template().spawns();
        for (int i = 0; i < kinds.size(); i++) {
            Pos s = spots.get((i + room.wave * 2) % spots.size());
            Location l = runs.at(run, room, s, null);
            Mob m = Mobs.spawn(run, kinds.get(i), l);
            room.mobs.add(m.getUniqueId());
            world().spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
            target(run, m);
        }
        runs.soundAll(run, "entity.zombie_villager.converted", 0.7f);
        for (Player p : runs.players(run)) {
            Ui.title(p, Component.empty(), Texts.text(runs.lang(p), "room.fight.wave",
                    Placeholder.unparsed("wave", String.valueOf(room.wave + 1)),
                    Placeholder.unparsed("waves", String.valueOf(room.waves.size()))), 1200);
        }
    }

    /** Gegner gehen gleich auf jemanden los, der steht. */
    private void target(Run run, Mob m) {
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : runs.players(run)) {
            if (run.revives.state(p.getUniqueId()) != Revives.State.UP) {
                continue;
            }
            double d = p.getLocation().distanceSquared(m.getLocation());
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        if (best != null) {
            m.setTarget(best);
        }
    }

    private void keepInside(Run run, Room room) {
        Iterator<UUID> it = room.mobs.iterator();
        while (it.hasNext()) {
            Entity e = runs.plugin().getServer().getEntity(it.next());
            if (e == null || !e.isValid() || e.isDead()) {
                it.remove();
                continue;
            }
            if (!runs.inside(run, room, e.getLocation(), 0.5)) {
                List<Pos> s = room.placed.template().spawns();
                e.teleport(runs.at(run, room, s.isEmpty() ? new Pos(0, 1, 0) : s.get(0), null));
            }
        }
    }

    void mobDied(Run run, Room room, Entity e) {
        room.mobs.remove(e.getUniqueId());
    }

    // ---------- Fallen ----------

    private void trapTick(Run run, Room room) {
        RoomTemplate.Trap trap = room.placed.template().trap();
        long t = room.ticks;
        double mult = Scaling.mobDamage(run.difficulty, run.size());
        for (RoomTemplate.Emitter e : trap.arrows()) {
            Location muzzle = runs.at(run, room, e.at(), null).add(0, 0.5, 0);
            if (TrapTiming.arrowWarns(t, e.phase())) {
                world().spawnParticle(Particle.SMOKE, muzzle, 8, 0.1, 0.15, 0.1, 0.01);
                world().playSound(muzzle, "block.stone_button.click_on", 0.8f, 0.7f);
            }
            if (TrapTiming.arrowFires(t, e.phase())) {
                Dir d = e.shoots().rotate(room.placed.rotation());
                Vector v = new Vector(d.dx(), 0.02, d.dz());
                Arrow a = world().spawnArrow(muzzle, v, 1.5f, 0f);
                a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                a.setDamage(1.6 * mult);
                a.addScoreboardTag("bv_trap");
                Mobs.mark(a, run, null);
                run.entities.add(a.getUniqueId());
                world().playSound(muzzle, "entity.arrow.shoot", 0.8f, 0.9f);
                room.trapFires++;
                runs.plugin().getServer().getScheduler().runTaskLater(runs.plugin(), a::remove, 40L);
            }
        }
        for (RoomTemplate.JetRow row : trap.jets()) {
            TrapTiming.Phase ph = TrapTiming.jet(t, row.group());
            if (ph == TrapTiming.Phase.IDLE) {
                continue;
            }
            int h = room.placed.template().half();
            if (ph == TrapTiming.Phase.WARN && t % 5 == 0) {
                for (int x = -h; x <= h; x += 2) {
                    Location l = runs.at(run, room, new Pos(x, 1, row.z()), null).add(0, 0.05, 0);
                    world().spawnParticle(Particle.SMOKE, l, 2, 0.2, 0.05, 0.2, 0.01);
                    world().spawnParticle(Particle.LAVA, l, 1, 0.2, 0, 0.2, 0);
                }
                if (TrapTiming.jet(t + 5, row.group()) == TrapTiming.Phase.ACTIVE) {
                    world().playSound(runs.at(run, room, new Pos(0, 1, row.z()), null), "block.fire.extinguish", 0.7f,
                            0.6f);
                }
            }
            if (ph == TrapTiming.Phase.ACTIVE) {
                if (TrapTiming.jet(t - 1, row.group()) != TrapTiming.Phase.ACTIVE) {
                    room.trapFires++;
                    world().playSound(runs.at(run, room, new Pos(0, 1, row.z()), null), "item.firecharge.use", 1f,
                            0.8f);
                }
                if (t % 2 == 0) {
                    for (int x = -h; x <= h; x++) {
                        Location l = runs.at(run, room, new Pos(x, 1, row.z()), null);
                        world().spawnParticle(Particle.FLAME, l, 3, 0.15, 0.8, 0.15, 0.01);
                    }
                }
                if (t % 10 == 0) {
                    for (Player p : upIn(run, room)) {
                        Pos lp = runs.local(run, room, p.getLocation());
                        if (lp.z() == row.z() && lp.y() >= 1 && lp.y() <= 3) {
                            p.setFireTicks(30);
                            p.damage(2 * mult);
                        }
                    }
                }
            }
        }
        if (room.crumble != null) {
            crumbleTick(run, room);
        }
        int[] c = runs.center(run, room);
        List<Player> up = upIn(run, room);
        for (Player p : up) {
            if (p.getLocation().getY() < c[1] + 0.5) {
                room.falls++;
                p.teleport(runs.entry(run, room));
                p.setFallDistance(0);
                p.damage(3 * mult);
                p.sendMessage(Texts.msg(runs.lang(p), "room.trap.fell"));
            }
        }
        List<Player> all = upPlayers(run);
        if (!all.isEmpty() && all.stream().allMatch(p -> runs.inside(run, room, p.getLocation(), 0)
                && runs.local(run, room, p.getLocation()).z() >= room.placed.template().goalZ())) {
            runs.cleared(run, room);
        }
    }

    private void crumbleTick(Run run, Room room) {
        for (Player p : upIn(run, room)) {
            Pos lp = runs.local(run, room, p.getLocation());
            if (lp.y() == 1 && room.crumble.crumbles(lp.x(), lp.z())) {
                room.crumbling.putIfAbsent(Room.key(lp.x(), 0, lp.z()), 0);
            }
        }
        Iterator<Map.Entry<Long, Integer>> it = room.crumbling.entrySet().iterator();
        BlockData cracked = Plan.data("cracked_stone_bricks");
        while (it.hasNext()) {
            Map.Entry<Long, Integer> e = it.next();
            int age = e.getValue() + 1;
            e.setValue(age);
            Pos lp = unkey(e.getKey());
            int[] b = runs.block(run, room, new Pos(lp.x(), 0, lp.z()));
            Block block = world().getBlockAt(b[0], b[1], b[2]);
            if (age < TrapTiming.CRUMBLE_WARN) {
                if (age % 3 == 0) {
                    world().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 1.05, 0.5), 6, 0.3, 0.02, 0.3,
                            cracked);
                    world().playSound(block.getLocation(), "block.gravel.hit", 0.6f, 0.7f);
                }
            } else if (age == TrapTiming.CRUMBLE_WARN) {
                block.setType(Material.CAVE_AIR, false);
                world().playSound(block.getLocation(), "block.stone.break", 1f, 0.6f);
                room.trapFires++;
            } else if (age >= TrapTiming.CRUMBLE_WARN + TrapTiming.CRUMBLE_GONE) {
                block.setBlockData(cracked, false);
                it.remove();
            }
        }
    }

    /** Fallen aus: bröckelnden Boden wieder hinlegen. */
    void restoreCrumble(Run run, Room room) {
        BlockData cracked = Plan.data("cracked_stone_bricks");
        for (Long k : room.crumbling.keySet()) {
            Pos lp = unkey(k);
            int[] b = runs.block(run, room, new Pos(lp.x(), 0, lp.z()));
            world().getBlockAt(b[0], b[1], b[2]).setBlockData(cracked, false);
        }
        room.crumbling.clear();
    }

    private static Pos unkey(long k) {
        int x = (int) (k >> 38);
        int z = (int) ((k << 26) >> 38);
        return new Pos(x, 0, z);
    }

    // ---------- Rätsel ----------

    private void puzzleTick(Run run, Room room) {
        if (room.showStep < 0 || room.ticks % SHOW_EVERY != 0) {
            return;
        }
        List<Integer> order = room.puzzle.order();
        if (room.showStep >= order.size()) {
            room.showStep = -1;
            runs.broadcast(run, "room.puzzle.shown");
            return;
        }
        int input = order.get(room.showStep);
        flash(run, room, input, room.showStep);
        room.showStep++;
    }

    private void flash(Run run, Room room, int input, int step) {
        RoomTemplate t = room.placed.template();
        Pos at = t.inputs().get(input);
        Pos marker = t.plates() ? at.add(0, 1, 0) : new Pos(at.x(), at.y() + 1, at.z());
        Location l = runs.at(run, room, marker, null).add(0, 0.5, 0);
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(DUST[input % DUST.length]), 2.2f);
        world().spawnParticle(Particle.DUST, l, 30, 0.35, 0.35, 0.35, 0, dust);
        world().spawnParticle(Particle.END_ROD, l, 6, 0.2, 0.3, 0.2, 0.02);
        world().playSound(l, "block.note_block.bell", 1f, 0.7f + 0.15f * input);
    }

    private void press(Run run, Room room, Player p, int input, Block lever) {
        if (room.showStep >= 0) {
            p.sendActionBar(Texts.text(runs.lang(p), "room.puzzle.wait"));
            return;
        }
        Puzzle.Step step = room.puzzle.press(input);
        switch (step) {
            case PROGRESS -> {
                power(lever, true);
                flash(run, room, input, room.puzzle.done());
                runs.soundAll(run, "block.note_block.pling", 0.8f + 0.1f * room.puzzle.done());
            }
            case WRONG -> {
                resetLevers(run, room);
                runs.broadcast(run, "room.puzzle.wrong", Placeholder.unparsed("name", p.getName()));
                runs.soundAll(run, "entity.villager.no", 0.6f);
                List<Pos> spots = room.placed.template().spawns();
                for (int i = 0; i < 2; i++) {
                    Location l = runs.at(run, room, spots.get(i % spots.size()), null);
                    Mob m = Mobs.spawn(run, MobKind.KELLERSPINNE, l);
                    room.mobs.add(m.getUniqueId());
                    target(run, m);
                }
            }
            case SOLVED -> {
                power(lever, true);
                runs.broadcast(run, "room.puzzle.solved", Placeholder.unparsed("name", p.getName()));
                runs.cleared(run, room);
            }
            default -> {
                // schon gelöst
            }
        }
    }

    private void resetLevers(Run run, Room room) {
        if (room.placed.template().plates()) {
            return;
        }
        for (Pos in : room.placed.template().inputs()) {
            int[] b = runs.block(run, room, in);
            power(world().getBlockAt(b[0], b[1], b[2]), false);
        }
    }

    private static void power(Block b, boolean on) {
        if (b != null && b.getBlockData() instanceof Powerable pw && b.getType() == Material.LEVER) {
            pw.setPowered(on);
            b.setBlockData(pw, false);
        }
    }

    // ---------- Klicks ----------

    /** Klick auf einen Block im Verlies; {@code true} = erledigt (Ereignis abbrechen). */
    boolean interact(Run run, Player p, Block b, Action action) {
        if (run.revives.state(p.getUniqueId()) != Revives.State.UP) {
            return true;
        }
        long key = Room.key(b.getX(), b.getY(), b.getZ());
        for (Room room : run.floorRooms()) {
            if (!runs.inside(run, room, b.getLocation(), 1.5)) {
                continue;
            }
            switch (room.placed.type()) {
                case PUZZLE -> {
                    if (room.phase != Room.Phase.ACTIVE) {
                        return b.getType() == Material.LEVER || b.getType() == Material.BELL;
                    }
                    Integer input = room.inputs.get(key);
                    if (input != null) {
                        if (room.placed.template().plates()) {
                            if (action != Action.PHYSICAL) {
                                return true;
                            }
                            long now = System.currentTimeMillis();
                            Long last = room.plateCooldown.get(p.getUniqueId());
                            if (last != null && now - last < PLATE_COOLDOWN_MS) {
                                return false;
                            }
                            room.plateCooldown.put(p.getUniqueId(), now);
                            press(run, room, p, input, null);
                            return false;
                        }
                        if (action == Action.RIGHT_CLICK_BLOCK) {
                            press(run, room, p, input, b);
                        }
                        return true;
                    }
                    if (b.getType() == Material.BELL) {
                        if (room.ticks >= room.bellReady && room.showStep < 0) {
                            room.bellReady = room.ticks + BELL_COOLDOWN;
                            room.showStep = 0;
                            world().playSound(b.getLocation(), "block.bell.use", 1f, 1f);
                        }
                        return true;
                    }
                }
                case TREASURE -> {
                    if (b.getType() == Material.CHEST) {
                        openChest(run, room, p, b);
                        return true;
                    }
                }
                default -> {
                    // nichts zu klicken
                }
            }
        }
        return false;
    }

    private void openChest(Run run, Room room, Player p, Block b) {
        long key = Room.key(b.getX(), b.getY(), b.getZ());
        if (!room.opened.add(key)) {
            p.sendActionBar(Texts.text(runs.lang(p), "room.treasure.empty"));
            return;
        }
        BlockState st = b.getState();
        if (st instanceof Lidded lid) {
            lid.open();
        }
        Random r = rnd(run, room, 100 + room.opened.size());
        int taler = Loot.chest(r, run.floor, run.difficulty);
        for (Hero h : run.active()) {
            h.earn(taler);
        }
        Loot.Item item = Loot.chestItem(r, run.floor);
        ItemStack stack = Gear.loot(item, runs.lang(p));
        if (stack != null) {
            p.getInventory().addItem(stack);
        }
        world().spawnParticle(Particle.WAX_OFF, b.getLocation().add(0.5, 1, 0.5), 25, 0.4, 0.4, 0.4, 0.5);
        world().playSound(b.getLocation(), "entity.player.levelup", 0.8f, 1.6f);
        runs.broadcast(run, "room.treasure.opened", Placeholder.unparsed("name", p.getName()),
                Placeholder.unparsed("taler", String.valueOf(taler)),
                Placeholder.component("item", item == Loot.Item.NONE ? Component.empty()
                        : Texts.text(run.lang, "loot." + item.name().toLowerCase(java.util.Locale.ROOT))));
        runs.sidebars(run);
    }

    // ---------- Krämerin ----------

    private void spawnTrader(Run run, Room room) {
        if (room.npc != null) {
            return;
        }
        Pos at = room.placed.template().npc();
        Location l = runs.at(run, room, at, Dir.EAST.rotate(room.placed.rotation()));
        Villager v = world().spawn(l, Villager.class, vl -> {
            Mobs.mark(vl, run, null);
            vl.addScoreboardTag("bv_trader");
            vl.setAI(false);
            vl.setInvulnerable(true);
            vl.setSilent(true);
            vl.setCollidable(false);
            vl.setProfession(Villager.Profession.CLERIC);
            vl.setVillagerType(Villager.Type.TAIGA);
            vl.customName(Texts.text(run.lang, "trader.name"));
            vl.setCustomNameVisible(true);
        });
        room.npc = v.getUniqueId();
        run.entities.add(v.getUniqueId());
    }

    private void shopTick(Run run, Room room) {
        if (room.ticks % 5 != 0) {
            return;
        }
        List<Player> up = upPlayers(run);
        boolean all = !up.isEmpty() && up.stream().allMatch(p -> onStairs(run, room, p));
        if (!all) {
            room.stairsTicks = 0;
            return;
        }
        if (run.floor + 1 >= run.plans.length) {
            return;
        }
        if (!run.built[run.floor + 1]) {
            for (Player p : up) {
                p.sendActionBar(Texts.text(runs.lang(p), "room.shop.building"));
            }
            return;
        }
        room.stairsTicks += 5;
        int need = runs.plugin().settings().stairsSeconds() * 20;
        for (Player p : up) {
            p.sendActionBar(Texts.text(runs.lang(p), "room.shop.descending",
                    Placeholder.unparsed("bar", Ui.bar((double) room.stairsTicks / need, 10))));
        }
        if (room.stairsTicks >= need) {
            runs.nextFloor(run);
        }
    }

    boolean onStairs(Run run, Room room, Player p) {
        if (!runs.inside(run, room, p.getLocation(), 0)) {
            return false;
        }
        Pos lp = runs.local(run, room, p.getLocation());
        return lp.x() >= STAIRS[0].x() && lp.x() <= STAIRS[1].x() && lp.z() >= STAIRS[0].z()
                && lp.z() <= STAIRS[1].z();
    }

    // ---------- Kerkermeister ----------

    private void bossTick(Run run, Room room) {
        if (room.phase != Room.Phase.ACTIVE) {
            return;
        }
        if (room.intro > 0) {
            room.intro--;
            if (room.intro == 0) {
                room.boss = new Boss(runs, run, room);
                room.boss.spawn(runs.at(run, room, room.placed.template().npc(), Dir.NORTH.rotate(room.placed.rotation())));
            }
            return;
        }
        if (room.boss != null) {
            room.boss.tick();
            if (room.ticks % 40 == 0) {
                keepInside(run, room);
            }
        }
    }

    // ---------- Hilfen ----------

    List<Player> upPlayers(Run run) {
        List<Player> out = new ArrayList<>();
        for (Player p : runs.players(run)) {
            if (run.revives.state(p.getUniqueId()) == Revives.State.UP) {
                out.add(p);
            }
        }
        return out;
    }

    private List<Player> upIn(Run run, Room room) {
        List<Player> out = new ArrayList<>();
        for (Player p : upPlayers(run)) {
            if (runs.inside(run, room, p.getLocation(), 0)) {
                out.add(p);
            }
        }
        return out;
    }

    /** Farben der Rätsel-Eingaben (für die Prüfung). */
    static String colour(int i) {
        return RoomPlans.PUZZLE_COLOURS.get(i % RoomPlans.PUZZLE_COLOURS.size());
    }
}
