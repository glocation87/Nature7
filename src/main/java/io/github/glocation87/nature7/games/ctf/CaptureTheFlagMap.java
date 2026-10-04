package io.github.glocation87.nature7.games.ctf;

import io.github.glocation87.nature7.map.Point;
import java.util.List;
import java.util.Objects;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record CaptureTheFlagMap(List<Point> redSpawns, List<Point> blueSpawns, Point redFlag, Point blueFlag) {

    public CaptureTheFlagMap {
        if (redSpawns == null || redSpawns.isEmpty() || blueSpawns == null || blueSpawns.isEmpty()) {
            throw new IllegalArgumentException("Each team needs at least one spawn");
        }
        Objects.requireNonNull(redFlag, "red-flag is missing");
        Objects.requireNonNull(blueFlag, "blue-flag is missing");
        redSpawns = List.copyOf(redSpawns);
        blueSpawns = List.copyOf(blueSpawns);
    }
}
