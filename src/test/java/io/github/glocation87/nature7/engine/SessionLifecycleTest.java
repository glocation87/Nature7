package io.github.glocation87.nature7.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.glocation87.nature7.games.laststanding.LastStandingMap;
import io.github.glocation87.nature7.item.HotbarItems;
import io.github.glocation87.nature7.item.ItemTags;
import io.github.glocation87.nature7.lobby.Lobby;
import io.github.glocation87.nature7.map.MapRegistry;
import io.github.glocation87.nature7.player.PlayerConnectionListener;
import io.github.glocation87.nature7.player.PlayerStateService;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.world.WorldInstancer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

// Drives the real SessionManager, SessionProcess, SessionIndex, StateManager, Router, PlayerStateService,
// MapRegistry and WorldInstancer together, with a small game that records every hook the engine calls
class SessionLifecycleTest {

    private static final Logger LOGGER = Logger.getLogger("test");
    private static final int COUNTDOWN_TICKS = 15 * 20;
    private static final int ENDING_TICKS = 5 * 20;
    private static final String MAP_YML = """
        name: Test Arena
        spectator-spawn: {x: 0.5, y: 65, z: 0.5}
        bounds:
          min: {x: -20, y: 0, z: -20}
          max: {x: 20, y: 128, z: 20}
        game:
          spawns:
            - {x: 5.5, y: 65, z: 0.5}
            - {x: -4.5, y: 65, z: 0.5}
        """;

    private static final class RecordingGame extends MinigameProcess {

        private final List<String> calls;
        private final boolean failOnStart;

        RecordingGame(SessionProcess session, List<String> calls, boolean failOnStart) {
            super(session);
            this.calls = calls;
            this.failOnStart = failOnStart;
        }

        @Override
        protected void onSetup() {
            calls.add("setup");
            listen(PlayerToggleSneakEvent.class, event -> calls.add("sneak:" + event.getPlayer().getName()));
            listen(WorldSaveEvent.class, event -> calls.add("save:" + event.getWorld().getName()));
        }

        @Override
        protected void onPlayerJoin(UUID playerId) {
            Player player = Bukkit.getPlayer(playerId);
            calls.add("join:" + player.getName());
            player.getInventory().addItem(ItemStack.of(Material.IRON_SWORD));
        }

        @Override
        protected void onPlayerLeave(UUID playerId) {
            Player player = Bukkit.getPlayer(playerId);
            calls.add("leave:" + player.getName() + ":sword=" + player.getInventory().contains(Material.IRON_SWORD));
        }

        @Override
        protected void onStart() {
            calls.add("start");
            if (failOnStart) {
                throw new IllegalStateException("broken game");
            }
        }

        @Override
        protected void onEnd() {
            calls.add("end");
        }

        @Override
        protected void onDispose() {
            calls.add("dispose");
        }
    }

    @TempDir
    Path temp;

    private ServerMock server;
    private PluginMock plugin;
    private PlayerStateService states;
    private WorldInstancer instancer;
    private SessionIndex index;
    private SessionManager sessions;
    private final List<String> calls = new ArrayList<>();
    private GameType testGame;

    @BeforeEach
    void setUp() throws IOException {
        Path level = temp.resolve("level");
        // MockBukkit doesn't implement getLevelDirectory, match worlds go under this temp folder instead
        server = MockBukkit.mock(new ServerMock() {
            @Override
            public Path getLevelDirectory() {
                return level;
            }
        });
        server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin();
        Executor mainThread = task -> server.getScheduler().runTask(plugin, task);

        writeMap("test_game");
        writeMap("broken_game");
        MapRegistry maps = new MapRegistry(temp.resolve("maps"), LOGGER);
        maps.loadAll(Map.of("test_game", LastStandingMap.class, "broken_game", LastStandingMap.class));

        HotbarItems hotbar = new HotbarItems(new ItemTags(plugin));
        states = new PlayerStateService(temp.resolve("snapshots"), LOGGER);
        instancer = new WorldInstancer(plugin, mainThread);
        index = new SessionIndex();
        SessionServices services = new SessionServices(
            states, index, maps, instancer, new Lobby(plugin, hotbar), hotbar, mainThread, RandomGenerator.getDefault(), LOGGER);
        sessions = new SessionManager(services, new Router(plugin, index));
        testGame = gameType("test_game", false);
    }

    @AfterEach
    void tearDown() {
        instancer.shutdown();
        states.shutdown();
        MockBukkit.unmock();
    }

