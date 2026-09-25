package io.github.glocation87.nature7.engine;

import java.util.list;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import io.github.glocation87.nature7.player.PlayerStateService;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.engine.SessionIndex;
import io.github.glocation87.nature7.engine.GameRegistry;

public final class SessionManager {
    private Logger logger;
    private SessionIndex index;
    private GameRegistry registry;
    private PlayerStateService player_states;
    private final List<SessionProcess> active_sessions;

    //wrapper for SessionProcess.create()
    private final SessionProcess createSessionProcess(GameType minigame_type) {
        SessionProcess new_process = SessionProcess.create(minigame_type, player_states, index, null, event -> logger.Log(event.id()), SessionIndex::remove, logger);
        return new_process;
    }

    public SessionManager(SessionIndex index, Logger logger) {
        this.logger = logger;
        this.index = index;
        this.registry = new GameRegistry();
        this.active_sessions = new ArrayList<>();
    }

    public SessionProcess joinSession(Player player, GameType minigame_type) {
        //check game registry, if not found, register new minigame type ideally this is done prior to SessionManager construction
        if (registry.getRecord(minigame_type.id()).isEmpty()) {
            //throw new IllegalArgumentException("Game type not registered: " + minigame_type.id());
            registry.register(minigame_type);
        }

        if (index.getSession(player).isEmpty()) {
            // player is not mapped to any session, search for an available one or create one
            SessionProcess selected_session = active_sessions.stream().filter(process -> return (process.type().id() ==  minigame_type.id() && process.canJoin());)
                .findFirst()
                .orElseGet(() -> createSessionProcess(minigame_type));

            //map new player to session
            index.add(player, selected_session);
            if (selected_session.onPlayerJoin(player)) return selected_session;
        }
    }

    public void leaveSession(Player player) {
        if (index.getSession(player).isEmpty()) {
            index.getSesstion(player).get().onPlayerLeave(player);
            index.remove(player);
        }
    }

    public void forceStart(SessionProcess session) {}

    public List<SessionProcess> getActiveSessions() {
        return active_sessions;
    }

    public void tickAll() {
        for (SessionProcess session : active_sessions) {
            session.tick();
        }
    }

    public void disposeAll() {
        for (SessionProcess session : active_sessions) {
            session.dispose();
        }
    }
}
