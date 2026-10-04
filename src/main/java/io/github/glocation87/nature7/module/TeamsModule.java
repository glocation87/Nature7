package io.github.glocation87.nature7.module;

import io.github.glocation87.nature7.engine.GameModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class TeamsModule extends GameModule {
    private final List<GameTeam> teams;
    private final Map<UUID, GameTeam> byPlayer = new HashMap<>();

    public TeamsModule(TeamColor... colors) {
        if (colors.length < 2) {
            throw new IllegalArgumentException("A team game needs at least two teams");
        }
        List<GameTeam> list = new ArrayList<>();
        for (TeamColor color : colors) {
            list.add(new GameTeam(color));
        }
        teams = List.copyOf(list);
    }

    public List<GameTeam> teams() {
        return teams;
    }

    public GameTeam team(TeamColor color) {
        for (GameTeam team : teams) {
            if (team.color() == color) {
                return team;
            }
        }
        throw new IllegalArgumentException("No " + color + " team in this game");
    }

    public Optional<GameTeam> teamOf(Player player) {
        return Optional.ofNullable(byPlayer.get(player.getUniqueId()));
    }

    public boolean areTeammates(Player first, Player second) {
        GameTeam team = byPlayer.get(first.getUniqueId());
        return team != null && team.contains(second.getUniqueId());
    }

    @Override
    protected void onInstall() {
        listen(EntityDamageByEntityEvent.class, EventPriority.LOW, this::onDamage);
    }

    // split at the start, not on join, people come and go freely while waiting
    @Override
    protected void onStart() {
        List<UUID> players = session().players().stream().map(Player::getUniqueId).toList();
        List<List<UUID>> split = TeamBalancer.split(players, teams.size(), session().random());
        Scoreboard scoreboard = session().scoreboard();
        for (int i = 0; i < teams.size(); i++) {
            GameTeam team = teams.get(i);
            Team display = scoreboard.registerNewTeam(team.color().name().toLowerCase(Locale.ROOT));
            display.color(team.color().textColor());
            display.prefix(Component.text("[" + team.color().name().charAt(0) + "] ", team.color().textColor()));
            for (UUID uuid : split.get(i)) {
                team.add(uuid);
                byPlayer.put(uuid, team);
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    display.addEntry(player.getName());
                    player.sendMessage(Component.text("You are on the ", NamedTextColor.GRAY)
                        .append(team.color().displayName())
                        .append(Component.text(" team", NamedTextColor.GRAY)));
                }
            }
        }
    }

    @Override
    protected void onPlayerLeave(Player player) {
        GameTeam team = byPlayer.remove(player.getUniqueId());
        if (team == null) {
            return;
        }
        team.remove(player.getUniqueId());
        Team display = session().scoreboard().getEntryTeam(player.getName());
        if (display != null) {
            display.removeEntry(player.getName());
        }
    }

    @Override
    protected List<Component> sidebar() {
        List<Component> lines = new ArrayList<>();
        for (GameTeam team : teams) {
            lines.add(team.color().displayName().append(Component.text(": " + team.members().size(), NamedTextColor.WHITE)));
        }
        return lines;
    }

    // the vanilla friendly fire rule only reads the main scoreboard, a session scoreboard just changes colours
    private void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = Combat.attackerOf(event.getDamager());
        if (attacker != null && event.getEntity() instanceof Player victim
                && !attacker.equals(victim) && areTeammates(attacker, victim)) {
            event.setCancelled(true);
        }
    }
}
