/*
* @author w00d7_/glocation87
 */
package io.github.glocation87.nature7;

import io.github.glocation87.nature7.command.NatureCommand;
import io.github.glocation87.nature7.engine.GameRegistry;
import io.github.glocation87.nature7.engine.Router;
import io.github.glocation87.nature7.engine.SessionIndex;
import io.github.glocation87.nature7.engine.SessionManager;
import io.github.glocation87.nature7.player.PlayerConnectionListener;
import io.github.glocation87.nature7.player.PlayerStateService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

// Not final: MockBukkit loads plugins in tests by generating a subclass
public class NatureEngine extends JavaPlugin {
    private PlayerStateService playerStates;
    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        // The one registry of games, shared with everything that needs to look games up
        GameRegistry registry = new GameRegistry();
        //TODO register minigames

        //manual dependency injection sequence more work but worth it lol
        playerStates = new PlayerStateService(getDataPath().resolve("snapshots"), getLogger());
        SessionIndex index = new SessionIndex();
        Router router = new Router(this, index);
        sessionManager = new SessionManager(index, router, playerStates, getLogger());
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(playerStates, sessionManager), this);
        getServer().getScheduler().runTaskTimer(this, sessionManager::tickAll, 1L, 1L);
        NatureCommand command = new NatureCommand(registry, sessionManager, index);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.build(), "Join and manage minigames", List.of("n7")));

        getLogger().info("Nature7 enabled");
    }

    @Override
    public void onDisable() {
        if (sessionManager != null) {
            sessionManager.disposeAll();
        }
        if (playerStates != null) {
            playerStates.shutdown();
        }
    }
}
