package io.github.glocation87.nature7.engine;

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.Nullable;

// a feature a game installs (teams, kits, deaths...), same hooks as the game and they always run before it
public abstract class GameModule {
    private @Nullable SessionProcess session;

    protected final SessionProcess session() {
        if (session == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " is not installed in a session");
        }
        return session;
    }

    final void attach(SessionProcess session) {
        if (this.session != null) {
            throw new IllegalStateException(getClass().getSimpleName() + " is already installed");
        }
        this.session = session;
    }

    protected void onInstall() {
    }

    protected void onPlayerJoin(Player player) {
    }

    protected void onPlayerLeave(Player player) {
    }

    protected void onStart() {
    }

    protected void onTick(long tick) {
    }

    protected void onEnd() {
    }

    protected void onDispose() {
    }

    protected List<Component> sidebar() {
        return List.of();
    }

    protected final <E extends Event> void listen(Class<E> type, Consumer<? super E> handler) {
        listen(type, EventPriority.NORMAL, handler);
    }

    protected final <E extends Event> void listen(Class<E> type, EventPriority priority, Consumer<? super E> handler) {
        session().events().listen(type, priority, handler);
    }
}
