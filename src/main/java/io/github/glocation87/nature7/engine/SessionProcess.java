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
import io.github.glocation87.nature7.player.PlayerStateService;
import io.github.glocation87.nature7.types.GameType;

//handles state machine transitions and lifecycle events for a single game session
public final class SessionProcess {
    private static final int COUNTDOWN_SECONDS = 10;
    private static final int ENDING_TICKS = 5 * 20; //5 seconds

    // Session properties
    private final UUID sessionId = UUID.randomUUID();
    private final GameType type;
    private final PlayerStateService playerStates;
    private final SessionIndex index;
    private final EventDispatcher events;
    private final Consumer<SessionProcess> onDispose;
    private final Logger logger;
    private final Set<UUID> persistentPlayers = new LinkedHashSet<>();

    // Session runtime actors
    private MinigameProcess game;
    private StateManager stateMachine;
    private int countdown;
    private int countdownTicks;
    private int endingTicks;
    private long activeTicks;

    // Session constructor is private to enforce factory pattern
    private SessionProcess(
        GameType type,
        PlayerStateService playerStates,
        SessionIndex index,
        EventDispatcher events,
        Consumer<SessionProcess> onDispose,
        Logger logger
    ) {
        this.type = type;
        this.playerStates = playerStates;
        this.index = index;
        this.events = events;
        this.onDispose = onDispose;
        this.logger = logger;

        stateMachine = new StateManager();
    }

    //factory pattern, idea is all sessions are created through static class constructor
    protected static SessionProcess create(
        GameType type,
        PlayerStateService playerStates,
        SessionIndex index,
        Consumer<Class<? extends Event>> registrarCallback,
        Consumer<SessionProcess> onDispose,
        Logger logger
    ) {
        SessionProcess session = new SessionProcess(type, playerStates, index, new EventDispatcher(registrarCallback), onDispose, logger);
        session.game = type.factory().apply(session);
        session.game.onSetup();
        return session;
    }

    public UUID id() {
        return sessionId;
    }

    public GameType type() {
        return type;
    }

    public States state() {
        return stateMachine.getCurrentState();
    }

    public int playerCount() {
        return persistentPlayers.size();
    }

    EventDispatcher events() {
        return events;
    }

    private String describe() {
        return type.id() + " session " + sessionId;
    }

    public List<Player> players() {
        List<Player> online = new ArrayList<>(persistentPlayers.size());
        for (UUID uuid : persistentPlayers) {
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
            logger.log(Level.WARNING, "Player cannot join this session process");
            return false;
        }
        persistentPlayers.add(player.getUniqueId());
        index.add(player, this);
        // Capture before the game hook runs, otherwise items the game hands out would end up in the snapshot
        playerStates.capture(player);
        broadcastMessage(Component.text(player.getName() + " joined the game", NamedTextColor.GREEN));
        safeHookCall("onPlayerJoin", () -> game.onPlayerJoin(player));

        if (state() == States.WAITING && persistentPlayers.size() >= type.minPlayers()) {
            beginCountdown();
        }
        return true;
    }

    void onPlayerLeave(Player player) {
        if (!persistentPlayers.remove(player.getUniqueId())) {
            logger.log(Level.WARNING, "Player " + player.getName() + " left session " + describe() + " but was not in the session");
            return;
        }
        broadcastMessage(Component.text(player.getName() + " left the game", NamedTextColor.RED));
        // Restore after the game hook, so the game can still see what the leaving player was carrying
        safeHookCall("onPlayerLeave", () -> game.onPlayerLeave(player));
        playerStates.restore(player);
        index.remove(player);

        if (persistentPlayers.isEmpty()) {
            dispose();
        } else if (state() == States.STARTING && persistentPlayers.size() < type.minPlayers()) {
            stateMachine.setState(States.WAITING);
            broadcastMessage(Component.text("Not enough players, countdown cancelled", NamedTextColor.RED));
        }
    }

    public void broadcastMessage(Component message) {
        Audience.audience(players()).sendMessage(message);
    }

    public void end(@Nullable Player winner) {
        if (state() != States.ACTIVE) {
            logger.log(Level.WARNING, "Attempted to end session that is not active");
            return;
        }

        stateMachine.nextState();
        endingTicks = ENDING_TICKS;
        Component headline = winner == null ? Component.text("Game Over", NamedTextColor.RED) : Component.text("Winner: " + winner.getName(), NamedTextColor.GREEN);
        Audience.audience(players()).showTitle(Title.title(headline, type.displayName()));
        safeHookCall("onEnd", game::onEnd);
    }

    private void start() {
        if (state() != States.STARTING) {
            logger.log(Level.WARNING, "Attempted to start session that is not counting down");
            return;
        }
        stateMachine.nextState();
        activeTicks = 0;
        safeHookCall("onStart", game::onStart);
        broadcastMessage(Component.text("Go!", NamedTextColor.GREEN));
    }

    private void beginCountdown() {
        stateMachine.setState(States.STARTING);
        countdown = COUNTDOWN_SECONDS;
        countdownTicks = 0;
        broadcastMessage(Component.text("Game starting in " + countdown + " seconds", NamedTextColor.YELLOW));
    }

    private void tickStarting() {
        if (++countdownTicks % 20 != 0) {
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
                activeTicks++;
                safeHookCall("onTick", () -> game.onTick(activeTicks));
            }
            case States.ENDING -> {
                endingTicks--;
                if (endingTicks <= 0) {
                    dispose();
                }
            }
            // Waiting sessions are ticked 20 times a second too, nothing to do and nothing to log
            case States.WAITING, States.DISPOSED -> {
            }
        }
    }

    void dispatch(Class<? extends Event> eventType, Event event) {
        if (state() == States.DISPOSED) {
            logger.log(Level.WARNING, "Attempted to dispatch event in disposed session: " + eventType.getName());
            return;
        }
        events.dispatch(eventType, event);
    }

    void dispose() {
        // Several paths lead here (last player leaving, ENDING timing out, a crashing hook), a second call is normal
        if (state() == States.DISPOSED) {
            return;
        }
        // StateManager only allows DISPOSED from ENDING, so an early dispose passes through ENDING first
        if (state() != States.ENDING) {
            stateMachine.setState(States.ENDING);
        }
        stateMachine.nextState();
        try {
            game.onDispose();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "onDispose failed in " + describe(), e);
        } finally {
            for (Player player : players()) {
                playerStates.restore(player);
                index.remove(player);
            }
            persistentPlayers.clear();
            events.clear();
            onDispose.accept(this);
        }
    }

    private void safeHookCall(String hookName, Runnable hook) {
        try {
            hook.run();
        } catch (Exception e) {
            logger.log(Level.SEVERE, hookName + " failed in " + describe() + ", disposing session", e);
            dispose();
        }
    }
}
