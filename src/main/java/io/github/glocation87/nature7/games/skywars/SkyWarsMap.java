package io.github.glocation87.nature7.games.skywars;

import io.github.glocation87.nature7.map.Point;
import java.util.List;
import java.util.Objects;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record SkyWarsMap(List<Point> spawns, List<Point> islandChests, List<Point> centerChests, Point center) {

    public SkyWarsMap {
        if (spawns == null || spawns.size() < 2) {
            throw new IllegalArgumentException("SkyWars maps need at least 2 spawns");
        }
        Objects.requireNonNull(center, "center is missing");
        spawns = List.copyOf(spawns);
        islandChests = islandChests == null ? List.of() : List.copyOf(islandChests);
        centerChests = centerChests == null ? List.of() : List.copyOf(centerChests);
    }
}
