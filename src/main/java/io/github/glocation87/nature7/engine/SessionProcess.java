package io.github.glocation87.nature7.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;
import io.github.glocation87.nature7.map.GameMap;
import io.github.glocation87.nature7.map.MapVote;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.world.MapInstance;

//handles state machine transitions and lifecycle events for a single game session
public final class SessionProcess {
    private static final int COUNTDOWN_SECONDS = 15;
    private static final int VOTE_CLOSES_AT = 5;
    private static final int VOTE_CANDIDATES = 3;
    private static final int ENDING_TICKS = 5 * 20; //5 seconds

    // Session properties
    private final UUID sessionId = UUID.randomUUID();
    private final GameType type;
    private final SessionServices services;
    private final EventDispatcher events;
    private final Consumer<SessionProcess> onDispose;
    private final MapVote vote;
    private final SessionHud hud;
    private final Set<UUID> persistentPlayers = new LinkedHashSet<>();

    // Session runtime actors
    private MinigameProcess game;
    private StateManager stateMachine;
    private @Nullable MapInstance instance;
    private int countdown;
    private int countdownTicks;
    private int endingTicks;
    private long activeTicks;
    private long ticks;

    // Session constructor is private to enforce factory pattern
    private SessionProcess(
        GameType type,
        SessionServices services,
        EventDispatcher events,
        Consumer<SessionProcess> onDispose,
        List<GameMap> candidates
    ) {
        this.type = type;
        this.services = services;
        this.events = events;
        this.onDispose = onDispose;
        this.vote = new MapVote(candidates);
        this.hud = new SessionHud(type.displayName());

        stateMachine = new StateManager();
    }

