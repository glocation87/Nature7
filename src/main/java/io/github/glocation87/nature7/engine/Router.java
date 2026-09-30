package io.github.glocation87.nature7.engine;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.world.WorldEvent;
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
        sessionFor(event).ifPresent(session -> session.dispatch(type, event));
    }

    // player first, an admin standing in a match world who isn't playing must not trigger the game's player handlers
    private Optional<SessionProcess> sessionFor(Event event) {
        Player subject = subjectOf(event);
        if (subject != null) {
            return index.getSession(subject);
        }
        World world = worldOf(event);
        return world == null ? Optional.empty() : index.getSession(world);
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

    // for events without a player: tnt, falling blocks, mobs dying
    private static @Nullable World worldOf(Event event) {
        return switch (event) {
            case WorldEvent e -> e.getWorld();
            case BlockEvent e -> e.getBlock().getWorld();
            case EntityEvent e -> e.getEntity().getWorld();
            case VehicleEvent e -> e.getVehicle().getWorld();
            default -> null;
        };
    }
}
