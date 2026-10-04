package io.github.glocation87.nature7.module;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jspecify.annotations.Nullable;

// a PlayerEvent, so the router hands it to the victim's session like any other player event
public final class GameDeathEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable Player killer;
    private final boolean eliminated;

    public GameDeathEvent(Player victim, @Nullable Player killer, boolean eliminated) {
        super(victim);
        this.killer = killer;
        this.eliminated = eliminated;
    }

    public @Nullable Player getKiller() {
        return killer;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
