package de.moinmorni.burgverlies.listen;

import de.moinmorni.burgverlies.BurgverliesPlugin;
import de.moinmorni.burgverlies.run.Mobs;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/** Schutz: niemand baut etwas kaputt, kein Hunger, nichts fällt herunter, alte Gegner verschwinden. */
public final class GuardListener implements Listener {

    private static final String ADMIN = "moinmorni.burgverlies.admin";

    private final BurgverliesPlugin plugin;

    public GuardListener(BurgverliesPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean ours(Block b) {
        return b.getWorld() == plugin.world();
    }

    private boolean builder(Player p) {
        return p.getGameMode() == GameMode.CREATIVE && p.hasPermission(ADMIN);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (ours(e.getBlock()) && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (ours(e.getBlock()) && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    /** Türen, Truhen, Betten, Werkbänke … nur zum Anschauen. Gegenstände benutzen (Trank, Bogen) geht weiter. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        if (b == null || !ours(b) || builder(e.getPlayer())) {
            return;
        }
        Material m = b.getType();
        if (e.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
            if (m == Material.FARMLAND || Tag.WOODEN_PRESSURE_PLATES.isTagged(m)) {
                e.setCancelled(true);
            }
            return;
        }
        if (m.isInteractable()) {
            e.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() && plugin.hall().inHall(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity().getWorld() == plugin.world()) {
            e.setCancelled(true);
            e.getEntity().setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (e.getRightClicked().getWorld() == plugin.world()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHanging(HangingBreakEvent e) {
        if (e.getEntity().getWorld() == plugin.world()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChange(EntityChangeBlockEvent e) {
        if (ours(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (e.getEntity().getWorld() == plugin.world()) {
            e.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (ours(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (ours(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (ours(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (ours(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (ours(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    /** Gegner aus Abstiegen, die es nicht mehr gibt (z. B. nach einem Neustart), verschwinden beim Laden. */
    @EventHandler
    public void onLoad(EntitiesLoadEvent e) {
        if (e.getWorld() != plugin.world()) {
            return;
        }
        for (Entity ent : e.getEntities()) {
            int run = Mobs.runOf(ent);
            if (run != 0 && plugin.runs().byId(run) == null) {
                ent.remove();
            }
        }
    }
}
