package io.github.glocation87.nature7.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import io.github.glocation87.nature7.player.PlayerStateService;
import io.github.glocation87.nature7.types.GameType;

public final class SessionManager {
    private final Logger logger;
    private final SessionIndex index;
    private final Router router;
    private final GameRegistry registry;
    private final PlayerStateService player_states;
    private final List<SessionProcess> active_sessions = new ArrayList<>();

    public SessionManager(SessionIndex index, Router router, PlayerStateService player_states, Logger logger) {
        this.logger = logger;
        this.index = index;
        this.router = router;
        this.player_states = player_states;
        this.registry = new GameRegistry();
    }

    //wrapper for SessionProcess.create()
    private SessionProcess createSessionProcess(GameType minigame_type) {
        SessionProcess new_process = SessionProcess.create(
            minigame_type, player_states, index, router::ensureRegistered, active_sessions::remove, logger);
        active_sessions.add(new_process);
        return new_process;
    }

    public Optional<SessionProcess> joinSession(Player player, GameType minigame_type) {
        //check game registry, if not found, register new minigame type ideally this is done prior to SessionManager construction
        if (registry.find(minigame_type.id()).isEmpty()) {
            registry.register(minigame_type);
        }
        if (index.getSession(player).isPresent()) {
            return Optional.empty();
        }
        // player is not mapped to any session, search for an available one or create one
        SessionProcess selected_session = active_sessions.stream()
            .filter(process -> process.type().id().equals(minigame_type.id()) && process.canJoin())
            .findFirst()
            .orElseGet(() -> createSessionProcess(minigame_type));

        return selected_session.onPlayerJoin(player) ? Optional.of(selected_session) : Optional.empty();
    }

    public void leaveSession(Player player) {
        index.getSession(player).ifPresent(session -> session.onPlayerLeave(player));
    }

    public void forceStart(SessionProcess session) {}

    public List<SessionProcess> getActiveSessions() {
        return Collections.unmodifiableList(active_sessions);
    }

    public void tickAll() {
        // copy first, a session can dispose itself mid-tick and remove itself from the list
        for (SessionProcess session : List.copyOf(active_sessions)) {
            session.tick();
        }
    }

    public void disposeAll() {
        for (SessionProcess session : List.copyOf(active_sessions)) {
            session.dispose();
        }
    }
}
