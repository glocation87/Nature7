package io.github.glocation87.nature7.player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

public final class PlayerStateService {

    private final Path directory;
    private final Logger logger;
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    // One thread keeps writes and deletes for the same player in order
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Nature7-IO"));

    public PlayerStateService(Path directory, Logger logger) {
        this.directory = directory;
        this.logger = logger;
    }

    public boolean isCaptured(Player player) {
        return snapshots.containsKey(player.getUniqueId());
    }

    public void capture(Player player) {
        if (isCaptured(player)) {
            // A second capture would overwrite the real state with game state
            logger.warning(player.getName() + " was captured twice, keeping the first snapshot");
            return;
        }
        PlayerSnapshot snapshot = PlayerSnapshot.capture(player);
        snapshots.put(player.getUniqueId(), snapshot);
        String data = snapshot.serialize();
        Path file = fileOf(player.getUniqueId());
        io.execute(() -> write(file, data));
        reset(player);
    }

    public void restore(Player player) {
        PlayerSnapshot snapshot = snapshots.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        snapshot.apply(player);
        Path file = fileOf(player.getUniqueId());
        io.execute(() -> delete(file));
    }

    public void restoreFromDisk(Player player) {
        Path file = fileOf(player.getUniqueId());
        if (!Files.exists(file)) {
            return;
        }
        try {
            PlayerSnapshot.deserialize(Files.readString(file)).apply(player);
            Files.delete(file);
            logger.info("Restored " + player.getName() + " from a snapshot left by an unclean shutdown");
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Could not restore snapshot for " + player.getName() + ", file kept at " + file, e);
        }
    }

    public void shutdown() {
        io.shutdown();
        try {
            if (!io.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Snapshot writes did not finish in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void reset(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(PlayerSnapshot.maxHealth(player));
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setLevel(0);
        player.setExp(0.0f);
        player.setFireTicks(0);
        player.setFallDistance(0.0f);
        player.clearActivePotionEffects();
    }

    private Path fileOf(UUID uuid) {
        return directory.resolve(uuid + ".yml");
    }

    private void write(Path file, String data) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, data);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not write snapshot " + file, e);
        }
    }

    private void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not delete snapshot " + file, e);
        }
    }
}