    //factory pattern, idea is all sessions are created through static class constructor
    protected static SessionProcess create(
        GameType type,
        SessionServices services,
        Consumer<Class<? extends Event>> registrarCallback,
        Consumer<SessionProcess> onDispose
    ) {
        List<GameMap> candidates = MapVote.pickCandidates(services.maps().mapsFor(type.id()), VOTE_CANDIDATES, services.random());
        SessionProcess session = new SessionProcess(type, services, new EventDispatcher(registrarCallback), onDispose, candidates);
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

    public MapVote vote() {
        return vote;
    }

    // fails loudly instead of returning null, a game asking before onStart is a bug
    public MapInstance map() {
        if (instance == null) {
            throw new IllegalStateException("The map is only available once the game has started");
        }
        return instance;
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

    void onPlayerJoin(Player player) {
        if (!canJoin()) {
            services.logger().log(Level.WARNING, "Player cannot join this session process");
            return;
        }
        persistentPlayers.add(player.getUniqueId());
        services.index().add(player, this);
        // Capture before the game hook runs, otherwise items the game hands out would end up in the snapshot
        services.playerStates().capture(player);
        player.teleport(services.lobby().waitingRoom());
        services.hotbar().giveWaitingItems(player);
        hud.show(player);
        broadcastMessage(Component.text(player.getName() + " joined the game (" + playerCount() + "/" + type.maxPlayers() + ")", NamedTextColor.GREEN));
        safeHookCall("onPlayerJoin", () -> game.onPlayerJoin(player.getUniqueId()));

        if (state() == States.WAITING && persistentPlayers.size() >= type.minPlayers()) {
            beginCountdown();
        }
        updateHud();
    }

    void onPlayerLeave(Player player) {
        if (!persistentPlayers.remove(player.getUniqueId())) {
            services.logger().log(Level.WARNING, "Player " + player.getName() + " left session " + describe() + " but was not in the session");
            return;
        }
        // a player who left shouldn't get a say in the map
        vote.retract(player.getUniqueId());
        broadcastMessage(Component.text(player.getName() + " left the game", NamedTextColor.RED));
        // Restore after the game hook, so the game can still see what the leaving player was carrying
        safeHookCall("onPlayerLeave", () -> game.onPlayerLeave(player.getUniqueId()));
        hud.hide(player);
        services.playerStates().restore(player);
        services.index().remove(player);

        if (persistentPlayers.isEmpty()) {
            dispose();
        } else if (state() == States.STARTING && persistentPlayers.size() < type.minPlayers()) {
            stateMachine.setState(States.WAITING);
            broadcastMessage(Component.text("Not enough players, countdown cancelled", NamedTextColor.RED));
        }
        updateHud();
    }

    public void broadcastMessage(Component message) {
        Audience.audience(players()).sendMessage(message);
    }

    public void end(@Nullable Player winner) {
        if (state() != States.ACTIVE) {
            services.logger().log(Level.WARNING, "Attempted to end session that is not active");
            return;
        }

        stateMachine.nextState();
        endingTicks = ENDING_TICKS;
        Component headline = winner == null ? Component.text("Game Over", NamedTextColor.RED) : Component.text("Winner: " + winner.getName(), NamedTextColor.GREEN);
        Audience.audience(players()).showTitle(Title.title(headline, type.displayName()));
        safeHookCall("onEnd", game::onEnd);
        updateHud();
    }

    // jumps the countdown to 0, the game starts as soon as the voted map has loaded
    boolean forceStart() {
        if (persistentPlayers.isEmpty()) {
            return false;
        }
        if (state() == States.WAITING) {
            stateMachine.nextState();
            countdownTicks = 0;
        }
        if (state() != States.STARTING) {
            return false;
        }
        countdown = 0;
        broadcastMessage(Component.text("Starting as soon as the map is ready", NamedTextColor.YELLOW));
        return true;
    }

    private void start() {
        if (state() != States.STARTING) {
            services.logger().log(Level.WARNING, "Attempted to start session that is not counting down");
            return;
        }
        stateMachine.nextState();
        activeTicks = 0;
        // everyone lands in the map even if the game forgets to teleport someone
        MapInstance map = map();
        Location spectate = map.location(map.info().spectatorSpawn());
        for (Player player : players()) {
            player.getInventory().clear();
            player.teleport(spectate);
        }
        broadcastMessage(Component.text("Go!", NamedTextColor.GREEN));
        safeHookCall("onStart", game::onStart);
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
        if (countdown > 0) {
            countdown--;
        }
        if (countdown <= VOTE_CLOSES_AT) {
            closeVoteAndLoad();
        }
        if (countdown > 0) {
            if (countdown <= 5) {
                broadcastMessage(Component.text("Game starting in " + countdown + " seconds", NamedTextColor.YELLOW));
            }
        } else if (instance != null) {
            start();
        }
        // countdown hit 0 but the map is still copying, check again next second
    }

    // safe to call every second, only the first call does anything
    private void closeVoteAndLoad() {
        if (vote.isClosed()) {
            return;
        }
        GameMap map = vote.close(services.random());
        broadcastMessage(Component.text("Map: " + map.info().credit(), NamedTextColor.AQUA));
        services.instancer().create(map).whenCompleteAsync(this::onMapLoaded, services.mainThread());
    }

    // runs later on the main thread, by then everyone may have left, so check before using anything
    private void onMapLoaded(@Nullable MapInstance loaded, @Nullable Throwable error) {
        if (state() == States.DISPOSED) {
            if (loaded != null) {
                services.instancer().release(loaded.world());
            }
            return;
        }
        if (error != null || loaded == null) {
            services.logger().log(Level.SEVERE, "Could not load the map for " + describe(), error);
            broadcastMessage(Component.text("The map failed to load, the game was cancelled", NamedTextColor.RED));
            dispose();
            return;
        }
        instance = loaded;
        services.index().addWorld(loaded.world(), this);
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
        if (++ticks % 20 == 0) {
            updateHud();
        }
    }

    void dispatch(Class<? extends Event> eventType, Event event) {
        if (state() == States.DISPOSED) {
            services.logger().log(Level.WARNING, "Attempted to dispatch event in disposed session: " + eventType.getName());
            return;
        }
        safeHookCall(eventType.getSimpleName(), () -> events.dispatch(eventType, event));
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
            services.logger().log(Level.SEVERE, "onDispose failed in " + describe(), e);
        } finally {
            for (Player player : players()) {
                hud.hide(player);
                services.playerStates().restore(player);
                services.index().remove(player);
            }
            persistentPlayers.clear();
            events.clear();
            MapInstance map = instance;
            instance = null;
            if (map != null) {
                services.index().removeWorld(map.world());
                services.instancer().release(map.world());
            }
            onDispose.accept(this);
        }
    }

    private void updateHud() {
        if (state() == States.DISPOSED) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(label("Players", playerCount() + "/" + type.maxPlayers()));
        switch (state()) {
            case States.WAITING -> lines.add(Component.text("Waiting for " + (type.minPlayers() - playerCount()) + " more", NamedTextColor.YELLOW));
            case States.STARTING -> lines.add(countdown > 0
                ? Component.text("Starting in " + countdown + "s", NamedTextColor.YELLOW)
                : Component.text("Loading map...", NamedTextColor.YELLOW));
            case States.ACTIVE -> safeHookCall("sidebar", () -> lines.addAll(game.sidebar()));
            case States.ENDING -> lines.add(Component.text("Game over", NamedTextColor.RED));
            case States.DISPOSED -> {
            }
        }
        lines.add(Component.empty());
        lines.add(label("Map", vote.isClosed() ? vote.winner().info().name() : "voting"));
        hud.lines(lines);

        if (state() == States.STARTING) {
            hud.bossBar(Component.text(countdown > 0 ? "Starting in " + countdown + "s" : "Loading map..."),
                countdown / (float) COUNTDOWN_SECONDS);
        } else {
            hud.bossBar(type.displayName(), 1.0f);
        }
    }

    private static Component label(String name, String value) {
        return Component.text(name + ": ", NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.WHITE));
    }

    // A crashing game should take down its own session, not the tick loop or other sessions
    private void safeHookCall(String hookName, Runnable hook) {
        try {
            hook.run();
        } catch (Exception e) {
            services.logger().log(Level.SEVERE, hookName + " failed in " + describe() + ", disposing session", e);
            dispose();
        }
    }
}
