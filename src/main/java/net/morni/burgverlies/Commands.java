package net.morni.burgverlies;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.burgverlies.data.PlayerData;
import net.morni.burgverlies.dungeon.Difficulty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * /burgverlies (start, suchen, verlassen, stats, top, spuren), /verlassen und /bvadmin (info, neubauen, neuladen,
 * debug, loesen, taler). Einstellungen gibt es nur in der config.yml – im Netzwerk-Panel stellt man sie ein.
 */
final class Commands implements TabExecutor {

    private static final List<String> SUBS = List.of("start", "suchen", "verlassen", "stats", "top", "spuren");
    private static final List<String> ADMIN = List.of("info", "neubauen", "neuladen", "debug", "loesen", "taler", "start");

    private final BurgverliesPlugin plugin;

    Commands(BurgverliesPlugin plugin) {
        this.plugin = plugin;
    }

    private String lang(CommandSender s) {
        return s instanceof Player p ? plugin.lang(p) : "de";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName()) {
            case "verlassen" -> leave(sender);
            case "bvadmin" -> admin(sender, args);
            default -> main(sender, args);
        };
    }

    private boolean main(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("stats") || sub.equals("statistik")) {
            return stats(sender, args.length > 1 ? args[1] : null);
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.msg("de", "only-players"));
            return true;
        }
        String lang = plugin.lang(p);
        switch (sub) {
            case "" -> {
                if (plugin.hall().inHall(p)) {
                    plugin.menus().start(p);
                } else {
                    p.sendMessage(Texts.msg(lang, "help"));
                }
            }
            case "start", "los" -> {
                if (!plugin.hall().inHall(p)) {
                    p.sendMessage(Texts.msg(lang, "run.already"));
                    return true;
                }
                Difficulty d = difficulty(p, args);
                plugin.data(p).chosen(d);
                plugin.startNow(p, d);
            }
            case "suchen", "search" -> {
                if (!plugin.hall().inHall(p)) {
                    p.sendMessage(Texts.msg(lang, "run.already"));
                    return true;
                }
                Difficulty d = difficulty(p, args);
                plugin.data(p).chosen(d);
                plugin.search(p, d);
            }
            case "verlassen", "leave" -> leave(p);
            case "top", "bestenliste" -> plugin.menus().stats(p, plugin.data(p));
            case "spuren", "trails" -> plugin.menus().trails(p);
            default -> p.sendMessage(Texts.msg(lang, "help"));
        }
        return true;
    }

    private Difficulty difficulty(Player p, String[] args) {
        if (args.length > 1) {
            Optional<Difficulty> d = Difficulty.parse(args[1]);
            if (d.isPresent()) {
                return d.get();
            }
        }
        return plugin.data(p).chosen();
    }

    private boolean leave(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.msg("de", "only-players"));
            return true;
        }
        if (plugin.queue().contains(p.getUniqueId())) {
            plugin.queue().remove(p.getUniqueId(), true);
            return true;
        }
        if (plugin.runs().of(p).isPresent()) {
            plugin.runs().leave(p, false);
            p.sendMessage(Texts.msg(plugin.lang(p), "run.left.self"));
            return true;
        }
        p.sendMessage(Texts.msg(plugin.lang(p), "nothing-to-leave"));
        return true;
    }

    private boolean stats(CommandSender sender, String name) {
        String lang = lang(sender);
        PlayerData d;
        if (name == null) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Texts.msg(lang, "only-players"));
                return true;
            }
            d = plugin.data(p);
        } else {
            Player online = plugin.getServer().getPlayerExact(name);
            UUID u = online != null ? online.getUniqueId() : plugin.getServer().getOfflinePlayer(name).getUniqueId();
            d = plugin.store().read(u).orElse(null);
            if (d == null) {
                sender.sendMessage(Texts.msg(lang, "stats.unknown", Placeholder.unparsed("name", name)));
                return true;
            }
        }
        if (sender instanceof Player p) {
            plugin.menus().stats(p, d);
        } else {
            sender.sendMessage(Component.text(d.name() + ": " + d.runs() + " runs, " + d.wins() + " wins, "
                    + d.bosses() + " bosses, " + d.rooms() + " rooms"));
        }
        return true;
    }

    private boolean admin(CommandSender sender, String[] args) {
        String lang = lang(sender);
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "info" -> sender.sendMessage(Texts.msg(lang, "admin.info",
                    Placeholder.unparsed("runs", String.valueOf(plugin.runs().running())),
                    Placeholder.unparsed("max", String.valueOf(plugin.settings().maxRuns())),
                    Placeholder.unparsed("inside", String.valueOf(plugin.runs().playersInside())),
                    Placeholder.unparsed("queued", String.valueOf(plugin.queue().waiting())),
                    Placeholder.unparsed("net", plugin.net().connected() ? "✔" : "✖")));
            case "neubauen" -> {
                plugin.buildHall(true);
                plugin.boards().spawn();
                sender.sendMessage(Texts.msg(lang, "admin.rebuilt"));
            }
            case "neuladen" -> {
                plugin.reloadSettings();
                sender.sendMessage(Texts.msg(lang, "admin.reloaded"));
            }
            case "start" -> {
                Player target = args.length > 1 ? plugin.getServer().getPlayerExact(args[1]) : null;
                if (target == null || !plugin.hall().inHall(target)) {
                    sender.sendMessage(Texts.msg(lang, "admin.no-player"));
                    return true;
                }
                Difficulty d = args.length > 2 ? Difficulty.parse(args[2]).orElse(Difficulty.NORMAL)
                        : plugin.data(target).chosen();
                plugin.startNow(target, d);
                sender.sendMessage(Texts.msg(lang, "admin.started", Placeholder.unparsed("name", target.getName())));
            }
            case "debug", "loesen", "taler" -> {
                Player target = args.length > 1 ? plugin.getServer().getPlayerExact(args[1])
                        : sender instanceof Player p ? p : null;
                if (target == null) {
                    sender.sendMessage(Texts.msg(lang, "admin.no-player"));
                    return true;
                }
                if (sub.equals("debug")) {
                    sender.sendMessage(Component.text(plugin.runs().debug(target)));
                } else if (sub.equals("loesen")) {
                    sender.sendMessage(Texts.msg(lang, plugin.runs().solve(target) ? "admin.solved" : "admin.not-solved"));
                } else {
                    int n;
                    try {
                        n = args.length > 2 ? Integer.parseInt(args[2]) : 100;
                    } catch (NumberFormatException e) {
                        n = 100;
                    }
                    sender.sendMessage(Texts.msg(lang, plugin.runs().giveTaler(target, n) ? "admin.taler"
                            : "admin.no-run", Placeholder.unparsed("n", String.valueOf(n))));
                }
            }
            default -> sender.sendMessage(Texts.msg(lang, "admin.usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (command.getName().equals("bvadmin")) {
            if (args.length == 1) {
                options.addAll(ADMIN);
            } else if (args.length == 2) {
                plugin.getServer().getOnlinePlayers().forEach(p -> options.add(p.getName()));
            }
        } else if (command.getName().equals("burgverlies")) {
            if (args.length == 1) {
                options.addAll(SUBS);
            } else if (args.length == 2 && List.of("start", "suchen").contains(args[0].toLowerCase(Locale.ROOT))) {
                for (Difficulty d : Difficulty.values()) {
                    options.add(d.id());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
                plugin.getServer().getOnlinePlayers().forEach(p -> options.add(p.getName()));
            }
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }
}
