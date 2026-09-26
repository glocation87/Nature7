package io.github.glocation87.nature7.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

// 1:1 map of Player <-> SessionProcess index
public final class SessionIndex {

    private final Map<UUID, SessionProcess> playerToSession = new HashMap<>();

    public void add(Player player, SessionProcess session) {
        playerToSession.put(player.getUniqueId(), session);
    }

    public void remove(Player player) {
        playerToSession.remove(player.getUniqueId());
    }

    public Optional<SessionProcess> getSession(Player player) {
        return Optional.ofNullable(playerToSession.get(player.getUniqueId()));
    }
}
