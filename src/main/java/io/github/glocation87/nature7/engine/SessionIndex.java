package io.github.glocation87.nature7.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;

// Player -> SessionProcess and match World -> SessionProcess
// keyed by UUID, holding a World object would keep an unloaded world in memory
public final class SessionIndex {

    private final Map<UUID, SessionProcess> playerToSession = new HashMap<>();
    private final Map<UUID, SessionProcess> worldToSession = new HashMap<>();

    public void add(Player player, SessionProcess session) {
        playerToSession.put(player.getUniqueId(), session);
    }

    public void remove(Player player) {
        playerToSession.remove(player.getUniqueId());
    }

    public Optional<SessionProcess> getSession(Player player) {
        return Optional.ofNullable(playerToSession.get(player.getUniqueId()));
    }

    public void addWorld(World world, SessionProcess session) {
        worldToSession.put(world.getUID(), session);
    }

    public void removeWorld(World world) {
        worldToSession.remove(world.getUID());
    }

    public Optional<SessionProcess> getSession(World world) {
        return Optional.ofNullable(worldToSession.get(world.getUID()));
    }
}
