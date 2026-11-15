package de.moinmorni.burgverlies;

import de.moinmorni.burgverlies.data.Leaderboard;
import de.moinmorni.burgverlies.data.PlayerData;
import de.moinmorni.burgverlies.data.Store;
import de.moinmorni.burgverlies.dungeon.Difficulty;
import de.moinmorni.burgverlies.dungeon.RoomCatalog;
import de.moinmorni.burgverlies.dungeon.RoomTemplate;
import de.moinmorni.burgverlies.dungeon.Scaling;
import de.moinmorni.burgverlies.dungeon.TrapLayout;
import de.moinmorni.burgverlies.hall.Boards;
import de.moinmorni.burgverlies.hall.Hall;
import de.moinmorni.burgverlies.hall.Queue;
import de.moinmorni.burgverlies.listen.CombatListener;
import de.moinmorni.burgverlies.listen.GuardListener;
import de.moinmorni.burgverlies.listen.PlayerListener;
import de.moinmorni.burgverlies.menu.Menus;
import de.moinmorni.burgverlies.net.Network;
import de.moinmorni.burgverlies.run.Mobs;
import de.moinmorni.burgverlies.run.Runs;
import de.moinmorni.burgverlies.run.Sidebar;
import de.moinmorni.burgverlies.world.Builder;
import de.moinmorni.burgverlies.world.HallPlan;
import de.moinmorni.burgverlies.world.Plan;
import de.moinmorni.burgverlies.world.RoomPlans;
import de.moinmorni.burgverlies.world.VoidGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Morni Burgverlies: Koop-Verlies für 1–4 Spieler. Torhaus mit Suche, drei Etagen aus handgebauten Räumen (Kampf,
 * Fallen, Rätsel, Schatz, Rast, Krämerin) und ganz unten der Kerkermeister. Spricht mit dem MoinMorni-Netzwerk,
 * wenn es da ist, und läuft sonst allein.
 */
public final class BurgverliesPlugin extends JavaPlugin {

    /** Bau-Version des Torhauses: erhöhen, wenn es sich ändert – dann wird es beim nächsten Start neu gebaut. */
    public static final int HALL_VERSION = 1;
    private static final long SECOND = 20L;
    private static final long LANGUAGE_CHECK = 40L;
    private static final long STATE_REFRESH = 20L * 60;

