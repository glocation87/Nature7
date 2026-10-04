package io.github.glocation87.nature7.module;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DeathModuleTest {

    @Test
    void oneLifeNeedsNoRespawnPoint() {
        assertDoesNotThrow(() -> DeathModule.builder().build());
    }

    @Test
    void moreLivesNeedARespawnPoint() {
        assertThrows(IllegalStateException.class, () -> DeathModule.builder().lives(3).build());
        assertThrows(IllegalStateException.class, () -> DeathModule.builder().unlimitedLives().build());
    }

    @Test
    void livesStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> DeathModule.builder().lives(0));
    }
}
