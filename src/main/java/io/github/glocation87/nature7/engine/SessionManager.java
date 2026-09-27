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
    private final PlayerStateService playerStates;
    private final List<SessionProcess> activeSessions = new ArrayList<>();

    public SessionManager(SessionIndex index, Router router, PlayerStateService playerStates, Logger logger) {
        this.logger = logger;
        this.index = index;
        this.router = router;
        this.playerStates = playerStates;
    }

    //wrapper for SessionProcess.create()
    private SessionProcess createSessionProcess(GameType minigameType) {
        SessionProcess newProcess = SessionProcess.create(
            minigameType, playerStates, index, router::ensureRegistered, activeSessions::remove, logger);
        activeSessions.add(newProcess);
        return newProcess;
    }

    public Optional<SessionProcess> joinSession(Player player, GameType minigameType) {
        if (index.getSession(player).isPresent()) {
            return Optional.empty();
        }
        // player is not mapped to any session, search for an available one or create one
        SessionProcess selectedSession = activeSessions.stream()
            .filter(process -> process.type().id().equals(minigameType.id()) && process.canJoin())
            .findFirst()
            .orElseGet(() -> createSessionProcess(minigameType));

        return selectedSession.onPlayerJoin(player) ? Optional.of(selectedSession) : Optional.empty();
    }

    public void leaveSession(Player player) {
        index.getSession(player).ifPresent(session -> session.onPlayerLeave(player));
    }

    public boolean forceStart(SessionProcess session) {
        return session.forceStart();
    }

    public List<SessionProcess> getActiveSessions() {
        return Collections.unmodifiableList(activeSessions);
    }

    public void tickAll() {
        // copy first, a session can dispose itself mid-tick and remove itself from the list
        for (SessionProcess session : List.copyOf(activeSessions)) {
            session.tick();
        }
    }

    public void disposeAll() {
        for (SessionProcess session : List.copyOf(activeSessions)) {
            session.dispose();
        }
    }
}
