package io.github.glocation87.nature7.map;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

// the part of map.yml every game shares, everything under game: is per game
@ConfigSerializable
public record MapInfo(
    String name,
    List<String> authors,
    @Nullable String source,
    @Nullable String license,
    Point spectatorSpawn,
    Region bounds
) {
    public MapInfo {
        Objects.requireNonNull(name, "name is missing");
        Objects.requireNonNull(spectatorSpawn, "spectator-spawn is missing");
        Objects.requireNonNull(bounds, "bounds is missing");
        authors = authors == null ? List.of() : List.copyOf(authors);
    }

    public String credit() {
        return authors.isEmpty() ? name : name + " by " + String.join(", ", authors);
    }
}
