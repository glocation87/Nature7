package io.github.glocation87.nature7.ui;

import io.github.glocation87.nature7.engine.SessionIndex;
import io.github.glocation87.nature7.engine.SessionManager;
import io.github.glocation87.nature7.item.HotbarItems;
import io.github.glocation87.nature7.item.ItemTags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

// runs the hotbar items, and stops tagged items being moved, dropped or swapped anywhere
public final class HotbarListener implements Listener {
    private final ItemTags tags;
    private final GameSelectorMenu gameSelector;
    private final SessionManager sessionManager;
    private final SessionIndex index;

    public HotbarListener(ItemTags tags, GameSelectorMenu gameSelector, SessionManager sessionManager, SessionIndex index) {
        this.tags = tags;
        this.gameSelector = gameSelector;
        this.sessionManager = sessionManager;
        this.index = index;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // fires once per hand, without this the menu opens twice
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) {
            return;
        }
        String id = tags.idOf(event.getItem());
        if (id == null) {
            return;
        }
        Player player = event.getPlayer();
        switch (id) {
            case HotbarItems.GAME_SELECTOR -> gameSelector.open(player);
            case HotbarItems.MAP_VOTE -> index.getSession(player)
                .ifPresent(session -> MapVoteMenu.open(player, session.vote()));
            case HotbarItems.LEAVE_GAME -> sessionManager.leaveSession(player);
            default -> {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        boolean tagged = tags.isTagged(event.getCurrentItem()) || tags.isTagged(event.getCursor());
        if (event.getClick() == ClickType.NUMBER_KEY) {
            // number keys swap with a hotbar slot, so check the item being swapped in too
            tagged |= tags.isTagged(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        }
        if (tagged) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (tags.isTagged(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (tags.isTagged(event.getMainHandItem()) || tags.isTagged(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }
}
