package io.github.glocation87.nature7.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

// 1:1 map of Player <-> SessionProcess index
public final class SessionIndex {

    private final Map<UUID, SessionProcess> player_to_session = new HashMap<>();

    public void add(Player player, SessionProcess session) {
        player_to_session.put(player.getUniqueId(), session);
    }

    public void remove(Player player) {
        player_to_session.remove(player.getUniqueId());
    }

    public Optional<SessionProcess> getSession(Player player) {
        return Optional.ofNullable(player_to_session.get(player.getUniqueId()));
    }
}
