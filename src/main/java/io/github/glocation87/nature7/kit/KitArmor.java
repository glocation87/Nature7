package io.github.glocation87.nature7.kit;

import org.bukkit.Material;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record KitArmor(
    @Nullable Material helmet,
    @Nullable Material chestplate,
    @Nullable Material leggings,
    @Nullable Material boots
) {
}
