package io.github.glocation87.nature7.lobby;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;

// everything here is by world, so waiting players are covered too since the waiting room is in the lobby world
public final class LobbyListener implements Listener {
    private final Lobby lobby;

    public LobbyListener(Lobby lobby) {
        this.lobby = lobby;
    }

    // HIGH so it runs after PlayerConnectionListener (NORMAL) has restored anything left by a crash
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        lobby.send(event.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !lobby.contains(player.getWorld())) {
            return;
        }
        event.setCancelled(true);
        // cancelling void damage alone would leave them falling forever
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            player.teleport(lobby.spawn());
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (lobby.contains(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        protectBlocks(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        protectBlocks(event.getPlayer(), event);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (lobby.contains(event.getPlayer().getWorld())) {
            event.setCancelled(true);
        }
    }

    private void protectBlocks(Player player, Cancellable event) {
        if (lobby.contains(player.getWorld()) && !player.hasPermission("nature7.build")) {
            event.setCancelled(true);
        }
    }
}
