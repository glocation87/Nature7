package io.github.glocation87.nature7.map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

// takes game id -> schema instead of the GameRegistry so the map package doesn't depend on engine
public final class MapRegistry {
    private final Path directory;
    private final Logger logger;
    private final Map<String, List<GameMap>> byGame = new HashMap<>();
    private Map<String, Class<?>> schemas = Map.of();

    public MapRegistry(Path directory, Logger logger) {
        this.directory = directory;
        this.logger = logger;
    }

    public Path directory() {
        return directory;
    }

    public void reload() {
        loadAll(Map.copyOf(schemas));
    }

    public void loadAll(Map<String, Class<?>> schemas) {
        this.schemas = Map.copyOf(schemas);
        byGame.clear();
        for (Map.Entry<String, Class<?>> entry : schemas.entrySet()) {
            String gameId = entry.getKey();
            List<GameMap> maps = new ArrayList<>();
            for (Path folder : mapFolders(directory.resolve(gameId))) {
                try {
                    maps.add(MapLoader.load(folder, gameId, entry.getValue()));
                } catch (MapLoadException e) {
                    logger.severe("Skipping map " + gameId + "/" + folder.getFileName() + ": " + e.getMessage());
                }
            }
            byGame.put(gameId, List.copyOf(maps));
            if (maps.isEmpty()) {
                logger.warning("No playable maps for " + gameId + ", add some to " + directory.resolve(gameId));
            } else {
                logger.info("Loaded " + maps.size() + " map(s) for " + gameId);
            }
        }
    }

    public List<GameMap> mapsFor(String gameId) {
        return byGame.getOrDefault(gameId, List.of());
    }

    private List<Path> mapFolders(Path gameDirectory) {
        try {
            Files.createDirectories(gameDirectory);
            // Files.list holds a directory handle open, on windows that locks the folder until it's closed
            try (Stream<Path> children = Files.list(gameDirectory)) {
                return children.filter(Files::isDirectory).sorted().toList();
            }
        } catch (IOException e) {
            logger.severe("Could not read " + gameDirectory + ": " + e.getMessage());
            return List.of();
        }
    }
}
