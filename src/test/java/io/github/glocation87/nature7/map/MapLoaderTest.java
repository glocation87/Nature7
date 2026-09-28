package io.github.glocation87.nature7.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.glocation87.nature7.games.laststanding.LastStandingMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapLoaderTest {

    private static final String VALID = """
        name: Colosseum
        authors: [Builder]
        source: https://example.com/colosseum
        license: CC BY 4.0
        spectator-spawn: {x: 0.5, y: 80, z: 0.5, yaw: 90, pitch: 0}
        bounds:
          min: {x: -50, y: 0, z: -50}
          max: {x: 50, y: 128, z: 50}
        game:
          spawns:
            - {x: 10.5, y: 65, z: 0.5, yaw: 90, pitch: 0}
            - {x: -9.5, y: 65, z: 0.5}
        """;

    @TempDir
    Path temp;

    private Path mapFolder(String yaml) throws IOException {
        Path folder = temp.resolve("colosseum");
        Files.createDirectories(folder.resolve("world").resolve("region"));
        Files.writeString(folder.resolve("map.yml"), yaml);
        return folder;
    }

    @Test
    void loadsAValidMap() throws Exception {
        GameMap map = MapLoader.load(mapFolder(VALID), "last_standing", LastStandingMap.class);

        assertEquals("colosseum", map.id());
        assertEquals("Colosseum", map.info().name());
        assertEquals(List.of("Builder"), map.info().authors());
        assertEquals(new Point(0.5, 80, 0.5, 90, 0), map.info().spectatorSpawn());
        assertEquals(2, map.data(LastStandingMap.class).spawns().size());
    }

    @Test
    void yawAndPitchDefaultToZero() throws Exception {
        GameMap map = MapLoader.load(mapFolder(VALID), "last_standing", LastStandingMap.class);

        assertEquals(new Point(-9.5, 65, 0.5, 0, 0), map.data(LastStandingMap.class).spawns().get(1));
    }

    @Test
    void rejectsAMapWithTooFewSpawns() throws IOException {
        String oneSpawn = VALID.replace("    - {x: -9.5, y: 65, z: 0.5}\n", "");
        Path folder = mapFolder(oneSpawn);

        MapLoadException error = assertThrows(MapLoadException.class,
            () -> MapLoader.load(folder, "last_standing", LastStandingMap.class));
        assertTrue(error.getMessage().contains("at least 2 spawns"), error.getMessage());
    }

    @Test
    void rejectsAMapWithoutBounds() throws IOException {
        String noBounds = VALID.replaceAll("bounds:\\n  min: .*\\n  max: .*\\n", "");
        Path folder = mapFolder(noBounds);

        MapLoadException error = assertThrows(MapLoadException.class,
            () -> MapLoader.load(folder, "last_standing", LastStandingMap.class));
        assertTrue(error.getMessage().contains("bounds"), error.getMessage());
    }

    @Test
    void rejectsAPointWithAMissingCoordinate() throws IOException {
        Path folder = mapFolder(VALID.replace("{x: -9.5, y: 65, z: 0.5}", "{x: -9.5, z: 0.5}"));

        MapLoadException error = assertThrows(MapLoadException.class,
            () -> MapLoader.load(folder, "last_standing", LastStandingMap.class));
        assertTrue(error.getMessage().startsWith("game.spawns.1.y"), error.getMessage());
    }

    @Test
    void rejectsAMapWithoutAGameSection() throws IOException {
        Path folder = mapFolder(VALID.substring(0, VALID.indexOf("game:")));

        MapLoadException error = assertThrows(MapLoadException.class,
            () -> MapLoader.load(folder, "last_standing", LastStandingMap.class));
        assertTrue(error.getMessage().contains("game"), error.getMessage());
    }

    @Test
    void rejectsAMapWithoutWorldData() throws IOException {
        Path folder = temp.resolve("empty");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("map.yml"), VALID);

        assertThrows(MapLoadException.class, () -> MapLoader.load(folder, "last_standing", LastStandingMap.class));
    }

    @Test
    void findsWorldDataInTheNewSaveLayout() throws Exception {
        Path folder = temp.resolve("modern");
        Path overworld = folder.resolve("world").resolve("dimensions").resolve("minecraft").resolve("overworld");
        Files.createDirectories(overworld.resolve("region"));

        assertEquals(overworld, MapLoader.worldData(folder));
    }

    @Test
    void loadsWhatLmsMapsExports() throws Exception {
        // same shape /lmsmap export writes, block style with explicit yaw and pitch
        String exported = """
            name: Colosseum 4242
            authors:
            - LmsMaps
            spectator-spawn:
              x: 0.5
              y: 88.0
              z: 0.5
              yaw: 0.0
              pitch: 90.0
            bounds:
              min:
                x: -58
                y: 30
                z: -58
              max:
                x: 58
                y: 90
                z: 58
            game:
              spawns:
              - x: 32.5
                y: 65.0
                z: 9.5
                yaw: 105.70864
                pitch: 0.0
              - x: 23.5
                y: 65.0
                z: 23.5
                yaw: 135.0
                pitch: 0.0
            """;

        GameMap map = MapLoader.load(mapFolder(exported), "last_standing", LastStandingMap.class);

        assertEquals("Colosseum 4242 by LmsMaps", map.info().credit());
        assertTrue(map.info().bounds().contains(32.5, 65, 9.5));
        assertEquals(2, map.data(LastStandingMap.class).spawns().size());
    }
}
