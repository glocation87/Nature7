package io.github.glocation87.nature7.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.StateManager;
import io.github.glocation87.nature7.engine.EventDispatcher;
import io.github.glocation87.nature7.engine.SessionIndex;

//handles state machine transitions and lifecycle events for a single game session
public final class SessionProcess {
    private static final int COUNTDOWN_SECONDS = 10;
    private static final int ENDING_TICKS = 5 * 20; //5 seconds

    // Session properties
    private final UUID session_id = UUID.randomUUID();
    private final GameType type;
    private final PlayerStateService player_states;
    private final SessionIndex index;
    private final EventDispatcher events;
    private final Consumer<SessionProcess> on_dispose;
    private final Logger logger;
    private final Set<UUID> persistent_players = new LinkedHashSet<>();

    // Session runtime actors
    private MinigameProcess game;
    private StateManager state_machine;
    private int countdown;
    private int countdown_ticks;
    private int ending_ticks;
    private long active_ticks;

    // Session constructor is private to enforce factory pattern
    private SessionProcess(
        GameType type,
        PlayerStateService player_states,
        SessionIndex index,
        EventDispatcher events,
        Consumer<SessionProcess> on_dispose,
        Logger logger
    ) {
        this.type = type;
        this.player_states = player_states;
        this.index = index;
        this.events = events;
        this.on_dispose = on_dispose;
        this.logger = logger;

        state_machine = new StateManager();
    }

    //factory pattern, idea is all sessions are created through static class constructor
    protected static SessionProcess create(
        GameType type,
        PlayerStateService player_states,
        SessionIndex index,
        Consumer<Class<? extends Event>> registrar_callback,
        Consumer<SessionProcess> on_dispose,
        Logger logger
    ) {
        SessionProcess session = new SessionProcess(type, player_states, index, new EventDispatcher(registrar_callback), on_dispose, logger);
        session.game = type.factory().apply(session);
        session.game.onSetup();
        return session;
    }

    public UUID id() {
        return session_id;
    }

    public GameType type() {
        return type;
    }

    public States state() {
        return state_machine.getCurrentState();
    }

    public int playerCount() {
        return persistent_players.size();
    }

    EventDispatcher events() {
        return events;
    }

    private String describe() {
        return type.id() + " session " + session_id;
    }

    public List<Player> players() {
        List<Player> online = new ArrayList<>(persistent_players.size());
        for (UUID uuid : persistent_players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }

    public boolean canJoin() {
        if (playerCount() >= type().maxPlayers()) return false;
        if (state() != States.WAITING) return false;
        return true;
    }

    boolean onPlayerJoin(Player player) {
        if (!canJoin()) {
            Logger.log(Level.WARNING, "Player cannot join this session process");
            return false;
        }
        persistent_players.add(player.getUniqueId());
        broadcastMessage(Component.text(player.getName() + " joined the game", NamedTextColor.GREEN));
        safeHookCall("onPlayerJoin", () -> game.onPlayerJoin(player));
        //TODO need to capture player inventory state

        if (state() == States.WAITING && persistent_players.size() >= type.minPlayers()) {
            beginCountdown();
        }
        return true;
    }

    void onPlayerLeave(Player player) {
        if (!persistent_players.remove(player.getUniqueId())) {
            logger.log(Level.WARNING, "Player " + player.getName() + " left session " + describe() + " but was not in the session");
            return;
        }
        broadcastMessage(Component.text(player.getName() + " left the game", NamedTextColor.RED));
        safeHookCall("onPlayerLeave", () -> game.onPlayerLeave(player));

        //TODO need to restore player inventory state
        //TODO handle case for players leaving during countdown
    }

    public void broadcastMessage(Component message) {
        Audience.audience(players()).sendMessage(message);
    }

    public void end(@Nullable Player winner) {
        if (state() != States.ACTIVE) {
            logger.log(Level.WARNING, "Attempted to end session that is not active");
            return;
        }

        state_machine.nextState();
        ending_ticks = ENDING_TICKS;
        Component headline = winner == null ? Component.text("Game Over", NamedTextColor.RED) : Component.text("Winner: " + winner.getName(), NamedTextColor.GREEN);
        Audience.audience(players()).showTitle(Title.title(headline, type.displayName()));
        safeHookCall("onEnd", game::onEnd);
    }

    private void start() {
        if (state() != States.WAITING) {
            logger.log(Level.WARNING, "Attempted to start session that is not waiting");
            return;
        }
        state_machine.nextState();
        active_ticks = 0;
        safeHookCall("onStart", game::onStart);
        broadcastMessage(Component.text("Go!", NamedTextColor.GREEN));
    }

    private void beginCountdown() {
        state_machine.setState(States.STARTING);
        countdown = COUNTDOWN_SECONDS;
        countdown_ticks = 0;
        broadcastMessage(Component.text("Game starting in " + countdown + " seconds", NamedTextColor.YELLOW));
    }

    private void tickStarting() {
        if (++countdown_ticks % 20 != 0) {
            return;
        }
        countdown--;
        if (countdown <= 0) {
            start();
        } else if (countdown <= 5) {
            broadcastMessage(Component.text("Game starting in " + countdown + " seconds", NamedTextColor.YELLOW));
        }
    }

    void tick() {
        switch (state()) {
            case States.STARTING -> tickStarting();
            case States.ACTIVE -> {
                active_ticks++;
                safeHookCall("onTick", () -> game.onTick(active_ticks));
            }
            case States.ENDING -> {
                ending_ticks--;
                if (ending_ticks <= 0) {
                    dispose();
                }
            }
            default -> {
                logger.log(Level.WARNING, "Session tick called in invalid state: " + state());
            }
        }
    }

    void dispatch(Class<? extends Event> event_type, Event event) {
        if (state() == States.DISPOSED) {
            logger.log(Level.WARNING, "Attempted to dispatch event in disposed session: " + event_type.getName());
            return;
        }
        events.dispatch(event_type, event);
    }

    void dispose() {
        if (state() == States.DISPOSED) {
            logger.log(Level.WARNING, "Attempted to dispose session that is already disposed");
            return;
        }
        state_machine.nextState();
        try {
            game.onDispose();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "onDispose failed in " + describe(), e);
        } finally {
            List<Player> players = players();
            for (Player player : players) {
                //player_states.restore(player);

            }
            players.clear();
            events.clear();
            on_dispose.accept(this);
        }

    }

    private void safeHookCall(String hookName, Runnable hook) {
        try {
            hook.run();
        } catch (Exception e) {
             logger.log(Level.SEVERE, hook + " failed in " + describe() + ", disposing session", e);
            dispose();
        }
    }
}
