package io.github.glocation87.nature7.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// not called Team so it doesn't clash with the bukkit scoreboard Team
public final class GameTeam {
    private final TeamColor color;
    private final Set<UUID> members = new LinkedHashSet<>();

    GameTeam(TeamColor color) {
        this.color = color;
    }

    public TeamColor color() {
        return color;
    }

    public Set<UUID> members() {
        return Collections.unmodifiableSet(members);
    }

    public boolean contains(UUID player) {
        return members.contains(player);
    }

    public List<Player> onlineMembers() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }

    void add(UUID player) {
        members.add(player);
    }

    void remove(UUID player) {
        members.remove(player);
    }
}
