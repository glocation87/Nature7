package io.github.glocation87.nature7.engine;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import io.github.glocation87.nature7.engine.SessionProcess;

/*
* Minigame base class contract
* the engine will hook lifecycle events on inherited instances
* callbacks will execute defined behavior in respective subclasses
*/
abstract class MinigameProcess {
    protected final SessionProcess session;

    protected MinigameProcess(SessionProcess session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    //Main session lifecycle hooks
    protected void onSetup() {

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

    //Event dispatch listener hook
    /*protected final <E extends Event> void listen(Class<E> type, Consumer<? super E> event_handler) {
        session.events().listen(type, event_handler);
    }
    */
}
