package io.github.glocation87.nature7.module;

import io.github.glocation87.nature7.engine.GameModule;
import io.github.glocation87.nature7.engine.States;
import io.github.glocation87.nature7.player.PlayerStateService;
import io.github.glocation87.nature7.world.MapInstance;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public final class DeathModule extends GameModule {
    // knocked off a ledge and dying to the fall still counts for whoever hit you
    private static final int KILL_CREDIT_TICKS = 10 * 20;

    private record Hit(UUID attacker, long tick) {
    }

    private final int lives;
    private final int respawnTicks;
    private final int protectionTicks;
    private final boolean dropItems;
    private final @Nullable Function<Player, Location> respawnPoint;
    private final Predicate<Player> canRespawn;
    private final Consumer<Player> onEliminated;

    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Map<UUID, Integer> livesLeft = new HashMap<>();
    // timers are ticks checked in onTick, scheduler tasks could outlive a disposed session
    private final Map<UUID, Long> respawnAt = new HashMap<>();
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private final Map<UUID, Hit> lastHit = new HashMap<>();
    private long now;

    private DeathModule(Builder builder) {
        lives = builder.lives;
        respawnTicks = builder.respawnSeconds * 20;
        protectionTicks = builder.protectionSeconds * 20;
        dropItems = builder.dropItems;
        respawnPoint = builder.respawnPoint;
        canRespawn = builder.canRespawn;
        onEliminated = builder.onEliminated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isAlive(Player player) {
        return alive.contains(player.getUniqueId());
    }

    public boolean isRespawning(Player player) {
        return respawnAt.containsKey(player.getUniqueId());
    }

    public List<Player> alivePlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : alive) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    // for deaths the damage system never sees, like walking out of the map
    public void kill(Player victim) {
        if (isAlive(victim) && !isRespawning(victim)) {
            die(victim);
        }
    }

    @Override
    protected void onInstall() {
        // HIGH so games and other modules get to cancel damage before it's judged lethal
        listen(EntityDamageEvent.class, EventPriority.HIGH, this::onDamage);
    }

    @Override
    protected void onStart() {
        for (Player player : session().players()) {
            alive.add(player.getUniqueId());
            livesLeft.put(player.getUniqueId(), lives);
        }
    }

    @Override
    protected void onTick(long tick) {
        now = tick;
        Iterator<Map.Entry<UUID, Long>> due = respawnAt.entrySet().iterator();
        while (due.hasNext()) {
            Map.Entry<UUID, Long> entry = due.next();
            if (entry.getValue() > now) {
                continue;
            }
            due.remove();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                respawn(player);
            }
        }
    }

    @Override
    protected void onPlayerLeave(Player player) {
        UUID uuid = player.getUniqueId();
        alive.remove(uuid);
        livesLeft.remove(uuid);
        respawnAt.remove(uuid);
        protectedUntil.remove(uuid);
        lastHit.remove(uuid);
    }

    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        UUID uuid = victim.getUniqueId();
        if (session().state() != States.ACTIVE || !alive.contains(uuid) || respawnAt.containsKey(uuid)
                || protectedUntil.getOrDefault(uuid, -1L) > now) {
            event.setCancelled(true);
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Player attacker = Combat.attackerOf(byEntity.getDamager());
            if (attacker != null && !attacker.equals(victim)) {
                lastHit.put(uuid, new Hit(attacker.getUniqueId(), now));
            }
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID || event.getFinalDamage() >= victim.getHealth()) {
            // no death screen, the module decides what happens next
            event.setCancelled(true);
            die(victim);
        }
    }

    private void die(Player victim) {
        UUID uuid = victim.getUniqueId();
        Player killer = recentAttacker(uuid);
        lastHit.remove(uuid);
        int left = livesLeft.merge(uuid, -1, Integer::sum);
        boolean eliminated = left <= 0 || !canRespawn.test(victim);

        Bukkit.getPluginManager().callEvent(new GameDeathEvent(victim, killer, eliminated));
        session().broadcastMessage(killer == null
            ? Component.text(victim.getName() + " died", NamedTextColor.GRAY)
            : Component.text(victim.getName() + " was killed by " + killer.getName(), NamedTextColor.GRAY));

        if (dropItems) {
            for (ItemStack item : victim.getInventory().getContents()) {
                if (item != null && !item.isEmpty()) {
                    victim.getWorld().dropItemNaturally(victim.getLocation(), item);
                }
            }
        }
        PlayerStateService.reset(victim);
        MapInstance map = session().map();
        victim.teleport(map.location(map.info().spectatorSpawn()));

        if (eliminated) {
            alive.remove(uuid);
            session().module(SpectatorModule.class).ifPresentOrElse(
                spectators -> spectators.spectate(victim),
                () -> victim.setGameMode(GameMode.SPECTATOR));
            session().broadcastMessage(Component.text(victim.getName() + " was eliminated", NamedTextColor.RED));
            onEliminated.accept(victim);
        } else {
            victim.setGameMode(GameMode.SPECTATOR);
            respawnAt.put(uuid, now + respawnTicks);
            victim.showTitle(Title.title(
                Component.text("You died", NamedTextColor.RED),
                Component.text("Respawning in " + respawnTicks / 20 + "s", NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));
        }
    }

    private void respawn(Player player) {
        // build() makes sure there's a respawn point whenever anyone can respawn
        player.teleport(Objects.requireNonNull(respawnPoint, "respawnPoint").apply(player));
        player.setGameMode(GameMode.SURVIVAL);
        session().module(KitModule.class).ifPresent(kits -> kits.apply(player));
        protectedUntil.put(player.getUniqueId(), now + protectionTicks);
    }

    private @Nullable Player recentAttacker(UUID victim) {
        Hit hit = lastHit.get(victim);
        if (hit == null || now - hit.tick() > KILL_CREDIT_TICKS) {
            return null;
        }
        return Bukkit.getPlayer(hit.attacker());
    }

    public static final class Builder {
        private int lives = 1;
        private int respawnSeconds = 5;
        private int protectionSeconds = 3;
        private boolean dropItems;
        private @Nullable Function<Player, Location> respawnPoint;
        private Predicate<Player> canRespawn = player -> true;
        private Consumer<Player> onEliminated = player -> {
        };

        private Builder() {
        }

        public Builder lives(int lives) {
            if (lives < 1) {
                throw new IllegalArgumentException("Players need at least one life, got " + lives);
            }
            this.lives = lives;
            return this;
        }

        public Builder unlimitedLives() {
            lives = Integer.MAX_VALUE;
            return this;
        }

        public Builder respawnSeconds(int seconds) {
            respawnSeconds = seconds;
            return this;
        }

        public Builder protectionSeconds(int seconds) {
            protectionSeconds = seconds;
            return this;
        }

        public Builder dropItems(boolean dropItems) {
            this.dropItems = dropItems;
            return this;
        }

        public Builder respawnAt(Function<Player, Location> respawnPoint) {
            this.respawnPoint = respawnPoint;
            return this;
        }

        public Builder canRespawn(Predicate<Player> canRespawn) {
            this.canRespawn = canRespawn;
            return this;
        }

        public Builder onEliminated(Consumer<Player> onEliminated) {
            this.onEliminated = onEliminated;
            return this;
        }

        public DeathModule build() {
            if (lives > 1 && respawnPoint == null) {
                throw new IllegalStateException("respawnAt is required when players have more than one life");
            }
            return new DeathModule(this);
        }
    }
}
