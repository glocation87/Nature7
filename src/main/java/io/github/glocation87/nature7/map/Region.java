package io.github.glocation87.nature7.map;

import java.util.Objects;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record Region(BlockPoint min, BlockPoint max) {

    public Region {
        Objects.requireNonNull(min, "min is missing");
        Objects.requireNonNull(max, "max is missing");
        BlockPoint low = new BlockPoint(Math.min(min.x(), max.x()), Math.min(min.y(), max.y()), Math.min(min.z(), max.z()));
        BlockPoint high = new BlockPoint(Math.max(min.x(), max.x()), Math.max(min.y(), max.y()), Math.max(min.z(), max.z()));
        min = low;
        max = high;
    }

    public boolean contains(double x, double y, double z) {
        // block coords are inclusive, so the far edge is max + 1
        return x >= min.x() && x < max.x() + 1
            && y >= min.y() && y < max.y() + 1
            && z >= min.z() && z < max.z() + 1;
    }

    public boolean contains(Location location) {
        return contains(location.getX(), location.getY(), location.getZ());
    }
}
