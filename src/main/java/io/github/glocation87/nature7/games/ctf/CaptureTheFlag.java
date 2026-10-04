package io.github.glocation87.nature7.games.ctf;

import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.engine.States;
import io.github.glocation87.nature7.games.GameResources;
import io.github.glocation87.nature7.map.Point;
import io.github.glocation87.nature7.module.DeathModule;
import io.github.glocation87.nature7.module.GameDeathEvent;
import io.github.glocation87.nature7.module.GameTeam;
import io.github.glocation87.nature7.module.KitModule;
import io.github.glocation87.nature7.module.StatsModule;
import io.github.glocation87.nature7.module.TeamColor;
import io.github.glocation87.nature7.module.TeamsModule;
import io.github.glocation87.nature7.module.Timeline.Milestone;
import io.github.glocation87.nature7.module.TimelineModule;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.world.MapInstance;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

// red vs blue, carry the enemy banner back to your own, first to 3 or the higher score at 10:00
public final class CaptureTheFlag extends MinigameProcess {
    private static final int CAPTURES_TO_WIN = 3;
    private static final double REACH_SQUARED = 1.5 * 1.5;

    public static GameType type(GameResources resources) {
        return new GameType(
            "capture_the_flag",
            Component.text("Capture the Flag", NamedTextColor.GREEN),
            Material.WHITE_BANNER,
            2,
            16,
            CaptureTheFlagMap.class,
            session -> new CaptureTheFlag(session, resources)
        );
    }

    private final TeamsModule teams;
    private final DeathModule deaths;
    private final Map<TeamColor, Flag> flags = new EnumMap<>(TeamColor.class);
    private final Map<TeamColor, Integer> captures = new EnumMap<>(TeamColor.class);

    private CaptureTheFlag(SessionProcess session, GameResources resources) {
        super(session);
        teams = install(new TeamsModule(TeamColor.RED, TeamColor.BLUE));
        install(new KitModule(resources.kits().kitsFor("capture_the_flag"), resources.tags()));
        // nobody gets eliminated, so no spectator module
        deaths = install(DeathModule.builder()
            .unlimitedLives()
            .respawnSeconds(5)
            .respawnAt(this::spawnFor)
            .build());
        install(new StatsModule(resources.stats()));
        install(new TimelineModule(List.of(new Milestone("Time up", 600, this::endByScore))));
    }

    @Override
    protected void onSetup() {
        listen(PlayerMoveEvent.class, this::onMove);
        listen(GameDeathEvent.class, event -> returnFlagCarriedBy(event.getPlayer()));
        // nobody digs a banner out of its base
        listen(BlockBreakEvent.class, event -> {
            for (Flag flag : flags.values()) {
                if (event.getBlock().getLocation().equals(flag.home().getBlock().getLocation())) {
                    event.setCancelled(true);
                }
            }
        });
    }

    // the teams module already split everyone, modules start before the game
    @Override
    protected void onStart() {
        MapInstance map = session.map();
        CaptureTheFlagMap data = map.data(CaptureTheFlagMap.class);
        flags.put(TeamColor.RED, new Flag(TeamColor.RED, map.location(data.redFlag())));
        flags.put(TeamColor.BLUE, new Flag(TeamColor.BLUE, map.location(data.blueFlag())));
        for (Flag flag : flags.values()) {
            flag.reset();
            captures.put(flag.color(), 0);
        }
        for (Player player : session.players()) {
            player.teleport(spawnFor(player));
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    // still online while leaving, so the carried flag and the glow can be cleaned up
    @Override
    protected void onPlayerLeave(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            returnFlagCarriedBy(player);
        }
        if (session.state() != States.ACTIVE) {
            return;
        }
        for (GameTeam team : teams.teams()) {
            if (team.members().isEmpty()) {
                win(opponentOf(team.color()));
                return;
            }
        }
    }

    @Override
    protected List<Component> sidebar() {
        List<Component> lines = new ArrayList<>();
        for (Flag flag : flags.values()) {
            lines.add(flag.color().displayName()
                .append(Component.text(" captures: " + captures.get(flag.color()) + "/" + CAPTURES_TO_WIN, NamedTextColor.WHITE)));
        }
        return lines;
    }

    private Location spawnFor(Player player) {
        MapInstance map = session.map();
        CaptureTheFlagMap data = map.data(CaptureTheFlagMap.class);
        TeamColor color = teams.teamOf(player).map(GameTeam::color).orElse(TeamColor.RED);
        List<Point> spawns = color == TeamColor.RED ? data.redSpawns() : data.blueSpawns();
        return map.location(spawns.get(session.random().nextInt(spawns.size())));
    }

    private void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!event.hasChangedBlock() || session.state() != States.ACTIVE
                || !deaths.isAlive(player) || deaths.isRespawning(player)) {
            return;
        }
        GameTeam team = teams.teamOf(player).orElse(null);
        if (team == null) {
            return;
        }
        Flag own = flags.get(team.color());
        Flag enemy = flags.get(opponentOf(team.color()));
        Location to = event.getTo();
        if (enemy.isHome() && near(to, enemy.home())) {
            enemy.pickUp(player);
            player.setGlowing(true);
            player.getInventory().setHelmet(ItemStack.of(enemy.color().banner()));
            session.broadcastMessage(Component.text(player.getName() + " took the ", NamedTextColor.YELLOW)
                .append(enemy.color().displayName())
                .append(Component.text(" flag!", NamedTextColor.YELLOW)));
        } else if (enemy.isCarriedBy(player) && own.isHome() && near(to, own.home())) {
            int score = captures.merge(team.color(), 1, Integer::sum);
            dropFlag(enemy, player);
            session.broadcastMessage(Component.text(player.getName() + " captured the ", NamedTextColor.GOLD)
                .append(enemy.color().displayName())
                .append(Component.text(" flag!", NamedTextColor.GOLD)));
            if (score >= CAPTURES_TO_WIN) {
                win(team.color());
            }
        }
    }

    private void returnFlagCarriedBy(Player player) {
        for (Flag flag : flags.values()) {
            if (flag.isCarriedBy(player)) {
                dropFlag(flag, player);
                session.broadcastMessage(Component.text("The ", NamedTextColor.GRAY)
                    .append(flag.color().displayName())
                    .append(Component.text(" flag was returned", NamedTextColor.GRAY)));
            }
        }
    }

    private void dropFlag(Flag flag, Player carrier) {
        flag.reset();
        carrier.setGlowing(false);
        carrier.getInventory().setHelmet(null);
    }

    private void endByScore() {
        int red = captures.get(TeamColor.RED);
        int blue = captures.get(TeamColor.BLUE);
        if (red == blue) {
            session.end(null);
        } else {
            win(red > blue ? TeamColor.RED : TeamColor.BLUE);
        }
    }

    private void win(TeamColor color) {
        session.end(color.displayName().append(Component.text(" team wins!", color.textColor())),
            teams.team(color).onlineMembers());
    }

    private static TeamColor opponentOf(TeamColor color) {
        return color == TeamColor.RED ? TeamColor.BLUE : TeamColor.RED;
    }

    // measured from the middle of the flag's block so the reach is the same from every side
    private static boolean near(Location location, Location target) {
        return location.distanceSquared(target.getBlock().getLocation().add(0.5, 0, 0.5)) <= REACH_SQUARED;
    }
}
