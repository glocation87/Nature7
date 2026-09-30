package io.github.glocation87.nature7.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import io.github.glocation87.nature7.types.GameType;

public final class SessionManager {
    private final SessionServices services;
    private final Router router;
    private final List<SessionProcess> activeSessions = new ArrayList<>();

    public SessionManager(SessionServices services, Router router) {
        this.services = services;
        this.router = router;
    }

    //wrapper for SessionProcess.create()
    private SessionProcess createSessionProcess(GameType minigameType) {
        SessionProcess newProcess = SessionProcess.create(minigameType, services, router::ensureRegistered, activeSessions::remove);
        activeSessions.add(newProcess);
        return newProcess;
    }

    public JoinResult joinSession(Player player, GameType minigameType) {
        if (services.index().getSession(player).isPresent()) {
            return JoinResult.ALREADY_IN_GAME;
        }
        if (services.maps().mapsFor(minigameType.id()).isEmpty()) {
            return JoinResult.NO_MAPS;
        }
        // player is not mapped to any session, search for an available one or create one
        SessionProcess selectedSession = activeSessions.stream()
            .filter(process -> process.type().id().equals(minigameType.id()) && process.canJoin())
            .findFirst()
            .orElseGet(() -> createSessionProcess(minigameType));

        selectedSession.onPlayerJoin(player);
        return JoinResult.JOINED;
    }

    public void leaveSession(Player player) {
        services.index().getSession(player).ifPresent(session -> session.onPlayerLeave(player));
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
