package io.github.glocation87.nature7.engine;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

public final class Router implements Listener {
    private final Plugin plugin;
    private final SessionIndex index;
    private final Set<Class<? extends Event>> registered = new HashSet<>();

    public Router(Plugin plugin, SessionIndex index) {
        this.plugin = plugin;
        this.index = index;
    }

    void ensureRegistered(Class<? extends Event> type) {
        if (!registered.add(type)) return;
        plugin.getServer().getPluginManager()
            .registerEvent(type, this, EventPriority.NORMAL, (listener, event) -> route(type, event), plugin, false);
    }

    private void route(Class<? extends Event> type, Event event) {
        if (!type.isInstance(event)) {
            return;
        }

        Player subject = subjectOf(event);
        if (subject != null) {
            index.getSession(subject).ifPresent(session -> session.dispatch(type, event));
        }
    }

    private static @Nullable Player subjectOf(Event event) {
        return switch (event) {
            case PlayerEvent e -> e.getPlayer();
            case EntityEvent e when e.getEntity() instanceof Player player -> player;
            case BlockBreakEvent e -> e.getPlayer();
            case BlockPlaceEvent e -> e.getPlayer();
            case InventoryInteractEvent e when e.getWhoClicked() instanceof Player player -> player;
            default -> null;
        };
    }

}
