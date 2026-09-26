package io.github.glocation87.nature7.games.laststanding;

import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.World;

import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.types.GameType;
import org.bukkit.damage.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LastStanding extends MinigameProcess {
    private static final double ARENA_RADIUS = 8.0;
    private static final Set<UUID> active_players = new LinkedHashSet<UUID>();

    LastStanding(SessionProcess session) {
        super(session);
    }

    public static final GameType TYPE = new GameType(
        "last_standing",
        Component.text("Last Standing", NamedTextColor.GREEN),
        Material.IRON_SWORD,
        2,
        8,
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
            session.broadcastMessage(Component.text("| " + source.getCausingEntity().getName() + " -> damaged -> " + damagedEntity.getName() + " |"));
        });

        listen(PlayerDropItemEvent.class, event ->
            session.broadcastMessage(Component.text("Player [" + event.getPlayer().getName() + "] dropped item: " + event.getItemDrop().getName()))
        );
    }

    @Override
    public void onPlayerJoin(Player player) {
        World worldInstance = Bukkit.getWorlds().getFirst();
        player.teleport(worldInstance.getSpawnLocation());
        session.broadcastMessage(Component.text(player.getName() + " has joined the game!"));
    }
}
