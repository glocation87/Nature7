package io.github.glocation87.nature7.kit;

import io.github.glocation87.nature7.map.MapLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

// kits/<game>.yml, one broken kit only costs that kit
public final class KitRegistry {
    private final Path directory;
    private final Logger logger;
    private final Map<String, List<Kit>> byGame = new HashMap<>();

    public KitRegistry(Path directory, Logger logger) {
        this.directory = directory;
        this.logger = logger;
    }

    public void loadAll(Collection<String> gameIds) {
        byGame.clear();
        for (String gameId : gameIds) {
            Path file = directory.resolve(gameId + ".yml");
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                List<Kit> kits = parse(MapLoader.yaml(file).load(),
                    error -> logger.severe("Skipping kit in " + file.getFileName() + ": " + error));
                byGame.put(gameId, validated(kits, file));
                logger.info("Loaded " + byGame.get(gameId).size() + " kit(s) for " + gameId);
            } catch (ConfigurateException e) {
                logger.severe("Could not read " + file + ": " + e.getMessage());
            }
        }
    }

    public List<Kit> kitsFor(String gameId) {
        return byGame.getOrDefault(gameId, List.of());
    }

    // no server needed, errors go to the callback so tests can just collect them
    public static List<Kit> parse(ConfigurationNode root, Consumer<String> errors) {
        List<Kit> kits = new ArrayList<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.node("kits").childrenMap().entrySet()) {
            try {
                Kit kit = entry.getValue().get(Kit.class);
                if (kit == null) {
                    errors.accept(entry.getKey() + ": empty kit");
                } else {
                    kits.add(kit);
                }
            } catch (ConfigurateException e) {
                errors.accept(entry.getKey() + ": " + rootMessage(e));
            }
        }
        return List.copyOf(kits);
    }

    private List<Kit> validated(List<Kit> kits, Path file) {
        List<Kit> valid = new ArrayList<>();
        for (Kit kit : kits) {
            try {
                kit.validate();
                valid.add(kit);
            } catch (IllegalArgumentException e) {
                logger.severe("Skipping kit " + kit.name() + " in " + file.getFileName() + ": " + e.getMessage());
            }
        }
        return List.copyOf(valid);
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
