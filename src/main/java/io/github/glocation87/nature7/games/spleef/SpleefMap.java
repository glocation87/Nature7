package io.github.glocation87.nature7.games.spleef;

import io.github.glocation87.nature7.map.Point;
import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record SpleefMap(List<Point> spawns) {

    public SpleefMap {
        if (spawns == null || spawns.size() < 2) {
            throw new IllegalArgumentException("Spleef maps need at least 2 spawns");
        }
        spawns = List.copyOf(spawns);
    }
}
