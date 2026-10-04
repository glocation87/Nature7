package io.github.glocation87.nature7.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// real sqlite in a temp folder, catches wrong sql and a broken upsert where a mock wouldn't
class StatsServiceTest {

    @TempDir
    Path temp;

    private StatsService stats;
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @BeforeEach
    void open() {
        stats = new StatsService(temp.resolve("stats.db"), Logger.getLogger("test"));
        stats.open().join();
    }

    @AfterEach
    void close() {
        stats.shutdown();
    }

    @Test
    void unknownPlayersHaveNoStats() {
        assertTrue(stats.load(alice).join().isEmpty());
    }

    @Test
    void storesAMatch() {
        stats.add("skywars", List.of(
            new StatsDelta(alice, 1, 1, 3, 0),
            new StatsDelta(bob, 1, 0, 0, 1))).join();

        assertEquals(new PlayerStats(1, 1, 3, 0), stats.load(alice).join().get("skywars"));
        assertEquals(new PlayerStats(1, 0, 0, 1), stats.load(bob).join().get("skywars"));
    }

    @Test
    void addsUpAcrossMatches() {
        stats.add("skywars", List.of(new StatsDelta(alice, 1, 1, 3, 0))).join();
        stats.add("skywars", List.of(new StatsDelta(alice, 1, 0, 1, 1))).join();

        assertEquals(new PlayerStats(2, 1, 4, 1), stats.load(alice).join().get("skywars"));
    }

    @Test
    void keepsGamesSeparate() {
        stats.add("skywars", List.of(new StatsDelta(alice, 1, 1, 0, 0))).join();
        stats.add("spleef", List.of(new StatsDelta(alice, 1, 0, 0, 0))).join();

        Map<String, PlayerStats> loaded = stats.load(alice).join();
        assertEquals(2, loaded.size());
        assertEquals(1, loaded.get("skywars").wins());
        assertEquals(0, loaded.get("spleef").wins());
    }

    @Test
    void storesTextLiterallyThanksToParameters() {
        String hostile = "x'); DROP TABLE player_stats; --";
        stats.add(hostile, List.of(new StatsDelta(alice, 1, 0, 0, 0))).join();

        assertTrue(stats.load(alice).join().containsKey(hostile));
    }

    @Test
    void survivesAReopen() {
        stats.add("skywars", List.of(new StatsDelta(alice, 1, 1, 0, 0))).join();
        stats.shutdown();

        stats = new StatsService(temp.resolve("stats.db"), Logger.getLogger("test"));
        stats.open().join();

        assertEquals(1, stats.load(alice).join().get("skywars").wins());
    }

    @Test
    void killDeathRatioHandlesNoDeaths() {
        assertEquals(3.0, new PlayerStats(1, 0, 3, 0).killDeathRatio());
        assertEquals(1.5, new PlayerStats(1, 0, 3, 2).killDeathRatio());
    }
}
