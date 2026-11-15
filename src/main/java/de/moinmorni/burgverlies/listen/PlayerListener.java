package de.moinmorni.burgverlies.listen;

import de.moinmorni.burgverlies.BurgverliesPlugin;
import de.moinmorni.burgverlies.Items;
import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.hall.Hall;
import de.moinmorni.burgverlies.world.HallPlan;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Betreten, Verlassen, Gegenstände im Torhaus, Klicks im Verlies. */
public final class PlayerListener implements Listener {

    private static final long MENU_COOLDOWN_MS = 4000;

    private final BurgverliesPlugin plugin;
    private final Map<UUID, Long> descentMenu = new HashMap<>();

    public PlayerListener(BurgverliesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(null);
        plugin.data(p);
        plugin.hall().enter(p);
        String lang = plugin.lang(p);
        de.moinmorni.burgverlies.Ui.title(p, Texts.text(lang, "join.title"), Texts.text(lang, "join.subtitle"), 2500);
        p.sendMessage(Texts.msg(lang, "join.welcome"));
        plugin.stateChanged();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(null);
        plugin.queue().remove(p.getUniqueId(), true);
        plugin.runs().leave(p, true);
        plugin.store().unload(p.getUniqueId());
        plugin.sidebar().remove(p.getUniqueId());
        plugin.hall().forget(p.getUniqueId());
        descentMenu.remove(p.getUniqueId());
        plugin.stateChanged();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() == Action.PHYSICAL || e.getHand() == EquipmentSlot.HAND || e.getHand() == null) {
            if (e.getClickedBlock() != null && plugin.runs().interact(p, e.getClickedBlock(), e.getAction())) {
                e.setCancelled(true);
                return;
            }
        }
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() == Action.PHYSICAL) {
            return;
        }
        String action = Items.hallTag(e.getItem());
        if (action == null || !plugin.hall().inHall(p)) {
            return;
        }
        e.setCancelled(true);
        switch (action) {
            case Hall.START -> plugin.menus().start(p);
            case Hall.LEAVE_QUEUE -> plugin.queue().remove(p.getUniqueId(), true);
            case Hall.STATS -> plugin.menus().stats(p, plugin.data(p));
            case Hall.TRAILS -> plugin.menus().trails(p);
            case Hall.LOBBY -> {
                p.sendMessage(Texts.msg(plugin.lang(p), "lobby.going"));
                plugin.net().toLobby(p, Texts.text(plugin.lang(p), "lobby.kick"));
            }
            default -> {
                // unbekannt
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            if (e.getRightClicked().getScoreboardTags().contains("bv_trader")) {
                e.setCancelled(true);
            }
            return;
        }
        if (plugin.runs().interactEntity(e.getPlayer(), e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo().getBlockX() == e.getFrom().getBlockX() && e.getTo().getBlockY() == e.getFrom().getBlockY()
                && e.getTo().getBlockZ() == e.getFrom().getBlockZ()) {
            return;
        }
        Player p = e.getPlayer();
        if (!plugin.hall().inHall(p)) {
            return;
        }
        if (e.getTo().getY() < HallPlan.FLOOR_Y - 30) {
            p.teleport(plugin.hall().spawn());
            p.setFallDistance(0);
            return;
        }
        if (Hall.atDescent(e.getTo()) && !plugin.queue().contains(p.getUniqueId())) {
            long now = System.currentTimeMillis();
            Long last = descentMenu.get(p.getUniqueId());
            if (last == null || now - last > MENU_COOLDOWN_MS) {
                descentMenu.put(p.getUniqueId(), now);
                plugin.menus().start(p);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (plugin.runs().of(p).isEmpty()) {
            e.setRespawnLocation(plugin.hall().spawn());
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.hall().enter(p));
        }
    }
}
