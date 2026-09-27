package io.github.glocation87.nature7.games.laststanding;

import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.eclipse.sisu.Nullable;

import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.types.GameType;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.damage.*;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LastStanding extends MinigameProcess {
    private static final double ARENA_RADIUS = 8.0;
    private static final Set<UUID> alive = new LinkedHashSet<UUID>();
    protected World gameWorld;
    protected Location spawnLocation;

    private LastStanding(SessionProcess session) {
        super(session);
    }

    public static final GameType TYPE = new GameType(
        "last_standing",
        Component.text("Last Standing", NamedTextColor.GREEN),
        Material.IRON_SWORD,
        1,
        2,
        LastStanding::init
    );


    private static LastStanding init(SessionProcess session) {
        return new LastStanding(session);
    }

    @Override
    public void onSetup() {
        listen(EntityDamageEvent.class, event -> {
            DamageSource source = event.getDamageSource();
            Entity damagedEntity = event.getEntity();
            if (source != null && source.getCausingEntity() != null) session.broadcastMessage(Component.text("| " + source.getCausingEntity().getName() + " -> damaged -> " + damagedEntity.getName() + " |"));
        });

        listen(PlayerDropItemEvent.class, event ->
            session.broadcastMessage(Component.text("Player [" + event.getPlayer().getName() + "] dropped item: " + event.getItemDrop().getName()))
        );
        this.gameWorld = Bukkit.getWorlds().getFirst();
        this.spawnLocation = new Location(this.gameWorld, -63.23f, 0.0f, -86.377f);
    }

    @Override
    public void onPlayerJoin(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        player.teleport(gameWorld.getSpawnLocation().clone().add(new Vector(0, 5, 0)));
        alive.add(playerId);
        session.broadcastMessage(Component.text(player.getName() + " has joined the game!"));
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        if (alive.remove(playerId)) {
            //player left
        }
    }

    @Override
    public void onStart()
    {
        //only expecting one player for gamemode
        Player player = Bukkit.getPlayer(alive.stream().findFirst().get());
        //set time to evening
        gameWorld.setFullTime(0);

        //populate player inventory with sword for battle
        ItemStack sword = ItemStack.of(Material.IRON_SWORD);
        sword.damage(90, player);
        player.getInventory().addItem(sword);
        player.spawnAt(spawnLocation);
        //amount of zombies to spawn == 10, angle is 360/10 ~ 36
        // x - math.cos(36*i) * r + x _enter
        // z - math.sin(36*i) * r + z_center
        // r will be 10 blocks
        for (int i = 1; i < 10; i++) {
            double angle = 2*Math.PI/i;
            Location playerLocation = player.getLocation();
            if (playerLocation == null) {
                session.broadcastMessage(Component.text("Player location is null"));
                return;
            }
            Vector zombieSpawnOffset = new Vector(ARENA_RADIUS * Math.cos(angle), 0.0, ARENA_RADIUS * Math.sin(angle));
            Zombie zombie = gameWorld.spawn(spawnLocation.clone().add(zombieSpawnOffset), Zombie.class);
            zombie.setGlowing(true);
            zombie.setVelocity(new Vector());
        }
    }



    private void eliminate(Player player) {
        alive.remove(player.getUniqueId());
    }

}
