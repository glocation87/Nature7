package io.github.glocation87.nature7.stats;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sqlite.SQLiteDataSource;

public final class StatsService {
    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS player_stats (
            uuid TEXT NOT NULL,
            game TEXT NOT NULL,
            games INTEGER NOT NULL DEFAULT 0,
            wins INTEGER NOT NULL DEFAULT 0,
            kills INTEGER NOT NULL DEFAULT 0,
            deaths INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY (uuid, game)
        )
        """;

    private static final String ADD = """
        INSERT INTO player_stats (uuid, game, games, wins, kills, deaths)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (uuid, game) DO UPDATE SET
            games = games + excluded.games,
            wins = wins + excluded.wins,
            kills = kills + excluded.kills,
            deaths = deaths + excluded.deaths
        """;

    private static final String LOAD = "SELECT game, games, wins, kills, deaths FROM player_stats WHERE uuid = ?";

    @FunctionalInterface
    private interface SqlTask<T> {
        T run(Connection connection) throws SQLException;
    }

    private final Path file;
    private final Logger logger;
    // sqlite has one writer at a time and a Connection isn't thread safe, so one thread owns it and runs every query
    private final ExecutorService database = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Nature7-Stats"));
    private Connection connection;

    public StatsService(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public CompletableFuture<Void> open() {
        return CompletableFuture.runAsync(() -> {
            try {
                // DataSource and not DriverManager, the driver lives in the plugin class loader where DriverManager can't see it
                SQLiteDataSource source = new SQLiteDataSource();
                source.setUrl("jdbc:sqlite:" + file.toAbsolutePath());
                connection = source.getConnection();
                try (Statement statement = connection.createStatement()) {
                    statement.execute(CREATE_TABLE);
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database);
    }

    // one transaction per match, either every player's row lands or none do
    public CompletableFuture<Void> add(String gameId, List<StatsDelta> deltas) {
        return submit(conn -> {
            conn.setAutoCommit(false);
            try (PreparedStatement statement = conn.prepareStatement(ADD)) {
                for (StatsDelta delta : deltas) {
                    statement.setString(1, delta.player().toString());
                    statement.setString(2, gameId);
                    statement.setInt(3, delta.games());
                    statement.setInt(4, delta.wins());
                    statement.setInt(5, delta.kills());
                    statement.setInt(6, delta.deaths());
                    statement.addBatch();
                }
                statement.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            return null;
        });
    }

    public CompletableFuture<Map<String, PlayerStats>> load(UUID player) {
        return submit(conn -> {
            Map<String, PlayerStats> stats = new HashMap<>();
            try (PreparedStatement statement = conn.prepareStatement(LOAD)) {
                statement.setString(1, player.toString());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        stats.put(rows.getString("game"), new PlayerStats(
                            rows.getInt("games"), rows.getInt("wins"), rows.getInt("kills"), rows.getInt("deaths")));
                    }
                }
            }
            return Map.copyOf(stats);
        });
    }

    // closing is queued behind pending writes, so a match that ends as the server stops still gets saved
    public void shutdown() {
        database.execute(() -> {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Could not close the stats database", e);
            }
        });
        database.shutdown();
        try {
            if (!database.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Stats writes did not finish in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private <T> CompletableFuture<T> submit(SqlTask<T> task) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return task.run(connection);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database);
        future.whenComplete((result, error) -> {
            if (error != null) {
                logger.log(Level.SEVERE, "Stats query failed", error);
            }
        });
        return future;
    }
}
