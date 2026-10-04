package io.github.glocation87.nature7.module;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.jspecify.annotations.Nullable;

public final class Combat {

    private Combat() {
    }

    // the player behind a hit, arrows and snowballs count, mobs and tnt don't
    public static @Nullable Player attackerOf(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
