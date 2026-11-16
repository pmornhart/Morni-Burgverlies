package net.morni.burgverlies.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Setzt Blöcke Stück für Stück (höchstens {@link #BUDGET_NANOS} pro Tick), damit der Server beim Bauen flüssig
 * bleibt. Gebaut wird ohne Physik (Gitter, Fackeln und Hebel bleiben, wie der Plan sie will).
 */
public final class Builder {

    private static final long BUDGET_NANOS = 15_000_000L;
    private static final int CHECK_EVERY = 128;

    /** Ein Block an einer Weltstelle. */
    public record Put(int x, int y, int z, BlockData data) {
    }

    /** Ein Bauauftrag. */
    private static final class Job {
        final World world;
        final List<Put> puts;
        final List<Plan.Banner> banners;
        final Runnable done;
        int index;

        Job(World world, List<Put> puts, List<Plan.Banner> banners, Runnable done) {
            this.world = world;
            this.puts = puts;
            this.banners = banners;
            this.done = done;
        }
    }

    private final Plugin plugin;
    private final Deque<Job> jobs = new ArrayDeque<>();
    private final Map<String, BlockData> rotatedCache = new HashMap<>();
    private BukkitTask task;
    private long placed;

    public Builder(Plugin plugin) {
        this.plugin = plugin;
    }

    public long placed() {
        return placed;
    }

    public boolean busy() {
        return !jobs.isEmpty();
    }

    /** Auftrag anstellen; {@code done} läuft auf dem Hauptthread, wenn alles steht. */
    public void place(World world, List<Put> puts, List<Plan.Banner> banners, Runnable done) {
        jobs.add(new Job(world, puts, banners, done));
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    /** Sofort setzen (kleine Mengen, z. B. Fallgitter). */
    public static void now(World world, List<Put> puts) {
        for (Put p : puts) {
            set(world.getBlockAt(p.x(), p.y(), p.z()), p.data());
        }
    }

    /**
     * Einen Block setzen. Steht dort noch ein Block mit eigenem Inhalt aus einem früheren Abstieg (z. B. ein Lesepult),
     * kann Minecraft beim Ersetzen ohne Physik stolpern – dann erst mit Physik leeren und noch einmal setzen.
     */
    static void set(Block b, BlockData data) {
        try {
            b.setBlockData(data, false);
        } catch (RuntimeException e) {
            try {
                b.setType(org.bukkit.Material.AIR, true);
                b.setBlockData(data, false);
            } catch (RuntimeException again) {
                // Einzelner Block bleibt, wie er ist – der Rest wird trotzdem gebaut.
                failed++;
            }
        }
    }

    private static int failed;

    /** Blöcke, die sich nicht setzen ließen (für /bvadmin info). */
    public static int failed() {
        return failed;
    }

    private void tick() {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        while (!jobs.isEmpty() && System.nanoTime() < deadline) {
            Job job = jobs.peekFirst();
            boolean finished;
            try {
                finished = step(job, deadline);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Bauschritt fehlgeschlagen – wird übersprungen", e);
                finished = true;
            }
            if (finished) {
                jobs.pollFirst();
                try {
                    job.done.run();
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.WARNING, "Nach dem Bauen ging etwas schief", e);
                }
            }
        }
        if (jobs.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    private boolean step(Job job, long deadline) {
        int n = 0;
        while (job.index < job.puts.size()) {
            Put p = job.puts.get(job.index++);
            set(job.world.getBlockAt(p.x(), p.y(), p.z()), p.data());
            placed++;
            if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                return false;
            }
        }
        for (Plan.Banner b : job.banners) {
            Block block = job.world.getBlockAt(b.x(), b.y(), b.z());
            BlockState st = block.getState();
            if (st instanceof org.bukkit.block.Banner banner) {
                banner.setPatterns(b.patterns());
                banner.update(true, false);
            }
        }
        return true;
    }

    /** Plan (im Vorlagen-Rahmen) gedreht an eine Weltstelle legen. */
    public List<Put> transform(Plan plan, int ox, int oy, int oz, int quarters, List<Plan.Banner> bannersOut) {
        List<Put> out = new ArrayList<>(plan.size());
        int q = Math.floorMod(quarters, 4);
        plan.forEach((k, d) -> {
            int[] w = rotate(Plan.keyX(k), Plan.keyZ(k), q);
            out.add(new Put(ox + w[0], oy + Plan.keyY(k), oz + w[1], rotated(d, q)));
        });
        if (bannersOut != null) {
            for (Plan.Banner b : plan.banners()) {
                int[] w = rotate(b.x(), b.z(), q);
                bannersOut.add(new Plan.Banner(ox + w[0], oy + b.y(), oz + w[1], b.patterns()));
            }
        }
        return out;
    }

    /** Rüstungsständer eines Plans an ihre Weltstelle (Blickrichtung mitgedreht). */
    public List<Plan.Stand> transformStands(Plan plan, int ox, int oy, int oz, int quarters) {
        List<Plan.Stand> out = new ArrayList<>();
        int q = Math.floorMod(quarters, 4);
        for (Plan.Stand s : plan.stands()) {
            int[] w = rotate(s.x(), s.z(), q);
            out.add(new Plan.Stand(ox + w[0], oy + s.y(), oz + w[1], s.facing().rotate(q), s.kit()));
        }
        return out;
    }

    static int[] rotate(int x, int z, int q) {
        int nx = x;
        int nz = z;
        for (int i = 0; i < q; i++) {
            int t = nx;
            nx = -nz;
            nz = t;
        }
        return new int[] {nx, nz};
    }

    private BlockData rotated(BlockData d, int q) {
        if (q == 0) {
            return d;
        }
        String key = q + d.getAsString();
        BlockData cached = rotatedCache.get(key);
        if (cached != null) {
            return cached;
        }
        BlockData r = d.clone();
        r.rotate(switch (q) {
            case 1 -> StructureRotation.CLOCKWISE_90;
            case 2 -> StructureRotation.CLOCKWISE_180;
            default -> StructureRotation.COUNTERCLOCKWISE_90;
        });
        rotatedCache.put(key, r);
        return r;
    }
}
