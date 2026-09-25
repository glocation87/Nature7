package io.github.glocation87.nature7.player;

import io.github.glocation87.nature7.engine.SessionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final PlayerStateService playerStates;
    private final SessionManager sessions;

    public PlayerConnectionListener(PlayerStateService playerStates, SessionManager sessions) {
        this.playerStates = playerStates;
        this.sessions = sessions;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerStates.restoreFromDisk(event.getPlayer());
    }

    // Quitting counts as leaving, so the state is restored before the server saves player data
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.leaveSession(event.getPlayer());
    }
}
