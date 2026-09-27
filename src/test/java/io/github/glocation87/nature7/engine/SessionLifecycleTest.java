package io.github.glocation87.nature7.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.glocation87.nature7.player.PlayerConnectionListener;
import io.github.glocation87.nature7.player.PlayerStateService;
import io.github.glocation87.nature7.types.GameType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Bukkit;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

// Drives the real SessionManager, SessionProcess, SessionIndex, StateManager, Router and
// PlayerStateService together, with a small game that records every hook the engine calls
class SessionLifecycleTest {

    private static final Logger LOGGER = Logger.getLogger("test");
    private static final int COUNTDOWN_TICKS = 10 * 20;
    private static final int ENDING_TICKS = 5 * 20;

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
    private SessionIndex index;
    private SessionManager sessions;
    private final List<String> calls = new ArrayList<>();
    private GameType testGame;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        states = new PlayerStateService(temp, LOGGER);
        index = new SessionIndex();
        sessions = new SessionManager(index, new Router(plugin, index), states, LOGGER);
        testGame = gameType("test_game", false);
    }

    @AfterEach
    void tearDown() {
        states.shutdown();
        MockBukkit.unmock();
    }

    private GameType gameType(String id, boolean failOnStart) {
        return new GameType(id, Component.text(id), Material.STONE, 2, 3,
                session -> new RecordingGame(session, calls, failOnStart));
    }

    private PlayerMock playerWithDiamonds(String name) {
        PlayerMock player = server.addPlayer(name);
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 3));
        player.setGameMode(GameMode.SURVIVAL);
        return player;
    }

    private SessionProcess join(PlayerMock player) {
        return sessions.joinSession(player, testGame).orElseThrow();
    }

    private void ticks(int count) {
        for (int i = 0; i < count; i++) {
            sessions.tickAll();
        }
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

        assertTrue(sessions.joinSession(alice, testGame).isEmpty());
        assertEquals(1, sessions.getActiveSessions().size());
    }

    @Test
    void reachingTheMinimumStartsTheCountdownAndTheGame() {
        SessionProcess session = join(playerWithDiamonds("Alice"));
        join(playerWithDiamonds("Bob"));
        assertEquals(States.STARTING, session.state());

        ticks(COUNTDOWN_TICKS);

        assertEquals(States.ACTIVE, session.state());
        assertTrue(calls.contains("start"));
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
    void endingRestoresEveryoneAfterTheDelay() {
        PlayerMock alice = playerWithDiamonds("Alice");
        PlayerMock bob = playerWithDiamonds("Bob");
        SessionProcess session = join(alice);
        join(bob);
        ticks(COUNTDOWN_TICKS);

        session.end(alice);
        assertEquals(States.ENDING, session.state());
        ticks(ENDING_TICKS);

        assertEquals(States.DISPOSED, session.state());
        assertTrue(sessions.getActiveSessions().isEmpty());
        assertRestored(alice);
        assertRestored(bob);
        assertTrue(calls.containsAll(List.of("end", "dispose")));
    }

    @Test
    void aCrashingGameOnlyTakesDownItsOwnSession() {
        GameType brokenGame = gameType("broken_game", true);
        PlayerMock alice = playerWithDiamonds("Alice");
        PlayerMock bob = playerWithDiamonds("Bob");
        PlayerMock carol = playerWithDiamonds("Carol");
        SessionProcess broken = sessions.joinSession(alice, brokenGame).orElseThrow();
        sessions.joinSession(bob, brokenGame).orElseThrow();
        SessionProcess healthy = join(carol);

        ticks(COUNTDOWN_TICKS);

        assertEquals(States.DISPOSED, broken.state());
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
