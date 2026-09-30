package io.github.glocation87.nature7;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

// Smoke test for the composition root: every dependency in onEnable is built and wired without throwing
class NatureEngineTest {

    @TempDir
    Path temp;

    private ServerMock server;

    @BeforeEach
    void setUp() {
        // MockBukkit doesn't implement getLevelDirectory, which the world instancer needs
        server = MockBukkit.mock(new ServerMock() {
            @Override
            public Path getLevelDirectory() {
                return temp;
            }
        });
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void enablesTicksAndDisablesCleanly() {
        NatureEngine plugin = MockBukkit.load(NatureEngine.class);
        assertTrue(plugin.isEnabled());

        server.addPlayer("Alice");
        server.getScheduler().performTicks(40);

        server.getPluginManager().disablePlugin(plugin);
        assertFalse(plugin.isEnabled());
    }
}
