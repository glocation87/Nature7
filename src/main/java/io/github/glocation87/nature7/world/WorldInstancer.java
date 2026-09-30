package io.github.glocation87.nature7.world;

import io.github.glocation87.nature7.map.GameMap;
import io.github.glocation87.nature7.map.Point;
import io.papermc.paper.math.Position;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

// copies a map into a throwaway world for each match: copy off thread, load on main, delete off thread
public final class WorldInstancer {
    private static final List<String> WORLD_FOLDERS = List.of("region", "entities", "poi");

    private final Plugin plugin;
    private final Executor mainThread;
    private final Logger logger;
    private final Path directory;
    // one thread so operations on the same folder never overlap or run out of order
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Nature7-Worlds"));
    private int nextId;

    public WorldInstancer(Plugin plugin, Executor mainThread) {
        this.plugin = plugin;
        this.mainThread = mainThread;
        this.logger = plugin.getLogger();
        // plugin worlds live at world/dimensions/<namespace>/<key>/ since 26.1
        this.directory = plugin.getServer().getLevelDirectory()
            .resolve("dimensions")
            .resolve(plugin.namespace());
    }

    // every world in our namespace is temporary, anything here at startup was left behind by a crash
    public void deleteLeftovers() {
        try {
            FileTrees.delete(directory);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not delete leftover worlds in " + directory, e);
        }
    }

    public CompletableFuture<MapInstance> create(GameMap map) {
        String name = "instance_" + nextId++;
        Path target = directory.resolve(name);
        Point spawn = map.info().spectatorSpawn();
        return CompletableFuture.runAsync(() -> copyWorldFolders(map.worldData(), target), io)
            .thenApplyAsync(ignored -> new MapInstance(name, map, load(name, spawn)), mainThread);
    }

    public void release(World world) {
        // same folder load() created, found through the key so this doesn't depend on getWorldPath
        Path folder = directory.resolve(world.getKey().getKey());
        Location fallback = plugin.getServer().getWorlds().getFirst().getSpawnLocation();
        for (Player player : world.getPlayers()) {
            player.teleport(fallback);
        }
        if (!plugin.getServer().unloadWorld(world, false)) {
            logger.warning("Could not unload world " + world.getName());
            return;
        }
        io.execute(() -> {
            try {
                FileTrees.delete(folder);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not delete " + folder, e);
            }
        });
    }

    public void shutdown() {
        io.shutdown();
        try {
            if (!io.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warning("World file operations did not finish in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private World load(String name, Point spawn) {
        World world = WorldCreator.ofKey(new NamespacedKey(plugin, name))
            .generator(new VoidGenerator())
            // skips the slow safe spawn search minecraft does for new worlds, ~3s down to ~150ms
            .forcedSpawnPosition(Position.fine(spawn.x(), spawn.y(), spawn.z()), spawn.yaw(), spawn.pitch())
            .createWorld();
        if (world == null) {
            throw new IllegalStateException("Could not load world " + name);
        }
        world.setAutoSave(false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.SPAWN_MONSTERS, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setTime(6000);
        world.setStorm(false);
        return world;
    }

    private static void copyWorldFolders(Path source, Path target) {
        try {
            Files.createDirectories(target);
            for (String name : WORLD_FOLDERS) {
                Path folder = source.resolve(name);
                if (Files.isDirectory(folder)) {
                    FileTrees.copy(folder, target.resolve(name));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // bukkit's default generator methods place nothing, so everything outside the map is empty air
    private static final class VoidGenerator extends ChunkGenerator {
    }
}
