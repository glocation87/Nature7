package io.github.glocation87.nature7.games;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.glocation87.nature7.games.ctf.CaptureTheFlagMap;
import io.github.glocation87.nature7.games.skywars.SkyWarsMap;
import io.github.glocation87.nature7.games.spleef.SpleefMap;
import io.github.glocation87.nature7.map.GameMap;
import io.github.glocation87.nature7.map.MapLoadException;
import io.github.glocation87.nature7.map.MapLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// the game: section of map.yml for each new game, this is the contract lms-maps exports have to meet
class GameMapsTest {

    private static final String HEADER = """
        name: Test
        spectator-spawn: {x: 0.5, y: 80, z: 0.5}
        bounds:
          min: {x: -50, y: 0, z: -50}
          max: {x: 50, y: 128, z: 50}
        """;

    @TempDir
    Path temp;

    private GameMap load(String gameId, Class<?> schema, String game) throws IOException, MapLoadException {
        Path folder = temp.resolve(gameId);
        Files.createDirectories(folder.resolve("world").resolve("region"));
        Files.writeString(folder.resolve("map.yml"), HEADER + game);
        return MapLoader.load(folder, gameId, schema);
    }

    @Test
    void spleefNeedsTwoSpawns() throws Exception {
        GameMap map = load("spleef", SpleefMap.class, """
            game:
              spawns: [{x: 1, y: 65, z: 1}, {x: -1, y: 65, z: -1}]
            """);
        assertEquals(2, map.data(SpleefMap.class).spawns().size());

        assertThrows(MapLoadException.class, () -> load("spleef", SpleefMap.class, """
            game:
              spawns: [{x: 1, y: 65, z: 1}]
            """));
    }

    @Test
    void skywarsChestsAreOptionalButTheCentreIsnt() throws Exception {
        GameMap map = load("skywars", SkyWarsMap.class, """
            game:
              spawns: [{x: 20, y: 65, z: 0}, {x: -20, y: 65, z: 0}]
              center: {x: 0, y: 70, z: 0}
            """);
        SkyWarsMap data = map.data(SkyWarsMap.class);
        assertTrue(data.islandChests().isEmpty());
        assertTrue(data.centerChests().isEmpty());

        MapLoadException error = assertThrows(MapLoadException.class, () -> load("skywars", SkyWarsMap.class, """
            game:
              spawns: [{x: 20, y: 65, z: 0}, {x: -20, y: 65, z: 0}]
            """));
        assertTrue(error.getMessage().contains("center"), error.getMessage());
    }

    @Test
    void captureTheFlagReadsKebabCaseKeys() throws Exception {
        GameMap map = load("capture_the_flag", CaptureTheFlagMap.class, """
            game:
              red-spawns: [{x: 0, y: 65, z: 20}]
              blue-spawns: [{x: 0, y: 65, z: -20}]
              red-flag: {x: 0, y: 65, z: 25}
              blue-flag: {x: 0, y: 65, z: -25}
            """);
        assertEquals(25, map.data(CaptureTheFlagMap.class).redFlag().z());

        assertThrows(MapLoadException.class, () -> load("capture_the_flag", CaptureTheFlagMap.class, """
            game:
              red-spawns: [{x: 0, y: 65, z: 20}]
              blue-spawns: []
              red-flag: {x: 0, y: 65, z: 25}
              blue-flag: {x: 0, y: 65, z: -25}
            """));
    }
}
