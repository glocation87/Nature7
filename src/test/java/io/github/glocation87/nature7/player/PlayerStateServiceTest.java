package io.github.glocation87.nature7.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class PlayerStateServiceTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir
    Path temp;

    private ServerMock server;
    private PlayerStateService states;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        states = new PlayerStateService(temp, LOGGER);
        player = server.addPlayer("Alice");
        player.getInventory().setItem(0, ItemStack.of(Material.DIAMOND, 5));
        player.setGameMode(GameMode.SURVIVAL);
        player.setLevel(12);
        player.setHealth(7.0);
    }

    @AfterEach
    void tearDown() {
        states.shutdown();
        MockBukkit.unmock();
    }

    private Path snapshotFile() {
        return temp.resolve(player.getUniqueId() + ".yml");
    }

    private void assertOriginalState() {
        assertEquals(Material.DIAMOND, player.getInventory().getItem(0).getType());
        assertEquals(5, player.getInventory().getItem(0).getAmount());
        assertEquals(GameMode.SURVIVAL, player.getGameMode());
        assertEquals(12, player.getLevel());
        assertEquals(7.0, player.getHealth());
    }

    @Test
    void captureSavesTheStateAndResetsThePlayer() {
        states.capture(player);

        assertTrue(states.isCaptured(player));
        assertTrue(player.getInventory().isEmpty());
        assertEquals(GameMode.ADVENTURE, player.getGameMode());
        assertEquals(0, player.getLevel());
        assertEquals(20.0, player.getHealth());
    }

    @Test
    void restoreGivesEverythingBack() {
        Location before = player.getLocation();
        states.capture(player);
        player.getInventory().addItem(ItemStack.of(Material.IRON_SWORD));
        player.teleport(before.clone().add(100, 0, 0));

        states.restore(player);

        assertOriginalState();
        assertFalse(player.getInventory().contains(Material.IRON_SWORD));
        assertEquals(before.getX(), player.getLocation().getX());
        assertFalse(states.isCaptured(player));
    }

    @Test
    void restoringTwiceDoesNotOverwriteTheInventoryAgain() {
        states.capture(player);
        states.restore(player);
        player.getInventory().addItem(ItemStack.of(Material.STONE));

        states.restore(player);

        assertTrue(player.getInventory().contains(Material.STONE));
        assertEquals(5, player.getInventory().getItem(0).getAmount());
    }

    @Test
    void capturingTwiceKeepsTheFirstSnapshot() {
        states.capture(player);
        player.getInventory().addItem(ItemStack.of(Material.IRON_SWORD));

        states.capture(player);
        states.restore(player);

        assertOriginalState();
        assertFalse(player.getInventory().contains(Material.IRON_SWORD));
    }

    @Test
    void restoringSomeoneWhoWasNeverCapturedChangesNothing() {
        states.restore(player);

        assertOriginalState();
    }

    @Test
    void writesTheSnapshotToDisk() {
        states.capture(player);
        states.shutdown();

        assertTrue(Files.exists(snapshotFile()));
    }

    @Test
    void deletesTheSnapshotAfterRestoring() {
        states.capture(player);
        states.restore(player);
        states.shutdown();

        assertFalse(Files.exists(snapshotFile()));
    }

    @Test
    void recoversFromDiskAfterACrash() {
        states.capture(player);
        // Only the file survives a crash, a new service stands in for the restarted server
        states.shutdown();
        PlayerStateService afterRestart = new PlayerStateService(temp, LOGGER);

        afterRestart.restoreFromDisk(player);
        afterRestart.shutdown();

        assertOriginalState();
        assertFalse(Files.exists(snapshotFile()));
    }

    @Test
    void keepsACorruptSnapshotFileForAnAdmin() throws Exception {
        Files.writeString(snapshotFile(), "this is: [not a snapshot");

        states.restoreFromDisk(player);

        assertTrue(Files.exists(snapshotFile()));
        assertOriginalState();
    }
}
