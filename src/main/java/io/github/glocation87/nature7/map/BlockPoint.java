package io.github.glocation87.nature7.map;

import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

@ConfigSerializable
public record BlockPoint(@Required int x, @Required int y, @Required int z) {

    public static BlockPoint of(Location location) {
        return new BlockPoint(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
