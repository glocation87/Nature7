package io.github.glocation87.nature7.engine;

import io.github.glocation87.nature7.item.HotbarItems;
import io.github.glocation87.nature7.lobby.Lobby;
import io.github.glocation87.nature7.map.MapRegistry;
import io.github.glocation87.nature7.player.PlayerStateService;
import io.github.glocation87.nature7.world.WorldInstancer;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

// parameter object, only things that are the same for every session go in here
public record SessionServices(
    PlayerStateService playerStates,
    SessionIndex index,
    MapRegistry maps,
    WorldInstancer instancer,
    Lobby lobby,
    HotbarItems hotbar,
    Executor mainThread,
    RandomGenerator random,
    Logger logger
) {
}
