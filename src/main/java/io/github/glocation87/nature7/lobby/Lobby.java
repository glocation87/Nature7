package io.github.glocation87.nature7.lobby;

import io.github.glocation87.nature7.item.HotbarItems;
import io.github.glocation87.nature7.player.PlayerStateService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class Lobby {
    private static final String SPAWN = "lobby.spawn";
    private static final String WAITING_ROOM = "lobby.waiting-room";

    private final JavaPlugin plugin;
    private final HotbarItems hotbar;

    public Lobby(JavaPlugin plugin, HotbarItems hotbar) {
        this.plugin = plugin;
        this.hotbar = hotbar;
    }

    public Location spawn() {
        Location spawn = configured(SPAWN);
        return spawn != null ? spawn : plugin.getServer().getWorlds().getFirst().getSpawnLocation();
    }

    public Location waitingRoom() {
        Location room = configured(WAITING_ROOM);
        return room != null ? room : spawn();
    }

    public void setSpawn(Location location) {
        plugin.getConfig().set(SPAWN, location);
        plugin.saveConfig();
    }

    public void setWaitingRoom(Location location) {
        plugin.getConfig().set(WAITING_ROOM, location);
        plugin.saveConfig();
    }

    public boolean contains(World world) {
        return world.equals(spawn().getWorld());
    }

    public void send(Player player) {
        PlayerStateService.reset(player);
        player.teleport(spawn());
        hotbar.giveLobbyItems(player);
    }

    private @Nullable Location configured(String path) {
        try {
            return plugin.getConfig().getLocation(path);
        } catch (IllegalArgumentException e) {
            // the saved world doesn't exist anymore, a deleted world shouldn't stop startup
            plugin.getLogger().warning("Ignoring " + path + " in config.yml: " + e.getMessage());
            return null;
        }
    }
}
