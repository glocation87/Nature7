package io.github.glocation87.nature7.world;

import io.github.glocation87.nature7.map.GameMap;
import io.github.glocation87.nature7.map.MapInfo;
import io.github.glocation87.nature7.map.Point;
import org.bukkit.Location;
import org.bukkit.World;

// a running copy of a map, one per match
public record MapInstance(String name, GameMap map, World world) {

    public Location location(Point point) {
        return point.toLocation(world);
    }

    public MapInfo info() {
        return map.info();
    }

    public <T> T data(Class<T> schema) {
        return map.data(schema);
    }
}
