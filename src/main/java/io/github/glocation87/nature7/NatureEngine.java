/*
* @author w00d7_/glocation87
 */
package io.github.glocation87.nature7;

import io.github.glocation87.nature7.command.NatureCommand;
import io.github.glocation87.nature7.engine.GameRegistry;
import io.github.glocation87.nature7.engine.Router;
import io.github.glocation87.nature7.engine.SessionIndex;
import io.github.glocation87.nature7.engine.SessionManager;
import io.github.glocation87.nature7.engine.SessionServices;
import io.github.glocation87.nature7.games.GameResources;
import io.github.glocation87.nature7.games.laststanding.LastStanding;
import io.github.glocation87.nature7.item.HotbarItems;
import io.github.glocation87.nature7.item.ItemTags;
import io.github.glocation87.nature7.kit.KitRegistry;
import io.github.glocation87.nature7.lobby.Lobby;
import io.github.glocation87.nature7.lobby.LobbyListener;
import io.github.glocation87.nature7.map.MapRegistry;
import io.github.glocation87.nature7.player.PlayerConnectionListener;
import io.github.glocation87.nature7.player.PlayerStateService;
import io.github.glocation87.nature7.stats.StatsCommand;
import io.github.glocation87.nature7.stats.StatsService;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.ui.GameSelectorMenu;
import io.github.glocation87.nature7.ui.HotbarListener;
import io.github.glocation87.nature7.ui.MenuListener;
import io.github.glocation87.nature7.world.WorldInstancer;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.random.RandomGenerator;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

// Not final: MockBukkit loads plugins in tests by generating a subclass
public class NatureEngine extends JavaPlugin {
    private static final List<String> DEFAULT_KITS = List.of("kits/skywars.yml", "kits/capture_the_flag.yml");

    private PlayerStateService playerStates;
    private WorldInstancer instancer;
    private StatsService stats;
    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultKits();
        Executor mainThread = task -> getServer().getScheduler().runTask(this, task);

        // blocking is fine before the server is up, and a broken database should stop the plugin loudly
        stats = new StatsService(getDataPath().resolve("stats.db"), getLogger());
        stats.open().join();

        //manual dependency injection sequence more work but worth it lol
        //order matters: leftovers are deleted before anything can make a world, maps load before any session needs one
        ItemTags tags = new ItemTags(this);
        HotbarItems hotbar = new HotbarItems(tags);
        KitRegistry kits = new KitRegistry(getDataPath().resolve("kits"), getLogger());
        GameResources resources = new GameResources(this, tags, kits, stats);

        // The one registry of games, shared with everything that needs to look games up
        GameRegistry registry = new GameRegistry();
        registry.register(LastStanding.type(resources));
        // kits are found by game id, so they load once the games are registered
        kits.loadAll(registry.all().stream().map(GameType::id).toList());

        playerStates = new PlayerStateService(getDataPath().resolve("snapshots"), getLogger());
        instancer = new WorldInstancer(this, mainThread);
        instancer.deleteLeftovers();

        MapRegistry maps = new MapRegistry(getDataPath().resolve("maps"), getLogger());
        maps.loadAll(schemas(registry));

        Lobby lobby = new Lobby(this, hotbar);
        SessionIndex index = new SessionIndex();
        Router router = new Router(this, index);
        SessionServices services = new SessionServices(
            playerStates, index, maps, instancer, lobby, hotbar, mainThread, RandomGenerator.getDefault(), getLogger());
        sessionManager = new SessionManager(services, router);

        GameSelectorMenu gameSelector = new GameSelectorMenu(registry, sessionManager, maps);
        register(
            new PlayerConnectionListener(playerStates, sessionManager),
            new LobbyListener(lobby),
            new MenuListener(this),
            new HotbarListener(tags, gameSelector, sessionManager, index));
        getServer().getScheduler().runTaskTimer(this, sessionManager::tickAll, 1L, 1L);

        NatureCommand command = new NatureCommand(registry, sessionManager, index, lobby,
            new StatsCommand(stats, registry, mainThread));
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.build(), "Join and manage minigames", List.of("n7")));

        getLogger().info("Nature7 enabled");
    }

    // outermost first: sessions queue world deletes and snapshot writes, so the executors shut down last
    // and the database goes very last so stats from a match that just ended still make it to disk
    @Override
    public void onDisable() {
        if (sessionManager != null) {
            sessionManager.disposeAll();
        }
        if (instancer != null) {
            instancer.shutdown();
        }
        if (playerStates != null) {
            playerStates.shutdown();
        }
        if (stats != null) {
            stats.shutdown();
        }
    }

    // admins edit their copy, saveResource warns if asked to write over it so only copy what isn't there
    private void saveDefaultKits() {
        for (String resource : DEFAULT_KITS) {
            if (!Files.exists(getDataPath().resolve(resource))) {
                saveResource(resource, false);
            }
        }
    }

    private void register(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    // the plugin is the one place that knows both engine and map, so the id -> schema translation lives here
    private static Map<String, Class<?>> schemas(GameRegistry registry) {
        Map<String, Class<?>> schemas = new HashMap<>();
        for (GameType type : registry.all()) {
            schemas.put(type.id(), type.mapSchema());
        }
        return schemas;
    }
}
