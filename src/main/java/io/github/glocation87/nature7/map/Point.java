package io.github.glocation87.nature7.map;

import org.bukkit.Location;
import org.bukkit.World;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

// yaw and pitch are optional in map.yml and default to 0
@ConfigSerializable
public record Point(@Required double x, @Required double y, @Required double z, float yaw, float pitch) {

    public static Point of(Location location) {
        return new Point(
            round(location.getX()),
            round(location.getY()),
            round(location.getZ()),
            (float) round(location.getYaw()),
            (float) round(location.getPitch()));
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
