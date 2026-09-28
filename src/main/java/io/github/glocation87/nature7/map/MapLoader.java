package io.github.glocation87.nature7.map;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class MapLoader {
    public static final String MAP_FILE = "map.yml";
    public static final String GAME_NODE = "game";

    private MapLoader() {
    }

    public static GameMap load(Path folder, String gameId, Class<?> schema) throws MapLoadException {
        Path file = folder.resolve(MAP_FILE);
        if (!Files.isRegularFile(file)) {
            throw new MapLoadException("Missing " + MAP_FILE);
        }
        Path worldData = worldData(folder);
        try {
            CommentedConfigurationNode root = yaml(file).load();
            return parse(root, folder.getFileName().toString(), gameId, schema, worldData);
        } catch (ConfigurateException e) {
            throw new MapLoadException(e.getMessage(), e);
        }
    }

    public static GameMap parse(CommentedConfigurationNode root, String mapId, String gameId, Class<?> schema, Path worldData)
        throws MapLoadException {
        try {
            MapInfo info = root.get(MapInfo.class);
            if (info == null) {
                throw new MapLoadException("map.yml is empty");
            }
            Object data = root.node(GAME_NODE).get(schema);
            if (data == null) {
                throw new MapLoadException("map.yml has no " + GAME_NODE + " section");
            }
            return new GameMap(mapId, gameId, info, data, worldData);
        } catch (ConfigurateException e) {
            throw new MapLoadException(describe(e), e);
        }
    }

    // Configurate wraps errors thrown by record constructors, dig down to the message that explains the problem
    private static String describe(ConfigurateException error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String reason = root == error ? error.rawMessage() : root.getMessage();
        List<String> path = new ArrayList<>();
        error.path().forEach(part -> path.add(String.valueOf(part)));
        return (path.isEmpty() ? "map.yml" : String.join(".", path)) + ": " + reason;
    }

    // old saves keep region/ at the top, 26.1+ saves nest it under dimensions/minecraft/overworld
    public static Path worldData(Path folder) throws MapLoadException {
        Path world = folder.resolve("world");
        if (Files.isDirectory(world.resolve("region"))) {
            return world;
        }
        Path modern = world.resolve("dimensions").resolve("minecraft").resolve("overworld");
        if (Files.isDirectory(modern.resolve("region"))) {
            return modern;
        }
        throw new MapLoadException("No world data, expected " + world.resolve("region"));
    }

    public static YamlConfigurationLoader yaml(Path file) {
        return YamlConfigurationLoader.builder()
            .path(file)
            .nodeStyle(NodeStyle.BLOCK)
            .indent(2)
            // without this a missing section is silently filled with zeros instead of failing
            .defaultOptions(options -> options.implicitInitialization(false))
            .build();
    }
}