    private void writeMap(String gameId) throws IOException {
        Path folder = temp.resolve("maps").resolve(gameId).resolve("arena");
        Files.createDirectories(folder.resolve("world").resolve("region"));
        Files.writeString(folder.resolve("map.yml"), MAP_YML);
    }

    private GameType gameType(String id, boolean failOnStart) {
        return new GameType(id, Component.text(id), Material.STONE, 2, 3, LastStandingMap.class,
            session -> new RecordingGame(session, calls, failOnStart));
    }

    private PlayerMock playerWithDiamonds(String name) {
        PlayerMock player = server.addPlayer(name);
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 3));
        player.setGameMode(GameMode.SURVIVAL);
        return player;
    }

    private SessionProcess join(PlayerMock player) {
        return join(player, testGame);
    }

    private SessionProcess join(PlayerMock player, GameType type) {
        assertEquals(JoinResult.JOINED, sessions.joinSession(player, type));
        return index.getSession(player).orElseThrow();
    }

    // runs the session tick and the scheduler, which is where map loads finish on the "main thread"
    private void ticks(int count) {
        for (int i = 0; i < count; i++) {
            sessions.tickAll();
            server.getScheduler().performOneTick();
        }
    }

    // the map copy runs on a real background thread, so keep ticking until the session gets there
    private void tickUntil(SessionProcess session, States state) throws InterruptedException {
        for (int i = 0; i < 200 && session.state() != state; i++) {
            ticks(20);
            Thread.sleep(5);
        }
        assertEquals(state, session.state());
    }

    private void assertRestored(PlayerMock player) {
        assertTrue(player.getInventory().contains(Material.DIAMOND, 3), player.getName() + " lost their diamonds");
        assertFalse(player.getInventory().contains(Material.IRON_SWORD), player.getName() + " kept a game item");
        assertEquals(GameMode.SURVIVAL, player.getGameMode());
        assertTrue(index.getSession(player).isEmpty(), player.getName() + " is still mapped to a session");
    }

    @Test
    void joiningCapturesStateBeforeTheGameHandsOutItems() {
        PlayerMock alice = playerWithDiamonds("Alice");

        SessionProcess session = join(alice);

        assertEquals(States.WAITING, session.state());
        assertSame(session, index.getSession(alice).orElseThrow());
        assertTrue(alice.getInventory().contains(Material.IRON_SWORD));
        assertFalse(alice.getInventory().contains(Material.DIAMOND));
        assertEquals(GameMode.ADVENTURE, alice.getGameMode());
        assertEquals(List.of("setup", "join:Alice"), calls);
    }

    @Test
    void aPlayerCannotBeInTwoSessions() {
        PlayerMock alice = playerWithDiamonds("Alice");
        join(alice);

        assertEquals(JoinResult.ALREADY_IN_GAME, sessions.joinSession(alice, testGame));
        assertEquals(1, sessions.getActiveSessions().size());
    }

    @Test
    void aGameWithoutMapsCannotBeJoined() {
        PlayerMock alice = playerWithDiamonds("Alice");

        assertEquals(JoinResult.NO_MAPS, sessions.joinSession(alice, gameType("no_maps", false)));
        assertTrue(sessions.getActiveSessions().isEmpty());
        assertTrue(alice.getInventory().contains(Material.DIAMOND, 3));
    }

    @Test
    void reachingTheMinimumStartsTheCountdownAndTheGame() throws InterruptedException {
        SessionProcess session = join(playerWithDiamonds("Alice"));
        join(playerWithDiamonds("Bob"));
        assertEquals(States.STARTING, session.state());

        ticks(COUNTDOWN_TICKS);
        tickUntil(session, States.ACTIVE);

        assertTrue(calls.contains("start"));
        assertTrue(session.vote().isClosed());
        assertEquals("Test Arena", session.map().info().name());
    }

    @Test
    void forceStartSkipsTheCountdown() throws InterruptedException {
        PlayerMock alice = playerWithDiamonds("Alice");
        SessionProcess session = join(alice);

        assertTrue(sessions.forceStart(session));
        tickUntil(session, States.ACTIVE);

        assertSame(session.map().world(), alice.getWorld());
        assertFalse(sessions.forceStart(session));
    }

    @Test
    void aSessionCountingDownTakesNoNewPlayers() {
        SessionProcess first = join(playerWithDiamonds("Alice"));
        join(playerWithDiamonds("Bob"));

        SessionProcess second = join(playerWithDiamonds("Carol"));

        assertNotSame(first, second);
        assertEquals(2, sessions.getActiveSessions().size());
    }

    @Test
    void leavingRestoresThePlayerAfterTheGameHookRan() {
        PlayerMock alice = playerWithDiamonds("Alice");
        join(alice);
        join(playerWithDiamonds("Bob"));

        sessions.leaveSession(alice);

        assertRestored(alice);
        assertTrue(calls.contains("leave:Alice:sword=true"), "the leave hook should still see the game items");
    }

    @Test
    void leavingDuringTheCountdownCancelsIt() {
        SessionProcess session = join(playerWithDiamonds("Alice"));
        PlayerMock bob = playerWithDiamonds("Bob");
        join(bob);

        sessions.leaveSession(bob);

        assertEquals(States.WAITING, session.state());
    }

    @Test
    void theLastPlayerLeavingDisposesTheSession() {
        PlayerMock alice = playerWithDiamonds("Alice");
        SessionProcess session = join(alice);

        sessions.leaveSession(alice);

        assertEquals(States.DISPOSED, session.state());
        assertTrue(sessions.getActiveSessions().isEmpty());
        assertTrue(calls.contains("dispose"));
        assertRestored(alice);
    }

    @Test
    void endingRestoresEveryoneAndReleasesTheMapWorld() throws InterruptedException {
        PlayerMock alice = playerWithDiamonds("Alice");
        PlayerMock bob = playerWithDiamonds("Bob");
        SessionProcess session = join(alice);
        join(bob);
        tickUntil(session, States.ACTIVE);
        World mapWorld = session.map().world();

        session.end(alice);
        assertEquals(States.ENDING, session.state());
        ticks(ENDING_TICKS);

        assertEquals(States.DISPOSED, session.state());
        assertTrue(sessions.getActiveSessions().isEmpty());
        assertRestored(alice);
        assertRestored(bob);
        assertTrue(calls.containsAll(List.of("end", "dispose")));
        assertTrue(index.getSession(mapWorld).isEmpty());
        assertTrue(server.getWorlds().stream().noneMatch(world -> world.getUID().equals(mapWorld.getUID())));
    }

    @Test
    void aCrashingGameOnlyTakesDownItsOwnSession() throws InterruptedException {
        GameType brokenGame = gameType("broken_game", true);
        PlayerMock alice = playerWithDiamonds("Alice");
        PlayerMock bob = playerWithDiamonds("Bob");
        PlayerMock carol = playerWithDiamonds("Carol");
        SessionProcess broken = join(alice, brokenGame);
        join(bob, brokenGame);
        SessionProcess healthy = join(carol);

        tickUntil(broken, States.DISPOSED);

        assertRestored(alice);
        assertRestored(bob);
        assertEquals(States.WAITING, healthy.state());
        assertSame(healthy, index.getSession(carol).orElseThrow());
    }

    @Test
    void eventsOnlyReachTheSessionOfThePlayer() {
        PlayerMock alice = playerWithDiamonds("Alice");
        PlayerMock outsider = playerWithDiamonds("Eve");
        join(alice);

        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(alice, true));
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(outsider, true));

        assertTrue(calls.contains("sneak:Alice"));
        assertFalse(calls.contains("sneak:Eve"));
    }

    @Test
    void eventsWithoutAPlayerReachTheSessionThatOwnsTheWorld() throws InterruptedException {
        SessionProcess session = join(playerWithDiamonds("Alice"));
        sessions.forceStart(session);
        tickUntil(session, States.ACTIVE);
        World mapWorld = session.map().world();

        server.getPluginManager().callEvent(new WorldSaveEvent(mapWorld));
        server.getPluginManager().callEvent(new WorldSaveEvent(server.getWorld("world")));

        assertTrue(calls.contains("save:" + mapWorld.getName()));
        assertFalse(calls.contains("save:world"));
    }

    @Test
    void quittingTheServerCountsAsLeaving() {
        server.getPluginManager().registerEvents(new PlayerConnectionListener(states, sessions), plugin);
        PlayerMock alice = playerWithDiamonds("Alice");
        join(alice);
        join(playerWithDiamonds("Bob"));

        alice.disconnect();

        assertRestored(alice);
    }

    @Test
    void disposingEverythingRestoresEveryone() {
        PlayerMock alice = playerWithDiamonds("Alice");
        PlayerMock bob = playerWithDiamonds("Bob");
        join(alice);
        join(bob);

        sessions.disposeAll();

        assertRestored(alice);
        assertRestored(bob);
        assertTrue(sessions.getActiveSessions().isEmpty());
    }
}
