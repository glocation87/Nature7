package io.github.glocation87.nature7.games.ctf;

import io.github.glocation87.nature7.module.TeamColor;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

// package private, nothing outside ctf should end up depending on it
final class Flag {
    private final TeamColor color;
    private final Location home;
    private @Nullable UUID carrier;

    Flag(TeamColor color, Location home) {
        this.color = color;
        this.home = home;
    }

    TeamColor color() {
        return color;
    }

    Location home() {
        return home;
    }

    boolean isHome() {
        return carrier == null;
    }

    boolean isCarriedBy(Player player) {
        return player.getUniqueId().equals(carrier);
    }

    void pickUp(Player player) {
        carrier = player.getUniqueId();
        home.getBlock().setType(Material.AIR);
    }

    void reset() {
        carrier = null;
        home.getBlock().setType(color.banner());
    }
}
