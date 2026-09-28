package io.github.glocation87.nature7.games.laststanding;

import io.github.glocation87.nature7.map.Point;
import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

// spawn 0 is the player, the rest are where the zombies come from
@ConfigSerializable
public record LastStandingMap(List<Point> spawns) {

    public LastStandingMap {
        if (spawns == null || spawns.size() < 2) {
            throw new IllegalArgumentException("Last Standing maps need at least 2 spawns");
        }
        spawns = List.copyOf(spawns);
    }
}
