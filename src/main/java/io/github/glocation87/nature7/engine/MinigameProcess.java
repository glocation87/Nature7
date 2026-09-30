package io.github.glocation87.nature7.engine;

import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;

/*
* Minigame base class contract
* the engine will hook lifecycle events on inherited instances
* callbacks will execute defined behavior in respective subclasses
*/
public abstract class MinigameProcess {
    protected final SessionProcess session;

    protected MinigameProcess(SessionProcess session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    //Main session lifecycle hooks
    protected void onSetup() {

    }

    protected void onPlayerJoin(UUID playerId) {

    }

    protected void onPlayerLeave(UUID playerId) {

    }

    protected void onStart() {

    }

    protected void onTick(long tick) {

    }

    protected void onEnd() {

    }

    protected void onDispose() {

    }

    // lines the game adds to the middle of the sidebar while it's active, the engine owns the rest
    protected List<Component> sidebar() {
        return List.of();
    }

    //Event dispatch listener hook
    protected final <E extends Event> void listen(Class<E> type, Consumer<? super E> eventHandler) {
        session.events().listen(type, eventHandler);
    }

}
