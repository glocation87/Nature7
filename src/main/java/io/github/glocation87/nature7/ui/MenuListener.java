package io.github.glocation87.nature7.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.Plugin;

public final class MenuListener implements Listener {
    private final Plugin plugin;

    public MenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    // cancelling every click while a menu is open closes shift click, number key, double click and offhand dupes in one go
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player
            && event.getClickedInventory() == event.getView().getTopInventory()) {
            int slot = event.getSlot();
            // opening/closing inventories inside a click event can desync the client, so run it next tick
            plugin.getServer().getScheduler().runTask(plugin, () -> menu.click(player, slot));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
