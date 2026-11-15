package de.moinmorni.burgverlies.run;

import de.moinmorni.burgverlies.Items;
import de.moinmorni.burgverlies.Texts;
import de.moinmorni.burgverlies.Ui;
import de.moinmorni.burgverlies.dungeon.Offer;
import de.moinmorni.burgverlies.dungeon.ShopMath;
import de.moinmorni.burgverlies.menu.Menu;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Laden der Krämerin: Kerkertaler gegen Verbesserungen, Tränke und Wiederbelebungen. */
public final class ShopMenu {

    /** Plätze im Menü (zweite Reihe). */
    public static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 22};
    private static final Material[] ICONS = {Material.IRON_SWORD, Material.IRON_CHESTPLATE, Material.BOW,
        Material.SHIELD, Material.ARROW, Material.POTION, Material.GOLDEN_APPLE, Material.TOTEM_OF_UNDYING};

    private final Runs runs;

    ShopMenu(Runs runs) {
        this.runs = runs;
    }

    void open(Player p, Run run) {
        String lang = runs.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "shop.title"));
        draw(menu, p, run);
        menu.refresher(() -> draw(menu, p, run));
        menu.open(p);
        Ui.sound(p, "entity.villager.trade", 1f);
    }

    private void draw(Menu menu, Player p, Run run) {
        String lang = runs.lang(p);
        Hero h = run.hero(p.getUniqueId());
        if (h == null) {
            return;
        }
        menu.set(4, Items.named(Material.GOLD_NUGGET, 1, Texts.text(lang, "shop.wallet",
                Placeholder.unparsed("taler", String.valueOf(h.taler()))), Texts.lines(lang, "shop.wallet.lore")), null);
        Offer[] offers = Offer.values();
        for (int i = 0; i < offers.length; i++) {
            Offer o = offers[i];
            int level = level(run, h, o);
            int price = ShopMath.price(o, level, run.floor);
            boolean maxed = ShopMath.maxed(o, level);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "shop." + o.key() + ".desc"));
            lore.add(Component.empty());
            if (o.upgrade()) {
                lore.add(Texts.text(lang, "shop.level", Placeholder.unparsed("level", String.valueOf(level)),
                        Placeholder.unparsed("max", String.valueOf(o.maxLevel()))));
            }
            lore.add(maxed ? Texts.text(lang, "shop.maxed") : Texts.text(lang, h.taler() >= price ? "shop.price"
                    : "shop.price.poor", Placeholder.unparsed("price", String.valueOf(price))));
            ItemStack icon = Items.named(ICONS[i], 1, Texts.text(lang, "shop." + o.key()), lore);
            Items.glow(icon, !maxed && h.taler() >= price);
            menu.set(SLOTS[i], icon, who -> buy(who, run, o));
        }
    }

    private int level(Run run, Hero h, Offer o) {
        return h.level(o);
    }

    private void buy(Player p, Run run, Offer o) {
        Hero h = run.hero(p.getUniqueId());
        String lang = runs.lang(p);
        if (h == null || run.stage != Run.Stage.PLAYING) {
            return;
        }
        int level = h.level(o);
        ShopMath.Purchase buy = ShopMath.buy(h.taler(), o, level, run.floor);
        switch (buy.result()) {
            case MAXED -> {
                p.sendMessage(Texts.msg(lang, "shop.maxed.msg"));
                Ui.sound(p, "entity.villager.no", 1f);
            }
            case TOO_POOR -> {
                p.sendMessage(Texts.msg(lang, "shop.poor", Placeholder.unparsed("price", String.valueOf(buy.price()))));
                Ui.sound(p, "entity.villager.no", 1f);
            }
            case OK -> {
                h.pay(buy.walletAfter());
                h.level(o, buy.levelAfter());
                if (o.upgrade()) {
                    Gear.apply(p, h, lang);
                } else if (o == Offer.REVIVE) {
                    run.revives.addRevive(p.getUniqueId());
                } else {
                    ItemStack item = Gear.consumable(o, lang);
                    if (item != null) {
                        p.getInventory().addItem(item);
                    }
                }
                p.sendMessage(Texts.msg(lang, "shop.bought", Placeholder.component("item", Texts.text(lang,
                        "shop." + o.key())), Placeholder.unparsed("price", String.valueOf(buy.price()))));
                Ui.sound(p, "entity.villager.yes", 1f);
                Ui.sound(p, "block.amethyst_block.chime", 1.2f);
                runs.sidebars(run);
            }
        }
        var holder = p.getOpenInventory().getTopInventory().getHolder(false);
        if (holder instanceof Menu m) {
            m.refresh();
        }
    }
}
