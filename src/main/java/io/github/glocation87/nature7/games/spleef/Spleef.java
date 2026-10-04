package io.github.glocation87.nature7.games.spleef;

import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.engine.States;
import io.github.glocation87.nature7.games.GameResources;
import io.github.glocation87.nature7.map.Point;
import io.github.glocation87.nature7.module.DeathModule;
import io.github.glocation87.nature7.module.SpectatorModule;
import io.github.glocation87.nature7.module.StatsModule;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.world.MapInstance;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

// dig the snow out from under everyone else, last one standing wins
public final class Spleef extends MinigameProcess {
    private static final int GRACE_TICKS = 3 * 20;

    public static GameType type(GameResources resources) {
        return new GameType(
            "spleef",
            Component.text("Spleef", NamedTextColor.AQUA),
            Material.DIAMOND_SHOVEL,
            2,
            16,
            SpleefMap.class,
            session -> new Spleef(session, resources)
        );
    }

    private final DeathModule deaths;
    private long tick;

    private Spleef(SessionProcess session, GameResources resources) {
        super(session);
        install(new SpectatorModule(resources.plugin(), resources.tags()));
        deaths = install(DeathModule.builder().onEliminated(player -> checkForWinner()).build());
        install(new StatsModule(resources.stats()));
    }

    @Override
    protected void onSetup() {
        // NORMAL runs before the death module's HIGH, so by the time it looks only the void is left
        listen(EntityDamageEvent.class, event -> {
            if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
            }
        });
        listen(BlockBreakEvent.class, this::onBreak);
        listen(ProjectileHitEvent.class, this::onProjectileHit);
        listen(PlayerMoveEvent.class, this::onMove);
        listen(BlockPlaceEvent.class, event -> event.setCancelled(true));
        listen(PlayerDropItemEvent.class, event -> event.setCancelled(true));
        listen(FoodLevelChangeEvent.class, event -> event.setCancelled(true));
    }

    @Override
    protected void onStart() {
        MapInstance map = session.map();
        List<Point> spawns = map.data(SpleefMap.class).spawns();
        List<Player> players = session.players();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.teleport(map.location(spawns.get(i % spawns.size())));
            player.setGameMode(GameMode.SURVIVAL);
            ItemStack shovel = ItemStack.of(Material.DIAMOND_SHOVEL);
            shovel.addUnsafeEnchantment(Enchantment.EFFICIENCY, 5);
            player.getInventory().addItem(shovel);
        }
    }

    @Override
    protected void onTick(long tick) {
        this.tick = tick;
    }

    @Override
    protected void onPlayerLeave(UUID playerId) {
        checkForWinner();
    }

    @Override
    protected List<Component> sidebar() {
        return List.of(Component.text("Alive: ", NamedTextColor.GRAY)
            .append(Component.text(deaths.alivePlayers().size(), NamedTextColor.WHITE)));
    }

    private void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (session.state() != States.ACTIVE || tick < GRACE_TICKS
                || event.getBlock().getType() != Material.SNOW_BLOCK || !deaths.isAlive(player)) {
            event.setCancelled(true);
            return;
        }
        event.setDropItems(false);
        player.getInventory().addItem(ItemStack.of(Material.SNOWBALL));
    }

    // a snowball has no player subject, this only gets here through routing by world
    private void onProjectileHit(ProjectileHitEvent event) {
        Block block = event.getHitBlock();
        if (event.getEntity() instanceof Snowball && block != null && block.getType() == Material.SNOW_BLOCK) {
            block.setType(Material.AIR);
        }
    }

    private void onMove(PlayerMoveEvent event) {
        if (event.hasChangedBlock() && session.state() == States.ACTIVE
                && !session.map().info().bounds().contains(event.getTo())) {
            deaths.kill(event.getPlayer());
        }
    }

    private void checkForWinner() {
        List<Player> alive = deaths.alivePlayers();
        if (session.state() == States.ACTIVE && alive.size() <= 1) {
            session.end(alive.isEmpty() ? null : alive.getFirst());
        }
    }
}
