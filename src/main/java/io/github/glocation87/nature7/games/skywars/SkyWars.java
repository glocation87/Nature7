package io.github.glocation87.nature7.games.skywars;

import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.engine.States;
import io.github.glocation87.nature7.games.GameResources;
import io.github.glocation87.nature7.loot.LootTable;
import io.github.glocation87.nature7.map.Point;
import io.github.glocation87.nature7.module.DeathModule;
import io.github.glocation87.nature7.module.KitModule;
import io.github.glocation87.nature7.module.SpectatorModule;
import io.github.glocation87.nature7.module.StatsModule;
import io.github.glocation87.nature7.module.Timeline.Milestone;
import io.github.glocation87.nature7.module.TimelineModule;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.world.MapInstance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

// loot your island, fight for the centre, refill at 3:00, deathmatch at 5:00, draw at 8:00
public final class SkyWars extends MinigameProcess {
    private static final int ISLAND_ROLLS = 6;
    private static final int CENTER_ROLLS = 9;
    private static final double DEATHMATCH_BORDER = 60;
    private static final double FINAL_BORDER = 10;
    private static final long BORDER_SHRINK_TICKS = 60 * 20;

    public static GameType type(GameResources resources) {
        return new GameType(
            "skywars",
            Component.text("SkyWars", NamedTextColor.YELLOW),
            Material.ENDER_EYE,
            2,
            12,
            SkyWarsMap.class,
            session -> new SkyWars(session, resources)
        );
    }

    private final DeathModule deaths;

    private SkyWars(SessionProcess session, GameResources resources) {
        super(session);
        install(new SpectatorModule(resources.plugin(), resources.tags()));
        install(new KitModule(resources.kits().kitsFor("skywars"), resources.tags()));
        // dropping items on death is the whole point, killing someone is how you get their gear
        deaths = install(DeathModule.builder()
            .dropItems(true)
            .onEliminated(player -> checkForWinner())
            .build());
        install(new StatsModule(resources.stats()));
        install(new TimelineModule(List.of(
            new Milestone("Chest refill", 180, this::refillChests),
            new Milestone("Deathmatch", 300, this::startDeathmatch),
            new Milestone("Draw", 480, () -> session.end(null)))));
    }

    @Override
    protected void onStart() {
        MapInstance map = session.map();
        List<Point> spawns = map.data(SkyWarsMap.class).spawns();
        List<Player> players = session.players();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.teleport(map.location(spawns.get(i % spawns.size())));
            player.setGameMode(GameMode.SURVIVAL);
        }
        fillChests();
    }

    @Override
    protected void onPlayerLeave(UUID playerId) {
        checkForWinner();
    }

    @Override
    protected List<Component> sidebar() {
        return List.of(Component.text("Alive: ", NamedTextColor.GRAY)
            .append(Component.text(deaths.alivePlayers().size(), NamedTextColor.WHITE)));
    }

    private void fillChests() {
        MapInstance map = session.map();
        SkyWarsMap data = map.data(SkyWarsMap.class);
        for (Point point : data.islandChests()) {
            fill(map.location(point), SkyWarsLoot.ISLAND, ISLAND_ROLLS);
        }
        for (Point point : data.centerChests()) {
            fill(map.location(point), SkyWarsLoot.CENTER, CENTER_ROLLS);
        }
    }

    private void refillChests() {
        fillChests();
        session.broadcastMessage(Component.text("All chests have been refilled", NamedTextColor.GOLD));
    }

    private void fill(Location location, LootTable table, int rolls) {
        Block block = location.getBlock();
        // a builder forgetting a chest shouldn't break the game, just put one there
        if (block.getType() != Material.CHEST) {
            block.setType(Material.CHEST);
        }
        if (!(block.getState() instanceof Chest chest)) {
            return;
        }
        Inventory inventory = chest.getBlockInventory();
        inventory.clear();
        RandomGenerator random = session.random();
        // shuffled slots so the loot is scattered around the chest instead of packed into the top row
        List<Integer> slots = new ArrayList<>(IntStream.range(0, inventory.getSize()).boxed().toList());
        Collections.shuffle(slots, random);
        List<ItemStack> items = table.items(random, rolls);
        for (int i = 0; i < items.size() && i < slots.size(); i++) {
            inventory.setItem(slots.get(i), items.get(i));
        }
    }

    // the border lives in the match world, which is deleted afterwards, so there's nothing to reset
    private void startDeathmatch() {
        MapInstance map = session.map();
        Location center = map.location(map.data(SkyWarsMap.class).center());
        for (Player player : deaths.alivePlayers()) {
            player.teleport(center);
        }
        WorldBorder border = map.world().getWorldBorder();
        border.setCenter(center);
        border.setSize(DEATHMATCH_BORDER);
        border.changeSize(FINAL_BORDER, BORDER_SHRINK_TICKS);
        session.broadcastMessage(Component.text("Deathmatch! The border is closing in", NamedTextColor.RED));
    }

    private void checkForWinner() {
        List<Player> alive = deaths.alivePlayers();
        if (session.state() == States.ACTIVE && alive.size() <= 1) {
            session.end(alive.isEmpty() ? null : alive.getFirst());
        }
    }
}