    private Settings settings;
    private Network net;
    private Store store;
    private Leaderboard board;
    private World world;
    private Builder builder;
    private Hall hall;
    private Queue queue;
    private Runs runs;
    private Menus menus;
    private Boards boards;
    private Sidebar sidebar;
    private boolean stateQueued;
    private String lastState = "";
    private int lastPlayers = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        Items.init(this);
        Mobs.init(this);
        net = Network.connect(this);
        getLogger().info(net.connected()
                ? "Mit dem MoinMorni-Netzwerk verbunden (Server „" + net.serverName() + "“)."
                : "Kein Netzwerk-Plugin: Burgverlies läuft allein (deutsche Texte, Statistik nur lokal).");
        store = new Store(getLogger(), getDataFolder().toPath());
        board = new Leaderboard(getLogger(), getDataFolder().toPath());
        board.load();
        world = createWorld(settings.world());
        builder = new Builder(this);
        sidebar = new Sidebar();
        hall = new Hall(this);
        queue = new Queue(this);
        runs = new Runs(this);
        menus = new Menus(this);
        boards = new Boards(this);
        checkPlans();

        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GuardListener(this), this);
        Commands commands = new Commands(this);
        for (String name : List.of("burgverlies", "verlassen", "bvadmin")) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(commands);
                c.setTabCompleter(commands);
            }
        }

        // Das Torhaus bleibt immer geladen – sonst verschwänden die Tafeln, wenn niemand da ist.
        for (int cx = -2; cx <= 1; cx++) {
            for (int cz = -2; cz <= 1; cz++) {
                world.setChunkForceLoaded(cx, cz, true);
            }
        }
        buildHall(false);
        boards.spawn();

        var scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, runs::tick, 1L, 1L);
        scheduler.runTaskTimer(this, () -> {
            queue.tick();
            menus.refreshOpen();
        }, SECOND, SECOND);
        scheduler.runTaskTimer(this, () -> getServer().getOnlinePlayers().forEach(p -> {
            hall.checkLanguage(p);
            if (hall.inHall(p)) {
                hall.sidebar(p);
            }
        }), LANGUAGE_CHECK, LANGUAGE_CHECK);
        scheduler.runTaskTimer(this, () -> reportState(true), STATE_REFRESH, STATE_REFRESH);

        for (Player p : getServer().getOnlinePlayers()) {
            data(p);
            hall.enter(p);
        }
        stateChanged();
        getLogger().info("Burgverlies ist bereit: " + RoomCatalog.all().size() + " Raumvorlagen, bis zu "
                + settings.maxRuns() + " Abstiege gleichzeitig.");
    }

    @Override
    public void onDisable() {
        if (runs != null) {
            runs.abortAll();
        }
        if (boards != null) {
            boards.remove();
        }
        if (store != null) {
            if (board != null) {
                store.write(board.file(), board.toYaml());
            }
            store.close();
        }
    }

    private World createWorld(String name) {
        World w = getServer().getWorld(name);
        if (w == null) {
            w = new WorldCreator(name).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                    .generateStructures(false).createWorld();
        }
        if (w == null) {
            throw new IllegalStateException("Welt " + name + " ließ sich nicht anlegen");
        }
        w.setGameRule(GameRules.ADVANCE_TIME, false);
        w.setGameRule(GameRules.ADVANCE_WEATHER, false);
        w.setGameRule(GameRules.SPAWN_MOBS, false);
        w.setGameRule(GameRules.SPAWN_MONSTERS, false);
        w.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        w.setGameRule(GameRules.SPAWN_PATROLS, false);
        w.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
        w.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        w.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRules.KEEP_INVENTORY, true);
        w.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRules.MOB_GRIEFING, false);
        w.setGameRule(GameRules.BLOCK_DROPS, false);
        w.setGameRule(GameRules.ENTITY_DROPS, false);
        w.setGameRule(GameRules.MOB_DROPS, false);
        w.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        w.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        w.setGameRule(GameRules.PVP, false);
        w.setTime(13000L);
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(org.bukkit.Difficulty.NORMAL);
        w.setSpawnLocation(HallPlan.spawn(w));
        return w;
    }

    /** Alle Baupläne einmal erzeugen: ein falscher Blockname fällt so schon beim Start auf. */
    private void checkPlans() {
        HallPlan.build();
        for (RoomTemplate t : RoomCatalog.all()) {
            for (int f = 0; f < RoomPlans.PALETTES.length; f++) {
                TrapLayout crumble = t.trap() != null && t.trap().crumbles()
                        ? TrapLayout.create(new Random(1), t.half(), t.trap().crumbleMin(), t.trap().crumbleMax(), 0.6)
                        : null;
                RoomPlans.build(t, f, 1, crumble);
            }
        }
    }

    /** Torhaus bauen, wenn es fehlt oder sich geändert hat (Bau-Version in {@code bau.yml}). */
    void buildHall(boolean force) {
        Path state = getDataFolder().toPath().resolve("bau.yml");
        String want = "torhaus: " + HALL_VERSION + "\n";
        try {
            if (!force && Files.isRegularFile(state) && want.equals(Files.readString(state, StandardCharsets.UTF_8))) {
                return;
            }
        } catch (IOException e) {
            getLogger().warning("bau.yml: " + e.getMessage());
        }
        Plan plan = HallPlan.build();
        List<Plan.Banner> banners = new ArrayList<>();
        Builder.now(world, builder.transform(plan, 0, HallPlan.FLOOR_Y, 0, 0, banners));
        store.write(state, want);
        getLogger().info("Torhaus gebaut (" + plan.size() + " Blöcke).");
    }

    // ---------- Starten ----------

    /** Gruppe (Party oder allein) bestimmen und dann {@code then} ausführen – auf dem Hauptthread. */
    private void withGroup(Player p, java.util.function.Consumer<List<Player>> then) {
        UUID me = p.getUniqueId();
        net.party(me).whenComplete((party, err) -> getServer().getScheduler().runTask(this, () -> {
            if (!p.isOnline() || !hall.inHall(p)) {
                return;
            }
            Optional<Network.Party> opt = err != null || party == null ? Optional.empty() : party;
            List<Player> group = new ArrayList<>();
            group.add(p);
            if (opt.isPresent()) {
                Network.Party pa = opt.get();
                if (!pa.leader().equals(me)) {
                    p.sendMessage(Texts.msg(lang(p), "party.not-leader"));
                    return;
                }
                int missing = 0;
                for (Network.Party.Member m : pa.members()) {
                    if (m.uuid().equals(me)) {
                        continue;
                    }
                    Player other = getServer().getPlayer(m.uuid());
                    if (other != null && hall.inHall(other)) {
                        group.add(other);
                    } else {
                        missing++;
                    }
                }
                if (group.size() > Scaling.MAX_PLAYERS) {
                    p.sendMessage(Texts.msg(lang(p), "party.too-big", Placeholder.unparsed("max",
                            String.valueOf(Scaling.MAX_PLAYERS))));
                    return;
                }
                if (missing > 0) {
                    p.sendMessage(Texts.msg(lang(p), "party.missing", Placeholder.unparsed("n",
                            String.valueOf(missing))));
                }
            }
            then.accept(group);
        }));
    }

    /** Gleich mit der eigenen Gruppe hinab. */
    public void startNow(Player p, Difficulty d) {
        withGroup(p, group -> {
            for (Player g : group) {
                queue.remove(g.getUniqueId(), false);
            }
            if (!runs.start(group, d)) {
                p.sendMessage(Texts.msg(lang(p), "run.full"));
                queue.join(group, d);
            }
        });
    }

    /** Mitspieler suchen (bis 4). */
    public void search(Player p, Difficulty d) {
        withGroup(p, group -> queue.join(group, d));
    }

    // ---------- Zugriff für die Bausteine ----------

    public Settings settings() {
        return settings;
    }

    public Network net() {
        return net;
    }

    public Store store() {
        return store;
    }

    public Leaderboard board() {
        return board;
    }

    public World world() {
        return world;
    }

    public Builder builder() {
        return builder;
    }

    public Hall hall() {
        return hall;
    }

    public Queue queue() {
        return queue;
    }

    public Runs runs() {
        return runs;
    }

    public Menus menus() {
        return menus;
    }

    public Boards boards() {
        return boards;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    public String lang(Player p) {
        return net.language(p.getUniqueId());
    }

    public PlayerData data(Player p) {
        return store.get(p.getUniqueId(), p.getName());
    }

    /** Zustand fürs Netzwerk (Lobby-Figur, Webseite) – gesammelt, höchstens einmal pro Tick. */
    public void stateChanged() {
        if (stateQueued) {
            return;
        }
        stateQueued = true;
        getServer().getScheduler().runTask(this, () -> {
            stateQueued = false;
            reportState(false);
        });
    }

    private void reportState(boolean force) {
        String state = runs.running() > 0 ? "running" : "waiting";
        int players = getServer().getOnlinePlayers().size();
        if (force || !state.equals(lastState) || players != lastPlayers) {
            lastState = state;
            lastPlayers = players;
            net.state(state, players);
        }
    }

    /** /bvadmin neuladen */
    void reloadSettings() {
        reloadConfig();
        settings = Settings.load(getConfig());
    }
}
